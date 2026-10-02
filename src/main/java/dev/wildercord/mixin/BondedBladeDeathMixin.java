package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A dying swordsman's bonded blade is kept for their next body: taken out of the inventory before anything drops (or vanishes to a
 * curse), as the first thing {@code dropEquipment} does. With keepInventory nothing drops anyway, and vanilla carries it over.
 */
@Mixin(Player.class)
public abstract class BondedBladeDeathMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void wildercord$keepBlade(ServerLevel level, CallbackInfo ci) {
		if (!level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			BondedBlades.dying((Player) (Object) this);
		}
	}
}
