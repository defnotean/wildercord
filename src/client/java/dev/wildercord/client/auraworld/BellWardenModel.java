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
 * Elbows and knees bend, every move has its own pose wound up through its tell, each move blends into the next, a blow jolts it
 * and rattles the bell on its shoulders, and dying it sinks back onto its knees and topples forward under its bell.
 */
public final class BellWardenModel extends EntityModel<BellWardenRenderState> {
	/** Hip height standing, in model units above the feet's 24, and the hunch it stands with. */
	private static final float HIPS=12,HUNCH=.32F;
	private final ModelPart hips,body,bell,right,left,foreR,foreL,legR,legL,shinR,shinL,clapper,kelpL,kelpR,kelpB;
	private final ModelPart[] posed;
	/** Whether its bell is torn open, for the moves it makes twice over then. */
	private boolean cracked;
	/** How far the kelp is flung by the move's last jolt (it hangs from the bell and swings after it), and the clock the kelp ripples on. */
	private float kelpKick,clock;
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
		super.setupAnim(s);cracked=s.cracked;
		pose(s,s.move,s.elapsed,s.tell);
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1){
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
	/** Time held still for two ticks at {@code at}, the contact pose of a blow, then running on: the hit-stop. */
	private static float stop(float el,float at){return el<at?el:el<at+2?at:el-2;}
	/** The ring-down after a blow lands at {@code at}: a quick overshoot, decaying away. */
	private static float ring(float el,float at){float q=el-at;return q<0?0:Mth.exp(-q*.3F)*Mth.sin(q*1.1F);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: kneeling or standing (or between), walking, then the move itself. */
	private void pose(BellWardenRenderState s,int move,float el,int tell){
		float r=s.rise,t=s.ageInTicks;
		clock=t;kelpKick=0;
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
		// The kelp hangs from the bell, so it swings a beat behind whatever the bell does, each strand on its own period.
		kelpL.xRot+=kelpKick+(bell.xRot+body.xRot)*.35F+Mth.sin(t*.21F-.9F)*.03F*walk;
		kelpR.xRot+=kelpKick*.9F+(bell.xRot+body.xRot)*.35F+Mth.sin(t*.17F-2F)*.03F*walk;
		kelpB.xRot-=kelpKick+(bell.xRot+body.xRot)*.35F;
		kelpL.zRot=bell.zRot*.8F+Mth.sin(t*.09F-1.1F)*.05F;kelpR.zRot=bell.zRot*.8F+Mth.sin(t*.08F-2.3F)*.05F;kelpB.zRot=-bell.zRot*.8F+Mth.sin(t*.07F-.4F)*.05F;
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
	 * Its moves, laid over the standing pose: every one wound up through its tell so it reads from across the hall, then thrown fast.
	 * It twists far round with the clapper trailing and then whirls all the way round; raises the clapper overhead and brings it down
	 * to stick fast in the floor, hauling at it after; heaves both fists up and down onto the floor; drops its bell like a ram to charge
	 * and stands ringing after a wall; strikes its own bell, twice once cracked; whirls its chain overhead and lashes it out; lifts its
	 * bell off its head and brings it down over the floor ahead, hunched bare-headed over it; hums its bell with both hands on the rim;
	 * spreads its arms with its head back, gathers its fists overhead and brings them down; and, half beaten, wrenches at its bell until
	 * it bursts and throws its arms wide. Spent, it leans back with its bell tipped up; under its bell it crouches right down.
	 */
	private void attack(int move,float el,int tellTicks){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(move==BellWardenRules.HAMMER||move==BellWardenRules.SLAM?stop(el,tell+2):el,tell,2);
		// After a landed blow it rebounds past the mark and settles, the bell and kelp swinging on after it.
		float hit=move==BellWardenRules.HAMMER||move==BellWardenRules.SLAM?ring(el,tell+2):0;
		switch(move){
			case BellWardenRules.SWING->{
				// Feet planted and twisted far round, the clapper arm out level and trailing behind it; then all the way round in a
				// blur, the arm flung straight out, and a stagger as the weight of it carries on.
				float out=Math.min(1,wind*1.5F),spin=Mth.clamp((el-tell)/BellWardenRules.SWING_SPIN,0,1);
				body.yRot=spin>=1?0:1.15F*wind-(1.15F+(float)(Math.PI*2))*spin;
				left.zRot=Mth.lerp(out,left.zRot,-1.35F);left.xRot=Mth.lerp(out,left.xRot,-.35F);left.yRot=.5F*out*(1-spin);
				foreL.xRot=Mth.lerp(out,foreL.xRot,-.5F)*(1-go);
				right.zRot+=.5F*out;right.xRot-=.3F*out;
				clapper.xRot=Mth.lerp(out,clapper.xRot,-.3F)-.6F*go;
				bend(.35F*wind);body.xRot+=.15F*wind;
				if(spin>=1){float st=ramp(el,tell+BellWardenRules.SWING_SPIN,3);body.zRot+=Mth.sin(el*.9F)*.08F*(1-st*.5F);}
			}
			case BellWardenRules.HAMMER->{
				// Up overhead in both hands, back arched, elbows bent to bring the clapper back over its shoulder, then down straight
				// onto the floor with its whole weight behind it.
				left.xRot=Mth.lerp(go,Mth.lerp(wind,-1.45F,-3.0F),-1.55F);left.yRot=.25F*(1-go);
				right.xRot=Mth.lerp(go,Mth.lerp(wind,-1.2F,-2.8F),-1.45F);right.yRot=-.35F;right.zRot=-.2F;
				foreL.xRot=Mth.lerp(go,-1.1F*wind,-.1F);foreR.xRot=Mth.lerp(go,-1.0F*wind,-.15F);
				body.xRot+=-.25F*wind*(1-go)+.5F*go;bell.xRot-=.2F*wind*(1-go);
				clapper.xRot=Mth.lerp(go,-.2F,-1.1F);
				body.xRot-=.1F*hit;left.xRot-=.12F*hit;right.xRot-=.12F*hit;bell.xRot-=.18F*hit;clapper.xRot+=.2F*hit;kelpKick+=.5F*hit;
				bend(.3F*go);
			}
			case BellWardenRules.STUCK->{
				// Bent over its clapper where it sticks in the floor, both hands on it, heaving back on it again and again.
				float k=ramp(el,0,3),heave=Math.max(0,Mth.sin(el*.4F));
				left.xRot=-1.55F+.25F*heave;left.yRot=.1F;right.xRot=-1.45F+.25F*heave;right.yRot=-.35F;right.zRot=-.2F;
				foreL.xRot=foreR.xRot=-.1F-.3F*heave;
				body.xRot+=(.5F-.2F*heave)*k;bell.xRot-=.15F*heave;body.zRot+=Mth.sin(el*.23F)*.05F;
				clapper.xRot=-1.1F;
				bend((.35F+.15F*heave)*k);
			}
			case BellWardenRules.SLAM->{
				// Both fists up overhead as it leans back, elbows bent, then down together with straight arms, its knees giving
				// and its hips dropping into it.
				float arm=Mth.lerp(go,-2.9F*wind,-1.25F);
				left.xRot=right.xRot=arm;left.zRot=-.25F*(1-go);right.zRot=.25F*(1-go);
				foreL.xRot=foreR.xRot=Mth.lerp(go,-.8F*wind,-.05F);
				body.xRot+=-.3F*wind*(1-go)+.55F*go;
				body.xRot-=.12F*hit;left.xRot-=.15F*hit;right.xRot-=.15F*hit;bell.xRot-=.2F*hit;kelpKick+=.55F*hit;
				bend(.55F*go);
			}
			case BellWardenRules.CHARGE->{
				// Head down, bell forward like a ram, arms swept back; crouched low and pawing through the tell, then its legs pump
				// through the run.
				body.xRot+=.75F*wind;bell.xRot+=.25F*wind;clapper.xRot=Mth.lerp(wind,clapper.xRot,-.2F);
				left.xRot+=.7F*wind;right.xRot+=.7F*wind;left.zRot-=.25F*wind;right.zRot+=.25F*wind;
				foreL.xRot=foreR.xRot=-.4F*wind;
				if(el>tell){
					float a=(el-tell)*1.7F,pump=Mth.sin(a)*.9F;
					legL.xRot=pump-.2F;legR.xRot=-pump-.2F;shinL.xRot=Math.max(0,-Mth.cos(a))*1.1F;shinR.xRot=Math.max(0,Mth.cos(a))*1.1F;
				}else{bend(.4F*wind);legR.xRot-=Math.max(0,Mth.sin(el*.8F))*.3F*wind;}
			}
			case BellWardenRules.DAZED->{
				// Its bell rung on stone: swaying on soft knees, arms slack, its bell lolling on its shoulders.
				float k=ramp(el,0,4);
				body.zRot+=Mth.sin(el*.2F)*.15F*k;bell.zRot=Mth.sin(el*.2F+.6F)*.2F*k;bell.xRot+=(.15F+.15F*Mth.sin(el*.13F))*k;
				body.xRot+=.15F*k;left.zRot-=.2F*k;right.zRot+=.2F*k;left.xRot+=.2F*k;right.xRot+=.2F*k;
				foreL.xRot=foreR.xRot=-.1F*k;clapper.xRot=Mth.lerp(k,clapper.xRot,-.1F);
				bend((.25F+.08F*Mth.sin(el*.2F))*k);
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
				// swung in hard against the bronze, and the bell shudders on its shoulders after. Cracked, it draws back and strikes again.
				float w2=cracked?smooth(el,tell+2,tell+BellWardenRules.TOLL_AGAIN):0,g2=cracked?ramp(el,tell+BellWardenRules.TOLL_AGAIN,2):0;
				float raise=wind*(1-go)+w2*(1-g2),strike=go*(1-w2)+g2;
				left.xRot=Mth.lerp(wind,left.xRot,-2.5F);left.zRot=-.08F+.6F*raise-.3F*strike;
				foreL.xRot=-1.2F*raise-.2F*strike;
				clapper.xRot=.3F*raise-1.2F*strike;
				right.zRot=.08F+.4F*wind;foreR.xRot=-.3F*wind;body.xRot-=.15F*wind;
				bend(.2F*wind);
				if(el>tell){float a=el-(g2>0?tell+BellWardenRules.TOLL_AGAIN:tell);bell.zRot=Mth.sin(a*1.6F)*.12F*(float)Math.exp(-a*.25F);
					kelpKick+=Mth.sin(a*1.6F-1F)*.25F*(float)Math.exp(-a*.25F);body.xRot+=.05F*Mth.exp(-a*.5F);}
			}
			case BellWardenRules.LASH->{
				// The chain arm raised straight up, whirling the chain round overhead faster and faster; then flung out ahead at the
				// height of its shoulder, the body turning in behind it, and a beat with the arm out as the chain comes home.
				float a=el*(.5F+el*.04F),up=Math.min(1,wind*1.6F);
				right.xRot=Mth.lerp(go,Mth.lerp(up,right.xRot,-2.9F+.25F*Mth.cos(a)*(1-go)),-1.5F);right.zRot=Mth.lerp(go,.08F+.3F*Mth.sin(a)*up,-.15F);
				foreR.xRot=Mth.lerp(go,-.35F*up,-.05F);
				body.yRot=.35F*wind*(1-go)-.4F*go;body.xRot+=-.15F*wind*(1-go)+.3F*go;
				left.zRot-=.3F*wind;left.xRot+=.3F*wind;
				bend(.2F*wind+.15F*go);
			}
			case BellWardenRules.COVER->{
				// Both hands on the rim, it lifts its bell clear off its head and holds it high, leaning back under the weight; then
				// brings it down mouth first on the floor ahead and stays hunched over it, bare-headed, straining to lift it again.
				float lift=Math.min(1,wind*1.3F);
				body.xRot+=-.25F*lift*(1-go)+.55F*go;
				left.xRot=right.xRot=Mth.lerp(go,-2.9F*lift,-1.15F-body.xRot);left.zRot=Mth.lerp(go,.35F*lift,.2F);right.zRot=Mth.lerp(go,-.35F*lift,-.2F);
				foreL.xRot=foreR.xRot=Mth.lerp(go,-.6F*lift,-.1F);
				// Where its bell sits on the floor ahead, worked back through the lean of its body.
				float th=body.xRot,ty=9,tz=-24,by=ty*Mth.cos(th)+tz*Mth.sin(th),bz=-ty*Mth.sin(th)+tz*Mth.cos(th);
				bell.y=Mth.lerp(go,bell.y-7*lift,by);bell.z=Mth.lerp(go,0,bz);bell.xRot=Mth.lerp(go,bell.xRot-.2F*lift,-th);bell.yRot*=1-go;
				clapper.xRot=Mth.lerp(go,clapper.xRot,-.2F);
				bend(.25F*wind+.25F*go);
				if(el>tell)body.zRot+=Mth.sin(el*1.3F)*.03F;
			}
			case BellWardenRules.PEAL->{
				// Both hands up on its bell's rim, the bell shivering faster and faster on its shoulders; then head up into the sound,
				// the bell jolting with every pulse that goes out.
				float k=Math.min(1,wind*1.5F);
				left.xRot=right.xRot=Mth.lerp(k,left.xRot,-2.3F);left.zRot=Mth.lerp(k,left.zRot,-.55F);right.zRot=Mth.lerp(k,right.zRot,.55F);
				foreL.xRot=foreR.xRot=-1.15F*k;clapper.xRot=Mth.lerp(k,clapper.xRot,-.4F);
				bell.zRot=Mth.sin(el*(1.2F+wind*1.6F))*.05F*wind;
				body.xRot-=.12F*wind;bend(.15F*wind);
				if(el>tell){
					float pulse=(float)Math.exp(-((el-tell)%BellWardenRules.PEAL_EVERY)*.6F);
					bell.xRot-=.12F+.15F*pulse;body.xRot-=.1F*pulse;bell.zRot+=Mth.sin(el*3.1F)*.04F;
				}
			}
			case BellWardenRules.UNDERTOW->{
				// Arms flung wide and head back while water pours off its bell; then its fists gathered together high overhead as
				// the floor drags in, swaying with the pull; then down, knees giving, onto the floor at its feet.
				float pour=ramp(el,0,BellWardenRules.UNDERTOW_POUR),gather=smooth(el,BellWardenRules.UNDERTOW_POUR,BellWardenRules.UNDERTOW_POUR+8);
				float wide=pour*(1-gather),up=gather*(1-go);
				left.zRot=-.08F-1.3F*wide-.25F*up;right.zRot=.08F+1.3F*wide+.25F*up;
				left.xRot=right.xRot=Mth.lerp(go,-2.95F*up-.2F*wide,-1.3F);
				foreL.xRot=foreR.xRot=Mth.lerp(go,-.7F*up,-.1F);
				body.xRot+=-.4F*wide-.15F*up+.6F*go;bell.xRot-=.3F*wide;
				if(up>0)body.zRot+=Mth.sin(el*.3F)*.06F*up;
				bend(.15F*wide+.2F*up+.6F*go);
			}
			case BellWardenRules.CRACK->{
				// Gripping its bell in both hands and wrenching at it, shaking harder and harder, sinking lower as it strains; then the
				// bell bursts and it is flung upright with its arms thrown wide.
				float k=Math.min(1,wind*3);
				left.xRot=right.xRot=Mth.lerp(go,Mth.lerp(k,left.xRot,-2.4F),-.6F);
				left.zRot=Mth.lerp(go,Mth.lerp(k,left.zRot,-.5F),-1.7F);right.zRot=Mth.lerp(go,Mth.lerp(k,right.zRot,.5F),1.7F);
				foreL.xRot=foreR.xRot=Mth.lerp(go,-1.2F*k,-.1F);
				float shake=wind*wind*(1-go);
				bell.zRot=Mth.sin(el*2.9F)*.1F*shake;bell.yRot+=Mth.sin(el*2.3F)*.08F*shake;body.zRot+=Mth.sin(el*3.7F)*.03F*shake;
				body.xRot+=.25F*wind*(1-go)-.5F*go;bell.xRot-=.45F*go;
				bend(.35F*wind*(1-go)+.1F*go);
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
		// The shoulders flinch up, the head-bell lags behind the body's recoil, and the kelp is flung.
		left.zRot-=.1F*k;right.zRot+=.1F*k;bell.xRot-=.1F*s.hurt*Mth.sin(s.hurt*7);kelpL.xRot+=.4F*k;kelpR.xRot+=.4F*k;kelpB.xRot-=.4F*k;
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
		// A stagger back before it sinks, and a last rebound off the floor as it lands, the bell ringing dully on its head.
		float stagger=smooth(s.deathTime,0,3)*(1-smooth(s.deathTime,3,8));
		body.xRot-=.3F*stagger;bell.xRot-=.2F*stagger;
		float q=Math.max(0,s.deathTime-19),land=Mth.exp(-q*.35F)*Mth.sin(q*1.3F)*fall;
		body.xRot-=.1F*land;bell.xRot-=.15F*land;bell.zRot+=.08F*land;
		kelpL.xRot+=.5F*land+.3F*fall*(1-fall);kelpR.xRot+=.5F*land+.3F*fall*(1-fall);kelpB.xRot-=.5F*land;
	}
}
