package dev.wildercord.gametest.scavenger.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.world.ScavengerFoodProbe;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Mob.class)
public abstract class ScavengerTickMixin {
 @WrapMethod(method="tick()V",require=1,expect=1,allow=1)
 private void wildercord$scavengerTick(Operation<Void> original){ScavengerFoodProbe.tick((Mob)(Object)this,original);}
}
