package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AshenStagRulesTest {
	@Test void aPlayerAFewStridesPastTheGateWakesItAndOneAtTheGateDoesNot(){
		assertTrue(AshenStagRules.wakes(0,0,-10));
		assertTrue(AshenStagRules.wakes(7,2,-8));
		assertFalse(AshenStagRules.wakes(0,0,-16));
		assertFalse(AshenStagRules.wakes(0,7,-3));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(AshenStagRules.scripted(AshenStagRules.DORMANT));
		assertTrue(AshenStagRules.scripted(AshenStagRules.RISE));
		assertTrue(AshenStagRules.scripted(AshenStagRules.BED));
		assertFalse(AshenStagRules.scripted(AshenStagRules.WATCH));
		assertFalse(AshenStagRules.scripted(AshenStagRules.STUCK));
	}
	@Test void theRiseRunsForwardAndBeddingDownRunsItBack(){
		assertEquals(0,AshenStagRules.rise(AshenStagRules.DORMANT,30));
		assertEquals(.5F,AshenStagRules.rise(AshenStagRules.RISE,AshenStagRules.RISE_TICKS/2F),1e-6);
		assertEquals(1,AshenStagRules.rise(AshenStagRules.RISE,999));
		assertEquals(1,AshenStagRules.rise(AshenStagRules.BED,0));
		assertEquals(0,AshenStagRules.rise(AshenStagRules.BED,AshenStagRules.RISE_TICKS));
		assertEquals(1,AshenStagRules.rise(AshenStagRules.CHARGE,0));
	}
	@Test void itKicksWhoeverIsAtItsBackAndGoresWhoeverIsInFront(){
		assertEquals(AshenStagRules.KICK,AshenStagRules.choose(2,true,.9,true,true,true));
		assertEquals(AshenStagRules.GORE,AshenStagRules.choose(2,false,.9,true,true,true));
		assertEquals(AshenStagRules.STOMP,AshenStagRules.choose(2,false,.1,true,true,true));
		assertEquals(AshenStagRules.GORE,AshenStagRules.choose(2,true,.9,true,true,false));
	}
	@Test void itStompsAtMiddleRangeAndChargesFromFurtherOut(){
		assertEquals(AshenStagRules.STOMP,AshenStagRules.choose(4.5,false,.9,true,true,true));
		assertEquals(AshenStagRules.CHARGE,AshenStagRules.choose(10,false,.9,true,true,true));
		assertEquals(-1,AshenStagRules.choose(10,false,.9,true,false,true));
		assertEquals(-1,AshenStagRules.choose(30,false,.9,true,true,true));
	}
	@Test void eachBlowReachesWhereItShould(){
		assertTrue(AshenStagRules.hits(AshenStagRules.GORE,3,0,1));
		assertFalse(AshenStagRules.hits(AshenStagRules.GORE,-3,0,1));
		assertTrue(AshenStagRules.hits(AshenStagRules.KICK,-3,0,1));
		assertFalse(AshenStagRules.hits(AshenStagRules.KICK,3,0,1));
		assertTrue(AshenStagRules.hits(AshenStagRules.STOMP,AshenStagRules.STOMP_AHEAD,2,0));
		assertFalse(AshenStagRules.hits(AshenStagRules.STOMP,AshenStagRules.STOMP_AHEAD,2,3));
		assertTrue(AshenStagRules.hits(AshenStagRules.CHARGE,2,1,1));
		assertFalse(AshenStagRules.hits(AshenStagRules.WATCH,1,0,1));
	}
	@Test void itsBrowTurnsBlowsAndItIsOpenWhenSpentOrStuck(){
		assertTrue(AshenStagRules.armour(AshenStagRules.WATCH,true)<1);
		assertEquals(1,AshenStagRules.armour(AshenStagRules.WATCH,false));
		assertTrue(AshenStagRules.armour(AshenStagRules.RECOVER,true)>1);
		assertTrue(AshenStagRules.armour(AshenStagRules.STUCK,true)>AshenStagRules.armour(AshenStagRules.RECOVER,true));
		assertTrue(AshenStagRules.frontal(30));assertTrue(AshenStagRules.frontal(-370));assertFalse(AshenStagRules.frontal(90));
		assertTrue(AshenStagRules.behind(180));assertTrue(AshenStagRules.behind(-150));assertFalse(AshenStagRules.behind(100));
	}
	@Test void itBoundsAndLeapsOnlyWhenRestedAndOnItsFeet(){
		assertTrue(AshenStagRules.dodges(AshenStagRules.WATCH,AshenStagRules.BOUND_REST,.1));
		assertFalse(AshenStagRules.dodges(AshenStagRules.WATCH,AshenStagRules.BOUND_REST-1,.1));
		assertFalse(AshenStagRules.dodges(AshenStagRules.STUCK,999,.1));
		assertFalse(AshenStagRules.dodges(AshenStagRules.WATCH,999,.9));
		assertTrue(AshenStagRules.leaps(AshenStagRules.WATCH,AshenStagRules.STING_AFTER,AshenStagRules.LEAP_REST));
		assertFalse(AshenStagRules.leaps(AshenStagRules.STUCK,999,999));
		assertFalse(AshenStagRules.leaps(AshenStagRules.WATCH,AshenStagRules.STING_AFTER-1,999));
	}
	@Test void leftAloneLongEnoughItBedsDown(){
		assertFalse(AshenStagRules.forgets(AshenStagRules.RESET_TICKS));
		assertTrue(AshenStagRules.forgets(AshenStagRules.RESET_TICKS+1));
		assertTrue(AshenStagRules.wounded(190,380));assertFalse(AshenStagRules.wounded(191,380));
	}
}
