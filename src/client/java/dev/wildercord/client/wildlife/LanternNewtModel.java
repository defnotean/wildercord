package dev.wildercord.client.wildlife;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Broad soft head, wet belly, low splayed limbs, frilled gills and articulated paddle tail. UVs: wetland_art.py. */
public final class LanternNewtModel extends EntityModel<WildlifeRenderState> {
	private final ModelPart body,head,tail,tip,leftGill,rightGill,leftEye,rightEye;
	private final ModelPart[] feet=new ModelPart[4];
	public LanternNewtModel(ModelPart root) {
		super(root);body=root.getChild("body");head=root.getChild("head");tail=body.getChild("tail");tip=tail.getChild("tip");
		leftGill=head.getChild("left_gill");rightGill=head.getChild("right_gill");leftEye=head.getChild("eye_1");rightEye=head.getChild("eye_-1");
		for(int i=0;i<4;i++)feet[i]=root.getChild("foot_"+i);
	}
	public static LayerDefinition createLayer() {
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-3,-1.5F,-4,6,3,9),PartPose.offset(0,21,0));
		var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(32,0).addBox(-3.5F,-1.5F,-5,7,3,5),PartPose.offset(0,20.5F,-3));
		var tail=body.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(0,16).addBox(-2,-1,0,4,2,6),PartPose.offset(0,0,4.5F));
		tail.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(22,16).addBox(-1,-1,0,2,2,6),PartPose.offset(0,0,5.5F));
		for(int i=0;i<4;i++)root.addOrReplaceChild("foot_"+i,CubeListBuilder.create().texOffs(42,16).addBox(-1,0,-1,2,1,3),PartPose.offset((i%2==0?1:-1)*3,22.5F,i<2?-2.5F:3.5F));
		for(int s=-1;s<=1;s+=2) {
			var g=head.addOrReplaceChild(s==1?"left_gill":"right_gill",CubeListBuilder.create(),PartPose.offset(s*3.2F,-1,-2));
			for(int i=0;i<3;i++)g.addOrReplaceChild("frond_"+i,CubeListBuilder.create().texOffs(54,16).addBox(-.5F,-3,-.5F,1,3,1),PartPose.offsetAndRotation(s*.6F,0,-i,0,0,s*(.55F+i*.2F)));
			head.addOrReplaceChild("eye_"+s,CubeListBuilder.create().texOffs(70,16).addBox(-.5F,-.5F,-.5F,1,1,1),PartPose.offset(s*2.6F,-1.6F,-4));
		}
		for(int i=0;i<3;i++)body.addOrReplaceChild("lantern_"+i,CubeListBuilder.create().texOffs(60,16).addBox(-1,-2,-1,2,2,2),PartPose.offset(0,-1.1F,i*2));
		return LayerDefinition.create(mesh,128,32);
	}
	@Override public void setupAnim(WildlifeRenderState s) {
		super.setupAnim(s);float t=s.ageInTicks+s.seed,move=Math.min(1,s.walkAnimationSpeed*3)*(1-s.rest),swim=s.air*(1-s.rest);
		body.y=21+Mth.sin(t*.08F)*.1F+s.rest*.15F;body.zRot=Mth.sin(s.walkAnimationPos*.8F)*move*.045F;
		head.yRot=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;
		head.xRot=s.rest*.14F+s.xRot*Mth.DEG_TO_RAD*.4F*(1-s.rest)+s.graze*(.18F+Mth.sin(t*.8F)*.08F);
		tail.yRot=s.rest*.55F+Mth.sin(t*(swim>.5F?.24F:.08F))*(.12F+move*.24F+swim*.13F);
		tip.yRot=s.rest*.4F+Mth.sin(t*(swim>.5F?.24F:.08F)-.9F)*(.18F+move*.28F+swim*.18F);
		for(int i=0;i<4;i++) {feet[i].yRot=Mth.sin(s.walkAnimationPos*1.2F+(i==0 || i==3?0:Mth.PI))*move*.5F;feet[i].zRot=(i%2==0?1:-1)*swim*.35F;}
		leftEye.yScale=rightEye.yScale=1-s.rest*.84F;leftEye.xScale=rightEye.xScale=1+s.rest*.4F;
		leftGill.zRot=Mth.sin(t*.16F)*(.09F-s.rest*.07F)+s.bow*.17F-s.rest*.18F;rightGill.zRot=-leftGill.zRot;
	}
}
