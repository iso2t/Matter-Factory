package matterfactory.client.renderer.blockentity;

import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class PowerCableRenderState extends BlockEntityRenderState {

	final CableConnectionMode[] modes = new CableConnectionMode[Direction.values().length];
	final boolean[] endpoints = new boolean[Direction.values().length];

	PowerCableRenderState () {
		for (Direction direction : Direction.values()) {
			modes[direction.ordinal()] = CableConnectionMode.AUTO;
		}
	}

}
