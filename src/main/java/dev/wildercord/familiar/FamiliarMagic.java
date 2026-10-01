package dev.wildercord.familiar;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.SpellDefence;
import dev.wildercord.cast.Targets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * A familiar's own small magic, one spell of its element every so often (sooner and stronger as
 * it levels): Fire throws an ember bolt at what you're fighting, Frost slows it, Storm zaps it,
 * Life heals you, Wind lifts your jumps (and catches you when you fall), Earth hardens you while
 * you're under attack, Void pulls drops to you, and Arcane shows the monsters hiding nearby.
 *
 * <p>It never changes blocks, and it only harms what its owner is fighting: another player only when
 * the two of them are in a fight with each other and PvP is on (the same friendly-fire rules as
 * every spell, see {@link Targets}).</p>
 */
public final class FamiliarMagic {
	private FamiliarMagic() {}

	/** How long after a blow is struck (either way) its owner still counts as fighting it: 5 seconds. */
	private static final int IN_COMBAT = 100;
	private static final double REACH = 16.0;

	static void tick(ServerLevel level, Wisp wisp, ServerPlayer owner, int familiarLevel) {
		if(FamiliarRoles.get(owner)!=FamiliarRoles.Role.COMPANION) {
			if(--wisp.helpCooldown<=0)wisp.helpCooldown=FamiliarRoles.help(owner,wisp)?WispRules.helpInterval(familiarLevel):40;
			return;
		}
		if (--wisp.watchCooldown <= 0) {
			wisp.watchCooldown = 20;
			watch(level, wisp, owner, familiarLevel);
		}
		if (--wisp.helpCooldown <= 0) {
			// Nothing to do yet: look again in a second.
			wisp.helpCooldown = help(level, wisp, owner, familiarLevel) ? WispRules.helpInterval(familiarLevel) : 20;
		}
	}

