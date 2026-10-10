package dev.wildercord.pairs.b045;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs of stone, iron, roots, mire and wards: each has its own mechanic, look and sounds. */
public final class Pairs045 {
	private Pairs045() {}

	/**
	 * Ironstack: nine motes of iron lift off the ground and stack over the point, higher each beat, then the stack
	 * settles on the allies round it like a block set down. The look: a grey column growing, then a ring of iron
	 * dust and a shell that falls round each ally.
	 */
	@Pair(a = "blockpack", b = "ironhold", name = "Ironstack", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Nine iron motes stack over the point for a second, then settle on each ally within 4 blocks: 3 healing and "
			+ "Resistance II for 6 seconds. Enemies are not touched.")
	public static void ironstack(PairCast c) {
		Vec3 spot = c.point();
		double reach = 4 * c.radius;
		c.sound(SoundEvents.ARMOR_EQUIP_IRON, spot, 0.9F, 0.8F);
		c.every(7, 3, frame -> {
			double top = 0.8 + frame * 0.9;
			c.line(PairCast.dust(0xD8DDE3, 1.0F), spot.add(0, 0.1, 0), spot.add(0, top, 0), 3);
			c.ring(PairCast.dust(0xE6EBF0, 0.9F), spot.add(0, top, 0), 0.5, 9, frame * 0.7);
		});
		c.later(24, () -> {
			c.sound(SoundEvents.ANVIL_LAND, spot, 0.5F, 1.5F);
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.heal(a, 3 * c.power);
				c.effect(a, MobEffects.RESISTANCE, 6, 1);
				c.particles(ParticleTypes.CRIT, PairCast.mid(a), 8, 0.4, 0.1);
			}
			c.sphere(PairCast.shift(0xE6EBF0, 0x7A8794, 1.0F), spot.add(0, 1, 0), reach * 0.6, 30);
		});
	}

	/**
	 * Shellrush: you curl into a shell of brick dust, then ram forward along your aim until a wall stops you. Whatever
	 * stands along the path is struck and shoved aside. The look: a turning ring round you, then a dust trail
	 * streaking out and a thud where you come to rest.
	 */
	@Pair(a = "brickwork", b = "shellback", name = "Shellrush", element = "earth", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration", "radius"},
		text = "You curl into a shell for 8 seconds of Resistance II, then 0.4 seconds later ram up to 8 blocks along your aim, "
			+ "stopping short of any wall. Each enemy within 1.5 blocks of your path takes 6 damage and is shoved aside.")
	public static void shellrush(PairCast c) {
		Vec3 start = c.caster.position();
		Vec3 eye = start.add(0, 0.5, 0);
		Vec3 flat = flatDir(c.point().subtract(start), c.dir());
		double reach = 8 * c.radius;
		c.effect(c.caster, MobEffects.RESISTANCE, 8, 1);
		c.sound(SoundEvents.SHIELD_BLOCK, start, 0.8F, 0.6F);
		c.every(4, 3, frame -> c.ring(PairCast.shift(0xC9A36B, 0x6E4A2E, 1.0F), start.add(0, 0.8, 0), 0.9, 12, frame * 0.7));
		c.later(8, () -> {
			Vec3 end = eye.add(flat.scale(reach));
			BlockHitResult wall = c.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster));
			double run = wall.getType() == HitResult.Type.MISS ? reach : Math.max(0, wall.getLocation().distanceTo(eye) - 0.6);
			if (!c.blink(c.caster, eye.add(flat.scale(run)))) {
				return;
			}
			Vec3 landed = c.caster.position();
			Vec3 mid = start.lerp(landed, 0.5);
			c.sound(SoundEvents.PISTON_EXTEND, start, 1.0F, 0.5F);
			c.line(PairCast.dust(0xD9B98A, 1.0F), start.add(0, 0.2, 0), landed.add(0, 0.2, 0), 2);
			for (LivingEntity t : c.enemiesNear(mid, reach / 2 + 2)) {
				if (distToSegment(t.position(), start, landed) <= 1.5 * c.radius) {
					c.strike(t, 6 * c.power);
					c.push(t, flat.scale(0.9).add(0, 0.35, 0));
				}
			}
			c.wave(PairCast.dust(0xB08A5A, 1.0F), landed.add(0, 0.3, 0), 14, 0.3);
			c.disc(PairCast.dust(0x8C6A45, 1.0F), landed, 1.2, 16);
			c.sound(SoundEvents.DEEPSLATE_BREAK, landed, 0.8F, 1.3F);
			c.shake(landed, 0.2F, 6);
			c.punch(0.15F);
		});
	}

	/**
	 * Rotbrood: silverfish burrow out of each enemy struck, nipping at it, and at two seconds the brood moves on to
	 * the enemies standing close to each. The look: grey-olive dust boiling up out of the target, then a second
	 * swarm of dust leaping to the nearby enemies.
	 */
	@Pair(a = "compost", b = "infest", name = "Rotbrood", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Silverfish burrow out of each of up to 4 enemies: 1 damage eight times, half a second apart, and Slowness I for "
			+ "4 seconds. At 2 seconds the brood moves on to up to 2 enemies within 3 blocks of each, which take 1 damage four "
			+ "times, half a second apart.")
	public static void rotbrood(PairCast c) {
		List<LivingEntity> hosts = PairCast.first(c.enemies(), 4);
		Set<LivingEntity> infested = new HashSet<>(hosts);
		List<LivingEntity> carried = new ArrayList<>();
		c.sound(SoundEvents.SILVERFISH_AMBIENT, c.point(), 1.0F, 0.6F);
		for (LivingEntity t : hosts) {
			c.effect(t, MobEffects.SLOWNESS, 4, 0);
		}
		c.every(10, 8, frame -> {
			for (LivingEntity t : c.still(hosts)) {
				c.hurt(t, c.power);
				c.column(PairCast.dust(0x7A7458, 1.0F), t.position(), 0.9, 1.2, 6);
				c.particles(ParticleTypes.SMOKE, PairCast.mid(t), 3, 0.3, 0.0);
			}
		});
		c.later(40, () -> {
			for (LivingEntity host : c.still(hosts)) {
				int moved = 0;
				for (LivingEntity n : c.enemiesNear(PairCast.mid(host), 3 * c.radius)) {
					if (moved == 2) {
						break;
					}
					if (infested.add(n)) {
						carried.add(n);
						moved++;
						c.line(PairCast.shift(0x9AA86A, 0x4A5A2E, 0.8F), PairCast.mid(host), PairCast.mid(n), 3);
					}
				}
			}
			c.every(10, 4, frame -> {
				for (LivingEntity n : c.still(carried)) {
					c.hurt(n, c.power);
					c.column(PairCast.dust(0x7A7458, 0.9F), n.position(), 0.8, 1.0, 5);
				}
			});
		});
	}

	/**
	 * Hardset: grey concrete pours over the point and the ground round it sets in pulses. Each pulse grips the enemies
	 * standing in it, and the last hardens them. The look: a falling grey pour, then slow grey discs pressing outward
	 * on each set, with an anvil's thud.
	 */
	@Pair(a = "concreteset", b = "fossilize", name = "Hardset", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Grey concrete pours on the point: each enemy it strikes takes 1 damage and Slowness I for 1 second. A second later "
			+ "it sets 3 blocks round in four pulses, 1.5 seconds apart: each enemy within takes 2 damage and Slowness II for "
			+ "2 seconds. The last pulse hardens them to Slowness III for 3 seconds and 4 more damage.")
	public static void hardset(PairCast c) {
		Vec3 spot = c.point();
		double reach = 3 * c.radius;
		c.sound(SoundEvents.STONE_PLACE, spot, 0.9F, 0.5F);
		for (LivingEntity t : PairCast.first(c.enemies(), PairCast.MAX_TARGETS)) {
			c.hurt(t, c.power);
			c.effect(t, MobEffects.SLOWNESS, 1, 0);
		}
		c.every(4, 5, frame -> {
			Vec3 top = spot.add(0, 4 - frame * 0.8, 0);
			c.particles(PairCast.dust(0xB9BCBF, 1.0F), top, 6, 0.3, 0.0);
		});
		c.later(20, () -> c.every(30, 4, frame -> {
			boolean last = frame == 3;
			c.disc(PairCast.dust(0x8E9296, 1.0F), spot.add(0, 0.2, 0), reach, 24);
			for (LivingEntity t : c.enemiesNear(spot, reach)) {
				c.hurt(t, 2 * c.power);
				c.effect(t, MobEffects.SLOWNESS, last ? 3 : 2, last ? 2 : 1);
				if (last) {
					c.hurt(t, 4 * c.power);
				}
			}
			c.sound(SoundEvents.ANVIL_LAND, spot, 0.5F, last ? 0.7F : 1.4F);
			if (last) {
				c.shake(spot, 0.25F, 6);
			}
		}));
	}

	/**
	 * Rootward: roots take hold under each enemy struck. Three seconds on, the spots sprout saplings that heal the allies
	 * near them, and any foe that has strayed is drawn back to where its roots held it. The look: brown roots spiralling
	 * into the ground, then green shoots and a pull back along the same line.
	 */
	@Pair(a = "clockroot", b = "coppice", name = "Rootward", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Roots take hold under up to 4 enemies: 2 damage and Slowness I for 1 second. Three seconds later each spot sprouts "
			+ "a sapling that heals every ally within 3 blocks of it 2. Any of those foes that has moved more than 2 blocks away "
			+ "is drawn back to its spot if that is safe (bosses stay put), and takes 4 more damage.")
	public static void rootward(PairCast c) {
		List<LivingEntity> rooted = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.GRASS_BREAK, c.point(), 1.0F, 0.6F);
		for (LivingEntity t : rooted) {
			Vec3 spot = t.position();
			c.hurt(t, 2 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 1, 0);
			c.later(60, () -> root(c, t, spot));
		}
		c.every(6, 4, frame -> {
			for (LivingEntity t : c.still(rooted)) {
				c.spiral(PairCast.dust(0x5B4326, 1.0F), t.position(), 0.6, 0.9, 2, 12);
			}
		});
	}

	private static void root(PairCast c, LivingEntity t, Vec3 spot) {
		c.column(PairCast.dust(0x4E9A3A, 0.9F), spot, 0.8, 1.2, 10);
		for (LivingEntity a : c.alliesNear(spot, 3 * c.radius)) {
			c.heal(a, 2 * c.power);
		}
		if (!c.here(t) || t.position().distanceTo(spot) <= 2) {
			return;
		}
		if (!c.blink(t, spot)) {
			return;
		}
		c.hurt(t, 4 * c.power);
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, spot, 0.8F, 0.6F);
		c.wave(PairCast.dust(0x7FB35A, 0.9F), spot.add(0, 0.2, 0), 14, 0.3);
	}

	/**
	 * Deepcall: a sonar ping goes out from the point, and rings out twice more. Whatever it catches glows through walls.
	 * The look: pale teal rings widening from the point, each with a chime, and a glow on every enemy the ring crosses.
	 */
	@Pair(a = "deepsound", b = "deepwarn", name = "Deepcall", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A sonar ping goes out from the point. Each of up to 8 enemies it struck takes 2 damage and glows for 6 seconds. A second ring at "
			+ "half a second reaches enemies within 8 blocks, and a third at a second reaches within 12, doing the same to each "
			+ "enemy they find. Allies within 12 blocks get Night Vision for 6 seconds.")
	public static void deepcall(PairCast c) {
		Vec3 spot = c.point();
		Set<LivingEntity> pinged = new HashSet<>();
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : c.alliesNear(spot, 12 * c.radius)) {
			c.effect(a, MobEffects.NIGHT_VISION, 6, 0);
		}
		c.every(10, 3, frame -> {
			double rr = frame == 0 ? 1.5 : (frame == 1 ? 8 : 12) * c.radius;
			List<LivingEntity> hit = frame == 0 ? struck : c.enemiesNear(spot, rr);
			c.ring(PairCast.shift(0x7FF2E6, 0x1B4D6B, 1.0F), spot.add(0, 0.2, 0), rr, 28, frame * 0.3);
			c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, spot, 0.8F, 0.7F + 0.3F * frame);
			for (LivingEntity t : hit) {
				if (pinged.add(t)) {
					c.hurt(t, 2 * c.power);
					c.effect(t, MobEffects.GLOWING, 6, 0);
				}
			}
		});
	}

	/**
	 * Timberfall: a tree stands up over the point and tips over along your aim, bringing down whatever is under its
	 * length. The look: a brown trunk rising, then tipping in three frames and a crack of dust where it lands.
	 */
	@Pair(a = "fell", b = "gash", name = "Timberfall", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A tree stands up over the point, then tips over along your aim for 6 blocks in about a second. Each enemy within "
			+ "1.5 blocks of its fall takes 6 damage and is left bleeding.")
	public static void timberfall(PairCast c) {
		Vec3 base = c.point();
		Vec3 flat = flatDir(c.dir(), new Vec3(1, 0, 0));
		double length = 6 * c.radius;
		Vec3 fallen = base.add(flat.scale(length));
		c.sound(SoundEvents.WOOD_BREAK, base, 0.9F, 0.8F);
		c.every(6, 4, frame -> {
			double f = frame / 3.0;
			Vec3 tip = base.add(flat.scale(length * f)).add(0, 5 * Math.cos(f * Math.PI / 2), 0);
			c.line(PairCast.shift(0x8A5A2B, 0x3E2A16, 1.0F), base.add(0, 0.1, 0), tip, 2);
		});
		c.later(18, () -> {
			c.shake(fallen, 0.35F, 8);
			c.sound(SoundEvents.DEEPSLATE_BREAK, fallen, 1.0F, 0.6F);
			c.disc(PairCast.dust(0x6B4A2B, 1.0F), fallen.add(0, 0.1, 0), 1.2, 20);
			for (LivingEntity t : c.enemiesNear(base.add(flat.scale(length / 2)), length / 2 + 2)) {
				if (distToSegment(t.position(), base, fallen) <= 1.5 * c.radius) {
					c.strike(t, 6 * c.power);
					c.mark(t, Reactions.Mark.BLEEDING);
					c.push(t, flat.scale(0.8).add(0, 0.3, 0));
				}
			}
		});
	}

	/**
	 * Herdfeed: feed scatters outward from the point in widening rings, and each ally it reaches is fed. Anyone on a
	 * mount feeds the mount too. The look: gold and green rings spreading, spores drifting, then hearts over the herd.
	 */
	@Pair(a = "fodder", b = "steedmend", name = "Herdfeed", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Feed scatters in rings out to 5 blocks from the point. Each ally it reaches heals 2. An ally riding a mount heals "
			+ "the mount 4 and gives it Regeneration II for 6 seconds. Enemies are not touched.")
	public static void herdfeed(PairCast c) {
		Vec3 spot = c.point();
		double reach = 5 * c.radius;
		c.sound(SoundEvents.HORSE_EAT, spot, 0.9F, 1.2F);
		c.every(4, 3, frame -> {
			double r = reach * (frame + 1) / 3.0;
			c.ring(PairCast.shift(0xFFE38A, 0x8BC34A, 0.8F), spot.add(0, 0.2, 0), r, 16 + frame * 4, frame * 0.5);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, spot.add(0, 1, 0), 4, r * 0.5, 0.0);
		});
		c.later(12, () -> {
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.heal(a, 2 * c.power);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 6, 0.4, 0.0);
				if (a.getVehicle() instanceof LivingEntity mount) {
					c.heal(mount, 4 * c.power);
					c.effect(mount, MobEffects.REGENERATION, 6, 1);
					c.particles(ParticleTypes.HEART, PairCast.mid(mount).add(0, 0.6, 0), 4, 0.5, 0.0);
				}
			}
			c.sound(SoundEvents.HORSE_EAT, spot, 0.6F, 1.5F);
		});
		c.later(30, () -> c.particles(ParticleTypes.HEART, spot.add(0, 1.2, 0), 8, reach * 0.6, 0.0));
	}

	/**
	 * Slagflow: molten rock runs from the point to each enemy it finds, and a pool of magma spreads under each one.
	 * The look: orange slag streaming over the ground, then pools widening and smoking for four seconds.
	 */
	@Pair(a = "gangue", b = "magma", name = "Slagflow", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Slag runs from the point to each of up to 3 enemies over half a second, and each takes 4 damage. A pool of magma "
			+ "then spreads under each one for 4 seconds, widening to 2.6 blocks out: 2 fire damage a second to every enemy "
			+ "standing in one.")
	public static void slagflow(PairCast c) {
		Vec3 spot = c.point();
		List<Vec3> pools = new ArrayList<>();
		for (LivingEntity t : PairCast.first(c.enemies(), 3)) {
			pools.add(t.position());
		}
		c.sound(SoundEvents.FIRECHARGE_USE, spot, 0.7F, 0.6F);
		c.every(4, 3, frame -> {
			for (Vec3 pool : pools) {
				c.line(PairCast.shift(0xFFB020, 0xFF4A10, 0.9F), spot.add(0, 0.2, 0), spot.lerp(pool, (frame + 1) / 3.0).add(0, 0.1, 0), 3);
			}
		});
		c.later(10, () -> c.every(20, 5, frame -> {
			double rad = (1.0 + 0.4 * frame) * c.radius;
			for (Vec3 pool : pools) {
				c.disc(PairCast.dust(0xFF7A1A, 1.0F), pool.add(0, 0.1, 0), rad, 14);
				for (LivingEntity t : c.enemiesNear(pool, rad)) {
					c.burn(t, (frame == 0 ? 4 : 2) * c.power);
				}
			}
			c.sound(SoundEvents.FIRECHARGE_USE, spot, 0.4F, 0.8F + 0.1F * frame);
		}));
	}

	/**
	 * Warding Glyph: a ward is carved on the ground round the point, and when it is set the allies inside are cleansed
	 * of their hexes and bound in Absorption. Then the ward holds them in its light. The look: violet rings tightening
	 * into a star, a flash of enchantment, and motes of light over each ally.
	 */
	@Pair(a = "glyph_carve", b = "hexguard", name = "Warding Glyph", element = "void", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A ward glyph is carved over 4 blocks round the point in one second. Each ally inside it loses Slowness, Weakness, "
			+ "Poison, Blindness, Nausea and Darkness, gets 4 Absorption for 8 seconds, and is healed 1 every second for 10 "
			+ "seconds while it stays inside.")
	public static void wardingGlyph(PairCast c) {
		Vec3 spot = c.point();
		double reach = 4 * c.radius;
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, spot, 0.8F, 0.8F);
		c.every(4, 5, frame -> c.ring(PairCast.shift(0xB58CFF, 0x2A1B4D, 0.9F), spot.add(0, 0.1, 0),
			reach * (0.4 + 0.15 * frame), 18, frame * 0.4));
		c.later(20, () -> {
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				a.removeEffect(MobEffects.SLOWNESS);
				a.removeEffect(MobEffects.WEAKNESS);
				a.removeEffect(MobEffects.POISON);
				a.removeEffect(MobEffects.BLINDNESS);
				a.removeEffect(MobEffects.NAUSEA);
				a.removeEffect(MobEffects.DARKNESS);
				c.absorb(a, 4 * c.power, 8);
				c.particles(ParticleTypes.ENCHANT, PairCast.mid(a), 10, 0.5, 0.2);
			}
			c.star(PairCast.dust(0xD9C8FF, 1.0F), spot.add(0, 0.1, 0), 6, reach, 0);
			c.sound(SoundEvents.BEACON_POWER_SELECT, spot, 0.7F, 1.6F);
			c.every(20, 10, frame -> {
				for (LivingEntity a : c.alliesNear(spot, reach)) {
					c.heal(a, c.power);
					c.particles(ParticleTypes.END_ROD, PairCast.mid(a).add(0, 0.8, 0), 3, 0.3, 0.02);
				}
				c.ring(PairCast.dust(0xB58CFF, 0.8F), spot.add(0, 0.1, 0), reach, 18, frame * 0.3);
			});
		});
	}

	// ------------------------------------------------------------------ helpers

	/** A horizontal unit vector along {@code v}, or along {@code fallback} when {@code v} is straight up or down. */
	private static Vec3 flatDir(Vec3 v, Vec3 fallback) {
		Vec3 flat = new Vec3(v.x, 0, v.z);
		if (flat.lengthSqr() < 1.0E-4) {
			flat = new Vec3(fallback.x, 0, fallback.z);
		}
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	/** How far {@code p} is from the segment {@code a} to {@code b}. */
	private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		if (len2 < 1.0E-6) {
			return p.distanceTo(a);
		}
		double f = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(f)));
	}
}
