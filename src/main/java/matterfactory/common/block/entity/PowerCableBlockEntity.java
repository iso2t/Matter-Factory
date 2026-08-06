package matterfactory.common.block.entity;

import matterfactory.Tier;
import matterfactory.common.block.cable.PowerCable;
import matterfactory.common.registries.FBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jetbrains.annotations.Nullable;

public class PowerCableBlockEntity extends BaseBlockEntity {

	private final SimpleEnergyHandler energyHandler;

	public PowerCableBlockEntity (BlockEntityType<PowerCableBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.energyHandler = createEnergyHandler(state);
	}

	public PowerCableBlockEntity (BlockPos pos, BlockState state) {
		this(FBlockEntities.POWER_CABLE.get(), pos, state);
	}

	@Nullable
	public EnergyHandler getEnergyHandler (@Nullable Direction direction) {
		return energyHandler;
	}

	@Override
	protected void loadAdditional (ValueInput input) {
		super.loadAdditional(input);
		energyHandler.deserialize(input.childOrEmpty("energy"));
	}

	@Override
	protected void saveAdditional (ValueOutput output) {
		super.saveAdditional(output);
		energyHandler.serialize(output.child("energy"));
	}

	private SimpleEnergyHandler createEnergyHandler (BlockState state) {
		Tier tier = getTier(state);
		return new SimpleEnergyHandler(tier.getCapacity(), tier.getTransferRate(), tier.getTransferRate()) {
			@Override
			protected void onEnergyChanged (int previousAmount) {
				setChanged();
			}
		};
	}

	private static Tier getTier (BlockState state) {
		return state.getBlock() instanceof PowerCable powerCable ? powerCable.getTier() : Tier.BASIC;
	}

}
