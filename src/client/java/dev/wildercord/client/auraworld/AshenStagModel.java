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
	private final ModelPart body,neck,head,tail,ruff;
	private final ModelPart[] ears=new ModelPart[2],antlers=new ModelPart[2];
	private final ModelPart[] legs=new ModelPart[4],shins=new ModelPart[4];
	private final ModelPart[] posed;
	private final PoseBlend blender;
	/** The pose being built: each leg's thigh angle in the world and shin angle from the thigh, the body's pitch and roll, the neck, the head. */
	private final float[] u=new float[4],l=new float[4],uz=new float[4];
	private float pitch,roll,yaw,neckX,neckY,headX,tailX;
	public AshenStagModel(ModelPart root){
		super(root);body=root.getChild("body");neck=body.getChild("neck");head=neck.getChild("head");tail=body.getChild("tail");ruff=neck.getChild("ruff");
		ears[0]=head.getChild("earR");ears[1]=head.getChild("earL");antlers[0]=head.getChild("antlerR");antlers[1]=head.getChild("antlerL");
		String[] names={"legFL","legFR","legHL","legHR"};
		for(int i=0;i<4;i++){legs[i]=body.getChild(names[i]);shins[i]=legs[i].getChild("shin");}
		posed=new ModelPart[]{body,neck,head,tail,legs[0],legs[1],legs[2],legs[3],shins[0],shins[1],shins[2],shins[3]};blender=new PoseBlend(posed);
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
		pose(s,s.move,hold(s.move,s.elapsed,s.tell),s.tell,s.side);
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1){
			blender.store();
			root().resetPose();
			pose(s,s.prevMove,hold(s.prevMove,s.prevElapsed,s.prevTell),s.prevTell,s.side);
			float k=s.blend*s.blend*(3-2*s.blend);
			blender.mix(k);
		}
		secondary(s);
		hurt(s);
		die(s);
		ground();
		// Off the ground through a bound, highest halfway.
		if(s.move==AshenStagRules.BOUND&&s.deathTime<=0)body.y-=5*Mth.sin(Mth.PI*Mth.clamp(s.elapsed/Math.max(1,s.tell),0,1));
		// Rolled onto its side in death, it lies on its flank.
		if(s.deathTime>0){
			body.y=Mth.lerp(smooth(s.deathTime,6,18),body.y,24-5.5F);
			// The flank lands with a small bounce, then lies still.
			float landed=s.deathTime-17;
			if(landed>0)body.y-=2.2F*(float)(Math.exp(-.5F*landed)*Math.abs(Mth.sin(landed*.9F)));
		}
	}

	/**
	 * The hit-stop: a beat in which the pose of a blow's landing is held before the move plays on, so the hit has weight. Only the moves
	 * whose blow lands once (the toss, buck, rear, lunge and kindling), and the held time is taken out of the move's own clock after it.
	 */
	private static float hold(int move,float el,int tell){
		if(move!=AshenStagRules.TOSS&&move!=AshenStagRules.BUCK&&move!=AshenStagRules.REAR&&move!=AshenStagRules.LUNGE&&move!=AshenStagRules.KINDLE)return el;
		float at=Math.max(1,tell)+2;
		return el<at?el:el<at+1.5F?at:el-1.5F;
	}

	/**
	 * What follows the body rather than being posed: the ears flick on their own periods and flatten for a charge, the antlers and
	 * ruff swing a beat behind the head and lift on a rear, the tail whips after a blow and swings behind a stride, and every move
	 * rings on past its blow (the neck and head overshoot and settle) instead of stopping dead.
	 */
	private void secondary(AshenStagRenderState s){
		float t=s.ageInTicks,r=s.rise;
		boolean moving=s.move!=0;
		float walk=Math.min(1,s.walkAnimationSpeed)*r;
		// Ears: each flicks on its own period, both pinned back through a tell.
		float tellK=moving&&s.tell>0?ramp(s.elapsed,0,s.tell)*(1-ramp(s.elapsed,s.tell,3)):0;
		for(int i=0;i<2;i++){
			float sign=i==0?-1:1,flick=(float)Math.pow(Math.max(0,Mth.sin(t*(.21F+.05F*i)+i*2.1F)),9);
			ears[i].xRot=.2F+.2F*flick+.7F*tellK;
			ears[i].zRot=sign*(1.0F+.35F*flick+.5F*tellK)+Mth.sin(t*.08F+i*1.7F)*.04F;
		}
		// Antlers and ruff trail the head by a few ticks: they swing against the neck's turn and back, and bob with the stride.
		float lag=Mth.sin(t*.09F-.7F)*.02F+Mth.sin(s.walkAnimationPos*1.2F-1.3F)*.04F*walk;
		antlers[0].zRot=-.5F-neck.yRot*.18F+lag;antlers[1].zRot=.5F-neck.yRot*.18F-lag;
		antlers[0].xRot=-.25F+neck.xRot*.1F+lag;antlers[1].xRot=-.25F+neck.xRot*.1F+lag;
		ruff.xRot=Mth.sin(t*.06F-.9F)*.03F+Mth.sin(s.walkAnimationPos*1.2F-.8F)*.07F*walk+.15F*tellK;
		// Tail: swung behind the stride, a lazy sway standing, flagged by the move, and whipped after the blow.
		float since=moving&&s.tell>0?s.elapsed-s.tell:-1;
		float ring=since>=0?(float)(Math.exp(-.28F*since)*Mth.sin(since*.85F)):0;
		tail.xRot+=Mth.sin(s.walkAnimationPos*1.2F-1.6F)*.12F*walk-.35F*ring*(s.move==AshenStagRules.RECOVER?0:1);
		tail.yRot=Mth.sin(t*.07F+1.0F)*.1F*(1-walk)+Mth.sin(s.walkAnimationPos*.6F-.8F)*.25F*walk+.3F*ring;
		// Follow-through: the head runs on past the blow and settles, the neck a tick behind it.
		if(since>=0&&s.move!=AshenStagRules.RECOVER&&s.move!=AshenStagRules.STUCK&&s.deathTime<=0){
			float over=(float)(Math.exp(-.3F*since)*Mth.sin(since*.9F));
			head.xRot+=.22F*over;neck.xRot+=.1F*(float)(Math.exp(-.3F*Math.max(0,since-1))*Mth.sin(Math.max(0,since-1)*.9F));
		}
		// Breathing on offset periods: the barrel, then the neck, then the head, so the loop never lines up.
		body.yScale=1+Mth.sin(t*.07F)*.008F*(1-walk);
		neck.xRot+=Mth.sin(t*.043F+1.1F)*.02F;
		head.zRot=Mth.sin(t*.051F+2.2F)*.03F;
		// The hind legs give a little when the pose lands: weight settles through the shins.
		if(since>=0&&since<8)for(int i=2;i<4;i++)shins[i].xRot+=.12F*(float)(Math.exp(-.35F*since)*Mth.cos(since*.9F));
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
		attack(move,el,tell,side,t,s.kindled);
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
	 * toss drops its head low and twisted to one side and wrenches it up; the buck sinks its head to its knees and lashes both hind
	 * hooves out high; the rear stands it up on its hind legs and drives its forehooves down (twice once kindled); the charge (and each
	 * run of a rush) lowers its antlers and paws, then gallops; the rake drags its brow side to side and flicks its head up with each
	 * cinder; the stampede throws its head back bellowing; the smoke shakes its head hard; the stalk slinks it low; the lunge crouches it
	 * on its haunches and springs it out flat; the lock levels its antlers, drives in and heaves at whoever it holds, then throws them;
	 * the skid sits it back on dug-in hooves; reeling, its head swings; kindling, it rears as high as it goes and crashes down; the bound
	 * gathers its legs under it in the air; spent, its head hangs and its sides heave; stuck, it braces all four legs and wrenches at
	 * its antlers.
	 */
	private void attack(int move,float el,int tellTicks,int side,float t,boolean kindled){
		float tell=Math.max(1,tellTicks);
		float wind=ramp(el,0,tell),go=ramp(el,tell,2);
		switch(move){
			case AshenStagRules.TOSS->{
				// Head dropped low and twisted to one side, the low tine scraping the ash and the forelegs splayed; then wrenched up and
				// round through whoever is in front, the chest thrown open, and held up there a moment.
				float down=wind*(1-go),scrape=Mth.sin(el*1.6F)*.05F*down;
				neckX+=.7F*down-.75F*go;headX+=.5F*down-.5F*go;neckY+=side*(.35F*down-.2F*go)+scrape;
				roll=side*.06F*down;pitch+=.14F*down-.2F*go;
				u[0]-=.3F*down;u[1]-=.3F*down;l[0]+=.35F*down;l[1]+=.35F*down;uz[0]=-.15F*down;uz[1]=.15F*down;
				u[2]+=.25F*go;u[3]+=.25F*go;tailX+=.4F*go;
			}
			case AshenStagRules.BUCK->{
				// Head down to its knees and tail flagged, its weight rocked onto braced forelegs and the hind legs gathered under it; then
				// both hind hooves lashed out high behind it, the hindquarters thrown up after them.
				pitch+=.2F*wind+.25F*go;neckX+=.5F*wind;headX+=.35F*wind;
				u[0]-=.15F*wind;u[1]-=.15F*wind;
				u[2]=u[3]=Mth.lerp(go,HIND_U-.5F*wind,1.55F);l[2]=l[3]=Mth.lerp(go,HIND_L+.7F*wind,-.2F);
				tailX+=.9F*wind;
			}
			case AshenStagRules.REAR->{
				// Up on its hind legs, belly bare and forehooves pawing the air, head thrown back; then both forehooves driven down hard,
				// the head dropping with them. Kindled, it heaves itself up again straight off the landing and stamps a second time.
				float up=wind*(1-go),down=go;
				if(kindled&&el>tell+2){
					float a=ramp(el,tell+2,AshenStagRules.REAR_AGAIN-2)*(1-ramp(el,tell+AshenStagRules.REAR_AGAIN,2));
					up=Math.max(up,a);down=go*(1-a);
				}
				pitch+=-1.0F*up+.2F*down;neckX+=-.5F*up+.15F*down;headX+=-.25F*up+.3F*down;
				float paw=Mth.sin(el*.9F)*.3F*up;
				u[0]=Mth.lerp(down,-1.05F*up+paw,-.2F);u[1]=Mth.lerp(down,-1.05F*up-paw,-.2F);
				l[0]=Mth.lerp(down,1.6F*up,0);l[1]=Mth.lerp(down,1.6F*up,0);
				u[2]=u[3]=Mth.lerp(up,HIND_U,.15F);l[2]=l[3]=Mth.lerp(up,HIND_L,.3F);
				tailX+=.5F*up;
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
			case AshenStagRules.RAKE->{
				// Brow down in the ash and the antlers dragged side to side, forelegs spread wide; then with each cinder its head is
				// flicked up, throwing it.
				float sweep=Mth.sin(el*.55F);
				float flick=el>=tell&&el<tell+AshenStagRules.RAKE_TICKS?(float)Math.pow(Math.max(0,1-((el-tell)%AshenStagRules.RAKE_EVERY)/3F),2):0;
				neckX+=.85F*wind-.6F*flick;headX+=.45F*wind-.4F*flick;neckY+=sweep*.45F*wind;roll=sweep*.05F*wind;
				pitch+=.16F*wind;
				u[0]-=.35F*wind;u[1]-=.35F*wind;l[0]+=.5F*wind;l[1]+=.5F*wind;uz[0]=-.2F*wind;uz[1]=.2F*wind;
				u[2]+=.1F*wind;u[3]+=.1F*wind;tailX+=.3F*flick;
			}
			case AshenStagRules.STAMPEDE->{
				// Head thrown back and neck stretched up, bellowing the herd on and a forehoof stamping time; the call shakes it.
				float beat=Math.max(0,Mth.sin(el*.6F));
				neckX-=.7F*wind;headX-=.55F*wind;pitch-=.12F*wind;
				u[1]=-.5F*beat*wind;l[1]=1.1F*beat*wind;
				float shake=el>=tell?Mth.sin(el*2.2F)*.08F*(1-ramp(el,tell,12)):0;
				neckY+=shake;roll=shake*.4F;
				u[2]-=.12F*wind;u[3]-=.12F*wind;tailX+=.6F*wind;
			}
			case AshenStagRules.SMOKE->{
				// Head swung low and shaken hard side to side, smoke pouring off its hide, legs braced wide.
				float shake=Mth.sin(el*1.9F)*(.25F+.25F*wind);
				neckX+=.5F*wind;headX+=.2F*wind;neckY+=shake*wind;roll=-shake*.12F*wind;pitch+=.05F*wind;
				for(int i=0;i<4;i++)uz[i]=(i%2==0?-.18F:.18F)*wind;
			}
			case AshenStagRules.STALK->{
				// Slunk low through its smoke, head down and level, every leg bent and the tail clamped.
				float k=ramp(el,0,6);
				neckX+=.7F*k;headX-=.15F*k;pitch+=.06F*k;
				for(int i=0;i<4;i++){u[i]-=.25F*k;l[i]+=.5F*k;}
				tailX-=.35F*k;
			}
			case AshenStagRules.LUNGE->{
				// Crouched right down on gathered hind legs, antlers low and aimed; then sprung out flat, forelegs reaching and the hind
				// legs flung back.
				float crouch=wind*(1-go),spring=go*(1-ramp(el,tell+AshenStagRules.LUNGE_TICKS,4));
				neckX+=.6F*crouch+.3F*spring;headX+=.2F*crouch;pitch+=.12F*crouch-.1F*spring;
				u[2]-=.55F*crouch;u[3]-=.55F*crouch;l[2]+=.9F*crouch;l[3]+=.9F*crouch;
				u[0]+=.15F*crouch;u[1]+=.15F*crouch;l[0]+=.3F*crouch;l[1]+=.3F*crouch;
				u[0]-=.9F*spring;u[1]-=.9F*spring;u[2]+=1.1F*spring;u[3]+=1.1F*spring;l[2]-=.2F*spring;l[3]-=.2F*spring;
				tailX+=.5F*spring;
			}
			case AshenStagRules.LOCK->{
				// Antlers levelled at chest height and a forehoof scraping, hindquarters set; then driven forward, and whoever it catches
				// held on its antlers while it shoves and grinds in heaves, until it flings its head up and throws them.
				neckX+=.45F*wind;headX+=.35F*wind;pitch+=.08F*wind;
				if(el<=tell){float scrape=Mth.sin(el*1.1F);u[0]=-.3F*Math.max(0,scrape)*wind+.3F*Math.max(0,-scrape)*wind;l[0]=.8F*Math.max(0,scrape)*wind;}
				u[2]+=.35F*go;u[3]+=.35F*go;l[2]-=.15F*go;l[3]-=.15F*go;u[1]-=.2F*go;
				float held=el-tell-AshenStagRules.LOCK_TICKS;
				if(held>0){
					float heave=(float)Math.pow(Math.max(0,Mth.sin(held*Mth.TWO_PI/AshenStagRules.LOCK_GRIND)),3),fling=ramp(held,AshenStagRules.LOCK_HOLD-6,4);
					pitch+=.08F*heave;neckY+=Mth.sin(held*.9F)*.15F*heave;u[2]+=.15F*heave;u[3]+=.15F*heave;
					neckX-=1.1F*fling;headX-=.6F*fling;pitch-=.15F*fling;
				}
			}
			case AshenStagRules.SKID->{
				// Forehooves dug in ahead of it and sat back on its haunches as it slides, head flung up; then pulling round, flank turned.
				float dig=ramp(el,0,2)*(1-smooth(el,AshenStagRules.SKID_SLIDE,AshenStagRules.SKID_SLIDE+6));
				pitch-=.3F*dig;neckX-=.35F*dig;headX-=.2F*dig;
				u[0]-=.7F*dig;u[1]-=.6F*dig;
				u[2]-=.45F*dig;u[3]-=.45F*dig;l[2]+=.7F*dig;l[3]+=.7F*dig;
				roll=Mth.sin(el*.8F)*.04F*dig;tailX+=.3F*dig;
			}
			case AshenStagRules.STAGGER->{
				// Reeling back from the wrench, legs splayed, head swinging low from side to side as it shakes off the blows.
				float k=ramp(el,0,3),swing=Mth.sin(el*.45F);
				neckX+=.55F*k;headX+=.25F*k;neckY+=swing*.4F*k;roll=swing*.08F*k;yaw=swing*.06F*k;
				pitch-=.1F*k*(1-ramp(el,0,8));
				for(int i=0;i<4;i++)uz[i]=(i%2==0?-.22F:.22F)*k;
				u[0]-=.2F*k;u[1]-=.2F*k;
			}
			case AshenStagRules.KINDLE->{
				// Reared up as high as it goes and held there bellowing at the sky, forehooves churning and the whole of it shuddering as
				// the fire climbs; then it crashes down on all fours, head driven at the ground.
				float up=smooth(el,0,tell*.4F)*(1-go),shudder=Mth.sin(el*2.5F)*.03F*up*wind;
				pitch+=-1.1F*up+.25F*go+shudder;neckX+=-.8F*up+.45F*go;headX+=-.6F*up+.3F*go;
				float churn=Mth.sin(el*.7F)*.4F*up;
				u[0]=Mth.lerp(go,-1.2F*up+churn,-.25F);u[1]=Mth.lerp(go,-1.2F*up-churn,-.25F);
				l[0]=Mth.lerp(go,1.4F*up,0);l[1]=Mth.lerp(go,1.4F*up,0);
				u[2]=u[3]=Mth.lerp(up,HIND_U,.2F);l[2]=l[3]=Mth.lerp(up,HIND_L,.25F);
				roll=shudder;tailX+=.7F*up;
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
		body.xRot-=.08F*k;ears[0].xRot+=.5F*k;ears[1].xRot+=.5F*k;tail.xRot+=.5F*k;
		for(int i=0;i<4;i++){legs[i].zRot+=(i%2==0?-1:1)*.1F*k;shins[i].xRot+=.18F*k;}
		body.y+=1.2F*k;
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
		ears[0].xRot=Mth.lerp(buckle,ears[0].xRot,.8F);ears[1].xRot=Mth.lerp(buckle,ears[1].xRot,.8F);
		ears[0].zRot=Mth.lerp(fall,ears[0].zRot,-1.5F);ears[1].zRot=Mth.lerp(fall,ears[1].zRot,1.5F);
	}
}
