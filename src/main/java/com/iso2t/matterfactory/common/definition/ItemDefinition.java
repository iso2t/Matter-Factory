package com.iso2t.matterfactory.common.definition;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record ItemDefinition<T extends Item>(String englishName, DeferredItem<T> item) implements ItemLike, Supplier<T> {

	public String getRegistryFriendlyName () {
		return englishName.toLowerCase().replace(' ', '_');
	}

	public Identifier getId () {
		return this.item.getId();
	}

	public ItemStack getStack () {
		return getStack(1);
	}

	public ItemStack getStack (int stackSize) {
		return new ItemStack((ItemLike) item, stackSize);
	}

	public Holder<Item> getHolder () {
		return item;
	}

	public String getEnglishName () {
		return englishName;
	}

	@Override
	public T get () {
		return item.get();
	}

	@Override
	public @NotNull T asItem () {
		return item.get();
	}
}
