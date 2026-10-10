package dev.wildercord.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Raw spell damage admitted to defences, shared by every continuation of one payment. */
public final class SpellDamageAllowance {
 public static final double MAX_HIT=40;
 public static final double MAX_TOTAL=256;
 public static final int MAX_TARGETS=256;
 /** Damage per point of mana paid, and the flat share every cast gets however cheap. */
 public static final double PER_MANA=1.2;
 public static final double FLAT=4;
 /** Targets past this many share out: the k-th takes FULL_TARGETS/k of the full allowance. */
 public static final int FULL_TARGETS=3;
 private final Map<UUID,Double> used=new HashMap<>();
 private final Map<UUID,Double> limit=new HashMap<>();
 private double mana;
 public SpellDamageAllowance(double mana){this.mana=finiteMana(mana);}
 public static double total(double mana){return Math.min(MAX_TOTAL,FLAT+PER_MANA*finiteMana(mana));}
 /** The allowance of the {@code k}-th creature (1-based) this payment reaches. */
 public static double total(double mana,int k){return total(mana)*Math.min(1.0,(double)FULL_TARGETS/Math.max(1,k));}
 private static double finiteMana(double mana){return Double.isFinite(mana)?Math.max(0,mana):0;}
 /** Price may be set only before damage begins; lowering/resetting it cannot replenish a cast. */
 public void price(double mana){if(used.isEmpty())this.mana=finiteMana(mana);}
 private double cap(UUID target){Double l=limit.get(target);return l!=null?l:total(mana,used.size()+1);}
 public boolean available(UUID target){return target!=null && used.getOrDefault(target,0.0)<cap(target) && (used.containsKey(target)||used.size()<MAX_TARGETS);}
 public float take(UUID target,double requested){
  if(target==null || !Double.isFinite(requested) || requested<=0)return 0;
  if(!used.containsKey(target)&&used.size()>=MAX_TARGETS)return 0;
  double cap=cap(target),spent=used.getOrDefault(target,0.0),remaining=Math.max(0,cap-spent);
  // Round toward zero so float conversion cannot overspend a double allowance.
  float granted=(float)Math.min(Math.min(MAX_HIT,requested),remaining);
  if(granted>remaining)granted=Math.nextDown(granted);
  if(granted>0){used.put(target,spent+granted);limit.putIfAbsent(target,cap);}
  return granted;
 }
}
