package dev.wildercord.pairs.b017;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs017 {
	private Pairs017() {}

	/**
	 * Undertow: a wall of water rolls out from you and soaks and hurls back everything in its lane. Two seconds later the
	 * water drains back and drags each one to the spot where it stood.
	 */
	@Pair(a = "recoil", b = "tidewrit", name = "Undertow", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A wall of water 7 blocks wide rolls 11 blocks on from you: 8 damage to what it struck and each enemy in it, soaked and hurled back. "
			+ "Two seconds later the undertow drags each one back to where it stood, for 4 more damage.")
	public static void undertow(PairCast c) {
		Vec3 from = c.origin();
		Vec3 dir = flat(c.dir());
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		double length = 11;
		double half = 3.5 * c.radius;
		// What the spell struck is caught first; then whatever stands in the wall's lane.
		List<LivingEntity> swept = new ArrayList<>(PairCast.first(c.enemies(), PairCast.MAX_TARGETS));
		for (LivingEntity e : c.enemiesNear(from.add(dir.scale(length / 2)), length / 2 + half)) {
			if (swept.size() < PairCast.MAX_TARGETS && !swept.contains(e) && inLane(from, dir, length, half, PairCast.mid(e))) {
				swept.add(e);
			}
		}
		List<Vec3> spots = new ArrayList<>();
		c.sound(SoundEvents.GENERIC_SPLASH, from, 0.9F, 0.7F);
		for (LivingEntity t : swept) {
			spots.add(t.position());
			c.freeze(t, 8 * c.power);
			c.mark(t, Reactions.Mark.SOAKED);
			c.knockFrom(t, from, 1.1, 0.25);
		}
		// The wall rolls out: a bar of water two blocks a frame, six frames.
		c.every(2, 6, frame -> {
			Vec3 front = from.add(dir.scale(1.5 + frame * 2.0));
			c.line(PairCast.shift(0xA6E8FF, 0x2A6FD6, 1.0F), front.subtract(side.scale(half)), front.add(side.scale(half)), 2);
			c.particles(ParticleTypes.SPLASH, front, 4, 0.4, 0.05);
		});
		// Two seconds on, the undertow drags each soaked one back to its spot.
		c.later(40, () -> {
			c.sound(SoundEvents.GENERIC_SPLASH, from, 0.7F, 0.5F);
			for (int i = 0; i < swept.size(); i++) {
				LivingEntity t = swept.get(i);
				if (!c.here(t)) {
					continue;
				}
				Vec3 spot = spots.get(i);
				c.pullTo(t, spot, 1.3);
				c.hurt(t, 4 * c.power);
				c.spiral(PairCast.dust(0x9FE6FF, 0.8F), spot, 0.8, 1.6, 2, 14);
			}
		});
	}

	/**
	 * Surf Leap: in water or rain a current carries you forward in short steps, stopped by walls, and at its crest you leap
	 * like a dolphin. On dry land you just hop. Neither leap takes fall damage.
	 */
	@Pair(a = "current", b = "porpoise", name = "Surf Leap", element = "frost", kind = EffectKind.MOVEMENT,
		text = "In water or rain: a current carries you up to 15 blocks the way you look (walls stop it), then you leap like "
			+ "a dolphin. On dry land you just hop forward. Neither leap takes fall damage.")
	public static void surfLeap(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 dir = flat(self.getLookAngle());
		Vec3 feet = self.position();
		c.sound(SoundEvents.GENERIC_SPLASH, feet, 0.8F, 1.2F);
		if (!(self.isInWater() || c.level.isRainingAt(self.blockPosition()))) {
			// Dry land: a hop forward, and no fall for a moment after it.
			c.push(self, dir.scale(0.7).add(0, 0.45, 0));
			c.particles(ParticleTypes.CLOUD, feet, 6, 0.3, 0.02);
			c.every(3, 8, frame -> self.resetFallDistance());
			return;
		}
		// The current: five steps of three blocks, two ticks apart; a wall or a blocked spot ends it.
		boolean[] stopped = {false};
		c.every(2, 5, step -> {
			if (stopped[0] || !self.isAlive()) {
				return;
			}
			Vec3 from = self.position();
			Vec3 ahead = from.add(dir.scale(3));
			BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.5, 0), ahead.add(0, 0.5, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (wall.getType() != HitResult.Type.MISS || !c.blink(self, ahead)) {
				stopped[0] = true;
				return;
			}
			c.particles(ParticleTypes.BUBBLE_COLUMN_UP, from, 5, 0.4, 0.05);
			c.particles(PairCast.dust(0x7FD8FF, 1.0F), from.add(0, 0.1, 0), 3, 0.2, 0.0);
		});
		// The crest: the dolphin leap, with no fall after it.
		c.later(10, () -> {
			if (!self.isAlive()) {
				return;
			}
			c.push(self, dir.scale(0.5).add(0, 0.9, 0));
			c.sound(SoundEvents.GENERIC_SPLASH, self.position(), 0.9F, 1.6F);
			c.arc(ParticleTypes.DOLPHIN, feet, self.position().add(dir.scale(3)), 2.5, 16);
			c.every(3, 10, frame -> self.resetFallDistance());
		});
	}

	/**
	 * Glassrun: skates and skims. For fifteen seconds the nearby allies go faster, faster again on ice, and stay up on water
	 * unless they crouch: their frost trails behind them.
	 */
	@Pair(a = "skaters_edge", b = "skimstep", name = "Glassrun", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "For 15 seconds you and allies within 6 blocks get Speed I, Speed II while on ice, and can run across water "
			+ "(crouch to sink).")
	public static void glassrun(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 at = self.position();
		c.sound(SoundEvents.GLASS_PLACE, at, 0.9F, 1.4F);
		c.ring(PairCast.shift(0xE6F7FF, 0x7FD4FF, 1.0F), at.add(0, 0.1, 0), 1.0, 24, 0);
		// Sixty steps a quarter second apart: each one keeps the speed on and holds the skimmers up.
		c.every(5, 60, frame -> {
			for (LivingEntity a : c.alliesNear(self.position(), 6 * c.radius)) {
				c.effect(a, MobEffects.SPEED, 1.2, 0);
				if (onIce(c, a)) {
					c.effect(a, MobEffects.SPEED, 1.2, 1);
				}
				if (!a.isShiftKeyDown() && onWater(c, a) && a.getDeltaMovement().y < 0) {
					a.setDeltaMovement(a.getDeltaMovement().multiply(1, 0, 1));
				}
				c.particles(ParticleTypes.SNOWFLAKE, PairCast.mid(a).add(0, -0.6, 0), 3, 0.4, 0.02);
			}
		});
	}

	/**
	 * Amber Ward: a honeyed ward. The allies are healed and cured at once, then every second for nine more seconds the
	 * ward cures poison and wither again and heals a little.
	 */
	@Pair(a = "honeydew", b = "staunch", name = "Amber Ward", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Allies within 6 blocks are healed 3 (more with power) and cured of poison and wither. For 9 more seconds, once a "
			+ "second, they are cured again and healed 1.")
	public static void amberWard(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> ward = c.alliesNear(at, 6 * c.radius);
		c.sound(SoundEvents.NOTE_BLOCK_CHIME, at, 0.9F, 0.8F);
		c.ring(PairCast.shift(0xF2B632, 0xFFF3B0, 1.0F), at.add(0, 0.2, 0), 6 * c.radius, 30, 0);
		// Ten frames a second apart: the first heals 3, each after it heals 1; the cure runs in all ten.
		c.every(20, 10, frame -> {
			for (LivingEntity a : c.still(ward)) {
				cure(a);
				c.heal(a, (frame == 0 ? 3 : 1) * c.power);
				c.particles(ParticleTypes.FALLING_HONEY, PairCast.mid(a), 4, 0.4, 0.0);
			}
		});
	}

	/** Takes poison and wither off a creature. */
	private static void cure(LivingEntity a) {
		a.removeEffect(MobEffects.POISON);
		a.removeEffect(MobEffects.WITHER);
	}

	/**
	 * Steam Plate: the quench puts out every fire near, and the steam that rises hardens the frost of the burning and
	 * the frozen into a plate of ice that soaks the next blows.
	 */
	@Pair(a = "frost_molt", b = "quench", name = "Steam Plate", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Puts out the fire on you and allies within 4 blocks. Each ally who was burning or frozen gets a steam plate: "
			+ "3 hearts of absorption for 3 seconds, and their frost peels off.")
	public static void steamPlate(PairCast c) {
		Vec3 at = c.caster.position();
		List<LivingEntity> near = c.alliesNear(at, 4 * c.radius);
		List<LivingEntity> plated = new ArrayList<>();
		c.sound(SoundEvents.GENERIC_SPLASH, at, 1.0F, 1.5F);
		c.particles(ParticleTypes.CLOUD, at.add(0, 1, 0), 18, 1.2 * c.radius, 0.03);
		for (LivingEntity a : near) {
			boolean hot = a.isOnFire();
			boolean cold = a.getTicksFrozen() > 0;
			c.douse(a);
			if (cold) {
				a.setTicksFrozen(0);
			}
			if (hot || cold) {
				c.absorb(a, 6, 3);
				plated.add(a);
			}
		}
		// Six ticks later each plate forms: a ring of frost turning round its wearer, four frames.
		c.later(6, () -> c.every(4, 4, frame -> {
			for (LivingEntity a : c.still(plated)) {
				c.ring(PairCast.shift(0xE6F7FF, 0x9AD8FF, 0.9F), PairCast.mid(a), 0.9, 18, frame * 0.5);
				if (frame == 0) {
					c.sound(SoundEvents.GLASS_PLACE, PairCast.mid(a), 0.6F, 1.6F);
				}
			}
		}));
	}

	/**
	 * Slumber Chain: a drowse that sleeps each target, and a lullaby that spreads it. A blow wakes a sleeper and strikes
	 * again; a second later the sleepers' yawn lulls the enemies standing close by.
	 */
	@Pair(a = "drowse", b = "lullaby", name = "Slumber Chain", element = "life", kind = EffectKind.HARMFUL,
		traits = {"duration"},
		text = "Targets sleep 6 seconds: mobs get Slowness III and forget their target, and a blow that wakes one strikes again at 75% (12 at most) "
			+ "of its damage. Players and bosses are only slowed (Slowness III: 2 seconds for players, 6 for bosses). A second "
			+ "on, enemies within 3 blocks of a sleeper doze for 2 seconds, once each.")
	public static void slumberChain(PairCast c) {
		List<LivingEntity> dozing = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<LivingEntity> napping = new ArrayList<>(dozing);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.9F, 0.6F);
		for (LivingEntity t : dozing) {
			lull(c, t, 6);
		}
		// A second later the sleep spreads to the enemies close to each sleeper, once each.
		c.later(20, () -> {
			List<LivingEntity> spread = new ArrayList<>();
			for (LivingEntity s : c.still(napping)) {
				for (LivingEntity e : c.enemiesNear(PairCast.mid(s), 3 * c.radius)) {
					if (!napping.contains(e) && !spread.contains(e)) {
						spread.add(e);
					}
				}
			}
			for (LivingEntity e : spread) {
				lull(c, e, 2);
			}
			napping.addAll(spread);
		});
	}

	/** Puts one creature to sleep for {@code seconds}: a mob is slowed and forgets its target each moment; a player (2 seconds at most) and a boss are only slowed. */
	private static void lull(PairCast c, LivingEntity t, double seconds) {
		c.sound(SoundEvents.SCULK_CLICKING, PairCast.mid(t), 0.6F, 0.6F);
		if (t instanceof Player) {
			c.effect(t, MobEffects.SLOWNESS, Math.min(seconds, 2), 2);
			return;
		}
		if (!(t instanceof Mob m) || !c.movable(m)) {
			c.effect(t, MobEffects.SLOWNESS, seconds, 2);
			return;
		}
		c.effect(m, MobEffects.SLOWNESS, seconds, 2);
		boolean[] awake = {false};
		float[] hp = {m.getHealth()};
		int frames = Math.max(1, c.ticks(seconds) / 2);
		c.every(2, frames, frame -> {
			if (awake[0] || !m.isAlive()) {
				return;
			}
			if (m.getHealth() < hp[0]) {
				// A blow wakes it, and the blow strikes again at three quarters of its strength.
				awake[0] = true;
				m.removeEffect(MobEffects.SLOWNESS);
				c.hurt(m, Math.min(12, (hp[0] - m.getHealth()) * 0.75));
				c.particles(ParticleTypes.CRIT, PairCast.mid(m), 6, 0.3, 0.1);
				return;
			}
			// Asleep: it forgets what it hunted and stops where it stands.
			m.setTarget(null);
			m.getNavigation().stop();
			hp[0] = m.getHealth();
			c.particles(ParticleTypes.NOTE, PairCast.mid(m).add(0, 0.9, 0), 1, 0.2, 0.0);
		});
	}

	/**
	 * Umbral Bloom: moon petals fall into a dark disc. Those inside are struck and blinded and marked shadowed, the allies
	 * there are healed, and the disc lingers five seconds, withering whatever stands in it.
	 */
	@Pair(a = "eclipse", b = "moonpetal", name = "Umbral Bloom", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Petals fall on the point: 5 damage to each enemy within 3 blocks, and each is blinded for 4 seconds and marked "
			+ "shadowed. Allies there are healed 4. For 5 seconds, enemies in the dark take 2 withering damage a second.")
	public static void umbralBloom(PairCast c) {
		Vec3 at = c.point();
		double reach = 3 * c.radius;
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 0.7F, 0.6F);
		// The wind-up: a dark disc draws over the point while petals drift down into it.
		c.every(3, 5, frame -> {
			c.disc(PairCast.dust(0x1A0F2E, 1.3F), at, reach, 24);
			c.particles(PairCast.dust(0xF7C6E0, 0.9F), at.add(0, 3.5 - frame * 0.7, 0), 6, reach * 0.6, 0.0);
		});
		c.later(15, () -> {
			c.sound(SoundEvents.ELDER_GUARDIAN_CURSE, at, 0.5F, 1.6F);
			List<LivingEntity> under = c.enemiesNear(at, reach);
			for (LivingEntity e : c.still(under)) {
				c.hurt(e, 5 * c.power);
				c.effect(e, MobEffects.BLINDNESS, 4, 0);
				c.mark(e, Reactions.Mark.SHADOWED);
				c.line(PairCast.dust(0xF7C6E0, 0.8F), at.add(0, 3.5, 0), PairCast.mid(e), 3);
			}
			for (LivingEntity a : c.alliesNear(at, reach)) {
				c.heal(a, 4 * c.power);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 5, 0.4, 0.02);
			}
			c.star(PairCast.shift(0xF7C6E0, 0x1A0F2E, 0.9F), at.add(0, 0.2, 0), 6, reach, 0);
			// The disc lingers: five beats a second, each withering whatever is still inside it.
			c.every(20, 5, frame -> {
				for (LivingEntity e : c.enemiesNear(at, reach)) {
					c.wither(e, 2 * c.power);
				}
				c.disc(PairCast.dust(0x1A0F2E, 1.0F), at, reach, 14);
			});
		});
	}

	/**
	 * Petal Gate: a door of blossoms steps you to where the spell landed. The allies by your arrival get Regeneration, and
	 * three seconds later the gate draws you back to where you left, unless you're sneaking.
	 */
	@Pair(a = "bloomstep", b = "warp_step", name = "Petal Gate", element = "life", kind = EffectKind.MOVEMENT,
		text = "Steps you to where the spell landed (up to 24 blocks). Allies within 3 blocks of where you arrive get Regeneration I "
			+ "for 5 seconds. 3 seconds later you are drawn back to where you left, unless you're sneaking.")
	public static void petalGate(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 left = self.position();
		Vec3 to = c.point();
		if (to.distanceTo(left) > 24) {
			to = left.add(to.subtract(left).normalize().scale(24));
		}
		c.ring(PairCast.shift(0xFFB7D5, 0xFFFFFF, 1.0F), left.add(0, 1, 0), 1.2, 22, 0);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, left, 0.6F, 1.6F);
		if (!c.blink(self, to)) {
			return;
		}
		Vec3 arrived = self.position();
		c.sound(SoundEvents.ENDERMAN_TELEPORT, arrived, 0.6F, 1.2F);
		c.every(3, 6, frame -> c.ring(PairCast.dust(0xFFB7D5, 1.0F), arrived.add(0, 1, 0), 1.4 - frame * 0.2, 20, frame * 0.4));
		for (LivingEntity a : c.alliesNear(arrived, 3)) {
			c.effect(a, MobEffects.REGENERATION, 5, 0);
		}
		// Three seconds on, the gate draws you back to where you left.
		c.later(60, () -> {
			if (self.isAlive() && !self.isShiftKeyDown() && c.blink(self, left)) {
				c.ring(PairCast.dust(0xFFB7D5, 1.0F), left.add(0, 1, 0), 1.2, 22, 0);
				c.sound(SoundEvents.ENDERMAN_TELEPORT, left, 0.6F, 0.8F);
			}
		});
	}

	/**
	 * Black Fortune: the dark strike, and fortune's luck. Every second for three seconds each target may sparkle black,
	 * and a black spark lashes the nearest other enemy and puts the caster in the zone.
	 */
	@Pair(a = "blackspark", b = "fortune", name = "Black Fortune", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "8 damage now. For 3 seconds, once a second, each target has a 1 in 4 chance to spark black: 3 damage, 2 to the "
			+ "nearest other enemy within 8 blocks, and you're in the zone (Strength I and Speed I for 6 seconds).")
	public static void blackFortune(PairCast c) {
		List<LivingEntity> hit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, c.point(), 0.8F, 0.6F);
		for (LivingEntity t : hit) {
			c.hurt(t, 8 * c.power);
			c.particles(ParticleTypes.SQUID_INK, PairCast.mid(t), 8, 0.4, 0.05);
		}
		int rolls = Math.max(1, (int) Math.round(3 * c.duration));
		c.every(20, rolls, frame -> {
			for (LivingEntity t : c.still(hit)) {
				Vec3 at = PairCast.mid(t);
				c.particles(ParticleTypes.ENCHANT, at, 4, 0.3, 0.2);
				if (c.random() >= 0.25) {
					continue;
				}
				c.wither(t, 3 * c.power);
				c.star(PairCast.shift(0x2A0A3D, 0xE8C34A, 0.9F), at, 6, 1.0, frame * 0.3);
				c.sound(SoundEvents.GLASS_BREAK, at, 0.8F, 1.7F);
				LivingEntity arc = c.nearestEnemy(at, 8 * c.radius, t);
				if (arc != null) {
					c.zigzag(ParticleTypes.SQUID_INK, at, PairCast.mid(arc), 0.3, 3);
					c.wither(arc, 2 * c.power);
				}
				c.effect(c.caster, MobEffects.STRENGTH, 6, 0);
				c.effect(c.caster, MobEffects.SPEED, 6, 0);
			}
		});
	}

	/**
	 * Mendfield: a ring of mending. Allies are doused and healed, creatures are healed more, and the gear the caster
	 * wears or holds is mended: the hearts rise over the healed and the blossom settles after.
	 */
	@Pair(a = "restore", b = "tend", name = "Mendfield", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Within 4 blocks: allies are doused and healed 4 (animals, villagers and pets 8, iron golems 16), and the gear you "
			+ "wear and hold mends 8% of its durability. Power scales the healing.")
	public static void mendfield(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 at = c.point();
		double reach = 4 * c.radius;
		List<LivingEntity> near = c.alliesNear(at, reach);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.8F, 1.3F);
		c.ring(PairCast.shift(0xFFE27A, 0x7DFFB0, 1.0F), at.add(0, 0.2, 0), reach, 28, 0);
		for (LivingEntity a : near) {
			c.douse(a);
			double amount;
			if (a instanceof Player) {
				amount = 4;
			} else if (a instanceof IronGolem) {
				amount = 16;
			} else {
				amount = 8;
			}
			c.heal(a, amount * c.power);
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = self.getItemBySlot(slot);
			if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
				int mend = (int) Math.ceil(stack.getMaxDamage() * 0.08);
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - mend));
			}
		}
		// Ten ticks later the hearts rise over each one healed; then the blossom settles over the field.
		c.later(10, () -> {
			for (LivingEntity a : c.still(near)) {
				c.particles(ParticleTypes.HEART, PairCast.mid(a).add(0, 0.8, 0), 2, 0.3, 0.02);
			}
		});
		c.later(20, () -> c.particles(ParticleTypes.CHERRY_LEAVES, at.add(0, 2.5, 0), 14, reach * 0.4, 0.02));
	}

	// ------------------------------------------------------------------ helpers

	/** A horizontal unit vector of {@code v}, facing forward if {@code v} points straight up or down. */
	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** Whether {@code p} lies in the lane from {@code from} along {@code dir} for {@code length}, {@code half} wide. */
	private static boolean inLane(Vec3 from, Vec3 dir, double length, double half, Vec3 p) {
		Vec3 rel = p.subtract(from);
		double along = rel.dot(dir);
		if (along < 0 || along > length) {
			return false;
		}
		return rel.subtract(dir.scale(along)).length() <= half;
	}

	/** Whether the creature stands on water. */
	private static boolean onWater(PairCast c, LivingEntity a) {
		return c.level.getFluidState(BlockPos.containing(a.getX(), a.getY() - 0.1, a.getZ())).is(FluidTags.WATER);
	}

	/** Whether the creature stands on ice. */
	private static boolean onIce(PairCast c, LivingEntity a) {
		return c.level.getBlockState(BlockPos.containing(a.getX(), a.getY() - 0.1, a.getZ())).is(BlockTags.ICE);
	}
}
