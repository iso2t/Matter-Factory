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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EnergyCableNetwork(List<PowerCableBlockEntity> cables, List<EnergyEndpoint> sources, List<EnergyEndpoint> sinks, BlockPos controller, int transferLimit) {

	public static EnergyCableNetwork discover (Level level, BlockPos origin) {
		return discover(level, origin, null);
	}

	public static EnergyCableNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
		List<PowerCableBlockEntity> cables = CableNetwork.discover(level, origin, PowerCable.class, PowerCableBlockEntity.class);
		List<EnergyEndpoint> endpoints = new ArrayList<>();
		for (PowerCableBlockEntity cable : cables) {
			BlockPos cablePos = cable.getBlockPos();
			BlockState cableState = cable.getBlockState();
			for (Direction direction : Direction.values()) {
				if (!cableState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighborPos = cablePos.relative(direction);
				BlockState neighborState = level.getBlockState(neighborPos);
				if (!(neighborState.getBlock() instanceof PowerCable)) {
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

		BlockPos controller = CableNetwork.controller(cables, origin);
		int transferLimit = CableNetwork.totalTransferRate(cables, PowerCableBlockEntity::getTransferRate);

		return new EnergyCableNetwork(cables, sources, sinks, controller, transferLimit);
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
