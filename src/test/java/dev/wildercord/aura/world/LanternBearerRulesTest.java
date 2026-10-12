package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LanternBearerRulesTest {
	@Test void onlyAfterDarkAndInAmongTheGravesDoesItWake(){
		assertTrue(LanternBearerRules.wakes(5,0,5,true));
		assertFalse(LanternBearerRules.wakes(5,0,5,false));
		assertFalse(LanternBearerRules.wakes(10,0,10,true));
		assertFalse(LanternBearerRules.wakes(0,8,0,true));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(LanternBearerRules.scripted(LanternBearerRules.DORMANT));
		assertTrue(LanternBearerRules.scripted(LanternBearerRules.RISE));
		assertTrue(LanternBearerRules.scripted(LanternBearerRules.BED));
		assertFalse(LanternBearerRules.scripted(LanternBearerRules.WATCH));
		assertFalse(LanternBearerRules.scripted(LanternBearerRules.FADE));
	}
	@Test void theRiseRunsForwardAndSinkingRunsItBack(){
		assertEquals(0,LanternBearerRules.rise(LanternBearerRules.DORMANT,30));
		assertEquals(.5F,LanternBearerRules.rise(LanternBearerRules.RISE,LanternBearerRules.RISE_TICKS/2F),1e-6);
		assertEquals(1,LanternBearerRules.rise(LanternBearerRules.BED,0));
		assertEquals(0,LanternBearerRules.rise(LanternBearerRules.BED,LanternBearerRules.RISE_TICKS));
		assertEquals(1,LanternBearerRules.rise(LanternBearerRules.REAP,0));
	}
	@Test void itTakesTheLanternDownPartWayUpAndHangsItBackPartWayDown(){
		assertFalse(LanternBearerRules.holdsLantern(0));
		assertFalse(LanternBearerRules.holdsLantern(.5F));
		assertTrue(LanternBearerRules.holdsLantern(LanternBearerRules.rise(LanternBearerRules.RISE,LanternBearerRules.HOOK_AT)));
		assertTrue(LanternBearerRules.holdsLantern(1));
		assertTrue(LanternBearerRules.ROAR_AT>LanternBearerRules.HOOK_AT);
	}
	@Test void itDrivesItsSpadeDownAmongAnyoneCrowdingItOrRoundItsFlank(){
		assertEquals(LanternBearerRules.SLAM,LanternBearerRules.choose(2,0,.9,true,true,true));
		assertEquals(LanternBearerRules.SLAM,LanternBearerRules.choose(3.5,120,.9,true,true,true));
		assertEquals(LanternBearerRules.REAP,LanternBearerRules.choose(3,10,.9,true,true,true));
		assertEquals(LanternBearerRules.REAP,LanternBearerRules.choose(2,0,.9,true,true,false));
	}
	@Test void aWayOffItHooksOrLoosesWispsAndOtherwiseCloses(){
		assertEquals(LanternBearerRules.HOOK,LanternBearerRules.choose(7,0,.2,true,true,true));
		assertEquals(LanternBearerRules.WISPS,LanternBearerRules.choose(7,0,.8,true,true,true));
		assertEquals(LanternBearerRules.HOOK,LanternBearerRules.choose(7,0,.8,true,false,true));
		assertEquals(LanternBearerRules.WISPS,LanternBearerRules.choose(14,0,.2,true,true,true));
		assertEquals(-1,LanternBearerRules.choose(14,0,.2,true,false,true));
		assertEquals(-1,LanternBearerRules.choose(4.2,0,.2,true,false,true));
	}
	@Test void theReapSweepsInFrontAndNotBehind(){
		assertTrue(LanternBearerRules.hits(LanternBearerRules.REAP,3,1,0));
		assertTrue(LanternBearerRules.hits(LanternBearerRules.REAP,2,-3,1));
		assertFalse(LanternBearerRules.hits(LanternBearerRules.REAP,-1,0,0));
		assertFalse(LanternBearerRules.hits(LanternBearerRules.REAP,5,0,0));
	}
	@Test void theHookReachesFarButNarrowAndTheSlamAllRound(){
		assertTrue(LanternBearerRules.hits(LanternBearerRules.HOOK,9,.5,0));
		assertFalse(LanternBearerRules.hits(LanternBearerRules.HOOK,9,2,0));
		assertFalse(LanternBearerRules.hits(LanternBearerRules.HOOK,11,0,0));
		assertTrue(LanternBearerRules.hits(LanternBearerRules.SLAM,-2,2,0));
		assertFalse(LanternBearerRules.hits(LanternBearerRules.SLAM,3,3,0));
		assertFalse(LanternBearerRules.hits(LanternBearerRules.WATCH,1,0,0));
	}
	@Test void theCrookDragsHardAtFirstAndStopsClose(){
		assertTrue(LanternBearerRules.pull(9)>LanternBearerRules.pull(4));
		assertEquals(0,LanternBearerRules.pull(LanternBearerRules.HOOK_PULL_TO));
		assertEquals(0,LanternBearerRules.pull(1));
		assertTrue(LanternBearerRules.pull(100)<=1.6);
	}
	@Test void wispsBurstOnlyWhenTheyReachTheirPrey(){
		assertTrue(LanternBearerRules.bursts(1));
		assertFalse(LanternBearerRules.bursts(2));
	}
	@Test void smokeTakesNothingAndSpentItIsOpen(){
		assertEquals(0,LanternBearerRules.armour(LanternBearerRules.FADE,true));
		assertTrue(LanternBearerRules.armour(LanternBearerRules.RECOVER,true)>1);
		assertTrue(LanternBearerRules.armour(LanternBearerRules.WATCH,true)<LanternBearerRules.armour(LanternBearerRules.WATCH,false));
		assertTrue(LanternBearerRules.frontal(30));
		assertTrue(LanternBearerRules.frontal(370));
		assertFalse(LanternBearerRules.frontal(120));
	}
	@Test void itFadesOnlyWhileWatchingAndNotTooOften(){
		assertTrue(LanternBearerRules.dodges(LanternBearerRules.WATCH,LanternBearerRules.FADE_REST,.1));
		assertFalse(LanternBearerRules.dodges(LanternBearerRules.WATCH,LanternBearerRules.FADE_REST-1,.1));
		assertFalse(LanternBearerRules.dodges(LanternBearerRules.REAP,999,.1));
		assertFalse(LanternBearerRules.dodges(LanternBearerRules.WATCH,999,.9));
	}
	@Test void stungEnoughItDriftsAway(){
		assertTrue(LanternBearerRules.leaps(LanternBearerRules.WATCH,LanternBearerRules.STING_AFTER,LanternBearerRules.LEAP_REST));
		assertFalse(LanternBearerRules.leaps(LanternBearerRules.WATCH,LanternBearerRules.STING_AFTER-1,LanternBearerRules.LEAP_REST));
		assertFalse(LanternBearerRules.leaps(LanternBearerRules.SLAM,99,999));
	}
	@Test void dawnSendsItDownAndBeingLeftAloneLongEnoughForgets(){
		assertFalse(LanternBearerRules.dawn(LanternBearerRules.DAWN_TICKS-1));
		assertTrue(LanternBearerRules.dawn(LanternBearerRules.DAWN_TICKS));
		assertFalse(LanternBearerRules.forgets(LanternBearerRules.RESET_TICKS));
		assertTrue(LanternBearerRules.forgets(LanternBearerRules.RESET_TICKS+1));
		assertTrue(LanternBearerRules.wounded(170,340));
		assertFalse(LanternBearerRules.wounded(171,340));
	}
	@Test void theBarNamesWhatItIsDoing(){
		assertEquals("reap",LanternBearerRules.key(LanternBearerRules.REAP));
		assertEquals("wisps",LanternBearerRules.key(LanternBearerRules.WISPS));
		assertEquals("watch",LanternBearerRules.key(LanternBearerRules.RISE));
	}
}
