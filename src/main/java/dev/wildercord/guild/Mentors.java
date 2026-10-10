package dev.wildercord.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.player.Heart;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Mentoring (0.13; the rules are {@link MentorRules}): a seasoned mage takes an apprentice with {@code /mentor take}, the
 * apprentice agrees with {@code /mentor accept}, and either ends it with {@code /mentor end}. Who is whose is the overworld's
 * saved data, so the bond outlasts a restart. Offers are held only in memory for a minute.
 */
public final class Mentors {
	private Mentors() {}

	public static final class Ledger extends SavedData {
		public static final Codec<Ledger> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC).optionalFieldOf("mentors", Map.of()).forGetter(l -> l.book.all()),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(l -> l.names)
		).apply(i, Ledger::new));
		static final SavedDataType<Ledger> TYPE = new SavedDataType<>(Wildercord.id("mentors"), Ledger::new, CODEC, null);

		final MentorRules.Book book;
		private final Map<UUID, String> names;

		public Ledger() {
			this(Map.of(), Map.of());
		}

		private Ledger(Map<UUID, UUID> mentors, Map<UUID, String> names) {
			this.book = new MentorRules.Book(mentors);
			this.names = new HashMap<>(names);
		}

		public MentorRules.Book book() {
			return book;
		}

		public void take(UUID mentor, UUID apprentice) {
			book.take(mentor, apprentice);
			setDirty();
		}

		public boolean end(UUID a, UUID b) {
			boolean ended = book.end(a, b);
			if (ended) setDirty();
			return ended;
		}

		void remember(UUID id, String name) {
			if (!name.equals(names.put(id, name))) setDirty();
		}

		String name(UUID id) {
			return names.getOrDefault(id, id.toString().substring(0, 8));
		}
	}

	private record Offer(UUID mentor, long until) {}

	private static final Map<UUID, Offer> OFFERS = new ConcurrentHashMap<>();
	private static final long OFFER_TICKS = 20 * 60;

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> register(dispatcher));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ledger(server).remember(handler.player.getUUID(), handler.player.getGameProfile().name()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> OFFERS.remove(handler.player.getUUID()));
	}

	public static Ledger ledger(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Ledger.TYPE);
	}

	/** Whether {@code apprentice}'s mentor is close enough to teach. */
	public static boolean mentorNear(ServerPlayer apprentice) {
		UUID mentor = ledger(apprentice.level().getServer()).book.mentorOf(apprentice.getUUID());
		if (mentor == null) return false;
		ServerPlayer m = apprentice.level().getServer().getPlayerList().getPlayer(mentor);
		return m != null && m.isAlive() && !m.isSpectator() && m.level() == apprentice.level()
			&& m.distanceToSqr(apprentice) <= MentorRules.NEAR * MentorRules.NEAR;
	}

	/** What {@code player}'s condensed mana counts for from their mentor's nearness. */
	public static double gain(ServerPlayer player) {
		return MentorRules.gain(mentorNear(player));
	}

	/** {@code player} formed circle {@code n}: their mentor is paid, and at the last circle of it they graduate. */
	public static void formed(ServerPlayer player, int n) {
		Ledger ledger = ledger(player.level().getServer());
		UUID mentorId = ledger.book.mentorOf(player.getUUID());
		if (mentorId == null) return;
		ServerPlayer mentor = player.level().getServer().getPlayerList().getPlayer(mentorId);
		if (mentor != null) {
			ItemStack crystals = new ItemStack(dev.wildercord.content.WildercordItems.MANA_CRYSTAL, MentorRules.tutelage(n));
			if (!mentor.getInventory().add(crystals)) mentor.spawnAtLocation(mentor.level(), crystals);
			mentor.sendSystemMessage(text("tutelage", player.getDisplayName(), dev.wildercord.spell.Circles.ordinal(n), MentorRules.tutelage(n)).withStyle(ChatFormatting.AQUA));
		}
		if (MentorRules.graduates(n)) {
			ledger.end(mentorId, player.getUUID());
			player.sendSystemMessage(text("graduated", ledger.name(mentorId)).withStyle(ChatFormatting.GOLD));
			if (mentor != null) mentor.sendSystemMessage(text("graduated_mentor", player.getDisplayName()).withStyle(ChatFormatting.GOLD));
		}
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("mentor")
			.executes(ctx -> show(ctx.getSource().getPlayerOrException()))
			.then(literal("take").then(argument("player", EntityArgument.player()).executes(ctx ->
				take(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
			.then(literal("accept").executes(ctx -> accept(ctx.getSource().getPlayerOrException())))
			.then(literal("end").then(argument("name", StringArgumentType.word()).suggests((ctx, builder) -> {
				ServerPlayer player = ctx.getSource().getPlayer();
				return SharedSuggestionProvider.suggest(player == null ? List.of() : bonds(player).stream()
					.map(ledger(player.level().getServer())::name).toList(), builder);
			}).executes(ctx -> end(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"))))));
	}

	/** Everyone {@code player} is bonded to: their mentor, then their apprentices. */
	private static List<UUID> bonds(ServerPlayer player) {
		Ledger ledger = ledger(player.level().getServer());
		List<UUID> all = new ArrayList<>();
		UUID mentor = ledger.book.mentorOf(player.getUUID());
		if (mentor != null) all.add(mentor);
		all.addAll(ledger.book.apprenticesOf(player.getUUID()));
		return all;
	}

	private static int show(ServerPlayer player) {
		Ledger ledger = ledger(player.level().getServer());
		UUID mentor = ledger.book.mentorOf(player.getUUID());
		List<UUID> apprentices = ledger.book.apprenticesOf(player.getUUID());
		if (mentor == null && apprentices.isEmpty()) {
			player.sendSystemMessage(text("none", MentorRules.MENTOR_FROM, MentorRules.TAKE_BELOW).withStyle(ChatFormatting.GRAY));
			return 0;
		}
		if (mentor != null) {
			player.sendSystemMessage(text("your_mentor", ledger.name(mentor), mentorNear(player) ? text("near") : text("far")).withStyle(ChatFormatting.AQUA));
		}
		if (!apprentices.isEmpty()) {
			player.sendSystemMessage(text("your_apprentices", String.join(", ", apprentices.stream().map(ledger::name).toList()),
				apprentices.size(), MentorRules.MAX_APPRENTICES).withStyle(ChatFormatting.AQUA));
		}
		return 1;
	}

	private static int take(ServerPlayer mentor, ServerPlayer apprentice) {
		Ledger ledger = ledger(mentor.level().getServer());
		ledger.remember(apprentice.getUUID(), apprentice.getGameProfile().name());
		MentorRules.Refusal refusal = refusal(ledger, mentor, apprentice);
		if (refusal != MentorRules.Refusal.NONE) return refuse(mentor, refusal);
		OFFERS.put(apprentice.getUUID(), new Offer(mentor.getUUID(), mentor.level().getServer().overworld().getGameTime() + OFFER_TICKS));
		MutableComponent accept = text("accept_button").withStyle(style -> style.withColor(ChatFormatting.GREEN)
			.withClickEvent(new ClickEvent.RunCommand("/mentor accept")));
		apprentice.sendSystemMessage(text("offered", mentor.getDisplayName()).append(Component.literal(" ")).append(accept));
		mentor.sendSystemMessage(text("offer_sent", apprentice.getDisplayName()));
		return 1;
	}

	private static MentorRules.Refusal refusal(Ledger ledger, ServerPlayer mentor, ServerPlayer apprentice) {
		return MentorRules.refusal(mentor.getUUID().equals(apprentice.getUUID()), Heart.circles(mentor), Heart.circles(apprentice),
			ledger.book.apprenticesOf(mentor.getUUID()).size(), ledger.book.mentorOf(apprentice.getUUID()) != null,
			ledger.book.mentorOf(mentor.getUUID()) != null);
	}

	private static int accept(ServerPlayer apprentice) {
		Offer offer = OFFERS.remove(apprentice.getUUID());
		MinecraftServer server = apprentice.level().getServer();
		if (offer == null || offer.until() < server.overworld().getGameTime()) return refuse(apprentice, "no_offer");
		ServerPlayer mentor = server.getPlayerList().getPlayer(offer.mentor());
		if (mentor == null) return refuse(apprentice, "mentor_gone");
		Ledger ledger = ledger(server);
		ledger.remember(apprentice.getUUID(), apprentice.getGameProfile().name());
		ledger.remember(mentor.getUUID(), mentor.getGameProfile().name());
		MentorRules.Refusal refusal = refusal(ledger, mentor, apprentice);
		if (refusal != MentorRules.Refusal.NONE) return refuse(apprentice, refusal);
		ledger.take(mentor.getUUID(), apprentice.getUUID());
		apprentice.sendSystemMessage(text("taken_apprentice", mentor.getDisplayName(), (int) MentorRules.NEAR, MentorRules.GRADUATE_AT).withStyle(ChatFormatting.AQUA));
		mentor.sendSystemMessage(text("taken_mentor", apprentice.getDisplayName()).withStyle(ChatFormatting.AQUA));
		return 1;
	}

	private static int end(ServerPlayer player, String name) {
		Ledger ledger = ledger(player.level().getServer());
		UUID other = bonds(player).stream().filter(id -> ledger.name(id).equalsIgnoreCase(name)).findFirst().orElse(null);
		if (other == null || !ledger.end(player.getUUID(), other)) return refuse(player, "not_bonded");
		player.sendSystemMessage(text("ended", ledger.name(other)));
		ServerPlayer them = player.level().getServer().getPlayerList().getPlayer(other);
		if (them != null) them.sendSystemMessage(text("ended_by", player.getDisplayName()));
		return 1;
	}

	private static int refuse(ServerPlayer player, MentorRules.Refusal refusal) {
		return refuse(player, refusal.name().toLowerCase(java.util.Locale.ROOT));
	}

	private static int refuse(ServerPlayer player, String why) {
		player.sendSystemMessage(text("refuse." + why, MentorRules.MENTOR_FROM, MentorRules.TAKE_BELOW, MentorRules.MAX_APPRENTICES).withStyle(ChatFormatting.RED));
		return 0;
	}

	private static MutableComponent text(String key, Object... args) {
		return Component.translatable("message.wildercord.mentor." + key, args);
	}
}
