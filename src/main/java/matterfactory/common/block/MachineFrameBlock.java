package matterfactory.common.block;

import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.model.CustomBlockModel;
import matterfactory.core.Factory;
import matterfactory.core.datagen.util.IPickaxe;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SoundType;

public class MachineFrameBlock extends MachineBlock implements CustomBlockModel, IPickaxe {

    private static final Identifier MODEL = Factory.get("block/machine_frame");

	public MachineFrameBlock (Properties properties) {
        super(Type.MACHINE_FRAME, properties.noOcclusion().requiresCorrectToolForDrops().strength(8.0F, 6.0F).sound(SoundType.IRON));
	}

	@Override
	public void registerModel (BlockModelGenerators generator, BlockDefinition<?> block) {
		MultiVariant model = BlockModelGenerators.variant(new Variant(MODEL));
		generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.getBlock(), model));
        generator.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(MODEL));
	}

}
