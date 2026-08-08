package matterfactory.common.block.machine;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.common.block.BaseBlock;
import matterfactory.common.block.BlockEntityTypeOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class AbstractMachineEntityBlock<T extends AbstractMachineBlockEntity> extends BaseBlock implements EntityBlock, BlockEntityTypeOwner<T> {

	public static BooleanProperty ACTIVE = BooleanProperty.create("active");

	private Class<T> blockEntityClass;

	@Getter
	private       BlockEntityType<T>  blockEntityType;
	private final MapCodec<BaseBlock> codec = getCodec();

	public AbstractMachineEntityBlock (Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
	}

	@Override
	public void setBlockEntity (Class<T> blockEntityClass, BlockEntityType<T> blockEntityType) {
		this.blockEntityClass = blockEntityClass;
		this.blockEntityType = blockEntityType;
	}

	@Nullable
	public T getBlockEntity (BlockGetter level, BlockPos pos) {
		final BlockEntity te = level.getBlockEntity(pos);
		if (this.blockEntityClass != null && this.blockEntityClass.isInstance(te)) {
			return this.blockEntityClass.cast(te);
		}

		return null;
	}

	public abstract MapCodec<BaseBlock> getCodec();

	@Override
	public @Nullable BlockEntity newBlockEntity (@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
		return blockEntityType.create(blockPos, blockState);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker (@NonNull Level level, @NonNull BlockState state, @NonNull BlockEntityType<E> type) {
		if (level.isClientSide() || type != blockEntityType) {
			return null;
		}

		return (_, _, _, blockEntity) -> ((T) blockEntity).serverTick();
	}

	@Override
	public @Nullable BlockState getStateForPlacement (@NonNull BlockPlaceContext context) {
		return this.defaultBlockState().setValue(ACTIVE, false);
	}

	@Override
	protected void createBlockStateDefinition (StateDefinition.@NonNull Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ACTIVE);
	}

	@Override
	protected @NonNull RenderShape getRenderShape (@NonNull BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected @NonNull MapCodec<? extends Block> codec () {
		return getCodec();
	}

}
