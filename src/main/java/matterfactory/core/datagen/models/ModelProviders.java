package matterfactory.core.datagen.models;

import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

public abstract sealed class ModelProviders extends ModelProvider permits FactoryModelProvider {

	public ModelProviders (PackOutput output) {
		super(output, matterfactory.core.Factory.MODID);
	}

}
