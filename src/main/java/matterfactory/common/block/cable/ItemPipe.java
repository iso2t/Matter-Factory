package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.core.Factory;
import matterfactory.core.Tier;
import matterfactory.util.shape.CableShape;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemPipe extends EntityCableBlock<ItemPipeBlockEntity> {

	public static final  MapCodec<ItemPipe> CODEC                  = simpleCodec(properties -> new ItemPipe(properties, Tier.BASIC));
	private static final Identifier         ITEM_PIPE_MODEL_PARENT = Factory.get("block/item_pipe_translucent");

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
	protected boolean hasEndpointCapability (Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
		return level.getCapability(Capabilities.Item.BLOCK, pos, state, level.getBlockEntity(pos), side) != null;
	}

	@Override
	public @NotNull CableRenderGeometry getRenderGeometry () {
		return CableRenderGeometry.ITEM_PIPE;
	}

	@Override
	public CableShape getCableShape () {
		return CableShape.from(box(4, 4, 4, 12, 12, 12), box(4, 0, 4, 12, 4, 12), box(4, 12, 4, 12, 16, 12), box(4, 4, 0, 12, 12, 4), box(4, 4, 12, 12, 12, 16), box(0, 4, 4, 4, 12, 12), box(12, 4, 4, 16, 12, 12));
	}

	@Override
	public @NotNull ModelTemplate getCenterModel () {
		ExtendedModelTemplateBuilder builder = itemPipeTemplate("_center", CABLE_CENTER_TEXTURE);
		addJunctionFrame(builder, CABLE_CENTER_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getArmModel () {
		ExtendedModelTemplateBuilder builder = itemPipeTemplate("_arm", CABLE_ARM_TEXTURE);
		addNorthSouthTube(builder, 0, 4, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getStraightModel () {
		ExtendedModelTemplateBuilder builder = itemPipeTemplate("_straight", CABLE_ARM_TEXTURE);
		addNorthSouthTube(builder, 0, 16, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getItemModel () {
		ExtendedModelTemplateBuilder builder = itemPipeTemplate("_item", CABLE_ARM_TEXTURE);
		addNorthSouthTube(builder, 0, 16, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	@Override
	public @NotNull ModelTemplate getGuiModel () {
		ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder().parent(ITEM_PIPE_MODEL_PARENT).suffix("_gui").requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(CABLE_GLASS_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE);
		addJunctionFrame(builder, CABLE_CENTER_TEXTURE);
		addNorthSouthTube(builder, 0, 4, CABLE_ARM_TEXTURE);
		addNorthSouthTube(builder, 12, 16, CABLE_ARM_TEXTURE);
		return builder.build();
	}

	private static ExtendedModelTemplateBuilder itemPipeTemplate (String suffix, TextureSlot frameTexture) {
		return ExtendedModelTemplateBuilder.builder().parent(ITEM_PIPE_MODEL_PARENT).suffix(suffix).requiredTextureSlot(frameTexture).requiredTextureSlot(CABLE_GLASS_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE);
	}

	private static void addJunctionFrame (ExtendedModelTemplateBuilder builder, TextureSlot frameTexture) {
		addFrameElement(builder, 4, 4, 4, 12, 5, 5, frameTexture);
		addFrameElement(builder, 4, 11, 4, 12, 12, 5, frameTexture);
		addFrameElement(builder, 4, 4, 11, 12, 5, 12, frameTexture);
		addFrameElement(builder, 4, 11, 11, 12, 12, 12, frameTexture);
		addFrameElement(builder, 4, 4, 4, 5, 12, 5, frameTexture);
		addFrameElement(builder, 11, 4, 4, 12, 12, 5, frameTexture);
		addFrameElement(builder, 4, 4, 11, 5, 12, 12, frameTexture);
		addFrameElement(builder, 11, 4, 11, 12, 12, 12, frameTexture);
		addFrameElement(builder, 4, 4, 4, 5, 5, 12, frameTexture);
		addFrameElement(builder, 11, 4, 4, 12, 5, 12, frameTexture);
		addFrameElement(builder, 4, 11, 4, 5, 12, 12, frameTexture);
		addFrameElement(builder, 11, 11, 4, 12, 12, 12, frameTexture);

		addGlassElement(builder, 5, 5, 4.25F, 11, 11, 4.5F);
		addGlassElement(builder, 5, 5, 11.5F, 11, 11, 11.75F);
		addGlassElement(builder, 4.25F, 5, 5, 4.5F, 11, 11);
		addGlassElement(builder, 11.5F, 5, 5, 11.75F, 11, 11);
		addGlassElement(builder, 5, 4.25F, 5, 11, 4.5F, 11);
		addGlassElement(builder, 5, 11.5F, 5, 11, 11.75F, 11);
	}

	private static void addNorthSouthTube (ExtendedModelTemplateBuilder builder, float minZ, float maxZ, TextureSlot frameTexture) {
		addFrameElement(builder, 4, 4, minZ, 5, 5, maxZ, frameTexture);
		addFrameElement(builder, 11, 4, minZ, 12, 5, maxZ, frameTexture);
		addFrameElement(builder, 4, 11, minZ, 5, 12, maxZ, frameTexture);
		addFrameElement(builder, 11, 11, minZ, 12, 12, maxZ, frameTexture);

		addGlassElement(builder, 5, 5, minZ, 5.25F, 11, maxZ);
		addGlassElement(builder, 10.75F, 5, minZ, 11, 11, maxZ);
		addGlassElement(builder, 5, 5, minZ, 11, 5.25F, maxZ);
		addGlassElement(builder, 5, 10.75F, minZ, 11, 11, maxZ);
	}

	private static void addFrameElement (ExtendedModelTemplateBuilder builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, TextureSlot texture) {
		builder.element(element -> element.from(minX, minY, minZ).to(maxX, maxY, maxZ).textureAll(texture));
	}

	private static void addGlassElement (ExtendedModelTemplateBuilder builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		builder.element(element -> element.from(minX, minY, minZ).to(maxX, maxY, maxZ).textureAll(CABLE_GLASS_TEXTURE));
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
