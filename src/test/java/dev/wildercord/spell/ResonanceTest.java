package dev.wildercord.spell;

import dev.wildercord.world.LeyLines;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A world's resonances and quirks: drawn from its seed, stable, clean spells, clear of the secrets, and kept private. */
class ResonanceTest {
	private static final long SEED = 8_213_557_431L;

	@Test
	void theSameSeedAndSaltGiveTheSameResonances() {
		assertEquals(ResonanceForge.forge(SEED, "", 12), ResonanceForge.forge(SEED, "", 12));
		assertEquals(ResonanceForge.forge(-4L, "autumn", 12), ResonanceForge.forge(-4L, "autumn", 12));
		assertEquals(RuneQuirks.forge(SEED, "", 4), RuneQuirks.forge(SEED, "", 4));
	}

	@Test
	void differentSeedsGetDifferentMagic() {
		List<Resonance> a = ResonanceForge.forge(1L, "", 12);
		List<Resonance> b = ResonanceForge.forge(2L, "", 12);
		assertNotEquals(a, b);
		Set<List<String>> sequences = new HashSet<>();
		a.forEach(r -> sequences.add(r.runes()));
		long shared = b.stream().filter(r -> sequences.contains(r.runes())).count();
		assertTrue(shared <= 1, "two worlds should share almost no sequences, these share " + shared);
	}

	@Test
	void aRerollSaltDrawsAFreshSet() {
		List<Resonance> plain = ResonanceForge.forge(SEED, "", 12);
		List<Resonance> rerolled = ResonanceForge.forge(SEED, "second age", 12);
		assertNotEquals(plain, rerolled);
		Set<String> ids = new HashSet<>();
		plain.forEach(r -> ids.add(r.id()));
		assertTrue(rerolled.stream().noneMatch(r -> ids.contains(r.id())), "a reroll keeps none of the old ids, so old finds don't count");
		assertNotEquals(RuneQuirks.forge(SEED, "", 4), RuneQuirks.forge(SEED, "second age", 4));
	}

	@Test
	void theCountIsKeptAndTheFirstOnesNeverMove() {
		List<Resonance> twelve = ResonanceForge.forge(SEED, "", 12);
		assertEquals(12, twelve.size());
		assertEquals(twelve.subList(0, 8), ResonanceForge.forge(SEED, "", 8));
		assertEquals(ResonanceForge.MAX_COUNT, ResonanceForge.forge(SEED, "", 99).size());
		assertTrue(ResonanceForge.forge(SEED, "", 0).isEmpty());
		assertTrue(ResonanceForge.forge(SEED, "", -3).isEmpty());
		assertEquals(4, RuneQuirks.forge(SEED, "", 4).size());
		assertEquals(RuneQuirks.MAX_COUNT, RuneQuirks.forge(SEED, "", 99).size());
	}

