package dev.wildercord.client.mixin;

import dev.wildercord.client.fx.StormSky;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Under a mana storm the sky, the fog and the clouds lean violet (see {@link StormSky}): three more
 * layers on the client level's environment attributes, beside vanilla's own lightning-flash layer,
 * so the sky disc, the fog colour and the clouds all read the same tinted values.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
	private void wildercord$stormLayers(EnvironmentAttributeSystem.Builder builder, CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
		EnvironmentAttributeSystem.Builder layers = cir.getReturnValue();
		layers.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (color, tick) -> StormSky.tintSky(color));
		layers.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (color, tick) -> StormSky.tintFog(color));
		layers.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR, (color, tick) -> StormSky.tintCloud(color));
	}

	@ModifyVariable(method = "setTimeFromServer", at = @At("HEAD"), argsOnly = true)
	private long wildercord$adjustPostTickTime(long gameTime) {
		ClientLevel self = (ClientLevel) (Object) this;
		if (!dev.wildercord.client.WildercordClient.inStartClientTick && self.tickRateManager().runsNormally()) {
			return gameTime + 1L;
		}
		return gameTime;
	}
}
