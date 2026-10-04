package dev.wildercord.gametest.mixin;
import dev.wildercord.client.fx.RainshieldClient;
import dev.wildercord.wildlife.RainshieldFx;
import dev.wildercord.wildlife.RainshieldAckTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Gametest-only ingress delay of the real received immutable event, never manufactures a cue. */
@Mixin(value=RainshieldClient.class,remap=false)
public abstract class RainshieldInputProbeMixin {
 @Inject(method="receive",at=@At("HEAD"),cancellable=true,remap=false)
 private static void delay(RainshieldFx.Event e,Minecraft mc,CallbackInfo ci){if(RainshieldAckTest.hold(e,mc))ci.cancel();}
}
