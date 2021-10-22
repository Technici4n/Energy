package team.reborn.energy.api.base;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * A base energy storage implementation with a dynamic capacity, and per-side per-operation insertion and extraction limits.
 * {@link #getSideStorage} can be used to get an {@code EnergyStorage} implementation for a given side.
 * Make sure to override {@link #onFinalCommit} to call {@code markDirty} and similar functions.
 *
 * <p>Use {@link SimpleSidedEnergyContainer} if you an amount field is enough for you.
 */
@SuppressWarnings({"unused", "deprecation", "UnstableApiUsage"})
public abstract class SidedEnergyContainer extends SnapshotParticipant<Long> {
	private final SideStorage[] sideStorages = new SideStorage[7];

	public SidedEnergyContainer() {
		for (int i = 0; i < 7; ++i) {
			sideStorages[i] = new SideStorage(i == 6 ? null : Direction.byId(i));
		}
	}

	/**
	 * Get the currently stored energy amount.
	 */
	public abstract long getAmount();

	/**
	 * Set the currently stored energy amount.
	 */
	public abstract void setAmount(long amount);

	/**
	 * @return The current capacity of this storage.
	 */
	public abstract long getCapacity();

	/**
	 * @return The maximum amount of energy that can be inserted in a single operation from the passed side.
	 */
	public abstract long getMaxInsert(@Nullable Direction side);

	/**
	 * @return The maximum amount of energy that can be extracted in a single operation from the passed side.
	 */
	public abstract long getMaxExtract(@Nullable Direction side);

	/**
	 * @return An {@link EnergyStorage} implementation for the passed side.
	 */
	public EnergyStorage getSideStorage(@Nullable Direction side) {
		return sideStorages[side == null ? 6 : side.getId()];
	}

	@Override
	protected Long createSnapshot() {
		return getAmount();
	}

	@Override
	protected void readSnapshot(Long snapshot) {
		setAmount(snapshot);
	}

	private class SideStorage implements EnergyStorage {
		private final Direction side;

		private SideStorage(Direction side) {
			this.side = side;
		}

		@Override
		public boolean supportsInsertion() {
			return getMaxInsert(side) > 0;
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notNegative(maxAmount);

			long amount = getAmount();
			long inserted = Math.min(getMaxInsert(side), Math.min(maxAmount, getCapacity() - amount));

			if (inserted > 0) {
				updateSnapshots(transaction);
				setAmount(amount + inserted);
				return inserted;
			}

			return 0;
		}

		@Override
		public boolean supportsExtraction() {
			return getMaxExtract(side) > 0;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notNegative(maxAmount);

			long amount = getAmount();
			long extracted = Math.min(getMaxExtract(side), Math.min(maxAmount, amount));

			if (extracted > 0) {
				updateSnapshots(transaction);
				setAmount(amount - extracted);
				return extracted;
			}

			return 0;
		}

		@Override
		public long getAmount() {
			return SidedEnergyContainer.this.getAmount();
		}

		@Override
		public long getCapacity() {
			return SidedEnergyContainer.this.getCapacity();
		}
	}
}