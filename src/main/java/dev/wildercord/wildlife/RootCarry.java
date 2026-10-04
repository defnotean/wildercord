package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Casters;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Two separate ordinary mana payments relocate one exact young fern; no plant or item escrow. */
public final class RootCarry {
 private RootCarry(){}
 public static final int SELECT_TICKS=300,REST_TICKS=400;
 public enum Result{REFUSED,SELECTED,MOVED}
 public record Selection(ResourceKey<Level> dimension,BlockPos at,BlockState state,long until){
  public Selection{at=at.immutable();if(state==null||dimension==null||until<0)throw new IllegalArgumentException("Invalid selected root");}
 }
 public static final AttachmentType<Selection> SELECT=AttachmentRegistry.create(Wildercord.id("root_carry_selection"),b->{});
 public static final AttachmentType<Long> READY=AttachmentRegistry.create(Wildercord.id("root_carry_ready"),b->b.initializer(()->0L).persistent(Codec.LONG).copyOnDeath());
 private static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 private static boolean actor(Cast c){return c.alive()&&!c.passive&&c.caster instanceof ServerPlayer p&&!p.isCreative()&&!p.isSpectator()&&p.level()==c.level&&Casters.mayBuild(p);}
 private static boolean cell(Cast c,BlockPos p){return actor(c)&&c.level.hasChunkAt(p)&&c.level.getWorldBorder().isWithinBounds(p)&&c.caster.getEyePosition().distanceToSqr(point(p))<=36&&c.level.mayInteract((ServerPlayer)c.caster,p)&&!dev.wildercord.cast.Effects.isTemporary(c.level,p)&&c.level.getBlockEntity(p)==null&&actor(c)&&c.caster.getEyePosition().distanceToSqr(point(p))<=36&&visible(c,p);}
 private static Vec3 point(BlockPos p){return new Vec3(p.getX()+.5,p.getY()+.15,p.getZ()+.5);}
 private static boolean visible(Cast c,BlockPos p){var hit=c.level.clip(new ClipContext(c.caster.getEyePosition(),point(p),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,c.caster));return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(p);}
 private static boolean young(BlockState s){return s.is(EmberContent.FERN)&&s.getValue(CinderFernBlock.AGE)==0;}
 private static boolean source(Cast c,Selection selected){return selected!=null&&selected.dimension().equals(c.level.dimension())&&now((ServerPlayer)c.caster)<selected.until()&&cell(c,selected.at())&&c.level.getBlockState(selected.at()).equals(selected.state())&&young(selected.state())&&selected.state().canSurvive(c.level,selected.at());}
 public static Result apply(Cast cast,Cast.Hit hit){return apply(cast,hit,EmberPlantAdmission.WORLD);}
 private static boolean claim(Cast c,BlockPos at){return Casters.mayEdit(c.caster,c.level,at);}
 private static final ThreadLocal<java.util.Set<ServerPlayer>> OPERATING=ThreadLocal.withInitial(()->java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
 /** Scope prevents a synchronous claim callback selecting or moving a different cell for this player. */
 private static final class Operation implements AutoCloseable {
  private final ServerPlayer player;private Operation(ServerPlayer p){player=p;}
  static Operation open(ServerPlayer p){var players=OPERATING.get();if(players.size()>=8||!players.add(p))return null;return new Operation(p);}
  public void close(){var players=OPERATING.get();players.remove(player);if(players.isEmpty())OPERATING.remove();}
 }
 /** Real-world writer seam exists solely for admission/rollback fault acceptance. */
 static Result apply(Cast cast,Cast.Hit hit,EmberPlantAdmission.Writer writer){
  if(!actor(cast)||hit.block()==null)return Result.REFUSED;var p=(ServerPlayer)cast.caster;
  try(var operation=Operation.open(p)){return operation==null?Result.REFUSED:operate(cast,hit,writer,p);}
 }
 private static boolean body(Cast c,Vec3 original,long ready){return actor(c)&&c.caster.position().equals(original)&&((ServerPlayer)c.caster).getAttachedOrElse(READY,0L)==ready;}
 /** Callback-free snapshot across both cells, both footings and the authoritative selection identity. */
 private static boolean whole(Cast c,BlockPos from,BlockPos to,BlockState fromState,BlockState toState,BlockState fromFloor,BlockState toFloor,Selection mark,Vec3 original,long ready){
  var p=(ServerPlayer)c.caster;
  return body(c,original,ready)&&p.getAttached(SELECT)==mark&&cell(c,from)&&cell(c,to)
   &&c.level.getBlockState(from).equals(fromState)&&c.level.getBlockState(to).equals(toState)
   &&c.level.getBlockState(from.below()).equals(fromFloor)&&c.level.getBlockState(to.below()).equals(toFloor)
   &&c.level.getBlockEntity(from.below())==null&&c.level.getBlockEntity(to.below())==null;
 }
 private static Result operate(Cast cast,Cast.Hit hit,EmberPlantAdmission.Writer writer,ServerPlayer p){
  long ready=p.getAttachedOrElse(READY,0L);var original=p.position();
  if(now(p)<ready||!cast.once("root_carry_operation"))return Result.REFUSED;
  var selected=p.getAttached(SELECT);if(selected!=null&&!source(cast,selected)){if(p.getAttached(SELECT)==selected)p.removeAttached(SELECT);return Result.REFUSED;}
  if(selected==null){var at=hit.block();if(!cell(cast,at))return Result.REFUSED;var state=cast.level.getBlockState(at);var floor=cast.level.getBlockState(at.below());if(!young(state)||!state.canSurvive(cast.level,at)||cast.level.getBlockEntity(at.below())!=null)return Result.REFUSED;
   try(var lease=EmberPlantAdmission.open(cast.level,at)){
    if(lease==null||!claim(cast,at)||!body(cast,original,ready)||p.getAttached(SELECT)!=null||!cell(cast,at)||!cast.level.getBlockState(at).equals(state)||!cast.level.getBlockState(at.below()).equals(floor)||cast.level.getBlockEntity(at.below())!=null||!state.canSurvive(cast.level,at))return Result.REFUSED;
    p.setAttached(SELECT,new Selection(cast.level.dimension(),at,state,now(p)+SELECT_TICKS));RootCarryFx.send(p,at,at,false);return Result.SELECTED;
   }
  }
  if(hit.face()!=net.minecraft.core.Direction.UP)return Result.REFUSED;var to=hit.block().above();var from=selected.at();
  if(from.equals(to)||from.distSqr(to)>64||!cell(cast,to)||!cast.level.getBlockState(to).isAir()||!cast.level.getFluidState(to).isEmpty()||!selected.state().canSurvive(cast.level,to))return Result.REFUSED;
  var old=selected.state();var empty=cast.level.getBlockState(to);var fromFloor=cast.level.getBlockState(from.below());var toFloor=cast.level.getBlockState(to.below());
  try(var a=EmberPlantAdmission.open(cast.level,from);var b=EmberPlantAdmission.open(cast.level,to)){
   if(a==null||b==null||!claim(cast,from)||!claim(cast,to)||!source(cast,selected)||!whole(cast,from,to,old,empty,fromFloor,toFloor,selected,original,ready)||!old.canSurvive(cast.level,to)||!cast.takeBlocks(2))return Result.REFUSED;
   p.removeAttached(SELECT);boolean success=false,destinationAttempted=false;var air=Blocks.AIR.defaultBlockState();
   try{
    if(!writer.set(cast.level,from,air)||!whole(cast,from,to,air,empty,fromFloor,toFloor,null,original,ready))return Result.REFUSED;
    if(!claim(cast,from)||!claim(cast,to)||!whole(cast,from,to,air,empty,fromFloor,toFloor,null,original,ready)||!old.canSurvive(cast.level,to))return Result.REFUSED;
    destinationAttempted=true;
    if(!writer.set(cast.level,to,old)||!whole(cast,from,to,air,old,fromFloor,toFloor,null,original,ready)||!old.canSurvive(cast.level,to))return Result.REFUSED;
    if(!claim(cast,from)||!claim(cast,to)||!whole(cast,from,to,air,old,fromFloor,toFloor,null,original,ready)||!old.canSurvive(cast.level,to))return Result.REFUSED;
    success=true;p.setAttached(READY,now(p)+REST_TICKS);RootCarryFx.send(p,from,to,true);return Result.MOVED;
   }finally{if(!success)rollback(cast,from,to,old,destinationAttempted);}
  }
 }
 /** Revert only our exact retained writes; never replace a callback's foreign block/state. */
 private static void rollback(Cast c,BlockPos from,BlockPos to,BlockState old,boolean destinationAttempted){
  if(destinationAttempted&&c.level.hasChunkAt(to)&&c.level.getBlockState(to).equals(old))c.level.setBlock(to,Blocks.AIR.defaultBlockState(),2);
  // If destination removal failed, do not manufacture a second root at the source.
  if(c.level.hasChunkAt(from)&&c.level.hasChunkAt(to)&&!c.level.getBlockState(to).equals(old)&&c.level.getBlockState(from).isAir()&&old.canSurvive(c.level,from))c.level.setBlock(from,old,2);
 }
 public static void init(){
  RootCarryFx.init();ServerPlayConnectionEvents.JOIN.register((h,sender,s)->h.player.removeAttached(SELECT));ServerPlayConnectionEvents.DISCONNECT.register((h,s)->h.player.removeAttached(SELECT));
  ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers()){var mark=p.getAttached(SELECT);if(mark!=null&&(!p.isAlive()||p.isRemoved()||p.isSpectator()||!p.level().dimension().equals(mark.dimension())||now(p)>=mark.until()||!p.level().hasChunkAt(mark.at())||p.getEyePosition().distanceToSqr(point(mark.at()))>36))p.removeAttached(SELECT);}});
 }
}
