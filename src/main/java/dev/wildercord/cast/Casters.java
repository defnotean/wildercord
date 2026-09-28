package dev.wildercord.cast;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spells are cast by players and by Runebound monsters alike. What only makes sense for a
 * player (messages, building rights, creative mode, reach) is answered here, with sensible
 * answers for a monster: it hears nothing, never changes the world and has an arm's reach.
 */
public final class Casters {
	private Casters() {}

	/** The caster as a player, or null for a monster. */
	public static ServerPlayer player(LivingEntity caster) {
		return caster instanceof ServerPlayer player ? player : null;
	}

	/** A line above the hotbar; monsters don't read. */
	public static void tell(LivingEntity caster, Component message) {
		if (caster instanceof ServerPlayer player) {
			player.sendOverlayMessage(message);
		}
	}

	/** Whether the caster may change blocks. Monsters never do, so a Runebound can't grief. */
	public static boolean mayBuild(LivingEntity caster) {
		return caster instanceof ServerPlayer player && player.mayBuild();
	}

	public static boolean creative(LivingEntity caster) {
		return caster instanceof ServerPlayer player && player.isCreative();
	}

	public static double entityReach(LivingEntity caster) {
		return caster instanceof ServerPlayer player ? player.entityInteractionRange() : 3.0;
	}

	public static double blockReach(LivingEntity caster) {
		return caster instanceof ServerPlayer player ? player.blockInteractionRange() : 4.5;
	}
}
