package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Visuals for the second batch of new runes: Glaive, Imprint and Latch, the cue of On Reaction and On
 * Weakness, Kindred, Thirst and Belated, and the ten effects. Built like {@link ExpansionVfx}: shaped
 * light ({@link Light}), circles through {@link Sigils}, particles through {@link Fx} (never in front of
 * a player's own eyes), and each element's colours.
 */
final class CraftedVfx {
	private CraftedVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;
	private static final int FIRE = 0xF06E32;
	private static final int FROST = 0x8CDCFF;
	private static final int STORM = 0xFFE650;
	private static final int WIND = 0xC8F0DC;
	private static final int EARTH = 0xB48C5A;
	private static final int LIFE = 0x6EDC64;
	private static final int VOID = 0xB45AF0;
	private static final int ARCANE = 0xE678DC;
	private static final int TIME = 0xF2D98A;
	private static final int BLOOD = 0xD2283C;
	/** The tag on Prospect's glowing ores (besides the one {@link BlockFx#fresh} gives every short-lived display). */
	static final String PROSPECT_TAG = "wildercord.prospect";

	private static void glow(ServerLevel level, int color, Vec3 at, double size) {
		Vfx.emit(level, SigilOption.glow(color, (float) size), at, 1, 0.0, 0.0);
	}

	private static Vec3 unit(Vec3 v) {
		return v.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : v.normalize();
	}

	private static Vec3 head(Entity t) {
		return t.position().add(0, t.getBbHeight() + 0.45, 0);
	}

	// ------------------------------------------------------------------ Glaive

	static void glaiveLaunch(ServerLevel level, Vec3 origin, Vfx.Theme theme) {
		glow(level, theme.primary(), origin, 0.6);
		Fx.sound(level, origin, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.3F);
		// (the element's cast sound already played once, with the cast circle)
	}

	/** The glaive in flight: two crescent blades spinning flat round a bright hub. */
	static void glaiveTick(ServerLevel level, Vec3 pos, double width, Vfx.Theme theme, int tick) {
		double spin = tick * 1.1;
		Vec3 a = new Vec3(Math.cos(spin), 0, Math.sin(spin));
		double radius = Math.max(0.45, width * 0.7);
		Light.slash(level, pos, UP, a, theme.primary(), radius, 2.2, 0.12, 1, 3);
		Light.slash(level, pos, UP, a.scale(-1), theme.secondary(), radius, 2.2, 0.09, 1, 3);
		glow(level, theme.primary(), pos, 0.45);
		if (tick % 3 == 0) {
			Vfx.emit(level, theme.spark(), pos, 1, 0.1, 0.02);
		}
		if (tick % 6 == 0) {
			Fx.sound(level, pos, SoundEvents.TRIDENT_RIPTIDE_1, 0.25F, 1.9F);
		}
	}

	/** Where it turns back: a ring of light, and a hum as it comes home. */
	static void glaiveTurn(ServerLevel level, Vec3 pos, Vfx.Theme theme) {
		Light.ring(level, pos, UP, theme.primary(), 0.2, 1.2, 0.05, 7);
		Fx.sound(level, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.5F);
	}

	/** Caught (or spent): it folds up into a flash. */
	static void glaiveEnd(ServerLevel level, Vec3 pos, Vfx.Theme theme) {
		Sigils.flash(level, pos, theme.primary(), 0.9F);
		Vfx.radial(level, theme.mote(), pos, 6, 0.08);
		Fx.sound(level, pos, SoundEvents.ITEM_PICKUP, 0.5F, 1.6F);
	}

	// ------------------------------------------------------------------ Imprint

	/** The imprint is pressed into the ground: a circle of the spell's colour that stays till it erupts. */
	static void imprintSet(ServerLevel level, Vec3 feet, double radius, Vfx.Theme theme, int delay) {
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, theme.primary(), (float) radius, delay + 6, 0.02F), feet.add(0, 0.06, 0));
		Sigils.send(level, SigilOption.flat(SigilOption.RING, theme.secondary(), (float) (radius * 0.55), delay + 6, -0.05F), feet.add(0, 0.07, 0));
		Light.groundRing(level, feet, theme.primary(), radius, radius * 0.3, 0.05, 10);
		Fx.sound(level, feet, SoundEvents.MUD_PLACE, 0.8F, 0.8F);
		// (the element's cast sound already played once, with the cast circle)
	}

	/** Every half second while it waits: a pulse closing in, quicker as it's about to go. */
	static void imprintTick(ServerLevel level, Vec3 feet, double radius, Vfx.Theme theme, int left) {
		Light.groundRing(level, feet, theme.primary(), radius, 0.2, 0.04, 8);
		Fx.sound(level, feet, SoundEvents.NOTE_BLOCK_HAT, 0.5F, 1.6F - Math.min(0.8F, left / 60F));
	}

	static void imprintErupt(ServerLevel level, Vec3 feet, double radius, Vfx.Theme theme) {
		Vec3 heart = feet.add(0, 1.0, 0);
		Sigils.flash(level, heart, theme.primary(), (float) Math.min(5, radius * 1.4));
		Light.groundRing(level, feet, theme.primary(), 0.3, radius * 1.1, 0.1, 10);
		Light.ray(level, feet, feet.add(0, 2.6, 0), theme.secondary(), 0.35, 7);
		Vfx.radial(level, theme.mote(), heart, 16, 0.25);
		Vfx.radial(level, theme.spark(), feet.add(0, 0.3, 0), 10, 0.3);
		Fx.sound(level, feet, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.5F);
		Fx.sound(level, feet, theme.impact(), 0.7F, 1.0F);
		ScreenFx.shake(level, feet, 0.25F, 8.0);
	}

	// ------------------------------------------------------------------ Latch

	static void latchOn(ServerLevel level, Vec3 from, Entity target, Vfx.Theme theme) {
		Vec3 c = target.getBoundingBox().getCenter();
		Light.ray(level, from, c, theme.primary(), 0.08, 6);
		Sigils.layer(level, c, unit(c.subtract(from)), SigilOption.TARGET, theme.primary(), (float) Math.max(0.8, target.getBbWidth() + 0.4), 10, 0.2F);
		Fx.sound(level, c, SoundEvents.LEAD_TIED, 0.8F, 1.4F);
		// (the element's cast sound already played once, with the cast circle)
	}

	/** The thread, redrawn as it follows its creature: a thin line of light with a bead running along it. */
	static void latchThread(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int tick) {
		Light.ray(level, from, to, theme.primary(), 0.035, 3);
		double run = (tick % 10) / 10.0;
		glow(level, theme.secondary(), from.lerp(to, run), 0.3);
	}

	static void latchStrike(ServerLevel level, Vec3 from, Vec3 at, Vfx.Theme theme) {
		Light.ray(level, from, at, WHITE, 0.07, 4);
		Sigils.flash(level, at, theme.primary(), 1.0F);
		Light.ring(level, at, unit(at.subtract(from)), theme.primary(), 0.1, 0.8, 0.04, 6);
		Fx.sound(level, at, theme.impact(), 0.5F, 1.2F);
	}

	/** The thread snaps: a spray of motes where it broke. */
	static void latchSnap(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Vfx.radial(level, theme.mote(), at, 8, 0.12);
		Fx.sound(level, at, SoundEvents.LEAD_BREAK, 0.7F, 1.3F);
	}

	// ------------------------------------------------------------------ On Reaction, On Weakness

	/** A link watching for a reaction or a weakness went off at this creature. */
	static void linkSprung(ServerLevel level, Entity t, boolean reaction) {
		Vec3 c = t.getBoundingBox().getCenter();
		int color = 0xA064F0;
		Light.ring(level, c, UP, color, Math.max(0.9, t.getBbWidth() + 0.6), 0.2, 0.05, 8);
		Sigils.layer(level, head(t), UP, reaction ? SigilOption.STAR : SigilOption.TARGET, color, 0.5F, 12, 0.2F);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, reaction ? 1.8F : 1.4F);
	}

	// ------------------------------------------------------------------ Kindred, Thirst, Belated

	/** Kindred: a wisp of the effect's light flies from where it landed to the one it's shared with. */
	static void share(ServerLevel level, Vec3 from, Entity to, Vfx.Theme theme) {
		Vec3 c = to.getBoundingBox().getCenter();
		Motes.seek(level, from, c, theme.primary(), 0.25, 10, 1.0);
		Light.ring(level, to.position().add(0, 0.1, 0), UP, theme.primary(), 0.2, Math.max(0.8, to.getBbWidth() + 0.3), 0.04, 9);
	}

	/** Thirst: a thread of blood drawn from what was hurt back to its caster. */
	static void thirst(ServerLevel level, Entity from, Entity caster) {
		Motes.seek(level, from.getBoundingBox().getCenter(), caster.getBoundingBox().getCenter(), BLOOD, 0.18, 12, 0.5);
	}

	/** Belated: a small clock over each creature it will land on, running down. */
	static void belated(ServerLevel level, Vec3 at, int ticks) {
		Sigils.layer(level, at, UP, SigilOption.CIRCLE, TIME, 0.55F, ticks, 0.25F);
		Light.ring(level, at, UP, TIME, 0.8, 0.2, 0.03, Math.max(6, ticks));
		Fx.sound(level, at, SoundEvents.NOTE_BLOCK_HAT, 0.5F, 0.8F);
	}

	// ------------------------------------------------------------------ the effects

	/** Spellbrand: a sigil burned into the air over the target, turning while it waits. */
	static void spellbrand(ServerLevel level, Entity t, int ticks) {
		Sigils.layer(level, head(t), UP, SigilOption.STAR, ARCANE, 0.55F, Math.min(ticks, 160), 0.1F);
		Light.ring(level, t.getBoundingBox().getCenter(), UP, ARCANE, 0.2, Math.max(0.9, t.getBbWidth() + 0.5), 0.05, 9);
		Vfx.emit(level, ParticleTypes.ENCHANT, head(t), 10, 0.3, 0.2);
		dev.wildercord.cast.feel.Feels.sound(level, t.position(), "arcane_stamp", 0.9F, 1.0F);
	}

	static void spellbrandBurst(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		Sigils.flash(level, c, ARCANE, 2.0F);
		Light.ring(level, c, UP, ARCANE, 0.2, 1.8, 0.07, 9);
		Light.ring(level, c, new Vec3(0.3, 0.2, 0.9).normalize(), 0xFFD8FA, 0.2, 1.4, 0.05, 8);
		Vfx.radial(level, ParticleTypes.ENCHANTED_HIT, c, 14, 0.3);
		dev.wildercord.cast.feel.Feels.sound(level, c, "arcane_stamp_burst", 0.9F, 1.0F);
	}

	/** Gash: a ragged cut and the first drops. */
	static void gash(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vec3 look = unit(new Vec3(-Math.sin(Math.toRadians(t.getYRot())), 0, Math.cos(Math.toRadians(t.getYRot()))));
		Vec3 side = unit(look.cross(UP));
		Light.slash(level, c.add(look.scale(0.4)), look, side.add(0, 0.8, 0).normalize(), BLOOD, Math.max(0.5, t.getBbHeight() * 0.45), 2.0, 0.09, 1, 7);
		gashDrip(level, t);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.7F, 0.7F);
	}

	/** A wound that won't close keeps weeping. */
	static void gashDrip(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		for (int i = 0; i < 2; i++) {
			Vec3 p = c.add((level.getRandom().nextDouble() - 0.5) * t.getBbWidth(), (level.getRandom().nextDouble() - 0.5) * t.getBbHeight() * 0.5,
				(level.getRandom().nextDouble() - 0.5) * t.getBbWidth());
			Vfx.fling(level, new DustParticleOptions(0x8A0A1A, 1.0F), p, new Vec3(0, -1, 0), 0.05);
		}
	}

	/** Prospect: the ground rings, a ripple of earth-coloured light running out as far as it reaches. */
	static void prospectRing(ServerLevel level, Vec3 point, double radius, int found) {
		Vec3 ground = CastEngine.ground(level, point.add(0, 0.5, 0));
		Light.groundRing(level, ground, EARTH, 0.3, radius, 0.08, 16);
		Light.groundRing(level, ground, 0xE8C890, 0.2, radius * 0.6, 0.05, 12);
		Sigils.send(level, SigilOption.flat(SigilOption.TARGET, EARTH, 1.2F, 20, 0.1F), ground.add(0, 0.07, 0));
		Fx.sound(level, ground, SoundEvents.BELL_BLOCK, 0.7F, found > 0 ? 1.4F : 0.8F);
		Fx.sound(level, ground, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.7F);
	}

	/**
	 * One ore Prospect found: the ore itself, a touch smaller, glowing in its own colour through the rock
	 * around it for {@code ticks}. A block display, which never touches the world's blocks and is gone
	 * after a restart (see {@link BlockFx#fresh}). Returns it (null if it couldn't be made), so a newer
	 * Prospect of the same caster's can put it out early.
	 */
	static Display oreGlow(ServerLevel level, BlockPos pos, BlockState ore, int color, int ticks) {
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(pos.getX(), pos.getY(), pos.getZ());
		display.setBlockState(ore);
		display.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(0.1F, 0.1F, 0.1F), new org.joml.Quaternionf(),
			new org.joml.Vector3f(0.8F, 0.8F, 0.8F), new org.joml.Quaternionf()));
		display.setGlowingTag(true);
		display.setGlowColorOverride(color);
		display.addTag(PROSPECT_TAG);
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		Scheduler.later(ticks, display::discard);
		return display;
	}

	/** Searing Edge: flames run up the target's weapon arm. */
	static void searingEdge(ServerLevel level, LivingEntity t) {
		Vec3 hand = hand(t);
		ElementFx.embers(level, hand, 0.2, 6);
		Light.ring(level, t.getBoundingBox().getCenter(), UP, FIRE, 0.2, Math.max(0.9, t.getBbWidth() + 0.5), 0.05, 9);
		Fx.sound(level, hand, SoundEvents.FIRECHARGE_USE, 0.6F, 1.4F);
	}

	/** While it lasts, the weapon smoulders. */
	static void searingGlow(ServerLevel level, LivingEntity t) {
		Vfx.emit(level, ParticleTypes.FLAME, hand(t), 2, 0.08, 0.01);
	}

	/** A seared blow: flames bursting off the foe. */
	static void searingHit(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		ElementFx.embers(level, c, 0.35, 10);
		Sigils.flash(level, c, FIRE, 1.2F);
		Fx.sound(level, c, SoundEvents.FIRECHARGE_USE, 0.5F, 1.2F);
	}

	private static Vec3 hand(LivingEntity t) {
		double yaw = Math.toRadians(t.yBodyRot);
		return t.position().add(-Math.sin(yaw) * 0.35 - Math.cos(yaw) * 0.35, t.getBbHeight() * 0.55, Math.cos(yaw) * 0.35 - Math.sin(yaw) * 0.35);
	}

	/** Flash Freeze: frost bursts over it; a wet target is sealed in ice (the ice itself is {@link BlockFx#encase}). */
	static void flashFreeze(ServerLevel level, Entity t, boolean frozen) {
		Vec3 c = t.getBoundingBox().getCenter();
		Sigils.flash(level, c, FROST, frozen ? 1.8F : 1.0F);
		Light.ring(level, t.position().add(0, 0.1, 0), UP, frozen ? WHITE : FROST, 0.2, Math.max(0.8, t.getBbWidth() + 0.5), 0.05, 9);
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, c, frozen ? 12 : 5, 0.2);
		// A frozen splash: a crown of blue-to-white spikes standing up round the feet (higher when it froze solid).
		int spikes = frozen ? 10 : 6;
		double rim = Math.max(0.5, t.getBbWidth() * 0.8);
		for (int i = 0; i < spikes; i++) {
			double a = Math.PI * 2 * i / spikes;
			Vec3 foot = t.position().add(Math.cos(a) * rim, 0.05, Math.sin(a) * rim);
			ElementFx.ray(level, foot, foot.add(Math.cos(a) * -0.15, (frozen ? 0.9 : 0.5) + 0.25 * (i % 2), Math.sin(a) * -0.15), i % 2 == 0 ? WHITE : FROST, 0.05, 7);
		}
		if (frozen) {
			Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), c, 10, 0.2);
			Feels.sound(level, c, "frost_splat", 1.0F, 1.0F);
		}
	}

	/** Drowse: a puff of pink pollen, and the target sinks into sleep. */
	static void drowse(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		Motes.clouds(level, c, 4, 0.4, 0xF7C6E8, 0.8, 30, new Vec3(0, 0.01, 0), 0.02, 0.35);
		ElementFx.petals(level, c, 0.5, 8);
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_lull", 0.9F, 1.0F);
	}

	/** While it sleeps: pale motes drifting up off its head. */
	static void sleeping(ServerLevel level, Entity t) {
		Motes.glow(level, head(t), 0xFBE7F4, 0.14, 26, new Vec3(0.01, 0.025, 0), 0.01);
	}

	static void wake(ServerLevel level, Entity t) {
		Vfx.emit(level, ParticleTypes.ANGRY_VILLAGER, head(t), 1, 0.1, 0.0);
		Fx.sound(level, t.position(), SoundEvents.PANDA_SNEEZE, 0.5F, 1.3F);
	}

	/** Galvanize: the spark sets itself against the block with a crack. */
	static void galvanize(ServerLevel level, BlockPos pos) {
		Vec3 c = Vec3.atCenterOf(pos);
		Sigils.flash(level, c, STORM, 1.3F);
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, c, 12, 0.25);
		dev.wildercord.cast.feel.Feels.sound(level, c, "storm_pip", 0.8F, 1.0F);
	}

	static void galvanizeHum(ServerLevel level, BlockPos pos) {
		Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, Vec3.atCenterOf(pos), 3, 0.35, 0.05);
		ElementFx.ring(level, Vec3.atCenterOf(pos), new Vec3(0, 1, 0), ElementFx.STORM.primary(), 0.75, 0.6, 0.02, 10);
	}

	/** Nothing to set a spark against (no room, or no building there): it fizzles. */
	static void galvanizeFizzle(ServerLevel level, Vec3 at) {
		Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, at, 4, 0.1, 0.05);
		Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.3F, 1.8F);
	}

	/** Prolong: a clock face turning slowly round the target, gold light stretching out. */
	static void prolong(ServerLevel level, Entity t, boolean any) {
		Vec3 c = t.getBoundingBox().getCenter();
		ElementFx.clock(level, c, UP, Math.max(0.9, t.getBbWidth() + 0.5), 20, false);
		ElementFx.goldenTicks(level, c, 0.5, any ? 10 : 3);
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 0.5F, any ? 1.5F : 0.9F);
	}

	/** Umbra: darkness closes its jaws on the target, deeper where the light was already dim. */
	static void umbra(ServerLevel level, Entity t, boolean dim) {
		Vec3 c = t.getBoundingBox().getCenter();
		ElementFx.blackCore(level, c, dim ? 0.8 : 0.5, 8);
		Light.ring(level, c, UP, VOID | Light.DARK, Math.max(1.0, t.getBbWidth() + 0.8), 0.2, 0.08, 8);
		Vfx.emit(level, ParticleTypes.SQUID_INK, c, dim ? 8 : 4, 0.25, 0.02);
		Fx.sound(level, c, SoundEvents.SCULK_CATALYST_BLOOM, 0.7F, dim ? 0.6F : 0.9F);
	}

	/** Disarm: a snatching gust, and the weapon tumbling up and out of reach. */
	static void disarm(ServerLevel level, Entity t, ItemStack taken) {
		Vec3 c = t.getBoundingBox().getCenter();
		ElementFx.swirl(level, t.position(), Math.max(0.7, t.getBbWidth()), t.getBbHeight(), 3);
		if (!taken.isEmpty()) {
			Vfx.fling(level, new ItemParticleOption(ParticleTypes.ITEM, taken.getItem()), c, new Vec3(0, 1, 0), 0.3);
			Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, taken.getItem()), c, 6, 0.2);
			Feels.sound(level, c, "wind_snatch", 0.9F, 1.0F);
		}
	}

	/** The weapon drifts back into its hand. */
	static void rearm(ServerLevel level, Entity t) {
		Vfx.emit(level, ParticleTypes.SMALL_GUST, t.getBoundingBox().getCenter(), 3, 0.3, 0.02);
		Fx.sound(level, t.position(), SoundEvents.ARMOR_EQUIP_GENERIC, 0.6F, 1.2F);
	}

	static void wind(ServerLevel level, Entity t) {
		Vfx.emit(level, ParticleTypes.SMALL_GUST, t.getBoundingBox().getCenter(), 4, 0.3, 0.02);
	}
}
