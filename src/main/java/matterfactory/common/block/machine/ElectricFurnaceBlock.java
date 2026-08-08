package matterfactory.common.block.machine;

import com.mojang.serialization.MapCodec;
import matterfactory.common.block.BaseBlock;
import matterfactory.common.block.entity.machine.ElectricFurnaceBlockEntity;
import matterfactory.core.datagen.util.IPickaxe;

public class ElectricFurnaceBlock extends AbstractMachineEntityBlock<ElectricFurnaceBlockEntity> implements IPickaxe {

	public ElectricFurnaceBlock (Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<BaseBlock> getCodec () {
		return simpleCodec(ElectricFurnaceBlock::new);
	}

}
