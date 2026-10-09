package dev.wildercord.aura.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildercord.aura.TechniqueRules;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure rules for the Rime, Thunder, Verdant and Hollow Sword Masters (masters-a pack). */
class ElementalMastersRulesTest {
	private static final int[] SCHOOLS = {ElementalMasters.RIME, ElementalMasters.THUNDER, ElementalMasters.VERDANT, ElementalMasters.HOLLOW};
	private static final MastersRules.Move[] SIGNATURES = {MastersRules.Move.RIME_LATTICE, MastersRules.Move.THUNDER_CHAIN,
		MastersRules.Move.VERDANT_BLOOM, MastersRules.Move.HOLLOW_PULL};

	@Test
	void schoolsRoundTripThroughEveryLookup() {
		assertEquals(List.of(3, 4, 5, 6), ElementalMasters.SCHOOL_IDS);
		assertEquals(MastersRules.SCHOOLS, MethodsBMasters.VENOM + 1); // ---- methods-b pack: Echo, Dawn and Venom (13-15) are the last schools
		Set<String> names = new HashSet<>();
		for (int i = 0; i < SCHOOLS.length; i++) {
			int school = SCHOOLS[i];
			assertTrue(ElementalMasters.owns(school));
			assertTrue(MastersRules.knownSchool(school));
			assertEquals(school, MastersRules.discipline(school), "an owned school is never clamped to Stone");
			assertTrue(names.add(ElementalMasters.name(school)));
			assertEquals(school, ElementalMasters.schoolOf(ElementalMasters.method(school)));
			assertEquals(ElementalMasters.name(school) + "_lesson", ElementalMasters.lesson(school));
			assertEquals(SIGNATURES[i], ElementalMasters.signatureMove(school));
			assertEquals(school, ElementalMasters.schoolOf(SIGNATURES[i]));
			assertTrue(ElementalMasters.signature(SIGNATURES[i]));
			assertEquals("message.wildercord.master." + ElementalMasters.name(school) + "_hint", ElementalMasters.hintKey(SIGNATURES[i]));
		}
		for (int school : new int[] {Integer.MIN_VALUE, -1, MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE, 7, Integer.MAX_VALUE}) {
			assertFalse(ElementalMasters.owns(school));
			assertNull(ElementalMasters.lesson(school));
		}
		for (var move : MastersRules.Move.values()) {
			if (List.of(SIGNATURES).contains(move)) continue;
			assertFalse(ElementalMasters.signature(move), move.name());
			assertEquals(-1, ElementalMasters.schoolOf(move));
			assertNull(ElementalMasters.hintKey(move));
		}
		assertEquals(Set.of(MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE), Set.of(MastersRules.discipline(-5), MastersRules.discipline(1),
			MastersRules.discipline(99)), "outsiders still clamp into the established three");
	}

	@Test
	void signatureWireIdsAndAnimationIdsAgree() {
		int[] animation = {ElementalMasterAnimation.RIME_LATTICE, ElementalMasterAnimation.THUNDER_CHAIN,
			ElementalMasterAnimation.VERDANT_BLOOM, ElementalMasterAnimation.HOLLOW_PULL};
		for (int i = 0; i < SIGNATURES.length; i++) {
			assertEquals(12 + i, SIGNATURES[i].ordinal() + 1);
			assertEquals(12 + i, LegacyMasterMoves.wireId(SIGNATURES[i]));
			assertEquals(12 + i, animation[i]);
			assertTrue(ElementalMasterAnimation.owns(animation[i]));
		}
		assertFalse(ElementalMasterAnimation.owns(11));
		assertFalse(ElementalMasterAnimation.owns(16));
	}

