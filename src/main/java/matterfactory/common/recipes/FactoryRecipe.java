package matterfactory.common.recipes;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

public interface FactoryRecipe<T extends RecipeInput> extends Recipe<T> {
}
