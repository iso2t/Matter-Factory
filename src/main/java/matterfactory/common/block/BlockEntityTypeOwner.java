package matterfactory.common.block;

import matterfactory.common.block.entity.BaseBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Allows the block entity registry to bind a block to its registered type.
 */
public interface BlockEntityTypeOwner<T extends BaseBlockEntity> {

	void setBlockEntity (Class<T> blockEntityClass, BlockEntityType<T> blockEntityType);

}
