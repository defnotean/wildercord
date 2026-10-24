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
	@Test void aStowedSilkNeverHoldsAndAnOutOneHoldsOnlyWhileItGrips(){
		assertTrue(HollowMatronRules.holds(false,0));
		assertTrue(HollowMatronRules.holds(false,HollowMatronRules.SILK_GRIP-1));
		assertFalse(HollowMatronRules.holds(false,HollowMatronRules.SILK_GRIP));
		assertFalse(HollowMatronRules.holds(true,0));
		assertFalse(HollowMatronRules.holds(true,HollowMatronRules.SILK_GRIP-1));
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
	private static long[] rested(){long[] since=new long[32];java.util.Arrays.fill(since,10000);return since;}
	private static int at(int move){for(int i=0;i<HollowMatronRules.PICKS.length;i++)if(HollowMatronRules.PICKS[i]==move)return i;return -1;}
	@Test void sheNeverMakesTheSameMoveTwiceRunningAndRangesOverManyOfThem(){
		var deck=new dev.wildercord.cast.MoveDeck(5);var r=new java.util.Random(11);var seen=new java.util.HashSet<Integer>();
		long[] since=rested();int last=-1;
		for(int i=0;i<300;i++){
			int m=HollowMatronRules.choose(deck,new double[]{3,6,12}[i%3],false,0,since,i>=150,r.nextDouble());
			assertNotEquals(-1,m);assertNotEquals(last,m);
			deck.played(m);seen.add(m);last=m;
		}
		assertTrue(seen.size()>=6,"only "+seen);
	}
	@Test void withNothingElseToHandSheKeepsClosingRatherThanRepeatHerself(){
		var deck=new dev.wildercord.cast.MoveDeck(5);long[] since=rested();
		since[HollowMatronRules.DRUM]=0;since[HollowMatronRules.SNARE]=0;
		assertEquals(HollowMatronRules.BRISTLE,HollowMatronRules.choose(deck,3,true,0,since,false,.5));
		deck.played(HollowMatronRules.BRISTLE);
		assertEquals(-1,HollowMatronRules.choose(deck,3,true,0,since,false,.5));
		deck.clear();
		assertEquals(HollowMatronRules.BRISTLE,HollowMatronRules.choose(deck,3,true,0,since,false,.5));
	}
	@Test void aMoveStillRestingIsNotWantedAndRousedTheyRestLess(){
		long[] since=rested();
		assertTrue(HollowMatronRules.weights(3,false,0,since,false)[at(HollowMatronRules.BITE)]>0);
		since[HollowMatronRules.BITE]=0;
		assertEquals(0,HollowMatronRules.weights(3,false,0,since,false)[at(HollowMatronRules.BITE)]);
		for(int m:HollowMatronRules.PICKS)assertTrue(HollowMatronRules.rest(m,true)<HollowMatronRules.rest(m,false));
	}
	@Test void whereYouStandSaysWhatSheReachesFor(){
		long[] since=rested();
		var front=HollowMatronRules.weights(3,false,0,since,false);var back=HollowMatronRules.weights(3,true,0,since,false);
		assertEquals(0,front[at(HollowMatronRules.BRISTLE)]);assertTrue(back[at(HollowMatronRules.BRISTLE)]>0);
		assertTrue(front[at(HollowMatronRules.BITE)]>0);assertEquals(0,back[at(HollowMatronRules.BITE)]);
		var far=HollowMatronRules.weights(12,false,0,since,false);
		assertEquals(0,far[at(HollowMatronRules.BITE)]);assertEquals(0,far[at(HollowMatronRules.SCYTHE)]);
		assertTrue(far[at(HollowMatronRules.REEL)]>0&&far[at(HollowMatronRules.SPIT)]>0);
		var up=HollowMatronRules.weights(6,false,4,since,false);
		assertEquals(0,up[at(HollowMatronRules.POUNCE)]);assertTrue(up[at(HollowMatronRules.SPIT)]>up[at(HollowMatronRules.DRUM)]);
	}
	@Test void herRunsNeverRepeatAMoveAndSomeEndWhereTheyAre(){
		for(int m=0;m<=HollowMatronRules.ROUSE;m++)for(double d:new double[]{2,4,5,7,10,15})for(double roll=0;roll<1;roll+=.05)
			assertNotEquals(m,HollowMatronRules.chain(m,d,roll));
		assertEquals(-1,HollowMatronRules.chain(HollowMatronRules.BRISTLE,3,0));
		assertEquals(-1,HollowMatronRules.chain(HollowMatronRules.STUCK,3,0));
		assertEquals(-1,HollowMatronRules.chain(HollowMatronRules.ROUSE,3,0));
	}
	@Test void everyMoveHasItsOwnName(){
		var keys=new java.util.HashSet<String>();
		for(int m:HollowMatronRules.PICKS)assertTrue(keys.add(HollowMatronRules.key(m)));
		for(int m:new int[]{HollowMatronRules.STUCK,HollowMatronRules.HAUL,HollowMatronRules.ROUSE})assertTrue(keys.add(HollowMatronRules.key(m)));
		assertFalse(keys.contains("watch"));
	}
	@Test void halfBeatenSheRousesOnceAndRousedSheThrowsMore(){
		assertTrue(HollowMatronRules.rouses(100,300,false));
		assertTrue(HollowMatronRules.rouses(150,300,false));
		assertFalse(HollowMatronRules.rouses(100,300,true));
		assertFalse(HollowMatronRules.rouses(300,300,false));
		assertTrue(HollowMatronRules.globs(true)>HollowMatronRules.globs(false));
		assertTrue(HollowMatronRules.scatter(true)>HollowMatronRules.scatter(false));
		assertTrue(HollowMatronRules.GAP_ROUSED<HollowMatronRules.GAP);
	}
	@Test void twoBlowsSnapHerThread(){
		assertFalse(HollowMatronRules.snaps(1));
		assertTrue(HollowMatronRules.snaps(2));
	}
	@Test void herBlowsReachWhatTheyLookLikeTheyReach(){
		assertTrue(HollowMatronRules.hits(HollowMatronRules.BITE,2,0,1));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.BITE,2,2,1));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.BITE,3,0,1));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.BITE,-2,0,1));
		assertTrue(HollowMatronRules.scythes(2,3.5,1,false));
		assertFalse(HollowMatronRules.scythes(2,3.5,1,true));
		assertFalse(HollowMatronRules.scythes(-2,1,1,false));
		assertFalse(HollowMatronRules.scythes(4,4,1,false));
		assertTrue(HollowMatronRules.bristles(-3,0,1,false));
		assertFalse(HollowMatronRules.bristles(-3,3,1,false));
		assertTrue(HollowMatronRules.bristles(-3,3,1,true));
		assertFalse(HollowMatronRules.bristles(3,0,1,true));
		assertFalse(HollowMatronRules.bristles(-7,0,1,true));
		assertTrue(HollowMatronRules.crosses(5,4.5,5.1,0));
		assertFalse(HollowMatronRules.crosses(8,4.5,5.1,0));
		assertFalse(HollowMatronRules.crosses(5,4.5,5.1,1));
		assertTrue(HollowMatronRules.snared(1,0,1));
		assertFalse(HollowMatronRules.snared(2,0,0));
		assertTrue(HollowMatronRules.sticks(1,0,0));
		assertFalse(HollowMatronRules.sticks(0,1.5,0));
		assertTrue(HollowMatronRules.blasts(4,0,4));
		assertFalse(HollowMatronRules.blasts(5,0,5));
		assertTrue(HollowMatronRules.hits(HollowMatronRules.POUNCE,-1,1.5,0));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.POUNCE,3,0,0));
		assertFalse(HollowMatronRules.hits(HollowMatronRules.SPIT,1,0,1));
	}
	@Test void herFrontTurnsBlowsHerAbdomenDoesNotAndSpentSheIsOpen(){
		assertTrue(HollowMatronRules.armour(HollowMatronRules.WATCH,true,false)<1);
		assertTrue(HollowMatronRules.armour(HollowMatronRules.WATCH,false,true)>1);
		assertEquals(1,HollowMatronRules.armour(HollowMatronRules.WATCH,false,false));
		assertTrue(HollowMatronRules.armour(HollowMatronRules.RECOVER,true,false)>1);
		assertEquals(1.6,HollowMatronRules.armour(HollowMatronRules.STUCK,true,false),1e-9);
		assertEquals(1.8,HollowMatronRules.armour(HollowMatronRules.BRISTLE,true,false,true),1e-9);
		assertEquals(1.15,HollowMatronRules.armour(HollowMatronRules.BRISTLE,false,false,true),1e-9);
		assertTrue(HollowMatronRules.armour(HollowMatronRules.DRUM,true,true,false)>HollowMatronRules.armour(HollowMatronRules.DRUM,false,true,false));
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
