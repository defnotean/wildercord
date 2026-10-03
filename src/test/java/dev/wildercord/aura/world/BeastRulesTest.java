package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class BeastRulesTest {
	@Test void habitatRequiresDryHighOpenGroundAndRoom() {
		assertTrue(BeastRules.habitat(true,false,true,true,90,63,1));
		assertFalse(BeastRules.habitat(true,false,true,true,74,63,0));
		assertFalse(BeastRules.habitat(true,false,true,true,90,63,2));
		assertFalse(BeastRules.habitat(true,true,true,true,90,63,0));
		assertFalse(BeastRules.habitat(true,false,false,true,90,63,0));
		assertFalse(BeastRules.habitat(true,false,true,false,90,63,0));
		assertFalse(BeastRules.habitat(false,false,true,true,90,63,0));
	}
	@Test void warningsHaveDistinctEvasionShapes() {
		assertTrue(BeastRules.chargeHit(2,0,0));assertFalse(BeastRules.chargeHit(2,1.3,0));assertFalse(BeastRules.chargeHit(-1,0,0));
		assertTrue(BeastRules.leapHit(4,0));assertFalse(BeastRules.leapHit(6,0));assertFalse(BeastRules.leapHit(1,3));
	}
	@Test void supportMagicAndRecoveryHaveAnOpening() {
		assertEquals(.12,BeastRules.spell(false,false));assertEquals(.4,BeastRules.spell(false,true));
		assertEquals(.2,BeastRules.spell(true,false));assertEquals(.65,BeastRules.spell(true,true));
	}
	@Test void quietApproachAvoidsTerritorialWarning() {
		assertTrue(BeastRules.wary(3,false,false));assertFalse(BeastRules.wary(3,true,false));assertFalse(BeastRules.wary(3,false,true));assertFalse(BeastRules.wary(5,false,false));
	}
}
