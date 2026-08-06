package matterfactory.common.block.cable;

import lombok.Getter;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class EntityCableBlock<T extends BaseCableBlockEntity> extends CableBlock implements EntityBlock {

	private Class<T> blockEntityClass;

	@Getter
	private BlockEntityType<T> blockEntityType;

	public EntityCableBlock (Properties properties) {
		super(properties);
	}

	public void setBlockEntity (Class<T> blockEntityClass, BlockEntityType<T> blockEntityType) {
		this.blockEntityClass = blockEntityClass;
		this.blockEntityType = blockEntityType;
	}

	@Nullable
	public T getBlockEntity (BlockGetter level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (this.blockEntityClass != null && this.blockEntityClass.isInstance(blockEntity)) {
			return this.blockEntityClass.cast(blockEntity);
		}

		return null;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity (@NonNull BlockPos pos, @NonNull BlockState state) {
		return blockEntityType.create(pos, state);
	}

	@Override
	protected boolean supportsManualDisconnect (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		return true;
	}

	@Override
	protected CableConnectionMode getConnectionMode (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		T blockEntity = getBlockEntity(level, pos);
		return blockEntity == null ? CableConnectionMode.AUTO : blockEntity.getConnectionMode(direction);
	}

	@Override
	protected void setConnectionMode (Level level, BlockPos pos, BlockState state, Direction direction, CableConnectionMode mode) {
		T blockEntity = getBlockEntity(level, pos);
		if (blockEntity != null) {
			blockEntity.setConnectionMode(direction, mode);
		}
	}

	@Override
	protected boolean isManuallyDisconnected (LevelReader level, BlockPos pos, BlockState state, Direction direction) {
		if (!(level instanceof BlockGetter blockGetter)) {
			return false;
		}

		T blockEntity = getBlockEntity(blockGetter, pos);
		return blockEntity != null && blockEntity.isManuallyDisconnected(direction);
	}

	@Override
	protected void setManuallyDisconnected (Level level, BlockPos pos, BlockState state, Direction direction, boolean disconnected) {
		T blockEntity = getBlockEntity(level, pos);
		if (blockEntity != null) {
			blockEntity.setManuallyDisconnected(direction, disconnected);
		}
	}

	@Override
	protected boolean shouldChangedStateKeepBlockEntity (BlockState oldState) {
		return oldState.getBlock() == this;
	}

}
