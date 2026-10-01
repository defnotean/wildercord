package dev.wildercord.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** A knotted trunk, leafy crown, branching arms and opening heart: its own articulated creature. */
public final class RootGuardianModel extends EntityModel<DungeonBossRenderState> {
	private final ModelPart trunk, head, left, right, legLeft, legRight, heart, crown;
	private final ModelPart[] bindings = new ModelPart[3];
	public RootGuardianModel(ModelPart root) {
		super(root); trunk = root.getChild("trunk"); head = trunk.getChild("head"); crown = head.getChild("crown");
		left = trunk.getChild("left"); right = trunk.getChild("right"); legLeft = trunk.getChild("leg_left"); legRight = trunk.getChild("leg_right"); heart = trunk.getChild("heart");
		for(int i=0;i<3;i++) bindings[i]=trunk.getChild("binding"+i);
	}
	public static LayerDefinition createLayer() {
		var mesh = new MeshDefinition();
		var trunk = mesh.getRoot().addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0,0).addBox(-6,-14,-5,12,18,10), PartPose.offset(0,8,0));
		var head = trunk.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48,0).addBox(-5,-10,-5,10,10,10), PartPose.offset(0,-14,0));
		head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(48,24).addBox(-8,-4,-7,16,4,14), PartPose.offset(0,-10,0));
		for (int side : new int[] {-1,1}) {
			var arm = trunk.addOrReplaceChild(side < 0 ? "right" : "left", CubeListBuilder.create().texOffs(24,32).addBox(-2.5F,-2,-2.5F,5,18,5), PartPose.offset(side*8,-11,0));
			arm.addOrReplaceChild("branch", CubeListBuilder.create().texOffs(24,32).addBox(-1,0,-1,2,8,2), PartPose.offsetAndRotation(side*2,4,0,0,0,-side*.7F));
			trunk.addOrReplaceChild(side < 0 ? "leg_right" : "leg_left", CubeListBuilder.create().texOffs(0,32).addBox(-2.5F,0,-3,5,12,6), PartPose.offset(side*3.5F,4,0));
		}
		trunk.addOrReplaceChild("heart", CubeListBuilder.create().texOffs(96,0).addBox(-3,-4,-1,6,8,1), PartPose.offset(0,-6,-5));
		for(int i=0;i<3;i++) trunk.addOrReplaceChild("binding"+i,CubeListBuilder.create().texOffs(48,24).addBox(-7,-1,-.5F,14,2,1),PartPose.offsetAndRotation(0,-9+i*3,-6,0,0,(i-1)*.15F));
		return LayerDefinition.create(mesh,128,64);
	}
	@Override public void setupAnim(DungeonBossRenderState s) {
		super.setupAnim(s);
		for(int i=0;i<3;i++) bindings[i].visible=i<s.rootBindings;
		head.yRot = s.yRot * Mth.DEG_TO_RAD; head.xRot = s.xRot * Mth.DEG_TO_RAD;
		float sway = Mth.sin(s.ageInTicks*.045F)*.08F;
		left.zRot = -.12F-sway-s.casting*.7F; right.zRot = .12F+sway+s.casting*.7F;
		left.xRot = -s.casting*.65F; right.xRot = -s.casting*.65F;
		legLeft.xRot = Mth.cos(s.walkAnimationPos*.6F)*s.walkAnimationSpeed*.65F;
		legRight.xRot = -legLeft.xRot;
		crown.zRot = sway; heart.z = -5-s.exposed*.8F; heart.xScale = heart.yScale = 1+s.exposed*(.08F+.04F*Mth.sin(s.ageInTicks*.3F));
		trunk.xRot = s.deathTime > 0 ? Math.min(1,s.deathTime/60F)*.8F : s.shifting*.15F;
	}
}
