package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Passive unchanged world item call; state, model, baseline, item, material and matrices stay bound. */
@Mixin(ItemInHandLayer.class)
public abstract class CounterPeerWorldItemMixin {
    @Unique private static final ThreadLocal<CounterPeerRenderProbe.WorldItem> counter$current=new ThreadLocal<>();
    @WrapMethod(method="submitArmWithItem",require=1,expect=1,allow=1)
    private void counter$scope(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose,SubmitNodeCollector collector,int light,Operation<Void> original){
        var previous=counter$current.get();
        var call=CounterPeerRenderProbe.worldItemBegin(((ItemInHandLayer<?,?>)(Object)this).getParentModel(),state,item,stack,arm,pose,collector);
        counter$current.set(call);boolean completed=false;
        try{original.call(state,item,stack,arm,pose,collector,light);completed=true;}
        finally{try{CounterPeerRenderProbe.worldItemEnd(call,completed);}finally{if(previous==null)counter$current.remove();else counter$current.set(previous);}}
    }
    @WrapOperation(method="submitArmWithItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"),require=1,expect=1,allow=1)
    private void counter$item(ItemStackRenderState item,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,Operation<Void> original){
        var actual=new org.joml.Matrix4f(pose.last().pose());original.call(item,pose,collector,light,overlay,outline);
        CounterPeerRenderProbe.worldItemSubmitted(counter$current.get(),item,actual);
    }
}
