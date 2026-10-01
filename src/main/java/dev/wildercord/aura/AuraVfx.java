package dev.wildercord.aura;

import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Sigils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * How aura looks when it acts, sent from the server so everyone sees the same: the stance's breath, the beat, the guard and its
 * perfect moment, Flow's sweep, the slash's flight (the Crescent's crescents of shaped light, in the aura's colour) and the
 * burst of a breakthrough. Built from the mod's shaped light ({@link Light}), circles' flashes and motes, like every spell;
 * the blade's own glow is drawn by each client from the synced look. Kept to a few strong shapes, never a cloud of particles.
 */
public final class AuraVfx {
	private AuraVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A colour lifted toward white, for a light's hot core. */
	static int hot(int color, double t) {
		return AuraRules.mix(color, 0xFFFFFF, t);
	}

	/** Each breath of the stance: a slow ring along the ground and a few motes drawn up into the body. */
	static void breathe(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Light.groundRing(level, feet, color, 0.3, 1.5, 0.05, 30);
		for (int i = 0; i < 4; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 from = feet.add(Math.cos(a) * 1.2, 0.2 + level.getRandom().nextDouble() * 0.5, Math.sin(a) * 1.2);
			Motes.seek(level, from, feet.add(0, 1.0, 0), hot(color, 0.3), 0.09, 18, 0.5);
		}
	}

