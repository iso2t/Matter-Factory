package matterfactory.common.recipes.builder;

import matterfactory.common.recipes.ElectricFurnaceRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class ElectricFurnaceRecipeBuilder {

	public static void build (RecipeOutput recipeOutput, Identifier identifier, Ingredient input, ItemStack output, ItemStack secondaryOutput, float secondaryOutputChance, float energy) {
		recipeOutput.accept(ResourceKey.create(Registries.RECIPE, identifier), new ElectricFurnaceRecipe(input, output, secondaryOutput, secondaryOutputChance, energy), null);
	}

}
