package dev.wildercord.cast.feel;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Vfx;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.spell.RuneDef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * The layers of a spell's feel: cue, travel, impact, aftermath and hit, plus the one way to play a kit sound. Every method
 * does what the mod has always done when no {@link Signature} applies, so nothing changes until a rune gets one.
 *
 * <p>The {@link Feel} of a group travels inside its {@link Vfx.Theme} ({@code theme.feel()}), so shape code that already
 * has a theme reaches it with no new parameter. See docs/ADDING_RUNES.md ("Give it its own feel").</p>
 */
public final class Feels {
	private Feels() {}

	private static boolean initialised;

	/** Registers every element's signatures. Called once at start-up; add your element's class here only if it is new. */
	public static synchronized void init() {
		if (initialised) {
			return;
		}
		initialised = true;
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(MarkHalos::tick);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> MarkHalos.clear());
		ShapeFeels.register();
		FireFeels.register();
		FrostFeels.register();
		StormFeels.register();
		WindFeels.register();
		EarthFeels.register();
		LifeFeels.register();
		VoidFeels.register();
		ArcaneFeels.register();
		TimeFeels.register();
		BloodFeels.register();
	}

	// ------------------------------------------------------------------ sound

	private static final Set<String> MISSING = new HashSet<>();

	/**
	 * Plays a kit sound (a name from tools/feel, e.g. {@code "fire_flick"}) with the mod's variation rules: a random spread of
	 * about 3% in pitch, and the per-tick voice limit of {@link Fx#sound}. An unknown name is skipped (and logged once), so
	 * code can name a sound before it has been authored.
	 */
	public static void sound(ServerLevel level, Vec3 at, String kitName, float volume, float pitch) {
		SoundEvent event = WildercordSounds.kit(kitName);
		if (event == null) {
			if (MISSING.add(kitName)) {
				dev.wildercord.Wildercord.LOGGER.warn("Feel: no kit sound named '{}' (yet)", kitName);
			}
			return;
		}
		float jitter = 1.0F + (level.getRandom().nextFloat() - 0.5F) * 0.06F;
		Fx.sound(level, at, event, volume, pitch * jitter);
	}

	/** As {@link #sound(ServerLevel, Vec3, String, float, float)}, with the volume scaled by the feel's band (S 0.7, M 1.0, L 1.15, XL 1.3). */
	public static void sound(ServerLevel level, Vec3 at, Feel feel, String kitName, float volume, float pitch) {
		sound(level, at, kitName, volume * feel.band().volume, pitch);
	}

	/** The pentatonic ratios: playing a tonal kit sound at {@code step(i)} climbs the scale (i = 0, 1, 2...; 5 is an octave up). */
	private static final float[] SCALE = {1.0F, 1.122F, 1.26F, 1.498F, 1.682F};

	public static float step(int i) {
		int k = Math.max(0, i);
		return Math.min(2.0F, SCALE[k % 5] * (1 << Math.min(1, k / 5)));
	}

	/** The spell circle's radius under the caster for a feel: 0.8 (S), 1.0 (M, as always), 1.3 (L), 1.7 (XL). */
	public static double circleRadius(Feel feel) {
		return switch (feel.band()) {
			case S -> 0.8;
			case M -> 1.0;
			case L -> 1.3;
			case XL -> 1.7;
		};
	}

	// ------------------------------------------------------------------ helpers

	/** The theme of a group's first effect (or the shape colour) with the feel attached and the signature's accent applied. */
	public static Vfx.Theme themed(Vfx.Theme base, Feel feel) {
		Signature s = Signatures.of(feel);
		if (s != null && s.accent != null) {
			return new Vfx.Theme(base.primary(), s.accent, base.mote(), base.spark(), base.cast(), base.impact(), feel);
		}
		return base.with(feel);
	}

	/**
	 * Plays the signature's sound for {@code phase} and runs its hook.
	 *
	 * @return whether the default look and sound of the phase should still happen (false when the signature replaced it)
	 */
	private static boolean run(Phase phase, Feel feel, Vfx.Theme theme, ServerLevel level, Vec3 at, Vec3 dir, Entity target, Cast cast, int tick) {
		Signature s = Signatures.of(feel);
		if (s == null) {
			return true;
		}
		Signature.Cue cue = s.soundOf(phase);
		if (cue != null) {
			sound(level, at, feel, cue.name(), cue.volume(), cue.pitch());
		}
		Hook hook = s.hookOf(phase);
		if (hook != null) {
			hook.run(new FeelCtx(level, feel, theme, at, dir == null ? new Vec3(0, 1, 0) : dir, target, cast, tick));
		}
		return !s.replaces(phase);
	}

	// ------------------------------------------------------------------ the layers

	/**
	 * CUE: the first 200 ms. Called once per cast, for the spell's first group: the shape's gesture (its motion's sound at the
	 * shape's own pitch), the modifiers' gold tells, a kick for the big ones, then the leading rune's signature.
	 */
	public static void cue(Cast cast, Feel feel, Vfx.Theme theme) {
		var caster = cast.caster;
		Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.9)).add(0, -0.3, 0);
		Signature lead = Signatures.of(feel);
		if (lead == null || !lead.replaces(Phase.CUE)) {
			ShapeFeels.gesture(cast.level, hand, feel);
			Tells.modifiers(cast, feel, theme, hand);
			if (feel.band().ordinal() >= Band.L.ordinal()) {
				// A big spell leaves the hand with a shove.
				dev.wildercord.cast.ScreenFx.kick(caster, feel.band() == Band.XL ? 0.6F : 0.35F);
			}
		}
		run(Phase.CUE, feel, theme, cast.level, hand, caster.getLookAngle(), null, cast, 0);
	}

	/** TRAVEL: one tick of a projectile's flight. Returns whether the shape's own trail should still be drawn. */
	public static boolean travel(ServerLevel level, Vfx.Theme theme, Vec3 from, Vec3 to, int tick) {
		Feel feel = theme.feel();
		if (feel == null) {
			return true;
		}
		Vec3 d = to.subtract(from);
		return run(Phase.TRAVEL, feel, theme, level, to, d.lengthSqr() > 1.0E-6 ? d.normalize() : null, null, null, tick);
	}

	/**
	 * IMPACT (then AFTERMATH): where a Bolt, Beam or Touch lands. Replaces the direct calls of {@code Vfx.impact}.
	 *
	 * @param size the shape's own size for the flare (0.4 to 1.4)
	 */
	public static void impact(ServerLevel level, Vec3 at, Vfx.Theme theme, double size) {
		Feel feel = theme.feel();
		boolean standard = true;
		boolean aftermath = true;
		if (feel != null) {
			standard = run(Phase.IMPACT, feel, theme, level, at, null, null, null, 0);
		}
		if (standard) {
			Vfx.impactDefault(level, at, theme, size, true);
		} else {
			// The signature took over the sound and hooks; the flare and rings are still the shape's.
			Vfx.impactDefault(level, at, theme, size, false);
		}
		if (feel != null && feel.band() != Band.S && size >= 0.8) {
			if (run(Phase.AFTERMATH, feel, theme, level, at, null, null, null, 0)) {
				aftermath(level, at, feel);
			}
		}
	}

	/**
	 * The default aftermath: for a moment, the element leaves a small mark where a spell of band M or bigger landed (embers,
	 * rime, static, dust, petals, a dark wisp, glyph flecks, golden ticks, drops). A handful of particles, never a second show.
	 */
	public static void aftermath(ServerLevel level, Vec3 at, Feel feel) {
		double s = Math.min(1.6, feel.scale());
		Vec3 floor = dev.wildercord.cast.ElementFx.floor(level, at, 2.5);
		Vec3 ground = floor == null ? at : floor;
		switch (feel.element()) {
			case "fire" -> dev.wildercord.cast.ElementFx.embers(level, ground.add(0, 0.2, 0), 0.4 * s, (int) (4 * s));
			case "frost" -> dev.wildercord.cast.ElementFx.frostCreep(level, ground, 0.5 * s, 40);
			case "storm" -> dev.wildercord.cast.ElementFx.sparks(level, ground.add(0, 0.2, 0), (int) (4 * s), 0.15);
			case "wind" -> dev.wildercord.cast.ElementFx.gustRing(level, ground, 0.9 * s);
			case "earth" -> dev.wildercord.cast.ElementFx.crack(level, ground, 0.6 * s, 50);
			case "life" -> dev.wildercord.cast.ElementFx.petals(level, ground.add(0, 0.4, 0), 0.4 * s, (int) (3 * s));
			case "void" -> dev.wildercord.cast.ElementFx.implode(level, at, 0.5 * s, 8);
			case "arcane" -> dev.wildercord.cast.ElementFx.shimmer(level, at, 0.3 * s, (int) (3 * s));
			case "time" -> dev.wildercord.cast.ElementFx.goldenTicks(level, at, 0.3 * s, (int) (3 * s));
			case "blood" -> dev.wildercord.cast.ElementFx.drip(level, at, 0.3 * s, (int) (3 * s));
			default -> { }
		}
	}

	/**
	 * HIT: each creature an effect touches. Replaces the generic glow when the effect's signature replaces {@link Phase#HIT}.
	 */
	public static void touched(ServerLevel level, Entity target, Vfx.Theme theme, RuneDef effect, Cast cast) {
		Signature s = Signatures.get(effect.id());
		if (s == null) {
			Vfx.touched(level, target, theme);
			return;
		}
		double scale = Feel.scaleOf(cast.weight(), 0, effect.tier());
		Feel feel = new Feel(Motion.HURL, effect.element(), "", Role.of(effect), Band.of(scale), scale, java.util.Map.of(), "", effect.id());
		Vec3 at = target.getBoundingBox().getCenter();
		if (run(Phase.HIT, feel, theme, level, at, null, target, cast, 0)) {
			Vfx.touched(level, target, theme);
		}
	}
}
