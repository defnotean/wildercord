package dev.wildercord.content;

import dev.wildercord.world.ResidueRules.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How each residue looks and sounds up close, a few soft particles and now and then a quiet sound, made on
 * each client from the block's display ticks (only near a player, never sent). Embers lift off ash, frost
 * glitters over everfrost, storm-glass fizzes, an eddy circles, dust breathes out of riven stone, petals
 * drift from a wildbloom, motes spiral down into a void scar, starlight rises off a glyph, golden grains hang
 * over stilled sand and bloodmoss weeps. The particles are the mod's own glow and material sprites, which
 * stay correct under shader packs.
 */
public final class ResidueAmbience {
	private ResidueAmbience() {}

	/** One display tick of a residue (about twice in five seconds for each block near you). */
	public static void animate(Kind kind, BlockState state, Level level, BlockPos pos, RandomSource r) {
		double x = pos.getX() + 0.5;
		double y = pos.getY();
		double z = pos.getZ() + 0.5;
		switch (kind) {
			case SMOULDERING_ASH -> {
				level.addParticle(material(MaterialOption.EMBER, r.nextBoolean() ? 0xFF8A3A : 0xFFC860, 0.07F + r.nextFloat() * 0.05F, 30 + r.nextInt(20)),
					x + jitter(r, 0.45), y + 0.14, z + jitter(r, 0.45), jitter(r, 0.004), 0.012 + r.nextDouble() * 0.01, jitter(r, 0.004));
				if (r.nextInt(5) == 0) {
					level.addParticle(new MoteOption(MoteOption.CLOUD, 0x8C8580, 0.5F, 50, 0, 0.012F, 0, 0.18F), x + jitter(r, 0.3), y + 0.2, z + jitter(r, 0.3), 0, 0, 0);
				}
				snd(level, pos, r, 6, "fire_smoulder", SoundEvents.CAMPFIRE_CRACKLE, 0.25F);
			}
			case EVERFROST -> {
				level.addParticle(material(MaterialOption.FROST, 0xD8F6FF, 0.06F + r.nextFloat() * 0.04F, 40), x + jitter(r, 0.5), y + 1.05, z + jitter(r, 0.5),
					jitter(r, 0.003), 0.004, jitter(r, 0.003));
				if (r.nextInt(3) == 0) {
					level.addParticle(new MoteOption(MoteOption.GLOW, 0xE6FAFF, 0.06F, 24, 0, 0.002F, 0, 0.01F), x + jitter(r, 0.5), y + 1.02, z + jitter(r, 0.5), 0, 0, 0);
				}
				snd(level, pos, r, 9, "frost_glint", SoundEvents.AMETHYST_BLOCK_CHIME, 0.2F);
			}
			case FULGURITE -> {
				level.addParticle(material(MaterialOption.STORM, r.nextBoolean() ? 0xFFF07A : 0xA8C8FF, 0.05F + r.nextFloat() * 0.04F, 8 + r.nextInt(6)),
					x + jitter(r, 0.25), y + 0.3 + r.nextDouble() * 0.5, z + jitter(r, 0.25), 0, 0, 0);
				if (r.nextInt(4) == 0) {
					level.addParticle(new MoteOption(MoteOption.GLOW, 0xFFFBE0, 0.05F, 10, 0, 0.004F, 0, 0.02F), x + jitter(r, 0.2), y + 0.75, z + jitter(r, 0.2), 0, 0, 0);
				}
				snd(level, pos, r, 5, "storm_fizz", SoundEvents.AMETHYST_CLUSTER_STEP, 0.25F);
			}
			case LINGERING_EDDY -> {
				// Wisps circling the eddy's heart, each set off along the circle.
				for (int i = 0; i < 2; i++) {
					double a = r.nextDouble() * Math.PI * 2;
					double radius = 0.25 + r.nextDouble() * 0.2;
					level.addParticle(material(MaterialOption.WIND, 0xD8FFF0, 0.08F + r.nextFloat() * 0.05F, 24),
						x + Math.cos(a) * radius, y + 0.1 + r.nextDouble() * 0.7, z + Math.sin(a) * radius,
						-Math.sin(a) * 0.05, 0.02 + r.nextDouble() * 0.02, Math.cos(a) * 0.05);
				}
				snd(level, pos, r, 7, "wind_lingering_eddy", SoundEvents.BREEZE_IDLE_GROUND, 0.15F);
			}
			case RIVEN_STONE -> {
				if (r.nextInt(2) == 0) {
					level.addParticle(new MoteOption(MoteOption.GLOW, 0xFFB04A, 0.07F, 40, 0, 0.006F, 0, 0.01F), x + jitter(r, 0.5), y + 1.02, z + jitter(r, 0.5), 0, 0, 0);
				}
				if (r.nextInt(4) == 0) {
					dust(level, pos);
				}
				snd(level, pos, r, 10, "earth_rumble", SoundEvents.TUFF_STEP, 0.25F);
			}
			case WILDBLOOM -> {
				level.addParticle(material(MaterialOption.PETAL, r.nextBoolean() ? 0xFFB0E0 : 0xF4FFD0, 0.06F + r.nextFloat() * 0.04F, 50),
					x + jitter(r, 0.3), y + 0.55 + r.nextDouble() * 0.3, z + jitter(r, 0.3), jitter(r, 0.01), 0.004, jitter(r, 0.01));
				if (r.nextInt(12) == 0) {
					level.addParticle(new MoteOption(MoteOption.BUTTERFLY, 0xE8FFB0, 0.18F, 70, (float) jitter(r, 0.03), 0.01F, (float) jitter(r, 0.03), 0.4F),
						x, y + 0.8, z, 0, 0, 0);
				}
				snd(level, pos, r, 9, "life_bloom", SoundEvents.AZALEA_LEAVES_STEP, 0.2F);
			}
			case VOID_SCAR -> {
				int stage = state.getValue(ResidueBlock.STAGE);
				// Motes falling in from around it, fewer as it closes.
				if (r.nextInt(4) >= stage) {
					double a = r.nextDouble() * Math.PI * 2;
					double d = 1.4 + r.nextDouble() * 1.2;
					float dx = (float) (-Math.cos(a) * d);
					float dz = (float) (-Math.sin(a) * d);
					level.addParticle(new MoteOption(MoteOption.SEEK, r.nextBoolean() ? 0xB45AF0 : 0xE0B0FF, 0.07F, 30, dx, -0.55F, dz, 0.6F),
						x - dx, y + 1.6, z - dz, 0, 0, 0);
				}
				level.addParticle(material(MaterialOption.VOID, 0x1A0830, 0.12F, 20), x + jitter(r, 0.3), y + 1.05, z + jitter(r, 0.3), 0, 0.01, 0);
				snd(level, pos, r, 4, "void_hum", SoundEvents.SCULK_CLICKING, 0.2F);
			}
			case STAR_GLYPH -> {
				level.addParticle(material(MaterialOption.ARCANE, r.nextBoolean() ? 0xFFB8F5 : 0x9A7CFF, 0.06F + r.nextFloat() * 0.04F, 50),
					x + jitter(r, 0.45), y + 0.08, z + jitter(r, 0.45), 0, 0.01 + r.nextDouble() * 0.008, 0);
				if (r.nextInt(3) == 0) {
					level.addParticle(new MoteOption(MoteOption.GLOW, 0xFFD8FA, 0.05F, 30, 0, 0.003F, 0, 0.015F), x + jitter(r, 0.45), y + 0.15, z + jitter(r, 0.45), 0, 0, 0);
				}
				snd(level, pos, r, 8, "arcane_glyph", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.15F);
			}
			case STILLED_SAND -> {
				// Grains caught mid-fall: they hang, barely rising.
				level.addParticle(new MoteOption(MoteOption.GLOW, r.nextBoolean() ? 0xF2D98A : 0xFFF8E0, 0.035F, 60, 0, 0.0015F, 0, 0.002F),
					x + jitter(r, 0.4), y + 0.2 + r.nextDouble() * 0.6, z + jitter(r, 0.4), 0, 0, 0);
				if (r.nextInt(3) == 0) {
					level.addParticle(material(MaterialOption.TIME, 0xF2D98A, 0.07F, 40), x + jitter(r, 0.35), y + 0.25, z + jitter(r, 0.35), 0, 0.006, 0);
				}
				snd(level, pos, r, 9, "time_trickle", SoundEvents.SAND_STEP, 0.2F);
			}
			case BLOODMOSS -> {
				if (r.nextInt(2) == 0) {
					level.addParticle(material(MaterialOption.BLOOD, 0xD2283C, 0.05F + r.nextFloat() * 0.03F, 30), x + jitter(r, 0.45), y + 0.12, z + jitter(r, 0.45),
						0, 0.02, 0);
				}
				snd(level, pos, r, 10, "blood_pulse", SoundEvents.HONEY_BLOCK_STEP, 0.2F);
			}
		}
	}

