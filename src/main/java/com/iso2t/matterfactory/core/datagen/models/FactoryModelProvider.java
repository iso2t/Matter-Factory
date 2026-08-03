package com.iso2t.matterfactory.core.datagen.models;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.stream.Stream;

public final class FactoryModelProvider extends ModelProviders {

	public FactoryModelProvider (PackOutput output) {
		super(output);
	}

	@Override
	public @NonNull String getName () {
		return "Model Definitions - " + com.iso2t.matterfactory.core.Factory.MODID;
	}

	@Override
	protected @NotNull Stream<? extends Holder<Item>> getKnownItems () {
		return BuiltInRegistries.ITEM.listElements()
				.filter(holder -> holder.getKey().identifier().getNamespace().equals(com.iso2t.matterfactory.core.Factory.MODID));
	}

	@Override
	protected void registerModels (@NotNull BlockModelGenerators blockGenerator, @NotNull ItemModelGenerators itemGenerator) {
		BlockModelProvider.registerModels(blockGenerator);
		ItemModelProvider.registerModels(itemGenerator);
	}

}
