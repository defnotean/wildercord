package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Observes the real baked item node after native display/local transforms; never invokes a detached renderer. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class CounterPeerItemDrawMixin {
    // 1.21.8: the outer state is field_55345, there is one ItemTransform, and the
    // render type is an argument of submitItem; the local pose is the PoseStack itself.
    @Shadow @Final private ItemStackRenderState field_55345;
    @Shadow private ItemTransform transform;
    @Shadow private net.minecraft.client.renderer.RenderType renderType;
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"),require=1,expect=1,allow=1)
    private void counter$draw(ItemDisplayContext context,PoseStack pose,MultiBufferSource buffer,int light,int overlay,
        int[] tints,List<BakedQuad> quads,net.minecraft.client.renderer.RenderType material,ItemStackRenderState.FoilType foil,Operation<Void> original){
        var actual=new Matrix4f(pose.last().pose());var local=new Matrix4f(actual);var display=transform;
        original.call(context,pose,buffer,light,overlay,tints,quads,material,foil);
        CounterPeerRenderProbe.itemDraw(field_55345,display,local,context,quads,actual,pose,buffer);
    }
}
