package dev.wildercord.pairs.b050;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. Nothing here places or breaks blocks. */
public final class Pairs050 {
	private Pairs050() {}

	/**
	 * Clutch Hour: the hens are pecked, then an egg is laid over each one and drops on it: once after half a second and
	 * once more after a second and a half. The look: a cream disc sinking down over each target, then a crack of shell
	 * particles and a wool-break sound at each drop.
	 */
	@Pair(a = "henhouse", b = "second_bell", name = "Clutch Hour", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 3 enemies are pecked for 2 magic damage at once. An egg is laid over each one and drops on it after half "
			+ "a second for 6 magic damage, then a second egg drops 1 second later for 4 more. Each drop also does 2 magic "
			+ "damage to other enemies within 2 blocks.")
	public static void clutchHour(PairCast c) {
		List<LivingEntity> hens = PairCast.first(c.enemies(), 3);
		c.sound(SoundEvents.NOTE_BLOCK_CHIME, c.point(), 0.8F, 1.6F);
		for (LivingEntity t : hens) {
			c.hurt(t, 2 * c.power);
		}
		for (int egg = 0; egg < 2; egg++) {
			final int index = egg;
			final double damage = egg == 0 ? 6 : 4;
			// The first egg is laid at once, the second a second later; each drops ten ticks after it is laid.
			c.later(20 * egg, () -> {
				c.every(2, 5, frame -> {
					for (LivingEntity t : c.still(hens)) {
						Vec3 at = PairCast.mid(t).add(0, 2.4 - 0.4 * frame, 0);
						c.disc(PairCast.dust(0xFFF4D6, 0.9F), at, 0.35, 8);
					}
				});
				c.later(10, () -> {
					for (LivingEntity t : c.still(hens)) {
						Vec3 at = PairCast.mid(t);
						c.hurt(t, damage * c.power);
						for (LivingEntity e : c.enemiesNear(at, 2 * c.radius)) {
							if (e != t) {
								c.hurt(e, 2 * c.power);
							}
						}
						c.particles(ParticleTypes.EGG_CRACK, at, 6, 0.3, 0.1);
						c.ring(PairCast.shift(0xFFF4D6, 0xF2D27A, 1.0F), at.add(0, -0.7, 0), 0.8 * c.radius, 12, 0);
					}
					c.sound(SoundEvents.WOOL_BREAK, c.point(), 0.7F, 0.9F + 0.2F * index);
				});
			});
		}
	}

	/**
	 * Triage Row: the allies in reach (you too) are lined up most hurt first, and one at a time, every half second, each
	 * is mended and given a shield. The look: a gold ring laid round you, then a spiral of light rising over each ally
	 * in turn as the chest-open sound sounds.
	 */
	@Pair(a = "chestsort", b = "worst_first", name = "Triage Row", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Up to 4 allies within 8 blocks (you too), most hurt first, are each healed 3 health (1.5 hearts) one after another, "
			+ "every half second, and each gets 4 Absorption (2 hearts) for 5 seconds.")
	public static void triageRow(PairCast c) {
		LivingEntity me = c.caster;
		List<LivingEntity> row = new ArrayList<>(c.alliesNear(PairCast.mid(me), 8 * c.radius));
		if (!row.contains(me)) {
			row.add(me);
		}
		row.sort(Comparator.comparingDouble((LivingEntity a) -> a.getHealth() / a.getMaxHealth()));
		List<LivingEntity> lined = PairCast.first(row, 4);
		Vec3 feet = me.position();
		c.sound(SoundEvents.CHEST_OPEN, feet, 0.7F, 1.2F);
		// Wind-up: the row is set out as a gold ring on the ground around you.
		c.ring(PairCast.dust(0xE8C15A, 0.8F), feet.add(0, 0.2, 0), 2.0 * c.radius, 16, 0);
		// One ally per half second, the most hurt first.
		c.every(10, lined.size(), frame -> {
			LivingEntity a = lined.get(frame);
			if (!c.here(a)) {
				return;
			}
			c.heal(a, 3 * c.power);
			c.absorb(a, 4 * c.power, 5);
			Vec3 at = PairCast.mid(a);
			c.spiral(PairCast.shift(0xE8C15A, 0xFFF4C8, 0.9F), at, 0.5, 1.6, 1, 10);
			c.particles(ParticleTypes.HAPPY_VILLAGER, at, 4, 0.4, 0.05);
			c.sound(SoundEvents.ITEM_PICKUP, at, 0.6F, 1.0F + 0.1F * frame);
		});
	}

