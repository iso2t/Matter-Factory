package matterfactory.common.registries;

import matterfactory.common.fluid.BaseFluid;
import matterfactory.core.Factory;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class FactoryFluidTypes {

	private static final List<BaseFluid> FLUID_TYPES = new ArrayList<>();

	public static final Identifier WATER_STILL   = Factory.getMinecraft("block/water_still");
	public static final Identifier WATER_FLOWING = Factory.getMinecraft("block/water_flow");
	public static final Identifier WATER_OVERLAY = Factory.getMinecraft("block/water_overlay");
	public static final Identifier OPAQUE_STILL  = Factory.get("block/fluid/opaque_still");
	public static final Identifier OPAQUE_FLOW   = Factory.get("block/fluid/opaque_flow");

	public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Factory.MODID);

	public static List<BaseFluid> getFluidTypes () {
		return Collections.unmodifiableList(FLUID_TYPES);
	}

	static Supplier<FluidType> register (String name, BaseFluid type) {
		FLUID_TYPES.add(type);
		return REGISTRY.register(name, () -> type);
	}

}
