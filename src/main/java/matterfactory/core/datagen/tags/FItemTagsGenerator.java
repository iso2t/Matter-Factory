package matterfactory.core.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class FItemTagsGenerator extends ItemTagsProvider {

	public FItemTagsGenerator (PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider, matterfactory.core.Factory.MODID);
	}

	@Override
	protected void addTags (HolderLookup.@NonNull Provider provider) {

	}
}
