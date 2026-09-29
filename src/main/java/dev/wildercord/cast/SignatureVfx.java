package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * How the signature fusions look ({@link SignatureFusions}, {@link SignatureWards}). Like every fused effect, each
 * draws on both of its runes' elements' visual languages ({@link ElementFx}), and on the look of the two runes
 * themselves:
 * <ul>
 *   <li><b>Frostwire</b>: frost closing on each target, then a pale blue current jumping from one cold enemy to the
 *       next, ice crystals flashing where it lands.</li>
 *   <li><b>Seethe</b>: a bubble of water rolling with boiling bubbles and steam, bursting into a white cloud with
 *       tongues of flame in it.</li>
 *   <li><b>Bloomstep</b>: a ring of petals and a void seal where you leave, a door of blossom where you arrive.</li>
 *   <li><b>Skyburst</b>: a gust hurling the target up, trailing embers, then a firework of flame raining down.</li>
 *   <li><b>Stitchtime</b>: a golden thread stitched round the ally with a clock face at their feet, each wound
 *       drawing a stitch, and the thread pulling tight in a flash of green and gold as it heals back.</li>
 *   <li><b>Parasite</b>: a green-and-crimson mote burrowing in, a thin blood-light draining back to the caster each
 *       second, and a leap to its next host.</li>
 *   <li><b>Razorgale</b>: crescents of wind and crimson cuts whirling round the ground, twice.</li>
 *   <li><b>Doomclock</b>: a clock over the target, its gold hand winding, ticking faster and brighter the tighter it's
 *       wound, and a burst of fire and gold at zero.</li>
 *   <li><b>Thunderstep</b>: a dark flicker where you vanish and a lightning strike where you come down.</li>
 *   <li><b>Halo</b>: a ring of pink-gold light over the ally, and a beam of holy light down onto what it smites.</li>
 *   <li><b>Thunderquake</b>: three rings of cracking ground and forks of lightning running out along it.</li>
 *   <li><b>Cometfall</b>: a star seal marking where it will land, a comet trailing fire down onto it, a crater of
 *       light and its shards arcing out.</li>
 *   <li><b>Riposte</b>: a turning clock of arcane light round the ally; a sidestep leaving an afterimage, and a
 *       slash of light into the striker.</li>
 *   <li><b>Dust Devil</b>: a funnel of sand-coloured wind rings wobbling as it wanders, dust whirling round it.</li>
 *   <li><b>Malison</b>: a black curse seal biting in with pink runes, and its thread running on to its heirs.</li>
 *   <li><b>Avalanche</b>: snow, ice and stone crashing down onto a frost seal, and white billows rolling out.</li>
 * </ul>
 */
final class SignatureVfx {
	private SignatureVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Frostwire's current: storm light gone cold. */
	private static final int ICE_SPARK = 0xB8ECFF;
	/** Seethe's water, and its steam. */
	private static final int WATER = 0x5AA8F0;
	private static final int STEAM = 0xF2F6FA;
	/** Bloomstep's door of blossom. */
	private static final int BLOSSOM = 0xF6B8E0;
	/** Stitchtime's golden thread. */
	private static final int THREAD = 0xFFD978;
	/** Parasite: green poison in blood. */
	private static final int PARASITE = 0x9CD84A;
	/** Doomclock's hand, and the fire in its burst. */
	private static final int DOOM = 0xFFB040;
	/** Halo: arcane's pink warmed with the gold of life. */
	private static final int HALO = 0xFFD2E6;
	/** Cometfall's tail. */
	private static final int COMET = 0xFF9ADF;
	/** Dust Devil's sand. */
	private static final int SAND = 0xE8CFA0;
	/** Malison's curse, and the runes written in it. */
	private static final int CURSE = ElementFx.dark(0x2A0A36);
	private static final int CURSE_RUNE = 0xF0A0E8;

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	private static double width(Entity t) {
		return Math.max(0.5, t.getBbWidth());
	}

	// ------------------------------------------------------------------ Frostwire (Chill and Shock)

