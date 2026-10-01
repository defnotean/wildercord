package dev.wildercord.cast;

import com.mojang.math.Transformation;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.feel.MarkHalos;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * How each twist of a world's resonances looks and sounds (see {@link TwistMagic} for what they do). Built from the
 * element languages ({@link ElementFx}), shaped light, motes and magic circles like every other spell, with a voice of
 * its own from the sound kit for each; block displays (flowers, standing stones, a thicket) grow, hold and sink away
 * without touching the world, as {@link BlockFx}'s do.
 */
final class TwistVfx {
	private TwistVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	// ------------------------------------------------------------------ waking, and almost waking

	/** A resonance found: its colour bursting out of the caster in a rising ring, a star seal at their feet. */
	static void revealed(ServerLevel level, Entity caster, int color) {
		Vec3 feet = caster.position();
		ElementFx.flatSigil(level, feet, SigilOption.STAR, color, 2.6, 50, 0.04);
		ElementFx.flatSigil(level, feet, SigilOption.RING, 0xFFF4D8, 3.4, 46, -0.03);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(i * 6, () -> ElementFx.ring(level, feet.add(0, 0.2 + k * 0.9, 0), UP, k == 1 ? 0xFFF4D8 : color, 0.4, 2.4 - k * 0.4, 0.05, 14));
		}
		Motes.burst(level, caster.getBoundingBox().getCenter(), 24, color, 0.16, 40, 0.18);
		Motes.glows(level, feet.add(0, 0.4, 0), 18, 1.2, 0xFFF4D8, 0.12, 50, new Vec3(0, 0.05, 0), 0.02);
	}

	/** A resonance held back by a condition: a faint shimmer, as if the world almost answered. */
	static void almost(ServerLevel level, Entity caster, int color) {
		Motes.glows(level, caster.position().add(0, 0.2, 0), 8, 0.8, color, 0.08, 30, new Vec3(0, 0.03, 0), 0.01);
		Feels.sound(level, caster.position(), "gate_fail", 0.35F, 1.3F);
	}

	/** A quirk met: a soft glint at the spell's landing, in its element's colour. */
	static void quirk(ServerLevel level, Vec3 at, int color) {
		Sigils.flash(level, at, color, 0.9F);
		ElementFx.ring(level, at, UP, color, 0.2, 1.1, 0.03, 10);
		Motes.glows(level, at, 6, 0.4, color, 0.1, 24, new Vec3(0, 0.04, 0), 0.02);
		Feels.sound(level, at, "ready_ping", 0.5F, Feels.step(3));
	}

	// ------------------------------------------------------------------ the twists

	/** Glass Rain: a strike lands as falling glass and shatters. */
	static void glass(ServerLevel level, Vec3 at) {
		BlockParticleOption glass = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState());
		Vfx.emit(level, glass, at.add(0, 0.3, 0), 14, 0.5, 0.15);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GLASS.defaultBlockState()), at.add(0, 2.5, 0), 8, 0.6, 0.0);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 6; i++) {
			Vec3 dir = ElementFx.flatDir(r.nextDouble() * Math.PI * 2).add(0, 0.3 + r.nextDouble() * 0.5, 0).normalize();
			ElementFx.ray(level, at.add(0, 0.1, 0), at.add(dir.scale(0.6 + r.nextDouble() * 0.6)), i % 2 == 0 ? 0xE8FCFF : 0xB8E8FF, 0.03, 6);
		}
		ElementFx.ring(level, at.add(0, 0.08, 0), UP, 0xE8FCFF, 0.2, 1.6, 0.03, 8);
		Feels.sound(level, at, "frost_glass_fall", 0.8F, 1.0F);
	}

	/** Kindly Flame: warm tongues of gold and rose round an ally it mends. */
	static void kindly(ServerLevel level, Entity ally) {
		Vec3 feet = ally.position();
		ElementFx.tongues(level, feet, ally.getBbWidth() * 0.6, ally.getBbHeight(), 6, 0xFFC870, 0xFF9AB0, 5, 16);
		Vfx.emit(level, ParticleTypes.HEART, feet.add(0, ally.getBbHeight() + 0.3, 0), 2, 0.3, 0.0);
		Motes.glows(level, feet.add(0, 0.5, 0), 8, 0.4, 0xFFD890, 0.1, 30, new Vec3(0, 0.05, 0), 0.02);
		Feels.sound(level, feet, "fire_kindly", 0.7F, 1.0F);
	}

	/** Birds of Light: the burst where it strikes, wings scattering. */
	static void birdsTakeOff(ServerLevel level, Vec3 at, int color) {
		Sigils.flash(level, at, color, 1.6F);
		for (int i = 0; i < 6; i++) {
			Motes.butterfly(level, at, i % 2 == 0 ? color : 0xFFFFFF, 0.5, 26, ElementFx.flatDir(i * Math.PI / 3).scale(0.12).add(0, 0.08, 0));
		}
		Feels.sound(level, at, "arcane_wingflock", 0.9F, 1.0F);
	}

	/** One bird of light flying from {@code from} to its prey. */
	static void bird(ServerLevel level, Vec3 from, Vec3 to, int color, int ticks) {
		Motes.seek(level, from, to, color, 0.24, ticks, 0.6);
		Motes.butterfly(level, from, color, 0.35, ticks, to.subtract(from).scale(1.0 / ticks));
	}

	/** A bird striking: a small bright peck. */
	static void peck(ServerLevel level, Vec3 at, int color) {
		Sigils.flash(level, at, color, 0.7F);
		Motes.burst(level, at, 6, color, 0.08, 14, 0.08);
	}

	/** Winter Blossom: pale flowers grow out of the frost in a ring, hold, and sink back. */
	static void blossom(ServerLevel level, Vec3 at) {
		Vec3 ground = groundAt(level, at);
		BlockState[] flowers = {Blocks.BLUE_ORCHID.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState(),
			Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState()};
		RandomSource r = level.getRandom();
		for (int i = 0; i < 7; i++) {
			double a = Math.PI * 2 * i / 7 + r.nextDouble() * 0.4;
			double d = i == 0 ? 0 : 0.8 + r.nextDouble() * 1.1;
			rise(level, ground.add(Math.cos(a) * d, 0, Math.sin(a) * d), flowers[i % flowers.length], 0.8F, 0.8F, 50 + r.nextInt(12));
		}
		ElementFx.frostCreep(level, ground, 2.0, 40);
		ElementFx.petals(level, ground.add(0, 0.5, 0), 1.2, 10);
		Motes.glows(level, ground.add(0, 0.3, 0), 10, 1.0, 0xDCEEFF, 0.1, 40, new Vec3(0, 0.03, 0), 0.01);
		Feels.sound(level, ground, "life_frost_bloom", 0.8F, 1.0F);
	}

	/** Upfall: an upturned circle overhead, motes falling up into it. */
	static void upfallOmen(ServerLevel level, Vec3 at, double radius) {
		Vec3 sky = at.add(0, 5.5, 0);
		ElementFx.sigil(level, sky, new Vec3(0, -1, 0), SigilOption.CRACKED, 0xB49CFF, radius, 40, 0.05);
		ElementFx.sigil(level, sky.add(0, -0.02, 0), new Vec3(0, -1, 0), SigilOption.RING, 0xE0D0FF, radius * 1.25, 40, -0.04);
		Motes.glows(level, at.add(0, 0.2, 0), 26, radius * 0.5, 0xC8B4FF, 0.12, 40, new Vec3(0, 0.12, 0), 0.02);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()), at, 10, radius * 0.5, 0.0);
		Feels.sound(level, at, "void_upfall", 0.9F, 1.0F);
	}

	/** One foe falling up: a thread of violet light under it. */
	static void floating(ServerLevel level, Entity target) {
		Motes.glow(level, target.position(), 0xC8B4FF, 0.1, 12, new Vec3(0, 0.1, 0), 0.01);
	}

	/** Upfall's end: the foe comes crashing back down. */
	static void slam(ServerLevel level, Entity target) {
		ElementFx.ring(level, target.position().add(0, 0.1, 0), UP, 0xB49CFF, 0.2, 1.6, 0.05, 9);
		Fx.sound(level, target.position(), net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND, 0.6F, 1.2F);
	}

	/** Pale Steed: the horse rises out of a pale circle. */
	static void steedRises(ServerLevel level, Entity horse) {
		Vec3 feet = horse.position();
		ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, 0xD8F0FF, 2.2, 30, 0.05);
		Motes.clouds(level, feet.add(0, 0.6, 0), 10, 0.8, 0xE8F4FF, 1.1, 40, new Vec3(0, 0.03, 0), 0.02, 0.45);
		Motes.burst(level, horse.getBoundingBox().getCenter(), 16, 0xD8F0FF, 0.12, 30, 0.15);
		Feels.sound(level, feet, "wind_spirit_gallop", 0.9F, 1.0F);
	}

	/** Pale Steed in stride: wisps trailing from its hooves. */
	static void steedTrail(ServerLevel level, Entity horse) {
		Vec3 feet = horse.position();
		Motes.clouds(level, feet.add(0, 0.3, 0), 2, 0.4, 0xE8F4FF, 0.7, 24, new Vec3(0, 0.02, 0), 0.01, 0.35);
		Motes.glow(level, feet.add(0, 1.0, 0), 0xD8F0FF, 0.1, 20, new Vec3(0, 0.02, 0), 0.03);
	}

	/** Pale Steed fading. */
	static void steedFades(ServerLevel level, Entity horse) {
		Motes.clouds(level, horse.getBoundingBox().getCenter(), 12, 0.7, 0xE8F4FF, 1.2, 36, new Vec3(0, 0.04, 0), 0.02, 0.5);
		Motes.burst(level, horse.getBoundingBox().getCenter(), 14, 0xD8F0FF, 0.1, 26, 0.12);
		Fx.sound(level, horse.position(), net.minecraft.sounds.SoundEvents.SKELETON_HORSE_AMBIENT, 0.5F, 1.4F);
	}

	/** Second Voice: the spell's own circle opens again where it landed. */
	static void secondVoice(ServerLevel level, Vec3 at, List<dev.wildercord.spell.RuneDef> runes, int color) {
		Sigils.spell(level, at.add(0, 0.1, 0), UP, runes, color, 1.6F, 24);
		ElementFx.ring(level, at.add(0, 0.12, 0), UP, 0xF2D98A, 0.3, 2.4, 0.04, 12);
		Feels.sound(level, at, "time_reecho", 0.9F, 1.0F);
	}

	/** Slow Hour: a clock face round the caster, its hands dragging. */
	static void slowHour(ServerLevel level, Entity caster, boolean start) {
		Vec3 at = caster.position().add(0, 0.1, 0);
		ElementFx.clock(level, at, UP, 3.0, 20, true);
		if (start) {
			ElementFx.flatSigil(level, at, SigilOption.RING, 0xF2E2A0, 8.0, 120, 0.004);
			ElementFx.goldenTicks(level, caster.getBoundingBox().getCenter(), 1.2, 14);
			Feels.sound(level, at, "time_slow_hour", 0.9F, 1.0F);
		}
	}

	/** An arrow caught in the slow hour: golden flecks hanging round it. */
	static void slowed(ServerLevel level, Entity projectile) {
		Vfx.emit(level, ParticleTypes.WAX_ON, projectile.position(), 1, 0.1, 0.0);
		Motes.glow(level, projectile.position(), 0xF2E2A0, 0.07, 10, Vec3.ZERO, 0.0);
	}

	/** Paper Storm: a whirl of pages and runes round the caster. */
	static void pages(ServerLevel level, Entity caster, int pulse) {
		Vec3 centre = caster.position().add(0, 1.0, 0);
		ItemParticleOption paper = new ItemParticleOption(ParticleTypes.ITEM, Items.PAPER);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 18; i++) {
			double a = Math.PI * 2 * i / 18 + pulse * 0.7;
			double d = 1.5 + r.nextDouble() * 2.5;
			Vec3 at = centre.add(Math.cos(a) * d, (r.nextDouble() - 0.4) * 1.6, Math.sin(a) * d);
			Vec3 spin = new Vec3(-Math.sin(a), 0.15, Math.cos(a));
			Vfx.fling(level, paper, at, spin, 0.35);
		}
		for (int i = 0; i < 3; i++) {
			Vec3 normal = ElementFx.tilted(0.5, pulse * 1.3 + i * 2.1);
			ElementFx.slash(level, centre, normal, ElementFx.inPlane(normal, i * 2.1), 0xF0E4C8, 3.0 + i * 0.4, Math.PI * 1.4, 0.06, 10, 14);
		}
		ElementFx.sigil(level, centre.add(0, 1.4, 0), UP, SigilOption.BAND, 0xF0E4C8, 2.6, 18, 0.1);
		Vfx.emit(level, ParticleTypes.ENCHANT, centre, 20, 0.3, 2.0);
		if (pulse == 0) {
			Feels.sound(level, centre, "arcane_page_storm", 0.9F, 1.0F);
		}
	}

	/** A page catching a foe. */
	static void pageCut(ServerLevel level, Entity target) {
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.PAPER), target.getBoundingBox().getCenter(), 4, 0.3, 0.05);
		Sigils.flash(level, target.getBoundingBox().getCenter(), 0xF0E4C8, 0.6F);
	}

	/** Star Wake: a small star falling to {@code at}, a seal where it bursts. */
	static void star(ServerLevel level, Vec3 at, int k) {
		Vec3 sky = at.add(1.2, 9, -0.8);
		ElementFx.ray(level, sky, at, 0xC8D4FF, 0.06, 6);
		ElementFx.ray(level, sky, at, 0xFFFFFF, 0.025, 4);
		ElementFx.starSeal(level, at.add(0, 0.1, 0), UP, 0.9, 16);
		Sigils.flash(level, at, 0xC8D4FF, 1.4F);
		Motes.burst(level, at, 10, 0xDCE4FF, 0.1, 20, 0.12);
		Feels.sound(level, at, "arcane_star_chime", 0.8F, Feels.step(k * 2));
	}

	/** Crown of Thorns: a crown of green thorns turning over the foe's head. */
	static void thorns(ServerLevel level, Entity target, boolean first) {
		MarkHalos.halo(level, target, 0x9CD860, MarkHalos.Style.CROWN);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
			target.position().add(0, target.getBbHeight() + 0.2, 0), 3, 0.25, 0.02);
		if (first) {
			Feels.sound(level, target.position(), "life_thorn_crown", 0.7F, 1.0F);
		}
	}

	/** Mirror Shards: three glints of mirror circling the caster. */
	static void shards(ServerLevel level, Entity caster, int count, int tick) {
		Vec3 centre = caster.position().add(0, 1.1, 0);
		for (int i = 0; i < count; i++) {
			double a = tick * 0.25 + Math.PI * 2 * i / 3;
			Vec3 at = centre.add(Math.cos(a) * 1.5, Math.sin(tick * 0.2 + i) * 0.2, Math.sin(a) * 1.5);
			ElementFx.sigil(level, at, ElementFx.flatDir(a), SigilOption.GLOW, 0xE4F0FF, 0.35, 4, 0);
			Motes.glow(level, at, 0xFFFFFF, 0.06, 6, Vec3.ZERO, 0.0);
		}
	}

	/** A mirror shard striking a foe. */
	static void shardStrike(ServerLevel level, Vec3 from, Vec3 to, int k) {
		ElementFx.ray(level, from, to, 0xE4F0FF, 0.05, 6);
		ElementFx.ray(level, from, to, 0xFFFFFF, 0.02, 4);
		ElementFx.shatterRing(level, to, 0.8);
		Feels.sound(level, to, "frost_mirror_ring", 0.7F, Feels.step(k));
	}

	/** Soul Lanterns: a lantern of soul-light drifting from the fallen to the caster. */
	static void lantern(ServerLevel level, Vec3 from, Vec3 to) {
		Motes.seek(level, from, to, 0x8CE0FF, 0.3, 24, 1.2);
		Vfx.emit(level, ParticleTypes.SOUL_FIRE_FLAME, from, 6, 0.2, 0.02);
		Feels.sound(level, from, "void_soul_wisp", 0.7F, 1.0F);
	}

	/** Aurora: ribbons of colour arching over the caster. */
	static void aurora(ServerLevel level, Entity caster, int band) {
		Vec3 centre = caster.position();
		int[] colors = {0x9CFFD8, 0x7FC8FF, 0xC89CFF};
		double height = 5.5 + band * 0.9;
		double width = 7.0 - band;
		Vec3 last = null;
		for (int i = 0; i <= 8; i++) {
			double t = i / 8.0;
			double angle = Math.PI * t;
			Vec3 at = centre.add(Math.cos(angle) * width - 0.0, height + Math.sin(angle) * 1.2 + Math.sin(t * 9 + band) * 0.3, Math.sin(t * 6 + band) * 1.2 - 1.5 + band);
			if (last != null) {
				ElementFx.ray(level, last, at, colors[band % 3], 0.22, 40);
			}
			last = at;
		}
		Motes.glows(level, centre.add(0, height - 1, 0), 12, 3.0, colors[band % 3], 0.1, 50, new Vec3(0, -0.02, 0), 0.01);
		if (band == 0) {
			Feels.sound(level, centre, "arcane_aurora", 0.9F, 1.0F);
		}
	}

	/** Aurora's protection settling on an ally. */
	static void shimmer(ServerLevel level, Entity ally) {
		ElementFx.ring(level, ally.position().add(0, ally.getBbHeight() * 0.5, 0), UP, 0x9CFFD8, 0.2, ally.getBbWidth() + 0.4, 0.04, 14);
		Motes.glows(level, ally.getBoundingBox().getCenter(), 6, 0.4, 0xC8FFE8, 0.08, 26, new Vec3(0, 0.02, 0), 0.01);
	}

	/** Turning Tide: a ring of seawater bursting out of where it landed. */
	static void tide(ServerLevel level, Vec3 at, double radius) {
		Vec3 ground = groundAt(level, at);
		ElementFx.ring(level, ground.add(0, 0.15, 0), UP, 0x70C8F0, 0.3, radius, 0.12, 10);
		ElementFx.ring(level, ground.add(0, 0.4, 0), UP, 0xC8F0FF, 0.2, radius * 0.85, 0.05, 12);
		Vfx.emit(level, ParticleTypes.SPLASH, ground.add(0, 0.4, 0), 40, radius * 0.4, 0.2);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, ground.add(0, 0.5, 0), 16, radius * 0.4, 0.05);
		Vfx.emit(level, ParticleTypes.FALLING_WATER, ground.add(0, 1.6, 0), 20, radius * 0.4, 0.0);
		Feels.sound(level, ground, "frost_tide_surge", 0.9F, 1.0F);
	}

	/** Standing Stones: a ring of grey stones heaving up out of the ground, holding, sinking back. */
	static void stones(ServerLevel level, Vec3 at, double radius) {
		Vec3 ground = groundAt(level, at);
		BlockState[] stones = {Blocks.STONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState()};
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			rise(level, ground.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), stones[i % stones.length], 0.7F, 2.0F + (i % 2) * 0.4F, 34);
		}
		ElementFx.crack(level, ground, radius + 0.5, 30);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), ground.add(0, 0.3, 0), 20, radius * 0.5, 0.1);
		ScreenFx.shake(level, ground, 0.35F, 10);
		Feels.sound(level, ground, "earth_menhir_rise", 1.0F, 1.0F);
	}

	/** Storm Crown: the little cloud over a foe's head. */
	static void cloud(ServerLevel level, Entity target) {
		Vec3 top = target.position().add(0, target.getBbHeight() + 1.2, 0);
		Motes.clouds(level, top, 3, 0.35, 0x8A8E9A, 0.9, 22, new Vec3(0, 0.0, 0), 0.01, 0.6);
	}

	/** Storm Crown: the cloud zapping the foe under it. */
	static void zap(ServerLevel level, Entity target, int k) {
		Vec3 top = target.position().add(0, target.getBbHeight() + 1.1, 0);
		ElementFx.bolt(level, top, target.getBoundingBox().getCenter(), 0.05, 1, 2);
		ElementFx.sparks(level, target.getBoundingBox().getCenter(), 4, 0.1);
		Feels.sound(level, target.position(), "storm_crown_zap", 0.6F, Feels.step(k));
	}

	/** Red Moon: a crimson disc hanging over the caster. */
	static void redMoon(ServerLevel level, Entity caster) {
		Vec3 sky = caster.position().add(0, 4.5, 0);
		ElementFx.sigil(level, sky, new Vec3(0, -1, 0), SigilOption.GLOW, 0xE04050, 2.0, 60, 0);
		ElementFx.sigil(level, sky, new Vec3(0, -1, 0), SigilOption.RING, 0xFF8090, 2.6, 60, 0.02);
		Light.orb(level, sky, 0xE04050, 0.9, 60);
		Feels.sound(level, caster.position(), "blood_moon_toll", 0.8F, 1.0F);
	}

	/** A drop of red light running from a struck foe back to the caster. */
	static void drop(ServerLevel level, Vec3 from, Vec3 to) {
		Motes.seek(level, from, to, 0xFF5060, 0.16, 12, 0.3);
		ElementFx.drip(level, from, 0.2, 2);
	}

	/** Sun Seal: a seal of the sun burning on the ground. */
	static void sunSeal(ServerLevel level, Vec3 at, double radius) {
		Vec3 ground = groundAt(level, at);
		ElementFx.flatSigil(level, ground, SigilOption.STAR, 0xFFD860, radius, 64, 0.02);
		ElementFx.flatSigil(level, ground, SigilOption.CIRCLE, 0xFFB040, radius * 1.15, 64, -0.015);
		for (int i = 0; i < 8; i++) {
			Vec3 dir = ElementFx.flatDir(Math.PI * 2 * i / 8);
			ElementFx.ray(level, ground.add(0, 0.12, 0), ground.add(dir.scale(radius * 1.35)).add(0, 0.12, 0), 0xFFE890, 0.04, 30);
		}
		Feels.sound(level, ground, "fire_sun_seal", 0.9F, 1.0F);
	}

	/** Sun Seal flaring. */
	static void sunPulse(ServerLevel level, Vec3 at, double radius) {
		Vec3 ground = groundAt(level, at);
		ElementFx.ring(level, ground.add(0, 0.1, 0), UP, 0xFFD860, 0.3, radius, 0.06, 8);
		ElementFx.flames(level, ground, radius * 0.7, 0.8, 6);
	}

	/** Falling Blades: a spectral blade hanging over a foe. */
	static void bladeHangs(ServerLevel level, Vec3 over) {
		ElementFx.slash(level, over, new Vec3(1, 0, 0), new Vec3(0, -1, 0), 0xE8E8F8, 0.8, Math.PI * 0.5, 0.08, 4, 14);
		Sigils.flash(level, over, 0xFFFFFF, 0.6F);
	}

	/** A blade falling on a foe. */
	static void bladeFalls(ServerLevel level, Vec3 from, Entity target, int k) {
		Vec3 to = target.getBoundingBox().getCenter();
		ElementFx.ray(level, from, to, 0xE8E8F8, 0.09, 5);
		ElementFx.cut(level, to, ElementFx.flatDir(k * 1.3), UP, 0.9, 0.08);
		Feels.sound(level, to, "blood_blade_fall", 0.8F, Feels.step(k));
	}

	/** Whirling Eddy: crescents of wind spinning up out of where it landed. */
	static void eddy(ServerLevel level, Vec3 at, double radius, boolean start) {
		Vec3 ground = groundAt(level, at);
		ElementFx.swirl(level, ground, radius * 0.7, 3.0, 4, 0xD8F8E8, 0xFFFFFF);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), ground.add(0, 0.6, 0), 6, radius * 0.4, 0.1);
		if (start) {
			ElementFx.gustRing(level, ground, radius);
			Feels.sound(level, ground, "wind_eddy", 0.9F, 1.0F);
		}
	}

	/** Lantern Flies: a cloud of fireflies spilling out round the caster. */
	static void fireflies(ServerLevel level, Entity caster, int wave) {
		Vec3 centre = caster.position().add(0, 1.0, 0);
		Motes.glows(level, centre, 24, 3.0 + wave, 0xE8FF90, 0.09, 100, new Vec3(0, 0.005, 0), 0.04);
		Motes.glows(level, centre, 8, 2.0 + wave, 0xFFF4B0, 0.12, 80, new Vec3(0, 0.01, 0), 0.03);
		if (wave == 0) {
			Feels.sound(level, centre, "life_firefly_hush", 0.8F, 1.0F);
		}
	}

	/** Tolling Hour: a clock face opening over a foe's head. */
	static void clockOver(ServerLevel level, Entity target) {
		Vec3 over = target.position().add(0, target.getBbHeight() + 0.9, 0);
		ElementFx.clock(level, over, UP, 0.7, 20, false);
	}

	/** Tolling Hour: the toll. */
	static void toll(ServerLevel level, Entity target) {
		Vec3 over = target.position().add(0, target.getBbHeight() + 0.9, 0);
		ElementFx.stoppedClock(level, over, UP, 0.8, 0, 12);
		ElementFx.ring(level, target.getBoundingBox().getCenter(), UP, 0xF0D070, 0.2, 1.8, 0.06, 10);
		ElementFx.goldenTicks(level, target.getBoundingBox().getCenter(), 0.5, 10);
		Feels.sound(level, target.position(), "time_toll", 0.8F, 1.0F);
	}

	/** Rime Steps: a rime flower left on the ground behind the caster. */
	static void rime(ServerLevel level, Vec3 feet, boolean first) {
		ElementFx.flatSigil(level, feet, SigilOption.STAR, 0xC8F0FF, 0.9, 100, 0.01);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, feet.add(0, 0.2, 0), 3, 0.3, 0.01);
		Feels.sound(level, feet, "frost_rime_step", first ? 0.8F : 0.25F, 1.0F);
	}

	/** Crackling Wake: one burst of sparks along the way it flew. */
	static void crackle(ServerLevel level, Vec3 at, int k) {
		ElementFx.sparks(level, at, 8, 0.15);
		ElementFx.ring(level, at, UP, 0xFFE0A0, 0.15, 1.3, 0.04, 7);
		Sigils.flash(level, at, 0xFFF0C0, 0.9F);
		Feels.sound(level, at, "storm_crackle_wake", 0.7F, Feels.step(k));
	}

	/** Sudden Thicket: seeds scattering, then a thicket bursting up round the foes. */
	static void thicket(ServerLevel level, Vec3 at, double radius) {
		Vec3 ground = groundAt(level, at);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 10; i++) {
			Vec3 dir = ElementFx.flatDir(r.nextDouble() * Math.PI * 2).add(0, 0.5, 0);
			Motes.fling(level, ground.add(0, 0.3, 0), dir, 0.25, 0x70C050, 0.08, 20, new Vec3(0, -0.02, 0));
		}
		BlockState[] growth = {Blocks.LARGE_FERN.defaultBlockState(), Blocks.MANGROVE_ROOTS.defaultBlockState(), Blocks.FERN.defaultBlockState(),
			Blocks.AZALEA_LEAVES.defaultBlockState()};
		for (int i = 0; i < 9; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = r.nextDouble() * radius;
			rise(level, ground.add(Math.cos(a) * d, 0, Math.sin(a) * d), growth[i % growth.length], 0.7F, 1.1F + r.nextFloat() * 0.5F, 24);
		}
		ElementFx.leafSpiral(level, ground, radius * 0.6, 1.4, 8);
		Feels.sound(level, ground, "life_thicket_burst", 0.9F, 1.0F);
	}

	/** Great Bell: a bell of light pealing over the caster, rings running out. */
	static void bell(ServerLevel level, Entity caster) {
		Vec3 over = caster.position().add(0, 3.6, 0);
		ElementFx.sigil(level, over, UP, SigilOption.TARGET, 0xE8D8A8, 2.0, 40, 0.06);
		ElementFx.sigil(level, over.add(0, -0.6, 0), UP, SigilOption.RING, 0xFFF4D0, 2.6, 40, -0.05);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(i * 5, () -> ElementFx.ring(level, caster.position().add(0, 1.0, 0), UP, k == 1 ? 0xFFF4D0 : 0xE8D8A8, 0.5, 6.0, 0.07, 12));
		}
		ScreenFx.shake(level, caster.position(), 0.25F, 8);
		Feels.sound(level, caster.position(), "arcane_great_bell", 1.0F, 1.0F);
	}

	// ------------------------------------------------------------------ helpers

	/** The ground under {@code at} (within a few blocks), or {@code at} itself in the air. */
	static Vec3 groundAt(ServerLevel level, Vec3 at) {
		Vec3 floor = ElementFx.floor(level, at.add(0, 0.5, 0), 4.0);
		return floor == null ? at : floor;
	}

	/** A block display of {@code state} that grows up out of the ground at {@code base}, holds, and sinks away again. */
	static void rise(ServerLevel level, Vec3 base, BlockState state, float width, float height, int hold) {
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return;
		}
		float w = width;
		display.snapTo(base.x, base.y, base.z);
		display.setBlockState(state);
		display.setTransformation(box(-w / 2, -0.02F, -w / 2, w, 0.02F, w));
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		Scheduler.later(1, () -> tween(display, box(-w / 2, -0.02F, -w / 2, w, height, w), 4));
		Scheduler.later(5 + hold, () -> tween(display, box(-w / 2, -0.02F, -w / 2, w, 0.02F, w), 8));
		Scheduler.later(14 + hold, display::discard);
	}

	private static Transformation box(float x, float y, float z, float w, float h, float d) {
		return new Transformation(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(w, h, d), new Quaternionf());
	}

	private static void tween(Display.BlockDisplay display, Transformation to, int ticks) {
		if (display.isRemoved()) {
			return;
		}
		display.setTransformationInterpolationDelay(0);
		display.setTransformationInterpolationDuration(ticks);
		display.setTransformation(to);
	}
}
