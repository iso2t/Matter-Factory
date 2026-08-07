package matterfactory.common.block.entity;

import matterfactory.common.block.cable.EntityCableBlock;
import matterfactory.common.block.cable.FacadeBlock;
import matterfactory.common.block.entity.FluidPipeBlockEntity;
import matterfactory.common.block.entity.ItemPipeBlockEntity;
import matterfactory.common.block.entity.PowerCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/** Stores the painted appearance and the cable replaced by a facade. */
public class FacadeBlockEntity extends BaseBlockEntity {

	private BlockState coveredState = Blocks.AIR.defaultBlockState();
	private BlockState paintedState = Blocks.AIR.defaultBlockState();
	@Nullable
	private BaseCableBlockEntity coveredCable;
	@Nullable
	private CompoundTag coveredCableData;

	public FacadeBlockEntity (BlockEntityType<FacadeBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void serverTick (Level level, BlockPos pos, BlockState state, FacadeBlockEntity facade) {
		PowerCableBlockEntity cable = facade.getCoveredCable(PowerCableBlockEntity.class);
		if (cable != null) {
			PowerCableBlockEntity.serverTick(level, pos, facade.coveredState, cable);
			return;
		}

		ItemPipeBlockEntity itemPipe = facade.getCoveredCable(ItemPipeBlockEntity.class);
		if (itemPipe != null) {
			ItemPipeBlockEntity.serverTick(level, pos, facade.coveredState, itemPipe);
			return;
		}

		FluidPipeBlockEntity fluidPipe = facade.getCoveredCable(FluidPipeBlockEntity.class);
		if (fluidPipe != null) {
			FluidPipeBlockEntity.serverTick(level, pos, facade.coveredState, fluidPipe);
		}
	}

	public void setCoveredCable (BlockState state, BaseCableBlockEntity cable) {
		coveredState = state;
		coveredCable = cable;
		coveredCableData = null;
		cable.clearRemoved();
		if (level != null) {
			cable.setLevel(level);
		}
		cable.setBlockState(state);
		markUpdated();
	}

	public BlockState getCoveredState () {
		return coveredState;
	}

	public void setCoveredState (BlockState state) {
		coveredState = state;
		if (coveredCable != null) {
			coveredCable.setBlockState(state);
		}
		markUpdated();
	}

	public BlockState getPaintedState () {
		return paintedState;
	}

	public void setPaintedState (BlockState state) {
		paintedState = state;
		if (level != null && !level.isClientSide() && !getBlockState().getValue(FacadeBlock.PAINTED)) {
			level.setBlock(worldPosition, getBlockState().setValue(FacadeBlock.PAINTED, true), Block.UPDATE_ALL);
		}
		markUpdated();
	}

	public void restoreCoveredCable (Level level) {
		restoreCoveredCable();
		if (coveredCable == null || coveredState.isAir()) {
			return;
		}

		level.setBlock(worldPosition, coveredState, 3);
		coveredCable.clearRemoved();
		coveredCable.setLevel(level);
		coveredCable.setBlockState(coveredState);
		level.setBlockEntity(coveredCable);
	}

	@Nullable
	public EnergyHandler getEnergyHandler (@Nullable net.minecraft.core.Direction direction) {
		PowerCableBlockEntity cable = getCoveredCable(PowerCableBlockEntity.class);
		return cable == null ? null : cable.getEnergyHandler(direction);
	}

	@Nullable
	public ResourceHandler<ItemResource> getItemHandler (@Nullable net.minecraft.core.Direction direction) {
		ItemPipeBlockEntity pipe = getCoveredCable(ItemPipeBlockEntity.class);
		return pipe == null ? null : pipe.getItemHandler(direction);
	}

	@Nullable
	public ResourceHandler<FluidResource> getFluidHandler (@Nullable net.minecraft.core.Direction direction) {
		FluidPipeBlockEntity pipe = getCoveredCable(FluidPipeBlockEntity.class);
		return pipe == null ? null : pipe.getFluidHandler(direction);
	}

	@Nullable
	public <T extends BaseCableBlockEntity> T getCoveredCable (Class<T> type) {
		restoreCoveredCable();
		return type.isInstance(coveredCable) ? type.cast(coveredCable) : null;
	}

	private void restoreCoveredCable () {
		if (coveredCable != null || coveredCableData == null || level == null || !(coveredState.getBlock() instanceof EntityCableBlock<?>)) {
			return;
		}

		BlockEntity blockEntity = BlockEntity.loadStatic(worldPosition, coveredState, coveredCableData, level.registryAccess());
		if (blockEntity instanceof BaseCableBlockEntity cable) {
			cable.setLevel(level);
			cable.setBlockState(coveredState);
			coveredCable = cable;
		}
	}

	@Override
	public void setLevel (@NonNull Level level) {
		super.setLevel(level);
		restoreCoveredCable();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket () {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public @NonNull CompoundTag getUpdateTag (HolderLookup.@NonNull Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void loadAdditional (@NonNull ValueInput input) {
		super.loadAdditional(input);
		coveredState = input.read("covered_state", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		paintedState = input.read("painted_state", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		coveredCableData = input.read("covered_cable", CompoundTag.CODEC).orElse(null);
		coveredCable = null;
		restoreCoveredCable();
	}

	@Override
	protected void saveAdditional (@NonNull ValueOutput output) {
		super.saveAdditional(output);
		output.store("covered_state", BlockState.CODEC, coveredState);
		output.store("painted_state", BlockState.CODEC, paintedState);
		CompoundTag data = coveredCableData;
		if (coveredCable != null && level != null) {
			data = coveredCable.saveWithFullMetadata(level.registryAccess());
		}
		if (data != null) {
			output.store("covered_cable", CompoundTag.CODEC, data);
		}
	}

	private void markUpdated () {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

}
