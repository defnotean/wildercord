package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.aura.MethodsBArtRules.*;

/**
 * Venom Breath's arts: the serpent. Every bite leaves a toxin that stacks with the next (poison, one stronger a bite, to a cap), its
 * sprays and coils weaken, and its body slips away: back out of reach, or weaving through a crowd. Its light is acid green on black.
 * <ul>
 * <li><b>Fang Strike</b> (I): a quick bite, its toxin stacking.</li>
 * <li><b>Spitting Cobra</b> (II): a spray of venom ahead, poisoning and weakening all it wets.</li>
 * <li><b>Shed Skin</b> (III): a cut and a slither back, the husk left behind poisoning whoever stands in it.</li>
 * <li><b>Serpent Slither</b> (IV): a weaving dash, every foe in the way bitten and weakened.</li>
 * <li><b>Hydra Coil</b> (V): five heads striking all round in turn, each bite stacking the toxin higher.</li>
 * </ul>
 */
public final class VenomArts {
	private VenomArts() {}

	public static final String METHOD = VENOM;
	public static final String FANG_STRIKE = "fang_strike";
	public static final String SPITTING_COBRA = "spitting_cobra";
	public static final String SHED_SKIN = "shed_skin";
	public static final String SERPENT_SLITHER = "serpent_slither";
	public static final String HYDRA_COIL = "hydra_coil";

