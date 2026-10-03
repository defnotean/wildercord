package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.BeastRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** A feathered ridge runner with a hooked beak, fan tail, articulated wing fingers and taloned feet. */
public final class GaleclawModel extends EntityModel<BeastRenderState> {
	private final ModelPart body,head,wingR,wingL,legR,legL,tail;
	public GaleclawModel(ModelPart root) { super(root);body=root.getChild("body");head=body.getChild("head");wingR=body.getChild("wingR");wingL=body.getChild("wingL");tail=body.getChild("tail");legR=root.getChild("legR");legL=root.getChild("legL"); }
	public static LayerDefinition createLayer() {
		var m=new MeshDefinition();var root=m.getRoot();
		var b=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,-6,-7,8,10,14),PartPose.offset(0,12,1));
		b.addOrReplaceChild("neck",CubeListBuilder.create().texOffs(0,32).addBox(-2.5F,-8,-3,5,8,6),PartPose.offsetAndRotation(0,-4,-5,-.22F,0,0));
		var h=b.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,52).addBox(-3,-5,-4,6,6,7),PartPose.offset(0,-11,-7));
		h.addOrReplaceChild("beak",CubeListBuilder.create().texOffs(96,0).addBox(-1.5F,-1,-5,3,2,5),PartPose.offset(0,-1,-4));
		h.addOrReplaceChild("hook",CubeListBuilder.create().texOffs(96,0).addBox(-1,-1,-1,2,3,2),PartPose.offset(0,-1,-8));
		h.addOrReplaceChild("crest",CubeListBuilder.create().texOffs(64,32).addBox(-.5F,-6,-2,1,6,6),PartPose.offsetAndRotation(0,-4,0,-.35F,0,0));
		for(int sign:new int[]{-1,1}) {
			var w=b.addOrReplaceChild(sign==-1?"wingR":"wingL",CubeListBuilder.create().texOffs(48,0).addBox(-1,-2,-5,2,5,11),PartPose.offset(sign*4,-3,0));
			for(int i=0;i<4;i++) w.addOrReplaceChild("finger"+i,CubeListBuilder.create().texOffs(64,32).addBox(-.5F,0,-1,1,10-i,3),PartPose.offsetAndRotation(sign*.3F,1,-3+i*2,0,0,sign*(.08F+i*.06F)));
			var leg=root.addOrReplaceChild(sign==-1?"legR":"legL",CubeListBuilder.create().texOffs(96,32).addBox(-1,0,-1,2,6,2),PartPose.offset(sign*2.5F,17,1));
			for(int toe=0;toe<3;toe++) leg.addOrReplaceChild("toe"+toe,CubeListBuilder.create().texOffs(96,48).addBox(-.5F,0,-4,1,1,4),PartPose.offset((toe-1)*1.3F,6,-.3F));
		}
		var tail=b.addOrReplaceChild("tail",CubeListBuilder.create(),PartPose.offsetAndRotation(0,-1,7,-.2F,0,0));
		for(int i=0;i<5;i++) tail.addOrReplaceChild("fan"+i,CubeListBuilder.create().texOffs(48,64).addBox(-1,0,0,2,1,10-Math.abs(i-2)),PartPose.offsetAndRotation((i-2)*1.6F,0,0,0,(i-2)*.15F,0));
		return LayerDefinition.create(m,128,128);
	}
	@Override public void setupAnim(BeastRenderState s) {
		super.setupAnim(s);head.yRot=s.yRot*Mth.DEG_TO_RAD*.7F;head.xRot=s.xRot*Mth.DEG_TO_RAD*.5F;
		legR.xRot=Mth.cos(s.walkAnimationPos*.9F)*s.walkAnimationSpeed*.9F;legL.xRot=-legR.xRot;
		wingR.zRot=.08F;wingL.zRot=-.08F;tail.yRot=Mth.sin(s.ageInTicks*.08F)*.06F;
		if(s.pose==BeastRules.WARN) { float t=Mth.clamp(s.elapsed/40,0,1);body.y+=t*2;body.xRot=.25F;head.xRot=-.25F;wingR.zRot=.3F+t*.5F;wingL.zRot=-wingR.zRot;legR.xRot=legL.xRot=-t*.5F; }
		if(s.pose==BeastRules.LEAP) { body.xRot=-.25F;wingR.zRot=1.15F+Mth.sin(s.elapsed*.65F)*.25F;wingL.zRot=-wingR.zRot;legR.xRot=legL.xRot=.75F;tail.xRot=-.3F; }
		if(s.pose==BeastRules.RECOVER) { body.y+=1.5F*(1-Mth.clamp(s.elapsed/50,0,1));head.xRot=.4F;wingR.zRot=.25F;wingL.zRot=-.25F; }
		if(s.deathTime>0) body.xRot=Math.min(1,s.deathTime/20F)*1.3F;
	}
}
