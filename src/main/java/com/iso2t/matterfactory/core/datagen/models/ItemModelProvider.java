package com.iso2t.matterfactory.core.datagen.models;

import com.iso2t.matterfactory.common.registries.FItems;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.stream.Stream;

public final class ItemModelProvider extends ModelProviders {

	public ItemModelProvider (PackOutput output) {
		super(output);
	}

	@Override
	public @NonNull String getName () {
		return "Model Definitions - " + com.iso2t.matterfactory.core.Factory.MODID + " (items)";
	}

	@Override
	protected @NotNull Stream<? extends Holder<Item>> getKnownItems () {
		return BuiltInRegistries.ITEM.listElements()
				.filter(holder -> holder.getKey().identifier().getNamespace().equals(com.iso2t.matterfactory.core.Factory.MODID))
				.filter(holder -> !(holder.value() instanceof BlockItem));
	}

	@Override
	protected void registerItemModels (@NotNull ItemModelGenerators itemGenerator) {
		for (var item : FItems.getItems()) {
			itemGenerator.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
		}
	}

}
