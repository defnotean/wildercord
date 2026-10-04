package dev.wildercord.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** External saved configuration and source-compatible pre-Rootmolt construction contracts. */
class RootmoltConfigCompatibilityTest {
 @Test void explicitRootmoltOptOutSurvivesSerializationAndUpgrade(){
  String saved="{\"creatures\":{\"wildlife\":true,\"wildlife_spawn_multiplier\":2.75,\"rootmolt_strider\":false,\"sporeback_snail\":true,\"reedback_crab\":false}}";
  var parsed=WildercordConfig.parse(saved);
  assertTrue(parsed.warnings().isEmpty(),parsed.warnings().toString());
  assertFalse(parsed.config().wildlife().spawns("rootmolt_strider"));
  assertTrue(parsed.config().wildlife().spawns("sporeback_snail"));
  var roundTrip=WildercordConfig.parse(parsed.config().toJson());
  assertTrue(roundTrip.warnings().isEmpty(),roundTrip.warnings().toString());
  assertEquals(parsed.config(),roundTrip.config());
  String upgraded=WildercordConfig.addMissing(saved).orElseThrow();
  var loaded=WildercordConfig.parse(upgraded);
  assertTrue(loaded.warnings().isEmpty(),loaded.warnings().toString());
  assertEquals(parsed.config(),loaded.config());
  assertFalse(loaded.config().wildlife().rootmoltStrider());
  assertTrue(WildercordConfig.addMissing(upgraded).isEmpty(),"Upgrade is idempotent and never reinstates an explicit opt-out");
 }
 @Test void olderConfigurationKeepsGlobalAndIndividualOptOuts(){
  String saved="{\"creatures\":{\"wildlife\":false,\"wildlife_spawn_multiplier\":2.75,\"sporeback_snail\":false,\"reedback_crab\":false}}";
  var parsed=WildercordConfig.parse(saved);
  assertTrue(parsed.warnings().isEmpty(),parsed.warnings().toString());
  assertTrue(parsed.config().wildlife().rootmoltStrider(),"Absent new creature setting uses the normal opt-in default");
  assertFalse(parsed.config().wildlife().spawns("rootmolt_strider"),"An older administrator's global disable still wins");
  String upgraded=WildercordConfig.addMissing(saved).orElseThrow();
  var loaded=WildercordConfig.parse(upgraded);
  assertTrue(loaded.warnings().isEmpty(),loaded.warnings().toString());
  assertEquals(parsed.config(),loaded.config());
  assertFalse(loaded.config().wildlife().sporebackSnail());
  assertFalse(loaded.config().wildlife().reedbackCrab());
  assertEquals(2.75,loaded.config().wildlife().spawnMultiplier());
  assertFalse(loaded.config().wildlife().spawns("rootmolt_strider"));
  assertTrue(WildercordConfig.addMissing(upgraded).isEmpty());
 }
 @Test void preRootmoltConstructorRetainsEarlierChoicesAndGlobalAdmission(){
  var old=new WildercordConfig.WildlifeSettings(true,1.5,false,true,false,true,false,true,false,true,false);
  assertTrue(old.rootmoltStrider());
  assertTrue(old.spawns("rootmolt_strider"));
  assertFalse(old.spawns("sporeback_snail"));
  assertFalse(old.spawns("glimmerwing"));
  assertTrue(old.spawns("lumen_stag"));
  assertFalse(old.spawns("lantern_newt"));
  assertTrue(old.spawns("reedback_crab"));
  assertEquals(1.5,old.spawnMultiplier());
  var disabled=new WildercordConfig.WildlifeSettings(false,1.5,true,true,true,true,true,true,true,true,true);
  var zero=new WildercordConfig.WildlifeSettings(true,0,true,true,true,true,true,true,true,true,true);
  assertTrue(disabled.rootmoltStrider() && zero.rootmoltStrider());
  assertFalse(disabled.spawns("rootmolt_strider"));
  assertFalse(zero.spawns("rootmolt_strider"));
 }
}
