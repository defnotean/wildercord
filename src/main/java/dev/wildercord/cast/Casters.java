package dev.wildercord.cast;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

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

	/**
	 * Whether the caster may change blocks. Monsters never do, so a Runebound can't grief, and nobody's
	 * spells do on a server that turned casting.spells_edit_blocks off.
	 */
	public static boolean mayBuild(LivingEntity caster) {
		return caster instanceof ServerPlayer player && player.mayBuild() && dev.wildercord.config.Config.get().spellsEditBlocks();
	}

	/**
	 * Whether a spell of the caster's may change the block at {@code pos}: they may build, it isn't
	 * spawn-protected or past the world border, and claim and protection mods agree, whether it takes
	 * something away or puts something into the air there (a Glimmer's lichen, a Glowvine, an Ancient
	 * Seed's flower). The change is offered to them as the player breaking that block: there's no event
	 * for placing to ask, and claim mods answer a break by where it is.
	 */
	public static boolean mayEdit(LivingEntity caster, ServerLevel level, BlockPos pos) {
		if (!(caster instanceof ServerPlayer player) || !mayBuild(player) || !level.mayInteract(player, pos)) {
			return false;
		}
		// A spell's own passing blocks (a Span's glass, a Rampart) are never edited, and never offered as a break:
		// the break handlers that take them down would run on a mere question (a Grow nearby, a Collect over them).
		if (Effects.isTemporary(level, pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
	}

	/** How deeply the current thread is inside {@link #probeBreak}; unset outside it. */
	private static final ThreadLocal<Integer> PROBING = new ThreadLocal<>();

	/**
	 * Asks the break callbacks about a block that is only read, or changed in place, and not broken: a
	 * survey of footing, a harvest that leaves the root. Claim mods answer as they would for a break, while
	 * Wildercord's own break handlers (dungeon wards forgetting a placed block, a glyph going off at the
	 * breaker) see {@link #probing()} and only answer, since nothing is actually being taken down.
	 */
	public static boolean probeBreak(ServerLevel level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state,
		net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		Integer outer = PROBING.get();
		PROBING.set(outer == null ? 1 : outer + 1);
		try {
			return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity);
		} finally {
			if (outer == null) {
				PROBING.remove();
			} else {
				PROBING.set(outer);
			}
		}
	}

	/** Whether the break callbacks are being asked by {@link #probeBreak}, not run for a real break. */
	public static boolean probing() {
		return PROBING.get() != null;
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
