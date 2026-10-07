package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stone Breath's arts: weight, footing taken, and standing firm. Stone hits hardest of all on one foe, takes the ground from under
 * it (shaken, cracked, carried, thrown up on rising stone), and stands unmoved itself. The stone that rises is only ever a shape
 * ({@link ArtBlocks}): the world's own ground is never broken.
 * <ul>
 * <li><b>Rockbreaker</b> (I): a heavy cut that shakes its foe's footing and cracks it (every spell bites it harder a while), the
 * shock spilling onto those beside it.</li>
 * <li><b>Avalanche</b> (II): a slam where you come down, and a shockwave rolling out over the ground.</li>
 * <li><b>Unmoved</b> (III): whoever struck hurled back by their own blow, and you hardened like stone a while.</li>
 * <li><b>Landslide</b> (IV): a heavy charge that carries the foes in front along and throws them at its end; one driven into a wall
 * is crushed against it.</li>
 * <li><b>Mountain Splitter</b> (V): an overhead strike that splits the ground in a line ahead, stone rising along it in turn and
 * throwing up whatever stands there.</li>
 * </ul>
 */
public final class StoneArts {
	private StoneArts() {}

	public static final String METHOD = "stone";
	public static final String ROCKBREAKER = "rockbreaker";
	public static final String AVALANCHE = "avalanche";
	public static final String UNMOVED = "unmoved";
	public static final String LANDSLIDE = "landslide";
	public static final String MOUNTAIN_SPLITTER = "mountain_splitter";

