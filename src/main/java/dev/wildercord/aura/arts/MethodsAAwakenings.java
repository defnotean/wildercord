package dev.wildercord.aura.arts;

import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.DuneRules;
import dev.wildercord.aura.IronRules;
import dev.wildercord.aura.TideRules;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Vfx;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tide, Iron and Dune's awakenings ({@link Awakenings} hands their flavours here): the flourish round the body, and the sovereign
 * Dominion's ground, what it does when raised and each beat after. The Drowning Tide pushes foes to its rim and soaks them, and puts
 * its owner's fire out; the Anvil Court cracks every foe's armour and hardens its owner; the Shifting Sea sinks foes and blinds one.
 */
public final class MethodsAAwakenings {
	private MethodsAAwakenings() {}

	/** The burst of the awakening, round the body. */
	public static void flourish(ServerPlayer player, AwakeningRules.Flavour flavour) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		switch (flavour) {
			case TIDE -> {
				show.swirl(feet, 1.1, 3.0, 5, color, MethodsAFlavours.FOAM);
				show.groundRing(feet, MethodsAFlavours.FOAM, 0.5, 3.6, 0.14, 16);
				world.ground(feet, SigilOption.RING, color, 2.6, 50, 0.04);
				Vfx.emit(level, ParticleTypes.SPLASH, feet.add(0, 0.4, 0), 40, 1.2, 0.3);
				Vfx.emit(level, ParticleTypes.BUBBLE_POP, feet.add(0, 1.0, 0), 20, 0.8, 0.1);
			}
			case IRON -> {
				show.groundRing(feet, MethodsAFlavours.SPARK, 0.5, 3.4, 0.16, 14);
				show.shards(feet.add(0, 1.0, 0), 2.6, 10, color, MethodsAFlavours.SPARK);
				world.ground(feet, SigilOption.TARGET, color | ArtLight.DARK, 2.6, 50, 0);
				Vfx.emit(level, ParticleTypes.LAVA, feet.add(0, 0.6, 0), 8, 0.6, 0.2);
				Vfx.emit(level, ParticleTypes.CRIT, feet.add(0, 1.0, 0), 30, 0.9, 0.4);
			}
			case DUNE -> {
				show.swirl(feet, 1.3, 3.2, 6, color, MethodsAFlavours.GOLD);
				show.groundRing(feet, MethodsAFlavours.GOLD, 0.5, 3.8, 0.14, 16);
				world.ground(feet, SigilOption.CRACKED, color, 2.8, 50, 0.02);
				Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), feet.add(0, 1.4, 0), 40, 1.4, 0.05);
			}
			default -> { }
		}
	}

	/** Raised: what it does at once to the foes inside. */
	public static void raised(ServerPlayer owner, AwakeningRules.Flavour flavour, List<LivingEntity> foes, Vec3 centre, double radius, int color) {
		ArtLight world = ArtLight.world(owner);
		switch (flavour) {
			case TIDE -> {
				for (LivingEntity foe : foes) {
					MethodsAFlavours.soak(owner, foe, TideRules.SOVEREIGN_SOAK * 2);
				}
				world.ring(centre.add(0, 0.1, 0), ArtKit.UP, MethodsAFlavours.FOAM, 0.3, radius, 0.1, 12);
			}
			case IRON -> {
				for (LivingEntity foe : foes) {
					MethodsAFlavours.sunder(owner, foe, IronRules.SOVEREIGN_ARMOUR * 2, IronRules.SOVEREIGN_TICKS * 2);
				}
				world.ring(centre.add(0, 0.1, 0), ArtKit.UP, MethodsAFlavours.SPARK, 0.3, radius, 0.1, 12);
			}
			case DUNE -> {
				for (LivingEntity foe : foes) {
					MethodsAFlavours.sink(owner, foe, AwakeningRules.Sovereign.PULSE + 10, DuneRules.SOVEREIGN_DEPTH);
				}
				world.ring(centre.add(0, 0.1, 0), ArtKit.UP, MethodsAFlavours.GOLD, 0.3, radius, 0.1, 12);
			}
			default -> { }
		}
	}

	/** The ground it lies on. */
	public static void ground(ServerPlayer owner, AwakeningRules.Flavour flavour, Vec3 at, double radius, int color, int ticks) {
		ArtLight world = ArtLight.world(owner);
		switch (flavour) {
			case TIDE -> {
				world.ground(at, SigilOption.RING, MethodsAFlavours.FOAM, radius * 0.96, ticks, 0.02);
				world.ground(at, SigilOption.BAND, color, radius * 1.0, ticks, -0.01);
			}
			case IRON -> {
				world.ground(at, SigilOption.TARGET, color | ArtLight.DARK, radius * 0.96, ticks, 0);
				world.ground(at, SigilOption.BAND, MethodsAFlavours.SPARK, radius * 1.0, ticks, 0.01);
			}
			case DUNE -> {
				world.ground(at, SigilOption.CRACKED, color, radius * 0.96, ticks, 0.005);
				world.ground(at, SigilOption.BAND, MethodsAFlavours.GOLD, radius * 1.0, ticks, -0.02);
			}
			default -> { }
		}
	}

	/** One tick of it standing. */
	public static void tick(ServerPlayer owner, AwakeningRules.Flavour flavour, List<LivingEntity> foes, long age, boolean ownerInside, Vec3 centre,
			double radius, int color, ArtKit.Hits hits) {
		ServerLevel level = owner.level();
		int pulse = AwakeningRules.Sovereign.PULSE;
		boolean beat = age > 0 && age % pulse == 0;
		RandomSource r = level.getRandom();
		switch (flavour) {
			case TIDE -> {
				if (ownerInside && age % 10 == 0) {
					owner.clearFire();
				}
				if (beat) {
					for (LivingEntity foe : foes) {
						Vec3 away = foe.position().subtract(centre);
						away = new Vec3(away.x, 0, away.z);
						if (away.lengthSqr() > 1.0E-4) {
							MethodsAFlavours.current(foe, away.normalize().scale(TideRules.SOVEREIGN_PUSH).add(0, 0.05, 0));
						}
						Reactions.mark(foe, Reactions.Mark.WET, TideRules.SOVEREIGN_SOAK);
						foe.clearFire();
					}
					ArtLight.world(owner).groundRing(centre, MethodsAFlavours.FOAM, radius * 0.2, radius, 0.08, 10);
				}
			}
			case IRON -> {
				if (ownerInside && age % 10 == 0) {
					ArtWards.harden(owner, 25);
				}
				if (beat) {
					for (LivingEntity foe : foes) {
						MethodsAFlavours.sunder(owner, foe, IronRules.SOVEREIGN_ARMOUR, IronRules.SOVEREIGN_TICKS);
					}
					if (!foes.isEmpty()) {
						LivingEntity foe = foes.get(r.nextInt(foes.size()));
						Vec3 at = foe.getBoundingBox().getCenter();
						ArtLight.world(owner).bare().ray(at.add(0, 5, 0), at, MethodsAFlavours.SPARK, 0.12, 6);
						hits.strike(foe, AwakeningRules.Sovereign.THUNDER_BOLT * 0.6, AuraFxRules.Weight.LIGHT);
					}
				}
			}
			case DUNE -> {
				if (age % 5 == 0) {
					double a = r.nextDouble() * Math.PI * 2;
					Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
						centre.add(Math.cos(a) * radius * 0.7, 1.5, Math.sin(a) * radius * 0.7), 4, 0.5, 0.02);
				}
				if (beat) {
					for (LivingEntity foe : foes) {
						MethodsAFlavours.sink(owner, foe, pulse + 10, DuneRules.SOVEREIGN_DEPTH);
					}
					if (!foes.isEmpty()) {
						MethodsAFlavours.blind(owner, foes.get(r.nextInt(foes.size())), DuneRules.SOVEREIGN_BLIND);
					}
				}
			}
			default -> { }
		}
	}
}
