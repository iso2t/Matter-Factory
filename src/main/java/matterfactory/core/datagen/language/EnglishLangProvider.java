package matterfactory.core.datagen.language;

import matterfactory.common.registries.FBlocks;
import matterfactory.common.registries.FItems;
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
		for (var item : FItems.getItems()) {
			add(item.asItem(), item.getEnglishName());
		}

		blocks:
		for (var block : FBlocks.getBlocks()) {
			add(block.getBlock(), block.getEnglishName());
		}
	}

	protected void addManualStrings() {
		add("itemGroup." + matterfactory.core.Factory.MODID, matterfactory.core.Factory.NAME);
	}

	protected void addSubtitles() {

	}

}
