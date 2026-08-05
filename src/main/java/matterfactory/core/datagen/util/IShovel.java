package matterfactory.core.datagen.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public interface IShovel extends IMiningTier {

	@Override
	default TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_STONE_TOOL;
	}

}
