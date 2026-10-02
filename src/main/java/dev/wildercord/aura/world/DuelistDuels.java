package dev.wildercord.aura.world;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraExperience;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.SigilOption;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Duels with a wandering duelist: the player duel's rules ({@link DuelRules}: a countdown, the fight, a knockout, leaving the
 * area, a draw after five minutes) with a duelist in the second place. One player and one duelist, nobody else: the duelist
 * harms only its challenger and takes harm only from them, and another player striking the challenger calls it off. Nobody
 * dies: the challenger brought down is knocked out on one health, the duelist yields on one knee.
 *
 * <p>When it ends the challenger is put back as they began, as a player duel does it: the health the duelist took is given
 * back (never more than they had) and the harmful effects it left are taken off, so losing costs nothing but pride. Winning
 * teaches the duelist's method (or, to one already breathing another way, hands over its manual to choose), a little aura
 * experience, a Grimoire entry, and, if no magic touched the fight, the {@link #TRIAL} breakthrough trial; then the duelist
 * bows and goes.</p>
 */
public final class DuelistDuels {
	private DuelistDuels() {}

	/** The breakthrough trial a won duel completes (allowed for Form and Sovereign). */
	public static final String TRIAL = "duel";
	/** A duelist's lesson, as a source of methods (for {@link AuraApi#grantMethod}). */
	public static final String SOURCE = "duelist";

	private static final int GOLD = 0xFFD870;

	/** A challenge offered: which duelist, and until when a second use accepts it. */
	private record Offer(UUID duelist, long until) {}

	/** A duel under way. */
	private static final class Active {
		final DuelRules.Duel duel;
		final ServerLevel level;
		final Vec3 centre;
		final UUID player;
		final UUID duelist;
		final int stage;
		final float healthBefore;
		final Set<Holder<MobEffect>> hadBefore = new HashSet<>();
		/** The health the duelist's blows and slashes took, and the harmful effects it laid on, to give back and take off. */
		float taken;
		/** The challenger's health as a blow of the duelist's began to land (at a knockout, what it found is what it took). */
		float landing = -1;
		final Set<Holder<MobEffect>> harms = new HashSet<>();
		/** Whether any magic of the challenger's touched the duelist (the duel still teaches, but isn't a trial). */
		boolean magic;
		int shown = -1;

		Active(DuelRules.Duel duel, ServerLevel level, Vec3 centre, ServerPlayer player, Duelist duelist, int stage) {
			this.duel = duel;
			this.level = level;
			this.centre = centre;
			this.player = player.getUUID();
			this.duelist = duelist.getUUID();
			this.stage = stage;
			this.healthBefore = player.getHealth();
			for (MobEffectInstance effect : player.getActiveEffects()) {
				hadBefore.add(effect.getEffect());
			}
		}
	}

	private static final Map<UUID, Offer> OFFERS = new HashMap<>();
	private static final Map<UUID, Active> BY_PLAYER = new HashMap<>();
	private static final Map<UUID, Active> BY_DUELIST = new HashMap<>();

	public static void init() {
		AuraApi.allowTrial(AuraRules.FORM, TRIAL);
		AuraApi.allowTrial(AuraRules.SOVEREIGN, TRIAL);
		ServerTickEvents.END_SERVER_TICK.register(DuelistDuels::tick);
		// The challenger's health as each of the duelist's blows begins to land.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!BY_PLAYER.isEmpty() && entity instanceof ServerPlayer player && BY_PLAYER.get(player.getUUID()) instanceof Active active
					&& source.getEntity() instanceof Duelist duelist && duelist.getUUID().equals(active.duelist)) {
				active.landing = player.getHealth();
			}
			return true;
		});
		// Brought down by the duelist: knocked out, not killed.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !(BY_PLAYER.get(player.getUUID()) instanceof Active active) || !active.duel.fighting()
					|| !(source.getEntity() instanceof Duelist duelist) || !duelist.getUUID().equals(active.duelist)) {
				return true;
			}
			// All the health the last blow found, less the one they're left with (by now their health already reads nothing).
			active.taken += Math.max(0, (active.landing >= 0 ? active.landing : amount) - 1.0F);
			active.landing = -1;
			player.setHealth(1.0F);
			active.duel.knockout(player.getUUID());
			finish(active, player.level().getServer());
			return false;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (BY_PLAYER.isEmpty() || !(entity instanceof ServerPlayer player) || !(BY_PLAYER.get(player.getUUID()) instanceof Active active)) {
				return;
			}
			if (source.getEntity() instanceof Duelist duelist && duelist.getUUID().equals(active.duelist)) {
				active.taken += Math.max(0, taken);
				active.landing = -1;
			} else if (playerBehind(source) instanceof Player other && other != player) {
				// Another player striking the challenger: the duel is off.
				active.duel.interrupt();
				finish(active, player.level().getServer());
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player && BY_PLAYER.get(player.getUUID()) instanceof Active active) {
				active.duel.forfeit(player.getUUID(), DuelRules.Ending.DIED);
				finish(active, player.level().getServer());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			OFFERS.remove(handler.player.getUUID());
			if (BY_PLAYER.get(handler.player.getUUID()) instanceof Active active) {
				active.duel.forfeit(active.player, DuelRules.Ending.LOGGED_OFF);
				finish(active, server, handler.player);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Active active : List.copyOf(BY_PLAYER.values())) {
				active.duel.interrupt();
				finish(active, server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			OFFERS.clear();
			BY_PLAYER.clear();
			BY_DUELIST.clear();
		});
	}

	// ------------------------------------------------------------------ for the rest of the mod

	/** Whether {@code player} is in a duel with a duelist (countdown included). */
	public static boolean inDuel(Player player) {
		return BY_PLAYER.containsKey(player.getUUID());
	}

	/** Whether {@code duelist}'s duel is past its countdown. */
	public static boolean fighting(Duelist duelist) {
		Active active = BY_DUELIST.get(duelist.getUUID());
		return active != null && active.duel.fighting();
	}

	/** The phase of {@code player}'s duel with a duelist, or null (for the tests). */
	public static DuelRules.Phase phase(Player player) {
		Active active = BY_PLAYER.get(player.getUUID());
		return active == null ? null : active.duel.phase();
	}

	/** The stage the duelist in {@code player}'s duel fights at, or 0. */
	public static int stage(Player player) {
		Active active = BY_PLAYER.get(player.getUUID());
		return active == null ? 0 : active.stage;
	}

	/**
	 * Whether this harm reaches {@code duelist}: only its challenger's (a blow, a shot, their pet, their spell or slash), and
	 * only once the fight is on. Magic of theirs is noted: it spoils the duel as a trial.
	 */
	static boolean counts(Duelist duelist, DamageSource source) {
		Active active = BY_DUELIST.get(duelist.getUUID());
		if (active == null || !active.duel.fighting()) {
			return false;
		}
		Player behind = playerBehind(source);
		if (behind == null || !behind.getUUID().equals(active.player)) {
			return false;
		}
		boolean blade = source.getDirectEntity() == behind && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.SPEAR)) || source.is(Aura.DAMAGE);
		if (!blade && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)
				|| dev.wildercord.cast.Effects.applying() != null)) {
			active.magic = true;
		}
		return true;
	}

	// ------------------------------------------------------------------ the challenge

	/** A player used a duelist: the first use offers the duel, a second within ten seconds accepts it. */
	static void use(ServerPlayer player, Duelist duelist) {
		long now = player.level().getGameTime();
		Component name = duelist.getDisplayName().copy().withColor(0xFF000000 | duelist.method().color());
		if (duelist.leaving()) {
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.farewell", name).withColor(0xC8B89A));
			return;
		}
		if (duelist.inDuel()) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.duelist.busy", name).withColor(0xA89CC8));
			return;
		}
		if (inDuel(player) || Duels.inDuel(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.duel_busy").withColor(0xA89CC8));
			return;
		}
		if (!Aura.enabled(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.disabled").withColor(0xA89CC8));
			return;
		}
		if (now < duelist.restUntil) {
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.resting", name).withColor(0xC8B89A));
			return;
		}
		if (!Aura.holdsWeapon(player)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.no_blade", name).withColor(0xC8B89A));
			return;
		}
		Offer offer = OFFERS.get(player.getUUID());
		if (offer == null || !offer.duelist().equals(duelist.getUUID()) || now > offer.until()) {
			OFFERS.put(player.getUUID(), new Offer(duelist.getUUID(), now + AuraWorldRules.CHALLENGE_TICKS));
			int stage = AuraWorldRules.duelStage(Aura.stage(player), AuraStages.highest());
			BreathingMethod method = duelist.method();
			Component methodName = Component.translatable(method.nameKey()).withColor(0xFF000000 | method.color());
			Component stageName = Component.translatable("aura.wildercord.stage." + AuraStages.id(stage)).withColor(0xFF000000 | method.color(stage));
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.challenge", name, methodName).withColor(0xE8D8B0));
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.challenge_how", stageName).withColor(0xB8A8D8));
			duelist.faceTarget(player);
			Fx.sound(player.level(), duelist.position(), SoundEvents.BELL_BLOCK, 0.5F, 1.6F);
			return;
		}
		OFFERS.remove(player.getUUID());
		start(player, duelist);
	}

	/** Starts a duel between a player and a duelist standing near each other, counting down. */
	public static void start(ServerPlayer player, Duelist duelist) {
		ServerLevel level = player.level();
		int stage = AuraWorldRules.duelStage(Aura.stage(player), AuraStages.highest());
		Vec3 centre = player.position().add(duelist.position()).scale(0.5);
		Active active = new Active(new DuelRules.Duel(player.getUUID(), duelist.getUUID(), level.getGameTime()), level, centre, player, duelist, stage);
		BY_PLAYER.put(active.player, active);
		BY_DUELIST.put(active.duelist, active);
		duelist.begin(player, stage);
		Component name = duelist.getDisplayName().copy().withColor(0xFF000000 | duelist.method().color());
		player.sendSystemMessage(Component.translatable("message.wildercord.duelist.begins", name).withStyle(ChatFormatting.GOLD));
	}

	// ------------------------------------------------------------------ the fight

	private static void tick(MinecraftServer server) {
		if (!OFFERS.isEmpty() && server.getTickCount() % 40 == 0) {
			long now = server.overworld().getGameTime();
			OFFERS.values().removeIf(o -> now > o.until() + 200);
		}
		if (BY_PLAYER.isEmpty()) {
			return;
		}
		for (Active active : List.copyOf(BY_PLAYER.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(active.player);
			Entity entity = active.level.getEntity(active.duelist);
			if (player == null) {
				active.duel.forfeit(active.player, DuelRules.Ending.LOGGED_OFF);
				finish(active, server);
				continue;
			}
			if (!(entity instanceof Duelist duelist) || !duelist.isAlive()) {
				active.duel.interrupt();
				finish(active, server);
				continue;
			}
			long now = active.level.getGameTime();
			if (server.getTickCount() % 10 == 0 && (player.level() != active.level
					|| DuelRules.outside(active.centre.x, active.centre.z, player.getX(), player.getZ()))) {
				active.duel.forfeit(active.player, DuelRules.Ending.LEFT_AREA);
				finish(active, server);
				continue;
			}
			if (active.duel.phase() == DuelRules.Phase.COUNTDOWN) {
				int second = active.duel.countdown(now);
				if (second != active.shown && second > 0) {
					active.shown = second;
					title(player, Component.literal(Integer.toString(second)).withColor(GOLD), null);
					circle(active.level, player.position());
					circle(active.level, duelist.position());
					Fx.sound(active.level, player.position(), SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F, 1.0F);
				}
			}
			if (active.duel.tick(now)) {
				if (active.duel.fighting()) {
					title(player, Component.translatable("message.wildercord.duel_fight").withColor(GOLD).withStyle(ChatFormatting.BOLD), null);
					dev.wildercord.aura.AuraFx.groundScar(active.level, player.position(), 4.0, 60, 1);
					dev.wildercord.aura.AuraFx.groundScar(active.level, duelist.position(), 4.0, 60, 1);
					Fx.sound(active.level, active.centre, SoundEvents.BELL_BLOCK, 1.2F, 1.0F);
				} else {
					finish(active, server);
				}
			}
		}
	}

	/** A circle of light round each side while the countdown runs. */
	private static void circle(ServerLevel level, Vec3 at) {
		dev.wildercord.aura.AuraFx.groundScar(level, at, 2.0, 80, 0);
		dev.wildercord.aura.AuraFx.groundScar(level, at, 2.2, 80, 1);
	}

	/** The duelist laid harmful effects on its challenger (its perfect guard's stagger): the end takes them off again. */
	@SafeVarargs
	static void harmed(Duelist duelist, Entity victim, Holder<MobEffect>... effects) {
		Active active = BY_DUELIST.get(duelist.getUUID());
		if (active != null && victim.getUUID().equals(active.player)) {
			active.harms.addAll(List.of(effects));
		}
	}

	/** The duelist yields: the challenger wins. */
	static void yielded(Duelist duelist) {
		Active active = BY_DUELIST.get(duelist.getUUID());
		if (active == null || !active.duel.fighting()) {
			return;
		}
		duelist.kneel();
		active.duel.knockout(active.duelist);
		finish(active, active.level.getServer());
	}

	/** A duelist went (for a duel, as if it were interrupted). */
	static void gone(Duelist duelist) {
		Active active = BY_DUELIST.get(duelist.getUUID());
		if (active != null) {
			active.duel.interrupt();
			finish(active, active.level.getServer());
		}
	}

	private static void finish(Active active, MinecraftServer server) {
		finish(active, server, null);
	}

	/** Ends a duel: the challenger put back as they began, the duelist sheathed, and the lesson for a victor. */
	private static void finish(Active active, MinecraftServer server, ServerPlayer leaving) {
		if (BY_PLAYER.remove(active.player) == null) {
			return;
		}
		BY_DUELIST.remove(active.duelist);
		ServerPlayer player = leaving != null ? leaving : server.getPlayerList().getPlayer(active.player);
		Duelist duelist = active.level.getEntity(active.duelist) instanceof Duelist d ? d : null;
		boolean won = active.player.equals(active.duel.winner()) && active.duel.ending() == DuelRules.Ending.KNOCKOUT;
		if (player != null && player.isAlive()) {
			restore(player, active);
		}
		if (duelist != null) {
			duelist.end(won);
		}
		if (player == null || player == leaving) {
			return;
		}
		Component name = duelist == null ? Component.translatable("entity.wildercord.duelist")
			: duelist.getDisplayName().copy().withColor(0xFF000000 | duelist.method().color());
		DuelRules.Ending ending = active.duel.ending();
		if (won && duelist != null) {
			teach(player, duelist, active);
		} else if (ending == DuelRules.Ending.KNOCKOUT) {
			title(player, Component.translatable("message.wildercord.duel_defeat").withStyle(ChatFormatting.GRAY), name);
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.lost", name).withColor(0xC8B89A));
		} else if (ending == DuelRules.Ending.INTERRUPTED || ending == DuelRules.Ending.DRAW) {
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.called_off", name).withColor(0xC8B89A));
		} else {
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.forfeit", name).withColor(0xC8B89A));
		}
		Fx.sound(active.level, active.centre, SoundEvents.PLAYER_LEVELUP, 0.8F, won ? 1.0F : 0.7F);
	}

	/** The victor's lesson: the duelist's method (or its manual), a little experience, the Grimoire, and maybe the trial. */
	private static void teach(ServerPlayer player, Duelist duelist, Active active) {
		BreathingMethod method = duelist.method();
		Component methodName = Component.translatable(method.nameKey()).withColor(0xFF000000 | method.color());
		Component name = duelist.getDisplayName().copy().withColor(0xFF000000 | method.color());
		title(player, Component.translatable("message.wildercord.duel_victory").withColor(GOLD).withStyle(ChatFormatting.BOLD), name);
		player.sendSystemMessage(Component.translatable("message.wildercord.duelist.won", name, methodName).withColor(0xE8D8B0));
		String mine = Aura.data(player).method();
		if (!Aura.data(player).learned()) {
			AuraApi.grantMethod(player, method.id(), SOURCE);
		} else if (!mine.equals(method.id())) {
			// Already breathing another way: the choice to switch stays theirs.
			ItemStack manual = AuraApi.manual(method.id());
			if (!player.getInventory().add(manual)) {
				player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), manual));
			}
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.manual", methodName).withColor(0xB8A8D8));
		}
		AuraExperience.grant(player, AuraWorldRules.duelXp(active.stage));
		Grimoire.unlock(player, "aura:duelist");
		// And a part of a technique of their own: one the challenger doesn't know yet, its method's favourites likelier.
		dev.wildercord.aura.Techniques.duelistLesson(player, method, name);
		// A bonded blade in hand remembers the duel won.
		dev.wildercord.aura.BondedBlades.dueled(player, method.id());
		if (active.magic) {
			player.sendSystemMessage(Component.translatable("message.wildercord.duelist.magic", name).withColor(0xC8A0A0));
		} else {
			AuraApi.completeTrial(player, TRIAL);
		}
	}

	/** The challenger put back: the health the duelist took, and none of the harm it left. */
	private static void restore(ServerPlayer player, Active active) {
		player.setHealth(DuelRules.restored(player.getHealth(), active.healthBefore, player.getMaxHealth(), active.taken));
		for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
			if (DuelRules.undone(effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL, active.hadBefore.contains(effect.getEffect()),
					active.harms.contains(effect.getEffect()))) {
				player.removeEffect(effect.getEffect());
			}
		}
	}

	/** The player behind some harm: the attacker, a projectile's shooter, a pet's owner. */
	private static Player playerBehind(DamageSource source) {
		Entity entity = source.getEntity();
		if (entity instanceof Player player) {
			return player;
		}
		if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player owner) {
			return owner;
		}
		return null;
	}

	private static void title(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 8));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}
}
