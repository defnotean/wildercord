package dev.wildercord.client.ferryman;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import static dev.wildercord.world.ferryman.FerrymanRules.*;

/** Reed pontoons carry a woven mantle, uneven reed crown, hanging light, oar and iron anchor. */
public final class FerrymanModel extends EntityModel<FerrymanRenderState> {
	private final ModelPart hull, mantle, left, right, crown, lantern, oar, anchor;
	private final ModelPart[] reeds = new ModelPart[9];
	public FerrymanModel(ModelPart root) {
		super(root); hull = root.getChild("hull"); mantle = hull.getChild("mantle");
		left = mantle.getChild("left"); right = mantle.getChild("right"); crown = mantle.getChild("crown");
		lantern = mantle.getChild("lantern"); oar = hull.getChild("oar"); anchor = hull.getChild("anchor");
		for (int i=0;i<9;i++) reeds[i] = crown.getChild("reed"+i);
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
		float t=s.ageInTicks, amp=s.second?1.4F:1, dying=s.deathTime>0?1:0;
		// Idle: the raft heaves on the water, and everything above it lags the hull by its own phase.
		float roll=Mth.sin(t*.06F)*.024F*amp;
		hull.zRot=roll; hull.xRot=Mth.sin(t*.045F)*.014F*amp; hull.y+=Mth.sin(t*.05F)*.5F*amp;
		mantle.zRot=-roll*.6F+Mth.sin(t*.06F-.7F)*.012F; mantle.xRot=Mth.sin(t*.045F-.8F)*.012F+(s.second?-.035F:0); mantle.yRot=Mth.sin(t*.031F)*.03F;
		crown.zRot=Mth.sin(t*.04F-1.1F)*.03F*amp; crown.xRot=Mth.sin(t*.037F+.6F)*.02F; if(s.second) crown.y-=.5F;
		for(int i=0;i<9;i++){
			float a=i*Mth.TWO_PI/9;
			reeds[i].xRot+=Mth.sin(t*.08F+i*.9F)*.05F*amp+Mth.sin(t*.045F-.9F)*.02F*Mth.cos(a);
			reeds[i].zRot+=Mth.sin(t*.07F+i*1.3F)*.05F*amp-Mth.sin(t*.06F-.9F)*.024F*.8F;
		}
		lantern.zRot=Mth.sin(t*.09F-.5F)*.08F*amp-roll; lantern.xRot=Mth.sin(t*.07F+1)*.05F;
		oar.yRot=-.38F+Mth.sin(t*.04F)*.03F; oar.xRot=-.10F+Mth.sin(t*.055F+1)*.025F;
		anchor.zRot=Mth.sin(t*.07F-.6F)*.035F; anchor.xRot=Mth.sin(t*.05F+.4F)*.03F;
		left.yRot=-.2F+Mth.sin(t*.05F+.3F)*.05F; right.yRot=.2F-Mth.sin(t*.05F+.3F)*.05F;
		left.xRot=Mth.sin(t*.07F)*.04F; right.xRot=Mth.sin(t*.07F+.8F)*.04F;

		if(s.move!=IDLE && dying==0) {
			float T=Math.max(1,tell(s.move)), R=Math.max(8,recovery(s.move)), el=Math.max(0,s.elapsed), dt=el-T;
			float wind=smooth(el/T), sk=burst(dt), rec=dt>4?smooth((dt-4)/(R-4)):0, w=1-rec;
			float tw=wind*(1-Mth.clamp(sk,0,1))*w, st=sk*w, jolt=jolt(dt)*w;
			// The last beat of the tell: the whole raft trembles at the top of the pose.
			float quiver=el<T?Mth.sin(el*2.4F)*smooth((el-(T-10))/10)*amp:0;
			float stag=dt>1?(float)Math.exp(-dt*.1F)*Mth.sin(dt*.55F)*w:0;
			float slump=dt>2?smooth((dt-2)/6)*w:0;
			hull.zRot+=quiver*.02F+stag*.04F; hull.yRot+=quiver*.01F;
			if(s.move==SWEEP) {
				// Oar drawn right back over the shoulder, body wound the other way and crouched; then whipped round, the cloak and lantern lagging after.
				oar.yRot+=-1.05F*tw+1.7F*st+stag*.15F; oar.xRot+=-.12F*tw+.1F*st;
				mantle.yRot+=.35F*tw-.45F*st; mantle.xRot+=-.1F*tw+.12F*st+.1F*slump;
				hull.yRot+=-.07F*tw+.12F*st; hull.y+=1.2F*tw; hull.zRot+=.05F*tw-.06F*st;
				left.yRot+=-.4F*tw+.5F*burst(dt-2)*w; right.yRot+=.4F*tw-.5F*burst(dt-2)*w;
				lantern.zRot+=.3F*tw-.55F*burst(dt-3)*w; lantern.xRot-=.2F*burst(dt-3)*w;
				for(int i=0;i<9;i++) reeds[i].zRot+=-.3F*burst(dt-2-i*.4F)*w+.1F*tw;
				crown.yRot+=.2F*tw-.3F*burst(dt-2)*w;
			} else if(s.move==ANCHOR) {
				// Anchor hauled high on its chain, the ferryman leaning back and the flaps lifting; then dropped like a stone and the raft slams down.
				anchor.zRot-=1.5F*tw+quiver*.08F; anchor.y+=-12*tw+3F*st; anchor.xRot+=.3F*tw-.5F*st+stag*.4F;
				oar.xRot-=.22F*tw; mantle.xRot+=-.18F*tw+.2F*jolt+.16F*slump+Mth.sin(dt*.5F)*.03F*slump;
				hull.y+=1.5F*wind*w+4F*jolt; hull.xRot+=.05F*jolt;
				left.yRot+=-.35F*tw; right.yRot+=.35F*tw;
				crown.xRot+=.35F*jolt+.15F*slump;
				for(int i=0;i<9;i++){ float a=i*Mth.TWO_PI/9; reeds[i].xRot+=Mth.cos(a)*.35F*jolt; reeds[i].zRot+=-Mth.sin(a)*.35F*jolt; }
				lantern.xRot+=.5F*jolt;
			} else if(s.move==WAKE) {
				// Flaps flung wide, lantern thrust out, the raft rocking harder and harder; then the cloak slaps shut and the wave is let go.
				hull.zRot+=Mth.sin(el*.5F)*.08F*wind*w; hull.xRot+=Mth.sin(el*.37F)*.03F*wind*w+.1F*st;
				left.yRot+=-1.1F*tw+.5F*st; right.yRot+=1.1F*tw-.5F*st;
				lantern.z+=-4*tw+2F*st; lantern.y+=-3*tw; lantern.xRot+=-.2F*burst(dt-2)*w;
				oar.xRot+=-.45F*tw+.35F*st; mantle.xRot+=.1F*st-.06F*tw;
				for(int i=0;i<9;i++){ float a=i*Mth.TWO_PI/9; reeds[i].xRot+=Mth.cos(a)*.3F*wind*w; reeds[i].zRot+=-Mth.sin(a)*.3F*wind*w; }
				hull.zRot+=Mth.sin(dt*.45F)*.07F*(float)Math.exp(-Math.max(0,dt)*.08F)*(dt>0?1:0)*w;
			}
		}
		if(s.hurt>0 && dying==0) {
			// A blow: the mantle snaps back, the raft dips, the lantern kicks and every reed flares.
			float h=s.hurt;
			mantle.xRot+=-.3F*h; hull.y+=1.2F*h; hull.xRot+=.05F*h; lantern.zRot+=Mth.sin(h*14)*.3F*h;
			anchor.zRot+=Mth.sin(h*12)*.2F*h; left.yRot-=.4F*h; right.yRot+=.4F*h;
			for(int i=0;i<9;i++) reeds[i].xRot+=(i%2==0?1:-1)*.2F*h;
		}
		if(dying>0) {
			// Dying: it rears back and shudders, the mantle folds forward and the reeds droop, then the raft lists and sinks.
			float d=s.deathTime, s1=smooth(d/6), s2=smooth((d-5)/9), s3=smooth((d-10)/10);
			mantle.xRot+=-.35F*s1+1.3F*s2; mantle.zRot+=.25F*s2; crown.xRot+=.6F*s2; crown.zRot+=.3F*s2;
			hull.zRot+=Mth.sin(d*1.6F)*.04F*s1*(1-s2)+.45F*s3; hull.xRot+=.12F*s3; hull.y+=12*s3;
			lantern.zRot+=Mth.sin(d*1.2F)*.4F*s1*(1-s2)-.6F*s2; lantern.y+=4*s3;
			left.yRot+=-.5F*s1+.7F*s2; right.yRot+=.5F*s1-.7F*s2;
			oar.yRot+=.6F*s2; oar.xRot+=.3F*s2;
			anchor.y+=12*s2; anchor.zRot+=.5F*s2; anchor.xRot+=.4F*s2;
			for(int i=0;i<9;i++){ float a=i*Mth.TWO_PI/9; reeds[i].xRot+=Mth.cos(a)*(.4F*s1+.7F*s2); reeds[i].zRot+=-Mth.sin(a)*(.4F*s1+.7F*s2); }
			hull.y+=Mth.sin(d*.9F)*.5F*s3;
		}
	}
	private static float smooth(float x) { x=Mth.clamp(x,0,1); return x*x*(3-2*x); }
	/** 0 until the strike, then a two-tick snap that overshoots by a fifth and settles. */
	private static float burst(float dt) {
		if(dt<=0) return 0;
		float k=Mth.clamp(dt/2,0,1);
		return 1-(1-k)*(1-k)+.2F*(float)Math.exp(-dt*.18F)*Mth.sin(dt*.75F);
	}
	/** An impact that lands in a tick and rings down. */
	private static float jolt(float dt) { return dt>0?Mth.clamp(dt,0,1)*(float)Math.exp(-dt*.2F)*Mth.cos(dt*.55F):0; }
}
