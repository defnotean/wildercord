package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Exact vanilla body material and root submitted for the actual connected remote actor. */
@Mixin(LivingEntityRenderer.class)
public abstract class CounterPeerWorldBodyMixin {
    @WrapOperation(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"),require=1,expect=1,allow=1)
    private void counter$body(SubmitNodeCollector collector,Model<?> model,Object state,PoseStack pose,RenderType material,
        int light,int overlay,int color,UvMapping uv,int outline,Operation<Void> original){
        var root=new org.joml.Matrix4f(pose.last().pose());
        original.call(collector,model,state,pose,material,light,overlay,color,uv,outline);
        CounterPeerRenderProbe.worldSubmitted(model,state,root,material);
    }
}
