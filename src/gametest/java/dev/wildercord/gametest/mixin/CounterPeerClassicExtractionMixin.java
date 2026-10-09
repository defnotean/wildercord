package dev.wildercord.gametest.mixin;
import dev.wildercord.client.MastersArtPose;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Retains the exact immutable production frame before HitStop can preserve it. */
@Mixin(value=MastersArtPose.class,remap=false)
public abstract class CounterPeerClassicExtractionMixin {
 @Inject(method="extract",at=@At("RETURN"),require=1)
 private static void counter$origin(Avatar owner,AvatarRenderState state,float partial,CallbackInfo ci){CounterPeerRenderProbe.classicExtracted(owner,state,partial);}
}
