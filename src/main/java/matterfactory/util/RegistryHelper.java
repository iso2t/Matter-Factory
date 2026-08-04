package matterfactory.util;

public class RegistryHelper {

	/**
	 * Converts the provided name into a registry-friendly format.
	 * The resulting format is all lowercase with spaces replaced by underscores.
	 *
	 * @param name the input string to be converted into a registry-friendly name
	 * @return the converted string in registry-friendly format
	 */
	public static String getRegistryFriendlyName (String name) {
		return name.toLowerCase().replace(" ", "_");
	}

}
