package dev.wildercord.cast;

import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Bestiary;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Secrets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes discoveries into a player's Grimoire. The first time for each entry, a little mana
 * condenses toward the next Heart Circle and the client shows a Grimoire toast (Bestiary entries for a
 * creature met or a resistance found go in quietly: see {@link Bestiary#quiet}; an affinity's first level
 * has a toast of its own).
 */
public final class Grimoire {
	private Grimoire() {}

	/** Adds an entry; returns true if it was new. */
	public static boolean unlock(ServerPlayer player, String key) {
		return unlock(player, key, true);
	}

	/**
	 * Adds an entry; returns true if it was new.
	 *
	 * @param announce whether it gets the page's chime and a toast: an affinity's first level brings its own
	 *                 (see {@link PlayerAffinities}), so it only condenses its mana here
	 */
	public static boolean unlock(ServerPlayer player, String key, boolean announce) {
		List<String> entries = Heart.grimoire(player);
		if (entries.contains(key)) {
			return false;
		}
		List<String> next = new ArrayList<>(entries);
		next.add(key);
		player.setAttached(WildercordAttachments.GRIMOIRE, List.copyOf(next));
		dev.wildercord.advancement.Advancements.grimoire(player);
		// A weakness found feeds that element's affinity, a riddle read arcane.
		PlayerAffinities.discovered(player, key);
		// A creature met, or a resistance found, goes in quietly: the callout over the creature already said it.
		if (Bestiary.quiet(key)) {
			return true;
		}
		if (!key.startsWith("hint:")) {
			player.setAttached(WildercordAttachments.CONDENSED, Heart.condensed(player) + Feats.reward(key));
			if (announce) {
				Fx.sound(player.level(), player.position(), SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.1F);
				Fx.sound(player.level(), player.position(), dev.wildercord.content.WildercordSounds.DISCOVERY, 0.9F, 1.0F);
			}
		}
		if (announce) {
			ServerPlayNetworking.send(player, new WildercordNetworking.Discovery(key));
		}
		return true;
	}

	public static boolean feat(LivingEntity who, String feat) {
		return who instanceof ServerPlayer player && unlock(player, "feat:" + feat);
	}

	/** Called whenever a reaction goes off: the caster learns its name, and its elements' affinities grow. */
	public static void reaction(LivingEntity caster, String reaction) {
		if (caster instanceof ServerPlayer player && Feats.REACTIONS.contains(reaction)) {
			unlock(player, Feats.reactionKey(reaction));
			PlayerAffinities.reaction(player, reaction);
		}
	}

	/** Whether a secret is left that {@code player} hasn't found or read the riddle of. */
	public static boolean hintsLeft(ServerPlayer player) {
		for (Secrets.Secret secret : Secrets.ALL) {
			if (!Heart.discovered(player, secret.key()) && !Heart.discovered(player, "hint:" + secret.id())) {
				return true;
			}
		}
		return false;
	}

	/** A Torn Page: the riddle of a secret not found or hinted yet, or null if none are left. */
	public static Secrets.Secret hint(ServerPlayer player) {
		List<Secrets.Secret> left = new ArrayList<>();
		for (Secrets.Secret secret : Secrets.ALL) {
			if (!Heart.discovered(player, secret.key()) && !Heart.discovered(player, "hint:" + secret.id())) {
				left.add(secret);
			}
		}
		if (left.isEmpty()) {
			return null;
		}
		Secrets.Secret secret = left.get(player.getRandom().nextInt(left.size()));
		unlock(player, "hint:" + secret.id());
		return secret;
	}
}
