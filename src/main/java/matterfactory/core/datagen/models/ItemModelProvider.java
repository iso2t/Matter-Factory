package matterfactory.core.datagen.models;

import matterfactory.common.registries.FactoryItems;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import org.jetbrains.annotations.NotNull;

public final class ItemModelProvider {

	private ItemModelProvider () {
	}

	static void registerModels (@NotNull ItemModelGenerators itemGenerator) {
		for (var item : FactoryItems.getItems()) {
			itemGenerator.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
		}
	}

}
