package dev.wildercord.pairs.b014;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs, group 4: each has its own mechanic, look, sounds and rule text. */
public final class Pairs014 {
	private Pairs014() {}

	/**
	 * Hearth Ward: guard and hearth. Threads of warm light bind you to the nearest allies; a share of what a bound ally
	 * loses is taken by you instead, and when one of them falls low, the rest of the circle rallies round it.
	 */
	@Pair(a = "guardlink", b = "hearthbond", name = "Hearth Ward", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Bonds you to up to 4 allies within 12 blocks for 15 seconds. Twice a second, 40% of the health a bonded ally "
			+ "loses is taken by you instead, never below 6 health for you. A bond snaps past 16 blocks. When an ally falls "
			+ "under 6 health, the other bonded creatures get Regeneration II for 3 seconds, once per ally.")
	public static void hearthWard(PairCast c) {
		LivingEntity self = c.caster;
		List<LivingEntity> bonded = new ArrayList<>();
		for (LivingEntity a : c.alliesNear(PairCast.mid(self), 12)) {
			if (a != self && bonded.size() < 4) {
				bonded.add(a);
			}
		}
		if (bonded.isEmpty()) {
			return;
		}
		Map<LivingEntity, Float> seen = new HashMap<>();
		Set<LivingEntity> rallied = new HashSet<>();
		for (LivingEntity a : bonded) {
			seen.put(a, a.getHealth());
			c.line(PairCast.dust(0xC9F5A0, 0.8F), PairCast.mid(self), PairCast.mid(a), 3);
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, PairCast.mid(self), 0.8F, 1.4F);
		int beats = Math.max(1, (int) Math.round(30 * c.duration));
		c.every(10, beats, frame -> {
			bonded.removeIf(a -> !c.here(a) || a.distanceTo(self) > 16);
			for (LivingEntity a : bonded) {
				float now = a.getHealth();
				float lost = seen.getOrDefault(a, now) - now;
				if (lost > 0) {
					double moved = Math.min(0.4 * lost, self.getHealth() - 6);
					if (moved > 0) {
						c.hurt(self, moved);
						c.heal(a, moved);
						c.line(PairCast.dust(0xFFE27A, 0.9F), PairCast.mid(a), PairCast.mid(self), 2);
						c.sound(SoundEvents.ARMOR_EQUIP_IRON, PairCast.mid(self), 0.6F, 1.3F);
					}
				}
				seen.put(a, a.getHealth());
				if (a.getHealth() < 6 && rallied.add(a)) {
					c.sound(SoundEvents.BEACON_ACTIVATE, PairCast.mid(a), 0.7F, 1.6F);
					c.ring(PairCast.dust(0xFFE27A, 0.9F), PairCast.mid(a), 1.2, 16, frame * 0.3);
					c.effect(self, MobEffects.REGENERATION, 3, 1);
					for (LivingEntity o : bonded) {
						if (o != a) {
							c.effect(o, MobEffects.REGENERATION, 3, 1);
						}
					}
				}
			}
			if (frame % 3 == 0) {
				for (LivingEntity a : bonded) {
					c.line(PairCast.dust(0xC9F5A0, 0.6F), PairCast.mid(self), PairCast.mid(a), 2);
				}
			}
		});
	}

	/**
	 * Iron Lifeline: shell and second wind. Each ally wears an iron shell of absorption, topped back up every second.
	 * The first time one of them is nearly dead, a gust rises and the lifeline pulls it back, and shoves the attackers off.
	 */
	@Pair(a = "ironhold", b = "second_wind", name = "Iron Lifeline", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Each ally reached (up to 8) gets 6 absorption hearts, topped back up to 6 every second for 8 seconds. The first "
			+ "time an ally is at 4 health or less (checked every quarter second) it is healed 8, given Regeneration II and Speed II "
			+ "for 4 seconds, and a gust shoves enemies within 3 blocks away.")
	public static void ironLifeline(PairCast c) {
		List<LivingEntity> shielded = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		Set<LivingEntity> spent = new HashSet<>();
		int seconds = Math.max(1, (int) Math.round(8 * c.duration));
		int checks = Math.max(1, (int) Math.round(32 * c.duration));
		c.sound(SoundEvents.ARMOR_EQUIP_IRON, c.point(), 0.8F, 0.8F);
		// The shell is re-forged every second: a ring of iron dust round each ally.
		c.every(20, seconds, frame -> {
			for (LivingEntity a : c.still(shielded)) {
				c.absorb(a, 6 * c.power, 1.1);
				c.sphere(PairCast.dust(0xC8CCD2, 0.9F), PairCast.mid(a), 1.1, 20);
			}
		});
		// The lifeline watches for an ally at 4 health or less, four times a second.
		c.every(5, checks, frame -> {
			for (LivingEntity a : c.still(shielded)) {
				if (spent.contains(a) || a.getHealth() > 4) {
					continue;
				}
				spent.add(a);
				Vec3 at = PairCast.mid(a);
				c.heal(a, 8 * c.power);
				c.effect(a, MobEffects.REGENERATION, 4, 1);
				c.effect(a, MobEffects.SPEED, 4, 1);
				c.sound(SoundEvents.TOTEM_USE, at, 0.9F, 1.2F);
				c.column(PairCast.shift(0xFFE27A, 0xC8CCD2, 0.9F), at, 1.0, 2.2, 30);
				c.wave(PairCast.dust(0xE0E4EA, 1.0F), at.add(0, 0.2, 0), 18, 0.45);
				for (LivingEntity e : c.enemiesNear(at, 3)) {
					c.knockFrom(e, at, 1.2, 0.4);
				}
				c.punch(0.1F);
			}
		});
	}

