package dev.wildercord.pairs.b027;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs027 {
	private Pairs027() {}

	/** The ally the spell struck, or the caster when it struck none. */
	private static LivingEntity anchorOf(PairCast c) {
		LivingEntity ally = c.firstAlly();
		return ally != null ? ally : c.caster;
	}

	/**
	 * Sounding Wall: sonar. Three pings roll out from an ally; each one outlines the enemies it reaches, and the last
	 * pins the ones close to the ally to the ground. The look: sand-coloured rings, then a dusty disc under the ally.
	 */
	@Pair(a = "caveward", b = "deepwarn", name = "Sounding Wall", element = "earth", kind = EffectKind.HELPFUL,
		text = "Three sonar pings from the ally the spell struck (you, if none), half a second apart. Each outlines every "
			+ "enemy within 8 blocks with Glowing for 5 seconds. The third also pins enemies within 3 blocks of that ally: "
			+ "Slowness III for 3 seconds.")
	public static void soundingWall(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.sound(SoundEvents.NOTE_BLOCK_BELL, PairCast.mid(anchor), 0.7F, 0.5F);
		c.every(10, 3, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			c.ring(PairCast.dust(0xE3C27A, 0.9F), at, 1.5 + frame * 2.5, 30, frame * 0.4);
			c.sound(SoundEvents.NOTE_BLOCK_BELL, at, 0.8F, 0.6F + 0.3F * frame);
			for (LivingEntity e : c.enemiesNear(at, 8)) {
				c.effect(e, MobEffects.GLOWING, 5, 0);
				c.line(PairCast.dust(0x8A6A3B, 0.6F), at, PairCast.mid(e), 6);
			}
			if (frame == 2) {
				c.disc(PairCast.dust(0xB89A5E, 1.0F), anchor.position(), 3, 40);
				c.shake(at, 0.3F, 10);
				for (LivingEntity e : c.enemiesNear(at, 3)) {
					c.effect(e, MobEffects.SLOWNESS, 3, 2);
				}
			}
		});
	}

	/**
	 * Skipstone: a bounding ally. The ally gets a great spring, then hops three times, and each hop rings out a
	 * shockwave that shields the allies standing near. The look: pale green rings bouncing off the ground.
	 */
	@Pair(a = "leap", b = "surefoot", name = "Skipstone", element = "wind", kind = EffectKind.HELPFUL,
		text = "The ally the spell struck (you, if none) gets Jump Boost III for 10 seconds and hops three times, half a "
			+ "second apart. Each hop rings the ground: allies within 3 blocks gain 2 absorption for 4 seconds.")
	public static void skipstone(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.effect(anchor, MobEffects.JUMP_BOOST, 10, 2);
		c.every(10, 3, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 feet = anchor.position();
			c.lift(anchor, 0.9);
			c.ring(PairCast.shift(0xE6FFF5, 0xB08A55, 1.0F), feet.add(0, 0.1, 0), 2.5, 24, frame);
			c.wave(PairCast.dust(0xD9FFF0, 0.8F), feet, 16, 0.2);
			c.sound(SoundEvents.SLIME_JUMP, feet, 0.9F, 1.0F + 0.2F * frame);
			for (LivingEntity a : c.alliesNear(feet, 3)) {
				c.absorb(a, 2, 4);
			}
		});
	}

	/**
	 * Warmstead: a warm ring on the ground. Inside it allies are kept warm and enemies are weakened. The look:
	 * a column of gold sparks, then a ring of flame turning slowly over twelve seconds.
	 */
	@Pair(a = "hearthglow", b = "hearthguard", name = "Warmstead", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"radius"},
		text = "A warm ring out to 4 blocks (times radius) round the spot for 12 seconds. Each second, allies inside get "
			+ "Regeneration I for 2 seconds, and enemies inside get Weakness I for 2 seconds.")
	public static void hearthward(PairCast c) {
		Vec3 hearth = c.point();
		double r = 4 * c.radius;
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, hearth, 1.0F, 0.9F);
		c.column(PairCast.shift(0xFFE08A, 0xFF7A1F, 1.1F), hearth, r, 3, 40);
		c.every(20, 12, frame -> {
			c.ring(PairCast.shift(0xFFB347, 0xFF5A1F, 1.1F), hearth.add(0, 0.2, 0), r, 28, frame * 0.35);
			if (frame % 3 == 0) {
				c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hearth.add(0, 0.5, 0), 3, r * 0.6, 0.02);
				c.sound(SoundEvents.CAMPFIRE_CRACKLE, hearth, 0.5F, 1.1F);
			}
			for (LivingEntity a : c.alliesNear(hearth, r)) {
				c.effect(a, MobEffects.REGENERATION, 2, 0);
			}
			for (LivingEntity e : c.enemiesNear(hearth, r)) {
				c.effect(e, MobEffects.WEAKNESS, 2, 0);
			}
		});
	}

	/**
	 * Whetfire: an edge that keeps cutting. Each enemy struck is lit, then flings a cinder at the nearest other enemy
	 * close by, again and again. The look: sparks running in zigzags from body to body.
	 */
	@Pair(a = "keenkeep", b = "searing_edge", name = "Whetfire", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 4 enemies: 3 fire damage each and alight for 4 seconds. Each then throws a cinder five times, 2 seconds "
			+ "apart (the first at once), at the nearest other enemy within 3 blocks: 2 fire damage and alight for 2 seconds.")
	public static void whetfire(PairCast c) {
		List<LivingEntity> edged = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : edged) {
			c.burn(t, 3 * c.power);
			c.ignite(t, 4);
			c.ring(PairCast.shift(0xFFE08A, 0xFF5A1F, 0.9F), PairCast.mid(t), 0.8, 14, 0);
		}
		c.sound(SoundEvents.FLINTANDSTEEL_USE, c.point(), 0.8F, 1.2F);
		c.every(40, 5, frame -> {
			for (LivingEntity t : c.still(edged)) {
				Vec3 at = PairCast.mid(t);
				LivingEntity next = c.nearestEnemy(at, 3, t);
				if (next == null) {
					continue;
				}
				Vec3 to = PairCast.mid(next);
				c.zigzag(ParticleTypes.SMALL_FLAME, at, to, 0.25, 4);
				c.burn(next, 2 * c.power);
				c.ignite(next, 2);
				c.sound(SoundEvents.BLAZE_SHOOT, at, 0.5F, 1.2F);
			}
		});
	}

	/**
	 * Anchorline: a void line you reel along. You blink to where the spell lands, and the enemies at the landing are
	 * dragged in and slowed. The look: a violet line drawn out from you, then a dark burst where you come down.
	 */
	@Pair(a = "grapple", b = "long_arm", name = "Anchorline", element = "void", kind = EffectKind.MOVEMENT,
		text = "Moves you to where the spell lands if that is within 12 blocks and the straight line to it is clear of walls: "
			+ "you arrive 0.3 seconds later. Enemies within 2.5 blocks of where you land are drawn in and given Slowness II "
			+ "for 2 seconds.")
	public static void anchorline(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = PairCast.mid(self);
		Vec3 to = c.point();
		if (from.distanceTo(to) > 12) {
			return;
		}
		HitResult wall = c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (wall.getType() != HitResult.Type.MISS) {
			return;
		}
		c.every(2, 3, frame -> {
			c.line(PairCast.shift(0xB9A7FF, 0x2B0B4D, 0.9F), from, to, 2);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.4F, 1.6F + 0.2F * frame);
		});
		c.later(6, () -> {
			if (!self.isAlive() || !c.blink(self, to)) {
				return;
			}
			Vec3 landed = PairCast.mid(self);
			c.sphere(PairCast.shift(0x2B0B4D, 0xB9A7FF, 1.0F), landed, 1.8, 30);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, landed, 0.8F, 0.8F);
			c.punch(0.15F);
			for (LivingEntity e : c.enemiesNear(landed, 2.5)) {
				c.pullTo(e, landed, 0.8);
				c.effect(e, MobEffects.SLOWNESS, 2, 1);
			}
		});
	}

	/**
	 * Hum Charge: a charge humming through an ally. For ten seconds it runs quick, and five pulses heal it and the
	 * nearest other ally. The look: a gold and cyan helix wound round the ally at every pulse.
	 */
	@Pair(a = "dynamo_stride", b = "tinker_hum", name = "Hum Charge", element = "storm", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "The ally the spell struck (you, if none) gets Speed I for 10 seconds. Five pulses, 2 seconds apart (the first "
			+ "at once): it heals 1, and another ally within 4 blocks heals 1 too.")
	public static void humCharge(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.effect(anchor, MobEffects.SPEED, 10, 0);
		c.every(40, 5, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			c.heal(anchor, 1 * c.power);
			c.helix(PairCast.dust(0xFFF6A0, 1.0F), ParticleTypes.ELECTRIC_SPARK, at, 0.6, 2.2, 1.5, 24);
			c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.6F, 1.1F + 0.1F * frame);
			for (LivingEntity a : c.alliesNear(at, 4)) {
				if (a != anchor) {
					c.heal(a, 1 * c.power);
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(a), 0.3, 3);
					break;
				}
			}
		});
	}

	/**
	 * Spore Cloud: a cloud of spores settles on what it strikes and eats at it for five seconds. Whatever dies in
	 * it bursts its spores over the enemies beside it. The look: green motes drifting, then a violet burst.
	 */
	@Pair(a = "sporebloom", b = "venom", name = "Spore Cloud", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 8 enemies it strikes take 3 damage at once and Poison I for 5 seconds, and 1 more damage every second "
			+ "for 5 seconds. One of them that dies bursts its spores: 2 damage to every enemy within 2 blocks of it.")
	public static void sporeCloud(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Set<LivingEntity> burst = new HashSet<>();
		for (LivingEntity t : caught) {
			c.hurt(t, 3 * c.power);
			c.effect(t, MobEffects.POISON, 5, 0);
		}
		c.sound(SoundEvents.BUBBLE_POP, spot, 1.0F, 0.6F);
		c.every(4, 30, frame -> {
			c.sphere(PairCast.shift(0x9BE37A, 0x5B2E7A, 0.9F), spot.add(0, 1, 0), 2.2, 12);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, spot.add(0, 1.5, 0), 6, 2.2, 0.02);
		});
		c.every(20, 6, frame -> {
			for (LivingEntity t : caught) {
				if (!t.isAlive()) {
					if (burst.add(t)) {
						Vec3 at = PairCast.mid(t);
						c.sphere(PairCast.dust(0xB7F58A, 1.0F), at, 1.5, 24);
						c.sound(SoundEvents.BUBBLE_POP, at, 0.8F, 0.7F);
						for (LivingEntity near : c.enemiesNear(at, 2)) {
							c.hurt(near, 2 * c.power);
						}
					}
				} else if (frame > 0 && c.here(t)) {
					c.hurt(t, 1 * c.power);
				}
			}
		});
	}

	/**
	 * Petalstar: petals hang over the spot before they fall. Enemies it strikes are slowed at once; then the petals
	 * drop, striking the enemies around and healing the allies there. The look: pale petals rising, then stars.
	 */
	@Pair(a = "moonpetal", b = "starfall", name = "Petalstar", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Enemies it strikes are Slowness I for 2 seconds at once. 1.5 seconds later the petals fall: 6 damage to every "
			+ "enemy it struck or within 3 blocks (times radius), and 4 health to every ally within that radius.")
	public static void petalstar(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : struck) {
			c.effect(t, MobEffects.SLOWNESS, 2, 0);
		}
		c.every(6, 5, frame -> {
			double rise = frame * 0.25;
			c.ring(PairCast.shift(0xFFE9F5, 0xB48CFF, 1.0F), spot.add(0, 0.5 + rise, 0), r * (1 - frame * 0.1), 20, frame * 0.5);
			c.particles(ParticleTypes.CHERRY_LEAVES, spot.add(0, 2 + rise, 0), 6, r, 0.03);
			c.sound(SoundEvents.AZALEA_LEAVES_STEP, spot, 0.6F, 1.0F + 0.1F * frame);
		});
		c.later(30, () -> {
			Set<LivingEntity> hit = new LinkedHashSet<>(struck);
			hit.addAll(c.enemiesNear(spot, r));
			for (LivingEntity t : hit) {
				if (c.here(t)) {
					c.hurt(t, 6 * c.power);
				}
			}
			for (LivingEntity a : c.alliesNear(spot, r)) {
				c.heal(a, 4 * c.power);
			}
			c.star(ParticleTypes.END_ROD, spot.add(0, 0.5, 0), 8, r, 0.3);
			c.sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, spot, 1.0F, 1.4F);
			c.every(4, 5, frame -> c.particles(ParticleTypes.END_ROD, spot.add(0, 2.2 - frame * 0.4, 0), 6, r * 0.8, 0.02));
		});
	}

	/**
	 * Tithe Ward: a tithe of light. The ally is healed at once, then for six seconds a ward of gold rings out each
	 * second, topping up its absorption and pushing back the enemies close to it. The look: a golden spiral, then rings.
	 */
	@Pair(a = "halo", b = "heal", name = "Tithe Ward", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Heals the ally the spell struck (you, if none) 6 health at once. Six times, a second apart, its absorption is "
			+ "topped up to 2, and enemies within 3 blocks of it are knocked back.")
	public static void titheWard(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.heal(anchor, 6 * c.power);
		c.sound(SoundEvents.PLAYER_LEVELUP, PairCast.mid(anchor), 0.5F, 1.6F);
		c.spiral(PairCast.dust(0xFFF3C4, 1.0F), PairCast.mid(anchor), 0.9, 2.0, 2, 30);
		c.every(20, 6, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			c.absorb(anchor, 2 * c.power, 6);
			c.ring(PairCast.dust(0xFFF3C4, 1.0F), at, 1.0 + frame * 0.6, 26, frame);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.6F, 1.0F + 0.1F * frame);
			for (LivingEntity e : c.enemiesNear(at, 3)) {
				c.knockFrom(e, at, 0.6, 0.15);
			}
		});
	}

	/**
	 * Dewsalve: a salve of cold dew. It heals and gives Regeneration at once, puts out the ally's fire, then three
	 * more doses of dew come down on it. The look: falling drips and a frost sphere round the ally.
	 */
	@Pair(a = "dew_drink", b = "salve", name = "Dewsalve", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Heals the ally the spell struck (you, if none) 2 health, gives it Regeneration I for 8 seconds, and puts out its "
			+ "fire. At 2, 4 and 6 seconds after, it is doused again and healed 1 more.")
	public static void dewsalve(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		c.heal(anchor, 2 * c.power);
		c.douse(anchor);
		c.effect(anchor, MobEffects.REGENERATION, 8, 0);
		c.sound(SoundEvents.GENERIC_SPLASH, PairCast.mid(anchor), 0.8F, 1.4F);
		c.every(4, 40, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(anchor);
			c.particles(ParticleTypes.DRIPPING_WATER, at.add(0, 2.2, 0), 5, 0.6, 0.02);
			c.sphere(PairCast.dust(0xCDEFFF, 0.8F), at, 1.2, 10);
		});
		c.later(40, () -> dose(c, anchor));
		c.later(80, () -> dose(c, anchor));
		c.later(120, () -> dose(c, anchor));
	}

	/** One more dose of dew on the ally: doused and healed 1, with a little shimmer. */
	private static void dose(PairCast c, LivingEntity anchor) {
		if (!anchor.isAlive()) {
			return;
		}
		Vec3 at = PairCast.mid(anchor);
		c.douse(anchor);
		c.heal(anchor, 1 * c.power);
		c.ring(PairCast.shift(0xCDEFFF, 0x7FD8FF, 0.9F), at, 1.1, 18, 0);
		c.sound(SoundEvents.BUCKET_EMPTY, at, 0.5F, 1.5F);
	}
}
