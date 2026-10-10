package dev.wildercord.pairs.b048;

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
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs048 {
	private Pairs048() {}

	/**
	 * Sediment Keep: stone strata build up round each ally in three layers, then the ally is wrapped in absorption, and
	 * every blow that costs them health is paid back a second later. The look: grey-brown rings climbing each ally one
	 * layer at a time, then a pale stone shell and soft mending hearts.
	 */
	@Pair(a = "aftercare", b = "strata_rise", name = "Sediment Keep", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Allies within 3 blocks (8 at most) get 3 hearts of absorption for 12 seconds. For 15 seconds, each blow that "
			+ "costs an ally health heals them 1 a second later, 6 in all.")
	public static void aftercareStrata(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> kept = PairCast.first(c.alliesNear(at, 3), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.BASALT_PLACE, at, 0.9F, 0.7F);
		c.every(4, 3, layer -> {
			for (LivingEntity t : c.still(kept)) {
				c.ring(PairCast.dust(0x9C948A, 1.0F), PairCast.mid(t).add(0, -0.8 + layer * 0.8, 0), 0.9, 14, layer * 0.5);
			}
		});
		c.later(14, () -> {
			for (LivingEntity t : c.still(kept)) {
				c.absorb(t, 6 * c.power, 12);
				c.disc(PairCast.dust(0xD9D2C5, 0.9F), PairCast.mid(t), 1.0, 14);
			}
			c.sound(SoundEvents.AMETHYST_CLUSTER_PLACE, at, 0.9F, 0.8F);
			watchBlows(c, kept);
		});
	}

	/** The aftercare half: a blow that costs an ally health pays 1 back a second later, 6 in all for each ally. */
	private static void watchBlows(PairCast c, List<LivingEntity> kept) {
		double[] last = new double[kept.size()];
		double[] promised = new double[kept.size()];
		for (int i = 0; i < kept.size(); i++) {
			last[i] = kept.get(i).getHealth();
		}
		c.every(4, 75, frame -> {
			for (int i = 0; i < kept.size(); i++) {
				LivingEntity ally = kept.get(i);
				if (!c.here(ally)) {
					continue;
				}
				if (ally.getHealth() < last[i] && promised[i] < 6 * c.power) {
					promised[i] += c.power;
					c.later(20, () -> {
						if (c.here(ally)) {
							c.heal(ally, c.power);
							c.particles(ParticleTypes.HEART, PairCast.mid(ally), 1, 0.3, 0.0);
						}
					});
				}
				last[i] = ally.getHealth();
			}
		});
	}

	/**
	 * Furrow Sap: a patch of ground is ploughed in rows, again and again for ten seconds, and sap runs in the furrows.
	 * The look: brown furrow lines drawn across the patch every two seconds, green sap motes rising off the allies
	 * standing in it.
	 */
	@Pair(a = "sapflow", b = "tillage", name = "Furrow Sap", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"radius"},
		text = "Furrows are drawn across a 5-block patch at the point five times, two seconds apart. Allies within 2.5 blocks "
			+ "of the point (8 at most) get Regeneration I for 8 seconds each time they are drawn.")
	public static void sapflowTillage(PairCast c) {
		Vec3 at = c.point();
		double half = 2.5 * c.radius;
		c.sound(SoundEvents.HOE_TILL, at, 1.0F, 0.9F);
		c.every(40, 5, drawing -> {
			for (int row = -2; row <= 2; row++) {
				double z = at.z + row * half / 2;
				Vec3 west = new Vec3(at.x - half, at.y + 0.1, z);
				Vec3 east = new Vec3(at.x + half, at.y + 0.1, z);
				c.line(PairCast.dust(0x6B4A2B, 0.9F), west, east, 3);
			}
			for (LivingEntity t : PairCast.first(c.alliesNear(at, half), PairCast.MAX_TARGETS)) {
				c.effect(t, MobEffects.REGENERATION, 8, 0);
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 4, 0.3, 0.02);
			}
			c.sound(SoundEvents.GRASS_PLACE, at, 0.6F, 1.1F);
		});
	}

	/**
	 * Mole Step: you burrow to the point as a mole would, up to 16 blocks, and come up there. Two seconds later the
	 * ground you came up on bursts. The look: a dark violet spiral sinking under you, a trail of sparks, then a grey
	 * burst that throws the enemies round the spot up into the air.
	 */
	@Pair(a = "tunnel", b = "warp_step", name = "Mole Step", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You step to the point (16 blocks at most; if the spot is unsafe you stay where you are). Two seconds after you "
			+ "arrive, the ground bursts: enemies within 3 blocks take 5 damage and are thrown up.")
	public static void tunnelWarpStep(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 to = c.point();
		Vec3 off = to.subtract(from);
		if (off.length() > 16) {
			to = from.add(off.normalize().scale(16));
		}
		// The burrow stops at the first wall in the way, never through it.
		BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.5, 0), to.add(0, 0.5, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			to = wall.getLocation().subtract(to.subtract(from).normalize().scale(0.6));
		}
		Vec3 target = to;
		c.sound(SoundEvents.SCULK_CLICKING, from, 1.0F, 0.6F);
		c.spiral(PairCast.dust(0x3B1E5C, 1.0F), from, 1.0, 1.6, 1.5, 16);
		c.later(6, () -> {
			c.blink(self, target);
			Vec3 arrival = self.position();
			c.line(PairCast.dust(0x6A4C9C, 0.9F), from.add(0, 0.2, 0), arrival.add(0, 0.2, 0), 2);
			c.particles(ParticleTypes.SMOKE, arrival, 8, 0.3, 0.02);
			c.later(40, () -> moleBurst(c, arrival));
		});
	}

	private static void moleBurst(PairCast c, Vec3 at) {
		c.sound(SoundEvents.LAVA_POP, at, 1.0F, 0.6F);
		c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.6F, 0.8F);
		c.column(PairCast.dust(0x6A4C9C, 1.2F), at, 1.2, 2.5, 20);
		c.every(3, 3, puff -> c.ring(PairCast.dust(0x9A9AA8, 1.0F), at.add(0, 0.2, 0), 1.0 + puff * 1.2, 20, puff * 0.3));
		for (LivingEntity t : PairCast.first(c.enemiesNear(at, 3), PairCast.MAX_TARGETS)) {
			c.strike(t, 5 * c.power);
			c.lift(t, 0.9);
		}
	}

	/**
	 * Lodestone Gather: the enemies you hit are bled into one thread, drawn in to their middle, and the vein bursts
	 * there. The look: red threads linking them, the threads tightening to a point, then a dark crimson burst.
	 */
	@Pair(a = "blood_thread", b = "vein", name = "Lodestone Gather", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 4 enemies you hit take 2 damage each and are threaded together. For 1.5 seconds they are drawn to their "
			+ "middle, then the vein bursts there: 6 damage and Bleeding to every enemy within 2.5 blocks (8 at most).")
	public static void bloodVein(PairCast c) {
		List<LivingEntity> threaded = PairCast.first(c.enemies(), 4);
		if (threaded.isEmpty()) {
			return;
		}
		for (LivingEntity t : threaded) {
			c.wither(t, 2 * c.power);
		}
		c.sound(SoundEvents.SCULK_CLICKING, c.point(), 1.0F, 0.6F);
		c.every(5, 6, pull -> {
			List<LivingEntity> now = c.still(threaded);
			if (now.isEmpty()) {
				return;
			}
			Vec3 middle = centre(now);
			for (LivingEntity t : now) {
				c.line(PairCast.dust(0x8B0A14, 1.0F), PairCast.mid(t), middle, 3);
				c.pullTo(t, middle, 0.8);
			}
		});
		c.later(30, () -> {
			List<LivingEntity> now = c.still(threaded);
			Vec3 middle = now.isEmpty() ? c.point() : centre(now);
			c.sound(SoundEvents.LODESTONE_HIT, middle, 1.0F, 0.7F);
			c.sphere(PairCast.dust(0x8B0A14, 1.2F), middle, 1.6, 30);
			c.every(3, 3, ring -> c.ring(PairCast.dust(0x6E0A12, 1.0F), middle, 0.8 + ring * 0.9, 20, ring * 0.3));
			for (LivingEntity t : PairCast.first(c.enemiesNear(middle, 2.5 * c.radius), PairCast.MAX_TARGETS)) {
				c.wither(t, 6 * c.power);
				c.mark(t, Reactions.Mark.BLEEDING);
				c.line(PairCast.dust(0xB0161F, 0.9F), middle, PairCast.mid(t), 3);
			}
		});
	}

	/** The middle of several bodies. */
	private static Vec3 centre(List<LivingEntity> bodies) {
		Vec3 sum = Vec3.ZERO;
		for (LivingEntity b : bodies) {
			sum = sum.add(PairCast.mid(b));
		}
		return sum.scale(1.0 / bodies.size());
	}

	/**
	 * Honeyed Wake: the allies near the point lose their Poison at once, then three petal rings roll out over them,
	 * each healing whoever it passes. The look: gold rings widening, sweet motes drifting down, honey-coloured sparkle.
	 */
	@Pair(a = "honeydew", b = "wildflower", name = "Honeyed Wake", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 4 blocks (8 at most) lose Poison, and three petal rings, 0.75 seconds apart, each heal them 2 "
			+ "(6 in all).")
	public static void honeydewWildflower(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> near = PairCast.first(c.alliesNear(at, 4 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity t : near) {
			t.removeEffect(MobEffects.POISON);
		}
		c.sound(SoundEvents.HONEY_DRINK, at, 0.9F, 1.3F);
		c.every(15, 3, petals -> {
			double spread = 1.2 + petals * 1.4;
			c.ring(PairCast.dust(0xF2C94C, 1.0F), at, spread * c.radius, 22, petals * 0.4);
			c.particles(ParticleTypes.HAPPY_VILLAGER, at, 6, spread * 0.5, 0.02);
			for (LivingEntity t : c.still(near)) {
				c.heal(t, 2 * c.power);
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 3, 0.3, 0.02);
			}
		});
	}

	/**
	 * Scalding Arc: a surge of steam arcs from you to the point, and the enemies it struck are scalded. A second later
	 * the surge pools at the point and scalds whoever is still in it. The look: a pale hot arc over the line, then a
	 * grey pool that rings out and pops.
	 */
	@Pair(a = "basinfill", b = "boiling_surge", name = "Scalding Arc", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Each enemy it hit takes 5 fire damage and 2 seconds of Weakness. A second later the surge pools at the point: "
			+ "2 more fire damage to every enemy within 2 blocks of it (8 at most).")
	public static void basinSurge(PairCast c) {
		Vec3 from = c.origin();
		Vec3 at = c.point();
		List<LivingEntity> hit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.every(4, 3, lift -> c.arc(PairCast.dust(0xE8F4FF, 0.8F), from, at, 1.0 + lift * 0.6, 16));
		for (LivingEntity t : c.still(hit)) {
			c.burn(t, 5 * c.power);
			c.effect(t, MobEffects.WEAKNESS, 2, 0);
			c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, PairCast.mid(t), 6, 0.3, 0.02);
		}
		c.sound(SoundEvents.GENERIC_BURN, at, 0.8F, 1.2F);
		c.later(20, () -> {
			c.sound(SoundEvents.LAVA_POP, at, 1.0F, 1.2F);
			c.every(3, 3, ring -> c.ring(PairCast.dust(0xC8D8E8, 1.0F), at, 0.8 + ring * 0.6, 18, ring * 0.5));
			for (LivingEntity t : PairCast.first(c.enemiesNear(at, 2 * c.radius), PairCast.MAX_TARGETS)) {
				c.burn(t, 2 * c.power);
			}
		});
	}

	/**
	 * Dewfall: the allies near the point are put out if they burn, and for eight seconds dew falls on them. The look:
	 * fine blue drops falling onto each ally, a soft ring of spray each second.
	 */
	@Pair(a = "brimming", b = "dew_drink", name = "Dewfall", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Allies within 4 blocks (8 at most) are put out if they're burning. For 8 seconds, once a second, the dew heals "
			+ "each of them 1 (8 in all).")
	public static void brimmingDew(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> wet = PairCast.first(c.alliesNear(at, 4), PairCast.MAX_TARGETS);
		for (LivingEntity t : wet) {
			c.douse(t);
		}
		c.sound(SoundEvents.BOTTLE_FILL, at, 0.8F, 1.1F);
		int seconds = Math.max(1, (int) Math.round(8 * c.duration));
		c.every(20, seconds, second -> {
			for (LivingEntity t : c.still(wet)) {
				c.heal(t, c.power);
				c.particles(ParticleTypes.DRIPPING_WATER, PairCast.mid(t).add(0, 2.2, 0), 6, 0.4, 0.0);
			}
			c.ring(PairCast.dust(0x9FD8FF, 0.9F), at, 2.5, 18, second * 0.4);
			c.sound(SoundEvents.GENERIC_SPLASH, at, 0.5F, 1.4F);
		});
	}

	/**
	 * Hoar Ward: the allies near the point get Regeneration II, and for eight seconds the ground round them frosts over:
	 * each second, every enemy that comes within 2 blocks of an ally is frozen stiff. The look: pale hoarfrost rings
	 * round each ally, snowflakes on each frozen enemy.
	 */
	@Pair(a = "dewcatch", b = "frostbloom", name = "Hoar Ward", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 3 blocks (8 at most) get Regeneration II for 5 seconds. For 8 seconds, once a second, each enemy "
			+ "within 2 blocks of an ally (6 at most) takes 1 cold damage and Slowness III for 2 seconds.")
	public static void dewcatchFrostbloom(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> ward = PairCast.first(c.alliesNear(at, 3 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.POWDER_SNOW_PLACE, at, 0.9F, 1.4F);
		for (LivingEntity t : c.still(ward)) {
			c.effect(t, MobEffects.REGENERATION, 5, 1);
		}
		c.every(20, 8, second -> {
			List<LivingEntity> chilled = new ArrayList<>();
			for (LivingEntity ally : c.still(ward)) {
				for (LivingEntity foe : c.enemiesNear(PairCast.mid(ally), 2)) {
					if (chilled.size() < 6 && !chilled.contains(foe)) {
						chilled.add(foe);
					}
				}
				c.ring(PairCast.dust(0xE6F7FF, 0.8F), PairCast.mid(ally).add(0, -0.9, 0), 2.0, 16, second * 0.3);
			}
			for (LivingEntity foe : c.still(chilled)) {
				c.freeze(foe, c.power);
				c.effect(foe, MobEffects.SLOWNESS, 2, 2);
				c.particles(ParticleTypes.SNOWFLAKE, PairCast.mid(foe), 5, 0.4, 0.02);
			}
		});
	}

	/**
	 * Lily Sweep: you dash toward the point along a clear path, stopped by walls. Each enemy you pass within 1.5 blocks
	 * is struck once and knocked aside, and where you stop, lily pads spread out on the ground. The look: a green
	 * wake of petals and spores along the path, a splash of pads at the stop.
	 */
	@Pair(a = "bloomstep", b = "lily_path", name = "Lily Sweep", element = "life", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You dash up to 10 blocks toward the point, stopped by walls. Each enemy you pass within 1.5 blocks takes 3 "
			+ "damage and is knocked aside, once. Where you stop, lily pads spread round you for a moment.")
	public static void bloomstepLilyPath(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 aim = c.point().subtract(from);
		Vec3 dir = aim.lengthSqr() < 1.0E-4 ? c.dir() : aim.normalize();
		Vec3 end = from.add(dir.scale(10));
		BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.5, 0), end.add(0, 0.5, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			end = wall.getLocation().subtract(dir.scale(0.6));
		}
		Vec3 stop = end;
		List<LivingEntity> struck = new ArrayList<>();
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, from, 0.9F, 1.3F);
		c.every(3, 6, step -> {
			Vec3 before = from.lerp(stop, step / 6.0);
			Vec3 next = from.lerp(stop, (step + 1) / 6.0);
			if (!c.blink(self, next)) {
				return;
			}
			c.line(PairCast.dust(0x9BE37A, 0.9F), before.add(0, 0.2, 0), next.add(0, 0.2, 0), 3);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, next, 3, 0.3, 0.02);
			Vec3 body = PairCast.mid(self);
			for (LivingEntity foe : c.enemiesNear(body, 1.5)) {
				if (!struck.contains(foe)) {
					struck.add(foe);
					c.strike(foe, 3 * c.power);
					c.knockFrom(foe, body, 0.8, 0.3);
				}
			}
		});
		c.later(20, () -> {
			Vec3 spot = self.position();
			c.sound(SoundEvents.LILY_PAD_PLACE, spot, 1.0F, 1.0F);
			c.disc(PairCast.dust(0x3E9A3E, 0.9F), spot.add(0, 0.1, 0), 1.6, 14);
			c.later(8, () -> c.ring(PairCast.dust(0x7FD35A, 0.8F), spot.add(0, 0.1, 0), 2.4, 18, 0.2));
		});
	}

	/**
	 * Curdle Veil: milk curdles over the enemies it hits and those near the point. They choke on it, then the curd sets
	 * and they stiffen. You douse your own fire and heal for each one caught. The look: white drops falling in a ring,
	 * a pale veil round each enemy, then a cold crumble.
	 */
	@Pair(a = "milkmaid", b = "restore", name = "Curdle Veil", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "The enemies it hit, and those within 3 blocks of the point (6 at most in all), take 3 damage and Nausea for 4 seconds. A second "
			+ "and a half later the curd sets: 2 more cold damage and Slowness II for 3 seconds. You put out your fire and "
			+ "heal 1 for each enemy caught (4 at most).")
	public static void milkmaidRestore(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> caught = new ArrayList<>(PairCast.first(c.enemies(), 6));
		for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
			if (caught.size() < 6 && !caught.contains(near)) {
				caught.add(near);
			}
		}
		c.sound(SoundEvents.COW_MILK, at, 1.0F, 1.1F);
		c.every(4, 3, pour -> c.ring(PairCast.dust(0xF6F1E4, 1.0F), at.add(0, 2.4 - pour * 0.8, 0), 2.0, 16, pour * 0.4));
		for (LivingEntity t : c.still(caught)) {
			c.hurt(t, 3 * c.power);
			c.effect(t, MobEffects.NAUSEA, 4, 0);
		}
		c.douse(c.caster);
		c.heal(c.caster, Math.min(4, caught.size()) * c.power);
		c.later(30, () -> {
			for (LivingEntity t : c.still(caught)) {
				c.freeze(t, 2 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 3, 1);
				c.disc(PairCast.dust(0xDDEEFF, 0.8F), PairCast.mid(t), 0.9, 12);
				c.particles(ParticleTypes.CLOUD, PairCast.mid(t), 5, 0.4, 0.02);
			}
			c.sound(SoundEvents.POWDER_SNOW_STEP, at, 0.9F, 0.8F);
		});
	}
}
