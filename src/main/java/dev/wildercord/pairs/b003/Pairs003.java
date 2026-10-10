package dev.wildercord.pairs.b003;

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

/** Batch 3: hand-made fusions of the chill, push, heal, bleed, pull, stoneskin, icicle, lightning, frost, tremor, flash freeze, launch, coldsnap, venom, undertow and vinelash runes. */
public final class Pairs003 {
	private Pairs003() {}

	/**
	 * Rime Gale: a cold wind throws the enemies away from the spell. The ones already slowed are thrown twice as hard, and
	 * they slam into whatever they meet. The look: pale rings racing out, then a steel-blue lane to each target.
	 */
	@Pair(a = "chill", b = "push", name = "Rime Gale", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A gale hurls each enemy away from the spell, with Slowness II for 6 seconds and 1 freeze damage. "
			+ "An enemy already slowed flies twice as hard, and slams into the enemy it meets within 1.5 blocks: both take 4 damage.")
	public static void chillPush(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 at = c.point();
		Set<LivingEntity> hard = new HashSet<>();
		Set<LivingEntity> slammed = new HashSet<>();
		for (LivingEntity t : hit) {
			boolean slowed = t.hasEffect(MobEffects.SLOWNESS);
			if (slowed) {
				hard.add(t);
			}
			c.effect(t, MobEffects.SLOWNESS, 6, 1);
			c.freeze(t, 1 * c.power);
			c.knockFrom(t, at, slowed ? 2.2 : 1.1, 0.35);
		}
		c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.9F, 1.3F);
		c.every(2, 6, frame -> {
			// The gale: pale rings racing out from the spell and fading.
			c.ring(ParticleTypes.CLOUD, at, (0.6 + frame * 0.9) * c.radius, 18, frame * 0.4);
			if (frame == 0) {
				for (LivingEntity t : hit) {
					c.line(PairCast.shift(0xE8FBFF, 0x7FC8E8, 0.9F), at, PairCast.mid(t), 3);
				}
			}
			// The thrown ones that meet another enemy slam together.
			for (LivingEntity t : c.still(hard)) {
				if (slammed.contains(t)) {
					continue;
				}
				LivingEntity other = c.nearestEnemy(PairCast.mid(t), 1.5 * c.radius, t);
				if (other != null && !slammed.contains(other)) {
					slammed.add(t);
					slammed.add(other);
					Vec3 mid = PairCast.mid(t).lerp(PairCast.mid(other), 0.5);
					c.strike(t, 4 * c.power);
					c.strike(other, 4 * c.power);
					c.sphere(PairCast.dust(0xFFFFFF, 1.0F), mid, 0.9, 18);
					c.particles(ParticleTypes.SNOWFLAKE, mid, 12, 0.4, 0.1);
					c.sound(SoundEvents.ANVIL_LAND, mid, 0.5F, 1.8F);
					c.shake(mid, 0.25F, 6);
				}
			}
		});
		c.later(12, () -> c.particles(ParticleTypes.CLOUD, at, 10, 0.5, 0.05));
	}

	/**
	 * Tithe of Cold: the warmth a chilled enemy gives up goes to the allies. Heals on the second beat, after a thread of
	 * frost has run from the donor to each of them. The look: an ice thread turning gold, then green spirals rising.
	 */
	@Pair(a = "chill", b = "heal", name = "Tithe of Cold", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Heals each ally 8 health. The nearest enemy within 4 blocks is chilled (Slowness II for 6 seconds and 1 freeze damage), "
			+ "and if it was already slowed, each ally heals 10 instead.")
	public static void chillHeal(PairCast c) {
		Vec3 at = c.point();
		LivingEntity donor = c.nearestEnemy(at, 4 * c.radius, null);
		boolean cold = donor != null && donor.hasEffect(MobEffects.SLOWNESS);
		double amount = (cold ? 10 : 8) * c.power;
		if (donor != null) {
			c.effect(donor, MobEffects.SLOWNESS, 6, 1);
			c.freeze(donor, 1 * c.power);
		}
		List<LivingEntity> mend = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.9F, 1.4F);
		c.every(3, 5, frame -> {
			for (LivingEntity ally : c.still(mend)) {
				Vec3 heart = PairCast.mid(ally);
				if (frame == 0 && donor != null && c.here(donor)) {
					c.arc(PairCast.shift(0xBDE9FF, 0xF2C94C, 0.9F), PairCast.mid(donor), heart, 1.2, 14);
				}
				if (frame == 1) {
					c.spiral(PairCast.shift(0xF2C94C, 0x6BCB77, 1.0F), ally.position(), 0.7, 2.2, 1.5, 22);
				}
				if (frame == 2) {
					c.heal(ally, amount);
					c.ring(ParticleTypes.HEART, heart, 0.9, 10, 0);
					c.tint(heart, 1.5, 0xF2C94C, 8);
					c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, heart, 0.8F, 1.0F);
				}
				if (frame == 3) {
					c.column(PairCast.shift(0x6BCB77, 0xF2C94C, 0.8F), ally.position(), 0.8, 2.0, 16);
				}
			}
		});
	}

	/**
	 * Pressure Clot: frost clamps each wound, and the blood builds against it for three seconds. Then the clamp bursts
	 * outward, and the spray reaches the enemies beside it. The look: a crimson core in a tightening frost shell.
	 */
	@Pair(a = "bleed", b = "chill", name = "Pressure Clot", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Frost clamps each wound shut: Slowness II for 6 seconds and 1 freeze damage. The blood can't escape, and 3 seconds later "
			+ "the clamp bursts: 10 damage, and 3 damage to each other enemy within 2 blocks of it.")
	public static void bleedChill(PairCast c) {
		List<LivingEntity> wounded = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : wounded) {
			c.effect(t, MobEffects.SLOWNESS, 6, 1);
			c.freeze(t, 1 * c.power);
			c.sound(SoundEvents.SNOW_PLACE, PairCast.mid(t), 0.9F, 0.7F);
		}
		int hold = c.ticks(3);
		// The clamp tightens round the wound while the blood strains under it.
		c.every(5, Math.max(1, hold / 5), frame -> {
			for (LivingEntity t : c.still(wounded)) {
				Vec3 mid = PairCast.mid(t);
				c.sphere(PairCast.shift(0xB3121B, 0xD9F4FF, 0.9F), mid, Math.max(0.35, 1.3 - frame * 0.08), 20);
				c.particles(PairCast.dust(0x8E0F14, 1.0F), mid, 2, 0.3, 0.0);
			}
		});
		c.later(hold, () -> {
			for (LivingEntity t : c.still(wounded)) {
				Vec3 mid = PairCast.mid(t);
				c.strike(t, 10 * c.power);
				c.sphere(PairCast.shift(0xFF4D4D, 0xB3121B, 1.2F), mid, 1.5, 34);
				c.particles(ParticleTypes.DAMAGE_INDICATOR, mid, 8, 0.4, 0.2);
				c.sound(SoundEvents.SLIME_BLOCK_BREAK, mid, 1.0F, 0.7F);
				c.shake(mid, 0.2F, 6);
				for (LivingEntity near : c.enemiesNear(mid, 2 * c.radius)) {
					if (near != t) {
						c.hurt(near, 3 * c.power);
						c.line(PairCast.dust(0xFF4D4D, 0.8F), mid, PairCast.mid(near), 3);
					}
				}
			}
		});
	}

	/**
	 * Frozen Huddle: the pulled enemies are drawn in to the spell and chilled. Once they've gathered, the cold passes
	 * between them. The look: a void-purple implosion that turns to frost, then a flash of snow where they met.
	 */
	@Pair(a = "chill", b = "pull", name = "Frozen Huddle", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Draws each enemy in toward the spell and chills it (Slowness II for 6 seconds, 1 freeze damage). Once they gather, "
			+ "each takes 3 freeze damage for every other gathered enemy within 1.5 blocks of it, up to 3.")
	public static void chillPull(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> drawn = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : drawn) {
			c.effect(t, MobEffects.SLOWNESS, 6, 1);
			c.freeze(t, 1 * c.power);
		}
		c.sound(SoundEvents.BEACON_AMBIENT, at, 0.8F, 0.6F);
		c.every(3, 6, frame -> {
			for (LivingEntity t : c.still(drawn)) {
				if (frame < 4) {
					c.pullTo(t, at, 0.9);
				}
			}
			if (frame == 0) {
				for (LivingEntity t : c.still(drawn)) {
					c.line(PairCast.shift(0x7F5AF0, 0xBDF3FF, 0.8F), PairCast.mid(t), at, 2);
				}
			} else if (frame < 4) {
				c.ring(PairCast.shift(0x7F5AF0, 0xBDF3FF, 1.0F), at, (2.6 - frame * 0.6) * c.radius, 18, frame * 0.5);
			}
			if (frame == 4) {
				gather(c, drawn, at);
			}
		});
	}

	private static void gather(PairCast c, List<LivingEntity> drawn, Vec3 at) {
		double reach = 1.5 * c.radius;
		for (LivingEntity t : c.still(drawn)) {
			int others = 0;
			for (LivingEntity o : c.still(drawn)) {
				if (o != t && others < 3 && PairCast.mid(o).distanceTo(PairCast.mid(t)) <= reach) {
					others++;
					c.line(PairCast.dust(0xBDF3FF, 0.7F), PairCast.mid(t), PairCast.mid(o), 3);
				}
			}
			if (others > 0) {
				c.freeze(t, 3 * c.power * others);
			}
		}
		c.sphere(PairCast.dust(0xBDF3FF, 1.2F), at, 0.9 * c.radius, 26);
		c.particles(ParticleTypes.SNOWFLAKE, at, 20, 0.6, 0.05);
		c.shake(at, 0.2F, 5);
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 1.0F, 1.4F);
	}

	/**
	 * Rimed Bulwark: the allies near the spell are braced with Resistance II, and for five seconds every enemy that comes
	 * near them is chilled once a second. The look: stone plates rising, then frost rings pulsing out from each ally.
	 */
	@Pair(a = "chill", b = "stoneskin", name = "Rimed Bulwark", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Allies within 4 blocks of the spell get Resistance II for 10 seconds. For 5 seconds, once a second, every enemy within "
			+ "2 blocks of an ally is chilled: Slowness I for 2 seconds and 1 freeze damage.")
	public static void chillStoneskin(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> wards = PairCast.first(c.alliesNear(at, 4 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.STONE_PLACE, at, 0.9F, 0.8F);
		for (LivingEntity w : wards) {
			c.effect(w, MobEffects.RESISTANCE, 10, 1);
		}
		c.every(20, 5, frame -> {
			for (LivingEntity w : c.still(wards)) {
				Vec3 feet = w.position();
				if (frame == 0) {
					c.column(PairCast.dust(0x9C8F7A, 1.1F), feet, 1.0, 1.8, 16);
				}
				c.ring(PairCast.shift(0xCFEFFF, 0x9C8F7A, 0.9F), feet.add(0, 0.3, 0), 1.6 * c.radius, 20, frame * 0.3);
				for (LivingEntity e : c.enemiesNear(PairCast.mid(w), 2 * c.radius)) {
					c.effect(e, MobEffects.SLOWNESS, 2, 0);
					c.freeze(e, 1 * c.power);
					c.line(PairCast.dust(0xCFEFFF, 0.6F), PairCast.mid(w), PairCast.mid(e), 3);
				}
			}
		});
	}

	/**
	 * Glasswire Spear: an icicle impales each enemy and soaks it. Then lightning leaps from the spear from one soaked
	 * enemy to the next. The look: falling spears of ice, then a crackle of sparks zigzagging between them.
	 */
	@Pair(a = "icicle", b = "lightning", name = "Glasswire Spear", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "An icicle impales each enemy: 4 freeze damage, or 6 if it's already slowed, and it's soaked. 1.5 seconds later, "
			+ "lightning leaps from the spear to the nearest soaked enemy within 6 blocks, then on to the next: 6 damage each, up to 4 leaps.")
	public static void icicleLightning(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> impaled = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : impaled) {
			boolean slowed = t.hasEffect(MobEffects.SLOWNESS);
			c.freeze(t, (slowed ? 6 : 4) * c.power);
			c.mark(t, Reactions.Mark.SOAKED);
			Vec3 mid = PairCast.mid(t);
			c.line(PairCast.shift(0xE6FBFF, 0x7FDBFF, 1.0F), mid.add(0, 3.5, 0), mid, 4);
		}
		c.sound(SoundEvents.GLASS_PLACE, at, 0.9F, 1.6F);
		c.later(30, () -> {
			List<LivingEntity> order = leapOrder(c, at, c.still(impaled), 4);
			Vec3[] prev = {at};
			c.every(4, order.size(), frame -> {
				LivingEntity t = order.get(frame);
				if (!c.here(t)) {
					return;
				}
				Vec3 to = PairCast.mid(t);
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, prev[0], to, 0.35, 4);
				if (frame == 0) {
					c.bolt(to);
				}
				c.shock(t, 6 * c.power);
				c.sphere(PairCast.dust(0xE6FBFF, 0.6F), to, 0.5, 8);
				c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, to, 0.6F, 1.0F + frame * 0.1F);
				c.shake(to, 0.1F, 4);
				prev[0] = to;
			});
		});
	}

	/** The soaked enemies in the order the lightning leaps: always the nearest left within reach, up to {@code max}. */
	private static List<LivingEntity> leapOrder(PairCast c, Vec3 from, List<LivingEntity> pool, int max) {
		List<LivingEntity> left = new ArrayList<>(pool);
		List<LivingEntity> order = new ArrayList<>();
		Vec3 at = from;
		double reach = 6 * c.radius;
		while (!left.isEmpty() && order.size() < max) {
			LivingEntity best = null;
			double bestD = reach * reach;
			for (LivingEntity t : left) {
				double d = PairCast.mid(t).distanceToSqr(at);
				if (d <= bestD) {
					bestD = d;
					best = t;
				}
			}
			if (best == null) {
				break;
			}
			order.add(best);
			left.remove(best);
			at = PairCast.mid(best);
		}
		return order;
	}

	/**
	 * Hoar Spires: spires of frozen earth shoot up under each target, pinning it in cold, then shatter outward. The
	 * look: star cracks on the ground, columns that grow over three beats, and a glassy burst on the shatter.
	 */
	@Pair(a = "frost", b = "tremor", name = "Hoar Spires", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Spires of frozen earth shoot up under each target: 5 freeze and 6 damage, and Slowness III for 3 seconds. "
			+ "A second later they shatter outward: 3 damage to each other enemy within 2.5 blocks.")
	public static void frostTremor(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> speared = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : speared) {
			c.freeze(t, 5 * c.power);
			c.strike(t, 6 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 3, 2);
		}
		c.sound(SoundEvents.DEEPSLATE_BREAK, at, 1.0F, 0.6F);
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(speared)) {
				Vec3 feet = t.position();
				if (frame == 0) {
					c.star(PairCast.dust(0x8FD9F7, 0.8F), feet.add(0, 0.1, 0), 6, 1.8 * c.radius, 0.3);
				}
				c.column(PairCast.shift(0xBFF4FF, 0x6B5A46, 1.2F), feet, 0.5 * c.radius, 0.6 + frame * 0.6, 14);
			}
		});
		c.later(20, () -> {
			for (LivingEntity t : c.still(speared)) {
				Vec3 mid = PairCast.mid(t);
				Vec3 feet = t.position();
				c.sphere(PairCast.shift(0xDFF8FF, 0x6B5A46, 1.0F), mid, 1.6, 30);
				c.star(PairCast.dust(0xBFF4FF, 1.0F), feet.add(0, 0.1, 0), 8, 2.5 * c.radius, 0.2);
				c.sound(SoundEvents.GLASS_BREAK, mid, 1.0F, 1.1F);
				c.shake(mid, 0.3F, 7);
				for (LivingEntity near : c.enemiesNear(feet, 2.5 * c.radius)) {
					if (near != t) {
						c.hurt(near, 3 * c.power);
					}
				}
			}
		});
	}

	/**
	 * Hailfall: the enemies are flung into the air, freezing as they go, and the fall is what hurts. The look: a column
	 * of frost motes over the spell, snow trailing each target up and down, then a cloud shock where it lands.
	 */
	@Pair(a = "flash_freeze", b = "launch", name = "Hailfall", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Flings each enemy high into the air, freezing it on the way (4 freeze damage). Where it lands it takes 8 damage, "
			+ "and each other enemy within 2 blocks of its landing takes 3.")
	public static void flashLaunch(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> flung = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Set<LivingEntity> aloft = new HashSet<>();
		Set<LivingEntity> landed = new HashSet<>();
		c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, at, 1.0F, 1.5F);
		c.column(PairCast.shift(0xE8FBFF, 0x5C7A99, 1.0F), at.add(0, 2.5, 0), 2.4 * c.radius, 4.5, 26);
		for (LivingEntity t : flung) {
			c.freeze(t, 4 * c.power);
			c.lift(t, 1.0);
		}
		c.every(4, 8, frame -> {
			for (LivingEntity t : c.still(flung)) {
				if (landed.contains(t)) {
					continue;
				}
				Vec3 mid = PairCast.mid(t);
				if (!t.onGround()) {
					aloft.add(t);
					c.particles(ParticleTypes.SNOWFLAKE, mid, 2, 0.2, 0.02);
				} else if (aloft.contains(t)) {
					landed.add(t);
					Vec3 feet = t.position().add(0, 0.2, 0);
					c.strike(t, 8 * c.power);
					for (LivingEntity near : c.enemiesNear(feet, 2 * c.radius)) {
						if (near != t) {
							c.hurt(near, 3 * c.power);
						}
					}
					c.wave(ParticleTypes.CLOUD, feet, 18, 0.35);
					c.star(PairCast.dust(0xDCEFFF, 1.1F), feet.add(0, 0.1, 0), 8, 2.4 * c.radius, 0.2);
					c.sound(SoundEvents.GLASS_BREAK, feet, 0.9F, 0.9F);
					c.shake(feet, 0.35F, 8);
				}
			}
		});
	}

	/**
	 * Blackfrost: a cold snap ripples out and grips the enemies with poison in them. Two seconds later the ice cracks and
	 * the poison runs between the nearest of them. The look: a frost ring fading to venom green, drips falling onto
	 * each target, then dark-green cracks linking them.
	 */
	@Pair(a = "coldsnap", b = "venom", name = "Blackfrost", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A cold snap grips each enemy within 3 blocks: 3 freeze damage, Slowness II for 4 seconds, and 2 damage with Poison I "
			+ "for 4 seconds. Two seconds later the ice cracks: each poisoned enemy takes 4 damage, and 2 more for each of up to two "
			+ "other poisoned enemies within 2.5 blocks.")
	public static void coldVenom(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> snapped = PairCast.first(c.enemiesNear(at, 3 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.SNOW_PLACE, at, 1.0F, 0.6F);
		c.every(2, 6, frame -> c.ring(PairCast.shift(0xD6F3FF, 0x6CC24A, 1.0F), at,
			(0.8 + frame * 0.5) * c.radius, 22, frame * 0.2));
		for (LivingEntity t : snapped) {
			c.freeze(t, 3 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
			c.hurt(t, 2 * c.power);
			c.effect(t, MobEffects.POISON, 4, 0);
			Vec3 mid = PairCast.mid(t);
			c.line(PairCast.shift(0x6CC24A, 0xD6F3FF, 0.7F), mid.add(0, 2.5, 0), mid, 3);
		}
		c.later(40, () -> {
			List<LivingEntity> cracked = c.still(snapped);
			for (LivingEntity t : cracked) {
				Vec3 mid = PairCast.mid(t);
				int near = 0;
				for (LivingEntity o : cracked) {
					if (o != t && near < 2 && PairCast.mid(o).distanceTo(mid) <= 2.5 * c.radius) {
						near++;
						c.line(PairCast.dust(0x6CC24A, 0.8F), mid, PairCast.mid(o), 3);
					}
				}
				c.hurt(t, (4 + 2 * near) * c.power);
				c.sphere(PairCast.shift(0xD6F3FF, 0x6CC24A, 0.9F), mid, 1.0, 18);
				c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, mid, 10, 0.5, 0.05);
			}
			c.sound(SoundEvents.GLASS_BREAK, at, 0.9F, 1.5F);
		});
	}

	/**
	 * Tidewhip: a thorned vine lashes each enemy and yanks it in, then the undertow drags it down, soaked and slowed,
	 * for as long as the drag lasts. The look: a green lash line, a yank toward the caster, then blue swirls sinking
	 * beneath each target with bubbles.
	 */
	@Pair(a = "undertow", b = "vinelash", name = "Tidewhip", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A thorned vine lashes each enemy for 5 damage, yanks it toward you and trips it (Slowness II for 2 seconds). "
			+ "Then the undertow drags it under: soaked, Slowness III for 3 seconds, and 2 damage now and once a second after, three times in all.")
	public static void undertowVinelash(PairCast c) {
		Vec3 hand = c.origin();
		List<LivingEntity> lashed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		int beats = Math.max(1, c.ticks(3) / 20);
		c.sound(SoundEvents.GRASS_BREAK, hand, 1.0F, 0.6F);
		for (LivingEntity t : lashed) {
			c.strike(t, 5 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 2, 1);
			Vec3 toward = c.caster.position().subtract(t.position()).multiply(1, 0, 1);
			if (toward.lengthSqr() > 0.01) {
				c.push(t, toward.normalize().scale(1.1).add(0, 0.2, 0));
			}
			c.zigzag(ParticleTypes.HAPPY_VILLAGER, hand, PairCast.mid(t), 0.3, 3);
		}
		c.every(20, beats, frame -> {
			for (LivingEntity t : c.still(lashed)) {
				Vec3 mid = PairCast.mid(t);
				if (frame == 0) {
					c.mark(t, Reactions.Mark.SOAKED);
					c.effect(t, MobEffects.SLOWNESS, 3, 2);
					c.sound(SoundEvents.GENERIC_SPLASH, mid, 0.9F, 0.8F);
				}
				c.hurt(t, 2 * c.power);
				c.spiral(PairCast.shift(0x2B7FB8, 0x1B4F72, 1.0F), t.position(), 0.8, -1.4, 2, 16);
				c.particles(ParticleTypes.BUBBLE, mid, 6, 0.3, 0.02);
			}
		});
	}
}
