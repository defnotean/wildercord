package dev.wildercord.aura.world;
import static dev.wildercord.aura.world.AshenStagRules.*;
import dev.wildercord.cast.MoveDeck;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AshenStagRulesTest {
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

	@Test void itTossesAndLocksOnlyWhoeverIsCloseInFront(){
		assertTrue(picks(TOSS,2,false));assertTrue(picks(TOSS,4,false));
		assertFalse(picks(TOSS,2,true));assertFalse(picks(TOSS,6,false));
		assertTrue(weight(TOSS,2,false,0)>weight(TOSS,4,false,0));
		assertTrue(picks(LOCK,4,false));assertFalse(picks(LOCK,4,true));assertFalse(picks(LOCK,1,false));assertFalse(picks(LOCK,8,false));
	}
	@Test void itBucksOnlyAtWhoeverIsAtItsBack(){
		assertTrue(picks(BUCK,2,true));assertTrue(picks(BUCK,6,true));
		assertFalse(picks(BUCK,2,false));assertFalse(picks(BUCK,9,true));
		assertTrue(weight(BUCK,2,true,0)>weight(REAR,2,true,0));
	}
	@Test void itChargesAndRakesWhoeverKeepsAwayAndRearsAtTheMiddleDistance(){
		assertFalse(picks(CHARGE,3,false));assertTrue(picks(CHARGE,10,false));assertFalse(picks(CHARGE,CHARGE_REACH+4,false));
		assertTrue(weight(CHARGE,10,false,0)>weight(CHARGE,6,false,0));
		assertTrue(weight(CHARGE,10,true,0)<weight(CHARGE,10,false,0));
		assertFalse(picks(RAKE,2,false));assertTrue(picks(RAKE,10,false));assertFalse(picks(RAKE,CINDER_RANGE+1,false));
		assertTrue(picks(REAR,4,false));assertTrue(picks(REAR,9,false));assertFalse(picks(REAR,12,false));
		assertFalse(picks(STAMPEDE,2,false));assertTrue(picks(STAMPEDE,10,false));
		assertTrue(picks(SMOKE,4,false));assertFalse(picks(SMOKE,12,false));
	}
	@Test void anyoneUpOutOfReachOfItsAntlersGetsCinders(){
		for(int m:new int[]{TOSS,BUCK,CHARGE,STAMPEDE,LOCK})assertEquals(0,weight(m,4,false,3),"weighed up: "+key(m));
		double rake=weight(RAKE,4,false,3);
		for(int m:PICKS)if(m!=RAKE)assertTrue(weight(m,4,false,3)<rake,key(m)+" over the rake");
	}
	@Test void aMoveStillRestingWeighsNothingAndKindledItRestsLess(){
		long[] since=rested();since[TOSS]=rest(TOSS,false)-1;
		double[] w=weights(2,false,0,since,false);
		assertEquals(0,w[at(TOSS)]);
		since[TOSS]=rest(TOSS,false);
		assertTrue(weights(2,false,0,since,false)[at(TOSS)]>0);
		for(int m:PICKS){assertTrue(rest(m,true)<rest(m,false),key(m));assertTrue(rest(m,false)>0,key(m));}
		long[] none=new long[32];
		assertEquals(-1,choose(new MoveDeck(5),4,false,0,none,false,.5));
	}
	@Test void itNeverMakesTheSameMoveTwiceRunningAndMixesThemAllUp(){
		var deck=new MoveDeck(5);
		var rng=new Random(11);
		long[] since=rested();
		Map<Integer,Integer> made=new HashMap<>();
		int last=-1;
		for(int i=0;i<300;i++){
			double d=new double[]{3,6,12}[i%3];
			int m=choose(deck,d,false,0,since,false,rng.nextDouble());
			assertNotEquals(-1,m);
			assertNotEquals(last,m,"the same move twice running at "+d);
			made.merge(m,1,Integer::sum);
			deck.played(m);
			last=m;
		}
		assertTrue(made.size()>=6,"only "+made.keySet());
		for(var e:made.entrySet())assertTrue(e.getValue()<150,key(e.getKey())+" made "+e.getValue()+" times");
	}
	@Test void kindledItRunsOnIntoTheMovesThatFollowAndNeverTheSameAgain(){
		assertEquals(REAR,chain(TOSS,3,.1));assertEquals(-1,chain(TOSS,3,.9));
		assertEquals(TOSS,chain(BUCK,3,.1));assertEquals(-1,chain(BUCK,6,.1));
		assertEquals(CHARGE,chain(REAR,8,.1));assertEquals(TOSS,chain(REAR,3,.1));assertEquals(-1,chain(REAR,3,.9));
		assertEquals(REAR,chain(RAKE,6,.1));assertEquals(RAKE,chain(STAMPEDE,6,.1));
		assertEquals(-1,chain(SMOKE,3,0));assertEquals(-1,chain(LOCK,3,0));assertEquals(-1,chain(CHARGE,3,0));
		for(int m:PICKS)for(double d:new double[]{2,5,9})for(double r=0;r<1;r+=.05)assertNotEquals(m,chain(m,d,r),key(m));
		assertTrue(rushes(true,false));assertFalse(rushes(true,true));assertFalse(rushes(false,false));
	}
	@Test void itKindlesOnceAtHalfHealth(){
		assertTrue(kindles(190,380,false));assertFalse(kindles(190,380,true));assertFalse(kindles(300,380,false));
		assertTrue(wounded(190,380));assertFalse(wounded(191,380));
		assertTrue(breaks(LOCK_BREAK));assertFalse(breaks(LOCK_BREAK-1));
	}
	@Test void everyTellIsLongEnoughToReadAndTheKindlingLongest(){
		for(int t:new int[]{TOSS_TELL,BUCK_TELL,REAR_TELL,CHARGE_TELL,RUSH_TELL,RAKE_TELL,HERD_TELL,SMOKE_TELL,LUNGE_TELL,LOCK_TELL}){
			assertTrue(t>=10&&t<=18,"tell "+t);assertTrue(t<KINDLE_TELL);
		}
		assertTrue(TOSS_TELL_CHAINED<TOSS_TELL&&REAR_TELL_CHAINED<REAR_TELL&&CHARGE_TELL_CHAINED<CHARGE_TELL&&BUCK_TELL_CHAINED<BUCK_TELL);
		assertTrue(TOSS_LOCK>0&&REAR_LOCK>0&&CHARGE_LOCK>0&&LUNGE_LOCK>0&&LOCK_LOCK>0);
		assertTrue(GAP_KINDLED<GAP);
	}
	@Test void eachBlowReachesWhereItShould(){
		assertTrue(hits(TOSS,3,0,1));assertTrue(hits(TOSS,3,1.2,1));assertFalse(hits(TOSS,3,1.5,1));assertFalse(hits(TOSS,-1,0,1));
		assertTrue(hits(BUCK,-3,0,1));assertFalse(hits(BUCK,3,0,1));assertFalse(hits(BUCK,-3,0,3.5));
		assertTrue(hits(LOCK,2,0,1));assertFalse(hits(LOCK,4,0,1));assertFalse(hits(LOCK,2,1.6,1));
		assertTrue(hits(LUNGE,3,1.5,1));assertFalse(hits(LUNGE,3,1.7,1));
		assertTrue(hits(CHARGE,2,1,1));assertTrue(hits(RUSH,2,1,1));assertFalse(hits(CHARGE,2,2.5,1));
		assertFalse(hits(WATCH,1,0,1));assertFalse(hits(REAR,2,0,0));
	}
	@Test void theBucksEmbersFanOutBehindIt(){
		assertTrue(fans(-5,1,0));assertFalse(fans(-5,4,0));assertFalse(fans(-FAN_REACH-.5,0,0));assertFalse(fans(1,0,0));assertFalse(fans(-4,0,3.5));
	}
	@Test void theRearCrushesWhereItsHoovesComeDownAndItsRingIsJumped(){
		assertTrue(crushes(REAR_AHEAD,0,0));assertTrue(crushes(REAR_AHEAD,REAR_RADIUS-.1,0));
		assertFalse(crushes(REAR_AHEAD+REAR_RADIUS+.1,0,0));assertFalse(crushes(REAR_AHEAD,0,2.5));
		assertTrue(crosses(5,4.6,5.3,0));assertFalse(crosses(5,4.6,5.3,1));assertFalse(crosses(8,4.6,5.3,0));
	}
	@Test void cindersBurstWhereTheyComeDownAndTheKindlingBlastsAllAboutIt(){
		assertTrue(cinders(1,0,1));assertFalse(cinders(1.5,0,1.5));assertFalse(cinders(0,3,0));
		assertTrue(blasts(5,0,0));assertFalse(blasts(KINDLE_RADIUS+.5,0,0));assertFalse(blasts(0,3.5,0));
		assertTrue(smothered(6,0,0));assertFalse(smothered(SMOKE_RADIUS+1,0,0));assertFalse(smothered(0,5,0));
	}
	@Test void theHerdRunsDownEvenlySpacedLanesWithGapsBetween(){
		double[] l=lanes(0);
		assertEquals(7,l.length);
		assertTrue(Arrays.stream(l).anyMatch(x->x==0));
		for(int i=1;i<l.length;i++)assertEquals(LANE_SPACING,l[i]-l[i-1],1e-9);
		for(double x:l)assertTrue(Math.abs(x)<=HERD_DEPTH);
		assertEquals(-l[0],l[l.length-1],1e-9);
		double[] h=lanes(LANE_SPACING/2);
		for(double x:h){assertTrue(Math.abs(x)<=HERD_DEPTH);for(double y:l)assertFalse(inLane(x,y),"the second herd runs down a gap");}
		assertTrue(inLane(1.5,0));assertFalse(inLane(1.6,0));assertFalse(inLane(LANE_SPACING/2,0));
		assertEquals(-HERD_WIDTH,lateral(1,0),1e-9);assertEquals(HERD_WIDTH,lateral(-1,0),1e-9);
		assertTrue(through(HERD_WIDTH*2/DEER_SPEED+1));assertFalse(through(HERD_WIDTH*2/DEER_SPEED-1));
		assertTrue(trampled(0,-.5,.6,0,0,0));assertFalse(trampled(3,-.5,.6,0,0,0));assertFalse(trampled(0,-.5,.6,3,0,0));assertFalse(trampled(0,-.5,.6,0,0,3));
	}
	@Test void itsBrowTurnsBlowsAndEachMoveLeavesItsOwnOpening(){
		assertTrue(armour(WATCH,true,false)<1);assertEquals(1,armour(WATCH,false,false));
		assertTrue(armour(LOCK,true,true)>1);assertTrue(armour(LOCK,true,false)<1);
		assertTrue(armour(REAR,true,true)>1);assertTrue(armour(REAR,true,false)<1);
		assertTrue(armour(TOSS,true,true)>1);assertTrue(armour(TOSS,true,false)<1);
		assertTrue(armour(RAKE,true,false)>1);assertTrue(armour(STAMPEDE,true,false)>1);
		assertTrue(armour(SKID,false,false)>1);assertEquals(1,armour(SKID,true,false));
		assertTrue(armour(RECOVER,true,false)>1);
		assertTrue(armour(STUCK,true,false)>armour(RECOVER,true,false));assertEquals(armour(STUCK,true,false),armour(STAGGER,false,false));
		assertTrue(frontal(30));assertTrue(frontal(-370));assertFalse(frontal(90));
		assertTrue(behind(180));assertTrue(behind(-150));assertFalse(behind(100));
	}
	@Test void everyMoveItShowsHasItsOwnName(){
		var seen=new HashSet<String>();
		for(int m=WATCH;m<=LUNGE;m++){if(scripted(m))continue;assertTrue(seen.add(key(m)),"two moves called "+key(m));}
		for(int m:PICKS)assertNotEquals("watch",key(m));
		assertEquals("herd",key(STAMPEDE));assertEquals("kindle",key(KINDLE));
	}
	@Test void theAntlerGoresOnlyAfterARunAndThenCools(){
		assertTrue(gores(ANTLER_SPRINT,ANTLER_RECHARGE));
		assertFalse(gores(ANTLER_SPRINT-1,ANTLER_RECHARGE));
		assertFalse(gores(0,ANTLER_RECHARGE));
		assertFalse(gores(ANTLER_SPRINT*4,ANTLER_RECHARGE-1));
	}
	@Test void aPlayerAFewStridesPastTheGateWakesItAndOneAtTheGateDoesNot(){
		assertTrue(wakes(0,0,-10));assertTrue(wakes(7,2,-8));
		assertFalse(wakes(0,0,-16));assertFalse(wakes(0,7,-3));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(scripted(DORMANT));assertTrue(scripted(RISE));assertTrue(scripted(BED));
		assertFalse(scripted(WATCH));assertFalse(scripted(STUCK));assertFalse(scripted(KINDLE));assertFalse(scripted(RUSH));
	}
	@Test void theRiseRunsForwardAndBeddingDownRunsItBack(){
		assertEquals(0,rise(DORMANT,30));
		assertEquals(.5F,rise(RISE,RISE_TICKS/2F),1e-6);
		assertEquals(1,rise(RISE,999));
		assertEquals(1,rise(BED,0));
		assertEquals(0,rise(BED,RISE_TICKS));
		assertEquals(1,rise(CHARGE,0));
	}
	@Test void itBoundsAndLeapsOnlyWhenRestedAndOnItsFeet(){
		assertTrue(dodges(WATCH,BOUND_REST,.1));
		assertFalse(dodges(WATCH,BOUND_REST-1,.1));
		assertFalse(dodges(STUCK,999,.1));assertFalse(dodges(TOSS,999,.1));
		assertFalse(dodges(WATCH,999,.9));
		assertTrue(leaps(WATCH,STING_AFTER,LEAP_REST));
		assertFalse(leaps(STUCK,999,999));assertFalse(leaps(LOCK,999,999));
		assertFalse(leaps(WATCH,STING_AFTER-1,999));
	}
	@Test void leftAloneLongEnoughItBedsDown(){
		assertFalse(forgets(RESET_TICKS));
		assertTrue(forgets(RESET_TICKS+1));
	}
	@Test void theEmbersBurnAlongTheTrackAndNotBesideOrBeyondIt(){
		assertTrue(onStrip(5,0,0,0,10,0));
		assertTrue(onStrip(5,.8,0,0,10,0));
		assertFalse(onStrip(5,1.2,0,0,10,0));
		assertTrue(onStrip(-.5,0,0,0,10,0));
		assertFalse(onStrip(11.5,0,0,0,10,0));
		assertTrue(onStrip(.3,.3,0,0,0,0));
	}
}
