package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlefieldRulesTest {
	@Test void dryGentleGroundOnly() {
		assertTrue(BattlefieldRules.footing(70,70,new int[]{65,75,70,72},63));
		assertFalse(BattlefieldRules.footing(70,63,new int[]{70,70,70,70},63));
		assertFalse(BattlefieldRules.footing(62,62,new int[]{62,62,62,62},63));
		assertFalse(BattlefieldRules.footing(70,70,new int[]{76,70,70,70},63));
		assertFalse(BattlefieldRules.footing(70,70,new int[]{70},63));
	}
	@Test void riteRequiresAliveBreathingBladeRangeAndWorld() {
		assertTrue(BattlefieldRules.holding(true,true,true,true,16));
		assertFalse(BattlefieldRules.holding(true,true,true,true,16.01));
		assertFalse(BattlefieldRules.holding(false,true,true,true,1));
		assertFalse(BattlefieldRules.holding(true,false,true,true,1));
		assertFalse(BattlefieldRules.holding(true,true,false,true,1));
		assertFalse(BattlefieldRules.holding(true,true,true,false,1));
	}
	@Test void historiesHaveDifferentLessonsAndStableKeys() {
		assertEquals(3,BattlefieldRules.MEMORIES.stream().map(BattlefieldRules.Memory::key).distinct().count());
		assertEquals(3,BattlefieldRules.MEMORIES.stream().map(BattlefieldRules.Memory::part).distinct().count());
		assertSame(BattlefieldRules.memory(0),BattlefieldRules.memory(3));
	}
}
