package dev.wildercord.gear;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellCompiler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

class GearBonusesTest {
	private static final GearDef FIRE_STAFF = GearDef.staff("fire");
	private static final GearDef FROST_STAFF = GearDef.staff("frost");
	private static final GearDef GREATER_FIRE = GearDef.greaterStaff("fire");

	@Test
	void everyElementHasAStaffAndAGreaterOne() {
		assertEquals(10, GearDef.ELEMENTS.size());
		for (String element : GearDef.ELEMENTS) {
			assertEquals(GearDef.GearKind.STAFF, GearDef.staff(element).kind());
			assertEquals(GearDef.GearKind.GREATER_STAFF, GearDef.greaterStaff(element).kind());
		}
		// 20 staffs, the tome and seven foci.
		assertEquals(28, GearDef.all().size());
		assertTrue(GearDef.get("focus_of_the_deep_well").isPresent());
	}

	@Test
	void aStaffStrengthensAndDiscountsItsOwnElement() {
		GearBonuses gear = GearBonuses.of(FIRE_STAFF, null);
		assertEquals(1.20, gear.power("fire"), 1e-9);
		assertEquals(1.0, gear.power("frost"), 1e-9);
		assertEquals(1.0, gear.power(""), 1e-9);
		assertEquals(0.90, gear.cost(Set.of("fire", "frost")), 1e-9);
		assertEquals(1.0, gear.cost(Set.of("frost")), 1e-9);
		assertEquals(1.35, GearBonuses.of(GREATER_FIRE, null).power("fire"), 1e-9);
		assertEquals(0.90, GearBonuses.of(GREATER_FIRE, null).cost(Set.of("fire")), 1e-9);
	}

	@Test
	void staffsWorkInEitherHandButDontStack() {
		assertEquals(1.20, GearBonuses.of(null, FIRE_STAFF).power("fire"), 1e-9);
		// The same staff twice counts once; a greater one beats a plain one.
		assertEquals(1.20, GearBonuses.of(FIRE_STAFF, FIRE_STAFF).power("fire"), 1e-9);
		assertEquals(1.35, GearBonuses.of(FIRE_STAFF, GREATER_FIRE).power("fire"), 1e-9);
		// Two elements: each keeps its power, but a spell of both is discounted once.
		GearBonuses both = GearBonuses.of(FIRE_STAFF, FROST_STAFF);
		assertEquals(1.20, both.power("fire"), 1e-9);
		assertEquals(1.20, both.power("frost"), 1e-9);
		assertEquals(0.90, both.cost(Set.of("fire", "frost")), 1e-9);
	}

	@Test
	void theTomeAndFociOnlyWorkFromTheOffHand() {
		assertTrue(GearBonuses.of(null, GearDef.TOME).fifthSpell());
		assertFalse(GearBonuses.of(GearDef.TOME, null).fifthSpell());
		assertEquals(0, GearBonuses.of(GearDef.DEEP_WELL, null).mana());
		assertEquals(50, GearBonuses.of(null, GearDef.DEEP_WELL).mana());
		assertTrue(GearBonuses.of(GearDef.HASTE, GearDef.THRIFT).pieces().equals(List.of(GearDef.THRIFT)));
	}

	@Test
	void fociNumbers() {
		GearBonuses thrift = GearBonuses.of(null, GearDef.THRIFT);
		assertEquals(0.85, thrift.cost(Set.of()), 1e-9);
		assertEquals(0.90, thrift.power("fire"), 1e-9);
		assertEquals(0.90, thrift.power(""), 1e-9);
		// Thrift and a staff multiply: each is its own factor.
		GearBonuses both = GearBonuses.of(FIRE_STAFF, GearDef.THRIFT);
		assertEquals(0.85 * 0.90, both.cost(Set.of("fire")), 1e-9);
		assertEquals(1.20 * 0.90, both.power("fire"), 1e-9);
		assertEquals(1.40, GearBonuses.of(null, GearDef.HASTE).chargeSpeed(), 1e-9);
		assertEquals(0.10, GearBonuses.of(null, GearDef.ECHOES).echo(), 1e-9);
		assertEquals(0.85, GearBonuses.of(null, GearDef.RESOLVE).power("fire"), 1e-9);
		assertEquals(0.0, GearBonuses.NONE.echo(), 1e-9);
		assertEquals(1.0, GearBonuses.NONE.cost(Set.of("fire")), 1e-9);
		assertFalse(GearBonuses.NONE.fifthSpell());
	}

