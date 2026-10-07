package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Observes the real baked item node after native display/local transforms; never invokes a detached renderer. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class CounterPeerItemDrawMixin {
    @Shadow @Final private ItemStackRenderState this$0;
    @Shadow private ItemTransform itemTransform;
    @Shadow @Final private Matrix4f localTransform;
    @WrapOperation(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILnet/minecraft/client/resources/model/geometry/ItemQuads;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"),require=1,expect=1,allow=1)
    private void counter$draw(SubmitNodeCollector collector,PoseStack pose,ItemDisplayContext context,int light,int overlay,int outline,
        int[] tints,ItemQuads quads,ItemStackRenderState.FoilType foil,Operation<Void> original){
        var actual=new Matrix4f(pose.last().pose());var local=new Matrix4f(localTransform);var transform=itemTransform;
        original.call(collector,pose,context,light,overlay,outline,tints,quads,foil);
        CounterPeerRenderProbe.itemDraw(this$0,transform,local,context,quads,actual,pose,collector);
    }
}
