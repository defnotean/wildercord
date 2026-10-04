package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.equipment.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Bell commits a full second; boots prepare stationary crouched feet and resist one narrow physical root hold. */
public final class BelowkeeperEquipment {
 private BelowkeeperEquipment(){}
 public static final int BELL_REST=240,GREAVES_REST=200,PREP_TICKS=30;
 private static ResourceKey<Item> key(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 private static AttachmentType<Long> rest(String n){return AttachmentRegistry.create(Wildercord.id(n),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());}
 public static final AttachmentType<Long> BELL_READY=rest("empty_bell_ready"),GREAVES_READY=rest("rootbound_ready");
 private static final class Prep {ItemStack worn=ItemStack.EMPTY;Vec3 at=Vec3.ZERO;ServerLevel level;int ticks;}
 private record Ring(UUID source,ItemStack held,InteractionHand hand,ServerLevel level,Vec3 at){}
 private static final AttachmentType<Ring> RING=AttachmentRegistry.create(Wildercord.id("empty_bell_commitment"),b -> {});
 private static boolean committed(ServerPlayer p,Ring ring){return ring!=null && actor(p) && p.level()==ring.level() && p.position().distanceToSqr(ring.at())<.0009 && p.isUsingItem() && p.getUsedItemHand()==ring.hand() && p.getUseItem()==ring.held() && p.getItemInHand(ring.hand())==ring.held() && ring.held().is(BELL) && ring.level().getEntity(ring.source()) instanceof RootmoltStrider r && reachable(p,r);}
 private static void clearRing(ServerPlayer p){if(p.getAttached(RING)!=null){p.removeAttached(RING);if(p.isUsingItem() && p.getUseItem().is(BELL))p.stopUsingItem();}}
 private static final AttachmentType<Prep> PREP=AttachmentRegistry.create(Wildercord.id("rootbound_preparation"),b -> b.initializer(Prep::new));
 private static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 private static boolean actor(ServerPlayer p){return p.isAlive() && !p.isSpectator() && !p.isCreative();}
 private static RootmoltStrider active(ServerPlayer p){var l=p.level();var owned=l.getEntity(p.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER));if(owned instanceof RootmoltStrider r && reachable(p,r))return r;return l.getEntitiesOfClass(RootmoltStrider.class,p.getBoundingBox().inflate(5),r -> reachable(p,r)).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);}
 private static boolean reachable(ServerPlayer p,RootmoltStrider r){return r.isAlive() && !r.isRemoved() && r.level()==p.level() && p.distanceToSqr(r)<=25 && p.hasLineOfSight(r) && (r.pose()==RootmoltStrider.WARNING || r.pose()==RootmoltStrider.RAKING || r.pose()==RootmoltStrider.HOLDING);}
 public static final Item BELL=Registry.register(BuiltInRegistries.ITEM,key("maras_empty_bell"),new AuraWorld.Lore("maras_empty_bell",new Item.Properties().setId(key("maras_empty_bell")).durability(32).rarity(Rarity.RARE).component(DataComponents.CONSUMABLE,Consumable.builder().consumeSeconds(1F).animation(ItemUseAnimation.BLOCK).sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.COPPER_BULB_HIT)).hasConsumeParticles(false).build())) {
  @Override public InteractionResult use(Level l,Player user,InteractionHand hand){if(!user.isAlive() || user.isSpectator())return InteractionResult.PASS;if(user instanceof ServerPlayer p){var source=active(p);if(!actor(p) || now(p)<p.getAttachedOrElse(BELL_READY,0L) || source==null)return InteractionResult.FAIL;p.setAttached(RING,new Ring(source.getUUID(),p.getItemInHand(hand),hand,p.level(),p.position()));}user.startUsingItem(hand);return InteractionResult.CONSUME;}
  @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity e){if(!(e instanceof ServerPlayer p))return stack;var ring=p.getAttached(RING);if(now(p)<p.getAttachedOrElse(BELL_READY,0L) || !committed(p,ring) || p.getTicksUsingItem()<20 || ring.held()!=stack){clearRing(p);return stack;}var r=(RootmoltStrider)ring.level().getEntity(ring.source());p.removeAttached(RING);if(!r.answerBell(p))return stack;p.setAttached(BELL_READY,now(p)+BELL_REST);p.getCooldowns().addCooldown(stack,BELL_REST);stack.hurtAndBreak(1,p,p.getUsedItemHand().asEquipmentSlot());p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40));p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,40));Feels.sound(p.level(),p.position(),"belowkeeper_bell",.65F,1);return stack;}
 });
 private static final ArmorMaterial MATERIAL=new ArmorMaterial(8,Map.of(ArmorType.HELMET,0,ArmorType.CHESTPLATE,0,ArmorType.LEGGINGS,0,ArmorType.BOOTS,1),8,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,ItemTags.REPAIRS_LEATHER_ARMOR,ResourceKey.create(EquipmentAssets.ROOT_ID,Wildercord.id("rootbound")));
 public static final Item GREAVES=Registry.register(BuiltInRegistries.ITEM,key("rootbound_greaves"),new AuraWorld.Lore("rootbound_greaves",new Item.Properties().setId(key("rootbound_greaves")).humanoidArmor(MATERIAL,ArmorType.BOOTS).rarity(Rarity.UNCOMMON)));
 public static int prepared(ServerPlayer p){return p.getAttachedOrCreate(PREP).ticks;}
 private static void tick(ServerPlayer p){var ring=p.getAttached(RING);if(ring!=null && !committed(p,ring))clearRing(p);var boots=p.getItemBySlot(EquipmentSlot.FEET);if(!boots.is(GREAVES)){var old=p.getAttached(PREP);if(old!=null){old.ticks=0;old.worn=ItemStack.EMPTY;old.level=null;}return;}var prep=p.getAttachedOrCreate(PREP);boolean still=actor(p) && prep.level==p.level() && boots.is(GREAVES) && p.onGround() && p.isShiftKeyDown() && !p.isSprinting() && !p.isPassenger() && !p.isInWater() && p.position().distanceToSqr(prep.at)<.0009 && now(p)>=p.getAttachedOrElse(GREAVES_READY,0L);if(still && prep.worn==boots)prep.ticks=Math.min(PREP_TICKS,prep.ticks+1);else {prep.ticks=0;prep.at=p.position();}prep.worn=boots;prep.level=p.level();}
 private static boolean resist(LivingEntity victim,RootmoltStrider source){if(!(victim instanceof ServerPlayer p) || !actor(p))return false;var boots=p.getItemBySlot(EquipmentSlot.FEET);if(!boots.is(GREAVES))return false;var prep=p.getAttachedOrCreate(PREP);if(!boots.is(GREAVES) || prep.level!=p.level() || prep.worn!=boots || prep.ticks<PREP_TICKS || !p.onGround() || !p.isShiftKeyDown() || p.isSprinting() || p.isPassenger() || p.isInWater() || p.position().distanceToSqr(prep.at)>=.0009 || now(p)<p.getAttachedOrElse(GREAVES_READY,0L))return false;prep.ticks=0;p.setAttached(GREAVES_READY,now(p)+GREAVES_REST);boots.hurtAndBreak(8,p,EquipmentSlot.FEET);p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40));Feels.sound(p.level(),p.position(),"belowkeeper_anchor",.5F,1);return true;}
 public static void init(){RootmoltContent.ROOT_RESISTANCE.register(BelowkeeperEquipment::resist);ServerTickEvents.END_SERVER_TICK.register(s -> s.getPlayerList().getPlayers().forEach(BelowkeeperEquipment::tick));ServerPlayConnectionEvents.JOIN.register((handler,sender,s) -> {long left=handler.player.getAttachedOrElse(BELL_READY,0L)-now(handler.player);if(left>0)handler.player.getCooldowns().addCooldown(new ItemStack(BELL),(int)Math.min(BELL_REST,left));});CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> {o.accept(BELL);o.accept(GREAVES);});}
}
