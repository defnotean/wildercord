package dev.wildercord.aura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.aura.arts.DuneArts;
import dev.wildercord.aura.arts.IronArts;
import dev.wildercord.aura.arts.MethodsAFinishers;
import dev.wildercord.aura.arts.TideArts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Tide, Iron and Dune Breath (the methods-a pack): their pure rules, and that each is wired, named and voiced. */
class MethodsARulesTest {
	private static final List<String> OURS = List.of("tide", "iron", "dune");

	// ------------------------------------------------------------------ the methods

	@Test
	void theThreeAreBuiltInWithTheirOwnElementsAndFlavours() {
		assertEquals(OURS, List.of(TideArts.METHOD, IronArts.METHOD, DuneArts.METHOD));
		assertEquals(BreathingMethod.Flavour.CURRENT, BreathingMethods.TIDE.flavour());
		assertEquals(BreathingMethod.Flavour.FORGE, BreathingMethods.IRON.flavour());
		assertEquals(BreathingMethod.Flavour.GRIT, BreathingMethods.DUNE.flavour());
		assertEquals("brine", BreathingMethods.TIDE.element());
		assertEquals("metal", BreathingMethods.IRON.element());
		assertEquals("sand", BreathingMethods.DUNE.element());
		for (String id : OURS) {
			assertTrue(BreathingMethods.BUILT_IN.stream().anyMatch(m -> m.id().equals(id)), id);
			assertNotEquals(AwakeningRules.Flavour.PLAIN, AwakeningRules.Flavour.of(id), id + " has a Dominion of its own");
			assertEquals(5, ArtRules.ARTS.stream().filter(a -> a.method().equals(id)).count(), id + " has five arts");
		}
	}

	// ------------------------------------------------------------------ Tide

	@Test
	void tidePushesAndSoaksMoreAtEachStage() {
		assertEquals(0, TideRules.push(AuraRules.NONE));
		assertEquals(0, TideRules.soak(AuraRules.NONE));
		for (int stage = AuraRules.NONE + 1; stage < AuraRules.SOVEREIGN; stage++) {
			assertTrue(TideRules.push(stage + 1) > TideRules.push(stage));
			assertTrue(TideRules.soak(stage + 1) > TideRules.soak(stage));
		}
	}

	@Test
	void aSoakedFoeIsCarriedFurtherButPlayersAndBossesAreHeld() {
		double dry = TideRules.current(1.0, false, false, false);
		double soaked = TideRules.current(1.0, true, false, false);
		assertEquals(dry * 1.2, soaked, 1e-9);
		assertTrue(TideRules.current(20.0, true, true, false) < TideRules.current(20.0, true, false, false), "a player is held");
		assertEquals(0, TideRules.current(1.0, true, false, true), "a boss is held");
	}

	// ------------------------------------------------------------------ Iron

	@Test
	void ironCracksMoreArmourAtEachStage() {
		assertEquals(0, IronRules.crack(AuraRules.NONE));
		for (int stage = AuraRules.NONE + 1; stage < AuraRules.SOVEREIGN; stage++) {
			assertTrue(IronRules.crack(stage + 1) > IronRules.crack(stage));
		}
		assertTrue(IronRules.crack(AuraRules.SOVEREIGN) < IronRules.SUNDER_MAX);
	}

	@Test
	void sunderNeverPassesItsCapNorTakesMoreThanTheFoeHas() {
		assertEquals(4.0, IronRules.sunder(4.0, 0, 20), 1e-9);
		assertEquals(2.0, IronRules.sunder(4.0, 6.0, 20), 1e-9, "only up to the cap in all");
		assertEquals(0.0, IronRules.sunder(4.0, IronRules.SUNDER_MAX, 20), 1e-9);
		assertEquals(0.0, IronRules.sunder(4.0, IronRules.SUNDER_MAX + 3, 20), 1e-9);
		assertEquals(1.5, IronRules.sunder(4.0, 0, 1.5), 1e-9, "never more than it wears");
		assertEquals(0.0, IronRules.sunder(4.0, 0, 0), 1e-9, "nothing off a foe with none");
		assertEquals(0.0, IronRules.sunder(-1, 0, 10), 1e-9);
		assertEquals(4.0, IronRules.sunder(4.0, -2, 20), 1e-9, "a negative tally reads as none");
		assertTrue(IronRules.WORLDFORGE_ARMOUR <= IronRules.SUNDER_MAX);
	}

	// ------------------------------------------------------------------ Dune

