package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * How the fused effects of void, arcane and time look ({@link FusedVoid}). Like every fused effect, each draws on both of
 * its elements' visual languages ({@link ElementFx}):
 * <ul>
 *   <li><b>Entropy</b> (void, time): the target frays into dark threads and motes under a dial of golden pips that go
 *       dark one a second, the hand ticking down.</li>
 *   <li><b>Devour</b> (void, blood): a maw of darkness rimmed in crimson, rows of fangs above and below, bites shut.</li>
 *   <li><b>Timesteal</b> (arcane, time): a clock face round the target, an arcane star turning backward in it; its golden
 *       hands point at the thief and draw each stolen effect across as a light in that effect's own colour.</li>
 *   <li><b>Hemomancy</b> (arcane, blood): blood runes circle the caster's feet, crimson threads run from them into the
 *       target and a blood star seal flares on it.</li>
 *   <li><b>Reckoning</b> (time, blood): a gold seal over the target fills with crimson as each wound is written in, a hand
 *       drawing its time round it, then folds shut like a book and stamps the debt down.</li>
 *   <li><b>Singularity</b> (void, void): a real black hole, a sphere of darkness in a tilted accretion disk of white-hot and
 *       violet light, arcs racing round it, specks spiralling in, until it bursts.</li>
 *   <li><b>Prismatic Burst</b> (arcane, arcane): over a star seal, a rainbow of one arch for every mark used up, each in
 *       its element's colour, drawn in and burst out in all of them.</li>
 *   <li><b>Chronoshift</b> (time, time): gold cogs mesh and turn forward on the ground round the ally, and a clock hand
 *       jumps on round them.</li>
 * </ul>
 */
final class FusedVoidVfx {
	private FusedVoidVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;
	/** Void's own darkness: a hole in the world with a thin violet rim. */
	private static final int HOLE = ElementFx.dark(ElementFx.VOID.accent());
	/** Darkness rimmed in crimson: void with blood in it. */
	private static final int RED_HOLE = ElementFx.dark(ElementFx.BLOOD.primary());
	/** The dark motes a creature unravels into. */
	private static final DustParticleOptions MOTE = new DustParticleOptions(0x1C0A2A, 1.1F);
	private static final int SMOKE = 0x2A1838;
	/** The accretion disk's hottest band. */
	private static final int HOT = 0xFBF2FF;
	/** Prismatic Burst's chimes, one a mark, climbing the pentatonic scale. */
	private static final float[] CHORD = {0.75F, 0.84F, 0.94F, 1.12F, 1.26F, 1.5F};

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	private static double girth(Entity t) {
		return Math.max(0.6, t.getBbWidth());
	}

	/** Leaning back from level toward whoever is looking from {@code viewer}, so a disc over a head reads from the ground. */
	static Vec3 facing(Entity t, Entity viewer) {
		Vec3 to = viewer.position().subtract(t.position());
		Vec3 flat = new Vec3(to.x, 0, to.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return UP;
		}
		return flat.normalize().scale(0.55).add(0, 0.85, 0).normalize();
	}

	// ------------------------------------------------------------------ Entropy

	private static Vec3 dialCentre(Entity t) {
		return t.position().add(0, t.getBbHeight() + 0.45, 0);
	}

	private static double dialRadius(Entity t) {
		return Math.max(0.42, t.getBbWidth() * 0.75);
	}

	/** Entropy takes hold: darkness falls in on the target, it starts to fray, and a dial of golden pips opens over its head. */
	static void entropy(ServerLevel level, LivingEntity t, LivingEntity caster, int steps) {
		Vec3 c = centre(t);
		ElementFx.implode(level, c, girth(t) + 0.8, 8);
		fray(level, t, 6);
		dial(level, t, facing(t, caster), 0, steps, 22);
		ElementFx.goldenTicks(level, c, 0.3, 4);
		Fx.sound(level, c, WildercordSounds.cast("void"), 0.7F, 0.8F);
		Fx.sound(level, c, SoundEvents.NOTE_BLOCK_HAT, 0.7F, 1.4F);
	}

	/** Struck again while it frays: the dial winds back up to its full count of pips. */
	static void entropyWound(ServerLevel level, LivingEntity t, LivingEntity caster, int step, int last) {
		Vec3 face = facing(t, caster);
		dial(level, t, face, step, last, 14);
		ElementFx.ring(level, dialCentre(t), face, ElementFx.TIME.secondary(), dialRadius(t) * 1.6, dialRadius(t), 0.04, 7);
		fray(level, t, 3);
		Fx.sound(level, centre(t), SoundEvents.NOTE_BLOCK_HAT, 0.5F, 1.8F);
	}

