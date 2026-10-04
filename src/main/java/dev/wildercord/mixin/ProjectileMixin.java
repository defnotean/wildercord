package dev.wildercord.mixin;

import dev.wildercord.cast.Imbuing;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An arrow fired from an imbued bow lets its spell go wherever it strikes, creature or block; and shooting a glyph's block sets it off. */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void wildercord$releaseImbued(HitResult result, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;
		// One physical vanilla arrow catch precedes imbued release; magic shots refuse the relic.
		if (self instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow
				&& result instanceof net.minecraft.world.phys.EntityHitResult hit
				&& dev.wildercord.wildlife.RooksRainshield.intercept(arrow, hit)) { ci.cancel(); return; }
		if (!self.level().isClientSide() && self.hasAttached(WildercordAttachments.IMBUED_SHOT)) {
			Imbuing.onShotHit(self, result);
		}
		// Shooting a glyph's block sets it off.
		if (self.level() instanceof net.minecraft.server.level.ServerLevel level && result instanceof net.minecraft.world.phys.BlockHitResult block
				&& block.getType() != HitResult.Type.MISS) {
			Imbuing.onBlockShot(level, block.getBlockPos());
		}
	}
}
