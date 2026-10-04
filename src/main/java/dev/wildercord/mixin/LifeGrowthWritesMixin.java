package dev.wildercord.mixin;
import dev.wildercord.cast.LifeGrowthWrites;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
/** Observes actual scoped growth writes without changing vanilla setBlock results. */
@Mixin(Level.class)
public abstract class LifeGrowthWritesMixin {
 @WrapMethod(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
 private boolean wildercord$observedGrowth(BlockPos pos,BlockState state,int flags,int recursion,Operation<Boolean> original){
  var level=(Level)(Object)this;Object token=LifeGrowthWrites.begin(level,pos);
  boolean changed=original.call(pos,state,flags,recursion);
  LifeGrowthWrites.end(level,token,changed);return changed;
 }
}
