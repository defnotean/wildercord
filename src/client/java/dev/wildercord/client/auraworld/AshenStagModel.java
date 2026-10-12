package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.AshenStagRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * A great stag burnt black, its hide cracked like charred bark with embers glowing in the cracks, ash-white about the muzzle and
 * belly, and a crown of burnt antlers smouldering at the tips. It lies bedded in the ash with its legs folded under it; the rise brings
 * its hindquarters up first, kneeling on its forelegs, then the forelegs unfold and it tosses its head. Every leg bends at the knee,
 * each move blends into the next, a blow jolts it, and dying its legs buckle and it rolls over onto its side.
 */
public final class AshenStagModel extends EntityModel<AshenStagRenderState> {
	/** Where each leg hangs from the body, front to back, and the angle (from straight down) its thigh and shin stand at. */
	private static final float FRONT=-9,HIND=9,HIND_U=-.2F,HIND_L=.45F;
	/** How long the thigh is to the knee, and the shin and hoof below it. */
	private static final float THIGH=6.5F,SHIN=8;
	private final ModelPart body,neck,head,tail;
	private final ModelPart[] legs=new ModelPart[4],shins=new ModelPart[4];
	private final ModelPart[] posed;
	/** The pose being built: each leg's thigh angle in the world and shin angle from the thigh, the body's pitch and roll, the neck, the head. */
	private final float[] u=new float[4],l=new float[4],uz=new float[4];
	private float pitch,roll,yaw,neckX,neckY,headX,tailX;
	public AshenStagModel(ModelPart root){
		super(root);body=root.getChild("body");neck=body.getChild("neck");head=neck.getChild("head");tail=body.getChild("tail");
		String[] names={"legFL","legFR","legHL","legHR"};
		for(int i=0;i<4;i++){legs[i]=body.getChild(names[i]);shins[i]=legs[i].getChild("shin");}
		posed=new ModelPart[]{body,neck,head,tail,legs[0],legs[1],legs[2],legs[3],shins[0],shins[1],shins[2],shins[3]};
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-5,-5,-12,10,10,24),PartPose.offset(0,8.5F,0));
		var neck=body.addOrReplaceChild("neck",CubeListBuilder.create().texOffs(0,34).addBox(-2.5F,-12,-3,5,13,6),PartPose.offset(0,-2,-10));
		neck.addOrReplaceChild("ruff",CubeListBuilder.create().texOffs(64,56).addBox(-3,-8,-4,6,8,2),PartPose.ZERO);
		var head=neck.addOrReplaceChild("head",CubeListBuilder.create().texOffs(24,34).addBox(-3,-5,-7,6,6,9),PartPose.offset(0,-11,0));
		head.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(56,34).addBox(-2,-3,-11,4,4,4),PartPose.ZERO);
		for(int sign:new int[]{-1,1}){
			String side=sign<0?"R":"L";
			head.addOrReplaceChild("ear"+side,CubeListBuilder.create().texOffs(72,34).addBox(-1,-4,0,2,4,1),PartPose.offsetAndRotation(sign*3,-4,1,.2F,0,sign*1.0F));
			// A beam swept up and out, two tines off its front, and a fork at its top.
			var beam=head.addOrReplaceChild("antler"+side,CubeListBuilder.create().texOffs(68,0).addBox(-1,-10,-1,2,10,2),PartPose.offsetAndRotation(sign*2,-5,-1,-.25F,0,sign*.5F));
			beam.addOrReplaceChild("brow",CubeListBuilder.create().texOffs(80,0).addBox(-.5F,-4,-.5F,1,4,1),PartPose.offsetAndRotation(0,-3,-.5F,.9F,0,0));
			beam.addOrReplaceChild("bez",CubeListBuilder.create().texOffs(80,0).addBox(-.5F,-4,-.5F,1,4,1),PartPose.offsetAndRotation(0,-6.5F,-.5F,.6F,0,-sign*.3F));
			beam.addOrReplaceChild("forkA",CubeListBuilder.create().texOffs(88,0).addBox(-.5F,-5,-.5F,1,5,1),PartPose.offsetAndRotation(0,-9.5F,0,.35F,0,sign*.35F));
			beam.addOrReplaceChild("forkB",CubeListBuilder.create().texOffs(88,0).addBox(-.5F,-5,-.5F,1,5,1),PartPose.offsetAndRotation(0,-9.5F,0,-.45F,0,-sign*.2F));
		}
		for(int i=0;i<4;i++){
			boolean hind=i>=2;float x=(i%2==0?1:-1)*3.5F,z=hind?HIND:FRONT;
			var cubes=hind?CubeListBuilder.create().texOffs(12,56).addBox(-2,-1,-2.5F,4,9,5):CubeListBuilder.create().texOffs(0,56).addBox(-1.5F,-1,-1.5F,3,8,3);
			var leg=body.addOrReplaceChild(new String[]{"legFL","legFR","legHL","legHR"}[i],cubes,PartPose.offset(x,2,z));
			var shin=leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(32,56).addBox(-1,-.5F,-1,2,6.5F,2),PartPose.offset(0,THIGH,0));
			shin.addOrReplaceChild("hoof",CubeListBuilder.create().texOffs(40,56).addBox(-1.5F,6,-1.5F,3,2,3),PartPose.ZERO);
		}
		body.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(52,56).addBox(-1.5F,0,0,3,4,2),PartPose.offsetAndRotation(0,-3,12,.3F,0,0));
		return LayerDefinition.create(mesh,128,128);
	}

	@Override public void setupAnim(AshenStagRenderState s){
		super.setupAnim(s);
		pose(s,s.move,s.elapsed,s.tell,s.side);
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1&&s.prevMove!=s.move){
			PartPose[] now=new PartPose[posed.length];
			for(int i=0;i<posed.length;i++)now[i]=posed[i].storePose();
			root().resetPose();
			pose(s,s.prevMove,s.prevElapsed,s.prevTell,s.side);
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
		// Off the ground through a bound, highest halfway.
		if(s.move==AshenStagRules.BOUND&&s.deathTime<=0)body.y-=5*Mth.sin(Mth.PI*Mth.clamp(s.elapsed/Math.max(1,s.tell),0,1));
		// Rolled onto its side in death, it lies on its flank.
		if(s.deathTime>0)body.y=Mth.lerp(smooth(s.deathTime,6,18),body.y,24-5.5F);
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: bedded or standing (or between), walking, then the move itself. */
	private void pose(AshenStagRenderState s,int move,float el,int tell,int side){
		float r=s.rise,t=s.ageInTicks;
		java.util.Arrays.fill(uz,0);roll=0;yaw=0;
		bed(r);
		// Breathing, slow in its bed, and its gaze followed once it is up.
		pitch+=Mth.sin(t*.06F)*.012F;neckX+=Mth.sin(t*.06F+.5F)*.03F;
		neckY=Mth.clamp(s.yRot,-60,60)*Mth.DEG_TO_RAD*.55F*r;headX+=s.xRot*Mth.DEG_TO_RAD*.5F*r;
		tailX=.3F+Mth.sin(t*.09F)*.08F;
		// Walking: a diagonal trot, each hoof lifted as it comes through, the head nodding with the stride.
		float walk=Math.min(1,s.walkAnimationSpeed)*r,step=s.walkAnimationPos*.6F;
		float sw=Mth.cos(step)*walk*.5F;
		u[0]+=sw;u[3]+=sw;u[1]-=sw;u[2]-=sw;
		l[0]+=Math.max(0,-Mth.sin(step))*walk*1.1F;l[3]+=Math.max(0,-Mth.sin(step))*walk*.6F;
		l[1]+=Math.max(0,Mth.sin(step))*walk*1.1F;l[2]+=Math.max(0,Mth.sin(step))*walk*.6F;
		neckX+=Mth.sin(step*2)*walk*.06F;
		attack(move,el,tell,side,t);
		apply();
	}

	/**
	 * Bedded to standing, {@code r} 0 to 1, hindquarters first: its hind legs straighten under it while it kneels on its folded
	 * forelegs, nose down; then its forelegs unfold one after the other; then it tosses its head up and bellows.
	 */
	private void bed(float r){
		float h=smooth(r,0,.45F),fl=smooth(r,.45F,.75F),fr=smooth(r,.52F,.82F),toss=smooth(r,.78F,1);
		u[2]=u[3]=Mth.lerp(h,-1.3F,HIND_U);l[2]=l[3]=Mth.lerp(h,2.6F,HIND_L);
		// Folded flat under it, then kneeling on its knees with the shins flat behind, then straight.
		float kneelU=Mth.lerp(h,-1.3F,.15F),kneelL=Mth.lerp(h,2.6F,1.6F);
		u[0]=Mth.lerp(fl,kneelU,0);l[0]=Mth.lerp(fl,kneelL,0);
		u[1]=Mth.lerp(fr,kneelU,0);l[1]=Mth.lerp(fr,kneelL,0);
		pitch=.4F*h*(1-(fl+fr)/2);
		float up=smooth(r,.2F,.85F),lift=Mth.sin(Mth.PI*toss);
		neckX=Mth.lerp(up,.95F,.5F)-.45F*lift;
		headX=Mth.lerp(up,.35F,0)-.3F*lift;
	}

	/**
	 * Its moves, laid over the standing pose, each wound up through its tell so it reads from across the glade, then thrown fast. The
	 * gore drops its head and sweeps the antlers up; the kick puts its weight on its forelegs and lashes both hind hooves back; the
	 * stomp rears it right up and brings it down; the charge (and each run of a rush) lowers its antlers and paws, then gallops; the bound gathers its legs
	 * under it in the air; spent, its head hangs and its sides heave; stuck, it braces all four legs and wrenches at its antlers.
	 */
	private void attack(int move,float el,int tellTicks,int side,float t){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case AshenStagRules.GORE->{
				// Head down, antlers levelled, forelegs bracing; then the head flung up and back through whoever is in front.
				float down=wind*(1-go);
				neckX+=.55F*down-.55F*go;headX+=.55F*down-.45F*go;
				pitch+=.12F*down-.12F*go;
				u[0]-=.25F*down;u[1]-=.25F*down;l[0]+=.4F*down;l[1]+=.4F*down;
				u[2]+=.2F*go;u[3]+=.2F*go;
			}
			case AshenStagRules.KICK->{
				// Weight onto its forelegs, hind legs gathered under; then both lashed out straight behind it.
				pitch+=.15F*wind+.15F*go;neckX+=.25F*wind;headX+=.1F*wind;
				u[2]=u[3]=Mth.lerp(go,HIND_U-.45F*wind,1.25F);l[2]=l[3]=Mth.lerp(go,HIND_L+.6F*wind,-.15F);
				u[0]-=.1F*wind;u[1]-=.1F*wind;tailX+=.6F*wind;
			}
			case AshenStagRules.STOMP->{
				// Reared up on its hind legs, forelegs folded and pawing the air, head thrown back; then down hard on straight forelegs.
				float rear=wind*(1-go);
				pitch+=-.95F*rear+.18F*go;
				neckX-=.45F*rear;headX-=.2F*rear;headX+=.2F*go;
				float paw=Mth.sin(el*.9F)*.25F*rear;
				u[0]=Mth.lerp(go,-1.0F*wind+paw,-.15F);u[1]=Mth.lerp(go,-1.0F*wind-paw,-.15F);
				l[0]=Mth.lerp(go,1.5F*wind,0);l[1]=Mth.lerp(go,1.5F*wind,0);
				u[2]=u[3]=Mth.lerp(rear,HIND_U,.1F);l[2]=l[3]=Mth.lerp(rear,HIND_L,.25F);
				tailX+=.5F*rear;
			}
			case AshenStagRules.CHARGE,AshenStagRules.RUSH->{
				// Antlers lowered at its prey and a forehoof scraping the ash through the tell; then a flat-out gallop, forelegs
				// together and hind legs together, the body rocking over them.
				neckX+=.5F*wind;headX+=.3F*wind;
				if(el<=tell){
					float scrape=Mth.sin(el*1.1F);
					u[1]=-.35F*Math.max(0,scrape)*wind+.3F*Math.max(0,-scrape)*wind;l[1]=.9F*Math.max(0,scrape)*wind;
					u[2]-=.15F*wind;u[3]-=.15F*wind;pitch+=.06F*wind;
				}else{
					float a=(el-tell)*1.3F,c=Mth.sin(a);
					u[0]=c*.8F;u[1]=c*.8F-.15F;l[0]=l[1]=Math.max(0,-Mth.cos(a))*1.2F;
					u[2]=HIND_U-c*.8F;u[3]=HIND_U-c*.8F+.15F;l[2]=l[3]=HIND_L+Math.max(0,Mth.cos(a))*.7F;
					pitch+=c*.08F+.08F;tailX+=.5F;
				}
			}
			case AshenStagRules.BOUND->{
				// Legs tucked up under it in the air and the body leaning into the bound; backwards, its forehand lifts.
				float p=Mth.clamp(el/tell,0,1),tuck=Mth.sin(Mth.PI*p);
				u[0]-=.7F*tuck;u[1]-=.7F*tuck;l[0]+=1.3F*tuck;l[1]+=1.3F*tuck;
				u[2]-=.5F*tuck;u[3]-=.5F*tuck;l[2]+=.8F*tuck;l[3]+=.8F*tuck;
				if(side==0){pitch-=.25F*tuck;neckX-=.2F*tuck;}
				else{roll=side*.3F*tuck;yaw=-side*.25F*tuck;for(int i=0;i<4;i++)uz[i]=side*.35F*tuck;neckY-=side*.3F*tuck;}
			}
			case AshenStagRules.RECOVER->{
				// Spent: head hanging low, legs splayed and sides heaving.
				float k=ramp(el,0,4),heave=Mth.sin(el*.5F);
				neckX+=.65F*k;headX+=.35F*k;pitch+=.04F*k+.025F*heave*k;roll=.03F*heave*k;
				uz[0]=-.12F*k;uz[1]=.12F*k;uz[2]=-.1F*k;uz[3]=.1F*k;
				tailX-=.2F*k;
			}
			case AshenStagRules.STUCK->{
				// Antlers driven in low ahead of it, all four legs braced, wrenching at them in heaves.
				float k=ramp(el,0,3),heave=(float)Math.pow(Math.max(0,Mth.sin((el-3)*Mth.TWO_PI/15)),3);
				neckX+=.75F*k;headX+=.25F*k;pitch+=.15F*k-.12F*heave;
				u[0]-=.3F*k;u[1]-=.3F*k;u[2]+=(.45F+.2F*heave)*k;u[3]+=(.45F+.2F*heave)*k;l[2]-=.2F*k;l[3]-=.2F*k;
				neckY+=Mth.sin(el*1.7F)*.12F*heave;roll=Mth.sin(el*1.7F)*.04F*heave;
				tailX+=.4F*heave;
			}
			default->{}
		}
	}

	/** Sets the parts from the pose built: each leg's world angle turned into one from the body, and the head held against the neck. */
	private void apply(){
		body.xRot=pitch;body.zRot=roll;body.yRot=yaw;
		neck.xRot=neckX;neck.yRot=neckY;
		head.xRot=-neckX-pitch+headX;
		tail.xRot=tailX;
		for(int i=0;i<4;i++){legs[i].xRot=u[i]-pitch;legs[i].zRot=uz[i];shins[i].xRot=l[i];}
	}

	/**
	 * Sets the body's height so whatever reaches lowest just touches the floor: a hoof or a knee, or its belly while it lies bedded.
	 * However its legs are bent and however it is pitched, it stands (or lies) on the ground rather than floating or sinking.
	 */
	private void ground(){
		float b=body.xRot,low=0;
		for(float y:new float[]{-5,5})for(float z:new float[]{-12,12})low=Math.max(low,lift(y,z,b));
		for(int i=0;i<4;i++){
			float zp=i<2?FRONT:HIND,a=legs[i].xRot,c=a+shins[i].xRot;
			float ky=2+THIGH*Mth.cos(a),kz=zp+THIGH*Mth.sin(a);
			float hy=ky+SHIN*Mth.cos(c)+1.5F*Math.abs(Mth.sin(c)),hz=kz+SHIN*Mth.sin(c);
			low=Math.max(low,Math.max(lift(ky+1,kz,b),lift(hy,hz,b)));
		}
		body.y=24-low;
	}
	/** How far below the body's pivot a point in the body (at {@code y}, {@code z}) sits, the body pitched {@code b}. */
	private static float lift(float y,float z,float b){return y*Mth.cos(b)-z*Mth.sin(b);}

	/** A blow it takes: it flinches, its head tossed up and its hindquarters dropping a touch. */
	private void hurt(AshenStagRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		neck.xRot-=.25F*k;head.xRot-=.1F*k;body.zRot+=Mth.sin(s.hurt*18)*.04F*k;
	}

	/** Dying: its legs buckle under it and it sinks onto its belly, then rolls onto its side, legs gone slack and its head down. */
	private void die(AshenStagRenderState s){
		if(s.deathTime<=0)return;
		float buckle=smooth(s.deathTime,0,7),fall=smooth(s.deathTime,6,18);
		body.xRot=Mth.lerp(buckle,body.xRot,.05F);body.yRot*=1-buckle;
		body.zRot=Mth.lerp(fall,Mth.lerp(buckle,body.zRot,0),1.45F);
		for(int i=0;i<4;i++){
			legs[i].xRot=Mth.lerp(fall,Mth.lerp(buckle,legs[i].xRot,-1.2F),-.3F+.15F*i);
			shins[i].xRot=Mth.lerp(fall,Mth.lerp(buckle,shins[i].xRot,2.4F),.4F);
			legs[i].zRot=Mth.lerp(fall,legs[i].zRot,-.25F);
		}
		neck.xRot=Mth.lerp(buckle,neck.xRot,.9F);neck.yRot=Mth.lerp(fall,neck.yRot*(1-buckle),.25F);
		head.xRot=Mth.lerp(fall,Mth.lerp(buckle,head.xRot,-.6F),-.2F);
		tail.xRot=Mth.lerp(buckle,tail.xRot,.1F);
	}
}
