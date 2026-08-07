package matterfactory.common.registries;

import com.google.common.base.Preconditions;
import matterfactory.common.block.cable.EntityCableBlock;
import matterfactory.common.block.cable.FacadeBlock;
import matterfactory.common.block.entity.BaseCableBlockEntity;
import matterfactory.common.block.entity.FacadeBlockEntity;
import matterfactory.common.block.entity.FluidPipeBlockEntity;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import matterfactory.common.block.entity.BaseBlockEntity;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.definition.BlockEntityDefinition;
import matterfactory.core.Factory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FactoryBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Factory.MODID);

	private static final List<BlockEntityDefinition<?>> BLOCK_ENTITIES = new ArrayList<>();

	public static final BlockEntityDefinition<PowerCableBlockEntity> POWER_CABLE = create("power_cable", PowerCableBlockEntity.class, PowerCableBlockEntity::new, FactoryBlocks.BASIC_POWER_CABLE, FactoryBlocks.ADVANCED_POWER_CABLE, FactoryBlocks.ELITE_POWER_CABLE, FactoryBlocks.ULTIMATE_POWER_CABLE, FactoryBlocks.INFINITE_POWER_CABLE);
	public static final BlockEntityDefinition<FluidPipeBlockEntity>   FLUID_PIPE  = create("fluid_pipe", FluidPipeBlockEntity.class, FluidPipeBlockEntity::new, FactoryBlocks.BASIC_FLUID_PIPE, FactoryBlocks.ADVANCED_FLUID_PIPE, FactoryBlocks.ELITE_FLUID_PIPE, FactoryBlocks.ULTIMATE_FLUID_PIPE, FactoryBlocks.INFINITE_FLUID_PIPE);
	public static final BlockEntityDefinition<ItemPipeBlockEntity>   ITEM_PIPE   = create("item_pipe", ItemPipeBlockEntity.class, ItemPipeBlockEntity::new, FactoryBlocks.BASIC_ITEM_PIPE, FactoryBlocks.ADVANCED_ITEM_PIPE, FactoryBlocks.ELITE_ITEM_PIPE, FactoryBlocks.ULTIMATE_ITEM_PIPE, FactoryBlocks.INFINITE_ITEM_PIPE);
	public static final BlockEntityDefinition<FacadeBlockEntity>     FACADE      = createFacade();

	public static List<BlockEntityDefinition<?>> getBlockEntities () {
		return Collections.unmodifiableList(BLOCK_ENTITIES);
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	private static <T extends BaseCableBlockEntity, B extends EntityCableBlock<T>> BlockEntityDefinition<T> create (String id, Class<T> entityClass, BlockEntityFactory<T> factory, BlockDefinition<? extends B>... blockDefinitions) {
		Preconditions.checkArgument(blockDefinitions.length > 0);
		var deferred = REGISTRY.register(id, () -> {
			AtomicReference<BlockEntityType<T>> typeHolder = new AtomicReference<>();
			BlockEntityType.BlockEntitySupplier<T> supplier = (blockPos, blockState) -> factory.create(typeHolder.get(), blockPos, blockState);

			var blocks = Arrays.stream(blockDefinitions).map(BlockDefinition::getBlock).toArray(EntityCableBlock[]::new);
			var type = new BlockEntityType<>(supplier, blocks);
			typeHolder.setPlain(type);

			for (var block : blocks) {
				((B) block).setBlockEntity(entityClass, type);
			}

			return type;
		});

		var result = new BlockEntityDefinition<>(entityClass, deferred);
		BLOCK_ENTITIES.add(result);
		return result;
	}

	private static BlockEntityDefinition<FacadeBlockEntity> createFacade () {
		var deferred = REGISTRY.register("facade", () -> {
			AtomicReference<BlockEntityType<FacadeBlockEntity>> typeHolder = new AtomicReference<>();
			BlockEntityType<FacadeBlockEntity> type = new BlockEntityType<>((pos, state) -> new FacadeBlockEntity(typeHolder.get(), pos, state), FactoryBlocks.FACADE.getBlock());
			typeHolder.setPlain(type);
			FactoryBlocks.FACADE.getBlock().setBlockEntityType(type);
			return type;
		});

		var result = new BlockEntityDefinition<>(FacadeBlockEntity.class, deferred);
		BLOCK_ENTITIES.add(result);
		return result;
	}

	@FunctionalInterface
	interface BlockEntityFactory<T extends BaseBlockEntity> {
		T create (BlockEntityType<T> type, BlockPos pos, BlockState state);
	}

}
