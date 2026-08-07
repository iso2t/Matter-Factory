package matterfactory.common.block.entity;

import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.PowerCable;
import matterfactory.common.network.EnergyCableNetwork;
import matterfactory.core.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class PowerCableBlockEntity extends BaseCableBlockEntity {

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

		EnergyCableNetwork network = EnergyCableNetwork.discover(level, pos);
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

	@Override
	protected boolean isNetworkCable (BlockState state) {
		return state.getBlock() instanceof PowerCable;
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

			EnergyCableNetwork network = EnergyCableNetwork.discover(cable.getLevel(), cable.getBlockPos(), transaction);
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

}
