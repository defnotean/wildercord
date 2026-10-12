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
 * four, each move blends into the next, a blow jolts her, and dying she rolls onto her back with her legs curling in over her.
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
	/** The pose being built: each leg's femur lift, its tibia's bend from the femur, its fan; the body's pitch, roll and yaw; the head, fangs, palps and abdomen. */
	private final float[] a=new float[8],b=new float[8],fan=new float[8];
	private float pitch,roll,yaw,headX,headY,fangs,palps,abdX;
	public HollowMatronModel(ModelPart root){
		super(root);body=root.getChild("body");head=body.getChild("head");abdomen=body.getChild("abdomen");spinner=abdomen.getChild("spinner");
		fangL=head.getChild("fangL");fangR=head.getChild("fangR");palpL=head.getChild("palpL");palpR=head.getChild("palpR");thread=root.getChild("thread");
		for(int i=0;i<8;i++){hips[i]=body.getChild("hip"+i);femurs[i]=hips[i].getChild("femur");tibias[i]=femurs[i].getChild("tibia");}
		posed=new ModelPart[8*3+7];
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
		if(s.blend<1&&s.prevMove!=s.move){
			PartPose[] now=new PartPose[posed.length];
			for(int i=0;i<posed.length;i++)now[i]=posed[i].storePose();
			root().resetPose();
			pose(s,s.prevMove,s.prevElapsed,s.prevTell,s.side);
			float k=s.blend*s.blend*(3-2*s.blend);
			for(int i=0;i<posed.length;i++){
				PartPose pa=posed[i].storePose(),pb=now[i];
				posed[i].loadPose(PartPose.offsetAndRotation(Mth.lerp(k,pa.x(),pb.x()),Mth.lerp(k,pa.y(),pb.y()),Mth.lerp(k,pa.z(),pb.z()),
					Mth.lerp(k,pa.xRot(),pb.xRot()),Mth.lerp(k,pa.yRot(),pb.yRot()),Mth.lerp(k,pa.zRot(),pb.zRot())));
			}
		}
		hurt(s);
		die(s);
		ground();
		// Off the ground through a pounce, highest halfway.
		if(s.move==HollowMatronRules.POUNCE&&s.deathTime<=0&&s.elapsed>s.tell)
			body.y-=4*Mth.sin(Mth.PI*Mth.clamp((s.elapsed-s.tell)/HollowMatronRules.POUNCE_TICKS,0,1));
		thread(s);
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: hanging or standing (or turning over between), walking, then the move itself. */
	private void pose(HollowMatronRenderState s,int move,float el,int tell,int side){
		float t=s.ageInTicks,d=HollowMatronRules.drop(move,el);
		pitch=0;roll=0;yaw=0;headX=0;fangs=0;palps=0;abdX=.2F;
		for(int i=0;i<8;i++){a[i]=STAND_A;b[i]=STAND_B;fan[i]=side(i)*FAN[i/2];}
		hang(d,t);
		float up=smooth(d,HollowMatronRules.FLIP_FROM,HollowMatronRules.FLIP_TO);
		// Breathing, her gaze followed once she is down, and her palps never still.
		abdX+=Mth.sin(t*.07F)*.025F;
		headY=Mth.clamp(s.yRot,-40,40)*Mth.DEG_TO_RAD*.4F*up;headX+=s.xRot*Mth.DEG_TO_RAD*.3F*up;
		palps+=Mth.sin(t*.23F)*.12F+Mth.sin(t*.61F)*.05F;
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
	 * rears her back with her fangs spread, then lunges; the sweep raises her forelegs to one side and swings them across; the spit rears
	 * her and curls her abdomen under her; the pounce crouches her on bent legs, then flings her with her forelegs reaching; skittering,
	 * her legs blur; spent, she sinks low on splayed legs with her abdomen heaving.
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
			}
			case HollowMatronRules.SWEEP->{
				float up=wind*(1-ramp(el,tell+HollowMatronRules.SWEEP_TICKS,5));
				float cross=smooth(el,tell,tell+HollowMatronRules.SWEEP_TICKS);
				float across=Mth.lerp(cross,side*.7F,-side*.8F)*up;
				for(int i=0;i<4;i++){a[i]+=(i<2?1.0F:.7F)*up;b[i]-=(i<2?.6F:.4F)*up;fan[i]+=across;}
				yaw=across*.35F;pitch-=.15F*up;fangs+=.3F*up;
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
				for(int i=0;i<8;i++){a[i]+=.45F*crouch;b[i]+=.45F*crouch;}
				pitch+=.1F*crouch;abdX+=.2F*crouch;fangs+=.3F*crouch;
				if(el>tell){
					float p=Mth.clamp((el-tell)/HollowMatronRules.POUNCE_TICKS,0,1),fly=Mth.sin(Mth.PI*p);
					for(int i=0;i<8;i++){
						int sd=side(i);
						if(i<4){a[i]-=.3F*fly;b[i]-=.9F*fly;fan[i]+=sd*.3F*fly;}
						else{b[i]-=.7F*fly;fan[i]-=sd*.3F*fly;}
					}
					pitch+=Mth.lerp(p,-.25F,.2F)*fly;fangs+=.6F*fly;
				}
			}
			case HollowMatronRules.SKITTER->{
				gait(el*2.4F,1);
				if(side==0){headX-=.3F;fangs+=.5F;pitch-=.15F;palps+=.3F;}
				else roll+=side*.12F;
			}
			case HollowMatronRules.RECOVER->{
				float k=ramp(el,0,4),heave=Mth.sin(el*.5F);
				for(int i=0;i<8;i++){a[i]+=.3F*k;b[i]+=.45F*k;fan[i]*=1+.15F*k;}
				abdX+=(.25F+.06F*heave)*k;pitch+=.03F*heave*k;palps-=.3F*k;fangs-=.05F*k;
			}
			default->{}
		}
	}

	/** Sets the parts from the pose built. */
	private void apply(){
		body.xRot=pitch;body.zRot=roll;body.yRot=yaw;
		head.xRot=headX;head.yRot=headY;
		abdomen.xRot=abdX;
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
	}
}
