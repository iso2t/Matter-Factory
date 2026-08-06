package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.core.Tier;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class ItemPipe extends EntityCableBlock<ItemPipeBlockEntity> {

	public static final MapCodec<ItemPipe> CODEC = simpleCodec(properties -> new ItemPipe(properties, Tier.BASIC));

	@Getter
	private final Tier tier;

	public ItemPipe (Properties properties, Tier tier) {
		super(properties.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE));
		this.tier = tier;
	}

	@Override
	public @NotNull MapCodec<? extends CableBlock> getCodec () {
		return CODEC;
	}

	@Override
	public boolean canConnectTo (LevelReader level, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		if (neighborState.getBlock() instanceof ItemPipe) {
			return !(level instanceof BlockGetter blockGetter) || !(getBlockEntity(blockGetter, neighborPos) instanceof ItemPipeBlockEntity neighborPipe) || !neighborPipe.isManuallyDisconnected(direction.getOpposite());
		}

		if (level instanceof Level realLevel) {
			var blockEntity = realLevel.getBlockEntity(neighborPos);
			return realLevel.getCapability(Capabilities.Item.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
					|| realLevel.getCapability(Capabilities.Item.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
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
		if (neighborState.getBlock() instanceof ItemPipe || !(level instanceof Level realLevel)) {
			return false;
		}

		var blockEntity = realLevel.getBlockEntity(neighborPos);
		return realLevel.getCapability(Capabilities.Item.BLOCK, neighborPos, neighborState, blockEntity, direction.getOpposite()) != null
				|| realLevel.getCapability(Capabilities.Item.BLOCK, neighborPos, neighborState, blockEntity, null) != null;
	}

	@Override
	public @NotNull CableRenderGeometry getRenderGeometry () {
		return CableRenderGeometry.ITEM_PIPE;
	}

	@Override
	public VoxelShape getCoreShape () {
		return box(4, 4, 4, 12, 12, 12);
	}

	@Override
	public VoxelShape getDownShape () {
		return box(4, 0, 4, 12, 4, 12);
	}

	@Override
	public VoxelShape getUpShape () {
		return box(4, 12, 4, 12, 16, 12);
	}

	@Override
	public VoxelShape getNorthShape () {
		return box(4, 4, 0, 12, 12, 4);
	}

	@Override
	public VoxelShape getSouthShape () {
		return box(4, 4, 12, 12, 12, 16);
	}

	@Override
	public VoxelShape getWestShape () {
		return box(0, 4, 4, 4, 12, 12);
	}

	@Override
	public VoxelShape getEastShape () {
		return box(12, 4, 4, 16, 12, 12);
	}

	@Override
	public @NotNull ModelTemplate getCenterModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_center").requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
				.element(element -> element.from(4, 4, 4).to(12, 12, 12).textureAll(CABLE_CENTER_TEXTURE)).build();
	}

	@Override
	public @NotNull ModelTemplate getArmModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_arm").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
				.element(element -> element.from(4, 4, 0).to(12, 12, 4).textureAll(CABLE_ARM_TEXTURE)).build();
	}

	@Override
	public @NotNull ModelTemplate getStraightModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_straight").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
				.element(element -> element.from(4, 4, 0).to(12, 12, 16).textureAll(CABLE_ARM_TEXTURE)).build();
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker (@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return type == getBlockEntityType() ? (tickLevel, pos, tickState, blockEntity) -> ItemPipeBlockEntity.serverTick(tickLevel, pos, tickState, (ItemPipeBlockEntity) blockEntity) : null;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
