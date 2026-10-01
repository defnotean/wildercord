package dev.wildercord.client.cosmetic;

import dev.wildercord.cosmetic.CordCosmetics;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Cast trails: the sparks, petals, snow, embers or stars that stream from a player's Cord hand as
 * they cast (and, fainter, while they charge). Every client draws them for every player near it,
 * from the synced cast pose and style, so nothing extra is sent.
 */
public final class CordTrails {
	private CordTrails() {}

	/** The last cast seen from each player (by entity id), so each cast bursts once. */
	private static final Map<Integer, Long> SEEN = new HashMap<>();

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			SEEN.clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		long now = mc.level.getGameTime();
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		for (AbstractClientPlayer player : mc.level.players()) {
			if (player.isInvisible() || player.position().distanceToSqr(camera) > 48 * 48) {
				continue;
			}
			WildercordAttachments.CordLook cord = player.getAttachedOrElse(WildercordAttachments.CORD_LOOK, WildercordAttachments.CordLook.NONE);
			String trail = CordCosmetics.style(player).trail();
			if (cord.tier().isEmpty() || trail.equals("none")) {
				continue;
			}
			WildercordAttachments.CastPose pose = player.getAttached(WildercordAttachments.CAST_POSE);
			if (pose != null && now - pose.start() < WildercordAttachments.CastPose.TICKS) {
				Long seen = SEEN.put(player.getId(), pose.start());
				// The moment of the cast: a burst from the hand; then a thinning stream while the pose lasts.
				int count = seen == null || seen != pose.start() ? 14 : 2;
				emit(mc, player, trail, count, true);
			} else if (player.hasAttached(WildercordAttachments.CHARGE) && now % 3 == 0) {
				emit(mc, player, trail, 1, false);
			}
		}
		if (now % 200 == 0) {
			SEEN.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		}
	}

	/** Where the Cord hand is: held out in front while casting or charging. */
	private static Vec3 hand(AbstractClientPlayer player) {
		double yaw = Math.toRadians(player.yBodyRot);
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 look = player.getLookAngle();
		return player.getEyePosition().add(0, -0.45, 0).add(look.scale(0.55)).add(right.scale(0.32));
	}

	private static void emit(Minecraft mc, AbstractClientPlayer player, String trail, int count, boolean burst) {
		RandomSource random = player.getRandom();
		Vec3 at = hand(player);
		Vec3 look = player.getLookAngle();
		// Your own trail stays out of your eyes in first person: it starts a little further along your arm.
		if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
			at = at.add(look.scale(0.35)).add(0, -0.15, 0);
		}
		for (int i = 0; i < count; i++) {
			double spread = burst ? 0.18 : 0.08;
			double x = at.x + (random.nextDouble() - 0.5) * spread;
			double y = at.y + (random.nextDouble() - 0.5) * spread;
			double z = at.z + (random.nextDouble() - 0.5) * spread;
			double speed = burst ? 0.05 + random.nextDouble() * 0.08 : 0.01;
			Vec3 v = look.scale(speed).add((random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.3) * 0.04, (random.nextDouble() - 0.5) * 0.04);
			mc.level.addParticle(dev.wildercord.cast.SpellMaterials.custom(particle(trail, random),new Vec3(x,y,z)), x, y, z, v.x, v.y + lift(trail), v.z);
		}
	}

	private static ParticleOptions particle(String trail, RandomSource random) {
		return switch (trail) {
			case "sparks" -> random.nextInt(3) == 0 ? ParticleTypes.FIREWORK : ParticleTypes.ELECTRIC_SPARK;
			case "petals" -> ParticleTypes.CHERRY_LEAVES;
			case "snow" -> ParticleTypes.SNOWFLAKE;
			case "embers" -> random.nextInt(4) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME;
			case "stars" -> random.nextInt(3) == 0 ? ParticleTypes.GLOW : ParticleTypes.END_ROD;
			default -> ParticleTypes.END_ROD;
		};
	}

	/** Embers rise, snow and petals drift down. */
	private static double lift(String trail) {
		return switch (trail) {
			case "embers" -> 0.03;
			case "snow", "petals" -> -0.01;
			default -> 0;
		};
	}
}
