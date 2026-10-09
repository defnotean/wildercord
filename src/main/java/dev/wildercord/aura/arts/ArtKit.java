package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraCombat;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.MomentumRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import dev.wildercord.monster.MonsterMagic;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What every art does with the world, in one place, so each method's arts read as what they mean: who an art may touch, where its
 * foes are (an arc, a cone, a line, a circle), its strikes (projected aura with the PvP cap, an impact on each foe), and what it
 * does to them (lift, throw, pull, set alight, chill, freeze, shake, shock) and to its swordsman (a dash, a blink, a leap), every
 * one keeping the rules of {@link ArtRules}: a boss is only ever slowed and hurt, a player is held, thrown and burned less, and
 * nobody the swordsman may not harm is touched.
 */
public final class ArtKit {
	private ArtKit() {}

	static final Vec3 UP = new Vec3(0, 1, 0);

	// ------------------------------------------------------------------ who

	/** Whether an art of {@code player}'s may harm {@code entity}: alive, not them, a foe by the mod's rules, and the game's team rule too. */
	public static boolean harmable(ServerPlayer player, Entity entity) {
		var counter = dev.wildercord.aura.MastersArts.earnedCounter(player);
		return harmableWithoutAim(player, entity)
			&& (dev.wildercord.aura.MastersArts.committedAim(player) == null || counter != null && counter.originalGeometry()
				|| player.hasLineOfSight(entity));
	}

	/** Original instant-art permission, with no implicit LOS introduced by an authored pose. */
	public static boolean harmableWithoutAim(ServerPlayer player, Entity entity) {
		return !ArtFields.blocksRetiredHarm(player, entity)
			&& entity instanceof LivingEntity living && living.isAlive() && entity != player && !entity.isSpectator() && Targets.canHarm(player, entity)
			&& (!(entity instanceof Player other) || player.canHarmPlayer(other));
	}

	/** Whether an art of {@code player}'s may help {@code entity} (themselves, their pets, their team). */
	public static boolean helpable(ServerPlayer player, Entity entity) {
		return Targets.canHelp(player, entity) && !entity.isSpectator();
	}

	public static boolean boss(Entity entity) {
		return Spirits.isBoss(entity);
	}

	// ------------------------------------------------------------------ where

	/** The way the swordsman faces, level. */
	public static Vec3 flat(Entity player) {
		Vec3 committed = dev.wildercord.aura.MastersArts.committedAim(player);
		if (committed != null) return committed;
		Vec3 look = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() >= 1.0E-4) return flat.normalize();
		// Looking vertically still has a meaningful horizontal facing; never snap a committed cut to world south.
		double yaw = Math.toRadians(player.getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	/** A style's locked three-dimensional view, or the ordinary view outside a committed active frame. */
	public static Vec3 view(Entity player) {
		Vec3 committed = dev.wildercord.aura.MastersArts.committedView(player);
		return committed == null ? player.getViewVector(1.0F) : committed;
	}

	/** Square to {@code flat}, toward the swordsman's right hand. */
	public static Vec3 right(Vec3 flat) {
		return new Vec3(-flat.z, 0, flat.x);
	}

	/** Toward the side the swordsman holds their blade (their main arm). */
	public static Vec3 bladeSide(ServerPlayer player, Vec3 flat) {
		return player.getMainArm() == HumanoidArm.RIGHT ? right(flat) : right(flat).scale(-1);
	}

	/** Where the blade is, near enough. */
	public static Vec3 hand(ServerPlayer player) {
		Vec3 look = flat(player);
		return player.position().add(0, 1.05, 0).add(look.scale(0.45)).add(bladeSide(player, look).scale(0.35));
	}

	/** A colour lifted toward white. */
	public static int hot(int color, double t) {
		return mix(color, 0xFFFFFF, t);
	}

	public static int mix(int a, int b, double t) {
		int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}

	/** The top of the ground at or under {@code at} (searching {@code up} above and {@code down} below), or null with nothing to stand on. */
	public static Vec3 floor(ServerLevel level, Vec3 at, double up, double down) {
		BlockPos start = BlockPos.containing(at.x, at.y + up, at.z);
		int steps = (int) Math.ceil(up + down) + 1;
		for (int i = 0; i <= steps; i++) {
			BlockPos pos = start.below(i);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
				double top = level.getBlockState(pos).getCollisionShape(level, pos).max(net.minecraft.core.Direction.Axis.Y);
				return new Vec3(at.x, pos.getY() + top, at.z);
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ finding foes

	/** The foes within {@code radius} of {@code centre} (level), from {@code below} under it to {@code above} over it, nearest first. */
	public static List<LivingEntity> around(ServerPlayer player, Vec3 centre, double radius, double below, double above, int max) {
		return around(player, centre, radius, below, above, max, false);
	}

	/** The same area query, with owner LOS applied before the target cap; used only by Red Rain's immediate burst. */
	public static List<LivingEntity> aroundVisible(ServerPlayer player, Vec3 centre, double radius, double below, double above, int max) {
		return around(player, centre, radius, below, above, max, true);
	}

	private static List<LivingEntity> around(ServerPlayer player, Vec3 centre, double radius, double below, double above, int max, boolean visible) {
		List<LivingEntity> out = new ArrayList<>();
		AABB box = new AABB(centre.x - radius - 1, centre.y - below, centre.z - radius - 1, centre.x + radius + 1, centre.y + above, centre.z + radius + 1);
		for (Entity e : player.level().getEntities(player, box, e -> harmable(player, e) && (!visible || player.hasLineOfSight(e)))) {
			double dx = e.getX() - centre.x;
			double dz = e.getZ() - centre.z;
			double reach = radius + e.getBbWidth() / 2;
			if (dx * dx + dz * dz <= reach * reach) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(centre)));
		return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
	}

	/** The foes in an arc in front ({@code reach} long, {@code degrees} wide), {@code first} first if it's still standing near; nearest first. */
	public static List<LivingEntity> arc(ServerPlayer player, LivingEntity first, double reach, double degrees, int max) {
		Vec3 look = flat(player);
		Vec3 at = player.position();
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(reach + 1, 1.8, reach + 1), e -> harmable(player, e))) {
			Vec3 to = e.position().subtract(at);
			if (Math.abs(to.y) <= 2.2 && ArtRules.inCone(to.x, to.z, look.x, look.z, reach + e.getBbWidth() / 2, degrees)) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		if (first != null && first.isAlive() && harmable(player, first) && first.distanceToSqr(player) < (reach + 2.5) * (reach + 2.5)) {
			out.remove(first);
			out.addFirst(first);
		}
		return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
	}

	/** The foes in an arc from {@code at} facing {@code look} (level: an afterimage's cut where its swordsman stood), nearest first. */
	public static List<LivingEntity> arcFrom(ServerPlayer player, Vec3 at, Vec3 look, double reach, double degrees, int max) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : player.level().getEntities(player, new AABB(at, at).inflate(reach + 1, 2.2, reach + 1), e -> harmable(player, e))) {
			Vec3 to = e.position().subtract(at);
			if (Math.abs(to.y) <= 2.2 && ArtRules.inCone(to.x, to.z, look.x, look.z, reach + e.getBbWidth() / 2, degrees)) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
		return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
	}

