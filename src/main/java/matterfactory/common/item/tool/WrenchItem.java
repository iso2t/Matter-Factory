package matterfactory.common.item.tool;

import matterfactory.common.item.BaseItem;
import matterfactory.common.registries.FSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

	public InteractionResult successfulWrenchAction (Player player, ItemStack stack, LevelReader levelReader, BlockPos pos, boolean play) {
		if (!(levelReader instanceof Level level)) return InteractionResult.SUCCESS;
		if (!(stack.getItem() instanceof WrenchItem)) return InteractionResult.SUCCESS;

		if (play) level.playSound(player, pos, FSounds.WRENCH_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

		return InteractionResult.SUCCESS;
	}

}
