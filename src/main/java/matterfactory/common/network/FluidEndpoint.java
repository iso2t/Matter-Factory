package matterfactory.common.network;

import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

public record FluidEndpoint(BlockPos pos, Direction side, CableConnectionMode mode, @Nullable ResourceHandler<FluidResource> handler) {

	public boolean isSameConnection (@Nullable FluidEndpoint other) {
		return other != null && pos.equals(other.pos()) && side == other.side();
	}

}
