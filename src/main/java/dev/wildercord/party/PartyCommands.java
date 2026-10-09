package dev.wildercord.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Player-only commands; each mutation takes its actor from the authenticated command source. */
final class PartyCommands {
	private PartyCommands() {}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("party")
			.executes(ctx -> list(ctx.getSource().getPlayerOrException()))
			.then(literal("list").executes(ctx -> list(ctx.getSource().getPlayerOrException())))
			.then(literal("invite").then(argument("player", EntityArgument.player()).executes(ctx ->
				invite(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
			.then(literal("accept").then(argument("player", EntityArgument.player()).executes(ctx ->
				answer(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), true))))
			.then(literal("decline").then(argument("player", EntityArgument.player()).executes(ctx ->
				answer(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), false))))
			.then(literal("leave").executes(ctx -> leave(ctx.getSource().getPlayerOrException())))
			.then(literal("disband").executes(ctx -> disband(ctx.getSource().getPlayerOrException())))
			.then(literal("kick").then(argument("member", StringArgumentType.word()).suggests((ctx, builder) -> {
				ServerPlayer player = ctx.getSource().getPlayer();
				if (player == null) return builder.buildFuture();
				PartySession state = state(player);
				PartyRules.Party party = state.rules.party(player.getUUID());
				return SharedSuggestionProvider.suggest(party == null ? List.of() : party.members().stream()
					.filter(id -> !id.equals(player.getUUID())).map(id -> name(state, id)).toList(), builder);
			}).executes(ctx -> kick(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "member"))))));
	}

	private static int invite(ServerPlayer from, ServerPlayer to) {
		PartySession state = state(from);
		state.remember(to.getUUID(), to.getGameProfile().name());
		PartyRules.Result result = state.rules.invite(from.getUUID(), to.getUUID(), Parties.now(from.level().getServer()));
		if (!check(from, result)) return 0;
		String sender = from.getGameProfile().name();
		MutableComponent accept = text("accept", "[Accept]").withStyle(style -> style.withColor(ChatFormatting.GREEN)
			.withClickEvent(new ClickEvent.RunCommand("/party accept " + sender)));
		MutableComponent decline = text("decline", "[Decline]").withStyle(style -> style.withColor(ChatFormatting.RED)
			.withClickEvent(new ClickEvent.RunCommand("/party decline " + sender)));
		to.sendSystemMessage(text("invited", "%s invited you to a party. Expires in 60 seconds. ", from.getDisplayName())
			.append(accept).append(Component.literal(" ")).append(decline));
		from.sendSystemMessage(text("sent", "Party invitation sent to %s. They must accept to join.", to.getDisplayName()));
		return 1;
	}

	private static int answer(ServerPlayer player, ServerPlayer inviter, boolean accept) {
		PartySession state = state(player);
		long now = Parties.now(player.level().getServer());
		PartyRules.Result result = accept ? state.rules.accept(player.getUUID(), inviter.getUUID(), now)
			: state.rules.decline(player.getUUID(), inviter.getUUID(), now);
		if (!check(player, result)) return 0;
		if (accept) {
			announce(player, state.rules.party(player.getUUID()).members(), text("joined", "%s joined the party.", player.getDisplayName()));
			player.sendSystemMessage(text("session", "Party harm protection is on. Agreed duels still work. Parties reset when the server restarts."));
		} else {
			player.sendSystemMessage(text("declined", "Party invitation declined."));
			inviter.sendSystemMessage(text("declined_by", "%s declined your party invitation.", player.getDisplayName()));
		}
		return 1;
	}

	private static int leave(ServerPlayer player) {
		PartySession state = state(player);
		PartyRules.Party before = state.rules.party(player.getUUID());
		if (!check(player, state.rules.leave(player.getUUID()))) return 0;
		announce(player, before.members(), text("left", "%s left the party.", player.getDisplayName()));
		if (before.leader().equals(player.getUUID()) && before.members().size() > 1) {
			UUID remaining = before.members().stream().filter(id -> !id.equals(player.getUUID())).findFirst().orElseThrow();
			PartyRules.Party after = state.rules.party(remaining);
			announce(player, after.members(), text("new_leader", "%s is now the party leader.", name(state, after.leader())));
		}
		return 1;
	}

	private static int disband(ServerPlayer player) {
		PartySession state = state(player);
		PartyRules.Party before = state.rules.party(player.getUUID());
		if (!check(player, state.rules.disband(player.getUUID()))) return 0;
		announce(player, before.members(), text("disbanded", "%s disbanded the party.", player.getDisplayName()));
		return 1;
	}

	/** Offline kicks resolve only an already enrolled UUID's server-observed name, never a made-up UUID. */
	private static int kick(ServerPlayer player, String requested) {
		PartySession state = state(player);
		PartyRules.Party before = state.rules.party(player.getUUID());
		if (before == null) return check(player, PartyRules.Result.NO_PARTY) ? 1 : 0;
		List<UUID> matches = before.members().stream().filter(id -> name(state, id).equalsIgnoreCase(requested)).toList();
		if (matches.size() != 1) {
			player.sendSystemMessage(text("unknown_member", "No unique party member has that name. Use /party list.").withStyle(ChatFormatting.RED));
			return 0;
		}
		UUID member = matches.getFirst();
		if (!check(player, state.rules.kick(player.getUUID(), member))) return 0;
		announce(player, before.members(), text("kicked", "%s removed %s from the party.", player.getDisplayName(), name(state, member)));
		return 1;
	}

	private static int list(ServerPlayer player) {
		PartySession state = state(player);
		PartyRules.Party party = state.rules.party(player.getUUID());
		if (party == null) {
			player.sendSystemMessage(text("none", "You are not in a party. Use /party invite <player>; they must accept."));
			return 0;
		}
		player.sendSystemMessage(text("list", "Party (%s/%s), leader: %s. Friendly harm protection is on; agreed duels still work.",
			party.members().size(), PartyRules.MAX_MEMBERS, name(state, party.leader())));
		for (UUID member : party.members()) {
			boolean online = player.level().getServer().getPlayerList().getPlayer(member) != null;
			player.sendSystemMessage(text(online ? "member_online" : "member_offline", online ? "- %s (online)" : "- %s (offline)", name(state, member)));
		}
		player.sendSystemMessage(text("lifecycle", "Membership survives reconnect and death, but resets on server restart. Only the leader can invite, kick or disband."));
		return party.members().size();
	}

	private static boolean check(ServerPlayer player, PartyRules.Result result) {
		if (result == PartyRules.Result.OK) return true;
		String message = switch (result) {
			case SELF -> "You cannot target yourself. Use /party leave to leave.";
			case NO_PARTY -> "You are not in a party.";
			case NOT_LEADER -> "Only the party leader can do that.";
			case ALREADY_GROUPED -> "That player is already in a party. Leave the current party before accepting another.";
			case NO_INVITE -> "There is no live invitation from that player. Invitations expire after 60 seconds.";
			case FULL -> "That party is full (8 members).";
			case INVITE_LIMIT -> "Too many pending invitations. Wait for an answer or for them to expire.";
			case WAIT -> "Wait a second before sending another party invitation.";
			case NOT_MEMBER -> "That player is not in your party.";
			default -> throw new IllegalStateException("Unexpected party result: " + result);
		};
		player.sendSystemMessage(text("error." + result.name().toLowerCase(java.util.Locale.ROOT), message).withStyle(ChatFormatting.RED));
		return false;
	}

	private static PartySession state(ServerPlayer player) {
		PartySession state = Parties.session(player.level().getServer());
		state.remember(player.getUUID(), player.getGameProfile().name());
		return state;
	}

	private static String name(PartySession state, UUID member) {
		return state.name(member);
	}

	private static void announce(ServerPlayer actor, List<UUID> members, Component message) {
		for (UUID member : members) {
			ServerPlayer online = actor.level().getServer().getPlayerList().getPlayer(member);
			if (online != null) online.sendSystemMessage(message);
		}
	}

	private static MutableComponent text(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("message.wildercord.party." + key, fallback, args);
	}
}
