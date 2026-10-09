package dev.wildercord.mixin;

import dev.wildercord.cast.packs.WardState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The support pack's hold on mobs: a pacified, lured or spooked mob picks no target and a taunted one only its taunter
 * (Pacify, Truce, Accord, Lure, Spook, Taunt, Withdraw), and no monster spawns of itself inside a Sanctuary or an Accord.
 */
@Mixin(Mob.class)
public abstract class FxSupportMobMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void wildercord$fxSupportCalm(LivingEntity target, CallbackInfo ci) {
		if (WardState.refusesTarget((Mob) (Object) this, target)) {
			ci.cancel();
		}
	}

	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private void wildercord$fxSupportSanctuary(LevelAccessor world, EntitySpawnReason reason, CallbackInfoReturnable<Boolean> cir) {
		Mob self = (Mob) (Object) this;
		if (self instanceof Enemy && world instanceof Level level && !level.isClientSide()
				&& (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.REINFORCEMENT || reason == EntitySpawnReason.PATROL)
				&& WardState.forbidsSpawn(level, self.getX(), self.getY(), self.getZ())) {
			cir.setReturnValue(false);
		}
	}
}
