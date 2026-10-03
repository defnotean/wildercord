package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.SwordTombRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** A carved, blindfolded burial effigy with segmented stone arms, a long brass-bound blade and hanging cloth. */
public final class GravekeeperModel extends EntityModel<GravekeeperRenderState> {
	private final ModelPart body,head,right,left,legR,legL,cloth,blade;
	public GravekeeperModel(ModelPart root){super(root);body=root.getChild("body");head=body.getChild("head");right=body.getChild("right");left=body.getChild("left");legR=body.getChild("legR");legL=body.getChild("legL");cloth=body.getChild("cloth");blade=right.getChild("blade");}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-6,-14,-4,12,20,8),PartPose.offset(0,4,0));
		body.addOrReplaceChild("collar",CubeListBuilder.create().texOffs(40,0).addBox(-8,-2,-5,16,3,10),PartPose.offset(0,-13,0));
		var head=body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,32).addBox(-6,-10,-5,12,10,10),PartPose.offset(0,-14,0));
		head.addOrReplaceChild("blindfold",CubeListBuilder.create().texOffs(48,34).addBox(-6.5F,-7,-5.5F,13,3,11),PartPose.ZERO);
		head.addOrReplaceChild("brow",CubeListBuilder.create().texOffs(40,0).addBox(-7,-1,-6,14,2,12),PartPose.offset(0,-10,0));
		for(int sign:new int[]{-1,1}){
			var arm=body.addOrReplaceChild(sign==-1?"right":"left",CubeListBuilder.create().texOffs(72,0).addBox(-3,-2,-3,6,17,6),PartPose.offset(sign*9,-10,0));
			arm.addOrReplaceChild("cuff",CubeListBuilder.create().texOffs(40,0).addBox(-3.5F,-1,-3.5F,7,3,7),PartPose.offset(0,12,0));
			body.addOrReplaceChild(sign==-1?"legR":"legL",CubeListBuilder.create().texOffs(96,0).addBox(-3,0,-3,6,14,6),PartPose.offset(sign*3.5F,6,0));
		}
		var arm=body.getChild("right");var sword=arm.addOrReplaceChild("blade",CubeListBuilder.create().texOffs(104,32).addBox(-1.5F,1,-1,3,24,2),PartPose.offset(0,13,-1));
		sword.addOrReplaceChild("guard",CubeListBuilder.create().texOffs(40,0).addBox(-6,-1,-2,12,2,4),PartPose.ZERO);
		sword.addOrReplaceChild("grip",CubeListBuilder.create().texOffs(48,34).addBox(-1,-6,-1,2,5,2),PartPose.ZERO);
		body.addOrReplaceChild("cloth",CubeListBuilder.create().texOffs(64,48).addBox(-5,0,-.2F,10,13,0),PartPose.offset(0,4,-4.4F));
		body.addOrReplaceChild("crest",CubeListBuilder.create().texOffs(40,0).addBox(-3,-3,-.5F,6,6,1),PartPose.offset(0,-6,-4.5F));
		return LayerDefinition.create(mesh,128,64);
	}
	@Override public void setupAnim(GravekeeperRenderState s){
		super.setupAnim(s);head.yRot=s.yRot*Mth.DEG_TO_RAD*.65F;head.xRot=.05F+s.xRot*Mth.DEG_TO_RAD*.4F;
		legL.xRot=Mth.cos(s.walkAnimationPos*.55F)*s.walkAnimationSpeed*.5F;legR.xRot=-legL.xRot;
		right.xRot=-.85F;right.zRot=.08F;left.xRot=-.2F;left.zRot=-.08F;
		cloth.xRot=.08F+Math.abs(legL.xRot)*.45F+Mth.sin(s.ageInTicks*.05F)*.025F;
		float wind=Mth.clamp(s.elapsed/SwordTombRules.WINDUP,0,1);
		if(s.move==SwordTombRules.SWEEP){right.xRot=-1.2F-wind*.35F;right.yRot=-.9F-wind*.55F;body.yRot=-wind*.3F;left.xRot=-.45F;}
		else if(s.move==SwordTombRules.THRUST){right.xRot=-1.55F;right.yRot=-.18F;right.z=-wind*1.6F;body.xRot=-wind*.15F;left.xRot=-1.1F;}
		else if(s.move==SwordTombRules.GUARD){right.xRot=-1.3F;right.yRot=-.8F;right.zRot=-.6F;left.xRot=-1.35F;left.yRot=.55F;blade.zRot=.8F;}
		else if(s.move==SwordTombRules.RECOVER){float swing=1-Mth.clamp(s.elapsed/SwordTombRules.RECOVER_TICKS,0,1);right.xRot=-1.3F+swing*.7F;right.yRot=swing*1.2F;body.xRot=swing*.18F;}
		else if(s.move==SwordTombRules.BROKEN){body.xRot=-.3F;head.xRot=-.4F;right.xRot=.2F;left.zRot=-.8F;body.zRot=Mth.sin(s.elapsed*.35F)*.045F;}
		if(s.deathTime>0){body.xRot=Math.min(1,s.deathTime/20F)*1.2F;right.xRot=.3F;left.xRot=.3F;}
	}
}
