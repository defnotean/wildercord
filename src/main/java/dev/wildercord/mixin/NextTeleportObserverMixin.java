package dev.wildercord.mixin;
import dev.wildercord.cast.CounterSignatures;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Draft promotion must validate both mapped descriptors. Small true teleports release a movement ledger. */
@Mixin(Entity.class)
public abstract class NextTeleportObserverMixin {
 @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",at=@At("HEAD"))
 private void wildercord$ledgerWorldTeleport(CallbackInfoReturnable<Boolean> info){CounterSignatures.teleported(((Entity)(Object)this).getUUID());}
 @Inject(method="teleportTo(DDD)V",at=@At("HEAD"))
 private void wildercord$ledgerLocalTeleport(CallbackInfo info){CounterSignatures.teleported(((Entity)(Object)this).getUUID());}
}
