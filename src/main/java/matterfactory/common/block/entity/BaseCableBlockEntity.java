package matterfactory.common.block.entity;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.EnumSet;

public abstract class BaseCableBlockEntity extends BaseBlockEntity {

	private final EnumSet<Direction> disconnectedSides = EnumSet.noneOf(Direction.class);
	private final EnumMap<Direction, CableConnectionMode> connectionModes = new EnumMap<>(Direction.class);

	public BaseCableBlockEntity (BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean isManuallyDisconnected (Direction direction) {
		return disconnectedSides.contains(direction);
	}

	public void setManuallyDisconnected (Direction direction, boolean disconnected) {
		if (disconnected) {
			disconnectedSides.add(direction);
		} else {
			disconnectedSides.remove(direction);
		}

		markConnectionDataChanged();
	}

	public CableConnectionMode getConnectionMode (Direction direction) {
		return connectionModes.getOrDefault(direction, CableConnectionMode.AUTO);
	}

	public void setConnectionMode (Direction direction, CableConnectionMode mode) {
		if (mode == CableConnectionMode.AUTO) {
			connectionModes.remove(direction);
		} else {
			connectionModes.put(direction, mode);
		}

		markConnectionDataChanged();
	}

	public boolean isEndpointConnection (Level level, BlockPos pos, BlockState state, Direction direction) {
		return state.getValue(CableBlock.getConnectionProperty(direction)) && !isNetworkCable(level.getBlockState(pos.relative(direction))) && !isManuallyDisconnected(direction);
	}

	protected abstract boolean isNetworkCable (BlockState state);

	protected void markConnectionDataChanged () {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket () {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public @NonNull CompoundTag getUpdateTag (HolderLookup.@NonNull Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void loadAdditional (@NonNull ValueInput input) {
		super.loadAdditional(input);
		disconnectedSides.clear();
		connectionModes.clear();

		for (Direction direction : Direction.values()) {
			String suffix = direction.getSerializedName();
			if (input.getBooleanOr("disconnected_" + suffix, false)) {
				disconnectedSides.add(direction);
			}

			CableConnectionMode mode = CableConnectionMode.byName(input.getStringOr("mode_" + suffix, CableConnectionMode.AUTO.getSerializedName()));
			if (mode != CableConnectionMode.AUTO) {
				connectionModes.put(direction, mode);
			}
		}
	}

	@Override
	protected void saveAdditional (@NonNull ValueOutput output) {
		super.saveAdditional(output);

		for (Direction direction : Direction.values()) {
			String suffix = direction.getSerializedName();
			if (isManuallyDisconnected(direction)) {
				output.putBoolean("disconnected_" + suffix, true);
			}

			CableConnectionMode mode = getConnectionMode(direction);
			if (mode != CableConnectionMode.AUTO) {
				output.putString("mode_" + suffix, mode.getSerializedName());
			}
		}
	}

}
