package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * What the time runes draw that the shared clock ({@link ElementFx#clock}) cannot: faces that stand up and look at you,
 * a halo of pips for wards, a column of frozen sand, a run of gold after-images, a path lit in reverse, an hourglass and a
 * thread of sand. Sounds are the kit's ({@code tools/feel/time.py}). Per-tick budgets keep a crowd from flooding the wire.
 */
public final class TimeFx {
	private TimeFx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int GOLD = ElementFx.TIME.primary();
	private static final int PALE = ElementFx.TIME.secondary();
	private static final int BRONZE = ElementFx.TIME.accent();

	// ------------------------------------------------------------------ budgets

	private static final Map<String, long[]> BUDGET = new HashMap<>();

	/**
	 * Whether one more of {@code key} may be drawn in full this tick: the first {@code max} of a tick say yes, the rest no
	 * (draw a single puff instead). A Countdown on sixty-four mobs sends eight faces, not sixty-four.
	 */
	public static synchronized boolean allow(ServerLevel level, String key, int max) {
		long now = level.getGameTime();
		long[] slot = BUDGET.computeIfAbsent(key, k -> new long[] {-1, 0});
		if (slot[0] != now) {
			slot[0] = now;
			slot[1] = 0;
		}
		return ++slot[1] <= max;
	}

	// ------------------------------------------------------------------ faces

	/** A horizontal unit vector from {@code from} toward {@code viewer} (the face turns to whoever cast it), or {@code +z} if they coincide. */
	public static Vec3 toward(Vec3 from, Vec3 viewer) {
		Vec3 d = new Vec3(viewer.x - from.x, 0, viewer.z - from.z);
		return d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
	}

	/** A clock face standing upright, its face toward {@code facing}: gold rim, hands sweeping, the quarter hours ticked. */
	public static void face(ServerLevel level, Vec3 centre, Vec3 facing, double radius, int ticks, boolean backward) {
		ElementFx.clock(level, centre, flat(facing), radius, ticks, backward);
	}

	/** A stopped upright face, both hands still at {@code hand} radians. */
	public static void stoppedFace(ServerLevel level, Vec3 centre, Vec3 facing, double radius, double hand, int lifetime) {
		ElementFx.stoppedClock(level, centre, flat(facing), radius, hand, lifetime);
	}

	/**
	 * A face whose second hand sweeps once over {@code ticks} while a ring closes on it: the fuse of a Countdown. It stands upright
	 * over the head, turned toward {@code viewer}.
	 */
	public static void fuse(ServerLevel level, Vec3 head, Vec3 viewer, double radius, int ticks) {
		Vec3 f = toward(head, viewer);
		face(level, head, f, radius, ticks, false);
		ElementFx.ring(level, head.add(f.scale(0.02)), f, PALE, radius * 1.6, radius * 0.9, 0.025, ticks);
	}

	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	// ------------------------------------------------------------------ Foresight and Riposte: pips over the head

	/**
	 * A halo above the head with one pip per charge: a thin gold ring and a bright dot on each place. The pips are spread
	 * evenly, so two charges read as two and one as one.
	 */
	public static void halo(ServerLevel level, Entity t, int charges, int lifetime) {
		Vec3 head = t.position().add(0, t.getBbHeight() + 0.35, 0);
		double r = 0.32;
		ElementFx.ring(level, head, UP, GOLD, r, r, 0.02, lifetime);
		int pips = Math.max(1, Math.min(6, charges));
		double spin = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < pips; i++) {
			double a = spin + Math.PI * 2 * i / pips;
			Vec3 p = head.add(Math.cos(a) * r, 0.02, Math.sin(a) * r);
			Vfx.emit(level, SigilOption.glow(PALE, 0.55F), p, 1, 0.0, 0.0);
		}
		Vfx.emit(level, ParticleTypes.END_ROD, head, 2, 0.2, 0.01);
	}

	/** One pip spent: the halo blinks out one dot upward, and a gold flake falls. */
	public static void pipSpent(ServerLevel level, Entity t) {
		Vec3 head = t.position().add(0, t.getBbHeight() + 0.4, 0);
		Vfx.emit(level, SigilOption.glow(PALE, 0.9F), head, 1, 0.0, 0.0);
		Vfx.emit(level, ParticleTypes.WAX_ON, head, 3, 0.15, 0.0);
	}

	// ------------------------------------------------------------------ Time Skip: a flicker of gold frames

	/**
	 * Time Skip: three gold after-images where you were, blinking in and out a tick apart like frames dropped from a film, a
	 * stopped face in the last of them, and a clean gold ring where you land. Nothing crimson: that colour belongs to blood.
	 */
	public static void skip(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 dir = to.subtract(from);
		Vec3 heading = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
		int[] beats = {0, 1, 3};
		for (int i = 0; i < beats.length; i++) {
			int k = i;
			Scheduler.later(beats[i], () -> {
				Vec3 at = from.add(heading.scale(k * 0.35));
				frame(level, at, heading, 0.85F - k * 0.15F);
			});
		}
		Scheduler.later(4, () -> stoppedFace(level, from.add(0, 1.1, 0), heading, 0.6, 0.0, 12));
		Vfx.emit(level, SigilOption.glow(PALE, 2.0F), to.add(0, 1, 0), 1, 0.0, 0.0);
		ElementFx.ring(level, to.add(0, 1, 0), UP, GOLD, 0.2, 1.3, 0.04, 8);
		ElementFx.groundRing(level, to, PALE, 0.2, 1.2, 0.04, 10);
		ElementFx.goldenTicks(level, to.add(0, 1, 0), 0.4, 6);
	}

	/** One frame of a skip: a standing outline of gold dust. */
	private static void frame(ServerLevel level, Vec3 feet, Vec3 heading, float size) {
		Vec3 right = flat(heading).cross(UP).normalize();
		DustParticleOptions dust = new DustParticleOptions(GOLD, size);
		Vec3 head = feet.add(0, 1.65, 0);
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			Vfx.emit(level, dust, head.add(right.scale(Math.cos(a) * 0.16)).add(0, Math.sin(a) * 0.16, 0), 1, 0.0, 0.0);
		}
		for (int k = 0; k < 6; k++) {
			double y = 1.35 - k * 0.22;
			double w = k == 0 ? 0.28 : 0.16;
			Vfx.emit(level, dust, feet.add(0, y, 0).add(right.scale(w)), 1, 0.0, 0.0);
			Vfx.emit(level, dust, feet.add(0, y, 0).add(right.scale(-w)), 1, 0.0, 0.0);
		}
	}

	// ------------------------------------------------------------------ Rewind: the path lit in reverse

	/**
	 * Rewind: dots of gold light the way back from where you are to where you were, one after the other (so it reads as time
	 * running backward), then a backward face opens at the arrival.
	 */
	public static void rewindPath(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		int steps = (int) Math.max(4, Math.min(14, length * 1.2));
		for (int i = 0; i <= steps; i++) {
			double u = (double) i / steps;
			// A gentle wobble so it looks like a walked path, not a ruler.
			Vec3 p = from.add(d.scale(u)).add(0, 1.0 + Math.sin(u * 9) * 0.12, 0).add(Math.cos(u * 7) * 0.12 * (1 - u), 0, 0);
			Scheduler.later(1 + i / 2, () -> {
				Vfx.emit(level, SigilOption.glow(GOLD, 0.6F), p, 1, 0.0, 0.0);
				Vfx.emit(level, ParticleTypes.END_ROD, p, 1, 0.05, 0.0);
			});
		}
		Scheduler.later(2 + steps / 2, () -> {
			face(level, to.add(0, 1.2, 0), toward(to, from), 0.9, 10, true);
			ElementFx.groundRing(level, to, GOLD, 1.4, 0.2, 0.05, 10);
			ElementFx.goldenTicks(level, to.add(0, 1, 0), 0.4, 6);
		});
	}

	// ------------------------------------------------------------------ Stasis: frozen sand and hanging shards

	/**
	 * A column of frozen sand round the target: pale shards standing on a ring, one still face at its heart, all of it lasting
	 * the whole hold (so nothing needs redrawing every few ticks).
	 */
	public static void stasisColumn(ServerLevel level, Entity t, int holdTicks) {
		Vec3 feet = t.position();
		double r = Math.max(0.55, t.getBbWidth() * 0.75);
		double h = Math.max(1.0, t.getBbHeight());
		int life = Math.max(10, Math.min(holdTicks, 120));
		int shards = 8;
		double spin = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < shards; i++) {
			double a = spin + Math.PI * 2 * i / shards;
			double len = h * (0.45 + 0.35 * ((i * 5) % 3) / 2.0);
			Vec3 base = feet.add(Math.cos(a) * r, 0.05 + (i % 2) * 0.1, Math.sin(a) * r);
			ElementFx.ray(level, base, base.add(0, len, 0), i % 2 == 0 ? PALE : GOLD, 0.045, life);
		}
		ElementFx.groundRing(level, feet, PALE, r, r, 0.05, life);
		stoppedFace(level, t.getBoundingBox().getCenter(), toward(t.position(), t.position().add(t.getLookAngle())), Math.max(0.6, r * 0.9), 0.0, life);
		Vfx.emit(level, new DustParticleOptions(0xD8D0B8, 0.9F), t.getBoundingBox().getCenter(), 6, r, 0.0);
	}

	/** A blow held in the column: one shard hangs where it struck. */
	public static void stasisShard(ServerLevel level, Entity t, int holdTicks) {
		var rnd = level.getRandom();
		Vec3 c = t.getBoundingBox().getCenter();
		Vec3 at = c.add((rnd.nextDouble() - 0.5) * t.getBbWidth(), (rnd.nextDouble() - 0.5) * t.getBbHeight() * 0.8, (rnd.nextDouble() - 0.5) * t.getBbWidth());
		double a = rnd.nextDouble() * Math.PI * 2;
		Vec3 dir = new Vec3(Math.cos(a), (rnd.nextDouble() - 0.5) * 1.4, Math.sin(a)).normalize().scale(0.3);
		ElementFx.ray(level, at.subtract(dir), at.add(dir), 0xFFFFFF, 0.03, Math.min(holdTicks, 60));
	}

	/** Time moves again: the column shatters outward and the held blows arrive as one flash. */
	public static void stasisRelease(ServerLevel level, Entity t, float stored) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vfx.emit(level, SigilOption.glow(0xFFFFFF, 2.2F), c, 1, 0.0, 0.0);
		ElementFx.ring(level, c, UP, PALE, 0.3, 2.4, 0.06, 7);
		ElementFx.groundRing(level, t.position(), GOLD, 0.4, 2.0, 0.08, 8);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS), c, (int) Math.min(18, 6 + stored), 0.35);
		Vfx.radial(level, ParticleTypes.END_ROD, c, 10, 0.3);
		Feels.sound(level, c, "time_resume", stored > 0 ? 1.0F : 0.7F, 1.0F);
	}

	// ------------------------------------------------------------------ sand: Prolong and Timesteal

	/** An hourglass of light: two cones of rings, and a thread of sand falling through the waist. */
	public static void hourglass(ServerLevel level, Vec3 base, double height, int color, int lifetime) {
		int rings = 5;
		for (int i = 0; i < rings; i++) {
			double u = (double) i / (rings - 1);
			double y = height * u;
			// The profile pinches at the middle: wide at both ends.
			double r = 0.12 + 0.4 * Math.abs(u - 0.5) * 2;
			ElementFx.ring(level, base.add(0, y, 0), UP, i == rings / 2 ? PALE : color, r, r, 0.02, lifetime);
		}
		ElementFx.ray(level, base.add(0, height * 0.5, 0), base.add(0, height * 0.05, 0), color, 0.025, lifetime);
		Vfx.emit(level, new DustParticleOptions(color, 0.6F), base.add(0, height * 0.3, 0), 4, 0.04, 0.0);
	}

	/**
	 * A thread of sand from {@code from} to {@code to}: grains of {@code color} laid out along a low arc, one after the other,
	 * so it reads as poured from one glass into another.
	 */
	public static void sandThread(ServerLevel level, Vec3 from, Vec3 to, int color) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		int steps = (int) Math.max(4, Math.min(16, length * 2));
		for (int i = 0; i <= steps; i++) {
			double u = (double) i / steps;
			Vec3 p = from.add(d.scale(u)).add(0, Math.sin(u * Math.PI) * Math.min(0.9, length * 0.15), 0);
			Scheduler.later(i / 2, () -> {
				Vfx.emit(level, new DustParticleOptions(color, 0.7F), p, 1, 0.02, 0.0);
				Vfx.emit(level, ParticleTypes.WAX_ON, p, 1, 0.03, 0.0);
			});
		}
		Scheduler.later(steps / 2 + 1, () -> Vfx.emit(level, SigilOption.glow(color, 1.0F), to, 1, 0.0, 0.0));
	}

	// ------------------------------------------------------------------ Borrowed Time: a coin of gold

	/** A payment on a borrowed moment: one gold coin falls from you and a soft tock. */
	public static void coin(ServerLevel level, Entity t) {
		Vec3 at = t.getBoundingBox().getCenter().add(0, 0.2, 0);
		Vfx.fling(level, new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET), at, new Vec3(0, -0.4, 0), 0.05);
		Vfx.emit(level, ParticleTypes.WAX_ON, at, 2, 0.2, 0.0);
		Feels.sound(level, at, "time_tock", 0.7F, 1.0F);
	}

	// ------------------------------------------------------------------ end cues

	/** The end of something timed: a ring closes on {@code at} and a soft chime, so the player knows it is over. */
	public static void ending(ServerLevel level, Vec3 at, int color, String sound, float volume) {
		ElementFx.ring(level, at, UP, color, 0.9, 0.2, 0.03, 8);
		Vfx.emit(level, ParticleTypes.END_ROD, at, 3, 0.25, 0.01);
		if (sound != null) {
			Feels.sound(level, at, sound, volume, 1.0F);
		}
	}

	/** Schedules {@link #ending} at the waist of a creature {@code ticks} from now, if it is still there then. */
	public static void endingLater(ServerLevel level, Entity t, int ticks, int color, String sound, float volume) {
		Scheduler.later(Math.max(1, ticks), () -> {
			if (t.isAlive() && !t.isRemoved()) {
				ending(level, t.getBoundingBox().getCenter(), color, sound, volume);
			}
		});
	}

	public static int gold() {
		return GOLD;
	}

	public static int bronze() {
		return BRONZE;
	}

	/** A trail of gold from {@code a} to {@code b}, for wards that carry something across (a dodge, a stolen effect). */
	public static void trail(ServerLevel level, Vec3 a, Vec3 b, int color, int ticks) {
		Fx.send(level, new TrailParticleOption(b, color, ticks), a.x, a.y, a.z, 2, 0.1, 0.1, 0.1, 0);
	}
}
