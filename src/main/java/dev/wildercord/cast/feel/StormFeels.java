package dev.wildercord.cast.feel;

import dev.wildercord.cast.Vfx;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;

/**
 * The signatures of the storm runes (docs/audit/spell-feel-storm-earth.md, table 2D). One owner per file. Each rune gets an
 * accent of its own and, where the first 200 ms should already say which rune it is, a cue; the rest of each rune's look
 * and sound lives in its effect code (the tells its mechanics need, the lasting cues of timed effects).
 */
final class StormFeels {
	private StormFeels() {}

	static final int WHITE = 0xFFFBE0;
	static final int BLUE = 0xA8C8FF;

	static void register() {
		Signature.of("thunder_tide").accent(0x91DDEE).sound(Phase.CUE,"storm_dive",.4F,.84F).register();
		Signature.of("thunder_walk").accent(0xD8FBE2).sound(Phase.CUE,"storm_dive",.35F,1.26F).register();
		// Conduit: a rod driven into the floor, a short grounded zap.
		Signature.of("conduit").motion(Motion.SEAL).accent(0xC8F0FF).sound(Phase.CUE,"storm_zap",.45F,.75F).register();
		// A tether that seeks conductors: white-hot, a dry zap at the hand.
		Signature.of("shock").accent(WHITE).sound(Phase.CUE, "storm_zap", 0.5F, 1.0F).register();
		// The counter-spell: pale blue, a lower zap; the stun ring and clamp are its own (StormEarthFx.stunRing).
		Signature.of("jolt").accent(BLUE).sound(Phase.CUE, "storm_zap", 0.5F, 0.75F).register();
		// The stun ring: its flash has a whine of its own before the crack (in the effect).
		Signature.of("thunderclap").accent(BLUE).register();
		// The sky strike: a flash-whine at the hand, then the crack and the roll where it lands.
		Signature.of("lightning").accent(WHITE).scale(1.1).sound(Phase.CUE, "storm_flash", 0.7F, 1.0F).register();
		// Sunlight: gold, warm, no storm in it.
		Signature.of("ripple").accent(0xFFD050).sound(Phase.CUE, "storm_sun", 0.4F, 2.0F).register();
		Signature.of("thunderbird").accent(WHITE).register();
		Signature.of("stormheart").accent(BLUE).register();
		// A spark of power: redstone red.
		Signature.of("galvanize").accent(0xFF3A2A).sound(Phase.CUE, "storm_pip", 0.6F, 1.0F).register();
		// Strike, fling, strike: storm with a wind accent.
		Signature.of("tempest").accent(0xC8F0DC).sound(Phase.CUE, "storm_gale", 0.4F, 1.26F).register();
		// A steady beam in a violet sheath; its touch is violet too, not the storm's yellow.
		Signature.of("plasma").accent(0xD070FF).sound(Phase.CUE, "storm_sizzle", 0.4F, 1.26F)
			.replace(Phase.HIT).hook(Phase.HIT, ctx -> {
				Vfx.emit(ctx.level(), SigilOption.glow(0xD070FF, 0.9F), ctx.at(), 1, 0.0, 0.0);
				Vfx.emit(ctx.level(), ParticleTypes.ELECTRIC_SPARK, ctx.at(), 2, 0.3, 0.02);
			}).register();
		// Lightning in the hands: storm with a life accent, a rising whine.
		Signature.of("surge").accent(0x6EDC64).sound(Phase.CUE, "storm_whine", 0.6F, 1.0F).register();
		Signature.of("magnetize").accent(0xE8C060).register();
		Signature.of("riftbolt").accent(0xB45AF0).register();
		Signature.of("stormweave").accent(0xE678DC).register();
		Signature.of("stormclock").accent(0xF2D98A).sound(Phase.CUE, "storm_tick", 0.6F, 1.26F).register();
		Signature.of("thunderhead").accent(0x8A9AB0).register();
		// A current through the cold: frost-blue, so its touch is blue as well.
		Signature.of("frostwire").accent(0x8CDCFF).replace(Phase.HIT).hook(Phase.HIT, ctx -> {
			Vfx.emit(ctx.level(), SigilOption.glow(0x8CDCFF, 0.9F), ctx.at(), 1, 0.0, 0.0);
			Vfx.emit(ctx.level(), ParticleTypes.SNOWFLAKE, ctx.at(), 3, 0.3, 0.02);
		}).register();
		Signature.of("thunderstep").accent(WHITE).register();
		// ---- fx-explore pack
		Signature.of("sky_reading").accent(0xBFD0E8).sound(Phase.CUE, "storm_pip", 0.5F, 1.0F).register();
	}
}
