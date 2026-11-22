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
/** An unticking generated marker. Ordinary placement never acquires provenance. */
public final class BreathmarkEntity extends BlockEntity {
 private boolean authentic;
 public BreathmarkEntity(BlockPos p,BlockState s) {super(FungalGarden.BREATHMARK_ENTITY,p,s);}
 public boolean authentic() {return authentic;}
 public void awaken() {authentic=true;setChanged();}
 @Override protected void loadAdditional(CompoundTag iTag, HolderLookup.Provider registries) { McValueInput i = new McValueInput(iTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));super.loadAdditional(iTag, registries);authentic=(i.contains("belowkeeper_mark", 1) ? i.getBoolean("belowkeeper_mark") : false);}
 @Override protected void saveAdditional(CompoundTag oTag, HolderLookup.Provider registries) { McValueOutput o = new McValueOutput(oTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));super.saveAdditional(oTag, registries);o.putBoolean("belowkeeper_mark",authentic);}
}
