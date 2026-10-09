package dev.wildercord.gametest.scavenger.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.world.AuraBeast;
import dev.wildercord.aura.world.ScavengerFoodProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=AuraBeast.class,remap=false)
public abstract class ScavengerAiMixin {
 @WrapMethod(method="customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",require=1,expect=1,allow=1)
 private void wildercord$scavengerAi(ServerLevel level,Operation<Void> original){ScavengerFoodProbe.ai(this,level,original);}
}
