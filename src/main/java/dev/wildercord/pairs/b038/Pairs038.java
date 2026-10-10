package dev.wildercord.pairs.b038;

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
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs038 {
	private Pairs038() {}

	/**
	 * Lullaby Balm: the allies near the point are healed at once; two seconds later a hush settles the enemies close
	 * to them, who lose their target. The look: a green spiral rising round the point, hearts over the healed, and
	 * soft chimes as the hush falls.
	 */
	@Pair(a = "gentlehand", b = "heal", name = "Lullaby Balm", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius", "duration"},
		text = "Allies within 4 blocks heal 6 (8 allies at most). Two seconds later the enemies within 3 blocks of them (6 at "
			+ "most) lose their target for about 2 seconds, and dawdle (Slowness I).")
	public static void lullabyBalm(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> healed = PairCast.first(c.alliesNear(at, 4 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.8F, 1.4F);
		c.every(3, 3, frame -> c.spiral(PairCast.dust(0xC8FFD8, 0.9F), at, 1.2 + frame * 0.5, 1.5, 1.0, 14));
		c.later(10, () -> {
			for (LivingEntity t : c.still(healed)) {
				c.heal(t, 6 * c.power);
				c.particles(ParticleTypes.HEART, PairCast.mid(t), 3, 0.3, 0.02);
			}
			c.sphere(PairCast.dust(0xB8FFC8, 0.8F), at, 1.8, 30);
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, at, 0.9F, 1.2F);
		});
		c.later(40, () -> hush(c, c.still(healed)));
	}

	private static void hush(PairCast c, List<LivingEntity> allies) {
		List<LivingEntity> hushed = new ArrayList<>();
		for (LivingEntity ally : allies) {
			for (LivingEntity near : c.enemiesNear(PairCast.mid(ally), 3 * c.radius)) {
				if (hushed.size() < 6 && !hushed.contains(near)) {
					hushed.add(near);
				}
			}
		}
		for (LivingEntity t : c.still(hushed)) {
			c.sound(SoundEvents.NOTE_BLOCK_BELL, PairCast.mid(t), 0.5F, 1.5F);
		}
		c.every(5, 10, frame -> {
			for (LivingEntity t : c.still(hushed)) {
				if (t instanceof Mob mob && c.movable(mob)) {
					mob.setTarget(null);
				}
				c.effect(t, MobEffects.SLOWNESS, 0.3, 0);
				c.ring(PairCast.dust(0xC8FFD8, 0.7F), PairCast.mid(t), 0.8, 10, frame * 0.5);
			}
		});
	}

	/**
	 * Leafcut: a wind-borne blade opens the target along a slash and leaves it bleeding. A second and a half later the
	 * cut opens into a gale of leaves that tears at the enemies near it. The look: a dark red slash, then a green whirl
	 * of leaves widening out from the wound.
	 */
	@Pair(a = "cleave", b = "prune", name = "Leafcut", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "The target takes 6 damage and Bleeding. 1.5 seconds later the cut opens into a gale of leaves: 2 more damage "
			+ "to it, and 3 to every other enemy within 3 blocks of it.")
	public static void leafcut(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 at = PairCast.mid(t);
		c.strike(t, 6 * c.power);
		c.mark(t, Reactions.Mark.BLEEDING);
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, at, 1.0F, 1.2F);
		c.every(3, 3, frame -> {
			if (frame == 0) {
				c.line(PairCast.dust(0x8B0A14, 1.1F), at.add(-0.9, 0.9, -0.9), at.add(0.9, -0.9, 0.9), 4);
			} else if (c.here(t)) {
				c.particles(PairCast.dust(0x8B0A14, 1.0F), at, 8, 0.4, 0.05);
			}
		});
		c.later(30, () -> {
			c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.8F, 1.3F);
			c.sound(SoundEvents.AZALEA_LEAVES_BREAK, at, 1.0F, 0.9F);
			c.every(3, 4, frame -> c.ring(PairCast.dust(0x6E9B2E, 1.0F), at, 0.6 + frame * 0.7, 20, frame * 0.4));
			c.wave(PairCast.dust(0xA8D85A, 0.9F), at, 16, 0.3);
			for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
				if (near == t) {
					if (c.here(t)) {
						c.strike(t, 2 * c.power);
					}
				} else {
					c.strike(near, 3 * c.power);
					c.line(PairCast.dust(0xA8D85A, 0.8F), at, PairCast.mid(near), 3);
				}
			}
		});
	}

	/**
	 * Mustering Horn: a horn call draws the allies near the point in to it in three beats, and then they stride on
	 * with Speed and Jump Boost. The look: straw-gold rings closing inward, and a gust of gold motes on the call.
	 */
	@Pair(a = "herdcall", b = "rally", name = "Mustering Horn", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "Allies within 6 blocks (8 at most, you included) are drawn in to the point; you stay where you are. Then they "
			+ "get Speed I and Jump Boost I for 12 seconds.")
	public static void musteringHorn(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> called = PairCast.first(c.alliesNear(at, 6 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.RAID_HORN, at, 0.5F, 1.2F);
		c.every(8, 3, beat -> {
			c.ring(PairCast.dust(0xF2E3B0, 1.0F), at, (3.0 - beat * 0.9) * c.radius, 22, beat * 0.5);
			for (LivingEntity t : c.still(called)) {
				if (t != c.caster) {
					c.pullTo(t, at, 0.9);
				}
			}
		});
		c.later(24, () -> {
			for (LivingEntity t : c.still(called)) {
				c.effect(t, MobEffects.SPEED, 12, 0);
				c.effect(t, MobEffects.JUMP_BOOST, 12, 0);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 4, 0.3, 0.02);
			}
			c.wave(PairCast.dust(0xFFE08A, 1.0F), at, 20, 0.4);
			c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.7F, 1.0F);
		});
	}

	/**
	 * Woolward: wool gathers round the point and wraps the allies near it in Absorption. Then the wool sheds outward
	 * and shoves the enemies off them. The look: white tufts closing in as a ring, then a cloud bursting out with a
	 * shear snip.
	 */
	@Pair(a = "fleece", b = "shield", name = "Woolward", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Allies within 3 blocks get 4 Absorption for 8 seconds. Two seconds later the wool sheds outward: up to 6 enemies "
			+ "within 2 blocks of those allies are knocked back from the point and slowed (Slowness I) for 3 seconds.")
	public static void woolward(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> wrapped = PairCast.first(c.alliesNear(at, 3 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity t : wrapped) {
			c.absorb(t, 4 * c.power, 8);
		}
		c.sound(SoundEvents.WOOL_PLACE, at, 1.0F, 0.8F);
		c.every(4, 4, frame -> {
			c.ring(PairCast.dust(0xF7F4EC, 1.0F), at, (2.6 - frame * 0.5) * c.radius, 18, frame * 0.4);
			for (LivingEntity t : c.still(wrapped)) {
				c.particles(ParticleTypes.CLOUD, PairCast.mid(t), 2, 0.3, 0.01);
			}
		});
		c.later(40, () -> shed(c, at, c.still(wrapped)));
	}

	private static void shed(PairCast c, Vec3 at, List<LivingEntity> wrapped) {
		List<LivingEntity> shoved = new ArrayList<>();
		for (LivingEntity t : wrapped) {
			for (LivingEntity near : c.enemiesNear(PairCast.mid(t), 2 * c.radius)) {
				if (shoved.size() < 6 && !shoved.contains(near)) {
					shoved.add(near);
				}
			}
		}
		c.sound(SoundEvents.SHEARS_SNIP, at, 1.0F, 0.9F);
		c.sound(SoundEvents.WOOL_BREAK, at, 0.8F, 1.1F);
		c.particles(ParticleTypes.CLOUD, at, 24, 1.2, 0.08);
		c.wave(PairCast.dust(0xF7F4EC, 1.1F), at, 20, 0.35);
		for (LivingEntity e : shoved) {
			c.knockFrom(e, at, 0.9, 0.2);
			c.effect(e, MobEffects.SLOWNESS, 3, 0);
		}
	}

	/**
	 * Glowthorn: a glowing vine whips out from you to the enemies near the point, lashes them, and yanks them back
	 * toward you. The look: green zigzag lashes, glow spores on the yank, and thorn pricks that flare after a second.
	 */
	@Pair(a = "glowvine", b = "vinelash", name = "Glowthorn", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 4 enemies are lashed by a thorned vine: 5 damage, Slowness II for 2 seconds and Glowing for 6 seconds. "
			+ "Each is yanked toward you, then pricked by thorns a second and two seconds after the lash: 2 damage each time.")
	public static void glowthorn(PairCast c) {
		Vec3 from = c.origin();
		List<LivingEntity> lashed = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.TRIPWIRE_CLICK_ON, from, 1.0F, 1.1F);
		for (LivingEntity t : lashed) {
			c.zigzag(PairCast.dust(0x7CFF6B, 0.8F), from, PairCast.mid(t), 0.5, 3);
			c.strike(t, 5 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 2, 1);
			c.effect(t, MobEffects.GLOWING, 6, 0);
		}
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(lashed)) {
				c.pullTo(t, from, 0.7);
				c.particles(ParticleTypes.GLOW, PairCast.mid(t), 3, 0.3, 0.01);
			}
		});
		c.later(20, () -> c.every(20, 2, beat -> {
			for (LivingEntity t : c.still(lashed)) {
				c.hurt(t, 2 * c.power);
				c.particles(ParticleTypes.CRIT, PairCast.mid(t), 6, 0.3, 0.1);
				c.sound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, PairCast.mid(t), 0.6F, 1.3F);
			}
		}));
	}

	/**
	 * Sicklefall: a scythe sweeps round the point, a half-turn in four beats. Every enemy in reach is cut, and those
	 * already under 40% health are reaped, and the reaper heals from each. The look: pale green sickle arcs, then
	 * spores lifting from the reaped.
	 */
	@Pair(a = "harvest", b = "lifesteal", name = "Sicklefall", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A scythe sweep reaches 3 blocks round the point: each enemy in reach takes 2 damage. Any already under 40% health "
			+ "is reaped for 8 more, and you heal 2 for each reaped (3 at most).")
	public static void sicklefall(PairCast c) {
		Vec3 at = c.point();
		double reach = 3 * c.radius;
		List<LivingEntity> near = c.enemiesNear(at, reach);
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, at, 0.9F, 0.9F);
		c.every(4, 4, frame -> {
			double a = -Math.PI / 2 + frame * Math.PI / 3;
			Vec3 tip = at.add(Math.cos(a) * reach, 0.2, Math.sin(a) * reach);
			c.line(PairCast.dust(0xD9F5B0, 0.9F), at.add(0, 0.2, 0), tip, 3);
			if (frame == 3) {
				reap(c, near);
			}
		});
	}

	private static void reap(PairCast c, List<LivingEntity> near) {
		int reaped = 0;
		for (LivingEntity t : c.still(near)) {
			boolean low = t.getHealth() < t.getMaxHealth() * 0.4F;
			c.hurt(t, 2 * c.power);
			if (low && reaped < 3 && c.here(t)) {
				reaped++;
				c.hurt(t, 8 * c.power);
				c.heal(c.caster, 2 * c.power);
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 12, 0.4, 0.05);
			}
		}
	}

	/**
	 * Ridgeroot: a ridge of roots rises from you out to the point, drawn over three beats; then it heaves up under
	 * the enemies along its line, and shelters the allies beside it. The look: a brown root line creeping out, green
	 * shoots budding along it, and a shake when the ridge bursts.
	 */
	@Pair(a = "rampart", b = "root_bulwark", name = "Ridgeroot", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "A ridge of roots rises from you to the point over half a second. Up to 6 enemies within 1.5 blocks of its line take "
			+ "3 damage, are lifted a block and slowed (Slowness II) for 3 seconds. Allies within 2 blocks of it get Regeneration I "
			+ "for 5 seconds.")
	public static void ridgeroot(PairCast c) {
		Vec3 start = c.origin();
		Vec3 end = c.point().distanceTo(start) < 2 ? start.add(c.dir().scale(4 * c.radius)) : c.point();
		c.sound(SoundEvents.ROOTED_DIRT_BREAK, start, 0.9F, 0.8F);
		c.every(3, 4, frame -> {
			Vec3 tip = start.add(end.subtract(start).scale((frame + 1) / 4.0));
			c.line(PairCast.dust(0x7A5230, 1.1F), start.add(0, 0.2, 0), tip.add(0, 0.2, 0), 3);
			c.line(PairCast.dust(0x5FA84A, 0.8F), start.add(0, 0.9, 0), tip.add(0, 0.9, 0), 2);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, tip, 2, 0.3, 0.02);
		});
		c.later(12, () -> raise(c, start, end));
	}

	private static void raise(PairCast c, Vec3 start, Vec3 end) {
		Vec3 mid = start.add(end).scale(0.5);
		double reach = start.distanceTo(end) / 2 + 2 * c.radius;
		List<LivingEntity> heaved = new ArrayList<>();
		for (LivingEntity near : c.enemiesNear(mid, reach)) {
			if (heaved.size() < 6 && distanceToLine(PairCast.mid(near), start, end) <= 1.5 * c.radius) {
				heaved.add(near);
			}
		}
		for (LivingEntity t : heaved) {
			c.strike(t, 3 * c.power);
			c.lift(t, 1.0);
			c.effect(t, MobEffects.SLOWNESS, 3, 1);
			c.line(PairCast.dust(0x8A6A3A, 0.9F), start, PairCast.mid(t), 3);
		}
		for (LivingEntity ally : c.alliesNear(mid, reach)) {
			if (distanceToLine(PairCast.mid(ally), start, end) <= 2 * c.radius) {
				c.effect(ally, MobEffects.REGENERATION, 5, 0);
			}
		}
		c.shake(mid, 0.25F, 6);
		c.sound(SoundEvents.MANGROVE_ROOTS_BREAK, mid, 1.0F, 0.9F);
	}

	private static double distanceToLine(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double lengthSq = ab.lengthSqr();
		double f = lengthSq < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / lengthSq));
		return p.distanceTo(a.add(ab.scale(f)));
	}

	/**
	 * Tether: you are hauled across the ground toward where the spell hit, in four steps, and walls stop you. The
	 * roots at your landing throw the enemies round it outward and mend the allies. The look: a green streak behind
	 * you, and root rings bursting out of the ground on landing.
	 */
	@Pair(a = "grapple", b = "root_carry", name = "Tether", element = "life", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You are hauled up to 12 blocks toward where it hit, flat across the ground, in four steps; walls stop you. Where you "
			+ "land, enemies within 2 blocks take 4 damage and are thrown outward, and allies within 2 blocks heal 3.")
	public static void tether(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 across = new Vec3(c.point().x - self.getX(), 0, c.point().z - self.getZ());
		Vec3 way = across.length() > 12 ? across.normalize().scale(12) : across;
		Vec3 step = way.scale(0.25);
		boolean[] stopped = {false};
		c.sound(SoundEvents.LEAD_TIED, self.position(), 0.8F, 1.3F);
		c.every(2, 4, leg -> {
			if (stopped[0] || !self.isAlive()) {
				stopped[0] = true;
				return;
			}
			Vec3 from = self.position();
			Vec3 ahead = from.add(step);
			BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.5, 0), ahead.add(0, 0.5, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (wall.getType() != HitResult.Type.MISS || !c.blink(self, ahead)) {
				stopped[0] = true;
				return;
			}
			c.line(PairCast.dust(0xB7F5A0, 0.8F), from.add(0, 0.3, 0), ahead.add(0, 0.3, 0), 3);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, from, 4, 0.4, 0.02);
		});
		c.later(10, () -> {
			Vec3 at = self.position();
			c.sound(SoundEvents.ROOTED_DIRT_BREAK, at, 1.0F, 0.7F);
			c.sound(SoundEvents.MANGROVE_ROOTS_BREAK, at, 0.8F, 1.1F);
			c.every(3, 3, beat -> c.ring(PairCast.dust(0x6B4A2B, 1.0F), at.add(0, 0.1, 0), 0.8 + beat * 0.8, 18, beat * 0.3));
			for (LivingEntity near : c.enemiesNear(at, 2)) {
				c.strike(near, 4 * c.power);
				c.knockFrom(near, at, 1.0, 0.4);
			}
			for (LivingEntity ally : c.alliesNear(at, 2)) {
				c.heal(ally, 3 * c.power);
			}
		});
	}

	/**
	 * Leaf Bower: a bower of leaves spreads over the point for 7 seconds. Allies sheltering in it are steadied, and
	 * enemies that come into it are shoved out once a second. The look: a green ring closing round the point each
	 * beat, spores drifting inside, and a leaf rustle with each push.
	 */
	@Pair(a = "haven", b = "petward", name = "Leaf Bower", element = "life", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "For 7 seconds a bower of leaves covers the point out to 2.5 blocks. Allies inside get Resistance I for 4 seconds "
			+ "at a time, and enemies inside are shoved out of it once a second.")
	public static void leafBower(PairCast c) {
		Vec3 at = c.point();
		double reach = 2.5 * c.radius;
		c.sound(SoundEvents.AZALEA_LEAVES_PLACE, at, 1.0F, 0.9F);
		c.every(20, 7, beat -> {
			c.ring(PairCast.dust(0x7DBA5A, 1.0F), at, reach, 24, beat * 0.25);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, at, 6, reach * 0.8, 0.01);
			for (LivingEntity t : c.alliesNear(at, reach)) {
				c.effect(t, MobEffects.RESISTANCE, 4, 0);
			}
			for (LivingEntity e : c.enemiesNear(at, reach)) {
				c.knockFrom(e, at, 0.9, 0.25);
			}
			c.sound(SoundEvents.AZALEA_LEAVES_STEP, at, 0.8F, 1.0F + 0.1F * beat);
		});
	}

	/**
	 * Ashbloom: a seed of flame sprouts at the point and opens three petals, a second apart. Each petal burns the
	 * enemies near it, and the last heals and warms the allies. The look: an ember column, then star-shaped flame
	 * petals, and ash drifting down after.
	 */
	@Pair(a = "ancient_seed", b = "phoenix_pyre", name = "Ashbloom", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "A seed of flame sprouts at the point and opens three petals, a second apart. Each petal burns enemies within 2.5 "
			+ "blocks for 2 damage, and the first sets them alight for 3 seconds. The third heals allies within 2.5 blocks 4 and "
			+ "gives them Fire Resistance for 10 seconds.")
	public static void ashbloom(PairCast c) {
		Vec3 at = c.point();
		double reach = 2.5 * c.radius;
		c.sound(SoundEvents.CROP_PLANTED, at, 1.0F, 0.8F);
		c.column(PairCast.shift(0xFFD35A, 0xFF5A1F, 0.9F), at, 0.4, 2.0, 16);
		c.every(20, 3, petal -> {
			c.star(PairCast.shift(0xFFB347, 0xD9400F, 1.1F), at.add(0, 0.3, 0), 6 + 2 * petal, reach, petal * 0.3);
			c.sound(SoundEvents.FIRECHARGE_USE, at, 0.8F, 0.9F + 0.2F * petal);
			for (LivingEntity t : c.enemiesNear(at, reach)) {
				c.burn(t, 2 * c.power);
				if (petal == 0) {
					c.ignite(t, 3);
				}
			}
			if (petal == 2) {
				bloom(c, at, reach);
			}
		});
		c.later(50, () -> c.particles(ParticleTypes.ASH, at, 24, reach * 0.8, 0.02));
	}

	private static void bloom(PairCast c, Vec3 at, double reach) {
		for (LivingEntity ally : c.alliesNear(at, reach)) {
			c.heal(ally, 4 * c.power);
			c.effect(ally, MobEffects.FIRE_RESISTANCE, 10, 0);
			c.sphere(PairCast.dust(0xFFE9A8, 0.9F), PairCast.mid(ally), 0.9, 14);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.8F, 0.8F);
	}
}
