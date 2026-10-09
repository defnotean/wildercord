package dev.wildercord.aura.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.world.MastersRules.Move;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MethodsAMastersTest {
	private static final Move[] SIGNATURES = {Move.TIDE_UNDERTOW_RING, Move.IRON_ANVIL_VERDICT, Move.DUNE_SHIFTING_SANDS};

	@Test
	void schoolsRoundTripAndStayClearOfTheOriginalThree() {
		assertEquals(List.of(10, 11, 12), MethodsAMasters.SCHOOLS);
		Set<String> ids = new HashSet<>();
		for (int school : MethodsAMasters.SCHOOLS) {
			assertTrue(MethodsAMasters.owns(school));
			assertTrue(MastersRules.knownSchool(school));
			assertEquals(school, MastersRules.discipline(school), "a methods-a school is not clamped into Stone");
			assertEquals(school, MethodsAMasters.school(MethodsAMasters.method(school)));
			assertTrue(ids.add(MethodsAMasters.id(school)));
			Move signature = MethodsAMasters.signature(school);
			assertNotNull(signature);
			assertEquals(school, MethodsAMasters.school(signature));
			assertTrue(MethodsAMasters.signature(signature));
			assertEquals(MethodsASignatureRules.COST, MethodsAMasters.cost(signature));
			assertEquals(MethodsASignatureRules.COOLDOWN, MethodsAMasters.cooldown(signature));
			assertNotNull(MethodsAMasters.hint(signature));
			for (Move move : MethodsAMasters.pattern(school))
				assertTrue(move == Move.SWEEP || move == Move.THRUST || move == Move.CRESCENT, "ordinary rotation uses only shared strikes");
			for (int sequence = 0; sequence < 8; sequence++)
				for (int phase = 0; phase < 3; phase++)
					assertFalse(MethodsAMasters.signature(MastersRules.move(school, sequence, phase, 3)), "the signature is never an ordinary slot");
		}
		assertEquals(Set.of("tide", "iron", "dune"), ids);
		for (int school : new int[] {MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE, 9, 13, -1}) {
			assertFalse(MethodsAMasters.owns(school));
			assertNull(MethodsAMasters.signature(school));
			assertEquals("", MethodsAMasters.reward(school));
			assertThrows(IllegalArgumentException.class, () -> MethodsAMasters.id(school));
		}
		assertEquals(-1, MethodsAMasters.school((dev.wildercord.aura.BreathingMethod) null));
		for (Move move : List.of(Move.SWEEP, Move.THRUST, Move.TECHNIQUE)) {
			assertFalse(MethodsAMasters.signature(move));
			assertEquals(-1, MethodsAMasters.school(move));
			assertEquals(MastersRules.ATTACK_COST, MethodsAMasters.cost(move));
			assertEquals(0, MethodsAMasters.cooldown(move));
			assertNull(MethodsAMasters.hint(move));
		}
	}

	@Test
	void tellAndRecoveryAreClampedToTheRetunedWindow() {
		for (int authored = -5; authored < 40; authored++) {
			for (int index = 0; index < 4; index++) {
				int tell = MethodsAMasters.tell(index, authored);
				assertTrue(tell >= 12 && tell <= 14, "tell " + tell);
			}
			int recovery = MethodsAMasters.recovery(authored);
			assertTrue(recovery >= 14 && recovery <= 16, "recovery " + recovery);
		}
		assertEquals(13, MethodsAMasters.tell(0, 13));
		assertEquals(14, MethodsAMasters.tell(1, 10), "a chained strike reads for longer than a fresh one");
		assertEquals(15, MethodsAMasters.recovery(15));
	}

	@Test
	void everySchoolHasTwentyTelegraphedTechniquesThatEndOpen() {
		Set<String> keys = new HashSet<>();
		for (var technique : MasterTechniques.all()) assertTrue(keys.add(technique.key()), "duplicate key " + technique.key());
		for (int school : MethodsAMasters.SCHOOLS) {
			var techniques = MasterTechniques.forSchool(school);
			assertTrue(techniques.size() >= MethodsAMasters.MIN_TECHNIQUES, MethodsAMasters.id(school) + " has " + techniques.size());
			for (var technique : techniques) {
				assertTrue(technique.id() > 100, "the original hundred keep ids 1 to 100");
				int previous = 0;
				for (int i = 0; i < technique.strikes().size(); i++) {
					int tell = technique.impact(i) - previous;
					assertTrue(tell >= MethodsAMasters.MIN_TELL && tell <= MethodsAMasters.MAX_TELL, technique.key() + " strike " + i + " tells " + tell);
					assertEquals(tell, technique.strikes().get(i).tell(), technique.key());
					previous = technique.impact(i);
				}
				assertTrue(technique.recovery() >= MethodsAMasters.MIN_RECOVERY && technique.recovery() <= MethodsAMasters.MAX_RECOVERY,
					technique.key() + " recovers " + technique.recovery());
				double total = technique.strikes().stream().mapToDouble(MasterTechniques.Strike::damage).sum();
				assertTrue(total > 0 && total <= MasterTechniques.MAX_SHARE + 1e-9, technique.key());
				assertSame(technique, MasterTechniques.byId(technique.id()));
			}
		}
	}

	@Test
	void tideWaveIsDryInsideAndTheUndertowIsSafeOutside() {
		Move tide = Move.TIDE_UNDERTOW_RING;
		assertArrayEquals(new int[] {14, 28}, MethodsASignatureRules.beats(tide));
		assertEquals(28, MethodsASignatureRules.tell(tide));
		assertEquals(tide.tell, MethodsASignatureRules.tell(tide));
		// Standing still at four blocks: the wave catches you, the undertow does not.
		assertTrue(MethodsASignatureRules.hits(tide, 0, 4, 0, 0, true));
		assertFalse(MethodsASignatureRules.hits(tide, 1, 4, 0, 0, true));
		// Step in for the wave: dry; but then the undertow drags you.
		assertFalse(MethodsASignatureRules.hits(tide, 0, 1.5, 0, 0, true));
		assertTrue(MethodsASignatureRules.hits(tide, 1, 1.5, 0, 0, true));
		// The full answer: in for the wave, back out past the undertow.
		assertFalse(MethodsASignatureRules.hits(tide, 0, 0, 2, 0, true) || MethodsASignatureRules.hits(tide, 1, 0, 4, 0, true));
		assertFalse(MethodsASignatureRules.hits(tide, 0, 7, 0, 0, true), "past the outer ring is also dry");
		assertFalse(MethodsASignatureRules.wave(4, 3), "far above the ground is not touched");
		assertFalse(MethodsASignatureRules.wave(Double.NaN, 0));
	}

	@Test
	void ironHammerMissesASidestepAndTheShockMissesAJump() {
		Move iron = Move.IRON_ANVIL_VERDICT;
		assertArrayEquals(new int[] {13, 26}, MethodsASignatureRules.beats(iron));
		assertTrue(MethodsASignatureRules.hits(iron, 0, 0, 0, 0, true), "standing on the locked spot");
		assertTrue(MethodsASignatureRules.hits(iron, 0, 1.2, 1.2, 0, true));
		assertFalse(MethodsASignatureRules.hits(iron, 0, 2.5, 0, 0, true), "a sidestep off the spot");
		assertTrue(MethodsASignatureRules.hits(iron, 1, 3, 0, 0, true), "the shock catches anyone grounded");
		assertFalse(MethodsASignatureRules.hits(iron, 1, 3, 0, .8, false), "a jump clears the shock");
		assertFalse(MethodsASignatureRules.shock(3, .5, true), "standing on a block above the ground clears it");
		assertFalse(MethodsASignatureRules.hits(iron, 1, 6, 0, 0, true), "beyond the ring");
		assertTrue(MethodsASignatureRules.HAMMER - MethodsASignatureRules.HAMMER_LOCK >= 6, "the lock leaves time to read");
	}

	@Test
	void duneLaneMissesASidestepAndTheStormSparesTheSpentLane() {
		Move dune = Move.DUNE_SHIFTING_SANDS;
		assertArrayEquals(new int[] {13, 27}, MethodsASignatureRules.beats(dune));
		assertTrue(MethodsASignatureRules.hits(dune, 0, 3, 0, 0, true));
		assertFalse(MethodsASignatureRules.hits(dune, 0, 3, 2, 0, true), "a sidestep out of the lane");
		assertFalse(MethodsASignatureRules.hits(dune, 0, -2, 0, 0, true), "behind the master is outside the lane");
		assertTrue(MethodsASignatureRules.hits(dune, 1, 3, 2, 0, true), "but the storm takes the sidestepper");
		assertFalse(MethodsASignatureRules.hits(dune, 1, 3, 0, 0, true), "the pale spent lane is the storm's safe ground");
		assertFalse(MethodsASignatureRules.hits(dune, 1, 0, 6, 0, true), "beyond the storm");
		assertEquals(MethodsASignatureRules.BLIND_TICKS, MethodsASignatureRules.blind(false, false));
		assertTrue(MethodsASignatureRules.blind(true, false) <= MethodsASignatureRules.BLIND_TICKS);
	}

	@Test
	void noSingleSpotAnswersBothBeats() {
		for (Move kind : SIGNATURES) {
			if (kind == Move.IRON_ANVIL_VERDICT) continue; // the hammer frame is relative to the lock, checked above
			for (double f = -7; f <= 7; f += .25)
				for (double s = -7; s <= 7; s += .25) {
					double r = Math.hypot(f, s);
					if (r < 2 || r > 5) continue; // within signature range of the planted master
					assertTrue(MethodsASignatureRules.hits(kind, 0, f, s, 0, true) || MethodsASignatureRules.hits(kind, 1, f, s, 0, true),
						kind + " standing still at " + f + "," + s + " must answer something");
				}
		}
		assertFalse(MethodsASignatureRules.hits(Move.SWEEP, 0, 0, 0, 0, true));
		assertEquals(0, MethodsASignatureRules.beats(Move.SWEEP).length);
	}

	@Test
	void eligibilityNeedsTheSlotRangeAuraAndCooldown() {
		double aura = MethodsASignatureRules.COST + MastersRules.GUARD_COST;
		for (int school : MethodsAMasters.SCHOOLS) {
			assertTrue(MethodsASignatureRules.eligible(school, 1, 3, 0, aura, 100, 100), MethodsAMasters.id(school));
			assertTrue(MethodsASignatureRules.eligible(school, 5, 3, 0, aura, 100, 100));
			assertFalse(MethodsASignatureRules.eligible(school, 2, 3, 0, aura, 100, 100), "wrong slot");
			assertFalse(MethodsASignatureRules.eligible(school, 1, 3, 0, aura, 99, 100), "cooling down");
			assertFalse(MethodsASignatureRules.eligible(school, 1, 3, 0, aura - 1, 100, 100), "keeps a guard's worth of Aura");
			assertFalse(MethodsASignatureRules.eligible(school, 1, 3, 2, aura, 100, 100), "too far above");
			assertFalse(MethodsASignatureRules.eligible(school, 1, 1, 0, aura, 100, 100), "too close to read");
			assertFalse(MethodsASignatureRules.eligible(school, 1, 9, 0, aura, 100, 100), "too far");
			assertFalse(MethodsASignatureRules.eligible(school, 1, Double.NaN, 0, aura, 100, 100));
		}
		assertFalse(MethodsASignatureRules.eligible(MastersRules.EMBER, 1, 3, 0, aura, 100, 100));
	}

	@Test
	void damageIsCappedPerFormAndTheReceiptCounts() {
		assertEquals(MethodsASignatureRules.DAMAGE, MethodsAMasters.capped(0, MethodsASignatureRules.DAMAGE), 1e-9);
		double first = MethodsAMasters.capped(0, MethodsASignatureRules.DAMAGE);
		double second = MethodsAMasters.capped(first, MethodsASignatureRules.DAMAGE);
		assertTrue(first + second <= MethodsAMasters.CHAIN_CAP + 1e-9);
		assertEquals(0, MethodsAMasters.capped(MethodsAMasters.CHAIN_CAP, 10));
		assertEquals(0, MethodsAMasters.capped(0, Double.NaN));
		assertEquals(0, MethodsAMasters.capped(0, -1));

		var receipt = new MethodsASignatureRules.Receipt(Move.IRON_ANVIL_VERDICT, 0, 0, 0);
		assertFalse(receipt.complete());
		receipt = receipt.beat(0, 1).beat(1, 0);
		assertTrue(receipt.complete());
		assertEquals(2, receipt.beats());
		assertEquals(1, receipt.landed());
		assertEquals(1, receipt.evaded());
		assertFalse(MethodsASignatureRules.Receipt.NONE.complete());
		assertThrows(IllegalArgumentException.class, () -> new MethodsASignatureRules.Receipt(null, -1, 0, 0));
	}

	@Test
	void firstClearsAreBoundedBitsWithExistingLessons() {
		var progress = MasterVictoryRules.Progress.NONE;
		for (int school : MethodsAMasters.SCHOOLS) {
			assertFalse(progress.cleared(school));
			progress = progress.withClear(school);
			assertTrue(progress.cleared(school));
			String reward = MasterVictoryRules.reward(school);
			assertEquals(MethodsAMasters.reward(school), reward);
			assertTrue(TechniqueRules.family(reward).isPresent(), reward);
			assertFalse(TechniqueRules.WAY_ONLY.contains(reward), reward + " is only lent by a Way");
		}
		assertFalse(progress.cleared(MastersRules.EMBER));
		assertEquals(0, new MasterVictoryRules.Progress(1 << 16).schools(), "an unknown future bit is dropped");
		assertEquals(Set.of(TechniqueRules.WAVE, TechniqueRules.PIERCE, TechniqueRules.BIND),
			Set.of(MethodsAMasters.reward(10), MethodsAMasters.reward(11), MethodsAMasters.reward(12)));
	}

	@Test
	void catalogAndAnimationAgreeWithTheRules() {
		var catalog = MasterMoveCatalog.legacy();
		for (Move kind : SIGNATURES) {
			var definition = catalog.forMove(kind);
			assertEquals(kind.ordinal() + 1, definition.wireId());
			assertEquals(MasterMoveCatalog.ExecutionFamily.METHODS_A_SIGNATURE, definition.executionFamily());
			assertEquals(Set.of(MethodsAMasters.school(kind)), definition.schools());
			assertTrue(catalog.forSchool(MethodsAMasters.school(kind)).contains(definition));
			int[] beats = MethodsASignatureRules.beats(kind);
			float[] posed = MethodsAMasterAnimation.beats(definition.wireId());
			assertEquals(beats.length, posed.length);
			for (int i = 0; i < beats.length; i++) assertEquals(beats[i], posed[i], 1e-6);
			assertTrue(MethodsAMasterAnimation.owns(definition.wireId()));
			var pose = MasterAnimationRules.sample(definition.wireId(), beats[0], kind.tell, 1, kind.recovery - 1);
			assertNotNull(pose);
			assertTrue(pose.weight() > .99F, kind + " is fully posed on its first beat");
			assertEquals(0, MasterAnimationRules.sample(definition.wireId(), kind.tell + kind.recovery, kind.tell, 1, kind.recovery - 1).weight());
		}
		assertFalse(MethodsAMasterAnimation.owns(Move.SWEEP.ordinal() + 1));
		assertNull(MethodsAMasterAnimation.sample(Move.SWEEP.ordinal() + 1, 3, 12, 1, 12));
		assertEquals(0, MethodsAMasterAnimation.beats(Move.SWEEP.ordinal() + 1).length);
	}

	@Test
	void everyNameLessonHintAndTechniqueIsTranslated() throws IOException {
		JsonObject lang;
		try (InputStream in = MethodsAMastersTest.class.getResourceAsStream("/assets/wildercord/lang/en_us.json")) {
			assertNotNull(in);
			lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		for (int school : MethodsAMasters.SCHOOLS) {
			String id = MethodsAMasters.id(school);
			for (String key : List.of("master.wildercord.school." + id, "message.wildercord.master." + id + "_lesson",
				MethodsAMasters.hint(MethodsAMasters.signature(school))))
				assertTrue(lang.has(key), key);
			for (var technique : MasterTechniques.forSchool(school)) assertTrue(lang.has(technique.translationKey()), technique.translationKey());
		}
		for (Move kind : SIGNATURES) {
			String path = MasterMoveCatalog.legacy().forMove(kind).id().substring("wildercord:master/".length());
			assertTrue(lang.has("boss.wildercord.master." + path), path);
		}
	}
}