	/** Once a second: Wind's feather fall, and Void gathering drops. */
	private static void watch(ServerLevel level, Wisp wisp, ServerPlayer owner, int familiarLevel) {
		switch (wisp.element()) {
			case "wind" -> {
				if (owner.fallDistance > 4 && !owner.hasEffect(MobEffects.SLOW_FALLING) && !owner.getAbilities().flying) {
					owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true));
					cast(level, wisp, owner.position().add(0, 1, 0));
					ElementFx.windImpact(level, owner.position(), 0.6);
				}
			}
			case "void" -> {
				double radius = 5 + 2 * familiarLevel;
				AABB box = owner.getBoundingBox().inflate(radius);
				Vec3 to = owner.position().add(0, 0.6, 0);
				boolean pulled = false;
				// Only what nobody dropped (a monster's loot, a broken block's), and nothing nearer someone else:
				// never another player's things, their death drops or their experience.
				for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, i -> i.isAlive() && i.getAge() > 10
						&& ((dev.wildercord.mixin.ItemEntityAccessor) i).wildercord$thrower() == null && !othersNear(level, owner, i))) {
					pull(item, to);
					pulled = true;
				}
				for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box, o -> o.isAlive() && !othersNear(level, owner, o))) {
					pull(orb, to);
					pulled = true;
				}
				if (pulled && level.getGameTime() % 40 == 0) {
					ElementFx.implode(level, wisp.position().add(0, 0.2, 0), 0.6, 10);
				}
			}
			default -> { }
		}
	}

	/** How near another player must be to something for a Void familiar to leave it to them. */
	private static final double OTHERS = 8.0;

	/** Whether another player (not the owner, not a spectator) is close to this thing. */
	private static boolean othersNear(ServerLevel level, ServerPlayer owner, net.minecraft.world.entity.Entity thing) {
		for (ServerPlayer other : level.players()) {
			if (other != owner && !other.isSpectator() && other.distanceToSqr(thing) < OTHERS * OTHERS) {
				return true;
			}
		}
		return false;
	}

	private static void pull(net.minecraft.world.entity.Entity thing, Vec3 to) {
		Vec3 d = to.subtract(thing.position());
		if (d.lengthSqr() > 1.5) {
			thing.setDeltaMovement(d.normalize().scale(0.45));
			thing.needsSync = true;
		}
	}

	/** One little spell, if there's a reason for it. Returns whether it cast. */
	private static boolean help(ServerLevel level, Wisp wisp, ServerPlayer owner, int familiarLevel) {
		double power = WispRules.helpPower(familiarLevel);
		LivingEntity target = target(level, wisp, owner);
		Vec3 from = wisp.position().add(0, 0.22, 0);
		switch (wisp.element()) {
			case "fire" -> {
				if (target == null) {
					return false;
				}
				Vec3 at = target.getBoundingBox().getCenter();
				ElementFx.ray(level, from, at, ElementFx.FIRE.primary(), 0.12, 6);
				ElementFx.embers(level, at, 0.3, 6);
				ElementFx.fireImpact(level, at, 0.5);
				SpellDefence.hurt(level, target, level.damageSources().indirectMagic(wisp, owner), (float) (3 * power));
				target.igniteForTicks(40);
			}
			case "frost" -> {
				if (target == null) {
					return false;
				}
				Vec3 at = target.getBoundingBox().getCenter();
				ElementFx.ray(level, from, at, ElementFx.FROST.primary(), 0.1, 6);
				ElementFx.frostImpact(level, at, 0.6);
				SpellDefence.hurt(level, target, level.damageSources().indirectMagic(wisp, owner), (float) (1.5 * power));
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (80 * power), 1, false, true), owner);
			}
			case "storm" -> {
				if (target == null) {
					return false;
				}
				Vec3 at = target.getBoundingBox().getCenter();
				ElementFx.bolt(level, from, at, 0.06, 1, 2);
				ElementFx.stormImpact(level, at, 0.5);
				SpellDefence.hurt(level, target, level.damageSources().indirectMagic(wisp, owner), (float) (4 * power));
			}
			case "life" -> {
				if (owner.getHealth() > owner.getMaxHealth() - 2) {
					return false;
				}
				owner.heal((float) (3 * power));
				ElementFx.ray(level, from, owner.position().add(0, 1, 0), ElementFx.LIFE.primary(), 0.08, 8);
				ElementFx.lifeImpact(level, owner.position().add(0, 0.8, 0), 0.6);
			}
			case "wind" -> {
				if (!fighting(owner)) {
					return false;
				}
				owner.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, (int) (160 * power), familiarLevel >= 3 ? 1 : 0, false, true));
				owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, (int) (60 * power), 0, false, true));
				ElementFx.windImpact(level, owner.position().add(0, 0.2, 0), 0.7);
			}
			case "earth" -> {
				if (owner.tickCount - owner.getLastHurtByMobTimestamp() > IN_COMBAT || owner.getLastHurtByMob() == null) {
					return false;
				}
				owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, (int) (80 * power), 0, false, true));
				ElementFx.earthImpact(level, owner.position().add(0, 0.1, 0), 0.6);
			}
			case "arcane" -> {
				double radius = 12 + 4 * familiarLevel;
				List<Mob> hidden = level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(radius),
					m -> m instanceof Enemy && m.isAlive() && !m.hasEffect(MobEffects.GLOWING));
				if (hidden.isEmpty()) {
					return false;
				}
				for (Mob mob : hidden) {
					mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) (120 * power), 0, false, false), owner);
				}
				ElementFx.arcaneImpact(level, from, 0.6);
				ElementFx.ring(level, from, new Vec3(0, 1, 0), ElementFx.ARCANE.primary(), 0.2, 2.5, 0.05, 12);
			}
			default -> {
				return false;
			}
		}
		cast(level, wisp, from);
		return true;
	}

	private static void cast(ServerLevel level, Wisp wisp, Vec3 at) {
		wisp.flare();
		Fx.sound(level, at, FamiliarContent.WISP_CAST, 0.7F, 0.95F + level.getRandom().nextFloat() * 0.1F);
	}

	/** Whether the owner has struck or been struck in the last few seconds. */
	private static boolean fighting(ServerPlayer owner) {
		return owner.getLastHurtMob() != null && owner.tickCount - owner.getLastHurtMobTimestamp() <= IN_COMBAT
			|| owner.getLastHurtByMob() != null && owner.tickCount - owner.getLastHurtByMobTimestamp() <= IN_COMBAT;
	}

	/**
	 * What the familiar may harm: whatever its owner just struck or was struck by (another player
	 * only if the fight is theirs and harming them is allowed at all), or failing that the nearest
	 * monster hunting its owner. It has to be close and in sight.
	 */
	static LivingEntity target(ServerLevel level, Wisp wisp, ServerPlayer owner) {
		LivingEntity struck = owner.getLastHurtMob();
		if (struck != null && owner.tickCount - owner.getLastHurtMobTimestamp() <= IN_COMBAT && fair(wisp, owner, struck)) {
			return struck;
		}
		LivingEntity attacker = owner.getLastHurtByMob();
		if (attacker != null && owner.tickCount - owner.getLastHurtByMobTimestamp() <= IN_COMBAT && fair(wisp, owner, attacker)) {
			return attacker;
		}
		return level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(12), m -> m.getTarget() == owner && m instanceof Enemy && fair(wisp, owner, m))
			.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(owner))).orElse(null);
	}

	private static boolean fair(Wisp wisp, ServerPlayer owner, LivingEntity target) {
		return target.isAlive() && target != wisp && !(target instanceof Wisp) && target.level() == wisp.level()
			&& target.distanceTo(wisp) <= REACH && Targets.canHarm(owner, target) && wisp.hasLineOfSight(target);
	}
}
