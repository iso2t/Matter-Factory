package matterfactory.common.block.entity;

import matterfactory.core.Tier;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.PowerCable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PowerCableBlockEntity extends BaseBlockEntity {

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

		CableNetwork network = CableNetwork.discover(level, pos);
		if (!network.controller().equals(pos)) {
			return;
		}

		network.distribute();
	}

	private static Tier getTier (BlockState state) {
		return state.getBlock() instanceof PowerCable powerCable ? powerCable.getTier() : Tier.BASIC;
	}

	private int getTransferRate () {
		return getTier(getBlockState()).getEnergyTransferRate();
	}

	private record CableNetwork(List<PowerCableBlockEntity> cables, List<EnergyEndpoint> sources, List<EnergyEndpoint> sinks, BlockPos controller, int transferLimit) {

		private static CableNetwork discover (Level level, BlockPos origin) {
			return discover(level, origin, null);
		}

		private static CableNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
			Set<BlockPos> visited = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			List<PowerCableBlockEntity> cables = new ArrayList<>();
			List<EnergyEndpoint> endpoints = new ArrayList<>();

			visited.add(origin.immutable());
			queue.add(origin.immutable());

			while (!queue.isEmpty()) {
				BlockPos cablePos = queue.removeFirst();
				BlockState cableState = level.getBlockState(cablePos);
				if (!(cableState.getBlock() instanceof PowerCable) || !(level.getBlockEntity(cablePos) instanceof PowerCableBlockEntity cable)) {
					continue;
				}

				cables.add(cable);

				for (Direction direction : Direction.values()) {
					if (!cableState.getValue(CableBlock.getConnectionProperty(direction))) {
						continue;
					}

					BlockPos neighborPos = cablePos.relative(direction);
					BlockState neighborState = level.getBlockState(neighborPos);
					if (neighborState.getBlock() instanceof PowerCable) {
						BlockPos immutableNeighbor = neighborPos.immutable();
						if (visited.add(immutableNeighbor)) {
							queue.add(immutableNeighbor);
						}
					} else {
						getEndpoint(level, neighborPos, neighborState, direction.getOpposite()).ifPresent(endpoints::add);
					}
				}
			}

			List<EnergyEndpoint> sources = new ArrayList<>();
			List<EnergyEndpoint> sinks = new ArrayList<>();
			for (EnergyEndpoint endpoint : endpoints) {
				boolean canExtract = simulateExtract(endpoint.handler(), 1, transaction) > 0;
				boolean canInsert = simulateInsert(endpoint.handler(), 1, transaction) > 0;

				if (canInsert) {
					sinks.add(endpoint);
				} else if (canExtract) {
					sources.add(endpoint);
				}
			}

			BlockPos controller = cables.stream()
					.map(PowerCableBlockEntity::getBlockPos)
					.min(Comparator.comparingLong(BlockPos::asLong))
					.orElse(origin)
					.immutable();
			int transferLimit = cables.stream().mapToInt(PowerCableBlockEntity::getTransferRate).reduce(0, PowerCableBlockEntity::saturatingAdd);

			return new CableNetwork(cables, sources, sinks, controller, transferLimit);
		}

		private static Optional<EnergyEndpoint> getEndpoint (Level level, BlockPos pos, BlockState state, Direction side) {
			EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, state, level.getBlockEntity(pos), side);
			return handler == null ? Optional.empty() : Optional.of(new EnergyEndpoint(pos.immutable(), side, handler));
		}

		private void distribute () {
			moveBetweenEndpoints(sources, sinks, transferLimit);
		}

		private int insertFromCable (PowerCableBlockEntity cable, @Nullable Direction direction, int amount, TransactionContext transaction) {
			return insertFromCable(cable, direction, amount, transaction, false);
		}

		private int insertFromCable (PowerCableBlockEntity cable, @Nullable Direction direction, int amount, TransactionContext transaction, boolean simulate) {
			if (amount <= 0) {
				return 0;
			}

			EnergyEndpoint excludedSource = getExcludedSource(cable, direction);
			int moved = 0;
			for (EnergyEndpoint sink : sinks) {
				if (moved >= amount) {
					return moved;
				}

				if (sink.isSameConnection(excludedSource)) {
					continue;
				}

				moved += insertEnergy(sink.handler(), amount - moved, transaction, simulate);
			}

			return moved;
		}

		private EnergyEndpoint getExcludedSource (PowerCableBlockEntity cable, @Nullable Direction direction) {
			if (direction == null) {
				return null;
			}

			BlockPos sourcePos = cable.getBlockPos().relative(direction);
			return new EnergyEndpoint(sourcePos.immutable(), direction.getOpposite(), null);
		}

		private int moveBetweenEndpoints (List<EnergyEndpoint> sources, List<EnergyEndpoint> sinks, int maxTransfer) {
			int moved = 0;

			for (EnergyEndpoint source : sources) {
				for (EnergyEndpoint sink : sinks) {
					if (moved >= maxTransfer) {
						return moved;
					}

					if (sink.isSameConnection(source)) {
						continue;
					}

					moved += moveEnergy(source.handler(), sink.handler(), maxTransfer - moved);
				}
			}

			return moved;
		}

	}

	private record EnergyEndpoint(BlockPos pos, Direction side, @Nullable EnergyHandler handler) {

		private boolean isSameConnection (@Nullable EnergyEndpoint other) {
			return other != null && pos.equals(other.pos()) && side == other.side();
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
		public int insert (int amount, TransactionContext transaction) {
			if (cable.getLevel() == null || cable.getLevel().isClientSide()) {
				return 0;
			}

			CableNetwork network = CableNetwork.discover(cable.getLevel(), cable.getBlockPos(), transaction);
			return network.insertFromCable(cable, direction, Math.min(amount, cable.getTransferRate()), transaction);
		}

		@Override
		public int extract (int amount, TransactionContext transaction) {
			return 0;
		}
	}

	private static int moveEnergy (EnergyHandler source, EnergyHandler sink, int maxAmount) {
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

	private static int simulateExtract (EnergyHandler handler, int amount, @Nullable TransactionContext transaction) {
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

	private static int simulateInsert (EnergyHandler handler, int amount, @Nullable TransactionContext transaction) {
		if (amount <= 0) {
			return 0;
		}

		try (Transaction simulation = Transaction.open(transaction)) {
			return handler.insert(amount, simulation);
		}
	}

	private static int insertEnergy (EnergyHandler handler, int amount, TransactionContext transaction, boolean simulate) {
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

	private static int saturatingAdd (int left, int right) {
		long result = (long) left + right;
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}

}
