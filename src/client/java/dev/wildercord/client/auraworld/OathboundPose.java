package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.OathboundTechnique;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The shared Oathbound pose vocabulary. Each rig hands in the parts it actually has; the archetype then drives them with
 * the technique's own timings, so the same "overhead slam" reads as a mace drop on the Bastion, a greatsword plunge on the
 * Blade-Champion, a halberd crash on the Halberdier, and a sunblade judgment on the Banner-Marshal.
 */
public final class OathboundPose {
	private OathboundPose() {}

	/** Arm, weapon, and leg references a rig exposes to the shared poses. */
	public record Rig(
			ModelPart body, ModelPart head,
			ModelPart right, ModelPart foreR, ModelPart left, ModelPart foreL,
			ModelPart weapon, ModelPart offhand,
			ModelPart legL, ModelPart shinL, ModelPart legR, ModelPart shinR) {}

	/**
	 * Applies {@code archetype} for a move of {@code duration} ticks with {@code tell} ticks of windup. {@code style} is the
	 * move id, used only to mirror the lead side and nudge the amplitude so neighbouring attacks do not look identical.
	 */
	public static void apply(Rig r, OathboundTechnique.Archetype archetype, float el, float tell, float duration,
			int style) {
		// Five different windup curves: some moves coil slowly and snap, others are already moving by the time you read them.
		float windPow = 0.74F + (style % 5) * 0.14F;
		float wind = (float) Math.pow(ramp(el, 0.0F, Math.max(1.0F, tell)), windPow);
		float hit = ramp(el, tell, 3.0F);
		float rec = smooth(el, tell + Math.max(4.0F, tell * 0.35F), Math.max(tell + 5.0F, duration));
		float act = hit * (1.0F - rec);
		float amp = 0.84F + (style % 4) * 0.08F;
		boolean mirror = (style & 1) == 0;
		// Per-move offsets so two attacks of the same archetype never read as the same swing.
		float lean = (((style * 5) % 4) - 1.5F) * 0.055F;
		float roll = (((style * 11) % 5) - 2.0F) * 0.05F;
		float sway = (((style * 13) % 7) - 3.0F) * 0.045F;

		switch (archetype) {
			case OVERHEAD_SLAM -> {
				float up = -2.15F * amp;
				r.body().xRot = Mth.lerp(act, -0.16F * wind, 0.34F * amp);
				r.head().xRot = Mth.lerp(act, 0.10F * wind, -0.20F);
				r.right().xRot = Mth.lerp(act, up, 0.35F);
				r.left().xRot = Mth.lerp(act, up * 0.92F, 0.28F);
				r.right().yRot = -0.22F * act;
				r.left().yRot = 0.24F * act;
				r.foreR().xRot = Mth.lerp(act, -0.30F, -0.10F);
				r.foreL().xRot = Mth.lerp(act, -0.24F, -0.08F);
				r.weapon().xRot = Mth.lerp(act, 2.25F * wind, -1.15F);
				legs(r, 0.0F);
				bend(r, 0.34F * act);
			}
			case HORIZONTAL_SWEEP -> {
				float side = mirror ? 1.0F : -1.0F;
				r.body().yRot = Mth.lerp(act, -0.72F * wind * side, 0.92F * amp * side);
				r.body().xRot = Mth.lerp(act, -0.05F, 0.16F);
				r.head().yRot = -r.body().yRot * 0.55F;
				r.right().xRot = Mth.lerp(act, -0.72F, -0.58F);
				r.right().yRot = Mth.lerp(act, 0.55F * wind * side, -0.78F * side);
				r.left().xRot = -0.55F * act;
				r.left().yRot = 0.35F * act;
				r.weapon().xRot = Mth.lerp(act, 1.95F, 1.45F);
				r.weapon().zRot = Mth.lerp(act, -0.85F * wind * side, 0.65F * side);
				legs(r, 0.22F * (1.0F - rec));
			}
			case DIAGONAL_SLASH -> {
				float side = mirror ? 1.0F : -1.0F;
				r.body().yRot = Mth.lerp(act, 0.55F * wind * side, -0.62F * amp * side);
				r.body().xRot = Mth.lerp(act, -0.10F * wind, 0.22F);
				r.head().yRot = -r.body().yRot * 0.4F;
				r.right().xRot = Mth.lerp(act, -1.55F * wind, -0.28F);
				r.right().yRot = Mth.lerp(act, 0.30F * side, -0.55F * side);
				r.left().xRot = Mth.lerp(act, -1.35F * wind, -0.20F);
				r.foreR().xRot = Mth.lerp(act, -0.55F, -0.12F);
				r.weapon().xRot = Mth.lerp(act, 1.25F, -0.55F);
				r.weapon().zRot = Mth.lerp(act, 0.85F * wind * side, -0.55F * side);
				legs(r, 0.18F * act);
			}
			case THRUST -> {
				r.body().xRot = Mth.lerp(act, 0.10F * wind, 0.30F);
				r.body().yRot = Mth.lerp(act, 0.26F * wind, -0.10F);
				r.head().xRot = Mth.lerp(act, -0.10F, -0.22F);
				r.right().xRot = Mth.lerp(act, -0.85F, -1.32F);
				r.right().yRot = Mth.lerp(act, -0.10F, -0.32F);
				r.left().xRot = Mth.lerp(act, -0.72F, -1.18F);
				r.left().yRot = Mth.lerp(act, 0.18F, 0.42F);
				r.right().zRot = 0.10F;
				r.foreR().xRot = Mth.lerp(act, -0.85F, -0.28F);
				r.foreL().xRot = Mth.lerp(act, -0.72F, -0.24F);
				r.weapon().xRot = Mth.lerp(act, 0.90F, 0.42F);
				r.weapon().zRot = Mth.lerp(act, 0.18F, -0.30F);
				lunge(r, act, mirror);
			}
			case UPPERCUT -> {
				bend(r, 0.42F * wind * (1.0F - act));
				r.body().xRot = Mth.lerp(act, 0.24F * wind, -0.30F);
				r.head().xRot = Mth.lerp(act, 0.12F, -0.28F);
				r.right().xRot = Mth.lerp(act, 0.62F * wind, -2.35F * amp);
				r.right().yRot = Mth.lerp(act, -0.10F, -0.30F);
				r.left().xRot = Mth.lerp(act, 0.45F * wind, -0.75F);
				r.foreR().xRot = Mth.lerp(act, -0.90F, -0.18F);
				r.weapon().xRot = Mth.lerp(act, 0.55F, -0.75F);
				r.weapon().zRot = Mth.lerp(act, 0.30F, -0.20F);
				legs(r, 0.12F);
			}
			case SPIN_SWEEP -> {
				float spins = 1.0F + (style % 2);
				r.body().yRot = act * Mth.TWO_PI * spins;
				r.head().yRot = -r.body().yRot * 0.25F;
				r.body().xRot = 0.08F;
				r.right().xRot = -0.55F;
				r.right().zRot = 1.18F * act + 0.20F;
				r.left().xRot = -0.45F;
				r.left().zRot = -1.18F * act - 0.20F;
				r.weapon().xRot = Mth.lerp(act, 1.35F, 1.55F);
				r.weapon().zRot = 0.0F;
				legs(r, 0.10F * act);
				r.legL().yRot = 0.25F * act;
				r.legR().yRot = -0.25F * act;
			}
			case LEAP_SLAM -> {
				float air = smooth(el, tell - 2.0F, tell + 2.0F) * (1.0F - smooth(el, tell + 7.0F, tell + 12.0F));
				bend(r, 0.48F * wind * (1.0F - air));
				r.body().xRot = Mth.lerp(air, 0.25F * wind, -0.28F);
				r.head().xRot = Mth.lerp(air, 0.16F, -0.20F);
				r.right().xRot = Mth.lerp(air, -1.55F * wind, -2.55F * amp);
				r.left().xRot = Mth.lerp(air, -1.40F * wind, -2.45F * amp);
				r.foreR().xRot = Mth.lerp(act, -0.45F, -0.10F);
				r.foreL().xRot = Mth.lerp(act, -0.40F, -0.08F);
				r.weapon().xRot = Mth.lerp(act, 2.35F, -1.35F);
				r.legL().xRot = Mth.lerp(air, -0.45F, -0.95F);
				r.legR().xRot = Mth.lerp(air, 0.45F, 0.85F);
				r.shinL().xRot = Mth.lerp(air, 0.85F, 1.35F);
				r.shinR().xRot = Mth.lerp(air, 0.55F, 0.95F);
			}
			case SHIELD_PUSH -> {
				r.body().xRot = Mth.lerp(act, 0.05F, 0.26F);
				r.body().yRot = Mth.lerp(act, -0.18F * wind, 0.12F);
				r.head().xRot = Mth.lerp(act, 0.0F, -0.18F);
				r.left().xRot = Mth.lerp(act, -0.70F, -1.55F);
				r.left().yRot = Mth.lerp(act, 0.28F, 0.12F);
				r.foreL().xRot = Mth.lerp(act, -0.55F, -0.18F);
				r.right().xRot = Mth.lerp(act, 0.35F, -0.30F);
				r.weapon().xRot = Mth.lerp(act, 0.65F, 0.95F);
				if (r.offhand() != null) {
					r.offhand().xRot = Mth.lerp(act, -0.15F, -0.55F);
					r.offhand().zRot = Mth.lerp(act, 0.10F, -0.10F);
				}
				bend(r, 0.28F * act);
			}
			case STOMP -> {
				float lift = Math.max(0.0F, Mth.sin(wind * Mth.PI));
				bend(r, 0.20F * wind * (1.0F - act));
				r.body().xRot = Mth.lerp(act, -0.10F * wind, 0.34F);
				r.head().xRot = Mth.lerp(act, 0.08F, 0.22F);
				r.legR().xRot = -1.55F * lift;
				r.shinR().xRot = 1.45F * lift;
				r.right().xRot = Mth.lerp(act, -0.30F, 0.42F);
				r.left().xRot = Mth.lerp(act, -0.30F, 0.42F);
				r.weapon().xRot = Mth.lerp(act, 0.30F, 0.85F);
				bend(r, 0.26F * act);
			}
			case ROAR -> {
				float swell = Math.max(wind, act);
				r.body().xRot = -0.22F * swell;
				r.head().xRot = -0.42F * swell;
				r.right().xRot = -0.55F - 0.55F * swell;
				r.right().zRot = 0.75F * swell + 0.28F;
				r.left().xRot = -0.55F - 0.55F * swell;
				r.left().zRot = -0.75F * swell - 0.28F;
				r.foreR().xRot = -0.35F;
				r.foreL().xRot = -0.35F;
				r.weapon().xRot = Mth.lerp(swell, 1.15F, 0.35F);
				r.weapon().zRot = 0.35F * swell;
				if (r.offhand() != null) {
					r.offhand().xRot = 0.20F * swell;
				}
				legs(r, 0.10F * swell);
			}
			case OVERHAND_THROW -> {
				r.body().yRot = Mth.lerp(act, 0.35F * wind, -0.42F);
				r.body().xRot = Mth.lerp(act, -0.12F * wind, 0.26F);
				r.head().yRot = -r.body().yRot * 0.5F;
				r.right().xRot = Mth.lerp(act, -2.35F * wind, -0.55F);
				r.right().yRot = Mth.lerp(act, -0.10F, -0.55F);
				r.left().xRot = Mth.lerp(act, -1.20F, -0.35F);
				r.foreR().xRot = Mth.lerp(act, -0.70F, -0.15F);
				r.weapon().xRot = Mth.lerp(act, 1.85F, 0.35F);
				r.weapon().zRot = Mth.lerp(act, 0.55F, -0.45F);
				lunge(r, act, mirror);
			}
			case GUARD_BRACE -> {
				bend(r, 0.34F * Math.max(wind, act));
				r.body().xRot = 0.18F * Math.max(wind, act);
				r.head().xRot = -0.10F * act;
				r.left().xRot = Mth.lerp(act, -1.10F, -1.65F);
				r.left().yRot = 0.55F;
				r.foreL().xRot = Mth.lerp(act, -0.85F, -0.75F);
				r.right().xRot = Mth.lerp(act, -0.20F, 0.35F);
				r.right().zRot = 0.30F;
				r.weapon().xRot = 1.20F;
				if (r.offhand() != null) {
					r.offhand().xRot = -0.45F;
					r.offhand().zRot = 0.0F;
				}
				legs(r, 0.16F * Math.max(wind, act));
			}
			case CAST_RAISE -> {
				float raise = Math.max(wind, act);
				r.body().xRot = -0.10F * raise;
				r.head().xRot = -0.34F * raise;
				r.right().xRot = -2.05F * raise + 0.10F;
				r.right().yRot = -0.20F * raise;
				r.left().xRot = -2.05F * raise + 0.10F;
				r.left().yRot = 0.20F * raise;
				r.foreR().xRot = Mth.lerp(raise, -1.20F, -0.20F);
				r.foreL().xRot = Mth.lerp(raise, -1.20F, -0.20F);
				r.weapon().xRot = Mth.lerp(raise, 1.15F, 0.15F);
				r.weapon().zRot = 0.10F * raise;
				if (r.offhand() != null) {
					r.offhand().xRot = -0.30F * raise;
				}
				legs(r, 0.10F * raise);
			}
			case FLURRY -> {
				float beat = Mth.sin(el * 1.35F);
				r.body().yRot = 0.22F * beat * Math.max(act, 0.35F);
				r.head().yRot = -r.body().yRot * 0.4F;
				r.right().xRot = -1.05F + 0.55F * beat;
				r.left().xRot = -1.05F - 0.55F * beat;
				r.right().yRot = -0.20F - 0.25F * beat;
				r.left().yRot = 0.20F + 0.25F * beat;
				r.foreR().xRot = -0.35F;
				r.foreL().xRot = -0.35F;
				r.weapon().xRot = Mth.lerp(Math.max(act, 0.25F), 1.15F, 0.55F + 0.30F * beat);
				r.weapon().zRot = 0.35F * beat;
				legs(r, 0.12F);
			}
			case BACKSTEP -> {
				float back = Math.max(wind, act);
				r.body().xRot = -0.16F * back;
				r.head().xRot = 0.10F * back;
				r.right().xRot = -0.85F - 0.20F * back;
				r.left().xRot = -0.60F - 0.20F * back;
				r.weapon().xRot = Mth.lerp(back, 1.15F, 0.75F);
				r.legL().xRot = -0.55F * back;
				r.legR().xRot = 0.55F * back;
				r.shinL().xRot = 0.62F * back;
				r.shinR().xRot = 0.30F * back;
			}
			case WEAPON_TWIRL -> {
				float twirl = act * Mth.TWO_PI * 2.0F;
				r.right().xRot = -1.10F;
				r.right().zRot = 0.55F + 0.35F * Mth.sin(el * 0.8F);
				r.left().xRot = -0.55F;
				r.weapon().xRot = Mth.lerp(act, 1.55F, 1.55F) + 0.35F * Mth.sin(twirl);
				r.weapon().zRot = twirl;
				r.body().yRot = 0.25F * act;
				legs(r, 0.10F * act);
			}
			case KNEEL_STRIKE -> {
				float kneel = Math.max(wind, act);
				r.legR().xRot = -1.15F * kneel;
				r.shinR().xRot = 1.85F * kneel;
				r.legL().xRot = -0.30F * kneel;
				r.shinL().xRot = 0.65F * kneel;
				r.body().xRot = Mth.lerp(act, -0.10F, 0.45F);
				r.head().xRot = Mth.lerp(act, 0.10F, 0.30F);
				r.right().xRot = Mth.lerp(act, -1.45F, 0.35F);
				r.left().xRot = Mth.lerp(act, -1.20F, 0.25F);
				r.foreR().xRot = Mth.lerp(act, -0.50F, -0.10F);
				r.weapon().xRot = Mth.lerp(act, 1.75F, -0.95F);
				bend(r, 0.10F * kneel);
			}
			case CROSS_CUT -> {
				float side = mirror ? 1.0F : -1.0F;
				// First diagonal winds across the chest, the return cuts the opposite way.
				float first = ramp(el, tell * 0.35F, Math.max(2.0F, tell * 0.5F)) * (1.0F - ramp(el, tell + 2.0F, 3.0F));
				float second = ramp(el, tell + 1.0F, 3.0F) * (1.0F - rec);
				r.body().yRot = Mth.lerp(second, -0.55F * first * side, 0.70F * side);
				r.body().xRot = 0.12F * (first + second);
				r.head().yRot = -r.body().yRot * 0.45F;
				r.right().xRot = Mth.lerp(second, -1.70F * first, -0.35F);
				r.left().xRot = Mth.lerp(second, -1.55F * first, -0.30F);
				r.foreR().xRot = Mth.lerp(second, -0.55F, -0.10F);
				r.foreL().xRot = Mth.lerp(second, -0.45F, -0.10F);
				r.weapon().xRot = Mth.lerp(second, 1.35F, -0.35F);
				r.weapon().zRot = Mth.lerp(second, 0.95F * side, -0.85F * side);
				legs(r, 0.18F * (first + second));
			}
		}
		// Posture bias every archetype inherits: a slight body roll, a weapon jaw angle, and a head counter-tilt.
		float live = 0.35F + 0.65F * act;
		r.body().xRot += lean * live;
		r.body().zRot += roll * live;
		r.head().zRot -= roll * 0.55F * live;
		r.weapon().yRot += sway * (0.4F + 0.6F * Math.max(wind, act));
	}

	private static void legs(Rig r, float spread) {
		r.legL().zRot = -0.10F - spread;
		r.legR().zRot = 0.10F + spread;
	}

	private static void lunge(Rig r, float act, boolean mirror) {
		float lead = mirror ? 1.0F : -1.0F;
		r.legL().xRot = Mth.lerp(act, 0.0F, -0.62F * lead);
		r.shinL().xRot = Mth.lerp(act, 0.0F, 0.82F * Math.max(0.0F, lead));
		r.legR().xRot = Mth.lerp(act, 0.0F, 0.62F * lead);
		r.shinR().xRot = Mth.lerp(act, 0.0F, 0.82F * Math.max(0.0F, -lead));
	}

	private static void bend(Rig r, float k) {
		r.legL().xRot -= k;
		r.legR().xRot -= k;
		r.shinL().xRot += 2.0F * k;
		r.shinR().xRot += 2.0F * k;
	}

	private static float ramp(float t, float from, float ticks) {
		float k = Mth.clamp((t - from) / Math.max(1.0F, ticks), 0.0F, 1.0F);
		return 1.0F - (1.0F - k) * (1.0F - k);
	}

	private static float smooth(float t, float from, float to) {
		float k = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}
}
