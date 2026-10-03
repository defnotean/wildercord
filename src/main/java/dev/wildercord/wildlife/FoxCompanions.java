package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.mixin.FoxAccessor;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import java.util.*;

/** Fish friendship for both fox species; vanilla trust remains intact. */
public final class FoxCompanions {
 public static final TagKey<Item> FISH=TagKey.create(Registries.ITEM,Wildercord.id("fox_taming_fish"));
 public record Bond(UUID owner,boolean sitting) {
  public static final Codec<Bond> CODEC=RecordCodecBuilder.create(i -> i.group(UUIDUtil.CODEC.fieldOf("owner").forGetter(Bond::owner),Codec.BOOL.optionalFieldOf("sitting",true).forGetter(Bond::sitting)).apply(i,Bond::new));
 }
 public static final AttachmentType<Bond> BOND=AttachmentRegistry.create(Wildercord.id("fox_companion"),b -> b.persistent(Bond.CODEC).syncWith(ByteBufCodecs.fromCodec(Bond.CODEC),AttachmentSyncPredicate.all()));
 public static boolean fish(ItemStack stack) {return stack.is(FISH);}
 public static void init() {UseEntityCallback.EVENT.register((p,l,h,e,hit) -> e instanceof Fox f ? use(f,p,h) : InteractionResult.PASS);}
 public static InteractionResult use(Fox fox,Player p,InteractionHand hand) {
  if(p.isSpectator() || !fox.isAlive())return InteractionResult.PASS;
  var held=p.getItemInHand(hand);var bond=fox.getAttached(BOND);
  if(bond==null && fish(held)) {
   if(fox.level() instanceof ServerLevel level) {
    held.consume(1,p);boolean success=fox.getRandom().nextDouble()<WildlifeRules.TAME_CHANCE;
    if(success) {fox.setAttached(BOND,new Bond(p.getUUID(),true));fox.setPersistenceRequired();((FoxAccessor)fox).wildercord$trust(p);wake(fox);fox.setSitting(true);fox.getNavigation().stop();fox.setTarget(null);}
    level.sendParticles(success?ParticleTypes.HEART:ParticleTypes.SMOKE,fox.getX(),fox.getY()+.5,fox.getZ(),7,.3,.25,.3,.01);
   }
   return InteractionResult.SUCCESS;
  }
  if(bond==null || !bond.owner().equals(p.getUUID()))return InteractionResult.PASS;
  if(fish(held)) {
   if(fox.getHealth()<fox.getMaxHealth() && !fox.level().isClientSide()) {held.consume(1,p);fox.heal(3);}
   return InteractionResult.SUCCESS;
  }
  if(held.isEmpty()) {
   if(!fox.level().isClientSide()) {fox.setAttached(BOND,new Bond(bond.owner(),!bond.sitting()));wake(fox);fox.setSitting(!bond.sitting());fox.getNavigation().stop();fox.setTarget(null);}
   return InteractionResult.SUCCESS.withoutItem();
  }
  return InteractionResult.PASS;
 }
 private static void wake(Fox f) {((FoxAccessor)f).wildercord$sleep(false);((FoxAccessor)f).wildercord$defend(false);f.setIsCrouching(false);}
 public static void goals(Fox f) {f.getGoalSelector().addGoal(1,new Sit(f));f.getGoalSelector().addGoal(3,new Follow(f));}
 private static final class Sit extends Goal {
  private final Fox f;
  Sit(Fox f) {this.f=f;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP,Flag.LOOK));}
  public boolean canUse() {var b=f.getAttached(BOND);return b!=null && b.sitting() && !f.isInWater() && f.onGround();}
  public void start() {f.getNavigation().stop();wake(f);f.setSitting(true);}
  public void tick() {f.getNavigation().stop();f.setSitting(true);}
  public void stop() {f.setSitting(false);}
 }
 private static final class Follow extends Goal {
  private final Fox f;private Player owner;private int repath;
  Follow(Fox f) {this.f=f;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse() {var b=f.getAttached(BOND);owner=b==null || b.sitting()?null:f.level().getPlayerByUUID(b.owner());return owner!=null && owner.isAlive() && !owner.isSpectator() && f.distanceToSqr(owner)>64;}
  public boolean canContinueToUse() {var b=f.getAttached(BOND);return b!=null && !b.sitting() && owner!=null && owner.isAlive() && !owner.isSpectator() && f.distanceToSqr(owner)>9;}
  public void start() {wake(f);f.setSitting(false);repath=0;}
  public void tick() {f.getLookControl().setLookAt(owner,30,30);if(--repath<=0) {repath=10;f.getNavigation().moveTo(owner,1.2);}}
  public void stop() {owner=null;f.getNavigation().stop();}
 }
}
