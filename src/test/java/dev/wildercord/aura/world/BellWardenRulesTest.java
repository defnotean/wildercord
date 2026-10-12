package dev.wildercord.aura.world;
import static dev.wildercord.aura.world.BellWardenRules.*;
import dev.wildercord.cast.MoveDeck;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BellWardenRulesTest {
	private static long[] rested(){
		long[] since=new long[32];
		Arrays.fill(since,10_000);
		return since;
	}
	private static double weight(int move,double distance,boolean behind,double height){
		return weights(distance,behind,height,rested(),false)[at(move)];
	}
	private static int at(int move){for(int i=0;i<PICKS.length;i++)if(PICKS[i]==move)return i;throw new AssertionError("not a pick: "+move);}
	private static boolean picks(int move,double distance,boolean behind){return weight(move,distance,behind,0)>0;}

	@Test void aPlayerHalfwayUpTheNaveWakesItAndOneAtTheDoorDoesNot(){
		assertTrue(wakes(0,0,-17));
		assertTrue(wakes(8,2,-12));
		assertFalse(wakes(0,0,-30));
		assertFalse(wakes(0,7,-4));
	}
	@Test void theHallIsABoxWidenedByItsMargin(){
		assertTrue(within(5.5,8,20,4,7,19,26,21,65,0));
		assertTrue(within(26.9,8,65.9,4,7,19,26,21,65,0));
		assertFalse(within(27.1,8,40,4,7,19,26,21,65,0));
		assertTrue(within(28.5,8,40,4,7,19,26,21,65,2));
		assertFalse(within(10,8,16.5,4,7,19,26,21,65,2));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(scripted(DORMANT));
		assertTrue(scripted(RISE));
		assertTrue(scripted(KNEEL));
		assertFalse(scripted(WATCH));
		assertFalse(scripted(CRACK));
	}
	@Test void theRiseRunsForwardAndTheKneelRunsItBack(){
		assertEquals(0,rise(DORMANT,30));
		assertEquals(.5F,rise(RISE,RISE_TICKS/2F),1e-6);
		assertEquals(1,rise(RISE,999));
		assertEquals(1,rise(KNEEL,0));
		assertEquals(0,rise(KNEEL,RISE_TICKS));
		assertEquals(1,rise(WATCH,0));
	}
	@Test void itShowsItsBarBeforeYouCanReachItAndForgetsOnlyAfterKneeling(){
		assertTrue(LEASH<BAR_SHOW && BAR_SHOW<BAR_DROP);
		assertTrue(RETURN_TICKS<RESET_TICKS);
		assertTrue(TOLL_AT>0 && TOLL_AT<RISE_TICKS);
		assertFalse(forgets(RESET_TICKS));assertTrue(forgets(RESET_TICKS+1));
	}
	@Test void everyMoveHasItsOwnName(){
		assertEquals("watch",key(WATCH));
		var names=new HashSet<String>();
		for(int m:PICKS)assertTrue(names.add(key(m)),"named twice: "+key(m));
		for(int m:new int[]{STUCK,DAZED,CRACK,SHELL,BACKSTEP,RECOVER})assertTrue(names.add(key(m)),"named twice: "+key(m));
		assertFalse(names.contains("watch"));
		assertTrue(PICKS.length>=7 && PICKS.length<=10);
	}
	@Test void everyMoveIsWoundUpLongEnoughToReadAndQuickerChained(){
		int[][] tells={{SWING_TELL,SWING_TELL_CHAINED},{HAMMER_TELL,HAMMER_TELL_CHAINED},{SLAM_TELL,SLAM_TELL_CHAINED},{CHARGE_TELL,CHARGE_TELL_CHAINED},
			{TOLL_TELL,TOLL_TELL_CHAINED},{LASH_TELL,LASH_TELL_CHAINED},{COVER_TELL,COVER_TELL_CHAINED}};
		for(int[] t:tells){
			assertTrue(t[0]>=10 && t[0]<=18,"tell "+t[0]);
			assertTrue(t[1]<t[0] && t[1]>=9,"chained tell "+t[1]);
		}
		assertTrue(PEAL_TELL>=10 && PEAL_TELL<=18);
		assertTrue(UNDERTOW_POUR>=10 && UNDERTOW_POUR<=18);
		// Its aim locks a few ticks before each blow, so a step at the last moment still counts.
		for(int lock:new int[]{HAMMER_LOCK,CHARGE_LOCK,LASH_LOCK,COVER_LOCK})assertTrue(lock>=3 && lock<=6);
		assertTrue(CRACK_TELL>=30,"its bell bursting is the longest tell of all");
	}

	@Test void itWhirlsMostAtWhoeverIsAtItsBack(){
		double whirl=weight(SWING,3,true,0);
		for(int m:PICKS)if(m!=SWING)assertTrue(weight(m,3,true,0)<whirl,key(m)+" over the whirl");
		assertTrue(weight(SWING,3,true,0)>weight(SWING,3,false,0));
		assertFalse(picks(SWING,SWING_REACH+1,false));
	}
	@Test void itHammersAndBringsItsBellDownOnlyOnWhoeverIsCloseInFront(){
		assertTrue(picks(HAMMER,3,false));assertFalse(picks(HAMMER,3,true));assertFalse(picks(HAMMER,1,false));assertFalse(picks(HAMMER,8,false));
		assertTrue(picks(COVER,4,false));assertFalse(picks(COVER,4,true));assertFalse(picks(COVER,1,false));assertFalse(picks(COVER,8,false));
	}
	@Test void itChargesLashesAndBreaksTheFloorUnderWhoeverKeepsAway(){
		for(int m:new int[]{SWING,HAMMER,COVER})assertFalse(picks(m,10,false),key(m)+" at range");
		for(int m:new int[]{CHARGE,LASH,SLAM,PEAL,UNDERTOW})assertTrue(picks(m,10,false),key(m)+" not at range");
		assertFalse(picks(CHARGE,3,false));assertTrue(weight(CHARGE,10,false,0)>weight(CHARGE,7,false,0));
		assertTrue(weight(CHARGE,10,true,0)<weight(CHARGE,10,false,0));
		assertFalse(picks(LASH,2,false));assertFalse(picks(PEAL,6,true));
		// Too far off for anything, it keeps closing.
		assertEquals(-1,choose(new MoveDeck(5),20,false,0,rested(),false,.5));
	}
	@Test void anyoneUpOutOfReachGetsTheFloorAndTheChain(){
		for(int m:new int[]{SWING,HAMMER,CHARGE,COVER})assertEquals(0,weight(m,3,false,4),"weighed up: "+key(m));
		double slam=weight(SLAM,3,false,4);
		for(int m:PICKS)if(m!=SLAM)assertTrue(weight(m,3,false,4)<slam,key(m)+" over the slam");
		assertTrue(weight(LASH,3,false,4)>0);
	}
	@Test void aMoveStillRestingWeighsNothingAndCrackedItRestsLess(){
		long[] since=rested();since[SLAM]=rest(SLAM,false)-1;
		assertEquals(0,weights(8,false,0,since,false)[at(SLAM)]);
		since[SLAM]=rest(SLAM,false);
		assertTrue(weights(8,false,0,since,false)[at(SLAM)]>0);
		since[SLAM]=rest(SLAM,true);
		assertTrue(weights(8,false,0,since,true)[at(SLAM)]>0);
		for(int m:PICKS){assertTrue(rest(m,true)<rest(m,false),key(m));assertTrue(rest(m,false)>0,key(m));}
		assertEquals(-1,choose(new MoveDeck(MEMORY),4,false,0,new long[32],false,.5));
	}
	@Test void itNeverMakesTheSameMoveTwiceRunningAndMixesThemAllUp(){
		var deck=new MoveDeck(MEMORY);
		var rng=new Random(7);
		long[] since=rested();
		Map<Integer,Integer> made=new HashMap<>();
		int last=-1;
		for(int i=0;i<300;i++){
			double d=new double[]{2,4,7,10,13}[i%5];boolean behind=i%7==0;
			int m=choose(deck,d,behind,0,since,i%2==0,rng.nextDouble());
			assertNotEquals(-1,m);
			assertNotEquals(last,m,"the same move twice running at "+d);
			made.merge(m,1,Integer::sum);
			deck.played(m);
			last=m;
		}
		assertTrue(made.size()>=8,"only "+made.keySet());
		for(var e:made.entrySet())assertTrue(e.getValue()<120,key(e.getKey())+" made "+e.getValue()+" times");
	}
	@Test void crackedItRunsOnIntoAnotherMoveAndNeverTheSameAgain(){
		assertEquals(HAMMER,chain(SWING,3,.1));assertEquals(LASH,chain(SWING,8,.1));assertEquals(-1,chain(SWING,3,.9));
		assertEquals(HAMMER,chain(LASH,4,.1));assertEquals(-1,chain(LASH,8,.1));
		assertEquals(CHARGE,chain(TOLL,8,.1));assertEquals(TOLL,chain(SLAM,8,.1));assertEquals(SLAM,chain(PEAL,8,.1));
		assertEquals(SWING,chain(UNDERTOW,3,.1));assertEquals(LASH,chain(CHARGE,8,.1));
		assertEquals(-1,chain(HAMMER,3,.1));assertEquals(-1,chain(COVER,3,.1));
		var picks=new HashSet<Integer>();for(int m:PICKS)picks.add(m);
		for(int m:PICKS)for(double d=0;d<=16;d+=.5){
			int n=chain(m,d,.1);
			assertNotEquals(m,n,key(m)+" chained into itself");
			assertTrue(n==-1 || picks.contains(n));
			assertEquals(-1,chain(m,d,.55));
		}
		assertEquals(2,CHAIN_MOST);
		assertTrue(GAP_CRACKED<GAP);
	}
	@Test void halfBeatenItCracksItsBellOnce(){
		assertTrue(cracks(50,100,false));assertTrue(cracks(20,100,false));
		assertFalse(cracks(51,100,false));
		assertFalse(cracks(20,100,true));
		assertTrue(blasts(CRACK_RADIUS-.5,0));assertFalse(blasts(CRACK_RADIUS+.5,0));assertFalse(blasts(2,5));
		assertTrue(CRACK_WAVE_REACH>WAVE_REACH);
	}

	@Test void theWhirlSweepsRoundItsReachAndCatchesEachInTurn(){
		assertTrue(whirls(SWING_REACH,0));assertFalse(whirls(SWING_REACH+.5,0));assertFalse(whirls(3,4));
		double step=360.0/SWING_SPIN;
		assertTrue(swept(10,1));assertFalse(swept(step+10,1));assertTrue(swept(step+10,2));
		assertTrue(swept(-10,SWING_SPIN));assertFalse(swept(-10,SWING_SPIN-1));
		assertFalse(swept(0,0));
		// Every bearing is passed exactly once in a whole turn.
		for(int a=0;a<360;a+=7){int n=0;for(long k=1;k<=SWING_SPIN;k++)if(swept(a,k))n++;assertEquals(1,n,"bearing "+a);}
	}
	@Test void theHammerIsNarrowAndTheSplitRunsOnAlongTheLine(){
		assertTrue(hits(HAMMER,3,1,0));assertFalse(hits(HAMMER,3,HAMMER_HALF+.3,0));assertFalse(hits(HAMMER,-1,0,0));assertFalse(hits(HAMMER,HAMMER_REACH+.5,0,0));
		assertEquals(HAMMER_REACH,split(0),1e-9);assertEquals(SPLIT_REACH,split(SPLIT_TICKS),1e-9);assertEquals(SPLIT_REACH,split(99),1e-9);
		for(long k=1;k<=SPLIT_TICKS;k++)assertTrue(split(k)>split(k-1));
		assertTrue(splits(6,.5,0,5,7));assertFalse(splits(6,SPLIT_HALF+.3,0,5,7));assertFalse(splits(6,0,2,5,7));assertFalse(splits(4,0,0,5,7));
	}
	@Test void theFistsLandAheadAndTheSpoutsComeUpUnderTheirMarks(){
		assertTrue(hits(SLAM,2,1,0));assertFalse(hits(SLAM,2,1,2.5));assertFalse(hits(SLAM,-1,0,0));
		assertTrue(spouts(1,0,0));assertFalse(spouts(SPOUT_RADIUS+.2,0,0));assertFalse(spouts(0,0,SPOUT_HEIGHT+1));
		assertTrue(SPOUT_DELAY>=10,"the marks bubble long enough to step off");
	}
	@Test void theChargeRunsDownOnlyWhatIsInItsPath(){
		assertTrue(hits(CHARGE,2,1.5,0));assertFalse(hits(CHARGE,2,2.5,0));assertFalse(hits(CHARGE,4,0,0));
		assertFalse(hits(WATCH,1,0,1));
		assertTrue(DAZED_TICKS>STUCK_TICKS && STUCK_TICKS>SWING_RECOVER);
	}
	@Test void theRingRollsOutAndIsJumped(){
		double was=4.45,r=5;
		assertTrue(crosses(5,was,r,0));assertTrue(crosses(5.4,was,r,.5));
		assertFalse(crosses(5,was,r,WAVE_HEIGHT+.2));
		assertFalse(crosses(7,was,r,0));assertFalse(crosses(3,was,r,0));
		assertTrue(WAVE_FAST>WAVE_SPEED);
	}
	@Test void theChainMissesWhoeverIsInsideItsReachAndHaulsTheFarthestHardest(){
		assertFalse(hits(LASH,1.5,0,0));assertTrue(hits(LASH,8,0,0));assertFalse(hits(LASH,8,LASH_HALF+.3,0));assertFalse(hits(LASH,LASH_REACH+1,0,0));
		assertEquals(0,tug(2),1e-9);assertTrue(tug(9)>tug(5));assertEquals(LASH_TUG_MOST,tug(100),1e-9);
	}
	@Test void itsBellComesDownWithinReachAndCoversItsCircle(){
		assertEquals(COVER_NEAR,drop(1),1e-9);assertEquals(3.3,drop(3.3),1e-9);assertEquals(COVER_FAR,drop(9),1e-9);
		assertTrue(covers(1,1,0));assertFalse(covers(2,2,0));assertFalse(covers(0,0,3.5));
		assertTrue(COVER_HOLD>=COVER_TELL,"it stays down long enough to punish");
	}
	@Test void thePealGoesOutInACone(){
		assertTrue(peals(5,0,0,false));assertFalse(peals(5,5,0,false));assertTrue(peals(5,5,0,true));
		assertFalse(peals(-2,0,0,false));assertFalse(peals(PEAL_REACH+1,0,0,false));
		assertTrue(PEAL_TURN<TURN && PEAL_TURN_CRACKED>PEAL_TURN,"it turns slowly enough to outrun");
	}
	@Test void theUndertowDragsTheWholeFloorInAndTheFistsComeDownAtItsFeet(){
		assertEquals(0,drag(1,false),1e-9);assertEquals(UNDERTOW_DRAG,drag(5,false),1e-9);assertEquals(UNDERTOW_DRAG_CRACKED,drag(5,true),1e-9);
		assertEquals(0,drag(UNDERTOW_RADIUS+1,false),1e-9);assertTrue(UNDERTOW_DRAG_CRACKED>UNDERTOW_DRAG);
		assertTrue(crushes(3,0,false));assertFalse(crushes(4,0,false));assertTrue(crushes(4,0,true));assertFalse(crushes(2,3,false));
	}

	@Test void itsBellTurnsBlowsFromTheFrontAndItsOpeningsLetMoreThrough(){
		assertTrue(armour(SHELL,false,false)<.25);
		assertTrue(armour(WATCH,true,false)<armour(WATCH,false,false));
		assertTrue(armour(STUCK,true,false)>armour(RECOVER,true,false) && armour(RECOVER,true,false)>1);
		assertEquals(armour(STUCK,true,false),armour(DAZED,true,false));
		assertTrue(armour(COVER,true,true)>1 && armour(COVER,true,true)>armour(COVER,true,false));
		assertTrue(armour(PEAL,false,false)>armour(SWING,false,false));
		assertTrue(armour(CRACK,false,false)<1);
		assertTrue(frontal(30));assertTrue(frontal(-390));assertFalse(frontal(150));
		assertTrue(behind(170));assertTrue(behind(-130));assertFalse(behind(100));
		assertEquals(-170,wrap(190),1e-9);assertEquals(170,wrap(-190),1e-9);
	}
	@Test void itShellsOnlyWhenHurtFastAndNotTwiceInARow(){
		assertTrue(shells(WATCH,SHELL_AFTER,SHELL_REST));
		assertTrue(shells(RECOVER,SHELL_AFTER,SHELL_REST));
		assertFalse(shells(WATCH,SHELL_AFTER-1,SHELL_REST));
		assertFalse(shells(WATCH,SHELL_AFTER,SHELL_REST-1));
		assertFalse(shells(SWING,99,999));
		assertFalse(shells(STUCK,99,999));
		assertTrue(dodges(WATCH,true,BACKSTEP_REST,.1));
		assertFalse(dodges(WATCH,false,BACKSTEP_REST,.1));
		assertFalse(dodges(WATCH,true,BACKSTEP_REST-1,.1));
		assertFalse(dodges(WATCH,true,BACKSTEP_REST,.9));
		assertFalse(dodges(SWING,true,999,.1));
	}
	@Test void theShardTollsOnlyNearDeathAndThenRestsAWhile(){
		assertTrue(knells(5,20,KNELL_RECHARGE));
		assertFalse(knells(10,20,KNELL_RECHARGE));
		assertFalse(knells(5,20,KNELL_RECHARGE-1));
		assertFalse(knells(0,20,KNELL_RECHARGE));
	}
}
