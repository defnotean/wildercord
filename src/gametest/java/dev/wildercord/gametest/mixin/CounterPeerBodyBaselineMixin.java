package dev.wildercord.gametest.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
/** Copies vanilla locals at the real production boundary, never resets or prepares the model. */
@Mixin(value=ArticulatedCombat.class,remap=false)
public abstract class CounterPeerBodyBaselineMixin {
 @WrapMethod(method="applyPlayer",require=1,expect=1,allow=1)
 private static boolean counter$baseline(PlayerModel model,AvatarRenderState state,Operation<Boolean> original){CounterPeerRenderProbe.passive(()->CounterPeerRenderProbe.bodyBaseline(model,state));return original.call(model,state);}
}
