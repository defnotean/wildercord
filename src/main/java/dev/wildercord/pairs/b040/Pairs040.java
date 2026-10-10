package dev.wildercord.pairs.b040;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. Nothing here places or breaks blocks. */
public final class Pairs040 {
	private Pairs040() {}

	/**
	 * Quiet Pool: the allies round you are cleansed at once, and the ones that stand still in the pool are mended
	 * two seconds later. The look: a teal ring swelling out across the pool, then a spiral of light on each ally
	 * that held still, and smoke on the ones that wandered.
	 */
	@Pair(a = "cleanse", b = "stillwell", name = "Quiet Pool", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 4 blocks lose every harmful effect, their fire and every elemental mark. Two seconds later each one "
			+ "that has not moved more than 0.3 blocks is healed 4 health (2 hearts) and gets 6 Absorption (3 hearts) for 6 seconds.")
	public static void quietPool(PairCast c) {
		Vec3 pool = c.caster.position();
		List<LivingEntity> allies = c.alliesNear(pool, 4 * c.radius);
		Map<LivingEntity, Vec3> where = new LinkedHashMap<>();
		c.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, pool, 0.6F, 1.3F);
		for (LivingEntity a : allies) {
			where.put(a, a.position());
			cleanse(c, a);
			c.particles(ParticleTypes.END_ROD, PairCast.mid(a), 10, 0.4, 0.05);
		}
		// The pool swells outward: a teal ring growing across five beats.
		c.every(5, 5, frame -> c.ring(PairCast.shift(0x7FF5E0, 0xE6FFF4, 1.0F), pool.add(0, 0.1, 0),
			0.8 * (frame + 1) * c.radius, 20, frame * 0.2));
		// Payoff: the still ones are mended; the restless ones only puff smoke.
		c.later(40, () -> {
			for (Map.Entry<LivingEntity, Vec3> entry : where.entrySet()) {
				LivingEntity a = entry.getKey();
				if (!c.here(a)) {
					continue;
				}
				Vec3 at = PairCast.mid(a);
				if (a.position().distanceToSqr(entry.getValue()) > 0.09) {
					c.particles(ParticleTypes.SMOKE, at, 4, 0.2, 0.02);
					continue;
				}
				c.heal(a, 4 * c.power);
				c.absorb(a, 6 * c.power, 6);
				c.spiral(PairCast.dust(0xB8FFE6, 0.9F), at, 0.5, 1.8, 1, 10);
				c.particles(ParticleTypes.HAPPY_VILLAGER, at, 4, 0.4, 0.05);
			}
			c.ring(PairCast.dust(0x7FF5E0, 1.0F), pool.add(0, 0.1, 0), 4 * c.radius, 24, 0);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, pool, 0.7F, 1.3F);
		});
	}

	private static void cleanse(PairCast c, LivingEntity t) {
		List<Holder<MobEffect>> bad = new ArrayList<>();
		for (MobEffectInstance effect : t.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				bad.add(effect.getEffect());
			}
		}
		bad.forEach(t::removeEffect);
		c.douse(t);
		for (Reactions.Mark mark : Reactions.Mark.values()) {
			Reactions.clear(t, mark);
		}
	}

	/**
	 * Fault Line: a crack runs from your feet to each enemy, and the ground erupts under it half a second later. The
	 * look: a brown jagged seam crawling out to each target, a shove up into the air, then a column of dust.
	 */
	@Pair(a = "break", b = "tremor", name = "Fault Line", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A crack runs from your feet to each of up to 3 enemies: 6 damage to each, marked Cracked, and thrown up. Half a "
			+ "second later the ground erupts under each one: 3 damage to every other enemy within 2 blocks of it.")
	public static void faultLine(PairCast c) {
		Vec3 from = c.caster.position().add(0, 0.1, 0);
		List<LivingEntity> struck = PairCast.first(c.enemies(), 3);
		List<Vec3> feet = new ArrayList<>();
		for (LivingEntity t : struck) {
			feet.add(t.position());
		}
		c.sound(SoundEvents.DEEPSLATE_BREAK, from, 0.6F, 0.6F);
		// The seam crawls out over half a second, five pieces at a time.
		c.every(2, 5, frame -> {
			for (int i = 0; i < struck.size(); i++) {
				if (!c.here(struck.get(i))) {
					continue;
				}
				Vec3 end = feet.get(i);
				Vec3 prev = from.add(end.subtract(from).scale(frame / 5.0));
				Vec3 tip = from.add(end.subtract(from).scale((frame + 1) / 5.0));
				c.zigzag(PairCast.dust(0x8A5A34, 0.9F), prev, tip, 0.15, 3);
			}
		});
		c.later(10, () -> {
			for (LivingEntity t : c.still(struck)) {
				c.hurt(t, 6 * c.power);
				c.mark(t, Reactions.Mark.CRACKED);
				c.lift(t, 0.6);
				c.zigzag(PairCast.dust(0xE0B070, 0.8F), PairCast.mid(t), PairCast.mid(t).add(0, 1.2, 0), 0.2, 3);
			}
			c.sound(SoundEvents.STONE_BREAK, from, 0.7F, 0.7F);
		});
		// The eruption, where each one stood when the crack reached it.
		c.later(20, () -> {
			for (int i = 0; i < struck.size(); i++) {
				LivingEntity t = struck.get(i);
				Vec3 at = feet.get(i);
				c.column(PairCast.dust(0xB07A40, 1.2F), at, 1.2 * c.radius, 1.6, 30);
				c.wave(PairCast.shift(0xE0B070, 0x5A3A22, 1.1F), at.add(0, 0.2, 0), 16, 0.3);
				c.sound(SoundEvents.MACE_SMASH_GROUND, at, 0.6F, 1.1F);
				c.shake(at, 0.3F, 8);
				for (LivingEntity e : c.enemiesNear(at, 2 * c.radius)) {
					if (e != t) {
						c.strike(e, 3 * c.power);
					}
				}
			}
		});
	}

	/**
	 * Scored Hide: each enemy is cut twice across the body and marked to bleed, then struck by three chisel taps a
	 * second apart that pass through armour. The look: two dark red slashes, then a puff of crit sparks each tap.
	 */
	@Pair(a = "chisel", b = "rend", name = "Scored Hide", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 3 enemies are scored: Weakness I for 8 seconds, and marked Bleeding. Then three chisel taps, a second apart, "
			+ "each 2 magic damage that ignores armour.")
	public static void scoredHide(PairCast c) {
		List<LivingEntity> scored = PairCast.first(c.enemies(), 3);
		for (LivingEntity t : scored) {
			c.effect(t, MobEffects.WEAKNESS, 8, 0);
			c.mark(t, Reactions.Mark.BLEEDING);
		}
		c.sound(SoundEvents.SHEARS_SNIP, c.point(), 0.7F, 0.8F);
		// Frame 0 cuts the slashes; frames 1 to 3 are the taps.
		c.every(20, 4, frame -> {
			for (LivingEntity t : c.still(scored)) {
				Vec3 at = PairCast.mid(t);
				if (frame == 0) {
					c.line(PairCast.shift(0xB01E2A, 0x5A0A12, 0.9F), at.add(-0.5, 0.6, -0.4), at.add(0.5, -0.6, 0.4), 4);
					c.line(PairCast.shift(0xB01E2A, 0x5A0A12, 0.9F), at.add(-0.5, -0.1, 0.4), at.add(0.5, 0.9, -0.4), 4);
					c.sound(SoundEvents.SHEARS_SNIP, at, 0.6F, 1.1F);
				} else {
					c.hurt(t, 2 * c.power);
					c.particles(ParticleTypes.DAMAGE_INDICATOR, at, 3, 0.2, 0.05);
					c.particles(ParticleTypes.CRIT, at, 4, 0.3, 0.1);
					c.sound(SoundEvents.DEEPSLATE_BREAK, at, 0.5F, 1.2F + 0.1F * frame);
				}
			}
		});
	}

	/**
	 * Rampart Bell: a bronze ring rises round you and rings out; allies inside are shielded, and every two seconds the
	 * enemies inside the ring are knocked back out of it. The look: a bronze wall of ring rising, then a pale pulse
	 * each time the bell sounds.
	 */
	@Pair(a = "citadel", b = "shieldwall", name = "Rampart Bell", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Allies within 6 blocks take 6 Absorption (3 hearts) for 10 seconds and get Resistance I for 8 seconds. At once, then "
			+ "every 2 seconds up to 6 seconds, enemies within 6 blocks are knocked away from you and take 2 damage.")
	public static void rampartBell(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 center = PairCast.mid(me);
		double reach = 6 * c.radius;
		for (LivingEntity a : c.alliesNear(center, reach)) {
			c.absorb(a, 6 * c.power, 10);
			c.effect(a, MobEffects.RESISTANCE, 8, 0);
		}
		c.sound(SoundEvents.BELL_BLOCK, center, 0.9F, 0.8F);
		// Wind-up: a bronze ring rising from the ground to the top of the wall.
		c.every(5, 4, frame -> c.ring(PairCast.shift(0xE8D9A8, 0x8A7A5A, 1.0F), me.position().add(0, 0.3 + 0.6 * frame, 0),
			reach, 32, frame * 0.1));
		// Each beat: the bell rings, and whatever is inside the wall is knocked out of it.
		c.every(40, 4, frame -> {
			for (LivingEntity e : c.enemiesNear(center, reach)) {
				c.knockFrom(e, me.position(), 0.9, 0.3);
				c.hurt(e, 2 * c.power);
			}
			c.wave(PairCast.shift(0xFFE9B0, 0x8A7A5A, 1.0F), center.add(0, 1, 0), 24, 0.5);
			c.ring(PairCast.dust(0xFFE9B0, 1.0F), me.position().add(0, 0.2, 0), reach, 32, 0);
			c.sound(SoundEvents.BELL_RESONATE, center, 0.8F, 0.9F + 0.1F * frame);
		});
	}

	/**
	 * Cave-in: each enemy struck is dusted and slowed, then a 3x3 roof sinks over the point and falls. The look: a
	 * square of dust descending above the spot, ash sifting from it, then a column of stone dust at the collapse.
	 */
	@Pair(a = "excavate", b = "sinkhole", name = "Cave-in", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 3 enemies it hits are dusted: Slowness I for 2 seconds. A 3x3 roof sinks over the point for 1.5 seconds, then falls: "
			+ "10 damage and Slowness III for 2 seconds to each enemy within 2 blocks of the point, and 3 damage to each one up to 4 blocks away.")
	public static void caveIn(PairCast c) {
		Vec3 at = c.point();
		for (LivingEntity t : PairCast.first(c.enemies(), 3)) {
			c.effect(t, MobEffects.SLOWNESS, 2, 0);
		}
		c.sound(SoundEvents.SAND_FALL, at, 0.8F, 0.7F);
		// The roof: a square of dust sinking from five blocks up to two, sifting ash as it comes.
		c.every(5, 6, frame -> {
			double h = 5.0 - 0.5 * frame;
			Vec3 nw = at.add(-1.5, h, -1.5);
			Vec3 ne = at.add(1.5, h, -1.5);
			Vec3 se = at.add(1.5, h, 1.5);
			Vec3 sw = at.add(-1.5, h, 1.5);
			c.line(PairCast.dust(0xB8A88A, 0.9F), nw, ne, 3);
			c.line(PairCast.dust(0xB8A88A, 0.9F), ne, se, 3);
			c.line(PairCast.dust(0xB8A88A, 0.9F), se, sw, 3);
			c.line(PairCast.dust(0xB8A88A, 0.9F), sw, nw, 3);
			c.particles(ParticleTypes.ASH, at.add(0, h, 0), 10, 1.4, 0.02);
		});
		// The collapse: close enemies are crushed and pinned, farther ones are only struck.
		c.later(30, () -> {
			c.sound(SoundEvents.DEEPSLATE_BREAK, at, 1.0F, 0.6F);
			c.column(PairCast.dust(0x7A6A55, 1.4F), at, 1.6 * c.radius, 1.5, 40);
			c.wave(PairCast.shift(0xD9C7A0, 0x5A4A3A, 1.2F), at.add(0, 0.3, 0), 22, 0.35);
			c.shake(at, 0.5F, 10);
			for (LivingEntity e : c.enemiesNear(at, 4 * c.radius)) {
				if (PairCast.mid(e).distanceTo(at) <= 2 * c.radius) {
					c.strike(e, 10 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 2, 2);
				} else {
					c.strike(e, 3 * c.power);
				}
			}
		});
	}

	/**
	 * Quagmire: enemies it hits are drawn down to the spot, and a mud floor forms there a second later. The look:
	 * a violet vortex drawing in, then a brown disc of mud that holds for four seconds.
	 */
	@Pair(a = "gravity_well", b = "pitfloor", name = "Quagmire", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Enemies it hits are pulled to the spot with Slowness II for 4 seconds, and airborne ones within 4 blocks are dragged "
			+ "down. A second later a 3x3 mud floor holds for 4 seconds: each enemy on it takes 2 withering damage and gets Slowness "
			+ "III for 2 seconds, every second.")
	public static void quagmire(PairCast c) {
		Vec3 at = c.point();
		for (LivingEntity t : PairCast.first(c.enemies(), 3)) {
			c.pullTo(t, at, 0.7);
			c.mark(t, Reactions.Mark.PULLED);
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
		}
		for (LivingEntity e : c.enemiesNear(at, 4 * c.radius)) {
			if (!e.onGround() && e.getY() > at.y + 1) {
				c.push(e, new Vec3(0, -1.0, 0));
			}
		}
		c.sound(SoundEvents.ENDERMAN_TELEPORT, at, 0.4F, 0.5F);
		// The vortex: a violet ring that shrinks as it draws in over the spot.
		c.every(4, 4, frame -> c.ring(PairCast.shift(0x6B4FA0, 0x1A1030, 1.0F), at.add(0, 3.0 - 0.6 * frame, 0),
			(4 - frame) * c.radius, 16, frame * 0.3));
		// The mud floor: redrawn and hurting once a second for four seconds.
		c.later(20, () -> c.every(20, 4, frame -> {
			c.disc(PairCast.dust(0x5C4330, 1.4F), at.add(0, 0.1, 0), 1.5 * c.radius, 24);
			if (frame == 0) {
				c.sound(SoundEvents.MUD_PLACE, at, 0.8F, 0.8F);
			}
			for (LivingEntity e : c.enemiesNear(at, 1.5 * c.radius)) {
				c.wither(e, 2 * c.power);
				c.effect(e, MobEffects.SLOWNESS, 2, 2);
			}
		}));
	}

	/**
	 * Richvein: up to four enemies are drawn to the point and held in a gold vein. Two seconds later the vein pays
	 * out to whoever is gathered there. The look: gold threads pulsing in from each one, then a star of gold
	 * bursting from the spot.
	 */
	@Pair(a = "magnetize", b = "motherlode", name = "Richvein", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Pulls up to 4 enemies it hits towards the point, with Slowness I for 3 seconds. Two seconds later the vein pays out: "
			+ "each enemy within 1.5 blocks of the point takes 2 magic damage for every enemy gathered there, 6 at most.")
	public static void richvein(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> drawn = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : drawn) {
			c.pullTo(t, at, 0.9);
			c.mark(t, Reactions.Mark.PULLED);
			c.effect(t, MobEffects.SLOWNESS, 3, 0);
		}
		c.sound(SoundEvents.LODESTONE_HIT, at, 0.8F, 1.2F);
		// The vein: gold threads from each one to the point, pulsing as the ore fills.
		c.every(10, 4, frame -> {
			for (LivingEntity t : c.still(drawn)) {
				c.line(PairCast.shift(0xFFD34A, 0xFFF4C2, 0.8F), PairCast.mid(t), at.add(0, 1, 0), 3);
			}
			c.ring(PairCast.dust(0xFFD34A, 1.0F), at.add(0, 0.1, 0), 1.0 + 0.5 * frame, 16, frame * 0.2);
		});
		// The payout: each enemy gathered there takes 2 per enemy gathered, up to 6.
		c.later(40, () -> {
			Set<LivingEntity> gathered = new LinkedHashSet<>(c.enemiesNear(at, 1.5 * c.radius));
			if (gathered.isEmpty()) {
				return;
			}
			double each = Math.min(6, 2 * gathered.size()) * c.power;
			for (LivingEntity e : gathered) {
				c.hurt(e, each);
			}
			c.star(PairCast.shift(0xFFF4C2, 0xFFD34A, 1.2F), at.add(0, 0.5, 0), 8, 1.6, 0);
			c.column(PairCast.dust(0xFFD34A, 1.0F), at, 1.0 * c.radius, 1.5, 24);
			c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, at, 0.8F, 1.0F);
			c.shake(at, 0.25F, 6);
		});
	}

	/**
	 * Stairfall: up to three enemies are lifted up five steps, marked Airborne at the top, and dropped. The look: a
	 * pale ring at each one's feet at every step, a wind burst at the top, then a stone-coloured impact.
	 */
	@Pair(a = "launch", b = "riser", name = "Stairfall", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Lifts up to 3 enemies in five steps over 2 seconds, marks them Airborne at the top and drops them. Each one back on "
			+ "the ground 1.5 seconds later takes 6 damage.")
	public static void stairfall(PairCast c) {
		List<LivingEntity> lifted = PairCast.first(c.enemies(), 3);
		c.sound(SoundEvents.BREEZE_JUMP, c.point(), 0.6F, 0.8F);
		// Five steps, a step every 0.4 seconds.
		c.every(10, 5, frame -> {
			for (LivingEntity t : c.still(lifted)) {
				c.lift(t, 0.5);
				c.ring(PairCast.dust(0xDDF4FF, 1.0F), t.position().add(0, 0.1, 0), 0.9, 12, frame * 0.4);
			}
			c.sound(SoundEvents.BREEZE_JUMP, c.point(), 0.5F, 0.9F + 0.1F * frame);
		});
		c.later(50, () -> {
			for (LivingEntity t : c.still(lifted)) {
				c.mark(t, Reactions.Mark.AIRBORNE);
				c.wave(PairCast.shift(0xDDF4FF, 0x9FB0C0, 0.9F), PairCast.mid(t), 14, 0.25);
			}
			c.sound(SoundEvents.WIND_CHARGE_BURST, c.point(), 0.5F, 1.2F);
		});
		c.later(60, () -> {
			for (LivingEntity t : c.still(lifted)) {
				c.push(t, new Vec3(0, -1.6, 0));
			}
		});
		c.later(90, () -> {
			for (LivingEntity t : c.still(lifted)) {
				if (t.onGround()) {
					c.strike(t, 6 * c.power);
					c.wave(PairCast.dust(0xB8A88A, 1.0F), t.position().add(0, 0.2, 0), 14, 0.3);
					c.sound(SoundEvents.MACE_SMASH_GROUND, t.position(), 0.6F, 1.3F);
					c.shake(t.position(), 0.2F, 6);
				}
			}
		});
	}

	/**
	 * Gilded Bolt: a bolt strikes up to three enemies, and each then rolls for luck: half of them are struck by a
	 * second bolt a second later. The look: a gold ring spinning over each target, then gold stars where the luck
	 * lands.
	 */
	@Pair(a = "lightning", b = "luckstrike", name = "Gilded Bolt", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Strikes up to 3 enemies for 5 lightning damage and Slowness I for 3 seconds. Each then rolls for luck: half the time a "
			+ "second bolt falls 1 second later for 5 more damage and sets it alight for 2 seconds.")
	public static void gildedBolt(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 3);
		List<LivingEntity> lucky = new ArrayList<>();
		for (LivingEntity t : struck) {
			c.shock(t, 5 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 3, 0);
			c.bolt(t.position());
			if (c.random() < 0.5) {
				lucky.add(t);
			}
		}
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, c.point(), 0.5F, 1.3F);
		// The roll: a gold ring spins over each target while the luck is drawn.
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(struck)) {
				c.ring(PairCast.shift(0xFFD34A, 0xFFF4C2, 0.8F), t.position().add(0, 2.0, 0), 0.7, 12, frame * 0.6);
			}
			if (frame == 0) {
				c.sound(SoundEvents.NOTE_BLOCK_BELL, c.point(), 0.6F, 1.5F);
			}
		});
		c.later(20, () -> {
			for (LivingEntity t : c.still(lucky)) {
				c.bolt(t.position());
				c.shock(t, 5 * c.power);
				c.ignite(t, 2);
				c.star(PairCast.shift(0xFFF4C2, 0xFFD34A, 1.2F), PairCast.mid(t), 6, 1.2, 0);
				c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, PairCast.mid(t), 0.7F, 0.6F);
			}
		});
	}

	/**
	 * Held Moment: up to four enemies are held in time for three seconds, aged twice, and released all at once with
	 * a blast that reaches those beside them. The look: a clock face over each one with its hand turning, grey ash
	 * drifting on them, then a pale shatter.
	 */
	@Pair(a = "agestone", b = "stasis", name = "Held Moment", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 4 enemies are held for 3 seconds: Slowness III, and each one ages at 1 and 2 seconds, taking 2 withering "
			+ "damage. At 3 seconds time moves on: 6 damage to each held one and to every other enemy within 2 blocks of one.")
	public static void heldMoment(PairCast c) {
		List<LivingEntity> held = PairCast.first(c.enemies(), 4);
		for (LivingEntity t : held) {
			c.effect(t, MobEffects.SLOWNESS, 3, 2);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 0.6F, 0.6F);
		// The clock: each held one has a face, and its hand turns a quarter every 0.75 seconds.
		c.every(15, 5, frame -> {
			for (LivingEntity t : c.still(held)) {
				Vec3 face = PairCast.mid(t);
				c.ring(PairCast.dust(0xB8C8FF, 0.8F), face, 0.9, 14, 0);
				double a = frame * Math.PI / 2;
				c.line(PairCast.dust(0xFFFFFF, 0.7F), face, face.add(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9), 4);
			}
		});
		c.later(20, () -> age(c, held));
		c.later(40, () -> age(c, held));
		// Release: time moves on for them, and the blast takes in everyone round each one.
		c.later(60, () -> {
			Set<LivingEntity> blasted = new LinkedHashSet<>();
			for (LivingEntity t : c.still(held)) {
				blasted.add(t);
				blasted.addAll(c.enemiesNear(PairCast.mid(t), 2 * c.radius));
				c.sphere(PairCast.shift(0xFFFFFF, 0x7FA8FF, 1.2F), PairCast.mid(t), 1.2 * c.radius, 24);
			}
			for (LivingEntity e : blasted) {
				c.hurt(e, 6 * c.power);
			}
			c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, c.point(), 0.9F, 0.7F);
			c.shake(c.point(), 0.3F, 8);
		});
	}

	private static void age(PairCast c, List<LivingEntity> held) {
		for (LivingEntity t : c.still(held)) {
			Vec3 at = PairCast.mid(t);
			c.wither(t, 2 * c.power);
			c.particles(ParticleTypes.ASH, at, 6, 0.3, 0.02);
			c.sound(SoundEvents.ROOTED_DIRT_BREAK, at, 0.5F, 0.8F);
		}
	}
}
