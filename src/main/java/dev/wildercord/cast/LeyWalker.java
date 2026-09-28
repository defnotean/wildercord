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
 * form a Heart Circle).
 */
public final class LeyWalker {
	private LeyWalker() {}

	/** How strong a line has to be underfoot to count as standing on it. */
	public static final double ON_LINE = 0.45;

	public static long seed(ServerLevel level) {
		return LeyLines.seedOf(level.getServer().overworld().getSeed());
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ServerPlayNetworking.send(handler.player, new WildercordNetworking.LeySeed(LeyLines.seedOf(server.overworld().getSeed()))));
	}

	public static boolean onLine(ServerPlayer player) {
		return player.level().dimension() == Level.OVERWORLD
			&& LeyLines.strength(seed(player.level()), player.getX(), player.getZ()) >= ON_LINE;
	}

	/** Every 5 ticks, for players wearing a Cord. */
	public static void tick(ServerPlayer player) {
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
}
