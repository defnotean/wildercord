package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FrostWyrmlingRulesTest {
	@Test void aPlayerWellIntoTheHallWakesItAndOneAtTheMouthDoesNot(){
		assertTrue(FrostWyrmlingRules.wakes(0,0,-10));
		assertTrue(FrostWyrmlingRules.wakes(6,3,-8));
		assertFalse(FrostWyrmlingRules.wakes(0,0,-16));
		assertFalse(FrostWyrmlingRules.wakes(0,7,-3));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(FrostWyrmlingRules.scripted(FrostWyrmlingRules.DORMANT));
		assertTrue(FrostWyrmlingRules.scripted(FrostWyrmlingRules.RISE));
		assertTrue(FrostWyrmlingRules.scripted(FrostWyrmlingRules.BED));
		assertFalse(FrostWyrmlingRules.scripted(FrostWyrmlingRules.WATCH));
		assertFalse(FrostWyrmlingRules.scripted(FrostWyrmlingRules.BREATH));
	}
	@Test void theRiseRunsForwardAndCurlingUpRunsItBack(){
		assertEquals(0,FrostWyrmlingRules.rise(FrostWyrmlingRules.DORMANT,30));
		assertEquals(.5F,FrostWyrmlingRules.rise(FrostWyrmlingRules.RISE,FrostWyrmlingRules.RISE_TICKS/2F),1e-6);
		assertEquals(1,FrostWyrmlingRules.rise(FrostWyrmlingRules.RISE,999));
		assertEquals(1,FrostWyrmlingRules.rise(FrostWyrmlingRules.BED,0));
		assertEquals(0,FrostWyrmlingRules.rise(FrostWyrmlingRules.BED,FrostWyrmlingRules.RISE_TICKS));
		assertEquals(1,FrostWyrmlingRules.rise(FrostWyrmlingRules.POUNCE,0));
	}
	@Test void itSweepsItsTailAtWhoeverIsRoundItsFlank(){
		assertEquals(FrostWyrmlingRules.TAIL,FrostWyrmlingRules.choose(3,120,.9,true,true,true,true));
		assertEquals(FrostWyrmlingRules.BITE,FrostWyrmlingRules.choose(3,120,.9,false,true,true,true));
	}
	@Test void upCloseItBitesOrBeatsItsWings(){
		assertEquals(FrostWyrmlingRules.BITE,FrostWyrmlingRules.choose(2,0,.9,true,true,true,true));
		assertEquals(FrostWyrmlingRules.WING,FrostWyrmlingRules.choose(2,0,.1,true,true,true,true));
		assertEquals(FrostWyrmlingRules.BITE,FrostWyrmlingRules.choose(2,0,.1,true,false,true,true));
	}
	@Test void aLittleWayOffItBreathesAndFurtherOutItPounces(){
		assertEquals(FrostWyrmlingRules.BREATH,FrostWyrmlingRules.choose(6,0,.3,true,true,true,true));
		assertEquals(FrostWyrmlingRules.POUNCE,FrostWyrmlingRules.choose(6,0,.9,true,true,true,true));
		assertEquals(FrostWyrmlingRules.BREATH,FrostWyrmlingRules.choose(6,0,.9,true,true,true,false));
		assertEquals(FrostWyrmlingRules.POUNCE,FrostWyrmlingRules.choose(11,0,.3,true,true,true,true));
		assertEquals(-1,FrostWyrmlingRules.choose(11,0,.3,true,true,true,false));
		assertEquals(-1,FrostWyrmlingRules.choose(30,0,.3,true,true,true,true));
	}
	@Test void eachBlowReachesWhereItShould(){
		assertTrue(FrostWyrmlingRules.hits(FrostWyrmlingRules.BITE,3,0,1));
		assertFalse(FrostWyrmlingRules.hits(FrostWyrmlingRules.BITE,-3,0,1));
		assertTrue(FrostWyrmlingRules.hits(FrostWyrmlingRules.TAIL,-3,2,0));
		assertFalse(FrostWyrmlingRules.hits(FrostWyrmlingRules.TAIL,4,0,0));
		assertTrue(FrostWyrmlingRules.hits(FrostWyrmlingRules.WING,5,3,1));
		assertFalse(FrostWyrmlingRules.hits(FrostWyrmlingRules.WATCH,1,0,1));
	}
	@Test void theBreathIsAConeThatWidensAwayFromItsJaws(){
		assertTrue(FrostWyrmlingRules.breathed(8,3,0));
		assertFalse(FrostWyrmlingRules.breathed(2,3,0));
		assertFalse(FrostWyrmlingRules.breathed(FrostWyrmlingRules.BREATH_REACH+1,0,0));
		assertFalse(FrostWyrmlingRules.breathed(-2,0,0));
	}
	@Test void thePounceArcsAndLandsWhereItsPreyStood(){
		assertEquals(0,FrostWyrmlingRules.arc(0),1e-9);
		assertEquals(0,FrostWyrmlingRules.arc(1),1e-9);
		assertEquals(FrostWyrmlingRules.POUNCE_HEIGHT,FrostWyrmlingRules.arc(.5),1e-9);
		assertEquals(7,FrostWyrmlingRules.leap(7),1e-9);
		assertEquals(FrostWyrmlingRules.POUNCE_REACH,FrostWyrmlingRules.leap(40),1e-9);
		assertTrue(FrostWyrmlingRules.crushed(1,1,0));
		assertFalse(FrostWyrmlingRules.crushed(3,3,0));
	}
	@Test void itsBrowTurnsBlowsAndItIsOpenWhenSpent(){
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.WATCH,true)<1);
		assertEquals(1,FrostWyrmlingRules.armour(FrostWyrmlingRules.WATCH,false));
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.RECOVER,true)>1);
		assertTrue(FrostWyrmlingRules.frontal(30));assertTrue(FrostWyrmlingRules.frontal(-370));assertFalse(FrostWyrmlingRules.frontal(90));
	}
	@Test void itBoundsAndBeatsAwayOnlyWhenRestedAndOnItsFeet(){
		assertTrue(FrostWyrmlingRules.dodges(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.BOUND_REST,.1));
		assertFalse(FrostWyrmlingRules.dodges(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.BOUND_REST-1,.1));
		assertFalse(FrostWyrmlingRules.dodges(FrostWyrmlingRules.BREATH,999,.1));
		assertTrue(FrostWyrmlingRules.leaps(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.STING_AFTER,FrostWyrmlingRules.LEAP_REST));
		assertFalse(FrostWyrmlingRules.leaps(FrostWyrmlingRules.POUNCE,999,999));
		assertFalse(FrostWyrmlingRules.leaps(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.STING_AFTER-1,999));
	}
	@Test void itRearsForItsLancesOnlyWoundedRestedAndWithinReach(){
		assertTrue(FrostWyrmlingRules.lances(true,FrostWyrmlingRules.LANCE_REST,6));
		assertFalse(FrostWyrmlingRules.lances(false,999,6));
		assertFalse(FrostWyrmlingRules.lances(true,FrostWyrmlingRules.LANCE_REST-1,6));
		assertFalse(FrostWyrmlingRules.lances(true,999,FrostWyrmlingRules.LANCE_REACH));
		assertEquals("lances",FrostWyrmlingRules.key(FrostWyrmlingRules.LANCES));
		assertFalse(FrostWyrmlingRules.scripted(FrostWyrmlingRules.LANCES));
	}
	@Test void theLancesFollowTheCrackOutToTheirReach(){
		assertEquals(0,FrostWyrmlingRules.lanceFront(FrostWyrmlingRules.LANCE_WARN-1),1e-9);
		assertEquals(FrostWyrmlingRules.LANCE_START,FrostWyrmlingRules.lanceFront(FrostWyrmlingRules.LANCE_WARN),1e-9);
		assertEquals(FrostWyrmlingRules.LANCE_START+3*FrostWyrmlingRules.LANCE_SPEED,FrostWyrmlingRules.lanceFront(FrostWyrmlingRules.LANCE_WARN+3),1e-9);
		assertEquals(FrostWyrmlingRules.LANCE_REACH,FrostWyrmlingRules.lanceFront(FrostWyrmlingRules.LANCE_WARN+FrostWyrmlingRules.LANCE_RUN),1e-9);
		assertEquals(FrostWyrmlingRules.LANCE_REACH,FrostWyrmlingRules.lanceFront(999),1e-9);
	}
	@Test void oneLineRunsAtEachPlayerAndTheRestFanEvenlyRound(){
		float[] alone=FrostWyrmlingRules.lines(new float[]{30});
		assertEquals(FrostWyrmlingRules.LANCE_LINES,alone.length);
		assertEquals(30,alone[0],1e-6);
		assertEquals(75,alone[1],1e-6);
		float[] two=FrostWyrmlingRules.lines(new float[]{0,100});
		assertEquals(0,two[0],1e-6);assertEquals(100,two[1],1e-6);
		// The fanned line at 90 runs too close beside the one aimed at 100, so it is left out.
		for(int i=2;i<two.length;i++)assertNotEquals(90,two[i],1e-6);
		assertEquals(FrostWyrmlingRules.LANCE_LINES,two.length);
	}
	@Test void aLanceStrikesOnlyWhereItHasBurst(){
		assertTrue(FrostWyrmlingRules.lanced(4,.5,0,6));
		assertFalse(FrostWyrmlingRules.lanced(8,0,0,6));
		assertFalse(FrostWyrmlingRules.lanced(4,1.5,0,6));
		assertFalse(FrostWyrmlingRules.lanced(4,0,4,6));
		assertFalse(FrostWyrmlingRules.lanced(0,0,0,6));
	}
	@Test void rearedForItsLancesItsBellyIsBared(){
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.LANCES,true)>1);
	}
	@Test void theRimescaleBitesBackOnlyOnceRimedOverAgain(){
		assertTrue(FrostWyrmlingRules.rimes(FrostWyrmlingRules.RIME_REST));
		assertFalse(FrostWyrmlingRules.rimes(FrostWyrmlingRules.RIME_REST-1));
		assertTrue(FrostWyrmlingRules.SCALE_GUARD>FrostWyrmlingRules.WAKE_RADIUS);
	}
	@Test void leftAloneLongEnoughItCurlsUp(){
		assertFalse(FrostWyrmlingRules.forgets(FrostWyrmlingRules.RESET_TICKS));
		assertTrue(FrostWyrmlingRules.forgets(FrostWyrmlingRules.RESET_TICKS+1));
		assertTrue(FrostWyrmlingRules.wounded(180,360));assertFalse(FrostWyrmlingRules.wounded(181,360));
		assertEquals("breath",FrostWyrmlingRules.key(FrostWyrmlingRules.BREATH));
		assertEquals("watch",FrostWyrmlingRules.key(FrostWyrmlingRules.WATCH));
	}
}
