package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import matterfactory.core.Tier;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jspecify.annotations.NonNull;

public class PowerCable extends EntityCableBlock<PowerCableBlockEntity> {

	public static final MapCodec<PowerCable> CODEC = simpleCodec(properties -> new PowerCable(properties, Tier.BASIC));

	@Getter
	private final Tier tier;

	public PowerCable (Properties properties, Tier tier) {
		super(properties.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE));
		this.tier = tier;
	}

	@Override
	public @NonNull MapCodec<? extends CableBlock> getCodec () {
		return CODEC;
	}

	@Override
	protected boolean hasEndpointCapability (Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
		return level.getCapability(Capabilities.Energy.BLOCK, pos, state, level.getBlockEntity(pos), side) != null;
	}

	@Override
	public @NotNull CableRenderGeometry getRenderGeometry () {
		return CableRenderGeometry.POWER_CABLE;
	}

	@Override
	public @NotNull ModelTemplate getCenterModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_center").requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE).element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_CENTER_TEXTURE)).build();
	}

	@Override
	public @NotNull ModelTemplate getArmModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_arm").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE).element(element -> element.from(6, 6, 0).to(10, 10, 5).textureAll(CABLE_ARM_TEXTURE)).build();
	}

	@Override
	public @NotNull ModelTemplate getStraightModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_straight").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE).element(element -> element.from(6, 6, 0).to(10, 10, 16).textureAll(CABLE_ARM_TEXTURE)).build();
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker (@NonNull Level level, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
		return type == getBlockEntityType() ? (tickLevel, pos, tickState, blockEntity) -> PowerCableBlockEntity.serverTick(tickLevel, pos, tickState, (PowerCableBlockEntity) blockEntity) : null;
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
