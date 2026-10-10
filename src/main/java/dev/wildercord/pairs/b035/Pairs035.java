package dev.wildercord.pairs.b035;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs of storm, weather, traps, sun and growth: each has its own mechanic, look and sounds. */
public final class Pairs035 {
	private Pairs035() {}

	/**
	 * Stormstair: a staircase of wind stones laid from you to the point, then you come down the last of it as a
	 * bolt. The look: pale stones light one after another along a line, then a lightning strike at the landing.
	 */
	@Pair(a = "thunder_walk", b = "thunderstep", name = "Stormstair", element = "storm", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Five wind stones are laid in a line from you to the point (16 blocks at most): each enemy within 1.5 blocks of a "
			+ "stone takes 2 lightning damage, once. Then you come down on the point as a bolt, if the ground there is safe: "
			+ "8 lightning damage to each enemy within 2.5 blocks, and Slowness III for 1 second.")
	public static void stormstair(PairCast c) {
		Vec3 from = c.caster.position();
		Vec3 offset = c.point().subtract(from);
		Vec3 to = offset.lengthSqr() < 1.0E-4 ? from : from.add(offset.normalize().scale(Math.min(16, offset.length())));
		Set<LivingEntity> stomped = new HashSet<>();
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, from, 0.4F, 1.9F);
		for (int i = 1; i <= 5; i++) {
			Vec3 stone = c.ground(from.lerp(to, i / 6.0)).add(0, 0.1, 0);
			for (LivingEntity t : c.enemiesNear(stone, 1.5 * c.radius)) {
				if (stomped.add(t)) {
					c.shock(t, 2 * c.power);
				}
			}
		}
		// The stair lights one stone at a time, five frames.
		c.every(3, 5, frame -> {
			Vec3 stone = c.ground(from.lerp(to, (frame + 1) / 6.0)).add(0, 0.1, 0);
			c.ring(PairCast.shift(0xFFF6B0, 0x5FB8FF, 0.8F), stone, 0.8, 10, frame * 0.5);
			c.line(PairCast.dust(0xC9EEFF, 0.6F), from.add(0, 0.2, 0), stone, 2);
		});
		// The payoff: the caster comes down on the point, and the bolt strikes what stands round it.
		c.later(16, () -> {
			Vec3 landing = c.ground(to);
			c.blink(c.caster, landing);
			c.bolt(landing);
			c.sound(SoundEvents.TRIDENT_THUNDER, landing, 0.9F, 1.2F);
			for (LivingEntity t : c.enemiesNear(landing, 2.5 * c.radius)) {
				c.shock(t, 8 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 1, 2);
			}
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, landing.add(0, 8, 0), landing, 0.6, 2);
			c.shake(landing, 0.3F, 8);
			c.punch(0.2F);
		});
	}

	/**
	 * Lensbolt: a glass lens gathers light over each enemy it is set on, then focuses one bolt that leaps on to the
	 * next nearest enemies. The look: shrinking glass rings over each head, then a bolt and sparks chaining away.
	 */
	@Pair(a = "lightning", b = "storm_glass", name = "Lensbolt", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A glass lens hangs over each of up to 4 enemies and gathers light, slowing them (Slowness I for 1.5 seconds). "
			+ "At 1.2 seconds it focuses a bolt: 10 lightning damage to that enemy, which leaps to up to 2 other enemies "
			+ "within 4 blocks for 5 each.")
	public static void lensbolt(PairCast c) {
		List<LivingEntity> lensed = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.9F, 1.6F);
		for (LivingEntity t : lensed) {
			c.effect(t, MobEffects.SLOWNESS, 1.5, 0);
		}
		c.every(6, 5, frame -> {
			for (LivingEntity t : c.still(lensed)) {
				Vec3 at = PairCast.mid(t).add(0, 1.2, 0);
				c.ring(PairCast.shift(0xDDF6FF, 0x7FD8FF, 0.7F), at, 1.0 - frame * 0.18, 16, frame * 0.4);
				if (frame == 4) {
					focus(c, t);
				}
			}
		});
	}

	private static void focus(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.shock(t, 10 * c.power);
		c.bolt(at);
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, at, 0.5F, 1.5F);
		c.sphere(PairCast.dust(0xDDF6FF, 0.6F), at, 0.9, 24);
		int leaps = 0;
		for (LivingEntity n : c.enemiesNear(at, 4 * c.radius)) {
			if (n == t) {
				continue;
			}
			if (leaps == 2) {
				break;
			}
			leaps++;
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(n), 0.4, 3);
			c.shock(n, 5 * c.power);
		}
	}

	/**
	 * Cloudward: a small cloud over each ally, raining on them once a second for four seconds. In the rain itself the
	 * cloud is heavier. The look: grey discs drifting above heads, falling water, and a splash on each heal.
	 */
	@Pair(a = "rain_cloud", b = "sky_reading", name = "Cloudward", element = "storm", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A small cloud forms over each ally within 6 blocks (8 at most) and rains on them once a second for four seconds: "
			+ "2 healing each time, or 3 while it is raining in the world. Any burning ally is doused at once.")
	public static void cloudward(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> under = PairCast.first(c.alliesNear(spot, 6 * c.radius), PairCast.MAX_TARGETS);
		double drop = c.level.isRaining() ? 3 : 2;
		c.sound(SoundEvents.PLAYER_SPLASH, spot, 0.7F, 1.6F);
		for (LivingEntity a : under) {
			c.douse(a);
		}
		c.every(5, 16, frame -> {
			for (LivingEntity a : c.still(under)) {
				Vec3 cloud = a.position().add(0, a.getBbHeight() + 0.8, 0);
				c.disc(PairCast.dust(0xB8C7D6, 1.0F), cloud, 0.9, 10);
				c.particles(ParticleTypes.FALLING_WATER, cloud, 4, 0.7, 0.0);
				if (frame % 4 == 0) {
					c.heal(a, drop * c.power);
					c.particles(ParticleTypes.DRIPPING_WATER, PairCast.mid(a), 6, 0.4, 0.0);
					c.sound(SoundEvents.BUBBLE_POP, PairCast.mid(a), 0.5F, 1.7F);
				}
			}
		});
	}

	/**
	 * Dewveil: a fine mist over the point for eight seconds. Allies inside it gather a sheen of Absorption when it
	 * settles, and are healed a little every second while they stay in it. The look: a pale disc of mist and dew
	 * drops falling through it.
	 */
	@Pair(a = "dewfall", b = "mending_mist", name = "Dewveil", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A dew falls for 8 seconds over 4 blocks round the point. Each ally inside it gets 4 Absorption for 6 seconds and "
			+ "is doused at once, then every second it stays in the mist it heals 1.")
	public static void dewveil(PairCast c) {
		Vec3 spot = c.point();
		double reach = 4 * c.radius;
		c.sound(SoundEvents.BUBBLE_POP, spot, 0.8F, 1.4F);
		for (LivingEntity a : c.alliesNear(spot, reach)) {
			c.douse(a);
			c.absorb(a, 4 * c.power, 6);
		}
		c.every(20, 8, frame -> {
			c.disc(PairCast.dust(0xD9F4FF, 0.9F), spot.add(0, 0.3, 0), reach, 36);
			c.particles(ParticleTypes.DRIPPING_WATER, spot.add(0, 3, 0), 8, reach * 0.6, 0.0);
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.heal(a, c.power);
			}
		});
	}

	/**
	 * Throwswitch: you and the first enemy trade places, and it is hurt where you stood. Half a second later it is
	 * snapped away from you like a lever. The look: an arc of violet light between the two places, then a sharp click
	 * and a ring of dust where it lands.
	 */
	@Pair(a = "leverflip", b = "swap", name = "Throwswitch", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "You and the first enemy trade places at once, if both spots are safe (bosses stay put). It takes 4 damage and "
			+ "Slowness III for 1 second. Then the lever snaps half a second later: 6 more damage and a shove away from you.")
	public static void throwswitch(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 mine = c.caster.position();
		Vec3 theirs = t.position();
		if (c.movable(t) && c.blink(c.caster, theirs)) {
			if (!c.blink(t, mine)) {
				c.blink(c.caster, mine);
			}
		}
		c.hurt(t, 4 * c.power);
		c.effect(t, MobEffects.SLOWNESS, 1, 2);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(t), 0.7F, 1.5F);
		c.arc(PairCast.shift(0xE0C8FF, 0x6A4BD6, 0.8F), mine.add(0, 1, 0), theirs.add(0, 1, 0), 1.2, 14);
		c.later(10, () -> {
			if (!c.here(t)) {
				return;
			}
			c.hurt(t, 6 * c.power);
			c.knockFrom(t, c.caster.position(), 1.0, 0.3);
			c.sound(SoundEvents.CHAIN_PLACE, PairCast.mid(t), 1.0F, 1.8F);
			c.wave(PairCast.dust(0xB9A7FF, 0.9F), PairCast.mid(t), 16, 0.3);
		});
	}

	/**
	 * Slamdoor: each enemy is thrown 8 blocks further away, and the spot it left is shut like a door a moment later.
	 * The look: two violet posts marking the doorway, then a slam of rings and dust that crushes what stands there.
	 */
	@Pair(a = "banish", b = "doorcall", name = "Slamdoor", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 4 enemies are banished 8 blocks further from you (if there is a safe spot, bosses stay put) and blinded for 2 seconds. The "
			+ "spot each left slams shut 1.5 seconds later: 6 damage to every enemy within 1.5 blocks of it, and Slowness II for "
			+ "2 seconds.")
	public static void slamdoor(PairCast c) {
		c.sound(SoundEvents.ENDERMAN_TELEPORT, c.point(), 0.8F, 0.6F);
		for (LivingEntity t : PairCast.first(c.enemies(), 4)) {
			Vec3 from = t.position();
			Vec3 flat = new Vec3(from.x - c.caster.getX(), 0, from.z - c.caster.getZ());
			Vec3 away = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
			c.blink(t, from.add(away.scale(8)));
			c.effect(t, MobEffects.BLINDNESS, 2, 0);
			c.line(PairCast.dust(0x6B5CFF, 0.9F), from, from.add(0, 2.6, 0), 3);
			c.later(30, () -> slam(c, from));
		}
	}

	private static void slam(PairCast c, Vec3 at) {
		c.sound(SoundEvents.ANVIL_LAND, at, 0.6F, 0.6F);
		c.shake(at, 0.3F, 6);
		for (LivingEntity n : c.enemiesNear(at, 1.5 * c.radius)) {
			c.hurt(n, 6 * c.power);
			c.effect(n, MobEffects.SLOWNESS, 2, 1);
		}
		// The door's frame closes in on the spot, three frames of rings collapsing into it.
		c.every(4, 3, frame -> {
			c.ring(PairCast.shift(0x9C8CFF, 0x2A2050, 0.9F), at.add(0, 1.2, 0), 1.6 - frame * 0.5, 16, frame * 0.3);
			c.disc(PairCast.dust(0x6B5CFF, 0.8F), at, 1.5 - frame * 0.4, 12);
		});
	}

	/**
	 * Bogdrag: a ditch of water runs from the point to each enemy it catches and drags them back to it, then the
	 * ground there turns to mire. The look: blue lines pulling in, then brown mud spread under each one that
	 * ticks down.
	 */
	@Pair(a = "ditchwater", b = "mire", name = "Bogdrag", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A ditch of water runs from the point to each of up to 3 enemies and drags it towards the point over half a second (bosses stay put). "
			+ "Its footing turns to mire for 4 seconds: Slowness III, soaked, and 1 damage a second.")
	public static void bogdrag(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemies(), 3);
		c.sound(SoundEvents.GENERIC_SPLASH, spot, 0.9F, 0.7F);
		for (LivingEntity t : caught) {
			c.effect(t, MobEffects.SLOWNESS, 4, 2);
			c.mark(t, Reactions.Mark.SOAKED);
		}
		c.every(5, 3, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, spot, 1.2);
				c.line(PairCast.dust(0x3B8FB0, 0.9F), PairCast.mid(t), spot, 3);
			}
		});
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.disc(PairCast.dust(0x5A4A2A, 1.0F), t.position().add(0, 0.1, 0), 1.1, 14);
				if (frame > 0) {
					c.hurt(t, c.power);
				}
			}
		});
	}

	/**
	 * Pressplate: an unseen plate lies at the point. Whatever it strikes is pressed at once, and whatever comes near
	 * it after is pressed too: each press is a click, a hurt and a knock up and away. The look: a grey disc of plate
	 * over the ground, and a puff of cloud at each press.
	 */
	@Pair(a = "buttonpush", b = "push", name = "Pressplate", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "An unseen plate lies at the point for 2 seconds. Up to 8 enemies it strikes are pressed at once, and each enemy that "
			+ "comes within 1.5 blocks of the point after is too: 4 damage, Slowness II for 2 seconds, and knocked up and away. "
			+ "Each enemy is pressed only once.")
	public static void pressplate(PairCast c) {
		Vec3 spot = c.point();
		double reach = 1.5 * c.radius;
		Set<LivingEntity> pressed = new HashSet<>();
		c.sound(SoundEvents.SCULK_CLICKING, spot, 0.9F, 1.1F);
		for (LivingEntity t : PairCast.first(c.enemies(), PairCast.MAX_TARGETS)) {
			pressed.add(t);
			press(c, t, spot);
		}
		c.every(5, 9, frame -> {
			c.disc(PairCast.dust(0xC9D3DE, 0.9F), spot.add(0, 0.2, 0), reach, 14);
			for (LivingEntity t : c.enemiesNear(spot, reach)) {
				if (pressed.add(t)) {
					press(c, t, spot);
				}
			}
		});
	}

	private static void press(PairCast c, LivingEntity t, Vec3 spot) {
		c.hurt(t, 4 * c.power);
		c.effect(t, MobEffects.SLOWNESS, 2, 1);
		c.knockFrom(t, spot, 1.0, 0.8);
		c.sound(SoundEvents.STONE_PLACE, PairCast.mid(t), 0.7F, 1.4F);
		c.particles(ParticleTypes.CLOUD, PairCast.mid(t), 6, 0.3, 0.05);
	}

	/**
	 * Bloomtide: petals gather over the point and open over the allies round it, healing each and putting Regeneration
	 * on those still hurt. The look: pink and green petals spiralling in, a burst of blossom as it opens, then petals
	 * drifting down over the spot.
	 */
	@Pair(a = "regrowth", b = "ripen", name = "Bloomtide", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Petals gather over the point for 2 seconds, then open over every ally within 5 blocks: each heals 6, and any still "
			+ "below half health gets Regeneration II for 4 seconds. Enemies are not touched.")
	public static void bloomtide(PairCast c) {
		Vec3 spot = c.point();
		double reach = 5 * c.radius;
		c.sound(SoundEvents.AZALEA_LEAVES_PLACE, spot, 0.9F, 0.8F);
		c.every(5, 8, frame -> c.ring(PairCast.shift(0xFFD6E8, 0x7CD992, 0.8F), spot.add(0, 1.2, 0),
			reach * (1 - frame * 0.1), 12, frame * 0.5));
		c.later(40, () -> {
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.heal(a, 6 * c.power);
				if (a.getHealth() < a.getMaxHealth() / 2) {
					c.effect(a, MobEffects.REGENERATION, 4, 1);
				}
				c.particles(ParticleTypes.CHERRY_LEAVES, PairCast.mid(a), 8, 0.5, 0.0);
			}
			c.sphere(PairCast.dust(0xFFD6E8, 1.0F), spot.add(0, 1, 0), reach * 0.6, 40);
			c.sound(SoundEvents.PLAYER_LEVELUP, spot, 0.6F, 1.8F);
		});
		c.later(60, () -> {
			c.column(PairCast.dust(0xFFC4DA, 0.8F), spot, reach, 3, 30);
			c.particles(ParticleTypes.HAPPY_VILLAGER, spot.add(0, 1, 0), 6, reach * 0.5, 0.0);
		});
	}

	/**
	 * Noonfire: sunlight gathers on the enemies it strikes and burns, hotter at noon than at night. The look: rays
	 * of sun gathering down onto each head, a flash of gold over the screen, then the burn.
	 */
	@Pair(a = "sun_reading", b = "sunbask", name = "Noonfire", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Daylight gathers on each of up to 4 enemies for 1.5 seconds, then burns: 2 damage at midnight to 8 at noon, "
			+ "and they are set alight for 2 seconds and blinded for 1. Enemies within 2 blocks of one take half that.")
	public static void noonfire(PairCast c) {
		long clock = c.level.getOverworldClockTime() % 24000L;
		double sun = Math.max(0, Math.sin(Math.PI * 2 * clock / 24000.0));
		double burn = (2 + 6 * sun) * c.power;
		List<LivingEntity> focused = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.BEACON_ACTIVATE, c.point(), 0.6F, 1.8F);
		c.tint(c.point(), 8, 0xFFD27A, 10);
		c.every(10, 4, frame -> {
			for (LivingEntity t : c.still(focused)) {
				Vec3 at = PairCast.mid(t).add(0, 1.6, 0);
				c.ring(PairCast.shift(0xFFF4B0, 0xFF9A3C, 0.8F), at, 1.0 - frame * 0.2, 14, frame * 0.6);
				c.line(PairCast.dust(0xFFE9A0, 0.6F), at.add(0, 2, 0), at, 2);
				if (frame == 3) {
					c.hurt(t, burn);
					c.ignite(t, 2);
					c.effect(t, MobEffects.BLINDNESS, 1, 0);
					c.sound(SoundEvents.FIRECHARGE_USE, at, 0.8F, 1.3F);
					for (LivingEntity n : c.enemiesNear(PairCast.mid(t), 2 * c.radius)) {
						if (n != t) {
							c.hurt(n, burn / 2);
						}
					}
				}
			}
		});
	}
}
