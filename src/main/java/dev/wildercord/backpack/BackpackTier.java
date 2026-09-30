package dev.wildercord.backpack;

/**
 * The three backpacks, smallest first: each holds a few more rows than the one below it, and is crafted
 * from it (keeping what's inside). Pure data: the item, the menu, the tooltip and the look all read it.
 */
public enum BackpackTier {
	/** Leather round a chest: two rows. */
	BACKPACK("backpack", 2, 0xA06540),
	/** Iron-cornered, with a bedroll: three rows, a chest's worth. */
	REINFORCED("reinforced_backpack", 3, 0x8C5A36),
	/** Woven with runes: four rows, as much as your whole inventory. */
	RUNEWOVEN("runewoven_backpack", 4, 0x6048A8);

	/** The item's id (under {@code wildercord:}), and its texture's name. */
	public final String path;
	/** Rows of nine slots it holds. */
	public final int rows;
	/** Its colour before it's dyed (RGB), on the item and on the wearer's back. */
	public final int color;

	BackpackTier(String path, int rows, int color) {
		this.path = path;
		this.rows = rows;
		this.color = color;
	}

	/** How many stacks it holds. */
	public int slots() {
		return rows * 9;
	}
}
