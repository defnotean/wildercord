package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.world.MasterHitReceipt;
import dev.wildercord.player.ManaSkinDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Native damage resolves first; interruption observes the wound before Mana Skin's bounded rebate. */
@Mixin(Player.class)
public abstract class PlayerMasterHitReceiptMixin {
	@WrapMethod(method = "hurtServer")
	private boolean wildercord$manaSkinScope(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
		return ManaSkinDamage.during((Player) (Object) this, source, () -> original.call(level, source, amount));
	}

	@WrapMethod(method = "actuallyHurt")
	private void wildercord$masterHitReceipt(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
		Player player = (Player) (Object) this;
		var skin = ManaSkinDamage.begin(player, source);
		var observation = MasterHitReceipt.begin(player, source);
		try {
			original.call(level, source, amount);
		} finally {
			MasterHitReceipt.finish(observation);
		}
		ManaSkinDamage.finish(skin);
	}
}
