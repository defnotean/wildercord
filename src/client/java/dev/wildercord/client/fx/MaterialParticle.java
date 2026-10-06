package dev.wildercord.client.fx;

import dev.wildercord.content.MaterialOption;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/** Twelve authored materials: flame curls, angular ice, forked sparks, wind ribbons, tumbling
 * stone, fluttering petals, contracting voids, rune fragments, clock gears, drops and vapour. */
public final class MaterialParticle extends SingleQuadParticle implements SigilGroup.Extent {
	private final int style;
	private final float size, phase;
	private final java.util.UUID outcomeSource=LifeOwnerClearance.source();
	private final boolean outcomeOwner=LifeOwnerClearance.owner();
	MaterialParticle(ClientLevel level, double x, double y, double z, MaterialOption o, double vx, double vy, double vz) {
		super(level,x,y,z,SpellCircleParticle.particleSprite("material_"+o.style()+"_0"));
		style=o.style(); size=o.size(); lifetime=o.lifetime(); phase=random.nextFloat()*6.283185F;
		setColor((o.color()>>16&255)/255F,(o.color()>>8&255)/255F,(o.color()&255)/255F);
		xd=vx; yd=vy; zd=vz; hasPhysics=false; quadSize=size;
		roll=style==MaterialOption.WATER || style==MaterialOption.BLOOD ? 0 : phase; oRoll=roll;
	}
	@Override public void tick() {
		xo=x; yo=y; zo=z; oRoll=roll;
		if(age++>=lifetime){remove();return;}
		double curl=Math.sin(age*.36+phase);
		switch(style) {
			case MaterialOption.EMBER -> { yd+=.0015; xd+=curl*.0018; roll+=.045F; }
			case MaterialOption.FROST -> { yd-=.001; roll+=.075F; }
			case MaterialOption.STORM -> { xd+=curl*.008; zd+=Math.cos(age*.9+phase)*.008; roll+=.17F; }
			case MaterialOption.WIND -> { xd+=Math.cos(phase)*.001; zd+=Math.sin(phase)*.001; roll+=.09F; }
			case MaterialOption.STONE -> { yd-=.006; roll+=.13F; }
			case MaterialOption.PETAL -> { yd-=.0005; xd+=curl*.002; roll+=Math.cos(age*.22+phase)*.055F; }
			case MaterialOption.VOID -> { xd*=.86; yd*=.86; zd*=.86; roll-=.085F; }
			case MaterialOption.ARCANE -> { yd+=.0004; roll+=.04F; }
			case MaterialOption.TIME -> { xd*=.94; yd*=.94; zd*=.94; roll-=.025F; }
			case MaterialOption.BLOOD -> { yd-=.0028; }
			case MaterialOption.WATER -> { yd-=.0045; xd*=.96; zd*=.96; }
			case MaterialOption.VAPOUR -> { yd+=.001; xd*=.94; zd*=.94; roll+=.009F; }
		}
		x+=xd; y+=yd; z+=zd; xd*=.98; yd*=.98; zd*=.98;
		float t=age/(float)lifetime;
		alpha=Math.min(1,t*5)*Math.min(1,(1-t)*3);
		quadSize=size*(style==MaterialOption.VOID?1-t*.7F:style==MaterialOption.VAPOUR?.6F+t:1);
		setSprite(SpellCircleParticle.particleSprite("material_"+style+"_"+(age/4%2)));
	}
	@Override public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		if(!outcomeOwner||LifeOwnerClearance.visible(outcomeSource,level,camera,partial,xo,yo,zo,x,y,z))super.extract(state,camera,partial);
	}
	@Override protected Layer getLayer() { return style==MaterialOption.STONE || style==MaterialOption.VAPOUR ? Layer.TRANSLUCENT : GlowLayers.GLOW; }
	@Override public int getLightCoords(float partial) { return style==MaterialOption.STONE || style==MaterialOption.VAPOUR ? super.getLightCoords(partial) : LightCoordsUtil.FULL_BRIGHT; }
	@Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
	@Override public double centreX(){return x;}
	@Override public double centreY(){return y;}
	@Override public double centreZ(){return z;}
	@Override public double reach(){return size*2;}
	public static final class Provider implements ParticleProvider<MaterialOption> {
		@Override public Particle createParticle(MaterialOption o, ClientLevel level,double x,double y,double z,double vx,double vy,double vz,RandomSource random){return new MaterialParticle(level,x,y,z,o,vx,vy,vz);}
	}
}
