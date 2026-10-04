package dev.wildercord.gametest.mossveil.mixin;
import dev.wildercord.wildlife.MossveilNativeFaults;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
abstract class MossveilNoticeCallbackMixin{
 @Inject(method="onEffectUpdated",at=@At("TAIL"))
 private void mossveil$notice(MobEffectInstance effect,boolean attributes,Entity source,CallbackInfo ci){if((Object)this instanceof ServerPlayer p&&effect.is(MobEffects.POISON))MossveilNativeFaults.callback(p,true,null);}
}
