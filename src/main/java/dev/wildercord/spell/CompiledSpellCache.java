package dev.wildercord.spell;

import java.util.*;
import java.util.function.Supplier;

/** Bounded cache of compiler work. Every caller receives its own mutable plan graph. */
public final class CompiledSpellCache {
 private CompiledSpellCache() {}
 private record Ranked(RuneDef rune, int rank) {}
 private record Key(List<RuneDef> runes, RuneDef implicit, List<Ranked> ranks) {}
 private static final int LIMIT = 256;
 private static final Map<Key,SpellCompiler.Compiled> CACHE = new LinkedHashMap<>(32,.75F,true);
 private static long hits, misses;
 static synchronized SpellCompiler.Compiled get(List<RuneDef> runes,RuneDef implicit,Ranks.Lookup ranks,Supplier<SpellCompiler.Compiled> build) {
  List<RuneDef> flat=Knots.flatten(runes);
  if(flat.size()>256 || runes.size()>64) return build.get();
  var rankKeys=new ArrayList<Ranked>();
  for(RuneDef rune:flat) if(rune.family()==RuneFamily.EFFECT) rankKeys.add(new Ranked(rune,Ranks.clamp(ranks.rank(rune.id()))));
  Key key=new Key(List.copyOf(runes),implicit,List.copyOf(rankKeys));
  var compiled=CACHE.get(key);
  if(compiled==null) {
   misses++; compiled=build.get();
   // Take an owned graph even on a miss; callers are allowed to modify their returned plan.
   CACHE.put(key,copy(compiled));
   if(CACHE.size()>LIMIT) CACHE.remove(CACHE.keySet().iterator().next());
   return compiled;
  }
  hits++; return copy(compiled);
 }
 public static synchronized void clear() { CACHE.clear(); hits=misses=0; }
 public static synchronized int size() { return CACHE.size(); }
 public static synchronized String report() { return "Compiled plans: "+CACHE.size()+"/"+LIMIT+", cache hits: "+hits+", misses: "+misses; }
 private static SpellCompiler.Compiled copy(SpellCompiler.Compiled c) {
  var copier=new Copier();
  return new SpellCompiler.Compiled(copier.segment(c.root()),c.cost(),c.cooldownTicks(),c.lines(),c.warnings(),c.attachedTo().clone(),c.healthCost());
 }
 private static final class Copier {
  final Map<SpellPlan.Segment,SpellPlan.Segment> segments=new IdentityHashMap<>();
  final Map<SpellPlan.Group,SpellPlan.Group> groups=new IdentityHashMap<>();
  SpellPlan.Group group(SpellPlan.Group original) {
   if(original==null)return null;
   if(groups.containsKey(original))return groups.get(original);
   var out=new SpellPlan.Group(original.shape,original.implicit); groups.put(original,out);
   out.factor=original.factor; out.shapeMods.addAll(original.shapeMods);
   for(var node:original.effects) {
    var effect=new SpellPlan.EffectNode(node.effect); effect.rank=node.rank; effect.factor=node.factor;
    effect.mods.addAll(node.mods); out.effects.add(effect);
   }
   return out;
  }
  SpellPlan.Segment segment(SpellPlan.Segment original) {
   if(original==null)return null;
   if(segments.containsKey(original))return segments.get(original);
   var out=new SpellPlan.Segment(original.implicitShape); segments.put(original,out);
   for(var group:original.groups)out.groups.add(group(group));
   if(original.link!=null) {
    var old=original.link; var link=new SpellPlan.Link(old.link,group(old.anchor)); out.link=link;
    link.factor=old.factor; link.firstOnly=old.firstOnly; link.mods.addAll(old.mods);
    link.next=segment(old.next); link.echoPrefix=segment(old.echoPrefix);
   }
   return out;
  }
 }
}
