package dev.wildercord.cast;

import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;

import static dev.wildercord.cast.FusedStormNumbers.*;

/**
 * Fused effects of storm and wind, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedStormVfx}. Numbers match the rune descriptions in
 * {@code Runes} (see {@link FusedStormNumbers}).
 * <ul>
 *   <li><b>Riftbolt</b> (storm and void): a black bolt, and the target torn through a rift to somewhere safe up to 5 blocks on.</li>
 *   <li><b>Stormweave</b> (storm and arcane): up to four enemies marked, then a web of lightning between them all.</li>
 *   <li><b>Stormclock</b> (storm and time): a clock in the sky over the spot, striking it again at 2 and 4 seconds.</li>
 *   <li><b>Heartstopper</b> (storm and blood): a shock to the heart, which then skips (a short stun) every 2 seconds.</li>
 *   <li><b>Thunderhead</b> (storm with storm): a thundercloud that follows its target, striking an enemy under it every second.</li>
 *   <li><b>Downdraft</b> (wind and earth): every airborne enemy around slammed into the ground, harder the further it fell.</li>
 *   <li><b>Updraft</b> (wind with wind): a column of wind throws enemies up, and a downdraft smashes them down.</li>
 *   <li><b>Skyglyph</b> (wind and arcane): a glyph on the ground that launches allies and throws enemies off.</li>
 *   <li><b>Recoil</b> (wind and time): targets hurled back, then snapped back to where they stood.</li>
 * </ul>
 * Creatures are only ever moved somewhere they fit, out of lava and fire and over solid ground, and never a boss
 * (see {@link Spirits#isBoss}); if there's no such place they stay put. Nothing here outlives the server: every
 * table is emptied when it stops, and each entry goes when its effect ends.
 */
final class FusedStorm {
	private FusedStorm() {}

	/** At most this many targets get a lingering or moving part of their own, so one hit can't flood the server. */
	private static final int MAX_TARGETS = 8;
	/** Stormclocks and thunderclouds one landing may raise. */
	private static final int MAX_SPOTS = 4;

	/** An effect of one caster's on one creature: refreshed or added to by a second, never stacked beside it. */
	private record Key(UUID target, UUID caster) {}

	/** Heartstopper: the current skipping heart on each creature (a newer one replaces it). */
	private static final Map<Key, Object> HEARTS = new HashMap<>();
	/** Recoil: where each creature is to be snapped back to, and what the snap will deal. */
	private static final Map<Key, Snap> RECOILS = new HashMap<>();
	/** Updraft: the smash each thrown creature has coming (more updrafts before it only add to it). */
	private static final Map<Key, double[]> UPDRAFTS = new HashMap<>();
	/** Stormclock: each caster's clocks still ticking, oldest first (each one is only a flag: set, it stops). */
	private static final Map<UUID, Deque<boolean[]>> CLOCKS = new HashMap<>();
	/** Thunderhead: each caster's clouds, oldest first. */
	private static final Map<UUID, List<Cloud>> CLOUDS = new HashMap<>();
	/** Skyglyph: each caster's glyphs, oldest first. */
	private static final Map<UUID, Deque<Glyph>> GLYPHS = new HashMap<>();
	/** When a glyph last launched or threw each creature. */
	private static final Map<UUID, Long> SPRUNG = new HashMap<>();
	/** Creatures the wind is carrying (launched by a glyph, thrown up by an updraft): their next fall doesn't hurt, until this game time. */
	private static final Map<UUID, Long> CUSHIONED = new HashMap<>();

	/** How fast to throw a creature so it lands a whole number of blocks away (0 to 6), worked out once. */
	private static final double[] THROWS = new double[7];

	static {
		for (int blocks = 1; blocks < THROWS.length; blocks++) {
			THROWS[blocks] = throwSpeed(blocks);
		}
	}

	private record Snap(Vec3 anchor, double[] damage) {}

	/** A thundercloud: over a creature (it follows it) or over a spot. */
	private static final class Cloud {
		final UUID target;
		Vec3 centre;
		long until;
		double power;
		double reach;
		boolean over;

		Cloud(UUID target, Vec3 centre, long until, double power, double reach) {
			this.target = target;
			this.centre = centre;
			this.until = until;
			this.power = power;
			this.reach = reach;
		}
	}

	/** A wind glyph on the ground. */
	private static final class Glyph {
		final ServerLevel level;
		final Vec3 feet;
		final double power;
		final long until;
		boolean over;

