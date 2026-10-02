package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Awakening at runtime (the rules and numbers are {@link AwakeningRules}): a swordsman's awakening, kept on the server, saved (game
 * time carries over a restart, so leaving never cuts a rest short) and synced to everyone near ({@link #AWAKENING}), since everyone
 * draws an awakened body's aura and a spent one's embers. Its phases follow from the times written, on both sides:
 * <ul>
 * <li>{@link #awaken}, the Aura key's fifth way (a tap, then a held press): checked here (stage, a full pool, momentum, no rest left,
 *     a blade in hand), then the swordsman is awakened: momentum held at its peak ({@link Momentum#hold}), the Final Art open,
 *     arts priced at a share ({@link #priceShare}), coated blows harder ({@link #damage}), faster on foot and with the blade, and the
 *     transformation played ({@link AwakeningFx});</li>
 * <li>each finisher landed while it lasts feeds it a second ({@link AwakeningRules#extend});</li>
 * <li>when it runs out ({@link #tick}) the rest of the pool burns away and the swordsman is spent: slowed, gathering no aura at all
 *     ({@link #spent}, read by {@code Aura.gain} and {@code Aura.giveBack}), momentum emptied; the next waits its rest.</li>
 * </ul>
 * A death ends it (the new body isn't spent, the rest still runs); a change of world or a return to the game keeps it, momentum held
 * again for what's left.
 */
public final class Awakening {
	private Awakening() {}

	// ------------------------------------------------------------------ the state

	/**
	 * A swordsman's awakening, in the server's game time.
	 *
	 * @param phase      where it stands ({@link AwakeningRules.Phase}): none yet, awakened, spent, or resting until the next
	 * @param since      when the last one began (-1 never)
	 * @param until      when it ends (while awakened)
	 * @param spentUntil when the spent state lets go (once it has ended)
	 * @param readyAt    when the next may come
	 * @param price      what an art costs while it lasts, as a share of its price (the server's setting, written as it began, so both
	 *                   sides price arts alike)
	 * @param extended   the ticks finishers have fed it so far
	 */
	public record State(int phase, long since, long until, long spentUntil, long readyAt, float price, int extended) {
		public static final State NONE = new State(0, -1, 0, 0, 0, 1.0F, 0);

		public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("phase", 0).forGetter(State::phase),
			Codec.LONG.optionalFieldOf("since", -1L).forGetter(State::since),
			Codec.LONG.optionalFieldOf("until", 0L).forGetter(State::until),
			Codec.LONG.optionalFieldOf("spent_until", 0L).forGetter(State::spentUntil),
			Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(State::readyAt),
			Codec.FLOAT.optionalFieldOf("price", 1.0F).forGetter(State::price),
			Codec.INT.optionalFieldOf("extended", 0).forGetter(State::extended)
		).apply(i, State::new));

		public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::phase, ByteBufCodecs.VAR_LONG, State::since, ByteBufCodecs.VAR_LONG, State::until,
			ByteBufCodecs.VAR_LONG, State::spentUntil, ByteBufCodecs.VAR_LONG, State::readyAt, ByteBufCodecs.FLOAT, State::price,
			ByteBufCodecs.VAR_INT, State::extended, State::new);

		public AwakeningRules.Phase stage() {
			return AwakeningRules.Phase.of(phase);
		}

		/** Whether it's awakened at {@code now}. */
		public boolean awakened(long now) {
			return AwakeningRules.awakened(phase, until, now);
		}

		/** Whether its swordsman is spent at {@code now}. */
		public boolean spent(long now) {
			return AwakeningRules.spent(phase, spentUntil, now);
		}

		/** Whether the next must still wait at {@code now}. */
		public boolean resting(long now) {
			return now < readyAt;
		}
	}

	/** Each swordsman's awakening: saved, kept through death (the rest still runs), synced to everyone near. */
	public static final AttachmentType<State> AWAKENING = AttachmentRegistry.create(
		Wildercord.id("aura_awakening"),
		builder -> builder
			.initializer(() -> State.NONE)
			.persistent(State.CODEC)
			.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.all())
			.copyOnDeath()
	);

	private static final Identifier SPEED = Wildercord.id("aura_awakened_speed");
	private static final Identifier ATTACK_SPEED = Wildercord.id("aura_awakened_attack");

	// ------------------------------------------------------------------ reading (both sides)

	public static State state(Player player) {
		return player.getAttachedOrElse(AWAKENING, State.NONE);
	}

	/** Whether {@code player} is awakened now. Both sides (everyone near is told). */
	public static boolean awakened(Player player) {
		return player != null && state(player).awakened(player.level().getGameTime());
	}

	/** Whether {@code player} is spent now: slowed, gathering no aura. Both sides. */
	public static boolean spent(Player player) {
		return player != null && state(player).spent(player.level().getGameTime());
	}

	/** Ticks of {@code player}'s awakening left (0 when not awakened). */
	public static long left(Player player) {
		State s = state(player);
		long now = player.level().getGameTime();
		return s.awakened(now) ? s.until() - now : 0;
	}

	/** Ticks since {@code player}'s awakening began, with the partial tick (for the look); negative before any. */
	public static float age(Player player, float time) {
		State s = state(player);
		return s.since() < 0 ? -1 : time - s.since();
	}

	/** Why {@code player} can't awaken now, or null when they can. Both sides: the client asks first (its HUD, the Aura key). */
	public static AwakeningRules.Refusal refusal(Player player) {
		long now = player.level().getGameTime();
		State s = state(player);
		return AwakeningRules.refusal(Config.awakening(player) && Aura.enabled(player), Aura.stage(player), s.awakened(now), s.spent(now), s.resting(now),
			Aura.holdsWeapon(player), Aura.aura(player), Aura.capacity(player), Config.momentum(player), Momentum.value(player),
			Config.awakeningMomentum(player));
	}

	/** Whether {@code player} could awaken now. Both sides. */
	public static boolean ready(Player player) {
		return refusal(player) == null;
	}

	/** What an art costs {@code player} now, as a share of its price: the awakening's share while awakened, else all of it. Both sides. */
	public static double priceShare(Player player) {
		State s = state(player);
		return s.awakened(player.level().getGameTime()) ? Math.max(0, Math.min(1, s.price())) : 1.0;
	}

	/** What {@code player}'s coated blow on {@code target} is multiplied by for awakening (1 when not awakened). */
	public static double damage(Player player, LivingEntity target) {
		if (!awakened(player)) {
			return 1.0;
		}
		return AwakeningRules.damage(Config.get().aura().awakening().awakeningDamage(), target instanceof Player);
	}

	// ------------------------------------------------------------------ awakening (server)

	private static void write(ServerPlayer player, State state) {
		if (!state.equals(state(player))) {
			player.setAttached(AWAKENING, state);
		}
	}

	/** The technique (the Aura key tapped, then held): awakens {@code player}, or says why not. */
	public static boolean awaken(ServerPlayer player) {
		AwakeningRules.Refusal why = refusal(player);
		if (why != null) {
			refuse(player, why);
			return false;
		}
		begin(player);
		return true;
	}

	/** Says why an awakening was refused, above the hotbar. */
	private static void refuse(ServerPlayer player, AwakeningRules.Refusal why) {
		long now = player.level().getGameTime();
		State s = state(player);
		Component line = switch (why) {
			case NO_WEAPON -> Component.translatable("message.wildercord.aura.no_weapon");
			case SPENT -> Component.translatable(why.key(), (s.spentUntil() - now + 19) / 20);
			case RESTING -> Component.translatable(why.key(), (s.readyAt() - now + 19) / 20);
			case POOL -> Component.translatable(why.key(), (int) Aura.aura(player), Aura.capacity(player));
			case MOMENTUM -> Component.translatable(why.key(), (int) Math.round(Config.awakeningMomentum(player)), (int) Math.floor(Momentum.value(player)));
			default -> Component.translatable(why.key());
		};
		player.sendOverlayMessage(line.copy().withColor(0xA89CC8));
	}

	/** Awakens {@code player} (already checked). */
	static void begin(ServerPlayer player) {
		long now = player.level().getGameTime();
		WildercordConfig.AuraAwakening settings = Config.get().aura().awakening();
		int stage = Aura.stage(player);
		int ticks = AwakeningRules.ticks(stage, settings.awakeningDuration());
		long until = now + ticks;
		write(player, new State(AwakeningRules.Phase.AWAKENED.ordinal(), now, until, 0, until + settings.cooldownTicks(),
			(float) settings.awakeningArtPrice(), 0));
		// Momentum holds at its peak for the whole of it: arts at their strongest, stance worn fastest, the Final Art open.
		Momentum.hold(player, AwakeningRules.HOLD, ticks);
		modifiers(player, true);
		AwakeningFx.transform(player, ticks);
		Grimoire.unlock(player, "aura:awakening");
		for (AuraApi.AwakeningHook hook : AuraApi.awakeningHooks()) {
			try {
				hook.awakened(player, ticks);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("An awakening hook threw; skipping it", e);
			}
		}
	}

	/** Ends {@code player}'s awakening now, if they're awakened: it runs out on the next tick as if its time had come (spent after). */
	public static void endNow(ServerPlayer player) {
		long now = player.level().getGameTime();
		State s = state(player);
		if (s.awakened(now)) {
			write(player, new State(s.phase(), s.since(), now, s.spentUntil(), Math.min(s.readyAt(), now + Config.get().aura().awakening().cooldownTicks()),
				s.price(), s.extended()));
		}
	}

	/** A finisher landed by {@code player} while awakened: the awakening is fed a moment more (up to its limit). */
	static void fed(ServerPlayer player) {
		long now = player.level().getGameTime();
		State s = state(player);
		if (!s.awakened(now)) {
			return;
		}
		int more = AwakeningRules.extend(s.extended());
		if (more <= 0) {
			return;
		}
		State next = new State(s.phase(), s.since(), s.until() + more, s.spentUntil(), s.readyAt() + more, s.price(), s.extended() + more);
		write(player, next);
		Momentum.hold(player, AwakeningRules.HOLD, (int) (next.until() - now));
		AwakeningFx.fed(player);
	}

	/** Every tick, for every swordsman: an awakening that ran out ends, a spent state lets go, and its speed follows it. */
	static void tick(ServerPlayer player, long now) {
		State s = state(player);
		AwakeningRules.Phase phase = s.stage();
		if (phase == AwakeningRules.Phase.NONE) {
			return;
		}
		if (phase == AwakeningRules.Phase.AWAKENED) {
			if (now >= s.until()) {
				end(player, s, now);
				return;
			}
			if ((now + player.getId()) % 20 == 0) {
				modifiers(player, true);
			}
			AwakeningFx.burning(player, now - s.since(), s.until() - now);
			return;
		}
		if (phase == AwakeningRules.Phase.SPENT) {
			if (now >= s.spentUntil()) {
				write(player, new State(AwakeningRules.Phase.RESTING.ordinal(), s.since(), s.until(), s.spentUntil(), s.readyAt(), s.price(), s.extended()));
				AwakeningFx.recovered(player);
				return;
			}
			// The slow holds the whole spent time (milk doesn't wash a spent body clean).
			if ((now + player.getId()) % 20 == 0 && !player.hasEffect(MobEffects.SLOWNESS)) {
				slow(player, (int) (s.spentUntil() - now));
			}
		}
	}

	/** The awakening runs out: what aura is left burns away, momentum empties, and the swordsman is spent. */
	private static void end(ServerPlayer player, State s, long now) {
		WildercordConfig.AuraAwakening settings = Config.get().aura().awakening();
		// The spent time runs from when it ran out (a swordsman who left mid-awakening comes back to what's left of it).
		AwakeningRules.Ending ending = AwakeningRules.ended(s.until(), s.readyAt(), settings.spentTicks(), settings.cooldownTicks(), now);
		boolean still = ending.phase() == AwakeningRules.Phase.SPENT;
		long spentUntil = ending.spentUntil();
		write(player, new State(ending.phase().ordinal(), s.since(), s.until(), spentUntil, ending.readyAt(), s.price(), s.extended()));
		modifiers(player, false);
		AuraAttachments.Data data = Aura.data(player);
		if (data.aura() > 0) {
			Aura.set(player, data.withAura(0));
		}
		Momentum.hold(player, 0, 0);
		Momentum.reset(player);
		if (still) {
			slow(player, (int) (spentUntil - now));
			AwakeningFx.spent(player);
		}
		for (AuraApi.AwakeningHook hook : AuraApi.awakeningHooks()) {
			try {
				hook.ended(player);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("An awakening hook threw; skipping it", e);
			}
		}
	}

	private static void slow(ServerPlayer player, int ticks) {
		if (ticks > 0) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, AwakeningRules.SPENT_SLOW, false, true, true));
		}
	}

	/** An awakened swordsman's speed, on foot and with the blade (or none). */
	private static void modifiers(ServerPlayer player, boolean on) {
		double speed = on ? Config.get().aura().awakening().awakeningSpeed() : 0;
		modifier(player, Attributes.MOVEMENT_SPEED, SPEED, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED, speed * AwakeningRules.ATTACK_SPEED / AwakeningRules.SPEED,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(id);
		if (amount <= 0) {
			if (current != null) {
				instance.removeModifier(id);
			}
			return;
		}
		if (current == null || Math.abs(current.amount() - amount) > 1.0E-6) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	/** Momentum held again for what's left of an awakening (a new world or a return to the game empties it). */
	private static void holdAgain(ServerPlayer player) {
		long left = left(player);
		if (left > 0) {
			Momentum.hold(player, AwakeningRules.HOLD, (int) left);
			modifiers(player, true);
		}
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		// The Final Art opens while awakened (momentum's floor keeps it at the peak anyway; this holds where a hit took the meter down
		// the moment it was read, or where the server's momentum is off).
		AuraApi.openFinalArt(Awakening::awakened);
		AuraApi.onFinisher(new AuraApi.FinisherHook() {
			@Override
			public void landed(ServerPlayer attacker, LivingEntity target, AuraApi.Finisher finisher, float dealt) {
				fed(attacker);
			}
		});
		// A death ends it: the new body isn't spent (it starts empty anyway), and the rest runs from now.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (alive) {
				return;
			}
			long now = newPlayer.level().getGameTime();
			State s = state(newPlayer);
			if (s.stage() == AwakeningRules.Phase.AWAKENED || s.stage() == AwakeningRules.Phase.SPENT) {
				long readyAt = s.stage() == AwakeningRules.Phase.AWAKENED && now < s.until()
					? Math.min(s.readyAt(), now + Config.get().aura().awakening().cooldownTicks()) : s.readyAt();
				write(newPlayer, new State(AwakeningRules.Phase.RESTING.ordinal(), s.since(), Math.min(s.until(), now), Math.min(s.spentUntil(), now),
					readyAt, s.price(), s.extended()));
			}
		});
		// Momentum empties in a new world and isn't saved: an awakening carried there (or back into the game) holds it again.
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> holdAgain(player));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> holdAgain(handler.player)));
	}
}
