package matterfactory.client;

import matterfactory.common.definition.FluidDefinition;
import matterfactory.common.fluid.BaseFluid;
import matterfactory.common.registries.FactoryFluids;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ClientFluidRegistration {

	private static final float FOG_START = 1.0F;
	private static final float FOG_END   = 6.0F;

	private ClientFluidRegistration () {
	}

	static void registerExtensions (RegisterClientExtensionsEvent event) {
		for (FluidDefinition definition : FactoryFluids.getFluids()) {
			if (definition.type().get() instanceof BaseFluid fluid) {
				event.registerFluidType(createExtension(fluid), fluid);
			}
		}
	}

	static void registerModels (RegisterFluidModelsEvent event) {
		for (FluidDefinition definition : FactoryFluids.getFluids()) {
			if (definition.type().get() instanceof BaseFluid fluid) {
				FluidModel.Unbaked model = new FluidModel.Unbaked(
						new Material(fluid.getStillTexture()),
						new Material(fluid.getFlowingTexture()),
						fluid.isOpaque() ? null : new Material(fluid.getOverlayTexture()),
						state -> fluid.getTintColor()
				);
				event.register(model, definition.source(), definition.flowing());
			}
		}
	}

	private static IClientFluidTypeExtensions createExtension (BaseFluid fluid) {
		Vector3f fogColor = new Vector3f(fluid.getFogColor());
		return new IClientFluidTypeExtensions() {
			@Override
			public void modifyFogColor (@NonNull Camera camera, float partialTick, @NonNull ClientLevel level, int renderDistance, float darkenWorldAmount, @NonNull Vector4f fluidFogColor) {
				fluidFogColor.set(fogColor.x, fogColor.y, fogColor.z, fluidFogColor.w);
			}

			@Override
			public void modifyFogRender (@NonNull Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
				fogData.environmentalStart = FOG_START;
				fogData.environmentalEnd = FOG_END;
				fogData.skyEnd = FOG_END;
				fogData.cloudEnd = FOG_END;
			}
		};
	}

}
