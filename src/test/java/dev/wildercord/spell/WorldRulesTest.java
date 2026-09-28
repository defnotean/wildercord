package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static dev.wildercord.spell.WorldRules.Interaction.*;
import static org.junit.jupiter.api.Assertions.*;

/** Magic that changes the world: which effects touch the ground, the caps, and being wet. */
class WorldRulesTest {
	@Test
	void eachElementDoesItsOwnThingToTheWorld() {
		assertEquals(IGNITE, WorldRules.of(FIRE));
		assertEquals(IGNITE, WorldRules.of(EXPLODE));
		assertEquals(WorldRules.Interaction.FREEZE, WorldRules.of(FROST));
		assertEquals(WorldRules.Interaction.FREEZE, WorldRules.of(CHILL));
		assertEquals(CONDUCT, WorldRules.of(LIGHTNING));
		assertEquals(CONDUCT, WorldRules.of(SHOCK));
		assertEquals(GUST, WorldRules.of(PUSH));
		assertEquals(HEAVE, WorldRules.of(TREMOR));
		assertEquals(WorldRules.Interaction.BLOOM, WorldRules.of(HEAL));
		assertEquals(DRAW, WorldRules.of(PULL));
	}

	@Test
	void helpfulFireAndFrostLeaveTheWorldAlone() {
		// Fireward on a friend never burns the grass round them; Tidebreath never freezes the pond they swim in.
		assertEquals(NONE, WorldRules.of(FIREWARD));
		assertEquals(NONE, WorldRules.of(TIDEBREATH));
		assertEquals(NONE, WorldRules.of(FROSTWARD));
		assertEquals(NONE, WorldRules.of(SWIFT));
		assertEquals(NONE, WorldRules.of(THUNDERBIRD));
	}

	@Test
	void runesThatAlreadyChangeBlocksAreLeftToThemselves() {
		for (RuneDef rune : Set.of(GROW, HARVEST, ICEPATH, SMELT, BREAK, LIGHT, PRUNE)) {
			assertEquals(NONE, WorldRules.of(rune), rune.name());
		}
		// Bubble soaks rather than freezes; the pins hold creatures down rather than heave them up.
		assertEquals(NONE, WorldRules.of(BUBBLE));
		assertEquals(NONE, WorldRules.of(ROOT));
		assertEquals(NONE, WorldRules.of(SHACKLE));
	}

	@Test
	void onlyEffectsInteract() {
		assertEquals(NONE, WorldRules.of(BOLT));
		assertEquals(NONE, WorldRules.of(AMPLIFY));
		assertEquals(NONE, WorldRules.of(ON_HIT));
		assertEquals(NONE, WorldRules.of(null));
		// Elements without a world interaction.
		assertEquals(NONE, WorldRules.of(HARM));
		assertEquals(NONE, WorldRules.of(CLEAVE));
		assertEquals(NONE, WorldRules.of(STASIS));
	}

	@Test
	void onlySomeInteractionsChangeBlocks() {
		// These need building rights and never happen for a monster's spell.
		assertTrue(IGNITE.editsBlocks());
		assertTrue(WorldRules.Interaction.FREEZE.editsBlocks());
		assertTrue(WorldRules.Interaction.BLOOM.editsBlocks());
		assertTrue(GUST.editsBlocks());
		// These work for monsters too: a shock through water, heaved ground (block displays), items drawn in.
		assertFalse(CONDUCT.editsBlocks());
		assertFalse(HEAVE.editsBlocks());
		assertFalse(DRAW.editsBlocks());
		assertFalse(NONE.editsBlocks());
	}

	@Test
	void everyInteractionHasATooltipLine() {
		for (WorldRules.Interaction interaction : WorldRules.Interaction.values()) {
			assertTrue(interaction.tooltipKey().startsWith("tooltip.wildercord.world."), interaction.name());
		}
		assertEquals("tooltip.wildercord.world.freeze", WorldRules.Interaction.FREEZE.tooltipKey());
	}

	@Test
	void allowancesNeverRunPastTheirCaps() {
		assertEquals(3, WorldRules.allowance(10, WorldRules.IGNITE_MAX, 24));
		assertEquals(2, WorldRules.allowance(10, WorldRules.IGNITE_MAX, 2));
		assertEquals(1, WorldRules.allowance(1, WorldRules.IGNITE_MAX, 24));
		assertEquals(0, WorldRules.allowance(10, WorldRules.IGNITE_MAX, 0));
		assertEquals(0, WorldRules.allowance(10, WorldRules.IGNITE_MAX, -5));
		// A whole cast's world edits fit inside the cast's block budget for a single strike.
		assertTrue(WorldRules.EDITS_PER_CAST <= 32);
		assertTrue(WorldRules.FREEZE_MAX <= WorldRules.EDITS_PER_CAST);
	}

	@Test
	void theShockFadesThroughWater() {
		assertEquals(4.0, WorldRules.conductDamage(0), 1e-9);
		assertEquals(4.0, WorldRules.conductDamage(3), 1e-9);
		assertEquals(2.4, WorldRules.conductDamage(WorldRules.CONDUCT_RADIUS), 1e-9);
		assertEquals(2.4, WorldRules.conductDamage(40), 1e-9);
		assertTrue(WorldRules.conductDamage(5) < 4.0 && WorldRules.conductDamage(5) > 2.4);
		assertTrue(WorldRules.CONDUCTOR_FEAT <= WorldRules.CONDUCT_TARGETS, "Conductor must be reachable");
	}

	@Test
	void wetnessDullsFireOnly() {
		assertTrue(WorldRules.wet(true, false, false));
		assertTrue(WorldRules.wet(false, true, false));
		assertTrue(WorldRules.wet(false, false, true));
		assertFalse(WorldRules.wet(false, false, false));
		assertEquals(0.75, WorldRules.wetDamage("fire", true), 1e-9);
		assertEquals(1.0, WorldRules.wetDamage("fire", false), 1e-9);
		// Storm's bonus on the wet is Conduct's, not a second multiplier on top.
		assertEquals(1.0, WorldRules.wetDamage("storm", true), 1e-9);
		assertEquals(1.0, WorldRules.wetDamage("frost", true), 1e-9);
	}

	@Test
	void tidebreathLeavesYouWet() {
		assertTrue(WorldRules.wets(TIDEBREATH));
		assertFalse(WorldRules.wets(FIRE));
		assertFalse(WorldRules.wets(null));
	}

	@Test
	void theNewFeatsAreInTheGrimoire() {
		assertEquals("Conductor", Feats.feat(Feats.CONDUCTOR).name());
		assertEquals("Icebridge", Feats.feat(Feats.ICEBRIDGE).name());
		assertEquals(250, Feats.reward("feat:" + Feats.CONDUCTOR));
	}
}
