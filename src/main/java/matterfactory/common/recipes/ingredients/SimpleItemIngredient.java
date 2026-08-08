package matterfactory.common.recipes.ingredients;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jspecify.annotations.NonNull;

public record SimpleItemIngredient(ItemStack input) implements RecipeInput {

	@Override
	public @NonNull ItemStack getItem (int slot) {
		return input;
	}

	@Override
	public int size () {
		return 1;
	}
}
