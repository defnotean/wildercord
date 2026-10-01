package dev.wildercord.gear;

import dev.wildercord.menu.CordSlot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The gear slots: which piece fits where, how a slotted piece takes the place of held ones, and where the slots sit on screen. */
class GearSlotsTest {
	private static final GearDef FIRE_STAFF = GearDef.staff("fire");
	private static final GearDef FROST_STAFF = GearDef.staff("frost");
	private static final GearDef VOID_STAFF = GearDef.staff("void");
	private static final GearDef GREATER_FIRE = GearDef.greaterStaff("fire");

	private static Map<GearSlot, GearDef> slots(GearDef staff, GearDef focus, GearDef tome) {
		java.util.LinkedHashMap<GearSlot, GearDef> map = new java.util.LinkedHashMap<>();
		if (staff != null) {
			map.put(GearSlot.STAFF, staff);
		}
		if (focus != null) {
			map.put(GearSlot.FOCUS, focus);
		}
		if (tome != null) {
			map.put(GearSlot.TOME, tome);
		}
		return map;
	}

	// ------------------------------------------------------------------ which piece fits where

	@Test
	void everyPieceOfGearFitsExactlyOneSlot() {
		for (GearDef def : GearDef.all()) {
			List<GearSlot> fits = GearSlot.all().stream().filter(slot -> slot.accepts(def)).toList();
			assertEquals(1, fits.size(), def.path() + " should fit one slot");
			assertSame(fits.getFirst(), GearSlot.of(def));
			assertSame(fits.getFirst(), GearSlot.of(def.kind()));
		}
	}

	@Test
	void theSlotsTakeWhatTheirNamesSay() {
		assertEquals(List.of(GearSlot.STAFF, GearSlot.FOCUS, GearSlot.TOME), GearSlot.all());
		assertTrue(GearSlot.STAFF.accepts(FIRE_STAFF) && GearSlot.STAFF.accepts(GREATER_FIRE));
		assertFalse(GearSlot.STAFF.accepts(GearDef.HASTE) || GearSlot.STAFF.accepts(GearDef.TOME));
		for (GearDef focus : List.of(GearDef.HASTE, GearDef.THRIFT, GearDef.DEEP_WELL, GearDef.ECHOES)) {
			assertTrue(GearSlot.FOCUS.accepts(focus));
			assertFalse(GearSlot.STAFF.accepts(focus) || GearSlot.TOME.accepts(focus));
		}
		assertTrue(GearSlot.TOME.accepts(GearDef.TOME));
		assertFalse(GearSlot.FOCUS.accepts(GearDef.TOME) || GearSlot.STAFF.accepts(GearDef.TOME));
		// The Breath Sash is worn where the tome goes: a fifth spell, or a steadier breath.
		assertTrue(GearSlot.TOME.accepts(GearDef.BREATH_SASH));
		assertFalse(GearSlot.FOCUS.accepts(GearDef.BREATH_SASH) || GearSlot.STAFF.accepts(GearDef.BREATH_SASH));
		assertFalse(GearBonuses.of(slots(null, null, GearDef.BREATH_SASH), null, null).fifthSpell(), "the sash opens no spell");
		assertFalse(GearSlot.STAFF.accepts(null));
		assertNull(GearSlot.of((GearDef) null));
	}

	@Test
	void aSlotIsNamedAfterItsIdForItsIconAndText() {
		for (GearSlot slot : GearSlot.all()) {
			assertSame(slot, GearSlot.get(slot.id()));
			assertEquals("container/slot/gear_" + slot.id(), slot.iconPath());
			assertEquals("gear_slot.wildercord." + slot.id(), slot.nameKey());
			assertEquals("gear_slot.wildercord." + slot.id() + ".hint", slot.hintKey());
			assertEquals(GearSlot.all().indexOf(slot), slot.index());
		}
		assertEquals(GearSlot.Look.BACK, GearSlot.STAFF.look());
		assertEquals(GearSlot.Look.SHOULDER, GearSlot.FOCUS.look());
		assertEquals(GearSlot.Look.HIP, GearSlot.TOME.look());
	}

	@Test
	void aKindOfGearCantHaveTwoSlotsOrASlotTwoNames() {
		assertThrows(IllegalStateException.class, () -> GearSlot.register("second_focus", GearSlot.Look.HIP, GearDef.GearKind.FOCUS));
		assertThrows(IllegalStateException.class, () -> GearSlot.register("staff", GearSlot.Look.BACK, GearDef.GearKind.GREATER_STAFF));
		assertEquals(3, GearSlot.all().size());
	}

	// ------------------------------------------------------------------ a slotted piece takes the place of held ones

