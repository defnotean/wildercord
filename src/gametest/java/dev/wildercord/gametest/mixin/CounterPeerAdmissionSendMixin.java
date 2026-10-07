package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.gametest.CounterPeerAdmissionProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The payload at the actual production send, before the unchanged send can reach the server. */
@Mixin(value=SwordStringsClient.class,remap=false)
public abstract class CounterPeerAdmissionSendMixin {
    @WrapOperation(method="handle",at=@At(value="INVOKE",target="Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking;send(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"),require=1,expect=1,allow=1)
    private static void counter$sent(CustomPacketPayload payload,Operation<Void> original){
        CounterPeerAdmissionProbe.sent(Minecraft.getInstance(),(SwordStrings.Perform)payload,()->original.call(payload));
    }
}
