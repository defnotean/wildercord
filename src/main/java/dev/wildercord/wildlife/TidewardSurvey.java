package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.net.TidewardCue;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** A bounded private read of actual footing, never a terrain mutation or companion command. */
public final class TidewardSurvey {
 private TidewardSurvey(){}
 public static final int REST=80,ANCHOR_LIFE=100,ROUTE_LIFE=100,MAX_CELLS=9;
 private static final ResourceKey<Item> KEY=ResourceKey.create(Registries.ITEM,Wildercord.id("bank_surveyors_line"));
 public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,KEY,new AuraWorld.Lore("bank_surveyors_line",new Item.Properties().setId(KEY).durability(96)){
  @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer() instanceof ServerPlayer p)survey(p,c);return InteractionResult.SUCCESS;}
 });
 public static final AttachmentType<Long> READY=AttachmentRegistry.create(Wildercord.id("bank_survey_ready"),b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath());
 private static final class State {ServerLevel world;ItemStack held=ItemStack.EMPTY;InteractionHand hand;boolean reading;BlockPos first;List<BlockPos> route=List.of();long until,nonce;}
 private static final AttachmentType<State> STATE=AttachmentRegistry.create(Wildercord.id("bank_survey_state"),b -> b.initializer(State::new));
 private static boolean current(ServerPlayer p,State s){return TidewardReadAdmission.actor(p) && s.world==p.level() && s.hand!=null && p.getItemInHand(s.hand)==s.held && s.held.is(ITEM);}
 private static void clear(ServerPlayer p,State s){if(!s.route.isEmpty())TidewardCue.clear(p,s.nonce);s.route=List.of();s.first=null;s.until=0;}
 private static boolean refuse(ServerPlayer p,String reason){p.sendOverlayMessage(Component.translatable("message.wildercord.bank_line."+reason));return false;}
 public static List<BlockPos> columns(BlockPos a,BlockPos b){
  int dx=b.getX()-a.getX(),dy=b.getY()-a.getY(),dz=b.getZ()-a.getZ();int steps=Math.max(Math.abs(dx),Math.abs(dz));
  if(steps<1 || steps>8 || Math.abs(dy)>1 || (long)dx*dx+(long)dz*dz>64)return List.of();
  var cells=new ArrayList<BlockPos>(steps+1);
  for(int i=0;i<=steps;i++){double t=i/(double)steps;var at=a.offset((int)Math.round(dx*t),(int)Math.round(dy*t),(int)Math.round(dz*t));if(cells.isEmpty() || !cells.getLast().equals(at))cells.add(at);}
  return List.copyOf(cells);
 }
 public static boolean footing(ServerPlayer p,BlockPos floor){
  var l=p.level();var feet=floor.above();var head=feet.above();
  if(!l.hasChunkAt(floor) || !l.hasChunkAt(feet) || !l.hasChunkAt(head))return false;
  var f=l.getBlockState(floor);var low=l.getBlockState(feet);var high=l.getBlockState(head);
  if(!f.isCollisionShapeFullBlock(l,floor) || !f.getFluidState().isEmpty() || !low.getCollisionShape(l,feet).isEmpty() || !high.getCollisionShape(l,head).isEmpty()
   || !high.getFluidState().isEmpty() || !low.getFluidState().isEmpty() && !low.getFluidState().is(FluidTags.WATER))return false;
  // Read admission is independent of magic-edit configuration and Adventure build permission.
  return TidewardReadAdmission.allows(p,l,floor) && TidewardReadAdmission.allows(p,l,feet) && TidewardReadAdmission.allows(p,l,head)
   && l.getBlockState(floor)==f && l.getBlockState(feet)==low && l.getBlockState(head)==high && p.level()==l && TidewardReadAdmission.actor(p);
 }
 private static boolean visibleBank(ServerPlayer p,BlockPos floor){
  if(!footing(p,floor) || Vec3.atCenterOf(floor).distanceToSqr(p.getEyePosition())>36)return false;
  var hit=p.level().clip(new ClipContext(p.getEyePosition(),Vec3.atCenterOf(floor).add(0,.499,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
  return hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(floor);
 }
 private static boolean route(ServerPlayer p,List<BlockPos> cells){
  if(cells.size()<2 || cells.size()>MAX_CELLS || !TidewardReadAdmission.actor(p))return false;
  var world=p.level();var snapshot=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
  // Retain every support/feet/head cell BEFORE any permission callback can change earlier cells.
  for(var floor:cells)for(var at:List.of(floor,floor.above(),floor.above(2))){
   if(!world.hasChunkAt(at) || world.isOutsideBuildHeight(at) || !world.getWorldBorder().isWithinBounds(at))return false;
   snapshot.put(at.immutable(),world.getBlockState(at));
  }
  for(var at:cells)if(p.level()!=world || !footing(p,at))return false;
  if(p.level()!=world || !TidewardReadAdmission.actor(p))return false;
  for(var entry:snapshot.entrySet())if(!world.hasChunkAt(entry.getKey()) || world.getBlockState(entry.getKey())!=entry.getValue())return false;
  return true;
 }
 public static boolean survey(ServerPlayer p,UseOnContext c){
  var s=p.getAttachedOrCreate(STATE);if(s.reading)return false;s.reading=true;
  try{return surveyLeased(p,c,s);}finally{s.reading=false;}
 }
 private static boolean surveyLeased(ServerPlayer p,UseOnContext c,State s){
  var held=p.getItemInHand(c.getHand());long now=TidewardEquipment.clock(p);
  if(!held.is(ITEM) || !TidewardReadAdmission.actor(p))return false;
  if(p.isShiftKeyDown()){clear(p,s);return true;}
  if(now<p.getAttachedOrElse(READY,0L))return refuse(p,"rest");
  var floor=c.getClickedPos().immutable();if(!visibleBank(p,floor))return refuse(p,"bank");
  if(!current(p,s) || now>=s.until || s.first==null){
   clear(p,s);s.world=p.level();s.held=held;s.hand=c.getHand();s.first=floor;s.until=now+ANCHOR_LIFE;
   p.sendOverlayMessage(Component.translatable("message.wildercord.bank_line.first"));return true;
  }
  var cells=columns(s.first,floor);if(!route(p,cells) || !current(p,s))return refuse(p,"crossing");
  // Payment is committed once; callbacks cannot create an unpaid success or evade its shared rest.
  p.setAttached(READY,now+REST);p.getCooldowns().addCooldown(held,REST);held.hurtAndBreak(2,p,c.getHand().asEquipmentSlot());
  if(!current(p,s) || !route(p,cells)){clear(p,s);return false;}
  s.first=null;s.route=cells;s.until=now+ROUTE_LIFE;s.nonce=p.level().getGameTime();
  TidewardCue.route(p,s.nonce,cells);Feels.sound(p.level(),p.position(),"tideward_spool_commit",.35F,1);return true;
 }
 private static void tick(ServerPlayer p){
  var s=p.getAttached(STATE);if(s==null)return;
  if(!current(p,s) || TidewardEquipment.clock(p)>=s.until){clear(p,s);return;}
  if(!s.route.isEmpty() && p.tickCount%10==0 && !route(p,s.route))clear(p,s);
 }
 public static void init(){
  // ItemStack.useOn otherwise rejects Adventure before calling a read-only Item.useOn.
  // Fabric's native callback is item-scoped and performs no block action.
  UseBlockCallback.EVENT.register((p,l,hand,hit) -> {
   if(!p.getItemInHand(hand).is(ITEM) || p.isSpectator())return InteractionResult.PASS;
   if(p instanceof ServerPlayer server)survey(server,new UseOnContext(p,hand,hit));
   return InteractionResult.SUCCESS;
  });
  ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(TidewardSurvey::tick));
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
   var p=handler.player;long remaining=p.getAttachedOrElse(READY,0L)-TidewardEquipment.clock(p);
   if(remaining>0)p.getCooldowns().addCooldown(new ItemStack(ITEM),(int)Math.min(REST,remaining));
  });
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> o.accept(ITEM));
 }
}
