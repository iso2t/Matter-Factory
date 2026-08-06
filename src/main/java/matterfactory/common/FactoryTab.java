package matterfactory.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import matterfactory.common.block.BaseBlock;
import matterfactory.common.definition.ItemDefinition;
import matterfactory.common.item.BaseBlockItem;
import matterfactory.common.item.BaseItem;
import matterfactory.common.registries.FactoryBlocks;
import matterfactory.core.Factory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.List;

public class FactoryTab {

	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Factory.get("main"));

	private static final Multimap<ResourceKey<CreativeModeTab>, ItemDefinition<?>> externalItemDefs = HashMultimap.create();
	private static final List<ItemDefinition<?>>                                   itemDefs         = new ArrayList<>();

	public static void init (Registry<CreativeModeTab> registry) {
		var tab = CreativeModeTab.builder()
				.title(Component.translatable("itemGroup." + Factory.MODID))
				.icon(FactoryBlocks.MACHINE_FRAME::getStack)
				.displayItems(FactoryTab::buildDisplayItems)
				.build();
		Registry.register(registry, MAIN, tab);
	}

	public static void initExternal (BuildCreativeModeTabContentsEvent contents) {
		for (var itemDefinition : externalItemDefs.get(contents.getTabKey())) {
			contents.accept(itemDefinition);
		}
	}

	public static void add (ItemDefinition<?> itemDef) {
		itemDefs.add(itemDef);
	}

	public static void addExternal (ResourceKey<CreativeModeTab> tab, ItemDefinition<?> itemDef) {
		externalItemDefs.put(tab, itemDef);
	}

	private static void buildDisplayItems (CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
		for (var itemDef : itemDefs) {
			var item = itemDef.asItem();
			if (item instanceof BaseBlockItem baseItem && baseItem.getBlock() instanceof BaseBlock baseBlock) {
				baseBlock.addToCreativeTab(output);
			} else if (item instanceof BaseItem baseItem) {
				baseItem.addToCreativeTab(output);
			} else {
				output.accept(itemDef);
			}
		}
	}

}
