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
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record ItemPipeNetwork(List<ItemPipeBlockEntity> pipes, List<ItemEndpoint> sources, List<ItemEndpoint> sinks, BlockPos controller, int transferLimit) {

	public static ItemPipeNetwork discover (Level level, BlockPos origin) {
		return discover(level, origin, null);
	}

	public static ItemPipeNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<ItemPipeBlockEntity> pipes = new ArrayList<>();
		List<ItemEndpoint> sources = new ArrayList<>();
		List<ItemEndpoint> sinks = new ArrayList<>();

		visited.add(origin.immutable());
		queue.add(origin.immutable());

		while (!queue.isEmpty()) {
			var pipePos = queue.removeFirst();
			var pipeState = level.getBlockState(pipePos);
			if (!(pipeState.getBlock() instanceof ItemPipe) || !(level.getBlockEntity(pipePos) instanceof ItemPipeBlockEntity pipe)) {
				continue;
			}

			pipes.add(pipe);

			for (var direction : Direction.values()) {
				if (!pipeState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				var neighborPos = pipePos.relative(direction);
				var neighborState = level.getBlockState(neighborPos);
				if (neighborState.getBlock() instanceof ItemPipe) {
					var immutableNeighbor = neighborPos.immutable();
					if (visited.add(immutableNeighbor)) {
						queue.add(immutableNeighbor);
					}
				} else {
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

		var controller = pipes.stream().map(ItemPipeBlockEntity::getBlockPos).min(Comparator.comparingLong(BlockPos::asLong)).orElse(origin).immutable();
		var transferLimit = pipes.stream().mapToInt(ItemPipeBlockEntity::getTransferRate).reduce(0, ItemPipeBlockEntity::saturatingAdd);

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
		int moved = 0;
		for (var sink : sinks) {
			if (moved >= amount) {
				return moved;
			}

			if (sink.isSameConnection(excludedSource)) {
				continue;
			}

			moved += ResourceHandlerUtil.insertStacking(sink.handler(), resource, amount - moved, transaction);
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
		int moved = 0;

		for (var source : sources) {
			for (var sink : sinks) {
				if (moved >= maxTransfer) {
					return moved;
				}

				if (sink.isSameConnection(source)) {
					continue;
				}

				var movedStack = ResourceHandlerUtil.moveFirstStacking(source.handler(), sink.handler(), resource -> true, maxTransfer - moved, transaction);
				if (movedStack != null) {
					moved += movedStack.amount();
					showTransfer(source, sink, movedStack.resource(), movedStack.amount());
				}
			}
		}

		return moved;
	}

	private void showTransfer (ItemEndpoint source, ItemEndpoint sink, ItemResource resource, int amount) {
		Map<BlockPos, ItemPipeBlockEntity> pipeByPos = new HashMap<>();
		for (ItemPipeBlockEntity pipe : pipes) {
			pipeByPos.put(pipe.getBlockPos(), pipe);
		}

		BlockPos startPipe = source.pos().relative(source.side());
		BlockPos endPipe = sink.pos().relative(sink.side());
		List<BlockPos> path = findPipePath(startPipe, endPipe, pipeByPos);
		if (path.isEmpty()) {
			return;
		}

		Level level = pipes.isEmpty() ? null : pipes.get(0).getLevel();
		long startGameTime = level == null ? 0 : level.getGameTime();
		int segmentDuration = ItemPipeBlockEntity.VISUAL_TRANSFER_DURATION;
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
				pipe.showItemTransfer(resource, amount, from, to, startGameTime + (long) index * segmentDelay, segmentDuration);
			}
		}
	}

	private static List<BlockPos> findPipePath (BlockPos start, BlockPos end, Map<BlockPos, ItemPipeBlockEntity> pipeByPos) {
		if (!pipeByPos.containsKey(start) || !pipeByPos.containsKey(end)) {
			return List.of();
		}

		Map<BlockPos, BlockPos> previous = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();

		visited.add(start);
		queue.add(start);

		while (!queue.isEmpty()) {
			BlockPos current = queue.removeFirst();
			if (current.equals(end)) {
				return buildPath(end, previous);
			}

			ItemPipeBlockEntity pipe = pipeByPos.get(current);
			BlockState state = pipe.getBlockState();
			for (Direction direction : Direction.values()) {
				if (!state.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighbor = current.relative(direction);
				ItemPipeBlockEntity neighborPipe = pipeByPos.get(neighbor);
				if (neighborPipe == null || !neighborPipe.getBlockState().getValue(CableBlock.getConnectionProperty(direction.getOpposite()))) {
					continue;
				}

				if (visited.add(neighbor)) {
					previous.put(neighbor, current);
					queue.add(neighbor);
				}
			}
		}

		return List.of();
	}

	private static List<BlockPos> buildPath (BlockPos end, Map<BlockPos, BlockPos> previous) {
		LinkedList<BlockPos> path = new LinkedList<>();
		for (BlockPos at = end; at != null; at = previous.get(at)) {
			path.addFirst(at);
		}
		return path;
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