	@Test
	void aPieceInASlotWorksWithNothingHeld() {
		GearBonuses gear = GearBonuses.of(slots(FIRE_STAFF, null, null), null, null);
		assertEquals(1.20, gear.power("fire"), 1e-9);
		assertEquals(0.90, gear.cost(Set.of("fire")), 1e-9);
		assertEquals(List.of(FIRE_STAFF), gear.pieces());
		assertTrue(GearBonuses.of(slots(null, null, GearDef.TOME), null, null).fifthSpell());
		assertEquals(50, GearBonuses.of(slots(null, GearDef.DEEP_WELL, null), null, null).mana());
	}

	@Test
	void aSlottedStaffTakesThePlaceOfHeldStaffs() {
		GearBonuses gear = GearBonuses.of(slots(FIRE_STAFF, null, null), FROST_STAFF, VOID_STAFF);
		assertEquals(List.of(FIRE_STAFF), gear.pieces());
		assertEquals(1.20, gear.power("fire"), 1e-9);
		assertEquals(1.0, gear.power("frost"), 1e-9);
		assertEquals(1.0, gear.power("void"), 1e-9);
		assertEquals(1.0, gear.cost(Set.of("frost")), 1e-9);
		// A greater staff in the slot, a plain one of the same element in a hand: only the slotted one counts.
		assertEquals(1.35, GearBonuses.of(slots(GREATER_FIRE, null, null), FIRE_STAFF, null).power("fire"), 1e-9);
		assertEquals(1.20, GearBonuses.of(slots(FIRE_STAFF, null, null), GREATER_FIRE, null).power("fire"), 1e-9);
	}

	@Test
	void anEmptyStaffSlotLetsHeldStaffsWorkAsBefore() {
		GearBonuses gear = GearBonuses.of(slots(null, null, null), FROST_STAFF, VOID_STAFF);
		assertEquals(List.of(FROST_STAFF, VOID_STAFF), gear.pieces());
		assertEquals(1.20, gear.power("frost"), 1e-9);
		assertEquals(1.20, gear.power("void"), 1e-9);
		// The same as the rule with no slots at all.
		assertEquals(GearBonuses.of(FROST_STAFF, VOID_STAFF), gear);
		assertEquals(GearBonuses.of(null, GearDef.TOME), GearBonuses.of(slots(null, null, null), null, GearDef.TOME));
	}

	@Test
	void aSlottedFocusReplacesAHeldFocusAndAnEmptySlotDoesnt() {
		GearBonuses slotted = GearBonuses.of(slots(null, GearDef.THRIFT, null), null, GearDef.HASTE);
		assertEquals(List.of(GearDef.THRIFT), slotted.pieces());
		assertEquals(1.0, slotted.chargeSpeed(), 1e-9);
		assertEquals(0.85, slotted.cost(Set.of()), 1e-9);
		GearBonuses held = GearBonuses.of(slots(null, null, null), null, GearDef.HASTE);
		assertEquals(1.40, held.chargeSpeed(), 1e-9);
		// A focus still only works held in the off-hand.
		assertTrue(GearBonuses.of(slots(null, null, null), GearDef.HASTE, null).isEmpty());
	}

	@Test
	void aSlottedTomeReplacesAHeldTomeAndAHeldOneStillWorksWithTheSlotEmpty() {
		assertEquals(List.of(GearDef.TOME), GearBonuses.of(slots(null, null, GearDef.TOME), null, GearDef.TOME).pieces());
		assertTrue(GearBonuses.of(slots(null, null, null), null, GearDef.TOME).fifthSpell());
		// Held in the main hand it never worked, slot or not.
		assertFalse(GearBonuses.of(slots(null, null, null), GearDef.TOME, null).fifthSpell());
		assertTrue(GearBonuses.of(slots(null, null, GearDef.TOME), GearDef.TOME, null).fifthSpell());
	}

	@Test
	void aSlotOnlyTakesThePlaceOfItsOwnKind() {
		// A staff in its slot, a focus held: both work; a focus in its slot, a staff held: both work.
		GearBonuses staffAndHeldFocus = GearBonuses.of(slots(FIRE_STAFF, null, null), null, GearDef.HASTE);
		assertEquals(List.of(FIRE_STAFF, GearDef.HASTE), staffAndHeldFocus.pieces());
		GearBonuses focusAndHeldStaff = GearBonuses.of(slots(null, GearDef.HASTE, null), FROST_STAFF, null);
		assertEquals(List.of(GearDef.HASTE, FROST_STAFF), focusAndHeldStaff.pieces());
		assertEquals(1.40, focusAndHeldStaff.chargeSpeed(), 1e-9);
		assertEquals(1.20, focusAndHeldStaff.power("frost"), 1e-9);
	}

