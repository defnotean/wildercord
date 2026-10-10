package dev.wildercord.pairs.b034;

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
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs034 {
	private Pairs034() {}

	/**
	 * Sea Lullaby: a song that calms. Up to six enemies go quiet: they forget who they were fighting, and the calm ripples
	 * out to the enemies near them. The look: green spirals round each one, chimes, and a ring of mist spreading each
	 * beat.
	 */
	@Pair(a = "kelpsong", b = "soothe", name = "Sea Lullaby", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Up to 6 enemies hit are lulled: they forget their target (not bosses) and get Slowness II for 4 seconds. "
			+ "Four ripples, a second apart: each lulled one takes 1 magic damage (times power) and calms the enemies within "
			+ "3 blocks of it, up to 8 in all.")
	public static void seaLullaby(PairCast c) {
		List<LivingEntity> lulled = new ArrayList<>(PairCast.first(c.enemies(), 6));
		for (LivingEntity t : lulled) {
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
			calm(c, t);
			c.spiral(PairCast.shift(0x2E8B57, 0x9AD9C8, 1.0F), PairCast.mid(t), 0.7, 1.6, 2, 18);
		}
		c.sound(SoundEvents.NOTE_BLOCK_CHIME, c.point(), 0.8F, 0.7F);
		c.every(20, 4, frame -> {
			for (LivingEntity t : c.still(lulled)) {
				Vec3 at = PairCast.mid(t);
				calm(c, t);
				c.hurt(t, c.power);
				c.ring(PairCast.dust(0x9AD9C8, 0.9F), at, 0.6 + frame * 0.8, 16, frame * 0.3);
				c.particles(ParticleTypes.NOTE, at, 3, 0.4, 0.0);
				if (frame > 0) {
					for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
						if (lulled.size() < PairCast.MAX_TARGETS && !lulled.contains(near)) {
							lulled.add(near);
							c.effect(near, MobEffects.SLOWNESS, 4, 1);
							calm(c, near);
							c.line(PairCast.dust(0x9AD9C8, 0.7F), at, PairCast.mid(near), 3);
							c.sound(SoundEvents.NOTE_BLOCK_CHIME, PairCast.mid(near), 0.6F, 1.0F + frame * 0.2F);
						}
					}
				}
			}
		});
	}

	/**
	 * Basalt Rain: meteors on the first two enemies, and the crater left behind hardens into a burning, slowing basin.
	 * The look: rings of orange drawn on the ground while the meteors are coming, a fiery streak down onto each target,
	 * then a dim disc that stays for four seconds.
	 */
	@Pair(a = "lava_crust", b = "meteor", name = "Basalt Rain", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Meteors fall on the first two enemies hit 1.2 seconds later: 8 fire damage (times power) to each, and 4 to "
			+ "enemies within 3 blocks of it. Each crater burns for 4 seconds: every second, enemies within 2.5 blocks take "
			+ "1 fire damage (times power) and get Slowness I.")
	public static void basaltRain(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), 2);
		c.every(6, 4, frame -> {
			for (LivingEntity t : c.still(marked)) {
				Vec3 ground = c.ground(t.position());
				c.ring(PairCast.shift(0xFFB347, 0x5A2A10, 1.0F), ground.add(0, 0.1, 0), 2.8 - frame * 0.6, 20, frame * 0.4);
			}
		});
		c.later(24, () -> {
			List<Vec3> craters = new ArrayList<>();
			for (LivingEntity t : c.still(marked)) {
				Vec3 at = PairCast.mid(t);
				craters.add(at);
				c.line(PairCast.shift(0xFFD27A, 0x3A1A10, 1.4F), at.add(0, 12, 0), at, 1);
				c.burn(t, 8 * c.power);
				for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
					if (near != t) {
						c.burn(near, 4 * c.power);
					}
				}
				c.sound(SoundEvents.FIRECHARGE_USE, at, 1.0F, 0.6F);
				c.sound(SoundEvents.ANVIL_LAND, at, 0.5F, 1.6F);
				c.shake(at, 0.3F, 6);
			}
			c.every(20, 5, frame -> {
				for (Vec3 crater : craters) {
					c.disc(PairCast.shift(0xFF7A1F, 0x2A1208, 1.1F), crater, 2.5, 24);
					for (LivingEntity near : c.enemiesNear(crater, 2.5 * c.radius)) {
						c.burn(near, c.power);
						c.effect(near, MobEffects.SLOWNESS, 1.5, 0);
					}
				}
			});
		});
	}

	/**
	 * Tidal Vow: a sworn favour for the allies. They are doused, given the sea's breath and grace and two hearts of
	 * Absorption, and a surge of tide shoves them forward. The look: blue rings spreading from each ally, bubbles, and a
	 * splash at the surge.
	 */
	@Pair(a = "oceans_favor", b = "tidebreath", name = "Tidal Vow", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Up to 8 allies reached are doused and get Water Breathing, Dolphin's Grace and Night Vision for 60 seconds, "
			+ "and 2 hearts of Absorption for 15 seconds. A second later a tide surge shoves each one forward a few blocks.")
	public static void tidalVow(PairCast c) {
		List<LivingEntity> sworn = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		Vec3 forward = flat(c.dir());
		c.sound(SoundEvents.CONDUIT_ACTIVATE, c.point(), 0.8F, 1.1F);
		for (LivingEntity a : sworn) {
			c.douse(a);
			c.effect(a, MobEffects.WATER_BREATHING, 60, 0);
			c.effect(a, MobEffects.DOLPHINS_GRACE, 60, 0);
			c.effect(a, MobEffects.NIGHT_VISION, 60, 0);
			c.absorb(a, 4, 15);
		}
		c.every(4, 6, frame -> {
			for (LivingEntity a : c.still(sworn)) {
				Vec3 feet = a.position().add(0, 0.1, 0);
				c.ring(PairCast.shift(0x6FE0FF, 0x1F6FB2, 1.0F), feet, 0.5 + frame * 0.35, 18, frame * 0.25);
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(a), 4, 0.4, 0.05);
				if (frame == 5) {
					c.push(a, forward.scale(0.9).add(0, 0.2, 0));
					c.particles(ParticleTypes.SPLASH, feet, 10, 0.5, 0.1);
					c.sound(SoundEvents.GENERIC_SPLASH, feet, 0.7F, 1.0F);
				}
			}
		});
	}

	/**
	 * Angler's Hook: a line cast from you to up to three enemies. They are caught and reeled in four times, and the last
	 * reel hurts. The look: a zigzag line to each hooked enemy, a taut line on every reel, and a splash at the catch.
	 */
	@Pair(a = "pull", b = "reeling_tide", name = "Angler's Hook", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Hooks up to 3 enemies with a line from where you cast: each is staggered (Slowness I for 2 seconds) and marked "
			+ "pulled. Four reels half a second apart draw each toward you; the last one lands 4 magic damage (times power).")
	public static void anglersHook(PairCast c) {
		Vec3 from = c.origin();
		List<LivingEntity> hooked = PairCast.first(c.enemies(), 3);
		c.sound(SoundEvents.FISHING_BOBBER_SPLASH, from, 0.8F, 1.0F);
		for (LivingEntity t : hooked) {
			c.effect(t, MobEffects.SLOWNESS, 2, 0);
			c.mark(t, Reactions.Mark.PULLED);
			c.zigzag(PairCast.dust(0x7FD8FF, 0.6F), from.add(0, 1, 0), PairCast.mid(t), 0.3, 2);
		}
		c.every(10, 4, frame -> {
			for (LivingEntity t : c.still(hooked)) {
				Vec3 at = PairCast.mid(t);
				c.pullTo(t, from, 0.9);
				c.line(PairCast.dust(0x7FD8FF, 0.6F), from.add(0, 1, 0), at, 2);
				c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, at, 0.6F, 0.9F + frame * 0.15F);
				if (frame == 3) {
					c.hurt(t, 4 * c.power);
					c.particles(ParticleTypes.SPLASH, at, 12, 0.4, 0.1);
					c.sound(SoundEvents.GENERIC_SPLASH, at, 0.8F, 0.8F);
				}
			}
		});
	}

	/**
	 * Smouldering Shroud: flame that is snuffed into smoke. Enemies catch fire, and a second later the fire is put out
	 * as a cloud that blinds and burns whatever is inside it. The look: flame spiralling up each target, then grey
	 * smoke rolling out for two seconds.
	 */
	@Pair(a = "ember", b = "snuffout", name = "Smouldering Shroud", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Up to 4 enemies hit take 3 fire damage (times power) and burn for 3 seconds. After one second the flames are "
			+ "smothered into a cloud: they go out, and enemies within 2.5 blocks of each one are blinded for 2 seconds and "
			+ "take 1 magic damage (times power) every half second.")
	public static void smoulderingShroud(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.FIRECHARGE_USE, c.point(), 0.8F, 0.9F);
		for (LivingEntity t : hit) {
			c.burn(t, 3 * c.power);
			c.ignite(t, 3);
			c.particles(ParticleTypes.FLAME, PairCast.mid(t), 10, 0.3, 0.05);
		}
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(hit)) {
				c.spiral(PairCast.shift(0xFF7A1F, 0x6B6B6B, 0.9F), PairCast.mid(t), 0.5, 1.4, 1, 10);
			}
		});
		c.later(20, () -> {
			List<Vec3> clouds = new ArrayList<>();
			for (LivingEntity t : c.still(hit)) {
				Vec3 at = PairCast.mid(t);
				clouds.add(at);
				c.douse(t);
				c.sphere(PairCast.dust(0x8A8A8A, 1.2F), at, 2.2 * c.radius, 24);
				c.sound(SoundEvents.FIRE_EXTINGUISH, at, 0.9F, 1.0F);
			}
			c.every(10, 5, frame -> {
				for (Vec3 cloud : clouds) {
					c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cloud, 6, 1.2, 0.02);
					for (LivingEntity near : c.enemiesNear(cloud, 2.5 * c.radius)) {
						c.effect(near, MobEffects.BLINDNESS, 2, 0);
						c.hurt(near, c.power);
					}
				}
			});
		});
	}

	/**
	 * Stormsoak: a shock that holds, then a bolt into the soaked. The stunned enemies are held still for a second, and
	 * a bolt comes down on each of them at a second and a half, arcing into anything standing close. The look: crackling
	 * sparks while they are held, then a bolt on each with white arcs jumping between.
	 */
	@Pair(a = "jolt", b = "soak_through", name = "Stormsoak", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Up to 4 enemies hit take 4 lightning damage (times power) and are stunned for a second: target forgotten, "
			+ "path stopped, Slowness III. Soaked, each is struck 1.5 seconds later by a bolt for 6 lightning damage (times "
			+ "power), with 2 (times power) arcing to other enemies within 2.5 blocks.")
	public static void stormsoak(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, c.point(), 0.25F, 1.6F);
		for (LivingEntity t : hit) {
			c.shock(t, 4 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 1, 2);
			c.mark(t, Reactions.Mark.SOAKED);
		}
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(hit)) {
				calm(c, t);
				c.particles(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(t), 5, 0.5, 0.2);
				c.ring(PairCast.dust(0xBFE8FF, 0.8F), t.position().add(0, 0.1, 0), 0.9, 14, frame * 0.4);
			}
		});
		c.later(30, () -> {
			for (LivingEntity t : c.still(hit)) {
				Vec3 at = PairCast.mid(t);
				c.bolt(at);
				c.shock(t, 6 * c.power);
				for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
					if (!hit.contains(near)) {
						c.shock(near, 2 * c.power);
						c.zigzag(PairCast.dust(0xE8F4FF, 0.5F), at, PairCast.mid(near), 0.4, 2);
					}
				}
				c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, at, 0.4F, 1.2F);
			}
		});
	}

	/**
	 * Wellspring: a spring that rises where the spell lands and heals the allies standing by it. A wounded ally is drawn
	 * up once more. The look: a column of pale water rising and a mist of bubbles, with splashes for each draw.
	 */
	@Pair(a = "mending_mist", b = "spring_draw", name = "Wellspring", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A spring rises where it lands. Six times, a second apart, allies within 3 blocks heal 1 (times power) and are "
			+ "doused. An ally below half health is drawn up once, for 4 healing (times power).")
	public static void wellspring(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> drawn = new ArrayList<>();
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, spot, 0.8F, 1.0F);
		c.every(20, 6, frame -> {
			c.column(PairCast.dust(0x7FD8FF, 1.0F), spot, 0.8, 2.6, 26);
			c.particles(ParticleTypes.CLOUD, spot.add(0, 0.5, 0), 6, 2.0, 0.02);
			for (LivingEntity a : c.alliesNear(spot, 3 * c.radius)) {
				if (!a.isAlive()) {
					continue;
				}
				c.heal(a, c.power);
				c.douse(a);
				if (!drawn.contains(a) && a.getHealth() < a.getMaxHealth() / 2) {
					drawn.add(a);
					c.heal(a, 4 * c.power);
					c.particles(ParticleTypes.SPLASH, PairCast.mid(a), 14, 0.5, 0.15);
					c.sound(SoundEvents.PLAYER_SPLASH, PairCast.mid(a), 0.7F, 1.2F);
				}
			}
		});
	}

	/**
	 * Tithe of Blood: a wound opens on up to four enemies and bleeds for three seconds, and the blood is wrung into you.
	 * The look: dark drops falling from each wound, then threads of red that run to you at the end.
	 */
	@Pair(a = "bleed", b = "wring", name = "Tithe of Blood", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Opens a wound on up to 4 enemies: 1 magic damage (times power) every half second for 3 seconds, and they are "
			+ "marked bleeding. Then their blood is wrung to you: 2 healing (times power) for each one still standing.")
	public static void tithe(PairCast c) {
		List<LivingEntity> wounded = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.SHULKER_BULLET_HIT, c.point(), 0.6F, 0.8F);
		for (LivingEntity t : wounded) {
			c.mark(t, Reactions.Mark.BLEEDING);
		}
		c.every(10, 7, frame -> {
			for (LivingEntity t : c.still(wounded)) {
				Vec3 at = PairCast.mid(t);
				if (frame < 6) {
					c.hurt(t, c.power);
					c.mote(PairCast.dust(0x8A0F1C, 0.9F), at, new Vec3(0, -0.15, 0));
					c.particles(ParticleTypes.DRIPPING_WATER, at, 3, 0.3, 0.0);
				} else {
					c.line(PairCast.dust(0x8A0F1C, 0.8F), at, PairCast.mid(c.caster), 2);
					c.heal(c.caster, 2 * c.power);
					c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, at, 0.6F, 0.7F);
				}
			}
		});
	}

	/**
	 * Floodgate: a wall of water sweeps forward from where it lands. Each enemy it reaches is doused, soaked and slowed,
	 * and is carried along in front of the wall. The look: a pale blue line across the way that moves on, with splashes
	 * where it catches someone.
	 */
	@Pair(a = "sluice", b = "undertow", name = "Floodgate", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "A wall of water 5 blocks wide (times radius) sweeps about 6 blocks forward from where it lands, in 1.3 seconds. "
			+ "Each enemy within 3 blocks of it takes 2 magic damage (times power), is doused, soaked and slowed (Slowness III "
			+ "for 3 seconds), and is carried along in front of the wall.")
	public static void floodgate(PairCast c) {
		Vec3 start = c.point();
		Vec3 forward = flat(c.dir());
		Vec3 side = new Vec3(-forward.z, 0, forward.x);
		List<LivingEntity> swept = new ArrayList<>();
		c.sound(SoundEvents.GENERIC_SPLASH, start, 1.0F, 0.6F);
		c.every(3, 10, frame -> {
			Vec3 front = start.add(forward.scale(frame * 0.7));
			double half = 2.5 * c.radius;
			double reach = 3 * c.radius;
			c.line(PairCast.shift(0x7FD8FF, 0x1F6FB2, 1.2F), front.subtract(side.scale(half)), front.add(side.scale(half)), 2);
			for (LivingEntity e : c.enemiesNear(front, reach)) {
				if (!swept.contains(e)) {
					swept.add(e);
					c.hurt(e, 2 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 3, 2);
					c.mark(e, Reactions.Mark.SOAKED);
					c.douse(e);
					c.particles(ParticleTypes.SPLASH, PairCast.mid(e), 8, 0.4, 0.1);
					c.sound(SoundEvents.PLAYER_SPLASH, PairCast.mid(e), 0.6F, 1.2F);
				}
			}
			for (LivingEntity e : c.still(swept)) {
				if (PairCast.mid(e).distanceTo(front) < reach) {
					c.push(e, forward.scale(0.45));
				}
			}
		});
	}

	/**
	 * Hoarfrost Glide: you glide forward over a trail of rime, and the ice freezes whatever your path passes. The look: a
	 * pale trail of ice under your feet, snowflakes rising, and a chime of breaking glass at each enemy caught.
	 */
	@Pair(a = "frost", b = "rime_causeway", name = "Hoarfrost Glide", element = "frost", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration"},
		text = "You glide up to 4 blocks along your look over the next second, stopping short of walls. Each enemy within "
			+ "2 blocks of your path takes 3 cold damage (times power) and Slowness II for 3 seconds, once.")
	public static void rimeglide(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 ahead = flat(self.getLookAngle());
		List<LivingEntity> chilled = new ArrayList<>();
		boolean[] stopped = {false};
		c.sound(SoundEvents.GLASS_PLACE, self.position(), 0.8F, 1.6F);
		c.every(3, 8, frame -> {
			if (stopped[0] || !self.isAlive()) {
				return;
			}
			Vec3 from = self.position();
			Vec3 to = from.add(ahead.scale(0.5));
			HitResult wall = c.level.clip(new ClipContext(from.add(0, 0.9, 0), to.add(0, 0.9, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (wall.getType() != HitResult.Type.MISS || !c.blink(self, to)) {
				stopped[0] = true;
				return;
			}
			Vec3 now = self.position();
			c.line(PairCast.shift(0xE8FBFF, 0x7FD8FF, 0.9F), from.add(0, 0.1, 0), now.add(0, 0.1, 0), 2);
			c.particles(ParticleTypes.SNOWFLAKE, now.add(0, 0.9, 0), 4, 0.5, 0.02);
			for (LivingEntity e : c.enemiesNear(now, 2)) {
				if (!chilled.contains(e)) {
					chilled.add(e);
					c.freeze(e, 3 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 3, 1);
					c.particles(ParticleTypes.ITEM_SNOWBALL, PairCast.mid(e), 10, 0.4, 0.2);
					c.sound(SoundEvents.GLASS_BREAK, PairCast.mid(e), 0.6F, 1.4F);
				}
			}
		});
	}

	/** Forgets its target, so it stops chasing; mobs only, and never a boss. Called again each beat. */
	private static void calm(PairCast c, LivingEntity t) {
		if (t instanceof Mob m && c.movable(m)) {
			m.setTarget(null);
			m.getNavigation().stop();
		}
	}

	/** The horizontal way of {@code v}, or south when it has none. */
	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}
}