	@Test
	void everyResonanceIsAFairCleanSpell() {
		for (long seed = 0; seed < 150; seed++) {
			List<Resonance> world = ResonanceForge.forge(seed * 7_919L - 31, seed % 3 == 0 ? "" : "salt" + seed, ResonanceForge.MAX_COUNT);
			assertEquals(ResonanceForge.MAX_COUNT, world.size(), "seed " + seed + " should fill the world");
			Set<List<String>> sequences = new HashSet<>();
			Set<String> twists = new HashSet<>();
			Set<String> names = new HashSet<>();
			Set<String> ids = new HashSet<>();
			for (Resonance resonance : world) {
				String what = "seed " + seed + ", " + resonance.name() + " " + resonance.runes();
				List<RuneDef> runes = resonance.defs().orElseThrow();
				assertTrue(runes.size() == 3 || runes.size() == 4, what);
				for (RuneDef rune : runes) {
					assertTrue(rune.tier() >= 1 && rune.tier() <= 3, what + ": " + rune.name() + " is tier " + rune.tier());
					assertTrue(Runes.common(rune), what + ": " + rune.name() + " isn't one a caster can craft");
				}
				SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
				assertTrue(compiled.warnings().isEmpty(), what + ": " + compiled.warnings());
				assertFalse(compiled.isEmpty(), what);
				assertTrue(compiled.manaCost() <= ResonanceForge.MAX_COST, what + " costs " + compiled.manaCost());
				assertEquals(Optional.empty(), ResonanceForge.problem(runes, List.of()), what);
				assertTrue(Secrets.match(runes).isEmpty(), what + " is a secret spell");
				for (Fusions.Signature signature : Fusions.SIGNATURES) {
					assertFalse(runes.contains(signature.a()) && runes.contains(signature.b()), what + " holds a signature pair");
				}
				ResonanceTwists.Twist twist = resonance.twistDef().orElseThrow();
				assertEquals(RuneFamily.SHAPE, runes.getFirst().family(), what);
				assertTrue(twist.fit().allowsShape(runes.getFirst().path()), what + " doesn't fit " + twist.id());
				RuneDef lead = runes.stream().filter(r -> r.family() == RuneFamily.EFFECT).findFirst().orElseThrow();
				assertTrue(twist.fit().allowsLead(lead), what + ": " + lead.name() + " doesn't fit " + twist.id());
				assertTrue(sequences.add(resonance.runes()), what + " repeats a sequence");
				assertTrue(twists.add(resonance.twist()), what + " repeats a twist");
				assertTrue(names.add(resonance.name()), what + " repeats a name");
				assertTrue(ids.add(resonance.id()), what + " repeats an id");
			}
		}
	}

	@Test
	void namesAndRiddlesComeFromTheTwistAndTheRunes() {
		for (Resonance resonance : ResonanceForge.forge(SEED, "", ResonanceForge.MAX_COUNT)) {
			ResonanceTwists.Twist twist = resonance.twistDef().orElseThrow();
			assertTrue(resonance.name().startsWith("the "), resonance.name());
			assertTrue(twist.stems().stream().anyMatch(stem -> resonance.name().contains(stem)), resonance.name() + " should carry a word of " + twist.id());
			assertTrue(resonance.riddle().toLowerCase(Locale.ROOT).contains(twist.omen()), resonance.riddle());
			assertTrue(resonance.riddle().endsWith("."), resonance.riddle());
			for (RuneDef rune : resonance.defs().orElseThrow()) {
				if (rune.family() == RuneFamily.EFFECT) {
					assertTrue(resonance.riddle().contains(rune.name().toLowerCase(Locale.ROOT)), resonance.riddle() + " should name " + rune.name());
				}
				if (rune.family() == RuneFamily.SHAPE) {
					assertTrue(resonance.riddle().toLowerCase(Locale.ROOT).contains(ResonanceForge.SHAPES.get(rune.path())), resonance.riddle());
				}
			}
			assertFalse(resonance.riddle().matches(".*\\d.*"), "no numbers in a riddle: " + resonance.riddle());
		}
	}

	@Test
	void theRiddleReadsTheRunesInOrder() {
		assertEquals("a loosed bolt, split in three, carrying fire twice",
			ResonanceForge.body(List.of(Runes.BOLT, Runes.SPLIT_MOD, Runes.FIRE, Runes.FIRE), "carrying"));
		assertEquals("a breath let outward bearing frost and chill, made greater",
			ResonanceForge.body(List.of(Runes.BURST, Runes.FROST, Runes.CHILL, Runes.AMPLIFY), "bearing"));
		assertEquals("a line of light with harm, and where it strikes, shock",
			ResonanceForge.body(List.of(Runes.BEAM, Runes.HARM, Runes.ON_HIT, Runes.SHOCK), "with"));
	}

