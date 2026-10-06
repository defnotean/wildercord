package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.RelayState;
import dev.wildercord.content.LightOption;
import dev.wildercord.net.RelayInput;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.RelayRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Existing spell bindings provide fresh physical edges; server attachments only draw the committed focus. */
public final class RelayClient {
	private RelayClient() {}
	private static final boolean[] DOWN = new boolean[dev.wildercord.gear.SpellSlots.ALL + 1];
	private static final boolean[] BLOCKED = new boolean[DOWN.length];
	private static final boolean[] RELAY = new boolean[DOWN.length];
	private static net.minecraft.client.player.LocalPlayer body;
	private static net.minecraft.client.multiplayer.ClientLevel world;
	private static long nonce;
	private static boolean wasPlaying;
	private static KeyMapping lastBinding;

	public static void beginTick(Minecraft client) {
		boolean playing = client.player != null && client.player.isAlive() && client.gui.screen() == null && client.isWindowActive();
		if (body != client.player || world != client.level) {
			body = client.player; world = client.level;
			java.util.Arrays.fill(DOWN, false); java.util.Arrays.fill(RELAY, false); java.util.Arrays.fill(BLOCKED, true);
			wasPlaying = false;
		}
		if (!playing) {
			if (wasPlaying) cancel();
			java.util.Arrays.fill(BLOCKED, true);
		}
		wasPlaying = playing;
	}

	/** Called for selected and direct-slot bindings even when no click event was queued. */
	public static boolean key(Minecraft client, KeyMapping binding, int requested) {
		int index = requested + 1;
		boolean down = binding.isDown(), previous = DOWN[index];
		DOWN[index] = down;
		if (!down) BLOCKED[index] = false;
		int slot = client.player == null ? -1 : requested < 0 ? Spellbooks.get(client.player).selected() : requested;
		boolean row = client.player != null && slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL
			&& RelayRules.containsIds(Spellbooks.get(client.player).spells().get(slot));
		boolean handles = row || RELAY[index];
		if (!handles) return false;
		if (!wasPlaying) return true;
		if (down && !previous && !BLOCKED[index]) {
			RELAY[index] = true; lastBinding = binding;
			send(RelayInputRules.DOWN, requested);
		} else if (!down && previous && RELAY[index]) {
			send(RelayInputRules.UP, requested); RELAY[index] = false;
		}
		return true;
	}

	public static void cancel() {
		if (body != null && (body.hasAttached(RelayState.VIEW) || anyHeld())) send(RelayInputRules.CANCEL, -1);
		java.util.Arrays.fill(RELAY, false);
	}
	private static boolean anyHeld() { for (boolean held : RELAY) if (held) return true; return false; }
	private static void send(int action, int slot) {
		if (ClientPlayNetworking.canSend(RelayInput.TYPE)) ClientPlayNetworking.send(new RelayInput(action, slot, ++nonce));
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.net.RelayEditorReply.TYPE, (reply, context) -> {
			var screen = context.client().gui.screen();
			for (int depth = 0; screen != null && depth < 8; depth++) {
				if (screen instanceof CordScreen editor) { editor.reconcileRelay(reply); break; }
				if (!(screen instanceof CordEditorParent child)) break;
				screen = child.cordEditorParent();
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(RelayClient::drawWorld);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("relay_hud"), (g, delta) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.level == null) return;
			RelayState state = client.player.getAttached(RelayState.VIEW);
			if (state == null) return;
			double left = Math.max(0, (state.until() - client.level.getGameTime()) / 20.0);
			Component key = lastBinding == null ? WildercordKeys.castKey() : lastBinding.getTranslatedKeyMessage();
			String phase = state.phase() == RelayState.PLACED ? "Focus set" : state.phase() == RelayState.WARNING ? "Lane committed" : "Recovering";
			String line = phase + " · " + String.format(java.util.Locale.ROOT, "%.1f s", left);
			String hint = state.phase() == RelayState.PLACED ? "Press " + key.getString() + " to release" : state.phase() == RelayState.WARNING ? "Keep both sightlines clear" : "Casting will return shortly";
			int width = Math.min(Math.max(client.font.width(line), client.font.width(hint)) + 12, g.guiWidth() - 8);
			int x = (g.guiWidth() - width) / 2, y = g.guiHeight() - 103;
			g.fill(x, y, x + width, y + 37, 0xC8181721);
			g.text(client.font, line, x + 4, y + 4, 0xFFE9E1FF, true);
			g.text(client.font, client.font.plainSubstrByWidth(hint, width - 8), x + 4, y + 15, 0xFFCCC3E1, true);
			int total = state.phase() == RelayState.PLACED ? RelayRules.FOCUS_TICKS : state.phase() == RelayState.WARNING ? RelayRules.WARN_TICKS : RelayRules.RECOVERY_TICKS;
			int fill = (int) ((width - 8) * Math.clamp((state.until() - client.level.getGameTime()) / (double) total, 0, 1));
			g.fill(x + 4, y + 28, x + width - 4, y + 31, 0xFF393346);
			g.fill(x + 4, y + 28, x + 4 + fill, y + 31, 0xFF000000 | state.color());
		});
	}

	/** One clock ring per nearby active body every four ticks; a warning lane every two. Essential tells survive quiet mode. */
	private static void drawWorld(Minecraft client) {
		if (client.level == null || client.player == null) return;
		long now = client.level.getGameTime();
		int shown = 0;
		var nearby = new java.util.ArrayList<>(client.level.players());
		nearby.sort(java.util.Comparator.comparingDouble(player -> player == client.player ? -1 : player.distanceToSqr(client.player)));
		for (var player : nearby) {
			RelayState state = player.getAttached(RelayState.VIEW);
			if (state == null || state.phase() == RelayState.RECOVERING || now >= state.until()
				|| player.distanceToSqr(client.player) > 64 * 64 || shown++ >= 32) continue;
			var at = state.focus();
			if (now % 4 == 0) {
				float radius = state.phase() == RelayState.PLACED ? .15F + .6F * Math.clamp((state.until() - now) / (float) RelayRules.FOCUS_TICKS, 0, 1) : .55F;
				client.level.addParticle(new LightOption(LightOption.RING, state.color(), radius, radius, 0, .025F, 0, -90, 0, 5), at.x, at.y, at.z, 0, 0, 0);
			}
			if (state.phase() == RelayState.WARNING && now % 2 == 0) {
				var dir = state.end().subtract(at);
				client.level.addParticle(new LightOption(LightOption.RAY, state.color(), (float) dir.x, (float) dir.y, (float) dir.z, .035F, 0, 0, 0, 3), at.x, at.y, at.z, 0, 0, 0);
			}
		}
	}
}
