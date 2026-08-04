package com.iso2t.matterfactory.common.definition;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

public record BlockDefinition<T extends Block>(String englishName, DeferredBlock<T> block, ItemDefinition<BlockItem> item) implements ItemLike {

	public String getRegistryFriendlyName () {
		return englishName.toLowerCase().replace(' ', '_');
	}

	public Identifier getId () {
		return block.getId();
	}

	public String getEnglishName () {
		return englishName;
	}

	public T getBlock () {
		return this.block.get();
	}

	public ItemStack getStack () {
		return item.getStack();
	}

	public ItemStack getStack (int stackSize) {
		return item.getStack(stackSize);
	}

	@Override
	public @NotNull Item asItem () {
		return item.asItem();
	}
}