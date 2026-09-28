package dev.wildercord.mixin;

import dev.wildercord.runesmith.DuplicateSwap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Runesmith's swap only takes plain rank I runes: a ranked rune never goes for a rank I's price. */
@Mixin(MerchantOffer.class)
public abstract class SwapOfferMixin {
	@Inject(method = "satisfiedBy", at = @At("HEAD"), cancellable = true)
	private void wildercord$plainRunesOnly(ItemStack buyA, ItemStack buyB, CallbackInfoReturnable<Boolean> cir) {
		if (DuplicateSwap.isSwap((MerchantOffer) (Object) this) && (!DuplicateSwap.pays(buyA) || !DuplicateSwap.pays(buyB))) {
			cir.setReturnValue(false);
		}
	}
}
