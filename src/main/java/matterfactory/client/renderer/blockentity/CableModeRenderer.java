package matterfactory.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class CableModeRenderer<T extends BaseCableBlockEntity> implements BlockEntityRenderer<T, CableModeRenderState> {

	private static final int IMPORT_COLOR = 0xFF186EFF;
	private static final int EXPORT_COLOR = 0xFFFF7418;
	private static final float ITEM_TRAVEL_DISTANCE = 0.5F;
	private static final float ITEM_SCALE = 0.5F;
	private static final float BLOCK_SCALE = 0.75F;

	private final ItemModelResolver itemModelResolver;

	public CableModeRenderer (BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public @NonNull CableModeRenderState createRenderState () {
		return new CableModeRenderState();
	}

	@Override
	public void extractRenderState (@NonNull T blockEntity, @NonNull CableModeRenderState state, float partialTick, @NonNull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);

		Level level = blockEntity.getLevel();
		BlockState blockState = blockEntity.getBlockState();
		extractConnectionModes(state, blockEntity, level, blockState);

		extractVisualItem(blockEntity, state, partialTick, level);
	}

	@Override
	public void submit (@NonNull CableModeRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
		submitConnectionModeBands(state, poseStack, collector);

		for (CableModeRenderState.VisualItemRenderState visualItem : state.visualItems) {
			renderVisualItem(visualItem, poseStack, collector, cameraState, state.lightCoords);
		}
	}

	static void extractConnectionModes (CableModeRenderState state, BaseCableBlockEntity cable, Level level, BlockState blockState) {
		state.geometry = blockState.getBlock() instanceof CableBlock cableBlock ? cableBlock.getRenderGeometry() : CableBlock.CableRenderGeometry.POWER_CABLE;
		for (Direction direction : Direction.values()) {
			state.modes[direction.ordinal()] = cable.getConnectionMode(direction);
			state.endpoints[direction.ordinal()] = level != null && cable.isEndpointConnection(level, cable.getBlockPos(), blockState, direction);
		}
	}

	static void submitConnectionModeBands (CableModeRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		for (Direction direction : Direction.values()) {
			CableConnectionMode mode = state.modes[direction.ordinal()];
			if (mode == CableConnectionMode.AUTO || !state.endpoints[direction.ordinal()]) {
				continue;
			}

			int color = mode == CableConnectionMode.IMPORT ? IMPORT_COLOR : EXPORT_COLOR;
			collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> renderModeBand(pose, consumer, direction, state.geometry, color));
		}
	}

	private void extractVisualItem (T blockEntity, CableModeRenderState state, float partialTick, Level level) {
		if (!(blockEntity instanceof ItemPipeBlockEntity itemPipe) || level == null) {
			state.visualItems.clear();
			return;
		}

		float gameTime = level.getGameTime() + partialTick;
		int visualIndex = 0;
		for (ItemPipeBlockEntity.VisualItemTransfer transfer : itemPipe.getVisualTransfers()) {
			if (!transfer.isActive(gameTime)) {
				continue;
			}

			CableModeRenderState.VisualItemRenderState visual = getVisualItemState(state, visualIndex++);
			visual.from = transfer.from();
			visual.to = transfer.to();
			visual.itemCount = transfer.item().getCount();
			visual.elapsedTicks = gameTime - transfer.startGameTime();
			visual.travelDuration = transfer.duration();
			visual.itemSpacing = transfer.itemSpacing();
			itemModelResolver.updateForTopItem(visual.item, transfer.item(), ItemDisplayContext.GROUND, level, null, (int) itemPipe.getBlockPos().asLong());
		}

		while (state.visualItems.size() > visualIndex) {
			state.visualItems.removeLast();
		}
	}

	private static CableModeRenderState.VisualItemRenderState getVisualItemState (CableModeRenderState state, int index) {
		while (state.visualItems.size() <= index) {
			state.visualItems.add(new CableModeRenderState.VisualItemRenderState());
		}

		return state.visualItems.get(index);
	}

	private static void renderVisualItem (CableModeRenderState.VisualItemRenderState visual, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, int lightCoords) {
		int count = Math.max(1, visual.itemCount);
		for (int index = 0; index < count; index++) {
			float itemElapsed = visual.elapsedTicks - index * visual.itemSpacing;
			if (itemElapsed < 0.0F || itemElapsed > visual.travelDuration) {
				continue;
			}

			float itemProgress = Mth.clamp(itemElapsed / Math.max(1, visual.travelDuration), 0.0F, 1.0F);
			renderVisualItemAt(visual, poseStack, collector, cameraState, lightCoords, itemProgress);
		}
	}

	private static void renderVisualItemAt (CableModeRenderState.VisualItemRenderState visual, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, int lightCoords, float progress) {
		Vec3 position = sharpPipePathPoint(visual.from, visual.to, progress);

		poseStack.pushPose();
		poseStack.translate(position.x, position.y, position.z);
		poseStack.mulPose(cameraState.orientation);
		poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
		visual.item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

	private static Vec3 sharpPipePathPoint (Direction fromDirection, Direction toDirection, float progress) {
		Vec3 from = sidePoint(fromDirection);
		Vec3 to = sidePoint(toDirection);
		Vec3 center = new Vec3(0.5, 0.5, 0.5);
		float segmentProgress = progress * 2.0F;

		if (segmentProgress <= 1.0F) {
			return lerp(from, center, segmentProgress);
		}

		return lerp(center, to, segmentProgress - 1.0F);
	}

	private static Vec3 lerp (Vec3 from, Vec3 to, float progress) {
		return new Vec3(
				Mth.lerp(progress, from.x, to.x),
				Mth.lerp(progress, from.y, to.y),
				Mth.lerp(progress, from.z, to.z));
	}

	private static Vec3 sidePoint (Direction direction) {
		return new Vec3(
				0.5 + direction.getStepX() * ITEM_TRAVEL_DISTANCE,
				0.5 + direction.getStepY() * ITEM_TRAVEL_DISTANCE,
				0.5 + direction.getStepZ() * ITEM_TRAVEL_DISTANCE);
	}

	private static void renderModeBand (PoseStack.Pose pose, VertexConsumer consumer, Direction direction, CableBlock.CableRenderGeometry geometry, int color) {
		float outerMin = geometry.crossMin() - 0.25F / 16.0F;
		float outerMax = geometry.crossMax() + 0.25F / 16.0F;
		float innerMin = geometry.crossMin() + 1.0F / 16.0F;
		float innerMax = geometry.crossMax() - 1.0F / 16.0F;

		switch (direction) {
			case DOWN -> renderVerticalModeFrame(pose, consumer, geometry.bandMin(), geometry.bandMax(), outerMin, outerMax, innerMin, innerMax, color);
			case UP -> renderVerticalModeFrame(pose, consumer, 1.0F - geometry.bandMax(), 1.0F - geometry.bandMin(), outerMin, outerMax, innerMin, innerMax, color);
			case NORTH -> renderNorthSouthModeFrame(pose, consumer, geometry.bandMin(), geometry.bandMax(), outerMin, outerMax, innerMin, innerMax, color);
			case SOUTH -> renderNorthSouthModeFrame(pose, consumer, 1.0F - geometry.bandMax(), 1.0F - geometry.bandMin(), outerMin, outerMax, innerMin, innerMax, color);
			case WEST -> renderEastWestModeFrame(pose, consumer, geometry.bandMin(), geometry.bandMax(), outerMin, outerMax, innerMin, innerMax, color);
			case EAST -> renderEastWestModeFrame(pose, consumer, 1.0F - geometry.bandMax(), 1.0F - geometry.bandMin(), outerMin, outerMax, innerMin, innerMax, color);
		}
	}

	private static void renderVerticalModeFrame (PoseStack.Pose pose, VertexConsumer consumer, float minY, float maxY, float outerMin, float outerMax, float innerMin, float innerMax, int color) {
		renderBox(pose, consumer, outerMin, minY, outerMin, outerMax, maxY, innerMin, color);
		renderBox(pose, consumer, outerMin, minY, innerMax, outerMax, maxY, outerMax, color);
		renderBox(pose, consumer, outerMin, minY, innerMin, innerMin, maxY, innerMax, color);
		renderBox(pose, consumer, innerMax, minY, innerMin, outerMax, maxY, innerMax, color);
	}

	private static void renderNorthSouthModeFrame (PoseStack.Pose pose, VertexConsumer consumer, float minZ, float maxZ, float outerMin, float outerMax, float innerMin, float innerMax, int color) {
		renderBox(pose, consumer, outerMin, outerMin, minZ, outerMax, innerMin, maxZ, color);
		renderBox(pose, consumer, outerMin, innerMax, minZ, outerMax, outerMax, maxZ, color);
		renderBox(pose, consumer, outerMin, innerMin, minZ, innerMin, innerMax, maxZ, color);
		renderBox(pose, consumer, innerMax, innerMin, minZ, outerMax, innerMax, maxZ, color);
	}

	private static void renderEastWestModeFrame (PoseStack.Pose pose, VertexConsumer consumer, float minX, float maxX, float outerMin, float outerMax, float innerMin, float innerMax, int color) {
		renderBox(pose, consumer, minX, outerMin, outerMin, maxX, innerMin, outerMax, color);
		renderBox(pose, consumer, minX, innerMax, outerMin, maxX, outerMax, outerMax, color);
		renderBox(pose, consumer, minX, innerMin, outerMin, maxX, innerMax, innerMin, color);
		renderBox(pose, consumer, minX, innerMin, innerMax, maxX, innerMax, outerMax, color);
	}

	private static void renderBox (PoseStack.Pose pose, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
		addQuad(pose, consumer, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, color);
		addQuad(pose, consumer, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, color);
		addQuad(pose, consumer, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
		addQuad(pose, consumer, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, minX, maxY, maxZ, color);
		addQuad(pose, consumer, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, color);
		addQuad(pose, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
	}

	private static void addQuad (PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color) {
		addVertex(pose, consumer, x1, y1, z1, color);
		addVertex(pose, consumer, x2, y2, z2, color);
		addVertex(pose, consumer, x3, y3, z3, color);
		addVertex(pose, consumer, x4, y4, z4, color);
	}

	private static void addVertex (PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, int color) {
		consumer.addVertex(pose, x, y, z).setColor(color);
	}

}
