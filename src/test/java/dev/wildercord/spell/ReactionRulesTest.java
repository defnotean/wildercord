package dev.wildercord.spell;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.wildercord.spell.ReactionRules.*;
import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The newer element reactions: their numbers, which runes leave their marks and which set them off (as
 * rune tooltips say), and that every element now takes part in a reaction.
 */
class ReactionRulesTest {
	private static final double EPSILON = 1.0E-9;

	@Test
	void unweaveNeedsTwoMarksAndCountsFourAtMost() {
		assertEquals(1.0, ReactionRules.unweave(0), EPSILON);
		assertEquals(1.0, ReactionRules.unweave(1), EPSILON, "one mark isn't enough");
		assertEquals(1.6, ReactionRules.unweave(2), EPSILON);
		assertEquals(1.9, ReactionRules.unweave(3), EPSILON);
		assertEquals(2.2, ReactionRules.unweave(4), EPSILON);
		assertEquals(2.2, ReactionRules.unweave(9), EPSILON, "at most four marks count");
	}

	@Test
	void elapseCountsWhatBurningPoisonAndWitherStillHadToDeal() {
		// Burning: 1 a second.
		assertEquals(4.5, ReactionRules.lingering(90, 0, 0, 0, 0), EPSILON);
		// Poison I wounds every 25 ticks, Poison II every 12; Wither III every 10.
		assertEquals(4.0, ReactionRules.lingering(0, 100, 0, 0, 0), EPSILON);
		assertEquals(8.0, ReactionRules.lingering(0, 100, 1, 0, 0), EPSILON);
		assertEquals(16.0, ReactionRules.lingering(0, 0, 0, 160, 2), EPSILON);
		assertEquals(12, ReactionRules.poisonInterval(1));
		assertEquals(40, ReactionRules.witherInterval(0));
		assertEquals(1, ReactionRules.witherInterval(9), "never a wound every 0 ticks");
		// All of them together.
		assertEquals(4.5 + 4.0 + 2.0, ReactionRules.lingering(90, 100, 0, 80, 0), EPSILON);
	}

	@Test
	void elapseDealsHalfAgainBetweenThreeAndSixteen() {
		assertEquals(0.0, ReactionRules.elapse(0), EPSILON, "nothing still to come: no Elapse");
		assertEquals(3.0, ReactionRules.elapse(0.5), EPSILON, "a burn almost out still lands for 3");
		assertEquals(6.75, ReactionRules.elapse(4.5), EPSILON);
		assertEquals(16.0, ReactionRules.elapse(40), EPSILON, "at most 16");
	}

	@Test
	void theNewerReactionsAreBalancedAgainstTheFirstFive() {
		// Shatter is +60% and Conduct +50%: none of the newer ones' hits goes past that on its own.
		for (double bonus : List.of(OVERLOAD_BONUS, FRACTURE_BONUS, RUPTURE_BONUS)) {
			assertTrue(bonus > 1.0 && bonus <= 1.6, "bonus " + bonus);
		}
		// Unweave needs two marks for Shatter's +60%; four for its best.
		assertEquals(1.6, ReactionRules.unweave(UNWEAVE_MIN_MARKS), EPSILON);
		assertTrue(CRACKED_BONUS < FRACTURE_BONUS);
		assertTrue(BLIGHT_REACH >= 2);
	}

	@Test
	void voidsCursesShadowAndBloodsCutsBleed() {
		for (RuneDef rune : List.of(HEX, BLIND, WITHER, ECHOLOCATE, HUSH, ECLIPSE, RESONANT_SHRIEK, BLACKFLAME, ENTROPY)) {
			assertEquals(SHADOWED, ReactionRules.marks(rune), rune.name());
			assertEquals("void", rune.element(), rune.name());
		}
		for (RuneDef rune : List.of(BLEED, REND, CLEAVE, DISMANTLE, CRIMSON_MIST, BONESPUR)) {
			assertEquals(BLEEDING, ReactionRules.marks(rune), rune.name());
		}
		assertNull(ReactionRules.marks(FIRE));
		assertNull(ReactionRules.marks(PULL), "pulled is its own mark, for Implode and Collapse");
		assertNull(ReactionRules.marks(BOLT));
		assertNull(ReactionRules.marks(null));
		assertEquals(BLIGHT, ReactionRules.reactionFor(SHADOWED));
		assertEquals(RUPTURE, ReactionRules.reactionFor(BLEEDING));
	}

	@Test
	void everyRuneNamedIsRealAndHarmful() {
		Set<String> named = new HashSet<>(SHADOWS);
		named.addAll(BLEEDS);
		for (String path : named) {
			RuneDef rune = Runes.get("wildercord:" + path).orElse(null);
			assertNotNull(rune, "no rune " + path);
			assertEquals(EffectKind.HARMFUL, rune.kind(), path);
		}
	}

