package matterfactory.common.registries;

import com.google.common.base.Preconditions;
import matterfactory.common.block.BaseEntityBlock;
import matterfactory.common.block.entity.BaseBlockEntity;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.definition.BlockEntityDefinition;
import matterfactory.core.Factory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Factory.MODID);

	private static final List<BlockEntityDefinition<?>> BLOCK_ENTITIES = new ArrayList<>();

	public static List<BlockEntityDefinition<?>> getBlockEntities () {
		return Collections.unmodifiableList(BLOCK_ENTITIES);
	}

	/*@SuppressWarnings("unchecked")
	@SafeVarargs
	private static <T extends BaseBlockEntity> BlockEntityDefinition<T> create (String id, Class<T> entityClass, BlockEntityFactory<T> factory, BlockDefinition<? extends BaseEntityBlock<?>>... blockDefinitions) {
		Preconditions.checkArgument(blockDefinitions.length > 0);
		var deferred = REGISTRY.register(id, () -> {
			AtomicReference<BlockEntityType<T>> typeHolder = new AtomicReference<>();
			BlockEntityType.BlockEntitySupplier<T> supplier = (blockPos, blockState) -> factory.create(typeHolder.get(), blockPos, blockState);

			var blocks = Arrays.stream(blockDefinitions).map(BlockDefinition::getBlock).toArray(BaseEntityBlock[]::new);
			var type = new BlockEntityType(supplier, blocks);
			typeHolder.setPlain(type);

			for (var block : blocks) {
				BaseEntityBlock<T> baseBlock = (BaseEntityBlock<T>) block;
				baseBlock.setBlockEntity(entityClass, type);
			}

			return type;
		});

		var result = new BlockEntityDefinition<>(entityClass, deferred);
		BLOCK_ENTITIES.add(result);
		return result;
	}*/

	@FunctionalInterface
	interface BlockEntityFactory<T extends BaseBlockEntity> {
		T create (BlockEntityType<T> type, BlockPos pos, BlockState state);
	}

}
