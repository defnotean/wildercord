package dev.wildercord.pairs.b016;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Batch 16: ten hand-made pairs, each with its own mechanic, palette, sounds and beats. */
public final class Pairs016 {
	private Pairs016() {}

	/**
	 * Star Loom: starfire motes streak from the landing spot to the nearest enemies and set them alight; a moment later
	 * the lightning weaves from one mark to the next, so the more of them caught, the harder each one is struck.
	 */
	@Pair(a = "starfire", b = "stormweave", name = "Star Loom", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Starfire motes seek up to 4 enemies within 6 blocks: 2 damage each, alight for 3 seconds. A moment later "
			+ "lightning weaves between them: 3 damage each and 1 more for every other one caught; a target alone takes 2 more.")
	public static void starLoom(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> marked = PairCast.first(c.enemiesNear(at, 6 * c.radius), 4);
		if (marked.isEmpty()) {
			return;
		}
		c.sound(SoundEvents.BLAZE_SHOOT, at, 0.8F, 1.3F);
		for (LivingEntity t : marked) {
			c.burn(t, 2 * c.power);
			c.ignite(t, 3);
		}
		// Five frames of motes streaking from above the spot to each mark.
		c.every(2, 5, frame -> {
			double f = (frame + 1) / 5.0;
			for (LivingEntity t : c.still(marked)) {
				Vec3 p = at.add(0, 1.5, 0).lerp(PairCast.mid(t), f);
				c.particles(PairCast.shift(0xFFD36B, 0xFF5A1F, 0.9F), p, 2, 0.05, 0);
			}
		});
		c.later(10, () -> weave(c, at, c.still(marked)));
	}

