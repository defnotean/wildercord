package dev.wildercord.spell;

import dev.wildercord.spell.AltarReagents.Context;
import dev.wildercord.spell.AltarReagents.Effect;
import dev.wildercord.spell.AltarReagents.Result;
import dev.wildercord.spell.Fusions.Catalyst;
import dev.wildercord.spell.Fusions.Slot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What each reagent does at the Fusion Altar: one clear change, and never spent for nothing. */
class AltarReagentsTest {
	/** Two runes in the first two sockets, the reagent in the third (which reads as empty). */
	private static List<Slot> slots(Slot a, Slot b) {
		return List.of(a, b, Slot.EMPTY);
	}

	private static Result with(Effect effect, Slot a, Slot b, Catalyst catalyst, Context context) {
		List<Slot> slots = slots(a, b);
		return AltarReagents.apply(effect, Fusions.plan(slots, catalyst), slots, context);
	}

	private static Result with(Effect effect, Slot a, Slot b, Catalyst catalyst) {
		return with(effect, a, b, catalyst, Context.NONE);
	}

	private static final Slot FIRE_II = Slot.of(Runes.FIRE, 2);
	private static final Slot FROST_I = Slot.of(Runes.FROST, 1);

	@Test
	void everyElementHasOneReagentEffect() {
		Set<Effect> seen = EnumSet.noneOf(Effect.class);
		for (String element : Affinity.ELEMENTS) {
			seen.add(AltarReagents.of(element).orElseThrow());
		}
		assertEquals(EnumSet.allOf(Effect.class), seen);
		assertTrue(AltarReagents.of("").isEmpty());
	}

	@Test
	void cinderAshKeepsTheHigherRank() {
		Fusions.Plan plain = Fusions.plan(slots(FIRE_II, FROST_I), Catalyst.SHARD);
		assertEquals(1, plain.rank(), "a fusion keeps the lower rank");
		Result tempered = with(Effect.TEMPERED, FIRE_II, FROST_I, Catalyst.SHARD);
		assertTrue(tempered.ready());
		assertEquals(Runes.STEAM, tempered.plan().result());
		assertEquals(2, tempered.plan().rank());
		assertEquals(plain.xp(), tempered.plan().xp(), "at the same price");
		assertFalse(with(Effect.TEMPERED, FROST_I, Slot.of(Runes.FIRE, 1), Catalyst.SHARD).ready(), "two runes of one rank: it would change nothing");
	}

	@Test
	void everfrostHalvesTheXp() {
		assertEquals(2, with(Effect.STILLED, FIRE_II, FROST_I, Catalyst.SHARD).plan().xp());
		// A weave of eight effects: 21 levels, stilled to 11.
		RuneDef seven = Runes.FIRE;
		for (RuneDef next : List.of(Runes.FROST, Runes.SHOCK, Runes.HEAL, Runes.HARM, Runes.BLEED, Runes.GROW)) {
			seven = WovenRunes.bind(seven, next);
		}
		Result big = with(Effect.STILLED, Slot.of(seven, 1), Slot.of(Runes.FIRE, 1), Catalyst.BLOCK);
		assertTrue(big.ready(), big.plan().problem());
		assertEquals(11, big.plan().xp());
	}

	@Test
	void fulguriteChargesOnlyARankOneResult() {
		Result charged = with(Effect.CHARGED, Slot.of(Runes.FIRE, 1), FROST_I, Catalyst.SHARD);
		assertEquals(2, charged.plan().rank());
		assertFalse(with(Effect.CHARGED, FIRE_II, Slot.of(Runes.FROST, 2), Catalyst.SHARD).ready(), "already rank II");
	}

	@Test
	void aBottledGaleUnbindsASignaturePair() {
		Result unbound = with(Effect.UNBOUND, Slot.of(Runes.CHILL, 1), Slot.of(Runes.SHOCK, 1), Catalyst.SHARD);
		assertTrue(unbound.ready());
		assertEquals(Runes.HAIL, unbound.plan().result(), "Chill and Shock make Hail, not Frostwire");
		assertFalse(unbound.plan().signature());
		assertEquals("fusion:hail", unbound.plan().recipe().key(), "and the Grimoire records Hail");
		assertFalse(with(Effect.UNBOUND, FIRE_II, FROST_I, Catalyst.SHARD).ready(), "no signature to unbind");
		assertFalse(with(Effect.UNBOUND, Slot.of(Runes.CHILL, 1), Slot.of(Runes.SHOCK, 1), Catalyst.BLOCK).ready(), "a weave has none either");
	}

