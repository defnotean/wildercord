package dev.wildercord.spell;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompiledSpellCacheTest {
 @Test void callerMutationsNeverPoisonLaterCasts() {
  CompiledSpellCache.clear();
  var runes=List.of(Runes.BOLT,Runes.FIRE,Runes.SPLIT_MOD,Runes.ON_HIT,Runes.BURST,Runes.HARM);
  var first=SpellCompiler.compile(runes); var untouched=SpellCompiler.compile(runes);
  first.root().groups.clear(); first.root().link.mods.add(Runes.RAPID_MOD); first.attachedTo()[2]=999;
  var next=SpellCompiler.compile(runes);
  assertEquals(untouched.lines(),next.lines()); assertEquals(untouched.cost(),next.cost());
  assertEquals(1,next.root().groups.size()); assertTrue(next.root().link.mods.isEmpty());
  assertNotEquals(999,next.attachedTo()[2]);
  assertSame(next.root().groups.getFirst(),next.root().link.anchor);
 }
 @Test void rankChangesSelectDifferentPlans() {
  var runes=List.of(Runes.BOLT,Runes.FIRE);
  assertEquals(1,SpellCompiler.compile(runes,id->1).root().groups.getFirst().effects.getFirst().rank);
  assertEquals(3,SpellCompiler.compile(runes,id->3).root().groups.getFirst().effects.getFirst().rank);
  assertEquals(1,SpellCompiler.compile(runes,id->1).root().groups.getFirst().effects.getFirst().rank);
 }
 @Test void emptyAndEchoPlansKeepTheirStructure() {
  var runes=List.of(Runes.BOLT,Runes.FIRE,Runes.ECHO,Runes.BURST,Runes.HARM);
  var first=SpellCompiler.compile(runes); var second=SpellCompiler.compile(runes);
  assertEquals(first.lines(),second.lines()); assertNotSame(first.root(),second.root());
  assertTrue(SpellCompiler.compile(List.of()).isEmpty());
 }
 @Test void cacheIsBoundedAndExplicitlyInvalidated() {
  CompiledSpellCache.clear();
  for(RuneDef shape:Runes.all()) if(shape.family()==RuneFamily.SHAPE)
   for(RuneDef effect:Runes.all()) if(effect.family()==RuneFamily.EFFECT) SpellCompiler.compile(List.of(shape,effect));
  assertEquals(256,CompiledSpellCache.size());
  CompiledSpellCache.clear(); assertEquals(0,CompiledSpellCache.size());
 }
}
