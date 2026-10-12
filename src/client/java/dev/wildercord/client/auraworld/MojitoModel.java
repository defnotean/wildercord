package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.MojitoRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * Mojito: a giant black standard poodle, his coat in tight curls, a white bib down his chest and a white beard under his chin, a gold
 * chain collar, a poofed topknot, long floppy ears, a curly pom on an upturned tail and a pink tongue hanging out the side of his
 * mouth; and in his mouth, held crosswise, a greatsword, its blade out past his right side. He sleeps curled with the sword lying
 * before his paws; waking, he lifts his head, dips it to take the sword up, gets to his feet and shakes his coat out, and barks. Every
 * leg bends at the knee, each move blends into the next, a blow jolts him, and dying his legs buckle and he rolls onto his side.
 */
public final class MojitoModel extends EntityModel<MojitoRenderState> {
	/** Where each leg hangs from the body, front to back, and the angles (from straight down) a hind thigh and shin stand at. */
	private static final float FRONT=-6.5F,HIND=6.5F,HIND_U=-.3F,HIND_L=.6F;
	/** How long the thigh is to the knee, and the shin and paw below it. */
	private static final float THIGH=6,FOOT=8;
	/** How far through getting up he takes the sword up, and starts shaking his coat out. */
	private static final float GRAB=MojitoRules.GRAB_AT/(float)MojitoRules.RISE_TICKS,SHAKE=MojitoRules.SHAKE_AT/(float)MojitoRules.RISE_TICKS;
	private final ModelPart body,neck,head,jaw,tongue,earL,earR,tail,sword,fallen,orbL,orbR;
	private final ModelPart[] legs=new ModelPart[4],shins=new ModelPart[4];
	private final ModelPart[] posed;
	/** The pose being built: each leg's thigh angle in the world and shin angle from the thigh, the body, the neck, the head and so on. */
	private final float[] u=new float[4],l=new float[4],uz=new float[4];
	private float pitch,roll,yaw,neckX,neckY,headX,headZ,jawX,tongueX,earX,earZ,tailX,tailY,lying;
	public MojitoModel(ModelPart root){
		super(root);body=root.getChild("body");neck=body.getChild("neck");head=neck.getChild("head");jaw=head.getChild("jaw");tongue=jaw.getChild("tongue");
		earL=head.getChild("earL");earR=head.getChild("earR");tail=body.getChild("tail");sword=head.getChild("sword");fallen=root.getChild("fallen");
		orbL=body.getChild("orbL");orbR=body.getChild("orbR");
		String[] names={"legFL","legFR","legHL","legHR"};
		for(int i=0;i<4;i++){legs[i]=body.getChild(names[i]);shins[i]=legs[i].getChild("shin");}
		posed=new ModelPart[]{body,neck,head,jaw,tongue,earL,earR,tail,legs[0],legs[1],legs[2],legs[3],shins[0],shins[1],shins[2],shins[3]};
	}
	private static CubeListBuilder box(int tu,int tv,float x,float y,float z,float w,float h,float d){return CubeListBuilder.create().texOffs(tu,tv).addBox(x,y,z,w,h,d);}
	/** The greatsword, its grip across the mouth, the guard at his right cheek and the blade running out past his right side. */
	private static CubeListBuilder blade(){
		return CubeListBuilder.create().texOffs(72,45).addBox(-3,-.5F,-.5F,6,1,1).texOffs(88,45).addBox(-4,-2,-1,1,4,2).texOffs(96,45).addBox(3,-1,-1,2,2,2)
			.texOffs(0,60).addBox(-28,-1.5F,-.5F,24,3,1).texOffs(52,60).addBox(-31,-1,-.5F,3,2,1);
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",box(0,0,-4.5F,-4.5F,-9,9,9,18),PartPose.offset(0,7,0));
		body.addOrReplaceChild("bib",box(60,0,-3.5F,-3,-10,7,8,2),PartPose.ZERO);
		var neck=body.addOrReplaceChild("neck",box(84,0,-3,-9,-3,6,10,6),PartPose.offset(0,-2.5F,-7));
		neck.addOrReplaceChild("collar",box(84,16,-3.5F,-1.5F,-3.5F,7,2,7),PartPose.ZERO);
		var head=neck.addOrReplaceChild("head",box(0,30,-4,-6,-5,8,7,8),PartPose.offset(0,-8,0));
		head.addOrReplaceChild("topknot",box(32,30,-3.5F,-9.5F,-4,7,4,7),PartPose.ZERO);
		head.addOrReplaceChild("snout",box(60,30,-2,-3,-10,4,4,5),PartPose.ZERO);
		var jaw=head.addOrReplaceChild("jaw",box(78,30,-1.75F,0,-4.5F,3.5F,1.5F,4.5F),PartPose.offset(0,1,-5));
		jaw.addOrReplaceChild("beard",box(78,37,-1.5F,1.5F,-4.5F,3,2,3),PartPose.ZERO);
		jaw.addOrReplaceChild("tongue",box(108,30,-1,0,-.5F,2,3,1),PartPose.offsetAndRotation(1.6F,.5F,-2.5F,0,0,-.15F));
		head.addOrReplaceChild("earL",box(94,30,-1,0,-2,2,8,4),PartPose.offset(4.2F,-5,-.5F));
		head.addOrReplaceChild("earR",box(94,30,-1,0,-2,2,8,4),PartPose.offset(-4.2F,-5,-.5F));
		head.addOrReplaceChild("sword",blade(),PartPose.offset(0,-.5F,-7.5F));
		// The same sword lying flat in the grass before his paws while he sleeps.
		root.addOrReplaceChild("fallen",blade(),PartPose.offsetAndRotation(10,23.5F,-22,Mth.HALF_PI,.12F,0));
		for(int i=0;i<4;i++){
			float x=(i%2==0?1:-1)*2.8F,z=i>=2?HIND:FRONT;
			var leg=body.addOrReplaceChild(new String[]{"legFL","legFR","legHL","legHR"}[i],box(0,45,-2,-1,-2,4,7,4),PartPose.offset(x,3,z));
			var shin=leg.addOrReplaceChild("shin",box(16,45,-1.5F,0,-1.5F,3,6,3),PartPose.offset(0,THIGH,0));
			shin.addOrReplaceChild("paw",box(28,45,-2,6,-2.5F,4,2,4),PartPose.ZERO);
		}
		var tail=body.addOrReplaceChild("tail",box(44,45,-1.5F,-6,-1.5F,3,6,3),PartPose.offset(0,-3.5F,8.5F));
		tail.addOrReplaceChild("pom",box(56,45,-2,-4,-2,4,4,4),PartPose.offset(0,-6,0));
		// The magic he gathers at his sides to fire off.
		body.addOrReplaceChild("orbL",box(64,60,-2,-2,-2,4,4,4),PartPose.offset(16,-7,-3));
		body.addOrReplaceChild("orbR",box(64,60,-2,-2,-2,4,4,4),PartPose.offset(-16,-7,-3));
		return LayerDefinition.create(mesh,128,128);
	}

