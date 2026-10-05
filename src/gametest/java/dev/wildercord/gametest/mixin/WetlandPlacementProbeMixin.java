package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.wildlife.MoonreedFeature;
import dev.wildercord.wildlife.WetlandGenerationProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.Consumer;

@Mixin(value=FeaturePlacer.class,remap=false)
public abstract class WetlandPlacementProbeMixin {
 @Shadow @Final private WorldGenLevel level;
 @Shadow @Final private ChunkGenerator generator;
 @WrapMethod(method="place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",require=1,expect=1,allow=1)
 private boolean wildercord$placement(PlacedFeature feature,RandomSource random,BlockPos origin,boolean biomeCheck,Operation<Boolean> original) {
  var source=WetlandGenerationProbe.beginPlacement(level,generator,feature,biomeCheck);Boolean result=null;
  try {result=original.call(feature,random,origin,biomeCheck);return result;}
  finally {WetlandGenerationProbe.endPlacement(source,result);}
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
   at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/placement/PlacementModifier;modify(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/Consumer;)V"),require=1,expect=1,allow=1)
 private void wildercord$modifier(PlacementModifier modifier,PlacementContext context,RandomSource random,BlockPos origin,Consumer<BlockPos> output,Operation<Void> original) {
  var source=WetlandGenerationProbe.modifierSource();
  if(source==null || context.topFeature().isEmpty() || !(context.topFeature().get().feature().value() instanceof MoonreedFeature)) {
   original.call(modifier,context,random,origin,output);return;
  }
  WetlandGenerationProbe.modifier(source,modifier.getClass().getSimpleName(),origin,
    forwarded -> original.call(modifier,context,random,origin,forwarded),output);
 }
}
