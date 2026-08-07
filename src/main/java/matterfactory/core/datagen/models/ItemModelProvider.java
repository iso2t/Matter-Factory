package matterfactory.core.datagen.models;

import matterfactory.client.color.FluidItemTintSource;
import matterfactory.common.registries.FactoryFluids;
import matterfactory.common.registries.FactoryItems;
import matterfactory.core.Factory;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.world.item.BucketItem;
import org.jetbrains.annotations.NotNull;

public final class ItemModelProvider {

	private ItemModelProvider () {
	}

	static void registerModels (@NotNull ItemModelGenerators itemGenerator) {
		for (var item : FactoryItems.getItems()) {

			if (item.asItem() instanceof BucketItem && FactoryFluids.getFluids().stream().anyMatch(fluid -> fluid.bucket().getId().equals(item.getId()))) {
				itemGenerator.itemModelOutput.accept(item.get(), ItemModelUtils.tintedModel(Factory.get("item/fluid_bucket"), ItemModelGenerators.BLANK_LAYER, FluidItemTintSource.INSTANCE));
				continue;
			}

			itemGenerator.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
		}
	}

}
