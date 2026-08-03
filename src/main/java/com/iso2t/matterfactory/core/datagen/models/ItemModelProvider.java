package com.iso2t.matterfactory.core.datagen.models;

import com.iso2t.matterfactory.common.registries.FItems;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import org.jetbrains.annotations.NotNull;

public final class ItemModelProvider {

	private ItemModelProvider () {
	}

	static void registerModels (@NotNull ItemModelGenerators itemGenerator) {
		for (var item : FItems.getItems()) {
			itemGenerator.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
		}
	}

}
