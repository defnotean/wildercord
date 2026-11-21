package dev.wildercord.client.fx;

import dev.wildercord.cast.CustomMobFireRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws clean perimeter flame embers and smoke wisps around burning {@code wildercord} mobs
 * so their models, tells, and animations remain clearly visible instead of hidden inside vanilla fire planes.
 */
public final class CustomMobFireFx {
	private static final double RANGE_SQR = 48.0 * 48.0;

	private CustomMobFireFx() {}

	/** Whether {@code entity} is a custom Wildercord living mob whose fire visual uses clean perimeter embers. */
	public static boolean isCustomMob(Entity entity) {
		if (!(entity instanceof LivingEntity)) {
			return false;
		}
		Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return CustomMobFireRules.isCustomMobNamespace(key.getNamespace());
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.isPaused() || client.player == null) {
			return;
		}
		RandomSource random = level.getRandom();
		for (Entity e : level.entitiesForRendering()) {
			if (!e.isAlive() || e.distanceToSqr(client.player) > RANGE_SQR || !isCustomMob(e) || !e.displayFireAnimation()) {
				continue;
			}
			int phase = e.tickCount + e.getId();
			if ((phase & 1) != 0) {
				continue;
			}
			double w = e.getBbWidth();
			double h = e.getBbHeight();
			double radius = CustomMobFireRules.perimeterRadius(w);
			int count = CustomMobFireRules.emberCount(w, h);
			for (int i = 0; i < count; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double px = e.getX() + Math.cos(angle) * radius;
				double py = e.getY() + 0.12 + random.nextDouble() * Math.max(0.25, h * 0.55);
				double pz = e.getZ() + Math.sin(angle) * radius;
				double vx = Math.cos(angle) * 0.015;
				double vy = 0.03 + random.nextDouble() * 0.025;
				double vz = Math.sin(angle) * 0.015;
				level.addParticle(ParticleTypes.SMALL_FLAME, px, py, pz, vx, vy, vz);
			}
			if ((phase & 3) == 0) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double sx = e.getX() + Math.cos(angle) * (radius * 0.85);
				double sy = e.getY() + h * (0.78 + random.nextDouble() * 0.18);
				double sz = e.getZ() + Math.sin(angle) * (radius * 0.85);
				level.addParticle(ParticleTypes.SMOKE, sx, sy, sz, 0.0, 0.025, 0.0);
				if (random.nextFloat() < 0.35F) {
					double lx = e.getX() + Math.cos(angle + Math.PI * 0.5) * (radius * 0.9);
					double ly = e.getY() + 0.15 + random.nextDouble() * Math.max(0.2, h * 0.4);
					double lz = e.getZ() + Math.sin(angle + Math.PI * 0.5) * (radius * 0.9);
					level.addParticle(ParticleTypes.LAVA, lx, ly, lz, 0.0, 0.02, 0.0);
				}
			}
		}
	}
}
