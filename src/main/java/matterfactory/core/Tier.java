package matterfactory.core;

import lombok.Getter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum Tier {

	BASIC(16_384, 512, 1_024, 256, 1),
	ADVANCED(65_536, 2_048, 4_096, 1_024, 4),
	ELITE(262_144, 8_192, 16_384, 4_096, 16),
	ULTIMATE(1_048_576, 32_768, 65_536, 16_384, 64),
	INFINITE(Integer.MAX_VALUE, Integer.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Integer.MAX_VALUE);

	@Getter
	final BlockBehaviour.Properties blockProperties;

	@Getter
	final int energyCapacity;

	@Getter
	final int energyTransferRate;

	@Getter
	final float fluidCapacity; // in mB | 1024 = 1 bucket

	@Getter
	final float fluidTransferRate; // in mB/t | 1024 = 1 bucket

	@Getter
	final int itemTransferRate; // in items/t

	Tier (BlockBehaviour.Properties blockProperties, int energyCapacity, int energyTransferRate, float fluidCapacity, float fluidTransferRate, int itemTransferRate) {
		this.blockProperties = blockProperties;
		this.energyCapacity = energyCapacity;
		this.energyTransferRate = energyTransferRate;
		this.fluidCapacity = fluidCapacity;
		this.fluidTransferRate = fluidTransferRate;
		this.itemTransferRate = itemTransferRate;
	}

	Tier (int energyCapacity, int energyTransferRate, float fluidCapacity, float fluidTransferRate, int itemTransferRate) {
		this(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON), energyCapacity, energyTransferRate, fluidCapacity, fluidTransferRate, itemTransferRate);
	}

}
