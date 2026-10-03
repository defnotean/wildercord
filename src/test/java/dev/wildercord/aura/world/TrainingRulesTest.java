package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrainingRulesTest {
	@Test void waterfallNeedsARealDropAndDryFooting() {
		assertTrue(TrainingRules.waterfall(6, true, true, true));
		assertFalse(TrainingRules.waterfall(5, true, true, true));
		assertFalse(TrainingRules.waterfall(6, false, true, true));
		assertFalse(TrainingRules.waterfall(6, true, false, true));
		assertFalse(TrainingRules.waterfall(6, true, true, false));
	}
	@Test void summitNeedsAltitudeExposureAndSurroundingRelief() {
		int[] top = {130, 130, 127, 127, 120, 120, 120, 120};
		assertTrue(TrainingRules.summit(130, 63, true, true, true, top));
		assertFalse(TrainingRules.summit(126, 63, true, true, true, top));
		assertFalse(TrainingRules.summit(130, 63, false, true, true, top));
		assertFalse(TrainingRules.summit(130, 63, true, false, true, top));
		assertFalse(TrainingRules.summit(130, 63, true, true, false, top));
		assertFalse(TrainingRules.summit(130, 63, true, true, true, new int[]{130}));
		assertFalse(TrainingRules.summit(130, 63, true, true, true, new int[]{130,130,130,130,130,130,130,130}));
		assertFalse(TrainingRules.summit(130, 63, true, true, true, new int[]{140,120,120,120,120,120,120,120}));
	}
	@Test void TerrainPracticeSharesTheLifetimeCap() {
		assertEquals(TrainingRules.PRACTICE, AuraRules.practice(0, TrainingRules.PRACTICE / AuraRules.PRACTICE_RATE));
		assertEquals(0, AuraRules.practice(AuraRules.PRACTICE_CAP, TrainingRules.PRACTICE / AuraRules.PRACTICE_RATE));
	}
}
