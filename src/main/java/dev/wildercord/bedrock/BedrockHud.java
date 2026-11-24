package dev.wildercord.bedrock;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.Clashes;
import dev.wildercord.aura.Unity;
import dev.wildercord.aura.world.WeaversSilk;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.net.ServerDisconnects;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Passives;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.travel.Waypoints;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compact actionbar HUD for Bedrock/Console players (since client-side Java HUD overlays do not
 * travel over Geyser). Displays Mana, Active Spell, Aura Pool/Stage, Coating, Silk, Passives,
 * Unity, and Clash prompts every 10 ticks when enabled.
 */
public final class BedrockHud {
	private static final String[] COMPASS_DIRS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
	private static final Set<UUID> DISABLED = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, Waypoints.Track> TRACKED_WAYPOINTS = new ConcurrentHashMap<>();
	private static int tickCounter = 0;
	private static volatile boolean initialized = false;

	private BedrockHud() {}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if ((++tickCounter % 10) != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.connection != null && BedrockPlayers.isBedrock(player) && !DISABLED.contains(player.getUUID())) {
					sendHud(player);
				}
			}
		});
		ServerDisconnects.register((handler, server) -> TRACKED_WAYPOINTS.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			tickCounter = 0;
			TRACKED_WAYPOINTS.clear();
		});
	}

	public static void setTrackedWaypoint(UUID playerId, Waypoints.Track track) {
		if (playerId == null) {
			return;
		}
		if (track == null || track.none()) {
			TRACKED_WAYPOINTS.remove(playerId);
		} else {
			TRACKED_WAYPOINTS.put(playerId, track);
		}
	}

	public static String compassDirection(double dx, double dz) {
		double deg = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0) % 360.0;
		return COMPASS_DIRS[((int) Math.round(deg / 45.0)) % COMPASS_DIRS.length];
	}

	public static boolean isEnabled(ServerPlayer player) {
		return player != null && !DISABLED.contains(player.getUUID());
	}

	public static boolean toggle(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean enabled;
		if (DISABLED.contains(id)) {
			DISABLED.remove(id);
			enabled = true;
		} else {
			DISABLED.add(id);
			enabled = false;
		}
		player.sendSystemMessage(Component.literal(enabled ? "[Wildercord] Bedrock HUD enabled." : "[Wildercord] Bedrock HUD hidden.")
			.withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY));
		return enabled;
	}

	private static void sendHud(ServerPlayer player) {
		CordTier tier = Spellbooks.tier(player);
		int stage = Aura.stage(player);
		boolean clashing = Clashes.clashing(player);
		boolean unity = Unity.active(player);
		boolean hasSilk = WeaversSilk.bound(player);
		Waypoints.Track wp = TRACKED_WAYPOINTS.get(player.getUUID());
		String currentDim = player.level().dimension().identifier().toString();
		String wpText = "";
		if (wp != null && !wp.none() && currentDim.equals(wp.dimension())) {
			double dx = wp.x() - player.getX();
			double dz = wp.z() - player.getZ();
			long dist = Math.round(Math.hypot(dx, dz));
			wpText = "§aWP " + wp.name() + " " + dist + "m " + compassDirection(dx, dz);
		}
		if (tier == null && stage <= 0 && !clashing && !unity && !hasSilk && wpText.isEmpty()) {
			return;
		}

		Spellbook book = Spellbooks.get(player);
		String spellName = "";
		int slot = book.selected();
		if (tier != null) {
			List<RuneDef> activeRunes = SpellCaster.activeRunes(book, slot, tier);
			spellName = activeRunes.isEmpty() ? "Empty" : SpellCaster.nameOf(player, book, slot, activeRunes);
		}
		String stageName = stage > 0
			? AuraStages.get(stage).map(AuraStages.Stage::id).orElse("s" + stage)
			: "";

		boolean anyPassive = false;
		int passiveSlots = Passives.slots(Heart.circles(player));
		for (int i = 0; i < passiveSlots; i++) {
			if ((book.passivesOff() & (1 << i)) == 0 && !book.passives().get(i).isEmpty()) {
				anyPassive = true;
				break;
			}
		}

		String text = formatSummary(
			tier != null, Spellbooks.mana(player), Mana.max(player), slot + 1, spellName,
			stage > 0, Aura.aura(player), Aura.capacity(player), stageName, Aura.coated(player),
			unity, clashing, hasSilk && !WeaversSilk.stowed(player), anyPassive
		);
		if (!wpText.isEmpty()) {
			text = text.isEmpty() ? wpText : (text + " §8| " + wpText);
		}
		if (!text.isEmpty()) {
			player.sendOverlayMessage(Component.literal(text).withStyle(clashing ? ChatFormatting.GOLD : ChatFormatting.AQUA));
		}
	}

	public static String formatSummary(
		boolean hasCord, double mana, double maxMana, int slotNum, String spellName,
		boolean hasAura, double aura, int maxAura, String stageId, boolean coated,
		boolean unity, boolean clashing, boolean silkOut, boolean passiveOn
	) {
		List<String> parts = new ArrayList<>();
		if (clashing) {
			parts.add("§l§eCLASH! Right-click / /wc clash");
		}
		if (hasCord) {
			parts.add(String.format("§bMP %.0f/%.0f §7| §f[%d] %s", mana, maxMana, slotNum, spellName));
		}
		if (hasAura) {
			String st = stageId.isEmpty() ? "" : " " + Character.toUpperCase(stageId.charAt(0)) + stageId.substring(1);
			parts.add(String.format("§6Aura %.0f/%d§e%s%s", aura, maxAura, st, coated ? " [COAT]" : ""));
		}
		List<String> tags = new ArrayList<>();
		if (passiveOn) tags.add("PASSIVE");
		if (silkOut) tags.add("SILK");
		if (unity) tags.add("UNITY");
		if (!tags.isEmpty()) {
			parts.add("§d[" + String.join(" ", tags) + "]");
		}
		return String.join(" §8| ", parts);
	}
}
