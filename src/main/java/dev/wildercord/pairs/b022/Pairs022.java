package dev.wildercord.pairs.b022;

import dev.wildercord.cast.PairCast;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs022 {
	private Pairs022() {}

	/** How far the Firewind Lane reaches. */
	private static final double LANE = 8;

	/**
	 * Ember Ledger: a brand that keeps accounts. Each target is branded, and a ledger opens that notes every wound it
	 * takes; when it closes, half of the tally comes due again as fire. The look: thin gold tallies rising off each
	 * target while the ledger is open, then a column of fire as the debt is paid.
	 */
	@Pair(a = "cinderbrand", b = "reckoning", name = "Ember Ledger", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Brands up to 4 targets for 3 fire damage each. For 4 seconds the ledger counts every wound they take; then half of "
			+ "each tally comes due again as fire (12 at most), and half of what comes due heals you (6 at most in all).")
	public static void emberLedger(PairCast c) {
		List<LivingEntity> branded = new ArrayList<>(PairCast.first(c.enemies(), 4));
		int n = branded.size();
		double[] tally = new double[n];
		float[] last = new float[n];
		for (int i = 0; i < n; i++) {
			LivingEntity t = branded.get(i);
			c.burn(t, 3 * c.power);
			last[i] = t.getHealth();
			c.ring(PairCast.shift(0xFFD27A, 0x8A2A0A, 1.0F), PairCast.mid(t), 0.8, 14, i);
			c.sound(SoundEvents.FIRECHARGE_USE, PairCast.mid(t), 0.7F, 1.2F);
		}
		// The ledger is open for 4 seconds: every tick it notes the health each target lost.
		c.every(1, 80, frame -> {
			for (int i = 0; i < n; i++) {
				LivingEntity t = branded.get(i);
				if (!c.here(t)) {
					continue;
				}
				float h = t.getHealth();
				if (h < last[i]) {
					tally[i] += last[i] - h;
				}
				last[i] = h;
				if (frame % 5 == 0) {
					Vec3 at = PairCast.mid(t);
					c.line(PairCast.dust(0xFFE9A8, 0.6F), at.add(0, 0.5, 0), at.add(0, 1.4 + tally[i] * 0.02, 0), 3);
				}
			}
		});
		// The ledger closes: the debt comes due.
		c.later(80, () -> {
			double totalDue = 0;
			for (int i = 0; i < n; i++) {
				LivingEntity t = branded.get(i);
				double due = Math.min(12, tally[i] / 2);
				if (due <= 0 || !c.here(t)) {
					continue;
				}
				totalDue += due;
				Vec3 at = PairCast.mid(t);
				c.burn(t, due * c.power);
				c.column(PairCast.shift(0xFFD27A, 0xFF3B1A, 1.2F), at, 0.5, 2.0, 14);
				c.particles(ParticleTypes.FLAME, at, 8, 0.3, 0.05);
			}
			if (totalDue > 0) {
				c.heal(c.caster, Math.min(6, totalDue / 2) * c.power);
				c.spiral(PairCast.dust(0xFFE9A8, 1.0F), PairCast.mid(c.caster), 0.8, 1.6, 2, 16);
				c.sound(SoundEvents.BELL_RESONATE, PairCast.mid(c.caster), 0.6F, 1.3F);
			}
			c.sound(SoundEvents.ANVIL_LAND, c.point(), 0.4F, 1.4F);
		});
	}

	/**
	 * Hourglass Ember: fire poured through glass. Targets burn and are slowed at once, then the sand runs: a steady
	 * drip of fire each second, and the slowness renewed with it. The look: a gold stream of sand falling past each
	 * target, and a ring of embers turning over at each second.
	 */
	@Pair(a = "everburn", b = "tarry", name = "Hourglass Ember", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Sets up to 6 targets alight for 5 seconds, 2 fire damage each, with Slowness I. The sand then runs: each second for "
			+ "4 seconds, 1.5 more fire damage, and the Slowness is renewed for a second.")
	public static void hourglassEmber(PairCast c) {
		List<LivingEntity> glassed = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : glassed) {
			c.burn(t, 2 * c.power);
			c.ignite(t, 5);
			c.effect(t, MobEffects.SLOWNESS, 5, 0);
			c.sound(SoundEvents.SAND_PLACE, PairCast.mid(t), 0.7F, 0.9F);
		}
		// Sand falls through the neck: a thin gold stream past each target, for the first second.
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(glassed)) {
				Vec3 at = PairCast.mid(t);
				c.line(PairCast.dust(0xE8C15A, 0.7F), at.add(0, 1.8, 0), at.add(0, 0.9, 0), 4);
			}
		});
		// Then the glass turns over once a second: a ring of embers, fire and renewed slowness.
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(glassed)) {
				Vec3 at = PairCast.mid(t);
				c.ring(PairCast.shift(0xFFE9A8, 0xB8741A, 0.9F), at.add(0, 1.0, 0), 0.6, 12, frame * 0.5);
				if (frame > 0) {
					c.burn(t, 1.5 * c.power);
					c.effect(t, MobEffects.SLOWNESS, 1, 0);
					c.particles(ParticleTypes.FLAME, at, 3, 0.2, 0.02);
				}
			}
			c.sound(SoundEvents.SAND_FALL, c.point(), 0.4F, 0.8F + 0.1F * frame);
		});
	}

	/**
	 * Maw Feast: a black pit that feeds. It opens on the spot and drags in whoever is near, striking each by how
	 * wounded it already is; its core burns, and every enemy it kills sends a soul back to you. The look: a dark ring
	 * on the ground, violet threads pulling inward, and a soul-streak to you for each death.
	 */
	@Pair(a = "devour", b = "hellmouth", name = "Maw Feast", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Opens a pit of black fire for 3 seconds, drawing in enemies within 3 blocks. Each is struck for 3 void damage plus 1 "
			+ "per tenth of its health missing (up to 6 extra). Each second, an enemy in the core burns for 2. The collapse strikes all "
			+ "of them for 4 more. Each enemy the pit kills heals you 2 (6 at most).")
	public static void mawFeast(PairCast c) {
		Vec3 pit = c.point();
		double r = 3 * c.radius;
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(pit, r), PairCast.MAX_TARGETS);
		for (LivingEntity t : caught) {
			double missing = 1.0 - (double) t.getHealth() / Math.max(1.0, t.getMaxHealth());
			int extra = Math.min(6, (int) (missing * 10));
			c.wither(t, (3 + extra) * c.power);
		}
		c.sound(SoundEvents.PORTAL_TRIGGER, pit, 0.6F, 0.6F);
		Set<LivingEntity> fed = new HashSet<>();
		int healed = 0;
		c.every(4, 15, frame -> {
			c.ring(PairCast.dust(0x1A0B2E, 1.2F), pit.add(0, 0.1, 0), r, 24, frame * 0.3);
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, pit, 0.6);
				c.line(PairCast.shift(0x6B2BD9, 0xFF5A1F, 0.8F), PairCast.mid(t), pit.add(0, 0.5, 0), 3);
				if (frame % 5 == 0 && PairCast.mid(t).distanceTo(pit) < 1.5) {
					c.burn(t, 2 * c.power);
					c.particles(ParticleTypes.SOUL_FIRE_FLAME, PairCast.mid(t), 4, 0.2, 0.02);
				}
			}
			c.sound(SoundEvents.SCULK_CLICKING, pit, 0.5F, 0.8F + 0.05F * frame);
		});
		c.later(60, () -> {
			c.wave(PairCast.shift(0x1A0B2E, 0x6B2BD9, 1.3F), pit, 26, 0.4);
			c.particles(ParticleTypes.SMOKE, pit, 12, 0.6, 0.02);
			c.sound(SoundEvents.GENERIC_EXPLODE, pit, 0.5F, 0.6F);
			c.shake(pit, 0.25F, 6);
			for (LivingEntity t : c.still(caught)) {
				c.wither(t, 4 * c.power);
			}
		});
		// Feeding goes on while the pit lasts (on each step) and once more when it collapses.
		int[] count = {healed};
		c.every(4, 15, frame -> count[0] = feed(c, caught, fed, pit, count[0]));
		c.later(61, () -> feed(c, caught, fed, pit, count[0]));
	}

	/** Each enemy the pit has just killed sends a soul back to the caster, 2 health each, 6 at most. */
	private static int feed(PairCast c, List<LivingEntity> caught, Set<LivingEntity> fed, Vec3 pit, int healed) {
		int count = healed;
		for (LivingEntity t : caught) {
			if (!t.isAlive() && count < 3 && fed.add(t)) {
				count++;
				c.heal(c.caster, 2 * c.power);
				c.line(PairCast.dust(0xD9B3FF, 0.8F), pit.add(0, 0.5, 0), PairCast.mid(c.caster), 3);
				c.sound(SoundEvents.BLAZE_AMBIENT, PairCast.mid(c.caster), 0.4F, 1.6F);
			}
		}
		return count;
	}

	/**
	 * Starrift: a black rift opens above each target, and starfire comes out of it. The rifts tear first; a second
	 * later five motes leave them and seek the enemies round. The look: a violet disc widening in the air, then
	 * gold-and-violet motes streaking down from it.
	 */
	@Pair(a = "riftbolt", b = "starfire", name = "Starrift", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Tears a black rift 5 blocks above each of up to 3 targets: 7 void damage and Darkness for 3 seconds. A second later "
			+ "a mote of starfire flies from a rift to each of up to 5 enemies within 6 blocks of the rifts: 2 fire damage, alight for 3 seconds.")
	public static void starrift(PairCast c) {
		List<LivingEntity> torn = PairCast.first(c.enemies(), 3);
		List<Vec3> rifts = new ArrayList<>();
		for (LivingEntity t : torn) {
			Vec3 rift = PairCast.mid(t).add(0, 5, 0);
			rifts.add(rift);
			c.wither(t, 7 * c.power);
			c.effect(t, MobEffects.DARKNESS, 3, 0);
			c.line(PairCast.shift(0x2A0A4A, 0xB15CFF, 1.0F), rift, PairCast.mid(t), 3);
			c.sound(SoundEvents.PORTAL_TRIGGER, rift, 0.5F, 1.6F);
		}
		// The rifts widen in the air.
		c.every(3, 6, frame -> {
			for (Vec3 rift : rifts) {
				c.disc(PairCast.shift(0x1B0633, 0x7A3BD9, 1.2F), rift, 0.4 + frame * 0.2, 20);
			}
		});
		c.later(20, () -> {
			if (rifts.isEmpty()) {
				return;
			}
			Vec3 centre = average(rifts);
			List<LivingEntity> foes = PairCast.first(c.enemiesNear(centre, 6 * c.radius), 5);
			for (Vec3 rift : rifts) {
				c.wave(PairCast.dust(0xB15CFF, 1.0F), rift, 16, 0.3);
			}
			c.every(3, foes.size(), k -> {
				LivingEntity f = foes.get(k);
				if (!c.here(f)) {
					return;
				}
				Vec3 from = rifts.get(k % rifts.size());
				Vec3 to = PairCast.mid(f);
				c.line(PairCast.shift(0xFFE27A, 0xB15CFF, 0.8F), from, to, 2);
				c.burn(f, 2 * c.power);
				c.ignite(f, 3);
				c.sound(SoundEvents.BLAZE_SHOOT, to, 0.5F, 1.4F);
			});
		});
	}

	/**
	 * Second Dawn: the flame keeps a record and turns back time. Allies are wreathed in healing fire; the first one to
	 * fall low is returned to where it stood five seconds before. The look: gold rings marking each ally's place every
	 * second, then a pale blue thread running back to the spot, and a burst where the ally fell.
	 */
	@Pair(a = "phoenix_pyre", b = "rewind", name = "Second Dawn", element = "time", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Wreathes up to 6 allies within 6 blocks in 6 seconds of Regeneration I and Fire Resistance. Each second the flame "
			+ "records their place and health. The first ally to fall under 35% health is moved back to where it stood 5 seconds "
			+ "ago and healed to its old health, and enemies within 3 blocks of the fall take 4.")
	public static void secondDawn(PairCast c) {
		List<LivingEntity> wreathed = PairCast.first(c.alliesNear(c.point(), 6), 6);
		int secs = Math.max(1, (int) Math.round(6 * c.duration));
		Map<LivingEntity, List<double[]>> past = new HashMap<>();
		Set<LivingEntity> turned = new HashSet<>();
		for (LivingEntity a : wreathed) {
			c.effect(a, MobEffects.REGENERATION, 6, 0);
			c.effect(a, MobEffects.FIRE_RESISTANCE, 6, 0);
			past.put(a, new ArrayList<>());
		}
		// Every second, the record: each ally's place and health, with a gold ring on its feet.
		c.every(20, secs, s -> {
			for (LivingEntity a : c.still(wreathed)) {
				Vec3 at = a.position();
				past.get(a).add(new double[] {at.x, at.y, at.z, a.getHealth()});
				c.ring(PairCast.dust(0xFFD27A, 0.9F), PairCast.mid(a), 0.9, 14, s * 0.6);
			}
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, c.point(), 0.4F, 0.9F + 0.1F * s);
		});
		// Checked every tenth of a second, so a fall is caught as it happens.
		c.every(2, secs * 10, f -> {
			int sec = f / 10;
			for (LivingEntity a : c.still(wreathed)) {
				if (turned.contains(a) || a.getHealth() >= a.getMaxHealth() * 0.35F) {
					continue;
				}
				turned.add(a);
				List<double[]> record = past.get(a);
				if (record == null || record.isEmpty()) {
					continue;
				}
				double[] old = record.get(Math.max(0, Math.min(record.size() - 1, sec - 5)));
				Vec3 fell = PairCast.mid(a);
				Vec3 back = new Vec3(old[0], old[1], old[2]);
				c.line(PairCast.shift(0xFFE9A8, 0x9FD8FF, 1.0F), fell, back.add(0, a.getBbHeight() / 2, 0), 2);
				c.blink(a, back);
				if (old[3] > a.getHealth()) {
					c.heal(a, old[3] - a.getHealth());
				}
				c.ring(PairCast.dust(0x9FD8FF, 1.0F), back.add(0, 0.2, 0), 1.0, 18, 0);
				c.sound(SoundEvents.PORTAL_TRIGGER, back, 0.6F, 1.3F);
				c.wave(PairCast.shift(0xFFD27A, 0x9FD8FF, 1.1F), fell, 18, 0.3);
				for (LivingEntity e : c.enemiesNear(fell, 3)) {
					c.burn(e, 4 * c.power);
				}
			}
		});
	}

	/**
	 * Firewind Lane: a hot gust cut along the way you face, and a lane that stays hot. The lane is a visible strip of
	 * fire-wind; it sears anything in it each second for four seconds. The look: an orange streak laid along the
	 * ground, with sparks and smoke rising off it while it burns.
	 */
	@Pair(a = "searing_edge", b = "windcut", name = "Firewind Lane", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A searing gust cuts an 8-block lane, 1.5 blocks wide, the way you face from where it lands. Up to 8 enemies it reaches take "
			+ "4 wind damage and are shoved. For 4 seconds the lane burns: each second, every enemy in it takes 2 fire damage and is set alight.")
	public static void firewindLane(PairCast c) {
		Vec3 from = c.point();
		Vec3 along = flatDir(c);
		double width = 1.5 * c.radius;
		Vec3 end = from.add(along.scale(LANE));
		c.every(5, 16, frame -> {
			Vec3 lift = new Vec3(0, 0.5, 0);
			c.line(PairCast.shift(0xFFB347, 0xFF3B1A, 0.9F), from.add(lift), end.add(lift), 2);
			if (frame % 2 == 0) {
				c.particles(ParticleTypes.FLAME, from.add(along.scale(LANE * (frame % 16) / 16.0)).add(0, 0.4, 0), 2, 0.3, 0.02);
				c.particles(ParticleTypes.SMOKE, from.add(along.scale(LANE * (frame % 16) / 16.0)).add(0, 1.0, 0), 1, 0.2, 0.01);
			}
			if (frame == 0) {
				c.wave(PairCast.dust(0xFFE9A8, 1.0F), from, 16, 0.3);
				c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, from, 0.6F, 1.3F);
			}
		});
		c.every(20, 4, s -> {
			Set<LivingEntity> seared = new LinkedHashSet<>();
			if (s == 0) {
				seared.addAll(PairCast.first(c.enemies(), PairCast.MAX_TARGETS));
			}
			seared.addAll(inLane(c, from, along, width));
			for (LivingEntity e : seared) {
				if (!c.here(e)) {
					continue;
				}
				if (s == 0) {
					c.strike(e, 4 * c.power);
					c.push(e, along.scale(0.7).add(0, 0.25, 0));
				}
				c.burn(e, 2 * c.power);
				c.ignite(e, 4);
				c.particles(ParticleTypes.FLAME, PairCast.mid(e), 6, 0.3, 0.05);
			}
			c.sound(SoundEvents.FIRE_EXTINGUISH, from, 0.3F, 1.5F + 0.1F * s);
		});
	}

	/** The enemies whose middle is inside the lane (within {@code width} of its line, from its start to its end). */
	private static List<LivingEntity> inLane(PairCast c, Vec3 from, Vec3 along, double width) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : c.enemiesNear(from.add(along.scale(LANE / 2)), LANE / 2 + 1.5)) {
			Vec3 rel = PairCast.mid(e).subtract(from);
			double forward = rel.dot(along);
			Vec3 side = rel.subtract(along.scale(forward));
			if (forward >= -0.5 && forward <= LANE + 0.5 && side.length() <= width + 0.5) {
				out.add(e);
			}
		}
		return out;
	}

	/**
	 * Warmstride: a short dash that leaves a warm track. The caster is carried along the way they face until a wall
	 * stops them, and the cold can't take hold while the warmth lasts. The look: warm wisps wound round the caster,
	 * a gust trail on the way, and a thaw ring where they land.
	 */
	@Pair(a = "gale_mantle", b = "warm_cloak", name = "Warmstride", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "Dashes you up to 8 blocks the way you face, stopping short of walls. For 10 seconds your freezing is wiped every second, "
			+ "so the cold barely takes hold. Where you land, allies within 2 blocks thaw and get Regeneration I for 4 seconds.")
	public static void warmstride(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 along = flatDir(c);
		Vec3 goal = start.add(along.scale(8));
		BlockHitResult wall = c.level.clip(new ClipContext(start.add(0, 1, 0), goal.add(0, 1, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		Vec3 stop = wall.getType() == HitResult.Type.MISS ? goal : wall.getLocation().subtract(along.scale(0.6));
		// The wind-up: warm wisps wound round the caster for 0.4 seconds.
		c.every(2, 4, frame -> {
			Vec3 at = PairCast.mid(self);
			c.spiral(PairCast.shift(0xFFB36B, 0xFFF4C2, 0.8F), at.add(0, -0.4, 0), 0.9, 1.6, 1.0, 12);
			c.sound(SoundEvents.FIRECHARGE_USE, at, 0.3F, 1.6F + 0.1F * frame);
		});
		// The dash: the caster is set down at the far end of the track, and a gust trail is left behind.
		c.later(8, () -> {
			Vec3 before = self.position();
			c.blink(self, stop);
			Vec3 landed = self.position();
			c.line(PairCast.shift(0xFFF4C2, 0xFFB36B, 1.0F), before.add(0, 1, 0), landed.add(0, 1, 0), 1);
			c.line(ParticleTypes.GUST, before.add(0, 1, 0), landed.add(0, 1, 0), 2);
			c.punch(0.2F);
			c.sound(SoundEvents.WIND_CHARGE_BURST, landed, 0.7F, 1.2F);
			// Where they land: the warmth thaws allies round them.
			for (LivingEntity a : c.alliesNear(landed, 2)) {
				a.setTicksFrozen(0);
				c.effect(a, MobEffects.REGENERATION, 4, 0);
			}
			c.wave(PairCast.shift(0xFFB36B, 0xFFF4C2, 1.0F), landed.add(0, 0.2, 0), 20, 0.3);
			c.ring(PairCast.dust(0xFFE7A8, 0.9F), landed.add(0, 0.15, 0), 2, 20, 0);
		});
		// Warm while it lasts: the cold is wiped off the caster every second.
		int pulses = Math.max(1, (int) Math.round(10 * c.duration));
		c.every(20, pulses, frame -> {
			self.setTicksFrozen(0);
			if (frame > 0) {
				c.particles(ParticleTypes.CLOUD, PairCast.mid(self), 2, 0.3, 0.01);
			}
		});
	}

	/**
	 * Knotburst: targets are drawn together by a countdown, then the knot bursts. The look: a ring tightening round
	 * the marks as they are pulled in, gold threads joining them, then a blast of white-gold light and throw.
	 */
	@Pair(a = "countdown", b = "explode", name = "Knotburst", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Marks up to 4 targets and draws them into a knot for 1.5 seconds. Then the knot bursts: 6 damage to each mark and every enemy "
			+ "within 3 blocks of its middle, each thrown outward. Each enemy is hit once, however many marks are near.")
	public static void knotburst(PairCast c) {
		List<LivingEntity> marked = new ArrayList<>(PairCast.first(c.enemies(), 4));
		double r = 3 * c.radius;
		c.every(10, 4, frame -> {
			List<LivingEntity> still = new ArrayList<>(c.still(marked));
			Vec3 mid = knotCentre(still, c.point());
			if (frame < 3) {
				for (LivingEntity t : still) {
					c.pullTo(t, mid, 0.8);
				}
				for (int i = 1; i < still.size(); i++) {
					c.line(PairCast.dust(0xFFD27A, 0.7F), PairCast.mid(still.get(i - 1)), PairCast.mid(still.get(i)), 3);
				}
				c.ring(PairCast.shift(0xFFF4C2, 0xFF8A3D, 1.0F), mid.add(0, 0.2, 0), 3.0 - frame * 0.8, 22, frame * 0.5);
				c.sound(SoundEvents.NOTE_BLOCK_HAT, mid, 0.6F, 1.0F + 0.2F * frame);
				return;
			}
			Set<LivingEntity> struck = new LinkedHashSet<>(still);
			struck.addAll(c.enemiesNear(mid, r));
			c.wave(PairCast.shift(0xFFF4C2, 0x3A1A0A, 1.2F), mid, 22, 0.35);
			c.particles(ParticleTypes.EXPLOSION, mid, 1, 0, 0);
			c.sound(SoundEvents.GENERIC_EXPLODE, mid, 0.6F, 1.3F);
			c.shake(mid, 0.3F, 6);
			for (LivingEntity e : struck) {
				if (PairCast.mid(e).distanceTo(mid) <= r + 0.5 || marked.contains(e)) {
					c.hurt(e, 6 * c.power);
					c.knockFrom(e, mid, 0.9, 0.4);
				}
			}
		});
	}

	/** The middle of the knot: the average middle of the marks still standing, or {@code fallback} if none. */
	private static Vec3 knotCentre(List<LivingEntity> still, Vec3 fallback) {
		if (still.isEmpty()) {
			return fallback;
		}
		Vec3 sum = Vec3.ZERO;
		for (LivingEntity t : still) {
			sum = sum.add(PairCast.mid(t));
		}
		return sum.scale(1.0 / still.size());
	}

	/**
	 * Hot Ascent: the caster crouches in heat, is launched up and forward, and comes down in a flash of fire. The look:
	 * shimmering rings round the caster, a trail of flame on the way up, then a disc of fire where they land.
	 */
	@Pair(a = "flashfire", b = "launch", name = "Hot Ascent", element = "fire", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Crouch for half a second in a shimmer of heat, then leap up and forward, falling slowly for 3 seconds. Where you land: "
			+ "4 fire damage and alight for 4 seconds to every enemy within 3 blocks, and allies there are thawed.")
	public static void hotAscent(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 along = flatDir(c);
		// The crouch: heat shimmers round the caster for half a second.
		c.every(4, 3, frame -> {
			Vec3 at = PairCast.mid(self);
			c.ring(PairCast.shift(0xFFB347, 0xFFF4C2, 0.9F), at.add(0, -0.6, 0), 0.9 - frame * 0.2, 14, frame * 0.4);
			c.particles(ParticleTypes.FLAME, at, 6, 0.4, 0.02);
			c.sound(SoundEvents.FIRECHARGE_USE, at, 0.4F, 0.8F + 0.2F * frame);
		});
		// The launch: up and forward, with slow falling; the flame trails as it goes.
		boolean[] landed = {false};
		c.later(10, () -> {
			c.push(self, along.scale(0.6).add(0, 1.1, 0));
			c.effect(self, MobEffects.SLOW_FALLING, 3, 0);
			c.sound(SoundEvents.WIND_CHARGE_BURST, PairCast.mid(self), 0.8F, 1.0F);
			c.every(2, 40, frame -> {
				if (landed[0]) {
					return;
				}
				Vec3 at = PairCast.mid(self);
				c.particles(ParticleTypes.FLAME, at, 2, 0.2, 0.01);
				if (frame >= 3 && self.onGround()) {
					landed[0] = true;
					flash(c, self.position());
				}
			});
		});
	}

	/** The flash where the caster lands: fire to every enemy within 3 blocks, and allies there are thawed. */
	private static void flash(PairCast c, Vec3 spot) {
		double r = 3 * c.radius;
		c.disc(PairCast.shift(0xFFF4C2, 0xFF6A1A, 1.2F), spot.add(0, 0.1, 0), r, 28);
		c.wave(PairCast.dust(0xFFB347, 1.2F), spot.add(0, 0.2, 0), 20, 0.35);
		c.particles(ParticleTypes.FLAME, spot, 20, 0.8, 0.05);
		c.sound(SoundEvents.FIRECHARGE_USE, spot, 0.9F, 0.8F);
		c.shake(spot, 0.25F, 6);
		for (LivingEntity e : c.enemiesNear(spot, r)) {
			c.burn(e, 4 * c.power);
			c.ignite(e, 4);
		}
		for (LivingEntity a : c.alliesNear(spot, r)) {
			a.setTicksFrozen(0);
		}
	}

	/**
	 * Hangfire: enemies are hung in the air, then fire rains down on them. The look: a pale wind column holding each
	 * target still, then a flame burst and a streak of fire falling onto it.
	 */
	@Pair(a = "levitate", b = "skyburst", name = "Hangfire", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Holds up to 3 targets (not bosses) in the air for 3 seconds, so they can't fall. Then fire rains on each: 6 damage, alight for 3 "
			+ "seconds, and 3 damage to every other enemy within 3 blocks of it.")
	public static void hangfire(PairCast c) {
		List<LivingEntity> hung = PairCast.first(c.enemies(), 3);
		int hang = c.ticks(3);
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 0.8F, 1.5F);
		// Held still: each target's fall is cancelled every tick, and its drift stopped.
		c.every(1, hang, frame -> {
			for (LivingEntity t : c.still(hung)) {
				if (!c.movable(t)) {
					continue;
				}
				t.setDeltaMovement(0.0, 0.08, 0.0);
				if (frame % 4 == 0) {
					Vec3 at = PairCast.mid(t);
					c.line(PairCast.dust(0xCFEFFF, 0.7F), at.add(0, -0.8, 0), at.add(0, 0.6, 0), 3);
					c.particles(ParticleTypes.CLOUD, at, 2, 0.3, 0.01);
				}
			}
		});
		// The burst: flame falls onto each held target, and the fire spreads to what stands beside it.
		c.later(hang, () -> {
			for (LivingEntity t : c.still(hung)) {
				Vec3 at = PairCast.mid(t);
				c.burn(t, 6 * c.power);
				c.ignite(t, 3);
				c.line(PairCast.shift(0xFFD27A, 0xFF3B1A, 0.9F), at.add(0, 4, 0), at, 4);
				c.sphere(PairCast.shift(0xFFB347, 0x2A0A00, 1.2F), at, 1.2, 26);
				c.particles(ParticleTypes.FLAME, at.add(0, 2, 0), 14, 0.9, 0.08);
				for (LivingEntity e : c.enemiesNear(at, 3)) {
					if (!hung.contains(e)) {
						c.burn(e, 3 * c.power);
						c.ignite(e, 2);
					}
				}
			}
			c.sound(SoundEvents.GENERIC_EXPLODE, c.point(), 0.5F, 1.4F);
		});
	}

	/** The horizontal way the caster was facing, as a unit vector (east if they face straight up or down). */
	private static Vec3 flatDir(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	/** The average of a few points. */
	private static Vec3 average(List<Vec3> points) {
		Vec3 sum = Vec3.ZERO;
		for (Vec3 p : points) {
			sum = sum.add(p);
		}
		return sum.scale(1.0 / points.size());
	}
}
