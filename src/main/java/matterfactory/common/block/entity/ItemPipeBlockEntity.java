package matterfactory.common.block.entity;

import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.CableConnectionMode;
import matterfactory.common.block.cable.ItemPipe;
import matterfactory.common.network.ItemPipeNetwork;
import matterfactory.core.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class ItemPipeBlockEntity extends BaseCableBlockEntity {

	public static final int VISUAL_TRANSFER_DURATION = 12;

	private ItemStack visualItem = ItemStack.EMPTY;
	private Direction visualFrom = Direction.NORTH;
	private Direction visualTo = Direction.SOUTH;
	private long visualStartGameTime;
	private int visualDuration = VISUAL_TRANSFER_DURATION;

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

	public ItemStack getVisualItem () {
		return visualItem;
	}

	public Direction getVisualFrom () {
		return visualFrom;
	}

	public Direction getVisualTo () {
		return visualTo;
	}

	public long getVisualStartGameTime () {
		return visualStartGameTime;
	}

	public int getVisualDuration () {
		return visualDuration;
	}

	public void showItemTransfer (ItemResource resource, int amount, Direction from, Direction to, long startGameTime, int duration) {
		if (resource.isEmpty() || amount <= 0) {
			return;
		}

		if (!visualItem.isEmpty() && startGameTime < visualStartGameTime + visualDuration) {
			return;
		}

		this.visualItem = resource.toStack(Math.min(amount, resource.getMaxStackSize()));
		this.visualFrom = from;
		this.visualTo = to;
		this.visualStartGameTime = startGameTime;
		this.visualDuration = duration;
		markConnectionDataChanged();
	}

	@Override
	protected boolean isNetworkCable (BlockState state) {
		return state.getBlock() instanceof ItemPipe;
	}

	@Override
	protected void loadAdditional (@NonNull ValueInput input) {
		super.loadAdditional(input);
		visualItem = input.read("visual_item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		visualFrom = directionOr(input.getStringOr("visual_from", Direction.NORTH.getSerializedName()), Direction.NORTH);
		visualTo = directionOr(input.getStringOr("visual_to", Direction.SOUTH.getSerializedName()), Direction.SOUTH);
		visualStartGameTime = input.getLongOr("visual_start", 0);
		visualDuration = input.getIntOr("visual_duration", VISUAL_TRANSFER_DURATION);
	}

	@Override
	protected void saveAdditional (@NonNull ValueOutput output) {
		super.saveAdditional(output);
		if (!visualItem.isEmpty()) {
			output.store("visual_item", ItemStack.OPTIONAL_CODEC, visualItem);
			output.putString("visual_from", visualFrom.getSerializedName());
			output.putString("visual_to", visualTo.getSerializedName());
			output.putLong("visual_start", visualStartGameTime);
			output.putInt("visual_duration", visualDuration);
		}
	}

	private static Direction directionOr (String name, Direction fallback) {
		Direction direction = Direction.byName(name);
		return direction == null ? fallback : direction;
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
