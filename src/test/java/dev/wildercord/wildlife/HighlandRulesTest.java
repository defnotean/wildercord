package dev.wildercord.wildlife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class HighlandRulesTest {
	@Test void hunterAndGrazerKeepDistinctRhythms() {
		assertTrue(HighlandRules.wantsCover(true,6000,false));assertFalse(HighlandRules.wantsCover(false,6000,false));
		assertFalse(HighlandRules.wantsCover(true,16000,false));assertTrue(HighlandRules.wantsCover(false,16000,false));
		assertFalse(HighlandRules.wantsCover(true,10000,false));assertFalse(HighlandRules.wantsCover(false,23000,false));
		assertTrue(HighlandRules.wantsCover(true,30000,false));assertTrue(HighlandRules.wantsCover(false,-8000,false));
	}
	@Test void rainOverridesRhythm() {for(boolean predator:new boolean[]{true,false})for(long day:new long[]{0,6000,12000,18000})assertTrue(HighlandRules.wantsCover(predator,day,true));}
	@Test void finiteMealDeadlineDoesNotProducePermanentOrdinarySatiety() {assertFalse(HighlandRules.hungry(1299,HighlandRules.meal(100)));assertTrue(HighlandRules.hungry(1300,HighlandRules.meal(100)));assertEquals(Long.MAX_VALUE,HighlandRules.meal(Long.MAX_VALUE-1));}
}
