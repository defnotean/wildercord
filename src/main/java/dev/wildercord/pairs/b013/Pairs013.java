package dev.wildercord.pairs.b013;

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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs013 {
	private Pairs013() {}

	/**
	 * Gnawing Brood: the silverfish gnaw while the parasite drinks. Each bite and each drink is drawn as it lands; if the
	 * host dies with the parasite still in it, the parasite jumps to the nearest enemy and carries on with what it had left.
	 */
	@Pair(a = "infest", b = "parasite", name = "Gnawing Brood", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Silverfish gnaw the target: 1 damage every half second for 4 seconds, with Slowness I. A parasite drains 1 "
			+ "damage a second from it for 6 seconds (you heal that much), with Poison I. If the host dies first, the parasite "
			+ "leaps to the nearest enemy within 6 blocks, poisoning it for the seconds it had left.")
	public static void gnawingBrood(PairCast c) {
		LivingEntity host = c.firstEnemy();
		if (host == null) {
			return;
		}
		c.effect(host, MobEffects.SLOWNESS, 4, 0);
		c.effect(host, MobEffects.POISON, 6, 0);
		c.sound(SoundEvents.SILVERFISH_AMBIENT, PairCast.mid(host), 0.9F, 0.8F);
		c.every(10, 8, bite -> {
			if (!c.here(host)) {
				return;
			}
			Vec3 at = PairCast.mid(host);
			c.particles(PairCast.dust(0x6B5B45, 1.0F), at, 10, 0.5, 0.02);
			c.strike(host, 1 * c.power);
			c.sound(SoundEvents.SILVERFISH_STEP, at, 0.5F, 1.0F + 0.05F * bite);
		});
		Vec3[] at = {PairCast.mid(host)};
		boolean[] leapt = {false};
		int drains = Math.max(1, (int) Math.round(6 * c.duration));
		c.every(20, drains, second -> {
			if (c.here(host)) {
				at[0] = PairCast.mid(host);
				c.hurt(host, 1 * c.power);
				c.heal(c.caster, 1 * c.power);
				c.line(PairCast.dust(0x8E0F1E, 0.9F), at[0], PairCast.mid(c.caster), 3);
				return;
			}
			if (!leapt[0]) {
				leapt[0] = true;
				LivingEntity next = c.nearestEnemy(at[0], 6 * c.radius, host);
				if (next != null) {
					c.effect(next, MobEffects.POISON, drains - second, 0);
					c.line(PairCast.dust(0x8E0F1E, 0.9F), at[0], PairCast.mid(next), 3);
					c.sound(SoundEvents.SLIME_JUMP, PairCast.mid(next), 0.7F, 0.6F);
				}
			}
		});
	}

	/**
	 * Crimson Bloom: the sap rises in you, and the crimson moss spreads on the ground where it landed. For six seconds the
	 * moss draws the life from the enemies standing in it, and gives it to whichever of you is most wounded.
	 */
	@Pair(a = "blood_moss", b = "sapflow", name = "Crimson Bloom", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "You gain Regeneration I for 8 seconds. Crimson moss spreads 3 blocks round the point for 6 seconds: each "
			+ "second, 1 wither damage to each enemy in it, and the most wounded of you and your allies in it heals for twice "
			+ "that total.")
	public static void crimsonBloom(PairCast c) {
		LivingEntity self = c.caster;
		c.effect(self, MobEffects.REGENERATION, 8, 0);
		c.spiral(PairCast.dust(0x5FE07A, 0.9F), self.position(), 0.6, 2.0, 2, 24);
		Vec3 ground = c.ground(c.point());
		double r = 3 * c.radius;
		c.sound(SoundEvents.GRASS_PLACE, ground, 0.9F, 0.7F);
		c.every(20, 6, second -> {
			c.ring(PairCast.dust(0x7A1024, 1.0F), ground.add(0, 0.1, 0), r, 28, second * 0.3);
			if (second == 0) {
				c.disc(PairCast.shift(0x9E1B32, 0x3B0A16, 0.9F), ground.add(0, 0.05, 0), r, 40);
			}
			double dealt = 0;
			for (LivingEntity e : c.enemiesNear(ground, r)) {
				c.wither(e, 1 * c.power);
				dealt += 1 * c.power;
			}
			if (dealt <= 0) {
				return;
			}
			LivingEntity worst = null;
			double missing = 0;
			for (LivingEntity a : c.alliesNear(ground, r)) {
				double m = a.getMaxHealth() - a.getHealth();
				if (m > missing) {
					missing = m;
					worst = a;
				}
			}
			if (worst != null) {
				c.heal(worst, 2 * dealt);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(worst), 6, 0.4, 0.02);
			}
		});
	}

	/**
	 * Sanguine Tether: a gift of your own blood, carried along the bond to one ally over three seconds. The bond is a
	 * red thread that stays drawn while the ally is in reach; if the ally strays past it, the gift stops there.
	 */
	@Pair(a = "soulbond", b = "transfusion", name = "Sanguine Tether", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Gives up to 4 of your health (never below 2) to one ally you're bound to: it heals three times that over "
			+ "3 seconds (4 a second at power 1) and loses one harmful effect. The bond breaks past 16 blocks, and the rest is lost.")
	public static void sanguineTether(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity ally = otherAlly(c);
		if (ally == null) {
			return;
		}
		double gift = Math.min(4 * c.power, self.getHealth() - 2);
		if (gift <= 0) {
			return;
		}
		self.setHealth((float) (self.getHealth() - gift));
		for (MobEffectInstance e : new ArrayList<>(ally.getActiveEffects())) {
			if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				ally.removeEffect(e.getEffect());
				break;
			}
		}
		c.line(PairCast.dust(0xC8102E, 0.9F), PairCast.mid(self), PairCast.mid(ally), 3);
		c.sound(SoundEvents.BEACON_POWER_SELECT, PairCast.mid(self), 0.6F, 1.5F);
		boolean[] open = {true};
		c.every(20, 3, beat -> {
			if (!open[0]) {
				return;
			}
			if (!c.here(ally) || ally.distanceTo(self) > 16) {
				open[0] = false;
				return;
			}
			c.heal(ally, gift);
			c.line(PairCast.dust(0xFF5A6E, 0.7F), PairCast.mid(self), PairCast.mid(ally), 2);
			c.particles(ParticleTypes.HEART, PairCast.mid(ally), 4, 0.4, 0.02);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(ally), 0.6F, 0.8F + 0.2F * beat);
		});
	}

	/**
	 * Lodestone Thread: the target draws the enemies near it in and shocks any that touch it. Those still gathered when
	 * the pull lets go are tied together by blood threads, and each second each one takes damage for every thread-mate
	 * close by.
	 */
	@Pair(a = "blood_thread", b = "magnetize", name = "Lodestone Thread", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Shocks the target for 2 at once, then magnetizes it for 4 seconds: enemies within 5 blocks are pulled towards "
			+ "it, and any touching it are shocked for 3 once a second. Up to 4 enemies still within 3 blocks are threaded for "
			+ "6 seconds: each second each takes 1 damage while another thread-mate is within 4 blocks.")
	public static void lodestoneThread(PairCast c) {
		LivingEntity lode = c.firstEnemy();
		if (lode == null) {
			return;
		}
		c.shock(lode, 2 * c.power);
		c.zigzag(ParticleTypes.ELECTRIC_SPARK, c.point().add(0, 3, 0), PairCast.mid(lode), 0.3, 2);
		c.sound(SoundEvents.LODESTONE_HIT, PairCast.mid(lode), 1.0F, 1.2F);
		c.every(10, 8, frame -> {
			if (!c.here(lode)) {
				return;
			}
			Vec3 at = PairCast.mid(lode);
			c.ring(PairCast.dust(0x6A7BFF, 0.8F), at, 5 * c.radius, 20, frame * 0.2);
			for (LivingEntity e : c.enemiesNear(at, 5 * c.radius)) {
				if (e == lode) {
					continue;
				}
				c.pullTo(e, at, 0.6);
				if (PairCast.mid(e).distanceTo(at) <= 1.5 && frame % 2 == 0) {
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(e), 0.3, 2);
					c.shock(e, 3 * c.power);
				}
			}
		});
		c.later(80, () -> {
			List<LivingEntity> threads = PairCast.first(c.still(c.enemiesNear(PairCast.mid(lode), 3 * c.radius)), 4);
			if (threads.isEmpty()) {
				return;
			}
			c.sound(SoundEvents.CHAIN_HIT, PairCast.mid(lode), 0.9F, 0.7F);
			int seconds = Math.max(1, (int) Math.round(6 * c.duration));
			c.every(20, seconds, second -> {
				List<LivingEntity> now = c.still(threads);
				for (LivingEntity g : now) {
					int mates = 0;
					for (LivingEntity o : now) {
						if (o != g && mates < 1 && PairCast.mid(o).distanceTo(PairCast.mid(g)) <= 4 * c.radius) {
							mates++;
							c.line(PairCast.dust(0xB0102A, 0.7F), PairCast.mid(g), PairCast.mid(o), 2);
						}
					}
					if (mates > 0) {
						c.hurt(g, mates * c.power);
					}
				}
			});
		});
	}

	/**
	 * Ransom Ward: you pay the ally's ward out of your own blood, and when the ward fades, whatever it didn't need is
	 * paid back to you. A gold spiral wraps the ally while it holds.
	 */
	@Pair(a = "blood_escrow", b = "reversal", name = "Ransom Ward", element = "blood", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Pays up to 3 of your health (never below 4; refused while crouching, or if the ally already has a ward) for a "
			+ "6-second absorption ward of twice the payment. When the ward fades, what is left of it returns to you as half its "
			+ "value in health.")
	public static void ransomWard(PairCast c) {
		LivingEntity self = c.caster;
		LivingEntity ally = otherAlly(c);
		if (ally == null || self.isShiftKeyDown() || ally.getAbsorptionAmount() > 0) {
			return;
		}
		double pay = Math.min(3 * c.power, self.getHealth() - 4);
		if (pay <= 0) {
			return;
		}
		self.setHealth((float) (self.getHealth() - pay));
		double ward = 2 * pay;
		c.absorb(ally, ward, 6);
		c.sound(SoundEvents.BEACON_POWER_SELECT, PairCast.mid(ally), 0.7F, 1.3F);
		c.every(20, 6, second -> {
			if (!ally.isAlive()) {
				return;
			}
			c.ring(PairCast.dust(0xFFF6D8, 0.8F), ally.position().add(0, 0.1, 0), 1.0, 16, second * 0.5);
			if (second == 0) {
				c.spiral(PairCast.shift(0xFFE27A, 0xFFFFFF, 0.8F), ally.position(), 0.8, 2.0, 1.5, 30);
			}
		});
		c.later(Math.max(1, c.ticks(6) - 2), () -> {
			double left = Math.min(ally.getAbsorptionAmount(), ward);
			if (left <= 0) {
				return;
			}
			ally.setAbsorptionAmount((float) (ally.getAbsorptionAmount() - left));
			if (self.isAlive()) {
				c.heal(self, left / 2);
				c.line(PairCast.shift(0xFFE27A, 0xFFFFFF, 0.8F), PairCast.mid(ally), PairCast.mid(self), 3);
				c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(self), 0.8F, 1.6F);
			}
		});
	}

	/**
	 * Reckoned Cut: a reckoning opens on the target. Every time it walks, it is cut, and when the reckoning closes the
	 * cuts come due again, with half of that healing you. Standing still keeps it clean.
	 */
	@Pair(a = "reckoning", b = "red_ledger", name = "Reckoned Cut", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Opens a reckoning on the target for 4 seconds: 2 damage at once. Each quarter second it moves more than half a "
			+ "block, it takes a 1-damage cut (4 cuts at most). At the end each cut comes due again for 2 damage (8 at most), and "
			+ "you heal half of that (4 at most).")
	public static void reckonedCut(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		c.hurt(t, 2 * c.power);
		c.sound(SoundEvents.CHAIN_PLACE, PairCast.mid(t), 0.9F, 0.9F);
		int[] cuts = {0};
		Vec3[] last = {t.position()};
		int frames = Math.max(1, c.ticks(4) / 5);
		c.every(5, frames, frame -> {
			if (!c.here(t)) {
				return;
			}
			Vec3 now = t.position();
			c.ring(PairCast.shift(0xC8A2FF, 0x6A0018, 0.8F), PairCast.mid(t), 0.9, 10, frame * 0.4);
			if (cuts[0] < 4 && now.distanceTo(last[0]) > 0.5) {
				cuts[0]++;
				c.hurt(t, 1 * c.power);
				c.particles(PairCast.dust(0xB0102A, 0.9F), PairCast.mid(t), 6, 0.3, 0.05);
				c.sound(SoundEvents.CHAIN_HIT, PairCast.mid(t), 0.5F, 1.6F);
			}
			last[0] = now;
		});
		c.later(c.ticks(4) + 1, () -> {
			if (!c.here(t) || cuts[0] == 0) {
				return;
			}
			double due = Math.min(8 * c.power, 2 * cuts[0] * c.power);
			c.hurt(t, due);
			if (c.caster.isAlive()) {
				c.heal(c.caster, Math.min(4 * c.power, due / 2));
			}
			c.wave(PairCast.dust(0xC8A2FF, 1.0F), PairCast.mid(t), 16, 0.3);
			c.sound(SoundEvents.BELL_BLOCK, PairCast.mid(t), 0.8F, 0.8F);
		});
	}

	/**
	 * Caldera Ridge: basalt columns burst up along the line from you to the point, and the ground at the point turns to
	 * magma that widens as it burns.
	 */
	@Pair(a = "basalt_surge", b = "magma", name = "Caldera Ridge", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Basalt columns burst up along the line from you to the point: 4 damage to each enemy within 1.5 blocks of the "
			+ "line, and they are tossed into the air. The ground at the point turns to magma for 4 seconds, widening from 2 to "
			+ "4 blocks: each second, 2 fire damage to each enemy in it.")
	public static void calderaRidge(PairCast c) {
		Vec3 from = c.origin();
		Vec3 to = c.point();
		Vec3 ground = c.ground(to);
		double len = from.distanceTo(to);
		Vec3 mid = from.add(to).scale(0.5);
		for (LivingEntity e : c.enemiesNear(mid, len / 2 + 2 * c.radius)) {
			if (distanceToSegment(PairCast.mid(e), from, to) <= 1.5 * c.radius) {
				c.strike(e, 4 * c.power);
				c.lift(e, 0.6);
			}
		}
		c.sound(SoundEvents.BASALT_PLACE, ground, 1.0F, 0.7F);
		c.every(3, 5, k -> {
			Vec3 p = c.ground(from.lerp(to, (k + 1) / 6.0));
			c.column(PairCast.shift(0x3A3340, 0x7A4A2E, 1.0F), p, 0.8 * c.radius, 2.5, 12);
		});
		c.every(10, 9, frame -> {
			double pool = (2 + 0.25 * frame) * c.radius;
			c.disc(PairCast.shift(0xFF7A1A, 0xFFD27A, 0.9F), ground.add(0, 0.05, 0), pool, 24);
			if (frame == 0) {
				c.sound(SoundEvents.LAVA_POP, ground, 0.8F, 0.6F);
			}
			if (frame % 2 == 0 && frame < 8) {
				for (LivingEntity e : c.enemiesNear(ground, pool)) {
					c.burn(e, 2 * c.power);
				}
			}
		});
	}

	/**
	 * Ossuary Spire: a monolith bursts up under the target and hurls it aloft; when it crashes down, bone spurs jut up
	 * under the enemies round the spot and bleed them.
	 */
	@Pair(a = "bonespur", b = "monolith", name = "Ossuary Spire", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A stone monolith bursts up under the target: 6 damage, and it is hurled into the air. 1.2 seconds later it "
			+ "crashes down: 4 damage to enemies within 1.5 blocks of the spot, and bone spurs jut up under up to 4 enemies "
			+ "within 4 blocks: 3 damage each, then 1 damage a second for 3 seconds.")
	public static void ossuarySpire(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3 base = c.ground(t.position());
		c.column(PairCast.dust(0x7D7868, 1.1F), base, 0.8 * c.radius, 3.0, 36);
		c.strike(t, 6 * c.power);
		c.lift(t, 0.9);
		c.sound(SoundEvents.DEEPSLATE_BREAK, base, 1.0F, 0.6F);
		c.later(24, () -> {
			Vec3 land = c.here(t) ? c.ground(t.position()) : base;
			c.disc(PairCast.dust(0x5A5548, 1.0F), land, 1.5 * c.radius, 30);
			c.shake(land, 0.4F, 10);
			c.sound(SoundEvents.MACE_SMASH_GROUND, land, 1.0F, 0.9F);
			for (LivingEntity e : c.enemiesNear(land, 1.5 * c.radius)) {
				c.strike(e, 4 * c.power);
			}
			List<LivingEntity> spurred = PairCast.first(c.enemiesNear(land, 4 * c.radius), 4);
			for (LivingEntity v : spurred) {
				c.line(PairCast.dust(0xEDE6CF, 0.9F), c.ground(v.position()), PairCast.mid(v), 3);
				c.strike(v, 3 * c.power);
			}
			c.every(20, 3, second -> {
				for (LivingEntity v : c.still(spurred)) {
					c.hurt(v, 1 * c.power);
				}
			});
		});
	}

	/**
	 * Permafrost Relic: the target is chilled and slowed step by step until it is held, then it cracks. If it dies before
	 * the crack, it shatters, and the frost hits the enemies round it.
	 */
	@Pair(a = "black_ice", b = "fossilize", name = "Permafrost Relic", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "radius", "duration"},
		text = "Chills and slows the target: Slowness I, then II, then III from 2 seconds on, with Weakness II for 5 seconds. "
			+ "At 4 seconds it cracks for 6 damage and is Cracked for 5 seconds. If it dies before that, it shatters: 4 freeze "
			+ "damage to enemies within 3 blocks, which are cracked too.")
	public static void permafrostRelic(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		Vec3[] at = {PairCast.mid(t)};
		boolean[] shattered = {false};
		c.chill(t, 2);
		c.effect(t, MobEffects.WEAKNESS, 5, 1);
		c.sound(SoundEvents.POWDER_SNOW_STEP, at[0], 1.0F, 0.8F);
		c.every(20, 5, beat -> {
			if (c.here(t)) {
				at[0] = PairCast.mid(t);
				if (beat == 0) {
					c.effect(t, MobEffects.SLOWNESS, 1, 0);
				} else if (beat == 1) {
					c.effect(t, MobEffects.SLOWNESS, 1, 1);
				} else if (beat == 2) {
					c.effect(t, MobEffects.SLOWNESS, 2, 2);
				}
				c.sphere(PairCast.shift(0xBDEFFF, 0x6B6B66, 0.9F), at[0], 1.2 - 0.15 * beat, 22);
				if (beat == 4) {
					c.hurt(t, 6 * c.power);
					c.mark(t, Reactions.Mark.CRACKED);
					c.shake(at[0], 0.3F, 6);
					c.sound(SoundEvents.STONE_BREAK, at[0], 1.0F, 0.9F);
				}
			} else if (!shattered[0]) {
				shattered[0] = true;
				for (LivingEntity e : c.enemiesNear(at[0], 3 * c.radius)) {
					if (e == t) {
						continue;
					}
					c.freeze(e, 4 * c.power);
					c.mark(e, Reactions.Mark.CRACKED);
				}
				c.particles(PairCast.dust(0xDDF6FF, 1.0F), at[0], 30, 0.8, 0.1);
				c.sound(SoundEvents.GLASS_BREAK, at[0], 1.0F, 1.2F);
			}
		});
	}

	/**
	 * Shard Shell: the ally is sealed in a crystal shell for four seconds. Harm taken inside is counted, and when the
	 * shell opens the count comes back as shards that seek the enemies near the ally.
	 */
	@Pair(a = "geode", b = "shulkershell", name = "Shard Shell", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Seals the first ally in reach in a crystal shell for 4 seconds, with Resistance III, and counts the harm it takes "
			+ "inside. When the shell opens, one shard flies per 4 counted (3 at most) at the nearest enemies within 8 blocks, "
			+ "2 damage each. Enemies within 3 blocks are lifted.")
	public static void shardShell(PairCast c) {
		LivingEntity ally = c.firstAlly();
		if (ally == null) {
			return;
		}
		Vec3[] at = {PairCast.mid(ally)};
		double[] stored = {0};
		double[] last = {ally.getHealth()};
		int frames = Math.max(1, c.ticks(4) / 5);
		c.effect(ally, MobEffects.RESISTANCE, 4, 2);
		c.sound(SoundEvents.AMETHYST_CLUSTER_PLACE, at[0], 0.9F, 0.8F);
		c.every(5, frames, beat -> {
			if (ally.isAlive()) {
				at[0] = PairCast.mid(ally);
				float hp = ally.getHealth();
				if (hp < last[0]) {
					stored[0] += last[0] - hp;
				}
				last[0] = hp;
			}
			c.sphere(PairCast.shift(0x7FF7E8, 0xC77DFF, 0.8F), at[0], 1.0, 16);
		});
		c.later(frames * 5, () -> {
			Vec3 from = at[0];
			int count = (int) Math.min(3, Math.ceil(stored[0] / 4));
			for (LivingEntity e : PairCast.first(c.enemiesNear(from, 8 * c.radius), count)) {
				c.line(PairCast.dust(0xB9F6FF, 0.8F), from, PairCast.mid(e), 1.5);
				c.hurt(e, 2 * c.power);
			}
			for (LivingEntity e : c.enemiesNear(from, 3 * c.radius)) {
				c.lift(e, 0.6);
			}
			c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, from, 1.0F, 1.1F);
		});
	}

	/** The first ally the shape reached that is not the caster, or null. */
	private static LivingEntity otherAlly(PairCast c) {
		for (LivingEntity a : c.allies()) {
			if (a != c.caster) {
				return a;
			}
		}
		return null;
	}

	/** The distance from {@code p} to the segment {@code a}-{@code b}. */
	private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		double f = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(f)));
	}
}
