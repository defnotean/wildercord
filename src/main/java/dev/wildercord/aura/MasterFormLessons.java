package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.Duelist;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Survival teacher offers retain their original body and teacher; a full inventory cannot lose the readable lesson. */
public final class MasterFormLessons {
	private MasterFormLessons() {}
	public record Open(long nonce, boolean known) implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(Wildercord.id("master_form_lesson"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Open::nonce, ByteBufCodecs.BOOL, Open::known, Open::new).cast();
		@Override public Type<Open> type() { return TYPE; }
	}
	public record Accept(long nonce) implements CustomPacketPayload {
		public static final Type<Accept> TYPE = new Type<>(Wildercord.id("master_form_lesson_accept"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Accept> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Accept::nonce, Accept::new).cast();
		@Override public Type<Accept> type() { return TYPE; }
	}
	private record Offer(Duelist teacher, ServerLevel level, long until, long nonce, dev.wildercord.aura.arts.ReleasedArtOwner owner) {}
	private static final Map<ServerPlayer, Offer> OFFERS = new IdentityHashMap<>();
	private static long nonces;
	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Accept.TYPE, Accept.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Accept.TYPE, (packet, context) -> accept(context.player(), packet.nonce()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> OFFERS.remove(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> OFFERS.clear());
	}
	/** Sneak-use with an empty hand leaves the existing weapon-in-hand Master-trial introduction intact. */
	public static boolean offer(ServerPlayer player, Duelist teacher) {
		if (!player.getMainHandItem().isEmpty() || !player.isShiftKeyDown() || teacher.inDuel() || teacher.leaving()
			|| !player.isAlive() || teacher.level() != player.level() || teacher.distanceToSqr(player) > 36 || !teacher.hasLineOfSight(player)) return false;
		if (!MasterForms.eligibleLesson(player) && !MasterForms.data(player).learned()) return false;
		OFFERS.entrySet().removeIf(entry -> entry.getKey().isRemoved() || !entry.getValue().owner().valid()
			|| entry.getValue().until() < MasterForms.now(entry.getKey()));
		long nonce = ++nonces;
		OFFERS.put(player, new Offer(teacher, player.level(), MasterForms.now(player) + 2400, nonce,
			dev.wildercord.aura.arts.ReleasedArtOwner.capture(player)));
		if (ServerPlayNetworking.canSend(player, Open.TYPE)) ServerPlayNetworking.send(player, new Open(nonce, MasterForms.data(player).learned()));
		return true;
	}
	public static boolean accept(ServerPlayer player, long nonce) {
		Offer offer = OFFERS.get(player);
		if (offer == null || nonce != offer.nonce()) return false;
		OFFERS.remove(player);
		if (!offer.owner().valid() || offer.level() != player.level() || offer.until() < MasterForms.now(player) || !player.isAlive()
			|| offer.teacher().isRemoved() || !offer.teacher().isAlive() || offer.teacher().level() != player.level()
			|| offer.teacher().inDuel() || offer.teacher().leaving() || offer.teacher().distanceToSqr(player) > 36
			|| !offer.teacher().hasLineOfSight(player) || !MasterForms.learn(player)) return false;
		// The permanent readback exists before any inventory operation. Do not drop an irreplaceable copy into the world.
		if (player.getInventory().getFreeSlot() >= 0) player.getInventory().add(book());
		player.sendSystemMessage(Component.translatable("message.wildercord.wall_turn.learned"));
		return true;
	}
	public static ItemStack book() {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.ITEM_MODEL, Wildercord.id("wall_turn_lesson"));
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("The Scout's Unfinished Step"), "Iven Reed, road tutor", 0,
			List.of(Filterable.passThrough(Component.translatable("book.wildercord.wall_turn.1")),
				Filterable.passThrough(Component.translatable("book.wildercord.wall_turn.2")),
				Filterable.passThrough(Component.translatable("book.wildercord.wall_turn.3"))), true));
		return book;
	}
}
