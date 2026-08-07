package matterfactory.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.item.tool.WrenchItem;
import matterfactory.core.Factory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class FacadeRenderer implements BlockEntityRenderer<FacadeBlockEntity, FacadeRenderState> {

	private static final Identifier FACADE_TEXTURE = Factory.get("textures/block/facade.png");

	private final BlockModelResolver blockModelResolver;
	private final ItemModelResolver  itemModelResolver;

	public FacadeRenderer (BlockEntityRendererProvider.Context context) {
		blockModelResolver = context.blockModelResolver();
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public @NonNull FacadeRenderState createRenderState () {
		return new FacadeRenderState();
	}

	@Override
	public void extractRenderState (@NonNull FacadeBlockEntity facade, @NonNull FacadeRenderState state, float partialTick, @NonNull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(facade, state, partialTick, cameraPos, crumblingOverlay);
		Level level = facade.getLevel();
		if (level != null) {
			state.lightCoords = getFacadeLight(level, facade.getBlockPos());
		}

		ItemStack heldItem = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
		state.revealingCable = heldItem.getItem() instanceof WrenchItem;
		BlockState coveredState = facade.getCoveredState();
		BlockState paintedState = facade.getPaintedState();
		state.showUnpaintedFacade = !state.revealingCable && paintedState.isAir();
		if (state.revealingCable || isTransparentPaint(paintedState)) {
			blockModelResolver.update(state.coveredModel, coveredState, BlockDisplayContext.create());
		} else {
			state.coveredModel.clear();
		}

		if (state.showUnpaintedFacade) {
			state.model.clear();
		} else if (state.revealingCable) {
			state.model.clear();
		} else {
			blockModelResolver.update(state.model, paintedState, BlockDisplayContext.create());
		}

		BaseCableBlockEntity cable = facade.getCoveredCable(BaseCableBlockEntity.class);
		if (cable != null && level != null) {
			CableModeRenderer.extractConnectionModes(state, cable, level, coveredState);
			if (isTransparentPaint(paintedState)) {
				CableModeRenderer.extractVisualItems(itemModelResolver, state, cable, partialTick, level);
			} else {
				state.visualItems.clear();
			}
		} else {
			state.visualItems.clear();
		}
	}

	@Override
	public void submit (@NonNull FacadeRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
		state.coveredModel.submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		state.model.submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		if (state.showUnpaintedFacade) {
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(FACADE_TEXTURE, false), (pose, consumer) -> renderUnpaintedFacade(pose, consumer, state.lightCoords));
		}
		if (!state.coveredModel.isEmpty()) {
			CableModeRenderer.submitConnectionModeBands(state, poseStack, collector);
		}
		CableModeRenderer.submitVisualItems(state, poseStack, collector, cameraState);
	}

	private static boolean isTransparentPaint (BlockState paintedState) {
		return !paintedState.isAir() && !paintedState.isSolidRender();
	}

	private static int getFacadeLight (Level level, BlockPos pos) {
		int light = LightCoordsUtil.getLightCoords(level, pos);
		for (Direction direction : Direction.values()) {
			light = LightCoordsUtil.max(light, LightCoordsUtil.getLightCoords(level, pos.relative(direction)));
		}
		return light;
	}

	private static void renderUnpaintedFacade (PoseStack.Pose pose, VertexConsumer consumer, int lightCoords) {
		renderFace(pose, consumer, lightCoords, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, -1);
		renderFace(pose, consumer, lightCoords, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1);
		renderFace(pose, consumer, lightCoords, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, -1, 0, 0);
		renderFace(pose, consumer, lightCoords, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0);
		renderFace(pose, consumer, lightCoords, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0);
		renderFace(pose, consumer, lightCoords, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, -1, 0);
	}

	private static void renderFace (PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float normalX, float normalY, float normalZ) {
		addVertex(pose, consumer, lightCoords, x1, y1, z1, 0, 1, normalX, normalY, normalZ);
		addVertex(pose, consumer, lightCoords, x2, y2, z2, 1, 1, normalX, normalY, normalZ);
		addVertex(pose, consumer, lightCoords, x3, y3, z3, 1, 0, normalX, normalY, normalZ);
		addVertex(pose, consumer, lightCoords, x4, y4, z4, 0, 0, normalX, normalY, normalZ);
	}

	private static void addVertex (PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ) {
		consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, normalX, normalY, normalZ);
	}

}
