package dev.wildercord.cast;

import com.mojang.math.Transformation;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * How the fused effects of frost look ({@link FusedFrost}). Like every fused effect, each draws on both of
 * its elements' visual languages ({@link ElementFx}):
 * <ul>
 *   <li><b>Blizzard</b>: a funnel of wind rings wobbling over a fast-turning wind seal, crescents of snow
 *       whipping round it and white billows carried on the whirl.</li>
 *   <li><b>Frostbloom</b>: an ice lotus of frost-and-green petals opening at the feet, and a frost flower
 *       blooming on whatever strikes the ally.</li>
 *   <li><b>Black Ice</b>: violet-black ice closing on the target, spikes of darkness with violet edges
 *       driven through it, hairline violet cracks while it's brittle, and a burst of black glass when it
 *       shatters.</li>
 *   <li><b>Rime Seal</b>: an arcane frost sigil written on the ground layer by layer, comets circling low
 *       over it, rings tightening on whoever stands on it and a pillar of light as it freezes them.</li>
 *   <li><b>Cryostasis</b>: a clear ice cocoon, a clock face turning round its waist and gold ticking up
 *       the ice every half second, cracking open when time runs out.</li>
 *   <li><b>Frostbite</b>: jaws of frost over crimson snapping shut, then a heartbeat going colder each
 *       second with blood-red crystals forcing out, and blood frozen in ice at the end.</li>
 *   <li><b>Absolute Zero</b>: hard white rings falling in, snow stopping dead in the air; frozen solid, a
 *       crown of ice spikes bursting from the ground and a starburst of frozen light.</li>
 *   <li><b>Fossilize</b>: stone climbing the target in rings of grit while a clock at its feet turns
 *       slower and slower; then a pillar of stone, a stopped clock, and the stone cracking apart.</li>
 *   <li><b>Geode</b>: amethyst bursting up round the ally and a cage of crystal light closing on it; its
 *       facets glinting; shards of amethyst flying into whatever strikes it.</li>
 * </ul>
 */
final class FusedFrostVfx {
	private FusedFrostVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Blizzard's white-out. */
	private static final int SNOW = 0xF4FAFF;
	/** Frost on the wind: between wind's mint and frost's blue. */
	private static final int GALE = 0xDDF6FF;
	/** Frostbloom: ice with green in it. */
	private static final int MINT_ICE = 0xB8F5E6;
	/** Black Ice: its violet edge, and the darkness it's made of. */
	private static final int VIOLET_ICE = 0x8A5CE0;
	private static final int BLACK_ICE = ElementFx.dark(0x1A0830);
	/** Rime Seal: frost's blue through arcane's pink. */
	private static final int LAVENDER = 0xC8B4FF;
	/** Cryostasis: gold light through clear ice. */
	private static final int CLOCK_ICE = 0xFFF4D6;
	/** Frostbite: ice with blood in it. */
	private static final int BLOOD_ICE = 0xFFB4C4;
	/** Absolute Zero: the deepest blue. */
	private static final int DEEP = 0x2A5CFF;
	/** Fossilize: grey stone. */
	private static final int STONE = 0xA59C8E;
	/** Geode: amethyst, from its heart to its palest facet. */
	private static final int AMETHYST = 0xA064F0;
	private static final int AMETHYST_LIGHT = 0xD6B4FF;
	private static final int AMETHYST_PALE = 0xF2E6FF;

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	private static double width(Entity t) {
		return Math.max(0.5, t.getBbWidth());
	}

	// ------------------------------------------------------------------ Blizzard (frost and wind)