	@Test
	void damageOfEachTriggeringElementSetsItsReactionOff() {
		assertEquals(OVERLOAD, ReactionRules.triggers(SHOCK));
		assertEquals(OVERLOAD, ReactionRules.triggers(HEARTSTOPPER), "Heartstopper's shocks are storm");
		assertEquals(OVERLOAD, ReactionRules.triggers(THUNDERBIRD), "Thunderbird's strikes are storm");
		assertEquals(FRACTURE, ReactionRules.triggers(PELT));
		assertEquals(FRACTURE, ReactionRules.triggers(TREMOR));
		assertEquals(FRACTURE, ReactionRules.triggers(BONESPUR));
		assertEquals(FRACTURE, ReactionRules.triggers(TUSK_CHARGE), "Tusk Charge tosses what it runs through");
		assertEquals(BLIGHT, ReactionRules.triggers(VENOM));
		assertEquals(BLIGHT, ReactionRules.triggers(VINELASH));
		assertEquals(BLIGHT, ReactionRules.triggers(BRAMBLE), "Bramble's thorns are life");
		assertEquals(UNWEAVE, ReactionRules.triggers(HARM));
		assertEquals(UNWEAVE, ReactionRules.triggers(SMITE));
		assertEquals(RUPTURE, ReactionRules.triggers(WINDCUT));
		assertEquals(RUPTURE, ReactionRules.triggers(CYCLONE));
		assertEquals(RUPTURE, ReactionRules.triggers(REPEL));
		assertEquals(ELAPSE, ReactionRules.triggers(COUNTDOWN));
		assertEquals(ELAPSE, ReactionRules.triggers(RECKONING));
	}

	@Test
	void runesThatDealNoDamageSetNothingOff() {
		for (RuneDef rune : List.of(ROOT, WEIGH, SHACKLE, MIRE, SPOREBLOOM, REVEAL, SILENCE, PUSH, LAUNCH, LEVITATE, STASIS, TIMESTEAL, HEAL)) {
			assertNull(ReactionRules.triggers(rune), rune.name());
		}
		// Prismatic Burst uses the marks up itself.
		assertNull(ReactionRules.triggers(PRISMATIC_BURST));
		// Fire and frost make their marks and set off the first five.
		assertNull(ReactionRules.triggers(FIRE));
		assertNull(ReactionRules.triggers(FROST));
		assertNull(ReactionRules.triggers(BOLT));
	}

	@Test
	void everyElementTakesPart() {
		// The first five: fire and storm set them off; frost, wind and void leave the marks.
		Set<String> taking = new HashSet<>(Set.of("fire", "storm", "frost", "wind", "void"));
		// The newer ones: each is set off by an element's damage...
		taking.addAll(ReactionRules.TRIGGER.values());
		// ...on a mark another element leaves.
		for (String path : SHADOWS) {
			taking.add(Runes.get("wildercord:" + path).orElseThrow().element());
		}
		for (String path : BLEEDS) {
			taking.add(Runes.get("wildercord:" + path).orElseThrow().element());
		}
		assertEquals(Set.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"), taking);
		assertEquals(Set.copyOf(NEWER), ReactionRules.TRIGGER.keySet(), "each newer reaction has one element that sets it off");
	}

	@Test
	void theGrimoireListsEveryReactionAndTheSixthCircleAsksForAnyFive() {
		assertTrue(Feats.REACTIONS.containsAll(NEWER));
		assertEquals(Feats.REACTIONS.size(), Set.copyOf(Feats.REACTIONS).size());
		assertEquals(11, Feats.REACTIONS.size());
		assertTrue(Circles.requirements(6).contains(new Circles.Requirement(Circles.Need.REACTIONS, 5)));
		assertTrue(Feats.REACTIONS.size() > 5, "five different reactions, not every one");
		for (String reaction : NEWER) {
			assertEquals(150, Feats.reward(Feats.reactionKey(reaction)), "worth the same as the first five");
		}
	}

	@Test
	void everyReactionHasItsWordsAndItsAdvancement() throws IOException {
		Path resources = Path.of("src/main/resources");
		JsonObject lang = JsonParser.parseString(Files.readString(resources.resolve("assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		for (String reaction : Feats.REACTIONS) {
			for (String key : List.of("reaction.wildercord." + reaction, "reaction.wildercord." + reaction + ".desc", "contract.wildercord.reaction." + reaction)) {
				assertTrue(lang.has(key), "no text for " + key + ": add it in tools/generate_assets.py");
			}
			assertTrue(Files.exists(resources.resolve("data/wildercord/advancement/discovery/" + reaction + ".json")), "no advancement for " + reaction);
		}
		for (String reaction : NEWER) {
			assertTrue(lang.has(triggerKey(reaction)), "no tooltip line for what sets off " + reaction);
		}
		for (String mark : List.of(SHADOWED, BLEEDING)) {
			assertTrue(lang.has(markKey(mark)), "no tooltip line for the " + mark + " mark");
		}
	}
}
