package matterfactory.common.registries;

import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class FCapabilities {

	public static void register (RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FBlockEntities.POWER_CABLE.get(), PowerCableBlockEntity::getEnergyHandler);
	}

}