	/** The earth palette's sand and dark earth (cast.ElementFx.EARTH). */
	private static final int SAND = 0xE8C890;
	private static final int DARK_EARTH = 0x6E5436;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, ROCKBREAKER, StoneArts::rockbreaker),
			MethodArts.art(AuraApi.ArtSlot.SECOND, AVALANCHE, StoneArts::avalanche),
			MethodArts.art(AuraApi.ArtSlot.THIRD, UNMOVED, StoneArts::unmoved),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, LANDSLIDE, StoneArts::landslide),
			MethodArts.art(AuraApi.ArtSlot.FINAL, MOUNTAIN_SPLITTER, StoneArts::mountainSplitter));
	}

	// ------------------------------------------------------------------ I. Rockbreaker

	static boolean rockbreaker(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		LivingEntity foe = ArtKit.primary(player, context, ArtRules.ROCK_REACH, 110);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_rockbreaker", 1.1F, 1.0F);
		Vec3 where = foe != null ? foe.position() : player.position().add(ArtKit.flat(player).scale(2.0));
		Vec3 ground = ArtKit.floor(level, where.add(0, 0.5, 0), 1.0, 3);
		Vec3 base = ground == null ? where : ground;
		BlockState earth = ArtBlocks.ground(level, base);
		// The ground under it cracking, slabs of it heaved up round its feet, chips flung, dust.
		AuraPhysicalFx.crack(level, base, 1.7, 30);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 3; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			ArtBlocks.slab(level, base.add(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9), earth, (float) a, 0.6F, 0.5F, 1 + i, 10);
		}
		ElementFx.stoneShards(level, base.add(0, 0.3, 0), earth, 8, 0.3);
		ArtLight.world(player).groundRing(base, color, 0.3, 2.2, 0.12, 9);
		ScreenFx.shake(level, base, 0.15F, 8);
		if (foe == null) {
			return true;
		}
		hits.strike(foe, ArtRules.ROCK_FACTOR);
		if (foe.isAlive()) {
			// Its footing shaken: a moment it can't act, then heavy-footed a while; cracked, so every spell bites it harder.
			ArtKit.hold(player, foe, ArtRules.ROCK_HOLD);
			ArtKit.slow(player, foe, ArtRules.ROCK_SLOW, 2);
			Reactions.mark(foe, Reactions.Mark.CRACKED, ArtRules.ROCK_CRACKED);
			AuraPhysicalFx.earthImpact(level, foe.getBoundingBox().getCenter(), 1.0);
		}
		for (LivingEntity other : ArtKit.around(player, foe.position(), ArtRules.ROCK_SHOCK_RADIUS, 1.0, 2.5, 5)) {
			if (other != foe) {
				hits.strike(other, ArtRules.ROCK_SHOCK_FACTOR, AuraFxRules.Weight.LIGHT);
				ArtKit.slow(player, other, 20, 0);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Avalanche

	static boolean avalanche(ServerPlayer player, AuraApi.StringContext context) {
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		if (!player.onGround()) {
			// Still in the air: driven down, and the slam where you land.
			ArtKit.launch(player, new Vec3(0, -1.3, 0));
			MethodArts.whenLanded(player, 20, at -> slam(player, hits, at));
		} else {
			slam(player, hits, player.position());
		}
		return true;
	}

	private static void slam(ServerPlayer player, ArtKit.Hits hits, Vec3 at) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 ground = ArtKit.floor(level, at.add(0, 0.5, 0), 1.0, 3);
		Vec3 base = ground == null ? at : ground;
		BlockState earth = ArtBlocks.ground(level, base);
		Feels.sound(level, base.add(0, 1, 0), "aura_art_avalanche", 1.2F, 1.0F);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 0.9F, 0.8F);
		crackUnder(player, base, 2.2, 36, earth);
		ScreenFx.shake(level, base, 0.3F, 12);
		ArtLight world = ArtLight.world(player);
		Set<UUID> struck = new HashSet<>();
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		// The shockwave rolling out a stretch a tick, heaving the ground as it passes, striking each foe as it reaches it.
		for (int t = 0; t < ArtRules.AVALANCHE_ROLL; t++) {
			int tick = t;
			Scheduler.later(1 + t, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				double outer = ArtRules.AVALANCHE_RADIUS * (tick + 1) / ArtRules.AVALANCHE_ROLL;
				double inner = ArtRules.AVALANCHE_RADIUS * tick / ArtRules.AVALANCHE_ROLL;
				world.groundRing(base, color, inner, outer + 0.2, 0.26, 7);
				world.groundRing(base, SAND, inner, outer, 0.08, 6);
				int around = 6 + tick * 2;
				for (int i = 0; i < around; i++) {
					double a = phase + Math.PI * 2 * i / around;
					Vec3 p = base.add(Math.cos(a) * outer, 0.1, Math.sin(a) * outer);
					Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, earth), p, 2, 0.15, 0.12);
				}
				if (tick >= 1 && tick <= 4) {
					for (int i = 0; i < 2; i++) {
						double a = phase + tick * 1.7 + Math.PI * i;
						ArtBlocks.slab(level, base.add(Math.cos(a) * outer, 0, Math.sin(a) * outer), earth, (float) a, 0.7F, 0.45F, 1, 12);
					}
				}
				for (LivingEntity foe : ArtKit.around(player, base, outer, 1.5, 3.0, ArtRules.AVALANCHE_TARGETS)) {
					if (!struck.add(foe.getUUID())) {
						continue;
					}
					double d = foe.position().subtract(base).horizontalDistance();
					hits.strike(foe, ArtRules.falloff(ArtRules.AVALANCHE_CENTRE, ArtRules.AVALANCHE_EDGE, d, ArtRules.AVALANCHE_RADIUS));
					ArtKit.knock(foe, base, ArtRules.AVALANCHE_THROW, 0.32);
					ArtKit.slow(player, foe, ArtRules.AVALANCHE_SLOW, 1);
				}
			});
		}
	}

	/**
	 * The ground cracking under the swordsman's own feet: the cracked seal, a ring of dust and a puff of it, as
	 * {@code ElementFx.crack}, but its chips thrown low and outward from the rim (crack's fly straight up from all over the seal,
	 * through the swordsman's own eyes).
	 */
	static void crackUnder(ServerPlayer player, Vec3 feet, double radius, int lifetime, BlockState earth) {
		ServerLevel level = player.level();
		ArtLight world = ArtLight.world(player);
		world.ground(feet, SigilOption.CRACKED, ElementFx.EARTH.secondary(), radius, lifetime, 0.0);
		world.groundRing(feet, ElementFx.EARTH.primary(), 0.3, radius * 1.15, 0.09, 10);
		BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, earth);
		RandomSource r = level.getRandom();
		int chips = (int) Math.max(4, Math.min(12, radius * 4));
		for (int i = 0; i < chips; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double out = radius * (0.7 + 0.3 * r.nextDouble());
			Vfx.fling(level, chip, feet.add(Math.cos(a) * out, 0.1, Math.sin(a) * out), new Vec3(Math.cos(a), 0.35, Math.sin(a)),
				0.16 + r.nextDouble() * 0.1);
		}
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, earth), feet.add(0, 0.1, 0), (int) Math.max(3, Math.min(14, radius * 3)),
			radius * 0.4, 0.08);
	}

	// ------------------------------------------------------------------ III. Unmoved

	static boolean unmoved(ServerPlayer player, AuraApi.StringContext context) {
		var counter = dev.wildercord.aura.MastersArts.earnedCounter(player);
		if (counter == null || !counter.art().equals(UNMOVED) || !counter.valid()) return false;
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		LivingEntity foe = counter.target();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_unmoved", 1.2F, 1.0F);
		BlockState earth = ArtBlocks.ground(level, feet);
		// Stone gathering round the swordsman's feet as they harden, the ground cracking under them.
		crackUnder(player, feet, 1.4, 30, earth);
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			ArtBlocks.slab(level, feet.add(Math.cos(a) * 0.95, 0, Math.sin(a) * 0.95), earth, (float) (a + Math.PI / 2), 0.45F, -0.35F, 1 + i % 2, 14);
		}
		ArtLight.world(player).groundRing(feet, color, 1.6, 0.4, 0.14, 10);
		AuraFx.burst(level, player, feet.add(0, 1.0, 0), Vec3.ZERO, color, 2.2F, AuraFx.Burst.RING | AuraFx.Burst.FLASH);
		if (foe != null && counter.primaryValid()) {
			hits.strike(foe, ArtRules.UNMOVED_FACTOR);
			if (!counter.afterDamage(foe)) return true;
			var landing = counter.landingPermission(foe);
			ArtKit.knock(foe, feet, ArtRules.UNMOVED_THROW, 0.4);
			if (!landing.getAsBoolean()) return true;
			// Stunned once it comes down (held in the air it would hang there).
			ArtKit.holdLater(player, foe, 10, 20, landing);
			Vec3 at = foe.position();
			AuraPhysicalFx.earthImpact(level, foe.getBoundingBox().getCenter(), 1.1);
			ArtLight.world(player).ground(at, SigilOption.CRACKED, SAND, 1.4, 30, 0);
			Vec3 between = feet.add(at).scale(0.5);
			ArtLight.world(player).ray(feet.add(0, 0.1, 0), at.add(0, 0.1, 0), color, 0.35, 12);
			ElementFx.stoneShards(level, between.add(0, 0.3, 0), earth, 6, 0.35);
		}
		return true;
	}

	/** A hardened swordsman, while it lasts: stone dust sifting off them and a faint ring over the ground (asked by {@link ArtWards}). */
	static void hardenedLook(ServerPlayer player) {
		ServerLevel level = player.level();
		RandomSource r = level.getRandom();
		BlockState earth = ArtBlocks.ground(level, player.position());
		for (int i = 0; i < 3; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, earth),
				player.position().add(Math.cos(a) * 0.45, 0.3 + r.nextDouble() * 1.4, Math.sin(a) * 0.45), 1, 0.05, 0.0);
		}
		ArtLight.world(player).groundRing(player.position(), ArtKit.color(player), 0.9, 0.6, 0.04, 9);
	}

	// ------------------------------------------------------------------ IV. Landslide

	static boolean landslide(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.LANDSLIDE_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, LANDSLIDE);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		AuraStep.afterimages(player, from, path.getLast(), dir, color);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_landslide", 1.2F, 1.0F);
		BlockState earth = ArtBlocks.ground(level, from);
		List<LivingEntity> carried = new ArrayList<>();
		Set<UUID> crushed = new HashSet<>();
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, ArtRules.LANDSLIDE_TICKS, (a, b, step, last) -> {
			// The ground heaving and cracking under the charge, dust thrown up behind.
			world.ray(a.add(0, 0.12, 0), b.add(0, 0.12, 0), color, 0.4, 12);
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, earth), a.add(0, 0.2, 0), 5, 0.4, 0.12);
			Motes.clouds(level, a.add(0, 0.4, 0), 2, 0.4, 0xB8A888, 0.7, 22, new Vec3(0, 0.02, 0), 0.01, 0.3);
			if (step % 2 == 0) {
				double yaw = Math.atan2(dir.x, dir.z);
				Vec3 right = ArtKit.right(dir);
				ArtBlocks.slab(level, a.add(right.scale(0.7)), earth, (float) yaw, 0.55F, 0.6F, 1, 8);
				ArtBlocks.slab(level, a.add(right.scale(-0.7)), earth, (float) yaw, 0.55F, -0.6F, 1, 8);
			}
			// Whoever stands in front is caught up and carried along.
			for (LivingEntity foe : ArtKit.line(player, b, dir, 1.6, ArtRules.LANDSLIDE_WIDTH / 2, 2.0, ArtRules.LANDSLIDE_TARGETS)) {
				if (carried.contains(foe) || crushed.contains(foe.getUUID())) {
					continue;
				}
				if (ArtKit.boss(foe)) {
					if (!hits.hurt(foe)) {
						hits.strike(foe, ArtRules.LANDSLIDE_FACTOR);
					}
					continue;
				}
				if (carried.size() < ArtRules.LANDSLIDE_TARGETS) {
					carried.add(foe);
					Feels.sound(level, foe.position(), "earth_stomp", 0.6F, 1.0F);
				}
			}
			for (int i = 0; i < carried.size(); i++) {
				LivingEntity foe = carried.get(i);
				if (!foe.isAlive()) {
					continue;
				}
				Vec3 want = b.add(dir.scale(1.1 + foe.getBbWidth() / 2 + 0.15 * i));
				want = new Vec3(want.x, foe.getY(), want.z);
				AABB body = foe.getBoundingBox().move(want.subtract(foe.position()));
				if (!level.noCollision(foe, body)) {
					// Driven into a wall: crushed against it.
					crushed.add(foe.getUUID());
					hits.strike(foe, ArtRules.LANDSLIDE_FACTOR * ArtRules.LANDSLIDE_WALL, AuraFxRules.Weight.GRAND);
					ArtKit.hold(player, foe, ArtRules.LANDSLIDE_WALL_HOLD);
					AuraPhysicalFx.earthImpact(level, foe.getBoundingBox().getCenter(), 1.3);
					AuraPhysicalFx.crack(level, foe.position(), 1.2, 24);
					ScreenFx.shake(level, foe.position(), 0.25F, 10);
					Feels.sound(level, foe.position(), "earth_slam", 1.0F, 0.9F);
					carried.remove(i--);
					continue;
				}
				if (foe instanceof Player) {
					// Another player is shoved, never dragged.
					ArtKit.shove(foe, dir.scale(0.6).add(0, 0.1, 0));
				} else {
					foe.teleportTo(want.x, want.y, want.z);
					foe.setDeltaMovement(dir.scale(0.3));
					foe.needsSync = true;
				}
			}
			if (last) {
				// The end: each one carried is thrown on, stunned a moment.
				// (Ahead of the swordsman and low, so the burst and its chips stay out of their own eyes.)
				AuraPhysicalFx.earthImpact(level, b.add(dir.scale(2.0)).add(0, 0.5, 0), 1.2);
				crackUnder(player, b.add(dir.scale(1.2)), 1.8, 30, earth);
				ScreenFx.shake(level, b, 0.25F, 10);
				Feels.sound(level, b, "earth_slam", 1.0F, 1.0F);
				for (LivingEntity foe : carried) {
					if (!foe.isAlive()) {
						continue;
					}
					hits.strike(foe, ArtRules.LANDSLIDE_FACTOR);
					ArtKit.shove(foe, dir.scale(ArtRules.LANDSLIDE_THROW).add(0, 0.35, 0));
					ArtKit.holdLater(player, foe, 8, ArtRules.LANDSLIDE_HOLD);
				}
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Mountain Splitter

	static boolean mountainSplitter(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.8F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_mountain_splitter", 1.3F, 1.0F);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.7F);
		ScreenFx.shake(level, feet, 0.35F, 16);
		Vec3 start = feet.add(dir.scale(1.5));
		List<Vec3> line = EmberArts.groundLine(level, start, dir, ArtRules.SPLITTER_LENGTH - 1.5, ArtRules.SPLITTER_SPACING);
		ArtLight world = ArtLight.world(player);
		crackUnder(player, feet.add(dir.scale(1.2)), 1.2, 40, ArtBlocks.ground(level, feet));
		Set<UUID> struck = new HashSet<>();
		double yaw = Math.atan2(dir.x, dir.z);
		for (int i = 0; i < line.size(); i++) {
			Vec3 p = line.get(i);
			Vec3 prev = i == 0 ? feet.add(dir.scale(0.8)) : line.get(i - 1);
			int index = i;
			int delay = 1 + (int) Math.round(i * ArtRules.SPLITTER_SPACING / ArtRules.SPLITTER_SPEED);
			Scheduler.later(delay, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				BlockState earth = ArtBlocks.ground(level, p);
				// The split running on: a crack in the ground, a seam of the aura's light along it, and stone bursting up.
				if (index > 0) {
					// The seam of light opens from where the stone begins, ahead, never up from under your own feet.
					world.ray(prev.add(0, 0.06, 0), p.add(0, 0.06, 0), color, 0.48, 30);
					world.bare().ray(prev.add(0, 0.08, 0), p.add(0, 0.08, 0), SAND, 0.12, 26);
				}
				if (index < 2) {
					// Close in front: the chips thrown low and outward, not up through your own view.
					crackUnder(player, p, 1.0, 26, earth);
				} else {
					AuraPhysicalFx.crack(level, p, 1.0, 26);
				}
				RandomSource r = level.getRandom();
				float lean = (float) ((r.nextDouble() - 0.5) * 0.5);
				if (index >= 1) {
					// Stone only from three blocks out, low at first, so none ever stands in your own view; taller as it runs on.
					float height = index < 3 ? 0.3F + 0.3F * index : 1.5F + 0.3F * Math.min(4, index - 3);
					ArtBlocks.spire(level, p, earth, index < 3 ? 0.8F : 0.95F, height, (float) (yaw + r.nextDouble() * 0.8), lean, 30);
					if (index % 2 == 0 && index >= 3) {
						Vec3 side = ArtKit.right(dir).scale(r.nextBoolean() ? 0.8 : -0.8);
						ArtBlocks.spire(level, p.add(side), earth, 0.6F, height * 0.6F, (float) (yaw + r.nextDouble()), -lean * 1.5F, 26);
					}
				}
				if (index % 3 == 0) {
					Feels.sound(level, p, "earth_menhir_rise", 0.7F, 0.9F + 0.04F * index);
				}
				for (LivingEntity foe : ArtKit.around(player, p, ArtRules.SPLITTER_REACH, 1.0, 3.0, ArtRules.SPLITTER_TARGETS)) {
					if (!struck.add(foe.getUUID()) || struck.size() > ArtRules.SPLITTER_TARGETS) {
						continue;
					}
					hits.strike(foe, ArtRules.SPLITTER_FACTOR, AuraFxRules.Weight.GRAND);
					ArtKit.lift(foe, ArtRules.SPLITTER_LIFT, 30);
					ArtKit.holdLater(player, foe, 12, ArtRules.SPLITTER_HOLD);
					AuraPhysicalFx.earthImpact(level, foe.getBoundingBox().getCenter(), 1.0);
				}
			});
		}
		return true;
	}
}
