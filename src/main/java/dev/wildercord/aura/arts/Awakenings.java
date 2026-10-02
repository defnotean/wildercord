package dev.wildercord.aura.arts;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Awakening's method-shaped parts, beside the arts that share their looks:
 * <ul>
 * <li>{@link #flourish}: the burst of an awakening in its method's own element (a ring of flame, ice bursting, bolts out of the sky, a
 *     rising wind, the ground cracking under stone, a flower of light, a black point bursting, a star, a clock face, crescents of
 *     blood). The frame round it (the flash, the shockwave, the column of light, the banner, the voice) is {@code aura.AwakeningFx}.</li>
 * <li>{@link Ground}: a Dominion raised while awakened (Sovereign), shaped by the method ({@link AwakeningRules.Flavour}): what it does
 *     to the foes and allies inside, beat by beat, on top of an ordinary Dominion's slow, weakening and chain (which
 *     {@code aura.AuraDominion} keeps, stronger).</li>
 * </ul>
 * Everything a foe suffers goes through {@link ArtKit} (so a player is held, thrown, burned and silenced only as long as any art may,
 * a boss is only slowed), and every strike through one {@link ArtKit.Hits} for the whole Dominion (so another player takes no more from
 * all its strikes together than from one art).
 */
public final class Awakenings {
	private Awakenings() {}

	private static final int WHITE = 0xFFFFFF;

	// ------------------------------------------------------------------ the transformation's flourish