	private static void weave(PairCast c, Vec3 at, List<LivingEntity> caught) {
		if (caught.isEmpty()) {
			return;
		}
		// 3 each plus one per other caught; alone, 2 more.
		double each = caught.size() == 1 ? 5 : 2 + caught.size();
		for (LivingEntity t : caught) {
			c.shock(t, each * c.power);
		}
		c.sound(SoundEvents.TRIDENT_THUNDER, at, 0.6F, 1.5F);
		c.shake(at, 0.2F, 8);
		c.every(3, 3, frame -> {
			for (int i = 0; i < caught.size(); i++) {
				Vec3 from = PairCast.mid(caught.get(i));
				Vec3 to = caught.size() == 1 ? at.add(0, 1, 0) : PairCast.mid(caught.get((i + 1) % caught.size()));
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, from, to, 0.5, 2);
			}
		});
	}

	/**
	 * Scald Tide: the tide crashes in, bunching the enemies round the spot and soaking them; half a second later the
	 * steam boils out over the same crowd, scalding and blinding it.
	 */
	@Pair(a = "steam", b = "tidecall", name = "Scald Tide", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "The tide crashes in: 4 damage to up to 8 enemies within 3.5 blocks, pulled into the middle and soaked. Half a "
			+ "second later the steam boils out over them: 5 more damage to each within 2.5 blocks, and Blindness for 3 seconds.")
	public static void scaldTide(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> swept = PairCast.first(c.enemiesNear(at, 3.5 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, at, 1.0F, 0.7F);
		c.particles(ParticleTypes.SPLASH, at, 24, 1.5, 0.1);
		for (LivingEntity t : swept) {
			c.strike(t, 4 * c.power);
			c.mark(t, Reactions.Mark.SOAKED);
			c.pullTo(t, at, 1.2);
		}
		// The wave collapses inward over three frames.
		c.every(2, 3, frame -> c.ring(PairCast.shift(0x9FE8FF, 0x2F6FB0, 0.9F), at.add(0, 0.2, 0),
			3.5 * c.radius * (1 - frame * 0.3), 30, frame * 0.4));
		c.later(10, () -> boil(c, at));
	}

	private static void boil(PairCast c, Vec3 at) {
		c.sound(SoundEvents.LAVA_EXTINGUISH, at, 1.0F, 1.2F);
		c.sphere(PairCast.dust(0xF2F6FA, 1.4F), at.add(0, 1, 0), 2.4 * c.radius, 40);
		for (LivingEntity t : c.enemiesNear(at, 2.5 * c.radius)) {
			c.hurt(t, 5 * c.power);
			c.effect(t, MobEffects.BLINDNESS, 3, 0);
		}
		c.every(3, 4, frame -> c.column(PairCast.dust(0xD8DEE6, 1.0F), at, 2.2 * c.radius, 3, 10));
	}

	/**
	 * Noon Ripple: the noon sun is focused down a beam onto the target, which burns and glows. A second later the
	 * light ripples out from it, and each enemy it reaches gives the caster a little health back.
	 */
	@Pair(a = "ripple", b = "sunscorch", name = "Noon Ripple", element = "fire", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "The noon sun focused on the target: 5 fire damage, alight for 4 seconds and glowing for 6. A second later the "
			+ "light ripples out from it: 4 damage to every other enemy within 3 blocks (6 to undead), and you heal 1 for each struck, 4 at most.")
	public static void noonRipple(PairCast c) {
		LivingEntity target = c.firstEnemy();
		if (target == null) {
			return;
		}
		Vec3 at = PairCast.mid(target);
		c.burn(target, 5 * c.power);
		c.ignite(target, 4);
		c.effect(target, MobEffects.GLOWING, 6, 0);
		c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.9F, 1.6F);
		// The beam drops from the sky onto the target.
		c.line(ParticleTypes.END_ROD, at.add(0, 8, 0), at, 2);
		c.every(3, 3, frame -> c.particles(ParticleTypes.FLAME, at, 6, 0.3, 0.05));
		c.later(20, () -> ripple(c, target, at));
	}

	private static void ripple(PairCast c, LivingEntity target, Vec3 at) {
		c.sound(SoundEvents.BEACON_ACTIVATE, at, 0.8F, 1.5F);
		c.every(2, 4, frame -> c.ring(PairCast.shift(0xFFE27A, 0xFFFFFF, 1.1F), at, (0.8 + frame * 0.7) * c.radius, 28, frame * 0.3));
		int healed = 0;
		for (LivingEntity o : c.enemiesNear(at, 3 * c.radius)) {
			if (o == target) {
				continue;
			}
			c.hurt(o, (o.isInvertedHealAndHarm() ? 6 : 4) * c.power);
			c.line(PairCast.dust(0xFFC83A, 0.8F), at, PairCast.mid(o), 3);
			if (healed < 4) {
				c.heal(c.caster, 1 * c.power);
				healed++;
			}
		}
	}

	/**
	 * Ash Ward: the ally is put out and fire-proofed, and ash falls on them; then for ten seconds a ring of ash
	 * pulses each second, setting alight and blinding any enemy that comes close to a warded ally.
	 */
	@Pair(a = "ashen_veil", b = "emberguard", name = "Ash Ward", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Puts up to 8 allies out and gives them Fire Resistance for 15 seconds, and wreathes them in ash for 10 seconds. "
			+ "Each second, enemies within 2.5 blocks of a warded ally are set alight for 3 seconds and blinded for 1.")
	public static void ashWard(PairCast c) {
		List<LivingEntity> wards = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : wards) {
			c.douse(a);
			c.effect(a, MobEffects.FIRE_RESISTANCE, 15, 0);
			c.column(PairCast.dust(0x8A8580, 1.0F), PairCast.mid(a), 0.9, 2.2, 24);
			c.particles(ParticleTypes.WHITE_ASH, PairCast.mid(a), 16, 0.5, 0.02);
		}
		c.sound(SoundEvents.FIRE_EXTINGUISH, PairCast.mid(c.caster), 0.9F, 1.1F);
		int pulses = Math.max(1, (int) Math.round(10 * c.duration));
		c.later(10, () -> c.every(20, pulses, frame -> {
			for (LivingEntity a : c.still(wards)) {
				Vec3 at = PairCast.mid(a);
				c.ring(PairCast.dust(0xFF8A3D, 0.9F), at, 2.5 * c.radius, 22, frame * 0.5);
				for (LivingEntity e : c.enemiesNear(at, 2.5 * c.radius)) {
					c.ignite(e, 3);
					c.effect(e, MobEffects.BLINDNESS, 1, 0);
					c.particles(ParticleTypes.FLAME, PairCast.mid(e), 6, 0.3, 0.02);
				}
			}
			c.sound(SoundEvents.CAMPFIRE_CRACKLE, PairCast.mid(c.caster), 0.5F, 1.2F);
		}));
	}

	/**
	 * Whiteout Slide: a gale lifts the enemies round the spot off their feet; a second later the snow slide comes
	 * down on whoever is still close, heavy and slowing.
	 */
	@Pair(a = "avalanche", b = "summit_wind", name = "Whiteout Slide", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A gale lifts up to 8 enemies within 3 blocks into the air: 3 damage each. A second later the snow slide comes down on "
			+ "whoever is still within 4 blocks: 6 damage each, and Slowness III for 3 seconds.")
	public static void whiteoutSlide(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemiesNear(at, 3 * c.radius), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.WIND_CHARGE_BURST, at, 1.0F, 0.8F);
		for (LivingEntity t : caught) {
			c.strike(t, 3 * c.power);
			c.lift(t, 0.9);
		}
		// The gale rises as a white ring, carrying the snow upward.
		c.every(2, 4, frame -> c.ring(PairCast.shift(0xFFFFFF, 0xA9D8FF, 1.0F), at.add(0, frame * 0.8, 0),
			(1.5 + frame * 0.5) * c.radius, 26, frame * 0.5));
		c.later(20, () -> slide(c, at, caught));
	}

	private static void slide(PairCast c, Vec3 at, List<LivingEntity> caught) {
		c.sound(SoundEvents.MACE_SMASH_GROUND_HEAVY, at, 1.0F, 0.9F);
		c.shake(at, 0.35F, 10);
		c.sphere(PairCast.dust(0xEAF6FF, 1.2F), at.add(0, 1, 0), 2.5 * c.radius, 28);
		for (LivingEntity t : c.still(caught)) {
			if (PairCast.mid(t).distanceTo(at) <= 4 * c.radius) {
				c.freeze(t, 6 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 3, 2);
			}
		}
		c.every(2, 4, frame -> c.column(ParticleTypes.SNOWFLAKE, at.add(0, 3.5, 0), 3 * c.radius, 3.5, 20));
	}

	/**
	 * Ice Absolution: the allies are cured of their harmful effects and put out, then sealed in ice: nothing hurts
	 * them while it holds, and when it breaks it throws back the enemies round them.
	 */
	@Pair(a = "ashen_mercy", b = "cryostasis", name = "Ice Absolution", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Cures up to 4 allies of their harmful effects and puts out their fire, then seals each in ice for 3 seconds: "
			+ "immune to damage (Resistance V), and 2 health per condition cured (8 at most). When the ice breaks, enemies within "
			+ "3 blocks are thrown back, take 3 damage and are slowed for 3 seconds.")
	public static void iceAbsolution(PairCast c) {
		List<LivingEntity> sealed = PairCast.first(c.allies(), 4);
		for (LivingEntity a : sealed) {
			List<MobEffectInstance> bad = new ArrayList<>();
			for (MobEffectInstance inst : a.getActiveEffects()) {
				if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					bad.add(inst);
				}
			}
			for (MobEffectInstance inst : bad) {
				a.removeEffect(inst.getEffect());
			}
			c.douse(a);
			if (!bad.isEmpty()) {
				c.heal(a, Math.min(8, 2 * bad.size()) * c.power);
			}
			c.effect(a, MobEffects.RESISTANCE, 3, 4);
			c.sound(SoundEvents.BELL_RESONATE, PairCast.mid(a), 0.9F, 1.2F);
			c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, PairCast.mid(a), 0.7F, 0.8F);
			// The shell holds for the whole three seconds, redrawn as it stands.
			c.every(10, 6, frame -> {
				for (LivingEntity s : c.still(List.of(a))) {
					c.sphere(PairCast.shift(0xE8FBFF, 0x7FD4FF, 1.0F), PairCast.mid(s), 1.2 * c.radius, 28);
				}
			});
			c.later(60, () -> {
				if (c.here(a)) {
					shatter(c, a);
				}
			});
		}
	}

	private static void shatter(PairCast c, LivingEntity ally) {
		Vec3 at = PairCast.mid(ally);
		c.sound(SoundEvents.GLASS_BREAK, at, 1.0F, 1.1F);
		c.wave(PairCast.dust(0xCFF4FF, 1.0F), at, 22, 0.35 * c.radius);
		c.shake(at, 0.3F, 8);
		for (LivingEntity e : c.enemiesNear(at, 3 * c.radius)) {
			c.knockFrom(e, at, 1.0, 0.4);
			c.freeze(e, 3 * c.power);
			c.effect(e, MobEffects.SLOWNESS, 3, 1);
		}
	}

	/**
	 * Drowned Silence: a pocket of silence hangs over the spot. Whatever stands in it loses its target, drowns a
	 * little each second and is left nauseous and weakened.
	 */
	@Pair(a = "drowning_word", b = "hush", name = "Drowned Silence", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A pocket of silence within 4 blocks of the spot for 6 seconds. Each enemy in it takes 1 damage a second (the first "
			+ "at once), is nauseous and weakened, and mobs other than bosses lose their target.")
	public static void drownedSilence(PairCast c) {
		Vec3 at = c.point();
		double r = 4 * c.radius;
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, at, 1.0F, 0.6F);
		c.sound(SoundEvents.SCULK_CLICKING, at, 0.8F, 0.5F);
		int pulses = Math.max(1, (int) Math.round(6 * c.duration));
		c.every(20, pulses, frame -> {
			for (LivingEntity e : c.still(c.enemiesNear(at, r))) {
				c.hurt(e, 1 * c.power);
				c.effect(e, MobEffects.NAUSEA, 3, 0);
				c.effect(e, MobEffects.WEAKNESS, 4, 1);
				if (e instanceof Mob m && c.movable(m)) {
					m.setTarget(null);
				}
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(e), 8, 0.4, 0.1);
			}
			c.sphere(PairCast.dust(0x1B1140, 1.1F), at.add(0, 1, 0), r, 30);
			c.column(ParticleTypes.BUBBLE, at, r, 4, 16);
		});
	}

	/**
	 * Glass Storm: plasma strikes the target and freezes it solid, the ice holding a current; half a second later the
	 * ice cracks under a bolt that leaps to the enemies round it.
	 */
	@Pair(a = "freeze", b = "plasma", name = "Glass Storm", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Plasma hits the target for 8 damage (half of it ignoring armour), ionises it, and freezes it solid for 2.5 seconds, "
			+ "slowed to a crawl. Half a second later the ice cracks under a bolt: 4 more damage, and it leaps to up to 3 other "
			+ "enemies within 3 blocks: 3 damage each and frozen for 1 second.")
	public static void glassStorm(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 at = PairCast.mid(t);
		c.hurt(t, 4 * c.power);
		c.strike(t, 4 * c.power);
		c.mark(t, Reactions.Mark.IONISED);
		freezeSolid(c, t, 2.5);
		c.effect(t, MobEffects.SLOWNESS, 2.5, 4);
		c.sound(SoundEvents.TRIDENT_THUNDER, at, 0.8F, 1.4F);
		c.sound(SoundEvents.GLASS_PLACE, at, 0.8F, 0.8F);
		c.zigzag(ParticleTypes.ELECTRIC_SPARK, c.origin(), at, 0.4, 3);
		c.every(2, 4, frame -> c.spiral(PairCast.shift(0xBDEBFF, 0xE8FFFF, 0.8F), at, 0.8, 1.8, 1.5, 16));
		c.later(10, () -> crack(c, t, at));
	}

	private static void crack(PairCast c, LivingEntity t, Vec3 at) {
		if (!c.here(t)) {
			return;
		}
		Vec3 p = PairCast.mid(t);
		c.shock(t, 4 * c.power);
		c.sound(SoundEvents.GLASS_BREAK, p, 1.0F, 1.3F);
		c.star(PairCast.shift(0xFFFFFF, 0x7FE7FF, 0.8F), p, 6, 1.4, 0.2);
		c.shake(p, 0.25F, 8);
		int leapt = 0;
		for (LivingEntity n : c.enemiesNear(p, 3 * c.radius)) {
			if (n == t || leapt == 3) {
				continue;
			}
			leapt++;
			c.shock(n, 3 * c.power);
			freezeSolid(c, n, 1);
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, p, PairCast.mid(n), 0.5, 3);
		}
		c.every(3, 3, frame -> c.column(ParticleTypes.SNOWFLAKE, p, 1.0 * c.radius, 1.2, 12));
	}

	private static void freezeSolid(PairCast c, LivingEntity t, double seconds) {
		t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + c.ticks(seconds)));
	}

	/**
	 * Hollow Rime: rime creeps over each target, slowing it more every second as it unravels; then it freezes solid,
	 * and when the frost shatters it hurts whatever is round it.
	 */
	@Pair(a = "entropy", b = "hoarfrost", name = "Hollow Rime", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Rime creeps over each target for 3 seconds: it slows more each second and unravels it for 1 damage a second. "
			+ "Then it freezes solid for 2 seconds; when the frost shatters, 5 damage, and enemies within 2 blocks take 3 and are "
			+ "slowed for 3 seconds.")
	public static void hollowRime(PairCast c) {
		List<LivingEntity> rimed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		if (rimed.isEmpty()) {
			return;
		}
		// Three beats, a second apart: the rime thickens and the target unravels.
		c.every(20, 3, frame -> {
			for (LivingEntity t : c.still(rimed)) {
				c.hurt(t, 1 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 1.2, frame);
				c.column(PairCast.dust(0xD6F6FF, 0.8F), PairCast.mid(t), 0.7, 1.6, 10);
			}
			c.sound(SoundEvents.SCULK_CLICKING, PairCast.mid(c.caster), 0.4F, 0.6F + frame * 0.2F);
		});
		c.later(60, () -> {
			for (LivingEntity t : c.still(rimed)) {
				freezeSolid(c, t, 2);
				c.sound(SoundEvents.GLASS_PLACE, PairCast.mid(t), 0.8F, 1.0F);
				c.sphere(PairCast.dust(0xB8F0FF, 1.0F), PairCast.mid(t), 1.0, 22);
			}
		});
		c.later(100, () -> {
			for (LivingEntity t : c.still(rimed)) {
				shatterRime(c, t);
			}
		});
	}

	private static void shatterRime(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.freeze(t, 5 * c.power);
		c.sound(SoundEvents.GLASS_BREAK, at, 0.9F, 0.9F);
		c.wave(PairCast.dust(0xE0FAFF, 1.0F), at, 18, 0.35 * c.radius);
		for (LivingEntity n : c.enemiesNear(at, 2 * c.radius)) {
			if (n != t) {
				c.freeze(n, 3 * c.power);
				c.effect(n, MobEffects.SLOWNESS, 3, 1);
				c.line(PairCast.dust(0xE0FAFF, 0.7F), at, PairCast.mid(n), 4);
			}
		}
	}

	/**
	 * Gale Pen: a ring of wind herds the enemies round the spot into its middle and blows back any that stray. A frost
	 * seal under the middle freezes whoever stands on it long enough, once each.
	 */
	@Pair(a = "corral", b = "rime_seal", name = "Gale Pen", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"duration", "radius"},
		text = "Enemies within 5 blocks are pulled into the middle of a wind ring for 6 seconds, and pulled back in whenever they "
			+ "stray past 4 blocks. Whoever stands on the frost seal under the middle for a second freezes solid for 1.5 seconds "
			+ "(1 on players) and takes 3 damage, once each.")
	public static void galePen(PairCast c) {
		Vec3 centre = c.point();
		double wall = 5 * c.radius;
		double seal = 1.5 * c.radius;
		c.sound(SoundEvents.WIND_CHARGE_BURST, centre, 1.0F, 1.2F);
		for (LivingEntity t : c.enemiesNear(centre, wall)) {
			c.pullTo(t, centre, 1.0);
		}
		Map<LivingEntity, Integer> standing = new HashMap<>();
		Set<LivingEntity> sealed = new HashSet<>();
		int frames = Math.max(1, (int) Math.round(24 * c.duration));
		c.every(5, frames, frame -> {
			for (LivingEntity t : c.still(c.enemiesNear(centre, wall))) {
				Vec3 p = PairCast.mid(t);
				double flat = Math.hypot(p.x - centre.x, p.z - centre.z);
				if (flat > 4 * c.radius) {
					c.pullTo(t, centre, 0.8);
				}
				if (flat <= seal) {
					int frames4 = standing.merge(t, 1, Integer::sum);
					if (frames4 >= 4 && sealed.add(t)) {
						freezeSolid(c, t, t instanceof Player ? 1 : 1.5);
						c.freeze(t, 3 * c.power);
						c.sound(SoundEvents.GLASS_PLACE, p, 0.8F, 1.2F);
					}
				} else {
					standing.remove(t);
				}
			}
			c.ring(PairCast.dust(0xDDF7FF, 1.0F), centre.add(0, 0.5, 0), wall, 36, frame * 0.25);
			c.disc(PairCast.dust(0x9FE8FF, 0.9F), centre, seal, 14);
		});
	}
}
