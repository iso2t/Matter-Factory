package matterfactory.core;

import matterfactory.common.FTab;
import matterfactory.common.registries.FBlockEntities;
import matterfactory.common.registries.FBlocks;
import matterfactory.common.registries.FCapabilities;
import matterfactory.common.registries.FItems;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class FactoryBase implements Factory {

	static Factory INSTANCE;

	@Getter
	private final IEventBus eventBus;

	@Getter
	private final ModContainer modContainer;

	public FactoryBase (IEventBus bus, ModContainer container) {
		if (INSTANCE != null) throw new IllegalStateException("%s already initialized".formatted(NAME));
		INSTANCE = this;

		this.eventBus = bus;
		this.modContainer = container;

		bus.addListener(FTab::initExternal);
		bus.addListener((RegisterEvent event) -> {
			if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) FTab.init(BuiltInRegistries.CREATIVE_MODE_TAB);
		});

		register();
	}

	private void register () {
		var bus = getEventBus();

		FBlocks.REGISTRY.register(bus);
		FItems.REGISTRY.register(bus);
		FBlockEntities.REGISTRY.register(bus);
		bus.addListener(FCapabilities::register);
	}

	@Override
	public Collection<ServerPlayer> getPlayers () {
		var server = getCurrentServer();

		if (server != null) {
			return server.getPlayerList().getPlayers();
		}

		return Collections.emptyList();
	}

	@Nullable
	@Override
	public MinecraftServer getCurrentServer () {
		return ServerLifecycleHooks.getCurrentServer();
	}

}
