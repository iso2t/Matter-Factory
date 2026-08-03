package com.iso2t.matterfactory.common.definition;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlockDefinition<T extends Block> implements ItemLike {

	@Getter
	private final String                    englishName;
	@Getter
	private final ItemDefinition<BlockItem> item;
	private final DeferredBlock<T>          block;

	public BlockDefinition (String englishName, DeferredBlock<T> block, ItemDefinition<BlockItem> item) {
		this.englishName = englishName;
		this.item = Objects.requireNonNull(item, "item");
		this.block = Objects.requireNonNull(block, "block");
	}

	public String getRegistryFriendlyName () {
		return englishName.toLowerCase().replace(' ', '_');
	}

	public Identifier getId () {
		return block.getId();
	}

	public final T getBlock () {
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