	@Test
	void aSpellsElementsIncludeItsLinksAndEchoes() {
		SpellCompiler.Compiled c = SpellCompiler.compile(List.of(BOLT, FIRE, ON_HIT, BURST, FROST));
		assertEquals(Set.of("fire", "frost"), GearBonuses.elements(c.root()));
		SpellCompiler.Compiled echoed = SpellCompiler.compile(List.of(SELF, HEAL, ECHO));
		assertEquals(Set.of("life"), GearBonuses.elements(echoed.root()));
		assertEquals(Set.of("fire"), GearBonuses.elements(List.<RuneDef>of(BOLT, FIRE, SPLIT_MOD)));
		assertTrue(GearBonuses.elements(List.<RuneDef>of(SELF)).isEmpty());
	}

	@Test
	void aKnotsRunesCountForWhatACastCountsToward() {
		// Leaning and the contracts count a cast's elements from its runes: a Knot's count as if threaded one by one.
		RuneDef knot = get(dev.wildercord.spell.Knots.id(List.of(BOLT, FIRE), "")).orElseThrow();
		assertEquals(Set.of("fire", "frost"), GearBonuses.elements(List.of(knot, FROST)));
		RuneDef inner = get(dev.wildercord.spell.Knots.id(List.of(SELF, HEAL), "")).orElseThrow();
		RuneDef outer = get(dev.wildercord.spell.Knots.id(List.of(inner, SHOCK), "")).orElseThrow();
		assertEquals(Set.of("life", "storm"), GearBonuses.elements(List.of(outer)));
	}

	@Test
	void theReadoutMentionsGearOnlyWhenItMatters() {
		assertTrue(GearBonuses.of(FIRE_STAFF, null).changes(Set.of("fire")));
		assertFalse(GearBonuses.of(FIRE_STAFF, null).changes(Set.of("frost")));
		assertTrue(GearBonuses.of(null, GearDef.THRIFT).changes(Set.of()));
		assertFalse(GearBonuses.of(null, GearDef.TOME).changes(Set.of("fire")));
	}

	@Test
	void spellSlotsStepOnToTheTome() {
		assertTrue(SpellSlots.open(1, false, 0));
		assertFalse(SpellSlots.open(1, false, 1));
		assertFalse(SpellSlots.open(1, false, SpellSlots.TOME));
		assertTrue(SpellSlots.open(1, true, SpellSlots.TOME));
		assertFalse(SpellSlots.open(1, true, 3));
		assertEquals(List.of(0, 1, 4), SpellSlots.open(2, true));
		assertEquals(List.of(0, 1, 2, 3), SpellSlots.open(4, false));
		// "The selected one plus one": through the Cord's, on to the tome's, round to the first.
		assertEquals(4, SpellSlots.resolve(1, true, 1));
		assertEquals(0, SpellSlots.resolve(1, true, 5));
		assertEquals(0, SpellSlots.resolve(1, false, 1));
		assertEquals(1, SpellSlots.resolve(2, false, 1));
		assertEquals(0, SpellSlots.resolve(2, false, 2));
		assertEquals(4, SpellSlots.resolve(4, true, 4));
		assertEquals(4, SpellSlots.resolve(3, true, -1));
		assertEquals(2, SpellSlots.resolve(3, false, -1));
	}
}
