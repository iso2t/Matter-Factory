package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class PowerCable extends CableBlock {

	public static final MapCodec<PowerCable> CODEC = simpleCodec(properties -> new PowerCable(properties, Tier.BASIC));

	@Getter
	private final Tier tier;

	public PowerCable (Properties properties, Tier tier) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public @NonNull MapCodec<? extends CableBlock> getCodec () {
		return CODEC;
	}

	@Override
	public boolean canConnectTo (LevelReader level, BlockPos neighborPos, BlockState neighborState) {
		return neighborState.getBlock() instanceof PowerCable;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
