package dev.wildercord.cast.packs;

import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Sigils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The support pack's looks and sounds, kept small and soft: mending is green-gold motes and a chime, a ward is a ring of
 * light on the ground, a calmed creature gets a drifting note, a refused blow or hex a bright flash.
 */
final class WardVfx {
	private WardVfx() {}

	private static Vec3 chest(Entity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	/** Health or mana flowing into {@code e}. */
	static void mend(ServerLevel level, Entity e, int color, int motes) {
		Vec3 at = chest(e);
		Fx.send(level, Fx.dust(color, 1.1F), at, motes * 3, 0.35, 0.02);
		Fx.send(level, ParticleTypes.HAPPY_VILLAGER, at, motes, 0.4, 0.0);
	}

	static void heal(ServerLevel level, Entity e, int color) {
		mend(level, e, color, 4);
		Light.orb(level, chest(e), color, 0.6, 10);
		Fx.sound(level, e.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.35F);
	}

	/** A buff settling on {@code e}: a thin halo at its feet and a soft clink. */
	static void buff(ServerLevel level, Entity e, int color) {
		Light.groundRing(level, e.position().add(0, 0.05, 0), color, 0.3, 0.9, 0.12, 14);
		Fx.send(level, Fx.dust(color, 1.0F), chest(e), 10, 0.4, 0.02);
		Fx.sound(level, e.position(), SoundEvents.BEACON_POWER_SELECT, 0.35F, 1.6F);
	}

	/** A guard put on {@code e}: a shell of motes and an iron clink. */
	static void guard(ServerLevel level, Entity e, int color) {
		Sigils.target(level, chest(e), color, 0.9F, 20);
		Fx.send(level, ParticleTypes.WAX_ON, chest(e), 8, 0.45, 0.0);
		Fx.sound(level, e.position(), SoundEvents.ARMOR_EQUIP_IRON, 0.7F, 1.2F);
	}

	/** A place ward raised: a sigil and a ring on the ground, and a bell-like tone. */
	static void place(ServerLevel level, Vec3 at, double radius, int color, int secondary, int lifetime) {
		Sigils.ground(level, at, color, secondary, (float) Math.min(4.0, radius * 0.5), Math.min(lifetime, 60));
		Light.groundRing(level, at.add(0, 0.05, 0), color, Math.max(0.1, radius - 0.4), radius, 0.18, 30);
		Fx.sound(level, at, SoundEvents.BELL_RESONATE, 0.45F, 1.4F);
	}

	/** A standing ward's outline, refreshed now and then. */
	static void ring(ServerLevel level, Vec3 at, double radius, int color) {
		Light.groundRing(level, at.add(0, 0.05, 0), color, Math.max(0.1, radius - 0.25), radius, 0.1, 22);
	}

	static void mist(ServerLevel level, Vec3 at, double radius, int color) {
		Fx.send(level, ParticleTypes.CLOUD, at.add(0, 0.6, 0), (int) Math.min(30, 6 + radius * 3), radius * 0.45, 0.005);
		Fx.send(level, Fx.dust(color, 1.3F), at.add(0, 0.8, 0), (int) Math.min(24, 4 + radius * 3), radius * 0.45, 0.0);
	}

	static void glow(ServerLevel level, Vec3 at, double radius, int color) {
		Fx.send(level, Fx.dust(color, 1.2F), at.add(0, 0.5, 0), (int) Math.min(24, 4 + radius * 3), radius * 0.45, 0.01);
		Fx.send(level, ParticleTypes.SMALL_FLAME, at.add(0, 0.3, 0), 4, radius * 0.35, 0.005);
	}

	static void well(ServerLevel level, Vec3 at, double radius, int color) {
		Fx.send(level, ParticleTypes.ENCHANT, at.add(0, 1.0, 0), 16, radius * 0.4, 0.4);
		Fx.send(level, Fx.dust(color, 1.0F), at.add(0, 0.4, 0), 10, radius * 0.4, 0.02);
	}

	/** A soothing note over {@code e}: it has calmed down (or been drawn off). */
	static void calm(ServerLevel level, Entity e, int color) {
		Fx.send(level, ParticleTypes.NOTE, e.position().add(0, e.getBbHeight() + 0.4, 0), 2, 0.2, 0.5);
		Fx.send(level, Fx.dust(color, 1.0F), chest(e), 8, 0.35, 0.01);
		Fx.sound(level, e.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 0.8F);
	}

	/** {@code e} held, slowed or herded without harm. */
	static void bind(ServerLevel level, Entity e, int color) {
		Light.groundRing(level, e.position().add(0, 0.05, 0), color, 0.2, 0.8, 0.14, 16);
		Fx.send(level, Fx.dust(color, 1.1F), chest(e), 12, 0.3, 0.0);
		Fx.sound(level, e.position(), SoundEvents.CHAIN_PLACE, 0.55F, 1.1F);
	}

	/** Something turned away at {@code at}: a missile, a hex, a blow. */
	static void refuse(ServerLevel level, Vec3 at, int color) {
		Sigils.flash(level, at, color, 0.7F);
		Fx.send(level, ParticleTypes.END_ROD, at, 4, 0.15, 0.03);
	}

	static void refuse(ServerLevel level, Entity e, int color) {
		refuse(level, chest(e), color);
		Fx.sound(level, e.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.5F, 1.5F);
	}

	static void evade(ServerLevel level, Entity e) {
		Fx.send(level, ParticleTypes.CLOUD, e.position().add(0, 0.4, 0), 8, 0.3, 0.05);
		Fx.sound(level, e.position(), SoundEvents.PLAYER_BREATH, 0.6F, 1.6F);
	}

	/** A strand between two creatures (Guardlink's share passing, a taunt). */
	static void tether(ServerLevel level, Entity from, Entity to, int color) {
		Light.ray(level, chest(from), chest(to), color, 0.06, 8);
	}

	/** A death turned aside. */
	static void saved(ServerLevel level, Entity e, int color) {
		Fx.send(level, ParticleTypes.TOTEM_OF_UNDYING, chest(e), 30, 0.4, 0.3);
		Light.orb(level, chest(e), color, 1.0, 16);
		Fx.sound(level, e.position(), SoundEvents.TOTEM_USE, 0.6F, 1.3F);
	}

	/** Nothing to do: a small grey puff. */
	static void fizzle(ServerLevel level, Vec3 at) {
		Fx.send(level, ParticleTypes.SMOKE, at, 6, 0.2, 0.01);
	}
}
