package matterfactory.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.item.tool.WrenchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class FacadeRenderer implements BlockEntityRenderer<FacadeBlockEntity, FacadeRenderState> {

	private final BlockModelResolver blockModelResolver;

	public FacadeRenderer (BlockEntityRendererProvider.Context context) {
		blockModelResolver = context.blockModelResolver();
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
		blockModelResolver.update(state.model, state.revealingCable ? coveredState : facade.getPaintedState(), BlockDisplayContext.create());

		BaseCableBlockEntity cable = facade.getCoveredCable(BaseCableBlockEntity.class);
		if (cable != null && level != null) {
			CableModeRenderer.extractConnectionModes(state, cable, level, coveredState);
		}
	}

	@Override
	public void submit (@NonNull FacadeRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
		state.model.submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		if (state.revealingCable) {
			CableModeRenderer.submitConnectionModeBands(state, poseStack, collector);
		}
	}

	private static int getFacadeLight (Level level, BlockPos pos) {
		int light = LightCoordsUtil.getLightCoords(level, pos);
		for (Direction direction : Direction.values()) {
			light = LightCoordsUtil.max(light, LightCoordsUtil.getLightCoords(level, pos.relative(direction)));
		}
		return light;
	}

}
