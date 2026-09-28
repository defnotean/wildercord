package dev.wildercord.cast;

import dev.wildercord.content.CordTier;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps each player's {@link WildercordAttachments.CordLook} up to date, so everyone around sees
 * the Cord on their wrist: its tier, and a glowing bead for each rune of the spell they have ready.
 * Checked twice a second; only a change is sent.
 */
public final class CordLook {
	private CordLook() {}

	/** Beads drawn at most. */
	public static final int MAX_BEADS = 8;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				WildercordAttachments.CordLook now = lookOf(player);
				if (!now.equals(player.getAttachedOrElse(WildercordAttachments.CORD_LOOK, WildercordAttachments.CordLook.NONE))) {
					player.setAttached(WildercordAttachments.CORD_LOOK, now);
				}
			}
		});
	}

	static WildercordAttachments.CordLook lookOf(ServerPlayer player) {
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return WildercordAttachments.CordLook.NONE;
		}
		Spellbook book = Spellbooks.get(player);
		List<Integer> beads = new ArrayList<>();
		if (dev.wildercord.gear.Gear.spellOpen(player, tier, book.selected())) {
			for (RuneDef rune : SpellCaster.activeRunes(book, book.selected(), tier)) {
				if (beads.size() < MAX_BEADS) {
					beads.add(RuneColors.of(rune));
				}
			}
		}
		return new WildercordAttachments.CordLook(tier.key, List.copyOf(beads));
	}
}