	/**
	 * The burst of {@code player}'s awakening in their method's element, round their body. What stands up round them is spectacle (seen
	 * by everyone else, and by them only in third person); what lies on the ground everyone sees.
	 */
	public static void flourish(ServerPlayer player) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		Vec3 heart = feet.add(0, 1.0, 0);
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		RandomSource r = level.getRandom();
		switch (AwakeningRules.Flavour.of(Aura.data(player).method())) {
			case EMBER -> {
				int gold = ElementFx.FIRE.secondary();
				int red = ElementFx.FIRE.accent();
				show.tongues(feet, 0.85, 2.6, 12, color, red, 18);
				show.tongues(feet, 0.55, 3.2, 6, gold, color, 14);
				world.groundRing(feet, red, 0.6, 3.4, 0.16, 16);
				world.ground(feet, SigilOption.CRACKED, red | ArtLight.DARK, 2.6, 50, 0);
				for (int i = 0; i < 18; i++) {
					Vec3 dir = ElementFx.randomDir(r).add(0, 1.1, 0).normalize();
					Motes.fling(level, heart, dir, 0.2 + r.nextDouble() * 0.2, i % 3 == 0 ? WHITE : i % 2 == 0 ? color : gold, 0.07, 18 + r.nextInt(12),
						new Vec3(0, 0.012, 0));
				}
			}
			case RIME -> {
				int frost = ElementFx.FROST.primary();
				show.shards(heart, 2.8, 18, frost, WHITE);
				world.ring(feet.add(0, 0.12, 0), ArtKit.UP, ElementFx.FROST.accent(), 0.4, 3.2, 0.12, 14);
				world.ground(feet, SigilOption.STAR, frost, 2.8, 50, 0.02);
				ElementFx.frostCreep(level, feet, 2.6, 50);
				RimeArts.chips(level, heart, 14, 0.28);
			}
			case THUNDER -> {
				int pale = ElementFx.STORM.secondary();
				double phase = r.nextDouble() * Math.PI * 2;
				for (int i = 0; i < 3; i++) {
					double a = phase + Math.PI * 2 * i / 3;
					Vec3 at = feet.add(Math.cos(a) * 2.6, 0, Math.sin(a) * 2.6);
					ElementFx.bolt(level, at.add(0, 12, 0), at, 0.1, 2, 2, WHITE, color);
					world.groundRing(at, color, 0.2, 1.4, 0.08, 8);
					world.bare().arc(heart, at.add(0, 0.2, 0), pale, 0.06, 1, false, 5);
				}
				world.groundRing(feet, pale, 0.4, 3.4, 0.1, 10);
				Feels.sound(level, heart, "storm_boom", 0.9F, 1.2F);
			}
			case GALE -> {
				show.swirl(feet, 1.25, 3.4, 7, color, WHITE);
				show.whirl(heart, 1.6, 6, color, WHITE, ElementFx.WIND.accent());
				world.groundRing(feet, color, 0.5, 4.0, 0.1, 14);
				world.bare().groundRing(feet, WHITE, 0.3, 2.8, 0.04, 10);
				for (int i = 0; i < 12; i++) {
					double a = Math.PI * 2 * i / 12;
					Vec3 rim = feet.add(Math.cos(a) * 1.2, 0.2, Math.sin(a) * 1.2);
					Motes.fling(level, rim, new Vec3(-Math.sin(a), 0.9, Math.cos(a)), 0.22, i % 2 == 0 ? WHITE : color, 0.06, 18, new Vec3(0, 0.01, 0));
				}
			}
			case STONE -> {
				BlockState earth = ArtBlocks.ground(level, feet);
				StoneArts.crackUnder(player, feet, 2.8, 50, earth);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8 + Math.PI / 8;
					ArtBlocks.slab(level, feet.add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5), earth, (float) (a + Math.PI / 2), 0.55F, -0.4F, 1 + i % 2, 18);
				}
				world.groundRing(feet, ElementFx.EARTH.secondary(), 0.4, 3.6, 0.14, 12);
			}
			case VERDANT -> {
				VerdantArts.bloom(world, feet, color, 1.5, 50);
				VerdantArts.leaves(level, heart.add(0, 0.4, 0), 1.2, 16);
				show.tongues(feet, 0.6, 2.2, 8, color, ElementFx.LIFE.secondary(), 16);
			}
			case HOLLOW -> {
				HollowArts.hole(show, heart.add(0, 0.2, 0), 0.55, color, 6);
				show.shade(heart.add(0, 0.2, 0), 0x1A0830 | ArtLight.DARK, 1.6F);
				Scheduler.later(3, () -> {
					if (player.isAlive()) {
						Vec3 c = player.position().add(0, 1.2, 0);
						ArtLight.spectacle(player).bare().ring(c, ArtKit.UP, ElementFx.VOID.secondary(), 0.4, 3.0, 0.06, 10);
						ArtLight.world(player).groundRing(player.position(), color, 0.4, 3.4, 0.12, 12);
					}
				});
				world.ground(feet, SigilOption.RING, 0x1A0830 | ArtLight.DARK, 2.6, 40, -0.02);
			}
			case STARLIT -> {
				StarlitArts.burst(player, heart.add(0, 0.9, 0), color, 2.2, 4);
				world.ground(feet, SigilOption.STAR, color, 2.6, 50, 0.03);
				for (int i = 0; i < 5; i++) {
					double a = Math.PI * 2 * i / 5;
					show.bare().ray(heart, heart.add(Math.cos(a) * 2.2, 1.6, Math.sin(a) * 2.2), ElementFx.ARCANE.secondary(), 0.05, 10);
				}
			}
			case HOURGLASS -> {
				int gold = HourglassArts.gold(player);
				HourglassArts.clockFace(world, feet.add(0, 0.08, 0), ArtKit.UP, 2.4, r.nextDouble() * Math.PI * 2, gold, 50);
				world.groundRing(feet, gold, 0.3, 3.2, 0.1, 12);
				Motes.burst(level, heart, 10, ElementFx.TIME.secondary(), 0.07, 18, 0.12);
			}
			case CRIMSON -> {
				int pale = ElementFx.BLOOD.secondary();
				double phase = r.nextDouble() * Math.PI * 2;
				for (int i = 0; i < 4; i++) {
					double a = phase + Math.PI / 2 * i;
					Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
					show.slash(heart.add(out.scale(0.4)), ArtKit.UP, out, i % 2 == 0 ? color : pale, 1.4, 1.5, 0.14, 2, 10);
				}
				ElementFx.pulse(level, feet.add(0, 0.1, 0), ArtKit.UP, 2.8);
				world.groundRing(feet, ElementFx.BLOOD.accent() | ArtLight.DARK, 0.4, 3.0, 0.16, 14);
				CrimsonArts.drops(level, heart, 0.7, 14);
			}
			case PLAIN -> {
				world.groundRing(feet, color, 0.4, 3.4, 0.12, 14);
				world.bare().ground(feet, SigilOption.CIRCLE, ArtKit.hot(color, 0.4), 2.4, 40, 0.02);
				show.tongues(feet, 0.7, 2.4, 8, color, WHITE, 14);
			}
		}
	}

	// ------------------------------------------------------------------ the Sovereign's Dominion

	/** The ground {@code owner}'s awakened Dominion claims: its method's shape, raised at {@code centre}, {@code radius} out, for {@code ticks}. */
	public static Ground ground(ServerPlayer owner, Vec3 centre, double radius, int ticks) {
		return new Ground(owner, AwakeningRules.Flavour.of(Aura.data(owner).method()), centre, radius, ticks);
	}

	/**
	 * An awakened Dominion's shape: its method's flavour, what it has struck (one {@link ArtKit.Hits}, so the PvP cap holds over the
	 * whole of it), and what it has slowed already (Hourglass's shots).
	 */
	public static final class Ground {
		private final AwakeningRules.Flavour flavour;
		private final Vec3 centre;
		private final double radius;
		private final int color;
		private final ArtKit.Hits hits;
		private final Set<UUID> slowedShots = new HashSet<>();

		Ground(ServerPlayer owner, AwakeningRules.Flavour flavour, Vec3 centre, double radius, int ticks) {
			this.flavour = flavour;
			this.centre = centre;
			this.radius = radius;
			this.color = ArtKit.color(owner);
			this.hits = ArtKit.hits(owner, AuraFx.art(owner));
		}

		public AwakeningRules.Flavour flavour() {
			return flavour;
		}

		/**
		 * Raised: its look rising in its method's shape, and what it does at once to the foes inside ({@code foes}): Rime freezes them,
		 * Verdant roots them, Hollow silences them, Hourglass holds them still.
		 */
		public void raised(ServerPlayer owner, List<LivingEntity> foes, int ticks) {
			ServerLevel level = owner.level();
			ArtLight world = ArtLight.world(owner);
			switch (flavour) {
				case EMBER -> {
					world.ground(centre, SigilOption.CRACKED, ElementFx.FIRE.accent() | ArtLight.DARK, radius * 1.05, ticks, 0);
					rim(owner, ElementFx.FIRE.accent());
				}
				case RIME -> {
					world.ground(centre, SigilOption.STAR, ElementFx.FROST.secondary(), radius * 0.9, ticks, 0.01);
					ElementFx.frostCreep(level, centre, radius, ticks);
					for (LivingEntity foe : foes) {
						RimeArts.freezeSolid(owner, foe, AwakeningRules.Sovereign.RIME_FREEZE);
					}
				}
				case THUNDER -> world.ground(centre, SigilOption.TARGET, ElementFx.STORM.secondary(), radius * 0.9, ticks, 0.03);
				case GALE -> world.ground(centre, SigilOption.RING, ElementFx.WIND.secondary(), radius * 0.95, ticks, 0.06);
				case STONE -> world.ground(centre, SigilOption.CRACKED, ElementFx.EARTH.secondary(), radius * 1.05, ticks, 0);
				case VERDANT -> {
					VerdantArts.bloom(world, centre, color, radius / 2.4, Math.min(ticks, 60));
					for (LivingEntity foe : foes) {
						if (ArtKit.root(owner, foe, AwakeningRules.Sovereign.VERDANT_ROOT)) {
							VerdantArts.rootsOn(owner, foe, AwakeningRules.Sovereign.VERDANT_ROOT, 3, 1.0F);
						}
					}
				}
				case HOLLOW -> {
					world.ground(centre, SigilOption.RING, 0x1A0830 | ArtLight.DARK, radius * 0.9, ticks, -0.03);
					for (LivingEntity foe : foes) {
						if (ArtWards.silence(foe, AwakeningRules.Sovereign.HOLLOW_SILENCE) > 0) {
							HollowArts.silencedLook(owner, foe, AwakeningRules.Sovereign.HOLLOW_SILENCE);
						}
					}
				}
				case STARLIT -> world.ground(centre, SigilOption.STAR, color, radius * 0.95, ticks, 0.02);
				case HOURGLASS -> {
					int gold = HourglassArts.gold(owner);
					HourglassArts.clockFace(world, centre.add(0, 0.06, 0), ArtKit.UP, radius * 0.8, level.getRandom().nextDouble() * Math.PI * 2, gold,
						Math.min(ticks, 200));
					for (LivingEntity foe : foes) {
						ArtKit.hold(owner, foe, AwakeningRules.Sovereign.HOURGLASS_HOLD);
					}
				}
				case CRIMSON -> {
					world.ground(centre, SigilOption.CIRCLE, ElementFx.BLOOD.accent() | ArtLight.DARK, radius * 1.0, ticks, 0.01);
					ElementFx.pulse(level, centre.add(0, 0.1, 0), ArtKit.UP, radius);
				}
				case PLAIN -> world.ground(centre, SigilOption.BAND, ArtKit.hot(color, 0.3), radius, ticks, 0.01);
			}
		}

		/**
		 * One tick of it, {@code age} ticks since it was raised: its method's beat on the foes inside ({@code foes}), and what it gives its
		 * owner while they stand in it ({@code ownerInside}).
		 */
		public void tick(ServerPlayer owner, List<LivingEntity> foes, long age, boolean ownerInside) {
			ServerLevel level = owner.level();
			int pulse = AwakeningRules.Sovereign.PULSE;
			boolean beat = age > 0 && age % pulse == 0;
			boolean everyOther = age > 0 && age % (pulse * 2) == 0;
			switch (flavour) {
				case EMBER -> {
					if (beat) {
						for (LivingEntity foe : foes) {
							ArtKit.ignite(foe, AwakeningRules.Sovereign.EMBER_BURN);
						}
						rim(owner, ElementFx.FIRE.accent());
					}
				}
				case RIME -> {
					if (beat) {
						for (LivingEntity foe : foes) {
							ArtKit.chill(owner, foe, pulse + 10, foe instanceof Player ? 0 : AwakeningRules.Sovereign.RIME_CHILL);
						}
					}
				}
				case THUNDER -> {
					if (beat && !foes.isEmpty()) {
						LivingEntity foe = foes.get(level.getRandom().nextInt(foes.size()));
						ThunderArts.strike(owner, foe.position(), color, 0.55F);
						hits.strike(foe, AwakeningRules.Sovereign.THUNDER_BOLT, AuraFxRules.Weight.FULL);
						ArtKit.shock(owner, foe, AwakeningRules.Sovereign.THUNDER_SHOCK);
					}
				}
				case GALE -> {
					if (ownerInside && age % 10 == 0) {
						// Shots loosed at the owner standing in it are turned aside by the wind (the Eye of the Storm's ward).
						ArtWards.eye(owner, 15);
					}
					if (everyOther) {
						for (LivingEntity foe : foes) {
							ArtKit.lift(foe, AwakeningRules.Sovereign.GALE_LIFT, AwakeningRules.Sovereign.GALE_AIRBORNE);
							ArtWards.juggled(owner, foe, AwakeningRules.Sovereign.GALE_AIRBORNE);
							ArtLight.world(owner).swirl(foe.position(), 0.6, 1.6, 3, color, WHITE);
						}
					}
				}
				case STONE -> {
					if (ownerInside && age % 10 == 0) {
						// Standing in it, the owner is the mountain: Resistance I, and nothing moves them.
						ArtWards.harden(owner, 25);
					}
					if (beat) {
						for (LivingEntity foe : foes) {
							dev.wildercord.api.AuraApi.wearStance(owner, foe, AwakeningRules.Sovereign.STONE_STANCE);
						}
						ArtLight.world(owner).groundRing(centre, ElementFx.EARTH.secondary(), radius * 0.2, radius, 0.08, 10);
					}
				}
				case VERDANT -> {
					if (beat) {
						for (LivingEntity ally : allies(owner)) {
							if (ArtKit.mend(owner, ally, AwakeningRules.Sovereign.VERDANT_MEND) > 0) {
								VerdantArts.leaves(level, ally.getBoundingBox().getCenter(), 0.3, 3);
							}
						}
					}
				}
				case HOLLOW -> {
					if (age % 2 == 0) {
						for (LivingEntity foe : foes) {
							ArtKit.drag(foe, centre, AwakeningRules.Sovereign.HOLLOW_DRAG, 0.8);
						}
					}
					if (beat) {
						for (LivingEntity foe : foes) {
							HollowArts.tendril(ArtLight.world(owner), foe.getBoundingBox().getCenter(), centre.add(0, 0.6, 0), color, 6);
						}
					}
				}
				case STARLIT -> {
					if (beat && !foes.isEmpty()) {
						LivingEntity foe = foes.get(level.getRandom().nextInt(foes.size()));
						boolean burst = ArtWards.burstStar(owner, foe);
						Vec3 at = foe.getBoundingBox().getCenter();
						ArtLight.world(owner).bare().ray(at.add(0, 6, 0), at, ElementFx.ARCANE.secondary(), 0.08, 6);
						StarlitArts.burst(owner, at, color, burst ? 1.4 : 0.9, (int) (age / pulse));
						hits.strike(foe, AwakeningRules.Sovereign.STARLIT_STAR * (burst ? 2 : 1), AuraFxRules.Weight.LIGHT);
						if (!burst) {
							ArtWards.star(owner, foe);
						}
					}
				}
				case HOURGLASS -> {
					if (beat) {
						for (LivingEntity foe : foes) {
							ArtKit.slow(owner, foe, pulse + 10, foe instanceof Player ? 0 : AwakeningRules.Sovereign.HOURGLASS_SLOW);
						}
					}
					if (age % 4 == 0) {
						slowShots(owner);
					}
				}
				case CRIMSON -> {
					if (everyOther) {
						for (LivingEntity foe : foes) {
							if (hits.strike(foe, AwakeningRules.Sovereign.CRIMSON_BLEED, AuraFxRules.Weight.LIGHT) > 0) {
								Reactions.mark(foe, Reactions.Mark.BLEEDING, 60);
								CrimsonArts.drip(foe);
							}
						}
					}
				}
				case PLAIN -> {
					// Only stronger: nothing of its own.
				}
			}
		}

		/** A blow of the owner's landed on {@code struck} inside, taking {@code taken}: Crimson's court drinks a share. */
		public void struck(ServerPlayer owner, LivingEntity struck, float taken) {
			if (flavour == AwakeningRules.Flavour.CRIMSON && taken > 0 && ArtKit.drink(owner, taken * AwakeningRules.Sovereign.CRIMSON_DRINK) > 0) {
				CrimsonArts.drinkLook(owner, struck);
			}
		}

		/** Ember's rim: tongues of flame round the edge. */
		private void rim(ServerPlayer owner, int second) {
			ArtLight world = ArtLight.world(owner);
			int count = (int) Math.round(radius * 3);
			double phase = owner.level().getRandom().nextDouble() * Math.PI * 2;
			for (int i = 0; i < count; i++) {
				double a = phase + Math.PI * 2 * i / count;
				world.tongues(centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 0.25, 1.1, 2, color, second, 12);
			}
		}

		/** The owner and the allies they may help standing inside. */
		private List<LivingEntity> allies(ServerPlayer owner) {
			AABB box = new AABB(centre, centre).inflate(radius + 1, 3, radius + 1);
			return owner.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && ArtKit.helpable(owner, e) && inside(e));
		}

		/** Hourglass: shots foes loose inside drag through the slowed time (each slowed once). */
		private void slowShots(ServerPlayer owner) {
			AABB box = new AABB(centre, centre).inflate(radius + 1, 4, radius + 1);
			for (Projectile shot : owner.level().getEntitiesOfClass(Projectile.class, box, p -> p.isAlive() && inside(p))) {
				if (shot.getOwner() == owner || !slowedShots.add(shot.getUUID())) {
					continue;
				}
				if (shot.getOwner() instanceof LivingEntity shooter && !ArtKit.harmable(owner, shooter)) {
					continue;
				}
				shot.setDeltaMovement(shot.getDeltaMovement().scale(0.45));
				shot.needsSync = true;
				ElementFx.sigil(owner.level(), shot.position(), ArtKit.UP, SigilOption.RING, HourglassArts.gold(owner), 0.35, 8, 0.1);
			}
		}

		private boolean inside(net.minecraft.world.entity.Entity e) {
			double dx = e.getX() - centre.x;
			double dz = e.getZ() - centre.z;
			double reach = radius + e.getBbWidth() / 2;
			return dx * dx + dz * dz <= reach * reach && e.getY() > centre.y - 2.0 && e.getY() < centre.y + 4.0;
		}
	}
}
