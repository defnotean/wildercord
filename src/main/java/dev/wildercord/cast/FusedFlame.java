package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntPredicate;

/**
 * Fused effects of flame and stone, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedFlameVfx}. Numbers match the rune descriptions in
 * {@code Runes}.
 * <ul>
 *   <li><b>Phoenix Pyre</b> (fire + life): healing flame on allies; an aura that follows each of them and burns
 *       the enemies round them once a second, never their own side.</li>
 *   <li><b>Hellmouth</b> (fire + void): a pit of black fire that drags enemies in, burns its core, then caves in.</li>
 *   <li><b>Starfire</b> (fire + arcane): five motes that really seek, steering after their marks each tick.</li>
 *   <li><b>Everburn</b> (fire + time): a second burn on the half beat of the fire's own, and one rekindling.</li>
 *   <li><b>Bloodboil</b> (fire + blood): a mark that answers every hurt with fire, five times at most.</li>
 *   <li><b>Conflagration</b> (fire + fire): sets alight, and every burning enemy around flares up.</li>
 *   <li><b>Monolith</b> (earth + earth): a column of stone (block displays, never blocks) throws the target up.</li>
 *   <li><b>Magnetize</b> (storm + earth): the target draws enemies in and shocks those touching it.</li>
 *   <li><b>Sinkhole</b> (earth + void): the ground gives way, drags enemies in, pins and crushes them.</li>
 * </ul>
 * Pulls never move a boss, never pull through a wall, and ease a creature along rather than fling it. Everything
 * lasting is keyed to its creature and refreshed by a new cast rather than stacked; all of it is forgotten when
 * the server stops.
 */
final class FusedFlame {
	private FusedFlame() {}

