package matterfactory.common.network;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/** Shared topology operations for cable and pipe networks. */
public final class CableNetwork {

	private CableNetwork () {
	}

	public static <T extends BaseCableBlockEntity> List<T> discover (Level level, BlockPos origin, Class<? extends CableBlock> blockType, Class<T> blockEntityType) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<T> pipes = new ArrayList<>();

		BlockPos immutableOrigin = origin.immutable();
		visited.add(immutableOrigin);
		queue.add(immutableOrigin);
		while (!queue.isEmpty()) {
			BlockPos pipePos = queue.removeFirst();
			BlockState pipeState = level.getBlockState(pipePos);
			var blockEntity = level.getBlockEntity(pipePos);
			if (!blockType.isInstance(pipeState.getBlock()) || !blockEntityType.isInstance(blockEntity)) {
				continue;
			}

			pipes.add(blockEntityType.cast(blockEntity));
			for (Direction direction : Direction.values()) {
				if (!pipeState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighborPos = pipePos.relative(direction);
				if (blockType.isInstance(level.getBlockState(neighborPos).getBlock()) && visited.add(neighborPos.immutable())) {
					queue.add(neighborPos.immutable());
				}
			}
		}

		return pipes;
	}

	public static <T extends BaseCableBlockEntity> Map<BlockPos, T> indexPipes (List<T> pipes) {
		Map<BlockPos, T> pipeByPos = new HashMap<>();
		for (T pipe : pipes) {
			pipeByPos.put(pipe.getBlockPos(), pipe);
		}
		return pipeByPos;
	}

	public static <T extends BaseCableBlockEntity> BlockPos controller (List<T> pipes, BlockPos fallback) {
		return pipes.stream().map(BaseCableBlockEntity::getBlockPos).min(Comparator.comparingLong(BlockPos::asLong)).orElse(fallback).immutable();
	}

	public static <T extends BaseCableBlockEntity> int totalTransferRate (List<T> pipes, ToIntFunction<T> transferRate) {
		return pipes.stream().mapToInt(transferRate).reduce(0, CableNetwork::saturatingAdd);
	}

	public static <T extends BaseCableBlockEntity> Map<BlockPos, Integer> routeBudgets (List<T> pipes, ToIntFunction<T> transferRate) {
		Map<BlockPos, Integer> budgets = new HashMap<>();
		for (T pipe : pipes) {
			budgets.put(pipe.getBlockPos(), transferRate.applyAsInt(pipe));
		}
		return budgets;
	}

	public static <T extends BaseCableBlockEntity> List<BlockPos> findPath (BlockPos start, BlockPos end, Map<BlockPos, T> pipeByPos) {
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

			T pipe = pipeByPos.get(current);
			BlockState state = pipe.getBlockState();
			for (Direction direction : Direction.values()) {
				if (!state.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighbor = current.relative(direction);
				T neighborPipe = pipeByPos.get(neighbor);
				if (neighborPipe != null && neighborPipe.getBlockState().getValue(CableBlock.getConnectionProperty(direction.getOpposite())) && visited.add(neighbor)) {
					previous.put(neighbor, current);
					queue.add(neighbor);
				}
			}
		}

		return List.of();
	}

	public static <T extends BaseCableBlockEntity> int routeTransferLimit (List<BlockPos> path, Map<BlockPos, T> pipeByPos, ToIntFunction<T> transferRate, @Nullable Map<BlockPos, Integer> routeBudgets) {
		if (path.isEmpty()) {
			return 0;
		}

		int routeLimit = Integer.MAX_VALUE;
		for (BlockPos pipePos : path) {
			T pipe = pipeByPos.get(pipePos);
			if (pipe == null) {
				return 0;
			}

			routeLimit = Math.min(routeLimit, transferRate.applyAsInt(pipe));
			if (routeBudgets != null) {
				routeLimit = Math.min(routeLimit, routeBudgets.getOrDefault(pipePos, 0));
			}
		}
		return routeLimit;
	}

	public static void consumeRouteBudget (List<BlockPos> path, @Nullable Map<BlockPos, Integer> routeBudgets, int amount) {
		if (routeBudgets == null || amount <= 0) {
			return;
		}

		for (BlockPos pipePos : path) {
			routeBudgets.computeIfPresent(pipePos, (pos, budget) -> Math.max(0, budget - amount));
		}
	}

	public static int saturatingAdd (int left, int right) {
		long result = (long) left + right;
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}

	private static List<BlockPos> buildPath (BlockPos end, Map<BlockPos, BlockPos> previous) {
		List<BlockPos> path = new ArrayList<>();
		for (BlockPos at = end; at != null; at = previous.get(at)) {
			path.addFirst(at);
		}
		return path;
	}

}
