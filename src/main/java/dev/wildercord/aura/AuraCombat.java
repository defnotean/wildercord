package dev.wildercord.aura;

import dev.wildercord.cast.AuraElements;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Mastery;
import dev.wildercord.cast.PracticeRoom;
import dev.wildercord.cast.SpellDefence;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.MasteryRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Aura in a fight: what a coated blow adds (the coat itself, the method's element against the foe's affinity and marks, the
 * Edge's bite through armour), what every meaningful blow gives back (aura and experience), the methods' passives, and the
 * wider sweep of Flow. Blows reach here from {@code mixin.PlayerAuraMixin}, wrapped round the very call that hurts the foe,
 * so a blow is measured by what it really took, killing blows included.
 *
 * <p>Against another player a blow's aura bonuses are one factor under the spell-defence cap and scaled by the aura PvP
 * scale; aura projected off the blade (the slash, a spark) meets the spell defences in full ({@link #projected}).</p>
 */
public final class AuraCombat {
	private AuraCombat() {}

	/** How much of a full swing each player's blow was struck at, noted as the swing begins (vanilla resets it before the blow lands). */
	private static final Map<Player, Float> SWINGS = new WeakHashMap<>();
	/** Whether each player's blow this tick was coated: a sweep's other blows share the first one's coat. */
	private static final Map<UUID, Long> COATED = new HashMap<>();
	/** When each player's Verdant blade last mended them. */
	private static final Map<UUID, Long> MENDED = new HashMap<>();
	/** Each player's memory of where they fought lately and against what: for repetition, as spell mastery remembers. */
	private static final Map<UUID, Map<Long, double[]>> PLACES = new HashMap<>();
	/** Each player's moment (low health, a crowd, a boss near, a dungeon), worked out at most once a second. */
	private static final Map<UUID, double[]> MOMENT = new HashMap<>();

	public static void init() {
		// Spells on a foe of a stronger-foe trial: it has to fall to melee and aura alone.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> spellOn(entity, source));
		ServerLivingEntityEvents.AFTER_DEATH.register(AuraCombat::spellOn);
	}

	private static void spellOn(LivingEntity entity, DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player && !melee(player, source) && !source.is(Aura.DAMAGE)) {
			AuraBreakthroughs.tainted(player, entity);
		}
	}

	/** Whether {@code source} is {@code player}'s own blow (a swing, a sweep or a spear's thrust). */
	static boolean melee(ServerPlayer player, DamageSource source) {
		return source.getDirectEntity() == player && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.SPEAR));
	}

	static void forget(UUID id) {
		COATED.remove(id);
		MENDED.remove(id);
		PLACES.remove(id);
		MOMENT.remove(id);
	}

	static void clear() {
		SWINGS.clear();
		COATED.clear();
		MENDED.clear();
		PLACES.clear();
		MOMENT.clear();
	}

	// ------------------------------------------------------------------ a blow

	/** Notes how full a player's swing is, as it begins (and what it was, for the trail everyone else sees). */
	public static void swing(Player player) {
		if (player instanceof ServerPlayer server) {
			float strength = player.isUsingItem() ? 1.0F : player.getAttackStrengthScale(0.5F);
			SWINGS.put(player, strength);
			AuraFx.swingBegins(server, strength);
		}
	}

	/**
	 * A player's blow on {@code target}, about to land at {@code damage}: coats it, measures what it takes and answers it.
	 * {@code hurt} lands it at the damage given and says whether it hurt.
	 *
	 * @param first whether this is the blow itself (a sweep's other blows share its coat and pay nothing more)
	 */
	public static boolean blow(Player attacker, Entity target, DamageSource source, float damage, boolean first, java.util.function.Function<Float, Boolean> hurt) {
		if (!(attacker instanceof ServerPlayer player) || !(target instanceof LivingEntity living) || Aura.stage(player) <= AuraRules.NONE
				|| !Aura.enabled(player) || !Aura.holdsWeapon(player)) {
			return hurt.apply(damage);
		}
		long now = player.level().getGameTime();
		boolean coated;
		if (first) {
			coated = Aura.coated(player) && harmable(player, living);
			if (coated) {
				COATED.put(player.getUUID(), now);
			} else {
				COATED.remove(player.getUUID());
			}
		} else {
			Long at = COATED.get(player.getUUID());
			coated = at != null && at == now;
		}
		float amount = coated ? coat(player, living, source, damage) : damage;
		float swing = first ? SWINGS.getOrDefault(player, 1.0F) : 0.5F;
		// A full swing on an opened foe is a finisher: it adds a share of what the foe has lost to the blow itself (armour and every
		// defence still have their say), and its method's strike plays once it lands (see Stance).
		double extra = Stance.finisher(player, living, swing, first);
		float before = living.getHealth();
		boolean critical = first && critical(player);
		boolean hurtIt = hurt.apply((float) (amount + extra));
		if (first && coated) {
			Aura.spend(player, AuraRules.COAT_COST, "coat");
		}
		boolean finisher = Stance.finished(player, living, hurtIt);
		if (hurtIt) {
			if (coated && !finisher) {
				felt(player, living, swing, critical, first);
			}
			float taken = Math.max(0, before - Math.max(0, living.getHealth()));
			// A training dummy heals at once: what the blow dealt is what it's measured by there.
			landed(player, living, living instanceof TrainingDummy ? Math.max(taken, amount) : taken, swing, coated, false);
			if (!finisher) {
				// The blow wears the foe's stance (a finisher's blow ends its opening instead).
				Stance.blow(player, living, amount, swing, critical, coated, first);
			}
		}
		Momentum.hit(player, living, swing, critical, first, hurtIt, !living.isAlive() || living.isDeadOrDying());
		return hurtIt;
	}

	/** Whether a player's blow now is a critical one, as vanilla judges it (falling, a full swing, not climbing, swimming or running). */
	private static boolean critical(ServerPlayer player) {
		return player.fallDistance > 0 && !player.onGround() && !player.onClimbable() && !player.isInWater() && !player.isPassenger()
			&& !player.isSprinting() && SWINGS.getOrDefault(player, 0.0F) >= AuraRules.FULL_SWING;
	}

	/**
	 * How a coated blow feels: a flash where it bit, the moment held for the striker (and a struck player) on a full swing, and the
	 * method's sounds: its blow landing for everyone, its blade's swing for everyone but the swinger (whose own client played it at
	 * once). A sweep's other blows only flash.
	 */
	private static void felt(ServerPlayer player, LivingEntity target, float swing, boolean critical, boolean first) {
		AuraFxRules.Weight weight = first ? AuraFxRules.blow(swing, critical) : AuraFxRules.Weight.LIGHT;
		AuraFx.impact(player, target, weight);
		if (first) {
			float volume = weight == AuraFxRules.Weight.LIGHT ? 0.45F : weight == AuraFxRules.Weight.HEAVY ? 0.9F : 0.7F;
			AuraFx.sound(player, AuraFx.Sound.IMPACT, volume, weight == AuraFxRules.Weight.HEAVY ? 0.9F : 1.0F);
			AuraFx.soundForOthers(player, AuraFx.Sound.SWING, 0.5F, 1.0F);
		}
	}

	private static boolean harmable(ServerPlayer player, LivingEntity target) {
		return !(target instanceof Player other) || player.canHarmPlayer(other);
	}

	/** What a coated blow lands at: the coat, the element, held to the cap against a player, and the Edge's bite. */
	static float coat(ServerPlayer player, LivingEntity target, DamageSource source, float damage) {
		WildercordConfig.AuraSettings settings = Config.get().aura();
		double bonus = 1.0 + settings.coatBonus() * settings.damageScale();
		bonus *= AuraElements.bonus(player, target, source, Aura.element(player));
		// A foe a Gale swordsman's Updraft threw, still in the air: the juggle (one more bonus, under the cap against a player).
		bonus *= dev.wildercord.aura.arts.ArtWards.juggle(player, target);
		// Awakened: a little harder (half that against a player, and that too under their cap and the PvP scale).
		bonus *= Awakening.damage(player, target);
		double amount = damage * againstPlayer(target, bonus);
		if (Aura.stage(player) >= AuraRules.EDGE && !source.is(DamageTypeTags.BYPASSES_ARMOR) && amount > 0) {
			float after = CombatRules.getDamageAfterAbsorb(target, (float) amount, source, target.getArmorValue(),
				(float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			// Against another player the bite through armour is a bonus like the rest: scaled as they are.
			double pierce = target instanceof Player ? AuraRules.EDGE_PIERCE * settings.pvpScale() : AuraRules.EDGE_PIERCE;
			amount = AuraRules.pierced(amount, pierce, after / amount);
		}
		return (float) amount;
	}

	/** An aura bonus against a player: held to the spell-defence cap, then scaled by the aura PvP scale. Unchanged against anything else. */
	public static double againstPlayer(LivingEntity target, double bonus) {
		if (!(target instanceof Player)) {
			return bonus;
		}
		double held = AuraRules.capBonus(bonus, Config.get().defence().maxBonus());
		return 1.0 + (held - 1.0) * Config.get().aura().pvpScale();
	}

	/**
	 * Aura projected off the blade (the slash, a spark) landing on {@code target} at {@code damage}: the element as for a
	 * coated blow, then against a player the spell defences (armour, Warding, the cap and the spellguard) and the PvP scale.
	 * Returns what it took.
	 */
	public static float projected(ServerPlayer player, LivingEntity target, double damage, boolean answer) {
		return projected(player, target, damage, 1.0, answer);
	}

	/**
	 * Projected aura with a bonus of its own on top of the element (an aura-forged glaive's slash): the two together are held
	 * to the spell-defence cap against a player.
	 */
	public static float projected(ServerPlayer player, LivingEntity target, double damage, double extra, boolean answer) {
		return projected(player, target, damage, extra, answer, Double.MAX_VALUE);
	}

	/** What the last projected strike dealt before its target's defences (the server thread's; an art keeps its PvP tally with it). */
	private static double lastAmount;
	/** Whether the projected strike landing now is an art's ({@link #artStrike}): an art wears stance itself, by its own weight. */
	private static boolean artStrike;

	/**
	 * An art's strike ({@code ArtKit.Hits}): projected aura held to {@code playerCap} against a player, as {@link #projected}, the art
	 * itself answering for the stance it wears. Returns what it took.
	 */
	public static float artStrike(ServerPlayer player, LivingEntity target, double damage, double playerCap, boolean answer) {
		boolean outer = artStrike;
		artStrike = true;
		try {
			return projected(player, target, damage, 1.0, answer, playerCap);
		} finally {
			artStrike = outer;
		}
	}

	public static double lastAmount() {
		return lastAmount;
	}

	/**
	 * Projected aura held, against another player, to {@code playerCap} after their bonuses' cap and the PvP scale (before their
	 * armour and spell defences): an art's share of what it may deal one player ({@code aura.ArtRules#PVP_ART_CAP}).
	 */
	public static float projected(ServerPlayer player, LivingEntity target, double damage, double extra, boolean answer, double playerCap) {
		ServerLevel level = player.level();
		DamageSource source = level.damageSources().source(Aura.DAMAGE, player, player);
		double bonus = AuraElements.bonus(player, target, source, Aura.element(player)) * Math.max(0, extra);
		double amount = damage;
		if (target instanceof Player) {
			amount *= AuraRules.capBonus(bonus, Config.get().defence().maxBonus()) * Config.get().aura().pvpScale();
			amount = Math.min(amount, Math.max(0, playerCap));
			if (amount <= 0) {
				lastAmount = 0;
				return 0;
			}
		} else {
			amount *= bonus;
		}
		lastAmount = amount;
		float before = target.getHealth();
		// Aura off the blade lands through a foe's moment of invulnerability, as a spell does.
		dev.wildercord.cast.Effects.readyToHurt(target);
		float dealt = (float) amount;
		boolean hurt = target instanceof Player ? SpellDefence.hurt(level, target, source, dealt) : target.hurtServer(level, source, dealt);
		float taken = Math.max(0, before - Math.max(0, target.getHealth()));
		if (hurt && answer) {
			landed(player, target, taken, 1.0F, true, true);
		}
		if (hurt && !artStrike) {
			// Aura off the blade that isn't an art (a slash, a spark) wears a foe's stance a little.
			Stance.slash(player, target, amount);
		}
		return taken;
	}

	// ------------------------------------------------------------------ what a blow gives back

	/**
	 * A blow (or a projected strike) that took {@code taken} from {@code target}: aura back, experience, the method's passive,
	 * and the trials.
	 */
	static void landed(ServerPlayer player, LivingEntity target, float taken, float swing, boolean coated, boolean projected) {
		Aura.fighting(player);
		boolean killed = !target.isAlive() || target.isDeadOrDying();
		boolean practice = target instanceof TrainingDummy || player.level().dimension() == PracticeRoom.DIMENSION;
		double worth = practice ? 1.0 : worth(player, target);
		if (worth <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		String kind = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
		boolean boss = Spirits.isBoss(target);
		double repetition = boss || practice ? 1.0 : repeat(player, target.blockPosition(), kind, now);
		if (!projected) {
			Aura.gain(player, AuraRules.hitGain(taken, swing, repetition, practice), "hit");
		}
		if (swing >= AuraRules.FULL_SWING) {
			double share = taken / Math.max(1F, target.getMaxHealth());
			double xp = AuraRules.strike(worth, share, killed) * repetition;
			if (!practice) {
				xp *= moment(player, now);
			}
			AuraExperience.earn(player, Math.min(AuraRules.MAX_PER_STRIKE, xp), practice);
		}
		if (coated && !practice || coated && target instanceof TrainingDummy) {
			flavour(player, target, taken, now);
			// An elemental strike may leave its element's reaction mark, for a mage's spell to set off.
			AuraMarks.strike(player, target);
		}
		if (!projected && !practice) {
			// In the striker's Dominion a blow chains once to another foe inside.
			AuraDominion.chain(player, target, taken);
		}
		AuraBreakthroughs.struck(player, target, killed, practice);
	}

	/** What a foe is worth to learn from (0 for anything that isn't a foe: animals, pets, allies). */
	static double worth(ServerPlayer player, LivingEntity target) {
		if (target instanceof Player) {
			return Targets.canHarm(player, target) ? AuraRules.worth(true, false, false, 0, 0) : 0;
		}
		boolean foe = target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == player
			|| target instanceof NeutralMob neutral && neutral.getTarget() == player;
		if (!foe || Targets.playerPet(target)) {
			return 0;
		}
		return AuraRules.worth(false, Spirits.isBoss(target), target.hasAttached(WildercordAttachments.RUNEBOUND), target.getMaxHealth(),
			player.getMaxHealth());
	}

	/** What repetition leaves of a blow on this kind of foe in this place (the same 16-block cube), remembering it. */
	private static double repeat(ServerPlayer player, BlockPos pos, String kind, long now) {
		Map<Long, double[]> places = PLACES.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		int s = Integer.numberOfTrailingZeros(MasteryRules.PLACE);
		long where = BlockPos.asLong(pos.getX() >> s, pos.getY() >> s, pos.getZ() >> s) * 31 + kind.hashCode();
		double[] seen = places.computeIfAbsent(where, k -> new double[] {0, now});
		// Blows come far faster than casts: each counts a quarter of a cast toward repetition.
		double recent = MasteryRules.forget(seen[0], now - (long) seen[1]);
		seen[0] = recent + 0.25;
		seen[1] = now;
		if (places.size() > 64) {
			places.entrySet().removeIf(e -> MasteryRules.forget(e.getValue()[0], now - (long) e.getValue()[1]) < 0.05);
		}
		return MasteryRules.repetition(recent);
	}

	/** The moment, as spell mastery reads it (low health, a crowd, a boss near, a dungeon), worked out at most once a second. */
	private static double moment(ServerPlayer player, long now) {
		double[] cached = MOMENT.get(player.getUUID());
		if (cached != null && now - (long) cached[1] < 20) {
			return cached[0];
		}
		ServerLevel level = player.level();
		int foes = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(MasteryRules.CROWD_RADIUS),
			m -> m.isAlive() && (m instanceof Enemy || m.getTarget() == player)).size();
		boolean boss = !level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MasteryRules.BOSS_RADIUS),
			e -> e.isAlive() && Spirits.isBoss(e)).isEmpty();
		double m = MasteryRules.situation(player.getHealth() / Math.max(1F, player.getMaxHealth()), foes, boss, Mastery.inDungeon(level, player.blockPosition()));
		MOMENT.put(player.getUUID(), new double[] {m, now});
		return m;
	}

	// ------------------------------------------------------------------ the methods' passives

	private static void flavour(ServerPlayer player, LivingEntity target, float taken, long now) {
		BreathingMethod method = Aura.method(player).orElse(null);
		if (method == null || !target.isAlive() && method.flavour() != BreathingMethod.Flavour.MEND && method.flavour() != BreathingMethod.Flavour.LEECH) {
			return;
		}
		int stage = Aura.stage(player);
		ServerLevel level = player.level();
		switch (method.flavour()) {
			case IGNITE -> {
				if (!target.fireImmune() && level.getRandom().nextDouble() < AuraRules.emberChance(stage)) {
					target.igniteForTicks(Math.max(target.getRemainingFireTicks(), AuraRules.emberTicks(stage)));
				}
			}
			case CHILL -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AuraRules.rimeTicks(stage), 0, false, true), player);
			case SPARK -> {
				if (level.getRandom().nextDouble() < AuraRules.thunderChance(stage)) {
					spark(player, target, taken);
				}
			}
			case MEND -> {
				Long last = MENDED.get(player.getUUID());
				if (last == null || now - last >= AuraRules.VERDANT_REST) {
					MENDED.put(player.getUUID(), now);
					player.heal((float) AuraRules.verdantHeal(stage));
				}
			}
			case PULL -> {
				double pull = AuraRules.hollowPull(stage);
				Vec3 c = target.position();
				for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(AuraRules.HOLLOW_RADIUS),
						e -> e instanceof LivingEntity && e != player && Targets.canHarm(player, e) && !Spirits.isBoss(e))) {
					Vec3 toward = c.subtract(e.position());
					if (toward.lengthSqr() > 0.25) {
						e.push(toward.normalize().scale(pull));
						e.syncVelocity = true;
					}
				}
			}
			case LEECH -> {
				double drink = Math.min(AuraRules.CRIMSON_MAX, taken * AuraRules.crimsonLeech(stage));
				if (drink > 0 && Aura.aura(player) >= AuraRules.CRIMSON_COST) {
					Aura.spend(player, AuraRules.CRIMSON_COST, "leech");
					player.heal((float) drink);
				}
			}
			default -> {
				// Gale, Stone, Starlit and Hourglass give theirs all the time (see Aura's modifiers and gain).
			}
		}
	}

	/** Thunder: a spark leaps from the struck foe to the nearest other, carrying part of the blow as projected aura. */
	private static void spark(ServerPlayer player, LivingEntity from, float taken) {
		ServerLevel level = player.level();
		LivingEntity next = null;
		double best = AuraRules.THUNDER_REACH * AuraRules.THUNDER_REACH;
		for (Entity e : level.getEntities(from, from.getBoundingBox().inflate(AuraRules.THUNDER_REACH),
				e -> e instanceof LivingEntity && e != player && e.isAlive() && Targets.canHarm(player, e))) {
			double d = e.distanceToSqr(from);
			if (d < best) {
				best = d;
				next = (LivingEntity) e;
			}
		}
		if (next == null) {
			return;
		}
		int color = Aura.color(player);
		Light.ray(level, from.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), color, 0.08, 5);
		Light.ray(level, from.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), 0xFFFBE0, 0.03, 4);
		dev.wildercord.cast.feel.Feels.sound(level, next.getBoundingBox().getCenter(), "tell_zap", 0.6F, 1.2F);
		if (projected(player, next, Math.max(1.0, taken * AuraRules.THUNDER_SHARE) * Config.get().aura().damageScale(), false) > 0) {
			AuraFx.impact(player, next, AuraFxRules.Weight.LIGHT);
		}
	}

	// ------------------------------------------------------------------ Flow's sweep

	/** Whether a player's blow sweeps whatever their aura weapon is (Flow, coated). */
	public static boolean flowSweeps(Player player) {
		return player instanceof ServerPlayer && Aura.stage(player) >= AuraRules.FLOW && Aura.coated(player);
	}

	/** The sweep's reach round the struck foe: wider with Flow. */
	public static double sweepInflate(Player player, double vanilla) {
		return flowSweeps(player) ? Math.max(vanilla, AuraRules.FLOW_SWEEP_INFLATE) : vanilla;
	}

	/** How far from the player the sweep reaches, squared: further with Flow. */
	public static double sweepRangeSq(Player player, double vanilla) {
		return flowSweeps(player) ? Math.max(vanilla, AuraRules.FLOW_SWEEP_RANGE * AuraRules.FLOW_SWEEP_RANGE) : vanilla;
	}

	/**
	 * A Flow sweep, seen: a wide, level trail of the aura's light round the front of the player, for everyone, the sweeper too
	 * (drawn low and thin in their own first-person view), and a broader whoosh of the method's blade under the swing's own.
	 */
	public static void sweep(Player player) {
		if (player instanceof ServerPlayer server && flowSweeps(player)) {
			AuraFx.swept(server);
		}
	}
}
