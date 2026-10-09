package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.TideRules;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Tide Breath's arts: water and the current. Tide moves foes where it wants them, pushing and dragging, and soaks them (fire put out,
 * a moment slowed, wet for lightning).
 * <ul>
 * <li><b>Riptide Cut</b> (I): a wide arc that drags the foes it cuts toward you, soaked.</li>
 * <li><b>Breaker</b> (II): a wave rolling out ahead, throwing foes back and leaving wet ground that slows.</li>
 * <li><b>Whirlpool</b> (III): a ring of water ahead drawing foes to its heart, cutting them each beat.</li>
 * <li><b>Surge</b> (IV): a wave dash, the foes in the way carried along with you.</li>
 * <li><b>Maelstrom</b> (V): the sea round you, wave after wave, and at its end a crash that throws them all.</li>
 * </ul>
 */
public final class TideArts {
	private TideArts() {}

	public static final String METHOD = "tide";
	public static final String RIPTIDE_CUT = "riptide_cut";
	public static final String BREAKER = "breaker";
	public static final String WHIRLPOOL = "whirlpool";
	public static final String SURGE = "surge";
	public static final String MAELSTROM = "maelstrom";

	public static final String WET_GROUND = "tide_wet";
	public static final String POOL = "tide_pool";
	public static final String SEA = "tide_sea";

