package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * Rhythm casting: cast again just as your last spell comes off cooldown, on the beat, and the
 * chain grows (up to three), each step adding power. A late or early cast simply starts over;
 * nothing is lost but the bonus. The HUD shows the beat coming.
 */
public final class Rhythm {
	private Rhythm() {}

	public static final int MAX = 3;
	public static final double POWER_PER_STACK = 0.08;

	/** How long the beat lasts: a quarter of the cooldown, between a quarter and half a second. */
	public static int window(int cooldown) {
		return Math.max(5, Math.min(10, cooldown / 4));
	}

	/** Called on every successful cast; returns the power multiplier this cast gets. */
	public static double onCast(ServerPlayer player, long now, int cooldown) {
		WildercordAttachments.Rhythm rhythm = player.getAttachedOrElse(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		boolean onBeat = rhythm.windowEnd() > 0 && now >= rhythm.windowStart() && now <= rhythm.windowEnd();
		int stacks = onBeat ? Math.min(MAX, rhythm.stacks() + 1) : 0;
		if (onBeat) {
			float pitch = 0.9F + 0.25F * stacks;
			Fx.sound(player.level(), player.position(), SoundEvents.NOTE_BLOCK_CHIME, 0.8F, pitch);
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, pitch);
			player.sendOverlayMessage(Component.translatable("message.wildercord.rhythm", stacks, Math.round(stacks * POWER_PER_STACK * 100))
				.withColor(0xF5D56A).withStyle(ChatFormatting.BOLD));
			TechniqueVfx.rhythm(player.level(), player, stacks);
			if (stacks >= MAX) {
				Grimoire.feat(player, Feats.RHYTHM);
			}
		}
		long start = now + cooldown;
		player.setAttached(WildercordAttachments.RHYTHM, new WildercordAttachments.Rhythm(stacks, start, start + window(cooldown)));
		// The bonus counts for the cast that landed on the beat.
		return 1 + stacks * POWER_PER_STACK;
	}

	/** Drops the chain once its beat has passed unanswered. */
	public static void tick(ServerPlayer player) {
		WildercordAttachments.Rhythm rhythm = player.getAttachedOrElse(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		if (rhythm.stacks() > 0 && player.level().getGameTime() > rhythm.windowEnd() + 2) {
			player.setAttached(WildercordAttachments.RHYTHM, new WildercordAttachments.Rhythm(0, rhythm.windowStart(), rhythm.windowEnd()));
		}
	}
}
