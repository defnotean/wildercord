package dev.wildercord.cast;

import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * The signature fusions: fused runes made only from two particular effects at the Fusion Altar (see
 * {@code spell.Fusions.SIGNATURES}), each what its two runes do at once. {@link FusedEffects} hands each of them
 * here. Their look is in {@link SignatureVfx}; what stays on a creature and answers what happens to it (Stitchtime,
 * Doomclock, Riposte, Malison's curse passing on) is in {@link SignatureWards}; their numbers are in
 * {@link SignatureRules} and match the rune descriptions in {@code Runes}.
 * <ul>
 *   <li><b>Frostwire</b> (Chill and Shock): chilled, then a current through every cold enemy nearby.</li>
 *   <li><b>Seethe</b> (Bubble and Fire): a bubble of boiling water that bursts into scalding steam.</li>
 *   <li><b>Bloomstep</b> (Grow and Blink): a step through blossoms, flowers at both ends, regeneration where you land.</li>
 *   <li><b>Skyburst</b> (Launch and Explode): flung high, and blown apart at the top of the flight.</li>
 *   <li><b>Stitchtime</b> (Heal and Countdown): a heal, and the next seconds' wounds healed back at once.</li>
 *   <li><b>Parasite</b> (Venom and Leech): poisoned and drained, and it moves on when its host dies.</li>
 *   <li><b>Razorgale</b> (Windcut and Bleed): blades that cut, and come back round to tear the wounds.</li>
 *   <li><b>Doomclock</b> (Primer and Stasis): a clock every blow winds tighter, then a burst of it all.</li>
 *   <li><b>Thunderstep</b> (Shadowstep and Lightning): down as a bolt behind the first enemy.</li>
 *   <li><b>Halo</b> (Smite and Regrowth): a crown of light that smites the nearest enemy and heals its bearer.</li>
 *   <li><b>Thunderquake</b> (Thunderclap and Tremor): three shockwaves, harder the nearer the heart.</li>
 *   <li><b>Cometfall</b> (Starfall and Meteor): a comet, and its shards into the enemies around.</li>
 *   <li><b>Riposte</b> (Reflect and Foresight): the next blows sidestepped and answered.</li>
 *   <li><b>Dust Devil</b> (Summit Wind and Sandstorm): a wandering whirl that catches enemies up and flings them.</li>
 *   <li><b>Malison</b> (Hex and Resonance): a curse that passes on when its bearer dies.</li>
 *   <li><b>Avalanche</b> (Coldsnap and Stalactite): snow and ice crashing down, drifts left where they fell.</li>
 * </ul>
 * Every creature one of them reaches beyond what its shape hit comes out of the cast's creature budget, and the
 * ones that strike again and again (a quake's waves, a dust devil) take a fresh budget each time, as a
 * repeating shape does. Bosses are never held, lifted or carried off, only struck.
 */
final class SignatureFusions {
	private SignatureFusions() {}

	/** At most this many targets get a lingering part of their own, so one hit can't flood the server (as in FusedEffects). */
	static final int MAX_TARGETS = 8;
	/** At most this many enemies an area of these runes touches each time it strikes. */
	static final int MAX_IN_AREA = 16;

	/** The lingering parts running on each creature (a Parasite, a Halo), by key: a newer one takes over from the older. */
	private static final Map<String, Object> RUNNING = new HashMap<>();

	/** A Dust Devil one caster set loose: where it is now, and until when. */
	private static final class Devil {
		final UUID caster;
		final ServerLevel level;
		Vec3 at;
		long until;
		/** Kept going, never past this: three times its length. */
		final long cap;

		Devil(Cast cast, Vec3 at, int ticks) {
			this.caster = cast.caster.getUUID();
			this.level = cast.level;
			this.at = at;
			long now = cast.level.getGameTime();
			this.until = now + ticks;
			this.cap = now + 3L * ticks;
		}
	}

	private static final List<Devil> DEVILS = new ArrayList<>();
	/** Avalanche's drifts still lying, and when each melts: every one goes before the world is saved. */
	private static final Map<GlobalPos, Long> DRIFTS = new HashMap<>();
	private static final BlockState DRIFT = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2);

	/** Registers what these runes listen for; called once at startup (from {@link FusedEffects#init}). */
	static void init() {
		SignatureWards.init();
		ServerLifecycleEvents.SERVER_STOPPING.register(SignatureFusions::meltDrifts);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RUNNING.clear();
			DEVILS.clear();
			DRIFTS.clear();
			HALOS.clear();
		});
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		if (!Effects.builtIn(node.effect)) {
			return false;
		}
		double radius = SpellNumbers.effectRadius(node);
		switch (node.effect.path()) {
			case "frostwire" -> frostwire(cast, hit, harmed, power, duration, radius);
			case "seethe" -> {
				Set<UUID> scalded = new HashSet<>();
				Set<Entity> touched = new HashSet<>(hit.entities());
				first(harmed, SignatureRules.SEETHE_BUBBLES).forEach(t -> seethe(cast, t, power, duration, radius, scalded, touched));
			}
			case "bloomstep" -> bloomstep(cast, hit, duration, radius);
			case "skyburst" -> {
				Set<UUID> blasted = new HashSet<>();
				Set<Entity> touched = new HashSet<>(hit.entities());
				first(harmed, SignatureRules.SKYBURST_TARGETS).forEach(t -> skyburst(cast, t, power, radius, blasted, touched));
			}
			case "stitchtime" -> first(helped).forEach(t -> SignatureWards.stitch(cast, t, power, Effects.ticks(SignatureRules.STITCH_SECONDS, duration)));
			case "parasite" -> first(harmed).forEach(t -> parasite(cast, t, power,
				(int) Math.max(1, Math.round(SignatureRules.PARASITE_SECONDS * duration)), true));
			case "razorgale" -> razorgale(cast, hit, power, radius);
			case "doomclock" -> first(harmed, SignatureRules.DOOMCLOCK_TARGETS).forEach(t -> SignatureWards.doomclock(cast, t, power,
				Effects.ticks(SignatureRules.DOOMCLOCK_SECONDS, duration), SignatureRules.DOOMCLOCK_RADIUS * radius, hit.entities()));
			case "thunderstep" -> thunderstep(cast, hit, power, radius);
			case "halo" -> first(helped).forEach(t -> halo(cast, t, power, Effects.ticks(SignatureRules.HALO_SECONDS, duration), SignatureRules.HALO_REACH * radius));
			case "thunderquake" -> thunderquake(cast, hit, power, radius);
			case "cometfall" -> cometfall(cast, hit, harmed, power, radius);
			case "riposte" -> first(helped).forEach(t -> SignatureWards.riposte(cast, t, power, Effects.ticks(SignatureRules.RIPOSTE_SECONDS, duration)));
			case "dust_devil" -> dustDevil(cast, hit, power, Effects.ticks(SignatureRules.DEVIL_SECONDS, duration), radius);
			case "malison" -> {
				for (int i = 0; i < harmed.size(); i++) {
					malison(cast, harmed.get(i), power, Effects.ticks(SignatureRules.MALISON_SECONDS, duration), SignatureRules.MALISON_REACH * radius,
						i < MAX_TARGETS);
				}
			}
			case "avalanche" -> avalanche(cast, hit, harmed, power, duration, radius);
			default -> {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ Frostwire (Chill and Shock)

	/**
	 * Frostwire: every target is chilled (Slowness II for 4 seconds, frost on its skin, brittle for Shatter); a
	 * moment later a current runs from each of the first few through every chilled or frozen enemy within 6 blocks
	 * of it, 6 at most, nearest first: 4 damage each, 6 to one frozen solid. However many currents reach an enemy,
	 * it's struck once.
	 */
	private static void frostwire(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double power, double duration, double radius) {
		ServerLevel level = cast.level;
		List<LivingEntity> origins = first(harmed, SignatureRules.FROSTWIRE_ORIGINS);
		for (LivingEntity t : harmed) {
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Effects.ticks(SignatureRules.FROSTWIRE_SLOW_SECONDS, duration), 1, false, true), cast.caster);
			Reactions.mark(t, Reactions.Mark.FROZEN, 40);
			chill(t, 40);
			if (origins.contains(t)) {
				SignatureVfx.frostwireChill(level, t);
			}
		}
		if (origins.isEmpty()) {
			return;
		}
		double reach = SignatureRules.FROSTWIRE_REACH * radius;
		Set<UUID> struck = new HashSet<>();
		Set<Entity> touched = new HashSet<>(hit.entities());
		Scheduler.later(SignatureRules.FROSTWIRE_DELAY, Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			DamageSource shock = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster);
			for (LivingEntity origin : origins) {
				if (!onHand(cast, origin)) {
					continue;
				}
				List<LivingEntity> chain = new ArrayList<>();
				for (LivingEntity t : nearest(cast, origin.getBoundingBox().getCenter(), reach, MAX_IN_AREA)) {
					if (chain.size() >= SignatureRules.FROSTWIRE_CHAIN) {
						break;
					}
					if (!struck.contains(t.getUUID()) && FusedFrost.alreadyCold(t)) {
						chain.add(t);
					}
				}
				chain = take(cast, chain, touched);
				if (chain.isEmpty()) {
					continue;
				}
				SignatureVfx.frostwire(level, origin, chain);
				for (LivingEntity t : chain) {
					struck.add(t.getUUID());
					boolean frozen = t.isFullyFrozen();
					SignatureVfx.frostwireStrike(level, t, frozen);
					Effects.hurt(cast, t, shock, SignatureRules.frostwireDamage(frozen) * power * Reactions.storm(cast, t));
				}
			}
		}));
	}

	// ------------------------------------------------------------------ Seethe (Bubble and Fire)

	/**
	 * Seethe: the target floats in a bubble of boiling water for 2 seconds, scalded for 1 every half second (a boss
	 * isn't lifted, only slowed); then the bubble bursts into steam: 4 damage to every enemy within 2.5 blocks,
	 * Blindness for 2 seconds, and soaked. An enemy caught by several bursts of one cast is scalded by one.
	 */
	private static void seethe(Cast cast, LivingEntity t, double power, double duration, double radius, Set<UUID> scalded, Set<Entity> touched) {
		ServerLevel level = cast.level;
		int ticks = Effects.ticks(SignatureRules.SEETHE_SECONDS, duration);
		if (t instanceof Mob) {
			Spirits.hold(t, ticks);
		} else {
			t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0, false, false));
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 3, false, false));
		}
		SignatureVfx.seetheTrap(level, t);
		DamageSource heat = level.damageSources().source(DamageTypes.HOT_FLOOR, cast.caster);
		Vec3[] last = {t.getBoundingBox().getCenter()};
		steps(cast, 2, ticks, tick -> {
			if (!onHand(cast, t)) {
				return;
			}
			// Floating up in the bubble, as Bubble floats its catch.
			Vec3 up = t.position().add(0, 0.06, 0);
			if (t instanceof Mob && !Spirits.isBoss(t) && fits(level, t, up)) {
				teleport(t, level, up);
			}
			last[0] = t.getBoundingBox().getCenter();
			if (tick % 4 == 0) {
				SignatureVfx.seetheBoil(level, t);
			}
			if (tick > 0 && tick % SignatureRules.SEETHE_EVERY == 0) {
				Effects.hurt(cast, t, heat, SignatureRules.SEETHE_SCALD * power);
			}
		}, () -> {
			if (onHand(cast, t)) {
				// The last scald, as the bubble's time runs out (one every half second in all: see SignatureRules.seetheScalds).
				if (ticks % SignatureRules.SEETHE_EVERY == 0) {
					Effects.lingering(() -> Effects.hurt(cast, t, heat, SignatureRules.SEETHE_SCALD * power));
				}
				t.removeEffect(MobEffects.LEVITATION);
				last[0] = t.getBoundingBox().getCenter();
			}
			double r = SignatureRules.SEETHE_RADIUS * radius;
			SignatureVfx.seetheBurst(level, last[0], r);
			for (LivingEntity e : take(cast, nearest(cast, last[0], r, MAX_IN_AREA), touched)) {
				if (!scalded.add(e.getUUID())) {
					continue;
				}
				Effects.hurt(cast, e, heat, SignatureRules.SEETHE_BURST * power * Reactions.fire(cast, e));
				e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Effects.ticks(SignatureRules.SEETHE_BLIND_SECONDS, duration), 0, false, true));
				if (e instanceof Mob mob) {
					mob.setTarget(null);
				}
				// Soaked last, so its own steam isn't dulled by the water it leaves.
				Reactions.mark(e, Reactions.Mark.SOAKED);
			}
		});
	}

	// ------------------------------------------------------------------ Bloomstep (Grow and Blink)

	/**
	 * Bloomstep: the caster steps to where the spell landed (32 blocks at most, always onto safe ground, as Blink),
	 * bone meal blooming the ground they left and the ground they reach (for a player allowed to build there), and
	 * they and their allies within 3 blocks of where they arrive get Regeneration I for 5 seconds. On Self, all of
	 * it happens where they stand.
	 */
	private static void bloomstep(Cast cast, Cast.Hit hit, double duration, double radius) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 from = caster.position();
		Vec3 arrive = from;
		if (!hit.self()) {
			if(hit.point().distanceTo(from)>SignatureRules.BLOOMSTEP_RANGE){LifeOwnerEvents.refused(cast,"bloomstep",caster);
				Casters.tell(caster, Component.translatableWithFallback("message.wildercord.bloomstep_far", "Too far to step (32 blocks at most)").withColor(0x6EDC64));
				return;
			}
			Vec3 spot = landing(cast, hit.point(), hit.dir());
			if(spot==null){
				LifeOwnerEvents.refused(cast,"bloomstep",caster);
				Casters.tell(caster, Component.translatableWithFallback("message.wildercord.step_nowhere", "Nowhere safe to set foot there").withColor(0x6EDC64));
				return;
			}
			boolean stepped=caster.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), caster.getYRot(), caster.getXRot(), false);
			if(!stepped || !cast.alive() || caster.position().distanceToSqr(spot)>.0001){
				LifeOwnerEvents.refused(cast,"bloomstep",caster);return;
			}
			caster.resetFallDistance();arrive=caster.position();
			if(arrive.distanceToSqr(from)>.0001)LifeOwnerEvents.admitted(cast,"bloomstep",caster,LifeOwnerEvents.Moment.APPLY,1,from);
		}
		blossom(cast, from);
		if (arrive != from) {
			blossom(cast, arrive);
		}
		int ticks = Effects.ticks(SignatureRules.BLOOMSTEP_REGEN_SECONDS, duration);
		double r = SignatureRules.BLOOMSTEP_RADIUS * radius;
		Vec3 centre = arrive.add(0, 1, 0);
		int eased = 0;
		for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(r + 1), e -> Targets.canHelp(caster, e))) {
			if (eased >= MAX_IN_AREA || e.getBoundingBox().getCenter().distanceTo(centre) > r + e.getBbWidth() / 2) {
				continue;
			}
			eased++;
			LivingEntity ally = (LivingEntity) e;
			if(ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,ticks,0,false,true)))LifeOwnerEvents.admitted(cast,"bloomstep",ally,LifeOwnerEvents.Moment.RENEW,1,from);
		}
	}

	/** Bone meal on the ground at {@code feet} and a couple of blocks beside it: grass and flowers, where the caster may build. */
	private static void blossom(Cast cast, Vec3 feet) {
		ServerLevel level = cast.level;
		BlockPos under = BlockPos.containing(feet.x, feet.y - 0.5, feet.z);
		List<BlockPos> spots = new ArrayList<>(List.of(under));
		for (BlockPos p : BlockPos.betweenClosed(under.offset(-1, -1, -1), under.offset(1, 1, 1))) {
			if (spots.size() >= SignatureRules.BLOOMSTEP_BLOOMS) {
				break;
			}
			if (!p.equals(under) && level.getBlockState(p).getBlock() instanceof BonemealableBlock) {
				spots.add(p.immutable());
			}
		}
		for (BlockPos p : spots) {
			// Only what bone meal would grow is asked about (claims hear it as a break), and each comes out of the block budget.
			if (!(level.getBlockState(p).getBlock() instanceof BonemealableBlock) || !mayEdit(cast, p)) {
				continue;
			}
			try(var observed=LifeGrowthWrites.open(cast,"bloomstep")){
				BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),level,p);
			}
		}
	}

	// ------------------------------------------------------------------ Skyburst (Launch and Explode)

	/**
	 * Skyburst: the target is flung high (windswept on the way), and at the top of its flight it explodes and rains
	 * fire down: 7 damage to it and every enemy within 3 blocks of it or beneath it, weaker toward the edge, setting
	 * them alight. The windswept one's own blast sets off Wildfire. A boss isn't flung: it goes off where it stands a
	 * moment later. Each enemy is caught by one blast of a cast at most.
	 */
	private static void skyburst(Cast cast, LivingEntity t, double power, double radius, Set<UUID> blasted, Set<Entity> touched) {
		ServerLevel level = cast.level;
		if (!Spirits.isBoss(t)) {
			Vec3 v = t.getDeltaMovement();
			Effects.push(t, new Vec3(v.x, Math.max(0, v.y), v.z).add(0, SignatureRules.SKYBURST_LIFT, 0).subtract(v));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT, SignatureRules.SKYBURST_LATEST + 20);
		}
		SignatureVfx.skyburstFling(level, t);
		Vec3[] last = {t.getBoundingBox().getCenter()};
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			tick[0]++;
			boolean here = onHand(cast, t);
			if (here) {
				last[0] = t.getBoundingBox().getCenter();
				if (tick[0] % 3 == 0) {
					SignatureVfx.skyburstRise(level, t);
				}
			}
			if (here && !SignatureRules.skyburstApex(tick[0], Spirits.isBoss(t) ? 0 : t.getDeltaMovement().y)) {
				Scheduler.later(1, next[0]);
				return;
			}
			skyblast(cast, last[0], SignatureRules.SKYBURST_RADIUS * radius, power, blasted, touched);
		});
		Scheduler.later(1, next[0]);
	}

	/**
	 * One of Skyburst's blasts at {@code at}: fire damage to every enemy within {@code radius} of it or beneath it down to
	 * the ground (weaker toward the edge), set alight, thrown back. Never breaks blocks.
	 */
	private static void skyblast(Cast cast, Vec3 at, double radius, double power, Set<UUID> blasted, Set<Entity> touched) {
		ServerLevel level = cast.level;
		// Enemies just pulled together implode, as any blast's do.
		double implode = Reactions.blast(cast, at, radius);
		double r = radius * implode;
		double strength = power * (implode > 1 ? 1.3 : 1.0);
		Vec3 floor = CastEngine.ground(level, at);
		SignatureVfx.skyburst(level, at, floor, r);
		DamageSource blast = level.damageSources().explosion(cast.caster, cast.caster);
		// A column of it: the ball of fire, and the fire it rains down on everything under it.
		List<LivingEntity> caught = new ArrayList<>();
		for (Entity e : level.getEntities((Entity) null, new AABB(at.x - r - 1, Math.min(floor.y, at.y) - 1, at.z - r - 1, at.x + r + 1, at.y + r, at.z + r + 1),
				e -> Targets.canHarm(cast.caster, e))) {
			Vec3 c = e.getBoundingBox().getCenter();
			boolean inBall = c.distanceTo(at) <= r + e.getBbWidth() / 2;
			boolean beneath = c.y <= at.y && horizontal(c, at) <= r + e.getBbWidth() / 2;
			if (caught.size() < MAX_IN_AREA && (inBall || beneath)) {
				caught.add((LivingEntity) e);
			}
		}
		caught.sort(Comparator.comparingDouble(e -> horizontal(e.getBoundingBox().getCenter(), at)));
		for (LivingEntity e : take(cast, caught, touched)) {
			if (!blasted.add(e.getUUID())) {
				continue;
			}
			double falloff = SignatureRules.blastFalloff(horizontal(e.getBoundingBox().getCenter(), at), r);
			Effects.hurt(cast, e, blast, SignatureRules.SKYBURST_DAMAGE * strength * falloff * Reactions.fire(cast, e));
			e.igniteForSeconds(SignatureRules.SKYBURST_BURN_SECONDS);
			Vec3 away = e.getBoundingBox().getCenter().subtract(at);
			Effects.push(e, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(0.7 * falloff).add(0, 0.2, 0));
		}
	}

	// ------------------------------------------------------------------ Parasite (Venom and Leech)

	/**
	 * Parasite: Poison I for {@code seconds}, and once a second it drains 1 from its host into the caster (fed only
	 * from 32 blocks). The same caster planting another in the same host starts it over. If its host dies with it
	 * inside ({@code leaps}), it leaps to the nearest enemy within 6 blocks with the seconds it had left, and that
	 * one can't leap again.
	 */
	private static void parasite(Cast cast, LivingEntity t, double power, int seconds, boolean leaps) {
		ServerLevel level = cast.level;
		t.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, 0, false, true), cast.caster);
		SignatureVfx.parasite(level, t);
		String key = key("parasite", t, cast);
		Object token = claim(key);
		drain(cast, t, power, seconds, 1, leaps, key, token);
	}

	private static void drain(Cast cast, LivingEntity t, double power, int seconds, int beat, boolean leaps, String key, Object token) {
		ServerLevel level = cast.level;
		Scheduler.later(20, Effects.carryContext(() -> {
			if (!current(key, token)) {
				return;
			}
			if (!cast.alive()) {
				release(key, token);
				return;
			}
			if (!onHand(cast, t)) {
				release(key, token);
				// Only a death sends it on: not a host that just went (unloaded, despawned, or through a portal).
				if (leaps && t.isDeadOrDying() && t.level() == cast.level) {
					Vec3 from = t.getBoundingBox().getCenter();
					LivingEntity next = nearestOther(cast, from, SignatureRules.PARASITE_LEAP, t);
					if (next != null && cast.takeEntities(1) > 0) {
						SignatureVfx.parasiteLeap(level, from, next);
						parasite(cast, next, power, SignatureRules.parasiteLeft(seconds, beat - 1), false);
					}
				}
				return;
			}
			float before = t.getHealth();
			Effects.lingering(() -> Effects.hurt(cast, t, magic(cast), SignatureRules.PARASITE_DRAIN * power));
			float taken = Math.max(0.0F, before - t.getHealth());
			LivingEntity caster = cast.caster;
			boolean fed = taken > 0 && caster.isAlive() && caster.distanceToSqr(t) <= SignatureRules.PARASITE_FEED_RANGE * SignatureRules.PARASITE_FEED_RANGE;
			if (fed) {
				caster.heal(taken);
			}
			SignatureVfx.parasiteDrain(level, t, fed ? caster : null);
			if (beat < seconds) {
				drain(cast, t, power, seconds, beat + 1, leaps, key, token);
			} else {
				release(key, token);
			}
		}));
	}

	// ------------------------------------------------------------------ Razorgale (Windcut and Bleed)

	/**
	 * Razorgale: a whirl of blades round where it lands cuts every enemy within 3 blocks for 2 and leaves it bleeding;
	 * half a second later the gale comes back round and cuts every enemy there for 2 again. That second cut is wind on
	 * a bleeding foe: it tears the wound open (Rupture: +50%, 4 more through armour, and the caster heals).
	 */
	private static void razorgale(Cast cast, Cast.Hit hit, double power, double radius) {
		ServerLevel level = cast.level;
		Vec3 centre = hit.self() ? cast.caster.position() : CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		double r = SignatureRules.RAZORGALE_RADIUS * radius;
		DamageSource wind = level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster);
		Set<Entity> touched = new HashSet<>(hit.entities());
		SignatureVfx.razorgale(level, centre, r, false);
		for (LivingEntity t : take(cast, around(cast, centre, r), touched)) {
			SignatureVfx.razorCut(level, t, centre, false);
			Effects.hurt(cast, t, wind, SignatureRules.RAZORGALE_CUT * power);
			Reactions.mark(t, Reactions.Mark.BLEEDING, SignatureRules.RAZORGALE_BLEED_TICKS);
		}
		Scheduler.later(SignatureRules.RAZORGALE_RETURN, Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			SignatureVfx.razorgale(level, centre, r, true);
			for (LivingEntity t : take(cast, around(cast, centre, r), touched)) {
				SignatureVfx.razorCut(level, t, centre, true);
				Effects.hurt(cast, t, wind, SignatureRules.RAZORGALE_CUT * power);
			}
		}));
	}

	// ------------------------------------------------------------------ Thunderstep (Shadowstep and Lightning)

	/**
	 * Thunderstep: the caster comes down as a bolt of lightning (a harmless one: the damage is the spell's) right behind
	 * the first enemy the spell hit, or where it landed (24 blocks at most, always onto safe ground), and every enemy
	 * within 2.5 blocks takes 8 storm damage and is stunned for half a second. On Self it strikes where they stand.
	 */
	private static void thunderstep(Cast cast, Cast.Hit hit, double power, double radius) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 spot = caster.position();
		if (!hit.self()) {
			LivingEntity enemy = firstEnemy(cast, hit);
			Vec3 aim = enemy != null ? enemy.position() : hit.point();
			if (aim.distanceTo(caster.position()) > SignatureRules.THUNDERSTEP_RANGE) {
				Casters.tell(caster, Component.translatableWithFallback("message.wildercord.thunderstep_far", "Too far to strike (24 blocks at most)").withColor(0xFFE650));
				return;
			}
			Vec3 found = enemy != null ? behind(cast, enemy) : null;
			if (found == null) {
				found = landing(cast, hit.point(), hit.dir());
			}
			if (found == null) {
				Casters.tell(caster, Component.translatableWithFallback("message.wildercord.step_nowhere", "Nowhere safe to set foot there").withColor(0xFFE650));
				return;
			}
			Vec3 from = caster.position();
			float yaw = caster.getYRot();
			if (enemy != null) {
				Vec3 look = enemy.position().subtract(found);
				yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
			}
			caster.teleportTo(level, found.x, found.y, found.z, Set.<Relative>of(), yaw, caster.getXRot(), false);
			caster.resetFallDistance();
			spot = found;
			SignatureVfx.thunderstepLeave(level, from);
		}
		double r = SignatureRules.THUNDERSTEP_RADIUS * radius;
		List<LivingEntity> caught = take(cast, around(cast, spot, r), new HashSet<>(hit.entities()));
		SignatureVfx.thunderstep(level, spot, r, !caught.isEmpty());
		DamageSource shock = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster);
		for (LivingEntity t : caught) {
			Effects.hurt(cast, t, shock, SignatureRules.THUNDERSTEP_DAMAGE * power * Reactions.storm(cast, t));
			Spirits.hold(t, SignatureRules.THUNDERSTEP_STUN);
		}
	}

	/** Somewhere right behind {@code target} (or beside it) for the caster to stand, as Shadowstep finds, or null. */
	private static Vec3 behind(Cast cast, LivingEntity target) {
		LivingEntity caster = cast.caster;
		Vec3 facing = Effects.horizontal(target.getLookAngle(), caster.getLookAngle());
		Vec3 side = new Vec3(-facing.z, 0, facing.x);
		double back = target.getBbWidth() / 2 + 0.9;
		Vec3 base = target.position();
		for (Vec3 offset : List.of(facing.scale(-back), side.scale(back), side.scale(-back))) {
			Vec3 spot = CastEngine.ground(cast.level, base.add(offset).add(0, 0.5, 0));
			if (Math.abs(spot.y - base.y) <= 2.5 && Effects.safeSpot(cast.level, caster, spot)) {
				return spot;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ Halo (Smite and Regrowth)

	/** A Halo over an ally: its caster's cast (the newest), how hard it smites and how far, and until when. */
	private static final class Halo {
		final LivingEntity ally;
		Cast cast;
		double power;
		double reach;
		long until;
		/** Kept going, never past this: three times its length. */
		final long cap;

		Halo(LivingEntity ally, Cast cast, double power, double reach, long now, int ticks) {
			this.ally = ally;
			this.cast = cast;
			this.power = power;
			this.reach = reach;
			this.until = now + ticks;
			this.cap = now + 3L * ticks;
		}
	}

	private static final Map<UUID, Halo> HALOS = new HashMap<>();

	/**
	 * Halo: a crown of light over the ally for {@code ticks}. A moment in and then every 2 seconds it smites the
	 * nearest enemy within {@code reach} of them that they can see, 3 holy damage (tripled against undead), and the
	 * ally heals 1 each time it does. One halo to an ally: cast again while it shines (a Zone's next pulse, an Echo),
	 * it shines on (never past three times its length) at the newest caster's strength, keeping its beat, rather than
	 * a second one starting. Its smites are lingering damage, which a Shield can block but not parry.
	 */
	private static void halo(Cast cast, LivingEntity ally, double power, int ticks, double reach) {
		ServerLevel level = cast.level;
		long now = level.getGameTime();
		Halo old = HALOS.get(ally.getUUID());
		if (old != null && old.ally == ally && now < old.until && old.cast.level == level) {
			old.until = Math.min(old.cap, Math.max(old.until, now + ticks));
			old.cast = cast;
			old.power = power;
			old.reach = reach;
			return;
		}
		Halo halo = new Halo(ally, cast, power, reach, now, ticks);
		HALOS.put(ally.getUUID(), halo);
		SignatureVfx.haloOpen(level, ally);
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			Cast by = halo.cast;
			if (HALOS.get(ally.getUUID()) != halo) {
				return;
			}
			if (!by.alive() || !onHand(by, ally) || level.getGameTime() >= halo.until) {
				HALOS.remove(ally.getUUID(), halo);
				if (onHand(by, ally)) {
					SignatureVfx.haloClose(level, ally);
				}
				return;
			}
			SignatureVfx.haloGlow(level, ally);
			if (SignatureRules.haloSmitesAt(tick[0])) {
				// A guardian answers whoever hurt the ally a moment ago; with no one to answer, it smites the nearest enemy.
				LivingEntity attacker = ally.getLastHurtByMob();
				boolean answers = attacker != null && attacker.isAlive() && ally.tickCount - ally.getLastHurtByMobTimestamp() <= 40 && attacker.distanceTo(ally) <= halo.reach
					&& Targets.canHarm(by.caster, attacker) && ally.hasLineOfSight(attacker);
				LivingEntity foe = answers ? attacker : nearestSeen(by, ally, halo.reach);
				if (foe != null) {
					double undead = foe.isInvertedHealAndHarm() ? SignatureRules.HALO_UNDEAD : 1.0;
					SignatureVfx.haloSmite(level, ally, foe);
					Effects.lingering(() -> Effects.hurt(by, foe, magic(by), SignatureRules.HALO_SMITE * halo.power * undead));
					ally.heal((float) (SignatureRules.HALO_HEAL * halo.power));
				}
			}
			tick[0] += 10;
			Scheduler.later(10, next[0]);
		});
		Scheduler.later(1, next[0]);
	}

	/** The enemy of the caster nearest {@code from} within {@code reach} that {@code from} can see, or null. */
	private static LivingEntity nearestSeen(Cast cast, LivingEntity from, double reach) {
		for (LivingEntity t : nearest(cast, from.getBoundingBox().getCenter(), reach, MAX_IN_AREA)) {
			if (t != from && from.hasLineOfSight(t)) {
				return t;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ Thunderquake (Thunderclap and Tremor)

	/**
	 * Thunderquake: three shockwaves roll out from where it lands, a second apart in all, reaching 2, 4 and 6 blocks.
	 * Each strikes every enemy on the ground it reaches for 4 and tosses it up (a boss isn't tossed), so the nearer
	 * the heart, the more waves reach it. Each wave strikes afresh, with its own creature budget.
	 */
	private static void thunderquake(Cast cast, Cast.Hit hit, double power, double radius) {
		ServerLevel level = cast.level;
		Vec3 centre = hit.self() ? cast.caster.position() : CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		SignatureVfx.quakeOpen(level, centre);
		Set<Entity> touched = new HashSet<>(hit.entities());
		for (int wave = 1; wave <= SignatureRules.QUAKE_WAVES; wave++) {
			int w = wave;
			Scheduler.later((wave - 1) * SignatureRules.QUAKE_EVERY, Effects.carryContext(() -> {
				if (!cast.alive()) {
					return;
				}
				double reach = SignatureRules.quakeReach(w) * radius;
				SignatureVfx.quakeWave(level, centre, reach, w);
				Cast strike = w == 1 ? cast : cast.pulse();
				DamageSource ground = level.damageSources().source(DamageTypes.FALLING_BLOCK, cast.caster);
				List<LivingEntity> reached = new ArrayList<>();
				for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(reach + 1, 3.0, reach + 1), e -> Targets.canHarm(cast.caster, e))) {
					double dy = e.getY() - centre.y;
					if (reached.size() < MAX_IN_AREA && dy >= -1.5 && dy <= 2.5 && horizontal(e.position(), centre) <= reach + e.getBbWidth() / 2) {
						reached.add((LivingEntity) e);
					}
				}
				reached.sort(Comparator.comparingDouble(e -> horizontal(e.position(), centre)));
				// The first wave arrives with the spell (what its shape hit is already paid for); the later ones strike afresh.
				List<LivingEntity> struck = w == 1 ? take(cast, reached, touched) : reached.subList(0, strike.takeEntities(reached.size()));
				for (LivingEntity t : struck) {
					SignatureVfx.quakeStrike(level, t);
					Effects.hurt(strike, t, ground, SignatureRules.QUAKE_DAMAGE * power);
					if (!Spirits.isBoss(t)) {
						Effects.push(t, new Vec3(0, 0.35, 0));
					}
				}
			}));
		}
	}

	// ------------------------------------------------------------------ Cometfall (Starfall and Meteor)

	/**
	 * Cometfall: a comet falls on the first target (or the point) a second later. 16 damage to every enemy within 4
	 * blocks, weaker toward the edge, setting them alight; then five shards of it strike the nearest other enemies
	 * within 10 blocks for 4 each (one each; a shard with nobody left for it strikes the ground). Never breaks blocks.
	 */
	private static void cometfall(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double power, double radius) {
		ServerLevel level = cast.level;
		Vec3 at = CastEngine.ground(level, (harmed.isEmpty() ? hit.point() : harmed.getFirst().position()).add(0, 0.5, 0));
		double r = SignatureRules.COMET_RADIUS * radius;
		SignatureVfx.cometMark(level, at, r, SignatureRules.COMET_DELAY);
		for (int i = 1; i < 5; i++) {
			double fall = i / 5.0;
			Scheduler.later(i * SignatureRules.COMET_DELAY / 5, () -> {
				if (cast.alive()) {
					SignatureVfx.cometFalling(level, at, fall);
				}
			});
		}
		Set<Entity> touched = new HashSet<>(hit.entities());
		Scheduler.later(SignatureRules.COMET_DELAY, Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			SignatureVfx.cometImpact(level, at, r);
			Vec3 heart = at.add(0, 1, 0);
			Set<LivingEntity> blasted = new HashSet<>();
			for (LivingEntity t : take(cast, nearest(cast, heart, r, MAX_IN_AREA), touched)) {
				blasted.add(t);
				double d = t.getBoundingBox().getCenter().distanceTo(heart);
				Effects.hurt(cast, t, magic(cast), SignatureRules.cometDamage(d, r) * power);
				t.igniteForSeconds(SignatureRules.COMET_BURN_SECONDS);
				Vec3 away = Effects.horizontal(t.position().subtract(at), cast.caster.getLookAngle());
				Effects.push(t, away.scale(0.6).add(0, 0.35, 0));
			}
			List<LivingEntity> others = new ArrayList<>();
			for (LivingEntity t : Exposed.first(nearest(cast, heart, SignatureRules.COMET_SHARD_REACH, MAX_IN_AREA), heart)) {
				if (others.size() < SignatureRules.COMET_SHARDS && !blasted.contains(t)) {
					others.add(t);
				}
			}
			others = take(cast, others, touched);
			for (int i = 0; i < SignatureRules.COMET_SHARDS; i++) {
				LivingEntity target = i < others.size() ? others.get(i) : null;
				int shard = i;
				Scheduler.later(3 + 2 * i, Effects.carryContext(() -> {
					if (!cast.alive()) {
						return;
					}
					if (target == null || !onHand(cast, target)) {
						double a = shard * Math.PI * 2 / SignatureRules.COMET_SHARDS + level.getRandom().nextDouble() * 0.6;
						double far = r + 1.5 + level.getRandom().nextDouble() * 3;
						SignatureVfx.cometShard(level, heart, CastEngine.ground(level, at.add(Math.cos(a) * far, 1.5, Math.sin(a) * far)));
						return;
					}
					SignatureVfx.cometShard(level, heart, target.getBoundingBox().getCenter());
					Effects.hurt(cast, target, magic(cast), SignatureRules.COMET_SHARD_DAMAGE * power);
				}));
			}
		}));
	}

	// ------------------------------------------------------------------ Dust Devil (Summit Wind and Sandstorm)

	/**
	 * Dust Devil: a whirl of sand touches down where it lands for {@code ticks} and drifts after the nearest enemy
	 * within 12 blocks, 3 blocks a second. Every quarter second enemies within 2 blocks of it are caught up, whirled
	 * round it (a boss is only blinded and scoured, never carried), blinded and windswept; once a second they're
	 * scoured for 3. When it blows out it flings whoever it still holds high. Cast again on its own devil (a Zone's
	 * next pulse, an Echo), the devil keeps going instead of a second one spinning up beside it.
	 */
	private static void dustDevil(Cast cast, Cast.Hit hit, double power, int ticks, double radius) {
		ServerLevel level = cast.level;
		Vec3 start = hit.self() ? cast.caster.position() : CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		if (renewed(cast, start, ticks)) {
			return;
		}
		Devil devil = new Devil(cast, start, ticks);
		DEVILS.add(devil);
		double reach = SignatureRules.DEVIL_REACH * radius;
		DamageSource sand = level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster);
		Set<LivingEntity> caught = new LinkedHashSet<>();
		SignatureVfx.devilOpen(level, start, reach);
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			if (!cast.alive() || level.getGameTime() >= devil.until) {
				DEVILS.remove(devil);
				if (cast.alive()) {
					Effects.lingering(() -> devilEnd(cast, devil, caught, reach));
				}
				return;
			}
			int now = tick[0];
			Effects.lingering(() -> devilStep(cast, devil, caught, reach, power, sand, now));
			tick[0] += 5;
			Scheduler.later(5, next[0]);
		});
		Scheduler.later(1, next[0]);
	}

	/** A quarter second of a dust devil: it drifts after its quarry, catches and whirls whoever is near, and scours them each second. */
	private static void devilStep(Cast cast, Devil devil, Set<LivingEntity> caught, double reach, double power, DamageSource sand, int tick) {
		ServerLevel level = cast.level;
		LivingEntity quarry = null;
		for (LivingEntity t : nearest(cast, devil.at.add(0, 1, 0), SignatureRules.DEVIL_CHASE, MAX_IN_AREA)) {
			// It goes after someone it hasn't caught yet, if there's anyone.
			if (quarry == null || caught.contains(quarry) && !caught.contains(t)) {
				quarry = t;
			}
		}
		if (quarry != null) {
			Vec3 to = new Vec3(quarry.getX() - devil.at.x, 0, quarry.getZ() - devil.at.z);
			double drift = SignatureRules.devilDrift(to.length());
			if (drift > 0) {
				Vec3 moved = devil.at.add(to.normalize().scale(drift));
				// Over a step up or down, never through a wall or off a cliff.
				Vec3 grounded = CastEngine.ground(level, moved.add(0, 1.2, 0));
				if (Math.abs(grounded.y - devil.at.y) <= 1.6) {
					devil.at = grounded;
				}
			}
		}
		SignatureVfx.devil(level, devil.at, reach, tick);
		Vec3 eye = devil.at.add(0, 1.0, 0);
		Cast strike = cast.pulse();
		List<LivingEntity> near = new ArrayList<>();
		for (Entity e : level.getEntities((Entity) null, new AABB(devil.at, devil.at).inflate(reach + 1, 0, reach + 1).expandTowards(0, 4, 0).move(0, -1, 0),
				e -> Targets.canHarm(cast.caster, e))) {
			if (near.size() < MAX_IN_AREA && horizontal(e.position(), devil.at) <= reach + e.getBbWidth() / 2) {
				near.add((LivingEntity) e);
			}
		}
		near = near.subList(0, strike.takeEntities(near.size()));
		caught.removeIf(t -> !onHand(cast, t) || horizontal(t.position(), devil.at) > reach + 2.5);
		for (LivingEntity t : near) {
			caught.add(t);
			t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, true));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT, 30);
			if (!Spirits.isBoss(t)) {
				whirl(t, eye, reach, tick);
			}
			if (tick % 20 == 0) {
				SignatureVfx.devilScour(level, t);
				Effects.hurt(strike, t, sand, SignatureRules.DEVIL_SCOUR * power);
			}
		}
	}

	/** Swings a caught creature round the devil's eye: along the whirl, drawn to its ring, and held off the ground a little. */
	private static void whirl(LivingEntity t, Vec3 eye, double reach, int tick) {
		Vec3 out = new Vec3(t.getX() - eye.x, 0, t.getZ() - eye.z);
		if (out.lengthSqr() < 1.0E-4) {
			out = ElementFx.flatDir(tick * 0.7);
		}
		double distance = out.length();
		Vec3 radial = out.normalize();
		Vec3 along = new Vec3(-radial.z, 0, radial.x);
		double ring = Math.max(0.8, reach * 0.55);
		Vec3 wanted = along.scale(0.32).add(radial.scale((ring - distance) * 0.25)).add(0, t.getY() < eye.y + 0.6 ? 0.28 : 0.04, 0);
		Effects.push(t, wanted.subtract(t.getDeltaMovement()));
	}

	/** The devil blows out: whoever it still holds is flung high and away, and it scatters in a last gust. */
	private static void devilEnd(Cast cast, Devil devil, Set<LivingEntity> caught, double reach) {
		ServerLevel level = cast.level;
		SignatureVfx.devilEnd(level, devil.at, reach);
		for (LivingEntity t : caught) {
			if (!onHand(cast, t) || Spirits.isBoss(t) || horizontal(t.position(), devil.at) > reach + 2.5) {
				continue;
			}
			Vec3 away = Effects.horizontal(t.position().subtract(devil.at), cast.caster.getLookAngle());
			Vec3 v = t.getDeltaMovement();
			Effects.push(t, new Vec3(v.x, Math.max(0, v.y), v.z).add(away.scale(0.45)).add(0, SignatureRules.DEVIL_FLING, 0).subtract(v));
			SignatureVfx.devilFling(level, t);
		}
	}

	/** Whether this caster already has a dust devil within 2 blocks of {@code at}: if so it keeps going, never past three times its length. */
	private static boolean renewed(Cast cast, Vec3 at, int ticks) {
		long now = cast.level.getGameTime();
		// One whose loop was lost is forgotten soon after its time.
		DEVILS.removeIf(d -> now > d.cap + 100);
		UUID caster = cast.caster.getUUID();
		for (Devil d : DEVILS) {
			if (d.caster.equals(caster) && d.level == cast.level && d.at.distanceToSqr(at) <= 4.0) {
				d.until = Math.min(d.cap, Math.max(d.until, now + ticks));
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ Malison (Hex and Resonance)

	/**
	 * Malison: 3 damage, then a curse for {@code ticks}: the caster's spells hit it 25% harder and it's shadowed (a
	 * Hex, see {@link Effects#hex}); and, for the first few of a crowd ({@code passes}), if it dies cursed the curse
	 * passes on (see {@link SignatureWards#curse}).
	 */
	private static void malison(Cast cast, LivingEntity t, double power, int ticks, double reach, boolean passes) {
		// The damage first, so the curse doesn't sharpen its own blow.
		Effects.hurt(cast, t, magic(cast), SignatureRules.MALISON_DAMAGE * power);
		Effects.hex(cast, t, ticks, Effects.HEX_BONUS, false);
		SignatureVfx.malison(cast.level, t);
		if (passes) {
			SignatureWards.curse(cast, t, ticks, reach);
		}
	}

	// ------------------------------------------------------------------ Avalanche (Coldsnap and Stalactite)

	/**
	 * Avalanche: snow and ice crash down round where it lands (on the first target, or the point): 6 frost damage to
	 * every enemy within 3 blocks, half again on a bare head, Slowness III for 3 seconds, frost on its skin and
	 * brittle for Shatter. It leaves drifts of snow for 10 seconds, on open ground the caster may build on; they go
	 * however they go, drop nothing, and are written down so a crash can't leave them (see {@link TemporaryBlocks}).
	 */
	private static void avalanche(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double power, double duration, double radius) {
		ServerLevel level = cast.level;
		Vec3 centre = hit.self() ? cast.caster.position()
			: CastEngine.ground(level, (harmed.isEmpty() ? hit.point() : harmed.getFirst().position()).add(0, 0.5, 0));
		double r = SignatureRules.AVALANCHE_RADIUS * radius;
		SignatureVfx.avalanche(level, centre, r);
		DamageSource cold = level.damageSources().source(DamageTypes.FREEZE, cast.caster);
		for (LivingEntity t : take(cast, around(cast, centre, r), new HashSet<>(hit.entities()))) {
			boolean bare = t.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
			SignatureVfx.avalancheBury(level, t, bare);
			Effects.hurt(cast, t, cold, SignatureRules.avalancheDamage(bare) * power);
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Effects.ticks(SignatureRules.AVALANCHE_SLOW_SECONDS, duration), 2, false, true), cast.caster);
			Reactions.mark(t, Reactions.Mark.FROZEN, 40);
			chill(t, 60);
		}
		drifts(cast, centre, r, Effects.ticks(SignatureRules.AVALANCHE_DRIFT_SECONDS, duration));
	}

	/** Lays up to 8 drifts of snow on open ground round {@code centre}, each gone after {@code ticks}. */
	private static void drifts(Cast cast, Vec3 centre, double radius, int ticks) {
		if (!Casters.mayBuild(cast.caster)) {
			return;
		}
		ServerLevel level = cast.level;
		long due = level.getGameTime() + ticks;
		int laid = 0;
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < 16 && laid < SignatureRules.AVALANCHE_DRIFTS; i++) {
			// A loose spiral out from the middle, so drifts lie all over where it fell.
			double a = phase + i * 2.4;
			double d = radius * Math.sqrt((i + 0.5) / 16.0);
			Vec3 foot = CastEngine.ground(level, centre.add(Math.cos(a) * d, 1.5, Math.sin(a) * d));
			BlockPos pos = BlockPos.containing(foot.x, foot.y + 0.05, foot.z);
			if (!level.getBlockState(pos).isAir() || !DRIFT.canSurvive(level, pos) || !mayEdit(cast, pos)) {
				continue;
			}
			level.setBlockAndUpdate(pos, DRIFT);
			TemporaryBlocks.put(level, pos, DRIFT, Blocks.AIR.defaultBlockState(), due);
			GlobalPos lying = GlobalPos.of(level.dimension(), pos.immutable());
			DRIFTS.put(lying, due);
			laid++;
			Scheduler.later(ticks, () -> melt(level, lying));
		}
	}

	/** A drift's time is up: it melts, if it's still there (out of loaded ground, it goes as its chunk loads). */
	private static void melt(ServerLevel level, GlobalPos lying) {
		DRIFTS.remove(lying);
		BlockPos pos = lying.pos();
		if (level.isLoaded(pos)) {
			if (level.getBlockState(pos).is(Blocks.SNOW)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
			TemporaryBlocks.remove(level, pos);
		}
	}

	/** Every drift still lying melts before the world is saved. */
	private static void meltDrifts(MinecraftServer server) {
		for (GlobalPos lying : new ArrayList<>(DRIFTS.keySet())) {
			ServerLevel level = server.getLevel(lying.dimension());
			if (level != null) {
				melt(level, lying);
			}
		}
		DRIFTS.clear();
	}

	// ------------------------------------------------------------------ helpers

	private static List<LivingEntity> first(List<LivingEntity> targets) {
		return first(targets, MAX_TARGETS);
	}

	private static List<LivingEntity> first(List<LivingEntity> targets, int n) {
		return targets.size() <= n ? targets : targets.subList(0, n);
	}

	static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	static boolean onHand(Cast cast, LivingEntity t) {
		return t.isAlive() && !t.isRemoved() && t.level() == cast.level;
	}

	/** The enemies of the caster within {@code radius} of {@code point} (by the middle of the body), nearest first, {@code cap} at most. */
	static List<LivingEntity> nearest(Cast cast, Vec3 point, double radius, int cap) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(point) <= radius + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(point)));
		return out.size() <= cap ? out : new ArrayList<>(out.subList(0, cap));
	}

	/** The enemies round a point on the ground: within {@code radius} of the air a block above it. */
	private static List<LivingEntity> around(Cast cast, Vec3 ground, double radius) {
		return nearest(cast, ground.add(0, 1, 0), radius, MAX_IN_AREA);
	}

	/** The enemy nearest {@code from} within {@code range}, other than {@code not}, or null (for a parasite leaping from its dead host). */
	private static LivingEntity nearestOther(Cast cast, Vec3 from, double range, LivingEntity not) {
		for (LivingEntity t : nearest(cast, from, range, MAX_IN_AREA)) {
			if (t != not && t.isAlive()) {
				return t;
			}
		}
		return null;
	}

	/**
	 * The creatures of {@code found} this cast may touch: those its shape already hit ({@code touched}) always, the
	 * rest only while the cast's creature budget lasts. Everyone it grants is added to {@code touched}, so they're
	 * counted once however often the effect comes back to them.
	 */
	static List<LivingEntity> take(Cast cast, List<LivingEntity> found, Collection<Entity> touched) {
		List<LivingEntity> out = new ArrayList<>();
		List<LivingEntity> fresh = new ArrayList<>();
		for (LivingEntity t : found) {
			(touched.contains(t) ? out : fresh).add(t);
		}
		int granted = cast.takeEntities(fresh.size());
		out.addAll(fresh.subList(0, granted));
		touched.addAll(fresh.subList(0, granted));
		return out;
	}

	/** The first creature the spell hit that the caster may harm (and whose Shield didn't stop it), or null. */
	private static LivingEntity firstEnemy(Cast cast, Cast.Hit hit) {
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living && e != cast.caster && living.isAlive() && Targets.canHarm(cast.caster, e) && !Shields.blocked(cast, living)) {
				return living;
			}
		}
		return null;
	}

	/** Somewhere safe to set the caster down at {@code target}, stepping back along {@code dir} a little at a time, as Blink does; or null. */
	private static Vec3 landing(Cast cast, Vec3 target, Vec3 dir) {
		Vec3 back = dir.lengthSqr() > 1.0E-4 ? dir.normalize().scale(-0.6) : Vec3.ZERO;
		for (int attempt = 0; attempt < 6; attempt++) {
			Vec3 spot = CastEngine.ground(cast.level, target.add(back.scale(1 + attempt * 0.5)).add(0, attempt % 2 == 0 ? 0 : 1, 0));
			if (Effects.safeSpot(cast.level, cast.caster, spot)) {
				return spot;
			}
		}
		return null;
	}

	/** Frost on the skin, well short of frozen solid: it creeps back on for {@code ticks} more. */
	private static void chill(LivingEntity t, int ticks) {
		t.setTicksFrozen(Math.max(t.getTicksFrozen(), Math.min(t.getTicksRequiredToFreeze() - 1, t.getTicksFrozen() + ticks)));
	}

	private static boolean fits(ServerLevel level, Entity entity, Vec3 feet) {
		return level.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(feet));
	}

	private static void teleport(Entity entity, ServerLevel level, Vec3 to) {
		entity.teleportTo(level, to.x, to.y, to.z, Set.<Relative>of(), entity.getYRot(), entity.getXRot(), false);
		entity.resetFallDistance();
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Changing blocks: never for monsters, never where the caster couldn't build, and within the cast's block budget. */
	private static boolean mayEdit(Cast cast, BlockPos pos) {
		return Casters.mayBuild(cast.caster) && !Techniques.isRampart(cast.level, pos) && Casters.mayEdit(cast.caster, cast.level, pos)
			&& cast.takeBlock();
	}

	/**
	 * Runs {@code step} every {@code every} ticks (the first at once, then one waiting at a time) for {@code ticks},
	 * while the cast lasts, then {@code end}. What a step deals after the first is lingering damage, which a Shield
	 * can block but not parry; {@code end} is the effect arriving (a burst), unless it says otherwise.
	 */
	private static void steps(Cast cast, int every, int ticks, IntConsumer step, Runnable end) {
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			int at = tick[0];
			if (at >= ticks) {
				end.run();
				return;
			}
			if (at == 0) {
				step.accept(at);
			} else {
				Effects.lingering(() -> step.accept(at));
			}
			tick[0] += every;
			Scheduler.later(every, next[0]);
		});
		Scheduler.later(1, next[0]);
	}

	private static String key(String rune, LivingEntity t, Cast cast) {
		return rune + ":" + t.getUUID() + ":" + cast.caster.getUUID();
	}

	/** Starts (or restarts) a lingering part; the token stays current until a newer one replaces it. */
	private static Object claim(String key) {
		Object token = new Object();
		RUNNING.put(key, token);
		return token;
	}

	private static boolean current(String key, Object token) {
		return RUNNING.get(key) == token;
	}

	private static void release(String key, Object token) {
		RUNNING.remove(key, token);
	}
}
