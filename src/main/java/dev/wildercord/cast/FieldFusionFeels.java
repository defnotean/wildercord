package dev.wildercord.cast;
import dev.wildercord.cast.feel.Signature;
import dev.wildercord.cast.feel.Phase;
/** Six independently synthesized cues; successful gameplay supplies its own material impact. */
public final class FieldFusionFeels {
 private FieldFusionFeels(){}
 public static final java.util.List<String> EFFECTS=java.util.List.of("springbed","cinder_sieve","ashen_mercy","clockroot","skylatch","thresherwind");
 public static void register(){
  for(String id:EFFECTS)
   Signature.of(id).sound(Phase.CUE,"fieldfusion_"+id+"_cue",.38F,1).replace(Phase.IMPACT,Phase.HIT).register();
 }
}
