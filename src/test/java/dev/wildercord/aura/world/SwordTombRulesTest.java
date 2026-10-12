package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SwordTombRulesTest {
	@Test void thrustHasANarrowFixedLaneAndDoesNotHitBehindOrAbove(){
		assertTrue(SwordTombRules.hit(SwordTombRules.THRUST,6,.7,1));
		assertFalse(SwordTombRules.hit(SwordTombRules.THRUST,6,1,1));
		assertFalse(SwordTombRules.hit(SwordTombRules.THRUST,-1,0,0));
		assertFalse(SwordTombRules.hit(SwordTombRules.THRUST,4,0,3));
	}
	@Test void sweepThreatensTheFrontWithFlankingAndRangeAnswers(){
		assertTrue(SwordTombRules.hit(SwordTombRules.SWEEP,3,3,0));
		assertFalse(SwordTombRules.hit(SwordTombRules.SWEEP,.5,3,0));
		assertFalse(SwordTombRules.hit(SwordTombRules.SWEEP,5,2,0));
	}
	@Test void guardAndRecoveryCreateDifferentDamageWindows(){
		assertEquals(.35,SwordTombRules.multiplier(SwordTombRules.GUARD,true,false));
		assertEquals(1,SwordTombRules.multiplier(SwordTombRules.GUARD,false,false));
		assertEquals(1,SwordTombRules.multiplier(SwordTombRules.GUARD,true,true));
		assertEquals(1.25,SwordTombRules.multiplier(SwordTombRules.BROKEN,true,false));
		assertEquals(1.25,SwordTombRules.multiplier(SwordTombRules.RECOVER,true,false));
	}
	@Test void intentGatesNeedActualAuraAndWeapon(){
		assertTrue(SwordTombRules.gate(3,2,true,true));
		assertFalse(SwordTombRules.gate(1,2,true,true));
		assertFalse(SwordTombRules.gate(3,2,false,true));
		assertFalse(SwordTombRules.gate(3,2,true,false));
		assertTrue(SwordTombRules.WINDUP>=30);
	}
	private static double weight(double distance,boolean behind,int move){
		var w=SwordTombRules.weights(distance,behind);
		for(int i=0;i<SwordTombRules.PICKS.length;i++)if(SwordTombRules.PICKS[i]==move)return w[i];
		throw new AssertionError(move);
	}
	@Test void behindItTheHeelTurnIsFavouredAndTheFrontalBlowsAreOut(){
		assertEquals(0,weight(3,true,SwordTombRules.SWEEP));
		assertEquals(0,weight(3,true,SwordTombRules.THRUST));
		assertEquals(0,weight(3,true,SwordTombRules.GUARD));
		assertTrue(weight(3,true,SwordTombRules.HEEL)>weight(3,false,SwordTombRules.HEEL));
		assertEquals(0,weight(6,true,SwordTombRules.HEEL));
	}
	@Test void farOffItReachesWithTheHandsAndTheCharge(){
		assertEquals(0,weight(10,false,SwordTombRules.SWEEP));
		assertEquals(0,weight(10,false,SwordTombRules.THRUST));
		assertTrue(weight(10,false,SwordTombRules.HANDS)>weight(5,false,SwordTombRules.HANDS));
		assertTrue(weight(10,false,SwordTombRules.RAMPART)>0);
		assertEquals(0,weight(2,false,SwordTombRules.HANDS));
		assertEquals(0,weight(2,false,SwordTombRules.RAMPART));
	}
	@Test void itNeverThrowsTheSameBlowTwiceRunningAndAlwaysHasOne(){
		var deck=new dev.wildercord.cast.MoveDeck(SwordTombRules.MEMORY);
		var rng=new java.util.Random(7);int last=-1;
		var seen=new java.util.HashSet<Integer>();
		for(int i=0;i<400;i++){
			double d=rng.nextDouble()*14;boolean behind=rng.nextBoolean();
			int move=SwordTombRules.choose(deck,d,behind,rng.nextDouble());
			assertTrue(move>=0,"nothing to do at "+d);
			assertNotEquals(last,move);
			deck.played(move);last=move;seen.add(move);
		}
		for(int move:SwordTombRules.PICKS)assertTrue(seen.contains(move),"never "+move);
	}
	@Test void rousedItRunsSweepIntoThrustAndLeapIntoHeelTurnWhenClose(){
		assertEquals(SwordTombRules.THRUST,SwordTombRules.chain(SwordTombRules.SWEEP,4));
		assertEquals(-1,SwordTombRules.chain(SwordTombRules.SWEEP,9));
		assertEquals(SwordTombRules.HEEL,SwordTombRules.chain(SwordTombRules.VERDICT,3));
		assertEquals(-1,SwordTombRules.chain(SwordTombRules.VERDICT,6));
		assertEquals(-1,SwordTombRules.chain(SwordTombRules.THRUST,3));
	}
	@Test void itHearsFootfallsAndHopsButNotCreepingOrStillness(){
		assertFalse(SwordTombRules.heard(0,0,false));
		assertFalse(SwordTombRules.heard(.2,0,true));
		assertTrue(SwordTombRules.heard(.2,0,false));
		assertTrue(SwordTombRules.heard(0,.4,true));
	}
	@Test void theHeelTurnIsJumpedAndReachesOnlySoFar(){
		assertTrue(SwordTombRules.heel(3,0,0));
		assertFalse(SwordTombRules.heel(3,0,1));
		assertFalse(SwordTombRules.heel(5,0,0));
		assertTrue(SwordTombRules.passed(180,0));
		assertFalse(SwordTombRules.passed(0,90));
		assertTrue(SwordTombRules.passed(0,180));
		assertTrue(SwordTombRules.passed(-90,90));
		assertFalse(SwordTombRules.passed(90,90));
	}
	@Test void theChargeHitsAheadButIsSteppedAsideFrom(){
		assertTrue(SwordTombRules.rampart(1,.5,0));
		assertFalse(SwordTombRules.rampart(1,2,0));
		assertFalse(SwordTombRules.rampart(3,0,0));
		assertFalse(SwordTombRules.rampart(-2,0,0));
	}
	@Test void itsOpeningsAndBlowsWeighAsTheyShould(){
		assertEquals(1.5,SwordTombRules.multiplier(SwordTombRules.WALLED,true,false));
		assertEquals(1.5,SwordTombRules.multiplier(SwordTombRules.LOST,true,false));
		assertEquals(1.25,SwordTombRules.multiplier(SwordTombRules.VERDICT,true,false));
		assertEquals(.5,SwordTombRules.multiplier(SwordTombRules.RITE,true,true));
		assertTrue(SwordTombRules.damage(SwordTombRules.VERDICT)>SwordTombRules.damage(SwordTombRules.SWEEP));
		assertTrue(SwordTombRules.damage(SwordTombRules.RITE)<SwordTombRules.damage(SwordTombRules.SWEEP));
	}
}
