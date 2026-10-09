package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.aura.world.AuraWorldRules;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.duel.Duels;
import dev.wildercord.party.Parties;
import dev.wildercord.net.PacketThrottle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The clash: two strikes meeting lock for a moment into a struggle won on timing (the rules and numbers are {@link ClashRules}).
 *
 * <p><b>What meets.</b> Two crescents in the air (a swordsman's slash, a duelist's, a fallen knight's, one a perfect guard sent back), owned by
 * two who aren't allies ({@link #meet}); an art loosed into an oncoming crescent, within {@link ClashRules#MEET_REACH} ahead of the swordsman;
 * and an art loosed straight back at a swordsman whose art struck them within {@link ClashRules#CROSS} ticks, both facing ({@link #meets}).
 * A technique is an art, so it meets what's coming as it's loosed; a technique's wave already flying is past that moment and passes through
 * crescents and other waves (it's the blade's force thrown along the ground, not a crescent in the air).</p>
 *
 * <p><b>The lock.</b> Both strikes hold where they met (the crescents grinding against each other in the air; an art held back unpaid), and
 * both sides are shown the rhythm ({@link Begin}, drawn by each client): three beats, a ring closing on each. A player presses on each beat
 * with a swing of their blade ({@link Press}, judged here with an allowance for their connection); a duelist or a knight presses on a timing of
 * its own drawn as the lock takes hold, its blade flashing as it presses. Every press is told to both sides and everyone near ({@link Mark}),
 * and the meeting point drifts toward whoever is losing. Neither side is ever held: they can move, guard and step all the while, and the same
 * two don't lock again for {@link ClashRules#REST} ticks (meeting before then, their strikes break each other as they always did).</p>
 *
 * <p><b>The end.</b> The winner's strike carries on ({@link End}): a crescent flies on at {@code clash_carry} of its harm, an art goes off as
 * it was loosed (an answering art turns aside the art it answered, the harm it did given back), and the loser's breaks (an art lost is still
 * paid for). The first striker winning against an answer carries on too: a strike of half what their art dealt. Equal scores break both, in
 * the old burst that harms nobody; the Way of the Blade's slash wins an equal score and has a little more time on each beat
 * ({@link ClashRules#EDGE}), and nothing more.</p>
 */
public final class Clashes {
	private Clashes() {}

	// ------------------------------------------------------------------ on the wire

	/**
	 * A clash has locked: its id and kind, the two sides (entity ids), where they met and which way from {@code a} to {@code b}, their colours,
	 * when it began (game time), and bits: 1 {@code a} has the Blade's edge, 2 {@code b} has, 4 {@code a} is a swordsman of the world, 8 {@code b}.
	 */
	public record Begin(int id, int kind, int a, int b, Vec3 at, Vec3 axis, int colorA, int colorB, long start, int flags) implements CustomPacketPayload {
		public static final Type<Begin> TYPE = new Type<>(Wildercord.id("clash_begin"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Begin> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeVarInt(p.id());
			buf.writeVarInt(p.kind());
			buf.writeVarInt(p.a());
			buf.writeVarInt(p.b());
			buf.writeDouble(p.at().x);
			buf.writeDouble(p.at().y);
			buf.writeDouble(p.at().z);
			buf.writeFloat((float) p.axis().x);
			buf.writeFloat((float) p.axis().y);
			buf.writeFloat((float) p.axis().z);
			buf.writeInt(p.colorA());
			buf.writeInt(p.colorB());
			buf.writeVarLong(p.start());
			buf.writeVarInt(p.flags());
		}, buf -> new Begin(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
			new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()), buf.readInt(), buf.readInt(), buf.readVarLong(), buf.readVarInt()));

		public static final int EDGE_A = 1;
		public static final int EDGE_B = 2;
		public static final int WORLD_A = 4;
		public static final int WORLD_B = 8;

		@Override
		public Type<Begin> type() {
			return TYPE;
		}
	}

	/** A press judged (or a beat missed): which side (0 {@code a}, 1 {@code b}), which beat (-1 a fumble), its grade, and both scores now. */
	public record Mark(int id, int side, int beat, int grade, int scoreA, int scoreB) implements CustomPacketPayload {
		public static final Type<Mark> TYPE = new Type<>(Wildercord.id("clash_mark"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Mark> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Mark::id, ByteBufCodecs.VAR_INT,
			Mark::side, ByteBufCodecs.VAR_INT, Mark::beat, ByteBufCodecs.VAR_INT, Mark::grade, ByteBufCodecs.VAR_INT, Mark::scoreA, ByteBufCodecs.VAR_INT,
			Mark::scoreB, Mark::new).cast();

		@Override
		public Type<Mark> type() {
			return TYPE;
		}
	}

	/** A clash is over: who won ({@link ClashRules.Outcome}'s ordinal), and the two scores. */
	public record End(int id, int outcome, int scoreA, int scoreB) implements CustomPacketPayload {
		public static final Type<End> TYPE = new Type<>(Wildercord.id("clash_end"));
		public static final StreamCodec<RegistryFriendlyByteBuf, End> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, End::id, ByteBufCodecs.VAR_INT,
			End::outcome, ByteBufCodecs.VAR_INT, End::scoreA, ByteBufCodecs.VAR_INT, End::scoreB, End::new).cast();

		@Override
		public Type<End> type() {
			return TYPE;
		}
	}

	/** A player's press in clash {@code id} (a swing of the blade while it lasts, taken by their client for the clash). */
	public record Press(int id) implements CustomPacketPayload {
		public static final Type<Press> TYPE = new Type<>(Wildercord.id("clash_press"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Press> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Press::id, Press::new).cast();

		@Override
		public Type<Press> type() {
			return TYPE;
		}
	}

	// ------------------------------------------------------------------ the clashes

	/** An art held back while its clash runs: whose, which, and the swings it was played with. */
	private record Held(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks, EarnedCounters.Attempt counter, Object key) {
		static Held capture(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks, EarnedCounters.Attempt counter) {
			Object key = new Object();
			if (counter != null) counter.hold(key, player.level().getServer().getTickCount());
			return new Held(player, art, List.copyOf(marks), counter, key);
		}
	}

	/** One side of a clash. */
	private static final class Side {
		final LivingEntity entity;
		final boolean edge;
		final int color;
		final int stage;
		final Crescents.Flight flight;
		final Held art;
		final ClashRules.Tally tally = new ClashRules.Tally();
		/** A swordsman of the world's presses, ticks off each beat ({@link ClashRules#NPC_MISS} for a miss), or null for a player. */
		final int[] npc;

		Side(LivingEntity entity, boolean edge, int color, int stage, Crescents.Flight flight, Held art, int[] npc) {
			this.entity = entity;
			this.edge = edge;
			this.color = color;
			this.stage = stage;
			this.flight = flight;
			this.art = art;
			this.npc = npc;
		}

		boolean player() {
			return entity instanceof ServerPlayer;
		}
	}

	/** One clash: its two sides, where and when, and for an answered art what the first striker's art did. */
	private static final class Clash {
		final int id;
		final ClashRules.Kind kind;
		final ServerLevel level;
		final Vec3 at;
		final Vec3 axis;
		final long start;
		final Side a;
		final Side b;
		/** What the art {@code a} answered took from them, and dealt before their defences (an answered art's clash). */
		final float answeredTaken;
		final double answeredAmount;
		double balance;
		boolean done;

		Clash(int id, ClashRules.Kind kind, ServerLevel level, Vec3 at, Vec3 axis, long start, Side a, Side b, float answeredTaken, double answeredAmount) {
			this.id = id;
			this.kind = kind;
			this.level = level;
			this.at = at;
			this.axis = axis;
			this.start = start;
			this.a = a;
			this.b = b;
			this.answeredTaken = answeredTaken;
			this.answeredAmount = answeredAmount;
		}

		Side side(LivingEntity entity) {
			return a.entity == entity ? a : b.entity == entity ? b : null;
		}

		/** Where the strikes meet now: drifted toward whoever is losing. */
		Vec3 point() {
			return at.add(axis.scale(balance * DRIFT));
		}
	}

	/** How far (blocks) the meeting point drifts toward the side losing it all. */
	private static final double DRIFT = 0.6;

	private static final Map<Integer, Clash> ACTIVE = new HashMap<>();
	private static final Map<UUID, Integer> BY_ENTITY = new HashMap<>();
	/** When each pair (by both ids, smaller first) last clashed: they rest before locking again. */
	private static final Map<String, Long> LAST = new HashMap<>();
	private static int nextId = 1;
	/** How many clashes have locked and how each ended, since the server started (the game tests read them). */
	private static int locked;
	private static final Map<ClashRules.Kind, Integer> RESOLVED = new HashMap<>();
	private static final PacketThrottle PRESSES = new PacketThrottle(6, 2);

	/** Whether clashes are on (the server's {@code clashes}). */
	public static boolean on() {
		return Config.get().aura().sparring().clashes();
	}

	/** Whether {@code entity} is in a clash now. */
	public static boolean clashing(Entity entity) {
		return entity != null && BY_ENTITY.containsKey(entity.getUUID());
	}

	/** Whether {@code player} has an art held back in a clash now (its other arts wait). */
	public static boolean holding(Player player) {
		Integer id = BY_ENTITY.get(player.getUUID());
		Clash c = id == null ? null : ACTIVE.get(id);
		if (c == null) {
			return false;
		}
		Side s = c.side(player);
		return s != null && s.art != null;
	}

	private static String pair(Entity x, Entity y) {
		String p = x.getUUID().toString();
		String q = y.getUUID().toString();
		return p.compareTo(q) < 0 ? p + q : q + p;
	}

	private static boolean rested(Entity x, Entity y, long now) {
		Long last = LAST.get(pair(x, y));
		return ClashRules.rested(last == null ? Long.MIN_VALUE : last, now);
	}

	/** An agreed, active duel overrides party protection; ordinary party and Banner allies pass through each other. */
	static boolean allies(LivingEntity x, LivingEntity y) {
		if (Boolean.TRUE.equals(Duels.canHarm(x, y)) || Boolean.TRUE.equals(Duels.canHarm(y, x))) return false;
		return Parties.sameParty(x, y) || x instanceof ServerPlayer p && y instanceof Player q && WayBanner.ally(p, q);
	}

	/** A clash is an interaction, so it uses the same party, explicit-duel and opt-in-trial boundaries as harm. */
	private static boolean opposed(LivingEntity x, LivingEntity y) {
		return x != y && x.level() == y.level() && !allies(x, y)
			&& permits(x, y) && permits(y, x);
	}

	private static boolean permits(LivingEntity owner, Entity target) {
		Boolean duel = Duels.canHarm(owner, target);
		return duel != null ? duel : !Parties.blocksHarm(owner, target);
	}

	static boolean mayMeet(Crescents.Flight x, Crescents.Flight y) {
		return opposed(x.caster, y.caster) && x.trialTarget(y.caster) && y.trialTarget(x.caster);
	}

	/** Both strikes must admit a bystander before the burst may move them. The owners may still push each other. */
	static boolean mayPush(LivingEntity a, Crescents.Flight fa, LivingEntity b, Crescents.Flight fb, LivingEntity target) {
		return pushAudience(a, fa, target) && pushAudience(b, fb, target);
	}

	private static boolean pushAudience(LivingEntity owner, Crescents.Flight flight, LivingEntity target) {
		if (flight != null && flight.trialMaster != null && (!flight.trialActive()
				|| target != flight.trialMaster && !flight.trialMaster.canHarmParticipant(target))) return false;
		return target == owner || !allies(owner, target) && permits(owner, target);
	}

	/** Recheck at resolution too: joining a party or leaving a trial during the lock cannot authorize a stale strike. */
	private static boolean admitted(Clash c) {
		return opposed(c.a.entity, c.b.entity) && (c.a.flight == null || c.a.flight.trialTarget(c.b.entity))
			&& (c.b.flight == null || c.b.flight.trialTarget(c.a.entity));
	}

	// ------------------------------------------------------------------ meeting

	/** What two crescents meeting in the air do. */
	public enum Meeting {
		/** They lock into a clash. */
		LOCKED,
		/** They break each other in a burst (clashes off, one owner already clashing, or these two clashed a moment ago). */
		BREAK,
		/** They pass through each other (allies, protected duellists, or outside the other strike's trial). */
		PASS
	}

	/** Two crescents of different owners met in the air this tick (from {@link Crescents}). */
	static Meeting meet(Crescents.Flight x, Crescents.Flight y) {
		LivingEntity cx = x.caster;
		LivingEntity cy = y.caster;
		if (!mayMeet(x, y)) {
			return Meeting.PASS;
		}
		long now = x.level.getGameTime();
		if (!on() || clashing(cx) || clashing(cy) || !rested(cx, cy, now)) {
			return Meeting.BREAK;
		}
		Vec3 at = x.front.add(y.front).scale(0.5);
		Vec3 axis = y.front.subtract(x.front);
		axis = axis.lengthSqr() < 1.0E-4 ? x.aim : axis.normalize();
		x.held = true;
		y.held = true;
		lock(ClashRules.Kind.CRESCENTS, side(cx, x.pierce, x.color, x, null), side(cy, y.pierce, y.color, y, null), x.level, at, axis, 0, 0);
		return Meeting.LOCKED;
	}

	/** A side of a clash for {@code entity}: its stage, its world timing if it's a swordsman of the world. */
	private static Side side(LivingEntity entity, boolean edge, int color, Crescents.Flight flight, Held art) {
		int stage = entity instanceof AuraFighter f ? f.stage() : entity instanceof Player p ? Aura.stage(p) : AuraRules.GLOW;
		int[] npc = null;
		if (!(entity instanceof Player)) {
			npc = new int[ClashRules.BEATS];
			for (int k = 0; k < npc.length; k++) {
				npc[k] = ClashRules.npcOffset(stage, entity.getRandom().nextDouble(), entity.getRandom().nextGaussian());
			}
		}
		return new Side(entity, edge, color, stage, flight, art, npc);
	}

	/**
	 * An art about to be performed (from {@code SwordStrings.perform}): if it meets an oncoming crescent ahead, or answers a foe's art that just
	 * struck, the two lock and the art is held back until the clash says whether it goes. Returns whether it was held.
	 */
	static boolean meets(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks) {
		// Legacy callers cannot manufacture earned evidence from a string's claimed marks.
		return !EarnedCounters.handles(art.id()) && meets(player, art, marks, null);
	}

	static boolean meets(ServerPlayer player, AuraApi.StringArt art, List<Integer> marks, EarnedCounters.Attempt counter) {
		if (!on() || clashing(player)) {
			return false;
		}
		long now = player.level().getGameTime();
		Vec3 eye = player.getEyePosition();
		Vec3 look = flat(player.getViewVector(1.0F));
		// An oncoming crescent within reach ahead.
		Crescents.Flight best = null;
		double nearest = ClashRules.MEET_REACH * ClashRules.MEET_REACH;
		for (Crescents.Flight f : Crescents.inFlight()) {
			if (f.done || f.held || f.caster == player || f.level != player.level()
					|| !opposed(player, f.caster) || !f.trialTarget(player) || clashing(f.caster)) {
				continue;
			}
			Vec3 to = f.front.subtract(eye);
			double d = to.lengthSqr();
			if (d > nearest) {
				continue;
			}
			Vec3 flatTo = flat(to);
			boolean ahead = d < 1.0 || look.dot(flatTo) >= ClashRules.FACING;
			boolean oncoming = flat(f.aim).dot(flatTo.scale(-1)) >= ClashRules.ONCOMING;
			if (ahead && oncoming && rested(player, f.caster, now)) {
				nearest = d;
				best = f;
			}
		}
		if (best != null) {
			best.held = true;
			Vec3 axis = best.front.subtract(eye);
			axis = axis.lengthSqr() < 1.0E-4 ? player.getViewVector(1.0F) : axis.normalize();
			Vec3 at = best.front.subtract(axis.scale(0.4));
			lock(ClashRules.Kind.ART_CRESCENT, side(player, false, Spars.colour(player), null, Held.capture(player, art, marks, counter)),
				side(best.caster, best.pierce, best.color, best, null), player.level(), at, axis, 0, 0);
			return true;
		}
		// An answer to a foe's art that struck a moment ago.
		Struck struck = STRUCK.get(player.getUUID());
		if (struck == null || now - struck.at() > ClashRules.CROSS) {
			return false;
		}
		Player foe = player.level().getPlayerByUUID(struck.by());
		if (!(foe instanceof ServerPlayer first) || !first.isAlive() || first.distanceTo(player) > ClashRules.ANSWER_REACH || clashing(first)
				|| !opposed(player, first) || !rested(player, first, now)) {
			return false;
		}
		Vec3 toFoe = flat(first.position().subtract(player.position()));
		if (toFoe.lengthSqr() > 1.0E-4 && look.dot(toFoe) < ClashRules.FACING) {
			return false;
		}
		STRUCK.remove(player.getUUID());
		Vec3 mid = player.getEyePosition().add(first.getEyePosition()).scale(0.5).subtract(0, 0.35, 0);
		Vec3 axis = first.getEyePosition().subtract(player.getEyePosition());
		axis = axis.lengthSqr() < 1.0E-4 ? player.getViewVector(1.0F) : axis.normalize();
		lock(ClashRules.Kind.ARTS, side(player, false, Spars.colour(player), null, Held.capture(player, art, marks, counter)),
			side(first, false, Spars.colour(first), null, null), player.level(), mid, axis, struck.taken(), struck.amount());
		return true;
	}

	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-6 ? Vec3.ZERO : f.normalize();
	}

	/** An art's strike landed on a player (from {@code ArtKit.Hits}): noted a moment, for an art answering it. */
	private record Struck(UUID by, long at, float taken, double amount) {}

	private static final Map<UUID, Struck> STRUCK = new HashMap<>();

	/** {@code attacker}'s art took {@code taken} from {@code victim} (dealing {@code amount} before their defences). */
	public static void artStruck(ServerPlayer attacker, LivingEntity victim, float taken, double amount) {
		if (!(victim instanceof ServerPlayer target) || target == attacker || taken <= 0) {
			return;
		}
		long now = attacker.level().getGameTime();
		Struck last = STRUCK.get(target.getUUID());
		if (last != null && last.by().equals(attacker.getUUID()) && now - last.at() <= 2) {
			STRUCK.put(target.getUUID(), new Struck(attacker.getUUID(), now, last.taken() + taken, last.amount() + amount));
		} else {
			STRUCK.put(target.getUUID(), new Struck(attacker.getUUID(), now, taken, amount));
		}
	}

	private static void lock(ClashRules.Kind kind, Side a, Side b, ServerLevel level, Vec3 at, Vec3 axis, float answeredTaken, double answeredAmount) {
		long now = level.getGameTime();
		Clash c = new Clash(nextId++, kind, level, at, axis, now, a, b, answeredTaken, answeredAmount);
		ACTIVE.put(c.id, c);
		BY_ENTITY.put(a.entity.getUUID(), c.id);
		BY_ENTITY.put(b.entity.getUUID(), c.id);
		locked++;
		int flags = (a.edge ? Begin.EDGE_A : 0) | (b.edge ? Begin.EDGE_B : 0) | (a.npc != null ? Begin.WORLD_A : 0) | (b.npc != null ? Begin.WORLD_B : 0);
		send(c, new Begin(c.id, kind.ordinal(), a.entity.getId(), b.entity.getId(), at, axis, a.color, b.color, now, flags));
		Feels.sound(level, at, "aura_clash_lock", 1.1F, 1.0F);
		ScreenFx.shake(level, at, 0.12F, 6);
		AuraFx.burst(level, null, at, Vec3.ZERO, 0xFFFFFF, 0.7F, AuraFx.Burst.FLASH | AuraFx.Burst.SPARKS);
		for (Side s : List.of(a, b)) {
			if (s.entity instanceof ServerPlayer p) {
				Grimoire.unlock(p, "aura:clash");
			}
		}
		for (AuraApi.ClashHook hook : AuraApi.clashHooks()) {
			try {
				hook.locked(a.entity, b.entity, kind);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A clash hook threw; skipping it", e);
			}
		}
	}

	// ------------------------------------------------------------------ presses

	/** A press from {@code player} in clash {@code id}: judged at its time less their connection's allowance, told to everyone. */
	static void press(ServerPlayer player, int id) {
		Clash c = ACTIVE.get(id);
		if (c == null || c.done) {
			return;
		}
		Side s = c.side(player);
		if (s == null || s.npc != null) {
			return;
		}
		int t = ClashRules.judgingTime((int) (c.level.getGameTime() - c.start), player.connection == null ? 0 : player.connection.latency());
		int k = ClashRules.beatFor(t, s.edge);
		boolean fresh = k >= 0 && s.tally.beat(k) == null;
		ClashRules.Grade grade = s.tally.press(t, s.edge);
		judged(c, s, fresh ? k : -1, grade, player);
	}

	/** A grade earned (or a beat missed): everyone told, a sound for those who didn't make it, the meeting point drifting. */
	private static void judged(Clash c, Side s, int beat, ClashRules.Grade grade, ServerPlayer presser) {
		int a = c.a.tally.score();
		int b = c.b.tally.score();
		c.balance = ClashRules.balance(a, b);
		send(c, new Mark(c.id, s == c.a ? 0 : 1, beat, grade.ordinal(), a, b));
		String sound = switch (grade) {
			case PERFECT -> "aura_clash_perfect";
			case GOOD -> "aura_clash_good";
			default -> "aura_clash_miss";
		};
		// The one who pressed heard their own at once (their client plays it); everyone else hears it here.
		soundExcept(c.level, c.point(), sound, presser, grade == ClashRules.Grade.PERFECT ? 1.0F : 0.8F, 1.0F);
	}

	private static void soundExcept(ServerLevel level, Vec3 at, String name, ServerPlayer except, float volume, float pitch) {
		SoundEvent event = WildercordSounds.kit(name);
		if (event == null) {
			return;
		}
		level.playSound(except, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch * (1.0F + (level.getRandom().nextFloat() - 0.5F) * 0.05F));
	}

	// ------------------------------------------------------------------ each tick

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 40 == 0) {
			long now = server.overworld().getGameTime();
			STRUCK.values().removeIf(s -> now - s.at() > 100);
			LAST.values().removeIf(at -> now - at > ClashRules.REST);
		}
		if (ACTIVE.isEmpty()) return;

		for (Clash c : List.copyOf(ACTIVE.values())) {
			if (c.done) {
				continue;
			}
			long now = c.level.getGameTime();
			int t = (int) (now - c.start);
			if (!admitted(c)) {
				resolve(c, ClashRules.Outcome.EVEN);
				continue;
			}
			// A side gone (dead, away, its crescent's owner fallen) loses; both gone, it's even.
			boolean aGone = gone(c, c.a);
			boolean bGone = gone(c, c.b);
			if (aGone || bGone) {
				resolve(c, aGone && bGone ? ClashRules.Outcome.EVEN : aGone ? ClashRules.Outcome.B : ClashRules.Outcome.A);
				continue;
			}
			for (Side s : List.of(c.a, c.b)) {
				if (s.npc != null) {
					for (int k = 0; k < ClashRules.BEATS; k++) {
						int o = s.npc[k];
						if (o != ClashRules.NPC_MISS && t == ClashRules.beat(k) + o && s.tally.beat(k) == null) {
							ClashRules.Grade grade = ClashRules.judge(o, s.edge);
							s.tally.set(k, grade);
							judged(c, s, k, grade, null);
							// Its blade flashes as it presses: a cue a player can read.
							AuraFx.burst(c.level, s.entity, s.entity.position().add(0, 1.3, 0).add(c.axis.scale(s == c.a ? 0.5 : -0.5)), Vec3.ZERO, s.color,
								grade == ClashRules.Grade.PERFECT ? 0.55F : 0.4F, AuraFx.Burst.FLASH);
						}
					}
				}
				int judgedAt = s.entity instanceof ServerPlayer player
					? ClashRules.judgingTime(t, player.connection == null ? 0 : player.connection.latency()) : t;
				int missed = s.tally.close(judgedAt, s.edge);
				for (int k = 0; k < ClashRules.BEATS; k++) {
					if ((missed & (1 << k)) != 0) {
						judged(c, s, k, ClashRules.Grade.MISS, null);
					}
				}
			}
			if (t % 2 == 0) {
				draw(c, t);
			}
			if (t >= ClashRules.serverLength()) {
				resolve(c, null);
			}
		}
	}

	/** Whether a side can't go on: its entity dead, removed or in another world, or its crescent ended some other way. */
	private static boolean gone(Clash c, Side s) {
		if (!s.entity.isAlive() || s.entity.isRemoved() || s.entity.level() != c.level) {
			return true;
		}
		return s.flight != null && s.flight.done;
	}

	/** The held crescents, grinding where they meet (the meeting point drifting toward whoever is losing). */
	private static void draw(Clash c, int t) {
		Vec3 shift = c.axis.scale(c.balance * DRIFT);
		double shudder = (t % 4 == 0 ? 0.05 : -0.05);
		for (Side s : List.of(c.a, c.b)) {
			Crescents.Flight f = s.flight;
			if (f != null) {
				AuraVfx.slashStep(c.level, f.front.add(shift).add(f.aim.scale(shudder)), f.aim, f.side, f.color, 1 + t, f.weak);
			}
		}
	}

	/** Ends clash {@code c}: {@code forced} if a side is gone, otherwise the scores' outcome. */
	private static void resolve(Clash c, ClashRules.Outcome forced) {
		if (c.done) {
			return;
		}
		boolean admitted = admitted(c);
		if (!admitted) forced = ClashRules.Outcome.EVEN;
		c.done = true;
		ACTIVE.remove(c.id);
		if (BY_ENTITY.get(c.a.entity.getUUID()) == c.id) {
			BY_ENTITY.remove(c.a.entity.getUUID());
		}
		if (BY_ENTITY.get(c.b.entity.getUUID()) == c.id) {
			BY_ENTITY.remove(c.b.entity.getUUID());
		}
		long now = c.level.getGameTime();
		LAST.put(pair(c.a.entity, c.b.entity), now);
		int sa = c.a.tally.score();
		int sb = c.b.tally.score();
		ClashRules.Outcome outcome = forced != null ? forced : ClashRules.outcome(sa, sb, c.a.edge, c.b.edge);
		RESOLVED.merge(c.kind, 1, Integer::sum);
		send(c, new End(c.id, outcome.ordinal(), sa, sb));
		Side winner = outcome == ClashRules.Outcome.A ? c.a : outcome == ClashRules.Outcome.B ? c.b : null;
		Side loser = winner == null ? null : winner == c.a ? c.b : c.a;
		Vec3 point = c.point();
		double carry = Config.get().aura().sparring().clashCarry();
		// The winner's strike carries on; the loser's breaks.
		for (Side s : List.of(c.a, c.b)) {
			boolean wins = s == winner;
			if (s.flight != null) {
				if (wins) {
					s.flight.held = false;
					s.flight.scale *= carry;
				} else {
					s.flight.held = false;
					s.flight.done = true;
				}
			}
			if (s.art != null) {
				if (wins) {
					SwordStrings.release(s.art.player(), s.art.art(), s.art.marks(), s.art.counter(), s.art.key());
				} else {
					if (s.art.counter() == null || s.art.counter().take(s.art.key(), c.level.getServer().getTickCount()) != EarnedCounterReservation.Take.UNAVAILABLE)
						SwordStrings.forfeit(s.art.player(), s.art.art());
				}
			}
		}
		if (c.kind == ClashRules.Kind.ARTS && winner != null) {
			if (winner == c.a && c.a.entity instanceof ServerPlayer answering && c.answeredTaken > 0) {
				// The answer won: the art it answered is turned aside, the harm it did given back.
				answering.heal(Math.min(c.answeredTaken, answering.getMaxHealth() - answering.getHealth()));
			} else if (winner == c.b && c.b.entity instanceof ServerPlayer first && c.a.entity.isAlive() && c.answeredAmount > 0) {
				// The first striker won: their art carries on, a strike of half what it dealt.
				float took = AuraCombat.projected(first, c.a.entity, c.answeredAmount * ClashRules.ANSWER_CARRY, false);
				if (took > 0) {
					AuraFx.impact(first, c.a.entity, c.b.color, Aura.stage(first), AuraFxRules.Weight.HEAVY);
				}
			}
		}
		if (winner != null) {
			Feels.sound(c.level, point, "aura_clash_win", 1.1F, 1.0F);
			AuraFx.burst(c.level, winner.entity, point, Vec3.ZERO, winner.color, 1.1F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.SPARKS);
			AuraFx.burst(c.level, loser.entity, point.add(c.axis.scale(winner == c.a ? 0.4 : -0.4)), Vec3.ZERO, loser.color, 0.6F, AuraFx.Burst.SPARKS);
			if (winner.entity instanceof ServerPlayer p) {
				AuraApi.addMomentum(p, ClashRules.WIN_MOMENTUM, "clash");
				AuraFx.bodyAuraFlare(p, 24, 0.6F);
			}
		} else {
			// Equal: both break in the old burst, harming nobody.
			Feels.sound(c.level, point, "aura_clash_even", 1.1F, 1.0F);
			AuraVfx.clash(c.level, point, c.axis, c.a.color, c.b.color);
			ScreenFx.shake(c.level, point, 0.2F, 10);
			for (Entity e : c.level.getEntities((Entity) null, new AABB(point, point).inflate(AuraWorldRules.CLASH_RADIUS),
					e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
				if (admitted && mayPush(c.a.entity, c.a.flight, c.b.entity, c.b.flight, (LivingEntity) e)) {
					dev.wildercord.monster.MonsterMagic.knock((LivingEntity) e, point, AuraWorldRules.CLASH_PUSH);
				}
			}
		}
		for (AuraApi.ClashHook hook : AuraApi.clashHooks()) {
			try {
				if (winner != null) {
					hook.resolved(winner.entity, loser.entity, c.kind, winner.tally.score(), loser.tally.score());
				} else {
					hook.even(c.a.entity, c.b.entity, c.kind);
				}
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A clash hook threw; skipping it", e);
			}
		}
	}

	/** Sends {@code payload} to both sides (if they're players) and everyone near where they met. */
	private static void send(Clash c, CustomPacketPayload payload) {
		Set<ServerPlayer> to = new HashSet<>(PlayerLookup.around(c.level, c.at, 48));
		for (Side s : List.of(c.a, c.b)) {
			if (s.entity instanceof ServerPlayer p) {
				to.add(p);
			}
		}
		for (ServerPlayer p : to) {
			if (ServerPlayNetworking.canSend(p, payload.type())) {
				ServerPlayNetworking.send(p, payload);
			}
		}
	}

	// ------------------------------------------------------------------ reading (the game tests)

	/** How many clashes have locked since the server started. */
	public static int lockedCount() {
		return locked;
	}

	/** How many clashes of {@code kind} have ended since the server started. */
	public static int resolvedCount(ClashRules.Kind kind) {
		return RESOLVED.getOrDefault(kind, 0);
	}

	/** The id of the clash {@code entity} is in, or -1. */
	public static int idOf(Entity entity) {
		Integer id = entity == null ? null : BY_ENTITY.get(entity.getUUID());
		return id == null ? -1 : id;
	}

	/** Elapsed ticks of an entity's active lock, or -1 when none. */
	public static int ageOf(Entity entity) {
		Integer id = BY_ENTITY.get(entity.getUUID());
		Clash clash = id == null ? null : ACTIVE.get(id);
		return clash == null ? -1 : (int)(clash.level.getGameTime() - clash.start);
	}

	/** {@code entity}'s score in its clash now, or 0. */
	public static int score(Entity entity) {
		Integer id = entity == null ? null : BY_ENTITY.get(entity.getUUID());
		Clash c = id == null ? null : ACTIVE.get(id);
		Side s = c == null ? null : c.side((LivingEntity) entity);
		return s == null ? 0 : s.tally.score();
	}

	/** Presses for a side as a player would (a stand-in in the game tests has no client): judged just as {@link Press} is. */
	public static void pressFor(ServerPlayer player) {
		Integer id = BY_ENTITY.get(player.getUUID());
		if (id != null) {
			press(player, id);
		}
	}

	/** Forgets which pairs clashed lately (the game tests, between scenes). */
	public static void forgetRest() {
		LAST.clear();
		STRUCK.clear();
	}

	// ------------------------------------------------------------------ lifecycle

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Begin.TYPE, Begin.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Mark.TYPE, Mark.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(End.TYPE, End.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Press.TYPE, Press.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Press.TYPE, (payload, context) -> {
			if (PRESSES.allow(context.player().getUUID(), context.server().getTickCount())) {
				press(context.player(), payload.id());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Clashes::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			STRUCK.remove(id);
			PRESSES.forget(id);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVE.clear();
			BY_ENTITY.clear();
			LAST.clear();
			STRUCK.clear();
			PRESSES.clear();
		});
	}

	/** Every clash sound, for the tests. */
	public static final List<String> SOUNDS = List.of("aura_clash_lock", "aura_clash_beat", "aura_clash_perfect", "aura_clash_good", "aura_clash_miss",
		"aura_clash_win", "aura_clash_even");

	/** Ids of every clash under way (the game tests). */
	public static List<Integer> underWay() {
		return new ArrayList<>(ACTIVE.keySet());
	}
}
