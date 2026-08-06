package matterfactory.common.block.cable;

import com.mojang.serialization.MapCodec;
import lombok.Getter;
import matterfactory.core.Tier;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class FluidPipe extends CableBlock {

	public static final MapCodec<FluidPipe> CODEC = simpleCodec(properties -> new FluidPipe(properties, Tier.BASIC));

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
		return neighborState.getBlock() instanceof FluidPipe;
	}

	@Override
	public @NotNull CableRenderGeometry getRenderGeometry () {
		return CableRenderGeometry.POWER_CABLE;
	}

	@Override
	public @NotNull ModelTemplate getCenterModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_center").requiredTextureSlot(CABLE_CENTER_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
				.element(element -> element.from(5, 5, 5).to(11, 11, 11).textureAll(CABLE_CENTER_TEXTURE)).build();
	}

	@Override
	public @NotNull ModelTemplate getArmModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_arm").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
				.element(element -> element.from(6, 6, 0).to(10, 10, 5).textureAll(CABLE_ARM_TEXTURE)).build();
	}

	@Override
	public @NotNull ModelTemplate getStraightModel () {
		return ExtendedModelTemplateBuilder.builder().suffix("_straight").requiredTextureSlot(CABLE_ARM_TEXTURE).requiredTextureSlot(TextureSlot.PARTICLE)
				.element(element -> element.from(6, 6, 0).to(10, 10, 16).textureAll(CABLE_ARM_TEXTURE)).build();
	}

	@Override
	public TagKey<Block> getMiningTier () {
		return BlockTags.NEEDS_IRON_TOOL;
	}

}
