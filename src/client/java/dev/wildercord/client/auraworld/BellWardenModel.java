package dev.wildercord.client.auraworld;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * A hunched drowned giant wearing a cracked church bell over its head and shoulders, eyes burning in the crack; long arms, one
 * wrapped in the bell's old chain, the other gripping its iron clapper like a mace; kelp trailing from the bell's lip. It kneels
 * bowed with its fists on the floor at its vigil, and the rise straightens it up off its knees.
 */
public final class BellWardenModel extends EntityModel<BellWardenRenderState> {
	/** Hip height standing, in model units above the feet's 24, and how far the hips sink kneeling. */
	private static final float HIPS=12,KNEEL_DROP=9.5F,HUNCH=.32F;
	private final ModelPart hips,body,bell,right,left,legR,legL,clapper,kelpL,kelpR,kelpB;
	public BellWardenModel(ModelPart root){
		super(root);hips=root.getChild("hips");body=hips.getChild("body");bell=body.getChild("bell");right=body.getChild("right");left=body.getChild("left");
		legR=hips.getChild("legR");legL=hips.getChild("legL");clapper=left.getChild("clapper");kelpL=bell.getChild("kelpL");kelpR=bell.getChild("kelpR");kelpB=bell.getChild("kelpB");
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var hips=root.addOrReplaceChild("hips",CubeListBuilder.create(),PartPose.offset(0,HIPS,0));
		var body=hips.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7,-12,-4.5F,14,12,9),PartPose.ZERO);
		var bell=body.addOrReplaceChild("bell",CubeListBuilder.create().texOffs(0,30).addBox(-8,-10,-8,16,13,16),PartPose.offset(0,-12,0));
		bell.addOrReplaceChild("crown",CubeListBuilder.create().texOffs(64,30).addBox(-5,-13,-5,10,3,10),PartPose.ZERO);
		bell.addOrReplaceChild("canon",CubeListBuilder.create().texOffs(104,30).addBox(-2,-16,-1,4,3,2),PartPose.ZERO);
		bell.addOrReplaceChild("lip",CubeListBuilder.create().texOffs(0,60).addBox(-9,1,-9,18,2,18),PartPose.ZERO);
		bell.addOrReplaceChild("kelpL",CubeListBuilder.create().texOffs(112,36).addBox(-2,0,0,4,12,0),PartPose.offset(-5,3,-9.2F));
		bell.addOrReplaceChild("kelpR",CubeListBuilder.create().texOffs(112,36).addBox(-2,0,0,4,12,0),PartPose.offset(6,3,-9.2F));
		bell.addOrReplaceChild("kelpB",CubeListBuilder.create().texOffs(112,36).addBox(-2,0,0,4,12,0),PartPose.offset(1,3,9.2F));
		for(int sign:new int[]{-1,1}){
			var arm=body.addOrReplaceChild(sign==-1?"right":"left",CubeListBuilder.create().texOffs(48,0).addBox(-3,-2,-3,6,18,6),PartPose.offset(sign*9.5F,-10,0));
			arm.addOrReplaceChild("fist",CubeListBuilder.create().texOffs(72,0).addBox(-3.5F,14,-3.5F,7,6,7),PartPose.ZERO);
			hips.addOrReplaceChild(sign==-1?"legR":"legL",CubeListBuilder.create().texOffs(100,0).addBox(-3.5F,0,-3.5F,7,12,7),PartPose.offset(sign*4,0,0));
		}
		body.getChild("right").addOrReplaceChild("chain",CubeListBuilder.create().texOffs(72,14).addBox(-3.5F,5,-3.5F,7,4,7),PartPose.ZERO);
		var clapper=body.getChild("left").addOrReplaceChild("clapper",CubeListBuilder.create().texOffs(72,60).addBox(-1,0,-1,2,6,2),PartPose.offset(0,17,0));
		clapper.addOrReplaceChild("ball",CubeListBuilder.create().texOffs(80,60).addBox(-3.5F,6,-3.5F,7,8,7),PartPose.ZERO);
		return LayerDefinition.create(mesh,128,128);
	}
	@Override public void setupAnim(BellWardenRenderState s){
		super.setupAnim(s);
		float r=s.rise,down=1-r,t=s.ageInTicks;
		// Kneeling: hips sunk, legs folded back under it, bowed over its fists. The rise straightens all of it.
		hips.y=HIPS+KNEEL_DROP*down;
		legL.xRot=legR.xRot=1.45F*down;
		body.xRot=HUNCH+.6F*down+Mth.sin(t*.05F)*.015F;
		// Arms hang straight down standing; kneeling they reach forward to plant its fists on the floor.
		right.xRot=left.xRot=-body.xRot-1.0F*down;
		right.zRot=.08F;left.zRot=-.08F;
		clapper.xRot=-1.25F+.9F*down;
		// The bell follows its gaze a little, and settles on its shoulders with each breath.
		bell.yRot=s.yRot*Mth.DEG_TO_RAD*.45F*r;bell.xRot=s.xRot*Mth.DEG_TO_RAD*.3F*r-.1F*down;
		bell.y=-12+Mth.sin(t*.05F)*.2F;
		// Walking: a heavy, rolling tread, arms swinging against the legs.
		float walk=s.walkAnimationSpeed*r,step=s.walkAnimationPos*.5F;
		legL.xRot+=Mth.cos(step)*walk*.6F;legR.xRot-=Mth.cos(step)*walk*.6F;
		right.xRot+=Mth.cos(step)*walk*.35F;left.xRot-=Mth.cos(step)*walk*.35F;
		body.zRot=Mth.sin(step)*walk*.06F;
		// Kelp drifts as if still under water.
		kelpL.xRot=-.08F+Mth.sin(t*.07F)*.08F+walk*.2F;kelpR.xRot=-.08F+Mth.sin(t*.07F+1.7F)*.08F+walk*.2F;kelpB.xRot=.08F-Mth.sin(t*.06F+.6F)*.08F-walk*.2F;
		if(s.deathTime>0){float d=Math.min(1,s.deathTime/20F);body.xRot+=d*.6F;right.xRot+=d*.4F;left.xRot+=d*.4F;}
	}
}
