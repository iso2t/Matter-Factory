package matterfactory.client.renderer.blockentity;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class FacadeRenderState extends CableModeRenderState {

	final BlockModelRenderState model = new BlockModelRenderState();
	final BlockModelRenderState coveredModel = new BlockModelRenderState();
	final List<BlockStateModelPart> ghostPaintModel = new ArrayList<>();
	int[] ghostPaintTints = BlockModelRenderState.EMPTY_TINTS;
	boolean revealingCable;
	boolean showGhostFacade;
	boolean showUnpaintedFacade;
	boolean showMaintenanceOutline;
	VoxelShape maintenanceShape = Shapes.empty();

}
