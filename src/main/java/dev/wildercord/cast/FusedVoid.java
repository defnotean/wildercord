package dev.wildercord.cast;

import dev.wildercord.gear.SpellSlots;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Fused effects of void, arcane and time, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedVoidVfx}. Numbers match the rune descriptions in
 * {@code Runes}.
 */
final class FusedVoid {
	private FusedVoid() {}

	/** At most this many targets get a lingering part of their own (an unravelling, a ledger), so one hit can't flood the server. */
	private static final int MAX_TARGETS = 8;

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
		// Reckoning counts every wound its target takes (what it really lost: after armour, before and after the
		// hit); nothing to do but a map check while no ledger is open.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			weigh(entity);
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> tally(entity, damage));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			UNRAVELLING.clear();
			LEDGERS.clear();
			HOLES.clear();
			SHIFTED.clear();
		});
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		switch (node.effect.path()) {
			case "entropy" -> first(harmed).forEach(t -> entropy(cast, t, power, duration));
			case "devour" -> {
				int shown = 0;
				for (LivingEntity t : harmed) {
					devour(cast, t, power, shown++ < MAX_TARGETS);
				}
			}
			case "timesteal" -> first(harmed).forEach(t -> timesteal(cast, t, duration));
			case "hemomancy" -> {
				if (harmed.isEmpty()) {
					return true;
				}
				int bonus = FusedVoidRules.hemomancyBonus(caster.getMaxHealth(), caster.getHealth());
				if (cast.once("hemomancy:runes")) {
					FusedVoidVfx.hemomancyRunes(level, caster, bonus);
				}
				for (LivingEntity t : harmed) {
					FusedVoidVfx.hemomancy(level, caster, t, bonus);
					Effects.hurt(cast, t, magic(cast), (FusedVoidRules.HEMOMANCY_DAMAGE + bonus) * power);
				}
			}
			case "reckoning" -> first(harmed).forEach(t -> reckoning(cast, t, power, duration));
			case "singularity" -> singularity(cast, hit, FusedVoidRules.SINGULARITY_RADIUS * SpellNumbers.effectRadius(node), power);
			case "prismatic_burst" -> harmed.forEach(t -> {
				List<Integer> marks = useMarks(t);
				FusedVoidVfx.prismaticBurst(level, t, marks);
				Effects.hurt(cast, t, magic(cast), FusedVoidRules.prismaticDamage(marks.size()) * power);
			});
			case "chronoshift" -> {
				int ticks = Effects.ticks(FusedVoidRules.CHRONOSHIFT_BUFF_SECONDS, duration);
				// Like Surge: Amplify (and rank III) adds a level; charge and gear only make it last longer.
				int extra = Math.min(2, Math.max(0, amplify));
				for (LivingEntity t : helped) {
					t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, extra, false, true), caster);
					t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, extra, false, true), caster);
					int turned = t instanceof ServerPlayer player ? turnClock(cast, player, power, ticks) : 0;
					FusedVoidVfx.chronoshift(level, t, turned);
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private static List<LivingEntity> first(List<LivingEntity> targets) {
		return targets.size() <= MAX_TARGETS ? targets : targets.subList(0, MAX_TARGETS);
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	private static boolean onHand(Cast cast, LivingEntity t) {
		return t.isAlive() && !t.isRemoved() && t.level() == cast.level;
	}

	// ------------------------------------------------------------------ Entropy

	/** One creature unravelling: which wound it's on, and the wound it ends with. */
	private static final class Unravel {
		LivingEntity target;
		Cast cast;
		double power;
		int step;
		int last;
	}

	private static final Map<UUID, Unravel> UNRAVELLING = new HashMap<>();

	/**
	 * Entropy: the target frays a little more every second, the wounds climbing by half a point to
	 * 2.5, magic through armour. One per creature: struck again while it frays (Linger, a Zone), it
	 * isn't started over or doubled but wound on, fraying at its peak for the full time again.
	 */
	private static void entropy(Cast cast, LivingEntity t, double power, double duration) {
		int steps = FusedVoidRules.entropyWounds(duration);
		Unravel running = UNRAVELLING.get(t.getUUID());
		if (running != null && running.target == t) {
			running.last = Math.max(running.last, running.step + steps);
			running.power = Math.max(running.power, power);
			running.cast = cast;
			FusedVoidVfx.entropyWound(cast.level, t, cast.caster, running.step, running.last);
			return;
		}
		Unravel unravel = new Unravel();
		unravel.target = t;
		unravel.cast = cast;
		unravel.power = power;
		unravel.last = steps;
		UNRAVELLING.put(t.getUUID(), unravel);
		FusedVoidVfx.entropy(cast.level, t, cast.caster, steps);
		Scheduler.later(20, () -> unravel(t, unravel));
	}

	private static void unravel(LivingEntity t, Unravel unravel) {
		if (UNRAVELLING.get(t.getUUID()) != unravel) {
			return;
		}
		Cast cast = unravel.cast;
		if (!cast.alive() || !onHand(cast, t)) {
			UNRAVELLING.remove(t.getUUID(), unravel);
			return;
		}
		unravel.step++;
		boolean last = unravel.step >= unravel.last;
		double amount = FusedVoidRules.entropyWound(unravel.step) * unravel.power;
		FusedVoidVfx.entropyTick(cast.level, t, cast.caster, unravel.step, unravel.last, last);
		Effects.lingering(() -> Effects.hurt(cast, t, magic(cast), amount));
		if (last || !t.isAlive()) {
			UNRAVELLING.remove(t.getUUID(), unravel);
			return;
		}
		Scheduler.later(20, () -> unravel(t, unravel));
	}

	// ------------------------------------------------------------------ Devour

	/** Devour: a void maw closes on the target; if the bite kills, the caster feeds. */
	private static void devour(Cast cast, LivingEntity t, double power, boolean full) {
		ServerLevel level = cast.level;
		Effects.hurt(cast, t, magic(cast), FusedVoidRules.DEVOUR_DAMAGE * power);
		boolean killed = !t.isAlive() || t.isDeadOrDying();
		FusedVoidVfx.devour(level, t, killed, full);
		// A cast feeds at most twice (a Burst through a flock of chickens isn't a mana well); its echoes share the two.
		if (killed && cast.caster.isAlive() && mayFeed(cast)) {
			feed(cast, t);
		}
	}

	private static boolean mayFeed(Cast cast) {
		for (int i = 0; i < FusedVoidRules.DEVOUR_FEEDS; i++) {
			if (cast.once("devour:fed " + i)) {
				return true;
			}
		}
		return false;
	}

	private static void feed(Cast cast, LivingEntity prey) {
		LivingEntity caster = cast.caster;
		if (caster instanceof ServerPlayer player) {
			Mana.restore(player, FusedVoidRules.DEVOUR_MANA);
		}
		// The absorption rides on an Absorption I (which lets it be held); a second kill tops it back up to 4, never past it.
		caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, FusedVoidRules.DEVOUR_ABSORPTION_TICKS, 0, false, true), caster);
		caster.setAbsorptionAmount(Math.max(caster.getAbsorptionAmount(), FusedVoidRules.DEVOUR_ABSORPTION));
		FusedVoidVfx.feed(cast.level, prey, caster);
	}

	// ------------------------------------------------------------------ Timesteal

	/**
	 * Timesteal: up to two of the target's good effects (the strongest, then the longest), each with the
	 * time it had left, at most 30 seconds, go to the caster. A creature loses the whole effect; a boss
	 * only the time taken (its fight is never stripped bare), and never an endless effect.
	 */
	private static void timesteal(Cast cast, LivingEntity t, double duration) {
		int cap = Math.max(20, Effects.ticks(FusedVoidRules.TIMESTEAL_SECONDS, duration));
		boolean boss = Spirits.isBoss(t);
		List<MobEffectInstance> good = new ArrayList<>();
		for (MobEffectInstance effect : t.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL && !(boss && effect.isInfiniteDuration())
					&& (effect.isInfiniteDuration() || effect.getDuration() > 0)) {
				good.add(effect);
			}
		}
		good.sort(Comparator.comparingInt(MobEffectInstance::getAmplifier).reversed()
			.thenComparing(Comparator.comparingInt(FusedVoid::timeLeft).reversed()));
		List<MobEffectInstance> taken = new ArrayList<>();
		for (MobEffectInstance effect : good.subList(0, Math.min(FusedVoidRules.TIMESTEAL_EFFECTS, good.size()))) {
			Holder<MobEffect> kind = effect.getEffect();
			int left = timeLeft(effect);
			int stolen = FusedVoidRules.stolenTicks(left, cap);
			int kept = FusedVoidRules.keptTicks(left, stolen, boss);
			t.removeEffect(kind);
			if (kept > 0) {
				t.addEffect(new MobEffectInstance(kind, kept, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
			}
			taken.add(new MobEffectInstance(kind, stolen, effect.getAmplifier(), false, true, true));
		}
		LivingEntity caster = cast.caster;
		List<Integer> colours = new ArrayList<>();
		for (MobEffectInstance effect : taken) {
			colours.add(effect.getEffect().value().getColor());
			if (caster.isAlive()) {
				caster.addEffect(effect, caster);
			}
		}
		FusedVoidVfx.timesteal(cast.level, t, caster, colours);
	}

	/** Ticks an effect has left, an endless one counting as longest of all. */
	private static int timeLeft(MobEffectInstance effect) {
		return effect.isInfiniteDuration() ? Integer.MAX_VALUE : effect.getDuration();
	}

	// ------------------------------------------------------------------ Reckoning

	/** An open ledger: the wounds counted on one creature so far, and when they come due. */
	private static final class Ledger {
		final Cast cast;
		final LivingEntity target;
		double power;
		float owed;
		int wounds;
		/** Health (and absorption) just before the hit being dealt now, to see what it really took. */
		float before = Float.NaN;
		final long opened;
		final int ticks;
		/** Which way its seal faces: leaning toward whoever opened it. */
		final Vec3 facing;

		Ledger(Cast cast, LivingEntity target, double power, int ticks) {
			this.cast = cast;
			this.target = target;
			this.power = power;
			this.ticks = ticks;
			this.opened = cast.level.getGameTime();
			this.facing = FusedVoidVfx.facing(target, cast.caster);
		}
	}

	private static final Map<UUID, Ledger> LEDGERS = new HashMap<>();
	/** Set while a ledger's reckoning is being dealt, so the blow itself is never counted into a ledger. */
	private static boolean settling;

	/**
	 * Reckoning: opens a ledger on the target for 4 seconds; every wound it takes meanwhile, from
	 * anything, is written in it, and then half comes due at once (at most 12). One ledger per creature:
	 * another Reckoning while it's open only weighs it (the stronger power), never opens a second.
	 */
	private static void reckoning(Cast cast, LivingEntity t, double power, double duration) {
		Ledger open = LEDGERS.get(t.getUUID());
		if (open != null && open.target == t) {
			open.power = Math.max(open.power, power);
			FusedVoidVfx.reckoningStamp(cast.level, t, open.facing);
			return;
		}
		int ticks = Math.max(10, Effects.ticks(FusedVoidRules.RECKONING_SECONDS, duration));
		Ledger ledger = new Ledger(cast, t, power, ticks);
		LEDGERS.put(t.getUUID(), ledger);
		FusedVoidVfx.reckoningOpen(cast.level, t, ledger.facing, ticks);
		// The seal starts to close a moment before the debt lands, so it slams shut as it does.
		Scheduler.later(Math.max(1, ticks - 4), () -> closing(ledger));
		Scheduler.later(ticks, () -> settle(ledger));
	}

	private static void weigh(LivingEntity entity) {
		if (LEDGERS.isEmpty()) {
			return;
		}
		Ledger ledger = LEDGERS.get(entity.getUUID());
		if (ledger != null && ledger.target == entity) {
			ledger.before = entity.getHealth() + entity.getAbsorptionAmount();
		}
	}

	private static void tally(LivingEntity entity, float damage) {
		if (LEDGERS.isEmpty() || settling) {
			return;
		}
		Ledger ledger = LEDGERS.get(entity.getUUID());
		if (ledger == null || ledger.target != entity || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		// What the wound really took (armour and all); the hit's own figure only if its start went unseen.
		float taken = Float.isNaN(ledger.before) ? damage : ledger.before - (entity.getHealth() + entity.getAbsorptionAmount());
		ledger.before = Float.NaN;
		if (taken <= 0) {
			return;
		}
		ledger.owed += taken;
		ledger.wounds++;
		int left = (int) Math.max(2, ledger.opened + ledger.ticks - level.getGameTime());
		FusedVoidVfx.reckoningCount(level, entity, ledger.facing, FusedVoidRules.reckoningDue(ledger.owed, 1.0) / FusedVoidRules.RECKONING_CAP, ledger.wounds, left);
	}

	private static void closing(Ledger ledger) {
		LivingEntity t = ledger.target;
		if (LEDGERS.get(t.getUUID()) == ledger && ledger.cast.alive() && onHand(ledger.cast, t)) {
			FusedVoidVfx.reckoningClosing(ledger.cast.level, t, ledger.facing, ledger.owed > 0);
		}
	}

	private static void settle(Ledger ledger) {
		LivingEntity t = ledger.target;
		if (!LEDGERS.remove(t.getUUID(), ledger)) {
			return;
		}
		Cast cast = ledger.cast;
		if (!cast.alive() || !onHand(cast, t)) {
			return;
		}
		double due = FusedVoidRules.reckoningDue(ledger.owed, ledger.power);
		FusedVoidVfx.reckoningSettle(cast.level, t, ledger.facing, due);
		if (due <= 0) {
			return;
		}
		settling = true;
		try {
			Effects.lingering(() -> Effects.hurt(cast, t, magic(cast), due));
		} finally {
			settling = false;
		}
	}

	// ------------------------------------------------------------------ Singularity

	/** How many black holes each caster has open. */
	private static final Map<UUID, Integer> HOLES = new HashMap<>();

	/**
	 * Singularity: a black hole hangs where the spell lands for 2.5 seconds, drawing in every enemy within
	 * 5 blocks that it can see (never through a wall, never a boss, at most 12), then bursts: 6 damage,
	 * and they're flung out once, less hard toward a wall close behind them.
	 */
	private static void singularity(Cast cast, Cast.Hit hit, double radius, double power) {
		ServerLevel level = cast.level;
		Vec3 centre = holeCentre(level, hit, cast.caster);
		UUID owner = cast.caster.getUUID();
		if (HOLES.getOrDefault(owner, 0) >= FusedVoidRules.SINGULARITY_OPEN) {
			FusedVoidVfx.singularityFizzle(level, centre);
			return;
		}
		HOLES.merge(owner, 1, Integer::sum);
		Vec3 disk = ElementFx.tilted(0.32, level.getRandom().nextDouble() * Math.PI * 2);
		Set<LivingEntity> caught = new LinkedHashSet<>();
		FusedVoidVfx.singularityOpen(level, centre, radius, disk, FusedVoidRules.SINGULARITY_TICKS);
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			if (!cast.alive()) {
				closeHole(owner);
				FusedVoidVfx.singularityFizzle(level, centre);
				return;
			}
			int now = tick[0]++;
			if (now < FusedVoidRules.SINGULARITY_TICKS) {
				FusedVoidVfx.singularity(level, centre, radius, disk, now);
				if (now % 2 == 0) {
					draw(cast, centre, radius, caught);
				}
				Scheduler.later(1, next[0]);
				return;
			}
			closeHole(owner);
			Effects.lingering(() -> burst(cast, centre, radius, disk, power, caught));
		};
		Scheduler.later(1, next[0]);
	}

	private static void closeHole(UUID owner) {
		HOLES.computeIfPresent(owner, (k, n) -> n <= 1 ? null : n - 1);
	}

	/** Where the hole hangs: where the spell landed, backed out of any wall, and at least a little above the ground. */
	private static Vec3 holeCentre(ServerLevel level, Cast.Hit hit, LivingEntity caster) {
		Vec3 at = hit.self() ? caster.position().add(0, 1.2, 0) : hit.point();
		BlockPos pos = BlockPos.containing(at);
		if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && hit.dir().lengthSqr() > 1.0E-6) {
			at = at.subtract(hit.dir().normalize().scale(0.7));
		}
		Vec3 ground = ElementFx.floor(level, at, 1.2);
		return ground != null ? new Vec3(at.x, ground.y + 1.2, at.z) : at;
	}

	/** One pull: every enemy it holds, and any new one in reach and in sight, drawn a step closer and a little round. */
	private static void draw(Cast cast, Vec3 centre, double radius, Set<LivingEntity> caught) {
		ServerLevel level = cast.level;
		caught.removeIf(v -> !onHand(cast, v));
		for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity v = (LivingEntity) e;
			Vec3 body = v.getBoundingBox().getCenter();
			Vec3 in = centre.subtract(body);
			double d = in.length();
			if (d > radius) {
				continue;
			}
			if (!caught.contains(v)) {
				if (caught.size() >= FusedVoidRules.SINGULARITY_CAUGHT || !sees(level, body, centre, v)) {
					continue;
				}
				caught.add(v);
			}
			if (Spirits.isBoss(v)) {
				continue;
			}
			Reactions.mark(v, Reactions.Mark.PULLED);
			v.resetFallDistance();
			if (d < 0.7) {
				// At the heart it's held, not thrown back and forth across it.
				setMotion(v, v.getDeltaMovement().scale(0.2));
				continue;
			}
			Vec3 dir = in.scale(1 / d);
			Vec3 around = new Vec3(-dir.z, 0, dir.x).scale(0.1);
			double speed = Math.min(0.55, 0.16 + (radius - d) * 0.05 + 0.3 / d);
			setMotion(v, v.getDeltaMovement().scale(0.35).add(dir.scale(speed)).add(around));
		}
	}

	private static boolean sees(ServerLevel level, Vec3 from, Vec3 to, Entity who) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who)).getType() == HitResult.Type.MISS;
	}

	private static void setMotion(LivingEntity target, Vec3 motion) {
		target.setDeltaMovement(motion);
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/** The hole bursts: 6 damage to all it held (and anything else of its in reach), each flung out once. */
	private static void burst(Cast cast, Vec3 centre, double radius, Vec3 disk, double power, Set<LivingEntity> caught) {
		ServerLevel level = cast.level;
		FusedVoidVfx.singularityBurst(level, centre, radius, disk);
		Set<LivingEntity> struck = new LinkedHashSet<>(caught);
		for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			if (struck.size() >= FusedVoidRules.SINGULARITY_CAUGHT) {
				break;
			}
			if (e.getBoundingBox().getCenter().distanceTo(centre) <= radius && sees(level, e.getBoundingBox().getCenter(), centre, e)) {
				struck.add((LivingEntity) e);
			}
		}
		for (LivingEntity v : struck) {
			if (!onHand(cast, v) || v.getBoundingBox().getCenter().distanceTo(centre) > radius + 1.5) {
				continue;
			}
			Effects.hurt(cast, v, magic(cast), FusedVoidRules.SINGULARITY_DAMAGE * power);
			if (Spirits.isBoss(v) || !v.isAlive()) {
				continue;
			}
			Vec3 away = Effects.horizontal(v.position().subtract(centre), cast.caster.getLookAngle());
			// Flung once, and less hard toward a wall close behind it, so nothing is dashed into stone.
			double room = room(level, v, away);
			double strength = FusedVoidRules.flingStrength(power, room);
			Effects.push(v, away.scale(strength).add(0, 0.45 + 0.25 * (1 - Math.min(1.0, room / 4.0)), 0));
			v.resetFallDistance();
			FusedVoidVfx.flung(level, v, away);
		}
	}

	/** How far {@code v} could fly along {@code away} before a wall (up to 4 blocks). */
	private static double room(ServerLevel level, LivingEntity v, Vec3 away) {
		Vec3 from = v.getBoundingBox().getCenter();
		Vec3 to = from.add(away.scale(4.0));
		HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v));
		return hit.getType() == HitResult.Type.MISS ? 4.0 : hit.getLocation().distanceTo(from);
	}

	// ------------------------------------------------------------------ Prismatic Burst

	/** Element colours for the marks Prismatic Burst uses up. */
	static final int BURNING = ElementFx.FIRE.primary();
	static final int FROZEN = ElementFx.FROST.primary();
	static final int WINDSWEPT = ElementFx.WIND.accent();
	static final int PULLED = ElementFx.VOID.primary();
	static final int SOAKED = 0x2F6BFF;
	static final int WET = 0x7CCBF2;

	/**
	 * Uses up every elemental mark on {@code t} (burning, frozen, windswept, pulled, soaked, wet) and says
	 * which, as their element's colours in that order.
	 */
	private static List<Integer> useMarks(LivingEntity t) {
		List<Integer> used = new ArrayList<>();
		if (t.isOnFire()) {
			t.clearFire();
			used.add(BURNING);
		}
		if (Reactions.has(t, Reactions.Mark.FROZEN) || t.isFullyFrozen()) {
			Reactions.clear(t, Reactions.Mark.FROZEN);
			t.setTicksFrozen(0);
			used.add(FROZEN);
		}
		if (Reactions.has(t, Reactions.Mark.WINDSWEPT)) {
			Reactions.clear(t, Reactions.Mark.WINDSWEPT);
			used.add(WINDSWEPT);
		}
		if (Reactions.has(t, Reactions.Mark.PULLED)) {
			Reactions.clear(t, Reactions.Mark.PULLED);
			used.add(PULLED);
		}
		if (Reactions.has(t, Reactions.Mark.SOAKED)) {
			Reactions.clear(t, Reactions.Mark.SOAKED);
			used.add(SOAKED);
		}
		if (Reactions.has(t, Reactions.Mark.WET)) {
			Reactions.clear(t, Reactions.Mark.WET);
			used.add(WET);
		}
		return used;
	}

	// ------------------------------------------------------------------ Chronoshift

	/** Until when each player's clock stays turned forward (a second Chronoshift meanwhile only refreshes the Haste and Speed). */
	private static final Map<UUID, Long> SHIFTED = new HashMap<>();

	/**
	 * Chronoshift on a player: every spell of theirs still cooling down comes off it 3 seconds sooner
	 * (more with power, at most twice that), except a spell holding Chronoshift itself (the one being
	 * cast, or any other), and never the shared cooldown of imbued things. Once per cast, and not again
	 * while their clock is still turned forward. Returns how many spells it turned.
	 */
	private static int turnClock(Cast cast, ServerPlayer player, double power, int window) {
		if (cast.passive) {
			// A spell that renews itself would keep every cooldown running fast for good.
			return 0;
		}
		long now = cast.level.getGameTime();
		Long until = SHIFTED.get(player.getUUID());
		if (until != null && now < until && until - now <= window * 4L) {
			return 0;
		}
		if (!cast.once("chronoshift:" + player.getUUID())) {
			return 0;
		}
		SHIFTED.put(player.getUUID(), now + window);
		if (SHIFTED.size() > 64) {
			SHIFTED.values().removeIf(t -> t < now);
		}
		int by = FusedVoidRules.chronoshiftTicks(power);
		Spellbook book = Spellbooks.get(player);
		int turned = 0;
		for (int slot = 0; slot < SpellSlots.ALL; slot++) {
			long ready = Spellbooks.readyAt(player, slot);
			if (ready <= now || holdsChronoshift(book, slot)) {
				continue;
			}
			Spellbooks.setReadyAt(player, slot, FusedVoidRules.shifted(ready, now, by));
			turned++;
		}
		return turned;
	}

	/** Whether spell {@code slot} has Chronoshift in it, threaded or tied inside a Knot. */
	private static boolean holdsChronoshift(Spellbook book, int slot) {
		if (slot < 0 || slot >= book.spells().size()) {
			return false;
		}
		for (String id : book.spells().get(slot)) {
			Optional<RuneDef> rune = Runes.get(id);
			if (rune.isPresent() && holds(rune.get(), 0)) {
				return true;
			}
		}
		return false;
	}

	private static boolean holds(RuneDef rune, int depth) {
		if (rune.is(Runes.CHRONOSHIFT.id())) {
			return true;
		}
		if (depth > Knots.MAX_DEPTH || !Knots.isKnot(rune)) {
			return false;
		}
		for (RuneDef inside : Knots.contents(rune)) {
			if (holds(inside, depth + 1)) {
				return true;
			}
		}
		return false;
	}
}
