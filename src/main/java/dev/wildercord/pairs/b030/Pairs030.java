package dev.wildercord.pairs.b030;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Statuses;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs030 {
	private Pairs030() {}

	/**
	 * Shearwind: a gale that cuts twice. The first pass cuts everything in a ring round the point and breaks what
	 * it was winding up; the gale comes back round the same ring half a second later. The look: a spinning ring of
	 * pale green blades, then a flat wave of wind pushing out.
	 */
	@Pair(a = "razorgale", b = "windcut", name = "Shearwind", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A cutting gale whirls round the point: each enemy within 3 blocks takes 2 damage, is left bleeding, is shoved "
			+ "outward and loses any charge or spell it was winding up. Half a second later the gale circles back: 4 more to "
			+ "each one still within 3 blocks.")
	public static void shearwind(PairCast c) {
		Vec3 spot = c.point();
		double reach = 3 * c.radius;
		List<LivingEntity> ring = c.enemiesNear(spot, reach);
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, spot, 0.8F, 1.5F);
		for (LivingEntity t : ring) {
			c.strike(t, 2 * c.power);
			c.mark(t, Reactions.Mark.BLEEDING);
			c.knockFrom(t, spot, 0.5, 0.15);
			Statuses.interrupt(t);
		}
		// The wind-up: a blade ring spinning out at the point, three frames.
		c.every(4, 3, frame -> {
			double turn = frame * 0.9;
			c.ring(PairCast.shift(0xE6FFF2, 0x4FC79A, 0.8F), spot, reach, 28, turn);
			c.ring(ParticleTypes.SWEEP_ATTACK, spot.add(0, 0.3, 0), reach * 0.5, 8, -turn);
		});
		// The payoff: the gale comes back round the same ring.
		c.later(10, () -> {
			for (LivingEntity t : c.still(c.enemiesNear(spot, reach))) {
				c.strike(t, 4 * c.power);
				c.line(PairCast.dust(0xB8F5D8, 0.9F), spot, PairCast.mid(t), 3);
			}
			c.wave(PairCast.dust(0x6FD3B0, 1.0F), spot, 24, 0.35);
			c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, spot, 1.0F, 1.2F);
		});
	}

	/**
	 * Dustwalker: a dust devil that hunts. It touches down at the point and creeps after the nearest enemy, scouring
	 * what it passes; when it blows out it flings the caught ones up. The look: a sand spiral that walks across the
	 * ground, then a burst of sand thrown skyward.
	 */
	@Pair(a = "dust_devil", b = "sandstorm", name = "Dustwalker", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "A dust devil touches down at the point and hunts the nearest enemy within 10 blocks for 5 seconds, moving 1.5 "
			+ "blocks every half second. Each step, whatever is within 2 blocks takes 1 damage, is blinded for 2 seconds and "
			+ "slowed. When it blows out it flings them up, with 2 more damage.")
	public static void dustwalker(PairCast c) {
		Vec3[] devil = {c.point()};
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, devil[0], 0.6F, 0.6F);
		c.every(10, 11, frame -> {
			Vec3 at = devil[0];
			if (frame == 10) {
				for (LivingEntity t : c.enemiesNear(at, 2 * c.radius)) {
					c.lift(t, 0.9);
					c.strike(t, 2 * c.power);
				}
				c.wave(PairCast.dust(0xE8C27A, 1.1F), at, 24, 0.4);
				c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, at, 1.0F, 1.5F);
				return;
			}
			c.spiral(PairCast.shift(0xF5E1B0, 0xB98B3F, 1.0F), at, 1.1, 2.4, 2, 20);
			for (LivingEntity t : c.enemiesNear(at, 2 * c.radius)) {
				c.strike(t, c.power);
				c.effect(t, MobEffects.BLINDNESS, 2, 0);
				c.effect(t, MobEffects.SLOWNESS, 2, 1);
			}
			LivingEntity prey = c.nearestEnemy(at, 10, null);
			if (prey != null) {
				Vec3 mid = PairCast.mid(prey);
				Vec3 flat = new Vec3(mid.x - at.x, 0, mid.z - at.z);
				double d = flat.length();
				if (d > 0.01) {
					devil[0] = at.add(flat.normalize().scale(Math.min(1.5, d)));
				}
			}
		});
	}

	/**
	 * Deadline: a clock on each marked enemy that counts the damage it takes, then strikes twice. The look: small
	 * gold clock faces over each head, ticking round; a chime at the moment, and a ring burst at the deadline.
	 */
	@Pair(a = "countdown", b = "doomclock", name = "Deadline", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Clocks up to 3 enemies for 3 seconds (Slowness I meanwhile) and counts the damage they take (12 at most). At 1.5 "
			+ "seconds the moment catches up for 6 damage; at 3 the count goes off again, to it and every enemy within 3 blocks. "
			+ "A clocked enemy that dies first passes its clock to the nearest enemy within 6 blocks.")
	public static void deadline(PairCast c) {
		// Each clock: {health at the last look, damage counted so far}.
		Map<LivingEntity, double[]> clocks = new LinkedHashMap<>();
		for (LivingEntity t : PairCast.first(c.enemies(), 3)) {
			clocks.put(t, new double[] {t.getHealth(), 0});
			c.effect(t, MobEffects.SLOWNESS, 3, 0);
		}
		c.sound(SoundEvents.BELL_RESONATE, c.point(), 0.8F, 1.4F);
		c.every(10, 7, frame -> {
			for (LivingEntity t : List.copyOf(clocks.keySet())) {
				double[] clock = clocks.get(t);
				if (!c.here(t)) {
					clocks.remove(t);
					LivingEntity heir = c.nearestEnemy(PairCast.mid(t), 6 * c.radius, t);
					if (heir != null && !clocks.containsKey(heir)) {
						clocks.put(heir, new double[] {heir.getHealth(), clock[1]});
					}
					continue;
				}
				double health = t.getHealth();
				if (health < clock[0]) {
					clock[1] = Math.min(12, clock[1] + clock[0] - health);
				}
				clock[0] = health;
				c.ring(PairCast.dust(0xFFE08A, 0.8F), PairCast.mid(t).add(0, 1.1, 0), 0.6, 12, frame * 0.35);
				if (frame == 3) {
					c.hurt(t, 6 * c.power);
					clock[0] = t.getHealth();
					c.sound(SoundEvents.NOTE_BLOCK_CHIME, PairCast.mid(t), 0.9F, 1.8F);
				}
			}
			if (frame == 6) {
				Map<LivingEntity, Double> burst = new LinkedHashMap<>();
				for (Map.Entry<LivingEntity, double[]> e : clocks.entrySet()) {
					Vec3 at = PairCast.mid(e.getKey());
					c.wave(PairCast.dust(0xFFE08A, 1.0F), at, 20, 0.3);
					for (LivingEntity n : c.enemiesNear(at, 3 * c.radius)) {
						burst.merge(n, e.getValue()[1], Math::max);
					}
				}
				for (Map.Entry<LivingEntity, Double> b : burst.entrySet()) {
					if (b.getValue() > 0 && c.here(b.getKey())) {
						c.hurt(b.getKey(), b.getValue() * c.power);
					}
				}
				c.sound(SoundEvents.ANVIL_LAND, c.point(), 0.4F, 1.9F);
			}
		});
	}

	/**
	 * Standstill: a held moment. The enemies stop where they stand, and the time they lost comes back in one blow.
	 * The look: pale spheres of stopped light shrinking round each one, then a chime and a knock back.
	 */
	@Pair(a = "stasis", b = "stillbind", name = "Standstill", element = "time", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Holds up to 4 enemies in place for 3 seconds without harm (a boss is only slowed). Then the time they "
			+ "lost comes back at once: 2 damage for each second they were held, and each is knocked back from the point.")
	public static void standstill(PairCast c) {
		List<LivingEntity> held = PairCast.first(c.enemies(), 4);
		int ticks = c.ticks(3);
		double seconds = ticks / 20.0;
		for (LivingEntity t : held) {
			if (c.movable(t)) {
				Statuses.stagger(t, ticks);
			} else {
				c.effect(t, MobEffects.SLOWNESS, 3, 2);
			}
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.8F, 0.6F);
		c.every(6, 5, frame -> {
			for (LivingEntity t : c.still(held)) {
				c.sphere(PairCast.shift(0xD9E8FF, 0x7A5CFF, 0.9F), PairCast.mid(t), 1.0 - frame * 0.15, 22);
			}
		});
		c.later(ticks, () -> {
			for (LivingEntity t : c.still(held)) {
				c.hurt(t, 2 * seconds * c.power);
				c.knockFrom(t, c.point(), 0.6, 0.25);
				c.wave(PairCast.shift(0xD9E8FF, 0x7A5CFF, 1.0F), PairCast.mid(t), 16, 0.3);
			}
			c.shake(c.point(), 0.3F, 8);
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 1.0F, 1.2F);
		});
	}

	/**
	 * Dread Tithe: a drain that also takes. Blood flows from each target to you, and what it leaves you is kept; the
	 * good effects it had are torn off and yours for as long as they had left. The look: a thread of dark red
	 * drawn from each target to the caster, then a spiral of blood round you.
	 */
	@Pair(a = "leech", b = "timesteal", name = "Dread Tithe", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 3 enemies take 3 damage and you heal what it takes; overflow becomes a shield of up to 4 for 30 seconds. "
			+ "Each gives up to 2 good effects, which you gain with the time left (30 seconds at most). With none to take, a "
			+ "target is dragged (Slowness I, 2 seconds) and you are quickened (Speed I, 2 seconds).")
	public static void dreadTithe(PairCast c) {
		List<LivingEntity> drained = PairCast.first(c.enemies(), 3);
		LivingEntity caster = c.caster;
		double dealt = 0;
		for (LivingEntity t : drained) {
			double before = t.getHealth();
			c.hurt(t, 3 * c.power);
			dealt += Math.max(0, before - t.getHealth());
			c.arc(PairCast.shift(0xFF4D5E, 0x7A0B1F, 0.9F), PairCast.mid(t), PairCast.mid(caster), 1.2, 18);
		}
		double room = caster.getMaxHealth() - caster.getHealth();
		c.heal(caster, dealt);
		if (dealt > room) {
			c.absorb(caster, Math.min(4, dealt - room), 30);
		}
		for (LivingEntity t : drained) {
			int taken = 0;
			for (MobEffectInstance inst : List.copyOf(t.getActiveEffects())) {
				if (taken == 2) {
					break;
				}
				if (inst.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL || inst.isInfiniteDuration()) {
					continue;
				}
				int left = Math.min(inst.getDuration(), c.ticks(30));
				t.removeEffect(inst.getEffect());
				caster.addEffect(new MobEffectInstance(inst.getEffect(), left, inst.getAmplifier(), false, true));
				taken++;
			}
			if (taken == 0) {
				c.effect(t, MobEffects.SLOWNESS, 2, 0);
				c.effect(caster, MobEffects.SPEED, 2, 0);
			}
		}
		c.sound(SoundEvents.SHULKER_BULLET_HIT, PairCast.mid(caster), 0.7F, 0.6F);
		c.later(8, () -> {
			c.spiral(PairCast.dust(0x8E1B2E, 1.0F), PairCast.mid(caster), 0.8, 2.0, 2, 24);
			c.particles(ParticleTypes.DAMAGE_INDICATOR, PairCast.mid(caster), 6, 0.3, 0.1);
			c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, PairCast.mid(caster), 0.8F, 0.5F);
		});
	}

	/**
	 * Quickening Pulse: a field of time that beats. Allies within reach are hastened; then the pulse beats again and
	 * again, each beat a ring of gold shrinking inward and a little healing for whoever stands in it. The look: gold
	 * rings contracting on the ground, and happy sparks over the allies in them.
	 */
	@Pair(a = "accelerate", b = "haste", name = "Quickening Pulse", element = "time", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Allies within 6 blocks of the point get Haste II and Speed I for 10 seconds. The pulse beats 6 times, every 2 "
			+ "seconds for 10 seconds, the first at once: each ally inside it gets Regeneration I for 2 seconds.")
	public static void quickeningPulse(PairCast c) {
		Vec3 spot = c.point();
		double reach = 6 * c.radius;
		for (LivingEntity a : c.alliesNear(spot, reach)) {
			c.effect(a, MobEffects.HASTE, 10, 1);
			c.effect(a, MobEffects.SPEED, 10, 0);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 1.0F, 0.8F);
		c.every(40, 6, beat -> {
			double r = reach * (1 - beat * 0.12);
			c.ring(PairCast.shift(0xFFE7A0, 0x5FD4FF, 0.9F), spot, r, 24, beat * 0.3);
			for (LivingEntity a : c.still(c.alliesNear(spot, reach))) {
				c.effect(a, MobEffects.REGENERATION, 2, 0);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 4, 0.4, 0.05);
			}
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, spot, 0.6F, 1.0F + 0.1F * beat);
		});
	}

	/**
	 * Pulsebound: a sonar ping that marks the enemies it finds and then reads their hearts. Each beat of the marked
	 * ones that are hurt bleeds through the heart. The look: a violet sonar wave across the ground, glowing outlines
	 * (vanilla Glowing), then a pulse ring over each beat.
	 */
	@Pair(a = "echolocate", b = "heartsense", name = "Pulsebound", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "A sonar pulse from the point: up to 16 enemies within 16 blocks glow through walls for 10 seconds, and "
			+ "those within 3 blocks of the point are slowed for 3 seconds. Then five beats, a second apart: each beat deals 2 "
			+ "wither damage to the first 8 of the glowing enemies still there that are under half health.")
	public static void pulsebound(PairCast c) {
		Vec3 spot = c.point();
		c.wave(PairCast.shift(0x9AE6FF, 0x3B1E7A, 1.2F), spot, 36, 0.8);
		c.sound(SoundEvents.BELL_RESONATE, spot, 0.7F, 0.6F);
		List<LivingEntity> glowing = c.enemiesNear(spot, 16);
		for (LivingEntity t : glowing) {
			c.effect(t, MobEffects.GLOWING, 10, 0);
			if (PairCast.mid(t).distanceTo(spot) <= 3) {
				c.effect(t, MobEffects.SLOWNESS, 3, 1);
			}
		}
		c.later(10, () -> c.every(20, 5, beat -> {
			List<LivingEntity> hurt = new ArrayList<>();
			for (LivingEntity t : PairCast.first(c.still(glowing), PairCast.MAX_TARGETS)) {
				if (t.getHealth() < t.getMaxHealth() / 2) {
					hurt.add(t);
				}
			}
			for (LivingEntity t : hurt) {
				Vec3 heart = PairCast.mid(t);
				c.wither(t, 2 * c.power);
				c.ring(PairCast.dust(0x6A3BFF, 1.0F), heart, 0.8, 16, beat * 0.4);
				c.particles(ParticleTypes.SCULK_SOUL, heart, 3, 0.2, 0.02);
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 0.8F, 0.5F + 0.1F * beat);
		}));
	}

	/**
	 * Red Tithe: one enemy's blood, taken for yourself. The first blow is worse the more of the enemy is gone; you
	 * heal half of it, and the wound goes on bleeding into you for four seconds. The look: a dark red thread of
	 * blood across to you, then single drops falling one per second.
	 */
	@Pair(a = "hemomancy", b = "lifesteal", name = "Red Tithe", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "4 damage to the first enemy, and 1 more for every 2 health it is missing (up to 4 more). You heal half of what "
			+ "it takes. For 4 seconds after, it bleeds 1 a second, and you heal half of each drop.")
	public static void redTithe(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		double missing = t.getMaxHealth() - t.getHealth();
		double before = t.getHealth();
		c.hurt(t, (4 + Math.min(4, missing / 2)) * c.power);
		double took = Math.max(0, before - t.getHealth());
		c.heal(c.caster, took / 2);
		c.arc(PairCast.shift(0xFF2E4D, 0x5A0010, 0.9F), PairCast.mid(t), PairCast.mid(c.caster), 0.8, 16);
		c.sound(SoundEvents.SHULKER_BULLET_HIT, PairCast.mid(t), 0.8F, 0.7F);
		c.every(20, 5, drop -> {
			if (drop == 0 || !c.here(t)) {
				return;
			}
			c.hurt(t, c.power);
			c.heal(c.caster, 0.5 * c.power);
			c.line(PairCast.dust(0xC8102E, 0.8F), PairCast.mid(t), PairCast.mid(c.caster), 2);
			c.particles(ParticleTypes.CRIMSON_SPORE, PairCast.mid(t), 4, 0.2, 0.02);
			c.sound(SoundEvents.SHULKER_BULLET_HIT, PairCast.mid(t), 0.4F, 1.4F);
		});
	}

	/**
	 * Riven: stripped, then cut. Each enemy first loses what protects it (its absorption and its Resistance), then a
	 * cleave lands in proportion to its size and cuts a little into anything standing beside it. The look: a shatter
	 * of pale sparks, then two crossing red slashes.
	 */
	@Pair(a = "cleave", b = "rend", name = "Riven", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 3 enemies lose their absorption hearts and any Resistance, then are cleaved for 6 damage plus 10% of their "
			+ "max health (12 at most). Up to 3 enemies within 2 blocks of each take half of that, once each per cast.")
	public static void riven(PairCast c) {
		List<LivingEntity> torn = PairCast.first(c.enemies(), 3);
		for (LivingEntity t : torn) {
			t.setAbsorptionAmount(0);
			t.removeEffect(MobEffects.RESISTANCE);
			c.sphere(PairCast.dust(0xFFE0E0, 0.8F), PairCast.mid(t), 0.9, 14);
		}
		c.sound(SoundEvents.GLASS_BREAK, c.point(), 0.8F, 0.9F);
		Set<LivingEntity> splashed = new HashSet<>();
		c.later(8, () -> {
			for (LivingEntity t : c.still(torn)) {
				Vec3 at = PairCast.mid(t);
				double dmg = (6 + Math.min(12, 0.1 * t.getMaxHealth())) * c.power;
				c.line(PairCast.dust(0xFF3B4E, 1.0F), at.add(-1, 0.6, -1), at.add(1, -0.2, 1), 4);
				c.line(PairCast.dust(0xFF3B4E, 1.0F), at.add(1, 0.6, -1), at.add(-1, -0.2, 1), 4);
				c.strike(t, dmg);
				int given = 0;
				for (LivingEntity n : c.enemiesNear(at, 2)) {
					if (given == 3) {
						break;
					}
					if (n == t || torn.contains(n) || !splashed.add(n)) {
						continue;
					}
					c.strike(n, dmg / 2);
					given++;
				}
			}
			c.sound(SoundEvents.PLAYER_ATTACK_CRIT, c.point(), 1.0F, 0.8F);
		});
	}

	/**
	 * Hush Chain: a silence that jumps. Each struck caster is burned and silenced; a second later the silence leaps
	 * from them to the nearest other enemy, and so on, four leaps at most. The look: rings of violet quieting round
	 * each target, then a crackling zigzag from one to the next.
	 */
	@Pair(a = "manaburn", b = "silence", name = "Hush Chain", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "5 magic damage to up to 3 enemies, and each is silenced for 4 seconds (2 on players): a cast in hand is cut short "
			+ "and none can follow. A second later the hush leaps to the nearest other enemy within 3 blocks of each: 3 damage "
			+ "and a 2 second silence, up to 4 leaps. A target already hushed is passed over.")
	public static void hushChain(PairCast c) {
		List<LivingEntity> hushed = PairCast.first(c.enemies(), 3);
		for (LivingEntity t : hushed) {
			c.hurt(t, 5 * c.power);
			Statuses.interrupt(t);
			Statuses.silence(t, c.ticks(4));
		}
		c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 0.9F, 0.7F);
		c.every(8, 3, frame -> {
			for (LivingEntity t : c.still(hushed)) {
				c.sphere(PairCast.shift(0xC7A6FF, 0x4B2A8F, 0.9F), PairCast.mid(t), 1.2 - frame * 0.35, 18);
			}
		});
		c.later(20, () -> {
			List<LivingEntity> leapt = new ArrayList<>();
			for (LivingEntity from : c.still(hushed)) {
				if (leapt.size() >= 4) {
					break;
				}
				LivingEntity to = c.nearestEnemy(PairCast.mid(from), 3, from);
				if (to == null || hushed.contains(to) || leapt.contains(to)) {
					continue;
				}
				leapt.add(to);
				c.zigzag(PairCast.shift(0xC7A6FF, 0x6A3BD8, 0.8F), PairCast.mid(from), PairCast.mid(to), 0.4, 3);
				c.hurt(to, 3 * c.power);
				Statuses.silence(to, c.ticks(2));
				c.sound(SoundEvents.NOTE_BLOCK_CHIME, PairCast.mid(to), 0.8F, 1.6F);
			}
		});
	}
}
