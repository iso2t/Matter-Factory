package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import matterfactory.common.block.BaseBlock;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.model.CustomBlockModel;
import matterfactory.core.datagen.util.IPickaxe;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class CableBlock extends BaseBlock implements CustomBlockModel, SimpleWaterloggedBlock, IPickaxe {

	public static final TextureSlot          CABLE_TEXTURE = TextureSlot.create("cable", TextureSlot.ALL);
	private static final Identifier          BLOCK_MODEL_PARENT = Identifier.withDefaultNamespace("block/block");

	public static final BooleanProperty DOWN  = BooleanProperty.create("down");
	public static final BooleanProperty UP    = BooleanProperty.create("up");
	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST  = BooleanProperty.create("west");
	public static final BooleanProperty EAST  = BooleanProperty.create("east");

	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);

	private static final VoxelShape DOWN_SHAPE  = box(5, 0, 5, 11, 5, 11);
	private static final VoxelShape UP_SHAPE    = box(5, 11, 5, 11, 16, 11);
	private static final VoxelShape NORTH_SHAPE = box(5, 5, 0, 11, 11, 5);
	private static final VoxelShape SOUTH_SHAPE = box(5, 5, 11, 11, 11, 16);
	private static final VoxelShape WEST_SHAPE  = box(0, 5, 5, 5, 11, 11);
	private static final VoxelShape EAST_SHAPE  = box(11, 5, 5, 16, 11, 11);

	private static final VoxelShape[] SHAPES = createShapes();

	public CableBlock (Properties properties) {
		super(properties);

		registerDefaultState(getStateDefinition().any().setValue(DOWN, false).setValue(UP, false).setValue(NORTH, false).setValue(SOUTH, false).setValue(WEST, false).setValue(EAST, false).setValue(WATERLOGGED, false));
	}

	public abstract @NotNull MapCodec<? extends CableBlock> getCodec ();

	@Override
	protected @NonNull MapCodec<? extends Block> codec () {
		return getCodec();
	}

	@Override
	public @Nullable BlockState getStateForPlacement (BlockPlaceContext context) {
		var pos = context.getClickedPos();
		var fluidState = context.getLevel().getFluidState(pos);

		return getConnectionState(context.getLevel(), context.getClickedPos(), defaultBlockState().setValue(WATERLOGGED, fluidState.is(Fluids.WATER)));
	}

	@Override
	protected @NonNull FluidState getFluidState (BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected @NonNull BlockState updateShape (BlockState state, @NonNull LevelReader level, @NonNull ScheduledTickAccess ticks, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return state.setValue(getProperty(direction), canConnectTo(level, neighborPos, neighborState));
	}

	@Override
	protected @NonNull VoxelShape getShape (@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		return SHAPES[getShapeIndex(state)];
	}

	@Override
	protected void createBlockStateDefinition (StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
	}

	/**
	 * Determines whether this block can connect to a neighboring block at the specified position
	 * with the given block state.
	 *
	 * @param level        the level reader providing access to the world state
	 * @param neighborPos  the position of the neighboring block to check connectivity
	 * @param neighborState the block state of the neighboring block to check connectivity
	 * @return true if this block can connect to the neighboring block, false otherwise
	 */
	public abstract boolean canConnectTo (LevelReader level, BlockPos neighborPos, BlockState neighborState);

	private BlockState getConnectionState (LevelReader level, BlockPos pos, BlockState state) {
		for (var direction : Direction.values()) {
			var neighborPos = pos.relative(direction);
			var neighborState = level.getBlockState(neighborPos);

			state = state.setValue(getProperty(direction), canConnectTo(level, neighborPos, neighborState));
		}

		return state;
	}

	private static BooleanProperty getProperty (Direction direction) {
		return switch (direction) {
			case DOWN -> DOWN;
			case UP -> UP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
		};
	}

	private static int getShapeIndex (BlockState state) {
		int index = 0;

		if (state.getValue(DOWN)) index |= 1;
		if (state.getValue(UP)) index |= 1 << 1;
		if (state.getValue(NORTH)) index |= 1 << 2;
		if (state.getValue(SOUTH)) index |= 1 << 3;
		if (state.getValue(WEST)) index |= 1 << 4;
		if (state.getValue(EAST)) index |= 1 << 5;
		return index;
	}

	private static VoxelShape[] createShapes () {
		VoxelShape[] shapes = new VoxelShape[64];

		for (int index = 0; index < shapes.length; index++) {
			VoxelShape shape = CORE;
			if ((index & 1) != 0) shape = Shapes.or(shape, DOWN_SHAPE);
			if ((index & (1 << 1)) != 0) shape = Shapes.or(shape, UP_SHAPE);
			if ((index & (1 << 2)) != 0) shape = Shapes.or(shape, NORTH_SHAPE);
			if ((index & (1 << 3)) != 0) shape = Shapes.or(shape, SOUTH_SHAPE);
			if ((index & (1 << 4)) != 0) shape = Shapes.or(shape, WEST_SHAPE);
			if ((index & (1 << 5)) != 0) shape = Shapes.or(shape, EAST_SHAPE);
			shapes[index] = shape.optimize();
		}

		return shapes;
	}

	@Override
	public void registerModel (BlockModelGenerators generator, BlockDefinition<?> block) {
		CustomBlockModel.registerCableType(generator, this);
	}

	public static final ModelTemplate CENTER_MODEL = ExtendedModelTemplateBuilder.builder().suffix("_center").requiredTextureSlot(CABLE_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE).element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_TEXTURE)).build();

	public static final ModelTemplate ARM_MODEL = ExtendedModelTemplateBuilder.builder().suffix("_arm").requiredTextureSlot(CABLE_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE).element(element -> element.from(5, 5, 0).to(11, 11, 5).textureAll(CABLE_TEXTURE)).build();

	public static final ModelTemplate ITEM_MODEL = ExtendedModelTemplateBuilder.builder().parent(BLOCK_MODEL_PARENT).suffix("_item").requiredTextureSlot(CABLE_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE).element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_TEXTURE)).build();

	public static final ModelTemplate GUI_MODEL = ExtendedModelTemplateBuilder.builder().parent(BLOCK_MODEL_PARENT).suffix("_gui").requiredTextureSlot(CABLE_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
			.element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_TEXTURE))
			.element(element -> element.from(5, 0, 5).to(11, 5, 11).textureAll(CABLE_TEXTURE))
			.element(element -> element.from(5, 11, 5).to(11, 16, 11).textureAll(CABLE_TEXTURE))
			.element(element -> element.from(5, 5, 0).to(11, 11, 5).textureAll(CABLE_TEXTURE))
			.element(element -> element.from(5, 5, 11).to(11, 11, 16).textureAll(CABLE_TEXTURE))
			.element(element -> element.from(0, 5, 5).to(5, 11, 11).textureAll(CABLE_TEXTURE))
			.element(element -> element.from(11, 5, 5).to(16, 11, 11).textureAll(CABLE_TEXTURE))
			.build();
}
