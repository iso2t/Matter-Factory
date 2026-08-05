package matterfactory;

import lombok.Getter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum Tier {

	BASIC(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON)),
	ADVANCED(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON)),
	ELITE(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON)),
	ULTIMATE(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON)),
	INFINITE(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON));

	@Getter
	final BlockBehaviour.Properties blockProperties;
	Tier (BlockBehaviour.Properties blockProperties) {
		this.blockProperties = blockProperties;
	}

}
