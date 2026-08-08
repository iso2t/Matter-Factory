package matterfactory.common.block.machine;

/**
 * Immutable storage limits shared by a machine block entity and its capability handlers.
 */
public record MachineConfiguration(int inventorySlots, int energyCapacity, int maxEnergyInsert, int maxEnergyExtract) {

	public MachineConfiguration {
		if (inventorySlots <= 0) {
			throw new IllegalArgumentException("inventorySlots must be positive");
		}

		if (energyCapacity <= 0 || maxEnergyInsert < 0 || maxEnergyExtract < 0) {
			throw new IllegalArgumentException("Energy limits must be non-negative and capacity must be positive");
		}
	}

	public static MachineConfiguration of (int inventorySlots, int energyCapacity, int maxEnergyTransfer) {
		return new MachineConfiguration(inventorySlots, energyCapacity, maxEnergyTransfer, maxEnergyTransfer);
	}

}
