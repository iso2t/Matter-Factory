package matterfactory.client;

import matterfactory.client.renderer.blockentity.PowerCableRenderer;
import matterfactory.common.registries.FBlockEntities;
import matterfactory.core.Factory;
import matterfactory.core.FactoryBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = Factory.MODID, dist = Dist.CLIENT)
public class FactoryClient extends FactoryBase {

	public FactoryClient (IEventBus bus, ModContainer container) {
		super(bus, container);
		bus.addListener(this::registerRenderers);
	}

	private void registerRenderers (EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(FBlockEntities.POWER_CABLE.get(), PowerCableRenderer::new);
	}

	@Override
	public Level getClientLevel () {
		return Minecraft.getInstance().level;
	}

}
