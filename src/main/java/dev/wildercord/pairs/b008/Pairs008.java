package dev.wildercord.pairs.b008;

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

import java.util.List;

/** Hand-made pair fusions for group 08: each its own mechanic, choreography and sound. */
public final class Pairs008 {
	private Pairs008() {}

	/**
	 * Night Stampede: blind and spook. The look: a violet shroud on each target, then a stampede of shoves with
	 * dark streaks, and a sharp line wherever two of them bump.
	 */
	@Pair(a = "blind", b = "spook", name = "Night Stampede", element = "void", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Blindness for 4 seconds and Darkness for 3 on up to 8 enemies near the spot. Then 2 seconds of stampede: "
			+ "every half second each is knocked away from the spot, and each one that bumps another takes 2 damage.")
	public static void blindSpook(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> herd = PairCast.first(c.enemiesNear(spot, 2.5 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity t : herd) {
			c.effect(t, MobEffects.BLINDNESS, 4, 0);
			c.effect(t, MobEffects.DARKNESS, 3, 0);
		}
		c.tint(spot, 4 * c.radius, 0x2B0F3D, 16);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, spot, 0.8F, 0.5F);
		c.every(10, 5, frame -> {
			List<LivingEntity> running = c.still(herd);
			for (LivingEntity t : running) {
				Vec3 at = PairCast.mid(t);
				c.sphere(PairCast.dust(0x2B0F3D, 1.1F), at, 1.0 - frame * 0.1, 16);
				c.mote(PairCast.shift(0x7A4FB0, 0xE8D8FF, 0.8F), at, outward(spot, at, 0.25));
				c.knockFrom(t, spot, 0.5, 0.1);
			}
			for (LivingEntity t : c.still(running)) {
				Vec3 at = PairCast.mid(t);
				for (LivingEntity other : running) {
					if (other != t && PairCast.mid(other).distanceTo(at) < 1.2) {
						c.line(PairCast.shift(0xE8D8FF, 0x7A4FB0, 0.6F), at, PairCast.mid(other), 4);
						c.hurt(t, 2 * c.power);
						c.shake(at, 0.15F, 6);
					}
				}
			}
		});
	}

