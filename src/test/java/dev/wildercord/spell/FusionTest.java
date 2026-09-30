package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Fusion Altar's rules: ranks, fusion recipes, and Knots (tying, reading, cost and nesting). */
class FusionTest {
	private static Fusions.Plan plan(Fusions.Catalyst catalyst, Fusions.Slot... slots) {
		return Fusions.plan(List.of(slots), catalyst);
	}

	private static RuneDef knot(String name, RuneDef... runes) {
		return Runes.get(Knots.id(List.of(runes), name)).orElseThrow();
	}

	// ------------------------------------------------------------------ ranks

	@Test
	void ranksAddAQuarterThenAHalfAtTheSameCost() {
		assertEquals(1.0, Ranks.power(1), 1e-9);
		assertEquals(1.25, Ranks.power(2), 1e-9);
		assertEquals(1.5, Ranks.power(3), 1e-9);
		assertEquals(1.5, Ranks.power(7), 1e-9, "ranks stop at III");
		assertEquals(2, Ranks.xpCost(2));
		assertEquals(5, Ranks.xpCost(3));
		assertEquals(0, Ranks.levels(2));
		assertEquals(1, Ranks.levels(3), "rank III counts as one Amplify for levels");
		assertEquals(" II", Ranks.suffix(2));
		assertEquals("", Ranks.suffix(1));
	}

	@Test
	void onlyEffectsWithPowerHaveRanks() {
		assertTrue(Ranks.rankable(FIRE));
		assertFalse(Ranks.rankable(FEATHER_FALL), "no power to grow");
		assertFalse(Ranks.rankable(BOLT));
		assertFalse(Ranks.rankable(AMPLIFY));
		assertFalse(Ranks.rankable(KINDLING), "innate runes grow with the heart instead");
	}

	@Test
	void theReadoutShowsTheRankAndItsPower() {
		SpellCompiler.Compiled plain = SpellCompiler.compile(List.of(BOLT, FIRE));
		assertEquals(List.of("A bolt: Fire"), plain.lines());
		SpellCompiler.Compiled ranked = SpellCompiler.compile(List.of(BOLT, FIRE), id -> id.equals(FIRE.id()) ? 2 : 1);
		assertEquals(List.of("A bolt: Fire II (+25% power)"), ranked.lines());
		assertEquals(plain.cost(), ranked.cost(), 1e-9, "a rank never changes the mana cost");
		SpellCompiler.Compiled amplified = SpellCompiler.compile(List.of(BOLT, FIRE, AMPLIFY), id -> 3);
		assertEquals(List.of("A bolt: Fire III (+125% power)"), amplified.lines());
	}

	// ------------------------------------------------------------------ combining

	@Test
	void recipesMatchOnElementsInEitherOrder() {
		assertSame(FIRESTORM, Fusions.recipe(FIRE, PUSH).orElseThrow().result());
		assertSame(FIRESTORM, Fusions.recipe(PUSH, FIRE).orElseThrow().result());
		// Any effect of the element counts, not one particular rune.
		assertSame(FIRESTORM, Fusions.recipe(EMBER, CYCLONE).orElseThrow().result());
		assertSame(STEAM, Fusions.recipe(FIRE, FROST).orElseThrow().result());
		assertSame(MAGMA, Fusions.recipe(PELT, FIRE).orElseThrow().result());
		assertSame(TEMPEST, Fusions.recipe(SHOCK, PUSH).orElseThrow().result());
		assertSame(PLASMA, Fusions.recipe(SHOCK, EMBER).orElseThrow().result());
		assertSame(HAIL, Fusions.recipe(JOLT, ICICLE).orElseThrow().result());
		assertSame(GLACIER, Fusions.recipe(FROST, ROOT).orElseThrow().result());
		assertSame(LIFESTEAL, Fusions.recipe(HEAL, PULL).orElseThrow().result());
		assertSame(WARP, Fusions.recipe(DASH, BLINK).orElseThrow().result());
		assertSame(BLOOM, Fusions.recipe(REGROWTH, STONESKIN).orElseThrow().result());
		assertSame(SURGE, Fusions.recipe(HEAL, SHOCK).orElseThrow().result());
		assertSame(NULLIFY, Fusions.recipe(HARM, HEX).orElseThrow().result());
		// Every pair fuses now, one element with itself too.
		assertSame(Runes.CONFLAGRATION, Fusions.recipe(FIRE, EMBER).orElseThrow().result());
		assertSame(Runes.BLOODBOIL, Fusions.recipe(FIRE, BLEED).orElseThrow().result());
		assertSame(Runes.BLOODBOIL, Fusions.recipe(BLEED, EMBER).orElseThrow().result());
		assertSame(Runes.RECKONING, Fusions.recipe(Runes.STASIS, BLEED).orElseThrow().result());
		assertSame(Runes.SINGULARITY, Fusions.recipe(PULL, Runes.WITHER).orElseThrow().result());
		assertTrue(Fusions.recipe(FIRE, BOLT).isEmpty(), "only effects fuse");
		assertTrue(Fusions.recipe(KINDLING, PUSH).isEmpty(), "innate runes never fuse");
	}