	@Test
	void theTomeAndAFocusNowApplyTogether() {
		// Both in slots, the tome in its slot with a held focus, and a slotted focus with a held tome.
		for (GearBonuses gear : List.of(
				GearBonuses.of(slots(null, GearDef.HASTE, GearDef.TOME), null, null),
				GearBonuses.of(slots(null, null, GearDef.TOME), null, GearDef.HASTE),
				GearBonuses.of(slots(null, GearDef.HASTE, null), null, GearDef.TOME))) {
			assertTrue(gear.fifthSpell());
			assertEquals(1.40, gear.chargeSpeed(), 1e-9);
			assertEquals(2, gear.pieces().size());
		}
	}

	@Test
	void theSamePieceCountsOnce() {
		GearBonuses gear = GearBonuses.of(slots(FIRE_STAFF, null, null), FIRE_STAFF, FIRE_STAFF);
		assertEquals(List.of(FIRE_STAFF), gear.pieces());
		assertEquals(1.20, gear.power("fire"), 1e-9);
		assertEquals(0.90, gear.cost(Set.of("fire")), 1e-9);
		GearBonuses echoes = GearBonuses.of(slots(null, GearDef.ECHOES, null), null, GearDef.ECHOES);
		assertEquals(1, echoes.pieces().size());
		assertEquals(0.10, echoes.echo(), 1e-9);
	}

	@Test
	void everythingInItsSlotAppliesTogetherWithNothingHeld() {
		GearBonuses gear = GearBonuses.of(slots(FIRE_STAFF, GearDef.THRIFT, GearDef.TOME), null, null);
		assertEquals(3, gear.pieces().size());
		assertTrue(gear.fifthSpell());
		// Cost: Thrift's 15% then the staff's 10%, on a fire spell; power: the staff's 20% then Thrift's 10% off.
		assertEquals(0.85 * 0.90, gear.cost(Set.of("fire")), 1e-9);
		assertEquals(0.85, gear.cost(Set.of("frost")), 1e-9);
		assertEquals(1.20 * 0.90, gear.power("fire"), 1e-9);
		assertEquals(0.90, gear.power("frost"), 1e-9);
		assertTrue(gear.changes(Set.of("fire")));
		assertEquals(List.of(FIRE_STAFF), gear.staffsFor(Set.of("fire")));
		assertTrue(gear.staffsFor(Set.of("frost")).isEmpty());
	}

	@Test
	void theNumbersReachTheSpellThroughTheSlots() {
		assertEquals(1.40, GearBonuses.of(slots(null, GearDef.HASTE, null), null, null).chargeSpeed(), 1e-9);
		assertEquals(0.10, GearBonuses.of(slots(null, GearDef.ECHOES, null), null, null).echo(), 1e-9);
		assertEquals(50, GearBonuses.of(slots(null, GearDef.DEEP_WELL, null), null, null).mana());
		assertEquals(0, GearBonuses.of(slots(FIRE_STAFF, GearDef.HASTE, null), null, null).mana());
		// The fifth spell opens through the slot: the tome counts, so slot 5 is open even on a Twine Cord.
		boolean tome = GearBonuses.of(slots(null, null, GearDef.TOME), null, null).fifthSpell();
		assertTrue(SpellSlots.open(1, tome, SpellSlots.TOME));
		assertEquals(List.of(0, SpellSlots.TOME), SpellSlots.open(1, tome));
		assertEquals(SpellSlots.TOME, SpellSlots.resolve(1, tome, 1));
		assertFalse(SpellSlots.open(1, GearBonuses.of(slots(null, null, null), null, null).fifthSpell(), SpellSlots.TOME));
	}

	@Test
	void aPieceInTheWrongSlotIsIgnored() {
		// The attachment is only ever filled through the slots, but a bad save mustn't give a focus a staff's discount.
		GearBonuses gear = GearBonuses.of(Map.of(GearSlot.STAFF, GearDef.HASTE), null, GearDef.THRIFT);
		assertEquals(List.of(GearDef.THRIFT), gear.pieces());
		assertEquals(1.0, gear.chargeSpeed(), 1e-9);
		assertTrue(GearBonuses.of(Map.of(GearSlot.TOME, FIRE_STAFF), null, null).isEmpty());
	}

	// ------------------------------------------------------------------ where the slots sit

	private record Box(String name, int x, int y) {
		boolean overlaps(Box other) {
			return x < other.x + 16 && other.x < x + 16 && y < other.y + 16 && other.y < y + 16;
		}
	}

