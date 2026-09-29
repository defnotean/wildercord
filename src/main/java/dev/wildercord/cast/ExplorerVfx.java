package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Visuals and sounds for the runes of the world ({@link ExplorerEffects}, {@link ExplorerShapes}).
 * Each is drawn in its element's language ({@link ElementFx}): fire in embers and flame tongues,
 * frost in shards and creeping rime, void as darkness with a violet rim, life in petals and leaves,
 * earth in cracks and thrown stone, arcane in star seals, blood in crimson pulses. The place a rune
 * comes from adds its own touch: sculk souls for the ancient cities, sand for the desert, soul fire
 * for the valley, starlight for the observatory. Everything goes out through {@link Fx} and
 * {@link Sigils}, so nothing opens in front of a player's own eyes.
 */
final class ExplorerVfx {
	private ExplorerVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;
	private static final int SCULK = 0x1FA8A0;
	private static final int SOUL = 0x5AD8E6;
	private static final int SAND = 0xE3C98E;
	private static final int MOON = 0xDCE6FF;
	private static final int PETAL = 0xFFB0D8;
	private static final int WATER = 0x3A8CFF;
	private static final int STAR = 0xFFF2B0;

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	private static void glow(ServerLevel level, int color, Vec3 at, double size) {
		Vfx.emit(level, SigilOption.glow(color, (float) size), at, 1, 0.0, 0.0);
	}

