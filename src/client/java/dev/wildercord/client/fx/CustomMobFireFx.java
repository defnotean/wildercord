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
			double w = e.getBbWidth();
			double h = e.getBbHeight();
			double radius = CustomMobFireRules.perimeterRadius(w);
			int count = CustomMobFireRules.emberCount(w, h);
			for (int i = 0; i < count; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double r = radius * (0.88 + random.nextDouble() * 0.24);
				double px = e.getX() + Math.cos(angle) * r;
				double py = e.getY() + 0.06 + random.nextDouble() * Math.max(0.35, h * 0.58);
				double pz = e.getZ() + Math.sin(angle) * r;
				double vx = Math.cos(angle) * 0.012;
				double vy = 0.025 + random.nextDouble() * 0.03;
				double vz = Math.sin(angle) * 0.012;
				var flame = (i == 0 || py < e.getY() + h * 0.32) ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME;
				level.addParticle(flame, px, py, pz, vx, vy, vz);
			}
			if ((phase & 1) == 0) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double sx = e.getX() + Math.cos(angle) * (radius * 0.95);
				double sy = e.getY() + h * (0.72 + random.nextDouble() * 0.24);
				double sz = e.getZ() + Math.sin(angle) * (radius * 0.95);
				level.addParticle(ParticleTypes.SMOKE, sx, sy, sz, 0.0, 0.028, 0.0);
				if (random.nextFloat() < 0.45F) {
					double lx = e.getX() + Math.cos(angle + Math.PI * 0.5) * (radius * 0.95);
					double ly = e.getY() + 0.15 + random.nextDouble() * Math.max(0.25, h * 0.45);
					double lz = e.getZ() + Math.sin(angle + Math.PI * 0.5) * (radius * 0.95);
					level.addParticle(ParticleTypes.LAVA, lx, ly, lz, 0.0, 0.02, 0.0);
				}
			}
		}
	}
}
