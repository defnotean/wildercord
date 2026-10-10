package dev.wildercord.pairs.b005;

import dev.wildercord.cast.CastLock;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs005 {
	private Pairs005() {}

	/**
	 * Storm Mercy: heal and charge. The healing light spirals round each ally; then the ally is charged for six
	 * seconds, and every second a crackle of lightning leaps from the charge to any enemy that strays close.
	 */
	@Pair(a = "heal", b = "shock", name = "Storm Mercy", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "Heals up to 8 allies 6 health each, and charges them for 6 seconds: an enemy within 2.5 blocks of a "
			+ "charged ally takes 2 lightning damage every second.")
	public static void stormMercy(PairCast c) {
		List<LivingEntity> healed = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity a : healed) {
			c.heal(a, 6 * c.power);
			c.helix(PairCast.dust(0x7DFFB0, 0.9F), PairCast.dust(0xFFF27A, 0.9F), PairCast.mid(a), 0.7, 1.8, 2, 28);
		}
		if (!healed.isEmpty()) {
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(healed.get(0)), 0.9F, 1.1F);
		}
		int pulses = Math.max(1, (int) Math.round(6 * c.duration));
		c.every(20, pulses, frame -> {
			for (LivingEntity a : c.still(healed)) {
				Vec3 at = PairCast.mid(a);
				c.ring(PairCast.dust(0xB8FFE0, 0.8F), at, 2.5 * c.radius, 22, frame * 0.5);
				for (LivingEntity e : c.enemiesNear(at, 2.5 * c.radius)) {
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(e), 0.35, 3);
					c.shock(e, 2 * c.power);
				}
			}
			c.sound(SoundEvents.NOTE_BLOCK_CHIME, PairCast.mid(healed.isEmpty() ? c.caster : healed.get(0)), 0.5F, 1.4F);
		});
	}

	/**
	 * Afterflash: blink and lightning. A violet shell collapses where you stand; you snap to the landing spot, and
	 * the lightning falls on the spot you left, so the enemies who were crowding you pay for it.
	 */
	@Pair(a = "blink", b = "lightning", name = "Afterflash", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Blinks you to where the spell landed (max 40 blocks). The spot you left is struck: 5 lightning damage to "
			+ "each enemy within 3 blocks of it, slowed (Slowness I for 3 seconds). If you can't land safely, nothing is struck.")
	public static void afterflash(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 left = self.position();
		Vec3 landing = landingSpot(left, c.point());
		c.sound(SoundEvents.ENDERMAN_TELEPORT, left, 0.7F, 1.3F);
		// The shell collapses on the spot for half a second before the blink.
		c.every(3, 4, frame -> c.sphere(PairCast.shift(0xB56CFF, 0x2A0B4A, 0.9F), left.add(0, 1, 0), 2.0 - frame * 0.45, 24));
		c.later(10, () -> {
			if (!c.blink(self, landing)) {
				return;
			}
			c.wave(PairCast.dust(0xB56CFF, 1.0F), self.position().add(0, 0.1, 0), 18, 0.35);
			c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, landing, 0.6F, 1.5F);
			c.punch(0.15F);
			c.bolt(left);
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, left, 0.6F, 1.4F);
			for (LivingEntity e : c.enemiesNear(left, 3 * c.radius)) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, left.add(0, 6, 0), PairCast.mid(e), 0.3, 3);
				c.shock(e, 5 * c.power);
				c.effect(e, MobEffects.SLOWNESS, 3, 0);
			}
			c.star(PairCast.shift(0xE0C8FF, 0xB56CFF, 0.8F), left.add(0, 0.2, 0), 6, 3 * c.radius, 0);
		});
	}

	/** Where a blink lands: the point, or 40 blocks along the way if it's further. */
	private static Vec3 landingSpot(Vec3 from, Vec3 to) {
		if (to.distanceTo(from) <= 40) {
			return to;
		}
		return from.add(to.subtract(from).normalize().scale(40));
	}

	/**
	 * Viper Chain: shock and venom. The first enemy is shocked and poisoned, then the arc leaps from it to the next
	 * unpoisoned enemy near it, four times, each leap a little weaker and each one leaving poison behind.
	 */
	@Pair(a = "shock", b = "venom", name = "Viper Chain", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Shocks the first enemy for 4 and poisons it: 2 damage now and Poison I for 4 seconds. The arc then leaps to "
			+ "the nearest unpoisoned enemy within 4 blocks, four times: 3 lightning, 1 damage and Poison I for 3 seconds each.")
	public static void viperChain(PairCast c) {
		LivingEntity first = c.firstEnemy();
		if (first == null) {
			return;
		}
		List<LivingEntity> poisoned = new ArrayList<>();
		LivingEntity[] tip = {first};
		c.zigzag(ParticleTypes.ELECTRIC_SPARK, c.origin(), PairCast.mid(first), 0.5, 3);
		c.sound(SoundEvents.TRIDENT_THUNDER, PairCast.mid(first), 0.5F, 1.4F);
		c.shock(first, 4 * c.power);
		poisoned.add(first);
		venom(c, first, 2 * c.power, 4);
		// The venom takes hold: a green spore burst round the first victim, a few ticks after the strike.
		c.later(4, () -> c.sphere(PairCast.dust(0x9BE35A, 0.9F), PairCast.mid(first), 1.1, 22));
		for (int hop = 1; hop <= 4; hop++) {
			int beat = hop;
			c.later(6 * hop, () -> {
				LivingEntity from = tip[0];
				if (!c.here(from)) {
					return;
				}
				LivingEntity next = null;
				for (LivingEntity e : c.enemiesNear(PairCast.mid(from), 4 * c.radius)) {
					if (!poisoned.contains(e) && c.here(e)) {
						next = e;
						break;
					}
				}
				if (next == null) {
					return;
				}
				Vec3 a = PairCast.mid(from);
				Vec3 b = PairCast.mid(next);
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, a, b, 0.5, 3);
				c.line(PairCast.dust(0xC6F25A, 0.7F), a, b, 4);
				c.sound(SoundEvents.TRIDENT_THUNDER, b, 0.35F, 1.0F + beat * 0.15F);
				c.shock(next, 3 * c.power);
				venom(c, next, 1 * c.power, 3);
				poisoned.add(next);
				tip[0] = next;
			});
		}
	}

	/** The venom: damage now, then Poison I, and a puff of spores. */
	private static void venom(PairCast c, LivingEntity t, double now, double seconds) {
		c.hurt(t, now);
		c.effect(t, MobEffects.POISON, seconds, 0);
		c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 8, 0.4, 0.02);
	}

	/**
	 * Bloodwire: bleed and jolt. The jolt lands with its stun, and the wound opens; then each beat the blood runs
	 * the current on: the wound's drip sparks into the enemies crowding its target.
	 */
	@Pair(a = "bleed", b = "jolt", name = "Bloodwire", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Each target takes 4 lightning, Slowness II for 1 second and a wound: 2 damage, then 1 more every half second "
			+ "for 4 seconds. Each of those pulses also sparks 1 lightning into up to 2 enemies within 1.5 blocks.")
	public static void bloodwire(PairCast c) {
		List<LivingEntity> cut = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 hand = c.origin();
		for (LivingEntity t : cut) {
			c.shock(t, 4 * c.power);
			c.effect(t, MobEffects.SLOWNESS, 1, 1);
			c.line(PairCast.dust(0x9E0B19, 0.9F), hand, PairCast.mid(t), 3);
			c.sound(SoundEvents.TRIDENT_HIT, PairCast.mid(t), 0.8F, 0.7F);
		}
		c.every(10, 9, frame -> {
			for (LivingEntity t : c.still(cut)) {
				Vec3 at = PairCast.mid(t);
				if (frame == 0) {
					c.strike(t, 2 * c.power);
					c.star(PairCast.dust(0xC4101F, 1.0F), at, 6, 0.9, 0);
					continue;
				}
				c.strike(t, 1 * c.power);
				c.mote(PairCast.dust(0xB0121F, 0.9F), at.add(0, 0.2, 0), new Vec3(0, -0.4, 0));
				c.particles(PairCast.dust(0xB0121F, 1.0F), at, 6, 0.3, 0.02);
				c.sound(SoundEvents.TRIDENT_HIT, at, 0.25F, 1.8F);
				int sparked = 0;
				for (LivingEntity e : c.enemiesNear(at, 1.5 * c.radius)) {
					if (e == t || sparked == 2) {
						continue;
					}
					c.zigzag(ParticleTypes.ELECTRIC_SPARK, at, PairCast.mid(e), 0.3, 3);
					c.shock(e, 1 * c.power);
					sparked++;
				}
			}
		});
	}

	/**
	 * Rolling Thunder: aftershock and thunderclap. A flash and a crack knock the crowd outward; half a second later the
	 * ground echoes, and only those still standing close to the point take the second blow.
	 */
	@Pair(a = "aftershock", b = "thunderclap", name = "Rolling Thunder", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A flash and a crack: 3 lightning damage to every enemy within 3 blocks, knocked outward and slowed (Slowness II) "
			+ "for half a second. Half a second later the ground echoes: 3 damage to whoever is still within 3 blocks.")
	public static void rollingThunder(PairCast c) {
		Vec3 at = c.point();
		double reach = 3 * c.radius;
		c.tint(at, 16, 0xFFF6D6, 4);
		c.bolt(at);
		c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, at, 1.0F, 1.2F);
		c.shake(at, 0.35F, 12);
		for (LivingEntity e : c.enemiesNear(at, reach)) {
			c.shock(e, 3 * c.power);
			c.knockFrom(e, at, 0.9, 0.3);
			c.effect(e, MobEffects.SLOWNESS, 0.5, 1);
		}
		c.wave(PairCast.shift(0xFFF6D6, 0x9AB0FF, 1.0F), at.add(0, 0.2, 0), 26, 0.4);
		c.later(10, () -> {
			c.sound(SoundEvents.MACE_SMASH_GROUND_HEAVY, at, 1.0F, 0.7F);
			c.column(PairCast.dust(0x7A5C3A, 1.4F), at, 2.5 * c.radius, 1.6, 40);
			c.shake(at, 0.45F, 10);
			for (LivingEntity e : c.enemiesNear(at, reach)) {
				c.strike(e, 3 * c.power);
			}
		});
	}

	/**
	 * Dynamo Sigil: empower and galvanize. A sigil glows on the ground for eight seconds: each pulse lifts the allies
	 * inside with Strength, and the lightning in its ring jolts the enemies inside.
	 */
	@Pair(a = "empower", b = "galvanize", name = "Dynamo Sigil", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A sigil within 4 blocks of the point lasts 8 seconds. Every 2 seconds allies inside gain Strength II for 4 "
			+ "seconds, and enemies inside take 2 lightning damage.")
	public static void dynamoSigil(PairCast c) {
		Vec3 at = c.point();
		double reach = 4 * c.radius;
		// Four pulses two seconds apart: the sigil lasts 8 seconds, as the rule text says.
		int pulses = Math.max(1, (int) Math.round(4 * c.duration));
		c.sound(SoundEvents.BEACON_ACTIVATE, at, 0.6F, 1.6F);
		c.every(40, pulses, frame -> {
			c.ring(PairCast.dust(0x6AF0FF, 1.0F), at.add(0, 0.1, 0), reach, 30, frame * 0.3);
			c.star(PairCast.shift(0x6AF0FF, 0xC77DFF, 0.8F), at.add(0, 0.15, 0), 4, reach, frame * 0.4);
			c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 0.7F, 0.9F + frame * 0.1F);
			for (LivingEntity a : c.alliesNear(at, reach)) {
				c.effect(a, MobEffects.STRENGTH, 4, 1);
			}
			for (LivingEntity e : c.enemiesNear(at, reach)) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.5, 0), PairCast.mid(e), 0.3, 3);
				c.shock(e, 2 * c.power);
			}
		});
	}

	/**
	 * Thunderhorn: stormheart and warcry. The horn's note rings red and the allies round the target swell with Strength
	 * and Speed; then the storm answers in two thunder rings that throw the enemies back.
	 */
	@Pair(a = "stormheart", b = "warcry", name = "Thunderhorn", element = "storm", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A war horn: allies within 8 blocks of the target gain Strength I and Speed I for 12 seconds. Then a thunder "
			+ "wave rolls out twice, a second apart: enemies within 6 blocks take 3 lightning each time and are shoved back.")
	public static void thunderhorn(PairCast c) {
		Vec3 at = c.point();
		c.sound(SoundEvents.RAID_HORN, at, 1.0F, 1.0F);
		c.wave(PairCast.dust(0xB3121E, 1.2F), at.add(0, 1.0, 0), 30, 0.5);
		for (LivingEntity a : c.alliesNear(at, 8 * c.radius)) {
			c.effect(a, MobEffects.STRENGTH, 12, 0);
			c.effect(a, MobEffects.SPEED, 12, 0);
		}
		c.every(20, 2, frame -> {
			double ringR = frame == 0 ? 3 * c.radius : 6 * c.radius;
			c.ring(PairCast.shift(0xA8F0FF, 0xFFFFFF, 1.0F), at.add(0, 0.3, 0), ringR, 36, frame * 0.2);
			c.sound(SoundEvents.TRIDENT_THUNDER, at, 0.9F, 1.0F - frame * 0.2F);
			for (LivingEntity e : c.enemiesNear(at, 6 * c.radius)) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 2, 0), PairCast.mid(e), 0.4, 3);
				c.shock(e, 3 * c.power);
				c.knockFrom(e, at, 0.8, 0.2);
			}
		});
	}

	/**
	 * Rainbloom: grow and rain cloud. A small cloud drizzles on the point: each second the crops in its patch
	 * drink the drizzle as bonemeal, and anyone standing in the rain is doused and mended a little.
	 */
	@Pair(a = "grow", b = "rain_cloud", name = "Rainbloom", element = "life", kind = EffectKind.WORLD,
		traits = {"radius", "duration"},
		text = "A small cloud rains on the point for 8 seconds, within 4 blocks: each second up to 8 crops there grow a stage "
			+ "as bonemeal would, and allies in the rain are put out and healed 1 health.")
	public static void rainbloom(PairCast c) {
		Vec3 at = c.point();
		double reach = 4 * c.radius;
		int reachBlocks = (int) Math.ceil(reach);
		int drops = Math.max(1, (int) Math.round(8 * c.duration));
		BlockPos centre = BlockPos.containing(at);
		c.particles(ParticleTypes.CLOUD, at.add(0, 3.5, 0), 40, 1.4 * c.radius, 0.02);
		c.sound(SoundEvents.WEATHER_RAIN, at, 0.5F, 1.2F);
		c.every(20, drops, frame -> {
			c.column(PairCast.dust(0x6FB8FF, 0.8F), at.add(0, 0.5, 0), reach, 3, 30);
			int grown = 0;
			for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reachBlocks, -1, -reachBlocks),
					centre.offset(reachBlocks, 1, reachBlocks))) {
				if (grown >= 8) {
					break;
				}
				if (Vec3.atCenterOf(pos).distanceTo(at) > reach) {
					continue;
				}
				BlockPos spot = pos.immutable();
				BlockState state = c.level.getBlockState(spot);
				if (!(state.getBlock() instanceof BonemealableBlock)) {
					continue;
				}
				if (!Casters.mayBuild(c.caster) || !Casters.mayEdit(c.caster, c.level, spot)) {
					continue;
				}
				if (!c.cast.takeBlock()) {
					break;
				}
				if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), c.level, spot)) {
					grown++;
					c.level.levelEvent(null, 1505, spot, 15);
				}
			}
			for (LivingEntity a : c.still(c.alliesNear(at, reach))) {
				c.douse(a);
				c.heal(a, 1);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(a), 6, 0.4, 0.02);
			}
		});
	}

	/**
	 * Skyfall: levitate and lightning. Enemies are flung up and hang in the air for two seconds, a wind coiling
	 * beneath them, then the lightning comes down on each one that is still there.
	 */
	@Pair(a = "levitate", b = "lightning", name = "Skyfall", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Launches up to 8 enemies upward and levitates them for 2 seconds. Then lightning falls on each one: 10 damage, "
			+ "set alight for 2 seconds and Slowness I for 2 seconds.")
	public static void skyfall(PairCast c) {
		List<LivingEntity> lifted = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		int hang = c.ticks(2);
		for (LivingEntity t : lifted) {
			c.lift(t, 1.0);
			c.effect(t, MobEffects.LEVITATION, 2, 0);
		}
		c.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, c.point(), 0.8F, 1.2F);
		// The wind coils up under each target and climbs with it.
		c.every(4, 6, frame -> {
			for (LivingEntity t : c.still(lifted)) {
				c.spiral(PairCast.dust(0xE6F4FF, 0.8F), t.position(), 0.9, 2.2, 2, 16);
			}
		});
		c.later(hang, () -> {
			c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, c.point(), 0.8F, 1.0F);
			for (LivingEntity t : c.still(lifted)) {
				Vec3 at = PairCast.mid(t);
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, at.add(0, 9, 0), at, 0.6, 4);
				c.bolt(at);
				c.shock(t, 10 * c.power);
				c.ignite(t, 2);
				c.effect(t, MobEffects.SLOWNESS, 2, 0);
			}
			c.shake(c.point(), 0.4F, 10);
		});
	}

	/**
	 * Dead Static: shock and silence. Each target is shocked and goes quiet: it can't cast, and monsters grow weak. The
	 * static it leaves hangs round it for three seconds and crackles through anyone close.
	 */
	@Pair(a = "shock", b = "silence", name = "Dead Static", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Each target takes 4 lightning and can't cast for 4 seconds (3 on players); monsters are also weakened "
			+ "(Weakness I for 6 seconds). For 3 seconds, every second, enemies within 2.5 blocks of a silenced target take 2 lightning damage.")
	public static void deadStatic(PairCast c) {
		List<LivingEntity> hushed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : hushed) {
			c.shock(t, 4 * c.power);
			CastLock.lock(t, c.ticks(t instanceof Player ? 3 : 4));
			if (t instanceof Mob) {
				c.effect(t, MobEffects.WEAKNESS, 6, 0);
			}
			c.sound(SoundEvents.SCULK_CLICKING, PairCast.mid(t), 0.9F, 0.5F);
		}
		c.sound(SoundEvents.ELDER_GUARDIAN_CURSE, c.point(), 0.5F, 1.8F);
		c.every(20, 3, frame -> {
			for (LivingEntity t : c.still(hushed)) {
				Vec3 at = PairCast.mid(t);
				c.disc(PairCast.dust(0x2B2F3A, 1.2F), t.position(), 2.5 * c.radius, 26);
				c.particles(ParticleTypes.SQUID_INK, at, 6, 0.4, 0.05);
				for (LivingEntity e : c.enemiesNear(at, 2.5 * c.radius)) {
					c.shock(e, 2 * c.power);
				}
			}
		});
	}
}