	/**
	 * Scree Storm: pelt and sandstorm. Stones fly into the sand first, each one shoving its target out of the whirl; then
	 * the sandstorm turns for four seconds and grinds everything inside it.
	 */
	@Pair(a = "pelt", b = "sandstorm", name = "Scree Storm", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A sandstorm whirls at the point for 4 seconds, 3.5 blocks wide. At once, up to 4 enemies in it are pelted: 4 damage "
			+ "each and a hard shove outward. Every second it lasts, enemies in it take 2 damage, are blinded for 2 seconds "
			+ "and slowed (Slowness I for 2 seconds).")
	public static void screeStorm(PairCast c) {
		Vec3 at = c.point();
		double reach = 3.5 * c.radius;
		int seconds = Math.max(1, (int) Math.round(4 * c.duration));
		List<LivingEntity> pelted = PairCast.first(c.enemiesNear(at, reach), 4);
		for (LivingEntity t : pelted) {
			c.line(PairCast.dust(0xC8A96A, 0.9F), c.origin(), PairCast.mid(t), 3);
			c.strike(t, 4 * c.power);
			c.knockFrom(t, at, 1.0, 0.35);
			c.particles(PairCast.dust(0xB8925A, 1.0F), PairCast.mid(t), 10, 0.3, 0.05);
			c.sound(SoundEvents.GRAVEL_BREAK, PairCast.mid(t), 0.8F, 1.1F);
		}
		c.every(20, seconds, frame -> {
			c.column(PairCast.shift(0xE8C98A, 0xB8925A, 1.0F), at, reach, 2.5, 40);
			c.spiral(PairCast.dust(0xE8C98A, 0.8F), at, reach * 0.7, 2.5, 3, 30);
			c.sound(SoundEvents.SAND_BREAK, at, 0.7F, 0.8F + frame * 0.05F);
			for (LivingEntity t : c.enemiesNear(at, reach)) {
				c.strike(t, 2 * c.power);
				c.effect(t, MobEffects.BLINDNESS, 2, 0);
				c.effect(t, MobEffects.SLOWNESS, 2, 0);
			}
		});
	}

	/**
	 * Thornhook: root and tidehook. Vines lash each target, hold it under a heavy slow, and reel it in to your feet over
	 * three tugs; whatever comes in close is left dripping.
	 */
	@Pair(a = "root", b = "tidehook", name = "Thornhook", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Vines lash up to 4 targets: 3 damage each and Slowness V for 3 seconds. Over one second, three tugs reel each "
			+ "toward your feet, and each one that comes within 3 blocks of you is left soaked.")
	public static void thornhook(PairCast c) {
		List<LivingEntity> hooked = PairCast.first(c.enemies(), 4);
		Vec3 hand = c.origin();
		for (LivingEntity t : hooked) {
			c.strike(t, 3 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 3, 4);
			c.line(PairCast.dust(0x3E7A2E, 0.9F), hand, PairCast.mid(t), 3);
			c.sound(SoundEvents.GRASS_BREAK, PairCast.mid(t), 0.9F, 0.8F);
		}
		c.every(10, 3, frame -> {
			Vec3 feet = c.caster.position();
			for (LivingEntity t : c.still(hooked)) {
				c.line(PairCast.dust(0x7CC24A, 0.7F), feet.add(0, 1, 0), PairCast.mid(t), 3);
				c.pullTo(t, feet, 1.2);
				c.sound(SoundEvents.GRASS_PLACE, feet, 0.6F, 0.9F + frame * 0.2F);
			}
		});
		c.later(25, () -> {
			Vec3 feet = c.caster.position();
			for (LivingEntity t : c.still(hooked)) {
				if (t.position().distanceTo(feet) <= 3) {
					c.mark(t, Reactions.Mark.SOAKED);
					c.particles(ParticleTypes.SPLASH, PairCast.mid(t), 10, 0.3, 0.1);
				}
			}
		});
	}