	@Test
	void theCatalogueOfTwistsIsFullAndEachCanBeDrawn() {
		assertTrue(ResonanceTwists.ALL.size() >= 24, "at least 24 twists, there are " + ResonanceTwists.ALL.size());
		assertTrue(ResonanceTwists.ALL.size() > ResonanceForge.MAX_COUNT);
		Set<String> ids = new HashSet<>();
		for (ResonanceTwists.Twist twist : ResonanceTwists.ALL) {
			assertTrue(ids.add(twist.id()), twist.id());
			assertFalse(twist.stems().isEmpty(), twist.id());
			assertFalse(twist.description().isBlank(), twist.id());
			assertEquals(twist.omen().toLowerCase(Locale.ROOT), twist.omen(), "an omen is a clause in lower case: " + twist.id());
			// Twists never borrow a rune's name: a resonance's name must not suggest a rune it doesn't hold.
			for (String stem : twist.stems()) {
				assertTrue(Runes.get("wildercord:" + stem.toLowerCase(Locale.ROOT)).isEmpty(), twist.id() + "'s stem " + stem + " is a rune's name");
			}
			Optional<List<RuneDef>> drawn = ResonanceForge.draw(twist, new Random(twist.id().hashCode()), List.of());
			assertTrue(drawn.isPresent(), twist.id() + " should have spells it can ride");
		}
		for (List<String> words : ResonanceForge.ELEMENT_WORDS.values()) {
			for (String word : words) {
				assertTrue(Runes.get("wildercord:" + word).isEmpty(), word + " is a rune's name");
			}
		}
	}

	@Test
	void everyRuneOfThePoolCanBeSpokenOfInARiddle() {
		for (RuneDef rune : ResonanceForge.pool()) {
			switch (rune.family()) {
				case SHAPE -> assertTrue(ResonanceForge.SHAPES.containsKey(rune.path()), rune.name());
				case MODIFIER -> assertTrue(ResonanceForge.MODIFIERS.containsKey(rune.path()), rune.name());
				default -> {}
			}
		}
		assertTrue(ResonanceForge.pool().stream().noneMatch(r -> r.tier() > 3 || Runes.innate(r) || Runes.fused(r) || RuneSources.foundOnly(r)));
		assertTrue(ResonanceForge.pool().size() > 100, "a broad pool");
	}

	@Test
	void theirSeedIsNotTheLeySeed() {
		// Every client is sent the ley seed; nothing about the resonances may follow from it.
		assertNotEquals(LeyLines.seedOf(SEED), ResonanceForge.seedOf(SEED, "", "resonances"));
		assertNotEquals(ResonanceForge.seedOf(SEED, "", "resonances"), ResonanceForge.seedOf(SEED, "", "quirks"));
		assertNotEquals(ResonanceForge.seedOf(SEED, "", "resonances"), ResonanceForge.seedOf(SEED, "x", "resonances"));
	}

	@Test
	void quirksTouchDifferentRunesAndSayWhatTheyDo() {
		for (long seed = 0; seed < 60; seed++) {
			List<RuneQuirks.Quirk> quirks = RuneQuirks.forge(seed * 31L + 7, "", RuneQuirks.MAX_COUNT);
			assertEquals(RuneQuirks.MAX_COUNT, quirks.size());
			Set<String> runes = new HashSet<>();
			Set<String> ids = new HashSet<>();
			for (RuneQuirks.Quirk quirk : quirks) {
				RuneDef rune = Runes.get(quirk.rune()).orElseThrow();
				assertTrue(RuneQuirks.quirkable(rune), rune.name());
				assertTrue(runes.add(quirk.rune()), "one quirk a rune");
				assertTrue(ids.add(quirk.id()), quirk.id());
				assertTrue(quirk.text().startsWith("Here, " + rune.name() + " "), quirk.text());
				assertTrue(quirk.text().contains(quirk.when().phrase), quirk.text());
				switch (quirk.kind()) {
					case STRONGER -> assertTrue(rune.has(Trait.POWER));
					case LONGER -> assertTrue(rune.has(Trait.DURATION));
					case ECHO -> assertTrue(rune.has(Trait.POWER) && rune.kind() == EffectKind.HARMFUL);
				}
				assertTrue(quirk.key().startsWith(RuneQuirks.KEY_PREFIX));
			}
		}
	}

