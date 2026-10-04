package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.mixin.MossveilEffectAccess;
import dev.wildercord.mixin.MossveilEffectNotice;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A small physical poison filter; no damage multiplier, healing, effect-chain replacement or immunity. */
public final class MossveilCowl{
 public static final int PREP=40,REST=200,MAX_TRIM=MossveilRules.MAX_TRIM;
 private static ResourceKey<Item> key(){return ResourceKey.create(Registries.ITEM,Wildercord.id("mossveil_cowl"));}
 private static final ArmorMaterial MATERIAL=new ArmorMaterial(6,Map.of(ArmorType.HELMET,1,ArmorType.CHESTPLATE,0,ArmorType.LEGGINGS,0,ArmorType.BOOTS,0),5,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,0,0,net.minecraft.tags.TagKey.create(Registries.ITEM,Wildercord.id("repairs_mossveil")),ResourceKey.create(EquipmentAssets.ROOT_ID,Wildercord.id("mossveil")));
 public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,key(),new AuraWorld.Lore("mossveil_cowl",new Item.Properties().setId(key()).humanoidArmor(MATERIAL,ArmorType.HELMET).rarity(Rarity.UNCOMMON)));
 public static final AttachmentType<Long> READY=AttachmentRegistry.create(Wildercord.id("mossveil_filter_ready"),b->b.initializer(()->0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());
 private record Facts(int amplifier,boolean ambient,boolean visible,boolean icon){static Facts of(MobEffectInstance e){return new Facts(e.getAmplifier(),e.isAmbient(),e.isVisible(),e.showIcon());}boolean matches(MobEffectInstance e){return e!=null&&amplifier==e.getAmplifier()&&ambient==e.isAmbient()&&visible==e.isVisible()&&icon==e.showIcon();}}
 private static final class Prep{boolean operating;ItemStack worn;ServerLevel world;MossveilDormouse pet;MossveilHome.Home home;MobEffectInstance poison;Facts facts;Vec3 planted;int ticks;void clear(){worn=null;world=null;pet=null;home=null;poison=null;facts=null;planted=null;ticks=0;}}
 private static final AttachmentType<Prep> STATE=AttachmentRegistry.create(Wildercord.id("mossveil_filter_preparation"),b->b.initializer(Prep::new));
 private static boolean actor(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!p.isCreative()&&!p.isSpectator()&&p.level().getEntity(p.getUUID())==p&&p.isShiftKeyDown()&&p.onGround()&&!p.isPassenger();}
 private static boolean poison(MobEffectInstance e){return e!=null&&!e.isInfiniteDuration()&&e.getDuration()>=8;}
 private static boolean pet(ServerPlayer p,MossveilDormouse e){return e!=null&&e.live(p.level())&&e.isTame()&&e.isOwnedBy(p)&&e.isOrderedToSit()&&e.isInSittingPose()&&p.distanceToSqr(e)<=9&&p.hasLineOfSight(e)&&MossveilDormouse.clock(p.level())>=e.filterReady();}
 private static boolean current(ServerPlayer p,Prep a){return actor(p)&&p.level()==a.world&&p.getItemBySlot(EquipmentSlot.HEAD)==a.worn&&a.worn!=null&&a.worn.is(ITEM)&&MossveilRules.planted(p.position().distanceToSqr(a.planted))&&p.getEffect(MobEffects.POISON)==a.poison&&poison(a.poison)&&a.facts.matches(a.poison)&&pet(p,a.pet)&&a.home!=null&&a.home.world()==p.level()&&a.home.current()&&Vec3.atCenterOf(a.home.canopy()).distanceToSqr(p.position())<=9&&Vec3.atCenterOf(a.home.canopy()).distanceToSqr(a.pet.position())<=9;}
 static void tick(ServerPlayer p){
  var a=p.getAttachedOrCreate(STATE);if(a.operating)return;
  if(!actor(p)||!p.getItemBySlot(EquipmentSlot.HEAD).is(ITEM)||!poison(p.getEffect(MobEffects.POISON))||MossveilDormouse.clock(p.level())<p.getAttachedOrElse(READY,0L)){a.clear();return;}
  if(a.worn==null){
   if(p.tickCount%10!=0)return;var pool=WetlandQueries.scan(p.level(),MossveilDormouse.class,p.getBoundingBox().inflate(3),null);if(pool.saturated())return;
   var found=pool.entities().stream().filter(e->pet(p,e)).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);if(found==null)return;
   var home=MossveilHome.find(p.level(),p.blockPosition(),p.position(),found.position());if(home==null)return;
   a.worn=p.getItemBySlot(EquipmentSlot.HEAD);a.world=p.level();a.pet=found;a.home=home;a.poison=p.getEffect(MobEffects.POISON);a.facts=Facts.of(a.poison);a.planted=p.position();a.ticks=0;
  }
  if(!current(p,a)){a.clear();return;}if(++a.ticks>=PREP)commit(p,a);
 }
 private static void commit(ServerPlayer p,Prep a){
  // Lease is active before compatibility claims. A nested tick cannot charge the same preparation.
  a.operating=true;
  try{
   var body=p.position();var sourceBody=a.pet.position();var world=a.world;var worn=a.worn;var source=a.pet;var old=a.poison;var home=a.home;var facts=a.facts;int before=old.getDuration();
   if(!current(p,a)||!home.allowed(p)||!current(p,a)||!p.position().equals(body)||!source.position().equals(sourceBody)||old.getDuration()!=before)return;
   int trim=MossveilRules.trim(before),after=before-trim;if(trim<1)return;
   long until=MossveilDormouse.clock(world)+REST;p.setAttached(READY,until);source.reserveFilter(until);
   worn.hurtAndBreak(1,p,EquipmentSlot.HEAD);
   // Rest was reserved before wear. Rechecking excludes that paid rest from pet readiness.
   if(!paidCurrent(p,world,worn,source,sourceBody,body,old,home,facts)||old.getDuration()!=before)return;
   ((MossveilEffectAccess)(Object)old).mossveil$duration(after);
   ((MossveilEffectNotice)(Object)p).mossveil$updated(old,false,source);
   if(!paidCurrent(p,world,worn,source,sourceBody,body,old,home,facts)||old.getDuration()!=after)return;
   p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,20,0));
   if(paidCurrent(p,world,worn,source,sourceBody,body,old,home,facts)&&old.getDuration()==after){MossveilFilterCue.send(p,source,trim);p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.mossveil.trim",trim));dev.wildercord.cast.feel.Feels.sound(world,p.position(),"mossveil_filter",.3F,1);}
  }finally{a.clear();a.operating=false;}
 }
 private static boolean paidCurrent(ServerPlayer p,ServerLevel world,ItemStack worn,MossveilDormouse source,Vec3 sourceBody,Vec3 body,MobEffectInstance old,MossveilHome.Home home,Facts facts){return actor(p)&&p.level()==world&&p.position().equals(body)&&p.getItemBySlot(EquipmentSlot.HEAD)==worn&&worn.is(ITEM)&&source.live(world)&&source.isOwnedBy(p)&&source.isOrderedToSit()&&source.isInSittingPose()&&source.position().equals(sourceBody)&&p.distanceToSqr(source)<=9&&p.hasLineOfSight(source)&&home.current()&&p.getEffect(MobEffects.POISON)==old&&facts.matches(old);}
 public static void init(){ServerTickEvents.END_SERVER_TICK.register(s->s.getPlayerList().getPlayers().forEach(MossveilCowl::tick));}
}