	@Test
	void everyFusedRuneIsATierThreeEffectMadeOnlyOneWay() {
		// One fusion for every pair of the ten elements (45), and one for each element with itself (10).
		assertEquals(55, Fusions.RECIPES.size());
		assertEquals(Runes.FUSED.size(), Fusions.RECIPES.size());
		List<String> elements = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood");
		for (int i = 0; i < elements.size(); i++) {
			for (int j = i; j < elements.size(); j++) {
				String a = elements.get(i);
				String b = elements.get(j);
				assertEquals(1, Fusions.RECIPES.stream().filter(r -> r.takes(a, b)).count(), a + " + " + b + " should make exactly one rune");
			}
		}
		for (Fusions.Recipe recipe : Fusions.RECIPES) {
			RuneDef made = recipe.result();
			assertEquals(RuneFamily.EFFECT, made.family(), made.name());
			assertEquals(3, made.tier(), made.name());
			assertTrue(Runes.fused(made), made.name());
			assertEquals(recipe, Fusions.recipeFor(made).orElseThrow());
			assertEquals("fusion:" + made.path(), recipe.key());
			// A fused rune counts as one of the two elements it was made from.
			assertTrue(made.element().equals(recipe.first()) || made.element().equals(recipe.second())
				|| made == LIFESTEAL, made.name() + " should be of " + recipe.first() + " or " + recipe.second());
		}
		assertEquals(Fusions.RECIPES.size(), Fusions.RECIPES.stream().map(r -> r.result().id()).distinct().count());
	}

	// ------------------------------------------------------------------ signature fusions

	@Test
	void aSignaturePairMakesItsOwnRuneInsteadOfItsElementsFusion() {
		assertSame(FROSTWIRE, Fusions.recipe(CHILL, SHOCK).orElseThrow().result());
		assertTrue(Fusions.recipe(CHILL, SHOCK).orElseThrow().signature());
		// Any other frost and storm effects still make Hail.
		assertSame(HAIL, Fusions.recipe(CHILL, JOLT).orElseThrow().result());
		assertSame(HAIL, Fusions.recipe(ICICLE, SHOCK).orElseThrow().result());
		assertSame(HAIL, Fusions.recipe(FROST, Runes.LIGHTNING).orElseThrow().result());
		assertFalse(Fusions.recipe(CHILL, JOLT).orElseThrow().signature());
		// A signature rune with itself, or with the other rune of its pair twice, is no signature: that's its elements' own.
		assertSame(Runes.ABSOLUTE_ZERO, Fusions.recipe(CHILL, CHILL).orElseThrow().result());
		assertSame(Runes.THUNDERHEAD, Fusions.recipe(SHOCK, SHOCK).orElseThrow().result());
		// Runes of the world and Tier IV runes make signatures too.
		assertSame(Runes.DUST_DEVIL, Fusions.recipe(Runes.SUMMIT_WIND, Runes.SANDSTORM).orElseThrow().result());
		assertSame(Runes.COMETFALL, Fusions.recipe(Runes.METEOR, Runes.STARFALL).orElseThrow().result());
		assertSame(Runes.DOWNDRAFT, Fusions.recipe(Runes.SUMMIT_WIND, PELT).orElseThrow().result(), "only the pair itself");
	}

