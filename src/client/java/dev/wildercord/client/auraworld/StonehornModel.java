package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.BeastRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Broad rocky shoulders, hanging moss, articulated cloven feet and segmented forward-curving horns. */
public final class StonehornModel extends EntityModel<BeastRenderState> {
	private final ModelPart body,head,fr,fl,br,bl,tail;
	public StonehornModel(ModelPart root) { super(root); body=root.getChild("body");head=body.getChild("head");fr=root.getChild("fr");fl=root.getChild("fl");br=root.getChild("br");bl=root.getChild("bl");tail=body.getChild("tail"); }
	public static LayerDefinition createLayer() {
		var m=new MeshDefinition();var root=m.getRoot();
		var b=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7,-6,-10,14,12,20),PartPose.offset(0,9,1));
		b.addOrReplaceChild("shoulder",CubeListBuilder.create().texOffs(0,36).addBox(-8,-6,-6,16,9,12),PartPose.offset(0,-2,-4));
		for(int s:new int[]{-1,1}) {
			b.addOrReplaceChild("moss"+s,CubeListBuilder.create().texOffs(72,0).addBox(-1,0,-6,2,8,14),PartPose.offset(s*7,0,-1));
			for(int i=0;i<3;i++) b.addOrReplaceChild("plate"+s+"_"+i,CubeListBuilder.create().texOffs(0,36).addBox(-2,-2,-3,4,4,6),PartPose.offsetAndRotation(s*6,-6,-7+i*6,0,0,s*.25F));
		}
		var h=b.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,60).addBox(-4,-5,-7,8,9,9),PartPose.offset(0,-1,-11));
		h.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(42,60).addBox(-3,-2,-5,6,5,5),PartPose.offset(0,2,-6));
		h.addOrReplaceChild("beard",CubeListBuilder.create().texOffs(72,0).addBox(-2,0,-1,4,5,2),PartPose.offset(0,6,-4));
		for(int s:new int[]{-1,1}) {
			h.addOrReplaceChild("ear"+s,CubeListBuilder.create().texOffs(72,32).addBox(-1,-2,-2,5,3,4),PartPose.offsetAndRotation(s*4,-2,-1,0,0,s*.3F));
			var horn=h.addOrReplaceChild("horn"+s,CubeListBuilder.create().texOffs(96,0).addBox(-1.5F,-8,-1.5F,3,8,3),PartPose.offsetAndRotation(s*3,-5,-1,-.4F,0,s*.25F));
			var curve=horn.addOrReplaceChild("curve",CubeListBuilder.create().texOffs(96,0).addBox(-1,-6,-1,2,6,2),PartPose.offsetAndRotation(0,-7,0,-.8F,0,0));
			curve.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(96,32).addBox(-.5F,-5,-.5F,1,5,1),PartPose.offsetAndRotation(0,-5,0,-.5F,0,0));
		}
		for(int s:new int[]{-1,1}) for(int z:new int[]{-1,1}) {
			var leg=root.addOrReplaceChild(z==-1?(s==-1?"fr":"fl"):(s==-1?"br":"bl"),CubeListBuilder.create().texOffs(72,48).addBox(-2,0,-2,4,7,4),PartPose.offset(s*5,15,z*7));
			leg.addOrReplaceChild("hoof",CubeListBuilder.create().texOffs(96,48).addBox(-2.5F,0,-2.5F,5,2,5),PartPose.offset(0,7,0));
		}
		b.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(72,48).addBox(-1,0,0,2,6,2),PartPose.offsetAndRotation(0,-1,10,.5F,0,0));
		return LayerDefinition.create(m,128,128);
	}
	@Override public void setupAnim(BeastRenderState s) {
		super.setupAnim(s); head.yRot=s.yRot*Mth.DEG_TO_RAD*.6F;head.xRot=s.xRot*Mth.DEG_TO_RAD*.5F;
		float walk=Mth.cos(s.walkAnimationPos*.6F)*s.walkAnimationSpeed*.65F;fr.xRot=bl.xRot=walk;fl.xRot=br.xRot=-walk;
		tail.zRot=Mth.sin(s.ageInTicks*.09F)*.1F;body.y+=Mth.sin(s.ageInTicks*.06F)*.06F;
		if(s.pose==BeastRules.FORAGE) { head.xRot=.7F+Mth.sin(s.elapsed*.17F)*.08F;head.y+=1; }
		if(s.pose==BeastRules.REST) {body.y+=3.5F;head.xRot=.35F;head.y+=2;fr.xRot=fl.xRot=-1.15F;br.xRot=bl.xRot=1.1F;tail.zRot=0;}
		if(s.pose==BeastRules.WARN) { float wind=Mth.clamp(s.elapsed/40,0,1);head.xRot=.15F+wind*.4F;body.xRot=.08F;fr.xRot=-Math.abs(Mth.sin(s.elapsed*.35F))*.8F; }
		if(s.pose==BeastRules.CHARGE) { head.xRot=.55F;body.xRot=.12F;head.yRot=0;fr.xRot=bl.xRot=Mth.cos(s.elapsed*1.2F)*.8F;fl.xRot=br.xRot=-fr.xRot; }
		if(s.pose==BeastRules.RECOVER) { head.xRot=-.15F;head.zRot=Mth.sin(s.elapsed*.35F)*.08F;body.y+=.6F; }
		if(s.deathTime>0) body.zRot=Math.min(1,s.deathTime/20F)*1.4F;
	}
}
