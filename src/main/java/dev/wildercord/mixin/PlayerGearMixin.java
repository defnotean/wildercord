package dev.wildercord.mixin;

import dev.wildercord.gear.GearSlots;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A dying player's gear slots drop with the rest of their inventory, unless keepInventory is on (then
 * {@code GearSlots} carries them to the respawned player). Vanilla drops the inventory here, so the gear
 * goes in the same place, and Curse of Vanishing is honoured the same way.
 */
@Mixin(Player.class)
public abstract class PlayerGearMixin {
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void wildercord$dropGear(ServerLevel level, CallbackInfo ci) {
		if (!level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			GearSlots.dropAll((Player) (Object) this);
		}
	}
}
