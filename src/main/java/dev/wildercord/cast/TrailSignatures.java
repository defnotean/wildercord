package dev.wildercord.cast;
import dev.wildercord.mixin.ItemEntityAccessor;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.*;
import java.util.*;

/** Draft increment C: explicit owned-container transfers, willing tow and bounded paid navigation clues. */
public final class TrailSignatures {
 private TrailSignatures(){}
 private record Tow(Cast cast,ServerPlayer rider,Vec3 destination,BlockPos support,long until){}
 private record Deposit(Cast cast,ServerPlayer player,BlockPos origin,Item sample,List<BlockPos> offsets,int cursor){}
 private record Transfer(ItemEntity loose,int count,ItemStack snapshot,Vec3 position){}
 private record ChestSnapshot(ChestBlockEntity entity,net.minecraft.world.level.block.state.BlockState state){}
 private static final Map<UUID,Tow> TOWS=new HashMap<>();
 private static final Map<UUID,Deposit> DEPOSITS=new HashMap<>();
 private static final Map<UUID,UUID> CRUMBS=new HashMap<>();
 private static int probes;
 public static int active(){return TOWS.size()+DEPOSITS.size()+CRUMBS.size();}
 static int successfulProbes(){return probes;}
 public static void init(){ServerLifecycleEvents.SERVER_STOPPING.register(s -> {TOWS.clear();DEPOSITS.clear();CRUMBS.clear();probes=0;NextSignaturePayments.clear();});}
 public static boolean apply(Cast c,SpellPlan.EffectNode node,Cast.Hit hit){
  if(c.passive || !Effects.builtIn(node.effect))return false;
  switch(node.effect.path()){
   case "pocket_current" -> pocket(c,hit);
   case "wayline" -> tow(c,hit);
   case "night_seam" -> seam(c,hit);
   case "shard_compass" -> compass(c,hit);
   default -> {return false;}
  }return true;
 }
 private static void pocket(Cast c,Cast.Hit hit){
  if(!(c.caster instanceof ServerPlayer p) || hit.block()==null || hit.face()==null || !NextSignatureSafety.editable(c,hit.block())
   || !c.level.getBlockState(hit.block()).is(Blocks.CHEST)
   || !(c.level.getBlockEntity(hit.block()) instanceof ChestBlockEntity chest)
   || !PocketChestOwnership.owns(p,chest) || chest.getLootTable()!=null || !chest.canOpen(p)
   || !c.level.mayInteract(p,hit.block()))return;
  var chests=new ArrayList<ChestSnapshot>();chests.add(new ChestSnapshot(chest,c.level.getBlockState(hit.block())));
  var interaction=UseBlockCallback.EVENT.invoker().interact(p,c.level,InteractionHand.MAIN_HAND,
   new BlockHitResult(hit.point(),hit.face(),hit.block(),false));
  if(interaction==InteractionResult.FAIL || c.level.getBlockEntity(hit.block())!=chest)return;
  Container destination=chest;
  if(c.level.getBlockState(hit.block()).getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
   var otherAt=hit.block().relative(ChestBlock.getConnectedDirection(c.level.getBlockState(hit.block())));
   if(!NextSignatureSafety.editable(c,otherAt) || !c.level.mayInteract(p,otherAt)
    || !c.level.getBlockState(otherAt).is(Blocks.CHEST)
    || c.level.getBlockState(otherAt).getValue(ChestBlock.TYPE)==ChestType.SINGLE
    || !otherAt.relative(ChestBlock.getConnectedDirection(c.level.getBlockState(otherAt))).equals(hit.block())
    || !(c.level.getBlockEntity(otherAt) instanceof ChestBlockEntity other)
    || !PocketChestOwnership.owns(p,other) || other.getLootTable()!=null || !other.canOpen(p))return;
   chests.add(new ChestSnapshot(other,c.level.getBlockState(otherAt)));
   var otherUse=UseBlockCallback.EVENT.invoker().interact(p,c.level,InteractionHand.MAIN_HAND,
    new BlockHitResult(Vec3.atCenterOf(otherAt),Direction.UP,otherAt,false));
   if(otherUse==InteractionResult.FAIL || c.level.getBlockEntity(otherAt)!=other
    || c.level.getBlockEntity(hit.block())!=chest)return;
   destination=new CompoundContainer(chest,other);
  }
  var original=new ArrayList<ItemStack>();var slots=new ArrayList<ItemStack>();
  for(int i=0;i<destination.getContainerSize();i++){original.add(destination.getItem(i).copy());slots.add(destination.getItem(i).copy());}
  var candidates=c.level.getEntitiesOfClass(ItemEntity.class,new AABB(hit.point(),hit.point()).inflate(3),e -> e.isAlive() && !e.hasPickUpDelay())
   .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(hit.point()))).limit(24).toList();
  Vec3 intake=hit.point().add(new Vec3(hit.face().getStepX(),hit.face().getStepY(),hit.face().getStepZ()).scale(.12));
  var jobs=new ArrayList<Transfer>();int count=0;
  for(var loose:candidates){
   if(count>=16)break;var owner=(ItemEntityAccessor)loose;
   if(!NextSignatureRules.reservedFor(p.getUUID(),owner.wildercord$target(),
      owner.wildercord$thrower()==null?null:owner.wildercord$thrower().getUUID())
    || loose.distanceToSqr(hit.point())>9 || !NextSignatureSafety.editable(c,loose.blockPosition().below())
    || !NextSignatureSafety.open(c,loose.position().add(0,.1,0),intake,4))continue;
   var input=loose.getItem();int amount=Math.min(16-count,input.getCount());
   if(amount<=0 || FieldFusions.capacity(slots,input)<amount)continue;
   FieldFusions.insert(slots,input.copyWithCount(amount));jobs.add(new Transfer(loose,amount,input.copy(),loose.position()));count+=amount;
  }
  if(jobs.isEmpty())return;
  // Permission callbacks can replace a chest or mutate drops/slots. Validate every snapshot AFTER
  // all callbacks finish; no further permission/event calls occur between validation and commit.
  for(var saved:chests){var entity=saved.entity();var at=entity.getBlockPos();
   if(!NextSignatureSafety.loaded(c,at) || !NextSignatureSafety.loaded(c,at.above())
    || c.level.getBlockEntity(at)!=entity || !c.level.getBlockState(at).equals(saved.state())
    || dev.wildercord.world.dungeons.DungeonWards.warded(c.level,at)
    || !PocketChestOwnership.owns(p,entity) || entity.getLootTable()!=null || !entity.canOpen(p)
    || ChestBlock.isChestBlockedAt(c.level,at))return;
  }
  for(int i=0;i<original.size();i++)if(!ItemStack.matches(destination.getItem(i),original.get(i)))return;
  for(var job:jobs){var item=job.loose();var reservation=(ItemEntityAccessor)item;
   if(!item.isAlive() || item.level()!=c.level || item.hasPickUpDelay() || !item.position().equals(job.position())
    || !NextSignatureSafety.loaded(c,item.blockPosition().below())
    || dev.wildercord.world.dungeons.DungeonWards.warded(c.level,item.blockPosition().below())
    || !ItemStack.matches(item.getItem(),job.snapshot())
    || !NextSignatureRules.reservedFor(p.getUUID(),reservation.wildercord$target(),
      reservation.wildercord$thrower()==null?null:reservation.wildercord$thrower().getUUID()))return;
  }
  if(!NextSignaturePayments.of(c).once("pocket_current"))return;
  for(int i=0;i<slots.size();i++)destination.setItem(i,slots.get(i));destination.setChanged();
  for(var job:jobs){var rest=job.loose().getItem().copy();rest.shrink(job.count());
   if(rest.isEmpty())job.loose().discard();else job.loose().setItem(rest);
   TrailSignatureFx.envelope(c.level,job.loose().position(),hit.point(),job.count());}
 }
 private static void tow(Cast c,Cast.Hit hit){
  if(!(c.caster instanceof ServerPlayer p) || hit.block()==null || hit.face()==null || TOWS.size()>=128 || p.isShiftKeyDown()
   || p.getAbilities().mayfly || !NextSignatureSafety.mobile(c,p,false) || !NextSignatureSafety.editable(c,hit.block()))return;
  Vec3 destination=Vec3.atBottomCenterOf(hit.block().relative(hit.face()));
  if(p.distanceToSqr(destination)>36 || !NextSignatureSafety.body(c,p,destination,true)
   || !NextSignatureSafety.open(c,p.getEyePosition(),destination.add(0,p.getEyeHeight(),0),7)
   || !NextSignaturePayments.of(c).once("wayline"))return;
  var t=new Tow(c,p,destination,hit.block(),c.level.getGameTime()+40);TOWS.put(p.getUUID(),t);
  TrailSignatureFx.anchor(c.level,destination);
  Casters.tell(p,Component.translatableWithFallback("message.wildercord.wayline_hold","Hold forward to take the line; crouch to release"));towTick(t);
 }
 private static void towTick(Tow t){
  var c=t.cast();var p=t.rider();if(TOWS.get(p.getUUID())!=t)return;
  var delta=t.destination().subtract(p.position());double distance=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
  if(!NextSignatureSafety.mobile(c,p,false) || p.isShiftKeyDown() || c.level.getGameTime()>=t.until()
   || !NextSignatureSafety.editable(c,t.support()) || p.getAbilities().mayfly || p.hasEffect(MobEffects.LEVITATION) || distance<.2
   || !NextSignatureSafety.open(c,p.getEyePosition(),t.destination().add(0,p.getEyeHeight(),0),7)){
   TOWS.remove(p.getUUID());if(p.isAlive() && p.level()==c.level && !p.onGround())p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,40,0,false,true));return;
  }
  if(p.getLastClientInput().forward()){
   double speed=NextSignatureRules.tow(distance);var flat=new Vec3(delta.x,0,delta.z).normalize().scale(speed*.4);
   var old=p.getDeltaMovement();var lateral=new Vec3(old.x+flat.x,0,old.z+flat.z);
   if(old.x*old.x+old.z*old.z>.1024 || Math.abs(old.y)>.35){TOWS.remove(p.getUUID());return;}
   if(lateral.lengthSqr()>.1024)lateral=lateral.normalize().scale(.32);
   var velocity=new Vec3(lateral.x,Math.clamp(delta.y*.08,-.06,.07),lateral.z);
   // A single tick's safe body check precedes normal physics; no teleport/noPhysics/flight flag is used.
   if(!NextSignatureSafety.body(c,p,p.position().add(velocity),false)
    || !NextSignatureSafety.body(c,p,p.position().add(old),false)){TOWS.remove(p.getUUID());return;}
   // Effects.push ADDS its impulse. Supply the difference rather than doubling the existing velocity.
   Effects.push(p,new Vec3(NextSignatureRules.velocityImpulse(old.x,velocity.x),
    NextSignatureRules.velocityImpulse(old.y,velocity.y),NextSignatureRules.velocityImpulse(old.z,velocity.z)));
   p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,3,0,false,false));
  }
  if(c.level.getGameTime()%6==0)TrailSignatureFx.links(c.level,p.getEyePosition().add(0,-.4,0),t.destination());
  Scheduler.later(1,Effects.carryContext(() -> towTick(t)));
 }
 private static void seam(Cast c,Cast.Hit hit){
  if(!(c.caster instanceof ServerPlayer p) || hit.block()==null || hit.face()==null)return;
  Direction inward=hit.face().getOpposite();BlockPos at=hit.block();int solids=0;
  for(int i=0;i<=3;i++,at=at.relative(inward)){
   if(!NextSignatureSafety.editable(c,at) || c.level.getBlockEntity(at)!=null
    || dev.wildercord.world.dungeons.DungeonWards.warded(c.level,at))return;
   var state=c.level.getBlockState(at);
   if(state.isAir()){
    var feet=Vec3.atBottomCenterOf(at);
    if(solids==0 || !NextSignatureSafety.body(c,p,feet,true) || !NextSignaturePayments.of(c).once("night_seam"))return;
    boolean threat=c.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(feet,feet).inflate(2),
     t -> NextSignatureSafety.target(c,t,true) && t.distanceToSqr(feet)<4).stream().limit(1).findAny().isPresent();
    probes++;
    Vec3 board=hit.point().add(new Vec3(hit.face().getStepX(),hit.face().getStepY(),hit.face().getStepZ()).scale(.1));
    BlockPos passage=at.immutable();int cells=solids;
    for(int beat=0;beat<8;beat++){boolean audible=beat==0;
     Scheduler.later(beat*10,Effects.carryContext(() -> {
      if(!NextSignatureSafety.target(c,p,false) || p.distanceToSqr(board)>64 || !NextSignatureSafety.body(c,p,feet,true))return;
      for(int cell=0;cell<cells;cell++){
       var stone=hit.block().relative(inward,cell);
       if(!NextSignatureSafety.editable(c,stone) || c.level.getBlockEntity(stone)!=null
        || dev.wildercord.world.dungeons.DungeonWards.warded(c.level,stone) || !inspectionStone(c,stone))return;
      }
      if(!NextSignatureSafety.editable(c,passage) || dev.wildercord.world.dungeons.DungeonWards.warded(c.level,passage)
       || !c.level.getBlockState(passage).isAir())return;
      TrailSignatureFx.seam(c.level,board,inward,cells,threat,audible);
     }));
    }
    Casters.tell(p,Component.translatableWithFallback(threat?"message.wildercord.seam_threat":"message.wildercord.seam_open",
     threat?"Open ground beyond the seam — nearby hostile movement":"Open ground beyond the seam"));return;
   }
   // Inspection stays a narrow stone slit. No ores, special blocks or block entities become an x-ray source.
   if(i==3 || !(state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLESTONE)
    || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.TUFF)))return;
   solids++;
  }
 }
 private static boolean inspectionStone(Cast c,BlockPos at){
  var state=c.level.getBlockState(at);return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
   || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.TUFF);
 }
 private static List<BlockPos> offsets(){
  var all=new ArrayList<BlockPos>();for(int x=-8;x<=8;x++)for(int y=-8;y<=8;y++)for(int z=-8;z<=8;z++)all.add(new BlockPos(x,y,z));
  all.sort(Comparator.comparingDouble(p -> p.distSqr(BlockPos.ZERO)));return List.copyOf(all.subList(0,512));
 }
 private static final List<BlockPos> OFFSETS=offsets();
 private static Item sample(ServerPlayer p){
  for(int i=0;i<36;i++){var item=p.getInventory().getItem(i).getItem();if(item==Items.RAW_IRON || item==Items.RAW_COPPER || item==Items.RAW_GOLD)return item;}return null;
 }
 private static void compass(Cast c,Cast.Hit hit){
  if(!(c.caster instanceof ServerPlayer p) || DEPOSITS.size()+CRUMBS.size()>=128 && !DEPOSITS.containsKey(p.getUUID()) && !CRUMBS.containsKey(p.getUUID())
   || hit.block()==null || !NextSignatureSafety.editable(c,hit.block()))return;
  Item sample=sample(p);if(sample==null || !NextSignaturePayments.of(c).once("shard_compass"))return;
  CRUMBS.remove(p.getUUID());
  var d=new Deposit(c,p,hit.block(),sample,OFFSETS,0);DEPOSITS.put(p.getUUID(),d);scan(d);
 }
 private static boolean ore(Cast c,BlockPos pos,Item sample){
  var state=c.level.getBlockState(pos);
  if(sample==Items.RAW_IRON)return state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE);
  if(sample==Items.RAW_COPPER)return state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE);
  return state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE);
 }
 private static Vec3 accessible(Deposit d,BlockPos pos){
  var c=d.cast();for(var direction:Direction.values()){
   var adjacent=pos.relative(direction);if(!NextSignatureSafety.editable(c,adjacent) || !c.level.getBlockState(adjacent).isAir())continue;
   Vec3 at=Vec3.atCenterOf(adjacent);
   if(NextSignatureSafety.open(c,d.player().getEyePosition(),at,12))return at;
  }return null;
 }
 private static void scan(Deposit d){
  var c=d.cast();var p=d.player();if(DEPOSITS.get(p.getUUID())!=d)return;
  if(!c.alive() || !NextSignatureSafety.target(c,p,false)){DEPOSITS.remove(p.getUUID());return;}
  int stop=Math.min(512,d.cursor()+32);
  for(int i=d.cursor();i<stop;i++){
   var pos=d.origin().offset(d.offsets().get(i));if(!NextSignatureSafety.editable(c,pos)
    || dev.wildercord.world.dungeons.DungeonWards.warded(c.level,pos) || !ore(c,pos,d.sample()))continue;
   Vec3 endpoint=accessible(d,pos);if(endpoint==null)continue;
   int slot=-1;for(int j=0;j<36;j++)if(p.getInventory().getItem(j).is(d.sample())){slot=j;break;}
   DEPOSITS.remove(p.getUUID());if(slot<0)return;
   p.getInventory().getItem(slot).shrink(1);p.getInventory().setChanged();
   UUID token=UUID.randomUUID();CRUMBS.put(p.getUUID(),token);
   for(int beat=0;beat<6;beat++){
    Scheduler.later(beat*20,Effects.carryContext(() -> {
     if(!token.equals(CRUMBS.get(p.getUUID())))return;
     if(NextSignatureSafety.target(c,p,false) && NextSignatureSafety.editable(c,pos) && ore(c,pos,d.sample())
      && NextSignatureSafety.open(c,p.getEyePosition(),endpoint,12))TrailSignatureFx.compass(c.level,p.position().add(0,.08,0),endpoint);
     else CRUMBS.remove(p.getUUID(),token);
    }));
   }Scheduler.later(120,Effects.carryContext(() -> CRUMBS.remove(p.getUUID(),token)));return;
  }
  if(stop>=512){DEPOSITS.remove(p.getUUID());return;}
  var next=new Deposit(c,p,d.origin(),d.sample(),d.offsets(),stop);DEPOSITS.put(p.getUUID(),next);
  Scheduler.later(1,Effects.carryContext(() -> scan(next)));
 }
}
