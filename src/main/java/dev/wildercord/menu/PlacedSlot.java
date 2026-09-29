package dev.wildercord.menu;

/**
 * A slot Wildercord adds to the player's inventory menu (the Cord slot, the gear slots). The creative
 * inventory re-lays out every slot itself, so it asks each of these where it belongs on that tab, and the
 * server accepts creative edits to them (see {@code mixin.ServerGamePacketListenerImplMixin}).
 */
public interface PlacedSlot {
	/** Where the slot sits on the creative inventory's Survival Inventory tab, relative to the window. */
	int creativeX();

	int creativeY();
}
