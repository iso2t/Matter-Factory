package matterfactory.client.renderer.gui;

import lombok.Getter;
import lombok.Setter;
import matterfactory.core.Factory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntSupplier;

public class GuiPowerRenderer extends AbstractBarRenderer {

	@Getter
	private final IntSupplier energyStored;

	@Getter
	private final IntSupplier energyCapacity;

	@Getter
	@Setter
	private Color renderColor = Color.BRIGHT_GREEN;

	public GuiPowerRenderer (int xMin, int yMin, IntSupplier energyStored, IntSupplier energyCapacity) {
		this(xMin, yMin, energyStored, energyCapacity, 8, 64);
	}

	public GuiPowerRenderer (int xMin, int yMin, IntSupplier energyStored, IntSupplier energyCapacity, int width, int height) {
		super(xMin, yMin, width, height);
		this.energyStored = energyStored;
		this.energyCapacity = energyCapacity;
	}

	public List<Component> getTooltips () {
		int stored = getEnergyStored().getAsInt();
		int cap = getEnergyCapacity().getAsInt();
		return List.of(Component.literal(stored + " / " + cap + " %s".formatted(Factory.POWER_UNIT.getAbbreviation())));
	}

	@Override
	public void render (GuiGraphicsExtractor guiGraphics) {
		/*int cap = getEnergyCapacity().getAsInt();
		if (cap <= 0) return;
		int storedPx = (int) (getHeight() * (getEnergyStored().getAsInt() / (float) cap));
		guiGraphics.fillGradient(getXPos(), getYPos() + (getHeight() - storedPx), getXPos() + getWidth(), getYPos() + getHeight(), Color.BRIGHT_RED.getArgb(), Color.RED.getArgb());*/
		render(guiGraphics, getXPos(), getYPos());
	}

	public void render (GuiGraphicsExtractor guiGraphics, int x, int y) {
		int cap = getEnergyCapacity().getAsInt();
		if (cap <= 0) return;
		int storedPx = (int) (getHeight() * (getEnergyStored().getAsInt() / (float) cap));
		guiGraphics.fillGradient(x, y + (getHeight() - storedPx), x + getWidth(), y + getHeight(), Color.BRIGHT_RED.getArgb(), getRenderColor().getArgb());
	}

}
