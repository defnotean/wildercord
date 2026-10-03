package dev.wildercord.mixin;
import dev.wildercord.wildlife.FoxCompanions;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Fox.class)
public class FoxCompanionGoalsMixin {
 @Inject(method="registerGoals",at=@At("TAIL"))
 private void wildercord$companionGoals(CallbackInfo ci) {FoxCompanions.goals((Fox)(Object)this);}
}
