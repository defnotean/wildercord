package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SerpentRulesTest {
	@Test void onlyAPlayerOnTheIslandNearItsCoilsWakesIt(){
		assertTrue(SerpentRules.wakes(5,0,5));
		assertTrue(SerpentRules.wakes(0,4,7));
		assertFalse(SerpentRules.wakes(6,0,6));
		assertFalse(SerpentRules.wakes(0,8,2));
		assertFalse(SerpentRules.wakes(0,-7,2));
	}
	@Test void itIsPutBackOnlyWhenItLeavesTheCavern(){
		assertFalse(SerpentRules.leashed(15,0,15));
		assertTrue(SerpentRules.leashed(17,0,17));
		assertTrue(SerpentRules.leashed(0,-8,0));
		assertTrue(SerpentRules.leashed(0,12,0));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(SerpentRules.scripted(SerpentRules.DORMANT));
		assertTrue(SerpentRules.scripted(SerpentRules.UNCOIL));
		assertTrue(SerpentRules.scripted(SerpentRules.RECOIL));
		assertFalse(SerpentRules.scripted(SerpentRules.WATCH));
	}
	@Test void theUncoilRunsForwardAndTheRecoilRunsItBack(){
		assertEquals(0,SerpentRules.uncoil(SerpentRules.DORMANT,30));
		assertEquals(.5F,SerpentRules.uncoil(SerpentRules.UNCOIL,SerpentRules.UNCOIL_TICKS/2F),1e-6);
		assertEquals(1,SerpentRules.uncoil(SerpentRules.UNCOIL,999));
		assertEquals(1,SerpentRules.uncoil(SerpentRules.RECOIL,0));
		assertEquals(0,SerpentRules.uncoil(SerpentRules.RECOIL,SerpentRules.UNCOIL_TICKS));
		assertEquals(1,SerpentRules.uncoil(SerpentRules.WATCH,0));
	}
	@Test void itWakesBeforeItCouldReachYouAndFightsInsideItsLeash(){
		assertTrue(SerpentRules.WAKE_RADIUS>=SerpentSpine.COIL_FOOT_R+3);
		assertTrue(SerpentRules.LAND_FAR<SerpentRules.ARENA && SerpentRules.ARENA<SerpentRules.LEASH);
		assertTrue(SerpentRules.BAR_SHOW<SerpentRules.BAR_DROP);
		assertTrue(SerpentRules.RETURN_TICKS<SerpentRules.RESET_TICKS);
	}
	@Test void itBitesUpCloseLungesFromFurtherOutAndClosesFromBeyond(){
		assertEquals(SerpentRules.STRIKE,SerpentRules.choose(3,.9,true,true));
		assertEquals(SerpentRules.CONSTRICT,SerpentRules.choose(3,.1,true,true));
		assertEquals(SerpentRules.STRIKE,SerpentRules.choose(3,.1,false,true));
		assertEquals(SerpentRules.LUNGE,SerpentRules.choose(7,.5,true,true));
		assertEquals(SerpentRules.TAIL_SLAM,SerpentRules.choose(7,.2,true,true));
		assertEquals(SerpentRules.LUNGE,SerpentRules.choose(7,.2,true,false));
		assertEquals(SerpentRules.TAIL_SLAM,SerpentRules.choose(12,.5,true,true));
		assertEquals(-1,SerpentRules.choose(12,.5,true,false));
		assertEquals(-1,SerpentRules.choose(16,.5,true,true));
	}
	@Test void aBiteReachesAheadAndNotBehindOrWide(){
		assertTrue(SerpentRules.hits(SerpentRules.STRIKE,4,0,1));
		assertTrue(SerpentRules.hits(SerpentRules.STRIKE,5,0,3));
		assertFalse(SerpentRules.hits(SerpentRules.STRIKE,6,0,1));
		assertFalse(SerpentRules.hits(SerpentRules.STRIKE,3,2,1));
		assertFalse(SerpentRules.hits(SerpentRules.STRIKE,-2,0,1));
		assertTrue(SerpentRules.hits(SerpentRules.LUNGE,1,-1,0));
		assertFalse(SerpentRules.hits(SerpentRules.WATCH,1,0,0));
	}
	@Test void itsScalesTurnBlowsFromTheFrontAndItsBellyDoesNot(){
		assertTrue(SerpentRules.frontal(0) && SerpentRules.frontal(-50) && SerpentRules.frontal(410));
		assertFalse(SerpentRules.frontal(90) || SerpentRules.frontal(180) || SerpentRules.frontal(-120));
		assertTrue(SerpentRules.scales(SerpentRules.WATCH,true)<SerpentRules.scales(SerpentRules.WATCH,false));
		assertTrue(SerpentRules.scales(SerpentRules.RECOVER,true)>1);
		assertTrue(SerpentRules.scales(SerpentRules.SUBMERGED,false)<.5);
	}
	@Test void itSlipsOnlyFrontalBlowsWhileWatchingAndNotTwiceInARow(){
		assertTrue(SerpentRules.slips(SerpentRules.WATCH,true,SerpentRules.SIDESTEP_REST));
		assertFalse(SerpentRules.slips(SerpentRules.WATCH,true,SerpentRules.SIDESTEP_REST-1));
		assertFalse(SerpentRules.slips(SerpentRules.WATCH,false,999));
		assertFalse(SerpentRules.slips(SerpentRules.RECOVER,true,999));
	}
	@Test void itDivesOnceAtHalfHealth(){
		assertFalse(SerpentRules.dives(200,360,false));
		assertTrue(SerpentRules.dives(180,360,false));
		assertFalse(SerpentRules.dives(100,360,true));
	}
	@Test void itsBlowsSitBelowAMastersAndItsOpeningsOutlastItsTells(){
		assertTrue(SerpentRules.STRIKE_DAMAGE<MastersRules.Move.SWEEP.damage);
		assertTrue(SerpentRules.STRIKE_TELL<=4 && SerpentRules.STRIKE_CHAIN_TELL>=3);
		assertTrue(SerpentRules.STRIKE_RECOVER>SerpentRules.STRIKE_TELL*2 && SerpentRules.LUNGE_RECOVER>SerpentRules.LUNGE_TELL*2);
		assertTrue(SerpentRules.flurry(0,false)<=SerpentRules.FLURRY && SerpentRules.flurry(.9,true)>=2);
		assertTrue(SerpentRules.RING-SerpentRules.LUNGE_REACH>0 && SerpentRules.RING<SerpentRules.ARENA);
		assertEquals("squeeze",SerpentRules.key(SerpentRules.SQUEEZE));
	}
	@Test void aGuardiansBiteGoesPartlyThroughDefences(){
		assertEquals(20,GuardianBite.pierced(20,20,.45),1e-6);
		assertEquals(10+10*.45,GuardianBite.pierced(20,10,.45),1e-4);
		assertEquals(10,GuardianBite.pierced(20,10,0),1e-6);
		assertEquals(10,GuardianBite.pierce(20,10),1e-6);
	}
	@Test void theSlamsClodsFlyChestHighAcrossItsReach(){
		for(double d=SerpentRules.SLAM_AHEAD;d<=SerpentRules.SLAM_REACH-SerpentRules.SLAM_AHEAD;d+=.5){
			double h=SerpentRules.DEBRIS_FROM+SerpentRules.rise(d);
			assertTrue(h>0.3 && h<1.8,"a clod is "+h+" up after "+d+" blocks");
		}
		assertTrue((SerpentRules.SLAM_REACH)/SerpentRules.DEBRIS_SPEED<SerpentRules.DEBRIS_LIFE);
		assertEquals("tail_slam",SerpentRules.key(SerpentRules.TAIL_SLAM));
	}
	@Test void theSlamGetsThroughFullNetheriteProtectionFour(){
		// Netherite (20 armour, 12 toughness) and Protection IV on all four pieces, the way vanilla works them out.
		double dmg=SerpentRules.DEBRIS_DAMAGE;
		double armour=Math.min(20,Math.max(20*.2,20-dmg/(2+12/4.0)))/25;
		double after=dmg*(1-armour)*(1-Math.min(20,16)/25.0);
		double through=GuardianBite.pierced((float)dmg,(float)after,SerpentRules.SLAM_PIERCE);
		assertTrue(through>=dmg*.6,"only "+through+" of "+dmg+" gets through");
		assertTrue(SerpentRules.SLAM_PIERCE>SerpentRules.PIERCE);
	}
	@Test void theCoilrootQuickensTheBreathingStanceAndNothingElse(){
		assertTrue(SerpentRules.COILROOT_BREATH>1);
		assertEquals(2*SerpentRules.COILROOT_BREATH,SerpentRules.coilroot("stance",2),1e-9);
		assertEquals(2*SerpentRules.COILROOT_BREATH,SerpentRules.coilroot("beat",2),1e-9);
		assertEquals(2,SerpentRules.coilroot("hit",2),1e-9);
		assertEquals(SerpentRules.COILROOT_TRICKLE,SerpentRules.coilroot("coilroot",SerpentRules.COILROOT_TRICKLE),1e-9);
		assertTrue(SerpentRules.COILROOT_TRICKLE>0&&SerpentRules.COILROOT_GUARD>=SerpentRules.WAKE_RADIUS);
	}
}
