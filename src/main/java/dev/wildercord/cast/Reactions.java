package dev.wildercord.cast;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Element reactions: effects leave short-lived marks on what they hit, and a later effect of
 * the right element that meets a mark sets off a bonus.
 * <ul>
 *   <li><b>Shatter</b>: fire damage on a frozen target deals +60% and bursts the ice.</li>
 *   <li><b>Conduct</b>: storm damage on a wet target (water or rain) deals +50% and arcs to two more enemies.</li>
 *   <li><b>Wildfire</b>: fire on a target just thrown by wind spreads flames to enemies around it.</li>
 *   <li><b>Implode</b>: a blast where enemies were just pulled together grows 50% wider and hits 30% harder.</li>
 * </ul>
 */
public final class Reactions {
	private Reactions() {}

	public enum Mark {
		FROZEN(80),
		WINDSWEPT(50),
		PULLED(50),
		/** Left by a popped Bubble: counts as wet for Conduct. */
		SOAKED(100),
		/** Resonance's cursed mark. */
		RESONANT(200);

		final int ticks;

		Mark(int ticks) {
			this.ticks = ticks;
		}
	}

	private static final Map<UUID, Map<Mark, Long>> MARKS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_CALLOUT = new ConcurrentHashMap<>();

	public static void mark(Entity target, Mark mark) {
		mark(target, mark, mark.ticks);
	}

	public static void mark(Entity target, Mark mark, int ticks) {
		long until = target.level().getGameTime() + ticks;
		MARKS.computeIfAbsent(target.getUUID(), k -> new EnumMap<>(Mark.class)).merge(mark, until, Math::max);
	}

	public static boolean has(Entity target, Mark mark) {
		Map<Mark, Long> marks = MARKS.get(target.getUUID());
		if (marks == null) {
			return false;
		}
		Long until = marks.get(mark);
		return until != null && until >= target.level().getGameTime();
	}

	public static void clear(Entity target, Mark mark) {
		Map<Mark, Long> marks = MARKS.get(target.getUUID());
		if (marks != null) {
			marks.remove(mark);
		}
	}

	/** Called for fire-element damage: returns the damage multiplier after Shatter / Wildfire. */
	public static double fire(Cast cast, LivingEntity target) {
		double multiplier = 1.0;
		ServerLevel level = cast.level;
		if (has(target, Mark.FROZEN)) {
			clear(target, Mark.FROZEN);
			target.setTicksFrozen(0);
			multiplier *= 1.6;
			Vec3 c = target.getBoundingBox().getCenter();
			Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), c, 26, 0.35);
			Sigils.flash(level, c, 0xFFBFEFFF, 2.2F);
			Fx.sound(level, c, SoundEvents.GLASS_BREAK, 1.0F, 0.7F);
			callout(cast, "shatter", 0x8CDCFF);
		}
		if (has(target, Mark.WINDSWEPT)) {
			clear(target, Mark.WINDSWEPT);
			for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(3.0), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity other = (LivingEntity) e;
				other.igniteForSeconds(4);
				Effects.hurt(cast, other, level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3);
				Vfx.stream(level, target.getBoundingBox().getCenter(), other.getBoundingBox().getCenter(), Vfx.theme("fire"), 4);
			}
			Vfx.radial(level, ParticleTypes.FLAME, target.getBoundingBox().getCenter(), 20, 0.3);
			callout(cast, "wildfire", 0xF06E32);
		}
		return multiplier;
	}

	/** Called for storm-element damage: returns the damage multiplier after Conduct. */
	public static double storm(Cast cast, LivingEntity target) {
		if (!target.isInWaterOrRain() && !has(target, Mark.SOAKED)) {
			return 1.0;
		}
		ServerLevel level = cast.level;
		int arcs = 0;
		for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(5.0), e -> Targets.canHarm(cast.caster, e))) {
			if (arcs++ >= 2) {
				break;
			}
			LivingEntity other = (LivingEntity) e;
			Vfx.shockArc(level, target.getBoundingBox().getCenter(), other.getBoundingBox().getCenter());
			Effects.hurt(cast, other, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 4);
		}
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, target.getBoundingBox().getCenter(), 16, 0.4);
		Vfx.radial(level, ParticleTypes.SPLASH, target.getBoundingBox().getCenter(), 10, 0.2);
		callout(cast, "conduct", 0xFFE650);
		return 1.5;
	}

	/** Called for blasts: returns the radius multiplier after Implode (damage bonus is radius-based too). */
	public static double blast(Cast cast, Vec3 center, double radius) {
		boolean pulled = false;
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			if (has(e, Mark.PULLED)) {
				clear(e, Mark.PULLED);
				pulled = true;
			}
		}
		if (!pulled) {
			return 1.0;
		}
		Vfx.radial(cast.level, ParticleTypes.REVERSE_PORTAL, center, 30, 0.5);
		Vfx.shockwave(cast.level, center.subtract(0, 0.8, 0), radius * 1.6, Vfx.theme("void"), 5);
		callout(cast, "implode", 0xB45AF0);
		return 1.5;
	}

	/**
	 * Called for Repel: enemies just pulled in meet a force pushing out. Returns the damage
	 * multiplier after Collapse.
	 */
	public static double collapse(Cast cast, LivingEntity target) {
		if (!has(target, Mark.PULLED)) {
			return 1.0;
		}
		clear(target, Mark.PULLED);
		TechniqueVfx.collapse(cast.level, target.getBoundingBox().getCenter());
		callout(cast, "collapse", 0xB45AF0);
		return 2.0;
	}

	/** Tells the caster what they set off, at most once a second. */
	static void callout(Cast cast, String reaction, int color) {
		LivingEntity caster = cast.caster;
		Grimoire.reaction(caster, reaction);
		long now = cast.level.getGameTime();
		Long last = LAST_CALLOUT.get(caster.getUUID());
		if (last != null && now - last < 20) {
			return;
		}
		LAST_CALLOUT.put(caster.getUUID(), now);
		Casters.tell(caster, Component.translatable("reaction.wildercord." + reaction).withColor(color).withStyle(ChatFormatting.BOLD));
	}

	/** Drops marks for entities that no longer exist, so the table can't grow forever. */
	public static void sweep(long gameTime) {
		MARKS.values().removeIf(marks -> {
			marks.values().removeIf(until -> until < gameTime);
			return marks.isEmpty();
		});
	}
}
