package dev.wildercord.cast;

import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.MoteOption;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

/** Converts legacy decorative calls at the shared spell boundary. World particles outside
 * Wildercord are untouched. Client formation themes also use these original materials. */
public final class SpellMaterials {
	private SpellMaterials(){}
	public static MaterialOption of(String element,int color,float size){
		int style=switch(element){case "fire"->0;case "frost"->1;case "storm"->2;case "wind"->3;case "earth"->4;case "life"->5;case "void"->6;case "time"->8;case "blood"->9;case "water"->10;case "vapour"->11;default->7;};
		return new MaterialOption(style,color,Math.max(.02F,Math.min(.8F,size)),style==3?18:style==8?32:24);
	}
	private static int rgb(org.joml.Vector3f c){return ((int)(c.x*255)&255)<<16|((int)(c.y*255)&255)<<8|((int)(c.z*255)&255);}
	public static ParticleOptions custom(ParticleOptions p,Vec3 origin){
		if(!BuiltInRegistries.PARTICLE_TYPE.getKey(p.getType()).getNamespace().equals("minecraft"))return p;
		if(p instanceof TrailParticleOption trail){Vec3 d=trail.target().subtract(origin);return new MoteOption(MoteOption.SEEK,trail.color(),.1F,Math.max(2,Math.min(80,trail.duration())),(float)d.x,(float)d.y,(float)d.z,.75F);}
		if(p instanceof DustParticleOptions dust)return of("arcane",rgb(dust.getColor()),dust.getScale()*.09F);
		if(p instanceof DustColorTransitionOptions dust)return of("arcane",rgb(dust.getFromColor()),dust.getScale()*.09F);
		String id=BuiltInRegistries.PARTICLE_TYPE.getKey(p.getType()).getPath();
		String element=switch(id){
			case "flame","small_flame","soul_fire_flame","lava"->"fire";
			case "snowflake","item_snowball"->"frost";
			case "electric_spark"->"storm";
			case "gust","small_gust","gust_emitter_small","gust_emitter_large"->"wind";
			case "block","block_marker","falling_dust","crit","item"->"earth";
			case "happy_villager","totem_of_undying","cherry_leaves","pale_oak_leaves"->"life";
			case "portal","reverse_portal","dragon_breath","sculk_soul","soul"->"void";
			case "wax_on","wax_off","scrape"->"time";
			case "crimson_spore","damage_indicator"->"blood";
			case "bubble","bubble_pop","bubble_column_up","splash","falling_water","dripping_water","rain","underwater"->"water";
			case "cloud","smoke","large_smoke","white_smoke","campfire_cosy_smoke","campfire_signal_smoke","poof"->"vapour";
			default->"arcane";
		};
		int color=element.equals("water")?0x57CFF0:element.equals("vapour")?0xE2E9EF:Vfx.theme(element).primary();
		if(p instanceof SpellParticleOption spell)color=((int)(spell.getRed()*255)&255)<<16|((int)(spell.getGreen()*255)&255)<<8|((int)(spell.getBlue()*255)&255);
		return of(element,color,element.equals("vapour")?.32F:.12F);
	}
}
