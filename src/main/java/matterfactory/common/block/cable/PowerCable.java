package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.core.Tier;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
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
	public boolean canConnectTo (LevelReader level, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		if (neighborState.getBlock() instanceof PowerCable) {
			return !CableBlock.isManuallyDisconnected(neighborState, direction.getOpposite());
		}

		if (level instanceof Level realLevel) {
			var blockEntity = realLevel.getBlockEntity(neighborPos);
			return realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
					|| realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
		}

		return false;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker (@NonNull Level level, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
		return type == getBlockEntityType() ? (tickLevel, pos, tickState, blockEntity) -> PowerCableBlockEntity.serverTick(tickLevel, pos, tickState, (PowerCableBlockEntity) blockEntity) : null;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
