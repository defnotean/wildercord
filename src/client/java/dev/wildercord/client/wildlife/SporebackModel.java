package dev.wildercord.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Offset spiral whorls, soft slug foot and articulated antennae. No wolf/vanilla creature mesh. */
public final class SporebackModel extends EntityModel<WildlifeRenderState> {
 private final ModelPart foot,head,shell,left,right;
 public SporebackModel(ModelPart root) {super(root);foot=root.getChild("foot");head=foot.getChild("head");shell=root.getChild("shell");left=head.getChild("left");right=head.getChild("right");}
 public static LayerDefinition createLayer() {
  var mesh=new MeshDefinition();var r=mesh.getRoot();
  var foot=r.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(0,0).addBox(-4,-2,-6,8,2,13),PartPose.offset(0,24,0));
  var head=foot.addOrReplaceChild("head",CubeListBuilder.create().texOffs(44,0).addBox(-2.5F,-2.5F,-4,5,3,5),PartPose.offset(0,-.5F,-4));
  for(int i=-1;i<=1;i+=2) {var eye=head.addOrReplaceChild(i<0?"left":"right",CubeListBuilder.create().texOffs(66,0).addBox(-.5F,-4,-.5F,1,4,1),PartPose.offsetAndRotation(i*1.8F,-2,-2,0,0,i*.3F));eye.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(74,0).addBox(-1,-1,-1,2,2,2),PartPose.offset(0,-4,0));}
  var shell=r.addOrReplaceChild("shell",CubeListBuilder.create().texOffs(0,18).addBox(-4,-8,-3,8,8,8),PartPose.offset(0,22,1));
  shell.addOrReplaceChild("whorl",CubeListBuilder.create().texOffs(36,18).addBox(-3,-6,-1,6,6,6),PartPose.offsetAndRotation(0,-2,-2,0,0,.12F));
  shell.addOrReplaceChild("crown",CubeListBuilder.create().texOffs(64,18).addBox(-2,-4,-1,4,4,4),PartPose.offsetAndRotation(0,-6,-1,0,0,-.15F));
  shell.addOrReplaceChild("dew",CubeListBuilder.create().texOffs(86,18).addBox(-1,-2,-1,2,2,2),PartPose.offset(0,-9,1));
  return LayerDefinition.create(mesh,128,64);
 }
 @Override public void setupAnim(WildlifeRenderState s) {super.setupAnim(s);float t=s.ageInTicks+s.seed;head.z= -4+s.rest*5;head.yScale=1-s.rest*.8F;head.xScale=1-s.rest*.5F;foot.zScale=1-s.rest*.25F;foot.xScale=1+Mth.sin(t*.12F)*Math.min(.035F,s.walkAnimationSpeed*.1F);head.xRot=s.graze*(.18F+Mth.sin(t*.4F)*.07F);left.zRot=-.3F+Mth.sin(t*.06F)*.08F;right.zRot=.3F+Mth.sin(t*.06F+1.5F)*.08F;shell.zRot=Mth.sin(s.walkAnimationPos*.7F)*s.walkAnimationSpeed*.08F;shell.getChild("dew").visible=s.bow>.5F;}
}
