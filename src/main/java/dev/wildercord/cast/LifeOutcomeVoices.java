package dev.wildercord.cast;
/** Explicit single sound owner. Ashen Mercy's synthesized Life alternative stays unregistered. */
final class LifeOutcomeVoices {
 static String outcome(String rune){
  if(!LifeOutcomeRoster.RUNES.contains(rune))throw new IllegalArgumentException(rune);
  return rune.equals("ashen_mercy")||rune.equals("pulse_ferry")?null:"life_auth_"+rune+"_outcome";
 }
 static String cue(String rune){
  if(!LifeOutcomeRoster.RUNES.contains(rune))throw new IllegalArgumentException(rune);
  return rune.equals("ashen_mercy")?"fieldfusion_ashen_mercy_cue":rune.equals("pulse_ferry")?"nextsignature_pulse_ferry_cue":"life_auth_"+rune+"_cue";
 }
}
