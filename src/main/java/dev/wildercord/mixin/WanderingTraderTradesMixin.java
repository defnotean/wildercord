package dev.wildercord.mixin;

import dev.wildercord.runesmith.VillagerTradeCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.10 builds the wandering trader's offers at restock; the wildercord uncommon offers are appended there. */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderTradesMixin {
	@Inject(method = "updateTrades", at = @At("RETURN"))
	private void wildercord$runeTrades(CallbackInfo ci) {
		VillagerTradeCompat.appendWanderingTrades((WanderingTrader) (Object) this, null);
	}
}
