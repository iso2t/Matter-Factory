package matterfactory.core.datagen.models;

import matterfactory.common.block.machine.AbstractMachineEntityBlock;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.model.CustomBlockModel;
import matterfactory.common.registries.FactoryBlocks;
import matterfactory.core.Factory;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.client.data.models.BlockModelGenerators.createSimpleBlock;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

public final class BlockModelProvider {

	private BlockModelProvider () {
	}

	static void registerModels (@NotNull BlockModelGenerators blockGenerator) {
		for (var block : FactoryBlocks.getBlocks()) {
			if (block.getBlock() instanceof CustomBlockModel custom) custom.registerModel(blockGenerator, block);
			else if (block.getBlock() instanceof AbstractMachineEntityBlock<?> machine) machine(blockGenerator, block);
			else blockWithItem(blockGenerator, block);
		}
	}

	private static void blockWithItem (@NotNull BlockModelGenerators blockGenerator, BlockDefinition<?> block) {
		var model = TexturedModel.CUBE.create(block.getBlock(), blockGenerator.modelOutput);
		blockGenerator.blockStateOutput.accept(createSimpleBlock(block.getBlock(), plainVariant(model)));
		blockGenerator.registerSimpleItemModel(block.getBlock(), model);
	}

	private static void machine (@NotNull BlockModelGenerators blockGenerator, BlockDefinition<?> block) {
		var sideTexture = Factory.get("block/machine_side");
		var sideTextureItem = Factory.get("block/machine_side_item");
		var sideTextureEnergy = Factory.get("block/machine_side_energy");
		var sideTextureFluid = Factory.get("block/machine_side_fluid");
		var faceTexture = TextureMapping.getBlockTexture(block.getBlock()).sprite();
		var inactiveModel = createMachineModel(blockGenerator, block, sideTexture, faceTexture, "_inactive");
		var activeModel = createMachineModel(blockGenerator, block, sideTexture, faceTexture.withSuffix("_active"), "_active");

		blockGenerator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.getBlock())
			.with(PropertyDispatch.initial(AbstractMachineEntityBlock.ACTIVE)
				.select(false, plainVariant(inactiveModel))
				.select(true, plainVariant(activeModel))));
		blockGenerator.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(inactiveModel));
	}

	private static Identifier createMachineModel (@NotNull BlockModelGenerators blockGenerator, BlockDefinition<?> block, Identifier sideTexture, Identifier faceTexture, String suffix) {
		return TexturedModel.ORIENTABLE.updateTexture(textures -> {
			var side = new Material(sideTexture);
			var face = new Material(faceTexture);
			textures.put(TextureSlot.SIDE, side)
				.put(TextureSlot.TOP, side)
				.put(TextureSlot.BOTTOM, side)
				.put(TextureSlot.FRONT, face)
				.put(TextureSlot.PARTICLE, face);
		}).createWithSuffix(block.getBlock(), suffix, blockGenerator.modelOutput);
	}

}
