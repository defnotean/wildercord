package dev.wildercord.pairs.b006;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Batch six: ten hand-made pairs, each with its own mechanic, look and sound. */
public final class Pairs006 {
	private Pairs006() {}

	/**
	 * Stormlance: the wind shoves the enemies in front of you, and a lance of lightning falls on each one
	 * shoved. The look: a white gust rush, then a yellow spear dropping from the sky onto each target.
	 */
	@Pair(a = "dash", b = "jolt", name = "Stormlance", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Shoves up to 8 enemies along your facing and up. 0.4 seconds later a lance of lightning falls on each: "
			+ "5 damage and Slowness IV for 1 second. Every other enemy within 1.5 blocks takes 3.")
	public static void dashJolt(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 forward = c.dir();
		for (LivingEntity t : struck) {
			c.push(t, forward.scale(1.1).add(0, 0.35, 0));
		}
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, c.origin(), 0.8F, 1.4F);
		// Frames 0 and 1 raise the lance above each target; frame 2, 8 ticks on, is the strike.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				if (frame < 2) {
					Vec3 high = at.add(0, 4.0 - frame * 2.0, 0);
					c.line(PairCast.shift(0xFFFFFF, 0xFFE94A, 0.9F), high, at, 3);
					c.particles(ParticleTypes.GUST, high, 1, 0, 0);
				} else {
					lance(c, t, at);
				}
			}
		});
	}

	private static void lance(PairCast c, LivingEntity t, Vec3 at) {
		c.shock(t, 5 * c.power);
		c.effect(t, MobEffects.SLOWNESS, 1, 3);
		c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 4, 0), at, 0.5, 3);
		c.particles(ParticleTypes.ELECTRIC_SPARK, at, 16, 0.4, 0.2);
		c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.6F, 1.3F);
		c.shake(at, 0.2F, 6);
		for (LivingEntity near : c.enemiesNear(at, 1.5 * c.radius)) {
			if (near != t) {
				c.shock(near, 3 * c.power);
				c.line(PairCast.dust(0xFFF6A0, 0.8F), at, PairCast.mid(near), 4);
			}
		}
	}

	/**
	 * Echo Lightning: the strike is rewound and played again. Each target is struck now, then a gold clock ring
	 * closes on it for five seconds, and the same strike falls again. The look: a bright bolt, then a slow ring
	 * tightening, then a blue-gold sphere flash at the second strike.
	 */
	@Pair(a = "lightning", b = "rewind", name = "Echo Lightning", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A lightning strike on each of up to 8 enemies: 7 damage, Slowness II and burning for 2 seconds. "
			+ "Five seconds later the strike echoes onto each still alive: 7 more, and 3 to every enemy within 2 blocks.")
	public static void lightningRewind(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : struck) {
			Vec3 at = PairCast.mid(t);
			c.shock(t, 7 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 2, 1);
			c.ignite(t, 2);
			c.bolt(at);
			c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, at, 0.35F, 1.6F);
		}
		// The clock: five frames, a second apart, of a gold ring closing on each target.
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(struck)) {
				c.ring(PairCast.shift(0xFFE27A, 0x6FD3FF, 0.9F), t.position().add(0, 0.2, 0), 1.4 - frame * 0.2, 20, frame * 0.4);
			}
		});
		c.later(100, () -> echo(c, struck));
	}

	private static void echo(PairCast c, List<LivingEntity> struck) {
		for (LivingEntity t : c.still(struck)) {
			Vec3 at = PairCast.mid(t);
			c.shock(t, 7 * c.power);
			c.bolt(at);
			c.particles(ParticleTypes.REVERSE_PORTAL, at, 20, 0.5, 0.3);
			c.sphere(PairCast.shift(0x6FD3FF, 0xFFE27A, 0.9F), at, 1.0, 26);
			for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
				if (near != t) {
					c.shock(near, 3 * c.power);
				}
			}
		}
		c.sound(SoundEvents.BELL_RESONATE, c.point(), 0.9F, 0.6F);
		c.shake(c.point(), 0.3F, 8);
	}

	/**
	 * Grounding Chain: lightning does not hop from enemy to enemy at random: it is pinned. Each jump holds the
	 * enemy it reaches in Slowness, and the chain ends on a purple column. The look: zigzags that snap from one
	 * target to the next, a violet ring under each, chain clinks getting higher.
	 */
	@Pair(a = "anchor", b = "shock", name = "Grounding Chain", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Lightning arcs from an enemy to up to four more, each jump within 4 blocks of the last: 4 damage and "
			+ "Slowness III for 2 seconds each. The last in the chain takes 4 more.")
	public static void anchorShock(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		List<LivingEntity> chain = new ArrayList<>();
		chain.add(first);
		Vec3 last = PairCast.mid(first);
		while (chain.size() < 5) {
			LivingEntity next = null;
			for (LivingEntity n : c.enemiesNear(last, 4 * c.radius)) {
				if (!chain.contains(n)) {
					next = n;
					break;
				}
			}
			if (next == null) {
				break;
			}
			chain.add(next);
			last = PairCast.mid(next);
		}
		jump(c, chain, 0);
		for (int i = 1; i < chain.size(); i++) {
			int hop = i;
			c.later(3 * i, () -> jump(c, chain, hop));
		}
	}

	private static void jump(PairCast c, List<LivingEntity> chain, int i) {
		LivingEntity t = chain.get(i);
		if (!c.here(t)) {
			return;
		}
		Vec3 at = PairCast.mid(t);
		Vec3 from = i == 0 ? at.add(0, 3, 0) : PairCast.mid(chain.get(i - 1));
		c.zigzag(ParticleTypes.ELECTRIC_SPARK, from, at, 0.4, 3);
		c.ring(PairCast.dust(0x9B5CFF, 1.0F), t.position().add(0, 0.1, 0), 0.9, 14, i * 0.3);
		c.sound(SoundEvents.CHAIN_PLACE, at, 0.9F, 0.9F + i * 0.12F);
		c.shock(t, 4 * c.power);
		c.effect(t, MobEffects.SLOWNESS, 2, 2);
		if (i == chain.size() - 1) {
			c.shock(t, 4 * c.power);
			c.column(PairCast.shift(0x9B5CFF, 0xFFE94A, 0.9F), at, 0.8, 2.0, 22);
			c.shake(at, 0.25F, 6);
		}
	}

	/**
	 * Second Wind: the heal is a breath that clears the ally. Each healed ally is lifted from any foe standing on
	 * it, with a gust, and the heal settles into a green ring. The look: a rising spiral of green sparkle, a
	 * white gust shove, then a settling ring.
	 */
	@Pair(a = "heal", b = "push", name = "Second Wind", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Heals each ally 8 health and gives 2 absorption for 10 seconds. Then each ally with an enemy within "
			+ "2.5 blocks is knocked clear of it by a gust.")
	public static void healPush(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : allies) {
			c.heal(a, 8 * c.power);
			c.absorb(a, 2 * c.power, 10);
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, c.origin(), 0.6F, 1.5F);
		c.every(4, 3, frame -> {
			for (LivingEntity a : c.still(allies)) {
				Vec3 at = PairCast.mid(a);
				if (frame == 0) {
					c.spiral(PairCast.shift(0x9CFFB0, 0xFFFFFF, 0.9F), a.position(), 0.8, 2.0, 2, 24);
					c.particles(ParticleTypes.HAPPY_VILLAGER, at, 8, 0.4, 0.1);
				} else if (frame == 1) {
					LivingEntity foe = c.nearestEnemy(at, 2.5 * c.radius, a);
					if (foe != null) {
						c.knockFrom(a, PairCast.mid(foe), 0.9, 0.3);
						c.wave(ParticleTypes.CLOUD, at, 16, 0.25);
						c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.7F, 1.2F);
					}
				} else {
					c.ring(PairCast.dust(0xDFFFE8, 1.0F), a.position().add(0, 0.2, 0), 1.1, 18, 0);
					c.particles(ParticleTypes.HEART, at.add(0, 0.8, 0), 2, 0.3, 0.02);
				}
			}
		});
	}

	/**
	 * Miasma Gust: a spore gust that throws the enemies away and poisons them, and the spores that were thrown
	 * settle into a cloud where the spell landed. The look: a green fan of spores on the wind, a disc of cloud
	 * that thickens on the ground, and bubbles popping in it.
	 */
	@Pair(a = "push", b = "venom", name = "Miasma Gust", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Hurls up to 8 enemies away from the spell and poisons each: 2 damage and Poison I for 3 seconds. "
			+ "A cloud settles where it landed: 1 damage to enemies within 2 blocks, at once and at 0.5 and 1 seconds.")
	public static void pushVenom(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 origin = c.origin();
		Vec3 point = c.point();
		c.sound(SoundEvents.BREWING_STAND_BREW, origin, 0.7F, 1.4F);
		c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, origin, 14, 0.5, 0.05);
		for (LivingEntity t : hit) {
			c.line(PairCast.dust(0x7CC43A, 0.9F), origin, PairCast.mid(t), 3);
			c.hurt(t, 2 * c.power);
			c.effect(t, MobEffects.POISON, 3, 0);
			c.knockFrom(t, origin, 1.1, 0.25);
		}
		// The cloud: three frames, half a second apart, the disc thickening and the poison hanging in it.
		c.every(10, 3, frame -> {
			c.disc(PairCast.shift(0xA6E85A, 0x3E6B1E, 1.2F), point, 2 * c.radius, 40);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, point, 10, 1.2, 0.02);
			c.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, point, 0.6F, 0.8F + frame * 0.1F);
			for (LivingEntity n : c.enemiesNear(point, 2 * c.radius)) {
				c.hurt(n, 1 * c.power);
			}
		});
	}

	/**
	 * Ribbon Gale: the gust throws the enemies and opens each one: a wound that bleeds for four seconds. Whoever
	 * lands near another tears that one open as well. The look: a crimson ribbon drawn from the caster to each
	 * target, a red thump where they land, drips of blood each half second.
	 */
	@Pair(a = "bleed", b = "push", name = "Ribbon Gale", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Hurls up to 8 enemies away from the spell and opens each: 2 damage, then 1 more every half second for "
			+ "4 seconds. A pushed enemy that lands within 1.5 blocks of another tears that other open: 2 damage and it bleeds.")
	public static void pushBleed(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 origin = c.origin();
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, origin, 0.9F, 0.8F);
		for (LivingEntity t : hit) {
			c.line(PairCast.shift(0xFF4D5E, 0x7A0014, 0.8F), origin, PairCast.mid(t), 4);
			c.knockFrom(t, origin, 1.0, 0.2);
		}
		// Frame 0 opens each wound; frames 1 to 8, half a second apart, bleed it once more each.
		c.every(10, 9, frame -> {
			for (LivingEntity t : c.still(hit)) {
				Vec3 at = PairCast.mid(t);
				if (frame == 0) {
					c.mark(t, Reactions.Mark.BLEEDING);
					c.hurt(t, 2 * c.power);
				} else {
					c.hurt(t, 1 * c.power);
					c.mote(PairCast.dust(0x8A0F1F, 0.9F), at.add(0, 0.6, 0), new Vec3(0, -0.2, 0));
				}
			}
		});
		c.later(8, () -> tear(c, hit));
	}

	private static void tear(PairCast c, List<LivingEntity> hit) {
		for (LivingEntity t : c.still(hit)) {
			Vec3 at = PairCast.mid(t);
			c.ring(PairCast.dust(0xFF4D5E, 0.9F), t.position().add(0, 0.1, 0), 0.8, 12, 0);
			c.sound(SoundEvents.STONE_HIT, at, 0.6F, 0.9F);
			for (LivingEntity n : c.enemiesNear(at, 1.5 * c.radius)) {
				if (n != t) {
					c.wither(n, 2 * c.power);
					c.mark(n, Reactions.Mark.BLEEDING);
					c.line(PairCast.dust(0x8A0F1F, 0.9F), at, PairCast.mid(n), 3);
				}
			}
		}
	}

	/**
	 * Wingstep: a dash that carries you forward, then the Jump Boost, then a second short step. The look: a
	 * white trail through the air, a spiral of light round you while the boost lasts, and a gust on each step.
	 */
	@Pair(a = "dash", b = "leap", name = "Wingstep", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "Dashes you up to 8 blocks along your facing, or 6 or 4 if that won't fit. Jump Boost III for 15 seconds, "
			+ "and one second later a second step of up to 4 blocks.")
	public static void dashLeap(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 flat = flatForward(c);
		Vec3 start = self.position();
		Vec3 first = dash(c, self, flat, 8, 6, 4);
		c.effect(self, MobEffects.JUMP_BOOST, 15, 2);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, start, 0.9F, 1.2F);
		c.punch(0.15F);
		c.line(PairCast.shift(0xFFFFFF, 0xB7F0FF, 0.8F), start.add(0, 1, 0), first.add(0, 1, 0), 3);
		// Frames 1 to 3: a spiral of light rising round the caster as the boost takes hold.
		c.every(3, 4, frame -> c.helix(PairCast.dust(0xE6FBFF, 0.9F), PairCast.dust(0xB7F0FF, 0.9F),
			self.position(), 0.9, 1.8, 1.0, 12));
		c.later(20, () -> {
			Vec3 from = self.position();
			Vec3 second = dash(c, self, flat, 4);
			c.line(PairCast.shift(0xE6FBFF, 0xB7F0FF, 0.8F), from.add(0, 1, 0), second.add(0, 1, 0), 3);
			c.sound(SoundEvents.WIND_CHARGE_BURST, from, 0.8F, 1.3F);
			c.ring(PairCast.dust(0xE6FBFF, 1.0F), second.add(0, 0.2, 0), 1.0, 16, 0);
		});
	}

	/** The caster's direction flattened onto the ground: a unit vector, never zero. */
	private static Vec3 flatForward(PairCast c) {
		Vec3 d = c.dir();
		Vec3 f = new Vec3(d.x, 0, d.z);
		if (f.lengthSqr() < 1.0E-4) {
			Vec3 look = c.caster.getLookAngle();
			f = new Vec3(look.x, 0, look.z);
		}
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** Blinks {@code e} the first of {@code distances} blocks along {@code flat} that fits; returns where it stands. */
	private static Vec3 dash(PairCast c, LivingEntity e, Vec3 flat, double... distances) {
		Vec3 from = e.position();
		for (double d : distances) {
			if (c.blink(e, from.add(flat.scale(d)))) {
				return e.position();
			}
		}
		return from;
	}

	/**
	 * Eyewall: the wind pulls every enemy in to the eye of a whirlwind, spins them there, then flings them out.
	 * The look: a pale teal ring drawing in on the eye, a column of spin rings, and a grey burst outward.
	 */
	@Pair(a = "cyclone", b = "pull", name = "Eyewall", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Pulls up to 8 enemies within 3 blocks into the eye, over half a second. They spin there: 1 damage and "
			+ "a lift, four times at half-second steps. Then the eye flings them out for 3 damage.")
	public static void cyclonePull(PairCast c) {
		Vec3 eye = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(eye, 3 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.TRIDENT_RIPTIDE_1, eye, 0.8F, 0.6F);
		// Seven frames, half a second apart: two pulls, four spins, then the fling.
		c.every(10, 7, frame -> {
			if (frame < 6) {
				c.ring(PairCast.shift(0xB8F5EE, 0x5B6B7A, 0.9F), eye.add(0, 0.3, 0), Math.max(0.6, 3 * c.radius - frame * 0.5), 20, frame * 0.5);
			}
			if (frame == 6) {
				c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, eye, 0.9F, 0.8F);
				c.shake(eye, 0.3F, 8);
				c.wave(PairCast.dust(0xB8F5EE, 1.0F), eye, 24, 0.35);
			}
			for (LivingEntity t : c.still(caught)) {
				if (frame < 2) {
					c.pullTo(t, eye, 1.2);
					c.line(PairCast.dust(0xB8F5EE, 0.7F), PairCast.mid(t), eye, 2);
				} else if (frame < 6) {
					c.lift(t, 0.3);
					c.hurt(t, 1 * c.power);
				} else {
					c.knockFrom(t, eye, 1.2, 0.5);
					c.hurt(t, 3 * c.power);
				}
			}
		});
	}

	/**
	 * Thousand Cuts: six slashes of wind in a tenth of a second, each one crossing the target the other way, and
	 * the last leaves a wound that weeps. The look: crimson slashes alternating across each target, a star of
	 * red sparks on the sixth.
	 */
	@Pair(a = "gash", b = "windcut", name = "Thousand Cuts", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Six slashes of wind a tenth of a second apart across up to 8 enemies: 1 damage each. The sixth leaves a "
			+ "wound: 3 more damage, Wither I for 4 seconds, and it is left bleeding.")
	public static void gashWindcut(PairCast c) {
		List<LivingEntity> cut = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 side = sideways(c);
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, c.origin(), 0.8F, 1.3F);
		c.every(2, 6, frame -> {
			int sign = frame % 2 == 0 ? 1 : -1;
			for (LivingEntity t : c.still(cut)) {
				Vec3 at = PairCast.mid(t);
				Vec3 s = side.scale(sign * 0.9);
				c.line(PairCast.shift(0xFFD1D1, 0x8A0F1F, 0.7F), at.subtract(s), at.add(s), 4);
				c.particles(ParticleTypes.SWEEP_ATTACK, at, 1, 0, 0);
				c.strike(t, 1 * c.power);
				if (frame == 5) {
					c.wither(t, 3 * c.power);
					c.effect(t, MobEffects.WITHER, 4, 0);
					c.mark(t, Reactions.Mark.BLEEDING);
					c.particles(ParticleTypes.CRIT, at, 14, 0.4, 0.3);
					c.star(PairCast.dust(0xFF2A3D, 1.0F), at, 6, 1.2, 0);
					c.sound(SoundEvents.SHIELD_BLOCK, at, 0.6F, 0.7F);
				}
			}
		});
	}

	/** A unit vector across the caster's flat facing: which way the slashes cross. */
	private static Vec3 sideways(PairCast c) {
		Vec3 f = flatForward(c);
		return new Vec3(-f.z, 0, f.x);
	}

	/**
	 * Bulwark Ward: a light barrier on each ally, and the barrier throws back whatever comes close. The look: a
	 * gold dome over each ally that pulses each second, and white sparks where the enemy is hurled.
	 */
	@Pair(a = "barrier", b = "repel", name = "Bulwark Ward", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Each ally in the spell gets 4 absorption (2 hearts) for 20 seconds. Then six pulses, one a second for five "
			+ "seconds: each enemy within 2.5 blocks of an ally is hurled away from it and takes 1 damage.")
	public static void barrierRepel(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : allies) {
			c.absorb(a, 4 * c.power, 20);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.8F, 1.2F);
		c.every(20, 6, frame -> {
			for (LivingEntity a : c.still(allies)) {
				Vec3 at = PairCast.mid(a);
				if (frame == 0) {
					c.sphere(PairCast.shift(0xFFF3C4, 0xFFFFFF, 1.0F), at, 1.3, 36);
					c.tint(at, 6, 0xFFE9A0, 10);
				} else {
					c.sphere(PairCast.dust(0xFFE9A0, 0.8F), at, 1.3, 18);
				}
				for (LivingEntity foe : c.enemiesNear(at, 2.5 * c.radius)) {
					c.knockFrom(foe, at, 1.2, 0.3);
					c.hurt(foe, 1 * c.power);
					c.wave(ParticleTypes.END_ROD, PairCast.mid(foe), 12, 0.3);
					c.sound(SoundEvents.SHIELD_BLOCK, PairCast.mid(foe), 0.6F, 1.2F);
				}
			}
		});
	}
}
