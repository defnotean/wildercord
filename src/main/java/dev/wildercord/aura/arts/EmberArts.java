package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ember Breath's arts: fire that spreads and lingers. Every one sets its foes alight, and most leave fire on the ground behind
 * them for a while (light only: nothing in the world burns, see {@link ArtFields}), so an Ember swordsman's fight is a field of
 * embers by its end.
 * <ul>
 * <li><b>Kindling Draw</b> (I): a fast low draw-cut that sets the foes in front alight, and a line of fire racing on ahead.</li>
 * <li><b>Rising Cinders</b> (II): a rising slash that throws the foes in front up in a shower of embers; a moment later cinders
 * rain off them onto whoever stands near.</li>
 * <li><b>Backdraft</b> (III): the caught blow thrown back as a gout of flame, the one who struck taking it whole.</li>
 * <li><b>Wildfire Rush</b> (IV): a second rush straight after the step, through foes, the ground left burning behind.</li>
 * <li><b>Sunfall</b> (V): up into the air with a sun on the blade, down in a blazing arc: a blast where it lands, and a ring of
 * fire standing round it.</li>
 * </ul>
 */
public final class EmberArts {
	private EmberArts() {}

	public static final String METHOD = "ember";
	public static final String KINDLING_DRAW = "kindling_draw";
	public static final String RISING_CINDERS = "rising_cinders";
	public static final String BACKDRAFT = "backdraft";
	public static final String WILDFIRE_RUSH = "wildfire_rush";
	public static final String SUNFALL = "sunfall";

	/** The kinds of field Ember leaves (for the tests). */
	public static final String FIRE_LINE = "ember_fire_line";
	public static final String FIRE_TRAIL = "ember_fire_trail";
	public static final String FIRE_RING = "ember_fire_ring";

