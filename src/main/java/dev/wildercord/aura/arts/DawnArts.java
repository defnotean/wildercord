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

import java.util.List;

import static dev.wildercord.aura.MethodsBArtRules.*;

/**
 * Dawn Breath's arts: light. Its blows light their foes up (seen through walls), its brighter arts blind a moment (a player's sight,
 * a mob's hold on its target), the undead take its light hardest, and its halo mends. Its light is white gold, rose at the edges.
 * <ul>
 * <li><b>First Light</b> (I): a bright cut, its foes lit up.</li>
 * <li><b>Sunrise Arc</b> (II): a rising arc ahead, its foes blinded a moment and lit.</li>
 * <li><b>Halo Guard</b> (III): a halo round you: you mend, the foes near blinded and thrown back.</li>
 * <li><b>Dawnbreak Rush</b> (IV): a dash down a line of light, the foes in the way cut and lit.</li>
 * <li><b>Noon Zenith</b> (V): the sun overhead, a pillar of light, then its noon falling on every foe near.</li>
 * </ul>
 */
public final class DawnArts {
	private DawnArts() {}

	public static final String METHOD = DAWN;
	public static final String FIRST_LIGHT = "first_light";
	public static final String SUNRISE_ARC = "sunrise_arc";
	public static final String HALO_GUARD = "halo_guard";
	public static final String DAWNBREAK_RUSH = "dawnbreak_rush";
	public static final String NOON_ZENITH = "noon_zenith";

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, FIRST_LIGHT, DawnArts::firstLight),
			MethodArts.art(AuraApi.ArtSlot.SECOND, SUNRISE_ARC, DawnArts::sunriseArc),
			MethodArts.art(AuraApi.ArtSlot.THIRD, HALO_GUARD, DawnArts::haloGuard),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, DAWNBREAK_RUSH, DawnArts::dawnbreakRush),
			MethodArts.art(AuraApi.ArtSlot.FINAL, NOON_ZENITH, DawnArts::noonZenith));
	}

	/** The gold the arts are drawn in: the aura's own colour taken toward white gold. */
	static int gold(ServerPlayer player) {
		return ArtKit.mix(ArtKit.color(player), MethodsBKit.GOLD, 0.6);
	}

	/** One foe struck by the light: harder on the undead (who smoke under it), and lit up. */
	static float light(ArtKit.Hits hits, LivingEntity foe, double factor, AuraFxRules.Weight weight, int glow) {
		ServerPlayer player = hits.player();
		boolean undead = MethodsBKit.undead(foe);
		float taken = hits.strike(foe, dawnFactor(factor, undead), weight);
		MethodsBKit.glow(player, foe, glow);
		Vec3 c = foe.getBoundingBox().getCenter();
		Motes.glows(player.level(), c, undead ? 8 : 4, 0.35, undead ? MethodsBKit.ROSE : MethodsBKit.GOLD, 0.09, 16, new Vec3(0, 0.03, 0), 0.03);
		if (undead) Vfx.emit(player.level(), ParticleTypes.WHITE_SMOKE, c, 6, 0.3, 0.02);
		return taken;
	}

	// ------------------------------------------------------------------ I. First Light

	static boolean firstLight(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_first_light", 1.0F, 1.0F);
		ArtLight.world(player).slash(feet.add(0, 0.9, 0).add(look.scale(1.1)), ArtKit.UP, look, color, LIGHT_REACH * 0.8, 2.0, 0.3, 1, 9);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), LIGHT_REACH, LIGHT_DEGREES, LIGHT_TARGETS)) {
			light(hits, foe, LIGHT_FACTOR, AuraFxRules.Weight.HEAVY, LIGHT_GLOW);
			ArtLight.world(player).flash(foe.getBoundingBox().getCenter(), MethodsBKit.GOLD, 0.8F);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Sunrise Arc

	static boolean sunriseArc(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_sunrise_arc", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		// A sun's rim rising out of the ground ahead.
		Vec3 rim = feet.add(look.scale(SUNRISE_REACH * 0.55));
		world.ring(rim.add(0, 0.2, 0), look, color, SUNRISE_REACH * 0.45, SUNRISE_REACH * 0.55, 0.12, 12);
		world.ring(rim.add(0, 0.2, 0), look, MethodsBKit.ROSE, SUNRISE_REACH * 0.55, SUNRISE_REACH * 0.62, 0.05, 12);
		for (LivingEntity foe : ArtKit.arcFrom(player, feet, look, SUNRISE_REACH, SUNRISE_DEGREES, SUNRISE_TARGETS)) {
			light(hits, foe, SUNRISE_FACTOR, AuraFxRules.Weight.FULL, LIGHT_GLOW);
			MethodsBKit.blind(player, foe, SUNRISE_BLIND);
			ArtKit.lift(foe, 0.25, 6);
		}
		return true;
	}

	// ------------------------------------------------------------------ III. Halo Guard

	static boolean haloGuard(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.3F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_halo_guard", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		world.ring(feet.add(0, 2.3, 0), ArtKit.UP, color, 0.45, 0.6, 0.08, 30);
		world.groundRing(feet.add(0, 0.05, 0), MethodsBKit.ROSE, HALO_RADIUS * 0.3, HALO_RADIUS, 0.08, 14);
		AuraFx.burst(level, player, feet.add(0, 1, 0), ArtKit.flat(player), color, 1.2F, AuraFx.Burst.RING | AuraFx.Burst.FLASH);
		float mended = ArtKit.mend(player, player, HALO_MEND);
		if (mended > 0) Motes.glows(level, feet.add(0, 1, 0), 8, 0.5, MethodsBKit.ROSE, 0.08, 20, new Vec3(0, 0.04, 0), 0.03);
		for (LivingEntity foe : ArtKit.around(player, feet, HALO_RADIUS, 1.0, 2.5, 8)) {
			light(hits, foe, HALO_FACTOR, AuraFxRules.Weight.FULL, LIGHT_GLOW);
			MethodsBKit.blind(player, foe, HALO_BLIND);
			ArtKit.knock(foe, feet, HALO_THROW, 0.2);
		}
		return true;
	}

	// ------------------------------------------------------------------ IV. Dawnbreak Rush

	static boolean dawnbreakRush(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, DAWNBREAK_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, DAWNBREAK_RUSH);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		AuraStep.afterimages(player, path.getFirst(), path.getLast(), dir, color);
		Feels.sound(level, path.getFirst().add(0, 1, 0), "aura_art_dawnbreak_rush", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, DAWNBREAK_TICKS, (a, b, step, last) -> {
			world.ray(a.add(0, 1.0, 0), b.add(0, 1.0, 0), color, 0.3, 14);
			world.ray(a.add(0, 0.08, 0), b.add(0, 0.08, 0), MethodsBKit.ROSE, 0.18, 18);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, DAWNBREAK_WIDTH / 2, 2.2, DAWNBREAK_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= DAWNBREAK_TARGETS) continue;
				light(hits, foe, DAWNBREAK_FACTOR, AuraFxRules.Weight.FULL, LIGHT_GLOW);
			}
			if (last) world.flash(b.add(0, 1, 0), MethodsBKit.GOLD, 1.2F);
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Noon Zenith

	static boolean noonZenith(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_noon_zenith", 1.4F, 1.0F);
		ArtLight sky = ArtLight.spectacle(player);
		// The sun climbs overhead...
		sky.orb(feet.add(0, 6, 0), color, 1.2, ZENITH_DELAY + 10);
		sky.ray(feet.add(0, 0.1, 0), feet.add(0, 6, 0), MethodsBKit.GOLD, 0.6, ZENITH_DELAY + 6);
		ArtLight.world(player).groundRing(feet.add(0, 0.05, 0), MethodsBKit.ROSE, ZENITH_RADIUS * 0.9, ZENITH_RADIUS, 0.08, ZENITH_DELAY + 6);
		// ...and at noon its light falls on everything round you.
		Scheduler.later(ZENITH_DELAY, () -> {
			if (!player.isAlive() || player.level() != level) return;
			sky.flash(feet.add(0, 2, 0), MethodsBKit.GOLD, 3.0F);
			ArtLight.world(player).groundRing(feet.add(0, 0.05, 0), color, 0.5, ZENITH_RADIUS, 0.12, 20);
			Feels.sound(level, feet.add(0, 1, 0), "aura_dawn_impact", 1.3F, 0.8F);
			for (LivingEntity foe : ArtKit.around(player, feet, ZENITH_RADIUS, 1.5, 4.0, ZENITH_TARGETS)) {
				sky.ray(foe.position().add(0, 6, 0), foe.position(), MethodsBKit.GOLD, 0.35, 10);
				light(hits, foe, ZENITH_FACTOR, AuraFxRules.Weight.GRAND, LIGHT_GLOW * 2);
				MethodsBKit.blind(player, foe, ZENITH_BLIND);
			}
		});
		return true;
	}
}
