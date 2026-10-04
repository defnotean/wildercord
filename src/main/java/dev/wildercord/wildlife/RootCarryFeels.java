package dev.wildercord.wildlife;
import dev.wildercord.cast.feel.*;
/** Actual owner wire owns selection/relocation body; collision does not imply a transplanted root. */
public final class RootCarryFeels {
 private RootCarryFeels(){}
 public static void init(){Signature.of("root_carry").sound(Phase.CUE,"root_carry_cue",.3F,1).authoredOutcome().register();}
}
