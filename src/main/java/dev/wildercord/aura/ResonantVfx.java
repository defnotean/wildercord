package dev.wildercord.aura;

import dev.wildercord.cast.*;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.spell.RuneColors;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Ten authored meetings: a physical cut opens first, the spell material answers, then disperses. No circles. */
public final class ResonantVfx {
	private ResonantVfx() {}
	public static void play(ServerLevel level, ServerPlayer striker, LivingEntity target, String spell, String blade, String name) {
		Vec3 at=target.getBoundingBox().getCenter(), feet=target.position();
		Vec3 toward=at.subtract(striker.getBoundingBox().getCenter()).normalize();
		if(toward.lengthSqr()<.01) toward=new Vec3(0,0,1);
		Vec3 side=ElementFx.perp(toward), axis=toward;
		int color=RuneColors.element(spell), steel=Aura.color(striker);
		// Blade schools change the incision's geometry, rather than painting one universal ring.
		switch(blade) {
			case "earth" -> { AuraFx.groundScar(level,feet,1.5,55,2); ElementFx.ray(level,at.subtract(side),at.add(side),steel,.045,8); }
			case "wind" -> { for(int i=0;i<2;i++) ElementFx.slash(level,at.add(0,i*.25,0),axis,side,steel,1.1,1.9,.06,2,8); }
			case "storm" -> { for(int i=-1;i<=1;i++) ElementFx.ray(level,at.add(side.scale(i*.35)).add(0,-.7,0),at.add(side.scale(-i*.35)).add(0,.7,0),steel,.035,6); }
			case "frost" -> { ElementFx.ray(level,at.subtract(side.scale(.8)).add(0,-.5,0),at.add(side.scale(.8)).add(0,.5,0),steel,.04,9); ElementFx.ray(level,at.subtract(side.scale(.5)).add(0,.7,0),at.add(side.scale(.5)).add(0,-.7,0),steel,.025,9); }
			case "life" -> ElementFx.slash(level,at.add(0,-.25,0),axis,side.add(0,.7,0).normalize(),steel,.8,2.5,.045,2,10);
			case "void" -> ElementFx.slash(level,at,axis,side,steel|Light.DARK,.9,2.8,.085,2,10);
			case "arcane" -> { for(int i=0;i<3;i++) ElementFx.ray(level,at.subtract(side.scale(.8-i*.2)).add(0,-.25+i*.25,0),at.add(side.scale(.8-i*.2)).add(0,.25+i*.25,0),steel,.025,7); }
			case "time" -> { ElementFx.slash(level,at,axis,side,steel,1,1.5,.055,2,8); ElementFx.slash(level,at.subtract(axis.scale(.18)),axis,side,steel,.85,1.3,.025,1,12); }
			case "blood" -> { ElementFx.ray(level,at.subtract(side.scale(.7)).add(0,.6,0),at.add(side.scale(.7)).add(0,-.6,0),steel,.055,10); ElementFx.ray(level,at.subtract(side.scale(.3)).add(0,.7,0),at.add(side.scale(.3)).add(0,-.7,0),steel,.025,8); }
			default -> ElementFx.slash(level,at,axis,side.add(0,.4,0).normalize(),steel,1,2.2,.07,2,8);
		}
		Feels.sound(level,at,ResonantRules.sound(spell),.65F,1);
		// Material must unfold on the visible face of the contact, not inside an opaque creature model.
		Vec3 face=at.subtract(axis.scale(Math.min(.8,target.getBbWidth()*.5+.16)));
		material(level,face,feet,axis,side,spell,color,0,name);
		Scheduler.later(4,() -> material(level,face,feet,axis,side,spell,color,1,name));
		Scheduler.later(9,() -> material(level,face,feet,axis,side,spell,color,2,name));
	}
	private static void material(ServerLevel l,Vec3 c,Vec3 feet,Vec3 axis,Vec3 side,String element,int color,int phase,String name) {
		double reach=.35+phase*.3;
		switch(element) {
			case "fire" -> {
				// Embers catch the incision, then peel into a forked flame fan (wind twists it).
				for(int i=-1;i<=1;i++) { Vec3 tip=c.add(side.scale(i*reach)).add(0,.2+phase*.3,0); ElementFx.slash(l,tip,axis,side,color,.35+phase*.12,1.5,.07,2,7); Vfx.emit(l,ParticleTypes.SMALL_FLAME,tip,2,.07,.015); }
				if(name.equals("bellows_cut")) ElementFx.swirl(l,c.add(0,-.35,0),reach,.8,2,0xF4B563,0xD8F4E9);
			}
			case "frost" -> {
				// Four brittle splinters separate along the cut; fire's thermal cleave vents steam.
				for(int i=0;i<4;i++) { Vec3 p=c.add(side.scale((i-1.5)*reach)); ElementFx.ray(l,p.add(0,-.18,0),p.add(0,.22+phase*.13,0),color,.025,8); }
				if(phase==1) ElementFx.shards(l,c,.6,5);
				if(name.equals("thermal_cleave")) Vfx.emit(l,ParticleTypes.CLOUD,c,3,.16,.025);
			}
			case "storm" -> {
				// A fork runs from blade scar into the body, then grounds into two separate prongs.
				if(phase<2) { ElementFx.bolt(l,c.subtract(side.scale(reach)),c.add(0,.55,0),.025,1,1); ElementFx.bolt(l,c.add(0,.55,0),feet.add(side.scale(reach)),.025,1,1); }
				else ElementFx.sparks(l,c,5,.08);
				if(name.equals("grounding_stroke") && phase==1) AuraFx.groundScar(l,feet,1.25,45,2);
			}
			case "wind" -> {
				// Two opposing gust crescents sweep past one another and leave a narrow wake.
				ElementFx.slash(l,c.add(side.scale(reach)),axis,side,color,.5+reach,1.4,.04,2,7);
				ElementFx.slash(l,c.subtract(side.scale(reach)),axis,side.scale(-1),color,.5+reach,1.4,.025,1,7);
				Vfx.fling(l,ParticleTypes.CLOUD,c,axis,.08+phase*.03);
			}
			case "earth" -> {
				// The blow spreads down to the floor: bounded visual fractures and grit, no block damage.
				if(phase==0) AuraFx.groundScar(l,feet,1.7,65,2);
				Vfx.emit(l,new BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),feet.add(0,.2,0),4,.2,.035);
				if(name.equals("gravel_rime")) ElementFx.ray(l,feet.subtract(side.scale(reach)),feet.add(side.scale(reach)).add(0,.07,0),0xC5ECF3,.025,8);
			}
			case "life" -> {
				// Leaves fold from the incision toward the wielder, flowering at the final beat.
				for(int i=0;i<3;i++) ElementFx.slash(l,c.add(side.scale((i-1)*reach)),axis,new Vec3(0,1,0),color,.22+phase*.09,2.2,.04,2,9);
				ElementFx.petals(l,c,.25+phase*.1,3);
				if(name.equals("grafted_edge")) ElementFx.ray(l,c.add(0,-.2,0),c.add(0,.5,0),0xB84850,.025,8);
			}
			case "void" -> {
				// A dark seam closes inward. Arcane's prism fractures along its rim.
				ElementFx.ray(l,c.subtract(side.scale(1-reach*.6)),c.add(side.scale(1-reach*.6)),color|Light.DARK,.06,8);
				for(int i=-1;i<=1;i++) ElementFx.ray(l,c.add(side.scale(i*.4)).add(0,-reach*.5,0),c.add(side.scale(i*.15)).add(0,reach*.5,0),color,.02,7);
				if(name.equals("veiled_prism")) ElementFx.shimmer(l,c,.25,3);
			}
			case "arcane" -> {
				// Three disconnected prism facets unfold; they never form a magic circle.
				for(int i=-1;i<=1;i++) { Vec3 p=c.add(side.scale(i*reach)); ElementFx.ray(l,p.add(0,-.3,0),p.add(axis.scale(.25)).add(0,.15,0),color,.025,9); ElementFx.ray(l,p.add(axis.scale(.25)).add(0,.15,0),p.add(0,.5,0),0xFFF1D2,.015,9); }
			}
			case "time" -> {
				// An incision's three displaced echoes recede instead of spinning a clock face.
				ElementFx.slash(l,c.subtract(axis.scale(phase*.22)),axis,side,phase==0?0xFFF6CA:color,.85-phase*.18,1.7,.04,1,9);
				if(phase==2) Vfx.emit(l,ParticleTypes.WAX_OFF,c,3,.15,.015);
			}
			case "blood" -> {
				// Broken red threads pull tight across the cut, then snap outward.
				for(int i=-1;i<=1;i++) ElementFx.ray(l,c.add(side.scale(-reach)).add(0,i*.15,0),c.add(side.scale(reach)).add(0,-i*.15,0),color,.025,7);
				if(phase==2) Vfx.radial(l,new DustParticleOptions(color,.7F),c,5,.09);
				if(name.equals("hollow_pulse")) ElementFx.ray(l,c.add(0,-.4,0),c.add(0,.4,0),color|Light.DARK,.045,6);
			}
			default -> { }
		}
	}
}
