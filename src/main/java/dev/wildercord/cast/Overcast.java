package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.Feats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Overcasting: casting a spell you can't afford by cracking your outermost Heart Circle to pay
 * for it. A cracked circle gives nothing (its mana, regeneration, power or perk) until it mends,
 * three minutes later; overcasting again cracks the next one in and starts the clock over.
 * It takes a second press to confirm, so it never happens by accident.
 */
public final class Overcast {
	private Overcast() {}

	/** How long cracked circles take to mend. */
	public static final int MEND_TICKS = 20 * 180;
	/** How long the second press has to come. */
	private static final int CONFIRM_TICKS = 40;

	private record Prompt(int spell, long until) {}

	private static final Map<UUID, Prompt> PROMPTS = new HashMap<>();

	/**
	 * Called when a spell costs more than the mana there is. The first press explains; a second
	 * press of the same spell soon after overcasts. Returns true if the cast should go ahead.
	 */
	public static boolean confirm(ServerPlayer player, int spell, int mana, int cost) {
		long now = player.level().getGameTime();
		int active = Heart.active(player);
		if (active <= 0) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.no_mana", mana, cost).withStyle(ChatFormatting.RED));
			return false;
		}
		Prompt prompt = PROMPTS.get(player.getUUID());
		if (prompt == null || prompt.spell() != spell || now > prompt.until()) {
			PROMPTS.put(player.getUUID(), new Prompt(spell, now + CONFIRM_TICKS));
			player.sendOverlayMessage(Component.translatable("message.wildercord.overcast_prompt", mana, cost, Circles.ordinal(active))
				.withColor(0xFF8A5A));
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 0.5F);
			return false;
		}
		PROMPTS.remove(player.getUUID());
		crack(player, active);
		return true;
	}

	/** Cracks the outermost working circle. */
	private static void crack(ServerPlayer player, int active) {
		long now = player.level().getGameTime();
		int cracked = Heart.cracked(player) + 1;
		player.setAttached(WildercordAttachments.CRACKS, new WildercordAttachments.Cracks(cracked, now + MEND_TICKS));
		ServerLevel level = player.level();
		Vec3 heart = player.position().add(0, 1.2, 0);
		// The ring bursts: it snaps outward in red light and breaks into shards, and the circle breaks on the ground.
		double r = 0.3 + 0.09 * (active - 1);
		Vec3 up = new Vec3(0, 1, 0);
		ElementFx.ring(level, heart, up, CRACK, r, r + 1.6, 0.05, 7);
		ElementFx.ring(level, heart, up, 0xFFE0C0, r, r + 1.1, 0.03, 9);
		double turn = level.getRandom().nextDouble() * Math.PI;
		for (int k = 0; k < 8; k++) {
			double a = turn + Math.PI * 2 * k / 8;
			Vec3 dir = new Vec3(Math.cos(a), k % 2 == 0 ? 0.15 : -0.15, Math.sin(a));
			ElementFx.ray(level, heart.add(dir.scale(r)), heart.add(dir.scale(r + 0.5 + level.getRandom().nextDouble() * 0.4)), k % 2 == 0 ? CRACK : 0xFFE0C0,
				0.035, 6);
		}
		for (int k = 0; k < 12; k++) {
			double a = Math.PI * 2 * k / 12;
			Vec3 dir = new Vec3(Math.cos(a), 0.15, Math.sin(a));
			Vfx.fling(level, new DustParticleOptions(k % 2 == 0 ? CRACK : 0xFFE0C0, 1.1F), heart.add(dir.scale(r)), dir, 0.25);
		}
		// Under your feet, where you'll see it too.
		ElementFx.groundRing(level, player.position(), CRACK, 0.3, 1.9, 0.06, 10);
		Vfx.radial(level, ParticleTypes.CRIT, heart, 10, 0.4);
		Sigils.send(level, SigilOption.flat(SigilOption.CRACKED, 0xFF5A3A, 1.6F, 30, 0.0F), player.position().add(0, 0.07, 0));
		Fx.sound(level, heart, dev.wildercord.content.WildercordSounds.OVERCAST, 1.0F, 1.0F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.overcast", Circles.ordinal(active), MEND_TICKS / 1200)
			.withColor(0xFF6A4A));
		Grimoire.feat(player, Feats.OVERCAST);
	}

	private static final int CRACK = 0xFF6A4A;

	/** Every few ticks: mends the circles once their time is up. */
	public static void tick(ServerPlayer player) {
		WildercordAttachments.Cracks cracks = player.getAttachedOrElse(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
		if (cracks.count() > 0 && player.level().getGameTime() >= cracks.until()) {
			player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
			player.sendSystemMessage(Component.translatable("message.wildercord.mended").withColor(0x9AD8FF));
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.2F);
		}
	}

	public static void forget(UUID player) {
		PROMPTS.remove(player);
	}
}
