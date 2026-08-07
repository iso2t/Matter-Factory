package matterfactory.common.network;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.ItemPipe;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ItemPipeNetwork(List<ItemPipeBlockEntity> pipes, List<ItemEndpoint> sources, List<ItemEndpoint> sinks, BlockPos controller, int transferLimit) {

	public static ItemPipeNetwork discover (Level level, BlockPos origin) {
		return discover(level, origin, null);
	}

	public static ItemPipeNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
		List<ItemPipeBlockEntity> pipes = CableNetwork.discover(level, origin, ItemPipe.class, ItemPipeBlockEntity.class);
		List<ItemEndpoint> sources = new ArrayList<>();
		List<ItemEndpoint> sinks = new ArrayList<>();
		for (ItemPipeBlockEntity pipe : pipes) {
			BlockPos pipePos = pipe.getBlockPos();
			BlockState pipeState = pipe.getBlockState();
			for (Direction direction : Direction.values()) {
				if (!pipeState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighborPos = pipePos.relative(direction);
				BlockState neighborState = level.getBlockState(neighborPos);
				if (!CableNetwork.isCableAt(level, neighborPos, ItemPipe.class, ItemPipeBlockEntity.class)) {
					getEndpoint(level, pipe, direction, neighborPos, neighborState, direction.getOpposite()).ifPresent(endpoint -> {
						if (endpoint.mode() == CableConnectionMode.IMPORT && canExtract(endpoint.handler(), transaction)) {
							sources.add(endpoint);
						} else if (endpoint.mode() == CableConnectionMode.EXPORT) {
							sinks.add(endpoint);
						}
					});
				}
			}
		}

		BlockPos controller = CableNetwork.controller(pipes, origin);
		int transferLimit = CableNetwork.totalTransferRate(pipes, ItemPipeBlockEntity::getTransferRate);

		return new ItemPipeNetwork(pipes, sources, sinks, controller, transferLimit);
	}

	private static Optional<ItemEndpoint> getEndpoint (Level level, ItemPipeBlockEntity pipe, Direction pipeSide, BlockPos pos, BlockState state, Direction side) {
		var handler = getItemHandler(level, pos, state, side);
		return handler == null ? Optional.empty() : Optional.of(new ItemEndpoint(pos.immutable(), side, pipe.getConnectionMode(pipeSide), handler));
	}

	@Nullable
	private static ResourceHandler<ItemResource> getItemHandler (Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
		var blockEntity = level.getBlockEntity(pos);
		var sidedHandler = level.getCapability(Capabilities.Item.BLOCK, pos, state, blockEntity, side);
		return sidedHandler != null ? sidedHandler : level.getCapability(Capabilities.Item.BLOCK, pos, state, blockEntity, null);
	}

	private static boolean canExtract (@Nullable ResourceHandler<ItemResource> handler, @Nullable TransactionContext transaction) {
		return handler != null && ResourceHandlerUtil.findExtractableResource(handler, resource -> true, transaction) != null;
	}

	public void distribute () {
		moveBetweenEndpoints(sources, sinks, transferLimit, null);
	}

	public int insertFromPipe (ItemPipeBlockEntity pipe, @Nullable Direction direction, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0) {
			return 0;
		}

		var excludedSource = getExcludedSource(pipe, direction);
		Map<BlockPos, ItemPipeBlockEntity> pipeByPos = CableNetwork.indexPipes(pipes);
		Level level = pipe.getLevel();
		long startGameTime = level == null ? 0 : level.getGameTime();
		int moved = 0;
		for (var sink : sinks) {
			if (moved >= amount) {
				return moved;
			}

			if (sink.isSameConnection(excludedSource)) {
				continue;
			}

			List<BlockPos> path = CableNetwork.findPath(pipe.getBlockPos(), sink.pos().relative(sink.side()), pipeByPos);
			int routeTransferLimit = CableNetwork.routeTransferLimit(path, pipeByPos, ItemPipeBlockEntity::getTransferRate, null);
			if (routeTransferLimit <= 0) {
				continue;
			}

			int segmentDuration = ItemPipeBlockEntity.VISUAL_TRANSFER_DURATION;

			int moveLimit = Math.min(amount - moved, routeTransferLimit);
			int accepted = simulateInsertStacking(sink.handler(), resource, moveLimit, getPendingReservations(sink), transaction);
			if (accepted <= 0) {
				continue;
			}

			ItemPipeBlockEntity arrivalPipe = getArrivalPipe(path, pipeByPos);
			if (arrivalPipe == null) {
				continue;
			}

			arrivalPipe.enqueueItemDelivery(resource, accepted, sink.pos(), sink.side(), startGameTime + (long) path.size() * segmentDuration, ItemPipeBlockEntity.VISUAL_ITEM_SPACING, transaction);
			if (direction != null) {
				ItemEndpoint source = new ItemEndpoint(pipe.getBlockPos().relative(direction), direction.getOpposite(), CableConnectionMode.AUTO, null);
				showTransfer(source, sink, path, pipeByPos, resource, accepted, startGameTime, segmentDuration, transaction);
			}
			moved += accepted;
		}

		return moved;
	}

	private ItemEndpoint getExcludedSource (ItemPipeBlockEntity pipe, @Nullable Direction direction) {
		if (direction == null) {
			return null;
		}

		var sourcePos = pipe.getBlockPos().relative(direction);
		return new ItemEndpoint(sourcePos.immutable(), direction.getOpposite(), CableConnectionMode.AUTO, null);
	}

	private int moveBetweenEndpoints (List<ItemEndpoint> sources, List<ItemEndpoint> sinks, int maxTransfer, @Nullable TransactionContext transaction) {
		Map<BlockPos, ItemPipeBlockEntity> pipeByPos = CableNetwork.indexPipes(pipes);
		Map<BlockPos, Integer> routeBudgets = CableNetwork.routeBudgets(pipes, ItemPipeBlockEntity::getTransferRate);
		int moved = 0;

		for (var source : sources) {
			for (var sink : sinks) {
				if (moved >= maxTransfer) {
					return moved;
				}

				if (sink.isSameConnection(source)) {
					continue;
				}

				List<BlockPos> path = CableNetwork.findPath(source.pos().relative(source.side()), sink.pos().relative(sink.side()), pipeByPos);
				int routeTransferLimit = CableNetwork.routeTransferLimit(path, pipeByPos, ItemPipeBlockEntity::getTransferRate, routeBudgets);
				if (routeTransferLimit <= 0) {
					continue;
				}

				Level level = pipes.isEmpty() ? null : pipes.get(0).getLevel();
				long startGameTime = level == null ? 0 : level.getGameTime();
				int segmentDuration = ItemPipeBlockEntity.VISUAL_TRANSFER_DURATION;

				int moveLimit = Math.min(maxTransfer - moved, routeTransferLimit);
				var movedStack = extractFirstForPendingDelivery(source.handler(), sink.handler(), moveLimit, getPendingReservations(sink), transaction);
				if (movedStack != null) {
					moved += movedStack.amount();
					CableNetwork.consumeRouteBudget(path, routeBudgets, movedStack.amount());
					showTransfer(source, sink, path, pipeByPos, movedStack.resource(), movedStack.amount(), startGameTime, segmentDuration, null);
					enqueueDelivery(sink, path, pipeByPos, movedStack.resource(), movedStack.amount(), startGameTime + (long) path.size() * segmentDuration);
				}
			}
		}

		return moved;
	}

	@Nullable
	private static ResourceStack<ItemResource> extractFirstForPendingDelivery (@Nullable ResourceHandler<ItemResource> source, @Nullable ResourceHandler<ItemResource> sink, int amount, List<ResourceStack<ItemResource>> reservations, @Nullable TransactionContext transaction) {
		if (source == null || sink == null || amount <= 0) {
			return null;
		}

		for (int index = 0; index < source.size(); index++) {
			ItemResource resource = source.getResource(index);
			if (resource.isEmpty()) {
				continue;
			}

			int requested = Math.min(amount, source.getAmountAsInt(index));
			if (requested <= 0) {
				continue;
			}

			int extractable;
			try (Transaction simulation = Transaction.open(transaction)) {
				extractable = source.extract(index, resource, requested, simulation);
			}

			if (extractable <= 0) {
				continue;
			}

			int transferable = Math.min(extractable, simulateInsertStacking(sink, resource, extractable, reservations, transaction));
			if (transferable <= 0) {
				continue;
			}

			try (Transaction transfer = Transaction.open(transaction)) {
				int extracted = source.extract(index, resource, transferable, transfer);
				if (extracted > 0) {
					transfer.commit();
					return new ResourceStack<>(resource, extracted);
				}
			}
		}

		return null;
	}

	private static int simulateInsertStacking (@Nullable ResourceHandler<ItemResource> sink, ItemResource resource, int amount, List<ResourceStack<ItemResource>> reservations, @Nullable TransactionContext transaction) {
		if (sink == null || resource.isEmpty() || amount <= 0) {
			return 0;
		}

		try (Transaction simulation = Transaction.open(transaction)) {
			for (ResourceStack<ItemResource> reservation : reservations) {
				int accepted = ResourceHandlerUtil.insertStacking(sink, reservation.resource(), reservation.amount(), simulation);
				if (accepted < reservation.amount()) {
					return 0;
				}
			}

			return ResourceHandlerUtil.insertStacking(sink, resource, amount, simulation);
		}
	}

	private List<ResourceStack<ItemResource>> getPendingReservations (ItemEndpoint sink) {
		List<ResourceStack<ItemResource>> reservations = new ArrayList<>();
		for (ItemPipeBlockEntity pipe : pipes) {
			reservations.addAll(pipe.getPendingReservations(sink.pos(), sink.side()));
		}

		return reservations;
	}

	private void enqueueDelivery (ItemEndpoint sink, List<BlockPos> path, Map<BlockPos, ItemPipeBlockEntity> pipeByPos, ItemResource resource, int amount, long firstArrivalGameTime) {
		ItemPipeBlockEntity arrivalPipe = getArrivalPipe(path, pipeByPos);
		if (arrivalPipe != null) {
			arrivalPipe.enqueueItemDelivery(resource, amount, sink.pos(), sink.side(), firstArrivalGameTime, ItemPipeBlockEntity.VISUAL_ITEM_SPACING);
		}
	}

	@Nullable
	private static ItemPipeBlockEntity getArrivalPipe (List<BlockPos> path, Map<BlockPos, ItemPipeBlockEntity> pipeByPos) {
		return path.isEmpty() ? null : pipeByPos.get(path.get(path.size() - 1));
	}

	private void showTransfer (ItemEndpoint source, ItemEndpoint sink, List<BlockPos> path, Map<BlockPos, ItemPipeBlockEntity> pipeByPos, ItemResource resource, int amount, long startGameTime, int segmentDuration, @Nullable TransactionContext transaction) {
		if (path.isEmpty()) {
			return;
		}

		int segmentDelay = segmentDuration;

		for (int index = 0; index < path.size(); index++) {
			BlockPos pipePos = path.get(index);
			ItemPipeBlockEntity pipe = pipeByPos.get(pipePos);
			if (pipe == null) {
				continue;
			}

			BlockPos previous = index == 0 ? source.pos() : path.get(index - 1);
			BlockPos next = index == path.size() - 1 ? sink.pos() : path.get(index + 1);
			Direction from = directionBetween(pipePos, previous);
			Direction to = directionBetween(pipePos, next);
			if (from != null && to != null) {
				pipe.showItemTransfer(resource, amount, from, to, startGameTime + (long) index * segmentDelay, segmentDuration, transaction);
			}
		}
	}

	@Nullable
	private static Direction directionBetween (BlockPos from, BlockPos to) {
		for (Direction direction : Direction.values()) {
			if (from.relative(direction).equals(to)) {
				return direction;
			}
		}

		return null;
	}

}
