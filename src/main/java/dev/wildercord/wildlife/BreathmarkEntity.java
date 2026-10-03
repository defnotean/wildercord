package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
/** An unticking generated marker. Ordinary placement never acquires provenance. */
public final class BreathmarkEntity extends BlockEntity {
 private boolean authentic;
 public BreathmarkEntity(BlockPos p,BlockState s) {super(FungalGarden.BREATHMARK_ENTITY,p,s);}
 public boolean authentic() {return authentic;}
 public void awaken() {authentic=true;setChanged();}
 @Override protected void loadAdditional(ValueInput i) {super.loadAdditional(i);authentic=i.getBooleanOr("belowkeeper_mark",false);}
 @Override protected void saveAdditional(ValueOutput o) {super.saveAdditional(o);o.putBoolean("belowkeeper_mark",authentic);}
}