	/** The foes along a line from {@code from} down {@code dir} (level), {@code length} long and {@code half} wide each side, nearest the start first. */
	public static List<LivingEntity> line(ServerPlayer player, Vec3 from, Vec3 dir, double length, double half, double height, int max) {
		Vec3 flat = new Vec3(dir.x, 0, dir.z);
		flat = flat.lengthSqr() < 1.0E-4 ? flat(player) : flat.normalize();
		Vec3 to = from.add(flat.scale(length));
		AABB box = new AABB(from, to).inflate(half + 1, height, half + 1);
		List<LivingEntity> out = new ArrayList<>();
		Map<LivingEntity, Double> along = new HashMap<>();
		for (Entity e : player.level().getEntities(player, box, e -> harmable(player, e))) {
			Vec3 rel = e.position().subtract(from);
			double[] aa = ArtRules.alongAcross(rel.x, rel.z, flat.x, flat.z);
			if (aa[0] >= -0.6 && aa[0] <= length + 0.6 && aa[1] <= half + e.getBbWidth() / 2 && Math.abs(rel.y) <= height) {
				out.add((LivingEntity) e);
				along.put((LivingEntity) e, aa[0]);
			}
		}
		out.sort(Comparator.comparingDouble(along::get));
		return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
	}

	/** The foes along a line in 3D (it follows the pitch), from {@code from} to {@code to}, within {@code half} of it. */
	public static List<LivingEntity> beam(ServerPlayer player, Vec3 from, Vec3 to, double half, int max) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		if (length < 1.0E-3) {
			return new ArrayList<>();
		}
		Vec3 u = d.scale(1 / length);
		List<LivingEntity> out = new ArrayList<>();
		Map<LivingEntity, Double> along = new HashMap<>();
		for (Entity e : player.level().getEntities(player, new AABB(from, to).inflate(half + 1), e -> harmable(player, e))) {
			Vec3 c = e.getBoundingBox().getCenter();
			Vec3 rel = c.subtract(from);
			double t = rel.dot(u);
			if (t < -0.5 || t > length + 0.5) {
				continue;
			}
			double off = rel.subtract(u.scale(t)).length();
			if (off <= half + Math.max(e.getBbWidth(), e.getBbHeight() * 0.5) * 0.6) {
				out.add((LivingEntity) e);
				along.put((LivingEntity) e, t);
			}
		}
		out.sort(Comparator.comparingDouble(along::get));
		return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
	}

	/**
	 * The foe an art fastens on: the one the string's last swing struck if it still stands near, or else the nearest in an arc in
	 * front; null with nobody there.
	 */
	public static LivingEntity primary(ServerPlayer player, AuraApi.StringContext context, double reach, double degrees) {
		LivingEntity struck = context.struck();
		if (struck != null && struck.isAlive() && harmable(player, struck) && struck.distanceToSqr(player) < (reach + 2.5) * (reach + 2.5)) {
			return struck;
		}
		List<LivingEntity> arc = arc(player, null, reach, degrees, 1);
		return arc.isEmpty() ? null : arc.getFirst();
	}

	/**
	 * The foe a counter answers: the one the counter struck, or the one whose blow the perfect guard just caught (if it's near
	 * enough to answer), or the nearest in front.
	 */
	public static LivingEntity attacker(ServerPlayer player, AuraApi.StringContext context, double reach) {
		LivingEntity struck = context.struck();
		if (struck != null && struck.isAlive() && harmable(player, struck)) {
			return struck;
		}
		AuraGuard.Caught caught = AuraGuard.caught(player);
		if (caught != null && caught.attacker() != null && caught.attacker().isAlive() && harmable(player, caught.attacker())
				&& caught.attacker().distanceToSqr(player) <= (reach + 2) * (reach + 2)) {
			return caught.attacker();
		}
		List<LivingEntity> arc = arc(player, null, reach, 120, 1);
		return arc.isEmpty() ? null : arc.getFirst();
	}

	/** The nearest foe to {@code from} within {@code reach}, leaving out {@code except}; null with none. */
	public static LivingEntity nearest(ServerPlayer player, Vec3 from, double reach, Collection<? extends Entity> except) {
		LivingEntity best = null;
		double bestD = reach * reach;
		for (Entity e : player.level().getEntities(player, new AABB(from, from).inflate(reach), e -> harmable(player, e) && !except.contains(e))) {
			double d = e.getBoundingBox().getCenter().distanceToSqr(from);
			if (d < bestD) {
				bestD = d;
				best = (LivingEntity) e;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ strikes

	/** The swordsman's weapon damage ({@code W}: at least 1). */
	public static double weapon(ServerPlayer player) {
		return Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	/** Every art's damage times the server's {@code aura.damage_scale} and {@code aura.art_damage}. */
	public static double scale() {
		return Config.get().aura().damageScale() * Config.get().aura().strings().artDamage();
	}

	/** An art's strikes, as it lands them: see {@link Hits}. */
	public static Hits hits(ServerPlayer player, AuraFx.Art fx) {
		return new Hits(player, fx);
	}

	/**
	 * One performance's strikes. Each lands as projected aura (the slash's rules) at the weapon's damage times a factor, with an
	 * impact on the foe; each foe answers the art once (its experience, aura marks and the trials come from the first strike on
	 * it), and another player takes no more than {@link ArtRules#PVP_ART_CAP} from all of the art's strikes together.
	 *
	 * <p>Momentum and stance ride every strike: they land harder at each tier of the swordsman's momentum as the art began
	 * ({@link MomentumRules#strength}), wear the foe's stance as an art does (more for an art that quakes or holds; another player's
	 * at most {@link StanceRules#PVP_ART_CAP} of it over the whole art), and each foe the art hurts builds momentum
	 * ({@code Momentum.artLanded}: the first by the art's slot, a few more a little).</p>
	 */
	public static final class Hits {
		private final ServerPlayer player;
		private final AuraFx.Art fx;
		private final Map<UUID, Double> pvp = new HashMap<>();
		private final Map<UUID, Double> pvpStance = new HashMap<>();
		private final Set<UUID> answered = new HashSet<>();
		private final Set<UUID> hurt = new HashSet<>();
		private final net.minecraft.world.item.ItemStack startingBlade;
		/** The art these strikes belong to (null for strikes outside one), how hard momentum makes them, and how they wear stance. */
		private final AuraApi.StringArt art;
		private final double strength;
		private double stanceWeight;
		private double scaling = scale();

		Hits(ServerPlayer player, AuraFx.Art fx) {
			this.player = player;
			this.startingBlade = player.getMainHandItem();
			this.fx = fx;
			this.art = SwordStrings.performing();
			this.strength = Momentum.on(player) ? MomentumRules.strength(Momentum.tier(player)) : 1.0;
			ArtRules.Art numbers = art == null ? null : ArtRules.find(art.id());
			this.stanceWeight = numbers == null ? 1.0 : StanceRules.artWeight(numbers.is(ArtRules.Kind.QUAKE), numbers.is(ArtRules.Kind.HOLD)
				|| numbers.is(ArtRules.Kind.FREEZE) || numbers.is(ArtRules.Kind.ROOT) || numbers.is(ArtRules.Kind.STILL) || numbers.is(ArtRules.Kind.SHOCK));
		}

		/** How hard momentum makes these strikes (1 at none). */
		public double strength() {
			return strength;
		}

		/**
		 * These strikes wear a stance at {@code weight} against an art's ordinary 1 (a technique's: its stroke, its intent and its method
		 * together, see {@code aura.TechniqueRules#stance}), within the same caps on another player.
		 */
		public Hits wearing(double weight) {
			this.stanceWeight = Math.max(1.0, weight);
			return this;
		}

		/** What these strikes' weapon damage is scaled by (every art's {@link #scale}; a technique's own setting instead, see {@link #scaled}). */
		public double scaling() {
			return scaling;
		}

		/** These strikes scaled by {@code scale} instead of the arts' (a technique's: {@code damage_scale} and {@code technique_damage}). */
		public Hits scaled(double scale) {
			this.scaling = Math.max(0, scale);
			return this;
		}

		public ServerPlayer player() {
			return player;
		}

		public AuraFx.Art fx() {
			return fx;
		}

		/** A heavy strike of the weapon's damage times {@code factor}. Returns what it took. */
		public float strike(LivingEntity foe, double factor) {
			return strike(foe, factor, AuraFxRules.Weight.HEAVY);
		}

		public float strike(LivingEntity foe, double factor, AuraFxRules.Weight weight) {
			return raw(foe, weapon(player) * factor * scaling, weight);
		}

		/** A strike of {@code damage} (already scaled), landing with {@code weight} ({@code null} for no impact). */
		public float raw(LivingEntity foe, double damage, AuraFxRules.Weight weight) {
			var counter = dev.wildercord.aura.ArtHitScope.boundary(player);
			if (counter != null && !counter.permits(foe)) return 0;
			if (foe == null || !foe.isAlive() || damage <= 0 || !harmable(player, foe)) {
				return 0;
			}
			boolean answer = answered.add(foe.getUUID());
			double cap = Double.MAX_VALUE;
			if (foe instanceof Player) {
				cap = ArtRules.pvpLeft(Double.MAX_VALUE, pvp.getOrDefault(foe.getUUID(), 0.0));
				if (cap <= 0) {
					return 0;
				}
			}
			// A bonded blade's Mountainfeller: harder on a boss or a Runebound foe (never a player).
			float taken = AuraCombat.artStrike(player, foe, damage * strength * dev.wildercord.aura.BladeTraits.art(player, foe), cap, answer);
			double dealt = AuraCombat.lastAmount();
			if (foe instanceof Player) {
				pvp.merge(foe.getUUID(), dealt, Double::sum);
			}
			// Damage callbacks can retire this field. Keep the resolved receipt/cap, but no later stance or hit side effects.
			if (ArtFields.blocksRetiredHarm(player, foe) || counter != null && !counter.afterDamage(foe)) return taken;
			if (taken > 0 && !answer && (!foe.isAlive() || foe.isDeadOrDying())) {
				dev.wildercord.aura.BondedBlades.artFelled(player, foe);
			}
			if (taken > 0) {
				boolean first = hurt.add(foe.getUUID());
				if (weight != null) {
					fx.impact(foe, weight);
				}
				if (art != null && foe instanceof Player) {
					// A swordsman struck by an art may answer it with one of their own in the same breath: the two lock (a clash).
					dev.wildercord.aura.Clashes.artStruck(player, foe, taken, dealt);
				}
				// The strike wears its foe's stance as an art does (another player's held to half of it over the whole art).
				double stanceCap = foe instanceof Player ? Math.max(0, StanceRules.PLAYER_POOL * StanceRules.PVP_ART_CAP
					- pvpStance.getOrDefault(foe.getUUID(), 0.0)) : Double.MAX_VALUE;
				double wore = Stance.art(player, foe, dealt, stanceWeight, stanceCap);
				if (foe instanceof Player && wore > 0) {
					pvpStance.merge(foe.getUUID(), wore, Double::sum);
				}
				if (counter != null && !counter.afterDamage(foe)) return taken;
				if (first) {
					Momentum.artLanded(player, art, hurt.size(), foe);
					if (counter != null && !counter.afterDamage(foe)) return taken;
					// The bonded blade in hand gathers resonance from an art that lands (and remembers which).
					dev.wildercord.aura.BondedBlades.artLanded(player, art, hurt.size(), foe);
					if (counter != null && !counter.afterDamage(foe)) return taken;
					if (hurt.size() == 1 && player.getMainHandItem() == startingBlade)
						dev.wildercord.aura.RuneEtchings.wake(player, foe, taken);
				}
			}
			return taken;
		}

		/** Whether this art has hurt {@code foe} yet. */
		public boolean hurt(LivingEntity foe) {
			return foe != null && hurt.contains(foe.getUUID());
		}

		/** How many foes it has hurt. */
		public int count() {
			return hurt.size();
		}
	}

	// ------------------------------------------------------------------ moving them

	/** Throws {@code foe} upward by {@code up} (never a boss; a player at most {@link ArtRules#PVP_THROW}) and marks it airborne. */
	public static void lift(LivingEntity foe, double up, int airborneTicks) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(foe)) return;
		double power = ArtRules.thrown(up, foe instanceof Player, boss(foe));
		if (power <= 0 || !foe.isAlive()) {
			return;
		}
		Vec3 v = foe.getDeltaMovement();
		if (foe instanceof ServerPlayer moving) dev.wildercord.aura.MasterFormMovement.begin(moving, 2);
		foe.setDeltaMovement(v.x * 0.5, Math.max(v.y, power), v.z * 0.5);
		foe.needsSync = true;
		MonsterMagic.sync(foe);
		if (airborneTicks > 0) {
			Statuses.airborne(foe, airborneTicks);
		}
	}

	/** Throws {@code foe} away from {@code from}, {@code power} hard and {@code up} upward (held to the rules for players and bosses). */
	public static void knock(LivingEntity foe, Vec3 from, double power, double up) {
		Vec3 away = foe.position().subtract(from);
		away = new Vec3(away.x, 0, away.z);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
		shove(foe, new Vec3(away.x * power, up, away.z * power));
	}

	/** Pushes {@code foe} by {@code impulse}, its strength held to the rules for players and bosses (and its knockback resistance). */
	public static void shove(LivingEntity foe, Vec3 impulse) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(foe)) return;
		double length = impulse.length();
		double allowed = ArtRules.thrown(length, foe instanceof Player, boss(foe));
		if (allowed <= 0 || !foe.isAlive()) {
			return;
		}
		MonsterMagic.shove(foe, length > allowed ? impulse.scale(allowed / length) : impulse);
	}

	/** Draws {@code foe} toward {@code to}, {@code power} hard (never a boss; a player gently). */
	public static void pull(LivingEntity foe, Vec3 to, double power) {
		Vec3 toward = to.subtract(foe.position());
		toward = new Vec3(toward.x, 0, toward.z);
		if (toward.lengthSqr() < 0.25) {
			return;
		}
		shove(foe, toward.normalize().scale(power).add(0, 0.04, 0));
	}

	/**
	 * Draws {@code foe} in toward {@code to} at {@code speed} blocks a tick, its own sideways motion replaced (so a strike's
	 * knockback a moment before doesn't carry it away): a whirlwind's pull. Never a boss; a player only gently.
	 */
	public static void draw(LivingEntity foe, Vec3 to, double speed) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(foe)) return;
		Vec3 toward = to.subtract(foe.position());
		toward = new Vec3(toward.x, 0, toward.z);
		double length = toward.length();
		double allowed = ArtRules.thrown(speed, foe instanceof Player, boss(foe));
		if (length < 0.6 || allowed <= 0 || !foe.isAlive()) {
			return;
		}
		Vec3 v = foe.getDeltaMovement();
		Vec3 pull = toward.scale(Math.min(allowed, length * 0.5) / length);
		if (foe instanceof ServerPlayer moving) dev.wildercord.aura.MasterFormMovement.begin(moving, 2);
		foe.setDeltaMovement(pull.x, Math.max(v.y, 0.02), pull.z);
		MonsterMagic.sync(foe);
	}

	/**
	 * Takes the push out of {@code foe}'s motion: every strike carries vanilla knockback, so an art that means to keep its foes where
	 * they are (in its rain, in its echo's reach, where its well gathered them) steadies them after it strikes. A player's own motion is
	 * theirs, and left alone.
	 */
	public static void steady(LivingEntity foe) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(foe)) return;
		if (foe.isAlive() && !(foe instanceof Player)) {
			Vec3 v = foe.getDeltaMovement();
			foe.setDeltaMovement(v.x * 0.15, Math.min(v.y, 0.1), v.z * 0.15);
			MonsterMagic.sync(foe);
		}
	}

	// ------------------------------------------------------------------ what they suffer

	/** Sets {@code foe} alight for {@code ticks} (a player at most {@link ArtRules#PVP_IGNITE_TICKS}); nothing for one fire can't touch. */
	public static void ignite(LivingEntity foe, int ticks) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(foe)) return;
		if (!foe.isAlive() || foe.fireImmune() || foe.isInWaterOrRain() && foe.isInWater()) {
			return;
		}
		int t = ArtRules.ignite(ticks, foe instanceof Player);
		if (t > 0) {
			foe.igniteForTicks(Math.max(foe.getRemainingFireTicks(), t));
		}
	}

	/** Slows {@code foe} ({@code amplifier} 0 is Slowness I), and frost creeps over it to see (never enough to hurt). */
	public static void chill(ServerPlayer player, LivingEntity foe, int ticks, int amplifier) {
		if (!harmable(player, foe)) return;
		chillAdmitted(player, foe, ticks, amplifier, null);
	}

	/** A caller with its own explicit permission may preserve its original collateral selection. */
	static void chillAdmitted(ServerPlayer player, LivingEntity foe, int ticks, int amplifier, java.util.function.BooleanSupplier permission) {
		if (permission != null && !permission.getAsBoolean()) return;
		if (!foe.isAlive()) {
			return;
		}
		int amp = foe instanceof Player ? Math.min(amplifier, 1) : amplifier;
		foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, amp, false, true), player);
		if (permission != null && !permission.getAsBoolean()) return;
		int frost = Math.min(foe.getTicksRequiredToFreeze() - 1, foe.getTicksFrozen() + 60);
		foe.setTicksFrozen(Math.max(foe.getTicksFrozen(), frost));
	}

	/** A plain slow. */
	public static void slow(ServerPlayer player, LivingEntity foe, int ticks, int amplifier) {
		if (!harmable(player, foe)) return;
		if (foe.isAlive()) {
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, foe instanceof Player ? Math.min(1, amplifier) : amplifier, false, true), player);
		}
	}

	/** When each player was last held by an art (so another art's hold waits {@link ArtRules#PVP_HOLD_REST}). */
	private static final Map<UUID, Long> HELD = new HashMap<>();

	/** How long a hold may take {@code foe} now: the rules' length, and nothing for a player held by an art too lately. */
	private static int holdFor(LivingEntity foe, int ticks) {
		int t = ArtRules.hold(ticks, foe instanceof Player, boss(foe));
		if (t > 0 && foe instanceof Player) {
			long now = foe.level().getGameTime();
			Long last = HELD.get(foe.getUUID());
			if (last != null && now - last < ArtRules.PVP_HOLD_REST && now >= last) {
				return 0;
			}
			HELD.put(foe.getUUID(), now);
		}
		return t;
	}

	/**
	 * Freezes {@code foe} solid for {@code ticks} (the mod's freeze: still, iced over, and frozen for a fire spell's Shatter); a
	 * player briefly, a boss only slowed hard. Returns whether it froze.
	 */
	public static boolean freeze(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!harmable(player, foe)) return false;
		if (!foe.isAlive()) {
			return false;
		}
		if (boss(foe)) {
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 3, false, true), player);
			Reactions.mark(foe, Reactions.Mark.FROZEN, ticks);
			return false;
		}
		int t = holdFor(foe, ticks);
		if (t <= 0) {
			chill(player, foe, ticks, 1);
			return false;
		}
		Spirits.freeze(foe, t);
		return true;
	}

	/** Shakes {@code foe}'s footing: it can't act for {@code ticks} (a player is slowed to a crawl that long; a boss only slowed). */
	public static boolean hold(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!harmable(player, foe)) return false;
		return holdAdmitted(player, foe, ticks);
	}

	private static boolean holdAdmitted(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!foe.isAlive()) {
			return false;
		}
		if (boss(foe)) {
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 1, false, true), player);
			return false;
		}
		int t = holdFor(foe, ticks);
		if (t <= 0) {
			return false;
		}
		Spirits.hold(foe, t);
		return true;
	}

	/**
	 * A hold for a foe just thrown: it waits until the foe comes down (at least {@code delay} ticks, at most two seconds), since a
	 * hold at once would leave it hanging in the air (a held creature doesn't move), then stuns it where it lands.
	 */
	public static void holdLater(ServerPlayer player, LivingEntity foe, int delay, int ticks) {
		if (!harmable(player, foe)) return;
		int[] waited = {0};
		Runnable[] step = new Runnable[1];
		step[0] = () -> {
			if (!foe.isAlive() || !player.isAlive() || foe.level() != player.level() || !harmable(player, foe)) {
				return;
			}
			waited[0]++;
			if (waited[0] >= delay && (foe.onGround() || foe.isInWater()) || waited[0] >= 40) {
				hold(player, foe, ticks);
				return;
			}
			Scheduler.later(1, step[0]);
		};
		Scheduler.later(1, step[0]);
	}

	/** Same landing timing, with an independently released exact-body permission rather than the physical pose. */
	public static void holdLater(ServerPlayer player, LivingEntity foe, int delay, int ticks, java.util.function.BooleanSupplier permission) {
		if (!permission.getAsBoolean()) return;
		int[] waited = {0};
		Runnable[] step = new Runnable[1];
		step[0] = () -> {
			if (!permission.getAsBoolean()) return;
			waited[0]++;
			if (waited[0] >= delay && (foe.onGround() || foe.isInWater()) || waited[0] >= 40) {
				holdAdmitted(player, foe, ticks);
				return;
			}
			Scheduler.later(1, step[0]);
		};
		Scheduler.later(1, step[0]);
	}

	/** Lightning through {@code foe}: what it was winding up breaks, it twitches still for {@code ticks}, and it's left ionised for a storm spell. */
	public static void shock(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!harmable(player, foe)) return;
		if (!foe.isAlive()) {
			return;
		}
		Statuses.interrupt(foe);
		hold(player, foe, ticks);
		Reactions.mark(foe, Reactions.Mark.IONISED, ArtRules.IONISED_TICKS);
	}

	/**
	 * Roots {@code foe} where it stands for {@code ticks}: it can turn and strike back but not walk away (as the Root rune holds;
	 * the art draws its own roots). On another player a root is a hold, held to {@link ArtRules#PVP_HOLD_TICKS} and not again within
	 * {@link ArtRules#PVP_HOLD_REST} (a player held lately is only slowed); a boss is only slowed. Returns whether it rooted.
	 */
	public static boolean root(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!harmable(player, foe)) return false;
		if (!foe.isAlive()) {
			return false;
		}
		if (boss(foe)) {
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 1, false, true), player);
			return false;
		}
		int t = holdFor(foe, ticks);
		if (t <= 0) {
			slow(player, foe, ticks, 1);
			return false;
		}
		if (foe instanceof ServerPlayer moving) dev.wildercord.aura.MasterForms.cancel(moving);
		foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, t, 6, false, false, true), player);
		foe.setDeltaMovement(0, Math.min(0, foe.getDeltaMovement().y), 0);
		MonsterMagic.sync(foe);
		return true;
	}

	/** Whether {@code foe} is rooted now (the slow only a root or a hold gives). */
	public static boolean rooted(LivingEntity foe) {
		return MonsterMagic.rooted(foe);
	}

	/**
	 * A steady pull, one beat of it: {@code foe} carried toward {@code to} at {@code speed} blocks a tick, its own sideways motion
	 * replaced. For pulls that beat every tick or two (a well, a black sphere): another player at most {@link ArtRules#PVP_DRAG}, under
	 * a sprint, so they can always run out of it; never a boss. Within {@code stop} of the point it's let be.
	 */
	public static void drag(LivingEntity foe, Vec3 to, double speed, double stop) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(foe)) return;
		Vec3 toward = to.subtract(foe.position());
		toward = new Vec3(toward.x, 0, toward.z);
		double length = toward.length();
		double allowed = ArtRules.dragged(speed, foe instanceof Player, boss(foe));
		if (length <= stop || allowed <= 0 || !foe.isAlive()) {
			return;
		}
		Vec3 v = foe.getDeltaMovement();
		Vec3 pull = toward.scale(Math.min(allowed, (length - stop) * 0.5) / length);
		if (foe instanceof Player) {
			// A player keeps their own motion and is only leaned on: they can always walk out of it.
			if (foe instanceof ServerPlayer moving) dev.wildercord.aura.MasterFormMovement.begin(moving, 2);
			foe.setDeltaMovement(v.add(pull.scale(0.5)));
		} else {
			foe.setDeltaMovement(pull.x, Math.max(v.y, foe.onGround() ? 0.0 : v.y), pull.z);
		}
		MonsterMagic.sync(foe);
	}

	// ------------------------------------------------------------------ mending, drinking and aura given back

	/** What arts have mended each body lately: the bucket's level and when it was last filled (see {@link ArtRules#mendRoom}). */
	private static final Map<UUID, double[]> MENDED = new HashMap<>();

	/**
	 * Mends {@code target} by {@code amount} health (Verdant's mending; {@link #drink} for Crimson's), if the swordsman may help it
	 * (themselves, their pets, their team) and it's hurt: held, with every other art's mending of it, to {@link ArtRules#MEND_CAP}
	 * (the bucket {@link ArtRules#mendRoom} drains a health a second). Returns the health it took.
	 */
	public static float mend(ServerPlayer player, LivingEntity target, double amount) {
		if (target == null || !target.isAlive() || amount <= 0 || !helpable(player, target) || target.getHealth() >= target.getMaxHealth()) {
			return 0;
		}
		long now = target.level().getGameTime();
		double[] bucket = MENDED.computeIfAbsent(target.getUUID(), k -> new double[] {0, now});
		double level = ArtRules.mendLevel(bucket[0], now - (long) bucket[1]);
		double room = Math.max(0, ArtRules.MEND_CAP - level);
		float before = target.getHealth();
		target.heal((float) Math.min(amount, room));
		float took = Math.max(0, target.getHealth() - before);
		bucket[0] = level + took;
		bucket[1] = now;
		if (MENDED.size() > 256) {
			MENDED.values().removeIf(b -> ArtRules.mendLevel(b[0], now - (long) b[1]) <= 0);
		}
		return took;
	}

	/** Crimson's drink: the swordsman mended by {@code amount} (a share of what an art dealt), in the same bucket as any mending. */
	public static float drink(ServerPlayer player, double amount) {
		return mend(player, player, amount);
	}

	/** How much more arts may mend {@code target} now (for the tests). */
	public static double mendRoom(LivingEntity target) {
		double[] bucket = MENDED.get(target.getUUID());
		return bucket == null ? ArtRules.MEND_CAP : ArtRules.mendRoom(bucket[0], target.level().getGameTime() - (long) bucket[1]);
	}

	/**
	 * Gives {@code amount} aura back to the swordsman (a Starlit art), a tick later, so it never lands before the art's own price is
	 * paid (a full pool would waste it). Never past their capacity, never scaled as a gain is.
	 */
	public static void giveBack(ServerPlayer player, double amount) {
		if (amount <= 0) {
			return;
		}
		Scheduler.later(1, () -> {
			if (player.isAlive()) {
				givenBack += Aura.giveBack(player, amount);
			}
		});
	}

	/** All the aura arts have given back since the server started (for the tests). */
	private static double givenBack;

	public static double givenBack() {
		return givenBack;
	}

	// ------------------------------------------------------------------ wounds

	/**
	 * What an art drinks (Crimson's): a share of what its strikes and wounds take, held to a cap over the whole art, each drink drawn
	 * by {@code look} from the foe it came from. One per performance, shared by everything the art lands.
	 */
	public static final class Drink {
		private final ServerPlayer player;
		private final double share;
		private final double cap;
		private final java.util.function.Consumer<LivingEntity> look;
		private double drunk;

		public Drink(ServerPlayer player, double share, double cap, java.util.function.Consumer<LivingEntity> look) {
			this.player = player;
			this.share = share;
			this.cap = cap;
			this.look = look;
		}

		/** Drinks its share of {@code taken} from {@code foe}; returns the health it gave its swordsman. */
		public float from(LivingEntity foe, float taken) {
			if (taken <= 0 || drunk >= cap || !player.isAlive()) {
				return 0;
			}
			float got = drink(player, ArtRules.drink(taken, share, cap - drunk));
			drunk += got;
			if (got > 0 && look != null) {
				look.accept(foe);
			}
			return got;
		}

		/** What it has drunk so far. */
		public double drunk() {
			return drunk;
		}
	}

	/**
	 * A wound in {@code foe} that bleeds {@code times} times, every {@link ArtRules#BLEED_PERIOD} ticks, {@code factor} weapons each
	 * ({@link ArtRules#BLEED_MOVING} times as much while it's on the move, as the Bleed spell's), through the art's own strikes (so a
	 * player's cap still holds), left marked bleeding for a wind spell's Rupture. {@code drink} (or null) drinks from each bleed;
	 * {@code drip} (or null) draws it.
	 */
	public static void wound(Hits hits, LivingEntity foe, double factor, int times, Drink drink, java.util.function.Consumer<LivingEntity> drip) {
		ServerPlayer player = hits.player();
		if (foe == null || !foe.isAlive() || times <= 0 || factor <= 0 || !harmable(player, foe)) {
			return;
		}
		Reactions.mark(foe, Reactions.Mark.BLEEDING, times * ArtRules.BLEED_PERIOD + 10);
		Vec3[] last = {foe.position()};
		ServerLevel level = player.level();
		for (int i = 1; i <= times; i++) {
			Scheduler.later(i * ArtRules.BLEED_PERIOD, () -> {
				if (!foe.isAlive() || !player.isAlive() || foe.level() != level || !harmable(player, foe)) {
					return;
				}
				boolean moving = foe.position().distanceToSqr(last[0]) > 0.04;
				last[0] = foe.position();
				float taken = hits.raw(foe, weapon(player) * factor * hits.scaling() * (moving ? ArtRules.BLEED_MOVING : 1.0), null);
				if (drip != null) {
					drip.accept(foe);
				}
				if (drink != null) {
					drink.from(foe, taken);
				}
			});
		}
	}

	/**
	 * An opt-in wound owned by an already released original body. Moon's wounds outlive weapon changes and
	 * physical recovery, but never a death, disconnect or world departure. Other wound callers retain the
	 * original helper above. Each hit still evaluates the ordinary live party/team/duel/trial admission.
	 */
	public static void wound(Hits hits, LivingEntity foe, double factor, int times, Drink drink,
			java.util.function.Consumer<LivingEntity> drip, ReleasedArtOwner released) {
		ServerPlayer player = hits.player();
		ServerLevel level = released.level();
		if (!released.owns(player) || !released.valid() || foe == null || !foe.isAlive() || foe.isRemoved()
			|| foe.level() != level || times <= 0 || factor <= 0 || !harmable(player, foe)) return;
		Reactions.mark(foe, Reactions.Mark.BLEEDING, times * ArtRules.BLEED_PERIOD + 10);
		Vec3[] last = {foe.position()};
		for (int i = 1; i <= times; i++) {
			Scheduler.later(i * ArtRules.BLEED_PERIOD, () -> {
				if (!released.valid() || !foe.isAlive() || foe.isRemoved() || foe.level() != level || !harmable(player, foe)) return;
				boolean moving = foe.position().distanceToSqr(last[0]) > 0.04;
				last[0] = foe.position();
				float taken = hits.raw(foe, weapon(player) * factor * hits.scaling() * (moving ? ArtRules.BLEED_MOVING : 1.0), null);
				// Do not require survival: an admitted lethal wound still drinks its actual health loss.
				if (!released.valid() || foe.isRemoved() || foe.level() != level) return;
				if (drip != null) drip.accept(foe);
				if (!released.valid() || foe.isRemoved() || foe.level() != level) return;
				if (drink != null) drink.from(foe, taken);
			});
		}
	}

	// ------------------------------------------------------------------ moving the swordsman

	/** Where a dash along {@code dir} can go ({@link AuraStep#path}: never through anything solid, over a ward's edge, into lava or fire). */
	public static List<Vec3> path(ServerPlayer player, Vec3 dir, double distance) {
		return AuraStep.path(player, dir, distance);
	}

	/** One stretch of a dash: from where to where, which stretch (1 to {@code ticks}), and whether it's the last. */
	@FunctionalInterface
	public interface Stretch {
		void moved(Vec3 from, Vec3 to, int step, boolean last);
	}

	/**
	 * Carries the swordsman along {@code path} over {@code ticks} ticks, a short teleport each (the view stays theirs), calling
	 * {@code stretch} after each; it stops if they die, change world or mount up. Fall distance is forgotten on the way.
	 */
	public static void dash(ServerPlayer player, List<Vec3> path, int ticks, Stretch stretch) {
		dev.wildercord.aura.MasterFormMovement.begin(player, ticks + 1);
		ServerLevel level = player.level();
		int n = Math.max(1, ticks);
		for (int i = 1; i <= n; i++) {
			Vec3 point = path.get(Math.min(path.size() - 1, (int) Math.round((path.size() - 1) * i / (double) n)));
			Vec3 prev = path.get(Math.min(path.size() - 1, (int) Math.round((path.size() - 1) * (i - 1) / (double) n)));
			int step = i;
			boolean last = i == n;
			Scheduler.later(i - 1, () -> {
				if (!player.isAlive() || player.level() != level || player.isPassenger()) {
					return;
				}
				player.teleportTo(level, point.x, point.y, point.z, Relative.ROTATION, 0.0F, 0.0F, false);
				player.resetFallDistance();
				if (last) {
					player.setDeltaMovement(Vec3.ZERO);
				}
				stretch.moved(prev, point, step, last);
			});
		}
	}

	/**
	 * A spot beside {@code foe} for a blink, on the side toward {@code from}, where the swordsman's whole body fits, inside the same
	 * ward (or none) they stand in now, within the world border and clear of lava and fire; null if there's none.
	 */
	public static Vec3 beside(ServerPlayer player, LivingEntity foe, Vec3 from) {
		return beside(player, foe, from, foe.getBbWidth() / 2 + 0.75);
	}

	/**
	 * A spot behind {@code foe}, on its far side from the swordsman, {@code gap} past its middle, where the swordsman's body fits (the
	 * same rules as {@link #beside}); null if there's none.
	 */
	public static Vec3 behind(ServerPlayer player, LivingEntity foe, double gap) {
		Vec3 away = foe.position().subtract(player.position());
		away = new Vec3(away.x, 0, away.z);
		away = away.lengthSqr() < 1.0E-4 ? flat(player) : away.normalize();
		return beside(player, foe, foe.position().add(away.scale(4)), foe.getBbWidth() / 2 + gap);
	}

	/** {@link #beside}, {@code gap} from the foe's middle. */
	public static Vec3 beside(ServerPlayer player, LivingEntity foe, Vec3 from, double gap) {
		ServerLevel level = player.level();
		Vec3 toward = from.subtract(foe.position());
		toward = new Vec3(toward.x, 0, toward.z);
		toward = toward.lengthSqr() < 1.0E-4 ? flat(player).scale(-1) : toward.normalize();
		boolean warded = DungeonWards.warded(level, player.blockPosition());
		for (double turn : new double[] {0, 0.6, -0.6, 1.2, -1.2}) {
			Vec3 dir = new Vec3(toward.x * Math.cos(turn) - toward.z * Math.sin(turn), 0, toward.x * Math.sin(turn) + toward.z * Math.cos(turn));
			for (double dy : new double[] {0, 0.6, -0.6, 1.0}) {
				Vec3 spot = foe.position().add(dir.scale(gap)).add(0, dy, 0);
				if (fits(player, spot, warded)) {
					return spot;
				}
			}
		}
		return null;
	}

	/** Whether the swordsman's body fits at {@code spot}: nothing solid, the same ward, inside the border, no lava or fire. */
	public static boolean fits(ServerPlayer player, Vec3 spot, boolean warded) {
		ServerLevel level = player.level();
		AABB body = player.getBoundingBox().move(spot.subtract(player.position()));
		return level.noCollision(player, body) && DungeonWards.warded(level, BlockPos.containing(spot)) == warded
			&& level.getWorldBorder().isWithinBounds(spot.x, spot.z)
			&& level.getBlockStates(body.inflate(0, 0.25, 0)).noneMatch(s -> s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE));
	}

	/** Moves the swordsman to {@code spot} at once (the view stays theirs). */
	public static void blink(ServerPlayer player, Vec3 spot) {
		dev.wildercord.aura.MasterFormMovement.begin(player, 2);
		player.teleportTo(player.level(), spot.x, spot.y, spot.z, Relative.ROTATION, 0.0F, 0.0F, false);
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
	}

	/** Sets the swordsman's own motion (a leap, a dive), told to their client at once. */
	public static void launch(ServerPlayer player, Vec3 velocity) {
		dev.wildercord.aura.MasterFormMovement.begin(player, 40);
		player.setDeltaMovement(velocity);
		player.needsSync = true;
		MonsterMagic.sync(player);
	}

	/** The aura's colour and a stage-scaled size, for an art's look. */
	public static int color(ServerPlayer player) {
		return Aura.color(player);
	}

	// ------------------------------------------------------------------ lifecycle

	static void forget(UUID id) {
		HELD.remove(id);
		MENDED.remove(id);
	}

	static void clear() {
		HELD.clear();
		MENDED.clear();
	}
}
