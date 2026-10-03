package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.WildercordEffects;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.GearDef;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/**
 * How a player stands up to spells, and the one way spell damage reaches anyone. Every spell hit (a player's, a
 * Runebound's, a boss's, a wisp's, a reflection) lands through {@link #hurt}: on a creature it lands as it is; on a player
 * it first meets their defences, which multiply:
 * <ul>
 *   <li><b>Armour</b>: against spells that armour doesn't stop at all (magic, frost), armour and toughness count for
 *   {@code defence.armour_rate} of what they would against a blade. Spells armour already stops (fire, lightning, blasts)
 *   are left to the game.</li>
 *   <li><b>Warding</b> (armour enchantment, I to IV): two protection points a level against spells, sharing the game's cap
 *   of 20 (80%) with Protection, which it can't sit beside.</li>
 *   <li><b>Warded</b> (the Potion of Warding): a fifth off per level, 80% at most.</li>
 *   <li><b>Focus of Resolve</b> (in the focus slot or off-hand): a fifth off, in exchange for 15% weaker outgoing spell effects.</li>
 * </ul>
 * Resistance, Protection and absorption then work as the game has them. Last comes the <b>spellguard</b>: a spell hit that
 * would kill a player it found at {@code defence.spellguard_health} of their health or more leaves them on one heart
 * instead, and for a moment after (the rest of that spell) no spell finishes them; then it recharges.
 *
 * <p>Spells never use damage that nothing survives: one that tries (an add-on's rune dealing /kill damage) lands as magic
 * instead, so it meets all of this. The real /kill and the void are never spell hits and are left alone.</p>
 */
public final class SpellDefence {
	private SpellDefence() {}

