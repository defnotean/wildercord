package dev.wildercord.gametest.mixin;

import dev.wildercord.client.fx.LightParticle;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightParticle.class)
public abstract class MastersNpcWarningProbeMixin extends SingleQuadParticle {
	@Shadow @Final private int kind;
	@Shadow @Final private int color;
	@Shadow @Final private float a;
	@Shadow @Final private float b;
	@Shadow @Final private float c;
	@Shadow @Final private float width;
	protected MastersNpcWarningProbeMixin(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) { super(level, x, y, z, sprite); }
	@Inject(method = "extract", at = @At("TAIL"))
	private void wildercord$nativeWarning(QuadParticleRenderState state, Camera camera, float partial, CallbackInfo ci) {
		if (kind == dev.wildercord.content.LightOption.RAY)
			MastersNpcCaptureProbe.ray(color, new Vec3(x, y, z), new Vec3(a, b, c), width, age, lifetime, partial);
	}
}
