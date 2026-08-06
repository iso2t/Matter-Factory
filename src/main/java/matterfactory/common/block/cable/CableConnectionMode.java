package matterfactory.common.block.cable;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum CableConnectionMode implements StringRepresentable {

	AUTO("auto"),
	IMPORT("import"),
	EXPORT("export");

	private final String serializedName;

	CableConnectionMode (String serializedName) {
		this.serializedName = serializedName;
	}

	public CableConnectionMode next () {
		return switch (this) {
			case AUTO -> IMPORT;
			case IMPORT -> EXPORT;
			case EXPORT -> AUTO;
		};
	}

	public static CableConnectionMode byName (String name) {
		for (CableConnectionMode mode : values()) {
			if (mode.serializedName.equals(name)) {
				return mode;
			}
		}

		return AUTO;
	}

	@Override
	public @NotNull String getSerializedName () {
		return serializedName;
	}

}
