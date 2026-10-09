package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.spell.LessonPackRules;
import dev.wildercord.spell.LessonPackRules.Lesson;
import dev.wildercord.spell.MasterStudyRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.Map;

/** A boss feat recovers the pack lesson in the Grimoire; only one completed live reading teaches its rune. */
public final class PackLesson {
	private PackLesson() {}

	private record Reading(Lesson lesson, ServerLevel level, long nonce, long request, long began, int page) {}
	private static final Map<ServerPlayer, Reading> READINGS = new IdentityHashMap<>();

	public record Show(int lesson, long nonce, int page, boolean studying, boolean learnedNow, long request) implements CustomPacketPayload {
		public static final Type<Show> TYPE = new Type<>(Wildercord.id("pack_lesson_show"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Show> CODEC = StreamCodec.of((buf, value) -> {
			buf.writeVarInt(value.lesson()); buf.writeLong(value.nonce()); buf.writeVarInt(value.page()); buf.writeBoolean(value.studying());
			buf.writeBoolean(value.learnedNow()); buf.writeLong(value.request());
		}, buf -> new Show(buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readLong()));
		@Override public Type<Show> type() { return TYPE; }
	}

	/** Opens or closes one lesson's saved reading; the client names a lesson, never an entitlement. */
	public record Retrieve(int lesson, long request, boolean close) implements CustomPacketPayload {
		public static final Type<Retrieve> TYPE = new Type<>(Wildercord.id("pack_lesson_retrieve"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Retrieve> CODEC = StreamCodec.of((buf, value) -> {
			buf.writeVarInt(value.lesson()); buf.writeLong(value.request()); buf.writeBoolean(value.close());
		}, buf -> new Retrieve(buf.readVarInt(), buf.readLong(), buf.readBoolean()));
		@Override public Type<Retrieve> type() { return TYPE; }
	}

	/** A bounded page action, not a learned bit or completion claim. */
	public record Turn(long nonce, int page) implements CustomPacketPayload {
		public static final Type<Turn> TYPE = new Type<>(Wildercord.id("pack_lesson_turn"));
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
			if (packet.request() == 0 || packet.lesson() < 0 || packet.lesson() >= LessonPackRules.ALL.size()) return;
			if (packet.close()) {
				Reading reading = READINGS.get(context.player());
				if (reading != null && reading.request() == packet.request()) READINGS.remove(context.player());
			} else openSaved(context.player(), LessonPackRules.ALL.get(packet.lesson()), packet.request());
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> RelayLesson.removeInvalid(READINGS, PackLesson::valid, PackLesson::interrupted));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> READINGS.remove(handler.player));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> retire(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> READINGS.remove(oldPlayer));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) retire(player);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> READINGS.clear());
	}

	private static void openSaved(ServerPlayer player, Lesson lesson, long request) {
		if (!connected(player)) return;
		if (!MasterStudies.hasLesson(player, lesson) || !MasterStudies.knows(player, lesson) && !MasterStudies.mayStudy(player, lesson)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord." + lesson.path + "_lesson."
				+ (MasterStudies.hasLesson(player, lesson) ? "locked" : "uncopied")));
			ServerPlayNetworking.send(player, new Show(lesson.ordinal(), 0, -1, false, false, request));
			return;
		}
		// The permanent boss feat also recovers this receipt in saves from before the pack existed.
		MasterStudies.remember(player, lesson.copied);
		READINGS.remove(player);
		if (MasterStudies.knows(player, lesson)) {
			ServerPlayNetworking.send(player, new Show(lesson.ordinal(), 0, 0, false, false, request));
			return;
		}
		long nonce;
		do { nonce = player.getRandom().nextLong(); } while (nonce == 0);
		Reading reading = new Reading(lesson, player.level(), nonce, request, player.level().getGameTime(), 0);
		if (!valid(player, reading)) return;
		READINGS.put(player, reading);
		ServerPlayNetworking.send(player, new Show(lesson.ordinal(), nonce, 0, true, false, request));
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
		int lesson = reading.lesson().ordinal();
		if (packet.page() < MasterStudyRules.PAGES) {
			READINGS.put(player, new Reading(reading.lesson(), reading.level(), reading.nonce(), reading.request(), reading.began(), packet.page()));
			ServerPlayNetworking.send(player, new Show(lesson, reading.nonce(), packet.page(), true, false, 0));
			return;
		}
		READINGS.remove(player);
		if (MasterStudies.learn(player, reading.lesson())) {
			player.sendSystemMessage(Component.translatable("message.wildercord." + reading.lesson().path + "_lesson.learned").withColor(0x7FDAD4));
			ServerPlayNetworking.send(player, new Show(lesson, reading.nonce(), 2, false, true, 0));
		}
	}

	private static void interrupted(ServerPlayer player, Reading reading) {
		if (player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player) return;
		player.sendOverlayMessage(Component.translatable("message.wildercord." + reading.lesson().path + "_lesson.interrupted"));
		ServerPlayNetworking.send(player, new Show(reading.lesson().ordinal(), reading.nonce(), -1, false, false, 0));
	}

	private static void retire(ServerPlayer player) {
		Reading reading = READINGS.remove(player);
		if (reading != null) interrupted(player, reading);
	}

	private static boolean valid(ServerPlayer player, Reading reading) {
		if (!connected(player) || player.level() != reading.level() || !MasterStudies.mayStudy(player, reading.lesson())) return false;
		return MasterStudyRules.liveReading(reading.level().getGameTime() - reading.began());
	}

	private static boolean connected(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && !player.isSpectator()
			&& player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
	}

	public static Component page(Lesson lesson, int page) {
		return Component.translatable("book.wildercord." + lesson.path + "_lesson." + (Math.clamp(page, 0, 2) + 1));
	}
}
