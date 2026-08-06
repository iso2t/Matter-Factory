package matterfactory.common.block.entity;

import matterfactory.core.Tier;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.PowerCable;
import matterfactory.common.network.CableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.EnumSet;

public class PowerCableBlockEntity extends BaseBlockEntity {

	private static final DustParticleOptions IMPORT_PARTICLE = new DustParticleOptions(0x186EFF, 1.0F);
	private static final DustParticleOptions EXPORT_PARTICLE = new DustParticleOptions(0xFF7418, 1.0F);

	private final EnumSet<Direction> disconnectedSides = EnumSet.noneOf(Direction.class);
	private final EnumMap<Direction, CableConnectionMode> connectionModes = new EnumMap<>(Direction.class);

	public PowerCableBlockEntity (BlockEntityType<PowerCableBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Nullable
	public EnergyHandler getEnergyHandler (@Nullable Direction direction) {
		return new CableEnergyHandler(this, direction);
	}

	public static void serverTick (Level level, BlockPos pos, BlockState state, PowerCableBlockEntity blockEntity) {
		if (level.isClientSide()) {
			return;
		}

		if (level instanceof ServerLevel serverLevel && level.getGameTime() % 10 == 0) {
			blockEntity.emitModeParticles(serverLevel, pos, state);
		}

		CableNetwork network = CableNetwork.discover(level, pos);
		if (!network.controller().equals(pos)) {
			return;
		}

		network.distribute();
	}

	private static Tier getTier (BlockState state) {
		return state.getBlock() instanceof PowerCable powerCable ? powerCable.getTier() : Tier.BASIC;
	}

	public int getTransferRate () {
		return getTier(getBlockState()).getEnergyTransferRate();
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
		setChanged();
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
		setChanged();
	}

	private void emitModeParticles (ServerLevel level, BlockPos pos, BlockState state) {
		for (Direction direction : Direction.values()) {
			CableConnectionMode mode = getConnectionMode(direction);
			if (mode == CableConnectionMode.AUTO || !isEndpointConnection(level, pos, state, direction)) {
				continue;
			}

			double x = pos.getX() + 0.5 + direction.getStepX() * 0.42;
			double y = pos.getY() + 0.5 + direction.getStepY() * 0.42;
			double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.42;
			level.sendParticles(mode == CableConnectionMode.IMPORT ? IMPORT_PARTICLE : EXPORT_PARTICLE, x, y, z, 1, 0.03, 0.03, 0.03, 0.0);
		}
	}

	private boolean isEndpointConnection (Level level, BlockPos pos, BlockState state, Direction direction) {
		return state.getValue(CableBlock.getConnectionProperty(direction)) && !(level.getBlockState(pos.relative(direction)).getBlock() instanceof PowerCable) && !isManuallyDisconnected(direction);
	}

	@Override
	protected void loadAdditional (ValueInput input) {
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
	protected void saveAdditional (ValueOutput output) {
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

	private record CableEnergyHandler(PowerCableBlockEntity cable, @Nullable Direction direction) implements EnergyHandler {

		@Override
		public long getAmountAsLong () {
			return 0;
		}

		@Override
		public long getCapacityAsLong () {
			return cable.getTransferRate();
		}

		@Override
		public int insert (int amount, @NonNull TransactionContext transaction) {
			if (cable.getLevel() == null || cable.getLevel().isClientSide()) {
				return 0;
			}

			if (direction != null && cable.getConnectionMode(direction) == CableConnectionMode.EXPORT) {
				return 0;
			}

			CableNetwork network = CableNetwork.discover(cable.getLevel(), cable.getBlockPos(), transaction);
			return network.insertFromCable(cable, direction, Math.min(amount, cable.getTransferRate()), transaction);
		}

		@Override
		public int extract (int amount, @NonNull TransactionContext transaction) {
			return 0;
		}
	}

	public static int moveEnergy (EnergyHandler source, EnergyHandler sink, int maxAmount) {
		if (maxAmount <= 0) {
			return 0;
		}

		int extractable = simulateExtract(source, maxAmount);
		if (extractable <= 0) {
			return 0;
		}

		int amount = Math.min(extractable, simulateInsert(sink, extractable));
		if (amount <= 0) {
			return 0;
		}

		try (Transaction transaction = Transaction.openRoot()) {
			int extracted = source.extract(amount, transaction);
			int inserted = insertEnergy(sink, extracted, transaction, false);
			if (inserted == extracted) {
				transaction.commit();
				return inserted;
			}
		}

		return 0;
	}

	private static int simulateExtract (EnergyHandler handler, int amount) {
		return simulateExtract(handler, amount, null);
	}

	public static int simulateExtract (EnergyHandler handler, int amount, @Nullable TransactionContext transaction) {
		if (amount <= 0) {
			return 0;
		}

		try (Transaction simulation = Transaction.open(transaction)) {
			return handler.extract(amount, simulation);
		}
	}

	private static int simulateInsert (EnergyHandler handler, int amount) {
		return simulateInsert(handler, amount, null);
	}

	public static int simulateInsert (EnergyHandler handler, int amount, @Nullable TransactionContext transaction) {
		if (amount <= 0) {
			return 0;
		}

		try (Transaction simulation = Transaction.open(transaction)) {
			return handler.insert(amount, simulation);
		}
	}

	public static int insertEnergy (EnergyHandler handler, int amount, TransactionContext transaction, boolean simulate) {
		if (amount <= 0) {
			return 0;
		}

		if (simulate) {
			try (Transaction simulation = Transaction.open(transaction)) {
				return handler.insert(amount, simulation);
			}
		}

		return handler.insert(amount, transaction);
	}

	public static int saturatingAdd (int left, int right) {
		long result = (long) left + right;
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}

}
