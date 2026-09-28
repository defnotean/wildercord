package dev.wildercord.cast;

import dev.wildercord.content.MoteOption;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * How the fused effects of storm and wind look ({@link FusedStorm}). Like every fused effect, each draws on both of
 * its elements' visual languages ({@link ElementFx}):
 * <ul>
 *   <li><b>Riftbolt</b>: storm's lightning drawn in void's darkness, a black bolt with a violet heart, and at either
 *       end a tear in the air: a jagged crack of darkness the height of what goes through, its edges crackling.</li>
 *   <li><b>Stormweave</b>: arcane star seals over each marked enemy and faint threads of light between them, then
 *       lightning in arcane pink racing along every thread at once: a glowing web.</li>
 *   <li><b>Stormclock</b>: a clock face of gold light hung in the sky, its hand a spark of lightning ticking round;
 *       as it comes to the hour the ground below is marked, and golden lightning falls with the toll of a bell.</li>
 *   <li><b>Heartstopper</b>: crimson lightning into the chest, blood's heartbeat rings, and each skipped beat a
 *       single ring, a ring of lightning clenching in and a moment of darkness.</li>
 *   <li><b>Thunderhead</b>: a real dark cloud heaped over the target, rain falling from it and lightning flickering
 *       inside it, that throws down a bolt every second.</li>
 *   <li><b>Downdraft</b>: wind spiralling down out of the sky into dust, the ground cracking where each flyer is
 *       driven into it.</li>
 *   <li><b>Updraft</b>: a column of wind, rings rising up it and crescents winding round it, and then a ring of air
 *       crashing down.</li>
 *   <li><b>Skyglyph</b>: a turning wind sigil on the ground, arcane star within a ring of wind, a pinwheel of
 *       crescents going round over it.</li>
 *   <li><b>Recoil</b>: a gust, and where the target stood a stopped clock holding its place, joined to it by a thread
 *       of gold until the wind winds it back.</li>
 * </ul>
 */
final class FusedStormVfx {
	private FusedStormVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;
	/** Storm clouds: slate at the heart, a paler grey on top. */
	private static final int CLOUD_DARK = 0x33353F;
	private static final int CLOUD_GREY = 0x5A5E6C;
	private static final int SCORCH = 0x2A2418;

	private static ElementFx.Palette storm() {
		return ElementFx.STORM;
	}

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	// ------------------------------------------------------------------ Riftbolt

