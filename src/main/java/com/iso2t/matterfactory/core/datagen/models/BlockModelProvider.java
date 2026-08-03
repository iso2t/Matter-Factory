package com.iso2t.matterfactory.core.datagen.models;

import com.iso2t.matterfactory.common.definition.BlockDefinition;
import com.iso2t.matterfactory.common.registries.FBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

import static net.minecraft.client.data.models.BlockModelGenerators.createSimpleBlock;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

public non-sealed class BlockModelProvider extends ModelProviders {

	private BlockModelGenerators generators;

	public BlockModelProvider (PackOutput output) {
		super(output);
	}

	@Override
	protected @NotNull Stream<? extends Holder<Item>> getKnownItems () {
		return BuiltInRegistries.ITEM.listElements()
				.filter(holder -> holder.getKey().identifier().getNamespace().equals(com.iso2t.matterfactory.core.Factory.MODID))
				.filter(holder -> holder.value() instanceof BlockItem);
	}

	@Override
	protected void registerBlockModels (@NotNull BlockModelGenerators blockGenerator, @NotNull ItemModelGenerators itemGenerator) {
		this.generators = blockGenerator;

		for (var block : FBlocks.getBlocks()) {
			blockWithItem(block);
		}
	}

	private void blockWithItem (BlockDefinition<?> block) {
		var model = TexturedModel.CUBE.create(block.getBlock(), generators.modelOutput);
		generators.blockStateOutput.accept(createSimpleBlock(block.getBlock(), plainVariant(model)));
	}

}
