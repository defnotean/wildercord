package dev.wildercord.pairs.b047;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs (group 7): each has its own mechanic, look, sounds and rule text. */
public final class Pairs047 {
	private Pairs047() {}

	/**
	 * Plumb Lodestone: a plumb bob drops onto each ally and anchors it, then loose drops come sliding in to it. The look:
	 * a grey line falling to brass-dotted feet, a brass ring when it lands, and item drops drawn along the ground.
	 */
	@Pair(a = "lodepull", b = "plumbline", name = "Plumb Lodestone", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "A plumb bob drops onto up to 3 allies and anchors them: Resistance I for 8 seconds. For the same 8 seconds, "
			+ "loose item drops within 6 blocks of each of them drift to it.")
	public static void plumbLodestone(PairCast c) {
		List<LivingEntity> anchored = PairCast.first(c.allies(), 3);
		int hold = c.ticks(8);
		ParticleOptions lead = PairCast.dust(0x5E6A72, 1.0F);
		ParticleOptions brass = PairCast.shift(0xF2C14E, 0x8A6A1E, 0.8F);
		c.sound(SoundEvents.CHAIN_PLACE, c.point(), 0.8F, 0.6F);
		// Wind-up: the bob slides down its line onto each ally in four steps, three ticks apart.
		c.every(3, 4, frame -> {
			for (LivingEntity a : c.still(anchored)) {
				Vec3 feet = a.position();
				Vec3 bob = feet.add(0, 0.5 + 5.0 * (3 - frame) / 3.0, 0);
				c.line(lead, bob, feet, 2);
				c.particles(brass, bob, 2, 0.05, 0);
			}
		});
		// Payoff, 12 ticks in: the anchor holds, and the drops start to come in.
		c.later(12, () -> {
			for (LivingEntity a : c.still(anchored)) {
				c.effect(a, MobEffects.RESISTANCE, 8, 0);
				c.ring(brass, a.position().add(0, 0.1, 0), 1.2, 16, 0);
				c.sound(SoundEvents.ANVIL_LAND, PairCast.mid(a), 0.4F, 1.6F);
			}
			c.every(5, Math.max(1, hold / 5), step -> {
				for (LivingEntity a : c.still(anchored)) {
					Vec3 middle = PairCast.mid(a);
					for (ItemEntity drop : c.level.getEntitiesOfClass(ItemEntity.class, a.getBoundingBox().inflate(6),
							ItemEntity::isAlive)) {
						Vec3 towards = middle.subtract(drop.position());
						if (towards.length() > 0.8) {
							drop.setDeltaMovement(towards.normalize().scale(0.3));
						}
					}
				}
			});
		});
	}

