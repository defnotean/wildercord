package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unison: two casters striking the same foe with different elements within a second of each
 * other. The second hit lands 50% harder, both colours burst together, and both casters hear
 * about it. Built for playing together.
 */
public final class Unison {
	private Unison() {}

	public static final int WINDOW = 20;
	public static final double BONUS = 1.5;

	private record Mark(UUID caster, String element, long time) {}

	private static final Map<UUID, Mark> LAST = new HashMap<>();

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST.clear());
	}

	/** Called for every spell hit; returns the damage multiplier for this one. */
	static double onHit(Cast cast, LivingEntity target, String element) {
		if (!(cast.caster instanceof ServerPlayer caster) || element.isEmpty() || target instanceof ServerPlayer) {
			return 1.0;
		}
		long now = cast.level.getGameTime();
		Mark last = LAST.put(target.getUUID(), new Mark(caster.getUUID(), element, now));
		if (LAST.size() > 2048) {
			LAST.values().removeIf(m -> now - m.time() > WINDOW);
		}
		if (last == null || last.caster().equals(caster.getUUID()) || last.element().equals(element) || now - last.time() > WINDOW) {
			return 1.0;
		}
		ServerPlayer partner = cast.level.getServer().getPlayerList().getPlayer(last.caster());
		LAST.remove(target.getUUID());
		burst(cast.level, target, RuneColors.element(last.element()), RuneColors.element(element));
		Component message = Component.translatable("reaction.wildercord.unison").withColor(0xFFF0C0).withStyle(ChatFormatting.BOLD);
		caster.sendOverlayMessage(message);
		Grimoire.feat(caster, Feats.UNISON);
		if (partner != null) {
			partner.sendOverlayMessage(message);
			Grimoire.feat(partner, Feats.UNISON);
		}
		return BONUS;
	}

	private static void burst(ServerLevel level, LivingEntity target, int a, int b) {
		Vec3 c = target.getBoundingBox().getCenter();
		for (int i = 0; i < 24; i++) {
			double angle = Math.PI * 2 * i / 24;
			Vec3 dir = new Vec3(Math.cos(angle), 0.2 * Math.sin(angle * 3), Math.sin(angle));
			Vfx.fling(level, new DustParticleOptions(i % 2 == 0 ? a : b, 1.3F), c, dir, 0.3);
		}
		Vfx.emit(level, SigilOption.glow(0xFF000000 | a, 2.2F), c, 1, 0.0, 0.0);
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, a, 1.4F, 24, 0.12F), target.position().add(0, 0.07, 0));
		Sigils.send(level, SigilOption.flat(SigilOption.RING, b, 1.8F, 24, -0.12F), target.position().add(0, 0.08, 0));
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 1.0F, 1.6F);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
	}
}
