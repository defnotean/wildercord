package dev.wildercord.pairs.b015;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs015 {
	private Pairs015() {}

	/**
	 * Pyrefall: a lift and a rain of fire. Each target is flung high; three blaze fireballs drop on it, and the
	 * last bursts over everything near. The look: orange sparks spiralling up, then falling flame lines.
	 */
	@Pair(a = "blazecall", b = "skyburst", name = "Pyrefall", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Flings up to 6 enemies high into the air. Three blaze fireballs then fall on each one, half a second apart: "
			+ "3 fire damage each, setting it alight. The third bursts for 4 more to every enemy within 2.5 blocks. Never breaks blocks.")
	public static void pyrefall(PairCast c) {
		List<LivingEntity> flung = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : flung) {
			c.lift(t, 0.7);
			c.spiral(PairCast.shift(0xFFD36B, 0xFF6A1A, 0.9F), PairCast.mid(t), 0.6, 2.5, 2, 20);
		}
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, c.point(), 0.6F, 1.4F);
		c.every(10, 4, frame -> {
			if (frame == 0) {
				return;
			}
			Set<LivingEntity> blast = new LinkedHashSet<>();
			for (LivingEntity t : c.still(flung)) {
				Vec3 at = PairCast.mid(t);
				c.line(PairCast.shift(0xFFF4C2, 0xFF6A1A, 1.1F), at.add(0, 6, 0), at, 3);
				c.particles(ParticleTypes.FLAME, at, 8, 0.3, 0.05);
				c.burn(t, 3 * c.power);
				c.ignite(t, 2);
				c.sound(SoundEvents.BLAZE_SHOOT, at, 0.6F, 1.0F + 0.1F * frame);
				if (frame == 3) {
					c.wave(PairCast.dust(0xFFB347, 1.2F), at, 20, 0.3);
					blast.addAll(c.enemiesNear(at, 2.5 * c.radius));
				}
			}
			for (LivingEntity e : blast) {
				c.burn(e, 4 * c.power);
			}
		});
	}

	/**
	 * Scaldtide: steam and storm. A scalding tide draws enemies in and soaks them; a second later the storm
	 * finds every soaked one. The look: pale steam wound up in spirals, then a lightning strike on each.
	 */
	@Pair(a = "boiling_surge", b = "thunder_tide", name = "Scaldtide", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Steam pulls up to 6 enemies within 4 blocks of the spot towards it and scalds each for 4 fire damage (7 with "
			+ "water within 2 blocks of the spot), soaking it. A second later the storm strikes each one for 3 lightning damage.")
	public static void scaldtide(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> scalded = PairCast.first(c.enemiesNear(spot, 4 * c.radius), 6);
		boolean water = waterNear(c, spot);
		double scald = (water ? 7 : 4) * c.power;
		for (LivingEntity t : scalded) {
			c.pullTo(t, spot, 0.8);
			c.burn(t, scald);
			c.mark(t, Reactions.Mark.SOAKED);
			c.line(PairCast.dust(0xB8F2FF, 0.9F), spot, PairCast.mid(t), 3);
		}
		c.sound(SoundEvents.FIRE_EXTINGUISH, spot, 1.0F, water ? 0.7F : 1.1F);
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(scalded)) {
				c.spiral(PairCast.dust(0xE8F8FF, 1.0F), PairCast.mid(t), 0.7, 1.6, 1.5, 16);
			}
		});
		c.later(20, () -> {
			for (LivingEntity t : c.still(scalded)) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, spot.add(0, 1, 0), PairCast.mid(t), 0.4, 3);
				c.shock(t, 3 * c.power);
			}
			c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, spot, 0.3F, 1.6F);
		});
	}

	/**
	 * Hearthcurrent: a relay of warmth. Your heart lights and the charge hops ally to ally, each hop lighting a spark
	 * path behind it. The look: gold rings handed along a chain of bright arcs.
	 */
	@Pair(a = "cinderheart", b = "surge", name = "Hearthcurrent", element = "storm", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "You are fire-proof for 8 seconds. The charge then hops from you to up to 4 allies, each within 6 blocks of "
			+ "the last, half a second apart. Each gets Speed I and Strength I for 6 seconds and 4 absorption.")
	public static void hearthcurrent(PairCast c) {
		List<LivingEntity> chain = new ArrayList<>();
		chain.add(c.caster);
		LivingEntity last = c.caster;
		for (int i = 0; i < 4; i++) {
			LivingEntity next = null;
			for (LivingEntity a : c.alliesNear(PairCast.mid(last), 6)) {
				if (!chain.contains(a)) {
					next = a;
					break;
				}
			}
			if (next == null) {
				break;
			}
			chain.add(next);
			last = next;
		}
		c.effect(c.caster, MobEffects.FIRE_RESISTANCE, 8, 0);
		c.every(10, chain.size(), i -> {
			LivingEntity a = chain.get(i);
			if (!a.isAlive()) {
				return;
			}
			c.effect(a, MobEffects.SPEED, 6, 0);
			c.effect(a, MobEffects.STRENGTH, 6, 0);
			c.absorb(a, 4 * c.power, 6);
			Vec3 at = PairCast.mid(a);
			if (i > 0) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(chain.get(i - 1)), at, 0.3, 3);
			}
			c.ring(PairCast.dust(0xFFB347, 0.9F), at, 0.9, 16, i);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.7F, 1.0F + 0.15F * i);
		});
	}

	/**
	 * Brandfuse: fire on a fuse. Lit enemies pass the flame along a chain to the nearest unlit enemy, four times;
	 * five seconds on, everything the flame reached catches again. The look: a fiery line racing from body to body.
	 */
	@Pair(a = "conflagration", b = "everburn", name = "Brandfuse", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Lights up to 6 targets for 5 seconds, 3 fire damage each. The fuse runs on from the last lit enemy to the nearest "
			+ "unlit one within 6 blocks, four times: 2 fire damage each, lit for 3 seconds. Five seconds on, all it reached catch again for 2.")
	public static void brandfuse(PairCast c) {
		List<LivingEntity> lit = new ArrayList<>(PairCast.first(c.enemies(), 6));
		for (LivingEntity t : lit) {
			c.burn(t, 3 * c.power);
			c.ignite(t, 5);
			c.particles(ParticleTypes.FLAME, PairCast.mid(t), 10, 0.3, 0.05);
		}
		c.sound(SoundEvents.FIRECHARGE_USE, c.point(), 0.8F, 1.0F);
		c.every(8, 4, frame -> {
			if (lit.isEmpty()) {
				return;
			}
			LivingEntity src = lit.get(lit.size() - 1);
			if (!src.isAlive()) {
				return;
			}
			LivingEntity next = null;
			for (LivingEntity e : c.enemiesNear(PairCast.mid(src), 6 * c.radius)) {
				if (e.isAlive() && !lit.contains(e)) {
					next = e;
					break;
				}
			}
			if (next == null) {
				return;
			}
			Vec3 from = PairCast.mid(src);
			Vec3 to = PairCast.mid(next);
			c.zigzag(ParticleTypes.FLAME, from, to, 0.25, 3);
			c.burn(next, 2 * c.power);
			c.ignite(next, 3);
			c.sound(SoundEvents.BLAZE_SHOOT, to, 0.5F, 1.2F);
			lit.add(next);
		});
		c.later(c.ticks(5), () -> {
			for (LivingEntity t : c.still(lit)) {
				c.ignite(t, 2);
			}
		});
	}

	/**
	 * Lit Fuse: each target carries a spark-fed fuse, then the blast. A fuse ends early if its target dies, so a
	 * dying bomb goes off where it fell. The look: a smoking spark over each target, then a flash and a throw.
	 */
	@Pair(a = "explode", b = "primer", name = "Lit Fuse", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Sets up to 4 targets alight at once. Each carries a fuse for 2 seconds, or until it dies, then blasts everything "
			+ "within 3 blocks for 6 fire damage and throws it back. An enemy is blasted only once. Never breaks blocks.")
	public static void litFuse(PairCast c) {
		List<LivingEntity> fused = PairCast.first(c.enemies(), 4);
		int n = fused.size();
		Vec3[] lastAt = new Vec3[n];
		boolean[] done = new boolean[n];
		Set<LivingEntity> struck = new HashSet<>();
		for (int i = 0; i < n; i++) {
			LivingEntity t = fused.get(i);
			lastAt[i] = PairCast.mid(t);
			c.ignite(t, 2);
			c.sound(SoundEvents.FIRECHARGE_USE, lastAt[i], 0.6F, 1.3F);
		}
		c.every(4, 11, frame -> {
			for (int i = 0; i < n; i++) {
				if (done[i]) {
					continue;
				}
				LivingEntity t = fused.get(i);
				if (t.isAlive()) {
					lastAt[i] = PairCast.mid(t);
				}
				if (!t.isAlive() || frame == 10) {
					done[i] = true;
					detonate(c, lastAt[i], struck);
				} else {
					Vec3 top = lastAt[i].add(0, 1.0, 0);
					c.line(PairCast.dust(0xFFE14D, 0.6F), lastAt[i].add(0, 0.6, 0), top, 3);
					c.particles(ParticleTypes.FLAME, top, 1, 0.05, 0.01);
					c.particles(ParticleTypes.SMOKE, top, 1, 0.05, 0.01);
				}
			}
		});
	}

	private static void detonate(PairCast c, Vec3 at, Set<LivingEntity> struck) {
		c.wave(PairCast.shift(0xFFE14D, 0x5A1F0A, 1.2F), at, 20, 0.35);
		c.particles(ParticleTypes.EXPLOSION, at, 1, 0, 0);
		c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.7F, 1.2F);
		c.shake(at, 0.3F, 6);
		for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
			if (struck.add(e)) {
				c.burn(e, 6 * c.power);
				c.knockFrom(e, at, 0.9, 0.4);
			}
		}
	}

	/**
	 * Warmgale: a breeze and a glow. Allies in reach are cleansed and quickened, and a warm glow stays on the spot
	 * for eight seconds, thawing and healing whoever stands in it. The look: a gold ring turning on the ground.
	 */
	@Pair(a = "hearthglow", b = "zephyr", name = "Warmgale", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Allies within 4 blocks lose blindness, darkness, nausea and slowness, and get Speed I and Jump Boost I for 6 seconds. "
			+ "For 8 seconds a warm glow holds on the spot: allies in it are thawed, get Regeneration I for 2 seconds each second, and are nudged along the breeze.")
	public static void warmgale(PairCast c) {
		Vec3 spot = c.point();
		double r = 4 * c.radius;
		Vec3 d = c.dir();
		Vec3 breeze = new Vec3(d.x, 0, d.z).scale(0.3);
		for (LivingEntity a : c.alliesNear(spot, r)) {
			a.removeEffect(MobEffects.BLINDNESS);
			a.removeEffect(MobEffects.DARKNESS);
			a.removeEffect(MobEffects.NAUSEA);
			a.removeEffect(MobEffects.SLOWNESS);
			c.effect(a, MobEffects.SPEED, 6, 0);
			c.effect(a, MobEffects.JUMP_BOOST, 6, 0);
		}
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, spot, 0.5F, 1.4F);
		int pulses = Math.max(1, (int) Math.round(8 * c.duration));
		c.every(20, pulses, frame -> {
			for (LivingEntity a : c.alliesNear(spot, r)) {
				a.setTicksFrozen(0);
				c.effect(a, MobEffects.REGENERATION, 2, 0);
				c.push(a, breeze);
				c.spiral(PairCast.dust(0xFFE7A8, 0.9F), PairCast.mid(a), 0.5, 1.6, 1, 10);
			}
			c.ring(PairCast.shift(0xFFB36B, 0xFFF4C2, 1.0F), spot.add(0, 0.15, 0), r, 28, frame * 0.35);
			c.column(PairCast.dust(0xFFE7A8, 0.8F), spot, r * 0.6, 2.2, 14);
		});
	}

	/**
	 * Choir Hearth: a hymn in two verses. The first heals the allies gathered; two seconds later the answer shields
	 * them. The look: notes rising from each ally, then a column of gold light.
	 */
	@Pair(a = "hearthsong", b = "morale", name = "Choir Hearth", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Heals each ally within 6 blocks 2 health, plus 1 for every other ally there (up to 6 more). Two seconds later the "
			+ "choir answers: each gets 2 absorption per other ally there (up to 6), lasting 20 seconds.")
	public static void choirHearth(PairCast c) {
		List<LivingEntity> choir = PairCast.first(c.alliesNear(c.point(), 6), 8);
		int others = Math.max(0, choir.size() - 1);
		for (LivingEntity a : choir) {
			c.heal(a, (2 + Math.min(6, others)) * c.power);
			c.ring(PairCast.dust(0xFFD27A, 1.0F), PairCast.mid(a).add(0, -0.5, 0), 1.0, 18, 0);
		}
		c.sound(SoundEvents.NOTE_BLOCK_BELL, c.point(), 0.8F, 1.0F);
		c.every(10, 4, frame -> {
			for (LivingEntity a : c.still(choir)) {
				c.particles(ParticleTypes.NOTE, PairCast.mid(a).add(0, 1, 0), 2, 0.4, 1.0);
				c.ring(PairCast.dust(0xFFE9A8, 0.8F), PairCast.mid(a), 1.0 + frame * 0.3, 18, frame * 0.4);
			}
		});
		c.later(40, () -> {
			for (LivingEntity a : c.still(choir)) {
				c.absorb(a, Math.min(6, 2 * others) * c.power, 20);
				c.column(PairCast.shift(0xFFD27A, 0xFFFFFF, 0.9F), a.position(), 0.6, a.getBbHeight(), 12);
			}
			c.sound(SoundEvents.NOTE_BLOCK_FLUTE, c.point(), 0.8F, 1.3F);
		});
	}

	/**
	 * Thermocline: a front of fire and frost rolling forward. Fire takes an enemy first, then frost catches it on the
	 * next pass. The look: a gold ring that turns pale blue as it travels, with flame and snow beside it.
	 */
	@Pair(a = "blizzard", b = "inferno", name = "Thermocline", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A front of fire and frost rolls 6 blocks the way you faced over 2 seconds, 2.2 blocks wide. Each enemy it reaches "
			+ "takes 3 fire damage and is alight for 2 seconds; the next time the front catches it, it takes 1 frost damage and Slowness II for 2 seconds.")
	public static void thermocline(PairCast c) {
		Vec3 start = c.point();
		Vec3 along = flatDir(c);
		Set<LivingEntity> burnt = new HashSet<>();
		Set<LivingEntity> chilled = new HashSet<>();
		c.every(10, 5, s -> {
			Vec3 front = start.add(along.scale(1.5 * s));
			c.ring(PairCast.shift(0xFFD27A, 0x9FE8FF, 1.0F), front.add(0, 0.2, 0), 2.2 * c.radius, 24, s * 0.3);
			c.particles(ParticleTypes.FLAME, front.add(0, 0.5, 0), 4, 1.0, 0.02);
			c.particles(ParticleTypes.SNOWFLAKE, front.add(0, 1.0, 0), 4, 1.0, 0.02);
			c.sound(s == 0 ? SoundEvents.FIRECHARGE_USE : SoundEvents.POWDER_SNOW_STEP, front, 0.7F, 0.8F + 0.1F * s);
			for (LivingEntity e : c.enemiesNear(front, 2.2 * c.radius)) {
				if (!burnt.contains(e)) {
					burnt.add(e);
					c.burn(e, 3 * c.power);
					c.ignite(e, 2);
				} else if (!chilled.contains(e)) {
					chilled.add(e);
					c.freeze(e, 1 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 2, 1);
				}
			}
		});
	}

	/** The horizontal way the caster was facing, as a unit vector (east if they face straight up or down). */
	private static Vec3 flatDir(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	/**
	 * Emberbloom: a flower of fire that heals first and bursts last. The look: pink petals at each ally's feet, then
	 * orange petals flung out from them.
	 */
	@Pair(a = "bloom", b = "phoenix_pyre", name = "Emberbloom", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 6 blocks get Regeneration I for 6 seconds and a ring of petals. A second later the petals open: 4 health "
			+ "and 4 absorption for 10 seconds each. At two seconds the flower bursts: each enemy within 2.5 blocks of an ally takes 3 fire damage, once.")
	public static void emberbloom(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> bloom = PairCast.first(c.alliesNear(spot, 6), 8);
		for (LivingEntity a : bloom) {
			c.effect(a, MobEffects.REGENERATION, 6, 0);
		}
		c.every(20, 3, frame -> {
			for (LivingEntity a : c.still(bloom)) {
				Vec3 at = PairCast.mid(a);
				if (frame == 0) {
					c.ring(PairCast.dust(0xFF9BD2, 1.0F), at, 1.2, 16, 0);
				} else if (frame == 1) {
					c.heal(a, 4 * c.power);
					c.absorb(a, 4 * c.power, 10);
					c.spiral(PairCast.shift(0xFF9BD2, 0xFFD27A, 0.9F), at, 0.9, 1.2, 1, 14);
				}
			}
			if (frame == 2) {
				Set<LivingEntity> burst = new LinkedHashSet<>();
				for (LivingEntity a : c.still(bloom)) {
					Vec3 at = PairCast.mid(a);
					c.wave(PairCast.dust(0xFF7A1F, 1.0F), at, 16, 0.25);
					burst.addAll(c.enemiesNear(at, 2.5 * c.radius));
				}
				for (LivingEntity e : burst) {
					c.burn(e, 3 * c.power);
				}
				c.sound(SoundEvents.FIRECHARGE_USE, spot, 0.8F, 1.4F);
			}
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, spot, 0.5F, 0.8F + 0.2F * frame);
		});
	}

	/**
	 * Wraithfire: black soul flames that pass on. A target that dies burning sends its soul to the nearest other
	 * enemy. The look: soul-blue columns rising off each target, and a streak leaping to the next.
	 */
	@Pair(a = "blackflame", b = "soulfire", name = "Wraithfire", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Black soul flames cling to up to 6 targets: 2 withering damage at once, then again every second for 5 seconds. "
			+ "A target that dies burning sends its soul to the nearest other enemy within 4 blocks, which takes 2 withering damage and is set alight for 3 seconds.")
	public static void wraithfire(PairCast c) {
		List<LivingEntity> souls = PairCast.first(c.enemies(), 6);
		Map<LivingEntity, Vec3> lastAt = new HashMap<>();
		Set<LivingEntity> spread = new HashSet<>();
		int pulses = Math.max(1, (int) Math.round(6 * c.duration));
		c.every(20, pulses, frame -> {
			for (LivingEntity t : souls) {
				if (t.isAlive()) {
					lastAt.put(t, PairCast.mid(t));
					c.wither(t, 2 * c.power);
					c.column(ParticleTypes.SOUL, t.position(), 0.5, t.getBbHeight(), 10);
				} else if (lastAt.containsKey(t) && spread.add(t)) {
					Vec3 at = lastAt.get(t);
					LivingEntity next = c.nearestEnemy(at, 4, t);
					if (next != null) {
						c.line(ParticleTypes.SOUL, at, PairCast.mid(next), 3);
						c.wither(next, 2 * c.power);
						c.ignite(next, 3);
						c.sound(SoundEvents.BLAZE_AMBIENT, at, 0.8F, 0.6F);
					}
				}
			}
			c.sound(SoundEvents.BLAZE_AMBIENT, c.point(), 0.4F, 1.4F);
		});
	}

	private static boolean waterNear(PairCast c, Vec3 at) {
		BlockPos centre = BlockPos.containing(at);
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, -2, -2), centre.offset(2, 2, 2))) {
			if (c.level.getFluidState(p).is(Fluids.WATER)) {
				return true;
			}
		}
		return false;
	}
}
