package dev.wildercord.pairs.b033;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs for the angler, shell, tide, bell and ice runes: each has its own mechanic, look and sound. */
public final class Pairs033 {
	private Pairs033() {}

	/**
	 * Lure Reel: a line is cast to the point and you are reeled along it to the spot, a moment later the hook bites
	 * everyone standing near where you land. The look: a pale line drawn out, then a ring where the hook came down.
	 */
	@Pair(a = "angler_lure", b = "grapple", name = "Lure Reel", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Reels you to where the spell hit, up to 12 blocks, stopped by walls. The hook bites on landing: every enemy within "
			+ "2.5 blocks takes 3 damage and gets Slowness I for 3 seconds.")
	public static void lureReel(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 eye = start.add(0, 0.9, 0);
		Vec3 out = c.point().subtract(start);
		Vec3 dir = out.lengthSqr() < 1.0E-4 ? flatDir(c) : out.normalize();
		double dist = Math.min(12, out.length());
		Vec3 goal = start.add(dir.scale(dist));
		BlockHitResult wall = c.level.clip(new ClipContext(eye, goal.add(0, 0.9, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			dist = Math.max(0, wall.getLocation().subtract(eye).length() - 0.6);
			goal = start.add(dir.scale(dist));
		}
		Vec3 land = goal;
		Vec3 hook = land.add(0, 0.6, 0);
		// Wind-up: the line runs out from the hand to where the hook will come down.
		c.sound(SoundEvents.FISHING_BOBBER_THROW, eye, 0.8F, 1.0F);
		c.every(3, 3, frame -> c.line(PairCast.dust(0xE0F4FF, 0.6F), eye, hook, 3));
		// Payoff: the reel pulls you in, and the hook bites.
		c.later(6, () -> {
			c.blink(self, land);
			c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, land, 1.0F, 0.9F);
			c.punch(0.2F);
			c.ring(PairCast.shift(0x9FD8FF, 0xE0E6EA, 0.8F), land.add(0, 0.2, 0), 0.8, 16, 0);
			for (LivingEntity t : c.enemiesNear(land, 2.5 * c.radius)) {
				c.strike(t, 3 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 3, 0);
				c.line(PairCast.dust(0xE0F4FF, 0.6F), hook, PairCast.mid(t), 3);
			}
		});
	}

	/**
	 * Gillsong: a song rises from the point; the allies it reaches are healed and shielded, the axolotls nearby are
	 * healed too, and a few seconds later the echo gives the choir a short Regeneration. The look: pink spirals
	 * climbing, then a lilac ring spreading over the water.
	 */
	@Pair(a = "axolotl_kinship", b = "heal", name = "Gillsong", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 6 blocks of the point are healed 4 and get 2 absorption for 6 seconds. Axolotls within 12 blocks are "
			+ "healed 4 and get Regeneration I for 8 seconds. Three seconds after the cast, the echo gives the allies Regeneration I for 4 seconds.")
	public static void gillsong(PairCast c) {
		Vec3 spot = c.point();
		double reach = 6 * c.radius;
		AABB box = new AABB(spot.x - 12, spot.y - 12, spot.z - 12, spot.x + 12, spot.y + 12, spot.z + 12);
		c.sound(SoundEvents.AXOLOTL_SPLASH, spot, 0.8F, 1.1F);
		c.every(4, 3, frame -> c.spiral(PairCast.shift(0xFFB3D9, 0xE9C8FF, 0.8F), spot, 0.6 + frame * 0.5, 1.6 + frame * 0.6, 2, 18));
		c.later(12, () -> {
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.heal(a, 4 * c.power);
				c.absorb(a, 2 * c.power, 6);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 8, 0.3, 0.05);
			}
			for (LivingEntity axo : c.level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.getType() == EntityTypes.AXOLOTL && e.distanceToSqr(spot) <= 144)) {
				c.heal(axo, 4 * c.power);
				c.effect(axo, MobEffects.REGENERATION, 8, 0);
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(axo), 6, 0.3, 0.02);
			}
			c.ring(PairCast.shift(0xFFB3D9, 0xE9C8FF, 0.8F), spot.add(0, 0.2, 0), reach, 28, 0.2);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 0.8F, 1.3F);
		});
		c.later(60, () -> {
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.effect(a, MobEffects.REGENERATION, 4, 0);
			}
			c.ring(PairCast.shift(0xE9C8FF, 0xFFB3D9, 0.7F), spot.add(0, 0.2, 0), reach * 0.6, 20, 0.5);
			c.sound(SoundEvents.AXOLOTL_IDLE_WATER, spot, 0.7F, 0.9F);
		});
	}

	/**
	 * Reef Balm: a reef of coral grows on the point and stays for eight seconds. Those standing in it are healed and
	 * put out, and when it fades they are left with a steady regeneration. The look: a coral ring growing from the
	 * ground, then a teal disc that pulses each time it heals.
	 */
	@Pair(a = "coral_mend", b = "salve", name = "Reef Balm", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "For 8 seconds a coral reef grows on the point, 3 blocks wide. Every 2 seconds each ally in it is healed 1 and put "
			+ "out if burning. When it fades, each of them gets Regeneration I for 4 seconds.")
	public static void reefBalm(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		c.sound(SoundEvents.GLASS_PLACE, spot, 0.7F, 0.7F);
		c.every(10, 4, frame -> c.ring(PairCast.shift(0xFF7F6E, 0x4FD6C9, 0.9F), spot.add(0, 0.2, 0), r * (frame + 1) / 4.0, 24,
			frame * 0.5));
		c.every(40, 5, pulse -> {
			for (LivingEntity a : c.alliesNear(spot, r)) {
				c.heal(a, 1 * c.power);
				if (a.isOnFire()) {
					c.douse(a);
				}
				if (pulse == 4) {
					c.effect(a, MobEffects.REGENERATION, 4, 0);
				}
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 5, 0.3, 0.05);
			}
			c.disc(PairCast.shift(0x4FD6C9, 0xFF7F6E, 0.9F), spot.add(0, 0.1, 0), r, 24);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 0.5F, 1.0F + 0.1F * pulse);
		});
	}

	/**
	 * Tidewake: a wake of current carries you and your crew, speeding them and nudging them along the way you face,
	 * four times in eight seconds. The look: foam rings out from the caster, dolphin-blue spray lifting off each one.
	 */
	@Pair(a = "currentkin", b = "swift", name = "Tidewake", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "You and up to 4 allies within 6 blocks get Speed II for 8 seconds. Four times, two seconds apart, a wake of current "
			+ "pulses out from you: each of you it reaches is nudged along the way you face and gets Dolphin's Grace for 3 seconds.")
	public static void tidewake(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 fwd = flatDir(c);
		List<LivingEntity> crew = new ArrayList<>();
		crew.add(self);
		for (LivingEntity a : c.alliesNear(PairCast.mid(self), 6 * c.radius)) {
			if (crew.size() < 5 && !crew.contains(a)) {
				crew.add(a);
			}
		}
		for (LivingEntity a : crew) {
			c.effect(a, MobEffects.SPEED, 8, 1);
		}
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, PairCast.mid(self), 0.8F, 1.0F);
		c.every(40, 4, pulse -> {
			Vec3 at = self.position().add(0, 0.2, 0);
			c.ring(PairCast.shift(0x3FC7E8, 0xE8FBFF, 0.9F), at, 6 * c.radius, 28, pulse * 0.3);
			for (LivingEntity a : c.still(crew)) {
				if (PairCast.mid(a).distanceTo(PairCast.mid(self)) <= 6 * c.radius) {
					c.push(a, fwd.scale(0.6).add(0, 0.1, 0));
					c.effect(a, MobEffects.DOLPHINS_GRACE, 3, 0);
					c.particles(ParticleTypes.SPLASH, PairCast.mid(a), 10, 0.4, 0.1);
				}
			}
			c.sound(SoundEvents.DOLPHIN_SPLASH, PairCast.mid(self), 0.6F, 1.0F + 0.1F * pulse);
		});
	}

	/**
	 * Airless Bell: a bell of air is dropped over each enemy you hit. It holds them, thinning their breath and
	 * weakening them, then lets go and they fall. The look: a pale bubble shell around each, shrinking as it closes.
	 */
	@Pair(a = "bubble", b = "diving_bell", name = "Airless Bell", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Each of up to 4 enemies you hit is wrapped in a bell of air for 4 seconds: 1 damage a second and "
			+ "Weakness I. Then the bell lets go: they are tossed up, and 0.8 seconds later they take 4 damage.")
	public static void airlessBell(PairCast c) {
		List<LivingEntity> bells = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.GLASS_PLACE, c.point(), 0.9F, 1.4F);
		for (LivingEntity t : bells) {
			c.effect(t, MobEffects.WEAKNESS, 4, 0);
		}
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(bells)) {
				c.sphere(PairCast.dust(0xA8E6FF, 0.9F), PairCast.mid(t), 1.0 * c.radius - frame * 0.15, 22);
			}
		});
		c.every(20, 4, sec -> {
			for (LivingEntity t : c.still(bells)) {
				c.hurt(t, 1 * c.power);
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(t), 6, 0.4, 0.05);
			}
			c.sound(SoundEvents.BUBBLE_POP, c.point(), 0.4F, 0.8F + 0.1F * sec);
		});
		c.later(80, () -> {
			for (LivingEntity t : c.still(bells)) {
				c.lift(t, 0.9);
			}
			c.sound(SoundEvents.BUBBLE_POP, c.point(), 0.8F, 0.6F);
		});
		c.later(96, () -> {
			for (LivingEntity t : c.still(bells)) {
				c.hurt(t, 4 * c.power);
				c.ring(PairCast.shift(0xDDEFF7, 0x3B7FA8, 0.8F), PairCast.mid(t), 0.9, 14, 0);
			}
		});
	}

	/**
	 * Pod Breach: enemies are shoved along the way you face, and a second later a pod of ripples breaches them: each
	 * takes a hard blow and anything standing close takes a lesser one. The look: blue ripples circling each target,
	 * a splash column on the breach.
	 */
	@Pair(a = "dash", b = "dolphin_call", name = "Pod Breach", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Shoves up to 4 enemies you hit along the way you face. A second later the pod breaches them: 4 damage and Slowness II "
			+ "for 2 seconds, and each other enemy within 2 blocks of one takes 2 damage.")
	public static void podBreach(PairCast c) {
		Vec3 fwd = flatDir(c);
		List<LivingEntity> pod = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.DOLPHIN_JUMP, c.point(), 0.8F, 1.2F);
		for (LivingEntity t : pod) {
			c.push(t, fwd.scale(1.3).add(0, 0.3, 0));
			c.particles(ParticleTypes.SPLASH, PairCast.mid(t), 12, 0.4, 0.1);
		}
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(pod)) {
				c.ring(PairCast.shift(0x2F7BD9, 0xDDF2FF, 0.9F), PairCast.mid(t), 0.5 + frame * 0.3, 14, frame * 0.4);
			}
		});
		c.later(20, () -> {
			for (LivingEntity t : c.still(pod)) {
				Vec3 at = PairCast.mid(t);
				c.strike(t, 4 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 1);
				c.column(PairCast.shift(0xDDF2FF, 0x2F7BD9, 0.9F), t.position(), 0.9, 2.5, 22);
				c.sound(SoundEvents.DOLPHIN_SPLASH, at, 0.9F, 0.9F);
				for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
					if (near != t) {
						c.hurt(near, 2 * c.power);
					}
				}
			}
		});
	}

	/**
	 * Trawl: enemies you hit are drawn into a net laid on the point. They are held there, stung every second, and
	 * then hauled up and struck. The look: a grey mesh drawn out on the ground, chain-coloured sparks each second.
	 */
	@Pair(a = "drift_net", b = "shackle", name = "Trawl", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Draws up to 4 enemies you hit toward the point and nets them for 4 seconds. Each second in the net is 1 damage and "
			+ "Slowness II for 2 seconds; anyone who strays more than 2.5 blocks is drawn back. Then the net is hauled: 3 damage and a lift.")
	public static void trawl(PairCast c) {
		Vec3 spot = c.point();
		double reach = 2.5 * c.radius;
		List<LivingEntity> netted = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.CHAIN_PLACE, spot, 0.9F, 0.8F);
		for (LivingEntity t : netted) {
			c.pullTo(t, spot, 1.2 * c.radius);
		}
		c.every(5, 4, frame -> {
			double r = reach * (frame + 1) / 4.0;
			for (int i = -2; i <= 2; i++) {
				double o = i * r * 0.5;
				c.line(PairCast.dust(0xB0B7BD, 0.6F), spot.add(-r, 0.1, o), spot.add(r, 0.1, o), 2);
				c.line(PairCast.dust(0xB0B7BD, 0.6F), spot.add(o, 0.1, -r), spot.add(o, 0.1, r), 2);
			}
		});
		c.every(20, 4, sec -> {
			for (LivingEntity t : c.still(netted)) {
				if (PairCast.mid(t).distanceTo(spot) > reach) {
					c.pullTo(t, spot, 1.2 * c.radius);
				}
				c.hurt(t, 1 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 1);
				c.particles(ParticleTypes.CRIT, PairCast.mid(t), 4, 0.3, 0.1);
			}
			c.sound(SoundEvents.CHAIN_STEP, spot, 0.5F, 1.1F);
		});
		c.later(80, () -> {
			for (LivingEntity t : c.still(netted)) {
				c.lift(t, 0.8);
				c.strike(t, 3 * c.power);
			}
			c.sound(SoundEvents.CHAIN_BREAK, spot, 0.9F, 0.9F);
			c.ring(PairCast.shift(0xB0B7BD, 0x8A6A3C, 0.8F), spot.add(0, 0.2, 0), reach, 20, 0);
		});
	}

	/**
	 * Hearthguard: a ward of cold over you and your allies. For six seconds fire can't keep hold of them: each second
	 * any that is burning is put out and shielded in frost. The look: pale ice rings beating out from each ward,
	 * ember-coloured smoke where a flame is snuffed.
	 */
	@Pair(a = "firebreak", b = "frostward", name = "Hearthguard", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "For 6 seconds you and up to 5 allies within 6 blocks have Fire Resistance. Every second, each of them that is burning "
			+ "is put out and gets 2 absorption for 4 seconds.")
	public static void hearthguard(PairCast c) {
		LivingEntity self = c.caster;
		List<LivingEntity> wards = new ArrayList<>();
		wards.add(self);
		for (LivingEntity a : c.alliesNear(PairCast.mid(self), 6 * c.radius)) {
			if (wards.size() < 6 && !wards.contains(a)) {
				wards.add(a);
			}
		}
		for (LivingEntity a : wards) {
			c.effect(a, MobEffects.FIRE_RESISTANCE, 6, 0);
		}
		c.sound(SoundEvents.FIRE_EXTINGUISH, PairCast.mid(self), 0.8F, 1.2F);
		c.every(20, 6, sec -> {
			for (LivingEntity a : c.still(wards)) {
				Vec3 at = PairCast.mid(a);
				c.ring(PairCast.shift(0xBFEFFF, 0xFFA640, 0.9F), at, 0.8, 16, sec * 0.4);
				if (a.isOnFire()) {
					c.douse(a);
					c.absorb(a, 2 * c.power, 4);
					c.particles(ParticleTypes.SMOKE, at, 10, 0.3, 0.05);
					c.sound(SoundEvents.FIRE_EXTINGUISH, at, 0.6F, 1.6F);
				}
			}
		});
	}

	/**
	 * Frost Bore: an ice drill spirals down onto each enemy you hit and bores into it a second later, then the drill
	 * spins on to the nearest enemy near it. The look: snowflakes winding down a helix, then a blue column, then a
	 * thin chain of frost between each link.
	 */
	@Pair(a = "ice_auger", b = "icicle", name = "Frost Bore", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 3 enemies you hit are slowed (Slowness I) for a second. A second later an ice drill bores into each: 6 freeze "
			+ "damage, and they're soaked. The drill spins on: each struck enemy is followed by the nearest other enemy within 3 blocks "
			+ "(3 damage, soaked), two links at most.")
	public static void frostBore(PairCast c) {
		List<LivingEntity> drilled = PairCast.first(c.enemies(), 3);
		c.sound(SoundEvents.POWDER_SNOW_STEP, c.point(), 0.8F, 0.7F);
		for (LivingEntity t : drilled) {
			c.effect(t, MobEffects.SLOWNESS, 1, 0);
		}
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(drilled)) {
				c.helix(PairCast.dust(0xCDF3FF, 0.9F), ParticleTypes.SNOWFLAKE, PairCast.mid(t).add(0, 2.0 - frame * 0.4, 0),
					0.6, 2.0, 2, 14);
			}
		});
		c.later(20, () -> {
			Set<LivingEntity> seen = new HashSet<>();
			for (LivingEntity t : c.still(drilled)) {
				bore(c, t, 6, 2, seen);
			}
		});
	}

	/** One bore: a freeze hit that soaks the target, then the drill spins on to the nearest unbored enemy near it. */
	private static void bore(PairCast c, LivingEntity t, double damage, int links, Set<LivingEntity> seen) {
		seen.add(t);
		Vec3 at = PairCast.mid(t);
		c.freeze(t, damage * c.power);
		c.mark(t, Reactions.Mark.SOAKED);
		c.column(PairCast.shift(0xCDF3FF, 0x2C6DAA, 0.9F), t.position(), 0.6, 2.2, 20);
		c.sound(SoundEvents.GLASS_BREAK, at, 0.8F, 1.3F);
		if (links > 0) {
			c.later(20, () -> {
				LivingEntity next = nearestUnbored(c, at, seen);
				if (next != null && c.here(next)) {
					c.line(PairCast.dust(0xCDF3FF, 0.7F), at, PairCast.mid(next), 3);
					bore(c, next, 3, links - 1, seen);
				}
			});
		}
	}

	/** The nearest enemy within 3 blocks of {@code at} that the drill has not bored yet, or null. */
	private static LivingEntity nearestUnbored(PairCast c, Vec3 at, Set<LivingEntity> seen) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
			double d = PairCast.mid(e).distanceToSqr(at);
			if (!seen.contains(e) && d < bestDist) {
				best = e;
				bestDist = d;
			}
		}
		return best;
	}

	/**
	 * Rimeglide: you wind up, then glide along your facing, leaving a lane of rime. Anything beside the glide is
	 * chilled and slowed, and you finish fast. The look: a frost ring gathering at your feet, then a lane of frost
	 * laid down as you pass.
	 */
	@Pair(a = "icepath", b = "swift", name = "Rimeglide", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration"},
		text = "After a short wind-up you glide up to 8 blocks along the way you face, stopped by walls, leaving a lane of rime. "
			+ "Enemies within 1.5 blocks of the glide take 2 damage and get Slowness II for 3 seconds; you get Speed III for 3 seconds.")
	public static void rimeglide(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 fwd = flatDir(c);
		Vec3 eye = start.add(0, 0.9, 0);
		double run = 8;
		BlockHitResult wall = c.level.clip(new ClipContext(eye, eye.add(fwd.scale(run)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			run = Math.max(0, wall.getLocation().subtract(eye).length() - 0.6);
		}
		double distance = run;
		int steps = Math.max(1, (int) Math.ceil(distance));
		Set<LivingEntity> brushed = new HashSet<>();
		// Wind-up: rime gathers under your feet.
		c.sound(SoundEvents.POWDER_SNOW_PLACE, start, 0.7F, 1.6F);
		c.every(2, 3, frame -> c.ring(PairCast.shift(0xE6FFFF, 0x9FE7FF, 0.8F), start.add(0, 0.1, 0), 1.0 - frame * 0.25, 12, frame));
		// Glide: one step a tick, each step laying frost and brushing whatever is near.
		c.later(6, () -> {
			c.effect(self, MobEffects.SPEED, 3, 2);
			c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, start, 0.8F, 1.4F);
			c.every(1, steps, i -> {
				Vec3 at = start.add(fwd.scale(distance * (i + 1) / steps));
				c.blink(self, at);
				c.ring(PairCast.dust(0xE6FFFF, 0.8F), at.add(0, 0.1, 0), 0.7, 10, 0);
				for (LivingEntity t : c.enemiesNear(at.add(0, 1, 0), 1.5)) {
					if (brushed.add(t)) {
						c.freeze(t, 2 * c.power);
						c.effect(t, MobEffects.SLOWNESS, 3, 1);
						c.particles(ParticleTypes.SNOWFLAKE, PairCast.mid(t), 8, 0.3, 0.05);
					}
				}
			});
			c.later(steps + 2, () -> {
				c.punch(0.2F);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, self.position(), 0.6F, 1.5F);
			});
		});
	}

	private static Vec3 flatDir(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}
}
