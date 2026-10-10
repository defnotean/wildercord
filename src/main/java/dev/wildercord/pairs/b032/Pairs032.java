package dev.wildercord.pairs.b032;

import dev.wildercord.cast.PairCast;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs032 {
	private Pairs032() {}

	/** The ally the spell struck, or the caster when it struck none. */
	private static LivingEntity anchorOf(PairCast c) {
		LivingEntity ally = c.firstAlly();
		return ally != null ? ally : c.caster;
	}

	/** The distance from {@code p} to the straight line segment {@code a}-{@code b}. */
	private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		double f = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(f)));
	}

	/**
	 * Kindled Aegis: a shield of light with cinder plates wheeling round it. The ally gets absorption; four plates
	 * orbit it for eight seconds, and each enemy that comes close enough to meet one takes a hit and cracks it away.
	 * The look: a tight gold hoop of plates swinging in, then embers flaring where each one breaks.
	 */
	@Pair(a = "barrier", b = "cinder_bulwark", name = "Kindled Aegis", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "The ally the spell struck (you, if none) gets 4 absorption (times power) for 20 seconds. Four cinder plates wheel round "
			+ "it for 8 seconds: each enemy that comes within 2 blocks is struck once for 2 magic damage (times power), "
			+ "and its plate cracks away.")
	public static void kindledAegis(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.absorb(anchor, 4 * c.power, 20);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(anchor), 0.8F, 1.3F);
		Set<LivingEntity> cracked = new HashSet<>();
		c.every(4, 40, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			// The plates fly in from three blocks out to one and a half, then keep their orbit.
			double r = frame < 6 ? 3.0 - frame * 0.25 : 1.5;
			c.ring(PairCast.shift(0xFFE08A, 0xFF5A1F, 0.9F), at.add(0, 0.4, 0), r, 4, frame * 0.3);
			if (frame % 4 == 0) {
				c.ring(PairCast.dust(0xFFD36B, 0.6F), at.add(0, 0.2, 0), 2.0, 24, frame * 0.1);
			}
			for (LivingEntity e : c.enemiesNear(at, 2)) {
				if (cracked.add(e)) {
					Vec3 hit = PairCast.mid(e);
					c.hurt(e, 2 * c.power);
					c.sphere(PairCast.shift(0xFFE08A, 0xFF5A1F, 0.8F), hit, 0.7, 12);
					c.sound(SoundEvents.FIRECHARGE_USE, hit, 0.6F, 1.4F);
				}
			}
		});
	}

	/**
	 * Second Flush: a regrowth bloom. The ally's regeneration comes in three tiers, each as the last runs out, and when
	 * the third opens the bloom flares: fire on the enemies that stand close round the ally. The look: green sparks
	 * budding over the ally, then a gold flare.
	 */
	@Pair(a = "ember_rest", b = "regrowth", name = "Second Flush", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "The ally the spell struck (you, if none) gets Regeneration I for 3 seconds, then II for 3 seconds, then III for "
			+ "2 seconds. When the third tier opens, enemies within 2.5 blocks of the ally take 2 fire damage (times power) "
			+ "and burn for 2 seconds.")
	public static void secondFlush(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.effect(anchor, MobEffects.REGENERATION, 3, 0);
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, PairCast.mid(anchor), 0.8F, 1.3F);
		// Sparks bud round the ally while the first tier runs.
		c.every(6, 20, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			c.ring(PairCast.shift(0x9BE37A, 0xFFE08A, 0.8F), at.add(0, -0.6 + frame * 0.05, 0), 0.9, 12, frame * 0.3);
			c.particles(ParticleTypes.HAPPY_VILLAGER, at.add(0, 1, 0), 2, 0.5, 0.02);
		});
		c.later(60, () -> {
			if (anchor.isAlive()) {
				c.effect(anchor, MobEffects.REGENERATION, 3, 1);
				c.sound(SoundEvents.AZALEA_LEAVES_STEP, PairCast.mid(anchor), 0.7F, 1.2F);
			}
		});
		c.later(120, () -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			c.effect(anchor, MobEffects.REGENERATION, 2, 2);
			c.sphere(PairCast.shift(0xFFE08A, 0xFF7A1F, 1.1F), at, 2.5, 30);
			c.sound(SoundEvents.BLAZE_SHOOT, at, 0.7F, 1.1F);
			c.shake(at, 0.15F, 6);
			for (LivingEntity e : c.enemiesNear(at, 2.5)) {
				c.burn(e, 2 * c.power);
				c.ignite(e, 2);
			}
		});
	}

	/**
	 * Rushbrew: a haste draught for the ally, and three flasks lobbed back at the enemies round it, one at a time.
	 * The look: a gold arc of flask from the ally to its target, and an orange burst where it lands.
	 */
	@Pair(a = "haste", b = "quickbrew", name = "Rushbrew", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "The ally the spell struck (you, if none) gets Haste II for 10 seconds. Three flasks are lobbed a second apart "
			+ "(the first at once), each at the enemy nearest the ally within 6 blocks: 2 magic damage (times power) and "
			+ "alight for 2 seconds.")
	public static void rushbrew(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.effect(anchor, MobEffects.HASTE, 10, 1);
		c.sound(SoundEvents.BREWING_STAND_BREW, PairCast.mid(anchor), 0.7F, 1.4F);
		c.every(20, 3, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 from = PairCast.mid(anchor);
			LivingEntity target = c.nearestEnemy(from, 6, null);
			if (target == null) {
				return;
			}
			Vec3 to = PairCast.mid(target);
			c.arc(PairCast.shift(0xFFD36B, 0xFF5A1F, 0.9F), from, to, 1.5, 12);
			c.sound(SoundEvents.BUCKET_EMPTY, from, 0.5F, 0.9F);
			c.later(8, () -> {
				if (!c.here(target)) {
					return;
				}
				c.hurt(target, 2 * c.power);
				c.ignite(target, 2);
				c.sphere(PairCast.shift(0xFFD36B, 0xFF5A1F, 0.9F), to, 0.9, 16);
				c.sound(SoundEvents.GLASS_BREAK, to, 0.6F, 1.3F);
			});
		});
	}

	/**
	 * Hearthtether: a warm line runs between you and the ally for ten seconds. Each second it warms you both, and
	 * whatever stands in the middle of it burns. The look: a copper line strung between you, with a flame helix
	 * coiled round its middle.
	 */
	@Pair(a = "barnwarmth", b = "warm_cloak", name = "Hearthtether", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A warm tether runs from you to the ally the spell struck (you, if none). Ten times, a second apart (the first at "
			+ "once), you and that ally each heal 1 (times power), and enemies within 2 blocks (times radius) of the tether's "
			+ "middle take 1 fire damage (times power) and burn for 1 second.")
	public static void hearthtether(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity anchor = anchorOf(c);
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, PairCast.mid(anchor), 0.9F, 1.2F);
		c.every(20, 10, frame -> {
			if (!self.isAlive() || !anchor.isAlive()) {
				return;
			}
			Vec3 a = PairCast.mid(self);
			Vec3 b = PairCast.mid(anchor);
			Vec3 middle = a.lerp(b, 0.5);
			c.heal(self, 1 * c.power);
			if (anchor != self) {
				c.heal(anchor, 1 * c.power);
			}
			c.line(PairCast.shift(0xFFB347, 0xFFF1C2, 0.6F), a, b, 3);
			c.helix(PairCast.dust(0xFFB347, 0.8F), ParticleTypes.FLAME, middle.add(0, -0.4, 0), 0.35, 1.0, 1.0, 16);
			c.sound(SoundEvents.FIRECHARGE_USE, middle, 0.5F, 1.0F + 0.05F * frame);
			for (LivingEntity e : c.enemiesNear(middle, 2 * c.radius)) {
				c.burn(e, 1 * c.power);
				c.ignite(e, 1);
			}
		});
	}

	/**
	 * Gilt Bait: a gleaming lure where the spell lands. Enemies it strikes forget whom they were after and are drawn
	 * to the bait for four seconds; those that reach it are struck and slowed. The look: gold threads running from each
	 * enemy to a bright gold ring on the ground.
	 */
	@Pair(a = "gold_parley", b = "lure", name = "Gilt Bait", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 8 enemies it strikes lose their target and are drawn to where the spell landed for 4 seconds (bosses are not moved). "
			+ "Each one that comes within 1.5 blocks of that spot takes 3 magic damage (times power) and gets Slowness II for 2 seconds.")
	public static void giltBait(PairCast c) {
		Vec3 bait = c.point();
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Set<LivingEntity> snared = new HashSet<>();
		c.sound(SoundEvents.NOTE_BLOCK_BELL, bait, 0.8F, 1.6F);
		c.every(2, 40, frame -> {
			c.ring(PairCast.shift(0xFFD35A, 0xFFF6C2, 0.9F), bait.add(0, 0.2, 0), 1.5, 20, frame * 0.25);
			for (LivingEntity t : c.still(struck)) {
				if (!c.movable(t)) {
					continue;
				}
				if (t instanceof Mob mob) {
					mob.setTarget(null);
				}
				Vec3 at = PairCast.mid(t);
				if (at.distanceTo(bait) > 1.5) {
					c.pullTo(t, bait, 0.15);
					if (frame % 5 == 0) {
						c.line(PairCast.dust(0xFFD35A, 0.6F), at, bait, 3);
					}
				} else if (snared.add(t)) {
					c.hurt(t, 3 * c.power);
					c.effect(t, MobEffects.SLOWNESS, 2, 1);
					c.sphere(PairCast.shift(0xFFF6C2, 0xFFD35A, 0.8F), at, 0.8, 12);
					c.sound(SoundEvents.BELL_BLOCK, at, 0.6F, 1.2F);
				}
			}
		});
	}

	/**
	 * Rift Step: you step to where the spell landed, and the place you left stays a rift for four seconds, hurting and
	 * slowing whatever comes near it. The look: a violet spiral winding up under you, a flash where you come down, and
	 * a turning ring left behind on the ground.
	 */
	@Pair(a = "blink", b = "portal_sense", name = "Rift Step", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "Steps you to where the spell landed if that is within 40 blocks, no wall is in the way and there is room to stand: 0.4 seconds later. "
			+ "Where you stood a rift stays for four seconds: once a second, enemies within 2 blocks of it take 1 magic damage "
			+ "(times power) and get Slowness I for 1 second.")
	public static void riftStep(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = PairCast.mid(self);
		Vec3 to = c.point();
		if (from.distanceTo(to) > 40) {
			return;
		}
		HitResult wall = c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			return;
		}
		c.every(2, 3, frame -> {
			c.spiral(PairCast.shift(0x6A4CFF, 0x9FE7FF, 1.0F), from.add(0, 0.1, 0), 1.2 - frame * 0.3, 2.0, 1.0, 18);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.4F, 1.4F + 0.2F * frame);
		});
		c.later(8, () -> {
			if (!self.isAlive() || !c.blink(self, to)) {
				return;
			}
			Vec3 landed = PairCast.mid(self);
			c.sphere(PairCast.shift(0x9FE7FF, 0x6A4CFF, 1.0F), landed, 1.5, 28);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, landed, 0.8F, 0.9F);
			c.punch(0.15F);
			c.every(20, 4, frame -> {
				c.ring(PairCast.dust(0x6A4CFF, 1.0F), from.add(0, 0.1, 0), 2.0 - frame * 0.2, 22, frame * 0.5);
				if (frame == 0) {
					c.sound(SoundEvents.PORTAL_AMBIENT, from, 0.5F, 0.8F);
				}
				for (LivingEntity e : c.enemiesNear(from, 2)) {
					c.hurt(e, 1 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 1, 0);
				}
			});
		});
	}

	/**
	 * Bastion Call: a rampart of crimson calls the enemies it strikes onto you. They are weakened and turn on the caster
	 * for five seconds, re-aimed every second; the caster stands behind a Resistance that lasts as long. The look: a
	 * dark red ring rising round you, and a thread from each enemy to you.
	 */
	@Pair(a = "fortress_sense", b = "taunt", name = "Bastion Call", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 6 enemies it strikes take 2 magic damage (times power) and get Weakness I for 5 seconds. For those 5 "
			+ "seconds they turn on you (monsters only), re-aimed every second. You get Resistance I for 5 seconds.")
	public static void bastionCall(PairCast c) {
		LivingEntity self = c.caster;
		List<LivingEntity> called = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : called) {
			c.hurt(t, 2 * c.power);
			c.effect(t, MobEffects.WEAKNESS, 5, 0);
		}
		c.effect(self, MobEffects.RESISTANCE, 5, 0);
		c.sound(SoundEvents.RAVAGER_ROAR, PairCast.mid(self), 0.6F, 1.4F);
		c.every(20, 5, frame -> {
			if (!self.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(self);
			c.ring(PairCast.shift(0x8B1A1A, 0x2A0A0A, 1.0F), at.add(0, 0.2, 0), 2.5, 24, frame * 0.4);
			for (LivingEntity t : c.still(called)) {
				if (t instanceof Mob mob && c.movable(mob)) {
					mob.setTarget(self);
				}
				c.line(PairCast.dust(0xB3262B, 0.6F), PairCast.mid(t), at, 2);
			}
		});
		c.later(100, () -> {
			for (LivingEntity t : called) {
				if (t instanceof Mob mob && c.movable(mob) && mob.getTarget() == self) {
					mob.setTarget(null);
				}
			}
		});
	}

	/**
	 * Cinderfall: an ember strikes what it lands on, and then three falls of cinder come down on the spot, each one
	 * setting the enemies round it alight again. The look: a fiery column that drops in steps, and a ring of sparks
	 * spreading with each fall.
	 */
	@Pair(a = "cinder_sieve", b = "ember", name = "Cinderfall", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 8 enemies it strikes take 3 fire damage (times power) and burn for 3 seconds. Then three cinder falls, half "
			+ "a second apart: each sets the enemies within 3 blocks (times radius) of the spot alight for 3 seconds and deals "
			+ "2 fire damage (times power).")
	public static void cinderfall(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : struck) {
			c.burn(t, 3 * c.power);
			c.ignite(t, 3);
		}
		c.sound(SoundEvents.FIRECHARGE_USE, spot, 0.9F, 0.8F);
		c.every(10, 4, frame -> {
			if (frame == 0) {
				c.column(PairCast.shift(0xFFD36B, 0xFF5A1F, 0.9F), spot, r * 0.6, 4, 24);
				return;
			}
			c.ring(PairCast.dust(0xFF7A1F, 1.0F), spot.add(0, 0.1, 0), r * (1 - frame * 0.15), 26, frame * 0.7);
			c.particles(ParticleTypes.FLAME, spot.add(0, 3 - frame * 0.6, 0), 8, r * 0.5, 0.05);
			c.sound(SoundEvents.BLAZE_SHOOT, spot, 0.6F, 0.9F + 0.15F * frame);
			for (LivingEntity e : c.enemiesNear(spot, r)) {
				c.burn(e, 2 * c.power);
				c.ignite(e, 3);
			}
		});
	}

	/**
	 * Hearthcrust: a warm loaf for the ally. It is healed at once; the dough rises for two seconds, and then the crust
	 * bursts: allies round the spot are healed again, and enemies there are scorched with crumbs of heat. The look:
	 * a golden sphere swelling, then a brown-gold burst.
	 */
	@Pair(a = "bakehouse", b = "heal", name = "Hearthcrust", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Heals the ally the spell struck (you, if none) 3 health (times power) at once. Two seconds later the crust bursts: allies within "
			+ "3 blocks (times radius) of where it was made heal 2 (times power), and enemies there take 2 magic damage (times power).")
	public static void hearthcrust(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		Vec3 spot = PairCast.mid(anchor);
		double r = 3 * c.radius;
		c.heal(anchor, 3 * c.power);
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, spot, 0.6F, 0.8F);
		c.every(4, 5, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			c.sphere(PairCast.shift(0xFFE2A8, 0xC97A3A, 0.9F), PairCast.mid(anchor), 0.6 + frame * 0.25, 14);
		});
		c.later(40, () -> {
			c.wave(PairCast.dust(0xFFE2A8, 0.9F), spot, 20, 0.35);
			c.sphere(PairCast.shift(0xFFF1C2, 0xC97A3A, 1.2F), spot, r * 0.6, 30);
			c.sound(SoundEvents.BUBBLE_POP, spot, 0.9F, 0.7F);
			c.shake(spot, 0.2F, 5);
			for (LivingEntity a : c.alliesNear(spot, r)) {
				c.heal(a, 2 * c.power);
			}
			for (LivingEntity e : c.enemiesNear(spot, r)) {
				c.hurt(e, 2 * c.power);
			}
		});
	}

	/**
	 * Forgeblade: a blade of molten slag. The enemies it strikes are seared; then five strokes sweep from your hand
	 * to the spot, each cutting whatever stands near the line. The look: a fiery arc drawn across the ground at every
	 * stroke, with sparks where it bites.
	 */
	@Pair(a = "searing_edge", b = "smelt", name = "Forgeblade", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 6 enemies it strikes take 3 fire damage (times power) and burn for 2 seconds. Then five strokes, half a "
			+ "second apart (the first at once), sweep from you to the spot: each enemy within 1.5 blocks of that line takes "
			+ "2 fire damage (times power) and burns for 1 second.")
	public static void forgeblade(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 spot = c.point();
		List<LivingEntity> seared = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : seared) {
			c.burn(t, 3 * c.power);
			c.ignite(t, 2);
		}
		c.sound(SoundEvents.FLINTANDSTEEL_USE, spot, 0.8F, 0.7F);
		c.every(10, 5, frame -> {
			if (!self.isAlive()) {
				return;
			}
			Vec3 hand = PairCast.mid(self).add(0, -0.2, 0);
			double length = hand.distanceTo(spot);
			Vec3 middle = hand.lerp(spot, 0.5);
			c.arc(PairCast.shift(0xFFD36B, 0xFF4A1F, 0.9F), hand, spot, 1.5 - frame * 0.2, 14);
			c.particles(ParticleTypes.LAVA, spot, 3, 0.4, 0.0);
			c.sound(SoundEvents.BLAZE_SHOOT, middle, 0.5F, 1.0F + 0.1F * frame);
			for (LivingEntity e : c.enemiesNear(middle, length / 2 + 1.5)) {
				if (distToSegment(PairCast.mid(e), hand, spot) <= 1.5) {
					c.burn(e, 2 * c.power);
					c.ignite(e, 1);
				}
			}
		});
	}
}
