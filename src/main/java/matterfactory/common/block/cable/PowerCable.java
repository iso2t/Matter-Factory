package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.Tier;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.NonNull;

public class PowerCable extends EntityCableBlock<PowerCableBlockEntity> {

	public static final MapCodec<PowerCable> CODEC = simpleCodec(properties -> new PowerCable(properties, Tier.BASIC));

	@Getter
	private final Tier tier;

	public PowerCable (Properties properties, Tier tier) {
		super(properties.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE));
		this.tier = tier;
	}

	@Override
	public @NonNull MapCodec<? extends CableBlock> getCodec () {
		return CODEC;
	}

	@Override
	public boolean canConnectTo (LevelReader level, BlockPos neighborPos, BlockState neighborState) {
		if (neighborState.getBlock() instanceof PowerCable) {
			return true;
		}

		if (level instanceof Level realLevel) {
			return realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, realLevel.getBlockEntity(neighborPos), null) != null;
		}

		return false;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