	@Test
	void theSurvivalTrayIsAboveThePanelAndClearOfEverything() {
		// The gear slots, then the Backpack slot after them.
		int count = GearLayout.traySlots();
		assertEquals(GearSlot.all().size() + 1, count);
		assertEquals(GearSlot.all().size(), GearLayout.backpackIndex());
		assertTrue(GearLayout.INVENTORY_X + count * GearLayout.SLOT <= 176, "the tray fits over the panel's width");
		for (GearSlot slot : GearSlot.all()) {
			// Slots are 16 wide, in 18-pixel wells, all above the window's top edge, so nothing in the panel is under them.
			assertTrue(GearLayout.inventoryY(slot) + 16 <= 0, slot + " should be above the panel");
			assertEquals(GearLayout.INVENTORY_X + slot.index() * 18, GearLayout.inventoryX(slot));
			assertTrue(GearLayout.onTray(GearLayout.inventoryX(slot) + 8, GearLayout.inventoryY(slot) + 8, count), slot + " is on the tray");
		}
		int backpackX = GearLayout.inventoryX(GearLayout.backpackIndex());
		assertEquals(GearLayout.inventoryX(GearSlot.TOME) + 18, backpackX, "the Backpack slot follows the last gear slot");
		assertTrue(GearLayout.onTray(backpackX + 8, GearLayout.INVENTORY_Y + 8, count), "the Backpack slot is on the tray");
		assertFalse(GearLayout.onTray(backpackX + 8, GearLayout.INVENTORY_Y + 8, count - 1), "a tray without it wouldn't reach it");
		// The tray takes a click on its own padding and its slots, and nothing off to the side of it.
		assertTrue(GearLayout.onTray(GearLayout.TRAY_X, GearLayout.TRAY_Y, count));
		assertFalse(GearLayout.onTray(GearLayout.TRAY_X - 1, GearLayout.TRAY_Y, count));
		assertFalse(GearLayout.onTray(GearLayout.TRAY_X + GearLayout.trayWidth(count), GearLayout.TRAY_Y, count));
		assertFalse(GearLayout.onTray(10, GearLayout.TRAY_Y + GearLayout.TRAY_HEIGHT, count));
		assertFalse(GearLayout.onTray(10, GearLayout.TRAY_Y - 1, count));
		assertFalse(GearLayout.onTray(10, 0, 0));
	}

	@Test
	void theCreativeBlockFitsTheFreeCornerOfTheSurvivalTab() {
		// Vanilla's slots on the Survival Inventory tab (195x136), and the Cord slot's mirror of the offhand.
		List<Box> taken = new ArrayList<>(List.of(
			new Box("head", 54, 6), new Box("chest", 54, 33), new Box("legs", 108, 6), new Box("feet", 108, 33),
			new Box("offhand", 35, 20), new Box("cord", CordSlot.CREATIVE_X, CordSlot.CREATIVE_Y),
			new Box("destroy", 173, 112)));
		for (int i = 0; i < 27; i++) {
			taken.add(new Box("inventory", 9 + i % 9 * 18, 54 + i / 9 * 18));
		}
		for (int i = 0; i < 9; i++) {
			taken.add(new Box("hotbar", 9 + i * 18, 112));
		}
		assertTrue(GearLayout.traySlots() <= GearLayout.CREATIVE_CAPACITY, "the gear slots and the Backpack slot fit the block");
		List<Box> gear = new ArrayList<>();
		List<Box> block = new ArrayList<>();
		for (GearSlot slot : GearSlot.all()) {
			block.add(new Box(slot.id(), GearLayout.creativeX(slot), GearLayout.creativeY(slot)));
		}
		block.add(new Box("backpack", GearLayout.creativeX(GearLayout.backpackIndex()), GearLayout.creativeY(GearLayout.backpackIndex())));
		for (Box box : block) {
			String slot = box.name();
			assertTrue(box.x() >= 0 && box.x() + 18 <= 195 && box.y() >= 0 && box.y() + 18 <= 136, slot + " is on the tab");
			for (Box other : taken) {
				assertFalse(box.overlaps(other), slot + " overlaps " + other.name());
			}
			for (Box other : gear) {
				assertFalse(box.overlaps(other), slot + " overlaps " + other.name());
			}
			gear.add(box);
		}
		// The free corner has room for the capacity claimed.
		for (int i = 0; i < GearLayout.CREATIVE_CAPACITY; i++) {
			Box box = new Box("spare", GearLayout.CREATIVE_X + i % GearLayout.CREATIVE_COLUMNS * 18, GearLayout.CREATIVE_Y + i / GearLayout.CREATIVE_COLUMNS * 18);
			assertTrue(box.x() + 18 <= 195 && box.y() + 18 <= 54, "spare " + i + " fits above the inventory rows");
			for (Box other : taken) {
				assertFalse(box.overlaps(other), "spare " + i + " overlaps " + other.name());
			}
		}
	}
}