	/** The armour enchantment against spells. Data-made (data/wildercord/enchantment/warding.json); what it does is here. */
	public static final ResourceKey<Enchantment> WARDING = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("warding"));
	/** The colour of spell defence (the spellguard's light, the Cord screen's badge, Warded): the amber of a Shield's circles ({@link Shields#COLOR}). */
	public static final int GUARD_COLOR = 0xF5B04A;
	/**
	 * After the spellguard holds, how long (ticks) spells can't finish the player it saved: a spell of several effects lands
	 * them one after another in the same moment, and the second shouldn't undo what the guard did for the first.
	 */
	static final int GUARD_GRACE = 10;
	/** The phase the spellguard answers a death in: before anything else might (Reversal, Rebirth, a duel's knockout, a totem). */
	private static final Identifier GUARD_PHASE = Wildercord.id("spellguard");

	/** A spell hit landing on a player right now: who, and their health as it arrived. Null outside one. */
	private record Landing(LivingEntity target, float healthBefore, Object castIdentity) {}
	private record Grace(Object castIdentity, long serverTick) {}

	private static Landing landing;
	/** Short lived protection for the remaining hits of the cast the guard stopped. */
	private static final Map<ServerPlayer, Grace> grace = new WeakHashMap<>();
	private static final class Burst {
		long tick=Long.MIN_VALUE;
		final java.util.IdentityHashMap<Object,Float> health=new java.util.IdentityHashMap<>();
	}
	private static final Map<ServerPlayer,Burst> BURSTS=new WeakHashMap<>();
	private static float firstHealth(LivingEntity target,Object identity){
		if(!(target instanceof ServerPlayer p))return target.getHealth();
		Burst burst=BURSTS.computeIfAbsent(p,k->new Burst());long tick=p.level().getServer().getTickCount();
		if(burst.tick!=tick){burst.health.clear();burst.tick=tick;}
		Float first=burst.health.get(identity);if(first!=null)return first;
		if(burst.health.size()>=128)burst.health.remove(burst.health.keySet().iterator().next());
		burst.health.put(identity,target.getHealth());return target.getHealth();
	}

	public static void init() {
		DefensiveFoci.init();
		// The guard is a limit on one hit, not a way back from death: a hit it stops never killed anyone, so nothing that
		// answers a death (Reversal, Rebirth, Second Wind, a duel's knockout, a totem) is spent on it. It goes first.
		ServerLivingEntityEvents.ALLOW_DEATH.addPhaseOrdering(GUARD_PHASE, Event.DEFAULT_PHASE);
		ServerLivingEntityEvents.ALLOW_DEATH.register(GUARD_PHASE, SpellDefence::allowDeath);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				grace.values().removeIf(saved -> server.getTickCount() - saved.serverTick > GUARD_GRACE);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {grace.clear();BURSTS.clear();});
	}

	// ------------------------------------------------------------------ landing

	/**
	 * Deals a spell's damage: to a player, what their defences leave of it, under the spellguard; to anything else, all of
	 * it. Every spell's damage comes through here.
	 *
	 * @return whether it hurt
	 */
	public static boolean hurt(ServerLevel level, LivingEntity target, DamageSource source, float amount) {
		return hurt(level, target, source, amount, new Object());
	}

	/** A hit from a known cast: children and repeated landings carry its identity through the guard. */
	public static boolean hurt(ServerLevel level, LivingEntity target, DamageSource source, float amount, Cast cast) {
		return hurt(level, target, source, amount, cast.identity());
	}

	/** A paid blade/spell resonance: normal shield, armour, boss resistance and cast guard, with no recursive rune triggers. */
	public static void resonantHurt(Cast cast, LivingEntity target, float amount) {
		if (!cast.alive() || !target.isAlive() || !Float.isFinite(amount) || amount <= 0
				|| Shields.stops(cast, target, cast.caster.getEyePosition())) return;
		Effects.readyToHurt(target);
		Dungeons.spellHit(() -> hurt(cast.level, target,
			cast.level.damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC,cast.caster,cast.caster), amount, cast));
	}

	private static boolean hurt(ServerLevel level, LivingEntity target, DamageSource source, float amount, Object castIdentity) {
		DamageSource spell = spellSource(level, source);
		if (target instanceof dev.wildercord.aura.world.AuraBeast beast) {
			return beast.hurtBySpell(level, spell, amount);
		}
		if (!(target instanceof Player player)) {
			return target.hurtServer(level, spell, amount);
		}
		float left = reduce(level, player, spell, amount);
		if (ArmorResponses.mirrorReady(player)) {
			boolean hurt = guarded(target, castIdentity, () -> target.hurtServer(level, spell, left*.5F));
			if(hurt) ArmorResponses.fragment(level,player,spell,left);
			return hurt;
		}
		if (player instanceof ServerPlayer serverPlayer && DefensiveFoci.available(serverPlayer, left)) {
			float delayed = left * DefensiveFoci.DELAY_SHARE;
			float immediate = left - delayed;
			boolean hurt = guarded(target, castIdentity, () -> target.hurtServer(level, spell, immediate));
			if (hurt) DefensiveFoci.defer(serverPlayer, delayed,spell);
			return hurt;
		}
		return guarded(target, castIdentity, () -> target.hurtServer(level, spell, left));
	}

	/**
	 * Runs damage a spell brought about that has already met the defences it should (a share of a wound passed along a bond,
	 * a Stasis letting its held hits go) as a spell hit for the spellguard, without weighing it again.
	 */
	public static boolean guarded(LivingEntity target, BooleanSupplier hit) {
		return guarded(target, new Object(), hit);
	}

	private static boolean guarded(LivingEntity target, Object castIdentity, BooleanSupplier hit) {
		if (!(target instanceof Player)) {
			return hit.getAsBoolean();
		}
		Landing outer = landing;
		// A hit inside a hit on the same player (a dodge letting part of it through) keeps the health it first found.
		landing = outer != null && outer.target == target && outer.castIdentity == castIdentity
			? outer : new Landing(target, firstHealth(target,castIdentity), castIdentity);
		try {
			return hit.getAsBoolean();
		} finally {
			landing = outer;
		}
	}

	/**
	 * Damage nothing survives (the /kill and void types) is never a spell's: a spell that asks for it gets magic from the same
	 * hand instead, so armour, Resistance, a totem and the rest still answer it.
	 */
	static DamageSource spellSource(ServerLevel level, DamageSource source) {
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return source;
		}
		return source.getEntity() != null ? level.damageSources().indirectMagic(source.getDirectEntity(), source.getEntity()) : level.damageSources().magic();
	}

	// ------------------------------------------------------------------ the defences

	/** What a player's own defences against spells (armour, Warding, Warded) leave of a spell hit, before the game's own. */
	public static float reduce(ServerLevel level, Player player, DamageSource source, float amount) {
		if (amount <= 0 || player.isCreative() || player.isSpectator()) {
			return amount;
		}
		WildercordConfig.DefenceSettings settings = Config.get().defence();
		double left = amount;
		// Armour the game ignores for this kind of harm counts for part of its worth; where the game counts it, it's the game's.
		if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
			left *= 1 - SpellDefenceRules.armourShare(felt(level, source, amount), player.getArmorValue(),
				player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), settings.armourRate());
		}
		int warding = wardingLevels(player);
		if (warding > 0) {
			// Protection's own points against this hit, which the game takes off after this; Warding shares their cap.
			double protection = source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) ? 0 : EnchantmentHelper.getDamageProtection(level, player, source);
			left *= SpellDefenceRules.wardingFactor(protection, warding);
		}
		left *= 1 - SpellDefenceRules.wardedShare(wardedLevel(player));
		// The focus slot offers a defensive choice in place of a casting focus. Its spell power penalty is in GearDef.
		if (Gear.of(player).pieces().contains(GearDef.RESOLVE)) {
			left *= 1 - GearDef.RESOLVE_PROTECTION;
		}
		left *= ArmorResponses.factor(player);
		return (float) left;
	}

	/**
	 * The size a hit will land at (a monster's harder on Hard, as the game makes it), which is what armour is weighed
	 * against: toughness keeps more of it against big hits.
	 */
	private static float felt(ServerLevel level, DamageSource source, float amount) {
		if (!source.scalesWithDifficulty()) {
			return amount;
		}
		Difficulty difficulty = level.getDifficulty();
		return difficulty == Difficulty.HARD ? amount * 1.5F : difficulty == Difficulty.EASY ? Math.min(amount / 2 + 1, amount) : amount;
	}

	/** Warding's levels on everything a player wears, added up (on a client too, for the Cord screen). */
	public static int wardingLevels(Player player) {
		Optional<? extends Holder<Enchantment>> warding = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(WARDING);
		if (warding.isEmpty()) {
			return 0;
		}
		int levels = 0;
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				levels += Math.min(SpellDefenceRules.WARDING_MAX_LEVEL, EnchantmentHelper.getItemEnchantmentLevel(warding.get(), player.getItemBySlot(slot)));
			}
		}
		return levels;
	}

	/** Protection's levels on everything a player wears (for the Cord screen, which can't ask the server's enchantment rules). */
	public static int protectionLevels(Player player) {
		Optional<? extends Holder<Enchantment>> protection = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
			.get(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
		if (protection.isEmpty()) {
			return 0;
		}
		int levels = 0;
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				levels += EnchantmentHelper.getItemEnchantmentLevel(protection.get(), player.getItemBySlot(slot));
			}
		}
		return levels;
	}

	/** Warded's level on a creature: its amplifier plus one, or 0 without it. */
	public static int wardedLevel(LivingEntity entity) {
		MobEffectInstance warded = entity.getEffect(WildercordEffects.WARDED);
		return warded == null ? 0 : warded.getAmplifier() + 1;
	}

	/** The most a hit's bonuses may multiply a spell by against {@code target}: the server's cap for a player, none for a creature. */
	static double maxBonus(LivingEntity target) {
		return target instanceof Player ? Config.get().defence().maxBonus() : Double.POSITIVE_INFINITY;
	}

	// ------------------------------------------------------------------ the spellguard

	/** Seconds before a player's spellguard is back (0: it's ready), on the server or, for the Cord screen, on their client. */
	public static int guardSeconds(Player player) {
		return SpellDefenceRules.guardSeconds(player.level().getGameTime(), player.getAttached(WildercordAttachments.SPELLGUARD),
			Config.defence(player).spellguardRechargeSeconds());
	}

	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		Landing hit = landing;
		if (hit == null || hit.target != entity || !(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
				|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}
		WildercordConfig.DefenceSettings settings = Config.get().defence();
		long now = level.getGameTime();
		long serverTick = level.getServer().getTickCount();
		Long heldAt = player.getAttached(WildercordAttachments.SPELLGUARD);
		// Just held: the rest of the same spell can't finish them either.
		Grace recent = grace.get(player);
		if (settings.spellguard() && recent != null && recent.castIdentity == hit.castIdentity && recent.serverTick <= serverTick
				&& serverTick - recent.serverTick <= GUARD_GRACE) {
			player.setHealth(Math.min(player.getMaxHealth(), SpellDefenceRules.GUARD_LEAVES));
			return false;
		}
		if (!SpellDefenceRules.guardHolds(settings.spellguard(), hit.healthBefore, player.getMaxHealth(), settings.spellguardHealth(), now, heldAt,
				settings.spellguardRechargeSeconds())) {
			return true;
		}
		player.setHealth(Math.min(player.getMaxHealth(), SpellDefenceRules.GUARD_LEAVES));
		player.setAttached(WildercordAttachments.SPELLGUARD, now);
		grace.put(player, new Grace(hit.castIdentity, serverTick));
		held(level, player, settings.spellguardRechargeSeconds());
		return false;
	}

	/** The guard breaking: a ring of amber light, glass shattering, a jolt, and a word on how long until it's back. */
	private static void held(ServerLevel level, ServerPlayer player, int recharge) {
		Vec3 at = player.getBoundingBox().getCenter();
		Light.ring(level, at, new Vec3(0, 1, 0), GUARD_COLOR, 0.3, 1.6, 0.06, 10);
		Light.ring(level, at.add(0, 0.5, 0), new Vec3(0, 1, 0), 0xFFF4C0, 0.2, 1.1, 0.04, 8);
		Fx.send(level, ParticleTypes.WAX_OFF, at, 16, 0.45, 0.4);
		Fx.send(level, ParticleTypes.END_ROD, at, 10, 0.3, 0.12);
		Fx.sound(level, at, WildercordSounds.SHIELD_BREAK, 1.0F, 1.15F);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F);
		ScreenFx.kick(player, 0.5F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.spellguard", recharge).withColor(GUARD_COLOR));
	}
}
