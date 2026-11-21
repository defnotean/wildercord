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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;
import java.util.stream.Stream;

@Mixin(PlacedFeature.class)
public abstract class WetlandPlacementProbeMixin {
 @WrapMethod(method="placeWithBiomeCheck(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",require=1,expect=1,allow=1)
 private boolean wildercord$placement(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin,Operation<Boolean> original) {
  var source=WetlandGenerationProbe.beginPlacement(level,generator,(PlacedFeature)(Object)this,true);Boolean result=null;
  try {result=original.call(level,generator,random,origin);return result;}
  finally {WetlandGenerationProbe.endPlacement(source,result);}
 }
 @WrapOperation(method="method_39649(Lnet/minecraft/world/level/levelgen/placement/PlacementModifier;Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Ljava/util/stream/Stream;",
   at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/placement/PlacementModifier;getPositions(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Ljava/util/stream/Stream;"),require=1,expect=1,allow=1)
 private static Stream<BlockPos> wildercord$modifier(PlacementModifier modifier,PlacementContext context,RandomSource random,BlockPos origin,Operation<Stream<BlockPos>> original) {
  var source=WetlandGenerationProbe.modifierSource();
  if(source==null || context.topFeature().isEmpty() || !(context.topFeature().get().feature().value().feature() instanceof MoonreedFeature)) {
   return original.call(modifier,context,random,origin);
  }
  var out=new ArrayList<BlockPos>();
  WetlandGenerationProbe.modifier(source,modifier.getClass().getSimpleName(),origin,
    forwarded -> original.call(modifier,context,random,origin).forEach(forwarded),out::add);
  return out.stream();
 }
}
