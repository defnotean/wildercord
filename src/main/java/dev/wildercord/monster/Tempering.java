package dev.wildercord.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.cast.DungeonBoss;
import dev.wildercord.cast.Spirits;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Tempers each hostile creature to the players around it when it first comes into the world ({@link TemperingRules}): more
 * health, harder blows (scaled where they land on a player, so arrows, blasts and spells grow too), a chance to be an elite, and
 * a boss that grows with its challengers. What it was tempered to is kept in its tags, so a chunk loading again changes nothing.
 */
public final class Tempering {
	private Tempering() {}

	public static final String TAG = "wildercord.tempered";
	public static final String THREAT_TAG = "wildercord.threat.";
	public static final String ELITE_TAG = "wildercord.elite.";
	private static final Identifier HEALTH = Wildercord.id("tempered_health");
	private static final Identifier ELITE_SPEED = Wildercord.id("elite_speed");
	private static final Identifier ELITE_ARMOUR = Wildercord.id("elite_armour");
	private static final Identifier ELITE_TOUGHNESS = Wildercord.id("elite_toughness");
	private static final Identifier ELITE_KNOCKBACK = Wildercord.id("elite_knockback");
	private static final Identifier SPLIT_HEALTH = Wildercord.id("split_health");
	private static final Identifier ENRAGE_SPEED = Wildercord.id("enrage_speed");
	private static final Identifier ENRAGE_DAMAGE = Wildercord.id("enrage_damage");
	public static final String ENRAGED_TAG = "wildercord.enraged";

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && !entity.entityTags().contains(TAG)) {
				temper(mob, level);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage > 0 && source.getEntity() instanceof Mob attacker && attacker.isAlive()
					&& attacker.entityTags().contains(TemperingRules.Elite.VAMPIRIC.tag())) {
				attacker.heal(damage * (float) TemperingRules.VAMPIRIC_DRAIN);
			}
			if (entity instanceof Mob boss && entity.isAlive() && phaseless(boss) && !boss.entityTags().contains(ENRAGED_TAG)
					&& TemperingRules.enrages(boss.getHealth(), boss.getMaxHealth())) {
				enrage(boss);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level && elite(entity) != null) {
				ExperienceOrb.award(level, entity.position(), TemperingRules.eliteExperience(threat(entity)));
				if (elite(entity) == TemperingRules.Elite.SPLITTING && entity instanceof Mob mob) split(mob, level);
			}
		});
	}

	/** Whether {@code mob} is one the world tempers: hostile, wild, and not a summoned or tamed thing. */
	static boolean temperable(Mob mob) {
		if (!(mob instanceof Enemy) || mob.isNoAi()) return false;
		return !(mob instanceof OwnableEntity owned && owned.getOwnerReference() != null);
	}

	private static void temper(Mob mob, ServerLevel level) {
		mob.addTag(TAG);
		if (!temperable(mob)) return;
		int threat = 0;
		int party = 0;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double d = player.distanceToSqr(mob);
			if (d <= TemperingRules.REACH * TemperingRules.REACH) {
				threat = Math.max(threat, TemperingRules.threat(Heart.circles(player), Aura.stage(player)));
			}
			if (d <= TemperingRules.PARTY_REACH * TemperingRules.PARTY_REACH) party++;
		}
		mob.addTag(THREAT_TAG + threat);
		double health = TemperingRules.health(threat);
		if (Spirits.isBoss(mob) || mob instanceof DungeonBoss) {
			health *= TemperingRules.bossParty(party);
		} else if (mob.getRandom().nextDouble() < TemperingRules.eliteChance(threat)) {
			TemperingRules.Elite[] kinds = TemperingRules.Elite.values();
			TemperingRules.Elite elite = kinds[mob.getRandom().nextInt(kinds.length)];
			mob.addTag(elite.tag());
			health *= TemperingRules.ELITE_HEALTH;
			switch (elite) {
				case SWIFT -> modify(mob, Attributes.MOVEMENT_SPEED, ELITE_SPEED, TemperingRules.SWIFT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
				case IRONHIDE -> {
					modify(mob, Attributes.ARMOR, ELITE_ARMOUR, TemperingRules.IRONHIDE_ARMOUR, AttributeModifier.Operation.ADD_VALUE);
					modify(mob, Attributes.ARMOR_TOUGHNESS, ELITE_TOUGHNESS, TemperingRules.IRONHIDE_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE);
				}
				case BRUTAL -> modify(mob, Attributes.ATTACK_KNOCKBACK, ELITE_KNOCKBACK, TemperingRules.BRUTAL_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE);
				case VAMPIRIC, SPLITTING -> { }
			}
			if (!mob.hasCustomName()) {
				mob.setCustomName(Component.translatableWithFallback("monster.wildercord.elite." + elite.key(), elite.title + " %s", mob.getType().getDescription())
					.withColor(elite.color));
			}
		}
		if (health > 1.0001 && modify(mob, Attributes.MAX_HEALTH, HEALTH, health - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
			mob.setHealth(mob.getMaxHealth());
		}
	}

	/** A boss with no phases of its own: these get the one second phase the tempering gives them. */
	static boolean phaseless(Mob mob) {
		return mob instanceof dev.wildercord.aura.world.Gravekeeper
			|| mob.getType() == net.minecraft.world.entity.EntityTypes.WARDEN
			|| mob.getType() == net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN;
	}

	/** Its second phase: faster, harder, a shield of absorption, and everyone near is told. */
	private static void enrage(Mob boss) {
		boss.addTag(ENRAGED_TAG);
		modify(boss, Attributes.MOVEMENT_SPEED, ENRAGE_SPEED, TemperingRules.ENRAGE_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modify(boss, Attributes.ATTACK_DAMAGE, ENRAGE_DAMAGE, TemperingRules.ENRAGE_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 20 * 30,
			Math.max(0, (int) Math.round(boss.getMaxHealth() * TemperingRules.ENRAGE_SHIELD / 4) - 1)));
		if (boss.level() instanceof ServerLevel level) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ(),
				16, boss.getBbWidth() * 0.6, boss.getBbHeight() * 0.4, boss.getBbWidth() * 0.6, 0);
			Component message = Component.translatable("message.wildercord.boss.enraged", boss.getDisplayName()).withColor(0xFF5A3A);
			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(boss) <= 64 * 64) player.sendSystemMessage(message);
			}
		}
	}

	/** A Splitting elite's death: lesser copies, already tempered (so never elites themselves), with half their kind's health. */
	private static void split(Mob mob, ServerLevel level) {
		for (int i = 0; i < TemperingRules.SPLIT_COUNT; i++) {
			if (!(mob.getType().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) instanceof Mob copy)) return;
			copy.addTag(TAG);
			copy.addTag(THREAT_TAG + threat(mob));
			double angle = Math.PI * 2 * i / TemperingRules.SPLIT_COUNT;
			copy.snapTo(mob.getX() + Math.cos(angle) * 0.6, mob.getY(), mob.getZ() + Math.sin(angle) * 0.6, mob.getYRot(), 0);
			double health = TemperingRules.health(threat(mob)) * TemperingRules.SPLIT_HEALTH;
			if (modify(copy, Attributes.MAX_HEALTH, SPLIT_HEALTH, health - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
				copy.setHealth(copy.getMaxHealth());
			}
			if (mob.getTarget() != null) copy.setTarget(mob.getTarget());
			level.addFreshEntity(copy);
		}
	}

	private static boolean modify(Mob mob, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance == null) return false;
		instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
		return true;
	}

	/** The threat {@code entity} was tempered to (0 if none). */
	public static int threat(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(THREAT_TAG)) {
				try {
					return Integer.parseInt(tag.substring(THREAT_TAG.length()));
				} catch (NumberFormatException ignored) {
					return 0;
				}
			}
		}
		return 0;
	}

	/** {@code entity}'s elite kind, or null. */
	public static TemperingRules.Elite elite(Entity entity) {
		for (TemperingRules.Elite elite : TemperingRules.Elite.values()) {
			if (entity.entityTags().contains(elite.tag())) return elite;
		}
		return null;
	}

	/** What a blow from {@code source} on a player becomes: a tempered creature's harder blow. */
	public static float onPlayer(DamageSource source, float amount) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker instanceof Player || !attacker.entityTags().contains(TAG)) {
			return amount;
		}
		double k = TemperingRules.damage(threat(attacker));
		if (elite(attacker) == TemperingRules.Elite.BRUTAL) k *= TemperingRules.BRUTAL_DAMAGE;
		return (float) (amount * k);
	}
}
