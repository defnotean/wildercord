package dev.wildercord.cast;

import dev.wildercord.config.Config;
import dev.wildercord.content.RuneItem;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneReading;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.WovenRunes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reading runes at runtime (see {@link RuneReading}): a rune learned starts unread, and each cast that holds it, and each
 * cast of it that lands on something, brings the caster closer to understanding it. Fed by the add-on events every cast
 * already raises ({@code AFTER_CAST}, {@code SPELL_HIT}), so the cast itself doesn't change.
 */
public final class RuneReadings {
	private RuneReadings() {}

	/** Each player's last cast: its runes (Knots and weaves untied), and which have already been credited for landing. */
	private record Recent(Set<String> runes, Set<String> landed) {}

	private static final Map<UUID, Recent> RECENT = new HashMap<>();

	public static void init() {
		dev.wildercord.api.WildercordEvents.AFTER_CAST.register((player, spell, runes, spent) -> cast(player, runes));
		dev.wildercord.api.WildercordEvents.SPELL_HIT.register((caster, targets, point, effects) -> {
			if (caster instanceof ServerPlayer player && !targets.isEmpty()) {
				landed(player);
			}
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RECENT.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> RECENT.clear());
	}

	/** Whether newly learned runes start unread on this server. */
	public static boolean enabled() {
		return Config.get().unreadRunes();
	}

	/** {@code player} has just learned {@code rune} for the first time: it starts unread (with the mechanic on). */
	public static void learned(ServerPlayer player, RuneDef rune) {
		if (!enabled() || !RuneReading.tracked(rune)) {
			return;
		}
		Map<String, Integer> reading = new HashMap<>(reading(player));
		if (reading.putIfAbsent(rune.id(), 0) == null) {
			player.setAttached(WildercordAttachments.RUNE_READING, Map.copyOf(reading));
		}
	}

	/** What {@code player} is still reading (rune id to progress); every rune not in it is understood. Both sides. */
	public static Map<String, Integer> reading(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.RUNE_READING, Map.of());
	}

	/** Where {@code rune} stands for {@code player} (always understood with the mechanic off, or for a creative player). Both sides. */
	public static RuneReading.Stage stage(Player player, RuneDef rune) {
		if (player == null || player.isCreative() || !Config.unreadRunes(player)) {
			return RuneReading.Stage.UNDERSTOOD;
		}
		dev.wildercord.player.Spellbook book = dev.wildercord.player.Spellbooks.get(player);
		return RuneReading.stage(rune, book::knows, reading(player));
	}

	private static void cast(ServerPlayer player, List<RuneDef> runes) {
		Set<String> ids = new LinkedHashSet<>();
		for (RuneDef rune : Knots.flatten(runes)) {
			if (WovenRunes.isWoven(rune)) {
				WovenRunes.contents(rune).forEach(part -> ids.add(part.id()));
			} else {
				ids.add(rune.id());
			}
		}
		RECENT.put(player.getUUID(), new Recent(ids, new HashSet<>()));
		credit(player, ids, RuneReading.CAST);
	}

	/** One of the player's spells landed on something: each rune of their last cast is credited once for it. */
	private static void landed(ServerPlayer player) {
		Recent recent = RECENT.get(player.getUUID());
		if (recent == null) {
			return;
		}
		Set<String> fresh = new LinkedHashSet<>();
		for (String id : recent.runes()) {
			if (recent.landed().add(id)) {
				fresh.add(id);
			}
		}
		credit(player, fresh, RuneReading.LANDED);
	}

	private static void credit(ServerPlayer player, Set<String> ids, int amount) {
		Map<String, Integer> reading = reading(player);
		if (reading.isEmpty() || ids.isEmpty()) {
			return;
		}
		Map<String, Integer> next = new HashMap<>(reading);
		boolean changed = false;
		for (String id : ids) {
			Integer progress = next.get(id);
			if (progress == null) {
				continue;
			}
			RuneReading.Stage before = RuneReading.stage(progress);
			int now = RuneReading.add(progress, amount);
			RuneReading.Stage after = RuneReading.stage(now);
			changed = true;
			if (after == RuneReading.Stage.UNDERSTOOD) {
				// Understood: nothing more to keep for it.
				next.remove(id);
			} else {
				next.put(id, now);
			}
			if (after != before) {
				told(player, id, after);
			}
		}
		if (changed) {
			player.setAttached(WildercordAttachments.RUNE_READING, Map.copyOf(next));
		}
	}

	/** A rune glimpsed, or understood: a word to the caster. */
	private static void told(ServerPlayer player, String id, RuneReading.Stage stage) {
		RuneDef rune = Runes.get(id).orElse(null);
		if (rune == null) {
			return;
		}
		Component name = RuneItem.runeName(rune).withColor(RuneColors.of(rune));
		if (stage == RuneReading.Stage.GLIMPSED) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.rune_glimpsed", name).withStyle(ChatFormatting.GRAY));
		} else if (stage == RuneReading.Stage.UNDERSTOOD) {
			player.sendSystemMessage(Component.translatable("message.wildercord.rune_understood", name).withColor(0xC8D8F0));
			Fx.sound(player.level(), player.position(), SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.25F);
		}
	}
}
