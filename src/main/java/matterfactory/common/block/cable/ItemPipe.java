package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.core.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class ItemPipe extends CableBlock {

	public static final MapCodec<ItemPipe> CODEC = simpleCodec(properties -> new ItemPipe(properties, Tier.BASIC));

	@Getter
	private final Tier tier;

	public ItemPipe (Properties properties, Tier tier) {
		super(properties.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE));
		this.tier = tier;
	}

	@Override
	public @NonNull MapCodec<? extends CableBlock> getCodec () {
		return CODEC;
	}

	@Override
	public boolean canConnectTo (LevelReader level, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return neighborState.getBlock() instanceof ItemPipe;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
