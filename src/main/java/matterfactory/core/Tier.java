package matterfactory.core;

import lombok.Getter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum Tier {

	BASIC(8_192, 256),
	ADVANCED(65_536, 4_096),
	ELITE(524_288, 32_768),
	ULTIMATE(4_194_304, 262_144),
	INFINITE(Integer.MAX_VALUE, Integer.MAX_VALUE);

	@Getter
	final BlockBehaviour.Properties blockProperties;

	@Getter
	final int energyCapacity;

	@Getter
	final int energyTransferRate;

	Tier (BlockBehaviour.Properties blockProperties, int energyCapacity, int energyTransferRate) {
		this.blockProperties = blockProperties;
		this.energyCapacity = energyCapacity;
		this.energyTransferRate = energyTransferRate;
	}

	Tier (int energyCapacity, int energyTransferRate) {
		this(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON), energyCapacity, energyTransferRate);
	}

}
