package dev.wildercord.aura.world;
import dev.wildercord.cast.MoveDeck;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FrostWyrmlingRulesTest {
	private static long[] rested(){
		long[] since=new long[32];
		Arrays.fill(since,10_000);
		return since;
	}
	private static boolean picks(int move,double distance,double angle,double height,boolean fierce){
		double[] w=FrostWyrmlingRules.weights(distance,angle,height,rested(),fierce);
		for(int i=0;i<FrostWyrmlingRules.PICKS.length;i++) if(FrostWyrmlingRules.PICKS[i]==move) return w[i]>0;
		return false;
	}
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
		assertFalse(FrostWyrmlingRules.scripted(FrostWyrmlingRules.SHELL));
		assertFalse(FrostWyrmlingRules.scripted(FrostWyrmlingRules.SPRAWL));
	}
	@Test void theRiseRunsForwardAndCurlingUpRunsItBack(){
		assertEquals(0,FrostWyrmlingRules.rise(FrostWyrmlingRules.DORMANT,30));
		assertEquals(.5F,FrostWyrmlingRules.rise(FrostWyrmlingRules.RISE,FrostWyrmlingRules.RISE_TICKS/2F),1e-6);
		assertEquals(1,FrostWyrmlingRules.rise(FrostWyrmlingRules.RISE,999));
		assertEquals(1,FrostWyrmlingRules.rise(FrostWyrmlingRules.BED,0));
		assertEquals(0,FrostWyrmlingRules.rise(FrostWyrmlingRules.BED,FrostWyrmlingRules.RISE_TICKS));
		assertEquals(1,FrostWyrmlingRules.rise(FrostWyrmlingRules.POUNCE,0));
	}
	@Test void eachMoveSuitsWhereItsPreyStands(){
		// Close in front: jaws and wings, never a dive or bounds.
		assertTrue(picks(FrostWyrmlingRules.BITE,2.5,0,0,false));
		assertTrue(picks(FrostWyrmlingRules.GALE,2.5,0,0,false));
		assertFalse(picks(FrostWyrmlingRules.DIVE,2.5,0,0,false));
		assertFalse(picks(FrostWyrmlingRules.POUNCE,2.5,0,0,false));
		// Round behind it: its tail, not its jaws.
		assertTrue(picks(FrostWyrmlingRules.TAIL,3,160,0,false));
		assertFalse(picks(FrostWyrmlingRules.BITE,3,160,0,false));
		assertFalse(picks(FrostWyrmlingRules.GALE,3,160,0,false));
		// A little way off: breath, the slide, bounds and the dive.
		assertTrue(picks(FrostWyrmlingRules.BREATH,7,0,0,false));
		assertTrue(picks(FrostWyrmlingRules.SLIDE,7,0,0,false));
		assertTrue(picks(FrostWyrmlingRules.POUNCE,7,0,0,false));
		assertTrue(picks(FrostWyrmlingRules.DIVE,7,0,0,false));
		assertFalse(picks(FrostWyrmlingRules.BITE,7,0,0,false));
		// Up out of reach: the icicles, and nothing that has to touch the floor they stand on.
		assertTrue(picks(FrostWyrmlingRules.ICEFALL,6,0,4,false));
		assertFalse(picks(FrostWyrmlingRules.BITE,4,0,4,false));
		assertFalse(picks(FrostWyrmlingRules.TAIL,4,160,4,false));
		assertFalse(picks(FrostWyrmlingRules.POUNCE,8,0,4,false));
		assertFalse(picks(FrostWyrmlingRules.DIVE,8,0,4,false));
		assertFalse(picks(FrostWyrmlingRules.SLIDE,8,0,4,false));
	}
	@Test void itsLancesComeOnlyOnceItIsFierce(){
		assertFalse(picks(FrostWyrmlingRules.LANCES,6,0,0,false));
		assertTrue(picks(FrostWyrmlingRules.LANCES,6,0,0,true));
		assertFalse(picks(FrostWyrmlingRules.LANCES,FrostWyrmlingRules.LANCE_REACH,0,0,true));
		assertEquals("lances",FrostWyrmlingRules.key(FrostWyrmlingRules.LANCES));
		assertFalse(FrostWyrmlingRules.scripted(FrostWyrmlingRules.LANCES));
	}
	@Test void aMoveStillRestingIsNotPickedAndFierceItRestsLess(){
		long[] since=rested();
		since[FrostWyrmlingRules.BITE]=FrostWyrmlingRules.rest(FrostWyrmlingRules.BITE,false)-1;
		double[] w=FrostWyrmlingRules.weights(2.5,0,0,since,false);
		for(int i=0;i<FrostWyrmlingRules.PICKS.length;i++) if(FrostWyrmlingRules.PICKS[i]==FrostWyrmlingRules.BITE) assertEquals(0,w[i]);
		for(int m:FrostWyrmlingRules.PICKS) assertTrue(FrostWyrmlingRules.rest(m,true)<FrostWyrmlingRules.rest(m,false),"move "+m);
	}
	@Test void itNeverMakesTheSameMoveTwiceRunningAndMixesThemAllUp(){
		var deck=new MoveDeck(FrostWyrmlingRules.MEMORY);
		var rng=new Random(11);
		long[] since=rested();
		Map<Integer,Integer> made=new HashMap<>();
		int last=-1;
		for(int i=0;i<300;i++){
			double d=new double[]{3,7,11}[i%3],a=new double[]{0,30,0,150}[i%4];
			int m=FrostWyrmlingRules.choose(deck,d,a,0,since,i>=150,rng.nextDouble());
			assertNotEquals(-1,m,"nothing at "+d+" and "+a);
			assertNotEquals(last,m,"the same move twice running at "+d+" and "+a);
			made.merge(m,1,Integer::sum);
			deck.played(m);
			last=m;
		}
		assertTrue(made.size()>=6,"only "+made.keySet());
		for(int n:made.values()) assertTrue(n<150,"one move made "+n+" times: "+made);
	}
	@Test void fierceItsMovesRunIntoOneAnotherButNeverIntoThemselves(){
		assertEquals(FrostWyrmlingRules.TAIL,FrostWyrmlingRules.chain(FrostWyrmlingRules.BITE,3,.2));
		assertEquals(FrostWyrmlingRules.GALE,FrostWyrmlingRules.chain(FrostWyrmlingRules.BITE,3,.6));
		assertEquals(-1,FrostWyrmlingRules.chain(FrostWyrmlingRules.BITE,9,.2));
		assertEquals(FrostWyrmlingRules.DIVE,FrostWyrmlingRules.chain(FrostWyrmlingRules.GALE,8,.2));
		// A dive or a slide always ends sprawled, and its lances stand alone.
		assertEquals(-1,FrostWyrmlingRules.chain(FrostWyrmlingRules.DIVE,5,.1));
		assertEquals(-1,FrostWyrmlingRules.chain(FrostWyrmlingRules.SLIDE,5,.1));
		assertEquals(-1,FrostWyrmlingRules.chain(FrostWyrmlingRules.LANCES,5,.1));
		for(int m:FrostWyrmlingRules.PICKS)for(double d:new double[]{2,4,6,9,13})for(double r:new double[]{0,.3,.55,.7,.9})
			assertNotEquals(m,FrostWyrmlingRules.chain(m,d,r),"move "+m+" into itself");
	}
	@Test void itShutsItselfInItsShellOnceWhenHalfBeaten(){
		assertTrue(FrostWyrmlingRules.shells(180,360,false));
		assertFalse(FrostWyrmlingRules.shells(180,360,true));
		assertFalse(FrostWyrmlingRules.shells(181,360,false));
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.SHELL,false)<.5);
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.DAZED,true)>1);
	}
	@Test void theSnapRunsStraightOutInFrontAndASidestepClearsIt(){
		assertTrue(FrostWyrmlingRules.bitten(3,0,1));
		assertTrue(FrostWyrmlingRules.bitten(FrostWyrmlingRules.BITE_REACH,1.5,0));
		assertFalse(FrostWyrmlingRules.bitten(3,2.5,0),"stepped aside");
		assertFalse(FrostWyrmlingRules.bitten(-2,0,0),"behind it");
		assertFalse(FrostWyrmlingRules.bitten(FrostWyrmlingRules.BITE_REACH+.5,0,0),"out of reach");
	}
	@Test void theTailSweepsRoundEverythingButItsFrontLowEnoughToJump(){
		assertEquals(45,FrostWyrmlingRules.tail(1,0),1e-9);
		assertEquals(-(45+FrostWyrmlingRules.TAIL_ARC),FrostWyrmlingRules.tail(-1,FrostWyrmlingRules.TAIL_TICKS),1e-9);
		boolean behind=false,flank=false,jumped=false,front=false;
		for(int k=1;k<=FrostWyrmlingRules.TAIL_TICKS;k++){
			behind|=FrostWyrmlingRules.lashed(1,k,180,3,0);
			flank|=FrostWyrmlingRules.lashed(-1,k,-90,4,0);
			jumped|=FrostWyrmlingRules.lashed(1,k,180,3,1);
			front|=FrostWyrmlingRules.lashed(1,k,0,3,0)||FrostWyrmlingRules.lashed(-1,k,0,3,0);
		}
		assertTrue(behind);assertTrue(flank);
		assertFalse(jumped,"jumped");assertFalse(front,"in front of it");
		assertFalse(FrostWyrmlingRules.lashed(1,3,180,FrostWyrmlingRules.TAIL_REACH+1,0),"out of reach");
		assertFalse(FrostWyrmlingRules.lashed(1,1,180,3,0),"the tail has not got there yet");
	}
	@Test void eachBeatOfItsWingsGustsAConeAhead(){
		assertTrue(FrostWyrmlingRules.gusted(5,3,1));
		assertFalse(FrostWyrmlingRules.gusted(5,5,0),"off to its side");
		assertFalse(FrostWyrmlingRules.gusted(-2,0,0),"behind it");
		assertFalse(FrostWyrmlingRules.gusted(FrostWyrmlingRules.GALE_REACH+1,0,0),"out of reach");
	}
	@Test void theBreathSweepsAcrossAndAPillarSheltersFromIt(){
		assertEquals(-FrostWyrmlingRules.BREATH_SWING,FrostWyrmlingRules.beam(1,0),1e-9);
		assertEquals(FrostWyrmlingRules.BREATH_SWING,FrostWyrmlingRules.beam(1,FrostWyrmlingRules.BREATH_TICKS),1e-9);
		assertEquals(-FrostWyrmlingRules.BREATH_SWING,FrostWyrmlingRules.beam(1,2*FrostWyrmlingRules.BREATH_TICKS),1e-9);
		assertEquals(FrostWyrmlingRules.BREATH_SWING,FrostWyrmlingRules.beam(-1,0),1e-9);
		assertTrue(FrostWyrmlingRules.breathed(0,5,0));
		assertFalse(FrostWyrmlingRules.breathed(30,5,0),"off the beam");
		assertFalse(FrostWyrmlingRules.breathed(0,FrostWyrmlingRules.BREATH_REACH+1,0),"out of reach");
		assertFalse(FrostWyrmlingRules.breathed(0,5,4),"above it");
		assertTrue(FrostWyrmlingRules.sheltered(0,8,0,4));
		assertFalse(FrostWyrmlingRules.sheltered(0,8,2,4),"the pillar off to one side");
		assertFalse(FrostWyrmlingRules.sheltered(0,8,0,10),"the pillar behind them");
		assertFalse(FrostWyrmlingRules.sheltered(0,8,0,-2),"the pillar behind its jaws");
	}
	@Test void itsHopsArcUpAndLandWhereItsPreyStood(){
		assertEquals(3,FrostWyrmlingRules.hops(false));assertEquals(4,FrostWyrmlingRules.hops(true));
		assertEquals(0,FrostWyrmlingRules.hop(-1),1e-9);
		assertEquals(0,FrostWyrmlingRules.hop(1),1e-9);
		assertEquals(FrostWyrmlingRules.HOP_HEIGHT,FrostWyrmlingRules.hop(FrostWyrmlingRules.HOP_GAP+FrostWyrmlingRules.HOP_TICKS/2.0),1e-9);
		assertEquals(0,FrostWyrmlingRules.hop(FrostWyrmlingRules.HOP_CYCLE),1e-9);
		assertTrue(FrostWyrmlingRules.lands(1,1,0,FrostWyrmlingRules.HOP_RADIUS));
		assertFalse(FrostWyrmlingRules.lands(2,2,0,FrostWyrmlingRules.HOP_RADIUS));
		assertTrue(FrostWyrmlingRules.lands(2,1.5,0,FrostWyrmlingRules.LAST_RADIUS),"the last lands widest");
		assertFalse(FrostWyrmlingRules.lands(1,0,3,FrostWyrmlingRules.HOP_RADIUS));
	}
	@Test void itDivesDownOntoItsPreyAndTheFrostRingCanBeJumped(){
		assertEquals(FrostWyrmlingRules.DIVE_MIN,FrostWyrmlingRules.dive(1),1e-9);
		assertEquals(8,FrostWyrmlingRules.dive(8),1e-9);
		assertEquals(FrostWyrmlingRules.DIVE_REACH,FrostWyrmlingRules.dive(40),1e-9);
		assertTrue(FrostWyrmlingRules.crosses(5,4.5,5.3,0));
		assertFalse(FrostWyrmlingRules.crosses(5,4.5,5.3,1),"jumped");
		assertFalse(FrostWyrmlingRules.crosses(8,4.5,5.3,0),"not there yet");
	}
	@Test void itsBrowTurnsBlowsAndItIsOpenWhenSpentSprawledOrRearedBack(){
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.WATCH,true)<1);
		assertEquals(1,FrostWyrmlingRules.armour(FrostWyrmlingRules.WATCH,false));
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.RECOVER,true)>1);
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.SPRAWL,true)>1);
		assertTrue(FrostWyrmlingRules.armour(FrostWyrmlingRules.LANCES,true)>1);
		assertEquals(1,FrostWyrmlingRules.armour(FrostWyrmlingRules.ICEFALL,true));
		assertTrue(FrostWyrmlingRules.frontal(30));assertTrue(FrostWyrmlingRules.frontal(-370));assertFalse(FrostWyrmlingRules.frontal(90));
	}
	@Test void itBoundsAndBeatsAwayOnlyWhenRestedAndOnItsFeet(){
		assertTrue(FrostWyrmlingRules.dodges(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.BOUND_REST,.1));
		assertFalse(FrostWyrmlingRules.dodges(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.BOUND_REST-1,.1));
		assertFalse(FrostWyrmlingRules.dodges(FrostWyrmlingRules.BREATH,999,.1));
		assertFalse(FrostWyrmlingRules.dodges(FrostWyrmlingRules.SPRAWL,999,.1));
		assertTrue(FrostWyrmlingRules.leaps(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.STING_AFTER,FrostWyrmlingRules.LEAP_REST));
		assertFalse(FrostWyrmlingRules.leaps(FrostWyrmlingRules.POUNCE,999,999));
		assertFalse(FrostWyrmlingRules.leaps(FrostWyrmlingRules.DAZED,999,999));
		assertFalse(FrostWyrmlingRules.leaps(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.STING_AFTER-1,999));
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
	@Test void theRimescaleBitesBackOnlyOnceRimedOverAgain(){
		assertTrue(FrostWyrmlingRules.rimes(FrostWyrmlingRules.RIME_REST));
		assertFalse(FrostWyrmlingRules.rimes(FrostWyrmlingRules.RIME_REST-1));
		assertTrue(FrostWyrmlingRules.SCALE_GUARD>FrostWyrmlingRules.WAKE_RADIUS);
	}
	@Test void leftAloneLongEnoughItCurlsUp(){
		assertFalse(FrostWyrmlingRules.forgets(FrostWyrmlingRules.RESET_TICKS));
		assertTrue(FrostWyrmlingRules.forgets(FrostWyrmlingRules.RESET_TICKS+1));
		assertTrue(FrostWyrmlingRules.wounded(180,360));assertFalse(FrostWyrmlingRules.wounded(181,360));
	}
	@Test void everyMoveHasItsOwnNameOnTheBar(){
		var keys=new HashSet<String>();
		int[] all={FrostWyrmlingRules.BITE,FrostWyrmlingRules.TAIL,FrostWyrmlingRules.GALE,FrostWyrmlingRules.BREATH,FrostWyrmlingRules.POUNCE,
			FrostWyrmlingRules.BOUND,FrostWyrmlingRules.RECOVER,FrostWyrmlingRules.LANCES,FrostWyrmlingRules.DIVE,FrostWyrmlingRules.SLIDE,
			FrostWyrmlingRules.ICEFALL,FrostWyrmlingRules.SPRAWL,FrostWyrmlingRules.SHELL,FrostWyrmlingRules.DAZED};
		for(int m:all){String k=FrostWyrmlingRules.key(m);assertNotEquals("watch",k,"move "+m);assertTrue(keys.add(k),"two moves called "+k);}
		assertEquals("breath",FrostWyrmlingRules.key(FrostWyrmlingRules.BREATH));
		assertEquals("watch",FrostWyrmlingRules.key(FrostWyrmlingRules.WATCH));
	}
}