	@Test
	void gritBlindsShortOnPlayersAndNeverOnBosses() {
		assertEquals(40, DuneRules.blind(40, false, false));
		assertEquals(DuneRules.PVP_BLIND, DuneRules.blind(40, true, false));
		assertEquals(10, DuneRules.blind(10, true, false));
		assertEquals(0, DuneRules.blind(40, false, true));
		assertEquals(0, DuneRules.blind(40, true, true));
		assertEquals(0, DuneRules.blind(0, false, false));
		assertEquals(0, DuneRules.blind(-5, false, false));
	}

	@Test
	void sinkingIsCappedByWhoSinks() {
		assertEquals(2, DuneRules.sink(2, false, false));
		assertEquals(3, DuneRules.sink(9, false, false), "a creature sinks to IV at most");
		assertEquals(1, DuneRules.sink(3, true, false), "a player to II at most");
		assertEquals(0, DuneRules.sink(0, true, false));
		assertEquals(-1, DuneRules.sink(3, false, true), "a boss never sinks");
		assertEquals(-1, DuneRules.sink(-1, false, false));
	}

	@Test
	void gritIsLikelierAtEachStageAndNeverCertain() {
		assertEquals(0, DuneRules.gritChance(AuraRules.NONE));
		for (int stage = AuraRules.NONE + 1; stage < AuraRules.SOVEREIGN; stage++) {
			assertTrue(DuneRules.gritChance(stage + 1) > DuneRules.gritChance(stage));
		}
		assertTrue(DuneRules.gritChance(AuraRules.SOVEREIGN) < 0.5);
	}

	// ------------------------------------------------------------------ momentum

	@Test
	void eachHasATemperFavouringItsOwnMark() {
		MomentumRules.Temper tide = MomentumRules.temper("tide");
		MomentumRules.Temper iron = MomentumRules.temper("iron");
		MomentumRules.Temper dune = MomentumRules.temper("dune");
		assertEquals(MomentumRules.SOAKED, tide.favours());
		assertEquals(MomentumRules.SUNDERED, iron.favours());
		assertEquals(MomentumRules.BLINDED, dune.favours());
		assertTrue(iron.guard() > 1.0 && iron.loss() < 1.0, "iron's guard is heavy and it holds its momentum");
		Set<Integer> bits = new HashSet<>();
		for (int bit : List.of(MomentumRules.SOAKED, MomentumRules.SUNDERED, MomentumRules.BLINDED)) {
			assertEquals(1, Integer.bitCount(bit));
			assertTrue(bits.add(bit));
		}
		assertNotEquals(MomentumRules.Temper.PLAIN, tide);
		assertNotEquals(MomentumRules.Temper.PLAIN, iron);
		assertNotEquals(MomentumRules.Temper.PLAIN, dune);
	}

	// ------------------------------------------------------------------ named and voiced

	@Test
	void finishersAreNamedDescribedAndVoiced() throws IOException {
		JsonObject lang = read("/assets/wildercord/lang/en_us.json");
		JsonObject kit = read("/assets/wildercord/kit_sounds.json").getAsJsonObject("events");
		assertEquals(3, MethodsAFinishers.IDS.size());
		Set<String> names = new HashSet<>();
		for (String id : MethodsAFinishers.IDS) {
			String key = "aura.wildercord.finisher." + id;
			assertTrue(lang.has(key), key);
			assertTrue(lang.has(key + ".desc"), key + ".desc");
			assertTrue(names.add(lang.get(key).getAsString()));
		}
		for (String sound : MethodsAFinishers.SOUNDS) {
			assertTrue(kit.has(sound), sound + ": run python tools/feel/build.py --only aura");
		}
		for (String id : OURS) {
			for (String key : List.of("aura.wildercord.method." + id, "aura.wildercord.method." + id + ".lore", "aura.wildercord.method." + id + ".flavour")) {
				assertTrue(lang.has(key), key);
			}
			for (String sound : List.of("aura_" + id + "_swing", "aura_" + id + "_impact", "aura_" + id + "_art", "aura_awaken_" + id)) {
				assertTrue(kit.has(sound), sound);
			}
		}
		for (String element : List.of("brine", "metal", "sand")) {
			assertTrue(lang.has("element.wildercord." + element), element);
		}
		for (String writing : List.of("current", "forge", "grit")) {
			assertTrue(lang.has("screen.wildercord.aura.writing.element." + writing), writing);
		}
	}

	private static JsonObject read(String path) throws IOException {
		try (InputStream in = MethodsARulesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
