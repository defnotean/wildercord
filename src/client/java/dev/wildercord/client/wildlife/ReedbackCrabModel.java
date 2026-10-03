package dev.wildercord.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Six segmented walking legs, independently hinged claws, eyestalks and a living reed crown. */
public final class ReedbackCrabModel extends EntityModel<WildlifeRenderState> {
 private final ModelPart shell,left,right,leftTip,rightTip,leftEye,rightEye;private final ModelPart[] legs=new ModelPart[6],reeds=new ModelPart[5];
 public ReedbackCrabModel(ModelPart r) {super(r);shell=r.getChild("shell");left=r.getChild("left_arm");right=r.getChild("right_arm");leftTip=left.getChild("claw");rightTip=right.getChild("claw");leftEye=shell.getChild("eye_left");rightEye=shell.getChild("eye_right");for(int i=0;i<6;i++)legs[i]=r.getChild("leg_"+i);for(int i=0;i<5;i++)reeds[i]=shell.getChild("reed_"+i);}
 public static LayerDefinition createLayer() {
  var root=new MeshDefinition();var r=root.getRoot();var shell=r.addOrReplaceChild("shell",CubeListBuilder.create().texOffs(0,0).addBox(-6,-5,-5,12,5,10).texOffs(0,20).addBox(-7,-1,-6,14,2,12),PartPose.offset(0,19,0));
  for(int i=0;i<5;i++)shell.addOrReplaceChild("reed_"+i,CubeListBuilder.create().texOffs(22,40).addBox(-.5F,-7,-.5F,1,7,1).texOffs(30,40).addBox(-1,-8,-1,2,3,2),PartPose.offsetAndRotation(-4+i*2,-4,(i%2)*3-1,.12F*(i%2),0,(i-2)*.12F));
  for(int side=-1;side<=1;side+=2) {
   shell.addOrReplaceChild(side==1?"eye_left":"eye_right",CubeListBuilder.create().texOffs(60,0).addBox(-.5F,-3,-.5F,1,3,1).texOffs(66,0).addBox(-1,-3.5F,-1,2,1,2),PartPose.offset(side*3,-3,-4.5F));
   var arm=r.addOrReplaceChild(side==1?"left_arm":"right_arm",CubeListBuilder.create().texOffs(54,28).addBox(-1,-1,-5,2,2,6),PartPose.offsetAndRotation(side*5,20,-4,0,side*.5F,0));
   var claw=arm.addOrReplaceChild("claw",CubeListBuilder.create().texOffs(64,12).addBox(-2.5F,-1.5F,-5,5,3,7),PartPose.offset(0,0,-5));
   claw.addOrReplaceChild("pincer",CubeListBuilder.create().texOffs(92,12).addBox(-1,-1,-5,2,2,5),PartPose.offsetAndRotation(side*1.5F,0,-3,0,side*.35F,0));
  }
  for(int i=0;i<6;i++) {int side=i%2==0?1:-1;var leg=r.addOrReplaceChild("leg_"+i,CubeListBuilder.create().texOffs(0,38).addBox(-1,-1,0,2,2,7),PartPose.offsetAndRotation(side*5,21,-3+(i/2)*3,0,side*(1.4F+(i/2-1)*.35F),side*.1F));leg.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(0,38).addBox(-1,-1,0,2,2,7),PartPose.offsetAndRotation(0,0,6,.5F,0,0));}
  return LayerDefinition.create(root,128,64);
 }
 @Override public void setupAnim(WildlifeRenderState s) {
  super.setupAnim(s);float t=s.ageInTicks+s.seed,m=Math.min(1,s.walkAnimationSpeed*4)*(1-s.settle);
  shell.y+=s.settle*.7F-Mth.sin(t*.06F)*.12F;left.xRot=right.xRot=-s.claws*.8F+s.strike*.35F;left.yRot=.5F-s.strike*1.15F;right.yRot=-.5F+s.strike*1.15F;
  leftTip.zRot=s.claws*.4F;rightTip.zRot=-s.claws*.4F;leftEye.xRot=rightEye.xRot=s.settle*.6F;
  for(int i=0;i<6;i++)legs[i].xRot+=Mth.sin(s.walkAnimationPos*1.5F+i*Mth.PI*.7F)*m*.35F;
  for(int i=0;i<5;i++)reeds[i].zRot+=Mth.sin(t*.09F+i)*(.045F+s.claws*.12F);
 }
}
