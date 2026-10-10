package dev.wildercord.pairs.b012;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs012 {
	private Pairs012() {}

	/**
	 * Dark Star Anchor: a void well drags the enemies round it in over a second; then starlight threads hold each of
	 * them to the well, and every tug back costs a little, until the well implodes. The look: a dark sphere pulling
	 * in, white threads taut to its centre, then a flash of starlight.
	 */
	@Pair(a = "gravity_well", b = "starlight_tether", name = "Dark Star Anchor", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Drags every enemy within 6 blocks towards the point over a second (not bosses), then tethers each with "
			+ "starlight for 4 seconds: one that strays 2.5 blocks is pulled back and takes 2 damage (once a second at most). "
			+ "At the end the well implodes: 4 damage to each.")
	public static void darkStarAnchor(PairCast c) {
		Vec3 well = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(well, 6 * c.radius), PairCast.MAX_TARGETS);
		if (caught.isEmpty()) {
			return;
		}
		c.sound(SoundEvents.BEACON_POWER_SELECT, well, 0.9F, 0.5F);
		c.sphere(PairCast.shift(0x2A1450, 0xFFF6C8, 1.2F), well, 2.2, 36);
		// Beat one: the pull, five frames over the first second.
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, well, 1.1);
				c.line(PairCast.dust(0x6A4BC4, 0.6F), PairCast.mid(t), well, 2);
			}
			c.ring(PairCast.dust(0x1B0B33, 1.2F), well, 6 * c.radius * (1 - frame * 0.15), 24, frame * 0.4);
		});
		// Beat two: the tether. Each second, a target that has strayed is drawn back and hurt; the threads stay taut.
		int tugs = Math.max(1, (int) Math.round(4 * c.duration));
		int implode = 20 + tugs * 20;
		c.later(20, () -> {
			c.every(20, tugs, frame -> {
				for (LivingEntity t : c.still(caught)) {
					if (PairCast.mid(t).distanceTo(well) > 2.5 * c.radius) {
						c.pullTo(t, well, 1.5);
						c.hurt(t, 2 * c.power);
					}
				}
			});
			c.every(5, tugs * 4, frame -> {
				for (LivingEntity t : c.still(caught)) {
					c.line(PairCast.dust(0xFFF6C8, 0.4F), well, PairCast.mid(t), 2);
				}
			});
		});
		// Beat three: the implosion, starlight bursting out of the dark.
		c.later(implode, () -> {
			for (LivingEntity t : c.still(caught)) {
				c.hurt(t, 4 * c.power);
			}
			c.sphere(PairCast.shift(0xFFF6C8, 0x2A1450, 1.4F), well, 2.5 * c.radius, 40);
			c.sound(SoundEvents.ENDERMAN_SCREAM, well, 0.8F, 0.5F);
			c.shake(well, 0.3F, 8);
		});
	}

	/**
	 * Dusk Hounds: two spirit hounds rise from the shadow of each guarded ally, pacing round it. Each second a hound
	 * bites the nearest enemy close to its ally, and where that enemy stands in dim light the bite is deeper.
	 */
	@Pair(a = "shades", b = "summon", name = "Dusk Hounds", element = "void", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Spirit hounds guard up to 4 allies within 8 blocks of where it lands, for 12 seconds. Each second a hound "
			+ "bites an enemy within 3 blocks of its ally for 3 damage and shadows it; where the enemy stands in light 7 or "
			+ "less, the bite is 5.")
	public static void duskHounds(PairCast c) {
		Vec3 point = c.point();
		List<LivingEntity> guarded = PairCast.first(c.alliesNear(point, 8 * c.radius), 4);
		if (guarded.isEmpty()) {
			return;
		}
		c.sound(SoundEvents.WARDEN_NEARBY_CLOSE, point, 0.8F, 0.7F);
		for (LivingEntity g : guarded) {
			c.column(PairCast.shift(0x9B7BFF, 0x05030D, 0.9F), PairCast.mid(g), 0.9, 1.8, 20);
		}
		int beats = Math.max(1, (int) Math.round(12 * c.duration));
		c.every(20, beats, frame -> {
			for (LivingEntity g : c.still(guarded)) {
				Vec3 at = PairCast.mid(g);
				// Two hounds pace round their ally.
				c.ring(PairCast.dust(0x3B2A66, 0.9F), at, 1.2, 12, frame * 0.7);
				c.ring(PairCast.dust(0x9B7BFF, 0.6F), at, 1.2, 8, frame * 0.7 + Math.PI);
				LivingEntity bitten = c.nearestEnemy(at, 3 * c.radius, null);
				if (bitten == null) {
					continue;
				}
				Vec3 bite = PairCast.mid(bitten);
				boolean dim = c.level.getMaxLocalRawBrightness(BlockPos.containing(bite)) <= 7;
				c.hurt(bitten, (dim ? 5 : 3) * c.power);
				c.mark(bitten, Reactions.Mark.SHADOWED);
				c.zigzag(PairCast.dust(0x2B1B4A, 0.7F), at, bite, 0.2, 3);
				c.sound(SoundEvents.SCULK_CLICKING, bite, 0.6F, dim ? 0.7F : 1.1F);
			}
		});
	}

	/**
	 * Twin Mirror: the spell lands twice. The first copy blesses the allies at the point with a frost shield; a second
	 * later the point is mirrored through you and the same blessing lands on the far side, a frozen line between them.
	 */
	@Pair(a = "mirrorfrost", b = "twin_star", name = "Twin Mirror", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Heals each ally within 3 blocks of where it lands 4 and shields them 3 for 6 seconds. A second later the "
			+ "point is mirrored through you and the same lands there: allies within 3 blocks of that copy get the same.")
	public static void twinMirror(PairCast c) {
		Vec3 first = c.point();
		Vec3 self = c.caster.position();
		Vec3 twin = new Vec3(2 * self.x - first.x, first.y, 2 * self.z - first.z);
		c.sound(SoundEvents.GLASS_PLACE, first, 0.9F, 1.4F);
		c.line(PairCast.dust(0xDFF8FF, 0.5F), first, twin, 1.5);
		mirror(c, first);
		c.later(20, () -> {
			c.sound(SoundEvents.BEACON_ACTIVATE, twin, 0.8F, 1.6F);
			mirror(c, twin);
		});
	}

	private static void mirror(PairCast c, Vec3 at) {
		for (LivingEntity a : PairCast.first(c.alliesNear(at, 3 * c.radius), PairCast.MAX_TARGETS)) {
			c.heal(a, 4 * c.power);
			c.absorb(a, 3 * c.power, 6);
		}
		c.ring(PairCast.shift(0xE8FBFF, 0x7FD4FF, 0.9F), at, 3 * c.radius, 28, 0);
		c.particles(ParticleTypes.SNOWFLAKE, at, 12, 1.2, 0.1);
	}

	/**
	 * Last Stitch: the most hurt ally in reach is healed, and a golden thread is stitched to it for four seconds. The
	 * health it loses while stitched is counted, and when the stitch ends that much heals back at once.
	 */
	@Pair(a = "stitchtime", b = "worst_first", name = "Last Stitch", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Heals the most hurt ally within 8 blocks of where it lands (you too) for 4, and stitches them for 4 seconds: "
			+ "the health they lose meanwhile is counted, and when the stitch ends all of it heals back at once (12 at most).")
	public static void lastStitch(PairCast c) {
		LivingEntity ward = mostHurt(c.alliesNear(c.point(), 8 * c.radius));
		if (ward == null) {
			return;
		}
		c.heal(ward, 4 * c.power);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(ward), 0.9F, 1.3F);
		double[] lost = {0};
		float[] last = {ward.getHealth()};
		int frames = Math.max(1, (int) Math.round(20 * c.duration));
		c.every(4, frames, frame -> {
			if (!c.here(ward)) {
				return;
			}
			float now = ward.getHealth();
			if (now < last[0]) {
				lost[0] += last[0] - now;
			}
			last[0] = now;
			c.helix(PairCast.dust(0xF2D27A, 0.6F), PairCast.dust(0x8BE8B0, 0.6F), PairCast.mid(ward), 0.5, 1.4, 1, 12);
		});
		// The stitch lets go: the counted health comes back in one warm burst.
		c.later(4 * frames, () -> {
			if (!c.here(ward) || lost[0] <= 0) {
				return;
			}
			c.heal(ward, Math.min(12 * c.power, lost[0]));
			c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(ward), 10, 0.5, 0.1);
			c.sound(SoundEvents.PLAYER_LEVELUP, PairCast.mid(ward), 0.5F, 1.6F);
		});
	}

	private static LivingEntity mostHurt(List<LivingEntity> allies) {
		LivingEntity worst = null;
		double ratio = Double.MAX_VALUE;
		for (LivingEntity a : allies) {
			double r = a.getHealth() / a.getMaxHealth();
			if (a.isAlive() && r < ratio) {
				ratio = r;
				worst = a;
			}
		}
		return worst;
	}

	/**
	 * Tidal Cistern: a cistern wells up where it lands, a column of mana-blue water ringed by a spiral. Each player who
	 * stands in it regains more mana every second they stay, the longer the more, and the tide falls back when they leave.
	 */
	@Pair(a = "manatide", b = "manawell", name = "Tidal Cistern", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A 3-block cistern wells up for 10 seconds. Each player standing in it regains 2 mana a second, rising by 1 "
			+ "for every second they stay (4 at most); stepping out resets the tide. Mobs gain nothing.")
	public static void tidalCistern(PairCast c) {
		Vec3 well = c.point();
		double r = 3 * c.radius;
		Map<LivingEntity, Integer> stayed = new IdentityHashMap<>();
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, well, 0.9F, 0.8F);
		c.column(PairCast.shift(0x3DE7FF, 0x6B3DFF, 0.9F), well, r, 2.5, 30);
		int beats = Math.max(1, (int) Math.round(10 * c.duration));
		c.every(20, beats, frame -> {
			List<LivingEntity> inside = c.alliesNear(well, r);
			stayed.keySet().retainAll(inside);
			c.ring(PairCast.dust(0x5FD8FF, 0.8F), well, r, 24, frame * 0.5);
			c.spiral(PairCast.dust(0xA58BFF, 0.6F), well, r * 0.6, 2.2, 1.5, 18);
			for (LivingEntity a : inside) {
				if (!(a instanceof Player p)) {
					continue;
				}
				int seconds = stayed.merge(a, 1, Integer::sum);
				int gain = Math.min(4, 1 + seconds);
				float have = Spellbooks.mana(p);
				Spellbooks.setMana(p, Math.min(Mana.max(p), have + (float) (gain * c.power)));
				c.particles(ParticleTypes.ENCHANT, PairCast.mid(p), 4, 0.4, 0.2);
			}
			c.sound(SoundEvents.BUBBLE_POP, well, 0.5F, 1.2F);
		});
	}

	/**
	 * Red Kettle: each target is shut in a bubble of boiling blood. It is held in place while it boils, and at the end
	 * the bubble bursts into steam that lifts and blinds everything round it.
	 */
	@Pair(a = "bloodboil", b = "seethe", name = "Red Kettle", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Each target takes 3 damage and is shut in a bubble of boiling blood for 2 seconds: held to its spot, Slowness III,"
			+ "1 fire damage every half second, and bleeding. The bubble bursts into steam: 4 damage to every enemy within "
			+ "2.5 blocks, lifted, and blinded for 2 seconds.")
	public static void redKettle(PairCast c) {
		List<LivingEntity> boiled = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		if (boiled.isEmpty()) {
			return;
		}
		Vec3[] anchor = new Vec3[boiled.size()];
		for (int i = 0; i < boiled.size(); i++) {
			LivingEntity t = boiled.get(i);
			anchor[i] = t.position();
			c.hurt(t, 3 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 2, 2);
			c.mark(t, Reactions.Mark.BLEEDING);
			c.sphere(PairCast.shift(0xFF6A3D, 0x7A0014, 0.9F), PairCast.mid(t), 1.1, 24);
		}
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, PairCast.mid(boiled.get(0)), 0.8F, 0.9F);
		// The boil: four frames half a second apart, each holding the target at its spot and burning it a little.
		c.every(10, 4, frame -> {
			for (int i = 0; i < boiled.size(); i++) {
				LivingEntity t = boiled.get(i);
				if (!c.here(t)) {
					continue;
				}
				c.pullTo(t, anchor[i], 0.5);
				c.burn(t, 1 * c.power);
				c.sphere(PairCast.dust(0xFF2A1A, 0.8F), PairCast.mid(t), 1.1 - frame * 0.1, 18);
			}
		});
		// The burst: steam, a lift and a blindness for everything within reach.
		c.later(40, () -> {
			for (Vec3 at : anchor) {
				for (LivingEntity e : c.enemiesNear(at, 2.5 * c.radius)) {
					c.hurt(e, 4 * c.power);
					c.lift(e, 0.9);
					c.effect(e, MobEffects.BLINDNESS, 2, 0);
				}
				c.sphere(PairCast.dust(0xE8E8E8, 1.3F), at.add(0, 1, 0), 2.4 * c.radius, 40);
				c.column(PairCast.dust(0xD8D8D8, 1.0F), at, 1.2, 3, 30);
			}
			c.sound(SoundEvents.FIRE_EXTINGUISH, anchor[0], 1.0F, 0.8F);
			c.shake(anchor[0], 0.2F, 6);
		});
	}

	/**
	 * Quiet Cleaver: three unseen slashes, a tenth of a second apart, each drawn as a red stroke across the target.
	 * The third cuts deepest, deeper still against one that has turned from you, and spills into the enemies beside it.
	 */
	@Pair(a = "cleave", b = "dismantle", name = "Quiet Cleaver", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Three unseen slashes a tenth of a second apart. The first two each do 2 damage through armour. The third cuts deeper: 4 damage "
			+ "plus 10% of the target's max health (6 at most), and 4 more if it has turned away from you. Enemies within "
			+ "2.5 blocks take 2 each (three at most).")
	public static void quietCleaver(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		if (struck.isEmpty()) {
			return;
		}
		c.every(2, 3, slash -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				double angle = 0.6 + slash * 0.9;
				Vec3 across = new Vec3(Math.cos(angle), 0, Math.sin(angle));
				c.line(PairCast.dust(0xB0001A, 0.8F), at.subtract(across.scale(1.0)).add(0, 0.5, 0),
					at.add(across.scale(1.0)).add(0, -0.3, 0), 6);
				c.particles(ParticleTypes.CRIT, at, 6, 0.3, 0.3);
				if (slash < 2) {
					c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, at, 0.6F, 1.3F + slash * 0.2F);
					c.hurt(t, 2 * c.power);
					continue;
				}
				double deep = 4 + Math.min(6, 0.1 * t.getMaxHealth()) + (turnedAway(c, t) ? 4 : 0);
				c.sound(SoundEvents.SHULKER_BULLET_HIT, at, 0.8F, 0.7F);
				c.hurt(t, deep * c.power);
				int beside = 0;
				for (LivingEntity n : c.enemiesNear(at, 2.5 * c.radius)) {
					if (n == t) {
						continue;
					}
					if (beside >= 3) {
						break;
					}
					beside++;
					c.line(PairCast.dust(0x7A0010, 0.6F), at, PairCast.mid(n), 4);
					c.hurt(n, 2 * c.power);
				}
			}
		});
	}

	/** Whether {@code t} has turned its back on the caster (its look points away from them). */
	private static boolean turnedAway(PairCast c, LivingEntity t) {
		Vec3 toYou = c.caster.position().subtract(t.position());
		return toYou.lengthSqr() > 1.0E-4 && t.getLookAngle().dot(toYou.normalize()) < 0;
	}

	/**
	 * Carmine Drift: a cloud of crimson mist that rolls on along the way the breath was blown, stopping at walls. The
	 * enemies in it bleed; the allies in it are healed by it.
	 */
	@Pair(a = "crimson_mist", b = "dragon_breath", name = "Carmine Drift", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A 3-block cloud of crimson mist rolls on along the way you blew it, stopping at walls, for 5 seconds. "
			+ "Enemies in it take 2 damage and bleed each second; allies in it heal 1 each second.")
	public static void carmineDrift(PairCast c) {
		Vec3 flat = new Vec3(c.dir().x, 0, c.dir().z);
		Vec3 heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3[] cloud = {c.point().add(0, 1, 0)};
		boolean[] walled = {false};
		c.sound(SoundEvents.WIND_CHARGE_THROW, cloud[0], 0.8F, 0.6F);
		int beats = Math.max(1, (int) Math.round(10 * c.duration));
		c.every(10, beats, frame -> {
			if (frame > 0 && !walled[0]) {
				Vec3 next = cloud[0].add(heading.scale(1.25));
				BlockHitResult wall = c.level.clip(
					new ClipContext(cloud[0], next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster));
				if (wall.getType() == HitResult.Type.MISS) {
					cloud[0] = next;
				} else {
					walled[0] = true;
				}
			}
			Vec3 at = cloud[0];
			c.sphere(PairCast.dust(0xB0001A, 0.9F), at, 3 * c.radius, 24);
			c.sphere(PairCast.shift(0x3A0A4A, 0xC8102E, 1.0F), at, 1.6 * c.radius, 14);
			if (frame % 2 == 0) {
				for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
					c.hurt(e, 2 * c.power);
					c.mark(e, Reactions.Mark.BLEEDING);
				}
				for (LivingEntity a : c.alliesNear(at, 3 * c.radius)) {
					c.heal(a, 1 * c.power);
				}
			}
		});
	}

	/**
	 * Skipped Beat: the target's heart is pounded into a broken rhythm. A heartbeat pulses through six seconds: a
	 * stumble at two, a lurch at four, and at six a stop: the target is held still and the stop shocks all round it.
	 */
	@Pair(a = "doomclock", b = "heartstopper", name = "Skipped Beat", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "5 damage, then for 6 seconds the heart skips: at 2 seconds it stumbles (knocked back), at 4 seconds it lurches "
			+ "(Slowness III for 1 second), and at 6 seconds it stops: held still for 1.5 seconds, and every enemy within 3 "
			+ "blocks of it takes 4 damage.")
	public static void skippedBeat(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : hit) {
			c.hurt(t, 5 * c.power);
		}
		if (hit.isEmpty()) {
			return;
		}
		c.every(10, 12, frame -> {
			for (LivingEntity t : c.still(hit)) {
				double pulse = frame % 2 == 0 ? 0.9 : 0.6;
				c.sphere(PairCast.dust(0xFF2A4A, 0.9F), PairCast.mid(t), pulse, 12);
			}
			c.sound(SoundEvents.WARDEN_HEARTBEAT, PairCast.mid(hit.get(0)), 0.5F, 1.0F + frame * 0.02F);
		});
		c.later(40, () -> {
			for (LivingEntity t : c.still(hit)) {
				c.knockFrom(t, c.origin(), 0.7, 0.25);
				c.particles(ParticleTypes.DAMAGE_INDICATOR, PairCast.mid(t), 3, 0.3, 0.1);
			}
		});
		c.later(80, () -> {
			for (LivingEntity t : c.still(hit)) {
				c.effect(t, MobEffects.SLOWNESS, 1, 2);
			}
		});
		c.later(120, () -> {
			List<LivingEntity> live = c.still(hit);
			Vec3[] stop = new Vec3[live.size()];
			for (int i = 0; i < live.size(); i++) {
				stop[i] = live.get(i).position();
			}
			Set<LivingEntity> shocked = new HashSet<>();
			for (LivingEntity t : live) {
				Vec3 at = PairCast.mid(t);
				for (LivingEntity e : c.enemiesNear(at, 3)) {
					if (shocked.add(e)) {
						c.hurt(e, 4 * c.power);
					}
				}
				c.sphere(PairCast.shift(0xFF2A4A, 0x2A0008, 1.2F), at, 1.6, 30);
			}
			c.sound(SoundEvents.WARDEN_SONIC_CHARGE, PairCast.mid(live.isEmpty() ? c.caster : live.get(0)), 0.7F, 0.8F);
			c.every(2, 15, frame -> {
				for (int i = 0; i < live.size(); i++) {
					LivingEntity t = live.get(i);
					if (c.here(t)) {
						c.pullTo(t, stop[i], 0.8);
					}
				}
			});
		});
	}

	/**
	 * Price of Blood: you bleed for the spell. A crimson ritual circle spins under you; then three of your own health
	 * is spent (never your last) and the strike lands through armour, deeper the more of your health is missing.
	 */
	@Pair(a = "hemomancy", b = "sanguine_rite", name = "Price of Blood", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Pays 3 of your own health (never your last: if it can't be paid in full, nothing happens), then the first enemy "
			+ "takes 6 damage through armour, plus 1 for every 1.5 health you are missing after paying (6 at most).")
	public static void priceOfBlood(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity victim = c.firstEnemy();
		if (victim == null) {
			return;
		}
		// The ritual: a blood circle tightens under the caster for a second.
		c.column(PairCast.shift(0xFF2A4A, 0x3A0006, 0.9F), PairCast.mid(self), 1.0, 2.0, 30);
		c.every(5, 4, frame -> c.ring(PairCast.dust(0xB0001A, 0.8F), PairCast.mid(self), 1.6 - frame * 0.3, 18, frame));
		c.later(20, () -> {
			if (!c.here(victim)) {
				return;
			}
			float cost = 3;
			if (self.getHealth() - cost < 1) {
				return;
			}
			self.setHealth(self.getHealth() - cost);
			double missing = self.getMaxHealth() - self.getHealth();
			double bonus = Math.min(6, Math.floor(missing / 1.5));
			c.line(PairCast.dust(0x8B0010, 0.9F), PairCast.mid(self), PairCast.mid(victim), 3);
			c.sound(SoundEvents.WITHER_SHOOT, PairCast.mid(self), 0.5F, 0.6F);
			c.hurt(victim, (6 + bonus) * c.power);
			c.sound(SoundEvents.SHULKER_BULLET_HIT, PairCast.mid(victim), 0.8F, 0.6F);
			c.punch(0.1F);
		});
		// The aftermath: a pool of red under the victim.
		c.later(40, () -> {
			if (c.here(victim)) {
				c.disc(PairCast.dust(0x5A0008, 1.0F), victim.position(), 1.2, 24);
			}
		});
	}
}
