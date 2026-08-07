package matterfactory.common.block.cable;

import lombok.Setter;
import matterfactory.common.block.BaseBlock;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.item.tool.WrenchItem;
import matterfactory.common.model.CustomBlockModel;
import matterfactory.core.Factory;
import matterfactory.core.datagen.util.IPickaxe;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@Setter
public class FacadeBlock extends BaseBlock implements EntityBlock, CustomBlockModel, IPickaxe {

	private static final Identifier      MODEL      = Factory.get("block/facade");
	private static final Identifier      ITEM_MODEL = Factory.get("block/facade_item");
	public static final  BooleanProperty PAINTED    = BooleanProperty.create("painted");
	public static final  BooleanProperty GLOWING    = BooleanProperty.create("glowing");

	private BlockEntityType<FacadeBlockEntity> blockEntityType;

	public FacadeBlock (Properties properties) {
		super(properties.noOcclusion().lightLevel(state -> state.getValue(GLOWING) ? 15 : 0));
		registerDefaultState(getStateDefinition().any().setValue(PAINTED, false).setValue(GLOWING, false));
	}

	public void cover (Level level, BlockPos pos, BlockState coveredState, BaseCableBlockEntity cable) {
		level.setBlock(pos, defaultBlockState(), UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			facade.setCoveredCable(coveredState, cable);
		}
	}

	@Override
	protected @NonNull InteractionResult useItemOn (ItemStack itemStack, @NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player, @NonNull InteractionHand hand, @NonNull BlockHitResult hit) {
		if (itemStack.getItem() instanceof WrenchItem wrenchItem && level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			return useWrenchOnFacade(wrenchItem, itemStack, level, pos, player, hit, facade);
		}

		if (itemStack.is(Items.GLOWSTONE_DUST) && !state.getValue(GLOWING)) {
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(GLOWING, true), UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
				consumeInteractionItem(itemStack, player);
			}
			return InteractionResult.SUCCESS;
		}

