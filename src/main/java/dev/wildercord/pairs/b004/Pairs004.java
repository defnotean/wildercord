package dev.wildercord.pairs.b004;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The fourth batch of hand-made pairs: ten fusions, each with its own mechanic, palette and beats. */
public final class Pairs004 {
	private Pairs004() {}

	/**
	 * Storm Globe: a bubble lifts up to four enemies off the ground and slows them, then the bubble bursts in lightning.
	 * The look: pale blue shells swelling round each target while it hangs, then yellow sparks and a thunderclap.
	 */
	@Pair(a = "bubble", b = "jolt", name = "Storm Globe", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Lifts up to 4 enemies into a bubble and slows them (Slowness III) for 1.5 seconds. Then it bursts in lightning: "
			+ "6 damage to each one inside (and soaked), and 3 to every other enemy within 2.5 blocks.")
	public static void stormGlobe(PairCast c) {
		List<LivingEntity> sealed = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : sealed) {
			c.lift(t, 0.45);
			c.effect(t, MobEffects.SLOWNESS, 1.5, 2);
			c.sound(SoundEvents.GLASS_PLACE, PairCast.mid(t), 0.7F, 1.4F);
		}
		// The bubble swells round each one and hangs there, crackling, for about a second.
		c.every(4, 6, frame -> {
			for (LivingEntity t : c.still(sealed)) {
				Vec3 at = PairCast.mid(t);
				c.sphere(PairCast.dust(0xD6F7FF, 0.9F), at, 0.5 + frame * 0.14, 22);
				c.mote(ParticleTypes.ELECTRIC_SPARK, at, new Vec3(0, 0.04, 0));
			}
		});
		c.later(26, () -> burst(c, sealed));
	}

	private static void burst(PairCast c, List<LivingEntity> sealed) {
		List<LivingEntity> popped = c.still(sealed);
		Set<LivingEntity> struck = new HashSet<>(popped);
		for (LivingEntity t : popped) {
			Vec3 at = PairCast.mid(t);
			c.shock(t, 6 * c.power);
			c.mark(t, Reactions.Mark.SOAKED);
			c.bolt(at);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 3, 0), at, 0.5, 2);
			c.wave(ParticleTypes.ELECTRIC_SPARK, at, 16, 0.3);
			for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
				if (struck.add(near)) {
					c.shock(near, 3 * c.power);
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(near), 0.4, 2);
				}
			}
		}
		Vec3 centre = c.point();
		c.sphere(PairCast.shift(0xD6F7FF, 0xFFE34D, 1.2F), centre, 1.4, 26);
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, centre, 0.5F, 1.6F);
		c.shake(centre, 0.25F, 8);
	}

	/**
	 * Second Spring: a heal that keeps going. The look: rising spirals of green turning to frost-white, and a chime
	 * each time the Regeneration steps up.
	 */
	@Pair(a = "regrowth", b = "salve", name = "Second Spring", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Heals each ally 4 and puts out its fire, then Regeneration I for 3 seconds, II for 3, III for 2 (about 7 more). "
			+ "Healing past full health becomes absorption, up to 4 hearts for 10 seconds.")
	public static void secondSpring(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : allies) {
			double overflow = 4 * c.power - (t.getMaxHealth() - t.getHealth());
			c.heal(t, 4 * c.power);
			c.douse(t);
			if (overflow > 0) {
				c.absorb(t, Math.min(8, overflow), 10);
			}
			c.effect(t, MobEffects.REGENERATION, 3, 0);
			c.later(c.ticks(3), () -> {
				if (c.here(t)) {
					c.effect(t, MobEffects.REGENERATION, 3, 1);
					c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(t), 0.6F, 1.5F);
				}
			});
			c.later(c.ticks(6), () -> {
				if (c.here(t)) {
					c.effect(t, MobEffects.REGENERATION, 2, 2);
					c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(t), 0.6F, 1.9F);
				}
			});
		}
		c.sound(SoundEvents.BONE_MEAL_USE, c.point(), 0.6F, 1.2F);
		c.every(3, 6, frame -> {
			for (LivingEntity t : c.still(allies)) {
				Vec3 feet = t.position();
				c.spiral(PairCast.shift(0x7CE38B, 0xD8F7FF, 1.0F), feet.add(0, frame * 0.3, 0), 0.7, 2.0, 1.5, 12);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 2, 0.4, 0.02);
			}
		});
		c.later(c.ticks(6), () -> {
			for (LivingEntity t : c.still(allies)) {
				c.particles(ParticleTypes.SNOWFLAKE, PairCast.mid(t), 8, 0.4, 0.05);
			}
			c.tint(c.point(), 6, 0x9CFFB0, 10);
		});
	}

	/**
	 * Shelter of Mist: a dome of leaf shutters with mist in it. The look: a green shell rising round the point, a
	 * drift of white cloud down its walls, and enemies shoved out with a shield-clatter each second.
	 */
	@Pair(a = "haven", b = "mending_mist", name = "Shelter of Mist", element = "life", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "A shelter of leaf shutters and mist 4 blocks round the point for 8 seconds. Allies inside heal 1 each second. "
			+ "Enemies inside are shoved out once a second, and left chilled and wet.")
	public static void shelterOfMist(PairCast c) {
		Vec3 at = c.point();
		double r = 4 * c.radius;
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, at, 0.8F, 0.8F);
		c.sphere(PairCast.shift(0x5DBF5A, 0xE9FFF0, 1.0F), at, r * 0.6, 36);
		c.every(20, 8, frame -> {
			for (LivingEntity a : c.alliesNear(at, r)) {
				c.heal(a, 1 * c.power);
			}
			for (LivingEntity e : c.enemiesNear(at, r)) {
				if (PairCast.mid(e).distanceTo(at) <= r) {
					c.knockFrom(e, at, 0.9, 0.2);
					c.chill(e, 1);
					c.mark(e, Reactions.Mark.WET);
					c.sound(SoundEvents.SHIELD_BLOCK, PairCast.mid(e), 0.5F, 1.4F);
				}
			}
			c.ring(PairCast.dust(0x8FE39A, 1.0F), at.add(0, 0.3 + (frame % 2) * 0.4, 0), r, 28, frame * 0.5);
			c.column(ParticleTypes.CLOUD, at, r * 0.8, 2.5, 14);
		});
		c.later(c.ticks(8), () -> {
			c.wave(ParticleTypes.CLOUD, at, 20, 0.2);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, at, 12, 1.5, 0.02);
		});
	}

	/**
	 * Brittle Dawn: a barrier of light that can't be frozen. The look: pale gold shells round each ally, and when a
	 * barrier is used up its light breaks into a star of shards.
	 */
	@Pair(a = "barrier", b = "frostward", name = "Brittle Dawn", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Allies get 6 absorption (3 hearts) for 20 seconds, and any freeze on them is cleared every half second for 30 seconds. "
			+ "When an ally's barrier is used up, shards fly out: 2 freeze and a chill to each enemy within 2.5 blocks.")
	public static void brittleDawn(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		double[] shield = new double[allies.size()];
		for (int i = 0; i < allies.size(); i++) {
			LivingEntity t = allies.get(i);
			c.absorb(t, 6, 20);
			shield[i] = t.getAbsorptionAmount();
			t.setTicksFrozen(0);
			Reactions.clear(t, Reactions.Mark.FROZEN);
		}
		// The barrier is watched every other tick, up to just before it fades: the moment it empties, the shards fly.
		c.every(2, 195, frame -> {
			for (int i = 0; i < allies.size(); i++) {
				LivingEntity t = allies.get(i);
				if (!c.here(t)) {
					continue;
				}
				float now = t.getAbsorptionAmount();
				if (shield[i] > 0 && now <= 0) {
					shatter(c, t);
				}
				shield[i] = now;
			}
		});
		// Frostward: the allies can't freeze, so every tick of frost is taken back out of them.
		c.every(10, 60, frame -> {
			for (LivingEntity t : c.still(allies)) {
				t.setTicksFrozen(0);
				Reactions.clear(t, Reactions.Mark.FROZEN);
			}
		});
		for (LivingEntity t : allies) {
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(t), 0.8F, 1.2F);
		}
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(allies)) {
				c.sphere(PairCast.shift(0xFFF6D6, 0xBFEFFF, 1.0F), PairCast.mid(t), 1.5 - frame * 0.1, 20);
				c.ring(PairCast.dust(0xCFEFFF, 0.8F), t.position().add(0, 0.1, 0), 0.9, 12, frame * 0.5);
			}
		});
	}

	private static void shatter(PairCast c, LivingEntity from) {
		Vec3 at = PairCast.mid(from);
		c.star(PairCast.dust(0xFFF6D6, 0.9F), at, 8, 2.2, 0.3);
		c.sound(SoundEvents.GLASS_BREAK, at, 0.9F, 1.4F);
		c.shake(at, 0.15F, 4);
		for (LivingEntity e : c.enemiesNear(at, 2.5)) {
			c.freeze(e, 2 * c.power);
			c.chill(e, 2);
		}
	}

	/**
	 * Glass Hour: time stops for up to four enemies, and cold builds in them while they hang. The look: a violet-to-ice
	 * shell tightening round each target, then time moves and the glass breaks in one shower.
	 */
	@Pair(a = "absolute_zero", b = "stasis", name = "Glass Hour", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Stops up to 4 enemies for 3 seconds: Slowness III for 4 seconds, chilled and frost-marked. When time moves again, "
			+ "each one still held, slowed, chilled or marked takes 3 freeze damage per sign (held counts as one): up to 12.")
	public static void glassHour(PairCast c) {
		List<LivingEntity> stopped = PairCast.first(c.enemies(), 4);
		List<Vec3> spots = new ArrayList<>();
		for (LivingEntity t : stopped) {
			spots.add(t.position());
			c.effect(t, MobEffects.SLOWNESS, 4, 2);
			c.chill(t, 2);
			c.mark(t, Reactions.Mark.FROZEN);
			c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, PairCast.mid(t), 0.8F, 0.5F);
		}
		int hold = c.ticks(3);
		// Time holds them: each frame the shell tightens, and anything that drifts is set back on its spot.
		c.every(4, Math.max(1, hold / 4), frame -> {
			for (int i = 0; i < stopped.size(); i++) {
				LivingEntity t = stopped.get(i);
				if (!c.here(t)) {
					continue;
				}
				Vec3 spot = spots.get(i);
				if (t.position().distanceTo(spot) > 0.25) {
					c.blink(t, spot);
				}
				c.chill(t, 0.2);
				c.sphere(PairCast.shift(0xB9A7FF, 0x9FE8FF, 0.9F), PairCast.mid(t), 1.5 - frame * 0.1, 16);
			}
		});
		c.later(hold, () -> release(c, stopped));
	}

	private static void release(PairCast c, List<LivingEntity> stopped) {
		for (LivingEntity t : c.still(stopped)) {
			int signs = 1;
			if (t.hasEffect(MobEffects.SLOWNESS)) {
				signs++;
			}
			if (t.getTicksFrozen() > 0) {
				signs++;
			}
			if (Reactions.has(t, Reactions.Mark.FROZEN)) {
				signs++;
			}
			Vec3 at = PairCast.mid(t);
			c.freeze(t, 3 * c.power * signs);
			c.star(PairCast.dust(0xE4F4FF, 0.9F), at, 6, 1.4, 0.2);
			c.sound(SoundEvents.GLASS_BREAK, at, 0.9F, 0.8F + 0.1F * signs);
		}
		Vec3 at = c.point();
		c.wave(ParticleTypes.END_ROD, at, 24, 0.4);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.8F, 0.6F);
		c.shake(at, 0.3F, 6);
	}

	/**
	 * Rimed Judgement: a frost ring slows the target, then an icicle of light falls through the ring. The look: a pale
	 * ring tightening round the feet, a thin falling column of holy light, then a gold-white burst.
	 */
	@Pair(a = "icicle", b = "smite", name = "Rimed Judgement", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A frost ring closes at the feet of up to 4 targets and slows them (Slowness II for 2 seconds). 0.7 seconds later "
			+ "an icicle of light falls: 14 damage, strips Absorption, and leaves each one soaked.")
	public static void rimedJudgement(PairCast c) {
		List<LivingEntity> judged = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : judged) {
			c.effect(t, MobEffects.SLOWNESS, 2, 1);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(t), 0.7F, 1.8F);
		}
		// The frost ring closes round each pair of feet: wide and pale, then tight.
		c.every(3, 5, frame -> {
			for (LivingEntity t : c.still(judged)) {
				c.ring(PairCast.shift(0xCFF4FF, 0xFFE7A0, 0.9F), t.position().add(0, 0.1, 0), 1.8 - frame * 0.3, 18, frame * 0.4);
			}
		});
		// The icicle comes down out of the sky over the last eight ticks before it lands.
		c.later(14, () -> {
			for (LivingEntity t : c.still(judged)) {
				Vec3 feet = t.position();
				Vec3 top = feet.add(0, 6, 0);
				c.every(2, 4, frame -> {
					Vec3 tip = top.add(feet.subtract(top).scale((frame + 1) / 4.0));
					c.particles(ParticleTypes.END_ROD, tip, 2, 0.05, 0.0);
				});
			}
		});
		c.later(22, () -> {
			for (LivingEntity t : c.still(judged)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 14 * c.power);
				t.setAbsorptionAmount(0);
				c.mark(t, Reactions.Mark.SOAKED);
				c.particles(ParticleTypes.END_ROD, at, 16, 0.4, 0.15);
				c.star(PairCast.dust(0xFFF6D0, 1.0F), at, 6, 1.2, 0.0);
				c.sound(SoundEvents.BELL_BLOCK, at, 0.9F, 1.3F);
			}
			c.shake(c.point(), 0.25F, 6);
		});
	}

	/**
	 * Still Minute: a marked target is frozen and brittle, and a clock's two hands sweep over it until the moment
	 * catches up. The look: a pale ring at the head, the hands turning, then a sharp tick and a shatter of frost.
	 */
	@Pair(a = "countdown", b = "frost", name = "Still Minute", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "5 freeze damage, Slowness III for 4 seconds, and brittle for Shatter (4 seconds). 1.5 seconds later the moment catches up: "
			+ "6 damage, and a target still brittle shatters for 4 more, with 2 freeze to enemies within 2 blocks. "
			+ "If it died first, the moment strikes the nearest enemy within 6 blocks for 6.")
	public static void stillMinute(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : marked) {
			c.freeze(t, 5 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 4, 2);
			c.mark(t, Reactions.Mark.FROZEN);
		}
		// Two hands sweep round over each marked head, every half second, as the minute runs down.
		c.every(10, 3, frame -> {
			for (LivingEntity t : c.still(marked)) {
				Vec3 head = PairCast.mid(t).add(0, 0.8, 0);
				c.ring(PairCast.dust(0xB8D8FF, 0.7F), head, 0.8, 10, 0);
				c.star(PairCast.dust(0xFFE9A8, 0.6F), head, 2, 0.9, frame * 0.9);
			}
			c.sound(SoundEvents.NOTE_BLOCK_HAT, c.point(), 0.6F, 1.0F + frame * 0.2F);
		});
		c.later(c.ticks(1.5), () -> moment(c, marked));
	}

	private static void moment(PairCast c, List<LivingEntity> marked) {
		for (LivingEntity t : marked) {
			Vec3 at = PairCast.mid(t);
			if (c.here(t)) {
				c.hurt(t, 6 * c.power);
				if (Reactions.has(t, Reactions.Mark.FROZEN)) {
					c.hurt(t, 4 * c.power);
					Reactions.clear(t, Reactions.Mark.FROZEN);
					c.star(PairCast.dust(0xDDF6FF, 1.0F), at, 8, 2.0, 0.0);
					for (LivingEntity near : c.enemiesNear(at, 2.0)) {
						if (near != t) {
							c.freeze(near, 2 * c.power);
						}
					}
				}
				c.sphere(PairCast.shift(0xFFE9A8, 0xB8D8FF, 1.0F), at, 1.0, 20);
			} else {
				LivingEntity heir = c.nearestEnemy(at, 6, t);
				if (heir != null) {
					c.line(PairCast.dust(0xFFE9A8, 0.7F), at, PairCast.mid(heir), 3);
					c.hurt(heir, 6 * c.power);
				}
			}
		}
		c.sound(SoundEvents.LODESTONE_COMPASS_LOCK, c.point(), 0.8F, 1.2F);
	}

	/**
	 * Rimestep: a blink that leaves the arrival chilled. The look: a spiral of frost and violet where you stood, then a
	 * ring of rime spreading out from where you land.
	 */
	@Pair(a = "blink", b = "chill", name = "Rimestep", element = "frost", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "Teleports you to where the spell landed (max 40 blocks). Enemies within 3 blocks of where you arrive are chilled: "
			+ "Slowness II for 4 seconds and 2 freeze damage each.")
	public static void rimestep(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 left = self.position().add(0, 0.2, 0);
		if (c.point().distanceTo(self.position()) > 40) {
			return;
		}
		if (!c.blink(self, c.point())) {
			return;
		}
		Vec3 landed = self.position().add(0, 0.2, 0);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, left, 0.6F, 1.6F);
		c.spiral(PairCast.shift(0x9FF4FF, 0x6A4CFF, 0.8F), left, 0.6, 2.2, 1.5, 22);
		for (LivingEntity e : c.enemiesNear(landed, 3)) {
			c.effect(e, MobEffects.SLOWNESS, 4, 1);
			c.chill(e, 2);
			c.freeze(e, 2 * c.power);
			c.line(PairCast.dust(0xCFF8FF, 0.8F), landed, PairCast.mid(e), 2);
		}
		c.every(3, 3, frame -> {
			c.ring(PairCast.dust(0xCFF8FF, 0.8F), landed, 1.0 + frame * 1.2, 20, frame * 0.3);
		});
		c.punch(0.2F);
	}

	/**
	 * Glacis: a frost wall at the point that shoves back whatever comes against it. The look: a snap of frost ringing
	 * out, then three rows of pale ice drawn across the wall for eight seconds.
	 */
	@Pair(a = "coldsnap", b = "rampart", name = "Glacis", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Raises a frost wall 5 blocks wide and 3 high at the point for 8 seconds. Every enemy within 3 blocks takes 4 freeze damage, "
			+ "Slowness II for 4 seconds and is brittle for Shatter. Enemies that come against the wall are shoved back and chilled.")
	public static void glacis(PairCast c) {
		final Vec3 at = c.point();
		final Vec3 flat = flatDir(c.dir());
		final Vec3 across = new Vec3(-flat.z, 0, flat.x);
		final double half = 2.5 * c.radius;
		for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
			c.freeze(e, 4 * c.power);
			c.effect(e, MobEffects.SLOWNESS, 4, 1);
			c.mark(e, Reactions.Mark.FROZEN);
		}
		c.sound(SoundEvents.STONE_HIT, at, 0.9F, 0.7F);
		c.sound(SoundEvents.GLASS_PLACE, at, 1.0F, 0.6F);
		c.wave(ParticleTypes.SNOWFLAKE, at.add(0, 0.2, 0), 24, 0.35);
		c.every(4, Math.max(1, c.ticks(8) / 4), frame -> {
			Vec3 west = at.add(across.scale(-half));
			Vec3 east = at.add(across.scale(half));
			c.line(PairCast.dust(0xDDF6FF, 0.9F), west.add(0, 0.1, 0), east.add(0, 0.1, 0), 1.0);
			c.line(PairCast.dust(0xBFE8FF, 0.9F), west.add(0, 1.5, 0), east.add(0, 1.5, 0), 1.0);
			c.line(PairCast.dust(0xDDF6FF, 0.9F), west.add(0, 2.9, 0), east.add(0, 2.9, 0), 1.0);
			for (LivingEntity e : c.enemiesNear(at, half + 1.5)) {
				Vec3 rel = PairCast.mid(e).subtract(at);
				double depth = rel.dot(flat);
				double side = rel.dot(across);
				if (Math.abs(depth) < 1.0 && Math.abs(side) <= half) {
					c.push(e, flat.scale(depth >= 0 ? 0.5 : -0.5));
					c.chill(e, 1);
				}
			}
		});
	}

	private static Vec3 flatDir(Vec3 d) {
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	/**
	 * Stormshove: a gust hurls enemies up and out, and the storm finds them in the air. The look: a white wind-ring
	 * riding each target as it flies, then a jagged bolt from the sky and a crackle arcing to its neighbour.
	 */
	@Pair(a = "push", b = "shock", name = "Stormshove", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Hurls up to 8 enemies 1.2 blocks away and 0.6 up. 0.8 seconds later a bolt from the sky strikes each one still alive: "
			+ "6 lightning damage, arcing to the nearest other enemy within 3 blocks for 3.")
	public static void stormshove(PairCast c) {
		List<LivingEntity> shoved = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 from = c.point();
		for (LivingEntity t : shoved) {
			c.knockFrom(t, from, 1.2, 0.6);
		}
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, from, 0.8F, 1.1F);
		c.every(3, 5, frame -> {
			for (LivingEntity t : c.still(shoved)) {
				c.ring(PairCast.dust(0xEAF6FF, 0.8F), PairCast.mid(t), 0.7, 8, frame * 0.7);
			}
		});
		c.later(16, () -> {
			for (LivingEntity t : c.still(shoved)) {
				Vec3 at = PairCast.mid(t);
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 7, 0), at, 0.6, 2);
				c.bolt(at);
				c.shock(t, 6 * c.power);
				LivingEntity arc = c.nearestEnemy(at, 3 * c.radius, t);
				if (arc != null) {
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(arc), 0.5, 2);
					c.shock(arc, 3 * c.power);
				}
			}
			c.shake(c.point(), 0.3F, 8);
		});
	}
}
