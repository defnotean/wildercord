package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.IronRules;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Iron Breath's arts: metal and the forge. Iron is slow and heavy: its blows sunder armour for a while (every later blow bites
 * deeper), and its guard holds where others give.
 * <ul>
 * <li><b>Sunder Cut</b> (I): one heavy cut through the plates: armour cracked off for a while.</li>
 * <li><b>Anvil Fall</b> (II): a leap and a smash where you land; the foes round it held a moment and sundered.</li>
 * <li><b>Bulwark</b> (III): the heavy guard: hard to hurt, unmoved, foes close shoved off you.</li>
 * <li><b>Forge Charge</b> (IV): shoulder first through the line, sundering what you hit.</li>
 * <li><b>Worldforge</b> (V): the hammer of the world: a great smash, a ring of sparks, armour broken wide, and the guard after.</li>
 * </ul>
 */
public final class IronArts {
	private IronArts() {}

	public static final String METHOD = "iron";
	public static final String SUNDER_CUT = "sunder_cut";
	public static final String ANVIL_FALL = "anvil_fall";
	public static final String BULWARK = "bulwark";
	public static final String FORGE_CHARGE = "forge_charge";
	public static final String WORLDFORGE = "worldforge";

	public static final String GUARD = "iron_guard";

	private static final int SPARK = MethodsAFlavours.SPARK;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, SUNDER_CUT, IronArts::sunderCut),
			MethodArts.art(AuraApi.ArtSlot.SECOND, ANVIL_FALL, IronArts::anvilFall),
			MethodArts.art(AuraApi.ArtSlot.THIRD, BULWARK, IronArts::bulwark),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, FORGE_CHARGE, IronArts::forgeCharge),
			MethodArts.art(AuraApi.ArtSlot.FINAL, WORLDFORGE, IronArts::worldforge));
	}

	private static void sparks(ServerLevel level, Vec3 at, int count) {
		Vfx.emit(level, ParticleTypes.CRIT, at, count, 0.35, 0.3);
		Vfx.emit(level, ParticleTypes.SMALL_FLAME, at, Math.max(1, count / 4), 0.25, 0.05);
	}

	// ------------------------------------------------------------------ I. Sunder Cut

	static boolean sunderCut(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_sunder_cut", 1.1F, 0.9F);
		ArtLight world = ArtLight.world(player);
		Vec3 dir = ArtKit.flat(player);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), IronRules.SUNDER_CUT_REACH, IronRules.SUNDER_CUT_DEGREES, IronRules.SUNDER_CUT_TARGETS)) {
			hits.strike(foe, IronRules.SUNDER_CUT_FACTOR, AuraFxRules.Weight.HEAVY);
			MethodsAFlavours.sunder(player, foe, IronRules.SUNDER_CUT_ARMOUR, IronRules.SUNDER_CUT_TICKS);
			Vec3 c = foe.getBoundingBox().getCenter();
			world.slash(c, ArtKit.right(dir), ArtKit.UP.scale(-1), color, 1.0, 2.0, 0.24, 1, 7);
			world.bare().slash(c.add(dir.scale(-0.05)), ArtKit.right(dir), ArtKit.UP.scale(-1), SPARK, 0.95, 1.6, 0.08, 1, 6);
			sparks(level, c, 14);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Anvil Fall

	static boolean anvilFall(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_anvil_fall", 1.1F, 1.0F);
		LivingEntity aim = ArtKit.primary(player, context, IronRules.ANVIL_REACH, 70);
		Vec3 toward = aim == null ? ArtKit.flat(player).scale(IronRules.ANVIL_REACH * 0.6) : aim.position().subtract(player.position());
		Vec3 flat = new Vec3(toward.x, 0, toward.z);
		double reach = Math.min(IronRules.ANVIL_REACH, flat.length());
		Vec3 hop = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(reach * 0.16);
		ArtKit.launch(player, new Vec3(hop.x, 0.62, hop.z));
		MethodArts.whenLanded(player, 40, at -> {
			ArtLight world = ArtLight.world(player);
			world.groundRing(at, color, 0.3, IronRules.ANVIL_RADIUS * 1.3, 0.2, 10);
			world.groundRing(at, SPARK, 0.2, IronRules.ANVIL_RADIUS, 0.08, 9);
			world.ground(at.add(0, 0.03, 0), SigilOption.CRACKED, color | ArtLight.DARK, IronRules.ANVIL_RADIUS, 40, 0);
			sparks(level, at.add(0, 0.3, 0), 24);
			AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.7F);
			for (LivingEntity foe : ArtKit.around(player, at, IronRules.ANVIL_RADIUS, 1.0, 2.5, IronRules.ANVIL_TARGETS)) {
				hits.strike(foe, IronRules.ANVIL_FACTOR, AuraFxRules.Weight.HEAVY);
				ArtKit.hold(player, foe, IronRules.ANVIL_HOLD);
				MethodsAFlavours.sunder(player, foe, IronRules.ANVIL_ARMOUR, IronRules.ANVIL_SUNDER_TICKS);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ III. Bulwark

	static boolean bulwark(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CROSS, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_bulwark", 1.1F, 0.9F);
		MethodsAFlavours.bulwark(player, IronRules.BULWARK_TICKS, IronRules.BULWARK_RESIST);
		for (LivingEntity foe : ArtKit.around(player, feet, IronRules.BULWARK_RADIUS, 1.0, 2.5, IronRules.BULWARK_TARGETS)) {
			hits.strike(foe, IronRules.BULWARK_FACTOR, AuraFxRules.Weight.FULL);
			ArtKit.knock(foe, feet, IronRules.BULWARK_SHOVE, 0.2);
		}
		ArtLight.world(player).ground(feet.add(0, 0.03, 0), SigilOption.TARGET, color, IronRules.BULWARK_RADIUS, IronRules.BULWARK_TICKS, 0);
		ArtFields.open(player, GUARD, ArtFields.disc(player::position, IronRules.BULWARK_RADIUS, 2.5), IronRules.BULWARK_TICKS, 4, (field, owner, age) -> {
			Vec3 at = owner.position();
			ArtLight show = ArtLight.spectacle(owner);
			if (age % 8 == 0) {
				show.ring(at.add(0, 1.0, 0), ArtKit.UP, SPARK, 0.9, 1.0, 0.05, 6);
				sparks(field.level(), at.add(0, 1.0, 0), 3);
			}
			if (age % 20 == 0) {
				// A foe that presses close is shoved off the guard.
				for (LivingEntity foe : field.foes(owner)) {
					ArtKit.knock(foe, at, IronRules.BULWARK_SHOVE * 0.6, 0.1);
				}
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ IV. Forge Charge

	static boolean forgeCharge(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, IronRules.CHARGE_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, FORGE_CHARGE);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		AuraStep.afterimages(player, from, path.getLast(), dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_forge_charge", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		MethodsAFlavours.bulwark(player, IronRules.CHARGE_TICKS + 6, 0);
		ArtKit.dash(player, path, IronRules.CHARGE_TICKS, (a, b, step, last) -> {
			world.ray(a.add(0, 1.0, 0), b.add(0, 1.0, 0), color, 0.4, 9);
			world.bare().ray(a.add(0, 0.2, 0), b.add(0, 0.2, 0), SPARK, 0.1, 12);
			sparks(level, b.add(0, 0.2, 0), 4);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, IronRules.CHARGE_WIDTH / 2 + 0.3, 2.2,
					IronRules.CHARGE_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= IronRules.CHARGE_TARGETS) {
					continue;
				}
				hits.strike(foe, IronRules.CHARGE_FACTOR, AuraFxRules.Weight.HEAVY);
				MethodsAFlavours.sunder(player, foe, IronRules.CHARGE_ARMOUR, 80);
				ArtKit.knock(foe, a, 0.7, 0.2);
			}
			if (last) {
				world.groundRing(b, SPARK, 0.4, 2.2, 0.12, 9);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Worldforge

	static boolean worldforge(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.8F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_worldforge", 1.3F, 0.9F);
		ArtLight show = ArtLight.spectacle(player);
		show.bare().ray(feet.add(0, 8, 0), feet, SPARK, 0.6, 10);
		// The hammer falls a breath later: the main foe takes the blow, the ring of sparks takes the rest.
		LivingEntity main = ArtKit.primary(player, context, IronRules.WORLDFORGE_RADIUS, 90);
		Scheduler.later(4, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			Vec3 at = player.position();
			ArtLight world = ArtLight.world(player);
			world.groundRing(at, color, 0.5, IronRules.WORLDFORGE_RADIUS * 1.4, 0.3, 14);
			world.groundRing(at, SPARK, 0.4, IronRules.WORLDFORGE_RADIUS * 1.1, 0.1, 12);
			world.ground(at.add(0, 0.03, 0), SigilOption.CRACKED, color | ArtLight.DARK, IronRules.WORLDFORGE_RADIUS, 60, 0);
			Vfx.emit(level, ParticleTypes.LAVA, at.add(0, 0.5, 0), 16, IronRules.WORLDFORGE_RADIUS * 0.4, 0.3);
			sparks(level, at.add(0, 0.5, 0), 40);
			AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.6F);
			for (LivingEntity foe : ArtKit.around(player, at, IronRules.WORLDFORGE_RADIUS, 1.5, 3.0, IronRules.WORLDFORGE_TARGETS)) {
				boolean first = foe == main || main == null && hits.count() == 0;
				hits.strike(foe, first ? IronRules.WORLDFORGE_FACTOR : IronRules.WORLDFORGE_RING, AuraFxRules.Weight.GRAND);
				MethodsAFlavours.sunder(player, foe, IronRules.WORLDFORGE_ARMOUR, IronRules.WORLDFORGE_TICKS);
				ArtKit.knock(foe, at, IronRules.WORLDFORGE_THROW, 0.35);
			}
			MethodsAFlavours.bulwark(player, IronRules.BULWARK_TICKS, 0);
		});
		return true;
	}
}
