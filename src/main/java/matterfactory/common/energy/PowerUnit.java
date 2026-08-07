package matterfactory.common.energy;

import lombok.Getter;

@Getter
public enum PowerUnit {

	FORGE_ENERGY("FE"),
	ENERGY_UNITS("EU"),
	REDSTONE_FLUX("RF");

	final int    factor = 10;
	final String abbreviation;

	PowerUnit (final String abbreviation) {
		this.abbreviation = abbreviation;
	}

	/**
	 * Convert from RF/FE to EU
	 * @param amount 10 RF/FE = 1 EU
	 * @return converted amount
	 */
	public float toEU (float amount) {
		return amount / factor;
	}

	/**
	 * Convert from EU to RF/FE
	 * @param amount 1 EU = 10 RF/FE
	 * @return converted amount
	 */
	public float toRF (float amount) {
		return amount * factor;
	}

	@Override
	public String toString () {
		return getAbbreviation();
	}
}
