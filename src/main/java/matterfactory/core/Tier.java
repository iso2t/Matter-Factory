package matterfactory.core;

import lombok.Getter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum Tier {

	BASIC(8_192, 256, 1024, 1024, 1),
	ADVANCED(65_536, 4_096, 2048, 2048, 16),
	ELITE(524_288, 32_768, 4_096, 4_096, 64),
	ULTIMATE(4_194_304, 262_144, 8_192, 8_192, 128),
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
