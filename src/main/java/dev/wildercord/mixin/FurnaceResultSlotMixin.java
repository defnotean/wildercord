package dev.wildercord.mixin;

import dev.wildercord.cast.PlayerAffinities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smelting feeds fire: what a player takes out of a furnace, blast furnace or smoker by hand (where the game
 * hands out the experience) counts toward their fire affinity. A hopper taking it counts for nobody.
 */
@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotMixin {
	@Shadow
	@Final
	private Player player;

	@Shadow
	private int removeCount;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void wildercord$smelted(net.minecraft.world.item.ItemStack carried, CallbackInfo ci) {
		if (removeCount > 0 && player instanceof ServerPlayer server && ((Slot) (Object) this).container instanceof AbstractFurnaceBlockEntity) {
			PlayerAffinities.smelted(server, removeCount);
		}
	}
}
