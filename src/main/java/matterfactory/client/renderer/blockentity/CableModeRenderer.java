package matterfactory.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class CableModeRenderer<T extends BaseCableBlockEntity> implements BlockEntityRenderer<T, CableModeRenderState> {

	private static final int IMPORT_COLOR = 0xFF186EFF;
	private static final int EXPORT_COLOR = 0xFFFF7418;

	public CableModeRenderer (BlockEntityRendererProvider.Context context) {
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