	/**
	 * Witchbrand: hex and manaburn. A violet sigil spirals over each target while its brand burns, then flares:
	 * the arcane burn that bites hardest on players, and a ring of spill into the enemies beside it.
	 */
	@Pair(a = "hex", b = "manaburn", name = "Witchbrand", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Each target is branded for 6 seconds with Weakness II. Then the brand flares: 5 damage to each target still "
			+ "there, 4 more to a player among them, and 2 damage to every other enemy within 3 blocks of a flaring brand.")
	public static void hexManaburn(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : marked) {
			c.effect(t, MobEffects.WEAKNESS, 6, 1);
		}
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 0.8F, 0.7F);
		c.every(8, 4, frame -> {
			for (LivingEntity t : c.still(marked)) {
				Vec3 at = PairCast.mid(t);
				Vec3 feet = new Vec3(at.x, t.getY() + 0.1, at.z);
				c.ring(PairCast.dust(0xB04CFF, 0.9F), feet, 0.9, 14, frame * 0.4);
				c.spiral(PairCast.shift(0xB04CFF, 0xFFE3F5, 0.7F), feet, 0.7, 1.6, 2, 14);
			}
			c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 0.5F, 0.9F + frame * 0.2F);
		});
		c.later(c.ticks(6), () -> flare(c, marked));
	}

	private static void flare(PairCast c, List<LivingEntity> marked) {
		for (LivingEntity t : c.still(marked)) {
			Vec3 at = PairCast.mid(t);
			c.hurt(t, 5 * c.power);
			if (t instanceof Player) {
				c.hurt(t, 4 * c.power);
			}
			c.sphere(PairCast.shift(0xB04CFF, 0xFFE3F5, 1.3F), at, 1.4, 40);
			c.ring(PairCast.dust(0xFFE3F5, 0.8F), new Vec3(at.x, t.getY() + 0.1, at.z), 3 * c.radius, 30, 0);
			for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
				if (near != t) {
					c.line(PairCast.dust(0xB04CFF, 0.7F), at, PairCast.mid(near), 3);
					c.hurt(near, 2 * c.power);
				}
			}
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 1.0F, 0.6F);
		c.shake(c.point(), 0.3F, 8);
	}

	/**
	 * Bogsink: hobble and mire. A brown disc of mud opens on the ground, bubbles rise, and the bog tugs each caught
	 * enemy back towards the middle once a second.
	 */
	@Pair(a = "hobble", b = "mire", name = "Bogsink", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Each enemy within 2.5 blocks of the spot is hobbled (Slowness III) and soaked for 5 seconds. The bog tugs "
			+ "each one back towards the spot once a second for 4 seconds.")
	public static void hobbleMire(PairCast c) {
		Vec3 spot = c.point();
		Vec3 ground = c.ground(spot);
		List<LivingEntity> stuck = PairCast.first(c.enemiesNear(spot, 2.5 * c.radius), PairCast.MAX_TARGETS);
		c.disc(PairCast.dust(0x4B3A22, 1.4F), ground, 2.5 * c.radius, 60);
		c.sound(SoundEvents.MUD_PLACE, ground, 1.0F, 0.7F);
		for (LivingEntity t : stuck) {
			c.effect(t, MobEffects.SLOWNESS, 5, 2);
			c.mark(t, Reactions.Mark.SOAKED);
		}
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(stuck)) {
				Vec3 at = PairCast.mid(t);
				c.column(PairCast.dust(0x6E8B3D, 0.8F), c.ground(at), 0.5, 1.0, 8);
				if (frame > 0) {
					c.pullTo(t, spot, 0.3);
					c.line(PairCast.dust(0x5C4A2A, 0.7F), at, spot, 2);
					c.sound(SoundEvents.SLIME_SQUISH, at, 0.6F, 0.5F);
				}
			}
		});
	}

	/**
	 * Skyhook: grapple and leap. A pale wind line snaps out to the landing spot, then the caster is hurled there in a
	 * high arc, trailing gusts, with no fall damage while in the air.
	 */
	@Pair(a = "grapple", b = "leap", name = "Skyhook", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "Hooks you towards where the spell landed and hurls you in a high arc: a push of 0.9 upward and up to 1.2 "
			+ "towards it, with Jump Boost III for 3 seconds and no fall damage for 1.5 seconds.")
	public static void grappleLeap(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 to = c.point();
		Vec3 from = PairCast.mid(me);
		Vec3 flat = new Vec3(to.x - me.getX(), 0, to.z - me.getZ());
		double d = flat.length();
		Vec3 sideways = d < 0.5 ? Vec3.ZERO : flat.normalize().scale(Math.min(1.2, 0.2 + d * 0.1));
		c.line(PairCast.shift(0xDDF6FF, 0x7FD4FF, 0.7F), from, to, 3);
		c.sound(SoundEvents.CHAIN_PLACE, to, 1.0F, 1.3F);
		c.later(5, () -> {
			c.push(me, sideways.add(0, 0.9, 0));
			c.effect(me, MobEffects.JUMP_BOOST, 3, 2);
			c.punch(0.3F);
			c.sound(SoundEvents.BREEZE_JUMP, from, 0.9F, 1.2F);
			c.sound(SoundEvents.WIND_CHARGE_BURST, from, 0.6F, 0.9F);
			c.every(3, 11, frame -> {
				me.resetFallDistance();
				c.particles(ParticleTypes.CLOUD, PairCast.mid(me), 2, 0.2, 0.01);
				if (frame % 4 == 0) {
					c.particles(ParticleTypes.GUST, PairCast.mid(me), 1, 0.1, 0.0);
				}
			});
		});
	}

	/**
	 * Eclipse Shroud: umbra and veil. Allies fade from sight, and a shell of darkness contracts over the spot, biting
	 * each enemy in it four times while squid ink hangs in the air.
	 */
	@Pair(a = "umbra", b = "veil", name = "Eclipse Shroud", element = "void", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Allies within 3 blocks turn invisible for 6 seconds. For 3 seconds an eclipse holds over the spot: four "
			+ "bites a second apart: each enemy in it takes 2 damage and is marked Shadowed at every bite.")
	public static void umbraVeil(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		for (LivingEntity ally : c.alliesNear(spot, r)) {
			c.effect(ally, MobEffects.INVISIBILITY, 6, 0);
			c.particles(PairCast.dust(0xC9B8FF, 0.6F), PairCast.mid(ally), 6, 0.4, 0.02);
		}
		c.sound(SoundEvents.GLOW_INK_SAC_USE, spot, 1.0F, 0.6F);
		c.column(PairCast.dust(0x1B0F2E, 1.2F), c.ground(spot), r, 2.5, 40);
		c.every(20, 4, frame -> {
			c.sphere(PairCast.shift(0x6B3FA0, 0x0B0614, 1.0F), spot, r * (1.0 - frame * 0.2), 30);
			c.particles(ParticleTypes.SQUID_INK, spot, 12, r * 0.5, 0.02);
			for (LivingEntity t : c.enemiesNear(spot, r)) {
				c.hurt(t, 2 * c.power);
				c.mark(t, Reactions.Mark.SHADOWED);
				c.line(PairCast.dust(0x2A1748, 0.8F), spot, PairCast.mid(t), 2);
			}
			c.sound(SoundEvents.SCULK_CLICKING, spot, 1.0F, 0.5F);
		});
	}

	/**
	 * Stillwater Hour: stillbind and tarry. A pale sphere of stopped time with two clock hands turning inside. Enemies
	 * are drawn back to its centre each second, then the hour breaks and strikes them for what they lost.
	 */
	@Pair(a = "stillbind", b = "tarry", name = "Stillwater Hour", element = "time", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "A bubble of stopped time reaching 3 blocks from its centre for 3 seconds. Enemies inside are tugged back towards it once "
			+ "a second, and when it breaks each one takes 6 damage. Allies inside gain Regeneration I for 5 seconds.")
	public static void stillwaterHour(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		List<LivingEntity> held = PairCast.first(c.enemiesNear(spot, r), PairCast.MAX_TARGETS);
		for (LivingEntity ally : c.alliesNear(spot, r)) {
			c.effect(ally, MobEffects.REGENERATION, 5, 0);
		}
		c.sound(SoundEvents.BELL_RESONATE, spot, 1.0F, 0.5F);
		c.every(20, 3, frame -> {
			c.sphere(PairCast.shift(0xFFE7A0, 0x6F8FFF, 1.0F), spot, r, 48);
			c.star(PairCast.dust(0xFFE7A0, 0.6F), spot, 4, 1.6 * r, frame * 0.7);
			if (frame > 0) {
				for (LivingEntity t : c.still(held)) {
					c.pullTo(t, spot, 0.3);
					c.line(PairCast.dust(0x6F8FFF, 0.7F), PairCast.mid(t), spot, 2);
				}
			}
			c.sound(SoundEvents.BELL_RESONATE, spot, 0.4F, 0.6F + frame * 0.3F);
		});
		c.later(c.ticks(3), () -> {
			for (LivingEntity t : c.still(held)) {
				c.hurt(t, 6 * c.power);
				c.particles(PairCast.dust(0xFFE7A0, 1.0F), PairCast.mid(t), 10, 0.4, 0.05);
			}
			c.tint(spot, r + 2, 0xFFE7A0, 10);
			c.sound(SoundEvents.AMETHYST_BLOCK_BREAK, spot, 1.0F, 0.6F);
			c.shake(spot, 0.2F, 6);
		});
	}

	/**
	 * Bloodbait: lure and taunt. A red ring marks the bait, enemies are dragged in over two seconds, and those that
	 * reach it are struck and turn on the caster, who is steeled by Resistance for as long.
	 */
	@Pair(a = "lure", b = "taunt", name = "Bloodbait", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Enemies within 3 blocks are dragged towards the spot for 2 seconds. Each one that reaches within 1.5 blocks of "
			+ "it takes 6 damage (3 hearts) and turns on you for 6 seconds, and you get Resistance I for those 6 seconds.")
	public static void lureTaunt(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> bait = PairCast.first(c.enemiesNear(spot, 3 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.NOTE_BLOCK_BASS, spot, 1.0F, 0.5F);
		c.ring(PairCast.dust(0x9B0F2E, 1.0F), c.ground(spot), 3 * c.radius, 24, 0);
		c.every(10, 5, frame -> {
			for (LivingEntity t : c.still(bait)) {
				Vec3 at = PairCast.mid(t);
				if (frame < 4) {
					c.pullTo(t, spot, 0.5);
					c.line(PairCast.dust(0xB0102A, 0.7F), at, spot, 2);
				} else if (at.distanceTo(spot) < 1.5) {
					arrive(c, t);
				}
			}
		});
	}

	private static void arrive(PairCast c, LivingEntity t) {
		c.hurt(t, 6 * c.power);
		c.particles(ParticleTypes.DAMAGE_INDICATOR, PairCast.mid(t), 6, 0.3, 0.1);
		c.sound(SoundEvents.RAVAGER_ROAR, PairCast.mid(t), 0.5F, 1.6F);
		c.effect(c.caster, MobEffects.RESISTANCE, 6, 0);
		if (t instanceof Mob mob && c.movable(mob)) {
			mob.setTarget(c.caster);
			c.later(c.ticks(6), () -> {
				if (mob.getTarget() == c.caster) {
					mob.setTarget(null);
				}
			});
		}
	}

	/**
	 * Gangrene: bleed and wither. Each target weeps dark blood and withers, and every second the rot creeps to the
	 * enemies standing within two blocks of it: a green-black spore spreading through a crowd.
	 */
	@Pair(a = "bleed", b = "wither", name = "Gangrene", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Each target bleeds: 2 damage now, then 1 more every half second for 4 seconds, and Wither I for 4 seconds. "
			+ "Every second the rot spreads: each enemy within 2 blocks of a bleeding target takes 1 damage and Wither I for 2 seconds.")
	public static void gangrene(PairCast c) {
		List<LivingEntity> sick = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.SLIME_SQUISH, c.point(), 1.0F, 0.6F);
		c.every(10, 9, frame -> {
			for (LivingEntity t : c.still(sick)) {
				Vec3 at = PairCast.mid(t);
				if (frame == 0) {
					c.hurt(t, 2 * c.power);
					c.effect(t, MobEffects.WITHER, 4, 0);
				} else {
					c.hurt(t, c.power);
				}
				c.particles(PairCast.dust(0x7A0000, 0.9F), at, 4, 0.3, 0.02);
				if (frame % 2 == 0) {
					for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
						if (near != t && !sick.contains(near)) {
							c.line(PairCast.shift(0x6B1A1A, 0x3E5A1E, 0.7F), at, PairCast.mid(near), 3);
							c.hurt(near, c.power);
							c.effect(near, MobEffects.WITHER, 2, 0);
							c.particles(ParticleTypes.CRIMSON_SPORE, PairCast.mid(near), 4, 0.3, 0.02);
						}
					}
					c.sound(SoundEvents.SCULK_CLICKING, at, 0.6F, 1.6F);
				}
			}
		});
	}

	/**
	 * Sanguine Tithe: heal and leech. Threads of red draw the blood out of two enemies, arcs carry it to the allies
	 * near the spot, and each ally is healed with hearts rising over it.
	 */
	@Pair(a = "heal", b = "leech", name = "Sanguine Tithe", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Up to two enemies within 3 blocks take 6 damage each (3 hearts). Each ally within 3 blocks heals 4 health for "
			+ "every enemy drained, 8 at most. Healing an ally can't use becomes absorption, up to 4 health for 10 seconds.")
	public static void sanguineTithe(PairCast c) {
		Vec3 spot = c.point();
		double r = 3 * c.radius;
		List<LivingEntity> drained = PairCast.first(c.enemiesNear(spot, r), 2);
		List<LivingEntity> fed = c.alliesNear(spot, r);
		double heal = Math.min(8.0, 4.0 * drained.size()) * c.power;
		c.sound(SoundEvents.WARDEN_HEARTBEAT, spot, 1.0F, 0.8F);
		c.every(10, 3, frame -> {
			if (frame == 0) {
				for (LivingEntity t : c.still(drained)) {
					c.line(PairCast.shift(0xB0102A, 0xFF6B8A, 0.8F), PairCast.mid(t), spot, 3);
					c.hurt(t, 6 * c.power);
				}
			} else if (frame == 1) {
				for (LivingEntity ally : c.still(fed)) {
					c.arc(PairCast.shift(0xB0102A, 0xFF6B8A, 0.7F), spot, PairCast.mid(ally), 1.2, 10);
				}
			} else {
				for (LivingEntity ally : c.still(fed)) {
					double missing = ally.getMaxHealth() - ally.getHealth();
					c.heal(ally, heal);
					double overflow = heal - missing;
					if (overflow > 0) {
						c.absorb(ally, Math.min(4 * c.power, overflow), 10);
					}
					c.particles(ParticleTypes.HEART, PairCast.mid(ally), 3, 0.3, 0.02);
					c.sound(SoundEvents.TOTEM_USE, PairCast.mid(ally), 0.4F, 1.4F);
				}
			}
		});
	}

	/**
	 * Mended Stone: heal and stoneskin. Allies are healed, and the overflow hardens into stone plates that crack off
	 * one at a time, with pale dust and a sharp crack each time.
	 */
	@Pair(a = "heal", b = "stoneskin", name = "Mended Stone", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Each ally within 3 blocks is healed 6 health and gets Resistance II and Slowness I for 10 seconds. Healing it "
			+ "can't use hardens into up to two stone plates of 2 health each (absorption), and a plate cracks off every 2 seconds.")
	public static void mendedStone(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> held = c.alliesNear(spot, 3 * c.radius);
		c.sound(SoundEvents.STONE_HIT, spot, 0.9F, 0.7F);
		for (LivingEntity ally : held) {
			c.effect(ally, MobEffects.RESISTANCE, 10, 1);
			c.effect(ally, MobEffects.SLOWNESS, 10, 0);
			double missing = ally.getMaxHealth() - ally.getHealth();
			double overflow = 6 * c.power - missing;
			c.heal(ally, 6 * c.power);
			int plates = (int) Math.min(2, Math.floor(overflow / 2));
			if (plates > 0) {
				c.absorb(ally, plates * 2, 10);
			}
		}
		c.every(40, 3, frame -> {
			for (LivingEntity ally : c.still(held)) {
				Vec3 at = PairCast.mid(ally);
				if (frame == 0) {
					c.column(PairCast.shift(0xB7C2B0, 0x6B7568, 0.9F), c.ground(at), 0.9, 1.6, 16);
				} else if (ally.getAbsorptionAmount() >= 2) {
					ally.setAbsorptionAmount(ally.getAbsorptionAmount() - 2);
					c.particles(PairCast.dust(0xB7C2B0, 0.7F), at, 10, 0.4, 0.05);
					c.sound(SoundEvents.STONE_BREAK, at, 0.9F, 1.2F);
				}
			}
		});
	}

	/** The horizontal step away from {@code from}, {@code speed} long, or zero if {@code at} is directly over it. */
	private static Vec3 outward(Vec3 from, Vec3 at, double speed) {
		Vec3 d = new Vec3(at.x - from.x, 0, at.z - from.z);
		return d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize().scale(speed);
	}
}
