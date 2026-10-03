package dev.wildercord.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Raw spell damage admitted to defences, shared by every continuation of one payment. */
public final class SpellDamageAllowance {
 public static final double MAX_HIT=96;
 public static final double MAX_TOTAL=512;
 public static final int MAX_TARGETS=256;
 private final Map<UUID,Double> used=new HashMap<>();
 private double mana;
 public SpellDamageAllowance(double mana){this.mana=finiteMana(mana);}
 public static double total(double mana){return Math.min(MAX_TOTAL,12+2*finiteMana(mana));}
 private static double finiteMana(double mana){return Double.isFinite(mana)?Math.max(0,mana):0;}
 /** Price may be set only before damage begins; lowering/resetting it cannot replenish a cast. */
 public void price(double mana){if(used.isEmpty())this.mana=finiteMana(mana);}
 public boolean available(UUID target){return target!=null && used.getOrDefault(target,0.0)<total(mana) && (used.containsKey(target)||used.size()<MAX_TARGETS);}
 public float take(UUID target,double requested){
  if(target==null || !Double.isFinite(requested) || requested<=0)return 0;
  if(!used.containsKey(target)&&used.size()>=MAX_TARGETS)return 0;
  double spent=used.getOrDefault(target,0.0),remaining=Math.max(0,total(mana)-spent);
  // Round toward zero so float conversion cannot overspend a double allowance.
  float granted=(float)Math.min(Math.min(MAX_HIT,requested),remaining);
  if(granted>remaining)granted=Math.nextDown(granted);
  if(granted>0)used.put(target,spent+granted);
  return granted;
 }
}
