package dev.wildercord.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Hand-built heavy four-footed fired-clay woodland animal, with three ordered vents. */
public final class CinderBailiffModel extends EntityModel<CinderBailiffState> {
 private final ModelPart body,head,tail;private final ModelPart[] legs=new ModelPart[4],vents=new ModelPart[3];
 public CinderBailiffModel(ModelPart root){super(root);body=root.getChild("body");head=body.getChild("head");tail=body.getChild("tail");for(int i=0;i<4;i++)legs[i]=body.getChild("foot_"+i);for(int i=0;i<3;i++)vents[i]=body.getChild("vent_"+i);}
 public static LayerDefinition createLayer(){var mesh=new MeshDefinition();var r=mesh.getRoot();var body=r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-6,-7,-8,12,8,16),PartPose.offset(0,16,0));
  body.addOrReplaceChild("breastplate",CubeListBuilder.create().texOffs(0,28).addBox(-6.5F,-6,-2,13,6,4),PartPose.offset(0,0,-7));
  var head=body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,0).addBox(-4,-4,-6,8,6,8),PartPose.offset(0,-4,-8));head.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(64,18).addBox(-3,-1,-4,6,3,4),PartPose.offset(0,0,-5));
  for(int side=-1;side<=1;side+=2){head.addOrReplaceChild("ear_"+side,CubeListBuilder.create().texOffs(96,18).addBox(-1,-3,-1,2,4,2),PartPose.offsetAndRotation(side*3,-4,-1,0,0,side*.35F));head.addOrReplaceChild("eye_"+side,CubeListBuilder.create().texOffs(112,18).addBox(-.5F,-.5F,-.5F,1,1,1),PartPose.offset(side*4,-2,-3));}
  for(int i=0;i<3;i++){var v=body.addOrReplaceChild("vent_"+i,CubeListBuilder.create().texOffs(0,42).addBox(-4,-1,-2,8,2,4),PartPose.offset(0,-7,-5+i*5));v.addOrReplaceChild("slats",CubeListBuilder.create().texOffs(32,42).addBox(-3.5F,-.5F,-1.5F,7,1,3),PartPose.offset(0,-1,0));}
  for(int i=0;i<4;i++){int side=i%2==0?-1:1;var leg=body.addOrReplaceChild("foot_"+i,CubeListBuilder.create().texOffs(52,42).addBox(-1.5F,0,-2,3,7,4),PartPose.offset(side*4.5F,-1,i<2?-5:5));leg.addOrReplaceChild("toe",CubeListBuilder.create().texOffs(68,42).addBox(-2,0,-3,4,2,5),PartPose.offset(0,6,0));}
  var tail=body.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(88,42).addBox(-2,-1,0,4,3,8),PartPose.offsetAndRotation(0,-3,7,-.3F,0,0));for(int i=0;i<4;i++)tail.addOrReplaceChild("scale_"+i,CubeListBuilder.create().texOffs(0,54).addBox(-3,-.5F,-1,6,1,3),PartPose.offsetAndRotation(0,-1,i*2,.1F*i,0,0));return LayerDefinition.create(mesh,128,128);
 }
 @Override public void setupAnim(CinderBailiffState s){super.setupAnim(s);float t=s.ageInTicks;body.y=16+s.rest*3;body.xRot=s.warning*.08F-s.rest*.05F;head.xRot=-s.warning*.16F+s.rest*.3F;tail.xRot=-.3F+Mth.sin(t*.075F)*.07F;
  for(int i=0;i<4;i++){legs[i].xRot=Mth.cos(s.walkAnimationPos*.65F+(i==0||i==3?0:Mth.PI))*s.walkAnimationSpeed*.48F*(1-s.rest);legs[i].zRot=(i%2==0?1:-1)*s.rest*.5F;}
  for(int i=0;i<3;i++){float selected=s.fan*(s.vent==i?1:.15F);vents[i].xRot=-s.warning*(.35F+i*.08F)+selected*.8F;vents[i].y=-7-Mth.sin(t*.15F+i)*.08F;}
 }
}
