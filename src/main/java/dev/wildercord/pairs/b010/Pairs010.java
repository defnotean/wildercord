package dev.wildercord.pairs.b010;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Batch 10: nine hand-made pairs, each with its own mechanic, palette, sounds and beats. */
public final class Pairs010 {
	private Pairs010() {}

	/**
	 * Hornsong Charge: a war horn sounds where the spell lands. A white wave runs out to the reach, the band in it
	 * gets faster, higher-jumping and stronger, and the horn calls again every few seconds to mend them.
	 */
	@Pair(a = "rally", b = "warcry", name = "Hornsong Charge", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A war horn sounds where it lands. Allies within 8 blocks get Speed I, Jump Boost I and Strength I for 12 seconds. "
			+ "Three more times, every 3 seconds, the horn heals each one still within 8 blocks for 2 health.")
	public static void rallyWarcry(PairCast c) {
		Vec3 at = c.point();
		double reach = 8 * c.radius;
		List<LivingEntity> band = c.alliesNear(at, reach);
		c.sound(SoundEvents.RAID_HORN, at, 1.0F, 1.0F);
		c.shake(at, 0.1F, 10);
		for (LivingEntity a : band) {
			c.effect(a, MobEffects.SPEED, 12, 0);
			c.effect(a, MobEffects.JUMP_BOOST, 12, 0);
			c.effect(a, MobEffects.STRENGTH, 12, 0);
		}
		c.every(60, 4, frame -> {
			if (frame == 0) {
				c.wave(PairCast.shift(0xEAF6FF, 0xB3262E, 1.0F), at, 36, 0.5);
				c.ring(PairCast.shift(0xEAF6FF, 0xFFB347, 0.9F), at, reach, 48, 0);
				return;
			}
			c.sound(SoundEvents.BELL_BLOCK, at, 0.8F, 0.9F + frame * 0.1F);
			for (LivingEntity a : within(c.still(band), at, reach)) {
				c.heal(a, 2 * c.power);
				c.spiral(PairCast.dust(0xB3262E, 1.0F), a.position(), 0.6, 2.0, 2, 24);
				c.particles(ParticleTypes.HEART, PairCast.mid(a).add(0, 0.6, 0), 2, 0.2, 0);
			}
		});
	}

	/**
	 * Ironbait Ward: the enemies hit are turned on you, and you stand in a ring of earth that shields you and the
	 * allies beside you. Every second the ring pushes back any turned enemy that has crept within two blocks.
	 */
	@Pair(a = "shieldwall", b = "taunt", name = "Ironbait Ward", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Enemies hit turn on you for 6 seconds. You and allies within 5 blocks get Resistance I for 8 seconds. "
			+ "Each second, a turned enemy that is within 2 blocks of one of them is knocked back.")
	public static void shieldwallTaunt(PairCast c) {
		Vec3 me = PairCast.mid(c.caster);
		double ring = 5 * c.radius;
		List<LivingEntity> bait = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<LivingEntity> wall = c.alliesNear(me, ring);
		for (LivingEntity a : wall) {
			c.effect(a, MobEffects.RESISTANCE, 8, 0);
		}
		c.sound(SoundEvents.SHIELD_BLOCK, me, 1.0F, 0.7F);
		c.every(20, 7, frame -> {
			Vec3 centre = PairCast.mid(c.caster);
			c.ring(PairCast.shift(0xE2C48A, 0x6B4A2B, 1.0F), centre, ring, 40, frame * 0.25);
			List<LivingEntity> baiting = c.still(bait);
			for (LivingEntity t : baiting) {
				if (t instanceof Mob m && c.movable(m)) {
					m.setTarget(c.caster);
				}
				c.zigzag(PairCast.dust(0xB3262E, 0.9F), PairCast.mid(t), centre, 0.25, 3);
			}
			for (LivingEntity t : baiting) {
				for (LivingEntity a : c.still(wall)) {
					if (PairCast.mid(t).distanceTo(PairCast.mid(a)) <= 2) {
						c.knockFrom(t, PairCast.mid(a), 0.6, 0.2);
						c.sound(SoundEvents.STONE_HIT, PairCast.mid(a), 0.9F, 0.8F);
						c.particles(ParticleTypes.CRIT, PairCast.mid(t), 6, 0.3, 0.1);
						break;
					}
				}
			}
		});
	}