	/** Riftbolt: black lightning with a violet heart, a pale crackle riding it, and darkness blooming where it lands. */
	static void riftbolt(ServerLevel level, Vec3 origin, Entity t, boolean full) {
		Vec3 c = centre(t);
		Vec3 start = origin.distanceToSqr(c) > 64 ? c.add(c.subtract(origin).normalize().scale(-6)) : origin;
		ElementFx.bolt(level, start, c, full ? 0.075 : 0.05, full ? 2 : 1, full ? 3 : 2, ElementFx.VOID.secondary(), ElementFx.dark(ElementFx.VOID.accent()));
		if (full) {
			ElementFx.arc(level, start, c, storm().accent(), 0.022, 1, false, 5);
		}
		Sigils.flash(level, c, ElementFx.dark(ElementFx.VOID.accent()), 2.2F);
		Sigils.flash(level, c, ElementFx.VOID.primary(), 0.9F);
		ElementFx.sparks(level, c, 8, 0.3);
		Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 0.6F);
		Fx.sound(level, c, WildercordSounds.impact("void"), 0.6F, 1.0F);
	}

	/**
	 * A tear in the air at {@code feet}: a jagged crack of darkness a little taller than what goes through it, zigzagging
	 * across the way it travels, violet at its heart and widest in the middle, its edges crackling with lightning. The
	 * entry tear falls in on itself; the exit spits out a ring and a spray of light.
	 */
	static void tear(ServerLevel level, Vec3 feet, double height, Vec3 dir, boolean entry, boolean full) {
		Vec3 along = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
		Vec3 across = ElementFx.perp(along);
		double h = Math.max(1.2, height * 1.25);
		Vec3 bottom = feet.add(0, -0.1, 0);
		RandomSource r = level.getRandom();
		int segments = full ? 7 : 4;
		int life = entry ? 14 : 20;
		Vec3 last = bottom;
		for (int i = 1; i <= segments; i++) {
			double k = i / (double) segments;
			double swell = Math.sin(Math.PI * k);
			double side = (i % 2 == 0 ? 1 : -1) * (0.1 + 0.16 * r.nextDouble()) * swell;
			Vec3 p = bottom.add(0, h * k, 0).add(across.scale(side)).add(along.scale((r.nextDouble() - 0.5) * 0.12));
			ElementFx.ray(level, last, p, ElementFx.dark(ElementFx.VOID.accent()), 0.12 + 0.14 * swell, life + 3);
			ElementFx.ray(level, last, p, ElementFx.VOID.primary(), 0.035, life);
			last = p;
		}
		Vec3 mid = feet.add(0, h * 0.5, 0);
		Sigils.flash(level, mid, ElementFx.dark(ElementFx.VOID.accent()), (float) (h * 1.2));
		if (full) {
			for (int s = -1; s <= 1; s += 2) {
				ElementFx.arc(level, bottom.add(across.scale(s * 0.2)), bottom.add(0, h, 0).add(across.scale(s * 0.2)), storm().secondary(), 0.02, 1, false,
					life - 3);
			}
		}
		if (entry) {
			ElementFx.implode(level, mid, 1.2, 10);
			Fx.sound(level, mid, SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.6F);
		} else {
			ElementFx.ring(level, mid, along, ElementFx.VOID.secondary(), 0.15, 1.4, 0.04, 9);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, mid, 12, 0.12);
			ElementFx.sparks(level, mid, 6, 0.25);
			Fx.sound(level, mid, WildercordSounds.BLINK, 0.9F, 1.0F);
		}
	}

	/** A Riftbolt that struck nothing: a small tear flickers open where it landed and closes. */
	static void riftFizzle(ServerLevel level, Vec3 point) {
		tear(level, point.add(0, -0.6, 0), 1.0, new Vec3(1, 0, 0), true, false);
	}

	// ------------------------------------------------------------------ Stormweave

	/**
	 * The marks: a small star seal turning over each head, a noose of light tightening round each body, and faint
	 * threads strung between every two of them: the pattern the lightning will follow.
	 */
	static void weaveMarks(ServerLevel level, List<? extends Entity> web, int ticks) {
		List<Vec3> nodes = new ArrayList<>();
		for (Entity t : web) {
			Vec3 over = t.position().add(0, t.getBbHeight() + 0.45, 0);
			ElementFx.sigil(level, over, UP, SigilOption.STAR, ElementFx.ARCANE.primary(), 0.38, ticks + 6, 0.18);
			ElementFx.ring(level, centre(t), UP, ElementFx.ARCANE.accent(), Math.max(0.6, t.getBbWidth()) + 0.5, 0.15, 0.035, ticks);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, over, 3, 0.15, 0.02);
			nodes.add(centre(t));
		}
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				ElementFx.ray(level, nodes.get(i), nodes.get(j), ElementFx.ARCANE.secondary(), 0.018, ticks + 3);
			}
		}
		Vec3 first = nodes.getFirst();
		Fx.sound(level, first, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
		Fx.sound(level, first, WildercordSounds.cast("arcane"), 0.5F, 1.0F);
	}

	/**
	 * The web: pink lightning with a white-hot core along every thread at once, crackling on after the strike, a star
	 * seal flaring at every knot. A web of one is struck from above.
	 */
	static void weave(ServerLevel level, List<? extends Entity> caught) {
		List<Vec3> nodes = new ArrayList<>();
		for (Entity t : caught) {
			nodes.add(centre(t));
		}
		if (nodes.size() == 1) {
			Vec3 n = nodes.getFirst();
			ElementFx.bolt(level, n.add(0, 4.5, 0), n, 0.07, 2, 2, storm().secondary(), ElementFx.ARCANE.primary());
			ElementFx.arc(level, n.add(0, 4.5, 0), n, ElementFx.ARCANE.accent(), 0.03, 1, false, 10);
		}
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				Vec3 a = nodes.get(i);
				Vec3 b = nodes.get(j);
				ElementFx.bolt(level, a, b, 0.055, 1, 1, storm().secondary(), ElementFx.ARCANE.primary());
				ElementFx.arc(level, a, b, ElementFx.ARCANE.accent(), 0.03, 1, false, 12);
			}
		}
		Vec3 mid = Vec3.ZERO;
		for (Vec3 n : nodes) {
			Sigils.flash(level, n, ElementFx.ARCANE.primary(), 1.6F);
			ElementFx.starSeal(level, n, UP, 0.55, 10);
			ElementFx.sparks(level, n, 6, 0.3);
			mid = mid.add(n.scale(1.0 / nodes.size()));
		}
		Fx.sound(level, mid, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9F, 1.3F);
		Fx.sound(level, mid, WildercordSounds.impact("arcane"), 0.8F, 1.0F);
	}

	/** A Stormweave with no one to catch: a star seal sparks and goes out. */
	static void weaveFizzle(ServerLevel level, Vec3 point) {
		ElementFx.starSeal(level, point, UP, 0.4, 8);
		ElementFx.sparks(level, point, 5, 0.2);
		Fx.sound(level, point, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 0.8F);
	}

	// ------------------------------------------------------------------ Stormclock

	/** How long each half of the clock face lasts: it's written again at the hour, and the two overlap a little. */
	private static final int FACE_LIFE = FusedStormNumbers.CLOCK_STRIKES[0] + 12;

	/** The clock opens in the sky over the spot, and the first bolt falls from it. */
	static void clockOpen(ServerLevel level, Vec3 face, Vec3 normal, Vec3 spot, double size) {
		face(level, face, normal, size);
		ElementFx.ring(level, face, normal, ElementFx.TIME.secondary(), 0.2, size * 1.2, 0.05, 9);
		ElementFx.goldenTicks(level, face, size * 0.4, 6);
		fall(level, face, spot, size);
		Fx.sound(level, face, WildercordSounds.impact("time"), 0.8F, 1.0F);
	}

	/** The face: a gold rim and a storm-yellow one inside it, the twelve hours marked in light (the quarters longer and brighter). */
	private static void face(ServerLevel level, Vec3 centre, Vec3 normal, double size) {
		ElementFx.sigil(level, centre, normal, SigilOption.BAND, ElementFx.TIME.primary(), size, FACE_LIFE, 0);
		ElementFx.sigil(level, centre.add(normal.scale(0.01)), normal, SigilOption.BAND, storm().primary(), size * 0.84, FACE_LIFE, 0);
		for (int hour = 0; hour < 12; hour++) {
			Vec3 dir = ElementFx.inPlane(normal, Math.PI * 2 * hour / 12);
			boolean quarter = hour % 3 == 0;
			ElementFx.ray(level, centre.add(dir.scale(size * (quarter ? 0.66 : 0.76))), centre.add(dir.scale(size * 0.93)),
				quarter ? ElementFx.TIME.secondary() : ElementFx.TIME.primary(), quarter ? 0.05 : 0.028, FACE_LIFE);
		}
	}

	/**
	 * The hand moves on a step: a gold hand with a spark of lightning running along it, and a short sweep behind it.
	 * On the last step before the hour the ground below is marked where it will strike and the face flares.
	 */
	static void clockTick(ServerLevel level, Vec3 face, Vec3 normal, Vec3 spot, double size, int hand, int steps) {
		double a = Math.PI * 2 * hand / steps;
		Vec3 dir = ElementFx.inPlane(normal, a);
		Vec3 tip = face.add(dir.scale(size * 0.82));
		ElementFx.ray(level, face, tip, ElementFx.TIME.secondary(), 0.05, 6);
		ElementFx.arc(level, face, tip, storm().primary(), 0.025, 0, false, 5);
		ElementFx.slash(level, face, normal, ElementFx.inPlane(normal, a - Math.PI / steps), ElementFx.TIME.accent(), size * 0.6, Math.PI * 2 / steps, 0.06,
			2, 6);
		boolean warn = hand == steps - 1;
		if (warn) {
			Sigils.target(level, spot, storm().primary(), (float) size, 8);
			ElementFx.ring(level, face, normal, storm().secondary(), size * 0.3, size * 1.05, 0.05, 6);
			ElementFx.sparks(level, face, 6, 0.15);
		}
		Fx.sound(level, face, SoundEvents.COMPARATOR_CLICK, warn ? 0.9F : 0.5F, 1.2F + 0.1F * hand);
	}

	/** The hour strikes: golden lightning falls on the spot, and the face is written again, or at the last hour breaks apart. */
	static void clockStrike(ServerLevel level, Vec3 face, Vec3 normal, Vec3 spot, double size, boolean last) {
		fall(level, face, spot, size);
		Sigils.flash(level, face, storm().secondary(), (float) (size * 1.6));
		if (!last) {
			face(level, face, normal, size);
			return;
		}
		// The last hour: the face breaks into its hours and golden flecks rain down.
		for (int hour = 0; hour < 12; hour++) {
			Vec3 dir = ElementFx.inPlane(normal, Math.PI * 2 * hour / 12 + 0.26);
			ElementFx.ray(level, face.add(dir.scale(size * 0.9)), face.add(dir.scale(size * 1.5)), ElementFx.TIME.secondary(), 0.04, 7);
		}
		ElementFx.ring(level, face, normal, ElementFx.TIME.primary(), size, size * 1.7, 0.05, 10);
		ElementFx.goldenTicks(level, face, size * 0.6, 10);
		Fx.sound(level, face, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.3F);
	}

	/** One bolt out of the clock: storm-white lightning in a gold sheath, the impact, a small clock stamped on the ground, and a bell's toll. */
	private static void fall(ServerLevel level, Vec3 face, Vec3 spot, double size) {
		Vec3 ground = spot.add(0, 0.05, 0);
		ElementFx.bolt(level, face, ground, 0.1, 2, 2, storm().secondary(), ElementFx.TIME.accent());
		ElementFx.stormImpact(level, spot.add(0, 0.4, 0), 0.9);
		ElementFx.clock(level, spot.add(0, 0.07, 0), UP, size * 0.75, 8, false);
		ElementFx.groundRing(level, spot, ElementFx.TIME.primary(), 0.2, size, 0.06, 9);
		ElementFx.goldenTicks(level, spot.add(0, 0.3, 0), 0.4, 5);
		Vfx.emit(level, new DustParticleOptions(SCORCH, 1.6F), spot.add(0, 0.08, 0), 5, 0.4, 0.0);
		Fx.sound(level, spot, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9F, 1.1F);
		Fx.sound(level, spot, SoundEvents.BELL_BLOCK, 0.8F, 1.0F);
		Fx.sound(level, face, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.45F, 1.5F);
	}

	// ------------------------------------------------------------------ Heartstopper

	private static Vec3 chest(Entity t) {
		return centre(t).add(0, t.getBbHeight() * 0.15, 0);
	}

	/** Heartstopper: crimson lightning driven into the chest, the heart pounding once, hard, and blood thrown off. */
	static void heartstopper(ServerLevel level, Vec3 origin, Entity t) {
		Vec3 heart = chest(t);
		Vec3 start = origin.distanceToSqr(heart) > 64 ? heart.add(heart.subtract(origin).normalize().scale(-6)) : origin;
		ElementFx.bolt(level, start.add(0, 0.8, 0), heart, 0.07, 2, 3, ElementFx.BLOOD.secondary(), ElementFx.BLOOD.accent());
		Sigils.flash(level, heart, ElementFx.BLOOD.primary(), 1.7F);
		ElementFx.pulse(level, heart, UP, Math.max(0.9, t.getBbWidth() + 0.5));
		ElementFx.pulse(level, heart, heart.subtract(start), 0.8);
		ElementFx.sparks(level, heart, 8, 0.3);
		ElementFx.drip(level, heart, 0.25, 5);
		Fx.sound(level, heart, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7F, 1.5F);
		Fx.sound(level, heart, SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1.0F);
	}

	/** A beat between skips: one soft heartbeat ring. */
	static void heartbeat(ServerLevel level, Entity t) {
		Vec3 heart = chest(t);
		ElementFx.ring(level, heart, UP, ElementFx.BLOOD.primary(), 0.1, Math.max(0.6, t.getBbWidth()) + 0.2, 0.035, 7);
		Fx.sound(level, heart, SoundEvents.WARDEN_HEARTBEAT, 0.35F, 1.1F);
	}

	/**
	 * A skipped beat: one crimson ring where two should be, a ring of lightning clenching in on the heart from two
	 * sides, lightning crawling over the body, and for a moment everything round it goes dark.
	 */
	static void heartSkip(ServerLevel level, Entity t) {
		Vec3 heart = chest(t);
		double w = Math.max(0.6, t.getBbWidth());
		RandomSource r = level.getRandom();
		ElementFx.ring(level, heart, UP, ElementFx.BLOOD.primary(), 0.15, w + 0.8, 0.06, 8);
		double tilt = r.nextDouble() * Math.PI * 2;
		ElementFx.ring(level, heart, ElementFx.tilted(1.1, tilt), storm().primary(), w + 0.7, 0.05, 0.04, 6);
		ElementFx.ring(level, heart, ElementFx.tilted(1.1, tilt + Math.PI / 2), ElementFx.BLOOD.secondary(), w + 0.6, 0.05, 0.04, 7);
		Sigils.flash(level, heart, ElementFx.dark(ElementFx.BLOOD.accent()), 2.0F);
		for (int i = 0; i < 3; i++) {
			Vec3 a = t.position().add((r.nextDouble() - 0.5) * w, r.nextDouble() * t.getBbHeight(), (r.nextDouble() - 0.5) * w);
			Vec3 b = t.position().add((r.nextDouble() - 0.5) * w, r.nextDouble() * t.getBbHeight(), (r.nextDouble() - 0.5) * w);
			ElementFx.arc(level, a, b, i == 0 ? storm().primary() : ElementFx.BLOOD.secondary(), 0.025, 1, false, 5);
		}
		ElementFx.drip(level, heart, 0.2, 3);
		ScreenFx.shake(level, t.position(), 0.35F, 2.5);
		Fx.sound(level, heart, SoundEvents.WARDEN_HEARTBEAT, 1.1F, 1.3F);
		Fx.sound(level, heart, SoundEvents.TRIDENT_THUNDER.value(), 0.25F, 1.9F);
	}

	// ------------------------------------------------------------------ Thunderhead

	/** Billows of storm cloud, heaped wider than they are tall: {@code count} scattered round {@code at}. */
	private static void billows(ServerLevel level, Vec3 at, int count, int color, double size, int lifetime, Vec3 drift, double thickness) {
		Fx.send(level, new MoteOption(MoteOption.CLOUD, color, (float) size, lifetime, (float) drift.x, (float) drift.y, (float) drift.z, (float) thickness),
			at.x, at.y, at.z, count, 0.8, 0.22, 0.8, 0.006);
	}

	/** The cloud gathers: billows drawn in from all round, and a far-off rumble. */
	static void cloudGather(ServerLevel level, Vec3 centre) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6 + r.nextDouble() * 0.4;
			Vec3 from = centre.add(Math.cos(a) * 2.6, (r.nextDouble() - 0.3) * 0.6, Math.sin(a) * 2.6);
			Motes.clouds(level, from, 1, 0.2, CLOUD_GREY, 1.7, 30, centre.subtract(from).scale(0.045), 0.004, 0.7);
		}
		billows(level, centre, 3, CLOUD_DARK, 1.6, 30, Vec3.ZERO, 0.75);
		Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35F, 0.5F);
	}

	/**
	 * The cloud itself, sent every few ticks: dark billows heaped round {@code centre}, a paler crown, rain falling out
	 * of its underside, and now and then lightning flickering inside it.
	 */
	static void cloud(ServerLevel level, Vec3 centre, int age) {
		billows(level, centre, 3, CLOUD_DARK, 2.1, 26, new Vec3(0, 0.002, 0), 0.85);
		billows(level, centre.add(0, 0.45, 0), 2, CLOUD_GREY, 1.8, 24, new Vec3(0, 0.004, 0), 0.7);
		Vfx.emit(level, ParticleTypes.FALLING_WATER, centre.add(0, -0.45, 0), 3, 0.75, 0.0);
		RandomSource r = level.getRandom();
		if (age % 8 == 4 || r.nextInt(3) == 0) {
			Vec3 a = centre.add((r.nextDouble() - 0.5) * 1.6, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 1.6);
			Vec3 b = centre.add((r.nextDouble() - 0.5) * 1.6, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 1.6);
			Sigils.flash(level, a, storm().secondary(), 1.3F);
			ElementFx.arc(level, a, b, storm().accent(), 0.025, 1, false, 4);
		}
	}

	/** A second Thunderhead on the same target: the cloud swells and grumbles. */
	static void cloudFed(ServerLevel level, Vec3 centre) {
		billows(level, centre, 4, CLOUD_DARK, 2.3, 30, Vec3.ZERO, 0.85);
		Sigils.flash(level, centre, storm().secondary(), 2.0F);
		Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4F, 0.6F);
	}

	/** A second with no one under the cloud to strike: it flashes inside and rumbles. */
	static void cloudRumble(ServerLevel level, Vec3 centre) {
		Sigils.flash(level, centre, storm().secondary(), 2.2F);
		Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.25F, 0.7F);
	}

	/** A bolt from the cloud: the whole cloud lights up, the lightning forks down, and the ground is scorched. */
	static void cloudStrike(ServerLevel level, Vec3 cloud, Entity t) {
		Vec3 top = cloud.add(0, -0.3, 0);
		Vec3 foot = t.position().add(0, 0.1, 0);
		ElementFx.bolt(level, top, foot, 0.1, 3, 2);
		Sigils.flash(level, cloud, storm().secondary(), 3.4F);
		Sigils.flash(level, centre(t), storm().primary(), 1.4F);
		ElementFx.groundRing(level, t.position(), storm().primary(), 0.2, 1.6, 0.06, 7);
		ElementFx.groundRing(level, t.position(), storm().accent(), 0.15, 1.1, 0.04, 9);
		ElementFx.sparks(level, centre(t), 10, 0.35);
		Vfx.emit(level, new DustParticleOptions(SCORCH, 1.5F), t.position().add(0, 0.08, 0), 4, 0.35, 0.0);
		RandomSource r = level.getRandom();
		Fx.sound(level, cloud, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55F, 1.1F + r.nextFloat() * 0.3F);
		Fx.sound(level, foot, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7F, 1.2F);
	}

	/** The storm passes: pale billows drifting apart and upward, and a last grumble. */
	static void cloudFade(ServerLevel level, Vec3 centre) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < 4; i++) {
			double a = Math.PI * 2 * i / 4 + r.nextDouble();
			Motes.clouds(level, centre.add(Math.cos(a) * 0.6, 0.2, Math.sin(a) * 0.6), 1, 0.3, CLOUD_GREY, 1.9, 34,
				new Vec3(Math.cos(a) * 0.03, 0.02, Math.sin(a) * 0.03), 0.004, 0.5);
		}
		Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.2F, 0.5F);
	}

	// ------------------------------------------------------------------ Downdraft

	/**
	 * The downburst: crescents of wind spiralling down out of the sky, turning to sand as they near the ground, streaks of
	 * air driving down round the point, rings falling in from above, a gust racing out along the ground and the earth
	 * cracking under it.
	 */
	static void downburst(ServerLevel level, Vec3 point, double radius) {
		Vec3 floor = ElementFx.floor(level, point, 4);
		Vec3 ground = floor != null ? floor : point;
		double top = 5.0;
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		int arcs = 6;
		for (int i = 0; i < arcs; i++) {
			double k = i / (double) (arcs - 1);
			double a = phase + i * 2.1;
			Vec3 at = ground.add(0, top * (1 - k) + 0.25, 0);
			ElementFx.slash(level, at, ElementFx.tilted(0.25, a + Math.PI / 2), ElementFx.flatDir(a), i < 3 ? ElementFx.WIND.primary() : ElementFx.EARTH.secondary(),
				0.9 + 1.4 * k, 2.3, 0.08, 1 + i, 7 + i);
		}
		for (int i = 0; i < 6; i++) {
			double a = phase + Math.PI * 2 * i / 6;
			Vec3 foot = ground.add(Math.cos(a) * 1.6, 0.15, Math.sin(a) * 1.6);
			ElementFx.ray(level, foot.add(0, top, 0), foot, i % 2 == 0 ? ElementFx.WIND.secondary() : ElementFx.WIND.primary(), 0.035, 7);
		}
		ElementFx.ring(level, ground.add(0, top, 0), UP, ElementFx.WIND.secondary(), radius * 0.8, 0.3, 0.05, 8);
		ElementFx.ring(level, ground.add(0, top * 0.5, 0), UP, ElementFx.WIND.accent(), radius * 0.6, 0.3, 0.04, 9);
		ElementFx.gustRing(level, ground, radius);
		ElementFx.crack(level, ground, 1.2, 24);
		Fx.sound(level, ground, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 0.6F);
		Fx.sound(level, ground, WildercordSounds.impact("earth"), 0.6F, 1.0F);
	}

	/** A flyer the downdraft can't bring down (nowhere safe below it, or a boss): the wind only buffets it. */
	static void buffeted(ServerLevel level, Entity t) {
		Vec3 c = centre(t);
		ElementFx.ring(level, c.add(0, t.getBbHeight() * 0.6, 0), UP, ElementFx.WIND.secondary(), 1.4, 0.2, 0.05, 7);
		ElementFx.windImpact(level, c, 1.0);
		Fx.sound(level, c, SoundEvents.WIND_CHARGE_BURST.value(), 0.7F, 0.8F);
	}

	/**
	 * A slam: streaks of wind along the way it was driven down, the wind closing over it, and the ground taking it:
	 * a cracked seal, chips and dust thrown up, a ring racing out, harder the further it fell.
	 */
	static void slam(ServerLevel level, Vec3 from, Vec3 to, Entity t, double fallen) {
		double w = Math.max(0.5, t.getBbWidth());
		double h = t.getBbHeight();
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		if (fallen > 0.5) {
			for (int s = 0; s < 3; s++) {
				Vec3 off = ElementFx.flatDir(phase + Math.PI * 2 * s / 3).scale(w * 0.6);
				ElementFx.ray(level, from.add(off).add(0, h, 0), to.add(off).add(0, 0.2, 0), s == 0 ? WHITE : ElementFx.WIND.primary(), 0.05, 7);
			}
		}
		for (int i = 0; i < 3; i++) {
			double a = phase + i * 2.1;
			ElementFx.slash(level, to.add(0, h + 0.4 - i * 0.45, 0), ElementFx.tilted(0.3, a + Math.PI / 2), ElementFx.flatDir(a),
				i == 2 ? ElementFx.EARTH.secondary() : ElementFx.WIND.secondary(), w * (0.9 - 0.15 * i) + 0.3, 2.2, 0.08, 1 + i, 6 + i);
		}
		double force = Math.min(6, fallen);
		ElementFx.crack(level, to, 0.8 + force * 0.15, 30);
		ElementFx.earthImpact(level, to.add(0, 0.2, 0), 0.8 + force * 0.08);
		ElementFx.groundRing(level, to, ElementFx.WIND.secondary(), 0.3, 2.0 + force * 0.25, 0.06, 9);
		if (fallen >= 3) {
			ScreenFx.shake(level, to, 0.35F, 10);
		}
		Fx.sound(level, to, fallen >= 4 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND, 1.0F, 0.9F);
	}

	// ------------------------------------------------------------------ Updraft

	/**
	 * The column of wind: rings of air rising up it one after another and narrowing, crescents winding round it in two
	 * strands, streaks of air running straight up its sides, and what it tears off the ground thrown skyward.
	 */
	static void updraft(ServerLevel level, Vec3 ground, double radius) {
		double height = 7.0;
		for (int k = 0; k < 6; k++) {
			int step = k;
			Scheduler.later(1 + k, () -> ElementFx.ring(level, ground.add(0, 0.3 + step * 1.2, 0), UP, step % 2 == 0 ? ElementFx.WIND.secondary() : ElementFx.WIND.accent(),
				radius * (0.95 - 0.07 * step), radius * (1.1 - 0.07 * step), 0.05, 8));
		}
		ElementFx.swirl(level, ground, radius * 0.85, height, 7, ElementFx.WIND.primary(), WHITE);
		Scheduler.later(4, () -> ElementFx.swirl(level, ground.add(0, 0.5, 0), radius * 0.65, height * 0.9, 6, ElementFx.WIND.accent(), ElementFx.WIND.secondary()));
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 4; i++) {
			Vec3 foot = ground.add(ElementFx.flatDir(phase + Math.PI / 2 * i).scale(radius * 0.9)).add(0, 0.2, 0);
			ElementFx.ray(level, foot, foot.add(0, height * 0.85, 0), ElementFx.WIND.secondary(), 0.03, 10);
		}
		for (int i = 0; i < 10; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(r.nextDouble()) * radius;
			Vfx.fling(level, ParticleTypes.CLOUD, ground.add(Math.cos(a) * d, 0.2, Math.sin(a) * d), new Vec3(-Math.sin(a) * 0.2, 1, Math.cos(a) * 0.2),
				0.5 + r.nextDouble() * 0.4);
		}
		ElementFx.gustRing(level, ground, radius * 1.4);
		Fx.sound(level, ground, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 1.3F);
		Fx.sound(level, ground, SoundEvents.WIND_CHARGE_THROW, 0.8F, 0.7F);
	}

	/** A creature caught up in it: the wind wrapped round it as it goes. */
	static void hurled(ServerLevel level, Entity t) {
		double w = Math.max(0.5, t.getBbWidth());
		ElementFx.swirl(level, t.position(), w * 0.6 + 0.2, t.getBbHeight() + 0.6, 3, WHITE, ElementFx.WIND.primary());
		Vfx.emit(level, ParticleTypes.SMALL_GUST, t.position().add(0, 0.2, 0), 2, w * 0.3, 0.0);
	}

	/**
	 * The smash: a ring of air crashing down on it from above, speed lines along its fall, and a burst of wind and
	 * cloud where it lands (all wind: no earth cracks under this one).
	 */
	static void smash(ServerLevel level, Vec3 from, Vec3 to, Entity t, boolean moved) {
		double w = Math.max(0.5, t.getBbWidth());
		double h = t.getBbHeight();
		Vec3 over = from.add(0, h + 1.2, 0);
		ElementFx.ring(level, over, UP, WHITE, w + 1.6, 0.2, 0.06, 6);
		ElementFx.slash(level, over, ElementFx.perp(ElementFx.flatDir(level.getRandom().nextDouble() * Math.PI * 2)), new Vec3(0, -1, 0),
			ElementFx.WIND.secondary(), w + 0.8, 2.4, 0.1, 1, 6);
		if (moved) {
			for (int s = -1; s <= 1; s += 2) {
				Vec3 off = ElementFx.flatDir(s * 1.3).scale(w * 0.55);
				ElementFx.ray(level, from.add(off).add(0, h, 0), to.add(off).add(0, 0.2, 0), ElementFx.WIND.primary(), 0.05, 6);
			}
		}
		ElementFx.gustRing(level, to, 2.4);
		ElementFx.windImpact(level, to.add(0, 0.4, 0), 1.2);
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			Vfx.fling(level, ParticleTypes.CLOUD, to.add(0, 0.15, 0), new Vec3(Math.cos(a), 0.1, Math.sin(a)), 0.3);
		}
		Fx.sound(level, to, SoundEvents.MACE_SMASH_GROUND, 0.9F, 1.1F);
		Fx.sound(level, to, SoundEvents.WIND_CHARGE_BURST.value(), 0.8F, 0.7F);
	}

	// ------------------------------------------------------------------ Skyglyph

	/** A Skyglyph with nothing to be written on (nothing solid below): a puff of wind and glyphs scattering. */
	static void glyphFizzle(ServerLevel level, Vec3 at) {
		ElementFx.shimmer(level, at, 0.4, 6);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, at, 2, 0.3, 0.0);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 0.7F);
	}

	/**
	 * The glyph is written: an arcane star turning inside a ring of wind that turns the other way, a pale band round
	 * them both, all lying on the ground for its whole life (fading as its time runs out), with a gust and glyphs drawn in.
	 */
	static void glyphOpen(ServerLevel level, Vec3 feet, double radius, int ticks) {
		ElementFx.flatSigil(level, feet, SigilOption.STAR, ElementFx.ARCANE.primary(), radius * 0.72, ticks, 0.06);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.RING, ElementFx.WIND.primary(), radius * 1.05, ticks, -0.045);
		ElementFx.flatSigil(level, feet.add(0, 0.02, 0), SigilOption.BAND, ElementFx.WIND.accent(), radius * 1.18, ticks, 0);
		Sigils.flash(level, feet.add(0, 0.3, 0), ElementFx.ARCANE.secondary(), 1.6F);
		ElementFx.shimmer(level, feet.add(0, 0.4, 0), radius * 0.6, 8);
		ElementFx.gustRing(level, feet, radius * 1.4);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.4F);
		Fx.sound(level, feet, WildercordSounds.cast("wind"), 0.7F, 1.0F);
	}

	/** Every half second on a glyph: a pinwheel of three wind crescents going round over it, and specks of light rising off it. */
	static void glyphIdle(ServerLevel level, Vec3 feet, double radius, int age) {
		double phase = age * 0.19;
		for (int i = 0; i < 3; i++) {
			double a = phase + i * Math.PI * 2 / 3;
			ElementFx.slash(level, feet.add(0, 0.18, 0), UP, ElementFx.flatDir(a), i == 1 ? ElementFx.ARCANE.primary() : ElementFx.WIND.secondary(), radius * 0.85,
				1.3, 0.06, 5, 11);
		}
		Motes.glows(level, feet.add(0, 0.2, 0), 2, radius * 0.45, ElementFx.WIND.secondary(), 0.25, 28, new Vec3(0, 0.06, 0), 0.01);
		if (age % 20 == 1) {
			Vfx.emit(level, ParticleTypes.SMALL_GUST, feet.add(0, 0.3, 0), 1, radius * 0.3, 0.0);
		}
	}

	/** The glyph goes: its rings fall in on the middle and its glyphs scatter upward. */
	static void glyphEnd(ServerLevel level, Vec3 feet, double radius) {
		ElementFx.groundRing(level, feet, ElementFx.ARCANE.accent(), radius * 1.2, 0.1, 0.05, 9);
		Vfx.emit(level, ParticleTypes.ENCHANT, feet.add(0, 0.5, 0), 10, radius * 0.4, 0.3);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, feet.add(0, 0.3, 0), 1, 0.2, 0.0);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.8F);
	}

	/** An ally launched: wind spiralling up round them in white and pink, the star under their feet flaring, cloud thrown down and out. */
	static void glyphLaunch(ServerLevel level, Vec3 feet, Entity t) {
		double w = Math.max(0.5, t.getBbWidth());
		ElementFx.swirl(level, t.position(), w * 0.6 + 0.3, t.getBbHeight() + 2.2, 5, ElementFx.WIND.secondary(), ElementFx.ARCANE.primary());
		ElementFx.starSeal(level, feet.add(0, 0.12, 0), UP, 0.9, 10);
		ElementFx.ring(level, feet.add(0, 0.12, 0), UP, ElementFx.WIND.primary(), 0.3, 2.2, 0.06, 8);
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			Vfx.fling(level, ParticleTypes.CLOUD, feet.add(0, 0.2, 0), new Vec3(Math.cos(a), -0.1, Math.sin(a)), 0.25);
		}
		Fx.sound(level, t.position(), SoundEvents.BREEZE_JUMP, 1.0F, 1.1F);
		Fx.sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.5F);
	}

	/** An enemy thrown off: a paddle of wind sweeping up and over it, outward, and a star flaring at the glyph's edge. */
	static void glyphThrow(ServerLevel level, Vec3 feet, Entity t, Vec3 away) {
		Vec3 c = centre(t);
		Vec3 side = ElementFx.perp(away);
		ElementFx.slash(level, c.subtract(away.scale(0.5)), side, away.add(0, 0.6, 0), ElementFx.WIND.primary(), Math.max(0.8, t.getBbWidth() + 0.3), 2.0, 0.12, 2, 7);
		ElementFx.slash(level, c.subtract(away.scale(0.7)), side, away.add(0, 0.6, 0), ElementFx.ARCANE.accent(), Math.max(0.7, t.getBbWidth() + 0.2), 1.7, 0.06, 3, 8);
		ElementFx.ring(level, c, away, ElementFx.WIND.secondary(), 0.3, 1.3, 0.05, 7);
		Sigils.flash(level, feet.add(away.scale(1.2)).add(0, 0.3, 0), ElementFx.ARCANE.primary(), 1.0F);
		Fx.sound(level, c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8F, 1.4F);
	}

	// ------------------------------------------------------------------ Recoil

	/**
	 * The throw: three crescents of wind bowing out along it and a ring of gold round the target; and, the first time,
	 * where it stood a stopped clock on the ground and a band of gold holding its place.
	 */
	static void recoilHurl(ServerLevel level, Entity t, Vec3 anchor, Vec3 away, boolean first) {
		Vec3 c = centre(t);
		for (int i = 0; i < 3; i++) {
			Vec3 at = c.add(0, (i - 1) * 0.45, 0).subtract(away.scale(0.8));
			ElementFx.slash(level, at, UP, away, i == 1 ? ElementFx.WIND.secondary() : ElementFx.WIND.primary(), Math.max(0.8, t.getBbWidth() + 0.3), 1.6, 0.11,
				1 + i, 7);
		}
		ElementFx.ring(level, c, away, ElementFx.TIME.primary(), 0.3, 1.4, 0.04, 8);
		Fx.sound(level, c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.9F, 1.1F);
		if (!first) {
			return;
		}
		int hold = FusedStormNumbers.RECOIL_DELAY + 6;
		ElementFx.stoppedClock(level, anchor.add(0, 0.09, 0), UP, 0.75, Math.atan2(away.z, away.x), hold);
		ElementFx.flatSigil(level, anchor, SigilOption.BAND, ElementFx.TIME.primary(), Math.max(0.9, t.getBbWidth() + 0.4), hold, 0);
		ElementFx.goldenTicks(level, anchor.add(0, 0.5, 0), 0.3, 5);
		Fx.sound(level, anchor, WildercordSounds.impact("time"), 0.6F, 1.0F);
	}

	/**
	 * While it waits: a thread of gold from where it stood to where it is, a fleck of light drawn back home along it,
	 * and a faint afterimage of gold rising where it stood. Just before the snap, the clock there ticks.
	 */
	static void recoilTether(ServerLevel level, Vec3 anchor, Entity t, int age, int delay) {
		Vec3 c = centre(t);
		Vec3 home = anchor.add(0, t.getBbHeight() * 0.5, 0);
		ElementFx.ray(level, anchor.add(0, 0.1, 0), c, ElementFx.TIME.secondary(), 0.022, 11);
		Motes.seek(level, c, home, ElementFx.TIME.primary(), 0.3, 10, 1.0);
		Motes.glows(level, home, 2, Math.max(0.2, t.getBbWidth() * 0.3), ElementFx.TIME.primary(), 0.3, 14, new Vec3(0, 0.02, 0), 0.005);
		if (age + 10 >= delay) {
			ElementFx.clock(level, anchor.add(0, 0.12, 0), UP, 0.7, 10, true);
			Fx.sound(level, anchor, SoundEvents.COMPARATOR_CLICK, 0.7F, 1.6F);
		}
	}

	/**
	 * The snap: time turns back, a clock over the spot with its hands sweeping backward and wind winding the wrong way
	 * round it, and a streak of wind and gold from where it was back home.
	 */
	static void recoilSnap(ServerLevel level, Vec3 from, Vec3 home, Entity t, boolean moved) {
		double h = t.getBbHeight();
		Vec3 back = home.add(0, h * 0.5, 0);
		ElementFx.clock(level, home.add(0, 0.1, 0), UP, 0.95, 8, true);
		ElementFx.swirl(level, home, Math.max(0.6, t.getBbWidth()) + 0.2, h + 0.5, 4, ElementFx.TIME.primary(), ElementFx.WIND.secondary());
		if (moved) {
			Vec3 was = from.add(0, h * 0.5, 0);
			ElementFx.ray(level, was, back, ElementFx.WIND.secondary(), 0.09, 8);
			ElementFx.ray(level, was, back, ElementFx.TIME.secondary(), 0.035, 7);
			Vfx.radial(level, ParticleTypes.CLOUD, was, 6, 0.12);
		}
		Sigils.flash(level, back, ElementFx.TIME.secondary(), 1.6F);
		ElementFx.goldenTicks(level, back, 0.4, 8);
		Fx.sound(level, back, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.8F);
		Fx.sound(level, back, SoundEvents.WIND_CHARGE_BURST.value(), 0.6F, 0.7F);
	}
}
