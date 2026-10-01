package dev.wildercord.cast;

import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

/**
 * Players and ley lines: tells each client the ley seed when it joins (so it can draw the lines),
 * and keeps track of who is standing on one (twice the mana regeneration, twice as quick to
 * form a Heart Circle), and who on a ley crossing, where two lines meet: a place of power, where
 * every spell is a little stronger and cheaper (see {@link Climate}).
 */
public final class LeyWalker {
	private LeyWalker() {}

	/** How strong a line has to be underfoot to count as standing on it. */
	public static final double ON_LINE = 0.45;

	public static long seed(ServerLevel level) {
		return LeyLines.seedOf(level.getServer().overworld().getSeed());
	}

	/** Who stood on a ley crossing at their last check, so stepping onto one is told once. */
	private static final java.util.Set<java.util.UUID> AT_CROSSING = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ServerPlayNetworking.send(handler.player, new WildercordNetworking.LeySeed(LeyLines.seedOf(server.overworld().getSeed()))));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AT_CROSSING.remove(handler.player.getUUID()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> AT_CROSSING.clear());
	}

	/** Whether a player stands where two ley lines cross (Overworld only, as the lines are). */
	public static boolean atCrossing(ServerPlayer player) {
		return player.level().dimension() == Level.OVERWORLD && LeyLines.atCrossing(seed(player.level()), player.getX(), player.getZ());
	}

	public static boolean onLine(ServerPlayer player) {
		return player.level().dimension() == Level.OVERWORLD
			&& LeyLines.strength(seed(player.level()), player.getX(), player.getZ()) >= ON_LINE;
	}

	/** Every 5 ticks, for players wearing a Cord. */
	public static void tick(ServerPlayer player) {
		crossing(player);
		boolean on = onLine(player);
		boolean was = player.getAttachedOrElse(WildercordAttachments.ON_LEY, false);
		if (on == was) {
			return;
		}
		player.setAttached(WildercordAttachments.ON_LEY, on);
		if (on) {
			if (Grimoire.feat(player, Feats.LEY_LINE)) {
				player.sendSystemMessage(Component.translatable("message.wildercord.ley_first").withColor(0xB8A0FF));
			}
			player.sendOverlayMessage(Component.translatable("message.wildercord.ley_on").withColor(0xB8A0FF));
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.4F);
			Vfx.emit(player.level(), ParticleTypes.END_ROD, player.position().add(0, 0.2, 0), 8, 0.5, 0.02);
		}
	}

	/** Stepping onto a ley crossing: a short line above the hotbar, a chord, and the first time a Grimoire entry. */
	private static void crossing(ServerPlayer player) {
		boolean on = dev.wildercord.config.Config.get().elementalClimate() && dev.wildercord.config.Config.get().power().leyCrossings() && atCrossing(player);
		boolean was = AT_CROSSING.contains(player.getUUID());
		if (on == was) {
			return;
		}
		if (!on) {
			AT_CROSSING.remove(player.getUUID());
			return;
		}
		AT_CROSSING.add(player.getUUID());
		if (Grimoire.feat(player, Feats.LEY_CROSSING)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.ley_crossing_first").withColor(0xD8C8FF));
		}
		player.sendOverlayMessage(Component.translatable("message.wildercord.ley_crossing").withColor(0xD8C8FF));
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.0F);
		Fx.sound(player.level(), player.position(), SoundEvents.BEACON_AMBIENT, 0.35F, 1.5F);
		Vfx.emit(player.level(), new dev.wildercord.content.MaterialOption(dev.wildercord.content.MaterialOption.ARCANE, 0xD8CCFF, 0.14F, 40),
			player.position().add(0, 0.3, 0), 14, 0.7, 0.03);
	}
}
