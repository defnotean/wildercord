package dev.wildercord.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Copper gyroscope with six articulated lightning terminals and a levitating blue coil. */
public final class StormConductorModel extends EntityModel<DungeonBossRenderState> {
	private final ModelPart body, core, cage;
	private final ModelPart[] rods = new ModelPart[6];
	public StormConductorModel(ModelPart root) {
		super(root); body=root.getChild("body"); core=body.getChild("core"); cage=body.getChild("cage");
		for(int i=0;i<6;i++) rods[i]=cage.getChild("rod"+i);
	}
	public static LayerDefinition createLayer() {
		var mesh=new MeshDefinition();
		var body=mesh.getRoot().addOrReplaceChild("body",CubeListBuilder.create(),PartPose.offset(0,4,0));
		body.addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),PartPose.ZERO);
		var cage=body.addOrReplaceChild("cage",CubeListBuilder.create().texOffs(32,0).addBox(-5,-5,-5,10,10,10),PartPose.ZERO);
		for(int i=0;i<6;i++) {
			float angle=i*Mth.TWO_PI/6;
			var arm=cage.addOrReplaceChild("rod"+i,CubeListBuilder.create().texOffs(0,24).addBox(-1,-1,0,2,2,18),PartPose.offsetAndRotation(0,0,0,0,angle,0));
			arm.addOrReplaceChild("terminal",CubeListBuilder.create().texOffs(48,24).addBox(-1,-6,-1,2,12,2),PartPose.offset(0,0,17));
			arm.addOrReplaceChild("cap",CubeListBuilder.create().texOffs(64,24).addBox(-3,-2,-3,6,4,6),PartPose.offset(0,-7,17));
		}
		return LayerDefinition.create(mesh,128,64);
	}
	@Override public void setupAnim(DungeonBossRenderState s) {
		super.setupAnim(s);
		body.y=4+Mth.sin(s.ageInTicks*.07F)*1.2F+s.exposed*4;
		core.yRot=s.ageInTicks*.035F; core.xRot=s.ageInTicks*.016F;
		cage.yRot=s.ageInTicks*(.008F+.025F*s.casting); cage.zRot=Mth.sin(s.ageInTicks*.025F)*.14F+s.shifting*.5F;
		for(int i=0;i<6;i++) rods[i].xRot=(i%2==0?1:-1)*(.15F+s.casting*.45F-s.exposed*.3F);
		if(s.deathTime>0) { body.y+=s.deathTime*.18F; cage.xRot=s.deathTime*.025F; }
	}
}