		if (itemStack.getItem() instanceof BlockItem blockItem && isValidPaint(blockItem.getBlock().defaultBlockState()) && level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			if (state.getValue(PAINTED)) {
				return InteractionResult.SUCCESS;
			}

			if (!level.isClientSide()) {
				BlockState paintedState = blockItem.getBlock().defaultBlockState();
				facade.setPaintedState(paintedState);
				playPlacementSound(level, pos, player, paintedState);
				consumeInteractionItem(itemStack, player);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private static void consumeInteractionItem (ItemStack itemStack, Player player) {
		if (!player.getAbilities().instabuild) {
			itemStack.shrink(1);
		}
	}

	public static void playPlacementSound (Level level, BlockPos pos, @Nullable Player player, BlockState state) {
		var sound = state.getSoundType(level, pos, player);
		level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
	}

	private static InteractionResult useWrenchOnFacade (WrenchItem wrench, ItemStack itemStack, Level level, BlockPos pos, Player player, BlockHitResult hit, FacadeBlockEntity facade) {
		BlockState coveredState = facade.getCoveredState();
		if (!(coveredState.getBlock() instanceof CableBlock cableBlock) || !(facade.getCoveredCable(BaseCableBlockEntity.class) instanceof BaseCableBlockEntity cable)) {
			return InteractionResult.PASS;
		}

		var targetedSide = CableBlock.getTargetedSide(coveredState, pos, hit);
		if (targetedSide.isEmpty()) {
			return InteractionResult.PASS;
		}

		Direction direction = targetedSide.get();
		boolean canModify = player.isShiftKeyDown() || cableBlock.supportsConnectionModesAt(level, pos, coveredState, direction);
		if (!canModify) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			if (player.isShiftKeyDown()) {
				boolean disconnected = !cable.isManuallyDisconnected(direction);
				cable.setManuallyDisconnected(direction, disconnected);
				facade.setCoveredState(coveredState.setValue(CableBlock.getConnectionProperty(direction), !disconnected));
				updateNeighborConnection(level, pos, direction, disconnected);
				level.invalidateCapabilities(pos);
				level.invalidateCapabilities(pos.relative(direction));
			} else {
				cable.setConnectionMode(direction, cable.getConnectionMode(direction).next());
			}
			return InteractionResult.SUCCESS;
		}

		return wrench.successfulWrenchAction(player, itemStack, level, pos, true);
	}

	private static void updateNeighborConnection (Level level, BlockPos pos, Direction direction, boolean disconnected) {
		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = level.getBlockState(neighborPos);
		Direction neighborDirection = direction.getOpposite();
		if (level.getBlockEntity(neighborPos) instanceof FacadeBlockEntity neighborFacade && neighborFacade.getCoveredCable(BaseCableBlockEntity.class) instanceof BaseCableBlockEntity neighborCable) {
			neighborCable.setManuallyDisconnected(neighborDirection, disconnected);
			BlockState coveredState = neighborFacade.getCoveredState();
			if (coveredState.getBlock() instanceof CableBlock) {
				neighborFacade.setCoveredState(coveredState.setValue(CableBlock.getConnectionProperty(neighborDirection), !disconnected));
			}
		} else if (neighborState.getBlock() instanceof CableBlock && level.getBlockEntity(neighborPos) instanceof BaseCableBlockEntity neighborCable) {
			neighborCable.setManuallyDisconnected(neighborDirection, disconnected);
			level.setBlock(neighborPos, neighborState.setValue(CableBlock.getConnectionProperty(neighborDirection), !disconnected), UPDATE_ALL);
		}
	}

	public static boolean isValidPaint (BlockState state) {
		return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && !state.hasBlockEntity();
	}

	@Override
	public @NonNull SoundType getSoundType (@NonNull BlockState state, LevelReader level, @NonNull BlockPos pos, @Nullable Entity entity) {
		if (level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			BlockState paintedState = facade.getPaintedState();
			if (!paintedState.isAir()) {
				return paintedState.getSoundType(level, pos, entity);
			}
		}
		return super.getSoundType(state, level, pos, entity);
	}

	@Override
	protected @NonNull VoxelShape getShape (@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		if (!isMaintainingFacade(context)) {
			return Shapes.block();
		}

		if (!(level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) || !(facade.getCoveredState().getBlock() instanceof CableBlock cable)) {
			return Shapes.block();
		}

		BlockState coveredState = facade.getCoveredState();
		VoxelShape shape = cable.getCableShape().core();
		for (Direction direction : Direction.values()) {
			if (coveredState.getValue(CableBlock.getConnectionProperty(direction))) {
				shape = Shapes.or(shape, CableBlock.getArmShape(direction, cable));
			}
		}
		return shape.optimize();
	}

	private static boolean isMaintainingFacade (CollisionContext context) {
		return context instanceof EntityCollisionContext entityContext
		       && entityContext.getEntity() instanceof Player player
		       && player.getMainHandItem().getItem() instanceof WrenchItem;
	}

	@Override
	protected @NonNull VoxelShape getCollisionShape (@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public void registerModel (BlockModelGenerators generators, BlockDefinition<?> block) {
		MultiVariant model = BlockModelGenerators.variant(new Variant(MODEL));
		generators.blockStateOutput.accept(MultiPartGenerator.multiPart(block.getBlock()).with(BlockModelGenerators.condition().term(PAINTED, false).build(), model));
		generators.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(ITEM_MODEL));
	}

	@Override
	protected void createBlockStateDefinition (StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PAINTED, GLOWING);
	}

	@Override
	public void playerDestroy (@NonNull Level level, @NonNull Player player, @NonNull BlockPos pos, @NonNull BlockState state, @Nullable BlockEntity blockEntity, @NonNull ItemStack tool) {
		if (!level.isClientSide() && !player.getAbilities().instabuild && blockEntity instanceof FacadeBlockEntity facade) {
			dropFacadeContents(level, pos, state, facade);
		}

		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		if (!level.isClientSide() && blockEntity instanceof FacadeBlockEntity facade) {
			facade.restoreCoveredCable(level);
		}
	}

	private static void dropFacadeContents (Level level, BlockPos pos, BlockState state, FacadeBlockEntity facade) {
		BlockState paintedState = facade.getPaintedState();
		if (!paintedState.isAir()) {
			Block.popResource(level, pos, new ItemStack(paintedState.getBlock()));
		}
		if (state.getValue(GLOWING)) {
			Block.popResource(level, pos, new ItemStack(Items.GLOWSTONE_DUST));
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity (@NonNull BlockPos pos, @NonNull BlockState state) {
		return blockEntityType.create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker (@NonNull Level level, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
		return type == blockEntityType ? (tickLevel, pos, tickState, blockEntity) -> FacadeBlockEntity.serverTick(tickLevel, pos, tickState, (FacadeBlockEntity) blockEntity) : null;
	}
}
