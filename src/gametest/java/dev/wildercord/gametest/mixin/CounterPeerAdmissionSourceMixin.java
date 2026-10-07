package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.MastersArts;
import dev.wildercord.gametest.CounterPeerAdmissionProbe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=MastersArts.class,remap=false)
public abstract class CounterPeerAdmissionSourceMixin {
    @Inject(method="broadcast",at=@At("HEAD"),require=1,expect=1,allow=1)
    private static void counter$source(ServerPlayer actor,MastersArts.Performed source,CallbackInfo ci){CounterPeerAdmissionProbe.broadcast(actor,source);}
}
