package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.core.Tier;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
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
			return !(level instanceof BlockGetter blockGetter) || !(getBlockEntity(blockGetter, neighborPos) instanceof PowerCableBlockEntity neighborCable) || !neighborCable.isManuallyDisconnected(direction.getOpposite());
		}

		if (level instanceof Level realLevel) {
			var blockEntity = realLevel.getBlockEntity(neighborPos);
			return realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
					|| realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
		}

		return false;
	}

	@Override
	protected boolean supportsManualDisconnect (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		return true;
	}

	@Override
	protected boolean supportsConnectionModes (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		if (!state.getValue(getConnectionProperty(direction))) {
			return false;
		}

		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = level.getBlockState(neighborPos);
		if (neighborState.getBlock() instanceof PowerCable || !(level instanceof Level realLevel)) {
			return false;
		}

		var blockEntity = realLevel.getBlockEntity(neighborPos);
		return realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
				|| realLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
	}

	@Override
	protected CableConnectionMode getConnectionMode (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		PowerCableBlockEntity blockEntity = getBlockEntity(level, pos);
		return blockEntity == null ? CableConnectionMode.AUTO : blockEntity.getConnectionMode(direction);
	}

	@Override
	protected void setConnectionMode (Level level, BlockPos pos, BlockState state, Direction direction, CableConnectionMode mode) {
		PowerCableBlockEntity blockEntity = getBlockEntity(level, pos);
		if (blockEntity != null) {
			blockEntity.setConnectionMode(direction, mode);
		}
	}

	@Override
	protected boolean isManuallyDisconnected (LevelReader level, BlockPos pos, BlockState state, Direction direction) {
		if (!(level instanceof BlockGetter blockGetter)) {
			return false;
		}

		PowerCableBlockEntity blockEntity = getBlockEntity(blockGetter, pos);
		return blockEntity != null && blockEntity.isManuallyDisconnected(direction);
	}

	@Override
	protected void setManuallyDisconnected (Level level, BlockPos pos, BlockState state, Direction direction, boolean disconnected) {
		PowerCableBlockEntity blockEntity = getBlockEntity(level, pos);
		if (blockEntity != null) {
			blockEntity.setManuallyDisconnected(direction, disconnected);
		}
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
