package matterfactory.common.registries;

import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.block.entity.FluidPipeBlockEntity;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import matterfactory.common.block.machine.AbstractMachineBlockEntity;
import matterfactory.common.definition.BlockEntityDefinition;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class FactoryCapabilities {

	public static <T extends AbstractMachineBlockEntity> void registerMachineCapabilities (RegisterCapabilitiesEvent event, BlockEntityDefinition<T> definition) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, definition.get(), AbstractMachineBlockEntity::getItemHandler);
		event.registerBlockEntity(Capabilities.Energy.BLOCK, definition.get(), AbstractMachineBlockEntity::getEnergyHandler);
	}

	public static void register (RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.POWER_CABLE.get(), PowerCableBlockEntity::getEnergyHandler);
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.FLUID_PIPE.get(), FluidPipeBlockEntity::getFluidHandler);
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.ITEM_PIPE.get(), ItemPipeBlockEntity::getItemHandler);
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.FACADE.get(), FacadeBlockEntity::getEnergyHandler);
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.FACADE.get(), FacadeBlockEntity::getFluidHandler);
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.FACADE.get(), FacadeBlockEntity::getItemHandler);

		registerMachineCapabilities(event, FactoryBlockEntities.ELECTRIC_FURNACE);
	}

}
