package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Exact vanilla body material and root submitted for the actual connected remote actor. */
@Mixin(LivingEntityRenderer.class)
public abstract class CounterPeerWorldBodyMixin {
    @WrapOperation(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),require=1,expect=1,allow=1)
    private void counter$body(MultiBufferSource buffer,Model model,Object state,PoseStack pose,RenderType material,
        int light,int overlay,int color,TextureAtlasSprite uv,int outline,ModelFeatureRenderer.CrumblingOverlay crumbling,Operation<Void> original){
        var root=CounterPeerRenderProbe.passive(()->new org.joml.Matrix4f(pose.last().pose()),null);
        var call=CounterPeerRenderProbe.passive(()->CounterPeerRenderProbe.worldSubmissionBegin(model,state,root,material),null);boolean completed=false;
        try{original.call(buffer,model,state,pose,material,light,overlay,color,uv,outline,crumbling);
            CounterPeerRenderProbe.passive(()->CounterPeerRenderProbe.worldSubmitted(model,state,root,material));completed=true;
        }finally{boolean done=completed;CounterPeerRenderProbe.passive(()->CounterPeerRenderProbe.worldSubmissionEnd(call,done));}
    }
}