	private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		Fx.sound(level, at, sound, volume, pitch);
	}

	// ------------------------------------------------------------------ modifiers and links

	/** Unstable: a flicker of rift-light, bright when it swings high and guttering when it swings low. */
	static void unstable(ServerLevel level, Vec3 at, double swing) {
		int color = swing >= 1 ? 0xE0B0FF : 0x6A3A90;
		glow(level, color, at, 0.4 + 0.5 * swing);
		ElementFx.ring(level, at, ElementFx.randomDir(level.getRandom()), ElementFx.dark(0x3A1060), 0.2, 0.3 + 0.5 * swing, 0.04, 6);
		sound(level, at, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.35F, (float) (0.6 + 0.5 * swing));
	}

	/** Kindled: a lick of flame on whatever the effect struck. */
	static void kindled(ServerLevel level, LivingEntity t) {
		ElementFx.tongues(level, t.position(), t.getBbWidth() * 0.6, t.getBbHeight() * 0.6, 3, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary(), 5, 8);
	}

	/** A condition link that holds: a small seal at the caster's feet in the link's own colour. */
	static void condition(ServerLevel level, LivingEntity caster, String id) {
		int color = id.equals(Runes.IF_WET.id()) ? WATER : id.equals(Runes.IF_WOUNDED.id()) ? ElementFx.BLOOD.primary() : 0xA064F0;
		ElementFx.flatSigil(level, caster.position(), SigilOption.RING, color, 1.4, 10, 0.2);
		ElementFx.groundRing(level, caster.position(), color, 0.3, 1.6, 0.05, 8);
	}

	// ------------------------------------------------------------------ vanilla structures

	/** Echolocate: rings of sculk-teal sound racing out from the point, along the ground and through the air. */
	static void echolocate(ServerLevel level, Vec3 at, double radius) {
		for (int i = 0; i < 3; i++) {
			int delay = i * 4;
			double size = radius * (1 - i * 0.2);
			Scheduler.later(1 + delay, () -> {
				ElementFx.groundRing(level, CastEngine.ground(level, at.add(0, 0.5, 0)), SCULK, 0.3, size, 0.12, 16);
				ElementFx.ring(level, at, UP, SCULK, 0.3, size * 0.6, 0.06, 14);
			});
		}
		glow(level, SCULK, at, 1.4);
		Vfx.radial(level, ParticleTypes.SCULK_SOUL, at, 6, 0.08);
		sound(level, at, SoundEvents.SCULK_CLICKING, 1.0F, 0.8F);
		sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
	}

	/** The echo back from one creature: a small teal ring round it. */
	static void echo(ServerLevel level, LivingEntity t) {
		if (!t.isAlive()) {
			return;
		}
		ElementFx.ring(level, centre(t), UP, SCULK, 0.2, t.getBbWidth() + 0.6, 0.04, 8);
		Vfx.emit(level, ParticleTypes.SCULK_CHARGE_POP, centre(t), 2, 0.2, 0.01);
	}

	/** Resonant Shriek: a shriek from the caster and a darkness blooming round the target. The echo is fainter. */
	static void shriek(ServerLevel level, LivingEntity caster, LivingEntity t, boolean echo) {
		Vec3 at = centre(t);
		if (!echo) {
			Vec3 from = caster.getEyePosition();
			Vec3 dir = at.subtract(from);
			double length = dir.length();
			for (double d = 1.5; d < length; d += 1.5) {
				ElementFx.ring(level, from.add(dir.normalize().scale(d)), dir, SCULK, 0.2, 0.7, 0.05, 6);
			}
			Vfx.emit(level, new ShriekParticleOption(0), t.position().add(0, 0.2, 0), 1, 0.0, 0.0);
			sound(level, at, SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 1.1F);
		}
		ElementFx.implode(level, at, echo ? 1.0 : 1.6, 10);
		ElementFx.ring(level, at, UP, echo ? SCULK : 0x7FF0E8, 0.3, echo ? 1.4 : 2.2, 0.08, 10);
		Vfx.emit(level, ParticleTypes.SONIC_BOOM, at, 1, 0.0, 0.0);
		Vfx.radial(level, ParticleTypes.SCULK_SOUL, at, echo ? 3 : 6, 0.1);
		sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, echo ? 0.5F : 0.9F, echo ? 1.5F : 1.2F);
	}

	/** Tidecall: water rising in a ring round the point and crashing inward. */
	static void tidecall(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.groundRing(level, centre, WATER, radius * 1.1, 0.2, 0.18, 12);
		ElementFx.groundRing(level, centre, ElementFx.FROST.secondary(), radius * 0.9, 0.3, 0.08, 10);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 16; i++) {
			double a = Math.PI * 2 * i / 16;
			Vec3 edge = centre.add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius);
			Vfx.fling(level, ParticleTypes.SPLASH, edge, centre.subtract(edge).add(0, 1.4, 0).normalize(), 0.5 + r.nextDouble() * 0.3);
			Vfx.emit(level, ParticleTypes.BUBBLE_COLUMN_UP, edge, 2, 0.2, 0.05);
		}
		Scheduler.later(6, () -> {
			Vfx.emit(level, ParticleTypes.SPLASH, centre.add(0, 0.5, 0), 40, radius * 0.4, 0.3);
			ElementFx.shards(level, centre.add(0, 0.6, 0), 1.5, 6);
			Feels.sound(level, centre, "frost_surge", 1.0F, 1.0F);
		});
	}

	/** Infest: chips of stone and silverfish scuttling round the target's feet. */
	static void infest(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 feet = t.position();
		ElementFx.stoneShards(level, feet.add(0, 0.2, 0), Blocks.STONE.defaultBlockState(), first ? 10 : 3, 0.15);
		Vfx.emit(level, ParticleTypes.INFESTED, feet.add(0, 0.4, 0), first ? 8 : 3, 0.3, 0.02);
		if (first) {
			ElementFx.crack(level, feet, 1.1, 12);
			sound(level, feet, SoundEvents.SILVERFISH_AMBIENT, 0.9F, 1.0F);
		} else {
			sound(level, feet, SoundEvents.SILVERFISH_STEP, 0.6F, 1.2F);
		}
	}

	/** Sandstorm opens: a cracked seal of sand and a column of dust. */
	static void sandstormOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		ElementFx.flatSigil(level, centre, SigilOption.CRACKED, SAND, radius * 1.1, ticks + 10, 0.02);
		ElementFx.flatSigil(level, centre, SigilOption.RING, 0xC8A060, radius * 0.62, ticks + 10, -0.03);
		ElementFx.earthImpact(level, centre.add(0, 0.2, 0), radius * 0.4);
		sound(level, centre, SoundEvents.SAND_BREAK, 1.0F, 0.6F);
		sound(level, centre, WildercordSounds.cast("earth"), 0.7F, 0.9F);
	}

	/** Sandstorm: sand whirling round the point, streaks of it flung in arcs. */
	static void sandstorm(ServerLevel level, Vec3 centre, double radius, int tick) {
		BlockParticleOption sand = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
		RandomSource r = level.getRandom();
		double spin = tick * 0.35;
		for (int i = 0; i < 6; i++) {
			double a = spin + Math.PI * 2 * i / 6;
			double rr = radius * (0.4 + r.nextDouble() * 0.6);
			Vec3 at = centre.add(Math.cos(a) * rr, 0.3 + r.nextDouble() * 2.5, Math.sin(a) * rr);
			Vfx.emit(level, sand, at, 2, 0.3, 0.0);
			Vfx.emit(level, ParticleTypes.DUST_PLUME, at, 1, 0.1, 0.02);
		}
		ElementFx.slash(level, centre.add(0, 0.8 + (tick % 3) * 0.6, 0), UP, ElementFx.flatDir(spin), SAND, radius * 0.8, 1.6, 0.08, 6, 8);
		if (tick % 10 == 0) {
			// Billows of sand blown round the storm: each sets off along the whirl and drifts on.
			for (int i = 0; i < 2; i++) {
				double a = spin * 0.7 + Math.PI * i + r.nextDouble() * 0.6;
				double rr = radius * (0.5 + r.nextDouble() * 0.4);
				Vec3 at = centre.add(Math.cos(a) * rr, 0.6 + r.nextDouble() * 1.4, Math.sin(a) * rr);
				Vec3 along = new Vec3(-Math.sin(a), 0.1, Math.cos(a));
				Motes.clouds(level, at, 1, 0.2, SAND, 1.8 + r.nextDouble() * 0.8, 36, along.scale(0.07), 0.01, 0.32);
			}
		}
		if (tick % 20 == 0) {
			sound(level, centre, SoundEvents.SAND_FALL, 0.8F, 0.7F);
		}
	}

	/** Vinelash: a green lash from the caster's hand to the target, thorns bursting where it lands. */
	static void vinelash(ServerLevel level, LivingEntity caster, LivingEntity t) {
		Vec3 from = caster.getEyePosition().subtract(0, 0.4, 0);
		Vec3 to = centre(t);
		ElementFx.ray(level, from, to, 0x3C9A3A, 0.08, 7);
		ElementFx.ray(level, from, to, ElementFx.LIFE.primary(), 0.03, 6);
		Vec3 dir = to.subtract(from).normalize();
		ElementFx.slash(level, to, dir.cross(UP).lengthSqr() < 1e-4 ? UP : dir.cross(UP), dir, ElementFx.LIFE.primary(), 0.9, 2.2, 0.07, 3, 7);
		ElementFx.petals(level, to, 0.4, 6);
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.VINE), to, 6, 0.3, 0.1);
		sound(level, to, SoundEvents.MANGROVE_ROOTS_BREAK, 1.0F, 1.3F);
		sound(level, to, WildercordSounds.impact("life"), 0.6F, 1.1F);
	}

	/** Remedy: a cure's glow, the bad lifted off as grey motes and a flower seal below. */
	static void remedy(ServerLevel level, LivingEntity t) {
		ElementFx.bloom(level, centre(t), t.position(), 0.9);
		Motes.clouds(level, centre(t), 3, 0.3, 0xB8B4C0, 0.8, 26, new Vec3(0, 0.04, 0), 0.02, 0.35);
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, centre(t), 5, 0.4, 0.0);
		sound(level, centre(t), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.35F, 1.6F);
	}

	/** Warcry: a horn's blast, crimson rings pulsing out along the ground. */
	static void warcry(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.pulse(level, centre.add(0, 0.1, 0), UP, radius);
		ElementFx.groundRing(level, CastEngine.ground(level, centre.add(0, 0.5, 0)), ElementFx.BLOOD.secondary(), 0.4, radius, 0.1, 14);
		Fx.sound(level, centre, SoundEvents.RAID_HORN, 0.8F, 1.1F);
		sound(level, centre, WildercordSounds.cast("blood"), 0.7F, 1.0F);
	}

	static void rallied(ServerLevel level, LivingEntity t) {
		ElementFx.ring(level, t.position().add(0, 0.2, 0), UP, ElementFx.BLOOD.primary(), 0.2, 0.9, 0.05, 8);
		Vfx.emit(level, ParticleTypes.ANGRY_VILLAGER, t.position().add(0, t.getBbHeight() + 0.3, 0), 1, 0.1, 0.0);
	}

	/** Fangs: an arcane seal under the target as the fangs rise. */
	static void fangs(ServerLevel level, LivingEntity t) {
		ElementFx.flatSigil(level, t.position(), SigilOption.STAR, ElementFx.ARCANE.primary(), 2.6, 16, 0.15);
		ElementFx.shimmer(level, t.position().add(0, 0.3, 0), 1.0, 6);
		sound(level, t.position(), SoundEvents.EVOKER_PREPARE_ATTACK, 0.8F, 1.3F);
	}

	/** Undertow: water dragging at the target, bubbles pulled downward. */
	static void undertow(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		// A downward corkscrew: bubbles spiralling from over its head into the ground, drawn a couple of steps a tick.
		double h = t.getBbHeight() + 0.5;
		for (int k = 0; k < 10; k++) {
			int step = k;
			Scheduler.later(step / 2, () -> {
				double a = step * 0.95;
				double rr = 0.7 - step * 0.03;
				Vfx.emit(level, ParticleTypes.BUBBLE, feet.add(Math.cos(a) * rr, h - step * h / 10.0, Math.sin(a) * rr), 2, 0.05, 0.0);
			});
		}
		ElementFx.groundRing(level, feet, ElementFx.FROST.accent(), 0.2, 1.2, 0.06, 10);
		Vfx.emit(level, ParticleTypes.CURRENT_DOWN, feet.add(0, 0.8, 0), 6, 0.4, 0.05);
		Feels.sound(level, feet, "frost_drag", 1.0F, 1.0F);
	}

	/** Treasure Sense: a coin-gold glint round the caster. */
	static void treasureSense(ServerLevel level, LivingEntity t) {
		ElementFx.starSeal(level, t.position().add(0, 0.08, 0), UP, 1.6, 16);
		Vfx.emit(level, new DustParticleOptions(0xF5C84A, 1.0F), centre(t), 10, 0.5, 0.0);
		sound(level, centre(t), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
	}

	/** A glint over a container or suspicious block Treasure Sense found. */
	static void treasureGlint(ServerLevel level, BlockPos pos) {
		Vec3 at = Vec3.atCenterOf(pos).add(0, 0.7, 0);
		glow(level, 0xF5C84A, at, 0.5);
		Vfx.emit(level, ParticleTypes.WAX_ON, at, 3, 0.3, 0.02);
	}

	/** Tusk Charge: dust and thrown stone behind the charging caster. */
	static void tuskCharge(ServerLevel level, LivingEntity runner, Vec3 dir, boolean first) {
		Vec3 feet = runner.position();
		ElementFx.stoneShards(level, feet.add(0, 0.2, 0), ElementFx.groundBlock(level, feet), first ? 10 : 4, 0.2);
		ElementFx.slash(level, feet.add(dir.scale(0.8)).add(0, 0.8, 0), dir.cross(UP), dir, ElementFx.EARTH.secondary(), 0.9, 1.8, 0.06, 2, 5);
		if (first) {
			ElementFx.crack(level, feet, 1.2, 10);
			sound(level, feet, SoundEvents.HOGLIN_ANGRY, 1.0F, 1.0F);
			sound(level, feet, WildercordSounds.cast("earth"), 0.7F, 1.1F);
		}
	}

	static void tossed(ServerLevel level, LivingEntity t) {
		ElementFx.earthImpact(level, t.position().add(0, 0.2, 0), 0.8);
		sound(level, t.position(), SoundEvents.HOGLIN_ATTACK, 0.8F, 1.0F);
	}

	/** Blazecall: a fireball streaking down from above onto the target. */
	static void blazeFireball(ServerLevel level, LivingEntity t, int shot) {
		Vec3 at = centre(t);
		double a = shot * 2.1;
		Vec3 from = at.add(Math.cos(a) * 2.5, 6, Math.sin(a) * 2.5);
		ElementFx.ray(level, from, at, ElementFx.FIRE.primary(), 0.14, 5);
		ElementFx.ray(level, from.lerp(at, 0.4), at, ElementFx.FIRE.secondary(), 0.05, 4);
		ElementFx.fireImpact(level, at, 0.7);
		sound(level, at, SoundEvents.BLAZE_SHOOT, 0.8F, 1.0F + shot * 0.1F);
	}

	/** Shulkershell closes: void-violet plates folding in round the target. */
	static void shellClose(ServerLevel level, LivingEntity t) {
		Vec3 at = centre(t);
		for (int i = 0; i < 3; i++) {
			double a = i * Math.PI / 3;
			Vec3 n = new Vec3(Math.cos(a) * 0.8, 0.5, Math.sin(a) * 0.8).normalize();
			ElementFx.ring(level, at, n, ElementFx.VOID.primary(), t.getBbHeight() * 1.2, t.getBbHeight() * 0.7, 0.08, 10);
		}
		ElementFx.flatSigil(level, t.position(), SigilOption.CIRCLE, ElementFx.VOID.secondary(), 2.0, 20, 0.05);
		sound(level, at, SoundEvents.SHULKER_CLOSE, 1.0F, 0.9F);
	}

	static void shellHold(ServerLevel level, LivingEntity t, int tick) {
		Vec3 at = centre(t);
		ElementFx.ring(level, at, ElementFx.tilted(0.6, tick * 0.4), ElementFx.VOID.primary(), t.getBbHeight() * 0.7, t.getBbHeight() * 0.72, 0.05, 11);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, at, 2, 0.4, 0.02);
	}

	/** Shulkershell opens: the plates burst outward and what's close lifts off the ground. */
	static void shellOpen(ServerLevel level, LivingEntity t) {
		Vec3 at = centre(t);
		ElementFx.ring(level, at, UP, ElementFx.VOID.secondary(), 0.4, 3.0, 0.1, 10);
		Vfx.radial(level, ParticleTypes.END_ROD, at, 10, 0.2);
		sound(level, at, SoundEvents.SHULKER_OPEN, 1.0F, 1.0F);
		sound(level, at, SoundEvents.SHULKER_SHOOT, 0.7F, 0.8F);
	}

	/** Portalfall: a portal of darkness opening under the target, and (if it found room) another above. */
	static void portalfall(ServerLevel level, Vec3 from, Vec3 to) {
		ElementFx.implode(level, from.add(0, 0.1, 0), 1.2, 10);
		ElementFx.flatSigil(level, from, SigilOption.CIRCLE, ElementFx.dark(ElementFx.VOID.accent()), 1.8, 12, 0.3);
		Vfx.emit(level, ParticleTypes.PORTAL, from.add(0, 0.5, 0), 20, 0.4, 0.4);
		sound(level, from, SoundEvents.PORTAL_TRIGGER, 0.4F, 1.8F);
		if (to != null) {
			ElementFx.ring(level, to.add(0, 1.2, 0), UP, ElementFx.VOID.primary(), 1.4, 0.4, 0.1, 10);
			Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, to.add(0, 1, 0), 16, 0.4, 0.1);
			sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 0.8F);
		}
	}

	/** Ancient Seed: a flower seal opening where the seed goes in. */
	static void ancientSeed(ServerLevel level, Vec3 at) {
		ElementFx.bloom(level, at.add(0, 0.4, 0), at, 1.0);
		ElementFx.leafSpiral(level, at, 0.6, 1.4, 8);
		Vfx.emit(level, ParticleTypes.EGG_CRACK, at.add(0, 0.2, 0), 6, 0.2, 0.02);
		sound(level, at, SoundEvents.SNIFFER_EGG_CRACK, 0.9F, 1.1F);
	}

	static void sprout(ServerLevel level, BlockPos pos) {
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos), 3, 0.3, 0.0);
	}

	// ------------------------------------------------------------------ attuned in the biomes

	/** Moonpetal: a pale moon disc over the point and cherry petals whirling out from under it. */
	static void moonpetal(ServerLevel level, Vec3 point, double radius) {
		Vec3 feet = CastEngine.ground(level, point.add(0, 0.5, 0));
		ElementFx.sigil(level, feet.add(0, 3.2, 0), UP, SigilOption.CIRCLE, MOON, radius * 0.8, 20, 0.02);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, PETAL, radius * 1.05, 24, 0.06);
		ElementFx.leafSpiral(level, feet, radius * 0.7, 2.0, 12);
		Vfx.emit(level, ParticleTypes.CHERRY_LEAVES, feet.add(0, 1.4, 0), 30, radius * 0.6, 0.05);
		ElementFx.groundRing(level, feet, PETAL, 0.3, radius, 0.08, 12);
		sound(level, feet, SoundEvents.CHERRY_LEAVES_BREAK, 1.0F, 0.8F);
		sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.8F);
	}

	static void petalMend(ServerLevel level, LivingEntity t) {
		ElementFx.petals(level, centre(t), 0.4, 5);
		Vfx.emit(level, ParticleTypes.HEART, t.position().add(0, t.getBbHeight() + 0.2, 0), 1, 0.2, 0.0);
	}

	/** Hoarfrost: rime creeping up the target a stage at a time; the last stage shatters into ice. */
	static void hoarfrost(ServerLevel level, LivingEntity t, int stage) {
		Vec3 feet = t.position();
		double height = t.getBbHeight() * (0.3 + 0.25 * Math.min(stage, 2));
		ElementFx.ring(level, feet.add(0, height, 0), UP, ElementFx.FROST.secondary(), t.getBbWidth() + 0.3, t.getBbWidth() * 0.6, 0.05, 10);
		ElementFx.frostCreep(level, feet, 0.8 + stage * 0.3, 20);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, feet.add(0, height, 0), 4 + stage * 2, 0.3, 0.01);
		if (stage >= 3) {
			ElementFx.frostImpact(level, centre(t), 1.1);
			Feels.sound(level, feet, "frost_break", 0.8F, 1.0F);
		} else {
			Feels.sound(level, feet, "frost_creep", 0.8F, 1.0F + stage * 0.12F);
		}
	}

	/** Hoarfrost about to close: the rime flares white and draws in tight. */
	static void hoarfrostFlash(ServerLevel level, LivingEntity t) {
		Sigils.flash(level, centre(t), 0xFFFFFF, 1.1F);
		ElementFx.ring(level, centre(t), UP, ElementFx.FROST.secondary(), t.getBbWidth() + 0.9, t.getBbWidth() * 0.5, 0.05, 6);
		Feels.sound(level, t.position(), "frost_tick", 0.5F, 1.4F);
	}

	/** Hush opens: a ring of darkness falling in around the point. */
	static void hushOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		ElementFx.flatSigil(level, centre, SigilOption.RING, ElementFx.dark(ElementFx.VOID.accent()), radius * 1.05, ticks + 10, 0.01);
		ElementFx.implode(level, centre.add(0, 1, 0), radius * 0.6, 14);
		sound(level, centre, SoundEvents.SCULK_CLICKING, 0.6F, 0.5F);
	}

	/** Hush: the dome's rim, and sculk souls drifting down into the silence. */
	static void hush(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.groundRing(level, centre, SCULK, radius, radius * 0.95, 0.06, 20);
		Vfx.emit(level, ParticleTypes.SCULK_SOUL, centre.add(0, 1.5, 0), 3, radius * 0.4, 0.0);
		Vfx.emit(level, ParticleTypes.SQUID_INK, centre.add(0, 1.0, 0), 4, radius * 0.4, 0.0);
	}

	/** Sporebloom: a red cap bursting open over the point, spores drifting out. */
	static void sporebloom(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.bloom(level, centre.add(0, 1, 0), centre, radius * 0.5);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()), centre.add(0, 0.6, 0), 14, 0.5, 0.1);
		Vfx.emit(level, ParticleTypes.SPORE_BLOSSOM_AIR, centre.add(0, 1.2, 0), 30, radius * 0.5, 0.01);
		Vfx.emit(level, ParticleTypes.MYCELIUM, centre.add(0, 0.3, 0), 20, radius * 0.5, 0.0);
		ElementFx.groundRing(level, centre, 0xC8506A, 0.3, radius, 0.08, 14);
		sound(level, centre, SoundEvents.FUNGUS_BREAK, 1.0F, 0.7F);
		sound(level, centre, WildercordSounds.impact("life"), 0.6F, 0.8F);
	}

	/** Sunscorch: a beam of sunlight burning down onto the target; brighter under the open sky. */
	static void sunscorch(ServerLevel level, LivingEntity t, boolean sunlit) {
		Vec3 at = centre(t);
		Vec3 top = at.add(0, sunlit ? 12 : 6, 0);
		ElementFx.ray(level, top, t.position(), ElementFx.FIRE.secondary(), sunlit ? 0.35 : 0.22, 8);
		ElementFx.ray(level, top, t.position(), WHITE, 0.08, 6);
		ElementFx.flatSigil(level, t.position(), SigilOption.STAR, ElementFx.FIRE.secondary(), 1.6, 12, 0.2);
		ElementFx.fireImpact(level, at, sunlit ? 1.2 : 0.9);
		sound(level, at, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.8F);
		sound(level, at, WildercordSounds.impact("fire"), 0.8F, 1.0F);
	}

	/** Mire: mud bubbling round the target's feet. */
	static void mire(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 feet = t.position();
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), feet.add(0, 0.1, 0), first ? 12 : 4, 0.4, 0.05);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, feet.add(0, 0.2, 0), 3, 0.4, 0.02);
		if (first) {
			ElementFx.crack(level, feet, 1.2, 40);
			sound(level, feet, SoundEvents.MUD_BREAK, 1.0F, 0.7F);
		}
	}

	/** Glowvine: berries lighting up as the vines come down. */
	static void glowvine(ServerLevel level, Vec3 at) {
		glow(level, 0xFFC04A, at, 0.8);
		ElementFx.petals(level, at, 0.4, 4);
		sound(level, at, SoundEvents.CAVE_VINES_PLACE, 1.0F, 1.0F);
	}

	static void glowvineFizzle(ServerLevel level, Vec3 at) {
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, at.add(0, 0.5, 0), 4, 0.3, 0.0);
		sound(level, at, SoundEvents.CAVE_VINES_PLACE, 0.5F, 1.6F);
	}

	/** Rootsnare: roots bursting up in a ring and closing over the point. */
	static void rootsnare(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.crack(level, centre, radius, 14);
		ElementFx.leafSpiral(level, centre, radius * 0.8, 1.2, 10);
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			Vec3 edge = centre.add(Math.cos(a) * radius * 0.8, 0, Math.sin(a) * radius * 0.8);
			ElementFx.ray(level, edge, edge.add(0, 1.4, 0).lerp(centre.add(0, 1.4, 0), 0.4), 0x6B4A2E, 0.09, 12);
		}
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), centre.add(0, 0.3, 0), 20, radius * 0.5, 0.1);
		sound(level, centre, SoundEvents.ROOTS_BREAK, 1.0F, 0.7F);
		sound(level, centre, WildercordSounds.impact("life"), 0.6F, 0.9F);
	}

	static void rooted(ServerLevel level, LivingEntity t) {
		ElementFx.ring(level, t.position().add(0, 0.4, 0), UP, 0x6B4A2E, t.getBbWidth() + 0.4, t.getBbWidth() * 0.6, 0.07, 30);
	}

	/** Stalactite: a warning drip over the target, then the spike falls and shatters. */
	static void stalactiteWarn(ServerLevel level, LivingEntity t) {
		Vec3 top = t.position().add(0, t.getBbHeight() + 3.5, 0);
		Vfx.emit(level, ParticleTypes.DRIPPING_DRIPSTONE_WATER, top, 3, 0.1, 0.0);
		Sigils.target(level, t.position(), ElementFx.EARTH.primary(), 1.2F, 10);
		sound(level, top, SoundEvents.POINTED_DRIPSTONE_FALL, 0.8F, 1.0F);
	}

	/** A stalactite that fell on nobody: it shatters on the ground where it was aimed. */
	static void stalactiteMiss(ServerLevel level, Vec3 spot) {
		ElementFx.stoneShards(level, spot.add(0, 0.2, 0), Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 8, 0.15);
		sound(level, spot, SoundEvents.POINTED_DRIPSTONE_LAND, 0.7F, 0.9F);
	}

	static void stalactite(ServerLevel level, LivingEntity t) {
		Vec3 head = t.position().add(0, t.getBbHeight(), 0);
		ElementFx.ray(level, head.add(0, 4, 0), head, ElementFx.EARTH.secondary(), 0.18, 4);
		ElementFx.stoneShards(level, head, Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 12, 0.2);
		ElementFx.earthImpact(level, head, 0.7);
		sound(level, head, SoundEvents.POINTED_DRIPSTONE_LAND, 1.0F, 0.9F);
	}

	/** Summit Wind: a howling updraft, gusts spiralling up round the point (or round the caster, carried off). */
	static void summitWind(ServerLevel level, Vec3 at, double radius, boolean self) {
		ElementFx.swirl(level, at, radius * 0.8, self ? 4.0 : 3.0, 5, WHITE, ElementFx.WIND.primary());
		ElementFx.gustRing(level, CastEngine.ground(level, at.add(0, 0.5, 0)), radius * 1.3);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, at.add(0, 1.5, 0), 16, radius * 0.5, 0.2);
		Vfx.emit(level, ParticleTypes.GUST, at.add(0, 0.6, 0), 2, radius * 0.3, 0.0);
		Feels.sound(level, at, "wind_rise", 1.0F, 0.6F);
	}

	/** Soulfire: blue flames licking up the target. */
	static void soulfire(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 feet = t.position();
		ElementFx.tongues(level, feet, t.getBbWidth() * 0.6, t.getBbHeight(), first ? 5 : 3, SOUL, 0xB0F4FF, 5, 10);
		Vfx.emit(level, ParticleTypes.SOUL_FIRE_FLAME, feet.add(0, t.getBbHeight() * 0.5, 0), first ? 12 : 5, 0.3, 0.02);
		Vfx.emit(level, ParticleTypes.SOUL, feet.add(0, t.getBbHeight(), 0), 1, 0.2, 0.02);
		if (first) {
			ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, SOUL, 1.6, 14, 0.2);
			sound(level, feet, SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.9F);
		}
		sound(level, feet, SoundEvents.FIRE_AMBIENT, 0.6F, 1.4F);
	}

	/** Warp Step: warped spores and a violet streak between where the caster was and where they are. */
	static void warpStep(ServerLevel level, Vec3 from, Vec3 to) {
		ElementFx.implode(level, from.add(0, 1, 0), 1.0, 8);
		ElementFx.ray(level, from.add(0, 1, 0), to.add(0, 1, 0), 0x2AB8A0, 0.05, 6);
		Vfx.emit(level, ParticleTypes.WARPED_SPORE, from.add(0, 1, 0), 16, 0.4, 0.05);
		Vfx.emit(level, ParticleTypes.WARPED_SPORE, to.add(0, 1, 0), 16, 0.4, 0.05);
		ElementFx.ring(level, to.add(0, 1, 0), UP, 0x2AB8A0, 1.2, 0.3, 0.07, 8);
		sound(level, from, WildercordSounds.BLINK, 0.8F, 1.2F);
		sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.7F, 1.3F);
	}

	static void warpStay(ServerLevel level, LivingEntity caster) {
		Vfx.emit(level, ParticleTypes.WARPED_SPORE, caster.position().add(0, 1, 0), 10, 0.4, 0.02);
	}

	static void warpFizzle(ServerLevel level, Vec3 at) {
		Motes.smoke(level, at, 2, 0.2);
		sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.6F);
	}

	/** Blood Moss: crimson spores on the target and a thin thread of blood back to the caster. */
	static void bloodMoss(ServerLevel level, LivingEntity t, LivingEntity caster, boolean first) {
		Vec3 at = centre(t);
		Vfx.emit(level, ParticleTypes.CRIMSON_SPORE, at, first ? 14 : 5, 0.35, 0.02);
		ElementFx.drip(level, at, 0.3, first ? 4 : 2);
		ElementFx.ray(level, at, centre(caster), ElementFx.BLOOD.primary(), 0.025, 5);
		if (first) {
			ElementFx.pulse(level, t.position().add(0, 0.1, 0), UP, 1.1);
			sound(level, at, SoundEvents.MOSS_PLACE, 1.0F, 0.7F);
		}
	}

	/** Basalt Surge: a column of basalt jutting up out of the ground and sinking back. */
	static void basaltColumn(ServerLevel level, Vec3 at, double width) {
		ElementFx.ray(level, at, at.add(0, 2.2, 0), 0x4A4A52, 0.35 * width, 10);
		ElementFx.ray(level, at, at.add(0, 1.4, 0), ElementFx.FIRE.primary(), 0.08 * width, 6);
		ElementFx.stoneShards(level, at.add(0, 0.4, 0), Blocks.BASALT.defaultBlockState(), 8, 0.25);
		Vfx.emit(level, ParticleTypes.WHITE_ASH, at.add(0, 1, 0), 6, 0.4, 0.02);
		ElementFx.crack(level, at, width, 10);
		sound(level, at, SoundEvents.BASALT_BREAK, 1.0F, 0.6F);
	}

	/** Starlight Tether: a thread of starlight from the anchor to the target; a bright flash when it yanks. */
	static void tether(ServerLevel level, Vec3 anchor, LivingEntity t, boolean strong) {
		Vec3 at = centre(t);
		ElementFx.ray(level, anchor.add(0, 0.3, 0), at, STAR, strong ? 0.07 : 0.03, strong ? 6 : 5);
		if (strong) {
			ElementFx.starSeal(level, anchor.add(0, 0.08, 0), UP, 1.1, 20);
			glow(level, STAR, at, 0.8);
			sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
			sound(level, at, WildercordSounds.impact("arcane"), 0.5F, 1.2F);
		}
	}

	// ------------------------------------------------------------------ dungeons, bosses and events

	/** Cinderbrand: a brand of fire seared onto the target. */
	static void cinderbrand(ServerLevel level, LivingEntity t) {
		Vec3 at = centre(t);
		ElementFx.flatSigil(level, t.position(), SigilOption.STAR, ElementFx.FIRE.accent(), 1.4, 20, 0.3);
		ElementFx.heatFlare(level, at, 0.8);
		ElementFx.embers(level, at, 0.4, 6);
		sound(level, at, SoundEvents.FIRECHARGE_USE, 0.8F, 1.3F);
	}

	/** Ashen Veil: grey ash whirling round the target. */
	static void ashenVeil(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 feet = t.position();
		ElementFx.swirl(level, feet, 0.7, t.getBbHeight(), first ? 3 : 1, 0x8A8480, ElementFx.FIRE.primary());
		Vfx.emit(level, ParticleTypes.ASH, feet.add(0, t.getBbHeight() * 0.5, 0), first ? 16 : 6, 0.4, 0.02);
		if (first) {
			sound(level, feet, SoundEvents.FIRE_AMBIENT, 0.8F, 0.6F);
			sound(level, feet, WildercordSounds.cast("fire"), 0.5F, 0.8F);
		}
	}

	static void ashIgnite(ServerLevel level, LivingEntity t, LivingEntity attacker) {
		ElementFx.ray(level, centre(t), centre(attacker), ElementFx.FIRE.primary(), 0.05, 4);
		ElementFx.fireImpact(level, centre(attacker), 0.6);
	}

	/** Cinderheart: a heat flare at the heart and flame tongues ringing the target. */
	static void cinderheart(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 feet = t.position();
		ElementFx.groundRing(level, feet, ElementFx.FIRE.primary(), 0.4, 4.0, 0.1, 12);
		ElementFx.tongues(level, feet, 1.6, 1.2, first ? 8 : 4, ElementFx.FIRE.primary(), ElementFx.FIRE.secondary(), 5, 10);
		if (first) {
			ElementFx.heatFlare(level, centre(t), 1.4);
			ElementFx.flatSigil(level, feet, SigilOption.STAR, ElementFx.FIRE.accent(), 3.2, 30, 0.1);
			sound(level, feet, SoundEvents.BLAZE_AMBIENT, 1.0F, 0.6F);
			sound(level, feet, WildercordSounds.cast("fire"), 0.8F, 0.7F);
		}
	}

	/** Eclipse opens: a disc of darkness over the point, its rim burning white. */
	static void eclipseOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		Vec3 sky = centre.add(0, 4.5, 0);
		ElementFx.sigil(level, sky, UP, SigilOption.CIRCLE, ElementFx.dark(ElementFx.VOID.accent()), radius * 1.4, ticks + 10, 0.01);
		ElementFx.ring(level, sky, UP, WHITE, radius * 0.7, radius * 0.72, 0.06, ticks + 10);
		ElementFx.flatSigil(level, centre, SigilOption.RING, ElementFx.dark(ElementFx.VOID.accent()), radius * 1.05, ticks + 10, 0.01);
		sound(level, centre, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.6F);
		sound(level, centre, WildercordSounds.cast("void"), 0.8F, 0.7F);
	}

	static void eclipse(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.groundRing(level, centre, ElementFx.VOID.primary(), radius, radius * 0.7, 0.06, 16);
		Vfx.emit(level, ParticleTypes.SQUID_INK, centre.add(0, 3.5, 0), 6, radius * 0.4, 0.0);
	}

	/** Starmaw: the light drawn out of the target into a black mouth, a star for each good effect swallowed. */
	static void starmaw(ServerLevel level, LivingEntity t, int swallowed) {
		Vec3 at = centre(t);
		ElementFx.blackCore(level, at, 0.9, 12);
		ElementFx.implode(level, at, 2.4, 12);
		for (int i = 0; i < Math.min(8, swallowed * 2 + 2); i++) {
			Vec3 from = at.add(ElementFx.randomDir(level.getRandom()).scale(1.8));
			ElementFx.ray(level, from, at, STAR, 0.04, 6);
		}
		ScreenFx.shake(level, at, 0.4F, 12);
		sound(level, at, SoundEvents.WITHER_SPAWN, 0.35F, 1.8F);
		sound(level, at, WildercordSounds.impact("void"), 1.0F, 0.6F);
	}

	/** Drowning Word: a word of water written in the air, bubbles rising from the target's mouth. */
	static void drowningWord(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 eyes = t.getEyePosition();
		Vfx.emit(level, ParticleTypes.BUBBLE, eyes, first ? 12 : 4, 0.2, 0.05);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, eyes.add(0, 0.3, 0), 2, 0.2, 0.02);
		if (first) {
			ElementFx.sigil(level, eyes.add(0, 0.8, 0), UP, SigilOption.BAND, WATER, 1.1, 30, 0.1);
			ElementFx.ring(level, eyes, UP, ElementFx.FROST.primary(), 1.2, 0.2, 0.06, 10);
			Feels.sound(level, eyes, "frost_drag", 0.6F, 1.0F);
		}
	}

	/** Tidewrit: the crest of a wall of water rolling forward. */
	static void tidewrit(ServerLevel level, Vec3 front, Vec3 side, double width, boolean sound) {
		Vec3 base = CastEngine.ground(level, front.add(0, 0.5, 0));
		Vec3 a = base.add(side.scale(width / 2));
		Vec3 b = base.subtract(side.scale(width / 2));
		// A wall: a curtain of upright rays, one a block, with a crest along the top and a little spray.
		ElementFx.ray(level, a.add(0, 2.2, 0), b.add(0, 2.2, 0), ElementFx.FROST.secondary(), 0.1, 3);
		for (int i = 0; i <= 6; i++) {
			Vec3 p = a.lerp(b, i / 6.0);
			ElementFx.ray(level, p.add(0, 0.2, 0), p.add(0, 2.2, 0), WATER, 0.14, 4);
			if (i % 2 == 0) {
				Vfx.emit(level, ParticleTypes.SPLASH, p.add(0, 2.0, 0), 3, 0.2, 0.15);
			}
		}
		if (sound) {
			Feels.sound(level, base, "frost_surge", 0.9F, 0.8F);
		}
	}

	/** Starshard: a falling shard of starlight striking the target. */
	static void starshard(ServerLevel level, LivingEntity t) {
		Vec3 at = centre(t);
		ElementFx.ray(level, at.add(1.5, 8, 0.5), at, STAR, 0.18, 5);
		ElementFx.ray(level, at.add(1.5, 8, 0.5), at, WHITE, 0.06, 4);
		ElementFx.arcaneImpact(level, at, 1.1);
		ElementFx.starSeal(level, t.position().add(0, 0.08, 0), UP, 1.6, 14);
		sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);
		sound(level, at, WildercordSounds.impact("arcane"), 0.8F, 1.0F);
	}

	static void starSpark(ServerLevel level, Vec3 from, LivingEntity to) {
		ElementFx.ray(level, from, centre(to), STAR, 0.05, 5);
		glow(level, STAR, centre(to), 0.6);
		sound(level, centre(to), SoundEvents.AMETHYST_BLOCK_HIT, 0.7F, 1.8F);
	}

	/** Riftcall opens: a tear of darkness in the air, violet rimmed. */
	static void riftOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		ElementFx.sigil(level, centre, UP, SigilOption.CRACKED, ElementFx.dark(ElementFx.VOID.accent()), 1.8, ticks + 10, 0.2);
		ElementFx.flatSigil(level, CastEngine.ground(level, centre), SigilOption.RING, ElementFx.VOID.primary(), radius * 1.05, ticks + 10, -0.03);
		sound(level, centre, SoundEvents.PORTAL_TRIGGER, 0.6F, 0.6F);
		sound(level, centre, WildercordSounds.cast("void"), 0.9F, 0.6F);
	}

	static void rift(ServerLevel level, Vec3 centre, double radius, int tick) {
		ElementFx.ring(level, centre, ElementFx.tilted(0.5, tick * 0.3), ElementFx.dark(ElementFx.VOID.accent()), radius * 0.8, 0.4, 0.08, 8);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, centre, 4, radius * 0.4, 0.05);
	}

	/** Riftcall snaps shut: the darkness collapses and bursts. */
	static void riftClose(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.voidImpact(level, centre, radius * 0.5);
		ScreenFx.shake(level, centre, 0.35F, 10);
		sound(level, centre, WildercordSounds.DOMAIN_CLOSE, 0.8F, 1.3F);
	}

	/** Manaburn: violet-pink fire on the target, flaring brighter on a caster as its mana burns. */
	static void manaburn(ServerLevel level, LivingEntity t, boolean caster) {
		Vec3 at = centre(t);
		ElementFx.tongues(level, t.position(), t.getBbWidth() * 0.6, t.getBbHeight(), caster ? 6 : 3, ElementFx.ARCANE.primary(), 0x9A7CFF, 5, 10);
		ElementFx.arcaneImpact(level, at, caster ? 1.1 : 0.7);
		Vfx.emit(level, ParticleTypes.ENCHANT, at, caster ? 20 : 8, 0.5, 0.4);
		sound(level, at, WildercordSounds.impact("arcane"), 0.8F, caster ? 0.7F : 1.0F);
	}

	/** Manatide: mana flowing into the target as motes drawn in from all round. */
	static void manatide(ServerLevel level, LivingEntity t, boolean first) {
		Vec3 at = centre(t);
		for (int i = 0; i < (first ? 8 : 3); i++) {
			Vec3 from = at.add(ElementFx.randomDir(level.getRandom()).scale(2.0));
			Vfx.fling(level, ParticleTypes.ENCHANT, from, at.subtract(from), 0.6);
		}
		if (first) {
			ElementFx.orbit(level, at, 0.9, 3, 30);
			ElementFx.starSeal(level, t.position().add(0, 0.08, 0), UP, 1.4, 20);
			sound(level, at, SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.6F);
		}
	}

	static void manatideSpent(ServerLevel level, LivingEntity t) {
		Motes.smoke(level, centre(t), 2, 0.2);
	}

	// ------------------------------------------------------------------ shapes

	/** Vortex opens: the spell's circle on the ground, turning. */
	static void vortexOpen(ServerLevel level, Vec3 centre, double radius, Vfx.Theme theme, int ticks) {
		Sigils.ground(level, centre, theme.primary(), theme.secondary(), (float) (radius * 0.9), ticks + 10);
		Fx.sound(level, centre, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8F, 0.6F);
		Fx.sound(level, centre, WildercordSounds.CIRCLE_OPEN, 0.7F, 0.9F);
	}

	/** A Vortex turning: crescents of light spiralling in toward its eye. */
	static void vortex(ServerLevel level, Vec3 centre, double radius, double eye, Vfx.Theme theme, int tick) {
		double spin = tick * 0.45;
		for (int i = 0; i < 3; i++) {
			double a = spin + i * Math.PI * 2 / 3;
			Light.slash(level, centre.add(0, 0.5 + i * 0.5, 0), UP, ElementFx.flatDir(a), i == 1 ? theme.secondary() : theme.primary(),
				radius * (0.9 - i * 0.2), 1.4, 0.07, 4, 6);
		}
		Light.ring(level, centre.add(0, 1.0, 0), UP, theme.primary(), eye * 1.2, eye * 0.3, 0.06, 6);
		Vfx.emit(level, theme.mote(), centre.add(0, 1, 0), 3, eye * 0.6, 0.05);
		if (tick % 10 == 0) {
			Fx.sound(level, centre, SoundEvents.BREEZE_IDLE_GROUND, 0.7F, 0.7F);
		}
	}

	/** A Snare is strung: a faint thread of light, gone in a moment. */
	static void snareSet(ServerLevel level, Vec3 a, Vec3 b, Vfx.Theme theme) {
		Light.ray(level, a, b, theme.primary(), 0.02, 12);
		Sigils.target(level, a, theme.primary(), 0.6F, 12);
		Sigils.target(level, b, theme.primary(), 0.6F, 12);
		Fx.sound(level, a, SoundEvents.TRIPWIRE_ATTACH, 0.8F, 1.2F);
	}

	/** A waiting Snare glints now and then, only near its posts. */
	static void snareIdle(ServerLevel level, Vec3 a, Vec3 b, Vfx.Theme theme) {
		Vfx.emit(level, new DustParticleOptions(theme.primary(), 0.5F), a.lerp(b, level.getRandom().nextDouble()), 1, 0.0, 0.0);
	}

	/** A Snare springs: the thread flares, then the spell's circle bursts where it was crossed. */
	static void snareSpring(ServerLevel level, Vec3 a, Vec3 b, Vec3 at, double radius, Vfx.Theme theme) {
		Light.ray(level, a, b, theme.primary(), 0.08, 6);
		Light.ray(level, a, b, WHITE, 0.03, 4);
		Sigils.ground(level, CastEngine.ground(level, at.add(0, 0.5, 0)), theme.primary(), theme.secondary(), (float) radius, 12);
		Light.groundRing(level, CastEngine.ground(level, at.add(0, 0.5, 0)), theme.primary(), 0.3, radius, 0.08, 10);
		Fx.sound(level, at, SoundEvents.TRIPWIRE_CLICK_ON, 1.0F, 1.0F);
		Fx.sound(level, at, theme.impact(), 0.8F, 1.0F);
	}

	/** Constellation: stars over each target, joined by lines of light in the order they're struck. */
	static void constellation(ServerLevel level, java.util.List<Vec3> stars, Vfx.Theme theme) {
		for (int i = 0; i < stars.size(); i++) {
			Vec3 star = stars.get(i);
			Sigils.layer(level, star.add(0, 0.9, 0), UP, SigilOption.STAR, theme.secondary(), 0.7F, 14, 0.2F);
			glow(level, theme.primary(), star, 0.8);
			if (i > 0) {
				Light.ray(level, stars.get(i - 1), star, theme.primary(), 0.04, 12);
			}
		}
		if (stars.size() > 2) {
			Light.ray(level, stars.getLast(), stars.getFirst(), theme.secondary(), 0.025, 12);
		}
		if (!stars.isEmpty()) {
			Fx.sound(level, stars.getFirst(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
			Fx.sound(level, stars.getFirst(), theme.impact(), 0.7F, 1.2F);
		}
	}

	// ------------------------------------------------------------------ fished from open water

	/** Where a line leaves the caster's hand. */
	private static Vec3 hand(LivingEntity caster) {
		return caster.getEyePosition().subtract(0, 0.4, 0);
	}

	/** Tidehook: a line of water cast from the caster's hand, and a hook of pale light biting the target in a splash. */
	static void tidehook(ServerLevel level, LivingEntity caster, LivingEntity t) {
		Vec3 from = hand(caster);
		Vec3 to = centre(t);
		ElementFx.ray(level, from, to, WATER, 0.07, 8);
		ElementFx.ray(level, from, to, ElementFx.FROST.secondary(), 0.025, 7);
		Vec3 dir = to.subtract(from).normalize();
		Vec3 side = dir.cross(UP).lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : dir.cross(UP).normalize();
		// The hook: a curl of light round the target's middle, bending back toward the caster.
		ElementFx.slash(level, to, side, dir.scale(-1), ElementFx.FROST.primary(), t.getBbWidth() * 0.5 + 0.35, 1.6, 0.07, 3, 10);
		ElementFx.ring(level, to, dir, WATER, 0.2, t.getBbWidth() + 0.5, 0.05, 8);
		Vfx.emit(level, ParticleTypes.SPLASH, to, 16, 0.35, 0.15);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, to, 6, 0.3, 0.05);
		Feels.sound(level, from, "frost_hook", 0.9F, 1.0F);
	}

	/** One of Tidehook's tugs: the line pulled taut back to the caster, water shaken off the catch. */
	static void tidehookTug(ServerLevel level, LivingEntity caster, LivingEntity t, int tug) {
		Vec3 from = hand(caster);
		Vec3 to = centre(t);
		ElementFx.ray(level, from, to, WATER, 0.05, 4);
		ElementFx.ray(level, from, to, ElementFx.FROST.secondary(), 0.02, 3);
		Vfx.emit(level, ParticleTypes.SPLASH, to, 10, 0.3, 0.1);
		Vfx.emit(level, ParticleTypes.DRIPPING_WATER, to.add(0, 0.3, 0), 4, 0.3, 0.0);
		Feels.sound(level, from, "frost_hook", 0.5F, 1.0F + tug * 0.12F);
	}

	/** Current: a ring of water round the rider as it takes hold, then spray and bubbles streaming off behind them. */
	static void current(ServerLevel level, LivingEntity rider, Vec3 dir, boolean first) {
		Vec3 at = centre(rider);
		Vec3 behind = at.subtract(dir.scale(0.8));
		if (first) {
			ElementFx.ring(level, at, dir, WATER, 0.3, 1.6, 0.1, 8);
			ElementFx.ring(level, behind, dir, ElementFx.FROST.secondary(), 0.2, 1.1, 0.05, 6);
			Vfx.emit(level, ParticleTypes.SPLASH, rider.position().add(0, 0.2, 0), 24, 0.6, 0.2);
			Feels.sound(level, at, "frost_surge", 0.7F, 1.2F);
		} else {
			ElementFx.ring(level, behind, dir, WATER, 0.9, 0.3, 0.06, 5);
		}
		Vfx.emit(level, rider.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH, behind, first ? 10 : 6, 0.3, 0.05);
		Vfx.emit(level, ParticleTypes.FISHING, behind, 3, 0.2, 0.02);
	}

	/** Current on dry land: a few drops and a hiss of mist, and nothing more. */
	static void currentFizzle(ServerLevel level, LivingEntity rider) {
		Vec3 at = rider.position().add(0, 0.3, 0);
		Vfx.emit(level, ParticleTypes.DRIPPING_WATER, centre(rider), 5, 0.3, 0.0);
		Motes.clouds(level, at, 2, 0.3, 0xDCEBFF, 0.7, 20, new Vec3(0, 0.03, 0), 0.01, 0.3);
		sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.35F, 1.8F);
	}
}
