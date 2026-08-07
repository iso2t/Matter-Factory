package matterfactory.common.registries;

import matterfactory.common.definition.BlockDefinition;
import matterfactory.common.definition.FluidDefinition;
import matterfactory.common.definition.ItemDefinition;
import matterfactory.common.fluid.BaseFluid;
import matterfactory.core.Factory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class FactoryFluids {

	public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(BuiltInRegistries.FLUID, Factory.MODID);

	private static final List<FluidDefinition> FLUIDS = new ArrayList<>();

	public static final FluidDefinition HEAVY_WATER     = registerFluid("Heavy Water", 0xFF99B3FF, new Vector3f(0.5F, 0.6F, 1.0F), 1105, 1100);
	public static final FluidDefinition DISTILLED_WATER = registerFluid("Distilled Water", 0xFF66B3FF, new Vector3f(0.4F, 0.6F, 1.0F), 1000, 1000);

	// Refining fluids
	public static final FluidDefinition SULFURIC_ACID          = registerFluid("Sulfuric Acid", 0xFFE0ED61, new Vector3f(0.8F, 0.9F, 0.4F), 1840, 1800);
	public static final FluidDefinition HYDROCHLORIC_ACID      = registerFluid("Hydrochloric Acid", 0xFFB2E8A3, new Vector3f(0.6F, 0.9F, 0.6F), 1190, 1050);
	public static final FluidDefinition NITRIC_ACID            = registerFluid("Nitric Acid", 0xFFECE88E, new Vector3f(0.9F, 0.9F, 0.5F), 1510, 1150);
	public static final FluidDefinition HYDROFLUORIC_ACID      = registerFluid("Hydrofluoric Acid", 0xFF91E9D5, new Vector3f(0.4F, 0.9F, 0.8F), 1150, 1100);
	public static final FluidDefinition SODIUM_HYDROXIDE       = registerFluid("Sodium Hydroxide", 0xFFC4DDFF, new Vector3f(0.7F, 0.8F, 1.0F), 1330, 1300);
	public static final FluidDefinition AMMONIA_SOLUTION       = registerFluid("Ammonia Solution", 0xFF81C9ED, new Vector3f(0.4F, 0.7F, 0.9F), 970, 950);
	public static final FluidDefinition FERRIC_CHLORIDE        = registerFluid("Ferric Chloride", 0xFFD18B32, new Vector3f(0.8F, 0.5F, 0.2F), 1450, 1700);
	public static final FluidDefinition AQUA_REGIA             = registerFluid("Aqua Regia", 0xFFF0B331, new Vector3f(0.9F, 0.6F, 0.2F), 1300, 1350);

	// Petroleum
	public static final FluidDefinition CRUDE_OIL               = registerFluid("Crude Oil", 0xFF201812, new Vector3f(0.1F, 0.08F, 0.05F), 920, 3500, true);
	public static final FluidDefinition REFINED_OIL             = registerFluid("Refined Oil", 0xFF4D3420, new Vector3f(0.3F, 0.2F, 0.1F), 860, 1200);
	public static final FluidDefinition GASOLINE                = registerFluid("Gasoline", 0xFFE9D36C, new Vector3f(0.9F, 0.8F, 0.3F), 740, 500);
	public static final FluidDefinition DIESEL                  = registerFluid("Diesel", 0xFF907228, new Vector3f(0.5F, 0.35F, 0.1F), 830, 900);

	public static List<FluidDefinition> getFluids () {
		return Collections.unmodifiableList(FLUIDS);
	}

	public static FluidDefinition registerFluid (String englishName, int tintColor, Vector3f fogColor, int density, int viscosity) {
		return registerFluid(englishName, tintColor, fogColor, density, viscosity, false);
	}

	public static FluidDefinition registerFluid (String englishName, int tintColor, Vector3f fogColor, int density, int viscosity, boolean opaque) {
		String baseName = englishName.toLowerCase(Locale.ROOT).replace(' ', '_');

		// FluidType
		FluidType.Properties fluidProperties = FluidType.Properties.create().density(density).viscosity(viscosity).motionScale(0.014D).canPushEntity(true).canSwim(true).canDrown(true).fallDistanceModifier(0.0F);
		Supplier<FluidType> type = FactoryFluidTypes.register(baseName, new BaseFluid(englishName, fluidProperties, opaque ? FactoryFluidTypes.OPAQUE_STILL : FactoryFluidTypes.WATER_STILL, opaque ? FactoryFluidTypes.OPAQUE_FLOW : FactoryFluidTypes.WATER_FLOWING, FactoryFluidTypes.WATER_OVERLAY, tintColor, fogColor, opaque));

		// Source/Flowing with forward references
		final AtomicReference<Supplier<FlowingFluid>> sourceRef = new AtomicReference<>();
		final AtomicReference<Supplier<FlowingFluid>> flowingRef = new AtomicReference<>();

		BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(type, () -> sourceRef.get().get(), () -> flowingRef.get().get());

		Supplier<FlowingFluid> SOURCE = REGISTRY.register("source_" + baseName, () -> new BaseFlowingFluid.Source(properties));
		Supplier<FlowingFluid> FLOWING = REGISTRY.register("flowing_" + baseName, () -> new BaseFlowingFluid.Flowing(properties));
		sourceRef.set(SOURCE);
		flowingRef.set(FLOWING);

		// Block
		BlockDefinition<LiquidBlock> BLOCK = FactoryBlocks.register(englishName, Factory.get(baseName + "_block"), blockProperties -> new LiquidBlock(SOURCE.get(), blockProperties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable(), null, false);

		// Bucket
		ItemDefinition<BucketItem> BUCKET = FactoryItems.register(englishName + " Bucket", "bucket_" + baseName, (Item.Properties p) -> new BucketItem(SOURCE.get(), p.stacksTo(1).craftRemainder(Items.BUCKET)));

		properties.block(BLOCK::getBlock).bucket(BUCKET);

		FluidDefinition definition = new FluidDefinition(englishName, type, SOURCE, FLOWING, BLOCK, BUCKET);
		FLUIDS.add(definition);
		return definition;
	}

}
