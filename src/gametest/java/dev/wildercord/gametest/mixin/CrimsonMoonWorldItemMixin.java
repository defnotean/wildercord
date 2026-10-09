package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
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
public abstract class CrimsonMoonWorldItemMixin {
    @Unique private static final ThreadLocal<CrimsonMoonRenderProbe.WorldItem> moon$current=new ThreadLocal<>();
    @WrapMethod(method="submitArmWithItem",require=1,expect=1,allow=1)
    private void moon$scope(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose,SubmitNodeCollector collector,int light,Operation<Void> original){
        var previous=moon$current.get();
        var call=CrimsonMoonRenderProbe.worldItemBegin(((ItemInHandLayer<?,?>)(Object)this).getParentModel(),state,item,stack,arm,pose);
        moon$current.set(call);boolean completed=false;
        try{original.call(state,item,stack,arm,pose,collector,light);completed=true;}
        finally{try{CrimsonMoonRenderProbe.worldItemEnd(call,completed);}finally{if(previous==null)moon$current.remove();else moon$current.set(previous);}}
    }
    @WrapOperation(method="submitArmWithItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"),require=1,expect=1,allow=1)
    private void moon$item(ItemStackRenderState item,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,Operation<Void> original){
        var actual=new org.joml.Matrix4f(pose.last().pose());original.call(item,pose,collector,light,overlay,outline);
        CrimsonMoonRenderProbe.worldItemSubmitted(moon$current.get(),item,actual);
    }
}
