package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.travel.Spot;
import dev.wildercord.travel.TravelRules;
import dev.wildercord.travel.Waypoints;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * The tracked waypoint, as one small line in the top-left corner (clear of the hotbar, the spell
 * panel, boss bars and effects):
 * <pre>
 *  ➤ camp  128 blocks          an arrow turned toward it as you look around
 *  ◆ camp  in the Nether       when it's in another world
 * </pre>
 * The server sends which waypoint is tracked ({@link Waypoints.Track}); everything else is worked out
 * here each frame. Hidden while the debug screen is open, since it uses the same corner; on a GUI too
 * narrow for it beside the boss bars, it moves down below them ({@link #lineY}).
 */
public final class WaypointHud {
	private WaypointHud() {}

	private static final int LAVENDER = 0xFFB8A8FF;
	private static final int NAME = 0xFFF0E8FF;
	private static final int DIM = 0xFFA89CC8;
	private static final int BACKDROP = 0x70100818;

	private static Waypoints.Track tracked;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Waypoints.Track.TYPE, (payload, context) -> tracked = payload.none() ? null : payload);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> tracked = null);
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Wildercord.id("waypoint"), WaypointHud::extract);
	}

	/** The waypoint this client was told is tracked, or null. */
	public static Waypoints.Track tracked() {
		return tracked;
	}

	private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Waypoints.Track track = tracked;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (track == null || player == null || mc.gui.hud.getDebugOverlay().showDebugScreen()) {
			return;
		}
		Font font = mc.font;
		float partial = delta.getGameTimeDeltaPartialTick(true);
		boolean here = track.dimension().equals(player.level().dimension().identifier().toString());
		Component info;
		double turn = 0;
		boolean arrived = false;
		if (here) {
			Vec3 at = player.getPosition(partial);
			double dx = track.x() - at.x;
			double dy = track.y() - at.y;
			double dz = track.z() - at.z;
			long blocks = Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
			arrived = blocks < 2;
			turn = TravelRules.bearing(dx, dz, player.getViewYRot(partial));
			info = arrived ? Component.translatable("hud.wildercord.waypoint.here") : Component.translatable("hud.wildercord.waypoint.blocks", blocks);
		} else {
			info = Component.translatable("hud.wildercord.waypoint.elsewhere", Spot.dimensionName(track.dimension()));
		}
		String name = track.name();
		int x = 6;
		int textX = x + 12;
		int width = 12 + font.width(name) + 6 + font.width(info);
		int bosses = ((dev.wildercord.client.mixin.BossHealthOverlayAccessor) mc.gui.hud.getBossOverlay()).wildercord$events().size();
		int y = lineY(g.guiWidth(), g.guiHeight(), x + width + 3, bosses);
		g.fill(x - 3, y - 3, x + width + 3, y + font.lineHeight + 1, BACKDROP);
		if (here && !arrived) {
			arrow(g, x + 4, y + 4, (float) Math.toRadians(turn));
		} else {
			diamond(g, x + 4, y + 4);
		}
		g.text(font, name, textX, y, NAME, true);
		g.text(font, info, textX + font.width(name) + 6, y, DIM, true);
	}

	/**
	 * Where the line goes on a GUI {@code guiWidth} by {@code guiHeight}, reaching {@code right} across, with
	 * {@code bosses} boss bars showing: the top-left corner, unless it would run under the bars (182 wide,
	 * centred); then just below the last bar drawn (one every 19 pixels from 12 down, until a third of the way).
	 */
	static int lineY(int guiWidth, int guiHeight, int right, int bosses) {
		if (bosses <= 0 || right < guiWidth / 2 - 91) {
			return 6;
		}
		int shown = 0;
		for (int bar = 12; shown < bosses; ) {
			shown++;
			bar += 19;
			if (bar >= guiHeight / 3) {
				break;
			}
		}
		// The last bar's bottom is 12 + 19 * (shown - 1) + 5; the line's backdrop starts 3 above its text.
		return 12 + 19 * (shown - 1) + 5 + 5;
	}

	/** A small arrow centred on {@code cx}, {@code cy}, turned {@code angle} radians clockwise from pointing up. */
	private static void arrow(GuiGraphicsExtractor g, int cx, int cy, float angle) {
		g.pose().pushMatrix();
		// Turned about the arrow's own middle (x 0.5 in the pixels below).
		g.pose().translate(cx + 0.5F, cy);
		g.pose().rotate(angle);
		g.pose().translate(-0.5F, 0F);
		// The head, widening row by row, then the shaft.
		for (int row = 0; row < 4; row++) {
			g.fill(-row, -4 + row, row + 1, -3 + row, LAVENDER);
		}
		g.fill(-1, 0, 2, 4, LAVENDER);
		g.pose().popMatrix();
	}

	/** A small diamond: the waypoint is here, or in another world. */
	private static void diamond(GuiGraphicsExtractor g, int cx, int cy) {
		for (int row = 0; row < 3; row++) {
			g.fill(cx - row, cy - 2 + row, cx + row + 1, cy - 1 + row, LAVENDER);
			g.fill(cx - row, cy + 2 - row, cx + row + 1, cy + 3 - row, LAVENDER);
		}
	}
}
