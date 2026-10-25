package dev.wildercord.client.fx;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reduced flash and the vanilla screen-effect setting together. The drawn effects themselves have native gates. */
class ScreenEffectsScaleTest {
	@Test void reducedFlashCapsTheSettingButNeverRaisesIt() {
		assertEquals(1, ScreenEffects.scale(false, 1));
		assertEquals(0.15F, ScreenEffects.scale(true, 1));
		assertEquals(0.1F, ScreenEffects.scale(true, 0.1F));
		assertEquals(0, ScreenEffects.scale(true, 0));
		assertEquals(0, ScreenEffects.scale(false, 0));
	}

	@Test void theStrainVignetteKeepsItsFloorUntilTheSettingIsOff() {
		assertEquals(0, ScreenEffects.strainSoften(0));
		assertEquals(0.4F, ScreenEffects.strainSoften(0.05F));
		assertEquals(0.4F, ScreenEffects.strainSoften(ScreenEffects.scale(true, 1)));
		assertEquals(0.8F, ScreenEffects.strainSoften(0.8F));
	}
}