	@Override public void setupAnim(MojitoRenderState s){
		super.setupAnim(s);
		pose(s,s.move,s.elapsed,s.tell,s.side);
		float lie=lying;
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
			lie=Mth.lerp(k,lying,lie);
		}
		hurt(s);
		die(s);
		ground();
		// Rolled onto his side, flopped out or dead, he lies on his flank.
		float down=Math.max(lie,s.deathTime>0?smooth(s.deathTime,6,18):0);
		if(down>0)body.y=Mth.lerp(down,body.y,24-4.5F);
		props(s);
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: curled asleep or standing (or between), walking, then the move itself. */
	private void pose(MojitoRenderState s,int move,float el,int tell,int side){
		float r=s.rise,t=s.ageInTicks;
		java.util.Arrays.fill(uz,0);roll=0;yaw=0;headZ=0;jawX=0;earZ=0;tailY=0;lying=0;
		curl(r,t);
		// Breathing, slow and deep asleep, and his gaze followed once he is up.
		pitch+=Mth.sin(t*.07F)*.01F;neckX+=Mth.sin(t*.07F+.5F)*.025F;
		neckY+=Mth.clamp(s.yRot,-60,60)*Mth.DEG_TO_RAD*.5F*r;headX+=s.xRot*Mth.DEG_TO_RAD*.4F*r;
		// The tongue lolling, and the tail going.
		tongueX=Mth.sin(t*.15F)*.08F;tailY+=Mth.sin(t*.3F)*.2F*r;
		// Walking: a springy trot, diagonal pairs together, the head bobbing and the ears bouncing with it.
		float walk=Math.min(1,s.walkAnimationSpeed)*r,step=s.walkAnimationPos*.7F;
		float sw=Mth.cos(step)*walk*.55F;
		u[0]+=sw;u[3]+=sw;u[1]-=sw;u[2]-=sw;
		l[0]+=Math.max(0,-Mth.sin(step))*walk*1.0F;l[3]+=Math.max(0,-Mth.sin(step))*walk*.5F;
		l[1]+=Math.max(0,Mth.sin(step))*walk*1.0F;l[2]+=Math.max(0,Mth.sin(step))*walk*.5F;
		neckX+=Mth.sin(step*2)*walk*.05F;earX+=Mth.sin(step*2+1)*walk*.15F;tailY+=Mth.sin(step)*walk*.3F;
		attack(move,el,tell,side,t);
		apply();
	}
	/**
	 * Curled asleep to standing, {@code r} 0 to 1: lying on his belly, forelegs out in front, hind legs folded under and his chin on his
	 * paws, tail curled round; he lifts his head, dips it to the sword and takes it up, gets his hind legs then his forelegs under him,
	 * shakes his coat out from his ears to his tail, and barks round the sword.
	 */
	private void curl(float r,float t){
		float lift=smooth(r,0,.25F),dip=Mth.sin(Mth.PI*smooth(r,.26F,GRAB+.08F)),hind=smooth(r,GRAB+.04F,.52F),fore=smooth(r,.48F,SHAKE-.02F);
		float shake=Mth.sin(Mth.PI*smooth(r,SHAKE,SHAKE+.2F)),bark=Mth.sin(Mth.PI*smooth(r,.88F,1));
		u[0]=u[1]=Mth.lerp(fore,-1.45F,0);l[0]=l[1]=Mth.lerp(fore,.1F,0);
		u[2]=u[3]=Mth.lerp(hind,-1.2F,HIND_U);l[2]=l[3]=Mth.lerp(hind,2.7F,HIND_L);
		// Up on his haunches before his forelegs come under him, and lying a little over on one hip while asleep.
		pitch=.25F*hind*(1-fore);roll=.18F*(1-hind);
		neckX=Mth.lerp(lift,1.35F,.35F)+.75F*dip;neckY=Mth.lerp(lift,.4F,0);
		headX=Mth.lerp(lift,.3F,0)+.35F*dip;
		tailX=Mth.lerp(fore,-1.7F,-.45F);tailY=.9F*(1-fore);
		earX=0;
		// The shake: a roll running down him, his head going hardest, ears and tail flying.
		float w=Mth.sin(t*2.4F);
		roll+=w*.13F*shake;neckY+=Mth.sin(t*2.4F+.7F)*.55F*shake;headZ+=Mth.sin(t*2.4F+1.2F)*.35F*shake;
		earZ+=Math.abs(Mth.sin(t*2.4F+1.6F))*.9F*shake;tailY+=Mth.sin(t*2.4F+2.2F)*.6F*shake;
		neckX-=.35F*bark;headX-=.15F*bark;jawX+=.25F*bark;
	}

