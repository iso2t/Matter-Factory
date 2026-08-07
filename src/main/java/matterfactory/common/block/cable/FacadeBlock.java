package matterfactory.common.block.cable;

import matterfactory.common.block.BaseBlock;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.item.tool.WrenchItem;
import matterfactory.core.Factory;
import matterfactory.core.datagen.util.IDefinedModel;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class FacadeBlock extends BaseBlock implements EntityBlock, IDefinedModel {

	private static final Identifier MODEL = Factory.get("block/facade");
	public static final BooleanProperty PAINTED = BooleanProperty.create("painted");

	private BlockEntityType<FacadeBlockEntity> blockEntityType;

	public FacadeBlock (Properties properties) {
		super(properties);
		registerDefaultState(getStateDefinition().any().setValue(PAINTED, false));
	}

	public void setBlockEntityType (BlockEntityType<FacadeBlockEntity> blockEntityType) {
		this.blockEntityType = blockEntityType;
	}

	public void cover (Level level, BlockPos pos, BlockState coveredState, BaseCableBlockEntity cable) {
		level.setBlock(pos, defaultBlockState(), UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			facade.setCoveredCable(coveredState, cable);
		}
	}

	@Override
	protected @NonNull InteractionResult useItemOn (ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (itemStack.getItem() instanceof WrenchItem wrenchItem && level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			return useWrenchOnFacade(wrenchItem, itemStack, level, pos, player, hit, facade);
		}

		if (itemStack.getItem() instanceof BlockItem blockItem && isValidPaint(blockItem.getBlock().defaultBlockState()) && level.getBlockEntity(pos) instanceof FacadeBlockEntity facade) {
			if (!level.isClientSide()) {
				facade.setPaintedState(blockItem.getBlock().defaultBlockState());
				if (!player.getAbilities().instabuild) {
					itemStack.shrink(1);
				}
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
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
	public void registerDefinedModel (BlockModelGenerators generators, BlockDefinition<?> block) {
		MultiVariant model = BlockModelGenerators.variant(new Variant(MODEL));
		generators.blockStateOutput.accept(MultiPartGenerator.multiPart(block.getBlock())
				.with(BlockModelGenerators.condition().term(PAINTED, false).build(), model));
		generators.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(MODEL));
	}

	@Override
	protected void createBlockStateDefinition (StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PAINTED);
	}

	@Override
	public void playerDestroy (Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		if (!level.isClientSide() && blockEntity instanceof FacadeBlockEntity facade) {
			facade.restoreCoveredCable(level);
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity (BlockPos pos, BlockState state) {
		return blockEntityType.create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker (Level level, BlockState state, BlockEntityType<T> type) {
		return type == blockEntityType ? (tickLevel, pos, tickState, blockEntity) -> FacadeBlockEntity.serverTick(tickLevel, pos, tickState, (FacadeBlockEntity) blockEntity) : null;
	}
}
