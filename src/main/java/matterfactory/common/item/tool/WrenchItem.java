package matterfactory.common.item.tool;

import matterfactory.common.item.BaseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.NonNull;

public class WrenchItem extends BaseItem {

	public WrenchItem (Properties properties) {
		super(properties);
	}

	@Override
	public boolean doesSneakBypassUse (@NonNull ItemStack stack, @NonNull LevelReader level, @NonNull BlockPos pos, @NonNull Player player) {
		return true;
	}

}
