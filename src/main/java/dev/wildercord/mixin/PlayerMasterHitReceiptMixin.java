package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.world.MasterHitReceipt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Observe a scoped spellbreaking strike's native damage; vanilla owns every defence and resource mutation. */
@Mixin(Player.class)
public abstract class PlayerMasterHitReceiptMixin {
	@WrapMethod(method = "actuallyHurt")
	private void wildercord$masterHitReceipt(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
		var observation = MasterHitReceipt.begin((Player) (Object) this, source);
		try {
			original.call(level, source, amount);
		} finally {
			MasterHitReceipt.finish(observation);
		}
	}
}
