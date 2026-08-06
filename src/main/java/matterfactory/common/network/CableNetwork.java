package matterfactory.common.network;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.PowerCable;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record CableNetwork(List<PowerCableBlockEntity> cables, List<EnergyEndpoint> sources, List<EnergyEndpoint> sinks, BlockPos controller, int transferLimit) {

	public static CableNetwork discover (Level level, BlockPos origin) {
		return discover(level, origin, null);
	}

	public static CableNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<PowerCableBlockEntity> cables = new ArrayList<>();
		List<EnergyEndpoint> endpoints = new ArrayList<>();

		visited.add(origin.immutable());
		queue.add(origin.immutable());

		while (!queue.isEmpty()) {
			var cablePos = queue.removeFirst();
			var cableState = level.getBlockState(cablePos);
			if (!(cableState.getBlock() instanceof PowerCable) || !(level.getBlockEntity(cablePos) instanceof PowerCableBlockEntity cable)) {
				continue;
			}

			cables.add(cable);

			for (var direction : Direction.values()) {
				if (!cableState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				var neighborPos = cablePos.relative(direction);
				var neighborState = level.getBlockState(neighborPos);
				if (neighborState.getBlock() instanceof PowerCable) {
					var immutableNeighbor = neighborPos.immutable();
					if (visited.add(immutableNeighbor)) {
						queue.add(immutableNeighbor);
					}
				} else {
					getEndpoint(level, cable, direction, neighborPos, neighborState, direction.getOpposite()).ifPresent(endpoints::add);
				}
			}
		}

		List<EnergyEndpoint> sources = new ArrayList<>();
		List<EnergyEndpoint> sinks = new ArrayList<>();
		for (EnergyEndpoint endpoint : endpoints) {
			var canExtract = PowerCableBlockEntity.simulateExtract(endpoint.handler(), 1, transaction) > 0;
			var canInsert = PowerCableBlockEntity.simulateInsert(endpoint.handler(), 1, transaction) > 0;

			if (endpoint.mode() == CableConnectionMode.IMPORT && canExtract) {
				sources.add(endpoint);
			} else if (endpoint.mode() == CableConnectionMode.EXPORT && canInsert) {
				sinks.add(endpoint);
			} else if (endpoint.mode() == CableConnectionMode.AUTO) {
				if (canInsert) {
					sinks.add(endpoint);
				} else if (canExtract) {
					sources.add(endpoint);
				}
			}
		}

		var controller = cables.stream().map(PowerCableBlockEntity::getBlockPos).min(Comparator.comparingLong(BlockPos::asLong)).orElse(origin).immutable();
		var transferLimit = cables.stream().mapToInt(PowerCableBlockEntity::getTransferRate).reduce(0, PowerCableBlockEntity::saturatingAdd);

		return new CableNetwork(cables, sources, sinks, controller, transferLimit);
	}

	private static Optional<EnergyEndpoint> getEndpoint (Level level, PowerCableBlockEntity cable, Direction cableSide, BlockPos pos, BlockState state, Direction side) {
		var handler = level.getCapability(Capabilities.Energy.BLOCK, pos, state, level.getBlockEntity(pos), side);
		return handler == null ? Optional.empty() : Optional.of(new EnergyEndpoint(pos.immutable(), side, cable.getConnectionMode(cableSide), handler));
	}

	public void distribute () {
		moveBetweenEndpoints(sources, sinks, transferLimit);
	}

	public int insertFromCable (PowerCableBlockEntity cable, @Nullable Direction direction, int amount, TransactionContext transaction) {
		return insertFromCable(cable, direction, amount, transaction, false);
	}

	private int insertFromCable (PowerCableBlockEntity cable, @Nullable Direction direction, int amount, TransactionContext transaction, boolean simulate) {
		if (amount <= 0) {
			return 0;
		}

		var excludedSource = getExcludedSource(cable, direction);
		int moved = 0;
		for (var sink : sinks) {
			if (moved >= amount) {
				return moved;
			}

			if (sink.isSameConnection(excludedSource)) {
				continue;
			}

			moved += PowerCableBlockEntity.insertEnergy(sink.handler(), amount - moved, transaction, simulate);
		}

		return moved;
	}

	private EnergyEndpoint getExcludedSource (PowerCableBlockEntity cable, @Nullable Direction direction) {
		if (direction == null) {
			return null;
		}

		var sourcePos = cable.getBlockPos().relative(direction);
		return new EnergyEndpoint(sourcePos.immutable(), direction.getOpposite(), CableConnectionMode.AUTO, null);
	}

	private int moveBetweenEndpoints (List<EnergyEndpoint> sources, List<EnergyEndpoint> sinks, int maxTransfer) {
		int moved = 0;

		for (var source : sources) {
			for (var sink : sinks) {
				if (moved >= maxTransfer) {
					return moved;
				}

				if (sink.isSameConnection(source)) {
					continue;
				}

				moved += PowerCableBlockEntity.moveEnergy(source.handler(), sink.handler(), maxTransfer - moved);
			}
		}

		return moved;
	}

}
