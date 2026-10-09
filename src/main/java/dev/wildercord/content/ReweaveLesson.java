package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.TideScribe;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.content.dungeons.DungeonAltarBlockEntity;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.spell.MasterStudyRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Low Tide recovers a permanent ledger; only its completed live reading teaches Reweave. */
public final class ReweaveLesson {
	private ReweaveLesson() {}

	public static final String TITLE = "The Ebb Ledger";
	public static final String AUTHOR = "The Tide Scribe";
	private record Reading(ServerLevel level, long nonce, long request, long began, int page) {}
	private static final Map<ServerPlayer, Reading> READINGS = new IdentityHashMap<>();

	public record Show(long nonce, int page, boolean studying, boolean learnedNow, long request) implements CustomPacketPayload {
		public Show(long nonce, int page, boolean studying, boolean learnedNow) { this(nonce, page, studying, learnedNow, 0); }
		public static final Type<Show> TYPE = new Type<>(Wildercord.id("reweave_lesson_show"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Show> CODEC = StreamCodec.of((buf, value) -> {
			buf.writeLong(value.nonce()); buf.writeVarInt(value.page()); buf.writeBoolean(value.studying()); buf.writeBoolean(value.learnedNow()); buf.writeLong(value.request());
		}, buf -> new Show(buf.readLong(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readLong()));
		@Override public Type<Show> type() { return TYPE; }
	}

	/** Opens or closes a saved reading; the client cannot supply the permanent copying receipt. */
	public record Retrieve(long request, boolean close) implements CustomPacketPayload {
		public static final Type<Retrieve> TYPE = new Type<>(Wildercord.id("reweave_lesson_retrieve"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Retrieve> CODEC = StreamCodec.of((buf, value) -> {
			buf.writeLong(value.request()); buf.writeBoolean(value.close());
		}, buf -> new Retrieve(buf.readLong(), buf.readBoolean()));
		@Override public Type<Retrieve> type() { return TYPE; }
	}

	/** A bounded page action, not a learned bit, entitlement, book, location, or completion claim. */
	public record Turn(long nonce, int page) implements CustomPacketPayload {
		public static final Type<Turn> TYPE = new Type<>(Wildercord.id("reweave_lesson_turn"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Turn> CODEC = StreamCodec.of((buf, value) -> {
			buf.writeLong(value.nonce()); buf.writeVarInt(value.page());
		}, buf -> new Turn(buf.readLong(), buf.readVarInt()));
		@Override public Type<Turn> type() { return TYPE; }
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Show.TYPE, Show.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Turn.TYPE, Turn.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Retrieve.TYPE, Retrieve.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Turn.TYPE, (packet, context) -> turn(context.player(), packet));
		ServerPlayNetworking.registerGlobalReceiver(Retrieve.TYPE, (packet, context) -> {
			if (packet.request() == 0) return;
			if (packet.close()) {
				Reading reading = READINGS.get(context.player());
				if (reading != null && reading.request() == packet.request()) READINGS.remove(context.player());
			} else openSaved(context.player(), packet.request());
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> RelayLesson.removeInvalid(READINGS, ReweaveLesson::valid, ReweaveLesson::interrupted));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> READINGS.remove(handler.player));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> retire(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> READINGS.remove(oldPlayer));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) retire(player);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> READINGS.clear());
	}

	/** A quiet Tide altar offers an optional physical copy; the Grimoire never needs this block. */
	public static void open(ServerPlayer player, BlockPos pos) {
		if (!connected(player) || !player.level().hasChunkAt(pos)
			|| player.distanceToSqr(Vec3.atCenterOf(pos)) > MasterStudyRules.REACH * MasterStudyRules.REACH
			|| !player.level().clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(pos),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getBlockPos().equals(pos)) return;
		if (!(player.level().getBlockEntity(pos) instanceof DungeonAltarBlockEntity altar) || !quiet(player.level(), pos, altar)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.reweave_lesson.quiet"));
			return;
		}
		if (!MasterStudies.hasReweaveLesson(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.reweave_lesson.locked"));
			return;
		}
		if (MasterStudies.remember(player, MasterStudyRules.REWEAVE_COPIED)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.reweave_lesson.copied").withColor(0x7FDAD4));
		}
		copy(player);
		openSaved(player, 0);
	}

	private static void openSaved(ServerPlayer player, long request) {
		if (!connected(player)) return;
		if (!MasterStudies.hasReweaveLesson(player) || !MasterStudies.knowsReweave(player) && !MasterStudies.mayStudyReweave(player)) {
			player.sendOverlayMessage(Component.translatable(MasterStudies.hasReweaveLesson(player)
				? "message.wildercord.reweave_lesson.locked" : "message.wildercord.reweave_lesson.uncopied"));
			ServerPlayNetworking.send(player, new Show(0, -1, false, false, request));
			return;
		}
		// The permanent Low Tide feat also recovers this receipt in saves from before the ledger existed.
		MasterStudies.remember(player, MasterStudyRules.REWEAVE_COPIED);
		READINGS.remove(player);
		if (MasterStudies.knowsReweave(player)) {
			ServerPlayNetworking.send(player, new Show(0, 0, false, false, request));
			return;
		}
		long nonce;
		do { nonce = player.getRandom().nextLong(); } while (nonce == 0);
		Reading reading = new Reading(player.level(), nonce, request, player.level().getGameTime(), 0);
		if (!valid(player, reading)) return;
		READINGS.put(player, reading);
		ServerPlayNetworking.send(player, new Show(nonce, 0, true, false, request));
	}

	private static void turn(ServerPlayer player, Turn packet) {
		Reading reading = READINGS.get(player);
		if (reading == null || reading.nonce() != packet.nonce()) return;
		if (packet.page() == -1) { READINGS.remove(player); return; }
		if (!valid(player, reading)) {
			READINGS.remove(player);
			interrupted(player, reading);
			return;
		}
		if (!MasterStudyRules.nextPage(reading.page(), packet.page())) return;
		if (packet.page() < MasterStudyRules.PAGES) {
			READINGS.put(player, new Reading(reading.level(), reading.nonce(), reading.request(), reading.began(), packet.page()));
			ServerPlayNetworking.send(player, new Show(reading.nonce(), packet.page(), true, false));
			return;
		}
		READINGS.remove(player);
		if (MasterStudies.learnReweave(player)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.reweave_lesson.learned").withColor(0x7FDAD4));
			ServerPlayNetworking.send(player, new Show(reading.nonce(), 2, false, true));
		}
	}

	private static void interrupted(ServerPlayer player, Reading reading) {
		if (player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player) return;
		player.sendOverlayMessage(Component.translatable("message.wildercord.reweave_lesson.interrupted"));
		ServerPlayNetworking.send(player, new Show(reading.nonce(), -1, false, false));
	}

	private static void retire(ServerPlayer player) {
		Reading reading = READINGS.remove(player);
		if (reading != null) interrupted(player, reading);
	}

	private static boolean valid(ServerPlayer player, Reading reading) {
		if (!connected(player) || player.level() != reading.level() || !MasterStudies.mayStudyReweave(player)) return false;
		long elapsed = reading.level().getGameTime() - reading.began();
		return MasterStudyRules.liveReading(elapsed);
	}

	private static boolean connected(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && !player.isSpectator()
			&& player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
	}

	/** The boss must have fallen here; legacy victories can always recover the ledger through the Grimoire. */
	private static boolean quiet(ServerLevel level, BlockPos pos, DungeonAltarBlockEntity altar) {
		var state = altar.getBlockState();
		return state.getValue(DungeonAltarBlock.KIND) == DungeonAltarBlock.Kind.TIDE
			&& state.getValue(DungeonAltarBlock.AWAKE) && altar.isSlain()
			&& level.getEntitiesOfClass(TideScribe.class, new AABB(pos).inflate(48), TideScribe::isAlive).isEmpty();
	}

	public static ItemStack book() {
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(TITLE), AUTHOR, 0,
			List.of(Filterable.passThrough(page(0)), Filterable.passThrough(page(1)), Filterable.passThrough(page(2))), true));
		stack.set(DataComponents.ITEM_MODEL, Wildercord.id("reweave_lesson"));
		return stack;
	}

	public static Component page(int page) {
		return Component.translatable("book.wildercord.reweave_lesson." + (Math.clamp(page, 0, 2) + 1));
	}

	/** A full inventory never loses the lesson. The altar offers the original copy later, then one replacement if lost. */
	private static void copy(ServerPlayer player) {
		String key = Heart.discovered(player, MasterStudyRules.REWEAVE_COPY) ? MasterStudyRules.REWEAVE_REPLACEMENT : MasterStudyRules.REWEAVE_COPY;
		if (Heart.discovered(player, key)) return;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (Wildercord.id("reweave_lesson").equals(stack.get(DataComponents.ITEM_MODEL))) return;
		}
		if (player.getInventory().add(book())) MasterStudies.remember(player, key);
		else player.sendSystemMessage(Component.translatable("message.wildercord.reweave_lesson.retained").withColor(0xB7CEC9));
	}
}
