package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.net.TidewardCue;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.*;
import net.minecraft.world.item.*;
import dev.wildercord.compat.McArmor;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import dev.wildercord.compat.McAttachments;

/** Shallow-water footwear and a single visible creature-warning observation. */
public final class TidewardEquipment {
 private TidewardEquipment(){}
 public static final int WALK_WEAR=20,WALK_WINDOW=100,WALK_REST=40,SIGHT_PREP=10,SIGHT_REST=60;
 public static final double SIGHT_RANGE=8;
 private static ResourceKey<Item> key(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 private static final net.minecraft.core.Holder<ArmorMaterial> WADER_MATERIAL=McArmor.material(Map.of(ArmorItem.Type.HELMET,0,ArmorItem.Type.CHESTPLATE,0,ArmorItem.Type.LEGGINGS,0,ArmorItem.Type.BOOTS,1),8,0,0,TagKey.create(Registries.ITEM,Wildercord.id("repairs_reedwater")),dev.wildercord.compat.McSpawnEgg.armorModel("wildercord:reedwater"));
 private static final net.minecraft.core.Holder<ArmorMaterial> GLASS_MATERIAL=McArmor.material(Map.of(ArmorItem.Type.HELMET,0,ArmorItem.Type.CHESTPLATE,0,ArmorItem.Type.LEGGINGS,0,ArmorItem.Type.BOOTS,0),4,0,0,TagKey.create(Registries.ITEM,Wildercord.id("repairs_dewglass")),dev.wildercord.compat.McSpawnEgg.armorModel("wildercord:dewglass"));
 public static final Item WADERS=Registry.register(BuiltInRegistries.ITEM,key("reedwater_waders"),new McArmor.LoreArmor("reedwater_waders",WADER_MATERIAL,ArmorItem.Type.BOOTS,new Item.Properties();var worn=p.getItemBySlot(EquipmentSlot.FEET);var pos=p.position();
  boolean same=state.world==p.level() && state.worn==worn;
  boolean eligible=same && worn.is(WADERS) && shallow(p) && clock(p)>=p.getAttachedOrElse(WALK_READY,0L);
  if(!eligible){removeWalk(p);state.world=((ServerLevel) p.level());state.worn=worn;state.at=pos;return;}
  double distance=pos.subtract(state.at).multiply(1,0,1).lengthSqr();state.at=pos;
  // A teleport does not pay or earn a walking window.
  if(distance>.25){removeWalk(p);return;}
  var attribute=p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
  if(attribute==null)return;
  if(!attribute.hasModifier(WALK_ID))attribute.addTransientModifier(WALK_MODIFIER);
  if(distance<.0001)return;
  int progress=p.getAttachedOrElse(WALK_PROGRESS,0)+1;
  boolean rest=progress>=WALK_WINDOW;
  p.setAttached(WALK_PROGRESS,rest?0:progress);
  if(rest)p.setAttached(WALK_READY,clock(p)+WALK_REST);
  if(progress%WALK_WEAR==0){removeWalk(p);worn.hurtAndBreak(1,p,EquipmentSlot.FEET);
   // Wear callbacks may remove, move or replace the actual wearer/equipment.
   if(!actor(p) || p.level()!=state.world || p.getItemBySlot(EquipmentSlot.FEET)!=worn || !worn.is(WADERS) || !shallow(p)){
    return;
   }
  }
  if(rest){removeWalk(p);Feels.sound(((ServerLevel) p.level()),p.position(),"tideward_weave_rest",.25F,1);}
 }
 public static boolean visibleWarning(ServerPlayer p,ReedbackCrab crab){
  return actor(p) && p.isShiftKeyDown() && !p.isPassenger() && crab.level()==p.level()
   && crab.isAlive() && !crab.isRemoved() && !crab.isInvisibleTo(p) && crab.pose()==ReedbackCrab.WARNING
   && p.getLookAngle().dot(crab.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize())>=.5
   && p.distanceToSqr(crab)<=SIGHT_RANGE*SIGHT_RANGE && p.level().hasChunkAt(crab.blockPosition()) && p.hasLineOfSight(crab);
 }
 private static void sight(ServerPlayer p){
  var state=p.getAttachedOrCreate(SIGHT);var worn=p.getItemBySlot(EquipmentSlot.HEAD);
  if(!actor(p) || !p.isShiftKeyDown() || !worn.is(SPECTACLES) || clock(p)<p.getAttachedOrElse(SIGHT_READY,0L)){
   state.target=null;state.ticks=0;state.world=((ServerLevel) p.level());state.worn=worn;return;
  }
  if(state.world!=((ServerLevel) p.level()) || state.worn!=worn){state.target=null;state.ticks=0;state.world=((ServerLevel) p.level());state.worn=worn;}
  ReedbackCrab crab=state.target!=null && p.serverLevel().getEntity(state.target) instanceof ReedbackCrab c?c:null;
  if(crab==null || !visibleWarning(p,crab)){
   state.target=null;state.ticks=0;
   if(p.tickCount%10!=0)return;
   // Query consumer aborts on its thirteenth candidate; it never materialises a dense list.
   var candidates=new ArrayList<ReedbackCrab>(13);
   p.level().getEntities(EntityTypeTest.<Entity,ReedbackCrab>forClass(ReedbackCrab.class),p.getBoundingBox().inflate(SIGHT_RANGE),c -> true,candidates,13);
   if(candidates.size()>12)return;
   crab=candidates.stream().filter(c -> visibleWarning(p,c)).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
   if(crab==null)return;state.target=crab.getUUID();
  }
  if(++state.ticks<SIGHT_PREP)return;
  state.target=null;state.ticks=0;
  if(!visibleWarning(p,crab) || p.getItemBySlot(EquipmentSlot.HEAD)!=worn)return;
  worn.hurtAndBreak(1,p,EquipmentSlot.HEAD);
  // The actual wear is paid before the observation; failed post-wear callbacks emit no success.
  if(p.getItemBySlot(EquipmentSlot.HEAD)!=worn || !worn.is(SPECTACLES) || !visibleWarning(p,crab))return;
  p.setAttached(SIGHT_READY,clock(p)+SIGHT_REST);
  TidewardCue.warning(p,crab);
  Feels.sound(((ServerLevel) p.level()),p.position(),"tideward_glass_warning",.3F,1);
 }
 public static void init(){
  ServerTickEvents.END_SERVER_TICK.register(s -> s.getPlayerList().getPlayers().forEach(p -> {walk(p);sight(p);}));
  dev.wildercord.net.ServerDisconnects.register((handler,server) -> removeWalk(handler.player));
  ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> {o.accept(WADERS);o.accept(SPECTACLES);});
 }
}
