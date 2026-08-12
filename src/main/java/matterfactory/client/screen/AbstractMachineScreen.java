package matterfactory.client.screen;

import lombok.Getter;
import matterfactory.client.renderer.gui.GuiPowerRenderer;
import matterfactory.client.renderer.gui.GuiProgressBarRenderer;
import matterfactory.common.menu.AbstractMachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

public abstract class AbstractMachineScreen<T extends AbstractMachineMenu<?, ?>> extends AbstractContainerScreen<T> {

	@Getter
	private GuiPowerRenderer                powerRenderer;
	private GuiPowerRenderer.Size           powerRendererSize;
	private GuiPowerRenderer.RenderLocation powerRendererLocation;

	@Getter
	private GuiProgressBarRenderer progressBarRenderer;

	public AbstractMachineScreen (T menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	/**
	 * Sets the texture for the GUI
	 * @return {@link Identifier} path to the texture.
	 */
	public abstract Identifier getGuiTexture ();

	@Override
	protected void init () {
		super.init();
		this.powerRendererSize = GuiPowerRenderer.Size.getDefault();
		this.powerRendererLocation = GuiPowerRenderer.RenderLocation.getDefault();
		this.powerRenderer = new GuiPowerRenderer(powerRendererLocation, () -> 0, () -> 0, powerRendererSize);
	}

	@Override
	public void extractBackground (@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = this.leftPos;
		int yo = this.topPos;
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		graphics.blit(RenderPipelines.GUI_TEXTURED, getGuiTexture(), xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		powerRenderer.render(graphics, x + powerRendererLocation.left(), y + powerRendererLocation.top());
	}
}
