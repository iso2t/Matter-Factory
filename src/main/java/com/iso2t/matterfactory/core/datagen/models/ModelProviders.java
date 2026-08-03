package com.iso2t.matterfactory.core.datagen.models;

import com.iso2t.matterfactory.core.Factory;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

public abstract sealed class ModelProviders extends ModelProvider permits BlockModelProvider, ItemModelProvider {

	public ModelProviders (PackOutput output) {
		super(output, com.iso2t.matterfactory.core.Factory.MODID);
	}

	@Override
	protected void registerModels (@NotNull BlockModelGenerators blockGenerator, @NotNull ItemModelGenerators itemGenerator) {
		registerBlockModels(blockGenerator, itemGenerator);
		registerItemModels(itemGenerator);
	}

	protected void registerBlockModels (@NotNull BlockModelGenerators blockGenerator, @NotNull ItemModelGenerators itemGenerator) {
	}

	protected void registerItemModels (@NotNull ItemModelGenerators itemGenerator) {
	}

}