	/** One second of unravelling: a pip goes dark, the hand ticks on, and the target frays a little more (the last time, the dial collapses). */
	static void entropyTick(ServerLevel level, LivingEntity t, LivingEntity caster, int step, int last, boolean end) {
		Vec3 c = centre(t);
		Vec3 face = facing(t, caster);
		if (!end) {
			dial(level, t, face, step, last, 22);
		}
		fray(level, t, Math.min(12, 3 + 2 * step));
		ElementFx.ring(level, c, ElementFx.tilted(1.0, level.getRandom().nextDouble() * Math.PI * 2), HOLE, girth(t) + 0.5, 0.1, 0.06, 7);
		Fx.sound(level, c, SoundEvents.NOTE_BLOCK_HAT, 0.8F, step % 2 == 0 ? 1.05F : 1.3F);
		Fx.sound(level, c, SoundEvents.SOUL_ESCAPE, 0.5F, 0.55F + 0.07F * Math.min(5, step));
		if (end) {
			Vec3 top = dialCentre(t);
			double r = dialRadius(t);
			ElementFx.ring(level, top, face, ElementFx.TIME.primary(), r, 0.05, 0.05, 8);
			ElementFx.ring(level, top, face, HOLE, r * 1.4, 0.05, 0.09, 9);
			ElementFx.blackCore(level, c, 0.2 + 0.15 * girth(t), 10);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, c, 14, 0.18);
			Motes.clouds(level, c, 4, girth(t) * 0.4, SMOKE, 1.0, 34, new Vec3(0, 0.03, 0), 0.03, 0.55);
			Fx.sound(level, c, SoundEvents.BELL_RESONATE, 0.6F, 0.5F);
			Fx.sound(level, c, WildercordSounds.impact("void"), 0.7F, 0.7F);
		}
	}

	/**
	 * The countdown dial over the head: a gold ring, a pip for every second (up to twelve; the spent ones
	 * gone to darkness) and a hand on the next one to go.
	 */
	private static void dial(ServerLevel level, LivingEntity t, Vec3 face, int spent, int total, int lifetime) {
		Vec3 at = dialCentre(t);
		double r = dialRadius(t);
		int pips = Math.max(1, Math.min(12, total));
		int gone = (int) Math.min(pips, Math.round(spent * pips / (double) Math.max(1, total)));
		ElementFx.ring(level, at, face, ElementFx.TIME.primary(), r, r, 0.03, lifetime);
		for (int k = 0; k < pips; k++) {
			Vec3 dir = ElementFx.inPlane(face, Math.PI / 2 - Math.PI * 2 * k / pips);
			boolean dark = k < gone;
			ElementFx.ray(level, at.add(dir.scale(r * 0.7)), at.add(dir.scale(r * 1.2)), dark ? HOLE : ElementFx.TIME.secondary(), dark ? 0.08 : 0.045,
				lifetime);
		}
		if (gone < pips) {
			Vec3 hand = ElementFx.inPlane(face, Math.PI / 2 - Math.PI * 2 * gone / pips);
			ElementFx.ray(level, at, at.add(hand.scale(r * 0.78)), ElementFx.TIME.accent(), 0.05, lifetime);
		}
	}

	/** Fraying: dark threads peeling off the body and drifting up, dark motes, and a few violet sparks where it comes apart. */
	private static void fray(ServerLevel level, LivingEntity t, int count) {
		RandomSource r = level.getRandom();
		Vec3 c = centre(t);
		double w = t.getBbWidth() * 0.5 + 0.05;
		double h = t.getBbHeight();
		for (int i = 0; i < count; i++) {
			Vec3 out = ElementFx.flatDir(r.nextDouble() * Math.PI * 2);
			Vec3 from = t.position().add(out.scale(w)).add(0, h * (0.15 + 0.8 * r.nextDouble()), 0);
			Vec3 dir = out.scale(0.6).add(0, 0.5 + r.nextDouble() * 0.4, 0).normalize();
			double length = 0.3 + r.nextDouble() * 0.4;
			boolean spark = i % 3 == 2;
			ElementFx.ray(level, from, from.add(dir.scale(length)), spark ? ElementFx.VOID.primary() : HOLE, spark ? 0.025 : 0.055, 8 + r.nextInt(5));
		}
		Vfx.emit(level, MOTE, c, count + 2, w * 0.9, 0.02);
		Motes.glows(level, c, Math.max(1, count / 3), w * 0.8, ElementFx.VOID.secondary(), 0.07, 22, new Vec3(0, 0.025, 0), 0.01);
		if (count >= 6) {
			Motes.clouds(level, c, 1, w * 0.6, SMOKE, 0.7, 30, new Vec3(0, 0.025, 0), 0.02, 0.5);
		}
	}

	// ------------------------------------------------------------------ Devour

	/**
	 * Devour: a maw of darkness opens round the target, a row of crimson-rimmed fangs above it and one
	 * below, and closes on it in three snaps; then the bite. {@code full} false (past the first few
	 * targets) skips the jaws.
	 */
	static void devour(ServerLevel level, LivingEntity t, boolean killed, boolean full) {
		Vec3 c = centre(t);
		double r = Math.max(0.75, t.getBbWidth() * 0.8 + 0.4);
		double h = Math.max(0.9, t.getBbHeight());
		ElementFx.implode(level, c, r * 1.7, 7);
		if (full) {
			double phase = level.getRandom().nextDouble() * Math.PI * 2;
			jaws(level, c, r, h, phase, 0.0);
			Scheduler.later(2, () -> jaws(level, c, r, h, phase, 0.5));
			Scheduler.later(4, () -> jaws(level, c, r, h, phase, 1.0));
			Fx.sound(level, c, SoundEvents.WARDEN_HEARTBEAT, 0.6F, 0.7F);
		}
		Scheduler.later(full ? 5 : 1, () -> bite(level, c, r, killed));
	}

	/** One snap of the jaws, {@code shut} from 0 (gaping) to 1 (closed): two rings of gum and their fangs, the lower row between the upper. */
	private static void jaws(ServerLevel level, Vec3 c, double r, double h, double phase, double shut) {
		int teeth = 7;
		double gape = h * (0.62 - 0.52 * shut);
		double round = r * (1.15 - 0.3 * shut);
		for (int row = -1; row <= 1; row += 2) {
			Vec3 gum = c.add(0, row * gape, 0);
			ElementFx.ring(level, gum, UP, RED_HOLE, round, round, 0.13, 4);
			for (int k = 0; k < teeth; k++) {
				Vec3 out = ElementFx.flatDir(phase + Math.PI * 2 * (k + (row > 0 ? 0.0 : 0.5)) / teeth);
				Vec3 base = gum.add(out.scale(round));
				Vec3 tip = base.subtract(out.scale(0.14)).add(0, -row * (0.3 + 0.1 * (k % 2)), 0);
				ElementFx.ray(level, base, tip, RED_HOLE, 0.085, 4);
			}
		}
	}

	/** The bite: the maw swallows (a flash of darkness round a black core), a heartbeat of crimson and blood falling. */
	private static void bite(ServerLevel level, Vec3 c, double r, boolean killed) {
		Sigils.flash(level, c, HOLE, (float) (r * 2.2));
		ElementFx.blackCore(level, c, 0.2 + r * 0.15, 8);
		ElementFx.ring(level, c, UP, ElementFx.BLOOD.primary(), r * 1.2, 0.1, 0.06, 6);
		ElementFx.pulse(level, c, UP, r * 0.9);
		ElementFx.drip(level, c, 0.3, 6);
		Fx.sound(level, c, SoundEvents.EVOKER_FANGS_ATTACK, 0.9F, 0.7F);
		Fx.sound(level, c, WildercordSounds.impact("void"), 0.6F, 0.8F);
		if (killed) {
			ElementFx.implode(level, c, r * 2.2, 10);
			Vfx.radial(level, ParticleTypes.SQUID_INK, c, 8, 0.12);
			Fx.sound(level, c, SoundEvents.GENERIC_EAT, 1.0F, 0.6F);
		}
	}

	/** The kill feeds the caster: crimson and violet motes spiral out of the prey into them, and a heartbeat rings out at their feet. */
	static void feed(ServerLevel level, LivingEntity prey, LivingEntity caster) {
		Vec3 from = centre(prey);
		Vec3 to = centre(caster);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 7; i++) {
			int colour = i % 3 == 0 ? ElementFx.VOID.secondary() : i % 3 == 1 ? ElementFx.BLOOD.primary() : ElementFx.BLOOD.secondary();
			Motes.seek(level, from.add(ElementFx.randomDir(r).scale(0.3)), to, colour, 0.24, 12 + i, (i % 2 == 0 ? 1 : -1) * 0.8);
		}
		Scheduler.later(12, () -> {
			if (!caster.isAlive() || caster.level() != level) {
				return;
			}
			Vec3 feet = caster.position();
			ElementFx.groundRing(level, feet, ElementFx.BLOOD.primary(), 0.2, 1.4, 0.06, 10);
			ElementFx.groundRing(level, feet, ElementFx.VOID.primary(), 0.1, 0.95, 0.04, 12);
			ElementFx.pulse(level, centre(caster), UP, 1.1);
			// A glint of gold: the absorption it leaves.
			Motes.glows(level, centre(caster), 5, 0.4, 0xFFD27A, 0.1, 20, new Vec3(0, 0.02, 0), 0.01);
			Fx.sound(level, feet, SoundEvents.WARDEN_HEARTBEAT, 0.9F, 1.3F);
		});
	}

	// ------------------------------------------------------------------ Timesteal

	/**
	 * Timesteal: a clock face opens round the target (twelve marks, an arcane star turning backward inside
	 * it), its golden hands swing round to point at the thief and reach across to them, and each stolen
	 * effect goes along them as a light in its own colour. With nothing to take, the hands close on nothing.
	 */
	static void timesteal(ServerLevel level, LivingEntity t, LivingEntity caster, List<Integer> stolen) {
		Vec3 c = centre(t);
		Vec3 to = centre(caster);
		double r = Math.max(0.7, t.getBbWidth() * 0.6 + 0.45);
		Vec3 flat = new Vec3(to.x - c.x, 0, to.z - c.z);
		Vec3 point = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
		// The face.
		ElementFx.ring(level, c, UP, ElementFx.TIME.primary(), r, r, 0.035, 18);
		for (int q = 0; q < 12; q++) {
			Vec3 d = ElementFx.flatDir(Math.PI * 2 * q / 12);
			boolean quarter = q % 3 == 0;
			ElementFx.ray(level, c.add(d.scale(r * (quarter ? 0.76 : 0.86))), c.add(d.scale(r)), quarter ? WHITE : ElementFx.TIME.secondary(),
				quarter ? 0.04 : 0.025, 18);
		}
		ElementFx.sigil(level, c, UP, SigilOption.STAR, ElementFx.ARCANE.primary(), r * 0.62, 20, -0.2);
		// The hands, swung round onto the thief: the long one reaching all the way across.
		double at = Math.atan2(point.z, point.x);
		ElementFx.slash(level, c, UP, ElementFx.flatDir(at - 0.8), ElementFx.TIME.secondary(), r * 0.8, 1.6, 0.06, 3, 7);
		ElementFx.ray(level, c, c.add(point.scale(r * 0.9)), ElementFx.TIME.secondary(), 0.05, 16);
		ElementFx.ray(level, c, c.add(ElementFx.flatDir(at + 0.45).scale(r * 0.55)), ElementFx.TIME.accent(), 0.075, 16);
		boolean took = !stolen.isEmpty();
		Vec3 reach = took ? to : c.add(point.scale(Math.min(2.0, c.distanceTo(to) * 0.4)));
		ElementFx.ray(level, c.add(point.scale(r)), reach, ElementFx.TIME.primary(), took ? 0.03 : 0.02, 14);
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 0.5F, 1.6F);
		if (!took) {
			ElementFx.goldenTicks(level, c, 0.3, 4);
			Fx.sound(level, c, SoundEvents.NOTE_BLOCK_HAT, 0.6F, 0.6F);
			return;
		}
		RandomSource random = level.getRandom();
		for (int i = 0; i < stolen.size(); i++) {
			int colour = stolen.get(i);
			Vec3 from = c.add(ElementFx.randomDir(random).scale(0.25));
			ElementFx.orb(level, from, colour, 0.2, 4);
			Sigils.flash(level, from, colour, 0.9F);
			Motes.seek(level, from, to, colour, 0.38, 14, i % 2 == 0 ? 0.5 : -0.5);
			Motes.seek(level, from, to, WHITE, 0.14, 14, i % 2 == 0 ? -0.5 : 0.5);
		}
		Fx.sound(level, c, SoundEvents.TRIDENT_RETURN, 0.8F, 1.3F);
		Scheduler.later(14, () -> {
			if (!caster.isAlive() || caster.level() != level) {
				return;
			}
			Vec3 feet = caster.position();
			for (int i = 0; i < stolen.size(); i++) {
				ElementFx.groundRing(level, feet, stolen.get(i), 0.2, 1.1 + 0.25 * i, 0.05, 11 + i);
				Motes.burst(level, centre(caster), 4, stolen.get(i), 0.12, 18, 0.25);
			}
			ElementFx.goldenTicks(level, centre(caster), 0.4, 5);
			Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.3F);
		});
	}

	// ------------------------------------------------------------------ Hemomancy

	/**
	 * Blood runes circle the caster: a crimson rune ring turning at their feet, a pale band inside it and,
	 * the more blood they've lost, a violet ring turning the other way and comets of blood racing round them.
	 */
	static void hemomancyRunes(ServerLevel level, LivingEntity caster, int bonus) {
		Vec3 feet = caster.position();
		double r = 1.25 + 0.05 * bonus;
		int life = 22;
		ElementFx.flatSigil(level, feet, SigilOption.RING, ElementFx.BLOOD.primary(), r, life, 0.1 + 0.02 * bonus);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.BAND, ElementFx.BLOOD.secondary(), r * 0.8, life, 0.0);
		if (bonus >= 2) {
			ElementFx.flatSigil(level, feet.add(0, 0.02, 0), SigilOption.RING, ElementFx.ARCANE.accent(), r * 0.62, life, -0.14 - 0.02 * bonus);
		}
		ElementFx.orbit(level, centre(caster), 0.9, 1 + bonus / 2, 12, ElementFx.BLOOD.primary(), ElementFx.BLOOD.secondary());
		ElementFx.drip(level, centre(caster).add(0, -0.3, 0), 0.25, 2 + bonus);
		Feels.sound(level, feet, "blood_tempo", 0.8F + 0.05F * bonus, 1.0F);
	}

	/** The blood strikes: crimson threads run from the caster's runes into the target, where a blood star seal flares, larger the more was paid. */
	static void hemomancy(ServerLevel level, LivingEntity caster, LivingEntity t, int bonus) {
		Vec3 c = centre(t);
		Vec3 feet = caster.position().add(0, 0.25, 0);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		int threads = 1 + Math.min(3, bonus / 2);
		for (int i = 0; i < threads; i++) {
			Vec3 start = feet.add(ElementFx.flatDir(phase + Math.PI * 2 * i / threads).scale(1.25));
			ElementFx.ray(level, start, c, i % 2 == 0 ? ElementFx.BLOOD.primary() : ElementFx.BLOOD.secondary(), 0.035 + 0.006 * bonus, 9);
		}
		Vec3 back = caster.position().subtract(t.position());
		Vec3 face = new Vec3(back.x, 0, back.z).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(back.x, 0, back.z).normalize();
		double size = 0.55 + 0.08 * bonus;
		ElementFx.sigil(level, c.add(face.scale(0.3)), face, SigilOption.STAR, ElementFx.BLOOD.primary(), size, 14, 0.14);
		ElementFx.sigil(level, c.add(face.scale(0.31)), face, SigilOption.RING, ElementFx.ARCANE.accent(), size * 1.4, 14, -0.09);
		ElementFx.pulse(level, c, UP, 0.8 + 0.1 * bonus);
		Vec3 across = ElementFx.inPlane(face, phase);
		ElementFx.cut(level, c.subtract(across.scale(0.5)), face, across, 0.5 + 0.04 * bonus, 0.12);
		ElementFx.drip(level, c, 0.25, 3 + bonus);
		Feels.sound(level, c, "blood_twang", 0.7F, 1.0F);
	}

	// ------------------------------------------------------------------ Reckoning

	private static Vec3 sealCentre(Entity t) {
		return t.position().add(0, t.getBbHeight() + 0.55, 0);
	}

	private static double sealRadius(Entity t) {
		return Math.max(0.5, t.getBbWidth() * 0.75);
	}

	/** The ledger opens: a gold seal over the target, bordered in crimson, and a hand drawing its time round it. */
	static void reckoningOpen(ServerLevel level, LivingEntity t, Vec3 face, int ticks) {
		Vec3 s = sealCentre(t);
		double r = sealRadius(t);
		ElementFx.sigil(level, s, face, SigilOption.CIRCLE, ElementFx.TIME.accent(), r, ticks + 6, 0.015);
		ElementFx.sigil(level, s.add(face.scale(0.01)), face, SigilOption.BAND, ElementFx.BLOOD.primary(), r * 1.12, ticks + 6, 0.0);
		ElementFx.slash(level, s, face, ElementFx.inPlane(face, Math.PI / 2), ElementFx.TIME.secondary(), r * 1.28, Math.PI * 1.97, 0.035, ticks, ticks + 4);
		ElementFx.ring(level, s, face, ElementFx.TIME.primary(), r * 2.0, r, 0.05, 8);
		ElementFx.ray(level, s, centre(t), ElementFx.TIME.primary(), 0.02, 10);
		Fx.sound(level, s, SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.7F);
		Fx.sound(level, s, WildercordSounds.cast("time"), 0.6F, 0.8F);
	}

	/** Another Reckoning while the ledger is open: the seal is stamped again, harder. */
	static void reckoningStamp(ServerLevel level, LivingEntity t, Vec3 face) {
		Vec3 s = sealCentre(t);
		Sigils.flash(level, s, ElementFx.TIME.primary(), 1.0F);
		ElementFx.ring(level, s, face, ElementFx.TIME.secondary(), sealRadius(t) * 1.6, sealRadius(t), 0.04, 7);
		Fx.sound(level, s, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 0.7F);
	}

	/**
	 * A wound written into the ledger: a crimson mark flies up into the seal, a tally stroke goes on its
	 * rim, and a crimson ring widens from its heart toward the rim as the debt grows ({@code fill} 0 to 1).
	 */
	static void reckoningCount(ServerLevel level, LivingEntity t, Vec3 face, double fill, int wounds, int left) {
		Vec3 s = sealCentre(t);
		double r = sealRadius(t);
		Motes.seek(level, centre(t), s, ElementFx.BLOOD.secondary(), 0.2, 6, 0.3);
		if (wounds <= 12) {
			int life = Math.min(left + 3, 100);
			double f = Math.max(0.0, Math.min(1.0, fill));
			double at = r * (0.2 + 0.72 * f);
			ElementFx.ring(level, s, face, ElementFx.BLOOD.primary(), at, at, 0.03 + 0.05 * f, life);
			Vec3 d = ElementFx.inPlane(face, Math.PI / 2 - Math.PI * 2 * (wounds - 1) / 12.0);
			ElementFx.ray(level, s.add(d.scale(r * 0.84)), s.add(d.scale(r * 1.08)), ElementFx.BLOOD.secondary(), 0.04, life);
		}
		Fx.sound(level, s, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 0.5F, 1.4F);
	}

	/**
	 * The ledger closes: the seal comes down round the target and folds shut toward whoever opened it, two
	 * halves (gold and crimson) swinging together like the covers of a book.
	 */
	static void reckoningClosing(ServerLevel level, LivingEntity t, Vec3 face, boolean owed) {
		Vec3 c = centre(t);
		Vec3 s = sealCentre(t);
		double radius = Math.max(0.8, t.getBbHeight() * 0.55);
		double toward = Math.atan2(face.z, face.x);
		ElementFx.ring(level, s, face, ElementFx.TIME.primary(), sealRadius(t) * 1.2, 0.05, 0.06, 5);
		ElementFx.ray(level, s, c, ElementFx.TIME.primary(), 0.08, 4);
		double[] open = {Math.PI / 2, Math.PI / 3.2, Math.PI / 8, 0.0};
		for (int frame = 0; frame < open.length; frame++) {
			double swing = open[frame];
			boolean shut = frame == open.length - 1;
			Scheduler.later(1 + frame, () -> {
				for (int side = -1; side <= 1; side += 2) {
					Vec3 cover = ElementFx.flatDir(toward + side * swing);
					Vec3 spine = cover.cross(UP);
					int colour = !owed ? 0xB8A77A : side < 0 ? ElementFx.TIME.primary() : ElementFx.BLOOD.primary();
					ElementFx.slash(level, c, spine, cover, shut ? WHITE : colour, radius, Math.PI, shut ? 0.09 : 0.07, 1, 3);
				}
			});
		}
		Fx.sound(level, c, SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.5F);
	}

	/** The reckoning lands: the closed seal stamped on the ground under the target, and a crimson cut as deep as the debt. */
	static void reckoningSettle(ServerLevel level, LivingEntity t, Vec3 face, double due) {
		Vec3 c = centre(t);
		Vec3 feet = t.position();
		if (due <= 0) {
			ElementFx.groundRing(level, feet, 0xB8A77A, 0.2, 1.0, 0.03, 8);
			Fx.sound(level, c, SoundEvents.BOOK_PUT, 0.8F, 0.8F);
			return;
		}
		double k = Math.min(1.0, due / FusedVoidRules.RECKONING_CAP);
		Sigils.flash(level, c, ElementFx.BLOOD.secondary(), (float) (1.6 + 1.2 * k));
		ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, ElementFx.TIME.accent(), 1.0 + 0.6 * k, 24, 0.0);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.CRACKED, ElementFx.BLOOD.primary(), 1.1 + 0.7 * k, 24, 0.0);
		ElementFx.groundRing(level, feet, ElementFx.BLOOD.primary(), 0.3, 1.8 + 1.5 * k, 0.08, 10);
		ElementFx.groundRing(level, feet, ElementFx.TIME.primary(), 0.2, 1.3 + 1.0 * k, 0.04, 12);
		Vec3 flat = new Vec3(face.x, 0, face.z).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(face.x, 0, face.z).normalize();
		Vec3 across = ElementFx.inPlane(flat, 0.7);
		ElementFx.cut(level, c.subtract(across.scale(0.5)), flat, across, 0.7 + 0.4 * k, 0.12 + 0.1 * k);
		ElementFx.drip(level, c, 0.3, (int) (4 + 8 * k));
		ScreenFx.shake(level, c, (float) (0.2 + 0.4 * k), 12);
		Fx.sound(level, c, SoundEvents.ANVIL_LAND, (float) (0.4 + 0.4 * k), 0.8F);
		Fx.sound(level, c, SoundEvents.BELL_BLOCK, 0.7F, 0.6F);
		Fx.sound(level, c, WildercordSounds.impact("blood"), 0.9F, 1.0F);
	}

	// ------------------------------------------------------------------ Singularity

	private static double core(int tick) {
		return 0.42 + Math.min(0.12, tick * 0.004);
	}

	/** The hole opens: darkness falls in on a point and it tears open, with a deep hum. */
	static void singularityOpen(ServerLevel level, Vec3 centre, double radius, Vec3 disk, int ticks) {
		ElementFx.implode(level, centre, 1.8, 8);
		Sigils.flash(level, centre, HOLE, 2.2F);
		Light.ring(level, centre, disk, ElementFx.VOID.primary(), radius, core(0) * 1.5, 0.06, 12);
		Fx.sound(level, centre, WildercordSounds.cast("void"), 1.0F, 0.5F);
		Fx.sound(level, centre, SoundEvents.BEACON_ACTIVATE, 0.9F, 0.5F);
		Fx.sound(level, centre, SoundEvents.PORTAL_TRIGGER, 0.35F, 1.8F);
	}

	/**
	 * One tick of the black hole: a sphere of darkness; round it a tilted accretion disk, white-hot at the
	 * rim of the dark and violet further out, with its far side bent up over the hole as a photon ring;
	 * arcs racing round the disk, specks spiralling in from the edge, and now and then a ring of darkness
	 * falling in from the edge of its reach.
	 */
	static void singularity(ServerLevel level, Vec3 centre, double radius, Vec3 disk, int tick) {
		double core = core(tick);
		if (tick % 3 == 0) {
			Light.orb(level, centre, HOLE, core, 4);
		}
		if (tick % 5 == 0) {
			Light.ring(level, centre, disk, HOT, core * 1.55, core * 1.55, 0.065, 6);
			Light.ring(level, centre, disk, ElementFx.VOID.secondary(), core * 2.2, core * 2.2, 0.05, 6);
			Light.ring(level, centre, disk, ElementFx.VOID.primary(), core * 3.0, core * 3.0, 0.04, 6);
			Light.ring(level, centre, disk, 0x6A2AB0, core * 3.9, core * 3.9, 0.025, 6);
			Vec3 across = ElementFx.perp(disk);
			Light.ring(level, centre, across, ElementFx.VOID.secondary(), core * 1.3, core * 1.3, 0.035, 6);
			Light.ring(level, centre, disk.cross(across), HOT, core * 1.22, core * 1.22, 0.025, 6);
		}
		if (tick % 2 == 0) {
			for (int i = 0; i < 2; i++) {
				double a = tick * 0.45 + i * Math.PI;
				double band = 1.8 + 1.8 * (((tick / 2) * 7 + i * 3) % 10) / 10.0;
				Light.slash(level, centre, disk, ElementFx.inPlane(disk, a), i == 0 ? HOT : ElementFx.VOID.secondary(), core * band, 1.3, 0.05, 3, 5);
			}
		}
		if (tick % 3 == 0) {
			RandomSource r = level.getRandom();
			Vec3 dir = ElementFx.inPlane(disk, r.nextDouble() * Math.PI * 2).add(ElementFx.randomDir(r).scale(0.35)).normalize();
			Motes.seek(level, centre.add(dir.scale(radius * 0.85)), centre, tick % 2 == 0 ? ElementFx.VOID.secondary() : HOT, 0.16, 16, 1.25);
		}
		if (tick % 12 == 0) {
			ElementFx.ring(level, centre, disk, HOLE, radius * 0.9, core * 1.2, 0.1, 14);
			Vfx.emit(level, ParticleTypes.PORTAL, centre, 6, 0.1, radius * 0.5);
		}
		if (tick % 20 == 0) {
			Fx.sound(level, centre, SoundEvents.BEACON_AMBIENT, 0.9F, 0.5F);
		}
	}

	/** The hole bursts: a flash, the disk thrown out as a shockwave of white and violet, darkness racing out behind it. */
	static void singularityBurst(ServerLevel level, Vec3 centre, double radius, Vec3 disk) {
		Sigils.flash(level, centre, WHITE, 2.4F);
		Sigils.flash(level, centre, ElementFx.VOID.primary(), 4.0F);
		Light.ring(level, centre, disk, HOT, core(0), radius * 1.1, 0.12, 10);
		Light.ring(level, centre, UP, ElementFx.VOID.primary(), 0.3, radius * 1.2, 0.07, 12);
		Light.ring(level, centre, ElementFx.tilted(1.2, level.getRandom().nextDouble() * Math.PI * 2), HOLE, 0.3, radius, 0.14, 12);
		Vec3 floor = ElementFx.floor(level, centre, 4.0);
		if (floor != null) {
			ElementFx.groundRing(level, floor, ElementFx.VOID.primary(), 0.4, radius * 1.2, 0.09, 12);
			ElementFx.groundRing(level, floor, HOLE, 0.3, radius, 0.12, 14);
		}
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, centre, 28, 0.7);
		Motes.burst(level, centre, 18, ElementFx.VOID.secondary(), 0.14, 26, 0.5);
		ScreenFx.shake(level, centre, 0.7F, radius * 3);
		Fx.sound(level, centre, SoundEvents.GENERIC_EXPLODE, 0.9F, 0.55F);
		Fx.sound(level, centre, SoundEvents.WARDEN_SONIC_BOOM, 0.5F, 1.5F);
		Fx.sound(level, centre, WildercordSounds.impact("void"), 1.0F, 0.6F);
	}

	/** A creature flung out of the burst: a violet streak where it was. */
	static void flung(ServerLevel level, LivingEntity v, Vec3 away) {
		Vec3 c = centre(v);
		ElementFx.ray(level, c.subtract(away.scale(1.2)), c, ElementFx.VOID.primary(), 0.05, 7);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, c, 4, 0.2, 0.05);
	}

	/** A hole that couldn't open (too many already) or closed early: a small implosion and nothing more. */
	static void singularityFizzle(ServerLevel level, Vec3 centre) {
		ElementFx.implode(level, centre, 0.9, 6);
		Fx.sound(level, centre, WildercordSounds.impact("void"), 0.5F, 1.4F);
	}

	// ------------------------------------------------------------------ Prismatic Burst

	/**
	 * Prismatic Burst: arcane at its purest, a star seal under the target and a flash of pink light; over it
	 * a rainbow of one arch for every mark it uses up, each in its element's colour, the arches rising one
	 * after another inside each other; each mark's colour drawn in on a ring of its own tilt and burst back
	 * out in rays, a chime a mark, climbing.
	 */
	static void prismaticBurst(ServerLevel level, LivingEntity t, List<Integer> marks) {
		Vec3 c = centre(t);
		Vec3 feet = t.position();
		int n = marks.size();
		double w = girth(t);
		double phase = level.getRandom().nextDouble() * Math.PI;
		ElementFx.starSeal(level, feet.add(0, 0.08, 0), UP, 0.8 + 0.18 * n, 18);
		Sigils.flash(level, c, ElementFx.ARCANE.secondary(), 1.2F + 0.3F * n);
		ElementFx.orbit(level, c, 0.5 + 0.1 * n, 2, 6);
		for (int i = 0; i < n; i++) {
			int colour = marks.get(i);
			double r = w * 0.6 + 0.9 + 0.28 * i;
			for (int bow = 0; bow < 2; bow++) {
				Vec3 along = ElementFx.flatDir(phase + bow * Math.PI / 2);
				ElementFx.slash(level, feet.add(0, 0.1, 0), along.cross(UP), UP, colour, r, Math.PI, 0.065, 2 + i, 14 + 2 * i);
			}
			ElementFx.ring(level, c, ElementFx.tilted(0.9, phase + i * 1.05), colour, r, 0.1, 0.045, 7 + i);
			int k = i;
			Scheduler.later(2 + i, () -> {
				RandomSource random = level.getRandom();
				for (int j = 0; j < 3; j++) {
					Vec3 d = ElementFx.randomDir(random);
					ElementFx.ray(level, c.add(d.scale(0.2)), c.add(d.scale(1.0 + 0.2 * k)), colour, 0.05, 7);
				}
				Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, CHORD[Math.min(CHORD.length - 1, k)]);
			});
		}
		Scheduler.later(2 + n, () -> {
			ElementFx.ring(level, c, UP, WHITE, 0.2, 1.3 + 0.35 * n, 0.07, 9);
			for (int colour : marks) {
				Motes.burst(level, c, 4, colour, 0.14, 22, 0.35);
			}
			if (n == 0) {
				Motes.burst(level, c, 8, ElementFx.ARCANE.primary(), 0.12, 20, 0.3);
			}
			ElementFx.shimmer(level, c, 0.4, 6);
		});
		Fx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);
		Fx.sound(level, c, WildercordSounds.impact("arcane"), 0.8F, 1.0F);
	}

	// ------------------------------------------------------------------ Chronoshift

	/**
	 * Chronoshift: clockwork turns forward round the ally, a big gold cog on the ground under them and two
	 * small ones meshing with it, turning the other way; round them a clock face whose hand jumps forward
	 * a quarter at a time (three jumps for a player whose spells it hurried, one otherwise), and golden ticks.
	 */
	static void chronoshift(ServerLevel level, LivingEntity t, int turned) {
		Vec3 feet = t.position();
		double w = girth(t);
		int life = 30;
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		double big = 0.95 + w * 0.3;
		double small = big * 0.45;
		double mesh = (big + small) * 0.82;
		gear(level, feet, big, 0.07, life, ElementFx.TIME.primary(), ElementFx.TIME.secondary());
		for (int side = 0; side < 2; side++) {
			Vec3 at = feet.add(ElementFx.flatDir(phase + side * Math.PI * 0.85).scale(mesh)).add(0, 0.015, 0);
			gear(level, at, small, -0.07 * big / small, life, ElementFx.TIME.accent(), ElementFx.TIME.primary());
		}
		Vec3 c = centre(t);
		double r = w * 0.5 + 0.55;
		ElementFx.ring(level, c, UP, ElementFx.TIME.primary(), r, r, 0.03, 22);
		int jumps = turned > 0 ? 3 : 1;
		for (int j = 0; j < jumps; j++) {
			double from = phase + j * Math.PI / 2;
			float pitch = 1.5F + 0.15F * j;
			Scheduler.later(1 + 5 * j, () -> {
				ElementFx.slash(level, c, UP, ElementFx.flatDir(from + Math.PI / 4), ElementFx.TIME.secondary(), r * 0.9, Math.PI / 2, 0.08, 2, 7);
				ElementFx.ray(level, c, c.add(ElementFx.flatDir(from + Math.PI / 2).scale(r * 0.85)), WHITE, 0.04, 5);
				Fx.sound(level, c, SoundEvents.NOTE_BLOCK_HAT, 0.8F, pitch);
			});
		}
		ElementFx.goldenTicks(level, c, 0.4, 5);
		Fx.sound(level, feet, SoundEvents.VAULT_OPEN_SHUTTER, 0.7F, 1.5F);
		Fx.sound(level, feet, WildercordSounds.cast("time"), 0.6F, 1.2F);
		if (turned > 0) {
			Fx.sound(level, feet, SoundEvents.BELL_BLOCK, 0.4F, 1.8F);
		}
	}

	/** A cog of light lying on the ground: an eight-pointed star turning inside a band that cuts its points into teeth, round a small hub. */
	private static void gear(ServerLevel level, Vec3 feet, double size, double spin, int life, int colour, int rim) {
		ElementFx.flatSigil(level, feet, SigilOption.STAR, colour, size, life, spin);
		ElementFx.flatSigil(level, feet.add(0, 0.005, 0), SigilOption.BAND, rim, size * 0.74, life, spin);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.BAND, rim, size * 0.28, life, spin);
	}
}
