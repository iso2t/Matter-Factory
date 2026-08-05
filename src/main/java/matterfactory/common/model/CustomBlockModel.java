package matterfactory.common.model;

import com.mojang.math.Quadrant;
import matterfactory.common.block.cable.CableBlock;
import matterfactory.common.definition.BlockDefinition;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.List;
import java.util.Optional;

public interface CustomBlockModel {

	void registerModel (BlockModelGenerators generator, BlockDefinition<?> block);

	static void registerCableType (BlockModelGenerators generators, CableBlock block) {
		registerCableModels(generators, block);
	}

	private static void registerCableModels (BlockModelGenerators generators, CableBlock cable) {
		Identifier texture = TextureMapping.getBlockTexture(cable).sprite().withPath(path -> path.replace("block/", "block/cable/"));
		TextureMapping textures = new TextureMapping()
				.put(CableBlock.CABLE_CENTER_TEXTURE, new Material(texture.withPath(path -> path + "_center")))
				.put(CableBlock.CABLE_ARM_TEXTURE, new Material(texture.withPath(path -> path + "_arm")))
				.put(TextureSlot.PARTICLE, new Material(texture.withPath(path -> path + "_center")));

		Identifier centerModel = CableBlock.CENTER_MODEL.create(cable, textures, generators.modelOutput);
		Identifier armModel = CableBlock.ARM_MODEL.create(cable, textures, generators.modelOutput);
		Identifier itemModel = CableBlock.ITEM_MODEL.create(cable, textures, generators.modelOutput);
		Identifier guiModel = CableBlock.GUI_MODEL.create(cable, textures, generators.modelOutput);

		registerCableBlockState(generators, cable, centerModel, armModel);
		registerCableItemModel(generators, cable, itemModel, guiModel);
	}

	private static void registerCableBlockState (BlockModelGenerators generators, CableBlock cable, Identifier centerModel, Identifier armModel) {
		MultiVariant center = BlockModelGenerators.variant(new Variant(centerModel));

		Variant northArm = new Variant(armModel);

		MultiPartGenerator multipart = MultiPartGenerator.multiPart(cable).with(center).with(condition(CableBlock.NORTH), BlockModelGenerators.variant(northArm)).with(condition(CableBlock.EAST), BlockModelGenerators.variant(northArm.with(VariantMutator.Y_ROT.withValue(Quadrant.R90)))).with(condition(CableBlock.SOUTH), BlockModelGenerators.variant(northArm.with(VariantMutator.Y_ROT.withValue(Quadrant.R180)))).with(condition(CableBlock.WEST), BlockModelGenerators.variant(northArm.with(VariantMutator.Y_ROT.withValue(Quadrant.R270)))).with(condition(CableBlock.UP), BlockModelGenerators.variant(northArm.with(VariantMutator.X_ROT.withValue(Quadrant.R270)))).with(condition(CableBlock.DOWN), BlockModelGenerators.variant(northArm.with(VariantMutator.X_ROT.withValue(Quadrant.R90))));

		generators.blockStateOutput.accept(multipart);
	}

	private static void registerCableItemModel (BlockModelGenerators generators, CableBlock cable, Identifier itemModel, Identifier guiModel) {
		ItemModel.Unbaked normalModel = ItemModelUtils.plainModel(itemModel);

		ItemModel.Unbaked inventoryModel = ItemModelUtils.plainModel(guiModel);

		SelectItemModel.UnbakedSwitch<DisplayContext, ItemDisplayContext> displayContextSwitch = new SelectItemModel.UnbakedSwitch<>(new DisplayContext(), List.of(new SelectItemModel.SwitchCase<>(List.of(ItemDisplayContext.GUI), inventoryModel)));

		generators.itemModelOutput.accept(cable.asItem(), new SelectItemModel.Unbaked(Optional.empty(), displayContextSwitch, Optional.of(normalModel)));
	}

	private static Condition condition (BooleanProperty property) {
		return BlockModelGenerators.condition().term(property, true).build();
	}

}
