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
 * hands; and still, head cocked and a hand to the blindfold, while it listens.
 */
public final class GravekeeperModel extends EntityModel<GravekeeperRenderState> {
	private final ModelPart body,head,right,left,legR,legL,cloth,blade;
	public GravekeeperModel(ModelPart root){super(root);body=root.getChild("body");head=body.getChild("head");right=body.getChild("right");left=body.getChild("left");legR=body.getChild("legR");legL=body.getChild("legL");cloth=body.getChild("cloth");blade=right.getChild("blade");}
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
		body.addOrReplaceChild("cloth",CubeListBuilder.create().texOffs(64,48).addBox(-5,0,-.2F,10,13,0),PartPose.offset(0,4,-4.4F));
		body.addOrReplaceChild("crest",CubeListBuilder.create().texOffs(40,0).addBox(-3,-3,-.5F,6,6,1),PartPose.offset(0,-6,-4.5F));
		return LayerDefinition.create(mesh,128,64);
	}
	@Override public void setupAnim(GravekeeperRenderState s){
		super.setupAnim(s);head.yRot=s.yRot*Mth.DEG_TO_RAD*.65F;head.xRot=.05F+s.xRot*Mth.DEG_TO_RAD*.4F;
		legL.xRot=Mth.cos(s.walkAnimationPos*.55F)*s.walkAnimationSpeed*.5F;legR.xRot=-legL.xRot;
		right.xRot=-.85F;right.zRot=.08F;left.xRot=-.2F;left.zRot=-.08F;
		cloth.xRot=.08F+Math.abs(legL.xRot)*.45F+Mth.sin(s.ageInTicks*.05F)*.025F;
		float wind=Mth.clamp(s.elapsed/Math.max(1,s.tell),0,1),e=s.elapsed;
		if(s.move==SwordTombRules.SWEEP){right.xRot=-1.2F-wind*.35F;right.yRot=-.9F-wind*.55F;body.yRot=-wind*.3F;left.xRot=-.45F;}
		else if(s.move==SwordTombRules.THRUST){right.xRot=-1.55F;right.yRot=-.18F;right.z=-wind*1.6F;body.xRot=-wind*.15F;left.xRot=-1.1F;}
		else if(s.move==SwordTombRules.GUARD){right.xRot=-1.3F;right.yRot=-.8F;right.zRot=-.6F;left.xRot=-1.35F;left.yRot=.55F;blade.zRot=.8F;}
		else if(s.move==SwordTombRules.RECOVER){float swing=1-Mth.clamp(s.elapsed/SwordTombRules.RECOVER_TICKS,0,1);right.xRot=-1.3F+swing*.7F;right.yRot=swing*1.2F;body.xRot=swing*.18F;}
		else if(s.move==SwordTombRules.BROKEN){body.xRot=-.3F;head.xRot=-.4F;right.xRot=.2F;left.zRot=-.8F;body.zRot=Mth.sin(s.elapsed*.35F)*.045F;}
		else if(s.move==SwordTombRules.LISTEN){
			// Stock still, the blade point lowered, head turning this way and that, a hand raised to the blindfold.
			float w=Mth.clamp(e/SwordTombRules.LISTEN_TELL,0,1);
			head.yRot=Mth.sin(e*.25F)*.6F*w;head.zRot=Mth.sin(e*.13F)*.25F*w;head.xRot=.05F+.15F*w;
			left.xRot=-2.4F*w;left.zRot=.35F*w;right.xRot=-.85F+.5F*w;blade.xRot=.4F*w;body.xRot=.1F*w;
		}
		else if(s.move==SwordTombRules.LOST){head.xRot=.5F;head.yRot=Mth.sin(e*.08F)*.3F;right.xRot=-.2F;left.xRot=0;body.xRot=.08F;}
		else if(s.move==SwordTombRules.VERDICT){
			int crouch=SwordTombRules.VERDICT_CROUCH,leap=SwordTombRules.VERDICT_LEAP;
			if(e<crouch+leap-1){
				// Crouched with the blade raised two-handed overhead; in the air, knees tucked.
				float w=Mth.clamp(e/crouch,0,1),air=e<crouch?0:1;
				right.xRot=-.85F-2.1F*w;left.xRot=-2.9F*w;left.yRot=-.3F*w;right.yRot=.3F*w;
				body.y+=3*w*(1-air);body.xRot=-.15F*air;
				legR.xRot=-.6F*w-.3F*air;legL.xRot=.4F*w-.7F*air;
			}else{
				// The blade driven into the ground before it, bent over the hilt and tugging at it.
				float t=e-(crouch+leap-1),tug=Mth.sin(t*1.3F)*.05F;
				right.xRot=-1F;left.xRot=-1F;left.yRot=-.5F;right.yRot=.5F;body.xRot=.45F+tug;body.zRot=tug;head.xRot=.3F;
				legR.xRot=-.4F;legL.xRot=.3F;
			}
		}
		else if(s.move==SwordTombRules.HEEL){
			int tell=SwordTombRules.HEEL_TELL;
			if(e<=tell){
				// Wound up the wrong way, the blade trailed low behind it.
				float w=Mth.clamp(e/tell,0,1);
				right.xRot=.9F*w;right.zRot=.08F+.3F*w;body.yRot=-.5F*w;body.xRot=.2F*w;legR.xRot=-.3F*w;legL.xRot=.3F*w;left.xRot=-.6F*w;
			}else{
				// Then one full turn, the blade held out low.
				float t=Mth.clamp((e-tell)/SwordTombRules.HEEL_SPIN,0,1);
				body.yRot=-.5F*(1-t)+t*Mth.TWO_PI;right.xRot=.4F;right.zRot=1.2F;body.xRot=.15F;left.zRot=-.6F;legR.xRot=-.3F;legL.xRot=.3F;
			}
		}
		else if(s.move==SwordTombRules.HANDS){
			// The blade raised and turned point down, then plunged; kneeling over it, head bowed.
			int tell=SwordTombRules.HANDS_TELL;float w=Mth.clamp(e/(tell-2),0,1),k=Mth.clamp((e-tell+2)/3,0,1);
			right.xRot=Mth.lerp(k,-.85F-2.2F*w,-.9F);left.xRot=Mth.lerp(k,-3F*w,-.9F);right.yRot=.4F*k;left.yRot=-.4F*k;
			blade.xRot=Mth.lerp(k,Mth.PI*w,.9F);
			body.y+=5*k;legR.xRot=-1.4F*k;legL.xRot=.5F*k;head.xRot=.05F+.45F*k;body.zRot=Mth.sin(e*.4F)*.02F*k;
		}
		else if(s.move==SwordTombRules.RAMPART){
			// The blade held crosswise before its chest, leaning in, the back foot planted; then the charge.
			int tell=SwordTombRules.RAMPART_TELL;float w=Mth.clamp(e/tell,0,1);
			right.xRot=-.85F-.6F*w;left.xRot=-1.45F*w;right.yRot=.35F*w;left.yRot=-.35F*w;blade.zRot=1.57F*w;body.xRot=.25F*w;head.xRot=-.15F*w;
			if(e<tell){legL.xRot=.5F*w;legR.xRot=-.3F*w;body.y+=Mth.abs(Mth.sin(e*Mth.PI/6))*.6F*w;}
			else{legR.xRot=Mth.sin(e*1.2F)*.9F;legL.xRot=-legR.xRot;body.xRot=.35F;}
		}
		else if(s.move==SwordTombRules.WALLED){
			// Run into the wall: arms out stiff, the blade still crosswise, shaking.
			float shake=Mth.sin(e*2.1F)*.06F*(1-Mth.clamp(e/SwordTombRules.WALLED_TICKS,0,1));
			right.xRot=-1.57F;left.xRot=-1.57F;right.yRot=.35F;left.yRot=-.35F;blade.zRot=1.57F;body.xRot=-.15F;head.xRot=-.3F;body.zRot=shake;head.zRot=shake*2;
		}
		else if(s.move==SwordTombRules.RITE){
			// Kneeling, both hands on the pommel of the planted blade, head bowed; then rising, arms thrown wide.
			float w=Mth.clamp(e/8,0,1),r=Mth.clamp((e-(SwordTombRules.RITE_TICKS-6))/6F,0,1);
			right.xRot=Mth.lerp(r,-.9F*w,-.2F);left.xRot=Mth.lerp(r,-.9F*w,-.2F);right.yRot=.4F*w*(1-r);left.yRot=-.4F*w*(1-r);
			right.zRot=.08F+1.3F*r;left.zRot=-.08F-1.3F*r;blade.xRot=.9F*w*(1-r);
			body.y+=4*w*(1-r);legR.xRot=-1.4F*w*(1-r);legL.xRot=.5F*w*(1-r);
			head.xRot=Mth.lerp(r,.7F*w,-.4F);body.xRot=-.15F*r;
		}
		if(s.deathTime>0){body.xRot=Math.min(1,s.deathTime/20F)*1.2F;right.xRot=.3F;left.xRot=.3F;}
	}
}
