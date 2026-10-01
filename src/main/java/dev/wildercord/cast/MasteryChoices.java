package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.api.SpellMasteryApi;
import dev.wildercord.config.Config;
import dev.wildercord.net.PacketThrottle;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Choosing a mastered spell's traits: the offers made as each rank is reached, the player's choice from the Cord
 * screen, and changing one's mind once per rank (re-rolling the offer, or unbinding the trait chosen) for
 * {@link MasteryRules#CHANGE_LEVELS} experience levels. Also the notices a player gets: the toast when a spell reaches
 * a rank, and a spell's name spoken when a named Adept spell is cast nearby.
 */
public final class MasteryChoices {
	private MasteryChoices() {}

	/** Choose one of the traits offered for a slot. */
	public static final int CHOOSE = 0;
	/** Re-roll the offer for a slot (its one change). */
	public static final int REROLL = 1;
	/** Unbind the trait in a slot to choose again (its one change). */
	public static final int UNBIND = 2;

	/**
	 * Client to server: a choice about the traits of the spell in Cord slot {@code spell} ({@link #CHOOSE}, {@link #REROLL}
	 * or {@link #UNBIND}), for trait slot {@code slot}; {@code trait} is the one chosen. Nothing about the spell travels:
	 * the server reads it from its own spellbook.
	 */
	public record Request(int kind, int spell, int slot, String trait) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(Wildercord.id("mastery_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Request::kind, ByteBufCodecs.VAR_INT, Request::spell, ByteBufCodecs.VAR_INT, Request::slot,
			ByteBufCodecs.stringUtf8(128), Request::trait, Request::new).cast();

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}

	/** Server to client: one of your spells reached a rank (the client shows a toast, and the Cord screen offers the trait). */
	public record Rise(String name, int rank, int color, boolean choice, long seed) implements CustomPacketPayload {
		public static final Type<Rise> TYPE = new Type<>(Wildercord.id("mastery_rise"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Rise> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Rise::name, ByteBufCodecs.VAR_INT, Rise::rank, ByteBufCodecs.INT, Rise::color, ByteBufCodecs.BOOL, Rise::choice,
			ByteBufCodecs.VAR_LONG, Rise::seed, Rise::new).cast();

		@Override
		public Type<Rise> type() {
			return TYPE;
		}
	}

	/** Server to client: a named spell of Adept rank or higher was cast by {@code caster} (an entity id): its name, briefly, by them. */
	public record Title(int caster, String name, int rank, int color) implements CustomPacketPayload {
		public static final Type<Title> TYPE = new Type<>(Wildercord.id("mastery_title"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Title> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Title::caster, ByteBufCodecs.stringUtf8(64), Title::name, ByteBufCodecs.VAR_INT, Title::rank, ByteBufCodecs.INT, Title::color,
			Title::new).cast();

		@Override
		public Type<Title> type() {
			return TYPE;
		}
	}

	/** Choices are spellbook changes: a burst, then a steady trickle. */
	private static final PacketThrottle THROTTLE = new PacketThrottle(10, 4);

	static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Rise.TYPE, Rise.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Title.TYPE, Title.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, context) -> {
			if (!THROTTLE.allow(context.player().getUUID(), context.server().getTickCount())) {
				return;
			}
			Component answer = request(context.player(), payload.kind(), payload.spell(), payload.slot(), payload.trait());
			if (answer != null) {
				context.player().sendOverlayMessage(answer);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> THROTTLE.forget(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> THROTTLE.clear());
	}

	// ------------------------------------------------------------------ offers

	/**
	 * {@code entry} with an offer for its next open slot if one is due and none is waiting (a rank reached, a trait chosen
	 * with another rank already open, a trait unbound).
	 */
	static MasteryBook.Entry offerIfDue(ServerPlayer player, MasteryBook.Entry entry, List<RuneDef> runes) {
		int slot = entry.pendingSlot();
		if (slot < 0 || !entry.offer().isEmpty() || runes.isEmpty()) {
			return entry;
		}
		return entry.withOffer(offer(player, entry, runes, slot, 0, Set.of()));
	}

	/** The traits offered for {@code slot}: three from the catalogue (and outside it), plus a borrowed trait to keep, if the slot has one. */
	static List<String> offer(ServerPlayer player, MasteryBook.Entry entry, List<RuneDef> runes, int slot, int salt, Set<String> alsoExclude) {
		Set<String> exclude = new HashSet<>(alsoExclude);
		for (int i = 0; i < MasteryRules.SLOTS; i++) {
			if (i != slot && !entry.traits().get(i).isEmpty()) {
				exclude.add(entry.traits().get(i));
			}
		}
		String borrowed = entry.borrowed(slot) ? entry.traits().get(slot) : "";
		if (!borrowed.isEmpty()) {
			exclude.add(borrowed);
		}
		List<String> offered = new ArrayList<>(MasteryTraits.offer(MasteryTraits.Profile.of(runes), entry.counters(), entry.casts(), entry.seed(), slot, salt,
			exclude, SpellMasteryApi.offers(player, entry.key(), runes, MasteryRules.rankOf(slot))));
		if (!borrowed.isEmpty()) {
			offered.add(borrowed);
		}
		return offered;
	}

	/** {@code entry} reached a new rank (from {@code from}): a toast for its owner, and a sound. */
	static void rose(ServerPlayer player, MasteryBook.Entry entry, List<RuneDef> runes, int from) {
		String name = nameOf(player, runes);
		int color = 0xE8C46A;
		for (RuneDef rune : dev.wildercord.spell.Knots.flatten(runes)) {
			if (rune.family() == dev.wildercord.spell.RuneFamily.EFFECT) {
				color = dev.wildercord.spell.RuneColors.of(rune);
				break;
			}
		}
		if (ServerPlayNetworking.canSend(player, Rise.TYPE)) {
			ServerPlayNetworking.send(player, new Rise(name, entry.rank(), color, entry.pendingSlot() >= 0, entry.seed()));
		}
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.8F + 0.1F * entry.rank());
		Fx.sound(player.level(), player.position(), SoundEvents.PLAYER_LEVELUP, 0.35F, 1.4F);
		player.sendSystemMessage(Component.translatable("message.wildercord.mastery.rank", Component.literal(name).withColor(color),
			Component.translatable("mastery.wildercord.rank." + entry.rank())).withColor(0xE8D8B0));
	}

	/** The name {@code player} knows a spell by: the name of a slot it's threaded in, or one made from its runes. */
	static String nameOf(ServerPlayer player, List<RuneDef> runes) {
		Spellbook book = Spellbooks.get(player);
		var tier = Spellbooks.tier(player);
		String key = Mastery.keyOf(runes);
		if (tier != null) {
			for (int i = 0; i < book.spells().size(); i++) {
				List<RuneDef> slot = SpellCaster.activeRunes(book, i, tier);
				if (!slot.isEmpty() && Mastery.keyOf(slot).equals(key)) {
					return SpellCaster.nameOf(player, book, i, slot);
				}
			}
		}
		return dev.wildercord.spell.SpellNames.auto(runes);
	}

	// ------------------------------------------------------------------ the player's choices

	/**
	 * A choice about the traits of the spell in Cord slot {@code spell}: checked against the server's own record of it.
	 *
	 * @return what to tell the player above the hotbar (why not, or what happened)
	 */
	public static Component request(ServerPlayer player, int kind, int spell, int slot, String trait) {
		if (!Config.get().mastery().enabled()) {
			return refuse("message.wildercord.mastery.off");
		}
		var tier = Spellbooks.tier(player);
		if (tier == null || spell < 0 || spell >= dev.wildercord.gear.SpellSlots.ALL || slot < 0 || slot >= MasteryRules.SLOTS) {
			return refuse("message.wildercord.no_cord");
		}
		List<RuneDef> runes = SpellCaster.activeRunes(Spellbooks.get(player), spell, tier);
		if (runes.isEmpty()) {
			return refuse("message.wildercord.mastery.no_record");
		}
		MasteryBook book = MasteryAttachments.book(player);
		MasteryBook.Entry entry = book.entry(Mastery.keyOf(runes)).orElse(null);
		if (entry == null) {
			return refuse("message.wildercord.mastery.no_record");
		}
		MasteryBook.Entry next = switch (kind) {
			case CHOOSE -> choose(entry, slot, trait);
			case REROLL -> reroll(player, entry, runes, slot);
			case UNBIND -> unbind(player, entry, runes, slot);
			default -> null;
		};
		if (next == null) {
			return refuse(switch (kind) {
				case CHOOSE -> "message.wildercord.mastery.not_offered";
				default -> entry.changed(slot) ? "message.wildercord.mastery.changed" : "message.wildercord.mastery.cant_change";
			});
		}
		if ((kind == REROLL || kind == UNBIND) && !player.isCreative()) {
			if (player.experienceLevel < MasteryRules.CHANGE_LEVELS) {
				return refuse("message.wildercord.mastery.levels", MasteryRules.CHANGE_LEVELS);
			}
			player.giveExperienceLevels(-MasteryRules.CHANGE_LEVELS);
		}
		next = offerIfDue(player, next, runes);
		player.setAttached(MasteryAttachments.MASTERY, book.with(next, Mastery.threaded(player)));
		Mastery.refreshLook(player);
		if (kind == CHOOSE) {
			MasteryTraits.Trait chosen = MasteryTraits.get(trait).orElseThrow();
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
			Fx.sound(player.level(), player.position(), dev.wildercord.content.WildercordSounds.CIRCLE_OPEN, 0.6F, 1.3F);
			return Component.translatable("message.wildercord.mastery.chosen", traitName(chosen)).withColor(0xE8D8B0);
		}
		Fx.sound(player.level(), player.position(), SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F);
		return Component.translatable(kind == REROLL ? "message.wildercord.mastery.rerolled" : "message.wildercord.mastery.unbound").withColor(0xE8D8B0);
	}

	private static Component refuse(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.RED);
	}

	/** A trait's name, as the language file has it (an outside trait's own name otherwise). */
	public static Component traitName(MasteryTraits.Trait trait) {
		return Component.translatableWithFallback("mastery.wildercord.trait." + trait.id().replace(':', '.'), trait.name());
	}

	/** Takes {@code trait} for {@code slot}, if it's what is waiting there and it was offered (or is the borrowed one, kept). */
	static MasteryBook.Entry choose(MasteryBook.Entry entry, int slot, String trait) {
		if (entry.pendingSlot() != slot || !entry.offer().contains(trait) || MasteryTraits.get(trait).isEmpty()) {
			return null;
		}
		return entry.withTrait(slot, trait, false).withOffer(List.of());
	}

	/** A fresh offer for the waiting slot, excluding what was offered, if the slot hasn't used its change. */
	static MasteryBook.Entry reroll(ServerPlayer player, MasteryBook.Entry entry, List<RuneDef> runes, int slot) {
		if (entry.pendingSlot() != slot || entry.changed(slot) || entry.offer().isEmpty()) {
			return null;
		}
		List<String> offered = offer(player, entry, runes, slot, 1, Set.copyOf(entry.offer()));
		if (offered.isEmpty()) {
			return null;
		}
		return entry.withChanged(slot).withOffer(offered);
	}

	/** Takes back the trait chosen for {@code slot}, to choose again from a fresh offer, if the slot hasn't used its change. */
	static MasteryBook.Entry unbind(ServerPlayer player, MasteryBook.Entry entry, List<RuneDef> runes, int slot) {
		if (!entry.settled(slot) || entry.changed(slot) || entry.pendingSlot() >= 0) {
			return null;
		}
		String old = entry.traits().get(slot);
		MasteryBook.Entry cleared = entry.withTrait(slot, "", false).withChanged(slot);
		return cleared.withOffer(offer(player, cleared, runes, slot, 2, Set.of(old)));
	}
}
