package dev.wildercord.aura.world;
import dev.wildercord.cast.MoveDeck;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MojitoRulesTest {
	private static long[] rested(){
		long[] since=new long[32];
		Arrays.fill(since,10_000);
		return since;
	}
	private static boolean picks(int move,double distance,boolean behind){
		double[] w=MojitoRules.weights(distance,behind,rested());
		for(int i=0;i<MojitoRules.PICKS.length;i++) if(MojitoRules.PICKS[i]==move) return w[i]>0;
		return false;
	}
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
		assertEquals(1,MojitoRules.rise(MojitoRules.SWEEP,0));
	}
	@Test void upCloseHeHasTheBladeAtHisBackHisTailAndFurtherOffHisLongerReach(){
		assertTrue(picks(MojitoRules.SWEEP,4,false));
		assertTrue(picks(MojitoRules.CHOP,4,false));
		assertFalse(picks(MojitoRules.SWEEP,4,true));
		assertTrue(picks(MojitoRules.CHASE,4,true));
		assertFalse(picks(MojitoRules.FETCH,3,false));
		assertTrue(picks(MojitoRules.FETCH,12,false));
		assertTrue(picks(MojitoRules.DIG,12,false));
		assertTrue(picks(MojitoRules.POUNCE,12,false));
		assertTrue(picks(MojitoRules.BOLTS,25,false));
		assertFalse(picks(MojitoRules.POUNCE,25,false));
	}
	@Test void aMoveStillRestingIsNotPicked(){
		long[] since=rested();
		since[MojitoRules.CHOP]=MojitoRules.rest(MojitoRules.CHOP)-1;
		double[] w=MojitoRules.weights(4,false,since);
		for(int i=0;i<MojitoRules.PICKS.length;i++) if(MojitoRules.PICKS[i]==MojitoRules.CHOP) assertEquals(0,w[i]);
	}
	@Test void heNeverThrowsTheSameBlowTwiceRunningAndMixesThemAllUp(){
		var deck=new MoveDeck(4);
		var rng=new Random(11);
		long[] since=rested();
		Map<Integer,Integer> made=new HashMap<>();
		int last=-1;
		for(int i=0;i<300;i++){
			double d=new double[]{3,6,12}[i%3];
			int m=MojitoRules.choose(deck,d,false,since,rng.nextDouble());
			assertNotEquals(-1,m);
			assertNotEquals(last,m,"the same blow twice running at "+d);
			made.merge(m,1,Integer::sum);
			deck.played(m);
			last=m;
		}
		assertTrue(made.size()>=6,"only "+made.keySet());
		for(int n:made.values()) assertTrue(n<150,"one blow made "+n+" times: "+made);
	}
	@Test void wildHisBlowsRunIntoOneAnother(){
		assertEquals(MojitoRules.SWEEP,MojitoRules.chain(MojitoRules.SWEEP,4,.2));
		assertEquals(MojitoRules.CHOP,MojitoRules.chain(MojitoRules.SWEEP,4,.7));
		assertEquals(-1,MojitoRules.chain(MojitoRules.SWEEP,12,.2));
		assertEquals(MojitoRules.SWEEP,MojitoRules.chain(MojitoRules.POUNCE,4,.1));
		assertEquals(-1,MojitoRules.chain(MojitoRules.BOLTS,4,.1));
	}
	@Test void theSweepGoesAcrossTheFrontLowAndCanBeJumped(){
		assertEquals(90,MojitoRules.blade(1,0),1e-9);
		assertEquals(-90,MojitoRules.blade(1,MojitoRules.SWEEP_TICKS),1e-9);
		assertTrue(MojitoRules.sweeps(1,3,0,5,0));
		assertTrue(MojitoRules.sweeps(-1,1,-70,5,0));
		assertFalse(MojitoRules.sweeps(1,1,-70,5,0),"the blade has not got there yet");
		assertFalse(MojitoRules.sweeps(1,3,0,5,1.2),"jumped");
		assertFalse(MojitoRules.sweeps(1,3,0,9,0),"out of reach");
		assertFalse(MojitoRules.sweeps(1,3,180,3,0),"behind him");
	}
	@Test void theChopStrikesAlongALineAndTheCrackRunsOnPastIt(){
		assertTrue(MojitoRules.hits(MojitoRules.CHOP,5,0.5,0));
		assertFalse(MojitoRules.hits(MojitoRules.CHOP,5,2.5,0),"stepped off the line");
		assertFalse(MojitoRules.hits(MojitoRules.CHOP,-2,0,0));
		assertTrue(MojitoRules.cracks(12,1,0));
		assertFalse(MojitoRules.cracks(4,0,0),"the crack starts where the sword landed");
		assertFalse(MojitoRules.cracks(12,3,0));
	}
	@Test void aRingRunningOutCatchesOnlyTheGroundedAsItPasses(){
		assertTrue(MojitoRules.crosses(6,5.6,6.4,0));
		assertFalse(MojitoRules.crosses(6,5.6,6.4,1.2),"jumped");
		assertFalse(MojitoRules.crosses(9,5.6,6.4,0),"not there yet");
		assertFalse(MojitoRules.crosses(3,5.6,6.4,0),"already past");
	}
	@Test void aPounceComesDownOnItsPreyButNoFurtherThanHeCanLeap(){
		assertEquals(9,MojitoRules.leapLength(9),1e-9);
		assertEquals(MojitoRules.POUNCE_REACH,MojitoRules.leapLength(40),1e-9);
		assertTrue(MojitoRules.lands(3,0,2));
		assertFalse(MojitoRules.lands(5,0,0));
		assertFalse(MojitoRules.lands(0,4,0));
	}
	@Test void boltsSwordAndClodsHitWhoeverTheyPassThrough(){
		assertTrue(MojitoRules.boltHits(0,1,0,0,0,0,1.8));
		assertTrue(MojitoRules.boltHits(.8,2.2,0,0,0,0,1.8));
		assertFalse(MojitoRules.boltHits(2,1,0,0,0,0,1.8));
		assertTrue(MojitoRules.swordHits(1.3,1,0,0,0,0,1.8));
		assertFalse(MojitoRules.swordHits(2,1,0,0,0,0,1.8));
		assertTrue(MojitoRules.clods(1,0,1));
		assertFalse(MojitoRules.clods(2,0,1));
	}
	@Test void heShakesOffAnyoneWhoCrowdsHimOrKeepsHittingHim(){
		assertTrue(MojitoRules.shakes(MojitoRules.SHAKE_BLOWS,0,MojitoRules.SHAKE_REST));
		assertTrue(MojitoRules.shakes(0,MojitoRules.SHAKE_CROWD,MojitoRules.SHAKE_REST));
		assertFalse(MojitoRules.shakes(MojitoRules.SHAKE_BLOWS,0,MojitoRules.SHAKE_REST-1));
		assertFalse(MojitoRules.shakes(1,5,MojitoRules.SHAKE_REST));
	}
	@Test void heHowlsOnceAtHalfHealthAndOnlyThenZooms(){
		assertTrue(MojitoRules.howls(200,440,false));
		assertFalse(MojitoRules.howls(200,440,true));
		assertFalse(MojitoRules.howls(300,440,false));
		assertTrue(MojitoRules.zooms(true,MojitoRules.ZOOM_REST,6));
		assertFalse(MojitoRules.zooms(false,MojitoRules.ZOOM_REST,6));
		assertFalse(MojitoRules.zooms(true,MojitoRules.ZOOM_REST-1,6));
		assertEquals(MojitoRules.ZOOM_MIN_RADIUS,MojitoRules.zoomRadius(1),1e-9);
		assertEquals(MojitoRules.ZOOM_MAX_RADIUS,MojitoRules.zoomRadius(50),1e-9);
	}
	@Test void eachBlowLeavesItsOwnOpening(){
		assertTrue(MojitoRules.armour(MojitoRules.FLOP,true,false)>MojitoRules.armour(MojitoRules.RECOVER,true,false));
		assertTrue(MojitoRules.armour(MojitoRules.RECOVER,true,false)>MojitoRules.armour(MojitoRules.WATCH,false,false));
		assertTrue(MojitoRules.armour(MojitoRules.WATCH,true,false)<MojitoRules.armour(MojitoRules.WATCH,false,false));
		assertTrue(MojitoRules.armour(MojitoRules.CHOP,true,true)>MojitoRules.armour(MojitoRules.CHOP,true,false),"his belly bare reared up");
		assertTrue(MojitoRules.armour(MojitoRules.DIG,true,false)>MojitoRules.armour(MojitoRules.DIG,false,false),"his face, not his rump");
		assertTrue(MojitoRules.armour(MojitoRules.DIZZY,true,false)>1);
		assertTrue(MojitoRules.armour(MojitoRules.PRANCE,true,false)>1);
	}
	@Test void everyMoveHasItsOwnNameOnTheBar(){
		var keys=new java.util.HashSet<String>();
		for(int m=MojitoRules.SWEEP;m<=MojitoRules.PRANCE;m++) assertTrue(keys.add(MojitoRules.key(m)),"two moves named "+MojitoRules.key(m));
	}
	@Test void heSitsBetweenTheWyrmlingAndTheBellWarden(){
		assertTrue(MojitoRules.HEALTH>FrostWyrmlingRules.HEALTH && MojitoRules.HEALTH<BellWardenRules.HEALTH);
	}
}