	/** A puff of dust off riven stone (stepped on, or now and then by itself). */
	public static void dust(Level level, BlockPos pos) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < 3; i++) {
			level.addParticle(new MoteOption(MoteOption.CLOUD, 0xB49470, 0.35F, 30, (float) jitter(r, 0.01), 0.01F, (float) jitter(r, 0.01), 0.22F),
				pos.getX() + 0.5 + jitter(r, 0.45), pos.getY() + 1.05, pos.getZ() + 0.5 + jitter(r, 0.45), 0, 0, 0);
		}
	}

	/** Storm-glass discharging into whatever brushed it (from the server, for everyone near). */
	static void jolt(Level level, BlockPos pos) {
		if (level instanceof ServerLevel server) {
			server.sendParticles(material(MaterialOption.STORM, 0xFFF07A, 0.12F, 8), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.05);
		}
	}

	private static MaterialOption material(int style, int color, float size, int lifetime) {
		return new MaterialOption(style, color, size, lifetime);
	}

	private static double jitter(RandomSource r, double spread) {
		return (r.nextDouble() - 0.5) * 2 * spread;
	}

	/** Now and then (one display tick in {@code oneIn}), the residue's own quiet sound (or a vanilla one before the kit has it). */
	private static void snd(Level level, BlockPos pos, RandomSource r, int oneIn, String kit, SoundEvent fallback, float volume) {
		if (r.nextInt(oneIn) != 0) {
			return;
		}
		SoundEvent event = WildercordSounds.kit(kit);
		level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, event != null ? event : fallback, SoundSource.BLOCKS, volume,
			event != null ? 1.0F : 0.8F + r.nextFloat() * 0.3F, false);
	}
}
