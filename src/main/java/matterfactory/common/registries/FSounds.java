package matterfactory.common.registries;

import matterfactory.core.Factory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class FSounds {

	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Factory.MODID);

	public static final Supplier<SoundEvent> WRENCH_USE = registerFixed("wrench_use");

	private static Supplier<SoundEvent> registerFixed (String name) {
		return REGISTRY.register(name, () -> SoundEvent.createFixedRangeEvent(Factory.get(name), 1.0F));
	}

	private static Supplier<SoundEvent> registerVariable (String name) {
		return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(Factory.get(name)));
	}

}
