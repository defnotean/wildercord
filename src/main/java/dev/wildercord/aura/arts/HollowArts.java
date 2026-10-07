package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hollow Breath's arts: drawing in, silencing, stepping through. Hollow takes the space between a swordsman and their foes away: it
 * draws foes to the blade and gathers crowds into one point for the next cut, swallows a blow and silences whoever struck, and steps
 * through a rift to the far side of a foe. What it strikes is left shadowed (a life spell on it sets off Blight). Its light is
 * darkness rimmed in violet, falling inward.
 * <ul>
 * <li><b>Void Cut</b> (I): a cut that tears a hole in the air at the blade's end and draws the foes in front to your feet,
 * shadowed and off balance.</li>
 * <li><b>Collapse</b> (II): a falling cut that opens a well in the ground ahead, dragging everything near into it, then collapsing
 * on everything it gathered.</li>
 * <li><b>Null Parry</b> (III): the caught blow swallowed by the void; whoever struck is cut and silenced (no spells, no arts, no
 * Aura key but the guard for a player; no spells, bow or fuse for a creature), the foes near shoved off.</li>
 * <li><b>Rift Step</b> (IV): through a rift and out of another past the foe ahead, cutting it as you pass; the rifts' edges drag in
 * and cut whoever stands near them.</li>
 * <li><b>Event Horizon</b> (V): a black sphere cut into the air ahead that drags in everything near, grinds whatever it holds, and
 * then crushes it all.</li>
 * </ul>
 * A boss is never drawn, dragged or silenced: it's only slowed, and what it was winding up broken.
 */
public final class HollowArts {
	private HollowArts() {}

	public static final String METHOD = "hollow";
	public static final String VOID_CUT = "void_cut";
	public static final String COLLAPSE = "collapse";
	public static final String NULL_PARRY = "null_parry";
	public static final String RIFT_STEP = "rift_step";
	public static final String EVENT_HORIZON = "event_horizon";

	/** The kinds of field Hollow leaves (for the tests). */
	public static final String WELL = "hollow_well";
	public static final String HORIZON = "hollow_horizon";

