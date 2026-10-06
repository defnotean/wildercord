package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Protect the adapter's no-load boundary; boolean hasChunk/false-create queries can still join incomplete futures. */
final class MoonreedResidencyContractTest {
 @Test void compiledAdapterUsesCompletedChunkSnapshotsWithoutLoadingWorldQueries() throws IOException {
  String block="dev/wildercord/wildlife/MoonreedBlock";
  Set<String> adapters=new HashSet<>();
  for(var method:read(block).methods())if(method.methodName().equalsString("findBud"))
   method.code().ifPresent(code->code.forEach(element->{
    if(element instanceof NewObjectInstruction allocation&&allocation.className().asInternalName().startsWith(block+"$"))
     adapters.add(allocation.className().asInternalName());
   }));
  assertFalse(adapters.isEmpty(),"Inspect the actual production adapter rather than an independent fake");
  Set<String> calls=new HashSet<>();
  for(String adapter:adapters)for(var method:read(adapter).methods())method.code().ifPresent(code->code.forEach(element->{
   if(!(element instanceof InvokeInstruction call))return;
   String owner=call.owner().asInternalName(),name=call.name().stringValue();calls.add(owner+"."+name);
   if(owner.equals("net/minecraft/server/level/ServerChunkCache"))
    assertEquals("getChunkNow",name,"Only completed nonloading chunk access is permitted: "+name);
   if(owner.equals("net/minecraft/server/level/ServerLevel")||owner.equals("net/minecraft/world/level/Level")||owner.equals("net/minecraft/world/level/LevelReader"))
    assertFalse(Set.of("hasChunkAt","hasChunk","getChunk","getHeight","getBlockState","getFluidState","getBiome","getNoiseBiome","isRainingAt","precipitationAt").contains(name),
     "Ticket checks or implicit loading world reads are forbidden in the acquisition adapter: "+name);
  }));
  assertTrue(calls.contains("net/minecraft/server/level/ServerChunkCache.getChunkNow"));
  assertTrue(calls.contains("net/minecraft/world/level/biome/BiomeManager.withDifferentSource"),"Rain keeps native zoom selection with a nonloading biome resolver");
 }
 private static ClassModel read(String type) throws IOException {
  try(var input=MoonreedResidencyContractTest.class.getClassLoader().getResourceAsStream(type+".class")){
   assertNotNull(input,"Compiled production class "+type);return ClassFile.of().parse(input.readAllBytes());
  }
 }
}
