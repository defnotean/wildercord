package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** One finite planted fan, one real frontal vanilla arrow, one exact consenting recipient. */
public final class RooksRainshield {
 private RooksRainshield(){}
 public static final int PREP=12,WINDOW=40,REST=300,WEAR=4;
 public static final double RANGE_SQR=9,STILL_SQR=.0064;
 /** Seventy-degree total frontal cone, including vertical incoming motion. */
 private static final double FRONT_COS=Math.cos(Math.toRadians(35));
 private static final class Selection {
  final ServerLevel level;final UUID recipient;final ServerPlayer recipientBody;final long until;final ItemStack held;final InteractionHand hand;boolean previousShift,consented;
  Selection(ServerPlayer p,ServerPlayer r,ItemStack stack,InteractionHand h){level=p.level();recipient=r.getUUID();recipientBody=r;until=now(p)+40;held=stack;hand=h;previousShift=r.isShiftKeyDown();}
  ServerLevel level(){return level;}UUID recipient(){return recipient;}long until(){return until;}
 }
 private record Draw(ServerLevel level,ItemStack held,InteractionHand hand,UUID nonce) {}
 private static final class Fan {
  final ServerLevel level;final ServerPlayer carrier,recipient;final ItemStack held;final InteractionHand hand;final Vec3 origin,normal;final long start,until,generation;final UUID nonce=UUID.randomUUID();boolean spent;int beat;
  Fan(ServerPlayer p,ServerPlayer r,InteractionHand h,long generation){this.generation=generation;level=p.level();carrier=p;recipient=r;held=p.getItemInHand(h);hand=h;origin=p.position();normal=p.getLookAngle().multiply(1,0,1).normalize();start=now(p);until=start+PREP+WINDOW;}
 }
 private static final AttachmentType<Selection> SELECT=AttachmentRegistry.create(Wildercord.id("rainshield_selection"),b->{});
 private static final AttachmentType<Long> GENERATION=AttachmentRegistry.create(Wildercord.id("rainshield_generation"),b->b.initializer(()->0L));
 private static final AttachmentType<Draw> DRAW=AttachmentRegistry.create(Wildercord.id("rainshield_draw"),b->{});
 private static final AttachmentType<Fan> FAN=AttachmentRegistry.create(Wildercord.id("rainshield_fan"),b->{}),COVER=AttachmentRegistry.create(Wildercord.id("rainshield_cover"),b->{});
 public static final AttachmentType<Long> READY=AttachmentRegistry.create(Wildercord.id("rainshield_ready"),b->b.initializer(()->0L).persistent(Codec.LONG).copyOnDeath());
 private static ResourceKey<Item> key(){return ResourceKey.create(Registries.ITEM,Wildercord.id("rooks_rainshield"));}
 // The final pivot point is retained: a draw requires at least five durability, so wear cannot break ownership during callbacks.
 public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,key(),new AuraWorld.Lore("rooks_rainshield",new Item.Properties().setId(key()).durability(65).rarity(Rarity.RARE)){
  @Override public int getUseDuration(ItemStack stack,LivingEntity e){return PREP+WINDOW+1;}
  @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.BLOCK;}
  @Override public InteractionResult interactLivingEntity(ItemStack stack,Player user,LivingEntity target,InteractionHand h){
   if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;
   if(!(target instanceof ServerPlayer r)||p==r||!pair(p,r)||!stack.is(ITEM)||p.getItemInHand(h)!=stack||p.getAttached(FAN)!=null||p.getAttached(COVER)!=null||r.getAttached(COVER)!=null)return InteractionResult.FAIL;
   var selection=p.getAttached(SELECT);
   if(selection!=null&&selection.level==p.level()&&selection.recipient.equals(r.getUUID())&&selection.recipientBody==r&&selection.held==stack&&selection.hand==h&&now(p)<=selection.until&&selection.consented&&r.isShiftKeyDown())return use(p.level(),p,h);
   p.setAttached(SELECT,new Selection(p,r,stack,h));r.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.rainshield.offer",p.getDisplayName()));return InteractionResult.SUCCESS;
  }
  @Override public InteractionResult use(Level l,Player user,InteractionHand h){
   if(!(user instanceof ServerPlayer p)){if(user.isAlive()&&!user.isSpectator()){user.startUsingItem(h);return InteractionResult.CONSUME;}return InteractionResult.FAIL;}
   var drawn=p.getAttached(DRAW);if(drawn!=null&&(drawn.level()!=p.level()||drawn.held()!=p.getItemInHand(drawn.hand())))p.removeAttached(DRAW);
   if(l!=p.level()||p.getAttached(DRAW)!=null||p.isUsingItem()||!actor(p)||!p.getItemInHand(h).is(ITEM)||p.getAttached(FAN)!=null||p.getAttached(COVER)!=null||now(p)<p.getAttachedOrElse(READY,0L)||p.getItemInHand(h).getMaxDamage()-p.getItemInHand(h).getDamageValue()<=WEAR)return InteractionResult.FAIL;
   var selection=p.getAttached(SELECT);ServerPlayer r=p;
   if(selection!=null){if(now(p)>selection.until()){p.removeAttached(SELECT);selection=null;}else{if(selection.level()!=p.level()||p.getItemInHand(h)!=selection.held||h!=selection.hand||!(p.level().getEntity(selection.recipient()) instanceof ServerPlayer selected)||selected!=selection.recipientBody||!pair(p,selected)||!selection.consented||!selected.isShiftKeyDown())return InteractionResult.FAIL;r=selected;}}
   if(!pair(p,r)||r.getAttached(COVER)!=null||now(p)<r.getAttachedOrElse(READY,0L))return InteractionResult.FAIL;
   long generation=p.getAttachedOrElse(GENERATION,0L);if(generation==Long.MAX_VALUE)return InteractionResult.FAIL;var fan=new Fan(p,r,h,generation+1);if(fan.normal.lengthSqr()<.9||!front(fan))return InteractionResult.FAIL;
   p.removeAttached(SELECT);p.setAttached(GENERATION,fan.generation);p.setAttached(DRAW,new Draw(p.level(),fan.held,h,fan.nonce));p.setAttached(FAN,fan);r.setAttached(COVER,fan);p.startUsingItem(h);RainshieldFx.send(p,r,fan.normal,0,fan.nonce,fan.generation);Feels.sound(p.level(),p.position(),"rainshield_pivot",.3F,1);return InteractionResult.CONSUME;
  }
  @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity e){if(e instanceof ServerPlayer p){var f=p.getAttached(FAN);if(f!=null&&f.level==l&&f.held==stack&&p.isUsingItem()&&p.getUseItem()==stack&&p.getUseItemRemainingTicks()==0)clear(f,true);}return stack;}
  @Override public boolean releaseUsing(ItemStack stack,Level l,LivingEntity e,int left){if(e instanceof ServerPlayer p){var f=p.getAttached(FAN);if(f!=null&&f.level==l&&f.held==stack&&p.isUsingItem()&&p.getUseItem()==stack&&left==p.getUseItemRemainingTicks()){var d=p.getAttached(DRAW);if(d!=null&&d.nonce().equals(f.nonce))p.removeAttached(DRAW);clear(f,false);}}return false;}
 });
 private static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 private static boolean actor(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&!p.isPassenger()&&!p.isInWater()&&p.onGround()&&p.level().hasChunkAt(p.blockPosition())&&p.level().getEntity(p.getUUID())==p&&p.level().getServer().getPlayerList().getPlayer(p.getUUID())==p;}
 private static boolean pair(ServerPlayer p,ServerPlayer r){return actor(p)&&actor(r)&&p.level()==r.level()&&(p==r||p.getTeam()!=null&&p.isAlliedTo(r))&&p.distanceToSqr(r)<=RANGE_SQR&&loadedSpan(p,r)&&p.hasLineOfSight(r);}
 private static boolean loadedSpan(ServerPlayer p,ServerPlayer r){var a=p.getBoundingBox();var b=r.getBoundingBox();int minX=net.minecraft.core.BlockPos.containing(Math.min(a.minX,b.minX),0,0).getX()>>4,maxX=net.minecraft.core.BlockPos.containing(Math.max(a.maxX,b.maxX),0,0).getX()>>4,minZ=net.minecraft.core.BlockPos.containing(0,0,Math.min(a.minZ,b.minZ)).getZ()>>4,maxZ=net.minecraft.core.BlockPos.containing(0,0,Math.max(a.maxZ,b.maxZ)).getZ()>>4;if(maxX-minX>1||maxZ-minZ>1)return false;for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!p.level().hasChunkAt(new net.minecraft.core.BlockPos(x<<4,p.blockPosition().getY(),z<<4)))return false;return true;}
 private static boolean front(Fan f){var d=f.recipient.position().subtract(f.carrier.position()).multiply(1,0,1);return f.carrier==f.recipient||d.lengthSqr()>.04&&d.normalize().dot(f.normal)>=.5;}
 private static boolean bound(Fan f){return f!=null&&!f.spent&&f.carrier.getAttached(FAN)==f&&f.recipient.getAttached(COVER)==f&&facts(f)&&now(f.carrier)<=f.until;}
 private static boolean facts(Fan f){return pair(f.carrier,f.recipient)&&f.carrier.level()==f.level&&f.carrier.getItemInHand(f.hand)==f.held&&f.held.is(ITEM)&&f.carrier.isUsingItem()&&f.carrier.getUseItem()==f.held&&f.carrier.getUsedItemHand()==f.hand&&!f.carrier.isSprinting()&&f.carrier.position().distanceToSqr(f.origin)<=STILL_SQR&&f.carrier.getLookAngle().multiply(1,0,1).normalize().dot(f.normal)>=.95&&front(f)&&(f.carrier==f.recipient||f.recipient.isShiftKeyDown());}
 private static void clear(Fan f,boolean stop){if(f==null)return;boolean owned=f.carrier.getAttached(FAN)==f;if(owned){f.carrier.removeAttached(FAN);RainshieldFx.send(f.carrier,f.recipient,f.normal,3,f.nonce,f.generation);}if(f.recipient.getAttached(COVER)==f)f.recipient.removeAttached(COVER);if(stop&&(owned||f.carrier.getAttached(FAN)==null)&&f.carrier.isUsingItem()&&f.carrier.getUseItem()==f.held)f.carrier.stopUsingItem();}
 /** Called only at actual Projectile.onHit HEAD before imbued-arrow release; false leaves vanilla collision unchanged. */
 public static boolean intercept(AbstractArrow arrow,EntityHitResult hit){
  if(!(arrow.level() instanceof ServerLevel l)||!l.getServer().isSameThread()||!(hit.getEntity() instanceof ServerPlayer r)||!arrow.isAlive()||arrow.isRemoved()||l.getEntity(arrow.getUUID())!=arrow||arrow.hasAttached(WildercordAttachments.IMBUED_SHOT))return false;
  if(!(arrow.getType()==EntityTypes.ARROW&&arrow.getClass()==Arrow.class||arrow.getType()==EntityTypes.SPECTRAL_ARROW&&arrow.getClass()==SpectralArrow.class))return false;
  var f=r.getAttached(COVER);if(!bound(f)||f.level!=l||f.recipient!=r||now(f.carrier)<f.start+PREP||f.carrier.getTicksUsingItem()<PREP||now(f.carrier)<f.carrier.getAttachedOrElse(READY,0L)||now(r)<r.getAttachedOrElse(READY,0L))return false;
  var velocity=arrow.getDeltaMovement();if(velocity.lengthSqr()<.0001||velocity.normalize().dot(f.normal)>-FRONT_COS||arrow.getOwner()==r||arrow.getOwner()==f.carrier)return false;
  var position=arrow.position();var owner=arrow.getOwner();var recipientAt=r.position();var carrierAt=f.carrier.position();var carrierVelocity=f.carrier.getDeltaMovement();var recipientVelocity=r.getDeltaMovement();float carrierHealth=f.carrier.getHealth(),recipientHealth=r.getHealth();var collision=hit.getLocation();if(collision.distanceToSqr(r.getBoundingBox().getCenter())>4||!l.hasChunkAt(net.minecraft.core.BlockPos.containing(collision)))return false;
  f.spent=true;clear(f,false);long deadline=now(f.carrier)+REST;f.carrier.setAttached(READY,deadline);r.setAttached(READY,deadline);f.carrier.getCooldowns().addCooldown(f.held,REST);if(r!=f.carrier)r.getCooldowns().addCooldown(new ItemStack(ITEM),REST);
  f.held.hurtAndBreak(WEAR,f.carrier,f.hand.asEquipmentSlot());
  // Cost is not refunded if an external wear callback invalidates actor, recipient or projectile ownership.
  if(!facts(f)||!f.carrier.position().equals(carrierAt)||!f.carrier.getDeltaMovement().equals(carrierVelocity)||!r.getDeltaMovement().equals(recipientVelocity)||f.carrier.getHealth()!=carrierHealth||r.getHealth()!=recipientHealth||f.carrier.getAttached(FAN)!=null||r.getAttached(COVER)!=null||f.carrier.getAttachedOrElse(READY,0L)!=deadline||r.getAttachedOrElse(READY,0L)!=deadline||!arrow.isAlive()||arrow.isRemoved()||arrow.level()!=l||l.getEntity(arrow.getUUID())!=arrow||arrow.hasAttached(WildercordAttachments.IMBUED_SHOT)||arrow.getDeltaMovement().distanceToSqr(velocity)>.000001||!arrow.position().equals(position)||arrow.getOwner()!=owner||!r.position().equals(recipientAt)){clear(f,true);return false;}
  arrow.discard();f.carrier.stopUsingItem();f.carrier.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,40,0));Feels.sound(l,collision,"rainshield_catch",.55F,1);RainshieldFx.send(f.carrier,r,f.normal,2,f.nonce,f.generation);return true;
 }
 /** Input intent identifies only this player's exact transient draw; never grants or renews cover/rest. */
 static void releaseInput(ServerPlayer p,UUID nonce){
  if(!p.level().getServer().isSameThread()||p.level().getServer().getPlayerList().getPlayer(p.getUUID())!=p||p.level().getEntity(p.getUUID())!=p)return;
  var d=p.getAttached(DRAW);if(d==null||!d.nonce().equals(nonce)||d.level()!=p.level()||p.getItemInHand(d.hand())!=d.held())return;
  var f=p.getAttached(FAN);if(f!=null&&(!f.nonce.equals(nonce)||f.held!=d.held()))return;
  p.removeAttached(DRAW);if(f!=null)clear(f,true);
 }
 private static void tick(ServerPlayer p){var drawn=p.getAttached(DRAW);if(drawn!=null&&(!p.isAlive()||p.isRemoved()||drawn.level()!=p.level()||p.getItemInHand(drawn.hand())!=drawn.held()))p.removeAttached(DRAW);var selection=p.getAttached(SELECT);if(selection!=null){if(selection.level()!=p.level()||now(p)>selection.until()||p.getItemInHand(selection.hand)!=selection.held||!(p.level().getEntity(selection.recipient()) instanceof ServerPlayer r)||r!=selection.recipientBody||!pair(p,r)){p.removeAttached(SELECT);}else{boolean shift=r.isShiftKeyDown();if(selection.consented&&!shift){p.removeAttached(SELECT);}else{if(!selection.previousShift&&shift)selection.consented=true;selection.previousShift=shift;}}}var f=p.getAttached(FAN);if(f==null)return;if(!bound(f)){clear(f,true);return;}int elapsed=(int)(now(p)-f.start);if(elapsed==4||elapsed==8||elapsed==PREP){RainshieldFx.send(p,f.recipient,f.normal,elapsed==PREP?1:0,f.nonce,f.generation);Feels.sound(p.level(),p.position(),elapsed==PREP?"rainshield_open":"rainshield_pivot",.3F,1);} }
 public static boolean active(ServerPlayer p){return bound(p.getAttached(FAN));}
 public static void init(){RainshieldFx.init();ServerTickEvents.END_SERVER_TICK.register(s->s.getPlayerList().getPlayers().forEach(RooksRainshield::tick));ServerLivingEntityEvents.AFTER_DAMAGE.register((e,damage,base,taken,blocked)->{if(taken>0&&e instanceof ServerPlayer p){clear(p.getAttached(FAN),true);clear(p.getAttached(COVER),true);}});ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{clear(h.player.getAttached(FAN),true);clear(h.player.getAttached(COVER),true);h.player.removeAttached(SELECT);h.player.removeAttached(DRAW);});ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{long left=h.player.getAttachedOrElse(READY,0L)-now(h.player);if(left>0)h.player.getCooldowns().addCooldown(new ItemStack(ITEM),(int)Math.min(REST,left));});CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->o.accept(ITEM));}
}
