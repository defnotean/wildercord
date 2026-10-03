package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Generated provenance persists. An ordinary placed marker cannot mint discoveries. */
public final class BattlefieldMemoryEntity extends BlockEntity {
	private boolean oldGround;
	public BattlefieldMemoryEntity(BlockPos pos,BlockState state) { super(Battlefields.MEMORY_ENTITY,pos,state); }
	public boolean oldGround() { return oldGround; }
	public void awaken() { oldGround=true; setChanged(); }
	@Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); oldGround=in.getBooleanOr("old_ground",false); }
	@Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putBoolean("old_ground",oldGround); }
}
