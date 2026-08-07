package matterfactory.compat.jei;

import matterfactory.core.Factory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

@JeiPlugin
public class FactoryJeiPlugin implements IModPlugin {

	@Override
	public @NonNull Identifier getPluginUid () {
		return Factory.get("jei_plugin");
	}

	@Override
	public void registerCategories (@NonNull IRecipeCategoryRegistration registration) {

	}

	@Override
	public void registerRecipes (@NonNull IRecipeRegistration registration) {

	}

	@Override
	public void registerGuiHandlers (@NonNull IGuiHandlerRegistration registration) {

	}

	@Override
	public void registerRecipeCatalysts (@NonNull IRecipeCatalystRegistration registration) {

	}

	/*private void screenAdapter(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(BaseScreen.class, new IGuiContainerHandler<>() {
			@Override
			public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull BaseScreen screen) {
				if (!screen.isSettingsPanelOpen()) return List.of();

				int x = screen.getGuiLeft() + screen.modifiedWidth();
				int y = screen.getGuiTop();
				int w = screen.getImageWidth() + screen.modifiedWidth() + 80;
				int h = screen.getImageHeight();

				return List.of(new Rect2i(x, y, w, h));
			}
		});
	}*/

}
