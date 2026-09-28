package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
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
			shatterFx(level, target);
			Fx.sound(level, c, SoundEvents.GLASS_BREAK, 1.0F, 0.7F);
			callout(cast, "shatter", 0x8CDCFF);
		}
		if (has(target, Mark.WINDSWEPT)) {
			clear(target, Mark.WINDSWEPT);
			for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(3.0), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity other = (LivingEntity) e;
				other.igniteForSeconds(4);
				Effects.hurt(cast, other, level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3);
				wildfireLeap(level, target, other);
			}
			wildfireFx(level, target);
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
		conductFx(level, target);
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
		implodeFx(cast.level, center, radius);
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

	// ------------------------------------------------------------------ how they look

	/**
	 * Shatter: the ice bursts apart in a storm of shards, white rings snapping out through a flare of
	 * fire, a cracked frost seal on the ground and steam rising.
	 */
	private static void shatterFx(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.6, target.getBbWidth());
		double tilt = level.getRandom().nextDouble() * Math.PI * 2;
		Sigils.flash(level, c, 0xBFEFFF, 2.8F);
		ElementFx.heatFlare(level, c, 1.3);
		ElementFx.shards(level, c, 1.2 + w * 0.5, 12);
		ElementFx.ring(level, c, UP, 0xFFFFFF, 0.3, 2.4 + w, 0.07, 8);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, tilt), ElementFx.FROST.accent(), 0.2, 1.9 + w, 0.05, 9);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, tilt + Math.PI), ElementFx.FIRE.primary(), 0.2, 1.6 + w, 0.05, 10);
		ElementFx.flatSigil(level, target.position(), SigilOption.CRACKED, ElementFx.FROST.primary(), 1.2 + w, 24, 0.0);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), c, 18, 0.35);
		Vfx.emit(level, ParticleTypes.WHITE_SMOKE, c, 8, 0.4, 0.04);
	}

	/** Wildfire leaps: a streak of flame from the burning target to another, flames catching on it. */
	private static void wildfireLeap(ServerLevel level, LivingEntity from, LivingEntity to) {
		Vec3 a = from.getBoundingBox().getCenter();
		Vec3 b = to.getBoundingBox().getCenter();
		ElementFx.ray(level, a, b, ElementFx.FIRE.primary(), 0.09, 8);
		ElementFx.ray(level, a, b, ElementFx.FIRE.secondary(), 0.035, 7);
		ElementFx.flames(level, to.position(), Math.max(0.35, to.getBbWidth() * 0.6), to.getBbHeight(), 3);
		Vfx.stream(level, a, b, Vfx.theme("fire"), 2);
	}

	/** Wildfire: wind and fire together, a whirl of flame slashes spiralling up out of the target over a ring of fire. */
	private static void wildfireFx(ServerLevel level, LivingEntity target) {
		Vec3 base = target.position();
		Vec3 c = target.getBoundingBox().getCenter();
		ElementFx.heatFlare(level, c, 1.6);
		ElementFx.swirl(level, base.add(0, 0.1, 0), 1.1, target.getBbHeight() + 1.2, 5, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary());
		ElementFx.flameBurst(level, c, 1.3, 5);
		ElementFx.groundRing(level, base, ElementFx.FIRE.primary(), 0.3, 3.2, 0.1, 12);
		ElementFx.groundRing(level, base, ElementFx.WIND.secondary(), 0.2, 2.4, 0.04, 10);
		Vfx.radial(level, ParticleTypes.FLAME, c, 14, 0.3);
	}

	/** Conduct: lightning crawls over the wet target in a cage of short arcs, a ring of water bursting off it. */
	private static void conductFx(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.6, target.getBbWidth());
		Sigils.flash(level, c, ElementFx.STORM.secondary(), 2.2F);
		ElementFx.ring(level, c, UP, 0x4AA8FF, 0.2, 2.2 + w, 0.06, 8);
		ElementFx.ring(level, c, UP, ElementFx.STORM.primary(), 0.2, 1.6 + w, 0.04, 6);
		for (int i = 0; i < 3; i++) {
			Vec3 a = c.add(ElementFx.randomDir(level.getRandom()).scale(w * 0.8));
			Vec3 b = c.add(ElementFx.randomDir(level.getRandom()).scale(w * 0.8));
			ElementFx.bolt(level, a, b, 0.035, 0, 2);
		}
		ElementFx.sparks(level, c, 12, 0.4);
		Vfx.radial(level, ParticleTypes.SPLASH, c, 10, 0.2);
	}

	/** Implode: darkness falls in on the blast from far out, round a black core, before it goes off. */
	private static void implodeFx(ServerLevel level, Vec3 center, double radius) {
		ElementFx.implode(level, center, radius * 1.6, 7);
		ElementFx.blackCore(level, center, 0.5, 8);
		Vec3 floor = ElementFx.floor(level, center, radius + 1);
		if (floor != null) {
			ElementFx.groundRing(level, floor, ElementFx.dark(ElementFx.VOID.accent()), radius * 1.8, 0.3, 0.14, 10);
			ElementFx.groundRing(level, floor, ElementFx.VOID.primary(), radius * 1.9, 0.4, 0.04, 9);
		}
		Vfx.emit(level, ParticleTypes.PORTAL, center, 20, 0.1, radius * 0.8);
	}

	private static final Vec3 UP = new Vec3(0, 1, 0);

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