	/**
	 * His moves, laid over the standing pose, each wound up through its tell so it reads from across the grove, then thrown fast. The
	 * slash turns his head right round to one side and sweeps the blade back across; the spin crouches him low, blade level; the
	 * leap sits him back on his haunches, then stretches him out in the air; the bolts lift his head as the magic swells at his
	 * sides; the hop tucks his legs up; spent, he pants; the zoomies bow him down, rump up and tail going, then gallop him flat out;
	 * flopped, he lies on his side with his tongue out.
	 */
	private void attack(int move,float el,int tellTicks,int side,float t){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case MojitoRules.WATCH->tailY+=Mth.sin(t*.55F)*.2F;
			case MojitoRules.SLASH->{
				// Head wrenched round to one side, the blade drawn back, his weight on the far legs; then flung across to the other.
				float sw=side*Mth.lerp(go,.95F*wind,-1.05F);
				neckY+=sw;yaw+=.3F*sw;headZ-=.3F*sw;neckX+=.15F*wind;
				uz[0]=uz[2]=-.1F*sw;uz[1]=uz[3]=-.1F*sw;roll-=.06F*sw;
				u[0]-=.1F*wind;u[1]-=.1F*wind;tailY+=.4F*sw;
			}
			case MojitoRules.SPIN->{
				// Crouched low on all four legs, head down and the blade levelled out to his right ahead of him, and kept there round the spin.
				float k=wind;
				u[0]-=.35F*k;u[1]-=.35F*k;l[0]+=.75F*k;l[1]+=.75F*k;
				u[2]-=.25F*k;u[3]-=.25F*k;l[2]+=.6F*k;l[3]+=.6F*k;
				uz[0]=.15F*k;uz[1]=-.15F*k;uz[2]=.15F*k;uz[3]=-.15F*k;
				neckX+=.35F*k;headX-=.2F*k;neckY-=.45F*k;roll-=.12F*go;
				earZ+=.6F*go;tailX-=.5F*go;
			}
			case MojitoRules.LEAP->{
				float air=Mth.clamp((el-tell-1)/MojitoRules.LEAP_TICKS,0,1),flying=el>tell?Mth.sin(Mth.PI*air):0;
				// Sat back on his haunches, forequarters up and head high; then stretched out flat in the air, forelegs reaching.
				float sit=wind*(1-go);
				u[2]-=.7F*sit;u[3]-=.7F*sit;l[2]+=1.4F*sit;l[3]+=1.4F*sit;
				u[0]+=.15F*sit;u[1]+=.15F*sit;pitch-=.3F*sit;neckX-=.25F*sit;
				u[0]=Mth.lerp(flying,u[0],-1.15F);u[1]=Mth.lerp(flying,u[1],-1.05F);l[0]=Mth.lerp(flying,l[0],.3F);l[1]=Mth.lerp(flying,l[1],.3F);
				u[2]=Mth.lerp(flying,u[2],1.15F);u[3]=Mth.lerp(flying,u[3],1.05F);l[2]=Mth.lerp(flying,l[2],-.2F);l[3]=Mth.lerp(flying,l[3],-.2F);
				pitch+=Mth.lerp(air,-.25F,.3F)*flying;earX-=.9F*flying;tailX-=.6F*flying;
			}
			case MojitoRules.BOLTS->{
				// Head up and feet planted wide as the magic swells at his sides, jolted a little with every volley he looses.
				float since=el>tell?(el-tell)%MojitoRules.BOLT_EVERY:99,kick=el>tell?Math.max(0,1-since/3):0;
				neckX-=.35F*wind;headX-=.15F*wind;
				uz[0]=.18F*wind;uz[1]=-.18F*wind;uz[2]=.15F*wind;uz[3]=-.15F*wind;
				pitch-=.06F*kick;neckX-=.1F*kick;earZ+=.3F*wind+.3F*kick;
			}
			case MojitoRules.HOP->{
				// Legs tucked up under him in the air; sideways, he leans away from the hop; backwards, his forehand lifts.
				float p=Mth.clamp(el/Math.max(1,MojitoRules.HOP_TICKS),0,1),tuck=Mth.sin(Mth.PI*p);
				u[0]-=.6F*tuck;u[1]-=.6F*tuck;l[0]+=1.2F*tuck;l[1]+=1.2F*tuck;
				u[2]-=.4F*tuck;u[3]-=.4F*tuck;l[2]+=.7F*tuck;l[3]+=.7F*tuck;
				if(side==0){pitch-=.25F*tuck;neckX-=.2F*tuck;}
				else{roll=side*.3F*tuck;yaw=-side*.2F*tuck;for(int i=0;i<4;i++)uz[i]=side*.3F*tuck;}
				earX-=.6F*tuck;
			}
			case MojitoRules.RECOVER->{
				// Spent: head low, tongue right out and panting, his sides heaving.
				float k=ramp(el,0,4),pant=Mth.sin(el*1.4F);
				neckX+=.45F*k;headX-=.1F*k;pitch+=.03F*k+.02F*pant*k;roll+=.02F*pant*k;
				jawX+=.2F*k;tongueX=(.25F+.2F*pant)*k;
				uz[0]=-.1F*k;uz[1]=.1F*k;tailX-=.25F*k;earX+=.1F*k;
			}
			case MojitoRules.ZOOMIES->{
				if(el<=tell){
					// The play bow: chest down on his forelegs, rump in the air and tail going wild.
					u[0]-=1.3F*wind;u[1]-=1.3F*wind;l[0]+=.2F*wind;l[1]+=.2F*wind;
					pitch+=.45F*wind;neckX-=.3F*wind;headX+=.1F*wind;
					tailY+=Mth.sin(t*1.6F)*.7F*wind;tailX-=.2F*wind;earZ+=.3F*wind;jawX+=.1F*wind;
				}else{
					// Flat out round and round, forelegs together and hind legs together, ears flying and tongue flapping.
					float a=(el-tell)*1.5F,c=Mth.sin(a);
					u[0]=c*.9F;u[1]=c*.9F-.2F;l[0]=l[1]=Math.max(0,-Mth.cos(a))*1.2F;
					u[2]=HIND_U-c*.9F;u[3]=HIND_U-c*.9F+.2F;l[2]=l[3]=HIND_L+Math.max(0,Mth.cos(a))*.6F;
					pitch+=c*.1F;neckX+=.2F-c*.08F;earX-=1.0F;tailX-=.7F;tailY+=Mth.sin(a*2)*.3F;
					jawX+=.15F;tongueX=.6F+Mth.sin(a*2)*.3F;
					roll-=.15F;
				}
			}
			case MojitoRules.FLOP->{
				// Flopped over on his side, legs out, tongue lolling, panting, his tail thumping the ground.
				float k=ramp(el,0,6),pant=Mth.sin(el*1.5F);
				lying=k;roll=Mth.lerp(k,roll,1.45F);
				for(int i=0;i<4;i++){u[i]=Mth.lerp(k,u[i],i<2?-.5F:.4F);l[i]=Mth.lerp(k,l[i],.25F);uz[i]=Mth.lerp(k,uz[i],-.2F);}
				neckX=Mth.lerp(k,neckX,.6F);neckY=Mth.lerp(k,neckY,-.3F);headZ-=.2F*k;
				pitch=Mth.lerp(k,pitch,.02F*pant);jawX+=.25F*k;tongueX=(.35F+.2F*pant)*k;
				tailX=Mth.lerp(k,tailX,-1.4F);tailY+=Math.abs(Mth.sin(el*.6F))*.5F*k;
			}
			default->{}
		}
	}

	/** Sets the parts from the pose built: each leg's world angle turned into one from the body, and the head held against the neck. */
	private void apply(){
		body.xRot=pitch;body.zRot=roll;body.yRot=yaw;
		neck.xRot=neckX;neck.yRot=neckY;
		head.xRot=-neckX-pitch+headX;head.zRot=headZ;
		jaw.xRot=jawX;tongue.xRot=tongueX;
		earL.xRot=earR.xRot=earX;earL.zRot=-.1F-earZ;earR.zRot=.1F+earZ;
		tail.xRot=tailX;tail.yRot=tailY;
		for(int i=0;i<4;i++){legs[i].xRot=u[i]-pitch;legs[i].zRot=uz[i];shins[i].xRot=l[i];}
	}

	/**
	 * Sets the body's height so whatever reaches lowest just touches the floor: a paw or a knee, or his belly while he lies curled.
	 * However his legs are bent and however he is pitched, he stands (or lies) on the ground rather than floating or sinking.
	 */
	private void ground(){
		float b=body.xRot,low=0;
		for(float y:new float[]{-4.5F,4.5F})for(float z:new float[]{-9,9})low=Math.max(low,lift(y,z,b));
		for(int i=0;i<4;i++){
			float zp=i<2?FRONT:HIND,a=legs[i].xRot,c=a+shins[i].xRot;
			float ky=3+THIGH*Mth.cos(a),kz=zp+THIGH*Mth.sin(a);
			float hy=ky+FOOT*Mth.cos(c)+2*Math.abs(Mth.sin(c)),hz=kz+FOOT*Mth.sin(c);
			low=Math.max(low,Math.max(lift(ky+1.5F,kz,b),lift(hy,hz,b)));
		}
		body.y=24-low;
	}
	/** How far below the body's pivot a point in the body (at {@code y}, {@code z}) sits, the body pitched {@code b}. */
	private static float lift(float y,float z,float b){return y*Mth.cos(b)-z*Mth.sin(b);}

	/** A blow he takes: he flinches, head tossed up and ears flung back, his body jolted. */
	private void hurt(MojitoRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		neck.xRot-=.25F*k;head.xRot-=.1F*k;body.zRot+=Mth.sin(s.hurt*18)*.04F*k;earL.xRot-=.5F*k;earR.xRot-=.5F*k;
	}

	/** Dying: his legs buckle under him and he sinks onto his belly, then rolls onto his side, legs gone slack, the sword still held. */
	private void die(MojitoRenderState s){
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
		tail.xRot=Mth.lerp(buckle,tail.xRot,-1.5F);tongue.xRot=Mth.lerp(fall,tongue.xRot,.5F);
	}

	/**
	 * What shows besides the dog: the sword in his mouth from the moment he takes it up (lying in the grass until then), his tongue
	 * once his head is up, and through the bolts the magic swelling at each side, pulsing with each volley and gone after the last.
	 */
	private void props(MojitoRenderState s){
		boolean held=s.rise>=GRAB;
		sword.visible=held;fallen.visible=!held;
		tongue.visible=s.rise>.2F;
		float size=0;
		if(s.move==MojitoRules.BOLTS&&s.deathTime<=0){
			float tell=Math.max(1,s.tell),el=s.elapsed,end=tell+MojitoRules.BOLT_VOLLEYS*MojitoRules.BOLT_EVERY;
			float since=el>tell?(el-tell)%MojitoRules.BOLT_EVERY:99;
			size=ramp(el,0,tell)*(1-smooth(el,end-2,end+4))*(1+.35F*Math.max(0,1-since/3));
		}
		for(ModelPart orb:new ModelPart[]{orbL,orbR}){
			orb.visible=size>.03F;
			orb.xScale=orb.yScale=orb.zScale=size;
			orb.yRot=s.ageInTicks*.25F;orb.xRot=s.ageInTicks*.17F;
			orb.y=-7+Mth.sin(s.ageInTicks*.2F)*.6F;
		}
	}
}