	/** The void palette's lilac, and its abyss drawn as darkness. */
	private static final int LILAC = 0xE0B0FF;
	private static final int ABYSS = 0x1A0830 | ArtLight.DARK;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, VOID_CUT, HollowArts::voidCut),
			MethodArts.art(AuraApi.ArtSlot.SECOND, COLLAPSE, HollowArts::collapse),
			MethodArts.art(AuraApi.ArtSlot.THIRD, NULL_PARRY, HollowArts::nullParry),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, RIFT_STEP, HollowArts::riftStep),
			MethodArts.art(AuraApi.ArtSlot.FINAL, EVENT_HORIZON, HollowArts::eventHorizon));
	}

	// ------------------------------------------------------------------ the look they share

	/** A hole in the air: a black core wrapped in thin violet rims, darkness falling in on it. */
	static void hole(ArtLight light, Vec3 at, double radius, int color, int life) {
		light.bare().orb(at, ABYSS, radius, Math.min(4, life));
		light.bare().ring(at, ElementFx.tilted(1.15, at.x * 3.1 + at.z), color, radius * 1.45, radius * 1.35, 0.03, life);
		light.bare().ring(at, ElementFx.tilted(1.15, at.x * 3.1 + at.z + Math.PI / 2), LILAC, radius * 1.5, radius * 1.4, 0.02, life);
	}

	/** Darkness drawn from {@code from} toward {@code to}: a dark tendril with a thin violet thread down it. */
	static void tendril(ArtLight light, Vec3 from, Vec3 to, int color, int life) {
		light.ray(from, to, ABYSS, 0.16, life);
		light.bare().ray(from, to, color, 0.04, life);
	}

	/** Struck by the void: shadowed for a life spell's Blight, and darkness folding in on it. */
	static void shadow(ServerPlayer player, LivingEntity foe, double size) {
		Reactions.mark(foe, Reactions.Mark.SHADOWED, 80);
		AuraPhysicalFx.voidImpact(player.level(), foe.getBoundingBox().getCenter(), size);
	}

	/**
	 * Draws {@code foe} in to {@code to} over {@code ticks}: a creature carried a stretch each tick (stopping at anything solid), a
	 * player given one gentle pull ({@link ArtKit#pull}, held to their throw), a boss left be.
	 */
	static void pullIn(ServerPlayer player, LivingEntity foe, Vec3 to, int ticks, int color) {
		if (ArtKit.boss(foe) || !foe.isAlive()) {
			return;
		}
		if (foe instanceof Player) {
			ArtKit.pull(foe, to, ArtRules.VOID_DRAW);
			return;
		}
		ServerLevel level = player.level();
		for (int t = 1; t <= ticks; t++) {
			int step = t;
			Scheduler.later(t, () -> {
				if (!foe.isAlive() || foe.level() != level || !player.isAlive()) {
					return;
				}
				Vec3 at = foe.position();
				Vec3 goal = new Vec3(to.x, at.y, to.z);
				Vec3 next = at.lerp(goal, 1.0 / (ticks - step + 1));
				AABB body = foe.getBoundingBox().move(next.subtract(at));
				if (next.distanceToSqr(at) < 1.0E-4 || !level.noCollision(foe, body)) {
					return;
				}
				foe.teleportTo(next.x, next.y, next.z);
				foe.setDeltaMovement(Vec3.ZERO);
				foe.needsSync = true;
				tendril(ArtLight.world(player), next.add(0, foe.getBbHeight() * 0.5, 0), to.add(0, 0.9, 0), color, 3);
				Vfx.emit(level, ParticleTypes.PORTAL, next.add(0, foe.getBbHeight() * 0.5, 0), 3, 0.2, 0.3);
			});
		}
	}

	// ------------------------------------------------------------------ I. Void Cut

	static boolean voidCut(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT, true, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_void_cut", 1.0F, 1.0F);
		// The tear at the blade's end: a hole in the air with darkness falling into it, at the height of the cut for everyone else, and
		// low and small in your own view (where the foes it draws come to rest). The dark crescent of the cut is seen from outside (in
		// your own view it would cut across the middle).
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		Vec3 tear = feet.add(0, 1.05, 0).add(look.scale(2.0));
		Vec3 low = feet.add(0, 0.35, 0).add(look.scale(2.2));
		for (int t = 0; t < 10; t += 3) {
			int tick = t;
			Scheduler.later(1 + t, () -> {
				hole(show, tear, 0.28 + 0.04 * tick, color, 4);
				show.shade(tear, ABYSS, (float) (1.2 + 0.1 * tick));
				hole(world, low, 0.1 + 0.015 * tick, color, 4);
			});
		}
		AuraPhysicalFx.implode(level, low, 1.4, 8);
		Vec3 centre = feet.add(0, 1.0, 0).add(look.scale(1.2));
		Vec3 normal = ArtKit.UP.add(ArtKit.bladeSide(player, look).scale(-0.35)).normalize();
		show.slash(centre, normal, look, ABYSS, 2.0, 2.4, 0.5, 1, 9);
		show.bare().slash(centre.add(0, 0.03, 0), normal, look, color, 1.96, 2.1, 0.14, 1, 8);
		List<LivingEntity> foes = ArtKit.arc(player, context.struck(), ArtRules.VOID_REACH, ArtRules.VOID_DEGREES, ArtRules.VOID_TARGETS);
		Vec3 side = ArtKit.right(look);
		for (int i = 0; i < foes.size(); i++) {
			LivingEntity foe = foes.get(i);
			hits.strike(foe, ArtRules.VOID_FACTOR, i == 0 ? AuraFxRules.Weight.HEAVY : AuraFxRules.Weight.FULL);
			if (!foe.isAlive()) {
				continue;
			}
			shadow(player, foe, 0.7);
			// Drawn in to your feet, side by side, then slowed a moment as they come.
			double offset = (i - (foes.size() - 1) / 2.0) * 1.0;
			Vec3 stop = feet.add(look.scale(ArtRules.VOID_STOP)).add(side.scale(offset));
			if (foe.position().distanceTo(feet) > ArtRules.VOID_STOP + 0.4) {
				pullIn(player, foe, stop, 4, color);
			}
			ArtKit.slow(player, foe, ArtRules.VOID_SLOW + 4, 1);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Collapse

	static boolean collapse(ServerPlayer player, AuraApi.StringContext context) {
		ReleasedArtOwner released = ReleasedArtOwner.capture(player);
		ServerLevel level = released.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_collapse", 1.1F, 1.0F);
		Vec3 ahead = feet.add(look.scale(ArtRules.COLLAPSE_AHEAD));
		Vec3 ground = ArtKit.floor(level, ahead.add(0, 1, 0), 1.5, 3);
		Vec3 well = ground == null ? ahead : ground;
		Vec3 core = well.add(0, 0.55, 0);
		ArtLight world = ArtLight.world(player);
		// A seal of darkness spinning fast on the ground round the well, a violet ring round it turning the other way.
		world.bare().ground(well, SigilOption.BAND, ABYSS, ArtRules.COLLAPSE_PULL * 0.75, ArtRules.COLLAPSE_TICKS + 8, 0.25);
		world.ground(well, SigilOption.BAND, color, ArtRules.COLLAPSE_PULL, ArtRules.COLLAPSE_TICKS + 8, -0.18);
		ArtFields.openReleased(player, released, WELL, ArtFields.disc(() -> well, ArtRules.COLLAPSE_PULL, 2.5), ArtRules.COLLAPSE_TICKS, 1, (field, owner, age) -> {
			ServerLevel lv = field.level();
			ArtLight w = ArtLight.world(owner);
			int c = ArtKit.color(owner);
			double grow = Math.min(1.0, age / (double) ArtRules.COLLAPSE_TICKS);
			if (age % 3 == 1) {
				hole(w, core, 0.2 + 0.35 * grow, c, 4);
				w.shade(core, ABYSS, (float) (0.9 + 1.2 * grow));
			}
			if (age % 4 == 1) {
				// Darkness falling in from the rim, over and over.
				w.bare().groundRing(well, ABYSS, ArtRules.COLLAPSE_PULL, 0.2, 0.12, 6);
				w.bare().groundRing(well, c, ArtRules.COLLAPSE_PULL * 1.05, 0.25, 0.03, 6);
				Vfx.emit(lv, ParticleTypes.PORTAL, core, 10, 0.2, ArtRules.COLLAPSE_PULL * 0.5);
			}
			if (age % 2 == 0) {
				for (LivingEntity foe : field.foes(owner)) {
					ArtKit.drag(foe, well, ArtRules.COLLAPSE_DRAG, 0.5);
					if (age % 4 == 0) {
						tendril(w, foe.getBoundingBox().getCenter(), core, c, 3);
					}
				}
			}
			if (age >= ArtRules.COLLAPSE_TICKS) {
				// It collapses on everything it gathered.
				w.flash(core, LILAC, 2.4F);
				w.bare().groundRing(well, ABYSS, 0.2, ArtRules.COLLAPSE_RADIUS * 1.6, 0.3, 8);
				w.groundRing(well, c, 0.3, ArtRules.COLLAPSE_RADIUS * 1.4, 0.12, 10);
				w.bare().ring(core, ElementFx.tilted(1.2, age), LILAC, 0.2, ArtRules.COLLAPSE_RADIUS, 0.05, 8);
				Vfx.radial(lv, ParticleTypes.REVERSE_PORTAL, core, 24, 0.25);
				ScreenFx.shake(lv, well, 0.25F, 10);
				AuraFx.sound(owner, AuraFx.Sound.IMPACT, 0.9F, 0.7F);
				for (LivingEntity foe : ArtKit.around(owner, well, ArtRules.COLLAPSE_RADIUS, 1.5, 3.0, ArtRules.COLLAPSE_TARGETS)) {
					double d = foe.position().subtract(well).horizontalDistance();
					hits.strike(foe, ArtRules.falloff(ArtRules.COLLAPSE_CENTRE, ArtRules.COLLAPSE_EDGE, d, ArtRules.COLLAPSE_RADIUS), AuraFxRules.Weight.HEAVY);
					if (!field.active()) break;
					if (foe.isAlive()) {
						shadow(owner, foe, 0.6);
						// The collapse leaves them where it gathered them, for the next cut.
						ArtKit.steady(foe);
					}
				}
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ III. Null Parry

	static boolean nullParry(ServerPlayer player, AuraApi.StringContext context) {
		var counter = dev.wildercord.aura.MastersArts.earnedCounter(player);
		if (counter == null || !counter.art().equals(NULL_PARRY) || !counter.valid()) return false;
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		LivingEntity foe = counter.target();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.3F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_null_parry", 1.0F, 1.0F);
		// The blow swallowed: a disc of darkness opening at the blade and snapping shut (seen from outside; through your own eyes a dark
		// ring low in the view).
		Vec3 hand = ArtKit.hand(player);
		ArtLight show = ArtLight.spectacle(player);
		Vec3 look = ArtKit.flat(player);
		for (int t = 0; t < 6; t += 2) {
			int tick = t;
			Scheduler.later(t, () -> {
				double r = tick < 3 ? 0.55 : 0.25;
				show.bare().orb(hand.add(look.scale(0.4)), ABYSS, r, 3);
				show.bare().ring(hand.add(look.scale(0.4)), look, LILAC, r * 1.5, r * 1.1, 0.03, 4);
			});
		}
		AuraFx.burst(level, player, hand, look, color, 1.4F, AuraFx.Burst.RING | AuraFx.Burst.FLASH);
		// A pulse of the void off you: the foes near shoved off and nicked.
		ArtLight world = ArtLight.world(player);
		world.bare().groundRing(feet, ABYSS, 0.4, ArtRules.NULL_PULSE * 1.2, 0.2, 8);
		world.groundRing(feet, color, 0.3, ArtRules.NULL_PULSE, 0.06, 8);
		for (LivingEntity other : ArtKit.around(player, feet, ArtRules.NULL_PULSE, 1.0, 2.5, 6)) {
			if (other != foe && counter.permits(other)) {
				hits.strike(other, ArtRules.NULL_PULSE_FACTOR, AuraFxRules.Weight.LIGHT);
				if (!counter.afterDamage(other)) {
					if (!counter.valid()) return true;
					continue;
				}
				ArtKit.knock(other, feet, ArtRules.NULL_SHOVE, 0.15);
			}
		}
		if (foe != null && counter.primaryValid()) {
			hits.strike(foe, ArtRules.NULL_FACTOR);
			if (foe.isAlive() && counter.afterDamage(foe)) {
				shadow(player, foe, 0.9);
				if (!counter.afterDamage(foe)) return true;
				ArtKit.hold(player, foe, ArtRules.NULL_HOLD);
				if (!counter.afterDamage(foe)) return true;
				int silenced = ArtWards.silence(foe, ArtRules.NULL_SILENCE, () -> counter.afterDamage(foe));
				if (counter.afterDamage(foe)) silencedLook(player, foe, Math.max(10, silenced));
			}
		}
		return true;
	}

	/** Silenced: a crown of darkness over its head and darkness drawn out of its mouth, while it lasts (and the cast lock's own halo). */
	static void silencedLook(ServerPlayer player, LivingEntity foe, int ticks) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		for (int t = 0; t < ticks; t += 10) {
			Scheduler.later(t, () -> {
				if (!foe.isAlive() || !player.isAlive()) {
					return;
				}
				Vec3 head = foe.position().add(0, foe.getBbHeight() + 0.25, 0);
				double r = Math.max(0.35, foe.getBbWidth() * 0.6);
				ArtLight world = ArtLight.world(player);
				world.bare().ring(head, ArtKit.UP, ABYSS, r, r * 0.8, 0.06, 11);
				world.bare().ring(head.add(0, 0.02, 0), ArtKit.UP, color, r * 1.08, r * 1.0, 0.02, 11);
				Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, foe.getEyePosition(), 2, 0.1, 0.02);
			});
		}
		Feels.sound(level, foe.position(), "void_hush_dome", 0.6F, 1.2F);
	}

	// ------------------------------------------------------------------ IV. Rift Step

	static boolean riftStep(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 from = player.position();
		// The foe ahead to pass through: the nearest in front and in sight.
		List<LivingEntity> ahead = new ArrayList<>(ArtKit.arc(player, null, ArtRules.RIFT_REACH, ArtRules.RIFT_DEGREES, 8));
		ahead.removeIf(e -> !player.hasLineOfSight(e));
		ahead.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		LivingEntity foe = null;
		Vec3 spot = null;
		for (LivingEntity e : ahead) {
			Vec3 s = ArtKit.behind(player, e, ArtRules.RIFT_BEHIND);
			if (s != null) {
				foe = e;
				spot = s;
				break;
			}
		}
		if (foe == null) {
			MethodArts.blocked(player, RIFT_STEP);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_rift_step", 1.05F, 1.0F);
		// A rift torn where you stood and another past the foe: you go in one and come out of the other, cutting it as you pass.
		rift(player, from, look, color, 30);
		rift(player, spot, look, color, 30);
		Vec3 dir = spot.subtract(from);
		dir = new Vec3(dir.x, 0, dir.z);
		dir = dir.lengthSqr() < 1.0E-4 ? look : dir.normalize();
		ArtKit.blink(player, spot);
		AuraStep.afterimages(player, from, spot, dir, color);
		ArtLight world = ArtLight.world(player);
		tendril(world, from.add(0, 1.0, 0), spot.add(0, 1.0, 0), color, 8);
		fx.trail(AuraFxRules.Stroke.CUT, true, 1.4F);
		hits.strike(foe, ArtRules.RIFT_FACTOR);
		if (foe.isAlive()) {
			shadow(player, foe, 1.0);
			ArtKit.slow(player, foe, ArtRules.RIFT_SLOW, 1);
		}
		// The rifts' edges: those near either one dragged in and cut.
		for (Vec3 mouth : List.of(from, spot)) {
			for (LivingEntity other : ArtKit.around(player, mouth, ArtRules.RIFT_EDGE, 1.0, 2.5, ArtRules.RIFT_EDGE_TARGETS)) {
				if (other == foe || hits.hurt(other)) {
					continue;
				}
				hits.strike(other, ArtRules.RIFT_EDGE_FACTOR, AuraFxRules.Weight.FULL);
				ArtKit.pull(other, mouth, ArtRules.RIFT_DRAW);
				ArtKit.slow(player, other, ArtRules.RIFT_SLOW, 0);
				tendril(world, other.getBoundingBox().getCenter(), mouth.add(0, 1.0, 0), color, 6);
			}
		}
		return true;
	}

	/**
	 * A rift standing at {@code feet}: a tall lens of darkness facing along {@code look}, rimmed in violet, open a while and closing
	 * (seen from outside; it stands where its swordsman goes in and comes out, round their own body).
	 */
	static void rift(ServerPlayer player, Vec3 feet, Vec3 look, int color, int ticks) {
		ArtLight show = ArtLight.spectacle(player);
		Vec3 side = ArtKit.right(look);
		Vec3 mid = feet.add(0, 1.0, 0);
		for (int t = 0; t < ticks; t += 5) {
			int tick = t;
			Scheduler.later(t, () -> {
				double open = tick < ticks - 8 ? 1.0 : 0.5;
				// Two crescents in the upright plane facing the way you go, bulging apart: a lens of darkness.
				for (int k = -1; k <= 1; k += 2) {
					show.bare().slash(mid.add(side.scale(-k * 0.55 * open)), look, side.scale(k), ABYSS, 1.2, 1.9, 0.32 * open, 1, 6);
					show.bare().slash(mid.add(side.scale(-k * 0.55 * open)), look, side.scale(k), k < 0 ? color : LILAC, 1.24, 1.8, 0.05, 1, 6);
				}
			});
		}
		Vfx.emit(player.level(), ParticleTypes.PORTAL, mid, 12, 0.3, 0.5);
		Feels.sound(player.level(), mid, "void_unzip", 0.6F, 1.1F);
	}

	// ------------------------------------------------------------------ V. Event Horizon

	static boolean eventHorizon(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CROSS, false, 1.7F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_event_horizon", 1.3F, 1.0F);
		Vec3 ahead = feet.add(look.scale(ArtRules.HORIZON_AHEAD));
		Vec3 ground = ArtKit.floor(level, ahead.add(0, 1, 0), 1.5, 3);
		Vec3 base = ground == null ? ahead : ground;
		Vec3 heart = base.add(0, ArtRules.HORIZON_HEIGHT, 0);
		// Low at its foot, for everyone (and the only part of the sphere in your own first person, under the middle of the view): a
		// small black core and a seal of darkness on the ground.
		Vec3 low = base.add(0, 0.45, 0);
		ArtLight world = ArtLight.world(player);
		world.bare().ground(base, SigilOption.BAND, ABYSS, ArtRules.HORIZON_PULL * 0.7, ArtRules.HORIZON_TICKS + 10, 0.2);
		world.ground(base, SigilOption.BAND, color, ArtRules.HORIZON_PULL, ArtRules.HORIZON_TICKS + 10, -0.12);
		RandomSource r = level.getRandom();
		ArtFields.open(player, HORIZON, ArtFields.disc(() -> base, ArtRules.HORIZON_PULL, 3.5), ArtRules.HORIZON_TICKS, 1, (field, owner, age) -> {
			ServerLevel lv = field.level();
			ArtLight w = ArtLight.world(owner);
			ArtLight show = ArtLight.spectacle(owner);
			int c = ArtKit.color(owner);
			double grow = Math.min(1.0, age / 8.0);
			if (age % 3 == 1 && age < ArtRules.HORIZON_TICKS) {
				// The sphere (seen from outside): a hole in the world, an accretion ring of violet light wheeling round it.
				double radius = 0.4 + 0.9 * grow;
				// A soft disc of darkness for its body (smooth at its edge), a smaller hard core in it, and the accretion rings.
				show.shade(heart, ABYSS, (float) (radius * 2.6));
				show.bare().orb(heart, ABYSS, radius * 0.7, 4);
				show.bare().ring(heart, ElementFx.tilted(1.3, age * 0.15), c, radius * 2.1, radius * 1.5, 0.06, 4);
				show.bare().ring(heart, ElementFx.tilted(1.1, age * 0.15 + 1.7), LILAC, radius * 1.8, radius * 1.4, 0.03, 4);
				show.flash(heart, c, (float) (radius * 1.2));
				hole(w, low, 0.2 + 0.12 * grow, c, 4);
			}
			if (age % 4 == 2) {
				Vfx.emit(lv, ParticleTypes.PORTAL, heart, 14, 0.3, ArtRules.HORIZON_PULL * 0.4);
				w.bare().groundRing(base, ABYSS, ArtRules.HORIZON_PULL, 0.4, 0.1, 6);
			}
			if (age % 2 == 0) {
				for (LivingEntity foe : field.foes(owner)) {
					ArtKit.drag(foe, base, ArtRules.HORIZON_DRAG, 0.8);
					if (age % 6 == 0) {
						tendril(w, foe.getBoundingBox().getCenter(), heart, c, 4);
					}
				}
			}
			if (age % ArtRules.HORIZON_GRIP_PERIOD == 0 && age < ArtRules.HORIZON_TICKS) {
				// Whatever it holds, ground in its grip.
				for (LivingEntity foe : ArtKit.around(owner, base, ArtRules.HORIZON_CORE, 1.5, 3.5, ArtRules.HORIZON_TARGETS)) {
					hits.strike(foe, ArtRules.HORIZON_GRIP, AuraFxRules.Weight.LIGHT);
					if (foe.isAlive()) {
						foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, foe instanceof Player ? 1 : 3, false, true), owner);
						Reactions.mark(foe, Reactions.Mark.SHADOWED, 60);
						Vfx.emit(lv, ParticleTypes.REVERSE_PORTAL, foe.getBoundingBox().getCenter(), 3, 0.25, 0.05);
					}
				}
			}
			if (age % 20 == 10) {
				Feels.sound(lv, heart, "void_hum", 0.5F, 0.7F);
			}
			if (age >= ArtRules.HORIZON_TICKS) {
				crush(owner, hits, base, heart, c, r);
			}
		});
		return true;
	}

	/** The sphere falling in on itself and crushing everything it holds: darkness rushing in, then a violet shockwave out. */
	private static void crush(ServerPlayer player, ArtKit.Hits hits, Vec3 base, Vec3 heart, int color, RandomSource r) {
		ServerLevel level = player.level();
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		show.bare().orb(heart, ABYSS, 1.6, 3);
		for (int k = 0; k < 3; k++) {
			show.bare().ring(heart, ElementFx.randomDir(r), LILAC, 2.4, 0.1, 0.06, 5 + k);
		}
		Scheduler.later(3, () -> {
			world.flash(heart, LILAC, 4.0F);
			world.bare().groundRing(base, ABYSS, 0.3, ArtRules.HORIZON_CRUSH_RADIUS * 1.6, 0.4, 12);
			world.groundRing(base, color, 0.4, ArtRules.HORIZON_CRUSH_RADIUS * 1.5, 0.16, 12);
			world.groundRing(base, LILAC, 0.3, ArtRules.HORIZON_CRUSH_RADIUS * 1.2, 0.05, 10);
			show.ring(heart, ArtKit.UP.add(0.2, 0, 0.2).normalize(), color, 0.3, ArtRules.HORIZON_CRUSH_RADIUS * 1.3, 0.12, 10);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, heart, 40, 0.35);
			ScreenFx.shake(level, base, 0.4F, 16);
			Feels.sound(level, heart, "aura_art_event_horizon_crush", 1.3F, 1.0F);
			AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.65F);
			for (LivingEntity foe : ArtKit.around(player, base, ArtRules.HORIZON_CRUSH_RADIUS, 1.5, 3.5, ArtRules.HORIZON_TARGETS)) {
				double d = foe.position().subtract(base).horizontalDistance();
				hits.strike(foe, ArtRules.falloff(ArtRules.HORIZON_CRUSH, ArtRules.HORIZON_CRUSH_EDGE, d, ArtRules.HORIZON_CRUSH_RADIUS), AuraFxRules.Weight.GRAND);
				if (foe.isAlive()) {
					shadow(player, foe, 1.0);
				}
			}
			Motes.burst(level, heart, 16, LILAC, 0.1, 18, 0.35);
		});
	}
}
