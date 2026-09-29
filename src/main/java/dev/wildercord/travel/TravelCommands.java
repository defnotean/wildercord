package dev.wildercord.travel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.wildercord.Wildercord;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * The travel command tree. Everything is open to every player except {@code /setwarp} and
 * {@code /delwarp} (operators). Names are suggested as you type: your homes, the warps, your
 * waypoints, online players, and for {@code /tpaccept} and {@code /tpdeny} whoever has asked you.
 *
 * <p>Minecraft has a {@code /waypoint} command of its own, for operators, to restyle the locator bar.
 * Ours replaces it in the tree so every player can use it, and keeps the game's under it for
 * operators: {@code /waypoint modify ...} as before, and its list as {@code /waypoint locator}.</p>
 */
public final class TravelCommands {
	private TravelCommands() {}

	private static final String NAME = "name";

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (!Travel.enabled()) {
			return;
		}
		dispatcher.register(literal("sethome")
			.executes(ctx -> Homes.set(player(ctx), TravelRules.DEFAULT_HOME))
			.then(argument(NAME, StringArgumentType.word()).suggests(HOMES).executes(ctx -> Homes.set(player(ctx), name(ctx)))));
		dispatcher.register(literal("home")
			.executes(ctx -> Homes.go(player(ctx), null, operator(ctx)))
			.then(argument(NAME, StringArgumentType.word()).suggests(HOMES).executes(ctx -> Homes.go(player(ctx), name(ctx), operator(ctx)))));
		dispatcher.register(literal("delhome")
			.then(argument(NAME, StringArgumentType.word()).suggests(HOMES).executes(ctx -> Homes.delete(player(ctx), name(ctx)))));
		dispatcher.register(literal("homes").executes(ctx -> Homes.list(player(ctx))));

