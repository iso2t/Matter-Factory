package matterfactory.common.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import matterfactory.common.recipes.ingredients.SimpleItemIngredient;
import matterfactory.common.registries.FactoryRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public record ElectricFurnaceRecipe(Ingredient input, ItemStack output, ItemStack secondaryOutput, float secondaryOutputChance, float energy) implements FactoryRecipe<SimpleItemIngredient> {

	public static final MapCodec<ElectricFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.CODEC.fieldOf("input").forGetter(ElectricFurnaceRecipe::input),
			ItemStack.CODEC.fieldOf("output").forGetter(ElectricFurnaceRecipe::output),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("secondary_output", ItemStack.EMPTY).forGetter(ElectricFurnaceRecipe::secondaryOutput),
			Codec.FLOAT.optionalFieldOf("secondary_output_chance", 0.0F).forGetter(ElectricFurnaceRecipe::secondaryOutputChance),
			Codec.FLOAT.fieldOf("energy").forGetter(ElectricFurnaceRecipe::energy)
	).apply(instance, ElectricFurnaceRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ElectricFurnaceRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, ElectricFurnaceRecipe::input,
			ItemStack.STREAM_CODEC, ElectricFurnaceRecipe::output,
			ItemStack.OPTIONAL_STREAM_CODEC, ElectricFurnaceRecipe::secondaryOutput,
			ByteBufCodecs.FLOAT, ElectricFurnaceRecipe::secondaryOutputChance,
			ByteBufCodecs.FLOAT, ElectricFurnaceRecipe::energy,
			ElectricFurnaceRecipe::new
	);

	public static final RecipeSerializer<ElectricFurnaceRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	@Override
	public boolean matches (@NonNull SimpleItemIngredient ingredient, Level level) {
		if (level.isClientSide()) return false;
		return input.test(ingredient.getItem(0));
	}

	@Override
	public @NonNull ItemStack assemble (@NonNull SimpleItemIngredient ingredient) {
		return output.copy();
	}

	@Override
	public boolean showNotification () {
		return false;
	}

	@Override
	public @NonNull String group () {
		return "";
	}

	@Override
	public @NonNull RecipeSerializer<? extends Recipe<SimpleItemIngredient>> getSerializer () {
		return FactoryRecipes.ELECTRIC_FURNACE_RECIPE.getSerializer();
	}

	@Override
	public @NonNull RecipeType<? extends Recipe<SimpleItemIngredient>> getType () {
		return FactoryRecipes.ELECTRIC_FURNACE_RECIPE.getType();
	}

	@Override
	public @NonNull PlacementInfo placementInfo () {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public @NonNull RecipeBookCategory recipeBookCategory () {
		return RecipeBookCategories.FURNACE_MISC;
	}

}
