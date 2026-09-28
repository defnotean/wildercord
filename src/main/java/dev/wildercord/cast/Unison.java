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

	/**
	 * Both colours at once: two shells of light racing out on crossed tilts, a cross of crescents
	 * (one of each colour) cut through the target, rings of both over the ground and the two
	 * circles turning against each other under it.
	 */
	private static void burst(ServerLevel level, LivingEntity target, int a, int b) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.6, target.getBbWidth());
		double spin = level.getRandom().nextDouble() * Math.PI;
		Vec3 facing = ElementFx.flatDir(spin);
		Vec3 side = facing.cross(new Vec3(0, 1, 0));
		for (int i = 0; i < 2; i++) {
			int color = i == 0 ? a : b;
			double yaw = spin + i * Math.PI / 2;
			ElementFx.ring(level, c, new Vec3(Math.cos(yaw), 0, Math.sin(yaw)), color, 0.3, 2.2 + w, 0.07, 10);
			Vec3 bulge = side.scale(i == 0 ? 1 : -1).add(0, 1, 0).normalize();
			double r = 1.2 + w * 0.3;
			ElementFx.slash(level, c.subtract(bulge.scale(r)), facing, bulge, color, r, 2.2, 0.22, 2, 8);
			ElementFx.groundRing(level, target.position(), color, 0.3, (i == 0 ? 2.8 : 2.2) + w, i == 0 ? 0.09 : 0.05, 12 + i * 2);
		}
		for (int i = 0; i < 12; i++) {
			double angle = Math.PI * 2 * i / 12;
			Vec3 dir = new Vec3(Math.cos(angle), 0.2 * Math.sin(angle * 3), Math.sin(angle));
			Vfx.fling(level, new DustParticleOptions(i % 2 == 0 ? a : b, 1.3F), c, dir, 0.3);
		}
		Vfx.emit(level, SigilOption.glow(0xFF000000 | a, 2.2F), c, 1, 0.0, 0.0);
		Vfx.emit(level, SigilOption.glow(0xFF000000 | b, 1.2F), c, 1, 0.0, 0.0);
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, a, 1.4F, 24, 0.12F), target.position().add(0, 0.07, 0));
		Sigils.send(level, SigilOption.flat(SigilOption.RING, b, 1.8F, 24, -0.12F), target.position().add(0, 0.08, 0));
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 1.0F, 1.6F);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
	}
}
