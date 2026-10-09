package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.gametest.CounterPeerAdmissionProbe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

/** Native calls and results are preserved; all observation failures are latched for the test thread. */
@Mixin(value=SwordStrings.class,remap=false)
public abstract class CounterPeerAdmissionMixin {
    @WrapMethod(method="request",require=1,expect=1,allow=1)
    private static void counter$request(ServerPlayer actor,SwordStrings.Perform payload,Operation<Void> original){
        CounterPeerAdmissionProbe.request(actor,payload,()->original.call(actor,payload));
    }
    @WrapOperation(method="request",at=@At(value="INVOKE",target="Ldev/wildercord/cast/ActionAdmission;busy(Lnet/minecraft/server/level/ServerPlayer;)Z"),require=1,expect=1,allow=1)
    private static boolean counter$busy(ServerPlayer actor,Operation<Boolean> original){return CounterPeerAdmissionProbe.gate(actor,"busy",()->original.call(actor));}
    @WrapOperation(method="request",at=@At(value="INVOKE",target="Ldev/wildercord/cast/ExciseCasting;blocking(Lnet/minecraft/server/level/ServerPlayer;)Z"),require=1,expect=1,allow=1)
    private static boolean counter$excise(ServerPlayer actor,Operation<Boolean> original){return CounterPeerAdmissionProbe.gate(actor,"excise",()->original.call(actor));}
    @WrapOperation(method="request",at=@At(value="INVOKE",target="Ldev/wildercord/aura/SwordStrings;check(Lnet/minecraft/server/level/ServerPlayer;Ldev/wildercord/api/AuraApi$StringArt;Ljava/util/List;)Ljava/util/Optional;"),require=1,expect=1,allow=1)
    private static Optional<SwordStrings.Refusal> counter$check(ServerPlayer actor,AuraApi.StringArt art,List<Integer> marks,Operation<Optional<SwordStrings.Refusal>> original){
        return CounterPeerAdmissionProbe.check(actor,()->original.call(actor,art,marks));
    }
    @Inject(method="refuse",at=@At("HEAD"),require=1,expect=1,allow=1)
    private static void counter$refused(ServerPlayer actor,String id,AuraApi.StringArt art,SwordStrings.Refusal reason,CallbackInfo ci){CounterPeerAdmissionProbe.refused(actor,id,reason);}
    @WrapOperation(method="payAndRest",at=@At(value="INVOKE",target="Ldev/wildercord/aura/Aura;spend(Lnet/minecraft/server/level/ServerPlayer;DLjava/lang/String;)Ldev/wildercord/aura/AuraRules$Spend;"),require=1,expect=1,allow=1)
    private static AuraRules.Spend counter$paid(ServerPlayer actor,double cost,String reason,Operation<AuraRules.Spend> original){return CounterPeerAdmissionProbe.spend(actor,cost,reason,()->original.call(actor,cost,reason));}
    @Inject(method="perform(Lnet/minecraft/server/level/ServerPlayer;Ldev/wildercord/api/AuraApi$StringArt;Ljava/util/List;Ldev/wildercord/aura/EarnedCounters$Attempt;Z)Z",at=@At("RETURN"),require=1)
    private static void counter$performed(ServerPlayer actor,AuraApi.StringArt art,List<Integer> marks,@Coerce Object counter,boolean fromClash,CallbackInfoReturnable<Boolean> result){
        CounterPeerAdmissionProbe.performed(actor,art,marks,counter,fromClash,result.getReturnValue());
    }
}