		dispatcher.register(literal("warp")
			.executes(ctx -> Warps.list(player(ctx)))
			.then(argument(NAME, StringArgumentType.word()).suggests(WARPS).executes(ctx -> Warps.go(player(ctx), name(ctx), operator(ctx)))));
		dispatcher.register(literal("warps").executes(ctx -> Warps.list(player(ctx))));
		dispatcher.register(literal("setwarp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(argument(NAME, StringArgumentType.word()).suggests(WARPS).executes(ctx -> Warps.set(player(ctx), name(ctx)))));
		dispatcher.register(literal("delwarp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(argument(NAME, StringArgumentType.word()).suggests(WARPS).executes(ctx -> Warps.delete(player(ctx), name(ctx)))));

		dispatcher.register(literal("tpa")
			.then(argument("player", EntityArgument.player()).executes(ctx -> TeleportRequests.ask(player(ctx), EntityArgument.getPlayer(ctx, "player"), false))));
		dispatcher.register(literal("tpahere")
			.then(argument("player", EntityArgument.player()).executes(ctx -> TeleportRequests.ask(player(ctx), EntityArgument.getPlayer(ctx, "player"), true))));
		dispatcher.register(literal("tpaccept")
			.executes(ctx -> TeleportRequests.accept(player(ctx), null))
			.then(argument("player", EntityArgument.player()).suggests(ASKERS)
				.executes(ctx -> TeleportRequests.accept(player(ctx), EntityArgument.getPlayer(ctx, "player")))));
		dispatcher.register(literal("tpdeny")
			.executes(ctx -> TeleportRequests.deny(player(ctx), null))
			.then(argument("player", EntityArgument.player()).suggests(ASKERS)
				.executes(ctx -> TeleportRequests.deny(player(ctx), EntityArgument.getPlayer(ctx, "player")))));
		dispatcher.register(literal("tpcancel").executes(ctx -> TeleportRequests.cancel(player(ctx))));
		dispatcher.register(literal("tptoggle").executes(ctx -> TeleportRequests.toggle(player(ctx))));

		dispatcher.register(literal("back").executes(ctx -> Teleports.back(player(ctx), operator(ctx))));
		dispatcher.register(literal("spawn").executes(ctx -> Teleports.spawn(player(ctx), operator(ctx))));
		dispatcher.register(literal("rtp").executes(ctx -> Teleports.randomly(player(ctx), operator(ctx))));

		registerWaypoint(dispatcher);
	}

	private static void registerWaypoint(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> waypoint = literal("waypoint")
			.executes(ctx -> Waypoints.list(player(ctx)))
			.then(literal("add").then(argument(NAME, StringArgumentType.word()).suggests(WAYPOINTS)
				.executes(ctx -> Waypoints.add(player(ctx), name(ctx), null, null))
				.then(argument("pos", Vec3Argument.vec3())
					.executes(ctx -> Waypoints.add(player(ctx), name(ctx), Vec3Argument.getVec3(ctx, "pos"), null))
					// Shared waypoints come with their world, so they're added where they really are.
					.then(argument("dimension", DimensionArgument.dimension())
						.executes(ctx -> Waypoints.add(player(ctx), name(ctx), Vec3Argument.getVec3(ctx, "pos"), DimensionArgument.getDimension(ctx, "dimension")))))))
			.then(literal("remove").then(argument(NAME, StringArgumentType.word()).suggests(WAYPOINTS)
				.executes(ctx -> Waypoints.remove(player(ctx), name(ctx)))))
			.then(literal("list").executes(ctx -> Waypoints.list(player(ctx))))
			.then(literal("track").then(argument(NAME, StringArgumentType.word()).suggests(WAYPOINTS)
				.executes(ctx -> Waypoints.track(player(ctx), name(ctx)))))
			.then(literal("untrack").executes(ctx -> Waypoints.untrack(player(ctx))))
			.then(literal("share").then(argument(NAME, StringArgumentType.word()).suggests(WAYPOINTS)
				.then(argument("player", EntityArgument.player())
					.executes(ctx -> Waypoints.share(player(ctx), name(ctx), EntityArgument.getPlayer(ctx, "player"))))));
		// The game's own /waypoint, for operators, kept under ours.
		LiteralCommandNode<CommandSourceStack> vanilla = detach(dispatcher.getRoot(), "waypoint");
		if (vanilla != null) {
			CommandNode<CommandSourceStack> list = vanilla.getChild("list");
			if (list != null && list.getCommand() != null) {
				waypoint.then(literal("locator").requires(vanilla.getRequirement()).executes(list.getCommand()));
			}
			CommandNode<CommandSourceStack> modify = vanilla.getChild("modify");
			if (modify != null && !modify.getChildren().isEmpty()) {
				// Its branches moved under a new "modify" that only operators see (a redirect wouldn't reach the client's copy of the tree).
				LiteralArgumentBuilder<CommandSourceStack> ours = literal("modify").requires(vanilla.getRequirement());
				modify.getChildren().forEach(ours::then);
				waypoint.then(ours);
			}
		}
		dispatcher.register(waypoint);
	}

	/**
	 * Takes the command {@code name} out of the tree and returns it (null if there's none). Brigadier
	 * merges a second command of the same name into the first, keeping the first's requirement, so the
	 * game's operators-only {@code /waypoint} has to come out before ours goes in.
	 */
	@SuppressWarnings("unchecked")
	private static LiteralCommandNode<CommandSourceStack> detach(RootCommandNode<CommandSourceStack> root, String name) {
		if (!(root.getChild(name) instanceof LiteralCommandNode<CommandSourceStack> node)) {
			return null;
		}
		try {
			for (String field : List.of("children", "literals")) {
				Field f = CommandNode.class.getDeclaredField(field);
				f.setAccessible(true);
				((Map<String, ?>) f.get(root)).remove(name);
			}
			return node;
		} catch (ReflectiveOperationException | RuntimeException e) {
			Wildercord.LOGGER.warn("Couldn't take Minecraft's /{} out of the command tree ({}); only operators will be able to use Wildercord's", name, e.toString());
			return null;
		}
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return ctx.getSource().getPlayerOrException();
	}

	private static boolean operator(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return Travel.operator(player(ctx));
	}

	private static String name(CommandContext<CommandSourceStack> ctx) {
		return StringArgumentType.getString(ctx, NAME);
	}

	/** Suggests names from the player running the command (none from the console). */
	private static SuggestionProvider<CommandSourceStack> fromPlayer(Function<ServerPlayer, Stream<String>> names) {
		return (ctx, builder) -> {
			ServerPlayer player = ctx.getSource().getPlayer();
			return SharedSuggestionProvider.suggest(player == null ? Stream.empty() : names.apply(player), builder);
		};
	}

	private static final SuggestionProvider<CommandSourceStack> HOMES = fromPlayer(p -> Travel.data(p).homes().keySet().stream());
	private static final SuggestionProvider<CommandSourceStack> WAYPOINTS = fromPlayer(p -> Travel.data(p).waypoints().keySet().stream());
	private static final SuggestionProvider<CommandSourceStack> ASKERS = fromPlayer(p -> TeleportRequests.waitingFor(p).stream()
		.map(r -> p.level().getServer().getPlayerList().getPlayer(r.from()))
		.filter(Objects::nonNull)
		.map(asker -> asker.getGameProfile().name()));
	private static final SuggestionProvider<CommandSourceStack> WARPS = (ctx, builder) ->
		SharedSuggestionProvider.suggest(Warps.of(ctx.getSource().getServer()).names(), builder);
}
