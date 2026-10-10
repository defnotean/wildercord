package dev.wildercord.pairs.b001;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The second batch of hand-made pairs: ten fusions, each with its own mechanic, palette, sounds and beats. */
public final class Pairs001 {
	private Pairs001() {}

	/**
	 * Arc Furnace: a bolt that is also a fire. The target burns, and from it a spark leaps on to the next enemy, and
	 * the next, each one set alight: a chain that the storm builds one link a half second at a time.
	 */
	@Pair(a = "fire", b = "shock", name = "Arc Furnace", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Fire and lightning in one bolt: 5 fire damage and 4 lightning on the target, which burns for 6 seconds. "
			+ "Then a spark leaps from it to the nearest enemy within 4 blocks it has not hit yet, three times, half a second "
			+ "apart: 2 lightning each, and each one alight for 3 seconds.")
	public static void fireShock(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		Vec3 hitAt = PairCast.mid(first);
		Vec3 top = hitAt.add(0, 7, 0);
		// Beat one: a white-yellow bolt comes down out of the cloud and the target catches.
		c.zigzag(PairCast.shift(0xF5F06B, 0xFFFFFF, 0.9F), top, hitAt, 0.5, 3);
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, hitAt, 0.8F, 1.3F);
		c.shock(first, 4 * c.power);
		c.burn(first, 5 * c.power);
		c.ignite(first, 6);
		c.shake(hitAt, 0.3F, 8);
		c.tint(hitAt, 6, 0xB8E6FF, 5);

