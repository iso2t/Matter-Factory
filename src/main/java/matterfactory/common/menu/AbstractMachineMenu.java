package matterfactory.common.menu;

import lombok.Getter;
import matterfactory.common.block.entity.BaseBlockEntity;
import matterfactory.common.block.machine.AbstractMachineBlockEntity;
import matterfactory.common.block.machine.AbstractMachineEntityBlock;
import matterfactory.util.gui.QuickMoveStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMachineMenu<B extends AbstractMachineEntityBlock<?>, T extends BaseBlockEntity> extends AbstractContainerMenu {

	@Getter
	private final B block;

	@Getter
	private final T blockEntity;

	@Getter
	private ContainerData data;

	public AbstractMachineMenu (MenuType<?> type, int containerId, Inventory inventory, B block, T blockEntity) {
		super(type, containerId);
		this.block = block;
		this.blockEntity = blockEntity;

		addPlayerInventory(inventory);
		addPlayerHotbar(inventory);
		addContainerSlots();
	}

	/**
	 * Returns the number of slots in the inventory.
	 * @return the number of slots in the inventory.
	 */
	public abstract int getSlotCount ();

	public abstract void addContainerSlots ();

	@Override
	public @NonNull ItemStack quickMoveStack (@NonNull Player player, int index) {
		return new QuickMoveStack(this, getSlotCount(), player, index).move();
	}

	@Override
	public boolean stillValid (@NonNull Player player) {
		return stillValid(ContainerLevelAccess.create(player.level(), blockEntity.getBlockPos()), player, blockEntity.getBlockState().getBlock());
	}

	/**
	 * Adds the player inventory.
	 * @param playerInventory the player inventory.
	 */
	public void addPlayerInventory (Inventory playerInventory) {
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, (8 + col * 18), 84 + row * 18));
			}
		}
	}

	/**
	 * Adds the player hotbar.
	 * @param playerInventory the player inventory.
	 */
	public void addPlayerHotbar (Inventory playerInventory) {
		for (int i = 0; i < 9; ++i) {
			this.addSlot(new Slot(playerInventory, i, (8 + i * 18), 142));
		}
	}

	public @Nullable SimpleEnergyHandler getEnergyStorage () {
		return ((AbstractMachineBlockEntity) blockEntity).getEnergyStorage();
	}

}
