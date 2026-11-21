package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import dev.wildercord.client.compat.McQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Quaternionf;

/**
 * A rimehare's frost print: a little paw-shaped rime lying flat on the ground where it landed, fading over three
 * seconds. Made straight into the particle engine by {@link RimehareRenderer} on each client, never sent; vanilla's
 * translucent particle layer, so shader packs draw it as they do any particle.
 */
final class FrostPrint extends McQuadParticle {
	private final Quaternionf flat;

	FrostPrint(ClientLevel level, double x, double y, double z, float yaw, float size) {
		super(level, x, y, z, sprite());
		this.flat = new Quaternionf().rotationY(-yaw).rotateX(-(float) Math.PI / 2);
		this.quadSize = size;
		this.lifetime = 50 + random.nextInt(20);
		this.hasPhysics = false;
		this.gravity = 0;
		this.alpha = 0;
		setColor(0.92F, 0.97F, 1.0F);
	}

	private static TextureAtlasSprite sprite() {
		return dev.wildercord.client.compat.RenderCompat.atlas(TextureAtlas.LOCATION_PARTICLES).apply(Wildercord.id("frost_print"));
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
			return;
		}
		float t = age / (float) lifetime;
		// Pressed in at once, then thinning away as the rime sublimes.
		alpha = Math.min(1, t * 8) * Math.min(1, (1 - t) / 0.7F) * 0.85F;
	}

	@Override
	public FacingCameraMode getFacingCameraMode() {
		return (target, camera, partial) -> target.set(flat);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