	/** At most this many targets get a lingering or spreading part of their own, as in {@link FusedEffects}. */
	private static final int MAX_TARGETS = 8;
	/** Starfire's motes, and so the most enemies it seeks. */
	static final int STARFIRE_MOTES = 5;
	/** Conflagration flares up at most this many burning enemies. */
	private static final int MAX_FLARES = 12;
	/** Hellmouths (and Sinkholes) one caster may have open at once. */
	private static final int MAX_PITS = 4;
	/** Magnets one caster may have at once. */
	private static final int MAX_MAGNETS = 6;
	/** Bloodboil answers at most this many hurts. */
	static final int BOILS = 5;
	/** Monolith's throw: the upward speed that carries a creature about 3 blocks up. */
	static final double MONOLITH_LIFT = 0.69;
	private static final Identifier SINKHOLE_ID = Wildercord.id("sinkhole");

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> boil(entity, baseDamage, blocked));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!BOILING.isEmpty()) {
				BOILING.remove(entity.getUUID());
			}
		});
		// Stone goes before the world is saved; one saved anyway (its chunk unloaded) is removed as it loads (see BlockFx).
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> FusedFlameVfx.clearStone());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	/** Forgets every pyre, pit, burn, boil, magnet and pin (the server stopped, so their timers went with it). */
	static void clear() {
		PYRES.clear();
		PITS.clear();
		SINKS.clear();
		EVERBURNING.clear();
		BOILING.clear();
		MAGNETS.clear();
		PINNED.clear();
		boiling = false;
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		// Wildercord's own runes by name: an add-on's example:hellmouth is never this one.
		if (!Effects.builtIn(node.effect)) {
			return false;
		}
		double radius = SpellNumbers.effectRadius(node);
		switch (node.effect.path()) {
			case "phoenix_pyre" -> phoenixPyre(cast, helped, 2.0 * radius, power, duration, amplify);
			case "hellmouth" -> hellmouth(cast, hit.point(), 3.0 * radius, power, Effects.ticks(3, duration));
			case "starfire" -> starfire(cast, hit, harmed, 6.0 * radius, power, duration);
			case "everburn" -> {
				int n = 0;
				for (LivingEntity t : harmed) {
					everburn(cast, t, power, duration, n++ < MAX_TARGETS);
				}
			}
			case "bloodboil" -> {
				int n = 0;
				for (LivingEntity t : harmed) {
					bloodboil(cast, t, power, Effects.ticks(5, duration), n++ < MAX_TARGETS);
				}
			}
			case "conflagration" -> conflagration(cast, hit, harmed, 6.0 * radius, power, duration);
			case "monolith" -> {
				int n = 0;
				for (LivingEntity t : harmed) {
					monolith(cast, t, power, n++ < MAX_TARGETS);
				}
			}
			case "magnetize" -> {
				for (LivingEntity t : first(harmed, 4)) {
					magnetize(cast, t, 5.0 * radius, power, Effects.ticks(4, duration));
				}
			}
			case "sinkhole" -> sinkhole(cast, hit.point(), 3.0 * radius, power, Effects.ticks(2, duration));
			default -> {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ helpers

	private static List<LivingEntity> first(List<LivingEntity> targets, int n) {
		return targets.size() <= n ? targets : targets.subList(0, n);
	}

	private static boolean onHand(Cast cast, LivingEntity t) {
		return t.isAlive() && !t.isRemoved() && t.level() == cast.level;
	}

	private static DamageSource fire(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster);
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	private static double flat(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/**
	 * Runs {@code step} with its age every {@code every} ticks, from the next tick, while it returns true, the cast
	 * lasts and fewer than {@code ticks} have passed; then {@code end}, always (it tidies up).
	 */
	private static void every(Cast cast, int ticks, int every, IntPredicate step, Runnable end) {
		int[] age = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			boolean going;
			try {
				going = cast.alive() && age[0] < ticks && step.test(age[0]);
			} catch (RuntimeException e) {
				end.run();
				throw e;
			}
			if (!going) {
				end.run();
				return;
			}
			age[0] += every;
			Scheduler.later(every, next[0]);
		};
		Scheduler.later(1, next[0]);
	}

	/** Runs damage dealt after the first beat as lingering (a Shield blocks it but can't parry it). */
	private static void beat(int age, Runnable pulse) {
		if (age > 0) {
			Effects.lingering(pulse);
		} else {
			pulse.run();
		}
	}

	private static void setMotion(LivingEntity e, Vec3 motion) {
		e.setDeltaMovement(motion);
		e.needsSync = true;
		if (e instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/**
	 * Eases {@code e} along the ground toward {@code dir} (level, unit length) at {@code speed}: half of what it was
	 * doing is kept, so it's drawn in rather than flung, and heavy creatures resist a little.
	 */
	private static void drag(LivingEntity e, Vec3 dir, double speed) {
		double resist = e instanceof Player ? 0.0 : Math.max(0.0, Math.min(1.0, e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
		double s = speed * (1.0 - 0.6 * resist);
		Vec3 v = e.getDeltaMovement();
		setMotion(e, new Vec3(v.x * 0.5 + dir.x * s, v.y, v.z * 0.5 + dir.z * s));
	}

	/** Whether nothing solid stands between {@code from} and the middle of {@code e}: nothing is dragged through a wall. */
	private static boolean inSight(ServerLevel level, Vec3 from, Entity e) {
		return level.clip(new ClipContext(from, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType()
			== HitResult.Type.MISS;
	}

	/** A pit or sinkhole one caster opened: where, and until when. */
	private static final class Zone {
		final UUID caster;
		final ServerLevel level;
		final Vec3 centre;
		long until;
		/** It can be stoked to last longer, but never past this. */
		final long cap;

		Zone(Cast cast, Vec3 centre, long until, long cap) {
			this.caster = cast.caster.getUUID();
			this.level = cast.level;
			this.centre = centre;
			this.until = until;
			this.cap = cap;
		}
	}

	private static final List<Zone> PITS = new ArrayList<>();
	private static final List<Zone> SINKS = new ArrayList<>();

	/** This caster's open zone within {@code within} of {@code at}, or null. */
	private static Zone zoneNear(List<Zone> zones, Cast cast, Vec3 at, double within) {
		long now = cast.level.getGameTime();
		// One whose loop was lost (a task that failed) goes on its own soon after its time.
		zones.removeIf(z -> now > z.cap + 100);
		UUID caster = cast.caster.getUUID();
		for (Zone z : zones) {
			if (z.caster.equals(caster) && z.level == cast.level && z.centre.distanceToSqr(at) <= within * within) {
				return z;
			}
		}
		return null;
	}

	private static int zonesOf(List<Zone> zones, Cast cast) {
		UUID caster = cast.caster.getUUID();
		int n = 0;
		for (Zone z : zones) {
			if (z.caster.equals(caster)) {
				n++;
			}
		}
		return n;
	}

	// ------------------------------------------------------------------ Phoenix Pyre

	/** Each wreathed ally's pyre (a new one takes over from the old, never burns alongside it). */
	private static final Map<UUID, Object> PYRES = new HashMap<>();

	/**
	 * Phoenix Pyre: Regeneration and Fire Resistance, and an aura of flame that goes where the ally goes, setting
	 * alight and burning every enemy within reach once a second. It never touches the caster's side, nor (for a
	 * player) the ally's own.
	 */
	private static void phoenixPyre(Cast cast, List<LivingEntity> helped, double radius, double power, double duration, int amplify) {
		ServerLevel level = cast.level;
		int ticks = Effects.ticks(6, duration);
		int regen = Math.min(2, Math.max(0, amplify));
		int wreathed = 0;
		for (LivingEntity t : helped) {
			t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, regen, false, true));
			t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, true));
			t.clearFire();
			if (wreathed++ >= MAX_TARGETS) {
				continue;
			}
			UUID id = t.getUUID();
			boolean renewed = PYRES.containsKey(id);
			Object token = new Object();
			PYRES.put(id, token);
			if (!renewed || !cast.passive) {
				FusedFlameVfx.phoenixRise(level, t, radius);
			}
			every(cast, ticks, 5, age -> {
				if (PYRES.get(id) != token || !onHand(cast, t)) {
					return false;
				}
				if (age % 20 == 0) {
					beat(age, () -> phoenixBurn(cast, t, radius, power));
					FusedFlameVfx.phoenixPulse(level, t, radius, age);
				} else {
					FusedFlameVfx.phoenixSmoulder(level, t);
				}
				return true;
			}, () -> {
				if (PYRES.remove(id, token) && onHand(cast, t)) {
					FusedFlameVfx.phoenixFade(level, t);
				}
			});
		}
	}

	/** One second of the pyre: every enemy near the ally is set alight and scorched. */
	private static void phoenixBurn(Cast cast, LivingEntity t, double radius, double power) {
		ServerLevel level = cast.level;
		Vec3 c = t.getBoundingBox().getCenter();
		boolean player = t instanceof Player;
		int burned = 0;
		for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(radius, 1.0, radius),
				e -> Targets.canHarm(cast.caster, e) && !(player && Targets.isAlly(t, e)))) {
			if (burned >= MAX_TARGETS) {
				break;
			}
			LivingEntity near = (LivingEntity) e;
			if (near.getBoundingBox().getCenter().distanceTo(c) > radius + near.getBbWidth() / 2) {
				continue;
			}
			burned++;
			FusedFlameVfx.phoenixScorch(level, t, near);
			near.igniteForSeconds(2);
			Effects.hurt(cast, near, fire(cast), 1 * power);
		}
	}

	// ------------------------------------------------------------------ Hellmouth

	/**
	 * Hellmouth: a pit of black fire opens on the ground at the point. For its time it drags enemies within reach
	 * toward its middle (never bosses, never through walls) and burns whatever stands in its core once a second;
	 * then it caves in on what's left in it. Cast again into its own pit, it's stoked (its time starts over, up to
	 * three times its length) instead of a second pit opening on top of it.
	 */
	private static void hellmouth(Cast cast, Vec3 point, double radius, double power, int ticks) {
		ServerLevel level = cast.level;
		Vec3 centre = CastEngine.ground(level, point.add(0, 0.5, 0));
		double core = Math.max(1.0, radius * 0.4);
		long now = level.getGameTime();
		Zone open = zoneNear(PITS, cast, centre, 2.0);
		if (open != null) {
			open.until = Math.min(open.cap, Math.max(open.until, now + ticks));
			FusedFlameVfx.hellmouthStoke(level, open.centre, radius);
			return;
		}
		if (zonesOf(PITS, cast) >= MAX_PITS) {
			FusedFlameVfx.hellmouthFizzle(level, centre);
			return;
		}
		Zone pit = new Zone(cast, centre, now + ticks, now + 3L * ticks);
		PITS.add(pit);
		FusedFlameVfx.hellmouthOpen(level, centre, radius, core, ticks);
		every(cast, 3 * ticks + 10, 5, age -> {
			if (level.getGameTime() >= pit.until) {
				return false;
			}
			beat(age, () -> hellmouthPulse(cast, centre, radius, core, power, age));
			return true;
		}, () -> {
			PITS.remove(pit);
			if (cast.alive()) {
				Effects.lingering(() -> hellmouthClose(cast, centre, radius, core, power));
			}
		});
	}

	/** A quarter second of the pit: it drags, and on the second it burns its core. */
	private static void hellmouthPulse(Cast cast, Vec3 centre, double radius, double core, double power, int age) {
		ServerLevel level = cast.level;
		FusedFlameVfx.hellmouth(level, centre, radius, core, age);
		Vec3 mouth = centre.add(0, 0.6, 0);
		boolean burn = age % 20 == 0;
		int n = 0;
		for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius, 2.5, radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			Vec3 in = new Vec3(centre.x - t.getX(), 0, centre.z - t.getZ());
			double d = in.length();
			if (d > radius || n >= MAX_TARGETS) {
				continue;
			}
			n++;
			if (burn && d <= core) {
				FusedFlameVfx.hellmouthBurn(level, t);
				t.igniteForSeconds(2);
				Effects.hurt(cast, t, fire(cast), 2 * power * (age == 0 ? Reactions.fire(cast, t) : 1.0));
			}
			if (d > 0.35 && !Spirits.isBoss(t) && t.isAlive() && inSight(level, mouth, t)) {
				drag(t, in.scale(1 / d), Math.min(0.3, 0.06 + d * 0.07));
				Reactions.mark(t, Reactions.Mark.PULLED);
				if (age % 10 == 0) {
					FusedFlameVfx.hellmouthPull(level, centre, t);
				}
			}
		}
	}

	/** The pit caves in on everything still in it. */
	private static void hellmouthClose(Cast cast, Vec3 centre, double radius, double core, double power) {
		ServerLevel level = cast.level;
		FusedFlameVfx.hellmouthClose(level, centre, radius, core);
		double reach = Math.max(core, radius * 0.6);
		int n = 0;
		for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(reach, 2.5, reach), e -> Targets.canHarm(cast.caster, e))) {
			if (n >= MAX_TARGETS * 2 || flat(e.position(), centre) > reach) {
				continue;
			}
			n++;
			Effects.hurt(cast, (LivingEntity) e, magic(cast), 4 * power);
		}
	}

	// ------------------------------------------------------------------ Starfire

	/**
	 * Starfire: five motes burst from where it lands and seek up to five enemies within reach: those it struck
	 * first, then the nearest others it can see. With fewer enemies than motes, the rest go round again, so a lone
	 * enemy takes them all. Each mote steers after its mark every tick (onto another if its mark falls first), and
	 * where it lands it burns.
	 */
	private static void starfire(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power, double duration) {
		ServerLevel level = cast.level;
		Vec3 from;
		if (hit.self()) {
			from = cast.caster.getBoundingBox().getCenter().add(0, 0.6, 0);
		} else if (!harmed.isEmpty()) {
			from = harmed.getFirst().getBoundingBox().getCenter();
		} else {
			// Off the face of whatever it hit, so a wall doesn't hide everything from it.
			Vec3 back = hit.dir().lengthSqr() > 1.0E-6 ? hit.dir().normalize().scale(-0.4) : Vec3.ZERO;
			from = hit.point().add(back).add(0, 0.3, 0);
		}
		List<LivingEntity> marks = new ArrayList<>();
		for (LivingEntity t : harmed) {
			if (marks.size() < STARFIRE_MOTES && t.isAlive()) {
				marks.add(t);
			}
		}
		if (marks.size() < STARFIRE_MOTES) {
			List<LivingEntity> near = new ArrayList<>();
			for (Entity e : level.getEntities(cast.caster, new AABB(from, from).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity t = (LivingEntity) e;
				if (!marks.contains(t) && t.getBoundingBox().getCenter().distanceTo(from) <= radius && inSight(level, from, t)) {
					near.add(t);
				}
			}
			near.sort(Comparator.comparingDouble(t -> t.getBoundingBox().getCenter().distanceToSqr(from)));
			for (LivingEntity t : near) {
				if (marks.size() >= STARFIRE_MOTES) {
					break;
				}
				marks.add(t);
			}
		}
		FusedFlameVfx.starfireBurst(level, from);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < STARFIRE_MOTES; i++) {
			LivingEntity mark = marks.isEmpty() ? null : marks.get(i % marks.size());
			Vec3 launch = ElementFx.tilted(0.8, phase + i * Math.PI * 2 / STARFIRE_MOTES).scale(0.55);
			mote(cast, from, launch, mark, marks, i, power, duration);
		}
	}

	/** One mote in flight: it curves after its mark a little faster every tick, and gives up after two seconds. */
	private static void mote(Cast cast, Vec3 start, Vec3 launch, LivingEntity mark, List<LivingEntity> marks, int index, double power, double duration) {
		ServerLevel level = cast.level;
		Vec3[] pos = {start};
		Vec3[] velocity = {launch};
		LivingEntity[] aim = {mark};
		int[] age = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			if (!cast.alive()) {
				return;
			}
			if (aim[0] == null || !onHand(cast, aim[0])) {
				aim[0] = nearestMark(cast, marks, pos[0]);
			}
			if (aim[0] == null || age[0] > 40) {
				FusedFlameVfx.starfireFizzle(level, pos[0]);
				return;
			}
			LivingEntity t = aim[0];
			Vec3 goal = t.getBoundingBox().getCenter();
			Vec3 to = goal.subtract(pos[0]);
			double d = to.length();
			double speed = Math.min(1.2, 0.4 + age[0] * 0.06);
			if (d <= speed + 0.35) {
				FusedFlameVfx.starfireMote(level, pos[0], goal, index, age[0]);
				FusedFlameVfx.starfireStrike(level, goal, index);
				Effects.hurt(cast, t, fire(cast), 2 * power * Reactions.fire(cast, t));
				t.igniteForSeconds((float) (3 * duration));
				return;
			}
			Vec3 v = velocity[0].scale(0.72).add(to.scale(0.5 / d));
			v = v.lengthSqr() < 1.0E-6 ? to.scale(speed / d) : v.normalize().scale(speed);
			Vec3 moved = pos[0].add(v);
			FusedFlameVfx.starfireMote(level, pos[0], moved, index, age[0]);
			pos[0] = moved;
			velocity[0] = v;
			age[0]++;
			Scheduler.later(1, next[0]);
		};
		Scheduler.later(1 + index, next[0]);
	}

	private static LivingEntity nearestMark(Cast cast, List<LivingEntity> marks, Vec3 at) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity t : marks) {
			double d = t.getBoundingBox().getCenter().distanceToSqr(at);
			if (onHand(cast, t) && d < bestDistance) {
				best = t;
				bestDistance = d;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ Everburn

	/** Each burning creature's Everburn (a new one takes over from the old). */
	private static final Map<UUID, Object> EVERBURNING = new HashMap<>();

	/**
	 * Everburn: sets the target alight for 5 seconds, and while that fire lasts it burns on the half beat of the
	 * fire's own (1 more damage a second, and the fire's own tick still lands). When it goes out it rekindles, once,
	 * for 3 seconds more (not under water or rain, where it waits for the target to dry out). Only its own two
	 * fires burn twice as fast: any other fire it's left in burns as usual. Fire-proof creatures shrug it all off.
	 */
	private static void everburn(Cast cast, LivingEntity t, double power, double duration, boolean track) {
		ServerLevel level = cast.level;
		int burn = Effects.ticks(5, duration);
		int again = Effects.ticks(3, duration);
		t.igniteForTicks(burn);
		FusedFlameVfx.everburnLight(level, t, burn);
		if (!track || t.fireImmune()) {
			return;
		}
		UUID id = t.getUUID();
		Object token = new Object();
		EVERBURNING.put(id, token);
		long start = level.getGameTime();
		long giveUp = start + burn + again + 200;
		long[] window = {start + burn + 5};
		boolean[] rekindled = {false};
		boolean[] first = {true};
		long[] last = {-100};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			long now = level.getGameTime();
			if (EVERBURNING.get(id) != token || !cast.alive() || !onHand(cast, t) || now > giveUp) {
				EVERBURNING.remove(id, token);
				return;
			}
			int fire = t.getRemainingFireTicks();
			int wait = 5;
			if (fire > 0) {
				if (now <= window[0]) {
					// Vanilla fire burns when its ticks left reach a multiple of 20; this burns 11 ticks before that,
					// clear of the hurt cooldown either way, so both land.
					int due = Math.floorMod(fire - 11, 20);
					if (due == 0 && now - last[0] >= 15) {
						last[0] = now;
						double react = first[0] ? Reactions.fire(cast, t) : 1.0;
						first[0] = false;
						FusedFlameVfx.everburnTick(level, t, rekindled[0]);
						Effects.lingering(() -> Effects.hurt(cast, t, level.damageSources().source(DamageTypes.ON_FIRE, cast.caster), 1 * power * react));
						due = 20;
					}
					wait = Math.max(1, Math.min(5, due));
				} else if (rekindled[0]) {
					// Its own second fire has run its course: whatever burns now is some other fire.
					EVERBURNING.remove(id, token);
					return;
				}
			} else if (rekindled[0]) {
				FusedFlameVfx.everburnOut(level, t);
				EVERBURNING.remove(id, token);
				return;
			} else if (!t.isInWaterOrRain()) {
				rekindled[0] = true;
				t.igniteForTicks(again);
				window[0] = now + again + 5;
				FusedFlameVfx.everburnRekindle(level, t);
				wait = 1;
			}
			Scheduler.later(wait, next[0]);
		};
		Scheduler.later(1, next[0]);
	}

	// ------------------------------------------------------------------ Bloodboil

	/** A boiling creature: whose boil it is, how strong, until when, and how many more hurts it answers. */
	private static final class Boil {
		final Cast cast;
		final double power;
		final long until;
		int left = BOILS;
		/** The burst to deal, set just while {@link #inContext} runs it. */
		final Runnable[] job = new Runnable[1];
		/** Runs {@link #job} with this effect's own Execute and Unison, never those of whatever hurt the target. */
		final Runnable inContext;

		Boil(Cast cast, double power, long until) {
			this.cast = cast;
			this.power = power;
			this.until = until;
			this.inContext = Effects.carryContext(() -> {
				if (job[0] != null) {
					job[0].run();
				}
			});
		}
	}

	private static final Map<UUID, Boil> BOILING = new HashMap<>();
	/** Set while a boil's own burst is dealt, so the burst can never set off another. */
	private static boolean boiling;

	/**
	 * Bloodboil: 3 damage, and the target's blood boils for 5 seconds. A fresh boil replaces the one it had (it never
	 * stacks a second), starting its five answers over.
	 */
	private static void bloodboil(Cast cast, LivingEntity t, double power, int ticks, boolean mark) {
		ServerLevel level = cast.level;
		FusedFlameVfx.bloodboil(level, t);
		Effects.hurt(cast, t, magic(cast), 3 * power);
		if (!mark || !onHand(cast, t)) {
			return;
		}
		UUID id = t.getUUID();
		Boil boil = new Boil(cast, power, level.getGameTime() + ticks);
		BOILING.put(id, boil);
		every(cast, ticks, 10, age -> {
			if (BOILING.get(id) != boil || !onHand(cast, t)) {
				return false;
			}
			if (age > 0) {
				FusedFlameVfx.bloodboilSimmer(level, t, age);
			}
			return true;
		}, () -> BOILING.remove(id, boil));
	}

	/**
	 * The listener: a boiling creature was hurt. Its own burst, and anything dealt while that burst lands, is ignored,
	 * so it never answers itself; a boil that has answered five times or run out of time is gone. One map lookup when
	 * nothing boils.
	 */
	private static void boil(LivingEntity entity, float baseDamage, boolean blocked) {
		if (BOILING.isEmpty() || boiling || blocked || !(baseDamage > 0)) {
			return;
		}
		UUID id = entity.getUUID();
		Boil b = BOILING.get(id);
		if (b == null) {
			return;
		}
		if (entity.level() != b.cast.level || b.cast.level.getGameTime() > b.until || b.left <= 0) {
			BOILING.remove(id, b);
			return;
		}
		if (--b.left <= 0) {
			BOILING.remove(id, b);
		}
		int count = BOILS - b.left;
		// After the blow settles, so the burst never lands inside another hit's damage.
		Scheduler.later(1, () -> burst(b, entity, count));
	}

	private static void burst(Boil b, LivingEntity t, int count) {
		Cast cast = b.cast;
		if (!cast.alive() || !onHand(cast, t) || !Targets.canHarm(cast.caster, t)) {
			return;
		}
		FusedFlameVfx.bloodboilBurst(cast.level, t, count);
		b.job[0] = () -> {
			boiling = true;
			try {
				Effects.lingering(() -> Effects.hurt(cast, t, fire(cast), 2 * b.power));
			} finally {
				boiling = false;
			}
		};
		try {
			b.inContext.run();
		} finally {
			b.job[0] = null;
		}
	}

	// ------------------------------------------------------------------ Conflagration

	/**
	 * Conflagration: sets the targets alight for 8 seconds, and every burning enemy within reach of them (the targets
	 * themselves too) flares up, the flare leaping outward from each target to the furthest: 3 damage and 2 seconds
	 * more burning. With nothing struck it flares up whatever burns around the point.
	 */
	private static void conflagration(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power, double duration) {
		ServerLevel level = cast.level;
		for (LivingEntity t : harmed) {
			t.igniteForSeconds((float) (8 * duration));
		}
		List<Vec3> hearts = new ArrayList<>();
		Map<LivingEntity, Vec3> flare = new LinkedHashMap<>();
		for (LivingEntity t : first(harmed, MAX_TARGETS)) {
			Vec3 c = t.getBoundingBox().getCenter();
			hearts.add(c);
			if (t.isOnFire()) {
				flare.put(t, c);
			}
		}
		if (hearts.isEmpty()) {
			hearts.add(hit.point());
		}
		for (Vec3 heart : hearts) {
			for (Entity e : level.getEntities((Entity) null, new AABB(heart, heart).inflate(radius), e -> Targets.canHarm(cast.caster, e) && e.isOnFire())) {
				if (flare.size() >= MAX_FLARES) {
					break;
				}
				if (e.getBoundingBox().getCenter().distanceTo(heart) <= radius) {
					flare.putIfAbsent((LivingEntity) e, heart);
				}
			}
		}
		FusedFlameVfx.conflagration(level, hearts, radius);
		int longer = Effects.ticks(2, duration);
		for (Map.Entry<LivingEntity, Vec3> entry : flare.entrySet()) {
			LivingEntity t = entry.getKey();
			Vec3 heart = entry.getValue();
			double d = t.getBoundingBox().getCenter().distanceTo(heart);
			Scheduler.later(1 + (int) Math.round(d * 1.2), () -> {
				if (!cast.alive() || !onHand(cast, t) || !t.isOnFire()) {
					return;
				}
				FusedFlameVfx.flareUp(level, heart, t);
				Effects.hurt(cast, t, fire(cast), 3 * power * Reactions.fire(cast, t));
				if (t.isOnFire()) {
					t.setRemainingFireTicks(t.getRemainingFireTicks() + longer);
				}
			});
		}
	}

	// ------------------------------------------------------------------ Monolith

	/**
	 * Monolith: a column of stone bursts up under the target (block displays, never real blocks) for 6 damage and
	 * throws it about 3 blocks up and a little away from the caster; the column crumbles 4 seconds later. A boss takes
	 * the blow but stays on its feet, and heavy creatures are thrown less (knockback resistance). A creature has one
	 * column at a time: another bursts up in place of the old.
	 */
	private static void monolith(Cast cast, LivingEntity t, double power, boolean column) {
		ServerLevel level = cast.level;
		Vec3 floor = ElementFx.floor(level, t.position().add(0, 0.1, 0), 3.0);
		double width = Math.max(0.8, Math.min(1.6, t.getBbWidth() + 0.3));
		FusedFlameVfx.monolith(level, t, floor == null ? t.position() : floor, width, column && floor != null);
		Effects.hurt(cast, t, magic(cast), 6 * power);
		if (Spirits.isBoss(t) || !t.isAlive()) {
			return;
		}
		double resist = t instanceof Player ? 0.0 : Math.max(0.0, Math.min(1.0, t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
		if (resist >= 1.0) {
			return;
		}
		Vec3 away = Effects.horizontal(t.position().subtract(cast.caster.position()), cast.caster.getLookAngle()).scale(0.1 * (1 - resist));
		Vec3 v = t.getDeltaMovement();
		// Set, not added: two columns under one creature in the same moment don't throw it twice as high.
		setMotion(t, new Vec3(away.x, Math.max(v.y, MONOLITH_LIFT * (1 - resist)), away.z));
		Reactions.mark(t, Reactions.Mark.WINDSWEPT);
	}

	// ------------------------------------------------------------------ Magnetize

	/** Each magnetized creature's magnet: whose, and the token of the latest (a new one takes over). */
	private record Magnet(UUID caster, Object token) {}

	private static final Map<UUID, Magnet> MAGNETS = new HashMap<>();

	/**
	 * Magnetize: for 4 seconds the target draws every enemy within reach toward it (never bosses, never through
	 * walls, easing off once they touch), and shocks each one touching it once a second. The magnet itself is left
	 * alone. Magnetizing it again refreshes the magnet.
	 */
	private static void magnetize(Cast cast, LivingEntity t, double radius, double power, int ticks) {
		ServerLevel level = cast.level;
		UUID id = t.getUUID();
		UUID caster = cast.caster.getUUID();
		Magnet old = MAGNETS.get(id);
		if (old == null && MAGNETS.values().stream().filter(m -> m.caster().equals(caster)).count() >= MAX_MAGNETS) {
			FusedFlameVfx.magnetFizzle(level, t);
			return;
		}
		Magnet magnet = new Magnet(caster, new Object());
		MAGNETS.put(id, magnet);
		if (old == null || !cast.passive) {
			FusedFlameVfx.magnetize(level, t, radius);
		}
		Map<UUID, Long> shocked = new HashMap<>();
		every(cast, ticks, 4, age -> {
			if (MAGNETS.get(id) != magnet || !onHand(cast, t)) {
				return false;
			}
			beat(age, () -> magnetPulse(cast, t, radius, power, age, shocked));
			return true;
		}, () -> {
			if (MAGNETS.remove(id, magnet) && onHand(cast, t)) {
				FusedFlameVfx.magnetizeEnd(level, t);
			}
		});
	}

	private static void magnetPulse(Cast cast, LivingEntity t, double radius, double power, int age, Map<UUID, Long> shocked) {
		ServerLevel level = cast.level;
		Vec3 c = t.getBoundingBox().getCenter();
		long now = level.getGameTime();
		AABB touch = t.getBoundingBox().inflate(0.3);
		int n = 0;
		for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity other = (LivingEntity) e;
			Vec3 oc = other.getBoundingBox().getCenter();
			if (oc.distanceTo(c) > radius || n >= MAX_TARGETS) {
				continue;
			}
			n++;
			if (other.getBoundingBox().intersects(touch)) {
				Long last = shocked.get(other.getUUID());
				if (last == null || now - last >= 20) {
					shocked.put(other.getUUID(), now);
					FusedFlameVfx.magnetShock(level, t, other);
					Effects.hurt(cast, other, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 3 * power * Reactions.storm(cast, other));
				}
				continue;
			}
			if (Spirits.isBoss(other) || !inSight(level, c, other)) {
				continue;
			}
			Vec3 in = new Vec3(c.x - oc.x, 0, c.z - oc.z);
			double d = in.length();
			if (d > 0.2) {
				drag(other, in.scale(1 / d), Math.min(0.28, 0.05 + d * 0.05));
				Reactions.mark(other, Reactions.Mark.PULLED);
			}
			if (age % 8 == 0) {
				FusedFlameVfx.magnetPull(level, t, other);
			}
		}
		if (age % 8 == 0) {
			FusedFlameVfx.magnetField(level, t, age);
		}
	}

	// ------------------------------------------------------------------ Sinkhole

	/** Each pinned creature's latest pin: only the last to end lets go of its jump. */
	private static final Map<UUID, Object> PINNED = new HashMap<>();

	/**
	 * Sinkhole: the ground gives way at the point. Every enemy within reach is dragged to its middle over half a second
	 * (bosses stay put), pinned there for 2 seconds (Slowness IV and no jumping) and then crushed. Cast again where its
	 * own ground has already given way, nothing more happens.
	 */
	private static void sinkhole(Cast cast, Vec3 point, double radius, double power, int ticks) {
		ServerLevel level = cast.level;
		Vec3 centre = CastEngine.ground(level, point.add(0, 0.5, 0));
		long now = level.getGameTime();
		if (zoneNear(SINKS, cast, centre, 1.5) != null || zonesOf(SINKS, cast) >= MAX_PITS) {
			FusedFlameVfx.sinkholeRumble(level, centre, radius);
			return;
		}
		Zone sink = new Zone(cast, centre, now + ticks + 4, now + ticks + 4);
		SINKS.add(sink);
		List<LivingEntity> caught = new ArrayList<>();
		for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius, 2.5, radius), e -> Targets.canHarm(cast.caster, e))) {
			if (caught.size() >= MAX_TARGETS) {
				break;
			}
			if (flat(e.position(), centre) <= radius) {
				caught.add((LivingEntity) e);
			}
		}
		FusedFlameVfx.sinkholeOpen(level, centre, radius, ticks);
		for (LivingEntity t : caught) {
			pin(cast, t, ticks);
		}
		Vec3 mouth = centre.add(0, 0.6, 0);
		every(cast, ticks, 2, age -> {
			if (age < 12) {
				for (LivingEntity t : caught) {
					if (!onHand(cast, t) || Spirits.isBoss(t)) {
						continue;
					}
					Vec3 in = new Vec3(centre.x - t.getX(), 0, centre.z - t.getZ());
					double d = in.length();
					if (d > 0.4 && d <= radius + 1.5 && inSight(level, mouth, t)) {
						// The ground carries it in: quick from the edge, slowing as it nears the middle, never past it.
						double s = Math.min(0.45, d * 0.3);
						setMotion(t, new Vec3(in.x / d * s, Math.min(t.getDeltaMovement().y, 0.0), in.z / d * s));
					}
				}
			}
			if (age % 6 == 0) {
				FusedFlameVfx.sinkhole(level, centre, radius, age);
			}
			return true;
		}, () -> {
			SINKS.remove(sink);
			if (!cast.alive()) {
				return;
			}
			FusedFlameVfx.sinkholeCrush(level, centre, radius);
			Effects.lingering(() -> {
				for (LivingEntity t : caught) {
					if (onHand(cast, t) && flat(t.position(), centre) <= radius + 1.0 && Math.abs(t.getY() - centre.y) < 3.0) {
						FusedFlameVfx.sinkholeCrushed(level, t);
						Effects.hurt(cast, t, magic(cast), 5 * power);
					}
				}
			});
		});
	}

	/** Pinned: Slowness IV and no jumping for {@code ticks}. The jump comes back when the last pin on it ends. */
	private static void pin(Cast cast, LivingEntity t, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 3, false, true), cast.caster);
		AttributeInstance jump = t.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) {
			jump.addOrUpdateTransientModifier(new AttributeModifier(SINKHOLE_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		FusedFlameVfx.sinkholePinned(cast.level, t);
		UUID id = t.getUUID();
		Object token = new Object();
		PINNED.put(id, token);
		// Not tied to the cast: the jump comes back even if the caster is gone.
		Scheduler.later(ticks, () -> {
			if (PINNED.remove(id, token) && jump != null) {
				jump.removeModifier(SINKHOLE_ID);
			}
		});
	}
}
