package dev.wildercord.wildlife;
import org.junit.jupiter.api.Test;
import dev.wildercord.config.WildercordConfig;
import static org.junit.jupiter.api.Assertions.*;
final class WetlandRulesTest {
	@Test void finiteGatheringAndPopulationBoundaries() {assertTrue(WetlandRules.room(2));assertFalse(WetlandRules.room(3));assertFalse(WetlandRules.ready(2399,2400));assertTrue(WetlandRules.ready(2400,2400));}
	@Test void wetNightAndMagicHaveSeparateReadableStrengths() {assertFalse(WetlandRules.night(12999));assertTrue(WetlandRules.night(13000));assertFalse(WetlandRules.night(23000));assertTrue(WetlandRules.night(37000));assertTrue(WetlandRules.glow(true,true,false)>WetlandRules.glow(false,true,false));assertTrue(WetlandRules.glow(false,false,true)>WetlandRules.glow(true,true,false));}
	@Test void stalkingAnchorStaysWithinItsOneCellBankRing() {assertFalse(WetlandRules.staleAnchor(2,4,2,4));assertFalse(WetlandRules.staleAnchor(2,4,3,5));assertFalse(WetlandRules.staleAnchor(2,4,1,3));assertTrue(WetlandRules.staleAnchor(2,4,4,4));assertTrue(WetlandRules.staleAnchor(2,4,2,2));}
	@Test void newSwitchRoundTripsAndOldConfigDefaultsSafely() {
		var old=WildercordConfig.parse("{}");assertTrue(old.config().wildlife().spawns("lantern_newt"));
		var parsed=WildercordConfig.parse("{\"creatures\":{\"lantern_newt\":false}}");assertTrue(parsed.warnings().isEmpty());assertFalse(parsed.config().wildlife().spawns("lantern_newt"));
		assertEquals(parsed.config(),WildercordConfig.parse(parsed.config().toJson()).config());
		assertFalse(WildercordConfig.parse("{\"creatures\":{\"wildlife\":false}}").config().wildlife().spawns("lantern_newt"));
	}
}
