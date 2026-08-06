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
		state.geometry = blockState.getBlock() instanceof CableBlock cableBlock ? cableBlock.getRenderGeometry() : CableBlock.CableRenderGeometry.POWER_CABLE;
		for (Direction direction : Direction.values()) {
			state.modes[direction.ordinal()] = blockEntity.getConnectionMode(direction);
			state.endpoints[direction.ordinal()] = level != null && blockEntity.isEndpointConnection(level, blockEntity.getBlockPos(), blockState, direction);
		}

		extractVisualItem(blockEntity, state, partialTick, level);
	}

	@Override
	public void submit (@NonNull CableModeRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
		for (Direction direction : Direction.values()) {
			CableConnectionMode mode = state.modes[direction.ordinal()];
			if (mode == CableConnectionMode.AUTO || !state.endpoints[direction.ordinal()]) {
				continue;
			}

			int color = mode == CableConnectionMode.IMPORT ? IMPORT_COLOR : EXPORT_COLOR;
			collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> renderModeBand(pose, consumer, direction, state.geometry, color));
		}

		if (!state.visualItem.isEmpty()) {
			renderVisualItem(state, poseStack, collector, cameraState);
		}
	}

	private void extractVisualItem (T blockEntity, CableModeRenderState state, float partialTick, Level level) {
		if (!(blockEntity instanceof ItemPipeBlockEntity itemPipe) || itemPipe.getVisualItem().isEmpty()) {
			state.visualItem.clear();
			state.visualItemCount = 0;
			state.visualProgress = 1.0F;
			return;
		}

		float duration = Math.max(1, itemPipe.getVisualDuration());
		float elapsedTicks = (level == null ? 0 : level.getGameTime()) + partialTick - itemPipe.getVisualStartGameTime();
		if (elapsedTicks < 0.0F || elapsedTicks > itemPipe.getVisualTotalDuration()) {
			state.visualItem.clear();
			state.visualItemCount = 0;
			state.visualProgress = 1.0F;
			return;
		}

		state.visualFrom = itemPipe.getVisualFrom();
		state.visualTo = itemPipe.getVisualTo();
		state.visualItemCount = itemPipe.getVisualItem().getCount();
		state.visualElapsedTicks = elapsedTicks;
		state.visualTravelDuration = itemPipe.getVisualDuration();
		state.visualItemSpacing = itemPipe.getVisualItemSpacing();
		state.visualProgress = Mth.clamp(elapsedTicks / duration, 0.0F, 1.0F);
		itemModelResolver.updateForTopItem(state.visualItem, itemPipe.getVisualItem(), ItemDisplayContext.GROUND, level, null, (int) itemPipe.getBlockPos().asLong());
	}

	private static void renderVisualItem (CableModeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		int count = Math.max(1, state.visualItemCount);
		for (int index = 0; index < count; index++) {
			float itemElapsed = state.visualElapsedTicks - index * state.visualItemSpacing;
			if (itemElapsed < 0.0F || itemElapsed > state.visualTravelDuration) {
				continue;
			}

			float itemProgress = Mth.clamp(itemElapsed / Math.max(1, state.visualTravelDuration), 0.0F, 1.0F);
			renderVisualItemAt(state, poseStack, collector, cameraState, itemProgress);
		}
	}

	private static void renderVisualItemAt (CableModeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, float progress) {
		Vec3 position = sharpPipePathPoint(state.visualFrom, state.visualTo, progress);

		poseStack.pushPose();
		poseStack.translate(position.x, position.y, position.z);
		poseStack.mulPose(cameraState.orientation);
		poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
		state.visualItem.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
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
		float minX = geometry.crossMin();
		float minY = geometry.crossMin();
		float minZ = geometry.crossMin();
		float maxX = geometry.crossMax();
		float maxY = geometry.crossMax();
		float maxZ = geometry.crossMax();

		switch (direction) {
			case DOWN -> {
				minY = geometry.bandMin();
				maxY = geometry.bandMax();
			}
			case UP -> {
				minY = 1.0F - geometry.bandMax();
				maxY = 1.0F - geometry.bandMin();
			}
			case NORTH -> {
				minZ = geometry.bandMin();
				maxZ = geometry.bandMax();
			}
			case SOUTH -> {
				minZ = 1.0F - geometry.bandMax();
				maxZ = 1.0F - geometry.bandMin();
			}
			case WEST -> {
				minX = geometry.bandMin();
				maxX = geometry.bandMax();
			}
			case EAST -> {
				minX = 1.0F - geometry.bandMax();
				maxX = 1.0F - geometry.bandMin();
			}
		}

		renderBox(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ, color);
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
