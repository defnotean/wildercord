package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.net.TidewardCue;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
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
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Shallow-water footwear and a single visible creature-warning observation. */
public final class TidewardEquipment {
 private TidewardEquipment(){}
 public static final int WALK_WEAR=20,WALK_WINDOW=100,WALK_REST=40,SIGHT_PREP=10,SIGHT_REST=60;
 public static final double SIGHT_RANGE=8;
 private static ResourceKey<Item> key(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 private static final ArmorMaterial WADER_MATERIAL=new ArmorMaterial(8,Map.of(ArmorType.HELMET,0,ArmorType.CHESTPLATE,0,ArmorType.LEGGINGS,0,ArmorType.BOOTS,1),8,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,TagKey.create(Registries.ITEM,Wildercord.id("repairs_reedwater")),ResourceKey.create(EquipmentAssets.ROOT_ID,Wildercord.id("reedwater")));
 private static final ArmorMaterial GLASS_MATERIAL=new ArmorMaterial(6,Map.of(ArmorType.HELMET,0,ArmorType.CHESTPLATE,0,ArmorType.LEGGINGS,0,ArmorType.BOOTS,0),4,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,TagKey.create(Registries.ITEM,Wildercord.id("repairs_dewglass")),ResourceKey.create(EquipmentAssets.ROOT_ID,Wildercord.id("dewglass")));
 public static final Item WADERS=Registry.register(BuiltInRegistries.ITEM,key("reedwater_waders"),new AuraWorld.Lore("reedwater_waders",new Item.Properties().setId(key("reedwater_waders")).humanoidArmor(WADER_MATERIAL,ArmorType.BOOTS).rarity(Rarity.UNCOMMON)));
 public static final Item SPECTACLES=Registry.register(BuiltInRegistries.ITEM,key("dewglass_spectacles"),new AuraWorld.Lore("dewglass_spectacles",new Item.Properties().setId(key("dewglass_spectacles")).humanoidArmor(GLASS_MATERIAL,ArmorType.HELMET).rarity(Rarity.UNCOMMON)));
 private static AttachmentType<Long> rest(String name){return AttachmentRegistry.create(Wildercord.id(name),b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath());}
 public static final AttachmentType<Long> WALK_READY=rest("tideward_walk_ready"),SIGHT_READY=rest("tideward_sight_ready");
 public static final AttachmentType<Integer> WALK_PROGRESS=AttachmentRegistry.create(Wildercord.id("tideward_walk_progress"),b -> b.initializer(() -> 0).persistent(Codec.intRange(0,WALK_WINDOW-1)).copyOnDeath());
 private static final Identifier WALK_ID=Wildercord.id("reedwater_walk");
 private static final AttributeModifier WALK_MODIFIER=new AttributeModifier(WALK_ID,.25,AttributeModifier.Operation.ADD_VALUE);
 private static final class Walk {ItemStack worn=ItemStack.EMPTY;ServerLevel world;Vec3 at=Vec3.ZERO;}
 private static final class Sight {ItemStack worn=ItemStack.EMPTY;ServerLevel world;UUID target;int ticks;}
 private static final AttachmentType<Walk> WALK=AttachmentRegistry.create(Wildercord.id("tideward_walk"),b -> b.initializer(Walk::new));
 private static final AttachmentType<Sight> SIGHT=AttachmentRegistry.create(Wildercord.id("tideward_sight"),b -> b.initializer(Sight::new));
 public static long clock(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 public static boolean actor(ServerPlayer p){return p.isAlive() && !p.isRemoved() && !p.isSpectator() && !p.isCreative();}
 public static boolean shallow(ServerPlayer p){
  var l=p.level();var at=p.blockPosition();var floor=at.below();
  return actor(p) && p.onGround() && p.isInWater() && !p.isSwimming() && !p.isSprinting() && !p.isPassenger()
   && l.hasChunkAt(at) && l.hasChunkAt(floor) && l.hasChunkAt(at.above())
   && l.getFluidState(at).is(FluidTags.WATER) && l.getFluidState(at.above()).isEmpty()
   && l.getBlockState(floor).isCollisionShapeFullBlock(l,floor);
 }
 private static void removeWalk(ServerPlayer p){var a=p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);if(a!=null)a.removeModifier(WALK_ID);}
 private static void walk(ServerPlayer p){
  var state=p.getAttachedOrCreate(WALK);var worn=p.getItemBySlot(EquipmentSlot.FEET);var pos=p.position();
  boolean same=state.world==p.level() && state.worn==worn;
  boolean eligible=same && worn.is(WADERS) && shallow(p) && clock(p)>=p.getAttachedOrElse(WALK_READY,0L);
  if(!eligible){removeWalk(p);state.world=p.level();state.worn=worn;state.at=pos;return;}
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
  if(rest){removeWalk(p);Feels.sound(p.level(),p.position(),"tideward_weave_rest",.25F,1);}
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
   state.target=null;state.ticks=0;state.world=p.level();state.worn=worn;return;
  }
  if(state.world!=p.level() || state.worn!=worn){state.target=null;state.ticks=0;state.world=p.level();state.worn=worn;}
  ReedbackCrab crab=state.target!=null && p.level().getEntity(state.target) instanceof ReedbackCrab c?c:null;
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
  Feels.sound(p.level(),p.position(),"tideward_glass_warning",.3F,1);
 }
 public static void init(){
  ServerTickEvents.END_SERVER_TICK.register(s -> s.getPlayerList().getPlayers().forEach(p -> {walk(p);sight(p);}));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server) -> removeWalk(handler.player));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> {o.accept(WADERS);o.accept(SPECTACLES);});
 }
}
