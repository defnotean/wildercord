package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The batch 4 effects that need more than a line or two: slashes and delayed impacts,
 * bindings, bombs, black flames, the Rampart's temporary wall, and the movement tricks.
 * Protections and time magic that react to incoming damage live in {@link Wards}.
 */
final class Techniques {
	private Techniques() {}

	private static final Identifier WEIGH_ID = Identifier.fromNamespaceAndPath("wildercord", "weigh");
	private static final Map<UUID, Long> WEIGHED = new HashMap<>();
	private static final Map<UUID, Long> BLACKFLAME = new HashMap<>();
	private static final Map<UUID, Integer> BIRDS = new HashMap<>();
	private static final int MAX_BIRDS = 2;
	/** A Thunderbird dives every this many ticks, on a spot it marked this many ticks before, for this much. */
	static final int BIRD_EVERY = 40;
	static final int BIRD_WARNING = 10;
	static final double BIRD_DAMAGE = 4.5;
	/** Rampart blocks still standing, and what they replaced. */
	private static final Map<GlobalPos, BlockState> RAMPART = new HashMap<>();
	private static final BlockState RAMPART_BLOCK = Blocks.PACKED_MUD.defaultBlockState();

	static void init() {
		// A Rampart block broken by hand crumbles away without dropping anything.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel server)) {
				return true;
			}
			BlockState replaced = RAMPART.remove(GlobalPos.of(server.dimension(), pos.immutable()));
			if (replaced == null || !state.is(RAMPART_BLOCK.getBlock())) {
				return true;
			}
			Vfx.emit(server, SpellMaterials.of("earth", 0xBAA17A, .14F), Vec3.atCenterOf(pos), 10, .35, .04);
			server.setBlockAndUpdate(pos, replaced);
			TemporaryBlocks.remove(server, pos);
			return false;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(Techniques::crumbleAll);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			WEIGHED.clear();
			BLACKFLAME.clear();
			OVERDRIVE.clear();
			OVERDRIVE_AMP.clear();
			BIRDS.clear();
			CONDEMNED.clear();
			RESTORED.clear();
		});
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	private static DamageSource strike(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.PLAYER_ATTACK, cast.caster);
	}

	private static void setMotion(LivingEntity target, Vec3 motion) {
		if (target instanceof ServerPlayer player) dev.wildercord.aura.MasterFormMovement.begin(player, 2);
		target.setDeltaMovement(motion);
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static boolean fits(ServerLevel level, Entity entity, Vec3 feet) {
		return level.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(feet));
	}

	private static void teleport(Entity entity, ServerLevel level, Vec3 to, float yRot, float xRot) {
		if (entity instanceof ServerPlayer player) dev.wildercord.aura.MasterFormMovement.begin(player, 2);
		entity.teleportTo(level, to.x, to.y, to.z, Set.<Relative>of(), yRot, xRot, false);
		entity.resetFallDistance();
	}

	// ------------------------------------------------------------------ damage

	/** Cleave: damage in proportion to the target's size. */
	static void cleave(Cast cast, LivingEntity t, double power, java.util.Set<java.util.UUID> struck, int[] budget) {
		double amount = cleaveDamage(t.getMaxHealth()) * power;
		TechniqueVfx.cleave(cast.level, t, cast.caster.getLookAngle());
		Effects.hurt(cast, t, strike(cast), amount);
		// A deep cut: bleeding for a few seconds, so wind damage sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, 60);
		// The axe swings on: enemies beside it take half (three at most).
		for (Entity e : cast.level.getEntities(t, t.getBoundingBox().inflate(CLEAVE_SWEEP), e -> Targets.canHarm(cast.caster, e))) {
			if (budget[0] <= 0) {
				break;
			}
			LivingEntity other = (LivingEntity) e;
			// Whoever the spell struck itself is cut in its own right, never again as a bystander.
			if (struck.contains(other.getUUID())
					|| other.getBoundingBox().getCenter().distanceTo(t.getBoundingBox().getCenter()) > CLEAVE_SWEEP + other.getBbWidth() / 2) {
				continue;
			}
			budget[0]--;
			struck.add(other.getUUID());
			Effects.hurt(cast, other, strike(cast), cleaveDamage(other.getMaxHealth()) * power * 0.5);
			Reactions.mark(other, Reactions.Mark.BLEEDING, 60);
			FireBloodVfx.cleaveSweep(cast.level, t, other);
		}
	}

	/** Cleave: how far beside its target the axe swings on. */
	static final double CLEAVE_SWEEP = 2.5;

	/** Cleave's damage before power: 6 plus 10% of the target's max health (20 more at most). */
	static double cleaveDamage(double maxHealth) {
		return FireBloodRules.cleaveDamage(maxHealth);
	}

	/** Whether {@code t} is looking toward {@code who} (within about 100 degrees of them). */
	static boolean facing(LivingEntity t, LivingEntity who) {
		Vec3 to = who.position().subtract(t.position());
		Vec3 flat = new Vec3(to.x, 0, to.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return true;
		}
		Vec3 look = t.getViewVector(1.0F);
		return new Vec3(look.x, 0, look.z).normalize().dot(flat.normalize()) > -0.2;
	}

	/** Dismantle: three slashes a tenth of a second apart, through armour; the last is a backstab. */
	static void dismantle(Cast cast, LivingEntity t, double power) {
		FireBloodVfx.dismantle(cast.level, t, 0, false);
		Effects.hurt(cast, t, magic(cast), 3 * power);
		// Cut three times over: bleeding until a little after the last, so wind damage sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, 64);
		for (int i = 1; i < 3; i++) {
			int slash = i;
			Scheduler.later(i * 2, () -> {
				// Not after a player who stepped through a portal meanwhile.
				if (cast.alive() && t.isAlive() && t.level() == cast.level) {
					// The last slash is a backstab, twice as deep, if its bearer isn't facing the caster.
					boolean back = slash == 2 && !facing(t, cast.caster);
					FireBloodVfx.dismantle(cast.level, t, slash, back);
					Effects.hurt(cast, t, magic(cast), 3 * power * (back ? 2.0 : 1.0));
				}
			});
		}
	}

	/** Blackspark: one hit in four lands true, for 2.5x damage and a moment in the zone. */
	static void blackspark(Cast cast, LivingEntity t, double power) {
		boolean spark = cast.level.getRandom().nextFloat() < 0.25F;
		TechniqueVfx.blackspark(cast.level, t, spark);
		Effects.hurt(cast, t, magic(cast), 8 * power * (spark ? 2.5 : 1.0));
		if (spark) {
			cast.caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0, false, true));
			cast.caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, false, true));
			Reactions.callout(cast, "blackspark", 0xD2283C);
			Residues.reaction(cast, "blood", t);
			// The spark arcs on to the next enemy near it.
			LivingEntity next = null;
			double best = Double.MAX_VALUE;
			for (net.minecraft.world.entity.Entity e : cast.level.getEntities(t, t.getBoundingBox().inflate(4.0), e2 -> Targets.canHarm(cast.caster, e2))) {
				double d = e.distanceToSqr(t);
				if (d < best) {
					best = d;
					next = (LivingEntity) e;
				}
			}
			if (next != null) {
				Effects.hurt(cast, next, magic(cast), 8 * power);
			}
		}
	}

	/** Aftershock: a hit now, and a second impact half a second later. */
	static void aftershock(Cast cast, LivingEntity t, double power) {
		TechniqueVfx.aftershock(cast.level, t, false);
		Effects.hurt(cast, t, strike(cast), 5 * power);
		// The ground remembers the blow: half a second later the same spot is struck again, hitting whoever stands
		// there (the target if it stayed, its neighbours too). Step aside and it misses.
		Vec3 spot = t.position();
		// The spot is marked as the ground gathers itself, two ticks before it is struck.
		Scheduler.later(6, () -> StormEarthFx.mark(cast.level, spot, ElementFx.EARTH.primary(), 1.8, 4));
		Scheduler.later(10, Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			TechniqueVfx.aftershockSpot(cast.level, spot);
			for (Entity e : cast.level.getEntities((Entity) null, new AABB(spot, spot).inflate(1.5, 1.5, 1.5), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity other = (LivingEntity) e;
				if (other.position().distanceTo(spot) <= 1.5 + other.getBbWidth() / 2) {
					Effects.lingering(() -> Effects.hurt(cast, other, strike(cast), 5 * power));
					Effects.push(other, new Vec3(0, 0.35, 0));
				}
			}
		}));
	}

	/** Resonance: marks the target; every other marked enemy nearby feels half the hit. */
	/** The most enemies one Resonance hit rings. */
	public static final int RESONANCE_LINKS = 4;

	static void resonance(Cast cast, LivingEntity t, double power, int markTicks) {
		double amount = 5 * power;
		List<LivingEntity> linked = new ArrayList<>();
		for (Entity e : cast.level.getEntities(t, t.getBoundingBox().inflate(16.0),
				e -> Targets.canHarm(cast.caster, e) && Reactions.has(e, Reactions.Mark.RESONANT))) {
			// An enemy rings once per cast, however many pulses or hits follow, so a crowd is linear, not quadratic.
			if (linked.size() < RESONANCE_LINKS && cast.once("resonance:" + e.getUUID())) {
				linked.add((LivingEntity) e);
			}
		}
		TechniqueVfx.resonance(cast.level, t, linked);
		Effects.hurt(cast, t, magic(cast), amount);
		Reactions.mark(t, Reactions.Mark.RESONANT, markTicks);
		for (LivingEntity other : linked) {
			Effects.hurt(cast, other, magic(cast), amount * 0.5);
		}
	}

	/**
	 * Ripple: sunlight damage, doubled on undead, and you heal a quarter of what it really took (at most 6). Half a second
	 * later the ripple runs out from the target: 3 to every other enemy within 2.5 blocks, and you heal 1 for each (3 at most).
	 */
	static void ripple(Cast cast, LivingEntity t, double power) {
		double amount = 6 * power * (t.isInvertedHealAndHarm() ? 2.0 : 1.0) * Reactions.storm(cast, t);
		TechniqueVfx.ripple(cast.level, t);
		float before = t.getHealth();
		Effects.hurt(cast, t, magic(cast), amount);
		float taken = Math.max(0.0F, before - Math.max(0.0F, t.getHealth()));
		if (taken > 0 && cast.caster.isAlive()) {
			cast.caster.heal(Math.min(6.0F, taken / 4.0F));
			Vfx.stream(cast.level, t.getBoundingBox().getCenter(), cast.caster.getBoundingBox().getCenter(), Vfx.themeOf(0xFFD050), 3);
		}
		Vec3 centre = t.getBoundingBox().getCenter();
		Scheduler.later(8, Effects.carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			TechniqueVfx.ripple(cast.level, t);
			int healed = 0;
			for (Entity e : cast.level.getEntities(t, new net.minecraft.world.phys.AABB(centre, centre).inflate(2.5),
					e -> e instanceof LivingEntity && Targets.canHarm(cast.caster, e))) {
				LivingEntity other = (LivingEntity) e;
				if (other.getBoundingBox().getCenter().distanceTo(centre) > 2.5 + other.getBbWidth() / 2) {
					continue;
				}
				float was = other.getHealth();
				Effects.lingering(() -> Effects.hurt(cast, other, magic(cast), 3 * power));
				if (healed < 3 && was > other.getHealth() && cast.caster.isAlive()) {
					cast.caster.heal(1.0F);
					healed++;
				}
			}
		}));
	}

	/** Primer: the target becomes a bomb that goes off two seconds later. */
	static void primer(Cast cast, LivingEntity t, double radius, double power, java.util.Map<java.util.UUID, Double> landed) {
		Vec3[] last = {t.getBoundingBox().getCenter()};
		boolean[] gone = {false};
		FireBloodVfx.fuse(cast.level, t);
		Runnable blast = () -> {
			if (gone[0]) {
				return;
			}
			gone[0] = true;
			if (!cast.alive()) {
				return;
			}
			Vec3 at = t.isAlive() && t.level() == cast.level ? t.getBoundingBox().getCenter() : last[0];
			Effects.explode(cast, at, radius, power * 10.0 / 12.0, landed);
		};
		for (int i = 2; i < 40; i += 2) {
			int tick = i;
			Scheduler.later(i, () -> {
				if (gone[0]) {
					return;
				}
				if (t.isAlive() && t.level() == cast.level) {
					last[0] = t.getBoundingBox().getCenter();
					if (tick % 6 == 0) {
						FireBloodVfx.fuseTick(cast.level, t, tick, 40);
					}
				} else if (t.isDeadOrDying() && t.level() == cast.level) {
					// Killed with the fuse lit: it goes off where it fell, at once.
					blast.run();
				}
			});
		}
		Scheduler.later(40, blast);
	}

	/**
	 * Blackflame: black fire that water can't reach, one hit a second. If the target dies while
	 * burning, the flames leap to the nearest enemy with the time they had left.
	 */
	static void blackflame(Cast cast, LivingEntity t, double power, int seconds, boolean spread) {
		long until = cast.level.getGameTime() + seconds * 20L;
		// Shadowed while the black flames burn: life damage on it sets off Blight.
		Reactions.mark(t, Reactions.Mark.SHADOWED, seconds * 20);
		Long burning = BLACKFLAME.get(t.getUUID());
		if (burning != null && burning >= cast.level.getGameTime()) {
			BLACKFLAME.put(t.getUUID(), Math.max(burning, until));
			TechniqueVfx.blackflame(cast.level, t);
			return;
		}
		BLACKFLAME.put(t.getUUID(), until);
		TechniqueVfx.blackflame(cast.level, t);
		dev.wildercord.cast.feel.Feels.sound(cast.level, t.position(), "void_black_ignite", 1.0F, 1.0F);
		boolean[] spent = {false};
		burnTick(cast, t, power, spread, spent);
	}

	private static void burnTick(Cast cast, LivingEntity t, double power, boolean spread, boolean[] spent) {
		Scheduler.later(20, () -> {
			if (!cast.alive()) {
				BLACKFLAME.remove(t.getUUID());
				return;
			}
			Long until = BLACKFLAME.get(t.getUUID());
			long now = cast.level.getGameTime();
			if (!t.isAlive() || t.level() != cast.level) {
				BLACKFLAME.remove(t.getUUID());
				int left = until == null ? 0 : (int) ((until - now) / 20);
				// Only a death spreads it: not a creature that just went (unloaded, despawned, or through a portal).
				if (spread && !spent[0] && left >= 1 && t.isDeadOrDying()) {
					spent[0] = true;
					LivingEntity next = ShapeRunners.nearestEnemy(cast, t.getBoundingBox().getCenter(), 6.0, null);
					if (next != null) {
						Vfx.stream(cast.level, t.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), Vfx.theme("void"), 8);
						blackflame(cast, next, power, Math.max(2, left), true);
					}
				}
				return;
			}
			if (until == null || now > until) {
				BLACKFLAME.remove(t.getUUID());
				return;
			}
			TechniqueVfx.blackflame(cast.level, t);
			Effects.hurt(cast, t, magic(cast), 3 * power);
			burnTick(cast, t, power, spread, spent);
		});
	}

	/**
	 * Hollow: heavy damage to what was hit, and everything around it is dragged into the gap.
	 * Up to three centres per application, so a crowd hit by a Burst still reads clearly.
	 */
	static void hollow(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power) {
		// One centre: the creature the spell struck (nearest the point it landed); everything else near it is only dragged in.
		List<LivingEntity> centres = new ArrayList<>();
		if (!harmed.isEmpty()) {
			LivingEntity best = harmed.getFirst();
			for (LivingEntity h : harmed) {
				if (h.distanceToSqr(hit.point()) < best.distanceToSqr(hit.point())) {
					best = h;
				}
			}
			centres.add(best);
		}
		List<Vec3> points = new ArrayList<>();
		centres.forEach(t -> points.add(t.getBoundingBox().getCenter()));
		if (points.isEmpty()) {
			points.add(hit.point());
		}
		for (int i = 0; i < points.size(); i++) {
			Vec3 c = points.get(i);
			LivingEntity direct = i < centres.size() ? centres.get(i) : null;
			// A repeating shape (Zone, Pulse, Echo) does not erase the same creature again within one cast.
			if (direct != null ? !VoidTime.once(cast, "hollow", direct, 100) : !VoidTime.onceAt(cast, "hollow", c, 100)) {
				continue;
			}
			TechniqueVfx.hollow(cast.level, c, radius);
			if (direct != null && !Spirits.isBoss(direct) && !VoidTime.anchored(direct)) {
				// Erased for the wind-up: it is gone from the fight until the cut lands.
				direct.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 8, 0, false, false));
				Spirits.hold(direct, 8);
			}
			Scheduler.later(6, () -> {
				if (!cast.alive()) {
					return;
				}
				if (direct != null && direct.isAlive()) {
					Effects.hurt(cast, direct, cast.level.damageSources().sonicBoom(cast.caster), 20 * power);
				}
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(c, c).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity other = (LivingEntity) e;
					double d = other.getBoundingBox().getCenter().distanceTo(c);
					if (other == direct || d > radius) {
						continue;
					}
					Vec3 towards = c.subtract(other.position());
					Effects.push(other, towards.normalize().scale(Math.min(1.6, 0.4 + d * 0.25)));
					Reactions.mark(other, Reactions.Mark.PULLED);
					Effects.hurt(cast, other, magic(cast), 8 * power);
				}
			});
		}
	}

	/** Repel: a violent outward blast from the point. Enemies just pulled in set off Collapse. */
	static void repel(Cast cast, Cast.Hit hit, double radius, double power) {
		Vec3 c = hit.self() ? cast.caster.position().add(0, 1, 0) : hit.point();
		TechniqueVfx.repel(cast.level, c, radius);
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(c, c).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			double d = t.getBoundingBox().getCenter().distanceTo(c);
			if (d > radius + t.getBbWidth() / 2) {
				continue;
			}
			double react = Reactions.collapse(cast, t);
			Effects.hurt(cast, t, magic(cast), 4 * power * react);
			Vec3 away = Effects.horizontal(t.position().subtract(c), cast.caster.getLookAngle());
			double falloff = 1.0 - 0.4 * Math.min(1.0, d / Math.max(0.5, radius));
			Statuses.windPush(t, away.scale(2.4 * power * falloff).add(0, 0.5, 0));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	// ------------------------------------------------------------------ control

	/** Decree's verdict: the caster's next {@link #DECREE_HITS} spell hits on a held creature within 4 s deal 40% more; a decree on this many or more costs nothing. */
	public static final int DECREE_HITS = 2;
	public static final int DECREE_CONDEMNED_TICKS = 80;
	public static final double DECREE_BONUS = 1.4;
	public static final int DECREE_FREE_AT = 3;

	private static final class Condemned {
		final java.util.UUID by;
		final long until;
		int hits;

		Condemned(java.util.UUID by, long until, int hits) {
			this.by = by;
			this.until = until;
			this.hits = hits;
		}
	}

	private static final java.util.Map<java.util.UUID, Condemned> CONDEMNED = new java.util.concurrent.ConcurrentHashMap<>();

	/** The damage multiplier for a hit on a creature its caster decreed (1 when it isn't): called for every spell hit. */
	static double condemned(Cast cast, LivingEntity target) {
		if (CONDEMNED.isEmpty()) {
			return 1.0;
		}
		Condemned c = CONDEMNED.get(target.getUUID());
		if (c == null) {
			return 1.0;
		}
		if (c.until < cast.level.getGameTime() || c.hits <= 0) {
			CONDEMNED.remove(target.getUUID(), c);
			return 1.0;
		}
		if (!c.by.equals(cast.caster.getUUID())) {
			return 1.0;
		}
		c.hits--;
		return DECREE_BONUS;
	}

	static void clearCondemned() {
		CONDEMNED.clear();
	}

	/** Decree: everything hit is stunned; speaking it costs the caster 2 health, once per cast. */
	static void decree(Cast cast, List<LivingEntity> harmed, int ticks) {
		if (harmed.isEmpty()) {
			return;
		}
		LivingEntity caster = cast.caster;
		if (cast.once("decree")) {
			TechniqueVfx.decreeSpoken(cast.level, caster);
			// Speaking to a crowd is free: the cost is for a single command.
			if (!Casters.creative(caster) && harmed.size() < DECREE_FREE_AT) {
				caster.setHealth(Math.max(1.0F, caster.getHealth() - 2.0F));
				Fx.sound(cast.level, caster.position(), SoundEvents.PLAYER_HURT, 0.6F, 0.8F);
			}
		}
		long until = cast.level.getGameTime() + DECREE_CONDEMNED_TICKS;
		for (LivingEntity t : harmed) {
			CONDEMNED.put(t.getUUID(), new Condemned(caster.getUUID(), until, DECREE_HITS));
			Spirits.hold(t, ticks);
			if (t instanceof Mob mob) {
				mob.setTarget(null);
			}
			TechniqueVfx.decree(cast.level, caster.getEyePosition(), t);
		}
	}

	/** Weigh: triple gravity, slow legs and weak jumps for a while; fliers are dragged down. */
	static void weigh(Cast cast, LivingEntity t, int ticks) {
		modifier(t, Attributes.GRAVITY, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(t, Attributes.MOVEMENT_SPEED, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifier(t, Attributes.JUMP_STRENGTH, -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		long until = cast.level.getGameTime() + ticks;
		WEIGHED.merge(t.getUUID(), until, Math::max);
		// Too heavy for the wind to hold: a soaring player comes down.
		Soar.ground(t);
		TechniqueVfx.weigh(cast.level, t, true);
		for (int i = 5; i < ticks; i += 5) {
			Scheduler.later(i, () -> {
				if (t.isAlive() && WEIGHED.containsKey(t.getUUID())) {
					if (!t.onGround()) {
						Effects.push(t, new Vec3(0, -0.35, 0));
					}
					TechniqueVfx.weigh(cast.level, t, false);
				}
			});
		}
		Scheduler.later(ticks, () -> {
			Long end = WEIGHED.get(t.getUUID());
			if (end != null && end <= t.level().getGameTime()) {
				WEIGHED.remove(t.getUUID());
				for (var attribute : List.of(Attributes.GRAVITY, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
					AttributeInstance instance = t.getAttribute(attribute);
					if (instance != null) {
						instance.removeModifier(WEIGH_ID);
					}
				}
			}
		});
	}

	private static void modifier(LivingEntity t, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = t.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(WEIGH_ID, amount, operation));
		}
	}

	/** Shackle: the target is chained to where it stood, and yanked back if it strays. */
	static void shackle(Cast cast, LivingEntity t, int ticks) {
		if (Spirits.isBoss(t)) {
			// Bosses can't be held in place, only slowed.
			t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, ticks, 1, false, true));
			TechniqueVfx.chain(cast.level, t.position(), t, true);
			return;
		}
		Vec3 anchor = t.position();
		TechniqueVfx.chain(cast.level, anchor, t, true);
		for (int i = 2; i <= ticks; i += 2) {
			int tick = i;
			Scheduler.later(i, () -> {
				if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
					return;
				}
				double d = t.position().distanceTo(anchor);
				if (d > 5.0) {
					teleport(t, cast.level, anchor, t.getYRot(), t.getXRot());
				} else if (d > 2.0) {
					Vec3 back = anchor.subtract(t.position()).normalize().scale(Math.min(1.2, 0.3 + (d - 2.0) * 0.4));
					setMotion(t, new Vec3(back.x, Math.max(t.getDeltaMovement().y, back.y), back.z));
					// The chain bites: 2 for a yank, at most once a second.
					if (tick % 20 == 0) {
						Effects.lingering(() -> Effects.hurt(cast, t, magic(cast), 2));
					}
					if (tick % 6 == 0) {
						Fx.sound(cast.level, t.position(), SoundEvents.CHAIN_HIT, 0.7F, 0.8F);
					}
				}
				if (tick % 4 == 0) {
					TechniqueVfx.chain(cast.level, anchor, t, false);
				}
			});
		}
	}

	/** Bubble: floats the target helplessly, then pops for damage and leaves it soaked. */
	static void bubble(Cast cast, LivingEntity t, int ticks, double power) {
		// One bubble at a time on a creature: a Zone's next pulse or a Linger doesn't stack pops.
		if (!Statuses.claim(t, "bubble", ticks + 10)) {
			return;
		}
		double lift = 2.5 / Math.max(1, ticks / 2);
		if (t instanceof Mob) {
			Spirits.hold(t, ticks);
		} else {
			t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0, false, false));
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 3, false, false));
		}
		Feels.sound(cast.level, t.position(), "frost_bubble_in", 0.9F, 1.0F);
		for (int i = 0; i < ticks; i += 2) {
			int tick = i;
			Scheduler.later(i + 1, () -> {
				if (!t.isAlive() || t.level() != cast.level) {
					return;
				}
				if (t instanceof Mob && !Spirits.isBoss(t) && fits(cast.level, t, t.position().add(0, lift, 0))) {
					teleport(t, cast.level, t.position().add(0, lift, 0), t.getYRot(), t.getXRot());
				}
				if (tick % 4 == 0 || tick > ticks - 12) {
					TechniqueVfx.bubble(cast.level, t, tick > ticks - 12);
				}
			});
		}
		Scheduler.later(ticks, () -> {
			if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
				return;
			}
			t.removeEffect(MobEffects.LEVITATION);
			// The bubble's own hold lets go now by itself (Spirits.hold); forcing a thaw would also end a longer
			// Freeze on the same creature.
			TechniqueVfx.bubblePop(cast.level, t);
			Effects.hurt(cast, t, magic(cast), 4 * power);
			Reactions.mark(t, Reactions.Mark.SOAKED);
		});
	}

	// ------------------------------------------------------------------ support

	/** Overdrive: strength, speed and haste, paid for with a little health every two seconds. */
	static void overdrive(Cast cast, LivingEntity t, int ticks, int amplify) {
		OVERDRIVE_AMP.put(t.getUUID(), amplify);
		t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, painStrength(t, amplify), false, true));
		t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1, false, true));
		t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, 1, false, true));
		FireBloodVfx.overdrive(cast.level, t, true, painStrength(t, amplify) - 1 - amplify);
		// One drain per target, however often Overdrive is renewed (a passive renews it every 2 s).
		long until = cast.level.getGameTime() + ticks;
		Long running = OVERDRIVE.put(t.getUUID(), Math.max(until, OVERDRIVE.getOrDefault(t.getUUID(), 0L)));
		if (running == null) {
			overdriveDrain(cast.level, t);
		}
	}

	private static final Map<UUID, Long> OVERDRIVE = new HashMap<>();
	private static final Map<UUID, Integer> OVERDRIVE_AMP = new HashMap<>();

	/** Overdrive's Strength level (an amplifier): II, one more below half health and another below a quarter (pain fuels it), IV at most. */
	static int painStrength(LivingEntity t, int amplify) {
		return FireBloodRules.painStrength(t.getHealth(), t.getMaxHealth(), amplify);
	}

	private static void overdriveDrain(ServerLevel level, LivingEntity t) {
		Scheduler.later(40, () -> {
			Long until = OVERDRIVE.get(t.getUUID());
			if (until == null || !t.isAlive() || level.getGameTime() > until) {
				OVERDRIVE.remove(t.getUUID());
				OVERDRIVE_AMP.remove(t.getUUID());
				if (until != null && t.isAlive()) {
					FireBloodVfx.overdriveEnd(level, t);
				}
				return;
			}
			if (t.getHealth() > 2.0F && !(t instanceof ServerPlayer player && player.isCreative())) {
				t.setHealth(t.getHealth() - 1.0F);
				FireBloodVfx.overdrive(level, t, false, painStrength(t, OVERDRIVE_AMP.getOrDefault(t.getUUID(), 0)) - 1 - OVERDRIVE_AMP.getOrDefault(t.getUUID(), 0));
			}
			// The weaker it leaves them, the harder they hit.
			int left = (int) Math.max(1, until - level.getGameTime());
			t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, left, painStrength(t, OVERDRIVE_AMP.getOrDefault(t.getUUID(), 0)), false, true));
			overdriveDrain(level, t);
		});
	}

	/** Restore: heals 4, mends 8% of each item, and an item can be mended by it once a minute (so Zone, Linger and Pulse can't repair without limit). */
	public static final double RESTORE_HEAL = 4.0;
	public static final double RESTORE_MEND = 0.08;
	public static final int RESTORE_REST = 1200;
	private static final java.util.Map<String, Long> RESTORED = new java.util.concurrent.ConcurrentHashMap<>();

	/** Restore: heals, puts out fire and mends worn and held gear a little. */
	static void restore(Cast cast, LivingEntity t, double power) {
		var restoredBody=LifeOwnerEvents.before(t);
		t.heal((float)(RESTORE_HEAL*power));t.clearFire();
		LifeOwnerEvents.changed(cast,"restore",t,restoredBody,null,LifeOwnerEvents.Moment.APPLY);
		boolean mended = false;
		long now = cast.level.getGameTime();
		if (RESTORED.size() > 512) {
			RESTORED.values().removeIf(at -> now - at >= RESTORE_REST || at > now);
		}
		for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			ItemStack stack = t.getItemBySlot(slot);
			String key = t.getUUID() + ":" + slot.getName();
			Long last = RESTORED.get(key);
			if (last != null && last <= now && now - last < RESTORE_REST) {
				continue;
			}
			if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0) {
				RESTORED.put(key, now);
				int fix = (int) Math.ceil(stack.getMaxDamage() * RESTORE_MEND * power);
				int priorDamage=stack.getDamageValue();stack.setDamageValue(Math.max(0,priorDamage-fix));
				LifeOwnerEvents.repair(cast,t,slot,stack,priorDamage);
				mended = true;
			}
		}
	}

	/** Accelerate: time runs faster for the target. */
	static void accelerate(Cast cast, LivingEntity t, int ticks, int amplify) {
		t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, Math.min(4, 1 + amplify), false, true));
		// Haste III is the mark of hurried time: it fills your charge faster and speeds your Bolts (see VoidTime.hurried).
		t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, Math.min(4, 2 + amplify), false, true));
		t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, 1, false, true));
		t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
		TechniqueVfx.accelerate(cast.level, t);
		// Time slowing back to its own pace: a ring closes on the waist and a tick.
		TimeFx.endingLater(cast.level, t, ticks, TimeFx.gold(), "time_tick", 0.6F);
	}

	// ------------------------------------------------------------------ movement

	/**
	 * The first creature in a hit that a movement trick may act on: not you, not a boss, and friend or fair game
	 * (an enemy whose Shield stopped this spell is neither: the spell ended at its circles).
	 */
	private static LivingEntity partner(Cast cast, Cast.Hit hit) {
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living && e != cast.caster && living.isAlive() && !Spirits.isBoss(e)
					&& (Targets.canHarm(cast.caster, e) && !Shields.blocked(cast, living) || Targets.isAlly(cast.caster, e))) {
				return living;
			}
		}
		return null;
	}

	/** Swap: you and the target trade places. */
	static void swap(Cast cast, Cast.Hit hit) {
		swap(cast, hit, true);
	}

	/** Swap with or without its own arcane show (Warp draws its own). */
	static void swap(Cast cast, Cast.Hit hit, boolean show) {
		LivingEntity caster = cast.caster;
		LivingEntity target = partner(cast, hit);
		if (target == null || VoidTime.anchored(target)) {
			return;
		}
		Vec3 a = caster.position();
		Vec3 b = target.position();
		if (!fits(cast.level, caster, b) || !fits(cast.level, target, a)) {
			Casters.tell(caster, Component.translatable("message.wildercord.swap_blocked"));
			return;
		}
		teleport(caster, cast.level, b, caster.getYRot(), caster.getXRot());
		teleport(target, cast.level, a, target.getYRot(), target.getXRot());
		if (show) {
			TechniqueVfx.swap(cast.level, a, b);
		}
	}

	/** Zipper: steps you through the wall you're facing (up to 6 blocks thick). */
	static void zipper(Cast cast) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 eye = caster.getEyePosition();
		Vec3 dir = caster.getLookAngle();
		double wall = -1;
		for (double d = 0.3; d <= 2.5; d += 0.1) {
			BlockPos p = BlockPos.containing(eye.add(dir.scale(d)));
			BlockState state = level.getBlockState(p);
			if (!state.getCollisionShape(level, p).isEmpty()) {
				if (state.getDestroySpeed(level, p) < 0) {
					Casters.tell(caster, Component.translatable("message.wildercord.zipper_unbreakable"));
					return;
				}
				wall = d;
				break;
			}
		}
		if (wall < 0) {
			Casters.tell(caster, Component.translatable("message.wildercord.zipper_no_wall"));
			return;
		}
		Vec3 entry = eye.add(dir.scale(wall));
		double eyeHeight = caster.getEyeHeight();
		for (double d = wall + 0.5; d <= wall + 6.5; d += 0.25) {
			Vec3 p = eye.add(dir.scale(d));
			BlockPos bp = BlockPos.containing(p);
			BlockState state = level.getBlockState(bp);
			if (!state.getCollisionShape(level, bp).isEmpty() && state.getDestroySpeed(level, bp) < 0) {
				Casters.tell(caster, Component.translatable("message.wildercord.zipper_unbreakable"));
				return;
			}
			for (double drop : new double[] {eyeHeight, eyeHeight * 0.5, 0.1}) {
				Vec3 feet = p.subtract(0, drop, 0);
				if (feet.y < level.getMinY() + 1) {
					continue;
				}
				// Out the far side: never into lava or fire (or onto it, a short drop below), nor past the world border.
				AABB landing = caster.getDimensions(caster.getPose()).makeBoundingBox(feet);
				if (fits(level, caster, feet) && !Effects.scorching(level, landing.expandTowards(0, -3, 0))
						&& level.getWorldBorder().isWithinBounds(feet.x, feet.z)) {
					if (!mayPass(caster, level, BlockPos.containing(entry), BlockPos.containing(feet))) {
						Casters.tell(caster, Component.translatable("message.wildercord.zipper_claimed"));
						return;
					}
					teleport(caster, level, feet, caster.getYRot(), caster.getXRot());
					TechniqueVfx.zipper(level, entry, feet.add(0, eyeHeight * 0.6, 0), dir);
					return;
				}
			}
		}
		Casters.tell(caster, Component.translatable("message.wildercord.zipper_thick"));
	}

	/** Whether the wall a Zipper opens, and the ground it opens onto, are ones the caster may go through (spawn protection, claims). */
	private static boolean mayPass(LivingEntity caster, ServerLevel level, BlockPos wall, BlockPos out) {
		if (!(caster instanceof ServerPlayer player)) {
			return true;
		}
		for (BlockPos pos : new BlockPos[] {wall, out}) {
			if (!level.mayInteract(player, pos) || !net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
					.beforeBlockBreak(level, player, pos, level.getBlockState(pos), level.getBlockEntity(pos))) {
				return false;
			}
		}
		return true;
	}

	/** Shadowstep: you reappear right behind the target, facing its back. */
	static void shadowstep(Cast cast, Cast.Hit hit) {
		LivingEntity caster = cast.caster;
		LivingEntity target = partner(cast, hit);
		if (target == null) {
			return;
		}
		Vec3 facing = Effects.horizontal(target.getLookAngle(), caster.getLookAngle());
		Vec3 side = new Vec3(-facing.z, 0, facing.x);
		double back = target.getBbWidth() / 2 + 0.9;
		Vec3 base = target.position();
		for (Vec3 offset : List.of(facing.scale(-back), side.scale(back), side.scale(-back))) {
			Vec3 spot = CastEngine.ground(cast.level, base.add(offset).add(0, 0.5, 0));
			if (Math.abs(spot.y - base.y) > 2.5 || !fits(cast.level, caster, spot)) {
				continue;
			}
			Vec3 from = caster.position();
			Vec3 look = target.position().subtract(spot);
			float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
			teleport(caster, cast.level, spot, yaw, 15.0F);
			caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 0, false, false));
			// You are behind it: the next blow you land on it within 3 s is half again as hard.
			VoidTime.backstab(caster, target);
			TechniqueVfx.shadowstep(cast.level, from, spot);
			return;
		}
	}

	/** Time Skip: you vanish, reappear up to 8 blocks ahead, and nearby monsters lose you. */
	static void timeSkip(Cast cast) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 dir = Effects.horizontal(caster.getLookAngle(), caster.getLookAngle());
		Vec3 start = caster.position();
		List<Vec3> path = new ArrayList<>();
		for (double d = 0.5; d <= 8.0; d += 0.5) {
			Vec3 spot = path.isEmpty() ? start.add(dir.scale(d)) : path.getLast().add(dir.scale(0.5));
			if (!fits(level, caster, spot)) {
				if (fits(level, caster, spot.add(0, 1.05, 0))) {
					spot = spot.add(0, 1.05, 0);
				} else {
					break;
				}
			}
			path.add(spot);
		}
		// The farthest spot along the way with safe ground under it: never into lava, never over a drop or the void.
		Vec3 end = null;
		for (int i = path.size() - 1; i >= 0 && end == null; i--) {
			Vec3 grounded = CastEngine.ground(level, path.get(i).add(0, 0.2, 0));
			if (Effects.safeSpot(level, caster, grounded)) {
				end = grounded;
			}
		}
		if (end == null) {
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.time_skip_nowhere", "Nowhere safe ahead to skip to"));
			return;
		}
		teleport(caster, level, end, caster.getYRot(), caster.getXRot());
		caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
		// Half a second out of time: nothing can hurt you as you come out (not again for 5 s). A dodge, not a guard.
		if (VoidTime.skipReady(caster)) {
			caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, DefenceCaps.DODGE_TICKS, 4, false, true));
		}
		for (Entity e : level.getEntities(caster, caster.getBoundingBox().inflate(16.0), e -> e instanceof Mob)) {
			Mob mob = (Mob) e;
			if (mob.getTarget() == caster) {
				mob.setTarget(null);
			}
		}
		TechniqueVfx.timeSkip(level, start, end);
	}

	// ------------------------------------------------------------------ world

	/** Whether {@code pos} is part of a Rampart's temporary wall. */
	static boolean isRampart(ServerLevel level, BlockPos pos) {
		return !RAMPART.isEmpty() && RAMPART.containsKey(GlobalPos.of(level.dimension(), pos));
	}

	/** Rampart: a temporary earth wall across your aim at the point. */
	static void rampart(Cast cast, Cast.Hit hit, double radiusScale, int ticks) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 base = hit.block() != null && hit.face() != null
			? Vec3.atBottomCenterOf(hit.block().relative(hit.face()))
			: CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		Vec3 look = Effects.horizontal(caster.getLookAngle(), caster.getLookAngle());
		Vec3 side = new Vec3(-look.z, 0, look.x);
		int half = Math.max(1, (int) Math.round(2 * radiusScale));
		Set<BlockPos> columns = new LinkedHashSet<>();
		for (int i = -half; i <= half; i++) {
			Vec3 c = base.add(side.scale(i));
			columns.add(BlockPos.containing(c.x, base.y + 0.01, c.z));
		}
		dev.wildercord.cast.feel.Feels.sound(level, base, "earth_stomp", 0.8F, 0.84F);
		long due = level.getGameTime() + ticks;
		for (int row = 0; row < 3; row++) {
			int r = row;
			Scheduler.later(1 + row * 2, () -> {
				if (!cast.alive()) {
					return;
				}
				for (BlockPos column : columns) {
					BlockPos p = column.above(r);
					BlockState state = level.getBlockState(p);
					// Never over a Light spell's light: put back when the wall crumbled, it would stay lit for good.
					if (!state.canBeReplaced() || state.is(Blocks.LIGHT)
							|| !level.getEntities((Entity) null, new AABB(p), e -> e instanceof LivingEntity).isEmpty()) {
						continue;
					}
					if (!Casters.mayEdit(caster, level, p) || !cast.takeBlock()) {
						continue;
					}
					RAMPART.put(GlobalPos.of(level.dimension(), p.immutable()), state);
					TemporaryBlocks.put(level, p, RAMPART_BLOCK, state, due);
					level.setBlockAndUpdate(p, RAMPART_BLOCK);
					Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, RAMPART_BLOCK), Vec3.atCenterOf(p), 4, 0.3, 0.05);
				}
				dev.wildercord.cast.feel.Feels.sound(level, base.add(0, r, 0), "earth_grind", 0.8F, new float[] {1.0F, 1.122F, 1.26F}[r]);
			});
		}
		Scheduler.later(ticks, () -> {
			if (level.isLoaded(BlockPos.containing(base))) {
				dev.wildercord.cast.feel.Feels.sound(level, base.add(0, 1, 0), "earth_crack", 0.9F, 0.84F);
			}
			for (BlockPos column : columns) {
				for (int r = 0; r < 3; r++) {
					crumble(level, column.above(r));
				}
			}
		});
	}

	/** A Rampart block crumbles. One whose chunk isn't loaded now is put back as it loads ({@link TemporaryBlocks}), never loaded just for this. */
	private static void crumble(ServerLevel level, BlockPos pos) {
		BlockState replaced = RAMPART.remove(GlobalPos.of(level.dimension(), pos.immutable()));
		if (replaced == null || !level.isLoaded(pos)) {
			return;
		}
		if (level.getBlockState(pos).is(RAMPART_BLOCK.getBlock())) {
			// Dust, not a vanilla break each: fifteen break sounds at once was a wall of noise (one crack plays for the wall).
			Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, RAMPART_BLOCK),
				Vec3.atCenterOf(pos), 6, 0.3, 0.05);
			level.setBlockAndUpdate(pos, replaced);
		}
		TemporaryBlocks.remove(level, pos);
	}

	/**
	 * On shutdown every Rampart still standing in loaded ground crumbles, so none can outlive its
	 * spell; the rest are saved in {@link TemporaryBlocks}, and crumble as their chunks load.
	 */
	private static void crumbleAll(MinecraftServer server) {
		for (Map.Entry<GlobalPos, BlockState> entry : new ArrayList<>(RAMPART.entrySet())) {
			ServerLevel level = server.getLevel(entry.getKey().dimension());
			BlockPos pos = entry.getKey().pos();
			if (level == null || !level.isLoaded(pos)) {
				continue;
			}
			if (level.getBlockState(pos).is(RAMPART_BLOCK.getBlock())) {
				level.setBlockAndUpdate(pos, entry.getValue());
			}
			TemporaryBlocks.remove(level, pos);
		}
		RAMPART.clear();
	}

	// ------------------------------------------------------------------ summons

	/** Thunderbird: a storm bird circles overhead and strikes the nearest enemy every 1.5 seconds. */
	static void thunderbird(Cast cast, double power, int ticks) {
		LivingEntity caster = cast.caster;
		UUID id = caster.getUUID();
		if (BIRDS.getOrDefault(id, 0) >= MAX_BIRDS) {
			Casters.tell(caster, Component.translatable("message.wildercord.too_many_birds", MAX_BIRDS));
			return;
		}
		BIRDS.merge(id, 1, Integer::sum);
		double phase = cast.level.getRandom().nextDouble() * Math.PI * 2;
		Vec3[] bird = {caster.position().add(0, 3.3, 0)};
		Vec3[] marked = {null};
		dev.wildercord.cast.feel.Feels.sound(cast.level, caster.position(), "storm_cry", 1.0F, 1.0F);
		for (int t = 0; t <= ticks; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				double a = phase + tick * 0.12;
				bird[0] = caster.position().add(Math.cos(a) * 2.6, 2.7 + Math.sin(tick * 0.25) * 0.15, Math.sin(a) * 2.6);
				TechniqueVfx.thunderbird(cast.level, bird[0], a, tick);
				if (tick == 0) {
					return;
				}
				if (tick % BIRD_EVERY == BIRD_EVERY - BIRD_WARNING) {
					// The bird picks the enemy you last hit (else the nearest) and marks the spot it will dive on.
					LivingEntity pick = ShapeRunners.nearestEnemy(cast, caster.position().add(0, 1, 0), 12.0, caster.getLastHurtMob());
					marked[0] = pick == null ? null : pick.position();
					if (marked[0] != null) {
						StormEarthFx.mark(cast.level, marked[0], ElementFx.STORM.primary(), 1.5, BIRD_WARNING);
						dev.wildercord.cast.feel.Feels.sound(cast.level, marked[0], "storm_dive", 0.8F, 1.0F);
					}
					return;
				}
				if (tick % BIRD_EVERY != 0 || marked[0] == null) {
					return;
				}
				// The dive lands on the marked spot: an enemy that stayed is struck, one that stepped out is not.
				Vec3 spot = marked[0];
				marked[0] = null;
				TechniqueVfx.birdStrike(cast.level, bird[0], spot.add(0, 1, 0));
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(spot, spot).inflate(1.5, 2.0, 1.5), e -> Targets.canHarm(caster, e))) {
					LivingEntity target = (LivingEntity) e;
					if (target.position().distanceTo(spot) > 1.5 + target.getBbWidth() / 2) {
						continue;
					}
					Effects.hurt(cast, target, cast.level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), BIRD_DAMAGE * power * Reactions.storm(cast, target));
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2, false, false));
				}
			});
		}
		Scheduler.later(ticks + 3, () -> {
			BIRDS.computeIfPresent(id, (k, n) -> n <= 1 ? null : n - 1);
			if (caster.level() == cast.level) {
				TechniqueVfx.birdFade(cast.level, bird[0]);
			}
		});
	}
}
