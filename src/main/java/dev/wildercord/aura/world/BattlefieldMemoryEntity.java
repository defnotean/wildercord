package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;

/** Generated provenance persists. An ordinary placed marker cannot mint discoveries. */
public final class BattlefieldMemoryEntity extends BlockEntity {
	private boolean oldGround;
	public BattlefieldMemoryEntity(BlockPos pos,BlockState state) { super(Battlefields.MEMORY_ENTITY,pos,state); }
	public boolean oldGround() { return oldGround; }
	public void awaken() { oldGround=true; setChanged(); }
	@Override protected void loadAdditional(CompoundTag inTag) { McValueInput in = new McValueInput(inTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess())); super.loadAdditional(inTag); oldGround=in.getBooleanOr("old_ground",false); }
	@Override protected void saveAdditional(CompoundTag outTag) { McValueOutput out = new McValueOutput(outTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess())); super.saveAdditional(outTag); out.putBoolean("old_ground",oldGround); }
}
