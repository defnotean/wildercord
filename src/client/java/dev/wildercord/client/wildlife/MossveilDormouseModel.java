package dev.wildercord.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
/** Original compact rodent: full belly, hinged ear cups, nose/whisker fans and a curling segmented tail. */
public final class MossveilDormouseModel extends EntityModel<MossveilDormouseState>{
 private final ModelPart body,head,nose;private final ModelPart[] feet=new ModelPart[4],tail=new ModelPart[3],whiskers=new ModelPart[2],ears=new ModelPart[2];
 public MossveilDormouseModel(ModelPart root){super(root);body=root.getChild("belly");head=body.getChild("head");nose=head.getChild("nose");for(int i=0;i<4;i++)feet[i]=body.getChild("paw_"+i);tail[0]=body.getChild("tail_base");tail[1]=tail[0].getChild("tail_fold");tail[2]=tail[1].getChild("tail_tip");for(int i=0;i<2;i++){whiskers[i]=head.getChild("whisker_"+i);ears[i]=head.getChild("ear_"+i);}}
 public static LayerDefinition createLayer(){
  var mesh=new MeshDefinition();var root=mesh.getRoot();
  var body=root.addOrReplaceChild("belly",CubeListBuilder.create().texOffs(0,0).addBox(-4,-5,-5,8,6,10),PartPose.offset(0,21,0));
  body.addOrReplaceChild("breast",CubeListBuilder.create().texOffs(0,20).addBox(-3,-3,-1,6,4,3),PartPose.offset(0,-1,-5));
  var head=body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(40,0).addBox(-3,-3,-4,6,5,6),PartPose.offset(0,-3,-5));
  var nose=head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(40,14).addBox(-1.5F,-1,-2,3,2,2),PartPose.offset(0,0,-4));
  nose.addOrReplaceChild("pink_nose",CubeListBuilder.create().texOffs(54,14).addBox(-.5F,-.5F,-.5F,1,1,1),PartPose.offset(0,-.1F,-2));
  for(int i=0;i<2;i++){int side=i==0?-1:1;
   var ear=head.addOrReplaceChild("ear_"+i,CubeListBuilder.create().texOffs(64,0).addBox(-1.5F,-2,-.5F,3,3,1),PartPose.offsetAndRotation(side*2.5F,-3,0,-.15F,0,side*.25F));
   ear.addOrReplaceChild("ear_inner",CubeListBuilder.create().texOffs(74,0).addBox(-1,-1.5F,-.2F,2,2,1),PartPose.offset(0,0,-.5F));
   head.addOrReplaceChild("eye_"+i,CubeListBuilder.create().texOffs(84,0).addBox(-.5F,-.5F,-.5F,1,1,1),PartPose.offset(side*3.05F,-1.2F,-2.6F));
   var w=head.addOrReplaceChild("whisker_"+i,CubeListBuilder.create(),PartPose.offset(side*1.8F,.3F,-4.8F));
   for(int j=0;j<3;j++)w.addOrReplaceChild("strand_"+j,CubeListBuilder.create().texOffs(0,32).addBox(side<0?-4:0,0,0,4,.15F,.15F),PartPose.offsetAndRotation(0,j*.28F,0,0,side*(j-1)*.17F,side*.05F));
  }
  for(int i=0;i<4;i++){int side=i%2==0?-1:1;var paw=body.addOrReplaceChild("paw_"+i,CubeListBuilder.create().texOffs(20,20).addBox(-1,0,-1.5F,2,2,3),PartPose.offset(side*2.8F,1,i<2?-3:3));paw.addOrReplaceChild("toes",CubeListBuilder.create().texOffs(32,20).addBox(-1,0,-1,2,1,2),PartPose.offset(0,1,-1.4F));}
  var tb=body.addOrReplaceChild("tail_base",CubeListBuilder.create().texOffs(40,22).addBox(-1,-1,0,2,2,5),PartPose.offsetAndRotation(0,-1,4,-.1F,0,0));
  var tf=tb.addOrReplaceChild("tail_fold",CubeListBuilder.create().texOffs(56,22).addBox(-1,-1,0,2,2,4),PartPose.offsetAndRotation(0,0,5,.15F,0,0));
  tf.addOrReplaceChild("tail_tip",CubeListBuilder.create().texOffs(70,22).addBox(-.5F,-.5F,0,1,1,3),PartPose.offsetAndRotation(0,0,4,.3F,0,0));
  body.addOrReplaceChild("moss_tuft",CubeListBuilder.create().texOffs(0,38).addBox(-2,-1,-1,4,1,3),PartPose.offset(0,-5,2));
  return LayerDefinition.create(mesh,96,64);
 }
 @Override public void setupAnim(MossveilDormouseState s){super.setupAnim(s);float t=s.ageInTicks,c=s.curl;body.y=21+c*.45F;body.zRot=c*.15F;body.xRot=-c*.1F+Mth.sin(t*.085F)*.015F;head.xRot=c*.65F+s.sniff*Mth.sin(t*.9F)*.09F;head.yRot=Mth.sin(t*.045F)*.1F*(1-c);nose.z=-4+Mth.sin(t*1.2F)*s.sniff*.18F;
  for(int i=0;i<4;i++){feet[i].xRot=Mth.cos(s.walkAnimationPos*1.4F+(i==0||i==3?0:Mth.PI))*s.walkAnimationSpeed*.5F*(1-c);feet[i].zRot=(i%2==0?1:-1)*c*.6F;}
  tail[0].yRot=c*1.2F+Mth.sin(t*.08F)*.06F;tail[1].yRot=c*1.35F;tail[2].yRot=c*1.2F;
  for(int i=0;i<2;i++){int side=i==0?-1:1;whiskers[i].yRot=side*s.sniff*Mth.sin(t*.7F)*.12F;ears[i].zRot=side*(.25F+c*.2F+Mth.sin(t*.15F+i)*s.sniff*.08F);}
 }
}
