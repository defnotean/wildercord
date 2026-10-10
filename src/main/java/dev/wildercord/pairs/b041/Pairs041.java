package dev.wildercord.pairs.b041;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs041 {
	private Pairs041() {}

	/**
	 * Sworn Peace: the struck enemies take a blow of 2 at once, and every enemy within 8 blocks of the point is pacified for
	 * five seconds, its target dropped each beat, bosses left free. Then the peace breaks in a flash of gold shards.
	 * The look: a pale gold ring rolling out to 8 blocks, doves of light over the pacified, a bell, then the shatter.
	 */
	@Pair(a = "accord", b = "truce", name = "Sworn Peace", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Each enemy the spell struck takes 2 damage. Every enemy within 8 blocks of the point is pacified for 5 seconds: "
			+ "it stops hunting (bosses are not held). When the peace breaks, each pacified enemy takes 6 damage.")
	public static void swornPeace(PairCast c) {
		Vec3 at = c.ground(c.point()).add(0, 1, 0);
		for (LivingEntity t : c.enemies()) {
			c.hurt(t, 2 * c.power);
		}
		List<Mob> sworn = new ArrayList<>();
		for (LivingEntity t : c.enemiesNear(at, 8 * c.radius)) {
			if (t instanceof Mob mob && c.movable(t)) {
				sworn.add(mob);
			}
		}
		int span = c.ticks(5);
		c.sound(SoundEvents.BELL_BLOCK, at, 1.0F, 1.4F);
		// Wind-up: a pale ring rolls out to the edge of the peace.
		c.every(3, 4, frame -> c.ring(PairCast.shift(0xFFF3C4, 0xE9C46A, 1.0F), at, 2.0 * (frame + 1) * c.radius, 32, frame * 0.2));
		// The pact holds: every 4 ticks each pacified mob is kept from its target, with doves over its head.
		c.every(4, Math.max(1, span / 4), frame -> {
			for (Mob mob : sworn) {
				if (c.here(mob) && c.movable(mob)) {
					mob.setTarget(null);
					mob.getNavigation().stop();
					c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(mob).add(0, mob.getBbHeight() / 2 + 0.4, 0), 2, 0.3, 0.0);
				}
			}
		});
		// Payoff: the peace breaks in gold shards, 6 damage each.
		c.later(span, () -> {
			c.sound(SoundEvents.GLASS_BREAK, at, 1.0F, 1.3F);
			c.sphere(PairCast.shift(0xFFF3C4, 0xE9C46A, 1.2F), at, 1.6 * c.radius, 30);
			for (Mob mob : sworn) {
				if (c.here(mob)) {
					c.hurt(mob, 6 * c.power);
					c.particles(ParticleTypes.ENCHANTED_HIT, PairCast.mid(mob), 8, 0.4, 0.2);
				}
			}
		});
	}

	/**
	 * Assay Glare: the struck enemies glow through walls and are exposed at once, and any invisibility is stripped. A
	 * scanner climbs each of them in three rungs of blue light, then the assay: gold motes read out its worth as damage,
	 * scaled by how much health it has left.
	 */
	@Pair(a = "appraise", b = "reveal", name = "Assay Glare", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 8 targets glow through walls for 10 seconds, are exposed, and lose any invisibility. Then, after 3 seconds, "
			+ "each is assayed: 2 damage plus 1 for every 4 health it has left, 10 at most.")
	public static void assayGlare(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 0.8F, 1.1F);
		for (LivingEntity t : marked) {
			t.removeEffect(MobEffects.INVISIBILITY);
			c.effect(t, MobEffects.GLOWING, 10, 0);
			c.mark(t, Reactions.Mark.EXPOSED);
		}
		// The scanner climbs each target, one rung of pale blue light every 5 ticks.
		c.every(5, 3, frame -> {
			for (LivingEntity t : c.still(marked)) {
				double rise = t.getBbHeight() * (frame + 1) / 3.0;
				c.ring(PairCast.shift(0xBDEBFF, 0x5AA9FF, 0.8F), t.position().add(0, rise, 0), 0.9, 12, frame * 0.5);
			}
		});
		// The assay: gold motes read out each target's worth, and it takes the damage.
		c.later(20, () -> {
			c.sound(SoundEvents.VILLAGER_WORK_LIBRARIAN, c.point(), 0.9F, 1.3F);
			for (LivingEntity t : c.still(marked)) {
				Vec3 mid = PairCast.mid(t);
				c.hurt(t, Math.min(10, 2 + t.getHealth() / 4.0) * c.power);
				c.particles(ParticleTypes.ENCHANT, mid, 14, 0.4, 0.3);
				c.sphere(PairCast.shift(0xFFE7A3, 0xFFFFFF, 0.8F), mid, 0.9, 16);
			}
		});
	}

	/**
	 * Lurehook: bait draws each struck enemy, and up to four more nearby, towards the point. Then the hook is set and reels
	 * every struck enemy to the caster's feet in three tugs. The look: bubbles drifting in, a blue line from each creature
	 * to you, and a splash when the hook sets.
	 */
	@Pair(a = "bait_blessing", b = "tidehook", name = "Lurehook", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Bait draws each struck enemy, and up to 4 others within 4 blocks, towards the point. Then the hook reels each struck "
			+ "enemy to your feet in three tugs: 2 damage per tug, and it is left soaked.")
	public static void lurehook(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> hooked = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<LivingEntity> lured = new ArrayList<>(hooked);
		for (LivingEntity n : c.enemiesNear(at, 4 * c.radius)) {
			if (lured.size() < hooked.size() + 4 && !lured.contains(n)) {
				lured.add(n);
			}
		}
		c.sound(SoundEvents.FISHING_BOBBER_SPLASH, at, 0.9F, 1.1F);
		// The bait: four beats in which the lured are drawn in and bubbles drift off them.
		c.every(5, 4, frame -> {
			for (LivingEntity t : c.still(lured)) {
				c.pullTo(t, at, 0.5);
				c.particles(ParticleTypes.BUBBLE, PairCast.mid(t), 2, 0.3, 0.05);
			}
		});
		// The hook sets, then three tugs, six ticks apart, each reeling the struck enemy towards you.
		c.later(24, () -> {
			c.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, at, 1.0F, 0.9F);
			c.every(6, 3, tug -> {
				for (LivingEntity t : c.still(hooked)) {
					c.hurt(t, 2 * c.power);
					c.pullTo(t, c.caster.position(), 1.2);
					c.line(PairCast.dust(0x8FE3FF, 0.8F), PairCast.mid(t), PairCast.mid(c.caster), 1.5);
					if (tug == 2) {
						c.mark(t, Reactions.Mark.SOAKED);
					}
				}
			});
		});
	}

	/**
	 * Fallen Beacon: a star shard falls on each target from twelve blocks up. It strikes, and three sparks leap to the
	 * nearest other enemies. A beacon beam then stands over each impact for four seconds, burning what is near its foot.
	 * The look: white motes streaking down, a bright flash, yellow sparks, and a column of light pulsing once a second.
	 */
	@Pair(a = "beacon_swell", b = "starshard", name = "Fallen Beacon", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Each target is struck by a falling star shard: 6 damage, and 3 sparks of 3 damage fly to the nearest other enemies "
			+ "within 8 blocks. A beacon beam then stands over its foot for 4 seconds: once a second it deals 1 damage to enemies "
			+ "within 2 blocks.")
	public static void fallenBeacon(PairCast c) {
		List<LivingEntity> struck = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.sound(SoundEvents.BEACON_POWER_SELECT, c.point(), 0.8F, 1.6F);
		// The fall: a streak of star motes from 12 blocks above each target, four frames.
		c.every(3, 4, frame -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 spot = c.ground(PairCast.mid(t));
				c.particles(ParticleTypes.END_ROD, spot.add(0, 12 * (1 - (frame + 1) / 4.0), 0), 3, 0.15, 0.02);
			}
		});
		// Impact: the shard strikes, and three sparks chain to the nearest others.
		c.later(12, () -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 6 * c.power);
				c.sphere(PairCast.shift(0xFFFFFF, 0xFFE27A, 1.1F), at, 1.2, 24);
				c.particles(ParticleTypes.ELECTRIC_SPARK, at, 10, 0.4, 0.2);
				int sparks = 0;
				for (LivingEntity n : c.enemiesNear(at, 8 * c.radius)) {
					if (n != t && sparks < 3) {
						sparks++;
						c.hurt(n, 3 * c.power);
						c.line(PairCast.dust(0xFFE27A, 0.7F), at, PairCast.mid(n), 3);
					}
				}
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, c.point(), 1.0F, 1.6F);
		});
		// The beacon: four pulses, a second apart, over each impact.
		c.later(12, () -> c.every(20, 4, pulse -> {
			for (LivingEntity t : c.still(struck)) {
				Vec3 foot = c.ground(PairCast.mid(t));
				c.column(PairCast.shift(0xFFF6CC, 0xFFFFFF, 0.9F), foot, 0.6, 12, 18);
				for (LivingEntity n : c.enemiesNear(foot, 2 * c.radius)) {
					c.hurt(n, 1 * c.power);
				}
			}
		}));
	}

	/**
	 * Warding Shriek: a sculk shriek at the point strikes each struck enemy for magic damage that ignores armour, and
	 * blinds it in Darkness. A second later the echo returns. Allies near the point are warded by the bell: Resistance.
	 * The look: a wave of sculk souls, a teal chime, and a ring of teal on each warded ally.
	 */
	@Pair(a = "bellward", b = "resonant_shriek", name = "Warding Shriek", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A sculk shriek at the point hits each struck enemy for 6 damage that ignores armour, with Darkness for 6 seconds. "
			+ "A second later the echo strikes them for 3 more. Allies within 6 blocks of the point get Resistance I for 8 seconds.")
	public static void wardingShriek(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> shrieked = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		List<LivingEntity> warded = c.alliesNear(at, 6 * c.radius);
		for (LivingEntity t : warded) {
			c.effect(t, MobEffects.RESISTANCE, 8, 0);
		}
		c.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, at, 0.8F, 1.0F);
		c.wave(ParticleTypes.SCULK_SOUL, at.add(0, 0.5, 0), 28, 0.35);
		for (LivingEntity t : shrieked) {
			c.hurt(t, 6 * c.power);
			c.effect(t, MobEffects.DARKNESS, 6, 0);
			c.line(PairCast.dust(0x2EE6D6, 0.8F), at, PairCast.mid(t), 2);
		}
		// The bell ward: three rings of teal on the allies it covers, a beat apart.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(warded)) {
				c.ring(PairCast.shift(0xBFE9FF, 0x2EE6D6, 0.8F), t.position().add(0, 0.1, 0), 0.9, 14, frame * 0.4);
			}
		});
		// The echo, a second later: a chime, a second wave, and 3 more on whoever is still there.
		c.later(20, () -> {
			c.sound(SoundEvents.BELL_RESONATE, at, 0.7F, 0.9F);
			c.wave(ParticleTypes.SCULK_SOUL, at.add(0, 0.5, 0), 36, 0.5);
			for (LivingEntity t : c.still(shrieked)) {
				c.hurt(t, 3 * c.power);
			}
		});
	}

	/**
	 * Whirlpool Spit: a whirlpool opens at the point for three seconds. Each enemy within 3 blocks is drawn into its middle,
	 * and takes a point of damage each second. Then it spits them all out: knocked away, soaked, and struck again.
	 * The look: a ring of bubbles turning in and tightening, a splash and a bell when it spits.
	 */
	@Pair(a = "bobber_bell", b = "tidewrit", name = "Whirlpool Spit", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A whirlpool opens at the point for 3 seconds: each enemy within 3 blocks is drawn to its middle and takes 1 damage "
			+ "a second. Then it spits them out: 4 damage each, soaked, and knocked away.")
	public static void undertow(PairCast c) {
		Vec3 at = c.ground(c.point()).add(0, 0.3, 0);
		double reach = 3 * c.radius;
		List<LivingEntity> sucked = new ArrayList<>(c.enemiesNear(at, reach));
		c.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, at, 1.0F, 0.9F);
		// The whirl: twelve frames, four ticks apart; the ring tightens as the creatures are drawn in.
		c.every(4, 12, frame -> {
			c.ring(PairCast.dust(0x9FE7FF, 0.9F), at, reach * (1 - frame / 14.0), 20, frame * 0.7);
			for (LivingEntity t : c.still(sucked)) {
				c.pullTo(t, at, 0.5);
				if (frame % 5 == 0) {
					c.hurt(t, 1 * c.power);
				}
			}
		});
		// The spit, at the end of the three seconds.
		c.later(60, () -> {
			c.sound(SoundEvents.FISHING_BOBBER_SPLASH, at, 1.0F, 0.8F);
			c.sound(SoundEvents.BELL_BLOCK, at, 0.7F, 1.6F);
			c.wave(PairCast.dust(0xCDF6FF, 0.9F), at, 28, 0.45);
			for (LivingEntity t : c.still(sucked)) {
				c.hurt(t, 4 * c.power);
				c.mark(t, Reactions.Mark.SOAKED);
				c.knockFrom(t, at, 1.2, 0.5);
			}
		});
	}

	/**
	 * Kindling Camp: a camp of six blocks round the point. Allies inside are doused and get Fire Resistance; the embers spiral
	 * in to make the ward, which flares four times, two seconds apart, setting every enemy inside alight.
	 * The look: embers spiralling inward, then orange rings flaring over the camp.
	 */
	@Pair(a = "camp_ward", b = "emberguard", name = "Kindling Camp", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"radius", "duration"},
		text = "Wards a camp of 6 blocks round the point. Each ally inside is doused and gets Fire Resistance for 30 seconds. The ward "
			+ "flares four times, two seconds apart, and sets each enemy inside alight for 3 seconds.")
	public static void emberWard(PairCast c) {
		Vec3 camp = c.ground(c.point());
		double r = 6 * c.radius;
		for (LivingEntity t : c.alliesNear(camp, r)) {
			c.douse(t);
			c.effect(t, MobEffects.FIRE_RESISTANCE, 30, 0);
		}
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, camp, 1.0F, 0.9F);
		// Embers spiral in from the edge of the camp.
		c.every(4, 8, frame -> c.ring(PairCast.shift(0xFFB347, 0xFF5A1F, 0.9F), camp.add(0, 0.2, 0), r * (1 - frame / 8.0), 24, frame * 0.6));
		// The ward flares: four times, two seconds apart.
		c.every(40, 4, flare -> {
			c.ring(PairCast.shift(0xFFD27A, 0xFF5A1F, 1.0F), camp.add(0, 0.2, 0), r, 36, flare);
			c.sound(SoundEvents.FIRECHARGE_USE, camp, 0.6F, 1.1F);
			for (LivingEntity n : c.enemiesNear(camp, r)) {
				c.ignite(n, 3);
				c.particles(ParticleTypes.SMALL_FLAME, PairCast.mid(n), 6, 0.3, 0.05);
			}
		});
	}

	/**
	 * Chalk Zip: a dust line is chalked from you to the point (or 12 blocks along it). You zip along it in six quick steps,
	 * stopping at the first wall. The look: a white chalk line, violet portal motes trailing you, and a whoosh at the end.
	 */
	@Pair(a = "chalk_line", b = "zipper", name = "Chalk Zip", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"radius"},
		text = "Chalks a dust line from you towards the point, up to 12 blocks, then zips you along it in six quick steps. "
			+ "You stop at the first wall, never through it.")
	public static void chalkZip(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 from = self.position();
		Vec3 aim = c.point().subtract(from);
		double reach = 12 * c.radius;
		Vec3 want = aim.length() > reach ? from.add(aim.normalize().scale(reach)) : c.point();
		Vec3 goal = c.ground(want);
		Vec3 run = goal.subtract(from);
		if (run.lengthSqr() < 1.0E-4) {
			return;
		}
		run = run.normalize();
		BlockHitResult wall = c.level.clip(new ClipContext(from.add(0, 0.9, 0), goal.add(0, 0.9, 0),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		Vec3 stop = wall.getType() == HitResult.Type.MISS ? goal : wall.getLocation().subtract(run.scale(0.6));
		if (stop.distanceTo(from) < 1.5) {
			c.sound(SoundEvents.WOOL_STEP, from, 0.6F, 0.8F);
			return;
		}
		// The chalk line, drawn at once.
		c.line(PairCast.dust(0xF2F2EA, 0.7F), from.add(0, 0.2, 0), stop.add(0, 0.2, 0), 4);
		c.sound(SoundEvents.SCULK_CLICKING, from, 0.8F, 1.2F);
		c.punch(0.1F);
		// Six steps, three ticks apart, each leaving violet motes where you were.
		c.every(3, 7, frame -> {
			if (frame == 0) {
				return;
			}
			c.blink(self, from.lerp(stop, frame / 6.0));
			c.particles(ParticleTypes.REVERSE_PORTAL, self.position().add(0, 1, 0), 8, 0.3, 0.1);
			if (frame == 6) {
				c.sound(SoundEvents.WIND_CHARGE_BURST, stop, 0.7F, 1.3F);
			}
		});
	}

	/**
	 * Chalk Verdict: a line of chalk is drawn from you to the point, and the decree is read. Each struck enemy takes 2 damage
	 * at once. Two seconds later, any still standing within 1.5 blocks of the line takes 6 more and is slowed for a second.
	 * The look: a chalk line writing itself out, a bell, and a flash of gold along the line at the verdict.
	 */
	@Pair(a = "chalkline", b = "decree", name = "Chalk Verdict", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Chalks a line from you to the point and reads a decree: each struck enemy takes 2 damage. Two seconds later, each "
			+ "still within 1.5 blocks of the line takes 6 more and is slowed for 1 second.")
	public static void chalkVerdict(PairCast c) {
		Vec3 from = c.origin();
		Vec3 to = c.ground(c.point()).add(0, 0.2, 0);
		List<LivingEntity> judged = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : judged) {
			c.hurt(t, 2 * c.power);
		}
		c.sound(SoundEvents.NOTE_BLOCK_BASS, to, 0.8F, 0.6F);
		// The line is written out over five beats.
		c.every(3, 5, frame -> c.line(PairCast.dust(0xF4F1DE, 0.7F), from, from.lerp(to, (frame + 1) / 5.0), 3));
		// The verdict, two seconds on.
		c.later(40, () -> {
			c.sound(SoundEvents.BELL_RESONATE, to, 1.0F, 0.7F);
			c.line(PairCast.shift(0xFFF4C2, 0xFFD27A, 1.0F), from, to, 4);
			for (LivingEntity t : c.still(judged)) {
				Vec3 mid = PairCast.mid(t);
				if (segmentDistance(mid, from, to) <= 1.5) {
					c.hurt(t, 6 * c.power);
					c.effect(t, MobEffects.SLOWNESS, 1, 2);
					c.column(PairCast.shift(0xFFFFFF, 0xF4F1DE, 0.9F), mid, 0.5, 1.5, 14);
				}
			}
		});
	}

	/**
	 * Betrothal: the caster is bound to an ally (or themself, alone) for ten seconds. Both get absorption, and every two
	 * seconds the ally heals while within 16 blocks. Grown farm animals near the point fall in love at once.
	 * The look: a pink rope of hearts between the two, a lead's tie, and hearts over the animals.
	 */
	@Pair(a = "courtship", b = "soulbond", name = "Betrothal", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Binds you to an ally for 10 seconds (yourself, if none): each of you gets 4 absorption health, and every 2 seconds "
			+ "the ally heals 2 while within 16 blocks. Up to 6 grown farm animals within 5 blocks of the point fall in love.")
	public static void betrothal(PairCast c) {
		LivingEntity partner = c.firstAlly() != null ? c.firstAlly() : c.caster;
		Vec3 at = c.point();
		c.sound(SoundEvents.LEAD_TIED, c.caster.position(), 0.8F, 1.1F);
		// The herd: grown animals that are not already in love.
		List<Animal> herd = c.level.getEntitiesOfClass(Animal.class, new AABB(at, at).inflate(5),
			a -> a.getAge() == 0 && !a.isInLove());
		int bred = 0;
		for (Animal a : herd) {
			if (bred >= 6) {
				break;
			}
			bred++;
			if (c.caster instanceof Player lover) {
				a.setInLove(lover);
			}
			c.particles(ParticleTypes.HEART, PairCast.mid(a).add(0, a.getBbHeight() / 2 + 0.2, 0), 3, 0.3, 0.02);
		}
		c.absorb(c.caster, 4 * c.power, 10);
		c.absorb(partner, 4 * c.power, 10);
		// The bond: a rope of hearts between you, renewed every two seconds, and the ally's heal with it.
		c.every(40, 5, pulse -> {
			if (!c.here(partner) || partner.distanceTo(c.caster) > 16) {
				return;
			}
			c.heal(partner, 2 * c.power);
			c.line(PairCast.dust(0xFF9EC7, 0.8F), PairCast.mid(c.caster), PairCast.mid(partner), 2);
			c.particles(ParticleTypes.HEART, PairCast.mid(partner).add(0, 0.6, 0), 3, 0.3, 0.02);
			c.sound(SoundEvents.VILLAGER_YES, PairCast.mid(partner), 0.5F, 1.3F);
		});
	}

	/** How far {@code p} is from the segment {@code a} to {@code b}. */
	private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		double f = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(f)));
	}
}
