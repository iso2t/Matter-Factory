package matterfactory.common.network;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.FluidPipe;
import matterfactory.common.block.entity.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record FluidPipeNetwork(List<FluidPipeBlockEntity> pipes, List<FluidEndpoint> sources, List<FluidEndpoint> sinks, BlockPos controller, int transferLimit) {

	public static FluidPipeNetwork discover (Level level, BlockPos origin) {
		return discover(level, origin, null);
	}

	public static FluidPipeNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<FluidPipeBlockEntity> pipes = new ArrayList<>();
		List<FluidEndpoint> sources = new ArrayList<>();
		List<FluidEndpoint> sinks = new ArrayList<>();

		visited.add(origin.immutable());
		queue.add(origin.immutable());
		while (!queue.isEmpty()) {
			BlockPos pipePos = queue.removeFirst();
			BlockState pipeState = level.getBlockState(pipePos);
			if (!(pipeState.getBlock() instanceof FluidPipe) || !(level.getBlockEntity(pipePos) instanceof FluidPipeBlockEntity pipe)) {
				continue;
			}

			pipes.add(pipe);
			for (Direction direction : Direction.values()) {
				if (!pipeState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighborPos = pipePos.relative(direction);
				BlockState neighborState = level.getBlockState(neighborPos);
				if (neighborState.getBlock() instanceof FluidPipe) {
					if (visited.add(neighborPos.immutable())) {
						queue.add(neighborPos.immutable());
					}
					continue;
				}

				getEndpoint(level, pipe, direction, neighborPos, neighborState, direction.getOpposite()).ifPresent(endpoint -> {
					if (endpoint.mode() == CableConnectionMode.IMPORT && canExtract(endpoint.handler(), transaction)) {
						sources.add(endpoint);
					} else if (endpoint.mode() == CableConnectionMode.EXPORT) {
						sinks.add(endpoint);
					}
				});
			}
		}

		BlockPos controller = pipes.stream().map(FluidPipeBlockEntity::getBlockPos).min(Comparator.comparingLong(BlockPos::asLong)).orElse(origin).immutable();
		int transferLimit = pipes.stream().mapToInt(FluidPipeBlockEntity::getTransferRate).reduce(0, FluidPipeBlockEntity::saturatingAdd);
		return new FluidPipeNetwork(pipes, sources, sinks, controller, transferLimit);
	}

	public void distribute () {
		Map<BlockPos, FluidPipeBlockEntity> pipeByPos = getPipeByPos();
		Map<BlockPos, Integer> routeBudgets = getRouteBudgets();
		int moved = 0;
		for (FluidEndpoint source : sources) {
			for (FluidEndpoint sink : sinks) {
				if (moved >= transferLimit) {
					return;
				}
				if (sink.isSameConnection(source)) {
					continue;
				}

				List<BlockPos> path = findEndpointPath(source, sink, pipeByPos);
				int routeLimit = getRouteTransferLimit(path, pipeByPos, routeBudgets);
				if (routeLimit <= 0) {
					continue;
				}

				ResourceStack<FluidResource> transferred = ResourceHandlerUtil.moveFirstStacking(source.handler(), sink.handler(), resource -> true, Math.min(transferLimit - moved, routeLimit), null);
				if (transferred != null) {
					moved += transferred.amount();
					consumeRouteBudget(path, routeBudgets, transferred.amount());
				}
			}
		}
	}

	public int insertFromPipe (FluidPipeBlockEntity pipe, @Nullable Direction direction, FluidResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0) {
			return 0;
		}

		FluidEndpoint excludedSource = direction == null ? null : new FluidEndpoint(pipe.getBlockPos().relative(direction).immutable(), direction.getOpposite(), CableConnectionMode.AUTO, null);
		Map<BlockPos, FluidPipeBlockEntity> pipeByPos = getPipeByPos();
		int moved = 0;
		for (FluidEndpoint sink : sinks) {
			if (moved >= amount) {
				return moved;
			}
			if (sink.isSameConnection(excludedSource)) {
				continue;
			}

			List<BlockPos> path = findPipePath(pipe.getBlockPos(), sink.pos().relative(sink.side()), pipeByPos);
			int routeLimit = getRouteTransferLimit(path, pipeByPos, null);
			if (routeLimit <= 0) {
				continue;
			}

			moved += ResourceHandlerUtil.insertStacking(sink.handler(), resource, Math.min(amount - moved, routeLimit), transaction);
		}

		return moved;
	}

	private static Optional<FluidEndpoint> getEndpoint (Level level, FluidPipeBlockEntity pipe, Direction pipeSide, BlockPos pos, BlockState state, Direction side) {
		ResourceHandler<FluidResource> handler = getFluidHandler(level, pos, state, side);
		return handler == null ? Optional.empty() : Optional.of(new FluidEndpoint(pos.immutable(), side, pipe.getConnectionMode(pipeSide), handler));
	}

	@Nullable
	private static ResourceHandler<FluidResource> getFluidHandler (Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
		var blockEntity = level.getBlockEntity(pos);
		var sidedHandler = level.getCapability(Capabilities.Fluid.BLOCK, pos, state, blockEntity, side);
		return sidedHandler != null ? sidedHandler : level.getCapability(Capabilities.Fluid.BLOCK, pos, state, blockEntity, null);
	}

	private static boolean canExtract (@Nullable ResourceHandler<FluidResource> handler, @Nullable TransactionContext transaction) {
		return handler != null && ResourceHandlerUtil.findExtractableResource(handler, resource -> true, transaction) != null;
	}

	private Map<BlockPos, FluidPipeBlockEntity> getPipeByPos () {
		Map<BlockPos, FluidPipeBlockEntity> pipeByPos = new HashMap<>();
		for (FluidPipeBlockEntity pipe : pipes) {
			pipeByPos.put(pipe.getBlockPos(), pipe);
		}
		return pipeByPos;
	}

	private Map<BlockPos, Integer> getRouteBudgets () {
		Map<BlockPos, Integer> budgets = new HashMap<>();
		for (FluidPipeBlockEntity pipe : pipes) {
			budgets.put(pipe.getBlockPos(), pipe.getTransferRate());
		}
		return budgets;
	}

	private static List<BlockPos> findEndpointPath (FluidEndpoint source, FluidEndpoint sink, Map<BlockPos, FluidPipeBlockEntity> pipeByPos) {
		return findPipePath(source.pos().relative(source.side()), sink.pos().relative(sink.side()), pipeByPos);
	}

	private static int getRouteTransferLimit (List<BlockPos> path, Map<BlockPos, FluidPipeBlockEntity> pipeByPos, @Nullable Map<BlockPos, Integer> routeBudgets) {
		if (path.isEmpty()) {
			return 0;
		}

		int routeLimit = Integer.MAX_VALUE;
		for (BlockPos pipePos : path) {
			FluidPipeBlockEntity pipe = pipeByPos.get(pipePos);
			if (pipe == null) {
				return 0;
			}
			routeLimit = Math.min(routeLimit, pipe.getTransferRate());
			if (routeBudgets != null) {
				routeLimit = Math.min(routeLimit, routeBudgets.getOrDefault(pipePos, 0));
			}
		}
		return routeLimit;
	}

	private static void consumeRouteBudget (List<BlockPos> path, Map<BlockPos, Integer> routeBudgets, int amount) {
		for (BlockPos pipePos : path) {
			routeBudgets.computeIfPresent(pipePos, (pos, budget) -> Math.max(0, budget - amount));
		}
	}

	private static List<BlockPos> findPipePath (BlockPos start, BlockPos end, Map<BlockPos, FluidPipeBlockEntity> pipeByPos) {
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

			FluidPipeBlockEntity pipe = pipeByPos.get(current);
			BlockState state = pipe.getBlockState();
			for (Direction direction : Direction.values()) {
				if (!state.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}
				BlockPos neighbor = current.relative(direction);
				FluidPipeBlockEntity neighborPipe = pipeByPos.get(neighbor);
				if (neighborPipe != null && neighborPipe.getBlockState().getValue(CableBlock.getConnectionProperty(direction.getOpposite())) && visited.add(neighbor)) {
					previous.put(neighbor, current);
					queue.add(neighbor);
				}
			}
		}

		return List.of();
	}

	private static List<BlockPos> buildPath (BlockPos end, Map<BlockPos, BlockPos> previous) {
		List<BlockPos> path = new ArrayList<>();
		for (BlockPos at = end; at != null; at = previous.get(at)) {
			path.addFirst(at);
		}
		return path;
	}

}