	/** A breath on the beat: a brighter ring and a soft flash at the heart. */
	static void beat(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 heart = player.position().add(0, 1.1, 0);
		Light.groundRing(level, player.position(), hot(color, 0.35), 0.4, 2.2, 0.07, 14);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.4), 1.1F);
		Motes.burst(level, heart, 8, hot(color, 0.25), 0.08, 18, 0.06);
	}

	/** The guard rising: a ring of the aura's light braced in front of the blade. */
	static void guard(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		Light.ring(level, at, look, color, 0.2, 0.9, 0.06, 8);
		Light.ring(level, at, look, hot(color, 0.5), 0.1, 0.6, 0.03, 6);
	}

	/** A blow caught on a held guard: a ripple where it met the blade. */
	static void held(ServerPlayer player, int color, DamageSource source) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		Light.ring(level, at, look, color, 0.1, 1.1, 0.05, 6);
		Fx.send(level, ParticleTypes.CRIT, at, 4, 0.2, 0.15);
	}

	/** A perfect guard: the parry's gold, flashing and racing out across the blade's ring. */
	static void perfect(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		Sigils.flash(level, at, 0xFF000000 | AuraGuard.PERFECT_COLOR, 1.8F);
		Light.ring(level, at, look, AuraGuard.PERFECT_COLOR, 0.2, 2.2, 0.08, 9);
		Light.ring(level, at, look, color, 0.1, 1.5, 0.05, 7);
		Fx.send(level, ParticleTypes.WAX_OFF, at, 12, 0.3, 0.4);
	}

	/** Flow's sweep: a wide crescent of the aura's colour round the player, at the height of the blow. */
	static void sweep(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 centre = player.position().add(0, 1.0, 0);
		Light.slash(level, centre, UP, look, color, AuraRules.FLOW_SWEEP_RANGE * 0.7, 2.6, 0.32, 3, 6);
		Light.slash(level, centre.add(0, 0.05, 0), UP, look, hot(color, 0.55), AuraRules.FLOW_SWEEP_RANGE * 0.68, 2.2, 0.12, 3, 5);
	}

	/** The slash leaving the blade: a bright crescent at the hand and a flash. */
	static void slashStart(ServerPlayer player, Vec3 origin, Vec3 aim, Vec3 side, int color) {
		ServerLevel level = player.level();
		Vec3 normal = UP.add(side.scale(0.75)).normalize();
		Light.slash(level, origin.add(aim.scale(0.6)), normal, aim, hot(color, 0.3), 1.0, 2.4, 0.4, 1, 4);
		Sigils.flash(level, origin.add(aim.scale(0.9)), 0xFF000000 | color, 1.2F);
	}

	/** One step of the slash's flight: the Crescent's crescent, in the aura's colour, white-hot at its edge. */
	static void slashStep(ServerLevel level, Vec3 front, Vec3 aim, Vec3 side, int color, int tick, boolean weak) {
		double width = AuraRules.SLASH_WIDTH * (weak ? 0.75 : 1.0);
		double radius = width * 0.62;
		double span = 2 * Math.asin(Math.min(0.99, width / 2 / radius));
		// Tilted like a real cut across the body, so it reads from behind the blade as well as from the side.
		Vec3 normal = UP.add(side.scale(0.75)).normalize();
		Vec3 centre = front.subtract(aim.scale(radius * 0.8));
		// A broad crescent of the aura's colour, a narrower brighter one inside it, and a white-hot edge: it trails a little as it flies.
		Light.slash(level, centre, normal, aim, color, radius, span, tick == 0 ? 0.75 : 0.66, tick == 0 ? 2 : 1, 6);
		Light.slash(level, centre.add(aim.scale(0.1)), normal, aim, hot(color, 0.3), radius * 0.97, span * 0.9, 0.34, 1, 5);
		Light.slash(level, centre.add(aim.scale(0.18)), normal, aim, hot(color, 0.75), radius * 0.95, span * 0.82, 0.12, 1, 4);
		if (tick % 3 == 0) {
			Motes.glows(level, front, 2, width * 0.2, hot(color, 0.3), 0.08, 12, aim.scale(-0.02), 0.01);
		}
	}

	/** The slash breaking on a wall. */
	static void slashEnd(ServerLevel level, Vec3 at, Vec3 aim, int color) {
		Sigils.flash(level, at, 0xFF000000 | color, 1.4F);
		Light.ring(level, at, aim, color, 0.2, 1.4, 0.06, 7);
		Motes.burst(level, at, 6, hot(color, 0.3), 0.08, 14, 0.08);
	}

	/** The slash cutting a foe. */
	static void slashCut(ServerLevel level, Vec3 at, Vec3 aim, int color) {
		Sigils.flash(level, at, 0xFF000000 | hot(color, 0.3), 1.0F);
		Fx.send(level, ParticleTypes.CRIT, at, 6, 0.25, 0.2);
	}

	/** A method learned: the aura stirs for the first time, a quiet rise of light. */
	static void learned(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Light.groundRing(level, feet, color, 0.2, 2.0, 0.06, 20);
		Motes.glows(level, feet.add(0, 0.8, 0), 12, 0.5, hot(color, 0.25), 0.09, 30, new Vec3(0, 0.03, 0), 0.01);
	}

	/** A breakthrough: aura bursting out of the body in its colour, a column of light and a ring racing over the ground. */
	static void breakthrough(ServerPlayer player, int color, int stage) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Vec3 heart = feet.add(0, 1.1, 0);
		double size = 2.5 + stage * 0.8;
		Light.groundRing(level, feet, color, 0.3, size * 2, 0.12, 16);
		Light.groundRing(level, feet, hot(color, 0.5), 0.2, size * 1.4, 0.05, 12);
		Light.ring(level, heart, flat(player), color, 0.3, size, 0.08, 12);
		Light.ray(level, feet, feet.add(0, 6 + stage * 2, 0), color, 0.55, 16);
		Light.ray(level, feet, feet.add(0, 5 + stage * 2, 0), hot(color, 0.6), 0.07, 12);
		Light.groundRing(level, feet, color, 0.5, size * 1.1, 0.2, 18);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.3), 3.0F);
		Motes.burst(level, heart, 28, hot(color, 0.2), 0.12, 34, 0.28);
		ScreenFx.shake(level, feet, 0.25F, 16);
	}

	// ------------------------------------------------------------------ Form: Aura Step

	/** A step leaving: a ring snapping out at the feet and a few motes flung back the way it came. */
	static void stepStart(ServerLevel level, Vec3 feet, Vec3 dir, int color) {
		Light.groundRing(level, feet, hot(color, 0.3), 0.2, 1.6, 0.08, 8);
		Sigils.flash(level, feet.add(0, 1.0, 0), 0xFF000000 | hot(color, 0.25), 1.6F);
		for (int i = 0; i < 6; i++) {
			Motes.fling(level, feet.add(0, 0.3 + i * 0.25, 0), dir.scale(-1).add(0, 0.15, 0), 0.25, hot(color, 0.3), 0.08, 14, new Vec3(0, 0.01, 0));
		}
	}

	/** One stretch of a step: a streak of the aura's light low along the way, white-hot down its middle. */
	static void stepTrail(ServerLevel level, Vec3 from, Vec3 to, int color) {
		if (from.distanceToSqr(to) < 1.0E-4) {
			return;
		}
		Light.ray(level, from.add(0, 0.9, 0), to.add(0, 0.9, 0), color, 0.26, 7);
		Light.ray(level, from.add(0, 0.9, 0), to.add(0, 0.9, 0), hot(color, 0.45), 0.06, 5);
		Light.ray(level, from.add(0, 0.12, 0), to.add(0, 0.12, 0), color, 0.16, 9);
		Motes.glows(level, to.add(0, 0.9, 0), 3, 0.35, hot(color, 0.3), 0.07, 12, new Vec3(0, 0.01, 0), 0.01);
	}

	/** A step arriving: a short burst where it lands. */
	static void stepEnd(ServerLevel level, Vec3 feet, Vec3 dir, int color) {
		Light.groundRing(level, feet, color, 0.3, 1.3, 0.06, 7);
		Motes.burst(level, feet.add(0, 0.9, 0), 6, hot(color, 0.3), 0.07, 12, 0.06);
	}

	/** A step with nowhere to go: the aura bumping the wall in front, and nothing more. */
	static void stepBlocked(ServerPlayer player, int color, Vec3 dir) {
		Vec3 at = player.position().add(0, 1.0, 0).add(dir.scale(0.6));
		Light.ring(player.level(), at, dir, color, 0.1, 0.7, 0.04, 5);
	}

	// ------------------------------------------------------------------ Form: aura armour and Intent

	/** Aura armour takes a blow: a ripple across the shell where it struck (the shell itself is drawn by each client). */
	static void shellStruck(ServerPlayer player, int color, DamageSource source) {
		ServerLevel level = player.level();
		Vec3 centre = player.getBoundingBox().getCenter();
		Vec3 from = source.getSourcePosition();
		Vec3 toward = from == null ? flat(player) : from.subtract(centre);
		Vec3 dir = toward.lengthSqr() < 1.0E-4 ? flat(player) : toward.normalize();
		Vec3 at = centre.add(dir.scale(0.55));
		Light.ring(level, at, dir, hot(color, 0.35), 0.05, 0.75, 0.05, 6);
		Motes.glows(level, at, 3, 0.15, hot(color, 0.4), 0.06, 10, dir.scale(0.02), 0.01);
	}

	/** Intent takes hold: a slow, low ring of the aura's colour spreading from the feet, faint, like heat over the ground. */
	static void intent(ServerPlayer player, int color) {
		Light.groundRing(player.level(), player.position(), AuraRules.mix(color, 0x000000, 0.35), 0.6, AuraRules.INTENT_RADIUS * 0.85, 0.05, 18);
	}

	/** A creature falters under Intent: a cold shiver of dark motes falling off it. */
	static void falter(ServerLevel level, net.minecraft.world.entity.LivingEntity mob, int color) {
		Vec3 head = mob.position().add(0, mob.getBbHeight() + 0.15, 0);
		Motes.glows(level, head, 3, 0.2, AuraRules.mix(color, 0x1A1424, 0.6), 0.07, 16, new Vec3(0, -0.03, 0), 0.01);
	}

	// ------------------------------------------------------------------ Sovereign: Dominion

	/**
	 * A Dominion raised: a great circle of the aura's colour laid on the ground for its whole length, a ring racing out to its
	 * edge, a column of light at the heart, and motes thrown up round the rim.
	 */
	static void dominionRise(ServerLevel level, Vec3 centre, double radius, int color, int ticks) {
		Vec3 heart = centre.add(0, 1.1, 0);
		Sigils.ground(level, centre, color, hot(color, 0.15), (float) radius, ticks);
		Sigils.layer(level, centre.add(0, 0.09, 0), UP, dev.wildercord.content.SigilOption.BAND, hot(color, 0.3), (float) (radius * 1.04), ticks, 0.01F);
		Light.groundRing(level, centre, hot(color, 0.4), 0.3, radius * 1.15, 0.22, 14);
		Light.groundRing(level, centre, color, 0.3, radius * 1.6, 0.1, 20);
		Light.ray(level, centre, centre.add(0, 9, 0), color, 0.7, 18);
		Light.ray(level, centre, centre.add(0, 8, 0), hot(color, 0.7), 0.1, 14);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.35), 3.4F);
		for (int i = 0; i < 16; i++) {
			double a = Math.PI * 2 * i / 16;
			Vec3 rim = centre.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
			Motes.glow(level, rim, hot(color, 0.3), 0.1, 30, new Vec3(0, 0.06, 0), 0.01);
		}
	}

	/** While a Dominion stands: its rim breathing light every second, and motes rising off the edge, faster as its end nears. */
	static void dominionPulse(ServerLevel level, Vec3 centre, double radius, int color, long age, long left) {
		if (age % 20 == 10) {
			Light.groundRing(level, centre, color, radius * 0.92, radius * 1.02, 0.12, 14);
		}
		if (age % 4 == 0) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 rim = centre.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
			Motes.glow(level, rim, hot(color, 0.25), 0.09, 26, new Vec3(0, 0.05, 0), 0.01);
		}
		if (left > 0 && left <= 40 && left % 10 == 0) {
			// The last two seconds: the circle flickers as it thins.
			Light.groundRing(level, centre, hot(color, 0.5), radius * 0.3, radius, 0.05, 8);
		}
	}

	/** A Dominion ending: the circle breaking up into motes that drift away. */
	static void dominionEnd(ServerLevel level, Vec3 centre, double radius, int color) {
		Light.groundRing(level, centre, hot(color, 0.3), radius, radius * 1.4, 0.08, 10);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			Vec3 rim = centre.add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius);
			Motes.fling(level, rim, new Vec3(Math.cos(a), 0.4, Math.sin(a)), 0.12, hot(color, 0.2), 0.08, 22, new Vec3(0, 0.02, 0));
		}
	}

	/** A blow chaining inside a Dominion: a bolt of the aura's light leaping from one foe to the next. */
	static void dominionChain(ServerLevel level, net.minecraft.world.entity.LivingEntity from, net.minecraft.world.entity.LivingEntity to, int color) {
		Vec3 a = from.getBoundingBox().getCenter();
		Vec3 b = to.getBoundingBox().getCenter();
		Vec3 mid = a.add(b).scale(0.5).add(0, 0.6, 0);
		Light.ray(level, a, mid, color, 0.14, 6);
		Light.ray(level, mid, b, color, 0.14, 6);
		Light.ray(level, a, b, hot(color, 0.7), 0.04, 5);
		Sigils.flash(level, b, 0xFF000000 | hot(color, 0.3), 1.1F);
	}

	// ------------------------------------------------------------------ the spellblade

	/** A spell flowing into the blade: motes of its colour spiralling from the circle into the hand, and a flash on the blade. */
	static void spellDrawn(ServerPlayer player, int spell, int aura) {
		ServerLevel level = player.level();
		Vec3 hand = hand(player);
		Vec3 front = player.getEyePosition().add(player.getViewVector(1.0F).scale(1.4));
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			Vec3 from = front.add(Math.cos(a) * 0.6, Math.sin(a) * 0.6, 0);
			Motes.seek(level, from, hand, i % 2 == 0 ? spell : hot(spell, 0.4), 0.08, 10, 0.6);
		}
		Sigils.flash(level, hand, 0xFF000000 | hot(spell, 0.3), 1.2F);
		Light.ring(level, hand, player.getViewVector(1.0F), aura, 0.05, 0.6, 0.04, 6);
	}

	/** While a spell rides the blade: sparks of its colour running off the weapon. */
	static void spellRiding(ServerPlayer player, int spell) {
		Vec3 hand = hand(player);
		Vec3 up = player.getViewVector(1.0F).scale(0.35).add(0, 0.25, 0);
		Motes.glow(player.level(), hand.add(up), hot(spell, 0.2), 0.06, 10, new Vec3(0, 0.02, 0), 0.02);
	}

	/** A spell slipping off the blade unused: it leaves as cast, with a puff of its colour at the hand. */
	static void spellSlips(ServerPlayer player, int spell) {
		Motes.burst(player.level(), hand(player), 5, spell, 0.07, 10, 0.05);
	}

	/** One step of a slash carrying a spell: a band of the spell's colour along the crescent's leading edge. */
	static void slashCarry(ServerLevel level, Vec3 front, Vec3 aim, Vec3 side, int spell, int tick) {
		double radius = AuraRules.SLASH_WIDTH * 0.62;
		double span = 2 * Math.asin(Math.min(0.99, AuraRules.SLASH_WIDTH / 2 / radius));
		Vec3 normal = UP.add(side.scale(0.75)).normalize();
		Vec3 centre = front.subtract(aim.scale(radius * 0.8));
		Light.slash(level, centre.add(aim.scale(0.24)), normal, aim, spell, radius * 1.02, span * 0.86, 0.2, 1, 5);
		if (tick % 2 == 0) {
			Motes.glows(level, front, 2, AuraRules.SLASH_WIDTH * 0.2, hot(spell, 0.3), 0.08, 12, aim.scale(-0.03), 0.01);
		}
	}

	/** Where the main hand's blade is, near enough, for light that rises off it. */
	private static Vec3 hand(ServerPlayer player) {
		Vec3 look = flat(player);
		Vec3 right = new Vec3(-look.z, 0, look.x);
		double side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
		return player.position().add(0, 1.05, 0).add(look.scale(0.45)).add(right.scale(0.35 * side));
	}

	// ------------------------------------------------------------------ aura marks

	/** An aura mark left on a foe: a small ring of the aura's colour closing on it (the mark's own halo follows). */
	static void marked(ServerLevel level, net.minecraft.world.entity.LivingEntity target, int color) {
		Vec3 at = target.getBoundingBox().getCenter();
		Light.ring(level, at, UP, hot(color, 0.3), 0.9, 0.2, 0.05, 8);
	}

	private static Vec3 flat(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}
}
