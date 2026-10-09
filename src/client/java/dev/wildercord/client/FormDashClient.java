package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.FormDash;
import dev.wildercord.aura.FormDashLessons;
import dev.wildercord.aura.FormDashRules;
import dev.wildercord.aura.WallTurnRules;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;

import java.util.HashMap;
import java.util.Map;

/** The field forms share the Master-form key: its edges go to whichever form holds the slot. Playback is from accepted events. */
public final class FormDashClient {
	private FormDashClient() {}
	private static long epoch, sequence;
	private static ClientLevel level;
	public record Playback(FormDash.Event event, long received) {}
	private static final Map<Integer, Playback> EVENTS = new HashMap<>();
	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(FormDash.Event.TYPE, (event, context) -> receive(context.client(), event));
		ClientPlayNetworking.registerGlobalReceiver(FormDashLessons.Open.TYPE, (event, context) -> {
			if (FormDashRules.form(event.form()))
				context.client().gui.setScreen(new FormDashScreen(context.client().gui.screen(), event.form(), event.nonce(), true));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != level) { clear(); level = client.level; }
			if (client.level != null) EVENTS.values().removeIf(play -> client.level.getGameTime() - play.received() > FormDashRules.MAX_TICKS + 10);
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Wildercord.id("field_form_state"), (graphics, delta) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.gui.screen() != null || client.gui.hud.isHidden() || !shown(client)) return;
			int x = graphics.guiWidth() / 2, y = graphics.guiHeight() - 66;
			var lines = client.font.split(state(), Math.max(80, graphics.guiWidth() - 12));
			int lineY = y - (lines.size() - 1) * 10;
			for (var part : lines) { graphics.centeredText(client.font, part, x, lineY, 0xFFF0DCC8); lineY += 10; }
			var view = FormDash.view(client.player);
			int span = FormDashRules.setTicks(view.form());
			if (view.phase() == FormDashRules.SET && span > 0) {
				graphics.fill(x - 35, y + 11, x + 35, y + 14, 0xCC3C2420);
				graphics.fill(x - 35, y + 11, x - 35 + 70 * Math.clamp(view.ticks(), 0, span) / span, y + 14, 0xFFF0B070);
			}
		});
	}
	/**
	 * True when a field form took this press/release edge: a field form holds the slot, or a perfect guard opened a follow-up
	 * moment. A release goes wherever its press went, so neither side sees half a press.
	 */
	static boolean route(int action) {
		Minecraft client = Minecraft.getInstance();
		if (action != WallTurnRules.PRESS && action != WallTurnRules.RELEASE || client.player == null) return false;
		boolean slot = FormDash.data(client.player).equipped() != 0;
		if (action == WallTurnRules.PRESS) {
			pressed = slot || FormDash.view(client.player).window() > 0;
			if (!pressed) return false;
		} else {
			boolean took = pressed || slot;
			pressed = false;
			if (!took) return false;
		}
		send(action, 0);
		return true;
	}
	// ---- moves pack
	private static boolean pressed;
	/** The line shows for a slotted form, an open follow-up moment, or a follow-up or Spell Cut still playing out. */
	private static boolean shown(Minecraft client) {
		var view = FormDash.view(client.player);
		return FormDash.data(client.player).equipped() != 0 || view.window() > 0 || view.phase() != FormDashRules.IDLE && FormDashRules.form(view.form());
	}
	/** Spell Cut rides the vanilla attack: a swing with a hostile spell in reach asks the server to judge the cut. */
	public static void swing() {
		Minecraft client = Minecraft.getInstance();
		var player = client.player;
		if (player == null || client.level == null || player.isSpectator() || !FormDash.data(player).knows(FormDashRules.SPELL_CUT)
			|| !ClientPlayNetworking.canSend(FormDash.Counter.TYPE)) return;
		long current = FormDash.view(player).epoch();
		if (current == 0) return;
		double reach = dev.wildercord.spell.SpellCutRules.COUNTER_ARM + 1;
		if (client.level.getEntitiesOfClass(dev.wildercord.cast.RuneBolt.class, player.getBoundingBox().inflate(reach), bolt -> true).isEmpty()) return;
		ClientPlayNetworking.send(new FormDash.Counter(current));
	}
	public static void send(int action, int form) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !ClientPlayNetworking.canSend(FormDash.Action.TYPE)) return;
		long current = FormDash.view(client.player).epoch();
		if (current == 0) return;
		if (epoch != current) { epoch = current; sequence = 0; }
		ClientPlayNetworking.send(new FormDash.Action(action, form, epoch, ++sequence));
	}
	private static void receive(Minecraft client, FormDash.Event event) {
		if (client.level == null || !FormDashRules.form(event.form()) || !FormDashRules.phase(event.phase()) || event.ticks() < 0
			|| event.ticks() > FormDashRules.MAX_TICKS || !Float.isFinite(event.yaw()) || event.epoch() <= 0 || event.serial() <= 0) return;
		if (client.level != level) { clear(); level = client.level; }
		Playback prior = EVENTS.get(event.entity());
		if (prior != null && (prior.event().epoch() > event.epoch() || prior.event().epoch() == event.epoch() && prior.event().serial() >= event.serial())) return;
		EVENTS.put(event.entity(), new Playback(event, event.at()));
		puff(client, event);
	}
	/** A few motes at the feet. Reduced flash keeps a third and drops the flame; nothing moves the camera. */
	private static void puff(Minecraft client, FormDash.Event event) {
		var entity = client.level.getEntity(event.entity());
		if (entity == null || client.options.particles().get() == net.minecraft.server.level.ParticleStatus.MINIMAL) return;
		boolean cinder = event.form() == FormDashRules.CINDER_LUNGE || event.form() == FormDashRules.RIPOSTE || event.form() == FormDashRules.SPELL_CUT;
		boolean stone = event.form() == FormDashRules.PLUNGE || event.form() == FormDashRules.GUARD_BREAK;
		int count = switch (event.phase()) {
			case FormDashRules.SET, FormDashRules.CUT, FormDashRules.STRIKE, FormDashRules.SEVER -> 6;
			case FormDashRules.LUNGE, FormDashRules.SLIP, FormDashRules.RISE, FormDashRules.DIVE -> 8;
			case FormDashRules.LAND -> 10; case FormDashRules.STALL -> 4; case FormDashRules.MISS -> 2; default -> 0;
		};
		boolean blow = event.phase() == FormDashRules.CUT || event.phase() == FormDashRules.STRIKE || event.phase() == FormDashRules.SEVER;
		if (MagicQuality.reducedFlash) count /= 3;
		var random = client.level.getRandom();
		double yaw = Math.toRadians(event.yaw()), fx = -Math.sin(yaw), fz = Math.cos(yaw);
		for (int i = 0; i < count; i++) {
			double ox = (random.nextDouble() - .5) * .8, oz = (random.nextDouble() - .5) * .8;
			var particle = blow ? ParticleTypes.CRIT : cinder ? MagicQuality.reducedFlash ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME
				: stone ? ParticleTypes.POOF : ParticleTypes.CLOUD;
			client.level.addParticle(particle, entity.getX() + ox + (blow ? fx * 1.2 : 0), entity.getY() + .1 + random.nextDouble() * (blow ? 1.2 : .3),
				entity.getZ() + oz + (blow ? fz * 1.2 : 0), -fx * .02, .01, -fz * .02);
		}
	}
	public static Playback timeline(Avatar avatar) {
		if (avatar == null || avatar.level() != level || !avatar.isAlive()) return null;
		return EVENTS.get(avatar.getId());
	}
	public static Component state() {
		var player = Minecraft.getInstance().player;
		if (player == null) return Component.empty();
		var data = FormDash.data(player); var view = FormDash.view(player);
		if (view.window() > 0 && view.phase() == FormDashRules.IDLE) return Component.translatable("hud.wildercord.follow.window", MasterFormsClient.binding());
		int form = view.phase() != FormDashRules.IDLE && FormDashRules.form(view.form()) ? view.form() : data.equipped();
		if (form == 0) return Component.empty();
		int phase = view.phase();
		if (phase == FormDashRules.LAND || phase == FormDashRules.SPLASH || phase == FormDashRules.SEVER || phase == FormDashRules.MISS)
			return Component.translatable("hud.wildercord.field_form." + (phase == FormDashRules.LAND ? "land" : phase == FormDashRules.SPLASH ? "splash" : phase == FormDashRules.SEVER ? "sever" : "miss"));
		String state = phase == FormDashRules.SET ? "set" : phase == FormDashRules.LUNGE || phase == FormDashRules.SLIP || phase == FormDashRules.RISE || phase == FormDashRules.DIVE ? "travel"
			: view.recovery() > 0 ? "recovery" : view.rest() > 0 ? "rest" : "ready";
		return Component.translatable("hud.wildercord." + FormDash.name(form) + "." + state, MasterFormsClient.binding(),
			String.format(java.util.Locale.ROOT, "%.1f", view.rest() / 20.0));
	}
	private static void clear() { EVENTS.clear(); level = null; epoch = sequence = 0; pressed = false; }
}
