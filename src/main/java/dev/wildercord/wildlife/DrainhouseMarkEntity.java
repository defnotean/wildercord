package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
/** Provenance belongs to the actual structure. No ticker, item form or survival cloning. */
public final class DrainhouseMarkEntity extends BlockEntity {
 private boolean authentic;
 public DrainhouseMarkEntity(BlockPos p,BlockState s){super(DrainhouseContent.MARK_ENTITY,p,s);}
 public boolean authentic(){return authentic;}
 void awaken(){authentic=true;setChanged();}
 @Override protected void loadAdditional(CompoundTag inTag, HolderLookup.Provider registries){ McValueInput in = new McValueInput(inTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));super.loadAdditional(inTag, registries);authentic=(in.contains("drainhouse_authentic", 1) ? in.getBoolean("drainhouse_authentic") : false);}
 @Override protected void saveAdditional(CompoundTag outTag, HolderLookup.Provider registries){ McValueOutput out = new McValueOutput(outTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));super.saveAdditional(outTag, registries);out.putBoolean("drainhouse_authentic",authentic);}
}
