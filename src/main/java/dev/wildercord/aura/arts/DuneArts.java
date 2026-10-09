package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.DuneRules;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Dune Breath's arts: sand and the desert. Dune fights with what's under a foe's feet and in its eyes: grit that blinds (a creature
 * loses its target), quicksand that slows it deep, and footing that shifts.
 * <ul>
 * <li><b>Grit Flick</b> (I): a cone of thrown sand: blinded and slowed.</li>
 * <li><b>Quicksand</b> (II): the ground ahead turns to sand that swallows feet and cuts each beat.</li>
 * <li><b>Sandveil</b> (III): a burst of sand all round: foes blinded, and you hidden in it a while (shots turn aside).</li>
 * <li><b>Dune Runner</b> (IV): across the sand, the footing shifting under the foes you pass.</li>
 * <li><b>Sea of Sand</b> (V): the desert rises round you: gusts that blind and sink, and at the end the dunes fall on them.</li>
 * </ul>
 */
public final class DuneArts {
	private DuneArts() {}

	public static final String METHOD = "dune";
	public static final String GRIT_FLICK = "grit_flick";
	public static final String QUICKSAND = "quicksand";
	public static final String SANDVEIL = "sandveil";
	public static final String DUNE_RUNNER = "dune_runner";
	public static final String SEA_OF_SAND = "sea_of_sand";

	public static final String SAND = "dune_quicksand";
	public static final String SEA = "dune_sea";