	/** The fire palette's gold and red (cast.ElementFx.FIRE), written out so the arts load without the particle registry. */
	private static final int GOLD = 0xFFD060;
	private static final int RED = 0xFF3A1A;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, KINDLING_DRAW, EmberArts::kindlingDraw),
			MethodArts.art(AuraApi.ArtSlot.SECOND, RISING_CINDERS, EmberArts::risingCinders),
			MethodArts.art(AuraApi.ArtSlot.THIRD, BACKDRAFT, EmberArts::backdraft),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, WILDFIRE_RUSH, EmberArts::wildfireRush),
			MethodArts.art(AuraApi.ArtSlot.FINAL, SUNFALL, EmberArts::sunfall));
	}

	// ------------------------------------------------------------------ I. Kindling Draw

	static boolean kindlingDraw(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		// The draw: a low crescent of fire swept across the knees in front, gold at its edge.
		ArtLight world = ArtLight.world(player);
		Vec3 centre = feet.add(0, 0.45, 0).add(look.scale(1.1));
		world.slash(centre, ArtKit.UP, look, color, 2.2, 2.6, 0.55, 2, 10);
		world.bare().slash(centre.add(0, 0.03, 0), ArtKit.UP, look, GOLD, 2.12, 2.2, 0.16, 2, 9);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_kindling_draw", 1.0F, 1.0F);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.KINDLING_REACH, ArtRules.KINDLING_DEGREES, ArtRules.KINDLING_TARGETS)) {
			hits.strike(foe, ArtRules.KINDLING_FACTOR);
			ArtKit.ignite(foe, ArtRules.KINDLING_IGNITE);
			ElementFx.embers(level, foe.getBoundingBox().getCenter(), 0.3, 5);
		}
		// The line of fire racing on ahead along the ground, a block a tick, then burning.
		List<Vec3> line = groundLine(level, feet.add(look.scale(ArtRules.KINDLING_LINE_FROM)), look, ArtRules.KINDLING_LINE, 1.0);
		if (!line.isEmpty()) {
			for (int i = 0; i < line.size(); i++) {
				Vec3 p = line.get(i);
				Vec3 prev = i == 0 ? p : line.get(i - 1);
				dev.wildercord.cast.Scheduler.later(1 + i, () -> {
					if (!player.isAlive()) {
						return;
					}
					ignitePoint(player, prev, p, color);
				});
			}
			ArtFields.open(player, FIRE_LINE, ArtFields.strip(line, ArtRules.KINDLING_LINE_HALF, 1.5), ArtRules.KINDLING_LINE_TICKS, 5,
				(field, owner, age) -> burn(owner, field, line, age, ArtRules.KINDLING_LINE_IGNITE));
		}
		return true;
	}

	/** Points along the ground from {@code from} down {@code dir}, {@code spacing} apart, {@code length} long; it stops at a drop or a wall. */
	static List<Vec3> groundLine(ServerLevel level, Vec3 from, Vec3 dir, double length, double spacing) {
		List<Vec3> out = new ArrayList<>();
		double y = from.y;
		for (double d = 0; d <= length + 1.0E-6; d += spacing) {
			Vec3 at = new Vec3(from.x + dir.x * d, y, from.z + dir.z * d);
			Vec3 ground = ArtKit.floor(level, at, 1.1, 2.1);
			if (ground == null) {
				break;
			}
			out.add(ground);
			y = ground.y;
		}
		return out;
	}

	/** Fire catching on one stretch of ground: a run of light along it, flame tongues and embers. */
	private static void ignitePoint(ServerPlayer player, Vec3 from, Vec3 to, int color) {
		ServerLevel level = player.level();
		ArtLight world = ArtLight.world(player);
		world.ray(from.add(0, 0.08, 0), to.add(0, 0.08, 0), color, 0.55, 18);
		world.bare().ray(from.add(0, 0.1, 0), to.add(0, 0.1, 0), GOLD, 0.14, 14);
		world.tongues(to, 0.24, 0.8, 3, color, GOLD, 9);
		ElementFx.embers(level, to.add(0, 0.2, 0), 0.25, 3);
	}

	/** A field of fire's beat: flames licking up along it, and what stands in it set alight (each foe at most every half second). */
	private static void burn(ServerPlayer owner, ArtFields.Field field, List<Vec3> points, long age, int ignite) {
		ServerLevel level = field.level();
		RandomSource r = level.getRandom();
		int color = ArtKit.color(owner);
		ArtLight world = ArtLight.world(owner);
		for (int i = 0; i < Math.max(1, points.size() / 2); i++) {
			Vec3 p = points.get(r.nextInt(points.size())).add((r.nextDouble() - 0.5) * 0.6, 0, (r.nextDouble() - 0.5) * 0.6);
			world.tongues(p, 0.2, 0.65, 2, color, GOLD, 8);
			if (r.nextInt(3) == 0) {
				ElementFx.embers(level, p.add(0, 0.15, 0), 0.2, 2);
			}
		}
		if (age % 10 == 0 && points.size() > 1) {
			// The glow along the ground, renewed while it burns, fading as its end nears.
			int glow = field.left() < 20 ? ArtKit.mix(color, 0x401008, 0.5) : color;
			for (int i = 1; i < points.size(); i++) {
				world.ray(points.get(i - 1).add(0, 0.06, 0), points.get(i).add(0, 0.06, 0), glow, 0.4, 12);
			}
		}
		if (age % 20 == 0) {
			Feels.sound(level, field.shape().centre(), "fire_coals", 0.35F, 1.1F);
		}
		for (LivingEntity foe : field.foes(owner)) {
			if (Statuses.claim(foe, "ember_field", 10)) {
				ArtKit.ignite(foe, ignite);
			}
		}
	}

	// ------------------------------------------------------------------ II. Rising Cinders

	static boolean risingCinders(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		// A crescent of flame cut upward across the foes in front, facing the swordsman (its top stays under their eyes).
		Vec3 centre = feet.add(0, 0.25, 0).add(look.scale(2.2));
		Vec3 offHand = ArtKit.bladeSide(player, look).scale(-1);
		Vec3 facing = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? look : look.scale(-1);
		Vec3 toward = ArtKit.UP.scale(0.87).add(offHand.scale(0.5)).normalize();
		// Seen from outside: in your own first person it would rise through the middle of the view, where the trail already is.
		ArtLight show = ArtLight.spectacle(player);
		show.slash(centre, facing, toward, color, 1.15, 2.2, 0.5, 1, 10);
		show.slash(centre.subtract(look.scale(0.04)), facing, toward, GOLD, 1.1, 1.9, 0.15, 1, 9);
		// A fountain of embers off the cut.
		for (int i = 0; i < 10; i++) {
			Vec3 at = centre.add((level.getRandom().nextDouble() - 0.5) * 1.6, 0.4 + level.getRandom().nextDouble() * 0.6, (level.getRandom().nextDouble() - 0.5) * 1.6);
			Motes.fling(level, at, new Vec3((level.getRandom().nextDouble() - 0.5) * 0.3, 1, (level.getRandom().nextDouble() - 0.5) * 0.3), 0.16,
				i % 3 == 0 ? GOLD : color, 0.09, 26, new Vec3(0, -0.006, 0));
		}
		ElementFx.heatFlare(level, centre.add(0, 0.5, 0), 0.9);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_rising_cinders", 1.0F, 1.0F);
		List<LivingEntity> lifted = new ArrayList<>();
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.CINDERS_REACH, ArtRules.CINDERS_DEGREES, ArtRules.CINDERS_TARGETS)) {
			hits.strike(foe, ArtRules.CINDERS_FACTOR);
			ArtKit.ignite(foe, ArtRules.CINDERS_IGNITE);
			ArtKit.lift(foe, ArtRules.CINDERS_LIFT, ArtRules.CINDERS_AIRBORNE);
			lifted.add(foe);
			// Embers streaming off it as it rises.
			for (int t = 1; t <= 8; t += 2) {
				dev.wildercord.cast.Scheduler.later(t, () -> {
					if (foe.isAlive()) {
						ElementFx.embers(level, foe.position().add(0, foe.getBbHeight() * 0.4, 0), 0.25, 3);
					}
				});
			}
		}
		// The cinders: a moment later, raining off each one thrown onto whoever stands near it.
		dev.wildercord.cast.Scheduler.later(ArtRules.CINDERS_RAIN_DELAY, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			for (LivingEntity foe : lifted) {
				cinders(player, hits, foe, color);
			}
		});
		return true;
	}

	private static void cinders(ServerPlayer player, ArtKit.Hits hits, LivingEntity source, int color) {
		ServerLevel level = player.level();
		Vec3 above = source.position().add(0, source.getBbHeight() * 0.5, 0);
		Vec3 ground = ArtKit.floor(level, source.position(), 0.5, 6);
		Vec3 base = ground == null ? source.position() : ground;
		RandomSource r = level.getRandom();
		for (int i = 0; i < 12; i++) {
			Vec3 from = above.add((r.nextDouble() - 0.5) * 0.8, r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.8);
			Motes.fling(level, from, new Vec3((r.nextDouble() - 0.5) * 1.6, -0.4, (r.nextDouble() - 0.5) * 1.6), 0.12, i % 2 == 0 ? GOLD : color, 0.08, 18,
				new Vec3(0, -0.02, 0));
		}
		ArtLight.world(player).groundRing(base, color, 0.3, ArtRules.CINDERS_RAIN_RADIUS, 0.08, 9);
		ElementFx.embers(level, base.add(0, 0.2, 0), ArtRules.CINDERS_RAIN_RADIUS * 0.5, 6);
		Feels.sound(level, base, "fire_ash", 0.6F, 1.2F);
		for (LivingEntity foe : ArtKit.around(player, base, ArtRules.CINDERS_RAIN_RADIUS, 1.0, 3.0, 6)) {
			if (foe == source) {
				continue;
			}
			hits.strike(foe, ArtRules.CINDERS_RAIN_FACTOR, AuraFxRules.Weight.LIGHT);
			ArtKit.ignite(foe, 40);
		}
	}

	// ------------------------------------------------------------------ III. Backdraft

	static boolean backdraft(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		LivingEntity foe = ArtKit.attacker(player, context, ArtRules.BACKDRAFT_REACH);
		AuraGuard.Caught caught = AuraGuard.caught(player);
		double blow = caught == null ? 0 : caught.damage();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		// The breath drawn in: a little flame pulled into the blade, then the gout.
		Vec3 hand = ArtKit.hand(player);
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			Motes.seek(level, hand.add(Math.cos(a) * 0.9, 0.3 + Math.sin(a) * 0.4, Math.sin(a) * 0.9), hand, i % 2 == 0 ? GOLD : color, 0.08, 4, 0.4);
		}
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_backdraft", 1.1F, 1.0F);
		// The gout: crescents of flame fanning out down the cone. The rings are seen only from outside (face-on they would
		// fill your own view), and so is the nearest crescent; you see the outer crescents and the embers.
		ArtLight show = ArtLight.spectacle(player);
		ArtLight world = ArtLight.world(player);
		Vec3 side = ArtKit.right(look);
		Vec3 normal = look;
		for (int k = 0; k < 4; k++) {
			double d = 1.4 + k * 0.95;
			Vec3 at = feet.add(0, 0.95, 0).add(look.scale(d));
			int c = k % 2 == 0 ? color : GOLD;
			ArtLight l = k == 0 ? show : world;
			double radius = 0.55 + k * 0.38;
			int delay = k;
			int edge = k % 2 == 0 ? RED : color;
			dev.wildercord.cast.Scheduler.later(1 + delay, () -> {
				show.ring(at, normal, c, radius * 0.3, radius * 1.15, 0.12 + 0.03 * delay, 7);
				l.slash(at, normal, side, edge, radius, 2.6, 0.18, 2, 7);
				l.whirl(at, radius * 0.75, 3, GOLD, color, RED);
				ElementFx.embers(level, at, radius * 0.5, 3);
			});
		}
		ElementFx.heatFlare(level, feet.add(0, 1.0, 0).add(look.scale(2.4)), 0.8);
		dev.wildercord.cast.Scheduler.later(2, () -> Feels.sound(level, feet.add(look.scale(2.5)), "fire_whump", 0.8F, 0.9F));
		double main = ArtRules.backdraft(ArtKit.weapon(player), blow) * ArtKit.scale();
		if (foe != null) {
			hits.raw(foe, main, AuraFxRules.Weight.HEAVY);
			ArtKit.ignite(foe, ArtRules.BACKDRAFT_IGNITE);
			ArtKit.knock(foe, feet, ArtRules.BACKDRAFT_THROW, 0.25);
			ElementFx.fireImpact(level, foe.getBoundingBox().getCenter(), 1.0);
		}
		for (LivingEntity other : ArtKit.arc(player, null, ArtRules.BACKDRAFT_REACH, ArtRules.BACKDRAFT_DEGREES, ArtRules.BACKDRAFT_TARGETS)) {
			if (other == foe) {
				continue;
			}
			hits.raw(other, main * ArtRules.BACKDRAFT_SPLASH, AuraFxRules.Weight.FULL);
			ArtKit.ignite(other, ArtRules.BACKDRAFT_IGNITE);
			ArtKit.knock(other, feet, ArtRules.BACKDRAFT_THROW * 0.7, 0.2);
		}
		return true;
	}

	// ------------------------------------------------------------------ IV. Wildfire Rush

	static boolean wildfireRush(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.WILDFIRE_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, WILDFIRE_RUSH);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.55F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		AuraStep.afterimages(player, from, to, dir, color);
		ElementFx.heatFlare(level, from.add(0, 1.0, 0), 0.9);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_wildfire_rush", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, ArtRules.WILDFIRE_TICKS, (a, b, step, last) -> {
			// A streak of fire low along the way, white-gold down its middle, flames bursting off it.
			world.ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), color, 0.36, 9);
			world.bare().ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), GOLD, 0.09, 7);
			world.ray(a.add(0, 0.1, 0), b.add(0, 0.1, 0), color, 0.5, 18);
			world.tongues(b, 0.3, 1.0, 3, color, GOLD, 10);
			ElementFx.embers(level, b.add(0, 0.5, 0), 0.4, 4);
			Vec3 seg = b.subtract(a);
			double length = Math.max(0.5, seg.horizontalDistance());
			for (LivingEntity foe : ArtKit.line(player, a, seg, length + 0.8, ArtRules.WILDFIRE_WIDTH / 2 + 0.3, 2.2, ArtRules.WILDFIRE_TARGETS)) {
				if (!hits.hurt(foe) && hits.count() < ArtRules.WILDFIRE_TARGETS) {
					hits.strike(foe, ArtRules.WILDFIRE_FACTOR);
					ArtKit.ignite(foe, ArtRules.WILDFIRE_IGNITE);
					ElementFx.fireImpact(level, foe.getBoundingBox().getCenter(), 0.8);
					ArtKit.shove(foe, ArtKit.right(dir).scale(foe.position().subtract(b).dot(ArtKit.right(dir)) >= 0 ? 0.35 : -0.35).add(0, 0.2, 0));
				}
			}
			if (last) {
				ElementFx.fireImpact(level, b.add(0, 0.9, 0), 0.9);
				Feels.sound(level, b, "fire_whump", 0.7F, 1.1F);
			}
		});
		// The ground left burning along the way.
		List<Vec3> trail = new ArrayList<>();
		for (int i = 0; i < path.size(); i += 4) {
			trail.add(path.get(i));
		}
		trail.add(to);
		ArtFields.open(player, FIRE_TRAIL, ArtFields.strip(trail, 0.7, 1.5), ArtRules.WILDFIRE_TRAIL_TICKS, 5,
			(field, owner, age) -> burn(owner, field, trail, age, 40));
		return true;
	}

	// ------------------------------------------------------------------ V. Sunfall

	static boolean sunfall(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		AuraFx.Art fx = AuraFx.art(player);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 v = player.getDeltaMovement();
		ArtKit.launch(player, new Vec3(v.x * 0.3 + look.x * 0.15, ArtRules.SUNFALL_LEAP, v.z * 0.3 + look.z * 0.15));
		fx.trail(AuraFxRules.Stroke.RISING, true, 1.3F);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_sunfall", 1.2F, 1.0F);
		ArtLight show = ArtLight.spectacle(player);
		// The sun gathering over the blade as the swordsman rises, seen from outside (overhead, it isn't in your own view).
		for (int t = 0; t < ArtRules.SUNFALL_RISE; t++) {
			int tick = t;
			dev.wildercord.cast.Scheduler.later(1 + t, () -> {
				if (!player.isAlive()) {
					return;
				}
				Vec3 sun = player.position().add(0, 2.6, 0).add(look.scale(0.3));
				double r = 0.35 + 0.07 * tick;
				show.orb(sun, GOLD, r, 3);
				if (tick % 3 == 0) {
					show.ring(sun, ArtKit.UP, color, r * 1.2, r * 2.6, 0.06, 8);
					show.flash(sun, GOLD, (float) (r * 3));
				}
				Motes.seek(player.level(), sun.add((level.getRandom().nextDouble() - 0.5) * 3, (level.getRandom().nextDouble() - 0.5) * 2,
					(level.getRandom().nextDouble() - 0.5) * 3), sun, GOLD, 0.09, 6, 0.3);
				player.resetFallDistance();
			});
		}
		// Down in a blazing arc.
		dev.wildercord.cast.Scheduler.later(ArtRules.SUNFALL_RISE, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			Vec3 l = ArtKit.flat(player);
			ArtKit.launch(player, new Vec3(l.x * 0.25, -ArtRules.SUNFALL_DIVE, l.z * 0.25));
			fx.trail(AuraFxRules.Stroke.FALLING, false, 1.7F);
			Vec3 centre = player.position().add(0, 0.6, 0).add(l.scale(1.6));
			Vec3 normal = ArtKit.right(l);
			show.slash(centre, normal, ArtKit.UP, GOLD, 2.4, 2.4, 0.5, 2, 10);
			show.slash(centre, normal, ArtKit.UP, color, 2.5, 2.5, 0.9, 2, 11);
			Feels.sound(level, player.position(), "fire_meteor_fall", 0.9F, 1.1F);
			MethodArts.whenLanded(player, ArtRules.SUNFALL_FALL_MAX, at -> sunfallLands(player, hits, at, color));
		});
		return true;
	}

	private static void sunfallLands(ServerPlayer player, ArtKit.Hits hits, Vec3 at, int color) {
		ServerLevel level = player.level();
		player.resetFallDistance();
		Vec3 ground = ArtKit.floor(level, at, 0.5, 2);
		Vec3 base = ground == null ? at : ground;
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		// The blast: rings of fire racing out over the ground, a pillar of flame, the ground scorched in a seal.
		world.groundRing(base, color, 0.4, ArtRules.SUNFALL_RADIUS * 1.15, 0.42, 12);
		world.groundRing(base, GOLD, 0.3, ArtRules.SUNFALL_RADIUS * 0.9, 0.14, 10);
		world.groundRing(base, RED, 0.6, ArtRules.SUNFALL_RADIUS * 1.35, 0.08, 16);
		world.ground(base, SigilOption.CRACKED, RED, ArtRules.SUNFALL_RADIUS * 0.8, 40, 0);
		show.ray(base, base.add(0, 7, 0), color, 0.9, 14);
		show.ray(base, base.add(0, 6, 0), GOLD, 0.25, 12);
		show.flash(base.add(0, 1.2, 0), GOLD, 3.6F);
		show.whirl(base.add(0, 0.8, 0), 2.2, 8, GOLD, color, RED);
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			world.tongues(base.add(Math.cos(a) * 2.6, 0, Math.sin(a) * 2.6), 0.3, 1.2, 2, color, GOLD, 12);
		}
		ElementFx.embers(level, base.add(0, 0.4, 0), 2.5, 18);
		ScreenFx.shake(level, base, 0.4F, 16);
		Feels.sound(level, base, "aura_art_sunfall_impact", 1.3F, 1.0F);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.75F);
		for (LivingEntity foe : ArtKit.around(player, base, ArtRules.SUNFALL_RADIUS, 1.5, 3.5, ArtRules.SUNFALL_TARGETS)) {
			double d = foe.position().subtract(base).horizontalDistance();
			hits.strike(foe, ArtRules.falloff(ArtRules.SUNFALL_CENTRE, ArtRules.SUNFALL_EDGE, d, ArtRules.SUNFALL_RADIUS), AuraFxRules.Weight.GRAND);
			ArtKit.ignite(foe, ArtRules.SUNFALL_IGNITE);
			ArtKit.knock(foe, base, ArtRules.SUNFALL_THROW, 0.35);
		}
		// The ring of fire, standing a while round where it fell.
		Vec3 centre = base;
		ArtFields.open(player, FIRE_RING, ArtFields.ring(centre, ArtRules.SUNFALL_RING - ArtRules.SUNFALL_RING_BAND / 2,
			ArtRules.SUNFALL_RING + ArtRules.SUNFALL_RING_BAND / 2, 2.0), ArtRules.SUNFALL_RING_TICKS, 2, (field, owner, age) -> {
			ServerLevel lv = field.level();
			RandomSource rr = lv.getRandom();
			int c = ArtKit.color(owner);
			ArtLight ring = ArtLight.world(owner);
			for (int i = 0; i < 3; i++) {
				double a = rr.nextDouble() * Math.PI * 2;
				Vec3 p = centre.add(Math.cos(a) * ArtRules.SUNFALL_RING, 0, Math.sin(a) * ArtRules.SUNFALL_RING);
				ring.tongues(p, 0.24, 1.0, 2, c, GOLD, 9);
			}
			if (age % 10 == 0) {
				ArtLight w = ArtLight.world(owner);
				w.groundRing(centre, c, ArtRules.SUNFALL_RING - 0.4, ArtRules.SUNFALL_RING + 0.2, 0.16, 12);
				if (field.left() > 20) {
					w.groundRing(centre, GOLD, ArtRules.SUNFALL_RING - 0.1, ArtRules.SUNFALL_RING, 0.05, 10);
				}
				for (LivingEntity foe : field.foes(owner)) {
					if (Statuses.claim(foe, "ember_ring", ArtRules.SUNFALL_RING_PERIOD)) {
						hits.strike(foe, ArtRules.SUNFALL_RING_FACTOR, AuraFxRules.Weight.LIGHT);
						ArtKit.ignite(foe, 60);
					}
				}
			}
			if (age % 20 == 0) {
				Feels.sound(lv, centre, "fire_pyre", 0.45F, 1.0F);
			}
		});
	}
}