		Glyph(ServerLevel level, Vec3 feet, double power, long until) {
			this.level = level;
			this.feet = feet;
			this.power = power;
			this.until = until;
		}
	}

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
		// A creature the wind is carrying lands without hurting itself: the spell deals what it says, and an ally's
		// glyph never costs you a fall. One cheap lookup, only while someone is being carried.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (CUSHIONED.isEmpty() || !source.is(DamageTypes.FALL)) {
				return true;
			}
			Long until = CUSHIONED.remove(entity.getUUID());
			return until == null || until < entity.level().getGameTime();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	/** Forgets everything (the server stopped, and its scheduled parts with it). */
	static void clear() {
		HEARTS.clear();
		RECOILS.clear();
		UPDRAFTS.clear();
		CLOCKS.clear();
		CLOUDS.clear();
		GLYPHS.clear();
		SPRUNG.clear();
		CUSHIONED.clear();
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		double radius = SpellNumbers.effectRadius(node);
		switch (node.effect.path()) {
			case "riftbolt" -> riftbolt(cast, hit, harmed, power, duration);
			case "stormweave" -> stormweave(cast, hit, harmed, WEAVE_RADIUS * radius, power);
			case "stormclock" -> stormclock(cast, hit, harmed, CLOCK_RADIUS * radius, power);
			case "heartstopper" -> {
				for (int i = 0; i < harmed.size(); i++) {
					heartstopper(cast, hit, harmed.get(i), power, duration, i < MAX_TARGETS);
				}
			}
			case "thunderhead" -> thunderhead(cast, hit, harmed, CLOUD_REACH * radius, power, Effects.ticks(CLOUD_SECONDS, duration));
			case "downdraft" -> downdraft(cast, hit, DOWNDRAFT_RADIUS * radius, power);
			case "updraft" -> updraft(cast, hit, harmed, UPDRAFT_RADIUS * radius, power);
			case "skyglyph" -> skyglyph(cast, hit, power, Effects.ticks(GLYPH_SECONDS, duration));
			case "recoil" -> first(harmed, MAX_TARGETS).forEach(t -> recoil(cast, hit, t, power));
			default -> {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ Riftbolt

	/**
	 * Riftbolt: a black bolt, darkness, and a rift that tears the target through to somewhere safe further on.
	 * Every target takes the bolt; only the first few of a crowd are torn through.
	 */
	private static void riftbolt(Cast cast, Cast.Hit hit, List<LivingEntity> targets, double power, double duration) {
		ServerLevel level = cast.level;
		if (targets.isEmpty()) {
			if (!hit.self()) {
				FusedStormVfx.riftFizzle(level, point(cast, hit));
			}
			return;
		}
		int drawn = 0;
		for (LivingEntity t : targets) {
			// Only the first few get the whole show; a crowd still gets every part of the spell.
			boolean full = drawn < 4;
			boolean torn = drawn++ < MAX_TARGETS;
			Vec3 from = t.position();
			Vec3 dir = Effects.horizontal(heading(cast, hit), from.subtract(origin(cast, hit)));
			if (torn) {
				FusedStormVfx.riftbolt(level, origin(cast, hit), t, full);
			}
			Effects.hurt(cast, t, lightning(cast), RIFTBOLT_DAMAGE * power * Reactions.storm(cast, t));
			if (!onHand(cast, t)) {
				continue;
			}
			t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Effects.ticks(RIFT_DARKNESS_SECONDS, duration), 0, false, true), cast.caster);
			if (!torn) {
				continue;
			}
			Vec3 exit = movable(t) ? riftExit(level, t, dir) : null;
			if (exit == null) {
				// The rift opens on it and can't take it (a boss, or nowhere safe to put it): it holds.
				FusedStormVfx.tear(level, from, t.getBbHeight(), dir, true, false);
				continue;
			}
			FusedStormVfx.tear(level, from, t.getBbHeight(), dir, true, full);
			move(t, level, exit);
			still(t);
			FusedStormVfx.tear(level, exit, t.getBbHeight(), dir, false, full);
		}
	}

	/**
	 * Where a rift carries {@code t}: up to {@link FusedStormNumbers#RIFT_REACH} blocks on the way the bolt was
	 * going (give or take), the farthest safe spot it can see, within 3 blocks up or down. Null if there's none.
	 */
	private static Vec3 riftExit(ServerLevel level, LivingEntity t, Vec3 dir) {
		Vec3 from = t.position();
		Vec3 middle = from.add(0, t.getBbHeight() * 0.5, 0);
		RandomSource r = level.getRandom();
		double base = Math.atan2(dir.z, dir.x);
		for (double d = RIFT_REACH; d >= 2; d -= 1) {
			for (int attempt = 0; attempt < 3; attempt++) {
				double a = base + (r.nextDouble() - 0.5) * 1.6;
				Vec3 feet = landing(level, from.add(Math.cos(a) * d, 2.0, Math.sin(a) * d), 5.0);
				if (feet == null || Math.abs(feet.y - from.y) > 3 || feet.distanceTo(from) > RIFT_REACH + 0.5 || !safe(level, t, feet)
						|| !clear(level, t, middle, feet.add(0, t.getBbHeight() * 0.5, 0))) {
					continue;
				}
				return feet;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ Stormweave

	/** Stormweave: the struck and the nearest enemies around (up to four) are marked, then lightning weaves between them all. */
	private static void stormweave(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power) {
		ServerLevel level = cast.level;
		Vec3 point = point(cast, hit);
		List<LivingEntity> web = new ArrayList<>();
		for (LivingEntity t : harmed) {
			if (web.size() < WEAVE_MAX && !web.contains(t)) {
				web.add(t);
			}
		}
		if (web.size() < WEAVE_MAX) {
			List<LivingEntity> near = enemiesAround(cast, point, radius, 32);
			near.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(point)));
			for (LivingEntity e : near) {
				if (web.size() >= WEAVE_MAX) {
					break;
				}
				if (!web.contains(e)) {
					web.add(e);
				}
			}
		}
		if (web.isEmpty()) {
			FusedStormVfx.weaveFizzle(level, point);
			return;
		}
		FusedStormVfx.weaveMarks(level, web, WEAVE_DELAY);
		Scheduler.later(WEAVE_DELAY, () -> {
			if (!cast.alive()) {
				return;
			}
			// Whoever is still there (and still an enemy) is caught; one that fled far is let go.
			List<LivingEntity> caught = new ArrayList<>();
			for (LivingEntity t : web) {
				if (onHand(cast, t) && Targets.canHarm(cast.caster, t) && t.position().distanceTo(point) <= radius + 8) {
					caught.add(t);
				}
			}
			if (caught.isEmpty()) {
				return;
			}
			FusedStormVfx.weave(level, caught);
			// A lone target has no web to run through, so the lightning anchors to the ground through it: 2 more.
			double each = (weaveDamage(caught.size()) + (caught.size() == 1 ? WEAVE_ANCHOR : 0)) * power;
			for (LivingEntity t : caught) {
				Effects.hurt(cast, t, lightning(cast), each * Reactions.storm(cast, t));
			}
		});
	}

	// ------------------------------------------------------------------ Stormclock

	/** Stormclock: lightning now, and a clock in the sky over each spot that strikes it again at 2 and 4 seconds. */
	private static void stormclock(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power) {
		ServerLevel level = cast.level;
		List<Vec3> spots = new ArrayList<>();
		for (LivingEntity t : first(harmed, MAX_TARGETS)) {
			Vec3 feet = t.position();
			if (spots.size() < MAX_SPOTS && spots.stream().noneMatch(s -> s.distanceToSqr(feet) < 1.0)) {
				spots.add(feet);
			}
		}
		if (spots.isEmpty()) {
			Vec3 ground = ElementFx.floor(level, point(cast, hit), 6);
			spots.add(ground != null ? ground : point(cast, hit));
		}
		for (Vec3 spot : spots) {
			clock(cast, spot, radius, power);
		}
		// The first strike lands on every target; only the clocks are few.
		for (LivingEntity t : harmed) {
			Effects.hurt(cast, t, lightning(cast), CLOCK_FIRST_DAMAGE * power * Reactions.storm(cast, t));
		}
	}

	/** One stormclock: its face opens with the first strike, its hand ticks round, and at each hour it strikes the spot again. */
	private static void clock(Cast cast, Vec3 spot, double radius, double power) {
		ServerLevel level = cast.level;
		Vec3 face = sky(level, spot, 4.5);
		Vec3 toCaster = Effects.horizontal(cast.caster.position().subtract(spot), new Vec3(0, 0, 1));
		// Hung facing down, tipped a little toward whoever cast it, so they can read it.
		Vec3 normal = new Vec3(toCaster.x * 0.45, -1, toCaster.z * 0.45).normalize();
		double size = Math.max(1.2, radius * 1.15);
		// A caster keeps a few clocks going at once; one more stops the oldest.
		UUID owner = cast.caster.getUUID();
		Deque<boolean[]> clocks = CLOCKS.computeIfAbsent(owner, k -> new ArrayDeque<>());
		while (clocks.size() >= CLOCKS_MAX) {
			clocks.pollFirst()[0] = true;
		}
		boolean[] stopped = {false};
		clocks.addLast(stopped);
		FusedStormVfx.clockOpen(level, face, normal, spot, size);
		int step = CLOCK_STRIKES[0] / CLOCK_STEPS;
		int last = CLOCK_STRIKES[CLOCK_STRIKES.length - 1];
		every(cast, step, step, age -> {
			if (stopped[0]) {
				return false;
			}
			boolean strike = false;
			for (int at : CLOCK_STRIKES) {
				strike |= age == at;
			}
			if (!strike) {
				int hand = (age % CLOCK_STRIKES[0]) / step;
				FusedStormVfx.clockTick(level, face, normal, spot, size, hand, CLOCK_STEPS);
				return true;
			}
			FusedStormVfx.clockStrike(level, face, normal, spot, size, age >= last);
			Effects.lingering(() -> {
				for (LivingEntity e : enemiesUnder(cast, spot, radius)) {
					Effects.hurt(cast, e, lightning(cast), CLOCK_STRIKE_DAMAGE * power * Reactions.storm(cast, e));
				}
			});
			return age < last;
		}, () -> {
			stopped[0] = true;
			clocks.remove(stopped);
			if (clocks.isEmpty()) {
				CLOCKS.remove(owner, clocks);
			}
		});
	}

	// ------------------------------------------------------------------ Heartstopper

	/**
	 * Heartstopper: a shock to the heart, and for a while every second beat is skipped: a short stun. Every
	 * target is shocked; only if {@code skipping} (the first few of a crowd) does its heart go on skipping.
	 */
	private static void heartstopper(Cast cast, Cast.Hit hit, LivingEntity t, double power, double duration, boolean skipping) {
		ServerLevel level = cast.level;
		FusedStormVfx.heartstopper(level, origin(cast, hit), t);
		Effects.hurt(cast, t, lightning(cast), HEART_DAMAGE * power * Reactions.storm(cast, t));
		if (!skipping || !onHand(cast, t)) {
			return;
		}
		int skips = heartSkips(Effects.ticks(HEART_SECONDS, duration));
		if (skips <= 0) {
			return;
		}
		// One skipping heart per caster and creature: a second Heartstopper (or a Linger) starts it over.
		Key key = new Key(t.getUUID(), cast.caster.getUUID());
		Object token = new Object();
		HEARTS.put(key, token);
		int end = skips * HEART_EVERY;
		every(cast, 20, 20, age -> {
			if (HEARTS.get(key) != token || !onHand(cast, t)) {
				return false;
			}
			if (age % HEART_EVERY == 0) {
				Spirits.hold(t, HEART_STUN);
				FusedStormVfx.heartSkip(level, t);
			} else {
				FusedStormVfx.heartbeat(level, t);
			}
			return age < end;
		}, () -> HEARTS.remove(key, token));
	}

	// ------------------------------------------------------------------ Thunderhead

	/** Thunderhead: a thundercloud over each target (or the spot), striking an enemy under it every second. */
	private static void thunderhead(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double reach, double power, int ticks) {
		List<LivingEntity> targets = first(harmed, MAX_SPOTS);
		if (targets.isEmpty()) {
			// Over the spot, as high as it would hang over someone standing there.
			Vec3 ground = ElementFx.floor(cast.level, point(cast, hit), 6);
			gather(cast, null, (ground != null ? ground : point(cast, hit)).add(0, 1.8, 0), reach, power, ticks);
		}
		for (LivingEntity t : targets) {
			gather(cast, t, t.position().add(0, t.getBbHeight(), 0), reach, power, ticks);
		}
	}

	/** Gathers a cloud over {@code t} (or over {@code under}), or feeds the one this caster already has over it. */
	private static void gather(Cast cast, LivingEntity t, Vec3 under, double reach, double power, int ticks) {
		ServerLevel level = cast.level;
		UUID owner = cast.caster.getUUID();
		long now = level.getGameTime();
		List<Cloud> clouds = CLOUDS.computeIfAbsent(owner, k -> new ArrayList<>());
		if (t != null) {
			for (Cloud c : clouds) {
				if (!c.over && t.getUUID().equals(c.target)) {
					// Already over it: it grows darker and lasts longer, but it's still one cloud.
					c.until = Math.max(c.until, now + ticks);
					c.power = Math.max(c.power, power);
					c.reach = Math.max(c.reach, reach);
					FusedStormVfx.cloudFed(level, c.centre);
					return;
				}
			}
		}
		while (clouds.size() >= CLOUDS_MAX) {
			Cloud old = clouds.removeFirst();
			old.over = true;
		}
		Cloud cloud = new Cloud(t == null ? null : t.getUUID(), sky(level, under, CLOUD_HEIGHT), now + ticks, power, reach);
		clouds.add(cloud);
		FusedStormVfx.cloudGather(level, cloud.centre);
		every(cast, 2, 2, age -> {
			if (cloud.over || level.getGameTime() > cloud.until) {
				return false;
			}
			// It drifts along over its target; over a spot (or once its target is gone) it stays where it is.
			if (t != null && onHand(cast, t)) {
				Vec3 want = sky(level, t.position().add(0, t.getBbHeight(), 0), CLOUD_HEIGHT);
				cloud.centre = cloud.centre.lerp(want, 0.3);
			}
			if (age % 4 == 0) {
				FusedStormVfx.cloud(level, cloud.centre, age);
			}
			if (age % 20 == 0) {
				strike(cast, cloud);
			}
			return true;
		}, () -> {
			cloud.over = true;
			clouds.remove(cloud);
			if (clouds.isEmpty()) {
				CLOUDS.remove(owner, clouds);
			}
			FusedStormVfx.cloudFade(level, cloud.centre);
		});
	}

	/** One strike from a cloud: an enemy under it that it can reach, if there is one; otherwise it only rumbles. */
	private static void strike(Cast cast, Cloud cloud) {
		ServerLevel level = cast.level;
		Vec3 c = cloud.centre;
		double reach = cloud.reach;
		List<LivingEntity> under = new ArrayList<>();
		AABB box = new AABB(c.x - reach - 1, c.y - 12, c.z - reach - 1, c.x + reach + 1, c.y + 1, c.z + reach + 1);
		for (Entity e : level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e))) {
			if (under.size() >= 16) {
				break;
			}
			LivingEntity le = (LivingEntity) e;
			if (flat(le.position(), c) <= reach + le.getBbWidth() / 2 && clear(level, null, c, le.getBoundingBox().getCenter())) {
				under.add(le);
			}
		}
		if (under.isEmpty()) {
			FusedStormVfx.cloudRumble(level, c);
			return;
		}
		LivingEntity victim = under.get(level.getRandom().nextInt(under.size()));
		FusedStormVfx.cloudStrike(level, c, victim);
		// The cloud's own rain soaks whoever it strikes, so its bolts Conduct (+50%, arcs on to two more).
		Reactions.mark(victim, Reactions.Mark.WET, 40);
		Effects.lingering(() -> Effects.hurt(cast, victim, lightning(cast), CLOUD_STRIKE_DAMAGE * cloud.power * Reactions.storm(cast, victim)));
	}

	// ------------------------------------------------------------------ Downdraft

	/** Downdraft: a downburst over the point slams every airborne enemy around it into the ground. */
	private static void downdraft(Cast cast, Cast.Hit hit, double radius, double power) {
		ServerLevel level = cast.level;
		Vec3 point = point(cast, hit);
		FusedStormVfx.downburst(level, point, radius);
		int slammed = 0;
		for (Entity e : level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			if (slammed >= MAX_TARGETS) {
				break;
			}
			if (t.getBoundingBox().getCenter().distanceTo(point) > radius + t.getBbWidth() / 2 || !airborne(level, t)) {
				continue;
			}
			slammed++;
			slam(cast, t, power);
		}
		if (slammed == 0) {
			// Nothing in the air to swat: the downburst pins whoever stands under it (Slowness III 1 s, 2 damage).
			int pinned = 0;
			for (Entity e : level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity t = (LivingEntity) e;
				if (pinned >= MAX_TARGETS) {
					break;
				}
				if (t.getBoundingBox().getCenter().distanceTo(point) > radius + t.getBbWidth() / 2) {
					continue;
				}
				pinned++;
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DOWNDRAFT_PIN_TICKS, 2, false, true), cast.caster);
				Effects.hurt(cast, t, wind(cast), DOWNDRAFT_PIN_DAMAGE * power);
			}
		}
	}

	/** Slams one creature straight down onto the ground under it, and takes its lift if it flies. */
	private static void slam(Cast cast, LivingEntity t, double power) {
		ServerLevel level = cast.level;
		Vec3 from = t.position();
		Vec3 ground = Spirits.isBoss(t) || !movable(t) ? null : landing(level, from, 48);
		if (ground == null || !safe(level, t, ground)) {
			// Nowhere safe to put it (a boss, lava or the void below): the wind only strikes it where it is.
			FusedStormVfx.buffeted(level, t);
			Effects.hurt(cast, t, wind(cast), SLAM_DAMAGE * power);
			return;
		}
		double fallen = Math.max(0, from.y - ground.y);
		boolean flyer = flyer(t);
		FusedStormVfx.slam(level, from, ground, t, fallen);
		move(t, level, ground);
		Effects.hurt(cast, t, wind(cast), slamDamage(fallen) * power);
		if (onHand(cast, t)) {
			setMotion(t, Vec3.ZERO);
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
			grounded(cast, t, flyer);
		}
	}

	/** Flyers lose their lift: no levitating or slow falling, no gliding for 2 seconds, and a flying mob is pinned where it fell for a moment. */
	private static void grounded(Cast cast, LivingEntity t, boolean flyer) {
		t.removeEffect(MobEffects.LEVITATION);
		t.removeEffect(MobEffects.SLOW_FALLING);
		if (t instanceof Player player) {
			player.stopFallFlying();
			every(cast, 2, 2, age -> {
				if (!onHand(cast, player)) {
					return false;
				}
				if (player.isFallFlying()) {
					player.stopFallFlying();
				}
				return age < 40;
			}, () -> { });
		} else if (flyer) {
			Spirits.hold(t, 30);
		}
	}

	/** Off the ground: not standing, swimming, riding or climbing, with some air under its feet. */
	private static boolean airborne(ServerLevel level, LivingEntity t) {
		return !t.onGround() && !t.isInWater() && !t.isInLava() && !t.isPassenger() && !t.onClimbable() && !t.isSpectator()
			&& landing(level, t.position().add(0, 0.05, 0), 0.4) == null;
	}

	/** Creatures that hold themselves up in the air. */
	private static boolean flyer(LivingEntity t) {
		return t.isNoGravity() || t instanceof Phantom || t instanceof Ghast || t instanceof Vex || t instanceof Blaze
			|| t instanceof Mob mob && mob.getNavigation() instanceof FlyingPathNavigation;
	}

	// ------------------------------------------------------------------ Updraft

	/** Updraft: a column of wind throws the enemies in it high, and a moment later a downdraft smashes them down. */
	private static void updraft(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power) {
		ServerLevel level = cast.level;
		Vec3 ground = ElementFx.floor(level, point(cast, hit), 4);
		Vec3 base = ground != null ? ground : point(cast, hit);
		FusedStormVfx.updraft(level, base, radius);
		List<LivingEntity> caught = new ArrayList<>();
		for (LivingEntity t : harmed) {
			if (caught.size() < MAX_TARGETS && !caught.contains(t)) {
				caught.add(t);
			}
		}
		AABB box = new AABB(base.x - radius - 1, base.y - 1, base.z - radius - 1, base.x + radius + 1, base.y + 3, base.z + radius + 1);
		for (Entity e : level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e))) {
			if (caught.size() >= MAX_TARGETS) {
				break;
			}
			if (!caught.contains(e) && flat(e.position(), base) <= radius + e.getBbWidth() / 2) {
				caught.add((LivingEntity) e);
			}
		}
		UUID owner = cast.caster.getUUID();
		for (LivingEntity t : caught) {
			Key key = new Key(t.getUUID(), owner);
			double[] pending = UPDRAFTS.get(key);
			if (pending != null) {
				// Already on its way up: this one's force goes into the same smash.
				pending[0] += UPDRAFT_DAMAGE * power;
				continue;
			}
			double[] damage = {UPDRAFT_DAMAGE * power};
			UPDRAFTS.put(key, damage);
			if (movable(t) && !Spirits.isBoss(t)) {
				Vec3 v = t.getDeltaMovement();
				fling(t, new Vec3(v.x * 0.2, UPDRAFT_LIFT, v.z * 0.2));
				cushion(t, UPDRAFT_SMASH + 60);
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				// Every spell hits it harder while it's in the air.
				Statuses.airborne(t, UPDRAFT_SMASH + 5);
				FusedStormVfx.hurled(level, t);
			}
			Scheduler.later(UPDRAFT_SMASH, () -> {
				UPDRAFTS.remove(key, damage);
				if (cast.alive() && onHand(cast, t)) {
					smash(cast, t, damage[0]);
				}
			});
		}
	}

	/** The downdraft: straight down onto the ground under it, if that's somewhere it can land. */
	private static void smash(Cast cast, LivingEntity t, double damage) {
		ServerLevel level = cast.level;
		Vec3 from = t.position();
		Vec3 ground = Spirits.isBoss(t) || !movable(t) ? null : landing(level, from, 48);
		boolean down = ground != null && from.y - ground.y > 0.05 && safe(level, t, ground);
		FusedStormVfx.smash(level, from, down ? ground : from, t, down);
		if (down) {
			move(t, level, ground);
		}
		// Down on the ground it's no longer airborne: the smash itself isn't a fifth harder for having lifted it.
		Reactions.clear(t, Reactions.Mark.AIRBORNE);
		Effects.hurt(cast, t, wind(cast), damage);
		if (down && onHand(cast, t)) {
			setMotion(t, Vec3.ZERO);
		}
	}

	// ------------------------------------------------------------------ Skyglyph

	/** Skyglyph: a wind glyph on the ground where it lands, launching allies who step on it and throwing enemies off. */
	private static void skyglyph(Cast cast, Cast.Hit hit, double power, int ticks) {
		ServerLevel level = cast.level;
		Vec3 at = hit.self() ? cast.caster.position() : point(cast, hit);
		Vec3 feet = ElementFx.floor(level, at, 6);
		if (feet == null || ticks <= 0) {
			FusedStormVfx.glyphFizzle(level, at);
			return;
		}
		UUID owner = cast.caster.getUUID();
		Deque<Glyph> glyphs = GLYPHS.computeIfAbsent(owner, k -> new ArrayDeque<>());
		while (glyphs.size() >= GLYPHS_MAX) {
			// A new glyph wipes out the caster's oldest.
			Glyph old = glyphs.pollFirst();
			old.over = true;
			FusedStormVfx.glyphEnd(old.level, old.feet, GLYPH_RADIUS);
		}
		Glyph glyph = new Glyph(level, feet, power, level.getGameTime() + ticks);
		glyphs.addLast(glyph);
		FusedStormVfx.glyphOpen(level, feet, GLYPH_RADIUS, ticks);
		every(cast, 1, GLYPH_CHECK, age -> {
			if (glyph.over || level.getGameTime() > glyph.until) {
				return false;
			}
			if (age % 10 == 1) {
				FusedStormVfx.glyphIdle(level, feet, GLYPH_RADIUS, age);
			}
			spring(cast, glyph);
			return true;
		}, () -> {
			boolean wiped = glyph.over;
			glyph.over = true;
			glyphs.remove(glyph);
			if (glyphs.isEmpty()) {
				GLYPHS.remove(owner, glyphs);
			}
			if (!wiped) {
				FusedStormVfx.glyphEnd(level, feet, GLYPH_RADIUS);
			}
		});
	}

	/** Everyone standing on the glyph right now: allies launched, enemies thrown off, each at most once a second. */
	private static void spring(Cast cast, Glyph glyph) {
		ServerLevel level = cast.level;
		long now = level.getGameTime();
		Vec3 feet = glyph.feet;
		double r = GLYPH_RADIUS;
		AABB box = new AABB(feet.x - r - 1, feet.y - 0.3, feet.z - r - 1, feet.x + r + 1, feet.y + 0.7, feet.z + r + 1);
		for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator())) {
			LivingEntity t = (LivingEntity) e;
			if (flat(t.position(), feet) > r + t.getBbWidth() / 2 || t.getY() < feet.y - 0.3 || t.getY() > feet.y + 0.6 || t.isPassenger()) {
				continue;
			}
			Long last = SPRUNG.get(t.getUUID());
			if (last != null && now - last < GLYPH_REST && now >= last) {
				continue;
			}
			boolean ally = Targets.canHelp(cast.caster, t);
			if (!ally && (!Targets.canHarm(cast.caster, t) || Spirits.isBoss(t))) {
				continue;
			}
			SPRUNG.put(t.getUUID(), now);
			if (ally) {
				// High, and on the way they're facing; the wind sets them down again without a fall.
				Vec3 forward = Effects.horizontal(t.getLookAngle(), cast.caster.getLookAngle());
				double lift = GLYPH_LIFT * Math.min(1.3, Math.sqrt(Math.max(0.5, glyph.power)));
				setMotion(t, forward.scale(GLYPH_FORWARD).add(0, lift, 0));
				t.resetFallDistance();
				cushion(t, 200);
				FusedStormVfx.glyphLaunch(level, feet, t);
			} else {
				Vec3 away = Effects.horizontal(t.position().subtract(feet), t.getLookAngle().scale(-1));
				throwSafely(level, t, away, (int) GLYPH_THROW);
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				FusedStormVfx.glyphThrow(level, feet, t, away);
			}
		}
		if (SPRUNG.size() > 64) {
			SPRUNG.values().removeIf(at -> now - at >= GLYPH_REST || at > now);
		}
	}

	// ------------------------------------------------------------------ Recoil

	/** Recoil: hurled back, and 2 seconds later snapped back by the wind to where it stood. */
	private static void recoil(Cast cast, Cast.Hit hit, LivingEntity t, double power) {
		ServerLevel level = cast.level;
		Key key = new Key(t.getUUID(), cast.caster.getUUID());
		Vec3 away = Effects.horizontal(t.position().subtract(origin(cast, hit)), heading(cast, hit));
		boolean moves = movable(t) && !Spirits.isBoss(t);
		Snap pending = RECOILS.get(key);
		if (moves) {
			throwSafely(level, t, away, (int) RECOIL_THROW);
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
		if (pending != null) {
			// It's already due to be snapped back: this throw adds its force to the snap, and it still goes home to where it first stood.
			pending.damage()[0] += RECOIL_DAMAGE * power;
			FusedStormVfx.recoilHurl(level, t, pending.anchor(), away, false);
			return;
		}
		Snap snap = new Snap(t.position(), new double[] {RECOIL_DAMAGE * power});
		RECOILS.put(key, snap);
		FusedStormVfx.recoilHurl(level, t, snap.anchor(), away, true);
		every(cast, 10, 10, age -> {
			if (RECOILS.get(key) != snap || !onHand(cast, t)) {
				return false;
			}
			if (age < RECOIL_DELAY) {
				FusedStormVfx.recoilTether(level, snap.anchor(), t, age, RECOIL_DELAY);
				return true;
			}
			Vec3 from = t.position();
			Vec3 home = snap.anchor();
			boolean back = moves && movable(t) && from.distanceToSqr(home) > 0.04 && from.distanceTo(home) <= RECOIL_LEASH && safeAnchor(level, t, home);
			FusedStormVfx.recoilSnap(level, from, home, t, back);
			if (back) {
				move(t, level, home);
			}
			Effects.hurt(cast, t, wind(cast), snap.damage()[0]);
			if (back && onHand(cast, t)) {
				setMotion(t, Vec3.ZERO);
			}
			return false;
		}, () -> RECOILS.remove(key, snap));
	}

	/** Whether {@code t} can be put back where it stood: still a safe place, over ground (or it flies). */
	private static boolean safeAnchor(ServerLevel level, LivingEntity t, Vec3 at) {
		return safe(level, t, at) && (flyer(t) || landing(level, at, 4) != null);
	}

	// ------------------------------------------------------------------ helpers

	/**
	 * Runs {@code step} {@code delay} ticks from now, then every {@code period} ticks, for as long as it returns
	 * true and the cast lasts (it's handed the ticks since now); {@code end} runs once when it stops, however it stops.
	 */
	private static void every(Cast cast, int delay, int period, IntPredicate step, Runnable end) {
		int[] age = {delay};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			boolean going = false;
			try {
				going = cast.alive() && step.test(age[0]);
			} finally {
				if (!going) {
					end.run();
				}
			}
			if (going) {
				age[0] += period;
				Scheduler.later(period, next[0]);
			}
		};
		Scheduler.later(delay, next[0]);
	}

	private static List<LivingEntity> first(List<LivingEntity> targets, int n) {
		return targets.size() <= n ? targets : targets.subList(0, n);
	}

	/** Every enemy within {@code radius} of the point (by the middle of its body), at most {@code cap}. */
	private static List<LivingEntity> enemiesAround(Cast cast, Vec3 point, double radius, int cap) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			if (out.size() >= cap) {
				break;
			}
			if (e.getBoundingBox().getCenter().distanceTo(point) <= radius + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	/** Every enemy standing within {@code radius} of a spot on the ground (and not far above or below it). */
	private static List<LivingEntity> enemiesUnder(Cast cast, Vec3 spot, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		AABB box = new AABB(spot.x - radius - 1, spot.y - 1, spot.z - radius - 1, spot.x + radius + 1, spot.y + 3, spot.z + radius + 1);
		for (Entity e : cast.level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e))) {
			if (out.size() >= MAX_TARGETS * 2) {
				break;
			}
			if (flat(e.position(), spot) <= radius + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	private static double flat(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Where it landed: the caster, if the shape didn't say. */
	private static Vec3 point(Cast cast, Cast.Hit hit) {
		return hit.point() != null ? hit.point() : cast.caster.position();
	}

	/** Where the hit came from (push and pull measure from here): the caster, if the shape didn't say. */
	private static Vec3 origin(Cast cast, Cast.Hit hit) {
		return hit.origin() != null ? hit.origin() : cast.caster.position();
	}

	/** Which way the hit was going: the way the caster looks, if the shape didn't say. */
	private static Vec3 heading(Cast cast, Cast.Hit hit) {
		return hit.dir() != null ? hit.dir() : cast.caster.getLookAngle();
	}

	private static boolean onHand(Cast cast, LivingEntity t) {
		return t.isAlive() && !t.isRemoved() && t.level() == cast.level;
	}

	/** Whether a spell may pick this creature up and put it somewhere else (never one riding or ridden). */
	private static boolean movable(LivingEntity t) {
		return t.isAlive() && !t.isPassenger() && !t.isVehicle() && !Spirits.isBoss(t);
	}

	private static DamageSource lightning(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster);
	}

	private static DamageSource wind(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster);
	}

	/**
	 * Where a creature can be put: a loaded place inside the world with room for its body, and nothing molten,
	 * burning, spiked or trapping in it or under its feet.
	 */
	static boolean safe(ServerLevel level, Entity entity, Vec3 feet) {
		BlockPos at = BlockPos.containing(feet);
		if (!level.isLoaded(at) || feet.y < level.getMinY() + 1 || feet.y > level.getMaxY()) {
			return false;
		}
		AABB body = entity.getDimensions(entity.getPose()).makeBoundingBox(feet);
		if (!level.noCollision(entity, body)) {
			return false;
		}
		BlockPos low = BlockPos.containing(body.minX, body.minY - 0.5, body.minZ);
		BlockPos high = BlockPos.containing(body.maxX, body.maxY, body.maxZ);
		for (BlockPos pos : BlockPos.betweenClosed(low, high)) {
			BlockState state = level.getBlockState(pos);
			if (state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.MAGMA_BLOCK)
					|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW)
					|| state.is(Blocks.POINTED_DRIPSTONE)) {
				return false;
			}
		}
		return true;
	}

	/** The ground (or the top of the water) under {@code from}, within {@code drop} blocks; null over the void or a long fall. */
	private static Vec3 landing(ServerLevel level, Vec3 from, double drop) {
		BlockHitResult hit = level.clip(new ClipContext(from, from.add(0, -drop, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
			CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
	}

	/** Whether nothing solid stands between two points. */
	private static boolean clear(ServerLevel level, Entity entity, Vec3 from, Vec3 to) {
		ClipContext context = entity != null
			? new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)
			: new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
		return level.clip(context).getType() == HitResult.Type.MISS;
	}

	/** A point {@code height} blocks over {@code spot}, or lower if there's a ceiling in the way (never less than 2.2). */
	private static Vec3 sky(ServerLevel level, Vec3 spot, double height) {
		Vec3 from = spot.add(0, 0.5, 0);
		BlockHitResult hit = level.clip(new ClipContext(from, spot.add(0, height + 0.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
			CollisionContext.empty()));
		double top = hit.getType() == HitResult.Type.MISS ? spot.y + height : Math.max(spot.y + 2.2, hit.getLocation().y - 0.6);
		return new Vec3(spot.x, top, spot.z);
	}

	private static void move(LivingEntity t, ServerLevel level, Vec3 to) {
		t.teleportTo(level, to.x, to.y, to.z, Set.<Relative>of(), t.getYRot(), t.getXRot(), false);
		t.resetFallDistance();
		if (t instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	/** Stops a creature moving sideways (the blow that just landed on it mustn't carry it off the spot it was put on). */
	private static void still(LivingEntity t) {
		Vec3 v = t.getDeltaMovement();
		setMotion(t, new Vec3(0, Math.min(0, v.y), 0));
	}

	/**
	 * Throws a creature {@code blocks} along the ground the way {@code dir} points, or less if that would land it
	 * somewhere unsafe (in lava or fire, off a drop of more than 3 blocks, over the void, or behind a wall): the
	 * furthest safe landing it can reach. Nowhere safe at all, and it isn't thrown.
	 */
	private static void throwSafely(ServerLevel level, LivingEntity t, Vec3 dir, int blocks) {
		Vec3 from = t.position();
		Vec3 middle = from.add(0, t.getBbHeight() * 0.5, 0);
		for (int d = Math.min(blocks, THROWS.length - 1); d >= 2; d--) {
			Vec3 feet = landing(level, from.add(dir.scale(d)).add(0, 1.0, 0), 4.0);
			if (feet != null && feet.y <= from.y + 1.0 && safe(level, t, feet) && clear(level, t, middle, feet.add(0, t.getBbHeight() * 0.5, 0))) {
				fling(t, dir.scale(THROWS[d]).add(0, HOP, 0));
				return;
			}
		}
	}

	/** Throws a creature: its motion becomes {@code motion}, less what a mob's knockback resistance takes (as for Push). */
	private static void fling(LivingEntity t, Vec3 motion) {
		double resist = t instanceof Player ? 0 : Math.max(0, Math.min(1, t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
		setMotion(t, motion.scale(1 - resist));
	}

	private static void setMotion(LivingEntity t, Vec3 motion) {
		t.setDeltaMovement(motion);
		t.needsSync = true;
		if (t instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/**
	 * The wind carries {@code t}: the fall that ends this flight doesn't hurt it. The cushion goes once it lands (so
	 * a soft landing doesn't leave it immune to the next fall), or after {@code ticks} at the latest.
	 */
	private static void cushion(LivingEntity t, int ticks) {
		long now = t.level().getGameTime();
		UUID id = t.getUUID();
		long until = now + ticks;
		CUSHIONED.merge(id, until, Math::max);
		if (CUSHIONED.size() > 64) {
			CUSHIONED.values().removeIf(at -> at < now);
		}
		// Only this flight's cushion: a newer one (a second launch) outlasts it and stays.
		Scheduler.onLand(t, ticks, at -> Scheduler.later(2, () -> CUSHIONED.remove(id, until)));
	}
}
