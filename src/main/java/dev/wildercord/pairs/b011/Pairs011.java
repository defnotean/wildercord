package dev.wildercord.pairs.b011;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Batch 11: ten hand-made pairs, each its own mechanic. */
public final class Pairs011 {
	private Pairs011() {}

	/**
	 * Frozen Meteor: ice pins the first enemies in place, then a comet of ice falls on the point and shatters on them.
	 */
	@Pair(a = "cometfall", b = "glacier", name = "Frozen Meteor", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Ice pins up to 8 enemies in place (Slowness III for 2 seconds). Half a second later a comet of ice crashes on "
			+ "the point: 6 damage and 2 seconds alight to every enemy within 3 blocks, and 4 more to those pinned.")
	public static void frozenMeteor(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> pinned = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : pinned) {
			c.effect(t, MobEffects.SLOWNESS, 2, 2);
			c.ring(PairCast.dust(0xBFF0FF, 0.9F), PairCast.mid(t), 0.8, 12, 0);
			c.sound(SoundEvents.GLASS_PLACE, PairCast.mid(t), 0.7F, 1.5F);
		}
		// The comet streaks down over five frames, and strikes on the sixth.
		c.every(2, 6, frame -> {
			if (frame < 5) {
				Vec3 top = at.add(0, 8 - frame * 1.4, 0);
				c.line(PairCast.shift(0xE6D8FF, 0x9FEBFF, 1.1F), top, at, 2);
				c.sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, top, 0.4F, 1.8F);
			} else {
				meteorImpact(c, at, pinned);
			}
		});
	}

	private static void meteorImpact(PairCast c, Vec3 at, List<LivingEntity> pinned) {
		c.wave(PairCast.dust(0xE6F8FF, 1.1F), at.add(0, 0.2, 0), 28, 0.35);
		c.sphere(PairCast.shift(0xB48CFF, 0xBFF0FF, 1.2F), at, 1.2, 30);
		c.particles(ParticleTypes.SNOWFLAKE, at, 30, 1.2, 0.1);
		c.particles(ParticleTypes.FLAME, at, 12, 0.5, 0.05);
		c.sound(SoundEvents.GLASS_BREAK, at, 1.0F, 0.7F);
		c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.5F, 1.6F);
		c.shake(at, 0.35F, 8);
		for (LivingEntity t : c.enemiesNear(at, 3 * c.radius)) {
			c.hurt(t, 6 * c.power);
			c.ignite(t, 2);
			if (pinned.contains(t)) {
				c.hurt(t, 4 * c.power);
				c.line(PairCast.shift(0xBFF0FF, 0xFFFFFF, 0.9F), at, PairCast.mid(t), 3);
			}
		}
	}

	/**
	 * Stellar Squall: hail falls on the point in volleys, and at the second volley a star streaks down on the nearest
	 * of them. The look: a column of white hail pinging about, then one violet star with a bright flare.
	 */
	@Pair(a = "hail", b = "starfall", name = "Stellar Squall", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Hail falls on the point for 2 seconds: every half second up to 4 enemies within 3 blocks take 2 damage, are lifted "
			+ "a little and slowed (Slowness II for 4 seconds). At 1 second a star falls on the nearest of them: 6 damage, and it is exposed.")
	public static void stellarSquall(PairCast c) {
		Vec3 at = c.point();
		double r = 3 * c.radius;
		c.every(10, 5, beat -> {
			List<LivingEntity> hit = PairCast.first(c.still(c.enemiesNear(at, r)), 4);
			c.column(PairCast.dust(0xDDF6FF, 0.9F), at, r, 6, 16);
			c.sound(SoundEvents.GLASS_HIT, at, 0.6F, 1.2F + beat * 0.1F);
			for (LivingEntity t : hit) {
				c.hurt(t, 2 * c.power);
				c.lift(t, 0.25);
				c.effect(t, MobEffects.SLOWNESS, 4, 1);
			}
			if (beat == 2 && !hit.isEmpty()) {
				starfall(c, hit.getFirst());
			}
		});
	}

	private static void starfall(PairCast c, LivingEntity t) {
		Vec3 hitAt = PairCast.mid(t);
		c.line(PairCast.shift(0xFFFFFF, 0xB48CFF, 1.2F), hitAt.add(0, 8, 0), hitAt, 2);
		c.hurt(t, 6 * c.power);
		c.mark(t, Reactions.Mark.EXPOSED);
		c.star(PairCast.dust(0xB48CFF, 1.0F), hitAt, 6, 1.2, 0);
		c.sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, hitAt, 1.0F, 1.3F);
	}

	/**
	 * Rime Splinter: a rime shard lodges in the first enemy and chills it each second, slowing it deeper, until it
	 * splinters and throws star sparks at the nearest others.
	 */
	@Pair(a = "frostbite", b = "starshard", name = "Rime Splinter", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A rime shard lodges in the first enemy: 4 damage at once and Slowness I. Each second after, it chills for 1 damage "
			+ "and slows deeper (Slowness II, then III). After 4 seconds it splinters: 8 more, and three star sparks strike the "
			+ "nearest other enemies within 8 blocks, 4 each.")
	public static void rimeSplinter(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		c.every(20, 5, beat -> {
			if (!c.here(t)) {
				return;
			}
			Vec3 at = PairCast.mid(t);
			if (beat == 0) {
				c.hurt(t, 4 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 1.5, 0);
				c.line(PairCast.shift(0xA8F0FF, 0xE6FBFF, 1.0F), t.position(), at, 3);
				c.sound(SoundEvents.AMETHYST_CLUSTER_PLACE, at, 1.0F, 1.2F);
			} else if (beat < 4) {
				c.hurt(t, 1 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 1.5, Math.min(beat, 2));
				c.ring(PairCast.dust(0xA8F0FF, 0.8F), at, 0.7, 10, beat * 0.5);
				c.sound(SoundEvents.AMETHYST_BLOCK_HIT, at, 0.6F, 1.0F + beat * 0.2F);
			} else {
				splinter(c, t, at);
			}
		});
	}

	private static void splinter(PairCast c, LivingEntity t, Vec3 at) {
		c.hurt(t, 8 * c.power);
		c.star(PairCast.dust(0xFFE8A0, 1.0F), at, 6, 1.2, 0);
		c.particles(ParticleTypes.END_ROD, at, 16, 0.4, 0.15);
		c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, at, 1.0F, 0.8F);
		List<LivingEntity> others = new ArrayList<>();
		for (LivingEntity n : c.enemiesNear(at, 8 * c.radius)) {
			if (n != t) {
				others.add(n);
			}
		}
		for (LivingEntity n : PairCast.first(others, 3)) {
			c.hurt(n, 4 * c.power);
			c.line(PairCast.shift(0xA8F0FF, 0xFFE8A0, 0.9F), at, PairCast.mid(n), 4);
			c.sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, PairCast.mid(n), 0.6F, 1.6F);
		}
	}

	/**
	 * Sentence of Stillness: the decree slows the enemies round the point and pacifies the ones that are not bosses;
	 * when the sentence has been read, whoever still stands in the circle takes a blow of light.
	 */
	@Pair(a = "decree", b = "truce", name = "Sentence of Stillness", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Up to 8 enemies within 4 blocks are slowed to a crawl (Slowness III) for 2 seconds, and those that are not bosses "
			+ "are pacified (Weakness II) for 4. Then the sentence is read: 6 damage to each still within 4 blocks.")
	public static void sentenceOfStillness(PairCast c) {
		Vec3 at = c.point();
		double r = 4 * c.radius;
		List<LivingEntity> held = PairCast.first(c.enemiesNear(at, r), PairCast.MAX_TARGETS);
		for (LivingEntity t : held) {
			c.effect(t, MobEffects.SLOWNESS, 2, 2);
			if (c.movable(t)) {
				c.effect(t, MobEffects.WEAKNESS, 4, 1);
			}
		}
		c.sound(SoundEvents.BELL_BLOCK, at, 1.0F, 0.6F);
		// A gold ring widens out to the circle's edge over the sentence.
		c.every(4, 5, frame -> c.ring(PairCast.dust(0xFFE08A, 1.0F), at.add(0, 0.3, 0), r * (frame + 1) / 5.0, 24, frame * 0.2));
		c.later(40, () -> readSentence(c, at, r, held));
	}

	private static void readSentence(PairCast c, Vec3 at, double r, List<LivingEntity> held) {
		c.sound(SoundEvents.ANVIL_LAND, at, 0.8F, 0.6F);
		c.sound(SoundEvents.BELL_BLOCK, at, 1.0F, 0.5F);
		c.shake(at, 0.3F, 8);
		for (LivingEntity t : c.still(held)) {
			if (t.position().distanceTo(at) <= r) {
				c.hurt(t, 6 * c.power);
				c.column(PairCast.dust(0xFFF6D6, 1.3F), t.position(), 0.6, 4, 20);
			}
		}
	}

	/**
	 * Glass Reflection: allies gain a glassy shield, and for six seconds whatever presses in on them is shoved off,
	 * hurt and slowed. The look: a thin mirror ring round each ally, pulsing once a second.
	 */
	@Pair(a = "reflect", b = "riposte", name = "Glass Reflection", element = "time", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Allies within 6 blocks (you included) gain 4 absorption hearts for 6 seconds. For those 6 seconds, every second, "
			+ "each enemy within 2.5 blocks of an ally is knocked back from it, takes 2 damage and is slowed (Slowness I for 1 second).")
	public static void glassReflection(PairCast c) {
		List<LivingEntity> allies = c.alliesNear(c.point(), 6 * c.radius);
		for (LivingEntity a : allies) {
			c.absorb(a, 4 * c.power, 6);
			c.sound(SoundEvents.GLASS_PLACE, PairCast.mid(a), 0.8F, 1.6F);
		}
		c.every(20, 6, beat -> {
			for (LivingEntity a : c.still(allies)) {
				Vec3 at = PairCast.mid(a);
				c.ring(PairCast.dust(0xE8F8FF, 0.9F), at, 2.5 * c.radius, 20, beat * 0.3);
				for (LivingEntity e : c.enemiesNear(at, 2.5 * c.radius)) {
					c.knockFrom(e, at, 1.0, 0.25);
					c.hurt(e, 2 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 1, 0);
				}
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.6F, 1.5F);
		});
	}

	/**
	 * Hallowed Bloom: the first ally is healed and crowned with a halo that smites the nearest enemy each beat; when
	 * the halo fades, the bloom bursts and heals everyone near. The look: a gold halo ring, a beam, then petals.
	 */
	@Pair(a = "halo", b = "lifebloom", name = "Hallowed Bloom", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "The first ally is healed 4 at once and crowned with a halo for 6 seconds: every 1.5 seconds it smites the nearest "
			+ "enemy within 6 blocks of them for 3 holy damage and heals them 1. When the halo fades the bloom bursts, healing "
			+ "every ally within 3 blocks for 3.")
	public static void hallowedBloom(PairCast c) {
		LivingEntity ally = c.firstAlly();
		if (ally == null) {
			return;
		}
		c.heal(ally, 4 * c.power);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(ally), 0.8F, 1.2F);
		c.every(30, 5, beat -> {
			if (!c.here(ally)) {
				return;
			}
			Vec3 crown = ally.position().add(0, ally.getBbHeight() + 0.6, 0);
			c.ring(PairCast.dust(0xFFF3C4, 0.9F), crown, 0.6, 14, beat * 0.4);
			LivingEntity foe = c.nearestEnemy(PairCast.mid(ally), 6 * c.radius, null);
			if (foe != null) {
				c.line(PairCast.shift(0xFFF3C4, 0xFFFFFF, 0.9F), crown, PairCast.mid(foe), 3);
				c.hurt(foe, 3 * c.power);
				c.heal(ally, 1 * c.power);
				c.sound(SoundEvents.NOTE_BLOCK_CHIME, PairCast.mid(ally), 0.7F, 1.0F + beat * 0.1F);
			}
			if (beat == 4) {
				bloomBurst(c, ally);
			}
		});
	}

	private static void bloomBurst(PairCast c, LivingEntity ally) {
		Vec3 at = PairCast.mid(ally);
		c.wave(PairCast.dust(0xFF9ACB, 1.0F), at, 24, 0.3);
		c.particles(ParticleTypes.HEART, at, 6, 1.0, 0.1);
		for (LivingEntity a : c.alliesNear(at, 3 * c.radius)) {
			c.heal(a, 3 * c.power);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 1.0F, 0.9F);
	}

	/**
	 * Stone Bastion: allies are shielded and resisting, and stone plates rise round each one, then settle into dust.
	 */
	@Pair(a = "aegis", b = "brace", name = "Stone Bastion", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Allies within 6 blocks (you included) gain 4 absorption hearts for 6 seconds and Resistance II for 3. Stone plates "
			+ "rise round each one, and after 3 seconds they settle into dust.")
	public static void stoneBastion(PairCast c) {
		List<LivingEntity> walled = PairCast.first(c.alliesNear(c.point(), 6 * c.radius), PairCast.MAX_TARGETS);
		for (LivingEntity a : walled) {
			c.absorb(a, 4 * c.power, 6);
			c.effect(a, MobEffects.RESISTANCE, 3, 1);
		}
		// Four frames: the plates climb from the ground to the shoulders.
		c.every(4, 4, frame -> {
			for (LivingEntity a : c.still(walled)) {
				Vec3 at = a.position().add(0, 0.3 + frame * 0.5, 0);
				c.ring(PairCast.dust(0xB8A88A, 1.1F), at, 1.1, 10, frame * 0.3);
				c.sound(SoundEvents.STONE_PLACE, a.position(), 0.6F, 0.8F + frame * 0.1F);
			}
		});
		c.later(60, () -> {
			for (LivingEntity a : c.still(walled)) {
				Vec3 at = PairCast.mid(a);
				c.disc(PairCast.dust(0xD8CCAA, 0.8F), a.position(), 1.2, 20);
				c.particles(ParticleTypes.ASH, at, 10, 0.6, 0.05);
				c.sound(SoundEvents.STONE_BREAK, at, 0.6F, 1.2F);
			}
		});
	}

	/**
	 * Purloined Hour: the first enemy is stripped of its good effects, and up to two of them go to you for up to 10
	 * seconds. A thread of gold streams from it to you. With nothing to steal, you are quickened instead.
	 */
	@Pair(a = "nullify", b = "timesteal", name = "Purloined Hour", element = "time", kind = EffectKind.HARMFUL,
		traits = {"duration"},
		text = "The first enemy is stripped of all its good effects, and up to 2 of them go to you with the time they had left "
			+ "(at most 10 seconds). The target drags (Slowness I for 2 seconds); with nothing to steal, you are quickened "
			+ "instead (Speed I for 2 seconds).")
	public static void purloinedHour(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 at = PairCast.mid(t);
		c.effect(t, MobEffects.SLOWNESS, 2, 0);
		int taken = 0;
		for (MobEffectInstance e : List.copyOf(t.getActiveEffects())) {
			if (e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
				continue;
			}
			t.removeEffect(e.getEffect());
			if (taken < 2) {
				int left = e.getDuration() < 0 ? 200 : Math.min(e.getDuration(), 200);
				c.effect(c.caster, e.getEffect(), left / 20.0, Math.min(e.getAmplifier(), 2));
				taken++;
			}
		}
		if (taken == 0) {
			c.effect(c.caster, MobEffects.SPEED, 2, 0);
		}
		c.sound(SoundEvents.BEACON_DEACTIVATE, at, 0.9F, 1.6F);
		// The gold thread runs from the target to the caster over five frames, then the caster's clock ticks.
		c.every(4, 5, frame -> {
			Vec3 hand = PairCast.mid(c.caster);
			Vec3 p = at.add(hand.subtract(at).scale((frame + 1) / 5.0));
			c.particles(PairCast.dust(0xE9D27A, 1.0F), p, 2, 0.08, 0);
		});
		c.later(20, () -> {
			Vec3 hand = PairCast.mid(c.caster);
			c.ring(PairCast.dust(0xE9D27A, 1.0F), hand, 1.0, 16, 0);
			c.particles(ParticleTypes.END_ROD, hand, 8, 0.5, 0.05);
			c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, hand, 0.8F, 1.1F);
		});
	}

	/**
	 * Prismatic Echo: the first enemy's marks are spent: each one is worth 3 damage on the hit and sends a ray of colour
	 * to the nearest other enemy. The marked enemies round it take half, and it is left resonant.
	 */
	@Pair(a = "prismatic_burst", b = "resonance", name = "Prismatic Echo", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "5 damage, and 3 more for each mark the first enemy carries (up to four), which are then used up: each sends a ray to "
			+ "the nearest other enemy within 6 blocks for 3 damage. Up to 4 other marked enemies within 16 blocks take half the "
			+ "first hit, and it is left resonant for 10 seconds.")
	public static void prismaticEcho(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 at = PairCast.mid(t);
		Set<Reactions.Mark> marks = Reactions.marks(t);
		int used = Math.min(4, marks.size());
		double hit = (5 + 3 * used) * c.power;
		List<LivingEntity> marked = new ArrayList<>();
		for (LivingEntity o : c.enemiesNear(at, 16 * c.radius)) {
			if (o != t && !Reactions.marks(o).isEmpty()) {
				marked.add(o);
			}
		}
		c.hurt(t, hit);
		List<Reactions.Mark> spent = new ArrayList<>(marks);
		for (int i = 0; i < used; i++) {
			Reactions.clear(t, spent.get(i));
		}
		for (LivingEntity o : PairCast.first(marked, 4)) {
			c.hurt(o, hit / 2);
			c.line(PairCast.shift(0xFFE66B, 0x6BB8FF, 0.9F), at, PairCast.mid(o), 3);
		}
		c.mark(t, Reactions.Mark.RESONANT);
		// The colours burst out in five rays, then each spent mark's ray flies to its neighbour.
		int[] colours = {0xFF6B6B, 0xFFE66B, 0x6BFFB8, 0x6BB8FF, 0xC06BFF};
		for (int i = 0; i < colours.length; i++) {
			c.star(PairCast.dust(colours[i], 1.0F), at, 1, 2.5, i * Math.PI * 2 / colours.length);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 1.0F, 1.2F);
		c.every(6, 3, beat -> {
			if (beat == 1) {
				for (int i = 0; i < used; i++) {
					LivingEntity n = c.nearestEnemy(at, 6 * c.radius, t);
					if (n != null) {
						c.hurt(n, 3 * c.power);
						c.line(PairCast.shift(0xFF6B6B, 0xC06BFF, 1.0F), at, PairCast.mid(n), 3);
					}
				}
				c.sound(SoundEvents.BELL_RESONATE, at, 0.8F, 1.4F);
			} else if (beat == 2) {
				c.wave(PairCast.shift(0x6BFFB8, 0xC06BFF, 1.0F), at, 20, 0.25);
			}
		});
	}

	/**
	 * Sonar Lantern: a sonar pulse makes every enemy near glow through walls. The first one is revealed and pinged four
	 * times, a second apart; each ping draws a line of sound back from the pulse's point to it and withers it.
	 */
	@Pair(a = "echolocate", b = "reveal", name = "Sonar Lantern", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Every enemy within 16 blocks glows through walls for 10 seconds. The first is stripped of invisibility, exposed, and "
			+ "pinged four times, a second apart, for 2 wither damage each: 8 in all.")
	public static void sonarLantern(PairCast c) {
		Vec3 at = c.point();
		for (LivingEntity e : c.enemiesNear(at, 16 * c.radius)) {
			c.effect(e, MobEffects.GLOWING, 10, 0);
		}
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		t.removeEffect(MobEffects.INVISIBILITY);
		c.mark(t, Reactions.Mark.EXPOSED);
		c.sphere(PairCast.dust(0x5FF0FF, 0.6F), at, 1.5, 24);
		c.every(20, 4, beat -> {
			if (!c.here(t)) {
				return;
			}
			Vec3 body = PairCast.mid(t);
			c.line(PairCast.shift(0x5FF0FF, 0x2B1B4A, 0.8F), at, body, 3);
			c.wave(PairCast.dust(0x5FF0FF, 0.8F), body, 18, 0.25);
			c.ring(PairCast.dust(0x2B1B4A, 1.0F), body, 1.2 + beat * 0.3, 16, beat);
			c.wither(t, 2 * c.power);
			c.sound(SoundEvents.WARDEN_HEARTBEAT, body, 0.9F, 1.1F + beat * 0.1F);
		});
	}
}
