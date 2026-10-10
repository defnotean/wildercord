package dev.wildercord.player;

import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneTwistRules;
import dev.wildercord.spell.RuneTwistRules.Twist;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A caster's twisted runes (see {@link RuneTwistRules}), read and written. Safe on both sides; writes belong on the server. */
public final class RuneTwists {
	private RuneTwists() {}

	public static Map<String, String> all(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.RUNE_TWISTS, Map.of());
	}

	/** The twist {@code caster} has cut into {@code runeId}, or null. Monsters' runes are never twisted. */
	public static Twist twist(LivingEntity caster, String runeId) {
		return caster instanceof Player player ? Twist.byId(all(player).get(runeId)) : null;
	}

	/** Twists a rune, or untwists it with null. */
	public static void set(Player player, String runeId, Twist twist) {
		Map<String, String> next = new HashMap<>(all(player));
		if (twist == null) next.remove(runeId);
		else next.put(runeId, twist.id());
		player.setAttached(WildercordAttachments.RUNE_TWISTS, Map.copyOf(next));
	}

	/** The power factor {@code caster}'s twist gives {@code rune}. */
	public static double power(LivingEntity caster, RuneDef rune) {
		return RuneTwistRules.power(twist(caster, rune.id()));
	}

	/** The factor on a spell's price from the flawed runes in it (each counted once, Knots opened). */
	public static double costFactor(Player player, List<RuneDef> runes) {
		Map<String, String> twists = all(player);
		if (twists.isEmpty() || runes.isEmpty()) return 1.0;
		Set<String> flawed = new HashSet<>();
		for (RuneDef rune : Knots.flatten(runes)) {
			if (Twist.FLAWED.id().equals(twists.get(rune.id()))) flawed.add(rune.id());
		}
		return RuneTwistRules.costFactor(flawed.size());
	}
}
