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
 * burst of a breakthrough. Built from the mod's shaped light ({@link Light}), physical ground scars, slash trails and motes;
 * the blade's own glow is drawn by each client from the synced look. Kept to a few strong shapes, never a cloud of particles.
 */
public final class AuraVfx {
	private static void pressureCut(ServerLevel level, Vec3 at, Vec3 normal, int color, double from, double to, double width, int life) {
		if (Math.abs(normal.y) > 0.95) AuraFx.groundScar(level, at, Math.max(from, to), Math.max(40, life * 3), 2);
		else Light.slash(level, at, normal, dev.wildercord.cast.ElementFx.perp(normal), color, Math.max(from, to), 1.8, width, 2, life);
	}

	private AuraVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A colour lifted toward white, for a light's hot core. */
	public static int hot(int color, double t) {
		return AuraRules.mix(color, 0xFFFFFF, t);
	}

	/** Each breath of the stance: a small ground impression and a few motes drawn up into the body. */
	static void breathe(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		AuraFx.groundScar(level, feet, Math.max(0.3, 1.5), Math.max(60, (30) * 3), 1);
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
		AuraFx.groundScar(level, player.position(), Math.max(0.4, 2.2), Math.max(60, (14) * 3), 1);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.4), 1.1F);
		Motes.burst(level, heart, 8, hot(color, 0.25), 0.08, 18, 0.06);
	}

	/** The guard rising: a ring of the aura's light braced in front of the blade. */
	static void guard(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		pressureCut(level, at, look, color, 0.2, 0.9, 0.06, 8);
		pressureCut(level, at, look, hot(color, 0.5), 0.1, 0.6, 0.03, 6);
	}

	/** A blow caught on a held guard: a ripple where it met the blade. */
	static void held(ServerPlayer player, int color, DamageSource source) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		pressureCut(level, at, look, color, 0.1, 1.1, 0.05, 6);
		Fx.send(level, ParticleTypes.CRIT, at, 4, 0.2, 0.15);
	}

	/**
	 * A perfect guard: the parry's gold flashing out across the blade's ring, a glint across it and sparks flung off, and a ring
	 * of the aura's own colour inside. Drawn by each client ({@link AuraFx#burst}): everyone else sees it whole where the blade
	 * met the blow; in the guard's own first-person view it's a thin gold glint low across the view, never a flash filling it.
	 */
	static void perfect(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		AuraFx.burst(level, player, at, look, AuraGuard.PERFECT_COLOR, 3.0F, AuraFx.Burst.GUARD | AuraFx.Burst.ECHO);
		AuraFx.burst(level, player, at, look, color, 2.0F, AuraFx.Burst.RING);
		AuraFx.bodyAuraFlare(player, 20, 0.6F);
	}

	/** The slash leaving the blade: a bright crescent at the hand and a flash. */
	static void slashStart(ServerPlayer player, Vec3 origin, Vec3 aim, Vec3 side, int color) {
		slashStart(player.level(), origin, aim, color);
	}

	/** The slash leaving whatever blade loosed it (a player's, a duelist's, a knight's, or a guard sending one back). */
	public static void slashStart(ServerLevel level, Vec3 origin, Vec3 aim, int color) {
		Vec3 side = aim.cross(UP);
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
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
		double width0 = tick == 0 ? 0.75 : 0.66;
		if (brightBehind(level, front)) {
			// Under the light, a rim of shadow a little wider than its halo. Added light alone washes out against a bright sky;
			// the darkness gives the crescent an edge to read by. Only where it's bright behind (by day, under the open sky): in
			// the dark the light reads on its own, and darkness there would only cut black corners out of the night. It's the
			// mod's darkness (as void magic is drawn), so shader packs draw it too.
			Light.slash(level, centre.subtract(aim.scale(0.04)), normal, aim, color | Light.DARK, radius * 1.01, span * 0.97, width0 * 1.15,
				tick == 0 ? 2 : 1, 6);
		}
		// A broad crescent of the aura's colour, a narrower brighter one inside it, and a white-hot edge: it trails a little as it flies.
		Light.slash(level, centre, normal, aim, color, radius, span, width0, tick == 0 ? 2 : 1, 6);
		Light.slash(level, centre.add(aim.scale(0.1)), normal, aim, hot(color, 0.3), radius * 0.97, span * 0.9, 0.34, 1, 5);
		Light.slash(level, centre.add(aim.scale(0.18)), normal, aim, hot(color, 0.85), radius * 0.95, span * 0.82, 0.14, 1, 4);
		if (tick % 3 == 0) {
			Motes.glows(level, front, 2, width * 0.2, hot(color, 0.3), 0.08, 12, aim.scale(-0.02), 0.01);
		}
	}

	/** Whether the sky is bright behind a point: day, in a world with a sky, and nothing overhead. */
	public static boolean brightBehind(ServerLevel level, Vec3 at) {
		return level.isBrightOutside() && level.canSeeSky(net.minecraft.core.BlockPos.containing(at));
	}

	/** The slash breaking on a wall. */
	static void slashEnd(ServerLevel level, Vec3 at, Vec3 aim, int color) {
		Sigils.flash(level, at, 0xFF000000 | color, 1.4F);
		pressureCut(level, at, aim, color, 0.2, 1.4, 0.06, 7);
		Motes.burst(level, at, 6, hot(color, 0.3), 0.08, 14, 0.08);
	}

	/**
	 * Two slashes meeting in the air: a white flash where they meet, a ring of each one's colour racing out across their
	 * path, a ring along the ground between, and their light flung off in sparks.
	 */
	static void clash(ServerLevel level, Vec3 at, Vec3 aim, int a, int b) {
		Vec3 flatAim = new Vec3(aim.x, 0, aim.z);
		flatAim = flatAim.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flatAim.normalize();
		Sigils.flash(level, at, 0xFFFFFFFF, 2.6F);
		pressureCut(level, at, flatAim, a, 0.2, 2.8, 0.1, 10);
		pressureCut(level, at, flatAim, b, 0.3, 3.4, 0.08, 12);
		pressureCut(level, at, UP, hot(AuraRules.mix(a, b, 0.5), 0.5), 0.2, 2.2, 0.06, 9);
		Light.ray(level, at.subtract(0, 0.9, 0), at.add(0, 1.6, 0), 0xFFFFFF, 0.1, 6);
		Motes.burst(level, at, 16, hot(a, 0.3), 0.12, 22, 0.32);
		Motes.burst(level, at, 16, hot(b, 0.3), 0.12, 22, 0.32);
		Fx.send(level, ParticleTypes.CRIT, at, 16, 0.5, 0.45);
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
		AuraFx.groundScar(level, feet, Math.max(0.2, 2.0), Math.max(60, (20) * 3), 1);
		Motes.glows(level, feet.add(0, 0.8, 0), 12, 0.5, hot(color, 0.25), 0.09, 30, new Vec3(0, 0.03, 0), 0.01);
	}

	/** A breakthrough: aura bursting out of the body in its colour, a column of light and a ring racing over the ground. */
	static void breakthrough(ServerPlayer player, int color, int stage) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Vec3 heart = feet.add(0, 1.1, 0);
		double size = 2.5 + stage * 0.8;
		AuraFx.groundScar(level, feet, Math.max(0.3, size * 2), Math.max(60, (16) * 3), 1);
		AuraFx.groundScar(level, feet, Math.max(0.2, size * 1.4), Math.max(60, (12) * 3), 1);
		pressureCut(level, heart, flat(player), color, 0.3, size, 0.08, 12);
		Light.ray(level, feet, feet.add(0, 6 + stage * 2, 0), color, 0.55, 16);
		Light.ray(level, feet, feet.add(0, 5 + stage * 2, 0), hot(color, 0.6), 0.07, 12);
		AuraFx.groundScar(level, feet, Math.max(0.5, size * 1.1), Math.max(60, (18) * 3), 1);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.3), 3.0F);
		Motes.burst(level, heart, 28, hot(color, 0.2), 0.12, 34, 0.28);
		ScreenFx.shake(level, feet, 0.25F, 16);
	}

	// ------------------------------------------------------------------ Form: Aura Step

	/** A step leaving: a ring snapping out at the feet and a few motes flung back the way it came. */
	static void stepStart(ServerLevel level, Vec3 feet, Vec3 dir, int color) {
		AuraFx.groundScar(level, feet, Math.max(0.2, 1.6), Math.max(60, (8) * 3), 1);
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
		AuraFx.groundScar(level, feet, Math.max(0.3, 1.3), Math.max(60, (7) * 3), 1);
		Motes.burst(level, feet.add(0, 0.9, 0), 6, hot(color, 0.3), 0.07, 12, 0.06);
	}

	/** A step with nowhere to go: the aura bumping the wall in front, and nothing more. */
	static void stepBlocked(ServerPlayer player, int color, Vec3 dir) {
		Vec3 at = player.position().add(0, 1.0, 0).add(dir.scale(0.6));
		pressureCut(player.level(), at, dir, color, 0.1, 0.7, 0.04, 5);
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
		pressureCut(level, at, dir, hot(color, 0.35), 0.05, 0.75, 0.05, 6);
		Motes.glows(level, at, 3, 0.15, hot(color, 0.4), 0.06, 10, dir.scale(0.02), 0.01);
	}

	/** Intent takes hold: a slow, low ring of the aura's colour spreading from the feet, faint, like heat over the ground. */
	static void intent(ServerPlayer player, int color) {
		AuraFx.groundScar(player.level(), player.position(), Math.max(0.6, AuraRules.INTENT_RADIUS * 0.85), Math.max(60, (18) * 3), 1);
	}

	/** A creature falters under Intent: a cold shiver of dark motes falling off it. */
	static void falter(ServerLevel level, net.minecraft.world.entity.LivingEntity mob, int color) {
		Vec3 head = mob.position().add(0, mob.getBbHeight() + 0.15, 0);
		Motes.glows(level, head, 3, 0.2, AuraRules.mix(color, 0x1A1424, 0.6), 0.07, 16, new Vec3(0, -0.03, 0), 0.01);
	}

	// ------------------------------------------------------------------ Sovereign: Dominion

	/**
	 * A Dominion raised: fractured ground and a ragged impact depression for its territory.
	 */
	static void dominionRise(ServerLevel level, Vec3 centre, double radius, int color, int ticks) {
		dominionRise(level, centre, radius, color, ticks, true);
	}

	/**
	 * A Dominion rising: ordinary fractures, or the awakened method's physical ground motif, with scattered floor debris.
	 */
	static void dominionRise(ServerLevel level, Vec3 centre, double radius, int color, int ticks, boolean circle) {
		AuraFx.groundScar(level, centre, radius, ticks, circle ? 0 : 1);
		AuraFx.groundScar(level, centre, radius * 1.15, 60, 1);
	}

	/** While a Dominion stands, renew the fractured ground defining its territory. */
	static void dominionPulse(ServerLevel level, Vec3 centre, double radius, int color, long age, long left) {
		if (age % 40 == 10) AuraFx.groundScar(level, centre, radius, 60, 0);
	}

	/** A Dominion ending: a last ground scar and scattered fragments. */
	static void dominionEnd(ServerLevel level, Vec3 centre, double radius, int color) {
		AuraFx.groundScar(level, centre, Math.max(radius, radius * 1.4), Math.max(60, (10) * 3), 1);
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
		pressureCut(level, hand, player.getViewVector(1.0F), aura, 0.05, 0.6, 0.04, 6);
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
		pressureCut(level, at, UP, hot(color, 0.3), 0.9, 0.2, 0.05, 8);
	}

	// ------------------------------------------------------------------ sword strings: the placeholder arts

	/*
	 * By day, under the open sky, added light alone washes out: each placeholder art lays a rim of the mod's darkness a little
	 * wider than its light under it (as the slash's crescent does), so it reads against a bright sky or pale stone too. In the
	 * dark the light reads on its own.
	 *
	 * Shaped light opening within a block and a quarter of someone's eyes isn't sent to them (Sigils.send: in first person it
	 * would fill the screen), so every crescent here is centred out in front of the swordsman, never on their body, and reaches
	 * back toward them from there. Beams start at them freely; circles on the ground at their feet show to everyone else, and to
	 * the swordsman only standing (crouching brings the eyes within reach of them).
	 */

	/**
	 * A placeholder art's arc (the First and Second Arts): a wide crescent of the aura's colour cut in front at the height of the
	 * blow, white-hot at its edge, sparks thrown off its tips; {@code rising}, a crescent cut upward across the foes in front,
	 * from low on the off hand's side to high on the blade's, facing the swordsman (so it reads from behind and in first person
	 * alike), for the art that lifts.
	 */
	static void artArc(ServerPlayer player, int color, boolean rising) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 feet = player.position();
		boolean day = brightBehind(level, feet.add(0, 1.5, 0).add(look.scale(2)));
		if (rising) {
			// A crescent sweeps the way its plane's angles climb: facing the swordsman that's from their left, over the top, to
			// their right, so a right-handed cut faces them and a left-handed one faces away, each starting low on the off hand's
			// side and ending high on the blade's.
			// Small enough that its top stays under a standing swordsman's eyes: in first person it rises across the lower view.
			Vec3 centre = feet.add(0, 0.3, 0).add(look.scale(2.2));
			boolean righty = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
			Vec3 offHand = righty ? new Vec3(look.z, 0, -look.x) : new Vec3(-look.z, 0, look.x);
			Vec3 facing = righty ? look : look.scale(-1);
			Vec3 toward = UP.scale(0.87).add(offHand.scale(0.5)).normalize();
			if (day) {
				Light.slash(level, centre.subtract(look.scale(0.03)), facing, toward, color | Light.DARK, 1.18, 2.0, 0.6, 1, 9);
			}
			Light.slash(level, centre, facing, toward, color, 1.15, 2.1, 0.5, 1, 9);
			Light.slash(level, centre.subtract(look.scale(0.04)), facing, toward, hot(color, 0.6), 1.11, 1.8, 0.15, 1, 8);
			Motes.glows(level, centre.add(0, 1.0, 0), 6, 0.5, hot(color, 0.3), 0.08, 16, new Vec3(0, 0.06, 0), 0.01);
		} else {
			// Its circle's centre a block and a half ahead, so the crescent opens round the foes in front, its tips beside you.
			Vec3 centre = feet.add(0, 0.85, 0).add(look.scale(1.4));
			double r = StringRules.ARC_REACH - 1.4;
			double span = Math.toRadians(StringRules.ARC_DEGREES + 40);
			if (day) {
				Light.slash(level, centre.subtract(0, 0.02, 0), UP, look, color | Light.DARK, r * 1.04, span * 0.97, 0.7, 1, 9);
			}
			Light.slash(level, centre, UP, look, color, r, span, 0.6, 1, 9);
			Light.slash(level, centre.add(0, 0.03, 0), UP, look, hot(color, 0.6), r * 0.96, span * 0.85, 0.16, 1, 8);
			for (int tip = -1; tip <= 1; tip += 2) {
				double a = Math.atan2(look.z, look.x) + tip * span / 2;
				Vec3 end = centre.add(Math.cos(a) * r, 0, Math.sin(a) * r);
				Motes.fling(level, end, new Vec3(Math.cos(a), 0.2, Math.sin(a)), 0.12, hot(color, 0.3), 0.08, 12, new Vec3(0, -0.01, 0));
			}
		}
		Sigils.flash(level, feet.add(0, 0.9, 0).add(look.scale(1.8)), 0xFF000000 | hot(color, 0.3), 1.0F);
	}

	/**
	 * The Third Art, a counter: the parry's gold ring snapping out round the foe ahead, kept small and low enough to leave a
	 * first-person view clear, and the aura's crescent cut through it.
	 */
	static void artCounter(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 0.9, 0).add(look.scale(2.0));
		boolean day = brightBehind(level, at);
		Vec3 side = look.cross(UP).normalize();
		Vec3 normal = UP.add(side.scale(0.9)).normalize();
		Vec3 centre = at.subtract(look.scale(0.1));
		if (day) {
			pressureCut(level, at.subtract(look.scale(0.03)), look, AuraGuard.PERFECT_COLOR | Light.DARK, 0.15, 1.2, 0.12, 8);
			Light.slash(level, centre.subtract(look.scale(0.03)), normal, look, color | Light.DARK, 1.25, 2.5, 0.5, 1, 7);
		}
		pressureCut(level, at, look, AuraGuard.PERFECT_COLOR, 0.15, 1.15, 0.09, 8);
		Light.slash(level, centre, normal, look, color, 1.2, 2.6, 0.44, 1, 7);
		Light.slash(level, centre.add(look.scale(0.04)), normal, look, hot(color, 0.7), 1.16, 2.2, 0.14, 1, 6);
		Sigils.flash(level, at, 0xFF000000 | AuraGuard.PERFECT_COLOR, 1.0F);
	}

	/** The Fourth Art: a line of the aura's light cut low and straight ahead, {@code length} blocks, white-hot down its middle. */
	static void artLine(ServerPlayer player, int color, Vec3 ahead, double length) {
		ServerLevel level = player.level();
		Vec3 from = player.position().add(0, 0.95, 0).add(ahead.scale(0.6));
		Vec3 to = from.add(ahead.scale(length));
		if (brightBehind(level, to)) {
			Light.ray(level, from, to, color | Light.DARK, 0.44, 8);
			Light.ray(level, from.subtract(0, 0.85, 0), to.subtract(0, 0.85, 0), color | Light.DARK, 0.26, 10);
		}
		Light.ray(level, from, to, color, 0.36, 8);
		Light.ray(level, from, to, hot(color, 0.6), 0.09, 6);
		Light.ray(level, from.subtract(0, 0.85, 0), to.subtract(0, 0.85, 0), color, 0.2, 10);
		pressureCut(level, to, ahead, hot(color, 0.3), 0.2, 1.2, 0.07, 7);
		Motes.glows(level, to, 5, 0.4, hot(color, 0.3), 0.08, 14, ahead.scale(0.03), 0.01);
	}

	/**
	 * The Final Art: a ring of the aura racing out over the ground, a second inside it, six cuts whirling round at the height of
	 * the blow (each centred out toward the ring, so they show to the swordsman too) and motes flung up off the rim.
	 */
	static void artRing(ServerPlayer player, int color, double radius) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Vec3 look = flat(player);
		boolean day = brightBehind(level, feet.add(0, 1, 0));
		if (day) {
			AuraFx.groundScar(level, feet.subtract(0, 0.01, 0), Math.max(0.4, radius * 1.18), Math.max(60, (12) * 3), 1);
		}
		AuraFx.groundScar(level, feet, Math.max(0.4, radius * 1.15), Math.max(60, (12) * 3), 1);
		AuraFx.groundScar(level, feet, Math.max(0.3, radius), Math.max(60, (10) * 3), 1);
		double base = Math.atan2(look.z, look.x);
		for (int i = 0; i < 6; i++) {
			double a = base + Math.PI * 2 * i / 6;
			Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
			Vec3 centre = feet.add(0, 0.9, 0).add(out.scale(radius - 1.5));
			// Each cut sweeps on round the circle, the way the whirl turns, overlapping the next into a ring of cuts.
			Vec3 toward = out.add(new Vec3(-out.z, 0, out.x).scale(0.5)).normalize();
			if (day) {
				Light.slash(level, centre.subtract(0, 0.02, 0), UP, toward, color | Light.DARK, 1.55, 2.1, 0.5, 2, 9);
			}
			Light.slash(level, centre, UP, toward, color, 1.5, 2.2, 0.42, 2, 9);
			Light.slash(level, centre.add(0, 0.03, 0), UP, toward, hot(color, 0.6), 1.46, 1.8, 0.12, 2, 8);
		}
		Sigils.flash(level, feet.add(0, 0.3, 0).add(look.scale(1.6)), 0xFF000000 | hot(color, 0.35), 2.0F);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			Vec3 rim = feet.add(Math.cos(a) * radius * 0.85, 0.3, Math.sin(a) * radius * 0.85);
			Motes.fling(level, rim, new Vec3(Math.cos(a), 0.6, Math.sin(a)), 0.15, hot(color, 0.25), 0.09, 20, new Vec3(0, 0.02, 0));
		}
		ScreenFx.shake(level, feet, 0.18F, 10);
	}

	private static Vec3 flat(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}
}
