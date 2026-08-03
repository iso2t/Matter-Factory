package com.iso2t.matterfactory.common.registries;

import com.iso2t.matterfactory.common.FTab;
import com.iso2t.matterfactory.common.definition.BlockDefinition;
import com.iso2t.matterfactory.common.definition.ItemDefinition;
import com.iso2t.matterfactory.core.Factory;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FBlocks {

	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(Factory.MODID);

	public static final List<BlockDefinition<?>> BLOCKS = new ArrayList<>();

	public static List<BlockDefinition<?>> getBlocks () {
		return Collections.unmodifiableList(BLOCKS);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, final Supplier<T> supplier) {
		String resourceFriendly = name.toLowerCase().replace(' ', '_');
		return register(name, Factory.get(resourceFriendly), supplier, null, true);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, String resourceName, final Supplier<T> supplier) {
		return register(name, Factory.get(resourceName), supplier, null, true);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, Identifier id, final Supplier<T> supplier, @Nullable BiFunction<Block, Item.Properties, BlockItem> itemFactory, boolean addToTab) {
		return register(name, id, supplier, itemFactory, addToTab, FTab.MAIN);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, Identifier id, final Supplier<T> supplier, @Nullable BiFunction<Block, Item.Properties, BlockItem> itemFactory, boolean addToTab, @Nullable ResourceKey<CreativeModeTab> group) {
		var deferredBlock = REGISTRY.register(id.getPath(), supplier);
		var deferredItem = FItems.REGISTRY.register(id.getPath(), () -> {
			var block = deferredBlock.get();
			var itemProperties = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
			if (itemFactory != null) {
				return itemFactory.apply(block, itemProperties);
			} else throw new IllegalArgumentException("BlockItem factory for " + id + " returned null.");
		});
		var itemDef = new ItemDefinition<>(name, deferredItem);
		if (addToTab) {
			if (Objects.equals(group, FTab.MAIN)) {
				FTab.add(itemDef);
			} else {
				FTab.addExternal(group, itemDef);
			}
		}
		BlockDefinition<T> definition = new BlockDefinition<>(name, deferredBlock, itemDef);
		BLOCKS.add(definition);
		return definition;
	}

}
