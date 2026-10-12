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
}
