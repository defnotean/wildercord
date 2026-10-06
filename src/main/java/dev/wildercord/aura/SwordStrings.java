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
 * Movement and charge marks come from the server before vanilla changes them. Client marks only describe the request;
 * the performed context uses a matching, one-use suffix of observed strokes. At most one stroke is admitted per server
 * tick, so a lag-bunched string may be refused rather than treating duplicate packets as new sword work.
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

	private static final Map<UUID, SwordStringLedger> SEEN = new HashMap<>();
	private static final PacketThrottle REQUESTS = new PacketThrottle(StringRules.REQUEST_BURST, StringRules.REQUEST_TICKS);

	private static long observedTick(ServerPlayer player) {
		return player.level().getServer().getTickCount();
	}

	private static SwordStringLedger.Context observationContext(ServerPlayer player) {
		return new SwordStringLedger.Context(player, player.level(), player.getMainHandItem());
	}

	private static boolean observing(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && !player.isSpectator() && Aura.enabled(player)
			&& Config.get().aura().strings().enabled() && Aura.stage(player) >= AuraRules.GLOW && Aura.holdsWeapon(player)
			&& !player.isUsingItem() && !player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)
			&& !MastersArts.committed(player);
	}

	/** The same movement predicates as the client reader, sampled only from the server's current state. */
	static int observedMarks(ServerPlayer player) {
		int marks = SwordString.Token.SWING.bit();
		if (player.getAttackStrengthScale(0.5F) >= AuraRules.FULL_SWING - 1.0E-4) marks |= SwordString.Token.FULL.bit();
		if (player.isShiftKeyDown()) marks |= SwordString.Token.LOW.bit();
		if (!player.onGround() && !player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isPassenger()
			&& !player.getAbilities().flying && !player.isFallFlying() && !player.isSwimming()) marks |= SwordString.Token.LEAP.bit();
		if (player.isSprinting()) marks |= SwordString.Token.RUN.bit();
		return marks;
	}

	/** Player.attack runs before its trailing Punch, and resets both charge and (on knockback) sprinting. */
	public static void attackBegins(ServerPlayer player) {
		if (!observing(player)) return;
		SEEN.computeIfAbsent(player.getUUID(), key -> new SwordStringLedger()).attack(observedTick(player), observedMarks(player),
			StringRules.recover(player.getCurrentItemAttackStrengthDelay()), observationContext(player));
	}

	/** A punch, before vanilla resets its attack ticker. Kept as the public ordinary-swing entrypoint. */
	public static void swung(ServerPlayer player) {
		if (!observing(player)) return;
		SEEN.computeIfAbsent(player.getUUID(), key -> new SwordStringLedger()).punch(observedTick(player), observedMarks(player),
			StringRules.recover(player.getCurrentItemAttackStrengthDelay()), observationContext(player));
	}

	/** A piercing component makes one stroke regardless of how many entities its thrust hits; it sends no Punch. */
	public static void thrust(ServerPlayer player) {
		if (!observing(player)) return;
		SEEN.computeIfAbsent(player.getUUID(), key -> new SwordStringLedger()).thrust(observedTick(player), observedMarks(player),
			StringRules.recover(player.getCurrentItemAttackStrengthDelay()), observationContext(player));
	}

	/** A server-earned cue belongs to its first observed swing, and is also sent to the client reader. */
	public static void cue(ServerPlayer player, StringReader.Cue cue) {
		SEEN.computeIfAbsent(player.getUUID(), key -> new SwordStringLedger()).cue(observedTick(player), cue == StringReader.Cue.GUARD,
			observationContext(player));
		if (ServerPlayNetworking.canSend(player, Cue.TYPE)) ServerPlayNetworking.send(player, new Cue(cue.ordinal()));
	}

	/** Cosmetic trail marks include the stroke just recorded; they do not make its consumed cue reusable as evidence. */
	static int cues(ServerPlayer player) {
		SwordStringLedger seen = SEEN.get(player.getUUID());
		if (seen == null) return 0;
		long now = observedTick(player);
		int marks = seen.cues(now, observationContext(player));
		SwordStringLedger.Stroke last = seen.last();
		if (last != null && last.tick() == now) marks |= last.marks() & (SwordString.Token.COUNTER.bit() | SwordString.Token.STEP.bit());
		return marks;
	}

	/** A read-only proof is checked again and reserved at the actual request boundary. */
	private static Optional<SwordStringLedger.Proof> proof(ServerPlayer player, AuraApi.StringArt art) {
		SwordStringLedger seen = SEEN.get(player.getUUID());
		return seen == null ? Optional.empty() : seen.proof(art.string(), observedTick(player), window(), observationContext(player));
	}

	static boolean saw(ServerPlayer player, AuraApi.StringArt art) {
		return proof(player, art).isPresent();
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
		if (MastersArts.committed(player)) return Optional.of(Refusal.NOT_READY);
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
		if (!rested(player, art) || Clashes.holding(player)) {
			// (An art held back in a clash: the others wait for it.)
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
        if (dev.wildercord.cast.ExciseCasting.blocking(player)) return;
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
		Optional<SwordStringLedger.Proof> observed = proof(player, art.get());
		SwordStringLedger seen = SEEN.get(player.getUUID());
		if (observed.isEmpty() || seen == null || !seen.consume(observed.get())) {
			refuse(player, payload.art(), art.get(), Refusal.UNSEEN);
			return;
		}
		// Reserve before the performer/clash: neither re-entrant hooks nor another request may reuse this suffix.
		perform(player, art.get(), observed.get().marks());
	}

	/** Whether an art held in a clash is being let go now (it goes as it was loosed, and meets nothing more). */
	private static boolean releasing;

	/**
	 * Performs {@code art} (already checked). The authored fixed-release style forms commit price/rest before their windup and run on their active frame;
	 * other arts keep their existing instant entry. Success hooks and the Grimoire follow actual performance. An art that meets an oncoming
	 * crescent, or answers a foe's art that just struck, locks into a clash first ({@link Clashes#meets}) and waits for it: it goes if the clash
	 * is won ({@link #release}) and is lost, still paid for, if not ({@link #forfeit}).
	 */
	public static boolean perform(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks) {
		if (dev.wildercord.cast.ExciseCasting.blocking(player) || MastersArts.committed(player) || MasterForms.committed(player)) return false;
		if (!releasing && Clashes.meets(player, art, marks)) return true;
		long now = player.level().getGameTime();
		MastersStyleRules.Style style = MastersStyleRules.of(art.id());
		// Target/counter forms must opt into their own validation rather than losing the observed victim
		// just because they acquire a timeline. Cone releases deliberately re-query on the active frame.
		AuraApi.StringContext context = new AuraApi.StringContext(art, marks,
			style == null || style.targets() != MastersStyleRules.TargetPolicy.ACTIVE_CONE ? struck(player) : null, now);
		// Price is fixed before either the windup or the art can change momentum.
		double cost = price(player, art);
		if (style != null) {
			if (!MastersArts.beginStyle(player, art, context, () -> {
				if (runPerformer(player, art, context)) completed(player, art, context);
			})) return false;
			// The accepted tell commits payment once. Interrupted or whiffed forms keep this price and rest.
			payAndRest(player, art, cost, now);
			return true;
		}
		if (!runPerformer(player, art, context)) return false;
		payAndRest(player, art, cost, now);
		completed(player, art, context);
		return true;
	}

	/** Runs under the original art identity, so delayed active frames retain marks, caps and elemental ownership. */
	private static boolean runPerformer(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext context) {
		boolean done;
		AuraApi.StringArt outer = performing;
		performing = art;
		try {
			done = dev.wildercord.cast.Effects.withSource(player, () -> art.performer().perform(player, context));
		} catch (RuntimeException e) {
			Wildercord.LOGGER.warn("Art {} threw", art.id(), e);
			done = false;
		} finally {
			performing = outer;
		}
		if (!done) refuse(player, art.id(), art, Refusal.CLOSED);
		return done;
	}

	private static void payAndRest(ServerPlayer player, AuraApi.StringArt art, double cost, long now) {
		double price = Math.min(cost, Aura.aura(player));
		if (price > 0) Aura.spend(player, price, "art:" + art.id());
		if (art.cooldownTicks() > 0) {
			Cooldowns rests = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
			player.setAttached(COOLDOWNS, rests.rest(art.id(), now + rest(player, art), now));
		}
	}

	private static void completed(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext context) {
		for (AuraApi.StringHook hook : AuraApi.stringHooks()) {
			try {
				hook.performed(player, art, context);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A sword string hook threw; skipping it", e);
			}
		}
		Grimoire.unlock(player, "aura:sword_string");
		if (!AuraApi.artMethod(art.id()).isEmpty()) Grimoire.unlock(player, grimoireKey(art.id()));
	}

	/**
	 * An art held in a clash that was won: it goes now, as it was loosed (if its swordsman can still play it: a blade in hand, not silenced,
	 * aura for it; otherwise it's lost after all). Returns whether it went.
	 */
	static boolean release(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks) {
		if (!player.isAlive() || !Aura.holdsWeapon(player) || dev.wildercord.aura.arts.ArtWards.silenced(player)
				|| Aura.aura(player) < price(player, art) - 1.0E-4) {
			forfeit(player, art);
			return false;
		}
		boolean outer = releasing;
		releasing = true;
		try {
			return perform(player, art, marks);
		} finally {
			releasing = outer;
		}
	}

	/** An art held in a clash that was lost: it doesn't go, but it's paid for and rests as if it had. */
	static void forfeit(ServerPlayer player, AuraApi.StringArt art) {
		long now = player.level().getGameTime();
		double price = Math.min(price(player, art), Aura.aura(player));
		if (price > 0) {
			Aura.spend(player, price, "art:" + art.id());
		}
		if (art.cooldownTicks() > 0) {
			Cooldowns rests = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
			player.setAttached(COOLDOWNS, rests.rest(art.id(), now + rest(player, art), now));
		}
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
		// A bonded blade's trait: its favourite art (Well-Worn Verse) or a technique (Inkbound Steel) a little cheaper.
		price *= BladeTraits.price(player, art);
		return AwakeningRules.price(price, Awakening.priceShare(player));
	}

	/** How long {@code art} rests for {@code player} once performed (ticks): its own rest, shorter on a bonded blade's favourite. Both sides. */
	public static int rest(Player player, AuraApi.StringArt art) {
		return BladeTraits.restTicks(player, art);
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

	/** Changing held equipment invalidates partial stroke evidence even when the original stack is selected again. */
	public static void weaponChanged(ServerPlayer player) {
		SEEN.remove(player.getUUID());
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