	/**
	 * Borrowed Blade: harm marks each enemy Exposed, and the Strength empower lends the allies can strike it for the
	 * next two seconds. A gold beam, then violet sparks from each ally that joins the blow.
	 */
	@Pair(a = "empower", b = "harm", name = "Borrowed Blade", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Harm: 7 magic damage to each enemy hit, which is Exposed for 3 seconds. Empower: each ally hit gets Strength II "
			+ "for 10 seconds, then Weakness I for 4. For the next two seconds, up to two allies with Strength within 6 blocks "
			+ "of an Exposed enemy each hit it for 2 magic.")
	public static void empowerHarm(PairCast c) {
		List<LivingEntity> edged = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : edged) {
			c.effect(a, MobEffects.STRENGTH, 10, 1);
			c.later(c.ticks(10), () -> {
				if (c.here(a)) {
					c.effect(a, MobEffects.WEAKNESS, 4, 0);
				}
			});
			c.spiral(PairCast.shift(0xFFF4CC, 0xFFD27A, 0.8F), a.position(), 0.6, 1.8, 2, 24);
		}
		for (LivingEntity t : struck) {
			c.hurt(t, 7 * c.power);
			c.mark(t, Reactions.Mark.EXPOSED);
			c.line(PairCast.shift(0xFFF4CC, 0x7A3CC8, 0.9F), c.origin(), PairCast.mid(t), 4);
			c.star(ParticleTypes.ENCHANTED_HIT, PairCast.mid(t), 4, 0.8, 0.3);
		}
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 1.0F, 1.3F);
		c.every(20, 3, frame -> {
			if (frame == 0) {
				return;
			}
			for (LivingEntity t : c.still(struck)) {
				if (!Reactions.has(t, Reactions.Mark.EXPOSED)) {
					continue;
				}
				Vec3 at = PairCast.mid(t);
				int blows = 0;
				for (LivingEntity a : c.alliesNear(at, 6)) {
					if (blows >= 2) {
						break;
					}
					if (a.hasEffect(MobEffects.STRENGTH)) {
						blows++;
						c.zigzag(PairCast.shift(0xFFD27A, 0x7A3CC8, 0.7F), PairCast.mid(a), at, 0.3, 3);
						c.hurt(t, 2 * c.power);
						c.sound(SoundEvents.PLAYER_ATTACK_CRIT, at, 0.9F, 1.1F);
					}
				}
				c.particles(ParticleTypes.ENCHANTED_HIT, at, 4, 0.3, 0.2);
			}
		});
	}

	/**
	 * Dawnstone Vigil: a barrier of light over each ally, and a stone shell that keeps it for ten seconds. When the
	 * stone wears off, the light still left is turned into healing: a white column, then a green bell tone.
	 */
	@Pair(a = "barrier", b = "stoneskin", name = "Dawnstone Vigil", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Each ally hit gets 4 absorption (2 hearts) for 20 seconds, and Resistance II and Slowness I for 10 seconds: stone "
			+ "is heavy. When the stone wears off at 10 seconds, the absorption still left on each of them becomes healing.")
	public static void barrierStoneskin(PairCast c) {
		List<LivingEntity> ward = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : ward) {
			c.absorb(a, 4 * c.power, 20);
			c.effect(a, MobEffects.RESISTANCE, 10, 1);
			c.effect(a, MobEffects.SLOWNESS, 10, 0);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 1.0F, 1.2F);
		c.every(20, 11, frame -> {
			for (LivingEntity a : c.still(ward)) {
				Vec3 feet = a.position();
				if (frame < 10) {
					c.column(PairCast.dust(0xFFF3C4, 0.9F), feet, 0.8, 2.2, 14);
					c.sphere(PairCast.shift(0xFFF3C4, 0x9C9482, 1.0F), PairCast.mid(a), 1.0, 22);
				} else {
					double left = a.getAbsorptionAmount();
					if (left > 0) {
						a.setAbsorptionAmount(0);
						c.heal(a, left);
					}
					c.spiral(PairCast.dust(0x7CFF9A, 1.0F), feet, 0.9, 2.2, 1, 20);
					c.particles(ParticleTypes.HEART, PairCast.mid(a).add(0, 0.8, 0), 2, 0.3, 0);
				}
			}
			if (frame == 10) {
				c.sound(SoundEvents.STONE_BREAK, c.point(), 1.0F, 0.8F);
				c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 0.9F, 1.3F);
			}
		});
	}

	/**
	 * Silent Vow: every creature hit is weakened, and a monster that is not a boss forgets its target until it is
	 * harmed. Then its silence spills: every enemy near it forgets its target for two seconds.
	 */
	@Pair(a = "pacify", b = "silence", name = "Silent Vow", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Each creature hit is Weakened I for 6 seconds. Monsters that are not bosses forget their target for 6 seconds, "
			+ "until one is harmed: then the hush spills to every enemy within 3 blocks, which forgets its target for 2 seconds.")
	public static void pacifySilence(PairCast c) {
		List<LivingEntity> hushed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Map<LivingEntity, Float> health = new HashMap<>();
		Set<LivingEntity> calm = new HashSet<>();
		Map<LivingEntity, Integer> spilled = new HashMap<>();
		for (LivingEntity t : hushed) {
			c.effect(t, MobEffects.WEAKNESS, 6, 0);
			health.put(t, t.getHealth());
			if (t instanceof Mob && c.movable(t)) {
				calm.add(t);
			}
		}
		c.sound(SoundEvents.SCULK_CLICKING, c.point(), 1.0F, 0.5F);
		// Thirteen beats, half a second apart: six seconds of quiet.
		c.every(10, 13, frame -> {
			for (LivingEntity t : c.still(hushed)) {
				Vec3 at = PairCast.mid(t);
				if (calm.contains(t) && t instanceof Mob m) {
					if (t.getHealth() < health.get(t) - 0.01F) {
						calm.remove(t);
						c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 1.0F, 0.5F);
						c.wave(PairCast.shift(0xD8C8FF, 0x6A7080, 0.9F), at, 28, 0.25);
						for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
							if (near != t) {
								spilled.put(near, frame + 4);
								c.line(PairCast.dust(0xB7A8FF, 0.8F), at, PairCast.mid(near), 3);
							}
						}
					} else {
						m.setTarget(null);
						c.ring(PairCast.dust(0xD8C8FF, 0.6F), at.add(0, 0.9, 0), 0.7, 10, frame * 0.4);
						c.particles(ParticleTypes.SQUID_INK, at, 3, 0.3, 0.02);
					}
				}
				health.put(t, t.getHealth());
			}
			for (LivingEntity s : new ArrayList<>(spilled.keySet())) {
				if (frame > spilled.get(s)) {
					spilled.remove(s);
				} else if (s instanceof Mob m && c.here(s) && c.movable(s)) {
					m.setTarget(null);
				}
			}
		});
	}

	/**
	 * Quarry Jaw: fangs snap up round each target and lift it off the ground; a stalactite then drops on the spot it
	 * rose from. Stone falls on a creature that cannot step away, so the drop only misses if it has drifted far.
	 */
	@Pair(a = "fangs", b = "stalactite", name = "Quarry Jaw", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Fangs snap up round each target: 4 magic damage, and it is lifted into the air. 0.6 seconds later a stalactite "
			+ "drops on the spot it rose from: 5 damage, or 6.5 if its head is bare, if it is still within 2.5 blocks of that spot across the ground.")
	public static void fangsStalactite(PairCast c) {
		List<LivingEntity> jaws = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Map<LivingEntity, Vec3> rose = new HashMap<>();
		for (LivingEntity t : jaws) {
			Vec3 feet = t.position();
			rose.put(t, feet);
			c.hurt(t, 4 * c.power);
			c.lift(t, 0.9);
			c.ring(PairCast.dust(0xB89A6A, 1.0F), feet.add(0, 0.1, 0), 1.6, 8, 0);
			c.particles(ParticleTypes.CLOUD, feet, 8, 0.5, 0.05);
		}
		c.sound(SoundEvents.EVOKER_PREPARE_ATTACK, c.point(), 1.0F, 1.2F);
		// Five beats, 3 ticks apart: a shadow on the ground, the stone falling, then the drop.
		c.every(3, 5, frame -> {
			for (LivingEntity t : c.still(jaws)) {
				Vec3 spot = rose.get(t);
				if (frame == 0) {
					c.disc(PairCast.dust(0x2E2A26, 0.8F), spot.add(0, 0.1, 0), 1.1, 14);
				} else if (frame < 4) {
					double top = 6 - frame * 1.5;
					c.line(PairCast.dust(0x3B3530, 1.0F), spot.add(0, top, 0), spot.add(0, top - 1.5, 0), 3);
				} else {
					if (across(t.position(), spot) <= 2.5) {
						boolean bare = t.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
						c.strike(t, (bare ? 6.5 : 5) * c.power);
					}
					c.particles(ParticleTypes.CLOUD, spot.add(0, 0.5, 0), 12, 0.6, 0.05);
					c.shake(spot, 0.25F, 6);
					c.sound(SoundEvents.POINTED_DRIPSTONE_LAND, spot, 1.0F, 0.7F);
				}
			}
		});
	}

	/**
	 * Mirror Stride: you blink to where the spell landed, and the first creature it hit trades places with you. A
	 * violet helix, a reverse-portal beam, then the two arrivals.
	 */
	@Pair(a = "blink", b = "swap", name = "Mirror Stride", element = "void", kind = EffectKind.MOVEMENT,
		traits = {},
		text = "You blink to where the spell landed (at most 40 blocks away, if there is room). The first enemy it hit, or else "
			+ "the first ally, trades places with you: it lands where you stood, if there is room there.")
	public static void blinkSwap(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 from = me.position();
		Vec3 aim = c.point();
		Vec3 off = aim.subtract(from);
		double dist = off.length();
		Vec3 to = dist > 40 ? from.add(off.scale(40 / dist)) : aim;
		final LivingEntity partner = partnerOf(c);
		if (!c.blink(me, to)) {
			c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.5F, 1.8F);
			return;
		}
		final Vec3 partnerFrom = partner == null ? from : partner.position();
		final boolean swapped = partner != null && c.here(partner) && c.blink(partner, from);
		c.helix(PairCast.dust(0x7B3FE4, 1.0F), PairCast.dust(0xE0D0FF, 0.8F), from.add(0, 1, 0), 0.6, 2.0, 2, 36);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 1.0F, 1.4F);
		c.every(3, 4, frame -> {
			if (frame == 0) {
				c.line(ParticleTypes.REVERSE_PORTAL, from.add(0, 1, 0), to.add(0, 1, 0), 0.5);
			} else if (frame == 1) {
				c.sphere(PairCast.shift(0x2E1A5C, 0xE0D0FF, 0.9F), PairCast.mid(me), 0.9, 26);
				c.sound(SoundEvents.ENDERMAN_TELEPORT, to, 1.0F, 1.0F);
			} else if (frame == 2 && swapped) {
				c.line(ParticleTypes.PORTAL, partnerFrom.add(0, 1, 0), from.add(0, 1, 0), 0.5);
				c.sphere(PairCast.shift(0xE0D0FF, 0x2E1A5C, 0.8F), partnerFrom.add(0, 1, 0), 0.7, 18);
				c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 1.0F, 0.8F);
			} else if (frame == 3) {
				c.tint(to, 6, 0x6A3FD0, 8);
				c.punch(0.2F);
			}
		});
	}

	/**
	 * Sentinel Sigil: every enemy near the target glows, and each enemy hit carries a sigil that bursts the next time
	 * anything hurts it. The burst jolts the glowing enemies round it, so the glow decides who is caught.
	 */
	@Pair(a = "sentry", b = "spellbrand", name = "Sentinel Sigil", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Up to 16 enemies within 16 blocks of the target glow for 10 seconds. Each enemy hit bears a sigil for 8 seconds: the next "
			+ "time anything hurts it, the sigil bursts for 7 magic damage, and 2 magic to each glowing enemy within 3 blocks.")
	public static void sentrySpellbrand(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> branded = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : c.enemiesNear(at, 16 * c.radius)) {
			c.effect(t, MobEffects.GLOWING, 10, 0);
		}
		c.ring(PairCast.dust(0xFFF1B0, 0.7F), at.add(0, 0.2, 0), 16 * c.radius, 48, 0);
		c.sound(SoundEvents.GLOW_INK_SAC_USE, at, 1.0F, 1.2F);
		Map<LivingEntity, Float> health = new HashMap<>();
		Set<LivingEntity> sealed = new HashSet<>();
		for (LivingEntity t : branded) {
			health.put(t, t.getHealth());
			sealed.add(t);
			c.star(PairCast.dust(0xFFE27A, 1.0F), PairCast.mid(t), 5, 0.9, 0);
		}
		// Forty-one beats, 4 ticks apart: eight seconds of sigil.
		c.every(4, 41, frame -> {
			for (LivingEntity t : c.still(branded)) {
				if (!sealed.contains(t)) {
					continue;
				}
				if (t.getHealth() < health.get(t) - 0.01F) {
					sealed.remove(t);
					burst(c, t);
				} else if (frame % 10 == 0) {
					c.star(PairCast.dust(0xFFE27A, 0.7F), PairCast.mid(t), 5, 0.6, frame * 0.1);
				}
				health.put(t, t.getHealth());
			}
		});
	}

	private static void burst(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.hurt(t, 7 * c.power);
		c.sphere(PairCast.shift(0xFFE27A, 0xA46BFF, 1.2F), at, 1.3, 30);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 1.0F, 1.5F);
		for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
			if (near != t && near.hasEffect(MobEffects.GLOWING)) {
				c.line(PairCast.dust(0xC9B6FF, 0.8F), at, PairCast.mid(near), 3);
				c.hurt(near, 2 * c.power);
			}
		}
	}

	/**
	 * Sundering Spire: a stalactite drops on each target's spot and stays as a spire for half a second; then the
	 * ground under it quakes and everything still near the spot feels it.
	 */
	@Pair(a = "aftershock", b = "stalactite", name = "Sundering Spire", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A stalactite drops on each target's spot: 5 damage, or 6.5 if its head is bare, unless it has stepped off that spot. "
			+ "Half a second after it lands, the spot quakes: 4 more to every enemy within 2 blocks of it.")
	public static void aftershockStalactite(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Map<LivingEntity, Vec3> spots = new HashMap<>();
		List<Vec3> landed = new ArrayList<>();
		for (LivingEntity t : marked) {
			spots.put(t, t.position());
		}
		c.sound(SoundEvents.RAVAGER_ROAR, c.point(), 0.5F, 0.6F);
		c.every(2, 5, frame -> {
			for (LivingEntity t : c.still(marked)) {
				Vec3 spot = spots.get(t);
				if (frame == 0) {
					c.disc(PairCast.dust(0x4A3B2A, 0.9F), spot.add(0, 0.1, 0), 1.0, 14);
				} else if (frame < 4) {
					double top = 6 - frame * 1.5;
					c.line(PairCast.dust(0x6B5B4B, 1.0F), spot.add(0, top, 0), spot.add(0, top - 1.5, 0), 3);
				} else {
					if (across(t.position(), spot) <= 1.0) {
						boolean bare = t.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
						c.strike(t, (bare ? 6.5 : 5) * c.power);
					}
					c.column(PairCast.dust(0x7A6A55, 1.0F), spot, 0.5, 1.6, 20);
					c.sound(SoundEvents.POINTED_DRIPSTONE_LAND, spot, 1.0F, 0.9F);
					if (!landed.contains(spot)) {
						landed.add(spot);
					}
				}
			}
		});
		c.later(18, () -> {
			for (Vec3 spot : landed) {
				c.shake(spot, 0.35F, 10);
				c.wave(PairCast.shift(0xB9A37A, 0x4A3B2A, 0.9F), spot.add(0, 0.2, 0), 24, 0.3);
				c.particles(ParticleTypes.DUST_PLUME, spot.add(0, 0.5, 0), 14, 0.5, 0.05);
				c.sound(SoundEvents.MACE_SMASH_GROUND, spot, 1.0F, 0.8F);
				for (LivingEntity near : c.enemiesNear(spot, 2 * c.radius)) {
					c.strike(near, 4 * c.power);
				}
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The ones of {@code ts} whose body centre is within {@code r} of {@code at}, as they stand now. */
	private static List<LivingEntity> within(List<LivingEntity> ts, Vec3 at, double r) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity t : ts) {
			if (PairCast.mid(t).distanceTo(at) <= r) {
				out.add(t);
			}
		}
		return out;
	}

	/** Distance across the ground between two points, ignoring height. */
	private static double across(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** The creature Mirror Stride trades with: the first enemy it hit, else the first ally that is not the caster. */
	private static LivingEntity partnerOf(PairCast c) {
		LivingEntity enemy = c.firstEnemy();
		if (enemy != null) {
			return enemy;
		}
		for (LivingEntity a : c.allies()) {
			if (a != c.caster) {
				return a;
			}
		}
		return null;
	}
}
