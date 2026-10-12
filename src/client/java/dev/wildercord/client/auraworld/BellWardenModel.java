package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.BellWardenRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * A hunched drowned giant wearing a cracked church bell over its head and shoulders, eyes burning in the crack; long arms, one
 * wrapped in the bell's old chain, the other gripping its iron clapper like a mace; kelp trailing from the bell's lip. It keeps its
 * vigil sat back on its heels, bowed over its fists on the floor; the rise lifts its bell, plants one foot, and pushes up off it.
 * Elbows and knees bend, each move blends into the next, a blow jolts it and rattles the bell on its shoulders, and dying it sinks
 * back onto its knees and topples forward under its bell.
 */
public final class BellWardenModel extends EntityModel<BellWardenRenderState> {
	/** Hip height standing, in model units above the feet's 24, and the hunch it stands with. */
	private static final float HIPS=12,HUNCH=.32F;
	private final ModelPart hips,body,bell,right,left,foreR,foreL,legR,legL,shinR,shinL,clapper,kelpL,kelpR,kelpB;
	private final ModelPart[] posed;
	public BellWardenModel(ModelPart root){
		super(root);hips=root.getChild("hips");body=hips.getChild("body");bell=body.getChild("bell");right=body.getChild("right");left=body.getChild("left");
		foreR=right.getChild("fore");foreL=left.getChild("fore");legR=hips.getChild("legR");legL=hips.getChild("legL");shinR=legR.getChild("shin");shinL=legL.getChild("shin");
		clapper=foreL.getChild("clapper");kelpL=bell.getChild("kelpL");kelpR=bell.getChild("kelpR");kelpB=bell.getChild("kelpB");
		posed=new ModelPart[]{hips,body,bell,right,left,foreR,foreL,legR,legL,shinR,shinL,clapper,kelpL,kelpR,kelpB};
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var hips=root.addOrReplaceChild("hips",CubeListBuilder.create(),PartPose.offset(0,HIPS,0));
		var body=hips.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7,-12,-4.5F,14,12,9),PartPose.ZERO);
		var bell=body.addOrReplaceChild("bell",CubeListBuilder.create().texOffs(0,30).addBox(-8,-10,-8,16,13,16),PartPose.offset(0,-12,0));
		bell.addOrReplaceChild("crown",CubeListBuilder.create().texOffs(64,30).addBox(-5,-13,-5,10,3,10),PartPose.ZERO);
		bell.addOrReplaceChild("canon",CubeListBuilder.create().texOffs(104,30).addBox(-2,-16,-1,4,3,2),PartPose.ZERO);
		bell.addOrReplaceChild("lip",CubeListBuilder.create().texOffs(0,60).addBox(-9,1,-9,18,2,18),PartPose.ZERO);
		bell.addOrReplaceChild("kelpL",CubeListBuilder.create().texOffs(112,36).addBox(-2,0,0,4,12,0),PartPose.offset(-5,3,-9.2F));
		bell.addOrReplaceChild("kelpR",CubeListBuilder.create().texOffs(112,36).addBox(-2,0,0,4,12,0),PartPose.offset(6,3,-9.2F));
		bell.addOrReplaceChild("kelpB",CubeListBuilder.create().texOffs(112,36).addBox(-2,0,0,4,12,0),PartPose.offset(1,3,9.2F));
		for(int sign:new int[]{-1,1}){
			// Upper arm and forearm bending at the elbow, thigh and shin bending at the knee; each lower piece starts a touch inside
			// the one above so the joint stays closed as it bends.
			var arm=body.addOrReplaceChild(sign==-1?"right":"left",CubeListBuilder.create().texOffs(48,0).addBox(-3,-2,-3,6,9,6),PartPose.offset(sign*9.5F,-10,0));
			var fore=arm.addOrReplaceChild("fore",CubeListBuilder.create().texOffs(48,15).addBox(-3,-.5F,-3,6,9,6,new CubeDeformation(-.05F)),PartPose.offset(0,7,0));
			fore.addOrReplaceChild("fist",CubeListBuilder.create().texOffs(72,0).addBox(-3.5F,7,-3.5F,7,6,7),PartPose.ZERO);
			var leg=hips.addOrReplaceChild(sign==-1?"legR":"legL",CubeListBuilder.create().texOffs(100,0).addBox(-3.5F,0,-3.5F,7,6,7),PartPose.offset(sign*4,0,0));
			leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(100,13).addBox(-3.5F,-.5F,-3.5F,7,6.5F,7,new CubeDeformation(-.05F)),PartPose.offset(0,6,0));
		}
		body.getChild("right").getChild("fore").addOrReplaceChild("chain",CubeListBuilder.create().texOffs(72,14).addBox(-3.5F,.5F,-3.5F,7,4,7),PartPose.ZERO);
		var clapper=body.getChild("left").getChild("fore").addOrReplaceChild("clapper",CubeListBuilder.create().texOffs(72,60).addBox(-1,0,-1,2,6,2),PartPose.offset(0,10,0));
		clapper.addOrReplaceChild("ball",CubeListBuilder.create().texOffs(80,60).addBox(-3.5F,6,-3.5F,7,8,7),PartPose.ZERO);
		return LayerDefinition.create(mesh,128,128);
	}

	@Override public void setupAnim(BellWardenRenderState s){
		super.setupAnim(s);
		pose(s,s.move,s.elapsed,s.tell);
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1&&s.prevMove!=s.move){
			PartPose[] now=new PartPose[posed.length];
			for(int i=0;i<posed.length;i++)now[i]=posed[i].storePose();
			root().resetPose();
			pose(s,s.prevMove,s.prevElapsed,s.prevTell);
			float k=s.blend*s.blend*(3-2*s.blend);
			for(int i=0;i<posed.length;i++){
				PartPose a=posed[i].storePose(),b=now[i];
				posed[i].loadPose(PartPose.offsetAndRotation(Mth.lerp(k,a.x(),b.x()),Mth.lerp(k,a.y(),b.y()),Mth.lerp(k,a.z(),b.z()),
					Mth.lerp(k,a.xRot(),b.xRot()),Mth.lerp(k,a.yRot(),b.yRot()),Mth.lerp(k,a.zRot(),b.zRot())));
			}
		}
		hurt(s);
		die(s);
		ground();
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: kneeling or standing (or between), walking, then the move itself. */
	private void pose(BellWardenRenderState s,int move,float el,int tell){
		float r=s.rise,t=s.ageInTicks;
		kneel(r);
		// The bell follows its gaze a little once it is up, and settles on its shoulders with each breath.
		bell.yRot=s.yRot*Mth.DEG_TO_RAD*.45F*r;bell.xRot+=s.xRot*Mth.DEG_TO_RAD*.3F*r;
		bell.y=-12+Mth.sin(t*.05F)*.2F;body.xRot+=Mth.sin(t*.05F)*.015F;
		// Walking: a heavy, rolling tread, each knee bending as its foot comes through, arms swinging against the legs.
		float walk=s.walkAnimationSpeed*r,step=s.walkAnimationPos*.5F;
		legL.xRot+=Mth.cos(step)*walk*.6F;legR.xRot-=Mth.cos(step)*walk*.6F;
		shinL.xRot+=Math.max(0,-Mth.sin(step))*walk*.9F;shinR.xRot+=Math.max(0,Mth.sin(step))*walk*.9F;
		right.xRot+=Mth.cos(step)*walk*.35F;left.xRot-=Mth.cos(step)*walk*.35F;
		body.zRot=Mth.sin(step)*walk*.06F;
		// Kelp drifts as if still under water.
		kelpL.xRot=-.08F+Mth.sin(t*.07F)*.08F+walk*.2F;kelpR.xRot=-.08F+Mth.sin(t*.07F+1.7F)*.08F+walk*.2F;kelpB.xRot=.08F-Mth.sin(t*.06F+.6F)*.08F-walk*.2F;
		attack(move,el,tell);
	}

	/**
	 * Kneeling to standing, {@code r} 0 to 1, in three steps: from bowed over its fists sat back on its heels, it first sits up and
	 * lifts its bell, then plants its right foot ahead (down on one knee as the bell tolls), then pushes itself up off it.
	 */
	private void kneel(float r){
		float sit=smooth(r,0,.32F),plant=smooth(r,.3F,.62F),up=smooth(r,.62F,1);
		// On its heels: thighs forward, shins folded back under them. Then the right foot comes forward, the left knee stays down.
		float lThigh=Mth.lerp(plant,-1.1F,-.1F),lShin=Mth.lerp(plant,2.6F,1.62F);
		float rThigh=Mth.lerp(plant,-1.1F,-.95F),rShin=Mth.lerp(plant,2.6F,.95F);
		legL.xRot=Mth.lerp(up,lThigh,0);shinL.xRot=Mth.lerp(up,lShin,0);
		legR.xRot=Mth.lerp(up,rThigh,0);shinR.xRot=Mth.lerp(up,rShin,0);
		// Bowed right over, then sitting up, then the hunch it stands with.
		body.xRot=HUNCH+Mth.lerp(sit,.5F,-.12F)*(1-up);
		bell.xRot=-.15F*(1-sit);
		// Its fists planted out ahead on the floor while it bows; sitting up, they come back to rest by its knees; standing, they hang.
		float arm=Mth.lerp(up,Mth.lerp(sit,-.95F,-.55F),0)-body.xRot;
		right.xRot=left.xRot=arm;
		foreR.xRot=foreL.xRot=Mth.lerp(up,Mth.lerp(sit,-.45F,-.55F),-.3F);
		right.zRot=.08F;left.zRot=-.08F;
		clapper.xRot=Mth.lerp(r,-1.3F,-1.5F);
	}

	/**
	 * Its blows, laid over the standing pose: every one wound up through its tell so it reads from across the hall, then thrown fast.
	 * The clapper swings from its left side across its front; the hammer and the slam come up overhead and down; the charge drops its
	 * bell; spent, it leans back with its bell tipped up; under its bell it crouches right down.
	 */
	private void attack(int move,float el,int tellTicks){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case BellWardenRules.SWING->{
				// Clapper arm out level and drawn back to its side, elbow cocked, then swept across its front, the arm straightening
				// into the blow and the body twisting with it.
				float out=Math.min(1,wind*1.5F);
				left.xRot=Mth.lerp(out,left.xRot,-1.45F);left.zRot=-.08F-.2F*out*(1-go);
				left.yRot=-1.15F*wind+2.3F*go;
				foreL.xRot=Mth.lerp(out,foreL.xRot,-.7F)*(1-go)-.1F*go;
				body.yRot=-.4F*wind*(1-go)+.45F*go;
				clapper.xRot=-.4F-.5F*wind*(1-go);
			}
			case BellWardenRules.HAMMER->{
				// Up overhead, elbow bent to bring the clapper back over its shoulder, then down straight onto the floor.
				left.xRot=Mth.lerp(go,Mth.lerp(wind,-1.45F,-3.0F),-1.55F);left.yRot=.25F*(1-go);
				foreL.xRot=Mth.lerp(go,-1.1F*wind,-.1F);
				body.xRot+=-.18F*wind*(1-go)+.45F*go;
				clapper.xRot=Mth.lerp(go,-.2F,-1.1F);
			}
			case BellWardenRules.SLAM->{
				// Both fists up overhead as it leans back, elbows bent, then down together with straight arms, its knees giving
				// and its hips dropping into it.
				float arm=Mth.lerp(go,-2.9F*wind,-1.25F);
				left.xRot=right.xRot=arm;left.zRot=-.25F*(1-go);right.zRot=.25F*(1-go);
				foreL.xRot=foreR.xRot=Mth.lerp(go,-.8F*wind,-.05F);
				body.xRot+=-.3F*wind*(1-go)+.55F*go;
				bend(.55F*go);
			}
			case BellWardenRules.CHARGE->{
				// Head down, bell forward like a ram, arms swept back; crouched low through the tell, then its legs pump through the run.
				body.xRot+=.75F*wind;bell.xRot+=.25F*wind;clapper.xRot=Mth.lerp(wind,clapper.xRot,-.2F);
				left.xRot+=.7F*wind;right.xRot+=.7F*wind;left.zRot-=.25F*wind;right.zRot+=.25F*wind;
				foreL.xRot=foreR.xRot=-.4F*wind;
				if(el>tell){
					float a=(el-tell)*1.7F,pump=Mth.sin(a)*.9F;
					legL.xRot=pump-.2F;legR.xRot=-pump-.2F;shinL.xRot=Math.max(0,-Mth.cos(a))*1.1F;shinR.xRot=Math.max(0,Mth.cos(a))*1.1F;
				}else bend(.4F*wind);
			}
			case BellWardenRules.BACKSTEP->{
				float k=ramp(el,0,2);
				body.xRot+=.3F*k;legL.xRot-=.5F*k;shinL.xRot+=.4F*k;legR.xRot+=.35F*k;left.xRot-=.4F*k;right.xRot-=.4F*k;
				foreL.xRot=foreR.xRot=-.5F*k;
			}
			case BellWardenRules.RECOVER->{
				// Spent: leaning back, bell tipped up, arms hanging wide and loose, knees soft, swaying where it stands.
				float k=ramp(el,0,4),sway=Mth.sin(el*.25F)*.05F;
				body.xRot-=(HUNCH+.12F)*k;bell.xRot-=.45F*k;
				left.zRot-=.35F*k;right.zRot+=.35F*k;left.xRot+=.25F*k;right.xRot+=.25F*k;body.zRot+=sway*k;
				foreL.xRot=foreR.xRot=-.15F*k;
				bend(.15F*k);
			}
			case BellWardenRules.SHELL->{
				// Down under its bell: crouched on bent knees, hunched right over, arms drawn in under the rim.
				float k=ramp(el,0,4);
				bend(.95F*k);
				body.xRot+=.75F*k;bell.xRot+=.35F*k;bell.y+=2F*k;
				left.xRot=Mth.lerp(k,left.xRot,-1.6F);right.xRot=Mth.lerp(k,right.xRot,-1.6F);left.zRot=Mth.lerp(k,left.zRot,.45F);right.zRot=Mth.lerp(k,right.zRot,-.45F);
				foreL.xRot=foreR.xRot=-1.3F*k;clapper.xRot=Mth.lerp(k,clapper.xRot,-.3F);
			}
			case BellWardenRules.TOLL->{
				// The clapper raised high and out beside its bell, elbow bent behind its head, the chain arm out for balance; then
				// swung in hard against the bronze, and the bell shudders on its shoulders after.
				left.xRot=Mth.lerp(wind,left.xRot,-2.5F);left.zRot=-.08F+.6F*wind*(1-go)-.3F*go;
				foreL.xRot=-1.2F*wind*(1-go)-.2F*go;
				clapper.xRot=.3F*wind*(1-go)-1.2F*go;
				right.zRot=.08F+.4F*wind;foreR.xRot=-.3F*wind;body.xRot-=.15F*wind;
				bend(.2F*wind);
				if(el>tell){float a=el-tell;bell.zRot=Mth.sin(a*1.6F)*.12F*(float)Math.exp(-a*.25F);}
			}
			default->{}
		}
	}

	/** Bends both knees by {@code k} (radians at the thigh), the shins folding back twice as far; {@link #ground} sinks the hips to match. */
	private void bend(float k){
		legL.xRot-=k;legR.xRot-=k;shinL.xRot+=2*k;shinR.xRot+=2*k;
	}

	/**
	 * Sets the hips so whichever leg reaches lowest just touches the floor: however its knees are bent, kneeling, getting up, crouched
	 * or mid-stride, it stands (or kneels) on the ground rather than floating over it or sinking through.
	 */
	private void ground(){
		hips.y=24-Math.max(reach(legL.xRot,shinL.xRot),reach(legR.xRot,shinR.xRot));
	}
	/** How far below the hip a leg reaches, thigh at {@code thigh}, shin at {@code shin} from it: the lowest corner of either piece. */
	private static float reach(float thigh,float shin){
		float a=thigh+shin,knee=6*Mth.cos(thigh);
		float top=Math.max(0,6*Mth.cos(thigh))+3.5F*Math.abs(Mth.sin(thigh));
		float low=knee+Math.max(-.5F*Mth.cos(a),6*Mth.cos(a))+3.5F*Math.abs(Mth.sin(a));
		return Math.max(top,low);
	}

	/** A blow it takes: its body jolts back from it and the bell rattles on its shoulders. */
	private void hurt(BellWardenRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		body.xRot-=.18F*k;bell.zRot+=Mth.sin(s.hurt*20)*.1F*k;bell.xRot-=.08F*k;
	}

	/** Dying: it sinks back onto its knees, then topples forward onto its bell, its arms gone slack. */
	private void die(BellWardenRenderState s){
		if(s.deathTime<=0)return;
		float knees=smooth(s.deathTime,0,8),fall=smooth(s.deathTime,5,19);
		// Down on both knees, upright, shins flat behind it.
		legL.xRot=Mth.lerp(knees,legL.xRot,0);shinL.xRot=Mth.lerp(knees,shinL.xRot,Mth.HALF_PI);
		legR.xRot=Mth.lerp(knees,legR.xRot,0);shinR.xRot=Mth.lerp(knees,shinR.xRot,Mth.HALF_PI);
		body.xRot=Mth.lerp(fall,Mth.lerp(knees,body.xRot,HUNCH-.1F),1.35F);body.yRot*=1-knees;body.zRot*=1-knees;
		bell.xRot=Mth.lerp(fall,Mth.lerp(knees,bell.xRot,0),.2F);bell.yRot*=1-knees;bell.zRot*=1-knees;
		// The arms hang slack, straight down from the shoulders, then trail behind as it falls.
		float arm=-body.xRot-1.3F*fall;
		right.xRot=Mth.lerp(knees,right.xRot,arm);left.xRot=Mth.lerp(knees,left.xRot,arm);right.yRot*=1-knees;left.yRot*=1-knees;
		right.zRot=Mth.lerp(knees,right.zRot,.3F);left.zRot=Mth.lerp(knees,left.zRot,-.3F);
		foreR.xRot=Mth.lerp(knees,foreR.xRot,-.1F);foreL.xRot=Mth.lerp(knees,foreL.xRot,-.1F);
		clapper.xRot=Mth.lerp(fall,clapper.xRot,-.2F);
	}
}
