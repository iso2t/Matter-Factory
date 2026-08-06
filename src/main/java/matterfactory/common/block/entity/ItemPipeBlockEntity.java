package matterfactory.common.block.entity;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.ItemPipe;
import matterfactory.common.network.ItemPipeNetwork;
import matterfactory.core.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class ItemPipeBlockEntity extends BaseCableBlockEntity {

	public ItemPipeBlockEntity (BlockEntityType<ItemPipeBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Nullable
	public ResourceHandler<ItemResource> getItemHandler (@Nullable Direction direction) {
		return new PipeItemHandler(this, direction);
	}

	public static void serverTick (Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity blockEntity) {
		if (level.isClientSide()) {
			return;
		}

		ItemPipeNetwork network = ItemPipeNetwork.discover(level, pos);
		if (!network.controller().equals(pos)) {
			return;
		}

		network.distribute();
	}

	private static Tier getTier (BlockState state) {
		return state.getBlock() instanceof ItemPipe itemPipe ? itemPipe.getTier() : Tier.BASIC;
	}

	public int getTransferRate () {
		return getTier(getBlockState()).getItemTransferRate();
	}

	@Override
	protected boolean isNetworkCable (BlockState state) {
		return state.getBlock() instanceof ItemPipe;
	}

	private record PipeItemHandler(ItemPipeBlockEntity pipe, @Nullable Direction direction) implements ResourceHandler<ItemResource> {

		@Override
		public int size () {
			return 1;
		}

		@Override
		public @NonNull ItemResource getResource (int index) {
			return ItemResource.EMPTY;
		}

		@Override
		public long getAmountAsLong (int index) {
			return 0;
		}

		@Override
		public long getCapacityAsLong (int index, ItemResource resource) {
			return pipe.getTransferRate();
		}

		@Override
		public boolean isValid (int index, ItemResource resource) {
			return !resource.isEmpty();
		}

		@Override
		public int insert (int index, ItemResource resource, int amount, TransactionContext transaction) {
			return insert(resource, amount, transaction);
		}

		@Override
		public int insert (@NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
			if (pipe.getLevel() == null || pipe.getLevel().isClientSide() || resource.isEmpty() || amount <= 0) {
				return 0;
			}

			if (direction != null && (!pipe.getBlockState().getValue(CableBlock.getConnectionProperty(direction)) || pipe.getConnectionMode(direction) == CableConnectionMode.EXPORT)) {
				return 0;
			}

			ItemPipeNetwork network = ItemPipeNetwork.discover(pipe.getLevel(), pipe.getBlockPos(), transaction);
			return network.insertFromPipe(pipe, direction, resource, Math.min(amount, pipe.getTransferRate()), transaction);
		}

		@Override
		public int extract (int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
			return 0;
		}

		@Override
		public int extract (@NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
			return 0;
		}
	}

	public static int saturatingAdd (int left, int right) {
		long result = (long) left + right;
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}

}
