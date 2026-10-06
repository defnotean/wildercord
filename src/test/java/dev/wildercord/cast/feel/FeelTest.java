package dev.wildercord.cast.feel;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.ReweaveRules;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeelTest {
	private static SpellPlan.Group first(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes)).root().groups.getFirst();
	}

	@Test
	void everyShapeHasAMotionAndTheEightAreAllUsed() {
		java.util.Set<Motion> seen = java.util.EnumSet.noneOf(Motion.class);
		int shapes = 0;
		for (RuneDef rune : Runes.all()) {
			if (rune.family() == RuneFamily.SHAPE) {
				shapes++;
				seen.add(Motion.of(rune.id()));
			}
		}
		assertTrue(shapes >= 39);
		assertEquals(java.util.EnumSet.allOf(Motion.class), seen);
	}

	@Test
	void roleComesFromTheFirstEffect() {
		assertEquals(Role.MEND, Feel.of(first(Runes.BOLT, Runes.HEAL), 12, 0).role());
		assertEquals(Role.STRIKE, Feel.of(first(Runes.BOLT, Runes.FIRE), 12, 0).role());
		assertEquals(Role.STRIKE, Feel.of(first(Runes.BOLT), 12, 0).role());
	}

	@Test
	void elementIsTheManaDominantOneAndTheFirstEffectWinsATie() {
		Feel a = Feel.of(first(Runes.BOLT, Runes.FIRE, Runes.FROST), 20, 0);
		assertEquals("fire", a.element());
		assertEquals("frost", a.accent());
		Feel b = Feel.of(first(Runes.BOLT, Runes.HARM, Runes.EXPLODE), 20, 0);
		assertEquals("fire", b.element(), "Explode costs more than Harm, so fire leads although Harm came first");
		assertEquals("", Feel.of(first(Runes.BOLT), 5, 0).element());
	}

	@Test
	void modifiersAreCounted() {
		Feel f = Feel.of(first(Runes.BOLT, Runes.QUICKEN, Runes.QUICKEN, Runes.FIRE, Runes.AMPLIFY), 20, 0);
		assertEquals(2, f.mod("quicken"));
		assertEquals(1, f.mod("amplify"));
		assertFalse(f.has("widen"));
	}

	@Test
	void bandMIsToday() {
		// A plain 12-mana spell sits inside band M, and the bands rise with cost and charge.
		assertEquals(Band.M, Band.of(Feel.scaleOf(12, 0, 1)));
		assertEquals(Band.S, Band.of(Feel.scaleOf(3, 0, 1)));
		assertTrue(Feel.scaleOf(44, 0, 4) > Feel.scaleOf(12, 0, 1));
		assertTrue(Feel.scaleOf(12, 1, 1) > Feel.scaleOf(12, 0, 1));
		assertEquals(Band.XL, Band.of(Feel.scaleOf(400, 1, 4)));
		assertTrue(Feel.scaleOf(1e9, 1, 9) <= 2.2 && Feel.scaleOf(0, 0, 1) >= 0.6);
	}

	@Test
	void everyRuneOfEveryFamilyGivesAFeelWithoutThrowing() {
		for (RuneDef shape : Runes.all()) {
			if (shape.family() != RuneFamily.SHAPE) {
				continue;
			}
			for (RuneDef effect : Runes.all()) {
				if (effect.family() != RuneFamily.EFFECT) {
					continue;
				}
				var compiled = SpellCompiler.compile(List.of(shape, effect));
				if (shape.equals(Runes.RELAY) && !dev.wildercord.spell.RelayRules.valid(List.of(shape, effect))) {
					assertTrue(compiled.isEmpty()); assertEquals(0, compiled.cost()); assertFalse(compiled.warnings().isEmpty());
					continue;
				}
				if (shape.equals(Runes.REWEAVE) && !effect.equals(Runes.HARM)) {
					assertTrue(compiled.isEmpty(), "Reweave refuses " + effect.name());
					assertEquals(0, compiled.cost());
					assertTrue(compiled.warnings().contains(ReweaveRules.GRAMMAR_PROBLEM));
					continue;
				}
				assertFalse(compiled.isEmpty(), shape.name() + " / " + effect.name());
				SpellPlan.Group g = compiled.root().groups.getFirst();
				Feel f = Feel.of(g, 30, 0.5);
				assertNotNull(f.role());
				assertNotNull(f.motion());
			}
		}
	}

	@Test
	void reweavesExactHarmFieldUsesTheGroundInscriptionGesture() {
		var compiled = SpellCompiler.compile(List.of(Runes.REWEAVE, Runes.HARM));
		assertTrue(compiled.warnings().isEmpty());
		assertEquals(1, compiled.root().groups.size());
		Feel feel = Feel.of(compiled.root().groups.getFirst(), compiled.cost(), 0);
		assertEquals(Motion.SEAL, feel.motion());
		assertEquals(Role.STRIKE, feel.role());
		assertTrue(ShapeFeels.hasGesture(Runes.REWEAVE.path()));
	}

	@Test
	void everyBuiltInShapeHasItsOwnGesture() {
		for (RuneDef rune : Runes.all()) {
			if (rune.family() == RuneFamily.SHAPE && rune.id().startsWith("wildercord:")) {
				assertTrue(ShapeFeels.hasGesture(rune.path()), rune.path() + " has no gesture in ShapeFeels");
			}
		}
	}

	@Test
	void everyBuiltInEffectHasASignature() {
		ShapeFeels.register();
		FireFeels.register();
		FrostFeels.register();
		StormFeels.register();
		WindFeels.register();
		EarthFeels.register();
		LifeFeels.register();
		VoidFeels.register();
		ArcaneFeels.register();
		TimeFeels.register();
		BloodFeels.register();
		dev.wildercord.cast.FieldFusionFeels.register();
		dev.wildercord.cast.NextSignatureFeels.register();
		dev.wildercord.wildlife.RootCarryFeels.init();
		dev.wildercord.cast.CampConcordFeels.init();
		java.util.List<String> missing = new java.util.ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (rune.id().startsWith("wildercord:") && rune.family() == RuneFamily.EFFECT) {
				if (Signatures.get(rune.id()) == null) missing.add(rune.id());
			}
		}
		assertTrue(missing.isEmpty(), "Effects without a cast signature: " + missing);
	}

	@Test
	void stepsClimbThePentatonicScale() {
		assertEquals(1.0F, Feels.step(0), 1e-6);
		assertEquals(1.498F, Feels.step(3), 1e-6);
		assertEquals(2.0F, Feels.step(5), 1e-6);
		assertEquals(2.0F, Feels.step(40), 1e-6);
		for (int i = 1; i < 6; i++) {
			assertTrue(Feels.step(i) > Feels.step(i - 1));
		}
	}

	@Test
	void theCircleGrowsWithTheBand() {
		assertEquals(1.0, Feels.circleRadius(Feel.plain("wildercord:bolt", "fire")), 1e-9);
		assertTrue(Feels.circleRadius(Feel.of(first(Runes.DOMAIN, Runes.HARM), 200, 1)) > 1.0);
	}

	@Test
	void everyKitSoundTheCodeNamesExists() throws java.io.IOException {
		java.util.Set<String> kit;
		try (java.io.InputStream in = FeelTest.class.getResourceAsStream("/assets/wildercord/kit_sounds.json")) {
			kit = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject()
				.getAsJsonObject("events").keySet();
		}
		for (Motion m : Motion.values()) {
			assertTrue(kit.contains(ShapeFeels.gestureSound(m)), ShapeFeels.gestureSound(m));
		}
		// Check dynamically assembled Life names and keep signature voices single.
		for (RuneDef rune : Runes.all()) {
			if (rune.family() != RuneFamily.EFFECT || !rune.element().equals("life")) continue;
			if (rune.path().equals("root_carry")) {
				for (String voice : java.util.List.of("cue", "select", "settle")) assertTrue(kit.contains("root_carry_" + voice), "Dedicated root owner voice missing: " + voice);
				assertFalse(kit.contains("life_auth_root_carry_cue"), "Dedicated owner must not duplicate a Life cue");
				assertFalse(kit.contains("life_auth_root_carry_outcome"), "Dedicated owner must not invent a Life delta voice");
			} else if (java.util.Set.of("ashen_mercy", "pulse_ferry").contains(rune.path())) {
				assertFalse(kit.contains("life_auth_" + rune.path() + "_cue"), "Signature cue duplicated: " + rune.path());
				assertFalse(kit.contains("life_auth_" + rune.path() + "_outcome"), "Signature landing duplicated: " + rune.path());
			} else {
				assertTrue(kit.contains("life_auth_" + rune.path() + "_cue"), rune.path() + " cue missing");
				assertTrue(kit.contains("life_auth_" + rune.path() + "_outcome"), rune.path() + " outcome missing");
			}
		}
		// Stop at the call's statement: a dynamic sound must not borrow a later switch case's string.
		java.util.regex.Pattern named = java.util.regex.Pattern.compile("Feels\\.sound\\([^\";]*\"([a-z0-9_]+)\"(?!\\s*\\+)");
		// Dynamic field-fusion names are checked exhaustively rather than mistaking their prefix for an event.
		for (String id : dev.wildercord.cast.FieldFusionFeels.EFFECTS) {
			assertTrue(kit.contains("fieldfusion_" + id + "_cue"), id + " cue missing");
			assertTrue(kit.contains("fieldfusion_" + id + "_impact"), id + " impact missing");
		}
		for (String id : dev.wildercord.cast.NextSignatureFeels.EFFECTS) {
			assertTrue(kit.contains("nextsignature_" + id + "_cue"), id + " cue missing");
			assertTrue(kit.contains("nextsignature_" + id + "_impact"), id + " impact missing");
		}
		java.util.List<String> missing = new java.util.ArrayList<>();
		try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/java"))) {
			for (java.nio.file.Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				java.util.regex.Matcher m = named.matcher(java.nio.file.Files.readString(f));
				while (m.find()) {
					if (!kit.contains(m.group(1))) {
						missing.add(f.getFileName() + ": " + m.group(1));
					}
				}
			}
		}
		assertTrue(missing.isEmpty(), "kit sounds named in code but not authored: " + missing);
	}

	@Test
	void signaturesAdjustMotionAndScale() {
		Signature.of("wildercord:meteor").motion(Motion.CALL).scale(1.5).register();
		Feel f = Signatures.adjust(Feel.of(first(Runes.BOLT, Runes.METEOR), 24, 0));
		assertEquals(Motion.CALL, f.motion());
		assertTrue(f.scale() > Feel.of(first(Runes.BOLT, Runes.METEOR), 24, 0).scale());
		Signatures.clear();
	}
}
