package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.spell.ReactionRules;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * How the element reactions look. Each is a moment of both elements at once, drawn from their
 * languages in {@link ElementFx}: Shatter bursts ice through a flare of fire, Overload blows the
 * flames apart with lightning, Blight turns darkness into rot, Unweave spins every mark off as its
 * own colour, Rupture tears a wound open under the wind, Elapse races a clock's hands round.
 */
final class ReactionVfx {
	private ReactionVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	/** Rot: a sickly green, darker than life's own. */
	static final int ROT = 0x7A9A3A;

	private static double width(LivingEntity target) {
		return Math.max(0.6, target.getBbWidth());
	}

	// ------------------------------------------------------------------ the first five

	/**
	 * Shatter: the ice bursts apart in a storm of shards, white rings snapping out through a flare of
	 * fire, a cracked frost seal on the ground and steam rising.
	 */
	static void shatter(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
		double tilt = level.getRandom().nextDouble() * Math.PI * 2;
		Sigils.flash(level, c, 0xBFEFFF, 2.8F);
		ElementFx.heatFlare(level, c, 1.3);
		ElementFx.shards(level, c, 1.2 + w * 0.5, 12);
		ElementFx.ring(level, c, UP, 0xFFFFFF, 0.3, 2.4 + w, 0.07, 8);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, tilt), ElementFx.FROST.accent(), 0.2, 1.9 + w, 0.05, 9);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, tilt + Math.PI), ElementFx.FIRE.primary(), 0.2, 1.6 + w, 0.05, 10);
		ElementFx.flatSigil(level, target.position(), SigilOption.CRACKED, ElementFx.FROST.primary(), 1.2 + w, 24, 0.0);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), c, 18, 0.35);
		Motes.clouds(level, c, 4, 0.4, Motes.STEAM, 1.3, 40, new Vec3(0, 0.04, 0), 0.05, 0.45);
	}

	/** Wildfire leaps: a streak of flame from the burning target to another, flames catching on it. */
	static void wildfireLeap(ServerLevel level, LivingEntity from, LivingEntity to) {
		Vec3 a = from.getBoundingBox().getCenter();
		Vec3 b = to.getBoundingBox().getCenter();
		ElementFx.ray(level, a, b, ElementFx.FIRE.primary(), 0.09, 8);
		ElementFx.ray(level, a, b, ElementFx.FIRE.secondary(), 0.035, 7);
		ElementFx.flames(level, to.position(), Math.max(0.35, to.getBbWidth() * 0.6), to.getBbHeight(), 3);
		Vfx.stream(level, a, b, Vfx.theme("fire"), 2);
	}

	/** Wildfire: wind and fire together, a whirl of flame slashes spiralling up out of the target over a ring of fire. */
	static void wildfire(ServerLevel level, LivingEntity target) {
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
	static void conduct(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
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
	static void implode(ServerLevel level, Vec3 center, double radius) {
		ElementFx.implode(level, center, radius * 1.6, 7);
		ElementFx.blackCore(level, center, 0.5, 8);
		Vec3 floor = ElementFx.floor(level, center, radius + 1);
		if (floor != null) {
			ElementFx.groundRing(level, floor, ElementFx.dark(ElementFx.VOID.accent()), radius * 1.8, 0.3, 0.14, 10);
			ElementFx.groundRing(level, floor, ElementFx.VOID.primary(), radius * 1.9, 0.4, 0.04, 9);
		}
		Vfx.emit(level, ParticleTypes.PORTAL, center, 20, 0.1, radius * 0.8);
	}

	// ------------------------------------------------------------------ the newer ones

	/**
	 * Overload: the flames on the target blow apart in a white-hot blast shot through with lightning,
	 * a fork leaping to every foe it throws, rings of fire and storm racing out over the ground.
	 */
	static void overload(ServerLevel level, LivingEntity target, List<LivingEntity> struck) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
		double reach = ReactionRules.OVERLOAD_RADIUS;
		double spin = level.getRandom().nextDouble() * Math.PI * 2;
		ScreenFx.shake(level, c, 0.35F, 14);
		Sigils.flash(level, c, 0xFFF4C8, 3.0F);
		Vfx.emit(level, ParticleTypes.EXPLOSION, c, 1, 0.2, 0.0);
		ElementFx.heatFlare(level, c, 1.8);
		ElementFx.flameBurst(level, c, 1.2 + w * 0.4, 6);
		ElementFx.ring(level, c, UP, ElementFx.STORM.secondary(), 0.3, reach + w, 0.08, 8);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, spin), ElementFx.FIRE.primary(), 0.2, reach, 0.06, 10);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, spin + Math.PI), ElementFx.STORM.primary(), 0.2, reach * 0.8, 0.04, 9);
		for (int i = 0; i < 4; i++) {
			ElementFx.bolt(level, c, c.add(ElementFx.randomDir(level.getRandom()).scale(1.1 + w)), 0.04, 1, 2);
		}
		for (LivingEntity other : struck) {
			ElementFx.bolt(level, c, other.getBoundingBox().getCenter(), 0.05, 1, 2);
			ElementFx.flames(level, other.position(), Math.max(0.35, other.getBbWidth() * 0.6), other.getBbHeight(), 2);
		}
		Vec3 floor = ElementFx.floor(level, c, 3);
		if (floor != null) {
			ElementFx.groundRing(level, floor, ElementFx.FIRE.primary(), 0.4, reach * 1.2, 0.12, 12);
			ElementFx.groundRing(level, floor, ElementFx.STORM.primary(), 0.3, reach, 0.05, 9);
		}
		ElementFx.sparks(level, c, 16, 0.5);
		Motes.clouds(level, c, 5, 0.5, Motes.SMOKE, 1.3, 40, new Vec3(0, 0.035, 0), 0.06, 0.45);
		Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.4F);
		Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.3F);
	}

	/**
	 * Fracture: the ice on the target cracks through like stone. A cracked seal of frost stands over
	 * it, chips of ice and of the ground burst out, rings of frost and earth snap round it and the
	 * ground splits under its feet.
	 */
	static void fracture(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
		double a = level.getRandom().nextDouble() * Math.PI * 2;
		Sigils.flash(level, c, ElementFx.EARTH.secondary(), 2.4F);
		ElementFx.sigil(level, c, ElementFx.flatDir(a), SigilOption.CRACKED, ElementFx.FROST.secondary(), 0.9 + w, 20, 0.0);
		ElementFx.shards(level, c, 1.0 + w * 0.5, 10);
		ElementFx.stoneShards(level, c, ElementFx.groundBlock(level, target.position()), 14, 0.3);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.PACKED_ICE), c, 12, 0.3);
		ElementFx.ring(level, c, UP, ElementFx.FROST.accent(), 0.2, 1.8 + w, 0.05, 8);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, a), ElementFx.EARTH.primary(), 0.2, 1.5 + w, 0.08, 9);
		ElementFx.crack(level, target.position(), 1.2 + w, 26);
		Fx.sound(level, c, SoundEvents.GLASS_BREAK, 1.0F, 0.5F);
		Fx.sound(level, c, SoundEvents.DEEPSLATE_BREAK, 1.0F, 0.7F);
	}

	/** A cracked foe struck: chips of ice and stone flake off it (Cracked's extra landing). */
	static void crackedBite(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), c, 3, 0.25, 0.1);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), c, 2, 0.25, 0.1);
	}

	/**
	 * Blight: darkness falls in on the target and bursts back out as rot. A withered seal spreads under
	 * it, spores billow, streams of rot reach every foe it spreads to and motes of green drift back to
	 * the caster.
	 */
	static void blight(ServerLevel level, LivingEntity target, List<LivingEntity> rotting, LivingEntity caster) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 base = target.position();
		double w = width(target);
		ElementFx.implode(level, c, 1.4 + w, 6);
		Sigils.flash(level, c, ROT, 2.4F);
		ElementFx.flatSigil(level, base, SigilOption.CRACKED, ElementFx.dark(ElementFx.LIFE.primary()), 1.4 + w, 30, 0.02);
		ElementFx.leafSpiral(level, base.add(0, 0.1, 0), 0.9, target.getBbHeight() + 0.6, 4);
		ElementFx.groundRing(level, base, ROT, 0.3, ReactionRules.BLIGHT_RADIUS, 0.06, 14);
		ElementFx.groundRing(level, base, ElementFx.dark(ElementFx.VOID.accent()), 0.2, ReactionRules.BLIGHT_RADIUS * 0.8, 0.12, 12);
		Motes.clouds(level, c, 6, 0.6, ROT, 1.2, 50, new Vec3(0, 0.02, 0), 0.05, 0.4);
		Vfx.radial(level, ParticleTypes.WITCH, c, 10, 0.2);
		ElementFx.petals(level, c, 0.5, 6);
		Vec3 home = caster.getBoundingBox().getCenter();
		for (LivingEntity other : rotting) {
			Vec3 o = other.getBoundingBox().getCenter();
			if (other != target) {
				ElementFx.ray(level, c, o, ROT, 0.07, 8);
				ElementFx.ray(level, c, o, ElementFx.dark(ElementFx.VOID.accent()), 0.03, 7);
				Motes.clouds(level, o, 2, 0.3, ROT, 0.9, 30, new Vec3(0, 0.02, 0), 0.04, 0.4);
			}
			// A little life drawn back to the caster from every one it reaches.
			Motes.seek(level, o, home, ElementFx.LIFE.secondary(), 0.25, 18, 0.5);
		}
		Fx.sound(level, c, SoundEvents.SCULK_CATALYST_BLOOM, 1.0F, 0.8F);
		Fx.sound(level, c, SoundEvents.BONE_MEAL_USE, 1.0F, 0.6F);
	}

	/**
	 * Unweave: every mark on the target comes undone at once. A comet of each mark's colour spins off
	 * round a star seal, a ring of light falls in, and a beat later it all snaps out in a flash.
	 */
	static void unweave(ServerLevel level, LivingEntity target, List<Integer> marks) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		ElementFx.starSeal(level, c, UP, 0.7 + w * 0.5, 18);
		for (int i = 0; i < marks.size(); i++) {
			double a = phase + Math.PI * 2 * i / marks.size();
			Vec3 normal = ElementFx.tilted(0.7, a);
			ElementFx.slash(level, c, normal, ElementFx.inPlane(normal, a * 2), marks.get(i), 0.9 + w * 0.5, Math.PI * 1.4, 0.07, 6, 10);
		}
		ElementFx.ring(level, c, UP, ElementFx.ARCANE.primary(), 1.8 + w, 0.15, 0.05, 6);
		ElementFx.shimmer(level, c, 0.5, 8);
		Scheduler.later(5, () -> {
			Sigils.flash(level, c, ElementFx.ARCANE.secondary(), 2.8F);
			ElementFx.ring(level, c, UP, ElementFx.ARCANE.accent(), 0.2, 2.4 + w, 0.06, 8);
			for (int color : marks) {
				Motes.burst(level, c, 3, color, 0.22, 20, 0.16);
			}
		});
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
		Fx.sound(level, c, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.4F);
	}

	/**
	 * Rupture: the wound tears open under the wind. Two crimson cuts cross through a white slash of air,
	 * heartbeat rings pulse out, blood sprays and red motes are drawn back to the caster.
	 */
	static void rupture(ServerLevel level, LivingEntity target, LivingEntity caster) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
		Vec3 home = caster.getBoundingBox().getCenter();
		Vec3 facing = Effects.horizontal(home.subtract(c), caster.getLookAngle().scale(-1));
		double r = 0.7 + w * 0.5;
		Sigils.flash(level, c, ElementFx.BLOOD.secondary(), 2.4F);
		ElementFx.cut(level, c, facing, ElementFx.inPlane(facing, 0.8), r, 0.16);
		ElementFx.cut(level, c, facing, ElementFx.inPlane(facing, 0.8 + Math.PI / 2), r, 0.16);
		ElementFx.slash(level, c, facing, ElementFx.inPlane(facing, 0.8 + Math.PI / 4), ElementFx.WIND.secondary(), r + 0.2, 2.4, 0.05, 1, 6);
		ElementFx.pulse(level, c, UP, 1.6 + w);
		ElementFx.swirl(level, target.position(), 0.8, target.getBbHeight() + 0.4, 3, ElementFx.BLOOD.primary(), ElementFx.WIND.primary());
		ElementFx.drip(level, c, 0.4, 10);
		Vfx.radial(level, new DustParticleOptions(ElementFx.BLOOD.primary(), 1.2F), c, 14, 0.3);
		for (int i = 0; i < 3; i++) {
			Motes.seek(level, c, home, ElementFx.BLOOD.secondary(), 0.22, 16, 0.4);
		}
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
		Fx.sound(level, c, WildercordSounds.impact("blood"), 0.9F, 0.8F);
	}

	/**
	 * Elapse: a clock opens over the target and at its feet, its hands racing round; what it was
	 * suffering flares out all at once (flame, poison, withering), and golden flecks scatter.
	 */
	static void elapse(ServerLevel level, LivingEntity target, boolean burned, boolean poisoned, boolean withered) {
		Vec3 c = target.getBoundingBox().getCenter();
		double w = width(target);
		double a = level.getRandom().nextDouble() * Math.PI * 2;
		Sigils.flash(level, c, ElementFx.TIME.secondary(), 2.6F);
		ElementFx.clock(level, c, ElementFx.flatDir(a), 0.8 + w * 0.5, 5, false);
		ElementFx.clock(level, target.position().add(0, 0.1, 0), UP, 1.2 + w, 8, false);
		ElementFx.ring(level, c, UP, ElementFx.TIME.accent(), 0.2, 2.0 + w, 0.06, 8);
		ElementFx.goldenTicks(level, c, 0.5, 10);
		if (burned) {
			ElementFx.heatFlare(level, c, 1.2);
			ElementFx.flameBurst(level, c, 1.0 + w * 0.5, 4);
		}
		if (poisoned) {
			Motes.burst(level, c, 8, 0x4E9A2A, 0.25, 22, 0.18);
			Motes.clouds(level, c, 3, 0.4, ROT, 0.9, 30, new Vec3(0, 0.02, 0), 0.04, 0.4);
		}
		if (withered) {
			ElementFx.implode(level, c, 1.0 + w, 5);
			Motes.smoke(level, c, 3, 0.4);
		}
		Fx.sound(level, c, SoundEvents.BELL_BLOCK, 1.0F, 1.6F);
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 0.7F, 1.4F);
	}
}
