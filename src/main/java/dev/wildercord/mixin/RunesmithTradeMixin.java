package dev.wildercord.mixin;

import dev.wildercord.runesmith.DuplicateSwap;
import dev.wildercord.runesmith.Runesmith;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A Runesmith's buyback or reroll gives no experience (to the player or the villager), and counts toward the day's buybacks. */
@Mixin(Villager.class)
public abstract class RunesmithTradeMixin {
	@Inject(method = "rewardTradeXp", at = @At("HEAD"), cancellable = true)
	private void wildercord$swapTraded(MerchantOffer offer, CallbackInfo ci) {
		if (Runesmith.is((Villager) (Object) this) && DuplicateSwap.isSwap(offer)) {
			DuplicateSwap.traded((Villager) (Object) this, offer);
			ci.cancel();
		}
	}
}
