package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the authentic receiver before its map can overwrite a first source. Never changes production admission. */
@Mixin(value=MastersArtsClient.class,remap=false)
public abstract class CounterPeerAcceptedSourceMixin {
    @Inject(method="receive",at=@At("HEAD"),require=1,expect=1,allow=1)
    private static void counter$firstReceive(Minecraft client,MastersArts.Performed payload,CallbackInfo ci){CounterPeerRenderProbe.received(client,payload);}
}
