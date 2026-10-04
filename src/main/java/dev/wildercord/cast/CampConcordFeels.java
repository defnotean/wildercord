package dev.wildercord.cast;
import dev.wildercord.cast.feel.*;
/** Cue only: only actual owner admissions may voice the later successful utility. */
public final class CampConcordFeels {
 private CampConcordFeels(){}
 public static void init(){Signature.of("watchweft").sound(Phase.CUE,"camp_watch_cue",.3F,1).replace(Phase.CUE).authoredOutcome().register();
  Signature.of("manabraid").sound(Phase.CUE,"camp_braid_cue",.3F,1).replace(Phase.CUE).authoredOutcome().register();}
}
