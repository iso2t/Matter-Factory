package com.iso2t.matterfactory.server;

import com.iso2t.matterfactory.core.Factory;
import com.iso2t.matterfactory.core.FactoryBase;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = Factory.MODID, dist = Dist.DEDICATED_SERVER)
public class FactoryServer extends FactoryBase {

	public FactoryServer (IEventBus bus, ModContainer container) {
		super(bus, container);
	}

	@Override
	public Level getClientLevel () {
		return null;
	}

}
