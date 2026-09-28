package dev.wildercord.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.cosmetic.CordStyles.Option;
import dev.wildercord.cosmetic.CordStyles.Style;
import dev.wildercord.player.Heart;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Cord cosmetics on the server: each player's chosen style (saved, and synced to everyone nearby
 * so they see it on the wrist), the options they've bought, and the two requests the Cosmetics
 * page sends. Every request is checked against {@link CordStyles}: nothing locked can be worn, and
 * buying takes the materials from the player's inventory (free in creative).
 */
public final class CordCosmetics {
	private CordCosmetics() {}

	public static final Codec<Style> STYLE_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.optionalFieldOf("material", Style.DEFAULT.material()).forGetter(Style::material),
		Codec.STRING.optionalFieldOf("glow", Style.DEFAULT.glow()).forGetter(Style::glow),
		Codec.STRING.optionalFieldOf("trail", Style.DEFAULT.trail()).forGetter(Style::trail)
	).apply(i, Style::new));
	public static final StreamCodec<ByteBuf, Style> STYLE_STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(32), Style::material, ByteBufCodecs.stringUtf8(32), Style::glow, ByteBufCodecs.stringUtf8(32), Style::trail, Style::new);

	/** How the player's Cord looks. Saved, kept through death, and synced to everyone nearby. */
	public static final AttachmentType<Style> STYLE = AttachmentRegistry.create(
		Wildercord.id("cord_style"),
		builder -> builder
			.persistent(STYLE_CODEC)
			.syncWith(STYLE_STREAM_CODEC, AttachmentSyncPredicate.all())
			.copyOnDeath()
	);

	/** Options bought in the Cosmetics page ({@code material:gold}...). Saved, kept through death, synced to the player. */
	public static final AttachmentType<List<String>> BOUGHT = AttachmentRegistry.create(
		Wildercord.id("cord_styles_bought"),
		builder -> builder
			.initializer(List::of)
			.persistent(Codec.STRING.listOf())
			.syncWith(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Wear this style (every part must be unlocked). */
	public record SetStyle(String material, String glow, String trail) implements CustomPacketPayload {
		public static final Type<SetStyle> TYPE = new Type<>(Wildercord.id("set_cord_style"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetStyle> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), SetStyle::material, ByteBufCodecs.stringUtf8(32), SetStyle::glow, ByteBufCodecs.stringUtf8(32), SetStyle::trail,
			SetStyle::new).cast();

		@Override
		public Type<SetStyle> type() {
			return TYPE;
		}
	}

	/** Buy an option with materials, and wear it. */
	public record BuyStyle(String key) implements CustomPacketPayload {
		public static final Type<BuyStyle> TYPE = new Type<>(Wildercord.id("buy_cord_style"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BuyStyle> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(64), BuyStyle::key, BuyStyle::new).cast();

		@Override
		public Type<BuyStyle> type() {
			return TYPE;
		}
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(SetStyle.TYPE, SetStyle.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BuyStyle.TYPE, BuyStyle.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SetStyle.TYPE, (payload, context) ->
			wear(context.player(), new Style(payload.material(), payload.glow(), payload.trail())));
		ServerPlayNetworking.registerGlobalReceiver(BuyStyle.TYPE, (payload, context) -> buy(context.player(), payload.key()));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> tidy(handler.player));
	}

	/** The style a player wears (the default if they never chose one). Works on both sides. */
	public static Style style(Player player) {
		return player.getAttachedOrElse(STYLE, Style.DEFAULT);
	}

	/** What the player has done toward unlocking styles. Works on both sides (the client for its own player). */
	public static CordStyles.Progress progress(Player player) {
		return new CordStyles.Progress(Heart.circles(player), Heart.grimoire(player), Heart.bossSlain(player),
			player.getAttachedOrElse(BOUGHT, List.of()));
	}

	/** Wears a style if every part of it is unlocked; returns whether it was. */
	public static boolean wear(ServerPlayer player, Style style) {
		if (!CordStyles.allowed(style, progress(player))) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.cosmetic.locked").withStyle(ChatFormatting.RED));
			return false;
		}
		if (!style.equals(style(player))) {
			player.setAttached(STYLE, style);
		}
		return true;
	}

	/** Buys an option with its materials (free in creative), then wears it. Returns whether it was bought. */
	public static boolean buy(ServerPlayer player, String key) {
		Option option = CordStyles.option(key);
		if (option == null || !CordStyles.buyable(option)) {
			return false;
		}
		CordStyles.Progress progress = progress(player);
		if (!CordStyles.unlocked(option, progress)) {
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(option.unlock().what()));
			int need = option.unlock().amount();
			if (!player.hasInfiniteMaterials()) {
				int have = player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), true, 0, player.inventoryMenu.getCraftSlots());
				if (have < need) {
					player.sendOverlayMessage(Component.translatable("message.wildercord.cosmetic.need", need, item.getName(new net.minecraft.world.item.ItemStack(item))).withStyle(ChatFormatting.RED));
					return false;
				}
				player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), false, need, player.inventoryMenu.getCraftSlots());
				player.inventoryMenu.broadcastChanges();
			}
			List<String> bought = new ArrayList<>(player.getAttachedOrElse(BOUGHT, List.of()));
			bought.add(option.key());
			player.setAttached(BOUGHT, List.copyOf(bought));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), WildercordSounds.RUNE_THREAD, SoundSource.PLAYERS, 0.8F, 1.2F);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
			player.sendOverlayMessage(Component.translatable("message.wildercord.cosmetic.bought",
				Component.translatable("cosmetic.wildercord." + option.group() + "." + option.id())).withColor(0xE8C46A));
		}
		return wear(player, style(player).with(option));
	}

	/** Puts back any part of a player's style that is no longer unlocked (checked as they join). */
	public static void tidy(ServerPlayer player) {
		Style style = player.getAttached(STYLE);
		if (style != null) {
			Style clean = CordStyles.sanitize(style, progress(player));
			if (!clean.equals(style)) {
				player.setAttached(STYLE, clean);
			}
		}
	}
}
