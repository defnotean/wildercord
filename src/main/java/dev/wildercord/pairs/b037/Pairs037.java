package dev.wildercord.pairs.b037;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Targets;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs (group 7): each has its own mechanic, look, sounds and rule text. */
public final class Pairs037 {
	private Pairs037() {}

	/**
	 * Hollow Weight: the enemies are lifted into a hang, the void drains their weight, then it all comes back at once:
	 * a slam that deals damage. The look: violet shells round each target while it hangs, a flat shockwave on landing.
	 */
	@Pair(a = "levitate", b = "unburden", name = "Hollow Weight", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Levitates up to 3 enemies for 3 seconds (Levitation I; bosses stay put). Then their weight comes back all at once: "
			+ "each of them takes 6 damage and is slammed down.")
	public static void hollowWeight(PairCast c) {
		List<LivingEntity> targets = PairCast.first(c.enemies(), 3);
		int hang = c.ticks(3);
		ParticleOptions dark = PairCast.dust(0x3B1F6B, 1.2F);
		ParticleOptions pale = PairCast.shift(0xC9B8FF, 0x2A0E4A, 0.9F);
		Vec3 at = c.point();
		c.sound(SoundEvents.ENDERMAN_TELEPORT, at, 0.7F, 0.5F);
		for (LivingEntity t : targets) {
			if (c.movable(t)) {
				c.effect(t, MobEffects.LEVITATION, 3, 0);
			}
		}
		c.every(4, Math.max(1, hang / 4), i -> {
			for (LivingEntity t : c.still(targets)) {
				c.sphere(pale, PairCast.mid(t), 0.9, 10);
				c.spiral(dark, t.position(), 0.8, 1.6, 1.0, 8);
			}
		});
		c.later(hang, () -> {
			for (LivingEntity t : c.still(targets)) {
				t.removeEffect(MobEffects.LEVITATION);
				c.push(t, new Vec3(0, -1.6, 0));
				c.wither(t, 6 * c.power);
				c.wave(pale, t.position().add(0, 0.2, 0), 16, 0.3);
			}
			c.sound(SoundEvents.ANVIL_LAND, at, 0.5F, 1.7F);
			c.shake(at, 0.3F, 10);
			c.tint(at, 8, 0x2A0E4A, 6);
		});
	}

	/**
	 * Mirror Tear: each enemy is torn sideways through a rift and leaves the rift behind it. A moment later the rift
	 * closes and drags back whoever stands near it. The look: a storm-blue helix spinning over each old spot.
	 */
	@Pair(a = "portal_reckoning", b = "riftbolt", name = "Mirror Tear", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Tears up to 3 enemies 5 blocks straight away from you and shocks each for 5, leaving them in Darkness for 3 seconds. "
			+ "The rift they leave spins for 1.5 seconds, then closes: enemies within 2 blocks of it are pulled in and take 3.")
	public static void mirrorTear(PairCast c) {
		List<LivingEntity> torn = PairCast.first(c.enemies(), 3);
		ParticleOptions storm = PairCast.shift(0x9FE7FF, 0x5B2A86, 0.9F);
		ParticleOptions rift = PairCast.dust(0x3A1A5C, 1.1F);
		List<Vec3> holes = new ArrayList<>();
		c.sound(SoundEvents.PORTAL_TRIGGER, c.point(), 0.5F, 1.8F);
		for (LivingEntity t : torn) {
			Vec3 old = t.position();
			c.shock(t, 5 * c.power);
			c.effect(t, MobEffects.DARKNESS, 3, 0);
			Vec3 away = horizontal(old.subtract(c.caster.position()));
			if (c.blink(t, old.add(away.scale(5 * c.radius)))) {
				holes.add(old);
			}
		}
		c.every(3, 10, i -> {
			for (Vec3 hole : holes) {
				c.helix(storm, rift, hole.add(0, 0.1, 0), 0.8, 2.2, 1.0, 12);
			}
		});
		c.later(30, () -> {
			for (Vec3 hole : holes) {
				c.sphere(storm, hole.add(0, 1, 0), 2.0, 30);
				for (LivingEntity e : c.enemiesNear(hole, 2 * c.radius)) {
					c.pullTo(e, hole, 1.0);
					c.shock(e, 3 * c.power);
				}
				c.sound(SoundEvents.ENDERMAN_TELEPORT, hole, 0.8F, 0.5F);
				c.shake(hole, 0.2F, 8);
			}
		});
	}

