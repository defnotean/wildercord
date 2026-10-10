package dev.wildercord.pairs.b049;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Targets;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs049 {
	private Pairs049() {}

	/**
	 * Mooring Ward: the nearest empty boat is hauled in beside you, and a mirror pocket opens over you and your allies
	 * near you. It catches the hostile shots flying at them, and gives each a shield.
	 */
	@Pair(a = "mooring_call", b = "nullcatch", name = "Mooring Ward", element = "void", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Hauls the nearest empty boat within 24 blocks to your side, if there is one. For 5 seconds you and allies "
			+ "within 3 blocks get 4 Absorption, and the first 3 hostile projectiles flying at them are caught and dropped. "
			+ "Ownerless and allied shots pass.")
	public static void mooringWard(PairCast c) {
		LivingEntity caster = c.caster;
		Vec3 flat = flatForward(c);
		Vec3 side = caster.position().add(-flat.z * 1.5, 0.1, flat.x * 1.5);
		List<LivingEntity> guarded = c.alliesNear(PairCast.mid(caster), 3);
		for (LivingEntity g : guarded) {
			c.absorb(g, 4 * c.power, 5);
		}
		AbstractBoat boat = c.level.getEntitiesOfClass(AbstractBoat.class, caster.getBoundingBox().inflate(24.0),
				b -> b.isAlive() && b.getPassengers().isEmpty() && b.distanceToSqr(caster) <= 24.0 * 24.0).stream()
			.min(Comparator.comparingDouble(b -> b.distanceToSqr(caster))).orElse(null);
		if (boat != null) {
			// The boat slides in over four beats, a rope of light drawn to the caster as it comes.
			c.every(4, 5, frame -> {
				if (!boat.isAlive()) {
					return;
				}
				Vec3 next = frame == 4 ? side : boat.position().lerp(side, 0.4);
				boat.teleportTo(next.x, next.y, next.z);
				boat.setDeltaMovement(Vec3.ZERO);
				c.line(PairCast.dust(0xE8DCC0, 0.7F), PairCast.mid(boat), PairCast.mid(caster), 2.0);
			});
			c.later(20, () -> c.sound(SoundEvents.BOAT_PADDLE_WATER, side, 0.9F, 1.1F));
		}
		// The mirror pocket: a ring of pale light turns round each guarded ally while it stands.
		int[] caught = {0};
		c.every(2, 50, frame -> {
			for (LivingEntity g : c.still(guarded)) {
				if (frame % 6 == 0) {
					c.ring(PairCast.shift(0xB8E8FF, 0xE6DCFF, 0.8F), PairCast.mid(g).add(0, 0.2, 0), 1.3, 14, frame * 0.2);
				}
				if (caught[0] >= 3) {
					continue;
				}
				for (Entity e : c.level.getEntities((Entity) null, g.getBoundingBox().inflate(2.0),
						x -> x instanceof Projectile)) {
					Projectile p = (Projectile) e;
					if (caught[0] >= 3 || !p.isAlive() || !hostileShot(c, p)) {
						continue;
					}
					if (p.getDeltaMovement().dot(PairCast.mid(g).subtract(p.position())) <= 0) {
						continue;
					}
					c.particles(ParticleTypes.ENCHANTED_HIT, p.position(), 8, 0.3, 0.1);
					c.sound(SoundEvents.SHIELD_BLOCK, p.position(), 0.8F, 1.3F);
					p.discard();
					caught[0]++;
				}
			}
		});
	}

	/**
	 * Drawn Tide: a water envelope closes round up to six enemies, soaks them and rolls them toward a landing twelve
	 * blocks along the way the cast went. Where they land, the splash hits everything close by.
	 */
	@Pair(a = "current", b = "pocket_current", name = "Drawn Tide", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 6 enemies are caught in a water envelope: 4 damage and soaked, then rolled towards a landing 12 blocks "
			+ "along the way you cast. They land with 4 more damage, and every enemy within 2 blocks of the landing, them "
			+ "included, takes 2.")
	public static void drawnTide(PairCast c) {
		List<LivingEntity> caught = PairCast.first(c.enemies(), 6);
		Vec3 flat = flatForward(c);
		Vec3 landing = c.ground(c.caster.position().add(flat.scale(12)));
		for (LivingEntity t : caught) {
			c.mark(t, Reactions.Mark.SOAKED);
			c.hurt(t, 4 * c.power);
		}
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, c.point(), 1.0F, 0.8F);
		// The envelope closes: three frames of water tightening round each.
		c.every(3, 3, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.sphere(PairCast.shift(0x6FD3FF, 0xD9FAFF, 1.0F), PairCast.mid(t), 1.2 - frame * 0.25, 20);
			}
		});
		// Then it rolls them along, with bubbles trailing.
		c.later(6, () -> c.every(3, 8, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, landing.add(0, 1.0, 0), 0.9);
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(t), 3, 0.3, 0.05);
			}
		}));
		c.later(30, () -> {
			c.ring(PairCast.shift(0xD9FAFF, 0x6FD3FF, 1.2F), landing.add(0, 0.2, 0), 2.0, 24, 0.0);
			c.sound(SoundEvents.GENERIC_SPLASH, landing, 1.0F, 0.9F);
			c.shake(landing, 0.2F, 6);
			for (LivingEntity t : c.still(caught)) {
				c.hurt(t, 4 * c.power);
			}
			for (LivingEntity e : c.enemiesNear(landing, 2)) {
				c.hurt(e, 2 * c.power);
			}
		});
	}

	/**
	 * Gulf Column: a bubble column hurls up to six enemies into the air, and they come down where they came up. Those
	 * that land on dry land are stranded and slowed; the ones that land in water take less.
	 */
	@Pair(a = "refloat", b = "upwell", name = "Gulf Column", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A bubble column hurls up to 6 enemies into the air: 3 damage as they rise. 1.2 seconds later each one is "
			+ "checked: 6 damage and Slowness II for 3 seconds if it is out of the water, 3 damage if it is in it. Each "
			+ "other enemy within 2 blocks of it takes 2.")
	public static void gulfColumn(PairCast c) {
		List<LivingEntity> lifted = PairCast.first(c.enemies(), 6);
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, c.point(), 1.0F, 0.7F);
		for (LivingEntity t : lifted) {
			c.hurt(t, 3 * c.power);
			c.lift(t, 1.0);
		}
		c.every(3, 8, frame -> {
			for (LivingEntity t : c.still(lifted)) {
				c.column(PairCast.shift(0xB8F0FF, 0x3A9AE0, 0.9F), t.position(), 0.7, 2.5, 14);
			}
		});
		c.later(24, () -> {
			for (LivingEntity t : c.still(lifted)) {
				Vec3 at = PairCast.mid(t);
				boolean wet = t.isInWater();
				c.hurt(t, (wet ? 3 : 6) * c.power);
				if (!wet) {
					c.effect(t, MobEffects.SLOWNESS, 3, 1);
				}
				c.ring(PairCast.shift(0xDDF6FF, 0x5AB0F0, 1.0F), t.position().add(0, 0.2, 0), 1.2, 16, 0.0);
				c.sound(SoundEvents.GENERIC_SPLASH, at, 0.9F, wet ? 1.2F : 0.7F);
				for (LivingEntity e : c.enemiesNear(at, 2)) {
					if (e != t) {
						c.hurt(e, 2 * c.power);
					}
				}
			}
			c.shake(c.point(), 0.2F, 6);
		});
	}

	/**
	 * Murk Lantern: allies near the spot slip into the ink, invisible for a while. Then, once a second for four seconds,
	 * a sense line finds the nearest enemy and marks it with a glow.
	 */
	@Pair(a = "inkveil", b = "shipwreck_sense", name = "Murk Lantern", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Allies within 4 blocks turn invisible: 10 seconds in water, 3 on land. For 4 seconds, once a second, the "
			+ "nearest enemy within 12 blocks glows for 2 seconds, and an ink-blue line points from you to it.")
	public static void murkLantern(PairCast c) {
		Vec3 at = c.point();
		for (LivingEntity a : c.alliesNear(at, 4)) {
			c.effect(a, MobEffects.INVISIBILITY, (a.isInWater() ? 10 : 3) * c.duration, 0);
		}
		c.sound(SoundEvents.SCULK_CLICKING, at, 0.8F, 0.6F);
		c.every(20, 4, frame -> {
			LivingEntity near = c.nearestEnemy(at, 12, null);
			if (near == null) {
				return;
			}
			c.effect(near, MobEffects.GLOWING, 2, 0);
			Vec3 to = PairCast.mid(near);
			c.line(PairCast.dust(0x2A4A7A, 0.8F), PairCast.mid(c.caster), to, 1.5);
			c.ring(PairCast.shift(0x3B6FB0, 0x8FD0FF, 0.8F), to, 1.0, 12, frame * 0.3);
			c.sound(SoundEvents.NOTE_BLOCK_BELL, to, 0.6F, 1.5F + 0.1F * frame);
		});
	}

	/**
	 * Shoal Standard: a banner of fire rallies the allies near the point: each gets Absorption, one level per ally
	 * there. For five seconds after, a ring of light turns round the point and heals them.
	 */
	@Pair(a = "morale", b = "shoal_herd", name = "Shoal Standard", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Allies within 6 blocks get Absorption for 20 seconds, one level for each ally there, you included (up to "
			+ "III). Then five times, once a second, each ally within 6 blocks heals 2, while a ring of light turns round "
			+ "the spot.")
	public static void shoalStandard(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> rallied = c.alliesNear(at, 6);
		if (rallied.isEmpty()) {
			return;
		}
		int level = Math.min(rallied.size(), 3) - 1;
		for (LivingEntity a : rallied) {
			c.effect(a, MobEffects.ABSORPTION, 20, level);
		}
		c.sound(SoundEvents.FISHING_BOBBER_SPLASH, at, 0.9F, 0.9F);
		c.every(20, 5, frame -> {
			for (LivingEntity a : c.alliesNear(at, 6)) {
				c.heal(a, 2 * c.power);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a).add(0, 0.8, 0), 2, 0.3, 0.0);
			}
			c.ring(PairCast.shift(0xFFB36B, 0x7AD7FF, 0.8F), at.add(0, 1.0, 0), 3.0, 16, frame * 0.4);
			c.particles(ParticleTypes.BUBBLE, at.add(0, 1.0, 0), 5, 2.0, 0.05);
			c.sound(SoundEvents.PLAYER_SPLASH, at, 0.5F, 1.0F + 0.1F * frame);
		});
	}

	/**
	 * Lullabrook: spores drift down over the enemies and lull them, and after a moment a spring wells up beneath each
	 * one, soaking it and striking whatever stands close.
	 */
	@Pair(a = "drowse", b = "springbed", name = "Lullabrook", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 6 enemies are lulled: Slowness III for 2 seconds and Weakness I for 4. 1.5 seconds later a spring "
			+ "wells up under each: 5 damage and soaked, and each other enemy within 2 blocks takes 2.")
	public static void lullabrook(PairCast c) {
		List<LivingEntity> lulled = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : lulled) {
			c.effect(t, MobEffects.SLOWNESS, 2, 2);
			c.effect(t, MobEffects.WEAKNESS, 4, 0);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.8F, 0.6F);
		// Spores drift down over each sleeper for a second and a half.
		c.every(5, 6, frame -> {
			for (LivingEntity t : c.still(lulled)) {
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t).add(0, 2.4 - frame * 0.35, 0), 3, 0.4, 0.02);
			}
		});
		c.later(30, () -> {
			for (LivingEntity t : c.still(lulled)) {
				Vec3 at = PairCast.mid(t);
				c.column(PairCast.shift(0x7ED8FF, 0x2A7FD0, 0.9F), t.position(), 0.6, 2.2, 16);
				c.mark(t, Reactions.Mark.SOAKED);
				c.hurt(t, 5 * c.power);
				for (LivingEntity e : c.enemiesNear(at, 2)) {
					if (e != t) {
						c.hurt(e, 2 * c.power);
					}
				}
			}
			c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, c.point(), 1.0F, 1.1F);
			c.shake(c.point(), 0.2F, 6);
		});
	}

	/**
	 * Blank Glyph: the enemies lose every good effect they carry, and each loss stings. Then the struck enemies glow,
	 * with a ring of sign-letters turning over their heads.
	 */
	@Pair(a = "nullify", b = "sign_glow", name = "Blank Glyph", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Strips every good effect from up to 6 enemies. Each effect stripped deals 1 damage to that enemy (3 at most). "
			+ "The struck enemies also glow for 4 seconds, with a ring of sign-letters turning over their heads.")
	public static void blankGlyph(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : struck) {
			int stripped = 0;
			for (MobEffectInstance e : List.copyOf(t.getActiveEffects())) {
				if (e.getEffect().value().isBeneficial()) {
					t.removeEffect(e.getEffect());
					stripped++;
				}
			}
			if (stripped > 0) {
				c.hurt(t, Math.min(stripped, 3) * c.power);
			}
			c.effect(t, MobEffects.GLOWING, 4, 0);
			c.particles(ParticleTypes.ENCHANT, PairCast.mid(t), 12, 0.4, 0.5);
		}
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 0.9F, 1.3F);
		c.every(6, 4, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 head = PairCast.mid(t).add(0, 1.1, 0);
				c.ring(PairCast.shift(0xD9C7FF, 0xFFF4B0, 0.8F), head, 0.6, 8, frame * 0.7);
				c.zigzag(PairCast.dust(0xFFF4B0, 0.7F), head.add(-0.3, 0.3, 0), head.add(0.3, 0.3, 0), 0.12, 3.0);
			}
			c.sound(SoundEvents.SCULK_CLICKING, c.point(), 0.5F, 1.6F);
		});
	}

	/**
	 * Gel Bastion: the first ally in reach is shelled in gel, damage cut by three fifths. When the shell bursts,
	 * bolts of gel seek the nearest enemies and the ones close to the shell float.
	 */
	@Pair(a = "shulkershell", b = "slime_sense", name = "Gel Bastion", element = "void", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Shells the first ally in reach (you, if alone) in gel: Resistance III, 60% less damage, for 4 seconds. When "
			+ "it bursts, 3 bolts of gel seek the nearest enemies within 8 blocks for 4 damage each, and enemies within 3 "
			+ "blocks float for 2 seconds (not bosses).")
	public static void gelBastion(PairCast c) {
		LivingEntity shelled = c.firstAlly() != null ? c.firstAlly() : c.caster;
		c.effect(shelled, MobEffects.RESISTANCE, 4, 2);
		c.sound(SoundEvents.SLIME_BLOCK_PLACE, PairCast.mid(shelled), 1.0F, 0.9F);
		// The gel pulses once a second while it holds.
		c.every(20, 4, frame -> {
			if (c.here(shelled)) {
				c.sphere(PairCast.shift(0xB9F5C8, 0x9FD8FF, 1.0F), PairCast.mid(shelled), 1.2 + 0.2 * (frame % 2), 26);
			}
		});
		c.later(80, () -> {
			if (!c.here(shelled)) {
				return;
			}
			Vec3 at = PairCast.mid(shelled);
			c.sound(SoundEvents.SLIME_SQUISH, at, 1.0F, 1.1F);
			c.shake(at, 0.15F, 5);
			c.ring(PairCast.shift(0xB9F5C8, 0x7FD0A0, 0.9F), at, 1.6, 20, 0.0);
			List<LivingEntity> bolted = PairCast.first(c.enemiesNear(at, 8), 3);
			for (int i = 0; i < bolted.size(); i++) {
				LivingEntity e = bolted.get(i);
				c.line(PairCast.shift(0xB9F5C8, 0x7FD0A0, 0.7F), at, PairCast.mid(e), 1.0);
				c.later(4 + 3 * i, () -> {
					if (c.here(e)) {
						c.hurt(e, 4 * c.power);
						c.particles(ParticleTypes.ITEM_SLIME, PairCast.mid(e), 6, 0.3, 0.1);
						c.sound(SoundEvents.SLIME_SQUISH_SMALL, PairCast.mid(e), 0.7F, 1.2F);
					}
				});
			}
			for (LivingEntity e : c.enemiesNear(at, 3)) {
				if (c.movable(e)) {
					c.effect(e, MobEffects.LEVITATION, 2, 0);
				}
			}
		});
	}

	/**
	 * Trailstride: a long stride the way you cast, stopping at walls, leaving green crumbs behind. Anything beside the
	 * path is knocked aside as you go past.
	 */
	@Pair(a = "fieldstride", b = "trailblaze", name = "Trailstride", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You dash 6 blocks the way you cast, stopping at walls, with Speed I for 4 seconds, and leave a trail of green "
			+ "crumbs. Enemies within 1.5 blocks of the path are knocked aside and take 2 damage.")
	public static void trailstride(PairCast c) {
		LivingEntity caster = c.caster;
		Vec3 flat = flatForward(c);
		Vec3 from = caster.position();
		Vec3 goal = from.add(flat.scale(6));
		HitResult wall = c.level.clip(new ClipContext(from.add(0, 0.5, 0), goal.add(0, 0.5, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 end = wall.getType() == HitResult.Type.MISS ? goal : wall.getLocation().subtract(flat.scale(0.6));
		if (end.distanceTo(from) < 0.5 || !c.blink(caster, end)) {
			return;
		}
		Vec3 landed = caster.position();
		Vec3 path = landed.subtract(from);
		c.effect(caster, MobEffects.SPEED, 4, 0);
		c.sound(SoundEvents.ARROW_SHOOT, from, 0.8F, 1.4F);
		// The crumbs fall along the path in four puffs.
		c.every(2, 4, frame -> {
			Vec3 p = from.lerp(landed, (frame + 1) / 4.0).add(0, 0.2, 0);
			c.particles(ParticleTypes.HAPPY_VILLAGER, p, 4, 0.3, 0.0);
		});
		c.later(3, () -> {
			Vec3 mid = from.lerp(landed, 0.5);
			double reach = path.length() / 2 + 1.5;
			for (LivingEntity e : c.enemiesNear(mid, reach)) {
				Vec3 at = PairCast.mid(e);
				double along = at.subtract(from).dot(path) / Math.max(path.lengthSqr(), 1.0E-4);
				Vec3 closest = from.add(path.scale(Math.max(0, Math.min(1, along))));
				if (at.distanceTo(closest) <= 1.5) {
					c.knockFrom(e, closest, 0.6, 0.3);
					c.strike(e, 2 * c.power);
					c.particles(ParticleTypes.SWEEP_ATTACK, at, 1, 0.0, 0.0);
				}
			}
		});
	}

	/**
	 * Misted Wire: up to six enemies are chilled, and a mist hangs over the point, soaking whoever it touches. When the
	 * mist clears, a current runs from enemy to enemy through the wet and the chilled.
	 */
	@Pair(a = "dewkeep", b = "frostwire", name = "Misted Wire", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Chills up to 6 enemies (Slowness II for 4 seconds). A mist 2.5 blocks out from the point soaks each enemy in "
			+ "it, three times over 3 seconds. Then a current runs through each chilled or soaked enemy within 6 blocks, "
			+ "up to 6 in all: 5 damage each, or 8 for those the mist soaked.")
	public static void mistedWire(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> wired = new ArrayList<>();
		List<LivingEntity> soaked = new ArrayList<>();
		for (LivingEntity t : PairCast.first(c.enemies(), 6)) {
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
			wired.add(t);
		}
		c.sound(SoundEvents.PLAYER_SPLASH, at, 0.9F, 0.8F);
		double reach = 2.5 * c.radius;
		c.every(20, 3, frame -> {
			c.disc(PairCast.dust(0xBFE9FF, 1.0F), at.add(0, 0.2, 0), reach, 22);
			c.particles(ParticleTypes.CLOUD, at.add(0, 1.0, 0), 4, reach / 2, 0.01);
			for (LivingEntity e : c.enemiesNear(at, reach)) {
				c.mark(e, Reactions.Mark.SOAKED);
				if (!soaked.contains(e)) {
					soaked.add(e);
				}
				if (!wired.contains(e)) {
					wired.add(e);
				}
			}
		});
		c.later(60, () -> {
			Vec3 prev = at;
			int hits = 0;
			for (LivingEntity e : wired) {
				if (hits >= PairCast.MAX_TARGETS || !c.here(e) || PairCast.mid(e).distanceTo(at) > 6) {
					continue;
				}
				Vec3 to = PairCast.mid(e);
				c.zigzag(PairCast.shift(0xE8F8FF, 0x6FD8FF, 0.7F), prev, to, 0.25, 3.0);
				c.hurt(e, (soaked.contains(e) ? 8 : 5) * c.power);
				c.particles(ParticleTypes.ELECTRIC_SPARK, to, 6, 0.3, 0.2);
				prev = to;
				hits++;
			}
			c.sound(SoundEvents.GENERIC_SPLASH, at, 1.0F, 1.5F);
			c.shake(at, 0.2F, 6);
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The horizontal way the cast went, a unit vector (east if it went straight up or down). */
	private static Vec3 flatForward(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	/** A shot from something hostile to the caster: an owned projectile, never the caster's or an ally's own. */
	private static boolean hostileShot(PairCast c, Projectile p) {
		return p.getOwner() instanceof LivingEntity owner && owner != c.caster && Targets.canHarm(c.caster, owner);
	}
}
