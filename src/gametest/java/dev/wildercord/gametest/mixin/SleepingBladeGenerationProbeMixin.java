package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.world.dungeons.SleepingBladeGenerationProbe;
import dev.wildercord.world.dungeons.SleepingBladePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Optional;

/** Every original operation executes once with its original arguments, RNG and return value. */
@Mixin(value=SleepingBladePiece.class,remap=false)
public abstract class SleepingBladeGenerationProbeMixin {
 @WrapMethod(method="locate(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;)Ljava/util/Optional;",require=1,expect=1,allow=1)
 private static Optional<Structure.GenerationStub> wildercord$locate(Structure.GenerationContext context,Operation<Optional<Structure.GenerationStub>> original) {
  var scope=SleepingBladeGenerationProbe.locate(context);Boolean admitted=null;
  try {var result=original.call(context);admitted=result.isPresent();return result;}
  finally {scope.finish(admitted);}
 }
 @WrapOperation(method="locate",at=@At(value="INVOKE",target="Ldev/wildercord/config/WildercordConfig$AuraWorldSettings;sleepingBlades()Z"),require=1,expect=1,allow=1)
 private static boolean wildercord$config(WildercordConfig.AuraWorldSettings settings,Operation<Boolean> original) {
  boolean result=original.call(settings);SleepingBladeGenerationProbe.configured(result);return result;
 }
 @WrapOperation(method="locate",at=@At(value="INVOKE",target="Lnet/minecraft/core/Direction$Plane;getRandomDirection(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/core/Direction;"),require=1,expect=1,allow=1)
 private static Direction wildercord$direction(Direction.Plane plane,RandomSource random,Operation<Direction> original) {
  var result=original.call(plane,random);SleepingBladeGenerationProbe.direction(result);return result;
 }
 @WrapMethod(method="height(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/Heightmap$Types;)I",require=1,expect=1,allow=1)
 private static int wildercord$height(Structure.GenerationContext context,BlockPos at,Heightmap.Types map,Operation<Integer> original) {
  int result=original.call(context,at,map);SleepingBladeGenerationProbe.height(at,map,result);return result;
 }
 @WrapOperation(method="locate",at=@At(value="INVOKE",target="Ldev/wildercord/aura/world/SleepingBladeRules;footing(III[I)Z"),require=1,expect=1,allow=1)
 private static boolean wildercord$footing(int surface,int floor,int sea,int[] neighbours,Operation<Boolean> original) {
  boolean result=original.call(surface,floor,sea,neighbours);SleepingBladeGenerationProbe.footing(surface,floor,neighbours,sea,result);return result;
 }
}
