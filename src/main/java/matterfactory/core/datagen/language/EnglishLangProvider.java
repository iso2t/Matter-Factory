package matterfactory.core.datagen.language;

import matterfactory.common.registries.FactoryBlocks;
import matterfactory.common.registries.FactoryFluids;
import matterfactory.common.registries.FactoryItems;
import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class EnglishLangProvider extends LanguageProvider {

	public EnglishLangProvider (DataGenerator generator) {
		super(generator.getPackOutput(), matterfactory.core.Factory.MODID, "en_us");
	}

	@Override
	protected void addTranslations () {
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

		fluid_types:
		for (var fluid : FactoryFluids.getFluids()) {
			// TODO: This is kind of cheating ngl
			var key = "fluid_type." + matterfactory.core.Factory.MODID + "." + fluid.englishName().toLowerCase().replace(' ', '_');
			add(key, fluid.englishName());
		}
	}

	protected void addManualStrings () {
		add("itemGroup." + matterfactory.core.Factory.MODID, matterfactory.core.Factory.NAME);
	}

	protected void addSubtitles () {
		add("subtitles.matterfactory.wrench_use", "Wrench Used");
	}

}
