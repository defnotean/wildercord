package dev.wildercord.gametest.perf;

import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * While {@link #on}, every GUI element, text and item extracted this frame with its screen bounds and who drew it: the
 * innermost Wildercord HUD or screen class on the stack, else "hotbar" for vanilla's item hotbar (its slots, offhand slot
 * and attack indicator), else nothing (not kept).
 */
public final class HudCapture {
	private HudCapture() {}

	public record Rect(String owner, int x0, int y0, int x1, int y1) {
		public boolean overlaps(Rect o) { return x0 < o.x1 && o.x0 < x1 && y0 < o.y1 && o.y0 < y1; }
		@Override public String toString() { return owner + "[" + x0 + "," + y0 + " .. " + x1 + "," + y1 + "]"; }
	}

	public static final Set<String> OWNERS = Set.of("SpellHud", "AuraHud", "StanceHud", "StringHud", "WayHud",
		"SpellWheelScreen", "CordScreen", "AuraScreen", "RuneNotebookScreen");
	public static volatile boolean on;
	public static final List<Rect> RECTS = new ArrayList<>();
	private static final StackWalker WALKER = StackWalker.getInstance();

	public static void add(ScreenRectangle bounds) {
		if (!on || bounds == null) return;
		String owner = WALKER.walk(frames -> frames.map(f -> {
			String c = f.getClassName();
			if (c.startsWith("dev.wildercord.client.")) {
				String simple = c.substring(c.lastIndexOf('.') + 1);
				int inner = simple.indexOf('$');
				simple = inner < 0 ? simple : simple.substring(0, inner);
				if (OWNERS.contains(simple)) return simple;
			}
			return c.equals("net.minecraft.client.gui.Hud") && f.getMethodName().equals("extractItemHotbar") ? "hotbar" : null;
		}).filter(java.util.Objects::nonNull).findFirst().orElse(null));
		if (owner != null) synchronized (RECTS) { RECTS.add(new Rect(owner, bounds.left(), bounds.top(), bounds.right(), bounds.bottom())); }
	}
}
