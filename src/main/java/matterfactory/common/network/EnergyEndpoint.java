package matterfactory.common.network;

import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;

public record EnergyEndpoint(BlockPos pos, Direction side, CableConnectionMode mode, @Nullable EnergyHandler handler) {

	public boolean isSameConnection (@Nullable EnergyEndpoint other) {
		return other != null && pos.equals(other.pos()) && side == other.side();
	}

}
