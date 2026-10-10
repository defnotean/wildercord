package dev.wildercord.pairs.b021;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs021 {
	private Pairs021() {}

	/**
	 * Wildfire Gale: a gust picks up each target, sets it alight and hurls it away. Where it comes down, lightning
	 * strikes and the fire leaps on to the enemies beside it. The look: embers spiral round each flung target, then a
	 * bolt and a splash of orange sparks where it lands.
	 */
	@Pair(a = "firestorm", b = "tempest", name = "Wildfire Gale", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Hurls up to 3 enemies away from you and sets them alight for 5 seconds. 0.7 seconds later, where each comes down, "
			+ "lightning strikes it for 6 damage, and the fire leaps to every other enemy within 2 blocks for 2 each, once per cast.")
	public static void wildfireGale(PairCast c) {
		List<LivingEntity> flung = PairCast.first(c.enemies(), 3);
		Vec3 from = c.origin();
		for (LivingEntity t : flung) {
			c.knockFrom(t, from, 1.2, 0.6);
			c.ignite(t, 5);
		}
		c.sound(SoundEvents.WIND_CHARGE_BURST, c.point(), 0.7F, 1.1F);
		// Wind-up: embers spiral round each flung target while it is in the air.
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(flung)) {
				c.spiral(PairCast.shift(0xFFB347, 0xFF4D1A, 0.8F), PairCast.mid(t), 0.7, 1.4, 1.5, 12);
			}
		});
		// Payoff: where each lands, lightning, and the fire leaps once to each enemy beside it.
		c.later(14, () -> {
			Set<LivingEntity> leapt = new HashSet<>();
			for (LivingEntity t : c.still(flung)) {
				Vec3 at = PairCast.mid(t);
				c.shock(t, 6 * c.power);
				c.bolt(at);
				c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.6F, 1.2F);
				c.particles(ParticleTypes.FLAME, at, 14, 0.4, 0.1);
				for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
					if (near != t && leapt.add(near)) {
						c.line(PairCast.dust(0xFFB347, 0.8F), at, PairCast.mid(near), 4);
						c.burn(near, 2 * c.power);
					}
				}
			}
		});
	}

	/**
	 * Ember Ward: a ward of warmth and cold on the allies. Their skin takes the heat without the frost, and every
	 * couple of seconds the warmth rolls out and scorches the enemies round them. The look: gold rising off each ally
	 * as blue sinks in, then rings of ember.
	 */
	@Pair(a = "fireward", b = "frostward", name = "Ember Ward", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Allies get Fire Resistance for 30 seconds, 4 absorption (2 hearts) for 10 seconds, and can't freeze for 30 seconds. "
			+ "Every 2 seconds for 8 seconds, heat rolls out: each enemy within 2 blocks of an ally takes 1 fire damage.")
	public static void hearthward(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : allies) {
			c.effect(t, MobEffects.FIRE_RESISTANCE, 30, 0);
			c.absorb(t, 4, 10);
			c.sound(SoundEvents.BEACON_ACTIVATE, PairCast.mid(t), 0.6F, 1.5F);
		}
		// Wind-up: gold rises off each ally while blue frost sinks into it.
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(allies)) {
				Vec3 at = PairCast.mid(t);
				c.spiral(PairCast.shift(0xFFD27A, 0xFF7A1A, 0.9F), at, 0.6, 1.6, 1, 10);
				c.spiral(PairCast.dust(0xBFE9FF, 0.7F), at.add(0, 1.6, 0), 0.6, -1.6, 1, 10);
			}
		});
		// Frostward holds for the whole 30 seconds: every freeze is cleared as it starts.
		c.every(10, 60, frame -> {
			for (LivingEntity t : c.still(allies)) {
				t.setTicksFrozen(0);
				Reactions.clear(t, Reactions.Mark.FROZEN);
			}
		});
		// The heat rolls out every 2 seconds, eight seconds in all.
		c.every(40, 5, frame -> {
			for (LivingEntity t : c.still(allies)) {
				c.ring(PairCast.dust(0xFF9A3C, 0.9F), t.position().add(0, 0.1, 0), 2.0, 16, frame * 0.4);
				for (LivingEntity near : c.enemiesNear(PairCast.mid(t), 2 * c.radius)) {
					c.burn(near, 1 * c.power);
				}
			}
		});
	}

	/**
	 * Dawnmend: a heal that the sun keeps going. The allies are healed at once and given Regeneration; then, while
	 * they stand in open daylight, the light tops them up three times. The look: columns of gold climbing from the
	 * allies, a bright shell on the heal, and rings of pale light in the sun.
	 */
	@Pair(a = "regrowth", b = "sunbask", name = "Dawnmend", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius", "duration"},
		text = "Heals each ally within 4 blocks 6 health and gives Regeneration I for 6 seconds. Then three times, 2 seconds apart, "
			+ "each ally in open daylight heals 1 more.")
	public static void dawnmend(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> warmed = PairCast.first(c.alliesNear(at, 4 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.8F, 1.4F);
		// Wind-up: gold light climbs in columns over the allies.
		c.every(5, 2, frame -> {
			for (LivingEntity t : c.still(warmed)) {
				c.column(PairCast.dust(0xFFE27A, 1.0F), t.position(), 0.6, 2.0, 10);
			}
		});
		c.later(10, () -> {
			for (LivingEntity t : c.still(warmed)) {
				c.heal(t, 6 * c.power);
				c.effect(t, MobEffects.REGENERATION, 6, 0);
				c.sphere(PairCast.shift(0xFFE58A, 0xD8FFB0, 1.0F), PairCast.mid(t), 1.0, 22);
			}
			c.sound(SoundEvents.TOTEM_USE, at, 0.4F, 1.6F);
			// The sun tops them up: three beats, two seconds apart, only for those under open sky by day.
			c.every(40, 3, frame -> {
				for (LivingEntity t : c.still(warmed)) {
					Vec3 mid = PairCast.mid(t);
					if (c.level.isBrightOutside() && c.level.canSeeSky(BlockPos.containing(mid))) {
						c.heal(t, 1 * c.power);
						c.ring(PairCast.dust(0xFFF0A0, 0.9F), t.position().add(0, 0.1, 0), 1.0, 14, frame * 0.4);
					}
				}
			});
		});
	}

	/**
	 * Gathering Fall: a well of gravity drags the enemies in, then a meteor drops on the spot they were dragged to. The
	 * look: a dark sphere closing in over a second, then a line of flame falling from the sky and a burst of lava.
	 */
	@Pair(a = "gravity_well", b = "meteor", name = "Gathering Fall", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Pulls each enemy within 6 blocks toward the point for 1 second, marked pulled. Then a meteor falls there: "
			+ "5 fire damage and alight for 4 seconds to each enemy within 2.5 blocks, and 7 more to the first one struck.")
	public static void gatheringFall(PairCast c) {
		Vec3 at = c.point();
		LivingEntity first = c.firstEnemy();
		List<LivingEntity> gathered = new ArrayList<>(c.enemiesNear(at, 6 * c.radius));
		c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.8F, 0.6F);
		// The well: a sphere closing in over the gathered enemies, pulling each toward the point.
		c.every(4, 5, frame -> {
			c.sphere(PairCast.dust(0x3A2A4A, 0.9F), at, 6.0 * c.radius * (1 - frame * 0.2), 26);
			for (LivingEntity t : c.still(gathered)) {
				c.pullTo(t, at, 0.9);
				c.mark(t, Reactions.Mark.PULLED);
			}
		});
		// The meteor: a line of flame from the sky, then the blast.
		c.later(20, () -> {
			c.line(ParticleTypes.FLAME, at.add(0, 9, 0), at, 3);
			c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.6F, 1.4F);
			c.shake(at, 0.3F, 8);
			for (LivingEntity t : c.enemiesNear(at, 2.5 * c.radius)) {
				c.burn(t, 5 * c.power);
				c.ignite(t, 4);
				if (t == first) {
					c.burn(t, 7 * c.power);
				}
				c.particles(ParticleTypes.LAVA, PairCast.mid(t), 6, 0.3, 0.1);
			}
			c.sphere(PairCast.shift(0xFFE08A, 0xFF4D1A, 1.2F), at, 1.6, 30);
		});
	}

	/**
	 * Draught Pyre: a flue of fire that holds each enemy up in its draught. Every second they're lifted again and
	 * burn; when the column breaks they drop. The look: a flame column round each target, and a smoke ring on the drop.
	 */
	@Pair(a = "inferno", b = "updraft", name = "Draught Pyre", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Hurls each enemy it hits (up to 8) into a column of fire that holds them up. For 3 seconds they're set alight and "
			+ "burn for 3 a second, lifted again each second. Then the column breaks and they drop, taking 4 fire damage more.")
	public static void draughtPyre(PairCast c) {
		List<LivingEntity> caught = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.FIRECHARGE_USE, c.point(), 0.8F, 0.7F);
		// Four beats, a second apart: three lift and burn, the fourth breaks the column and drops them.
		c.every(20, 4, frame -> {
			for (LivingEntity t : c.still(caught)) {
				Vec3 feet = t.position();
				if (frame == 0) {
					c.ignite(t, 3);
				}
				if (frame < 3) {
					c.lift(t, 0.6);
					c.burn(t, 3 * c.power);
					c.column(PairCast.shift(0xFFB347, 0xFF4D1A, 0.9F), feet, 0.9, 2.6, 16);
				} else {
					c.burn(t, 4 * c.power);
					c.sphere(PairCast.dust(0x6B6B6B, 1.1F), PairCast.mid(t), 1.2, 22);
					c.wave(ParticleTypes.SMOKE, PairCast.mid(t), 12, 0.2);
					c.sound(SoundEvents.GENERIC_EXPLODE, PairCast.mid(t), 0.3F, 1.6F);
				}
			}
		});
	}

	/**
	 * Tinderrot: a poison that smoulders. Each second it adds a stack and a little fire; at the fourth stack the
	 * target bursts into toxic smoke that poisons whoever is near. The look: green wisps rising off each target,
	 * then a green-and-ember cloud.
	 */
	@Pair(a = "kindling", b = "venom", name = "Tinderrot", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Poisons each target: 2 damage now and Poison I for 4 seconds. Each second it smoulders: a stack and 1 fire damage. "
			+ "At the fourth stack it bursts into toxic smoke: 5 damage and Poison I for 3 seconds to every enemy within 2.5 blocks.")
	public static void tinderrot(PairCast c) {
		List<LivingEntity> rotted = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : rotted) {
			c.hurt(t, 2 * c.power);
			c.effect(t, MobEffects.POISON, 4, 0);
		}
		c.sound(SoundEvents.SLIME_SQUISH_SMALL, c.point(), 0.8F, 0.7F);
		// Four stacks, one a second. The fourth one bursts.
		c.every(20, 4, frame -> {
			for (LivingEntity t : c.still(rotted)) {
				Vec3 at = PairCast.mid(t);
				c.burn(t, 1 * c.power);
				c.particles(ParticleTypes.SMOKE, at, 4, 0.3, 0.02);
				c.particles(ParticleTypes.FLAME, at, 2, 0.2, 0.02);
				if (frame == 3) {
					rotBurst(c, t);
				}
			}
		});
	}

	private static void rotBurst(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.sphere(PairCast.shift(0x7FB23A, 0xFF9A3C, 1.2F), at, 2.0, 30);
		c.wave(ParticleTypes.CAMPFIRE_COSY_SMOKE, at, 14, 0.25);
		c.sound(SoundEvents.BUBBLE_POP, at, 1.0F, 0.6F);
		c.shake(at, 0.15F, 6);
		for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
			c.hurt(near, 5 * c.power);
			c.effect(near, MobEffects.POISON, 3, 0);
		}
	}

	/**
	 * Scorn: each target is held in a bubble of boiling water, then the bubble bursts into steam. Whatever monsters
	 * the steam catches turn on the caster for six seconds, and the caster is armoured for as long. The look: a
	 * shimmering bubble round each target for two seconds, then a white steam cloud and angry marks over the monsters.
	 */
	@Pair(a = "seethe", b = "taunt", name = "Scorn", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Holds each target in boiling water for 2 seconds: Slowness III and 1 fire damage every half second. Then it bursts into "
			+ "steam: 4 damage and Blindness for 2 seconds to every enemy within 2.5 blocks. Monsters there turn on you for 6 seconds, and you get Resistance I as long.")
	public static void scorn(PairCast c) {
		List<LivingEntity> boiled = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : boiled) {
			c.effect(t, MobEffects.SLOWNESS, 2, 2);
		}
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, c.point(), 0.9F, 0.8F);
		// The bubble: four frames of boiling water round each target, half a second apart, each one scalding it.
		c.every(10, 4, frame -> {
			for (LivingEntity t : c.still(boiled)) {
				c.sphere(PairCast.shift(0xBFEFFF, 0xFFF0C8, 0.9F), PairCast.mid(t), 1.2, 22);
				c.burn(t, 1 * c.power);
			}
		});
		c.later(40, () -> {
			for (LivingEntity t : c.still(boiled)) {
				scornBurst(c, t);
			}
		});
	}

	private static void scornBurst(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.sphere(PairCast.shift(0xFFFFFF, 0xFFB070, 1.4F), at, 2.0, 40);
		c.wave(ParticleTypes.CLOUD, at, 16, 0.25);
		c.sound(SoundEvents.FIRE_EXTINGUISH, at, 0.9F, 1.2F);
		List<Mob> turned = new ArrayList<>();
		for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
			c.hurt(near, 4 * c.power);
			c.effect(near, MobEffects.BLINDNESS, 2, 0);
			if (near instanceof Mob mob && c.movable(mob)) {
				turned.add(mob);
			}
		}
		c.effect(c.caster, MobEffects.RESISTANCE, 6, 0);
		// The monsters are taken back to the caster every second, for six seconds, with an angry mark over each.
		c.every(20, 6, frame -> {
			for (Mob mob : turned) {
				if (c.here(mob)) {
					mob.setTarget(c.caster);
					c.particles(ParticleTypes.ANGRY_VILLAGER, mob.getBoundingBox().getCenter().add(0, 0.8, 0), 1, 0.1, 0);
				}
			}
		});
	}

	/**
	 * Noonscorch: a dazzle, then a sunbeam through the blindness. In open daylight the beam burns half again hotter.
	 * The look: rays of pale gold round each target, then a beam falling from above and a flare of orange.
	 */
	@Pair(a = "blind", b = "sunscorch", name = "Noonscorch", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Blinds each target (Blindness and Darkness for 5 seconds, 3 on players). One second later the noon sun, focused through "
			+ "the blindness, burns it for 8 fire damage (12 in open daylight), sets it alight for 5 seconds and makes it glow for 6.")
	public static void noonscorch(PairCast c) {
		List<LivingEntity> dazzled = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : dazzled) {
			double blind = t instanceof Player ? 3 : 5;
			c.effect(t, MobEffects.BLINDNESS, blind, 0);
			c.effect(t, MobEffects.DARKNESS, blind, 0);
		}
		c.sound(SoundEvents.ILLUSIONER_CAST_SPELL, c.point(), 0.7F, 1.3F);
		// Wind-up: pale gold rays turn round each target for a second.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(dazzled)) {
				c.star(PairCast.dust(0xFFF3B0, 0.9F), PairCast.mid(t), 8, 1.5, frame * 0.3);
			}
		});
		// Payoff: the beam falls on each one, hotter in open daylight.
		c.later(20, () -> {
			for (LivingEntity t : c.still(dazzled)) {
				Vec3 at = PairCast.mid(t);
				boolean day = c.level.isBrightOutside() && c.level.canSeeSky(BlockPos.containing(at));
				c.line(PairCast.shift(0xFFF3B0, 0xFF6A1A, 1.0F), at.add(0, 8, 0), at, 3);
				c.sphere(PairCast.shift(0xFFF3B0, 0xFF6A1A, 1.2F), at, 1.0, 24);
				c.burn(t, (day ? 12 : 8) * c.power);
				c.ignite(t, 5);
				c.effect(t, MobEffects.GLOWING, 6, 0);
			}
			c.sound(SoundEvents.FIRECHARGE_USE, c.point(), 0.8F, 0.9F);
			c.tint(c.point(), 8, 0xFFF3B0, 8);
		});
	}

	/**
	 * Bog Scald: the ground under each target turns to mire, sinking and soaking it, and the mire steams. The look: a
	 * brown column of mire rising, then a pale steam cloud over the target that scalds whoever stands in it.
	 */
	@Pair(a = "mire", b = "steam", name = "Bog Scald", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Mire spreads under each target for 5 seconds: Slowness III and soaked. The mire boils into a steam cloud over it for 4 seconds: "
			+ "4 damage to each target at once, then 1 fire damage a second and Blindness for 2 seconds to enemies within 2 blocks of the cloud.")
	public static void bogScald(PairCast c) {
		List<LivingEntity> bogged = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : bogged) {
			c.effect(t, MobEffects.SLOWNESS, 5, 2);
			c.mark(t, Reactions.Mark.SOAKED);
		}
		c.sound(SoundEvents.MUD_PLACE, c.point(), 0.9F, 0.7F);
		// Five frames a second apart: the cloud rises from the mire, and only the first one scalds the target.
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(bogged)) {
				Vec3 cloud = t.position().add(0, 1, 0);
				c.column(PairCast.dust(0xC9D6A0, 1.0F), t.position(), 1.6 * c.radius, 2.5, 22);
				if (frame == 0) {
					c.hurt(t, 4 * c.power);
					c.sound(SoundEvents.CAMPFIRE_CRACKLE, cloud, 0.8F, 0.6F);
				} else {
					for (LivingEntity near : c.enemiesNear(cloud, 2 * c.radius)) {
						c.burn(near, 1 * c.power);
						c.effect(near, MobEffects.BLINDNESS, 2, 0);
					}
				}
			}
		});
	}

	/**
	 * Flashover: a crackle of lightning stuns the target, and the stunned catch fire, the flame leaping on to the
	 * nearest enemy beside them. The look: a spark crackle falling from the sky, a flash, then an arc of sparks across
	 * to the next enemy.
	 */
	@Pair(a = "ember", b = "jolt", name = "Flashover", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Stuns each target for 1 second (Slowness III, and a monster loses its target), with 4 lightning damage. Then it flashes alight: "
			+ "3 fire damage and burning for 3 seconds. The spark leaps to the nearest other enemy within 3 blocks: alight for 2 seconds and slowed for half a second.")
	public static void flashover(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, c.point(), 0.4F, 1.3F);
		// Wind-up: sparks crackle down from the sky to each target.
		c.every(2, 3, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 6, 0), at, 0.6, 2);
			}
		});
		// Payoff, after the crackle: the stun, the lightning and the fire.
		c.later(6, () -> {
			List<LivingEntity> stunned = new ArrayList<>();
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				c.shock(t, 4 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 1, 2);
				c.ignite(t, 3);
				c.burn(t, 3 * c.power);
				c.bolt(at);
				c.particles(ParticleTypes.FLAME, at, 12, 0.3, 0.1);
				stunned.add(t);
			}
			// The stun holds for a second: each frame a monster is kept from its target and its path.
			c.every(5, 5, frame -> {
				for (LivingEntity t : c.still(stunned)) {
					if (t instanceof Mob mob && c.movable(mob)) {
						mob.setTarget(null);
						mob.getNavigation().stop();
					}
				}
			});
			// The spark leaps, 0.3 seconds on, to the nearest other enemy beside each one it hit.
			c.later(6, () -> {
				for (LivingEntity t : c.still(stunned)) {
					LivingEntity next = c.nearestEnemy(PairCast.mid(t), 3 * c.radius, t);
					if (next != null) {
						c.arc(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(t), PairCast.mid(next), 1.0, 12);
						c.ignite(next, 2);
						c.effect(next, MobEffects.SLOWNESS, 0.5, 2);
						c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, PairCast.mid(next), 0.5F, 1.4F);
					}
				}
			});
		});
	}
}
