package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SwordTombRulesTest {
	@Test void thrustHasANarrowFixedLaneAndDoesNotHitBehindOrAbove(){
		assertTrue(SwordTombRules.hit(SwordTombRules.THRUST,6,.7,1));
		assertFalse(SwordTombRules.hit(SwordTombRules.THRUST,6,1,1));
		assertFalse(SwordTombRules.hit(SwordTombRules.THRUST,-1,0,0));
		assertFalse(SwordTombRules.hit(SwordTombRules.THRUST,4,0,3));
	}
	@Test void sweepThreatensTheFrontWithFlankingAndRangeAnswers(){
		assertTrue(SwordTombRules.hit(SwordTombRules.SWEEP,3,3,0));
		assertFalse(SwordTombRules.hit(SwordTombRules.SWEEP,.5,3,0));
		assertFalse(SwordTombRules.hit(SwordTombRules.SWEEP,5,2,0));
	}
	@Test void guardAndRecoveryCreateDifferentDamageWindows(){
		assertEquals(.35,SwordTombRules.multiplier(SwordTombRules.GUARD,true,false));
		assertEquals(1,SwordTombRules.multiplier(SwordTombRules.GUARD,false,false));
		assertEquals(1,SwordTombRules.multiplier(SwordTombRules.GUARD,true,true));
		assertEquals(1.25,SwordTombRules.multiplier(SwordTombRules.BROKEN,true,false));
		assertEquals(1.25,SwordTombRules.multiplier(SwordTombRules.RECOVER,true,false));
	}
	@Test void intentGatesNeedActualAuraAndWeapon(){
		assertTrue(SwordTombRules.gate(3,2,true,true));
		assertFalse(SwordTombRules.gate(1,2,true,true));
		assertFalse(SwordTombRules.gate(3,2,false,true));
		assertFalse(SwordTombRules.gate(3,2,true,false));
		assertTrue(SwordTombRules.WINDUP>=30);
	}
}
