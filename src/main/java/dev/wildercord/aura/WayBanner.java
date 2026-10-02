package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Way of the Banner: what a swordsman walking it gives the people fighting beside them (the numbers are {@link WayRules}). It only
 * ever reaches <b>allies</b> ({@link #ally}): never two duelling each other; teammates; or, without teams, two players who couldn't harm
 * each other either way (PvP off). Two who could fight are never allies, so a Banner never helps an enemy, and everything it does to a
 * body in a fight with another player is scaled down as aura's other bonuses are.
 * <ul>
 * <li><b>Battle Cry</b> (Edge): a third of the momentum the Banner builds builds for each allied swordsman near too
 *     ({@link #momentumBuilt}), and every finisher they land lets out a rallying cry ({@link #cry}): they and everyone allied near
 *     (pets too) are steadied for a few seconds, allied swordsmen build momentum and share the finisher's aura.</li>
 * <li><b>Rallying Presence</b> (Form): a quarter of the aura the Banner gathers flows to allied swordsmen near ({@link #auraGained}),
 *     and while their Intent presses on a foe, they and their allies in its reach are steadied ({@link #presence}).</li>
 * <li><b>Shelter</b> (Sovereign): allied swordsmen near lose less momentum to hits ({@link #bannerNear}); an awakening holds their
 *     momentum at the second tier while it burns; and the Banner's Dominion shelters the party ({@link #shelter}): allies inside take less
 *     from foes, gather aura as the Banner does there, and their momentum doesn't ebb.</li>
 * </ul>
 * Steadying from every source together never takes off more than {@link WayRules#STEADY_MOST} ({@link #harm}).
 */
public final class WayBanner {
	private WayBanner() {}

	/** Each body steadied now: until when, and by how much. */
	private static final Map<UUID, double[]> STEADY = new HashMap<>();

	// ------------------------------------------------------------------ who's an ally

	/**
	 * Whether {@code other} is {@code bearer}'s ally, for anything a Way gives: never two duelling each other; teammates; or two who
	 * couldn't harm each other at all (PvP off, no teams set against them). The rule chorus casting keeps, so the two agree.
	 */
	public static boolean ally(ServerPlayer bearer, Player other) {
		if (other == null || other == bearer || !other.isAlive() || other.isSpectator() || other.level() != bearer.level()) {
			return false;
		}
		if (Duels.opponents(bearer.getUUID(), other.getUUID())) {
			return false;
		}
		if (bearer.isAlliedTo(other)) {
			return true;
		}
		return !bearer.canHarmPlayer(other) && !other.canHarmPlayer(bearer) && !Targets.canHarm(bearer, other) && !Targets.canHarm(other, bearer);
	}

	/** How far the Banner reaches (the server's {@code banner_range}). */
	static double range() {
		return Config.get().aura().ways().bannerRange();
	}

	/** {@code bearer}'s allied swordsmen within {@code range}: allies with a breathing method learned and aura working. */
	static List<ServerPlayer> swordsmen(ServerPlayer bearer, double range) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer other : bearer.level().players()) {
			if (other != bearer && other.distanceToSqr(bearer) <= range * range && Aura.stage(other) >= AuraRules.GLOW && Aura.enabled(other)
					&& ally(bearer, other)) {
				out.add(other);
			}
		}
		return out;
	}

	/** {@code bearer} and everyone fighting beside them within {@code range}: allied players, and their own pets and their team's. */
	static List<LivingEntity> company(ServerPlayer bearer, double range) {
		List<LivingEntity> out = new ArrayList<>();
		out.add(bearer);
		for (ServerPlayer other : bearer.level().players()) {
			if (other.distanceToSqr(bearer) <= range * range && ally(bearer, other)) {
				out.add(other);
			}
		}
		AABB box = bearer.getBoundingBox().inflate(range);
		for (LivingEntity pet : bearer.level().getEntitiesOfClass(LivingEntity.class, box,
				e -> !(e instanceof Player) && e.isAlive() && e.distanceToSqr(bearer) <= range * range && Targets.canHelp(bearer, e))) {
			out.add(pet);
		}
		return out;
	}

	// ------------------------------------------------------------------ sharing (Battle Cry, Rallying Presence)

	/** {@code player} built {@code built} momentum (from anything but a share): with Battle Cry, allied swordsmen near build a share. */
	static void momentumBuilt(ServerPlayer player, double built, String source) {
		if (built <= 0 || "banner".equals(source) || !Ways.has(player, WayRules.BANNER_EDGE)) {
			return;
		}
		double gain = Math.max(1.0E-6, Config.get().aura().momentum().momentumGain());
		double share = WayRules.share(built, Config.get().aura().ways().bannerShare()) / gain;
		if (share <= 0) {
			return;
		}
		for (ServerPlayer ally : swordsmen(player, range())) {
			Momentum.add(ally, share, "banner", MomentumRules.MAX);
		}
	}

	/** {@code player} gathered {@code got} aura: with Rallying Presence, allied swordsmen near are given a share (never while spent). */
	static void auraGained(ServerPlayer player, double got, String source) {
		if (got <= 0 || "banner".equals(source) || !Ways.has(player, WayRules.BANNER_FORM)) {
			return;
		}
		double share = WayRules.share(got, Config.get().aura().ways().bannerAuraShare());
		if (share <= 0) {
			return;
		}
		for (ServerPlayer ally : swordsmen(player, range())) {
			Aura.giveBack(ally, share);
		}
	}

	// ------------------------------------------------------------------ steadied

	/** Steadies {@code body} by {@code reduction} for {@code ticks} (the strongest steadying holds; a weaker one never shortens it). */
	static void steady(LivingEntity body, double reduction, int ticks) {
		long now = body.level().getGameTime();
		double[] cur = STEADY.get(body.getUUID());
		boolean active = cur != null && now < cur[0];
		if (!active || reduction > cur[1] + 1.0E-9 || reduction >= cur[1] - 1.0E-9 && now + ticks > cur[0]) {
			STEADY.put(body.getUUID(), new double[] {now + ticks, reduction});
		}
		if (STEADY.size() > 256) {
			STEADY.values().removeIf(s -> s[0] < now);
		}
	}

	/** How steadied {@code body} is now by cries and presence (0 for not at all). */
	public static double steadiness(LivingEntity body) {
		double[] s = STEADY.get(body.getUUID());
		return s != null && body.level().getGameTime() < s[0] ? s[1] : 0;
	}

	/**
	 * {@code player} landed a finisher on {@code foe} (Battle Cry): a rallying cry. They and their company near are steadied; allied
	 * swordsmen build momentum and share the aura it gave back.
	 */
	static void cry(ServerPlayer player, LivingEntity foe) {
		if (!Ways.has(player, WayRules.BANNER_EDGE)) {
			return;
		}
		ServerLevel level = player.level();
		double range = range();
		for (LivingEntity body : company(player, range)) {
			steady(body, WayRules.CRY_STEADY, WayRules.CRY_TICKS);
			if (body != player) {
				ArtLight.world(player).flash(body.getBoundingBox().getCenter(), AuraVfx.hot(WayRules.BANNER_COLOR, 0.3), 0.9F);
			}
		}
		double aura = StanceRules.finisherAura(Aura.stage(player), Momentum.practice(player, foe)) * WayRules.CRY_AURA;
		for (ServerPlayer ally : swordsmen(player, range)) {
			Momentum.add(ally, WayRules.CRY_MOMENTUM, "banner", MomentumRules.MAX);
			ArtKit.giveBack(ally, aura);
		}
		AuraFx.burst(level, player, player.position().add(0, 0.08, 0), new Vec3(0, 1, 0), WayRules.BANNER_COLOR, 3.2F, AuraFx.Burst.RING | AuraFx.Burst.ECHO);
		AuraFx.burst(level, player, player.position().add(0, 2.3, 0), Vec3.ZERO, AuraVfx.hot(WayRules.BANNER_COLOR, 0.3), 0.9F,
			AuraFx.Burst.FLASH | AuraFx.Burst.STAR);
		Feels.sound(level, player.position().add(0, 1.2, 0), "aura_way_cry", 1.0F, 1.0F);
		Grimoire.unlock(player, "aura:way_cry");
		cries++;
	}

	/** Each press of a Banner's Intent at Form (Rallying Presence): pressing on {@code pressed} foes, it steadies them and theirs near. */
	static void presence(ServerPlayer player, int pressed) {
		if (pressed <= 0 || !Ways.has(player, WayRules.BANNER_FORM)) {
			return;
		}
		for (LivingEntity body : company(player, AuraRules.INTENT_RADIUS)) {
			steady(body, WayRules.PRESENCE_STEADY, WayRules.PRESENCE_TICKS);
		}
	}

	// ------------------------------------------------------------------ Shelter (Sovereign)

	/** Whether an ally of {@code player}'s walking the Banner at Sovereign stands near them: hits take less of their momentum. */
	static boolean bannerNear(ServerPlayer player) {
		double range = range();
		for (ServerPlayer other : player.level().players()) {
			if (other != player && other.distanceToSqr(player) <= range * range && Ways.has(other, WayRules.BANNER_SOVEREIGN) && ally(other, player)) {
				return true;
			}
		}
		return false;
	}

	/** Whether a Dominion of {@code owner}'s shelters {@code body}: the owner, an ally, or a pet of theirs or their team's. */
	private static boolean sheltered(ServerPlayer owner, LivingEntity body) {
		if (body == owner) {
			return true;
		}
		if (body instanceof Player other) {
			return ally(owner, other);
		}
		return Targets.canHelp(owner, body);
	}

	/** How much the Banners' Dominions {@code body} stands in shelter it (0 for none). */
	public static double shelter(LivingEntity body) {
		if (!(body.level() instanceof ServerLevel level)) {
			return 0;
		}
		for (AuraDominion.Field field : AuraDominion.fields()) {
			if (field.level() != level || level.getGameTime() > field.until() || !field.inside(body)) {
				continue;
			}
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner());
			if (owner != null && Ways.has(owner, WayRules.BANNER_SOVEREIGN) && sheltered(owner, body)) {
				return WayRules.SHELTER;
			}
		}
		return 0;
	}

	/** Each tick of a Banner's Dominion at Sovereign: allied swordsmen inside gather aura there and their momentum holds. */
	static void sheltering(ServerPlayer owner, AuraDominion.Field field, long age) {
		if (age % 10 != 5 || !Ways.has(owner, WayRules.BANNER_SOVEREIGN)) {
			return;
		}
		for (ServerPlayer ally : swordsmen(owner, field.radius() + 2)) {
			if (field.inside(ally)) {
				Aura.gain(ally, AuraRules.DOMINION_TRICKLE / 2, "dominion");
				Momentum.engaged(ally);
				if (age % 20 == 5) {
					ArtLight.world(owner).flash(ally.position().add(0, 0.2, 0), AuraVfx.hot(WayRules.BANNER_COLOR, 0.2), 0.7F);
				}
			}
		}
	}

	// ------------------------------------------------------------------ what reaches a body

	/**
	 * Harm about to reach {@code target} (from {@code mixin.LivingEntityAuraMixin}, after a foe's Dominion has weakened it): lessened by
	 * how steadied they are (cries, presence), the shelter of a Banner's Dominion, and an unbroken Bulwark's awakening, together never
	 * past {@link WayRules#STEADY_MOST}, and against a player's harm only by the PvP scale's share. Only harm a foe deals (a creature, a
	 * player, their shot or spell): a fall, the void or hunger are the body's own.
	 */
	public static float harm(LivingEntity target, DamageSource source, float damage) {
		if (damage <= 0 || source.getEntity() == null || source.getEntity() == target || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return damage;
		}
		double r = steadiness(target);
		double shelter = AuraDominion.fields().isEmpty() ? 0 : shelter(target);
		if (shelter > 0) {
			r = WayRules.steadied(r, shelter);
		}
		if (target instanceof Player player && WayEffects.unbroken(player)) {
			r = WayRules.steadied(r, WayRules.BULWARK_AWAKENED_HARM);
		}
		if (r <= 0) {
			return damage;
		}
		return (float) (damage * WayRules.harmLeft(r, source.getEntity() instanceof Player, Config.get().aura().pvpScale()));
	}

	/** Cries let out since the server started (the game tests read it). */
	private static int cries;

	public static int cries() {
		return cries;
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		AuraApi.onFinisher(new AuraApi.FinisherHook() {
			@Override
			public void landed(ServerPlayer attacker, LivingEntity target, AuraApi.Finisher finisher, float dealt) {
				cry(attacker, target);
			}
		});
		// Shelter: an awakening holds allied swordsmen's momentum at the second tier while it burns (never lowering an ally's own).
		AuraApi.onAwakening(new AuraApi.AwakeningHook() {
			@Override
			public void awakened(ServerPlayer player, int ticks) {
				if (!Ways.has(player, WayRules.BANNER_SOVEREIGN)) {
					return;
				}
				for (ServerPlayer ally : swordsmen(player, range())) {
					if (!Awakening.awakened(ally)) {
						Momentum.hold(ally, WayRules.RALLY_FLOOR, ticks);
						AuraFx.burst(player.level(), ally, ally.position().add(0, 2.2, 0), Vec3.ZERO, WayRules.BANNER_COLOR, 0.8F,
							AuraFx.Burst.FLASH | AuraFx.Burst.STAR);
					}
				}
			}
		});
		// Allies standing in a Banner's Dominion gather aura there as its owner does.
		AuraApi.onGain((player, amount, source) -> {
			if (AuraDominion.fields().isEmpty()) {
				return amount;
			}
			for (AuraDominion.Field field : AuraDominion.fields()) {
				if (field.owner().equals(player.getUUID()) || field.level() != player.level() || player.level().getGameTime() > field.until()
						|| !field.inside(player)) {
					continue;
				}
				ServerPlayer owner = player.level().getServer().getPlayerList().getPlayer(field.owner());
				if (owner != null && Ways.has(owner, WayRules.BANNER_SOVEREIGN) && ally(owner, player)) {
					return amount * AuraRules.DOMINION_FLOW;
				}
			}
			return amount;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STEADY.clear();
			cries = 0;
		});
	}
}
