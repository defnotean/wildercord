package dev.wildercord.gametest.mossveil.mixin;
import dev.wildercord.wildlife.MossveilNativeFaults;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStack.class)
abstract class MossveilWearCallbackMixin{
 @Inject(method="hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",at=@At("TAIL"))
 private void mossveil$wear(int amount,LivingEntity actor,EquipmentSlot slot,CallbackInfo ci){if(actor instanceof ServerPlayer p&&slot==EquipmentSlot.HEAD)MossveilNativeFaults.callback(p,false,(ItemStack)(Object)this);}
}
