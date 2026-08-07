package matterfactory.common.block;

import lombok.Getter;
import matterfactory.core.datagen.util.IPickaxe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

import java.util.function.Supplier;

public class MachineBlock extends BaseBlock implements IPickaxe {

	@Getter
	private final Type type;

	public MachineBlock (Type type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public enum Type {
		MACHINE_FRAME(() -> Properties.of().requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.IRON)),
		MACHINE_CASING(() -> Properties.ofFullCopy(Blocks.IRON_BLOCK));

		private final Supplier<Properties> properties;

		Type (Supplier<Properties> properties) {
			this.properties = properties;
		}

		public Properties createProperties () {
			return properties.get();
		}
	}

}
