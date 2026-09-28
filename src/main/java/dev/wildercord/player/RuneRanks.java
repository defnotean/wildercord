package dev.wildercord.player;

import dev.wildercord.spell.Ranks;
import dev.wildercord.spell.RuneDef;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * A caster's rune ranks (see {@link Ranks}), read and written. Safe on both sides; writes belong on the
 * server. Monsters cast every rune at rank I.
 */
public final class RuneRanks {
	private RuneRanks() {}

	public static Map<String, Integer> all(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.RUNE_RANKS, Map.of());
	}

	public static int rank(LivingEntity caster, String runeId) {
		return caster instanceof Player player ? Ranks.clamp(all(player).getOrDefault(runeId, 1)) : 1;
	}

	/** The power a rank gives {@code rune} when {@code caster} casts it: 1, 1.25 or 1.5. */
	public static double power(LivingEntity caster, RuneDef rune) {
		return Ranks.power(rank(caster, rune.id()));
	}

	/** For the compiler's readout: this player's rank of every rune. */
	public static Ranks.Lookup lookup(Player player) {
		Map<String, Integer> ranks = all(player);
		return ranks.isEmpty() ? Ranks.Lookup.NONE : id -> Ranks.clamp(ranks.getOrDefault(id, 1));
	}

	/** Raises a rune to {@code rank}; never lowers it. Returns true if it went up. */
	public static boolean raise(Player player, String runeId, int rank) {
		if (rank <= rank(player, runeId)) {
			return false;
		}
		Map<String, Integer> next = new HashMap<>(all(player));
		next.put(runeId, Ranks.clamp(rank));
		player.setAttached(WildercordAttachments.RUNE_RANKS, Map.copyOf(next));
		return true;
	}
}
