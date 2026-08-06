package matterfactory.common.block.cable;

import lombok.Getter;
import matterfactory.common.block.entity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class EntityCableBlock<T extends BaseBlockEntity> extends CableBlock implements EntityBlock {

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

}
