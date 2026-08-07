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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record FluidPipeNetwork(List<FluidPipeBlockEntity> pipes, List<FluidEndpoint> sources, List<FluidEndpoint> sinks, BlockPos controller, int transferLimit) {

	public static FluidPipeNetwork discover (Level level, BlockPos origin) {
		return discover(level, origin, null);
	}

	public static FluidPipeNetwork discover (Level level, BlockPos origin, @Nullable TransactionContext transaction) {
		List<FluidPipeBlockEntity> pipes = CableNetwork.discover(level, origin, FluidPipe.class, FluidPipeBlockEntity.class);
		List<FluidEndpoint> sources = new ArrayList<>();
		List<FluidEndpoint> sinks = new ArrayList<>();
		for (FluidPipeBlockEntity pipe : pipes) {
			BlockPos pipePos = pipe.getBlockPos();
			BlockState pipeState = pipe.getBlockState();
			for (Direction direction : Direction.values()) {
				if (!pipeState.getValue(CableBlock.getConnectionProperty(direction))) {
					continue;
				}

				BlockPos neighborPos = pipePos.relative(direction);
				BlockState neighborState = level.getBlockState(neighborPos);
				if (!(neighborState.getBlock() instanceof FluidPipe)) {
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
		int transferLimit = CableNetwork.totalTransferRate(pipes, FluidPipeBlockEntity::getTransferRate);
		return new FluidPipeNetwork(pipes, sources, sinks, controller, transferLimit);
	}

	public void distribute () {
		Map<BlockPos, FluidPipeBlockEntity> pipeByPos = CableNetwork.indexPipes(pipes);
		Map<BlockPos, Integer> routeBudgets = CableNetwork.routeBudgets(pipes, FluidPipeBlockEntity::getTransferRate);
		int moved = 0;
		for (FluidEndpoint source : sources) {
			for (FluidEndpoint sink : sinks) {
				if (moved >= transferLimit) {
					return;
				}
				if (sink.isSameConnection(source)) {
					continue;
				}

				List<BlockPos> path = CableNetwork.findPath(source.pos().relative(source.side()), sink.pos().relative(sink.side()), pipeByPos);
				int routeLimit = CableNetwork.routeTransferLimit(path, pipeByPos, FluidPipeBlockEntity::getTransferRate, routeBudgets);
				if (routeLimit <= 0) {
					continue;
				}

				ResourceStack<FluidResource> transferred = ResourceHandlerUtil.moveFirstStacking(source.handler(), sink.handler(), resource -> true, Math.min(transferLimit - moved, routeLimit), null);
				if (transferred != null) {
					moved += transferred.amount();
					CableNetwork.consumeRouteBudget(path, routeBudgets, transferred.amount());
				}
			}
		}
	}

	public int insertFromPipe (FluidPipeBlockEntity pipe, @Nullable Direction direction, FluidResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0) {
			return 0;
		}

		FluidEndpoint excludedSource = direction == null ? null : new FluidEndpoint(pipe.getBlockPos().relative(direction).immutable(), direction.getOpposite(), CableConnectionMode.AUTO, null);
		Map<BlockPos, FluidPipeBlockEntity> pipeByPos = CableNetwork.indexPipes(pipes);
		int moved = 0;
		for (FluidEndpoint sink : sinks) {
			if (moved >= amount) {
				return moved;
			}
			if (sink.isSameConnection(excludedSource)) {
				continue;
			}

			List<BlockPos> path = CableNetwork.findPath(pipe.getBlockPos(), sink.pos().relative(sink.side()), pipeByPos);
			int routeLimit = CableNetwork.routeTransferLimit(path, pipeByPos, FluidPipeBlockEntity::getTransferRate, null);
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

}
