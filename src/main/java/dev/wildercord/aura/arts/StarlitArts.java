package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Starlit Breath's arts: stars that mark and burst, and aura given back. A Starlit swordsman fights from a little further off with
 * darts and falling stars, sets stars on the foes it strikes for its next art to burst, and has its aura given back for every foe its
 * stars find (a tick after the art's own price is paid, never scaled as a gain is: {@link ArtKit#giveBack}). Its light is pink and
 * pale starlight, its voice bright chimes.
 * <ul>
 * <li><b>Star Needle</b> (I): a thrust that looses three darts of starlight, each seeking a little toward a foe; each that strikes
 * sets a star on its foe and gives aura back.</li>
 * <li><b>Meteor Shower</b> (II): stars brought down out of the sky over a circle ahead, the last a great one that sets stars on what
 * it strikes; aura back for each foe struck.</li>
 * <li><b>Constellation Guard</b> (III): whoever struck is cut and set with a constellation that bursts star by star a moment later,
 * each burst giving aura back; every foe near already carrying a star bursts at once.</li>
 * <li><b>Comet Dash</b> (IV): a second rush trailing stars, the foes in the way cut and starred; a moment later the trail bursts star
 * by star along its length, aura back for each foe it catches.</li>
 * <li><b>Nova</b> (V): starlight gathered on the blade, then a ring of it bursting out over everything near; starred foes burst
 * with it, and aura comes back for every foe struck (up to half its price).</li>
 * </ul>
 */
public final class StarlitArts {
	private StarlitArts() {}

	public static final String METHOD = "starlit";
	public static final String STAR_NEEDLE = "star_needle";
	public static final String METEOR_SHOWER = "meteor_shower";
	public static final String CONSTELLATION_GUARD = "constellation_guard";
	public static final String COMET_DASH = "comet_dash";
	public static final String NOVA = "nova";

	/** The arcane palette's pale pink and violet, starlight's white, and Starlit's own pink (for a star when its swordsman's not at hand). */
	private static final int PALE = 0xFFD8FA;
	private static final int VIOLET = 0x9A7CFF;
	private static final int WHITE = 0xFFFFFF;
	private static final int PINK = 0xE678DC;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, STAR_NEEDLE, StarlitArts::starNeedle),
			MethodArts.art(AuraApi.ArtSlot.SECOND, METEOR_SHOWER, StarlitArts::meteorShower),
			MethodArts.art(AuraApi.ArtSlot.THIRD, CONSTELLATION_GUARD, StarlitArts::constellationGuard),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, COMET_DASH, StarlitArts::cometDash),
			MethodArts.art(AuraApi.ArtSlot.FINAL, NOVA, StarlitArts::nova));
	}

	// ------------------------------------------------------------------ the look they share

	/** A star on a foe, while it lasts (asked by {@link ArtWards} every half second): a small star turning over its head. */
	static void starLook(LivingEntity foe, long left) {
		if (!(foe.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 over = foe.position().add(0, foe.getBbHeight() + 0.45, 0);
		double size = left < 30 ? 0.26 : 0.36;
		Light.ray(level, over.add(-size, 0, 0), over.add(size, 0, 0), PINK, 0.035, 11);
		Light.ray(level, over.add(0, 0, -size), over.add(0, 0, size), PINK, 0.035, 11);
		Vfx.emit(level, ParticleTypes.END_ROD, over, 1, 0.12, 0.005);
	}

	/** A star bursting at {@code at}: a white flash, a ring snapping out, a star seal and sparks of starlight. */
	static void burst(ServerPlayer player, Vec3 at, int color, double size, int note) {
		ServerLevel level = player.level();
		ArtLight world = ArtLight.world(player);
		world.flash(at, WHITE, (float) (1.3 * size));
		world.ring(at, ArtKit.UP, color, 0.1, 1.1 * size, 0.06, 7);
		world.bare().ring(at, ElementFx.tilted(1.1, at.x + at.z), PALE, 0.1, 0.8 * size, 0.03, 6);
		world.sigil(at, ArtKit.UP, SigilOption.STAR, color, 0.6 * size, 10, 0.2);
		Motes.burst(level, at, (int) Math.max(3, 7 * size), PALE, 0.07, 14, 0.18 * size);
		Feels.sound(level, at, "arcane_star_chime", 0.55F, 1.0F + 0.12F * (note % 6));
	}

	// ------------------------------------------------------------------ I. Star Needle

	static boolean starNeedle(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 aim = new Vec3(look.x, look.y * 0.35, look.z).normalize();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_star_needle", 1.0F, 1.0F);
		Vec3 hand = ArtKit.hand(player).add(aim.scale(0.6));
		ArtLight.world(player).flash(hand.add(aim.scale(0.4)), PALE, 0.8F);
		Set<UUID> paid = new HashSet<>();
		for (int i = 0; i < ArtRules.NEEDLE_DARTS; i++) {
			double turn = Math.toRadians((i - (ArtRules.NEEDLE_DARTS - 1) / 2.0) * ArtRules.NEEDLE_SPREAD);
			Vec3 dir = new Vec3(aim.x * Math.cos(turn) - aim.z * Math.sin(turn), aim.y, aim.x * Math.sin(turn) + aim.z * Math.cos(turn)).normalize();
			dart(player, hits, hand, dir, color, i, paid);
		}
		return true;
	}

	/** One dart of starlight: flying on a tick at a time, turning a little toward the foe nearest its path, until it strikes or is spent. */
	private static void dart(ServerPlayer player, ArtKit.Hits hits, Vec3 start, Vec3 heading, int color, int index, Set<UUID> paid) {
		ServerLevel level = player.level();
		Vec3[] at = {start};
		Vec3[] dir = {heading};
		double[] flown = {0};
		boolean[] done = {false};
		int steps = (int) Math.ceil(ArtRules.NEEDLE_RANGE / ArtRules.NEEDLE_SPEED);
		for (int t = 0; t <= steps; t++) {
			Scheduler.later(t + index % 2, () -> {
				if (done[0] || !player.isAlive() || player.level() != level) {
					return;
				}
				// Seeking: a turn toward the nearest foe ahead of it.
				LivingEntity seek = ArtKit.nearest(player, at[0].add(dir[0].scale(2.0)), 3.0, List.of());
				if (seek != null) {
					Vec3 to = seek.getBoundingBox().getCenter().subtract(at[0]);
					if (to.lengthSqr() > 1.0E-4 && to.normalize().dot(dir[0]) > 0.2) {
						dir[0] = dir[0].add(to.normalize().scale(ArtRules.NEEDLE_SEEK)).normalize();
					}
				}
				Vec3 from = at[0];
				Vec3 next = from.add(dir[0].scale(ArtRules.NEEDLE_SPEED));
				flown[0] += ArtRules.NEEDLE_SPEED;
				BlockPos pos = BlockPos.containing(next);
				boolean wall = !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
				ArtLight world = ArtLight.world(player);
				// A needle of light with a long tail: the streak reaches back past where it was a tick ago, so the darts read as
				// three lines of light crossing the air, not specks.
				Vec3 head = wall ? from.add(dir[0].scale(0.5)) : next;
				Vec3 tail = from.subtract(dir[0].scale(flown[0] <= ArtRules.NEEDLE_SPEED ? 0.2 : 1.2));
				world.ray(tail, head, color, 0.17, 6);
				world.bare().ray(tail.lerp(head, 0.3), head, WHITE, 0.06, 5);
				world.bare().orb(head, WHITE, 0.13, 3);
				world.flash(head, PALE, 0.55F);
				// What it strikes: the first foe its path runs through.
				LivingEntity struck = null;
				double best = Double.MAX_VALUE;
				for (Entity e : level.getEntities(player, new AABB(from, next).inflate(0.7), e -> ArtKit.harmable(player, e))) {
					Vec3 c = e.getBoundingBox().getCenter();
					Vec3 seg = next.subtract(from);
					double tt = Math.max(0, Math.min(1, c.subtract(from).dot(seg) / Math.max(1.0E-6, seg.lengthSqr())));
					double off = c.distanceTo(from.add(seg.scale(tt)));
					if (off <= 0.45 + Math.max(e.getBbWidth(), e.getBbHeight() * 0.5) * 0.5 && tt < best) {
						best = tt;
						struck = (LivingEntity) e;
					}
				}
				if (struck != null) {
					done[0] = true;
					hits.strike(struck, ArtRules.NEEDLE_FACTOR, AuraFxRules.Weight.FULL);
					ArtWards.star(player, struck);
					ArtKit.giveBack(player, ArtRules.NEEDLE_AURA);
					paid.add(struck.getUUID());
					burst(player, struck.getBoundingBox().getCenter(), color, 0.45, index);
					return;
				}
				at[0] = next;
				if (wall || flown[0] >= ArtRules.NEEDLE_RANGE) {
					done[0] = true;
					burst(player, at[0], color, 0.4, index);
				}
			});
		}
	}

	// ------------------------------------------------------------------ II. Meteor Shower

	static boolean meteorShower(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_meteor_shower", 1.1F, 1.0F);
		LivingEntity struck = context.struck();
		Vec3 centre;
		if (struck != null && struck.isAlive() && struck.distanceToSqr(player) < 7 * 7) {
			Vec3 g = ArtKit.floor(level, struck.position().add(0, 0.5, 0), 1.0, 3);
			centre = g == null ? struck.position() : g;
		} else {
			Vec3 ahead = feet.add(look.scale(ArtRules.METEOR_AHEAD));
			Vec3 g = ArtKit.floor(level, ahead.add(0, 1, 0), 1.5, 3);
			centre = g == null ? ahead : g;
		}
		ArtLight world = ArtLight.world(player);
		world.ground(centre, SigilOption.TARGET, color, ArtRules.METEOR_RADIUS, ArtRules.METEOR_TICKS + 14, 0.05);
		world.ground(centre, SigilOption.STAR, VIOLET, ArtRules.METEOR_RADIUS * 0.6, ArtRules.METEOR_TICKS + 14, -0.08);
		Map<UUID, Integer> struckBy = new HashMap<>();
		Set<UUID> paid = new HashSet<>();
		RandomSource r = level.getRandom();
		for (int i = 0; i < ArtRules.METEOR_STARS; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(r.nextDouble()) * ArtRules.METEOR_RADIUS * (i == 0 ? 0.3 : 0.9);
			Vec3 drop = centre.add(Math.cos(a) * d, 0, Math.sin(a) * d);
			int delay = 1 + i * ArtRules.METEOR_TICKS / (ArtRules.METEOR_STARS + 1);
			int note = i;
			comet(player, drop, look, color, 0.13, delay, () -> {
				Vec3 g = ArtKit.floor(level, drop.add(0, 1, 0), 1.5, 3);
				Vec3 p = g == null ? drop : g;
				burst(player, p.add(0, 0.25, 0), color, 0.9, note);
				ArtLight.world(player).groundRing(p, color, 0.1, ArtRules.METEOR_REACH, 0.05, 7);
				for (LivingEntity foe : ArtKit.around(player, p, ArtRules.METEOR_REACH, 1.0, 3.0, 4)) {
					int n = struckBy.getOrDefault(foe.getUUID(), 0);
					if (n >= ArtRules.METEOR_PER_FOE) {
						continue;
					}
					struckBy.put(foe.getUUID(), n + 1);
					hits.strike(foe, ArtRules.METEOR_FACTOR, AuraFxRules.Weight.LIGHT);
					// Pinned under the shower (each star's knock would scatter them out from under the great one).
					ArtKit.steady(foe);
					if (paid.size() < ArtRules.METEOR_AURA_FOES && paid.add(foe.getUUID())) {
						ArtKit.giveBack(player, ArtRules.METEOR_AURA);
					}
				}
			});
		}
		// The great star, last, on the heart of it: struck harder, and starred.
		comet(player, centre, look, color, 0.28, ArtRules.METEOR_TICKS, () -> {
			burst(player, centre.add(0, 0.4, 0), color, 1.7, 5);
			ArtLight w = ArtLight.world(player);
			w.groundRing(centre, color, 0.2, ArtRules.METEOR_GREAT_REACH * 1.3, 0.16, 10);
			w.groundRing(centre, PALE, 0.2, ArtRules.METEOR_GREAT_REACH, 0.05, 8);
			ScreenFx.shake(level, centre, 0.15F, 10);
			AuraFx.sound(player, AuraFx.Sound.IMPACT, 0.8F, 0.9F);
			for (LivingEntity foe : ArtKit.around(player, centre, ArtRules.METEOR_GREAT_REACH, 1.0, 3.0, 6)) {
				hits.strike(foe, ArtRules.METEOR_GREAT, AuraFxRules.Weight.HEAVY);
				ArtWards.star(player, foe);
				if (paid.size() < ArtRules.METEOR_AURA_FOES && paid.add(foe.getUUID())) {
					ArtKit.giveBack(player, ArtRules.METEOR_AURA);
				}
			}
		});
		return true;
	}

	/**
	 * A star falling to {@code drop}, starting {@code delay} ticks on: out of the sky high behind the swordsman, streaking down over
	 * three ticks with a tail of light, and {@code landed} where it strikes.
	 */
	private static void comet(ServerPlayer player, Vec3 drop, Vec3 look, int color, double size, int delay, Runnable landed) {
		ServerLevel level = player.level();
		Vec3 sky = drop.add(look.scale(-2.5)).add(0, 11, 0);
		for (int t = 0; t <= 3; t++) {
			int step = t;
			Scheduler.later(delay + t, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				ArtLight world = ArtLight.world(player);
				Vec3 head = sky.lerp(drop, step / 3.0);
				Vec3 tail = sky.lerp(drop, Math.max(0, step - 1.6) / 3.0);
				world.ray(tail, head, color, size * 1.4, 4);
				world.bare().ray(tail, head, WHITE, size * 0.5, 3);
				world.bare().orb(head, WHITE, size, 3);
				if (step == 3) {
					landed.run();
				}
			});
		}
	}

	// ------------------------------------------------------------------ III. Constellation Guard

	/** The constellation's stars round a foe, in its own frame: across, up and toward (blocks from its middle). */
	private static final double[][] CONSTELLATION = {{-1.05, 0.7, 0.1}, {0.15, 1.3, -0.2}, {1.1, 0.45, 0.15}, {0.35, -0.5, 0.3}};

	static boolean constellationGuard(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		LivingEntity foe = ArtKit.attacker(player, context, 4.0);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CROSS, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_constellation_guard", 1.0F, 1.0F);
		AuraFx.burst(level, player, ArtKit.hand(player), ArtKit.flat(player), color, 1.3F, AuraFx.Burst.STAR | AuraFx.Burst.FLASH);
		// Every foe near already carrying a star of yours bursts at once.
		int note = 0;
		for (LivingEntity other : ArtKit.around(player, player.position(), ArtRules.CONSTELLATION_REACH, 2.0, 3.0, 8)) {
			if (other != foe && ArtWards.burstStar(player, other)) {
				hits.strike(other, ArtRules.CONSTELLATION_STARRED, AuraFxRules.Weight.FULL);
				ArtKit.giveBack(player, ArtRules.CONSTELLATION_AURA);
				burst(player, other.getBoundingBox().getCenter().add(0, 0.3, 0), color, 1.0, note++);
			}
		}
		if (foe == null) {
			return true;
		}
		hits.strike(foe, ArtRules.CONSTELLATION_FACTOR);
		AuraPhysicalFx.arcaneImpact(level, foe.getBoundingBox().getCenter(), 0.9);
		if (ArtWards.burstStar(player, foe)) {
			hits.strike(foe, ArtRules.CONSTELLATION_STARRED, AuraFxRules.Weight.FULL);
			ArtKit.giveBack(player, ArtRules.CONSTELLATION_AURA);
		}
		// The constellation, set on it: stars round its body joined by threads of light, turning with it until they burst.
		Vec3[] last = {foe.getBoundingBox().getCenter()};
		for (int t = 0; t < ArtRules.CONSTELLATION_DELAY; t += 4) {
			Scheduler.later(t, () -> {
				if (foe.isAlive()) {
					last[0] = foe.getBoundingBox().getCenter();
				}
				drawConstellation(player, last[0], color, ArtRules.CONSTELLATION_STARS, 5);
			});
		}
		for (int s = 0; s < ArtRules.CONSTELLATION_STARS; s++) {
			int star = s;
			Scheduler.later(ArtRules.CONSTELLATION_DELAY + s * ArtRules.CONSTELLATION_GAP, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				if (foe.isAlive()) {
					last[0] = foe.getBoundingBox().getCenter();
				}
				Vec3 at = starAt(last[0], player, star);
				burst(player, at, color, 1.0, star + 2);
				drawConstellation(player, last[0], color, ArtRules.CONSTELLATION_STARS - star - 1, 4);
				if (foe.isAlive()) {
					hits.strike(foe, ArtRules.CONSTELLATION_BURST, star == ArtRules.CONSTELLATION_STARS - 1 ? AuraFxRules.Weight.HEAVY : AuraFxRules.Weight.FULL);
				} else {
					// Its foe fell: the star bursts where it was, on whoever stands near.
					for (LivingEntity near : ArtKit.around(player, at, ArtRules.CONSTELLATION_SPLASH, 1.5, 2.5, 3)) {
						hits.strike(near, ArtRules.CONSTELLATION_BURST, AuraFxRules.Weight.FULL);
					}
				}
				ArtKit.giveBack(player, ArtRules.CONSTELLATION_AURA);
			});
		}
		return true;
	}

	/** Where star {@code index} of a constellation round {@code centre} stands, turned to face the swordsman. */
	private static Vec3 starAt(Vec3 centre, ServerPlayer player, int index) {
		Vec3 toward = player.position().subtract(centre);
		toward = new Vec3(toward.x, 0, toward.z);
		toward = toward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : toward.normalize();
		Vec3 across = new Vec3(-toward.z, 0, toward.x);
		double[] p = CONSTELLATION[index % CONSTELLATION.length];
		return centre.add(across.scale(p[0])).add(0, p[1], 0).add(toward.scale(p[2] + 0.35));
	}

	/** The first {@code count} stars of a constellation round {@code centre}, and the threads joining them. */
	private static void drawConstellation(ServerPlayer player, Vec3 centre, int color, int count, int life) {
		ArtLight world = ArtLight.world(player);
		Vec3 prev = null;
		int first = ArtRules.CONSTELLATION_STARS - count;
		for (int i = first; i < ArtRules.CONSTELLATION_STARS; i++) {
			Vec3 at = starAt(centre, player, i);
			Vec3 facing = at.subtract(player.getEyePosition());
			world.bare().orb(at, WHITE, 0.12, Math.min(4, life));
			world.flash(at, PALE, 0.7F);
			world.sigil(at, facing, SigilOption.STAR, color, 0.42, life + 1, 0.0);
			if (prev != null) {
				world.ray(prev, at, color, 0.07, life);
				world.bare().ray(prev, at, WHITE, 0.025, life);
			}
			prev = at;
		}
	}

	// ------------------------------------------------------------------ IV. Comet Dash

	static boolean cometDash(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.COMET_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, COMET_DASH);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		AuraStep.afterimages(player, from, to, dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_comet_dash", 1.05F, 1.0F);
		// The stars it leaves: one every block and a bit along the way, waiting to burst.
		List<Vec3> stars = new ArrayList<>();
		double length = from.distanceTo(to);
		Vec3 along = to.subtract(from).normalize();
		for (double d = 0.6; d <= length + 0.01; d += 1.2) {
			stars.add(from.add(along.scale(d)).add(0, 0.7, 0));
		}
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, ArtRules.COMET_TICKS, (a, b, step, last) -> {
			// The comet's tail: a pink streak with a white heart, sparks of starlight thrown off it.
			world.ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), color, 0.32, 10);
			world.bare().ray(a.add(0, 0.9, 0), b.add(0, 0.9, 0), WHITE, 0.08, 8);
			Motes.glows(level, b.add(0, 0.9, 0), 4, 0.4, PALE, 0.07, 16, Vec3.ZERO, 0.02);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, ArtRules.COMET_WIDTH / 2 + 0.3, 2.2,
					ArtRules.COMET_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= ArtRules.COMET_TARGETS) {
					continue;
				}
				hits.strike(foe, ArtRules.COMET_FACTOR);
				ArtWards.star(player, foe);
				AuraPhysicalFx.arcaneImpact(level, foe.getBoundingBox().getCenter(), 0.7);
			}
		});
		// The trail of stars hanging where it ran, twinkling while they wait: each a five-pointed star of light standing across the
		// way (so it shows its face to anyone looking along the trail), a bright heart and a glow.
		Scheduler.later(ArtRules.COMET_TICKS, () -> {
			ArtLight w = ArtLight.world(player);
			for (int i = 0; i < stars.size(); i++) {
				w.sigil(stars.get(i), along, SigilOption.STAR, color, 0.62, ArtRules.COMET_DELAY + 1 + i, i % 2 == 0 ? 0.08 : -0.08);
			}
		});
		for (int t = ArtRules.COMET_TICKS; t < ArtRules.COMET_TICKS + ArtRules.COMET_DELAY; t += 3) {
			int tick = t;
			Scheduler.later(t, () -> {
				ArtLight w = ArtLight.world(player);
				for (int i = 0; i < stars.size(); i++) {
					Vec3 s = stars.get(i);
					w.bare().orb(s, WHITE, (i + tick / 3) % 2 == 0 ? 0.1 : 0.07, 4);
					w.flash(s, PALE, (i + tick / 3) % 2 == 0 ? 0.9F : 0.6F);
				}
			});
		}
		// Then it bursts, star by star from where you set off, on everything near each one.
		Set<UUID> caught = new HashSet<>();
		int[] paid = {0};
		Scheduler.later(ArtRules.COMET_TICKS + ArtRules.COMET_DELAY, () -> Feels.sound(level, from.add(to).scale(0.5), "aura_art_comet_dash_burst", 1.1F, 1.0F));
		for (int i = 0; i < stars.size(); i++) {
			Vec3 s = stars.get(i);
			int note = i;
			Scheduler.later(ArtRules.COMET_TICKS + ArtRules.COMET_DELAY + i, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				burst(player, s, ArtKit.color(player), 1.15, note);
				for (LivingEntity foe : ArtKit.around(player, s.subtract(0, 0.7, 0), ArtRules.COMET_BURST_REACH, 1.0, 2.5, 6)) {
					if (!caught.add(foe.getUUID())) {
						continue;
					}
					hits.strike(foe, ArtRules.COMET_BURST, AuraFxRules.Weight.HEAVY);
					if (paid[0] < ArtRules.COMET_AURA_FOES) {
						paid[0]++;
						ArtKit.giveBack(player, ArtRules.COMET_AURA);
					}
				}
			});
		}
		return true;
	}

	// ------------------------------------------------------------------ V. Nova

	static boolean nova(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_nova", 1.3F, 1.0F);
		// The gathering: starlight drawn in to the blade from all round, stars wheeling round you (seen from outside), and you held a
		// moment in the stance.
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ArtRules.NOVA_GATHER, 2, false, false, true));
		fx.flare(ArtRules.NOVA_GATHER + 10, 1.0F);
		ArtLight show = ArtLight.spectacle(player);
		for (int t = 0; t < ArtRules.NOVA_GATHER; t += 2) {
			int tick = t;
			Scheduler.later(1 + t, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				RandomSource r = level.getRandom();
				Vec3 hand = ArtKit.hand(player);
				for (int i = 0; i < 3; i++) {
					Vec3 around = player.position().add((r.nextDouble() - 0.5) * 5, 0.2 + r.nextDouble() * 2.6, (r.nextDouble() - 0.5) * 5);
					Motes.seek(level, around, hand, i % 2 == 0 ? PALE : color, 0.08, 6, 0.3);
				}
				show.bare().orb(hand.add(ArtKit.flat(player).scale(0.35)), WHITE, 0.14 + 0.025 * tick, 3);
				double a = tick * 0.5;
				show.bare().slash(player.position().add(0, 1.0, 0), ElementFx.tilted(0.6, a), ElementFx.inPlane(ElementFx.tilted(0.6, a), a * 2), color, 1.4,
					Math.PI * 1.3, 0.06, 2, 5);
				if (tick % 4 == 0) {
					AuraFx.burst(level, player, hand, Vec3.ZERO, color, 0.8F, AuraFx.Burst.SPARKS | AuraFx.Burst.STAR);
				}
			});
		}
		Scheduler.later(ArtRules.NOVA_GATHER, () -> {
			if (player.isAlive() && player.level() == level) {
				release(player, hits, color);
			}
		});
		return true;
	}

	/** The nova: a ring of starlight bursting out over everything near, starred foes bursting with it, aura back for each foe struck. */
	private static void release(ServerPlayer player, ArtKit.Hits hits, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		hits.fx().trail(AuraFxRules.Stroke.SPIN, false, 1.7F);
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		world.groundRing(feet, color, 0.5, ArtRules.NOVA_RADIUS * 1.15, 0.42, 14);
		world.groundRing(feet, WHITE, 0.4, ArtRules.NOVA_RADIUS, 0.12, 12);
		world.groundRing(feet, VIOLET, 0.6, ArtRules.NOVA_RADIUS * 1.3, 0.06, 16);
		world.ground(feet, SigilOption.STAR, color, 3.2, 40, 0.06);
		world.ground(feet, SigilOption.BAND, VIOLET, 4.4, 40, -0.04);
		// Rays of starlight shot out low over the ground (yours to see too: they run out from your feet, under the view).
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 8; i++) {
			double a = phase + Math.PI * 2 * i / 8;
			Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
			world.ray(feet.add(out.scale(2.5)).add(0, 0.12, 0), feet.add(out.scale(ArtRules.NOVA_RADIUS)).add(0, 0.12, 0), i % 2 == 0 ? color : PALE, 0.11, 12);
		}
		// About the body, seen from outside: a star flaring, a column of light.
		Vec3 chest = feet.add(0, 1.2, 0);
		show.flash(chest, WHITE, 4.5F);
		show.sigil(chest, ArtKit.flat(player), SigilOption.STAR, color, 3.0, 14, 0.25);
		show.ray(feet, feet.add(0, 8, 0), color, 0.6, 12);
		show.bare().ray(feet, feet.add(0, 7, 0), WHITE, 0.18, 10);
		Motes.burst(level, chest, 30, PALE, 0.09, 24, 0.5);
		ScreenFx.shake(level, feet, 0.3F, 14);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.8F);
		int struck = 0;
		int note = 0;
		for (LivingEntity foe : ArtKit.around(player, feet, ArtRules.NOVA_RADIUS, 1.5, 3.5, ArtRules.NOVA_TARGETS)) {
			double d = foe.position().subtract(feet).horizontalDistance();
			float took = hits.strike(foe, ArtRules.falloff(ArtRules.NOVA_CENTRE, ArtRules.NOVA_EDGE, d, ArtRules.NOVA_RADIUS), AuraFxRules.Weight.GRAND);
			if (ArtWards.burstStar(player, foe)) {
				hits.strike(foe, ArtRules.NOVA_STARRED, AuraFxRules.Weight.FULL);
				burst(player, foe.getBoundingBox().getCenter().add(0, 0.4, 0), color, 1.2, note++);
			}
			if (took > 0 || hits.hurt(foe)) {
				struck++;
			}
			ArtKit.knock(foe, feet, ArtRules.NOVA_THROW, 0.3);
		}
		ArtKit.giveBack(player, ArtRules.novaAura(struck));
	}
}
