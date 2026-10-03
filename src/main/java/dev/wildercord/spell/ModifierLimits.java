package dev.wildercord.spell;

/** Tradeoffs with finite cooldown/radius or cheap conditional power cannot stack exponentially. */
public final class ModifierLimits {
 private ModifierLimits() {}
 public static int maximum(RuneDef rune) {
  if(rune.is(Runes.VOW_MOD.id()) || rune.is(Runes.EXECUTE_MOD.id()) || rune.is(Runes.TRIAL_KEY.id()))return 1;
  if(rune.is(Runes.FOCUS_MOD.id()))return 2;
  if(rune.is(Runes.EXTEND.id()))return 3;
  return Integer.MAX_VALUE;
 }
 public static int count(java.util.List<RuneDef> modifiers,RuneDef rune) {
  return Math.min(maximum(rune),SpellPlan.count(modifiers,rune));
 }
}
