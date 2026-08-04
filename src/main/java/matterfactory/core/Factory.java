package matterfactory.core;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Collection;

public interface Factory {

	String MODID = "matterfactory";
	String NAME  = "Matter Factory";

	static Factory instance () {
		return FactoryBase.INSTANCE;
	}

	static Identifier get (String value) {
		return get(MODID, value);
	}

	static Identifier get (String id, String value) {
		return Identifier.fromNamespaceAndPath(id, value);
	}

	static Identifier getMinecraft (String value) {
		return Identifier.withDefaultNamespace(value);
	}

	static Path gameDirectory () {
		return FMLPaths.GAMEDIR.get();
	}

	static Path configDirectory () {
		return FMLPaths.CONFIGDIR.get();
	}

	static Path modsDirectory () {
		return FMLPaths.MODSDIR.get();
	}

	Collection<ServerPlayer> getPlayers ();

	Level getClientLevel ();

	MinecraftServer getCurrentServer ();

}
