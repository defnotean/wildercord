package dev.wildercord.cast;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireBloodRulesTest {
	@Test
	void blastsOfEnemiesThatStandTogetherAreMergedIntoOne() {
		// Four enemies within a block of each other: one blast, not four.
		List<Vec3> pack = List.of(new Vec3(0, 0, 0), new Vec3(0.5, 0, 0), new Vec3(0, 0, 0.7), new Vec3(1, 0, 0.5));
		assertEquals(1, FireBloodRules.clusterCentres(pack, 3.5 * FireBloodRules.BLAST_MERGE, FireBloodRules.MAX_BLASTS).size());
	}

	@Test
	void farApartEnemiesKeepTheirOwnBlastsUpToTheCap() {
		List<Vec3> spread = List.of(new Vec3(0, 0, 0), new Vec3(10, 0, 0), new Vec3(20, 0, 0), new Vec3(30, 0, 0), new Vec3(40, 0, 0));
		assertEquals(FireBloodRules.MAX_BLASTS, FireBloodRules.clusterCentres(spread, 2.8, FireBloodRules.MAX_BLASTS).size());
		assertEquals(FireBloodRules.MAX_METEORS, FireBloodRules.clusterCentres(spread, 2.8, FireBloodRules.MAX_METEORS).size());
	}

	@Test
	void anEnemyTakesOnlyTheStrongestBlastOfACast() {
		Map<UUID, Double> landed = new HashMap<>();
		UUID enemy = UUID.randomUUID();
		assertEquals(10, FireBloodRules.unstacked(landed, enemy, 10), 1e-9);
		// A weaker blast adds nothing, a stronger one only the difference.
		assertEquals(0, FireBloodRules.unstacked(landed, enemy, 7), 1e-9);
		assertEquals(2, FireBloodRules.unstacked(landed, enemy, 12), 1e-9);
		assertEquals(0, FireBloodRules.unstacked(landed, enemy, 12), 1e-9);
		// Someone else is untouched by all that.
		assertEquals(5, FireBloodRules.unstacked(landed, UUID.randomUUID(), 5), 1e-9);
	}

	@Test
	void fourBunchedEnemiesTakeOneBlastEachNotFour() {
		// The old rule: four blasts of 12 falling off to 60% over 3.5 blocks, each hitting all four.
		double old = 12 + 3 * 12 * 0.85;
		Map<UUID, Double> landed = new HashMap<>();
		UUID enemy = UUID.randomUUID();
		double total = 0;
		for (int blast = 0; blast < 4; blast++) {
			total += FireBloodRules.unstacked(landed, enemy, blast == 0 ? 12 : 12 * 0.85);
		}
		assertEquals(12, total, 1e-9);
		assertTrue(old > 40);
	}

	@Test
	void cleaveCutsInProportionToTheTargetUpToACap() {
		assertEquals(8, FireBloodRules.cleaveDamage(20), 1e-9);
		assertEquals(16, FireBloodRules.cleaveDamage(100), 1e-9);
		assertEquals(26, FireBloodRules.cleaveDamage(300), 1e-9);
		assertEquals(26, FireBloodRules.cleaveDamage(3000), 1e-9);
	}

	@Test
	void theRitePriceGrowsWithTheSpellsPower() {
		assertEquals(3, FireBloodRules.sanguinePrice(0, 0, 1));
		assertEquals(4, FireBloodRules.sanguinePrice(1, 0, 1));
		assertEquals(5, FireBloodRules.sanguinePrice(0, 1, 1));
		assertEquals(4, FireBloodRules.sanguinePrice(0, 0, 5));
		assertEquals(3, FireBloodRules.sanguinePrice(0, 0, 4));
	}

	@Test
	void overdriveHitsHarderTheWeakerItLeavesYou() {
		assertEquals(1, FireBloodRules.painStrength(20, 20, 0));
		assertEquals(1, FireBloodRules.painStrength(10, 20, 0));
		assertEquals(2, FireBloodRules.painStrength(9, 20, 0));
		assertEquals(3, FireBloodRules.painStrength(4, 20, 0));
		// Never past Strength IV.
		assertEquals(3, FireBloodRules.painStrength(2, 20, 2));
		assertEquals(2, FireBloodRules.painStrength(20, 20, 1));
	}

	@Test
	void heartstopperSkipsEscalateToAFullStop() {
		assertEquals(5, FusedStormNumbers.heartStun(0));
		assertEquals(10, FusedStormNumbers.heartStun(1));
		assertEquals(30, FusedStormNumbers.heartStun(2));
		assertEquals(30, FusedStormNumbers.heartStun(7));
	}
}