	@Test
	void geodeGritKeepsTheAmethystAndAPetalMakesTwo() {
		Result grounded = with(Effect.GROUNDED, FIRE_II, FROST_I, Catalyst.BLOCK);
		assertTrue(grounded.keepCatalyst() && grounded.copies() == 1);
		Result bountiful = with(Effect.BOUNTIFUL, FIRE_II, FROST_I, Catalyst.SHARD);
		assertEquals(2, bountiful.copies());
		assertFalse(bountiful.keepCatalyst());
	}

	@Test
	void hollowDustKeepsTheLowerTierRune() {
		// Fire is Tier II, Heal Tier I: Heal's socket keeps its rune.
		Result hollowed = with(Effect.HOLLOWED, Slot.of(Runes.FIRE, 1), Slot.of(Runes.HEAL, 1), Catalyst.BLOCK);
		assertEquals(1, hollowed.keepSlot());
		Result again = with(Effect.HOLLOWED, Slot.of(Runes.HEAL, 1), Slot.of(Runes.FIRE, 1), Catalyst.BLOCK);
		assertEquals(0, again.keepSlot());
		// Extending a weave keeps the lone rune, whatever its tier: the weave goes into the new one.
		RuneDef weave = WovenRunes.bind(Runes.HEAL, Runes.SHOCK);
		assertEquals(1, with(Effect.HOLLOWED, Slot.of(weave, 1), Slot.of(Runes.LIGHTNING, 1), Catalyst.BLOCK).keepSlot());
		// The reagent in the middle socket: the rune sockets are 0 and 2.
		List<Slot> slots = List.of(Slot.of(Runes.HEAL, 1), Slot.EMPTY, Slot.of(Runes.FIRE, 1));
		assertEquals(0, AltarReagents.apply(Effect.HOLLOWED, Fusions.plan(slots, Catalyst.BLOCK), slots, Context.NONE).keepSlot());
	}

	@Test
	void starDustExaltsForAPrice() {
		Result exalted = with(Effect.EXALTED, FIRE_II, Slot.of(Runes.FROST, 2), Catalyst.SHARD);
		assertEquals(3, exalted.plan().rank());
		assertEquals(Fusions.COMBINE_XP + AltarReagents.EXALT_XP, exalted.plan().xp());
		assertFalse(with(Effect.EXALTED, Slot.of(Runes.FIRE, 3), Slot.of(Runes.FROST, 3), Catalyst.SHARD).ready(), "already rank III");
	}

	@Test
	void hourglassSandFreesAFusionYouveMadeBefore() {
		Context made = new Context(List.of("fusion:steam"), 20);
		assertEquals(0, with(Effect.FAMILIAR, FIRE_II, FROST_I, Catalyst.SHARD, made).plan().xp());
		assertFalse(with(Effect.FAMILIAR, FIRE_II, FROST_I, Catalyst.SHARD).ready(), "not made before");
		assertFalse(with(Effect.FAMILIAR, FIRE_II, FROST_I, Catalyst.BLOCK, made).ready(), "a weave isn't a named fusion");
	}

	@Test
	void aSanguineBeadPaysInBloodButNeverTheLastHeart() {
		Result blood = with(Effect.BLOODBOUND, FIRE_II, FROST_I, Catalyst.SHARD, new Context(List.of(), 20));
		assertEquals(0, blood.plan().xp());
		assertEquals(3, blood.health());
		RuneDef weave = WovenRunes.bind(WovenRunes.bind(Runes.FIRE, Runes.FROST), Runes.SHOCK);
		Result big = with(Effect.BLOODBOUND, Slot.of(weave, 1), Slot.of(Runes.HEAL, 1), Catalyst.BLOCK, new Context(List.of(), 20));
		assertEquals(9 - AltarReagents.BLOOD_LEVELS, big.plan().xp(), "at most six levels in blood");
		assertEquals(AltarReagents.BLOOD_LEVELS, big.health());
		assertFalse(with(Effect.BLOODBOUND, FIRE_II, FROST_I, Catalyst.SHARD, new Context(List.of(), 4.5F)).ready(), "it would take the last heart");
	}

	@Test
	void aReagentOnlyWorksInAFusionOrWeave() {
		List<Slot> empty = new ArrayList<>(List.of(Slot.EMPTY, Slot.EMPTY, Slot.EMPTY));
		Result nothing = AltarReagents.apply(Effect.STILLED, Fusions.plan(empty, Catalyst.NONE), empty, Context.NONE);
		assertFalse(nothing.ready());
		assertNotNull(nothing.plan().problem());
		// Two effects but no amethyst: the fusion's own refusal stands.
		Result noCatalyst = with(Effect.STILLED, FIRE_II, FROST_I, Catalyst.NONE);
		assertFalse(noCatalyst.ready());
		assertTrue(noCatalyst.plan().problem().contains("amethyst"));
	}
}
