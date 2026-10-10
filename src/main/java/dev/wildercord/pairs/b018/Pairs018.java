package dev.wildercord.pairs.b018;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs018 {
	private Pairs018() {}

	/**
	 * Hamper Ferry: food and sap. The hamper feeds and heals each ally in reach; then a sap parcel is ferried from
	 * you to the two most wounded of them, one after the other, and each visit heals them again.
	 */
	@Pair(a = "picnic", b = "pulse_ferry", name = "Hamper Ferry", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 6 allies each heal 2 and, if they are players, eat 3 hunger. Then a sap parcel visits the two most "
			+ "wounded of them, a second apart, healing each 3 more.")
	public static void hamperFerry(PairCast c) {
		List<LivingEntity> fed = PairCast.first(c.allies(), 6);
		if (fed.isEmpty()) {
			return;
		}
		for (LivingEntity a : fed) {
			c.heal(a, 2 * c.power);
			if (a instanceof Player p) {
				p.getFoodData().eat(3, 0.0F);
			}
			c.ring(PairCast.dust(0xF2D16B, 0.9F), PairCast.mid(a).add(0, 0.3, 0), 0.7, 14, 0);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(fed.get(0)), 0.8F, 0.9F);
		List<LivingEntity> wounded = new ArrayList<>();
		for (LivingEntity a : fed) {
			if (a.getHealth() < a.getMaxHealth()) {
				wounded.add(a);
			}
		}
		wounded.sort(Comparator.comparingDouble(a -> a.getHealth() - a.getMaxHealth()));
		List<LivingEntity> visits = PairCast.first(wounded, 2);
		Vec3 last = c.origin();
		for (int i = 0; i < visits.size(); i++) {
			LivingEntity to = visits.get(i);
			Vec3 from = last;
			last = PairCast.mid(to);
			// The parcel flies across on its beat, lands, and the visit heals.
			c.later(12 + 20 * i, () -> {
				if (!c.here(to)) {
					return;
				}
				Vec3 at = PairCast.mid(to);
				c.arc(PairCast.dust(0x9BD86B, 0.9F), from, at, 1.2, 12);
				c.sound(SoundEvents.BEEHIVE_ENTER, from, 0.6F, 1.3F);
			});
			c.later(18 + 20 * i, () -> {
				if (!c.here(to)) {
					return;
				}
				Vec3 at = PairCast.mid(to);
				c.heal(to, 3 * c.power);
				c.sphere(PairCast.shift(0xB8F06A, 0xF2D16B, 0.9F), at, 0.9, 18);
				c.particles(ParticleTypes.HAPPY_VILLAGER, at, 6, 0.3, 0.02);
				c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, at, 0.7F, 1.2F);
			});
		}
	}

	/**
	 * Afterbrew: a steeping that runs on a timer. Each good effect on the allies gets its extra time as a brew:
	 * bubbles rise first, then the new time flashes out in amber stars and sparkles for a moment.
	 */
	@Pair(a = "potion_steep", b = "prolong", name = "Afterbrew", element = "time", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Up to 6 allies: each good effect lasts 15 seconds longer, and if that leaves 30 seconds or more, a quarter "
			+ "more of it (at most 45 seconds). Never raised past 8 minutes; endless effects are left alone.")
	public static void afterbrew(PairCast c) {
		List<LivingEntity> brewed = PairCast.first(c.allies(), 6);
		if (brewed.isEmpty()) {
			return;
		}
		for (LivingEntity a : brewed) {
			Vec3 at = PairCast.mid(a);
			c.spiral(PairCast.dust(0x6FE6D2, 0.9F), at.add(0, -0.7, 0), 0.5, 2.0, 2, 20);
			c.sound(SoundEvents.BREWING_STAND_BREW, at, 0.8F, 0.9F);
		}
		c.later(20, () -> {
			for (LivingEntity a : c.still(brewed)) {
				extendGoodEffects(c, a);
				Vec3 at = PairCast.mid(a);
				c.star(PairCast.dust(0xFFC24A, 1.0F), at, 6, 0.8, 0.0);
				c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.5F, 1.4F);
			}
			c.every(6, 5, frame -> {
				for (LivingEntity a : c.still(brewed)) {
					c.ring(PairCast.shift(0xFFE08A, 0x6FE6D2, 0.8F), PairCast.mid(a), 0.9 + frame * 0.25, 14, frame * 0.3);
				}
			});
		});
	}

	/** Each good, finite effect on {@code a}: 15 seconds more, a quarter more if that is 30 seconds or longer, capped. */
	private static void extendGoodEffects(PairCast c, LivingEntity a) {
		for (MobEffectInstance e : List.copyOf(a.getActiveEffects())) {
			if (e.isInfiniteDuration() || !e.getEffect().value().isBeneficial()) {
				continue;
			}
			int old = e.getDuration();
			int longer = old + c.ticks(15);
			if (longer >= 600) {
				longer += Math.min(longer / 4, 900);
			}
			int next = Math.max(old, Math.min(longer, 9600));
			if (next > old) {
				a.addEffect(new MobEffectInstance(e.getEffect(), next, e.getAmplifier(), e.isAmbient(), e.isVisible(),
					e.showIcon()), c.caster);
			}
		}
	}

	/**
	 * Sleetfall: frost and lightning. The targets are chilled and hurled away from you; a second later a bolt strikes
	 * where each one came down, and any that are still chilled shatter with cold on top.
	 */
	@Pair(a = "frostwire", b = "tempest", name = "Sleetfall", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 6 enemies: Slowness II for 4 seconds, and a gale hurls each away from you. A second later a bolt strikes "
			+ "where each one landed: 5 lightning damage, and 3 cold more for any still slowed.")
	public static void sleetfall(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), 6);
		Vec3 from = c.origin();
		c.wave(PairCast.dust(0xE8FBFF, 1.0F), from, 16, 0.5);
		c.sound(SoundEvents.GLASS_PLACE, from, 0.9F, 1.5F);
		for (LivingEntity t : struck) {
			c.effect(t, MobEffects.SLOWNESS, 4, 1);
			c.line(PairCast.dust(0xDDF6FF, 0.8F), from, PairCast.mid(t), 4);
			c.knockFrom(t, from, 1.0, 0.5);
		}
		c.later(20, () -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				c.bolt(at);
				c.shock(t, 5 * c.power);
				if (t.hasEffect(MobEffects.SLOWNESS)) {
					c.freeze(t, 3 * c.power);
				}
				c.sphere(PairCast.shift(0xE8FBFF, 0x6FD8FF, 1.0F), at, 1.1, 22);
				c.particles(ParticleTypes.ITEM_SNOWBALL, at, 10, 0.3, 0.2);
				c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, at, 0.5F, 1.3F);
			}
		});
	}

	/**
	 * Torn Gate: a black bolt tears a rift under the first enemy. The rift draws the enemies round it into its centre
	 * over two seconds, then snaps shut on them.
	 */
	@Pair(a = "riftbolt", b = "riftcall", name = "Torn Gate", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A black bolt strikes the first enemy for 6 lightning and tears a rift under it. Up to 4 enemies within 3 blocks "
			+ "of it are drawn in over 2 seconds, then the rift snaps shut: 5 damage to each, and Darkness for 3 seconds.")
	public static void tornGate(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		Vec3 centre = PairCast.mid(first);
		c.zigzag(ParticleTypes.ELECTRIC_SPARK, c.origin(), centre, 0.4, 3);
		c.shock(first, 6 * c.power);
		c.sound(SoundEvents.TRIDENT_THUNDER, centre, 0.7F, 0.6F);
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(centre, 3 * c.radius), 4);
		c.sphere(PairCast.dust(0x1A0830, 1.2F), centre, 1.2, 30);
		c.every(4, 10, frame -> {
			double shrink = 3 * c.radius * (1 - frame / 12.0);
			c.ring(PairCast.shift(0x6CF2FF, 0x7A2BD9, 0.9F), centre, shrink, 16, frame * 0.4);
			for (LivingEntity e : c.still(caught)) {
				c.pullTo(e, centre, 0.5);
			}
		});
		c.later(40, () -> {
			c.wave(PairCast.shift(0x7A2BD9, 0x6CF2FF, 0.9F), centre, 20, 0.45);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, centre, 1.0F, 0.6F);
			for (LivingEntity e : c.still(caught)) {
				c.hurt(e, 5 * c.power);
				c.effect(e, MobEffects.DARKNESS, 3, 0);
			}
		});
	}

	/**
	 * Cloudlift: wind and storm. The enemies are lifted, a thundercloud gathers over them and strikes three times as
	 * they hang there, then a downdraft drives them into the ground.
	 */
	@Pair(a = "thunderhead", b = "updraft", name = "Cloudlift", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Lifts up to 6 enemies into the air. A small thundercloud gathers over them and strikes each three times, "
			+ "2 lightning a strike, one second apart; then a downdraft smashes them down for 4 damage.")
	public static void cloudlift(PairCast c) {
		List<LivingEntity> lifted = PairCast.first(c.enemies(), 6);
		if (lifted.isEmpty()) {
			return;
		}
		Vec3 cloud = c.point().add(0, 4.5, 0);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, c.point(), 0.8F, 0.8F);
		for (LivingEntity t : lifted) {
			c.lift(t, 0.9);
		}
		c.every(4, 12, frame -> c.disc(PairCast.dust(0x5B6B80, 1.4F), cloud, 1.8, 20));
		c.later(10, () -> c.every(10, 3, frame -> {
			for (LivingEntity t : c.still(lifted)) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, cloud, PairCast.mid(t), 0.4, 3);
				c.shock(t, 2 * c.power);
			}
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, cloud, 0.4F, 1.5F);
		}));
		c.later(40, () -> {
			for (LivingEntity t : c.still(lifted)) {
				c.push(t, new Vec3(0, -1.6, 0));
				c.strike(t, 4 * c.power);
				c.wave(PairCast.dust(0xD8E2F0, 0.9F), PairCast.mid(t), 12, 0.3);
			}
			c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, cloud, 0.7F, 0.6F);
		});
	}

	/**
	 * Wingdive: wind and storm. You hop along your look in quick steps, stopped by walls, and land without a fall; a
	 * storm bird then dives onto the spot you came down on.
	 */
	@Pair(a = "soar", b = "thunderbird", name = "Wingdive", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Carries you up to 8 blocks along your look in quick hops (a wall stops you short) and sets you down without fall "
			+ "damage. A storm bird dives where you land: 5 lightning to each enemy within 3 blocks, knocked back.")
	public static void wingdive(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 flat = forward(self);
		c.wave(PairCast.dust(0xE6FBFF, 1.0F), start.add(0, 0.2, 0), 14, 0.35);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, start, 0.8F, 1.4F);
		int hops = 6;
		for (int i = 1; i <= hops; i++) {
			int step = i;
			c.later(2 * i, () -> {
				Vec3 spot = stop(c, self.position(), start.add(flat.scale(8.0 * step / hops)));
				if (spot.distanceTo(self.position()) > 0.2) {
					c.blink(self, spot);
					c.particles(ParticleTypes.CLOUD, self.position().add(0, 0.4, 0), 4, 0.3, 0.02);
				}
			});
		}
		c.later(2 * hops + 4, () -> {
			Vec3 land = self.position();
			Vec3 sky = land.add(0, 6, 0);
			c.line(PairCast.dust(0xBFF4FF, 0.8F), sky, land.add(0, 0.5, 0), 3);
			c.sound(SoundEvents.TRIDENT_THUNDER, land, 0.6F, 1.6F);
			for (LivingEntity e : c.enemiesNear(land, 3 * c.radius)) {
				c.shock(e, 5 * c.power);
				c.knockFrom(e, land, 0.9, 0.3);
			}
			c.wave(PairCast.dust(0xE6FBFF, 1.0F), land.add(0, 0.2, 0), 16, 0.4);
		});
	}

	/**
	 * Thunder Shadow: shadow and storm. You vanish for a second, then reappear behind the first creature the spell hit,
	 * facing its back, and the shadow bolt strikes the crowd round you.
	 */
	@Pair(a = "shadowstep", b = "thunderstep", name = "Thunder Shadow", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power", "duration", "radius"},
		text = "You vanish for a second, then reappear behind the first enemy hit (or where the spell landed, if none), "
			+ "facing its back; a wall stops you short, 24 blocks at most. A shadow bolt strikes each enemy within 2.5 blocks "
			+ "of you for 6 lightning and Slowness I for 2 seconds. If you can't land safely, nothing strikes.")
	public static void thunderShadow(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity mark = c.firstEnemy();
		Vec3 from = self.position();
		Vec3 aim = c.point();
		if (mark != null) {
			aim = mark.position().subtract(forward(mark).scale(1.2));
			self.setYRot(mark.getYRot());
		}
		Vec3 toAim = aim.subtract(from);
		if (toAim.length() > 24) {
			aim = from.add(toAim.normalize().scale(24));
		}
		Vec3 destination = aim;
		c.effect(self, MobEffects.INVISIBILITY, 1, 0);
		c.sphere(PairCast.shift(0xB56CFF, 0x2A0B4A, 0.9F), from.add(0, 1, 0), 1.6, 24);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, from, 0.6F, 1.6F);
		c.later(20, () -> {
			if (!self.isAlive()) {
				return;
			}
			Vec3 end = stop(c, from, destination);
			if (!c.blink(self, end)) {
				return;
			}
			Vec3 at = self.position();
			c.wave(PairCast.dust(0xB56CFF, 1.0F), at.add(0, 0.2, 0), 16, 0.35);
			c.bolt(at);
			c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, at, 0.6F, 0.7F);
			for (LivingEntity e : c.enemiesNear(at, 2.5 * c.radius)) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 5, 0), PairCast.mid(e), 0.3, 3);
				c.shock(e, 6 * c.power);
				c.effect(e, MobEffects.SLOWNESS, 2, 0);
			}
		});
	}

	/**
	 * Gale Cadence: wind and storm. Allies nearby are hurried along with Speed and Jump Boost, and a cadence of chimes
	 * beats out four times, each beat a little heal.
	 */
	@Pair(a = "dynamo_stride", b = "wayfarer_hymn", name = "Gale Cadence", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Allies within 10 blocks of you get Speed I and Jump Boost I for 10 seconds. On a cadence three seconds apart, "
			+ "four beats in all, each heals 1 on every beat, with a chime.")
	public static void galeCadence(PairCast c) {
		LivingEntity self = c.caster;
		List<LivingEntity> marching = c.alliesNear(self.position(), 10 * c.radius);
		for (LivingEntity a : marching) {
			c.effect(a, MobEffects.SPEED, 10, 0);
			c.effect(a, MobEffects.JUMP_BOOST, 10, 0);
		}
		c.ring(PairCast.dust(0xFFE27A, 1.0F), self.position().add(0, 0.2, 0), 1.5, 24, 0);
		c.sound(SoundEvents.NOTE_BLOCK_CHIME, self.position(), 0.9F, 1.2F);
		c.every(60, 4, beat -> {
			for (LivingEntity a : c.still(marching)) {
				Vec3 at = PairCast.mid(a);
				c.heal(a, 1 * c.power);
				c.ring(PairCast.dust(0xB8FFE0, 0.8F), at, 1.0, 12, beat * 0.5);
			}
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, self.position(), 0.6F, 0.8F + beat * 0.2F);
		});
	}

	/**
	 * Hourglass Ward: time. Sand runs through three times: each beat tops the allies' absorption up, and the last
	 * grain also heals; Haste and Speed carry them through the first moments.
	 */
	@Pair(a = "chronoshift", b = "foresight", name = "Hourglass Ward", element = "time", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Up to 6 allies get Haste I and Speed I for 5 seconds. Sand runs through three times, two seconds apart: each "
			+ "gets 4 absorption (topped up, never stacked), and the last grain heals 2.")
	public static void hourglassWard(PairCast c) {
		List<LivingEntity> warded = PairCast.first(c.allies(), 6);
		if (warded.isEmpty()) {
			return;
		}
		for (LivingEntity a : warded) {
			c.effect(a, MobEffects.HASTE, 5, 0);
			c.effect(a, MobEffects.SPEED, 5, 0);
			c.ring(PairCast.dust(0xF5C451, 1.0F), PairCast.mid(a), 0.9, 16, 0);
		}
		c.sound(SoundEvents.BELL_BLOCK, c.origin(), 0.6F, 1.8F);
		c.every(40, 3, pulse -> {
			for (LivingEntity a : c.still(warded)) {
				Vec3 at = PairCast.mid(a);
				c.absorb(a, 4 * c.power, 6);
				c.column(PairCast.shift(0xFFE08A, 0xB0782A, 0.8F), at.add(0, 1.2, 0), 0.6, 1.4, 14);
				if (pulse == 2) {
					c.heal(a, 2 * c.power);
				}
			}
			c.sound(SoundEvents.BELL_BLOCK, c.origin(), 0.5F, 1.2F + pulse * 0.2F);
		});
	}

	/**
	 * Echo Knell: void and time. A shriek hurts each enemy through its armour and blinds it; then a knell tolls at
	 * each spot, and whoever is still standing in it is struck again.
	 */
	@Pair(a = "resonant_shriek", b = "second_bell", name = "Echo Knell", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 6 enemies: 6 damage that ignores armour, and Darkness for 4 seconds. A knell tolls at each spot at 1 and "
			+ "2 seconds: 3 damage to any enemy still within 1.5 blocks of it, each toll.")
	public static void echoKnell(PairCast c) {
		List<LivingEntity> rung = PairCast.first(c.enemies(), 6);
		List<Vec3> spots = new ArrayList<>();
		for (LivingEntity t : rung) {
			Vec3 at = PairCast.mid(t);
			spots.add(at);
			c.hurt(t, 6 * c.power);
			c.effect(t, MobEffects.DARKNESS, 4, 0);
			c.sphere(PairCast.dust(0x2A1B5C, 1.0F), at, 1.0, 22);
			c.particles(ParticleTypes.SCULK_SOUL, at, 8, 0.3, 0.02);
		}
		c.sound(SoundEvents.SCULK_CLICKING, c.origin(), 0.9F, 0.6F);
		for (int toll = 1; toll <= 2; toll++) {
			int beat = toll;
			c.later(20 * toll, () -> {
				for (int k = 0; k < rung.size(); k++) {
					LivingEntity t = rung.get(k);
					Vec3 spot = spots.get(k);
					c.ring(PairCast.dust(0xE8C15A, 1.0F), spot, 1.5, 18, beat * 0.3);
					if (c.here(t) && PairCast.mid(t).distanceTo(spot) <= 1.5) {
						c.wither(t, 3 * c.power);
					}
				}
				c.sound(SoundEvents.BELL_BLOCK, c.origin(), 0.8F, 0.7F);
			});
		}
	}

	// ------------------------------------------------------------------ helpers

	/** The way {@code e} is facing, flat on the ground. */
	private static Vec3 forward(LivingEntity e) {
		double yaw = Math.toRadians(e.getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	/** How far a step from {@code from} to {@code to} gets before a wall: {@code to}, or short of the wall. */
	private static Vec3 stop(PairCast c, Vec3 from, Vec3 to) {
		if (to.distanceTo(from) < 1.0E-3) {
			return to;
		}
		BlockHitResult hit = c.level.clip(new ClipContext(from.add(0, 0.9, 0), to.add(0, 0.9, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster));
		if (hit.getType() == HitResult.Type.MISS) {
			return to;
		}
		Vec3 unit = to.subtract(from).normalize();
		return hit.getLocation().subtract(unit.scale(0.8)).subtract(0, 0.9, 0);
	}
}
