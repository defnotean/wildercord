package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.config.Config;
import dev.wildercord.net.PacketThrottle;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sword strings on the server: the player's client reads its swings ({@link StringReader}) and, when a string is played, asks
 * for its art ({@link Perform}); the server checks the request against everything it knows and performs the art, or refuses it
 * ({@link Refused}). The client can't be trusted with any of it, so the server checks:
 * <ul>
 * <li>that aura and strings are on, the player alive and not a spectator, an aura weapon in hand, and the art theirs (stage
 *     reached, open to them), its swings as read fitting its string;</li>
 * <li>that it's rested, its condition met (the Final Art's full pool) and its price there (an art never spends past empty);</li>
 * <li>that the swings happened: the server saw at least that many swings of the player's own (punches and thrusts) in the time
 *     the string can take, the last of them just now, and the perfect guard or Aura Step a counter or a step cut needs;</li>
 * <li>and a small allowance of requests a second ({@link StringRules#REQUEST_BURST}).</li>
 * </ul>
 * What the server can't see exactly (how full a swing was, whether it crouched, leapt or ran at that instant) it takes from
 * the client: the network can blur those by a tick, and a client that lied about them would gain an art a moment sooner.
 *
 * <p>Performed, an art's price is spent, its rest begins ({@link #COOLDOWNS}, saved and synced to its owner for the reader),
 * the {@link AuraApi#onString} hooks hear of it, and the first one goes into the Grimoire. A perfect guard and an Aura Step are
 * told to the player's client ({@link Cue}), so its reader can mark a counter or a step cut.</p>
 */
public final class SwordStrings {
	private SwordStrings() {}

	// ------------------------------------------------------------------ payloads

	/** Client to server: the client read {@code art}'s string in the player's swings; {@code marks} are those swings, oldest first. */
	public record Perform(String art, List<Integer> marks) implements CustomPacketPayload {
		public static final Type<Perform> TYPE = new Type<>(Wildercord.id("aura_string"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Perform> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Perform::art, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(SwordString.MAX_LENGTH)), Perform::marks,
			Perform::new).cast();

		public Perform {
			marks = List.copyOf(marks);
		}

		@Override
		public Type<Perform> type() {
			return TYPE;
		}
	}

	/** Server to client: a perfect guard or an Aura Step just happened ({@link StringReader.Cue}'s ordinal), for the reader. */
	public record Cue(int cue) implements CustomPacketPayload {
		public static final Type<Cue> TYPE = new Type<>(Wildercord.id("aura_string_cue"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Cue> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Cue::cue, Cue::new).cast();

		@Override
		public Type<Cue> type() {
			return TYPE;
		}
	}

	/** Server to client: the server didn't let {@code art} go ({@link Refusal}'s ordinal), so the indicator shows a fumble. */
	public record Refused(String art, int reason) implements CustomPacketPayload {
		public static final Type<Refused> TYPE = new Type<>(Wildercord.id("aura_string_refused"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Refused> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Refused::art, ByteBufCodecs.VAR_INT, Refused::reason, Refused::new).cast();

		@Override
		public Type<Refused> type() {
			return TYPE;
		}
	}

	/** Why an art didn't go. */
	public enum Refusal {
		/** Aura or strings off, a spectator, dead, the art not theirs, or its swings not its string: nothing to say. */
		CLOSED,
		/** No aura weapon in hand. */
		NO_WEAPON,
		/** Still resting. */
		NOT_READY,
		/** Too little aura for its price. */
		NO_AURA,
		/** Its condition unmet (the Final Art's full pool). */
		CONDITION,
		/** The server didn't see the swings, guard or step it needs (a slow connection, or a client that made them up). */
		UNSEEN,
		/** Silenced by a Hollow swordsman's Null Parry: no arts until it passes (the server alone knows; the client hears it here). */
		SILENCED;

		public static Refusal of(int ordinal) {
			Refusal[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : CLOSED;
		}
	}

	// ------------------------------------------------------------------ the arts' rests

	/** When each of a player's arts is ready again (game time), by art id. Only arts resting (or lately rested) are in it. */
	public record Cooldowns(Map<String, Long> readyAt) {
		public static final Cooldowns NONE = new Cooldowns(Map.of());
		public static final Codec<Cooldowns> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(Cooldowns::new, Cooldowns::readyAt);
		public static final StreamCodec<ByteBuf, Cooldowns> STREAM_CODEC = ByteBufCodecs.<ByteBuf, String, Long, Map<String, Long>>map(HashMap::new,
			ByteBufCodecs.stringUtf8(128), ByteBufCodecs.VAR_LONG, 256).map(Cooldowns::new, c -> new HashMap<>(c.readyAt()));

		public Cooldowns {
			readyAt = readyAt == null ? Map.of() : Map.copyOf(readyAt);
		}

		public long readyAt(String id) {
			return readyAt.getOrDefault(id, 0L);
		}

		/** The same with {@code id} resting until {@code until}, and every rest that ended by {@code now} let go. */
		public Cooldowns rest(String id, long until, long now) {
			Map<String, Long> next = new HashMap<>();
			readyAt.forEach((k, v) -> {
				if (v > now) {
					next.put(k, v);
				}
			});
			next.put(id, until);
			return new Cooldowns(next);
		}
	}

	/** Each player's arts' rests: saved (game time carries over a restart), kept through death, synced to its owner (the reader asks). */
	public static final AttachmentType<Cooldowns> COOLDOWNS = AttachmentRegistry.create(
		Wildercord.id("aura_arts"),
		builder -> builder
			.initializer(() -> Cooldowns.NONE)
			.persistent(Cooldowns.CODEC)
			.syncWith(Cooldowns.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** When {@code player}'s art {@code id} is ready again (both sides: a client knows its own player's). */
	public static long readyAt(Player player, String id) {
		return player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE).readyAt(id);
	}

	/** Whether {@code art} has rested, for {@code player} at their level's time now. */
	public static boolean rested(Player player, AuraApi.StringArt art) {
		return player.level().getGameTime() >= readyAt(player, art.id());
	}

	// ------------------------------------------------------------------ what the server saw

	/** A player's swings as the server saw them (game times, the latest last), and their last perfect guard and Aura Step. */
	private static final class Seen {
		final long[] swings = new long[SwordString.MAX_LENGTH + 2];
		int count;
		long guardAt = Long.MIN_VALUE / 4;
		long stepAt = Long.MIN_VALUE / 4;

		void swing(long now) {
			System.arraycopy(swings, 1, swings, 0, swings.length - 1);
			swings[swings.length - 1] = now;
			count = Math.min(swings.length, count + 1);
		}

		/** How many swings came at or after {@code since}. */
		int since(long since) {
			int n = 0;
			for (int i = swings.length - 1; i >= swings.length - count; i--) {
				if (swings[i] >= since) {
					n++;
				}
			}
			return n;
		}

		long last() {
			return count == 0 ? Long.MIN_VALUE / 4 : swings[swings.length - 1];
		}
	}

	private static final Map<UUID, Seen> SEEN = new HashMap<>();
	private static final PacketThrottle REQUESTS = new PacketThrottle(StringRules.REQUEST_BURST, StringRules.REQUEST_TICKS);

	/** A swing the server saw: the punch every swing sends, or a spear's thrust (see {@code mixin.SwordStringsSeenMixin}). */
	public static void swung(ServerPlayer player) {
		if (Aura.stage(player) <= AuraRules.NONE) {
			return;
		}
		SEEN.computeIfAbsent(player.getUUID(), k -> new Seen()).swing(player.level().getGameTime());
	}

	/** A perfect guard or an Aura Step: remembered, and told to the player's client so its reader marks the next swing. */
	public static void cue(ServerPlayer player, StringReader.Cue cue) {
		Seen seen = SEEN.computeIfAbsent(player.getUUID(), k -> new Seen());
		long now = player.level().getGameTime();
		if (cue == StringReader.Cue.GUARD) {
			seen.guardAt = now;
		} else {
			seen.stepAt = now;
		}
		if (ServerPlayNetworking.canSend(player, Cue.TYPE)) {
			ServerPlayNetworking.send(player, new Cue(cue.ordinal()));
		}
	}

	/**
	 * The counter and step-cut marks a swing now carries, as far as the server knows: a perfect guard or an Aura Step within its
	 * moment (for how the swing's trail is cut, see {@link AuraFx#swung}; the reader on the client is what plays strings).
	 */
	static int cues(ServerPlayer player) {
		Seen seen = SEEN.get(player.getUUID());
		if (seen == null) {
			return 0;
		}
		long now = player.level().getGameTime();
		int marks = 0;
		if (now - seen.guardAt <= StringRules.COUNTER_TICKS) {
			marks |= SwordString.Token.COUNTER.bit();
		}
		if (now - seen.stepAt <= StringRules.STEP_CUT_TICKS) {
			marks |= SwordString.Token.STEP.bit();
		}
		return marks;
	}

	/** Whether the server saw what {@code art}'s string needs, just now: enough swings in its time, the last just now, its guard or step. */
	static boolean saw(ServerPlayer player, AuraApi.StringArt art) {
		Seen seen = SEEN.get(player.getUUID());
		if (seen == null) {
			return false;
		}
		long now = player.level().getGameTime();
		SwordString string = art.string();
		int recover = StringRules.recover(player.getCurrentItemAttackStrengthDelay());
		long span = StringRules.span(string.length(), window(), recover) + StringRules.SEEN_SLACK;
		if (seen.since(now - span) < string.length() || now - seen.last() > StringRules.LAST_SWING_SLACK) {
			return false;
		}
		if (string.has(SwordString.Token.COUNTER) && seen.guardAt < now - span - StringRules.COUNTER_TICKS) {
			return false;
		}
		return !string.has(SwordString.Token.STEP) || seen.stepAt >= now - span - StringRules.STEP_CUT_TICKS;
	}

	/** The window the server's setting gives (ticks). */
	static int window() {
		return StringRules.windowTicks(Config.get().aura().strings().windowSeconds());
	}

	// ------------------------------------------------------------------ checking and performing

	/**
	 * Why {@code player} can't perform {@code art} now with these swings, or empty if they can. The client asks the same of its
	 * own knowledge first (all but the swings the server saw), so a refusal here is rare: a race, a slow connection, or a cheat.
	 */
	public static Optional<Refusal> check(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks) {
		if (!player.isAlive() || player.isSpectator() || !Aura.enabled(player) || !Config.get().aura().strings().enabled()) {
			return Optional.of(Refusal.CLOSED);
		}
		if (Aura.stage(player) < art.stage() || !art.available().test(player) || !art.string().fits(toArray(marks))) {
			return Optional.of(Refusal.CLOSED);
		}
		if (!Aura.holdsWeapon(player)) {
			return Optional.of(Refusal.NO_WEAPON);
		}
		if (dev.wildercord.aura.arts.ArtWards.silenced(player)) {
			return Optional.of(Refusal.SILENCED);
		}
		if (!rested(player, art)) {
			return Optional.of(Refusal.NOT_READY);
		}
		if (!art.condition().met(player)) {
			return Optional.of(Refusal.CONDITION);
		}
		if (Aura.aura(player) < price(player, art) - 1.0E-4) {
			return Optional.of(Refusal.NO_AURA);
		}
		if (!saw(player, art)) {
			return Optional.of(Refusal.UNSEEN);
		}
		return Optional.empty();
	}

	/** A request from the player's client: checked, then performed or refused. */
	static void request(ServerPlayer player, Perform payload) {
		Optional<AuraApi.StringArt> art = AuraApi.artOf(player, payload.art());
		if (art.isEmpty()) {
			refuse(player, payload.art(), null, Refusal.CLOSED);
			return;
		}
		Optional<Refusal> why = check(player, art.get(), payload.marks());
		if (why.isPresent()) {
			refuse(player, payload.art(), art.get(), why.get());
			return;
		}
		perform(player, art.get(), payload.marks());
	}

	/** Performs {@code art} (already checked): the art itself, then its price, its rest, the hooks and the Grimoire. */
	public static boolean perform(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks) {
		long now = player.level().getGameTime();
		AuraApi.StringContext context = new AuraApi.StringContext(art, marks, struck(player), now);
		// Its price as it goes, before what it does builds momentum (a tier reached by the art itself makes the next one cheaper).
		double cost = price(player, art);
		boolean done;
		AuraApi.StringArt outer = performing;
		performing = art;
		try {
			done = art.performer().perform(player, context);
		} catch (RuntimeException e) {
			Wildercord.LOGGER.warn("Art {} threw", art.id(), e);
			done = false;
		} finally {
			performing = outer;
		}
		if (!done) {
			refuse(player, art.id(), art, Refusal.CLOSED);
			return false;
		}
		// Checked to be there, so this never spends past empty (an art that spent some itself takes what's left).
		double price = Math.min(cost, Aura.aura(player));
		if (price > 0) {
			Aura.spend(player, price, "art:" + art.id());
		}
		if (art.cooldownTicks() > 0) {
			Cooldowns rests = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
			player.setAttached(COOLDOWNS, rests.rest(art.id(), now + art.cooldownTicks(), now));
		}
		for (AuraApi.StringHook hook : AuraApi.stringHooks()) {
			try {
				hook.performed(player, art, context);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A sword string hook threw; skipping it", e);
			}
		}
		Grimoire.unlock(player, "aura:sword_string");
		// A method's own art goes into the Grimoire the first time it's played.
		if (!AuraApi.artMethod(art.id()).isEmpty()) {
			Grimoire.unlock(player, grimoireKey(art.id()));
		}
		return true;
	}

	/** The art being performed now (on the server thread, while its performer runs), or null: what an art's strikes belong to. */
	private static AuraApi.StringArt performing;

	public static AuraApi.StringArt performing() {
		return performing;
	}

	/**
	 * What {@code art} costs {@code player} now: its price, less at each tier of momentum ({@link Momentum#price}), and only the
	 * awakening's share of that while awakened ({@link Awakening#priceShare}: nothing, by default). Both sides.
	 */
	public static double price(Player player, AuraApi.StringArt art) {
		double price = Momentum.on(player) ? Momentum.price(player, art) : art.cost();
		return AwakeningRules.price(price, Awakening.priceShare(player));
	}

	/** The Grimoire entry a method's own art writes the first time it's played (see {@link ArtRules#grimoireKey}). */
	public static String grimoireKey(String artId) {
		return ArtRules.grimoireKey(artId);
	}

	/** The creature the player's last swing struck, by the server's own record (set as a blow lands), if it was just now. */
	static LivingEntity struck(ServerPlayer player) {
		LivingEntity last = player.getLastHurtMob();
		return last != null && player.tickCount - player.getLastHurtMobTimestamp() <= 2 ? last : null;
	}

	/** Tells the player's client its art didn't go, and (for what a player can do something about) why, above the hotbar. */
	private static void refuse(ServerPlayer player, String id, AuraApi.StringArt art, Refusal why) {
		if (ServerPlayNetworking.canSend(player, Refused.TYPE)) {
			ServerPlayNetworking.send(player, new Refused(id, why.ordinal()));
		}
		Component line = art == null ? null : refusal(player, art, why);
		if (line != null) {
			player.sendOverlayMessage(line);
		}
	}

	/** The line that says why {@code art} didn't go, or null for a refusal with nothing to say. Both sides. */
	public static Component refusal(AuraApi.StringArt art, Refusal why) {
		return refusal(null, art, why);
	}

	/** The same, for {@code player} (their price at their momentum, and what the condition waits on for them). Both sides. */
	public static Component refusal(Player player, AuraApi.StringArt art, Refusal why) {
		Component name = AuraApi.artName(player, art);
		return switch (why) {
			case NO_WEAPON -> Component.translatable("message.wildercord.aura.no_weapon").withColor(0xA89CC8);
			case NOT_READY -> Component.translatable("message.wildercord.aura.art.not_ready", name).withColor(0xA89CC8);
			case NO_AURA -> Component.translatable("message.wildercord.aura.art.no_aura", name, trim(player == null ? art.cost() : price(player, art)))
				.withColor(0xA89CC8);
			case CONDITION -> Component.translatable(art.condition().hintKey(player), name).withColor(0xA89CC8);
			case SILENCED -> Component.translatable("message.wildercord.aura.art.silenced", name).withColor(0xA89CC8);
			case CLOSED, UNSEEN -> null;
		};
	}

	private static String trim(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	private static int[] toArray(List<Integer> marks) {
		int[] out = new int[marks.size()];
		for (int i = 0; i < out.length; i++) {
			Integer m = marks.get(i);
			out[i] = m == null ? 0 : m;
		}
		return out;
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Perform.TYPE, Perform.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Cue.TYPE, Cue.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Refused.TYPE, Refused.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Perform.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) {
				request(context.player(), payload);
			}
		});
		PlaceholderArts.register();
		// Each method's own arts, on the same strings (the common ones step aside for them).
		dev.wildercord.aura.arts.MethodArts.init();
	}

	static void forget(UUID id) {
		SEEN.remove(id);
		REQUESTS.forget(id);
	}

	static void clear() {
		SEEN.clear();
		REQUESTS.clear();
	}
}
