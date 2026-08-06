package matterfactory.core.datagen.language;

import matterfactory.common.registries.FactoryBlocks;
import matterfactory.common.registries.FactoryItems;
import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class EnglishLangProvider extends LanguageProvider {

	public EnglishLangProvider (DataGenerator generator) {
		super(generator.getPackOutput(), matterfactory.core.Factory.MODID, "en_us");
	}

	@Override
	protected void addTranslations() {
		addManualStrings();
		addSubtitles();

		items:
		for (var item : FactoryItems.getItems()) {
			add(item.asItem(), item.getEnglishName());
		}

		blocks:
		for (var block : FactoryBlocks.getBlocks()) {
			add(block.getBlock(), block.getEnglishName());
		}
	}

	protected void addManualStrings() {
		add("itemGroup." + matterfactory.core.Factory.MODID, matterfactory.core.Factory.NAME);
	}

	protected void addSubtitles() {
		add("subtitles.matterfactory.wrench_use", "Wrench Used");
	}

}
