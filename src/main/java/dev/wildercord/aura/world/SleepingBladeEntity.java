package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.UUID;

/** A site yields one blade in its lifetime. An ordinary placed stone has no provenance. */
public final class SleepingBladeEntity extends BlockEntity {
	private boolean authentic;
	private UUID claimedBy;
	public SleepingBladeEntity(BlockPos p, BlockState s) { super(SleepingBlades.STONE_ENTITY, p, s); }
	public boolean authentic() { return authentic; }
	public UUID claimedBy() { return claimedBy; }
	public void awaken() { authentic = true; setChanged(); }
	public void claim(UUID owner) { claimedBy = owner; phase(4); setChanged(); }
	public void phase(int phase) {
		if (level != null && getBlockState().getValue(SleepingBladeStone.PHASE) != phase)
			level.setBlock(worldPosition, getBlockState().setValue(SleepingBladeStone.PHASE, phase), 3);
	}
	@Override protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out); out.putBoolean("authentic", authentic);
		if (claimedBy != null) out.store("claimed_by", UUIDUtil.CODEC, claimedBy);
	}
	@Override protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in); authentic = in.getBooleanOr("authentic", false);
		claimedBy = in.read("claimed_by", UUIDUtil.CODEC).orElse(null);
	}
}
