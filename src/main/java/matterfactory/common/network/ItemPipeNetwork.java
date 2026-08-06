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
				}
			}
		}

		return moved;
	}

}