	@Test
	void atLeastTwentyFiveNamedTechniquesPerSchoolWithReadableTellsAndRecoveries() {
		Set<String> keys = new HashSet<>();
		MasterTechniques.all().forEach(technique -> keys.add(technique.key()));
		assertEquals(MasterTechniques.all().size(), keys.size(), "every technique key is unique across all schools");
		for (int school : SCHOOLS) {
			var techniques = MasterTechniques.forSchool(school);
			assertTrue(techniques.size() >= 25, ElementalMasters.name(school) + " has " + techniques.size());
			Set<String> shapes = new HashSet<>();
			for (var technique : techniques) {
				assertEquals(school, technique.school());
				int previous = 0;
				for (int i = 0; i < technique.strikes().size(); i++) {
					int tell = technique.impact(i) - previous;
					assertTrue(tell >= ElementalMasters.MIN_TELL && tell <= ElementalMasters.MAX_TELL, technique.key() + " tell " + tell);
					assertEquals(technique.strikes().get(i).tell(), tell);
					previous = technique.impact(i);
				}
				assertTrue(technique.recovery() >= ElementalMasters.MIN_RECOVERY && technique.recovery() <= ElementalMasters.MAX_RECOVERY,
					technique.key() + " recovery " + technique.recovery());
				double total = technique.strikes().stream().mapToDouble(MasterTechniques.Strike::damage).sum();
				assertTrue(total > 0 && total <= MasterTechniques.MAX_SHARE + 1e-9, technique.key() + " chain damage " + total);
				assertTrue(technique.cost() + MastersRules.GUARD_COST <= MastersRules.AURA_MAX, technique.key() + " is affordable with a guard");
				String shape = technique.strikes().stream().map(strike -> strike.primitive().name() + strike.mirrored()
					+ strike.spin() + strike.step() + strike.quick() + strike.heavy()).toList().toString();
				assertTrue(shapes.add(shape), technique.key() + " repeats another technique of its school");
			}
		}
	}

	@Test
	void tellAndRecoveryFormulasStayInsideTheBand() {
		for (boolean heavy : new boolean[] {false, true})
			for (boolean spin : new boolean[] {false, true})
				for (boolean step : new boolean[] {false, true}) {
					int tell = ElementalMasters.tell(heavy, spin, step);
					assertTrue(tell >= 12 && tell <= 14);
				}
		assertEquals(12, ElementalMasters.tell(false, false, false));
		assertEquals(14, ElementalMasters.tell(true, true, true));
		for (int count = -3; count <= 9; count++) {
			int recovery = ElementalMasters.recovery(count);
			assertTrue(recovery >= 14 && recovery <= 16, "count " + count);
		}
		assertEquals(40, ElementalMasters.EXCHANGE_PAUSE);
		assertEquals(MastersRules.BREATH_TICKS, ElementalMasters.EXCHANGE_PAUSE);
	}

	@Test
	void openingOptionsSkipRecentTechniquesAndRespectReachAndAura() {
		for (int school : SCHOOLS) {
			var all = ElementalMasters.options(school, 0, MastersRules.AURA_MAX, Set.of());
			assertEquals(MasterTechniques.forSchool(school).size(), all.size(), "every technique opens point blank with full Aura");
			Set<Integer> recent = new HashSet<>();
			for (int i = 0; i < ElementalMasters.RECENT; i++) recent.add(all.get(i).id());
			var fresh = ElementalMasters.options(school, 0, MastersRules.AURA_MAX, recent);
			assertEquals(all.size() - ElementalMasters.RECENT, fresh.size());
			assertTrue(fresh.stream().noneMatch(technique -> recent.contains(technique.id())));
			assertTrue(ElementalMasters.options(school, 0, MastersRules.GUARD_COST + MastersRules.ATTACK_COST - .01, Set.of()).isEmpty(),
				"no technique without a guard in reserve");
			assertTrue(ElementalMasters.options(school, 100, MastersRules.AURA_MAX, Set.of()).isEmpty(), "nothing opens out of reach");
		}
	}

