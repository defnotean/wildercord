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
}
