package matterfactory.core.datagen.tags;

import matterfactory.common.registries.FactoryBlocks;
import matterfactory.core.datagen.util.IAxe;
import matterfactory.core.datagen.util.IPickaxe;
import matterfactory.core.datagen.util.IShovel;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class FactoryBlockTagsGenerator extends BlockTagsProvider {

	public FactoryBlockTagsGenerator (PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider, matterfactory.core.Factory.MODID);
	}

	@Override
	protected void addTags (HolderLookup.@NonNull Provider provider) {
		for (var block : FactoryBlocks.getBlocks()) {
			var key = block.block().getKey();
			if (block.getBlock() instanceof IPickaxe pickaxe) {
				this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(key);
				this.tag(pickaxe.getMiningTier()).add(key);
			}

			if (block.getBlock() instanceof IAxe axe) {
				this.tag(BlockTags.MINEABLE_WITH_AXE).add(key);
				this.tag(axe.getMiningTier()).add(key);
			}

			if (block.getBlock() instanceof IShovel shovel) {
				this.tag(BlockTags.MINEABLE_WITH_SHOVEL).add(key);
				this.tag(shovel.getMiningTier()).add(key);
			}
		}
	}
}
