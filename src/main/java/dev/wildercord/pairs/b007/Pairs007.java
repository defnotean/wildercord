package dev.wildercord.pairs.b007;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Group 7: ten hand-made pairs, each with its own mechanic, palette, beats and sound. */
public final class Pairs007 {
	private Pairs007() {}

	/**
	 * Stonewing: a stone helix wraps each ally and the wind inside it bursts out in pulses.
	 * Look: a pale teal and grey-brown helix tightening round the body, then six pale rings.
	 */
	@Pair(a = "gale_mantle", b = "stoneform", name = "Stonewing", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "For 12 seconds the target takes 20% less damage (Resistance I). Six times, every 2 seconds, the wind inside "
			+ "its stone shell bursts out: enemies within 3 blocks are knocked back and take 1 damage.")
	public static void stonewing(PairCast c) {
		List<LivingEntity> wrapped = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : wrapped) {
			c.effect(t, MobEffects.RESISTANCE, 12, 0);
			c.sound(SoundEvents.STONE_HIT, PairCast.mid(t), 0.9F, 0.7F);
		}
		// The shell forms: a stone helix, wound round with pale wind, swelling out over half a second.
		c.every(3, 8, frame -> {
			for (LivingEntity t : c.still(wrapped)) {
				c.helix(PairCast.dust(0xD8F4E8, 0.9F), PairCast.dust(0x8C8270, 1.1F), t.position(),
					0.4 + 0.12 * frame, 2.2, 1.5, 24);
			}
		});
		// Then the gusts: each pulse rings out from the body and pushes back whatever is near.
		c.every(40, 6, pulse -> {
			for (LivingEntity t : c.still(wrapped)) {
				Vec3 at = PairCast.mid(t);
				c.ring(PairCast.dust(0xD8F4E8, 1.1F), t.position().add(0, 0.2, 0), 1.2, 24, pulse * 0.4);
				c.wave(ParticleTypes.SMALL_GUST, at, 16, 0.35);
				c.sound(SoundEvents.WIND_CHARGE_BURST, at, 0.7F, 1.3F);
				for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
					c.knockFrom(e, at, 1.2, 0.3);
					c.strike(e, 1 * c.power);
				}
			}
		});
	}

	/**
	 * Gale Bulwark: a ward of absorption, and while it lasts projectiles flying at the target are turned round.
	 * Look: gold magic circles stacked in front of the target, and pale sparks where an arrow turns.
	 */
	@Pair(a = "deflect", b = "shield", name = "Gale Bulwark", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "For 8 seconds the target has 3 hearts of absorption, and every arrow or projectile flying at it within 4 blocks "
			+ "is turned round and sent back the way it came, at the same speed.")
	public static void galeBulwark(PairCast c) {
		List<LivingEntity> wards = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : wards) {
			c.absorb(t, 6 * c.power, 8);
			c.sound(SoundEvents.SHIELD_BLOCK, PairCast.mid(t), 0.9F, 0.8F);
		}
		// Magic circles appear in front of each ward, one above another, a circle at a time.
		c.every(5, 4, rise -> {
			for (LivingEntity t : c.still(wards)) {
				Vec3 front = PairCast.mid(t).add(t.getLookAngle().scale(1.3));
				Vec3 at = new Vec3(front.x, t.getY() + 0.4 + rise * 0.45, front.z);
				c.ring(PairCast.dust(0xF4E9B8, 1.0F), at, 0.9 + rise * 0.2, 28, rise * 0.3);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.5F, 0.9F + 0.15F * rise);
			}
		});
		// For the whole ward, whatever flies at it is turned round.
		c.every(2, Math.max(1, c.ticks(8) / 2), tick -> {
			for (LivingEntity t : c.still(wards)) {
				Vec3 at = PairCast.mid(t);
				AABB box = new AABB(at, at).inflate(4 * c.radius);
				for (Entity e : c.level.getEntities((Entity) null, box, p -> p instanceof Projectile)) {
					Vec3 v = e.getDeltaMovement();
					if (v.lengthSqr() > 1.0E-4 && v.dot(at.subtract(e.position())) > 0) {
						e.setDeltaMovement(v.scale(-1));
						c.line(PairCast.shift(0xFFF4C2, 0x9FD8FF, 0.7F), e.position(), e.position().add(v.scale(-2)), 3);
						c.sound(SoundEvents.BREEZE_DEFLECT, e.position(), 0.7F, 1.2F);
					}
				}
			}
		});
	}

	/**
	 * Tusk Upheaval: you charge along your look, and everything on the run is tossed into the air and comes down.
	 * Look: a brown dust streak across the ground, rising dust columns round the tossed, then a heavy disc of dust.
	 */
	@Pair(a = "launch", b = "tusk_charge", name = "Tusk Upheaval", element = "earth", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "You charge up to 8 blocks the way you look. Each enemy in your path is tossed into the air and marked airborne, "
			+ "and 1.5 seconds later lands for 1 damage per block you ran (8 at most), with 2 damage to each enemy within 2 blocks.")
	public static void tuskUpheaval(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 run = flatForward(c.dir());
		double ran = 0;
		for (int d = 8; d >= 1; d--) {
			// A charge runs, so it stops at a wall rather than passing through it.
			if (clear(c, start, start.add(run.scale(d))) && c.blink(self, start.add(run.scale(d)))) {
				ran = d;
				break;
			}
		}
		if (ran < 1) {
			c.sound(SoundEvents.RAVAGER_ROAR, start, 0.5F, 1.8F);
			return;
		}
		Vec3 mid = start.add(self.position()).scale(0.5);
		List<LivingEntity> onPath = new ArrayList<>();
		for (LivingEntity e : c.enemiesNear(mid, ran / 2 + 2.5)) {
			if (onPath(e, start, run, ran, 1.8 * c.radius)) {
				onPath.add(e);
			}
		}
		final double distance = ran;
		final List<LivingEntity> tossed = PairCast.first(onPath, PairCast.MAX_TARGETS);
		for (LivingEntity t : tossed) {
			c.lift(t, 0.9);
			c.mark(t, Reactions.Mark.AIRBORNE);
		}
		c.punch(0.25F);
		c.sound(SoundEvents.HOGLIN_ANGRY, start, 0.9F, 1.4F);
		// The charge: a dust streak sweeps forward along the path, five frames.
		c.every(2, 5, frame -> {
			Vec3 at = start.add(run.scale(distance * frame / 4.0));
			c.disc(PairCast.dust(0x7A5A3A, 1.1F), at, 0.9, 10);
			c.particles(ParticleTypes.DUST_PLUME, at, 4, 0.3, 0.02);
		});
		// The toss: each tossed enemy has a pale wind spiral lifting it up.
		c.every(2, 12, frame -> {
			for (LivingEntity t : c.still(tossed)) {
				c.spiral(PairCast.dust(0xDDE6EA, 0.8F), t.position(), 0.7, 3.5, 1.5, 14);
				c.column(PairCast.dust(0xB89B72, 1.0F), t.position(), 0.5, 1.5, 6);
			}
		});
		c.sound(SoundEvents.WIND_CHARGE_BURST, start, 0.6F, 0.8F);
		c.later(30, () -> {
			for (LivingEntity t : c.still(tossed)) {
				slam(c, t, distance);
			}
		});
	}

	/** Whether nothing solid stands between {@code from} and {@code to}, at knee and head height. */
	private static boolean clear(PairCast c, Vec3 from, Vec3 to) {
		for (double y : new double[] {0.5, 1.5}) {
			var ray = c.level.clip(new net.minecraft.world.level.ClipContext(from.add(0, y, 0), to.add(0, y, 0),
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, c.caster));
			if (ray.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Afterimage Feint: the target slips aside and a phantom stays; enemies are drawn to it, then it bursts.
	 * Look: a violet-white afterimage that condenses, a soul-dust pull, and a wide burst of pale motes.
	 */
	@Pair(a = "evade", b = "phantom", name = "Afterimage Feint", element = "void", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "The target slips 3 blocks aside, leaving a phantom where it stood for 4 seconds. Every enemy within 8 blocks "
			+ "of the phantom is dragged towards it, and when it bursts it deals 8 magic damage to each enemy within 3 blocks.")
	public static void afterimageFeint(PairCast c) {
		List<LivingEntity> slipped = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : slipped) {
			Vec3 spot = t.position();
			Vec3 side = flatSide(t.getLookAngle());
			if (!c.blink(t, spot.add(side.scale(3)))) {
				c.blink(t, spot.add(side.scale(-3)));
			}
			Vec3 phantom = c.ground(spot);
			c.line(PairCast.shift(0xB8C4FF, 0x6E5BD6, 0.9F), spot.add(0, 1, 0), t.position().add(0, 1, 0), 3);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, spot, 0.6F, 1.5F);
			// The afterimage condenses: a violet-white sphere shrinking to a figure.
			c.every(3, 6, frame -> c.sphere(PairCast.shift(0xE8ECFF, 0x6E5BD6, 0.9F), phantom.add(0, 1, 0),
				1.6 - frame * 0.12, 24));
			// The pull: everything within 8 blocks of the phantom is drawn in, trailing soul dust.
			List<LivingEntity> drawn = PairCast.first(c.enemiesNear(phantom, 8 * c.radius), PairCast.MAX_TARGETS);
			int hold = c.ticks(4);
			c.every(4, Math.max(1, hold / 4), frame -> {
				for (LivingEntity e : c.still(drawn)) {
					c.pullTo(e, phantom, 0.5);
					c.particles(ParticleTypes.SOUL, PairCast.mid(e), 2, 0.2, 0.02);
				}
			});
			// The burst.
			c.later(hold, () -> {
				Vec3 at = phantom.add(0, 1, 0);
				c.sphere(PairCast.shift(0xB8C4FF, 0x2A1B5C, 1.3F), at, 2.5 * c.radius, 60);
				c.wave(ParticleTypes.SOUL, at, 30, 0.5);
				for (LivingEntity e : c.enemiesNear(phantom, 3 * c.radius)) {
					c.hurt(e, 8 * c.power);
				}
				c.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, phantom, 1.0F, 0.8F);
				c.tint(phantom, 6, 0x6E5BD6, 8);
			});
		}
	}

	/**
	 * Hourglass Gale: a hasted ally, and each second a gust of hurried time shoves the enemies round it back and slows them.
	 * Look: a gold spiral climbing the ally, then a white END_ROD wave and a gold ring each second.
	 */
	@Pair(a = "accelerate", b = "swift", name = "Hourglass Gale", element = "time", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "For 10 seconds the target gets Speed III and sheds Slowness and frozen skin. Every second a gust of hurried time "
			+ "shoves enemies within 3 blocks back and slows them (Slowness II for 1.5 seconds).")
	public static void hourglassGale(PairCast c) {
		List<LivingEntity> hasted = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : hasted) {
			c.effect(t, MobEffects.SPEED, 10, 2);
			t.removeEffect(MobEffects.SLOWNESS);
			t.setTicksFrozen(0);
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, PairCast.mid(t), 0.8F, 1.6F);
		}
		// The climb: a gold spiral winding up the body over four frames.
		c.every(2, 8, frame -> {
			for (LivingEntity t : c.still(hasted)) {
				c.spiral(PairCast.dust(0xE8C56A, 1.0F), t.position(), 0.9, 2.2, 2, 16);
			}
		});
		// Each second: a white wave of END_ROD outward and a gold ring at the feet, then the gust shoves.
		c.every(20, Math.max(1, c.ticks(10) / 20), beat -> {
			for (LivingEntity t : c.still(hasted)) {
				Vec3 at = PairCast.mid(t);
				c.wave(ParticleTypes.END_ROD, at, 22, 0.4);
				c.ring(PairCast.shift(0xFFE49A, 0xFFFFFF, 1.0F), t.position().add(0, 0.1, 0), 1.5, 22, beat * 0.3);
				c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 0.7F, 1.0F + 0.1F * beat);
				for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
					c.knockFrom(e, at, 0.6, 0.1);
					c.effect(e, MobEffects.SLOWNESS, 1.5, 1);
				}
			}
		});
	}

	/**
	 * Herald's Truce: allies near the landing get a burst of speed, and a banner's calm settles on the monsters after them.
	 * Look: pale gold banner columns over each ally, then heart motes and rings over every monster it calms.
	 */
	@Pair(a = "rally", b = "soothe", name = "Herald's Truce", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Allies within 8 blocks of where it landed get Speed I and Jump Boost I for 12 seconds. For 6 seconds, once a "
			+ "second, every monster within 8 blocks that is after an ally forgets its target.")
	public static void heraldsTruce(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> rallied = c.alliesNear(at, 8 * c.radius);
		for (LivingEntity a : rallied) {
			c.effect(a, MobEffects.SPEED, 12, 0);
			c.effect(a, MobEffects.JUMP_BOOST, 12, 0);
			c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 4, 0.5, 0.02);
		}
		// The banner rises over each ally, a pale column four frames long.
		c.every(4, 4, frame -> {
			for (LivingEntity a : c.still(rallied)) {
				c.column(PairCast.dust(0xF7D4C8, 1.0F), a.position(), 0.5, 0.6 + frame * 0.6, 14);
			}
		});
		// Soothe: every second, the monsters after an ally let go of it.
		c.every(20, 6, beat -> {
			List<LivingEntity> friends = c.alliesNear(at, 8 * c.radius);
			for (LivingEntity m : c.enemiesNear(at, 8 * c.radius)) {
				if (m instanceof Mob mob && c.movable(mob) && mob.getTarget() != null && friends.contains(mob.getTarget())) {
					mob.setTarget(null);
					c.particles(ParticleTypes.HEART, PairCast.mid(mob).add(0, 0.6, 0), 1, 0.2, 0.0);
					c.ring(PairCast.dust(0xF7D4C8, 0.8F), mob.position().add(0, 0.2, 0), 0.9, 12, beat * 0.5);
					c.sound(SoundEvents.NOTE_BLOCK_FLUTE, mob.position(), 0.6F, 1.0F + 0.1F * beat);
				}
			}
		});
	}

	/**
	 * Mend Step: you blink to the landing spot, then the allies round you are healed, healing past full becoming absorption.
	 * Look: a green-to-gold reverse-portal streak, a double helix of hearts at the landing, then a golden sphere per ally.
	 */
	@Pair(a = "blink", b = "heal", name = "Mend Step", element = "life", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Teleports you to where the spell landed (max 40 blocks). Every ally within 4 blocks of your new spot is healed "
			+ "6 health (3 hearts); healing past full becomes absorption for 10 seconds.")
	public static void mendStep(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 to = c.point();
		Vec3 offset = to.subtract(from);
		if (offset.length() > 40) {
			to = from.add(offset.normalize().scale(40));
		}
		c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.7F, 1.4F);
		c.column(ParticleTypes.REVERSE_PORTAL, from, 0.5, 2.0, 16);
		c.blink(self, to);
		Vec3 landed = self.position();
		c.line(PairCast.shift(0x7CF5A8, 0xFFE27A, 0.8F), from.add(0, 1, 0), landed.add(0, 1, 0), 3);
		// The landing: a green and gold double helix wound up round the spot.
		c.helix(PairCast.dust(0x7CF5A8, 1.0F), PairCast.dust(0xFFE27A, 1.0F), landed, 0.9, 2.4, 1.5, 24);
		c.later(6, () -> {
			for (LivingEntity a : c.still(c.alliesNear(landed, 4 * c.radius))) {
				double amount = 6 * c.power;
				double missing = a.getMaxHealth() - a.getHealth();
				c.heal(a, amount);
				double over = amount - missing;
				if (over > 0) {
					c.absorb(a, over, 10);
				}
				c.sphere(PairCast.shift(0xFFE27A, 0xFFFFFF, 0.9F), PairCast.mid(a), 1.0, 20);
				c.particles(ParticleTypes.HEART, PairCast.mid(a), 3, 0.3, 0.02);
				c.sound(SoundEvents.PLAYER_LEVELUP, PairCast.mid(a), 0.4F, 1.4F);
			}
		});
	}

	/**
	 * Taproot Tug: enemies near the point are dragged in, then roots burst up under each and hold it.
	 * Look: green pull lines converging on the point, then brown root columns that grow out of the ground.
	 */
	@Pair(a = "pull", b = "rootsnare", name = "Taproot Tug", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Every enemy within 3 blocks of the point is dragged to it and marked as pulled, then roots burst up and hold each "
			+ "one for 1.5 seconds (Slowness V), dealing 1.5 damage per block it was dragged (9 at most).")
	public static void taprootTug(PairCast c) {
		Vec3 at = c.ground(c.point());
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(at, 3 * c.radius), PairCast.MAX_TARGETS);
		double[] began = new double[caught.size()];
		for (int i = 0; i < began.length; i++) {
			began[i] = PairCast.mid(caught.get(i)).distanceTo(at);
		}
		c.sound(SoundEvents.LEAD_TIED, at, 0.8F, 0.8F);
		// The tug: eight frames, every enemy pulled in with a green line behind it.
		c.every(2, 8, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, at, 0.9);
				c.line(PairCast.dust(0x9AD06A, 0.8F), PairCast.mid(t), at.add(0, 1, 0), 1.5);
			}
		});
		// The roots: each held enemy's roots grow up under it; it is hit for the distance it was dragged.
		c.later(16, () -> {
			for (int i = 0; i < caught.size(); i++) {
				LivingEntity t = caught.get(i);
				if (!c.here(t)) {
					continue;
				}
				double dragged = Math.max(0, began[i] - PairCast.mid(t).distanceTo(at));
				c.effect(t, MobEffects.SLOWNESS, 1.5, 4);
				c.mark(t, Reactions.Mark.PULLED);
				c.strike(t, Math.min(9, 1.5 * dragged) * c.power);
				rootUp(c, t);
			}
		});
	}

	/**
	 * Anchor Chain: enemies are chained to the point, yanked back when they stray, and you are anchored by their number.
	 * Look: a crackling iron zigzag from a hook above the point to each chained enemy, and an iron ring at your feet.
	 */
	@Pair(a = "anchor", b = "shackle", name = "Anchor Chain", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Chains each enemy within 2.5 blocks of the point to it for 5 seconds: it is yanked back if it strays over 2 blocks, "
			+ "and the chain bites for 2 damage a second. You gain 1 heart of absorption per chained enemy (4 hearts at most).")
	public static void anchorChain(PairCast c) {
		Vec3 at = c.ground(c.point());
		List<LivingEntity> chained = PairCast.first(c.enemiesNear(at, 2.5 * c.radius), PairCast.MAX_TARGETS);
		int count = chained.size();
		c.ring(PairCast.dust(0x3C4048, 1.0F), c.caster.position().add(0, 0.1, 0), 1.0, 20, 0);
		if (count == 0) {
			c.sound(SoundEvents.CHAIN_PLACE, at, 0.4F, 1.4F);
			return;
		}
		c.absorb(c.caster, 2.0 * Math.min(count, 4), 5);
		for (LivingEntity t : chained) {
			c.sound(SoundEvents.CHAIN_PLACE, PairCast.mid(t), 0.9F, 0.8F);
		}
		// The shackle: a crackling iron chain from the hook to each enemy, yanking it back and biting every second.
		int links = Math.max(1, c.ticks(5) / 4);
		c.every(4, links, step -> {
			for (LivingEntity t : c.still(chained)) {
				Vec3 body = PairCast.mid(t);
				c.zigzag(PairCast.dust(0xB7BFC8, 0.6F), at.add(0, 1, 0), body, 0.3, 3);
				double stray = Math.hypot(t.getX() - at.x, t.getZ() - at.z);
				if (stray > 2) {
					c.pullTo(t, at, 1.2);
				}
				if (step % 5 == 0) {
					c.strike(t, 2 * c.power);
					c.sound(SoundEvents.CHAIN_HIT, body, 0.6F, 1.0F);
				}
			}
		});
		// The hook: a dark iron disc on the ground at the point, pulsing as the chains take hold.
		c.every(5, 3, frame -> c.disc(PairCast.dust(0x3C4048, 1.2F), at, 1.2 + frame * 0.3, 18));
	}

	/**
	 * Recalled Exile: enemies are banished, struck, then pulled back to where they vanished, hurt for the distance.
	 * Look: a crimson-ink sphere that shrinks away at the origin, a reverse-portal arrival, and a magenta beam home.
	 */
	@Pair(a = "banish", b = "harm", name = "Recalled Exile", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Banishes up to 8 enemies up to 8 blocks further from you, each struck for 5 magic damage, exposed (arcane mark) and "
			+ "dazed (Nausea for 3 seconds). After 3 seconds each is recalled to where it vanished, taking 1 wither damage per block sent away (8 at most).")
	public static void recalledExile(PairCast c) {
		List<LivingEntity> banished = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		int recall = c.ticks(3);
		for (LivingEntity t : banished) {
			Vec3 origin = t.position();
			Vec3 away = flatForward(origin.subtract(c.caster.position()));
			double sent = 0;
			for (int d = 8; d >= 1; d--) {
				if (c.blink(t, origin.add(away.scale(d)))) {
					sent = d;
					break;
				}
			}
			final double back = sent;
			c.hurt(t, 5 * c.power);
			c.mark(t, Reactions.Mark.EXPOSED);
			c.effect(t, MobEffects.NAUSEA, 3, 0);
			// Vanishing: a violet sphere shrinks away where it stood.
			c.every(2, 4, frame -> c.sphere(PairCast.shift(0xD0306F, 0x2A0820, 1.0F), PairCast.mid(t), 1.1 - frame * 0.2, 20));
			c.sound(SoundEvents.ENDERMAN_TELEPORT, origin, 1.0F, 0.6F);
			c.particles(ParticleTypes.ENCHANTED_HIT, PairCast.mid(t), 6, 0.4, 0.1);
			// Arrival: reverse-portal motes at the far spot.
			c.later(8, () -> {
				if (c.here(t)) {
					c.column(ParticleTypes.REVERSE_PORTAL, t.position(), 0.5, 2.0, 14);
				}
			});
			if (back >= 1) {
				c.later(recall, () -> recallHome(c, t, origin, back));
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	/** The landing of a tossed enemy: a heavy hit on it, a dust shockwave, a shake, and 2 damage to those round it. */
	private static void slam(PairCast c, LivingEntity t, double ran) {
		Vec3 at = t.position();
		c.strike(t, Math.min(8, ran) * c.power);
		c.wave(ParticleTypes.CLOUD, at.add(0, 0.2, 0), 22, 0.45);
		c.disc(PairCast.dust(0x8B6A45, 1.2F), at, 1.8, 40);
		c.sound(SoundEvents.MACE_SMASH_GROUND_HEAVY, at, 1.0F, 0.8F);
		c.shake(at, 0.35F, 10);
		for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
			if (near != t) {
				c.line(PairCast.dust(0xB89B72, 0.8F), at, PairCast.mid(near), 3);
				c.strike(near, 2 * c.power);
			}
		}
	}

	/** Brown roots that grow up out of the ground under a held enemy, three frames. */
	private static void rootUp(PairCast c, LivingEntity t) {
		Vec3 feet = c.ground(t.position());
		c.sound(SoundEvents.ROOTED_DIRT_BREAK, feet, 1.0F, 0.7F);
		c.shake(feet, 0.15F, 4);
		c.every(3, 3, frame -> {
			c.column(PairCast.dust(0x5A3E22, 1.2F), feet, 0.5, 0.8 + frame * 0.5, 14);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 4, 0.4, 0.02);
		});
	}

	/** The banished enemy comes back to where it vanished, hit with wither for the blocks it was sent. */
	private static void recallHome(PairCast c, LivingEntity t, Vec3 origin, double sent) {
		if (!c.here(t)) {
			return;
		}
		Vec3 from = PairCast.mid(t);
		c.line(PairCast.shift(0x8A1248, 0xFF8FC0, 0.9F), from, origin.add(0, 1, 0), 3);
		c.blink(t, origin);
		c.wave(ParticleTypes.PORTAL, origin.add(0, 1, 0), 22, 0.35);
		c.sound(SoundEvents.PORTAL_TRIGGER, origin, 0.8F, 1.4F);
		c.wither(t, sent * c.power);
	}

	/** Whether {@code e} is on the run from {@code start} along {@code run} for {@code ran} blocks, {@code width} wide. */
	private static boolean onPath(LivingEntity e, Vec3 start, Vec3 run, double ran, double width) {
		Vec3 p = PairCast.mid(e).subtract(start);
		double along = p.dot(run);
		Vec3 side = p.subtract(run.scale(along));
		return along >= -1 && along <= ran + 1 && Math.hypot(side.x, side.z) <= width;
	}

	/** The flat (horizontal) unit direction of {@code v}, or straight ahead when it has none. */
	private static Vec3 flatForward(Vec3 v) {
		Vec3 flat = new Vec3(v.x, 0, v.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	/** The horizontal unit to the right of {@code look}. */
	private static Vec3 flatSide(Vec3 look) {
		Vec3 f = flatForward(look);
		return new Vec3(-f.z, 0, f.x);
	}
}
