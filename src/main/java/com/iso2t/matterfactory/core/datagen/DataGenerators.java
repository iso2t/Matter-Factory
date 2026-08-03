package com.iso2t.matterfactory.core.datagen;

import com.iso2t.matterfactory.core.Factory;
import com.iso2t.matterfactory.core.datagen.language.EnglishLangProvider;
import com.iso2t.matterfactory.core.datagen.models.FactoryModelProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Factory.MODID)
public class DataGenerators {

	@SubscribeEvent
	public static void gather (@NotNull GatherDataEvent.Client event) {
		var generator = event.getGenerator();
		var registries = event.getLookupProvider();
		var pack = generator.getVanillaPack(true);
		var localization = new EnglishLangProvider(generator);

		// MODELS & STATES
		pack.addProvider(FactoryModelProvider::new);

		// LANGUAGES (MUST RUN LAST)
		pack.addProvider(_ -> localization);
	}

	@Contract(pure = true)
	private static <T extends DataProvider> DataProvider.@NotNull Factory<T> bindRegistries (BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> factory, CompletableFuture<HolderLookup.Provider> factories) {
		return pOutput -> factory.apply(pOutput, factories);
	}

}