	@Test
	void signaturesAreFiniteConsentGatedAndCostAura() {
		for (var move : SIGNATURES) {
			int school = ElementalMasters.schoolOf(move);
			double cost = ElementalMasters.cost(move);
			assertTrue(cost > MastersRules.ATTACK_COST && cost + MastersRules.GUARD_COST <= MastersRules.AURA_MAX, move.name());
			assertEquals(cost, LegacyMasterMoves.auraCost(move, school));
			int[] beats = ElementalMasters.beats(move);
			assertTrue(beats.length >= 1);
			assertEquals(beats[beats.length - 1], ElementalMasters.lastEvent(move));
			assertTrue(beats[0] >= 20, move.name() + " is warned well before its first beat");
			for (int i = 1; i < beats.length; i++) assertTrue(beats[i] > beats[i - 1]);
			int end = ElementalMasters.end(move);
			assertTrue(end - beats[beats.length - 1] >= ElementalMasters.MIN_RECOVERY, move.name() + " leaves an open recovery");
			assertEquals(end, move.tell + move.recovery, "DATA_ATTACK clears exactly at the executor's end");
			assertTrue(ElementalMasters.cap(move) > 0 && ElementalMasters.cap(move) <= 30, move.name() + " per-challenger cap");
		}
		// Only the phrase's second slot, only once ready, only with Aura in hand.
		assertTrue(ElementalMasters.eligible(ElementalMasters.RIME, 1, 100, 10, 10, 26));
		assertTrue(ElementalMasters.eligible(ElementalMasters.RIME, 5, 100, 10, 10, 26));
		assertFalse(ElementalMasters.eligible(ElementalMasters.RIME, 2, 100, 10, 10, 26));
		assertFalse(ElementalMasters.eligible(ElementalMasters.RIME, 1, 100, 9, 10, 26));
		assertFalse(ElementalMasters.eligible(ElementalMasters.RIME, 1, 25, 10, 10, 26));
		assertFalse(ElementalMasters.eligible(ElementalMasters.RIME, 1, Double.NaN, 10, 10, 26));
		assertFalse(ElementalMasters.eligible(MastersRules.EMBER, 1, 100, 10, 10, 26));
		// Each signature only answers its own school, in its own band.
		assertTrue(RimeLatticeRules.eligible(ElementalMasters.RIME, 1, 4, 0, 100, 0, 0));
		assertFalse(RimeLatticeRules.eligible(ElementalMasters.THUNDER, 1, 4, 0, 100, 0, 0));
		assertFalse(RimeLatticeRules.eligible(ElementalMasters.RIME, 1, 1, 0, 100, 0, 0));
		assertFalse(RimeLatticeRules.eligible(ElementalMasters.RIME, 1, 4, 2, 100, 0, 0));
		assertTrue(ThunderChainRules.eligible(ElementalMasters.THUNDER, 1, 5, 0, 100, 0, 0));
		assertFalse(ThunderChainRules.eligible(ElementalMasters.THUNDER, 1, 9, 0, 100, 0, 0));
		assertTrue(VerdantBloomRules.eligible(ElementalMasters.VERDANT, 1, 4, 0, 100, 0, 0));
		assertFalse(VerdantBloomRules.eligible(ElementalMasters.HOLLOW, 1, 4, 0, 100, 0, 0));
		assertTrue(HollowPullRules.eligible(ElementalMasters.HOLLOW, 1, 4, 0, 100, 0, 0));
		assertFalse(HollowPullRules.eligible(ElementalMasters.HOLLOW, 1, 2, 0, 100, 0, 0));
		assertFalse(HollowPullRules.eligible(ElementalMasters.HOLLOW, 1, Double.NaN, 0, 100, 0, 0));
	}

	@Test
	void rimeLatticeFreezesThePlusAndCapsDamagePerChallenger() {
		assertArrayEquals(new int[] {20, 36, 52}, ElementalMasters.beats(MastersRules.Move.RIME_LATTICE));
		for (int pulse = 0; pulse < RimeLatticeRules.PULSES; pulse++) {
			assertEquals(pulse, RimeLatticeRules.freezingPulse(RimeLatticeRules.freezeAt(pulse)));
			assertEquals(pulse, RimeLatticeRules.markingPulse(RimeLatticeRules.markAt(pulse)));
			assertEquals(RimeLatticeRules.MARK, RimeLatticeRules.freezeAt(pulse) - RimeLatticeRules.markAt(pulse));
		}
		assertEquals(-1, RimeLatticeRules.freezingPulse(21));
		// Standing on the marked cell or an arm: frozen. Stepping diagonally or two cells away: safe.
		assertTrue(RimeLatticeRules.hits(.2, .2, .4, .4, 0));
		assertTrue(RimeLatticeRules.hits(.2, .2, 1.8, .4, 0));
		assertTrue(RimeLatticeRules.hits(.2, .2, .4, -1.2, 0));
		assertFalse(RimeLatticeRules.hits(.2, .2, 1.8, 1.8, 0), "a diagonal step leaves the plus");
		assertFalse(RimeLatticeRules.hits(.2, .2, 3.2, .4, 0), "two cells out is clear");
		assertFalse(RimeLatticeRules.hits(.2, .2, .4, .4, 1.0), "a jump clears the freeze");
		assertFalse(RimeLatticeRules.hits(.2, .2, Double.NaN, .4, 0));
		// Three freezes on a challenger who never moves still stop at the cap.
		double taken = 0;
		for (int pulse = 0; pulse < RimeLatticeRules.PULSES; pulse++) taken += RimeLatticeRules.damage(taken);
		assertEquals(RimeLatticeRules.CAP, taken, 1e-9);
		assertEquals(0, RimeLatticeRules.damage(RimeLatticeRules.CAP));
		assertEquals(0, RimeLatticeRules.damage(Double.NaN));
		assertEquals(RimeLatticeRules.DAMAGE, RimeLatticeRules.damage(-5));
	}

