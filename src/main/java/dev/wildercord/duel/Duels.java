package dev.wildercord.duel;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.SigilOption;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Duels: {@code /duel <player>} challenges someone nearby (they accept or decline from chat). On
 * accept, a 3-second countdown in a circle of light; then the two of them can hurt each other (even
 * with PvP or friendly fire off), and neither can harm anyone else. Nobody dies: brought down by the
 * other, the loser is knocked out at 1 health. When it ends, both are put back as they were when it
 * began: the health their opponent took is given back (never more than they had, and never what
 * anything else took), the harmful effects and fire their opponent left on them are taken away (noted
 * as they land: see {@link #byOpponent(Active, ServerPlayer)}), and the effects they had come back, so
 * a duel is never a free heal, nor a free cure for what anything else did. Mana spent isn't given back. Leaving the area, logging off or dying to anything else
 * forfeits; another player striking either duellist calls the duel off (and the blow lands). Nobody
 * hurt in the last few seconds, or fresh from a fight with another player or a duel, can start one.
 * The rules live in {@link DuelRules}.
 *
 * <p>A duel can also be fought on other terms ({@link #startBout}): a swordsmen's spar is one, in a small ring that ends at one heart, with
 * a {@link Watcher} of its own that says what harm counts and puts on its own words, light and sound in place of a duel's. Everything else
 * is a duel's: the two harm only each other, anyone else's blow calls it off, nobody dies, and both are put back as they began.</p>
 */
public final class Duels {
	private Duels() {}

	/**
	 * Who looks on a duel fought on other terms (a spar): what harm counts, and its own way of showing it in place of a duel's titles, circles
	 * and chat. Its calls come on the server thread, after the duel's own rules have done their part.
	 */
	public interface Watcher {
		/** Whether harm from {@code source} dealt by {@code attacker} reaches {@code victim} (both duellists, once the fight is on). */
		default boolean counts(Player attacker, ServerPlayer victim, DamageSource source) {
			return true;
		}

		/** Whether the duellists' spells may harm each other (asked while a spell's harm is being applied). */
		default boolean spells() {
			return true;
		}

		/** The countdown's second ({@code second}: 3, 2, 1) has come. */
		default void counting(int second) {}

		/** The fight is on. */
		default void began() {}

		/** Each tick while it lasts (after the duel's own). */
		default void tick(long now) {}

		/**
		 * It's over, and both are already put back as they began: the winner and loser (either null for an ending with neither, or one who has
		 * gone), and how it ended in {@code duel}.
		 */
		void ended(DuelRules.Duel duel, ServerPlayer winner, ServerPlayer loser);
	}

	/** Duels are on unless the server's config switches them off (features.duels); the {@code wildercord:allow_duels} game rule turns them off per world. */
	private static boolean enabled() {
		return dev.wildercord.config.Config.get().duels();
	}

	public static final GameRule<Boolean> ALLOW_DUELS = GameRuleBuilder.forBoolean(true)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(Wildercord.id("allow_duels"));

	/** A player's duelling record. */
	public record Record(int wins, int losses) {
		public static final Record NONE = new Record(0, 0);
		public static final Codec<Record> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("wins", 0).forGetter(Record::wins),
			Codec.INT.optionalFieldOf("losses", 0).forGetter(Record::losses)
		).apply(i, Record::new));
	}

	/** Wins and losses. Saved, kept through death, and sent to its player (for the Grimoire). */
	public static final AttachmentType<Record> RECORD = AttachmentRegistry.create(
		Wildercord.id("duel_record"),
		builder -> builder
			.initializer(() -> Record.NONE)
			.persistent(Record.CODEC)
			.syncWith(net.minecraft.network.codec.StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, Record::wins,
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, Record::losses, Record::new),
				net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/**
	 * How a duellist was when the duel began, to put them back that way when it ends. Mana isn't kept:
	 * giving back mana spent on anything at all would refill it (and condense it again) for free.
	 */
	private record Snapshot(float health, List<MobEffectInstance> effects, boolean burning) {
		static Snapshot of(ServerPlayer player) {
			List<MobEffectInstance> effects = new ArrayList<>();
			for (MobEffectInstance effect : player.getActiveEffects()) {
				effects.add(new MobEffectInstance(effect));
			}
			return new Snapshot(player.getHealth(), List.copyOf(effects), player.getRemainingFireTicks() > 0);
		}
	}

	/** A duel under way: its rules, where it's fought, the countdown second last shown, and how each duellist began. */
	private static final class Active {
		final DuelRules.Duel duel;
		final ServerLevel level;
		final Vec3 centre;
		final Map<UUID, Snapshot> before = new HashMap<>();
		/** The health each duellist lost to the other's blows and spells, to be given back. */
		final Map<UUID, Float> taken = new HashMap<>();
		/**
		 * Each duellist's health as a blow of their opponent's began to land. The damage the game reports
		 * afterwards is before armour, Resistance and absorption, so what the blow really took is measured.
		 */
		final Map<UUID, Float> landing = new HashMap<>();
		/** The harmful effects each duellist's opponent laid on them, and who their opponent set alight: all the end undoes. */
		final Map<UUID, Set<Holder<MobEffect>>> harms = new HashMap<>();
		final Set<UUID> burned = new HashSet<>();
		/** The server tick each duellist was last struck by the other (a blow, a shot, a pet or a spell), and who was burning last tick. */
		final Map<UUID, Long> struck = new HashMap<>();
		final Set<UUID> alight = new HashSet<>();
		/** Who looks on it, for a duel on other terms (null for a duel). */
		final Watcher watcher;
		int shown = -1;

		Active(DuelRules.Duel duel, ServerLevel level, Vec3 centre, Watcher watcher) {
			this.duel = duel;
			this.level = level;
			this.centre = centre;
			this.watcher = watcher;
		}
	}

	private static final Map<UUID, Active> BY_PLAYER = new HashMap<>();
	private static final List<DuelRules.Challenge> CHALLENGES = new ArrayList<>();
	/** Server ticks of each player's last hurt, last fight with another player, last duel's end and last challenge sent. */
	private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
	private static final Map<UUID, Long> LAST_PVP = new HashMap<>();
	private static final Map<UUID, Long> LAST_DUEL = new HashMap<>();
	private static final Map<UUID, Long> LAST_CHALLENGE = new HashMap<>();

	private static final int GOLD = 0xFFD870;

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("duel")
				.then(Commands.argument("player", EntityArgument.player()).executes(ctx -> challenge(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"))))
				.then(Commands.literal("accept").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> answer(ctx, true))))
				.then(Commands.literal("decline").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> answer(ctx, false))))
				.then(Commands.literal("stats")
					.executes(ctx -> stats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player()).executes(ctx -> stats(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));

		// The two duellists hurt each other only once the countdown ends. Anyone else striking either calls the duel off, and the blow lands.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (BY_PLAYER.isEmpty()) {
				return true;
			}
			Player attacker = playerBehind(source);
			if (attacker == null) {
				// An outsider's creature or environmental harm also calls a spar off.
				// Ongoing harm attributed to the partner still follows the duel's restoration rules.
				if (entity instanceof ServerPlayer victim && BY_PLAYER.get(victim.getUUID()) instanceof Active active
						&& active.watcher != null && !byOpponent(active, victim, source)) {
					active.duel.interrupt();
					finish(active, victim.level().getServer());
				}
				return true;
			}
			// A duellist harms nobody but their opponent: no other player, and no other player's pet.
			Player owner = entity instanceof Player p ? p : entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player o ? o : null;
			Active mine = BY_PLAYER.get(attacker.getUUID());
			if (mine != null && owner != null && owner != attacker && (entity != owner || !mine.duel.involves(owner.getUUID()))) {
				return false;
			}
			if (!(entity instanceof ServerPlayer victim) || attacker == victim) {
				return true;
			}
			Active active = BY_PLAYER.get(victim.getUUID());
			if (active == null) {
				return true;
			}
			if (active.duel.involves(attacker.getUUID())) {
				if (active.duel.fighting() && active.watcher != null && !active.watcher.counts(attacker, victim, source)) {
					// Fought on other terms (a spar: blades and aura only): this harm doesn't reach them, and calls nothing off.
					return false;
				}
				if (active.duel.fighting()) {
					active.landing.put(victim.getUUID(), victim.getHealth());
				}
				return active.duel.fighting();
			}
			active.duel.interrupt();
			finish(active, victim.level().getServer());
			return true;
		});
		// Who was hurt, and who fought another player, lately: neither may start a duel for a while.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (!(entity instanceof ServerPlayer victim) || damage <= 0) {
				return;
			}
			long now = victim.level().getServer().getTickCount();
			LAST_HURT.put(victim.getUUID(), now);
			Player attacker = playerBehind(source);
			if (attacker != null && BY_PLAYER.get(victim.getUUID()) instanceof Active active && attacker.getUUID().equals(active.duel.opponent(victim.getUUID()))) {
				// What the blow took from their health, not the damage before armour and absorption (that would give back more than was lost).
				Float before = active.landing.remove(victim.getUUID());
				if (before != null) {
					active.taken.merge(victim.getUUID(), Math.max(0F, before - victim.getHealth()), Float::sum);
				}
				active.struck.put(victim.getUUID(), now);
				if (active.duel.fighting() && active.duel.terms.knockedOut(victim.getHealth()) && victim.isAlive()) {
					// Brought to the health that ends it (a spar's one heart): held there, and the duel is over.
					if (victim.getHealth() < active.duel.terms.leftOn()) {
						active.taken.merge(victim.getUUID(), -Math.max(0F, active.duel.terms.leftOn() - victim.getHealth()), Float::sum);
						victim.setHealth(active.duel.terms.leftOn());
					}
					active.duel.knockout(victim.getUUID());
					finish(active, victim.level().getServer());
				}
			}
			if (attacker != null && attacker != victim && !opponents(attacker.getUUID(), victim.getUUID())) {
				LAST_PVP.put(victim.getUUID(), now);
				LAST_PVP.put(attacker.getUUID(), now);
			}
		});
		// A harmful effect the opponent lays on a duellist is noted as it lands, so the end takes off that and nothing else.
		ServerMobEffectEvents.AFTER_ADD.register((effect, entity, context) -> {
			if (!BY_PLAYER.isEmpty() && entity instanceof ServerPlayer victim && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
					&& BY_PLAYER.get(victim.getUUID()) instanceof Active active && byOpponent(active, victim)) {
				active.harms.computeIfAbsent(victim.getUUID(), k -> new HashSet<>()).add(effect.getEffect());
			}
		});
		// Brought down by the other duellist: knocked out, not killed.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer victim)) {
				return true;
			}
			Active active = BY_PLAYER.get(victim.getUUID());
			if (active == null || !active.duel.fighting() || !byOpponent(active, victim, source)) {
				return true;
			}
			float left = active.duel.terms.leftOn();
			victim.setHealth(left);
			// The last blow is given back with the rest (the duel ends before it's counted after the damage): all the health
			// it found, less what they're left with. Its damage before armour would give back more than the blow took.
			Float before = active.landing.remove(victim.getUUID());
			active.taken.merge(victim.getUUID(), Math.max(0F, (before != null ? before : amount) - left), Float::sum);
			active.duel.knockout(victim.getUUID());
			finish(active, victim.level().getServer());
			return false;
		});
		// Dying to anything else forfeits.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer victim && BY_PLAYER.get(victim.getUUID()) instanceof Active active) {
				active.duel.forfeit(victim.getUUID(), DuelRules.Ending.DIED);
				finish(active, victim.level().getServer());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;
			CHALLENGES.removeIf(c -> c.from().equals(player.getUUID()) || c.to().equals(player.getUUID()));
			if (BY_PLAYER.get(player.getUUID()) instanceof Active active) {
				active.duel.forfeit(player.getUUID(), DuelRules.Ending.LOGGED_OFF);
				// The record is written, and they're put back as they began, before the player is saved.
				finish(active, server, player);
			}
		});
		// The server stopping logs everyone off one by one: the duels end first, for nobody, and both duellists are put back.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Active active : List.copyOf(new java.util.LinkedHashSet<>(BY_PLAYER.values()))) {
				BY_PLAYER.remove(active.duel.a, active);
				BY_PLAYER.remove(active.duel.b, active);
				long elapsed = active.level.getGameTime() - active.duel.start;
				for (UUID id : List.of(active.duel.a, active.duel.b)) {
					ServerPlayer player = find(server, active, id);
					if (player != null && player.isAlive() && active.before.get(id) != null) {
						restore(player, active, id, elapsed);
					}
				}
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Duels::tick);
		// A leaver's own rests stay (a relog never skips them); everyone's that have run go once the maps grow.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			long now = server.overworld().getGameTime();
			dev.wildercord.spell.StatePrune.rested(LAST_HURT, now, DuelRules.HURT_TICKS);
			dev.wildercord.spell.StatePrune.rested(LAST_PVP, now, DuelRules.PVP_TICKS);
			dev.wildercord.spell.StatePrune.rested(LAST_DUEL, now, DuelRules.DUEL_COOLDOWN_TICKS);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BY_PLAYER.clear();
			CHALLENGES.clear();
			LAST_HURT.clear();
			LAST_PVP.clear();
			LAST_DUEL.clear();
			LAST_CHALLENGE.clear();
		});
	}

	// ------------------------------------------------------------------ for the rest of the mod

	/** Whether these two are duelling each other right now (countdown included). */
	public static boolean opponents(UUID a, UUID b) {
		Active active = BY_PLAYER.get(a);
		return active != null && b.equals(active.duel.opponent(a));
	}

	public static boolean inDuel(Player player) {
		return BY_PLAYER.containsKey(player.getUUID());
	}

	/** Whether {@code player} is in a duel fought on other terms whose watcher is of {@code kind} (a spar's), countdown included. */
	public static boolean watchedBy(Player player, Class<? extends Watcher> kind) {
		Active active = BY_PLAYER.get(player.getUUID());
		return active != null && active.watcher != null && kind.isInstance(active.watcher);
	}

	/** The duel {@code player} is in (its rules: phase, terms, opponent, start), or null. */
	public static DuelRules.Duel duelOf(Player player) {
		Active active = BY_PLAYER.get(player.getUUID());
		return active == null ? null : active.duel;
	}

	/** Where {@code player}'s duel began (the middle of a spar's ring), or null. */
	public static Vec3 centreOf(Player player) {
		Active active = BY_PLAYER.get(player.getUUID());
		return active == null ? null : active.centre;
	}

	/**
	 * Why {@code player} can't start a duel (or a spar) now: hurt too lately, fresh from a fight with another player, or from a duel's end; or
	 * {@link DuelRules.Refusal#NONE}.
	 */
	public static DuelRules.Refusal readiness(ServerPlayer player) {
		long now = player.level().getServer().getTickCount();
		UUID id = player.getUUID();
		return DuelRules.ready(now, LAST_HURT.getOrDefault(id, DuelRules.NEVER), LAST_PVP.getOrDefault(id, DuelRules.NEVER),
			LAST_DUEL.getOrDefault(id, DuelRules.NEVER));
	}

	/** Calls {@code player}'s duel off (for nobody: an operator, a test). */
	public static void callOff(ServerPlayer player) {
		Active active = BY_PLAYER.get(player.getUUID());
		if (active != null) {
			active.duel.interrupt();
			finish(active, player.level().getServer());
		}
	}

	/** Forgets when {@code player} was last hurt, fought another player or ended a duel (an operator, a test): they're ready at once. */
	public static void rested(ServerPlayer player) {
		LAST_HURT.remove(player.getUUID());
		LAST_PVP.remove(player.getUUID());
		LAST_DUEL.remove(player.getUUID());
	}

	/**
	 * Between two players, either way round: whether they may harm each other because they're
	 * duelling each other (only once the fight is on), or null when they aren't and the usual rules
	 * decide. What a duellist may do to anyone else is settled where the harm lands.
	 */
	public static Boolean between(Player one, Player other) {
		if (BY_PLAYER.isEmpty() || one == other) {
			return null;
		}
		Active active = BY_PLAYER.get(one.getUUID());
		return active != null && other.getUUID().equals(active.duel.opponent(one.getUUID())) ? active.duel.fighting() : null;
	}

	/**
	 * For spells and blows: whether {@code caster} may harm {@code target} because of a duel. The two
	 * duellists may harm each other once the fight is on (not in the countdown); a duellist may harm
	 * no other player (nor another player's pet) while it lasts. Null when no duel decides it and the
	 * usual rules do: someone else harming a duellist is left to them, and calls the duel off when
	 * the blow lands.
	 */
	public static Boolean canHarm(LivingEntity caster, Entity target) {
		if (BY_PLAYER.isEmpty() || caster == target || !(caster instanceof Player player)) {
			return null;
		}
		Player victim = target instanceof Player p ? p
			: target instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player owner ? owner : null;
		if (victim == null || victim == player) {
			return null;
		}
		Active mine = BY_PLAYER.get(player.getUUID());
		if (mine != null) {
			if (mine.watcher != null && !mine.watcher.spells() && dev.wildercord.cast.Effects.applyingCast() != null) {
				// On terms that keep spells out (a spar): a spell harms nobody, the opponent included.
				return false;
			}
			return target == victim && mine.duel.opponent(player.getUUID()).equals(victim.getUUID()) && mine.duel.fighting() && victim.isAlive();
		}
		return null;
	}

	// ------------------------------------------------------------------ challenges

	private static boolean allowed(ServerLevel level) {
		return enabled() && level.getGameRules().get(ALLOW_DUELS);
	}

	private static int challenge(ServerPlayer from, ServerPlayer to) {
		if (!allowed(from.level())) {
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_disabled").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (from == to) {
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_self").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (inDuel(from) || inDuel(to) || dev.wildercord.aura.world.DuelistDuels.inDuel(from) || dev.wildercord.aura.world.DuelistDuels.inDuel(to)) {
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_busy").withStyle(ChatFormatting.RED));
			return 0;
		}
		// Only someone close by, and not over and over.
		if (from.level() != to.level() || from.distanceTo(to) > DuelRules.ARENA_RADIUS) {
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_too_far", to.getDisplayName()).withStyle(ChatFormatting.RED));
			return 0;
		}
		long tick = from.level().getServer().getTickCount();
		if (DuelRules.within(tick, LAST_CHALLENGE.getOrDefault(from.getUUID(), DuelRules.NEVER), DuelRules.CHALLENGE_COOLDOWN_TICKS)) {
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_wait").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!ready(from, from) || !ready(from, to)) {
			return 0;
		}
		LAST_CHALLENGE.put(from.getUUID(), tick);
		long now = from.level().getGameTime();
		boolean renewed = CHALLENGES.removeIf(c -> c.from().equals(from.getUUID()) && c.to().equals(to.getUUID()));
		CHALLENGES.add(new DuelRules.Challenge(from.getUUID(), to.getUUID(), now));
		String name = from.getGameProfile().name();
		MutableComponent accept = Component.translatable("message.wildercord.duel_accept").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
			.withClickEvent(new ClickEvent.RunCommand("/duel accept " + name))
			.withHoverEvent(new HoverEvent.ShowText(Component.translatable("message.wildercord.duel_accept_hover", from.getDisplayName()))));
		MutableComponent decline = Component.translatable("message.wildercord.duel_decline").withStyle(style -> style.withColor(ChatFormatting.RED).withBold(true)
			.withClickEvent(new ClickEvent.RunCommand("/duel decline " + name))
			.withHoverEvent(new HoverEvent.ShowText(Component.translatable("message.wildercord.duel_decline_hover"))));
		to.sendSystemMessage(Component.translatable("message.wildercord.duel_challenged", from.getDisplayName()).withStyle(ChatFormatting.GOLD)
			.append(Component.literal("  ")).append(accept).append(Component.literal(" ")).append(decline));
		if (!renewed) {
			// Only the first time: a renewed challenge doesn't ring again.
			to.level().playSound(null, to.blockPosition(), SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.4F);
		}
		from.sendSystemMessage(Component.translatable("message.wildercord.duel_sent", to.getDisplayName()).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int answer(CommandContext<CommandSourceStack> ctx, boolean yes) throws CommandSyntaxException {
		ServerPlayer to = ctx.getSource().getPlayerOrException();
		ServerPlayer from = EntityArgument.getPlayer(ctx, "player");
		long now = to.level().getGameTime();
		CHALLENGES.removeIf(c -> c.expired(now));
		DuelRules.Challenge challenge = CHALLENGES.stream().filter(c -> c.from().equals(from.getUUID()) && c.to().equals(to.getUUID())).findFirst().orElse(null);
		if (challenge == null) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_no_challenge", from.getDisplayName()).withStyle(ChatFormatting.RED));
			return 0;
		}
		// An accept that can't go ahead yet (too far, hurt too lately...) leaves the challenge standing, to accept again.
		if (!yes) {
			CHALLENGES.remove(challenge);
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_declined", to.getDisplayName()).withStyle(ChatFormatting.GRAY));
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_you_declined", from.getDisplayName()).withStyle(ChatFormatting.GRAY));
			return 1;
		}
		if (!allowed(to.level())) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_disabled").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (inDuel(from) || inDuel(to) || dev.wildercord.aura.world.DuelistDuels.inDuel(from) || dev.wildercord.aura.world.DuelistDuels.inDuel(to)) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_busy").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!from.isAlive() || !to.isAlive()) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_not_alive", (from.isAlive() ? to : from).getDisplayName()).withStyle(ChatFormatting.RED));
			return 0;
		}
		if (from.level() != to.level() || from.distanceTo(to) > DuelRules.ARENA_RADIUS) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_too_far", from.getDisplayName()).withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!ready(to, from) || !ready(to, to)) {
			return 0;
		}
		CHALLENGES.remove(challenge);
		start(from, to);
		return 1;
	}

	/** Whether {@code player} may start a duel now; if not, {@code told} hears why. */
	private static boolean ready(ServerPlayer told, ServerPlayer player) {
		long now = player.level().getServer().getTickCount();
		UUID id = player.getUUID();
		DuelRules.Refusal refusal = DuelRules.ready(now, LAST_HURT.getOrDefault(id, DuelRules.NEVER), LAST_PVP.getOrDefault(id, DuelRules.NEVER),
			LAST_DUEL.getOrDefault(id, DuelRules.NEVER));
		if (refusal == DuelRules.Refusal.NONE) {
			return true;
		}
		told.sendSystemMessage(Component.translatable("message.wildercord.duel_" + refusal.name().toLowerCase(java.util.Locale.ROOT), player.getDisplayName())
			.withStyle(ChatFormatting.RED));
		return false;
	}

	/**
	 * Starts a duel between two players standing in the same world, counting down. Nothing about
	 * them changes: how they are is noted, to put them back that way when it ends.
	 */
	public static void start(ServerPlayer a, ServerPlayer b) {
		ServerLevel level = a.level();
		Vec3 centre = a.position().add(b.position()).scale(0.5);
		Active active = new Active(new DuelRules.Duel(a.getUUID(), b.getUUID(), level.getGameTime()), level, centre, null);
		BY_PLAYER.put(a.getUUID(), active);
		BY_PLAYER.put(b.getUUID(), active);
		for (ServerPlayer player : List.of(a, b)) {
			active.before.put(player.getUUID(), Snapshot.of(player));
			player.sendSystemMessage(Component.translatable("message.wildercord.duel_begins", (player == a ? b : a).getDisplayName()).withStyle(ChatFormatting.GOLD));
		}
	}

	/**
	 * Starts a duel on other {@code terms} between two players standing in the same world (neither in a duel already), round {@code centre},
	 * shown by {@code watcher} (a spar): counting down, nothing about them changed, how they are noted to put them back that way at the end.
	 * Returns its rules.
	 */
	public static DuelRules.Duel startBout(ServerPlayer a, ServerPlayer b, DuelRules.Terms terms, Vec3 centre, Watcher watcher) {
		ServerLevel level = a.level();
		Active active = new Active(new DuelRules.Duel(a.getUUID(), b.getUUID(), level.getGameTime(), terms), level, centre, watcher);
		BY_PLAYER.put(a.getUUID(), active);
		BY_PLAYER.put(b.getUUID(), active);
		active.before.put(a.getUUID(), Snapshot.of(a));
		active.before.put(b.getUUID(), Snapshot.of(b));
		return active.duel;
	}

	/** A duellist by id: online, or (a stand-in the game tests add to the world) in the duel's own world; null if neither. */
	private static ServerPlayer find(MinecraftServer server, Active active, UUID id) {
		ServerPlayer online = server.getPlayerList().getPlayer(id);
		if (online != null) {
			return online;
		}
		return active.level.getPlayerByUUID(id) instanceof ServerPlayer there && !there.isRemoved() ? there : null;
	}

	// ------------------------------------------------------------------ the fight

	private static void tick(MinecraftServer server) {
		if (!CHALLENGES.isEmpty() && server.getTickCount() % 20 == 0) {
			long now = server.overworld().getGameTime();
			CHALLENGES.removeIf(c -> c.expired(now));
		}
		if (BY_PLAYER.isEmpty()) {
			return;
		}
		for (Active active : List.copyOf(new java.util.LinkedHashSet<>(BY_PLAYER.values()))) {
			DuelRules.Duel duel = active.duel;
			ServerPlayer a = find(server, active, duel.a);
			ServerPlayer b = find(server, active, duel.b);
			if (a == null || b == null) {
				duel.forfeit(a == null ? duel.a : duel.b, DuelRules.Ending.LOGGED_OFF);
				finish(active, server);
				continue;
			}
			long now = active.level.getGameTime();
			// Fire has no name on it: a duellist catching alight just as their opponent struck was set alight by them.
			for (ServerPlayer player : List.of(a, b)) {
				boolean burning = player.getRemainingFireTicks() > 0;
				if (burning && active.alight.add(player.getUUID())
						&& DuelRules.byOpponent(server.getTickCount(), active.struck.getOrDefault(player.getUUID(), DuelRules.NEVER))) {
					active.burned.add(player.getUUID());
				} else if (!burning) {
					active.alight.remove(player.getUUID());
				}
			}
			// A small ring (a spar's) is watched closely; a duel's wide ground every half second.
			if (server.getTickCount() % (active.watcher != null ? 2 : 10) == 0) {
				for (ServerPlayer player : List.of(a, b)) {
					if (player.level() != active.level || duel.terms.outside(active.centre.x, active.centre.z, player.getX(), player.getZ())) {
						duel.forfeit(player.getUUID(), DuelRules.Ending.LEFT_AREA);
						break;
					}
				}
				if (duel.phase() == DuelRules.Phase.OVER) {
					finish(active, server);
					continue;
				}
			}
			if (duel.phase() == DuelRules.Phase.COUNTDOWN) {
				int second = duel.countdown(now);
				if (second != active.shown && second > 0 && active.watcher != null) {
					active.shown = second;
					active.watcher.counting(second);
				} else if (second != active.shown && second > 0) {
					active.shown = second;
					for (ServerPlayer player : List.of(a, b)) {
						title(player, Component.literal(Integer.toString(second)).withColor(GOLD), null);
						circle(active.level, player);
					}
					Fx.sound(active.level, a.position(), SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F, 1.0F);
					Fx.sound(active.level, b.position(), SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F, 1.0F);
				}
			}
			if (duel.tick(now)) {
				if (duel.fighting() && active.watcher != null) {
					active.watcher.began();
				} else if (duel.fighting()) {
					for (ServerPlayer player : List.of(a, b)) {
						title(player, Component.translatable("message.wildercord.duel_fight").withColor(GOLD).withStyle(ChatFormatting.BOLD), null);
						Light.groundRing(active.level, player.position(), GOLD, 0.5, 4.0, 0.12, 12);
					}
					Fx.sound(active.level, active.centre, SoundEvents.BELL_BLOCK, 1.2F, 1.0F);
				} else {
					finish(active, server);
					continue;
				}
			}
			if (active.watcher != null && BY_PLAYER.get(duel.a) == active) {
				active.watcher.tick(now);
			}
		}
	}

	/** A circle of light around a duellist while the countdown runs. */
	private static void circle(ServerLevel level, ServerPlayer player) {
		Vec3 at = player.position();
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, GOLD, 2.4F, 24, 0.1F), at.add(0, 0.06, 0));
		Sigils.send(level, SigilOption.flat(SigilOption.RING, 0xFFF4D0, 2.9F, 24, -0.08F), at.add(0, 0.07, 0));
		Light.groundRing(level, at, GOLD, 2.6, 2.4, 0.1, 22);
	}

	/** Whether the other duellist brought this one down: their blow, or their harm in the last five seconds. */
	private static boolean byOpponent(Active active, ServerPlayer victim, DamageSource source) {
		UUID opponent = active.duel.opponent(victim.getUUID());
		Player attacker = playerBehind(source);
		if (attacker != null) {
			return attacker.getUUID().equals(opponent);
		}
		LivingEntity last = victim.getLastHurtByMob();
		return last != null && last.getUUID().equals(opponent) && victim.tickCount - victim.getLastHurtByMobTimestamp() < 100;
	}

	/**
	 * Whether the harm landing on {@code victim} right now is their opponent's: their spell being applied (a spell
	 * says whose it is), or, for harm with no spell's name on it (an arrow's poison, a splash of harming), their
	 * opponent struck them just now.
	 */
	private static boolean byOpponent(Active active, ServerPlayer victim) {
		UUID opponent = active.duel.opponent(victim.getUUID());
		LivingEntity caster = dev.wildercord.cast.Effects.applying();
		if (caster != null) {
			return caster.getUUID().equals(opponent);
		}
		return DuelRules.byOpponent(victim.level().getServer().getTickCount(), active.struck.getOrDefault(victim.getUUID(), DuelRules.NEVER));
	}

	/** The player behind some damage: the attacker, the shooter of a projectile, or the owner of a pet. */
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

	private static void finish(Active active, MinecraftServer server) {
		finish(active, server, null);
	}

	/** Ends a duel: both put back as they began, the result announced nearby and written into their records. */
	private static void finish(Active active, MinecraftServer server, ServerPlayer leaving) {
		DuelRules.Duel duel = active.duel;
		if (!BY_PLAYER.remove(duel.a, active) & !BY_PLAYER.remove(duel.b, active)) {
			// Already finished (an interruption and a knockout in the same blow).
			return;
		}
		long tick = server.getTickCount();
		LAST_DUEL.put(duel.a, tick);
		LAST_DUEL.put(duel.b, tick);
		ServerPlayer winner = duel.winner() == null ? null : online(server, active, duel.winner(), leaving);
		ServerPlayer loser = duel.loser() == null ? null : online(server, active, duel.loser(), leaving);
		long elapsed = active.level.getGameTime() - duel.start;
		for (UUID id : List.of(duel.a, duel.b)) {
			// The one logging off too: they're saved as they began, not as the duel left them.
			ServerPlayer player = online(server, active, id, leaving);
			if (player != null && player.isAlive() && active.before.get(id) != null) {
				restore(player, active, id, elapsed);
			}
		}
		if (active.watcher != null) {
			// Fought on other terms: its watcher says how it ended, in its own way (and nothing goes into the duel record).
			try {
				active.watcher.ended(duel, winner == leaving ? null : winner, loser == leaving ? null : loser);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A duel's watcher failed as it ended", e);
			}
			return;
		}
		Component result;
		if (duel.ending() == DuelRules.Ending.INTERRUPTED) {
			result = Component.translatable("message.wildercord.duel_interrupted").withStyle(ChatFormatting.GOLD);
		} else if (duel.ending() == DuelRules.Ending.DRAW || winner == null && loser == null) {
			result = Component.translatable("message.wildercord.duel_draw").withStyle(ChatFormatting.GOLD);
		} else {
			Component winnerName = winner != null ? winner.getDisplayName() : Component.literal("?");
			Component loserName = loser != null ? loser.getDisplayName() : Component.literal("?");
			String how = duel.ending().name().toLowerCase(java.util.Locale.ROOT);
			result = Component.translatable("message.wildercord.duel_result." + how, winnerName, loserName).withStyle(ChatFormatting.GOLD);
			if (winner != null) {
				Record record = winner.getAttachedOrElse(RECORD, Record.NONE);
				winner.setAttached(RECORD, new Record(record.wins() + 1, record.losses()));
				if (winner != leaving) {
					title(winner, Component.translatable("message.wildercord.duel_victory").withColor(GOLD).withStyle(ChatFormatting.BOLD), loserName);
				}
			}
			if (loser != null) {
				Record record = loser.getAttachedOrElse(RECORD, Record.NONE);
				loser.setAttached(RECORD, new Record(record.wins(), record.losses() + 1));
				if (loser != leaving) {
					title(loser, Component.translatable("message.wildercord.duel_defeat").withStyle(ChatFormatting.GRAY), winnerName);
				}
			}
		}
		// Everyone nearby hears how it ended.
		for (ServerPlayer player : active.level.players()) {
			if (player != leaving && player.position().distanceTo(active.centre) <= 64) {
				player.sendSystemMessage(result);
			}
		}
		Fx.sound(active.level, active.centre, SoundEvents.PLAYER_LEVELUP, 1.0F, 0.9F);
	}

	private static ServerPlayer online(MinecraftServer server, Active active, UUID id, ServerPlayer leaving) {
		if (leaving != null && leaving.getUUID().equals(id)) {
			return leaving;
		}
		return find(server, active, id);
	}

	/**
	 * Puts a duellist back as they were when the duel began: the health their opponent took from them
	 * (never past what they had then), none of the harm their opponent left on them (a harmful effect or
	 * fire they didn't have before; what anything else did stays), and the helpful effects they had then
	 * (Absorption aside), less the time the duel took.
	 */
	private static void restore(ServerPlayer player, Active active, UUID id, long elapsed) {
		Snapshot before = active.before.get(id);
		Set<Holder<MobEffect>> harms = active.harms.getOrDefault(id, Set.of());
		player.setHealth(DuelRules.restored(player.getHealth(), before.health(), player.getMaxHealth(), active.taken.getOrDefault(id, 0F)));
		if (DuelRules.undone(true, before.burning(), active.burned.contains(id))) {
			player.clearFire();
		}
		for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
			boolean had = before.effects().stream().anyMatch(e -> e.getEffect().equals(effect.getEffect()));
			if (DuelRules.undone(effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL, had, harms.contains(effect.getEffect()))) {
				player.removeEffect(effect.getEffect());
			}
		}
		for (MobEffectInstance effect : before.effects()) {
			// Only helpful ones, and never Absorption: a Bad Omen used up by a raid, or absorption hearts a monster knocked
			// away, would otherwise come back after every duel.
			if (effect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL || effect.is(MobEffects.ABSORPTION)) {
				continue;
			}
			int left = DuelRules.remaining(effect.getDuration(), elapsed);
			if (left != 0 && !player.hasEffect(effect.getEffect())) {
				player.addEffect(new MobEffectInstance(effect.getEffect(), left, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
			}
		}
	}

	private static void title(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 6));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	private static int stats(CommandSourceStack source, ServerPlayer player) {
		Record record = player.getAttachedOrElse(RECORD, Record.NONE);
		source.sendSuccess(() -> Component.translatable("message.wildercord.duel_stats", player.getDisplayName(), record.wins(), record.losses())
			.withStyle(ChatFormatting.GOLD), false);
		return record.wins();
	}
}
