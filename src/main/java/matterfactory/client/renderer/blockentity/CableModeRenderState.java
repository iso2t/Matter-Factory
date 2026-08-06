package matterfactory.client.renderer.blockentity;

import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class CableModeRenderState extends BlockEntityRenderState {

	final CableConnectionMode[] modes = new CableConnectionMode[Direction.values().length];
	final boolean[] endpoints = new boolean[Direction.values().length];

	CableModeRenderState () {
		for (Direction direction : Direction.values()) {
			modes[direction.ordinal()] = CableConnectionMode.AUTO;
		}
	}

}
