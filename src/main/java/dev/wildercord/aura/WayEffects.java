package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the Ways of the Blade, the Bulwark and the Shadowstep do (the numbers are {@link WayRules}; the Banner's is {@link WayBanner}).
 * Each node changes a technique where that technique lives, asking {@link Ways#has}; what can go through aura's hooks does
 * ({@link AuraApi#onMomentum}, {@link AuraApi#onStance}, {@link AuraApi#onFinisher}), and the rest is asked from here by the technique
 * itself: Aura Slash and the crescents for the Blade's pierce, Aura Guard, aura armour, Intent and Dominion for the Bulwark's, the guard's
 * perfect moment and Aura Step for the Shadowstep's, and the awakening for each Sovereign node that changes it.
 */
public final class WayEffects {
	private WayEffects() {}

	/** When each Shadowstep's blows stop counting as from behind after a step (Afterimage), by swordsman. */
	private static final Map<UUID, Long> UNSEEN = new HashMap<>();
	/** Foes that lost a Shadowstep who slipped behind them, until when, by swordsman then foe. */
	private static final Map<UUID, Map<UUID, Long>> LOST = new HashMap<>();
	/** When each Bulwark's guard may next throw a blow back at, or stagger, each striker (a rest per striker). */
	private static final Map<UUID, Map<UUID, Long>> REFLECTED = new HashMap<>();
	private static final Map<UUID, Map<UUID, Long>> STAGGERED = new HashMap<>();

	/** How many times each effect has gone off since the server started (the game tests read it). */
	private static final Map<String, Integer> COUNTS = new HashMap<>();

	private static void count(String what) {
		COUNTS.merge(what, 1, Integer::sum);
	}

	/** How many times {@code what} has gone off (slip, afterimage, cascade, reflect, shot, stagger, challenge, bastion, ready). */
	public static int counted(String what) {
		return COUNTS.getOrDefault(what, 0);
	}

	// ------------------------------------------------------------------ the Blade

	/** Whether {@code player}'s Aura Slash pierces (Keen Edge): through guards, through more foes, an edge in a clash. */
	public static boolean pierces(ServerPlayer player) {
		return Ways.has(player, WayRules.BLADE_EDGE);
	}

	/** Whether {@code player} is awakened with the Blade's Sovereign node: the slash free and quick. */
	private static boolean stormOfEdges(ServerPlayer player) {
		return Ways.has(player, WayRules.BLADE_SOVEREIGN) && Awakening.awakened(player);
	}

	/** Aura Slash's price for {@code player}, of {@code price}. */
	public static double slashPrice(ServerPlayer player, double price) {
		return stormOfEdges(player) ? price * WayRules.BLADE_AWAKENED_SLASH_PRICE : price;
	}

	/** Aura Slash's rest for {@code player} (ticks), of {@code ticks}. */
	public static int slashRest(ServerPlayer player, int ticks) {
		return stormOfEdges(player) ? (int) Math.round(ticks * WayRules.BLADE_AWAKENED_SLASH_REST) : ticks;
	}

	/** What a finisher feeds {@code player}'s awakening now, fed {@code extended} already: twice as long with Storm of Edges. */
	public static int feed(ServerPlayer player, int extended) {
		if (Ways.has(player, WayRules.BLADE_SOVEREIGN)) {
			return Math.max(0, Math.min(WayRules.BLADE_FEED, WayRules.BLADE_FEED_MOST - Math.max(0, extended)));
		}
		return AwakeningRules.extend(extended);
	}

	/**
	 * A finisher of a Blade at Form landed on {@code finished}: the nearest creature near it worn half through or more is opened too
	 * (never a player, never a boss), and a crowd falls one after another.
	 */
	static void cascade(ServerPlayer attacker, LivingEntity finished) {
		ServerLevel level = attacker.level();
		long now = level.getGameTime();
		LivingEntity best = null;
		double nearest = Double.MAX_VALUE;
		double r = WayRules.CASCADE_RADIUS;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, finished.getBoundingBox().inflate(r),
				e -> e != finished && e != attacker && e.isAlive() && !(e instanceof Player) && !Spirits.isBoss(e))) {
			Stance.State s = Stance.state(e);
			if (s == null || s.opened(now) || s.steady(now) || !WayRules.cascades(s.left(now)) || !Stance.eligible(attacker, e)) {
				continue;
			}
			double d = e.distanceToSqr(finished);
			if (d <= r * r && d < nearest) {
				nearest = d;
				best = e;
			}
		}
		if (best == null) {
			return;
		}
		Stance.State s = Stance.state(best);
		double rest = s.pool() - s.wornAt(now);
		Vec3 from = finished.getBoundingBox().getCenter();
		Vec3 to = best.getBoundingBox().getCenter();
		if (Stance.wear(attacker, best, rest * 1.5 + 1, StanceRules.Source.ART) <= 0) {
			return;
		}
		// A crack of gold leaping from the finished foe to the next.
		int color = WayRules.BLADE_COLOR;
		Vec3 mid = from.add(to).scale(0.5).add(0, 0.5, 0);
		ArtLight.world(attacker).ray(from, mid, AuraVfx.hot(Stance.OPENED_COLOR, 0.2), 0.11, 8).ray(mid, to, AuraVfx.hot(Stance.OPENED_COLOR, 0.2), 0.11, 8)
			.ray(from, to, AuraVfx.hot(color, 0.6), 0.04, 6).flash(to, AuraVfx.hot(Stance.OPENED_COLOR, 0.4), 1.4F);
		Feels.sound(level, to, "aura_way_cascade", 0.9F, 1.0F);
		Grimoire.unlock(attacker, "aura:way_cascade");
		count("cascade");
	}

	// ------------------------------------------------------------------ the Bulwark

	/** Whether {@code player}'s Aura Guard covers every side (Wide Guard). */
	public static boolean coversAll(Player player) {
		return Ways.has(player, WayRules.BULWARK_EDGE);
	}

	/** How long {@code player}'s guard stays in its perfect moment (ticks): the parry's, or half again with Wide Guard. */
	public static int perfectWindow(Player player) {
		return Ways.has(player, WayRules.BULWARK_EDGE) ? WayRules.BULWARK_PERFECT : AuraRules.PERFECT_TICKS;
	}

	/**
	 * A blow caught on {@code player}'s held guard ({@code absorbed} of it taken off): with Wide Guard a third of it is thrown back at
	 * its striker as aura off the blade (a player's spell defences meet it); with Living Wall a creature that struck staggers.
	 */
	static void held(ServerPlayer player, DamageSource source, double absorbed) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker || attacker == player
				|| source.is(Aura.DAMAGE) || absorbed <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		if (Ways.has(player, WayRules.BULWARK_EDGE) && Targets.canHarm(player, attacker) && rested(REFLECTED, player, attacker, now, WayRules.BULWARK_REFLECT_REST)) {
			float back = AuraCombat.projected(player, attacker, absorbed * WayRules.BULWARK_REFLECT, false);
			if (back > 0) {
				Vec3 at = attacker.getBoundingBox().getCenter();
				AuraFx.burst(player.level(), player, at, Vec3.ZERO, AuraVfx.hot(WayRules.BULWARK_COLOR, 0.3), 1.0F, AuraFx.Burst.FLASH | AuraFx.Burst.RING);
				Feels.sound(player.level(), at, "aura_way_reflect", 0.8F, 1.0F);
				count("reflect");
			}
		}
		if (Ways.has(player, WayRules.BULWARK_FORM) && attacker instanceof Mob mob && !Spirits.isBoss(mob)
				&& rested(STAGGERED, player, attacker, now, WayRules.BULWARK_STAGGER_REST)) {
			mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WayRules.BULWARK_STAGGER, 1, false, true), player);
			mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WayRules.BULWARK_STAGGER, 0, false, true), player);
			mob.getNavigation().stop();
			count("stagger");
		}
	}

	/** Whether {@code player}'s held guard (not its perfect moment) turns a shot back (Wide Guard), paying for it. */
	static boolean turnsShot(ServerPlayer player) {
		if (!Ways.has(player, WayRules.BULWARK_EDGE) || !AuraGuard.guarding(player) || Aura.aura(player) < WayRules.BULWARK_SHOT_COST) {
			return false;
		}
		Aura.spend(player, WayRules.BULWARK_SHOT_COST, "guard");
		Feels.sound(player.level(), player.position().add(0, 1.2, 0), "aura_way_reflect", 0.7F, 1.25F);
		count("shot");
		return true;
	}

	/** Aura armour's share of a blow for {@code player}, of the server's {@code share}: a tenth more with Living Wall. */
	public static double armourShare(Player player, double share) {
		return Ways.has(player, WayRules.BULWARK_FORM) ? WayRules.armourShare(share) : share;
	}

	/** What aura armour costs {@code player} a point, as a share of its price: less with Living Wall. */
	public static double armourCost(Player player) {
		return Ways.has(player, WayRules.BULWARK_FORM) ? WayRules.BULWARK_ARMOUR_COST : 1.0;
	}

	/** What a hit takes off {@code player}'s momentum, as a share of what it otherwise would. */
	static double lossScale(ServerPlayer player) {
		return WayRules.lossScale(Ways.has(player, WayRules.BULWARK_EDGE), WayBanner.bannerNear(player));
	}

	/** Whether {@code player} is awakened with the Bulwark's Sovereign node: never opened, and what reaches them a fifth weaker. */
	public static boolean unbroken(Player player) {
		return Ways.has(player, WayRules.BULWARK_SOVEREIGN) && Awakening.awakened(player);
	}

	/** Whether a spent {@code player} is slowed (not a Bulwark at Sovereign). */
	static boolean spentSlows(ServerPlayer player) {
		return !Ways.has(player, WayRules.BULWARK_SOVEREIGN);
	}

	/**
	 * Each press of a Bulwark's Intent at Form (Living Wall's challenge): creatures within its reach set on an ally of theirs (another
	 * player they're allied with, a pet of theirs or their team's) turn on them instead. Never a boss. Returns how many turned.
	 */
	static int challenge(ServerPlayer player) {
		if (!Ways.has(player, WayRules.BULWARK_FORM)) {
			return 0;
		}
		double r = AuraRules.INTENT_RADIUS;
		int turned = 0;
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r), m -> m.isAlive() && m.distanceToSqr(player) <= r * r)) {
			LivingEntity target = mob.getTarget();
			if (target == null || target == player || Spirits.isBoss(mob) || !Targets.canHarm(player, mob) || Targets.playerPet(mob)) {
				continue;
			}
			boolean ally = target instanceof ServerPlayer other ? WayBanner.ally(player, other) : Targets.canHelp(player, target);
			if (!ally) {
				continue;
			}
			mob.setTarget(player);
			turned++;
			AuraFx.burst(player.level(), player, new Vec3(mob.getX(), mob.getBoundingBox().maxY + 0.35, mob.getZ()), Vec3.ZERO, WayRules.BULWARK_COLOR,
				0.6F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR);
		}
		if (turned > 0) {
			count("challenge");
		}
		return turned;
	}

	/**
	 * A bastion Dominion's edge (a Bulwark at Sovereign): each tick, shots of a foe's crossing into it from outside are turned back at its
	 * rim. Returns how many it turned.
	 */
	static int bastion(ServerPlayer owner, ServerLevel level, Vec3 centre, double radius) {
		int turned = 0;
		if (level.getGameTime() % 10 == 0) {
			// Its edge stands as a faint wall of the Bulwark's light: rings at the rim, one over another.
			for (double h : new double[] {0.15, 1.0, 1.9}) {
				ArtLight.world(owner).ring(centre.add(0, h, 0), new Vec3(0, 1, 0), AuraVfx.hot(WayRules.BULWARK_COLOR, h > 1.5 ? 0.4 : 0.15), radius - 0.08,
					radius, h > 0.5 ? 0.035 : 0.06, 13);
			}
		}
		AABB box = new AABB(centre, centre).inflate(radius + 3, 6, radius + 3);
		for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, Entity::isAlive)) {
			Entity shooter = p.getOwner();
			if (!(shooter instanceof LivingEntity living) || shooter == owner || !Targets.canHarm(owner, living)) {
				continue;
			}
			Vec3 pos = p.position();
			Vec3 v = p.getDeltaMovement();
			double dx = pos.x - centre.x;
			double dz = pos.z - centre.z;
			double d = Math.hypot(dx, dz);
			if (d < 1.0E-3 || d > radius + 0.9 || d < radius - 1.4 || pos.y < centre.y - 2 || pos.y > centre.y + 6) {
				continue;
			}
			double nx = dx / d;
			double nz = dz / d;
			double inward = -(v.x * nx + v.z * nz);
			if (inward < 0.05) {
				continue;
			}
			// Off the rim as off a wall: the part of its flight toward the heart turned outward.
			p.setDeltaMovement(v.x + 2 * inward * nx, v.y, v.z + 2 * inward * nz);
			p.needsSync = true;
			turned++;
			ArtLight.world(owner).flash(pos, AuraVfx.hot(WayRules.BULWARK_COLOR, 0.35), 1.1F);
			if (turned == 1) {
				Feels.sound(level, pos, "aura_way_bastion", 0.9F, 0.9F + level.getRandom().nextFloat() * 0.2F);
			}
			count("bastion");
		}
		return turned;
	}

	// ------------------------------------------------------------------ the Shadowstep

	/**
	 * Whether {@code attacker}'s blow on {@code target} counts as from behind: they stand in its rear, or it lost them (a slip), or
	 * they stepped just now (Afterimage).
	 */
	public static boolean fromBehind(ServerPlayer attacker, LivingEntity target) {
		long now = attacker.level().getGameTime();
		Long unseen = UNSEEN.get(attacker.getUUID());
		if (unseen != null && now <= unseen) {
			return true;
		}
		Map<UUID, Long> lost = LOST.get(attacker.getUUID());
		Long until = lost == null ? null : lost.get(target.getUUID());
		if (until != null && now <= until) {
			return true;
		}
		double yaw = Math.toRadians(target instanceof Player ? target.getYRot() : target.yBodyRot);
		Vec3 to = attacker.position().subtract(target.position());
		return WayRules.behind(-Math.sin(yaw), Math.cos(yaw), to.x, to.z);
	}

	/** Whether {@code player}'s blows count as from behind everywhere now (just stepped, with Afterimage). */
	public static boolean unseen(Player player) {
		Long until = UNSEEN.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	/**
	 * Where {@code player}'s perfect guard against {@code attacker}'s blow slips them (Slip): behind it, on its far side, where their body
	 * fits and nothing solid stands between it and the spot (never through a wall, a ward's edge or into lava). Null without the node or
	 * where there's no room.
	 */
	static Vec3 slipSpot(ServerPlayer player, LivingEntity attacker) {
		if (!Ways.has(player, WayRules.SHADOWSTEP_EDGE) || attacker == null || !attacker.isAlive() || attacker.level() != player.level()
				|| player.isPassenger() || player.isSleeping() || attacker.distanceTo(player) > 6) {
			return null;
		}
		Vec3 spot = ArtKit.behind(player, attacker, WayRules.SLIP_GAP);
		if (spot == null) {
			return null;
		}
		Vec3 heart = attacker.getBoundingBox().getCenter();
		HitResult through = player.level().clip(new ClipContext(heart, spot.add(0, 0.9, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
		return through.getType() == HitResult.Type.MISS ? spot : null;
	}

	/**
	 * {@code player}'s perfect guard just turned {@code attacker}'s blow aside (Slip): they slip to {@code spot} behind it ({@link #slipSpot}),
	 * facing its back, so the counter that follows falls there, and it loses them a moment.
	 */
	static void slip(ServerPlayer player, LivingEntity attacker, Vec3 spot) {
		ServerLevel level = player.level();
		Vec3 heart = attacker.getBoundingBox().getCenter();
		Vec3 from = player.position();
		Vec3 look = heart.subtract(spot.add(0, player.getEyeHeight(), 0));
		float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
		player.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), yaw, player.getXRot(), false);
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		long now = level.getGameTime();
		LOST.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(attacker.getUUID(), now + WayRules.SLIP_LOST);
		Vec3 dir = spot.subtract(from);
		AuraStep.afterimages(player, from, spot, new Vec3(dir.x, 0, dir.z).lengthSqr() < 1.0E-4 ? ArtKit.flat(player) : new Vec3(dir.x, 0, dir.z).normalize(),
			WayRules.SHADOWSTEP_COLOR);
		ArtLight.world(player).bare().flash(from.add(0, 1.0, 0), WayRules.SHADOWSTEP_COLOR | ArtLight.DARK, 1.4F);
		AuraFx.trail(player, AuraFxRules.Stroke.SPIN, false, 1.1F);
		Feels.sound(level, spot.add(0, 1, 0), "aura_way_slip", 1.0F, 1.0F);
		Grimoire.unlock(player, "aura:way_slip");
		count("slip");
	}

	/** How long the afterimage a step leaves where it began lingers for {@code player} (ticks; 0 for an ordinary step). */
	static int linger(ServerPlayer player) {
		return Ways.has(player, WayRules.SHADOWSTEP_FORM) ? WayRules.AFTERIMAGE_LINGER : 0;
	}

	/** Whether {@code player} is awakened with the Shadowstep's Sovereign node: the step free, quick, and its afterimage striking twice. */
	private static boolean thousandShadows(ServerPlayer player) {
		return Ways.has(player, WayRules.SHADOWSTEP_SOVEREIGN) && Awakening.awakened(player);
	}

	/** Aura Step's price for {@code player}, of {@code price}. */
	public static double stepPrice(ServerPlayer player, double price) {
		return thousandShadows(player) ? price * WayRules.SHADOW_AWAKENED_STEP_PRICE : price;
	}

	/** Aura Step's rest for {@code player} (ticks), of {@code ticks}. */
	public static int stepRest(ServerPlayer player, int ticks) {
		return thousandShadows(player) ? (int) Math.round(ticks * WayRules.SHADOW_AWAKENED_STEP_REST) : ticks;
	}

	/**
	 * {@code player} stepped from {@code from} (Aura Step): with Afterimage every blow counts as from behind for a moment, and the
	 * afterimage left where they set off strikes once they've gone (twice, awakened at Sovereign).
	 */
	static void stepped(ServerPlayer player, Vec3 from) {
		if (!Ways.has(player, WayRules.SHADOWSTEP_FORM)) {
			return;
		}
		ServerLevel level = player.level();
		UNSEEN.put(player.getUUID(), level.getGameTime() + WayRules.UNSEEN_TICKS);
		int strikes = thousandShadows(player) ? 2 : 1;
		float yaw = player.getYRot();
		for (int k = 0; k < strikes; k++) {
			int again = k;
			Scheduler.later(WayRules.AFTERIMAGE_DELAY + k * WayRules.AFTERIMAGE_AGAIN, () -> {
				if (player.isAlive() && player.level() == level) {
					afterimageStrikes(player, level, from, yaw, again);
				}
			});
		}
	}

	/** The afterimage at {@code from} strikes: a turning cut through every foe beside it. */
	private static void afterimageStrikes(ServerPlayer player, ServerLevel level, Vec3 from, float yaw, int again) {
		ArtKit.Hits hits = ArtKit.hits(player, AuraFx.art(player).color(WayRules.SHADOWSTEP_COLOR));
		for (LivingEntity foe : ArtKit.around(player, from, WayRules.AFTERIMAGE_RADIUS, 1.0, 2.6, 8)) {
			hits.strike(foe, WayRules.AFTERIMAGE_FACTOR, AuraFxRules.Weight.FULL);
		}
		// Its cut, seen by everyone (the swordsman has stepped away from it): a crescent of shadow-light sweeping round where they stood.
		Vec3 centre = from.add(0, 1.0, 0);
		double a = Math.toRadians(yaw) + (again == 0 ? 0 : Math.PI);
		Vec3 toward = new Vec3(-Math.sin(a), 0, Math.cos(a));
		int color = WayRules.SHADOWSTEP_COLOR;
		ArtLight.world(player).slash(centre, new Vec3(0, 1, 0), toward, AuraVfx.hot(color, 0.3), 1.6, 4.6, 0.12, 3, 9)
			.slash(centre.add(0, 0.3, 0), new Vec3(0.25, 1, 0).normalize(), toward.scale(-1), color, 1.3, 3.4, 0.07, 2, 8)
			.groundRing(from, color, 0.4, WayRules.AFTERIMAGE_RADIUS, 0.06, 9);
		Feels.sound(level, centre, "aura_way_afterimage", 0.9F, again == 0 ? 1.0F : 1.15F);
		Grimoire.unlock(player, "aura:way_afterimage");
		count("afterimage");
	}

	/** A finisher {@code player} landed on {@code target}: from behind, with Thousand Shadows, the step is ready at once. */
	static void finisherLanded(ServerPlayer player, LivingEntity target) {
		if (Ways.has(player, WayRules.SHADOWSTEP_SOVEREIGN) && fromBehind(player, target)) {
			long now = player.level().getGameTime();
			AuraPresence.Timers timers = AuraPresence.timers(player);
			if (timers.stepReadyAt() > now) {
				AuraPresence.timers(player, timers.stepReady(now));
				AuraFx.bodyAuraFlare(player, 14, 0.5F);
				count("ready");
			}
		}
	}

	// ------------------------------------------------------------------ the hooks

	/** The most one blow (or an art's strikes together) may wear of a player's stance. */
	private static double playerBlowCap() {
		return StanceRules.PLAYER_POOL * StanceRules.PVP_BLOW_CAP;
	}

	private static boolean rested(Map<UUID, Map<UUID, Long>> rests, ServerPlayer player, LivingEntity other, long now, int rest) {
		Map<UUID, Long> mine = rests.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		Long next = mine.get(other.getUUID());
		if (next != null && now < next) {
			return false;
		}
		mine.put(other.getUUID(), now + rest);
		if (mine.size() > 64) {
			mine.values().removeIf(t -> t < now);
		}
		return true;
	}

	static void init() {
		// Keen Edge: clean hits build momentum faster.
		AuraApi.onMomentum((player, amount, source) -> "hit".equals(source) && Ways.has(player, WayRules.BLADE_EDGE)
			? amount * WayRules.BLADE_HIT_MOMENTUM : amount);
		// Wide Guard and Unbroken: a Bulwark's stance is hard to break, unbreakable while awakened at Sovereign. Slip: blows from behind
		// open foes faster (against a player, never past what one blow may wear).
		AuraApi.onStance((attacker, target, wear, source) -> {
			double w = wear;
			if (target instanceof ServerPlayer bulwark) {
				if (unbroken(bulwark)) {
					return 0;
				}
				if (Ways.has(bulwark, WayRules.BULWARK_EDGE)) {
					w = WayRules.bulwarkStance(w, false);
				}
			}
			if (source != StanceRules.Source.GUARD && source != StanceRules.Source.GUARDED && Ways.has(attacker, WayRules.SHADOWSTEP_EDGE)
					&& fromBehind(attacker, target)) {
				w = WayRules.fromBehind(w, target instanceof Player, playerBlowCap());
			}
			return w;
		});
		AuraApi.onFinisher(new AuraApi.FinisherHook() {
			@Override
			public double extra(ServerPlayer attacker, LivingEntity target, double extra) {
				if (!Ways.has(attacker, WayRules.BLADE_FORM)) {
					return extra;
				}
				double cap = target instanceof Player ? StanceRules.playerFinisherCap(target.getMaxHealth(), Config.get().aura().pvpScale(),
					Config.get().aura().momentum().finisherDamage()) : Double.MAX_VALUE;
				return WayRules.finisher(extra, target instanceof Player, cap);
			}

			@Override
			public void landed(ServerPlayer attacker, LivingEntity target, AuraApi.Finisher finisher, float dealt) {
				if (Ways.has(attacker, WayRules.BLADE_FORM)) {
					cascade(attacker, target);
				}
				finisherLanded(attacker, target);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			UNSEEN.remove(id);
			LOST.remove(id);
			REFLECTED.remove(id);
			STAGGERED.remove(id);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			UNSEEN.clear();
			LOST.clear();
			REFLECTED.clear();
			STAGGERED.clear();
		});
	}
}