	/**
	 * Ember Sink: the sinkhole and the hellmouth in turn. The ground gives way and drags everything in the pit to its
	 * middle; a second later the magma rises through the core and burns whatever is still in it.
	 */
	@Pair(a = "hellmouth", b = "sinkhole", name = "Ember Sink", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "The ground gives way over 3 blocks: each enemy in it takes 2 damage and Slowness IV for 2 seconds, and is dragged "
			+ "to the middle. A second later magma rises: 5 fire damage to each one still within reach, set alight for 3 seconds.")
	public static void emberSink(PairCast c) {
		Vec3 at = c.point();
		double reach = 3 * c.radius;
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(at, reach), PairCast.MAX_TARGETS);
		for (LivingEntity t : caught) {
			c.strike(t, 2 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 2, 3);
		}
		c.sound(SoundEvents.GRAVEL_BREAK, at, 1.0F, 0.6F);
		// The ground shrinks in on itself over five frames as the enemies are dragged to the middle.
		c.every(4, 5, frame -> {
			c.disc(PairCast.dust(0x3A2A1E, 1.2F), at, reach * (1 - frame * 0.15), 24);
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, at, 0.7);
			}
		});
		c.later(20, () -> {
			c.sound(SoundEvents.LAVA_POP, at, 1.0F, 0.8F);
			c.sound(SoundEvents.FIRECHARGE_USE, at, 0.7F, 0.7F);
			c.column(PairCast.shift(0xFFB02E, 0xFF4500, 1.2F), at, reach * 0.6, 2.5, 40);
			c.particles(ParticleTypes.LAVA, at, 14, 0.8, 0.1);
			c.shake(at, 0.3F, 6);
			for (LivingEntity t : c.still(caught)) {
				if (t.position().distanceTo(at) <= reach) {
					c.burn(t, 5 * c.power);
					c.ignite(t, 3);
				}
			}
		});
	}

	/**
	 * Clockwork Quake: stormclock and thunderquake. Lightning strikes the point three times on a clock, and the last
	 * strike rolls a quake out across the ground that tosses whatever it reaches.
	 */
	@Pair(a = "stormclock", b = "thunderquake", name = "Clockwork Quake", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Lightning strikes the point: 4 damage to enemies within 1.5 blocks. Two seconds later and four seconds later it "
			+ "strikes the same spot again: 3 damage each time. The last strike quakes the ground, a shockwave rolling 6 blocks out "
			+ "in four steps: 3 damage to each enemy it reaches, tossed up.")
	public static void clockworkQuake(PairCast c) {
		Vec3 at = c.point();
		double near = 1.5 * c.radius;
		c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.9F, 1.1F);
		strikeAt(c, at, near, 4 * c.power, 1.0F);
		c.later(40, () -> {
			c.sound(SoundEvents.BELL_RESONATE, at, 1.0F, 0.8F);
			strikeAt(c, at, near, 3 * c.power, 1.2F);
		});
		c.later(80, () -> {
			c.sound(SoundEvents.BELL_RESONATE, at, 1.0F, 1.2F);
			strikeAt(c, at, near, 3 * c.power, 1.4F);
			c.shake(at, 0.4F, 10);
			Set<LivingEntity> rocked = new HashSet<>();
			c.every(3, 4, frame -> {
				double r = 1.5 * (frame + 1) * c.radius;
				c.ring(PairCast.shift(0xFFF6D6, 0x9AB0FF, 0.9F), at.add(0, 0.2, 0), r, 28, frame * 0.2);
				c.sound(SoundEvents.MACE_SMASH_GROUND_HEAVY, at, 0.7F, 1.0F - frame * 0.1F);
				for (LivingEntity e : c.enemiesNear(at, r)) {
					if (rocked.add(e)) {
						c.strike(e, 3 * c.power);
						c.knockFrom(e, at, 0.4, 0.6);
					}
				}
			});
		});
	}

	/** One lightning strike at {@code at}: {@code damage} to every enemy within {@code near} blocks, and a flash. */
	private static void strikeAt(PairCast c, Vec3 at, double near, double damage, float shake) {
		c.bolt(at);
		c.shake(at, 0.2F * shake, 8);
		for (LivingEntity e : c.enemiesNear(at, near)) {
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 6, 0), PairCast.mid(e), 0.4, 3);
			c.shock(e, damage);
		}
	}

	/**
	 * Millstone: downdraft and weigh. A stone wheel turns over the point and drops; everything under it is slammed
	 * down, the higher it was the harder, and then sits under a crushing weight.
	 */
	@Pair(a = "downdraft", b = "weigh", name = "Millstone", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A stone wheel drops onto the point from 3 blocks up half a second later. Up to 8 enemies within 5 blocks take 3 "
			+ "damage, plus 1 for every block they are above the ground (up to 6), and airborne ones are slammed down. Then for 4 "
			+ "seconds they are crushed: Slowness III and 1 damage a second.")
	public static void millstone(PairCast c) {
		Vec3 at = c.point();
		double reach = 5 * c.radius;
		int seconds = Math.max(1, (int) Math.round(4 * c.duration));
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, at, 0.7F, 0.6F);
		// The wheel turns and descends over five frames, sweeping the ground beneath.
		c.every(2, 5, frame -> {
			c.disc(PairCast.dust(0x8A8A86, 1.3F), at.add(0, 3 - frame * 0.6, 0), 2.5, 20);
			c.ring(PairCast.dust(0x5E5E5A, 1.0F), at.add(0, 3 - frame * 0.6, 0), 2.5, 18, frame * 0.4);
		});
		c.later(10, () -> {
			c.sound(SoundEvents.ANVIL_LAND, at, 1.0F, 0.6F);
			c.shake(at, 0.45F, 10);
			c.disc(PairCast.dust(0x8A8A86, 1.4F), at, 4 * c.radius, 40);
			List<LivingEntity> crushed = PairCast.first(c.enemiesNear(at, reach), PairCast.MAX_TARGETS);
			for (LivingEntity t : crushed) {
				double high = t.getY() - c.ground(t.position()).y;
				double drop = Math.min(6, Math.max(0, high));
				c.hurt(t, (3 + drop) * c.power);
				if (high > 1) {
					c.push(t, new Vec3(0, -1.2, 0));
				}
				c.effect(t, MobEffects.SLOWNESS, seconds, 2);
				c.particles(ParticleTypes.CRIT, PairCast.mid(t), 8, 0.3, 0.1);
			}
			c.every(20, seconds, frame -> {
				for (LivingEntity t : c.still(crushed)) {
					c.strike(t, 1 * c.power);
					c.column(PairCast.dust(0x8A8A86, 0.9F), t.position(), 0.6, 0.5, 8);
				}
			});
		});
	}

	/**
	 * Time Echo: clockroot and time_skip. You step forward onto safe ground and are untouchable for a second, leaving an
	 * echo behind; three seconds later the echo snaps back to where you stood and hurts the enemies gathered there.
	 */
	@Pair(a = "clockroot", b = "time_skip", name = "Time Echo", element = "time", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You step up to 8 blocks forward onto safe ground (walls stop you), untouchable for 1 second; nearby monsters lose "
			+ "track of you. The spot you left keeps an echo: 3 seconds later each enemy within 2.5 blocks of it takes 4 damage "
			+ "and is slowed (Slowness III for 2 seconds). No safe ground in reach: nothing happens.")
	public static void timeEcho(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 look = self.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return;
		}
		flat = flat.normalize();
		Vec3 before = self.position();
		Vec3 start = self.getEyePosition();
		Vec3 want = start.add(flat.scale(8));
		BlockHitResult wall = c.level.clip(new ClipContext(start, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		Vec3 end = wall.getType() == HitResult.Type.MISS ? want : wall.getLocation().subtract(flat.scale(0.6));
		if (!c.blink(self, c.ground(end))) {
			return;
		}
		c.effect(self, MobEffects.RESISTANCE, 1, 4);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, before, 0.8F, 0.6F);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, self.position(), 0.6F, 1.4F);
		c.column(PairCast.shift(0xE8E0FF, 0x6A5BFF, 0.9F), before, 0.8, 2.5, 22);
		c.wave(PairCast.dust(0x6A5BFF, 0.9F), self.position().add(0, 0.2, 0), 14, 0.3);
		for (LivingEntity m : c.enemiesNear(before, 8)) {
			if (m instanceof Mob mob && c.movable(mob)) {
				mob.setTarget(null);
			}
		}
		// The echo flickers on its spot for three seconds, shrinking inward.
		c.every(6, 4, frame -> c.ring(PairCast.dust(0x6A5BFF, 0.8F), before.add(0, 0.1, 0), 1.6 - frame * 0.3, 14, frame * 0.4));
		c.later(60, () -> {
			c.sound(SoundEvents.ENDERMAN_TELEPORT, before, 1.0F, 1.5F);
			c.wave(PairCast.shift(0xE8E0FF, 0x6A5BFF, 1.0F), before.add(0, 0.2, 0), 20, 0.3);
			c.spiral(PairCast.dust(0xE8E0FF, 0.8F), before, 1.2, 2.0, 2, 24);
			for (LivingEntity e : c.enemiesNear(before, 2.5)) {
				c.hurt(e, 4 * c.power);
				c.effect(e, MobEffects.SLOWNESS, 2, 2);
			}
		});
	}

	/**
	 * Frostbark: bloom and bark. Each ally is warmed with Regeneration and a frost bark forms on it; every time it takes
	 * a blow, the cold bursts out to the enemies beside it and the bark closes a little of the wound.
	 */
	@Pair(a = "aftercare", b = "frostbloom", name = "Frostbark", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Each ally reached (up to 8) gets Regeneration II for 5 seconds. For 10 seconds, twice a second: if an ally has lost "
			+ "health since the last check, each enemy within 3 blocks of it takes 2 frost damage and Slowness III for 2 seconds, "
			+ "and the ally is healed 1 (6 times in all).")
	public static void frostbark(PairCast c) {
		List<LivingEntity> barked = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		Map<LivingEntity, Float> seen = new HashMap<>();
		Map<LivingEntity, Integer> mended = new HashMap<>();
		for (LivingEntity a : barked) {
			c.effect(a, MobEffects.REGENERATION, 5, 1);
			seen.put(a, a.getHealth());
			mended.put(a, 0);
			c.sphere(PairCast.dust(0xBDEBFF, 0.9F), PairCast.mid(a), 1.0, 24);
		}
		c.sound(SoundEvents.SNOW_PLACE, c.point(), 0.9F, 0.9F);
		int checks = Math.max(1, (int) Math.round(20 * c.duration));
		c.every(10, checks, frame -> {
			for (LivingEntity a : c.still(barked)) {
				float now = a.getHealth();
				float before = seen.getOrDefault(a, now);
				seen.put(a, now);
				if (now >= before) {
					continue;
				}
				Vec3 at = PairCast.mid(a);
				c.sound(SoundEvents.GLASS_BREAK, at, 0.6F, 1.6F);
				c.particles(ParticleTypes.ITEM_SNOWBALL, at, 10, 0.4, 0.2);
				for (LivingEntity e : c.enemiesNear(at, 3)) {
					c.freeze(e, 2 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 2, 2);
					c.line(PairCast.dust(0xBDEBFF, 0.7F), at, PairCast.mid(e), 3);
				}
				int done = mended.getOrDefault(a, 0);
				if (done < 6) {
					mended.put(a, done + 1);
					c.heal(a, 1);
					seen.put(a, a.getHealth());
				}
			}
		});
	}

	/**
	 * Verdant Canopy: barkhide and leafshade. A canopy of leaves forms overhead; under it allies are hardened and cooled
	 * against harm and fire, and the leaves keep them mended.
	 */
	@Pair(a = "barkhide", b = "leafshade", name = "Verdant Canopy", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "A canopy of leaves forms 2.5 blocks up over the point, 3 blocks out, for 12 seconds. Allies under it get Resistance I "
			+ "and Fire Resistance, put out if burning, and are healed 1 every 2 seconds (6 in all).")
	public static void verdantCanopy(PairCast c) {
		Vec3 at = c.point();
		double reach = 3 * c.radius;
		int beats = Math.max(1, (int) Math.round(12 * c.duration));
		Vec3 roof = at.add(0, 2.5, 0);
		c.sound(SoundEvents.GRASS_PLACE, at, 0.9F, 0.8F);
		c.every(20, beats, frame -> {
			c.ring(PairCast.dust(0x3F8F2E, 1.0F), roof, reach, 30, frame * 0.25);
			c.column(PairCast.dust(0x7CC24A, 0.8F), roof, reach * 0.8, 0.8, 14);
			if (frame % 2 == 0) {
				c.sound(SoundEvents.AZALEA_LEAVES_PLACE, at, 0.5F, 1.1F);
			}
			for (LivingEntity a : c.alliesNear(at, reach)) {
				c.effect(a, MobEffects.RESISTANCE, 2, 0);
				c.effect(a, MobEffects.FIRE_RESISTANCE, 2, 0);
				c.douse(a);
				if (frame % 2 == 1) {
					c.heal(a, 1);
					c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 6, 0.4, 0.02);
				}
			}
		});
	}
}
