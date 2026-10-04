package dev.wildercord.cast.feel;

/** Original Life cue voices and explicit ownership of observed outcome bodies. */
final class LifeOutcomeSignatures {
 private LifeOutcomeSignatures() {}
 private static void install(String rune,int accent) {
  Signature.of(rune).accent(accent).sound(Phase.CUE,"life_auth_"+rune+"_cue",.6F,1F)
   .replace(Phase.HIT).authoredOutcome().register();
 }
 static void register() {
  install("heal",0xD9E6A0);install("grow",0xA8B872);install("regrowth",0xB1BF89);
  install("cleanse",0xDDD7B1);install("venom",0xA4B35C);install("nourish",0xD7B565);
  install("harvest",0xE0C27F);install("reversal",0xE6C581);install("restore",0xD2BE8A);
  install("bramble",0xA68C54);install("haven",0xB8C685);install("glimmer",0xC8D9B2);
  install("fortune",0xD4B65B);install("bloom",0xE0C5AE);install("soulbond",0xD3BACA);
  install("second_wind",0xDBD69C);install("lifebloom",0xD6C29E);install("root_bulwark",0xA79474);
  install("bloomstep",0xD8B5A6);install("stitchtime",0xD4C99A);install("vinelash",0xB5A078);
  install("remedy",0xCBD493);install("ancient_seed",0xA18A5B);install("moonpetal",0xBFC5D6);
  install("sporebloom",0xC8C095);install("glowvine",0xCFA764);install("rootsnare",0xB2986C);
  install("drowse",0xD0CBB8);
  // Ashen Mercy keeps FieldFusionFeels' original cue and FieldFusionFx's actual landing voice.
  // Life owns its observed material recipe, but must not install a second sound signature.
 }
}
