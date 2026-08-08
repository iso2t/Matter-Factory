package matterfactory.common.registries;

import matterfactory.common.definition.RecipeDefinition;
import matterfactory.common.recipes.ElectricFurnaceRecipe;
import matterfactory.core.Factory;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class FactoryRecipes {

	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Factory.MODID);
	public static final DeferredRegister<RecipeType<?>> REGISTRY = DeferredRegister.create(Registries.RECIPE_TYPE, Factory.MODID);

	private static final List<RecipeDefinition<?>> RECIPES = new ArrayList<>();

	public static final RecipeDefinition<ElectricFurnaceRecipe> ELECTRIC_FURNACE_RECIPE = register("Electric Furnace", () -> RecipeType.simple(Factory.get("electric_furnace")), () -> ElectricFurnaceRecipe.SERIALIZER);

	public static List<RecipeDefinition<?>> getRecipes () {
		return Collections.unmodifiableList(RECIPES);
	}

	private static <T extends Recipe<?>> RecipeDefinition<T> register (final @NotNull String name, @NotNull Supplier<RecipeType<T>> type, @NotNull Supplier<RecipeSerializer<T>> serializer) {
		var resourceFriendly = name.toLowerCase().replace(' ', '_');

		var recipeSerializer = SERIALIZER.register(resourceFriendly, serializer);
		var recipeType = REGISTRY.register(resourceFriendly, type);
		var definition = new RecipeDefinition<>(name, recipeType, recipeSerializer);
		RECIPES.add(definition);
		return definition;
	}

}