	/**
	 * Gold Fuse: each target is primed with a fuse of gold sparks that climb its body, then bursts in a ball of fire and
	 * lights up everything caught in it. The look: sparks rising, a gold sphere at the burst, smoke after.
	 */
	@Pair(a = "primer", b = "prospect", name = "Gold Fuse", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 4 enemies are primed with a fuse for 2 seconds, then each bursts: 6 damage to every enemy within 3 blocks, "
			+ "Glowing for 10 seconds, and fire for 2 seconds on each. An enemy caught by more than one burst takes it once.")
	public static void goldFuse(PairCast c) {
		List<LivingEntity> primed = PairCast.first(c.enemies(), 4);
		double r = 3 * c.radius;
		int fuse = 40;
		ParticleOptions gold = PairCast.shift(0xFFE38A, 0xFF6A1A, 0.9F);
		ParticleOptions smoke = PairCast.dust(0x3A3A3A, 1.1F);
		Set<LivingEntity> burst = new HashSet<>();
		c.sound(SoundEvents.FIRECHARGE_USE, c.point(), 0.7F, 1.4F);
		// The fuse: sparks climb each target's body, eight steps five ticks apart.
		c.every(5, 8, frame -> {
			for (LivingEntity t : c.still(primed)) {
				double h = t.getBbHeight() * (frame + 1) / 8.0;
				c.particles(gold, t.position().add(0, h, 0), 3, 0.2, 0.02);
			}
		});
		// The burst, when the fuse runs out.
		c.later(fuse, () -> {
			for (LivingEntity t : c.still(primed)) {
				Vec3 at = PairCast.mid(t);
				c.particles(ParticleTypes.EXPLOSION, at, 1, 0, 0);
				c.sphere(gold, at, r, 28);
				c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.6F, 1.3F);
				for (LivingEntity near : c.enemiesNear(at, r)) {
					if (burst.add(near)) {
						c.hurt(near, 6 * c.power);
						c.ignite(near, 2);
						c.effect(near, MobEffects.GLOWING, 10, 0);
					}
				}
			}
		});
		// Aftermath: the smoke hangs over where the bursts were.
		c.later(fuse + 8, () -> {
			for (LivingEntity t : c.still(primed)) {
				c.particles(smoke, PairCast.mid(t), 14, r * 0.4, 0.03);
			}
		});
	}

	/**
	 * Cane Snare: reed stalks grow out of the ground round each target and draw it in, cutting its grip. The look:
	 * green stalks rising in steps, then a cut of pale sparks as they pull the enemies to the point.
	 */
	@Pair(a = "disarm", b = "reed_cut", name = "Cane Snare", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Reed stalks grow up round up to 3 enemies and draw them in towards the point: 3 damage each and Weakness II for "
			+ "4 seconds (their grip is cut). Bosses take the damage but are never pulled.")
	public static void caneSnare(PairCast c) {
		List<LivingEntity> snared = PairCast.first(c.enemies(), 3);
		Vec3 at = c.point();
		ParticleOptions reed = PairCast.dust(0x7FB069, 0.9F);
		ParticleOptions cut = PairCast.shift(0xE9F5C9, 0x4E7A3A, 0.7F);
		c.sound(SoundEvents.GRASS_PLACE, at, 0.8F, 0.9F);
		// Wind-up: stalks grow out of the ground round each target, taller at every step.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(snared)) {
				c.column(reed, t.position(), 1.0, 1.2 + frame * 0.6, 12);
			}
		});
		// Payoff, 12 ticks in: the stalks close, cut the grip and pull.
		c.later(12, () -> {
			for (LivingEntity t : c.still(snared)) {
				c.effect(t, MobEffects.WEAKNESS, 4, 1);
				c.strike(t, 3 * c.power);
				c.pullTo(t, at, 1.2);
				c.ring(cut, PairCast.mid(t), 0.9, 12, 0);
				c.particles(ParticleTypes.CRIT, PairCast.mid(t), 6, 0.3, 0.1);
			}
			c.sound(SoundEvents.GRASS_BREAK, at, 0.9F, 0.7F);
			c.shake(at, 0.15F, 6);
		});
		// Aftermath: the cut stalks lie flat on the ground.
		c.later(24, () -> c.disc(reed, at, 2.0, 20));
	}

	/**
	 * Relic Rot: targets unravel in four wounds while they glow, and the last wound shatters into dust, the way a buried
	 * relic gives up its sand. The look: violet pings on each wound, a sand burst on the last.
	 */
	@Pair(a = "entropy", b = "relic_sense", name = "Relic Rot", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 3 enemies glow for 6 seconds and unravel in four wounds, one a second: 1, 2, 2 then 3 magic damage "
			+ "(through armour). The last wound shatters into a burst of sand.")
	public static void relicRot(PairCast c) {
		List<LivingEntity> rotting = PairCast.first(c.enemies(), 3);
		double[] wounds = {1, 2, 2, 3};
		ParticleOptions sand = PairCast.dust(0xD9B56B, 1.0F);
		ParticleOptions rot = PairCast.shift(0x6A4C9C, 0x0E0A1A, 1.0F);
		for (LivingEntity t : rotting) {
			c.effect(t, MobEffects.GLOWING, 6, 0);
		}
		c.sound(SoundEvents.BRUSH_SAND, c.point(), 0.8F, 0.8F);
		c.every(20, wounds.length, frame -> {
			for (LivingEntity t : c.still(rotting)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, wounds[frame] * c.power);
				c.particles(rot, at, 6, 0.3, 0.03);
				c.sound(SoundEvents.BRUSH_SAND, at, 0.5F, 1.0F + 0.2F * frame);
				if (frame == wounds.length - 1) {
					c.sphere(sand, at, 1.2, 24);
					c.sound(SoundEvents.SAND_BREAK, at, 0.8F, 0.9F);
				}
			}
		});
	}

	/**
	 * Dune Ward: the worst harmful effect on each ally is lifted by a gust, then a sandbank rises round it and shoves
	 * back what comes near. The look: a gust spiralling up each ally, sand rings growing out from its feet.
	 */
	@Pair(a = "sandbar", b = "shrug_off", name = "Dune Ward", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"radius"},
		text = "Up to 3 allies lose the harmful effect with the most time left. Then a sandbank rises round each, and enemies "
			+ "within 2 blocks of an ally are knocked back from it.")
	public static void duneWard(PairCast c) {
		List<LivingEntity> warded = PairCast.first(c.allies(), 3);
		double reach = 2 * c.radius;
		ParticleOptions sand = PairCast.dust(0xE3C78A, 1.0F);
		ParticleOptions gust = PairCast.shift(0xE8F4F8, 0xB8C4CC, 0.8F);
		c.sound(SoundEvents.WIND_CHARGE_BURST, c.point(), 0.8F, 1.1F);
		// Beat one: the gust takes the worst harm off each ally and spirals up away from it.
		for (LivingEntity a : c.still(warded)) {
			MobEffectInstance worst = null;
			for (MobEffectInstance e : List.copyOf(a.getActiveEffects())) {
				if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
						&& (worst == null || e.getDuration() > worst.getDuration())) {
					worst = e;
				}
			}
			if (worst != null) {
				a.removeEffect(worst.getEffect());
			}
			c.spiral(gust, a.position(), 0.9, 2.0, 2, 14);
		}
		// Beat two, 8 ticks in: the sandbank rises in three rings, and the enemies near each ally are shoved back.
		c.later(8, () -> {
			for (LivingEntity a : c.still(warded)) {
				c.every(4, 3, frame -> c.ring(sand, a.position().add(0, 0.1, 0), reach * (frame + 1) / 3.0, 20, frame * 0.3));
				for (LivingEntity e : c.enemiesNear(a.position(), reach)) {
					c.knockFrom(e, a.position(), 0.6, 0.25);
				}
			}
			c.sound(SoundEvents.SAND_PLACE, c.point(), 0.7F, 0.9F);
		});
	}

	/**
	 * Verdant Surge: sprouts push up round each ally and it blooms: regeneration, a heal, and the allies close by
	 * catch a lesser regeneration too. The look: green columns rising, pink hearts and leaves bursting out.
	 */
	@Pair(a = "bloom", b = "saplingrise", name = "Verdant Surge", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Up to 3 allies get Regeneration II for 6 seconds and 4 health at once. Other allies within 3 blocks of each of "
			+ "them catch Regeneration I for 5 seconds.")
	public static void verdantSurge(PairCast c) {
		List<LivingEntity> bloomed = PairCast.first(c.allies(), 3);
		double near = 3 * c.radius;
		ParticleOptions leaf = PairCast.shift(0x9BE37A, 0x2F8F3A, 0.9F);
		ParticleOptions pink = PairCast.dust(0xFFB7D5, 0.9F);
		c.sound(SoundEvents.CROP_PLANTED, c.point(), 0.8F, 1.2F);
		// Wind-up: sprouts push up from the ground round each ally, taller at each of three steps.
		c.every(4, 3, frame -> {
			for (LivingEntity a : c.still(bloomed)) {
				c.column(leaf, a.position(), near * 0.7, 0.8 + frame * 0.7, 10);
			}
		});
		// Payoff, 12 ticks in: the bloom takes, and the allies round it are caught in the regrowth.
		c.later(12, () -> {
			for (LivingEntity a : c.still(bloomed)) {
				c.effect(a, MobEffects.REGENERATION, 6, 1);
				c.heal(a, 4 * c.power);
				for (LivingEntity other : c.alliesNear(a.position(), near)) {
					if (!bloomed.contains(other)) {
						c.effect(other, MobEffects.REGENERATION, 5, 0);
					}
				}
				c.particles(pink, PairCast.mid(a), 10, 0.5, 0.05);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 8, 0.6, 0.02);
			}
			c.sound(SoundEvents.BONE_MEAL_USE, c.point(), 0.8F, 1.0F);
		});
		// Aftermath: spores drift up off the blooms.
		c.later(30, () -> {
			for (LivingEntity a : c.still(bloomed)) {
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, a.position().add(0, 1.5, 0), 8, 0.6, 0.02);
			}
		});
	}

	/**
	 * Stout Bulwark: a wall of pale stone dust rises half way between the point and each ally; the ally stands behind it
	 * with Resistance and Absorption, and enemies at the wall are shoved back. The look: a wall built course by course.
	 */
	@Pair(a = "shoreup", b = "stoutheart", name = "Stout Bulwark", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A stone wall rises between the point and up to 3 allies. Each of them gets Resistance I and 6 absorption "
			+ "(3 hearts) for 10 seconds, and enemies within 2 blocks of the wall are shoved back.")
	public static void stoutBulwark(PairCast c) {
		List<LivingEntity> sheltered = PairCast.first(c.allies(), 3);
		Vec3 at = c.point();
		double reach = 2 * c.radius;
		ParticleOptions stone = PairCast.dust(0xB7B2A7, 1.1F);
		ParticleOptions pale = PairCast.shift(0xF2EEE4, 0x8C857A, 0.9F);
		c.sound(SoundEvents.STONE_PLACE, at, 0.9F, 0.8F);
		// Wind-up: three courses of wall rise, one every four ticks.
		c.every(4, 3, frame -> {
			for (LivingEntity a : c.still(sheltered)) {
				Vec3 across = wallAcross(a.position(), at).scale(1.5);
				Vec3 mid = a.position().add(at.subtract(a.position()).scale(0.5));
				double h = 0.3 + frame * 0.7;
				c.line(stone, mid.subtract(across).add(0, h, 0), mid.add(across).add(0, h, 0), 2);
			}
		});
		// Payoff, 12 ticks in: the allies are covered, and the enemies at the wall are shoved back.
		c.later(12, () -> {
			for (LivingEntity a : c.still(sheltered)) {
				Vec3 mid = a.position().add(at.subtract(a.position()).scale(0.5));
				c.effect(a, MobEffects.RESISTANCE, 10, 0);
				c.absorb(a, 6 * c.power, 10);
				for (LivingEntity e : c.enemiesNear(mid, reach)) {
					c.knockFrom(e, mid, 0.8, 0.3);
				}
				c.ring(pale, mid.add(0, 1.2, 0), 1.0, 14, 0);
			}
			c.sound(SoundEvents.STONE_HIT, at, 0.8F, 0.7F);
		});
	}

	/**
	 * Sifting Storm: sand sifts down over the point in a lingering storm. Each pulse hurts what is inside and slows it,
	 * and anything airborne in it is slammed down. The look: sand falling from above, a low wind ring on the ground.
	 */
	@Pair(a = "downdraft", b = "siftfall", name = "Sifting Storm", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "For 3 seconds, sand sifts down over the point in a 3-block storm (4 pulses, one a second). Each pulse deals 2 damage "
			+ "and Slowness I for 2 seconds to enemies inside, and slams any airborne one down.")
	public static void siftingStorm(PairCast c) {
		Vec3 at = c.point();
		double r = 3 * c.radius;
		ParticleOptions sand = PairCast.dust(0xD8C08A, 1.0F);
		ParticleOptions gust = PairCast.dust(0xE8F4F8, 0.8F);
		c.sound(SoundEvents.SAND_FALL, at, 0.7F, 0.8F);
		c.every(20, 4, pulse -> {
			c.particles(sand, at.add(0, 3, 0), 16, r * 0.8, 0.05);
			c.disc(sand, at, r, 24);
			c.wave(gust, at, 14, 0.25);
			c.sound(SoundEvents.SAND_STEP, at, 0.5F, 0.7F);
			for (LivingEntity t : c.enemiesNear(at, r)) {
				c.hurt(t, 2 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 0);
				if (!t.onGround()) {
					c.push(t, new Vec3(0, -1.0, 0));
				}
			}
		});
	}

	/**
	 * Stalk Leap: cane stalks climb under the caster's feet, spring, and the caster leaps up and forward, drifting down
	 * slowly. The look: green stalks spiralling up, a wind wave where the caster lands.
	 */
	@Pair(a = "soar", b = "stalkrise", name = "Stalk Leap", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "The caster leaps up and forward along their look, then drifts down slowly with Slow Falling for 3 seconds. "
			+ "Cane stalks climb under their feet first.")
	public static void stalkLeap(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 forward = flat(c.dir());
		ParticleOptions cane = PairCast.shift(0x9ED36A, 0xE8F5C0, 0.9F);
		ParticleOptions wind = PairCast.dust(0xDDEEF7, 0.8F);
		// Wind-up: three steps of stalks climb under the caster's feet.
		c.every(4, 3, frame -> {
			c.spiral(cane, self.position(), 0.6, 0.6 + frame * 0.8, 1.5, 10);
			c.sound(SoundEvents.GRASS_BREAK, self.position(), 0.5F, 0.8F + 0.2F * frame);
		});
		// Launch, 12 ticks in: the stalks spring, and the caster leaps.
		c.later(12, () -> {
			c.push(self, forward.scale(0.8).add(0, 0.9, 0));
			c.effect(self, MobEffects.SLOW_FALLING, 3, 0);
			c.punch(0.2F);
			c.sound(SoundEvents.BREEZE_JUMP, self.position(), 0.8F, 1.0F);
			c.wave(wind, self.position(), 14, 0.3);
		});
		// Landing: the cane settles back into a ring.
		c.later(40, () -> c.ring(cane, self.position().add(0, 0.1, 0), 1.0, 12, 0));
	}

	/**
	 * Tallwind: a column of wind stands over the point and carries the enemies in it up in steps, hurting them at each
	 * step; when it ends they drop. The look: a gust column round a rising spiral of dust, a disc of dust where they fall.
	 */
	@Pair(a = "stilt", b = "summit_wind", name = "Tallwind", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A wind column 2.5 blocks out from the point stands for 2.5 seconds. Enemies inside are carried up in five "
			+ "steps, taking 1 damage at each, and when it ends they drop for 3 damage.")
	public static void tallWind(PairCast c) {
		Vec3 at = c.point();
		double r = 2.5 * c.radius;
		ParticleOptions gust = PairCast.dust(0xE6F1F7, 1.0F);
		ParticleOptions stone = PairCast.shift(0xC9B38A, 0x8A7A5C, 0.8F);
		Set<LivingEntity> caught = new HashSet<>();
		c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.8F, 0.7F);
		// The column: five steps ten ticks apart, each one lifting and striking whatever is inside.
		c.every(10, 5, step -> {
			c.column(gust, at, r, 3.0, 18);
			c.spiral(stone, at, r, 3.0, 2, 16);
			for (LivingEntity t : c.enemiesNear(at, r)) {
				caught.add(t);
				c.lift(t, 0.5);
				c.strike(t, 1 * c.power);
			}
		});
		// The drop, when the column ends.
		c.later(50, () -> {
			for (LivingEntity t : c.still(caught)) {
				c.strike(t, 3 * c.power);
			}
			c.disc(stone, at, r, 24);
			c.sound(SoundEvents.MACE_SMASH_GROUND, at, 0.7F, 1.3F);
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The horizontal part of {@code v}, as a unit vector (east when it has none). */
	private static Vec3 flat(Vec3 v) {
		Vec3 h = new Vec3(v.x, 0, v.z);
		return h.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : h.normalize();
	}

	/** The horizontal unit vector across the line from {@code from} to {@code to}: the way a wall runs. */
	private static Vec3 wallAcross(Vec3 from, Vec3 to) {
		Vec3 d = flat(to.subtract(from));
		return new Vec3(-d.z, 0, d.x);
	}
}
