package matterfactory.common.definition;

import matterfactory.core.Factory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Supplier;

public record RecipeDefinition<T extends Recipe<?>>(@NotNull String name, @NotNull Supplier<RecipeType<T>> type, @NotNull Supplier<RecipeSerializer<T>> serializer) {

	public RecipeType<T> getType () {
		return type.get();
	}

	public RecipeSerializer<T> getSerializer () {
		return serializer.get();
	}

	public String getName () {
		return name;
	}

	public String getResourceName () {
		return getName().toLowerCase(Locale.ROOT).replace(" ", "_");
	}

	public Identifier getIdentifier () {
		return Factory.get(getResourceName());
	}

}
