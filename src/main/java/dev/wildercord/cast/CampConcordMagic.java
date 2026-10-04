package dev.wildercord.cast;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;
/** Two ordinary utility owners. Later observations never execute spell links. */
public final class CampConcordMagic {
 private CampConcordMagic(){}
 public static boolean apply(Cast c,SpellPlan.EffectNode node,Cast.Hit hit,List<LivingEntity> helped){
  if(!Effects.builtIn(node.effect))return false;
  switch(node.effect.path()){
   case "watchweft"->{if(!c.passive)Watchweft.arm(c,hit);}
   case "manabraid"->{if(!c.passive)for(var target:helped)if(Manabraid.offer(c,target))break;}
   default->{return false;}
  }return true;
 }
 public static void init(){CampConcordFx.init();CampConcordFeels.init();
  ServerTickEvents.END_SERVER_TICK.register(s->{Watchweft.tick(s);Manabraid.tick(s);});
  ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{Watchweft.remove(h.player.getUUID(),false);Manabraid.depart(h.player.getUUID());});
  ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{Watchweft.remove(h.player.getUUID(),false);Manabraid.depart(h.player.getUUID());});
  ServerLifecycleEvents.SERVER_STOPPING.register(s->{Watchweft.clear();Manabraid.clear();});
 }
}
