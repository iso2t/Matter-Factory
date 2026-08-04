package matterfactory.common.block;

import lombok.Getter;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Supplier;

public class MachineBlock extends BaseBlock {

	@Getter
	private final Type type;

	public MachineBlock(Type type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public enum Type {
		MACHINE_FRAME(() -> Properties.ofFullCopy(Blocks.IRON_BLOCK)),
		MACHINE_CASING(() -> Properties.ofFullCopy(Blocks.IRON_BLOCK));

		private final Supplier<Properties> properties;

		Type(Supplier<Properties> properties) {
			this.properties = properties;
		}

		public Properties createProperties() {
			return properties.get();
		}
	}

}
