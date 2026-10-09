package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.MasterForms;
import dev.wildercord.aura.MasterFormLessons;
import dev.wildercord.aura.StoneHingeRules;
import dev.wildercord.aura.WallTurnRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;

import java.util.HashMap;
import java.util.Map;

/** Fresh key edges and bounded playback from actual movement events; menus never queue a future activation. */
public final class MasterFormsClient {
	private MasterFormsClient() {}
	private static KeyMapping key;
	private static boolean held, suppressed, playingBefore;
	private static long epoch, sequence;
	private static ClientLevel level;
	public record Playback(MasterForms.Event event, long received) {}
	private static final Map<Integer, Playback> EVENTS = new HashMap<>();
	public static void init() {
		key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.master_form", InputConstants.KEY_C,
			KeyMapping.Category.register(Wildercord.id("master_forms"))));
		ClientPlayNetworking.registerGlobalReceiver(MasterForms.Event.TYPE, (event, context) -> receive(context.client(), event));
		ClientPlayNetworking.registerGlobalReceiver(MasterFormLessons.Open.TYPE, (event, context) ->
			context.client().gui.setScreen(new MasterFormsScreen(context.client().gui.screen(), event.nonce(), true, event.form())));
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
			send(WallTurnRules.CANCEL); suppressed = true;
		});
		ClientTickEvents.END_CLIENT_TICK.register(MasterFormsClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Wildercord.id("master_form_state"), (graphics, delta) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.gui.screen() != null || client.gui.hud.isHidden() || MasterForms.data(client.player).equipped() == 0) return;
			Component line = state();
			int x = graphics.guiWidth() / 2, y = graphics.guiHeight() - 66;
			var lines = client.font.split(line, Math.max(80, graphics.guiWidth() - 12));
			int lineY = y - (lines.size() - 1) * 10;
			for (var part : lines) { graphics.centeredText(client.font, part, x, lineY, 0xFFD4EEE6); lineY += 10; }
			var view = MasterForms.view(client.player);
			int span = view.phase() == WallTurnRules.BRACE ? WallTurnRules.BRACE_TICKS : view.phase() == StoneHingeRules.BRACE ? StoneHingeRules.BRACE_TICKS
				: view.phase() == StoneHingeRules.CATCH ? StoneHingeRules.CATCH_TICKS : 0;
			if (span > 0) {
				graphics.fill(x - 35, y + 11, x + 35, y + 14, 0xCC203C38);
				graphics.fill(x - 35, y + 11, x - 35 + 70 * Math.clamp(view.ticks(), 0, span) / span, y + 14,
					view.phase() == StoneHingeRules.CATCH ? 0xFFE8C46A : 0xFFD4EEE6);
			}
		});
	}
	public static KeyMapping mapping() { return key; }
	public static Component binding() { return key == null ? Component.literal("C") : key.getTranslatedKeyMessage(); }
	public static void send(int action) {
		if (FormDashClient.route(action)) return;
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !ClientPlayNetworking.canSend(MasterForms.Action.TYPE)) return;
		long current = MasterForms.view(client.player).epoch();
		if (current == 0) return;
		if (epoch != current) { epoch = current; sequence = 0; }
		ClientPlayNetworking.send(new MasterForms.Action(action, epoch, ++sequence));
	}
	private static void tick(Minecraft client) {
		if (client.level != level) { clear(); level = client.level; suppressed = true; }
		boolean playing = client.player != null && client.player.isAlive() && !client.player.isSpectator() && !client.player.isCreative()
			&& client.gui.screen() == null && !client.isPaused();
		boolean clicked = false;
		while (key.consumeClick()) clicked = true;
		boolean down = key.isDown();
		if (!playing) {
			if (playingBefore) send(WallTurnRules.CANCEL);
			suppressed = true;
		} else if (suppressed) {
			if (!down) { suppressed = false; send(WallTurnRules.RELEASE); }
		} else {
			if (held && !down) send(WallTurnRules.RELEASE);
			if (clicked && !held) {
				send(WallTurnRules.PRESS);
				if (!down) send(WallTurnRules.RELEASE);
			}
		}
		held = down; playingBefore = playing;
		if (client.level != null) EVENTS.values().removeIf(play -> client.level.getGameTime() - play.received() > 30);
	}
	private static void receive(Minecraft client, MasterForms.Event event) {
		if (client.level == null || !WallTurnRules.phase(event.phase()) || event.ticks() < 0 || event.ticks() > Math.max(WallTurnRules.BRACE_TICKS, StoneHingeRules.CATCH_TICKS)
			|| !Float.isFinite(event.yaw()) || event.epoch() <= 0 || event.serial() <= 0) return;
		if (client.level != level) { clear(); level = client.level; }
		Playback prior = EVENTS.get(event.entity());
		if (prior != null && (prior.event().epoch() > event.epoch() || prior.event().epoch() == event.epoch() && prior.event().serial() >= event.serial())) return;
		// A re-sent kick step is stamped with the server tick, which is usually ahead of this client's clock; an event
		// can never be received in the local future, or every step samples a negative age and shows no pose.
		EVENTS.put(event.entity(), new Playback(event, Math.min(event.at(), client.level.getGameTime())));
	}
	public static Playback timeline(Avatar avatar) {
		if (avatar == null || avatar.level() != level || !avatar.isAlive()) return null;
		return EVENTS.get(avatar.getId());
	}
	public static Component state() {
		var player = Minecraft.getInstance().player;
		if (player == null) return Component.empty();
		var data = MasterForms.data(player); var view = MasterForms.view(player);
		if (data.equipped() == MasterForms.STONE_HINGE) {
			String hinge = !MasterForms.testedHinge(player) ? "testing" : view.phase() == StoneHingeRules.BRACE ? "brace" : view.phase() == StoneHingeRules.CATCH ? "catch"
				: view.recovery() > 0 ? "recovery" : view.rest() > 0 ? "rest" : "ready";
			return Component.translatable("hud.wildercord.stone_hinge." + hinge, binding(),
				String.format(java.util.Locale.ROOT, "%.1f", view.rest() / 20.0));
		}
		String state = !data.learned() && !data.hingeLearned() ? "locked" : data.equipped() == 0 ? "unequipped"
			: view.phase() == WallTurnRules.BRACE ? "brace" : view.phase() == WallTurnRules.KICK ? "kick"
			: view.recovery() > 0 ? "recovery" : view.commitment() > 0 ? "descent" : data.airborneUsed() ? "landing" : view.rest() > 0 ? "rest" : "ready";
		return Component.translatable("hud.wildercord.wall_turn." + state, binding(), String.format(java.util.Locale.ROOT, "%.1f", view.rest() / 20.0),
			String.format(java.util.Locale.ROOT, "%.1f", view.commitment() / 20.0));
	}
	private static void clear() { EVENTS.clear(); level = null; epoch = sequence = 0; held = false; suppressed = true; playingBefore = false; }
}
