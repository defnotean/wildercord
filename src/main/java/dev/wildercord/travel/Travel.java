package dev.wildercord.travel;

import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.UnaryOperator;

/**
 * The travel commands: homes, public warps, personal waypoints, teleport requests, {@code /back},
 * {@code /spawn} and {@code /rtp}. This class wires them up and holds what they share: each player's
 * {@link TravelData}, the server's settings, who counts as an operator, and how messages look.
 *
 * <ul>
 *   <li>{@link TravelCommands}: the command tree (registered only while {@code travel.enabled} is on)</li>
 *   <li>{@link Teleports}: every teleport's warmup, cooldown, safe landing and visuals</li>
 *   <li>{@link Homes}, {@link Warps}, {@link Waypoints}, {@link TeleportRequests}: what each command does</li>
 *   <li>{@link TravelRules} and {@link SafeSpots}: the pure rules, unit-tested</li>
 * </ul>
 */
public final class Travel {
	private Travel() {}

	/** Each player's homes, waypoints, back location and request switch. Saved, and kept through death. */
	public static final AttachmentType<TravelData> DATA = AttachmentRegistry.create(
		Wildercord.id("travel"),
		builder -> builder
			.initializer(() -> TravelData.EMPTY)
			.persistent(TravelData.CODEC)
			.copyOnDeath()
	);

	private static final String KEY = "message.wildercord.travel.";
	/** Names and places in messages. */
	static final int HIGHLIGHT = 0xF0D8FF;
	/** Good news: something set, something done. */
	static final int ARCANE = 0xC8A8FF;

	public static void init() {
		// Registered whatever the config says, so both sides always agree on the payload.
		PayloadTypeRegistry.clientboundPlay().register(Waypoints.Track.TYPE, Waypoints.Track.CODEC);
		// Read each time commands are built (at start and on /reload), so switching the feature off takes the commands away.
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TravelCommands.register(dispatcher));
		Teleports.init();
		TeleportRequests.init();
		Waypoints.init();
		// Where you died is where /back goes, and after respawning you're told so.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player && enabled()) {
				update(player, data -> data.withBack(Spot.of(player)));
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && enabled() && data(newPlayer).back().isPresent()) {
				newPlayer.sendSystemMessage(button(text("back_hint"), "/back", text("back_hover")).withStyle(ChatFormatting.GRAY));
			}
		});
	}

	public static WildercordConfig.TravelSettings config() {
		return Config.get().travel();
	}

	/** Whether the travel commands are on (the server's {@code travel.enabled}). */
	public static boolean enabled() {
		return config().enabled();
	}

	public static TravelData data(ServerPlayer player) {
		return player.getAttachedOrElse(DATA, TravelData.EMPTY);
	}

	public static void update(ServerPlayer player, UnaryOperator<TravelData> change) {
		player.setAttached(DATA, change.apply(data(player)));
	}

	/** Whether {@code player} is an operator (permission level 2, as for {@code /wildercord}): they skip warmups and cooldowns. */
	public static boolean operator(ServerPlayer player) {
		return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
	}

	// ------------------------------------------------------------------ messages

	static MutableComponent text(String key, Object... args) {
		return Component.translatable(KEY + key, args);
	}

	/** A home, warp or waypoint's name, picked out. */
	static MutableComponent name(String name) {
		return Component.literal(name).withColor(HIGHLIGHT);
	}

	static void fail(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(text(key, args).withStyle(ChatFormatting.RED));
	}

	static void info(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(text(key, args).withStyle(ChatFormatting.GRAY));
	}

	static void good(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(text(key, args).withColor(ARCANE));
	}

	/** {@code label}, running {@code command} when clicked and showing {@code hover} under the pointer. */
	static MutableComponent button(Component label, String command, Component hover) {
		return label.copy().withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)).withHoverEvent(new HoverEvent.ShowText(hover)));
	}

	/** Names laid out in a row, each a button: "home, base, mine". */
	static MutableComponent row(Iterable<MutableComponent> buttons) {
		MutableComponent line = Component.empty();
		boolean first = true;
		for (MutableComponent button : buttons) {
			if (!first) {
				line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
			}
			line.append(button);
			first = false;
		}
		return line;
	}

	/** Whether travel is on; if not, {@code player} is told so. */
	static boolean available(ServerPlayer player) {
		if (enabled()) {
			return true;
		}
		fail(player, "disabled");
		return false;
	}
}