	private static final int FOAM = MethodsAFlavours.FOAM;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, RIPTIDE_CUT, TideArts::riptideCut),
			MethodArts.art(AuraApi.ArtSlot.SECOND, BREAKER, TideArts::breaker),
			MethodArts.art(AuraApi.ArtSlot.THIRD, WHIRLPOOL, TideArts::whirlpool),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, SURGE, TideArts::surge),
			MethodArts.art(AuraApi.ArtSlot.FINAL, MAELSTROM, TideArts::maelstrom));
	}

	private static void splash(ServerLevel level, Vec3 at, int count) {
		Vfx.emit(level, ParticleTypes.SPLASH, at, count, 0.4, 0.2);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, at, Math.max(1, count / 3), 0.3, 0.05);
	}

	// ------------------------------------------------------------------ I. Riptide Cut

	static boolean riptideCut(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_riptide_cut", 1.0F, 1.0F);
		Vec3 dir = ArtKit.flat(player);
		Vec3 heart = player.position().add(0, 1.0, 0);
		ArtLight world = ArtLight.world(player);
		world.slash(heart.add(dir.scale(1.4)), ArtKit.UP, dir, color, TideRules.RIPTIDE_REACH * 0.7, 2.2, 0.3, 1, 7);
		world.bare().slash(heart.add(dir.scale(1.45)), ArtKit.UP, dir, FOAM, TideRules.RIPTIDE_REACH * 0.66, 1.9, 0.1, 1, 6);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), TideRules.RIPTIDE_REACH, TideRules.RIPTIDE_DEGREES, TideRules.RIPTIDE_TARGETS)) {
			hits.strike(foe, TideRules.RIPTIDE_FACTOR, AuraFxRules.Weight.FULL);
			MethodsAFlavours.soak(player, foe, TideRules.RIPTIDE_SOAK);
			// The current turns back: drawn toward the swordsman, never through them.
			Vec3 to = player.position().add(dir.scale(1.2));
			ArtKit.draw(foe, to, TideRules.RIPTIDE_DRAG);
			splash(level, foe.getBoundingBox().getCenter(), 8);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Breaker

	static boolean breaker(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		Vec3 from = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.LOW, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_breaker", 1.1F, 1.0F);
		Vec3 right = ArtKit.right(dir);
		ArtLight world = ArtLight.world(player);
		List<Vec3> points = new ArrayList<>();
		for (int i = 0; i <= (int) TideRules.BREAKER_LENGTH; i++) {
			Vec3 p = from.add(dir.scale(1 + i));
			points.add(p);
			if (i % 2 == 0) {
				world.ray(p.add(right.scale(-TideRules.BREAKER_WIDTH / 2)).add(0, 0.6, 0), p.add(right.scale(TideRules.BREAKER_WIDTH / 2)).add(0, 0.6, 0), FOAM, 0.2, 10);
				splash(level, p.add(0, 0.5, 0), 6);
			}
		}
		for (LivingEntity foe : ArtKit.line(player, from.add(0, 0.2, 0), dir, TideRules.BREAKER_LENGTH + 1, TideRules.BREAKER_WIDTH / 2, 2.4, TideRules.BREAKER_TARGETS)) {
			hits.strike(foe, TideRules.BREAKER_FACTOR, AuraFxRules.Weight.FULL);
			MethodsAFlavours.current(foe, dir.scale(TideRules.BREAKER_PUSH).add(0, 0.25, 0));
			Reactions.mark(foe, Reactions.Mark.WET, TideRules.BREAKER_WET);
			foe.clearFire();
		}
		// The wet ground left behind: slows foes standing in it and puts out fire.
		ArtFields.open(player, WET_GROUND, ArtFields.strip(points, TideRules.BREAKER_WIDTH / 2, 2.0), TideRules.BREAKER_WET, 10, (field, owner, age) -> {
			for (Vec3 p : points) {
				if (age % 20 == 0) {
					ArtLight.world(owner).ground(p.add(0, 0.03, 0), SigilOption.GLOW, color, TideRules.BREAKER_WIDTH * 0.6, 22, 0);
				}
			}
			for (LivingEntity foe : field.foes(owner)) {
				ArtKit.slow(owner, foe, 20, 0);
				Reactions.mark(foe, Reactions.Mark.WET, 40);
				foe.clearFire();
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ III. Whirlpool

	static boolean whirlpool(ServerPlayer player, AuraApi.StringContext context) {
		var counter = dev.wildercord.aura.MastersArts.earnedCounter(player);
		if (counter == null || !counter.art().equals(WHIRLPOOL) || !counter.valid()) return false;
		LivingEntity caught = counter.target();
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 centre = player.position().add(ArtKit.flat(player).scale(TideRules.WHIRLPOOL_AHEAD));
		Vec3 floor = ArtKit.floor(level, centre.add(0, 0.5, 0), 0.5, 3);
		Vec3 heart = floor == null ? centre : floor;
		Feels.sound(level, heart.add(0, 0.5, 0), "aura_art_whirlpool", 1.1F, 1.0F);
		// The counter: the one who struck is cut first and pulled toward the pool's heart.
		if (caught != null && counter.primaryValid()) {
			hits.strike(caught, TideRules.WHIRLPOOL_FACTOR * 2, AuraFxRules.Weight.FULL);
			if (counter.afterDamage(caught) && counter.permits(caught)) ArtKit.draw(caught, heart, TideRules.WHIRLPOOL_DRAG * 2);
		}
		ArtLight.world(player).ground(heart.add(0, 0.04, 0), SigilOption.RING, FOAM, TideRules.WHIRLPOOL_RADIUS, TideRules.WHIRLPOOL_TICKS, 0.08);
		ArtFields.open(player, POOL, ArtFields.disc(() -> heart, TideRules.WHIRLPOOL_RADIUS, 2.5), TideRules.WHIRLPOOL_TICKS, 2, (field, owner, age) -> {
			ServerLevel lv = field.level();
			if (age % 4 == 0) {
				ArtLight.world(owner).groundRing(heart, color, TideRules.WHIRLPOOL_RADIUS, 0.4, 0.08, 8);
				double a = age * 0.6;
				Vfx.emit(lv, ParticleTypes.SPLASH, heart.add(Math.cos(a) * 2, 0.3, Math.sin(a) * 2), 4, 0.2, 0.1);
			}
			List<LivingEntity> foes = field.foes(owner);
			if (foes.size() > TideRules.WHIRLPOOL_TARGETS) {
				foes = foes.subList(0, TideRules.WHIRLPOOL_TARGETS);
			}
			for (LivingEntity foe : foes) {
				ArtKit.draw(foe, heart, TideRules.WHIRLPOOL_DRAG);
				if (age % TideRules.WHIRLPOOL_PERIOD == 0) {
					hits.strike(foe, TideRules.WHIRLPOOL_FACTOR, AuraFxRules.Weight.LIGHT);
					Reactions.mark(foe, Reactions.Mark.WET, 40);
					foe.clearFire();
				}
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ IV. Surge

	static boolean surge(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, TideRules.SURGE_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, SURGE);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		AuraStep.afterimages(player, from, path.getLast(), dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_surge", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, TideRules.SURGE_TICKS, (a, b, step, last) -> {
			world.ray(a.add(0, 0.4, 0), b.add(0, 0.4, 0), color, 0.5, 10);
			world.bare().ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), FOAM, 0.14, 9);
			splash(level, b.add(0, 0.4, 0), 6);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 1.0, TideRules.SURGE_WIDTH / 2 + 0.3, 2.2,
					TideRules.SURGE_TARGETS)) {
				if (!hits.hurt(foe) && hits.count() < TideRules.SURGE_TARGETS) {
					hits.strike(foe, TideRules.SURGE_FACTOR, AuraFxRules.Weight.FULL);
					Reactions.mark(foe, Reactions.Mark.WET, 60);
				}
				// Carried along on the wave, until it breaks.
				if (hits.hurt(foe)) {
					MethodsAFlavours.current(foe, dir.scale(TideRules.SURGE_CARRY).add(0, 0.12, 0));
				}
			}
			if (last) {
				world.groundRing(b, FOAM, 0.4, 2.6, 0.14, 10);
				splash(level, b.add(0, 0.6, 0), 20);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Maelstrom

	static boolean maelstrom(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_maelstrom", 1.3F, 1.0F);
		int ticks = TideRules.MAELSTROM_WAVES * TideRules.MAELSTROM_PERIOD;
		ArtFields.open(player, SEA, ArtFields.disc(player::position, TideRules.MAELSTROM_RADIUS, 3.0), ticks, TideRules.MAELSTROM_PERIOD,
			(field, owner, age) -> sea(owner, field, hits, age, ticks));
		return true;
	}

	/** One wave of the maelstrom; the last one crashes and throws them all. */
	private static void sea(ServerPlayer owner, ArtFields.Field field, ArtKit.Hits hits, long age, int ticks) {
		ServerLevel level = field.level();
		int color = ArtKit.color(owner);
		Vec3 at = owner.position();
		RandomSource r = level.getRandom();
		ArtLight show = ArtLight.spectacle(owner);
		ArtLight world = ArtLight.world(owner);
		show.swirl(at, TideRules.MAELSTROM_RADIUS * 0.6, 3.0, 5, color, FOAM);
		world.groundRing(at, FOAM, TideRules.MAELSTROM_RADIUS, 0.8, 0.1, 8);
		for (int i = 0; i < 6; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			splash(level, at.add(Math.cos(a) * TideRules.MAELSTROM_RADIUS * 0.7, 0.5, Math.sin(a) * TideRules.MAELSTROM_RADIUS * 0.7), 4);
		}
		List<LivingEntity> inside = field.foes(owner);
		if (inside.size() > TideRules.MAELSTROM_TARGETS) {
			inside = inside.subList(0, TideRules.MAELSTROM_TARGETS);
		}
		boolean end = age >= ticks;
		for (LivingEntity foe : inside) {
			Vec3 rel = foe.position().subtract(at);
			rel = new Vec3(rel.x, 0, rel.z);
			Vec3 out = rel.lengthSqr() < 1.0E-4 ? ArtKit.flat(owner) : rel.normalize();
			if (end) {
				hits.strike(foe, TideRules.MAELSTROM_CRASH, AuraFxRules.Weight.GRAND);
				MethodsAFlavours.current(foe, out.scale(TideRules.MAELSTROM_THROW).add(0, 0.35, 0));
			} else {
				hits.strike(foe, TideRules.MAELSTROM_FACTOR, null);
				MethodsAFlavours.soak(owner, foe, 40);
				// Round and round: the current carries it along the ring, never out of it.
				Vec3 along = new Vec3(-out.z, 0, out.x);
				ArtKit.shove(foe, along.scale(0.35).add(out.scale(-0.15)));
			}
		}
		if (end) {
			world.groundRing(at, color, 0.5, TideRules.MAELSTROM_RADIUS * 1.4, 0.25, 12);
			world.groundRing(at, FOAM, 0.4, TideRules.MAELSTROM_RADIUS * 1.1, 0.08, 10);
			Vfx.emit(level, ParticleTypes.SPLASH, at.add(0, 1, 0), 60, TideRules.MAELSTROM_RADIUS * 0.5, 0.4);
			AuraFx.sound(owner, AuraFx.Sound.IMPACT, 0.9F, 0.8F);
		}
	}
}
