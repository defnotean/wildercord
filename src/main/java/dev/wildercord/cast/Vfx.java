package dev.wildercord.cast;

import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Spell visuals. Every element has a theme (two colours, a mote, a spark, a cast sound and an
 * impact sound); shapes and effects are built from animated primitives on top of it. All of it
 * is vanilla particles sent from the server, so every player sees the same show and no
 * resource pack is needed.
 */
public final class Vfx {
	private Vfx() {}

	public record Theme(int primary, int secondary, ParticleOptions mote, ParticleOptions spark, SoundEvent cast, SoundEvent impact) {
		DustParticleOptions dust(float scale) {
			return new DustParticleOptions(primary, scale);
		}

		DustColorTransitionOptions fade(float scale) {
			return new DustColorTransitionOptions(primary, secondary, scale);
		}

		TrailParticleOption trail(Vec3 target, int duration) {
			return new TrailParticleOption(target, primary, duration);
		}

		ColorParticleOption flash() {
			return ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | primary);
		}

		SpellParticleOption sparkle() {
			return SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFF000000 | primary, 1.0F);
		}
	}

	private static final Theme FIRE = new Theme(0xF06E32, 0xFFD060, ParticleTypes.FLAME, ParticleTypes.LAVA, SoundEvents.BLAZE_SHOOT, SoundEvents.FIRECHARGE_USE);
	private static final Theme FROST = new Theme(0x8CDCFF, 0xFFFFFF, ParticleTypes.SNOWFLAKE, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), SoundEvents.SNOW_GOLEM_SHOOT, SoundEvents.GLASS_BREAK);
	private static final Theme STORM = new Theme(0xFFE650, 0xFFFFFF, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD, SoundEvents.BEACON_POWER_SELECT, SoundEvents.LIGHTNING_BOLT_IMPACT);
	private static final Theme WIND = new Theme(0xC8F0DC, 0xFFFFFF, ParticleTypes.SMALL_GUST, ParticleTypes.CLOUD, SoundEvents.BREEZE_SHOOT, SoundEvents.BREEZE_JUMP);
	private static final Theme EARTH = new Theme(0xB48C5A, 0x6E5436, ParticleTypes.CRIT, new ItemParticleOption(ParticleTypes.ITEM, Items.COBBLESTONE), SoundEvents.HEAVY_CORE_PLACE, SoundEvents.MACE_SMASH_GROUND);
	private static final Theme LIFE = new Theme(0x6EDC64, 0xE8FFB0, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.TOTEM_OF_UNDYING, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundEvents.AMETHYST_CLUSTER_PLACE);
	private static final Theme VOID = new Theme(0xB45AF0, 0x3A1060, ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL, SoundEvents.SHULKER_SHOOT, SoundEvents.ENDER_EYE_DEATH);
	private static final Theme ARCANE = new Theme(0xE678DC, 0xFFD8FA, ParticleTypes.ENCHANT, ParticleTypes.ENCHANTED_HIT, SoundEvents.EVOKER_PREPARE_ATTACK, SoundEvents.AMETHYST_BLOCK_BREAK);
	private static final Theme TIME = new Theme(0xF2D98A, 0xFFFFFF, ParticleTypes.WAX_OFF, ParticleTypes.END_ROD, SoundEvents.BELL_BLOCK, SoundEvents.AMETHYST_BLOCK_CHIME);
	private static final Theme BLOOD = new Theme(0xD2283C, 0x5A0A14, ParticleTypes.CRIMSON_SPORE, new DustParticleOptions(0x8A0A1A, 1.1F), SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_CRIT);
	private static final Theme SHAPE = new Theme(RuneColors.SHAPE, 0xC8FFF8, ParticleTypes.GLOW, ParticleTypes.END_ROD, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundEvents.AMETHYST_BLOCK_BREAK);

	public static Theme theme(String element) {
		return switch (element) {
			case "fire" -> FIRE;
			case "frost" -> FROST;
			case "storm" -> STORM;
			case "wind" -> WIND;
			case "earth" -> EARTH;
			case "life" -> LIFE;
			case "void" -> VOID;
			case "arcane" -> ARCANE;
			case "time" -> TIME;
			case "blood" -> BLOOD;
			default -> SHAPE;
		};
	}

	public static Theme theme(RuneDef rune) {
		return theme(rune.element());
	}

	/** A group's look: its first effect's element. */
	public static Theme theme(SpellPlan.Group g) {
		return g.effects.isEmpty() ? SHAPE : theme(g.effects.getFirst().effect);
	}

	// ------------------------------------------------------------------ primitives

	public static void emit(ServerLevel level, ParticleOptions p, Vec3 at, int count, double spread, double speed) {
		Fx.send(level, p, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	/** One particle flying in {@code dir} (count 0 makes the offset a velocity). */
	static void fling(ServerLevel level, ParticleOptions p, Vec3 at, Vec3 dir, double speed) {
		Fx.send(level, p, at.x, at.y, at.z, 0, dir.x, dir.y, dir.z, speed);
	}

	/** Particles thrown outward from a point in every direction. */
	public static void radial(ServerLevel level, ParticleOptions p, Vec3 center, int count, double speed) {
		for (int i = 0; i < count; i++) {
			double y = 1 - (i + 0.5) * 2.0 / count;
			double r = Math.sqrt(Math.max(0, 1 - y * y));
			double a = i * 2.39996323;
			fling(level, p, center, new Vec3(Math.cos(a) * r, y, Math.sin(a) * r), speed);
		}
	}

	static void ring(ServerLevel level, ParticleOptions p, Vec3 center, double radius, int points) {
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			Fx.send(level, p, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	/** An expanding ring on the ground, drawn over {@code ticks} ticks. */
	public static void shockwave(ServerLevel level, Vec3 center, double radius, Theme theme, int ticks) {
		for (int t = 0; t < ticks; t++) {
			double r = radius * (t + 1) / ticks;
			float scale = 1.6F - 0.9F * t / ticks;
			int points = (int) Math.max(12, r * 9);
			Scheduler.later(t + 1, () -> ring(level, new DustColorTransitionOptions(theme.primary, theme.secondary, scale), center.add(0, 0.12, 0), r, points));
		}
	}

	/** Two strands spiralling up around a point over {@code ticks} ticks. */
	static void helix(ServerLevel level, Vec3 base, double radius, double height, Theme theme, int ticks) {
		for (int t = 0; t < ticks; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				for (int strand = 0; strand < 2; strand++) {
					double a = tick * 0.9 + strand * Math.PI;
					double y = height * tick / ticks;
					Vec3 p = base.add(Math.cos(a) * radius, y, Math.sin(a) * radius);
					emit(level, theme.fade(1.1F), p, 1, 0.0, 0.0);
					if (tick % 3 == 0) {
						emit(level, theme.mote, p, 1, 0.02, 0.0);
					}
				}
			});
		}
	}

	/** Particles streaming from {@code from} into {@code to}. */
	public static void stream(ServerLevel level, Vec3 from, Vec3 to, Theme theme, int count) {
		for (int i = 0; i < count; i++) {
			Vec3 start = from.add((level.getRandom().nextDouble() - 0.5) * 0.8, (level.getRandom().nextDouble() - 0.5) * 0.8, (level.getRandom().nextDouble() - 0.5) * 0.8);
			Fx.send(level, theme.trail(to, 10 + level.getRandom().nextInt(8)), start.x, start.y, start.z, 1, 0, 0, 0, 0);
		}
	}

	// ------------------------------------------------------------------ casting

	/**
	 * A spinning magic circle under the caster: two rings, six rune marks, and glyphs rising
	 * from it. Played on every cast.
	 */
	public static void castCircle(ServerPlayer caster, Theme theme) {
		ServerLevel level = caster.level();
		Vec3 feet = caster.position().add(0, 0.08, 0);
		for (int t = 0; t < 8; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double spin = tick * 0.35;
				ring(level, theme.dust(0.9F), feet, 1.25, 22);
				if (tick % 2 == 0) {
					ring(level, new DustParticleOptions(theme.secondary, 0.7F), feet, 0.75, 12);
				}
				for (int i = 0; i < 6; i++) {
					double a = spin + Math.PI * 2 * i / 6;
					Vec3 mark = feet.add(Math.cos(a) * 1.0, 0.02, Math.sin(a) * 1.0);
					emit(level, theme.dust(1.5F), mark, 1, 0.0, 0.0);
					if (tick == 0) {
						fling(level, ParticleTypes.ENCHANT, mark, new Vec3(0, 1, 0), 0.6);
					}
				}
			});
		}
		Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.7)).add(0, -0.3, 0);
		emit(level, theme.flash(), hand, 1, 0.0, 0.0);
		emit(level, theme.sparkle(), hand, 6, 0.15, 0.0);
		Fx.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.6F);
		Fx.sound(level, caster.position(), theme.cast, 0.55F, 1.2F);
	}

	// ------------------------------------------------------------------ shapes

	/** Self: a helix rising around you. */
	public static void self(ServerPlayer caster, Theme theme) {
		helix(caster.level(), caster.position(), 0.7, 2.0, theme, 10);
		radial(caster.level(), theme.mote, caster.position().add(0, 1, 0), 8, 0.08);
	}

	/** One tick of a bolt's flight: a bright core, a coloured streak behind it, and a spiral. */
	public static void boltTick(ServerLevel level, Vec3 from, Vec3 to, Theme theme, int age) {
		Vec3 delta = to.subtract(from);
		for (int i = 0; i < 3; i++) {
			Vec3 p = from.add(delta.scale(i / 3.0));
			emit(level, theme.dust(1.8F), p, 1, 0.02, 0.0);
		}
		// The streak: trail particles fly from just behind the bolt to its nose.
		Fx.send(level, theme.trail(to, 6), from.x, from.y, from.z, 2, 0.03, 0.03, 0.03, 0);
		Vec3 dir = delta.lengthSqr() > 1.0E-6 ? delta.normalize() : new Vec3(0, 0, 1);
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1.0E-4) {
			side = new Vec3(1, 0, 0);
		}
		side = side.normalize();
		Vec3 up = side.cross(dir).normalize();
		double a = age * 1.2;
		Vec3 swirl = to.add(side.scale(Math.cos(a) * 0.22)).add(up.scale(Math.sin(a) * 0.22));
		emit(level, new DustParticleOptions(theme.secondary, 0.8F), swirl, 1, 0.0, 0.0);
		if (age % 2 == 0) {
			emit(level, theme.mote, to, 1, 0.05, 0.01);
		}
	}

	/** Where a bolt, beam or touch lands: a flash, a burst of sparks and a small shockwave. */
	public static void impact(ServerLevel level, Vec3 at, Theme theme, double size) {
		emit(level, theme.flash(), at, 1, 0.0, 0.0);
		radial(level, theme.spark, at, (int) (10 * size), 0.18 * size);
		radial(level, theme.mote, at, (int) (8 * size), 0.1 * size);
		emit(level, theme.sparkle(), at, (int) (6 * size), 0.25 * size, 0.0);
		shockwave(level, at.subtract(0, 0.4, 0), 0.9 * size, theme, 3);
		Fx.sound(level, at, theme.impact, 0.7F, 1.1F);
	}

	/** A beam: a fading core line, a helix wrapped around it, lingering for a few ticks. */
	public static void beam(ServerLevel level, Vec3 from, Vec3 to, Theme theme) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		Vec3 dir = length > 1.0E-6 ? delta.scale(1 / length) : new Vec3(0, 0, 1);
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		Vec3 sideF = side;
		for (int t = 0; t < 3; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				int steps = (int) Math.min(140, length / 0.25);
				for (int i = 0; i <= steps; i++) {
					double d = length * i / steps;
					Vec3 p = from.add(dir.scale(d));
					if (tick == 0 || i % 2 == 0) {
						emit(level, theme.fade(tick == 0 ? 1.4F : 0.9F), p, 1, 0.0, 0.0);
					}
					if (tick == 0 && i % 2 == 0) {
						double a = d * 2.4;
						Vec3 h = p.add(sideF.scale(Math.cos(a) * 0.3)).add(up.scale(Math.sin(a) * 0.3));
						emit(level, new DustParticleOptions(theme.secondary, 0.7F), h, 1, 0.0, 0.0);
					}
				}
			});
		}
		emit(level, theme.flash(), from, 1, 0.0, 0.0);
		Fx.send(level, theme.trail(to, 8), from.x, from.y, from.z, 6, 0.08, 0.08, 0.08, 0);
		Fx.sound(level, from, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6F, 1.6F);
	}

	/** Burst: an expanding shell and a ground shockwave, with the element thrown outward. */
	public static void burst(ServerLevel level, Vec3 center, double radius, Theme theme) {
		emit(level, theme.flash(), center, 1, 0.0, 0.0);
		for (int t = 0; t < 4; t++) {
			double r = radius * (t + 1) / 4;
			int tick = t;
			Scheduler.later(t + 1, () -> {
				int points = (int) Math.min(90, Math.max(20, r * r * 3));
				for (int i = 0; i < points; i++) {
					double y = 1 - (i + 0.5) * 2.0 / points;
					double rr = Math.sqrt(Math.max(0, 1 - y * y));
					double a = i * 2.39996323 + tick;
					emit(level, theme.fade(1.4F - tick * 0.25F), center.add(Math.cos(a) * rr * r, y * r, Math.sin(a) * rr * r), 1, 0.0, 0.0);
				}
			});
		}
		shockwave(level, center.subtract(0, 0.9, 0), radius, theme, 5);
		radial(level, theme.mote, center, 24, 0.25);
		radial(level, theme.spark, center, 12, 0.35);
		Fx.sound(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 1.3F);
		Fx.sound(level, center, theme.impact, 0.6F, 0.9F);
	}

	/** One pulse of a Zone: a runic circle with spinning marks, a flare and rising motes. */
	public static void zonePulse(ServerLevel level, Vec3 center, double radius, Theme theme, int pulse) {
		Vec3 c = center.add(0, 0.1, 0);
		ring(level, theme.dust(1.2F), c, radius, (int) Math.max(24, radius * 12));
		ring(level, new DustParticleOptions(theme.secondary, 0.8F), c, radius * 0.6, (int) Math.max(14, radius * 7));
		for (int i = 0; i < 8; i++) {
			double a = pulse * 0.4 + Math.PI * 2 * i / 8;
			Vec3 mark = c.add(Math.cos(a) * radius * 0.8, 0, Math.sin(a) * radius * 0.8);
			emit(level, theme.dust(1.6F), mark, 1, 0.0, 0.0);
			fling(level, ParticleTypes.ENCHANT, mark, new Vec3(0, 1, 0), 0.4);
		}
		for (int i = 0; i < (int) (radius * 5); i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(level.getRandom().nextDouble()) * radius;
			fling(level, theme.mote, c.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), new Vec3(0, 1, 0), 0.05);
		}
		if (pulse == 0) {
			emit(level, theme.flash(), c.add(0, 0.5, 0), 1, 0.0, 0.0);
			Fx.sound(level, c, SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE, 0.5F, 1.4F);
		}
	}

	/** A Rain strike: a streak from the sky and a splash where it lands. */
	public static void rainStrike(ServerLevel level, Vec3 target, Theme theme) {
		Vec3 top = target.add(0, 14, 0);
		Fx.send(level, theme.trail(target, 7), top.x, top.y, top.z, 5, 0.12, 0.3, 0.12, 0);
		Scheduler.later(6, () -> {
			emit(level, theme.flash(), target.add(0, 0.3, 0), 1, 0.0, 0.0);
			radial(level, theme.spark, target.add(0, 0.2, 0), 10, 0.2);
			shockwave(level, target, 1.6, theme, 3);
			Fx.sound(level, target, theme.impact, 0.5F, 1.2F);
		});
	}

	// ------------------------------------------------------------------ effects

	public static void fire(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			fling(level, ParticleTypes.FLAME, base.add(Math.cos(a) * 0.5, 0.1, Math.sin(a) * 0.5), new Vec3(-Math.cos(a) * 0.3, 1, -Math.sin(a) * 0.3), 0.12);
		}
		emit(level, ParticleTypes.LAVA, target.getBoundingBox().getCenter(), 4, 0.3, 0.0);
		emit(level, ParticleTypes.LARGE_SMOKE, target.getBoundingBox().getCenter(), 3, 0.3, 0.02);
		Fx.sound(level, base, SoundEvents.GENERIC_BURN, 0.5F, 1.2F);
	}

	public static void frost(ServerLevel level, Entity target) {
		Vec3 center = target.getBoundingBox().getCenter();
		radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), center, 14, 0.18);
		radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), center, 8, 0.12);
		helix(level, target.position(), 0.6, target.getBbHeight() + 0.3, FROST, 6);
		Fx.sound(level, center, SoundEvents.GLASS_BREAK, 0.6F, 1.6F);
		Fx.sound(level, center, SoundEvents.POWDER_SNOW_BREAK, 0.8F, 0.8F);
	}

	public static void lightning(ServerLevel level, Vec3 at) {
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF4C0), at.add(0, 1, 0), 1, 0.0, 0.0);
		radial(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.3, 0), 30, 0.6);
		radial(level, ParticleTypes.END_ROD, at.add(0, 0.3, 0), 10, 0.25);
		shockwave(level, at, 2.2, STORM, 4);
		ring(level, new DustParticleOptions(0x2A2418, 1.8F), at.add(0, 0.06, 0), 0.9, 16);
		Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4F, 1.0F);
	}

	public static void explosion(ServerLevel level, Vec3 center, double radius) {
		emit(level, radius > 3.5 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, center, radius > 3.5 ? 1 : 3, 0.3, 0.0);
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFB060), center, 1, 0.0, 0.0);
		radial(level, ParticleTypes.FLAME, center, 28, 0.35);
		radial(level, ParticleTypes.LARGE_SMOKE, center, 14, 0.15);
		radial(level, ParticleTypes.LAVA, center, 6, 0.1);
		shockwave(level, center.subtract(0, 0.8, 0), radius * 1.2, FIRE, 5);
		Fx.sound(level, center, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.0F);
	}

	public static void heal(ServerLevel level, Entity target) {
		emit(level, ParticleTypes.HEART, target.position().add(0, target.getBbHeight() + 0.3, 0), 4, 0.35, 0.0);
		helix(level, target.position(), 0.55, target.getBbHeight() + 0.4, LIFE, 8);
		radial(level, ParticleTypes.TOTEM_OF_UNDYING, target.getBoundingBox().getCenter(), 10, 0.25);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.5F);
	}

	public static void shield(ServerLevel level, Entity target) {
		double r = Math.max(0.7, target.getBbWidth() * 0.9);
		for (int layer = 0; layer < 3; layer++) {
			int l = layer;
			Scheduler.later(1 + layer * 2, () -> {
				Vec3 c = target.position().add(0, 0.3 + l * target.getBbHeight() * 0.4, 0);
				for (int i = 0; i < 6; i++) {
					double a0 = Math.PI * 2 * i / 6 + l * 0.5;
					double a1 = Math.PI * 2 * (i + 1) / 6 + l * 0.5;
					for (int s = 0; s <= 3; s++) {
						double a = a0 + (a1 - a0) * s / 3;
						emit(level, new DustParticleOptions(0xF0C440, 1.0F), c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0.0, 0.0);
					}
				}
			});
		}
		emit(level, ParticleTypes.WAX_ON, target.getBoundingBox().getCenter(), 10, 0.4, 0.05);
		Fx.sound(level, target.position(), SoundEvents.ARMOR_EQUIP_GOLD, 0.9F, 1.2F);
	}

	public static void harm(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		radial(level, ParticleTypes.ENCHANTED_HIT, c, 14, 0.3);
		emit(level, ParticleTypes.WITCH, c, 6, 0.3, 0.0);
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFE678DC), c, 1, 0.0, 0.0);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 1.3F);
	}

	public static void push(ServerLevel level, Entity target, Vec3 direction) {
		Vec3 c = target.getBoundingBox().getCenter();
		emit(level, ParticleTypes.GUST, c, 1, 0.0, 0.0);
		for (int i = 0; i < 8; i++) {
			fling(level, ParticleTypes.CLOUD, c, direction.add((i - 4) * 0.06, 0.1, (i % 3 - 1) * 0.06), 0.35);
		}
		Fx.sound(level, c, SoundEvents.WIND_CHARGE_BURST, 0.7F, 1.1F);
	}

	public static void pull(ServerLevel level, Entity target, Vec3 towards) {
		stream(level, target.getBoundingBox().getCenter(), towards, VOID, 8);
		emit(level, ParticleTypes.REVERSE_PORTAL, target.getBoundingBox().getCenter(), 14, 0.3, 0.05);
		Fx.sound(level, target.position(), SoundEvents.ENDER_EYE_DEATH, 0.6F, 0.8F);
	}

	public static void launch(ServerLevel level, Entity target) {
		emit(level, ParticleTypes.GUST_EMITTER_SMALL, target.position(), 1, 0.0, 0.0);
		for (int i = 0; i < 16; i++) {
			double a = Math.PI * 2 * i / 16;
			fling(level, ParticleTypes.CLOUD, target.position().add(0, 0.1, 0), new Vec3(Math.cos(a), 0.15, Math.sin(a)), 0.25);
		}
		Fx.sound(level, target.position(), SoundEvents.BREEZE_JUMP, 0.9F, 1.0F);
	}

	public static void dash(ServerLevel level, Entity target, Vec3 direction) {
		Vec3 c = target.position().add(0, 1, 0);
		for (int t = 0; t < 6; t++) {
			Scheduler.later(t + 1, () -> {
				emit(level, ParticleTypes.SMALL_GUST, target.position().add(0, 0.6, 0), 2, 0.2, 0.0);
				emit(level, WIND.fade(1.2F), target.position().add(0, 1, 0).subtract(direction.normalize().scale(0.6)), 3, 0.2, 0.0);
			});
		}
		emit(level, ParticleTypes.GUST, c, 1, 0.0, 0.0);
		Fx.sound(level, c, SoundEvents.BREEZE_SHOOT, 0.8F, 1.4F);
	}

	public static void featherFall(ServerLevel level, Entity target) {
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			fling(level, new ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER), target.position().add(Math.cos(a) * 0.6, 1.8, Math.sin(a) * 0.6), new Vec3(0, -0.2, 0), 0.05);
		}
		emit(level, ParticleTypes.CLOUD, target.position().add(0, 0.2, 0), 10, 0.4, 0.02);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.9F);
	}

	public static void swift(ServerLevel level, Entity target) {
		helix(level, target.position(), 0.5, 0.8, WIND, 6);
		emit(level, ParticleTypes.SMALL_GUST, target.position().add(0, 0.3, 0), 6, 0.3, 0.0);
		Fx.sound(level, target.position(), SoundEvents.BREEZE_JUMP, 0.5F, 1.8F);
	}

	public static void nightEye(ServerLevel level, Entity target) {
		Vec3 eyes = target.getEyePosition();
		ring(level, ARCANE.dust(0.8F), eyes, 0.45, 12);
		emit(level, ParticleTypes.GLOW, eyes, 8, 0.3, 0.02);
		Fx.sound(level, eyes, SoundEvents.BEACON_POWER_SELECT, 0.4F, 1.8F);
	}

	public static void light(ServerLevel level, Vec3 at) {
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF0B0), at, 1, 0.0, 0.0);
		radial(level, ParticleTypes.END_ROD, at, 12, 0.08);
		emit(level, ParticleTypes.GLOW, at, 6, 0.3, 0.0);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_PLACE, 0.8F, 1.6F);
	}

	public static void blink(ServerLevel level, Vec3 from, Vec3 to) {
		// Implode where you were, burst where you arrive, and a streak between them.
		stream(level, from.add(0, 1, 0).add(1.2, 0.5, 1.2), from.add(0, 1, 0), VOID, 6);
		emit(level, ParticleTypes.PORTAL, from.add(0, 1, 0), 40, 0.4, 0.6);
		Fx.send(level, VOID.trail(to.add(0, 1, 0), 10), from.x, from.y + 1, from.z, 10, 0.2, 0.4, 0.2, 0);
		emit(level, VOID.flash(), to.add(0, 1, 0), 1, 0.0, 0.0);
		radial(level, ParticleTypes.REVERSE_PORTAL, to.add(0, 1, 0), 24, 0.2);
		shockwave(level, to, 1.4, VOID, 3);
		Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.2F);
		Fx.sound(level, to, SoundEvents.CHORUS_FRUIT_TELEPORT, 0.8F, 1.0F);
	}

	public static void sonicBoom(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		int steps = (int) Math.max(2, delta.length() / 1.2);
		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			emit(level, ParticleTypes.SONIC_BOOM, p, 1, 0.0, 0.0);
		}
		Fx.sound(level, from, SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.0F);
	}

	public static void wither(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		emit(level, ParticleTypes.LARGE_SMOKE, c, 12, 0.35, 0.02);
		radial(level, ParticleTypes.SOUL, c, 8, 0.08);
		emit(level, new DustParticleOptions(0x1A1A1A, 1.6F), c, 14, 0.4, 0.0);
		Fx.sound(level, c, SoundEvents.WITHER_SHOOT, 0.7F, 1.2F);
	}

	public static void dragonBreath(ServerLevel level, Vec3 center, double radius) {
		emit(level, PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), center.add(0, 0.4, 0), 40, radius / 2, 0.02);
		ring(level, VOID.dust(1.4F), center.add(0, 0.1, 0), radius, (int) Math.max(18, radius * 10));
	}

	public static void grow(ServerLevel level, Vec3 at) {
		radial(level, ParticleTypes.HAPPY_VILLAGER, at, 10, 0.1);
		emit(level, ParticleTypes.COMPOSTER, at, 8, 0.4, 0.0);
	}

	// ------------------------------------------------------------------ expansion shapes

	/** Cone: a fan of streaks and sparks sprayed forward. */
	public static void cone(ServerLevel level, Vec3 origin, Vec3 aim, double length, Theme theme) {
		Vec3 side = aim.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(aim).normalize();
		for (int i = 0; i < 26; i++) {
			double yaw = (level.getRandom().nextDouble() - 0.5) * Math.toRadians(56);
			double pitch = (level.getRandom().nextDouble() - 0.5) * Math.toRadians(40);
			Vec3 dir = aim.add(side.scale(Math.tan(yaw))).add(up.scale(Math.tan(pitch))).normalize();
			Vec3 start = origin.add(aim.scale(0.6));
			if (i % 2 == 0) {
				Fx.send(level, theme.trail(start.add(dir.scale(length * (0.6 + level.getRandom().nextDouble() * 0.4))), 6 + level.getRandom().nextInt(5)),
					start.x, start.y, start.z, 1, 0.05, 0.05, 0.05, 0);
			} else {
				fling(level, theme.mote, start, dir, 0.4 + level.getRandom().nextDouble() * 0.3);
			}
		}
		for (int r = 1; r <= 3; r++) {
			double d = length * r / 3.0;
			double halfWidth = d * Math.tan(Math.toRadians(30));
			for (int k = -3; k <= 3; k++) {
				emit(level, theme.fade(1.2F), origin.add(aim.scale(d)).add(side.scale(halfWidth * k / 3.0)), 1, 0.0, 0.0);
			}
		}
		Fx.sound(level, origin, theme.cast, 0.7F, 0.9F);
	}

	public static void trailPatch(ServerLevel level, Vec3 patch, Theme theme, int tick) {
		if (tick % 4 == 0) {
			emit(level, theme.dust(1.1F), patch.add(0, 0.08, 0), 2, 0.25, 0.0);
			if (tick % 8 == 0) {
				fling(level, theme.mote, patch.add(0, 0.1, 0), new Vec3(0, 1, 0), 0.04);
			}
		}
	}

	/** Wall: glowing pillars standing along the line, shimmering upward. */
	public static void wall(ServerLevel level, Vec3 a, Vec3 b, Theme theme, int tick) {
		Vec3 ab = b.subtract(a);
		int posts = (int) Math.max(4, ab.length() * 1.5);
		for (int i = 0; i <= posts; i++) {
			Vec3 base = a.add(ab.scale(i / (double) posts));
			double h = ((tick / 4 + i) % 6) * 0.5;
			emit(level, theme.fade(1.3F), base.add(0, h, 0), 1, 0.05, 0.0);
			if ((i + tick / 4) % 3 == 0) {
				emit(level, theme.dust(1.0F), base.add(0, 0.1 + level.getRandom().nextDouble() * 2.6, 0), 1, 0.05, 0.0);
			}
		}
		if (tick == 0) {
			for (int i = 0; i <= posts; i++) {
				Vec3 base = a.add(ab.scale(i / (double) posts));
				fling(level, theme.spark, base, new Vec3(0, 1, 0), 0.3);
			}
		}
	}

	public static void orb(ServerLevel level, Vec3 orb, Theme theme, int tick) {
		emit(level, theme.dust(1.9F), orb, 1, 0.0, 0.0);
		emit(level, new DustParticleOptions(theme.secondary, 0.9F), orb, 1, 0.08, 0.0);
		if (tick % 3 == 0) {
			emit(level, theme.mote, orb, 1, 0.05, 0.0);
		}
	}

	// ------------------------------------------------------------------ expansion effects

	/** A jagged little lightning arc between two points. */
	public static void shockArc(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		int steps = (int) Math.max(4, delta.length() * 3);
		Vec3 prev = from;
		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			if (i < steps) {
				p = p.add((level.getRandom().nextDouble() - 0.5) * 0.35, (level.getRandom().nextDouble() - 0.5) * 0.35, (level.getRandom().nextDouble() - 0.5) * 0.35);
			}
			Vec3 mid = prev.add(p).scale(0.5);
			emit(level, new DustParticleOptions(0xFFF6A0, 0.9F), mid, 1, 0.0, 0.0);
			emit(level, ParticleTypes.ELECTRIC_SPARK, p, 1, 0.02, 0.02);
			prev = p;
		}
		Fx.sound(level, to, SoundEvents.TRIDENT_THUNDER.value(), 0.25F, 1.9F);
	}

	public static void haste(ServerLevel level, Entity target) {
		helix(level, target.position(), 0.5, 1.4, ARCANE, 6);
		emit(level, ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 8, 0.35, 0.1);
	}

	public static void reveal(ServerLevel level, Entity target) {
		ring(level, ARCANE.dust(1.0F), target.position().add(0, 0.1, 0), Math.max(0.6, target.getBbWidth()), 14);
		emit(level, ParticleTypes.GLOW, target.getBoundingBox().getCenter(), 6, 0.3, 0.0);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_CLUSTER_PLACE, 0.4F, 2.0F);
	}

	public static void regrowth(ServerLevel level, Entity target) {
		helix(level, target.position(), 0.6, target.getBbHeight() + 0.3, LIFE, 10);
		emit(level, ParticleTypes.COMPOSTER, target.getBoundingBox().getCenter(), 8, 0.4, 0.0);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.7F);
	}

	public static void cleanse(ServerLevel level, Entity target) {
		Vec3 top = target.position().add(0, target.getBbHeight() + 0.4, 0);
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			fling(level, ParticleTypes.SPLASH, top.add(Math.cos(a) * 0.4, 0, Math.sin(a) * 0.4), new Vec3(0, -1, 0), 0.2);
		}
		emit(level, LIFE.sparkle(), target.getBoundingBox().getCenter(), 10, 0.35, 0.0);
		emit(level, ParticleTypes.BUBBLE_POP, target.getBoundingBox().getCenter(), 8, 0.35, 0.0);
		Fx.sound(level, target.position(), SoundEvents.BREWING_STAND_BREW, 0.6F, 1.6F);
	}

	public static void stoneskin(ServerLevel level, Entity target) {
		radial(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
			target.getBoundingBox().getCenter(), 14, 0.15);
		shockwave(level, target.position(), 1.2, EARTH, 3);
		Fx.sound(level, target.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 0.9F, 0.9F);
	}

	public static void root(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		for (int i = 0; i < 4; i++) {
			double a0 = Math.PI * 2 * i / 4;
			for (int s = 0; s < 8; s++) {
				double a = a0 + s * 0.45;
				double y = s * 0.2;
				double r = 0.55 - s * 0.03;
				emit(level, new DustParticleOptions(s % 3 == 0 ? 0x6EDC64 : 0x3E7A34, 1.2F), base.add(Math.cos(a) * r, y, Math.sin(a) * r), 1, 0.0, 0.0);
			}
		}
		emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState()),
			base.add(0, 0.2, 0), 12, 0.3, 0.05);
		Fx.sound(level, base, SoundEvents.AZALEA_LEAVES_PLACE, 0.9F, 0.8F);
	}

	public static void veil(ServerLevel level, Entity target) {
		emit(level, ParticleTypes.LARGE_SMOKE, target.getBoundingBox().getCenter(), 14, 0.4, 0.02);
		radial(level, ParticleTypes.REVERSE_PORTAL, target.getBoundingBox().getCenter(), 16, 0.12);
		Fx.sound(level, target.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.1F);
	}

	public static void empower(ServerLevel level, Entity target) {
		shockwave(level, target.position(), 1.4, Theme_EMPOWER, 3);
		emit(level, ParticleTypes.ANGRY_VILLAGER, target.position().add(0, target.getBbHeight() + 0.2, 0), 1, 0.0, 0.0);
		radial(level, ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 12, 0.3);
		Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 0.8F);
	}

	private static final Theme Theme_EMPOWER = new Theme(0xE04040, 0xFFB060, ParticleTypes.CRIT, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.PLAYER_ATTACK_STRONG);

	public static void levitate(ServerLevel level, Entity target) {
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			fling(level, ParticleTypes.END_ROD, target.position().add(Math.cos(a) * 0.6, 0.1, Math.sin(a) * 0.6), new Vec3(0, 1, 0), 0.12);
		}
		emit(level, ParticleTypes.CLOUD, target.position(), 8, 0.3, 0.02);
		Fx.sound(level, target.position(), SoundEvents.SHULKER_SHOOT, 0.6F, 1.5F);
	}

	public static void freeze(ServerLevel level, Entity target) {
		AABB_OUTLINE(level, target, new DustParticleOptions(0xCFF4FF, 1.1F));
		radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.PACKED_ICE), target.getBoundingBox().getCenter(), 12, 0.15);
		emit(level, ParticleTypes.SNOWFLAKE, target.getBoundingBox().getCenter(), 14, 0.4, 0.02);
		Fx.sound(level, target.position(), SoundEvents.GLASS_PLACE, 1.0F, 0.6F);
		Fx.sound(level, target.position(), SoundEvents.POWDER_SNOW_BREAK, 1.0F, 0.6F);
	}

	/** Dots along the edges of an entity's box: an "ice block" around it. */
	private static void AABB_OUTLINE(ServerLevel level, Entity target, ParticleOptions p) {
		var box = target.getBoundingBox().inflate(0.15);
		double[] xs = {box.minX, box.maxX};
		double[] ys = {box.minY, box.maxY};
		double[] zs = {box.minZ, box.maxZ};
		for (int k = 0; k <= 4; k++) {
			double t = k / 4.0;
			for (double y : ys) {
				for (double z : zs) {
					emit(level, p, new Vec3(box.minX + (box.maxX - box.minX) * t, y, z), 1, 0.0, 0.0);
				}
				for (double x : xs) {
					emit(level, p, new Vec3(x, y, box.minZ + (box.maxZ - box.minZ) * t), 1, 0.0, 0.0);
				}
			}
			for (double x : xs) {
				for (double z : zs) {
					emit(level, p, new Vec3(x, box.minY + (box.maxY - box.minY) * t, z), 1, 0.0, 0.0);
				}
			}
		}
	}

	/** A burning rock streaking down from the sky onto a point, over 12 ticks. */
	public static void meteorFall(ServerLevel level, Vec3 ground) {
		Vec3 start = ground.add(-6, 18, -3);
		for (int t = 0; t < 12; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 p = start.add(ground.subtract(start).scale((tick + 1) / 12.0));
				emit(level, new DustParticleOptions(0x3A2418, 3.0F), p, 3, 0.2, 0.0);
				emit(level, ParticleTypes.FLAME, p, 8, 0.35, 0.02);
				emit(level, ParticleTypes.LARGE_SMOKE, p, 3, 0.3, 0.01);
				emit(level, ParticleTypes.LAVA, p, 1, 0.1, 0.0);
			});
		}
		ring(level, new DustParticleOptions(0xF06E32, 1.4F), ground.add(0, 0.1, 0), 1.6, 20);
		Fx.sound(level, ground, SoundEvents.BLAZE_SHOOT, 1.2F, 0.5F);
	}

	public static void tremor(ServerLevel level, Vec3 ground, double radius) {
		net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(ground.x, ground.y - 0.5, ground.z);
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(below);
		if (state.isAir()) {
			state = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
		}
		var block = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state);
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			double r = radius * (0.3 + level.getRandom().nextDouble() * 0.7);
			fling(level, block, ground.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), new Vec3(0, 1, 0), 0.35);
		}
		emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, state), ground.add(0, 0.1, 0), 30, radius / 2, 0.1);
		shockwave(level, ground, radius, EARTH, 5);
		Fx.sound(level, ground, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.2F, 0.8F);
	}

	/** One frame of a gravity well: a dark core and particles spiralling inward. */
	public static void gravityWell(ServerLevel level, Vec3 point, double radius, int tick) {
		emit(level, new DustParticleOptions(0x140820, 2.4F), point.add(0, 0.8, 0), 3, 0.1, 0.0);
		emit(level, ParticleTypes.REVERSE_PORTAL, point.add(0, 0.8, 0), 2, 0.1, 0.0);
		for (int i = 0; i < 4; i++) {
			double a = tick * 0.35 + Math.PI * 2 * i / 4;
			double r = radius * (0.9 - (tick % 10) * 0.07);
			Vec3 from = point.add(Math.cos(a) * r, 0.5 + (i % 2) * 0.6, Math.sin(a) * r);
			Fx.send(level, VOID.trail(point.add(0, 0.8, 0), 12), from.x, from.y, from.z, 1, 0, 0, 0, 0);
		}
		if (tick % 6 == 0) {
			ring(level, VOID.dust(1.1F), point.add(0, 0.1, 0), radius * (1 - (tick % 30) / 30.0), (int) Math.max(12, radius * 6));
		}
	}

	public static void summon(ServerLevel level, Vec3 at) {
		emit(level, ParticleTypes.SOUL, at.add(0, 0.5, 0), 14, 0.3, 0.05);
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFE678DC), at.add(0, 0.6, 0), 1, 0.0, 0.0);
		radial(level, ParticleTypes.ENCHANT, at.add(0, 0.6, 0), 16, 0.6);
		shockwave(level, at, 1.2, ARCANE, 3);
	}

	// ------------------------------------------------------------------ batch 3 shapes

	public static void ringFront(ServerLevel level, Vec3 origin, double r, Theme theme) {
		int points = (int) Math.max(16, r * 8);
		ring(level, theme.fade(1.4F), origin.add(0, 0.15, 0), r, points);
		if (((int) (r * 2)) % 2 == 0) {
			for (int i = 0; i < points / 3; i++) {
				double a = Math.PI * 2 * i / (points / 3.0);
				fling(level, theme.mote, origin.add(Math.cos(a) * r, 0.2, Math.sin(a) * r), new Vec3(Math.cos(a), 0.4, Math.sin(a)), 0.08);
			}
		}
	}

	/** Pillar: a column shooting up out of the ground over a few ticks. */
	public static void pillar(ServerLevel level, Vec3 base, double radius, Theme theme) {
		for (int t = 0; t < 6; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double y = tick * 1.0;
				ring(level, theme.fade(1.5F), base.add(0, y, 0), radius, (int) Math.max(10, radius * 10));
				emit(level, theme.dust(1.6F), base.add(0, y + 0.5, 0), 4, radius * 0.4, 0.0);
				fling(level, theme.spark, base.add(0, y, 0), new Vec3(0, 1, 0), 0.4);
			});
		}
		emit(level, theme.flash(), base.add(0, 1, 0), 1, 0.0, 0.0);
		shockwave(level, base, radius + 1.0, theme, 3);
		Fx.sound(level, base, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 0.7F);
	}

	public static void waveFront(ServerLevel level, Vec3 front, Vec3 side, double width, Theme theme) {
		int points = (int) Math.max(6, width * 3);
		for (int i = 0; i <= points; i++) {
			double k = i / (double) points - 0.5;
			Vec3 p = front.add(side.scale(k * width)).add(0, 0.3 + (0.5 - Math.abs(k)) * 0.8, 0);
			emit(level, theme.fade(1.4F), p, 1, 0.05, 0.0);
			if (i % 2 == 0) {
				fling(level, theme.mote, p, new Vec3(0, 1, 0), 0.06);
			}
		}
		emit(level, ParticleTypes.SPLASH, front, 6, width / 3, 0.1);
	}

	public static void mineArm(ServerLevel level, Vec3 point, Theme theme) {
		ring(level, theme.dust(1.2F), point.add(0, 0.05, 0), 0.8, 14);
		emit(level, theme.sparkle(), point.add(0, 0.2, 0), 6, 0.3, 0.0);
		Fx.sound(level, point, SoundEvents.TRIPWIRE_ATTACH, 0.8F, 1.4F);
	}

	public static void mineIdle(ServerLevel level, Vec3 point, Theme theme) {
		emit(level, theme.dust(0.8F), point.add(0, 0.05, 0), 3, 0.35, 0.0);
	}

	public static void totem(ServerLevel level, Vec3 top, Theme theme, int tick) {
		double bob = Math.sin(tick * 0.15) * 0.15;
		for (int k = 0; k < 3; k++) {
			emit(level, theme.dust(1.6F), top.add(0, bob - k * 0.35, 0), 1, 0.08, 0.0);
		}
		double a = tick * 0.3;
		emit(level, new DustParticleOptions(theme.secondary, 0.9F), top.add(Math.cos(a) * 0.5, bob, Math.sin(a) * 0.5), 1, 0.0, 0.0);
	}

	public static void totemPulse(ServerLevel level, Vec3 base, double radius, Theme theme) {
		shockwave(level, base, radius, theme, 4);
		emit(level, theme.flash(), base.add(0, 1.6, 0), 1, 0.0, 0.0);
		Fx.sound(level, base, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.2F);
	}

	// ------------------------------------------------------------------ batch 3 effects

	public static void venom(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		emit(level, new DustParticleOptions(0x5A9C2A, 1.3F), c, 12, 0.35, 0.0);
		emit(level, ParticleTypes.ITEM_SLIME, c, 8, 0.3, 0.05);
		Fx.sound(level, c, SoundEvents.SPIDER_HURT, 0.6F, 1.4F);
	}

	public static void smite(ServerLevel level, Entity target) {
		Vec3 top = target.position().add(0, target.getBbHeight() + 3, 0);
		Fx.send(level, new TrailParticleOption(target.getBoundingBox().getCenter(), 0xFFF6C8, 5), top.x, top.y, top.z, 6, 0.1, 0.1, 0.1, 0);
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF6C8), target.getBoundingBox().getCenter(), 1, 0.0, 0.0);
		radial(level, ParticleTypes.END_ROD, target.getBoundingBox().getCenter(), 14, 0.2);
		ring(level, new DustParticleOptions(0xFFE890, 1.2F), target.position().add(0, 0.1, 0), 0.9, 14);
		Fx.sound(level, target.position(), SoundEvents.BELL_RESONATE, 0.8F, 1.6F);
	}

	public static void inferno(ServerLevel level, Vec3 point, double radius) {
		for (int i = 0; i < (int) (radius * 8); i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(level.getRandom().nextDouble()) * radius;
			fling(level, i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, point.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), new Vec3(0, 1, 0), 0.08);
		}
		ring(level, FIRE.dust(1.3F), point.add(0, 0.1, 0), radius, (int) Math.max(16, radius * 8));
		Fx.sound(level, point, SoundEvents.GENERIC_BURN, 0.7F, 0.8F);
	}

	public static void thunderclap(ServerLevel level, Vec3 point, double radius) {
		emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF4C0), point.add(0, 1, 0), 1, 0.0, 0.0);
		radial(level, ParticleTypes.ELECTRIC_SPARK, point.add(0, 1, 0), 24, 0.5);
		radial(level, ParticleTypes.CLOUD, point.add(0, 0.5, 0), 16, 0.3);
		shockwave(level, point, radius * 1.3, STORM, 4);
		Fx.sound(level, point, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.6F);
	}

	/** A falling star streaking down onto a point, then a bright burst. */
	public static void star(ServerLevel level, Vec3 target) {
		Vec3 top = target.add(2.5, 12, 1.5);
		Fx.send(level, new TrailParticleOption(target, 0xFFF0FF, 6), top.x, top.y, top.z, 5, 0.1, 0.1, 0.1, 0);
		Scheduler.later(6, () -> {
			emit(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFF0B0FF), target.add(0, 0.5, 0), 1, 0.0, 0.0);
			radial(level, ParticleTypes.END_ROD, target.add(0, 0.3, 0), 12, 0.2);
			radial(level, ParticleTypes.FIREWORK, target.add(0, 0.3, 0), 10, 0.15);
			Fx.sound(level, target, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6F, 1.4F);
		});
	}

	public static void blind(ServerLevel level, Entity target) {
		emit(level, ParticleTypes.SQUID_INK, target.getEyePosition(), 12, 0.3, 0.02);
		emit(level, new DustParticleOptions(0x100818, 1.5F), target.getEyePosition(), 10, 0.3, 0.0);
		Fx.sound(level, target.position(), SoundEvents.SQUID_SQUIRT, 0.7F, 1.2F);
	}

	public static void chill(ServerLevel level, Entity target) {
		emit(level, ParticleTypes.SNOWFLAKE, target.getBoundingBox().getCenter(), 10, 0.35, 0.01);
		ring(level, FROST.dust(0.9F), target.position().add(0, 0.1, 0), 0.7, 12);
		Fx.sound(level, target.position(), SoundEvents.POWDER_SNOW_STEP, 0.8F, 1.2F);
	}

	public static void silence(ServerLevel level, Entity target) {
		Vec3 head = target.position().add(0, target.getBbHeight() + 0.4, 0);
		ring(level, ARCANE.dust(1.0F), head, 0.4, 10);
		emit(level, ParticleTypes.WITCH, head, 6, 0.2, 0.0);
		Fx.sound(level, target.position(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.5F, 1.4F);
	}

	public static void fireward(ServerLevel level, Entity target) {
		helix(level, target.position(), 0.6, target.getBbHeight() + 0.2, FIRE, 8);
		emit(level, ParticleTypes.SMALL_FLAME, target.getBoundingBox().getCenter(), 10, 0.4, 0.01);
		Fx.sound(level, target.position(), SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.4F);
	}

	public static void nourish(ServerLevel level, Entity target) {
		emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BREAD), target.getEyePosition().subtract(0, 0.3, 0), 8, 0.2, 0.05);
		emit(level, ParticleTypes.HAPPY_VILLAGER, target.getBoundingBox().getCenter(), 6, 0.35, 0.0);
		Fx.sound(level, target.position(), SoundEvents.GENERIC_EAT.value(), 0.7F, 1.1F);
	}

	public static void tidebreath(ServerLevel level, Entity target) {
		for (int i = 0; i < 10; i++) {
			fling(level, ParticleTypes.BUBBLE_POP, target.position().add((i % 5 - 2) * 0.15, 0.2, (i / 5 - 0.5) * 0.3), new Vec3(0, 1, 0), 0.1);
		}
		emit(level, ParticleTypes.SPLASH, target.getBoundingBox().getCenter(), 10, 0.35, 0.05);
		Fx.sound(level, target.position(), SoundEvents.CONDUIT_ACTIVATE, 0.5F, 1.6F);
	}

	public static void leap(ServerLevel level, Entity target) {
		emit(level, ParticleTypes.CLOUD, target.position().add(0, 0.1, 0), 10, 0.35, 0.02);
		ring(level, WIND.dust(1.0F), target.position().add(0, 0.1, 0), 0.8, 14);
		Fx.sound(level, target.position(), SoundEvents.RABBIT_JUMP, 0.8F, 1.0F);
	}

	/** A rope of particles from the caster to the anchor point. */
	public static void grapple(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		int steps = (int) Math.min(80, Math.max(4, delta.length() * 3));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			emit(level, new DustParticleOptions(i % 2 == 0 ? 0xB45AF0 : 0xE0D0FF, 0.8F), p, 1, 0.0, 0.0);
		}
		emit(level, VOID.flash(), to, 1, 0.0, 0.0);
		radial(level, ParticleTypes.REVERSE_PORTAL, to, 10, 0.15);
		Fx.sound(level, from, SoundEvents.FISHING_BOBBER_THROW, 0.8F, 0.7F);
	}

	public static void icepath(ServerLevel level, Vec3 center, double radius) {
		ring(level, FROST.fade(1.2F), center.add(0, 0.6, 0), radius, (int) Math.max(14, radius * 8));
		emit(level, ParticleTypes.SNOWFLAKE, center.add(0, 0.8, 0), 16, radius / 2, 0.01);
		Fx.sound(level, center, SoundEvents.GLASS_PLACE, 0.8F, 1.4F);
	}

	/** The small colour pop on a creature any effect touches, so every hit reads clearly. */
	public static void touched(ServerLevel level, Entity target, Theme theme) {
		emit(level, theme.sparkle(), target.getBoundingBox().getCenter(), 5, 0.3, 0.0);
	}
}
