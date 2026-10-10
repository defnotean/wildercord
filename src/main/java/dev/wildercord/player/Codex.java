package dev.wildercord.player;

import dev.wildercord.spell.CodexRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * The codex bestiary (0.13; the rules are {@link CodexRules}): every field-guide creature a player kills is tallied on
 * them, and the Grimoire's field guide shows the count and what it has taught them. A new rank is told in chat.
 */
public final class Codex {
	private Codex() {}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && !(entity instanceof ServerPlayer)) slain(player, entity);
		});
	}

	public static Map<String, Integer> tally(net.minecraft.world.entity.player.Player player) {
		return player.getAttachedOrElse(WildercordAttachments.CODEX, Map.of());
	}

	public static int kills(net.minecraft.world.entity.player.Player player, String type) {
		return tally(player).getOrDefault(type, 0);
	}

	/** {@code player} slew {@code entity}: its kind's tally goes up, if it's in the field guide. */
	public static void slain(ServerPlayer player, LivingEntity entity) {
		String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		Map<String, Integer> before = tally(player);
		Map<String, Integer> after = CodexRules.slay(before, type);
		if (after == before) return;
		player.setAttached(WildercordAttachments.CODEX, after);
		int was = before.getOrDefault(type, 0), now = after.get(type);
		if (now > 1 && CodexRules.rankedUp(was, now)) {
			CodexRules.Rank rank = CodexRules.rank(now);
			player.sendSystemMessage(Component.translatable("message.wildercord.codex." + rank.key, entity.getType().getDescription(), now)
				.withStyle(rank == CodexRules.Rank.MASTERED ? ChatFormatting.GOLD : ChatFormatting.AQUA));
		}
	}
}
