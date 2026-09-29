package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * How the fused effects look, and the Fusion Altar's flourish. A fused effect is two elements at
 * once, so each draws on both elements' visual languages ({@link ElementFx}): Firestorm is fire
 * carried on swirling wind, Steam is frost's shatter ring boiling into white cloud, Plasma is storm's
 * lightning burning fire-hot, and so on.
 */
public final class FusionVfx {
	private FusionVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	// ------------------------------------------------------------------ the fused effects

	/** Firestorm: a whirl of flame tongues round the target, carried up on the wind. */
	static void firestorm(ServerLevel level, Entity t, double radius) {
		Vec3 feet = t.position();
		ElementFx.swirl(level, feet, Math.max(0.6, t.getBbWidth()), t.getBbHeight() + 0.6, 4, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary());
		ElementFx.flames(level, feet, Math.max(0.5, t.getBbWidth() * 0.7), t.getBbHeight(), 10);
		ElementFx.gustRing(level, feet.add(0, 0.1, 0), radius);
		ElementFx.embers(level, centre(t), 0.6, 10);
		Sigils.flash(level, centre(t), ElementFx.FIRE.primary(), 1.4F);
		Fx.sound(level, feet, SoundEvents.FIRECHARGE_USE, 0.8F, 0.8F);
		Fx.sound(level, feet, WildercordSounds.impact("wind"), 0.5F, 1.0F);
	}

	/** The fire leaping from one burning creature to the next: a flame-coloured streak and a lick of fire. */
	static void fireLeap(ServerLevel level, Entity from, Entity to) {
		ElementFx.ray(level, centre(from), centre(to), ElementFx.FIRE.primary(), 0.06, 6);
		ElementFx.flames(level, to.position(), Math.max(0.4, to.getBbWidth() * 0.6), to.getBbHeight() * 0.8, 5);
	}

	/** Steam: a shatter ring of frost that boils into a white cloud. */
	static void steam(ServerLevel level, Entity t) {
		Vec3 c = centre(t);
		ElementFx.shatterRing(level, c, Math.max(0.8, t.getBbWidth() + 0.3));
		ElementFx.heatFlare(level, c, 0.9);
		Motes.clouds(level, c, 6, Math.max(0.35, t.getBbWidth() * 0.5), Motes.STEAM, 1.5, 50, new Vec3(0, 0.04, 0), 0.04, 0.55);
		Motes.clouds(level, c.add(0, 0.5, 0), 3, 0.3, Motes.STEAM, 1.0, 34, new Vec3(0, 0.06, 0), 0.03, 0.4);
		Light.ring(level, c, UP, 0xE6FAFF, 0.1, Math.max(1.0, t.getBbWidth() + 0.8), 0.06, 9);
		Fx.sound(level, c, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.9F);
	}

	/** Magma opening: the ground cracks, glowing orange from below, for as long as the magma lasts ({@code ticks}). */
	static void magmaOpen(ServerLevel level, Vec3 at, double radius, int ticks) {
		ElementFx.crack(level, at.add(0, 0.05, 0), radius, ticks - 4);
		ElementFx.flatSigil(level, at.add(0, 0.07, 0), SigilOption.CRACKED, ElementFx.FIRE.accent(), (float) (radius * 1.1), ticks, 0.01);
		ElementFx.stoneShards(level, at.add(0, 0.3, 0), Blocks.MAGMA_BLOCK.defaultBlockState(), 10, 0.25);
		Fx.sound(level, at, SoundEvents.BASALT_BREAK, 1.0F, 0.6F);
		Fx.sound(level, at, WildercordSounds.impact("earth"), 0.7F, 1.0F);
	}

