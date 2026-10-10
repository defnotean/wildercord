package dev.wildercord.pairs.b046;

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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs046 {
	private Pairs046() {}

	/**
	 * Harvest Feast: the allies are fed. Gourd-coloured motes spiral up round each one, the feast lands with strength
	 * and a little mending, and a last course follows two seconds later. The look: orange motes rising, a warm ring on
	 * the payoff, and a shower of spores over each ally at the end.
	 */
	@Pair(a = "feastday", b = "gourdcall", name = "Harvest Feast", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 6 allies are healed 4, get Strength I for 8 seconds and Regeneration I for 6 seconds. Two seconds later "
			+ "the last course heals each still here and hurt by 2 more.")
	public static void harvestFeast(PairCast c) {
		List<LivingEntity> fed = PairCast.first(c.allies(), 6);
		c.sound(SoundEvents.NOTE_BLOCK_CHIME, c.point(), 0.6F, 1.0F);
		// Wind-up: gourd-coloured motes spiral up round each ally, four frames a fifth of a second apart.
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(fed)) {
				c.spiral(PairCast.shift(0xFF9A2E, 0xFFD36A, 0.9F), t.position(), 0.9, 1.8, 1.5, 12);
			}
		});
		// Payoff: the feast lands on each ally.
		c.later(20, () -> {
			for (LivingEntity t : c.still(fed)) {
				c.heal(t, 4 * c.power);
				c.effect(t, MobEffects.STRENGTH, 8, 0);
				c.effect(t, MobEffects.REGENERATION, 6, 0);
				c.ring(PairCast.dust(0xFFB347, 1.0F), t.position().add(0, 0.1, 0), 1.2, 16, 0);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 10, 0.4, 0.02);
			}
			c.sound(SoundEvents.BELL_RESONATE, c.point(), 0.5F, 1.4F);
		});
		// Aftermath: the last course, two seconds on, heals any ally still hurt, and spores drift down over it.
		c.later(40, () -> {
			for (LivingEntity t : c.still(fed)) {
				if (t.getHealth() < t.getMaxHealth()) {
					c.heal(t, 2 * c.power);
				}
				c.wave(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 10, 0.15);
			}
		});
	}

	/**
	 * Sinkstone: a stone is dropped on the enemies it strikes. They are weighed down, and fliers are dragged to the
	 * ground. The look: a grey slab lowering over each target in steps, then the stone settling in a shock ring and a
	 * plume of dust.
	 */
	@Pair(a = "holefill", b = "weigh", name = "Sinkstone", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 6 enemies it strikes are weighed: Slowness III for 4 seconds, and fliers are dragged down. A second later "
			+ "the stone settles on each one still there: 4 damage.")
	public static void sinkstone(PairCast c) {
		List<LivingEntity> pinned = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : pinned) {
			c.effect(t, MobEffects.SLOWNESS, 4, 2);
			if (!t.onGround()) {
				c.push(t, new Vec3(0, -1.0, 0));
			}
		}
		c.sound(SoundEvents.STONE_PLACE, c.point(), 0.8F, 0.6F);
		// Wind-up: a grey slab lowers over each target in four steps.
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(pinned)) {
				c.disc(PairCast.dust(0x8A8270, 1.0F), t.position().add(0, 2.6 - frame * 0.6, 0), 0.9, 14);
			}
		});
		// Payoff: the stone settles on each target still under it.
		c.later(20, () -> {
			for (LivingEntity t : c.still(pinned)) {
				c.hurt(t, 4 * c.power);
				c.ring(PairCast.dust(0x6E665A, 1.0F), t.position().add(0, 0.1, 0), 1.8, 18, 0);
				c.particles(ParticleTypes.DUST_PLUME, PairCast.mid(t), 12, 0.4, 0.05);
			}
			c.shake(c.point(), 0.25F, 6);
			c.sound(SoundEvents.ANVIL_LAND, c.point(), 0.6F, 0.6F);
		});
	}

	/**
	 * Stone Vow: allies are sworn in under stone. They harden for eight seconds, and then anything pressing on them is
	 * shoved back, four times. The look: a ring of stone-gold light growing round each ally, then a ring shuddering
	 * outward on each shove.
	 */
	@Pair(a = "guardlink", b = "keepsafe", name = "Stone Vow", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 4 allies within 8 blocks get Resistance I and 4 absorption (2 hearts) for 8 seconds. Three quarters of a "
			+ "second later, four shoves a second apart: each enemy within 2 blocks of an ally is pushed away from it.")
	public static void stoneVow(PairCast c) {
		List<LivingEntity> sworn = PairCast.first(c.alliesNear(c.point(), 8), 4);
		for (LivingEntity t : sworn) {
			c.effect(t, MobEffects.RESISTANCE, 8, 0);
			c.absorb(t, 4 * c.power, 8);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 0.7F, 0.7F);
		// Wind-up: a ring of stone-gold light grows round each ally, three frames.
		c.every(5, 3, frame -> {
			for (LivingEntity t : c.still(sworn)) {
				c.ring(PairCast.shift(0xE8D08A, 0x6B5A3A, 0.9F), t.position().add(0, 0.1, 0), 0.6 + frame * 0.5, 16, frame * 0.3);
			}
		});
		// Payoff: four shoves, a second apart, from the ring round each ally.
		c.later(15, () -> c.every(20, 4, frame -> {
			for (LivingEntity t : c.still(sworn)) {
				Vec3 at = t.position();
				for (LivingEntity near : c.enemiesNear(PairCast.mid(t), 2)) {
					c.knockFrom(near, at, 0.9, 0.2);
				}
				c.ring(PairCast.dust(0xC9B27A, 1.0F), at.add(0, 0.1, 0), 1.6, 14, 0);
			}
			c.sound(SoundEvents.SHIELD_BLOCK, c.point(), 0.6F, 0.8F);
		}));
	}

	/**
	 * Sure Footing: the ground under the point is read, and the footing made sure. Allies within 6 blocks step up a
	 * full block and run quicker. The look: a plumb line of frost dropping from the point to the ground it reads, then
	 * a pale ring pulsing under each ally as the steps come.
	 */
	@Pair(a = "land_reading", b = "surefoot", name = "Sure Footing", element = "earth",kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Up to 8 allies within 6 blocks get Jump Boost I and Speed I for 8 seconds. A frost plumb line falls from the point "
			+ "to the ground it reads, and a pulse runs under each ally a second later.")
	public static void sureFooting(PairCast c) {
		List<LivingEntity> sure = PairCast.first(c.alliesNear(c.point(), 6), PairCast.MAX_TARGETS);
		Vec3 ground = c.ground(c.point());
		for (LivingEntity t : sure) {
			c.effect(t, MobEffects.JUMP_BOOST, 8 * c.duration, 0);
			c.effect(t, MobEffects.SPEED, 8 * c.duration, 0);
		}
		// Wind-up: the plumb line of frost is let down from the point to the ground.
		c.line(PairCast.shift(0xBFEFFF, 0x5A8AB0, 0.8F), c.point(), ground, 2);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.7F, 1.6F);
		// Payoff: a second later, a pale pulse runs under each ally.
		c.later(20, () -> {
			for (LivingEntity t : c.still(sure)) {
				c.ring(PairCast.dust(0xDDF6FF, 0.9F), t.position().add(0, 0.1, 0), 0.9, 12, 0);
				c.particles(ParticleTypes.SNOWFLAKE, PairCast.mid(t), 8, 0.4, 0.02);
			}
		});
	}

	/**
	 * Upheaval: the ground heaves under what the spell strikes, lifting the enemies into the air, and a second later it
	 * drops them back hard. The look: a dust dome swelling from the ground, the enemies rising in its shadow, then a
	 * slam with a shake.
	 */
	@Pair(a = "levelground", b = "thunderquake", name = "Upheaval", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Lifts up to 6 enemies it strikes, and any others within 3 blocks of the point, into the air. A second later the "
			+ "ground drops them: 5 damage each.")
	public static void upheaval(PairCast c) {
		Set<LivingEntity> heaved = new LinkedHashSet<>(PairCast.first(c.enemies(), 6));
		heaved.addAll(c.enemiesNear(c.point(), 3 * c.radius));
		Vec3 spot = c.ground(c.point());
		c.sound(SoundEvents.RAVAGER_ROAR, spot, 0.3F, 1.8F);
		// Wind-up: a dust dome swells from the ground under the point, three frames.
		c.every(5, 3, frame -> c.disc(PairCast.dust(0x8A7A5A, 1.1F), spot.add(0, 0.1, 0), 1.0 + frame * 1.0, 24));
		// Lift: the ground heaves, and everything in the dome goes up.
		c.later(10, () -> {
			for (LivingEntity t : c.still(heaved)) {
				c.lift(t, 1.0);
			}
			c.shake(spot, 0.2F, 6);
		});
		// Slam: a second after the lift, the ground drops them.
		c.later(30, () -> {
			for (LivingEntity t : c.still(heaved)) {
				Vec3 at = PairCast.mid(t);
				c.strike(t, 5 * c.power);
				c.ring(PairCast.dust(0x6E665A, 1.0F), t.position().add(0, 0.1, 0), 1.6, 16, 0);
				c.particles(ParticleTypes.CLOUD, at, 8, 0.4, 0.05);
			}
			c.shake(spot, 0.35F, 8);
			c.sound(SoundEvents.MACE_SMASH_GROUND_HEAVY, spot, 0.7F, 0.7F);
		});
	}

	/**
	 * Grindstone: a millstone of bone dust turns over the enemies it strikes, grinding each at every turn, and the last
	 * turn leaves them bleeding. The look: a bone-coloured ring rolling round the group, a chip of dust at each turn.
	 */
	@Pair(a = "bonespur", b = "millstone", name = "Grindstone", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 4 enemies it strikes are ground at four turns, half a second apart: 1 damage at each turn. After the last "
			+ "turn they are left bleeding.")
	public static void grindstone(PairCast c) {
		List<LivingEntity> milled = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.STONE_BREAK, c.point(), 0.6F, 0.7F);
		c.every(10, 4, turn -> {
			for (LivingEntity t : c.still(milled)) {
				Vec3 at = t.position().add(0, 0.2, 0);
				c.ring(PairCast.shift(0xEDE3CC, 0x6E6250, 0.9F), at, 1.3, 14, turn * 0.7);
				c.particles(ParticleTypes.CRIT, at, 4, 0.3, 0.05);
				c.hurt(t, 1 * c.power);
				if (turn == 3) {
					c.mark(t, Reactions.Mark.BLEEDING);
				}
			}
			c.sound(SoundEvents.GRAVEL_BREAK, c.point(), 0.4F, 1.0F + turn * 0.1F);
		});
	}

	/**
	 * Brood Warmth: a nest tends its own. Up to six allies are healed as a warm nest closes round them, and two seconds
	 * later the warmth comes back to any still hurt. The look: a green-gold nest-ring under each ally, hearts rising,
	 * then a second ring and a crack of egg-light.
	 */
	@Pair(a = "nest_tend", b = "tend", name = "Brood Warmth", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Heals up to 6 allies within 5 blocks by 6. Two seconds later the warmth returns: each still here and hurt is "
			+ "healed 2 more.")
	public static void broodWarmth(PairCast c) {
		List<LivingEntity> brooded = PairCast.first(c.alliesNear(c.point(), 5), 6);
		c.sound(SoundEvents.TURTLE_EGG_CRACK, c.point(), 0.6F, 1.1F);
		// Wind-up: a nest-ring of warm green-gold closes under each ally.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(brooded)) {
				c.ring(PairCast.shift(0x9BD17A, 0xF4E7A1, 0.9F), t.position().add(0, 0.1, 0), 1.4 - frame * 0.3, 14, frame * 0.4);
			}
		});
		// Payoff: the nest heals each ally, and hearts rise from them.
		c.later(10, () -> {
			for (LivingEntity t : c.still(brooded)) {
				c.heal(t, 6 * c.power);
				c.particles(ParticleTypes.HEART, PairCast.mid(t), 3, 0.3, 0.02);
			}
		});
		// Second warmth: two seconds on, it returns to any ally still hurt.
		c.later(40, () -> {
			for (LivingEntity t : c.still(brooded)) {
				if (t.getHealth() < t.getMaxHealth()) {
					c.heal(t, 2 * c.power);
					c.ring(PairCast.dust(0xF4E7A1, 0.9F), t.position().add(0, 0.1, 0), 0.9, 12, 0);
				}
			}
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, c.point(), 0.5F, 1.3F);
		});
	}

	/**
	 * Vein Hook: a hook of ore-light flies out and catches up to 3 enemies it struck, and reels each in towards you. The
	 * look: a gilt thread running out from you, the catch sparking, a streak of grey ore-dust drawing each one in, and
	 * a prick of light for those still close.
	 */
	@Pair(a = "long_arm", b = "orepluck", name = "Vein Hook", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Hooks up to 3 enemies it struck and reels each in towards you, taking 5 damage. A second later each one still "
			+ "within 4 blocks of you takes 2 more.")
	public static void veinHook(PairCast c) {
		List<LivingEntity> hooked = PairCast.first(c.enemies(), 3);
		Vec3 me = PairCast.mid(c.caster);
		c.line(PairCast.shift(0xE8B040, 0xFFF0B0, 0.7F), me, c.point(), 3);
		c.particles(ParticleTypes.ENCHANTED_HIT, c.point(), 8, 0.3, 0.1);
		c.sound(SoundEvents.CHAIN_HIT, c.point(), 0.7F, 1.2F);
		// Catch: a moment later the hook bites each one, reels it in and strikes it.
		c.later(6, () -> {
			for (LivingEntity t : c.still(hooked)) {
				c.line(PairCast.dust(0x9AA6B8, 0.8F), PairCast.mid(t), me, 3);
				c.pullTo(t, me, 1.2);
				c.hurt(t, 5 * c.power);
			}
			c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, c.point(), 0.7F, 0.9F);
		});
		// Aftermath: a second later the ore pricks each one still close to you.
		c.later(26, () -> {
			for (LivingEntity t : c.still(hooked)) {
				if (t.position().distanceTo(c.caster.position()) <= 4) {
					c.hurt(t, 2 * c.power);
					c.particles(ParticleTypes.CRIT, PairCast.mid(t), 6, 0.3, 0.1);
				}
			}
		});
	}

	/**
	 * Skipping Stone: a flat stone strikes the enemy it hits, then skips on along the spell's flight, hopping over any
	 * other enemy in its way. The look: a frost spray at the strike, then a pale skip-trail and a splash at each hop.
	 */
	@Pair(a = "plankway", b = "skimstep", name = "Skipping Stone", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Enemies it strikes take 4 damage and are soaked. Half a second later the stone skips on along its flight: four "
			+ "hops, 3 blocks apart and half a second apart. Each other enemy within 1.5 blocks of a hop takes 3 damage, once.")
	public static void skippingStone(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 flight = flat(c);
		Set<LivingEntity> skipped = new HashSet<>(struck);
		for (LivingEntity t : struck) {
			c.hurt(t, 4 * c.power);
			c.mark(t, Reactions.Mark.SOAKED);
		}
		c.sound(SoundEvents.FISHING_BOBBER_SPLASH, c.point(), 0.7F, 1.4F);
		c.particles(ParticleTypes.SPLASH, c.point(), 12, 0.4, 0.1);
		// Each hop: the stone skims 3 blocks on from where it last landed, stopped by any wall in the way.
		Vec3[] at = {c.point()};
		boolean[] stopped = {false};
		c.later(10, () -> c.every(10, 4, hop -> {
			if (stopped[0]) {
				return;
			}
			Vec3 from = at[0];
			Vec3 to = from.add(flight.scale(3));
			BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.3, 0), to.add(0, 0.3, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster));
			if (wall.getType() != HitResult.Type.MISS) {
				stopped[0] = true;
				return;
			}
			at[0] = to;
			c.line(PairCast.shift(0xBFEFFF, 0x5A8AB0, 0.8F), from.add(0, 0.3, 0), to.add(0, 0.3, 0), 2);
			c.particles(ParticleTypes.SPLASH, to, 6, 0.3, 0.05);
			c.ring(PairCast.dust(0xDDF6FF, 0.9F), to.add(0, 0.1, 0), 0.8, 10, hop * 0.5);
			for (LivingEntity near : c.enemiesNear(to, 1.5)) {
				if (skipped.add(near)) {
					c.hurt(near, 3 * c.power);
				}
			}
			c.sound(SoundEvents.GENERIC_SPLASH, to, 0.5F, 1.2F + hop * 0.1F);
		}));
	}

	/**
	 * Furrow Charge: you charge the way you face like a boar through a field, ploughing a furrow as you go. Whatever
	 * stands by your path is struck and tossed aside. The look: dust gathering at your feet, a furrow cut in the ground
	 * behind each stride, and a shake when the charge ends.
	 */
	@Pair(a = "plowline", b = "tusk_charge", name = "Furrow Charge", element = "earth", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You charge up to 8 blocks the way you face, stopped by walls or a drop, ploughing a furrow. Each enemy within 1.5 blocks of "
			+ "your path takes 3 damage and is knocked aside, once.")
	public static void furrowCharge(PairCast c) {
		Vec3 heading = flat(c);
		int steps = Math.max(1, (int) Math.round(8 * c.power));
		Set<LivingEntity> rammed = new HashSet<>();
		Vec3[] at = {c.caster.position()};
		boolean[] walled = {false};
		c.sound(SoundEvents.RAVAGER_ROAR, at[0], 0.5F, 1.6F);
		// Wind-up: dust gathers round your feet for a third of a second before the charge.
		c.every(2, 3, frame -> c.ring(PairCast.shift(0xC8A060, 0x5A3A1A, 0.9F), c.caster.position().add(0, 0.1, 0),
			0.8 + frame * 0.3, 12, frame * 0.4));
		// Charge: one block a stride, two ticks apart, stopping at the first wall or drop.
		c.later(6, () -> c.every(2, steps, step -> {
			if (walled[0] || !c.caster.isAlive()) {
				return;
			}
			Vec3 here = at[0];
			Vec3 next = here.add(heading);
			BlockHitResult wall = c.level.clip(new ClipContext(here.add(0, 1, 0), next.add(0, 1, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster));
			if (wall.getType() != HitResult.Type.MISS || !c.blink(c.caster, next)) {
				walled[0] = true;
				return;
			}
			Vec3 now = c.caster.position();
			at[0] = now;
			c.line(PairCast.shift(0x7A5A2A, 0xC8A060, 0.9F), here.add(0, 0.2, 0), now.add(0, 0.2, 0), 2);
			c.particles(ParticleTypes.CLOUD, now, 4, 0.3, 0.02);
			Vec3 chest = PairCast.mid(c.caster);
			for (LivingEntity near : c.enemiesNear(chest, 1.5)) {
				if (rammed.add(near)) {
					c.hurt(near, 3 * c.power);
					c.knockFrom(near, now, 0.6, 0.6);
				}
			}
			c.sound(SoundEvents.HOE_TILL, now, 0.5F, 0.8F);
		}));
		// The charge ends: a punch of breath and a shake where you stop.
		c.later(6 + 2 * steps + 4, () -> {
			c.punch(0.25F);
			c.shake(at[0], 0.2F, 6);
		});
	}

	/** The flat direction of the spell's flight, as a unit vector (never zero). */
	private static Vec3 flat(PairCast c) {
		Vec3 d = new Vec3(c.dir().x, 0, c.dir().z);
		if (d.lengthSqr() < 1.0E-4) {
			d = new Vec3(1, 0, 0);
		}
		return d.normalize();
	}
}
