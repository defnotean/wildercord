package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.BrambleFireProbe;
import dev.wildercord.monster.Bramblewalker;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Registered only by the GameTest mod. All decisions, randomness and movement remain native. */
@Mixin(targets = "dev.wildercord.monster.Bramblewalker$FleeFireGoal", remap = false)
public abstract class BrambleFireProbeMixin {
	@Shadow @Final private Bramblewalker this$0;

	@WrapOperation(method = "pick()Z", at = @At(value = "INVOKE", target =
		"Lnet/minecraft/world/entity/ai/util/DefaultRandomPos;getPosAway(Lnet/minecraft/world/entity/PathfinderMob;IILnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"), require = 1, expect = 1, allow = 1)
	private Vec3 wildercord$pick(PathfinderMob actor, int horizontal, int vertical, Vec3 away, Operation<Vec3> original) {
		return BrambleFireProbe.pick(actor, horizontal, vertical, away, original);
	}

	@WrapOperation(method = {"start()V", "tick()V"}, at = @At(value = "INVOKE", target =
		"Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 2, expect = 2, allow = 2)
	private boolean wildercord$move(PathNavigation navigation, double x, double y, double z, double speed, Operation<Boolean> original) {
		return BrambleFireProbe.move(this$0, navigation, x, y, z, speed, original);
	}

	@Inject(method = "canUse()Z", at = @At("RETURN"), require = 1, allow = 1)
	private void wildercord$admission(CallbackInfoReturnable<Boolean> cir) {
		BrambleFireProbe.goal(this$0, "admission", cir.getReturnValue());
	}

	@Inject(method = "canContinueToUse()Z", at = @At("RETURN"), require = 1, allow = 1)
	private void wildercord$continuation(CallbackInfoReturnable<Boolean> cir) {
		BrambleFireProbe.goal(this$0, "continue", cir.getReturnValue());
	}

	@Inject(method = "start()V", at = @At("TAIL"), require = 1, allow = 1)
	private void wildercord$start(CallbackInfo ci) { BrambleFireProbe.goal(this$0, "start", null); }

	@Inject(method = "stop()V", at = @At("HEAD"), require = 1, allow = 1)
	private void wildercord$stop(CallbackInfo ci) { BrambleFireProbe.goal(this$0, "stop", null); }
}
