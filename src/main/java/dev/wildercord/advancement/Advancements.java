package dev.wildercord.advancement;

import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Grants the Wildercord advancement tab. The places that already notice progress call in here:
 * the Grimoire when it gains an entry, a Heart Circle forming, the spellbook or the worn Cord
 * changing, and a few moments that leave no trace in the player's state (a cast, a passive, a
 * glyph going off). {@link #sync} fires every state criterion at once, so it also runs when a
 * player joins and after a data pack reload: anyone who met a goal before its advancement existed
 * gets it then. A feat or moment with no advancement yet does nothing at all.
 */
public final class Advancements {
	private Advancements() {}

	/** Moments: events that aren't Grimoire entries, for {@code wildercord:moment} criteria. */
	public static final String CAST = "cast";
	/** Cast a spell of this many runes or more. */
	public static final String LONG_CAST = "long_cast";
	public static final int LONG_CAST_RUNES = 6;
	public static final String PASSIVE = "passive";
	public static final String GLYPH = "glyph";
	public static final String RUNEBOUND_ADEPT = "runebound_adept";

	public static void init() {
		WildercordTriggers.init();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (success) {
				server.getPlayerList().getPlayers().forEach(Advancements::sync);
			}
		});
	}

	/** Fires every state criterion: feats, the Grimoire, circles, runes known and the worn Cord. */
	public static void sync(ServerPlayer player) {
		grimoire(player);
		circles(player);
		spellbook(player);
	}

	/** The Grimoire gained an entry (a feat, a reaction, a secret or a riddle). */
	public static void grimoire(ServerPlayer player) {
		List<String> entries = Heart.grimoire(player);
		WildercordTriggers.FEAT.trigger(player, entries);
		WildercordTriggers.GRIMOIRE.trigger(player, entries);
	}

	/** A Heart Circle formed (or was set). */
	public static void circles(ServerPlayer player) {
		WildercordTriggers.HEART_CIRCLE.trigger(player, Heart.circles(player));
	}

	/** The spellbook or the worn Cord changed. */
	public static void spellbook(ServerPlayer player) {
		WildercordTriggers.RUNES_KNOWN.trigger(player, Spellbooks.get(player).learned());
		WildercordTriggers.CORD.trigger(player, Spellbooks.tier(player));
	}

	public static void moment(ServerPlayer player, String moment) {
		WildercordTriggers.MOMENT.trigger(player, moment);
	}

	/** A spell was cast (not a passive): the first cast, and a long one. */
	public static void cast(ServerPlayer player, int runes) {
		moment(player, CAST);
		if (runes >= LONG_CAST_RUNES) {
			moment(player, LONG_CAST);
		}
	}
}
