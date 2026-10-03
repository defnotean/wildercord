package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SleepingBladeRulesTest {
	@Test void intentRequiresFormAndAFreeHandAndBond() {
		assertTrue(SleepingBladeRules.intent(4,true,true,false));
		assertFalse(SleepingBladeRules.intent(3,true,true,false));
		assertFalse(SleepingBladeRules.intent(4,false,true,false));
		assertFalse(SleepingBladeRules.intent(4,true,false,false));
		assertFalse(SleepingBladeRules.intent(4,true,true,true));
	}
	@Test void DrawRejectsMovementDamageAirAndDistance() {
		assertTrue(SleepingBladeRules.holding(true,true,true,true,false,.04,9));
		assertFalse(SleepingBladeRules.holding(true,true,true,true,false,.1,9));
		assertFalse(SleepingBladeRules.holding(true,true,true,true,true,0,9));
		assertFalse(SleepingBladeRules.holding(true,false,true,true,false,0,9));
		assertFalse(SleepingBladeRules.holding(true,true,false,true,false,0,9));
		assertFalse(SleepingBladeRules.holding(true,true,true,false,false,0,9));
		assertFalse(SleepingBladeRules.holding(true,true,true,true,false,0,13));
	}
	@Test void TerrainRejectsWaterAndAbruptCliffs() {
		assertTrue(SleepingBladeRules.footing(85,85,63,new int[]{82,85,88,83}));
		assertFalse(SleepingBladeRules.footing(85,83,63,new int[]{85,85,85,85}));
		assertFalse(SleepingBladeRules.footing(60,60,63,new int[]{60,60,60,60}));
		assertFalse(SleepingBladeRules.footing(85,85,63,new int[]{85,85,89,85}));
	}
	@Test void AnsweringHasATradeoffRatherThanUniversalDamage() {
		assertEquals(1.3,SleepingBladeRules.momentum("guard"));
		assertEquals(.8,SleepingBladeRules.momentum("hit"));
		assertEquals(1,SleepingBladeRules.momentum("art"));
		assertEquals(.85,SleepingBladeRules.guardCost());
	}
}
