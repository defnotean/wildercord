package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.SwordTombRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * A carved, blindfolded burial effigy with segmented stone arms, a long brass-bound blade and hanging cloth. Each of its blows has
 * a stance of its own, held through the tell: the blade drawn back for the sweep, levelled for the thrust, trailed low behind for
 * the heel-turn, held crosswise for the charge, raised two-handed overhead for the leap, plunged point down as it kneels for the
 * hands; and still, head cocked and a hand to the blindfold, while it listens. The sweep and the thrust snap home with an overshoot,
 * hold a beat on contact and stagger on into the recovery; its cloth, crest and blindfold lag behind its body; a blow flinches it;
 * and dying it staggers, folds to its knees, topples forward onto its blade arm and settles with a small bounce.
 */
public final class GravekeeperModel extends EntityModel<GravekeeperRenderState> {
	private final ModelPart body,head,blindfold,right,left,legR,legL,cloth,hem,crest,blade;
	private final ModelPart[] posed;
	public GravekeeperModel(ModelPart root){
		super(root);body=root.getChild("body");head=body.getChild("head");blindfold=head.getChild("blindfold");right=body.getChild("right");left=body.getChild("left");
		legR=body.getChild("legR");legL=body.getChild("legL");cloth=body.getChild("cloth");hem=cloth.getChild("hem");crest=body.getChild("crest");blade=right.getChild("blade");
		posed=new ModelPart[]{body,head,blindfold,right,left,legR,legL,cloth,hem,crest,blade};
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-6,-14,-4,12,20,8),PartPose.offset(0,4,0));
		body.addOrReplaceChild("collar",CubeListBuilder.create().texOffs(40,0).addBox(-8,-2,-5,16,3,10),PartPose.offset(0,-13,0));
		var head=body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,32).addBox(-6,-10,-5,12,10,10),PartPose.offset(0,-14,0));
		head.addOrReplaceChild("blindfold",CubeListBuilder.create().texOffs(48,34).addBox(-6.5F,-7,-5.5F,13,3,11),PartPose.ZERO);
		head.addOrReplaceChild("brow",CubeListBuilder.create().texOffs(40,0).addBox(-7,-1,-6,14,2,12),PartPose.offset(0,-10,0));
		for(int sign:new int[]{-1,1}){
			var arm=body.addOrReplaceChild(sign==-1?"right":"left",CubeListBuilder.create().texOffs(72,0).addBox(-3,-2,-3,6,17,6),PartPose.offset(sign*9,-10,0));
			arm.addOrReplaceChild("cuff",CubeListBuilder.create().texOffs(40,0).addBox(-3.5F,-1,-3.5F,7,3,7),PartPose.offset(0,12,0));
			body.addOrReplaceChild(sign==-1?"legR":"legL",CubeListBuilder.create().texOffs(96,0).addBox(-3,0,-3,6,14,6),PartPose.offset(sign*3.5F,6,0));
		}
		var arm=body.getChild("right");var sword=arm.addOrReplaceChild("blade",CubeListBuilder.create().texOffs(104,32).addBox(-1.5F,1,-1,3,24,2),PartPose.offset(0,13,-1));
		sword.addOrReplaceChild("guard",CubeListBuilder.create().texOffs(40,0).addBox(-6,-1,-2,12,2,4),PartPose.ZERO);
		sword.addOrReplaceChild("grip",CubeListBuilder.create().texOffs(48,34).addBox(-1,-6,-1,2,5,2),PartPose.ZERO);
		// The hanging cloth, with a loose hem below it that swings a beat behind.
		var cloth=body.addOrReplaceChild("cloth",CubeListBuilder.create().texOffs(64,48).addBox(-5,0,-.2F,10,13,0),PartPose.offset(0,4,-4.4F));
		cloth.addOrReplaceChild("hem",CubeListBuilder.create().texOffs(64,48).addBox(-5,0,-.2F,10,5,0),PartPose.offset(0,13,0));
		body.addOrReplaceChild("crest",CubeListBuilder.create().texOffs(40,0).addBox(-3,-3,-.5F,6,6,1),PartPose.offset(0,-6,-4.5F));
		return LayerDefinition.create(mesh,128,64);
	}

	@Override public void setupAnim(GravekeeperRenderState s){
		super.setupAnim(s);
		pose(s,s.move,s.elapsed,s.tell,s.prevMove);
		// Blending out of the move before, so one blow flows into the next instead of snapping.
		if(s.blend<1&&s.prevMove!=s.move){
			PartPose[] now=new PartPose[posed.length];
			for(int i=0;i<posed.length;i++)now[i]=posed[i].storePose();
			root().resetPose();
			pose(s,s.prevMove,s.prevElapsed,s.prevTell,s.prevMove);
			float k=s.blend*s.blend*(3-2*s.blend);
			for(int i=0;i<posed.length;i++){
				PartPose a=posed[i].storePose(),b=now[i];
				posed[i].loadPose(PartPose.offsetAndRotation(Mth.lerp(k,a.x(),b.x()),Mth.lerp(k,a.y(),b.y()),Mth.lerp(k,a.z(),b.z()),
					Mth.lerp(k,a.xRot(),b.xRot()),Mth.lerp(k,a.yRot(),b.yRot()),Mth.lerp(k,a.zRot(),b.zRot())));
			}
		}
		hurt(s);
		die(s);
	}

	/** 0 to 1 over {@code ticks}, from {@code from}, eased out. */
	private static float ramp(float t,float from,float ticks){float k=Mth.clamp((t-from)/ticks,0,1);return 1-(1-k)*(1-k);}
	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code e} ticks in ({@code from} being the move before, which a recovery follows). */
	private void pose(GravekeeperRenderState s,int move,float e,int tellTicks,int from){
		float t=s.ageInTicks,ws=Math.min(1,s.walkAnimationSpeed),wp=s.walkAnimationPos*.55F;
		float wind=Mth.clamp(e/Math.max(1,tellTicks),0,1);
		head.yRot=s.yRot*Mth.DEG_TO_RAD*.65F;head.xRot=.05F+s.xRot*Mth.DEG_TO_RAD*.4F;
		legL.xRot=Mth.cos(wp)*ws*.5F;legR.xRot=-legL.xRot;
		right.xRot=-.85F;right.zRot=.08F;left.xRot=-.2F;left.zRot=-.08F;
		// Walking: the body drops onto each planted foot and twists against the stride, the head keeps level, the free arm swings.
		float bob=Math.abs(Mth.cos(wp))*ws;
		body.y+=bob*.8F;body.yRot+=Mth.sin(wp)*.07F*ws;body.zRot+=Mth.sin(wp)*.025F*ws;
		head.yRot-=Mth.sin(wp)*.06F*ws;head.xRot-=bob*.05F;left.xRot+=Mth.sin(wp)*.18F*ws;
		// Idle: breathing on periods that do not line up, the blade drifting, and, standing, the head slowly scanning like it listens.
		body.xRot+=Mth.sin(t*.06F)*.015F;body.y+=Mth.sin(t*.06F)*.3F;
		right.zRot+=Mth.sin(t*.045F+1)*.025F;left.zRot+=Mth.sin(t*.07F+2)*.03F;left.xRot+=Mth.sin(t*.05F+.5F)*.03F;
		blade.xRot+=Mth.sin(t*.053F+1.3F)*.04F;head.zRot+=Mth.sin(t*.031F)*.02F;
		if(move==SwordTombRules.IDLE){
			float cock=Math.max(0,Mth.sin(t*.04F));
			head.yRot+=(Mth.sin(t*.021F)*.18F+Mth.sin(t*.047F)*.07F)*(1-ws);head.zRot+=cock*cock*.14F*(1-ws);
		}
		// Secondary motion: the cloth swings with the stride, its hem a beat behind it; the crest and blindfold sway on their own time.
		cloth.xRot=.08F+Math.abs(legL.xRot)*.45F+Mth.sin(t*.05F)*.025F;cloth.zRot=Mth.sin(t*.04F)*.02F;
		hem.xRot=Math.abs(Mth.cos(wp-.9F))*ws*.5F+Mth.sin(t*.05F-1)*.04F;hem.zRot=Mth.sin(t*.04F-.8F)*.04F;
		crest.zRot=Mth.sin(t*.07F)*.12F;crest.yRot=Mth.sin(t*.05F+1)*.1F;
		blindfold.zRot=Mth.sin(t*.09F+.5F)*.02F;blindfold.yRot=Mth.sin(t*.07F)*.02F;
		switch(move){
			case SwordTombRules.SWEEP->{
				// Gathered low, the blade drawn far back and trembling, the head turned the other way; the ones who read it dodge.
				right.xRot=-1.2F-wind*.35F;right.yRot=-.9F-wind*.55F+Mth.sin(e*2.6F)*.02F*wind*wind*wind;body.yRot=-wind*.3F;left.xRot=-.45F;
				body.y+=wind*1F;legL.xRot+=.25F*wind;legR.xRot-=.2F*wind;head.yRot+=wind*.35F;blade.xRot-=.25F*wind;cloth.xRot+=.15F*wind;hem.xRot+=.1F*wind;
			}
			case SwordTombRules.THRUST->{
				// Crouched with the blade levelled, the front foot set ahead and the arm drawn in; the arm quivers as it is about to go.
				right.xRot=-1.55F;right.yRot=-.18F;right.z=-wind*1.6F+Mth.sin(e*2.6F)*.15F*wind*wind*wind;body.xRot=-wind*.15F;left.xRot=-1.1F;
				body.y+=wind*1.5F;legR.xRot-=.35F*wind;legL.xRot+=.3F*wind;head.xRot-=.1F*wind;cloth.xRot-=.1F*wind;
			}
			case SwordTombRules.GUARD->{
				right.xRot=-1.3F;right.yRot=-.8F;right.zRot=-.6F;left.xRot=-1.35F;left.yRot=.55F;blade.zRot=.8F;
				body.y+=.8F;body.xRot+=.06F;head.xRot-=.05F;
			}
			case SwordTombRules.RECOVER->recover(e,from);
			case SwordTombRules.BROKEN->{
				float sag=ramp(e,0,5);
				body.xRot=-.3F*sag;head.xRot=-.4F*sag;right.xRot=Mth.lerp(sag,right.xRot,.2F);left.zRot=Mth.lerp(sag,left.zRot,-.8F);body.zRot=Mth.sin(e*.35F)*.045F;
				head.zRot+=Mth.sin(e*.21F)*.08F*sag;cloth.xRot+=.2F*sag;hem.xRot+=Mth.sin(e*.3F-1)*.1F*sag;
			}
			case SwordTombRules.LISTEN->{
				// Stock still, the blade point lowered, head turning this way and that, a hand raised to the blindfold.
				float w=Mth.clamp(e/SwordTombRules.LISTEN_TELL,0,1);
				head.yRot=Mth.sin(e*.25F)*.6F*w;head.zRot=Mth.sin(e*.13F)*.25F*w;head.xRot=.05F+.15F*w;
				left.xRot=-2.4F*w;left.zRot=.35F*w;right.xRot=-.85F+.5F*w;blade.xRot=.4F*w;body.xRot=.1F*w;
				blindfold.zRot+=Mth.sin(e*.5F)*.03F*w;
			}
			case SwordTombRules.LOST->{
				head.xRot=.5F;head.yRot=Mth.sin(e*.08F)*.3F;right.xRot=-.2F;left.xRot=0;body.xRot=.08F;
				head.zRot+=Mth.sin(e*.05F)*.1F;left.zRot+=Mth.sin(e*.06F+1)*.08F;
			}
			case SwordTombRules.VERDICT->{
				int crouch=SwordTombRules.VERDICT_CROUCH,leap=SwordTombRules.VERDICT_LEAP;
				if(e<crouch+leap-1){
					// Crouched with the blade raised two-handed overhead; in the air, knees tucked.
					float w=Mth.clamp(e/crouch,0,1),air=e<crouch?0:1;
					right.xRot=-.85F-2.1F*w;left.xRot=-2.9F*w;left.yRot=-.3F*w;right.yRot=.3F*w;
					body.y+=3*w*(1-air);body.xRot=-.15F*air;
					legR.xRot=-.6F*w-.3F*air;legL.xRot=.4F*w-.7F*air;
					cloth.xRot+=.5F*air;hem.xRot+=.6F*air;
				}else{
					// The blade driven into the ground before it, bent over the hilt and tugging at it; the landing jolts it down.
					float t2=e-(crouch+leap-1),tug=Mth.sin(t2*1.3F)*.05F,land=(float)Math.exp(-t2*.5F);
					right.xRot=-1F;left.xRot=-1F;left.yRot=-.5F;right.yRot=.5F;body.xRot=.45F+tug+.2F*land;body.zRot=tug;head.xRot=.3F;
					legR.xRot=-.4F;legL.xRot=.3F;body.y+=2.5F*land;cloth.xRot+=.7F*land;hem.xRot+=Mth.sin(t2*.8F)*.4F*land;
				}
			}
			case SwordTombRules.HEEL->{
				int tell=SwordTombRules.HEEL_TELL;
				if(e<=tell){
					// Wound up the wrong way, the blade trailed low behind it.
					float w=Mth.clamp(e/tell,0,1);
					right.xRot=.9F*w;right.zRot=.08F+.3F*w;body.yRot=-.5F*w;body.xRot=.2F*w;legR.xRot=-.3F*w;legL.xRot=.3F*w;left.xRot=-.6F*w;
					head.yRot+=.4F*w;body.y+=.8F*w;cloth.xRot+=.2F*w;
				}else{
					// Then one full turn, the blade held out low, the cloth flung out wide.
					float u=Mth.clamp((e-tell)/SwordTombRules.HEEL_SPIN,0,1);
					body.yRot=u>=1?0:-.5F*(1-u)+u*Mth.TWO_PI;right.xRot=.4F;right.zRot=1.2F;body.xRot=.15F;left.zRot=-.6F;legR.xRot=-.3F;legL.xRot=.3F;
					cloth.xRot=.9F;hem.xRot=1.1F;head.yRot-=.4F*u;
				}
			}
			case SwordTombRules.HANDS->{
				// The blade raised and turned point down, then plunged; kneeling over it, head bowed.
				int tell=SwordTombRules.HANDS_TELL;float w=Mth.clamp(e/(tell-2),0,1),k=Mth.clamp((e-tell+2)/3,0,1);
				right.xRot=Mth.lerp(k,-.85F-2.2F*w,-.9F);left.xRot=Mth.lerp(k,-3F*w,-.9F);right.yRot=.4F*k;left.yRot=-.4F*k;
				blade.xRot=Mth.lerp(k,Mth.PI*w,.9F);
				body.y+=5*k;legR.xRot=-1.4F*k;legL.xRot=.5F*k;head.xRot=.05F+.45F*k;body.zRot=Mth.sin(e*.4F)*.02F*k;
				float jolt=(float)Math.exp(-Math.max(0,e-tell+2)*.45F)*k;
				body.y+=1.5F*jolt;body.xRot+=.15F*jolt;cloth.xRot+=.4F*jolt;hem.xRot+=Mth.sin(e*.8F)*.3F*jolt;
			}
			case SwordTombRules.RAMPART->{
				// The blade held crosswise before its chest, leaning in, the back foot planted; then the charge, the cloth streaming behind.
				int tell=SwordTombRules.RAMPART_TELL;float w=Mth.clamp(e/tell,0,1);
				right.xRot=-.85F-.6F*w;left.xRot=-1.45F*w;right.yRot=.35F*w;left.yRot=-.35F*w;blade.zRot=1.57F*w;body.xRot=.25F*w;head.xRot=-.15F*w;
				if(e<tell){legL.xRot=.5F*w;legR.xRot=-.3F*w;body.y+=Math.abs(Mth.sin(e*Mth.PI/6))*.6F*w;cloth.xRot+=.2F*w;}
				else{
					legR.xRot=Mth.sin(e*1.2F)*.9F;legL.xRot=-legR.xRot;body.xRot=.35F;
					body.y+=Math.abs(Mth.cos(e*1.2F))*1.1F;cloth.xRot=.8F;hem.xRot=.9F+Mth.sin(e*1.2F-1)*.2F;
				}
			}
			case SwordTombRules.WALLED->{
				// Run into the wall: arms out stiff, the blade still crosswise, shaking, the cloth settling after it.
				float shake=Mth.sin(e*2.1F)*.06F*(1-Mth.clamp(e/SwordTombRules.WALLED_TICKS,0,1));
				right.xRot=-1.57F;left.xRot=-1.57F;right.yRot=.35F;left.yRot=-.35F;blade.zRot=1.57F;body.xRot=-.15F;head.xRot=-.3F;body.zRot=shake;head.zRot=shake*2;
				float hit=(float)Math.exp(-e*.3F);
				cloth.xRot+=.8F*hit;hem.xRot+=Mth.sin(e*.7F-1)*.5F*hit;body.xRot-=.12F*hit;
			}
			case SwordTombRules.RITE->{
				// Kneeling, both hands on the pommel of the planted blade, head bowed; then rising, arms thrown wide with a little overshoot.
				float w=Mth.clamp(e/8,0,1),r=Mth.clamp((e-(SwordTombRules.RITE_TICKS-6))/6F,0,1);
				right.xRot=Mth.lerp(r,-.9F*w,-.2F);left.xRot=Mth.lerp(r,-.9F*w,-.2F);right.yRot=.4F*w*(1-r);left.yRot=-.4F*w*(1-r);
				right.zRot=.08F+1.3F*r+.3F*Mth.sin(Mth.PI*r);left.zRot=-.08F-1.3F*r-.3F*Mth.sin(Mth.PI*r);blade.xRot=.9F*w*(1-r);
				body.y+=4*w*(1-r);legR.xRot=-1.4F*w*(1-r);legL.xRot=.5F*w*(1-r);
				head.xRot=Mth.lerp(r,.7F*w,-.4F);body.xRot=-.15F*r;
				cloth.xRot+=.5F*Mth.sin(Mth.PI*r);hem.xRot+=.6F*Mth.sin(Mth.PI*r);
			}
			default->{}
		}
	}

	/**
	 * The recovery. After a sweep or a thrust it first plays the blow itself, which the server's jump straight to this move hides: a snap
	 * from the wound-up pose to contact with an overshoot, a held beat on contact, then a stagger as the weight carries on, the head, cloth
	 * and hem each a little behind the body. After any other move it is the same stagger and settle without the snap.
	 */
	private void recover(float e,int from){
		boolean sweep=from==SwordTombRules.SWEEP,thrust=from==SwordTombRules.THRUST,blow=sweep||thrust;
		float hold=blow?3.5F:0,total=SwordTombRules.RECOVER_TICKS;
		float d=smooth(e,hold,total),keep=1-d,stag=smooth(e,hold,hold+4)*keep;
		float snap=blow?smooth(e,0,1.5F):1,over=blow?Mth.sin(Mth.PI*Mth.clamp(e/hold,0,1)):0;
		if(sweep){
			right.xRot=Mth.lerp(snap,-1.55F,-.85F+.25F*keep);
			right.yRot=Mth.lerp(snap,-1.45F,1.2F*keep)+.3F*over;
			body.yRot=Mth.lerp(snap,-.3F,.5F*keep)+.25F*over;body.xRot=.18F*snap*keep;
			left.zRot-=.5F*snap*keep;head.yRot+=(1-smooth(e,1,5))*.4F;body.y+=(1-snap)*1F;
		}else if(thrust){
			right.xRot=Mth.lerp(d,-1.55F,-.85F);
			right.z=(Mth.lerp(snap,-1.6F,-5F)-1.8F*over)*keep;right.yRot=-.18F*keep;
			body.xRot=Mth.lerp(snap,-.15F,.3F)*keep;body.y+=1.5F*snap*keep;
			legR.xRot=-.55F*snap*keep;legL.xRot=.45F*snap*keep;left.xRot=Mth.lerp(snap,-1.1F,Mth.lerp(d,.15F,-.2F));
			head.xRot+=.15F*snap*keep;
		}else{
			right.xRot=Mth.lerp(smooth(e,total-6,total),-1.3F+keep*.7F,-.85F);right.yRot=keep*1.2F;body.xRot=keep*.18F;
		}
		// The stagger: weight carried on past the blow, a foot catching it, the head hanging.
		legL.xRot+=.45F*stag;legR.xRot-=.35F*stag;body.zRot+=Mth.sin(e*.8F)*.07F*stag;body.y+=1.2F*stag;head.xRot+=.25F*stag;
		// The cloth throws forward with the blow, the hem swinging a beat after it.
		float kick=(float)Math.exp(-e*.15F)*Mth.sin(e*.7F),kick2=e>2?(float)Math.exp(-e*.13F)*Mth.sin((e-2)*.7F):0;
		cloth.xRot+=(blow?.6F:.25F)*kick;hem.xRot+=(blow?.7F:.3F)*kick2;crest.zRot+=.3F*kick;
		blindfold.zRot+=.06F*kick2;
	}

	/** A blow it takes: it flinches back, its head snapping up and its arms jerking, the cloth and crest flung. */
	private void hurt(GravekeeperRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		body.xRot-=.2F*k;head.xRot-=.3F*k;body.zRot+=Mth.sin(s.hurt*18)*.06F*k;
		right.xRot+=.25F*k;left.zRot-=.2F*k;cloth.xRot+=.5F*k;hem.xRot+=.4F*k*(1-s.hurt*.3F);crest.zRot+=.4F*k;
	}

	/**
	 * Dying: it staggers and jerks its head back, folds to its knees, topples forward onto its face with the blade arm trailing, bounces
	 * a little on the stone and lies still.
	 */
	private void die(GravekeeperRenderState s){
		if(s.deathTime<=0)return;
		float d=s.deathTime;
		float stagger=smooth(d,0,5),fold=smooth(d,4,11),fall=smooth(d,10,18),settle=d>17.5F?(float)Math.exp(-(d-17.5F)*.45F)*Mth.sin((d-17.5F)*1.8F):0;
		body.zRot=Mth.sin(d*1.1F)*.12F*stagger*(1-fall);body.yRot*=1-stagger;
		head.xRot=Mth.lerp(stagger,head.xRot,-.35F);head.xRot=Mth.lerp(fold,head.xRot,.5F);head.xRot=Mth.lerp(fall,head.xRot,-.45F);
		head.yRot*=1-fold;head.zRot=Mth.sin(d*.9F)*.1F*(1-fold);
		// The knees go: the legs swing forward under it and the body drops to meet them.
		legL.xRot=Mth.lerp(fold,legL.xRot,-1.25F);legR.xRot=Mth.lerp(fold,legR.xRot,-1.1F);
		legL.xRot=Mth.lerp(fall,legL.xRot,0);legR.xRot=Mth.lerp(fall,legR.xRot,0);
		body.xRot=Mth.lerp(fold,body.xRot,.35F);body.xRot=Mth.lerp(fall,body.xRot,1.45F)-.08F*settle;
		body.y=Mth.lerp(fold,body.y,body.y+6)+9.5F*fall+(settle>0?-.6F*Math.abs(settle)*1F:0);
		// The arms fall slack and trail; the blade drags behind, the cloth falls over its face.
		right.xRot=Mth.lerp(fold,right.xRot,.3F);left.xRot=Mth.lerp(fold,left.xRot,.3F);right.yRot*=1-fold;left.yRot*=1-fold;
		right.zRot=Mth.lerp(fold,right.zRot,.25F);left.zRot=Mth.lerp(fold,left.zRot,-.25F);blade.xRot=Mth.lerp(fold,blade.xRot,.2F);blade.zRot*=1-fold;
		cloth.xRot=Mth.lerp(fall,cloth.xRot,1.35F);hem.xRot=Mth.lerp(fall,hem.xRot,.5F)+.3F*settle;crest.zRot*=1-fold;
	}
}
