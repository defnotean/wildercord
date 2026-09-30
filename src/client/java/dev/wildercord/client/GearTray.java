package dev.wildercord.client;

import dev.wildercord.gear.GearLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The gear slots and the Backpack slot on the inventory screens. In the survival inventory they sit in a tray
 * on the panel's top edge, drawn to match the vanilla panel (its black outline, white lit edge and grey shaded
 * edge) and open at the bottom into it; every slot on it, and the creative tab's, gets the Cord slot's riveted
 * well.
 */
public final class GearTray {
	private GearTray() {}

	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int FILL = 0xFFC6C6C6;
	private static final int SHADE = 0xFF555555;

	/** Draws the tray and the wells of every slot on it; {@code left} and {@code top} are the window's corner. */
	public static void drawSurvival(GuiGraphicsExtractor g, int left, int top) {
		int slots = GearLayout.traySlots();
		int x0 = left + GearLayout.TRAY_X;
		int y0 = top + GearLayout.TRAY_Y;
		int x1 = x0 + GearLayout.trayWidth(slots);
		int y1 = y0 + GearLayout.TRAY_HEIGHT;
		// The body, over the panel's own top border where the tray joins it, then the lit and shaded edges.
		g.fill(x0, y0, x1, y1, FILL);
		g.fill(x0 + 1, y0, x1 - 1, y0 + 1, BLACK);
		g.fill(x0, y0 + 1, x0 + 1, y1, BLACK);
		g.fill(x1 - 1, y0 + 1, x1, y1, BLACK);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 3, WHITE);
		g.fill(x0 + 1, y0 + 3, x0 + 3, y1, WHITE);
		g.fill(x1 - 4, y0 + 3, x1 - 1, y1, SHADE);
		g.fill(x1 - 3, y0 + 3, x1 - 1, y0 + 4, SHADE);
		for (int i = 0; i < slots; i++) {
			SlotWell.draw(g, left + GearLayout.inventoryX(i) - 1, top + GearLayout.INVENTORY_Y - 1);
		}
	}

	/** Draws the wells of the creative Survival Inventory tab's gear slots and Backpack slot. */
	public static void drawCreative(GuiGraphicsExtractor g, int left, int top) {
		for (int i = 0; i < GearLayout.traySlots(); i++) {
			SlotWell.draw(g, left + GearLayout.creativeX(i) - 1, top + GearLayout.creativeY(i) - 1);
		}
	}
}
