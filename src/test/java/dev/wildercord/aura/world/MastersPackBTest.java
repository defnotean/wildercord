package dev.wildercord.aura.world;

import com.google.gson.JsonParser;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.TechniqueRules;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Starlit, Hourglass and Crimson Masters: catalog parity, tempo bounds, caps and recent-technique avoidance. */
class MastersPackBTest {
	private static final int[] SCHOOLS = {MastersPackB.STARLIT, MastersPackB.HOURGLASS, MastersPackB.CRIMSON};

	@Test
	void schoolIdsFollowTheBreathingMethodIndexAndRoundTrip() {
		assertEquals(BreathingMethods.BUILT_IN.indexOf(BreathingMethods.STARLIT), MastersPackB.STARLIT);
		assertEquals(BreathingMethods.BUILT_IN.indexOf(BreathingMethods.HOURGLASS), MastersPackB.HOURGLASS);
		assertEquals(BreathingMethods.BUILT_IN.indexOf(BreathingMethods.CRIMSON), MastersPackB.CRIMSON);
		for (int school : SCHOOLS) {
			assertTrue(MastersPackB.owns(school));
			assertTrue(MastersRules.knownSchool(school));
			assertEquals(school, MastersRules.discipline(school), "A pack B school is not folded back into Ember");
			assertEquals(school, MastersPackB.school(MastersPackB.method(school)));
			assertEquals(MastersPackB.method(school).id(), MastersPackB.id(school));
			assertEquals(school, MastersPackB.school(MastersPackB.signature(school)));
			assertTrue(MastersPackB.signature(MastersPackB.signature(school)));
		}
		for (int other : new int[] {-1, MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE, 3, 4, 5, 6, 10, Integer.MAX_VALUE}) {
			assertFalse(MastersPackB.owns(other));
			assertNull(MastersPackB.signature(other));
		}
		assertEquals(-1, MastersPackB.school(BreathingMethods.EMBER));
		assertEquals(-1, MastersPackB.school((BreathingMethod) null));
		assertFalse(MastersPackB.signature(MastersRules.Move.KILN_RING));
		assertEquals(-1, MastersPackB.school(MastersRules.Move.TECHNIQUE));
	}

	@Test
	void signaturesAreCatalogedOnlyForTheirOwnSchoolWithStableWireIds() {
		var catalog = MasterMoveCatalog.legacy();
		int wire = 16;
		for (int school : SCHOOLS) {
			MastersRules.Move signature = MastersPackB.signature(school);
			assertEquals(wire++, signature.ordinal() + 1, "Signatures append after every older wire id");
			var definition = catalog.forMove(signature);
			assertEquals(Set.of(school), definition.schools());
			assertTrue(catalog.forSchool(school).contains(definition));
			for (int other : SCHOOLS) if (other != school) assertFalse(catalog.forSchool(other).contains(definition));
			for (int old = MastersRules.EMBER; old <= MastersRules.STONE; old++) assertFalse(catalog.forSchool(old).contains(definition));
			assertEquals(MastersPackB.cost(signature), LegacyMasterMoves.auraCost(signature, school));
			assertTrue(MastersPackB.cost(signature) + MastersRules.GUARD_COST <= MastersRules.AURA_MAX);
			assertTrue(MastersPackB.cooldown(signature) >= 40 + signature.tell, "A signature never repeats back to back");
			assertNotNull(MastersPackB.answer(signature));
		}
	}

	@Test
	void everySchoolHasAtLeastTwentyFiveDistinctNamedTechniques() {
		Set<String> keys = new HashSet<>();
		int expectedId = 205; // After the original hundred and the 104 Rime, Thunder, Verdant and Hollow techniques.
		for (int school : SCHOOLS) {
			var techniques = MasterTechniques.forSchool(school);
			assertTrue(techniques.size() >= MastersPackB.MIN_TECHNIQUES, "school " + school);
			Set<String> shapes = new HashSet<>();
			for (var technique : techniques) {
				assertEquals(school, technique.school());
				assertEquals(expectedId++, technique.id(), "Pack B ids follow the earlier tables without gaps");
				assertTrue(keys.add(technique.key()), technique.key());
				String sequence = technique.strikes().stream().map(s -> s.primitive().name() + s.mirrored() + s.spin() + s.step()
					+ s.quick() + s.heavy()).toList().toString();
				assertTrue(shapes.add(sequence), technique.key() + " repeats another technique");
			}
		}
		for (var technique : MasterTechniques.all()) if (technique.id() <= 100) assertFalse(MastersPackB.owns(technique.school()));
	}

	@Test
	void everyStrikeTellsTwelveToFourteenTicksAndEveryChainRecoversFourteenToSixteen() {
		for (int school : SCHOOLS) for (var technique : MasterTechniques.forSchool(school)) {
			int previous = 0;
			for (int i = 0; i < technique.strikes().size(); i++) {
				int tell = technique.impact(i) - previous;
				assertTrue(tell >= MastersPackB.MIN_TELL && tell <= MastersPackB.MAX_TELL, technique.key() + " strike " + i + " tells " + tell);
				previous = technique.impact(i);
			}
			assertTrue(technique.recovery() >= MastersPackB.MIN_RECOVERY && technique.recovery() <= MastersPackB.MAX_RECOVERY, technique.key());
			assertTrue(technique.cost() + MastersRules.GUARD_COST <= MastersRules.AURA_MAX);
		}
		for (int school : SCHOOLS) for (MastersRules.Move move : MastersPackB.pattern(school)) {
			assertTrue(move.tell >= MastersPackB.MIN_TELL && move.tell <= MastersPackB.MAX_TELL, move.name());
			assertTrue(move.recovery >= MastersPackB.MIN_RECOVERY && move.recovery <= MastersPackB.MAX_RECOVERY, move.name());
			assertNotNull(MastersPackB.answer(move), "Every ordinary attack names its answer");
		}
		for (int index = 0; index < 4; index++) for (int authored = -50; authored <= 50; authored++) {
			int tell = MastersPackB.tell(index, authored);
			assertTrue(tell >= 12 && tell <= 14);
			int recovery = MastersPackB.recovery(authored);
			assertTrue(recovery >= 14 && recovery <= 16);
		}
	}

