package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.FrostWyrmlingRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * A young ice drake: pale blue scales frosted white along its spines, a pale belly, two swept horns, a long neck and a longer tail
 * ending in a fin of ice, and two wings of thin pale membrane. It sleeps curled nose to tail with its wings folded over it; the rise
 * uncurls it, brings it up onto its legs, then throws its wings wide and its head up for the roar before it folds them again. Every
 * leg bends at the knee, each wing folds at its wrist, each move blends into the next, a blow jolts it, and dying its legs give and it
 * rolls onto its side with one wing flopped over it.
 */
public final class FrostWyrmlingModel extends EntityModel<FrostWyrmlingRenderState> {
	/** Where each leg hangs from the body, front to back, and the angle (from straight down) the hind thigh and shin stand at. */
	private static final float FRONT=-6,HIND=7,HIND_U=-.25F,HIND_L=.5F;
	/** How long the thigh is to the knee, and the shin and foot below it. */
	private static final float THIGH=7,SHIN=8;
	/** How long each wing's arm is, from shoulder to wrist. */
	private static final float ARM=8;
	private final ModelPart body,neck1,neck2,head,jaw,tail1,tail2,tail3;
	private final ModelPart[] legs=new ModelPart[4],shins=new ModelPart[4],arms=new ModelPart[2],outers=new ModelPart[2];
	private final ModelPart[] posed;
	/** The pose being built: each leg's thigh angle in the world and shin angle from the thigh; the body; the neck, head and jaw; the tail. */
	private final float[] u=new float[4],l=new float[4],uz=new float[4];
	private float pitch,roll,yaw,n1X,n1Y,n2X,n2Y,headX,headY,jawX,t1X,t2X,t3X,tY;
	/** The wings: how far folded (1 tucked along its flanks, 0 spread), how far raised past that, and how far swept forward. */
	private float fold,up,fwd;
	public FrostWyrmlingModel(ModelPart root){
		super(root);body=root.getChild("body");neck1=body.getChild("neck1");neck2=neck1.getChild("neck2");head=neck2.getChild("head");jaw=head.getChild("jaw");
		tail1=body.getChild("tail1");tail2=tail1.getChild("tail2");tail3=tail2.getChild("tail3");
		String[] names={"legFL","legFR","legHL","legHR"};
		for(int i=0;i<4;i++){legs[i]=body.getChild(names[i]);shins[i]=legs[i].getChild("shin");}
		for(int i=0;i<2;i++){arms[i]=body.getChild(i==0?"wingL":"wingR");outers[i]=arms[i].getChild("outer");}
		posed=new ModelPart[]{body,neck1,neck2,head,jaw,tail1,tail2,tail3,legs[0],legs[1],legs[2],legs[3],shins[0],shins[1],shins[2],shins[3],
			arms[0],arms[1],outers[0],outers[1]};
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-5,-4,-10,10,8,20),PartPose.offset(0,7,0));
		for(int i=0;i<5;i++)body.addOrReplaceChild("spine"+i,CubeListBuilder.create().texOffs(116,10).addBox(-.5F,-3,-1,1,3,2),PartPose.offsetAndRotation(0,-4,-8+4*i,-.4F,0,0));
		var neck1=body.addOrReplaceChild("neck1",CubeListBuilder.create().texOffs(0,28).addBox(-3,-3,-8,6,6,8),PartPose.offset(0,-1,-9));
		neck1.addOrReplaceChild("spine",CubeListBuilder.create().texOffs(116,10).addBox(-.5F,-3,-1,1,3,2),PartPose.offsetAndRotation(0,-3,-4,-.4F,0,0));
		var neck2=neck1.addOrReplaceChild("neck2",CubeListBuilder.create().texOffs(28,28).addBox(-2.5F,-2.5F,-7,5,5,7),PartPose.offset(0,0,-8));
		var head=neck2.addOrReplaceChild("head",CubeListBuilder.create().texOffs(52,28).addBox(-3.5F,-3.5F,-6,7,6,6),PartPose.offset(0,0,-7));
		head.addOrReplaceChild("snout",CubeListBuilder.create().texOffs(78,28).addBox(-2.5F,-2.5F,-11,5,3,5),PartPose.ZERO);
		head.addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(98,28).addBox(-2,0,-5,4,2,5),PartPose.offset(0,.5F,-6));
		for(int sign:new int[]{-1,1})
			head.addOrReplaceChild(sign<0?"hornR":"hornL",CubeListBuilder.create().texOffs(116,0).addBox(-1,-6,-1,2,6,2),PartPose.offsetAndRotation(sign*2.5F,-3.5F,-1,-.9F,0,sign*.2F));
		var tail1=body.addOrReplaceChild("tail1",CubeListBuilder.create().texOffs(0,44).addBox(-3.5F,-3,0,7,6,9),PartPose.offset(0,-1,10));
		tail1.addOrReplaceChild("spine",CubeListBuilder.create().texOffs(116,10).addBox(-.5F,-3,-1,1,3,2),PartPose.offsetAndRotation(0,-3,5,-.4F,0,0));
		var tail2=tail1.addOrReplaceChild("tail2",CubeListBuilder.create().texOffs(32,44).addBox(-2.5F,-2,0,5,4,9),PartPose.offset(0,0,9));
		tail2.addOrReplaceChild("spine",CubeListBuilder.create().texOffs(116,10).addBox(-.5F,-3,-1,1,3,2),PartPose.offsetAndRotation(0,-2,5,-.4F,0,0));
		var tail3=tail2.addOrReplaceChild("tail3",CubeListBuilder.create().texOffs(60,44).addBox(-1.5F,-1.5F,0,3,3,9),PartPose.offset(0,0,9));
		tail3.addOrReplaceChild("fin",CubeListBuilder.create().texOffs(84,44).addBox(-3,-.5F,0,6,1,4),PartPose.offset(0,0,8));
		for(int i=0;i<4;i++){
			boolean hind=i>=2;float x=(i%2==0?1:-1)*5,z=hind?HIND:FRONT;
			var cubes=hind?CubeListBuilder.create().texOffs(16,60).addBox(-2.5F,-1,-3,5,8,5):CubeListBuilder.create().texOffs(0,60).addBox(-2,-1,-2,4,8,4);
			var leg=body.addOrReplaceChild(new String[]{"legFL","legFR","legHL","legHR"}[i],cubes,PartPose.offset(x,2,z));
			var shin=leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(36,60).addBox(-1.5F,0,-1.5F,3,7,3),PartPose.offset(0,THIGH,0));
			shin.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(48,60).addBox(-2,6,-3,4,2,4),PartPose.ZERO);
		}
		// Each wing: an arm out to the wrist with the inner membrane trailing behind it, and the outer arm and membrane beyond.
		for(int sign:new int[]{1,-1}){
			var arm=body.addOrReplaceChild(sign>0?"wingL":"wingR",CubeListBuilder.create()
				.texOffs(0,76).addBox(sign>0?0:-ARM,-1,-1,ARM,2,2)
				.texOffs(0,82).addBox(sign>0?0:-ARM,0,0,ARM,0,11),PartPose.offset(sign*5,-4,-5));
			arm.addOrReplaceChild("outer",CubeListBuilder.create()
				.texOffs(40,76).addBox(sign>0?0:-9,-.5F,-.5F,9,1,1)
				.texOffs(40,82).addBox(sign>0?0:-9,0,0,9,0,10),PartPose.offset(sign*ARM,0,0));
		}
		return LayerDefinition.create(mesh,128,128);
	}

	@Override public void setupAnim(FrostWyrmlingRenderState s){
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
		if(s.deathTime<=0){
			float tell=Math.max(1,s.tell);
			// Off the ground through a pounce, along the arc it leaps, and through a bound, highest halfway.
			if(s.move==FrostWyrmlingRules.POUNCE&&s.elapsed>tell)
				body.y-=(float)FrostWyrmlingRules.arc((s.elapsed-tell)/FrostWyrmlingRules.POUNCE_TICKS)*16/FrostWyrmlingRenderer.SCALE;
			if(s.move==FrostWyrmlingRules.BOUND)body.y-=(s.side==0?7:4)*Mth.sin(Mth.PI*Mth.clamp(s.elapsed/tell,0,1));
		}
		// Rolled onto its side in death, it lies on its flank.
		if(s.deathTime>0)body.y=Mth.lerp(smooth(s.deathTime,6,18),body.y,24-5);
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: curled or standing (or between), walking, then the move itself. */
	private void pose(FrostWyrmlingRenderState s,int move,float el,int tell,int side){
		float r=s.rise,t=s.ageInTicks;
		java.util.Arrays.fill(uz,0);roll=0;yaw=0;fwd=0;
		curl(r);
		// Breathing, slow while it sleeps; its gaze followed once it is up, the turn shared down its neck; its tail never quite still.
		pitch+=Mth.sin(t*.06F)*.01F;n1X+=Mth.sin(t*.06F+.5F)*.03F;
		float look=Mth.clamp(s.yRot,-60,60)*Mth.DEG_TO_RAD*r;
		n1Y+=look*.3F;n2Y+=look*.3F;headY+=look*.2F;headX+=s.xRot*Mth.DEG_TO_RAD*.5F*r;
		tY+=Mth.sin(t*.08F)*.12F*(.4F+.6F*r);
		up+=Mth.sin(t*.05F)*.03F*r;
		// Walking: a diagonal trot, each foot lifted as it comes through, its head bobbing and its tail swinging with the stride.
		float walk=Math.min(1,s.walkAnimationSpeed)*r,step=s.walkAnimationPos*.6F;
		float sw=Mth.cos(step)*walk*.55F;
		u[0]+=sw;u[3]+=sw;u[1]-=sw;u[2]-=sw;
		l[0]+=Math.max(0,-Mth.sin(step))*walk*1.1F;l[3]+=Math.max(0,-Mth.sin(step))*walk*.7F;
		l[1]+=Math.max(0,Mth.sin(step))*walk*1.1F;l[2]+=Math.max(0,Mth.sin(step))*walk*.7F;
		n1X+=Mth.sin(step*2)*walk*.05F;tY+=Mth.cos(step)*walk*.15F;
		attack(move,el,tell,side);
		apply();
	}

	/**
	 * Curled to standing, {@code r} 0 to 1: its neck and tail unwind from round it while its head comes up off the snow; its legs
	 * come out from under it and it stands; then it throws its wings wide and its head back and roars, and folds them again.
	 */
	private void curl(float r){
		float unwind=smooth(r,0,.3F),lift=smooth(r,0,.5F),stand=smooth(r,.15F,.5F);
		float spread=smooth(r,.55F,.7F),refold=smooth(r,.88F,1),roar=Mth.sin(Mth.PI*smooth(r,.6F,.9F));
		u[0]=u[1]=Mth.lerp(stand,-1.5F,0);l[0]=l[1]=Mth.lerp(stand,3.0F,0);
		u[2]=u[3]=Mth.lerp(stand,-1.5F,HIND_U);l[2]=l[3]=Mth.lerp(stand,3.0F,HIND_L);
		pitch=-.12F*roar;
		n1X=Mth.lerp(lift,0,-.7F)-.35F*roar;n2X=Mth.lerp(lift,.2F,-.3F)-.2F*roar;
		headX=Mth.lerp(lift,.15F,0)-.5F*roar;jawX=.9F*roar;
		n1Y=n2Y=.9F*(1-unwind);headY=.5F*(1-unwind);
		t1X=Mth.lerp(lift,-.15F,-.45F);t2X=Mth.lerp(lift,0,-.2F);t3X=Mth.lerp(lift,0,.15F);tY=-.6F*(1-unwind);
		fold=1-spread*(1-refold);up=.5F*roar;
	}

	/**
	 * Its moves, laid over the standing pose, each wound up through its tell so it reads from across the hall, then thrown fast. The
	 * bite draws its neck back and lunges, jaws wide and snapped shut; the tail sweep coils its tail away and whips it round; the wing
	 * beat rears it up with its wings high and brings them down; the breath swells its chest, then lowers its head and breathes,
	 * wings half open; the pounce crouches, then stretches out in the air, wings spread; the bound tucks its legs, and the beat away
	 * spreads its wings for one great stroke; spent, its head hangs, its wings droop and its sides heave.
	 */
	private void attack(int move,float el,int tellTicks,int side){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case FrostWyrmlingRules.BITE->{
				float back=wind*(1-go),snap=go*(1-ramp(el,tell+1,1.5F));
				n1X+=-.3F*back+.75F*go;n2X+=-.2F*back+.35F*go;headX+=-.2F*back+.25F*go;pitch+=.1F*go;
				jawX+=.35F*back+snap;
				u[0]-=.2F*go;u[1]-=.2F*go;l[0]+=.2F*go;l[1]+=.2F*go;
			}
			case FrostWyrmlingRules.TAIL->{
				float coil=wind*(1-go),sweep=ramp(el,tell,4);
				tY+=side*.5F*coil-side*.7F*sweep;t1X+=.15F;
				yaw=side*.12F*coil-side*.35F*sweep;
				n1Y+=side*.4F*wind;headY+=side*.2F*wind;
				u[2]+=.15F*sweep;u[3]+=.15F*sweep;
			}
			case FrostWyrmlingRules.WING->{
				float rear=wind*(1-go);
				pitch+=-.55F*rear+.12F*go;n1X+=-.3F*rear+.3F*go;headX+=-.2F*rear+.1F*go;jawX+=.5F*rear;
				fold=Mth.lerp(wind,fold,0);up+=1.0F*rear-.7F*go;fwd+=-.3F*rear+.6F*go;
				u[0]=Mth.lerp(rear,u[0],-.7F);u[1]=Mth.lerp(rear,u[1],-.7F);l[0]=Mth.lerp(rear,l[0],1.2F);l[1]=Mth.lerp(rear,l[1],1.2F);
				u[2]+=.2F*rear;u[3]+=.2F*rear;
				t1X+=.3F*rear;
			}
			case FrostWyrmlingRules.BREATH->{
				float draw=wind*(1-go),b=ramp(el,tell,3);
				pitch+=-.15F*draw+.05F*b;n1X+=-.35F*draw+.3F*b;n2X+=-.1F*draw+.2F*b;headX+=-.25F*draw+.2F*b;
				jawX+=.15F*draw+.85F*b;n2Y+=Mth.sin(el*.35F)*.08F*b;
				fold=Mth.lerp(wind,fold,.45F);up-=.1F*b;
				u[0]-=.15F*b;u[1]-=.15F*b;
			}
			case FrostWyrmlingRules.POUNCE->{
				float air=el>tell?ramp(el,tell,2):0,crouch=wind*(1-air);
				float p=Mth.clamp((el-tell)/FrostWyrmlingRules.POUNCE_TICKS,0,1);
				u[0]-=.5F*crouch;u[1]-=.5F*crouch;l[0]+=1.1F*crouch;l[1]+=1.1F*crouch;
				u[2]-=.6F*crouch;u[3]-=.6F*crouch;l[2]+=1.2F*crouch;l[3]+=1.2F*crouch;
				pitch+=.08F*crouch;n1X+=.25F*crouch;headX-=.1F*crouch;t1X+=.3F*crouch;tY+=Mth.sin(el*1.5F)*.15F*crouch;
				fold=Mth.lerp(crouch,fold,.6F);
				// Stretched out in the air, forefeet reaching and hind legs trailing, nose up as it rises and down as it comes down.
				u[0]=Mth.lerp(air,u[0],-1.0F);u[1]=Mth.lerp(air,u[1],-1.0F);l[0]=Mth.lerp(air,l[0],.2F);l[1]=Mth.lerp(air,l[1],.2F);
				u[2]=Mth.lerp(air,u[2],.9F);u[3]=Mth.lerp(air,u[3],.9F);l[2]=Mth.lerp(air,l[2],.3F);l[3]=Mth.lerp(air,l[3],.3F);
				pitch+=air*(-.25F+.5F*p);n1X-=.2F*air;
				fold=Mth.lerp(air,fold,0);up+=air*.4F*Mth.cos(p*Mth.PI);
			}
			case FrostWyrmlingRules.BOUND->{
				float p=Mth.clamp(el/tell,0,1),tuck=Mth.sin(Mth.PI*p);
				u[0]-=.7F*tuck;u[1]-=.7F*tuck;l[0]+=1.3F*tuck;l[1]+=1.3F*tuck;
				u[2]-=.5F*tuck;u[3]-=.5F*tuck;l[2]+=.9F*tuck;l[3]+=.9F*tuck;
				if(side==0){
					// One great beat of its wings, up and then down, carrying it back.
					pitch-=.3F*tuck;n1X-=.2F*tuck;
					fold=Mth.lerp(tuck,fold,0);up+=(1-2*p)*tuck;
				}else{
					roll=side*.3F*tuck;yaw=-side*.2F*tuck;for(int i=0;i<4;i++)uz[i]=side*.35F*tuck;n1Y-=side*.3F*tuck;
					fold=Mth.lerp(tuck,fold,.5F);up+=.3F*tuck;
				}
			}
			case FrostWyrmlingRules.RECOVER->{
				float k=ramp(el,0,4),heave=Mth.sin(el*.5F);
				n1X+=.55F*k;n2X+=.2F*k;headX+=.3F*k;jawX+=(.25F+.1F*heave)*k;
				pitch+=.05F*k+.02F*heave*k;roll=.03F*heave*k;
				fold=Mth.lerp(k,fold,.55F);up-=.35F*k;
				uz[0]=.12F*k;uz[1]=-.12F*k;uz[2]=.1F*k;uz[3]=-.1F*k;
				t1X-=.1F*k;
			}
			default->{}
		}
	}

	/** Sets the parts from the pose built: each leg's world angle turned into one from the body, and the head and tail held against the body's pitch. */
	private void apply(){
		body.xRot=pitch;body.zRot=roll;body.yRot=yaw;
		neck1.xRot=n1X;neck1.yRot=n1Y;neck2.xRot=n2X;neck2.yRot=n2Y;
		head.xRot=headX-pitch-n1X-n2X;head.yRot=headY;jaw.xRot=jawX;
		tail1.xRot=t1X-pitch;tail2.xRot=t2X;tail3.xRot=t3X;tail1.yRot=tY;tail2.yRot=tY;tail3.yRot=tY*1.2F;
		for(int i=0;i<4;i++){legs[i].xRot=u[i]-pitch;legs[i].zRot=uz[i];shins[i].xRot=l[i];}
		float f=Mth.clamp(fold,0,1);
		float ax=Mth.lerp(f,0,-1.4F),ay=Mth.lerp(f,-.2F,-1.3F)+fwd,az=Mth.lerp(f,-.4F,-.25F)-up,oz=Mth.lerp(f,-.1F,3.0F)-.3F*up*(1-f);
		for(int i=0;i<2;i++){
			float sign=i==0?1:-1;
			arms[i].xRot=ax;arms[i].yRot=sign*ay;arms[i].zRot=sign*az;outers[i].zRot=sign*oz;
		}
	}

	/**
	 * Sets the body's height so whatever reaches lowest just touches the floor: a foot or a knee, or its belly while it lies curled.
	 * However its legs are bent and however it is pitched, it stands (or lies) on the ground rather than floating or sinking.
	 */
	private void ground(){
		float b=body.xRot,low=0;
		for(float y:new float[]{-4,4})for(float z:new float[]{-10,10})low=Math.max(low,lift(y,z,b));
		for(int i=0;i<4;i++){
			float zp=i<2?FRONT:HIND,a=legs[i].xRot,c=a+shins[i].xRot;
			float ky=2+THIGH*Mth.cos(a),kz=zp+THIGH*Mth.sin(a);
			float fy=ky+SHIN*Mth.cos(c)+2*Math.abs(Mth.sin(c)),fz=kz+SHIN*Mth.sin(c);
			low=Math.max(low,Math.max(lift(ky+1.5F,kz,b),lift(fy,fz,b)));
		}
		body.y=24-low;
	}
	/** How far below the body's pivot a point in the body (at {@code y}, {@code z}) sits, the body pitched {@code b}. */
	private static float lift(float y,float z,float b){return y*Mth.cos(b)-z*Mth.sin(b);}

	/** A blow it takes: it flinches, its head jerked up and its wings twitching. */
	private void hurt(FrostWyrmlingRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		neck1.xRot-=.2F*k;head.xRot-=.1F*k;body.zRot+=Mth.sin(s.hurt*18)*.04F*k;
		arms[0].zRot-=.2F*k;arms[1].zRot+=.2F*k;
	}

	/**
	 * Dying: its legs give and it sinks onto its belly, then rolls onto its side, legs slack, neck stretched out along the snow, the
	 * wing beneath it folded and the one on top flopped open over it.
	 */
	private void die(FrostWyrmlingRenderState s){
		if(s.deathTime<=0)return;
		float buckle=smooth(s.deathTime,0,7),fall=smooth(s.deathTime,6,18);
		body.xRot=Mth.lerp(buckle,body.xRot,.05F);body.yRot*=1-buckle;
		body.zRot=Mth.lerp(fall,Mth.lerp(buckle,body.zRot,0),1.45F);
		for(int i=0;i<4;i++){
			legs[i].xRot=Mth.lerp(fall,Mth.lerp(buckle,legs[i].xRot,-1.4F),-.3F+.15F*i);
			shins[i].xRot=Mth.lerp(fall,Mth.lerp(buckle,shins[i].xRot,2.8F),.4F);
			legs[i].zRot=Mth.lerp(fall,legs[i].zRot,-.25F);
		}
		neck1.xRot=Mth.lerp(buckle,neck1.xRot,.15F);neck1.yRot=Mth.lerp(fall,neck1.yRot*(1-buckle),.25F);
		neck2.xRot=Mth.lerp(buckle,neck2.xRot,.1F);neck2.yRot=Mth.lerp(fall,neck2.yRot*(1-buckle),.2F);
		head.xRot=Mth.lerp(buckle,head.xRot,-.2F);head.yRot*=1-buckle;
		jaw.xRot=Mth.lerp(fall,jaw.xRot,.35F);
		tail1.xRot=Mth.lerp(buckle,tail1.xRot,0);tail2.xRot=Mth.lerp(buckle,tail2.xRot,0);tail3.xRot=Mth.lerp(buckle,tail3.xRot,0);
		tail1.yRot=Mth.lerp(fall,tail1.yRot,.3F);tail2.yRot=Mth.lerp(fall,tail2.yRot,.3F);tail3.yRot=Mth.lerp(fall,tail3.yRot,.4F);
		// The wing it lies on, folded under it; the one on top, flopped open over its side.
		arms[0].xRot=Mth.lerp(buckle,arms[0].xRot,-1.4F);arms[0].yRot=Mth.lerp(buckle,arms[0].yRot,-1.3F);arms[0].zRot=Mth.lerp(buckle,arms[0].zRot,-.25F);
		outers[0].zRot=Mth.lerp(buckle,outers[0].zRot,3.0F);
		arms[1].xRot=Mth.lerp(fall,arms[1].xRot,-.2F);arms[1].yRot=Mth.lerp(fall,arms[1].yRot,.3F);arms[1].zRot=Mth.lerp(fall,arms[1].zRot,1.5F);
		outers[1].zRot=Mth.lerp(fall,outers[1].zRot,.4F);
	}
}
