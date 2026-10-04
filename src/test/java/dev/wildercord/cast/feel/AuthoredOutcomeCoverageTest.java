package dev.wildercord.cast.feel;
import dev.wildercord.spell.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Whole-group outcome ownership retains fallback for uncovered supporting effects. */
class AuthoredOutcomeCoverageTest {
 private Feel group(String lead,String... ids){return new Feel(Motion.HURL,"life","",Role.STRIKE,Band.M,1,Map.of(),"wildercord:bolt",lead,List.of(ids));}
 @Test void legacyReplaceDoesNotOptIntoCollisionSuppression(){
  var signature=Signature.of("coverage_legacy").replace(Phase.IMPACT,Phase.HIT).register();
  assertTrue(signature.replaces(Phase.IMPACT));assertFalse(signature.ownsOutcomeBody());
  assertFalse(Signatures.authoredOutcomes(group("wildercord:coverage_legacy","wildercord:coverage_legacy")));
 }
 @Test void completeCoveredGroupOptsInButSameElementAndForeignUncoveredRemain(){
  Signature.of("coverage_owned_a").authoredOutcome().register();Signature.of("coverage_owned_b").authoredOutcome().register();
  Signature.of("coverage_uncovered_life").register();
  assertTrue(Signatures.authoredOutcomes(group("wildercord:coverage_owned_a","wildercord:coverage_owned_a","wildercord:coverage_owned_b")));
  assertFalse(Signatures.authoredOutcomes(group("wildercord:coverage_owned_a","wildercord:coverage_owned_a","wildercord:coverage_uncovered_life")));
  assertFalse(Signatures.authoredOutcomes(group("wildercord:coverage_owned_a","wildercord:coverage_owned_a","foreign:heal")));
 }
 @Test void missingCompleteMetadataRetainsFallback(){
  Signature.of("coverage_plain").authoredOutcome().register();
  var old=new Feel(Motion.HURL,"life","",Role.STRIKE,Band.M,1,Map.of(),"wildercord:bolt","wildercord:coverage_plain");
  assertFalse(Signatures.authoredOutcomes(old));assertFalse(Signatures.authoredOutcomes(Feel.plain("wildercord:bolt","life")));assertFalse(Signatures.authoredOutcomes(null));
 }
 @Test void compilerMetadataIncludesEveryEffectInOrder(){
  var plan=SpellCompiler.compile(List.of(Runes.BOLT,Runes.HEAL,Runes.HARM));
  var feel=Feel.of(plan.root().groups.getFirst(),12,0);
  assertEquals(List.of(Runes.HEAL.id(),Runes.HARM.id()),feel.effectIds());
 }
 @Test void adjustingScalePreservesSupportingCoverageMetadata(){
  Signature.of("coverage_adjust").scale(1.2).motion(Motion.CALL).authoredOutcome().register();
  var source=group("wildercord:coverage_adjust","wildercord:coverage_adjust","foreign:uncovered");var adjusted=Signatures.adjust(source);
  assertEquals(source.effectIds(),adjusted.effectIds());assertEquals(Motion.CALL,adjusted.motion());assertFalse(Signatures.authoredOutcomes(adjusted));
 }
 @Test void metadataIsImmutable(){
  var ids=new ArrayList<>(List.of("wildercord:coverage_immutable"));
  var feel=new Feel(Motion.HURL,"life","",Role.STRIKE,Band.M,1,Map.of(),"wildercord:bolt",ids.getFirst(),ids);ids.clear();
  assertEquals(1,feel.effectIds().size());assertThrows(UnsupportedOperationException.class,()->feel.effectIds().clear());
 }
 @Test void outcomeOptInPreservesCueAndTravelContracts(){
  Hook hook=ctx->{};var signature=Signature.of("coverage_hooks").sound(Phase.CUE,"test_cue",.4F,1).sound(Phase.TRAVEL,"test_travel",.2F,1).hook(Phase.TRAVEL,hook).authoredOutcome();
  assertEquals("test_cue",signature.soundOf(Phase.CUE).name());assertEquals("test_travel",signature.soundOf(Phase.TRAVEL).name());assertSame(hook,signature.hookOf(Phase.TRAVEL));
  assertFalse(signature.replaces(Phase.CUE));assertFalse(signature.replaces(Phase.TRAVEL));assertFalse(signature.replaces(Phase.IMPACT));
 }
}