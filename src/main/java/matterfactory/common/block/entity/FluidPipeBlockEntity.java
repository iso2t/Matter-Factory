package matterfactory.common.block.entity;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.FluidPipe;
import matterfactory.common.network.FluidPipeNetwork;
import matterfactory.core.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class FluidPipeBlockEntity extends BaseCableBlockEntity {

	public FluidPipeBlockEntity (BlockEntityType<FluidPipeBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Nullable
	public ResourceHandler<FluidResource> getFluidHandler (@Nullable Direction direction) {
		return new PipeFluidHandler(this, direction);
	}

	public static void serverTick (Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity blockEntity) {
		if (level.isClientSide()) {
			return;
		}

		FluidPipeNetwork network = FluidPipeNetwork.discover(level, pos);
		if (network.controller().equals(pos)) {
			network.distribute();
		}
	}

	public int getTransferRate () {
		float rate = getTier(getBlockState()).getFluidTransferRate();
		return rate >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) rate);
	}

	private static Tier getTier (BlockState state) {
		return state.getBlock() instanceof FluidPipe fluidPipe ? fluidPipe.getTier() : Tier.BASIC;
	}

	@Override
	protected boolean isNetworkCable (BlockState state) {
		return state.getBlock() instanceof FluidPipe;
	}

	private record PipeFluidHandler(FluidPipeBlockEntity pipe, @Nullable Direction direction) implements ResourceHandler<FluidResource> {

		@Override
		public int size () {
			return 1;
		}

		@Override
		public @NonNull FluidResource getResource (int index) {
			return FluidResource.EMPTY;
		}

		@Override
		public long getAmountAsLong (int index) {
			return 0;
		}

		@Override
		public long getCapacityAsLong (int index, @NonNull FluidResource resource) {
			return pipe.getTransferRate();
		}

		@Override
		public boolean isValid (int index, FluidResource resource) {
			return !resource.isEmpty();
		}

		@Override
		public int insert (int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
			return insert(resource, amount, transaction);
		}

		@Override
		public int insert (@NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
			if (pipe.getLevel() == null || pipe.getLevel().isClientSide() || resource.isEmpty() || amount <= 0) {
				return 0;
			}

			if (direction != null && (!pipe.getBlockState().getValue(CableBlock.getConnectionProperty(direction)) || pipe.getConnectionMode(direction) == CableConnectionMode.EXPORT)) {
				return 0;
			}

			FluidPipeNetwork network = FluidPipeNetwork.discover(pipe.getLevel(), pipe.getBlockPos(), transaction);
			return network.insertFromPipe(pipe, direction, resource, Math.min(amount, pipe.getTransferRate()), transaction);
		}

		@Override
		public int extract (int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
			return 0;
		}

		@Override
		public int extract (@NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
			return 0;
		}
	}

}
