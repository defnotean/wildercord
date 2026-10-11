package dev.wildercord.client.auraworld;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.RootcoilSerpent;
import dev.wildercord.aura.world.SerpentRules;
import dev.wildercord.aura.world.SerpentSpine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the Rootcoil Serpent as one long scaled tube along its spine, with a boxed head on the front joint. Coiled, uncoiling
 * and recoiling it follows {@link SerpentSpine}'s scripted paths round its trunk; awake, the reared neck is posed from its
 * facing and the rest of the body trails after it, lying along the ground (or floating on the water) and sliding off the
 * tree when nothing holds it up.
 */
public final class SerpentRenderer extends EntityRenderer<RootcoilSerpent,SerpentRenderState> {
	private static final Identifier SKIN=Wildercord.id("textures/entity/rootcoil_serpent/skin.png"),GLOW=Wildercord.id("textures/entity/rootcoil_serpent/glow.png");
	/** Texture size, the ring's sides, and how much wider than tall the body is. */
	static final float TEX_W=128,TEX_H=64;
	private static final int SIDES=8;
	private static final double WIDE=1.15,TALL=0.9;
	/** How many ticks of a recoil it takes to settle from where the body lay into the climb. */
	private static final float SETTLE=12.5F;
	/** How fast an unsupported joint drops, in blocks a tick. */
	private static final double FALL=0.5;

	/** Each serpent's trailing body, in world positions; kept between frames since the trail has memory. */
	private final Map<RootcoilSerpent,Trail> trails=new WeakHashMap<>();
	private static final class Trail{double[][] joints;double time=Double.NaN;}

