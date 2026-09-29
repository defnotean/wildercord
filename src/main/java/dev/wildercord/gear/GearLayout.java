package dev.wildercord.gear;

/**
 * Where the gear slots sit on the inventory screens, from each slot's place in {@link GearSlot#all()}.
 * Pure arithmetic (in pixels, relative to the screen's window), so the menu, the drawing, the click
 * handling and the tests all read the same numbers.
 *
 * <p>Survival: the vanilla panel has no free room for three more slots beside the Cord slot (the recipe
 * book button and the crafting grid take it), so they sit in a tray on the panel's top edge, above the
 * armour column and the paper doll, left to right. It is part of the window for clicking, and moves with
 * it when the recipe book opens. Creative: a block of two columns to the right of the Cord slot, in the
 * free corner of the Survival Inventory tab (room for {@link #CREATIVE_CAPACITY}).</p>
 */
public final class GearLayout {
	private GearLayout() {}

	/** A slot's pitch, as vanilla's. */
	public static final int SLOT = 18;

	/** Survival: the first slot's x, level with the armour column. */
	public static final int INVENTORY_X = 8;
	/** Survival: every slot's y (the tray is above the window's top edge). */
	public static final int INVENTORY_Y = -18;
	/** The tray's plate: 6px of border and padding each side, from y -24 down over the panel's top border. */
	public static final int TRAY_X = INVENTORY_X - 6;
	public static final int TRAY_Y = -24;
	public static final int TRAY_HEIGHT = 27;
	private static final int TRAY_PADDING = 12;

	/** Creative Survival Inventory tab: the block's first slot, right of the Cord slot (127, 20). */
	public static final int CREATIVE_X = 146;
	public static final int CREATIVE_Y = 7;
	/** The block is two slots wide and two deep: the free corner holds four. */
	public static final int CREATIVE_COLUMNS = 2;
	public static final int CREATIVE_CAPACITY = 4;

	public static int inventoryX(GearSlot slot) {
		return INVENTORY_X + slot.index() * SLOT;
	}

	public static int inventoryY(GearSlot slot) {
		return INVENTORY_Y;
	}

	/** The tray's width for this many slots. */
	public static int trayWidth(int slots) {
		return slots * SLOT + TRAY_PADDING;
	}

	/** Whether a point (relative to the window's corner) is on the survival tray, for click handling. */
	public static boolean onTray(double x, double y, int slots) {
		return slots > 0 && x >= TRAY_X && x < TRAY_X + trayWidth(slots) && y >= TRAY_Y && y < TRAY_Y + TRAY_HEIGHT;
	}

	public static int creativeX(GearSlot slot) {
		return CREATIVE_X + slot.index() % CREATIVE_COLUMNS * SLOT;
	}

	public static int creativeY(GearSlot slot) {
		return CREATIVE_Y + slot.index() / CREATIVE_COLUMNS * SLOT;
	}
}
