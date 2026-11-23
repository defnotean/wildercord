package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import dev.wildercord.compat.McArmor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import dev.wildercord.compat.McAttachments;
/** Bell commits a full second; boots prepare stationary crouched feet and resist one narrow physical root hold. */
public final class BelowkeeperEquipment {
 private BelowkeeperEquipment(){}
 public static final int BELL_REST=240,GREAVES_REST=200,PREP_TICKS=30;
 private static ResourceKey<Item> key(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 private static AttachmentType<Long> rest(String n){return McAttachments.create(Wildercord.id(n),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());}
 public static final AttachmentType<Long> BELL_READY=rest("empty_bell_ready"),GREAVES_READY=rest("rootbound_ready");
 private static final class Prep {ItemStack worn=ItemStack.EMPTY;Vec3 at=Vec3.ZERO;ServerLevel level;int ticks;}
 private record Ring(UUID source,ItemStack held,InteractionHand hand,ServerLevel level,Vec3 at){}
 private static final AttachmentType<Ring> RING=McAttachments.create(Wildercord.id("empty_bell_commitment"),b -> {});
 private static boolean committed(ServerPlayer p,Ring ring){return ring!=null && actor(p) && p.level()==ring.level() && p.position().distanceToSqr(ring.at())<.0009 && p.isUsingItem() && p.getUsedItemHand()==ring.hand() && p.getUseItem()==ring.held() && p.getItemInHand(ring.hand())==ring.held() && ring.held().is(BELL) && ring.level().getEntity(ring.source()) instanceof RootmoltStrider r && reachable(p,r);}
 private static void clearRing(ServerPlayer p){if(p.getAttached(RING)!=null){p.removeAttached(RING);if(p.isUsingItem() && p.getUseItem().is(BELL))p.stopUsingItem();}}
 private static final AttachmentType<Prep> PREP=McAttachments.create(Wildercord.id("rootbound_preparation"),b -> b.initializer(Prep::new));
 private static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 private static boolean actor(ServerPlayer p){return p.isAlive() && !p.isSpectator() && !p.isCreative();}
 private static RootmoltStrider active(ServerPlayer p){var l=p.level();var owned=p.serverLevel().getEntity(p.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER));if(owned instanceof RootmoltStrider r && reachable(p,r))return r;return l.getEntitiesOfClass(RootmoltStrider.class,p.getBoundingBox().inflate(5),r -> reachable(p,r)).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);}
 private static boolean reachable(ServerPlayer p,RootmoltStrider r){return r.isAlive() && !r.isRemoved() && r.level()==p.level() && p.distanceToSqr(r)<=25 && p.hasLineOfSight(r) && (r.pose()==RootmoltStrider.WARNING || r.pose()==RootmoltStrider.RAKING || r.pose()==RootmoltStrider.HOLDING);}
	public static final Item BELL=Registry.register(BuiltInRegistries.ITEM,key("maras_empty_bell"),new AuraWorld.Lore("maras_empty_bell",new Item.Properties(){RootmoltContent.ROOT_RESISTANCE.register(BelowkeeperEquipment::resist);ServerTickEvents.END_SERVER_TICK.register(s -> s.getPlayerList().getPlayers().forEach(BelowkeeperEquipment::tick));ServerPlayConnectionEvents.JOIN.register((handler,sender,s) -> {long left=handler.player.getAttachedOrElse(BELL_READY,0L)-now(handler.player);if(left>0)handler.player.getCooldowns().addCooldown(new ItemStack(BELL),(int)Math.min(BELL_REST,left));});ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> {o.accept(BELL);o.accept(GREAVES);});}
}
