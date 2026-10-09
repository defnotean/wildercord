package dev.wildercord.aura.arts;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.DuneRules;
import dev.wildercord.aura.IronRules;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.TideRules;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Vfx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tide, Iron and Dune's elements on a foe, shared by their passives, techniques, arts, awakenings and finishers (and their Sword
 * Masters): soaking and the current (Tide), armour sundered for a while (Iron), grit in the eyes and ground that swallows feet (Dune).
 * Everything here is held for players and bosses by the rules ({@link DuneRules#blind}, {@link DuneRules#sink}, {@link TideRules#current},
 * {@link IronRules#sunder}), and nothing it leaves is saved: a sunder is a transient modifier and a blindness an ordinary effect.
 */
public final class MethodsAFlavours {
	private MethodsAFlavours() {}

	public static final int FOAM = 0xD8FFF6;
	public static final int SPARK = 0xFF9A3C;
	public static final int GOLD = 0xF6E3A0;

	// ------------------------------------------------------------------ Tide

	/** Soaks {@code foe} for {@code ticks}: wet (for Conduct and fire), its fire put out, a moment slowed. */
	public static void soak(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!foe.isAlive() || ticks <= 0) {
			return;
		}
		Reactions.mark(foe, Reactions.Mark.WET, ticks);
		foe.clearFire();
		ArtKit.slow(player, foe, Math.max(10, ticks / 2), 0);
		ServerLevel level = (ServerLevel) foe.level();
		Vfx.emit(level, ParticleTypes.SPLASH, foe.getBoundingBox().getCenter(), 8, 0.3, 0.1);
		Vfx.emit(level, ParticleTypes.FALLING_WATER, foe.position().add(0, foe.getBbHeight(), 0), 4, 0.25, 0.0);
	}

	/** The current takes {@code foe}: pushed by {@code impulse}, further when it's soaked. */
	public static void current(LivingEntity foe, Vec3 impulse) {
		double length = impulse.length();
		if (length < 1.0E-4) {
			return;
		}
		double allowed = TideRules.current(length, Reactions.has(foe, Reactions.Mark.WET), foe instanceof Player, ArtKit.boss(foe));
		ArtKit.shove(foe, impulse.scale(allowed / length));
	}

	// ------------------------------------------------------------------ Iron

	private static final Identifier SUNDERED = Wildercord.id("iron_sunder");
	/** When each foe's sunder ends (game time), so a later, longer one isn't cut short by an earlier one's end. */
	private static final Map<UUID, Long> SUNDER_ENDS = new ConcurrentHashMap<>();

	/** Sunders {@code armour} of {@code foe}'s armour for {@code ticks} (held to {@link IronRules#SUNDER_MAX} in all). Returns what it took. */
	public static double sunder(ServerPlayer player, LivingEntity foe, double armour, int ticks) {
		if (!foe.isAlive() || armour <= 0 || ticks <= 0 || player != null && !ArtKit.harmable(player, foe)) {
			return 0;
		}
		AttributeInstance instance = foe.getAttribute(Attributes.ARMOR);
		if (instance == null) {
			return 0;
		}
		AttributeModifier current = instance.getModifier(SUNDERED);
		double sundered = current == null ? 0 : -current.amount();
		double took = IronRules.sunder(armour, sundered, instance.getValue());
		long now = foe.level().getGameTime();
		long end = Math.max(now + ticks, SUNDER_ENDS.getOrDefault(foe.getUUID(), 0L));
		SUNDER_ENDS.put(foe.getUUID(), end);
		if (took > 0) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(SUNDERED, -(sundered + took), AttributeModifier.Operation.ADD_VALUE));
		}
		Scheduler.later(ticks + 1, () -> unsunder(foe));
		ServerLevel level = (ServerLevel) foe.level();
		Vfx.emit(level, ParticleTypes.CRIT, foe.getBoundingBox().getCenter(), 10, 0.35, 0.25);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), foe.getBoundingBox().getCenter(), 6, 0.3, 0.1);
		return took;
	}

	/** The armour sundered from {@code foe} now (points). */
	public static double sundered(LivingEntity foe) {
		AttributeInstance instance = foe.getAttribute(Attributes.ARMOR);
		AttributeModifier current = instance == null ? null : instance.getModifier(SUNDERED);
		return current == null ? 0 : -current.amount();
	}

	private static void unsunder(LivingEntity foe) {
		Long end = SUNDER_ENDS.get(foe.getUUID());
		if (end != null && foe.level().getGameTime() < end) {
			return;
		}
		SUNDER_ENDS.remove(foe.getUUID());
		AttributeInstance instance = foe.getAttribute(Attributes.ARMOR);
		if (instance != null) {
			instance.removeModifier(SUNDERED);
		}
	}

	/** The heavy guard: Resistance and knockback resistance for {@code ticks}. */
	public static void bulwark(ServerPlayer player, int ticks, int resist) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, resist, false, false, true));
		ArtWards.harden(player, ticks);
	}

	// ------------------------------------------------------------------ Dune

	/** Throws grit in {@code foe}'s eyes: blinded (held for players and bosses); a creature loses its target and stumbles. Returns the ticks. */
	public static int blind(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!foe.isAlive() || player != null && !ArtKit.harmable(player, foe)) {
			return 0;
		}
		int t = DuneRules.blind(ticks, foe instanceof Player, ArtKit.boss(foe));
		ServerLevel level = (ServerLevel) foe.level();
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), foe.getEyePosition(), 10, 0.3, 0.02);
		if (t <= 0) {
			return 0;
		}
		foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, t, 0, false, true), player);
		if (foe instanceof Mob mob) {
			mob.setTarget(null);
			mob.getNavigation().stop();
			mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, t, 1, false, true), player);
		}
		return t;
	}

	/** Sinks {@code foe} into the sand: slowed {@code depth} deep (held for players, never a boss), a little drawn down. */
	public static boolean sink(ServerPlayer player, LivingEntity foe, int ticks, int depth) {
		if (!foe.isAlive() || player != null && !ArtKit.harmable(player, foe)) {
			return false;
		}
		int amp = DuneRules.sink(depth, foe instanceof Player, ArtKit.boss(foe));
		if (amp < 0) {
			return false;
		}
		foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, amp, false, true), player);
		if (!(foe instanceof Player)) {
			// A creature's feet are taken: what way it was going stops short (a player's own client moves them; Slowness is enough).
			Vec3 v = foe.getDeltaMovement();
			foe.setDeltaMovement(v.x * 0.4, Math.min(v.y, -0.08), v.z * 0.4);
		}
		ServerLevel level = (ServerLevel) foe.level();
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), foe.position().add(0, 0.1, 0), 8, 0.35, 0.05);
		return true;
	}

	// ------------------------------------------------------------------ the passives (a coated blow)

	/** Whether {@code flavour} is one of these three. */
	public static boolean ours(dev.wildercord.aura.BreathingMethod.Flavour flavour) {
		return flavour == dev.wildercord.aura.BreathingMethod.Flavour.CURRENT || flavour == dev.wildercord.aura.BreathingMethod.Flavour.FORGE
			|| flavour == dev.wildercord.aura.BreathingMethod.Flavour.GRIT;
	}

	/** A coated blow of {@code player}'s on {@code target}: Tide pushes and soaks, Iron cracks armour, Dune may throw grit. */
	public static void passive(ServerPlayer player, LivingEntity target, dev.wildercord.aura.BreathingMethod.Flavour flavour, int stage) {
		switch (flavour) {
			case CURRENT -> {
				Vec3 away = target.position().subtract(player.position());
				away = new Vec3(away.x, 0, away.z);
				if (away.lengthSqr() > 1.0E-4) {
					current(target, away.normalize().scale(TideRules.push(stage)).add(0, 0.05, 0));
				}
				Reactions.mark(target, Reactions.Mark.WET, TideRules.soak(stage));
				target.clearFire();
			}
			case FORGE -> sunder(player, target, IronRules.crack(stage), IronRules.CRACK_TICKS);
			case GRIT -> {
				if (player.level().getRandom().nextDouble() < DuneRules.gritChance(stage)) {
					blind(player, target, DuneRules.GRIT_TICKS);
				}
			}
			default -> { }
		}
	}

	// ------------------------------------------------------------------ the techniques' element

	/** A technique's element on a foe it struck, for Tide, Iron and Dune ({@code k} its flavour's scale, {@code heart} where the stroke centres). */
	public static void technique(ServerPlayer player, LivingEntity foe, Vec3 heart, TechniqueRules.Flavour fl, double k, boolean infused) {
		ArtLight world = ArtLight.world(player);
		Vec3 centre = foe.getBoundingBox().getCenter();
		int color = ArtKit.color(player);
		switch (fl.of()) {
			case CURRENT -> {
				Vec3 away = foe.position().subtract(heart);
				away = new Vec3(away.x, 0, away.z);
				away = away.lengthSqr() < 1.0E-4 ? ArtKit.flat(player) : away.normalize();
				current(foe, away.scale(fl.current() * k).add(0, 0.08, 0));
				soak(player, foe, (int) Math.round(TechniqueRules.CURRENT_SOAK * k));
				world.ring(centre, ArtKit.UP, FOAM, 0.2, infused ? 1.2 : 0.8, 0.06, 7);
			}
			case FORGE -> {
				sunder(player, foe, fl.sunder() * k, 80);
				world.flash(centre, SPARK, infused ? 1.1F : 0.7F);
			}
			case GRIT -> {
				blind(player, foe, (int) Math.round(fl.grit() * k));
				world.bare().flash(centre, color, infused ? 1.0F : 0.7F);
			}
			default -> { }
		}
	}
}