	@Test
	void thunderChainArcsBetweenCrowdedChallengersButNotSpreadOnes() {
		assertArrayEquals(new int[] {36, 42, 48}, ElementalMasters.beats(MastersRules.Move.THUNDER_CHAIN));
		double[][] rods = ThunderChainRules.rods(10, 0, 1, 0);
		assertEquals(3, rods.length);
		assertArrayEquals(new double[] {10, 0}, rods[0], 1e-9);
		assertEquals(ThunderChainRules.SPREAD, Math.hypot(rods[1][0] - 10, rods[1][1]), 1e-9);
		assertEquals(ThunderChainRules.SPREAD, Math.hypot(rods[2][0] - 10, rods[2][1]), 1e-9);
		assertTrue(ThunderChainRules.nearRod(10, 0, 10.5, 0, 0));
		assertFalse(ThunderChainRules.nearRod(10, 0, 12, 0, 0), "stepping out of the circle avoids the bolt");
		// Wrong answer: three challengers huddled together. The rod's bolt jumps through all of them.
		boolean[] crowded = ThunderChainRules.chain(0, 0, new double[] {0, 2, 4}, new double[] {0, 0, 0}, new double[] {0, 0, 0}, new boolean[3]);
		assertArrayEquals(new boolean[] {true, true, true}, crowded);
		// Right answer: spread beyond a jump. Only the one standing at the rod is struck.
		boolean[] spread = ThunderChainRules.chain(0, 0, new double[] {0, 3, 6}, new double[] {0, 0, 0}, new double[] {0, 0, 0}, new boolean[3]);
		assertArrayEquals(new boolean[] {true, false, false}, spread);
		// Already struck challengers neither take nor carry a later bolt, so nobody is struck twice.
		boolean[] spent = ThunderChainRules.chain(0, 0, new double[] {0, 2, 4}, new double[] {0, 0, 0}, new double[] {0, 0, 0},
			new boolean[] {true, false, false});
		assertArrayEquals(new boolean[] {false, false, false}, spent);
		boolean[] bad = ThunderChainRules.chain(0, 0, new double[] {Double.NaN, 1}, new double[] {0, 0}, new double[] {0, 0}, new boolean[2]);
		assertArrayEquals(new boolean[] {false, true}, bad);
		assertEquals(0, ThunderChainRules.strikingRod(36));
		assertEquals(2, ThunderChainRules.strikingRod(48));
		assertEquals(-1, ThunderChainRules.strikingRod(37));
	}

	@Test
	void verdantBloomRootsInsideTheRingOnly() {
		assertArrayEquals(new int[] {30}, ElementalMasters.beats(MastersRules.Move.VERDANT_BLOOM));
		assertTrue(VerdantBloomRules.hits(0, 0, 0));
		assertTrue(VerdantBloomRules.hits(2.9, 0, 0));
		assertFalse(VerdantBloomRules.hits(3.2, 0, 0), "leaving the ring avoids the bloom");
		assertFalse(VerdantBloomRules.hits(1, 1, 1), "a jump clears the bloom");
		assertFalse(VerdantBloomRules.hits(Double.POSITIVE_INFINITY, 0, 0));
		assertTrue(VerdantBloomRules.ROOT_TICKS > 0 && VerdantBloomRules.ROOT_TICKS <= 40);
	}

