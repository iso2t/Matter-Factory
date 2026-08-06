package matterfactory.common.network;

import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public record ItemEndpoint(BlockPos pos, Direction side, CableConnectionMode mode, @Nullable ResourceHandler<ItemResource> handler) {

	public boolean isSameConnection (@Nullable ItemEndpoint other) {
		return other != null && pos.equals(other.pos()) && side == other.side();
	}

}
