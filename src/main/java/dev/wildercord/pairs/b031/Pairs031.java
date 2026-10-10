package dev.wildercord.pairs.b031;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs031 {
	private Pairs031() {}

	/**
	 * Vigil Lantern: a lantern hangs over each of up to four targets and sets them alight at once. For three seconds a
	 * halo of sparks closes in over each head, then a column of light falls on the spot it stood. Whatever burns near
	 * that spot catches the light too. The look: a gold halo shrinking, then a white-gold column and an ember burst.
	 */
	@Pair(a = "lamplighter", b = "smite", name = "Vigil Lantern", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Hangs a lantern over each of up to 4 targets and sets them alight for 3 seconds at once. Then a column of light "
			+ "falls on each: 10 damage. Other enemies within 3 blocks of that spot take 3 damage and burn for 2 seconds.")
	public static void vigilLantern(PairCast c) {
		List<LivingEntity> hung = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : hung) {
			c.ignite(t, 3);
		}
		c.sound(SoundEvents.LANTERN_PLACE, c.point(), 0.8F, 0.9F);
		c.every(4, 16, frame -> {
			for (LivingEntity t : c.still(hung)) {
				Vec3 head = PairCast.mid(t).add(0, t.getBbHeight() / 2 + 0.6, 0);
				c.ring(PairCast.shift(0xFFE7A3, 0xFFB347, 0.9F), head, 1.4 - frame * 0.07, 14, frame * 0.35);
				c.particles(ParticleTypes.SMALL_FLAME, head.add(0, 0.4, 0), 2, 0.1, 0.01);
			}
		});
		c.later(60, () -> {
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, c.point(), 0.4F, 1.8F);
			for (LivingEntity t : c.still(hung)) {
				Vec3 spot = c.ground(PairCast.mid(t));
				c.column(PairCast.shift(0xFFFFFF, 0xFFD27A, 0.9F), spot, 0.7, 3.5, 30);
				c.shake(spot, 0.2F, 5);
				c.hurt(t, 10 * c.power);
				for (LivingEntity n : c.enemiesNear(spot.add(0, 1, 0), 3)) {
					if (n != t) {
						c.hurt(n, 3 * c.power);
						c.ignite(n, 2);
					}
				}
			}
		});
	}

	/**
	 * Hearthfeast: a hearth glows at the point for six seconds. Allies beside it get a hot meal's absorption at once, then
	 * four warm pulses, every two seconds, heal those still beside it. The look: campfire smoke rising, a ring of
	 * orange embers round the hearth, and hearts over whoever is fed.
	 */
	@Pair(a = "hearthcook", b = "nourish", name = "Hearthfeast", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "A hearth glows at the point for 6 seconds. At once each ally within 3 blocks gets 4 absorption health for 8 seconds. "
			+ "Four times (at once, then every 2 seconds), each ally still within 3 blocks heals 2 health.")
	public static void hearthfeast(PairCast c) {
		Vec3 hearth = c.ground(c.point());
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, hearth, 0.9F, 1.0F);
		for (LivingEntity t : c.alliesNear(hearth, 3)) {
			c.absorb(t, 4 * c.power, 8);
		}
		c.every(4, 31, frame -> {
			c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hearth.add(0, 0.4, 0), 1, 0.2, 0.01);
			c.ring(PairCast.shift(0xFFD27A, 0xFF8A3D, 0.9F), hearth.add(0, 0.1, 0), 3, 20, frame * 0.3);
			if (frame % 10 != 0) {
				return;
			}
			c.sound(SoundEvents.GENERIC_EAT, hearth, 0.5F, 0.9F);
			for (LivingEntity t : c.alliesNear(hearth, 3)) {
				c.heal(t, 2 * c.power);
				c.particles(ParticleTypes.HEART, PairCast.mid(t).add(0, 0.5, 0), 2, 0.3, 0.02);
			}
		});
	}

	/**
	 * Kiln Carapace: allies within four blocks are wrapped in fired clay as the point turns to a kiln. Clay closes round
	 * each of them for 0.4 seconds, then they stand in Resistance and four absorption. Three seconds later the kiln fires:
	 * enemies close to the point are scorched. The look: orange-brown rings closing on the wrapped, a disc of kiln heat,
	 * and white ash.
	 */
	@Pair(a = "kilnbake", b = "stoneskin", name = "Kiln Carapace", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "radius", "duration"},
		text = "Wraps each ally within 4 blocks of the point in fired clay for 10 seconds: Resistance II and 4 absorption health. "
			+ "Three seconds later the kiln fires: enemies within 2 blocks of the point take 3 damage and burn for 2 seconds.")
	public static void kilnCarapace(PairCast c) {
		Vec3 kiln = c.ground(c.point());
		List<LivingEntity> wrapped = c.alliesNear(kiln, 4 * c.radius);
		c.sound(SoundEvents.FIRECHARGE_USE, kiln, 0.6F, 0.7F);
		c.every(3, 7, frame -> {
			for (LivingEntity t : c.still(wrapped)) {
				c.ring(PairCast.shift(0xE0A36B, 0x9A5A32, 0.9F), PairCast.mid(t), 1.3 - frame * 0.15, 14, frame * 0.4);
			}
		});
		for (LivingEntity t : wrapped) {
			c.effect(t, MobEffects.RESISTANCE, 10, 1);
			c.absorb(t, 4 * c.power, 10);
		}
		c.later(60, () -> {
			c.sound(SoundEvents.LAVA_POP, kiln, 1.0F, 0.8F);
			c.disc(PairCast.shift(0xFF8A3D, 0x5A2A10, 1.0F), kiln.add(0, 0.1, 0), 2 * c.radius, 30);
			c.particles(ParticleTypes.WHITE_ASH, kiln.add(0, 0.5, 0), 20, 1.0, 0.05);
			for (LivingEntity t : c.enemiesNear(kiln, 2 * c.radius)) {
				c.hurt(t, 3 * c.power);
				c.ignite(t, 2);
			}
		});
	}

	/**
	 * Heat Echo: three sonar pulses leave the point, a second apart, each reaching four blocks further than the last. Each
	 * enemy a pulse first reaches takes damage and glows. The struck ones are dazed by the first ping. The look: orange
	 * rings expanding through the ground, a chime with each ping, and electric sparks where it struck.
	 */
	@Pair(a = "echolocate", b = "lava_sense", name = "Heat Echo", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Three sonar pulses leave the point one second apart, reaching 4, 8 and 12 blocks. Each enemy a pulse first reaches "
			+ "takes 2 damage and glows for 6 seconds. Enemies the spell struck are dazed (Slowness II) for 3 seconds.")
	public static void heatEcho(PairCast c) {
		Vec3 at = c.ground(c.point()).add(0, 1, 0);
		for (LivingEntity t : c.enemies()) {
			c.effect(t, MobEffects.SLOWNESS, 3, 1);
		}
		Set<LivingEntity> echoed = new HashSet<>();
		c.every(20, 3, frame -> {
			double reach = (4 + 4 * frame) * c.radius;
			c.ring(PairCast.shift(0xFFB04A, 0xFF4D1A, 0.9F), at, reach, 28, frame * 0.5);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.6F, 0.8F + 0.3F * frame);
			for (LivingEntity t : c.enemiesNear(at, reach)) {
				if (echoed.add(t)) {
					c.hurt(t, 2 * c.power);
					c.effect(t, MobEffects.GLOWING, 6, 0);
					c.particles(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(t), 6, 0.4, 0.05);
				}
			}
		});
	}

	/**
	 * Sealed Ward: a ward of sealed heat stands three blocks round the point for six seconds. Allies inside are put out
	 * and fire-proofed at once and every second; enemies inside are scorched and slowed each second. The look: a ring of
	 * orange light on the ground, a wall of glowing ash, and a lava pop each beat.
	 */
	@Pair(a = "fireward", b = "lavaseal", name = "Sealed Ward", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "A ward of sealed heat stands 3 blocks round the point for 6 seconds. Allies inside are put out and get Fire Resistance "
			+ "for 30 seconds, every second. Enemies inside take 1 damage and Slowness I for 2 seconds, every second.")
	public static void sealedWard(PairCast c) {
		Vec3 ward = c.ground(c.point());
		double r = 3 * c.radius;
		c.sound(SoundEvents.LAVA_POP, ward, 0.8F, 0.7F);
		c.every(20, 6, frame -> {
			c.ring(PairCast.shift(0xFFB347, 0x7A2A00, 1.0F), ward.add(0, 0.1, 0), r, 28, frame * 0.2);
			c.column(PairCast.dust(0xFFE0A0, 0.8F), ward, r, 2.5, 14);
			for (LivingEntity t : c.alliesNear(ward, r)) {
				c.douse(t);
				c.effect(t, MobEffects.FIRE_RESISTANCE, 30, 0);
			}
			for (LivingEntity t : c.enemiesNear(ward, r)) {
				c.hurt(t, 1 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 0);
			}
		});
	}

	/**
	 * Blinding Flare: each enemy struck is blinded and darkened at once. A column of smoke then rises at the point for
	 * eight seconds, and four times it stings the enemies round it and blinds them again. The look: a dark violet flash,
	 * a grey signal column rising, and a sculk click each pulse.
	 */
	@Pair(a = "blind", b = "smoke_signal", name = "Blinding Flare", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Blinds and darkens each enemy it strikes for 5 seconds (3 on players). A smoke column rises at the point for 8 seconds: "
			+ "four times, every 2 seconds, enemies within 3 blocks take 2 damage and are blinded for 2 more seconds.")
	public static void blindingFlare(PairCast c) {
		Vec3 column = c.ground(c.point());
		for (LivingEntity t : c.enemies()) {
			int seconds = t instanceof Player ? 3 : 5;
			c.effect(t, MobEffects.BLINDNESS, seconds, 0);
			c.effect(t, MobEffects.DARKNESS, seconds, 0);
		}
		c.sound(SoundEvents.CANDLE_EXTINGUISH, column, 0.9F, 0.6F);
		c.every(4, 41, frame -> {
			c.particles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, column.add(0, 0.5 + frame * 0.06, 0), 2, 0.25, 0.02);
			if (frame == 0 || frame % 10 != 0) {
				return;
			}
			c.sound(SoundEvents.SCULK_CLICKING, column, 0.5F, 0.7F);
			for (LivingEntity t : c.enemiesNear(column.add(0, 1, 0), 3)) {
				c.wither(t, 2 * c.power);
				c.effect(t, MobEffects.BLINDNESS, 2, 0);
			}
		});
	}

	/**
	 * Pottage: a salve is spooned out for the allies beside the point: they heal, gain Regeneration and are put out.
	 * Three seconds of steam later the pot is served and they heal again and gain absorption. The look: a steam ring
	 * rising off the pot for three seconds, then a wave of happy sparks.
	 */
	@Pair(a = "salve", b = "stewpot", name = "Pottage", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Each ally within 4 blocks heals 2 health, gains Regeneration I for 8 seconds and is put out. Three seconds later "
			+ "the pot is served: each ally still within 4 blocks heals 3 more and gains 2 absorption health for 8 seconds.")
	public static void pottage(PairCast c) {
		Vec3 pot = c.ground(c.point());
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, pot, 0.7F, 1.0F);
		for (LivingEntity t : c.alliesNear(pot, 4 * c.radius)) {
			c.heal(t, 2 * c.power);
			c.effect(t, MobEffects.REGENERATION, 8, 0);
			c.douse(t);
			c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 4, 0.4, 0.02);
		}
		c.every(4, 16, frame -> {
			c.ring(PairCast.shift(0xFFE3A3, 0xF2F2F2, 0.8F), pot.add(0, 0.3 + frame * 0.08, 0), 0.8, 12, frame * 0.5);
			c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pot.add(0, 0.5, 0), 2, 0.4, 0.01);
		});
		c.later(60, () -> {
			c.sound(SoundEvents.GENERIC_EAT, pot, 0.9F, 0.9F);
			c.wave(ParticleTypes.HAPPY_VILLAGER, pot.add(0, 0.5, 0), 16, 0.25);
			for (LivingEntity t : c.alliesNear(pot, 4 * c.radius)) {
				c.heal(t, 3 * c.power);
				c.absorb(t, 2 * c.power, 8);
			}
		});
	}

	/**
	 * Rimesteam: each enemy struck takes a touch of freeze, is slowed and chilled. Two seconds later a steam breath fills
	 * the point, and what is still chilled takes a hard scald and loses its chill. The look: snowflakes on the frosted,
	 * then a white steam sphere that bursts and rolls outward.
	 */
	@Pair(a = "chill", b = "thawfield", name = "Rimesteam", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Each enemy it strikes takes 1 freeze damage, gains Slowness II for 6 seconds and is chilled for 6 seconds. Two seconds later steam "
			+ "fills 3 blocks round the point: 6 damage to each enemy there still chilled, which melts it, and 2 to the rest.")
	public static void rimesteam(PairCast c) {
		Vec3 breath = c.ground(c.point()).add(0, 1, 0);
		List<LivingEntity> frosted = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.SNOW_PLACE, breath, 0.8F, 0.8F);
		for (LivingEntity t : frosted) {
			c.freeze(t, 1 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 6, 1);
			c.chill(t, 6);
		}
		c.every(4, 6, frame -> {
			for (LivingEntity t : c.still(frosted)) {
				c.particles(ParticleTypes.SNOWFLAKE, PairCast.mid(t), 3, 0.4, 0.02);
			}
		});
		c.later(40, () -> {
			c.sound(SoundEvents.LAVA_EXTINGUISH, breath, 1.0F, 1.2F);
			c.sphere(PairCast.shift(0xE8F4FF, 0xB0C4DE, 1.0F), breath, 3, 40);
			c.wave(ParticleTypes.CLOUD, breath, 24, 0.3);
			for (LivingEntity t : c.enemiesNear(breath, 3)) {
				if (t.getTicksFrozen() > 0) {
					c.hurt(t, 6 * c.power);
					t.setTicksFrozen(0);
				} else {
					c.hurt(t, 2 * c.power);
				}
			}
		});
	}

	/**
	 * Ember Dash: embers wind round you for 0.4 seconds, then you dash up to 10 blocks along your facing, level, and stop
	 * at a wall. Whatever lies within 1.5 blocks of the path is burned and knocked aside. The look: an ember spiral
	 * round you, a streak of flame along the path and a flame trail after it.
	 */
	@Pair(a = "dash", b = "trail_blaze", name = "Ember Dash", element = "fire", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "Dashes you up to 10 blocks along your facing, level, stopping at a wall. Each enemy within 1.5 blocks of your path "
			+ "takes 4 damage, burns for 2 seconds and is knocked aside.")
	public static void emberDash(PairCast c) {
		LivingEntity self = c.caster;
		c.sound(SoundEvents.FIRECHARGE_USE, self.position(), 0.5F, 1.4F);
		c.every(2, 3, frame -> {
			Vec3 at = PairCast.mid(self);
			c.spiral(PairCast.shift(0xFFD36B, 0xFF5A1F, 0.8F), at.add(0, -0.6, 0), 0.8, 1.6, 1.0, 10);
		});
		c.later(6, () -> dash(c, self));
	}

	/** The dash itself: runs along the caster's facing, stops short of a wall, and burns whatever the path passes. */
	private static void dash(PairCast c, LivingEntity self) {
		Vec3 from = self.position();
		Vec3 run = flat(c.dir());
		Vec3 want = from.add(run.scale(10));
		BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.9, 0), want.add(0, 0.9, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		Vec3 goal = wall.getType() == HitResult.Type.MISS ? want : wall.getLocation().subtract(run.scale(0.6));
		if (goal.distanceTo(from) < 1.5) {
			c.sound(SoundEvents.WOOL_STEP, from, 0.6F, 0.8F);
			return;
		}
		Vec3 landing = c.ground(goal);
		if (!c.blink(self, landing)) {
			return;
		}
		Vec3 mid = from.add(landing).scale(0.5);
		double reach = landing.distanceTo(from) / 2 + 2;
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, from, 0.9F, 1.2F);
		c.punch(0.15F);
		c.line(PairCast.shift(0xFFE08A, 0xFF5A1F, 0.9F), from.add(0, 1, 0), landing.add(0, 1, 0), 3);
		c.every(2, 5, frame -> c.particles(ParticleTypes.FLAME, from.lerp(landing, frame / 4.0).add(0, 0.6, 0), 4, 0.25, 0.02));
		for (LivingEntity t : c.enemiesNear(mid, reach)) {
			if (segmentDistance(PairCast.mid(t), from, landing) <= 1.5) {
				c.hurt(t, 4 * c.power);
				c.ignite(t, 2);
				c.knockFrom(t, mid, 1.0, 0.3);
			}
		}
		c.sound(SoundEvents.FIRECHARGE_USE, landing, 0.8F, 0.9F);
	}

	/**
	 * Hush Smoke: calm smoke fills four blocks round the point for six seconds. Every quarter second it calms each creature
	 * in it, and what the spell struck; players struck get Weakness instead, bosses Slowness. The look: a grey-violet
	 * ring contracting on the ground, and soft smoke that drifts and settles.
	 */
	@Pair(a = "calmsmoke", b = "pacify", name = "Hush Smoke", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Calm smoke fills 4 blocks round the point for 6 seconds. Every quarter second each creature in it forgets its target, "
			+ "and so does each creature it struck (not bosses). Players it struck get Weakness I for 6 seconds; other creatures it struck get Slowness I for 6 seconds.")
	public static void hushSmoke(PairCast c) {
		Vec3 base = c.ground(c.point());
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, base, 0.6F, 0.7F);
		for (LivingEntity t : struck) {
			if (t instanceof Player) {
				c.effect(t, MobEffects.WEAKNESS, 6, 0);
			} else {
				c.effect(t, MobEffects.SLOWNESS, 6, 0);
			}
		}
		c.every(5, 25, frame -> {
			c.ring(PairCast.shift(0xE6ECFF, 0x9AA5C4, 0.9F), base.add(0, 0.3, 0), 4 * c.radius * (1.0 - frame * 0.02), 24, frame * 0.3);
			c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.add(0, 0.6, 0), 3, 1.2, 0.01);
			for (LivingEntity t : c.still(struck)) {
				calm(c, t);
			}
			for (LivingEntity t : c.enemiesNear(base, 4 * c.radius)) {
				calm(c, t);
			}
		});
	}

	/** Makes {@code t} forget its target, if it is a creature that may be moved (never a boss). */
	private static void calm(PairCast c, LivingEntity t) {
		if (t instanceof Mob mob && c.movable(mob)) {
			mob.setTarget(null);
		}
	}

	/** The flat (horizontal) unit direction of {@code v}, or straight ahead when it has none. */
	private static Vec3 flat(Vec3 v) {
		Vec3 flat = new Vec3(v.x, 0, v.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	/** The distance from {@code p} to the segment from {@code a} to {@code b}. */
	private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len = ab.lengthSqr();
		double f = len < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
		return p.distanceTo(a.add(ab.scale(f)));
	}
}