	@Test
	void signatureOrderDoesntMatter() {
		for (Fusions.Signature signature : Fusions.SIGNATURES) {
			assertSame(signature, Fusions.recipe(signature.a(), signature.b()).orElseThrow(), signature.result().name());
			assertSame(signature, Fusions.recipe(signature.b(), signature.a()).orElseThrow(), signature.result().name() + ", the other way round");
			assertSame(signature, Fusions.signature(signature.b(), signature.a()).orElseThrow());
		}
	}

	@Test
	void theAltarPlansASignatureAndKeepsTheLowerRank() {
		Fusions.Plan plan = plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(SHOCK, 3), Fusions.Slot.EMPTY, Fusions.Slot.of(CHILL, 2));
		assertTrue(plan.ready(), String.valueOf(plan.problem()));
		assertEquals(Fusions.Kind.COMBINE, plan.kind());
		assertTrue(plan.signature());
		assertSame(FROSTWIRE, plan.result());
		assertEquals(2, plan.rank(), "the lower of the two ranks put in, as any fusion");
		assertEquals(Fusions.COMBINE_XP, plan.xp(), "the same cost as any fusion");
		assertEquals("fusion:frostwire", plan.recipe().key());
		Fusions.Plan three = plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(SHOCK, 3), Fusions.Slot.of(CHILL, 3), Fusions.Slot.EMPTY);
		assertEquals(3, three.rank());
		// Bloomstep has no power to rank: it always comes out plain.
		Fusions.Plan plain = plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(GROW, 3), Fusions.Slot.of(BLINK, 3), Fusions.Slot.EMPTY);
		assertSame(Runes.BLOOMSTEP, plain.result());
		assertEquals(1, plain.rank());
		// Without a shard it's refused, as any combine.
		assertNotNull(plan(Fusions.Catalyst.NONE, Fusions.Slot.of(SHOCK, 1), Fusions.Slot.of(CHILL, 1), Fusions.Slot.EMPTY).problem());
		// The element fusion of the same elements says it isn't a signature.
		Fusions.Plan hail = plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(JOLT, 1), Fusions.Slot.of(CHILL, 1), Fusions.Slot.EMPTY);
		assertSame(HAIL, hail.result());
		assertFalse(hail.signature());
	}

	@Test
	void everySignatureIsAFusedRuneOfItsOwnFromTwoRunesACasterCanFind() {
		assertEquals(16, Fusions.SIGNATURES.size());
		assertEquals(Runes.SIGNATURE.size(), Fusions.SIGNATURES.size());
		java.util.Set<java.util.Set<String>> pairs = new java.util.HashSet<>();
		java.util.Map<String, Integer> uses = new java.util.HashMap<>();
		for (Fusions.Signature signature : Fusions.SIGNATURES) {
			RuneDef made = signature.result();
			assertTrue(Runes.SIGNATURE.contains(made), made.name());
			assertTrue(Runes.fused(made), made.name());
			assertFalse(Runes.common(made), made.name() + " is never found or sold");
			assertTrue(Runes.obtainable(made), made.name());
			assertEquals(RuneFamily.EFFECT, made.family(), made.name());
			assertTrue(made.tier() == 3 || made.tier() == 4, made.name());
			assertTrue(Fusions.isSignature(made));
			assertEquals(signature, Fusions.recipeFor(made).orElseThrow());
			assertEquals("fusion:" + made.path(), signature.key());
			assertFalse(Fusions.RECIPES.stream().anyMatch(r -> r.result() == made), made.name() + " is made only by its pair");
			// It wears its two runes' elements, and counts as one of them.
			assertTrue(made.element().equals(signature.first()) || made.element().equals(signature.second()), made.name());
			for (RuneDef rune : List.of(signature.a(), signature.b())) {
				assertTrue(Fusions.fusible(rune), rune.name());
				assertFalse(Runes.fused(rune), rune.name() + ": never a fused rune");
				assertFalse(Runes.innate(rune), rune.name() + ": never an innate rune");
				assertTrue(Runes.obtainable(rune), rune.name());
				uses.merge(rune.element(), 1, Integer::sum);
			}
			assertNotEquals(signature.a(), signature.b());
			assertTrue(pairs.add(java.util.Set.of(signature.first(), signature.second())), made.name() + ": two signatures share a pair of elements");
			assertNotEquals(made, signature.overrides().orElseThrow().result());
		}
		assertEquals(Fusions.SIGNATURES.size(), Fusions.SIGNATURES.stream().map(s -> s.result().id()).distinct().count());
		for (String element : List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood")) {
			assertTrue(uses.getOrDefault(element, 0) >= 2, element + " should be in at least two signature pairs");
		}
	}

	@Test
	void theGrimoireCountsElementFusionsAndSignaturesApart() {
		List<String> found = List.of("fusion:firestorm", "fusion:frostwire", "fusion:seethe", "reaction:shatter");
		assertEquals(1, Fusions.elementFusionsFound(found));
		assertEquals(2, Fusions.signaturesFound(found));
		assertEquals(Feats.reward("fusion:firestorm"), Feats.reward("fusion:frostwire"), "a signature condenses as much as any fusion");
	}

	// ------------------------------------------------------------------ what the altar works out

	@Test
	void threeOfTheSameRuneRankItUp() {
		Fusions.Plan two = plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(FIRE, 1));
		assertTrue(two.ready(), String.valueOf(two.problem()));
		assertEquals(Fusions.Kind.UPGRADE, two.kind());
		assertSame(FIRE, two.result());
		assertEquals(2, two.rank());
		assertEquals(2, two.xp());
		Fusions.Plan three = plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FIRE, 2), Fusions.Slot.of(FIRE, 2), Fusions.Slot.of(FIRE, 2));
		assertEquals(3, three.rank());
		assertEquals(5, three.xp());
		assertFalse(plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FIRE, 3), Fusions.Slot.of(FIRE, 3), Fusions.Slot.of(FIRE, 3)).ready(), "III is the top");
		assertFalse(plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(FIRE, 2), Fusions.Slot.of(FIRE, 1)).ready(), "ranks must match");
		assertFalse(plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(FROST, 1), Fusions.Slot.of(FIRE, 1)).ready());
		assertFalse(plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FEATHER_FALL, 1), Fusions.Slot.of(FEATHER_FALL, 1), Fusions.Slot.of(FEATHER_FALL, 1)).ready());
		assertFalse(plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(FIRE, 1)).ready(), "no catalyst for an upgrade");
	}

	@Test
	void twoEffectsAndAShardCombine() {
		Fusions.Plan combine = plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(FIRE, 2), Fusions.Slot.EMPTY, Fusions.Slot.of(PUSH, 1));
		assertTrue(combine.ready(), String.valueOf(combine.problem()));
		assertEquals(Fusions.Kind.COMBINE, combine.kind());
		assertSame(FIRESTORM, combine.result());
		assertEquals(1, combine.rank(), "a fused rune starts at rank I");
		assertEquals(3, combine.xp());
		Fusions.Plan noShard = plan(Fusions.Catalyst.NONE, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(PUSH, 1), Fusions.Slot.EMPTY);
		assertEquals(Fusions.Kind.COMBINE, noShard.kind());
		assertNotNull(noShard.problem());
		assertNotNull(plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(BOLT, 1), Fusions.Slot.EMPTY).problem(), "only effects fuse");
		Fusions.Plan pure = plan(Fusions.Catalyst.SHARD, Fusions.Slot.of(FIRE, 1), Fusions.Slot.of(EMBER, 1), Fusions.Slot.EMPTY);
		assertTrue(pure.ready(), String.valueOf(pure.problem()));
		assertSame(Runes.CONFLAGRATION, pure.result(), "two effects of one element make that element at its purest");
	}

	@Test
	void amethystBlockWeavesEveryPairIntoOneUsableRune() {
		RuneDef first = WovenRunes.bind(FIRE, HEAL);
		assertEquals(first.id(), WovenRunes.bind(HEAL, FIRE).id());
		assertEquals(List.of(FIRE, HEAL), WovenRunes.contents(first));
		assertEquals(first, Runes.get(first.id()).orElseThrow());
		assertEquals(RuneFamily.EFFECT, first.family());
		assertFalse(Fusions.fusible(first), "woven pairs cannot nest indefinitely");
		Fusions.Plan woven = plan(Fusions.Catalyst.BLOCK, Fusions.Slot.of(FIRE, 2), Fusions.Slot.of(HEAL, 3), Fusions.Slot.EMPTY);
		assertTrue(woven.ready(), String.valueOf(woven.problem()));
		assertEquals(first, woven.result());
		assertEquals(2, woven.rank());
		assertNull(woven.recipe());
		SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(BOLT, first));
		assertEquals(2, compiled.root().groups.getFirst().effects.size());
		assertEquals(SpellCompiler.compile(List.of(BOLT, FIRE, HEAL)).cost(), compiled.cost(), 1e-6);
	}

	@Test
	void everyFusibleEffectPairHasItsOwnWovenRune() {
		List<RuneDef> effects = Runes.all().stream().filter(Fusions::fusible).toList();
		var ids = new HashSet<String>();
		for (int i = 0; i < effects.size(); i++) {
			for (int j = i; j < effects.size(); j++) {
				RuneDef a = effects.get(i);
				RuneDef b = effects.get(j);
				RuneDef woven = WovenRunes.bind(a, b);
				assertTrue(ids.add(woven.id()), "each unordered pair needs a distinct result");
				assertEquals(woven, Runes.get(woven.id()).orElseThrow());
			}
		}
		assertEquals(effects.size() * (effects.size() + 1) / 2, ids.size());
	}

	@Test
	void aBlankRuneAndStringTieAKnot() {
		Fusions.Plan knot = plan(Fusions.Catalyst.STRING, Fusions.Slot.EMPTY, Fusions.Slot.BLANK, Fusions.Slot.EMPTY);
		assertTrue(knot.ready());
		assertEquals(Fusions.Kind.KNOT, knot.kind());
		assertFalse(plan(Fusions.Catalyst.NONE, Fusions.Slot.BLANK, Fusions.Slot.EMPTY, Fusions.Slot.EMPTY).ready(), "string too");
		assertFalse(plan(Fusions.Catalyst.STRING, Fusions.Slot.BLANK, Fusions.Slot.of(FIRE, 1), Fusions.Slot.EMPTY).ready(), "only the blank");
		assertEquals(Fusions.Kind.NONE, plan(Fusions.Catalyst.NONE, Fusions.Slot.EMPTY, Fusions.Slot.EMPTY, Fusions.Slot.EMPTY).kind());
	}

	// ------------------------------------------------------------------ Knots

	@Test
	void base32RoundTrips() {
		for (String text : List.of("", "a", "bolt,fire", "self,heal|Mend Me", "été")) {
			byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
			assertArrayEquals(bytes, Knots.decode(Knots.encode(bytes)), text);
		}
		assertNull(Knots.decode("ABC!"));
	}

	@Test
	void aKnotCarriesItsSpellInItsId() {
		String id = Knots.id(List.of(BOLT, FIRE, SPLIT_MOD), "");
		assertTrue(id.startsWith(Knots.PREFIX));
		assertTrue(id.matches("wildercord:knot/[a-z2-7]+"), "a valid resource path: " + id);
		RuneDef knot = Runes.get(id).orElseThrow();
		assertEquals(RuneFamily.KNOT, knot.family());
		assertEquals(List.of(BOLT, FIRE, SPLIT_MOD), Knots.contents(knot));
		assertEquals(3, knot.tier(), "as strong as its strongest rune, so a Cord's tier limit still holds");
		assertEquals("Bolt · Fire · Split", knot.description());
		assertEquals("knot", knot.category());
		RuneDef named = knot("Sunburst", SELF, HEAL);
		assertEquals("Sunburst", named.name());
		assertEquals("Sunburst", Knots.customName(named));
		assertEquals(1, named.tier());
		assertTrue(Runes.get("wildercord:knot/!!").isEmpty());
		assertTrue(Runes.get(Knots.PREFIX + Knots.encode("bolt,nonsense_rune".getBytes(StandardCharsets.UTF_8))).isEmpty(), "an unknown rune makes it silent");
	}

	@Test
	void aKnotCostsTenPercentLessThanItsRunes() {
		double loose = SpellCompiler.compile(List.of(BOLT, FIRE, AMPLIFY)).cost();
		double tied = SpellCompiler.compile(List.of(knot("", BOLT, FIRE, AMPLIFY))).cost();
		assertEquals(loose * 0.9, tied, 1e-9);
		// A Knot in a Knot: its runes are discounted twice.
		double nested = SpellCompiler.compile(List.of(knot("", knot("", BOLT, FIRE)))).cost();
		assertEquals(SpellCompiler.compile(List.of(BOLT, FIRE)).cost() * 0.81, nested, 1e-9);
		// Loose runes beside a Knot pay full price.
		double mixed = SpellCompiler.compile(List.of(SELF, HEAL, knot("", SELF, SWIFT))).cost();
		assertEquals(SELF.cost() + HEAL.cost() + 0.9 * SWIFT.cost(), mixed, 1e-9);
	}

	@Test
	void theCompilerUntiesAKnotIntoItsRunes() {
		SpellCompiler.Compiled c = SpellCompiler.compile(List.of(knot("", BOLT, FIRE), ON_HIT, BURST, FROST));
		SpellPlan.Group bolt = c.root().groups.getFirst();
		assertSame(BOLT, bolt.shape);
		assertSame(FIRE, bolt.effects.getFirst().effect);
		assertNotNull(c.root().link);
		assertSame(ON_HIT, c.root().link.link);
		assertSame(bolt, c.root().link.anchor);
		assertTrue(c.lines().stream().anyMatch(line -> line.contains("is a Knot: 2 runes in one socket, 10% less mana")), c.lines().toString());
		assertEquals(SpellCompiler.NOT_A_MODIFIER, c.attachedTo()[0]);
	}

	@Test
	void aKnotIsSealed() {
		// A modifier outside can't reach the runes inside...
		SpellCompiler.Compiled outside = SpellCompiler.compile(List.of(knot("", BOLT, FIRE), AMPLIFY));
		assertTrue(outside.root().groups.getFirst().effects.getFirst().mods.isEmpty());
		assertEquals(SpellCompiler.UNATTACHED, outside.attachedTo()[1]);
		// ...and one inside changes only what's inside.
		SpellCompiler.Compiled inside = SpellCompiler.compile(List.of(SELF, HEAL, knot("", AMPLIFY)));
		assertTrue(inside.root().groups.getFirst().effects.getFirst().mods.isEmpty());
		// Within the Knot, modifiers work as usual.
		SpellCompiler.Compiled own = SpellCompiler.compile(List.of(knot("", BOLT, FIRE, AMPLIFY)));
		assertEquals(List.of(AMPLIFY), own.root().groups.getFirst().effects.getFirst().mods);
	}

	@Test
	void knotsGoTwoDeepAtMost() {
		RuneDef inner = knot("", SELF, HEAL);
		RuneDef outer = knot("", inner, SWIFT);
		assertEquals(1, Knots.innerDepth(List.of(inner)));
		assertEquals(2, Knots.innerDepth(List.of(outer)));
		assertNull(Knots.problem(List.of(inner, SWIFT)), "a Knot may hold a Knot");
		assertNotNull(Knots.problem(List.of(outer)), "but not one that already holds a Knot");
		// A hand-made id three deep doesn't read at all.
		String tooDeep = Knots.id(List.of(outer), "");
		assertTrue(Runes.get(tooDeep).isEmpty());
		assertEquals(List.of(SELF, HEAL, SWIFT), Knots.flatten(List.of(outer)));
	}

	@Test
	void whatCanBeTiedAndWhatItCosts() {
		assertNotNull(Knots.problem(List.of()), "nothing to tie");
		assertNotNull(Knots.problem(List.of(SELF, IMBUE, FIRE)), "no Imbue in a Knot");
		assertNull(Knots.problem(List.of(BOLT, FIRE)));
		assertEquals(2, Knots.xpCost(List.of(FIRE)), "at least 2");
		assertEquals(2, Knots.xpCost(List.of(BOLT, FIRE)));
		assertEquals(4, Knots.xpCost(List.of(BOLT, FIRE, AMPLIFY, SPLIT_MOD)));
		assertEquals(5, Knots.xpCost(List.of(knot("", SELF, HEAL, SWIFT), BOLT, FIRE)), "one level per rune inside, a Knot's included");
	}

	@Test
	void knotsAreNeverPassivesAndNotOnTheRoster() {
		RuneDef tied = knot("", SELF, SWIFT);
		assertFalse(Passives.allowed(tied));
		assertFalse(Runes.all().contains(tied));
		assertEquals(Optional.of(tied), Runes.get(tied.id()));
	}
}
