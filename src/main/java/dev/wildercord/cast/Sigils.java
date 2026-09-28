package dev.wildercord.cast;

import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellSigil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Magic circles in the world, built from {@link SigilOption} layers: a main circle, a rune ring
 * around it turning the other way, and a star inside. Sent to every player within 128 blocks,
 * including one standing right on top of it: a circle under your feet never blocks the view.
 */
public final class Sigils {
	private Sigils() {}

	private static final double RANGE = 128.0;

	public static void send(ServerLevel level, net.minecraft.core.particles.ParticleOptions sigil, Vec3 at) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(at) <= RANGE * RANGE) {
				level.sendParticles(player, sigil, true, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** The yaw and pitch that make a sigil face along {@code normal}. */
	static float yaw(Vec3 normal) {
		return (float) Math.toDegrees(Math.atan2(-normal.x, normal.z));
	}

	static float pitch(Vec3 normal) {
		return (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -normal.normalize().y))));
	}

	public static void layer(ServerLevel level, Vec3 at, Vec3 normal, int style, int color, float size, int lifetime, float spin) {
		send(level, new SigilOption(style, color, size, yaw(normal), pitch(normal), lifetime, spin), at);
	}

	/** A full magic circle facing {@code normal}: circle, outer rune ring and inner star. */
	public static void circle(ServerLevel level, Vec3 at, Vec3 normal, int color, int secondary, float size, int lifetime) {
		layer(level, at, normal, SigilOption.CIRCLE, color, size, lifetime, 0.04F);
		layer(level, at.add(normal.normalize().scale(0.01)), normal, SigilOption.RING, secondary, size * 1.25F, lifetime, -0.06F);
		layer(level, at.add(normal.normalize().scale(0.02)), normal, SigilOption.STAR, secondary, size * 0.55F, lifetime, 0.1F);
	}

	/**
	 * A spell's own magic circle (see {@link SpellSigil}), {@code radius} blocks across its frame,
	 * facing along {@code normal}: one particle that every client builds from the spell's runes, so
	 * the circle can be read. {@code color} is the frame's, script's and star's.
	 */
	public static void spell(ServerLevel level, Vec3 at, Vec3 normal, List<RuneDef> runes, int color, float radius, int lifetime) {
		Vec3 n = normal.normalize();
		List<String> ids = runes.stream().limit(SpellSigil.MAX_RUNES).map(RuneDef::id).toList();
		send(level, new SpellCircleOption(ids, color & 0xFFFFFF, radius, yaw(n), pitch(n), lifetime), at);
	}

	/** A full magic circle lying on the ground. */
	public static void ground(ServerLevel level, Vec3 at, int color, int secondary, float size, int lifetime) {
		circle(level, at.add(0, 0.06, 0), new Vec3(0, 1, 0), color, secondary, size, lifetime);
	}

	/** A warning reticle on the ground where something is about to land. */
	public static void target(ServerLevel level, Vec3 at, int color, float size, int lifetime) {
		send(level, SigilOption.flat(SigilOption.TARGET, color, size, lifetime, 0.03F), at.add(0, 0.07, 0));
	}

	/**
	 * A flash of light, {@code size} blocks across at its brightest, facing whoever sees it. Used
	 * for impacts and bursts in place of the vanilla firework flash, which is several blocks wide
	 * and shows up close as a pale square.
	 */
	public static void flash(ServerLevel level, Vec3 at, int color, float size) {
		// Through Fx.send, which leaves it out for a player whose eyes it would sit in front of.
		Fx.send(level, SigilOption.glow(color, size), at, 1, 0.0, 0.0);
	}

	/** A circle facing out from a caster's hands, as a telegraph. */
	public static void telegraph(ServerLevel level, Vec3 at, Vec3 facing, int color, float size, int lifetime) {
		layer(level, at, facing, SigilOption.CIRCLE, color, size, lifetime, 0.08F);
		layer(level, at.add(facing.normalize().scale(0.02)), facing, SigilOption.RING, 0xFFFFFF, size * 1.3F, lifetime, -0.12F);
	}
}
