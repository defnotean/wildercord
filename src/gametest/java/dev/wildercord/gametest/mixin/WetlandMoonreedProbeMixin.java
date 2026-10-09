package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.wildlife.MoonreedFeature;
import dev.wildercord.wildlife.WetlandGenerationProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=MoonreedFeature.class,remap=false)
public abstract class WetlandMoonreedProbeMixin {
 @WrapMethod(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",require=1,expect=1,allow=1)
 private boolean wildercord$feature(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin,Operation<Boolean> original) {
  var invocation=WetlandGenerationProbe.beginFeature(level,origin);Boolean result=null;
  try {result=original.call(level,generator,random,origin);return result;}
  finally {WetlandGenerationProbe.endFeature(invocation,result);}
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",at=@At(value="INVOKE",target="Ldev/wildercord/wildlife/MoonreedBlock;surfaceHeight(Lnet/minecraft/world/level/LevelReader;II)I"),require=1,expect=1,allow=1)
 private int wildercord$height(LevelReader level,int x,int z,Operation<Integer> original) {
  int height=original.call(level,x,z);WetlandGenerationProbe.candidate(x,height,z);return height;
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;isEmptyBlock(Lnet/minecraft/core/BlockPos;)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$empty(WorldGenLevel level,BlockPos at,Operation<Boolean> original) {
  boolean result=original.call(level,at);WetlandGenerationProbe.gate("empty",result);return result;
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;canSurvive(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$supported(BlockState state,LevelReader level,BlockPos at,Operation<Boolean> original) {
  boolean result=original.call(state,level,at);WetlandGenerationProbe.gate("survive",result);return result;
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",at=@At(value="INVOKE",target="Ldev/wildercord/wildlife/MoonreedBlock;moist(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$moist(LevelReader level,BlockPos at,Operation<Boolean> original) {
  boolean result=original.call(level,at);WetlandGenerationProbe.gate("moist",result);return result;
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",at=@At(value="INVOKE",target="Ldev/wildercord/wildlife/MoonreedBlock;openSky(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$sky(LevelReader level,BlockPos at,Operation<Boolean> original) {
  boolean result=original.call(level,at);WetlandGenerationProbe.gate("openSky",result);return result;
 }
 @WrapOperation(method="place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$write(WorldGenLevel level,BlockPos at,BlockState state,int flags,Operation<Boolean> original) {
  boolean result=original.call(level,at,state,flags);WetlandGenerationProbe.write(result);return result;
 }
}