	/** Blizzard opens: a burst of frozen wind out over the ground, a wind seal turning fast under it and the first gusts of snow. */
	static void blizzardOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		Vec3 c = centre.add(0, 1.0, 0);
		ElementFx.flatSigil(level, centre, SigilOption.RING, ElementFx.WIND.accent(), radius * 1.02, ticks + 8, -0.09);
		ElementFx.flatSigil(level, centre.add(0, 0.01, 0), SigilOption.STAR, ElementFx.FROST.primary(), radius * 0.55, ticks + 8, 0.05);
		ElementFx.gustRing(level, centre, radius * 1.1);
		Light.groundRing(level, centre, ElementFx.FROST.secondary(), 0.3, radius * 1.2, 0.08, 10);
		ElementFx.swirl(level, centre, radius * 0.45, 3.2, 6, ElementFx.FROST.secondary(), ElementFx.WIND.accent());
		Sigils.flash(level, c, ElementFx.FROST.secondary(), (float) Math.min(4, radius));
		Motes.clouds(level, c, 6, radius * 0.4, SNOW, 1.6, 36, new Vec3(0, 0.02, 0), 0.06, 0.45);
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, c, 18, 0.3);
		Fx.sound(level, centre, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.9F, 0.6F);
		Fx.sound(level, centre, SoundEvents.POWDER_SNOW_BREAK, 1.0F, 0.6F);
		Fx.sound(level, centre, WildercordSounds.impact("frost"), 0.7F, 1.0F);
	}

	/**
	 * A quarter second of the storm: a funnel of wind rings at three heights, narrowing as they climb,
	 * each tipped toward a turning point so the stack wobbles like a whirl; crescents of snow whipping
	 * round; snow driven sideways; billows of white carried round.
	 */
	static void blizzard(ServerLevel level, Vec3 centre, double radius, int tick) {
		RandomSource r = level.getRandom();
		double spin = tick * 0.42;
		for (int i = 0; i < 3; i++) {
			double rr = radius * (0.95 - i * 0.2);
			Vec3 normal = ElementFx.tilted(0.12 + 0.05 * i, spin + i * 2.1);
			ElementFx.ring(level, centre.add(0, 0.35 + i * 1.0, 0), normal, i == 1 ? ElementFx.WIND.primary() : GALE, rr, rr, 0.035, 7);
		}
		for (int i = 0; i < 2; i++) {
			double a = spin * 1.3 + Math.PI * i;
			Vec3 at = centre.add(0, 0.6 + r.nextDouble() * 1.8, 0);
			ElementFx.slash(level, at, ElementFx.tilted(0.2, a + Math.PI / 2), ElementFx.flatDir(a), i == 0 ? ElementFx.FROST.secondary() : ElementFx.WIND.accent(),
				radius * (0.5 + 0.35 * r.nextDouble()), 1.9, 0.08, 3, 7);
		}
		for (int i = 0; i < 6; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double rr = radius * (0.3 + 0.7 * r.nextDouble());
			Vec3 p = centre.add(Math.cos(a) * rr, 0.2 + r.nextDouble() * 2.6, Math.sin(a) * rr);
			Vfx.fling(level, ParticleTypes.SNOWFLAKE, p, new Vec3(-Math.sin(a), -0.15, Math.cos(a)), 0.22 + r.nextDouble() * 0.12);
		}
		if (tick % 10 == 0) {
			for (int i = 0; i < 2; i++) {
				double a = spin * 0.8 + Math.PI * i + r.nextDouble() * 0.5;
				double rr = radius * (0.45 + r.nextDouble() * 0.4);
				Vec3 at = centre.add(Math.cos(a) * rr, 0.5 + r.nextDouble() * 1.5, Math.sin(a) * rr);
				Motes.clouds(level, at, 1, 0.2, SNOW, 1.7 + r.nextDouble() * 0.6, 30, new Vec3(-Math.sin(a), 0.1, Math.cos(a)).scale(0.08), 0.01, 0.34);
			}
		}
		if (tick % 20 == 0) {
			ElementFx.groundRing(level, centre, ElementFx.FROST.secondary(), radius * 0.25, radius, 0.05, 12);
			Fx.sound(level, centre, SoundEvents.BREEZE_IDLE_AIR, 0.6F, 0.6F);
			Fx.sound(level, centre, SoundEvents.POWDER_SNOW_STEP, 0.8F, 0.7F);
		}
	}

	/** The storm biting an enemy in it: a ring of frost closing on its feet and snow on it. */
	static void blizzardBite(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.ring(level, t.position().add(0, 0.1, 0), UP, GALE, width(t) + 0.5, 0.2, 0.035, 7);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 4, 0.3, 0.02);
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.SNOWBALL), c, 3, 0.25, 0.05);
	}

	/** The storm blows itself out: a last ring of wind and the snow thrown clear. */
	static void blizzardEnd(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.gustRing(level, centre, radius * 1.3);
		ElementFx.swirl(level, centre, radius * 0.3, 2.0, 3, SNOW, ElementFx.WIND.primary());
		Motes.clouds(level, centre.add(0, 0.8, 0), 5, radius * 0.5, SNOW, 1.4, 30, new Vec3(0, 0.03, 0), 0.08, 0.35);
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, centre.add(0, 1.0, 0), 14, 0.35);
		Fx.sound(level, centre, SoundEvents.BREEZE_IDLE_GROUND, 0.7F, 0.8F);
	}

	// ------------------------------------------------------------------ Frostbloom (frost and life)

	/** Frostbloom: an ice lotus opens at the ally's feet, six petals of frost and green curling up and out, on a frost flower seal. */
	static void frostbloom(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < 6; i++) {
			double a = phase + Math.PI * 2 * i / 6;
			Vec3 out = ElementFx.flatDir(a);
			ElementFx.slash(level, feet.add(out.scale(w * 0.3)).add(0, 0.3, 0), new Vec3(-Math.sin(a), 0, Math.cos(a)), out.scale(0.7).add(0, 0.7, 0),
				i % 2 == 0 ? ElementFx.FROST.secondary() : MINT_ICE, w * 0.55 + 0.25, 1.4, 0.12, 3 + i % 2, 18);
		}
		// Inner petals, smaller, turned half a petal round.
		for (int i = 0; i < 3; i++) {
			double a = phase + Math.PI / 6 + Math.PI * 2 * i / 3;
			Vec3 out = ElementFx.flatDir(a);
			ElementFx.slash(level, feet.add(0, 0.25, 0), new Vec3(-Math.sin(a), 0, Math.cos(a)), out.scale(0.4).add(0, 0.9, 0), ElementFx.LIFE.secondary(),
				w * 0.35 + 0.15, 1.2, 0.09, 4, 16);
		}
		ElementFx.flatSigil(level, feet, SigilOption.STAR, MINT_ICE, 0.5 + w * 0.6, 26, 0.04);
		ElementFx.groundRing(level, feet, ElementFx.LIFE.primary(), 0.25, 1.3 + w * 0.3, 0.045, 16);
		ElementFx.groundRing(level, feet, ElementFx.FROST.secondary(), 0.2, 0.9 + w * 0.3, 0.03, 12);
		// Cold motes rising slowly out of the flower.
		Motes.glows(level, feet.add(0, 0.4, 0), 6, w * 0.4, MINT_ICE, 0.1, 40, new Vec3(0, 0.025, 0), 0.01);
		Sigils.flash(level, c, ElementFx.LIFE.secondary(), 1.1F);
		ElementFx.petals(level, c.add(0, 0.4, 0), 0.6, 8);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 8, 0.5, 0.01);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.3F);
		Fx.sound(level, feet, SoundEvents.AZALEA_LEAVES_PLACE, 1.0F, 1.2F);
		Fx.sound(level, feet, WildercordSounds.impact("life"), 0.5F, 1.0F);
	}

	/** Once a second while it lasts: a cold mote or two and a petal drifting round the ally. */
	static void frostbloomAura(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		Motes.glows(level, c, 2, width(t) * 0.6, MINT_ICE, 0.1, 30, new Vec3(0, 0.012, 0), 0.01);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 1, 0.4, 0.0);
		Vfx.emit(level, ParticleTypes.CHERRY_LEAVES, c.add(0, 0.4, 0), 1, 0.4, 0.0);
	}

	/** Something struck the ally: a frost flower blooms on it, a streak of cold light back to it and ice closing round it. */
	static void frostbloomStrike(ServerLevel level, LivingEntity ally, LivingEntity attacker) {
		Vec3 b = centre(attacker);
		Vec3 feet = attacker.position();
		double w = width(attacker);
		ElementFx.ray(level, centre(ally), b, MINT_ICE, 0.05, 6);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, ElementFx.FROST.primary(), 0.5 + w * 0.5, 30, 0.03);
		ElementFx.ring(level, b, UP, ElementFx.FROST.secondary(), w + 0.8, w * 0.5, 0.05, 8);
		ElementFx.shards(level, b, 0.4 + w * 0.4, 4);
		ElementFx.petals(level, b, 0.4, 4);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, b, 6, 0.35, 0.02);
		Fx.sound(level, b, SoundEvents.GLASS_HIT, 0.8F, 1.5F);
		Fx.sound(level, b, SoundEvents.AMETHYST_CLUSTER_HIT, 0.6F, 1.4F);
	}

	// ------------------------------------------------------------------ Black Ice (frost and void)

	/** Black Ice: violet-black ice closes on the target, spikes of darkness driven out through it, a dark frost seal under it. */
	static void blackIce(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		if (encasable(t)) {
			shell(level, t, ticks, Blocks.TINTED_GLASS.defaultBlockState(), Blocks.STAINED_GLASS.purple().defaultBlockState(), true);
		}
		ElementFx.implode(level, c, w + 1.3, 8);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 7; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.35, 0).normalize();
			Vec3 end = c.add(dir.scale(w * 0.5 + 0.5 + 0.5 * r.nextDouble()));
			ElementFx.ray(level, c, end, BLACK_ICE, 0.09, 12);
			ElementFx.ray(level, c, end, VIOLET_ICE, 0.025, 11);
		}
		ElementFx.flatSigil(level, feet, SigilOption.STAR, BLACK_ICE, 0.7 + w * 0.6, ticks + 12, 0.01);
		ElementFx.groundRing(level, feet, ElementFx.VOID.primary(), 0.2, 1.0 + w * 0.6, 0.035, 12);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STAINED_GLASS.black().defaultBlockState()), c, 10, 0.18);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, c, 8, 0.3, 0.02);
		Fx.sound(level, feet, SoundEvents.GLASS_PLACE, 1.0F, 0.5F);
		Fx.sound(level, feet, WildercordSounds.impact("void"), 0.7F, 1.0F);
		Fx.sound(level, feet, WildercordSounds.impact("frost"), 0.5F, 0.5F);
	}

	/** Once a second while it's brittle: hairline cracks of violet light on it and darkness seeping out. */
	static void brittle(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double w = width(t);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 2; i++) {
			Vec3 a = c.add(ElementFx.randomDir(r).scale(w * 0.45));
			Vec3 b = a.add(ElementFx.randomDir(r).scale(0.3));
			ElementFx.ray(level, a, b, VIOLET_ICE, 0.02, 9);
			ElementFx.ray(level, b, b.add(ElementFx.randomDir(r).scale(0.18)), VIOLET_ICE, 0.015, 9);
		}
		Motes.glows(level, c, 2, w * 0.4, 0x6A3AA8, 0.1, 24, new Vec3(0, -0.01, 0), 0.01);
	}

	/** It dies brittle and shatters: a black core, then darkness and black glass bursting out with violet edges. */
	static void blackIceShatter(ServerLevel level, Vec3 at, double radius) {
		RandomSource r = level.getRandom();
		ElementFx.blackCore(level, at, 0.45, 5);
		Sigils.flash(level, at, VIOLET_ICE, 2.2F);
		ElementFx.ring(level, at, UP, BLACK_ICE, 0.3, radius, 0.16, 10);
		ElementFx.ring(level, at, UP, ElementFx.VOID.primary(), 0.2, radius * 1.05, 0.04, 9);
		ElementFx.ring(level, at, ElementFx.tilted(0.9, r.nextDouble() * Math.PI * 2), ElementFx.VOID.secondary(), 0.2, radius * 0.8, 0.03, 9);
		for (int i = 0; i < 10; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.2, 0).normalize();
			Vec3 from = at.add(dir.scale(0.3));
			Vec3 to = at.add(dir.scale(radius * (0.45 + 0.4 * r.nextDouble())));
			ElementFx.ray(level, from, to, BLACK_ICE, 0.08, 9);
			ElementFx.ray(level, from, to, VIOLET_ICE, 0.02, 8);
		}
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STAINED_GLASS.black().defaultBlockState()), at, 20, 0.35);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STAINED_GLASS.purple().defaultBlockState()), at, 10, 0.3);
		ScreenFx.shake(level, at, 0.35F, 12);
		Fx.sound(level, at, SoundEvents.GLASS_BREAK, 1.0F, 0.5F);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 0.6F);
		Fx.sound(level, at, WildercordSounds.impact("void"), 0.8F, 1.0F);
	}

	/** A shard of the shatter driven into an enemy. */
	static void blackIceShard(ServerLevel level, Vec3 from, LivingEntity to) {
		Vec3 b = centre(to);
		ElementFx.ray(level, from, b, BLACK_ICE, 0.07, 8);
		ElementFx.ray(level, from, b, VIOLET_ICE, 0.02, 7);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STAINED_GLASS.black().defaultBlockState()), b, 6, 0.15);
	}

	// ------------------------------------------------------------------ Rime Seal (frost and arcane)

	/**
	 * The seal writes itself on the ground from the outside in: a frost circle, a ring of arcane script
	 * turning against it, a star, a small inner ring; comets circling low over it and glyphs drawn in.
	 */
	static void rimeSealOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		int life = ticks + 10;
		ElementFx.flatSigil(level, centre, SigilOption.CIRCLE, ElementFx.FROST.primary(), radius, life, 0.02);
		Scheduler.later(3, () -> ElementFx.flatSigil(level, centre.add(0, 0.01, 0), SigilOption.RING, ElementFx.ARCANE.accent(), radius * 1.3, life - 3, -0.03));
		Scheduler.later(6, () -> ElementFx.flatSigil(level, centre.add(0, 0.02, 0), SigilOption.STAR, ElementFx.FROST.secondary(), radius * 0.62, life - 6, 0.05));
		Scheduler.later(8, () -> ElementFx.flatSigil(level, centre.add(0, 0.03, 0), SigilOption.RING, ElementFx.ARCANE.primary(), radius * 0.4, life - 8, -0.06));
		ElementFx.groundRing(level, centre, ElementFx.FROST.secondary(), 0.2, radius * 1.3, 0.05, 12);
		ElementFx.orbit(level, centre.add(0, 0.35, 0), radius * 0.9, 3, 14, ElementFx.ARCANE.primary(), ElementFx.FROST.secondary());
		ElementFx.shimmer(level, centre.add(0, 0.3, 0), radius * 0.5, 10);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, centre.add(0, 0.3, 0), 8, radius * 0.4, 0.01);
		Fx.sound(level, centre, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 1.4F);
		Fx.sound(level, centre, SoundEvents.POWDER_SNOW_STEP, 0.9F, 0.8F);
		Fx.sound(level, centre, WildercordSounds.impact("arcane"), 0.6F, 1.0F);
	}

	/** Every half second on the seal: cold motes and snow rising off it; once a second an arcane crescent running round its rim. */
	static void rimeSeal(ServerLevel level, Vec3 centre, double radius, int tick) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < 2; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double rr = Math.sqrt(r.nextDouble()) * radius * 0.9;
			Vec3 p = centre.add(Math.cos(a) * rr, 0.15, Math.sin(a) * rr);
			Motes.glow(level, p, i == 0 ? LAVENDER : ElementFx.FROST.secondary(), 0.1, 26, new Vec3(0, 0.03, 0), 0.01);
			Vfx.emit(level, ParticleTypes.SNOWFLAKE, p, 1, 0.05, 0.01);
		}
		if (tick % 20 == 0) {
			ElementFx.slash(level, centre.add(0, 0.12, 0), UP, ElementFx.flatDir(tick * 0.31), ElementFx.ARCANE.primary(), radius * 1.02, 1.8, 0.05, 8, 12);
			Vfx.emit(level, ParticleTypes.ENCHANT, centre.add(0, 0.6, 0), 6, 0.05, radius * 0.9);
		}
	}

	/** An enemy standing on the seal: a ring of lavender frost tightening on its feet, {@code step} of four. */
	static void rimeCharge(ServerLevel level, LivingEntity t, int step) {
		double w = width(t);
		double from = w + 1.0 - step * 0.25;
		ElementFx.ring(level, t.position().add(0, 0.12, 0), UP, step % 2 == 0 ? LAVENDER : ElementFx.FROST.secondary(), from, Math.max(0.2, from - 0.35), 0.04, 6);
		if (step >= 2) {
			Vfx.emit(level, ParticleTypes.SNOWFLAKE, t.position().add(0, 0.4, 0), 2, w * 0.4, 0.01);
		}
	}

	/** The seal takes it: a lash of light from the seal, a pillar of frost up through it, a diamond of ice and an arcane star round its waist. */
	static void rimeFreeze(ServerLevel level, LivingEntity t, Vec3 centre, int ticks) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		if (encasable(t)) {
			float s = t.getBbWidth() + 0.3F;
			List<Piece> parts = new ArrayList<>();
			parts.add(grow(level, feet, Blocks.ICE.defaultBlockState(), new Vector3f(0, -0.08F, 0), s, t.getBbHeight() + 0.3F, s,
				new Quaternionf().rotateY((float) Math.PI / 4), 3));
			end(parts, ticks, true);
		}
		Light.ray(level, feet, feet.add(0, t.getBbHeight() + 1.2, 0), ElementFx.FROST.secondary(), 0.14, 10);
		ElementFx.ray(level, centre.add(0, 0.15, 0), feet.add(0, 0.15, 0), LAVENDER, 0.04, 8);
		ElementFx.starSeal(level, c, UP, w + 0.7, ticks + 4);
		ElementFx.shatterRing(level, c, w + 1.0);
		ElementFx.shards(level, c, 0.5 + w * 0.4, 5);
		Sigils.flash(level, c, LAVENDER, 1.6F);
		Fx.sound(level, feet, SoundEvents.GLASS_PLACE, 1.0F, 0.8F);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
		Fx.sound(level, feet, WildercordSounds.impact("frost"), 0.6F, 1.0F);
	}

	/** The seal fades: a last arcane ring running out, and its frost thrown up as snow. */
	static void rimeSealClose(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.groundRing(level, centre, ElementFx.ARCANE.primary(), radius, radius * 1.4, 0.04, 10);
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, centre.add(0, 0.3, 0), 12, 0.2);
		ElementFx.shimmer(level, centre.add(0, 0.4, 0), radius * 0.4, 6);
		Fx.sound(level, centre, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.4F);
	}

	// ------------------------------------------------------------------ Cryostasis (frost and time)

	/**
	 * The ally is sealed: a clear ice cocoon grows up round it (two crystals of ice, one turned inside the
	 * other), a clock face turns round its waist for as long as the seal lasts, a dial of gold and a frost
	 * star under it. Returns the cocoon's blocks, for opening it when time runs out.
	 */
	static List<Display.BlockDisplay> cryostasisSeal(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		float w = t.getBbWidth() + 0.45F;
		float h = t.getBbHeight() + 0.3F;
		List<Piece> parts = new ArrayList<>();
		Vector3f base = new Vector3f(0, -0.05F, 0);
		parts.add(grow(level, feet, Blocks.ICE.defaultBlockState(), base, w, h, w, new Quaternionf(), 4));
		parts.add(grow(level, feet, Blocks.ICE.defaultBlockState(), base, w * 0.78F, h + 0.35F, w * 0.78F, new Quaternionf().rotateY((float) Math.PI / 4), 5));
		List<Display.BlockDisplay> shell = new ArrayList<>();
		for (Piece piece : parts) {
			if (piece != null) {
				shell.add(piece.display());
			}
		}
		// Gone a little after the seal would end, whatever happens to it.
		Scheduler.later(ticks + 20, () -> shell.forEach(Entity::discard));
		double r = w * 0.5 + 0.45;
		ElementFx.clock(level, c, UP, r, ticks, false);
		ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, ElementFx.TIME.primary(), r, ticks + 6, 0.02);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.STAR, ElementFx.FROST.primary(), r * 0.7, ticks + 6, -0.03);
		ElementFx.ring(level, c, UP, ElementFx.FROST.secondary(), r + 1.2, r, 0.05, 7);
		Sigils.flash(level, c, CLOCK_ICE, 1.6F);
		ElementFx.goldenTicks(level, c, 0.5, 6);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 6, 0.5, 0.01);
		Fx.sound(level, feet, SoundEvents.GLASS_PLACE, 1.0F, 1.2F);
		Fx.sound(level, feet, SoundEvents.BELL_BLOCK, 0.5F, 1.6F);
		Fx.sound(level, feet, WildercordSounds.impact("time"), 0.7F, 1.0F);
		return shell;
	}

	/** Every half second in the ice: a band of gold climbing it a notch at a time, like the tick of a clock, and a tick. */
	static void cryostasisTick(ServerLevel level, LivingEntity t, int left) {
		Vec3 feet = t.position();
		double h = t.getBbHeight();
		double r = t.getBbWidth() * 0.5 + 0.45;
		double y = 0.2 + (left / 10 % 4) * (h / 3.5);
		ElementFx.ring(level, feet.add(0, h + 0.1 - y, 0), UP, CLOCK_ICE, r + 0.05, r + 0.05, 0.03, 8);
		ElementFx.goldenTicks(level, feet.add(0, h * 0.6, 0), r * 0.7, 2);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, feet.add(0, h * 0.5, 0), 1, r, 0.0);
		Fx.sound(level, feet, SoundEvents.NOTE_BLOCK_HAT.value(), 0.35F, 2.0F);
	}

	/** A blow glances off the ice: a flash where it struck, a ring of gold on the surface and chips of ice. */
	static void cryostasisDeflect(ServerLevel level, LivingEntity t, Vec3 from) {
		Vec3 c = centre(t);
		double r = t.getBbWidth() * 0.5 + 0.45;
		Vec3 dir = from == null ? ElementFx.flatDir(level.getRandom().nextDouble() * Math.PI * 2) : Effects.horizontal(from.subtract(c), new Vec3(1, 0, 0));
		Vec3 at = c.add(dir.scale(r));
		Sigils.flash(level, at, ElementFx.FROST.secondary(), 0.9F);
		ElementFx.ring(level, at, dir, CLOCK_ICE, 0.1, 0.6, 0.03, 6);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), at, 5, 0.12);
		Fx.sound(level, at, SoundEvents.GLASS_HIT, 0.8F, 1.8F);
	}

	/** Time runs out and the cocoon cracks open: the clock stops, the ice bursts into shards and gold. */
	static void cryostasisRelease(ServerLevel level, LivingEntity t, List<Display.BlockDisplay> shell) {
		shell.forEach(Entity::discard);
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double r = t.getBbWidth() * 0.5 + 0.45;
		ElementFx.stoppedClock(level, c, UP, r, level.getRandom().nextDouble() * Math.PI * 2, 12);
		ElementFx.shatterRing(level, c, r + 0.8);
		ElementFx.shards(level, c, r + 0.4, 8);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), c, 16, 0.25);
		ElementFx.goldenTicks(level, c, 0.5, 8);
		Sigils.flash(level, c, CLOCK_ICE, 1.8F);
		Fx.sound(level, feet, SoundEvents.GLASS_BREAK, 0.9F, 1.4F);
		Fx.sound(level, feet, WildercordSounds.impact("time"), 0.6F, 2.0F);
	}

	/** A seal asked for while one still holds: the clock stands still, a dull knock on the ice. */
	static void cryostasisRefused(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.stoppedClock(level, c, UP, t.getBbWidth() * 0.5 + 0.4, 0.8, 10);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 3, 0.3, 0.01);
		Fx.sound(level, c, SoundEvents.GLASS_HIT, 0.6F, 0.6F);
	}

	// ------------------------------------------------------------------ Frostbite (frost and blood)

	/** The bite: jaws of frost over crimson snapping shut on the side facing {@code from}, blood freezing as it falls, a heartbeat gone cold. */
	static void frostbite(ServerLevel level, LivingEntity t, Vec3 from) {
		Vec3 c = centre(t);
		double w = width(t);
		Vec3 toward = Effects.horizontal(c.subtract(from), new Vec3(1, 0, 0));
		Vec3 side = new Vec3(-toward.z, 0, toward.x);
		Vec3 mouth = c.subtract(toward.scale(w * 0.5));
		for (int jaw = 0; jaw < 2; jaw++) {
			Vec3 bulge = new Vec3(0, jaw == 0 ? 1 : -1, 0);
			Vec3 hinge = mouth.subtract(bulge.scale(0.35));
			ElementFx.slash(level, hinge, side, bulge, ElementFx.BLOOD.primary(), 0.55, 2.2, 0.16, 2, 7);
			ElementFx.slash(level, hinge, side, bulge, ElementFx.FROST.secondary(), 0.52, 2.0, 0.06, 2, 6);
		}
		ElementFx.drip(level, c, 0.25, 5);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), c, 6, 0.15);
		ElementFx.ring(level, c, UP, ElementFx.BLOOD.secondary(), 0.15, w + 0.7, 0.05, 7);
		ElementFx.ring(level, c, UP, BLOOD_ICE, 0.05, w + 0.4, 0.035, 11);
		Sigils.flash(level, c, BLOOD_ICE, 1.2F);
		Fx.sound(level, c, WildercordSounds.impact("blood"), 0.7F, 1.0F);
		Fx.sound(level, c, SoundEvents.POWDER_SNOW_BREAK, 0.8F, 1.3F);
	}

	/** A beat of the cold: a heartbeat colder each time ({@code stage} 0 to 2), and crystals of blood-red ice forcing out through the skin. */
	static void frostbiteBeat(ServerLevel level, LivingEntity t, int stage) {
		Vec3 c = centre(t);
		double w = width(t);
		int cold = stage == 0 ? BLOOD_ICE : stage == 1 ? ElementFx.FROST.primary() : ElementFx.FROST.accent();
		ElementFx.ring(level, c, UP, ElementFx.BLOOD.primary(), 0.15, w + 0.5, 0.05, 7);
		ElementFx.ring(level, c, UP, cold, 0.05, w + 0.35, 0.04, 11);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 2 + stage * 2; i++) {
			Vec3 dir = ElementFx.randomDir(r);
			Vec3 at = c.add(dir.scale(w * 0.4));
			ElementFx.ray(level, at, at.add(dir.scale(0.2 + 0.1 * stage + 0.1 * r.nextDouble())), i % 2 == 0 ? BLOOD_ICE : 0xCFE8FF, 0.035, 10);
		}
		ElementFx.drip(level, c, 0.2, 2);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 1 + stage, 0.3, 0.01);
		Fx.sound(level, c, SoundEvents.WARDEN_HEARTBEAT, 0.6F, 1.3F - 0.2F * stage);
	}

	/** The last beat: blood frozen in ice, a shatter ring of frost and crimson, frost creeping out over the ground. */
	static void frostbiteFreeze(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		if (encasable(t)) {
			shell(level, t, ticks, Blocks.ICE.defaultBlockState(), Blocks.STAINED_GLASS.red().defaultBlockState(), true);
		}
		ElementFx.shatterRing(level, c, w + 1.0);
		ElementFx.ring(level, c, ElementFx.tilted(0.7, level.getRandom().nextDouble() * Math.PI * 2), ElementFx.BLOOD.primary(), 0.2, w + 0.8, 0.04, 9);
		ElementFx.frostCreep(level, feet, 0.8 + w * 0.4, ticks + 10);
		Sigils.flash(level, c, ElementFx.FROST.secondary(), 1.4F);
		Fx.sound(level, feet, SoundEvents.GLASS_PLACE, 1.0F, 0.7F);
		Fx.sound(level, feet, SoundEvents.WARDEN_HEARTBEAT, 0.9F, 0.6F);
	}

	// ------------------------------------------------------------------ Absolute Zero (frost and frost)

	/** The cold closes in: rings of hard white light falling in on it at the feet, the heart and the head, and snow stopping dead in the air. */
	static void absoluteZero(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		double h = t.getBbHeight();
		for (int i = 0; i < 3; i++) {
			double y = h * (0.1 + 0.42 * i);
			int color = i == 1 ? ElementFx.FROST.accent() : 0xFFFFFF;
			Scheduler.later(1 + i, () -> ElementFx.ring(level, feet.add(0, y, 0), UP, color, w + 2.0, w * 0.55, 0.05, 8));
		}
		Motes.glows(level, c, 10, w * 0.8 + 0.4, 0xFFFFFF, 0.09, 50, Vec3.ZERO, 0.0);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, ElementFx.FROST.accent(), 0.8 + w * 0.6, 30, 0.0);
		Sigils.flash(level, c, 0xFFFFFF, 1.2F);
		Fx.sound(level, feet, SoundEvents.POWDER_SNOW_BREAK, 0.9F, 0.5F);
		Fx.sound(level, feet, WildercordSounds.impact("frost"), 0.7F, 0.5F);
	}

	/** Frozen solid at absolute zero: the cold closing in, then a crown of ice spikes bursting out of the ground, a starburst of frozen light and a wide frost seal. */
	static void absoluteZeroSolid(ServerLevel level, LivingEntity t, int ticks) {
		absoluteZero(level, t);
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		if (encasable(t)) {
			shell(level, t, ticks, Blocks.ICE.defaultBlockState(), null, true);
		}
		spikes(level, feet, w, ticks);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 10; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.5, 0).normalize();
			ElementFx.ray(level, c, c.add(dir.scale(1.5 + r.nextDouble())), i % 2 == 0 ? 0xFFFFFF : DEEP, 0.05, 14);
		}
		ElementFx.frostCreep(level, feet, 1.6 + w, ticks + 10);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.RING, DEEP, 2.2 + w, ticks + 10, 0.0);
		ElementFx.shatterRing(level, c, w + 1.6);
		Sigils.flash(level, c, 0xFFFFFF, 2.6F);
		ScreenFx.shake(level, feet, 0.3F, 10);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), c, 16, 0.25);
		Fx.sound(level, feet, SoundEvents.GLASS_PLACE, 1.0F, 0.5F);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_BREAK, 0.8F, 0.5F);
	}

	/** Spikes of packed ice bursting out of the ground round {@code feet}, leaning outward, melting away after {@code ticks}. */
	private static void spikes(ServerLevel level, Vec3 feet, double width, int ticks) {
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		float ring = (float) (width * 0.5 + 0.25);
		List<Piece> parts = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			double a = phase + Math.PI * 2 * i / 5 + (r.nextDouble() - 0.5) * 0.4;
			Vector3f base = new Vector3f((float) (Math.cos(a) * ring), -0.15F, (float) (Math.sin(a) * ring));
			Quaternionf lean = new Quaternionf().rotateAxis(-(0.45F + r.nextFloat() * 0.35F), (float) -Math.sin(a), 0, (float) Math.cos(a))
				.rotateY(r.nextFloat() * (float) Math.PI);
			float thick = 0.14F + r.nextFloat() * 0.08F;
			parts.add(grow(level, feet, Blocks.PACKED_ICE.defaultBlockState(), base, thick, 0.9F + r.nextFloat() * 0.7F, thick, lean, 2));
		}
		end(parts, ticks, true);
	}

	// ------------------------------------------------------------------ Fossilize (earth and time)

	/**
	 * A second of turning to stone ({@code stage} 0 to 2): a band of grit and stone tightening round it,
	 * higher each time, stone dust falling off it, and a clock at its feet whose hands turn slower each time.
	 */
	static void fossilizeStage(ServerLevel level, LivingEntity t, int stage, int stepTicks) {
		Vec3 feet = t.position();
		double w = width(t);
		double y = t.getBbHeight() * (0.12 + 0.3 * stage);
		ElementFx.ring(level, feet.add(0, y, 0), UP, ElementFx.EARTH.primary(), w + 0.7, w * 0.55, 0.08, 10);
		ElementFx.ring(level, feet.add(0, y, 0), UP, STONE, w + 0.5, w * 0.55, 0.04, 12);
		BlockState stone = Blocks.TUFF.defaultBlockState();
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, stone), feet.add(0, y, 0), 6 + stage * 3, w * 0.35, 0.0);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, stone), feet.add(0, y, 0), 4 + stage * 2, 0.1);
		ElementFx.clock(level, feet.add(0, 0.1, 0), UP, w * 0.5 + 0.6, (int) Math.max(6, stepTicks * (0.6 + 0.5 * stage)), false);
		ElementFx.goldenTicks(level, feet.add(0, y, 0), w * 0.4, 2 + stage);
		Fx.sound(level, feet, SoundEvents.STONE_HIT, 0.7F, 1.1F - 0.2F * stage);
		if (stage == 0) {
			Fx.sound(level, feet, WildercordSounds.impact("time"), 0.5F, 1.0F);
		}
	}

	/** It's stone: a pillar of tuff where it stood, a clock stopped round its waist, the ground cracked under it. */
	static void fossilizeStone(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		if (encasable(t)) {
			shell(level, t, ticks, Blocks.TUFF.defaultBlockState(), null, false);
		}
		ElementFx.stoppedClock(level, c, UP, w * 0.5 + 0.55, level.getRandom().nextDouble() * Math.PI * 2, ticks + 4);
		ElementFx.crack(level, feet, 0.8 + w * 0.4, ticks + 6);
		Sigils.flash(level, c, ElementFx.EARTH.secondary(), 1.4F);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState()), c, 10, 0.12);
		Fx.sound(level, feet, SoundEvents.DEEPSLATE_PLACE, 1.0F, 0.6F);
		Fx.sound(level, feet, SoundEvents.BELL_BLOCK, 0.4F, 0.5F);
		Fx.sound(level, feet, WildercordSounds.impact("earth"), 0.7F, 1.0F);
	}

	/** The stone cracks apart: chips of tuff flying, the ground cracking, a ring of gold and one of dust, a flash. */
	static void fossilizeCrack(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		ElementFx.crack(level, feet, 1.0 + w * 0.5, 20);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState()), c, 22, 0.3);
		ElementFx.ring(level, c, UP, ElementFx.TIME.primary(), 0.3, w + 1.4, 0.05, 9);
		ElementFx.ring(level, c, ElementFx.tilted(0.8, level.getRandom().nextDouble() * Math.PI * 2), ElementFx.EARTH.primary(), 0.2, w + 1.1, 0.08, 9);
		ElementFx.goldenTicks(level, c, 0.5, 8);
		Sigils.flash(level, c, ElementFx.EARTH.secondary(), 1.8F);
		ScreenFx.shake(level, feet, 0.25F, 10);
		Fx.sound(level, feet, SoundEvents.STONE_BREAK, 1.0F, 0.7F);
		Fx.sound(level, feet, SoundEvents.BASALT_BREAK, 0.8F, 0.8F);
		Fx.sound(level, feet, WildercordSounds.impact("earth"), 0.7F, 1.0F);
	}

	// ------------------------------------------------------------------ Geode (earth and arcane)

	/** Geode: amethyst bursts up out of the ground in a ring round the ally, leaning outward, and a cage of crystal light closes on it. */
	static void geode(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		double w = width(t);
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		float ring = (float) (w * 0.5 + 0.45);
		List<Piece> parts = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			double a = phase + Math.PI * 2 * i / 6;
			Vector3f base = new Vector3f((float) (Math.cos(a) * ring), -0.05F, (float) (Math.sin(a) * ring));
			Quaternionf lean = new Quaternionf().rotateAxis(-0.45F, (float) -Math.sin(a), 0, (float) Math.cos(a));
			float s = 0.5F + 0.15F * (i % 2);
			parts.add(grow(level, feet, Blocks.AMETHYST_CLUSTER.defaultBlockState(), base, s, s, s, lean, 3));
		}
		end(parts, 16, true);
		crystalCage(level, feet, w * 0.5 + 0.3, t.getBbHeight(), 16);
		ElementFx.flatSigil(level, feet, SigilOption.CRACKED, ElementFx.EARTH.secondary(), 0.8 + w * 0.5, 22, 0.0);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.STAR, ElementFx.ARCANE.primary(), 0.5 + w * 0.4, 22, 0.06);
		Sigils.flash(level, c, AMETHYST_LIGHT, 1.4F);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.AMETHYST_SHARD), c, 10, 0.18);
		Fx.sound(level, feet, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0F, 0.9F);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.2F);
		Fx.sound(level, feet, WildercordSounds.impact("earth"), 0.6F, 1.0F);
	}

	/** A cage of crystal light round {@code feet}: a six-sided bipyramid, its edges in amethyst. */
	private static void crystalCage(ServerLevel level, Vec3 feet, double radius, double height, int life) {
		Vec3 top = feet.add(0, height + 0.35, 0);
		Vec3 bottom = feet.add(0, 0.05, 0);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		Vec3[] waist = new Vec3[6];
		for (int i = 0; i < 6; i++) {
			double a = phase + Math.PI * 2 * i / 6;
			waist[i] = feet.add(Math.cos(a) * radius, height * 0.5, Math.sin(a) * radius);
		}
		for (int i = 0; i < 6; i++) {
			int color = i % 2 == 0 ? AMETHYST_LIGHT : AMETHYST;
			ElementFx.ray(level, waist[i], top, color, 0.03, life);
			ElementFx.ray(level, waist[i], bottom, color, 0.03, life);
			ElementFx.ray(level, waist[i], waist[(i + 1) % 6], AMETHYST_PALE, 0.025, life);
		}
	}

	/** Once a second while it lasts: facets of the crystal armour catching the light. */
	static void geodeAura(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double w = width(t);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 2; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			Vec3 at = t.position().add(Math.cos(a) * (w * 0.5 + 0.3), 0.3 + r.nextDouble() * t.getBbHeight() * 0.8, Math.sin(a) * (w * 0.5 + 0.3));
			ElementFx.ray(level, at, at.add(0, 0.35, 0), i == 0 ? AMETHYST_LIGHT : AMETHYST_PALE, 0.03, 8);
		}
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.AMETHYST_SHARD), c, 1, 0.4, 0.0);
	}

	/** Something struck the ally: shards of amethyst fly off its armour into the striker and cut it. */
	static void geodeShards(ServerLevel level, LivingEntity ally, LivingEntity attacker) {
		Vec3 a = centre(ally);
		Vec3 b = centre(attacker);
		RandomSource r = level.getRandom();
		Vec3 toward = b.subtract(a);
		Vec3 dir = toward.lengthSqr() < 1.0E-4 ? UP : toward.normalize();
		Vec3 from = a.add(dir.scale(ally.getBbWidth() * 0.5 + 0.2));
		for (int i = 0; i < 3; i++) {
			ElementFx.ray(level, from.add(ElementFx.randomDir(r).scale(0.15)), b.add(ElementFx.randomDir(r).scale(0.25)), i == 1 ? AMETHYST_PALE : AMETHYST,
				0.035, 6);
		}
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.AMETHYST_SHARD), b, 8, 0.2);
		Vec3 normal = ElementFx.randomDir(r);
		ElementFx.slash(level, b, normal, ElementFx.inPlane(normal, r.nextDouble() * Math.PI * 2), AMETHYST_LIGHT, 0.45, 2.0, 0.08, 1, 6);
		Fx.sound(level, b, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.7F, 1.3F);
		Fx.sound(level, b, SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.0F);
	}

	// ------------------------------------------------------------------ blocks that grow and go (block displays)

	/** One block display of a shape, and what it grows to, so it can melt back down. */
	private record Piece(Display.BlockDisplay display, Vector3f base, float w, float h, float d, Quaternionf turn) {}

	/** Whether a creature is held still enough to close in blocks: a mob frozen or held (never a player or a boss, who keep moving). */
	private static boolean encasable(LivingEntity t) {
		return t instanceof Mob mob && mob.isAlive() && mob.isNoAi() && !Spirits.isBoss(t);
	}

	/** A shell closing round {@code t}: {@code outer} round its body, and a narrower {@code core} (if any) turned inside it, poking out above. */
	private static void shell(ServerLevel level, LivingEntity t, int ticks, BlockState outer, BlockState core, boolean melt) {
		float w = t.getBbWidth() + 0.35F;
		float h = t.getBbHeight() + 0.25F;
		Vector3f base = new Vector3f(0, -0.08F, 0);
		float turn = level.getRandom().nextFloat() * 0.5F;
		List<Piece> parts = new ArrayList<>();
		parts.add(grow(level, t.position(), outer, base, w, h, w, new Quaternionf().rotateY(turn), 3));
		if (core != null) {
			parts.add(grow(level, t.position(), core, base, w * 0.5F, h + 0.45F, w * 0.5F, new Quaternionf().rotateY(turn + (float) Math.PI / 4), 4));
		}
		end(parts, ticks, melt);
	}

	/** A box {@code w} by {@code h} by {@code d}, its bottom face centred on {@code base}, turned by {@code turn} round that point. */
	private static Transformation pillar(Vector3f base, float w, float h, float d, Quaternionf turn) {
		Vector3f corner = turn.transform(new Vector3f(w / 2, 0, d / 2), new Vector3f());
		return new Transformation(new Vector3f(base).sub(corner), new Quaternionf(turn), new Vector3f(w, h, d), new Quaternionf());
	}

	/** A block display at {@code at} that grows from a sliver to its full shape in {@code grow} ticks (null when nothing may be drawn). */
	private static Piece grow(ServerLevel level, Vec3 at, BlockState state, Vector3f base, float w, float h, float d, Quaternionf turn, int grow) {
		if (Fx.muted()) {
			return null;
		}
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(at.x, at.y, at.z);
		display.setBlockState(state);
		display.setTransformation(pillar(base, w * 0.3F, 0.02F, d * 0.3F, turn));
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		Scheduler.later(1, () -> tween(display, pillar(base, w, h, d, turn), grow));
		return new Piece(display, base, w, h, d, turn);
	}

	/** Ends the pieces after {@code ticks}: melting down into the ground, or gone at once (cracked apart). */
	private static void end(List<Piece> parts, int ticks, boolean melt) {
		Scheduler.later(Math.max(1, ticks), () -> {
			for (Piece piece : parts) {
				if (piece == null) {
					continue;
				}
				if (melt) {
					tween(piece.display(), pillar(piece.base(), piece.w() * 0.7F, 0.03F, piece.d() * 0.7F, piece.turn()), 8);
				} else {
					piece.display().discard();
				}
			}
		});
		if (melt) {
			Scheduler.later(Math.max(1, ticks) + 9, () -> parts.forEach(piece -> {
				if (piece != null) {
					piece.display().discard();
				}
			}));
		}
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
