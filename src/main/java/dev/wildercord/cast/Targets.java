package dev.wildercord.cast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Friendly fire is off by design: who counts as an ally, and who may be harmed. For a player
 * that's themselves, their pets, their party and their team (including their allies' pets). Another player may be
 * harmed only when the pvp game rule allows, or the two are duelling; another player's pets are as
 * safe as their owner, harmed only when the pvp game rule allows (and never by a duellist, who
 * harms nobody but their opponent). For a Runebound it's every other monster: its spells hit
 * players, their pets, golems and whatever it's hunting, never its own kind.
 */
public final class Targets {
	private Targets() {}

	public static boolean isAlly(LivingEntity caster, Entity entity) {
		if (entity == caster) {
			return true;
		}
		if (dev.wildercord.party.Parties.sameParty(caster, entity)) {
			return true;
		}
		if (!(caster instanceof Player)) {
			return entity instanceof Enemy && !playerPet(entity);
		}
		// Your pets, and your team's.
		if (entity instanceof OwnableEntity ownable) {
			LivingEntity owner = ownable.getRootOwner();
			if (owner == caster || owner instanceof Player && caster.isAlliedTo(owner)) {
				return true;
			}
		}
		return caster.isAlliedTo(entity);
	}

	/**
	 * Whether {@code entity} belongs to a player: tamed by one (a wolf, a cat, a horse, a spirit wolf
	 * summoned by a spell), whether or not they're online.
	 */
	public static boolean playerPet(Entity entity) {
		if (!(entity instanceof OwnableEntity ownable) || ownable.getOwnerReference() == null) {
			return false;
		}
		LivingEntity owner = ownable.getRootOwner();
		if (owner != null) {
			return owner instanceof Player;
		}
		// Its owner is away: only a player ever tames an animal or a horse.
		return entity instanceof TamableAnimal || entity instanceof AbstractHorse;
	}

	/** Harmful effects: living, not you, not an ally, and respecting the pvp game rule. */
	public static boolean canHarm(LivingEntity caster, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity instanceof ArmorStand) {
			return false;
		}
		// A master's redirected spell belongs to its opt-in encounter, including every linked hit.
		if (caster instanceof dev.wildercord.aura.world.SwordMaster master && !master.canHarmParticipant(entity)) {
			return false;
		}
		// Bystanders cannot add uncounted damage or posture/control pressure to an opted-in fight.
		if (entity instanceof dev.wildercord.aura.world.SwordMaster master && !master.acceptsHarmFrom(caster)) {
			return false;
		}
		// A duel overrides the rest: the two duellists may hurt each other, and neither may hurt another player.
		Boolean duel = dev.wildercord.duel.Duels.canHarm(caster, entity);
		if (duel != null) {
			return duel;
		}
		if (isAlly(caster, entity)) {
			return false;
		}
		if (entity instanceof Player player) {
			boolean pvp = !(caster instanceof Player) || ((ServerLevel) caster.level()).isPvpAllowed();
			return pvp && !player.isCreative() && !player.isSpectator();
		}
		if (!(caster instanceof Player)) {
			return playerPet(entity)
				|| entity instanceof AbstractGolem
				|| caster instanceof Mob mob && mob.getTarget() == entity
				|| entity instanceof net.minecraft.world.entity.decoration.Mannequin;
		}
		// Another player's pet is as safe as they are: only with pvp on.
		if (playerPet(entity)) {
			return ((ServerLevel) caster.level()).isPvpAllowed();
		}
		return true;
	}

	/** Helpful effects: only you and your allies. */
	public static boolean canHelp(LivingEntity caster, Entity entity) {
		return entity instanceof LivingEntity living && living.isAlive() && isAlly(caster, entity);
	}
}
