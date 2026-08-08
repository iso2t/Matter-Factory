package matterfactory.common.block.machine;

import matterfactory.common.block.entity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class AbstractMachineBlockEntity extends BaseBlockEntity {

	private final MachineConfiguration      configuration;
	private final ItemStacksResourceHandler inventory;
	private final SimpleEnergyHandler       energyStorage;

	public AbstractMachineBlockEntity (BlockEntityType<?> type, BlockPos pos, BlockState state, MachineConfiguration configuration) {
		super(type, pos, state);
		this.configuration = configuration;
		this.inventory = new ItemStacksResourceHandler(configuration.inventorySlots()) {
			@Override
			protected void onContentsChanged (int index, @NonNull ItemStack previousContents) {
				AbstractMachineBlockEntity.this.onInventoryChanged(index, previousContents);
			}
		};
		this.energyStorage = new SimpleEnergyHandler(configuration.energyCapacity(), configuration.maxEnergyInsert(), configuration.maxEnergyExtract()) {
			@Override
			protected void onEnergyChanged (int previousAmount) {
				AbstractMachineBlockEntity.this.onEnergyChanged(previousAmount);
			}
		};
	}

	public final MachineConfiguration getMachineConfiguration () {
		return configuration;
	}

	public final ItemStacksResourceHandler getInventory () {
		return inventory;
	}

	public final SimpleEnergyHandler getEnergyStorage () {
		return energyStorage;
	}

	/**
	 * The unsided item handler. Future side configuration should override this method with a directional view.
	 */
	public ResourceHandler<ItemResource> getItemHandler (@Nullable Direction direction) {
		return inventory;
	}

	/**
	 * The unsided energy handler. Future side configuration should override this method with a directional view.
	 */
	public EnergyHandler getEnergyHandler (@Nullable Direction direction) {
		return energyStorage;
	}

	public void serverTick () {
	}

	protected void onInventoryChanged (int index, ItemStack previousContents) {
		setChanged();
	}

	protected void onEnergyChanged (int previousAmount) {
		setChanged();
	}

	@Override
	protected void loadAdditional (@NonNull ValueInput input) {
		super.loadAdditional(input);
		inventory.deserialize(input);
		energyStorage.deserialize(input);
	}

	@Override
	protected void saveAdditional (@NonNull ValueOutput output) {
		super.saveAdditional(output);
		inventory.serialize(output);
		energyStorage.serialize(output);
	}

	@Override
	public void preRemoveSideEffects (@NonNull BlockPos pos, @NonNull BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (!(getLevel() instanceof Level level) || level.isClientSide()) {
			return;
		}

		for (ItemStack stack : inventory.copyToList()) {
			if (!stack.isEmpty()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
	}

}