	/** The chill: frost closing on the target, a crackle of cold sparks over it. */
	static void frostwireChill(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.ring(level, c, UP, ElementFx.FROST.secondary(), width(t) + 0.9, 0.2, 0.04, 8);
		ElementFx.frostCreep(level, t.position(), 0.6 + width(t) * 0.5, 30);
		ElementFx.shards(level, c, 0.4 + width(t) * 0.4, 4);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 6, 0.3, 0.01);
		Fx.sound(level, c, SoundEvents.POWDER_SNOW_STEP, 0.8F, 1.3F);
	}

	/** The current: an arc of cold lightning from each enemy in the chain to the next, flickering over a few ticks. */
	static void frostwire(ServerLevel level, LivingEntity origin, List<LivingEntity> chain) {
		Vec3 from = centre(origin);
		for (LivingEntity t : chain) {
			Vec3 to = centre(t);
			if (from.distanceToSqr(to) > 0.01) {
				ElementFx.arc(level, from, to, ICE_SPARK, 0.06, 1, false, 6);
				ElementFx.ray(level, from, to, ElementFx.FROST.secondary(), 0.02, 5);
			}
			from = to;
		}
		dev.wildercord.cast.feel.Feels.sound(level, centre(origin), "storm_zap", 0.8F, 0.75F);
		Fx.sound(level, centre(origin), SoundEvents.GLASS_BREAK, 0.4F, 1.8F);
	}

	/** Where the current lands: storm sparks and ice crystals; a starburst of ice on one frozen solid. */
	static void frostwireStrike(ServerLevel level, LivingEntity t, boolean frozen) {
		Vec3 c = centre(t);
		ElementFx.sparks(level, c, 6, 0.25);
		ElementFx.shards(level, c, 0.3 + width(t) * 0.4, frozen ? 7 : 3);
		Sigils.flash(level, c, frozen ? ElementFx.FROST.secondary() : ICE_SPARK, frozen ? 1.4F : 0.9F);
		if (frozen) {
			ElementFx.shatterRing(level, c, width(t) + 0.6);
		}
		dev.wildercord.cast.feel.Feels.sound(level, c, "storm_zap", 0.6F, 0.84F);
	}

	// ------------------------------------------------------------------ Seethe (Bubble and Fire)

	/** The bubble closing on the target: a ring of water and a hiss of steam. */
	static void seetheTrap(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double w = width(t);
		ElementFx.ring(level, c, UP, WATER, w + 1.1, w * 0.6 + 0.4, 0.07, 10);
		ElementFx.ring(level, c, ElementFx.tilted(0.9, 0), WATER, w * 0.6 + 0.4, w * 0.6 + 0.4, 0.04, 40);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, c, 12, w * 0.4, 0.05);
		Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.8F, 1.4F);
		Fx.sound(level, c, SoundEvents.FIRECHARGE_USE, 0.5F, 1.5F);
	}

	/** A moment of it boiling: bubbles rolling up round the target, steam curling off, a flicker of heat under it. */
	static void seetheBoil(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double w = width(t);
		RandomSource r = level.getRandom();
		ElementFx.ring(level, c, ElementFx.tilted(0.5 + r.nextDouble() * 0.6, r.nextDouble() * Math.PI * 2), WATER, w * 0.6 + 0.45, w * 0.6 + 0.45, 0.03, 6);
		Vfx.emit(level, ParticleTypes.BUBBLE, c, 4, w * 0.35, 0.02);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, c.add(0, w * 0.4, 0), 2, w * 0.35, 0.02);
		Motes.clouds(level, c.add(0, w * 0.5, 0), 1, 0.2, STEAM, 0.7, 20, new Vec3(0, 0.05, 0), 0.01, 0.35);
		ElementFx.heatFlare(level, t.position().add(0, 0.1, 0), 0.35);
		if (r.nextInt(3) == 0) {
			Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.5F, 0.8F + r.nextFloat() * 0.6F);
		}
	}

	/** The bubble bursts: a white cloud of steam thrown out, tongues of flame licking through it, a spray of water. */
	static void seetheBurst(ServerLevel level, Vec3 at, double radius) {
		Motes.clouds(level, at, 10, radius * 0.45, STEAM, 1.6, 44, new Vec3(0, 0.03, 0), 0.08, 0.5);
		ElementFx.ring(level, at, UP, STEAM, 0.3, radius * 1.1, 0.08, 10);
		ElementFx.tongues(level, at.add(0, -0.6, 0), radius * 0.4, 1.3, 5, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary(), 3, 10);
		Vfx.radial(level, ParticleTypes.SPLASH, at, 24, 0.35);
		Vfx.radial(level, ParticleTypes.BUBBLE_POP, at, 12, 0.25);
		Sigils.flash(level, at, STEAM, (float) Math.min(3, radius));
		Feels.sound(level, at, "fire_steam", 1.0F, 1.2F);
		Feels.sound(level, at, "fire_blast", 0.5F, 1.6F);
	}

	// ------------------------------------------------------------------ Bloomstep (Grow and Blink)

	/** The step: a void seal and a ring of petals where you leave, a door of blossom opening where you arrive. */
	static void bloomstep(ServerLevel level, Vec3 from, Vec3 to) {
		ElementFx.flatSigil(level, from, SigilOption.CIRCLE, ElementFx.dark(ElementFx.VOID.accent()), 1.2, 24, 0.08);
		ElementFx.implode(level, from.add(0, 1.0, 0), 1.3, 8);
		ElementFx.petals(level, from.add(0, 1.0, 0), 0.6, 12);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, from.add(0, 1, 0), 16, 0.3, 0.05);
		bloomDoor(level, to);
		dev.wildercord.cast.feel.Feels.sound(level, from, "life_step", 1.0F, 1.0F);
	}

	/** Bloomstep on Self: the door of blossom opens where you stand. */
	static void bloomstepHere(ServerLevel level, Vec3 at) {
		bloomDoor(level, at);
		dev.wildercord.cast.feel.Feels.sound(level, at, "life_step", 1.0F, 1.0F);
	}

	private static void bloomDoor(ServerLevel level, Vec3 feet) {
		Vec3 c = feet.add(0, 1.0, 0);
		// An upright ring of petals, turning to face every way as it opens.
		for (int i = 0; i < 3; i++) {
			ElementFx.ring(level, c, ElementFx.flatDir(i * Math.PI / 3), i == 1 ? BLOSSOM : ElementFx.LIFE.primary(), 0.2, 1.3, 0.05, 12);
		}
		ElementFx.flatSigil(level, feet, SigilOption.STAR, ElementFx.LIFE.secondary(), 1.3, 30, 0.05);
		ElementFx.leafSpiral(level, feet, 0.9, 2.2, 8);
		ElementFx.petals(level, c, 0.8, 16);
		Motes.butterfly(level, c.add(0.4, 0.3, 0), BLOSSOM, 0.18, 40, new Vec3(0.02, 0.03, 0));
		Motes.butterfly(level, c.add(-0.4, 0.1, 0.2), ElementFx.LIFE.secondary(), 0.16, 40, new Vec3(-0.02, 0.03, 0.01));
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, c, 10, 0.6, 0.02);
	}

	/** An ally eased where you arrive: a soft green ring at their feet and petals drifting up. */
	static void bloomstepMend(ServerLevel level, LivingEntity ally) {
		ElementFx.groundRing(level, ally.position(), ElementFx.LIFE.primary(), 0.2, width(ally) + 0.7, 0.04, 14);
		Motes.glows(level, centre(ally), 3, width(ally) * 0.5, ElementFx.LIFE.secondary(), 0.1, 30, new Vec3(0, 0.02, 0), 0.01);
	}

	// ------------------------------------------------------------------ Skyburst (Launch and Explode)

	/** The fling: a gust ring at the target's feet and a swirl of wind carrying it up, embers kindling in it. */
	static void skyburstFling(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		ElementFx.gustRing(level, feet, width(t) + 1.2);
		ElementFx.swirl(level, feet, width(t) * 0.6 + 0.3, 2.4, 3, ElementFx.WIND.primary(), ElementFx.FIRE.secondary());
		ElementFx.embers(level, centre(t), 0.4, 6);
		Fx.sound(level, feet, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8F, 1.1F);
	}

	/** On the way up: a trail of embers and a wisp of wind under it. */
	static void skyburstRise(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.embers(level, c.add(0, -0.4, 0), 0.25, 3);
		ElementFx.ring(level, t.position(), UP, ElementFx.WIND.accent(), width(t) * 0.5 + 0.2, width(t) * 0.5 + 0.6, 0.03, 5);
	}

	/**
	 * The blast at the top of the flight: a ball of flame and gold, flame slashes wheeling out of it, and fire raining
	 * down onto the ground under it in a ring.
	 */
	static void skyburst(ServerLevel level, Vec3 at, Vec3 floor, double radius) {
		ElementFx.fireImpact(level, at, Math.min(3, radius));
		ElementFx.flameBurst(level, at, radius * 0.8, 10);
		ElementFx.orb(level, at, ElementFx.FIRE.secondary(), radius * 0.35, 8);
		Sigils.flash(level, at, ElementFx.FIRE.secondary(), (float) Math.min(4, radius * 1.2));
		ElementFx.ring(level, at, UP, ElementFx.FIRE.primary(), 0.3, radius * 1.1, 0.1, 10);
		// The rain of fire: streaks from the burst down to the ground under it, and flames where they land.
		RandomSource r = level.getRandom();
		for (int i = 0; i < 7; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = radius * Math.sqrt(r.nextDouble());
			Vec3 land = floor.add(Math.cos(a) * d, 0.05, Math.sin(a) * d);
			ElementFx.ray(level, at.add(Math.cos(a) * 0.4, 0, Math.sin(a) * 0.4), land, i % 2 == 0 ? ElementFx.FIRE.primary() : ElementFx.FIRE.secondary(), 0.05, 7);
			Vfx.emit(level, ParticleTypes.FLAME, land.add(0, 0.2, 0), 3, 0.15, 0.02);
		}
		ElementFx.groundRing(level, floor.add(0, 0.05, 0), ElementFx.FIRE.primary(), 0.3, radius, 0.06, 12);
		Vfx.emit(level, ParticleTypes.LAVA, at, 6, 0.4, 0.1);
		Motes.smoke(level, at, 4, radius * 0.3);
		ScreenFx.shake(level, at, 0.5F, 14);
		Feels.sound(level, at, "fire_blast", 1.0F, 1.25F);
		Fx.sound(level, at, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.6F, 1.0F);
	}

	// ------------------------------------------------------------------ Stitchtime (Heal and Countdown)

	/** The stitch: a golden thread wound round the ally, a clock face under them and the first green of the heal. */
	static void stitch(ServerLevel level, LivingEntity ally) {
		Vec3 c = centre(ally);
		double w = width(ally);
		for (int i = 0; i < 3; i++) {
			ElementFx.ring(level, c.add(0, (i - 1) * 0.45, 0), ElementFx.tilted(0.25, i * 2.1), THREAD, w * 0.55 + 0.35, w * 0.55 + 0.35, 0.025, 30);
		}
		ElementFx.clock(level, ally.position().add(0, 0.05, 0), UP, w * 0.6 + 0.6, 40, false);
		ElementFx.bloom(level, c, ally.position(), 0.8);
		ElementFx.goldenTicks(level, c, w * 0.5, 6);
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_stitch", 0.9F, 1.0F);
	}

	/** A stitch renewed on an ally already stitched: a flick of the thread and a tick of gold. */
	static void stitchRenew(ServerLevel level, LivingEntity ally) {
		ElementFx.ring(level, centre(ally), ElementFx.tilted(0.25, level.getRandom().nextDouble() * 6), THREAD, width(ally) * 0.55 + 0.35, width(ally) * 0.55 + 0.35, 0.025, 14);
		ElementFx.goldenTicks(level, centre(ally), width(ally) * 0.4, 3);
	}

	/** A wound counted: a short stitch of gold across where it struck. */
	static void stitchCount(ServerLevel level, LivingEntity ally) {
		Vec3 c = centre(ally);
		RandomSource r = level.getRandom();
		Vec3 a = c.add(ElementFx.randomDir(r).scale(width(ally) * 0.45));
		ElementFx.ray(level, a, a.add(ElementFx.randomDir(r).scale(0.35)), THREAD, 0.02, 12);
		ElementFx.goldenTicks(level, a, 0.1, 2);
	}

	/** Time's up: the thread pulls tight in a flash of green and gold, healing back what it counted (a quiet fade if nothing). */
	static void stitchClose(ServerLevel level, LivingEntity ally, double healed) {
		Vec3 c = centre(ally);
		double w = width(ally);
		ElementFx.ring(level, c, UP, THREAD, w + 1.0, 0.15, 0.05, 10);
		if (healed <= 0) {
			ElementFx.goldenTicks(level, c, w * 0.4, 3);
			return;
		}
		ElementFx.lifeImpact(level, c, Math.min(1.6, 0.6 + healed * 0.08));
		ElementFx.ring(level, ally.position().add(0, 0.05, 0), UP, ElementFx.LIFE.primary(), 0.2, w + 1.2, 0.05, 14);
		Motes.glows(level, c, 6, w * 0.5, THREAD, 0.12, 30, new Vec3(0, 0.03, 0), 0.01);
		Vfx.emit(level, ParticleTypes.HEART, c.add(0, 0.6, 0), 2, 0.3, 0.02);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.5F);
		Fx.sound(level, c, SoundEvents.PLAYER_LEVELUP, 0.3F, 1.8F);
	}

	// ------------------------------------------------------------------ Parasite (Venom and Leech)

	/** The parasite burrows in: a green-and-crimson mote spiralling into the target, a pulse of blood round it. */
	static void parasite(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double w = width(t);
		Motes.seek(level, c.add(ElementFx.randomDir(level.getRandom()).scale(w + 0.8)), c, PARASITE, 0.18, 12, 1.5);
		ElementFx.pulse(level, c, UP, w + 0.5);
		Vfx.emit(level, ParticleTypes.ITEM_SLIME, c, 5, w * 0.3, 0.05);
		Fx.sound(level, c, SoundEvents.SILVERFISH_AMBIENT, 0.6F, 1.3F);
		Fx.sound(level, c, SoundEvents.HONEY_BLOCK_SLIDE, 0.6F, 0.7F);
	}

	/** A drain: a heartbeat on the host, and a thin thread of blood-light back to the caster it fed (if any). */
	static void parasiteDrain(ServerLevel level, LivingEntity t, LivingEntity fed) {
		Vec3 c = centre(t);
		ElementFx.pulse(level, c, UP, width(t) + 0.3);
		Vfx.emit(level, ParticleTypes.ITEM_SLIME, c, 2, width(t) * 0.3, 0.03);
		ElementFx.drip(level, c, width(t) * 0.3, 2);
		if (fed != null) {
			Motes.seek(level, c, centre(fed), ElementFx.BLOOD.secondary(), 0.12, 14, 0.6);
		}
	}

	/** The host fell: the parasite leaps on, a streak of green and red to its new host. */
	static void parasiteLeap(ServerLevel level, Vec3 from, LivingEntity to) {
		Vec3 end = centre(to);
		ElementFx.ray(level, from, end, PARASITE, 0.04, 8);
		ElementFx.ray(level, from, end, ElementFx.BLOOD.primary(), 0.015, 8);
		Motes.seek(level, from, end, PARASITE, 0.16, 10, 1.0);
		Fx.sound(level, end, SoundEvents.SLIME_JUMP_SMALL, 0.8F, 1.2F);
	}

	// ------------------------------------------------------------------ Razorgale (Windcut and Bleed)

	/** The gale of blades: crescents of wind and crimson whirling round the ground (the second pass, the other way round). */
	static void razorgale(ServerLevel level, Vec3 centre, double radius, boolean second) {
		Vec3 c = centre.add(0, 1.0, 0);
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < 6; i++) {
			double a = phase + i * Math.PI / 3;
			Vec3 at = c.add(0, (i % 3 - 1) * 0.35, 0);
			Vec3 toward = ElementFx.flatDir(second ? -a : a);
			ElementFx.slash(level, at, ElementFx.tilted(0.15, a), toward, i % 2 == 0 ? ElementFx.WIND.primary() : ElementFx.BLOOD.primary(),
				radius * (0.55 + 0.1 * (i % 3)), 1.8, 0.07, 3, 7);
		}
		ElementFx.gustRing(level, centre, radius * 1.1);
		ElementFx.groundRing(level, centre.add(0, 0.05, 0), ElementFx.BLOOD.accent(), radius * 0.2, radius, 0.04, 10);
		Feels.sound(level, centre, "wind_slash", 0.9F, second ? 1.3F : 1.0F);
	}

	/** A blade finds its mark: a crimson cut across the target (a deeper one as the gale tears the wound). */
	static void razorCut(ServerLevel level, LivingEntity t, Vec3 centre, boolean tear) {
		Vec3 c = centre(t);
		Vec3 across = Effects.horizontal(c.subtract(centre), UP);
		ElementFx.cut(level, c, ElementFx.tilted(0.4, Math.atan2(across.z, across.x)), new Vec3(-across.z, 0.3, across.x), width(t) * 0.5 + 0.4, tear ? 0.1 : 0.06);
		ElementFx.drip(level, c, width(t) * 0.3, tear ? 4 : 2);
	}

	// ------------------------------------------------------------------ Doomclock (Primer and Stasis)

	/** The clock is set: a clock face over the target, its hand at the top, a ring of fire round its rim. */
	static void doomclock(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 over = t.position().add(0, t.getBbHeight() + 0.6, 0);
		ElementFx.clock(level, over, UP, 0.6 + width(t) * 0.3, ticks, false);
		ElementFx.ring(level, over, UP, DOOM, 0.2, 0.8 + width(t) * 0.3, 0.05, 12);
		ElementFx.flatSigil(level, t.position(), SigilOption.TARGET, ElementFx.TIME.primary(), 0.9 + width(t) * 0.5, ticks, 0.0);
		Fx.sound(level, over, SoundEvents.TNT_PRIMED, 0.6F, 1.6F);
		Fx.sound(level, over, WildercordSounds.impact("time"), 0.6F, 0.8F);
	}

	/** A blow winds it tighter: a flare of fire on the clock and a sharp tick, brighter the tighter it's wound. */
	static void doomclockWind(ServerLevel level, LivingEntity t, double wound) {
		Vec3 over = t.position().add(0, t.getBbHeight() + 0.6, 0);
		float tight = (float) Math.min(1.0, wound / SignatureRules.DOOMCLOCK_WOUND);
		ElementFx.ring(level, over, UP, DOOM, 0.9 + tight * 0.6, 0.2, 0.04 + tight * 0.04, 8);
		ElementFx.goldenTicks(level, over, 0.3, 2 + (int) (tight * 4));
		Fx.sound(level, over, SoundEvents.NOTE_BLOCK_HAT.value(), 0.8F, 1.0F + tight);
	}

	/** Every half second: the clock ticks on over its bearer, embers gathering under it as it winds. */
	static void doomclockTick(ServerLevel level, LivingEntity t, int left, double wound) {
		Vec3 over = t.position().add(0, t.getBbHeight() + 0.6, 0);
		ElementFx.stoppedClock(level, over, UP, 0.6 + width(t) * 0.3, left / 60.0 * Math.PI * 2, 10);
		if (wound > 0) {
			ElementFx.embers(level, over.add(0, -0.2, 0), 0.2, 1 + (int) (wound / 4));
		}
		Fx.sound(level, over, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.5F, 1.4F);
	}

	/** Zero: the clock shatters in a ring of gold and a burst of fire, bigger the tighter it was wound. */
	static void doomclockBurst(ServerLevel level, Vec3 at, double radius, double wound) {
		double size = Math.min(3.5, radius * (0.8 + wound / 24));
		ElementFx.timeImpact(level, at, size);
		ElementFx.fireImpact(level, at, size * 0.8);
		ElementFx.ring(level, at, UP, DOOM, 0.3, radius * 1.2, 0.09, 10);
		ElementFx.ring(level, at, UP, ElementFx.TIME.secondary(), 0.2, radius, 0.05, 14);
		Sigils.flash(level, at, DOOM, (float) size);
		Motes.smoke(level, at, 4, radius * 0.3);
		ScreenFx.shake(level, at, (float) Math.min(0.8, 0.3 + wound / 20), 16);
		Fx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.1F);
		Fx.sound(level, at, SoundEvents.BELL_BLOCK, 1.0F, 0.6F);
	}

	// ------------------------------------------------------------------ Thunderstep (Shadowstep and Lightning)

	/** Where the caster vanishes: a flicker of darkness and a crackle of static. */
	static void thunderstepLeave(ServerLevel level, Vec3 from) {
		Vec3 c = from.add(0, 1.0, 0);
		ElementFx.implode(level, c, 1.1, 6);
		ElementFx.sparks(level, c, 6, 0.2);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, c, 10, 0.3, 0.05);
		dev.wildercord.cast.feel.Feels.sound(level, from, "storm_whine", 0.8F, 1.0F);
	}

	/** Where they come down: a column of lightning, forks thrown out over the ground and a dark ring round it. */
	static void thunderstep(ServerLevel level, Vec3 at, double radius, boolean shake) {
		ElementFx.bolt(level, at.add(0, 9, 0), at, 0.12, 3, 3);
		ElementFx.stormImpact(level, at.add(0, 0.2, 0), Math.min(3, radius));
		ElementFx.groundRing(level, at.add(0, 0.05, 0), ElementFx.dark(ElementFx.VOID.accent()), 0.3, radius * 1.2, 0.08, 10);
		for (int i = 0; i < 5; i++) {
			double a = i * Math.PI * 2 / 5 + level.getRandom().nextDouble() * 0.5;
			ElementFx.arc(level, at.add(0, 0.1, 0), at.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius), ElementFx.STORM.primary(), 0.05, 1, true, 5);
		}
		Sigils.flash(level, at.add(0, 1, 0), ElementFx.STORM.secondary(), 2.2F);
		if (shake) {
			ScreenFx.shake(level, at, 0.5F, 16);
		}
		dev.wildercord.cast.feel.Feels.sound(level, at, "storm_crack", 1.2F, 0.9F);
		Scheduler.later(8, () -> dev.wildercord.cast.feel.Feels.sound(level, at, "storm_roll", 0.8F, 1.0F));
	}

	// ------------------------------------------------------------------ Halo (Smite and Regrowth)

	private static Vec3 crown(LivingEntity ally) {
		return ally.position().add(0, ally.getBbHeight() + 0.35, 0);
	}

	/** The halo settles: a ring of pink-gold light over the ally's head, a star seal flashing in it, leaves of light falling. */
	static void haloOpen(ServerLevel level, LivingEntity ally) {
		Vec3 top = crown(ally);
		ElementFx.ring(level, top, UP, HALO, 1.2, 0.45, 0.06, 12);
		ElementFx.starSeal(level, top, UP, 0.5, 16);
		ElementFx.petals(level, top, 0.4, 6);
		Motes.glows(level, top, 4, 0.3, HALO, 0.1, 30, new Vec3(0, -0.02, 0), 0.01);
		dev.wildercord.cast.feel.Feels.sound(level, top, "arcane_halo", 1.0F, 1.0F);
	}

	/** While it lasts: the halo glowing over the ally. */
	static void haloGlow(ServerLevel level, LivingEntity ally) {
		Vec3 top = crown(ally);
		ElementFx.ring(level, top, UP, HALO, 0.45, 0.45, 0.04, 11);
		ElementFx.ring(level, top, UP, ElementFx.LIFE.secondary(), 0.38, 0.38, 0.015, 11);
	}

	/** A smite: a beam of holy light from the halo down onto the enemy, and a flash of pink and gold on it. */
	static void haloSmite(ServerLevel level, LivingEntity ally, LivingEntity foe) {
		Vec3 top = crown(ally);
		Vec3 c = centre(foe);
		ElementFx.ray(level, top, c, HALO, 0.08, 7);
		ElementFx.ray(level, top, c, 0xFFFFFF, 0.025, 6);
		ElementFx.arcaneImpact(level, c, 0.8 + width(foe) * 0.3);
		ElementFx.ring(level, foe.position().add(0, 0.05, 0), UP, ElementFx.LIFE.secondary(), 0.2, width(foe) + 0.6, 0.04, 10);
		Vfx.emit(level, ParticleTypes.END_ROD, c, 5, 0.25, 0.04);
		dev.wildercord.cast.feel.Feels.sound(level, c, "arcane_halo_zap", 0.9F, 1.0F);
	}

	/** The halo fades: its ring widening and thinning away. */
	static void haloClose(ServerLevel level, LivingEntity ally) {
		ElementFx.ring(level, crown(ally), UP, HALO, 0.45, 1.1, 0.03, 10);
		Fx.sound(level, crown(ally), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.4F);
	}

	// ------------------------------------------------------------------ Thunderquake (Thunderclap and Tremor)

	/** The first boom: the ground cracking at the heart, a flash of lightning out of the crack. */
	static void quakeOpen(ServerLevel level, Vec3 at) {
		ElementFx.crack(level, at, 1.4, 30);
		ElementFx.bolt(level, at.add(0, 3.5, 0), at, 0.08, 2, 2);
		Sigils.flash(level, at.add(0, 0.8, 0), ElementFx.STORM.secondary(), 1.8F);
		dev.wildercord.cast.feel.Feels.sound(level, at, "storm_boom", 0.9F, 1.0F);
	}

	/** A shockwave: a ring of cracking ground out to its reach, dust thrown up and forks of lightning running along it. */
	static void quakeWave(ServerLevel level, Vec3 at, double reach, int wave) {
		Vec3 floor = at.add(0, 0.05, 0);
		ElementFx.groundRing(level, floor, ElementFx.EARTH.primary(), Math.max(0.2, reach - 2.0), reach, 0.12, 10);
		ElementFx.groundRing(level, floor.add(0, 0.02, 0), ElementFx.STORM.primary(), Math.max(0.2, reach - 1.6), reach, 0.04, 8);
		RandomSource r = level.getRandom();
		net.minecraft.world.level.block.state.BlockState ground = ElementFx.groundBlock(level, at);
		for (int i = 0; i < 4 + wave * 2; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			Vec3 p = floor.add(Math.cos(a) * reach, 0.1, Math.sin(a) * reach);
			ElementFx.stoneShards(level, p, ground, 3, 0.25);
			if (i % 2 == 0) {
				ElementFx.arc(level, floor.add(Math.cos(a) * (reach - 1.5), 0.1, Math.sin(a) * (reach - 1.5)), p, ElementFx.STORM.primary(), 0.04, 1, true, 4);
			}
		}
		ScreenFx.shake(level, at, 0.35F + 0.1F * wave, 10 + reach * 2);
		dev.wildercord.cast.feel.Feels.sound(level, at, "earth_quake", 1.0F, new float[] {1.0F, 1.122F, 1.26F}[Math.max(0, Math.min(2, wave - 1))]);
	}

	/** A wave strikes an enemy: stone chips and a spark under its feet. */
	static void quakeStrike(ServerLevel level, LivingEntity t) {
		ElementFx.stoneShards(level, t.position().add(0, 0.2, 0), ElementFx.groundBlock(level, t.position()), 4, 0.2);
		ElementFx.sparks(level, t.position().add(0, 0.3, 0), 3, 0.15);
	}

	// ------------------------------------------------------------------ Cometfall (Starfall and Meteor)

	/** Where it will land: a star seal on the ground and a target ring closing, so everyone sees it coming. */
	static void cometMark(ServerLevel level, Vec3 at, double radius, int ticks) {
		ElementFx.flatSigil(level, at, SigilOption.STAR, ElementFx.ARCANE.primary(), radius, ticks + 6, 0.06);
		ElementFx.flatSigil(level, at.add(0, 0.01, 0), SigilOption.TARGET, ElementFx.FIRE.primary(), radius * 0.6, ticks + 6, -0.04);
		Light.groundRing(level, at, ElementFx.ARCANE.secondary(), radius * 1.3, radius * 0.9, 0.06, ticks);
		dev.wildercord.cast.feel.Feels.sound(level, at, "arcane_comet", 1.2F, 1.0F);
	}

	/** The comet on its way down ({@code fall}: how far, 0 to 1): a ball of pink fire high overhead, its tail streaming back. */
	static void cometFalling(ServerLevel level, Vec3 at, double fall) {
		Vec3 from = at.add(-9, 22, -6);
		Vec3 head = from.add(at.subtract(from).scale(fall));
		Vec3 tail = head.add(from.subtract(at).normalize().scale(3.5));
		Light.orb(level, head, COMET, 0.6, 6);
		Light.ray(level, head, tail, COMET, 0.25, 6);
		Light.ray(level, head, tail, ElementFx.FIRE.secondary(), 0.1, 5);
		Fx.send(level, ParticleTypes.FLAME, head, 6, 0.3, 0.02);
		if (fall > 0.5) {
			Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
		}
	}

	/** It lands: a crater of light, star and flame together, the ground thrown up and a shockwave out. */
	static void cometImpact(ServerLevel level, Vec3 at, double radius) {
		Vec3 c = at.add(0, 0.6, 0);
		ElementFx.arcaneImpact(level, c, Math.min(4, radius));
		ElementFx.fireImpact(level, c, Math.min(3.5, radius * 0.8));
		ElementFx.earthImpact(level, at, Math.min(3, radius * 0.7));
		ElementFx.starSeal(level, at.add(0, 0.05, 0), UP, radius * 0.8, 30);
		ElementFx.ring(level, c, UP, COMET, 0.3, radius * 1.4, 0.12, 12);
		Light.ray(level, at, at.add(0, 10, 0), COMET, 0.4, 12);
		Sigils.flash(level, c, COMET, (float) Math.min(5, radius * 1.2));
		Motes.smoke(level, c, 6, radius * 0.4);
		ScreenFx.shake(level, at, 0.9F, 24);
		Fx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 0.8F);
	}

	/** A shard of it arcs out into an enemy (or the ground): a streak of pink light and a spark of star where it strikes. */
	static void cometShard(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 mid = from.add(to).scale(0.5).add(0, 1.5, 0);
		ElementFx.ray(level, from, mid, COMET, 0.05, 8);
		ElementFx.ray(level, mid, to, COMET, 0.05, 8);
		ElementFx.shimmer(level, to, 0.3, 4);
		Sigils.flash(level, to, ElementFx.ARCANE.secondary(), 0.9F);
		Fx.sound(level, to, SoundEvents.AMETHYST_CLUSTER_HIT, 0.8F, 1.3F);
	}

	// ------------------------------------------------------------------ Dust Devil (Summit Wind and Sandstorm)

	/** It touches down: a burst of sand out over the ground and the first turn of the funnel. */
	static void devilOpen(ServerLevel level, Vec3 at, double reach) {
		ElementFx.gustRing(level, at, reach * 1.4);
		Motes.clouds(level, at.add(0, 0.5, 0), 6, reach * 0.5, SAND, 1.4, 36, new Vec3(0, 0.03, 0), 0.08, 0.45);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), at.add(0, 0.3, 0), 18, 0.25);
		Feels.sound(level, at, "wind_whirl", 0.9F, 0.7F);
	}

	/** A quarter second of the devil: a funnel of wind rings, wider as they climb and wobbling, sand whirling round it. */
	static void devil(ServerLevel level, Vec3 at, double reach, int tick) {
		RandomSource r = level.getRandom();
		double spin = tick * 0.5;
		for (int i = 0; i < 4; i++) {
			double rr = reach * (0.35 + i * 0.22);
			Vec3 normal = ElementFx.tilted(0.1 + 0.04 * i, spin + i * 1.7);
			ElementFx.ring(level, at.add(0, 0.3 + i * 0.8, 0), normal, i % 2 == 0 ? SAND : ElementFx.WIND.primary(), rr, rr, 0.04, 6);
		}
		for (int i = 0; i < 4; i++) {
			double a = spin * 1.4 + i * Math.PI / 2;
			double rr = reach * (0.3 + 0.6 * r.nextDouble());
			Vec3 p = at.add(Math.cos(a) * rr, 0.2 + r.nextDouble() * 2.8, Math.sin(a) * rr);
			Vfx.fling(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), p, new Vec3(-Math.sin(a), 0.2, Math.cos(a)), 0.25);
		}
		if (tick % 10 == 0) {
			Motes.clouds(level, at.add(0, 0.6 + r.nextDouble() * 1.5, 0), 1, reach * 0.4, SAND, 1.2, 24, new Vec3(0, 0.04, 0), 0.03, 0.35);
			Fx.sound(level, at, SoundEvents.BREEZE_IDLE_AIR, 0.7F, 0.7F);
		}
	}

	/** Once a second on each caught enemy: a scour of sand. */
	static void devilScour(ServerLevel level, LivingEntity t) {
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), centre(t), 6, width(t) * 0.4, 0.05);
		Fx.sound(level, centre(t), SoundEvents.SAND_BREAK, 0.6F, 1.2F);
	}

	/** It blows out: a last gust ring and the dust scattering. */
	static void devilEnd(ServerLevel level, Vec3 at, double reach) {
		ElementFx.gustRing(level, at, reach * 1.8);
		ElementFx.swirl(level, at, reach * 0.5, 3.0, 4, SAND, ElementFx.WIND.primary());
		Motes.clouds(level, at.add(0, 1.0, 0), 6, reach * 0.6, SAND, 1.3, 30, new Vec3(0, 0.05, 0), 0.1, 0.35);
		Feels.sound(level, at, "wind_thump", 1.0F, 0.9F);
	}

	/** Someone flung out of it: a streak of wind under them. */
	static void devilFling(ServerLevel level, LivingEntity t) {
		ElementFx.gustRing(level, t.position(), width(t) + 0.8);
		Vfx.emit(level, ParticleTypes.CLOUD, t.position(), 4, 0.3, 0.05);
	}

	// ------------------------------------------------------------------ Malison (Hex and Resonance)

	/** The curse bites in: a black seal under the target with pink runes turning in it, darkness drawn inward. */
	static void malison(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double w = width(t);
		ElementFx.flatSigil(level, t.position(), SigilOption.CIRCLE, CURSE, 0.8 + w * 0.6, 40, 0.05);
		ElementFx.flatSigil(level, t.position().add(0, 0.01, 0), SigilOption.STAR, CURSE_RUNE, 0.6 + w * 0.5, 40, -0.08);
		ElementFx.implode(level, c, w + 1.0, 8);
		ElementFx.ring(level, c, UP, CURSE_RUNE, w + 0.8, 0.2, 0.03, 10);
		Vfx.emit(level, ParticleTypes.WITCH, c, 6, 0.3, 0.02);
		Fx.sound(level, c, SoundEvents.EVOKER_CAST_SPELL, 0.6F, 0.7F);
	}

	/** The curse passes on: a thread of pink darkness from where its bearer fell to each heir, and the seal on each. */
	static void malisonPass(ServerLevel level, Vec3 from, List<LivingEntity> heirs) {
		ElementFx.blackCore(level, from, 0.6, 10);
		for (LivingEntity t : heirs) {
			ElementFx.ray(level, from, centre(t), CURSE, 0.08, 10);
			ElementFx.ray(level, from, centre(t), CURSE_RUNE, 0.02, 9);
			ElementFx.flatSigil(level, t.position(), SigilOption.STAR, CURSE_RUNE, 0.5 + width(t) * 0.5, 30, -0.08);
		}
		Fx.sound(level, from, SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.6F);
	}

	// ------------------------------------------------------------------ Avalanche (Coldsnap and Stalactite)

	/** It crashes down: shards of ice and stone falling onto a frost seal, a hard white ring and billows of snow rolling out. */
	static void avalanche(ServerLevel level, Vec3 at, double radius) {
		RandomSource r = level.getRandom();
		ElementFx.flatSigil(level, at, SigilOption.STAR, ElementFx.FROST.primary(), radius, 30, 0.03);
		for (int i = 0; i < 8; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = radius * Math.sqrt(r.nextDouble());
			Vec3 land = at.add(Math.cos(a) * d, 0.1, Math.sin(a) * d);
			ElementFx.ray(level, land.add(0, 5 + r.nextDouble() * 2, 0), land, i % 3 == 0 ? ElementFx.EARTH.secondary() : ElementFx.FROST.secondary(), 0.07, 5);
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, i % 3 == 0 ? Blocks.DRIPSTONE_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState()),
				land.add(0, 0.2, 0), 4, 0.2, 0.1);
		}
		ElementFx.frostImpact(level, at.add(0, 0.4, 0), Math.min(3, radius));
		ElementFx.shatterRing(level, at.add(0, 0.3, 0), radius);
		Motes.clouds(level, at.add(0, 0.5, 0), 8, radius * 0.5, 0xF4FAFF, 1.5, 40, new Vec3(0, 0.02, 0), 0.1, 0.45);
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, at.add(0, 1, 0), 20, 0.3);
		ScreenFx.shake(level, at, 0.5F, 14);
		Feels.sound(level, at, "frost_whump", 1.2F, 0.8F);
	}

	/** An enemy buried: snow heaped on it, and a crack of ice on a bare head. */
	static void avalancheBury(ServerLevel level, LivingEntity t, boolean bare) {
		Vec3 top = t.position().add(0, t.getBbHeight(), 0);
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.SNOWBALL), top, 6, width(t) * 0.4, 0.08);
		ElementFx.ring(level, t.position().add(0, 0.1, 0), UP, ElementFx.FROST.secondary(), width(t) + 0.5, 0.2, 0.05, 8);
		if (bare) {
			ElementFx.shards(level, top, 0.4, 4);
			Feels.sound(level, top, "frost_break", 0.4F, 1.5F);
		}
	}

	// ------------------------------------------------------------------ Riposte (Reflect and Foresight)

	/** The ward: a clock of arcane light turning round the ally's waist, and the flash of eyes opening. */
	static void riposte(ServerLevel level, LivingEntity ally) {
		Vec3 c = centre(ally);
		double w = width(ally);
		ElementFx.clock(level, c, UP, w * 0.6 + 0.5, 30, false);
		ElementFx.ring(level, c, UP, ElementFx.ARCANE.primary(), w + 1.0, w * 0.6 + 0.5, 0.05, 10);
		ElementFx.orbit(level, c, w * 0.6 + 0.5, 2, 30, ElementFx.ARCANE.secondary(), ElementFx.TIME.secondary());
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.8F);
		Fx.sound(level, c, WildercordSounds.impact("time"), 0.5F, 1.4F);
	}

	/** Once a second while it lasts: a mote of light for each blow it still answers. */
	static void riposteAura(ServerLevel level, LivingEntity ally, int blows) {
		Motes.glows(level, centre(ally), Math.max(1, blows), width(ally) * 0.6, ElementFx.ARCANE.secondary(), 0.08, 22, new Vec3(0, 0.01, 0), 0.01);
	}

	/** A blow sidestepped and answered: an afterimage of light where the ally stood, and a slash of light into the striker. */
	static void riposteDodge(ServerLevel level, LivingEntity ally, LivingEntity striker) {
		Vec3 c = centre(ally);
		Vec3 s = centre(striker);
		ElementFx.ring(level, c, UP, ElementFx.TIME.secondary(), width(ally) * 0.6 + 0.2, width(ally) + 0.8, 0.04, 8);
		Motes.glows(level, c, 5, width(ally) * 0.4, ElementFx.TIME.primary(), 0.12, 16, Vec3.ZERO, 0.02);
		Vec3 toward = s.subtract(c);
		if (toward.lengthSqr() > 0.01) {
			ElementFx.slash(level, s, ElementFx.perp(toward.normalize()), toward.normalize(), ElementFx.ARCANE.primary(), width(striker) * 0.5 + 0.5, 2.2, 0.08, 2, 6);
			ElementFx.ray(level, c, s, ElementFx.ARCANE.secondary(), 0.02, 5);
		}
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8F, 1.4F);
		Fx.sound(level, s, SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 1.2F);
	}
}