	@Test
	void quirkConditionsReadWhereTheEffectLands() {
		Attunements.Place midnight = new Attunements.Place("minecraft:plains", "minecraft:overworld", 70, 18000, 0, "none", true, false);
		Attunements.Place noonRain = new Attunements.Place("minecraft:plains", "minecraft:overworld", 70, 6000, 3, "rain", true, false);
		Attunements.Place cave = new Attunements.Place("minecraft:dripstone_caves", "minecraft:overworld", -20, 6000, 3, "none", false, false);
		Attunements.Place nether = new Attunements.Place("minecraft:nether_wastes", "minecraft:the_nether", 64, 18000, 0, "none", false, false);
		assertTrue(RuneQuirks.When.NIGHT.test(midnight));
		assertTrue(RuneQuirks.When.FULL_MOON.test(midnight));
		assertFalse(RuneQuirks.When.NEW_MOON.test(midnight));
		assertTrue(RuneQuirks.When.DAY.test(noonRain));
		assertTrue(RuneQuirks.When.RAIN.test(noonRain));
		assertTrue(RuneQuirks.When.DEEP.test(cave));
		assertTrue(RuneQuirks.When.UNDERGROUND.test(cave));
		assertTrue(RuneQuirks.When.NETHER.test(nether));
		// The Nether has no night, whatever its clock says.
		assertFalse(RuneQuirks.When.NIGHT.test(nether));
		assertFalse(RuneQuirks.When.FULL_MOON.test(nether));
	}

	// ------------------------------------------------------------------ privacy

	@Test
	void aClientLearnsOnlyWhatItsPlayerFoundReadOrWasTold() {
		List<Resonance> world = ResonanceForge.forge(SEED, "", 6);
		Resonance found = world.get(0);
		Resonance read = world.get(1);
		Resonance announced = world.get(2);
		Resonance readAndAnnounced = world.get(3);
		List<String> grimoire = List.of(found.key(), read.hintKey(), readAndAnnounced.hintKey(), "secret:sunfall");
		Map<String, String> finders = Map.of(found.id(), "Alex", announced.id(), "Sam", readAndAnnounced.id(), "Kai");
		List<ResonanceLore.View> views = ResonanceLore.views(world, grimoire, finders);
		assertEquals(4, views.size(), "the two nobody found or read stay hidden: " + views);
		for (ResonanceLore.View view : views) {
			assertTrue(world.stream().anyMatch(r -> r.id().equals(view.id())));
			assertNotEquals(world.get(4).id(), view.id());
			assertNotEquals(world.get(5).id(), view.id());
		}
		ResonanceLore.View mine = views.get(0);
		assertTrue(mine.found());
		assertEquals(found.runes(), mine.runes());
		assertEquals(found.riddle(), mine.riddle());
		assertEquals(found.twist(), mine.twist());
		assertEquals("Alex", mine.finder());
		assertTrue(mine.matchesIds(found.runes()));
		ResonanceLore.View riddle = views.get(1);
		assertFalse(riddle.found());
		assertTrue(riddle.hinted());
		assertTrue(riddle.runes().isEmpty(), "a riddle read never gives away its runes");
		assertEquals("", riddle.twist(), "nor its twist");
		assertEquals(read.riddle(), riddle.riddle());
		assertFalse(riddle.matchesIds(read.runes()));
		ResonanceLore.View told = views.get(2);
		assertEquals(announced.name(), told.name());
		assertEquals("Sam", told.finder());
		assertTrue(told.runes().isEmpty());
		assertEquals("", told.riddle(), "an announcement gives only a name and a finder");
		assertEquals("", told.twist());
		ResonanceLore.View both = views.get(3);
		assertEquals(readAndAnnounced.riddle(), both.riddle());
		assertEquals("Kai", both.finder());
		assertTrue(both.runes().isEmpty());
		// A world nobody has touched says nothing at all.
		assertTrue(ResonanceLore.views(world, List.of(), Map.of()).isEmpty());
	}

	@Test
	void aClientLearnsOnlyTheQuirksItsPlayerMet() {
		List<RuneQuirks.Quirk> quirks = RuneQuirks.forge(SEED, "", 4);
		List<ResonanceLore.QuirkView> met = ResonanceLore.quirks(quirks, List.of(quirks.get(2).key(), "feat:overcast"));
		assertEquals(List.of(new ResonanceLore.QuirkView(quirks.get(2).id(), quirks.get(2).text())), met);
		assertTrue(ResonanceLore.quirks(quirks, List.of()).isEmpty());
	}
}
