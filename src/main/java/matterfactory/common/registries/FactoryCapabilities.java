package matterfactory.common.registries;

import matterfactory.common.block.entity.FluidPipeBlockEntity;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class FactoryCapabilities {

	public static void register (RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.POWER_CABLE.get(), PowerCableBlockEntity::getEnergyHandler);
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.FLUID_PIPE.get(), FluidPipeBlockEntity::getFluidHandler);
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.ITEM_PIPE.get(), ItemPipeBlockEntity::getItemHandler);
	}

}
