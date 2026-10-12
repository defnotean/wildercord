package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MojitoRulesTest {
	@Test void aPlayerWellIntoTheGroveWakesHimAndOneAtTheEdgeDoesNot(){
		assertTrue(MojitoRules.wakes(0,0,-12));
		assertTrue(MojitoRules.wakes(8,3,-8));
		assertFalse(MojitoRules.wakes(0,0,-18));
		assertFalse(MojitoRules.wakes(0,8,-3));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(MojitoRules.scripted(MojitoRules.DORMANT));
		assertTrue(MojitoRules.scripted(MojitoRules.RISE));
		assertTrue(MojitoRules.scripted(MojitoRules.BED));
		assertFalse(MojitoRules.scripted(MojitoRules.WATCH));
		assertFalse(MojitoRules.scripted(MojitoRules.FLOP));
	}
	@Test void theRiseRunsForwardAndCurlingUpRunsItBack(){
		assertEquals(0,MojitoRules.rise(MojitoRules.DORMANT,30));
		assertEquals(.5F,MojitoRules.rise(MojitoRules.RISE,MojitoRules.RISE_TICKS/2F),1e-6);
		assertEquals(1,MojitoRules.rise(MojitoRules.BED,0));
		assertEquals(0,MojitoRules.rise(MojitoRules.BED,MojitoRules.RISE_TICKS));
		assertEquals(1,MojitoRules.rise(MojitoRules.SLASH,0));
	}
	@Test void heSlashesInReachSpinsAtHisBackLeapsAtMiddleRangeAndCastsFromAfar(){
		assertEquals(MojitoRules.SLASH,MojitoRules.choose(4,false,.9,true,true,true,true));
		assertEquals(MojitoRules.SPIN,MojitoRules.choose(4,true,.9,true,true,true,true));
		assertEquals(MojitoRules.SPIN,MojitoRules.choose(4,false,.1,true,true,true,true));
		assertEquals(MojitoRules.HOP,MojitoRules.choose(2,false,.1,true,true,true,true));
		assertEquals(MojitoRules.LEAP,MojitoRules.choose(12,false,.2,true,true,true,true));
		assertEquals(MojitoRules.BOLTS,MojitoRules.choose(12,false,.9,true,true,true,true));
		assertEquals(MojitoRules.BOLTS,MojitoRules.choose(30,false,.1,true,true,true,true));
		assertEquals(MojitoRules.LEAP,MojitoRules.choose(12,false,.9,true,true,false,true));
		assertEquals(-1,MojitoRules.choose(12,false,.9,true,false,false,true));
	}
	@Test void theSlashCoversTheFrontAndTheSpinGoesAllRound(){
		assertTrue(MojitoRules.hits(MojitoRules.SLASH,5,3,0));
		assertTrue(MojitoRules.hits(MojitoRules.SLASH,0,6,1));
		assertFalse(MojitoRules.hits(MojitoRules.SLASH,-3,0,0));
		assertFalse(MojitoRules.hits(MojitoRules.SLASH,6,6,0));
		assertTrue(MojitoRules.hits(MojitoRules.SPIN,-5,-3,0));
		assertFalse(MojitoRules.hits(MojitoRules.SPIN,-6,-5,0));
		assertFalse(MojitoRules.hits(MojitoRules.SPIN,2,0,6));
	}
	@Test void aLeapComesDownOnItsPreyButNoFurtherThanHeCanLeap(){
		assertEquals(9,MojitoRules.leapLength(10),1e-9);
		assertEquals(MojitoRules.LEAP_REACH,MojitoRules.leapLength(40),1e-9);
		assertTrue(MojitoRules.lands(3,0,2));
		assertFalse(MojitoRules.lands(5,0,0));
		assertFalse(MojitoRules.lands(0,4,0));
	}
	@Test void aBoltHitsWhoeverItPassesThroughHeadToFoot(){
		assertTrue(MojitoRules.boltHits(0,1,0,0,0,0,1.8));
		assertTrue(MojitoRules.boltHits(.8,2.2,0,0,0,0,1.8));
		assertFalse(MojitoRules.boltHits(2,1,0,0,0,0,1.8));
		assertFalse(MojitoRules.boltHits(0,3.5,0,0,0,0,1.8));
	}
	@Test void heZoomsOnlyWoundedAndRestedAndInWideCircles(){
		assertTrue(MojitoRules.zooms(true,MojitoRules.ZOOM_REST,6));
		assertFalse(MojitoRules.zooms(false,MojitoRules.ZOOM_REST,6));
		assertFalse(MojitoRules.zooms(true,MojitoRules.ZOOM_REST-1,6));
		assertEquals(MojitoRules.ZOOM_MIN_RADIUS,MojitoRules.zoomRadius(1),1e-9);
		assertEquals(MojitoRules.ZOOM_MAX_RADIUS,MojitoRules.zoomRadius(50),1e-9);
	}
	@Test void heIsOpenSpentAndWideOpenFloppedDown(){
		assertTrue(MojitoRules.armour(MojitoRules.FLOP,true)>MojitoRules.armour(MojitoRules.RECOVER,true));
		assertTrue(MojitoRules.armour(MojitoRules.RECOVER,true)>MojitoRules.armour(MojitoRules.WATCH,false));
		assertTrue(MojitoRules.armour(MojitoRules.WATCH,true)<MojitoRules.armour(MojitoRules.WATCH,false));
	}
	@Test void heSitsBetweenTheWyrmlingAndTheBellWarden(){
		assertTrue(MojitoRules.HEALTH>FrostWyrmlingRules.HEALTH && MojitoRules.HEALTH<BellWardenRules.HEALTH);
	}
}
