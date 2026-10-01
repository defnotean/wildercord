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
 * Breakthroughs: at each stage's threshold aura waits, full to its limit, for a trial. Either of the built-in trials makes it:
 * <ul>
 * <li>{@link #STILLNESS}: the breathing stance held unbroken for half a minute at a place of power (a ley crossing);</li>
 * <li>{@link #STRONGER_FOE}: a foe stronger than you (a boss, a Runebound, or one with twice your health or more) felled by
 * your blade within a minute of your first blow on it, with no spell of yours touching it.</li>
 * </ul>
 * A later stage can allow these or bring its own ({@link AuraApi#allowTrial}, {@link AuraApi#completeTrial}). Success is a
 * moment: a burst of aura in the method's colour, a title, a sound, and a Grimoire entry ({@code aura:<stage>}).
 */
public final class AuraBreakthroughs {
	private AuraBreakthroughs() {}

	public static final String STILLNESS = "stillness";
	public static final String STRONGER_FOE = "stronger_foe";

	/** A stronger foe under way: which, until when, and whether a spell of the player's has touched it. */
	private record Foe(UUID target, long until, boolean tainted) {}

	private static final Map<UUID, Foe> FOES = new HashMap<>();
	/** Stillness held so far, by player (written into the synced state a few times a second). */
	private static final Map<UUID, Integer> STILL = new HashMap<>();

	static void init() {}

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
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.waiting_how").withColor(0xB8A8D8));
		Aura.sound(player, "aura_breath", 0.8F, dev.wildercord.cast.feel.Feels.step(4));
	}

	// ------------------------------------------------------------------ stillness

	/** Called every tick while the player is in the breathing stance. */
	static void stillness(ServerPlayer player, long now) {
		if (!ready(player) || !allowed(player, STILLNESS) || !PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition())) {
			if (STILL.remove(player.getUUID()) != null) {
				Aura.state(player, Aura.state(player).stillness(0));
			}
			return;
		}
		int held = STILL.merge(player.getUUID(), 1, Integer::sum);
		if (held == 1) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.stillness_begins").withColor(0xFF000000 | Aura.color(player)));
		}
		if (held >= AuraRules.STILLNESS_TICKS) {
			STILL.remove(player.getUUID());
			complete(player, STILLNESS);
		} else if (held % 5 == 0) {
			Aura.state(player, Aura.state(player).stillness(held));
		}
	}

	/** The stance broke: stillness starts over. */
	static void broken(ServerPlayer player) {
		STILL.remove(player.getUUID());
	}

	// ------------------------------------------------------------------ a stronger foe

	/** A blow of the player's landed on {@code target} (from {@link AuraCombat}). */
	static void struck(ServerPlayer player, LivingEntity target, boolean killed, boolean practice) {
		if (practice || !ready(player) || !allowed(player, STRONGER_FOE)) {
			return;
		}
		boolean stronger = AuraRules.stronger(Spirits.isBoss(target), target.hasAttached(WildercordAttachments.RUNEBOUND), target.getMaxHealth(),
			player.getMaxHealth());
		if (!stronger) {
			return;
		}
		long now = player.level().getGameTime();
		Foe foe = FOES.get(player.getUUID());
		if (foe == null || !foe.target().equals(target.getUUID()) || now > foe.until()) {
			foe = new Foe(target.getUUID(), now + AuraRules.TRIAL_WINDOW, false);
			FOES.put(player.getUUID(), foe);
			Aura.state(player, Aura.state(player).trial(foe.until()));
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.trial_begins").withColor(0xFF000000 | Aura.color(player)));
		}
		if (killed) {
			FOES.remove(player.getUUID());
			Aura.state(player, Aura.state(player).trial(-1));
			if (foe.tainted()) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.aura.trial_tainted").withColor(0xC8A0A0));
			} else if (now <= foe.until()) {
				complete(player, STRONGER_FOE);
			}
		}
	}

	/** A spell of the player's touched {@code target}: if it's their trial's foe, the trial can't be won on it. */
	static void tainted(ServerPlayer player, LivingEntity target) {
		Foe foe = FOES.get(player.getUUID());
		if (foe != null && foe.target().equals(target.getUUID()) && !foe.tainted()) {
			FOES.put(player.getUUID(), new Foe(foe.target(), foe.until(), true));
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

	/** The breakthrough itself (also {@code /wildercord aura breakthrough} and the game tests): the next stage, full aura, and the moment. */
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
	}
}