		// Beats two to four: the spark walks from enemy to enemy, each leg a crackling zigzag and a new fire.
		Set<LivingEntity> struck = new HashSet<>();
		struck.add(first);
		LivingEntity[] last = {first};
		c.every(10, 4, frame -> {
			if (frame == 0) {
				c.column(PairCast.dust(0xFF7A1F, 1.0F), hitAt.add(0, -0.9, 0), 0.9, 1.8, 24);
				return;
			}
			if (!c.here(last[0])) {
				return;
			}
			LivingEntity next = nextSpark(c, last[0], struck);
			if (next == null) {
				return;
			}
			Vec3 from = PairCast.mid(last[0]);
			Vec3 to = PairCast.mid(next);
			struck.add(next);
			c.zigzag(PairCast.shift(0xF5F06B, 0xFF7A1F, 0.7F), from, to, 0.4, 4);
			c.sound(SoundEvents.FIRECHARGE_USE, to, 0.6F, 1.6F);
			c.shock(next, 2 * c.power);
			c.ignite(next, 3);
			c.particles(ParticleTypes.ELECTRIC_SPARK, to, 6, 0.3, 0.1);
			last[0] = next;
		});
	}

	/** The nearest enemy within 4 blocks of {@code from} that the spark has not struck yet, or null. */
	private static LivingEntity nextSpark(PairCast c, LivingEntity from, Set<LivingEntity> struck) {
		for (LivingEntity t : c.enemiesNear(PairCast.mid(from), 4 * c.radius)) {
			if (!struck.contains(t) && c.here(t)) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Ashen Gale: a gust that carries embers. Everything it hits is hurled out, and burns; while a target still burns,
	 * the gale keeps shoving it outward, and each shove costs it fire.
	 */
	@Pair(a = "fire", b = "push", name = "Ashen Gale", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A gust of embers hurls each enemy it hits (up to eight) away from the point and sets it alight for 4 seconds, with 4 fire "
			+ "damage. While one still burns, it is shoved further out each second, three times, and each shove costs it 1 fire damage.")
	public static void firePush(PairCast c) {
		Vec3 at = c.point();
		Vec3 floor = c.ground(at);
		List<LivingEntity> caught = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : caught) {
			c.knockFrom(t, at, 1.4, 0.5);
			c.burn(t, 4 * c.power);
			c.ignite(t, 4);
		}
		c.sound(SoundEvents.WIND_CHARGE_BURST, at, 1.0F, 0.8F);
		c.sound(SoundEvents.BLAZE_SHOOT, at, 0.6F, 1.4F);
		c.shake(at, 0.2F, 6);

		// Each second: the embers rise in a spiral, and a ring of heat rolls out and shoves whatever still burns.
		c.every(20, 4, frame -> {
			if (frame == 0) {
				c.spiral(PairCast.shift(0xFF7A1F, 0xFFF4D6, 0.9F), floor, 1.3, 2.4, 2, 36);
				c.wave(PairCast.shift(0xFFB347, 0xFFF4D6, 0.9F), at.add(0, 0.3, 0), 24, 0.3);
				return;
			}
			for (LivingEntity t : c.still(caught)) {
				if (t.isOnFire()) {
					c.knockFrom(t, at, 0.7, 0.1);
					c.burn(t, 1 * c.power);
					c.particles(ParticleTypes.FLAME, PairCast.mid(t), 4, 0.2, 0.02);
				}
			}
			c.wave(PairCast.shift(0xFFB347, 0xFFF4D6, 0.8F), at.add(0, 0.3, 0), 20, 0.25 + frame * 0.05);
			c.sound(SoundEvents.FIRECHARGE_USE, at, 0.5F, 0.9F + frame * 0.1F);
		});
	}

	/**
	 * Phoenix Balm: the heal, warmed. Allies are mended at once and fire-proofed, and a shield of cinders holds what
	 * they cannot use; then the warmth keeps mending them, a little each second.
	 */
	@Pair(a = "fire", b = "heal", name = "Phoenix Balm", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Heals each ally within 4 blocks (up to eight) 8 health and gives Fire Resistance for 8 seconds; what they can't use becomes a shield "
			+ "of 4 health for 10 seconds. Then for 3 seconds the warmth mends them 1 more health a second.")
	public static void fireHeal(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> mended = PairCast.first(c.alliesNear(at, 4 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity t : mended) {
			c.heal(t, 8 * c.power);
			c.effect(t, MobEffects.FIRE_RESISTANCE, 8, 0);
			c.absorb(t, 4 * c.power, 10);
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.8F, 1.5F);
		c.tint(at, 5, 0xFFD86B, 10);

		// A gold ring opens over the balm, then every second a warm spiral climbs each ally while they knit.
		c.every(20, 4, frame -> {
			if (frame == 0) {
				c.ring(PairCast.dust(0xFFF4D6, 1.0F), at, 1.6 * c.radius, 30, 0);
				for (LivingEntity t : mended) {
					c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 6, 0.4, 0.05);
				}
				return;
			}
			for (LivingEntity t : c.still(mended)) {
				c.heal(t, 1 * c.power);
				c.spiral(PairCast.shift(0xFFD86B, 0xFF8A3D, 0.7F), t.position(), 0.6, 1.9, 1, 16);
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.5F, 1.0F + frame * 0.15F);
		});
	}

	/**
	 * Blighted Kiln: rot that catches fire. The target burns and rots; two seconds on, if it still does, the rot and
	 * the flame leap to the enemies nearest it, three at most.
	 */
	@Pair(a = "fire", b = "venom", name = "Blighted Kiln", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "4 fire damage and alight for 4 seconds, with Poison I for 4 seconds. Two seconds later, if the target still burns "
			+ "and is poisoned, the rot and the flame pass to up to three enemies within 2.5 blocks of it: each takes 3 fire "
			+ "damage, Poison I and alight for 4 seconds.")
	public static void fireVenom(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		Vec3 feet = first.position();
		c.burn(first, 4 * c.power);
		c.ignite(first, 4);
		c.effect(first, MobEffects.POISON, 4, 0);
		c.sound(SoundEvents.SLIME_BLOCK_PLACE, PairCast.mid(first), 0.9F, 0.6F);
		c.sound(SoundEvents.FIRECHARGE_USE, PairCast.mid(first), 0.6F, 0.9F);

		// A green and orange helix winds up the target while the rot takes hold, then drips off it.
		c.every(8, 5, frame -> {
			if (frame == 0) {
				c.helix(PairCast.dust(0x7BD15E, 0.9F), PairCast.dust(0xFF7A1F, 0.9F), feet, 0.7, 1.8, 2, 28);
				return;
			}
			if (c.here(first)) {
				c.particles(PairCast.dust(0x7BD15E, 0.8F), PairCast.mid(first), 4, 0.3, 0.02);
			}
		});
		c.later(40, () -> spreadRot(c, first));
	}

	private static void spreadRot(PairCast c, LivingEntity from) {
		if (!c.here(from) || !from.isOnFire() || !from.hasEffect(MobEffects.POISON)) {
			return;
		}
		Vec3 at = PairCast.mid(from);
		List<LivingEntity> carried = new ArrayList<>();
		for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
			if (near != from && carried.size() < 3) {
				carried.add(near);
			}
		}
		c.sound(SoundEvents.BLAZE_SHOOT, at, 0.7F, 0.8F);
		for (LivingEntity t : carried) {
			c.line(PairCast.shift(0x7BD15E, 0xFF7A1F, 0.8F), at, PairCast.mid(t), 4);
			c.burn(t, 3 * c.power);
			c.effect(t, MobEffects.POISON, 4, 0);
			c.ignite(t, 4);
		}
	}

	/**
	 * Searing Wound: the cut is cauterised as it opens. Three fire at once, then a burn every half second, which hits
	 * half as much again while the target is moving: the burn follows the blood's wound round the target.
	 */
	@Pair(a = "bleed", b = "fire", name = "Searing Wound", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Fire seals the wound as it opens: 3 fire damage and alight for 2 seconds, then 1 more fire damage every half second "
			+ "for 4 seconds (half as much again while the target moves).")
	public static void bleedFire(PairCast c) {
		List<LivingEntity> wounded = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : wounded) {
			c.burn(t, 3 * c.power);
			c.ignite(t, 2);
		}
		c.shake(c.point(), 0.12F, 5);
		c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, c.point(), 0.8F, 0.7F);

		// Crimson beads leave the wound each half second, and each one is a burn that ticks harder while the target runs.
		c.every(10, 9, frame -> {
			for (LivingEntity t : c.still(wounded)) {
				Vec3 at = PairCast.mid(t);
				if (frame == 0) {
					c.particles(PairCast.dust(0x8B0000, 1.2F), at, 12, 0.3, 0.05);
					c.line(PairCast.shift(0x8B0000, 0xFF6A00, 0.9F), at.add(-0.4, 0.4, 0), at.add(0.4, -0.2, 0), 4);
					continue;
				}
				boolean moving = t.getDeltaMovement().horizontalDistanceSqr() > 0.01;
				c.burn(t, (moving ? 1.5 : 1.0) * c.power);
				c.particles(PairCast.dust(0x8B0000, 0.9F), at, 3, 0.2, 0.04);
				c.particles(ParticleTypes.FLAME, at, 2, 0.2, 0.01);
			}
			if (frame > 0) {
				c.sound(SoundEvents.GENERIC_BURN, c.point(), 0.25F, 1.1F);
			}
		});
	}

	/**
	 * Ember Step: you leave a scorched ring behind and land on a flare. The departure burns what stood near you; the
	 * landing flares, a second later, over what stands near where you came down.
	 */
	@Pair(a = "blink", b = "fire", name = "Ember Step", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration", "radius"},
		text = "Teleports you to where the spell landed (max 40 blocks, if it is safe to stand there). The ground you leave burns: "
			+ "each enemy within 2.5 blocks of where you stood takes 4 fire damage and burns for 4 seconds, and a second later each "
			+ "enemy within 2 blocks of where you land takes 2 more fire damage.")
	public static void blinkFire(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 to = c.point();
		if (to.distanceTo(from) > 40) {
			to = from.add(to.subtract(from).normalize().scale(40));
		}
		List<LivingEntity> scorched = c.enemiesNear(from, 2.5 * c.radius);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.7F, 1.0F);

		// A violet ring collapses where you stood, shrinking to nothing as the orange burn comes up through it.
		c.every(2, 5, frame -> {
			double r = 2.2 - frame * 0.45;
			c.ring(PairCast.shift(0x3A1B6B, 0xFF7A1F, 0.9F), from.add(0, 0.2, 0), r * c.radius, 22, frame * 0.4);
		});
		if (!c.blink(self, to)) {
			return;
		}
		for (LivingEntity t : scorched) {
			c.burn(t, 4 * c.power);
			c.ignite(t, 4);
		}
		c.column(PairCast.dust(0xFF7A1F, 1.0F), from, 1.0, 1.2, 20);

		// A second on, the landing flares: a shockwave of fire that scorches what stands close to where you came down.
		c.later(20, () -> {
			Vec3 landed = self.position();
			c.wave(PairCast.shift(0xFF7A1F, 0xFFD86B, 1.0F), landed.add(0, 0.2, 0), 28, 0.35);
			c.sphere(PairCast.dust(0xFFB347, 0.9F), landed.add(0, 1, 0), 1.2 * c.radius, 22);
			c.sound(SoundEvents.FIRECHARGE_USE, landed, 0.9F, 0.8F);
			c.shake(landed, 0.2F, 5);
			for (LivingEntity t : c.enemiesNear(landed, 2 * c.radius)) {
				c.burn(t, 2 * c.power);
			}
		});
		c.punch(0.15F);
	}

	/**
	 * Hearthstone: the stone shell is fired hot. Allies get Resistance and Fire Resistance and a shell of absorption;
	 * the caster, who carries the stone, is slowed by its weight.
	 */
	@Pair(a = "fire", b = "stoneskin", name = "Hearthstone", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Allies within 4 blocks (up to eight) get Resistance II and Fire Resistance for 10 seconds, and 4 health of absorption for 6 seconds. "
			+ "The caster is slowed (Slowness I) for 10 seconds: stone is heavy.")
	public static void fireStoneskin(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> shelled = PairCast.first(c.alliesNear(at, 4 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity t : shelled) {
			c.effect(t, MobEffects.RESISTANCE, 10, 1);
			c.effect(t, MobEffects.FIRE_RESISTANCE, 10, 0);
			c.absorb(t, 4 * c.power, 6);
		}
		c.effect(c.caster, MobEffects.SLOWNESS, 10, 0);
		c.sound(SoundEvents.STONE_PLACE, at, 1.0F, 0.7F);
		c.shake(at, 0.15F, 5);

		// Stone dust rises in columns round each ally, then a ring of fire rolls round them and fades into the stone.
		for (LivingEntity t : shelled) {
			c.column(PairCast.dust(0xB8B2A7, 1.0F), t.position(), 1.0, 2.4, 24);
		}
		c.every(8, 4, frame -> {
			for (LivingEntity t : c.still(shelled)) {
				Vec3 feet = t.position();
				c.ring(PairCast.shift(0xFF7A1F, 0xB8B2A7, 0.8F), feet.add(0, 1.0 + frame * 0.35, 0), 1.1 - frame * 0.15, 18, frame * 0.5);
			}
			if (frame == 1) {
				c.sound(SoundEvents.FIRECHARGE_USE, at, 0.5F, 0.5F);
			}
		});
	}

	/**
	 * Cinderblow: a cutting wind carries a lit ember. The cut is struck, and the flame it carries is flung on to the
	 * nearest enemy beside the target a second later.
	 */
	@Pair(a = "ember", b = "windcut", name = "Cinderblow", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A cutting wind carries an ember: 4 damage and a light shove, and the target alight for 3 seconds. A second later the "
			+ "wind flings the flame to the nearest enemy within 3 blocks of it: 3 fire damage and alight for 3 seconds.")
	public static void emberWindcut(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		Vec3 at = PairCast.mid(first);
		c.strike(first, 4 * c.power);
		c.knockFrom(first, c.origin(), 0.6, 0.1);
		c.ignite(first, 3);

		// The cut is a bright slash across the target, and the ember a streak of orange that rides the wind.
		Vec3 across = c.dir().cross(new Vec3(0, 1, 0));
		if (across.lengthSqr() > 1.0E-4) {
			Vec3 side = across.normalize().scale(1.2);
			c.line(PairCast.dust(0xDDF7FF, 0.8F), at.add(side), at.subtract(side), 4);
		}
		c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.8F, 1.4F);
		c.punch(0.1F);

		c.later(20, () -> {
			LivingEntity next = c.nearestEnemy(at, 3 * c.radius, first);
			if (next == null) {
				return;
			}
			Vec3 to = PairCast.mid(next);
			c.line(PairCast.shift(0xDDF7FF, 0xFF8A2A, 0.8F), at, to, 3);
			c.spiral(PairCast.shift(0xFF8A2A, 0xFFF4D6, 0.6F), to, 0.5, 0.9, 2, 14);
			c.burn(next, 3 * c.power);
			c.ignite(next, 3);
			c.sound(SoundEvents.FIRECHARGE_USE, to, 0.7F, 1.2F);
		});
	}

	/**
	 * Steamburst: heat flashes over frost and the frost turns to steam. The flash scalds everything near the point and
	 * slows it; the steam lingers two seconds more, cracking what it stays on, and thaws whoever stands in it.
	 */
	@Pair(a = "flashfire", b = "frost", name = "Steamburst", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Heat flashes over the frost: each enemy within 3 blocks (up to eight) takes 4 fire damage and Slowness II for 4 seconds. The steam "
			+ "lingers 2 seconds: each second, enemies still within 2 blocks of the point take 1 more fire damage and are marked "
			+ "Cracked. Allies in it are thawed.")
	public static void flashfireFrost(PairCast c) {
		Vec3 at = c.point();
		Vec3 floor = c.ground(at);
		List<LivingEntity> scalded = PairCast.first(c.enemiesNear(at, 3 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity t : scalded) {
			c.burn(t, 4 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
		}
		for (LivingEntity ally : c.alliesNear(at, 3 * c.radius)) {
			ally.setTicksFrozen(0);
		}
		c.sphere(PairCast.shift(0xE8F8FF, 0xFFB26B, 1.0F), at, 3 * c.radius, 40);
		c.sound(SoundEvents.FIRE_EXTINGUISH, at, 1.0F, 0.9F);
		c.sound(SoundEvents.GLASS_BREAK, at, 0.5F, 1.6F);
		c.shake(at, 0.25F, 6);

		// The steam hangs on the ground: a grey-white disc that thickens for two seconds and cracks what it touches.
		c.every(20, 3, frame -> {
			if (frame == 0) {
				c.particles(ParticleTypes.CLOUD, at, 30, 1.2, 0.02);
				return;
			}
			c.disc(PairCast.dust(0xDDEFFF, 1.4F), floor, 2 * c.radius, 40);
			for (LivingEntity t : c.still(scalded)) {
				if (PairCast.mid(t).distanceTo(at) <= 2 * c.radius) {
					c.burn(t, 1 * c.power);
					c.mark(t, Reactions.Mark.CRACKED);
				}
			}
			c.sound(SoundEvents.FIRE_EXTINGUISH, at, 0.4F, 1.2F);
		});
	}

	/**
	 * Ashspin: a whirlwind of sparks. Everything it catches is spun round the point for two seconds, burning, and then
	 * flung out on fire.
	 */
	@Pair(a = "cyclone", b = "kindling", name = "Ashspin", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A whirlwind of sparks spins each enemy within 3 blocks (up to eight) round the point for 2 seconds, each one burning: 1 fire damage "
			+ "every half second. Then it flings them all out, with 3 fire damage and alight for 3 seconds.")
	public static void cycloneKindling(PairCast c) {
		Vec3 center = c.point();
		List<LivingEntity> spun = PairCast.first(c.enemiesNear(center, 3 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.WIND_CHARGE_BURST, center, 0.7F, 1.1F);

		// Four spins at half-second beats: a pale ring tightens round the enemies and sparks climb the funnel.
		c.every(10, 5, frame -> {
			if (frame < 4) {
				c.column(PairCast.shift(0xFF7A1F, 0xFFF4D6, 0.9F), center, 2.2 * c.radius, 2.6, 20);
				for (LivingEntity t : c.still(spun)) {
					Vec3 rel = PairCast.mid(t).subtract(center);
					Vec3 side = new Vec3(-rel.z, 0, rel.x);
					if (side.lengthSqr() > 1.0E-4) {
						c.push(t, side.normalize().scale(0.45).add(0, 0.12, 0));
					}
					c.burn(t, 1 * c.power);
				}
				c.ring(PairCast.dust(0xFFB347, 1.0F), center.add(0, 0.2, 0), (3 - frame * 0.4) * c.radius, 24, frame * 0.6);
				c.sound(SoundEvents.BLAZE_SHOOT, center, 0.35F, 0.8F + frame * 0.1F);
				return;
			}
			// The fling: a burst of sparks outward, and each enemy goes out alight.
			for (LivingEntity t : c.still(spun)) {
				c.knockFrom(t, center, 1.2, 0.6);
				c.burn(t, 3 * c.power);
				c.ignite(t, 3);
			}
			c.wave(PairCast.shift(0xFFB347, 0xFF4A00, 1.0F), center.add(0, 0.5, 0), 28, 0.4);
			c.particles(ParticleTypes.FLAME, center, 20, 1.0, 0.08);
			c.sound(SoundEvents.FIRECHARGE_USE, center, 0.9F, 0.7F);
			c.shake(center, 0.3F, 7);
		});
	}
}
