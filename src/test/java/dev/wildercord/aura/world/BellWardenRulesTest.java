package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BellWardenRulesTest {
	@Test void aPlayerHalfwayUpTheNaveWakesItAndOneAtTheDoorDoesNot(){
		assertTrue(BellWardenRules.wakes(0,0,-17));
		assertTrue(BellWardenRules.wakes(8,2,-12));
		assertFalse(BellWardenRules.wakes(0,0,-30));
		assertFalse(BellWardenRules.wakes(0,7,-4));
	}
	@Test void theHallIsABoxWidenedByItsMargin(){
		assertTrue(BellWardenRules.within(5.5,8,20,4,7,19,26,21,65,0));
		assertTrue(BellWardenRules.within(26.9,8,65.9,4,7,19,26,21,65,0));
		assertFalse(BellWardenRules.within(27.1,8,40,4,7,19,26,21,65,0));
		assertTrue(BellWardenRules.within(28.5,8,40,4,7,19,26,21,65,2));
		assertFalse(BellWardenRules.within(10,8,16.5,4,7,19,26,21,65,2));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(BellWardenRules.scripted(BellWardenRules.DORMANT));
		assertTrue(BellWardenRules.scripted(BellWardenRules.RISE));
		assertTrue(BellWardenRules.scripted(BellWardenRules.KNEEL));
		assertFalse(BellWardenRules.scripted(BellWardenRules.WATCH));
	}
	@Test void theRiseRunsForwardAndTheKneelRunsItBack(){
		assertEquals(0,BellWardenRules.rise(BellWardenRules.DORMANT,30));
		assertEquals(.5F,BellWardenRules.rise(BellWardenRules.RISE,BellWardenRules.RISE_TICKS/2F),1e-6);
		assertEquals(1,BellWardenRules.rise(BellWardenRules.RISE,999));
		assertEquals(1,BellWardenRules.rise(BellWardenRules.KNEEL,0));
		assertEquals(0,BellWardenRules.rise(BellWardenRules.KNEEL,BellWardenRules.RISE_TICKS));
		assertEquals(1,BellWardenRules.rise(BellWardenRules.WATCH,0));
	}
	@Test void itShowsItsBarBeforeYouCanReachItAndForgetsOnlyAfterKneeling(){
		assertTrue(BellWardenRules.LEASH<BellWardenRules.BAR_SHOW && BellWardenRules.BAR_SHOW<BellWardenRules.BAR_DROP);
		assertTrue(BellWardenRules.RETURN_TICKS<BellWardenRules.RESET_TICKS);
		assertTrue(BellWardenRules.TOLL_AT>0 && BellWardenRules.TOLL_AT<BellWardenRules.RISE_TICKS);
		assertFalse(BellWardenRules.forgets(BellWardenRules.RESET_TICKS));assertTrue(BellWardenRules.forgets(BellWardenRules.RESET_TICKS+1));
		assertEquals("watch",BellWardenRules.key(BellWardenRules.WATCH));
	}
	@Test void upCloseItSwingsOrPoundsAndItChargesOnlyThoseKeepingAway(){
		assertEquals(BellWardenRules.SWING,BellWardenRules.choose(2,.9,true,true));
		assertEquals(BellWardenRules.SLAM,BellWardenRules.choose(2,.1,true,true));
		assertEquals(BellWardenRules.SWING,BellWardenRules.choose(2,.1,false,true));
		assertEquals(BellWardenRules.SLAM,BellWardenRules.choose(5,.9,true,true));
		assertEquals(BellWardenRules.CHARGE,BellWardenRules.choose(9,.9,true,true));
		assertEquals(-1,BellWardenRules.choose(9,.9,true,false));
		assertEquals(-1,BellWardenRules.choose(20,.9,true,true));
	}
	@Test void theSwingIsWideTheHammerNarrowAndTheSlamLandsAhead(){
		assertTrue(BellWardenRules.hits(BellWardenRules.SWING,3,2.5,1));
		assertFalse(BellWardenRules.hits(BellWardenRules.HAMMER,3,2.5,1));
		assertTrue(BellWardenRules.hits(BellWardenRules.HAMMER,3,1,1));
		assertTrue(BellWardenRules.hits(BellWardenRules.SLAM,BellWardenRules.SLAM_AHEAD+2,1,0));
		assertFalse(BellWardenRules.hits(BellWardenRules.SLAM,-2,0,0));
		assertFalse(BellWardenRules.hits(BellWardenRules.SLAM,BellWardenRules.SLAM_AHEAD,0,3));
		assertFalse(BellWardenRules.hits(BellWardenRules.WATCH,1,0,1));
	}
	@Test void theShellTakesAlmostEverythingAndTheOpeningLetsMoreThrough(){
		assertTrue(BellWardenRules.armour(BellWardenRules.SHELL,false)<.25);
		assertTrue(BellWardenRules.armour(BellWardenRules.RECOVER,true)>1);
		assertTrue(BellWardenRules.armour(BellWardenRules.WATCH,true)<BellWardenRules.armour(BellWardenRules.WATCH,false));
		assertTrue(BellWardenRules.frontal(30));assertTrue(BellWardenRules.frontal(-390));assertFalse(BellWardenRules.frontal(150));
	}
	@Test void itShellsOnlyWhenHurtFastAndNotTwiceInARow(){
		assertTrue(BellWardenRules.shells(BellWardenRules.WATCH,BellWardenRules.SHELL_AFTER,BellWardenRules.SHELL_REST));
		assertFalse(BellWardenRules.shells(BellWardenRules.WATCH,BellWardenRules.SHELL_AFTER-1,BellWardenRules.SHELL_REST));
		assertFalse(BellWardenRules.shells(BellWardenRules.WATCH,BellWardenRules.SHELL_AFTER,BellWardenRules.SHELL_REST-1));
		assertFalse(BellWardenRules.shells(BellWardenRules.SWING,99,999));
		assertTrue(BellWardenRules.dodges(BellWardenRules.WATCH,true,BellWardenRules.BACKSTEP_REST,.1));
		assertFalse(BellWardenRules.dodges(BellWardenRules.WATCH,false,BellWardenRules.BACKSTEP_REST,.1));
		assertFalse(BellWardenRules.dodges(BellWardenRules.SWING,true,999,.1));
		assertTrue(BellWardenRules.hammers(.9,true));assertFalse(BellWardenRules.hammers(.9,false));
		assertEquals("shell",BellWardenRules.key(BellWardenRules.SHELL));
	}
}
