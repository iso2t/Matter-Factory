package matterfactory.client.color;

import com.mojang.serialization.MapCodec;
import matterfactory.common.fluid.BaseFluid;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;

public final class FluidItemTintSource implements ItemTintSource {

	public static final FluidItemTintSource           INSTANCE = new FluidItemTintSource();
	public static final MapCodec<FluidItemTintSource> CODEC    = MapCodec.unit(INSTANCE);

	private FluidItemTintSource () {
	}

	@Override
	public int calculate (ItemStack stack, ClientLevel level, LivingEntity entity) {
		if (stack.getItem() instanceof BucketItem bucket && bucket.getContent().getFluidType() instanceof BaseFluid fluid) {
			return fluid.getTintColor();
		}
		return 0xFFFFFFFF;
	}

	@Override
	public MapCodec<FluidItemTintSource> type () {
		return CODEC;
	}

}
