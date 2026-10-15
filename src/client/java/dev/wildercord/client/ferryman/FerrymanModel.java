package dev.wildercord.client.ferryman;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import static dev.wildercord.world.ferryman.FerrymanRules.*;

/** Reed pontoons carry a woven mantle, uneven reed crown, hanging light, oar and iron anchor. */
public final class FerrymanModel extends EntityModel<FerrymanRenderState> {
	private final ModelPart hull, mantle, left, right, crown, lantern, oar, anchor;
	public FerrymanModel(ModelPart root) {
		super(root); hull = root.getChild("hull"); mantle = hull.getChild("mantle");
		left = mantle.getChild("left"); right = mantle.getChild("right"); crown = mantle.getChild("crown");
		lantern = mantle.getChild("lantern"); oar = hull.getChild("oar"); anchor = hull.getChild("anchor");
	}
	public static LayerDefinition createLayer() {
		var mesh = new MeshDefinition(); var root = mesh.getRoot();
		var hull = root.addOrReplaceChild("hull", CubeListBuilder.create().texOffs(0,0).addBox(-18,-4,-17,36,4,34), PartPose.offset(0,23,0));
		for (int side : new int[] {-1,1}) {
			hull.addOrReplaceChild("pontoon" + side, CubeListBuilder.create().texOffs(0,128).addBox(-4,-3,-21,8,6,42), PartPose.offset(side*17,0,0));
			for (int z : new int[] {-13,0,13}) hull.addOrReplaceChild("rope"+side+"_"+z,
				CubeListBuilder.create().texOffs(128,256).addBox(-4.4F,-3.4F,-1,8.8F,6.8F,2), PartPose.offset(side*17,0,z));
		}
		var mantle = hull.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0,256).addBox(-6,-23,-5,12,23,10), PartPose.offset(0,-4,0));
		mantle.addOrReplaceChild("left", CubeListBuilder.create().texOffs(44,256).addBox(0,0,-1,9,19,2), PartPose.offset(0,-20,-5.5F));
		mantle.addOrReplaceChild("right", CubeListBuilder.create().texOffs(44,256).addBox(-9,0,-1,9,19,2), PartPose.offset(0,-20,-5.5F));
		var crown = mantle.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0,128).addBox(-7,-3,-6,14,3,12), PartPose.offset(0,-23,0));
		for (int i=0;i<9;i++) {
			double a = i*Math.PI*2/9; float height=7+(i*5%7);
			crown.addOrReplaceChild("reed"+i, CubeListBuilder.create().texOffs(80,128).addBox(-.7F,-height,-.7F,1.4F,height,1.4F),
				PartPose.offsetAndRotation((float)Math.sin(a)*6,-2,(float)Math.cos(a)*5,(float)Math.cos(a)*.13F,0,-(float)Math.sin(a)*.13F));
		}
		mantle.addOrReplaceChild("lantern", CubeListBuilder.create().texOffs(256,0).addBox(-3,3,-3,6,8,6)
			.texOffs(128,256).addBox(-.5F,0,-.5F,1,3,1).addBox(-4,2,-4,8,1,8).addBox(-4,11,-4,8,1,8), PartPose.offset(0,-18,-8));
		hull.addOrReplaceChild("oar", CubeListBuilder.create().texOffs(0,0).addBox(-1,-1,-67,2,2,75)
			.texOffs(0,128).addBox(-4,-1.5F,-85,8,3,18), PartPose.offset(12,-15,-2));
		var anchor = hull.addOrReplaceChild("anchor", CubeListBuilder.create().texOffs(128,256).addBox(-.7F,0,-.7F,1.4F,12,1.4F)
			.addBox(-1,12,-1,2,9,2).addBox(-5,14,-1,10,2,2).addBox(-6,20,-1,12,2,2), PartPose.offset(-12,-18,-5));
		for(int side:new int[]{-1,1}) anchor.addOrReplaceChild("fluke"+side, CubeListBuilder.create().texOffs(128,256).addBox(-1,-5,-1.5F,2,6,3), PartPose.offsetAndRotation(side*5,20,0,0,0,side*.5F));
		return LayerDefinition.create(mesh,512,512);
	}
	@Override public void setupAnim(FerrymanRenderState s) {
		super.setupAnim(s);
		float t=s.ageInTicks; hull.zRot=Mth.sin(t*.06F)*.024F; hull.xRot=Mth.sin(t*.045F)*.014F;
		mantle.zRot=-hull.zRot*.6F; crown.zRot=Mth.sin(t*.04F)*.025F;
		lantern.zRot=Mth.sin(t*.09F)*.08F-hull.zRot; oar.yRot=-.38F; oar.xRot=-.10F;
		anchor.zRot=Mth.sin(t*.07F)*.035F; left.yRot=-.2F; right.yRot=.2F;
		float tell=Math.max(1,tell(s.move)), wind=smooth(s.elapsed/tell);
		float strike=smooth((s.elapsed-tell)/6), recover=smooth((s.elapsed-tell-6)/Math.max(1,recovery(s.move)-6));
		float held=wind*(1-recover);
		if(s.move==SWEEP) { oar.yRot+=(-1.05F+2.75F*strike)*held; oar.xRot-=.12F*held; mantle.yRot+=.2F*held*(1-2*strike); }
		if(s.move==ANCHOR) { anchor.zRot-=1.5F*held*(1-strike); anchor.y-=12*held*(1-strike); oar.xRot-=.22F*held; }
		if(s.move==WAKE) { left.yRot-=1.1F*held; right.yRot+=1.1F*held; lantern.z-=4*held; oar.xRot-=.45F*held; }
		if(s.second) { crown.y-=.5F; mantle.xRot=-.035F; }
		if(s.deathTime>0) { hull.y+=Math.min(10,s.deathTime*.5F); mantle.xRot+=Math.min(.8F,s.deathTime*.05F); }
	}
	private static float smooth(float x) { x=Mth.clamp(x,0,1); return x*x*(3-2*x); }
}
