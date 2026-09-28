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

	public static void set(Player player, Spellbook book) {
		player.setAttached(WildercordAttachments.SPELLBOOK, book);
		changed(player);
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
		player.setAttached(WildercordAttachments.CORD, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
		changed(player);
	}

	/** Runes learned or a Cord put on: the advancements for them (on the server). */
	private static void changed(Player player) {
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			dev.wildercord.advancement.Advancements.spellbook(serverPlayer);
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
		return readyAt - player.level().getGameTime() > MAX_COOLDOWN ? 0L : readyAt;
	}

	public static void setReadyAt(Player player, int spell, long gameTime) {
		List<Long> cooldowns = new ArrayList<>(player.getAttachedOrElse(WildercordAttachments.COOLDOWNS, List.of()));
		while (cooldowns.size() < CordTier.MAX_SPELLS) {
			cooldowns.add(0L);
		}
		cooldowns.set(spell, gameTime);
		player.setAttached(WildercordAttachments.COOLDOWNS, List.copyOf(cooldowns));
	}
}
