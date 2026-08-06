package matterfactory.client.renderer.blockentity;

import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.CableBlock;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class CableModeRenderState extends BlockEntityRenderState {

	final CableConnectionMode[] modes = new CableConnectionMode[Direction.values().length];
	final boolean[] endpoints = new boolean[Direction.values().length];
	CableBlock.CableRenderGeometry geometry = CableBlock.CableRenderGeometry.POWER_CABLE;
	final ItemStackRenderState visualItem = new ItemStackRenderState();
	int visualItemCount;
	float visualElapsedTicks;
	int visualTravelDuration;
	int visualItemSpacing;
	Direction visualFrom = Direction.NORTH;
	Direction visualTo = Direction.SOUTH;
	float visualProgress = 1.0F;

	CableModeRenderState () {
		for (Direction direction : Direction.values()) {
			modes[direction.ordinal()] = CableConnectionMode.AUTO;
		}
	}

}
