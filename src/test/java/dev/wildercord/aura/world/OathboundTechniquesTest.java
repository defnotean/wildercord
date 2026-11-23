package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the expanded Oathbound move sheets: thirteen extra techniques per knight, fifty-two in total, each with a legal
 * timing, a positive volume, and a unique translation key. Also pins the pure geometry contract that the shared executor
 * and the client pose vocabulary both depend on.
 */
final class OathboundTechniquesTest {
	@Test
	void eachKnightDeclaresThirteenDistinctExtraTechniques() {
		Set<String> keys = new HashSet<>();
		int total = 0;
		total += assertKnight("Bastion", OathboundBastionRules.COMBAT_MOVES, OathboundBastionRules::technique, keys);
		total += assertKnight("BladeChampion", OathboundBladeChampionRules.COMBAT_MOVES,
				OathboundBladeChampionRules::technique, keys);
		total += assertKnight("Halberdier", OathboundHalberdierRules.COMBAT_MOVES,
				OathboundHalberdierRules::technique, keys);
		total += assertKnight("BannerMarshal", OathboundBannerMarshalRules.COMBAT_MOVES,
				OathboundBannerMarshalRules::technique, keys);
		assertEquals(52, total, "fifty-two extra techniques across the four knights");
	}

	private static int assertKnight(String name, int[] moves, IntFunction<OathboundTechnique> lookup, Set<String> keys) {
		int count = 0;
		Set<Integer> ids = new HashSet<>();
		for (int move : moves) {
			OathboundTechnique t = lookup.apply(move);
			if (t == null) {
				continue;
			}
			count++;
			assertEquals(move, t.id(), name + " lookup id");
			assertTrue(ids.add(move), name + " duplicate id " + move);
			assertTrue(keys.add(t.key()), "duplicate key " + t.key());
			assertTrue(t.tell() >= 4, name + " " + t.key() + " tell");
			assertTrue(t.active() >= 1, name + " " + t.key() + " active window");
			assertTrue(t.recovery() >= 1, name + " " + t.key() + " recovery");
			assertTrue(t.duration() > t.tell(), name + " " + t.key() + " duration");
			assertTrue(t.hitDelay() >= 0 && t.hitDelay() <= t.active(), name + " " + t.key() + " hit delay");
			assertTrue(t.damage() > 0.0, name + " " + t.key() + " damage");
			assertTrue(t.reach() > 0.0, name + " " + t.key() + " reach");
			assertTrue(t.knockback() >= 0.0, name + " " + t.key() + " knockback");
			switch (t.shape()) {
				case CONE, WIDE_ARC, MULTI_HIT -> assertTrue(t.half() > 0.0 && t.half() <= 180.0,
						name + " " + t.key() + " arc");
				case LINE, PULL_LINE, TWIN_LINE -> assertTrue(t.half() > 0.0, name + " " + t.key() + " width");
				case RING, CROSS, ORBITAL -> assertTrue(t.inner() >= 0.0 && t.inner() < t.reach(),
						name + " " + t.key() + " inner radius");
				case RADIUS, PULL_RADIUS -> assertTrue(t.reach() > 0.0, name + " " + t.key() + " radius");
			}
			if (t.effect() != OathboundTechnique.Effect.NONE && t.effect() != OathboundTechnique.Effect.LAUNCH) {
				assertTrue(t.effectTicks() > 0, name + " " + t.key() + " effect duration");
			}
			if (t.shape() == OathboundTechnique.Shape.MULTI_HIT) {
				assertTrue(t.hits() >= 2, name + " " + t.key() + " multi-hit count");
			}
		}
		assertEquals(13, count, name + " extra techniques");
		return count;
	}

	@Test
	void coneLineRingAndCrossGeometryMatchTheirVolumes() {
		OathboundTechnique cone = OathboundBastionRules.technique(OathboundBastionRules.MACE_OVERHEAD);
		assertTrue(OathboundCombat.inVolume(cone, 0.0F, 0.0, 2.5));
		assertFalse(OathboundCombat.inVolume(cone, 0.0F, 2.5, 0.0));
		assertFalse(OathboundCombat.inVolume(cone, 0.0F, 0.0, 3.5));

		OathboundTechnique line = OathboundBastionRules.technique(OathboundBastionRules.SEISMIC_LINE);
		assertTrue(OathboundCombat.inVolume(line, 0.0F, 0.0, 5.0));
		assertTrue(OathboundCombat.inVolume(line, 0.0F, 1.0, 5.0));
		assertFalse(OathboundCombat.inVolume(line, 0.0F, 2.0, 5.0));
		assertFalse(OathboundCombat.inVolume(line, 0.0F, 0.0, -5.0));

		OathboundTechnique ring = OathboundBastionRules.technique(OathboundBastionRules.EARTHSPLIT_STOMP);
		assertFalse(OathboundCombat.inVolume(ring, 0.0F, 0.0, 0.5));
		assertTrue(OathboundCombat.inVolume(ring, 0.0F, 0.0, 3.0));
		assertFalse(OathboundCombat.inVolume(ring, 0.0F, 0.0, 6.0));

		OathboundTechnique cross = OathboundBastionRules.technique(OathboundBastionRules.SUN_ANVIL_JUDGMENT);
		assertTrue(OathboundCombat.inVolume(cross, 0.0F, 0.0, 0.0));
		assertTrue(OathboundCombat.inVolume(cross, 0.0F, 0.0, 5.0));
		assertTrue(OathboundCombat.inVolume(cross, 0.0F, 5.0, 0.0));
		assertFalse(OathboundCombat.inVolume(cross, 0.0F, 5.0, 5.0));
	}

	@Test
	void twinLinesAndLongBeamsReachWhereTheyShould() {
		OathboundTechnique twin = OathboundBladeChampionRules.technique(OathboundBladeChampionRules.PHANTOM_STRIKE);
		assertTrue(OathboundCombat.inVolume(twin, 0.0F, 0.0, 4.0));
		assertTrue(OathboundCombat.inVolume(twin, 0.0F, 0.0, -4.0));
		assertFalse(OathboundCombat.inVolume(twin, 0.0F, 0.0, 5.0));

		OathboundTechnique beam = OathboundBannerMarshalRules.technique(OathboundBannerMarshalRules.JUDGMENT_BEAM);
		assertTrue(OathboundCombat.inVolume(beam, 0.0F, 0.0, 9.0));
		assertFalse(OathboundCombat.inVolume(beam, 0.0F, 1.0, 9.0));

		OathboundTechnique fan = OathboundHalberdierRules.technique(OathboundHalberdierRules.GLAIVE_SWEEP);
		assertTrue(OathboundCombat.inVolume(fan, 0.0F, 3.0, 3.0));
		assertFalse(OathboundCombat.inVolume(fan, 0.0F, 4.0, -1.0));
	}
}
