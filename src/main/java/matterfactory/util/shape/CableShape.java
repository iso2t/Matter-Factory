package matterfactory.util.shape;

import net.minecraft.world.phys.shapes.VoxelShape;

public record CableShape(VoxelShape core, VoxelShape down, VoxelShape up, VoxelShape north, VoxelShape south, VoxelShape west, VoxelShape east) {

	public static CableShape from (VoxelShape core, VoxelShape down, VoxelShape up, VoxelShape north, VoxelShape south, VoxelShape west, VoxelShape east) {
		return new CableShape(core, down, up, north, south, west, east);
	}

}
