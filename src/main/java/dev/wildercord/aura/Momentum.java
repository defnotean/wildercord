package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.PracticeRoom;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.player.WildercordAttachments;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Momentum at runtime (the rules and numbers are {@link MomentumRules}): each swordsman's meter, kept on the server and sent to its
 * owner only when it changes ({@link #MOMENTUM}); the ebb is worked out on both sides from what was sent. What feeds it is told
 * here by the rest of aura:
 * <ul>
 * <li>{@link #hit}: a blade's blow ({@code AuraCombat.blow}): a clean one builds, by the method's temper and the foe's state,
 *     within the foe's budget; a kill a little more;</li>
 * <li>{@link #artLanded}: each foe an art hurts ({@code ArtKit.Hits}): the first builds by the art's slot, a few more a little;</li>
 * <li>{@link #guarded}: a perfect guard ({@code AuraGuard}); {@link #stepThrough}: an Aura Step's untouchable moment turning a
 *     real blow aside ({@code AuraStep});</li>
 * <li>{@link #broke} and {@link #finished}: a stance broken, a finisher landed ({@code Stance});</li>
 * <li>{@link #struck}: a hit taken knocks a share off;</li>
 * <li>and playing the Final Art spends {@link MomentumRules#FINAL_SPEND} ({@link #performed}).</li>
 * </ul>
 * The Final Art waits on its peak ({@link #FINAL_GATE}, given to {@link AuraApi#gateFinalArts}); the arts' price falls with each
 * tier ({@link #price}) and their strikes grow ({@code ArtKit.Hits}). Others see a swordsman at a high tier by their body's aura
 * burning brighter ({@link AuraPresence.Look#momentum}).
 */
public final class Momentum {
	private Momentum() {}

	// ------------------------------------------------------------------ the state

	/**
	 * A swordsman's momentum as last written: {@code value} at the time it was written, held (no ebb) until {@code heldUntil}, then
	 * ebbing {@code ebbPerTick}; while an awakening holds it ({@code floorUntil}) never below {@code floor}. See
	 * {@link MomentumRules#current}.
	 */
	public record State(float value, long heldUntil, float ebbPerTick, float floor, long floorUntil) {
		public static final State NONE = new State(0, 0, 0, 0, 0);

		public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, State::value, ByteBufCodecs.VAR_LONG, State::heldUntil, ByteBufCodecs.FLOAT, State::ebbPerTick,
			ByteBufCodecs.FLOAT, State::floor, ByteBufCodecs.VAR_LONG, State::floorUntil, State::new);

		/** What it is at {@code now}. */
		public double at(long now) {
			return MomentumRules.current(value, heldUntil, ebbPerTick, floor, floorUntil, now);
		}
	}

	/** Each swordsman's momentum: not saved (a fight's, not a path's), gone with a death or a change of world, synced to its owner only. */
	public static final AttachmentType<State> MOMENTUM = AttachmentRegistry.create(
		Wildercord.id("aura_momentum"),
		builder -> builder.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	// ------------------------------------------------------------------ reading (both sides)

	/** Whether momentum works for {@code player}: on on this server, aura on, and a method learned. */
	public static boolean on(Player player) {
		return player != null && Config.momentum(player) && Aura.enabled(player) && Aura.stage(player) >= AuraRules.GLOW;
	}

	public static State state(Player player) {
		return player.getAttachedOrElse(MOMENTUM, State.NONE);
	}

	/** {@code player}'s momentum now, 0 to 100 (0 where it's off). */
	public static double value(Player player) {
		if (!on(player)) {
			return 0;
		}
		return state(player).at(player.level().getGameTime());
	}

	public static int tier(Player player) {
		return MomentumRules.tier(value(player));
	}

	public static boolean peak(Player player) {
		return MomentumRules.peak(value(player));
	}

	/** What {@code art} costs {@code player} now: its price, less at each tier of momentum. Both sides. */
	public static double price(Player player, AuraApi.StringArt art) {
		return MomentumRules.price(art.cost(), tier(player));
	}

	/**
	 * What every Final Art waits on: the peak of momentum. Where momentum is off on the server, a full pool of aura as before. Both
	 * sides read it from what's synced.
	 */
	public static final AuraApi.ArtCondition FINAL_GATE = new AuraApi.ArtCondition() {
		@Override
		public boolean met(Player player) {
			if (!Config.momentum(player)) {
				return PlaceholderArts.FULL_POOL.met(player);
			}
			return peak(player) || AuraApi.finalArtOpen(player);
		}

		@Override
		public String hintKey() {
			return "message.wildercord.aura.art.peak";
		}

		@Override
		public String hintKey(Player player) {
			return player != null && !Config.momentum(player) ? PlaceholderArts.FULL_POOL.hintKey() : hintKey();
		}
	};

	// ------------------------------------------------------------------ changing it (server)

	/** What a blow on each foe has built, by swordsman and foe: how much, and when the last blow came. */
	private static final Map<UUID, Map<UUID, double[]>> BUDGETS = new HashMap<>();
	/** When each swordsman's last step turned a blow aside (once a step). */
	private static final Map<UUID, Long> STEPPED = new HashMap<>();

	private static MomentumRules.Temper temper(ServerPlayer player) {
		return MomentumRules.temper(Aura.data(player).method());
	}

	private static double ebb(ServerPlayer player) {
		return MomentumRules.ebbPerTick(temper(player), Config.get().aura().momentum().momentumEbb());
	}

	/** Writes {@code value} (held as a fight holds it, from now), keeping any awakening's hold. */
	private static void write(ServerPlayer player, double value, boolean engaged) {
		long now = player.level().getGameTime();
		State old = state(player);
		long held = engaged ? now + temper(player).grace() : Math.min(old.heldUntil(), now);
		State next = new State((float) MomentumRules.clamp(value), held, (float) ebb(player), old.floor(), old.floorUntil());
		if (!next.equals(old)) {
			player.setAttached(MOMENTUM, next);
		}
	}

	/**
	 * Builds {@code amount} (already by the temper), for {@code source}, through the {@link AuraApi#onMomentum} hooks and the
	 * server's {@code momentum_gain}, never past {@code ceiling} (what was there already above it stays). Returns what it built.
	 */
	public static double add(ServerPlayer player, double amount, String source, double ceiling) {
		if (!on(player) || amount <= 0) {
			return 0;
		}
		double a = amount * Math.max(0, Config.get().aura().momentum().momentumGain());
		for (AuraApi.MomentumHook hook : AuraApi.momentumHooks()) {
			try {
				a = hook.modify(player, a, source);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A momentum hook threw; skipping it", e);
			}
		}
		long now = player.level().getGameTime();
		double before = state(player).at(now);
		double after = Math.max(before, Math.min(Math.min(MomentumRules.MAX, ceiling), before + Math.max(0, a)));
		write(player, after, true);
		climbed(player, MomentumRules.tier(before), MomentumRules.tier(after));
		return after - before;
	}

	/** Knocks {@code amount} off (a hit taken, the Final Art's release). */
	public static void lose(ServerPlayer player, double amount, boolean engaged) {
		if (!on(player) || amount <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		double before = state(player).at(now);
		write(player, before - amount, engaged);
		refreshLook(player);
	}

	/** A fight goes on (a blow given or taken that built nothing): momentum holds a while longer before it ebbs. */
	public static void engaged(ServerPlayer player) {
		if (!on(player)) {
			return;
		}
		long now = player.level().getGameTime();
		State s = state(player);
		if (s.value() <= 0 && s.floorUntil() < now) {
			return;
		}
		long held = now + temper(player).grace();
		// Written only when the hold moves on by half a second, so a long fight sends little.
		if (held - s.heldUntil() >= 10) {
			player.setAttached(MOMENTUM, new State((float) s.at(now), held, (float) ebb(player), s.floor(), s.floorUntil()));
		}
	}

	/**
	 * Holds {@code player}'s momentum at {@code floor} or above, unebbing, for {@code ticks} (step 6's awakening, through
	 * {@link AuraApi#holdMomentum}): what's there is lifted to the floor at once.
	 */
	public static void hold(ServerPlayer player, double floor, int ticks) {
		if (!on(player)) {
			return;
		}
		long now = player.level().getGameTime();
		double before = state(player).at(now);
		double f = MomentumRules.clamp(floor);
		double value = Math.max(before, f);
		player.setAttached(MOMENTUM, new State((float) value, now + temper(player).grace(), (float) ebb(player), (float) f,
			ticks <= 0 ? 0 : now + ticks));
		climbed(player, MomentumRules.tier(before), MomentumRules.tier(value));
	}

	/** Empties it (a death, a change of world, the practice arena left). */
	public static void reset(ServerPlayer player) {
		if (player.hasAttached(MOMENTUM)) {
			player.removeAttached(MOMENTUM);
		}
		BUDGETS.remove(player.getUUID());
		refreshLook(player);
	}

	/** A tier gained: a rising note for its swordsman, and at the peak a ring and the body's aura blazing up. */
	private static void climbed(ServerPlayer player, int from, int to) {
		if (to <= from) {
			return;
		}
		refreshLook(player);
		if (to >= MomentumRules.PEAK_TIER) {
			Feels.sound(player.level(), player.position().add(0, 1, 0), "aura_momentum_peak", 0.8F, 1.0F);
			AuraFx.bodyAuraFlare(player, 40, 0.75F);
			Grimoire.unlock(player, "aura:peak_momentum");
		} else {
			Feels.sound(player.level(), player.position().add(0, 1, 0), "aura_momentum_rise", 0.45F, Feels.step(to - 1));
		}
	}

	/** Keeps everyone's view of the swordsman's tier current (their body's aura burns brighter with it). */
	static void refreshLook(ServerPlayer player) {
		int tier = tier(player);
		AuraPresence.Look look = AuraPresence.look(player);
		if (look.momentum() != tier) {
			AuraPresence.look(player, look.withMomentum(tier));
		}
	}

	/** Every half second or so: the look follows the ebb, and old budgets are let go. */
	static void tick(ServerPlayer player, long now) {
		if ((now + player.getId()) % 10 == 0) {
			refreshLook(player);
			Map<UUID, double[]> budgets = BUDGETS.get(player.getUUID());
			if (budgets != null) {
				budgets.values().removeIf(b -> now - (long) b[1] > MomentumRules.BUDGET_MEMORY || now < (long) b[1]);
				if (budgets.isEmpty()) {
					BUDGETS.remove(player.getUUID());
				}
			}
		}
	}

	// ------------------------------------------------------------------ what feeds it

	/** Whether {@code target} is a practice target: a training dummy, or anything in the practice arena. */
	static boolean practice(ServerPlayer player, LivingEntity target) {
		return target instanceof TrainingDummy || player.level().dimension() == PracticeRoom.DIMENSION;
	}

	/** How high blows on {@code target} may lift it. */
	private static double ceiling(ServerPlayer player, LivingEntity target) {
		return MomentumRules.ceiling(practice(player, target), player.level().dimension() == PracticeRoom.DIMENSION);
	}

	/** What a practice target leaves of a gain. */
	private static double share(ServerPlayer player, LivingEntity target) {
		return practice(player, target) ? MomentumRules.PRACTICE_SHARE : 1.0;
	}

	/**
	 * Whether {@code target} can't fight back: a creature with no mind of its own (not one an art or a stagger holds a moment), or
	 * one riding a boat or a cart. Blows on it build nothing.
	 */
	public static boolean helpless(LivingEntity target) {
		if (target instanceof Mob mob && mob.isNoAi() && !mob.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
			return true;
		}
		return target.isPassenger() && !(target.getVehicle() instanceof LivingEntity);
	}

	/** Whether blows on {@code target} may build momentum at all: a real foe (or a practice target) that can fight back. */
	static boolean worthy(ServerPlayer player, LivingEntity target) {
		if (practice(player, target)) {
			return true;
		}
		return AuraCombat.worth(player, target) > 0 && !helpless(target);
	}

	/** The states {@code target} is in that a method's blows feed on ({@link MomentumRules} bits). */
	public static int states(ServerPlayer player, LivingEntity target) {
		int s = 0;
		if (target.getRemainingFireTicks() > 0) {
			s |= MomentumRules.BURNING;
		}
		if (target.getTicksFrozen() > 0 || Reactions.has(target, Reactions.Mark.FROZEN)) {
			s |= MomentumRules.CHILLED;
		}
		if (Reactions.has(target, Reactions.Mark.IONISED)) {
			s |= MomentumRules.IONISED;
		}
		if (!target.onGround() && Reactions.has(target, Reactions.Mark.AIRBORNE)) {
			s |= MomentumRules.AIRBORNE;
		}
		if (Reactions.has(target, Reactions.Mark.CRACKED)) {
			s |= MomentumRules.CRACKED;
		}
		if (dev.wildercord.aura.arts.ArtKit.rooted(target)) {
			s |= MomentumRules.ROOTED;
		}
		if (dev.wildercord.aura.arts.ArtWards.silenced(target)) {
			s |= MomentumRules.SILENCED;
		}
		if (Reactions.has(target, Reactions.Mark.SHADOWED)) {
			s |= MomentumRules.SHADOWED;
		}
		if (dev.wildercord.aura.arts.ArtWards.starred(player, target)) {
			s |= MomentumRules.STARRED;
		}
		if (dev.wildercord.aura.arts.ArtWards.stopped(player, target) || target.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
			s |= MomentumRules.STOPPED;
		}
		if (Reactions.has(target, Reactions.Mark.BLEEDING)) {
			s |= MomentumRules.BLEEDING;
		}
		return s;
	}

	/**
	 * A blade's blow on {@code target} ({@code AuraCombat.blow}, after it landed or didn't): a full swing that hurt a worthy foe
	 * builds, within the foe's budget (more on a foe in a state the method feeds on), a kill a little more; any blow on a foe holds
	 * what's there.
	 */
	static void hit(ServerPlayer player, LivingEntity target, float swing, boolean critical, boolean first, boolean hurt, boolean killed) {
		if (!on(player) || !first) {
			return;
		}
		if (!worthy(player, target)) {
			return;
		}
		if (!hurt || swing < AuraRules.FULL_SWING - 1.0E-4) {
			engaged(player);
			return;
		}
		MomentumRules.Temper temper = temper(player);
		double amount = temper.hitOn(critical, states(player, target)) * share(player, target);
		amount = fromBudget(player, target, amount);
		if (killed && !practice(player, target)) {
			amount += MomentumRules.KILL;
		}
		if (amount > 0) {
			add(player, amount, "hit", ceiling(player, target));
		} else {
			engaged(player);
		}
	}

	/** What's left of {@code amount} in {@code target}'s budget for {@code player}'s blows, spending it. */
	private static double fromBudget(ServerPlayer player, LivingEntity target, double amount) {
		long now = player.level().getGameTime();
		Map<UUID, double[]> budgets = BUDGETS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		double[] b = budgets.computeIfAbsent(target.getUUID(), k -> new double[] {0, now});
		int memory = target instanceof Player ? MomentumRules.PLAYER_BUDGET_MEMORY : MomentumRules.BUDGET_MEMORY;
		if (now - (long) b[1] > memory || now < (long) b[1]) {
			b[0] = 0;
		}
		double budget = MomentumRules.budget(target.getMaxHealth(), Spirits.isBoss(target), target instanceof Player, target instanceof TrainingDummy);
		double got = MomentumRules.fromBudget(amount, b[0], budget);
		b[0] += got;
		b[1] = now;
		if (budgets.size() > 64) {
			budgets.values().removeIf(x -> now - (long) x[1] > MomentumRules.PLAYER_BUDGET_MEMORY);
		}
		return got;
	}

	/** A foe's budget starts again: its stance was broken (each opening earned lets blows build again). */
	static void freshBudget(ServerPlayer player, LivingEntity target) {
		Map<UUID, double[]> budgets = BUDGETS.get(player.getUUID());
		if (budgets != null) {
			budgets.remove(target.getUUID());
		}
	}

	/**
	 * An art of {@code player}'s hurt {@code foe}, the {@code n}th it has hurt ({@code ArtKit.Hits}): the first builds by the art's
	 * slot, a few more a little each (Thunder's temper twice as much); from afar it counts half (Gale's whole), and a practice
	 * target its share.
	 */
	public static void artLanded(ServerPlayer player, AuraApi.StringArt art, int n, LivingEntity foe) {
		if (!on(player) || art == null || !worthy(player, foe)) {
			return;
		}
		int slot = AuraApi.ArtSlot.of(art).map(Enum::ordinal).orElse(ArtRules.slot(art.stage()));
		MomentumRules.Temper temper = temper(player);
		double amount = MomentumRules.artFoe(slot, n, temper) * MomentumRules.reach(player.distanceTo(foe), temper) * share(player, foe);
		if (amount > 0) {
			add(player, amount, "art", ceiling(player, foe));
		} else {
			engaged(player);
		}
	}

	/** A perfect guard ({@code AuraGuard}): against a blow most, against a projectile or a spell less. */
	static void guarded(ServerPlayer player, boolean blow) {
		if (!on(player)) {
			return;
		}
		add(player, (blow ? MomentumRules.PERFECT_GUARD : MomentumRules.PERFECT_DEFLECT) * temper(player).guard(), "guard", MomentumRules.MAX);
	}

	/** An Aura Step's untouchable moment turned a real blow or shot aside ({@code AuraStep}): once a step. */
	static void stepThrough(ServerPlayer player, DamageSource source) {
		if (!on(player) || source.getEntity() == null && source.getDirectEntity() == null) {
			return;
		}
		long stepAt = AuraPresence.timers(player).stepReadyAt();
		Long last = STEPPED.get(player.getUUID());
		if (last != null && last == stepAt) {
			return;
		}
		STEPPED.put(player.getUUID(), stepAt);
		add(player, MomentumRules.STEP_THROUGH * temper(player).step(), "step", MomentumRules.MAX);
	}

	/** {@code player} broke {@code foe}'s stance ({@code Stance}): momentum, and the foe's budget starts again. */
	static void broke(ServerPlayer player, LivingEntity foe) {
		freshBudget(player, foe);
		if (on(player)) {
			add(player, MomentumRules.BREAK * share(player, foe), "break", ceiling(player, foe));
		}
	}

	/** {@code player} landed a finisher on {@code foe} ({@code Stance}). */
	static void finished(ServerPlayer player, LivingEntity foe) {
		if (on(player)) {
			add(player, MomentumRules.FINISHER * share(player, foe), "finisher", ceiling(player, foe));
		}
	}

	/**
	 * A hit taken (anything a creature, a player or their shot dealt): a share knocked off, less through a held guard and by the
	 * temper; Crimson's builds a little while bloodied.
	 */
	static void struck(ServerPlayer player, DamageSource source, float taken, boolean guarded) {
		if (!on(player) || taken <= 0 || source.getEntity() == null && source.getDirectEntity() == null) {
			return;
		}
		MomentumRules.Temper temper = temper(player);
		long now = player.level().getGameTime();
		double before = state(player).at(now);
		double lost = MomentumRules.loss(before, taken, player.getMaxHealth(), guarded, temper);
		if (lost > 0) {
			lose(player, lost, true);
		} else {
			engaged(player);
		}
		if (temper.bloodied() && player.getHealth() <= player.getMaxHealth() * MomentumRules.BLOODIED_AT) {
			add(player, MomentumRules.BLOODIED, "bloodied", MomentumRules.MAX);
		}
	}

	/** Every art performed ({@link AuraApi#onString}): the Final Art's release spends what built to it. */
	static void performed(ServerPlayer player, AuraApi.StringArt art) {
		if (on(player) && AuraApi.ArtSlot.of(art).orElse(null) == AuraApi.ArtSlot.FINAL) {
			lose(player, MomentumRules.FINAL_SPEND, true);
		}
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		AuraApi.gateFinalArts(FINAL_GATE);
		AuraApi.onString((player, art, context) -> performed(player, art));
		// A hit taken knocks a share off (a guarded one's is told by AuraGuard first, so it's counted there).
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && taken > 0 && Aura.stage(player) > AuraRules.NONE) {
				struck(player, source, taken, AuraGuard.guarding(player) && AuraGuard.covers(player, source));
			}
		});
		// A new world, a new fight: the arena's practice stays in the arena.
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> reset(player));
	}

	static void forget(UUID id) {
		BUDGETS.remove(id);
		STEPPED.remove(id);
	}

	static void clear() {
		BUDGETS.clear();
		STEPPED.clear();
	}
}
