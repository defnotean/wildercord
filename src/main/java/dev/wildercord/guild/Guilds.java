package dev.wildercord.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.guild.GuildRules.Guild;
import dev.wildercord.guild.GuildRules.Kind;
import dev.wildercord.guild.GuildRules.Result;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Guilds and covens (0.13; the rules are {@link GuildRules}). The roster is the overworld's saved data, so a guild outlasts a
 * restart and can name members who are away. {@code /guild} and {@code /coven} found, invite, join, leave, kick and disband;
 * a guild takes swordsmen (a breathing method learned) and a coven mages (a Cord worn or a Heart Circle formed). Founding one
 * costs {@link GuildRules#FOUND_COST} emeralds. Invitations are saved with the roster and lapse after a minute of game time,
 * so one outlasts a logout or a restart within that minute.
 */
public final class Guilds {
	private Guilds() {}

	/** The saved roster, with the names members were last seen by. */
	public static final class Ledger extends SavedData {
		private static final Codec<Kind> KIND = Codec.STRING.xmap(s -> s.equals("coven") ? Kind.COVEN : Kind.GUILD, Kind::key);
		private static final Codec<Guild> GUILD = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(Guild::name),
			KIND.fieldOf("kind").forGetter(Guild::kind),
			UUIDUtil.STRING_CODEC.fieldOf("leader").forGetter(Guild::leader),
			UUIDUtil.STRING_CODEC.listOf().fieldOf("members").forGetter(Guild::members)
		).apply(i, Guild::new));
		private static final Codec<Invite> INVITE = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("guild").forGetter(Invite::guild),
			Codec.LONG.fieldOf("until").forGetter(Invite::until)
		).apply(i, Invite::new));
		public static final Codec<Ledger> CODEC = RecordCodecBuilder.create(i -> i.group(
			GUILD.listOf().optionalFieldOf("guilds", List.of()).forGetter(l -> l.roster.guilds()),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(l -> l.names),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(KIND, INVITE)).optionalFieldOf("invites", Map.of()).forGetter(l -> l.invites)
		).apply(i, Ledger::new));
		static final SavedDataType<Ledger> TYPE = new SavedDataType<>(Wildercord.id("guilds"), Ledger::new, CODEC, null);

		final GuildRules.Roster roster;
		private final Map<UUID, String> names;

		/** Invitations waiting on each player, by kind. */
		private final Map<UUID, Map<Kind, Invite>> invites;

		public Ledger() {
			this(List.of(), Map.of(), Map.of());
		}

		private Ledger(List<Guild> guilds, Map<UUID, String> names, Map<UUID, Map<Kind, Invite>> invites) {
			this.roster = new GuildRules.Roster(guilds);
			this.names = new HashMap<>(names);
			this.invites = new HashMap<>();
			invites.forEach((id, byKind) -> this.invites.put(id, new HashMap<>(byKind)));
		}

		/** Holds an invitation to {@code guild} for {@code to} for a minute, dropping any that have lapsed by {@code now}. */
		public void invite(UUID to, Kind kind, String guild, long now) {
			invites.values().forEach(byKind -> byKind.values().removeIf(i -> i.until() < now));
			invites.values().removeIf(Map::isEmpty);
			invites.computeIfAbsent(to, id -> new HashMap<>()).put(kind, new Invite(guild, now + GuildRules.INVITE_TICKS));
			setDirty();
		}

		/** Takes {@code player}'s invitation to {@code kind}, or null. */
		Invite takeInvite(UUID player, Kind kind) {
			Map<Kind, Invite> mine = invites.get(player);
			Invite invite = mine == null ? null : mine.remove(kind);
			if (mine != null && mine.isEmpty()) invites.remove(player);
			if (invite != null) setDirty();
			return invite;
		}

		/** Whether {@code player} has an invitation to {@code kind} held, lapsed or not. */
		public boolean invited(UUID player, Kind kind) {
			Map<Kind, Invite> mine = invites.get(player);
			return mine != null && mine.containsKey(kind);
		}

		public GuildRules.Roster roster() {
			return roster;
		}

		/** Runs a change to the roster and marks the ledger to be saved when it took. */
		public Result change(java.util.function.Function<GuildRules.Roster, Result> change) {
			Result result = change.apply(roster);
			if (result == Result.OK) setDirty();
			return result;
		}

		void remember(UUID id, String name) {
			if (!name.equals(names.put(id, name))) setDirty();
		}

		String name(UUID id) {
			return names.getOrDefault(id, id.toString().substring(0, 8));
		}
	}

	private record Invite(String guild, long until) {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> register(dispatcher));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ledger(server).remember(handler.player.getUUID(), handler.player.getGameProfile().name()));
	}

	public static Ledger ledger(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Ledger.TYPE);
	}

	/** The guild or coven {@code player} is in, or null. */
	public static Guild of(ServerPlayer player, Kind kind) {
		return ledger(player.level().getServer()).roster.of(player.getUUID(), kind);
	}

	/** Fellow members of {@code player}'s {@code kind} close enough to count. */
	public static int nearby(ServerPlayer player, Kind kind) {
		Guild guild = of(player, kind);
		if (guild == null) return 0;
		int n = 0;
		for (UUID id : guild.members()) {
			if (id.equals(player.getUUID())) continue;
			ServerPlayer other = player.level().getServer().getPlayerList().getPlayer(id);
			if (other != null && other.isAlive() && !other.isSpectator() && other.level() == player.level()
					&& other.distanceToSqr(player) <= GuildRules.NEAR * GuildRules.NEAR) n++;
		}
		return n;
	}

	/** What {@code player}'s training counts for, with fellow members of their {@code kind} close by. */
	public static double bonus(ServerPlayer player, Kind kind) {
		return GuildRules.bonus(nearby(player, kind));
	}

	/** Whether {@code player} may stand in a {@code kind}: a guild takes swordsmen, a coven mages. */
	public static boolean eligible(ServerPlayer player, Kind kind) {
		return kind == Kind.GUILD ? dev.wildercord.aura.Aura.stage(player) > dev.wildercord.aura.AuraRules.NONE
			: !dev.wildercord.player.Spellbooks.cord(player).isEmpty() || dev.wildercord.player.Heart.circles(player) > 0;
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (Kind kind : Kind.values()) dispatcher.register(node(kind));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> node(Kind kind) {
		return literal(kind.key())
			.executes(ctx -> show(ctx.getSource().getPlayerOrException(), kind))
			.then(literal("found").then(argument("name", StringArgumentType.greedyString()).executes(ctx ->
				found(ctx.getSource().getPlayerOrException(), kind, StringArgumentType.getString(ctx, "name")))))
			.then(literal("invite").then(argument("player", EntityArgument.player()).executes(ctx ->
				invite(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), kind))))
			.then(literal("accept").executes(ctx -> accept(ctx.getSource().getPlayerOrException(), kind)))
			.then(literal("leave").executes(ctx -> leave(ctx.getSource().getPlayerOrException(), kind)))
			.then(literal("disband").executes(ctx -> disband(ctx.getSource().getPlayerOrException(), kind)))
			.then(literal("kick").then(argument("member", StringArgumentType.word()).suggests((ctx, builder) -> {
				ServerPlayer player = ctx.getSource().getPlayer();
				Guild guild = player == null ? null : of(player, kind);
				Ledger ledger = player == null ? null : ledger(player.level().getServer());
				return SharedSuggestionProvider.suggest(guild == null ? List.of() : guild.members().stream()
					.filter(id -> !id.equals(player.getUUID())).map(ledger::name).toList(), builder);
			}).executes(ctx -> kick(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "member"), kind))));
	}

	private static int show(ServerPlayer player, Kind kind) {
		Ledger ledger = ledger(player.level().getServer());
		Guild guild = ledger.roster.of(player.getUUID(), kind);
		if (guild == null) {
			player.sendSystemMessage(text(kind, "none", GuildRules.FOUND_COST));
			return 0;
		}
		MutableComponent members = Component.empty();
		for (int i = 0; i < guild.members().size(); i++) {
			UUID id = guild.members().get(i);
			boolean online = player.level().getServer().getPlayerList().getPlayer(id) != null;
			if (i > 0) members.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
			members.append(Component.literal(ledger.name(id) + (id.equals(guild.leader()) ? " ★" : ""))
				.withStyle(online ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
		}
		player.sendSystemMessage(text(kind, "show", guild.name(), guild.members().size(), GuildRules.MAX_MEMBERS).withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(members);
		int near = nearby(player, kind);
		player.sendSystemMessage(text(kind, "near", near, Math.round((GuildRules.bonus(near) - 1) * 100)).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int found(ServerPlayer player, Kind kind, String name) {
		Ledger ledger = ledger(player.level().getServer());
		ledger.remember(player.getUUID(), player.getGameProfile().name());
		if (!eligible(player, kind)) return refuse(player, kind, "eligible");
		Result check = !GuildRules.validName(name) ? Result.BAD_NAME : ledger.roster.named(name) != null ? Result.NAME_TAKEN
			: ledger.roster.of(player.getUUID(), kind) != null ? Result.ALREADY_IN : Result.OK;
		if (check != Result.OK) return refuse(player, kind, check);
		if (!player.isCreative()) {
			if (player.getInventory().countItem(Items.EMERALD) < GuildRules.FOUND_COST) return refuse(player, kind, "cost");
			int owed = GuildRules.FOUND_COST;
			for (int i = 0; i < player.getInventory().getContainerSize() && owed > 0; i++) {
				var stack = player.getInventory().getItem(i);
				if (!stack.is(Items.EMERALD)) continue;
				int take = Math.min(owed, stack.getCount());
				stack.shrink(take);
				owed -= take;
			}
		}
		ledger.change(r -> r.found(player.getUUID(), kind, name));
		player.sendSystemMessage(text(kind, "founded", name).withStyle(ChatFormatting.GOLD));
		return 1;
	}

	private static int invite(ServerPlayer from, ServerPlayer to, Kind kind) {
		Ledger ledger = ledger(from.level().getServer());
		ledger.remember(to.getUUID(), to.getGameProfile().name());
		Result check = ledger.roster.canInvite(from.getUUID(), to.getUUID(), kind);
		if (check != Result.OK) return refuse(from, kind, check);
		if (!eligible(to, kind)) return refuse(from, kind, "their_eligible");
		Guild guild = ledger.roster.of(from.getUUID(), kind);
		long now = from.level().getServer().overworld().getGameTime();
		ledger.invite(to.getUUID(), kind, guild.name(), now);
		MutableComponent accept = text(kind, "accept_button").withStyle(style -> style.withColor(ChatFormatting.GREEN)
			.withClickEvent(new ClickEvent.RunCommand("/" + kind.key() + " accept")));
		to.sendSystemMessage(text(kind, "invited", from.getDisplayName(), guild.name()).append(Component.literal(" ")).append(accept));
		from.sendSystemMessage(text(kind, "invite_sent", to.getDisplayName()));
		return 1;
	}

	private static int accept(ServerPlayer player, Kind kind) {
		Invite invite = ledger(player.level().getServer()).takeInvite(player.getUUID(), kind);
		if (invite == null || invite.until() < player.level().getServer().overworld().getGameTime()) return refuse(player, kind, "no_invite");
		if (!eligible(player, kind)) return refuse(player, kind, "eligible");
		Ledger ledger = ledger(player.level().getServer());
		ledger.remember(player.getUUID(), player.getGameProfile().name());
		Result result = ledger.change(r -> r.join(player.getUUID(), invite.guild()));
		if (result != Result.OK) return refuse(player, kind, result);
		announce(player, ledger.roster.named(invite.guild()), text(kind, "joined", player.getDisplayName(), invite.guild()));
		return 1;
	}

	private static int leave(ServerPlayer player, Kind kind) {
		Ledger ledger = ledger(player.level().getServer());
		Guild before = ledger.roster.of(player.getUUID(), kind);
		Result result = ledger.change(r -> r.leave(player.getUUID(), kind));
		if (result != Result.OK) return refuse(player, kind, result);
		player.sendSystemMessage(text(kind, "left", before.name()));
		Guild after = ledger.roster.named(before.name());
		if (after != null) announce(player, after, text(kind, "member_left", player.getDisplayName(), ledger.name(after.leader())));
		return 1;
	}

	private static int kick(ServerPlayer player, String member, Kind kind) {
		Ledger ledger = ledger(player.level().getServer());
		Guild guild = ledger.roster.of(player.getUUID(), kind);
		UUID target = guild == null ? null : guild.members().stream().filter(id -> ledger.name(id).equalsIgnoreCase(member)).findFirst().orElse(null);
		if (guild != null && target == null) return refuse(player, kind, Result.NOT_MEMBER);
		Result result = ledger.change(r -> target == null ? Result.NOT_IN : r.kick(player.getUUID(), target, kind));
		if (result != Result.OK) return refuse(player, kind, result);
		announce(player, ledger.roster.named(guild.name()), text(kind, "kicked", ledger.name(target)));
		ServerPlayer gone = player.level().getServer().getPlayerList().getPlayer(target);
		if (gone != null) gone.sendSystemMessage(text(kind, "you_were_kicked", guild.name()));
		return 1;
	}

	private static int disband(ServerPlayer player, Kind kind) {
		Ledger ledger = ledger(player.level().getServer());
		Guild guild = ledger.roster.of(player.getUUID(), kind);
		Result result = ledger.change(r -> r.disband(player.getUUID(), kind));
		if (result != Result.OK) return refuse(player, kind, result);
		for (UUID id : guild.members()) {
			ServerPlayer member = player.level().getServer().getPlayerList().getPlayer(id);
			if (member != null) member.sendSystemMessage(text(kind, "disbanded", guild.name()));
		}
		return 1;
	}

	private static void announce(ServerPlayer actor, Guild guild, Component message) {
		if (guild == null) return;
		for (UUID id : guild.members()) {
			ServerPlayer member = actor.level().getServer().getPlayerList().getPlayer(id);
			if (member != null) member.sendSystemMessage(message);
		}
	}

	private static int refuse(ServerPlayer player, Kind kind, Result result) {
		return refuse(player, kind, result.name().toLowerCase(java.util.Locale.ROOT));
	}

	private static int refuse(ServerPlayer player, Kind kind, String why) {
		player.sendSystemMessage(text(kind, "refuse." + why, GuildRules.FOUND_COST, GuildRules.MAX_MEMBERS).withStyle(ChatFormatting.RED));
		return 0;
	}

	private static MutableComponent text(Kind kind, String key, Object... args) {
		return Component.translatable("message.wildercord." + kind.key() + "." + key, args);
	}
}
