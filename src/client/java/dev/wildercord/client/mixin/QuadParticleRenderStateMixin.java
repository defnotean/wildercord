package dev.wildercord.client.mixin;

import dev.wildercord.client.compat.ShaderCompat;
import dev.wildercord.client.fx.GlowLayers;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Under a shader pack, magic's light and darkness go on vanilla's translucent particle layer instead
 * of their own blends (see {@link ShaderCompat}). Every particle quad passes through here, so every
 * spell is covered in one place. Light keeps its colour; darkness, which is drawn as what to take
 * away, becomes the dark shade that taking it away leaves behind.
 */
@Mixin(QuadParticleRenderState.class)
public abstract class QuadParticleRenderStateMixin {
	@Shadow
	public abstract void add(SingleQuadParticle.Layer layer, float x, float y, float z, float xRot, float yRot, float zRot, float wRot,
		float scale, float u0, float u1, float v0, float v1, int color, int lightCoords);

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void wildercord$plainUnderShaders(SingleQuadParticle.Layer layer, float x, float y, float z, float xRot, float yRot, float zRot,
		float wRot, float scale, float u0, float u1, float v0, float v1, int color, int lightCoords, CallbackInfo ci) {
		if ((layer != GlowLayers.GLOW && layer != GlowLayers.DARK) || !ShaderCompat.active()) {
			return;
		}
		ci.cancel();
		int plain = layer == GlowLayers.DARK ? GlowLayers.darkAsShade(color) : color;
		add(SingleQuadParticle.Layer.TRANSLUCENT, x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, plain, lightCoords);
	}
}
