package matterfactory.common.block.entity.machine;

import matterfactory.common.block.machine.AbstractMachineBlockEntity;
import matterfactory.common.block.machine.MachineConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlockEntity extends AbstractMachineBlockEntity {

	public ElectricFurnaceBlockEntity (BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, MachineConfiguration.of(4, 50_000, 2_048));
	}

}