	@Test
	void hollowPullCatchesTheStillAndTheWalkingButNotTheSprinting() {
		assertArrayEquals(new int[] {40}, ElementalMasters.beats(MastersRules.Move.HOLLOW_PULL));
		assertArrayEquals(new double[] {0, 0}, HollowPullRules.tug(5, 0, true), 1e-12);
		assertArrayEquals(new double[] {-HollowPullRules.PULL, 0}, HollowPullRules.tug(5, 0, false), 1e-12);
		assertArrayEquals(new double[] {0, 0}, HollowPullRules.tug(11, 0, false), 1e-12);
		assertArrayEquals(new double[] {0, 0}, HollowPullRules.tug(.5, 0, false), 1e-12);
		assertFalse(HollowPullRules.pulling(HollowPullRules.PULL_FROM - 1));
		assertTrue(HollowPullRules.pulling(HollowPullRules.PULL_FROM));
		assertFalse(HollowPullRules.pulling(HollowPullRules.TELL));
		for (double start = HollowPullRules.NEAR; start <= HollowPullRules.FAR; start += .5) {
			double still = HollowPullRules.drift(start, 0, false);
			double walking = HollowPullRules.drift(start, .215, false);
			double sprinting = HollowPullRules.drift(start, .28, true);
			assertTrue(HollowPullRules.hits(still, 0, 0), "standing at " + start + " is dragged into the core: " + still);
			assertTrue(HollowPullRules.hits(walking, 0, 0), "walking away from " + start + " is dragged in: " + walking);
			assertFalse(HollowPullRules.hits(sprinting, 0, 0), "sprinting away from " + start + " escapes: " + sprinting);
		}
		assertFalse(HollowPullRules.hits(0, 0, 3));
		assertFalse(HollowPullRules.hits(Double.NaN, 0, 0));
	}

	@Test
	void firstClearsRewardExistingTechniquePartsAndUseTheirOwnBits() {
		Set<String> rewards = new HashSet<>();
		var progress = MasterVictoryRules.Progress.NONE;
		for (int school : SCHOOLS) {
			String reward = MasterVictoryRules.reward(school);
			assertEquals(ElementalMasters.reward(school), reward);
			assertTrue(TechniqueRules.family(reward).isPresent(), reward);
			assertTrue(rewards.add(reward), "each school teaches a different part");
			assertFalse(progress.cleared(school));
			progress = progress.withClear(school);
			assertTrue(progress.cleared(school));
			assertEquals(progress, progress.withClear(school));
		}
		assertEquals(ElementalMasters.VICTORY_MASK, progress.schools());
		assertFalse(progress.cleared(MastersRules.EMBER));
		assertEquals(0b1111111 | 1 << MastersPackB.STARLIT | 1 << MastersPackB.HOURGLASS | 1 << MastersPackB.CRIMSON // ---- masters-b pack
			| 1 << MethodsAMasters.TIDE | 1 << MethodsAMasters.IRON | 1 << MethodsAMasters.DUNE // ---- methods-a pack
			| 1 << MethodsBMasters.ECHO | 1 << MethodsBMasters.DAWN | 1 << MethodsBMasters.VENOM, MasterVictoryRules.ALL); // ---- methods-b pack
		assertEquals("", ElementalMasters.reward(MastersRules.EMBER));
	}

	@Test
	void classicSignatureClipsAreFiniteAndBounded() {
		for (var move : SIGNATURES) {
			int id = move.ordinal() + 1;
			int end = ElementalMasters.end(move);
			assertEquals(0, ElementalMasterAnimation.sample(id, -1).weight());
			assertEquals(0, ElementalMasterAnimation.sample(id, end).weight());
			assertEquals(0, ElementalMasterAnimation.sample(id, Float.NaN).weight());
			int[] beats = ElementalMasters.beats(move);
			for (int beat : beats) assertTrue(ElementalMasterAnimation.sample(id, beat).weight() > .99F, move.name() + " lands on beat " + beat);
			var chamber = ElementalMasterAnimation.sample(id, beats[0] - 8);
			var impact = ElementalMasterAnimation.sample(id, beats[0]);
			assertFalse(chamber.sword().equals(impact.sword()), move.name() + " visibly commits to its first beat");
			for (float age = 0; age < end; age += .25F) {
				var pose = ElementalMasterAnimation.sample(id, age);
				assertTrue(pose.weight() >= 0 && pose.weight() <= 1, move.name() + " weight at " + age);
				assertTrue(Float.isFinite(pose.sword().x()) && Float.isFinite(pose.sword().y()) && Float.isFinite(pose.body().y())
					&& Float.isFinite(pose.bladeTilt()), move.name() + " at " + age);
				assertTrue(pose.stance() >= .1F && pose.stance() <= .35F, move.name() + " stance at " + age);
			}
		}
		assertEquals(0, ElementalMasterAnimation.sample(11, 5).weight());
	}
}
