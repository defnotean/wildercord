package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * The Rootcoil Serpent's head, built in its own frame on the front joint: +F along the snout, +U up, +S to one side. A skull,
 * a snout, heavy brows, horns swept back, fangs, and a jaw hinged under the skull that drops as it hisses. Each box takes the
 * vanilla cube layout from its corner of the skin (laid out by {@code tools/serpent_art.py}); sizes are in sixteenths.
 */
final class SerpentHead {
	/** Blocks to a sixteenth of the head: nearly twice a block model's, so its skull is wider than a player's shoulders. */
	static final float SCALE=1.8F/16;
	private final float[] o,f,u,s;
	private final float jaw;

	SerpentHead(SerpentRenderState state){
		o=new float[]{state.joints[0],state.joints[1],state.joints[2]};
		float[] fw=SerpentRenderer.norm(state.headX,state.headY,state.headZ);f=fw==null?new float[]{0,0,1}:fw;
		float[] side=SerpentRenderer.norm(f[2],0,-f[0]);s=side==null?new float[]{1,0,0}:side;
		u=new float[]{f[1]*s[2]-f[2]*s[1],f[2]*s[0]-f[0]*s[2],f[0]*s[1]-f[1]*s[0]};
		jaw=state.jaw;
	}

	void skin(PoseStack.Pose pose,VertexConsumer buffer,int overlay,int light){
		box(pose,buffer,overlay,light,0,1,9,12,8,14,64,0,0);                   // skull
		box(pose,buffer,overlay,light,0,0,20,8,5,8,64,22,0);                   // snout
		for(int side=-1;side<=1;side+=2){
			box(pose,buffer,overlay,light,side*5,5.5F,12,4,2,6,96,32,0);        // brow
			box(pose,buffer,overlay,light,side*5,5.5F,2,2,2,8,96,22,.35F);      // horn, swept back and up
			box(pose,buffer,overlay,light,side*2.5F,-4,23,1,3,1,120,0,0);       // fang
		}
		// The jaw, hinged under the back of the skull.
		box(pose,buffer,overlay,light,0,-4.5F,12,10,3,20,64,40,jaw*.75F,0,-3,3);
	}

	void eyes(PoseStack.Pose pose,VertexConsumer buffer,int overlay){
		for(int side=-1;side<=1;side+=2)box(pose,buffer,overlay,LightCoordsUtil.FULL_BRIGHT,side*6.05F,3,13,1,2,3,120,8,0);
	}

	private void box(PoseStack.Pose pose,VertexConsumer buffer,int overlay,int light,float x,float y,float z,float w,float h,float d,int tu,int tv,float tilt){
		box(pose,buffer,overlay,light,x,y,z,w,h,d,tu,tv,tilt,x,y,z);
	}

	/** A box centred at (x, y, z), pitched by {@code tilt} radians (positive tips its front down) about the hinge (hx, hy, hz). */
	private void box(PoseStack.Pose pose,VertexConsumer buffer,int overlay,int light,float x,float y,float z,float w,float h,float d,int tu,int tv,float tilt,float hx,float hy,float hz){
		float cs=Mth.cos(tilt),sn=Mth.sin(tilt);
		// Corners: bit 1 = +S, bit 2 = +U, bit 4 = +F.
		float[][] c=new float[8][];
		for(int i=0;i<8;i++){
			float lx=x+((i&1)==0?-w:w)/2,ly=y+((i&2)==0?-h:h)/2,lz=z+((i&4)==0?-d:d)/2;
			float ry=ly-hy,rz=lz-hz;
			c[i]=at(lx*SCALE,(hy+ry*cs-rz*sn)*SCALE,(hz+ry*sn+rz*cs)*SCALE);
		}
		float[] up=dir(0,cs,sn),fw=dir(0,-sn,cs),side=dir(1,0,0);
		quad(pose,buffer,overlay,light,c[2],c[3],c[7],c[6],up,tu+d,tv,w,d);              // top
		quad(pose,buffer,overlay,light,c[4],c[5],c[1],c[0],neg(up),tu+d+w,tv,w,d);        // bottom
		quad(pose,buffer,overlay,light,c[7],c[3],c[1],c[5],side,tu,tv+d,d,h);             // +S
		quad(pose,buffer,overlay,light,c[6],c[7],c[5],c[4],fw,tu+d,tv+d,w,h);             // front
		quad(pose,buffer,overlay,light,c[2],c[6],c[4],c[0],neg(side),tu+d+w,tv+d,d,h);   // -S
		quad(pose,buffer,overlay,light,c[3],c[2],c[0],c[1],neg(fw),tu+2*d+w,tv+d,w,h);   // back
	}

	private float[] at(float x,float y,float z){
		return new float[]{o[0]+s[0]*x+u[0]*y+f[0]*z,o[1]+s[1]*x+u[1]*y+f[1]*z,o[2]+s[2]*x+u[2]*y+f[2]*z};
	}

	private float[] dir(float x,float y,float z){return new float[]{s[0]*x+u[0]*y+f[0]*z,s[1]*x+u[1]*y+f[1]*z,s[2]*x+u[2]*y+f[2]*z};}

	private static float[] neg(float[] v){return new float[]{-v[0],-v[1],-v[2]};}

	/** Corners top-left, top-right, bottom-right, bottom-left of a texture rectangle. */
	private static void quad(PoseStack.Pose pose,VertexConsumer buffer,int overlay,int light,float[] a,float[] b,float[] c,float[] d,float[] n,float tu,float tv,float tw,float th){
		float u0=tu/SerpentRenderer.TEX_W,u1=(tu+tw)/SerpentRenderer.TEX_W,v0=tv/SerpentRenderer.TEX_H,v1=(tv+th)/SerpentRenderer.TEX_H;
		vertex(pose,buffer,a,n,u0,v0,overlay,light);
		vertex(pose,buffer,d,n,u0,v1,overlay,light);
		vertex(pose,buffer,c,n,u1,v1,overlay,light);
		vertex(pose,buffer,b,n,u1,v0,overlay,light);
	}

	private static void vertex(PoseStack.Pose pose,VertexConsumer buffer,float[] p,float[] n,float u,float v,int overlay,int light){
		buffer.addVertex(pose,p[0],p[1],p[2]).setColor(0xFFFFFFFF).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(pose,n[0],n[1],n[2]);
	}
}
