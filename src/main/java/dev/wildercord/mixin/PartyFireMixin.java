package dev.wildercord.mixin;

import dev.wildercord.party.Parties;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attributed allied magic cannot ignite or freeze a body. Cooling and environmental harm still work. */
@Mixin(Entity.class)
public abstract class PartyFireMixin {
	@Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
	private void wildercord$partyFire(int ticks, CallbackInfo ci) {
		Entity target = (Entity) (Object) this;
		if (ticks > target.getRemainingFireTicks() && Parties.blocksCurrentHarm(target)) ci.cancel();
	}

	@Inject(method = "setTicksFrozen", at = @At("HEAD"), cancellable = true)
	private void wildercord$partyFrost(int ticks, CallbackInfo ci) {
		Entity target = (Entity) (Object) this;
		if (ticks > target.getTicksFrozen() && Parties.blocksCurrentHarm(target)) ci.cancel();
	}
}
