package dev.wildercord.cast;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static dev.wildercord.cast.NextSignatureNative.*;

/** Native acceptance. Real placement, four paid casts, reservation/atomicity and full restart. */
public final class NextTrailTest implements FabricClientGameTest {
 private static final BlockPos CHEST=new BlockPos(0,101,3),ORE=new BlockPos(2,100,2);
 private static ItemEntity foreign,delayed,loose;
 private static Vec3 towStart;
 private static final AtomicBoolean seamVeto=new AtomicBoolean(false);
 public void runTest(ClientGameTestContext c){
  TestWorldSave save;
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");
   w.getServer().runOnServer(s->{var p=player(s);floor(p);placeChest(p,CHEST);
    check(PocketChestOwnership.owns(p,chest(p,CHEST)),"Actual chest item placement assigns persisted local owner");
    loose=drop(p,Items.DIAMOND,20,new Vec3(.5,101,2.2));
    foreign=drop(p,Items.GOLD_INGOT,3,new Vec3(1.2,101,2.2));foreign.setTarget(UUID.randomUUID());
    delayed=drop(p,Items.IRON_INGOT,3,new Vec3(-.2,101,2.2));delayed.setPickUpDelay(100);
    pose(p,.5,.5,25);cast(p,Runes.TOUCH,Runes.POCKET_CURRENT);
   });c.waitTicks(14);
   w.getServer().runOnServer(s->{var p=player(s);check(count(chest(p,CHEST),Items.DIAMOND)==16 && loose.getItem().getCount()==4,"Paid chest intake transfers at most sixteen actual units");
    check(foreign.isAlive() && foreign.getItem().getCount()==3 && delayed.isAlive() && delayed.getItem().getCount()==3,"Foreign target and pickup reservation stay outside transaction");
    foreign.discard();delayed.discard();loose.discard();pocketGuards(p);
    p.level().setBlockAndUpdate(CHEST,Blocks.AIR.defaultBlockState());pose(p,.5,.5,35);towStart=p.position();cast(p,Runes.BEAM,Runes.WAYLINE);
   });c.waitTicks(12);
   w.getServer().runOnServer(s->check(TrailSignatures.active()==1 && player(s).position().distanceToSqr(towStart)<.04,"Paid line waits for voluntary forward input"));
   c.getInput().holdKey(o->o.keyUp);c.waitTicks(12);c.getInput().releaseKey(o->o.keyUp);
   w.getServer().runOnServer(s->{var p=player(s);check(p.getZ()>towStart.z+.2 && !p.getAbilities().mayfly && !p.getAbilities().flying,"Real forward input uses bounded ordinary physics without flight flags");p.setShiftKeyDown(true);});c.waitTicks(3);
   w.getServer().runOnServer(s->{var p=player(s);check(TrailSignatures.active()==0,"Crouch cancels tow");p.setShiftKeyDown(false);p.setDeltaMovement(Vec3.ZERO);pose(p,.5,.5,35);
    at(p,new Cast(p),Runes.WAYLINE,new BlockPos(0,100,4),Direction.UP,new Vec3(.5,101,4.5));check(TrailSignatures.active()==1,"Fresh line fixture is active before external lift");p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION,200,1));
   });c.waitTicks(3);
   w.getServer().runOnServer(s->{var p=player(s);check(TrailSignatures.active()==0 && p.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION).getDuration()>190,"External lift ends tow and preserves its full independent effect");p.removeAllEffects();p.setDeltaMovement(Vec3.ZERO);pose(p,.5,.5,25);
    seamVeto.set(false);net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,owner,pos,state,entity)->!seamVeto.get() || !pos.equals(new BlockPos(0,101,4)));
    for(var q:BlockPos.betweenClosed(new BlockPos(0,101,3),new BlockPos(0,102,5)))p.level().setBlockAndUpdate(q,Blocks.STONE.defaultBlockState());
    cast(p,Runes.BEAM,Runes.NIGHT_SEAM);
   });c.waitTicks(14);
   c.runOnClient(mc->check(seamPieces(mc)>0,"Actual paid accepted seam emits its near-face physical inspection stitches"));
   w.getServer().runOnServer(s->seamVeto.set(true));c.runOnClient(mc->mc.particleEngine.clearParticles());c.waitTicks(14);
   c.runOnClient(mc->check(seamPieces(mc)==0,"New intermediate-cell claim stops later production seam stitches during the existing four-second probe"));
   w.getServer().runOnServer(s->{var p=player(s);check(p.position().distanceToSqr(new Vec3(.5,101,.5))<.04,"Claimed inspection never transports the player through its wall");
    for(var cell:BlockPos.betweenClosed(new BlockPos(0,101,3),new BlockPos(0,102,5)))check(p.level().getBlockState(cell).is(Blocks.STONE),"Claimed inspection preserves every actual wall cell");seamVeto.set(false);});c.waitTicks(12);
   c.runOnClient(mc->check(seamPieces(mc)>0,"Still-live permitted probe resumes actual short inspection stitches after claim release"));
   w.getServer().runOnServer(s->{var p=player(s);check(TrailSignatures.successfulProbes()==1,"Actual paid seam accepts three ordinary owned stone cells");
    for(int z=3;z<=5;z++)check(p.level().getBlockState(new BlockPos(0,101,z)).is(Blocks.STONE),"Inspection never edits its wall");
    p.level().setBlockAndUpdate(new BlockPos(0,101,6),Blocks.STONE.defaultBlockState());probe(p,new Cast(p));check(TrailSignatures.successfulProbes()==1,"Four stone cells refuse without distant reveal");
    p.level().setBlockAndUpdate(new BlockPos(0,101,6),Blocks.AIR.defaultBlockState());p.level().setBlockAndUpdate(new BlockPos(0,101,4),Blocks.CHEST.defaultBlockState());probe(p,new Cast(p));check(TrailSignatures.successfulProbes()==1,"Block entities cannot become inspection sources");
    p.level().setBlockAndUpdate(new BlockPos(0,101,4),Blocks.STONE.defaultBlockState());var wallVeto=new AtomicBoolean(true);
    net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,owner,pos,state,entity)->!wallVeto.get() || !pos.equals(new BlockPos(0,101,4)));
    try{probe(p,new Cast(p));check(TrailSignatures.successfulProbes()==1,"One protected intermediate cell prevents the entire passage clue");}finally{wallVeto.set(false);}
    for(var q:BlockPos.betweenClosed(new BlockPos(0,101,3),new BlockPos(0,102,6)))p.level().setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
    p.getInventory().setItem(0,new ItemStack(Items.RAW_IRON,2));p.level().setBlockAndUpdate(ORE,Blocks.IRON_ORE.defaultBlockState());pose(p,.5,.5,50);cast(p,Runes.BEAM,Runes.SHARD_COMPASS);
   });c.waitTicks(30);
   w.getServer().runOnServer(s->{var p=player(s);check(inventory(p,Items.RAW_IRON)==1 && p.level().getBlockState(ORE).is(Blocks.IRON_ORE),"Actual eligible matching route consumes one carried sample without mining");
    check(TrailSignatures.active()==1,"Compass keeps one finite breadcrumb generation");
    p.level().setBlockAndUpdate(ORE,Blocks.STONE_BRICKS.defaultBlockState());locate(p,new Cast(p));
   });c.waitTicks(20);
   w.getServer().runOnServer(s->{var p=player(s);check(inventory(p,Items.RAW_IRON)==1 && TrailSignatures.active()==0,"Failed replacement preserves sample and cancels prior crumbs");
    placeChest(p,CHEST);check(inventory(p,Items.RAW_IRON)==1,"Actual chest placement preserves the remaining carried Compass sample");
    loose=drop(p,Items.DIAMOND,20,new Vec3(.5,101,2.2));pose(p,.5,.5,25);cast(p,Runes.TOUCH,Runes.POCKET_CURRENT);
   });c.waitTicks(14);
   w.getServer().runOnServer(s->{var p=player(s);check(count(chest(p,CHEST),Items.DIAMOND)==16 && loose.isAlive() && loose.getItem().getCount()==4,"Actual final paid Pocket transfer commits sixteen chest units and leaves four loose units before save");
    loose.discard();p.level().setBlockAndUpdate(ORE,Blocks.IRON_ORE.defaultBlockState());pose(p,.5,.5,50);cast(p,Runes.BEAM,Runes.SHARD_COMPASS);
   });c.waitTicks(20);
   w.getServer().runOnServer(s->check(TrailSignatures.active()==1,"Active finite locator exists before complete server shutdown"));save=w.getWorldSave();
  }finally{seamVeto.set(false);c.getInput().releaseKey(o->o.keyUp);}
  try(var reopened=save.open()){c.waitTicks(20);reopened.getServer().runOnServer(s->{var p=player(s);check(TrailSignatures.active()==0 && TrailSignatures.successfulProbes()==0,"All trail watchers/callback generations clear across full server reopen");
   check(PocketChestOwnership.owns(p,chest(p,CHEST)) && count(chest(p,CHEST),Items.DIAMOND)==16,"Actual chest-local ownership and transferred contents persist across reopen");});}
 }
 private static void pocketGuards(ServerPlayer p){
  var destination=chest(p,CHEST);var payment=new Cast(p);var goods=drop(p,Items.DIAMOND,32,new Vec3(.5,101,2.2));
  pocket(p,payment,CHEST);pocket(p,payment.pulse(),CHEST);check(goods.getItem().getCount()==16,"Repeating copies share sixteen-unit intake budget");goods.discard();
  for(int i=0;i<27;i++)destination.setItem(i,new ItemStack(Items.STONE,64));goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));pocket(p,new Cast(p),CHEST);
  check(goods.getItem().getCount()==3 && count(destination,Items.DIAMOND)==0,"Full container leaves all inputs intact");goods.discard();destination.clearContent();
  var veto=new AtomicBoolean(true);UseBlockCallback.EVENT.register((player,level,hand,hit)->veto.get() && hit.getBlockPos().equals(CHEST)?InteractionResult.FAIL:InteractionResult.PASS);
  goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));try{pocket(p,new Cast(p),CHEST);check(goods.getItem().getCount()==3 && destination.isEmpty(),"Container interaction veto refuses transaction");}finally{veto.set(false);}goods.discard();
  p.level().setBlockAndUpdate(CHEST,Blocks.AIR.defaultBlockState());p.level().setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());
  check(!PocketChestOwnership.owns(p,chest(p,CHEST)),"Replacement without player placement cannot inherit provenance");goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));pocket(p,new Cast(p),CHEST);check(goods.getItem().getCount()==3,"Existing untracked chest preserves loose input");goods.discard();
  p.level().setBlockAndUpdate(CHEST,Blocks.AIR.defaultBlockState());placeChest(p,CHEST);
  var partner=CHEST.east();placeChest(p,partner);check(p.level().getBlockState(CHEST).getValue(ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE,"Two actual owned placements merge into a double chest");
  chest(p,partner).setAttached(PocketChestOwnership.OWNER,UUID.randomUUID().toString());goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));pocket(p,new Cast(p),CHEST);
  check(goods.getItem().getCount()==3 && chest(p,CHEST).isEmpty() && chest(p,partner).isEmpty(),"Foreign second half rejects entire joined transaction");goods.discard();p.level().setBlockAndUpdate(partner,Blocks.AIR.defaultBlockState());
  callbackTransactions(p);
 }
 private static void callbackTransactions(ServerPlayer p){
  var goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));var mutation=new java.util.concurrent.atomic.AtomicInteger(1);
  net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,owner,pos,state,entity)->{
   if(!pos.equals(new BlockPos(0,100,2)))return true;
   int mode=mutation.getAndSet(0);
   if(mode==1){level.setBlockAndUpdate(CHEST,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());}
   if(mode==2)chest(p,CHEST).setItem(0,new ItemStack(Items.GOLD_INGOT));return true;
  });
  try{pocket(p,new Cast(p),CHEST);check(goods.getItem().getCount()==3 && chest(p,CHEST).isEmpty(),"A chest replaced during a later drop permission callback cannot receive stale transaction/input deletion");}
  finally{mutation.set(0);}goods.discard();p.level().setBlockAndUpdate(CHEST,Blocks.AIR.defaultBlockState());placeChest(p,CHEST);
  goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));mutation.set(2);
  try{pocket(p,new Cast(p),CHEST);check(goods.getItem().getCount()==3 && count(chest(p,CHEST),Items.GOLD_INGOT)==1,"A callback slot change is preserved rather than overwritten by a stale simulated inventory");}
  finally{mutation.set(0);}goods.discard();chest(p,CHEST).clearContent();
  p.level().setBlockAndUpdate(CHEST.above(),Blocks.STONE.defaultBlockState());goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));pocket(p,new Cast(p),CHEST);
  check(goods.getItem().getCount()==3 && chest(p,CHEST).isEmpty(),"A physically blocked chest refuses magical intake");goods.discard();p.level().setBlockAndUpdate(CHEST.above(),Blocks.AIR.defaultBlockState());
  var stranger=guest(p,"PickupThrower13",false);goods=drop(p,Items.DIAMOND,3,new Vec3(.5,101,2.2));goods.setTarget(p.getUUID());goods.setThrower(stranger);stranger.discard();pocket(p,new Cast(p),CHEST);
  check(goods.getItem().getCount()==3 && chest(p,CHEST).isEmpty(),"Matching pickup target cannot defeat the foreign thrower's reservation");goods.discard();
 }
 private static void placeChest(ServerPlayer p,BlockPos at){var held=p.getMainHandItem().copy();
  try{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CHEST));
   var floor=at.below();p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false)));
   check(p.level().getBlockEntity(at) instanceof ChestBlockEntity,"Actual chest item creates block entity");
  }finally{p.setItemInHand(InteractionHand.MAIN_HAND,held);}
 }
 private static ChestBlockEntity chest(ServerPlayer p,BlockPos at){return (ChestBlockEntity)p.level().getBlockEntity(at);}
 private static int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
 private static int inventory(ServerPlayer p,Item item){return count(p.getInventory(),item);}
 private static ItemEntity drop(ServerPlayer p,Item item,int units,Vec3 at){var e=new ItemEntity(p.level(),at.x,at.y,at.z,new ItemStack(item,units));e.setDeltaMovement(Vec3.ZERO);e.setNoGravity(true);p.level().addFreshEntity(e);return e;}
 private static void at(ServerPlayer p,Cast c,RuneDef rune,BlockPos block,Direction face,Vec3 point){var node=SpellCompiler.compile(List.of(Runes.SELF,rune)).root().groups.getFirst().effects.getFirst();Effects.apply(c,node,new Cast.Hit(List.of(p),point,new Vec3(0,0,1),p.position(),block,face,true));}
 private static void pocket(ServerPlayer p,Cast c,BlockPos block){at(p,c,Runes.POCKET_CURRENT,block,Direction.NORTH,new Vec3(block.getX()+.5,block.getY()+.5,block.getZ()));}
 private static void probe(ServerPlayer p,Cast c){at(p,c,Runes.NIGHT_SEAM,new BlockPos(0,101,3),Direction.NORTH,new Vec3(.5,101.5,3));}
 private static void locate(ServerPlayer p,Cast c){at(p,c,Runes.SHARD_COMPASS,new BlockPos(0,100,2),Direction.UP,new Vec3(.5,101,2.5));}
 private static int seamPieces(net.minecraft.client.Minecraft mc){int found=0;var near=new Vec3(.5,101.6,2.9);
  for(var particle:nativeParticles(mc.particleEngine))if(particle.isAlive() && particle instanceof dev.wildercord.client.fx.VoidParticle
   && ((Number)nativeField(particle,net.minecraft.client.particle.Particle.class,"lifetime")).intValue()==12){
   var option=(dev.wildercord.content.VoidOption)nativeField(particle,dev.wildercord.client.fx.VoidParticle.class,"material");
   var at=new Vec3(((Number)nativeField(particle,net.minecraft.client.particle.Particle.class,"x")).doubleValue(),((Number)nativeField(particle,net.minecraft.client.particle.Particle.class,"y")).doubleValue(),((Number)nativeField(particle,net.minecraft.client.particle.Particle.class,"z")).doubleValue());
   if(option.style()==dev.wildercord.content.VoidOption.CLOTH && at.distanceTo(near)<.8)found++;
  }return found;
 }
 private static Object nativeField(Object value,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static List<net.minecraft.client.particle.Particle> nativeParticles(net.minecraft.client.particle.ParticleEngine engine){var list=new ArrayList<net.minecraft.client.particle.Particle>();
  for(var group:((Map<?,?>)nativeField(engine,net.minecraft.client.particle.ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)nativeField(group,net.minecraft.client.particle.ParticleGroup.class,"particles"))list.add((net.minecraft.client.particle.Particle)p);
  for(var p:(Queue<?>)nativeField(engine,net.minecraft.client.particle.ParticleEngine.class,"particlesToAdd"))list.add((net.minecraft.client.particle.Particle)p);return list;
 }
}
