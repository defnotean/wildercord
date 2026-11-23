package dev.wildercord.mixin;

import dev.wildercord.cast.BossRules;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raises vanilla's 1024.0 ceiling on {@code minecraft:max_health} so high-tier and co-op bosses keep their full health pools. */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMaxHealthMixin {
	@Shadow @Final @Mutable
	private double maxValue;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void wildercord$raiseMaxHealthCap(String descriptionId, double defaultValue, double minValue, double maxValue, CallbackInfo ci) {
		if (BossRules.isMaxHealthAttribute(descriptionId) && this.maxValue < BossRules.MAX_HEALTH_CAP) {
			this.maxValue = BossRules.MAX_HEALTH_CAP;
		}
	}
}
