package matterfactory;

import lombok.Getter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum Tier {

	BASIC(8192, 1024),
	ADVANCED(65536, 8192),
	ELITE(524288, 65536),
	ULTIMATE(4194304, 524288),
	INFINITE(Integer.MAX_VALUE, Integer.MAX_VALUE);

	@Getter
	final BlockBehaviour.Properties blockProperties;

	@Getter
	final int capacity;

	@Getter
	final int transferRate;

	Tier (BlockBehaviour.Properties blockProperties, int capacity, int transferRate) {
		this.blockProperties = blockProperties;
		this.capacity = capacity;
		this.transferRate = transferRate;
	}

	Tier (int capacity, int transferRate) {
		this(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON), capacity, transferRate);
	}

}
