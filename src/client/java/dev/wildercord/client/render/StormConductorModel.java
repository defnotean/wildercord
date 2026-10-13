package dev.wildercord.client.render;

import dev.wildercord.cast.StormConductorRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Copper gyroscope with six articulated lightning terminals, a levitating blue coil, and the baton it conducts the storm with. */
public final class StormConductorModel extends EntityModel<DungeonBossRenderState> {
	private final ModelPart body, core, cage, baton, tip;
	private final ModelPart[] rods = new ModelPart[6];
	public StormConductorModel(ModelPart root) {
		super(root); body=root.getChild("body"); core=body.getChild("core"); cage=body.getChild("cage"); baton=body.getChild("baton"); tip=baton.getChild("tip");
		for(int i=0;i<6;i++) rods[i]=cage.getChild("rod"+i);
	}
	public static LayerDefinition createLayer() {
		var mesh=new MeshDefinition();
		var body=mesh.getRoot().addOrReplaceChild("body",CubeListBuilder.create(),PartPose.offset(0,4,0));
		body.addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),PartPose.ZERO);
		var cage=body.addOrReplaceChild("cage",CubeListBuilder.create().texOffs(32,0).addBox(-5,-5,-5,10,10,10),PartPose.ZERO);
		for(int i=0;i<6;i++) {
			float angle=i*Mth.TWO_PI/6;
			var arm=cage.addOrReplaceChild("rod"+i,CubeListBuilder.create().texOffs(0,24).addBox(-1,-1,0,2,2,18),PartPose.offsetAndRotation(0,0,0,0,angle,0));
			arm.addOrReplaceChild("terminal",CubeListBuilder.create().texOffs(48,24).addBox(-1,-6,-1,2,12,2),PartPose.offset(0,0,17));
			arm.addOrReplaceChild("cap",CubeListBuilder.create().texOffs(64,24).addBox(-3,-2,-3,6,4,6),PartPose.offset(0,-7,17));
		}
		// Its baton, rising out of the top of its cage, with a knot of charge at the tip.
		var baton=body.addOrReplaceChild("baton",CubeListBuilder.create().texOffs(48,24).addBox(-1,-12,-1,2,12,2),PartPose.offset(0,-5,0));
		baton.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(64,24).addBox(-1.5F,-3,-1.5F,3,3,3),PartPose.offset(0,-12,0));
		return LayerDefinition.create(mesh,128,64);
	}
	@Override public void setupAnim(DungeonBossRenderState s) {
		super.setupAnim(s);
		float t=s.ageInTicks, sh=s.shifting, beat=s.extra[0]-.5F;
		body.y=4+Mth.sin(t*.07F)*1.2F+s.exposed*4;
		core.yRot=t*.035F; core.xRot=t*.016F;
		cage.yRot=t*(.008F+.025F*s.casting); cage.zRot=Mth.sin(t*.025F)*.14F+sh*.5F;
		for(int i=0;i<6;i++) rods[i].xRot=(i%2==0?1:-1)*(.15F+s.casting*.45F-s.exposed*.3F);
		// Conducting even at rest: a slow figure in the air, flicking over on every beat, raised toward its prey as it casts.
		baton.xRot=.35F+Mth.sin(t*.24F)*.1F+s.casting*.5F;
		baton.zRot=Mth.sin(t*.12F)*.3F+beat*.3F;
		tip.yRot=t*.1F;
		// Grounded, its baton droops.
		baton.xRot=Mth.lerp(s.exposed*.6F,baton.xRot,1.6F);
		// Its own moves, each eased in over the rest of its pose (see StormConductorRules for what each one is).
		for(int m=1;m<=StormConductorRules.MOVES;m++) {
			float w=Math.min(1,s.moves[m]);
			if(w>.01F) { boolean current=m==s.move; move(s,m,w,current?s.since:999,current?s.tell:0,t,beat); }
		}
		// Between phases its baton is raised straight up and trembles while the storm gathers over it, its cage spinning up.
		baton.xRot=Mth.lerp(sh,baton.xRot,-.15F+Mth.sin(t*1.7F)*.05F);
		baton.zRot=Mth.lerp(sh,baton.zRot,Mth.sin(t*2.3F)*.06F);
		cage.yRot+=sh*t*.2F;
		body.y-=3*sh;
		for(int i=0;i<6;i++) rods[i].xRot+=(i%2==0?1:-1)*sh*.5F;
		if(s.deathTime>0) { body.y+=s.deathTime*.18F; cage.xRot=s.deathTime*.025F; baton.xRot+=s.deathTime*.03F; }
	}

	/** Its pose for its own move {@code m}, eased in by {@code w}: {@code since} ticks in (past its end once easing out) with a tell of {@code tell}. */
	private void move(DungeonBossRenderState s, int m, float w, float since, float tell, float t, float beat) {
		float wind=phase(since,tell), k=phase(since-tell,3), hit=decay(since-tell,6);
		boolean thrown=since>=tell;
		switch(m) {
			case StormConductorRules.DOWNBEAT -> {
				// Raised high and back as the marks follow, then brought down hard; in its third phase raised again for a second beat.
				int again=StormConductorRules.DOWN_AGAIN;
				float x=thrown?Mth.lerp(k,-.9F,1.6F):Mth.lerp(wind,.35F,-.9F);
				if(s.phase>=3&&since>=tell+3) x=since<tell+again?Mth.lerp(phase(since-tell-3,again-3),1.6F,-.9F):Mth.lerp(phase(since-tell-again,3),-.9F,1.6F);
				pose(w,x,0);
				body.y+=(-2*wind+3*hit)*w;
			}
			case StormConductorRules.CRESCENDO -> {
				// Circling over its head faster and faster; then swept down as the ring rolls out.
				float a=since*since*.03F;
				pose(w,thrown?Mth.lerp(k,-.2F,1.3F):-.2F+Mth.cos(a)*.45F*wind,thrown?0:Mth.sin(a)*.45F*wind);
				cage.yRot+=since*.05F*w;
			}
			case StormConductorRules.STACCATO -> {
				// Pointed down its line, flicking over on every spark.
				pose(w,Mth.lerp(wind,.35F,.85F)+beat*.6F,beat*.5F);
				body.x+=beat*.6F*w;
			}
			case StormConductorRules.FERMATA -> {
				// Held straight up and dead still while the note is held, its cage slowed, risen on its podium.
				pose(w,-.25F+(thrown?Mth.sin(t*3)*.02F:0),0);
				body.y-=2*wind*w;
				for(int i=0;i<6;i++) rods[i].xRot=Mth.lerp(w,rods[i].xRot,0);
			}
			case StormConductorRules.GALE -> {
				// Swept back to one side while the air is drawn in, its whole body twisting; then swung across.
				float z=thrown?Mth.lerp(k,1.2F,-1.3F):1.2F*wind;
				pose(w,thrown?.9F:.4F,z);
				body.yRot+=(thrown?Mth.lerp(k,.5F,-.6F):.5F*wind)*w;
			}
			case StormConductorRules.DISCHARGE -> {
				// Pointed down at its feet and trembling, its cage spinning up and its terminals drawn in; then all of it flung open.
				pose(w,2.2F+Mth.sin(t*2.1F)*.08F*wind,Mth.cos(t*1.7F)*.08F*wind);
				cage.yRot+=since*since*.004F*w;
				body.y+=(2*wind-3*hit)*w;
				for(int i=0;i<6;i++) rods[i].xRot+=(i%2==0?1:-1)*(thrown?.9F*hit:-.3F*wind)*w;
			}
			case StormConductorRules.BALL -> {
				// Held out in front, circling the ball as it swells at the tip; then held out after it as it drifts.
				float c=thrown?.05F:.15F*wind;
				pose(w,1.0F+Mth.sin(t*.5F)*c,Mth.cos(t*.5F)*c*1.5F);
			}
			case StormConductorRules.CHORD -> {
				// Pointed straight out at one quiet rod and then the other; raised and shaking as the chord runs.
				pose(w,thrown?-.4F+Mth.sin(t*2.5F)*.06F:1.25F,thrown?0:Mth.sin(t*.4F)*.05F);
			}
			case StormConductorRules.ROLL -> {
				// Drawn back and beating slowly while the walls rumble; then thrown forward on each roll of thunder.
				float pulse=thrown?decay((since-tell)%StormConductorRules.ROLL_GAP,5):0;
				pose(w,thrown?.5F+.9F*pulse:Mth.lerp(wind,.35F,-.7F),beat*.4F);
				body.y+=pulse*1.5F*w;
			}
			case StormConductorRules.COUNTERPOINT -> {
				// Beating wide from side to side as the squares are marked out, jabbing down as each half is struck.
				float jab=decay(since-tell,4)+decay(since-tell-StormConductorRules.COUNTER_AGAIN,4);
				pose(w,.6F+jab*.9F,beat*1.4F);
			}
			case StormConductorRules.WINDED -> {
				// Limp to one side, sagging on its podium and heaving.
				pose(w,1.9F,.6F);
				body.y+=(3+Mth.sin(t*.2F)*.5F)*w;
				for(int i=0;i<6;i++) rods[i].xRot=Mth.lerp(w,rods[i].xRot,(i%2==0?1:-1)*-.2F);
			}
			case StormConductorRules.SPENT -> {
				// Hanging straight down, its cage tipped over and its core all but out.
				pose(w,2.5F,-.3F);
				body.y+=4*w;
				cage.xRot+=.4F*w;
				core.yRot*=1-.8F*w;
			}
			default -> { }
		}
	}

	/** Its baton eased toward {@code x} (forward) and {@code z} (to the side) by {@code w}. */
	private void pose(float w, float x, float z) { baton.xRot=Mth.lerp(w,baton.xRot,x); baton.zRot=Mth.lerp(w,baton.zRot,z); }

	/** How far through {@code length} ticks {@code s} is, eased, 0 to 1 (1 if there's no length to it). */
	private static float phase(float s, float length) {
		if(length<=0) return 1;
		float x=Mth.clamp(s/length,0,1);
		return x*x*(3-2*x);
	}

	/** 1 the moment {@code s} reaches 0, falling to nothing over {@code length} ticks (0 before). */
	private static float decay(float s, float length) { return s<0?0:Math.max(0,1-s/length); }
}
