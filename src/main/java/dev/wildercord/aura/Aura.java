package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.net.PacketThrottle;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Aura at runtime: reading a player's aura on either side, filling and spending it (with backlash), the breathing stance,
 * the attribute modifiers each stage and method gives, and the public look. Checked every tick for every player (the stance
 * has to see a quick breath of sneak let up and pressed again); the rest of aura lives beside it: {@link AuraCombat} (blows),
 * {@link AuraGuard}, {@link AuraSlash}, {@link AuraBreakthroughs}, {@link AuraMethods} (learning) and {@link AuraLoot}, and
 * for the top stages {@link AuraStep}, {@link AuraArmour}, {@link AuraIntent}, {@link AuraDominion}, with {@link Spellblade}
 * and {@link AuraMarks} where aura meets spells.
 *
 * <p>The rules and their numbers are {@link AuraRules}; the hooks for other systems are {@link AuraApi}.</p>
 */
public final class Aura {
	private Aura() {}

	/** What can carry aura: swords, axes, spears, the trident and the mace, and anything servers and add-ons add. */
	public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, Wildercord.id("aura_weapons"));
	/** Aura projected off a blade (the slash and a Thunder spark): armour applies, and against players the spell defences do. */
	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Wildercord.id("aura"));

	private static final Identifier REACH = Wildercord.id("aura_reach");
	private static final Identifier SWEEP = Wildercord.id("aura_sweep");
	private static final Identifier STEADY = Wildercord.id("aura_stone");
	private static final Identifier HASTE = Wildercord.id("aura_hourglass");
	private static final Identifier SWIFT = Wildercord.id("aura_gale");

	// ------------------------------------------------------------------ reading (both sides)

	public static AuraAttachments.Data data(Player player) {
		return player.getAttachedOrElse(AuraAttachments.AURA, AuraAttachments.Data.NONE);
	}

	public static AuraAttachments.State state(Player player) {
		return player.getAttachedOrElse(AuraAttachments.STATE, AuraAttachments.State.NONE);
	}

	public static AuraAttachments.Look look(Player player) {
		return player.getAttachedOrElse(AuraAttachments.LOOK, AuraAttachments.Look.NONE);
	}

	/** The player's stage: 0 before they learn a method. */
	public static int stage(Player player) {
		AuraAttachments.Data data = data(player);
		return data.learned() ? data.stage() : AuraRules.NONE;
	}

	public static Optional<BreathingMethod> method(Player player) {
		return BreathingMethods.byId(data(player).method());
	}

	/** The element the player's aura carries, or "". */
	public static String element(Player player) {
		return method(player).map(BreathingMethod::element).orElse("");
	}

	/** How much aura the player can hold. */
	public static int capacity(Player player) {
		int stage = stage(player);
		// The Breath Sash, worn, holds more.
		return stage <= AuraRules.NONE ? 0 : dev.wildercord.aura.world.ForgedGear.capacity(player, AuraStages.capacity(stage));
	}

	public static float aura(Player player) {
		return Math.min(data(player).aura(), capacity(player));
	}

	/** The aura's colour now (0xRRGGBB), or 0 without a method. */
	public static int color(Player player) {
		return method(player).map(m -> m.color(stage(player))).orElse(0);
	}

	/** Whether aura works on this server (or, on a client, the server it's on). */
	public static boolean enabled(Player player) {
		return Config.aura(player);
	}

	/** Whether {@code entity} holds something that carries aura in its main hand. */
	public static boolean holdsWeapon(LivingEntity entity) {
		return entity.getMainHandItem().is(WEAPONS);
	}

	/** Whether a blow struck now would be coated: aura works, a method is learned, an aura weapon is in hand, and there's aura for it. */
	public static boolean coated(Player player) {
		return enabled(player) && stage(player) >= AuraRules.GLOW && holdsWeapon(player) && aura(player) >= AuraRules.COAT_COST - 1.0E-4;
	}

	// ------------------------------------------------------------------ filling and spending (server)

	static void set(ServerPlayer player, AuraAttachments.Data data) {
		player.setAttached(AuraAttachments.AURA, data);
	}

	static void state(ServerPlayer player, AuraAttachments.State state) {
		if (!state.equals(state(player))) {
			player.setAttached(AuraAttachments.STATE, state);
		}
	}

	/** Fills aura by {@code amount} (see {@link AuraApi#gain}); returns what was really gained. */
	public static double gain(ServerPlayer player, double amount, String source) {
		AuraAttachments.Data data = data(player);
		// Spent (an awakening just burned out): nothing comes in at all, from anywhere, until it lets go.
		if (!data.learned() || !enabled(player) || amount <= 0 || Awakening.spent(player)) {
			return 0;
		}
		double a = amount * Config.get().aura().gainMultiplier();
		if (method(player).map(BreathingMethod::flavour).orElse(null) == BreathingMethod.Flavour.STARLIT) {
			a *= AuraRules.starlitGain(data.stage());
		}
		for (AuraApi.GainHook hook : AuraApi.gainHooks()) {
			try {
				a = hook.modify(player, a, source);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("An aura gain hook threw; skipping it", e);
			}
		}
		float before = Math.min(data.aura(), capacity(player));
		float after = (float) Math.min(capacity(player), before + Math.max(0, a));
		if (after != data.aura()) {
			set(player, data.withAura(after));
		}
		return after - before;
	}

	/**
	 * Gives {@code amount} aura straight back (a Starlit art's refund): up to the pool's capacity, and not scaled as a gain is (the
	 * server's gain rate, Starlit's own passive, the gain hooks), since it gives back what an art was paid. Returns what came back.
	 */
	public static double giveBack(ServerPlayer player, double amount) {
		AuraAttachments.Data data = data(player);
		if (!data.learned() || !enabled(player) || amount <= 0 || Awakening.spent(player)) {
			return 0;
		}
		float before = Math.min(data.aura(), capacity(player));
		float after = (float) Math.min(capacity(player), before + amount);
		if (after != data.aura()) {
			set(player, data.withAura(after));
		}
		return after - before;
	}

	/** Spends {@code cost} (see {@link AuraApi#spend}): short of the price, everything there goes and backlash follows. */
	public static AuraRules.Spend spend(ServerPlayer player, double cost, String reason) {
		AuraAttachments.Data data = data(player);
		AuraRules.Spend spend = AuraRules.spend(Math.min(data.aura(), capacity(player)), cost);
		if (player.isCreative()) {
			spend = new AuraRules.Spend(Math.min(data.aura(), capacity(player)), cost, false);
		}
		if ((float) spend.left() != data.aura()) {
			set(player, data.withAura((float) spend.left()));
		}
		for (AuraApi.SpendHook hook : AuraApi.spendHooks()) {
			try {
				hook.spent(player, spend.paid(), reason, spend.backlash());
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("An aura spend hook threw; skipping it", e);
			}
		}
		if (spend.backlash()) {
			backlash(player);
		}
		return spend;
	}

	/** Backlash: brief exhaustion, slowed and weakened. It never damages; a second one this soon only renews the first. */
	public static void backlash(ServerPlayer player) {
		int ticks = Config.get().aura().backlashTicks();
		if (ticks <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		AuraAttachments.State state = state(player);
		boolean fresh = now >= state.backlashUntil() - ticks + AuraRules.BACKLASH_REST;
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0, false, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true, true));
		state(player, state.backlash(now + ticks));
		if (fresh) {
			ServerLevel level = player.level();
			Vec3 at = player.position().add(0, 1.0, 0);
			Feels.sound(level, at, "aura_backlash", 0.9F, 1.0F);
			Motes.clouds(level, at, 5, 0.35, 0x6A6478, 0.5, 26, new Vec3(0, 0.01, 0), 0.02, 0.35);
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.backlash").withColor(0xC8A0A0));
		}
	}

	// ------------------------------------------------------------------ the breathing stance

	/** What the server remembers of each player's stance between ticks. */
	private static final class Stance {
		Vec3 anchor;
		int still;
		long releasedAt = -1;
	}

	private static final Map<UUID, Stance> STANCES = new HashMap<>();
	/** When each player last struck or was struck, for "in a fight" (Gale). */
	private static final Map<UUID, Long> FIGHTING = new HashMap<>();

	/** Notes that a player struck or was struck just now (and their body's aura flares for it, see {@link AuraFx}). */
	static void fighting(ServerPlayer player) {
		FIGHTING.put(player.getUUID(), player.level().getGameTime());
		AuraFx.fighting(player);
	}

	static boolean inFight(ServerPlayer player) {
		Long at = FIGHTING.get(player.getUUID());
		return at != null && player.level().getGameTime() - at <= AuraRules.COMBAT_TICKS;
	}

	/** Breaks the stance (moving, standing up for good, a blow taken): the beat and any stillness toward a breakthrough start over. */
	static void breakStance(ServerPlayer player) {
		Stance stance = STANCES.get(player.getUUID());
		if (stance != null) {
			stance.still = 0;
			stance.releasedAt = -1;
			stance.anchor = player.position();
		}
		AuraBreakthroughs.broken(player);
		AuraAttachments.State state = state(player);
		if (state.breathing() || state.stillness() > 0) {
			state(player, state.settled(-1).stillness(0));
		}
	}

	private static void stance(ServerPlayer player, long now) {
		Stance stance = STANCES.computeIfAbsent(player.getUUID(), k -> new Stance());
		AuraAttachments.State state = state(player);
		Vec3 pos = player.position();
		if (stance.anchor == null) {
			stance.anchor = pos;
		}
		boolean able = enabled(player) && stage(player) >= AuraRules.GLOW && holdsWeapon(player) && player.onGround() && !player.isUsingItem()
			&& !player.isSpectator() && player.getAttached(WildercordAttachments.CHARGE) == null && !player.isPassenger();
		boolean moved = stance.anchor.distanceToSqr(pos) > 0.04;
		if (!able || moved) {
			if (state.breathing() || state.stillness() > 0) {
				breakStance(player);
			}
			stance.anchor = pos;
			stance.still = 0;
			stance.releasedAt = -1;
			return;
		}
		if (player.isShiftKeyDown()) {
			if (stance.releasedAt >= 0) {
				// Sneak let up and pressed again: a breath. On the beat it draws aura in at once.
				if (state.breathing() && now - stance.releasedAt <= AuraRules.BOB_TICKS) {
					breath(player, state, now);
				}
				stance.releasedAt = -1;
			}
			stance.still++;
			// The Breath Sash, worn, settles it sooner.
			if (!state.breathing() && stance.still >= dev.wildercord.aura.world.ForgedGear.settleTicks(player)) {
				state = state.settled(now);
				state(player, state);
				Feels.sound(player.level(), pos, "aura_breath", 0.45F, 1.0F);
				AuraSense.pulse(player);
			}
		} else if (state.breathing()) {
			if (stance.releasedAt < 0) {
				stance.releasedAt = now;
			} else if (now - stance.releasedAt > AuraRules.BOB_TICKS) {
				breakStance(player);
				return;
			}
		} else {
			stance.still = 0;
		}
		state = state(player);
		if (!state.breathing() || stance.releasedAt >= 0) {
			return;
		}
		long held = now - state.settledAt();
		if (held % 5 == 0) {
			gain(player, AuraRules.BREATH_GAIN / 4, "stance");
		}
		if (held > 0 && held % AuraRules.BREATH_PERIOD == 0) {
			AuraSense.pulse(player);
			AuraVfx.breathe(player, color(player));
		}
		AuraBreakthroughs.stillness(player, now);
	}

	/** A breath (sneak let up and pressed again) while in the stance: on the beat it draws in aura at once. */
	private static void breath(ServerPlayer player, AuraAttachments.State state, long now) {
		if (!AuraRules.onBeat(state.settledAt(), now)) {
			return;
		}
		double got = gain(player, AuraRules.BEAT_GAIN, "beat");
		Feels.sound(player.level(), player.position(), "aura_breath", 0.7F, Feels.step(2));
		AuraVfx.beat(player, color(player));
		if (got > 0) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.beat").withColor(0xFF000000 | color(player)));
		}
	}

	// ------------------------------------------------------------------ what each stage and method gives

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0) {
			if (current != null) {
				instance.removeModifier(id);
			}
			return;
		}
		if (current == null || Math.abs(current.amount() - amount) > 1.0E-6 || current.operation() != operation) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	private static void modifiers(ServerPlayer player) {
		int stage = stage(player);
		boolean weapon = enabled(player) && stage >= AuraRules.GLOW && holdsWeapon(player);
		boolean coated = weapon && coated(player);
		BreathingMethod.Flavour flavour = weapon ? method(player).map(BreathingMethod::flavour).orElse(BreathingMethod.Flavour.NONE) : BreathingMethod.Flavour.NONE;
		modifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH, coated && stage >= AuraRules.EDGE ? AuraRules.EDGE_REACH : 0, AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.SWEEPING_DAMAGE_RATIO, SWEEP, coated && stage >= AuraRules.FLOW ? AuraRules.FLOW_SWEEP_SHARE : 0, AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.KNOCKBACK_RESISTANCE, STEADY, flavour == BreathingMethod.Flavour.STONE ? AuraRules.stoneResistance(stage) : 0,
			AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.ATTACK_SPEED, HASTE, flavour == BreathingMethod.Flavour.HASTE ? AuraRules.hourglassSpeed(stage) : 0,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifier(player, Attributes.MOVEMENT_SPEED, SWIFT, flavour == BreathingMethod.Flavour.GALE && inFight(player) ? AuraRules.galeSpeed(stage) : 0,
			AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	/** Keeps everyone's view of the player's aura current (only when it changes, so nothing is sent while it doesn't). */
	private static void refreshLook(ServerPlayer player, long now) {
		AuraAttachments.Look look;
		if (stage(player) <= AuraRules.NONE || !enabled(player)) {
			look = AuraAttachments.Look.NONE;
		} else {
			AuraAttachments.State state = state(player);
			look = new AuraAttachments.Look(color(player), stage(player), aura(player) >= AuraRules.COAT_COST - 1.0E-4, state.guarding(now), state.breathing());
		}
		if (!look.equals(look(player))) {
			player.setAttached(AuraAttachments.LOOK, look);
		}
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			long now = player.level().getGameTime();
			if (stage(player) <= AuraRules.NONE) {
				if (player.hasAttached(AuraAttachments.LOOK)) {
					player.removeAttached(AuraAttachments.LOOK);
				}
				if (player.hasAttached(AuraPresence.LOOK)) {
					player.removeAttached(AuraPresence.LOOK);
				}
				continue;
			}
			stance(player, now);
			AuraGuard.tick(player, now);
			if ((now + player.getId()) % 5 == 0) {
				modifiers(player);
			}
			refreshLook(player, now);
			// The top stages: Intent's pressure, sense in a fight, and aura armour's shell.
			AuraIntent.tick(player, now);
			AuraSense.combat(player, now);
			AuraPresence.look(player, AuraPresence.look(player).withShell(AuraArmour.up(player)));
			// Momentum's tier, as everyone sees it in the body's aura, follows its ebb.
			Momentum.tick(player, now);
			// An awakening running out (the swordsman spent), a spent body recovering.
			Awakening.tick(player, now);
		}
		AuraIntent.release(server);
		AuraDominion.tick(server);
		Spellblade.tick(server);
	}

	// ------------------------------------------------------------------ the Aura key

	/** The Aura key, pressed one of its ways (see {@link AuraApi.Trigger}): the server picks the technique. */
	public record Key(int trigger) implements CustomPacketPayload {
		public static final Type<Key> TYPE = new Type<>(Wildercord.id("aura_key"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Key> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Key::trigger, Key::new).cast();

		@Override
		public Type<Key> type() {
			return TYPE;
		}
	}

	private static final PacketThrottle KEYS = PacketThrottle.casting();

	/** Sets off the technique a trigger gives the player's stage; says why not when there's none. */
	public static boolean press(ServerPlayer player, AuraApi.Trigger trigger) {
		if (!player.isAlive() || player.isSpectator()) {
			return false;
		}
		if (!enabled(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.disabled").withColor(0xA89CC8));
			return false;
		}
		int stage = stage(player);
		if (stage <= AuraRules.NONE) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.no_method").withColor(0xA89CC8));
			return false;
		}
		// Silenced (a Hollow swordsman's Null Parry): nothing but the guard until it passes.
		if (trigger != AuraApi.Trigger.SNEAK_TAP && dev.wildercord.aura.arts.ArtWards.silenced(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.silenced").withColor(0xA89CC8));
			return false;
		}
		// Opened in a duel (their stance broken): the guard is broken until the opening passes.
		if (trigger == AuraApi.Trigger.SNEAK_TAP && dev.wildercord.aura.Stance.opened(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.guard_broken").withColor(0xE08A60));
			return false;
		}
		Optional<AuraApi.Technique> technique = AuraApi.techniqueFor(stage, trigger);
		if (technique.isEmpty()) {
			// What the key would do, and the stage it opens at.
			AuraApi.techniques().stream().filter(t -> t.trigger() == trigger).findFirst().ifPresent(t -> player.sendOverlayMessage(
				Component.translatable("message.wildercord.aura.not_yet", Component.translatable(t.nameKey()),
					Component.translatable("aura.wildercord.stage." + AuraStages.id(t.stage()))).withColor(0xA89CC8)));
			return false;
		}
		try {
			return technique.get().performer().perform(player);
		} catch (RuntimeException e) {
			Wildercord.LOGGER.warn("Aura technique {} threw", technique.get().id(), e);
			return false;
		}
	}

	// ------------------------------------------------------------------ lifecycle

	public static void init() {
		AuraAttachments.init();
		AuraPresence.init();
		PayloadTypeRegistry.serverboundPlay().register(Key.TYPE, Key.CODEC);
		AuraSense.init();
		AuraStep.init();
		AuraDominion.init();
		ServerPlayNetworking.registerGlobalReceiver(Key.TYPE, (payload, context) -> {
			AuraApi.Trigger[] triggers = AuraApi.Trigger.values();
			if (payload.trigger() < 0 || payload.trigger() >= triggers.length) {
				return;
			}
			if (KEYS.allow(context.player().getUUID(), context.server().getTickCount())) {
				press(context.player(), triggers[payload.trigger()]);
			}
		});
		AuraApi.registerTechnique(new AuraApi.Technique("guard", AuraRules.FLOW, AuraApi.Trigger.SNEAK_TAP, AuraRules.GUARD_RAISE_COST, AuraGuard::raise));
		AuraApi.registerTechnique(new AuraApi.Technique("slash", AuraRules.EDGE, AuraApi.Trigger.TAP, AuraRules.SLASH_COST, AuraSlash::loose));
		AuraApi.registerTechnique(new AuraApi.Technique("step", AuraRules.FORM, AuraApi.Trigger.DOUBLE_TAP, AuraRules.STEP_COST, AuraStep::step));
		AuraApi.registerTechnique(new AuraApi.Technique("dominion", AuraRules.SOVEREIGN, AuraApi.Trigger.HOLD, AuraRules.DOMINION_COST, AuraDominion::raise));
		// Awakening (a tap, then a held press): everything let go at once, for a while; spent after.
		AuraApi.registerTechnique(new AuraApi.Technique("awaken", AwakeningRules.FROM, AuraApi.Trigger.TAP_HOLD, 0, Awakening::awaken));
		// Sword strings: arts set off by a run of ordinary swings, read by the client, checked and performed here.
		SwordStrings.init();
		// Aura's feel: trails, impacts, banners, bursts and the body's aura, drawn by each client as it sees them.
		AuraFx.init();
		// Momentum (a clean fight fills it; the Final Art waits on its peak) and stance (worn by blade and aura, broken into an opening
		// and a finisher).
		Momentum.init();
		dev.wildercord.aura.Stance.init();
		// Awakening (after momentum: it holds momentum at its peak, and holds it again after momentum empties in a new world).
		Awakening.init();
		AuraMethods.init();
		Crescents.init();
		AuraCombat.init();
		AuraBreakthroughs.init();
		AuraLoot.init();
		ServerTickEvents.END_SERVER_TICK.register(Aura::tick);
		// A blow taken breaks the stance (and any stillness toward a breakthrough), and counts as being in a fight.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && taken > 0 && stage(player) > AuraRules.NONE) {
				breakStance(player);
				fighting(player);
			}
		});
		// A new body starts with its aura empty (the path itself is kept) and nothing under way.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				AuraAttachments.Data data = data(newPlayer);
				if (data.aura() > 0) {
					set(newPlayer, data.withAura(0));
				}
				newPlayer.removeAttached(AuraAttachments.STATE);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			STANCES.remove(id);
			FIGHTING.remove(id);
			KEYS.forget(id);
			AuraCombat.forget(id);
			AuraMethods.forget(id);
			AuraStep.forget(id);
			AuraIntent.forget(id);
			AuraDominion.forget(id);
			AuraMarks.forget(id);
			SwordStrings.forget(id);
			AuraFx.forget(id);
			AuraGuard.forget(id);
			Momentum.forget(id);
			dev.wildercord.aura.Stance.forget(id);
			dev.wildercord.aura.arts.MethodArts.forget(id);
			// A spell riding the blade leaves with its caster (its cast is no longer alive).
			Spellblade.forget(id);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STANCES.clear();
			FIGHTING.clear();
			KEYS.clear();
			AuraCombat.clear();
			AuraBreakthroughs.clear();
			AuraStep.clear();
			AuraIntent.clear();
			AuraDominion.clear();
			AuraMarks.clear();
			SwordStrings.clear();
			AuraFx.clear();
			AuraGuard.clear();
			Momentum.clear();
			dev.wildercord.aura.Stance.clear();
			dev.wildercord.aura.arts.MethodArts.clear();
			Spellblade.clear();
		});
	}

	/** Every kit sound aura plays, for the tests. */
	public static final List<String> SOUNDS = List.of("aura_slash", "aura_guard", "aura_perfect_guard", "aura_breakthrough", "aura_backlash", "aura_breath",
		"aura_step", "aura_armour", "aura_intent", "aura_dominion", "aura_dominion_fade", "aura_spellblade", "aura_string_tick", "aura_string_complete",
		"aura_string_fumble", "aura_momentum_rise", "aura_momentum_peak", "aura_stance_break", "aura_finisher", "aura_awaken", "aura_awaken_fed",
		"aura_spent", "aura_recovered");

	/** Plays one of aura's sounds where {@code player} is. */
	public static void sound(ServerPlayer player, String name, float volume, float pitch) {
		Feels.sound(player.level(), player.position().add(0, 1, 0), name, volume, pitch);
	}
}
