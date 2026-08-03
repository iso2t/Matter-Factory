package com.iso2t.matterfactory.core.datagen.models;

import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

public abstract sealed class ModelProviders extends ModelProvider permits FactoryModelProvider {

	public ModelProviders (PackOutput output) {
		super(output, com.iso2t.matterfactory.core.Factory.MODID);
	}

}
