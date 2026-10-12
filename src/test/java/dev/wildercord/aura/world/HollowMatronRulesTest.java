package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HollowMatronRulesTest {
	@Test void aPlayerAFewStridesIntoTheHallWakesHerAndOneAtTheDriftMouthDoesNot(){
		assertTrue(HollowMatronRules.wakes(0,0,-10));
		assertTrue(HollowMatronRules.wakes(8,3,-8));
		assertFalse(HollowMatronRules.wakes(0,0,-14));
		assertFalse(HollowMatronRules.wakes(0,7,-3));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(HollowMatronRules.scripted(HollowMatronRules.DORMANT));
		assertTrue(HollowMatronRules.scripted(HollowMatronRules.DROP));
		assertTrue(HollowMatronRules.scripted(HollowMatronRules.CLIMB));
		assertFalse(HollowMatronRules.scripted(HollowMatronRules.WATCH));
		assertFalse(HollowMatronRules.scripted(HollowMatronRules.RECOVER));
	}
	@Test void theDropRunsForwardAndClimbingRunsItBack(){
		assertEquals(0,HollowMatronRules.drop(HollowMatronRules.DORMANT,30));
		assertEquals(.5F,HollowMatronRules.drop(HollowMatronRules.DROP,HollowMatronRules.DROP_TICKS/2F),1e-6);
		assertEquals(1,HollowMatronRules.drop(HollowMatronRules.DROP,999));
		assertEquals(1,HollowMatronRules.drop(HollowMatronRules.CLIMB,0));
		assertEquals(0,HollowMatronRules.drop(HollowMatronRules.CLIMB,HollowMatronRules.DROP_TICKS));
		assertEquals(1,HollowMatronRules.drop(HollowMatronRules.BITE,0));
	}
	@Test void sheFallsFasterAndFasterThenBrakesOntoTheFloor(){
		assertEquals(0,HollowMatronRules.descent(0),1e-9);
		assertEquals(1,HollowMatronRules.descent(HollowMatronRules.FALL_END),1e-9);
		assertEquals(1,HollowMatronRules.descent(1),1e-9);
		double prev=0,step=0;
		for(int i=1;i<=8;i++){
			double now=HollowMatronRules.descent(HollowMatronRules.FALL_END*.8F*i/8);
			assertTrue(now-prev>=step-1e-9);step=now-prev;prev=now;
		}
		assertTrue(HollowMatronRules.descent(HollowMatronRules.FALL_END*.9F)<1);
	}
	@Test void upCloseSheBitesOrSweepsAndFurtherOutSheLeapsOrSpits(){
		assertEquals(HollowMatronRules.BITE,HollowMatronRules.choose(2,.9,true,true,true));
		assertEquals(HollowMatronRules.SWEEP,HollowMatronRules.choose(2,.1,true,true,true));
		assertEquals(HollowMatronRules.BITE,HollowMatronRules.choose(2,.1,false,true,true));
		assertEquals(HollowMatronRules.POUNCE,HollowMatronRules.choose(8,.3,true,true,true));
		assertEquals(HollowMatronRules.SPIT,HollowMatronRules.choose(8,.9,true,true,true));
		assertEquals(HollowMatronRules.SPIT,HollowMatronRules.choose(15,.1,true,true,true));
		assertEquals(-1,HollowMatronRules.choose(15,.1,true,true,false));
		assertEquals(-1,HollowMatronRules.choose(30,.1,true,true,true));
	}
	@Test void herBlowsReachWhatTheyLookLikeTheyReach(){
		assertTrue(HollowMatronRules.hits(HollowMatronRules.BITE,3,0,1));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.BITE,3,2.5,1));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.BITE,-2,0,1));
		assertTrue(HollowMatronRules.hits(HollowMatronRules.SWEEP,2,3.5,1));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.SWEEP,-2,1,1));
		assertTrue(HollowMatronRules.hits(HollowMatronRules.POUNCE,-1,1.5,0));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.POUNCE,3,0,0));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.SPIT,1,0,1));
	}
	@Test void herFrontTurnsBlowsHerAbdomenDoesNotAndSpentSheIsOpen(){
		assertTrue(HollowMatronRules.armour(HollowMatronRules.WATCH,true,false)<1);
		assertTrue(HollowMatronRules.armour(HollowMatronRules.WATCH,false,true)>1);
		assertEquals(1,HollowMatronRules.armour(HollowMatronRules.WATCH,false,false));
		assertTrue(HollowMatronRules.armour(HollowMatronRules.RECOVER,true,false)>1);
		assertTrue(HollowMatronRules.frontal(30)&&!HollowMatronRules.frontal(90));
		assertTrue(HollowMatronRules.rear(180)&&HollowMatronRules.rear(-150)&&!HollowMatronRules.rear(90));
	}
	@Test void aPounceArcsHighestHalfwayAndLandsOnTheGround(){
		assertEquals(0,HollowMatronRules.arc(0,10),1e-9);
		assertEquals(HollowMatronRules.POUNCE_HEIGHT,HollowMatronRules.arc(5,10),1e-9);
		assertEquals(0,HollowMatronRules.arc(10,10),1e-9);
		assertTrue(HollowMatronRules.arc(2,10)<HollowMatronRules.arc(4,10));
	}
	@Test void woundedSheAmbushesFromHerPillarButNotTooOften(){
		assertFalse(HollowMatronRules.ambushes(300,300,99999));
		assertTrue(HollowMatronRules.ambushes(200,300,99999));
		assertFalse(HollowMatronRules.ambushes(200,300,HollowMatronRules.AMBUSH_REST-1));
		assertTrue(HollowMatronRules.aloft(HollowMatronRules.SCALE)&&HollowMatronRules.aloft(HollowMatronRules.LURK)&&HollowMatronRules.aloft(HollowMatronRules.PLUNGE));
		assertFalse(HollowMatronRules.aloft(HollowMatronRules.TUMBLE)||HollowMatronRules.aloft(HollowMatronRules.HASTE));
	}
	@Test void moreHatchForMorePreyButNeverTooMany(){
		assertEquals(2,HollowMatronRules.brood(1));
		assertEquals(3,HollowMatronRules.brood(2));
		assertEquals(HollowMatronRules.BROOD_MAX,HollowMatronRules.brood(20));
	}
	@Test void threeStrikesCutHerDownAndSheIsOpenAfter(){
		assertFalse(HollowMatronRules.cut(2));
		assertTrue(HollowMatronRules.cut(3));
		assertTrue(HollowMatronRules.armour(HollowMatronRules.TUMBLE,true,false)>1);
	}
	@Test void herPlungeBracesThenFallsFasterAndFaster(){
		assertEquals(0,HollowMatronRules.plunge(0),1e-9);
		assertEquals(1,HollowMatronRules.plunge(1),1e-9);
		assertTrue(HollowMatronRules.plunge(.6)<HollowMatronRules.plunge(.9));
		assertTrue(HollowMatronRules.hits(HollowMatronRules.PLUNGE,1,2,0));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.PLUNGE,4,0,0));
	}
	@Test void herSilkHoldsAWhileOffTheGroundThenLetsGo(){
		assertTrue(HollowMatronRules.grips(0));
		assertTrue(HollowMatronRules.grips(HollowMatronRules.SILK_GRIP-1));
		assertFalse(HollowMatronRules.grips(HollowMatronRules.SILK_GRIP));
	}
}
