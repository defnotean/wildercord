package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.duel.Duels;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sparring: two swordsmen trying their blades on each other in a ring of light (the rules and numbers are {@link SparRules}).
 *
 * <p><b>The salute.</b> Sneaking with a blade in hand, a swordsman uses it on another (the use key on them): their blade comes up in a
 * flash of their colour, a thin line of it reaches the one saluted, and both are told. Saluted back within {@link SparRules#OFFER_TICKS}
 * (the same gesture), the spar begins. A salute is all a challenge ever is: nobody can be made to spar, and one ignored simply lapses.</p>
 *
 * <p><b>The ring.</b> A circle of light round the point between them, half in each one's colour, posts of light at its four quarters,
 * and a count-in (standards rising over fractured ground each second, the bowl struck higher), then the spar. It's fought on the duel's own rules
 * ({@link Duels#startBout}), on a spar's terms: blades and aura only (spells, shots and pets don't reach a sparring partner), the two harm
 * only each other, anyone else's blow calls it off, and it ends when one is brought to <b>one heart</b> (held there, never killed), steps out
 * of the ring (always free: nobody is trapped), or after two minutes (even).</p>
 *
 * <p><b>The end.</b> Both are put back as they began: the health and harm their partner dealt (the duel's own restoring), the aura they spent
 * (unless they awakened: an awakening is always paid for, and ends with the spar), and momentum to nothing. The winner's aura flares and a
 * column of their light rises as the ring falls in on them, a banner on each screen. A real spar (fought ten seconds, blows both ways) goes
 * into each one's record, and the first few a day for any pair teach both aura experience. A spar grows no blade, ranks no technique and
 * makes no trial, but one: a disciple besting their master ({@link Lineage}).</p>
 */
public final class Spars {
	private Spars() {}

	// ------------------------------------------------------------------ what each screen knows

	/**
	 * A swordsman's spar as their own screen shows it: who with, the ring (its middle and radius), when it began, the count-in, both colours,
	 * whether it counts today, and once it's over, how ({@link #result}: 1 won, 2 lost, 3 even, 4 called off) and when.
	 */
	public record Bout(UUID partner, String partnerName, double cx, double cy, double cz, double radius, long start, int count, int color, int partnerColor,
			boolean counts, int result, long endedAt) {
		public static final StreamCodec<ByteBuf, Bout> STREAM_CODEC = new StreamCodec<>() {
			@Override
			public Bout decode(ByteBuf buf) {
				return new Bout(UUIDUtil.STREAM_CODEC.decode(buf), ByteBufCodecs.stringUtf8(64).decode(buf), buf.readDouble(), buf.readDouble(), buf.readDouble(),
					buf.readDouble(), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readInt(), buf.readInt(), buf.readBoolean(),
					ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf));
			}

			@Override
			public void encode(ByteBuf buf, Bout b) {
				UUIDUtil.STREAM_CODEC.encode(buf, b.partner());
				ByteBufCodecs.stringUtf8(64).encode(buf, b.partnerName());
				buf.writeDouble(b.cx());
				buf.writeDouble(b.cy());
				buf.writeDouble(b.cz());
				buf.writeDouble(b.radius());
				ByteBufCodecs.VAR_LONG.encode(buf, b.start());
				ByteBufCodecs.VAR_INT.encode(buf, b.count());
				buf.writeInt(b.color());
				buf.writeInt(b.partnerColor());
				buf.writeBoolean(b.counts());
				ByteBufCodecs.VAR_INT.encode(buf, b.result());
				ByteBufCodecs.VAR_LONG.encode(buf, b.endedAt());
			}
		};

		public static final int WON = 1;
		public static final int LOST = 2;
		public static final int EVEN = 3;
		public static final int CALLED_OFF = 4;

		/** The middle of the ring. */
		public Vec3 centre() {
			return new Vec3(cx, cy, cz);
		}

		/** Whether it's over (its moment showing). */
		public boolean over() {
			return result != 0;
		}

		/** Whether the count-in is still running at {@code now}. */
		public boolean counting(long now) {
			return !over() && now - start < count;
		}

		Bout ended(int how, long now) {
			return new Bout(partner, partnerName, cx, cy, cz, radius, start, count, color, partnerColor, counts, how, now);
		}
	}

	/** A swordsman's spar, while it lasts and for its moment after (sent to them alone). */
	public static final AttachmentType<Bout> SPAR = AttachmentRegistry.create(
		Wildercord.id("spar"),
		builder -> builder.syncWith(Bout.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** A salute waiting to be answered, as the one saluted sees it: who, their colour, until when. */
	public record Offer(UUID from, String name, int color, long until) {
		public static final StreamCodec<ByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Offer::from,
			ByteBufCodecs.stringUtf8(64), Offer::name, ByteBufCodecs.INT, Offer::color, ByteBufCodecs.VAR_LONG, Offer::until, Offer::new);
	}

	public static final AttachmentType<Offer> OFFER = AttachmentRegistry.create(
		Wildercord.id("spar_offer"),
		builder -> builder.syncWith(Offer.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	private static final Codec<SparRules.Log> LOG_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("wins", 0).forGetter(SparRules.Log::wins),
		Codec.INT.optionalFieldOf("losses", 0).forGetter(SparRules.Log::losses),
		Codec.INT.optionalFieldOf("evens", 0).forGetter(SparRules.Log::evens),
		Codec.LONG.optionalFieldOf("day", Long.MIN_VALUE).forGetter(SparRules.Log::day),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("today", Map.of()).forGetter(SparRules.Log::today)
	).apply(i, SparRules.Log::new));
	/** What a swordsman's own page shows of their record (the day's counts stay on the server). */
	private static final StreamCodec<ByteBuf, SparRules.Log> LOG_STREAM = StreamCodec.composite(ByteBufCodecs.VAR_INT, SparRules.Log::wins,
		ByteBufCodecs.VAR_INT, SparRules.Log::losses, ByteBufCodecs.VAR_INT, SparRules.Log::evens, (w, l, e) -> new SparRules.Log(w, l, e, Long.MIN_VALUE, Map.of()));

	/** Each swordsman's sparring record (saved, kept through death; its wins, losses and evens sent to them for the Aura page). */
	public static final AttachmentType<SparRules.Log> LOG = AttachmentRegistry.create(
		Wildercord.id("spar_log"),
		builder -> builder.initializer(() -> SparRules.Log.NONE).persistent(LOG_CODEC).syncWith(LOG_STREAM, AttachmentSyncPredicate.targetOnly()).copyOnDeath()
	);

	// ------------------------------------------------------------------ reading (both sides)

	/** Whether sparring works for {@code player}: aura and sparring on (the server's, or on a client what it was sent). */
	public static boolean on(Player player) {
		return player != null && Aura.enabled(player) && Config.sparring(player);
	}

	/** The spar {@code player} is in, or over just now (their own client knows only its own), or null. */
	public static Bout bout(Player player) {
		return player.getAttached(SPAR);
	}

	/** Whether {@code player} is in a spar now (count-in included). Server: the duel's rules; a client: its own player's attachment. */
	public static boolean sparring(Player player) {
		if (player.level().isClientSide()) {
			Bout b = bout(player);
			return b != null && !b.over();
		}
		return Duels.watchedBy(player, Ring.class);
	}

	/** Whether {@code a} and {@code b} are sparring each other (server). */
	public static boolean partners(Player a, Entity b) {
		if (!(b instanceof Player other) || RINGS.isEmpty()) {
			return false;
		}
		Ring ring = RINGS.get(a.getUUID());
		return ring != null && ring == RINGS.get(other.getUUID());
	}

	/** {@code player}'s record. */
	public static SparRules.Log log(Player player) {
		return player.getAttachedOrElse(LOG, SparRules.Log.NONE);
	}

	// ------------------------------------------------------------------ the salute

	private record Salute(UUID to, long at) {}

	/** Each swordsman's salute waiting for an answer, by the one who made it. */
	private static final Map<UUID, Salute> SALUTES = new HashMap<>();
	/** When each swordsman last saluted (a click arrives once, but never twice in a tick). */
	private static final Map<UUID, Long> LAST = new HashMap<>();
	/** Each spar under way, by both its swordsmen. */
	private static final Map<UUID, Ring> RINGS = new HashMap<>();

	/**
	 * The use key on another player: a salute when sneaking with a blade in hand. Taken on both sides (the client so the item in hand isn't
	 * used instead: a spear's charge), decided on the server.
	 */
	static InteractionResult use(Player player, Level level, InteractionHand hand, Entity entity) {
		if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Player target) || target == player || !player.isShiftKeyDown() || player.isSpectator()
				|| !on(player) || Aura.stage(player) < AuraRules.GLOW || !Aura.holdsWeapon(player)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer from && target instanceof ServerPlayer to) {
			long now = level.getGameTime();
			Long last = LAST.get(from.getUUID());
			if (last == null || now - last > 4) {
				LAST.put(from.getUUID(), now);
				salute(from, to);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** {@code from} salutes {@code to}: an answer to their salute begins the spar; otherwise it's a challenge. */
	public static void salute(ServerPlayer from, ServerPlayer to) {
		long now = from.level().getGameTime();
		Salute theirs = SALUTES.get(to.getUUID());
		if (theirs != null && theirs.to().equals(from.getUUID()) && now - theirs.at() <= SparRules.OFFER_TICKS) {
			accept(from, to);
			return;
		}
		SparRules.Refusal refusal = challengeRefusal(from, to);
		if (refusal != null) {
			from.sendOverlayMessage(Component.translatable(refusal.key(), to.getDisplayName()).withColor(0xC8A0A0));
			return;
		}
		if (!readyOrSay(from, from, to)) {
			return;
		}
		Salute mine = SALUTES.get(from.getUUID());
		boolean renewed = mine != null && mine.to().equals(to.getUUID()) && now - mine.at() <= SparRules.RESALUTE;
		SALUTES.put(from.getUUID(), new Salute(to.getUUID(), now));
		to.setAttached(OFFER, new Offer(from.getUUID(), from.getGameProfile().name(), colour(from), now + SparRules.OFFER_TICKS));
		saluteLook(from, to);
		if (renewed) {
			return;
		}
		from.sendSystemMessage(Component.translatable("message.wildercord.aura.spar.saluted", to.getDisplayName()).withColor(0xE8D8B0));
		to.sendSystemMessage(Component.translatable("message.wildercord.aura.spar.challenged", from.getDisplayName()).withColor(0xE8D8B0));
		to.sendSystemMessage(Component.translatable("message.wildercord.aura.spar.challenged_how").withColor(0xB8A8D8));
	}

	/** Why {@code from} can't challenge {@code to} now (their own readiness, and whether {@code to} could spar at all), or null. */
	private static SparRules.Refusal challengeRefusal(ServerPlayer from, ServerPlayer to) {
		return SparRules.refusal(on(from), new boolean[] {Aura.stage(from) >= AuraRules.GLOW, Aura.stage(to) >= AuraRules.GLOW},
			new boolean[] {Aura.holdsWeapon(from)}, new boolean[] {hurtable(from), hurtable(to)}, from.distanceTo(to), from.level() == to.level(),
			new boolean[] {Duels.inDuel(from) || dev.wildercord.aura.world.DuelistDuels.inDuel(from), Duels.inDuel(to)
				|| dev.wildercord.aura.world.DuelistDuels.inDuel(to)},
			new boolean[] {Awakening.awakened(from)}, new double[] {health(from)});
	}

	/** Why {@code a} and {@code b} can't begin a spar now, every check (the salute aside), or null. */
	public static SparRules.Refusal refusal(ServerPlayer a, ServerPlayer b) {
		return SparRules.refusal(on(a) && on(b), new boolean[] {Aura.stage(a) >= AuraRules.GLOW, Aura.stage(b) >= AuraRules.GLOW},
			new boolean[] {Aura.holdsWeapon(a), Aura.holdsWeapon(b)}, new boolean[] {hurtable(a), hurtable(b)}, a.distanceTo(b), a.level() == b.level(),
			new boolean[] {Duels.inDuel(a) || dev.wildercord.aura.world.DuelistDuels.inDuel(a), Duels.inDuel(b) || dev.wildercord.aura.world.DuelistDuels.inDuel(b)},
			new boolean[] {Awakening.awakened(a), Awakening.awakened(b)}, new double[] {health(a), health(b)});
	}

	private static boolean hurtable(ServerPlayer player) {
		return player.isAlive() && !player.isCreative() && !player.isSpectator();
	}

	private static double health(ServerPlayer player) {
		return player.getHealth() / Math.max(1.0F, player.getMaxHealth());
	}

	/** Whether {@code player} is ready by the duel's own rules (not hurt just now, nor fresh from a fight with a player or a duel); if not {@code told} hears why. */
	private static boolean readyOrSay(ServerPlayer told, ServerPlayer player, ServerPlayer other) {
		DuelRules.Refusal why = Duels.readiness(player);
		if (why == DuelRules.Refusal.NONE) {
			return true;
		}
		String who = player == told ? "self" : "other";
		told.sendOverlayMessage(Component.translatable("message.wildercord.aura.spar.not_ready." + why.name().toLowerCase(java.util.Locale.ROOT) + "." + who,
			player.getDisplayName()).withColor(0xC8A0A0));
		return false;
	}

	/** The salute answered: every check, then the spar. */
	private static void accept(ServerPlayer answering, ServerPlayer challenger) {
		SparRules.Refusal refusal = refusal(challenger, answering);
		if (refusal != null) {
			Component line = Component.translatable(refusal.key(), challenger.getDisplayName()).withColor(0xC8A0A0);
			answering.sendOverlayMessage(line);
			return;
		}
		if (!readyOrSay(answering, answering, challenger) || !readyOrSay(answering, challenger, answering)) {
			return;
		}
		SALUTES.remove(challenger.getUUID());
		SALUTES.remove(answering.getUUID());
		begin(challenger, answering);
	}

	/** A salute's look: the blade up in a flash of the swordsman's colour, a thin line of it to the one saluted, steel ringing. */
	private static void saluteLook(ServerPlayer from, ServerPlayer to) {
		ServerLevel level = from.level();
		int color = colour(from);
		Vec3 blade = BladeCeremony.hand(from).add(0, 0.35, 0);
		from.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		AuraFx.burst(level, from, blade, Vec3.ZERO, color, 0.55F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR);
		Vec3 chest = to.position().add(0, 1.1, 0);
		ArtLight.spectacle(from).ray(blade, chest, color, 0.035, 10).bare().ray(blade, chest, AuraVfx.hot(color, 0.6), 0.012, 8);
		Motes.seek(level, blade, chest, color, 0.12, 14, 0.2);
		Feels.sound(level, blade, "aura_spar_salute", 0.9F, 1.0F);
	}

	// ------------------------------------------------------------------ the spar

	/**
	 * Begins a spar between {@code a} and {@code b} at once (every check but the salute: {@link #refusal}), the ring round the point between
	 * them. Returns whether it began. For an add-on's own challenge, or a tournament's bracket ({@link AuraApi#spar}).
	 */
	public static boolean start(ServerPlayer a, ServerPlayer b) {
		if (refusal(a, b) != null || Duels.readiness(a) != DuelRules.Refusal.NONE || Duels.readiness(b) != DuelRules.Refusal.NONE) {
			return false;
		}
		begin(a, b);
		return true;
	}

	private static void begin(ServerPlayer a, ServerPlayer b) {
		ServerLevel level = a.level();
		long now = level.getGameTime();
		double radius = Config.get().aura().sparring().ringRadius();
		Vec3 centre = a.position().add(b.position()).scale(0.5);
		centre = new Vec3(centre.x, Math.min(a.getY(), b.getY()), centre.z);
		int daily = Config.get().aura().sparring().sparsPerDay();
		long day = SparRules.day(now);
		Ring ring = new Ring(level, centre, radius, a, b, now);
		ring.countsA = SparRules.counts(log(a).counted(b.getUUID(), day), daily);
		ring.countsB = SparRules.counts(log(b).counted(a.getUUID(), day), daily);
		DuelRules.Terms terms = new DuelRules.Terms(radius, SparRules.COUNT_TICKS, SparRules.MAX_FIGHT_TICKS, SparRules.KNOCKOUT, SparRules.KNOCKOUT, false);
		ring.duel = Duels.startBout(a, b, terms, centre, ring);
		RINGS.put(a.getUUID(), ring);
		RINGS.put(b.getUUID(), ring);
		for (ServerPlayer p : List.of(a, b)) {
			ServerPlayer other = p == a ? b : a;
			p.removeAttached(OFFER);
			// A spar starts at nothing: momentum built before it doesn't come in, and what's built in it doesn't go out.
			Momentum.reset(p);
			p.setAttached(SPAR, new Bout(other.getUUID(), other.getGameProfile().name(), centre.x, centre.y, centre.z, radius, now, SparRules.COUNT_TICKS,
				colour(p), colour(other), p == a ? ring.countsA : ring.countsB, 0, 0));
			p.sendSystemMessage(Component.translatable("message.wildercord.aura.spar.begins", other.getDisplayName()).withColor(0xFFE8C46A));
			p.sendSystemMessage(Component.translatable(p == a && ring.countsA || p == b && ring.countsB ? "message.wildercord.aura.spar.counts"
				: "message.wildercord.aura.spar.for_its_own_sake", other.getDisplayName()).withColor(0xB8A8D8));
		}
		Feels.sound(level, centre.add(0, 1, 0), "aura_spar_ring", 1.0F, 1.0F);
		ring.draw(true);
		for (AuraApi.SparHook hook : AuraApi.sparHooks()) {
			try {
				hook.began(a, b);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A spar hook threw; skipping it", e);
			}
		}
	}

	/** Calls {@code player}'s spar off (for nobody). */
	public static void callOff(ServerPlayer player) {
		if (RINGS.containsKey(player.getUUID())) {
			Duels.callOff(player);
		}
	}

	/** A swordsman's colour (a plain silver before a method). */
	static int colour(Player player) {
		int c = Aura.color(player);
		return c == 0 ? 0xD8D0F0 : c;
	}

	/** One spar: its ring, its two, how each began, and what the duel's rules tell it. */
	static final class Ring implements Duels.Watcher {
		final ServerLevel level;
		final Vec3 centre;
		final double radius;
		final UUID a;
		final UUID b;
		final long begun;
		final float poolA;
		final float poolB;
		final int colorA;
		final int colorB;
		/** Which way from the middle each began (a flat unit vector toward {@code a}): each one's half of the ring faces their side. */
		final Vec3 toA;
		DuelRules.Duel duel;
		boolean countsA;
		boolean countsB;
		boolean struckA;
		boolean struckB;
		long fightFrom = Long.MIN_VALUE;

		Ring(ServerLevel level, Vec3 centre, double radius, ServerPlayer a, ServerPlayer b, long now) {
			this.level = level;
			this.centre = centre;
			this.radius = radius;
			this.a = a.getUUID();
			this.b = b.getUUID();
			this.begun = now;
			this.poolA = Aura.data(a).aura();
			this.poolB = Aura.data(b).aura();
			this.colorA = colour(a);
			this.colorB = colour(b);
			Vec3 d = a.position().subtract(centre).multiply(1, 0, 1);
			this.toA = d.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : d.normalize();
		}

		ServerPlayer player(UUID id) {
			ServerPlayer online = level.getServer().getPlayerList().getPlayer(id);
			if (online != null) {
				return online;
			}
			return level.getPlayerByUUID(id) instanceof ServerPlayer there && !there.isRemoved() ? there : null;
		}

		/** Blades and aura only: a blow of the blade (a swing, a sweep, a spear's thrust) or aura off it; never a spell, a shot or a pet. */
		@Override
		public boolean counts(Player attacker, ServerPlayer victim, DamageSource source) {
			boolean blade = source.getDirectEntity() == attacker && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.SPEAR))
				|| source.is(Aura.DAMAGE) && source.getEntity() == attacker;
			if (blade && dev.wildercord.cast.Effects.applying() == null) {
				if (attacker.getUUID().equals(a)) {
					struckA = true;
				} else if (attacker.getUUID().equals(b)) {
					struckB = true;
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean spells() {
			return false;
		}

		@Override
		public void counting(int second) {
			// Each second of the count-in: a ring racing out from the middle to the edge, the bowl struck higher.
			Vec3 at = centre.add(0, 0.12, 0);
			Feels.sound(level, at.add(0, 1, 0), "aura_spar_count", 1.0F, Feels.step(3 - second));
			ServerPlayer pa = player(a);
			if (pa != null) {
				ArtLight.world(pa).groundRing(centre, AuraRules.mix(colorA, colorB, 0.5), 0.4, radius - 0.3, 0.1, 12);
			}
		}

		@Override
		public void began() {
			fightFrom = level.getGameTime();
			Feels.sound(level, centre.add(0, 1, 0), "aura_spar_begin", 1.1F, 1.0F);
			for (UUID id : List.of(a, b)) {
				ServerPlayer p = player(id);
				if (p != null) {
					AuraFx.bodyAuraFlare(p, 26, 0.6F);
					AuraFx.burst(level, p, BladeCeremony.hand(p), Vec3.ZERO, colour(p), 0.6F, AuraFx.Burst.FLASH);
				}
			}
			draw(true);
		}

		@Override
		public void tick(long now) {
			if ((now - begun) % SparRules.RING_EVERY == 0) {
				draw(false);
			}
		}

		/** Four cloth standards and broken ground mark the freely walkable bounds. */
		void draw(boolean flare) {
			ServerPlayer owner = player(a);
			if (owner == null) owner = player(b);
			if (owner == null) return;
			Vec3 side = new Vec3(-toA.z, 0, toA.x);
			int index = 0;
			for (Vec3 dir : new Vec3[] {toA, toA.scale(-1), side, side.scale(-1)}) {
				Vec3 foot = centre.add(dir.scale(radius));
				int color = index++ % 2 == 0 ? colorA : colorB;
				AuraFx.standard(owner, foot, dir.scale(-1), "spar", color, flare ? 1F : 0.4F, 0.65F, SparRules.RING_LIFE);
				AuraFx.groundScar(level, foot, 0.65, SparRules.RING_LIFE, 2);
			}
			if (flare) AuraFx.groundScar(level, centre, radius, 16, 0);
		}

		@Override
		public void ended(DuelRules.Duel duel, ServerPlayer winner, ServerPlayer loser) {
			if (RINGS.get(a) == this) {
				RINGS.remove(a);
			}
			if (RINGS.get(b) == this) {
				RINGS.remove(b);
			}
			finished(this, duel, winner, loser);
		}
	}

	/** {@code ArtKit.UP}, without reaching into the arts' kit for one vector. */
	private static final class ArtKitUp {
		static final Vec3 UP = new Vec3(0, 1, 0);
	}

	/** A spar is over: both put back, the moment, the record, what it taught, a master bested, and the hooks. */
	private static void finished(Ring ring, DuelRules.Duel duel, ServerPlayer winner, ServerPlayer loser) {
		long now = ring.level.getGameTime();
		DuelRules.Ending ending = duel.ending();
		boolean decided = winner != null && loser != null && (ending == DuelRules.Ending.KNOCKOUT || ending == DuelRules.Ending.LEFT_AREA);
		boolean even = ending == DuelRules.Ending.DRAW;
		long fought = ring.fightFrom == Long.MIN_VALUE ? 0 : now - ring.fightFrom;
		boolean real = (decided || even) && SparRules.real(fought, ring.struckA && ring.struckB);
		long day = SparRules.day(now);
		for (UUID id : List.of(ring.a, ring.b)) {
			ServerPlayer p = ring.player(id);
			if (p == null) {
				continue;
			}
			putBack(p, id.equals(ring.a) ? ring.poolA : ring.poolB);
			int how = decided ? (p == winner ? Bout.WON : Bout.LOST) : even ? Bout.EVEN : Bout.CALLED_OFF;
			Bout bout = p.getAttached(SPAR);
			if (bout != null) {
				p.setAttached(SPAR, bout.ended(how, now));
			}
			ENDING.put(id, now + SparRules.OUTCOME_TICKS);
			Grimoire.unlock(p, "aura:spar");
		}
		boolean counted = false;
		if (real) {
			ServerPlayer pa = ring.player(ring.a);
			ServerPlayer pb = ring.player(ring.b);
			if (pa != null && pb != null) {
				counted = teach(pa, pb, decided ? (pa == winner ? SparRules.Result.WON : SparRules.Result.LOST) : SparRules.Result.EVEN, ring.countsA, day);
				counted |= teach(pb, pa, decided ? (pb == winner ? SparRules.Result.WON : SparRules.Result.LOST) : SparRules.Result.EVEN, ring.countsB, day);
			}
		}
		moment(ring, winner, loser, decided, even, ending);
		if (decided && real) {
			// A disciple who brought their master to one heart: the master's trial.
			Lineage.sparred(winner, loser, ending == DuelRules.Ending.KNOCKOUT);
		}
		for (AuraApi.SparHook hook : AuraApi.sparHooks()) {
			try {
				hook.ended(winner, loser, ending, counted);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A spar hook threw; skipping it", e);
			}
		}
	}

	/** Puts {@code player} back as the spar found them: the aura they spent (unless an awakening burned in it: that is always paid for), momentum to nothing. */
	private static void putBack(ServerPlayer player, float pool) {
		boolean awakened = Awakening.awakened(player);
		if (awakened) {
			Awakening.endNow(player);
		}
		Momentum.reset(player);
		if (!awakened && !Awakening.spent(player)) {
			AuraAttachments.Data data = Aura.data(player);
			float back = Math.min(Aura.capacity(player), pool);
			if (data.learned() && back > data.aura()) {
				Aura.set(player, data.withAura(back));
			}
		}
	}

	/** What a real spar teaches {@code player}: the record always, and aura experience if it counts for this pair today. Returns whether it counted. */
	private static boolean teach(ServerPlayer player, ServerPlayer partner, SparRules.Result result, boolean counts, long day) {
		SparRules.Log log = log(player).record(result);
		if (counts) {
			log = log.count(partner.getUUID(), day);
		}
		player.setAttached(LOG, log);
		if (!counts) {
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.spar.taught_nothing", partner.getDisplayName()).withColor(0xB8A8D8));
			return false;
		}
		int stage = Aura.stage(player);
		double xp = SparRules.xp(stage, SparRules.road(stage), Aura.stage(partner), result, Config.get().aura().sparring().sparXp());
		double got = AuraExperience.earn(player, xp, false);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.spar.taught", String.format(java.util.Locale.ROOT, "%.1f", got),
			log.counted(partner.getUUID(), day), Config.get().aura().sparring().sparsPerDay()).withColor(0xE8D8B0));
		return true;
	}

	/** The end's moment: the winner's aura flaring, their standard above fractured ground, banners on both screens. */
	private static void moment(Ring ring, ServerPlayer winner, ServerPlayer loser, boolean decided, boolean even, DuelRules.Ending ending) {
		ServerLevel level = ring.level;
		if (decided) {
			int color = colour(winner);
			Vec3 feet = winner.position();
			Feels.sound(level, feet.add(0, 1, 0), "aura_spar_win", 1.1F, 1.0F);
			if (ending == DuelRules.Ending.LEFT_AREA) {
				Feels.sound(level, loser.position().add(0, 1, 0), "aura_spar_out", 0.9F, 1.0F);
			}
			AuraFx.bodyAuraFlare(winner, 50, 1.0F);
			AuraFx.burst(level, winner, BladeCeremony.hand(winner), Vec3.ZERO, color, 1.3F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.SPARKS);
			AuraFx.groundScar(level, feet, 3, 28, 1);
			AuraFx.standard(winner, feet.add(1, 0, 0), ring.toA, "spar", color, 1F, 0.6F, 45);
			// The ring falls in on the winner.
			ArtLight.world(winner).groundRing(ring.centre, color, ring.radius, 0.6, 0.18, 14).groundRing(feet, AuraVfx.hot(color, 0.4), 0.3, 3.0, 0.1, 12);
			Motes.burst(level, feet.add(0, 1.0, 0), 16, color, 0.12, 28, 0.1);
			Component kicker = Component.translatable("aura.wildercord.spar.kicker", loser.getGameProfile().name());
			AuraFx.banner(winner, Component.translatable("aura.wildercord.spar.victory"), kicker, color, AuraFxRules.BannerKind.GRAND);
			AuraFx.banner(loser, Component.translatable("aura.wildercord.spar.bested"),
				Component.translatable("aura.wildercord.spar.kicker", winner.getGameProfile().name()), colour(loser), AuraFxRules.BannerKind.ART);
			Component how = Component.translatable(ending == DuelRules.Ending.LEFT_AREA ? "message.wildercord.aura.spar.won_out" : "message.wildercord.aura.spar.won",
				winner.getDisplayName(), loser.getDisplayName()).withColor(0xFFE8C46A);
			tell(ring, how);
			return;
		}
		Feels.sound(level, ring.centre.add(0, 1, 0), "aura_spar_end", 0.9F, 1.0F);
		ServerPlayer any = ring.player(ring.a) != null ? ring.player(ring.a) : ring.player(ring.b);
		if (any != null) {
			ArtLight.world(any).groundRing(ring.centre, AuraRules.mix(ring.colorA, ring.colorB, 0.5), ring.radius, ring.radius + 1.2, 0.08, 12);
		}
		tell(ring, Component.translatable(even ? "message.wildercord.aura.spar.even" : "message.wildercord.aura.spar.called_off").withColor(0xC8B89A));
		if (even) {
			for (UUID id : List.of(ring.a, ring.b)) {
				ServerPlayer p = ring.player(id);
				if (p != null) {
					AuraFx.banner(p, Component.translatable("aura.wildercord.spar.even"), null, colour(p), AuraFxRules.BannerKind.ART);
				}
			}
		}
	}

	/** Both of them and everyone near hear how it ended. */
	private static void tell(Ring ring, Component line) {
		for (ServerPlayer p : ring.level.players()) {
			if (p.position().distanceToSqr(ring.centre) <= 48 * 48 || p.getUUID().equals(ring.a) || p.getUUID().equals(ring.b)) {
				p.sendSystemMessage(line);
			}
		}
	}

	// ------------------------------------------------------------------ each tick

	/** When each swordsman's last spar's moment stops showing. */
	private static final Map<UUID, Long> ENDING = new HashMap<>();

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 == 0 && (!SALUTES.isEmpty() || !LAST.isEmpty())) {
			long now = server.overworld().getGameTime();
			SALUTES.values().removeIf(s -> now - s.at() > SparRules.OFFER_TICKS);
			LAST.values().removeIf(t -> now - t > 200);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				Offer offer = p.getAttached(OFFER);
				if (offer != null && p.level().getGameTime() > offer.until()) {
					p.removeAttached(OFFER);
				}
			}
		}
		if (ENDING.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Long> e : List.copyOf(ENDING.entrySet())) {
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			long now = server.overworld().getGameTime();
			if (p == null) {
				ENDING.remove(e.getKey());
				continue;
			}
			if (p.level().getGameTime() >= e.getValue() || now > e.getValue() + 200) {
				ENDING.remove(e.getKey());
				Bout bout = p.getAttached(SPAR);
				if (bout != null && bout.over()) {
					p.removeAttached(SPAR);
				}
			}
		}
	}

	/** Clears a stand-in's spar attachment by hand (the game tests: a stand-in isn't in the player list). */
	public static void forgetBout(ServerPlayer player) {
		ENDING.remove(player.getUUID());
		player.removeAttached(SPAR);
	}

	// ------------------------------------------------------------------ lifecycle

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> use(player, level, hand, entity));
		ServerTickEvents.END_SERVER_TICK.register(Spars::tick);
		// A spar grows no blade: no resonance while one is on (its partner, its arts, its guards, its finishers).
		AuraApi.onBlade(new AuraApi.BladeHook() {
			@Override
			public double resonance(ServerPlayer player, double amount, String source) {
				return RINGS.containsKey(player.getUUID()) ? 0 : amount;
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			SALUTES.remove(id);
			LAST.remove(id);
			ENDING.remove(id);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SALUTES.clear();
			LAST.clear();
			RINGS.clear();
			ENDING.clear();
		});
	}

	/** Every spar under way now, by one of its two (the game tests). */
	public static int underWay() {
		return (int) RINGS.values().stream().distinct().count();
	}

	/** Who saluted whom and hasn't been answered (the game tests). */
	public static boolean saluted(Player from, Player to) {
		Salute s = SALUTES.get(from.getUUID());
		return s != null && s.to().equals(to.getUUID());
	}

	/** The spars' sounds, for the tests. */
	public static final List<String> SOUNDS = List.of("aura_spar_salute", "aura_spar_ring", "aura_spar_count", "aura_spar_begin", "aura_spar_win",
		"aura_spar_end", "aura_spar_out");

	static List<UUID> partnersOf(Player player) {
		Ring ring = RINGS.get(player.getUUID());
		List<UUID> out = new ArrayList<>();
		if (ring != null) {
			out.add(ring.a.equals(player.getUUID()) ? ring.b : ring.a);
		}
		return out;
	}

	/** The partner of {@code player}'s spar (server), or null. */
	public static UUID partner(Player player) {
		List<UUID> p = partnersOf(player);
		return p.isEmpty() ? null : p.getFirst();
	}

	/** Whether harm to {@code target} from {@code attacker} is a sparring partner's (the stance and finisher caps still hold; this is for what a spar gives). */
	public static boolean sparHarm(LivingEntity attacker, LivingEntity target) {
		return attacker instanceof Player p && partners(p, target);
	}
}
