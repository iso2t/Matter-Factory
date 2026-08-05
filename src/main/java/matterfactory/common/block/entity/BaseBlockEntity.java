package matterfactory.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseBlockEntity extends BlockEntity {

	public BaseBlockEntity (BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

}
