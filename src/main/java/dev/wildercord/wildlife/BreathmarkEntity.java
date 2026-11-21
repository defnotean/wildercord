package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
/** An unticking generated marker. Ordinary placement never acquires provenance. */
public final class BreathmarkEntity extends BlockEntity {
 private boolean authentic;
 public BreathmarkEntity(BlockPos p,BlockState s) {super(FungalGarden.BREATHMARK_ENTITY,p,s);}
 public boolean authentic() {return authentic;}
 public void awaken() {authentic=true;setChanged();}
 @Override protected void loadAdditional(CompoundTag iTag) { McValueInput i = new McValueInput(iTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));super.loadAdditional(iTag);authentic=i.getBooleanOr("belowkeeper_mark",false);}
 @Override protected void saveAdditional(CompoundTag oTag) { McValueOutput o = new McValueOutput(oTag, (level == null ? RegistryAccess.EMPTY : level.registryAccess()));super.saveAdditional(oTag);o.putBoolean("belowkeeper_mark",authentic);}
}
