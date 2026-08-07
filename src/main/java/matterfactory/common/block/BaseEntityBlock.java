package matterfactory.common.block;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.common.block.entity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseEntityBlock<T extends BaseBlockEntity> extends BaseBlock implements EntityBlock {

	private Class<T> blockEntityClass;

	@Getter
	private             BlockEntityType<T>      blockEntityType;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	private final       MapCodec<BaseBlock>     codec  = getCodec();

	public BaseEntityBlock (Properties properties) {
		super(properties);
	}

	public void setBlockEntity (Class<T> blockEntityClass, BlockEntityType<T> blockEntityType) {
		this.blockEntityClass = blockEntityClass;
		this.blockEntityType = blockEntityType;
	}

	@Nullable
	public T getBlockEntity (BlockGetter level, int x, int y, int z) {
		return this.getBlockEntity(level, new BlockPos(x, y, z));
	}

	@Nullable
	public T getBlockEntity (BlockGetter level, BlockPos pos) {
		final BlockEntity te = level.getBlockEntity(pos);
		if (this.blockEntityClass != null && this.blockEntityClass.isInstance(te)) {
			return this.blockEntityClass.cast(te);
		}

		return null;
	}

	public abstract MapCodec<BaseBlock> getCodec ();

	@Nullable
	@Override
	public BlockEntity newBlockEntity (@NotNull BlockPos pos, @NotNull BlockState state) {
		return blockEntityType.create(pos, state);
	}

	/*@Nullable
	@Override
	public BlockState getStateForPlacement (BlockPlaceContext context) {
		PropertyComponent<Direction> property = PropertyHelper.of(FACING, context.getNearestLookingDirection().getOpposite());
		return this.defaultBlockState().setValue(POWERED, false).setValue(property.getProperty(), property.getValue()).setValue(CRAFTING, false);
	}*/

	@Override
	protected void createBlockStateDefinition (StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		//builder.add(POWERED, FACING, CRAFTING);
	}

	@Override
	protected @NotNull RenderShape getRenderShape (@NotNull BlockState pState) {
		return RenderShape.MODEL;
	}

	@Override
	protected @NotNull MapCodec<? extends Block> codec () {
		return codec;
	}

	@Override
	protected boolean isSignalSource (@NotNull BlockState state) {
		return false;
	}

	public Direction getConnectedDirection (BlockState state) {
		return switch (state.getValue(BlockStateProperties.ATTACH_FACE)) {
			case CEILING -> Direction.DOWN;
			case FLOOR -> Direction.UP;
			default -> state.getValue(FACING);
		};
	}

}
