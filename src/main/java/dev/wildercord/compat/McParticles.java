package dev.wildercord.compat;

import dev.wildercord.compat.McParticles;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import org.joml.Vector3f;

/**
 * 1.21.1 has no {@code SpellParticleOption} or {@code PowerParticleOption} (both arrive with 1.21.9):
 * a tinted spell sparkle is a dust particle there, and a power pulse is its plain particle type.
 */
public final class McParticles {
	private McParticles() {}

	/** A tinted dust particle from a raw colour vector. */
	public static DustParticleOptions dust(Vector3f color, float scale) {
		return new DustParticleOptions(color, scale);
	}

	/** A tinted dust particle: 1.21.1's dust ctor takes a Vector3f colour, not an int. */
	public static DustParticleOptions dust(int argb, float scale) {
		int rgb = argb & 0xFFFFFF;
		return McParticles.dust(new Vector3f((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F), scale);
	}

	/** A particle in the caster's colour: on 1.21.8 the tint rides on a dust particle. */
	public static ParticleOptions spell(ParticleType<?> type, int argb, float scale) {
		return dust(argb, scale);
	}

	/** A pulse of raw power: the particle type itself, uncoloured. */
	public static ParticleOptions power(SimpleParticleType type, float scale) {
		return type;
	}

	/** The colour a particle carries, or -1 when it carries none. */
	public static int colorOf(ParticleOptions options) {
		if (options instanceof DustParticleOptions dust) {
			Vector3f c = dust.getColor();
			return ((int) (c.x() * 255) & 255) << 16 | ((int) (c.y() * 255) & 255) << 8 | ((int) (c.z() * 255) & 255);
		}
		return -1;
	}
}
