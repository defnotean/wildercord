package dev.wildercord.pairs.b039;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs039 {
	private Pairs039() {}

	/**
	 * Dawnfall: a sun-mote sinks onto each struck enemy, then a column of light falls a second later. The light pools
	 * where it lands and strips Absorption from whoever stands in it.
	 */
	@Pair(a = "light", b = "smite", name = "Dawnfall", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Marks up to 6 enemies with a sun-mote: they glow for 2 seconds and lose their Absorption. A second later a "
			+ "column of light falls on each: 8 magic damage, and 3 to each other enemy within 2 blocks. The light pools "
			+ "there for 4 seconds, stripping Absorption from enemies within 2 blocks of it.")
	public static void dawnfall(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : struck) {
			c.effect(t, MobEffects.GLOWING, 2, 0);
			t.setAbsorptionAmount(0.0F);
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, c.point(), 0.6F, 1.8F);
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 high = PairCast.mid(t).add(0, 3.5 - frame * 0.5, 0);
				c.ring(PairCast.shift(0xFFF8D0, 0xFFD060, 0.9F), high, 0.6 + frame * 0.1, 10, frame * 0.4);
				c.particles(ParticleTypes.END_ROD, high, 2, 0.1, 0.0);
			}
		});
		c.later(20, () -> {
			List<Vec3> spots = new ArrayList<>();
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				spots.add(t.position());
				c.line(PairCast.shift(0xFFFFFF, 0xFFD060, 1.2F), at.add(0, 4, 0), at, 3);
				c.hurt(t, 8 * c.power);
				for (LivingEntity e : c.enemiesNear(at, 2)) {
					if (e != t) {
						c.hurt(e, 3 * c.power);
					}
				}
			}
			c.sound(SoundEvents.BEACON_ACTIVATE, c.point(), 0.9F, 1.4F);
			c.shake(c.point(), 0.2F, 10);
			c.every(10, 9, beat -> {
				for (Vec3 spot : spots) {
					c.disc(PairCast.dust(0xFFE38A, 1.0F), spot.add(0, 0.1, 0), 1.6, 10);
					for (LivingEntity e : c.enemiesNear(spot, 2)) {
						e.setAbsorptionAmount(0.0F);
					}
				}
			});
		});
	}

	/**
	 * Almsgiving: a gift of healing that is passed on. It lands on the first ally, then hops to the nearest ally the
	 * gift has not reached, half as much each time.
	 */
	@Pair(a = "manabraid", b = "managift", name = "Almsgiving", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Heals the first ally it reaches for 6. A second later the gift passes to the nearest other ally within 5 "
			+ "blocks for 3, and a second after that to the next for 2. Three allies at most, each once.")
	public static void almsgiving(PairCast c) {
		List<LivingEntity> gifted = new ArrayList<>();
		c.every(20, 3, link -> {
			LivingEntity to = null;
			if (gifted.isEmpty()) {
				to = c.firstAlly();
			} else {
				Vec3 from = PairCast.mid(gifted.get(gifted.size() - 1));
				for (LivingEntity a : c.alliesNear(from, 5)) {
					if (!gifted.contains(a) && c.here(a)) {
						to = a;
						break;
					}
				}
			}
			if (to == null) {
				return;
			}
			if (!gifted.isEmpty()) {
				c.arc(PairCast.shift(0xFFF2A8, 0x9BE8B0, 0.9F), PairCast.mid(gifted.get(gifted.size() - 1)),
					PairCast.mid(to), 1.2, 12);
			}
			gifted.add(to);
			c.heal(to, 6.0 * c.power / (link + 1));
			c.particles(ParticleTypes.HEART, PairCast.mid(to).add(0, 0.5, 0), 3, 0.3, 0.0);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(to), 0.8F, 1.0F + 0.25F * link);
		});
	}

	/**
	 * Tallysigil: a glowing sigil is drawn on each struck enemy, tightening for three seconds, then it bursts. Every
	 * burst sends a spark of light home to the caster, who is healed for each.
	 */
	@Pair(a = "orbcall", b = "spellbrand", name = "Tallysigil", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Marks up to 6 enemies with a glowing sigil for 3 seconds, then it bursts: 6 magic damage to each, and 1 "
			+ "healing for you per sigil that bursts, as its light flies home to you.")
	public static void tallysigil(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : marked) {
			c.effect(t, MobEffects.GLOWING, 3, 0);
		}
		c.sound(SoundEvents.NOTE_BLOCK_BELL, c.point(), 0.7F, 1.4F);
		c.every(15, 5, frame -> {
			if (frame == 4) {
				c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, c.caster.position(), 0.8F, 0.8F);
			}
			for (LivingEntity t : c.still(marked)) {
				Vec3 head = PairCast.mid(t).add(0, 0.9, 0);
				if (frame < 4) {
					c.ring(PairCast.shift(0xFFE070, 0x7CFFB0, 0.8F), head, 0.9 - frame * 0.15, 12, frame * 0.5);
				} else {
					c.sphere(PairCast.dust(0xB8FFC8, 0.9F), head, 1.2, 24);
					c.hurt(t, 6 * c.power);
					Vec3 chest = c.caster.position().add(0, 1.0, 0);
					c.line(ParticleTypes.END_ROD, head, chest, 2);
					c.heal(c.caster, 1.0 * c.power);
				}
			}
		});
	}

	/**
	 * Hallowbloom: a shelter of leaves that opens over a second and then holds for five. Each second it heals the
	 * allies inside and shoves the enemies inside away from its centre.
	 */
	@Pair(a = "haven", b = "sanctuary", name = "Hallowbloom", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Opens a 4-block bloom that lasts 5 seconds. Each second, allies inside heal 2 (10 in all), and enemies "
			+ "inside are shoved away from its centre. It opens with a shockwave of leaves.")
	public static void hallowbloom(PairCast c) {
		Vec3 at = c.point();
		c.wave(PairCast.dust(0xB6F59A, 1.0F), at, 24, 0.35);
		c.sound(SoundEvents.PINK_PETALS_PLACE, at, 1.0F, 1.2F);
		c.every(5, 5, frame -> {
			double r = 1.0 + frame * 0.75;
			c.ring(PairCast.shift(0xE8FFB0, 0x7FD9A0, 0.9F), at.add(0, 0.2, 0), r, 16 + frame * 4, frame * 0.3);
		});
		c.every(20, 5, second -> {
			for (LivingEntity a : c.alliesNear(at, 4)) {
				c.heal(a, 2 * c.power);
			}
			for (LivingEntity e : c.enemiesNear(at, 4)) {
				c.knockFrom(e, at, 0.7, 0.2);
			}
			c.sphere(PairCast.dust(0xB6F59A, 0.8F), at.add(0, 1, 0), 4, 30);
			c.sound(SoundEvents.AZALEA_LEAVES_STEP, at, 0.8F, 1.0F);
		});
	}

	/**
	 * Sunrise Vanguard: a beacon of light on the allies round the point. Strength and Speed at once, then two
	 * rallying beats, each giving Absorption and a ring of light round them.
	 */
	@Pair(a = "rally", b = "rally_light", name = "Sunrise Vanguard", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Allies within 8 blocks get Strength I and Speed I for 8 seconds. At 0, 2 and 4 seconds they each gain "
			+ "2 Absorption for 4 seconds, with a ring of light round them and a flute note.")
	public static void sunriseVanguard(PairCast c) {
		Vec3 at = c.point();
		c.column(PairCast.dust(0xFFE9A0, 1.2F), at, 0.8, 3.5, 30);
		c.sound(SoundEvents.BEACON_ACTIVATE, at, 0.7F, 1.2F);
		c.every(40, 3, beat -> {
			for (LivingEntity a : c.alliesNear(at, 8)) {
				if (beat == 0) {
					c.effect(a, MobEffects.STRENGTH, 8, 0);
					c.effect(a, MobEffects.SPEED, 8, 0);
				}
				c.absorb(a, 2 * c.power, 4);
				c.ring(PairCast.shift(0xFFE9A0, 0xFF9E5E, 0.9F), PairCast.mid(a).add(0, -0.8, 0), 0.9, 14, beat * 0.4);
			}
			c.wave(PairCast.dust(0xFFD27A, 1.0F), at, 20, 0.3);
			c.sound(SoundEvents.NOTE_BLOCK_FLUTE, at, 0.6F, 0.9F + 0.2F * beat);
		});
	}

	/**
	 * Lodestone Knot: what it hits is slowed. The spot becomes a lodestone that draws enemies in for three seconds,
	 * then shocks those gathered tight round it.
	 */
	@Pair(a = "lodestar", b = "magnetize", name = "Lodestone Knot", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Slows up to 6 enemies it hits (Slowness I, 3 seconds). The spot is a lodestone for 3 seconds: enemies "
			+ "within 6 blocks are drawn in every half second. Then each within 1.5 blocks takes 4 shock damage.")
	public static void lodestoneKnot(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : struck) {
			c.effect(t, MobEffects.SLOWNESS, 3, 0);
		}
		Vec3 spot = c.point();
		c.sound(SoundEvents.LODESTONE_COMPASS_LOCK, spot, 0.8F, 1.2F);
		c.every(10, 6, beat -> {
			for (LivingEntity e : c.enemiesNear(spot, 6)) {
				c.pullTo(e, spot, 0.5);
			}
			c.sphere(PairCast.shift(0xC9B8FF, 0x6AA8FF, 0.9F), spot.add(0, 1, 0), 6 - beat, 20);
		});
		c.later(60, () -> {
			for (LivingEntity e : c.enemiesNear(spot, 1.5)) {
				c.shock(e, 4 * c.power);
			}
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, spot.add(0, 0.2, 0), spot.add(0, 3, 0), 0.3, 3);
			c.sound(SoundEvents.TRIDENT_THUNDER, spot, 0.6F, 1.4F);
			c.tint(spot, 8, 0x9AD0FF, 6);
		});
	}

	/**
	 * Vigil Toll: every enemy near where it lands glows for ten seconds, and a bell tolls over them five times. Each
	 * toll hurts whoever glows.
	 */
	@Pair(a = "nightwatch", b = "sentry", name = "Vigil Toll", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 16 enemies within 16 blocks of where it lands glow for 10 seconds. A bell tolls every 2 seconds for 8 "
			+ "seconds (5 tolls), and each toll deals 1 magic damage to each enemy it marked.")
	public static void vigilToll(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> watched = c.enemiesNear(at, 16);
		for (LivingEntity e : watched) {
			c.effect(e, MobEffects.GLOWING, 10, 0);
		}
		c.every(40, 5, toll -> {
			c.sound(SoundEvents.BELL_BLOCK, at, 1.0F, 0.9F);
			c.wave(PairCast.dust(0xFFD27A, 1.0F), at, 28, 0.4);
			for (LivingEntity e : c.still(watched)) {
				c.hurt(e, 1 * c.power);
				c.particles(PairCast.dust(0xFFE7A0, 0.9F), PairCast.mid(e).add(0, 1, 0), 3, 0.3, 0.0);
			}
		});
	}

	/**
	 * Nightsong: night sight for the caster, then three echoes a second apart ring out from the point. Each echo hurts
	 * and dazes whatever stands within six blocks of it.
	 */
	@Pair(a = "echolocate", b = "night_eye", name = "Nightsong", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Gives you Night Vision for 60 seconds. Three echoes ring out from the point a second apart: each deals 2 "
			+ "magic damage to enemies within 6 blocks and dazes them (Slowness I, 2 seconds).")
	public static void nightsong(PairCast c) {
		Vec3 at = c.point();
		c.effect(c.caster, MobEffects.NIGHT_VISION, 60, 0);
		c.every(20, 3, echo -> {
			c.wave(PairCast.shift(0x9B7BFF, 0x30E0FF, 0.8F), at.add(0, 0.5, 0), 28, 0.3 + 0.15 * echo);
			c.sound(SoundEvents.SCULK_CLICKING, at, 1.0F, 0.8F + 0.3F * echo);
			for (LivingEntity e : c.still(c.enemiesNear(at, 6))) {
				c.hurt(e, 2 * c.power);
				c.effect(e, MobEffects.SLOWNESS, 2, 0);
			}
		});
	}

	/**
	 * Cocoonfall: silk threads the struck enemies up into a cocoon that holds them in the air for two seconds, then
	 * drops them. The drop slams each and whoever is near it.
	 */
	@Pair(a = "levitate", b = "silklift", name = "Cocoonfall", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Hoists up to 6 enemies it hits in silk: they hang for 2 seconds (Levitation I; a boss gets Slowness II instead). Then they drop, each taking "
			+ "5 damage, and enemies within 2 blocks of one take 2 damage.")
	public static void cocoonfall(PairCast c) {
		List<LivingEntity> snared = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : snared) {
			if (!c.movable(t)) {
				c.effect(t, MobEffects.SLOWNESS, 2, 1);
				continue;
			}
			c.effect(t, MobEffects.LEVITATION, 2, 0);
		}
		c.sound(SoundEvents.LEAD_TIED, c.point(), 0.8F, 1.3F);
		c.every(10, 5, beat -> {
			for (LivingEntity t : c.still(snared)) {
				Vec3 at = PairCast.mid(t);
				if (beat == 0) {
					c.line(PairCast.dust(0xF4F4F4, 0.6F), c.origin(), at, 3);
				}
				c.sphere(PairCast.dust(0xEDE3C8, 0.8F), at, 1.1 - beat * 0.15, 14);
				if (beat == 4) {
					c.strike(t, 5 * c.power);
					c.push(t, new Vec3(0, -1.4, 0));
					for (LivingEntity e : c.enemiesNear(at, 2)) {
						if (e != t) {
							c.strike(e, 2 * c.power);
						}
					}
				}
			}
			if (beat == 4) {
				c.sound(SoundEvents.WIND_CHARGE_BURST, c.point(), 0.7F, 0.9F);
				c.shake(c.point(), 0.25F, 8);
			}
		});
	}

	/**
	 * Starmap: the struck enemies are joined into a constellation, one a half second. Each star ignites in turn, lit
	 * from the one before, and the last bursts into rays.
	 */
	@Pair(a = "starchart", b = "starfire", name = "Starmap", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Joins up to 5 enemies it hits into a constellation: each, half a second apart, takes 2 fire damage and "
			+ "burns for 3 seconds, lit by a line from the star before it. The last star bursts into rays.")
	public static void starmap(PairCast c) {
		List<LivingEntity> stars = PairCast.first(c.enemies(), 5);
		c.every(10, stars.size(), link -> {
			LivingEntity t = stars.get(link);
			if (!c.here(t)) {
				return;
			}
			Vec3 at = PairCast.mid(t);
			if (link > 0 && c.here(stars.get(link - 1))) {
				c.line(PairCast.shift(0xFFE7A0, 0x9AD8FF, 0.8F), PairCast.mid(stars.get(link - 1)), at, 2);
			}
			c.burn(t, 2 * c.power);
			c.ignite(t, 3);
			boolean last = link == stars.size() - 1;
			c.star(ParticleTypes.END_ROD, at, last ? 8 : 6, last ? 1.5 : 0.8, link * 0.2);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.7F, 1.0F + 0.15F * link);
			if (last) {
				c.sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, at, 0.9F, 1.1F);
			}
		});
	}
}