	private static final int GOLD = MethodsAFlavours.GOLD;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, GRIT_FLICK, DuneArts::gritFlick),
			MethodArts.art(AuraApi.ArtSlot.SECOND, QUICKSAND, DuneArts::quicksand),
			MethodArts.art(AuraApi.ArtSlot.THIRD, SANDVEIL, DuneArts::sandveil),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, DUNE_RUNNER, DuneArts::duneRunner),
			MethodArts.art(AuraApi.ArtSlot.FINAL, SEA_OF_SAND, DuneArts::seaOfSand));
	}

	private static ParticleOptions dust() {
		return new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
	}

	private static void sand(ServerLevel level, Vec3 at, int count, double spread) {
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), at, count, spread, 0.1);
		Vfx.emit(level, dust(), at.add(0, 0.6, 0), Math.max(1, count / 2), spread, 0.02);
	}

	// ------------------------------------------------------------------ I. Grit Flick

	static boolean gritFlick(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.3F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_grit_flick", 1.0F, 1.1F);
		Vec3 dir = ArtKit.flat(player);
		Vec3 from = player.position().add(0, 0.8, 0);
		for (int i = 1; i <= (int) DuneRules.FLICK_REACH; i++) {
			sand(level, from.add(dir.scale(i)), 4 + i, 0.25 * i);
		}
		ArtLight.world(player).slash(from.add(dir.scale(1.5)), ArtKit.UP.add(dir.scale(-0.5)).normalize(), dir, color, 1.8, 1.6, 0.2, 1, 6);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), DuneRules.FLICK_REACH, DuneRules.FLICK_DEGREES, DuneRules.FLICK_TARGETS)) {
			hits.strike(foe, DuneRules.FLICK_FACTOR, AuraFxRules.Weight.LIGHT);
			MethodsAFlavours.blind(player, foe, DuneRules.FLICK_BLIND);
			ArtKit.slow(player, foe, DuneRules.FLICK_SLOW, 0);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Quicksand

	static boolean quicksand(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.LOW, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 centre = player.position().add(ArtKit.flat(player).scale(DuneRules.QUICKSAND_AHEAD));
		Vec3 floor = ArtKit.floor(level, centre.add(0, 0.5, 0), 0.5, 3);
		Vec3 heart = floor == null ? centre : floor;
		Feels.sound(level, heart.add(0, 0.5, 0), "aura_art_quicksand", 1.0F, 1.0F);
		ArtLight.world(player).ground(heart.add(0, 0.04, 0), SigilOption.CRACKED, color, DuneRules.QUICKSAND_RADIUS, DuneRules.QUICKSAND_TICKS, 0.02);
		ArtLight.world(player).ground(heart.add(0, 0.05, 0), SigilOption.GLOW, GOLD, DuneRules.QUICKSAND_RADIUS * 0.8, DuneRules.QUICKSAND_TICKS, -0.03);
		ArtFields.open(player, SAND, ArtFields.disc(() -> heart, DuneRules.QUICKSAND_RADIUS, 2.0), DuneRules.QUICKSAND_TICKS, 2, (field, owner, age) -> {
			if (age % 6 == 0) {
				sand(field.level(), heart.add(0, 0.1, 0), 6, DuneRules.QUICKSAND_RADIUS * 0.5);
			}
			for (LivingEntity foe : field.foes(owner)) {
				ArtKit.draw(foe, heart, DuneRules.QUICKSAND_SUCK);
				if (age % DuneRules.QUICKSAND_PERIOD == 0) {
					MethodsAFlavours.sink(owner, foe, DuneRules.QUICKSAND_PERIOD + 10, DuneRules.QUICKSAND_DEPTH);
				}
				if (age % (DuneRules.QUICKSAND_PERIOD * 4) == 0) {
					hits.strike(foe, DuneRules.QUICKSAND_FACTOR / 2, AuraFxRules.Weight.LIGHT);
				}
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ III. Sandveil

	static boolean sandveil(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_sandveil", 1.1F, 1.0F);
		for (LivingEntity foe : ArtKit.around(player, feet, DuneRules.VEIL_RADIUS, 1.5, 3.0, DuneRules.VEIL_TARGETS)) {
			hits.strike(foe, DuneRules.VEIL_FACTOR, AuraFxRules.Weight.FULL);
			MethodsAFlavours.blind(player, foe, DuneRules.VEIL_BLIND);
		}
		ArtLight.spectacle(player).swirl(feet, DuneRules.VEIL_RADIUS * 0.6, 2.6, 5, color, GOLD);
		ArtLight.world(player).groundRing(feet, GOLD, 0.4, DuneRules.VEIL_RADIUS * 1.2, 0.18, 10);
		sand(level, feet.add(0, 0.6, 0), 30, DuneRules.VEIL_RADIUS * 0.5);
		// Hidden in the sand a while: shots turn aside (the eye's ward), and you step light.
		ArtWards.eye(player, DuneRules.VEIL_TICKS);
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, DuneRules.VEIL_TICKS, 0, false, false, true));
		return true;
	}

	// ------------------------------------------------------------------ IV. Dune Runner

	static boolean duneRunner(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, DuneRules.RUNNER_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, DUNE_RUNNER);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		AuraStep.afterimages(player, from, path.getLast(), dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_dune_runner", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		List<Vec3> trail = new ArrayList<>();
		ArtKit.dash(player, path, DuneRules.RUNNER_TICKS, (a, b, step, last) -> {
			trail.add(b);
			world.ray(a.add(0, 0.2, 0), b.add(0, 0.2, 0), GOLD, 0.35, 14);
			sand(level, b.add(0, 0.2, 0), 6, 0.4);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, DuneRules.RUNNER_WIDTH / 2 + 0.3, 2.2,
					DuneRules.RUNNER_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= DuneRules.RUNNER_TARGETS) {
					continue;
				}
				hits.strike(foe, DuneRules.RUNNER_FACTOR, AuraFxRules.Weight.FULL);
				// The footing shifts under it: it sinks, and turns half round.
				MethodsAFlavours.sink(player, foe, DuneRules.RUNNER_SINK, 1);
				foe.setYRot(foe.getYRot() + 90);
			}
			if (last) {
				world.groundRing(b, GOLD, 0.4, 2.2, 0.12, 9);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Sea of Sand

	static boolean seaOfSand(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_sea_of_sand", 1.3F, 1.0F);
		Vec3 heart = player.position();
		int ticks = DuneRules.SEA_GUSTS * DuneRules.SEA_PERIOD;
		ArtLight.world(player).ground(heart.add(0, 0.04, 0), SigilOption.CRACKED, ArtKit.color(player), DuneRules.SEA_RADIUS, ticks + 10, 0.01);
		ArtFields.open(player, SEA, ArtFields.disc(() -> heart, DuneRules.SEA_RADIUS, 3.0), ticks, DuneRules.SEA_PERIOD,
			(field, owner, age) -> sea(owner, field, hits, heart, age, ticks));
		return true;
	}

	/** One gust of the sea of sand; the last one is the dunes falling. */
	private static void sea(ServerPlayer owner, ArtFields.Field field, ArtKit.Hits hits, Vec3 heart, long age, int ticks) {
		ServerLevel level = field.level();
		int color = ArtKit.color(owner);
		RandomSource r = level.getRandom();
		ArtLight.spectacle(owner).swirl(heart, DuneRules.SEA_RADIUS * 0.7, 3.2, 6, color, GOLD);
		ArtLight.world(owner).groundRing(heart, GOLD, DuneRules.SEA_RADIUS, 0.8, 0.1, 8);
		for (int i = 0; i < 5; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			sand(level, heart.add(Math.cos(a) * DuneRules.SEA_RADIUS * 0.7, 0.6, Math.sin(a) * DuneRules.SEA_RADIUS * 0.7), 4, 0.6);
		}
		List<LivingEntity> inside = field.foes(owner);
		if (inside.size() > DuneRules.SEA_TARGETS) {
			inside = inside.subList(0, DuneRules.SEA_TARGETS);
		}
		boolean end = age >= ticks;
		for (LivingEntity foe : inside) {
			if (end) {
				hits.strike(foe, DuneRules.SEA_CRUSH, AuraFxRules.Weight.GRAND);
				MethodsAFlavours.sink(owner, foe, 40, DuneRules.SEA_DEPTH + 1);
			} else {
				hits.strike(foe, DuneRules.SEA_FACTOR, null);
				MethodsAFlavours.sink(owner, foe, DuneRules.SEA_PERIOD + 10, DuneRules.SEA_DEPTH);
				if (age % (DuneRules.SEA_PERIOD * 2) == 0) {
					MethodsAFlavours.blind(owner, foe, DuneRules.SEA_BLIND);
				}
			}
		}
		if (end) {
			ArtLight.world(owner).groundRing(heart, color, 0.5, DuneRules.SEA_RADIUS * 1.4, 0.25, 12);
			sand(level, heart.add(0, 0.5, 0), 60, DuneRules.SEA_RADIUS * 0.6);
			AuraFx.sound(owner, AuraFx.Sound.IMPACT, 0.9F, 0.8F);
		}
	}
}