	public SerpentRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=1.6F;}

	@Override public SerpentRenderState createRenderState(){return new SerpentRenderState();}
	@Override protected AABB getBoundingBoxForCulling(RootcoilSerpent serpent,float partial){
		return serpent.getBoundingBox().minmax(AABB.ofSize(serpent.trunk().add(0,4,0),8,10,8)).inflate(SerpentSpine.LENGTH*.6);
	}

	@Override public void extractRenderState(RootcoilSerpent serpent,SerpentRenderState state,float partial){
		super.extractRenderState(serpent,state,partial);
		Level level=serpent.level();
		int move=serpent.move();float elapsed=serpent.elapsed(partial);double time=level.getGameTime()+partial;
		Vec3 foot=serpent.trunk();double[] trunk={foot.x,foot.y,foot.z};float rest=serpent.rest();
		double[][] world;
		Trail trail=trails.get(serpent);
		if(SerpentRules.scripted(move)){
			double[][] local=move==SerpentRules.DORMANT?SerpentSpine.coiled():SerpentSpine.uncoil(SerpentRules.uncoil(move,elapsed),landing(serpent));
			world=place(local,rest,trunk);
			if(move==SerpentRules.RECOIL && trail!=null && trail.joints!=null && elapsed<SETTLE)
				world=SerpentSpine.blend(trail.joints,world,Mth.clamp(elapsed/SETTLE,0,1));
			else if(move==SerpentRules.DORMANT)trails.remove(serpent);
			else if(move==SerpentRules.UNCOIL){
				if(trail==null)trails.put(serpent,trail=new Trail());
				trail.joints=copy(world);trail.time=Double.NaN;
			}
		}else{
			if(trail==null)trails.put(serpent,trail=new Trail());
			if(trail.joints==null)trail.joints=place(SerpentSpine.uncoil(1,landing(serpent)),rest,trunk);
			world=fight(serpent,trail,partial,time);
		}
		double ox=state.x,oy=state.y,oz=state.z;
		state.joints=new float[world.length*3];state.lights=new int[world.length];
		var at=new BlockPos.MutableBlockPos();
		for(int i=0;i<world.length;i++){
			state.joints[i*3]=(float)(world[i][0]-ox);state.joints[i*3+1]=(float)(world[i][1]-oy);state.joints[i*3+2]=(float)(world[i][2]-oz);
			state.lights[i]=LightCoordsUtil.getLightCoords(level,at.set(world[i][0],world[i][1]+.3,world[i][2]));
		}
		if(SerpentRules.scripted(move)){
			double x=world[0][0]-world[1][0],y=world[0][1]-world[1][1],z=world[0][2]-world[1][2],l=Math.max(1e-6,Math.sqrt(x*x+y*y+z*z));
			state.headX=(float)(x/l);state.headY=(float)(y/l);state.headZ=(float)(z/l);
		}else{
			float yaw=(float)Math.toRadians(serpent.getYHeadRot()),pitch=(float)Math.toRadians(serpent.getXRot(partial));
			state.headX=-Mth.sin(yaw)*Mth.cos(pitch);state.headY=-Mth.sin(pitch);state.headZ=Mth.cos(yaw)*Mth.cos(pitch);
		}
		// The jaw parts as it wakes and hisses at the end of its uncoil, and hangs a little open while it hunts.
		state.jaw=move==SerpentRules.UNCOIL?Mth.clamp((elapsed-30)/8,0,1)*Mth.clamp((SerpentRules.UNCOIL_TICKS+4-elapsed)/6,0,1)
			:SerpentRules.scripted(move)?0:.12F;
		state.hurt=serpent.hurtTime>0 || serpent.deathTime>0;
		state.move=move;
	}

	/** The landing, in the trunk frame. */
	private static double[] landing(RootcoilSerpent serpent){
		var at=Vec3.atBottomCenterOf(serpent.landing()).subtract(serpent.trunk());
		return SerpentSpine.fromWorld(new double[]{at.x,at.y,at.z},serpent.rest());
	}

	private static double[][] place(double[][] local,float yaw,double[] origin){
		double[][] out=new double[local.length][];
		for(int i=0;i<local.length;i++){double[] w=SerpentSpine.toWorld(local[i],yaw);out[i]=new double[]{w[0]+origin[0],w[1]+origin[1],w[2]+origin[2]};}
		return out;
	}

	private static double[][] copy(double[][] joints){
		double[][] out=new double[joints.length][];
		for(int i=0;i<joints.length;i++)out[i]=joints[i].clone();
		return out;
	}

	/** The awake body: the reared neck posed from its facing, weaving as it moves, and the rest trailing after it. */
	private static double[][] fight(RootcoilSerpent serpent,Trail trail,float partial,double time){
		Level level=serpent.level();
		float yaw=Mth.rotLerp(partial,serpent.yBodyRotO,serpent.yBodyRot);
		double ex=Mth.lerp(partial,serpent.xo,serpent.getX()),ey=Mth.lerp(partial,serpent.yo,serpent.getY()),ez=Mth.lerp(partial,serpent.zo,serpent.getZ());
		double[][] neck=SerpentSpine.fightNeck();
		double weave=Math.sin(serpent.walkAnimation.position(partial)*.6)*.45*Math.min(1,serpent.walkAnimation.speed(partial)*3);
		double[][] joints=trail.joints;
		for(int i=0;i<neck.length;i++){
			double[] p=neck[i].clone();p[0]+=weave*i/(neck.length-1);
			double[] w=SerpentSpine.toWorld(p,yaw);joints[i]=new double[]{w[0]+ex,w[1]+ey,w[2]+ez};
		}
		double dt=Double.isNaN(trail.time)?0:Mth.clamp(time-trail.time,0,2);trail.time=time;
		for(int i=neck.length;i<joints.length;i++){
			double[] lead=joints[i-1],cur=joints[i];
			double dx=cur[0]-lead[0],dy=cur[1]-lead[1],dz=cur[2]-lead[2],l=Math.sqrt(dx*dx+dy*dy+dz*dz);
			if(l<1e-6){dx=0;dy=0;dz=-1;l=1;}
			double x=lead[0]+dx/l*SerpentSpine.SEG,y=lead[1]+dy/l*SerpentSpine.SEG,z=lead[2]+dz/l*SerpentSpine.SEG;
			double floor=ground(level,x,y,z),lie=SerpentSpine.radius(i)*TALL;
			if(Double.isNaN(floor))y-=FALL*dt;
			else y=y<floor+lie?floor+lie:Math.max(floor+lie,y-FALL*dt);
			joints[i]=new double[]{x,y,z};
		}
		return joints;
	}

	/** The top of whatever holds a joint up, from a block above it to three below: ground, or the water's surface. */
	private static double ground(Level level,double x,double y,double z){
		var at=new BlockPos.MutableBlockPos();
		for(int by=Mth.floor(y+1);by>=Mth.floor(y-3);by--){
			at.set(Mth.floor(x),by,Mth.floor(z));
			var fluid=level.getFluidState(at);
			if(fluid.is(FluidTags.WATER))return by+fluid.getHeight(level,at);
			var shape=level.getBlockState(at).getCollisionShape(level,at);
			if(!shape.isEmpty())return by+shape.max(Direction.Axis.Y);
		}
		return Double.NaN;
	}

	@Override public void submit(SerpentRenderState state,PoseStack poseStack,SubmitNodeCollector collector,CameraRenderState camera){
		super.submit(state,poseStack,collector,camera);
		if(state.isInvisible || state.joints.length<6)return;
		int overlay=OverlayTexture.pack(OverlayTexture.u(0),OverlayTexture.v(state.hurt));
		SerpentHead head=new SerpentHead(state);
		collector.order(0).submitCustomGeometry(poseStack,RenderTypes.entityCutout(SKIN),(pose,buffer)->{
			body(state,pose,buffer,overlay);
			head.skin(pose,buffer,overlay,state.lights[0]);
		});
		collector.order(1).submitCustomGeometry(poseStack,RenderTypes.eyes(GLOW),(pose,buffer)->head.eyes(pose,buffer,overlay));
	}

	/** The tube: a flattened ring round each joint, joined to the next. Columns of the band run round the ring, dorsal at a quarter. */
	private static void body(SerpentRenderState state,PoseStack.Pose pose,VertexConsumer buffer,int overlay){
		int n=state.joints.length/3;
		float[][][] rings=new float[n][SIDES+1][],normals=new float[n][SIDES+1][];
		for(int i=0;i<n;i++){
			int a=Math.max(0,i-1),b=Math.min(n-1,i+1);
			float[] t=norm(state.joints[a*3]-state.joints[b*3],state.joints[a*3+1]-state.joints[b*3+1],state.joints[a*3+2]-state.joints[b*3+2]);
			if(t==null)t=new float[]{0,0,1};
			float[] s=norm(-t[2],0,t[0]);if(s==null)s=new float[]{1,0,0};
			float[] u={s[1]*t[2]-s[2]*t[1],s[2]*t[0]-s[0]*t[2],s[0]*t[1]-s[1]*t[0]};
			if(u[1]<0){u[0]=-u[0];u[1]=-u[1];u[2]=-u[2];s[0]=-s[0];s[1]=-s[1];s[2]=-s[2];}
			double r=SerpentSpine.radius(i);
			for(int k=0;k<=SIDES;k++){
				double ang=Math.PI*2*k/SIDES,c=Math.cos(ang),sn=Math.sin(ang);
				float ox=(float)(s[0]*c*r*WIDE+u[0]*sn*r*TALL),oy=(float)(s[1]*c*r*WIDE+u[1]*sn*r*TALL),oz=(float)(s[2]*c*r*WIDE+u[2]*sn*r*TALL);
				rings[i][k]=new float[]{state.joints[i*3]+ox,state.joints[i*3+1]+oy,state.joints[i*3+2]+oz};
				float[] nn=norm((float)(s[0]*c+u[0]*sn),(float)(s[1]*c+u[1]*sn),(float)(s[2]*c+u[2]*sn));
				normals[i][k]=nn==null?u:nn;
			}
		}
		for(int i=0;i<n-1;i++){
			float v0=(i%4)*16/TEX_H,v1=v0+16/TEX_H;
			for(int k=0;k<SIDES;k++){
				float u0=k*8/TEX_W,u1=(k+1)*8/TEX_W;
				vertex(buffer,pose,rings[i][k],normals[i][k],u0,v0,overlay,state.lights[i]);
				vertex(buffer,pose,rings[i+1][k],normals[i+1][k],u0,v1,overlay,state.lights[i+1]);
				vertex(buffer,pose,rings[i+1][k+1],normals[i+1][k+1],u1,v1,overlay,state.lights[i+1]);
				vertex(buffer,pose,rings[i][k+1],normals[i][k+1],u1,v0,overlay,state.lights[i]);
			}
		}
	}

	private static void vertex(VertexConsumer buffer,PoseStack.Pose pose,float[] p,float[] n,float u,float v,int overlay,int light){
		buffer.addVertex(pose,p[0],p[1],p[2]).setColor(0xFFFFFFFF).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(pose,n[0],n[1],n[2]);
	}

	static float[] norm(float x,float y,float z){
		float l=(float)Math.sqrt(x*x+y*y+z*z);return l<1e-6F?null:new float[]{x/l,y/l,z/l};
	}
}
