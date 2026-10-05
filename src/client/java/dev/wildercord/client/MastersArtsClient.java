package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersArts;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated, rebindable combat inputs and server-confirmed art playback. The request contains
 * only the chosen move: the server owns eligibility, targets, damage, costs and the whole timeline.
 */
public final class MastersArtsClient {
	private MastersArtsClient() {}

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Wildercord.id("masters_arts"));
	private static final String[] IDS = {"spellcut", "rising_break", "driving_cut"};
	private static final int[] DEFAULTS = {InputConstants.KEY_U, InputConstants.KEY_Y, InputConstants.KEY_J};
	private static final KeyMapping[] KEYS = new KeyMapping[IDS.length];
	private static final boolean[] HELD = new boolean[IDS.length];
	private static final Map<Integer, MastersArts.Performed> PLAYING = new HashMap<>();
	private static ClientLevel playbackLevel;

	public static void init() {
		for (int move = 0; move < KEYS.length; move++) {
			KEYS[move] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord." + IDS[move], DEFAULTS[move], CATEGORY));
		}
		ClientPlayNetworking.registerGlobalReceiver(MastersArts.Performed.TYPE, (payload, context) -> receive(context.client(), payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
		ClientTickEvents.END_CLIENT_TICK.register(MastersArtsClient::tick);
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Wildercord.id("masters_committed_aim"), MastersArtsClient::aimHint);
	}

	/** The registered mapping, also used by the client game tests to press the real combat controls. */
	public static KeyMapping mapping(int move) {
		if (move < 0 || move >= KEYS.length) throw new IllegalArgumentException("Unknown Master's Art: " + move);
		return KEYS[move];
	}

	/** Help reads the active bindings so it stays correct after a player changes them. */
	public static List<Component> help() {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.wildercord.aura.masters_help").withStyle(ChatFormatting.GOLD));
		for (int move = 0; move < KEYS.length; move++) {
			Component key = KEYS[move] == null ? Component.translatable("key.keyboard.unknown") : KEYS[move].getTranslatedKeyMessage();
			lines.add(Component.translatable("screen.wildercord.aura.masters_" + IDS[move], key));
			lines.add(Component.translatable("aura.wildercord.art." + IDS[move] + ".desc").withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("screen.wildercord.aura.masters_rebind").withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	private static void tick(Minecraft client) {
		checkLevel(client);
		if (client.level != null) {
			long now = client.level.getGameTime();
			PLAYING.values().removeIf(move -> now - move.startTick() >= move.windup() + move.recovery() || now - move.startTick() < -40);
		}
		boolean playing = client.player != null && client.level != null && client.player.isAlive() && !client.player.isSpectator()
			&& client.gui.screen() == null && !client.isPaused();
		for (int move = 0; move < KEYS.length; move++) {
			boolean pressed = false;
			while (KEYS[move].consumeClick()) {
				// Drain even while in a menu or dead, so an old press cannot fire on returning to play.
				pressed = true;
			}
			boolean fresh = pressed && !HELD[move];
			HELD[move] = KEYS[move].isDown();
			// OS key repeat must not keep spending aura as soon as each cooldown ends.
			if (fresh && playing && ClientPlayNetworking.canSend(MastersArts.Activate.TYPE)) {
				ClientPlayNetworking.send(new MastersArts.Activate(move));
			}
		}
	}

	private static void receive(Minecraft client, MastersArts.Performed payload) {
		checkLevel(client);
		if (client.level == null || !MastersArtAnimation.supports(payload.move())) return;
		MastersArts.Performed previous = PLAYING.get(payload.entity());
		// A delayed cancellation must not erase a newer art from the same fighter.
		if (previous != null && payload.startTick() < previous.startTick()) return;
		if (payload.windup() == 0 && payload.recovery() == 0) {
			PLAYING.remove(payload.entity());
			return;
		}
		if (payload.windup() <= 0 || payload.windup() > 60 || payload.recovery() <= 0 || payload.recovery() > 120
				|| !Float.isFinite(payload.yaw()) || !Float.isFinite(payload.pitch()) || Math.abs(payload.pitch()) > 90) return;
		PLAYING.put(payload.entity(), payload);
	}

	/** A large free-look turn gets a brief direction cue instead of dragging the player's camera back. */
	private static void aimHint(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.screen() != null || client.gui.hud.isHidden() || !client.options.getCameraType().isFirstPerson()) return;
		var timeline = timeline(client.player);
		if (timeline == null) return;
		float partial = delta.getGameTimeDeltaPartialTick(false);
		if (pose(client.player, partial).weight() < .25F) return;
		float yaw = net.minecraft.util.Mth.wrapDegrees(timeline.yaw() - client.player.getViewYRot(partial));
		float pitch = timeline.pitch() - client.player.getViewXRot(partial);
		String direction = MastersArtAnimation.offscreenDirection(yaw, pitch);
		if (!direction.isEmpty()) graphics.centeredText(client.font, Component.translatable("hud.wildercord.masters_aim_" + direction),
			graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 27, 0xFFE8C46A);
	}

	/** A cosmetic sample for this avatar, never a prediction that a requested attack succeeded. */
	public static MastersArtAnimation.Pose pose(Avatar avatar, float partial) {
		MastersArts.Performed move = timeline(avatar);
		if (move == null) return MastersArtAnimation.NONE;
		float age = (avatar.level().getGameTime() - move.startTick()) + partial;
		return MastersArtAnimation.sample(move.move(), age, move.windup(), move.recovery());
	}

	/** The current accepted timeline, for presentation and native verification; absent after recovery. */
	public static MastersArts.Performed timeline(Avatar avatar) {
		if (avatar == null || avatar.level() != playbackLevel || !avatar.isAlive()) return null;
		MastersArts.Performed move = PLAYING.get(avatar.getId());
		if (move == null) return null;
		long age = avatar.level().getGameTime() - move.startTick();
		return age < -40 || age >= move.windup() + move.recovery() ? null : move;
	}

	private static void checkLevel(Minecraft client) {
		if (client.level != playbackLevel) {
			PLAYING.clear();
			playbackLevel = client.level;
		}
	}

	private static void clear() {
		PLAYING.clear();
		playbackLevel = null;
		java.util.Arrays.fill(HELD, false);
	}
}
