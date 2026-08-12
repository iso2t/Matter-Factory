package matterfactory.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.item.tool.WrenchItem;
import matterfactory.core.Factory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
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
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class FacadeRenderer implements BlockEntityRenderer<FacadeBlockEntity, FacadeRenderState> {

	private static final Identifier FACADE_TEXTURE = Factory.get("textures/block/facade.png");
	private static final int        GHOST_COLOR    = 0x66FFFFFF;

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
		state.showGhostFacade = state.revealingCable;
		extractGhostPaint(state, level, facade.getBlockPos(), paintedState);
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
		BlockHitResult targetedHit = Minecraft.getInstance().hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(facade.getBlockPos()) ? hit : null;
		state.showMaintenanceOutline = state.revealingCable && targetedHit != null;
		state.maintenanceShape = net.minecraft.world.phys.shapes.Shapes.empty();
		if (cable != null && level != null) {
			CableModeRenderer.extractConnectionModes(state, cable, level, coveredState);
			if (state.showMaintenanceOutline) {
				var player = Minecraft.getInstance().player;
				state.maintenanceShape = CableBlock.getWrenchOutlineShape(coveredState, facade.getBlockPos(), targetedHit, player != null && player.isShiftKeyDown());
			}
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
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(FACADE_TEXTURE, false), (pose, consumer) -> renderUnpaintedFacade(pose, consumer, state.lightCoords, 0xFFFFFFFF));
		}
		CableModeRenderer.submitVisualItems(state, poseStack, collector, cameraState);
		if (state.showGhostFacade) {
			if (state.ghostPaintModel.isEmpty()) {
				collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(FACADE_TEXTURE, false), (pose, consumer) -> renderUnpaintedFacade(pose, consumer, state.lightCoords, GHOST_COLOR));
			} else {
				collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (pose, consumer) -> renderGhostPaint(pose, consumer, state));
			}
		}
		if (!state.coveredModel.isEmpty()) {
			CableModeRenderer.submitConnectionModeBands(state, poseStack, collector);
		}
		if (state.showMaintenanceOutline) {
			collector.submitShapeOutline(poseStack, state.maintenanceShape, RenderTypes.lines(), 0xFF000000, 1.0F, true);
		}
	}

	private static void extractGhostPaint (FacadeRenderState state, Level level, BlockPos pos, BlockState paintedState) {
		state.ghostPaintModel.clear();
		state.ghostPaintTints = new int[0];
		if (!state.showGhostFacade || paintedState.isAir()) {
			return;
		}

		BlockAndTintGetter tintGetter = level instanceof BlockAndTintGetter world ? world : BlockAndTintGetter.EMPTY;
		Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(paintedState).collectParts(tintGetter, pos, paintedState, RandomSource.create(pos.asLong()), state.ghostPaintModel);
		List<net.minecraft.client.color.block.BlockTintSource> tintSources = Minecraft.getInstance().getBlockColors().getTintSources(paintedState);
		if (!tintSources.isEmpty()) {
			state.ghostPaintTints = new int[tintSources.size()];
			for (int index = 0; index < tintSources.size(); index++) {
				state.ghostPaintTints[index] = tintSources.get(index).colorInWorld(paintedState, tintGetter, pos);
			}
		}
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

	private static void renderGhostPaint (PoseStack.Pose pose, VertexConsumer consumer, FacadeRenderState state) {
		QuadInstance quad = new QuadInstance();
		quad.setLightCoords(state.lightCoords);
		quad.setOverlayCoords(OverlayTexture.NO_OVERLAY);
		for (BlockStateModelPart part : state.ghostPaintModel) {
			for (Direction direction : Direction.values()) {
				renderGhostQuads(pose, consumer, quad, part.getQuads(direction), state.ghostPaintTints);
			}
			renderGhostQuads(pose, consumer, quad, part.getQuads(null), state.ghostPaintTints);
		}
	}

	private static void renderGhostQuads (PoseStack.Pose pose, VertexConsumer consumer, QuadInstance instance, List<BakedQuad> quads, int[] tintLayers) {
		for (BakedQuad quad : quads) {
			int tintIndex = quad.materialInfo().tintIndex();
			int tint = tintIndex >= 0 && tintIndex < tintLayers.length ? tintLayers[tintIndex] : 0xFFFFFFFF;
			instance.setColor(ARGB.multiply(GHOST_COLOR, tint));
			consumer.putBakedQuad(pose, quad, instance);
		}
	}

	private static void renderUnpaintedFacade (PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, int color) {
		renderFace(pose, consumer, lightCoords, color, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, -1);
		renderFace(pose, consumer, lightCoords, color, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1);
		renderFace(pose, consumer, lightCoords, color, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, -1, 0, 0);
		renderFace(pose, consumer, lightCoords, color, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0);
		renderFace(pose, consumer, lightCoords, color, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0);
		renderFace(pose, consumer, lightCoords, color, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, -1, 0);
	}

	private static void renderFace (PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, int color, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float normalX, float normalY, float normalZ) {
		addVertex(pose, consumer, lightCoords, color, x1, y1, z1, 0, 1, normalX, normalY, normalZ);
		addVertex(pose, consumer, lightCoords, color, x2, y2, z2, 1, 1, normalX, normalY, normalZ);
		addVertex(pose, consumer, lightCoords, color, x3, y3, z3, 1, 0, normalX, normalY, normalZ);
		addVertex(pose, consumer, lightCoords, color, x4, y4, z4, 0, 0, normalX, normalY, normalZ);
	}

	private static void addVertex (PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, int color, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ) {
		consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, normalX, normalY, normalZ);
	}

}
