package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.world.HollowMatronRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A great dusky spider, grey-brown with pale silk banding her legs and a bone-white mark on her swollen abdomen, eight red eyes in
 * two rows over her fangs. She hangs head-down on her thread with her legs drawn in; dropping, her legs fold to her as she falls, and
 * she turns over as she lands and rears up with her forelegs raised and her fangs spread. On her feet she walks on alternating sets of
 * four, every move has its own pose, wound up through its tell and thrown fast, each blends into the next, a blow jolts her, and
 * dying she rolls onto her back with her legs curling in over her.
 */
public final class HollowMatronModel extends EntityModel<HollowMatronRenderState> {
	/** Where each pair of legs leaves the body, front to back, and how far forward (or back) each pair is fanned. */
	private static final float[] HIP_Z={-4,-1.5F,1,3.5F},FAN={.75F,.25F,-.25F,-.7F};
	/** How long each femur is to the knee, and each tibia past it. */
	private static final float FEMUR=12,TIBIA=14;
	/** How high her femurs stand and how far her tibias bend down from them, on her feet. */
	private static final float STAND_A=.6F,STAND_B=1.8F;
	private final ModelPart body,head,fangL,fangR,palpL,palpR,abdomen,spinner,thread;
	private final ModelPart[] hips=new ModelPart[8],femurs=new ModelPart[8],tibias=new ModelPart[8];
	private final ModelPart[] posed;
	private final PoseBlend blender;
	/** The pose being built: each leg's femur lift, its tibia's bend from the femur, its fan; the body's pitch, roll and yaw; the head, fangs, palps and abdomen. */
	private final float[] a=new float[8],b=new float[8],fan=new float[8];
	private float pitch,roll,yaw,headX,headY,fangs,palps,abdX,abdY;
	public HollowMatronModel(ModelPart root){
		super(root);body=root.getChild("body");head=body.getChild("head");abdomen=body.getChild("abdomen");spinner=abdomen.getChild("spinner");
		fangL=head.getChild("fangL");fangR=head.getChild("fangR");palpL=head.getChild("palpL");palpR=head.getChild("palpR");thread=root.getChild("thread");
		for(int i=0;i<8;i++){hips[i]=body.getChild("hip"+i);femurs[i]=hips[i].getChild("femur");tibias[i]=femurs[i].getChild("tibia");}
		posed=new ModelPart[8*3+7];blender=new PoseBlend(posed);
		posed[0]=body;posed[1]=head;posed[2]=abdomen;posed[3]=fangL;posed[4]=fangR;posed[5]=palpL;posed[6]=palpR;
		for(int i=0;i<8;i++){posed[7+i]=hips[i];posed[15+i]=femurs[i];posed[23+i]=tibias[i];}
	}
	private static int side(int i){return i%2==0?1:-1;}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.5F,-3,-6,9,6,11),PartPose.offset(0,16,0));
		var head=body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(44,0).addBox(-3,-2.5F,-3,6,5,3),PartPose.offset(0,0,-6));
		for(int sign:new int[]{1,-1}){
			String side=sign>0?"L":"R";
			head.addOrReplaceChild("fang"+side,CubeListBuilder.create().texOffs(64,0).addBox(-1,0,-1,2,4,2),PartPose.offset(sign*1.5F,1.5F,-3));
			head.addOrReplaceChild("palp"+side,CubeListBuilder.create().texOffs(74,0).addBox(-.5F,-.5F,-4,1,1,4),PartPose.offset(sign*2.5F,1,-3));
		}
		var abdomen=body.addOrReplaceChild("abdomen",CubeListBuilder.create().texOffs(0,20).addBox(-7,-6,0,14,12,14),PartPose.offsetAndRotation(0,-1,5,.2F,0,0));
		abdomen.addOrReplaceChild("spinner",CubeListBuilder.create().texOffs(60,20).addBox(-1.5F,-1.5F,0,3,3,2),PartPose.offset(0,0,14));
		root.addOrReplaceChild("thread",CubeListBuilder.create().texOffs(60,28).addBox(-.5F,-.5F,0,1,1,16),PartPose.rotation(Mth.HALF_PI,0,0));
		for(int i=0;i<8;i++){
			int s=side(i),p=i/2;
			var hip=body.addOrReplaceChild("hip"+i,CubeListBuilder.create(),PartPose.offset(s*4.5F,0,HIP_Z[p]));
			var femur=hip.addOrReplaceChild("femur",CubeListBuilder.create().texOffs(0,50).addBox(s>0?0:-FEMUR,-1,-1,FEMUR,2,2),PartPose.ZERO);
			femur.addOrReplaceChild("tibia",CubeListBuilder.create().texOffs(0,56).addBox(s>0?0:-TIBIA,-.75F,-.75F,TIBIA,1.5F,1.5F),PartPose.offset(s*FEMUR,0,0));
		}
		return LayerDefinition.create(mesh,128,128);
	}

	@Override public void setupAnim(HollowMatronRenderState s){
		super.setupAnim(s);
		pose(s,s.move,s.elapsed,s.tell,s.side);
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1){
			blender.store();
			root().resetPose();
			pose(s,s.prevMove,s.prevElapsed,s.prevTell,s.side);
			float k=s.blend*s.blend*(3-2*s.blend);
			blender.mix(k);
		}
		hurt(s);
		die(s);
		ground();
		// Off the ground through a pounce, highest halfway.
		if(s.move==HollowMatronRules.POUNCE&&s.deathTime<=0&&s.elapsed>s.tell)
			body.y-=4*Mth.sin(Mth.PI*Mth.clamp((s.elapsed-s.tell)/HollowMatronRules.POUNCE_TICKS,0,1));
		// She lands on her back with a small bounce.
		if(s.deathTime>12)body.y-=1.4F*(float)Math.exp(-(s.deathTime-12)*.45F)*Math.abs(Mth.sin((s.deathTime-12)*1.8F));
		thread(s);
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: hanging or standing (or turning over between), walking, then the move itself. */
	private void pose(HollowMatronRenderState s,int move,float el,int tell,int side){
		float t=s.ageInTicks,d=HollowMatronRules.drop(move,el);
		pitch=0;roll=0;yaw=0;headX=0;fangs=0;palps=0;abdX=.2F;abdY=0;
		for(int i=0;i<8;i++){a[i]=STAND_A;b[i]=STAND_B;fan[i]=side(i)*FAN[i/2];}
		hang(d,t);
		float up=smooth(d,HollowMatronRules.FLIP_FROM,HollowMatronRules.FLIP_TO);
		// Breathing, her gaze followed once she is down, and her palps never still.
		abdX+=Mth.sin(t*.07F)*.025F;
		headY=Mth.clamp(s.yRot,-40,40)*Mth.DEG_TO_RAD*.4F*up;headX+=s.xRot*Mth.DEG_TO_RAD*.3F*up;
		palps+=Mth.sin(t*.23F)*.12F+Mth.sin(t*.61F)*.05F;
		// Her abdomen sways a little on its own time, a beat behind her breathing; each leg settles and shifts on a period of its own.
		abdY+=Mth.sin(t*.045F+1)*.05F*up;abdX+=Mth.sin(t*.05F-.8F)*.015F;headX+=Mth.sin(t*.09F+2)*.02F*up;
		for(int i=0;i<8;i++){a[i]+=Mth.sin(t*(.04F+.007F*i)+i*1.3F)*.02F*up;b[i]+=Mth.sin(t*(.05F+.006F*i)+i*.9F)*.02F*up;}
		// Walking: alternating sets of four, each foot lifted as it swings forward.
		float walk=Math.min(1,s.walkAnimationSpeed*1.5F)*up;
		gait(s.walkAnimationPos*.9F,walk);
		attack(move,el,tell,side,t);
		apply();
	}

	/** Each leg swung forward and back, the two sets of four in turn, lifted as it comes forward. */
	private void gait(float step,float walk){
		if(walk<=0)return;
		for(int i=0;i<8;i++){
			int s=side(i),p=i/2,group=(p+(s>0?0:1))%2;
			float phase=step+group*Mth.PI;
			fan[i]+=s*Mth.sin(phase)*.28F*walk;
			a[i]+=Math.max(0,Mth.cos(phase))*.4F*walk;
		}
		roll+=Mth.sin(step*2)*.02F*walk;
	}

	/**
	 * Hanging to standing, {@code d} 0 to 1 through the drop: head-down on her thread, legs drawn loosely in and twitching; folded close
	 * as she falls; turning over as she lands; then reared up on her hind legs with her forelegs raised and her fangs spread, and down.
	 */
	private void hang(float d,float t){
		float k=1-smooth(d,HollowMatronRules.FLIP_FROM,HollowMatronRules.FLIP_TO);
		float fold=Mth.sin(Mth.PI*Mth.clamp(d/HollowMatronRules.FALL_END,0,1))*.3F;
		if(k>0){
			pitch=Mth.lerp(k,pitch,Mth.HALF_PI);
			roll+=Mth.sin(t*.03F)*.05F*k;
			for(int i=0;i<8;i++){
				float twitch=Mth.sin(t*.11F+i*1.7F)*.04F*(1-d);
				a[i]=Mth.lerp(k,a[i],.25F+twitch);b[i]=Mth.lerp(k,b[i],1.1F);fan[i]=Mth.lerp(k,fan[i],fan[i]*1.2F);
			}
			abdX=Mth.lerp(k,abdX,.05F);
		}
		for(int i=0;i<8;i++){b[i]+=fold;a[i]-=fold*.5F;}
		float rear=smooth(d,.6F,.7F)*(1-smooth(d,.86F,1));
		if(rear>0){
			pitch-=.4F*rear;
			for(int i=0;i<4;i++){a[i]+=(i<2?.9F:.5F)*rear;b[i]-=(i<2?.6F:.3F)*rear;}
			fangs+=.5F*rear;palps+=.4F*rear;abdX+=.2F*rear;
		}
	}

	/**
	 * Her moves, laid over the standing pose, each wound up through its tell so it reads from across the hall, then thrown fast. The bite
	 * rears her back with her fangs spread, then lunges, and a missed one leaves her nose-down wrenching at her stuck fangs; the scythe
	 * raises her forelegs high to one side and swings them across; the spit rears her and curls her abdomen under her; the pounce
	 * crouches her on bent legs, then flings her with her forelegs reaching; the drum rears her up with her forelegs trembling high, then
	 * slams them down and leaves her low; the reel curls her abdomen right under her with her head low, and the haul draws her forelegs
	 * in over each other; the bristle swings her abdomen high and quivering with her hind legs scraping at it, then flicks it; the snare
	 * throws her abdomen straight up; the rousing hunches her in on herself, quivering, then throws every leg wide; skittering, her legs
	 * blur; spent, she sinks low on splayed legs with her abdomen heaving.
	 */
	private void attack(int move,float el,int tellTicks,int side,float t){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case HollowMatronRules.WATCH->{
				fangs+=Math.max(0,Mth.sin(t*.37F))*.08F;
			}
			case HollowMatronRules.BITE->{
				float rear=wind*(1-go),strike=go*(1-ramp(el,tell+HollowMatronRules.LUNGE_TICKS,5));
				pitch+=-.35F*rear+.22F*strike;
				for(int i=0;i<2;i++){a[i]+=.7F*rear-.2F*strike;b[i]-=.4F*rear;fan[i]+=side(i)*.2F*strike;}
				for(int i=6;i<8;i++){a[i]-=.15F*rear;}
				fangs+=.65F*rear-.25F*strike;palps+=.5F*rear;headX+=-.2F*rear+.35F*strike;
				abdX+=.15F*rear;
				// Past the mark with the fangs snapping shut on contact, a tick's hold, then the abdomen swinging on behind the lunge.
				float over=Mth.sin(Mth.PI*Mth.clamp((el-tell)/4F,0,1)),lag=Mth.sin(Mth.PI*Mth.clamp((el-tell-1.5F)/6F,0,1));
				pitch+=.1F*over;fangs-=.3F*over;headX+=.12F*over;abdX-=.25F*lag;abdY+=side*.12F*lag;
				for(int i=2;i<8;i++)a[i]+=.12F*Mth.sin(Mth.PI*Mth.clamp((el-tell-1)/5F,0,1));
			}
			case HollowMatronRules.SCYTHE->{
				// Forelegs raised high and drawn back to one side, then scythed across at chest height.
				float up=wind*(1-ramp(el,tell+HollowMatronRules.SCYTHE_TICKS+2,5));
				float cross=smooth(el,tell,tell+HollowMatronRules.SCYTHE_TICKS);
				float stop=el-tell-HollowMatronRules.SCYTHE_TICKS;
				// The two ticks held on contact, the forelegs overshooting the mark a little and coming back; the abdomen swings on after.
				float across=(Mth.lerp(cross,side*.8F,-side*.9F)-side*.2F*Mth.sin(Mth.PI*Mth.clamp((stop+2)/6F,0,1)))*up;
				abdY-=across*.3F+side*.2F*Mth.sin(Mth.PI*Mth.clamp((stop+1)/7F,0,1));abdX-=.15F*Mth.sin(Mth.PI*Mth.clamp((stop+1)/7F,0,1));
				for(int i=0;i<4;i++){a[i]+=(i<2?1.3F:.8F)*up;b[i]-=(i<2?.9F:.5F)*up;fan[i]+=across;}
				for(int i=6;i<8;i++){a[i]-=.15F*up;b[i]+=.2F*up;}
				yaw=across*.4F;roll-=side*.08F*wind*(1-cross);pitch-=.22F*up;fangs+=.3F*up;palps+=.3F*wind*(1-cross);
			}
			case HollowMatronRules.DRUM->{
				// Reared up on her hind legs, forelegs trembling high and drumming faster; slammed down; low a moment after.
				float rear=wind*(1-go),low=go*(1-ramp(el,tell+HollowMatronRules.DRUM_LOW-4,6));
				float shake=Mth.sin(el*(1.4F+wind*1.6F));
				pitch+=-.6F*rear+.18F*low;
				for(int i=0;i<4;i++){
					float k=i<2?1:.6F;
					a[i]+=(1.2F*rear-.35F*low)*k+shake*.18F*rear*k*(i%2==0?1:-1);b[i]+=(-.7F*rear+.4F*low)*k;fan[i]+=side(i)*.1F*rear;
				}
				for(int i=4;i<8;i++){a[i]-=.2F*rear-.25F*low;b[i]+=.3F*rear+.3F*low;}
				fangs+=.45F*rear;palps+=.4F*rear+.2F*shake*rear;abdX+=.2F*rear-.15F*low;headX+=-.15F*rear+.2F*low;
				// The slam jars her: a two-tick hold on the ground, the forelegs bouncing off it, the abdomen flopping down a beat behind.
				float hit=el-tell,jar=hit>0?(float)Math.exp(-hit*.35F)*Mth.sin(hit*1.6F):0,jarLate=hit>1.5F?(float)Math.exp(-(hit-1.5F)*.3F)*Mth.sin((hit-1.5F)*1.4F):0;
				for(int i=0;i<4;i++)a[i]+=.18F*jar;
				pitch+=.05F*jar;abdX+=.2F*jarLate;headX+=.08F*jarLate;abdY+=.06F*Mth.sin(hit*1.1F)*(hit>0?(float)Math.exp(-hit*.25F):0);
			}
			case HollowMatronRules.REEL->{
				// Abdomen curled right under her with the spinnerets aimed past her fangs, head low; jolted as the thread goes.
				float pulse=Math.max(0,1-Math.abs(el-tell)/2);
				abdX=Mth.lerp(wind,abdX,-1.25F)-.3F*pulse;pitch-=.2F*wind-.12F*pulse;headX+=.25F*wind;
				for(int i=0;i<2;i++){a[i]+=.35F*wind;b[i]-=.2F*wind;fan[i]+=side(i)*.15F*wind;}
				for(int i=6;i<8;i++){a[i]-=.2F*wind;b[i]+=.35F*wind;}
				fangs+=.25F*wind+.3F*pulse;palps+=.3F*wind;
			}
			case HollowMatronRules.HAUL->{
				// Foreleg over foreleg, drawing her thread in; then reared back with her fangs spread for the bite.
				float k=ramp(el,0,3),pull=1-smooth(el,HollowMatronRules.HAUL_PULL-2,HollowMatronRules.HAUL_PULL+2),bite=smooth(el,HollowMatronRules.HAUL_PULL,HollowMatronRules.HAUL_TICKS);
				for(int i=0;i<2;i++){
					float w=Mth.sin(el*.8F+(i==0?0:Mth.PI));
					a[i]+=(.6F+.35F*w*pull)*k;b[i]+=(-.3F+.35F*Mth.cos(el*.8F+(i==0?0:Mth.PI))*pull)*k;fan[i]+=side(i)*(.1F-.25F*w*pull)*k;
				}
				for(int i=2;i<4;i++)a[i]+=.25F*k;
				abdX=Mth.lerp(k,abdX,-.6F);pitch+=-.1F*k-.3F*bite;headX+=.15F*k*pull-.2F*bite;
				fangs+=.2F*k+.6F*bite+Math.max(0,Mth.sin(el*.6F))*.1F;palps+=.4F*k;
			}
			case HollowMatronRules.BRISTLE->{
				// Abdomen swung high and quivering at whoever is behind her, hind legs scraping the hairs off it; flicked; held up after.
				float flick=Math.max(0,1-Math.abs(el-tell)/2.5F),quiver=Mth.sin(el*2.1F)*(1-go);
				abdX=Mth.lerp(wind,abdX,.95F)+.06F*quiver*wind+.35F*flick;pitch+=.18F*wind;headX+=.1F*wind;
				for(int i=6;i<8;i++){
					float sc=Mth.sin(el*1.3F+(i==6?0:Mth.PI))*(1-go);
					a[i]+=(.9F+.25F*sc)*wind;b[i]-=(.4F-.3F*sc)*wind;fan[i]-=side(i)*(.35F+.15F*sc)*wind;
				}
				for(int i=0;i<4;i++){a[i]-=.15F*wind;b[i]+=.25F*wind;}
				palps-=.2F*wind;fangs+=.15F*flick;
			}
			case HollowMatronRules.SNARE->{
				// Abdomen thrown straight up at the roof, front braced low; it pulses as the silk goes up.
				float pulse=Math.max(0,1-Math.abs(el-tell)/2),throb=Mth.sin(el*.9F)*.06F*(1-go);
				abdX=Mth.lerp(wind,abdX,1.15F)+.3F*pulse+throb;pitch+=.25F*wind;headX+=.1F*wind;
				for(int i=0;i<4;i++){a[i]-=.2F*wind;b[i]+=.3F*wind;fan[i]*=1+.1F*wind;}
				for(int i=6;i<8;i++){a[i]+=.15F*wind;b[i]-=.2F*wind;}
				palps+=.3F*wind;
			}
			case HollowMatronRules.STUCK->{
				// Nose-down with her fangs driven into the floor, legs splayed and heaving, wrenching at them.
				float k=ramp(el,0,3),tug=Mth.sin(el*.9F);
				pitch+=(.45F+.06F*tug)*k;roll+=Mth.sin(el*.9F+1)*.05F*k;headX+=.3F*k;
				for(int i=0;i<8;i++){fan[i]*=1+.2F*k;a[i]-=.1F*k;b[i]+=.3F*k;}
				for(int i=4;i<8;i++)a[i]+=tug*.2F*k*(i%2==0?1:-1);
				for(int i=0;i<2;i++){a[i]+=.3F*k;b[i]+=.2F*k*tug;}
				fangs=-.1F*k;palps+=Mth.sin(el*1.7F)*.3F*k;abdX+=(.3F+.08F*tug)*k;
			}
			case HollowMatronRules.ROUSE->{
				// Hunched in on herself and quivering, harder and harder; then every leg thrown wide and her fangs spread.
				float hunch=wind*(1-go),wide=go*(1-ramp(el,tell+4,10)),quake=Mth.sin(el*(2+wind*1.5F));
				for(int i=0;i<8;i++){
					a[i]+=-.25F*hunch+Mth.sin(el*3.1F+i*1.3F)*.06F*hunch+(i<4?1.0F:.5F)*wide;b[i]+=.5F*hunch-.8F*wide;fan[i]*=1-.2F*hunch+.35F*wide;
				}
				pitch+=.12F*hunch-.45F*wide;roll+=quake*.035F*hunch;abdX+=-.15F*hunch+.03F*quake*hunch+.7F*wide;
				headX+=.2F*hunch-.3F*wide;fangs+=-.05F*hunch+.9F*wide;palps+=.6F*wide+.15F*quake*hunch;
			}
			case HollowMatronRules.SPIT->{
				float rear=wind*(1-ramp(el,tell+3,6)),pulse=Math.max(0,1-Math.abs(el-tell)/2);
				pitch-=.45F*rear;abdX=Mth.lerp(rear,abdX,-1.0F)-.3F*pulse;
				for(int i=0;i<4;i++)a[i]+=(i<2?.5F:.25F)*rear;
				for(int i=6;i<8;i++){a[i]-=.2F*rear;b[i]+=.25F*rear;}
				fangs+=.4F*wind*(1-go)+.3F*pulse;headX-=.15F*pulse;
			}
			case HollowMatronRules.POUNCE->{
				float crouch=wind*(1-go);
				// Landing: she squashes onto bent legs and the abdomen comes down a beat after the rest of her.
				float since=el-tell-HollowMatronRules.POUNCE_TICKS;
				if(since>0){
					float land=(float)Math.exp(-since*.45F),lateLand=since>1.5F?(float)Math.exp(-(since-1.5F)*.35F)*Mth.sin((since-1.5F)*1.5F):0;
					for(int i=0;i<8;i++){a[i]+=.3F*land;b[i]+=.35F*land;}
					pitch+=.1F*land;fangs+=.3F*land;abdX+=.25F*lateLand;
				}
				for(int i=0;i<8;i++){a[i]+=.45F*crouch;b[i]+=.45F*crouch;}
				pitch+=.1F*crouch;abdX+=.2F*crouch;fangs+=.3F*crouch;
				if(el>tell){
					float p=Mth.clamp((el-tell)/HollowMatronRules.POUNCE_TICKS,0,1),fly=Mth.sin(Mth.PI*p);
					for(int i=0;i<8;i++){
						int sd=side(i);
						if(i<4){a[i]-=.3F*fly;b[i]-=.9F*fly;fan[i]+=sd*.3F*fly;}
						else{b[i]-=.7F*fly;fan[i]-=sd*.3F*fly;}
					}
					pitch+=Mth.lerp(p,-.25F,.2F)*fly;fangs+=.6F*fly;abdX-=.2F*fly;
				}
			}
			case HollowMatronRules.SKITTER->{
					abdY+=Mth.sin(el*1.2F-1F)*.1F;
				gait(el*2.4F,1);
				if(side==0){headX-=.3F;fangs+=.5F;pitch-=.15F;palps+=.3F;}
				else roll+=side*.12F;
			}
			case HollowMatronRules.RECOVER->{
				float k=ramp(el,0,4),heave=Mth.sin(el*.5F);
				for(int i=0;i<8;i++){a[i]+=.3F*k;b[i]+=.45F*k;fan[i]*=1+.15F*k;}
				abdX+=(.25F+.06F*heave)*k;pitch+=.03F*heave*k;palps-=.3F*k;fangs-=.05F*k;
				// The stagger: her legs shuffle to catch her, her body rocking, the abdomen wobbling a beat behind.
				float stag=(float)Math.exp(-el*.2F);
				for(int i=0;i<8;i++)a[i]+=Mth.sin(el*1.1F+i*1.7F)*.1F*stag;
				roll+=Mth.sin(el*.9F)*.04F*stag;abdY+=Mth.sin(el*.9F-1.2F)*.1F*stag;abdX+=Mth.sin(el*.8F-1.5F)*.08F*stag;headX+=Mth.sin(el*.8F-.7F)*.06F*stag;
			}
			case HollowMatronRules.HASTE->{
				headX-=.15F;fangs+=.35F+Math.max(0,Mth.sin(t*.5F))*.15F;palps+=.3F;abdX+=.1F;
				for(int i=0;i<8;i++)a[i]+=.15F;
			}
			case HollowMatronRules.SCALE->{
				// Nose-up against the silk, legs hugging it, climbing hand over hand.
				float k=ramp(el,0,4);
				pitch=Mth.lerp(k,pitch,-Mth.HALF_PI);
				for(int i=0;i<8;i++){a[i]=Mth.lerp(k,a[i],.3F);b[i]=Mth.lerp(k,b[i],1.5F);}
				gait(el*1.6F,k);abdX=Mth.lerp(k,abdX,.05F);
			}
			case HollowMatronRules.LURK->{
				// Turned head-down on the silk, legs gripping; through the tell her forelegs coil and her fangs spread.
				float k=ramp(el,0,6),coil=ramp(el,tell,6);
				pitch=Mth.lerp(k,-Mth.HALF_PI,Mth.HALF_PI);
				for(int i=0;i<8;i++){a[i]=.25F+Mth.sin(t*.11F+i*1.7F)*.03F*(1-coil);b[i]=1.3F;}
				for(int i=0;i<4;i++){a[i]+=(i<2?.8F:.4F)*coil;b[i]+=(i<2?.5F:.25F)*coil;}
				fangs+=.15F+.6F*coil;palps+=.5F*coil;headX-=.2F*coil;abdX=.05F+.15F*coil;
			}
			case HollowMatronRules.PLUNGE->{
				// Falling headfirst and righting herself as she comes down, forelegs reaching for whoever is under her.
				float p=Mth.clamp(el/HollowMatronRules.PLUNGE_TICKS,0,1);
				pitch=Mth.lerp(smooth(p,0,.8F),Mth.HALF_PI,-.2F);
				for(int i=0;i<8;i++){a[i]=Mth.lerp(p,.6F,STAND_A+.3F);b[i]=Mth.lerp(p,1.6F,STAND_B-.2F);}
				for(int i=0;i<4;i++){a[i]-=.25F;b[i]-=(i<2?.9F:.5F);fan[i]+=side(i)*.25F;}
				fangs+=.8F;palps+=.4F;abdX+=.15F;
			}
			case HollowMatronRules.TUMBLE->{
				// Cut down: tumbling, legs flailing.
				pitch+=Mth.sin(el*.45F)*.6F;roll+=Mth.sin(el*.3F+1)*.5F;
				for(int i=0;i<8;i++){a[i]+=Mth.sin(el*1.3F+i*1.9F)*.5F;b[i]+=Mth.sin(el*1.7F+i)*.5F;}
				fangs+=.5F;palps+=Mth.sin(el)*.4F;
			}
			default->{}
		}
	}

	/** Sets the parts from the pose built. */
	private void apply(){
		body.xRot=pitch;body.zRot=roll;body.yRot=yaw;
		head.xRot=headX;head.yRot=headY;
		abdomen.xRot=abdX;abdomen.yRot=abdY;
		fangL.zRot=-fangs;fangR.zRot=fangs;fangL.xRot=fangR.xRot=-.25F-.2F*fangs;
		palpL.xRot=palpR.xRot=.3F-palps;
		for(int i=0;i<8;i++){int s=side(i);hips[i].yRot=fan[i];femurs[i].zRot=-s*a[i];tibias[i].zRot=s*b[i];}
	}

	/**
	 * Sets the body's height so whatever reaches lowest just touches the floor: a foot or a knee, her head while she hangs, her abdomen
	 * when she sinks onto it or lies on her back. However her legs are bent and however she is turned, she rests on the ground.
	 */
	private void ground(){
		body.x=0;body.y=0;body.z=0;
		float low=0;
		PoseStack ps=new PoseStack();
		body.translateAndRotate(ps);
		for(float x:new float[]{-4.5F,4.5F})for(float y:new float[]{-3,3})for(float z:new float[]{-6,5})low=Math.max(low,depth(ps,x,y,z));
		ps.pushPose();head.translateAndRotate(ps);
		for(float x:new float[]{-3,3})for(float y:new float[]{-2.5F,2.5F})for(float z:new float[]{-3,0})low=Math.max(low,depth(ps,x,y,z));
		for(ModelPart f:new ModelPart[]{fangL,fangR}){ps.pushPose();f.translateAndRotate(ps);low=Math.max(low,depth(ps,0,4,0));ps.popPose();}
		ps.popPose();
		ps.pushPose();abdomen.translateAndRotate(ps);
		for(float x:new float[]{-7,7})for(float y:new float[]{-6,6})for(float z:new float[]{0,14})low=Math.max(low,depth(ps,x,y,z));
		ps.popPose();
		for(int i=0;i<8;i++){
			int s=side(i);
			ps.pushPose();hips[i].translateAndRotate(ps);femurs[i].translateAndRotate(ps);
			low=Math.max(low,depth(ps,s*FEMUR,1,0));
			tibias[i].translateAndRotate(ps);
			low=Math.max(low,depth(ps,s*TIBIA,.75F,0));
			ps.popPose();
		}
		body.y=24-low;
	}
	/** How far below the root a point (in model pixels) sits in the frame {@code ps} has got to. */
	private static float depth(PoseStack ps,float x,float y,float z){
		return ps.last().pose().transformPosition(x/16,y/16,z/16,new Vector3f()).y*16;
	}

	/** Her thread, from her spinnerets straight up to the nest, while she hangs on it, drops down it or climbs it. */
	private void thread(HollowMatronRenderState s){
		boolean on=s.deathTime<=0&&(s.move==HollowMatronRules.DORMANT
			||((s.move==HollowMatronRules.DROP||s.move==HollowMatronRules.CLIMB)&&s.drop<HollowMatronRules.FLIP_TO));
		thread.visible=false;
		if(!on)return;
		PoseStack ps=new PoseStack();
		body.translateAndRotate(ps);abdomen.translateAndRotate(ps);spinner.translateAndRotate(ps);
		Vector3f at=ps.last().pose().transformPosition(0,0,2/16F,new Vector3f()).mul(16);
		float len=s.threadUp*16/HollowMatronRenderer.SCALE-(24-at.y);
		if(len<=0)return;
		thread.visible=true;thread.x=at.x;thread.y=at.y;thread.z=at.z;
		thread.xRot=Mth.HALF_PI;thread.yRot=0;thread.zRot=0;
		thread.xScale=1;thread.yScale=1;thread.zScale=len/16;
	}

	/** A blow she takes: she flinches back, her legs jerking in. */
	private void hurt(HollowMatronRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		body.xRot-=.15F*k;body.zRot+=Mth.sin(s.hurt*18)*.05F*k;
		for(int i=0;i<8;i++)tibias[i].zRot+=side(i)*.2F*k;
		// The recoil runs back through her: the head snaps up, the fangs flare, the legs jerk in a beat apart, the abdomen whipping late.
		float late=(float)Math.pow(Math.max(0,s.hurt-.12F),2);
		head.xRot-=.25F*k;fangL.zRot-=.3F*k;fangR.zRot+=.3F*k;palpL.xRot-=.3F*k;palpR.xRot-=.3F*k;
		abdomen.xRot+=.25F*late;abdomen.yRot+=Mth.sin(s.hurt*16-1F)*.15F*late;
		for(int i=0;i<8;i++)femurs[i].zRot-=side(i)*(.12F+.05F*(i%3))*(i%2==0?k:late);
	}

	/** Dying: she shudders, rolls onto her back, and her legs curl in over her. */
	private void die(HollowMatronRenderState s){
		if(s.deathTime<=0)return;
		float over=smooth(s.deathTime,2,12),curl=smooth(s.deathTime,0,10);
		body.xRot=Mth.lerp(over,body.xRot,0);body.yRot*=1-over;
		body.zRot=Mth.lerp(over,body.zRot+Mth.sin(s.deathTime*2.5F)*.08F*(1-over),Mth.PI);
		abdomen.xRot=Mth.lerp(over,abdomen.xRot,.1F);
		for(int i=0;i<8;i++){
			int sd=side(i);
			femurs[i].zRot=Mth.lerp(curl,femurs[i].zRot,-sd*1.3F);
			tibias[i].zRot=Mth.lerp(curl,tibias[i].zRot,sd*2.6F);
			hips[i].yRot=Mth.lerp(curl,hips[i].yRot,hips[i].yRot*.6F);
		}
		fangL.zRot=fangR.zRot=0;
		// Before she goes over she staggers, her legs buckling out of step; once down the legs twitch and slowly stop, and the abdomen bounces.
		float d=s.deathTime,stag=Mth.sin(d*1.8F)*.1F*(1-over),twitch=Mth.clamp((d-10)/6,0,1)*(float)Math.exp(-Math.max(0,d-16)*.25F);
		head.xRot=Mth.lerp(over,head.xRot-.2F*(1-over),.2F);
		body.zRot+=stag;body.yRot+=Mth.sin(d*1.3F)*.08F*(1-over);
		float land=d>12?(float)Math.exp(-(d-12)*.4F)*Mth.sin((d-12)*1.8F):0,lateLand=d>13.5F?(float)Math.exp(-(d-13.5F)*.4F)*Mth.sin((d-13.5F)*1.8F):0;
		abdomen.xRot+=.2F*lateLand;abdomen.yRot+=Mth.sin(d*1.1F)*.1F*(1-over);
		for(int i=0;i<8;i++){
			int sd=side(i);
			femurs[i].zRot+=-sd*Mth.sin(d*1.7F+i*1.9F)*.15F*(twitch+(1-curl)*.6F);
			tibias[i].zRot+=sd*Mth.sin(d*2.1F+i*1.1F)*.25F*twitch;
		}
		palpL.xRot+=.2F*lateLand;palpR.xRot+=.2F*lateLand;head.xRot+=.06F*land;
	}
}