	/**
	 * Lodestar Well: a compass needle picks the enemy nearest the point, and a black hole opens over it, drawing in
	 * what is near and bursting at the end. The look: a needle of gold light, then a shrinking void shell.
	 */
	@Pair(a = "singularity", b = "stronghold_compass", name = "Lodestar Well", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "A compass needle finds the enemy nearest the point (within 10 blocks) and a black hole opens on it for 2.5 seconds, "
			+ "pulling in enemies within 4 blocks. Then it bursts: 5 damage to each enemy it struck or swallowed, plus 1 for each "
			+ "one swallowed (5 at most), and they're flung outward.")
	public static void lodestarWell(PairCast c) {
		LivingEntity hub = c.nearestEnemy(c.point(), 10 * c.radius, null);
		Vec3 well = hub != null ? PairCast.mid(hub) : c.point();
		int life = c.ticks(2.5);
		int frames = Math.max(2, life / 5);
		Set<LivingEntity> swallowed = new LinkedHashSet<>();
		ParticleOptions needle = PairCast.shift(0xF2E6A0, 0x7A3FD1, 0.8F);
		ParticleOptions dark = PairCast.dust(0x14081F, 1.4F);
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 0.8F, 0.6F);
		c.line(needle, c.point(), well, 3);
		c.every(5, frames, i -> {
			double shrink = 1 - (double) i / frames;
			c.sphere(dark, well, 0.6 + 3.4 * shrink, 24);
			c.sphere(needle, well, 0.6 + 2.0 * shrink, 10);
			for (LivingEntity e : c.enemiesNear(well, 4 * c.radius)) {
				swallowed.add(e);
				c.pullTo(e, well, 0.7);
			}
		});
		c.later(life, () -> {
			Set<LivingEntity> burst = new LinkedHashSet<>(PairCast.first(c.enemies(), PairCast.MAX_TARGETS));
			burst.addAll(swallowed);
			int extra = Math.min(5, swallowed.size());
			for (LivingEntity t : c.still(burst)) {
				c.wither(t, (5 + extra) * c.power);
				c.knockFrom(t, well, 1.2, 0.5);
			}
			c.sound(SoundEvents.END_PORTAL_FRAME_FILL, well, 0.9F, 0.5F);
			c.wave(needle, well, 28, 0.4);
			c.shake(well, 0.4F, 10);
			c.tint(well, 8, 0x2A0A4A, 6);
		});
	}

	/**
	 * Turning Veil: a wind dome on an ally for 8 seconds. Missiles that enter it are sent back along their path, at the
	 * shooter, unless they were your side's. The look: a pale dome of wind, a flash on each turned missile.
	 */
	@Pair(a = "arrowveil", b = "deflect", name = "Turning Veil", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "For 8 seconds a veil of wind reaching 4 blocks out stands on your first ally (or on you). Enemy missiles entering it are sent "
			+ "back along their path, at whoever fired them; your own side's pass through.")
	public static void turningVeil(PairCast c) {
		LivingEntity ward = c.firstAlly() != null ? c.firstAlly() : c.caster;
		int life = c.ticks(8);
		Set<Entity> turned = new HashSet<>();
		ParticleOptions wind = PairCast.shift(0xDDF6FF, 0x7FB8D6, 0.9F);
		double r = 4 * c.radius;
		c.sound(SoundEvents.WIND_CHARGE_BURST, ward.position(), 0.7F, 1.4F);
		c.every(2, Math.max(1, life / 2), i -> {
			if (!c.here(ward)) {
				return;
			}
			Vec3 centre = PairCast.mid(ward);
			if (i % 5 == 0) {
				c.sphere(wind, centre, r, 20);
			}
			AABB box = new AABB(centre, centre).inflate(r);
			for (Projectile p : c.level.getEntitiesOfClass(Projectile.class, box,
					e -> foreign(c, e) && !turned.contains(e) && e.getDeltaMovement().lengthSqr() > 0.01)) {
				if (p.position().distanceTo(centre) <= r) {
					turned.add(p);
					p.setDeltaMovement(p.getDeltaMovement().scale(-1));
					c.particles(ParticleTypes.ENCHANTED_HIT, p.position(), 6, 0.2, 0.1);
					c.sound(SoundEvents.BREEZE_DEFLECT, p.position(), 0.8F, 1.3F);
				}
			}
		});
	}

	/**
	 * Startle Ward: the enemies near the point are startled (slowed), then a ward of wind stands there and shoves out
	 * whatever stays inside. The look: a straw-gold ring on the ground, dark violet fright shells, eerie sounds.
	 */
	@Pair(a = "scarecrow", b = "spook", name = "Startle Ward", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Startles up to 6 enemies within 5 blocks: Slowness II for 4 seconds. Then for 6 seconds a ward of wind stands on the spot: "
			+ "every 1.5 seconds it shoves enemies within 4 blocks out and deals them 1 damage. Bosses are slowed and struck, never shoved.")
	public static void startleWard(PairCast c) {
		Vec3 at = c.point();
		ParticleOptions straw = PairCast.dust(0xE6C75A, 1.0F);
		ParticleOptions fright = PairCast.shift(0x5A3C8C, 0x1A0F2E, 1.0F);
		List<LivingEntity> startled = PairCast.first(c.enemiesNear(at, 5 * c.radius), 6);
		c.sound(SoundEvents.EVOKER_PREPARE_ATTACK, at, 0.8F, 1.4F);
		for (LivingEntity t : startled) {
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
		}
		c.wave(fright, at, 20, 0.3);
		c.every(30, 5, i -> {
			c.ring(straw, at, 4 * c.radius, 20, i * 0.3);
			c.sound(SoundEvents.BREEZE_SLIDE, at, 0.6F, 1.2F);
			for (LivingEntity t : c.enemiesNear(at, 4 * c.radius)) {
				if (c.movable(t)) {
					c.knockFrom(t, at, 0.8, 0.2);
				}
				c.strike(t, 1 * c.power);
			}
		});
	}

	/**
	 * Muster Horn: a whistle calls your tamed pets in and a war horn sounds half a second later. The look: gold notes
	 * streaming to you, then a red-gold shockwave over the spot.
	 */
	@Pair(a = "warcry", b = "whistle", name = "Muster Horn", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Your whistle calls tamed pets within 16 blocks. Half a second later a war horn sounds at the spot: allies within 8 blocks "
			+ "gain Strength I, Speed I and 4 absorption (2 hearts) for 10 seconds, and the pets that reach your side get Regeneration I for 6.")
	public static void musterHorn(PairCast c) {
		Vec3 at = c.point();
		LivingEntity caster = c.caster;
		ParticleOptions note = PairCast.dust(0xF2C14E, 1.0F);
		ParticleOptions blood = PairCast.shift(0xA3121B, 0xF2C14E, 1.1F);
		List<TamableAnimal> pets = new ArrayList<>();
		for (LivingEntity e : c.alliesNear(caster.position(), 16 * c.radius)) {
			if (e instanceof TamableAnimal tame && tame.isTame()) {
				pets.add(tame);
			}
		}
		c.sound(SoundEvents.NOTE_BLOCK_FLUTE, caster.position(), 0.9F, 1.6F);
		for (TamableAnimal pet : pets) {
			c.line(note, PairCast.mid(pet), PairCast.mid(caster), 1.5);
		}
		c.later(10, () -> {
			c.sound(SoundEvents.RAID_HORN, at, 1.0F, 1.0F);
			c.wave(blood, at.add(0, 0.2, 0), 24, 0.35);
			c.ring(note, at, 8 * c.radius, 28, 0);
			for (LivingEntity ally : c.alliesNear(at, 8 * c.radius)) {
				c.effect(ally, MobEffects.STRENGTH, 10, 0);
				c.effect(ally, MobEffects.SPEED, 10, 0);
				c.absorb(ally, 4, 10);
			}
			for (int k = 0; k < pets.size(); k++) {
				TamableAnimal pet = pets.get(k);
				double a = k * 1.3;
				Vec3 spot = caster.position().add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
				if (c.here(pet) && c.blink(pet, spot)) {
					c.effect(pet, MobEffects.REGENERATION, 6, 0);
					c.particles(note, PairCast.mid(pet), 8, 0.4, 0.05);
				}
			}
			c.punch(0.2F);
		});
	}

	/**
	 * Gust Stair: five gusts lift the caster up and forward, one every 4 ticks, and a burst at the top shoves enemies
	 * away. The look: pale breeze curls at each step, a ring of wind on landing.
	 */
	@Pair(a = "leap", b = "wind_steps", name = "Gust Stair", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "Five gusts, one every 4 ticks, push you up 0.55 and forward 0.4 each, skipping any step that faces a wall. "
			+ "At the top you get Slow Falling for 3 seconds, and a burst shoves enemies within 3 blocks out for 2 damage.")
	public static void gustStair(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 forward = horizontal(c.dir());
		ParticleOptions air = PairCast.shift(0xE8F8FF, 0x9ED8F0, 0.9F);
		c.every(4, 5, i -> {
			Vec3 from = self.position().add(0, 1, 0);
			BlockHitResult wall = c.level.clip(new ClipContext(from, from.add(forward.scale(1.5)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (wall.getType() == HitResult.Type.BLOCK) {
				return;
			}
			c.push(self, forward.scale(0.4).add(0, 0.55, 0));
			c.particles(air, self.position(), 10, 0.4, 0.05);
			c.sound(SoundEvents.BREEZE_JUMP, self.position(), 0.7F, 0.9F + 0.1F * i);
		});
		c.later(20, () -> {
			c.effect(self, MobEffects.SLOW_FALLING, 3, 0);
			Vec3 top = self.position();
			for (LivingEntity t : c.enemiesNear(top, 3)) {
				c.knockFrom(t, top, 1.0, 0.4);
				c.strike(t, 2 * c.power);
			}
			c.wave(air, top.add(0, 0.2, 0), 24, 0.45);
			c.sound(SoundEvents.WIND_CHARGE_BURST, top, 0.9F, 0.8F);
			c.punch(0.15F);
		});
	}

	/**
	 * Canopy of Leaves: a canopy of leaves over the spot for 10 seconds. Allies under it are kept from harm in a fall
	 * and healed a little every 2 seconds. The look: green leaves dropping through a ring of canopy.
	 */
	@Pair(a = "cushion", b = "leaffall", name = "Canopy of Leaves", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius", "power"},
		text = "For 10 seconds a canopy of leaves hangs 3 blocks up over the spot, reaching 4 blocks out. Every 2 seconds the allies "
			+ "under it get 1 health back and Slow Falling for 3 seconds, so falls can't hurt them while it lasts.")
	public static void canopyOfLeaves(PairCast c) {
		Vec3 at = c.point();
		int life = c.ticks(10);
		double r = 4 * c.radius;
		ParticleOptions leaf = PairCast.shift(0x8DBF4A, 0xE0912F, 1.1F);
		ParticleOptions green = PairCast.dust(0x3F7A2A, 1.0F);
		c.sound(SoundEvents.AZALEA_LEAVES_PLACE, at, 0.9F, 0.9F);
		c.every(40, life / 40 + 1, i -> {
			Vec3 canopy = at.add(0, 3, 0);
			c.ring(green, canopy, r, 16, i * 0.4);
			c.particles(leaf, canopy, 12, r * 0.7, 0.02);
			for (LivingEntity ally : c.alliesNear(at, r)) {
				c.effect(ally, MobEffects.SLOW_FALLING, 3, 0);
				c.heal(ally, 1 * c.power);
			}
			c.sound(SoundEvents.CHERRY_LEAVES_BREAK, at, 0.5F, 1.1F);
		});
	}

	/**
	 * Spore Pollen: a spore cloud pulses three times over the point. Each pulse poisons and hurts what is inside, more
	 * for every bee nearby, and sets the monsters inside on one another. The look: green spore shells, violet puffs.
	 */
	@Pair(a = "pollinate", b = "sporebloom", name = "Spore Pollen", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Spores burst in a 3-block cloud at the point, pulsing three times 2 seconds apart. Each pulse deals 1 damage and Poison I "
			+ "for 4 seconds to enemies inside, plus 1 for each bee within 8 blocks (2 at most); monsters inside are set on another monster there.")
	public static void sporePollen(PairCast c) {
		Vec3 at = c.point();
		double r = 3 * c.radius;
		int bees = Math.min(2, c.level.getEntitiesOfClass(Bee.class, new AABB(at, at).inflate(8), Bee::isAlive).size());
		ParticleOptions spore = PairCast.dust(0x9BE8B4, 1.0F);
		ParticleOptions violet = PairCast.shift(0x6B3FA0, 0x2E7D5B, 1.0F);
		c.every(40, 3, i -> {
			c.sphere(spore, at, r, 30);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, at, 20, r * 0.6, 0.03);
			c.sound(SoundEvents.SLIME_SQUISH, at, 0.6F, 1.4F - 0.2F * i);
			List<LivingEntity> inside = c.still(c.enemiesNear(at, r));
			List<Mob> monsters = new ArrayList<>();
			for (LivingEntity t : inside) {
				c.hurt(t, (1 + bees) * c.power);
				c.effect(t, MobEffects.POISON, 4, 0);
				if (t instanceof Mob m && c.movable(m)) {
					monsters.add(m);
				}
			}
			for (int k = 0; k < monsters.size(); k++) {
				LivingEntity other = monsters.get((k + 1) % monsters.size());
				if (other != monsters.get(k)) {
					monsters.get(k).setTarget(other);
				}
			}
			c.particles(violet, at, 8, r * 0.5, 0.02);
		});
	}

	/**
	 * Reaping Gale: three shear lanes sweep out ahead of the point. Each lane shears what it passes once, and a lane
	 * reaps what is already low. The look: wheat-gold lines and sweep arcs over the ground, a harvest burst at the end.
	 */
	@Pair(a = "thresherwind", b = "windcut", name = "Reaping Gale", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Three shear lanes sweep out 6 blocks ahead of the point and pass in under a second. Each enemy within 2 blocks of a lane "
			+ "takes 3 damage and a shove, once per lane; one below half health when a lane reaches it is reaped for 4 more, once.")
	public static void reapingGale(PairCast c) {
		Vec3 at = c.point();
		Vec3 fwd = horizontal(c.dir());
		ParticleOptions wheat = PairCast.dust(0xE9C46A, 1.0F);
		ParticleOptions steel = PairCast.shift(0xE8F4F8, 0xB8C4CC, 0.8F);
		Set<LivingEntity> reaped = new HashSet<>();
		List<Set<LivingEntity>> hitBy = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>());
		Vec3[] last = {at, at, at};
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, at, 1.0F, 0.9F);
		c.every(3, 6, step -> {
			for (int lane = 0; lane < 3; lane++) {
				Vec3 sweep = at.add(turn(fwd, (lane - 1) * 0.35).scale(step + 1.0));
				c.line(steel, last[lane], sweep, 2);
				c.particles(ParticleTypes.SWEEP_ATTACK, sweep, 1, 0, 0);
				for (LivingEntity t : c.enemiesNear(sweep, 2)) {
					if (hitBy.get(lane).add(t)) {
						boolean low = t.getHealth() < t.getMaxHealth() / 2;
						c.strike(t, 3 * c.power);
						c.knockFrom(t, sweep, 0.5, 0.1);
						if (low && reaped.add(t)) {
							c.strike(t, 4 * c.power);
							c.sound(SoundEvents.PLAYER_ATTACK_CRIT, t.position(), 0.6F, 1.2F);
							c.particles(wheat, t.position().add(0, 1, 0), 8, 0.4, 0.05);
						}
					}
				}
				last[lane] = sweep;
			}
		});
		c.later(18, () -> {
			c.particles(wheat, at.add(fwd.scale(6)), 20, 1.0, 0.05);
			c.sound(SoundEvents.CROP_PLANTED, at, 0.6F, 0.6F);
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The horizontal part of {@code v}, as a unit vector (east when it has none). */
	private static Vec3 horizontal(Vec3 v) {
		Vec3 h = new Vec3(v.x, 0, v.z);
		return h.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : h.normalize();
	}

	/** The horizontal unit vector {@code v} turned {@code angle} radians about the up axis. */
	private static Vec3 turn(Vec3 v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec3(v.x * cos - v.z * sin, 0, v.x * sin + v.z * cos);
	}

	/** Whether {@code e} is a projectile that is not the caster's or an ally's. */
	private static boolean foreign(PairCast c, Entity e) {
		if (!(e instanceof Projectile p)) {
			return false;
		}
		Entity owner = p.getOwner();
		return owner == null || owner != c.caster && !(owner instanceof LivingEntity && Targets.isAlly(c.caster, owner));
	}
}
