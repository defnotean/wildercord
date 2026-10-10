package dev.wildercord.pairs.b002;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Batch two: ten hand-made pairs, each with its own shape, palette, sounds and screen feel. */
public final class Pairs002 {
	private Pairs002() {}

	/**
	 * Ashen Name: the target's name is burned into it in cinders. The look: a ring of black-ember sparks tightens
	 * over its chest, then the brand sinks in with a flash, and every second after the name flares with a small
	 * puff of smoke and flame.
	 */
	@Pair(a = "cinderbrand", b = "hex", name = "Ashen Name", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Writes each target's name in cinders (8 at most): 3 fire damage, alight for 4 seconds, and Shadowed. "
			+ "For 6 seconds after, the name flares: 1 more fire damage each second.")
	public static void ashenName(PairCast c) {
		List<LivingEntity> named = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		// Wind-up: sparks spiral inward over each chest for half a second.
		c.every(2, 4, frame -> {
			for (LivingEntity t : c.still(named)) {
				c.ring(PairCast.shift(0xFFB347, 0x3A0A2E, 0.9F), PairCast.mid(t), 1.5 - frame * 0.35, 12, frame * 0.7);
			}
		});
		// The brand lands at half a second, then it flares once a second for six seconds.
		c.later(10, () -> c.every(20, 7, frame -> {
			for (LivingEntity t : c.still(named)) {
				if (frame == 0) {
					brand(c, t);
				} else {
					flare(c, t);
				}
			}
		}));
	}

	private static void brand(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.burn(t, 3 * c.power);
		c.ignite(t, 4);
		c.mark(t, Reactions.Mark.SHADOWED);
		c.particles(ParticleTypes.FLAME, at, 14, 0.3, 0.05);
		c.particles(ParticleTypes.SMOKE, at, 8, 0.25, 0.02);
		c.sound(SoundEvents.FIRECHARGE_USE, at, 0.8F, 0.7F);
		c.sound(SoundEvents.BOOK_PAGE_TURN, at, 0.6F, 0.6F);
	}

