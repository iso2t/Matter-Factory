package matterfactory.client.renderer.gui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GuiProgressBarRenderer extends AbstractBarRenderer {

	@Getter
	private int progress = 0;

	public GuiProgressBarRenderer (int xMin, int yMin) {
		this(xMin, yMin, 3, 22);
	}

	public GuiProgressBarRenderer (int xMin, int yMin, int width, int height) {
		super(xMin, yMin, width, height);
	}

	public List<Component> getTooltips () {
		return List.of(Component.literal(progress * 100 / getHeight() + "%"));
	}

	@Override
	public void render (GuiGraphicsExtractor guiGraphics) {
	}

	public void render (GuiGraphicsExtractor guiGraphics, int amount) {
		render(guiGraphics, amount, 0, 0);
	}

	public void render (GuiGraphicsExtractor guiGraphics, int amount, int x, int y) {
		this.progress = amount;
		guiGraphics.fillGradient(x, y + (getHeight() - amount), x + getWidth(), y + getHeight(), Color.GREEN.getArgb(), Color.BRIGHT_GREEN.getArgb());
	}

}
