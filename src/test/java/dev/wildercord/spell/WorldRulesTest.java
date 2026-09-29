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
		assertEquals(AGE, WorldRules.of(COUNTDOWN));
		assertEquals(SHIMMER, WorldRules.of(HARM));
		assertEquals(FEED, WorldRules.of(CLEAVE));
		assertEquals(FEED, WorldRules.of(LEECH));
	}

	@Test
	void timeAgesTheWorldFromHelpfulSpellsToo() {
		// Like life, time's helpful spells change the world too: Accelerate on a field ripens it.
		assertEquals(AGE, WorldRules.of(ACCELERATE));
		assertEquals(AGE, WorldRules.of(CHRONOSHIFT));
		// But not the ones that stop time or turn it back.
		assertEquals(NONE, WorldRules.of(STASIS));
		assertEquals(NONE, WorldRules.of(REWIND));
		// Arcane's and blood's helpful spells leave the world alone.
		assertEquals(NONE, WorldRules.of(NIGHT_EYE));
		assertEquals(NONE, WorldRules.of(OVERDRIVE));
	}

	@Test
	void runesThatCallLightningLeaveCopperAndRodsToIt() {
		assertTrue(WorldRules.callsLightning(LIGHTNING));
		assertTrue(WorldRules.callsLightning(TEMPEST));
		assertFalse(WorldRules.callsLightning(SHOCK));
		assertFalse(WorldRules.callsLightning(null));
		// Either way, they're storm runes and conduct through water.
		assertEquals(CONDUCT, WorldRules.of(TEMPEST));
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
		// Movement runes never touch the world where they land.
		assertEquals(NONE, WorldRules.of(TIME_SKIP));
	}

	@Test
	void onlySomeInteractionsChangeBlocks() {
		// These change blocks, which needs building rights and never happens for a monster's spell.
		assertTrue(IGNITE.editsBlocks());
		assertTrue(WorldRules.Interaction.FREEZE.editsBlocks());
		assertTrue(WorldRules.Interaction.BLOOM.editsBlocks());
		assertTrue(GUST.editsBlocks());
		assertTrue(AGE.editsBlocks());
		assertTrue(FEED.editsBlocks());
		// Storm scrapes copper and pulses rods (blocks), though its shock through water works for monsters too.
		assertTrue(CONDUCT.editsBlocks());
		// These never change a block: heaved ground (block displays), items drawn in, shelves shimmering.
		assertFalse(HEAVE.editsBlocks());
		assertFalse(DRAW.editsBlocks());
		assertFalse(SHIMMER.editsBlocks());
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
		assertTrue(WorldRules.CRUST_MAX <= WorldRules.EDITS_PER_CAST);
		// Every per-hit cap of the second layer stays modest: a few blocks a hit.
		for (int cap : new int[] {WorldRules.KINDLE_MAX, WorldRules.COPPER_MAX, WorldRules.RODS_MAX, WorldRules.AGE_MAX,
				WorldRules.AGE_BABIES, WorldRules.FURNACE_MAX, WorldRules.FEED_MAX}) {
			assertTrue(cap > 0 && cap <= 6, "cap " + cap);
		}
	}

	@Test
	void theCrustWarnsBeforeItMelts() {
		// It holds 20 to 30 seconds, and glows for its last few before it goes.
		assertTrue(WorldRules.CRUST_TICKS >= 400 && WorldRules.CRUST_TICKS <= 600);
		assertTrue(WorldRules.CRUST_WARN_TICKS >= 60 && WorldRules.CRUST_WARN_TICKS < WorldRules.CRUST_TICKS / 2);
	}

	@Test
	void aCreeperIsOnlySometimesCharged() {
		assertTrue(WorldRules.CREEPER_CHARGE_CHANCE > 0 && WorldRules.CREEPER_CHARGE_CHANCE < 0.5);
	}

	@Test
	void aFurnaceJumpsAheadButNeverFinishesByItselfOrOutrunsItsFuel() {
		// A furnace (200 ticks an item) 10 ticks in, with plenty of coal: half a smelt on.
		assertEquals(WorldRules.FURNACE_SKIP_TICKS, WorldRules.furnaceSkip(10, 200, 1600));
		// A smoker (100 ticks an item) 10 ticks in: up to the tick before it finishes, which it does itself.
		assertEquals(89, WorldRules.furnaceSkip(10, 100, 1600));
		// Fuel for 30 more ticks: only as far as the fire lasts, and never out.
		assertEquals(29, WorldRules.furnaceSkip(0, 200, 30));
		// Already on its last tick, or out of fuel: nothing.
		assertEquals(0, WorldRules.furnaceSkip(199, 200, 1600));
		assertEquals(0, WorldRules.furnaceSkip(0, 200, 0));
	}

	@Test
	void aBabyGrowsUpALittleButNoFurther() {
		// Newborn (-24000): two minutes older.
		assertEquals(-24000 + WorldRules.AGE_BABY_SECONDS * 20, WorldRules.agedBaby(-24000));
		// Nearly grown: grown, never past.
		assertEquals(0, WorldRules.agedBaby(-100));
		// Grown ups (and their breeding cooldowns) are left alone.
		assertEquals(0, WorldRules.agedBaby(0));
		assertEquals(6000, WorldRules.agedBaby(6000));
		// It takes several casts to raise one from birth.
		assertTrue(24000 / (WorldRules.AGE_BABY_SECONDS * 20) >= 5);
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
