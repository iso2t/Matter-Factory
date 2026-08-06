package matterfactory.core.datagen.sound;

import matterfactory.common.registries.FSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class SoundProvider extends SoundDefinitionsProvider {

	public SoundProvider (PackOutput output) {
		super(output, matterfactory.core.Factory.MODID);
	}

	@Override
	public void registerSounds () {
		add(FSounds.WRENCH_USE.get(), SoundDefinition.definition()
				.with(sound(matterfactory.core.Factory.MODID + ":wrench_use", SoundDefinition.SoundType.SOUND)
						.volume(1.f).pitch(1.f).weight(1))
				.subtitle("subtitles.matterfactory.wrench_use"));
	}
}
