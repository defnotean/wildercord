package dev.wildercord.cast.feel;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.Vfx;
import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Modifiers and links announcing themselves, in the families' own colours: gold for modifiers at the hand as a spell leaves it,
 * violet for links where they hand on, wait or check. Short, small, and never in the element's colour.
 */
public final class Tells {
	private Tells() {}

	public static final int GOLD = RuneColors.MODIFIER;
	public static final int VIOLET = RuneColors.LINK;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	// ------------------------------------------------------------------ modifiers

	/** Every modifier of the spell, by path (the whole spell: its runes as written, Knots untied). */
	private static Map<String, Integer> modifiers(Cast cast, Feel feel) {
		Map<String, Integer> counts = new HashMap<>();
		if (cast.info.spell().isEmpty()) {
			counts.putAll(feel.mods());
			return counts;
		}
		for (RuneDef rune : dev.wildercord.spell.Knots.flatten(cast.info.spell())) {
			if (rune.family() == RuneFamily.MODIFIER) {
				counts.merge(rune.path(), 1, Integer::sum);
			}
		}
		return counts;
	}

	/** The gold tells of a spell's modifiers, at the hand as it leaves: each one seen and heard once. */
	static void modifiers(Cast cast, Feel feel, Vfx.Theme theme, Vec3 hand) {
		Map<String, Integer> mods = modifiers(cast, feel);
		if (mods.isEmpty()) {
			return;
		}
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 look = caster.getLookAngle();
		Vec3 feet = caster.position();
		int note = 0;
		for (Map.Entry<String, Integer> m : mods.entrySet()) {
			int n = Math.min(3, m.getValue());
			switch (m.getKey()) {
				case "amplify" -> {
					for (int i = 0; i < n; i++) {
						Light.ring(level, hand, look, GOLD, 0.15, 0.35 + 0.18 * i, 0.03 + 0.012 * i, 7 + i);
					}
					Feels.sound(level, hand, "tell_rumble", 0.25F * n, 1.0F);
				}
				case "overcharge" -> {
					Sigils.flash(level, hand, GOLD, 1.2F);
					Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, hand, 8, 0.25);
					Light.ring(level, hand, look, 0xFFF4C0, 0.2, 0.9, 0.06, 8);
					Feels.sound(level, hand, "tell_rumble", 0.6F, 0.75F);
				}
				case "focus" -> Light.ring(level, hand, look, GOLD, 0.8, 0.08, 0.04, 8);
				case "widen" -> {
					Light.ring(level, hand, look, GOLD, 0.1, 1.1, 0.035, 8);
					ElementFx.groundRing(level, feet, GOLD, 0.6, 1.9, 0.035, 9);
				}
				case "extend" -> {
					for (int i = 0; i < 5; i++) {
						Vfx.fling(level, new DustParticleOptions(0xE8C878, 0.6F), hand.add((i - 2) * 0.12, 0.25, 0), new Vec3(0, -1, 0), 0.03);
					}
				}
				case "quicken" -> Light.ray(level, hand, hand.add(look.scale(2.2)), GOLD, 0.03, 3);
				case "split" -> {
					Vec3 side = look.cross(UP).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.cross(UP).normalize();
					for (int i = -1; i <= 1; i++) {
						Sigils.layer(level, hand.add(side.scale(0.35 * i)), look, SigilOption.CIRCLE, GOLD, 0.18F, 8, 0.3F);
						Feels.sound(level, hand, "note_mod", 0.3F, Feels.step(2 + 2 * i + 2));
					}
				}
				case "pierce" -> {
					Light.ray(level, hand, hand.add(look.scale(3.0)), 0xFFF4C0, 0.015, 4);
					Feels.sound(level, hand, "tell_zap", 0.3F, 0.75F);
				}
				case "homing" -> Feels.sound(level, hand, "note_mod", 0.35F, Feels.step(3));
				case "chain" -> Feels.sound(level, hand, "tell_zap", 0.3F, 1.0F);
				case "linger" -> Light.ring(level, hand, look, GOLD, 0.9, 0.2, 0.02, 12);
				case "frugal" -> Light.ring(level, hand, look, GOLD, 0.1, 0.3, 0.012, 6);
				case "rapid" -> Feels.sound(level, hand, "tell_tick", 0.4F, 1.26F);
				case "vow" -> {
					ElementFx.flatSigil(level, feet, SigilOption.RING, GOLD, 1.5, 30, 0.05);
					Feels.sound(level, feet, "tell_toll", 0.5F, 1.0F);
				}
				case "blood_price" -> {
					ElementFx.groundRing(level, feet, 0xD2283C, 0.3, 1.4, 0.05, 10);
					Feels.sound(level, hand, "tell_drip", 0.5F, 0.8F);
					Feels.sound(level, feet, "tell_rumble", 0.3F, 0.5F);
				}
				default -> {
					// Execute, Trial Key, Kindled, Unstable, Kindred, Thirst, Belated and add-on modifiers: one gold note each
					// (their own moment comes when they take effect).
					Sigils.flash(level, hand, GOLD, 0.35F);
					Feels.sound(level, hand, "note_mod", 0.3F, Feels.step(note++));
				}
			}
		}
	}

	// ------------------------------------------------------------------ links

	/** A link handing on at a point (On Hit, On Reaction, On Weakness): a violet ring and a ting, the k-th a scale step higher and dimmer. */
	public static void handoff(ServerLevel level, Vec3 at, int k) {
		float dim = k >= 4 ? 0.5F : 1.0F;
		Light.ring(level, at, UP, VIOLET, 0.1, 0.8 * dim, 0.03, 7);
		Feels.sound(level, at, "link_ting", 0.4F * dim, Feels.step(k));
	}

	/** A condition link that held (true) or didn't (false). */
	public static void gate(Cast cast, boolean passed) {
		LivingEntity caster = cast.caster;
		if (passed) {
			ElementFx.groundRing(cast.level, caster.position(), VIOLET, 0.3, 1.2, 0.035, 8);
			Feels.sound(cast.level, caster.position(), "gate_pass", 0.45F, 1.0F);
		} else {
			Feels.sound(cast.level, caster.position(), "gate_fail", 0.3F, 1.0F);
		}
	}

	/** Delay: a fuse, three rising ticks over its wait, and a violet spark at the hand. */
	public static void fuse(Cast cast, int ticks) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.8)).add(0, -0.35, 0);
		Vfx.emit(level, SigilOption.glow(VIOLET, 0.4F), hand, 1, 0.0, 0.0);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(Math.max(0, ticks * i / 3), () -> {
				if (caster.isAlive() && caster.level() == level) {
					Feels.sound(level, caster.position(), "tell_tick", 0.35F, Feels.step(k));
				}
			});
		}
	}

	/** One beat of a Pulse (0, 1, 2): a tick up the scale and a violet ring at the feet. */
	public static void beat(Cast cast, int beat) {
		LivingEntity caster = cast.caster;
		ElementFx.groundRing(cast.level, caster.position(), VIOLET, 0.2, 1.0 + 0.3 * beat, 0.03, 7);
		Feels.sound(cast.level, caster.position(), "tell_tick", 0.4F, Feels.step(beat * 2));
	}

	/** Echo: the replay is announced by a dim violet ripple at the caster and a low ting. */
	public static void echo(Cast cast) {
		LivingEntity caster = cast.caster;
		Light.ring(cast.level, caster.position().add(0, 1, 0), UP, VIOLET, 0.3, 1.4, 0.025, 9);
		Feels.sound(cast.level, caster.position(), "link_ting", 0.35F, 0.75F);
	}

	/** A reactive link armed (On Land, On Hurt, On Low Health): a violet seal at the feet and a clink, seen by everyone near. */
	public static void armed(Cast cast) {
		LivingEntity caster = cast.caster;
		ElementFx.flatSigil(cast.level, caster.position(), SigilOption.RING, VIOLET, 1.1, 16, 0.1);
		Feels.sound(cast.level, caster.position(), "note_link", 0.4F, 0.749F);
	}

	/** A reactive link going off: the ting it hands on with. */
	public static void sprung(Cast cast, Vec3 at) {
		handoff(cast.level, at, 0);
	}

	/** On Kill: a low bell over the fallen. */
	public static void onKill(ServerLevel level, Vec3 at) {
		Feels.sound(level, at, "tell_toll", 0.3F, 1.498F);
	}

	/** Whether a link is one of the conditions (they tell with a gate). */
	public static boolean condition(SpellPlan.Link link) {
		String id = link.link.id();
		return id.equals(Runes.IF_SNEAKING.id()) || id.equals(Runes.IF_AIRBORNE.id()) || id.equals(Runes.COMBO.id()) || id.equals(Runes.IF_WOUNDED.id())
			|| id.equals(Runes.IF_OUTNUMBERED.id()) || id.equals(Runes.IF_WET.id());
	}
}
