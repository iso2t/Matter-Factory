package matterfactory.common.item;

import matterfactory.common.block.cable.EntityCableBlock;
import matterfactory.common.block.cable.FacadeBlock;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FacadeBlockItem extends BlockItem {

	public FacadeBlockItem (Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn (UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(getBlock() instanceof FacadeBlock facade) || !(state.getBlock() instanceof EntityCableBlock<?>) || !(level.getBlockEntity(pos) instanceof BaseCableBlockEntity cable)) {
			return super.useOn(context);
		}

		if (!level.isClientSide()) {
			facade.cover(level, pos, state, cable);
			if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
		}

		return InteractionResult.SUCCESS;
	}

}
