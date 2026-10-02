package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.PracticeRoom;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.player.WildercordAttachments;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Stance and openings at runtime (the rules and numbers are {@link StanceRules}). A foe's stance is kept on it as it's worn
 * ({@link #STANCE}, sent to everyone near when it changes, so every client draws its bar and its opened mark), coming back by
 * itself as both sides work out from what was sent. What wears it is told here by the rest of aura:
 * <ul>
 * <li>{@link #blow}: a swordsman's blade ({@code AuraCombat.blow}): a full swing by what it was dealt at, less for a glance or a
 *     bare blade, a fall's critical more, Stone's a quarter more;</li>
 * <li>{@link #art}: an art's strikes ({@code ArtKit.Hits}), more for an art that quakes or holds;</li>
 * <li>{@link #slash}: aura off the blade that isn't an art (a slash, a spark), a little;</li>
 * <li>{@link #guardBreak}: a perfect guard against a foe's blow breaks into its stance;</li>
 * <li>{@link #guarded}: in a duel, a blow caught on a held Aura Guard or a raised shield wears the guard's own.</li>
 * </ul>
 * Broken, a foe is opened ({@link #open}): staggered (a creature held, a boss slowed, a player slowed with their guard broken),
 * marked, and the next full swing of a swordsman's blade on it is a finisher ({@link #finisher}, {@link #finished}): its method's
 * own named strike (see {@code aura.arts.Finishers}), a share of what the foe has lost, aura back. Then it stands steady a while.
 */
public final class Stance {
	private Stance() {}

	// ------------------------------------------------------------------ the state

	/**
	 * A foe's stance as last written: {@code worn} at {@code at} (coming back after, see {@link StanceRules#worn}), out of
	 * {@code pool}, of {@code kind} ({@link StanceRules.Kind}'s ordinal); opened until {@code openUntil}, steady (nothing wears it)
	 * until {@code steadyUntil}; broken {@code breaks} times (a boss grows steadier).
	 */
	public record State(float worn, long at, float pool, int kind, long openUntil, long steadyUntil, int breaks) {
		public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, State::worn, ByteBufCodecs.VAR_LONG, State::at, ByteBufCodecs.FLOAT, State::pool, ByteBufCodecs.VAR_INT, State::kind,
			ByteBufCodecs.VAR_LONG, State::openUntil, ByteBufCodecs.VAR_LONG, State::steadyUntil, ByteBufCodecs.VAR_INT, State::breaks, State::new);

		public StanceRules.Kind kindOf() {
			return StanceRules.Kind.of(kind);
		}

		/** Whether it's opened at {@code now}. */
		public boolean opened(long now) {
			return now <= openUntil;
		}

		/** Whether it stands steady at {@code now} (opened, or just after: nothing wears it). */
		public boolean steady(long now) {
			return now <= steadyUntil;
		}

		/** How worn it is at {@code now} (all of it while opened, none while steady after). */
		public double wornAt(long now) {
			if (opened(now)) {
				return pool;
			}
			if (steady(now)) {
				return 0;
			}
			return StanceRules.worn(worn, at, pool, kindOf(), now);
		}

		/** How much of it is left at {@code now}, 0 to 1. */
		public double left(long now) {
			return StanceRules.left(wornAt(now), pool);
		}
	}

	/** A foe's stance, while it's worn (or opened, or steady): not saved, sent to everyone who can see it. */
	public static final AttachmentType<State> STANCE = AttachmentRegistry.create(
		Wildercord.id("aura_stance"),
		builder -> builder.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	// ------------------------------------------------------------------ reading (both sides)

	public static State state(LivingEntity entity) {
		return entity.getAttached(STANCE);
	}

	/** Whether {@code entity} is opened now: the next full swing of a swordsman's blade on it is a finisher. */
	public static boolean opened(LivingEntity entity) {
		State s = state(entity);
		return s != null && s.opened(entity.level().getGameTime());
	}

	/** How much of {@code entity}'s stance is left now (0 to 1; 1 for one never worn). */
	public static double left(LivingEntity entity) {
		State s = state(entity);
		return s == null ? 1 : s.left(entity.level().getGameTime());
	}

	/** What kind of foe {@code entity} is, for its stance. */
	public static StanceRules.Kind kind(LivingEntity entity) {
		if (entity instanceof Player) {
			return StanceRules.Kind.PLAYER;
		}
		if (entity instanceof TrainingDummy) {
			return StanceRules.Kind.DUMMY;
		}
		if (Spirits.isBoss(entity)) {
			return StanceRules.Kind.BOSS;
		}
		if (entity.hasAttached(WildercordAttachments.RUNEBOUND) || entity instanceof dev.wildercord.aura.world.AuraFighter) {
			return StanceRules.Kind.STURDY;
		}
		return StanceRules.Kind.CREATURE;
	}

	// ------------------------------------------------------------------ who may wear whose

	/** Whether stance works on this server for {@code target}: on, and for another player only with {@code pvp_stance}. */
	private static boolean on(LivingEntity target) {
		if (!Config.get().aura().momentum().stance()) {
			return false;
		}
		return !(target instanceof Player) || Config.get().aura().momentum().pvpStance();
	}

	/**
	 * Whether {@code attacker} may wear {@code target}'s stance: a swordsman (a method learned, aura on), a foe they may harm by the
	 * mod's rules and the game's (teams, pvp), and a real foe or a practice target (never an animal, a pet, an ally).
	 */
	public static boolean eligible(ServerPlayer attacker, LivingEntity target) {
		if (attacker == null || target == null || !target.isAlive() || target == attacker || !on(target) || !Aura.enabled(attacker)
				|| Aura.stage(attacker) < AuraRules.GLOW) {
			return false;
		}
		if (!dev.wildercord.aura.arts.ArtKit.harmable(attacker, target)) {
			return false;
		}
		return target instanceof TrainingDummy || attacker.level().dimension() == PracticeRoom.DIMENSION || AuraCombat.worth(attacker, target) > 0;
	}

	// ------------------------------------------------------------------ wearing it (server)

	/** Every creature with a stance worn, for the upkeep (opened marks, and letting go of one that has come back). */
	private static final Map<UUID, LivingEntity> TRACKED = new HashMap<>();

	private static State fresh(LivingEntity target) {
		StanceRules.Kind kind = kind(target);
		return new State(0, target.level().getGameTime(), (float) StanceRules.pool(kind, target.getMaxHealth(), 0), kind.ordinal(), -1, -1, 0);
	}

	/**
	 * Wears {@code wear} (already worked out by {@link StanceRules#wear}) off {@code target}'s stance, by {@code attacker}, through
	 * the {@link AuraApi#onStance} hooks; breaks it when it's all worn. Nothing while it's opened or steady. Returns what it wore.
	 */
	public static double wear(ServerPlayer attacker, LivingEntity target, double wear, StanceRules.Source source) {
		if (wear <= 0 || !eligible(attacker, target)) {
			return 0;
		}
		double w = wear;
		for (AuraApi.StanceHook hook : AuraApi.stanceHooks()) {
			try {
				w = hook.modify(attacker, target, w, source);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A stance hook threw; skipping it", e);
			}
		}
		if (w <= 0) {
			return 0;
		}
		long now = target.level().getGameTime();
		State s = state(target);
		if (s == null) {
			s = fresh(target);
		}
		if (s.steady(now)) {
			return 0;
		}
		double before = s.wornAt(now);
		double after = Math.min(s.pool(), before + w);
		State next = new State((float) after, now, s.pool(), s.kind(), s.openUntil(), s.steadyUntil(), s.breaks());
		target.setAttached(STANCE, next);
		TRACKED.put(target.getUUID(), target);
		if (after >= s.pool() - 1.0E-4) {
			open(attacker, target, next);
		}
		return after - before;
	}

	/** The striker's share of the wear: their momentum's tier and the server's {@code stance_damage}. */
	private static double wearOf(ServerPlayer attacker, LivingEntity target, double amount, StanceRules.Source source, double artWeight) {
		boolean stone = BreathingMethod.Flavour.STONE == Aura.method(attacker).map(BreathingMethod::flavour).orElse(null);
		double momentum = Momentum.on(attacker) ? MomentumRules.stanceFactor(Momentum.tier(attacker)) : 1.0;
		return StanceRules.wear(amount, source, artWeight, stone, momentum, kind(target), Config.get().aura().pvpScale(),
			Config.get().aura().momentum().stanceDamage());
	}

	/**
	 * A swordsman's blade landed on {@code target} at {@code amount} ({@code AuraCombat.blow}): a full coated swing wears by what it
	 * was dealt at, a fall's critical more, a glance (a short swing, a sweep's other blows) or a bare blade less.
	 */
	static void blow(ServerPlayer attacker, LivingEntity target, float amount, float swing, boolean critical, boolean coated, boolean first) {
		StanceRules.Source source;
		if (!first || swing < AuraRules.FULL_SWING - 1.0E-4) {
			source = StanceRules.Source.GLANCE;
		} else if (!coated) {
			source = StanceRules.Source.BARE;
		} else {
			source = critical ? StanceRules.Source.CRITICAL : StanceRules.Source.BLOW;
		}
		wear(attacker, target, wearOf(attacker, target, amount, source, 1.0), source);
	}

	/**
	 * An art's strike landed on {@code target} at {@code amount} ({@code ArtKit.Hits}), weighing {@code artWeight}
	 * ({@link StanceRules#artWeight}); on another player held to {@code cap} more of their stance. Returns what it wore.
	 */
	public static double art(ServerPlayer attacker, LivingEntity target, double amount, double artWeight, double cap) {
		double w = Math.min(wearOf(attacker, target, amount, StanceRules.Source.ART, artWeight), Math.max(0, cap));
		return wear(attacker, target, w, StanceRules.Source.ART);
	}

	/** Aura off the blade that isn't an art (a slash, a spark) landed on {@code target} at {@code amount}. */
	static void slash(ServerPlayer attacker, LivingEntity target, double amount) {
		wear(attacker, target, wearOf(attacker, target, amount, StanceRules.Source.SLASH, 1.0), StanceRules.Source.SLASH);
	}

	/** {@code guard}'s perfect guard turned {@code attacker}'s blow aside: a big share of the attacker's stance at once. */
	static void guardBreak(ServerPlayer guard, LivingEntity attacker) {
		if (!eligible(guard, attacker)) {
			return;
		}
		State s = state(attacker);
		double pool = s == null ? StanceRules.pool(kind(attacker), attacker.getMaxHealth(), 0) : s.pool();
		wear(guard, attacker, StanceRules.guardBreak(kind(attacker), pool, Config.get().aura().momentum().stanceDamage()), StanceRules.Source.GUARD);
	}

	/**
	 * In a duel: {@code player} caught {@code attacker}'s blow of {@code amount} on a held Aura Guard or a raised shield, so the guard
	 * takes the wear itself (pressure breaks a turtle).
	 */
	public static void guarded(LivingEntity player, LivingEntity attacker, double amount) {
		if (player instanceof ServerPlayer target && attacker instanceof ServerPlayer striker && amount > 0) {
			wear(striker, target, wearOf(striker, target, amount, StanceRules.Source.GUARDED, 1.0), StanceRules.Source.GUARDED);
		}
	}

	// ------------------------------------------------------------------ opened

	/** {@code target}'s stance broke under {@code attacker}: opened, staggered, marked, and momentum for the one who broke it. */
	private static void open(ServerPlayer attacker, LivingEntity target, State s) {
		long now = target.level().getGameTime();
		StanceRules.Kind kind = s.kindOf();
		int ticks = StanceRules.openTicks(kind);
		long openUntil = now + ticks;
		int breaks = s.breaks() + 1;
		// What it stands as once the opening passes: steady a while, its stance whole again (a boss's a little greater).
		float pool = (float) StanceRules.pool(kind, target.getMaxHealth(), breaks);
		target.setAttached(STANCE, new State(0, openUntil, pool, s.kind(), openUntil, openUntil + StanceRules.steadyTicks(kind), breaks));
		TRACKED.put(target.getUUID(), target);
		stagger(target, kind, ticks);
		// The break: a crack and a deep blow for everyone near, a gold flash and ring where it gave way (the breaker's own, so up close
		// in their first person it's drawn small and low).
		ServerLevel level = attacker.level();
		Vec3 chest = target.getBoundingBox().getCenter();
		Feels.sound(level, chest, "aura_stance_break", 1.0F, kind == StanceRules.Kind.BOSS ? 0.8F : 1.0F);
		AuraFx.burst(level, attacker, chest, Vec3.ZERO, OPENED_COLOR, 1.3F + target.getBbWidth(), AuraFx.Burst.FLASH | AuraFx.Burst.RING | AuraFx.Burst.STAR);
		AuraFx.burst(level, attacker, target.position().add(0, 0.1, 0), new Vec3(0, 1, 0), OPENED_COLOR, 1.6F + target.getBbWidth() * 1.5F,
			AuraFx.Burst.RING | AuraFx.Burst.ECHO | AuraFx.Burst.SPARKS);
		Momentum.broke(attacker, target);
		Grimoire.unlock(attacker, "aura:stance_break");
		if (target instanceof ServerPlayer struck) {
			struck.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.aura.opened").withColor(0xFFE08A60));
		}
		for (AuraApi.BreakHook hook : AuraApi.breakHooks()) {
			try {
				hook.broken(attacker, target);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A stance break hook threw; skipping it", e);
			}
		}
	}

	/** The colour of an opening: the parry's gold, run hot. */
	public static final int OPENED_COLOR = 0xFFE7A0;

	/**
	 * An opened foe staggers: a creature (or a dummy) can't act for the opening, a boss is only slowed hard (as every art leaves a
	 * boss), a player is slowed, their swing spent and their guard broken (shield and Aura Guard) for the opening, never held still.
	 */
	private static void stagger(LivingEntity target, StanceRules.Kind kind, int ticks) {
		Statuses.interrupt(target);
		switch (kind) {
			case BOSS -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 2, false, true));
			case PLAYER -> {
				Player player = (Player) target;
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0, false, true, true));
				player.resetAttackStrengthTicker();
				if (player.isUsingItem()) {
					player.stopUsingItem();
				}
				ItemStack shield = new ItemStack(Items.SHIELD);
				player.getCooldowns().addCooldown(shield, ticks);
				if (target instanceof ServerPlayer server && AuraGuard.guarding(server)) {
					Aura.state(server, Aura.state(server).guard(-1, server.level().getGameTime()));
				}
			}
			default -> {
				Spirits.hold(target, ticks);
				if (target instanceof Mob mob) {
					mob.getNavigation().stop();
				}
			}
		}
	}

	// ------------------------------------------------------------------ the finisher

	/** A finisher under way: the foe it falls on, what it adds, and the foe's health before. */
	private record Pending(UUID target, double extra, float before, long at) {}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	/**
	 * Whether {@code attacker}'s blow about to land on {@code target} is a finisher (the blow itself, a full swing, the foe opened and
	 * theirs to harm), and if so what it adds to the blow ({@link StanceRules#finisher}); 0 otherwise. Asked by
	 * {@code AuraCombat.blow} just before the blow lands; {@link #finished} answers after.
	 */
	static double finisher(ServerPlayer attacker, LivingEntity target, float swing, boolean first) {
		PENDING.remove(attacker.getUUID());
		if (!first || swing < StanceRules.FINISHER_SWING - 1.0E-4 || !opened(target) || !eligible(attacker, target)) {
			return 0;
		}
		StanceRules.Kind kind = kind(target);
		double missing = Math.max(0, target.getMaxHealth() - target.getHealth());
		double extra = StanceRules.finisher(kind, missing, target.getMaxHealth(), dev.wildercord.aura.arts.ArtKit.weapon(attacker),
			Config.get().aura().pvpScale(), Config.get().aura().momentum().finisherDamage());
		for (AuraApi.FinisherHook hook : AuraApi.finisherHooks()) {
			try {
				extra = hook.extra(attacker, target, extra);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A finisher hook threw; skipping it", e);
			}
		}
		extra = Math.max(0, extra);
		PENDING.put(attacker.getUUID(), new Pending(target.getUUID(), extra, target.getHealth(), target.level().getGameTime()));
		return extra;
	}

	/**
	 * The blow {@link #finisher} weighed has landed (or not): a finisher that hurt closes the opening (the foe stands steady), plays its
	 * method's strike, gives aura and momentum back, and tells the hooks. Returns whether it was one.
	 */
	static boolean finished(ServerPlayer attacker, LivingEntity target, boolean hurt) {
		Pending p = PENDING.remove(attacker.getUUID());
		if (p == null || !p.target().equals(target.getUUID())) {
			return false;
		}
		if (!hurt) {
			// Turned aside: the opening stands for the next.
			return false;
		}
		long now = target.level().getGameTime();
		State s = state(target);
		if (s != null) {
			StanceRules.Kind kind = s.kindOf();
			target.setAttached(STANCE, new State(0, now, s.pool(), s.kind(), -1, now + StanceRules.steadyTicks(kind), s.breaks()));
		}
		if (target instanceof Mob mob && mob.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
			// The stagger ends with the blow: a finished foe falls back or fights on, never stands frozen after.
			mob.setAttached(WildercordAttachments.FROZEN_UNTIL, now);
		}
		float dealt = Math.max(0, p.before() - Math.max(0, target.getHealth()));
		boolean practice = Momentum.practice(attacker, target);
		double aura = StanceRules.finisherAura(Aura.stage(attacker), practice);
		if (Aura.method(attacker).map(BreathingMethod::flavour).orElse(null) == BreathingMethod.Flavour.STARLIT) {
			aura *= StanceRules.STARLIT_AURA;
		}
		dev.wildercord.aura.arts.ArtKit.giveBack(attacker, aura);
		AuraApi.Finisher finisher = AuraApi.finisher(Aura.data(attacker).method());
		try {
			finisher.look().play(attacker, target, new AuraApi.FinisherContext(dealt, p.extra(), !target.isAlive() || target.isDeadOrDying(), practice));
		} catch (RuntimeException e) {
			Wildercord.LOGGER.warn("Finisher {} threw", finisher.id(), e);
		}
		AuraFx.banner(attacker, net.minecraft.network.chat.Component.translatable(finisher.nameKey()), kicker(attacker), AuraFxRules.BannerKind.FINISHER);
		Momentum.finished(attacker, target);
		Grimoire.unlock(attacker, "aura:finisher");
		finishers++;
		lastFinisher = finisher.id();
		for (AuraApi.FinisherHook hook : AuraApi.finisherHooks()) {
			try {
				hook.landed(attacker, target, finisher, dealt);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A finisher hook threw; skipping it", e);
			}
		}
		return true;
	}

	/** The small line over a finisher's name: "<method> · Finisher". */
	private static net.minecraft.network.chat.Component kicker(ServerPlayer player) {
		net.minecraft.network.chat.Component method = Aura.method(player).<net.minecraft.network.chat.Component>map(
			m -> net.minecraft.network.chat.Component.translatable(m.nameKey())).orElse(net.minecraft.network.chat.Component.empty());
		return net.minecraft.network.chat.Component.translatable("aura.wildercord.banner.kicker", method,
			net.minecraft.network.chat.Component.translatable("aura.wildercord.banner.finisher"));
	}

	/** Finishers landed since the server started, and the last one's id (for the tests). */
	private static int finishers;
	private static String lastFinisher = "";

	public static int finishers() {
		return finishers;
	}

	public static String lastFinisher() {
		return lastFinisher;
	}

	// ------------------------------------------------------------------ upkeep

	private static void tick(MinecraftServer server) {
		if (TRACKED.isEmpty()) {
			return;
		}
		long tick = server.getTickCount();
		if (tick % 5 != 0) {
			return;
		}
		for (Iterator<Map.Entry<UUID, LivingEntity>> it = TRACKED.entrySet().iterator(); it.hasNext(); ) {
			LivingEntity e = it.next().getValue();
			if (e.isRemoved() || !e.isAlive()) {
				it.remove();
				continue;
			}
			State s = state(e);
			if (s == null) {
				it.remove();
				continue;
			}
			long now = e.level().getGameTime();
			if (s.opened(now)) {
				if ((now - s.openUntil()) % 10 == 0 && e.level() instanceof ServerLevel level) {
					// The opened mark everyone sees: a glint of gold over its head, and a ring on the ground now and then.
					AuraFx.burst(level, null, new Vec3(e.getX(), e.getBoundingBox().maxY + 0.45, e.getZ()), Vec3.ZERO, OPENED_COLOR, 0.55F,
						AuraFx.Burst.FLASH | AuraFx.Burst.STAR);
				}
				if ((now - s.openUntil()) % 20 == 0 && e.level() instanceof ServerLevel level) {
					AuraFx.burst(level, null, e.position().add(0, 0.08, 0), new Vec3(0, 1, 0), OPENED_COLOR, 1.0F + e.getBbWidth(), AuraFx.Burst.RING);
				}
				continue;
			}
			// Let go of a stance that has come back whole and stands steady no more (a boss's breaks are kept a minute).
			boolean whole = !s.steady(now) && s.wornAt(now) <= 1.0E-4;
			boolean keep = s.breaks() > 0 && s.kindOf() == StanceRules.Kind.BOSS && now - s.at() < 1200;
			if (whole && !keep) {
				e.removeAttached(STANCE);
				it.remove();
			}
		}
	}

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Stance::tick);
		// In a duel, a swordsman's blow caught on a raised shield wears the shield-bearer's stance (as a held Aura Guard's is worn).
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (blocked && entity instanceof ServerPlayer target && source.getDirectEntity() instanceof ServerPlayer striker && source.getEntity() == striker
					&& Aura.holdsWeapon(striker)) {
				guarded(target, striker, base);
			}
		});
	}

	static void forget(UUID id) {
		PENDING.remove(id);
	}

	static void clear() {
		TRACKED.clear();
		PENDING.clear();
	}
}
