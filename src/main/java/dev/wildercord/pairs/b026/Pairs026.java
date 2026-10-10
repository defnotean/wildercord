package dev.wildercord.pairs.b026;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs026 {
	private Pairs026() {}

	/**
	 * Rekindling Wing: a storm-bird of flame circles the caster and dives on the nearest enemy four times. Each dive
	 * sets that enemy alight and heals whichever ally stands nearest it. The look: a ring of gold sparks turning
	 * round the caster, zigzag lightning down to each dive, and a puff of flame where it lands.
	 */
	@Pair(a = "phoenix_pyre", b = "thunderbird", name = "Rekindling Wing", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "A burning storm-bird circles you for 6 seconds. Four times, every 1.5 seconds, it dives on the enemy nearest you "
			+ "within 10 blocks: 4 fire damage and set alight for 2 seconds. The ally nearest that enemy heals 2 health.")
	public static void rekindlingWing(PairCast c) {
		Vec3 home = PairCast.mid(c.caster);
		c.spiral(PairCast.shift(0xFFB020, 0x7FD8FF, 0.9F), home, 1.5, 2.5, 2, 30);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, home, 0.5F, 1.6F);
		c.every(10, 13, frame -> {
			double a = frame * 0.5;
			Vec3 bird = home.add(Math.cos(a) * 2.5, 2.2 + Math.sin(a * 0.5) * 0.3, Math.sin(a) * 2.5);
			c.particles(ParticleTypes.FLAME, bird, 4, 0.15, 0.02);
			if (frame == 0 || frame % 3 != 0) {
				return;
			}
			LivingEntity t = c.nearestEnemy(home, 10, null);
			if (t == null) {
				return;
			}
			Vec3 at = PairCast.mid(t);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, bird, at, 0.4, 2);
			c.burn(t, 4 * c.power);
			c.ignite(t, 2);
			c.particles(ParticleTypes.FLAME, at, 12, 0.3, 0.1);
			c.sound(SoundEvents.FIRECHARGE_USE, at, 0.8F, 1.0F);
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.3F, 1.6F);
			LivingEntity healed = nearest(c.still(c.allies()), at);
			if (healed != null) {
				c.heal(healed, 2 * c.power);
				c.particles(ParticleTypes.HEART, PairCast.mid(healed).add(0, 0.5, 0), 3, 0.3, 0.02);
			}
		});
	}

	/**
	 * Stormstep: a blink to where the spell landed, and a lightning bolt down on the spot you left a second later.
	 * The look: a cyan afterimage ring shrinking where you stood, then the bolt and a white flash.
	 */
	@Pair(a = "blink", b = "thunderstep", name = "Stormstep", element = "storm", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Blinks you to where the spell landed, up to 24 blocks away. A second later lightning strikes the spot you left: "
			+ "6 damage to every enemy within 2.5 blocks of it, and Slowness III for half a second. No safe spot means no blink.")
	public static void stormstep(PairCast c) {
		Vec3 from = c.caster.position();
		Vec3 spot = c.point();
		if (spot.distanceTo(from) < 1.5) {
			return;
		}
		if (spot.distanceTo(from) > 24) {
			spot = from.add(spot.subtract(from).normalize().scale(24));
		}
		Vec3 left = c.ground(from);
		if (!c.blink(c.caster, c.ground(spot))) {
			return;
		}
		c.sound(SoundEvents.ENDERMAN_TELEPORT, left, 0.6F, 1.4F);
		c.every(4, 5, frame -> c.ring(PairCast.shift(0x9FE8FF, 0x6A7CFF, 0.9F), left.add(0, 0.1, 0), 2.5 * c.radius, 20, frame * 0.4));
		c.later(20, () -> {
			c.bolt(left);
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, left, 0.9F, 1.0F);
			c.shake(left, 0.3F, 6);
			c.tint(left, 3, 0xBFE8FF, 6);
			for (LivingEntity t : c.enemiesNear(left.add(0, 1, 0), 2.5 * c.radius)) {
				c.shock(t, 6 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 0.5, 2);
			}
		});
	}

	/**
	 * Stillshock: each target is held for three seconds, and three times a lightning arc leaps from it to the
	 * nearest other enemy. The look: crackling white-to-violet sparks between the held targets and the arcs.
	 */
	@Pair(a = "jolt", b = "stillbind", name = "Stillshock", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Holds each target for 3 seconds: Slowness III, and a creature forgets its target (bosses are only slowed). "
			+ "Three times, once a second, each held target takes 2 shock, and a lightning arc leaps from it to the nearest other "
			+ "enemy within 4 blocks, where it deals 2 more shock.")
	public static void stillshock(PairCast c) {
		List<LivingEntity> held = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : held) {
			c.effect(t, MobEffects.SLOWNESS, 3, 2);
			stopMind(c, t);
		}
		c.sound(SoundEvents.CONDUIT_ACTIVATE, c.point(), 0.6F, 1.6F);
		c.every(4, c.ticks(3) / 4, frame -> {
			for (LivingEntity t : c.still(held)) {
				stopMind(c, t);
				if (frame % 5 != 0) {
					continue;
				}
				Vec3 at = PairCast.mid(t);
				c.sphere(PairCast.shift(0xF4F7FF, 0x6A3DFF, 0.8F), at, 0.9, 16);
				c.shock(t, 2 * c.power);
				LivingEntity next = c.nearestEnemy(at, 4, t);
				if (next != null) {
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(next), 0.35, 3);
					c.shock(next, 2 * c.power);
					c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, PairCast.mid(next), 0.4F, 1.4F);
				}
			}
		});
	}

	/**
	 * Drownwell: a well pulls every enemy in towards its point for two seconds, then the ground gives way under them.
	 * The look: a violet well shrinking round the drawn enemies, lines of pull, then a brown collapse disc.
	 */
	@Pair(a = "gravity_well", b = "sinkhole", name = "Drownwell", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Drags up to 8 enemies within 7 blocks of the point towards it for 2 seconds (bosses are not dragged). Then the "
			+ "ground gives way there: each enemy within 2.5 blocks takes 5 damage and Slowness III for 2 seconds.")
	public static void undertow(PairCast c) {
		Vec3 well = c.ground(c.point());
		List<LivingEntity> drawn = PairCast.first(c.enemiesNear(well, 7 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, well, 0.9F, 0.5F);
		c.every(10, 4, frame -> {
			c.disc(PairCast.shift(0x3B1F6B, 0x9C7A4A, 1.0F), well.add(0, 0.1, 0), 7 * c.radius * (1.0 - frame * 0.2), 40);
			for (LivingEntity t : c.still(drawn)) {
				c.pullTo(t, well, 0.9);
				c.line(PairCast.dust(0x6A4BB0, 0.7F), PairCast.mid(t), well.add(0, 0.5, 0), 2);
			}
		});
		c.later(40, () -> {
			c.shake(well, 0.4F, 8);
			c.sound(SoundEvents.DEEPSLATE_BREAK, well, 1.0F, 0.6F);
			c.sound(SoundEvents.GRAVEL_BREAK, well, 0.8F, 0.8F);
			c.disc(PairCast.dust(0x7A5A3A, 1.2F), well, 2.5 * c.radius, 30);
			for (LivingEntity t : c.enemiesNear(well, 2.5 * c.radius)) {
				c.strike(t, 5 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 2);
			}
		});
	}

	/**
	 * Spire Drop: an icicle hangs over each target for 1.5 seconds, then falls on the spot it stood on. Only a target
	 * still close to that spot is hit. The fall chills whoever stands near the impact. The look: pale spikes hanging
	 * in the air, a streak down, a burst of snow and a frost disc.
	 */
	@Pair(a = "icicle", b = "stalactite", name = "Spire Drop", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Icicles hang over each target for 1.5 seconds, then fall on the spot it stood on: 6 freeze damage and soaked, if it "
			+ "is still within 1.5 blocks of that spot. Each fall chills enemies within 2 blocks: Slowness II for 3 seconds.")
	public static void spireDrop(PairCast c) {
		List<LivingEntity> under = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<Vec3> spots = new ArrayList<>();
		for (LivingEntity t : under) {
			spots.add(PairCast.mid(t));
		}
		c.every(5, 7, frame -> {
			for (int i = 0; i < under.size(); i++) {
				LivingEntity t = under.get(i);
				if (!c.here(t)) {
					continue;
				}
				Vec3 spot = spots.get(i);
				Vec3 top = spot.add(0, 6, 0);
				if (frame < 6) {
					c.column(PairCast.shift(0xCFEFFF, 0x7FD8FF, 0.8F), spot.add(0, 5, 0), 0.25, 1.4, 8);
					continue;
				}
				c.line(PairCast.dust(0xCFEFFF, 0.8F), top, spot, 3);
				if (PairCast.mid(t).distanceTo(spot) < 1.5) {
					c.freeze(t, 6 * c.power);
					c.mark(t, Reactions.Mark.SOAKED);
				}
				c.particles(ParticleTypes.SNOWFLAKE, spot, 16, 0.9, 0.08);
				c.disc(PairCast.dust(0xCFEFFF, 1.0F), c.ground(spot), 2 * c.radius, 24);
				c.sound(SoundEvents.POINTED_DRIPSTONE_LAND, spot, 1.0F, 0.8F);
				for (LivingEntity near : c.enemiesNear(spot, 2 * c.radius)) {
					c.effect(near, MobEffects.SLOWNESS, 3, 1);
				}
			}
		});
	}

	/**
	 * Anchorlock: each target is chained to where it stands. It is held for 1.5 seconds, then tethered: pulled back
	 * when it strays and bitten each second. The look: brown chains from the ground anchor to each target.
	 */
	@Pair(a = "shackle", b = "stillbind", name = "Anchorlock", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Anchors each target to the spot it stands on for 5 seconds. For 1.5 seconds it is held (Slowness III, and a creature "
			+ "forgets its target). After that it is pulled back if it strays more than 2 blocks, and bitten for 2 magic damage "
			+ "every second. Bosses are slowed, never held or pulled.")
	public static void anchorlock(PairCast c) {
		List<LivingEntity> chained = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<Vec3> anchors = new ArrayList<>();
		for (LivingEntity t : chained) {
			Vec3 a = c.ground(t.position());
			anchors.add(a);
			c.ring(PairCast.dust(0xB08A5A, 0.8F), a.add(0, 0.1, 0), 0.6, 10, 0);
		}
		c.sound(SoundEvents.CHAIN_PLACE, c.point(), 1.0F, 0.7F);
		c.every(5, 21, frame -> {
			for (int i = 0; i < chained.size(); i++) {
				LivingEntity t = chained.get(i);
				if (!c.here(t)) {
					continue;
				}
				Vec3 anchor = anchors.get(i);
				c.line(PairCast.dust(0xB08A5A, 0.6F), anchor.add(0, 0.3, 0), PairCast.mid(t), 3);
				if (frame <= 6) {
					if (frame == 0) {
						c.effect(t, MobEffects.SLOWNESS, 1.5, 2);
					}
					stopMind(c, t);
					continue;
				}
				double dx = t.getX() - anchor.x;
				double dz = t.getZ() - anchor.z;
				if (Math.sqrt(dx * dx + dz * dz) > 2) {
					c.pullTo(t, anchor, 0.6);
				}
				if (frame % 4 == 0) {
					c.hurt(t, 2 * c.power);
					c.particles(ParticleTypes.CRIT, PairCast.mid(t), 4, 0.3, 0.1);
					c.sound(SoundEvents.CHAIN_HIT, PairCast.mid(t), 0.5F, 1.0F);
				}
			}
		});
	}

	/**
	 * Fenbane: the ground turns to bog under each target and sinks it. The bog festers for four seconds, then the
	 * poison bursts out and passes to the enemies nearest each target. The look: a green-brown disc under each target
	 * and sporeflecks, then a green squelch and lines of poison to the nearby enemies.
	 */
	@Pair(a = "mire", b = "venom", name = "Fenbane", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Sinks each target in mire for 4 seconds: Slowness II and soaked. Each second for 4 seconds the mire festers: 1 magic "
			+ "damage and Poison I for 2 seconds. When it clears, the poison passes to up to 3 other creatures within 2.5 blocks "
			+ "of each target: Poison I for 3 seconds.")
	public static void fenbane(PairCast c) {
		List<LivingEntity> sunk = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : sunk) {
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
			c.mark(t, Reactions.Mark.SOAKED);
		}
		c.sound(SoundEvents.MUD_PLACE, c.point(), 1.0F, 0.7F);
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(sunk)) {
				Vec3 at = PairCast.mid(t);
				c.disc(PairCast.dust(0x4A5A2A, 1.0F), c.ground(at), 1.5, 16);
				if (frame < 4) {
					c.hurt(t, 1 * c.power);
					c.effect(t, MobEffects.POISON, 2, 0);
					c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, at, 6, 0.4, 0.02);
					c.sound(SoundEvents.SLIME_BLOCK_HIT, at, 0.5F, 0.6F);
					continue;
				}
				c.sound(SoundEvents.SLIME_SQUISH, at, 1.0F, 0.6F);
				int passed = 0;
				for (LivingEntity near : c.enemiesNear(at, 2.5)) {
					if (near == t || near instanceof Player || passed >= 3) {
						continue;
					}
					c.line(PairCast.dust(0x9BE35A, 0.8F), at, PairCast.mid(near), 3);
					c.effect(near, MobEffects.POISON, 3, 0);
					passed++;
				}
			}
		});
	}

	/**
	 * Briarcreep: a line of roots creeps along the ground in the direction of the spell. Each enemy it reaches is
	 * struck once and slowed to a crawl. The look: green vines and brown root lines inching forward, with leaf
	 * particles where the front passes.
	 */
	@Pair(a = "root", b = "rootsnare", name = "Briarcreep", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A line of roots creeps 6 blocks along the ground in the direction of the spell. Each enemy it reaches takes 3 damage "
			+ "and is slowed to a crawl (Slowness III, and it forgets its target) for 1.5 seconds. Each enemy is struck once.")
	public static void briarcreep(PairCast c) {
		Vec3 start = c.ground(c.point());
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
		Vec3 along = flat;
		Set<LivingEntity> rooted = new HashSet<>();
		c.every(6, 9, frame -> {
			Vec3 front = c.ground(start.add(along.scale(0.75 * frame)));
			Vec3 behind = c.ground(start.add(along.scale(0.75 * Math.max(0, frame - 1))));
			c.line(PairCast.shift(0x6BBF5A, 0x8A5A2B, 0.8F), behind.add(0, 0.2, 0), front.add(0, 0.2, 0), 3);
			c.particles(PairCast.dust(0x3E7A3A, 0.8F), front.add(0, 0.3, 0), 5, 0.4, 0.02);
			c.sound(SoundEvents.MANGROVE_ROOTS_BREAK, front, 0.5F, 0.8F);
			for (LivingEntity t : c.enemiesNear(front.add(0, 1, 0), 1.5)) {
				if (!rooted.add(t)) {
					continue;
				}
				c.hurt(t, 3 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 1.5, 2);
				stopMind(c, t);
				c.particles(ParticleTypes.COMPOSTER, PairCast.mid(t), 6, 0.4, 0.05);
			}
		});
	}

	/**
	 * Quarrywall: allies nearby are guarded with absorption and Resistance, and four times stone rings out from each
	 * of them to shove the enemies back. The look: a grey-tan ring of rock rolling out from every guarded ally.
	 */
	@Pair(a = "shieldwall", b = "stoneform", name = "Quarrywall", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 5 blocks of you absorb 4 damage and get Resistance I for 8 seconds. Four times, every 2 seconds, stone "
			+ "rings out from each of them: enemies within 3 blocks take 2 damage and are knocked away.")
	public static void quarrywall(PairCast c) {
		List<LivingEntity> guarded = c.alliesNear(PairCast.mid(c.caster), 5 * c.radius);
		for (LivingEntity t : guarded) {
			c.absorb(t, 4 * c.power, 8);
			c.effect(t, MobEffects.RESISTANCE, 8, 0);
		}
		c.sound(SoundEvents.STONE_PLACE, PairCast.mid(c.caster), 1.0F, 0.7F);
		c.every(40, 4, frame -> {
			Set<LivingEntity> shoved = new LinkedHashSet<>();
			for (LivingEntity ally : c.still(guarded)) {
				Vec3 at = PairCast.mid(ally);
				c.ring(PairCast.dust(0xB8A88A, 1.0F), at, 3 * c.radius, 28, frame * 0.3);
				for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
					if (shoved.add(near)) {
						c.strike(near, 2 * c.power);
						c.knockFrom(near, at, 0.9, 0.35);
					}
				}
			}
			c.sound(SoundEvents.STONE_BREAK, PairCast.mid(c.caster), 0.8F, 0.6F + frame * 0.1F);
		});
	}

	/**
	 * Hushfield: calms enemies for six seconds, and every two seconds the calm passes to any enemy near a calmed
	 * one. The look: lilac motes over each calmed creature, and a chime with a thread of light to each new one.
	 */
	@Pair(a = "pacify", b = "soothe", name = "Hushfield", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"duration"},
		text = "Calms each target for 6 seconds: a creature forgets its target and slows to Slowness I (bosses are not calmed), and a player gets Weakness II "
			+ "instead. Every 2 seconds the calm passes to enemies within 3 blocks of a calmed creature.")
	public static void hushfield(PairCast c) {
		Set<LivingEntity> calmed = new LinkedHashSet<>();
		for (LivingEntity t : PairCast.first(c.enemies(), PairCast.MAX_TARGETS)) {
			if (!c.movable(t)) {
				continue;
			}
			if (t instanceof Player) {
				c.effect(t, MobEffects.WEAKNESS, 6, 1);
			} else {
				calmed.add(t);
				c.effect(t, MobEffects.SLOWNESS, 6, 0);
			}
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 1.0F, 1.2F);
		c.every(4, c.ticks(6) / 4 + 1, frame -> {
			if (frame > 0 && frame % 10 == 0) {
				for (LivingEntity seed : new ArrayList<>(calmed)) {
					for (LivingEntity near : c.enemiesNear(PairCast.mid(seed), 3)) {
						if (!(near instanceof Player) && c.movable(near) && calmed.add(near)) {
							c.effect(near, MobEffects.SLOWNESS, 6, 0);
							c.line(PairCast.dust(0xE8C8FF, 0.7F), PairCast.mid(seed), PairCast.mid(near), 3);
						}
					}
				}
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.6F, 1.5F);
			}
			for (LivingEntity t : c.still(calmed)) {
				stopMind(c, t);
				c.particles(PairCast.dust(0xE8C8FF, 0.8F), PairCast.mid(t), 2, 0.4, 0.02);
			}
		});
	}

	/** The living entity in {@code from} nearest {@code at}, or null when there are none. */
	private static LivingEntity nearest(List<LivingEntity> from, Vec3 at) {
		LivingEntity best = null;
		double bestSq = Double.MAX_VALUE;
		for (LivingEntity e : from) {
			double sq = PairCast.mid(e).distanceToSqr(at);
			if (sq < bestSq) {
				bestSq = sq;
				best = e;
			}
		}
		return best;
	}

	/** Makes a mob forget its target and stop pathing; never a boss (the guard in {@link PairCast#movable}). */
	private static void stopMind(PairCast c, LivingEntity t) {
		if (t instanceof Mob mob && c.movable(mob)) {
			mob.setTarget(null);
			mob.getNavigation().stop();
		}
	}
}