	/**
	 * Nightseam Cloak: you turn invisible, and the enemies near you, walls or not, are blinded and lose you as their
	 * target. The look: a dark violet seam opening outward in rings, then smoke lines drawn to each enemy it reached.
	 */
	@Pair(a = "night_seam", b = "veil", name = "Nightseam Cloak", element = "void", kind = EffectKind.HARMFUL,
		traits = {"radius", "duration"},
		text = "You turn invisible for 12 seconds. Enemies within 8 blocks, walls or not, are Blinded for 2 seconds at once and "
			+ "lose you as their target; they keep off you for 5 seconds.")
	public static void nightseamCloak(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 center = PairCast.mid(me);
		double reach = 8 * c.radius;
		c.effect(me, MobEffects.INVISIBILITY, 12, 0);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, center, 0.6F, 0.5F);
		List<LivingEntity> near = c.enemiesNear(center, reach);
		for (LivingEntity e : near) {
			c.effect(e, MobEffects.BLINDNESS, 2, 0);
			clearTarget(c, e);
		}
		// Wind-up: a dark seam opens outward through the ground, a ring every quarter second.
		c.every(5, 4, frame -> c.ring(PairCast.shift(0x2A1F4A, 0x6A5ACD, 1.0F), me.position().add(0, 0.2, 0),
			reach * (frame + 1) / 4.0, 24, frame * 0.2));
		// Payoff: the seam is traced to each enemy it reached, and they are kept off you while they stay near.
		c.later(20, () -> {
			for (LivingEntity e : c.still(near)) {
				c.zigzag(PairCast.dust(0x6A5ACD, 0.7F), center, PairCast.mid(e), 0.2, 3);
				c.particles(ParticleTypes.SMOKE, PairCast.mid(e), 5, 0.3, 0.02);
			}
			c.sound(SoundEvents.BEACON_DEACTIVATE, center, 0.7F, 1.4F);
		});
		c.every(10, 11, frame -> {
			for (LivingEntity e : c.enemiesNear(center, reach)) {
				clearTarget(c, e);
			}
		});
	}

	private static void clearTarget(PairCast c, LivingEntity e) {
		if (e instanceof Mob mob && c.movable(mob) && mob.getTarget() == c.caster) {
			mob.setTarget(null);
		}
	}

	/**
	 * Bundled Stillness: for six seconds the enemies near you are slowed harder the closer they come, and the enemy
	 * missiles in that space slow to a stop. At the end the stopped ones are bundled away, and you are shielded for each.
	 * The look: a pale dome of sphere points breathing over you, and the missiles stalling in place.
	 */
	@Pair(a = "infinity", b = "packtidy", name = "Bundled Stillness", element = "void", kind = EffectKind.HARMFUL,
		traits = {"radius", "duration"},
		text = "What it strikes gets Slowness II for 6 seconds. For 6 seconds enemies within 5 blocks are slowed: Slowness III within 2 blocks, II within 3.5, I beyond. Enemy "
			+ "projectiles in that space slow to a stop, and are bundled away at the end, giving you 1 Absorption heart each, "
			+ "up to 4.")
	public static void bundledStillness(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 center = PairCast.mid(me);
		double reach = 5 * c.radius;
		Set<Projectile> caught = new LinkedHashSet<>();
		c.sound(SoundEvents.BUNDLE_INSERT, center, 0.8F, 0.9F);
		// What the spell struck is bundled first: Slowness II for 6 seconds wherever it stands.
		for (LivingEntity t : PairCast.first(c.enemies(), PairCast.MAX_TARGETS)) {
			c.effect(t, MobEffects.SLOWNESS, 6 * c.duration, 1);
		}
		// Slowness is renewed every half second, so it holds for the whole six seconds.
		c.every(10, 12, frame -> {
			for (LivingEntity e : c.enemiesNear(center, reach)) {
				double d = e.distanceTo(me);
				int amp = d < 2 ? 2 : d < 3.5 ? 1 : 0;
				c.effect(e, MobEffects.SLOWNESS, 0.6, amp);
			}
		});
		// Every tick, the enemy projectiles in reach shed most of their speed; allies' own are left alone.
		c.every(1, 120, frame -> {
			AABB box = me.getBoundingBox().inflate(reach);
			for (Entity e : c.level.getEntities((Entity) null, box, p -> p instanceof Projectile)) {
				Projectile p = (Projectile) e;
				Entity owner = p.getOwner();
				if (owner == me || (owner != null && me.isAlliedTo(owner))) {
					continue;
				}
				caught.add(p);
				p.setDeltaMovement(p.getDeltaMovement().scale(0.6));
			}
		});
		// The dome breathes over you while it holds.
		c.every(20, 6, frame -> c.sphere(PairCast.shift(0xC8D8F0, 0x6A7A90, 0.8F), center, reach, 40));
		// Payoff: the stopped missiles are bundled away, and each one shields you.
		c.later(120, () -> {
			int bundled = 0;
			for (Projectile p : caught) {
				if (p.isAlive()) {
					c.particles(ParticleTypes.CLOUD, PairCast.mid(p), 4, 0.2, 0.02);
					p.discard();
					bundled++;
				}
			}
			if (bundled > 0) {
				c.absorb(me, Math.min(8, 2 * bundled), 6);
			}
			c.sound(SoundEvents.BUNDLE_DROP_CONTENTS, center, 0.8F, 1.0F);
		});
	}

	/**
	 * Larder Rite: the allies near you are fed three times, two seconds apart, and the last feeding leaves them a steady
	 * regeneration. The look: warm amber spirals of light rising round each ally at every feeding.
	 */
	@Pair(a = "restock", b = "savor", name = "Larder Rite", element = "time", kind = EffectKind.HELPFUL,
		traits = {"power", "radius", "duration"},
		text = "Allies within 5 blocks (you too) are healed 2 health three times, at once and then every 2 seconds. After the last "
			+ "feeding they also get Regeneration I for 5 seconds.")
	public static void larderRite(PairCast c) {
		LivingEntity me = c.caster;
		List<LivingEntity> fed = new ArrayList<>(c.alliesNear(PairCast.mid(me), 5 * c.radius));
		if (!fed.contains(me)) {
			fed.add(me);
		}
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, me.position(), 0.6F, 1.2F);
		c.every(40, 3, frame -> {
			for (LivingEntity a : c.still(fed)) {
				c.heal(a, 2 * c.power);
				Vec3 at = PairCast.mid(a);
				c.spiral(PairCast.shift(0xF0B060, 0xFFF0C0, 0.9F), at, 0.6, 1.4, 1, 12);
				c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at, 3, 0.3, 0.02);
				if (frame == 2) {
					c.effect(a, MobEffects.REGENERATION, 5, 0);
				}
			}
			c.sound(SoundEvents.ITEM_PICKUP, me.position(), 0.6F, 0.8F + 0.1F * frame);
		});
	}

	/**
	 * Pocket Gap: the enemy struck floats up for a second while the enemies near it are drawn in toward its spot, then it
	 * drops back onto that spot for 10 damage. The look: a dark violet disc that narrows into the ground, a column of
	 * dust where the enemy lands, and a teleport sound on the way out.
	 */
	@Pair(a = "hollow", b = "stow", name = "Pocket Gap", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "The enemy struck floats up for 1 second (bosses are not lifted) while enemies within 3 blocks are drawn toward its "
			+ "spot. Then it drops back onto that spot for 10 magic damage.")
	public static void pocketGap(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 spot = t.position();
		Vec3 gap = PairCast.mid(t);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, gap, 0.8F, 0.6F);
		if (c.movable(t)) {
			c.effect(t, MobEffects.LEVITATION, 1, 0);
		}
		// The gap opens: a violet disc under the spot that narrows, drawing in what stands near it.
		c.every(4, 6, frame -> {
			c.disc(PairCast.shift(0x1A0B2E, 0x7A4FB0, 1.1F), spot.add(0, 0.1, 0), 1.2 * c.radius * (1 - 0.15 * frame), 20);
			for (LivingEntity e : c.enemiesNear(spot, 3 * c.radius)) {
				if (e != t) {
					c.pullTo(e, spot, 0.8);
				}
			}
		});
		// The drop: back onto the spot, where it hits.
		c.later(20, () -> {
			if (!c.here(t)) {
				return;
			}
			c.blink(t, spot);
			c.hurt(t, 10 * c.power);
			c.column(PairCast.dust(0x7A4FB0, 1.0F), spot, 0.6, 1.6, 24);
			c.sound(SoundEvents.SHULKER_BULLET_HIT, gap, 0.8F, 0.6F);
		});
	}

	/**
	 * Hush Stride: you slip up to 6 blocks along your look, stopping short of walls, and for 10 seconds the enemy mobs
	 * that were chasing you and stand more than 8 blocks away lose track of you. The look: a pale trail drawn along the
	 * path at once, then a hush of smoke where you come out.
	 */
	@Pair(a = "hollowsense", b = "softfoot", name = "Hush Stride", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"radius", "duration"},
		text = "You try to slip up to 6 blocks along your look, stopping short of walls. For 10 seconds, enemy mobs more than 8 blocks "
			+ "away that were chasing you lose track of you.")
	public static void hushStride(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 look = me.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
		Vec3 from = me.position();
		Vec3 want = from.add(flat.scale(6 * c.radius));
		BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.9, 0), want.add(0, 0.9, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, me));
		Vec3 end = wall.getType() == HitResult.Type.MISS ? want : wall.getLocation().subtract(flat.scale(0.6));
		c.sound(SoundEvents.CANDLE_EXTINGUISH, from, 0.6F, 1.5F);
		c.line(PairCast.shift(0x9FB7C9, 0xE8F4FF, 0.8F), from.add(0, 0.3, 0), end.add(0, 0.3, 0), 2);
		if (c.blink(me, c.ground(end))) {
			c.particles(ParticleTypes.SMOKE, me.position(), 10, 0.5, 0.03);
		}
		// The hush: every half second for 10 seconds, the chasers that are far off give up on you.
		c.every(10, 21, frame -> {
			AABB box = me.getBoundingBox().inflate(40);
			for (Mob mob : c.level.getEntitiesOfClass(Mob.class, box,
					m -> m instanceof Enemy && m.getTarget() == me && m.distanceTo(me) > 8)) {
				if (c.movable(mob)) {
					mob.setTarget(null);
				}
			}
		});
	}

	/**
	 * Tailwind Hoist: a leap forward and up, Slow Falling for 4 seconds, and where you land a wind glyph is left: the
	 * enemies near it are knocked back, and for a while an ally who stands on it is flung up. The look: a swirl under
	 * you while you climb, then a ring of wind on the landing spot and a flung-up spiral of light for each ally thrown.
	 */
	@Pair(a = "glidewind", b = "skyglyph", name = "Tailwind Hoist", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"radius", "duration"},
		text = "Leaps you forward and up, with Slow Falling for 4 seconds. Where you land a wind glyph is left: enemies within 3 "
			+ "blocks are knocked back and take 3 damage. Four times, two seconds apart, an ally standing within 1.2 blocks of it "
			+ "is flung up.")
	public static void tailwindHoist(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 look = me.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
		c.push(me, flat.scale(0.9 * c.radius).add(0, 0.8, 0));
		c.effect(me, MobEffects.SLOW_FALLING, 4, 0);
		c.sound(SoundEvents.WIND_CHARGE_BURST, me.position(), 0.7F, 1.0F);
		c.punch(0.2F);
		// Wind-up: a swirl under you while you climb.
		c.every(3, 8, frame -> c.spiral(PairCast.shift(0xB8E0FF, 0xFFFFFF, 0.9F), me.position().add(0, 0.2, 0), 0.6, 1.0, 1, 8));
		boolean[] landed = {false};
		// Landing: the first time you touch the ground, the glyph is written where you stand.
		c.every(2, 60, frame -> {
			if (landed[0] || frame < 3 || !me.onGround()) {
				return;
			}
			landed[0] = true;
			Vec3 glyph = me.position();
			c.ring(PairCast.shift(0xB8E0FF, 0xFFFFFF, 1.0F), glyph.add(0, 0.1, 0), 2.0 * c.radius, 24, 0);
			c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, glyph, 0.8F, 1.2F);
			for (LivingEntity e : c.enemiesNear(glyph, 3 * c.radius)) {
				c.knockFrom(e, glyph, 1.0, 0.4);
				c.strike(e, 3 * c.power);
			}
			// Four lifts, two seconds apart, for an ally who stands on the glyph.
			c.later(20, () -> c.every(40, 4, lift -> {
				for (LivingEntity a : c.alliesNear(glyph, 1.2)) {
					if (c.here(a)) {
						c.lift(a, 1.0);
						c.spiral(PairCast.dust(0xFFFFFF, 0.8F), PairCast.mid(a), 0.4, 1.0, 1, 8);
						c.sound(SoundEvents.BREEZE_JUMP, a.position(), 0.6F, 1.3F);
					}
				}
			}));
		});
	}

	/**
	 * Dampened Ember: the allies near you are put out and fire-proofed; for five seconds, once a second, the enemies
	 * close to them are blinded by the falling ash and set alight. The look: a grey ash sheet settling over the group,
	 * then white ash puffs and a small ember flare each second.
	 */
	@Pair(a = "ashen_veil", b = "snuff_out", name = "Dampened Ember", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "Allies within 6 blocks (you too) are put out and get Fire Resistance for 10 seconds. For 5 seconds, once a second, "
			+ "enemies within 2 blocks of an ally are blinded for 1 second and set alight for 2 seconds.")
	public static void dampenedEmber(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 center = PairCast.mid(me);
		double reach = 6 * c.radius;
		List<LivingEntity> ashed = new ArrayList<>(c.alliesNear(center, reach));
		if (!ashed.contains(me)) {
			ashed.add(me);
		}
		for (LivingEntity a : ashed) {
			c.douse(a);
			c.effect(a, MobEffects.FIRE_RESISTANCE, 10, 0);
		}
		c.sound(SoundEvents.FIRE_EXTINGUISH, center, 0.8F, 1.2F);
		// Wind-up: grey ash sheets down over the group.
		c.every(4, 4, frame -> c.disc(PairCast.shift(0xC8C8C8, 0x9A9A9A, 1.0F), center.add(0, 1.8 - 0.4 * frame, 0),
			reach * 0.6, 24));
		// Each second: the ash blinds and lights the enemies that stand close to an ally.
		c.every(20, 5, frame -> {
			for (LivingEntity a : c.still(ashed)) {
				Vec3 at = PairCast.mid(a);
				for (LivingEntity e : c.enemiesNear(at, 2 * c.radius)) {
					c.effect(e, MobEffects.BLINDNESS, 1, 0);
					c.ignite(e, 2);
				}
				c.particles(ParticleTypes.WHITE_ASH, at, 6, 0.5, 0.05);
				c.particles(ParticleTypes.SMALL_FLAME, at, 2, 0.3, 0.02);
			}
			c.sound(SoundEvents.FIRECHARGE_USE, center, 0.5F, 1.0F + 0.1F * frame);
		});
	}

	/**
	 * Watchful Herd: the animals and mounts near you get Resistance II and Fire Resistance, and are called in toward you
	 * three times, a second apart. Enemies near them are knocked back at each calling. The look: a violet spiral rising
	 * round every animal called in, and a gust ring at each calling.
	 */
	@Pair(a = "beastguard", b = "village_sense", name = "Watchful Herd", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "Animals, mounts and pets within 10 blocks get Resistance II and Fire Resistance for 10 seconds, and are called in "
			+ "toward you three times, a second apart. Enemies within 3 blocks of an animal are knocked back and take 2 damage "
			+ "at each calling.")
	public static void watchfulHerd(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 center = PairCast.mid(me);
		double reach = 10 * c.radius;
		List<LivingEntity> herd = new ArrayList<>();
		for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, me.getBoundingBox().inflate(reach),
				x -> (x instanceof Animal || x instanceof AbstractHorse) && !(x instanceof Enemy))) {
			herd.add(e);
		}
		List<LivingEntity> near = PairCast.first(herd, PairCast.MAX_TARGETS);
		for (LivingEntity a : near) {
			c.effect(a, MobEffects.RESISTANCE, 10, 1);
			c.effect(a, MobEffects.FIRE_RESISTANCE, 10, 0);
		}
		c.sound(SoundEvents.HORSE_AMBIENT, center, 0.7F, 1.1F);
		c.every(20, 3, frame -> {
			for (LivingEntity a : c.still(near)) {
				Vec3 at = PairCast.mid(a);
				c.pullTo(a, me.position(), 1.0);
				c.spiral(PairCast.shift(0xB5A0FF, 0xF0E6FF, 0.9F), at, 0.5, 1.4, 1, 10);
				for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
					c.knockFrom(e, a.position(), 1.0, 0.3);
					c.strike(e, 2 * c.power);
				}
			}
			c.ring(PairCast.dust(0xB5A0FF, 1.0F), center.add(0, -0.8, 0), reach * (frame + 1) / 3.0, 24, 0);
			c.sound(SoundEvents.NOTE_BLOCK_BELL, center, 0.6F, 1.0F + 0.2F * frame);
		});
	}
}
