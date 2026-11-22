package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import dev.wildercord.client.compat.vanilla.ArmedEntityRenderState;
import dev.wildercord.client.compat.vanilla.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Passive unchanged world item call; state, model, baseline, item, material and matrices stay bound. */
@Mixin(ItemInHandLayer.class)
public abstract class CrimsonMoonWorldItemMixin {
    @Unique private static final ThreadLocal<CrimsonMoonRenderProbe.WorldItem> moon$current=new ThreadLocal<>();
    @WrapMethod(method="renderArmWithItem",require=1,expect=1,allow=1)
    private void moon$scope(ArmedEntityRenderState state,ItemStackRenderState item,HumanoidArm arm,PoseStack pose,MultiBufferSource buffer,int light,Operation<Void> original){
        var previous=moon$current.get();
        ItemStack stack=arm==HumanoidArm.LEFT?dev.wildercord.client.compat.RenderCompat.leftHandStack(state):dev.wildercord.client.compat.RenderCompat.rightHandStack(state);
        var call=CrimsonMoonRenderProbe.worldItemBegin(((ItemInHandLayer<?,?>)(Object)this).getParentModel(),state,item,stack,arm,pose);
        moon$current.set(call);boolean completed=false;
        try{original.call(state,item,arm,pose,buffer,light);completed=true;}
        finally{try{CrimsonMoonRenderProbe.worldItemEnd(call,completed);}finally{if(previous==null)moon$current.remove();else moon$current.set(previous);}}
    }
    @WrapOperation(method="renderArmWithItem",at=@At(value="INVOKE",target="Ldev/wildercord/client/compat/vanilla/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"),require=1,expect=1,allow=1)
    private void moon$item(ItemStackRenderState item,PoseStack pose,MultiBufferSource buffer,int light,int overlay,Operation<Void> original){
        var actual=new org.joml.Matrix4f(pose.last().pose());original.call(item,pose,buffer,light,overlay);
        CrimsonMoonRenderProbe.worldItemSubmitted(moon$current.get(),item,actual);
    }
}
