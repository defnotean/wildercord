package dev.wildercord.cast;
import dev.wildercord.cast.feel.Signature;
import dev.wildercord.cast.feel.Phase;
/** Each preparation cue is separately synthesized; successful gameplay calls the separate impact voice. */
public final class NextSignatureFeels {
 private NextSignatureFeels(){}
 public static final java.util.List<String> EFFECTS=java.util.List.of("nullcatch","second_bell","red_ledger","quietus","blood_escrow","frost_molt",
   "pulse_ferry","last_lantern","pocket_current","wayline","night_seam","shard_compass");
 public static void register(){
  for(String id:EFFECTS){
   var signature=Signature.of(id).sound(Phase.CUE,"nextsignature_"+id+"_cue",.35F,1).replace(Phase.IMPACT,Phase.HIT);
   if(id.equals("pulse_ferry"))signature.authoredOutcome();
   signature.register();
  }
 }
}
