package com.iso2t.matterfactory.core.datagen.language;

import com.iso2t.matterfactory.common.registries.FBlocks;
import com.iso2t.matterfactory.common.registries.FItems;
import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class EnglishLangProvider extends LanguageProvider {

	public EnglishLangProvider (DataGenerator generator) {
		super(generator.getPackOutput(), com.iso2t.matterfactory.core.Factory.MODID, "en_us");
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
		add("itemGroup." + com.iso2t.matterfactory.core.Factory.MODID, com.iso2t.matterfactory.core.Factory.NAME);
	}

	protected void addSubtitles() {

	}

}
