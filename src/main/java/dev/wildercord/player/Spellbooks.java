package dev.wildercord.player;

import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Reads and writes a player's Wildercord state. Safe on both sides; writes belong on the server. */
public final class Spellbooks {
	private Spellbooks() {}

	public static Spellbook get(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.SPELLBOOK, Spellbook.EMPTY);
	}

	/** Saves the spellbook (and syncs it) only if something changed; the runes-known advancements only if the runes did. */
	public static void set(Player player, Spellbook book) {
		Spellbook old = get(player);
		if (old.equals(book)) {
			return;
		}
		if (player instanceof net.minecraft.server.level.ServerPlayer p
			&& (old.selected() != book.selected() || !old.spells().equals(book.spells()))) dev.wildercord.cast.RelayCircles.cancel(p);
		player.setAttached(WildercordAttachments.SPELLBOOK, book);
		if (!old.learned().equals(book.learned()) && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			dev.wildercord.advancement.Advancements.runesKnown(serverPlayer);
		}
	}

	public static boolean knows(Player player, String runeId) {
		return get(player).knows(runeId);
	}

	public static void learn(Player player, String runeId) {
		set(player, get(player).learn(runeId));
	}

	public static ItemStack cord(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.CORD, ItemStack.EMPTY);
	}

	public static void setCord(Player player, ItemStack stack) {
		if (player instanceof net.minecraft.server.level.ServerPlayer p) dev.wildercord.cast.RelayCircles.cancel(p);
		player.setAttached(WildercordAttachments.CORD, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			dev.wildercord.advancement.Advancements.cord(serverPlayer);
		}
	}

	/** The worn Cord's tier, or null when no Cord is worn. */
	public static CordTier tier(Player player) {
		return WildercordItems.tierOf(cord(player));
	}

	public static float mana(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.MANA, 0.0F);
	}

	public static void setMana(Player player, float mana) {
		player.setAttached(WildercordAttachments.MANA, mana);
	}

	/** No cooldown is longer than this: a saved one further off came from another world's clock. */
	private static final long MAX_COOLDOWN = 20L * 60 * 10;

	public static long readyAt(Player player, int spell) {
		List<Long> cooldowns = player.getAttachedOrElse(WildercordAttachments.COOLDOWNS, List.of());
		long readyAt = spell >= 0 && spell < cooldowns.size() ? cooldowns.get(spell) : 0L;
		if (spell >= 0 && spell < dev.wildercord.gear.SpellSlots.ALL
			&& dev.wildercord.spell.RelayRules.containsIds(get(player).spells().get(spell))) {
			long clock = player instanceof net.minecraft.server.level.ServerPlayer p ? dev.wildercord.cast.RelayCircles.now(p) : player.level().getGameTime();
			long sharedLeft = Math.clamp(player.getAttachedOrElse(dev.wildercord.cast.RelayState.REST, 0L) - clock, 0, dev.wildercord.spell.RelayRules.REST_TICKS);
			readyAt = Math.max(readyAt, player.level().getGameTime() + sharedLeft);
		}
        if (spell >= 0 && spell < dev.wildercord.gear.SpellSlots.ALL && dev.wildercord.spell.ReweaveRules.containsIds(get(player).spells().get(spell))) {
            long clock = player instanceof net.minecraft.server.level.ServerPlayer p ? dev.wildercord.cast.ReweaveFields.now(p) : player.level().getGameTime();
            long left = Math.clamp(player.getAttachedOrElse(dev.wildercord.cast.ReweaveState.REST, 0L) - clock, 0, dev.wildercord.spell.ReweaveRules.REST_TICKS);
            readyAt = Math.max(readyAt, player.level().getGameTime() + left);
        }
		return readyAt - player.level().getGameTime() > MAX_COOLDOWN ? 0L : readyAt;
	}

	public static void setReadyAt(Player player, int spell, long gameTime) {
		List<Long> cooldowns = new ArrayList<>(player.getAttachedOrElse(WildercordAttachments.COOLDOWNS, List.of()));
		while (cooldowns.size() < dev.wildercord.gear.SpellSlots.ALL) {
			cooldowns.add(0L);
		}
		cooldowns.set(spell, gameTime);
		player.setAttached(WildercordAttachments.COOLDOWNS, List.copyOf(cooldowns));
	}
}
