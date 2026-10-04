package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
/** Provenance belongs to the actual structure. No ticker, item form or survival cloning. */
public final class DrainhouseMarkEntity extends BlockEntity {
 private boolean authentic;
 public DrainhouseMarkEntity(BlockPos p,BlockState s){super(DrainhouseContent.MARK_ENTITY,p,s);}
 public boolean authentic(){return authentic;}
 void awaken(){authentic=true;setChanged();}
 @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);authentic=in.getBooleanOr("drainhouse_authentic",false);}
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putBoolean("drainhouse_authentic",authentic);}
}
