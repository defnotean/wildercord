package dev.wildercord.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import org.jspecify.annotations.Nullable;

/**
 * Version seam for Minecraft entity, player, hand-swing, hurt-cooldown and Enderman APIs (Minecraft 26.3).
 *
 * <p>All helpers here are stateless; only this package adapts 26.2 vs 26.3 entity changes.
 */
public final class McEntities {
	private McEntities() {}

	/** Version-independent snapshot of hurt-cooldown state around an extra damage hit. */
	public record HurtCooldown(int invulnerableTime, int damageCooldownTime) {}

	/** Version-independent callback for custom projectile deflection. */
	@FunctionalInterface
	public interface ProjectileRedirect {
		void deflect(Projectile turned, @Nullable Entity by, RandomSource random);
	}

	/** Drops {@code stack} from {@code player} under server authority. */
	public static @Nullable ItemEntity drop(Player player, ItemStack stack, boolean dropAround) {
		return player.drop(stack, dropAround, Prediction.SERVER_ONLY);
	}

	/** Adds {@code stack} to {@code player}'s inventory or drops any remainder at their feet. */
	public static void giveOrDrop(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			drop(player, stack, false);
		}
	}

	/** Drops {@code stack} on death/overflow without awarding thrower attribution or failing void drops. */
	public static void dropSilently(Player player, ItemStack stack) {
		ItemEntity drop = player.createItemStackToDrop(stack, true, false);
		if (drop != null) {
			player.level().addFreshEntity(drop);
		}
	}

	/** Returns {@code stack} to {@code player}'s inventory or drops excess cleanly. */
	public static void placeItemBackInInventory(Player player, ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
	}

	/** Swings {@code hand} with vanilla's default swing animation when required. */
	public static void swing(LivingEntity entity, InteractionHand hand, boolean sendToSource) {
		entity.swing(hand, SwingAnimation.DEFAULT, sendToSource);
	}

	/** Whether {@code entity} is currently mid-swing. */
	public static boolean isSwinging(LivingEntity entity) {
		return entity.isSwinging();
	}

	/** Current normalised swing progress for {@code entity}. */
	public static float swingProgress(LivingEntity entity, float partialTick) {
		return entity.getSwingAnimation(partialTick);
	}

	/** Marks an entity so velocity changes sync to tracking clients on the next tick. */
	public static void syncVelocity(Entity entity) {
		entity.syncVelocity = true;
	}

	/** Enables or clears permanent invulnerability on {@code entity}. */
	public static void setPermanentlyInvulnerable(Entity entity, boolean invulnerable) {
		entity.setPermanentlyInvulnerable(invulnerable);
	}

	/** Whether {@code entity} is permanently invulnerable. */
	public static boolean isPermanentlyInvulnerable(Entity entity) {
		return entity.isPermanentlyInvulnerable();
	}

	/** Captures hurt cooldown fields so callers can temporarily bypass i-frames and restore them. */
	public static HurtCooldown saveHurtCooldown(Entity entity) {
		return new HurtCooldown(entity.getInvulnerableTime(), entity instanceof LivingEntity living ? living.damageCooldownTime : 0);
	}

	/** Restores a previously captured {@link HurtCooldown}. */
	public static void restoreHurtCooldown(Entity entity, HurtCooldown saved) {
		entity.setInvulnerableTime(saved.invulnerableTime());
		if (entity instanceof LivingEntity living) {
			living.damageCooldownTime = saved.damageCooldownTime();
		}
	}

	/** Clears hurt cooldown i-frames so an immediate follow-up hit can apply. */
	public static void clearHurtCooldown(Entity entity) {
		entity.setInvulnerableTime(0);
		if (entity instanceof LivingEntity living) {
			living.damageCooldownTime = 0;
		}
	}

	/** Remaining hurt-cooldown ticks on {@code living}. */
	public static int damageCooldownTicks(LivingEntity living) {
		return living.damageCooldownTime;
	}

	/** Whether {@code entity} is an Enderman (handles {@code EnderMan} vs {@code Enderman}). */
	public static boolean isEnderman(@Nullable Entity entity) {
		return entity instanceof Enderman;
	}

	/** Whether {@code mob} is an Enderman currently angered at {@code player}. */
	public static boolean isEndermanAngryAt(Mob mob, Player player, ServerLevel level) {
		return mob instanceof Enderman man && man.isAngryAt(player, level);
	}

	/** Stops an Enderman's anger state and clears its current target. */
	public static void pacifyEnderman(Mob mob) {
		if (mob instanceof Enderman man) {
			man.stopBeingAngry();
		}
		mob.setTarget(null);
	}

	/** Adapts a 3-argument redirect callback to the active {@link ProjectileDeflection} signature. */
	public static ProjectileDeflection projectileDeflection(ProjectileRedirect redirect) {
		return (turned, by, random, power) -> redirect.deflect(turned, by, random);
	}
}
