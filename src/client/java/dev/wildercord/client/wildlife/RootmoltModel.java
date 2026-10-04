package dev.wildercord.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Original six-footed cave scavenger: plated chest, breathing gills, shovels and ground feelers. */
public final class RootmoltModel extends EntityModel<WildlifeRenderState> {
 private final ModelPart chest,head,abdomen,leftShovel,rightShovel;private final ModelPart[] legs=new ModelPart[6];
 public RootmoltModel(ModelPart root) {super(root);chest=root.getChild("chest");head=chest.getChild("head");abdomen=chest.getChild("abdomen");leftShovel=head.getChild("left_shovel");rightShovel=head.getChild("right_shovel");for(int i=0;i<6;i++)legs[i]=chest.getChild("leg_"+i);}
 public static LayerDefinition createLayer() {
  var mesh=new MeshDefinition();var r=mesh.getRoot();
  var chest=r.addOrReplaceChild("chest",CubeListBuilder.create().texOffs(0,0).addBox(-5,-4,-6,10,4,12),PartPose.offset(0,18,0));
  chest.addOrReplaceChild("ridge",CubeListBuilder.create().texOffs(48,24).addBox(-6,-1,-7,12,1,14),PartPose.offset(0,-4,0));
  var abdomen=chest.addOrReplaceChild("abdomen",CubeListBuilder.create().texOffs(0,24).addBox(-5,-4,-1,10,5,8),PartPose.offset(0,-1,5));
  for(int side=-1;side<=1;side+=2)for(int k=0;k<3;k++)abdomen.addOrReplaceChild("gill_"+side+"_"+k,CubeListBuilder.create().texOffs(96,0).addBox(-1,-2,-3,2,3,6),PartPose.offsetAndRotation(side*5.3F,-1,k*1.2F,0,0,side*.2F));
  var head=chest.addOrReplaceChild("head",CubeListBuilder.create().texOffs(48,0).addBox(-3,-3,-4,6,4,5),PartPose.offset(0,-1,-5));
  for(int side=-1;side<=1;side+=2) {
   var shovel=head.addOrReplaceChild(side<0?"left_shovel":"right_shovel",CubeListBuilder.create().texOffs(32,48).addBox(-1.5F,0,-1,3,6,2),PartPose.offsetAndRotation(side*3,-1,-2,-.6F,0,-side*.15F));
   shovel.addOrReplaceChild("edge",CubeListBuilder.create().texOffs(64,48).addBox(-2,0,-1,4,1,3),PartPose.offset(0,5,0));
   head.addOrReplaceChild("feeler_"+side,CubeListBuilder.create().texOffs(48,48).addBox(-.5F,-.5F,-5,1,1,5),PartPose.offsetAndRotation(side*2,-1,-3,.15F,side*.2F,0));
   head.addOrReplaceChild("eye_"+side,CubeListBuilder.create().texOffs(82,48).addBox(-.5F,-.5F,-.5F,1,1,1),PartPose.offset(side*2,-2,-4.2F));
  }
  for(int i=0;i<6;i++) {int side=i<3?-1:1,row=i%3;var leg=chest.addOrReplaceChild("leg_"+i,CubeListBuilder.create().texOffs(0,48).addBox(-1,-1,-7,2,2,7),PartPose.offsetAndRotation(side*4,0,row*4-4,-.1F,side*(1.1F+row*.35F),side*.2F));leg.addOrReplaceChild("tibia",CubeListBuilder.create().texOffs(24,48).addBox(-.5F,0,-.5F,1,6,1),PartPose.offsetAndRotation(0,0,-6,.15F,0,side*.25F));}
  return LayerDefinition.create(mesh,128,128);
 }
 @Override public void setupAnim(WildlifeRenderState s) {
  super.setupAnim(s);float t=s.ageInTicks+s.seed;
  chest.y=18-s.watch*1.4F+s.strike*1.2F;chest.xRot=s.strike*.2F-s.graze*.08F;abdomen.yScale=1+Mth.sin(t*.14F)*.055F;abdomen.xRot=Mth.sin(t*.07F)*.035F;
  head.xRot=-s.watch*.35F+s.graze*(.3F+Mth.sin(t*.4F)*.09F);head.yRot=Mth.sin(t*.08F)*.045F*(1-s.strike);
  leftShovel.xRot=-.6F-s.watch*1.15F+s.strike*2;rightShovel.xRot=-.6F-s.watch*.9F+s.strike*1.7F;leftShovel.zRot=.15F+s.watch*.2F;rightShovel.zRot=-.15F-s.watch*.2F;
  for(int side=-1;side<=1;side+=2)head.getChild("feeler_"+side).yRot=side*.2F+Mth.sin(t*.1F+side)*.08F;
  for(int i=0;i<6;i++) {int side=i<3?-1:1,row=i%3;float phase=s.walkAnimationPos*1.1F+row*2.1F+(side<0?0:3.14F);legs[i].yRot=side*(1.1F+row*.35F)+Mth.sin(phase)*s.walkAnimationSpeed*.35F;legs[i].zRot=side*(.2F+Mth.cos(phase)*s.walkAnimationSpeed*.2F)-side*s.watch*.08F;legs[i].getChild("tibia").xRot=.15F+Math.max(0,Mth.sin(phase))*s.walkAnimationSpeed*.35F;}
  for(int side=-1;side<=1;side+=2)for(int k=0;k<3;k++)abdomen.getChild("gill_"+side+"_"+k).zRot=side*(.2F+Mth.sin(t*.14F+k*.5F)*.055F);
 }
}
