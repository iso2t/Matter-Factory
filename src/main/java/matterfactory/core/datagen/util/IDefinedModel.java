package matterfactory.core.datagen.util;

import matterfactory.common.definition.BlockDefinition;
import net.minecraft.client.data.models.BlockModelGenerators;

/**
 * Supplies blockstate and item definitions for a block whose block model is
 * authored outside the standard model generator.
 */
public interface IDefinedModel {

	void registerDefinedModel (BlockModelGenerators generators, BlockDefinition<?> block);
}
