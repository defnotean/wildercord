package dev.wildercord.pairs.b023;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs023 {
	private Pairs023() {}

	/**
	 * Null Star: a point of absolute cold draws up to three enemies into its middle, slowed, over two and a half
	 * seconds; then it collapses. The look: a dark frost sphere shrinking round them, spokes of cold pulling in,
	 * and a white burst at the collapse.
	 */
	@Pair(a = "absolute_zero", b = "singularity", name = "Null Star", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 3 enemies are drawn to the point for 2.5 seconds, slowed (Slowness III). Then the star collapses: 8 cold "
			+ "damage to each, and 4 to every other enemy within 2 blocks of the point, which are flung outward.")
	public static void nullStar(PairCast c) {
		Vec3 centre = c.point();
		List<LivingEntity> drawn = PairCast.first(c.enemies(), 3);
		for (LivingEntity t : drawn) {
			c.effect(t, MobEffects.SLOWNESS, 2.5, 2);
		}
		c.sound(SoundEvents.GLASS_PLACE, centre, 0.9F, 0.6F);
		c.every(5, 11, frame -> {
			c.sphere(PairCast.dust(0x2B3F6E, 1.2F), centre, 2.4 - frame * 0.16, 22);
			for (LivingEntity t : c.still(drawn)) {
				c.pullTo(t, centre, 0.5);
				c.line(PairCast.dust(0x9FD8FF, 0.8F), PairCast.mid(t), centre, 3);
			}
			if (frame == 10) {
				collapse(c, centre, drawn);
			}
		});
	}

	private static void collapse(PairCast c, Vec3 centre, List<LivingEntity> drawn) {
		for (LivingEntity t : c.still(drawn)) {
			c.freeze(t, 8 * c.power);
			c.mark(t, Reactions.Mark.FROZEN);
		}
		for (LivingEntity near : c.enemiesNear(centre, 2 * c.radius)) {
			if (!drawn.contains(near)) {
				c.freeze(near, 4 * c.power);
				c.knockFrom(near, centre, 0.9, 0.3);
			}
		}
		c.sphere(PairCast.shift(0x2B3F6E, 0xE8FBFF, 1.3F), centre, 2.2, 40);
		c.sound(SoundEvents.GLASS_BREAK, centre, 1.0F, 0.6F);
		c.shake(centre, 0.3F, 8);
		c.later(4, () -> c.ring(PairCast.dust(0xE8FBFF, 1.0F), centre, 2.6, 24, 0.0));
	}

	/**
	 * Snowquake: the ground heaves and snow comes down on the same spot a second later. The look: dirt rising off
	 * the ground in a ring, then a white column of flakes falling into it.
	 */
	@Pair(a = "avalanche", b = "tremor", name = "Snowquake", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "The ground heaves: up to 6 enemies within 3 blocks are lifted. A second later snow comes down on the spot: "
			+ "5 cold damage to every enemy within 3 blocks, and Slowness III for 3 seconds.")
	public static void snowquake(PairCast c) {
		Vec3 spot = c.point();
		double reach = 3 * c.radius;
		List<LivingEntity> heaved = PairCast.first(c.enemiesNear(spot, reach), 6);
		for (LivingEntity t : heaved) {
			c.lift(t, 0.8);
		}
		c.sound(SoundEvents.STONE_BREAK, spot, 1.0F, 0.7F);
		c.every(4, 5, frame -> {
			c.ring(PairCast.dust(0x8A7A66, 1.1F), spot, reach * (0.4 + 0.15 * frame), 22, frame);
			for (LivingEntity t : c.still(heaved)) {
				c.spiral(PairCast.dust(0xA89B84, 0.9F), PairCast.mid(t), 0.7, 1.4, 1.5, 12);
			}
		});
		c.later(20, () -> {
			c.column(PairCast.dust(0xF4FAFF, 1.0F), spot.add(0, 1, 0), reach, 6, 36);
			c.particles(ParticleTypes.SNOWFLAKE, spot.add(0, 5, 0), 40, reach, 0.08);
			c.sound(SoundEvents.SNOW_PLACE, spot, 1.0F, 0.6F);
			c.shake(spot, 0.35F, 10);
			for (LivingEntity near : c.enemiesNear(spot, reach)) {
				c.freeze(near, 5 * c.power);
				c.effect(near, MobEffects.SLOWNESS, 3, 2);
			}
		});
	}

	/**
	 * Glass Lunge: a dash on black ice. You slide forward in four quick steps and stop at a wall; everything you
	 * brush past is struck cold and weakened. The look: a pale streak across the ground, and cracked shards behind.
	 */
	@Pair(a = "black_ice", b = "dash", name = "Glass Lunge", element = "frost", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You dash up to 8 blocks the way you face, stopped by walls. Each enemy you pass within 1.5 blocks takes "
			+ "3 cold damage and Weakness II for 5 seconds, once each.")
	public static void glassLunge(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 forward = flat(c.dir());
		List<LivingEntity> passed = new ArrayList<>();
		boolean[] stopped = {false};
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, PairCast.mid(self), 0.6F, 1.6F);
		c.every(2, 4, step -> {
			if (stopped[0] || !self.isAlive()) {
				stopped[0] = true;
				return;
			}
			Vec3 from = self.position();
			Vec3 ahead = from.add(forward.scale(2));
			BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.5, 0), ahead.add(0, 0.5, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (wall.getType() != HitResult.Type.MISS || !c.blink(self, ahead)) {
				stopped[0] = true;
				return;
			}
			c.line(PairCast.dust(0xD8F4FF, 0.8F), from.add(0, 0.2, 0), ahead.add(0, 0.2, 0), 2);
			c.particles(ParticleTypes.ITEM_SNOWBALL, from, 6, 0.4, 0.05);
			for (LivingEntity t : c.enemiesNear(PairCast.mid(self), 1.5)) {
				if (!passed.contains(t)) {
					passed.add(t);
					c.freeze(t, 3 * c.power);
					c.effect(t, MobEffects.WEAKNESS, 5, 1);
					c.sound(SoundEvents.GLASS_BREAK, PairCast.mid(t), 0.5F, 1.5F);
				}
			}
		});
	}

	/**
	 * Whiteout: a snow vortex that walks forward and spins whatever it reaches. The look: a pale disc and a spiral
	 * of flakes, moving three blocks a second; on the last beat the whole ring is thrown forward.
	 */
	@Pair(a = "blizzard", b = "cyclone", name = "Whiteout", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A snow vortex starts at the point and walks 6 blocks the way you faced, 3 blocks a second. At each of its "
			+ "three beats (now, 1 and 2 seconds) enemies within 2.5 blocks take 2 cold damage and Slowness II for 2 seconds. "
			+ "On the last beat they're flung forward.")
	public static void whiteout(PairCast c) {
		Vec3 forward = flat(c.dir());
		Vec3 start = c.point();
		double reach = 2.5 * c.radius;
		c.sound(SoundEvents.WIND_CHARGE_BURST, start, 0.8F, 0.8F);
		c.every(20, 3, beat -> {
			Vec3 at = c.ground(start.add(forward.scale(3.0 * beat))).add(0, 1, 0);
			c.disc(PairCast.dust(0xE8F8FF, 1.2F), at, reach, 26);
			c.spiral(PairCast.shift(0xE8F8FF, 0x9FD8FF, 1.0F), at, reach, 3, 2, 30);
			for (LivingEntity t : c.enemiesNear(at, reach)) {
				c.freeze(t, 2 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 1);
				if (beat == 2) {
					c.push(t, forward.scale(1.0).add(0, 0.3, 0));
				}
			}
			c.sound(SoundEvents.POWDER_SNOW_STEP, at, 1.0F, 0.7F + 0.2F * beat);
			c.shake(at, 0.2F, 6);
		});
	}

	/**
	 * Hanging Pearl: bubbles of frost lift enemies and hold them in the air, drifting them together; then they pop
	 * as one. The look: frosted spheres rising and clumping, a soft chime per beat, and a splash on the pop.
	 */
	@Pair(a = "bubble", b = "levitate", name = "Hanging Pearl", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 4 enemies are lifted into bubbles and held in the air for 2.5 seconds, airborne, drifting together. "
			+ "Then the bubbles pop as one: 4 damage to each, and each is soaked. Enemies within 2 blocks of the middle take 2 more.")
	public static void hangingPearl(PairCast c) {
		List<LivingEntity> held = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : held) {
			c.mark(t, Reactions.Mark.AIRBORNE);
			c.lift(t, 0.8);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.8F, 1.3F);
		c.every(5, 11, frame -> {
			List<LivingEntity> alive = c.still(held);
			if (alive.isEmpty()) {
				return;
			}
			Vec3 middle = centroid(alive);
			for (LivingEntity t : alive) {
				c.sphere(PairCast.dust(0xBFEFFF, 0.8F), PairCast.mid(t), 0.9, 16);
				if (frame < 10) {
					c.lift(t, 0.25);
					c.pullTo(t, middle, 0.2);
				}
			}
			if (frame == 10) {
				pop(c, middle, alive, held);
			} else {
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, middle, 0.4F, 1.0F + 0.1F * frame);
			}
		});
	}

	private static void pop(PairCast c, Vec3 middle, List<LivingEntity> alive, List<LivingEntity> held) {
		for (LivingEntity t : alive) {
			c.hurt(t, 4 * c.power);
			c.mark(t, Reactions.Mark.SOAKED);
			c.particles(ParticleTypes.BUBBLE_POP, PairCast.mid(t), 10, 0.5, 0.1);
		}
		for (LivingEntity near : c.enemiesNear(middle, 2 * c.radius)) {
			if (!held.contains(near)) {
				c.hurt(near, 2 * c.power);
			}
		}
		c.sphere(PairCast.shift(0xBFEFFF, 0x6FD6E8, 1.2F), middle, 1.8, 30);
		c.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, middle, 1.0F, 0.8F);
	}

	/**
	 * Rime Spark: a cold snap freezes the nearest enemies, and a spark leaps between them. The look: a white flash
	 * racing out, then a zigzag of blue sparks jumping from body to body, half a second apart.
	 */
	@Pair(a = "coldsnap", b = "jolt", name = "Rime Spark", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A cold snap races out from the point: 3 cold damage and Slowness II for 3 seconds to up to 6 enemies within "
			+ "3 blocks. Then a spark jumps from the first of them to the nearest enemy within 3 blocks it has not yet hit, "
			+ "three times over, half a second apart: 3 lightning damage each jump.")
	public static void rimeSpark(PairCast c) {
		Vec3 spot = c.point();
		double reach = 3 * c.radius;
		List<LivingEntity> chilled = PairCast.first(c.enemiesNear(spot, reach), 6);
		for (LivingEntity t : chilled) {
			c.freeze(t, 3 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 3, 1);
			c.line(PairCast.dust(0xE6FBFF, 0.8F), spot, PairCast.mid(t), 3);
		}
		c.sound(SoundEvents.GLASS_PLACE, spot, 1.0F, 1.4F);
		if (chilled.isEmpty()) {
			return;
		}
		Set<LivingEntity> struck = new LinkedHashSet<>();
		struck.add(chilled.get(0));
		LivingEntity[] from = {chilled.get(0)};
		c.every(10, 4, hop -> {
			if (hop == 0) {
				return;
			}
			LivingEntity next = null;
			for (LivingEntity n : c.enemiesNear(PairCast.mid(from[0]), reach)) {
				if (!struck.contains(n) && c.here(n)) {
					next = n;
					break;
				}
			}
			if (next == null) {
				return;
			}
			struck.add(next);
			Vec3 a = PairCast.mid(from[0]);
			Vec3 b = PairCast.mid(next);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, a, b, 0.4, 3);
			c.particles(ParticleTypes.SNOWFLAKE, b, 8, 0.3, 0.05);
			c.shock(next, 3 * c.power);
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, b, 0.5F, 1.2F + 0.1F * hop);
			from[0] = next;
		});
	}

	/**
	 * Undertow Verse: a drowning word drags up to five enemies towards its spot and drowns them a little each second;
	 * when it ends the word breaks as a wave. The look: violet bubbles rising off each one, then a teal ring
	 * rolling outward.
	 */
	@Pair(a = "drowning_word", b = "silence", name = "Undertow Verse", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 5 enemies drown for 5 seconds: 2 magic damage a second, and each is drawn towards the point every second. "
			+ "When the word ends it breaks as a wave: 4 damage to every enemy within 3 blocks, which are soaked and knocked back.")
	public static void undertowVerse(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> drowned = PairCast.first(c.enemies(), 5);
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, spot, 0.8F, 0.7F);
		c.every(20, 5, second -> {
			for (LivingEntity t : c.still(drowned)) {
				c.hurt(t, 2 * c.power);
				c.pullTo(t, spot, 0.6);
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(t), 8, 0.35, 0.1);
				c.spiral(PairCast.shift(0x2A1B5C, 0x6FD6E8, 0.9F), PairCast.mid(t), 0.6, 1.2, 1.0, 12);
			}
			c.sound(SoundEvents.BUBBLE_POP, spot, 0.5F, 0.8F + 0.1F * second);
		});
		c.later(100, () -> {
			c.ring(PairCast.shift(0x6FD6E8, 0x2A1B5C, 1.2F), spot, 1.5, 28, 0.0);
			c.sound(SoundEvents.FIRE_EXTINGUISH, spot, 1.0F, 0.9F);
			c.shake(spot, 0.25F, 6);
			for (LivingEntity near : c.enemiesNear(spot, 3 * c.radius)) {
				c.hurt(near, 4 * c.power);
				c.mark(near, Reactions.Mark.SOAKED);
				c.knockFrom(near, spot, 0.9, 0.3);
			}
		});
	}

	/**
	 * Hoarfrost Wound: a cut that bleeds, and the blood clots into frost when it stops. The look: red drops at each
	 * tick, then a star of pale spikes springing out of every wound.
	 */
	@Pair(a = "bleed", b = "frostbite", name = "Hoarfrost Wound", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 4 enemies are cut: 2 damage, then 1 more every half second for 4 times, and each is slowed (Slowness I). "
			+ "Half a second after the bleeding stops the blood clots into frost: 4 cold damage to each, and 2 to every enemy "
			+ "within 2 blocks that was not cut.")
	public static void hoarfrostWound(PairCast c) {
		List<LivingEntity> cut = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : cut) {
			c.hurt(t, 2 * c.power);
			c.mark(t, Reactions.Mark.BLEEDING);
			c.effect(t, MobEffects.SLOWNESS, 4, 0);
			c.sound(SoundEvents.PLAYER_HURT, PairCast.mid(t), 0.6F, 1.1F);
		}
		c.every(10, 5, tick -> {
			for (LivingEntity t : c.still(cut)) {
				if (tick > 0) {
					c.hurt(t, 1 * c.power);
				}
				c.particles(PairCast.dust(0xA3121B, 1.0F), PairCast.mid(t), 5, 0.25, 0.05);
			}
		});
		c.later(50, () -> {
			Set<LivingEntity> splashed = new LinkedHashSet<>();
			for (LivingEntity t : c.still(cut)) {
				c.freeze(t, 4 * c.power);
				c.mark(t, Reactions.Mark.FROZEN);
				c.star(PairCast.shift(0xCFF4FF, 0xA3121B, 1.0F), PairCast.mid(t), 8, 1.4, 0.2);
				for (LivingEntity near : c.enemiesNear(PairCast.mid(t), 2 * c.radius)) {
					if (!cut.contains(near)) {
						splashed.add(near);
					}
				}
			}
			for (LivingEntity near : splashed) {
				c.freeze(near, 2 * c.power);
			}
			c.sound(SoundEvents.GLASS_BREAK, c.point(), 0.8F, 1.3F);
		});
	}

	/**
	 * Glacier Spire: an ice-and-stone spire grows under up to three enemies, lifting them, and cracks two and a half
	 * seconds later. The look: a pale column climbing beside each body, then grey shards flying off it.
	 */
	@Pair(a = "glacier", b = "monolith", name = "Glacier Spire", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A spire of ice and stone rises under up to 3 enemies, lifting them and slowing each (Slowness III) for 2.5 "
			+ "seconds. The spire cracks after 2.5 seconds: 6 damage to each, and 2 to every enemy within 2 blocks of them that "
			+ "was not speared.")
	public static void glacierSpire(PairCast c) {
		List<LivingEntity> speared = PairCast.first(c.enemies(), 3);
		for (LivingEntity t : speared) {
			c.lift(t, 0.6);
			c.effect(t, MobEffects.SLOWNESS, 2.5, 2);
		}
		c.sound(SoundEvents.STONE_HIT, c.point(), 1.0F, 0.6F);
		c.every(5, 9, frame -> {
			for (LivingEntity t : c.still(speared)) {
				c.column(PairCast.shift(0xCFF4FF, 0x8A7A66, 1.0F), t.position(), 0.6, 0.35 * frame + 0.5, 14);
			}
		});
		c.later(50, () -> crack(c, speared));
	}

	private static void crack(PairCast c, List<LivingEntity> speared) {
		for (LivingEntity t : c.still(speared)) {
			Vec3 at = PairCast.mid(t);
			c.strike(t, 6 * c.power);
			c.sphere(PairCast.dust(0x8A7A66, 1.1F), at, 1.2, 20);
			c.particles(ParticleTypes.CRIT, at, 10, 0.4, 0.2);
			for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
				if (near != t && !speared.contains(near)) {
					c.strike(near, 2 * c.power);
				}
			}
		}
		c.sound(SoundEvents.DEEPSLATE_BREAK, c.point(), 1.0F, 0.7F);
		c.shake(c.point(), 0.3F, 7);
	}

	/**
	 * Gravel Hail: five stones fall on up to three enemies in turn, each shoving its target back, and the target is
	 * slowed from the start. The look: stony streaks dropping from above, a dust ring on each landing.
	 */
	@Pair(a = "hail", b = "pelt", name = "Gravel Hail", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 3 enemies within 3 blocks are slowed (Slowness II) for 4 seconds. Five stones fall on them in turn, a "
			+ "quarter second apart: 2 damage each, shoving its target back. The fifth does 4 damage instead.")
	public static void gravelHail(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> pelted = PairCast.first(c.enemiesNear(spot, 3 * c.radius), 3);
		if (pelted.isEmpty()) {
			return;
		}
		for (LivingEntity t : pelted) {
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
		}
		c.every(5, 5, stone -> {
			LivingEntity t = pelted.get(stone % pelted.size());
			if (!c.here(t)) {
				return;
			}
			Vec3 at = PairCast.mid(t);
			c.line(PairCast.shift(0xC9C2B0, 0x9FD8FF, 0.9F), at.add(0, 5, 0), at, 3);
			c.ring(PairCast.dust(0xB7AE9B, 0.9F), t.position(), 0.8, 12, stone);
			c.strike(t, (stone == 4 ? 4 : 2) * c.power);
			c.knockFrom(t, spot, 0.5, 0.2);
			c.sound(SoundEvents.STONE_HIT, at, 0.7F, 0.9F + 0.1F * stone);
		});
	}

	/** The flat (horizontal) unit direction of {@code v}, or straight ahead when it has none. */
	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** The average middle of {@code list}'s bodies. */
	private static Vec3 centroid(List<LivingEntity> list) {
		Vec3 sum = Vec3.ZERO;
		for (LivingEntity t : list) {
			sum = sum.add(PairCast.mid(t));
		}
		return sum.scale(1.0 / list.size());
	}
}
