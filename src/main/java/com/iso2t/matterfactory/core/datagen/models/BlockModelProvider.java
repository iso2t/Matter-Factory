package com.iso2t.matterfactory.core.datagen.models;

import com.iso2t.matterfactory.common.definition.BlockDefinition;
import com.iso2t.matterfactory.common.registries.FBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.TexturedModel;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.client.data.models.BlockModelGenerators.createSimpleBlock;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

public final class BlockModelProvider {

	private BlockModelProvider () {
	}

	static void registerModels (@NotNull BlockModelGenerators blockGenerator) {
		for (var block : FBlocks.getBlocks()) {
			blockWithItem(blockGenerator, block);
		}
	}

	private static void blockWithItem (@NotNull BlockModelGenerators blockGenerator, BlockDefinition<?> block) {
		var model = TexturedModel.CUBE.create(block.getBlock(), blockGenerator.modelOutput);
		blockGenerator.blockStateOutput.accept(createSimpleBlock(block.getBlock(), plainVariant(model)));
		blockGenerator.registerSimpleItemModel(block.getBlock(), model);
	}

}
