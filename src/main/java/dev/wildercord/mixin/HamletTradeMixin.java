package dev.wildercord.mixin;

import dev.wildercord.town.Hamlets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A trade with a villager counts toward its hamlet's standing. */
@Mixin(Villager.class)
public abstract class HamletTradeMixin {
	@Inject(method = "rewardTradeXp", at = @At("HEAD"))
	private void wildercord$hamletTraded(MerchantOffer offer, CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		if (villager.getTradingPlayer() instanceof ServerPlayer player) Hamlets.traded(player, villager);
	}
}