	/** One second of the magma burning: lava pops and a ring of heat. */
	static void magmaPulse(ServerLevel level, Vec3 at, double radius, boolean last) {
		Vfx.emit(level, ParticleTypes.LAVA, at.add(0, 0.2, 0), 3, radius * 0.5, 0.0);
		Vfx.emit(level, ParticleTypes.FLAME, at.add(0, 0.15, 0), 6, radius * 0.5, 0.01);
		ElementFx.groundRing(level, at.add(0, 0.06, 0), ElementFx.FIRE.primary(), radius * 0.3, radius, 0.05, 12);
		Fx.sound(level, at, SoundEvents.LAVA_POP, 0.8F, 0.9F);
		if (last) {
			Motes.smoke(level, at.add(0, 0.3, 0), 4, radius * 0.4);
			Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.6F, 0.8F);
		}
	}

	/** Tempest: storm's lightning over a burst of wind that throws everything outward. */
	static void tempest(ServerLevel level, Vec3 at) {
		ElementFx.stormImpact(level, at.add(0, 0.8, 0), 1.4);
		ElementFx.gustRing(level, at.add(0, 0.1, 0), 3.5);
		ElementFx.swirl(level, at, 1.2, 2.4, 5, ElementFx.WIND.primary(), ElementFx.STORM.primary());
		ScreenFx.shake(level, at, 0.5F, 16);
		Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9F, 1.2F);
		Fx.sound(level, at, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 0.7F);
	}

	/** Plasma: a white-hot lance of lightning into the target, burning where it lands. */
	static void plasma(ServerLevel level, Vec3 from, Entity t) {
		Vec3 c = centre(t);
		Vec3 start = from.distanceToSqr(c) > 64 ? c.add(c.subtract(from).normalize().scale(-6)) : from;
		ElementFx.bolt(level, start, c, 0.09, 2, 2, ElementFx.FIRE.secondary(), ElementFx.FIRE.primary());
		ElementFx.orb(level, c, 0xFFF0D0, Math.max(0.5, t.getBbWidth() * 0.6), 8);
		ElementFx.heatFlare(level, c, 1.0);
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, c, 12, 0.35);
		Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.5F);
		Fx.sound(level, c, SoundEvents.BLAZE_SHOOT, 0.5F, 1.4F);
	}

	/** One hailstone: ice falling out of a stormy flash and cracking on the target. */
	static void hailstone(ServerLevel level, Entity t, int stone) {
		Vec3 top = t.position().add((stone - 1) * 0.3, t.getBbHeight() + 1.6, 0.2 * (stone % 2));
		Vec3 c = centre(t);
		ElementFx.ray(level, top, c, ElementFx.FROST.secondary(), 0.05, 4);
		ElementFx.shards(level, c.add(0, t.getBbHeight() * 0.3, 0), 0.6, 4);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), c, 6, 0.15);
		if (stone == 0) {
			Sigils.flash(level, top, ElementFx.STORM.secondary(), 0.8F);
		}
		// The first stone opens with a patter of hail; each later one is one hard tick, rising.
		if (stone == 0) {
			Feels.sound(level, c, "frost_hail", 0.9F, 1.0F);
		} else {
			Feels.sound(level, c, "frost_tick", 0.6F, 1.0F + 0.1F * stone);
		}
	}

	/** Glacier: frost creeps out and closes round the target in a shell of ice and stone. */
	static void glacier(ServerLevel level, Entity t, int ticks) {
		// A mob held solid is closed in ice, as Freeze does it.
		if (t instanceof net.minecraft.world.entity.Mob mob && mob.isAlive() && mob.isNoAi()) {
			BlockFx.encase(level, mob, Math.max(10, ticks - 4));
		}
		Vec3 feet = t.position();
		ElementFx.frostCreep(level, feet.add(0, 0.05, 0), Math.max(1.0, t.getBbWidth() + 0.8), ticks + 10);
		ElementFx.crack(level, feet.add(0, 0.05, 0), Math.max(0.8, t.getBbWidth() + 0.4), ticks);
		ElementFx.shards(level, centre(t), Math.max(0.7, t.getBbWidth()), 8);
		Light.ring(level, centre(t), UP, ElementFx.FROST.primary(), Math.max(1.2, t.getBbWidth() + 0.9), 0.2, 0.07, 10);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), centre(t), 14, 0.2);
		Feels.sound(level, feet, "frost_lock", 1.0F, 0.8F);
	}

	/** Lifesteal: blood drawn out of the target along a dark stream into the caster. */
	static void lifesteal(ServerLevel level, Entity t, Entity caster, float taken) {
		Vec3 c = centre(t);
		ElementFx.pulse(level, c, UP, Math.max(0.6, t.getBbWidth()));
		ElementFx.drip(level, c, 0.3, 6);
		Vfx.stream(level, c, centre(caster), Vfx.theme("blood"), 10);
		ElementFx.implode(level, c, Math.max(0.8, t.getBbWidth() + 0.3), 10);
		if (taken > 0) {
			Scheduler.later(8, () -> ElementFx.lifeImpact(level, centre(caster), 0.6));
		}
		Fx.sound(level, c, SoundEvents.GENERIC_DRINK, 0.7F, 0.6F);
	}

	/** Warp: a hole in the world at either end, and a streak of darkness between them. */
	static void warp(ServerLevel level, Vec3 a, Vec3 b) {
		for (Vec3 end : new Vec3[] {a, b}) {
			Vec3 mid = end.add(0, 1.0, 0);
			ElementFx.implode(level, mid, 1.4, 12);
			ElementFx.blackCore(level, mid, 0.5, 10);
			ElementFx.swirl(level, end, 0.7, 2.0, 3, ElementFx.VOID.primary(), ElementFx.WIND.primary());
		}
		ElementFx.ray(level, a.add(0, 1, 0), b.add(0, 1, 0), ElementFx.dark(ElementFx.VOID.accent()), 0.08, 8);
		Fx.sound(level, a, WildercordSounds.BLINK, 0.9F, 0.8F);
		Fx.sound(level, b, WildercordSounds.BLINK, 0.9F, 0.8F);
	}

	private static final int PETAL_POLLEN = 0xFFE39A;

	/** Bloom: a flower seal under the ally and a spiral of leaves and petals. */
	static void bloom(ServerLevel level, Entity t) {
		Vec3 feet = t.position();
		ElementFx.bloom(level, centre(t), feet, 1.2);
		ElementFx.leafSpiral(level, feet, Math.max(0.6, t.getBbWidth()), t.getBbHeight() + 0.4, 10);
		ElementFx.petals(level, centre(t), 0.8, 12);
		ElementFx.groundRing(level, feet, PETAL_POLLEN, 0.4, 4.0, 0.05, 14);
		Motes.glows(level, feet.add(0, 0.6, 0), 6, 0.6, PETAL_POLLEN, 0.12, 26, new Vec3(0.02, 0.03, 0), 0.01);
		dev.wildercord.cast.feel.Feels.sound(level, feet, "life_pollen", 1.0F, 1.0F);
	}

	/** Surge: lightning running up the ally in green and gold. */
	static void surge(ServerLevel level, Entity t) {
		Vec3 feet = t.position();
		Vec3 top = feet.add(0, t.getBbHeight() + 0.3, 0);
		ElementFx.bolt(level, feet, top, 0.05, 1, 2, ElementFx.STORM.primary(), ElementFx.LIFE.primary());
		ElementFx.ring(level, feet.add(0, 0.1, 0), UP, ElementFx.LIFE.primary(), 0.2, 1.2, 0.05, 10);
		ElementFx.ring(level, top, UP, ElementFx.STORM.primary(), 1.0, 0.2, 0.04, 10);
		ElementFx.sparks(level, centre(t), 10, 0.25);
		Fx.sound(level, feet, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.6F);
	}

	/** Nullify: a star seal snuffs out the effects, drawn into a small void. */
	static void nullify(ServerLevel level, Entity t, boolean ally) {
		Vec3 c = centre(t);
		ElementFx.ring(level, c, UP, ElementFx.ARCANE.secondary(), Math.max(1.6, t.getBbWidth() + 1.2), 0.3, 0.06, 12);
		if (ally) {
			ElementFx.shimmer(level, c, 0.6, 10);
			Light.ring(level, c, UP, ElementFx.ARCANE.secondary(), 0.2, Math.max(1.0, t.getBbWidth() + 0.6), 0.05, 10);
		} else {
			ElementFx.implode(level, c, Math.max(1.0, t.getBbWidth() + 0.5), 12);
			Vfx.emit(level, ParticleTypes.WITCH, c, 8, 0.3, 0.02);
		}
		dev.wildercord.cast.feel.Feels.sound(level, c, "arcane_snuff", 0.9F, ally ? 1.2F : 1.0F);
	}

	// ------------------------------------------------------------------ the Fusion Altar

	/**
	 * The altar at work: a magic circle opens over it and turns, light gathers into the middle, and a
	 * pillar of light (in the colour of what was made) goes up as it's done. {@code kind}: 0 upgrade,
	 * 1 combine, 2 Knot, 3 a signature fusion (a combine that also opens a star seal over the pillar).
	 */
	public static void altar(ServerLevel level, Vec3 top, int color, int kind) {
		Vec3 above = top.add(0, 0.3, 0);
		Sigils.circle(level, top.add(0, 0.02, 0), UP, color, WHITE, 1.3F, 50);
		Sigils.layer(level, top.add(0, 0.9, 0), UP, SigilOption.RING, color, 0.9F, 44, 0.15F);
		ElementFx.ring(level, above, UP, color, 2.2, 0.2, 0.08, 16);
		for (int i = 0; i < 3; i++) {
			// Three streams of light from where the three runes lay, drawn into the middle.
			double a = -Math.PI / 2 + i * Math.PI * 2 / 3;
			Vec3 from = above.add(Math.cos(a) * 0.9, 0.1, Math.sin(a) * 0.9);
			Vfx.stream(level, from, above.add(0, 0.2, 0), Vfx.themeOf(color), 6);
		}
		Vfx.emit(level, ParticleTypes.ENCHANT, above.add(0, 0.6, 0), 30, 0.8, 0.6);
		Fx.sound(level, top, WildercordSounds.ALTAR_FUSE, 1.0F, 1.0F);
		Scheduler.later(14, () -> {
			Light.ray(level, top, top.add(0, 6, 0), color, 0.28, 18);
			Light.orb(level, top.add(0, 0.8, 0), color, 0.6, 16);
			Sigils.flash(level, top.add(0, 0.8, 0), color, 2.4F);
			ElementFx.ring(level, top.add(0, 0.1, 0), UP, color, 0.2, 3.2, 0.1, 14);
			Vfx.radial(level, ParticleTypes.END_ROD, top.add(0, 0.8, 0), 18, 0.2);
			Vfx.emit(level, new DustParticleOptions(color, 1.2F), top.add(0, 1.2, 0), 24, 0.6, 0.05);
			if (kind == 2) {
				ElementFx.orbit(level, top.add(0, 0.9, 0), 0.7, 3, 20, color, WHITE);
				Fx.sound(level, top, WildercordSounds.ALTAR_KNOT, 1.0F, 1.0F);
			} else if (kind == 1) {
				Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, top.add(0, 0.8, 0), 20, 0.35);
			} else if (kind == 3) {
				// A signature: a star seal turning over the pillar, and a second ring of white inside the first.
				Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, top.add(0, 0.8, 0), 20, 0.35);
				ElementFx.sigil(level, top.add(0, 1.6, 0), UP, SigilOption.STAR, color, 1.1, 30, 0.12);
				ElementFx.sigil(level, top.add(0, 1.61, 0), UP, SigilOption.RING, WHITE, 1.5, 30, -0.08);
				ElementFx.ring(level, top.add(0, 0.12, 0), UP, WHITE, 0.2, 2.0, 0.06, 12);
				Fx.sound(level, top, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
			}
		});
	}

	/** The altar refusing: a dull flicker of red over it. */
	public static void altarRefuse(ServerLevel level, Vec3 top) {
		Sigils.layer(level, top.add(0, 0.02, 0), UP, SigilOption.CRACKED, 0xE05050, 0.9F, 14, 0.0F);
		Fx.sound(level, top, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
	}
}
