package matterfactory.common.block.entity;

import lombok.Getter;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ItemPipeBlockEntity extends BaseCableBlockEntity {

	public static final int VISUAL_TRANSFER_DURATION = 12;
	public static final int VISUAL_ITEM_SPACING      = 1;

	@Getter
	private ItemStack visualItem = ItemStack.EMPTY;

	@Getter
	private Direction visualFrom = Direction.NORTH;

	@Getter
	private Direction visualTo = Direction.SOUTH;

	@Getter
	private long visualStartGameTime;

	@Getter
	private int visualDuration = VISUAL_TRANSFER_DURATION;

	@Getter
	private       int                       visualItemSpacing      = VISUAL_ITEM_SPACING;
	private final List<PendingItemDelivery> pendingDeliveries      = new ArrayList<>();
	private final PendingDeliveryJournal    pendingDeliveryJournal = new PendingDeliveryJournal();
	private final VisualStateJournal        visualStateJournal     = new VisualStateJournal();

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

		blockEntity.processPendingDeliveries(level);
		network.distribute();
	}

	private static Tier getTier (BlockState state) {
		return state.getBlock() instanceof ItemPipe itemPipe ? itemPipe.getTier() : Tier.BASIC;
	}

	public int getTransferRate () {
		return getTier(getBlockState()).getItemTransferRate();
	}

	public int getVisualTotalDuration () {
		return visualDuration + Math.max(0, visualItem.getCount() - 1) * visualItemSpacing;
	}

	public boolean canShowItemTransfer (long startGameTime, int duration) {
		return visualItem.isEmpty() || startGameTime >= visualStartGameTime + getVisualTotalDuration();
	}

	public boolean showItemTransfer (ItemResource resource, int amount, Direction from, Direction to, long startGameTime, int duration) {
		return showItemTransfer(resource, amount, from, to, startGameTime, duration, null);
	}

	public boolean showItemTransfer (ItemResource resource, int amount, Direction from, Direction to, long startGameTime, int duration, @Nullable TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0) {
			return false;
		}

		if (!canShowItemTransfer(startGameTime, duration)) {
			return false;
		}

		if (transaction != null) {
			visualStateJournal.updateSnapshots(transaction);
		}

		this.visualItem = resource.toStack(Math.min(amount, resource.getMaxStackSize()));
		this.visualFrom = from;
		this.visualTo = to;
		this.visualStartGameTime = startGameTime;
		this.visualDuration = duration;
		this.visualItemSpacing = VISUAL_ITEM_SPACING;
		if (transaction == null) {
			markConnectionDataChanged();
		}
		return true;
	}

	public void enqueueItemDelivery (ItemResource resource, int amount, BlockPos sinkPos, Direction sinkSide, long firstArrivalGameTime, int itemSpacing) {
		enqueueItemDelivery(resource, amount, sinkPos, sinkSide, firstArrivalGameTime, itemSpacing, null);
	}

	public void enqueueItemDelivery (ItemResource resource, int amount, BlockPos sinkPos, Direction sinkSide, long firstArrivalGameTime, int itemSpacing, @Nullable TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0) {
			return;
		}

		if (transaction != null) {
			pendingDeliveryJournal.updateSnapshots(transaction);
		}

		pendingDeliveries.add(new PendingItemDelivery(resource, amount, sinkPos.immutable(), sinkSide, firstArrivalGameTime, itemSpacing));
		if (transaction == null) {
			setChanged();
		}
	}

	private void processPendingDeliveries (Level level) {
		long gameTime = level.getGameTime();
		boolean changed = false;
		Iterator<PendingItemDelivery> iterator = pendingDeliveries.iterator();
		while (iterator.hasNext()) {
			PendingItemDelivery delivery = iterator.next();
			if (delivery.nextArrivalGameTime() > gameTime) {
				continue;
			}

			int due = 1 + (int) ((gameTime - delivery.nextArrivalGameTime()) / Math.max(1, delivery.itemSpacing()));
			int delivered = insertPendingItems(level, delivery, Math.min(delivery.amount(), due));
			if (delivered <= 0) {
				continue;
			}

			changed = true;
			if (delivered >= delivery.amount()) {
				iterator.remove();
			} else {
				delivery.remove(delivered);
			}
		}

		if (changed) {
			setChanged();
		}
	}

	private static int insertPendingItems (Level level, PendingItemDelivery delivery, int amount) {
		var state = level.getBlockState(delivery.sinkPos());
		var blockEntity = level.getBlockEntity(delivery.sinkPos());
		var handler = level.getCapability(Capabilities.Item.BLOCK, delivery.sinkPos(), state, blockEntity, delivery.sinkSide());
		if (handler == null) {
			handler = level.getCapability(Capabilities.Item.BLOCK, delivery.sinkPos(), state, blockEntity, null);
		}

		return ResourceHandlerUtil.insertStacking(handler, delivery.resource(), amount, null);
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
		visualItemSpacing = input.getIntOr("visual_spacing", VISUAL_ITEM_SPACING);

		pendingDeliveries.clear();
		for (ValueInput child : input.childrenListOrEmpty("pending_deliveries")) {
			ItemResource resource = child.read("resource", ItemResource.OPTIONAL_CODEC).orElse(ItemResource.EMPTY);
			int amount = child.getIntOr("amount", 0);
			if (resource.isEmpty() || amount <= 0) {
				continue;
			}

			pendingDeliveries.add(new PendingItemDelivery(resource, amount, BlockPos.of(child.getLongOr("sink_pos", worldPosition.asLong())), directionOr(child.getStringOr("sink_side", Direction.NORTH.getSerializedName()), Direction.NORTH), child.getLongOr("next_arrival", 0), child.getIntOr("item_spacing", VISUAL_ITEM_SPACING)));
		}
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
			output.putInt("visual_spacing", visualItemSpacing);
		}

		if (!pendingDeliveries.isEmpty()) {
			var deliveries = output.childrenList("pending_deliveries");
			for (PendingItemDelivery delivery : pendingDeliveries) {
				var child = deliveries.addChild();
				child.store("resource", ItemResource.OPTIONAL_CODEC, delivery.resource());
				child.putInt("amount", delivery.amount());
				child.putLong("sink_pos", delivery.sinkPos().asLong());
				child.putString("sink_side", delivery.sinkSide().getSerializedName());
				child.putLong("next_arrival", delivery.nextArrivalGameTime());
				child.putInt("item_spacing", delivery.itemSpacing());
			}
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
		public long getCapacityAsLong (int index, @NonNull ItemResource resource) {
			return pipe.getTransferRate();
		}

		@Override
		public boolean isValid (int index, ItemResource resource) {
			return !resource.isEmpty();
		}

		@Override
		public int insert (int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
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

	private static final class PendingItemDelivery {
		private final ItemResource resource;
		private       int          amount;
		private final BlockPos     sinkPos;
		private final Direction    sinkSide;
		private       long         nextArrivalGameTime;
		private final int          itemSpacing;

		private PendingItemDelivery (ItemResource resource, int amount, BlockPos sinkPos, Direction sinkSide, long nextArrivalGameTime, int itemSpacing) {
			this.resource = resource;
			this.amount = amount;
			this.sinkPos = sinkPos;
			this.sinkSide = sinkSide;
			this.nextArrivalGameTime = nextArrivalGameTime;
			this.itemSpacing = itemSpacing;
		}

		private ItemResource resource () {
			return resource;
		}

		private int amount () {
			return amount;
		}

		private BlockPos sinkPos () {
			return sinkPos;
		}

		private Direction sinkSide () {
			return sinkSide;
		}

		private long nextArrivalGameTime () {
			return nextArrivalGameTime;
		}

		private int itemSpacing () {
			return itemSpacing;
		}

		private void remove (int delivered) {
			amount -= delivered;
			nextArrivalGameTime += (long) delivered * Math.max(1, itemSpacing);
		}

		private PendingItemDelivery copy () {
			return new PendingItemDelivery(resource, amount, sinkPos, sinkSide, nextArrivalGameTime, itemSpacing);
		}
	}

	private final class PendingDeliveryJournal extends SnapshotJournal<List<PendingItemDelivery>> {
		@Override
		protected List<PendingItemDelivery> createSnapshot () {
			List<PendingItemDelivery> snapshot = new ArrayList<>(pendingDeliveries.size());
			for (PendingItemDelivery delivery : pendingDeliveries) {
				snapshot.add(delivery.copy());
			}

			return snapshot;
		}

		@Override
		protected void revertToSnapshot (List<PendingItemDelivery> snapshot) {
			pendingDeliveries.clear();
			pendingDeliveries.addAll(snapshot);
		}

		@Override
		protected void onRootCommit (List<PendingItemDelivery> originalState) {
			setChanged();
		}
	}

	private final class VisualStateJournal extends SnapshotJournal<VisualStateSnapshot> {
		@Override
		protected VisualStateSnapshot createSnapshot () {
			return new VisualStateSnapshot(visualItem.copy(), visualFrom, visualTo, visualStartGameTime, visualDuration, visualItemSpacing);
		}

		@Override
		protected void revertToSnapshot (VisualStateSnapshot snapshot) {
			visualItem = snapshot.item();
			visualFrom = snapshot.from();
			visualTo = snapshot.to();
			visualStartGameTime = snapshot.startGameTime();
			visualDuration = snapshot.duration();
			visualItemSpacing = snapshot.itemSpacing();
		}

		@Override
		protected void onRootCommit (VisualStateSnapshot originalState) {
			markConnectionDataChanged();
		}
	}

	private record VisualStateSnapshot(ItemStack item, Direction from, Direction to, long startGameTime, int duration, int itemSpacing) {
	}

}
