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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class CableBlock extends BaseBlock implements CustomBlockModel, SimpleWaterloggedBlock, IPickaxe {

	public static final TextureSlot CABLE_CENTER_TEXTURE = TextureSlot.create("cable_center", TextureSlot.ALL);
	public static final TextureSlot CABLE_ARM_TEXTURE    = TextureSlot.create("cable_arm", TextureSlot.ALL);
	private static final Identifier BLOCK_MODEL_PARENT   = Identifier.withDefaultNamespace("block/block");

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

		registerDefaultState(getStateDefinition().any()
				.setValue(DOWN, false)
				.setValue(UP, false)
				.setValue(NORTH, false)
				.setValue(SOUTH, false)
				.setValue(WEST, false)
				.setValue(EAST, false)
				.setValue(WATERLOGGED, false));
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

		return state.setValue(getConnectionProperty(direction), shouldConnectTo(state, level, pos, direction, neighborPos, neighborState));
	}

	@Override
	protected @NonNull VoxelShape getShape (@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		return SHAPES[getShapeIndex(state)];
	}

	@Override
	protected @NonNull InteractionResult useWithoutItem (@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hit) {
		Optional<Direction> targetedSide = getTargetedSide(state, pos, hit);
		if (targetedSide.isEmpty()) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			Direction direction = targetedSide.get();
			if (player.isShiftKeyDown()) {
				if (!supportsManualDisconnect(level, pos, state, direction)) {
					return InteractionResult.PASS;
				}

				BlockState updatedState = toggleConnection(level, pos, state, direction);
				level.setBlock(pos, updatedState, Block.UPDATE_ALL);
			} else if (supportsConnectionModes(level, pos, state, direction)) {
				cycleConnectionMode(level, pos, state, direction);
			} else {
				return InteractionResult.PASS;
			}

			level.invalidateCapabilities(pos);
		}

		return InteractionResult.SUCCESS;
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
	public abstract boolean canConnectTo (LevelReader level, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState);

	private boolean shouldConnectTo (BlockState state, LevelReader level, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return !isManuallyDisconnected(level, pos, state, direction) && canConnectTo(level, pos, direction, neighborPos, neighborState);
	}

	private BlockState getConnectionState (LevelReader level, BlockPos pos, BlockState state) {
		for (var direction : Direction.values()) {
			var neighborPos = pos.relative(direction);
			var neighborState = level.getBlockState(neighborPos);

			state = state.setValue(getConnectionProperty(direction), shouldConnectTo(state, level, pos, direction, neighborPos, neighborState));
		}

		return state;
	}

	private BlockState toggleConnection (Level level, BlockPos pos, BlockState state, Direction direction) {
		boolean disconnected = !isManuallyDisconnected(level, pos, state, direction);
		setManuallyDisconnected(level, pos, state, direction, disconnected);
		var neighborPos = pos.relative(direction);
		var neighborState = level.getBlockState(neighborPos);
		return state.setValue(getConnectionProperty(direction), !disconnected && shouldConnectTo(state, level, pos, direction, neighborPos, neighborState));
	}

	private void cycleConnectionMode (Level level, BlockPos pos, BlockState state, Direction direction) {
		setConnectionMode(level, pos, state, direction, getConnectionMode(level, pos, state, direction).next());
	}

	protected boolean supportsManualDisconnect (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		return false;
	}

	protected boolean supportsConnectionModes (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		return false;
	}

	protected CableConnectionMode getConnectionMode (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		return CableConnectionMode.AUTO;
	}

	protected void setConnectionMode (Level level, BlockPos pos, BlockState state, Direction direction, CableConnectionMode mode) {
	}

	protected boolean isManuallyDisconnected (LevelReader level, BlockPos pos, BlockState state, Direction direction) {
		return false;
	}

	protected void setManuallyDisconnected (Level level, BlockPos pos, BlockState state, Direction direction, boolean disconnected) {
	}

	public static Optional<Direction> getTargetedSide (BlockState state, BlockPos pos, BlockHitResult hit) {
		var localHit = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

		for (var direction : Direction.values()) {
			if (state.getValue(getConnectionProperty(direction)) && getArmShape(direction).bounds().inflate(1.0E-5).contains(localHit)) {
				return Optional.of(direction);
			}
		}

		Direction face = hit.getDirection();
		return state.getValue(getConnectionProperty(face)) ? Optional.empty() : Optional.of(face);
	}

	public static BooleanProperty getConnectionProperty (Direction direction) {
		return switch (direction) {
			case DOWN -> DOWN;
			case UP -> UP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
		};
	}

	public static VoxelShape getArmShape (Direction direction) {
		return switch (direction) {
			case DOWN -> DOWN_SHAPE;
			case UP -> UP_SHAPE;
			case NORTH -> NORTH_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			case EAST -> EAST_SHAPE;
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

	public static final ModelTemplate CENTER_MODEL = ExtendedModelTemplateBuilder.builder().suffix("_center").requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
			.element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_CENTER_TEXTURE)).build();

	public static final ModelTemplate ARM_MODEL = ExtendedModelTemplateBuilder.builder().suffix("_arm").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
			.element(element -> element.from(6, 6, 0).to(10, 10, 5).textureAll(CABLE_ARM_TEXTURE)).build();

	public static final ModelTemplate STRAIGHT_MODEL = ExtendedModelTemplateBuilder.builder().suffix("_straight").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
			.element(element -> element.from(6, 6, 0).to(10, 10, 16).textureAll(CABLE_ARM_TEXTURE)).build();

	public static final ModelTemplate ITEM_MODEL = ExtendedModelTemplateBuilder.builder().parent(BLOCK_MODEL_PARENT).suffix("_item").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
			.element(element -> element.from(0, 6, 6).to(16, 10, 10).textureAll(CABLE_ARM_TEXTURE)).build();

	public static final ModelTemplate GUI_MODEL = ExtendedModelTemplateBuilder.builder().parent(BLOCK_MODEL_PARENT).suffix("_gui").requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
			.element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_CENTER_TEXTURE))
			.element(element -> element.from(6, 0, 6).to(10, 6, 10).textureAll(CABLE_ARM_TEXTURE))
			.element(element -> element.from(6, 10, 6).to(10, 16, 10).textureAll(CABLE_ARM_TEXTURE))
			.element(element -> element.from(6, 6, 0).to(10, 10, 6).textureAll(CABLE_ARM_TEXTURE))
			.element(element -> element.from(6, 6, 10).to(10, 10, 16).textureAll(CABLE_ARM_TEXTURE))
			.element(element -> element.from(0, 6, 6).to(6, 10, 10).textureAll(CABLE_ARM_TEXTURE))
			.element(element -> element.from(10, 6, 6).to(16, 10, 10).textureAll(CABLE_ARM_TEXTURE))
			.build();
}
