package matterfactory.common.registries;

import matterfactory.Tier;
import matterfactory.common.FTab;
import matterfactory.common.block.BaseBlock;
import matterfactory.common.block.MachineBlock;
import matterfactory.common.block.cable.PowerCable;
import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.definition.ItemDefinition;
import matterfactory.common.item.BaseBlockItem;
import matterfactory.core.Factory;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class FBlocks {

	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(Factory.MODID);

	public static final List<BlockDefinition<?>> BLOCKS = new ArrayList<>();

	public static final BlockDefinition<MachineBlock> MACHINE_FRAME = register("Machine Frame", properties -> new MachineBlock(MachineBlock.Type.MACHINE_FRAME, properties), MachineBlock.Type.MACHINE_FRAME::createProperties);

	public static final BlockDefinition<PowerCable> BASIC_POWER_CABLE = register("Basic Power Cable", properties -> new PowerCable(properties, Tier.BASIC));
	public static final BlockDefinition<PowerCable> ADVANCED_POWER_CABLE = register("Advanced Power Cable", properties -> new PowerCable(properties, Tier.ADVANCED));
	public static final BlockDefinition<PowerCable> ELITE_POWER_CABLE = register("Elite Power Cable", properties -> new PowerCable(properties, Tier.ELITE));
	public static final BlockDefinition<PowerCable> ULTIMATE_POWER_CABLE = register("Ultimate Power Cable", properties -> new PowerCable(properties, Tier.ULTIMATE));
	public static final BlockDefinition<PowerCable> INFINITE_POWER_CABLE = register("Infinite Power Cable", properties -> new PowerCable(properties, Tier.INFINITE));

	public static List<BlockDefinition<?>> getBlocks () {
		return Collections.unmodifiableList(BLOCKS);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, final Function<BlockBehaviour.Properties, T> supplier) {
		var resourceFriendly = name.toLowerCase().replace(' ', '_');
		return register(name, Factory.get(resourceFriendly), supplier, null);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, final Function<BlockBehaviour.Properties, T> supplier, Supplier<BlockBehaviour.Properties> properties) {
		var resourceFriendly = name.toLowerCase().replace(' ', '_');
		return register(name, Factory.get(resourceFriendly), supplier, properties, null);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, String resourceName, final Function<BlockBehaviour.Properties, T> supplier) {
		return register(name, Factory.get(resourceName), supplier, null);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, Identifier id, final Function<BlockBehaviour.Properties, T> supplier, @Nullable BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
		return register(name, id, supplier, BlockBehaviour.Properties::of, itemFactory);
	}

	public static <T extends Block> BlockDefinition<T> register (final String name, Identifier id, final Function<BlockBehaviour.Properties, T> supplier, Supplier<BlockBehaviour.Properties> properties, @Nullable BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
		var deferredBlock = REGISTRY.registerBlock(id.getPath(), supplier, properties);
		var deferredItem = FItems.REGISTRY.register(id.getPath(), () -> {
			var block = deferredBlock.get();
			var itemProperties = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix();
			if (itemFactory != null) {
				var item = itemFactory.apply(block, itemProperties);
				if (item == null) {
					throw new IllegalArgumentException("BlockItem factory for " + id + " returned null");
				}
				return item;
			} else if (block instanceof BaseBlock) {
				return new BaseBlockItem(block, itemProperties);
			} else {
				return new BlockItem(block, itemProperties);
			}
		});
		var itemDef = new ItemDefinition<>(name, deferredItem);
		FTab.add(itemDef);

		BlockDefinition<T> definition = new BlockDefinition<>(name, deferredBlock, itemDef);
		BLOCKS.add(definition);
		return definition;
	}

}