	private static void flare(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t).add(0, 0.9, 0);
		c.burn(t, 1 * c.power);
		c.wave(ParticleTypes.SMALL_FLAME, at, 10, 0.12);
		c.particles(ParticleTypes.SMOKE, at, 3, 0.2, 0.01);
		c.sound(SoundEvents.FIRE_EXTINGUISH, at, 0.35F, 1.8F);
	}

	/**
	 * Seam of Embers: a blade of red heat is drawn across each target, the cut opens, and it keeps bleeding. The
	 * look: a red line grows across the body in five frames, then glowing drops fall from the wound.
	 */
	@Pair(a = "rend", b = "searing_edge", name = "Seam of Embers", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A red seam is cut across each target (8 at most): 5 magic damage that ignores armour, 3 fire damage, "
			+ "alight for 4 seconds, and it bleeds: 1 more magic damage each second for 4 seconds.")
	public static void seamOfEmbers(PairCast c) {
		List<LivingEntity> cut = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 side = sideways(c.dir());
		// The blade draws the seam: a red-hot line growing across each target in five frames.
		c.every(2, 5, frame -> {
			for (LivingEntity t : c.still(cut)) {
				Vec3 from = PairCast.mid(t).subtract(side.scale(1.2));
				c.line(PairCast.shift(0xFF5A1F, 0xFF2B1A, 0.8F), from, from.add(side.scale(2.4 * (frame + 1) / 5)), 4);
			}
		});
		c.later(10, () -> {
			for (LivingEntity t : c.still(cut)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 5 * c.power);
				c.burn(t, 3 * c.power);
				c.ignite(t, 4);
				c.mark(t, Reactions.Mark.BLEEDING);
				c.particles(ParticleTypes.CRIT, at, 10, 0.3, 0.2);
				c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, at, 1.0F, 0.9F);
				c.sound(SoundEvents.LAVA_POP, at, 0.6F, 1.2F);
			}
			c.shake(c.point(), 0.15F, 6);
			// The bleeding: four drops, one a second, each a little magic damage.
			c.every(20, 4, frame -> {
				for (LivingEntity t : c.still(cut)) {
					Vec3 at = PairCast.mid(t);
					c.hurt(t, 1 * c.power);
					c.particles(ParticleTypes.LAVA, at.add(0, -0.4, 0), 3, 0.2, 0.02);
					c.mote(ParticleTypes.DRIPPING_WATER, at, new Vec3(0, -0.2, 0));
				}
			});
		});
	}

	private static Vec3 sideways(Vec3 dir) {
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		return side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
	}

	/**
	 * Starfall Upheaval: a burning meteor is seen falling out of the sky over each target, lands 1.2 seconds later,
	 * and the ground heaves. The look: a streak of embers from high above, then a flash, a smoking crater, and the
	 * enemies round it thrown up.
	 */
	@Pair(a = "meteor", b = "tremor", name = "Starfall Upheaval", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A meteor falls on each target (8 at most) 1.2 seconds later: 9 fire damage, and the crater burns 1 fire "
			+ "damage a second for 3 seconds to enemies within 2.5 blocks. The blast tears the ground: other enemies within "
			+ "3.5 blocks take 4 damage and are thrown up.")
	public static void starfallUpheaval(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		// The fall: a streak of embers drops from 18 blocks up over 1.2 seconds.
		c.every(2, 12, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t).add(0, 18 - frame * 1.5, 0);
				c.particles(ParticleTypes.FLAME, at, 3, 0.25, 0.02);
				c.particles(ParticleTypes.SMALL_FLAME, at.add(0, 0.6, 0), 2, 0.3, 0.05);
				if (frame % 3 == 0) {
					c.sound(SoundEvents.FIRECHARGE_USE, at, 0.4F, 1.6F);
				}
			}
		});
		c.later(24, () -> {
			for (LivingEntity t : c.still(struck)) {
				impact(c, t);
			}
		});
	}

	private static void impact(PairCast c, LivingEntity t) {
		Vec3 ground = c.ground(t.position());
		Vec3 hitAt = PairCast.mid(t);
		c.burn(t, 9 * c.power);
		c.wave(ParticleTypes.CLOUD, ground.add(0, 0.2, 0), 24, 0.35);
		c.particles(ParticleTypes.LAVA, hitAt, 10, 0.5, 0.1);
		c.sound(SoundEvents.GENERIC_EXPLODE, hitAt, 0.9F, 0.9F);
		c.sound(SoundEvents.MACE_SMASH_GROUND, ground, 1.0F, 0.7F);
		c.shake(ground, 0.5F, 12);
		// The tremor: the enemies round the crater are struck through their armour and thrown up.
		for (LivingEntity near : c.enemiesNear(ground, 3.5 * c.radius)) {
			if (near != t) {
				c.strike(near, 4 * c.power);
				c.lift(near, 0.8);
			}
		}
		// The crater smoulders for three seconds: one fire damage a second to whatever stands in it.
		c.every(20, 3, frame -> {
			for (LivingEntity in : c.enemiesNear(ground, 2.5 * c.radius)) {
				c.burn(in, 1 * c.power);
			}
			c.disc(PairCast.shift(0xFF9A2E, 0x3B2A1E, 1.0F), ground, 2.5 * c.radius, 30);
			c.particles(ParticleTypes.SMOKE, ground.add(0, 0.5, 0), 6, 1.2, 0.02);
		});
	}

	/**
	 * Ember Updraft: a hot column of air lifts each target in seven puffs over three seconds, and every puff drops
	 * a cinder on it. The look: a pillar of orange sparks under each foe, a shockwave of smoke at the ground.
	 */
	@Pair(a = "levitate", b = "torchfall", name = "Ember Updraft", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A hot updraft lifts each target (8 at most) in seven puffs over three seconds, and each puff drops a "
			+ "cinder for 1 fire damage. Lifted foes are Airborne, so every spell hits them harder.")
	public static void emberUpdraft(PairCast c) {
		List<LivingEntity> lifted = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.every(10, 7, frame -> {
			for (LivingEntity t : c.still(lifted)) {
				c.mark(t, Reactions.Mark.AIRBORNE);
				c.lift(t, 0.45);
				c.burn(t, 1 * c.power);
				Vec3 foot = t.position();
				c.column(PairCast.shift(0xFFD27A, 0xFF4500, 0.9F), foot, 0.6, 1.8, 14);
				c.wave(ParticleTypes.SMALL_FLAME, foot.add(0, 0.2, 0), 8, 0.1);
			}
			if (frame == 0) {
				c.sound(SoundEvents.WIND_CHARGE_BURST, c.point(), 0.9F, 0.8F);
			} else {
				c.sound(SoundEvents.LAVA_POP, c.point(), 0.5F, 1.0F + 0.05F * frame);
			}
		});
	}

	/**
	 * Hearthbalm: a warm balm is spread over each ally. The look: a gold-and-honey ring rises round each ally in
	 * turn, hearts float off them at once, and a slow warm halo keeps turning for ten seconds while frost stays off.
	 */
	@Pair(a = "salve", b = "warm_cloak", name = "Hearthbalm", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Heals each ally (8 at most) 4 half-hearts, gives Regeneration I for 8 seconds and puts out their fire. "
			+ "For 10 seconds frost can't build on them: their freeze is reset every quarter second.")
	public static void hearthbalm(PairCast c) {
		List<LivingEntity> warmed = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : warmed) {
			Vec3 at = PairCast.mid(t);
			c.heal(t, 4 * c.power);
			c.effect(t, MobEffects.REGENERATION, 8, 0);
			c.douse(t);
			c.particles(ParticleTypes.HEART, at, 3, 0.3, 0.02);
			c.sound(SoundEvents.BONE_MEAL_USE, at, 0.7F, 1.3F);
		}
		// The warm halo: a ring turning round each ally for ten seconds, reset against frost every four ticks.
		c.every(4, Math.max(1, c.ticks(10) / 4), frame -> {
			for (LivingEntity t : c.still(warmed)) {
				t.setTicksFrozen(0);
				Vec3 foot = t.position().add(0, (frame % 10) * 0.18, 0);
				c.ring(PairCast.dust(0xFFC46B, 1.0F), foot, 0.8, 10, frame * 0.4);
			}
		});
	}

	/**
	 * Smouldering Hour: the target is slowed and a slow clock hand of fire sweeps round it, one tick each second.
	 * The look: a ring of small flames with a hand that moves once a second, and a crackle on the first and last.
	 */
	@Pair(a = "fire", b = "tarry", name = "Smouldering Hour", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Slows each target (8 at most) with Slowness I for 10 seconds, and a hot clock hand sweeps round it: "
			+ "1 fire damage each second, 10 times over.")
	public static void smoulderingHour(PairCast c) {
		List<LivingEntity> slowed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : slowed) {
			c.effect(t, MobEffects.SLOWNESS, 10, 0);
		}
		final int hours = Math.max(1, c.ticks(10) / 20);
		c.every(20, hours, frame -> {
			for (LivingEntity t : c.still(slowed)) {
				Vec3 foot = t.position().add(0, 0.1, 0);
				Vec3 at = PairCast.mid(t);
				double hand = frame * 0.6;
				c.ring(PairCast.shift(0xFFD27A, 0xFF5A1F, 0.8F), foot, 1.0, 12, 0);
				c.line(PairCast.dust(0xFF8A1F, 0.9F), at, at.add(Math.cos(hand) * 1.0, 0, Math.sin(hand) * 1.0), 3);
				c.burn(t, 1 * c.power);
			}
			if (frame == 0) {
				c.sound(SoundEvents.CAMPFIRE_CRACKLE, c.point(), 0.7F, 0.8F);
			} else if (frame == hours - 1) {
				c.sound(SoundEvents.FIRE_EXTINGUISH, c.point(), 0.8F, 0.9F);
			}
		});
	}

	/**
	 * Ember Tithe: a thread of blood runs from each target to your chest, the life drawn up it in motes. The look:
	 * a dark red thread and motes climbing it, then a flare of embers on the foe and a warm pulse in your chest.
	 */
	@Pair(a = "ember", b = "leech", name = "Ember Tithe", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Each target (8 at most) takes 4 magic damage and you heal 2 half-hearts for it; what a full heart can't "
			+ "hold becomes absorption of up to 4 half-hearts for 10 seconds. Each is alight for 3 seconds, or 3 more "
			+ "if already burning (10 at most).")
	public static void emberTithe(PairCast c) {
		List<LivingEntity> tithed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 chest = PairCast.mid(c.caster);
		for (LivingEntity t : tithed) {
			c.line(PairCast.dust(0x7A0010, 0.8F), PairCast.mid(t), chest, 2);
		}
		// The life climbs the thread: five motes a target, reaching the chest on the last.
		c.every(2, 5, frame -> {
			for (LivingEntity t : c.still(tithed)) {
				c.particles(PairCast.dust(0xC0102B, 0.9F), lerp(PairCast.mid(t), chest, frame / 5.0), 1, 0, 0);
			}
		});
		c.later(10, () -> {
			double over = 0;
			for (LivingEntity t : c.still(tithed)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 4 * c.power);
				double heal = 2 * c.power;
				double room = Math.max(0, c.caster.getMaxHealth() - c.caster.getHealth());
				c.heal(c.caster, heal);
				over += Math.max(0, heal - room);
				int burning = t.getRemainingFireTicks();
				if (burning > 0) {
					t.igniteForTicks(Math.min(c.ticks(10), burning + c.ticks(3)));
				} else {
					c.ignite(t, 3);
				}
				c.particles(ParticleTypes.ENCHANT, at, 8, 0.3, 0.1);
				c.sound(SoundEvents.CAMPFIRE_CRACKLE, at, 0.8F, 1.4F);
			}
			if (over > 0) {
				c.absorb(c.caster, Math.min(4, over), 10);
			}
			c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, chest, 0.8F, 0.8F);
			c.tint(chest, 2, 0x9A0014, 6);
			c.punch(0.12F);
		});
	}

	private static Vec3 lerp(Vec3 a, Vec3 b, double f) {
		return a.add(b.subtract(a).scale(f));
	}

	/**
	 * Judgment Pyre: a ring of flame closes at each target's feet, and 0.7 seconds later a column of fire and light
	 * stands on it. The look: a shrinking ring of gold-white and orange, then the column, then a shockwave of
	 * wax-white sparks.
	 */
	@Pair(a = "fire", b = "smite", name = "Judgment Pyre", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A ring of fire closes at each target's feet (8 at most); 0.7 seconds later a column of fire and light "
			+ "deals 6 fire and 7 magic damage, strips its Absorption and sets it alight for 3 seconds. Enemies within "
			+ "2 blocks take 2 fire damage.")
	public static void judgmentPyre(PairCast c) {
		List<LivingEntity> judged = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.every(2, 4, frame -> {
			for (LivingEntity t : c.still(judged)) {
				c.ring(PairCast.shift(0xFFF4C2, 0xFF6A00, 0.9F), t.position().add(0, 0.1, 0), 1.8 - frame * 0.4, 18, frame * 0.4);
			}
		});
		c.later(14, () -> {
			for (LivingEntity t : c.still(judged)) {
				judgment(c, t);
			}
		});
	}

	private static void judgment(PairCast c, LivingEntity t) {
		Vec3 foot = t.position();
		Vec3 at = PairCast.mid(t);
		c.burn(t, 6 * c.power);
		c.hurt(t, 7 * c.power);
		t.setAbsorptionAmount(0);
		c.ignite(t, 3);
		c.column(PairCast.shift(0xFFF4C2, 0xFF6A00, 1.2F), foot, 0.9, 3.2, 40);
		c.particles(ParticleTypes.END_ROD, at, 10, 0.3, 0.05);
		c.sound(SoundEvents.BEACON_ACTIVATE, at, 1.0F, 1.5F);
		c.sound(SoundEvents.FIRECHARGE_USE, at, 0.8F, 0.6F);
		c.shake(at, 0.35F, 10);
		c.tint(at, 12, 0xFFE9A0, 6);
		for (LivingEntity near : c.enemiesNear(foot, 2 * c.radius)) {
			if (near != t) {
				c.burn(near, 2 * c.power);
			}
		}
		c.every(3, 3, frame -> c.wave(ParticleTypes.WAX_ON, foot.add(0, 0.2, 0), 20, 0.2 + 0.1 * frame));
	}

	/**
	 * Unveiled Flame: an arcane flash strikes each target, a star of violet light, and the foe is unveiled: exposed,
	 * burning, and burned again each second. The look: a veil of violet shrinking onto the body, a star, a helix of
	 * fire and glow rising, and sparks each second.
	 */
	@Pair(a = "fire", b = "harm", name = "Unveiled Flame", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A flash of arcane light strikes each target (8 at most): 4 magic damage and 4 fire damage, alight for 6 "
			+ "seconds, and Exposed. Then for 3 seconds it burns 1 more fire damage each second.")
	public static void unveiledFlame(PairCast c) {
		List<LivingEntity> unveiled = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		// The veil: a violet shell shrinking onto each target over half a second.
		c.every(2, 4, frame -> {
			for (LivingEntity t : c.still(unveiled)) {
				c.sphere(PairCast.dust(0xB48CFF, 1.0F), PairCast.mid(t), 1.6 - frame * 0.35, 22);
			}
		});
		c.later(8, () -> {
			for (LivingEntity t : c.still(unveiled)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 4 * c.power);
				c.burn(t, 4 * c.power);
				c.ignite(t, 6);
				c.mark(t, Reactions.Mark.EXPOSED);
				c.star(PairCast.dust(0xE8D8FF, 1.0F), at, 6, 1.6, 0.3);
				c.sound(SoundEvents.EVOKER_PREPARE_ATTACK, at, 0.8F, 1.4F);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.9F, 1.2F);
			}
		});
		// A helix of fire and glow rises round each unveiled foe.
		c.later(12, () -> {
			for (LivingEntity t : c.still(unveiled)) {
				c.helix(ParticleTypes.FLAME, ParticleTypes.GLOW, t.position(), 0.6, 2.2, 1.5, 30);
			}
		});
		// Then the unveiling burns: one a second for three seconds, with a glint each time.
		c.later(20, () -> c.every(20, 3, frame -> {
			for (LivingEntity t : c.still(unveiled)) {
				c.burn(t, 1 * c.power);
				c.particles(ParticleTypes.GLOW, PairCast.mid(t), 4, 0.4, 0.05);
			}
		}));
	}

	/**
	 * Rime Conductor: each target is chilled into Slowness II, then struck by lightning that jumps to the nearest
	 * other enemy. The look: crystals gather inward over each target in a pale blue ring, a zigzag bolt crackles to
	 * its neighbour, and a shatter of snow.
	 */
	@Pair(a = "chill", b = "shock", name = "Rime Conductor", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Chills each target (8 at most): Slowness II for 6 seconds and 1 freeze damage, then 3 lightning damage. "
			+ "The bolt jumps to the nearest other enemy within 4 blocks for 2 lightning damage.")
	public static void rimeConductor(PairCast c) {
		List<LivingEntity> rimed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : rimed) {
			c.effect(t, MobEffects.SLOWNESS, 6, 1);
			c.freeze(t, 1 * c.power);
		}
		// Crystals gather inward over each target for 0.4 seconds.
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(rimed)) {
				c.ring(PairCast.dust(0xD8F4FF, 0.9F), PairCast.mid(t), 1.4 - frame * 0.3, 12, frame * 0.5);
			}
		});
		c.later(12, () -> {
			for (LivingEntity t : c.still(rimed)) {
				conduct(c, t);
			}
		});
	}

	private static void conduct(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.shock(t, 3 * c.power);
		c.particles(ParticleTypes.SNOWFLAKE, at, 12, 0.4, 0.08);
		c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.6F, 1.6F);
		LivingEntity next = c.nearestEnemy(at, 4 * c.radius, t);
		if (next != null) {
			Vec3 to = PairCast.mid(next);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, to, 0.5, 2);
			c.shock(next, 2 * c.power);
			c.particles(ParticleTypes.SNOWFLAKE, to, 6, 0.3, 0.05);
			c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, to, 0.8F, 1.7F);
			c.shake(to, 0.15F, 6);
		}
	}
}