	/** Shed Skin's husk, a field. */
	public static final String HUSK = "venom_husk";

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, FANG_STRIKE, VenomArts::fangStrike),
			MethodArts.art(AuraApi.ArtSlot.SECOND, SPITTING_COBRA, VenomArts::spittingCobra),
			MethodArts.art(AuraApi.ArtSlot.THIRD, SHED_SKIN, VenomArts::shedSkin),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, SERPENT_SLITHER, VenomArts::serpentSlither),
			MethodArts.art(AuraApi.ArtSlot.FINAL, HYDRA_COIL, VenomArts::hydraCoil));
	}

	/** The green the arts are drawn in: the aura's own colour taken toward acid green. */
	static int acid(ServerPlayer player) {
		return ArtKit.mix(ArtKit.color(player), MethodsBKit.ACID, 0.6);
	}

	/** A bite: the strike, then its toxin (and a fleck of venom). */
	static float bite(ArtKit.Hits hits, LivingEntity foe, double factor, AuraFxRules.Weight weight) {
		ServerPlayer player = hits.player();
		float taken = hits.strike(foe, factor, weight);
		if (taken > 0) MethodsBKit.toxin(player, foe, TOXIN_TICKS);
		Vfx.emit(player.level(), ParticleTypes.ITEM_SLIME, foe.getBoundingBox().getCenter(), 4, 0.25, 0.05);
		return taken;
	}

	// ------------------------------------------------------------------ I. Fang Strike

	static boolean fangStrike(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = acid(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.3F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_fang_strike", 1.0F, 1.0F);
		ArtLight world = ArtLight.world(player);
		// Two fangs of light, side by side.
		Vec3 side = ArtKit.right(look).scale(0.25);
		Vec3 from = feet.add(0, 1.0, 0).add(look.scale(0.6));
		world.ray(from.add(side), from.add(side).add(look.scale(FANG_REACH)), color, 0.12, 8);
		world.ray(from.subtract(side), from.subtract(side).add(look.scale(FANG_REACH)), MethodsBKit.BLACK, 0.1, 8);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), FANG_REACH, FANG_DEGREES, FANG_TARGETS)) {
			bite(hits, foe, FANG_FACTOR, AuraFxRules.Weight.HEAVY);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Spitting Cobra

	static boolean spittingCobra(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = acid(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		Vec3 mouth = player.getEyePosition().subtract(0, 0.3, 0);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, mouth, "aura_art_spitting_cobra", 1.1F, 1.0F);
		// A fan of venom, flecks and streaks.
		for (int i = -2; i <= 2; i++) {
			double a = Math.toRadians(COBRA_DEGREES / 4.0 * i);
			Vec3 d = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), 0, look.x * Math.sin(a) + look.z * Math.cos(a));
			ArtLight.world(player).ray(mouth, mouth.add(d.scale(COBRA_REACH)).subtract(0, 0.6, 0), i % 2 == 0 ? color : MethodsBKit.BLACK, 0.08, 10);
			Motes.glows(level, mouth.add(d.scale(COBRA_REACH * 0.6)), 4, 0.4, MethodsBKit.ACID, 0.07, 16, d.scale(0.05), 0.03);
		}
		for (LivingEntity foe : ArtKit.arcFrom(player, feet, look, COBRA_REACH, COBRA_DEGREES, COBRA_TARGETS)) {
			bite(hits, foe, COBRA_FACTOR, AuraFxRules.Weight.FULL);
			MethodsBKit.weaken(player, foe, COBRA_WEAKNESS);
		}
		return true;
	}

	// ------------------------------------------------------------------ III. Shed Skin

	static boolean shedSkin(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = acid(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.3F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_shed_skin", 1.1F, 1.0F);
		for (LivingEntity foe : ArtKit.around(player, feet, SHED_RADIUS, 1.0, 2.5, 6)) {
			bite(hits, foe, SHED_FACTOR, AuraFxRules.Weight.FULL);
		}
		// The husk: where you stood, a coil of green that poisons whoever stands in it.
		AuraStep.afterimages(player, feet, feet, look, MethodsBKit.BLACK);
		ArtFields.open(player, HUSK, ArtFields.disc(() -> feet, SHED_RADIUS, 2.0), SHED_TICKS, 5, (field, owner, age) -> {
			ServerLevel lv = field.level();
			if (age % 10 == 0) {
				Motes.glows(lv, feet.add(0, 0.3, 0), 6, SHED_RADIUS * 0.6, MethodsBKit.ACID, 0.07, 14, new Vec3(0, 0.02, 0), 0.02);
				ArtLight.world(owner).groundRing(feet.add(0, 0.05, 0), field.left() < 20 ? MethodsBKit.BLACK : acid(owner), SHED_RADIUS * 0.7, SHED_RADIUS, 0.06, 12);
			}
			if (age > 0 && age % SHED_PERIOD == 0) {
				for (LivingEntity foe : field.foes(owner)) MethodsBKit.toxin(owner, foe, TOXIN_TICKS);
			}
		});
		// ...and you slither back out of reach.
		Vec3 back = look.scale(-1);
		List<Vec3> path = ArtKit.path(player, back, SHED_BACK);
		if (path.size() >= 2) {
			ArtKit.dash(player, path, 4, (a, b, step, last) -> ArtLight.world(player).ray(a.add(0, 0.1, 0), b.add(0, 0.1, 0), color, 0.15, 10));
		}
		return true;
	}

	// ------------------------------------------------------------------ IV. Serpent Slither

	static boolean serpentSlither(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = acid(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, SLITHER_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, SERPENT_SLITHER);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.LOW, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		AuraStep.afterimages(player, path.getFirst(), path.getLast(), dir, color);
		Feels.sound(level, path.getFirst().add(0, 1, 0), "aura_art_serpent_slither", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		Vec3 right = ArtKit.right(dir);
		ArtKit.dash(player, path, SLITHER_TICKS, (a, b, step, last) -> {
			// The trail weaves side to side, the way a serpent goes.
			double sway = Math.sin(step * 1.6) * SLITHER_WEAVE;
			world.ray(a.add(right.scale(-sway)).add(0, 0.15, 0), b.add(right.scale(sway)).add(0, 0.15, 0), color, 0.2, 14);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, SLITHER_WIDTH / 2, 2.2, SLITHER_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= SLITHER_TARGETS) continue;
				bite(hits, foe, SLITHER_FACTOR, AuraFxRules.Weight.FULL);
				MethodsBKit.weaken(player, foe, VENOM_WEAKNESS);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Hydra Coil

	static boolean hydraCoil(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = acid(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_hydra_coil", 1.4F, 1.0F);
		ArtLight.world(player).groundRing(feet.add(0, 0.05, 0), MethodsBKit.BLACK, HYDRA_RADIUS * 0.85, HYDRA_RADIUS, 0.1, HYDRA_HEADS * HYDRA_PERIOD + 10);
		List<LivingEntity> foes = new ArrayList<>(ArtKit.around(player, feet, HYDRA_RADIUS, 1.5, 3.0, HYDRA_TARGETS));
		double share = HYDRA_FACTOR / HYDRA_HEADS;
		for (int h = 0; h < HYDRA_HEADS; h++) {
			int head = h;
			Scheduler.later(HYDRA_PERIOD * h, () -> {
				if (!player.isAlive() || player.level() != level) return;
				double a = Math.PI * 2 * head / HYDRA_HEADS;
				Vec3 rise = feet.add(Math.cos(a) * 1.2, 0, Math.sin(a) * 1.2);
				ArtLight world = ArtLight.world(player);
				for (LivingEntity foe : foes) {
					if (!foe.isAlive()) continue;
					Vec3 at = foe.getBoundingBox().getCenter();
					world.arc(rise.add(0, 2.4, 0), at, head % 2 == 0 ? color : MethodsBKit.BLACK, 0.14, 0, false, 8);
					bite(hits, foe, share, head == HYDRA_HEADS - 1 ? AuraFxRules.Weight.GRAND : AuraFxRules.Weight.LIGHT);
					if (head == HYDRA_HEADS - 1) MethodsBKit.weaken(player, foe, VENOM_WEAKNESS);
				}
				Feels.sound(level, rise.add(0, 1, 0), "aura_venom_impact", 0.8F, 0.9F + 0.08F * head);
			});
		}
		return true;
	}
}
