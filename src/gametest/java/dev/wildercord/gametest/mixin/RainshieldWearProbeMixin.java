package dev.wildercord.gametest.mixin;
import dev.wildercord.wildlife.RainshieldFaultTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Test-only synchronous witness at the actual paid wear call; never cancels or fabricates a hit. */
@Mixin(ItemStack.class)
public abstract class RainshieldWearProbeMixin {
 @Inject(method="hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",at=@At("HEAD"))
 private void wildercord$rainshieldWear(int amount,LivingEntity actor,EquipmentSlot slot,CallbackInfo ci){RainshieldFaultTest.observeWear((ItemStack)(Object)this,amount,actor,slot);}
}
