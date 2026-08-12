package matterfactory.client;

import matterfactory.client.color.ClientColors;
import matterfactory.client.renderer.blockentity.CableModeRenderer;
import matterfactory.client.renderer.blockentity.FacadeRenderer;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.block.cable.FacadeBlock;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.item.tool.WrenchItem;
import matterfactory.common.registries.FactoryBlockEntities;
import matterfactory.core.Factory;
import matterfactory.core.FactoryBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Factory.MODID, dist = Dist.CLIENT)
public class FactoryClient extends FactoryBase {

	public FactoryClient (IEventBus bus, ModContainer container) {
		super(bus, container);
		bus.addListener(this::registerRenderers);
		bus.addListener(ClientFluidRegistration::registerExtensions);
		bus.addListener(ClientFluidRegistration::registerModels);
		bus.addListener(ClientColors::registerItemColors);
		bus.addListener(ClientColors::registerBlockColors);
		NeoForge.EVENT_BUS.addListener(this::suppressFacadeMaintenanceOutline);
	}

	private void registerRenderers (EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(FactoryBlockEntities.POWER_CABLE.get(), CableModeRenderer::new);
		event.registerBlockEntityRenderer(FactoryBlockEntities.FLUID_PIPE.get(), CableModeRenderer::new);
		event.registerBlockEntityRenderer(FactoryBlockEntities.ITEM_PIPE.get(), CableModeRenderer::new);
		event.registerBlockEntityRenderer(FactoryBlockEntities.FACADE.get(), FacadeRenderer::new);
	}

	private void suppressFacadeMaintenanceOutline (ExtractBlockOutlineRenderStateEvent event) {
		if (!(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof WrenchItem)
		    || !(event.getBlockState().getBlock() instanceof FacadeBlock)
		    || !(event.getLevel().getBlockEntity(event.getBlockPos()) instanceof FacadeBlockEntity facade)
		    || !(facade.getCoveredState().getBlock() instanceof CableBlock)) {
			return;
		}

		event.setCanceled(true);
	}

	@Override
	public Level getClientLevel () {
		return Minecraft.getInstance().level;
	}

}
