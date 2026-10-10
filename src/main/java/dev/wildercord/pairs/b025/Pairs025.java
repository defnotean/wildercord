package dev.wildercord.pairs.b025;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs025 {
	private Pairs025() {}

	/**
	 * Skyhammer: a bolt pins up to two enemies, then a meteor falls on each spot 1.2 seconds later. The look: a
	 * shrinking shadow ring under each target, a jagged strike, then a falling fiery line.
	 */
	@Pair(a = "lightning", b = "meteor", name = "Skyhammer", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Lightning strikes up to 2 enemies for 6 damage and Slowness II for 3 seconds. 1.2 seconds later a meteor falls "
			+ "on each strike's spot: 8 fire damage to every enemy within 3 blocks of it, and they burn for 2 seconds.")
	public static void skyhammer(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 2);
		List<Vec3> spots = new ArrayList<>();
		for (LivingEntity t : struck) {
			Vec3 at = PairCast.mid(t);
			spots.add(at);
			c.shock(t, 6 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 3, 1);
			c.bolt(at);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 9, 0), at, 0.5, 3);
		}
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, c.point(), 0.6F, 1.2F);
		// Wind-up: a shadow ring on each spot, shrinking as the meteor comes down.
		c.every(6, 4, frame -> {
			for (Vec3 at : spots) {
				c.ring(PairCast.shift(0xC9D6FF, 0x3B2A1A, 1.0F), at.add(0, 0.1, 0), 3.0 - 0.6 * frame, 16, frame);
			}
		});
		c.later(24, () -> {
			Set<LivingEntity> blast = new LinkedHashSet<>();
			for (Vec3 at : spots) {
				c.line(PairCast.shift(0xFFE3A3, 0xFF5A1A, 1.2F), at.add(0, 10, 0), at, 2);
				c.wave(PairCast.dust(0xFFAA40, 1.2F), at, 20, 0.3);
				c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.6F, 1.2F);
				c.shake(at, 0.3F, 8);
				blast.addAll(c.enemiesNear(at, 3 * c.radius));
			}
			for (LivingEntity e : blast) {
				c.burn(e, 8 * c.power);
				c.ignite(e, 2);
			}
		});
	}

	/**
	 * Lodestone: pulls up to four enemies to one point and holds them there. They are drawn back in and shocked
	 * on a beat. The look: lines of pale blue drawn taut to a sphere of magnetic light.
	 */
	@Pair(a = "magnetize", b = "pull", name = "Lodestone", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Pulls up to 4 enemies to where it landed, with Slowness I, marked pulled for 4 seconds. Held there for 3 seconds: "
			+ "drawn back in and shocked for 3 damage at 0, 1, 2 and 3 seconds.")
	public static void lodestone(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> held = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : held) {
			c.pullTo(t, spot, 1.2);
			c.mark(t, Reactions.Mark.PULLED);
			c.effect(t, MobEffects.SLOWNESS, 4, 0);
			c.line(PairCast.shift(0x7FD4FF, 0xB0A0FF, 0.8F), PairCast.mid(t), spot, 3);
		}
		c.sound(SoundEvents.LODESTONE_COMPASS_LOCK, spot, 0.8F, 0.9F);
		c.every(10, 7, frame -> {
			c.sphere(PairCast.dust(0xB0A0FF, 1.0F), spot, 1.2, 20);
			for (LivingEntity t : c.still(held)) {
				c.pullTo(t, spot, 0.6);
				if (frame % 2 == 0) {
					c.shock(t, 3 * c.power);
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, spot, PairCast.mid(t), 0.3, 2);
					c.sound(SoundEvents.AMETHYST_BLOCK_HIT, PairCast.mid(t), 0.5F, 0.8F + 0.1F * frame);
				}
			}
		});
	}

	/**
	 * Plasmabloom: a fiery blast throws what it catches, and the plasma it leaves hangs on the spot for four
	 * seconds, ionising and burning whatever stands in it. The look: a white flash, a violet shockwave, then a
	 * flickering disc of blue and violet.
	 */
	@Pair(a = "explode", b = "plasma", name = "Plasmabloom", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A 3.5-block blast: 8 fire damage to each enemy in it, thrown outward. The plasma it leaves hangs for 4 seconds: "
			+ "each enemy within 2 blocks of the spot takes 2 magic damage (ignoring armour) a second and is ionised. Never breaks blocks.")
	public static void plasmabloom(PairCast c) {
		Vec3 at = c.point();
		for (LivingEntity e : c.enemiesNear(at, 3.5 * c.radius)) {
			c.burn(e, 8 * c.power);
			c.knockFrom(e, at, 0.9, 0.5);
		}
		c.particles(ParticleTypes.EXPLOSION, at, 1, 0, 0);
		c.wave(PairCast.shift(0xE9F6FF, 0x6A3CFF, 1.3F), at, 24, 0.45);
		c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.6F, 1.5F);
		c.shake(at, 0.35F, 8);
		// The plasma pool: a violet disc that flickers, and hurts once a second for four seconds.
		c.every(5, 17, frame -> {
			c.disc(PairCast.shift(0xB48CFF, 0x3FD8FF, 0.8F), at.add(0, 0.1, 0), 2.0 * c.radius, 10);
			if (frame > 0 && frame % 4 == 0) {
				for (LivingEntity e : c.enemiesNear(at, 2.0 * c.radius)) {
					c.hurt(e, 2 * c.power);
					c.mark(e, Reactions.Mark.IONISED);
				}
				c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 0.4F, 1.4F);
			}
		});
	}

	/**
	 * Echo Chime: three arcane strikes ring out, and a second later each one echoes into its neighbours. The look:
	 * violet rings growing from each target, then a wave of cyan crossing to whoever stands near.
	 */
	@Pair(a = "resonance", b = "ripple", name = "Echo Chime", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Strikes up to 3 enemies for 5 arcane damage each, and they glow for 10 seconds. A second later each one echoes: "
			+ "3 storm damage to every other enemy within 3 blocks of it, each enemy hit once.")
	public static void echoChime(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 3);
		List<Vec3> at = new ArrayList<>();
		for (LivingEntity t : struck) {
			c.hurt(t, 5 * c.power);
			c.effect(t, MobEffects.GLOWING, 10, 0);
			at.add(PairCast.mid(t));
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.8F, 1.4F);
		// The chime rings: violet rings grow out of each target for a second.
		c.every(5, 5, frame -> {
			for (Vec3 p : at) {
				c.ring(PairCast.shift(0xE7B8FF, 0x6FD8FF, 0.9F), p, 0.6 + 0.45 * frame, 14, frame * 0.3);
			}
		});
		c.later(20, () -> {
			Set<LivingEntity> echoed = new LinkedHashSet<>();
			for (int i = 0; i < struck.size(); i++) {
				Vec3 p = at.get(i);
				LivingEntity src = struck.get(i);
				c.wave(PairCast.dust(0x6FD8FF, 1.0F), p, 20, 0.35);
				c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, p, 0.7F, 1.0F + 0.1F * i);
				for (LivingEntity e : c.enemiesNear(p, 3 * c.radius)) {
					if (e != src) {
						echoed.add(e);
					}
				}
			}
			for (LivingEntity e : echoed) {
				c.shock(e, 3 * c.power);
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(e), PairCast.mid(e).add(0, 1.2, 0), 0.2, 3);
			}
		});
	}

	/**
	 * Thundertick: each mark gets a clock face and a storm bolt now; its moment catches up 1.5 seconds later, and
	 * lightning strikes the spot twice more. The look: a ring with a hand sweeping round it, then sparks falling
	 * on each mark's spot.
	 */
	@Pair(a = "countdown", b = "stormclock", name = "Thundertick", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Marks up to 3 enemies: 3 storm damage each now. 1.5 seconds later the moment catches up for 3 more, or if the mark "
			+ "is dead, it finds the nearest enemy within 6 blocks. Lightning strikes each mark's spot 2 and 4 seconds on: "
			+ "2 damage to enemies within 1.5 blocks.")
	public static void thundertick(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), 3);
		List<Vec3> spots = new ArrayList<>();
		for (LivingEntity t : marked) {
			spots.add(PairCast.mid(t));
			c.shock(t, 3 * c.power);
		}
		c.sound(SoundEvents.BELL_BLOCK, c.point(), 0.8F, 1.0F);
		// The clock faces: a ring round each mark, and its hand sweeping a quarter turn each beat.
		c.every(10, 4, frame -> {
			for (Vec3 at : spots) {
				Vec3 face = at.add(0, 0.2, 0);
				c.ring(PairCast.dust(0xFFE27A, 0.8F), face, 1.0, 12, 0);
				c.line(PairCast.dust(0xFFF4C2, 0.7F), face, face.add(Math.cos(frame * Math.PI / 2) * 1.0, 0, Math.sin(frame * Math.PI / 2) * 1.0), 4);
			}
		});
		c.later(30, () -> {
			for (int i = 0; i < marked.size(); i++) {
				LivingEntity t = marked.get(i);
				Vec3 at = spots.get(i);
				LivingEntity target = t.isAlive() && c.here(t) ? t : c.nearestEnemy(at, 6 * c.radius, t);
				if (target == null) {
					continue;
				}
				c.line(PairCast.shift(0xFFE27A, 0x3B2A8F, 1.0F), at, PairCast.mid(target), 3);
				c.hurt(target, 3 * c.power);
				c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, PairCast.mid(target), 0.6F, 1.6F);
			}
		});
		c.later(40, () -> thundertickStrike(c, spots));
		c.later(80, () -> thundertickStrike(c, spots));
	}

	private static void thundertickStrike(PairCast c, List<Vec3> spots) {
		for (Vec3 at : spots) {
			c.bolt(at);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 8, 0), at, 0.4, 3);
			for (LivingEntity e : c.enemiesNear(at, 1.5 * c.radius)) {
				c.shock(e, 2 * c.power);
			}
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.5F, 1.3F);
		}
	}

	/**
	 * Boomwake: a sonic boom runs down the line to the first enemy, hurting everything it passes through, and a
	 * thunderclap follows where it lands. The look: pale rings rolling along the line, then a white flash and a
	 * crack of blue.
	 */
	@Pair(a = "sonic_boom", b = "thunderclap", name = "Boomwake", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A sonic boom runs from you to the first enemy: 14 damage to it, ignoring armour, and 8 to everything on the line, "
			+ "through walls. Half a second later a thunderclap at its spot: 4 damage within 3 blocks, Slowness III for half a "
			+ "second, and monsters forget who they were hunting.")
	public static void boomwake(PairCast c) {
		LivingEntity main = c.firstEnemy();
		if (main == null) {
			return;
		}
		Vec3 from = c.origin();
		Vec3 to = PairCast.mid(main);
		Vec3 path = to.subtract(from);
		c.hurt(main, 14 * c.power);
		Set<LivingEntity> onLine = new LinkedHashSet<>();
		for (int i = 1; i <= 8; i++) {
			for (LivingEntity e : c.enemiesNear(from.add(path.scale(i / 8.0)), 1.5)) {
				if (e != main) {
					onLine.add(e);
				}
			}
		}
		for (LivingEntity e : onLine) {
			c.hurt(e, 8 * c.power);
		}
		c.sound(SoundEvents.WARDEN_SONIC_BOOM, from, 0.8F, 1.2F);
		// The boom rolls down the line: a pale ring every few ticks, each further along.
		c.every(3, 4, frame -> {
			Vec3 p = from.add(path.scale((frame + 1) / 4.0));
			c.ring(PairCast.dust(0xDDEBFF, 1.0F), p, 0.8 + 0.2 * frame, 14, frame * 0.4);
		});
		c.later(10, () -> {
			c.tint(to, 10, 0xE8F4FF, 6);
			c.bolt(to);
			c.wave(PairCast.shift(0xE8F4FF, 0x5B6BFF, 1.0F), to, 24, 0.4);
			c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, to, 0.5F, 1.0F);
			c.shake(to, 0.4F, 10);
			for (LivingEntity e : c.enemiesNear(to, 3 * c.radius)) {
				c.shock(e, 4 * c.power);
				c.effect(e, MobEffects.SLOWNESS, 0.5, 2);
				if (e instanceof Mob m && c.movable(m)) {
					m.setTarget(null);
				}
			}
		});
	}

	/**
	 * Hailcell: a hail cloud hangs over the spot for four beats. Each beat hails on the nearest enemy and its bolt
	 * jumps to whoever stands beside it. The look: a grey disc above, white streaks of hail, and blue sparks.
	 */
	@Pair(a = "hail", b = "thunderhead", name = "Hailcell", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A hail cloud hangs over the spot. Four times, a second apart, it hails on the nearest enemy within 4 blocks of the spot: "
			+ "2 frost damage, Slowness II for 1 second, and it's soaked. Its bolt then strikes it for 1 storm damage and jumps for "
			+ "1 more to each other enemy within 2 blocks.")
	public static void hailcell(PairCast c) {
		Vec3 spot = c.point();
		Vec3 cloud = spot.add(0, 4, 0);
		c.sound(SoundEvents.WEATHER_RAIN, cloud, 0.6F, 0.8F);
		c.every(20, 4, frame -> {
			c.disc(PairCast.dust(0x6E7A90, 2.0F), cloud, 2.5 * c.radius, 30);
			LivingEntity target = c.nearestEnemy(spot, 4 * c.radius, null);
			if (target == null) {
				return;
			}
			Vec3 at = PairCast.mid(target);
			c.line(PairCast.dust(0xDDEBFF, 0.8F), cloud, at, 2);
			c.particles(ParticleTypes.SNOWFLAKE, at, 6, 0.3, 0.05);
			c.freeze(target, 2 * c.power);
			c.effect(target, MobEffects.SLOWNESS, 1, 1);
			c.mark(target, Reactions.Mark.SOAKED);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, cloud, at, 0.4, 3);
			c.shock(target, 1 * c.power);
			for (LivingEntity e : c.enemiesNear(at, 2 * c.radius)) {
				if (e != target) {
					c.shock(e, 1 * c.power);
				}
			}
			c.sound(SoundEvents.GLASS_BREAK, at, 0.4F, 1.6F);
		});
	}

	/**
	 * Overclock: every ally in reach is hasted and quickened, then healed in beats over the surge. The look: a
	 * violet-to-gold ring tightening on the caster, and a spiral of gold round each ally as it is mended.
	 */
	@Pair(a = "haste", b = "surge", name = "Overclock", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 6 blocks get Haste II for 30 seconds, and Speed I and Strength I for 8 seconds. Four times, two seconds "
			+ "apart, each one is healed 1 health (half a heart) while the surge lasts.")
	public static void overclock(PairCast c) {
		LivingEntity me = c.caster;
		List<LivingEntity> crew = c.alliesNear(PairCast.mid(me), 6 * c.radius);
		for (LivingEntity a : crew) {
			c.effect(a, MobEffects.HASTE, 30, 1);
			c.effect(a, MobEffects.SPEED, 8, 0);
			c.effect(a, MobEffects.STRENGTH, 8, 0);
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, me.position(), 0.6F, 1.6F);
		// Wind-up: a ring tightening round the caster as the rune takes hold.
		c.every(5, 3, frame -> {
			c.ring(PairCast.shift(0xB48CFF, 0xFFF27A, 0.9F), me.position().add(0, 1, 0), 2.5 - 0.8 * frame, 16, frame);
		});
		// Payoff: a healing beat on each ally, every two seconds.
		c.every(40, 4, frame -> {
			for (LivingEntity a : c.still(crew)) {
				c.heal(a, 1 * c.power);
				c.spiral(PairCast.shift(0xFFF27A, 0xB48CFF, 0.6F), PairCast.mid(a), 0.5, 1.8, 1, 10);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 4, 0.4, 0.05);
			}
			c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, me.position(), 0.5F, 1.0F + 0.1F * frame);
		});
	}

	/**
	 * Storm Standard: a banner of rally, and a lightning answer to threats close by. The look: gusts rising round
	 * the caster, and a bolt from the sky onto whichever enemy comes within reach.
	 */
	@Pair(a = "rally", b = "stormheart", name = "Storm Standard", element = "storm", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 8 blocks get Speed I and Jump Boost I for 12 seconds. Five times, two seconds apart, the nearest "
			+ "enemy within 3 blocks of you is struck by lightning for 2 damage.")
	public static void stormStandard(PairCast c) {
		LivingEntity me = c.caster;
		for (LivingEntity a : c.alliesNear(PairCast.mid(me), 8 * c.radius)) {
			c.effect(a, MobEffects.SPEED, 12, 0);
			c.effect(a, MobEffects.JUMP_BOOST, 12, 0);
		}
		c.sound(SoundEvents.TRIDENT_THUNDER, me.position(), 0.5F, 1.3F);
		// Wind-up: gusts spiralling up round the caster.
		c.every(4, 3, frame -> {
			c.spiral(PairCast.dust(0xDDF4FF, 1.0F), PairCast.mid(me), 1.2 - 0.3 * frame, 2.0, 1.5, 14);
		});
		// Payoff: the standard's lightning, five times.
		c.every(40, 5, frame -> {
			LivingEntity t = c.nearestEnemy(PairCast.mid(me), 3 * c.radius, null);
			if (t == null) {
				return;
			}
			Vec3 at = PairCast.mid(t);
			c.bolt(at);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 7, 0), at, 0.4, 3);
			c.shock(t, 2 * c.power);
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.4F, 1.4F);
		});
	}

	/**
	 * Stormstride: a surge of Speed and a dash forward along the ground in front, stopped by any wall. Whatever
	 * stands beside the path is struck by lightning. The look: a storm spiral round the caster, then a trail of
	 * sparks drawn behind the dash, and a ring where it stops.
	 */
	@Pair(a = "dynamo_stride", b = "swift", name = "Stormstride", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You dash up to 8 blocks forward, stopping at walls, with Speed II for 6 seconds. Enemies within 2 blocks of your "
			+ "path take 4 lightning damage.")
	public static void stormstride(PairCast c) {
		LivingEntity me = c.caster;
		c.effect(me, MobEffects.SPEED, 6, 1);
		Vec3 look = me.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return;
		}
		flat = flat.normalize();
		Vec3 start = me.position();
		Vec3 eye = start.add(0, 0.5, 0);
		HitResult wall = c.level.clip(new ClipContext(eye, eye.add(flat.scale(8)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, me));
		double reach = wall.getType() == HitResult.Type.MISS ? 8 : Math.max(0, wall.getLocation().distanceTo(eye) - 1.0);
		if (!c.blink(me, start.add(flat.scale(reach)))) {
			return;
		}
		Vec3 landed = me.position();
		Vec3 path = landed.subtract(start);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, start, 0.6F, 1.5F);
		c.spiral(PairCast.dust(0x9FE7FF, 1.0F), PairCast.mid(me), 0.6, 1.8, 2, 16);
		Set<LivingEntity> swept = new LinkedHashSet<>();
		for (int i = 0; i <= 8; i++) {
			swept.addAll(c.enemiesNear(start.add(path.scale(i / 8.0)), 2));
		}
		for (LivingEntity e : swept) {
			c.shock(e, 4 * c.power);
		}
		// The trail: sparks drawn along the dash, a few beats behind the body.
		c.every(2, 5, frame -> {
			Vec3 a = start.add(path.scale(frame / 5.0)).add(0, 0.8, 0);
			Vec3 b = start.add(path.scale((frame + 1) / 5.0)).add(0, 0.8, 0);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, a, b, 0.3, 2);
		});
		c.later(12, () -> {
			c.ring(PairCast.shift(0xDDF4FF, 0x5BD8FF, 1.0F), landed.add(0, 0.2, 0), 1.2, 16, 0);
			c.sound(SoundEvents.WIND_CHARGE_BURST, landed, 0.5F, 1.2F);
			c.shake(landed, 0.2F, 6);
		});
	}
}
