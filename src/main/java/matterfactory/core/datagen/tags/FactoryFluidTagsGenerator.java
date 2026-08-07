package matterfactory.core.datagen.tags;

import matterfactory.common.registries.FactoryFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class FactoryFluidTagsGenerator extends FluidTagsProvider {

	public FactoryFluidTagsGenerator (PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider);
	}

	@Override
	protected void addTags (HolderLookup.@NonNull Provider provider) {
		var water = tag(FluidTags.WATER);
		for (var fluid : FactoryFluids.getFluids()) {
			water.add(BuiltInRegistries.FLUID.getResourceKey(fluid.source().get()).orElseThrow(), BuiltInRegistries.FLUID.getResourceKey(fluid.flowing().get()).orElseThrow());
		}
	}

}
