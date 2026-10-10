package dev.wildercord.pairs.b043;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ten hand-made pairs for the foresight, lost-find, lumen, lux, pearl, porpoise, comet, span, brace and staunch runes. */
public final class Pairs043 {
	private Pairs043() {}

	/**
	 * Foreseen Ledger: the ledger reads your allies, and the three most hurt are healed now and again when the foreseen
	 * blow lands. The look: a gold-and-blue ring turning over you, gold threads to each one read, then a chime.
	 */
	@Pair(a = "foresight", b = "lore_reading", name = "Foreseen Ledger", element = "time", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Reads the allies within 8 blocks and picks the three most hurt: each gets 4 health now, then 6 more 1.5 seconds "
			+ "later, with Resistance I for 5 seconds.")
	public static void foreseenLedger(PairCast c) {
		Vec3 from = PairCast.mid(c.caster);
		List<LivingEntity> read = new ArrayList<>(c.alliesNear(c.caster.position(), 8));
		read.sort(Comparator.comparingDouble(LivingEntity::getHealth));
		List<LivingEntity> hurt = PairCast.first(read, 3);
		c.sound(SoundEvents.BOOK_PAGE_TURN, from, 0.8F, 1.1F);
		// The ledger opens: a ring turns over the caster and gold threads run out to each one read.
		c.every(3, 4, frame -> {
			c.sphere(PairCast.shift(0xFFE58A, 0xB9D8FF, 0.9F), from, 0.6 + frame * 0.3, 20);
			for (LivingEntity t : c.still(hurt)) {
				c.line(PairCast.dust(0xFFF2B8, 0.6F), from, PairCast.mid(t), 2);
			}
		});
		for (LivingEntity t : hurt) {
			c.heal(t, 4 * c.power);
		}
		// The foreseen blow lands: each one is healed again, warded, and chimed over.
		c.later(30, () -> {
			for (LivingEntity t : c.still(hurt)) {
				c.heal(t, 6 * c.power);
				c.effect(t, MobEffects.RESISTANCE, 5, 0);
				Vec3 at = PairCast.mid(t);
				c.ring(PairCast.shift(0xFFE58A, 0xB9D8FF, 1.0F), at, 1.2, 24, 0);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.9F, 1.4F);
			}
			c.particles(ParticleTypes.ENCHANT, from, 16, 0.4, 0.2);
		});
	}

	/**
	 * Lost Thread: a rift drags what it struck into its middle, then snaps shut; a thread of gold then leaps from it
	 * to the nearest enemy, three times. The look: a dark violet well closing in gold, then gold sparks hopping away.
	 */
	@Pair(a = "lostfind", b = "riftcall", name = "Lost Thread", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Opens a rift at the point: the enemies it struck are dragged in for a second, then it snaps shut for 5 damage "
			+ "each. Then gold thread leaps three times, once a second, to the nearest enemy within 8 blocks, 3 damage each, "
			+ "each jump skipping the enemy the one before it hit.")
	public static void lostThread(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> caught = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.PORTAL_TRIGGER, at, 0.5F, 1.5F);
		// The rift opens and drags what it struck toward its middle, a thread on each.
		c.every(2, 10, frame -> {
			for (LivingEntity t : c.still(caught)) {
				c.pullTo(t, at, 0.7);
				c.line(PairCast.shift(0x6A2C9E, 0xE8C15A, 0.8F), PairCast.mid(t), at, 2);
			}
			c.sphere(PairCast.shift(0x3A0F4F, 0xE8C15A, 1.0F), at, 2.2 - frame * 0.15, 24);
		});
		// The rift snaps shut.
		c.later(20, () -> {
			c.sound(SoundEvents.ENDERMAN_TELEPORT, at, 0.8F, 0.7F);
			c.shake(at, 0.3F, 8);
			c.wave(ParticleTypes.REVERSE_PORTAL, at, 20, 0.3);
			for (LivingEntity t : c.still(caught)) {
				c.hurt(t, 5 * c.power);
			}
		});
		// The gold thread leaps from the rift to the nearest enemy, once a second, three times.
		LivingEntity[] last = new LivingEntity[1];
		Vec3[] spot = {at};
		c.later(40, () -> c.every(20, 3, jump -> {
			LivingEntity next = c.nearestEnemy(spot[0], 8 * c.radius, last[0]);
			if (next == null) {
				return;
			}
			Vec3 to = PairCast.mid(next);
			c.line(PairCast.shift(0xE8C15A, 0xFFFFFF, 0.7F), spot[0], to, 3);
			c.hurt(next, 3 * c.power);
			c.sound(SoundEvents.LODESTONE_COMPASS_LOCK, to, 0.7F, 1.6F);
			spot[0] = to;
			last[0] = next;
		}));
	}

	/**
	 * Starlit Leash: each enemy is tethered to the point by a thread of starlight, and lamps hang between you and the
	 * point so that whatever stands near them glows. The look: a bright thread per target, pale lamps on the line.
	 */
	@Pair(a = "lumenpath", b = "starlight_tether", name = "Starlit Leash", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 8 enemies are tethered to the point with starlight for 5 seconds and get Slowness I. One that strays 3 "
			+ "blocks is dragged back, taking 1 damage (once a second at most). Up to four lamps hang between you and the point: "
			+ "enemies within 1.5 blocks of one glow for 3 seconds.")
	public static void starlitLeash(PairCast c) {
		Vec3 at = c.point();
		Vec3 from = PairCast.mid(c.caster);
		List<LivingEntity> leashed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<Vec3> lamps = lampsBetween(from, at);
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, at, 0.9F, 1.8F);
		for (LivingEntity t : leashed) {
			c.effect(t, MobEffects.SLOWNESS, 5, 0);
		}
		// Six beats, a second apart: the threads hold, each stray is tugged back, the lamps shine.
		c.every(20, 6, frame -> {
			for (LivingEntity t : c.still(leashed)) {
				Vec3 p = PairCast.mid(t);
				c.line(PairCast.shift(0xFFF4C2, 0x9C7BFF, 0.5F), at, p, 2);
				if (p.distanceTo(at) > 3) {
					c.hurt(t, 1 * c.power);
					c.pullTo(t, at, 1.0);
					c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, p, 0.6F, 1.2F);
				}
			}
			for (Vec3 lamp : lamps) {
				c.particles(ParticleTypes.END_ROD, lamp, 2, 0.2, 0.01);
				for (LivingEntity near : c.enemiesNear(lamp, 1.5)) {
					c.effect(near, MobEffects.GLOWING, 3, 0);
				}
			}
		});
	}

	/**
	 * Dark Mirror: each enemy's light is read, and the darker it stands the harder the strike; the same strike mirrors a
	 * second later. The look: a golden pillar of reading over each target, then a violet ring as the mirror lands.
	 */
	@Pair(a = "lux_reading", b = "twin_star", name = "Dark Mirror", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Reads the light on each enemy (up to 8): 2 damage, plus 1 for every 4 levels of darkness below full light "
			+ "(3 at most). The same strike mirrors one second later.")
	public static void darkMirror(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		double[] dealt = new double[struck.size()];
		for (int i = 0; i < struck.size(); i++) {
			int light = c.level.getMaxLocalRawBrightness(BlockPos.containing(PairCast.mid(struck.get(i))));
			int darkness = Math.max(0, Math.min(3, (15 - light) / 4));
			dealt[i] = (2 + darkness) * c.power;
		}
		// The reading: a golden pillar stands over each one, then the strike.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(struck)) {
				c.column(PairCast.dust(0xFFE9A0, 0.8F), PairCast.mid(t), 0.6, 2.5, 10);
			}
		});
		c.later(12, () -> {
			for (int i = 0; i < struck.size(); i++) {
				LivingEntity t = struck.get(i);
				if (c.here(t)) {
					c.hurt(t, dealt[i]);
					c.sphere(PairCast.shift(0xFFE9A0, 0x7A4BD6, 1.0F), PairCast.mid(t), 0.9, 14);
				}
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, PairCast.mid(c.caster), 0.7F, 1.0F);
		});
		// The mirror: the same strike, again, in violet.
		c.later(32, () -> {
			for (int i = 0; i < struck.size(); i++) {
				LivingEntity t = struck.get(i);
				if (c.here(t)) {
					c.hurt(t, dealt[i]);
					c.ring(PairCast.shift(0x7A4BD6, 0xFFFFFF, 1.0F), PairCast.mid(t), 1.1, 20, 0);
					c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(t), 0.8F, 1.6F);
				}
			}
		});
	}

	/**
	 * Pearl Exchange: you and the creature the spell struck trade places through the void. The look: a cyan thread
	 * between the two, then portal motes sliding from one spot to the other.
	 */
	@Pair(a = "pearl_sight", b = "warp", name = "Pearl Exchange", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "You and the first enemy hit (else the first ally) swap places through the void, and you come out unseen for a "
			+ "second. An enemy is left reeling: Slowness II and Nausea for 2 seconds. You get Conduit Power for 10 seconds, "
			+ "even with nothing to swap with.")
	public static void pearlExchange(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity other = c.firstEnemy();
		if (other == null) {
			for (LivingEntity ally : c.allies()) {
				if (ally != self) {
					other = ally;
					break;
				}
			}
		}
		c.effect(self, MobEffects.CONDUIT_POWER, 10, 0);
		if (other == null || !c.here(other)) {
			c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(self), 0.4F, 1.9F);
			return;
		}
		boolean foe = c.firstEnemy() == other;
		LivingEntity partner = other;
		Vec3 mine = self.position();
		Vec3 theirs = partner.position();
		c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(self), 0.8F, 1.2F);
		c.line(PairCast.shift(0x6BD8FF, 0x9B59FF, 0.8F), PairCast.mid(self), PairCast.mid(partner), 3);
		if (c.blink(partner, mine) && !c.blink(self, theirs)) {
			c.blink(partner, theirs);
		}
		c.effect(self, MobEffects.INVISIBILITY, 1, 0);
		if (foe) {
			c.effect(partner, MobEffects.SLOWNESS, 2, 1);
			c.effect(partner, MobEffects.NAUSEA, 2, 0);
		}
		// The reeling: portal motes at both ends for a moment, then a ring of light at each.
		c.every(4, 4, frame -> {
			c.particles(ParticleTypes.REVERSE_PORTAL, PairCast.mid(self), 10, 0.3, 0.4);
			c.particles(ParticleTypes.PORTAL, PairCast.mid(partner), 10, 0.3, 0.4);
		});
		c.later(10, () -> {
			c.wave(ParticleTypes.PORTAL, PairCast.mid(self), 14, 0.25);
			c.wave(ParticleTypes.PORTAL, PairCast.mid(partner), 14, 0.25);
			c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(partner), 0.5F, 0.8F);
		});
	}

	/**
	 * Pod Leap: a hop on land or a dolphin's leap in water, with no fall from it; the sea creatures near where you
	 * land glow. The look: splash motes trailing the leap, then a splash ring and bubbles on every creature lit.
	 */
	@Pair(a = "porpoise", b = "school_sight", name = "Pod Leap", element = "frost", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "Leaps you the way the spell flew: a hop on land, a dolphin's leap in water, with no fall damage from it. Sea creatures "
			+ "(fish, squid, dolphins, turtles, axolotls) within 16 blocks of the point glow for 12 seconds.")
	public static void podLeap(PairCast c) {
		LivingEntity self = c.caster;
		boolean wet = self.isInWater();
		Vec3 flat = flatDir(c);
		Vec3 leap = flat.scale(wet ? 1.1 : 0.6).add(0, wet ? 0.5 : 0.45, 0);
		c.sound(SoundEvents.GENERIC_SPLASH, PairCast.mid(self), 0.8F, wet ? 1.2F : 1.5F);
		c.push(self, leap);
		// The leap: no fall while it lasts, and spray off the caster as it goes.
		c.every(3, 8, frame -> {
			self.resetFallDistance();
			c.particles(ParticleTypes.DOLPHIN, PairCast.mid(self), 6, 0.4, 0.1);
		});
		Vec3 spot = c.point();
		c.later(20, () -> {
			c.wave(ParticleTypes.SPLASH, spot, 16, 0.3);
			c.sound(SoundEvents.PLAYER_SPLASH, spot, 0.8F, 1.0F);
			int lit = 0;
			for (Entity e : c.level.getEntities((Entity) null, new AABB(spot, spot).inflate(16), Pairs043::seaCreature)) {
				if (lit >= PairCast.MAX_IN_AREA) {
					break;
				}
				LivingEntity living = (LivingEntity) e;
				c.effect(living, MobEffects.GLOWING, 12, 0);
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(living), 4, 0.3, 0.05);
				lit++;
			}
		});
	}

	/**
	 * Lodestar Fall: a lodestone draws what it struck toward the point, then a comet falls there. The look: silver
	 * threads drawing in, a streak of fire down from the sky, and a burst where it lands.
	 */
	@Pair(a = "cometfall", b = "shard_compass", name = "Lodestar Fall", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Drags up to 8 enemies toward the point for 1 second, marked Glowing for 2 seconds. A comet falls on the point "
			+ "1.5 seconds later: 6 damage to every enemy within 3 blocks, and they burn for 2 seconds.")
	public static void lodestarFall(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> drawn = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.LODESTONE_COMPASS_LOCK, at, 1.0F, 0.8F);
		// The lodestone draws: silver threads close in on the point.
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(drawn)) {
				c.pullTo(t, at, 1.0);
				c.effect(t, MobEffects.GLOWING, 2, 0);
				c.line(PairCast.shift(0xD9D9FF, 0x7A5CFF, 0.6F), PairCast.mid(t), at, 2);
			}
		});
		// The streak: a fiery line falling from the sky onto the point.
		c.later(20, () -> c.every(2, 5, frame -> {
			Vec3 top = at.add(0, 12 - frame * 2.4, 0);
			c.line(PairCast.shift(0xFFF4D6, 0xFF6A2B, 1.2F), top, at, 2);
			c.particles(ParticleTypes.FLAME, top, 2, 0.1, 0.02);
		}));
		// The impact.
		c.later(30, () -> {
			c.shake(at, 0.35F, 10);
			c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.8F, 1.4F);
			c.sphere(PairCast.shift(0xFFF4D6, 0xFF6A2B, 1.4F), at, 3, 30);
			c.particles(ParticleTypes.FLAME, at, 20, 0.6, 0.1);
			c.particles(ParticleTypes.SMOKE, at, 10, 0.5, 0.05);
			for (LivingEntity t : c.enemiesNear(at, 3 * c.radius)) {
				c.hurt(t, 6 * c.power);
				c.ignite(t, 2);
			}
		});
	}

	/**
	 * Glasswind: a causeway of glass reaches from you to the point, and whoever stands along it is lifted on the air;
	 * when it shatters they are thrown up. Nothing is built; the glass is light and sound only.
	 */
	@Pair(a = "skylatch", b = "span", name = "Glasswind", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "A causeway of glass reaches from you to the point, up to 16 blocks: allies within 2 blocks of it get 2 hearts of "
			+ "absorption for 10 seconds and Slow Falling for 8 seconds. It shatters 2 seconds later and throws those still on "
			+ "it up.")
	public static void glasswind(PairCast c) {
		Vec3 from = c.caster.position().add(0, 1, 0);
		Vec3 to = c.point();
		Vec3 span = to.subtract(from);
		if (span.length() > 16) {
			to = from.add(span.normalize().scale(16));
		}
		Vec3 end = to;
		List<LivingEntity> walkers = alongGlass(c, from, end);
		c.sound(SoundEvents.GLASS_PLACE, from, 0.8F, 1.6F);
		for (LivingEntity t : walkers) {
			c.absorb(t, 4, 10);
			c.effect(t, MobEffects.SLOW_FALLING, 8, 0);
		}
		// The glass shimmers out along its length for two seconds.
		c.every(3, 7, frame -> {
			c.line(PairCast.dust(0xBFF6FF, 0.7F), from, end, 1.5);
			c.particles(ParticleTypes.END_ROD, from.add(end.subtract(from).scale(0.5)), 2, 0.5, 0.01);
		});
		c.later(40, () -> {
			c.line(PairCast.shift(0xE6FCFF, 0x7FD8FF, 0.9F), from, end, 1.0);
			c.sound(SoundEvents.GLASS_BREAK, end, 0.9F, 1.3F);
			c.wave(PairCast.dust(0xE6FCFF, 0.8F), end, 12, 0.35);
			for (LivingEntity t : c.still(walkers)) {
				c.lift(t, 0.6);
			}
		});
	}

	/**
	 * Statue Stance: allies in the stance take much less damage and gain absorption, then the stance steps into its
	 * next pose: stone dust climbs round each one into a shell, and a stony burst lifts them. The look: grey stone
	 * rising, cream stone bursting out.
	 */
	@Pair(a = "brace", b = "stand_pose", name = "Statue Stance", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Allies within 4 blocks of the point take 60% less damage for 4 seconds and gain 2 hearts of absorption for 6 "
			+ "seconds. Two seconds in, the stance steps to its next pose: a stony burst that lifts each of them a little.")
	public static void statueStance(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> posed = PairCast.first(c.alliesNear(at, 4), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.STONE_PLACE, at, 0.9F, 0.7F);
		for (LivingEntity t : posed) {
			c.effect(t, MobEffects.RESISTANCE, 4, 2);
			c.absorb(t, 4, 6);
		}
		// Four frames: stone dust climbs round each ally into a shell.
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(posed)) {
				c.column(PairCast.shift(0x8A8F98, 0xD9CFA8, 0.9F), t.position(), 0.8, 1.0 + frame * 0.5, 10);
			}
		});
		// The next pose: a cream burst, a small lift, and a stone knock.
		c.later(40, () -> {
			for (LivingEntity t : c.still(posed)) {
				Vec3 mid = PairCast.mid(t);
				c.wave(PairCast.shift(0xD9CFA8, 0x8A8F98, 1.0F), mid, 14, 0.2);
				c.lift(t, 0.35);
				c.sound(SoundEvents.STONE_HIT, mid, 0.7F, 0.9F);
			}
		});
	}

	/**
	 * Reeled Mend: the allies within reach are cleansed of poison and wither, healed and given Regeneration, then
	 * reeled in toward you on a line. The look: a pale line cast out to each, a frost burst on cure, a green line back.
	 */
	@Pair(a = "staunch", b = "tackle_mend", name = "Reeled Mend", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Ends poison and wither on allies within 8 blocks of the point, heals each 3 health and gives Regeneration I for 6 "
			+ "seconds, then reels them in toward you like a catch.")
	public static void reeledMend(PairCast c) {
		Vec3 at = c.point();
		Vec3 from = PairCast.mid(c.caster);
		List<LivingEntity> mended = PairCast.first(c.alliesNear(at, 8), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, from, 0.8F, 1.0F);
		// The line is cast out to each: a white thread, three frames.
		c.every(3, 3, frame -> {
			for (LivingEntity t : c.still(mended)) {
				c.line(PairCast.dust(0xF2FBFF, 0.5F), from, PairCast.mid(t), 2);
			}
		});
		c.later(10, () -> {
			for (LivingEntity t : c.still(mended)) {
				t.removeEffect(MobEffects.POISON);
				t.removeEffect(MobEffects.WITHER);
				c.heal(t, 3 * c.power);
				c.effect(t, MobEffects.REGENERATION, 6, 0);
				Vec3 mid = PairCast.mid(t);
				c.particles(ParticleTypes.SNOWFLAKE, mid, 10, 0.4, 0.1);
				c.particles(ParticleTypes.HAPPY_VILLAGER, mid, 6, 0.4, 0.1);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, mid, 0.7F, 1.6F);
			}
		});
		// The reel: a green line back to you, and each one drawn in.
		c.later(22, () -> {
			for (LivingEntity t : c.still(mended)) {
				c.line(PairCast.shift(0x9BF3C7, 0xFFFFFF, 0.6F), PairCast.mid(t), from, 2);
				c.pullTo(t, from, 1.0);
			}
			c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, from, 0.9F, 1.1F);
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The horizontal way the cast flew (or the way the caster looks, or south if neither). */
	private static Vec3 flatDir(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	/** Evenly spaced lamps (four at most, about 4 blocks apart) between {@code a} and {@code b}. */
	private static List<Vec3> lampsBetween(Vec3 a, Vec3 b) {
		int n = Math.max(1, Math.min(4, (int) Math.ceil(a.distanceTo(b) / 4)));
		List<Vec3> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			out.add(a.add(b.subtract(a).scale((i + 0.5) / n)));
		}
		return out;
	}

	/** The distance from {@code p} to the segment from {@code a} to {@code b}. */
	private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		double f = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(f)));
	}

	/** The allies standing within 2 blocks of the glass from {@code from} to {@code to}, eight at most. */
	private static List<LivingEntity> alongGlass(PairCast c, Vec3 from, Vec3 to) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity a : c.alliesNear(from, 19)) {
			if (segmentDistance(PairCast.mid(a), from, to) <= 2) {
				out.add(a);
			}
		}
		return PairCast.first(out, PairCast.MAX_TARGETS);
	}

	/** The water creatures a leap lights up: fish, squid, dolphins, turtles and axolotls. */
	private static boolean seaCreature(Entity e) {
		if (!(e instanceof LivingEntity)) {
			return false;
		}
		return e.getType() == EntityTypes.COD || e.getType() == EntityTypes.SALMON
			|| e.getType() == EntityTypes.PUFFERFISH || e.getType() == EntityTypes.TROPICAL_FISH
			|| e.getType() == EntityTypes.SQUID || e.getType() == EntityTypes.GLOW_SQUID
			|| e.getType() == EntityTypes.DOLPHIN || e.getType() == EntityTypes.TURTLE
			|| e.getType() == EntityTypes.AXOLOTL;
	}
}
