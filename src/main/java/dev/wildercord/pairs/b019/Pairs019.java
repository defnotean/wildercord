package dev.wildercord.pairs.b019;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs019 {
	private Pairs019() {}

	/**
	 * Starved Maw: a void maw closes on the target and bites, then tears the good effects out of it and each one
	 * stings. The look: a dark shell shrinks onto the target, and starlight spills out of it afterwards.
	 */
	@Pair(a = "devour", b = "starmaw", name = "Starved Maw", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A void maw closes on the first enemy: a bite of 4 damage plus 1 for every tenth of its health it is missing "
			+ "(4 at most), then it swallows up to 3 of its good effects, 3 damage for each.")
	public static void starvedMaw(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		c.sound(SoundEvents.WARDEN_SONIC_CHARGE, PairCast.mid(t), 0.8F, 1.4F);
		// The jaws close: four frames of a dark shell shrinking onto the target.
		c.every(2, 4, frame -> {
			for (LivingEntity e : c.still(List.of(t))) {
				c.sphere(PairCast.shift(0x4A1A8A, 0x0B0014, 1.0F), PairCast.mid(e), 1.8 - frame * 0.4, 24);
			}
		});
		c.later(8, () -> maw(c, t));
	}

	private static void maw(PairCast c, LivingEntity t) {
		if (!c.here(t)) {
			return;
		}
		Vec3 at = PairCast.mid(t);
		double max = Math.max(1, t.getMaxHealth());
		int missing = (int) Math.min(4, Math.floor((max - t.getHealth()) / max * 10));
		c.hurt(t, (4 + missing) * c.power);
		c.sound(SoundEvents.ANVIL_LAND, at, 0.5F, 1.8F);
		c.wave(PairCast.dust(0x6A2BD9, 1.0F), at, 16, 0.3);
		// Up to three good effects are torn out of the target; each one stings.
		List<Holder<MobEffect>> good = new ArrayList<>();
		for (MobEffectInstance inst : t.getActiveEffects()) {
			if (good.size() < 3 && inst.getEffect().value().isBeneficial()) {
				good.add(inst.getEffect());
			}
		}
		if (good.isEmpty()) {
			return;
		}
		for (Holder<MobEffect> effect : good) {
			t.removeEffect(effect);
		}
		// The swallowed light spills back out of it a moment later.
		c.later(10, () -> {
			if (!c.here(t)) {
				return;
			}
			c.hurt(t, 3 * c.power * good.size());
			Vec3 spot = PairCast.mid(t);
			c.star(ParticleTypes.END_ROD, spot, good.size() + 2, 1.2, 0.3);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 0.9F, 1.6F);
		});
	}

	/**
	 * Gap Drop: a gap opens above each target and lifts it, then the gap lets it fall back. The look: a violet ring
	 * over every target with a thread drawing it up, and a shock of ash where it lands.
	 */
	@Pair(a = "hollow", b = "portalfall", name = "Gap Drop", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A gap opens above each of up to 8 enemies and lifts it into the air. 0.6 seconds later it is flung back "
			+ "down: 6 damage, and enemies within 2.5 blocks of where it lands take 2 and stagger.")
	public static void gapDrop(PairCast c) {
		List<LivingEntity> drawn = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : drawn) {
			c.lift(t, 0.45);
		}
		c.sound(SoundEvents.ENDERMAN_TELEPORT, c.point(), 0.7F, 0.6F);
		// The gap: four frames of a violet ring over each target, a thread pulling it up.
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(drawn)) {
				Vec3 gap = PairCast.mid(t).add(0, 3, 0);
				c.ring(PairCast.dust(0x6A2BD9, 1.1F), gap, 1.6 - frame * 0.3, 18, frame * 0.4);
				c.line(ParticleTypes.REVERSE_PORTAL, gap, PairCast.mid(t), 2);
			}
		});
		c.later(12, () -> {
			for (LivingEntity t : c.still(drawn)) {
				c.push(t, new Vec3(0, -1.4, 0));
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 6 * c.power);
				c.sound(SoundEvents.ANVIL_LAND, at, 0.6F, 0.7F);
				c.wave(PairCast.shift(0xE9D8FF, 0x2A0B4D, 1.0F), at.add(0, -0.8, 0), 20, 0.4);
				for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
					if (near != t) {
						c.hurt(near, 2 * c.power);
						c.knockFrom(near, at, 0.4, 0.1);
					}
				}
			}
		});
	}

	/**
	 * Hex Turn: the ward takes the curses off an ally and hands them on. The look: a gold ward ring round the ally,
	 * and a violet thread that carries each curse across to the nearest enemy.
	 */
	@Pair(a = "hexguard", b = "malison", name = "Hex Turn", element = "void", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "For 10 seconds, once a second, every harmful effect on an ally is stripped away and handed to the nearest "
			+ "enemy within 4 blocks (if there is one), for up to 6 seconds at the same level.")
	public static void hexTurn(PairCast c) {
		List<LivingEntity> warded = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		int pulses = Math.max(1, (int) Math.round(10 * c.duration));
		c.every(20, pulses, frame -> {
			for (LivingEntity a : c.still(warded)) {
				turn(c, a);
			}
		});
	}

	private static void turn(PairCast c, LivingEntity a) {
		List<MobEffectInstance> harmful = new ArrayList<>();
		for (MobEffectInstance inst : a.getActiveEffects()) {
			if (!inst.getEffect().value().isBeneficial()) {
				harmful.add(inst);
			}
		}
		Vec3 at = PairCast.mid(a);
		c.sphere(PairCast.dust(0xFFF3C4, 0.9F), at, 1.0, 16);
		if (harmful.isEmpty()) {
			return;
		}
		LivingEntity foe = c.nearestEnemy(at, 4 * c.radius, a);
		for (MobEffectInstance inst : harmful) {
			a.removeEffect(inst.getEffect());
			if (foe != null) {
				c.effect(foe, inst.getEffect(), Math.min(6, inst.getDuration() / 20.0), inst.getAmplifier());
			}
		}
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, at, 0.6F, 0.7F);
		if (foe != null) {
			Vec3 there = PairCast.mid(foe);
			c.line(PairCast.shift(0xFFF3C4, 0x7A3EC8, 0.8F), at, there, 3);
			c.later(6, () -> c.sphere(PairCast.dust(0x7A3EC8, 0.9F), there, 0.8, 12));
		}
	}

	/**
	 * Wake Hole: a dust devil that hunts the nearest enemy, drawing those it passes into a black eye, and bursts at
	 * the end. The look: a sand spiral that walks across the ground, its eye darkening.
	 */
	@Pair(a = "dust_devil", b = "singularity", name = "Wake Hole", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A dust devil lands on the spot and chases the nearest enemy for 5 seconds, 0.6 blocks every quarter second. "
			+ "Enemies within 2 blocks are drawn into its black eye, blinded for 2 seconds and scoured for 1 damage every "
			+ "half second. It then bursts: 4 damage to each one caught, and they're flung up.")
	public static void wakeHole(PairCast c) {
		Vec3[] eye = {c.ground(c.point())};
		c.sound(SoundEvents.BREEZE_IDLE_GROUND, eye[0], 0.9F, 0.8F);
		c.every(5, 20, frame -> {
			Vec3 at = eye[0];
			LivingEntity chase = c.nearestEnemy(at, 12 * c.radius, null);
			if (chase != null) {
				Vec3 flat = new Vec3(chase.getX() - at.x, 0, chase.getZ() - at.z);
				if (flat.lengthSqr() > 0.01) {
					eye[0] = at.add(flat.normalize().scale(0.6));
				}
			}
			Vec3 now = eye[0];
			c.spiral(PairCast.dust(0xD9C7A3, 1.0F), now, 1.4 * c.radius, 2.6, 2, 16);
			c.spiral(PairCast.shift(0xD9C7A3, 0x1A0F2E, 0.9F), now, 0.5, 2.6, 2, 8);
			for (LivingEntity e : c.enemiesNear(now, 2 * c.radius)) {
				c.pullTo(e, now.add(0, 1, 0), 0.5);
				if (frame % 2 == 0) {
					c.strike(e, c.power);
					c.effect(e, MobEffects.BLINDNESS, 2, 0);
				}
			}
		});
		c.later(100, () -> {
			Vec3 at = eye[0];
			c.sound(SoundEvents.WIND_CHARGE_BURST, at, 1.0F, 0.6F);
			c.wave(PairCast.dust(0xD9C7A3, 1.2F), at.add(0, 0.5, 0), 24, 0.5);
			for (LivingEntity e : c.enemiesNear(at, 2 * c.radius)) {
				c.hurt(e, 4 * c.power);
				c.knockFrom(e, at, 0.6, 0.6);
			}
		});
	}

	/**
	 * Keening Edge: a blade of sound cuts from you to the target, through whatever is in between, then the boom and
	 * a gale round the impact. The look: a shimmering blade line, a pale shockwave, and a green ring of gale.
	 */
	@Pair(a = "razorgale", b = "sonic_boom", name = "Keening Edge", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A blade of sound cuts from you to where it lands, through walls: each enemy within 1.2 blocks of that line "
			+ "takes 4 damage and bleeds. The target takes 8 more that ignores armour; half a second later the gale whirls "
			+ "round it: enemies within 3 blocks take 2 and bleed.")
	public static void keeningEdge(PairCast c) {
		Vec3 from = c.origin();
		Vec3 to = c.point();
		LivingEntity target = c.firstEnemy();
		Vec3 impact = target != null ? PairCast.mid(target) : to;
		Set<LivingEntity> cut = new LinkedHashSet<>();
		int samples = Math.max(2, (int) Math.ceil(from.distanceTo(impact) / 2));
		for (int i = 0; i <= samples; i++) {
			Vec3 p = from.add(impact.subtract(from).scale((double) i / samples));
			cut.addAll(c.enemiesNear(p, 1.2 * c.radius));
		}
		c.sound(SoundEvents.BREEZE_CHARGE, from, 0.7F, 1.5F);
		// The blade: three frames of a shimmering line from you to the impact.
		c.every(2, 3, frame -> c.line(PairCast.shift(0xE6F7FF, 0x6FA8FF, 0.6F), from, impact, 2 + frame));
		c.later(6, () -> {
			for (LivingEntity e : cut) {
				if (c.here(e)) {
					c.strike(e, 4 * c.power);
					c.mark(e, Reactions.Mark.BLEEDING);
				}
			}
			if (target != null && c.here(target)) {
				c.hurt(target, 8 * c.power);
			}
			c.particles(ParticleTypes.SONIC_BOOM, impact, 1, 0, 0);
			c.sound(SoundEvents.WARDEN_SONIC_BOOM, impact, 0.4F, 1.8F);
			c.wave(PairCast.dust(0xB8D8FF, 1.0F), impact, 18, 0.4);
		});
		c.later(10, () -> {
			c.sound(SoundEvents.BREEZE_SLIDE, impact, 0.8F, 1.4F);
			c.every(2, 4, frame -> c.ring(PairCast.dust(0xB8FFEE, 0.8F), impact,
				3 * c.radius * (0.4 + frame * 0.2), 22, frame * 0.5));
			for (LivingEntity e : c.enemiesNear(impact, 3 * c.radius)) {
				c.strike(e, 2 * c.power);
				c.mark(e, Reactions.Mark.BLEEDING);
			}
		});
	}

	/**
	 * Snatch Swap: you and the enemy trade places through the void, and it's left reeling. The look: a wind column
	 * and a void column on the two spots, a crossing arc between them, then rings where each one comes down.
	 */
	@Pair(a = "disarm", b = "warp", name = "Snatch Swap", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "You and the first enemy hit swap places through the void, and you're unseen for 1 second. The enemy is left "
			+ "reeling: Weakness I for 3 seconds, Slowness II and Nausea for 2. Not bosses; with no enemy, nothing happens.")
	public static void snatchSwap(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null || !c.movable(t)) {
			return;
		}
		LivingEntity self = c.caster;
		Vec3 mine = self.position();
		Vec3 theirs = t.position();
		c.sound(SoundEvents.PORTAL_TRIGGER, mine, 0.6F, 1.6F);
		c.spiral(PairCast.dust(0x9FE7FF, 0.9F), mine, 0.6, 2.0, 2, 14);
		c.spiral(PairCast.dust(0x2C1A4F, 1.1F), theirs, 0.6, 2.0, 2, 14);
		if (!c.blink(self, theirs)) {
			return;
		}
		if (!c.blink(t, mine)) {
			c.blink(self, mine);
			return;
		}
		c.effect(self, MobEffects.INVISIBILITY, 1, 0);
		c.effect(t, MobEffects.WEAKNESS, 3, 0);
		c.effect(t, MobEffects.SLOWNESS, 2, 1);
		c.effect(t, MobEffects.NAUSEA, 2, 0);
		// The crossing: two frames of an arc between the old spots.
		c.every(3, 2, frame -> c.arc(PairCast.shift(0x9FE7FF, 0x2C1A4F, 0.8F), mine.add(0, 1, 0), theirs.add(0, 1, 0), 1.4, 20));
		c.later(6, () -> {
			Vec3 now = self.position();
			Vec3 there = t.position();
			c.ring(PairCast.dust(0x9FE7FF, 1.0F), now.add(0, 0.2, 0), 1.0, 20, 0);
			c.sound(SoundEvents.BREEZE_DEFLECT, now, 0.8F, 1.2F);
			c.ring(PairCast.dust(0x2C1A4F, 1.2F), there.add(0, 0.2, 0), 1.0, 20, 0.2);
			c.sound(SoundEvents.SHULKER_BULLET_HIT, there, 0.7F, 0.6F);
		});
	}

	/**
	 * Zip Step: you zip ahead along the wind, stopping at any wall, and each creature you pass is stung. The look:
	 * a gust trail behind you, and a crackle along the path once you land.
	 */
	@Pair(a = "beeline", b = "zipper", name = "Zip Step", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration", "radius"},
		text = "You zip straight ahead up to 6 blocks, stopping before any wall, and land without a fall. Each enemy within "
			+ "1.5 blocks of your path is stung for 2 damage and slowed (Slowness I for 1 second).")
	public static void zipStep(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 flat = new Vec3(c.dir().x, 0, c.dir().z);
		if (flat.lengthSqr() < 1.0E-4) {
			Vec3 look = self.getLookAngle();
			flat = new Vec3(look.x, 0, look.z);
		}
		if (flat.lengthSqr() < 1.0E-4) {
			return;
		}
		Vec3 step = flat.normalize();
		Vec3 start = self.position();
		boolean[] stopped = {false};
		Set<LivingEntity> stung = new HashSet<>();
		c.sound(SoundEvents.BREEZE_CHARGE, start, 0.7F, 1.3F);
		c.every(2, 6, frame -> {
			if (stopped[0]) {
				return;
			}
			Vec3 from = self.position();
			Vec3 to = from.add(step);
			if (!clear(c, from, to, self) || !c.blink(self, to)) {
				stopped[0] = true;
				return;
			}
			Vec3 now = self.position();
			c.particles(ParticleTypes.GUST, now.add(0, 0.5, 0), 6, 0.3, 0.05);
			for (LivingEntity e : c.enemiesNear(now.add(0, 1, 0), 1.5 * c.radius)) {
				if (stung.add(e)) {
					c.hurt(e, 2 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 1, 0);
					c.sound(SoundEvents.PLAYER_ATTACK_WEAK, PairCast.mid(e), 0.5F, 1.6F);
				}
			}
		});
		c.later(12, () -> {
			Vec3 end = self.position();
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, start.add(0, 1, 0), end.add(0, 1, 0), 0.3, 2);
			c.sound(SoundEvents.END_PORTAL_FRAME_FILL, end, 0.5F, 1.6F);
		});
	}

	/** Whether the air from {@code from} to {@code to} is clear at feet and at head height. */
	private static boolean clear(PairCast c, Vec3 from, Vec3 to, LivingEntity self) {
		for (double h : new double[] {0.3, 1.5}) {
			Vec3 a = from.add(0, h, 0);
			Vec3 b = to.add(0, h, 0);
			if (c.level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self)).getType()
				!= HitResult.Type.MISS) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Hushed Nest: a pale dome of cloud settles on the landing spot. Allies drift down slowly with a shield of
	 * absorption; enemies in it are hushed, and the nearest of them most of all.
	 */
	@Pair(a = "cushion", b = "infinity", name = "Hushed Nest", element = "void", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "A pale hush settles on the landing spot. Allies within 4 blocks drift down slowly (Slow Falling for 8 seconds) "
			+ "with 4 absorption for 6 seconds. Enemies within 5 blocks are hushed for 6 seconds: Slowness III within 2 blocks, "
			+ "Slowness I beyond.")
	public static void hushedNest(PairCast c) {
		Vec3 at = c.point();
		double r = 4 * c.radius;
		c.sound(SoundEvents.WOOL_PLACE, at, 1.0F, 0.5F);
		// A dome of cloud swells out over the landing spot, five frames.
		c.every(4, 5, frame -> c.sphere(PairCast.dust(0xDDF6FF, 1.0F), at.add(0, 0.5, 0), r * (frame + 1) / 5.0, 26));
		for (LivingEntity a : c.alliesNear(at, r)) {
			c.effect(a, MobEffects.SLOW_FALLING, 8, 0);
			c.absorb(a, 4, 6);
		}
		for (LivingEntity e : c.enemiesNear(at, 5 * c.radius)) {
			boolean close = PairCast.mid(e).distanceTo(at) <= 2 * c.radius;
			c.effect(e, MobEffects.SLOWNESS, 6, close ? 2 : 0);
		}
		c.later(30, () -> {
			c.ring(PairCast.dust(0xB5E8FF, 1.0F), at.add(0, 0.3, 0), r, 28, 0);
			c.sound(SoundEvents.AMETHYST_CLUSTER_STEP, at, 0.6F, 0.8F);
		});
	}

	/**
	 * Ink Shroud: a cloud of ink turns allies invisible, and the mobs hunting them lose track. The look: a dark teal
	 * cloud thinning to frost, and a frost thread from each hunter back to the cloud.
	 */
	@Pair(a = "inkveil", b = "withdraw", name = "Ink Shroud", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Up to 8 allies within 4 blocks turn invisible for 4 seconds (10 in water), and any of them below half health "
			+ "also gets Speed II for 4 seconds. Mobs within 6 blocks that were hunting one of them lose their target.")
	public static void inkShroud(PairCast c) {
		Vec3 at = c.point();
		double r = 4 * c.radius;
		List<LivingEntity> veiled = PairCast.first(c.alliesNear(at, r), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.SQUID_SQUIRT, at, 1.0F, 0.8F);
		// The ink: three frames of a dark teal cloud that thins to frost.
		c.every(3, 3, frame -> c.sphere(PairCast.shift(0x1C3B4A, 0xBFF4FF, 1.0F), at.add(0, 1, 0), r * (frame + 1) / 3.0, 26));
		for (LivingEntity a : veiled) {
			c.effect(a, MobEffects.INVISIBILITY, a.isInWater() ? 10 : 4, 0);
			if (a.getHealth() < a.getMaxHealth() / 2) {
				c.effect(a, MobEffects.SPEED, 4, 1);
			}
		}
		// The hunters are checked now and twice more while the veil holds.
		c.every(20, 3, frame -> {
			List<LivingEntity> hidden = c.still(veiled);
			for (LivingEntity e : c.enemiesNear(at, 6 * c.radius)) {
				if (e instanceof Mob mob && c.movable(mob) && mob.getTarget() != null && hidden.contains(mob.getTarget())) {
					mob.setTarget(null);
					c.line(PairCast.shift(0xBFF4FF, 0x1C3B4A, 0.7F), PairCast.mid(e), at.add(0, 1, 0), 3);
					c.sound(SoundEvents.BEACON_DEACTIVATE, PairCast.mid(e), 0.5F, 1.5F);
				}
			}
		});
	}

	/**
	 * Drift Nudge: you lift into the air, float down slowly and drift where you look, while a soft gust pushes
	 * every enemy near you away. The look: a pale gust wave outward and cloud puffs trailing you down.
	 */
	@Pair(a = "feather_fall", b = "nudge", name = "Drift Nudge", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration", "radius"},
		text = "You lift into the air and float down slowly (Slow Falling for 6 seconds), and for 3 seconds in the air you drift "
			+ "the way you look. Every enemy within 4 blocks is nudged away from you, with no harm.")
	public static void driftNudge(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 at = self.position();
		c.sound(SoundEvents.BREEZE_JUMP, at, 0.8F, 1.3F);
		c.effect(self, MobEffects.SLOW_FALLING, 6, 0);
		c.lift(self, 0.45);
		// The nudge: a pale gust rolls out and pushes each enemy near you away.
		c.later(2, () -> {
			c.wave(PairCast.dust(0xE6FFF5, 1.0F), at.add(0, 0.4, 0), 22, 0.4);
			for (LivingEntity e : c.enemiesNear(at, 4 * c.radius)) {
				c.knockFrom(e, at, 0.8, 0.15);
			}
		});
		// The drift: for 3 seconds in the air, you go the way you look.
		c.every(2, 30, frame -> {
			if (self.onGround()) {
				return;
			}
			Vec3 look = self.getLookAngle();
			Vec3 flat = new Vec3(look.x, 0, look.z);
			if (flat.lengthSqr() > 1.0E-4) {
				c.push(self, flat.normalize().scale(0.08));
			}
			c.particles(ParticleTypes.CLOUD, self.position().add(0, 0.2, 0), 2, 0.2, 0.02);
		});
	}
}
