package dev.wildercord.pairs.b024;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs024 {
	private Pairs024() {}

	/**
	 * Shardgust: a gust that carries ice. Shards are driven into the front six: the wind cuts, the cold lodges them,
	 * and they melt two seconds later, soaking each. The look: a pale streak to each target, then a ring of glints.
	 */
	@Pair(a = "icicle", b = "windcut", name = "Shardgust", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A gust drives ice shards into up to 6 enemies: 3 wind and 3 freeze damage each, and a shove back. The shards "
			+ "stay lodged for 2 seconds, then melt: 2 more freeze damage each and soaked.")
	public static void shardgust(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 6);
		for (LivingEntity t : struck) {
			c.strike(t, 3 * c.power);
			c.freeze(t, 3 * c.power);
			c.knockFrom(t, c.origin(), 0.4, 0.1);
		}
		c.sound(SoundEvents.WIND_CHARGE_BURST, c.point(), 0.7F, 1.5F);
		c.every(2, 4, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				if (frame == 0) {
					c.line(PairCast.shift(0xE6FFFF, 0x7FD6E8, 0.8F), c.origin(), at, 2);
				}
				c.ring(PairCast.dust(0xCFF8FF, 0.9F), at, 0.6 + frame * 0.3, 14, frame * 0.5);
			}
		});
		c.later(40, () -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				c.freeze(t, 2 * c.power);
				c.mark(t, Reactions.Mark.SOAKED);
				c.star(PairCast.dust(0xBFEFFF, 0.7F), at, 8, 1.1, 0.3);
				c.particles(ParticleTypes.SPLASH, at, 12, 0.3, 0.1);
				c.sound(SoundEvents.GLASS_BREAK, at, 0.8F, 1.2F);
			}
		});
	}

	/**
	 * Undertide: a water column lifts up to four enemies, then the current drags them down a second later. The look:
	 * a blue column rising, bubbles climbing off each target, then a splash where they land.
	 */
	@Pair(a = "tidal_lift", b = "undertow", name = "Undertide", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Lifts up to 4 enemies within 4 blocks on a water column, and a second later the undertide drags them down: "
			+ "4 freeze damage (6 with water within 2 blocks of the spot), Slowness III for 3 seconds and soaked.")
	public static void undertide(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(spot, 4 * c.radius), 4);
		boolean wet = waterNear(c, spot);
		for (LivingEntity t : caught) {
			c.lift(t, 0.9);
		}
		c.column(PairCast.shift(0x9FE6FF, 0x2A6FB0, 1.0F), spot, 1.2 * c.radius, 4.0, 24);
		c.sound(SoundEvents.GENERIC_SPLASH, spot, 0.9F, 0.8F);
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.particles(ParticleTypes.BUBBLE_COLUMN_UP, PairCast.mid(t), 6, 0.4, 0.05);
			}
		});
		c.later(20, () -> {
			for (LivingEntity t : c.still(caught)) {
				Vec3 at = PairCast.mid(t);
				c.push(t, new Vec3(0, -1.2, 0));
				c.freeze(t, (wet ? 6 : 4) * c.power);
				c.effect(t, MobEffects.SLOWNESS, 3, 2);
				c.mark(t, Reactions.Mark.SOAKED);
				c.particles(ParticleTypes.SPLASH, at, 16, 0.4, 0.2);
				c.sound(SoundEvents.PLAYER_SPLASH, at, 0.9F, 0.7F);
			}
		});
	}

	/**
	 * Gathering Tide: a pull draws the nearest six into one point, then the tide crashes in on the crowd. The look:
	 * blue threads reeling in to a spot, and a flat disc of spray that spreads when the tide hits.
	 */
	@Pair(a = "pull", b = "tidecall", name = "Gathering Tide", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Pulls up to 6 enemies within 5 blocks into the point over a second, marked pulled. Then the tide crashes in: "
			+ "4 freeze damage to each within 2.5 blocks, plus 1 for each other one there (3 at most), and soaked.")
	public static void gatheringTide(PairCast c) {
		Vec3 spot = c.point();
		List<LivingEntity> gathered = PairCast.first(c.enemiesNear(spot, 5 * c.radius), 6);
		for (LivingEntity t : gathered) {
			c.mark(t, Reactions.Mark.PULLED);
		}
		c.every(5, 5, frame -> {
			for (LivingEntity t : c.still(gathered)) {
				c.pullTo(t, spot, 0.9);
				c.line(PairCast.dust(0x7FD0FF, 0.8F), PairCast.mid(t), spot, 3);
			}
		});
		c.later(20, () -> {
			List<LivingEntity> near = new ArrayList<>();
			for (LivingEntity t : c.still(gathered)) {
				if (PairCast.mid(t).distanceTo(spot) <= 2.5 * c.radius) {
					near.add(t);
				}
			}
			int crowd = near.size();
			for (LivingEntity t : near) {
				c.freeze(t, (4 + Math.min(3, crowd - 1)) * c.power);
				c.mark(t, Reactions.Mark.SOAKED);
				c.particles(ParticleTypes.SPLASH, PairCast.mid(t), 10, 0.3, 0.2);
			}
			c.disc(PairCast.dust(0xD8F6FF, 1.2F), spot, 2.5 * c.radius, 30);
			c.shake(spot, 0.3F, 6);
			c.sound(SoundEvents.GENERIC_SPLASH, spot, 1.0F, 0.6F);
		});
	}

	/**
	 * Rime Mirror: a ward of frost on an ally. The mirror hands back a little cold to whatever comes close, eight
	 * times. The look: a violet disc over the ally, and a chime with a frost ring on each thing the mirror catches.
	 */
	@Pair(a = "mirrorfrost", b = "reflect", name = "Rime Mirror", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "The ally you aim it at (or you, if none) gets 6 absorption for 8 seconds. Eight times, a second apart, the nearest enemy within "
			+ "3 blocks of it takes 1 damage and is chilled for a second.")
	public static void rimeMirror(PairCast c) {
		LivingEntity ward = c.firstAlly() != null ? c.firstAlly() : c.caster;
		c.absorb(ward, 6 * c.power, 8);
		c.every(20, 8, frame -> {
			if (!c.here(ward)) {
				return;
			}
			Vec3 at = PairCast.mid(ward);
			c.disc(PairCast.dust(0xE8D8FF, 1.0F), at, 1.2, 20);
			LivingEntity near = c.nearestEnemy(at, 3, null);
			if (near != null) {
				c.hurt(near, 1 * c.power);
				c.chill(near, 1);
				c.ring(PairCast.shift(0xE8D8FF, 0xFFFFFF, 0.9F), PairCast.mid(near), 0.8, 14, frame);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(near), 0.6F, 1.6F - 0.05F * frame);
			}
		});
	}

	/**
	 * Held Breath: your breath refilled, and bubbles that float the nearest enemies up and slow them until they pop.
	 * The look: a pale sphere swelling round each one as it rises, then a burst of bubbles when it breaks.
	 */
	@Pair(a = "air_pocket", b = "bubble", name = "Held Breath", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Refills your air. Bubbles lift up to 4 enemies within 4 blocks and slow them (Slowness II) as they float: 2.5 "
			+ "seconds for small ones, 2 for medium, 1.5 for large. Then each pops: 4 damage and soaked.")
	public static void heldBreath(PairCast c) {
		LivingEntity me = c.caster;
		me.setAirSupply(me.getMaxAirSupply());
		Vec3 spot = c.point();
		List<LivingEntity> trapped = PairCast.first(c.enemiesNear(spot, 4), 4);
		c.sphere(PairCast.dust(0xBFEFFF, 1.0F), spot, 2.0, 30);
		c.sound(SoundEvents.BUBBLE_POP, spot, 0.8F, 0.9F);
		for (LivingEntity t : trapped) {
			double size = t.getBbWidth() * t.getBbHeight();
			double hold = size < 1.0 ? 2.5 : size < 3.0 ? 2 : 1.5;
			c.effect(t, MobEffects.SLOWNESS, hold, 1);
			int frames = c.ticks(hold) / 2;
			c.every(2, frames, frame -> {
				if (c.here(t)) {
					c.lift(t, 0.08);
					c.sphere(PairCast.dust(0xBFEFFF, 0.8F), PairCast.mid(t), 0.9, 16);
				}
			});
			c.later(c.ticks(hold), () -> {
				if (!c.here(t)) {
					return;
				}
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 4 * c.power);
				c.mark(t, Reactions.Mark.SOAKED);
				c.sphere(PairCast.dust(0xE0FAFF, 1.0F), at, 1.1, 26);
				c.particles(ParticleTypes.BUBBLE_POP, at, 14, 0.5, 0.1);
				c.sound(SoundEvents.BUBBLE_POP, at, 1.0F, 1.3F);
			});
		}
	}

	/**
	 * Feather Crash: slow falling, and a landing from a height throws a gust out. Your air also tops up when it runs
	 * low. The look: a cloud trail on the way down, and a pale ring that blows out from where you land.
	 */
	@Pair(a = "cushion", b = "drown_ward", name = "Feather Crash", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Slow falling for 6 seconds, and any landing from over 4 blocks up in that time throws a gust: 0.75 damage per block fallen past 4 "
			+ "(6 at most) to enemies within 3 blocks, knocked back. For 60 seconds your air tops up when under half, 3 times.")
	public static void featherCrash(PairCast c) {
		LivingEntity me = c.caster;
		c.effect(me, MobEffects.SLOW_FALLING, 6, 0);
		double[] peak = {me.getY()};
		c.every(2, 60, frame -> {
			if (!me.isAlive()) {
				return;
			}
			if (!me.onGround()) {
				peak[0] = Math.max(peak[0], me.getY());
				c.particles(ParticleTypes.CLOUD, PairCast.mid(me), 2, 0.2, 0.01);
				return;
			}
			double drop = peak[0] - me.getY();
			if (drop >= 4) {
				crash(c, me, Math.min(6, 0.75 * (drop - 4)) * c.power);
			}
			peak[0] = me.getY();
		});
		int[] refills = {0};
		c.every(20, 60, frame -> {
			if (me.isAlive() && refills[0] < 3 && me.getAirSupply() < me.getMaxAirSupply() / 2) {
				me.setAirSupply(me.getMaxAirSupply());
				refills[0]++;
				c.sound(SoundEvents.BUBBLE_POP, PairCast.mid(me), 0.5F, 1.4F);
			}
		});
	}

	private static void crash(PairCast c, LivingEntity me, double damage) {
		Vec3 feet = me.position();
		for (LivingEntity t : c.enemiesNear(feet, 3)) {
			c.strike(t, damage);
			c.knockFrom(t, feet, 1.2, 0.4);
		}
		c.ring(PairCast.dust(0xE6FFF6, 1.2F), feet, 3, 28, 0);
		c.later(3, () -> c.ring(PairCast.dust(0xB8FFF0, 0.9F), feet, 1.6, 20, 1));
		c.sound(SoundEvents.WIND_CHARGE_BURST, feet, 1.0F, 0.9F);
		c.punch(0.2F);
	}

	/**
	 * Dewhaven: a mist-shelter that waters and heals whoever stands in it. The look: a pale disc over the spot each
	 * pulse, cloud curling up, and drips falling onto each ally.
	 */
	@Pair(a = "mending_mist", b = "tidebreath", name = "Dewhaven", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A mist 4 blocks round the spot for 7 seconds, pulsing eight times. Each pulse, every ally inside heals 1, is doused, "
			+ "and can breathe water for 2 seconds.")
	public static void dewhaven(PairCast c) {
		Vec3 spot = c.point();
		double reach = 4 * c.radius;
		c.sound(SoundEvents.PLAYER_SPLASH, spot, 0.8F, 1.3F);
		c.every(20, 8, frame -> {
			c.disc(PairCast.dust(0xD6F7FF, 1.0F), spot, reach, 36);
			c.particles(ParticleTypes.CLOUD, spot, 12, reach * 0.5, 0.01);
			for (LivingEntity a : c.alliesNear(spot, reach)) {
				c.heal(a, 1 * c.power);
				c.douse(a);
				c.effect(a, MobEffects.WATER_BREATHING, 2, 0);
				c.particles(ParticleTypes.DRIPPING_WATER, PairCast.mid(a), 4, 0.3, 0.02);
			}
			if (frame == 0) {
				c.sound(SoundEvents.GENERIC_SPLASH, spot, 0.5F, 1.5F);
			}
		});
	}

	/**
	 * Pingdive: a dash along your look that stops at walls, then a sonar ping that lights every enemy around. The look:
	 * a streak of blue along the path, and a ring of sound rolling out from where you stop.
	 */
	@Pair(a = "echolocate", b = "sounding", name = "Pingdive", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"radius"},
		text = "You dash up to 8 blocks along your look (12 in water), stopping at walls. Where you stop, a ping makes every "
			+ "enemy within 12 blocks glow through walls for 10 seconds, and those within 4 blocks dazed (Slowness II) for 3.")
	public static void pingdive(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 look = me.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		Vec3 d = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 feet = me.position();
		double reach = me.isInWater() ? 12 : 8;
		BlockHitResult wall = c.level.clip(new ClipContext(feet.add(0, 0.9, 0), feet.add(0, 0.9, 0).add(d.scale(reach)),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, me));
		double dist = wall.getType() == HitResult.Type.MISS ? reach
			: Math.max(0, wall.getLocation().distanceTo(feet.add(0, 0.9, 0)) - 0.6);
		Vec3 goal = feet.add(d.scale(dist));
		c.line(PairCast.dust(0xB8C8FF, 0.9F), feet.add(0, 0.9, 0), goal.add(0, 0.9, 0), 2);
		c.every(2, 4, frame -> {
			if (me.isAlive()) {
				c.blink(me, feet.lerp(goal, (frame + 1) / 4.0));
				c.particles(ParticleTypes.CLOUD, me.position(), 2, 0.2, 0.02);
			}
		});
		c.later(8, () -> {
			if (!me.isAlive()) {
				return;
			}
			Vec3 at = me.position().add(0, 1, 0);
			for (LivingEntity t : c.enemiesNear(at, 12 * c.radius)) {
				c.effect(t, MobEffects.GLOWING, 10, 0);
				if (PairCast.mid(t).distanceTo(at) <= 4) {
					c.effect(t, MobEffects.SLOWNESS, 3, 1);
				}
			}
			c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.8F, 1.8F);
			c.every(3, 4, frame -> c.ring(PairCast.dust(0x9FD8FF, 1.0F), at, 2 + frame * 3, 24, frame));
		});
	}

	/**
	 * Geyserfall: a leap up on a geyser of wind (higher from water), a hang in the air, then a dive. Where you land,
	 * the crowd is knocked out. The look: a ring spiralling under you on the way up, and a shock ring on landing.
	 */
	@Pair(a = "updraft", b = "upwell", name = "Geyserfall", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You leap about 5 blocks straight up (about 10 from water), hang a moment, then dive. Where you land, every enemy "
			+ "within 3 blocks takes 3 damage and is knocked outward.")
	public static void geyserfall(PairCast c) {
		LivingEntity me = c.caster;
		boolean wet = me.isInWater();
		c.lift(me, wet ? 1.25 : 0.9);
		c.sound(SoundEvents.WIND_CHARGE_BURST, me.position(), 0.8F, wet ? 0.7F : 1.2F);
		boolean[] dived = {false};
		boolean[] slammed = {false};
		c.every(3, 20, frame -> {
			if (!me.isAlive() || slammed[0]) {
				return;
			}
			Vec3 at = me.position();
			if (frame < 4) {
				c.ring(PairCast.shift(0xE6FFFF, 0x8FD8FF, 1.0F), at, 1.0, 16, frame);
				return;
			}
			if (!dived[0]) {
				dived[0] = true;
				c.push(me, new Vec3(0, -1.6, 0));
				c.sound(SoundEvents.PISTON_EXTEND, at, 0.6F, 1.6F);
				return;
			}
			if (!me.onGround()) {
				c.particles(ParticleTypes.GUST, at, 3, 0.3, 0.02);
				return;
			}
			slammed[0] = true;
			slam(c, me);
		});
	}

	private static void slam(PairCast c, LivingEntity me) {
		Vec3 feet = me.position();
		for (LivingEntity t : c.enemiesNear(feet, 3)) {
			c.strike(t, 3 * c.power);
			c.knockFrom(t, feet, 1.0, 0.5);
		}
		c.ring(PairCast.shift(0xFFFFFF, 0x8FD8FF, 1.2F), feet, 1.0, 20, 0);
		c.later(3, () -> c.ring(PairCast.dust(0x8FD8FF, 1.0F), feet, 2.2, 28, 1));
		c.shake(feet, 0.4F, 8);
		c.punch(0.3F);
		c.sound(SoundEvents.WIND_CHARGE_BURST, feet, 1.0F, 0.6F);
	}

	/**
	 * Deep Reel: a grapple hook flies to where you aim and reels you in, further from water. Whatever sits by the
	 * landing spot is dragged in with you. The look: a violet hook-line, and a ring where the hook bites.
	 */
	@Pair(a = "divers_hands", b = "grapple", name = "Deep Reel", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "A hook flies to the point you aim at (12 blocks, 18 from water) and reels you there, stopping at walls. Enemies "
			+ "within 1.5 blocks of where you stop are pulled towards that spot and take 2 damage.")
	public static void deepReel(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 feet = me.position();
		Vec3 aim = c.point();
		double reach = me.isInWater() ? 18 : 12;
		Vec3 offset = aim.subtract(feet);
		Vec3 dir = offset.lengthSqr() < 1.0E-4 ? me.getLookAngle() : offset.normalize();
		double length = offset.length() < 1.0E-4 ? reach : Math.min(reach, offset.length());
		Vec3 end = feet.add(dir.scale(length));
		BlockHitResult wall = c.level.clip(new ClipContext(feet.add(0, 0.9, 0), end.add(0, 0.9, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, me));
		Vec3 stop = wall.getType() == HitResult.Type.MISS ? end
			: feet.add(dir.scale(Math.max(0, wall.getLocation().distanceTo(feet.add(0, 0.9, 0)) - 0.6)));
		c.line(PairCast.dust(0xC8B8FF, 0.8F), feet.add(0, 1.2, 0), stop.add(0, 1.2, 0), 2);
		c.every(2, 5, frame -> {
			if (me.isAlive()) {
				c.blink(me, feet.lerp(stop, (frame + 1) / 5.0));
			}
		});
		c.later(10, () -> {
			for (LivingEntity t : c.enemiesNear(stop, 1.5)) {
				c.pullTo(t, stop, 1.0);
				c.hurt(t, 2 * c.power);
			}
			c.ring(PairCast.dust(0xC8B8FF, 1.1F), stop, 1.2, 20, 0);
			c.sound(SoundEvents.TRIDENT_RIPTIDE_1, stop, 0.8F, 1.1F);
		});
	}

	private static boolean waterNear(PairCast c, Vec3 at) {
		BlockPos centre = BlockPos.containing(at);
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, -2, -2), centre.offset(2, 2, 2))) {
			if (c.level.getFluidState(p).is(Fluids.WATER)) {
				return true;
			}
		}
		return false;
	}
}
