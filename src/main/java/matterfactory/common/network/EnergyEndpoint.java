package matterfactory.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;

public record EnergyEndpoint(BlockPos pos, Direction side, @Nullable EnergyHandler handler) {

	public boolean isSameConnection (@Nullable EnergyEndpoint other) {
		return other != null && pos.equals(other.pos()) && side == other.side();
	}

}
