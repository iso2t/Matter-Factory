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
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.block.dispatch.multipart.CombinedCondition;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.resources.model.sprite.Material;
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
		Identifier straightModel = CableBlock.STRAIGHT_MODEL.create(cable, textures, generators.modelOutput);
		Identifier itemModel = CableBlock.ITEM_MODEL.create(cable, textures, generators.modelOutput);
		Identifier guiModel = CableBlock.GUI_MODEL.create(cable, textures, generators.modelOutput);

		registerCableBlockState(generators, cable, centerModel, armModel, straightModel);
		registerCableItemModel(generators, cable, itemModel, guiModel);
	}

	private static void registerCableBlockState (BlockModelGenerators generators, CableBlock cable, Identifier centerModel, Identifier armModel, Identifier straightModel) {
		MultiVariant center = BlockModelGenerators.variant(new Variant(centerModel));

		MultiPartGenerator multipart = MultiPartGenerator.multiPart(cable)
				.with(centerCondition(), center)
				.with(straightNorthSouth(), BlockModelGenerators.variant(new Variant(straightModel)))
				.with(straightEastWest(), BlockModelGenerators.variant(new Variant(straightModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R90))))
				.with(straightUpDown(), BlockModelGenerators.variant(new Variant(straightModel).with(VariantMutator.X_ROT.withValue(Quadrant.R90))))
				.with(armCondition(CableBlock.NORTH), BlockModelGenerators.variant(new Variant(armModel)))
				.with(armCondition(CableBlock.EAST), BlockModelGenerators.variant(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R90))))
				.with(armCondition(CableBlock.SOUTH), BlockModelGenerators.variant(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R180))))
				.with(armCondition(CableBlock.WEST), BlockModelGenerators.variant(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R270))))
				.with(armCondition(CableBlock.UP), BlockModelGenerators.variant(new Variant(armModel).with(VariantMutator.X_ROT.withValue(Quadrant.R270))))
				.with(armCondition(CableBlock.DOWN), BlockModelGenerators.variant(new Variant(armModel).with(VariantMutator.X_ROT.withValue(Quadrant.R90))));

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

	private static Condition armCondition (BooleanProperty property) {
		return and(condition(property), notStraight());
	}

	private static Condition centerCondition () {
		return notStraight();
	}

	private static Condition notStraight () {
		return and(notStraightNorthSouth(), notStraightEastWest(), notStraightUpDown());
	}

	private static Condition straightNorthSouth () {
		return BlockModelGenerators.condition().term(CableBlock.NORTH, true).term(CableBlock.SOUTH, true).term(CableBlock.EAST, false).term(CableBlock.WEST, false).term(CableBlock.UP, false).term(CableBlock.DOWN, false).build();
	}

	private static Condition straightEastWest () {
		return BlockModelGenerators.condition().term(CableBlock.EAST, true).term(CableBlock.WEST, true).term(CableBlock.NORTH, false).term(CableBlock.SOUTH, false).term(CableBlock.UP, false).term(CableBlock.DOWN, false).build();
	}

	private static Condition straightUpDown () {
		return BlockModelGenerators.condition().term(CableBlock.UP, true).term(CableBlock.DOWN, true).term(CableBlock.NORTH, false).term(CableBlock.SOUTH, false).term(CableBlock.EAST, false).term(CableBlock.WEST, false).build();
	}

	private static Condition notStraightNorthSouth () {
		return or(BlockModelGenerators.condition().term(CableBlock.NORTH, false).build(), BlockModelGenerators.condition().term(CableBlock.SOUTH, false).build(), BlockModelGenerators.condition().term(CableBlock.EAST, true).build(), BlockModelGenerators.condition().term(CableBlock.WEST, true).build(), BlockModelGenerators.condition().term(CableBlock.UP, true).build(), BlockModelGenerators.condition().term(CableBlock.DOWN, true).build());
	}

	private static Condition notStraightEastWest () {
		return or(BlockModelGenerators.condition().term(CableBlock.EAST, false).build(), BlockModelGenerators.condition().term(CableBlock.WEST, false).build(), BlockModelGenerators.condition().term(CableBlock.NORTH, true).build(), BlockModelGenerators.condition().term(CableBlock.SOUTH, true).build(), BlockModelGenerators.condition().term(CableBlock.UP, true).build(), BlockModelGenerators.condition().term(CableBlock.DOWN, true).build());
	}

	private static Condition notStraightUpDown () {
		return or(BlockModelGenerators.condition().term(CableBlock.UP, false).build(), BlockModelGenerators.condition().term(CableBlock.DOWN, false).build(), BlockModelGenerators.condition().term(CableBlock.NORTH, true).build(), BlockModelGenerators.condition().term(CableBlock.SOUTH, true).build(), BlockModelGenerators.condition().term(CableBlock.EAST, true).build(), BlockModelGenerators.condition().term(CableBlock.WEST, true).build());
	}

	private static Condition and (Condition... conditions) {
		return new CombinedCondition(CombinedCondition.Operation.AND, List.of(conditions));
	}

	private static Condition or (Condition... conditions) {
		return new CombinedCondition(CombinedCondition.Operation.OR, List.of(conditions));
	}

}
