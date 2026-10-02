package dev.wildercord.aura.arts;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraVfx;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * An art's shaped light (the mod's {@code cast.Light}: blade arcs, crescents, glints and method accents, with physical ground scars),
 * sent one of two ways:
 * <ul>
 * <li>{@link #world}: to everyone, the swordsman too (a shape that would open right in front of anyone's eyes is left out for
 * them, as all shaped light is). For what lands out in the world: a fracture where a blow falls, a line of fire on the ground ahead,
 * lightning striking a foe.</li>
 * <li>{@link #spectacle}: to everyone else, and to the swordsman only while they watch themselves in third person
 * ({@link AuraFx#spectacle}). For the big shapes about the body: a whirlwind round them, a lance down their line of sight, a sun
 * over their head. Through their own eyes those would fill the view; what they see instead is the art's thin trail, its impacts
 * and whatever lands ahead of them.</li>
 * </ul>
 * By day, under the open sky, added light washes out: {@link #day} says when to lay a rim of the mod's darkness under a shape
 * ({@code color | DARK}), as the slash's crescent does.
 */
public final class ArtLight {
	/** Or'd into a colour: drawn as darkness (a rim under light by day). */
	public static final int DARK = ElementFx.DARK;

	private final ServerPlayer owner;
	private final boolean spectacle;
	private final boolean rim;

	private ArtLight(ServerPlayer owner, boolean spectacle, boolean rim) {
		this.owner = owner;
		this.spectacle = spectacle;
		this.rim = rim;
	}

	/**
	 * Shapes for everyone, the swordsman too. By day under the open sky each ring, beam and crescent gets a rim of darkness a
	 * little wider than its light under it (added light alone washes out against a bright sky or pale stone); in the dark, none.
	 */
	public static ArtLight world(ServerPlayer owner) {
		return new ArtLight(owner, false, true);
	}

	/** Shapes for everyone else, and the swordsman only in third person (rimmed by day, as {@link #world}). */
	public static ArtLight spectacle(ServerPlayer owner) {
		return new ArtLight(owner, true, true);
	}

	/** The same, without the daytime rim (a shape that is darkness itself, or one laid over another's rim). */
	public ArtLight bare() {
		return new ArtLight(owner, spectacle, false);
	}

	private boolean rimAt(Vec3 at) {
		return rim && day(owner.level(), at);
	}

	/** Whether it's bright behind {@code at} (day, under the open sky): lay a rim of darkness under light there. */
	public static boolean day(ServerLevel level, Vec3 at) {
		return AuraVfx.brightBehind(level, at);
	}

	public ServerLevel level() {
		return owner.level();
	}

	private void send(ParticleOptions option, Vec3 at) {
		if (spectacle) {
			AuraFx.spectacle(owner, option, at);
		} else {
			Sigils.send(owner.level(), option, at);
		}
	}

	/** A shockwave racing out from {@code from} to {@code to} blocks in the plane facing {@code normal}. */
	public ArtLight ring(Vec3 at, Vec3 normal, int color, double from, double to, double width, int life) {
		if (Math.abs(normal.y) > 0.95 && at.y < owner.position().y + 0.4) {
			AuraFx.groundScar(owner.level(), at, Math.max(from, to), Math.max(40, life * 3), 0);
			return this;
		}
		return slash(at, normal, ElementFx.perp(normal), color, Math.max(from, to), 1.8, width, 2, life);
	}

	/** A ragged impact depression with fragments of the actual floor. */
	public ArtLight groundRing(Vec3 at, int color, double from, double to, double width, int life) {
		AuraFx.groundScar(owner.level(), at, Math.max(from, to), Math.max(60, life * 3), 1);
		return this;
	}

	/** A beam from {@code from} to {@code to}. */
	public ArtLight ray(Vec3 from, Vec3 to, int color, double width, int life) {
		Vec3 d = to.subtract(from);
		if (d.lengthSqr() < 1.0E-6) {
			return this;
		}
		if ((color & DARK) == 0 && rimAt(from.add(d.scale(0.5)))) {
			send(new LightOption(LightOption.RAY, color | DARK, (float) d.x, (float) d.y, (float) d.z, (float) Math.min(width * 1.1, width + 0.06), 0, 0, 0, life), from);
		}
		send(new LightOption(LightOption.RAY, color, (float) d.x, (float) d.y, (float) d.z, (float) width, 0, 0, 0, life), from);
		return this;
	}

	/** A crescent round {@code centre} in the plane facing {@code normal}, its middle toward {@code toward}, sweeping in {@code sweep} ticks. */
	public ArtLight slash(Vec3 centre, Vec3 normal, Vec3 toward, int color, double radius, double span, double width, int sweep, int life) {
		if ((color & DARK) == 0 && rimAt(centre)) {
			send(ElementFx.slashOption(normal, toward, color | DARK, radius * 1.02, span * 0.98, width * 1.15, sweep, life + 1), centre);
		}
		send(ElementFx.slashOption(normal, toward, color, radius, span, width, sweep, life), centre);
		return this;
	}

	// ------------------------------------------------------------------ the elements' motifs, rimmed by day

	/**
	 * Flame tongues licking up round {@code base} (as {@code ElementFx.tongues}, but through this light, so they carry a rim by day):
	 * short crescents on the sides of a column, curling up and over, in two colours.
	 */
	public ArtLight tongues(Vec3 base, double radius, double height, int count, int color, int second, int life) {
		net.minecraft.util.RandomSource r = owner.level().getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = phase + Math.PI * 2 * i / count + (r.nextDouble() - 0.5) * 0.6;
			Vec3 centre = base.add(0, height * (0.15 + 0.7 * r.nextDouble()), 0);
			Vec3 toward = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.8).add(0, 0.6, 0);
			slash(centre, new Vec3(-Math.sin(a), 0, Math.cos(a)), toward, i % 2 == 0 ? color : second, radius + 0.12, 1.2 + r.nextDouble() * 0.4,
				0.14 + r.nextDouble() * 0.07, 2 + i % 2, life);
		}
		return this;
	}

	/** A burst of crescents whirling round {@code centre} on every tilt (fire's, frost's or wind's impact, in its colours). */
	public ArtLight whirl(Vec3 centre, double radius, int count, int a, int b, int c) {
		net.minecraft.util.RandomSource r = owner.level().getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 normal = ElementFx.randomDir(r);
			int color = i % 3 == 0 ? a : i % 3 == 1 ? b : c;
			slash(centre, normal, ElementFx.inPlane(normal, r.nextDouble() * Math.PI * 2), color, radius * (0.7 + 0.3 * r.nextDouble()),
				1.6 + r.nextDouble() * 0.6, 0.1 + radius * 0.07, 2, 8);
		}
		return this;
	}

	/** Crescents swirling up an axis from {@code base}, each on its own tilt (wind's motif). */
	public ArtLight swirl(Vec3 base, double radius, double height, int arcs, int color, int second) {
		double phase = owner.level().getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < arcs; i++) {
			double k = arcs == 1 ? 0 : i / (double) (arcs - 1);
			double a = phase + i * 2.1;
			slash(base.add(0, height * k, 0), ElementFx.tilted(0.25, a + Math.PI / 2), ElementFx.flatDir(a), i % 2 == 0 ? color : second,
				radius * (1 - 0.25 * k), 2.3, 0.08, 1 + i, 7 + i);
		}
		return this;
	}

	/** Shards of light bursting out of {@code at} (frost's motif): short spikes, up to {@code reach} long. */
	public ArtLight shards(Vec3 at, double reach, int count, int color, int second) {
		net.minecraft.util.RandomSource r = owner.level().getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.25, 0).normalize();
			double length = reach * (0.55 + 0.45 * r.nextDouble());
			ray(at.add(dir.scale(0.12)), at.add(dir.scale(length)), i % 2 == 0 ? color : second, 0.045 + 0.02 * r.nextDouble(), 7 + r.nextInt(3));
		}
		return this;
	}

	/** A glowing orb wrapped in turning rings (four ticks or less, redrawn as it moves). */
	public ArtLight orb(Vec3 at, int color, double radius, int life) {
		send(new LightOption(LightOption.ORB, color, (float) radius, 0, 0, 0.02F, 0, 0, 0, life), at);
		return this;
	}

	/** A crackling arc of lightning, a fresh jagged path each tick (see {@code ElementFx.arc}). */
	public ArtLight arc(Vec3 from, Vec3 to, int color, double width, int forks, boolean flat, int life) {
		Vec3 d = to.subtract(from);
		if ((color & DARK) == 0 && rimAt(from.add(d.scale(0.5)))) {
			send(new LightOption(LightOption.ARC, color | DARK, (float) d.x, (float) d.y, (float) d.z, (float) (width * 1.6), forks, flat ? 1 : 0, 0,
				life), from);
		}
		send(new LightOption(LightOption.ARC, color, (float) d.x, (float) d.y, (float) d.z, (float) width, forks, flat ? 1 : 0, 0, life), from);
		return this;
	}

	/** An aura pressure mark or glint ({@link SigilOption} style) facing {@code normal}. */
	public ArtLight sigil(Vec3 at, Vec3 normal, int style, int color, double size, int life, double spin) {
		// Aura uses pressure edges; rune-bearing seals belong to spell casting.
		if (style == SigilOption.CIRCLE || style == SigilOption.RING) style = SigilOption.BAND;
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? ArtKit.UP : normal.normalize();
		if (style == SigilOption.STAR) {
			Vec3 u = n.cross(new Vec3(0, 0, 1));
			u = u.lengthSqr() < 1.0E-5 ? new Vec3(1, 0, 0) : u.normalize();
			Vec3 v = n.cross(u).normalize();
			ray(at.subtract(u.scale(size)), at.add(u.scale(size)), color, 0.04, life);
			ray(at.subtract(v.scale(size)), at.add(v.scale(size)), color, 0.04, life);
			return this;
		}
		return slash(at, n, ElementFx.inPlane(n, spin), color, size, 1.4, 0.035, 2, life);
	}

	/** A physical fracture, impact depression or blade gouge projected onto solid ground. */
	public ArtLight ground(Vec3 feet, int style, int color, double size, int life, double spin) {
		AuraFx.groundScar(owner.level(), feet, size, life, style == SigilOption.STAR ? 2 : style == SigilOption.CRACKED ? 0 : 1);
		return this;
	}

	/** A flash of light {@code size} blocks across, facing whoever sees it. */
	public ArtLight flash(Vec3 at, int color, float size) {
		send(SigilOption.glow(0xFF000000 | color, size), at);
		return this;
	}

	/**
	 * A soft disc of darkness {@code size} blocks across, facing whoever sees it (the flash's opposite: light taken away). Laid under an
	 * orb, it gives a black sphere a smooth edge.
	 */
	public ArtLight shade(Vec3 at, int color, float size) {
		send(SigilOption.glow((color & 0xFFFFFF) | DARK, size), at);
		return this;
	}
}
