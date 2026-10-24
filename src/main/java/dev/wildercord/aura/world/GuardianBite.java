package dev.wildercord.aura.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * A guardian's blow landing: like a Master's cut it goes partly through armour, Protection and Resistance, so a guardian stays a
 * threat to someone in full netherite. While {@link #land} runs, the defence hooks give back {@link #pierce}'s share of what they stop.
 */
public final class GuardianBite {
	private GuardianBite(){}
	/** The share of what defences stop that the landing blow goes through anyway; zero when no guardian's blow is landing. */
	private static double piercing;

	/** {@code attacker}'s blow of {@code damage} landing on {@code victim} as a mob attack, through {@code share} of its defences. */
	public static boolean land(ServerLevel level,LivingEntity attacker,LivingEntity victim,float damage,double share){
		double was=piercing;piercing=share;
		try{return victim.hurtServer(level,level.damageSources().mobAttack(attacker),damage);}
		finally{piercing=was;}
	}

	/** The share of its defences the landing guardian's blow goes through; zero when none is landing. */
	static double share(){
		return piercing;
	}
	static float pierced(float before,float after,double share){
		return share>0 && after<before?(float)(after+(before-after)*share):after;
	}
}
