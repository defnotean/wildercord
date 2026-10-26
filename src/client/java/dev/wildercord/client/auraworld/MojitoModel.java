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
 * Thrown for him to fetch, the sword leaves his mouth and whirls about where it flies.
 */
public final class MojitoModel extends EntityModel<MojitoRenderState> {
	/** Where each leg hangs from the body, front to back, and the angles (from straight down) a hind thigh and shin stand at. */
	private static final float FRONT=-6.5F,HIND=6.5F,HIND_U=-.3F,HIND_L=.6F;
	/** How long the thigh is to the knee, and the shin and paw below it. */
	private static final float THIGH=6,FOOT=8;
	/** How far through getting up he takes the sword up, and starts shaking his coat out. */
	private static final float GRAB=MojitoRules.GRAB_AT/(float)MojitoRules.RISE_TICKS,SHAKE=MojitoRules.SHAKE_AT/(float)MojitoRules.RISE_TICKS;
	private final ModelPart body,neck,head,jaw,tongue,earL,earR,tail,sword,fallen,orbL,orbR,topknot,pom,beard,collar;
	private final ModelPart[] legs=new ModelPart[4],shins=new ModelPart[4];
	private final ModelPart[] posed;
	private final PoseBlend blender;
	/** The pose being built: each leg's thigh angle in the world and shin angle from the thigh, the body, the neck, the head and so on. */
	private final float[] u=new float[4],l=new float[4],uz=new float[4];
	private float pitch,roll,yaw,neckX,neckY,headX,headZ,jawX,tongueX,earX,earZ,tailX,tailY,lying;
	public MojitoModel(ModelPart root){
		super(root);body=root.getChild("body");neck=body.getChild("neck");head=neck.getChild("head");jaw=head.getChild("jaw");tongue=jaw.getChild("tongue");
		earL=head.getChild("earL");earR=head.getChild("earR");tail=body.getChild("tail");sword=head.getChild("sword");fallen=root.getChild("fallen");
		orbL=body.getChild("orbL");orbR=body.getChild("orbR");
		topknot=head.getChild("topknot");pom=tail.getChild("pom");beard=jaw.getChild("beard");collar=neck.getChild("collar");
		String[] names={"legFL","legFR","legHL","legHR"};
		for(int i=0;i<4;i++){legs[i]=body.getChild(names[i]);shins[i]=legs[i].getChild("shin");}
		posed=new ModelPart[]{body,neck,head,jaw,tongue,earL,earR,tail,legs[0],legs[1],legs[2],legs[3],shins[0],shins[1],shins[2],shins[3]};blender=new PoseBlend(posed);
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
		pose(s,s.move,hold(s.move,s.elapsed,s.tell),s.tell,s.side);
		float lie=lying;
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1){
			blender.store();
			root().resetPose();
			pose(s,s.prevMove,hold(s.prevMove,s.prevElapsed,s.prevTell),s.prevTell,s.side);
			float k=s.blend*s.blend*(3-2*s.blend);
			blender.mix(k);
			lie=Mth.lerp(k,lying,lie);
		}
		secondary(s);
		hurt(s);
		die(s);
		ground();
		// Rolled onto his side, flopped out or dead, he lies on his flank.
		float down=Math.max(lie,s.deathTime>0?smooth(s.deathTime,6,18):0);
		if(down>0)body.y=Mth.lerp(down,body.y,24-4.5F);
		// The flank lands with a small bounce, then lies still.
		if(s.deathTime>17)body.y-=2.0F*(float)(Math.exp(-.5F*(s.deathTime-17))*Math.abs(Mth.sin((s.deathTime-17)*.9F)));
		props(s);
	}

	/** The hit-stop: the chop holds a beat on the ground with the blade stuck before the tugging starts. */
	private static float hold(int move,float el,int tell){
		if(move!=MojitoRules.CHOP)return el;
		float at=Math.max(1,tell)+2;
		return el<at?el:el<at+1.5F?at:el-1.5F;
	}

	/**
	 * What swings loose from the body: the topknot, the tail's pom, the beard and the blade in his jaws each trail what carries them by
	 * a different phase, so they whip after a turn and settle; his head and neck run on past a blow and ring out; his breath, his coat
	 * and his weight each have their own period.
	 */
	private void secondary(MojitoRenderState s){
		float t=s.ageInTicks,r=s.rise;
		float walk=Math.min(1,s.walkAnimationSpeed)*r,step=s.walkAnimationPos*.7F;
		boolean moving=s.move!=0&&s.tell>0;
		float since=moving?s.elapsed-s.tell:-1;
		float ring=since>=0?(float)(Math.exp(-.3F*since)*Mth.sin(since*.95F)):0;
		// Topknot: bobs a beat behind the stride, tips against the head's turn, and bounces off a blow.
		topknot.xRot=Mth.sin(step*2-1.1F)*.12F*walk+Mth.sin(t*.05F+.6F)*.03F-.25F*ring;
		topknot.zRot=-neck.yRot*.25F+Mth.sin(t*.037F)*.04F;
		// Pom: swings out behind a stride and a wag, a beat after the tail carrying it.
		pom.xRot=Mth.sin(step-1.5F)*.35F*walk+Mth.sin(t*.1F-1.2F)*.1F+.5F*ring;
		pom.zRot=-tail.yRot*.6F+Mth.sin(t*.3F-1.0F)*.2F*r;
		// Beard and collar swing under the chin with the head.
		beard.xRot=Mth.sin(step*2-.5F)*.15F*walk+Mth.sin(t*.07F+1.0F)*.04F+.3F*Math.max(0,head.xRot);
		collar.zRot=Mth.sin(t*.06F+2.0F)*.015F;
		// The greatsword in his jaws lags the head: its tip swings against a turn of the neck and wobbles with the trot.
		sword.yRot=-neck.yRot*.2F+Mth.sin(step-.8F)*.05F*walk;
		sword.zRot=Mth.sin(t*.09F-.6F)*.02F+.12F*ring+Mth.sin(step*2-1.4F)*.04F*walk;
		// Follow-through: head and neck run on past the blow and settle; the body dips with it.
		if(since>=0&&s.deathTime<=0&&s.move!=MojitoRules.RECOVER&&s.move!=MojitoRules.FLOP){
			neck.xRot+=.12F*ring;head.xRot+=.2F*(float)(Math.exp(-.3F*Math.max(0,since-1))*Mth.sin(Math.max(0,since-1)*.95F));
			body.xRot+=.05F*ring;
		}
		// Breathing on offset periods: the barrel, the neck and the head do not line up, and his weight drifts from side to side.
		float calm=(1-walk)*(1-r*0)*r;
		body.yScale=1+Mth.sin(t*.08F)*.01F*(1-walk);
		neck.xRot+=Mth.sin(t*.047F+1.3F)*.02F*r;
		head.zRot+=Mth.sin(t*.055F+2.4F)*.025F*r;
		body.zRot+=Mth.sin(t*.031F)*.015F*calm;
		earL.zRot+=Mth.sin(t*.12F+.4F)*.04F;earR.zRot+=Mth.sin(t*.12F+1.9F)*.04F;
		// The hind legs give a little as the blow lands.
		if(since>=0&&since<8)for(int i=2;i<4;i++)shins[i].xRot+=.12F*(float)(Math.exp(-.35F*since)*Mth.cos(since*.9F));
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
	 * sweep crouches him low, head wrenched round to one side, and sweeps the blade along the grass to the other; the chop rears him
	 * up on his hind legs and brings him crashing down head first; the chase turns his head back after his tail; dizzy, he sways;
	 * the pounce crouches him with his rump up and wiggling, then stretches him out in the air; the fetch cocks his head back and
	 * flings it round, then gallops him after the sword (or stands him waiting, head tilted, for it to come back); prancing, he
	 * high-steps; digging, his forepaws scrabble with his head down; the shake shivers him then throws a roll down him nose to tail;
	 * the howl sits him back with his muzzle to the sky; the bolts lift his head as the magic swells at his sides; the hop tucks his
	 * legs up; spent, he pants; the zoomies bow him down, rump up and tail going, then gallop him flat out; flopped, he lies on his
	 * side with his tongue out.
	 */
	private void attack(int move,float el,int tellTicks,int side,float t){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case MojitoRules.WATCH->tailY+=Mth.sin(t*.55F)*.2F;
			case MojitoRules.SWEEP->{
				// Low on bent forelegs, head down and wrenched round to one side; then swept right across to the other along the grass.
				float p=Mth.clamp((el-tell)/MojitoRules.SWEEP_TICKS,0,1),sw=el<tell?side*.95F*wind:side*Mth.lerp(p*p*(3-2*p),.95F,-1.05F);
				neckY+=sw;yaw+=.3F*sw;headZ-=.3F*sw;neckX+=.5F*wind;headX-=.2F*wind;
				u[0]-=.35F*wind;u[1]-=.35F*wind;l[0]+=.8F*wind;l[1]+=.8F*wind;pitch+=.12F*wind;
				uz[0]=uz[2]=-.1F*sw;uz[1]=uz[3]=-.1F*sw;roll-=.06F*sw;tailY+=.4F*sw;earZ+=.3F*Math.abs(sw);
			}
			case MojitoRules.CHOP->{
				// Reared up on his hind legs, forepaws curled up before him and head high; then down head first, the blade into the ground,
				// and tugging at it while it sticks.
				float slam=ramp(el,tell,2),rear=wind*(1-slam),tug=el>tell+3?Mth.sin(el*1.3F)*slam:0;
				pitch+=-.9F*rear+.22F*slam;
				u[0]=Mth.lerp(rear,u[0],-1.25F)-.35F*slam;u[1]=Mth.lerp(rear,u[1],-1.05F)-.35F*slam;l[0]+=1.3F*rear;l[1]+=1.1F*rear;
				l[2]+=.35F*rear;l[3]+=.35F*rear;u[2]-=.15F*rear;u[3]-=.15F*rear;
				neckX+=-.4F*rear+.95F*slam+.06F*tug;headX+=-.2F*rear+.3F*slam;neckY+=.08F*tug;
				tailX-=.5F*rear;earX-=.5F*rear+.3F*slam;jawX+=.1F*rear;
			}
			case MojitoRules.CHASE->{
				// Head turned right back to his tail, the tail whisking away from it; then round and round after it at a skipping trot.
				float k=wind,a=el*1.4F;
				neckY+=side*1.35F*k;headZ+=side*.35F*k;yaw+=side*.25F*k;roll-=side*.08F*k;
				tailY+=-side*.7F*k+Mth.sin(t*1.8F)*.5F*k;tailX-=.3F*k;
				if(el>tell){float sw=Mth.sin(a)*.5F;u[0]+=sw;u[3]+=sw;u[1]-=sw;u[2]-=sw;l[0]+=Math.max(0,-Mth.sin(a));l[1]+=Math.max(0,Mth.sin(a));earZ+=.6F;}
				jawX+=.1F*k;
			}
			case MojitoRules.DIZZY->{
				// Swaying where he stands, legs splayed to stay up, head lolling round.
				float k=ramp(el,0,4),w=Mth.sin(el*.35F);
				roll+=w*.13F*k;yaw+=Mth.sin(el*.2F)*.1F*k;
				neckX+=.3F*k;neckY+=Mth.sin(el*.22F)*.45F*k;headZ+=Mth.sin(el*.35F+1)*.35F*k;
				uz[0]=.22F*k;uz[1]=-.22F*k;uz[2]=.18F*k;uz[3]=-.18F*k;
				jawX+=.2F*k;tongueX=(.4F+.15F*w)*k;earZ+=(.3F+.2F*w)*k;tailX-=.6F*k;
			}
			case MojitoRules.POUNCE->{
				float air=Mth.clamp((el-tell-1)/MojitoRules.POUNCE_TICKS,0,1),flying=el>tell?Mth.sin(Mth.PI*air):0,landed=el>tell+1+MojitoRules.POUNCE_TICKS?1:0;
				// Crouched with his chest to the grass and his rump up, wiggling it more and more; then stretched out flat in the air,
				// forelegs reaching; then head down hard onto where he lands.
				float crouch=wind*(1-go),wig=Mth.sin(t*2.2F)*crouch*smooth(el,tell*.4F,tell);
				u[0]-=.55F*crouch;u[1]-=.55F*crouch;l[0]+=1.1F*crouch;l[1]+=1.1F*crouch;
				pitch+=.18F*crouch;neckX+=.2F*crouch;headX-=.25F*crouch;yaw+=.12F*wig;roll+=.06F*wig;tailY+=Mth.sin(t*1.6F)*.6F*crouch;
				u[0]=Mth.lerp(flying,u[0],-1.15F);u[1]=Mth.lerp(flying,u[1],-1.05F);l[0]=Mth.lerp(flying,l[0],.3F);l[1]=Mth.lerp(flying,l[1],.3F);
				u[2]=Mth.lerp(flying,u[2],1.15F);u[3]=Mth.lerp(flying,u[3],1.05F);l[2]=Mth.lerp(flying,l[2],-.2F);l[3]=Mth.lerp(flying,l[3],-.2F);
				pitch+=Mth.lerp(air,-.25F,.3F)*flying;earX-=.9F*flying;tailX-=.6F*flying;
				neckX+=.7F*landed;headX+=.2F*landed;u[0]-=.3F*landed;u[1]-=.3F*landed;l[0]+=.6F*landed;l[1]+=.6F*landed;
			}
			case MojitoRules.FETCH->{
				if(side==0){
					// Head cocked back to throw, eyes on his prey, rump up and tail going; flung round as the sword leaves his jaws.
					neckY-=.55F*wind;headZ+=.25F*wind;neckX-=.1F*wind;pitch+=.08F*wind;tailY+=Mth.sin(t*1.6F)*.6F*wind;
					u[0]-=.3F*wind;u[1]-=.3F*wind;l[0]+=.5F*wind;l[1]+=.5F*wind;
				}else if(side==3){
					// Can't get to it: stood waiting, head tilted, ears up and tail going, for it to come back to him.
					float k=ramp(el,0,4);
					headZ+=.4F*k;neckX-=.15F*k;earX-=.3F*k;earZ-=.05F*k;jawX+=.15F*k;tailY+=Mth.sin(t*1.2F)*.5F*k;
				}else{
					// Bounding after it flat out, ears flying, mouth open for it.
					float a=(el-tell)*1.4F,c=Mth.sin(a),k=ramp(el,tell,4);
					u[0]=Mth.lerp(k,u[0],c*.9F);u[1]=Mth.lerp(k,u[1],c*.9F-.2F);l[0]=l[1]=Mth.lerp(k,l[0],Math.max(0,-Mth.cos(a))*1.2F);
					u[2]=Mth.lerp(k,u[2],HIND_U-c*.9F);u[3]=Mth.lerp(k,u[3],HIND_U-c*.9F+.2F);l[2]=l[3]=Mth.lerp(k,l[2],HIND_L+Math.max(0,Mth.cos(a))*.6F);
					pitch+=c*.1F*k;neckX+=(.25F-c*.08F)*k;earX-=1.0F*k;tailX-=.5F*k;jawX+=.3F*k;tongueX=(.5F+Mth.sin(a*2)*.3F)*k;
				}
			}
			case MojitoRules.PRANCE->{
				// High-stepping on the spot with the sword caught, head high and proud, the whole of him bouncing and his tail going.
				float k=ramp(el,0,3),a=el*.9F,fl=Math.max(0,Mth.sin(a)),fr=Math.max(0,-Mth.sin(a));
				u[0]-=.85F*fl*k;l[0]+=1.3F*fl*k;u[1]-=.85F*fr*k;l[1]+=1.3F*fr*k;
				u[2]+=.15F*fr*k;u[3]+=.15F*fl*k;
				pitch-=(.1F+.05F*Math.abs(Mth.sin(a)))*k;neckX-=.3F*k;headX-=.1F*k;earX+=Mth.sin(a*2)*.25F*k;
				tailY+=Mth.sin(t*1.8F)*.7F*k;tailX-=.15F*k;
			}
			case MojitoRules.DIG->{
				// Head down between his forelegs, his chest low and rump high, forepaws scrabbling at the ground like mad.
				float k=ramp(el,0,4),a=el*(el<tell?.9F:1.7F),dig=el<tell?.6F:1;
				pitch+=.25F*k;neckX+=.65F*k;headX+=.15F*k;
				u[0]+=(-.5F+Mth.sin(a)*.6F*dig)*k;l[0]+=(.7F+Math.max(0,Mth.cos(a))*.8F*dig)*k;
				u[1]+=(-.5F-Mth.sin(a)*.6F*dig)*k;l[1]+=(.7F+Math.max(0,-Mth.cos(a))*.8F*dig)*k;
				uz[2]=.15F*k;uz[3]=-.15F*k;l[2]+=.25F*k;l[3]+=.25F*k;
				earX+=.3F*k;tailX-=.3F*k;tailY+=Mth.sin(t*1.2F)*.4F*k;
			}
			case MojitoRules.SHAKE->{
				// A shiver running through his coat, ears up; then the shake, a roll running down him, his head going hardest, ears and
				// tail flying.
				float shiver=wind*(1-go),shake=el>=tell?Mth.sin(Mth.PI*Mth.clamp((el-tell)/8F,0,1)):0;
				roll+=Mth.sin(t*4.5F)*.035F*shiver;neckY+=Mth.sin(t*4.5F+.5F)*.05F*shiver;earX-=.3F*shiver;
				uz[0]=.15F*wind;uz[1]=-.15F*wind;uz[2]=.12F*wind;uz[3]=-.12F*wind;
				float w=Mth.sin(t*2.6F);
				roll+=w*.16F*shake;neckY+=Mth.sin(t*2.6F+.7F)*.6F*shake;headZ+=Mth.sin(t*2.6F+1.2F)*.4F*shake;
				earZ+=Math.abs(Mth.sin(t*2.6F+1.6F))*1.0F*shake;tailY+=Mth.sin(t*2.6F+2.2F)*.7F*shake;
			}
			case MojitoRules.HOWL->{
				// Sat back on his haunches, forelegs straight, muzzle to the sky; his jaws opening wide with each ring he sends out.
				float since=el>tell?(el-tell)%MojitoRules.HOWL_EVERY:99,cry=el>tell&&el<tell+MojitoRules.HOWL_RINGS*MojitoRules.HOWL_EVERY?Math.max(0,1-since/10):0;
				u[2]-=.7F*wind;u[3]-=.7F*wind;l[2]+=1.4F*wind;l[3]+=1.4F*wind;
				u[0]+=.15F*wind;u[1]+=.15F*wind;pitch-=.45F*wind;
				neckX-=.6F*wind;headX-=.55F*wind+.1F*cry;jawX+=.15F*wind+.25F*cry;
				earX-=.2F*wind;tailX-=.3F*wind;
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
		body.xRot-=.07F*k;tail.xRot+=.5F*k;topknot.xRot+=.3F*k;pom.xRot+=.4F*k;jaw.xRot+=.2F*k;
		for(int i=0;i<4;i++){legs[i].zRot+=(i%2==0?-1:1)*.1F*k;shins[i].xRot+=.18F*k;}
		body.y+=1.2F*k;
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
		topknot.zRot=Mth.lerp(fall,topknot.zRot,.4F);pom.xRot=Mth.lerp(buckle,pom.xRot,.9F);
		earL.zRot=Mth.lerp(fall,earL.zRot,-1.2F);earR.zRot=Mth.lerp(fall,earR.zRot,1.2F);
	}

	/**
	 * What shows besides the dog: the sword in his mouth from the moment he takes it up (lying in the grass until then) and out of it
	 * while thrown, whirling flat through the air where it flies or lying where it fell; his tongue once his head is up; and through
	 * the bolts the magic swelling at each side, pulsing with each volley and gone after the last.
	 */
	private void props(MojitoRenderState s){
		boolean held=s.rise>=GRAB;
		sword.visible=held&&!s.thrown;fallen.visible=!held||s.thrown;
		if(s.thrown){
			// The blade's middle (13 out along it from the grip) put where the sword is, turned about it.
			float a=s.side==2?.6F:s.ageInTicks*.9F;
			fallen.x=s.swordX+13*Mth.cos(a);fallen.y=s.swordY;fallen.z=s.swordZ-13*Mth.sin(a);
			fallen.xRot=Mth.HALF_PI;fallen.yRot=a;fallen.zRot=0;
		}
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
