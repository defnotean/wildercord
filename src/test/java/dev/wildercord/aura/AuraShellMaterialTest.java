package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuraShellMaterialTest {
	@Test
	void reducedFlashKeepsFundedShellVisibleWithoutPulseOrHitFlare() {
		for (float pulse : new float[] {-1, -.5F, 0, .5F, 1}) {
			for (float age : new float[] {-1, 0, .5F, 2, 4, 7.5F, 8, 99}) {
				assertEquals(0x4D4775A3, AuraShellMaterial.argb(0x336699, pulse, age, true),
					"Reduced flash keeps 30% opacity and the original 10% white tint at every phase");
			}
		}
	}

	@Test
	void reducedFlashPreservesAuraColourAndOriginalRestingMaterial() {
		for (int color : new int[] {0x000000, 0xFFFFFF, 0xFF0000, 0x00FF00, 0x0000FF, 0xA45AE0}) {
			int reduced = AuraShellMaterial.argb(color, 1, 0, true);
			assertEquals(AuraShellMaterial.argb(color, 0, 99, false), reduced);
			assertEquals(77, reduced >>> 24, "Reducing flash never disables a funded shell");
		}
		assertEquals(0x4DFF1A1A, AuraShellMaterial.argb(0xFF0000, 1, 0, true));
		assertEquals(0x4D1AFF1A, AuraShellMaterial.argb(0x00FF00, 1, 0, true));
		assertEquals(0x4D1A1AFF, AuraShellMaterial.argb(0x0000FF, 1, 0, true));
	}

	@Test
	void normalModeRetainsOriginalPulseAndHitMaterial() {
		assertEquals(0x3B4775A3, AuraShellMaterial.argb(0x336699, -1, 99, false));
		assertEquals(0x4D4775A3, AuraShellMaterial.argb(0x336699, 0, 99, false));
		assertEquals(0x5E4775A3, AuraShellMaterial.argb(0x336699, 1, 99, false));
		assertEquals(0xB385A3C2, AuraShellMaterial.argb(0x336699, 0, 0, false));
		assertEquals(0x80668CB3, AuraShellMaterial.argb(0x336699, 0, 4, false));
	}

	@Test
	void normalHitFlareEndsAtEightTicksAndIgnoresFutureHits() {
		int resting = AuraShellMaterial.argb(0x336699, .5F, 99, false);
		assertEquals(resting, AuraShellMaterial.argb(0x336699, .5F, -1, false));
		assertEquals(resting, AuraShellMaterial.argb(0x336699, .5F, 8, false));
		assertEquals(resting, AuraShellMaterial.argb(0x336699, .5F, 9, false));
		assertNotEquals(resting, AuraShellMaterial.argb(0x336699, .5F, 7, false));
	}
}
