package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.LanternBearerRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A gaunt hooded spirit in a grave-dirty robe that falls to the ground in tatters, a ragged cape behind it and a mantle over its
 * shoulders; nothing in the hood but dark and two eyes. In its right hand a gravedigger's spade, in its left a long shepherd's crook with
 * a soul lantern hanging from its tip, always hanging straight down however the crook is swung. It glides rather than walks, its robe
 * trailing. The rise brings it up out of the grave, reaches the crook straight up to take the lantern off the gibbet and brings it down
 * swinging; each blow blends into the next, a blow jolts it, and dying it crumples down into the earth.
 */
public final class LanternBearerModel extends EntityModel<LanternBearerRenderState> {
	/** Where its waist sits, and its shoulders above that. */
	private static final float WAIST=8,SHOULDER=-10.5F,HAND=7.5F;
	private final ModelPart body,head,skirt,cape,armR,foreR,spade,armL,foreL,crook,lantern;
	private final ModelPart[] posed;
	/** The pose being built: the body's drop, lean, twist and sway; the head; each arm, its elbow, and the world pitch and slide of what it holds. */
	private float by,bX,bY,bZ,hX,hY,rX,rY,rZ,rE,spadeW,spadeSlide,lX,lY,lZ,lE,crookW,crookSlide,swing,swingZ,skirtX,capeX;
	public LanternBearerModel(ModelPart root){
		super(root,RenderTypes::entityTranslucent);
		body=root.getChild("body");head=body.getChild("head");skirt=body.getChild("skirt");cape=body.getChild("cape");
		armR=body.getChild("armR");foreR=armR.getChild("fore");spade=foreR.getChild("spade");
		armL=body.getChild("armL");foreL=armL.getChild("fore");crook=foreL.getChild("crook");lantern=crook.getChild("tip").getChild("lantern");
		posed=new ModelPart[]{body,head,skirt,cape,armR,foreR,spade,armL,foreL,crook,lantern};
	}
	public static LayerDefinition createLayer(){
		var mesh=new MeshDefinition();var root=mesh.getRoot();
		var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.5F,-12,-3,9,12,6)
			.texOffs(40,36).addBox(-5.5F,-12.5F,-3.5F,11,3,7),PartPose.offset(0,WAIST,0));
		// The robe from the waist to the ground, its hem torn into tatters all round.
		var skirt=body.addOrReplaceChild("skirt",CubeListBuilder.create().texOffs(0,20).addBox(-5,0,-4,10,15,8),PartPose.ZERO);
		int n=0;
		for(float x:new float[]{-3.5F,-.5F,2.5F})for(float z:new float[]{-4.01F,4.01F})
			skirt.addOrReplaceChild("tatter"+n++,CubeListBuilder.create().texOffs(0,46).addBox(-1,0,0,2,3,0),PartPose.offsetAndRotation(x+(z>0?1:0),15,z,z<0?-.12F:.12F,0,0));
		for(float x:new float[]{-5.01F,5.01F})for(float z:new float[]{-2,1.5F})
			skirt.addOrReplaceChild("tatter"+n++,CubeListBuilder.create().texOffs(8,46).addBox(0,0,-1,0,3,2),PartPose.offsetAndRotation(x,15,z,0,0,x<0?.12F:-.12F));
		body.addOrReplaceChild("cape",CubeListBuilder.create().texOffs(70,0).addBox(-5,0,0,10,21,0),PartPose.offset(0,-11.5F,3.2F));
		// The hood, its brim jutting over the dark where a face should be, and its peak falling behind.
		body.addOrReplaceChild("head",CubeListBuilder.create().texOffs(40,0).addBox(-3.5F,-7,-4,7,7,7)
			.texOffs(70,24).addBox(-4,-7.5F,-4.6F,8,2,1)
			.texOffs(92,24).addBox(-2,-8.5F,0,4,2,3),PartPose.offset(0,-12,0));
		for(int sign:new int[]{-1,1}){
			var arm=body.addOrReplaceChild(sign<0?"armR":"armL",CubeListBuilder.create().texOffs(40,20).addBox(-1.5F,-1,-1.5F,3,9,3),PartPose.offset(sign*5.5F,SHOULDER,0));
			var fore=arm.addOrReplaceChild("fore",CubeListBuilder.create().texOffs(56,20).addBox(-1.5F,0,-1.5F,3,6,3)
				.texOffs(56,30).addBox(-1,6,-1,2,3,2),PartPose.offset(0,8,0));
			if(sign<0){
				// The spade, held near the top of its haft, the blade down by its feet.
				fore.addOrReplaceChild("spade",CubeListBuilder.create().texOffs(100,0).addBox(-.5F,-14,-.5F,1,20,1)
					.texOffs(106,0).addBox(-2.5F,6,-.5F,5,6,1),PartPose.offset(0,HAND,0));
			}else{
				// The crook, its tip bent forward and the lantern hanging from it on a short chain.
				var crook=fore.addOrReplaceChild("crook",CubeListBuilder.create().texOffs(120,0).addBox(-.5F,-24,-.5F,1,28,1),PartPose.offset(0,HAND,0));
				var tip=crook.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(100,30).addBox(-.5F,-1,-5,1,1,5),PartPose.offset(0,-24,0));
				tip.addOrReplaceChild("lantern",CubeListBuilder.create().texOffs(116,40).addBox(-.5F,0,-.5F,1,2,1)
					.texOffs(100,40).addBox(-2,2,-2,4,5,4)
					.texOffs(100,52).addBox(-2.5F,1.5F,-2.5F,5,1,5)
					.texOffs(100,60).addBox(-1.5F,7,-1.5F,3,1,3),PartPose.offset(0,-.5F,-4.5F));
			}
		}
		return LayerDefinition.create(mesh,128,128);
	}

	@Override public void setupAnim(LanternBearerRenderState s){
		super.setupAnim(s);
		pose(s,s.move,s.elapsed,s.tell,s.side);
		// Blending out of the move before (or the same move begun again), so one blow flows into the next instead of snapping.
		if(s.blend<1){
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
		lantern.visible=LanternBearerRules.holdsLantern(s.rise);
	}

	/** 0 to 1 as {@code t} runs {@code from} to {@code to}, eased in and out. */
	private static float smooth(float t,float from,float to){float k=Mth.clamp((t-from)/(to-from),0,1);return k*k*(3-2*k);}

	/** The whole pose for {@code move}, {@code el} ticks in: standing with its spade and lantern, risen (or not yet), gliding, then the move, a blow's jolt and its death. */
	private void pose(LanternBearerRenderState s,int move,float el,int tell,int side){
		float t=s.ageInTicks,ws=Math.min(1,s.walkAnimationSpeed),wp=s.walkAnimationPos;
		stand();
		// Hovering a hair off the ground, breathing slow; its robe trailing as it glides, the cape lifting, the lantern swaying.
		by+=Mth.sin(t*.08F)*.5F-.6F;bX+=Mth.sin(t*.05F)*.02F+.15F*ws;
		skirtX+=.3F*ws+Mth.sin(t*.07F)*.03F;capeX+=.5F*ws+Mth.sin(t*.1F)*.05F;
		rX+=Mth.sin(wp*.6F)*.12F*ws;lX-=Mth.sin(wp*.6F)*.08F*ws;
		swing+=Mth.sin(t*.12F)*.08F+Mth.sin(wp*.6F)*.25F*ws;swingZ+=Mth.sin(t*.09F+1)*.06F;
		hY+=Mth.clamp(s.yRot,-60,60)*Mth.DEG_TO_RAD*.8F;hX+=s.xRot*Mth.DEG_TO_RAD*.6F;
		float T=Math.max(1,tell);
		switch(move){
			case LanternBearerRules.RISE,LanternBearerRules.BED,LanternBearerRules.DORMANT->rise(s.rise,t);
			case LanternBearerRules.REAP->low(el,T,s.roused);
			case LanternBearerRules.HOOK->hook(el,T);
			case LanternBearerRules.SNAGGED->snagged(el,t);
			case LanternBearerRules.WISPS->wisps(el,T,s.roused);
			case LanternBearerRules.GRAVES->graves(el,T);
			case LanternBearerRules.FLARE->flare(el,T);
			case LanternBearerRules.SHOVEL->shovel(el,T,s.roused);
			case LanternBearerRules.BACKSWEEP->backsweep(el,T);
			case LanternBearerRules.BREATH->breath(el,T,side,s.roused);
			case LanternBearerRules.RITE->rite(el,T,t);
			case LanternBearerRules.FADE->fade(el,side);
			case LanternBearerRules.RECOVER->recover(el,t);
			case LanternBearerRules.LIGHTS_OUT->lights(el);
			default->{}
		}
		hurt(s);
		die(s);
		apply();
	}
	/** Upright, the spade standing by its right foot and the crook held out a little before it, the lantern at the height of its hood. */
	private void stand(){
		by=0;bX=.08F;bY=0;bZ=0;hX=.1F;hY=0;
		rX=-.25F;rY=0;rZ=.1F;rE=-.35F;spadeW=-.08F;spadeSlide=0;
		lX=-.45F;lY=0;lZ=-.1F;lE=-.5F;crookW=0;crookSlide=0;swing=0;swingZ=0;
		skirtX=0;capeX=.12F;
	}
	private void apply(){
		body.y=WAIST+by;body.xRot=bX;body.yRot=bY;body.zRot=bZ;
		head.xRot=hX;head.yRot=hY;
		skirt.xRot=skirtX;cape.xRot=capeX;
		armR.xRot=rX;armR.yRot=rY;armR.zRot=rZ;foreR.xRot=rE;
		spade.xRot=spadeW-bX-rX-rE;spade.y=HAND+spadeSlide;
		armL.xRot=lX;armL.yRot=lY;armL.zRot=lZ;foreL.xRot=lE;
		crook.xRot=crookW-bX-lX-lE;crook.y=HAND+crookSlide;
		// Whatever the crook does, the lantern hangs straight down from its tip, swinging.
		lantern.xRot=-crookW+swing;lantern.zRot=-(lZ+bZ)+swingZ;
	}

	/**
	 * Up out of the grave and taking down the lantern, {@code r} of the way: it comes up out of the earth bowed over, reaches its crook
	 * straight up to the lantern on the gibbet over its head (where the lantern block is), brings it down swinging, and wails.
	 */
	private void rise(float r,float t){
		float up=smooth(r,0,.45F),reach=smooth(r,.38F,.6F),lower=smooth(r,.66F,.95F),wail=smooth(r,.76F,.84F)*(1-smooth(r,.92F,1));
		by+=(1-up)*38;bX+=(1-up)*.5F;hX+=(1-up)*.5F;
		rE-=(1-up)*.6F;rX-=(1-up)*.3F;
		float held=reach*(1-lower);
		lX=Mth.lerp(held,lX,-2.95F);lE=Mth.lerp(held,lE,-.1F);lZ=Mth.lerp(held,lZ,-.15F);crookW=Mth.lerp(held,crookW,-.25F);crookSlide-=8*held;
		hX-=.7F*held;bX-=.12F*held;
		// Brought down, the lantern swings on its chain and settles.
		float settle=lower*(1-smooth(r,.9F,1));
		swing+=Mth.sin(t*.45F)*.6F*settle;swingZ+=Mth.sin(t*.3F)*.25F*settle;
		bX-=.18F*wail;hX-=.55F*wail;rZ+=.45F*wail;lZ-=.25F*wail;
	}
	/**
	 * The low reap: crouched with the spade drawn back past its right side, the blade scraping the ground, then reaped flat round in
	 * front of it at ankle height. Roused, it drags the spade on round behind its left side and reaps back the other way.
	 */
	private void low(float el,float T,boolean twice){
		float b0=T+LanternBearerRules.REAP_SWEEP+LanternBearerRules.REAP_BACK,done=twice?b0+1:T+1;
		float wind=smooth(el,0,T),cut=smooth(el,T-1,T+3),back=twice?smooth(el,b0-1,b0+3):0,
			off=1-smooth(el,done+4,done+LanternBearerRules.REAP_RECOVER);
		float on=wind*off;
		by+=2.4F*on;bX+=.3F*on;hX-=.25F*on;
		rX=Mth.lerp(on,rX,-1.2F);rE=Mth.lerp(on,rE,-.05F);rZ=Mth.lerp(on,rZ,.2F);spadeW=Mth.lerp(on,spadeW,-1.4F);spadeSlide+=9*on;
		bY+=(.85F*wind-1.95F*cut+1.95F*back)*off;rY+=(.5F*wind-1.0F*cut+1.0F*back)*off;
		hY-=(.6F*wind-1.2F*cut+1.2F*back)*off;
		lX+=.3F*on;lZ-=.2F*on;swing+=(.5F*cut-.7F*back)*off;swingZ+=.3F*(cut-back)*off;
		skirtX+=.2F*on;capeX+=.3F*(cut+back)*off;
	}
	/** The upright reap of Lights Out: the spade swung back round past its right side with its arm out straight, then reaped flat round in front of it. */
	private void reap(float el,float T){
		float wind=smooth(el,0,T),cut=smooth(el,T-1,T+3),back=1-smooth(el,T+5,T+LanternBearerRules.REAP_RECOVER);
		float on=wind*back;
		rX=Mth.lerp(on,rX,-1.15F);rE=Mth.lerp(on,rE,-.1F);rZ=Mth.lerp(on,rZ,.15F);spadeW=Mth.lerp(on,spadeW,-1.25F);spadeSlide+=8*on;
		bY+=(.8F*wind-1.9F*cut)*back;rY+=(.5F*wind-1.0F*cut)*back;
		bX+=(.15F*cut-.05F*wind)*back;hY-=(.6F*wind-1.2F*cut)*back;
		lX+=.3F*on;swing+=.5F*cut*back;
	}
	/** The hook: the crook drawn back level, thrust out at whoever keeps away, and hauled in with them on it. */
	private void hook(float el,float T){
		float draw=smooth(el,0,T),thrust=smooth(el,T-1,T+1.5F),haul=smooth(el,T+2,T+2+LanternBearerRules.HOOK_DRAG),
			back=1-smooth(el,T+3+LanternBearerRules.HOOK_DRAG,T+LanternBearerRules.HOOK_DRAG+LanternBearerRules.HOOK_RECOVER);
		float on=draw*back;
		lX=Mth.lerp(on,lX,Mth.lerp(thrust,-1.0F,-1.55F)+.45F*haul);
		lE=Mth.lerp(on,lE,Mth.lerp(thrust,-1.2F,-.05F)-.95F*haul);
		crookW=Mth.lerp(on,crookW,1.5F);
		crookSlide+=(Mth.lerp(thrust,-4,8)-10*haul)*on;
		bY+=(Mth.lerp(thrust,-.5F,.4F)-.6F*haul)*on;bX+=(Mth.lerp(thrust,-.1F,.2F)-.3F*haul)*on;
		swing+=Mth.sin(el*.6F)*.4F*thrust*back;
	}
	/** Snagged: the crook's tip caught fast in the earth before it, it leans back on the haft and tugs, its spade arm flung out for balance. */
	private void snagged(float el,float t){
		float k=smooth(el,0,3)*(1-smooth(el,LanternBearerRules.SNAG_TICKS-4,LanternBearerRules.SNAG_TICKS)),tug=Math.max(0,Mth.sin(el*.7F));
		lX=Mth.lerp(k,lX,-1.25F+.25F*tug);lE=Mth.lerp(k,lE,-.05F);crookW=Mth.lerp(k,crookW,1.8F);crookSlide+=6*k;
		bX+=(-.2F-.18F*tug)*k;by-=.4F*tug*k;bY+=(.3F+.12F*tug)*k;hX+=(.2F+.1F*tug)*k;
		rX=Mth.lerp(k,rX,-.5F);rZ+=(.35F+.2F*tug)*k;
		capeX+=.2F*tug*k;swing+=Mth.sin(t*.8F)*.3F*k;swingZ+=Mth.sin(t*.6F)*.2F*k;
	}
	/**
	 * The wisps: the lantern raised over its hood and whirled round, faster as the tell runs on; then, as each wisp is loosed, flicked out
	 * toward its prey and whirled on.
	 */
	private void wisps(float el,float T,boolean roused){
		int n=roused?LanternBearerRules.WISP_COUNT_ROUSED:LanternBearerRules.WISP_COUNT;
		float end=T+(n-1)*LanternBearerRules.WISP_EVERY+4;
		float raise=smooth(el,0,T*.5F),on=raise*(1-smooth(el,end,end+6));
		float a=el*(.35F+.3F*Math.min(1,el/T));
		float flick=el<T-1||el>=end?0:1-((el-T+1)%LanternBearerRules.WISP_EVERY)/LanternBearerRules.WISP_EVERY;
		flick*=flick;
		lX=Mth.lerp(on,lX,-2.65F+.25F*Mth.cos(a)+.6F*flick);lE=Mth.lerp(on,lE,-.15F);lZ=Mth.lerp(on,lZ,-.2F+.3F*Mth.sin(a));
		crookW=Mth.lerp(on,crookW,.15F+.3F*Mth.cos(a)+.6F*flick);crookSlide-=5*on;
		swing+=Mth.sin(a)*.9F*on;swingZ+=Mth.cos(a)*.7F*on;
		bX+=(-.12F+.2F*flick)*on;hX+=(-.4F+.25F*flick)*on;bY+=.12F*Mth.sin(a)*on;
		rX+=.15F*on;rE+=.1F*on;capeX+=.15F*on;
	}
	/**
	 * The graves: the spade raised straight up over its hood, blade down, then driven deep into the earth before it, where it sticks; it
	 * hangs on the haft straining to wrench it out while the graves are marked, and only then pulls it free.
	 */
	private void graves(float el,float T){
		float stuck=T+LanternBearerRules.GRAVE_MARK+LanternBearerRules.GRAVE_HOLD;
		float lift=smooth(el,0,T*.8F),drive=smooth(el,T-1,T+1),pull=smooth(el,stuck-3,stuck+3);
		float on=lift*(1-pull),strain=drive*(1-pull)*Math.max(0,Mth.sin(el*.5F));
		rX=Mth.lerp(on,rX,Mth.lerp(drive,-2.9F,-.85F));rE=Mth.lerp(on,rE,Mth.lerp(drive,-.2F,-.15F));rZ=Mth.lerp(on,rZ,Mth.lerp(drive,-.15F,0));
		rY+=.25F*on*(1-drive);
		spadeW=Mth.lerp(on,spadeW,Mth.lerp(drive,.3F,-.1F));spadeSlide+=Mth.lerp(drive,-6,7)*on;
		bX+=(Mth.lerp(drive,-.25F,.45F)-.12F*strain)*on;hX+=(Mth.lerp(drive,-.3F,.35F)-.1F*strain)*on;by+=Mth.lerp(drive,-.5F,2.2F)*on;
		lX+=Mth.lerp(drive,-.35F,.25F)*on;lZ-=.2F*on;swing+=.5F*drive*Mth.cos(el*.4F)*on;
	}
	/**
	 * The flare: the lantern drawn in close under its hood, hunched round it with its spade arm folded over as the souls pour in; then
	 * thrown wide, both arms out and its hood flung back, as it bursts.
	 */
	private void flare(float el,float T){
		float hug=smooth(el,0,T*.7F),burst=smooth(el,T-1,T+1),off=1-smooth(el,T+3,T+3+LanternBearerRules.FLARE_RECOVER*.5F);
		float in=hug*(1-burst)*off,out=burst*off;
		lX=Mth.lerp(in,lX,-1.6F);lE=Mth.lerp(in,lE,-1.3F);lZ=Mth.lerp(in,lZ,.45F);crookW=Mth.lerp(in,crookW,.1F);crookSlide-=9*in;
		rX=Mth.lerp(in,rX,-.9F);rZ=Mth.lerp(in,rZ,-.35F);rE=Mth.lerp(in,rE,-.9F);
		bX+=.35F*in;hX+=.5F*in;by+=1.2F*in;
		swing*=1-in;swingZ*=1-in;
		lX=Mth.lerp(out,lX,-1.7F);lZ=Mth.lerp(out,lZ,-1.1F);lE=Mth.lerp(out,lE,-.1F);crookW=Mth.lerp(out,crookW,.4F);
		rX=Mth.lerp(out,rX,-1.2F);rZ=Mth.lerp(out,rZ,1.1F);rE=Mth.lerp(out,rE,-.1F);spadeW=Mth.lerp(out,spadeW,.2F);
		bX-=.3F*out;hX-=.55F*out;capeX+=.9F*out;skirtX+=.3F*out;swing+=.8F*out;
	}
	/**
	 * The shovel: bent over the spade dug in at its right foot, levering the earth up; then heaved up and over its shoulder, flinging the
	 * earth high. Roused, it digs in again and flings a second time.
	 */
	private void shovel(float el,float T,boolean twice){
		float A=LanternBearerRules.SHOVEL_AGAIN,end=T+(twice?A:0);
		float on=smooth(el,0,4)*(1-smooth(el,end+3,end+10));
		float fling=smooth(el,T-1,T+1.5F),fling2=twice?smooth(el,T+A-1,T+A+1.5F):0,re=twice?smooth(el,T+2,T+5):0;
		float up=fling*(1-re)+fling2,lever=Mth.sin(el*.9F)*(1-up);
		rX=Mth.lerp(on,rX,Mth.lerp(up,-.35F+.15F*lever,-2.4F));rE=Mth.lerp(on,rE,Mth.lerp(up,-.4F,-.2F));rZ=Mth.lerp(on,rZ,Mth.lerp(up,.35F,.1F));
		spadeW=Mth.lerp(on,spadeW,Mth.lerp(up,-.45F+.2F*lever,1.6F));spadeSlide+=Mth.lerp(up,4,1)*on;
		bX+=(Mth.lerp(up,.45F,-.2F)+.05F*lever)*on;bY+=Mth.lerp(up,.35F,-.25F)*on;by+=Mth.lerp(up,1.5F,0)*on;
		hX+=Mth.lerp(up,.3F,-.35F)*on;hY+=Mth.lerp(up,.3F,0)*on;
		lX+=.2F*on;lZ-=.15F*on;swing+=.4F*up*on;capeX+=.25F*up*on;
	}
	/**
	 * The backsweep: looking back over its shoulder with the crook drawn across its chest and its weight on its front, then whipped round
	 * low behind it.
	 */
	private void backsweep(float el,float T){
		float coil=smooth(el,0,T),whip=smooth(el,T-1,T+2),off=1-smooth(el,T+3,T+3+LanternBearerRules.BACK_RECOVER);
		float on=coil*off;
		hY+=Mth.lerp(whip,1.3F,-.4F)*on;hX+=.15F*on;
		bY+=(.7F*coil-1.9F*whip)*off;bX+=Mth.lerp(whip,.1F,.3F)*on;by+=.8F*whip*off;
		lX=Mth.lerp(on,lX,Mth.lerp(whip,-1.1F,.6F));lZ=Mth.lerp(on,lZ,Mth.lerp(whip,.7F,-1.2F));lE=Mth.lerp(on,lE,Mth.lerp(whip,-1.0F,-.1F));
		crookW=Mth.lerp(on,crookW,Mth.lerp(whip,.6F,1.4F));
		swing+=.7F*whip*off;swingZ-=.6F*whip*off;
		rX+=.25F*on;rZ+=.2F*on;capeX+=.4F*whip*off;skirtX+=.2F*whip*off;
	}
	/**
	 * The breath: the lantern raised before its hood and its head cocked toward where the breath will turn, drawing in; then breathed
	 * out ahead of it, leaning into it; then, spent, doubled over coughing smoke with the lantern hanging low.
	 */
	private void breath(float el,float T,int side,boolean roused){
		float len=LanternBearerRules.breathTicks(roused),dir=side==0?1:side;
		float raise=smooth(el,0,T*.6F),blow=smooth(el,T-2,T+1),cough=smooth(el,T+len,T+len+3),
			off=1-smooth(el,T+len+LanternBearerRules.BREATH_COUGH-2,T+len+LanternBearerRules.BREATH_COUGH+4);
		float held=raise*(1-cough)*off,draw=raise*(1-blow)*off;
		lX=Mth.lerp(held,lX,-1.55F);lE=Mth.lerp(held,lE,-1.05F);lZ=Mth.lerp(held,lZ,.3F);crookW=Mth.lerp(held,crookW,.25F);crookSlide-=7*held;
		swing*=1-held;swingZ*=1-held;
		hY+=dir*.4F*draw;hX-=.3F*draw;bX-=.15F*draw;
		bX+=.25F*blow*(1-cough)*off;hX+=.05F*blow*(1-cough)*off;
		float c=cough*off,hack=Math.max(0,Mth.sin(el*1.2F));
		bX+=(.45F+.12F*hack)*c;hX+=(.45F+.15F*hack)*c;by+=(1.2F+.4F*hack)*c;
		lX=Mth.lerp(c,lX,-.15F);lE=Mth.lerp(c,lE,-.1F);crookW=Mth.lerp(c,crookW,.6F);rX=Mth.lerp(c,rX,-.6F);
	}
	/**
	 * The rite: up straight off the ground with its crook raised high over its head and its spade arm thrown wide, shaking harder as the
	 * souls pour in; then it brings both down at once and the ring bursts out.
	 */
	private void rite(float el,float T,float t){
		float raise=smooth(el,0,T*.4F),burst=smooth(el,T-2,T+1),off=1-smooth(el,T+3,T+10);
		float on=raise*off,shake=Mth.sin(t*1.1F)*smooth(el,T*.5F,T-2)*(1-burst);
		lX=Mth.lerp(on,lX,Mth.lerp(burst,-2.95F,-.6F));lE=Mth.lerp(on,lE,-.1F);lZ=Mth.lerp(on,lZ,Mth.lerp(burst,-.35F,-.5F));
		crookW=Mth.lerp(on,crookW,Mth.lerp(burst,-.2F,.9F));crookSlide-=Mth.lerp(burst,8,0)*on;
		rX=Mth.lerp(on,rX,Mth.lerp(burst,-.6F,-.9F));rZ=Mth.lerp(on,rZ,Mth.lerp(burst,1.3F,.4F));rE=Mth.lerp(on,rE,-.1F);
		bX+=(Mth.lerp(burst,-.25F,.4F)+.03F*shake)*on;hX+=Mth.lerp(burst,-.7F,.4F)*on;by+=Mth.lerp(burst,-1.5F,2F)*on;bZ+=.04F*shake*on;
		capeX+=(.4F+.1F*shake)*on;skirtX+=.15F*on;
		swing+=Mth.sin(t*.6F)*.4F*on*(1-burst);swingZ+=shake*.3F*on;
	}
	/**
	 * Lights Out: it raises its lantern up under its hood and bows over it to breathe it out; then, each time it stands up out of the
	 * dark behind someone, it draws its spade back as it glimmers and reaps.
	 */
	private void lights(float el){
		if(el<LanternBearerRules.SNUFF_TELL+LanternBearerRules.LIGHTS_VANISH){
			float raise=smooth(el,0,LanternBearerRules.SNUFF_TELL*.6F),blow=smooth(el,LanternBearerRules.SNUFF_TELL-3,LanternBearerRules.SNUFF_TELL+1);
			lX=Mth.lerp(raise,lX,-1.75F);lE=Mth.lerp(raise,lE,-1.1F);lZ=Mth.lerp(raise,lZ,.25F);crookW=Mth.lerp(raise,crookW,.2F);crookSlide-=6*raise;
			hX+=.45F*raise+.15F*blow;bX+=.2F*raise;by+=.8F*blow;rZ+=.15F*raise;
			swing*=1-raise;swingZ*=1-raise;
			return;
		}
		float g=LanternBearerRules.lightsGlimmer(el);
		if(g>=0)reap(g,LanternBearerRules.LIGHTS_GLIMMER);
	}
	/** The fade: it leans away from the blow and drifts aside, its arms and cape trailing. */
	private void fade(float el,int side){
		float k=smooth(el,0,2)*(1-smooth(el,LanternBearerRules.FADE_TICKS-2,LanternBearerRules.FADE_TICKS));
		float dir=side==0?1:side;
		bZ+=dir*.35F*k;rZ+=.4F*k;lZ-=.4F*k;capeX+=.6F*k;skirtX+=.3F*k;swingZ-=dir*.5F*k;
	}
	/** Spent: leaning on its planted spade, its hood bowed and its shoulders heaving, the lantern hanging low before it. */
	private void recover(float el,float t){
		float k=smooth(el,0,4);
		bX=Mth.lerp(k,bX,.3F+Mth.sin(t*.4F)*.04F);hX=Mth.lerp(k,hX,.35F);by+=k;
		rX=Mth.lerp(k,rX,-.7F);rE=Mth.lerp(k,rE,-.1F);spadeW=Mth.lerp(k,spadeW,-.05F);
		lX=Mth.lerp(k,lX,-.1F);lE=Mth.lerp(k,lE,-.1F);crookW=Mth.lerp(k,crookW,.7F);
	}
	/** A blow jolts it back, its hood snapping up. */
	private void hurt(LanternBearerRenderState s){
		if(s.hurt<=0||s.deathTime>0)return;
		float k=s.hurt*s.hurt;
		bX-=.15F*k;hX-=.25F*k;bZ+=Mth.sin(s.hurt*18)*.05F*k;swing+=.3F*k;
	}
	/** Dying: it doubles over, its arms fall slack and its spade and crook tip out of its hands, and it sinks down into the earth. */
	private void die(LanternBearerRenderState s){
		if(s.deathTime<=0)return;
		float fold=smooth(s.deathTime,0,8),sink=smooth(s.deathTime,4,20);
		bX=Mth.lerp(fold,bX,.7F);bY*=1-fold;bZ=Mth.lerp(fold,bZ,.08F);hX=Mth.lerp(fold,hX,.6F);hY*=1-fold;
		rX=Mth.lerp(fold,rX,-.15F);rE=Mth.lerp(fold,rE,0);rZ=Mth.lerp(fold,rZ,.15F);
		lX=Mth.lerp(fold,lX,-.15F);lE=Mth.lerp(fold,lE,0);lZ=Mth.lerp(fold,lZ,-.15F);
		spadeW=Mth.lerp(sink,Mth.lerp(fold,spadeW,-.5F),-1.4F);crookW=Mth.lerp(sink,Mth.lerp(fold,crookW,.4F),1.4F);
		capeX=Mth.lerp(fold,capeX,.6F);by+=3*fold+22*sink;
	}
}
