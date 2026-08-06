package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import com.mojang.math.Quadrant;
import lombok.Getter;
import matterfactory.common.block.entity.FluidPipeBlockEntity;
import matterfactory.core.Tier;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class FluidPipe extends EntityCableBlock<FluidPipeBlockEntity> {

	public static final MapCodec<FluidPipe> CODEC = simpleCodec(properties -> new FluidPipe(properties, Tier.BASIC));
	private static final Identifier FLUID_PIPE_MODEL_PARENT = Identifier.withDefaultNamespace("block/block");

	@Getter
	private final Tier tier;

	public FluidPipe (Properties properties, Tier tier) {
		super(properties.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE));
		this.tier = tier;
	}

	@Override
	public @NonNull MapCodec<? extends CableBlock> getCodec () {
		return CODEC;
	}

	@Override
	public boolean canConnectTo (LevelReader level, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		if (neighborState.getBlock() instanceof FluidPipe) {
			return !(level instanceof BlockGetter blockGetter) || !(getBlockEntity(blockGetter, neighborPos) instanceof FluidPipeBlockEntity neighborPipe) || !neighborPipe.isManuallyDisconnected(direction.getOpposite());
		}

		if (level instanceof Level realLevel) {
			var blockEntity = realLevel.getBlockEntity(neighborPos);
			return realLevel.getCapability(Capabilities.Fluid.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
					|| realLevel.getCapability(Capabilities.Fluid.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
		}

		return false;
	}

	@Override
	protected boolean supportsConnectionModes (BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
		if (!state.getValue(getConnectionProperty(direction))) {
			return false;
		}

		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = level.getBlockState(neighborPos);
		if (neighborState.getBlock() instanceof FluidPipe || !(level instanceof Level realLevel)) {
			return false;
		}

		var blockEntity = realLevel.getBlockEntity(neighborPos);
		return realLevel.getCapability(Capabilities.Fluid.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
				|| realLevel.getCapability(Capabilities.Fluid.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
	}

	@Override
	public @NotNull CableRenderGeometry getRenderGeometry () {
		return CableRenderGeometry.fromPixels(4.5F, 11.5F, 1, 2.5F);
	}

	@Override
	public VoxelShape getCoreShape () {
		return box(3.5, 3.5, 3.5, 12.5, 12.5, 12.5);
	}

	@Override
	public VoxelShape getDownShape () {
		return box(4.5, 0, 4.5, 11.5, 4.5, 11.5);
	}

	@Override
	public VoxelShape getUpShape () {
		return box(4.5, 11.5, 4.5, 11.5, 16, 11.5);
	}

	@Override
	public VoxelShape getNorthShape () {
		return box(4.5, 4.5, 0, 11.5, 11.5, 4.5);
	}

	@Override
	public VoxelShape getSouthShape () {
		return box(4.5, 4.5, 11.5, 11.5, 11.5, 16);
	}

	@Override
	public VoxelShape getWestShape () {
		return box(0, 4.5, 4.5, 4.5, 11.5, 11.5);
	}

	@Override
	public VoxelShape getEastShape () {
		return box(11.5, 4.5, 4.5, 16, 11.5, 11.5);
	}

	@Override
	public @NotNull ModelTemplate getCenterModel () {
		ExtendedModelTemplateBuilder builder = fluidPipeTemplate("_center", CABLE_CENTER_TEXTURE);
		addHub(builder, CABLE_CENTER_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getArmModel () {
		ExtendedModelTemplateBuilder builder = fluidPipeTemplate("_arm", CABLE_ARM_TEXTURE);
		addNorthSouthPipe(builder, 0, 4, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getStraightModel () {
		ExtendedModelTemplateBuilder builder = fluidPipeTemplate("_straight", CABLE_ARM_TEXTURE);
		addNorthSouthPipe(builder, 0, 16, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getItemModel () {
		ExtendedModelTemplateBuilder builder = fluidPipeTemplate("_item", CABLE_ARM_TEXTURE);
		addNorthSouthPipe(builder, 0, 16, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getGuiModel () {
		ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder().parent(FLUID_PIPE_MODEL_PARENT).suffix("_gui")
				.requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE);
		addHub(builder, CABLE_CENTER_TEXTURE);
		addNorthSouthPipe(builder, 0, 4, CABLE_ARM_TEXTURE);
		addNorthSouthPipe(builder, 12, 16, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	private static ExtendedModelTemplateBuilder fluidPipeTemplate (String suffix, TextureSlot texture) {
		return ExtendedModelTemplateBuilder.builder().suffix(suffix).requiredTextureSlot(texture).requiredTextureSlot(TextureSlot.PARTICLE);
	}

	private static void addHub (ExtendedModelTemplateBuilder builder, TextureSlot texture) {
		addElement(builder, 3.5F, 3.5F, 3.5F, 12.5F, 12.5F, 12.5F, texture);
	}

	private static void addNorthSouthPipe (ExtendedModelTemplateBuilder builder, float minZ, float maxZ, TextureSlot texture) {
		builder.element(element -> element.from(4.5F, 4.5F, minZ).to(11.5F, 11.5F, maxZ).textureAll(texture)
				.face(Direction.UP, face -> face.rotation(Quadrant.R90)));
	}

	private static void addElement (ExtendedModelTemplateBuilder builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, TextureSlot texture) {
		builder.element(element -> element.from(minX, minY, minZ).to(maxX, maxY, maxZ).textureAll(texture));
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker (@NonNull Level level, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
		return type == getBlockEntityType() ? (tickLevel, pos, tickState, blockEntity) -> FluidPipeBlockEntity.serverTick(tickLevel, pos, tickState, (FluidPipeBlockEntity) blockEntity) : null;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
