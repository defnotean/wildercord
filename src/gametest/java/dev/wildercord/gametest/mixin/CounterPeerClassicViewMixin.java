package dev.wildercord.gametest.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.MastersArtPose;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
/** The only mutation of the live stack is the unchanged production call. */
@Mixin(value=MastersArtPose.class,remap=false)
public abstract class CounterPeerClassicViewMixin {
 @WrapMethod(method="firstPerson",require=1,expect=1,allow=1)
 private static void counter$transform(PoseStack stack,InteractionHand hand,AvatarRenderState state,float inverse,Operation<Void> original){
  var call=CounterPeerRenderProbe.classicBegin(stack,hand,state,inverse);original.call(stack,hand,state,inverse);CounterPeerRenderProbe.classicEnd(call,stack);
 }
}
