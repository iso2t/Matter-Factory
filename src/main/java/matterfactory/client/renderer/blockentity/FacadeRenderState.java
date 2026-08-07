package matterfactory.client.renderer.blockentity;

import net.minecraft.client.renderer.block.BlockModelRenderState;

public class FacadeRenderState extends CableModeRenderState {

	final BlockModelRenderState model = new BlockModelRenderState();
	final BlockModelRenderState coveredModel = new BlockModelRenderState();
	boolean revealingCable;
	boolean showUnpaintedFacade;

}
