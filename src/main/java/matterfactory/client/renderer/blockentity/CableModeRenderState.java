package matterfactory.client.renderer.blockentity;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class CableModeRenderState extends BlockEntityRenderState {

	final CableConnectionMode[] modes     = new CableConnectionMode[Direction.values().length];
	final boolean[]             endpoints = new boolean[Direction.values().length];
	CableBlock.CableRenderGeometry geometry = CableBlock.CableRenderGeometry.POWER_CABLE;
	final List<VisualItemRenderState> visualItems = new ArrayList<>();

	CableModeRenderState () {
		for (Direction direction : Direction.values()) {
			modes[direction.ordinal()] = CableConnectionMode.AUTO;
		}
	}

	static final class VisualItemRenderState {

		final ItemStackRenderState item = new ItemStackRenderState();
		int       itemCount;
		float     elapsedTicks;
		int       travelDuration;
		int       itemSpacing;
		Direction from = Direction.NORTH;
		Direction to   = Direction.SOUTH;
	}

}
