package com.iso2t.matterfactory.common.registries;

import com.google.common.base.Preconditions;
import com.iso2t.matterfactory.common.FTab;
import com.iso2t.matterfactory.common.definition.ItemDefinition;
import com.iso2t.matterfactory.core.Factory;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class FItems {

	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(Factory.MODID);

	private static final List<ItemDefinition<?>> ITEMS = new ArrayList<>();

	public static final ItemDefinition<Item> PALLADIUM_INGOT  = register("Palladium Ingot", Item::new);
	public static final ItemDefinition<Item> PALLADIUM_NUGGET = register("Palladium Nugget", Item::new);
	public static final ItemDefinition<Item> PALLADIUM_DUST   = register("Palladium Dust", Item::new);
	public static final ItemDefinition<Item> PALLADIUM_RAW    = register("Raw Palladium", "palladium_raw", Item::new);

	public static List<ItemDefinition<?>> getItems () {
		return Collections.unmodifiableList(ITEMS);
	}

	public static <T extends Item> ItemDefinition<T> register (String name, Function<Item.Properties, T> factory) {
		String resourceFriendly = name.toLowerCase().replace(' ', '_');
		return register(name, Factory.get(resourceFriendly), factory, FTab.MAIN);
	}

	public static <T extends Item> ItemDefinition<T> register (final String name, String resourceName, Function<Item.Properties, T> factory) {
		return register(name, Factory.get(resourceName), factory, FTab.MAIN);
	}

	public static <T extends Item> ItemDefinition<T> register (String name, Identifier id, Function<Item.Properties, T> factory, @Nullable ResourceKey<CreativeModeTab> group) {
		Preconditions.checkArgument(id.getNamespace().equals(Factory.MODID), "Can only register items in " + Factory.MODID);
		var definition = new ItemDefinition<>(name, REGISTRY.registerItem(id.getPath(), factory));

		if (Objects.equals(group, FTab.MAIN)) {
			FTab.add(definition);
		} else if (group != null) {
			FTab.addExternal(group, definition);
		}

		ITEMS.add(definition);
		return definition;
	}

}
