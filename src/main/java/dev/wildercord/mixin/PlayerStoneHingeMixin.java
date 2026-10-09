package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.StoneHingeReceipt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Counts native hits and observes the wound for an armed Stone Hinge catch. Damage is never changed. */
@Mixin(Player.class)
public abstract class PlayerStoneHingeMixin {
	@WrapMethod(method = "hurtServer")
	private boolean wildercord$stoneHingeHit(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
		return StoneHingeReceipt.hurt((Player) (Object) this, source, () -> original.call(level, source, amount));
	}

	@WrapMethod(method = "actuallyHurt")
	private void wildercord$stoneHingeWound(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
		StoneHingeReceipt.wound((Player) (Object) this, source, () -> original.call(level, source, amount));
	}
}
