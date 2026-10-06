package dev.wildercord.gametest.nursery.mixin;

import dev.wildercord.wildlife.FungalGillProbe;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native-test-only reads of pickup delay and actual contact; never cancels or changes pickup. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityGillTraceMixin implements FungalGillProbe.ItemDelay {
 @Shadow private int pickupDelay;
 @Unique public int wildercord$gillPickupDelay(){return pickupDelay;}
 @Inject(method="playerTouch",at=@At("HEAD"))
 private void beforeTouch(Player player,CallbackInfo ci){FungalGillProbe.touch((ItemEntity)(Object)this,player,false);}
 @Inject(method="playerTouch",at=@At("RETURN"))
 private void afterTouch(Player player,CallbackInfo ci){FungalGillProbe.touch((ItemEntity)(Object)this,player,true);}
}
