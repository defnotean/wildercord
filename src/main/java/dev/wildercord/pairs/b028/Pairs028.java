package dev.wildercord.pairs.b028;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs028 {
	private Pairs028() {}

	/**
	 * Gilded Hand: luck on the lucky. Up to five of you and your allies glint gold; then four rolls, a second apart,
	 * each a one-in-three chance of a healing flash. The look: gold rings that pop up at random on each ally.
	 */
	@Pair(a = "fortune", b = "luckcharm", name = "Gilded Hand", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Up to 5 of you and allies within 8 blocks get Luck I for 20 seconds. Then each rolls four times, a second apart: "
			+ "one in three for 2 healing and a gold burst, otherwise nothing.")
	public static void gildedHand(PairCast c) {
		List<LivingEntity> lucky = new ArrayList<>();
		lucky.add(c.caster);
		for (LivingEntity a : c.alliesNear(PairCast.mid(c.caster), 8)) {
			if (lucky.size() < 5 && !lucky.contains(a)) {
				lucky.add(a);
			}
		}
		for (LivingEntity a : lucky) {
			c.effect(a, MobEffects.LUCK, 20, 0);
			c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 10, 0.4, 0.05);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(c.caster), 0.7F, 1.6F);
		c.every(20, 4, roll -> {
			for (LivingEntity a : c.still(lucky)) {
				Vec3 at = PairCast.mid(a);
				if (c.random() < 1.0 / 3) {
					c.heal(a, 2 * c.power);
					c.ring(PairCast.shift(0xFFE066, 0xFFF8D6, 0.9F), at, 0.9, 14, roll * 0.5);
					c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, at, 0.6F, 1.2F + 0.1F * roll);
				}
			}
		});
	}

	/**
	 * Hushwave: a wave of silence that rolls out from the spot. Allies it passes are healed and shielded; monsters
	 * it passes lose their target. The look: pale violet rings widening over the ground, a chime on each ring.
	 */
	@Pair(a = "hush", b = "lullaby", name = "Hushwave", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A wave of hush rolls out from the spot to 10 blocks in 1.5 seconds. Each ally it reaches (you included) is healed 3 "
			+ "and gets 2 absorption for 6 seconds; each monster it reaches loses its target.")
	public static void hushwave(PairCast c) {
		Vec3 spot = c.point();
		Set<LivingEntity> sung = new HashSet<>();
		Set<LivingEntity> hushed = new HashSet<>();
		c.sound(SoundEvents.BEACON_ACTIVATE, spot, 0.5F, 1.8F);
		c.every(10, 4, frame -> {
			double reach = 2.5 * (frame + 1) * c.radius;
			c.ring(PairCast.dust(0xE3D9FF, 1.0F), spot, reach, 28, frame * 0.4);
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				if (sung.add(a)) {
					c.heal(a, 3 * c.power);
					c.absorb(a, 2 * c.power, 6);
					c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 8, 0.3, 0.05);
				}
			}
			for (LivingEntity e : c.enemiesNear(spot, reach)) {
				if (hushed.add(e) && e instanceof Mob m && c.movable(m)) {
					m.setTarget(null);
				}
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 0.5F, 0.9F + 0.2F * frame);
		});
	}

	/**
	 * Sidestep Gale: you slip sideways along your facing, leaving a gust where you were. The gust shoves whatever
	 * stood there and you run on for a moment. The look: a pale wind line drawn across the ground, then rings of gust.
	 */
	@Pair(a = "evade", b = "second_wind", name = "Sidestep Gale", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration"},
		text = "You slip up to 5 blocks along the way you face, stopped by walls. A moment later a gust shoves enemies within 3 blocks "
			+ "of where you started away and strikes each for 2 damage, and you get Speed II for 4 seconds.")
	public static void sidestepGale(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 flat = flatDir(c);
		double run = 5;
		BlockHitResult wall = c.level.clip(new ClipContext(start.add(0, 0.9, 0), start.add(flat.scale(5)).add(0, 0.9, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			run = Math.max(0, wall.getLocation().subtract(start.add(0, 0.9, 0)).dot(flat) - 0.6);
		}
		Vec3 end = start.add(flat.scale(run));
		c.line(PairCast.dust(0xE6FFFF, 0.9F), start.add(0, 1, 0), end.add(0, 1, 0), 2);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, start, 0.8F, 1.4F);
		c.blink(self, end);
		c.punch(0.2F);
		c.later(4, () -> {
			for (LivingEntity t : c.enemiesNear(start, 3 * c.radius)) {
				c.knockFrom(t, start, 1.1, 0.3);
				c.strike(t, 2 * c.power);
			}
			c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, start, 1.0F, 0.9F);
		});
		c.effect(self, MobEffects.SPEED, 4, 1);
		c.every(3, 5, frame -> c.ring(ParticleTypes.GUST, start.add(0, 1, 0), 0.5 + frame * 0.7, 16, frame * 0.3));
	}

	/**
	 * Riftfall: the enemies are sent back up to six blocks and dazed; a second later a portal drops each where it
	 * stands. The look: violet rings wound round each target while it is sent, then a column of light on landing.
	 */
	@Pair(a = "banish", b = "portalfall", name = "Riftfall", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Banishes up to 4 enemies: each is dazed (Slowness II for 2 seconds) and reappears up to 6 blocks further from you, "
			+ "where there's room. A second later a portal drops each where it stands: 4 damage, and every other enemy within 2 blocks takes 2.")
	public static void riftfall(PairCast c) {
		List<LivingEntity> banished = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : banished) {
			Vec3 from = PairCast.mid(t);
			c.effect(t, MobEffects.SLOWNESS, 2, 1);
			c.particles(ParticleTypes.REVERSE_PORTAL, from, 20, 0.4, 0.2);
			c.blink(t, t.position().add(awayFrom(c, t).scale(6)));
			c.particles(ParticleTypes.PORTAL, PairCast.mid(t), 20, 0.4, 0.2);
		}
		c.sound(SoundEvents.ENDERMAN_TELEPORT, c.point(), 0.6F, 0.8F);
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(banished)) {
				c.ring(PairCast.dust(0x9B5CFF, 0.9F), PairCast.mid(t), 0.9, 12, frame * 0.6);
			}
		});
		c.later(20, () -> {
			for (LivingEntity t : c.still(banished)) {
				Vec3 at = PairCast.mid(t);
				c.column(PairCast.shift(0xC9A8FF, 0x2A0B4F, 0.9F), t.position(), 0.8, 4, 30);
				c.hurt(t, 4 * c.power);
				for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
					if (near != t) {
						c.hurt(near, 2 * c.power);
					}
				}
				c.sound(SoundEvents.PORTAL_TRAVEL, at, 0.4F, 1.4F);
			}
		});
	}

	/**
	 * Lightless Hour: a dark disc eclipses the point. Those beneath it are blinded and bleed slowly, and the blinded
	 * lash at whatever stands next to them. The look: a violet-black disc on the ground, sparks lashing between bodies.
	 */
	@Pair(a = "blind", b = "eclipse", name = "Lightless Hour", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A dark disc 3.5 blocks wide eclipses the point for 5 seconds. Up to 6 enemies under it are blinded for 2 seconds "
			+ "(renewed each second) and take 1 damage a second; each second a blinded one lashes at the nearest other enemy within 2 blocks for 1.")
	public static void lightlessHour(PairCast c) {
		Vec3 spot = c.point();
		double r = 3.5 * c.radius;
		List<LivingEntity> under = new ArrayList<>();
		for (LivingEntity t : c.enemies()) {
			if (under.size() < 6 && !under.contains(t)) {
				under.add(t);
			}
		}
		for (LivingEntity t : c.enemiesNear(spot, r)) {
			if (under.size() < 6 && !under.contains(t)) {
				under.add(t);
			}
		}
		c.sound(SoundEvents.BEACON_AMBIENT, spot, 0.8F, 0.6F);
		c.every(20, 5, frame -> {
			c.disc(PairCast.dust(0x1B1033, 1.3F), spot, r, 30);
			for (LivingEntity t : c.still(under)) {
				c.effect(t, MobEffects.BLINDNESS, 2, 0);
				c.wither(t, 1 * c.power);
				LivingEntity lash = c.nearestEnemy(PairCast.mid(t), 2 * c.radius, t);
				if (lash != null) {
					c.strike(lash, 1 * c.power);
					c.line(PairCast.dust(0x5B2A86, 0.8F), PairCast.mid(t), PairCast.mid(lash), 3);
				}
			}
		});
	}

	/**
	 * Rimebreath: a breath of cold rolls out along the way you blew it, chilling each enemy it passes. Where it
	 * ends, it bursts into frost. The look: a white cloud rolling forward in puffs, then a star of ice at its end.
	 */
	@Pair(a = "blizzard", b = "dragon_breath", name = "Rimebreath", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A cold breath rolls 6 blocks the way you blew it, 3 blocks wide: each enemy it passes takes 2 cold and gets Slowness II "
			+ "for 2 seconds. Where it ends it bursts for 4 more cold on enemies within 3 blocks, which are marked frozen.")
	public static void rimebreath(PairCast c) {
		Vec3 start = c.origin();
		Vec3 flat = flatDir(c);
		Set<LivingEntity> passed = new HashSet<>();
		for (LivingEntity t : PairCast.first(c.enemies(), 6)) {
			passed.add(t);
			c.freeze(t, 2 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 2, 1);
		}
		c.sound(SoundEvents.SNOWBALL_THROW, start, 0.9F, 0.6F);
		c.every(7, 7, frame -> {
			Vec3 at = start.add(flat.scale(frame));
			c.sphere(PairCast.shift(0xEFFBFF, 0x7FD8FF, 1.0F), at, 1.5 * c.radius, 18);
			for (LivingEntity t : c.enemiesNear(at, 1.5 * c.radius)) {
				if (passed.add(t)) {
					c.freeze(t, 2 * c.power);
					c.effect(t, MobEffects.SLOWNESS, 2, 1);
				}
			}
			if (frame == 6) {
				c.sound(SoundEvents.GLASS_BREAK, at, 0.9F, 1.3F);
				c.star(PairCast.dust(0xFFFFFF, 0.9F), at, 8, 3 * c.radius, 0);
				for (LivingEntity t : c.enemiesNear(at, 3 * c.radius)) {
					c.freeze(t, 4 * c.power);
					c.mark(t, Reactions.Mark.FROZEN);
				}
			}
		});
	}

	/**
	 * Ill Omen: a curse on up to four enemies that wears them down, and passes on when one of them dies. The look:
	 * a dark soul thread on each cursed body, and a thread leaping to the heir when a cursed body falls.
	 */
	@Pair(a = "hex", b = "malison", name = "Ill Omen", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Curses up to 4 enemies: 3 wither damage and marked shadowed at once, then 1 wither a second for 5 seconds. A cursed "
			+ "enemy that dies passes the curse to the nearest enemy within 5 blocks: 2 wither, and it's cursed too, up to 8 cursed.")
	public static void illOmen(PairCast c) {
		Set<LivingEntity> cursed = new LinkedHashSet<>(PairCast.first(c.enemies(), 4));
		for (LivingEntity t : cursed) {
			c.wither(t, 3 * c.power);
			c.mark(t, Reactions.Mark.SHADOWED);
			c.particles(ParticleTypes.SOUL, PairCast.mid(t), 10, 0.3, 0.05);
		}
		Set<LivingEntity> spent = new HashSet<>();
		c.every(20, 5, frame -> {
			for (LivingEntity t : new ArrayList<>(cursed)) {
				if (spent.contains(t)) {
					continue;
				}
				Vec3 at = PairCast.mid(t);
				if (!t.isAlive()) {
					spent.add(t);
					LivingEntity heir = c.nearestEnemy(at, 5 * c.radius, t);
					if (heir != null && cursed.size() < 8 && cursed.add(heir)) {
						c.wither(heir, 2 * c.power);
						c.mark(heir, Reactions.Mark.SHADOWED);
						c.line(PairCast.shift(0x8A3FD6, 0x1A0A24, 0.9F), at, PairCast.mid(heir), 3);
						c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(heir), 0.5F, 1.6F);
					}
					continue;
				}
				c.wither(t, 1 * c.power);
				c.ring(PairCast.dust(0x3B1C52, 0.9F), at, 0.7, 10, frame * 0.5);
			}
		});
	}

	/**
	 * Gaolchain: enemies are hobbled and chained to where they stand. Any that strays is yanked back and bitten.
	 * The look: a grey chain drawn from each anchor to its prisoner, and a clank on every yank.
	 */
	@Pair(a = "hobble", b = "shackle", name = "Gaolchain", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Hobbles up to 4 enemies (Slowness III for 4 seconds) and chains each to where it stands for 5 seconds: any that "
			+ "strays more than 2 blocks is yanked back, and each yank bites for 2 damage.")
	public static void gaolchain(PairCast c) {
		List<LivingEntity> chained = PairCast.first(c.enemies(), 4);
		Map<LivingEntity, Vec3> anchor = new HashMap<>();
		for (LivingEntity t : chained) {
			anchor.put(t, t.position());
			c.effect(t, MobEffects.SLOWNESS, 4, 2);
			c.sound(SoundEvents.CHAIN_PLACE, PairCast.mid(t), 0.9F, 0.9F);
		}
		c.every(10, 10, frame -> {
			for (LivingEntity t : c.still(chained)) {
				Vec3 pin = anchor.get(t);
				c.line(PairCast.dust(0xB7BDC6, 0.7F), pin.add(0, 1, 0), PairCast.mid(t), 3);
				if (t.position().distanceTo(pin) > 2) {
					c.pullTo(t, pin, 0.9);
					c.strike(t, 2 * c.power);
					c.sound(SoundEvents.CHAIN_PLACE, PairCast.mid(t), 0.7F, 0.7F);
				}
			}
		});
	}

	/**
	 * Lodestone: enemies are pulled in and drop what they were chasing. For four seconds they are drawn back to the
	 * point whenever they stray, and then a stone burst goes off. The look: violet threads pulling inwards, rings
	 * pulsing at the point, then a grey burst of shards.
	 */
	@Pair(a = "lure", b = "pull", name = "Ironpull Stone", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Pulls up to 6 enemies within 5 blocks towards the point, marked pulled and dropping their targets. Each half second for "
			+ "4 seconds, any that has strayed past 1.5 blocks is drawn back; then a stone burst deals 3 damage to each enemy within 2 blocks.")
	public static void lodestone(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> drawn = new ArrayList<>();
		for (LivingEntity t : c.enemies()) {
			if (drawn.size() < 6 && !drawn.contains(t)) {
				drawn.add(t);
			}
		}
		for (LivingEntity t : c.enemiesNear(spot, 5 * c.radius)) {
			if (drawn.size() < 6 && !drawn.contains(t)) {
				drawn.add(t);
			}
		}
		for (LivingEntity t : drawn) {
			c.pullTo(t, spot, 1.0);
			c.mark(t, Reactions.Mark.PULLED);
			c.line(PairCast.dust(0x5B2A86, 0.8F), spot, PairCast.mid(t), 3);
			if (t instanceof Mob m && c.movable(m)) {
				m.setTarget(null);
			}
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, spot, 0.9F, 0.6F);
		c.every(10, 9, frame -> {
			if (frame < 8) {
				for (LivingEntity t : c.still(drawn)) {
					if (PairCast.mid(t).distanceTo(spot) > 1.5) {
						c.pullTo(t, spot, 0.5);
					}
					if (t instanceof Mob m && c.movable(m)) {
						m.setTarget(null);
					}
				}
				c.ring(PairCast.dust(0x7B3FC0, 1.0F), spot, 0.5 + frame * 0.3, 18, frame * 0.4);
			} else {
				c.sound(SoundEvents.BEACON_DEACTIVATE, spot, 0.9F, 0.8F);
				c.wave(PairCast.dust(0x9A9AA6, 1.2F), spot, 22, 0.3);
				for (LivingEntity e : c.enemiesNear(spot, 2 * c.radius)) {
					c.hurt(e, 3 * c.power);
				}
			}
		});
	}

	/**
	 * Dread Shade: your afterimage stands at the point and frightens. Enemies near it are shoved away from it each
	 * half second, and when it fades it bursts. The look: a pale column of afterimage, shadows fleeing from it.
	 */
	@Pair(a = "phantom", b = "spook", name = "Dread Shade", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Your afterimage stands at the point for 3 seconds: the first 6 enemies struck are knocked back at once, and every "
			+ "enemy within 5 blocks of it is shoved away from it each half second. When it fades it bursts for 5 damage on each enemy within 3 blocks.")
	public static void dreadShade(PairCast c) {
		Vec3 spot = c.point();
		for (LivingEntity t : PairCast.first(c.enemies(), 6)) {
			c.knockFrom(t, spot, 0.9, 0.3);
		}
		c.every(10, 7, frame -> {
			if (frame < 6) {
				c.column(PairCast.shift(0xC7D4FF, 0x3A2A66, 1.0F), spot, 0.8, 2.2, 26);
				for (LivingEntity t : c.enemiesNear(spot, 5 * c.radius)) {
					c.knockFrom(t, spot, 0.35, 0.0);
				}
				c.sound(SoundEvents.BEACON_AMBIENT, spot, 0.4F, 1.5F);
			} else {
				c.sphere(PairCast.dust(0xC7D4FF, 1.0F), spot, 3 * c.radius, 40);
				c.sound(SoundEvents.GLASS_BREAK, spot, 0.8F, 0.6F);
				for (LivingEntity t : c.enemiesNear(spot, 3 * c.radius)) {
					c.hurt(t, 5 * c.power);
				}
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The way the spell was going, flattened to the ground (a unit vector; east when it's straight up or down). */
	private static Vec3 flatDir(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	/** The ground direction from the caster out through {@code t}, or the spell's own way if they stand on the caster. */
	private static Vec3 awayFrom(PairCast c, LivingEntity t) {
		Vec3 out = new Vec3(t.getX() - c.caster.getX(), 0, t.getZ() - c.caster.getZ());
		return out.lengthSqr() < 0.04 ? flatDir(c) : out.normalize();
	}
}
