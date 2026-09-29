package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Spell visuals. Every element has a theme (two colours, a mote, a spark, a cast sound and an
 * impact sound); shapes are drawn in shaped light ({@link Light}) and magic circles on top of it,
 * and effects in their element's visual language ({@link ElementFx}). All of it is sent from the
 * server, so every player sees the same show.
 */
public final class Vfx {
	private Vfx() {}

	public record Theme(int primary, int secondary, ParticleOptions mote, ParticleOptions spark, SoundEvent cast, SoundEvent impact,
			dev.wildercord.cast.feel.Feel feel) {
		/** A plain element theme, with no {@link dev.wildercord.cast.feel.Feel} yet. */
		public Theme(int primary, int secondary, ParticleOptions mote, ParticleOptions spark, SoundEvent cast, SoundEvent impact) {
			this(primary, secondary, mote, spark, cast, impact, null);
		}

		/** The same look carrying the feel of the spell it is drawn for (see {@code dev.wildercord.cast.feel}). */
		public Theme with(dev.wildercord.cast.feel.Feel feel) {
			return new Theme(primary, secondary, mote, spark, cast, impact, feel);
		}

		DustParticleOptions dust(float scale) {
			return new DustParticleOptions(primary, scale);
		}

		DustColorTransitionOptions fade(float scale) {
			return new DustColorTransitionOptions(primary, secondary, scale);
		}

		TrailParticleOption trail(Vec3 target, int duration) {
			return new TrailParticleOption(target, primary, duration);
		}

		SigilOption flash() {
			return SigilOption.glow(primary, 1.8F);
		}

		SpellParticleOption sparkle() {
			return SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFF000000 | primary, 1.0F);
		}
	}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	private static final Theme FIRE = new Theme(0xF06E32, 0xFFD060, ParticleTypes.FLAME, ParticleTypes.LAVA, dev.wildercord.content.WildercordSounds.cast("fire"), dev.wildercord.content.WildercordSounds.impact("fire"));
	private static final Theme FROST = new Theme(0x8CDCFF, 0xFFFFFF, ParticleTypes.SNOWFLAKE, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), dev.wildercord.content.WildercordSounds.cast("frost"), dev.wildercord.content.WildercordSounds.impact("frost"));
	private static final Theme STORM = new Theme(0xFFE650, 0xFFFFFF, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD, dev.wildercord.content.WildercordSounds.cast("storm"), dev.wildercord.content.WildercordSounds.impact("storm"));
	private static final Theme WIND = new Theme(0xC8F0DC, 0xFFFFFF, ParticleTypes.SMALL_GUST, ParticleTypes.CLOUD, dev.wildercord.content.WildercordSounds.cast("wind"), dev.wildercord.content.WildercordSounds.impact("wind"));
	private static final Theme EARTH = new Theme(0xB48C5A, 0x6E5436, ParticleTypes.CRIT, new ItemParticleOption(ParticleTypes.ITEM, Items.COBBLESTONE), dev.wildercord.content.WildercordSounds.cast("earth"), dev.wildercord.content.WildercordSounds.impact("earth"));
	private static final Theme LIFE = new Theme(0x6EDC64, 0xE8FFB0, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.TOTEM_OF_UNDYING, dev.wildercord.content.WildercordSounds.cast("life"), dev.wildercord.content.WildercordSounds.impact("life"));
	private static final Theme VOID = new Theme(0xB45AF0, 0x3A1060, ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL, dev.wildercord.content.WildercordSounds.cast("void"), dev.wildercord.content.WildercordSounds.impact("void"));
	private static final Theme ARCANE = new Theme(0xE678DC, 0xFFD8FA, ParticleTypes.ENCHANT, ParticleTypes.ENCHANTED_HIT, dev.wildercord.content.WildercordSounds.cast("arcane"), dev.wildercord.content.WildercordSounds.impact("arcane"));
	private static final Theme TIME = new Theme(0xF2D98A, 0xFFFFFF, ParticleTypes.WAX_OFF, ParticleTypes.END_ROD, dev.wildercord.content.WildercordSounds.cast("time"), dev.wildercord.content.WildercordSounds.impact("time"));
	private static final Theme BLOOD = new Theme(0xD2283C, 0x5A0A14, ParticleTypes.CRIMSON_SPORE, new DustParticleOptions(0x8A0A1A, 1.1F), dev.wildercord.content.WildercordSounds.cast("blood"), dev.wildercord.content.WildercordSounds.impact("blood"));
	private static final Theme SHAPE = new Theme(RuneColors.SHAPE, 0xC8FFF8, ParticleTypes.GLOW, ParticleTypes.END_ROD, dev.wildercord.content.WildercordSounds.cast("arcane"), dev.wildercord.content.WildercordSounds.impact("arcane"));

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

	/** A theme in any colour, for secret spells: arcane motes and sounds, tinted. */
	public static Theme themeOf(int color) {
		int light = ((((color >> 16) & 0xFF) + 255) / 2 << 16) | ((((color >> 8) & 0xFF) + 255) / 2 << 8) | (((color & 0xFF) + 255) / 2);
		return new Theme(color, light, ParticleTypes.END_ROD, ParticleTypes.ENCHANTED_HIT, dev.wildercord.content.WildercordSounds.cast("arcane"), dev.wildercord.content.WildercordSounds.impact("arcane"));
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
	public static void fling(ServerLevel level, ParticleOptions p, Vec3 at, Vec3 dir, double speed) {
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

	/**
	 * An expanding ring on the ground, racing out over about {@code ticks} ticks: a band of light,
	 * finer for a small ring (a bolt's impact) and heavier for a big one (an explosion), and a
	 * paler one behind it.
	 */
	public static void shockwave(ServerLevel level, Vec3 center, double radius, Theme theme, int ticks) {
		ElementFx.Palette palette = ElementFx.palette(theme);
		double width = Math.min(0.14, 0.04 + radius * 0.025);
		Vec3 c = center.add(0, 0.04, 0);
		ElementFx.groundRing(level, c, palette.primary(), Math.min(0.3, radius * 0.2), radius, width, ticks + 6);
		ElementFx.groundRing(level, c, palette.secondary(), Math.min(0.2, radius * 0.1), radius * 0.75, width * 0.6, ticks + 9);
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
	 * The spell's own magic circle under the caster (a ring and icon per rune, so it can be read:
	 * see {@link dev.wildercord.spell.SpellSigil}), glyphs rising from it and a spark in the hand.
	 * Played on every cast. Nothing here may fly outward from the caster: dust rings and potion
	 * sparkles used to, and scattered across the caster's own first-person view on every cast.
	 */
	public static void castCircle(LivingEntity caster, Theme theme, java.util.List<dev.wildercord.spell.RuneDef> runes) {
		ServerLevel level = (ServerLevel) caster.level();
		Vec3 feet = caster.position();
		// The circle's size follows the spell's scale: a Spark opens a small one, a charged ten-rune spell a great one (band M is 1.0).
		float r = theme.feel() == null ? 1.0F : (float) dev.wildercord.cast.feel.Feels.circleRadius(theme.feel());
		Sigils.spell(level, feet.add(0, 0.06, 0), new Vec3(0, 1, 0), runes, theme.primary, r, 28 + runes.size());
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			fling(level, ParticleTypes.ENCHANT, feet.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), new Vec3(0, 1, 0), 0.6);
		}
		Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.9)).add(0, -0.3, 0);
		emit(level, SigilOption.glow(theme.primary, 0.3F), hand, 1, 0.0, 0.0);
		Fx.sound(level, caster.position(), dev.wildercord.content.WildercordSounds.CIRCLE_OPEN, 0.35F, 1.0F);
		Fx.sound(level, caster.position(), theme.cast, 0.55F, 1.0F);
	}

	// ------------------------------------------------------------------ shapes

	/** Self: rings of light close in as they climb the caster, around a rising helix. */
	public static void self(LivingEntity caster, Theme theme) {
		ServerLevel level = (ServerLevel) caster.level();
		Vec3 feet = caster.position();
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> Light.ring(level, feet.add(0, 0.15 + k * 0.7, 0), UP, k == 1 ? theme.secondary : theme.primary, 1.0, 0.45, 0.05, 10));
		}
		emit(level, SigilOption.glow(theme.primary, 1.4F), feet.add(0, 1.1, 0), 1, 0.0, 0.0);
		helix(level, feet, 0.7, 2.0, theme, 8);
		radial(level, theme.mote, feet.add(0, 1, 0), 8, 0.08);
	}

	/** One tick of a bolt's flight: a bright core, a coloured streak behind it, and a spiral. */
	public static void boltTick(ServerLevel level, Vec3 from, Vec3 to, Theme theme, int age) {
		// A white-hot core in a glow, and a streak of light behind it that thins away.
		emit(level, SigilOption.glow(theme.primary, 0.9F), to, 1, 0.0, 0.0);
		Light.ray(level, from, to, theme.primary, 0.09, 5);
		Vec3 delta = to.subtract(from);
		for (int i = 0; i < 3; i++) {
			Vec3 p = from.add(delta.scale(i / 3.0));
			emit(level, theme.fade(0.9F), p, 1, 0.02, 0.0);
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

	/** Where a bolt, beam or touch lands: a flare, a shockwave racing out, and sparks. */
	public static void impact(ServerLevel level, Vec3 at, Theme theme, double size) {
		dev.wildercord.cast.feel.Feels.impact(level, at, theme, size);
	}

	/** The plain impact (what {@link #impact} always was), with or without its sound: {@code Feels.impact} decides which. */
	public static void impactDefault(ServerLevel level, Vec3 at, Theme theme, double size, boolean sound) {
		Sigils.flash(level, at, theme.primary, (float) (1.5 * size));
		Light.ring(level, at, UP, theme.primary, 0.15 * size, 1.3 * size, 0.06 * size, 9);
		Light.ring(level, at, UP.add(0.6, 0, 0.4).normalize(), theme.secondary, 0.1 * size, 0.9 * size, 0.035 * size, 7);
		radial(level, theme.spark, at, (int) (8 * size), 0.2 * size);
		radial(level, theme.mote, at, (int) (6 * size), 0.1 * size);
		emit(level, theme.sparkle(), at, (int) Math.max(1, 3 * size), 0.3 * size, 0.0);
		if (sound) {
			Fx.sound(level, at, theme.impact, 0.7F, 1.0F);
		}
	}

	/**
	 * A beam: a white-hot core in a coloured halo, fired through a magic circle at the hand, with
	 * rings of power racing down it and a flare where it ends.
	 */
	public static void beam(ServerLevel level, Vec3 from, Vec3 to, Theme theme) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		Vec3 dir = length > 1.0E-6 ? delta.scale(1 / length) : new Vec3(0, 0, 1);
		Light.ray(level, from, to, theme.primary, 0.16, 12);
		Sigils.layer(level, from, dir, SigilOption.CIRCLE, theme.primary, 0.42F, 12, 0.25F);
		Sigils.layer(level, from.add(dir.scale(0.04)), dir, SigilOption.RING, theme.secondary, 0.58F, 12, -0.3F);
		for (double d = 2.0; d < length - 0.5; d += 2.6) {
			Vec3 p = from.add(dir.scale(d));
			Scheduler.later(1 + (int) (d / 10), () -> Light.ring(level, p, dir, theme.primary, 0.12, 0.7, 0.035, 9));
		}
		Light.ring(level, to, dir, theme.secondary, 0.1, 1.1, 0.05, 9);
		Fx.send(level, theme.trail(to, 8), from.x, from.y, from.z, 4, 0.08, 0.08, 0.08, 0);
		Fx.sound(level, from, dev.wildercord.content.WildercordSounds.BEAM_FIRE, 0.8F, 1.0F);
	}

	/** Touch: a small contact, not a beam: a thin thread from the hand, a glow at the palm and a mote or two. */
	public static void contact(ServerLevel level, Vec3 from, Vec3 to, Theme theme) {
		Light.ray(level, from, to, theme.primary, 0.05, 4);
		emit(level, SigilOption.glow(theme.primary, 0.5F), from, 1, 0.0, 0.0);
		emit(level, theme.mote, from, 2, 0.1, 0.01);
	}

	/** Burst: a flare, a shell of light (two crossed rings racing out) and a shockwave along the ground. */
	public static void burst(ServerLevel level, Vec3 center, double radius, Theme theme) {
		Sigils.flash(level, center, theme.primary, (float) Math.min(6, radius * 1.4));
		ScreenFx.shake(level, center, (float) Math.min(0.6, radius * 0.12), radius * 4);
		double spin = level.getRandom().nextDouble() * Math.PI;
		for (int i = 0; i < 2; i++) {
			double a = spin + i * Math.PI / 2;
			Light.ring(level, center, new Vec3(Math.cos(a), 0, Math.sin(a)), theme.primary, 0.3, radius, 0.07, 10);
		}
		Light.ring(level, center, UP, theme.secondary, 0.3, radius * 0.9, 0.05, 9);
		Light.groundRing(level, center.subtract(0, 0.95, 0), theme.primary, 0.4, radius * 1.15, 0.1, 13);
		radial(level, theme.mote, center, 20, 0.25);
		radial(level, theme.spark, center, 12, 0.35);
		Fx.sound(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 1.3F);
		Fx.sound(level, center, theme.impact, 0.6F, 1.0F);
	}

	/** A Zone opens: its ornate circle on the ground for as long as it lasts. */
	public static void zoneOpen(ServerLevel level, Vec3 center, double radius, Theme theme, int lifetime) {
		Sigils.ground(level, center, theme.primary, theme.secondary, (float) radius, lifetime);
		Sigils.flash(level, center.add(0, 0.5, 0), theme.primary, (float) Math.min(5, radius));
		Fx.sound(level, center, dev.wildercord.content.WildercordSounds.CIRCLE_OPEN, 0.6F, 1.0F);
	}

	/** One pulse of a Zone: a wave of light across its circle, and motes rising from it. */
	public static void zonePulse(ServerLevel level, Vec3 center, double radius, Theme theme, int pulse) {
		Vec3 c = center.add(0, 0.1, 0);
		Light.groundRing(level, c, theme.primary, radius * 0.15, radius, 0.06, 12);
		for (int i = 0; i < 6; i++) {
			double a = pulse * 0.4 + Math.PI * 2 * i / 6;
			fling(level, ParticleTypes.ENCHANT, c.add(Math.cos(a) * radius * 0.8, 0, Math.sin(a) * radius * 0.8), UP, 0.4);
		}
		for (int i = 0; i < (int) (radius * 4); i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(level.getRandom().nextDouble()) * radius;
			fling(level, theme.mote, c.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), UP, 0.05);
		}
	}

	/** Rain gathers: a magic circle opens in the sky over the area, facing down. */
	public static void rainCloud(ServerLevel level, Vec3 center, double radius, Theme theme, int lifetime) {
		Vec3 sky = center.add(0, 12, 0);
		Vec3 down = new Vec3(0, -1, 0);
		Sigils.layer(level, sky, down, SigilOption.CIRCLE, theme.primary, (float) (radius * 0.8), lifetime, 0.04F);
		Sigils.layer(level, sky.add(0, -0.05, 0), down, SigilOption.RING, theme.secondary, (float) radius, lifetime, -0.05F);
		Sigils.target(level, center, theme.primary, (float) radius, lifetime);
	}

	/** A Rain strike: a streak of light from the sky and a splash of light where it lands. */
	public static void rainStrike(ServerLevel level, Vec3 target, Theme theme) {
		Vec3 top = target.add(0, 12, 0);
		Light.ray(level, top, target, theme.primary, 0.17, 9);
		Scheduler.later(4, () -> {
			Sigils.flash(level, target.add(0, 0.3, 0), theme.primary, 1.6F);
			Light.groundRing(level, target, theme.primary, 0.2, 1.8, 0.06, 9);
			radial(level, theme.spark, target.add(0, 0.2, 0), 8, 0.2);
			Fx.sound(level, target, theme.impact, 0.5F, 1.0F);
		});
	}

	// ------------------------------------------------------------------ effects
	// Each effect is drawn in its element's language (see ElementFx): a few strong shapes of light
	// and a handful of particles. The buffs a passive can renew (Feather Fall, Swift, Night Eye,
	// Haste, Regrowth, Stoneskin, Empower, Fireward, Tidebreath, Leap) send everything at once and
	// let the light itself do the moving, never the Scheduler, so a quiet renewal stays quiet.

	/** Fire: flame tongues licking up the target over a heat flare, a ring of fire at its feet, embers rising off it. */
	public static void fire(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.35, target.getBbWidth() * 0.6);
		double h = target.getBbHeight();
		ElementFx.heatFlare(level, c, 1.2);
		ElementFx.flames(level, base, w, h, 5);
		Scheduler.later(2, () -> ElementFx.flames(level, target.position(), w, h, 3));
		ElementFx.groundRing(level, base, ElementFx.FIRE.primary(), 0.2, w + 0.9, 0.05, 8);
		ElementFx.embers(level, base.add(0, h * 0.4, 0), w, 8);
		Motes.smoke(level, c.add(0, h * 0.3, 0), 1, 0.25);
		Fx.sound(level, base, SoundEvents.GENERIC_BURN, 0.5F, 1.2F);
	}

	/** Frost: crystal shards burst out of the target, a shatter ring snaps round it and frost creeps over the ground. */
	public static void frost(ServerLevel level, Entity target) {
		Vec3 center = target.getBoundingBox().getCenter();
		double w = Math.max(0.5, target.getBbWidth());
		Sigils.flash(level, center, ElementFx.FROST.primary(), 1.2F);
		ElementFx.shards(level, center, 0.6 + w * 0.5, 7);
		ElementFx.shatterRing(level, center, 0.8 + w);
		ElementFx.frostCreep(level, target.position(), 0.6 + w * 0.6, 24);
		Feels.sound(level, center, "frost_crust", 0.9F, 1.0F);
		Feels.sound(level, center, "frost_needle", 0.5F, 1.1F);
	}

	/** Lightning lands (the bolt itself is vanilla's): a white flare, forks racing out over the ground, rings of light and a scorch. */
	public static void lightning(ServerLevel level, Vec3 at) {
		Vec3 ground = at.add(0, 0.2, 0);
		Sigils.flash(level, at.add(0, 1, 0), ElementFx.STORM.secondary(), 2.6F);
		RandomSource random = level.getRandom();
		double phase = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 3; i++) {
			double a = phase + Math.PI * 2 * i / 3 + (random.nextDouble() - 0.5) * 0.8;
			double reach = 1.5 + random.nextDouble();
			ElementFx.bolt(level, ground, ground.add(Math.cos(a) * reach, 0, Math.sin(a) * reach), 0.05, i == 0 ? 1 : 0, 2);
		}
		ElementFx.groundRing(level, at, ElementFx.STORM.primary(), 0.3, 2.6, 0.07, 7);
		ElementFx.groundRing(level, at, ElementFx.STORM.accent(), 0.2, 1.8, 0.04, 9);
		ElementFx.sparks(level, at.add(0, 0.3, 0), 14, 0.5);
		emit(level, new DustParticleOptions(0x2A2418, 1.8F), at.add(0, 0.08, 0), 6, 0.45, 0.0);
		Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4F, 1.0F);
	}

	/** An explosion: a great heat flare, a shell of flame slashes and fire rings racing out, a shockwave over the ground, embers and smoke. */
	public static void explosion(ServerLevel level, Vec3 center, double radius) {
		ScreenFx.shake(level, center, (float) Math.min(1, 0.3 + radius * 0.12), radius * 5 + 6);
		emit(level, radius > 3.5 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, center, 1, 0.3, 0.0);
		ElementFx.heatFlare(level, center, Math.min(3.5, 1.0 + radius * 0.5));
		ElementFx.flameBurst(level, center, radius * 0.55, (int) Math.min(8, 3 + radius));
		double spin = level.getRandom().nextDouble() * Math.PI;
		for (int i = 0; i < 2; i++) {
			double a = spin + i * Math.PI / 2;
			ElementFx.ring(level, center, new Vec3(Math.cos(a), 0, Math.sin(a)), i == 0 ? ElementFx.FIRE.primary() : ElementFx.FIRE.secondary(), 0.3, radius,
				0.08, 9);
		}
		Vec3 floor = ElementFx.floor(level, center, radius + 1);
		if (floor != null) {
			ElementFx.groundRing(level, floor, ElementFx.FIRE.primary(), 0.4, radius * 1.2, 0.12, 12);
			Scheduler.later(2, () -> ElementFx.groundRing(level, floor, ElementFx.FIRE.accent(), 0.3, radius * 0.9, 0.06, 10));
		}
		radial(level, ParticleTypes.FLAME, center, 18, 0.3);
		Motes.clouds(level, center, 7, radius * 0.3, Motes.SMOKE, 1.2 + radius * 0.25, 45, new Vec3(0, 0.035, 0), 0.08, 0.45);
		radial(level, ParticleTypes.LAVA, center, 4, 0.1);
		Fx.sound(level, center, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.0F);
	}

	/** Heal: a soft green bloom, a leaf spiral climbing the target, petals and hearts. */
	public static void heal(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double h = target.getBbHeight();
		double w = Math.max(0.45, target.getBbWidth() * 0.75);
		ElementFx.bloom(level, target.getBoundingBox().getCenter(), base, 1.0 + w * 0.5);
		ElementFx.leafSpiral(level, base, w, h + 0.2, 5);
		ElementFx.petals(level, base.add(0, h + 0.3, 0), 0.4, 4);
		emit(level, ParticleTypes.HEART, base.add(0, h + 0.3, 0), 3, 0.35, 0.0);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.5F);
	}

	/** Harm: a star seal flares under the target, comets of pink light whirl round it and glyphs shimmer in. */
	public static void harm(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.5, target.getBbWidth());
		Sigils.flash(level, c, ElementFx.ARCANE.primary(), 1.8F);
		Sigils.flash(level, c, ElementFx.ARCANE.secondary(), 0.8F);
		ElementFx.starSeal(level, target.position().add(0, 0.07, 0), UP, 0.5 + w * 0.4, 16);
		ElementFx.orbit(level, c, 0.5 + w * 0.5, 3, 5);
		radial(level, ParticleTypes.ENCHANTED_HIT, c, 8, 0.3);
		ElementFx.shimmer(level, c, 0.35, 4);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 1.3F);
	}

	/** Push: gust crescents slam into the target the way it's thrown and a ring of wind blows out past it. */
	public static void push(ServerLevel level, Entity target, Vec3 direction) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 dir = direction.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : direction.normalize();
		double r = Math.max(0.8, target.getBbWidth() + 0.4);
		// One broad, blunt slab thrust along the push (heavy, not a burst) and a low ring of dust at the feet.
		Vec3 at = c.subtract(dir.scale(r * 0.9));
		ElementFx.slash(level, at, UP, dir, ElementFx.WIND.primary(), r * 1.2, 1.3, 0.3, 2, 7);
		ElementFx.groundRing(level, target.position(), ElementFx.WIND.secondary(), 0.2, r * 1.4, 0.06, 8);
		emit(level, ParticleTypes.GUST, c, 1, 0.0, 0.0);
		for (int i = 0; i < 3; i++) {
			fling(level, ParticleTypes.CLOUD, c, dir.add((i - 1) * 0.15, 0.06, 0), 0.3);
		}
		Feels.sound(level, c, "wind_thump", 0.8F, 1.1F);
	}

	/** Pull: darkness implodes round the target toward the pull, light streaming off it to where it's pulled. */
	public static void pull(ServerLevel level, Entity target, Vec3 towards) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 to = towards.subtract(c);
		Vec3 n = to.lengthSqr() < 1.0E-4 ? UP : to.normalize();
		double r = Math.max(0.8, target.getBbWidth() + 0.5);
		ElementFx.ring(level, c, n, ElementFx.dark(ElementFx.VOID.accent()), r * 1.4, 0.15, 0.09, 8);
		ElementFx.ring(level, c.add(n.scale(0.3)), n, ElementFx.VOID.primary(), r * 1.6, 0.2, 0.03, 8);
		stream(level, c, towards, VOID, 4);
		emit(level, ParticleTypes.PORTAL, c, 8, 0.1, 0.6);
		Fx.sound(level, target.position(), SoundEvents.ENDER_EYE_DEATH, 0.6F, 0.8F);
	}

	/** Launch: a ring of wind bursts out along the ground and a spiral of gusts throws the target skyward. */
	public static void launch(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double w = Math.max(0.5, target.getBbWidth());
		ElementFx.groundRing(level, base, 0xBFE3FF, 0.2, 1.3 + w, 0.07, 8);
		// A pillar: rings stacked up the lift, one a tick, each a little narrower than the last.
		for (int i = 0; i < 5; i++) {
			int k = i;
			Scheduler.later(i, () -> ElementFx.ring(level, base.add(0, 0.3 + k * 0.55, 0), UP, k % 2 == 0 ? ElementFx.WIND.primary() : ElementFx.WIND.secondary(),
				0.9 + w * 0.3 - k * 0.1, 0.35, 0.04, 8));
		}
		ElementFx.swirl(level, base.add(0, 0.1, 0), 0.5 + w * 0.4, 1.8, 3);
		emit(level, ParticleTypes.GUST_EMITTER_SMALL, base, 1, 0.0, 0.0);
		Feels.sound(level, target.position(), "wind_rise", 0.9F, 1.0F);
	}

	/** Dash: rings of air burst out behind the target, and streaks of wind trail it as it goes. */
	public static void dash(ServerLevel level, Entity target, Vec3 direction) {
		Vec3 dir = direction.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : direction.normalize();
		Vec3 c = target.position().add(0, 1, 0);
		ElementFx.ring(level, c.subtract(dir.scale(0.6)), dir, ElementFx.WIND.secondary(), 0.3, 1.5, 0.05, 7);
		ElementFx.ring(level, c.subtract(dir.scale(1.1)), dir, ElementFx.WIND.accent(), 0.2, 1.0, 0.035, 9);
		Vec3[] last = {target.position()};
		for (int t = 0; t < 5; t++) {
			Scheduler.later(t + 1, () -> {
				Vec3 now = target.position();
				Vec3 step = now.subtract(last[0]);
				double length = step.length();
				if (length > 0.2) {
					Vec3 along = step.scale(1 / length);
					Vec3 side = ElementFx.perp(along);
					for (int s = -1; s <= 1; s += 2) {
						// Low and to the sides: they stream past a dashing caster's view, never through it.
						Vec3 off = side.scale(s * 0.4).add(0, s < 0 ? 0.35 : 0.8, 0);
						ElementFx.ray(level, last[0].add(off), now.add(off).subtract(along.scale(Math.min(0.3, length * 0.4))), ElementFx.WIND.primary(), 0.04, 6);
					}
				}
				emit(level, ParticleTypes.SMALL_GUST, now.add(0, 0.4, 0), 1, 0.2, 0.0);
				last[0] = now;
			});
		}
		emit(level, ParticleTypes.GUST, c, 1, 0.0, 0.0);
		Feels.sound(level, c, "wind_dash", 0.9F, 1.0F);
	}

	/** Feather Fall: feathers drift down round the target, a slow crescent of air circles its feet and a ring opens under it. */
	public static void featherFall(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * 2 * i / 5;
			fling(level, new ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER), base.add(Math.cos(a) * 0.7, target.getBbHeight() + 0.1, Math.sin(a) * 0.7),
				new Vec3(0, -0.2, 0), 0.04);
		}
		ElementFx.groundRing(level, base, ElementFx.WIND.secondary(), 0.2, 1.3, 0.04, 16);
		ElementFx.slash(level, base.add(0, 0.25, 0), UP, ElementFx.flatDir(level.getRandom().nextDouble() * Math.PI * 2), ElementFx.WIND.primary(), 0.8,
			Math.PI * 1.6, 0.05, 8, 14);
		emit(level, ParticleTypes.CLOUD, base.add(0, 0.15, 0), 4, 0.35, 0.01);
		Feels.sound(level, target.position(), "wind_feather", 0.7F, 1.4F);
	}

	/** Swift: gusts swirl round the target's legs and a ring of wind runs out along the ground. */
	public static void swift(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		// Streaks trailing off the legs (low, behind the way it faces), and a thin sonic ring at the feet.
		Vec3 look = target.getLookAngle();
		Vec3 back = new Vec3(-look.x, 0, -look.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : new Vec3(-look.x, 0, -look.z).normalize();
		Vec3 side = ElementFx.perp(back);
		for (int s = -1; s <= 1; s++) {
			Vec3 from = base.add(side.scale(s * 0.28)).add(0, 0.25 + Math.abs(s) * 0.15, 0);
			ElementFx.ray(level, from, from.add(back.scale(1.1)), ElementFx.WIND.primary(), 0.03, 6);
		}
		ElementFx.groundRing(level, base, ElementFx.WIND.secondary(), 0.2, 1.6, 0.03, 7);
		Feels.sound(level, target.position(), "wind_dash", 0.5F, 1.4F);
	}

	/** Night Eye: a violet ring closes round the eyes and a spark of light kindles in each (not for your own eyes). */
	public static void nightEye(ServerLevel level, Entity target) {
		Vec3 eyes = target.getEyePosition();
		Vec3 look = target.getLookAngle();
		Vec3 ahead = new Vec3(look.x, 0, look.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(look.x, 0, look.z).normalize();
		Vec3 side = ahead.cross(UP);
		ElementFx.ring(level, eyes, UP, ElementFx.ARCANE.accent(), 0.8, 0.35, 0.03, 10);
		for (int s = -1; s <= 1; s += 2) {
			emit(level, SigilOption.glow(ElementFx.ARCANE.secondary(), 0.35F), eyes.add(side.scale(s * 0.12)).add(ahead.scale(0.3)), 1, 0.0, 0.0);
		}
		emit(level, ParticleTypes.GLOW, eyes, 4, 0.3, 0.02);
		Fx.sound(level, eyes, SoundEvents.BEACON_POWER_SELECT, 0.4F, 1.8F);
	}

	/** Light: an orb of light kindles at the point, a ring running out from it and motes drifting off. */
	public static void light(ServerLevel level, Vec3 at) {
		Sigils.flash(level, at, HOLY, 1.8F);
		ElementFx.orb(level, at, HOLY, 0.2, 24);
		ElementFx.ring(level, at, UP, ElementFx.ARCANE.secondary(), 0.1, 1.2, 0.04, 9);
		radial(level, ParticleTypes.END_ROD, at, 8, 0.07);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_PLACE, 0.8F, 1.6F);
	}

	/** Blink: darkness implodes where you were, a dark streak runs to where you arrive, and a black core snaps open there. */
	public static void blink(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 a = from.add(0, 1, 0);
		Vec3 b = to.add(0, 1, 0);
		ElementFx.implode(level, a, 1.3, 8);
		Vec3 d = b.subtract(a);
		double length = d.length();
		if (length > 2.2) {
			// It stops short of where you land, so its end never glows under your own eyes.
			Vec3 end = b.subtract(d.scale(1.2 / length));
			ElementFx.ray(level, a, end, ElementFx.dark(ElementFx.VOID.accent()), 0.16, 7);
			ElementFx.ray(level, a, end, ElementFx.VOID.primary(), 0.05, 8);
		}
		ElementFx.blackCore(level, b, 0.3, 7);
		ElementFx.ring(level, b, UP, ElementFx.VOID.primary(), 0.2, 1.6, 0.04, 8);
		ElementFx.groundRing(level, to, ElementFx.VOID.secondary(), 0.2, 1.4, 0.04, 9);
		radial(level, ParticleTypes.REVERSE_PORTAL, b, 12, 0.18);
		Fx.sound(level, from, dev.wildercord.content.WildercordSounds.BLINK, 0.4F, 1.0F);
		Fx.sound(level, to, dev.wildercord.content.WildercordSounds.BLINK, 0.8F, 1.0F);
	}

	/** Sonic Boom: a beam of darkness round a violet core, the warden's rings and void shockwaves racing down it. */
	public static void sonicBoom(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		Vec3 dir = length > 1.0E-4 ? delta.scale(1 / length) : new Vec3(0, 0, 1);
		ElementFx.ray(level, from, to, ElementFx.dark(ElementFx.VOID.accent()), 0.22, 10);
		ElementFx.ray(level, from, to, ElementFx.VOID.primary(), 0.05, 9);
		// Nothing within a couple of blocks of where it starts: that's often the caster's own face.
		for (double s = 2.5; s < length; s += 2.5) {
			Vec3 p = from.add(dir.scale(s));
			Scheduler.later(1 + (int) (s / 8), () -> ElementFx.ring(level, p, dir, ElementFx.VOID.secondary(), 0.2, 1.1, 0.04, 8));
			emit(level, ParticleTypes.SONIC_BOOM, p, 1, 0.0, 0.0);
		}
		ElementFx.voidImpact(level, to, 1.0);
		Fx.sound(level, from, SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.0F);
	}

	/** Wither: darkness falls in on the target round a small black core, smoke and souls rising off it. */
	public static void wither(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.5, target.getBbWidth());
		ElementFx.implode(level, c, 0.9 + w * 0.6, 9);
		ElementFx.blackCore(level, c, 0.14 + w * 0.06, 10);
		Motes.clouds(level, c, 3, 0.3, 0x3A3438, 0.9, 30, new Vec3(0, 0.02, 0), 0.02, 0.4);
		radial(level, ParticleTypes.SOUL, c, 4, 0.06);
		Fx.sound(level, c, SoundEvents.WITHER_SHOOT, 0.7F, 1.2F);
	}

	/** One pulse of Dragon Breath: violet breath over the ground, a rim of light round it and darkness drawing in. */
	public static void dragonBreath(ServerLevel level, Vec3 center, double radius) {
		emit(level, PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), center.add(0, 0.4, 0), 22, radius / 2, 0.02);
		ElementFx.groundRing(level, center, ElementFx.VOID.primary(), radius * 0.85, radius, 0.06, 20);
		ElementFx.groundRing(level, center, ElementFx.dark(ElementFx.VOID.accent()), radius, radius * 0.25, 0.12, 18);
	}

	/** Grow: a small bloom on the block, leaves and sparkles. */
	public static void grow(ServerLevel level, Vec3 at) {
		Sigils.flash(level, at, ElementFx.LIFE.secondary(), 1.0F);
		ElementFx.groundRing(level, at.subtract(0, 0.1, 0), ElementFx.LIFE.primary(), 0.2, 1.4, 0.04, 12);
		ElementFx.petals(level, at, 0.4, 4);
		radial(level, ParticleTypes.HAPPY_VILLAGER, at, 6, 0.08);
	}

	// ------------------------------------------------------------------ expansion shapes

	/** Cone: a fan of beams sprayed forward, swept by arcs of light that open with the cone. */
	public static void cone(ServerLevel level, Vec3 origin, Vec3 aim, double length, Theme theme) {
		Vec3 side = aim.cross(UP);
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(aim).normalize();
		Vec3 start = origin.add(aim.scale(0.6));
		for (int i = 0; i < 7; i++) {
			double yaw = Math.toRadians(-26 + 52 * i / 6.0) + (level.getRandom().nextDouble() - 0.5) * 0.1;
			double pitch = (level.getRandom().nextDouble() - 0.5) * Math.toRadians(24);
			Vec3 dir = aim.add(side.scale(Math.tan(yaw))).add(up.scale(Math.tan(pitch))).normalize();
			Light.ray(level, start, start.add(dir.scale(length * (0.75 + level.getRandom().nextDouble() * 0.25))), i % 2 == 0 ? theme.primary : theme.secondary, 0.07, 8);
		}
		for (int r = 1; r <= 3; r++) {
			double d = length * r / 3.0;
			int k = r;
			Scheduler.later(r, () -> Light.slash(level, origin, up, aim, k == 3 ? theme.secondary : theme.primary, d, Math.toRadians(62), 0.12 + 0.05 * k, 1, 6));
		}
		for (int i = 0; i < 10; i++) {
			double yaw = (level.getRandom().nextDouble() - 0.5) * Math.toRadians(56);
			fling(level, theme.mote, start, aim.add(side.scale(Math.tan(yaw))).normalize(), 0.4 + level.getRandom().nextDouble() * 0.3);
		}
	}

	public static void trailPatch(ServerLevel level, Vec3 patch, Theme theme, int tick) {
		if (tick % 30 == 0) {
			// A small glowing seal burned into the ground.
			Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, theme.primary, 0.5F, 34, 0.08F), patch.add(0, 0.07, 0));
		}
		if (tick % 4 == 0) {
			emit(level, theme.dust(1.0F), patch.add(0, 0.08, 0), 1, 0.25, 0.0);
			if (tick % 8 == 0) {
				fling(level, theme.mote, patch.add(0, 0.1, 0), UP, 0.04);
			}
		}
	}

	/** Wall: a fence of light, glowing posts joined by bars along the top and bottom, shimmering upward. */
	public static void wall(ServerLevel level, Vec3 a, Vec3 b, Theme theme, int tick) {
		Vec3 ab = b.subtract(a);
		int posts = (int) Math.max(3, ab.length() / 1.5);
		if (tick % 8 == 0) {
			for (int i = 0; i <= posts; i++) {
				Vec3 base = a.add(ab.scale(i / (double) posts));
				Light.ray(level, base, base.add(0, 3.0, 0), theme.primary, 0.22, 14);
			}
			Light.ray(level, a.add(0, 0.1, 0), b.add(0, 0.1, 0), theme.secondary, 0.11, 14);
			Light.ray(level, a.add(0, 3.0, 0), b.add(0, 3.0, 0), theme.secondary, 0.11, 14);
			Light.ray(level, a.add(0, 1.55, 0), b.add(0, 1.55, 0), theme.primary, 0.06, 14);
		}
		if (tick % 4 == 0) {
			Vec3 p = a.add(ab.scale(level.getRandom().nextDouble())).add(0, level.getRandom().nextDouble() * 2.8, 0);
			fling(level, theme.mote, p, UP, 0.05);
		}
		if (tick == 0) {
			for (int i = 0; i <= posts; i++) {
				fling(level, theme.spark, a.add(ab.scale(i / (double) posts)), UP, 0.3);
			}
		}
	}

	/** An Orbit's orb: a glowing core that trails light as it circles. */
	public static void orb(ServerLevel level, Vec3 orb, Theme theme, int tick) {
		emit(level, SigilOption.glow(theme.primary, 0.9F), orb, 1, 0.0, 0.0);
		emit(level, theme.dust(1.3F), orb, 1, 0.0, 0.0);
		if (tick % 3 == 0) {
			emit(level, theme.mote, orb, 1, 0.05, 0.0);
		}
	}

	// ------------------------------------------------------------------ expansion effects

	/** A jagged little lightning arc between two points, forking and flickering. */
	public static void shockArc(ServerLevel level, Vec3 from, Vec3 to) {
		ElementFx.bolt(level, from, to, 0.045, from.distanceTo(to) > 2.5 ? 1 : 0, 2);
		emit(level, ParticleTypes.ELECTRIC_SPARK, to, 4, 0.1, 0.1);
		Fx.sound(level, to, SoundEvents.TRIDENT_THUNDER.value(), 0.25F, 1.9F);
	}

	/** Haste: two comets of pink light whirl fast round the target's arms and a star seal flickers at its feet. */
	public static void haste(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		ElementFx.orbit(level, c.add(0, 0.1, 0), Math.max(0.6, target.getBbWidth() * 0.8), 2, 3);
		ElementFx.starSeal(level, target.position().add(0, 0.07, 0), UP, 0.55, 12);
		emit(level, ParticleTypes.CRIT, c, 5, 0.35, 0.1);
	}

	/** Reveal: a ring of light scans up the target over a star seal marking the ground under it. */
	public static void reveal(ServerLevel level, Entity target) {
		double r = Math.max(0.6, target.getBbWidth() * 0.9);
		double h = target.getBbHeight();
		ElementFx.starSeal(level, target.position().add(0, 0.07, 0), UP, r, 18);
		for (int i = 0; i < 4; i++) {
			double y = h * (i + 0.5) / 4;
			Scheduler.later(1 + i * 2, () -> ElementFx.ring(level, target.position().add(0, y, 0), UP, ElementFx.ARCANE.secondary(), r * 1.15, r, 0.035, 6));
		}
		emit(level, ParticleTypes.GLOW, target.getBoundingBox().getCenter(), 4, 0.3, 0.0);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_CLUSTER_PLACE, 0.4F, 2.0F);
	}

	/** Regrowth: a leaf spiral winds up the target, a ring of green opens under it and leaves drift down. */
	public static void regrowth(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		ElementFx.leafSpiral(level, base, Math.max(0.45, target.getBbWidth() * 0.75), target.getBbHeight() + 0.2, 6);
		ElementFx.groundRing(level, base, ElementFx.LIFE.primary(), 0.2, 1.2, 0.04, 14);
		ElementFx.petals(level, base.add(0, target.getBbHeight() * 0.6, 0), 0.4, 4);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.7F);
	}

	/** Cleanse: rings of clear light wash down the target from above its head, water falling with them. */
	public static void cleanse(ServerLevel level, Entity target) {
		double h = target.getBbHeight();
		double r = Math.max(0.55, target.getBbWidth() * 0.8);
		for (int i = 0; i < 4; i++) {
			int k = i;
			double y = h + 0.3 - (h + 0.2) * i / 3;
			Scheduler.later(1 + i * 2, () -> ElementFx.ring(level, target.position().add(0, y, 0), UP, k % 2 == 0 ? 0xDFFFF4 : ElementFx.LIFE.secondary(),
				r * 1.25, r * 0.9, 0.04, 6));
		}
		Scheduler.later(8, () -> ElementFx.groundRing(level, target.position(), ElementFx.LIFE.primary(), 0.2, 1.3, 0.05, 10));
		Vec3 top = target.position().add(0, h + 0.4, 0);
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			fling(level, ParticleTypes.SPLASH, top.add(Math.cos(a) * 0.4, 0, Math.sin(a) * 0.4), new Vec3(0, -1, 0), 0.2);
		}
		emit(level, ParticleTypes.BUBBLE_POP, target.getBoundingBox().getCenter(), 5, 0.35, 0.0);
		Fx.sound(level, target.position(), SoundEvents.BREWING_STAND_BREW, 0.6F, 1.6F);
	}

	/** Stoneskin: the ground cracks under the target and rings of sandstone light close hard round it. */
	public static void stoneskin(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double r = Math.max(0.6, target.getBbWidth() * 0.9);
		double h = target.getBbHeight();
		ElementFx.crack(level, base, 1.2, 20);
		for (int i = 0; i < 2; i++) {
			ElementFx.ring(level, base.add(0, 0.3 + i * h * 0.45, 0), UP, i == 0 ? ElementFx.EARTH.primary() : ElementFx.EARTH.secondary(), r * 1.7, r, 0.07,
				10 + i * 3);
		}
		Sigils.flash(level, target.getBoundingBox().getCenter(), ElementFx.EARTH.secondary(), 1.2F);
		Fx.sound(level, target.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 0.9F, 0.9F);
	}

	private static final int VINE = 0x3E8A34;

	/** Root: the ground cracks and vines of green light twist up round the target's legs and hold it. */
	public static void root(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double r = Math.max(0.45, target.getBbWidth() * 0.7);
		ElementFx.crack(level, base, 0.9 + r, 30);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int vine = 0; vine < 4; vine++) {
			for (int s = 0; s < 3; s++) {
				double a = phase + vine * Math.PI / 2 + s * 0.9;
				ElementFx.slash(level, base.add(0, 0.15 + s * 0.35, 0), ElementFx.tilted(0.5, a + Math.PI / 2), ElementFx.flatDir(a),
					s == 2 ? ElementFx.LIFE.primary() : VINE, r * (1 - s * 0.15), 1.0, 0.09, 2 + s * 2, 30);
			}
		}
		emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState()),
			base.add(0, 0.2, 0), 8, 0.3, 0.05);
		Fx.sound(level, base, SoundEvents.AZALEA_LEAVES_PLACE, 0.9F, 0.8F);
	}

	/** Veil: darkness falls in on the target as it fades from sight, a wisp of smoke left behind. */
	public static void veil(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(0.8, target.getBbWidth() + 0.4);
		ElementFx.implode(level, c, r * 1.2, 10);
		ElementFx.groundRing(level, target.position(), ElementFx.dark(ElementFx.VOID.accent()), r * 1.4, 0.2, 0.1, 12);
		Motes.clouds(level, c, 4, 0.35, 0x4A4450, 1.0, 30, new Vec3(0, 0.02, 0), 0.03, 0.4);
		radial(level, ParticleTypes.REVERSE_PORTAL, c, 8, 0.08);
		Fx.sound(level, target.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.1F);
	}

	private static final int EMPOWER = 0xE04040;

	/** Empower: a star seal blazes under the target and crescents of power surge up round it. */
	public static void empower(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		ElementFx.starSeal(level, base.add(0, 0.07, 0), UP, 0.9, 16);
		ElementFx.groundRing(level, base, EMPOWER, 0.3, 1.6, 0.06, 8);
		ElementFx.tongues(level, base, Math.max(0.4, target.getBbWidth() * 0.6), target.getBbHeight(), 4, EMPOWER, 0xFFB060, 2, 8);
		emit(level, ParticleTypes.ANGRY_VILLAGER, base.add(0, target.getBbHeight() + 0.2, 0), 1, 0.0, 0.0);
		radial(level, ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 8, 0.3);
		Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 0.8F);
	}

	/** Levitate: rings of air rise under the target and lift it, motes of light drifting up with it. */
	public static void levitate(ServerLevel level, Entity target) {
		double r = Math.max(0.55, target.getBbWidth() * 0.8);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 3, () -> ElementFx.ring(level, target.position().add(0, 0.05, 0), UP, k == 1 ? ElementFx.WIND.accent() : ElementFx.WIND.secondary(),
				r * 1.5, r * 0.8, 0.045, 8));
		}
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			fling(level, ParticleTypes.END_ROD, target.position().add(Math.cos(a) * 0.6, 0.1, Math.sin(a) * 0.6), UP, 0.1);
		}
		emit(level, ParticleTypes.CLOUD, target.position(), 3, 0.3, 0.02);
		// Where it comes to hang: a pale halo round it once it has risen.
		Scheduler.later(14, () -> {
			if (target.isAlive() && target.level() == level) {
				ElementFx.ring(level, target.position().add(0, target.getBbHeight() * 0.5, 0), UP, 0xBFE3FF, r * 1.6, r * 1.4, 0.03, 30);
			}
		});
		Feels.sound(level, target.position(), "wind_rise", 0.5F, 0.7F);
	}

	/** Freeze: ice closes round the target for as long as it's held, a ring of frost clamps in and frost creeps over the ground. */
	public static void freeze(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.5, target.getBbWidth());
		// Only a mob frozen solid is closed in ice: players and bosses are just slowed, and keep moving.
		if (target instanceof Mob mob && mob.isAlive() && mob.isNoAi()) {
			MobEffectInstance held = mob.getEffect(MobEffects.SLOWNESS);
			BlockFx.encase(level, mob, held != null ? Math.max(10, held.getDuration() - 4) : 46);
		}
		Sigils.flash(level, c, ElementFx.FROST.secondary(), 1.4F);
		ElementFx.ring(level, c, UP, ElementFx.FROST.secondary(), w + 1.2, w * 0.6, 0.06, 9);
		ElementFx.ring(level, c.add(0, target.getBbHeight() * 0.35, 0), UP, ElementFx.FROST.accent(), w + 0.9, w * 0.55, 0.04, 10);
		ElementFx.shards(level, c, 0.5 + w * 0.5, 5);
		ElementFx.frostCreep(level, target.position(), 0.8 + w * 0.5, 40);
		emit(level, ParticleTypes.SNOWFLAKE, c, 6, 0.4, 0.02);
		Feels.sound(level, target.position(), "frost_lock", 1.0F, 1.0F);
	}

	/** Ice about to give: hairline cracks run across the shell and a shard or two fall (the tell before a hold ends). */
	public static void iceCracking(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = Math.max(0.5, target.getBbWidth());
		ElementFx.crack(level, target.position(), 0.6 + w * 0.5, 10);
		ElementFx.shards(level, c, 0.4 + w * 0.4, 3);
		Feels.sound(level, target.position(), "frost_crack", 0.7F, 1.0F);
	}

	/** A burning rock streaking down out of the sky onto a point over 12 ticks, a reticle marking where it will land. */
	public static void meteorFall(ServerLevel level, Vec3 ground) {
		Scheduler.later(12, () -> ScreenFx.shake(level, ground, 0.8F, 24));
		Vec3 start = ground.add(-6, 18, -3);
		Vec3 path = ground.subtract(start);
		Sigils.target(level, ground, ElementFx.FIRE.primary(), 1.8F, 16);
		for (int t = 0; t < 12; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 p = start.add(path.scale((tick + 1) / 12.0));
				Vec3 back = start.add(path.scale(Math.max(0, tick - 2) / 12.0));
				ElementFx.orb(level, p, ElementFx.FIRE.secondary(), 0.4, 2);
				emit(level, SigilOption.glow(ElementFx.FIRE.primary(), 1.8F), p, 1, 0.0, 0.0);
				ElementFx.ray(level, back, p, ElementFx.FIRE.primary(), 0.32, 7);
				emit(level, ParticleTypes.FLAME, p, 4, 0.3, 0.02);
				Motes.smoke(level, back, 1, 0.3);
			});
		}
		Fx.sound(level, ground, SoundEvents.BLAZE_SHOOT, 1.2F, 0.5F);
	}

	/** Tremor: the ground cracks open, shockwaves of dust race out over it and spires of stone jut up and sink. */
	public static void tremor(ServerLevel level, Vec3 ground, double radius) {
		ScreenFx.shake(level, ground, (float) Math.min(0.9, 0.3 + radius * 0.1), radius * 3 + 8);
		ElementFx.crack(level, ground, radius * 0.8, 30);
		ElementFx.groundRing(level, ground, ElementFx.EARTH.secondary(), 0.4, radius * 1.1, 0.14, 12);
		Scheduler.later(3, () -> ElementFx.groundRing(level, ground, ElementFx.EARTH.primary(), 0.3, radius * 0.8, 0.08, 10));
		if (radius >= 2.5) {
			RandomSource random = level.getRandom();
			int spires = (int) Math.min(6, radius);
			for (int i = 0; i < spires; i++) {
				double a = Math.PI * 2 * i / spires + random.nextDouble() * 0.6;
				double r = radius * (0.35 + 0.45 * random.nextDouble());
				Vec3 p = CastEngine.ground(level, ground.add(Math.cos(a) * r, 1, Math.sin(a) * r));
				if (Math.abs(p.y - ground.y) < 1.5) {
					float height = 0.8F + (i % 3) * 0.3F;
					Scheduler.later(1 + i % 3, () -> BlockFx.spire(level, p, height, 0.4F, 4));
				}
			}
		}
		Fx.sound(level, ground, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.2F, 0.8F);
	}

	/** One frame of a gravity well (every other tick): a black core, light spiralling into it, darkness and a violet rim falling in. */
	public static void gravityWell(ServerLevel level, Vec3 point, double radius, int tick) {
		Vec3 core = point.add(0, 0.8, 0);
		ElementFx.orb(level, core, ElementFx.dark(ElementFx.VOID.accent()), 0.35, 3);
		for (int i = 0; i < 3; i++) {
			double a = tick * 0.35 + Math.PI * 2 * i / 3;
			double r = radius * (0.9 - (tick % 10) * 0.07);
			Vec3 from = point.add(Math.cos(a) * r, 0.5 + (i % 2) * 0.6, Math.sin(a) * r);
			Fx.send(level, VOID.trail(core, 12), from.x, from.y, from.z, 1, 0, 0, 0, 0);
		}
		if (tick % 10 == 0) {
			double tilt = tick * 0.3;
			ElementFx.ring(level, core, ElementFx.tilted(1.2, tilt), ElementFx.VOID.primary(), 0.5, 0.48, 0.02, 12);
			ElementFx.ring(level, core, ElementFx.tilted(1.2, tilt + Math.PI / 2), ElementFx.VOID.secondary(), 0.5, 0.48, 0.02, 12);
			ElementFx.groundRing(level, point, ElementFx.VOID.primary(), radius, 0.4, 0.05, 14);
			ElementFx.ring(level, core, ElementFx.tilted(0.9, -tilt), ElementFx.dark(ElementFx.VOID.accent()), radius * 0.7, 0.3, 0.1, 12);
		}
		if (tick % 6 == 0) {
			emit(level, ParticleTypes.PORTAL, core, 3, 0.1, 1.2);
		}
	}

	/** Summon: a star seal opens on the ground, a column of light rises from it and souls stream up. */
	public static void summon(ServerLevel level, Vec3 at) {
		ElementFx.starSeal(level, at.add(0, 0.07, 0), UP, 1.0, 24);
		ElementFx.ray(level, at, at.add(0, 1.8, 0), ElementFx.ARCANE.primary(), 0.2, 10);
		Sigils.flash(level, at.add(0, 0.6, 0), ElementFx.ARCANE.primary(), 2.0F);
		ElementFx.groundRing(level, at, ElementFx.ARCANE.accent(), 0.3, 1.8, 0.05, 10);
		emit(level, ParticleTypes.SOUL, at.add(0, 0.5, 0), 6, 0.3, 0.05);
		ElementFx.shimmer(level, at.add(0, 0.6, 0), 0.4, 8);
	}

	// ------------------------------------------------------------------ batch 3 shapes

	/** Ring: a band of light racing out along the ground, throwing motes. */
	public static void ringFront(ServerLevel level, Vec3 origin, double r, Theme theme) {
		Light.groundRing(level, origin.add(0, 0.07, 0), theme.primary, Math.max(0.2, r - 0.4), r + 0.6, 0.09, 5);
		if (((int) (r * 2)) % 2 == 0) {
			int points = (int) Math.max(8, r * 3);
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				fling(level, theme.mote, origin.add(Math.cos(a) * r, 0.2, Math.sin(a) * r), new Vec3(Math.cos(a), 0.4, Math.sin(a)), 0.08);
			}
		}
	}

	/** Pillar: a circle opens on the ground and a column of light erupts from it, ringed as it climbs. */
	public static void pillar(ServerLevel level, Vec3 base, double radius, Theme theme) {
		ScreenFx.shake(level, base, 0.35F, 14);
		Sigils.ground(level, base, theme.primary, theme.secondary, (float) (radius + 0.4), 26);
		Light.ray(level, base, base.add(0, 7, 0), theme.primary, radius * 0.55, 16);
		for (int t = 0; t < 6; t++) {
			double y = t * 1.1;
			Scheduler.later(t + 1, () -> {
				Light.ring(level, base.add(0, y, 0), UP, theme.secondary, radius * 1.1, radius * 0.6, 0.05, 8);
				fling(level, theme.spark, base.add(0, y, 0), UP, 0.4);
			});
		}
		Sigils.flash(level, base.add(0, 1, 0), theme.primary, (float) (radius * 2 + 1));
		Light.groundRing(level, base, theme.primary, radius * 0.5, radius + 1.5, 0.08, 10);
		Fx.sound(level, base, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 0.7F);
	}

	/** Wave: a glowing crest rolling forward, spray flying off it. */
	public static void waveFront(ServerLevel level, Vec3 front, Vec3 side, double width, Theme theme) {
		Vec3 motion = side.cross(UP).normalize();
		double radius = width * 0.62;
		Light.slash(level, front.subtract(0, radius * 0.72, 0), motion, UP, theme.primary, radius, 1.5, 0.22, 1, 5);
		Light.slash(level, front.subtract(0, radius * 0.72, 0).subtract(motion.scale(0.5)), motion, UP, theme.secondary, radius * 0.92, 1.3, 0.12, 1, 5);
		int points = (int) Math.max(4, width * 1.5);
		for (int i = 0; i <= points; i++) {
			double k = i / (double) points - 0.5;
			fling(level, theme.mote, front.add(side.scale(k * width)).add(0, 0.4, 0), UP, 0.06);
		}
		emit(level, ParticleTypes.SPLASH, front, 6, width / 3, 0.1);
	}

	public static void mineArm(ServerLevel level, Vec3 point, Theme theme) {
		Sigils.target(level, point, theme.primary, 0.8F, 60);
		Light.groundRing(level, point, theme.primary, 1.2, 0.3, 0.05, 8);
		Fx.sound(level, point, SoundEvents.TRIPWIRE_ATTACH, 0.8F, 1.4F);
	}

	public static void mineIdle(ServerLevel level, Vec3 point, Theme theme) {
		// A slow heartbeat: a faint ring breathing out, and the reticle renewed.
		Light.groundRing(level, point, theme.primary, 0.2, 0.9, 0.035, 12);
		Sigils.target(level, point, theme.primary, 0.8F, 30);
	}

	/** A Totem: a glowing crystal of light bobbing over its spot, a mote circling it. */
	public static void totem(ServerLevel level, Vec3 top, Theme theme, int tick) {
		double bob = Math.sin(tick * 0.15) * 0.15;
		Vec3 base = top.subtract(0, 1.6, 0);
		if (tick % 40 == 0) {
			Sigils.ground(level, base, theme.primary, theme.secondary, 0.9F, 46);
		}
		if (tick % 10 == 0) {
			Light.orb(level, top.add(0, bob, 0), theme.primary, 0.3, 12);
			Light.ray(level, base.add(0, 0.1, 0), top.add(0, bob, 0), theme.secondary, 0.07, 12);
		}
		emit(level, SigilOption.glow(theme.primary, 1.1F), top.add(0, bob, 0), 1, 0.0, 0.0);
		double a = tick * 0.3;
		emit(level, new DustParticleOptions(theme.secondary, 0.9F), top.add(Math.cos(a) * 0.55, bob, Math.sin(a) * 0.55), 1, 0.0, 0.0);
	}

	public static void totemPulse(ServerLevel level, Vec3 base, double radius, Theme theme) {
		Light.groundRing(level, base, theme.primary, 0.3, radius, 0.08, 12);
		Light.ray(level, base.add(0, 1.6, 0), base, theme.secondary, 0.1, 6);
		Sigils.flash(level, base.add(0, 1.6, 0), theme.primary, 1.8F);
	}

	// ------------------------------------------------------------------ batch 3 effects

	private static final int VENOM = 0x86D23A;

	/** Venom: two fangs of sickly green light bite into the target, a pulse of poison spreading and dripping off it. */
	public static void venom(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(0.5, target.getBbWidth() * 0.7);
		Vec3 across = ElementFx.flatDir(level.getRandom().nextDouble() * Math.PI * 2);
		// Upright jaws: one crescent dips from above, one rises from below, and they meet in the target.
		ElementFx.slash(level, c.add(0, r * 0.9, 0), across, new Vec3(0, -1, 0), VENOM, r, 1.3, 0.1, 1, 6);
		ElementFx.slash(level, c.subtract(0, r * 0.9, 0), across, UP, VENOM, r, 1.3, 0.1, 1, 6);
		ElementFx.ring(level, c, UP, VENOM, 0.15, 1.0 + r, 0.05, 8);
		ElementFx.ring(level, c, UP, 0x4E8A22, 0.1, 0.7 + r, 0.04, 11);
		emit(level, ParticleTypes.ITEM_SLIME, c, 5, 0.3, 0.05);
		emit(level, new DustParticleOptions(VENOM, 1.0F), c, 4, 0.35, 0.0);
		Fx.sound(level, c, SoundEvents.SPIDER_HURT, 0.6F, 1.4F);
	}

	private static final int HOLY = 0xFFF0B0;

	/** Smite: a lance of holy light drives down onto the target, a star flares under it and light bursts out. */
	public static void smite(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 top = base.add(0, target.getBbHeight() + 5, 0);
		ElementFx.ray(level, top, base, HOLY, 0.28, 10);
		ElementFx.ray(level, top, base, 0xFFFFFF, 0.08, 8);
		Sigils.flash(level, c, HOLY, 2.2F);
		ElementFx.flatSigil(level, base, SigilOption.STAR, HOLY, 1.1, 18, 0.1);
		ElementFx.groundRing(level, base, HOLY, 0.2, 2.0, 0.07, 9);
		radial(level, ParticleTypes.END_ROD, c, 10, 0.18);
		Fx.sound(level, target.position(), SoundEvents.BELL_RESONATE, 0.8F, 1.6F);
	}

	/** One pulse of Inferno: a ring of fire round the area, flame tongues leaping up inside it, embers and smoke. */
	public static void inferno(ServerLevel level, Vec3 point, double radius) {
		RandomSource random = level.getRandom();
		ElementFx.groundRing(level, point, ElementFx.FIRE.primary(), radius * 0.9, radius, 0.08, 20);
		ElementFx.groundRing(level, point, ElementFx.FIRE.accent(), radius * 0.4, radius * 0.95, 0.05, 14);
		int tongues = (int) Math.min(8, 2 + radius);
		for (int i = 0; i < tongues; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = Math.sqrt(random.nextDouble()) * radius * 0.85;
			ElementFx.flames(level, point.add(Math.cos(a) * r, 0, Math.sin(a) * r), 0.25, 0.8, 1);
		}
		for (int i = 0; i < (int) (radius * 4); i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = Math.sqrt(random.nextDouble()) * radius;
			fling(level, ParticleTypes.FLAME, point.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), UP, 0.08);
		}
		Motes.smoke(level, point.add(0, 0.6, 0), 2, radius * 0.4);
		Fx.sound(level, point, SoundEvents.GENERIC_BURN, 0.7F, 0.8F);
	}

	/** Thunderclap: a white flare, a shockwave of light and forks of lightning ripping out over the ground, a puff of thundercloud. */
	public static void thunderclap(ServerLevel level, Vec3 point, double radius) {
		thunderclap(level, point, radius, true);
	}

	/** As above; {@code shake} is false when the clap hit nothing (the ground rumbles for nobody). */
	public static void thunderclap(ServerLevel level, Vec3 point, double radius, boolean shake) {
		if (shake) {
			ScreenFx.shake(level, point, 0.3F, radius * 3 + 10);
		}
		Vec3 ground = point.add(0, 0.2, 0);
		Sigils.flash(level, point.add(0, 1, 0), ElementFx.STORM.secondary(), 2.6F);
		ElementFx.groundRing(level, point, ElementFx.STORM.primary(), 0.3, radius * 1.3, 0.1, 8);
		ElementFx.ring(level, point.add(0, 1, 0), UP, ElementFx.STORM.accent(), 0.3, radius, 0.05, 7);
		RandomSource random = level.getRandom();
		double phase = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 4; i++) {
			double a = phase + Math.PI / 2 * i + (random.nextDouble() - 0.5) * 0.7;
			ElementFx.bolt(level, ground, ground.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 0.05, i % 2, 2);
		}
		radial(level, ParticleTypes.CLOUD, point.add(0, 0.5, 0), 10, 0.3);
		ElementFx.sparks(level, point.add(0, 1, 0), 12, 0.5);
		Fx.sound(level, point, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.6F);
	}

	private static final int STARLIGHT = 0xFFF0FF;

	/** A falling star streaking down onto a point, then a burst of starlight shaped like a star. */
	public static void star(ServerLevel level, Vec3 target) {
		Vec3 top = target.add(2.5, 12, 1.5);
		// A star mark on the ground where it will land, then the star falling onto it.
		Sigils.send(level, SigilOption.flat(SigilOption.STAR, 0xE8E0FF, 0.8F, 16, 0.18F), target.add(0, 0.07, 0));
		for (int t = 0; t < 5; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 p = top.lerp(target, (tick + 1) / 6.0);
				Vec3 back = top.lerp(target, Math.max(0, tick - 1) / 6.0);
				ElementFx.orb(level, p, STARLIGHT, 0.16, 2);
				ElementFx.ray(level, back, p, 0xB8C8FF, 0.1, 6);
			});
		}
		Scheduler.later(6, () -> {
			Vec3 at = target.add(0, 0.4, 0);
			Sigils.flash(level, at, 0xF0B0FF, 2.2F);
			ElementFx.groundRing(level, target, ElementFx.ARCANE.primary(), 0.2, 1.8, 0.06, 9);
			// Five points of light flung out flat: a star.
			double a0 = level.getRandom().nextDouble() * Math.PI * 2;
			for (int i = 0; i < 5; i++) {
				double a = a0 + Math.PI * 2 * i / 5;
				ElementFx.ray(level, at, at.add(Math.cos(a) * 1.1, 0.1, Math.sin(a) * 1.1), i % 2 == 0 ? STARLIGHT : ElementFx.ARCANE.primary(), 0.05, 7);
			}
			radial(level, ParticleTypes.END_ROD, at, 8, 0.18);
			radial(level, ParticleTypes.FIREWORK, at, 6, 0.14);
			Fx.sound(level, target, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6F, 1.4F);
		});
	}

	/** Blind: darkness closes over the target's eyes and hangs there, ink dripping from it. */
	public static void blind(ServerLevel level, Entity target) {
		Vec3 eyes = target.getEyePosition();
		ElementFx.ring(level, eyes, UP, ElementFx.dark(ElementFx.VOID.accent()), 0.9, 0.2, 0.1, 10);
		ElementFx.orb(level, eyes, ElementFx.dark(ElementFx.VOID.accent()), 0.28, 14);
		ElementFx.ring(level, eyes, UP, ElementFx.VOID.primary(), 1.0, 0.3, 0.025, 9);
		emit(level, ParticleTypes.SQUID_INK, eyes, 6, 0.25, 0.02);
		Fx.sound(level, target.position(), SoundEvents.SQUID_SQUIRT, 0.7F, 1.2F);
	}

	/** Chill: frost creeps out under the target and a thin ring of cold closes on it. */
	public static void chill(ServerLevel level, Entity target) {
		double w = Math.max(0.5, target.getBbWidth());
		ElementFx.frostCreep(level, target.position(), 0.5 + w * 0.4, 16);
		// Three thin rings climbing from the feet to the hips, one every two ticks.
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> ElementFx.ring(level, target.position().add(0, 0.1 + k * target.getBbHeight() * 0.28, 0), UP,
				k == 2 ? ElementFx.FROST.secondary() : ElementFx.FROST.primary(), w + 0.6, w * 0.55, 0.025, 8));
		}
		emit(level, ParticleTypes.SNOWFLAKE, target.getBoundingBox().getCenter(), 4, 0.35, 0.01);
		Feels.sound(level, target.position(), "frost_crust", 0.5F, 1.3F);
	}

	/** Silence: a ring of light closes over the target's head and seals there. */
	public static void silence(ServerLevel level, Entity target) {
		Vec3 head = target.position().add(0, target.getBbHeight() + 0.4, 0);
		ElementFx.ring(level, head, UP, ElementFx.ARCANE.primary(), 0.8, 0.35, 0.04, 8);
		ElementFx.ring(level, head, UP, ElementFx.ARCANE.accent(), 0.36, 0.34, 0.03, 20);
		ElementFx.sigil(level, head, UP, SigilOption.CIRCLE, ElementFx.ARCANE.primary(), 0.3, 20, 0.08);
		emit(level, ParticleTypes.WITCH, head, 4, 0.2, 0.0);
		Fx.sound(level, target.position(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.5F, 1.4F);
	}

	/** Fireward: flame tongues curl round the target and fold into warm rings that close on it. */
	public static void fireward(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double w = Math.max(0.4, target.getBbWidth() * 0.65);
		double h = target.getBbHeight();
		ElementFx.flames(level, base, w, h, 4);
		ElementFx.ring(level, base.add(0, 0.2, 0), UP, ElementFx.FIRE.secondary(), w + 1.1, w + 0.1, 0.05, 12);
		ElementFx.ring(level, base.add(0, h * 0.6, 0), UP, ElementFx.FIRE.primary(), w + 0.9, w + 0.1, 0.04, 14);
		emit(level, ParticleTypes.SMALL_FLAME, target.getBoundingBox().getCenter(), 6, 0.4, 0.01);
		Fx.sound(level, target.position(), SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.4F);
	}

	/** Nourish: crumbs and a small green bloom. */
	public static void nourish(ServerLevel level, Entity target) {
		emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BREAD), target.getEyePosition().subtract(0, 0.3, 0), 6, 0.2, 0.05);
		ElementFx.bloom(level, target.getBoundingBox().getCenter(), target.position(), 0.9);
		emit(level, ParticleTypes.HAPPY_VILLAGER, target.getBoundingBox().getCenter(), 4, 0.35, 0.0);
		Fx.sound(level, target.position(), SoundEvents.GENERIC_EAT.value(), 0.7F, 1.1F);
	}

	private static final int TIDE = 0x4AA8FF;

	/** Tidebreath: water swirls up round the target in crescents of blue light, bubbles rising with it. */
	public static void tidebreath(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		ElementFx.swirl(level, base.add(0, 0.2, 0), Math.max(0.45, target.getBbWidth() * 0.75), target.getBbHeight() * 0.8, 4, TIDE, ElementFx.FROST.primary());
		ElementFx.groundRing(level, base, TIDE, 0.2, 1.3, 0.05, 10);
		for (int i = 0; i < 8; i++) {
			fling(level, ParticleTypes.BUBBLE_POP, base.add((i % 4 - 1.5) * 0.2, 0.2, (i / 4 - 0.5) * 0.4), UP, 0.1);
		}
		emit(level, ParticleTypes.SPLASH, target.getBoundingBox().getCenter(), 6, 0.35, 0.05);
		Feels.sound(level, target.position(), "frost_breath", 0.7F, 1.0F);
	}

	/** Leap: rings of air spring out along the ground and a pair of gusts curl up the legs. */
	public static void leap(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		// The crouch: a ring closes in on the feet, then springs up as a column of two rings.
		ElementFx.groundRing(level, base, ElementFx.WIND.secondary(), 1.1, 0.3, 0.05, 4);
		Scheduler.later(4, () -> {
			ElementFx.ring(level, base.add(0, 0.2, 0), UP, ElementFx.WIND.accent(), 0.3, 1.0, 0.04, 7);
			ElementFx.ring(level, base.add(0, 0.9, 0), UP, ElementFx.WIND.secondary(), 0.4, 0.8, 0.03, 8);
			emit(level, ParticleTypes.CLOUD, base.add(0, 0.1, 0), 4, 0.3, 0.02);
		});
		Feels.sound(level, target.position(), "wind_feather", 0.8F, 1.2F);
	}

	/** Grapple: a line of violet light from the caster to the anchor, and a black core where it bites. */
	public static void grapple(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		// It starts a little way out, so it never begins in the caster's own face.
		Vec3 near = length > 2.6 ? from.add(d.scale(1.3 / length)) : from;
		ElementFx.ray(level, near, to, ElementFx.VOID.primary(), 0.06, 12);
		ElementFx.ray(level, near, to, ElementFx.VOID.secondary(), 0.025, 12);
		ElementFx.blackCore(level, to, 0.2, 10);
		ElementFx.ring(level, to, d, ElementFx.VOID.primary(), 0.1, 0.9, 0.04, 8);
		radial(level, ParticleTypes.REVERSE_PORTAL, to, 8, 0.15);
		Fx.sound(level, from, SoundEvents.FISHING_BOBBER_THROW, 0.8F, 0.7F);
	}

	/** Icepath: frost creeps out over the water, shards of ice glinting up from it. */
	public static void icepath(ServerLevel level, Vec3 center, double radius) {
		ElementFx.frostCreep(level, center.add(0, 0.5, 0), radius, 30);
		ElementFx.shards(level, center.add(0, 0.8, 0), 0.7, 4);
		emit(level, ParticleTypes.SNOWFLAKE, center.add(0, 0.8, 0), 10, radius / 2, 0.01);
		Feels.sound(level, center, "frost_crust", 0.8F, 0.6F);
	}

	/** The small pop of light on a creature any effect touches, so every hit reads clearly: a glow and a few of its element's motes. */
	public static void touched(ServerLevel level, Entity target, Theme theme) {
		Vec3 c = target.getBoundingBox().getCenter();
		emit(level, SigilOption.glow(theme.primary, 0.9F), c, 1, 0.0, 0.0);
		emit(level, theme.mote, c, 3, 0.3, 0.02);
	}
}
