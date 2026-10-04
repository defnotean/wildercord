package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.PowerPlaces;
import dev.wildercord.cast.Spirits;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Breakthroughs: at each stage's threshold aura waits, full to its limit, for a trial. For Flow and Edge either of these makes
 * it:
 * <ul>
 * <li>{@link #STILLNESS}: the breathing stance held unbroken for half a minute at a place of power (a ley crossing);</li>
 * <li>{@link #STRONGER_FOE}: a foe stronger than you (a boss, a Runebound, or one with twice your health or more) felled by
 * your blade within a minute of your first blow on it, with no spell of yours touching it.</li>
 * </ul>
 * The top stages ask more. For Form and Sovereign:
 * <ul>
 * <li>{@link #TEMPEST}: the stance held unbroken at a ley crossing through a thunderstorm, open to the sky (45 seconds for
 * Form, a minute for Sovereign); a lightning strike breaks it, as any blow does;</li>
 * <li>{@link #GUARDIAN}: a boss (a dungeon's guardian, the Wither, the Warden...) felled by blade and aura alone within three
 * minutes of your first blow on it, no spell of yours touching it;</li>
 * <li>and the duelists' {@link #DUEL}, which they allow for the stages they teach.</li>
 * </ul>
 * Any stage can allow these or bring its own ({@link AuraApi#allowTrial}, {@link AuraApi#completeTrial}). Success is a moment:
 * a burst of aura in the method's colour, a title, a sound, and a Grimoire entry ({@code aura:<stage>}).
 */
public final class AuraBreakthroughs {
	private AuraBreakthroughs() {}

	public static final String STILLNESS = "stillness";
	public static final String STRONGER_FOE = "stronger_foe";
	public static final String TEMPEST = "tempest";
	public static final String GUARDIAN = "guardian";
	/**
	 * An aura duel won against a duelist: not allowed for any stage here; the duelists allow it ({@link AuraApi#allowTrial}) for the
	 * stages they teach and complete it ({@link AuraApi#completeTrial}) when a duel is won. Its line on the Aura page is
	 * {@code screen.wildercord.aura.trial.duel}.
	 */
	public static final String DUEL = "duel";

	/** A worthy foe under way: until when, whether a spell of the player's has touched it, and the trial it's for. */
	private record Foe(long until, boolean tainted, String trial) {}

	/** The stronger foes each player has struck lately, by foe: a sweep or a slash can open several at once. */
	private static final Map<UUID, Map<UUID, Foe>> FOES = new HashMap<>();
	/** The most foes one player's trial keeps track of at once. */
	private static final int MAX_FOES = 8;
	/** Stillness held so far, by player (written into the synced state a few times a second). */
	private static final Map<UUID, Integer> STILL = new HashMap<>();

	static void init() {}

	/** A player left: stillness starts over and their trials' foes are let go (an unbroken stance can't span a logout). */
	static void forget(UUID id) {
		FOES.remove(id);
		STILL.remove(id);
	}

	static void clear() {
		FOES.clear();
		STILL.clear();
	}

	/** Whether a breakthrough waits for {@code player}: the threshold is reached and the next stage is open. */
	public static boolean ready(net.minecraft.world.entity.player.Player player) {
		AuraAttachments.Data data = Aura.data(player);
		return data.learned() && AuraStages.canBreakThrough(data.stage()) && AuraRules.ready(data.xp(), AuraStages.cap(data.stage()));
	}

	/** Whether {@code trial} can make {@code player}'s waiting breakthrough. */
	static boolean allowed(ServerPlayer player, String trial) {
		return AuraApi.trials(Aura.stage(player) + 1).contains(trial);
	}

	/** The threshold is reached: the player is told a breakthrough waits, and how it's made. */
	static void waiting(ServerPlayer player) {
		int next = Aura.stage(player) + 1;
		Component stage = Component.translatable("aura.wildercord.stage." + AuraStages.id(next)).withColor(0xFF000000 | Aura.color(player));
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.waiting", stage).withColor(0xE8D8B0));
		player.sendSystemMessage(Component.translatable(next >= AuraRules.FORM ? "message.wildercord.aura.waiting_how_top"
			: "message.wildercord.aura.waiting_how").withColor(0xB8A8D8));
		Aura.sound(player, "aura_breath", 0.8F, dev.wildercord.cast.feel.Feels.step(4));
	}

	// ------------------------------------------------------------------ stillness

	/**
	 * The stillness trial the stance is held toward now, if any: the ley crossing's own ({@link #STILLNESS}), or at the top stages
	 * the tempest's ({@link #TEMPEST}: a ley crossing under a thunderstorm, open to the sky). Null when neither can be met here.
	 */
	static String stillTrial(ServerPlayer player) {
		if (!ready(player)) {
			return null;
		}
		boolean power = PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition());
		if (allowed(player, STILLNESS) && (power || dev.wildercord.aura.world.TrainingGrounds.ground(player)
			!= dev.wildercord.aura.world.TrainingRules.Ground.NONE)) {
			return STILLNESS;
		}
		if (power && allowed(player, TEMPEST) && tempest(player)) {
			return TEMPEST;
		}
		return null;
	}

	/** Whether a thunderstorm rages over the player, with nothing between them and the sky. */
	static boolean tempest(ServerPlayer player) {
		return player.level().isThundering() && player.level().canSeeSky(player.blockPosition().above());
	}

	/** Called every tick while the player is in the breathing stance. */
	static void stillness(ServerPlayer player, long now) {
		String trial = stillTrial(player);
		if (trial == null) {
			if (STILL.remove(player.getUUID()) != null) {
				Aura.state(player, Aura.state(player).stillness(0));
			}
			return;
		}
		int held = STILL.merge(player.getUUID(), 1, Integer::sum);
		if (held == 1) {
			player.sendOverlayMessage(Component.translatable(trial.equals(TEMPEST) ? "message.wildercord.aura.tempest_begins"
				: "message.wildercord.aura.stillness_begins").withColor(0xFF000000 | Aura.color(player)));
		}
		if (held >= AuraRules.stillnessTicks(Aura.stage(player) + 1)) {
			STILL.remove(player.getUUID());
			complete(player, trial);
		} else if (held % 5 == 0) {
			Aura.state(player, Aura.state(player).stillness(held));
		}
	}

	/** The stance broke: stillness starts over. */
	static void broken(ServerPlayer player) {
		STILL.remove(player.getUUID());
	}

	// ------------------------------------------------------------------ a stronger foe

	/**
	 * The foe trial a blow on {@code target} could open toward the waiting breakthrough: a boss for the guardian's (the top
	 * stages), a stronger foe for the earlier stages', or null when it's neither.
	 */
	static String foeTrial(ServerPlayer player, LivingEntity target) {
		boolean boss = Spirits.isBoss(target);
		if (boss && allowed(player, GUARDIAN)) {
			return GUARDIAN;
		}
		if (allowed(player, STRONGER_FOE) && AuraRules.stronger(boss, target.hasAttached(WildercordAttachments.RUNEBOUND), target.getMaxHealth(),
				player.getMaxHealth())) {
			return STRONGER_FOE;
		}
		return null;
	}

	/** A blow of the player's landed on {@code target} (from {@link AuraCombat}). */
	static void struck(ServerPlayer player, LivingEntity target, boolean killed, boolean practice) {
		if (practice || !ready(player)) {
			return;
		}
		String trial = foeTrial(player, target);
		if (trial == null) {
			return;
		}
		long now = player.level().getGameTime();
		Map<UUID, Foe> foes = FOES.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		foes.values().removeIf(f -> now > f.until());
		Foe foe = foes.get(target.getUUID());
		if (foe == null) {
			if (foes.size() >= MAX_FOES) {
				return;
			}
			foe = new Foe(now + (trial.equals(GUARDIAN) ? AuraRules.GUARDIAN_WINDOW : AuraRules.TRIAL_WINDOW), false, trial);
			foes.put(target.getUUID(), foe);
			player.sendOverlayMessage(Component.translatable(trial.equals(GUARDIAN) ? "message.wildercord.aura.guardian_begins"
				: "message.wildercord.aura.trial_begins").withColor(0xFF000000 | Aura.color(player)));
		}
		if (killed) {
			foes.remove(target.getUUID());
			if (foe.tainted()) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.aura.trial_tainted").withColor(0xC8A0A0));
			} else if (now <= foe.until() && complete(player, foe.trial())) {
				return;
			}
		}
		// The HUD counts down the trial that runs out last.
		long last = foes.values().stream().filter(f -> !f.tainted()).mapToLong(Foe::until).max().orElse(-1);
		Aura.state(player, Aura.state(player).trial(last));
	}

	/** Whether a spell of the player's has spoiled their trial on {@code target} (the server's view). */
	public static boolean spoiled(net.minecraft.world.entity.player.Player player, LivingEntity target) {
		Map<UUID, Foe> foes = FOES.get(player.getUUID());
		Foe foe = foes == null ? null : foes.get(target.getUUID());
		return foe != null && foe.tainted();
	}

	/** A spell of the player's touched {@code target}: if it's a foe of their trial, the trial can't be won on it. */
	static void tainted(ServerPlayer player, LivingEntity target) {
		Map<UUID, Foe> foes = FOES.get(player.getUUID());
		Foe foe = foes == null ? null : foes.get(target.getUUID());
		if (foe != null && !foe.tainted()) {
			foes.put(target.getUUID(), new Foe(foe.until(), true, foe.trial()));
		}
	}

	// ------------------------------------------------------------------ the breakthrough

	/** Makes the waiting breakthrough by {@code trial}, if one waits and the trial counts for it. Returns whether it was made. */
	public static boolean complete(ServerPlayer player, String trial) {
		if (!ready(player) || !allowed(player, trial)) {
			return false;
		}
		breakThrough(player);
		return true;
	}

	/** The breakthrough itself (also the game tests): the next stage, full aura, and the moment. */
	public static void breakThrough(ServerPlayer player) {
		AuraAttachments.Data data = Aura.data(player);
		int next = Math.min(AuraStages.highest(), data.stage() + 1);
		if (!data.learned() || next <= data.stage()) {
			return;
		}
		Aura.set(player, data.withStage(next).withAura(AuraStages.capacity(next)));
		FOES.remove(player.getUUID());
		STILL.remove(player.getUUID());
		Aura.state(player, Aura.state(player).stillness(0).trial(-1));
		int color = Aura.color(player);
		Component stage = Component.translatable("aura.wildercord.stage." + AuraStages.id(next));
		player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(stage.copy().withColor(0xFF000000 | color)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.wildercord.aura.breakthrough_subtitle").withColor(0xE8D8B0)));
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.breakthrough", stage.copy().withColor(0xFF000000 | color)).withColor(0xE8D8B0));
		Aura.sound(player, "aura_breakthrough", 1.0F, 1.0F);
		AuraVfx.breakthrough(player, color, next);
		Grimoire.unlock(player, "aura:" + AuraStages.id(next));
		// A bonded blade carried through it remembers it (and may wake to a tier the new stage allows).
		BondedBlades.brokeThrough(player, next);
		// A disciple's master earns a share of the road walked; a disciple who has reached their master's stage graduates.
		Lineage.brokeThrough(player, next);
		if (next == WayRules.FROM) {
			// The path forks here: once the moment has settled, the crossroads rises for the swordsman to choose their Way.
			Crossroads.brokeThrough(player);
		}
	}
}
