package dev.wildercord.pairs.b029;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs029 {
	private Pairs029() {}

	/**
	 * Gravecinder: a brand that spreads. Black fire takes hold of the first enemy and burns it every second; twice
	 * during its burning the brand leaps to a nearby enemy. The look: a dark spiral wound round the target, soul
	 * wisps and smoke each second, and a violet line jumping from one branded body to the next.
	 */
	@Pair(a = "blackflame", b = "wither", name = "Gravecinder", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Black fire brands the first enemy hit for 6 seconds: 1 wither damage a second (times power). At 2 and 4 seconds "
			+ "the brand leaps from a branded enemy to the nearest unbranded one within 3 blocks, for 2 wither damage on arrival. "
			+ "Up to 3 are branded.")
	public static void gravecinder(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		List<LivingEntity> brand = new ArrayList<>();
		brand.add(first);
		c.spiral(PairCast.shift(0x5A2A8C, 0x0B0512, 1.1F), PairCast.mid(first), 0.8, 2.2, 2, 24);
		c.sound(SoundEvents.WITHER_SHOOT, PairCast.mid(first), 0.5F, 1.6F);
		c.every(20, 6, frame -> {
			for (LivingEntity t : c.still(brand)) {
				Vec3 at = PairCast.mid(t);
				c.wither(t, c.power);
				c.particles(ParticleTypes.SOUL, at, 6, 0.35, 0.02);
				c.wave(PairCast.dust(0x1A0826, 1.2F), at, 12, 0.2);
			}
			if (frame == 2 || frame == 4) {
				leap(c, brand);
			}
		});
	}

	/** One leap of the brand: to the nearest enemy near a branded one that doesn't have it yet. */
	private static void leap(PairCast c, List<LivingEntity> brand) {
		if (brand.size() >= 3) {
			return;
		}
		for (LivingEntity from : c.still(brand)) {
			for (LivingEntity to : c.enemiesNear(PairCast.mid(from), 3)) {
				if (!brand.contains(to)) {
					brand.add(to);
					c.line(PairCast.shift(0x3B1466, 0x9B5CFF, 1.0F), PairCast.mid(from), PairCast.mid(to), 2);
					c.wither(to, 2 * c.power);
					c.sound(SoundEvents.SHULKER_BULLET_HIT, PairCast.mid(to), 0.6F, 0.7F);
					return;
				}
			}
		}
	}

	/**
	 * Eclipse Well: a black hole that waits, then collapses. It opens where the spell lands and drags what it hit and
	 * what is near into its centre for 2.5 seconds, then the collapse wounds and blinds everything it caught. The look:
	 * a violet sphere that darkens, purple pull lines, and a star of dark rays at the moment of collapse.
	 */
	@Pair(a = "singularity", b = "starmaw", name = "Eclipse Well", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "A black hole opens where it lands. For 2.5 seconds it drags up to 8 enemies towards its centre, 4 times a second: "
			+ "the ones it hit first, then any within 5 blocks (bosses stay put). Then it collapses: 4 wither damage to each one "
			+ "caught, and 3 seconds of blindness.")
	public static void eclipseWell(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> gathered = new ArrayList<>(c.enemies());
		for (LivingEntity t : c.enemiesNear(spot, 5 * c.radius)) {
			if (!gathered.contains(t)) {
				gathered.add(t);
			}
		}
		List<LivingEntity> caught = PairCast.first(gathered, PairCast.MAX_TARGETS);
		c.sound(SoundEvents.PORTAL_TRIGGER, spot, 0.5F, 0.6F);
		c.sphere(PairCast.shift(0xB58CFF, 0x05020A, 1.2F), spot, 1.0, 30);
		c.every(5, 10, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, spot, 0.5);
				c.line(PairCast.dust(0x7B4FD6, 0.8F), PairCast.mid(t), spot, 2);
			}
			c.ring(PairCast.dust(0x120625, 1.4F), spot, 0.9, 20, frame * 0.5);
		});
		c.later(50, () -> {
			for (LivingEntity t : c.still(caught)) {
				c.wither(t, 4 * c.power);
				c.effect(t, MobEffects.BLINDNESS, 3, 0);
			}
			c.star(PairCast.shift(0xE0CCFF, 0x3A1B66, 1.0F), spot, 8, 2.5, 0.3);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, spot, 0.8F, 0.5F);
			c.shake(spot, 0.2F, 10);
		});
	}

	/**
	 * Hollow Hush: silence that holds. Up to four enemies lose their target and are held back, and the hush wears on
	 * them each second. The look: violet spheres round each one, then a dark pulse of sparks each second.
	 */
	@Pair(a = "enderhush", b = "silence", name = "Hollow Hush", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Hushes up to 4 enemies for 6 seconds. Each forgets its target and is held back (Slowness II, and its navigation "
			+ "stopped every quarter second), and takes 1 magic damage a second (times power). A boss is only slowed and hurt.")
	public static void hollowHush(PairCast c) {
		List<LivingEntity> hushed = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, c.point(), 0.4F, 0.5F);
		for (LivingEntity t : hushed) {
			c.effect(t, MobEffects.SLOWNESS, 6, 1);
			c.sphere(PairCast.dust(0x3D1F5C, 0.9F), PairCast.mid(t), 0.9, 18);
		}
		c.every(5, 25, frame -> {
			for (LivingEntity t : c.still(hushed)) {
				if (t instanceof Mob m && c.movable(m)) {
					m.setTarget(null);
					m.getNavigation().stop();
				}
				if (frame % 4 == 0 && frame < 24) {
					Vec3 at = PairCast.mid(t);
					c.hurt(t, c.power);
					c.particles(ParticleTypes.WITCH, at, 8, 0.4, 0.02);
					c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 0.4F, 0.6F);
				}
			}
		});
	}

	/**
	 * Lodeleap: a leap that comes back. You blink up to six blocks along your look to safe ground, and two seconds
	 * later you return to where you leapt from, unless your health dropped in between. The look: an arcane spiral at
	 * the start, a violet line to the landing, and a ring at each end.
	 */
	@Pair(a = "homeward", b = "wayline", name = "Lodeleap", element = "arcane", kind = EffectKind.MOVEMENT,
		traits = {},
		text = "Leaps you up to 6 blocks along your look, stopping short of walls, onto safe ground. Two seconds later you "
			+ "return to where you leapt from, unless your health dropped in between. Nothing else is moved.")
	public static void lodeleap(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 to = clearRun(c, self, from, flat(self.getLookAngle()), 6);
		c.spiral(PairCast.shift(0x7FE8FF, 0xC77DFF, 0.9F), from.add(0, 1, 0), 0.5, 1.8, 2, 20);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.6F, 1.3F);
		if (!c.blink(self, to)) {
			return;
		}
		float atLeap = self.getHealth();
		c.line(PairCast.shift(0x7FE8FF, 0xC77DFF, 1.0F), from.add(0, 1, 0), to.add(0, 1, 0), 2);
		c.ring(ParticleTypes.REVERSE_PORTAL, to.add(0, 0.2, 0), 0.8, 16, 0.0);
		c.later(40, () -> {
			if (!self.isAlive() || self.getHealth() < atLeap) {
				return;
			}
			if (c.blink(self, from)) {
				c.ring(ParticleTypes.PORTAL, from.add(0, 0.2, 0), 0.8, 16, 0.0);
				c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.6F, 1.6F);
			}
		});
	}

	/**
	 * Stalking Edge: a blink behind the first enemy hit that leaves hunters blind to you. The caster vanishes to the far
	 * side of the target, and the target takes a blow; then, for a while, creatures near the caster that were after
	 * the caster lose track of them. The look: a dark puff where you vanish, a line to the target, a ring where you
	 * reappear, and a steady drip of smoke while they lose the scent.
	 */
	@Pair(a = "shadowstep", b = "softfoot", name = "Stalking Edge", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration"},
		text = "If the spot 1.5 blocks behind the first enemy hit is safe, you vanish there and it takes 4 magic damage (times "
			+ "power). For 8 seconds, every creature within 8 blocks that was after you loses track of you (not bosses).")
	public static void stalkingEdge(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		Vec3 toward = flat(PairCast.mid(first).subtract(PairCast.mid(self)));
		Vec3 behind = first.position().add(toward.scale(1.5));
		c.particles(ParticleTypes.SMOKE, PairCast.mid(self), 12, 0.4, 0.04);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(self), 0.7F, 0.6F);
		if (!c.blink(self, behind)) {
			return;
		}
		c.hurt(first, 4 * c.power);
		c.line(PairCast.shift(0x2B1B3F, 0xB68CFF, 0.9F), PairCast.mid(self), PairCast.mid(first), 2);
		c.ring(PairCast.dust(0x2B1B3F, 1.1F), PairCast.mid(self), 0.8, 16, 0.0);
		c.sound(SoundEvents.SHULKER_BULLET_HIT, PairCast.mid(first), 0.6F, 1.3F);
		c.every(4, c.ticks(8) / 4 + 1, frame -> {
			for (LivingEntity t : c.enemiesNear(PairCast.mid(self), 8)) {
				if (t instanceof Mob m && c.movable(m) && m.getTarget() == self) {
					m.setTarget(null);
				}
			}
			if (frame % 5 == 0) {
				c.particles(ParticleTypes.SMOKE, PairCast.mid(self), 4, 0.5, 0.02);
			}
		});
	}

	/**
	 * Lodeshock: a magnetic stone. The first enemy hit becomes a lodestone for four seconds: every second it draws
	 * enemies near to it, shocks whatever stands against it, and pulls loose drops in. The look: a pale-blue sphere
	 * on the lodestone, violet lines drawing crowds in, and sparks where they touch.
	 */
	@Pair(a = "lodepull", b = "magnetize", name = "Lodeshock", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Makes the first enemy hit a lodestone for 4 seconds, which takes 2 lightning damage at once (times power). Each second, enemies "
			+ "within 5 blocks are drawn to it (not bosses), each enemy within 1.5 blocks of it takes 3 lightning damage (times "
			+ "power), and loose item drops within 6 blocks drift to it.")
	public static void lodeshock(PairCast c) {
		LivingEntity lode = c.firstEnemy();
		if (lode == null) {
			return;
		}
		c.shock(lode, 2 * c.power);
		c.sphere(PairCast.shift(0xC9D6FF, 0x3A6BFF, 0.8F), PairCast.mid(lode), 0.7, 18);
		c.sound(SoundEvents.LODESTONE_COMPASS_LOCK, PairCast.mid(lode), 1.0F, 0.8F);
		c.every(20, 4, frame -> {
			if (!c.here(lode)) {
				return;
			}
			Vec3 at = PairCast.mid(lode);
			for (LivingEntity t : c.enemiesNear(at, 5 * c.radius)) {
				if (t == lode) {
					continue;
				}
				c.pullTo(t, at, 0.6);
				c.line(PairCast.dust(0xB7C8FF, 0.8F), PairCast.mid(t), at, 2);
				if (PairCast.mid(t).distanceTo(at) <= 1.5) {
					c.shock(t, 3 * c.power);
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(t), 0.3, 3);
				}
			}
			for (ItemEntity item : c.level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(6 * c.radius))) {
				Vec3 towards = at.subtract(item.position());
				if (towards.lengthSqr() > 0.01) {
					item.setDeltaMovement(towards.normalize().scale(0.25));
				}
			}
		});
	}

	/**
	 * Tailwind Rush: a gust that carries the caster. You are blown along your look in a few quick steps, stopping at
	 * walls, and Speed III and a clean slate of Slowness and frost follow. The look: a pale line along the path, a
	 * puff of cloud at each step, and a ring of wind where you come to rest.
	 */
	@Pair(a = "fair_wind", b = "swift", name = "Tailwind Rush", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "A gust carries you up to 8 blocks along your look in under half a second, stopping short of walls. You get Speed III "
			+ "for 10 seconds, and Slowness and frozen skin are shaken off.")
	public static void tailwindRush(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 end = clearRun(c, self, from, flat(self.getLookAngle()), 8);
		c.line(PairCast.shift(0xE6FFF5, 0x9FD8FF, 0.9F), from.add(0, 1, 0), end.add(0, 1, 0), 1.5);
		c.sound(SoundEvents.WIND_CHARGE_BURST, from, 0.7F, 1.2F);
		c.effect(self, MobEffects.SPEED, 10, 2);
		self.removeEffect(MobEffects.SLOWNESS);
		self.setTicksFrozen(0);
		c.every(2, 4, frame -> {
			Vec3 at = from.lerp(end, (frame + 1) / 4.0);
			c.blink(self, at);
			c.particles(ParticleTypes.CLOUD, at.add(0, 1, 0), 6, 0.3, 0.1);
			c.ring(PairCast.dust(0xD6F4FF, 0.9F), at, 0.6, 12, frame * 0.5);
		});
		c.later(8, () -> c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, end, 0.6F, 1.5F));
	}

	/**
	 * Skyglide: a self-buff for the air. The caster sinks slowly with no fall damage, and while gliding on elytra a
	 * tailwind keeps pushing them along their look. The look: a puff of cloud at the start, then a faint trail of
	 * cloud while the wind holds.
	 */
	@Pair(a = "feather_fall", b = "glidewind", name = "Skyglide", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "For 12 seconds you sink slowly (Slow Falling) and take no fall damage. While you glide on elytra, a tailwind "
			+ "pushes you a little along your look, so the glide doesn't bleed speed.")
	public static void skyglide(PairCast c) {
		LivingEntity self = c.caster;
		c.effect(self, MobEffects.SLOW_FALLING, 12, 0);
		c.sound(SoundEvents.BREEZE_JUMP, self.position(), 0.8F, 1.4F);
		c.particles(ParticleTypes.CLOUD, PairCast.mid(self), 14, 0.6, 0.05);
		c.every(2, c.ticks(12) / 2, frame -> {
			if (!self.isAlive()) {
				return;
			}
			self.resetFallDistance();
			if (self.isFallFlying()) {
				c.push(self, flat(self.getLookAngle()).scale(0.15));
				if (frame % 5 == 0) {
					c.particles(ParticleTypes.CLOUD, PairCast.mid(self), 3, 0.3, 0.02);
				}
			}
		});
	}

	/**
	 * Featherbed: a cushion of wind on the spot. Allies inside never fall hard and walk light; enemies inside are
	 * blown out from the centre once a second. The look: a pale disc laid on the ground, a ring that turns, and drifting
	 * cloud over it for its six seconds.
	 */
	@Pair(a = "cushion", b = "softsole", name = "Featherbed", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "A cushion of wind 3 blocks out from the spot (times radius) for 6 seconds. Allies inside take no fall damage, fall slowly and "
			+ "walk light (Speed I), for a second after they leave. Enemies inside are blown from the centre once a second.")
	public static void featherbed(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		c.disc(PairCast.shift(0xF2FFFF, 0xB7E4FF, 1.0F), spot, r, 40);
		c.sound(SoundEvents.WOOL_PLACE, spot, 1.0F, 0.8F);
		c.every(4, c.ticks(6) / 4 + 1, frame -> {
			for (LivingEntity a : c.alliesNear(spot, r)) {
				a.resetFallDistance();
				c.effect(a, MobEffects.SLOW_FALLING, 1, 0);
				c.effect(a, MobEffects.SPEED, 1, 0);
			}
			if (frame % 5 == 0) {
				for (LivingEntity t : c.enemiesNear(spot, r)) {
					c.knockFrom(t, spot, 0.4, 0.1);
				}
				c.ring(PairCast.dust(0xDFF6FF, 1.0F), spot, r, 24, frame * 0.3);
			}
			c.particles(ParticleTypes.CLOUD, spot, 6, r * 0.7, 0.02);
		});
	}

	/**
	 * Gale Ring: a shockwave that leaves a barrier. A blast of wind knocks back everything near the spot, then for
	 * four seconds a ring of wind turns back each projectile that crosses it, once. The look: a pale ring that
	 * expands at the blast, a turning ring of wind after, and a small burst on each arrow turned back.
	 */
	@Pair(a = "deflect", b = "repel", name = "Gale Ring", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A blast at the spot: 4 damage (times power) to each enemy within 3 blocks (times radius), hurling them outward. "
			+ "For 4 seconds after, each enemy arrow or other projectile that comes within 2.5 blocks (times radius) of the spot is "
			+ "turned back, once each.")
	public static void galeRing(PairCast c) {
		Vec3 spot = c.point();
		double blastR = 3 * c.radius;
		double ringR = 2.5 * c.radius;
		c.sound(SoundEvents.WIND_CHARGE_BURST, spot, 1.0F, 0.8F);
		c.ring(PairCast.shift(0xE0FFFF, 0x8FB8FF, 1.0F), spot, blastR, 32, 0.0);
		for (LivingEntity t : c.enemiesNear(spot, blastR)) {
			c.strike(t, 4 * c.power);
			c.knockFrom(t, spot, 1.0, 0.4);
		}
		c.shake(spot, 0.2F, 8);
		c.every(10, 9, frame -> {
			c.ring(PairCast.dust(0xBFEFFF, 1.0F), spot, ringR, 24, frame * 0.4);
			for (Projectile p : c.level.getEntitiesOfClass(Projectile.class, new AABB(spot, spot).inflate(ringR))) {
				Entity owner = p.getOwner();
				if (owner == c.caster || (owner != null && c.caster.isAlliedTo(owner))) {
					continue;
				}
				if (PairCast.mid(p).distanceTo(spot) <= ringR && c.once("gale" + p.getId())) {
					p.setDeltaMovement(p.getDeltaMovement().scale(-1));
					c.sound(SoundEvents.BREEZE_DEFLECT, PairCast.mid(p), 0.6F, 1.2F);
					c.wave(PairCast.dust(0xFFFFFF, 0.8F), PairCast.mid(p), 10, 0.2);
				}
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The horizontal unit vector of {@code v}; straight up or down counts as forward. */
	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** How far along {@code ahead} from {@code from} (at waist height) one can go before a wall, at most {@code reach}. */
	private static Vec3 clearRun(PairCast c, LivingEntity self, Vec3 from, Vec3 ahead, double reach) {
		Vec3 eye = from.add(0, 1, 0);
		HitResult wall = c.level.clip(new ClipContext(eye, eye.add(ahead.scale(reach)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, self));
		double run = wall.getType() == HitResult.Type.MISS
			? reach
			: Math.max(0, wall.getLocation().subtract(eye).dot(ahead) - 0.8);
		return from.add(ahead.scale(run));
	}
}
