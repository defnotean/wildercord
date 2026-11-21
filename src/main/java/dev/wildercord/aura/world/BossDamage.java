package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.Giants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Bosses hitting players harder (see {@link BossDamageRules}): every creature in the {@code wildercord:bosses} tag, and every giant
 * while it carries its bar. Its own blows and its projectiles count; what it summons fights at its own strength.
 */
public final class BossDamage {
	private BossDamage(){}
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Wildercord.id("bosses"));

	public static boolean boss(Entity entity) {
		return entity != null && (entity.getType().is(BOSSES) || entity instanceof Mob mob && Giants.bar(mob) != null);
	}

	private static boolean bossOnPlayer(LivingEntity victim, DamageSource source) {
		return victim instanceof Player && boss(source.getEntity());
	}

	/** What a blow of {@code damage} lands as on {@code victim}: harder if it is a boss's on a player. */
	public static float harm(LivingEntity victim, DamageSource source, float damage) {
		return bossOnPlayer(victim, source) ? BossDamageRules.scaled(damage) : damage;
	}

	/** What a defence leaves of a hit ({@code after} of {@code before}), with a landing guardian's or a boss's blow going partly through. */
	public static float pierce(LivingEntity victim, DamageSource source, float before, float after) {
		return GuardianBite.pierced(before, after, BossDamageRules.share(bossOnPlayer(victim, source), GuardianBite.share()));
	}
}
