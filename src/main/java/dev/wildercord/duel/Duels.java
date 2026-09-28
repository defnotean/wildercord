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
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Duels: {@code /duel <player>} challenges someone nearby (they accept or decline from chat). On
 * accept, a 3-second countdown in a circle of light; then the two of them can hurt each other (even
 * with PvP or friendly fire off), and neither can harm anyone else. Nobody dies: brought down by the
 * other, the loser is knocked out at 1 health. When it ends, both are put back as they were when it
 * began (their health, mana, effects and fire as they had them, never better), so a duel is never a
 * free heal. Leaving the area, logging off or dying to anything else forfeits; another player
 * striking either duellist calls the duel off (and the blow lands). Nobody hurt in the last few
 * seconds, or fresh from a fight with another player or a duel, can start one. The rules live in
 * {@link DuelRules}.
 */
public final class Duels {
	private Duels() {}

	/** Flip to false to take duels out of the game entirely; the {@code wildercord:allow_duels} game rule turns them off per world. */
	public static final boolean ENABLED = true;

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

	/** How a duellist was when the duel began, to put them back that way when it ends. */
	private record Snapshot(float health, float mana, List<MobEffectInstance> effects, boolean burning) {
		static Snapshot of(ServerPlayer player) {
			List<MobEffectInstance> effects = new ArrayList<>();
			for (MobEffectInstance effect : player.getActiveEffects()) {
				effects.add(new MobEffectInstance(effect));
			}
			return new Snapshot(player.getHealth(), Spellbooks.mana(player), List.copyOf(effects), player.getRemainingFireTicks() > 0);
		}
	}

	/** A duel under way: its rules, where it's fought, the countdown second last shown, and how each duellist began. */
	private static final class Active {
		final DuelRules.Duel duel;
		final ServerLevel level;
		final Vec3 centre;
		final Map<UUID, Snapshot> before = new HashMap<>();
		int shown = -1;

		Active(DuelRules.Duel duel, ServerLevel level, Vec3 centre) {
			this.duel = duel;
			this.level = level;
			this.centre = centre;
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
			if (attacker != null && attacker != victim && !opponents(attacker.getUUID(), victim.getUUID())) {
				LAST_PVP.put(victim.getUUID(), now);
				LAST_PVP.put(attacker.getUUID(), now);
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
			victim.setHealth(1.0F);
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
				// The record is written before the player is saved.
				finish(active, server, player);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Duels::tick);
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
			return target == victim && mine.duel.opponent(player.getUUID()).equals(victim.getUUID()) && mine.duel.fighting() && victim.isAlive();
		}
		return null;
	}

	// ------------------------------------------------------------------ challenges

	private static boolean allowed(ServerLevel level) {
		return ENABLED && level.getGameRules().get(ALLOW_DUELS);
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
		if (inDuel(from) || inDuel(to)) {
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
		CHALLENGES.remove(challenge);
		if (!yes) {
			from.sendSystemMessage(Component.translatable("message.wildercord.duel_declined", to.getDisplayName()).withStyle(ChatFormatting.GRAY));
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_you_declined", from.getDisplayName()).withStyle(ChatFormatting.GRAY));
			return 1;
		}
		if (!allowed(to.level())) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_disabled").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (inDuel(from) || inDuel(to)) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_busy").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (from.level() != to.level() || from.distanceTo(to) > DuelRules.ARENA_RADIUS || !from.isAlive() || !to.isAlive()) {
			to.sendSystemMessage(Component.translatable("message.wildercord.duel_too_far", from.getDisplayName()).withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!ready(to, from) || !ready(to, to)) {
			return 0;
		}
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
		Active active = new Active(new DuelRules.Duel(a.getUUID(), b.getUUID(), level.getGameTime()), level, centre);
		BY_PLAYER.put(a.getUUID(), active);
		BY_PLAYER.put(b.getUUID(), active);
		for (ServerPlayer player : List.of(a, b)) {
			active.before.put(player.getUUID(), Snapshot.of(player));
			player.sendSystemMessage(Component.translatable("message.wildercord.duel_begins", (player == a ? b : a).getDisplayName()).withStyle(ChatFormatting.GOLD));
		}
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
			ServerPlayer a = server.getPlayerList().getPlayer(duel.a);
			ServerPlayer b = server.getPlayerList().getPlayer(duel.b);
			if (a == null || b == null) {
				duel.forfeit(a == null ? duel.a : duel.b, DuelRules.Ending.LOGGED_OFF);
				finish(active, server);
				continue;
			}
			long now = active.level.getGameTime();
			if (server.getTickCount() % 10 == 0) {
				for (ServerPlayer player : List.of(a, b)) {
					if (player.level() != active.level || DuelRules.outside(active.centre.x, active.centre.z, player.getX(), player.getZ())) {
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
				if (second != active.shown && second > 0) {
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
				if (duel.fighting()) {
					for (ServerPlayer player : List.of(a, b)) {
						title(player, Component.translatable("message.wildercord.duel_fight").withColor(GOLD).withStyle(ChatFormatting.BOLD), null);
						Light.groundRing(active.level, player.position(), GOLD, 0.5, 4.0, 0.12, 12);
					}
					Fx.sound(active.level, active.centre, SoundEvents.BELL_BLOCK, 1.2F, 1.0F);
				} else {
					finish(active, server);
				}
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
		ServerPlayer winner = duel.winner() == null ? null : online(server, duel.winner(), leaving);
		ServerPlayer loser = duel.loser() == null ? null : online(server, duel.loser(), leaving);
		long elapsed = active.level.getGameTime() - duel.start;
		for (UUID id : List.of(duel.a, duel.b)) {
			ServerPlayer player = online(server, id, leaving);
			Snapshot before = active.before.get(id);
			if (player != null && player != leaving && player.isAlive() && before != null) {
				restore(player, before, elapsed);
			}
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

	private static ServerPlayer online(MinecraftServer server, UUID id, ServerPlayer leaving) {
		if (leaving != null && leaving.getUUID().equals(id)) {
			return leaving;
		}
		return server.getPlayerList().getPlayer(id);
	}

	/**
	 * Puts a duellist back as they were when the duel began: the health and mana they had (or what
	 * they have now, if that's more), no harm the duel left on them (a harmful effect or fire they
	 * didn't have before), and the effects they had then, less the time the duel took.
	 */
	private static void restore(ServerPlayer player, Snapshot before, long elapsed) {
		player.setHealth(DuelRules.restored(player.getHealth(), before.health(), player.getMaxHealth()));
		Spellbooks.setMana(player, DuelRules.restored(Spellbooks.mana(player), before.mana(), Mana.max(player)));
		if (!before.burning()) {
			player.clearFire();
		}
		for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
			boolean had = before.effects().stream().anyMatch(e -> e.getEffect().equals(effect.getEffect()));
			if (!had && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				player.removeEffect(effect.getEffect());
			}
		}
		for (MobEffectInstance effect : before.effects()) {
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