	@Test
	void theOrdinaryRotationUsesThePackPatterns() {
		for (int school : SCHOOLS) {
			MastersRules.Move[] pattern = MastersPackB.pattern(school);
			assertEquals(4, pattern.length);
			for (int sequence = 0; sequence < 8; sequence++)
				assertEquals(pattern[sequence % 4], MastersRules.move(school, sequence, 0, 3));
			assertEquals(MastersRules.Move.CRESCENT, MastersRules.move(school, 0, 0, 8), "Out of reach still answers with a lane");
		}
	}

	@Test
	void chainDamageIsCappedPerChallenger() {
		assertEquals(MastersRules.TECHNIQUE_DAMAGE * MasterTechniques.MAX_SHARE, MastersPackB.CHAIN_CAP, 1e-9);
		double dealt = 0;
		for (int beat = 0; beat < 10; beat++) {
			double share = MastersPackB.capped(dealt, 18);
			assertTrue(share >= 0 && share <= 18);
			dealt += share;
		}
		assertEquals(MastersPackB.CHAIN_CAP, dealt, 1e-9);
		assertEquals(0, MastersPackB.capped(MastersPackB.CHAIN_CAP + 5, 10));
		assertEquals(0, MastersPackB.capped(0, -3));
		assertEquals(0, MastersPackB.capped(Double.NaN, 3));
		assertEquals(0, MastersPackB.capped(0, Double.POSITIVE_INFINITY));
		assertEquals(10, MastersPackB.capped(-4, 10), "A negative ledger cannot lift the cap");
		// The signatures' raw beats add to more than the cap, so the cap is what bounds a full chain.
		assertTrue(StarlitConstellationRules.STARS * StarlitConstellationRules.DAMAGE > MastersPackB.CHAIN_CAP);
		assertTrue(2 * HourglassRewindRules.DAMAGE > MastersPackB.CHAIN_CAP);
		assertTrue(CrimsonFrenzyRules.BEATS.length * CrimsonFrenzyRules.DAMAGE > MastersPackB.CHAIN_CAP);
		for (int school : SCHOOLS) for (var technique : MasterTechniques.forSchool(school))
			assertTrue(technique.strikes().stream().mapToDouble(MasterTechniques.Strike::damage).sum() <= MasterTechniques.MAX_SHARE + 1e-9);
	}

	@Test
	void recentTechniquesAreAvoidedAndASchoolAlwaysKeepsFreshChoices() {
		for (int school : SCHOOLS) {
			var all = MasterTechniques.forSchool(school);
			List<Integer> recent = new ArrayList<>();
			for (int i = 0; i < 6; i++) recent.add(all.get(i * 3).id());
			var fresh = MastersPackB.fresh(all, recent);
			assertEquals(all.size() - 6, fresh.size());
			assertTrue(fresh.stream().noneMatch(technique -> recent.contains(technique.id())));
			assertTrue(fresh.size() >= MastersPackB.MIN_TECHNIQUES - 6);
			assertEquals(all, MastersPackB.fresh(all, List.of()));
		}
	}

	@Test
	void victoriesRecordAndRewardEachNewSchool() {
		Set<String> rewards = new HashSet<>();
		var progress = MasterVictoryRules.Progress.NONE;
		for (int school : SCHOOLS) {
			assertFalse(progress.cleared(school));
			progress = progress.withClear(school);
			assertTrue(progress.cleared(school));
			assertEquals(progress, progress.withClear(school));
			String reward = MasterVictoryRules.reward(school);
			assertFalse(reward.isEmpty());
			assertTrue(TechniqueRules.family(reward).isPresent(), reward);
			rewards.add(reward);
		}
		assertEquals(3, rewards.size());
		assertFalse(progress.cleared(MastersRules.EMBER));
	}

	@Test
	void everyPlayerFacingKeyIsGenerated() throws Exception {
		var lang = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		for (int school : SCHOOLS) {
			String id = MastersPackB.id(school);
			assertTrue(lang.has("master.wildercord.school." + id), id);
			assertTrue(lang.has("message.wildercord.master." + id + "_lesson"), id);
			assertTrue(lang.has("boss.wildercord.master." + MastersPackB.signature(school).name().toLowerCase(Locale.ROOT)), id);
			assertTrue(lang.has(MastersPackB.answer(MastersPackB.signature(school))), id);
			for (var technique : MasterTechniques.forSchool(school)) assertTrue(lang.has(technique.translationKey()), technique.key());
			assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/textures/entity/master/" + id + ".png")));
			assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/items/master_blade/" + id + ".json")));
		}
		for (var shape : MasterTechniques.Shape.values()) assertTrue(lang.has(MastersPackB.answer(shape)), shape.name());
	}
}
