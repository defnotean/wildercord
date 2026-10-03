package dev.wildercord.client.fx;

import dev.wildercord.content.AirflowOption;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Wind-only stream bands: curved, world-lit and translucent, with a crest moving along the flow. */
public final class AirflowParticle extends SingleQuadParticle implements SigilGroup.Extent {
    private final AirflowOption flow;
    private final Quaternionf turn = new Quaternionf();
    private final Vector3f tangent = new Vector3f();
    AirflowParticle(ClientLevel level,double x,double y,double z,AirflowOption option){
        super(level,x,y,z,SpellCircleParticle.particleSprite("wind_filament"));
        flow=option;lifetime=option.lifetime();hasPhysics=false;gravity=0;
    }
    static Vec3 point(AirflowOption o,double t){return o.control().scale(2*t*(1-t)).add(o.end().scale(t*t));}
    @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();}
    @Override public void extract(QuadParticleRenderState state,Camera camera,float partial){
        float f=Mth.clamp((age+partial)/lifetime,0,1);
        float fade=Math.min(1,(age+partial)*2)*Math.min(1,(1-f)*3);
        if(MagicQuality.reducedFlash)fade*=.65F;
        int steps=flow.minimal()?6:16;
        Vec3 origin=new Vec3(x,y,z).subtract(camera.position());
        float crest=.12F+f*.76F;
        int light=super.getLightCoords(partial);
        for(int i=0;i<steps;i++){
            double a=i/(double)steps,b=(i+1)/(double)steps,m=(a+b)*.5;
            Vec3 from=point(flow,a),to=point(flow,b),d=to.subtract(from);
            if(d.lengthSqr()<.000001)continue;
            Vec3 at=origin.add(point(flow,m));
            tangent.set((float)d.x,(float)d.y,(float)d.z).normalize();
            Facing.along(tangent,(float)at.x,(float)at.y,(float)at.z,turn);
            double taper=Math.pow(Math.sin(Math.PI*m),.6);
            double wave=.25+.75*Math.exp(-Math.pow((m-crest)*5,2));
            int alpha=Mth.clamp((int)(190*fade*taper*wave),0,255);
            float half=(float)Math.max(d.length()*.64,flow.width()*(.6+.4*wave));
            state.add(Layer.TRANSLUCENT,(float)at.x,(float)at.y,(float)at.z,turn.x,turn.y,turn.z,turn.w,half,
                sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1(),(alpha<<24)|flow.color(),light);
        }
    }
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
    @Override public double centreX(){return x;}
    @Override public double centreY(){return y;}
    @Override public double centreZ(){return z;}
    @Override public double reach(){return Math.max(flow.control().length(),flow.end().length())+.4;}
    public static final class Provider implements ParticleProvider<AirflowOption>{
        @Override public Particle createParticle(AirflowOption o,ClientLevel l,double x,double y,double z,double vx,double vy,double vz,RandomSource random){
            return new AirflowParticle(l,x,y,z,o);
        }
    }
}
