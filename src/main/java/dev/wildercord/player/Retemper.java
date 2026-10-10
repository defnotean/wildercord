package dev.wildercord.player;

import dev.wildercord.spell.Circles;
import dev.wildercord.spell.CondenseRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.12 "Tempering" measures every heart (and every aura) again the first time its player joins: circles that today's thresholds no longer pay
 * for are given back (the mana condensed is kept, so they form again as it grows). The player is told what changed.
 */
public final class Retemper {
	private Retemper() {}

	/** Bumped whenever a release re-measures saves. */
	public static final int VERSION = 1;

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> apply(handler.player)));
	}

	public static void apply(ServerPlayer player) {
		if (player.isRemoved() || player.getAttachedOrElse(WildercordAttachments.TEMPERED, 0) >= VERSION) {
			return;
		}
		player.setAttached(WildercordAttachments.TEMPERED, VERSION);
		int had = Heart.circles(player);
		int keeps = CondenseRules.retemper(had, Heart.condensed(player));
		if (keeps < had) {
			player.setAttached(WildercordAttachments.CIRCLES, keeps);
			Spellbooks.setMana(player, Math.min(Spellbooks.mana(player), Mana.max(player)));
		}
		TribulationScars.apply(player);
		int[] aura = dev.wildercord.aura.AuraExperience.retemper(player);
		if (aura[1] < aura[0]) {
			player.sendSystemMessage(Component.translatable("message.wildercord.retemper.aura",
				Component.translatable("aura.wildercord.stage." + dev.wildercord.aura.AuraStages.id(aura[0])),
				Component.translatable("aura.wildercord.stage." + dev.wildercord.aura.AuraStages.id(aura[1]))).withColor(0xE8D8B0));
		}
		if (had > 0) {
			player.sendSystemMessage(Component.translatable(keeps < had ? "message.wildercord.retemper.dropped" : "message.wildercord.retemper.kept",
				had, keeps, Circles.condenseNeeded(keeps + 1)).withColor(0xF5C46A));
		}
	}
}
