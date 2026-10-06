package dev.wildercord.wildlife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

/** Observation of the original first harvest and stationary pickup. Never supplies items or movement. */
public final class FungalGillProbe {
 static final int MAX_ROWS=32,MAX_DROPS=8;
 private static volatile FungalGillProbe active;
 private static boolean installed;
 private final ServerLevel level;
 private final ServerPlayer player;
 private final BlockPos plant;
 private final Thread owner;
 private final Consumer<String> output;
 private final Map<UUID,ItemEntity> drops=new LinkedHashMap<>();
 private final Map<UUID,Boolean> existing=new LinkedHashMap<>();
 private final long started;
 private final int before;
 private int rows,suppressed,omittedDrops,touchEntries,touchReturns,errors;
 private long collectorStarted=-1;
 private String previous="";
 private String lastTouchEnter="none",lastTouchReturn="none";
 private volatile String client="not-observed";
 private boolean terminal,baselineResident;

 /** Implemented by a GameTest-only mixin; reads the native integer without changing it. */
 public interface ItemDelay {int wildercord$gillPickupDelay();}

 private FungalGillProbe(ServerLevel level,ServerPlayer player,BlockPos plant,Consumer<String> output){
  this.level=level;this.player=player;this.plant=plant.immutable();this.output=output;owner=Thread.currentThread();
  started=level.getGameTime();before=player.getInventory().countItem(FungalGarden.GILLS);
 }
 static void begin(ServerLevel level,ServerPlayer player,BlockPos plant){
  try{install(level,player,plant);}
  catch(Exception|AssertionError|LinkageError failure){active=null;try{dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GILL event=setup-error type={}",failure.getClass().getSimpleName());}catch(Exception|AssertionError|LinkageError ignored){}}
 }
 private static void install(ServerLevel level,ServerPlayer player,BlockPos plant){
  // Registered callbacks retain no world after finish clears active.
  if(!installed){
   installed=true;
   ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{var probe=active;if(probe!=null&&probe.level==world)probe.loaded(entity);});
   ServerTickEvents.END_SERVER_TICK.register(server->{var probe=active;if(probe!=null&&probe.level.getServer()==server)probe.tick();});
   ClientTickEvents.END_CLIENT_TICK.register(mc->{var probe=active;if(probe!=null)probe.client(mc);});
  }
  var probe=new FungalGillProbe(level,player,plant,row->dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GILL {}",row));
  active=probe;
  probe.observe(()->{
   var area=new AABB(plant).inflate(4);
   probe.baselineResident=resident(level,area);
   if(probe.baselineResident)for(var item:level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(FungalGarden.GILLS)))probe.track(item,true);
   probe.snapshot("before-harvest",false);
  });
 }
 static void collectorStarted(){var probe=active;if(probe!=null)probe.observe(()->{probe.collectorStarted=probe.level.getGameTime();probe.snapshot("collector-start",false);});}
 static void terminal(){var probe=active;if(probe!=null)probe.observe(()->{probe.terminal=true;probe.snapshot("collector-final",true);});}
 static void finish(){
  var probe=active;
  try{if(probe!=null&&!probe.terminal)probe.observe(()->probe.snapshot("aborted",true));}
  finally{active=null;}
 }
 private boolean own(){return Thread.currentThread()==owner;}
 private void loaded(Entity entity){
  if(!own()||!(entity instanceof ItemEntity item))return;
  observe(()->{if(item.getItem().is(FungalGarden.GILLS)&&new AABB(plant).inflate(4).intersects(item.getBoundingBox())){track(item,false);snapshot("entity-load",false);}});
 }
 private void track(ItemEntity item,boolean prior){
  if(drops.containsKey(item.getUUID()))return;
  if(drops.size()>=MAX_DROPS){omittedDrops++;return;}
  drops.put(item.getUUID(),item);existing.put(item.getUUID(),prior);
 }
 public static void touch(ItemEntity item,Player player,boolean returned){
  var probe=active;
  if(probe==null||!probe.own()||probe.player!=player||!probe.drops.containsKey(item.getUUID()))return;
  probe.observe(()->{
   String receipt="now="+probe.level.getGameTime()+" uuid="+item.getUUID()+" count="+item.getItem().getCount()+" delay="+((ItemDelay)item).wildercord$gillPickupDelay()+" removed="+item.isRemoved()+" inventory="+player.getInventory().countItem(FungalGarden.GILLS);
   if(returned){probe.touchReturns++;probe.lastTouchReturn=receipt;}else{probe.touchEntries++;probe.lastTouchEnter=receipt;}
   probe.snapshot(returned?"touch-return":"touch-enter",false);
  });
 }
 private void tick(){
  if(!own())return;
  observe(()->{
   String key=player.getInventory().countItem(FungalGarden.GILLS)+":"+player.blockPosition()+":"+player.onGround();
   for(var item:drops.values())key+=" "+item.getUUID()+":"+item.isRemoved()+":"+item.blockPosition()+":"+item.onGround()+":"+item.isInWater()+":"+item.hasPickUpDelay()+":"+pickupBox().intersects(item.getBoundingBox());
   boolean changed=!previous.equals(key);previous=key;
   long elapsed=level.getGameTime()-(collectorStarted<0?started:collectorStarted);
   if(changed||elapsed%5==0)snapshot(changed?"state-change":"tick",false);
  });
 }
 private void client(Minecraft mc){
  // Immutable cross-thread receipt; never reads a ServerLevel on the render thread.
  try{var p=mc.player;if(p==null||!p.getUUID().equals(player.getUUID()))return;
   client="time="+(mc.level==null?"none":mc.level.getGameTime())+" position="+p.position()+" velocity="+p.getDeltaMovement()+" body="+p.getBoundingBox()
    +" input="+p.input.keyPresses+" sentInput="+p.getLastSentInput()+" keys[up,down,left,right,jump,shift,sprint]="+mc.options.keyUp.isDown()+","+mc.options.keyDown.isDown()+","+mc.options.keyLeft.isDown()+","+mc.options.keyRight.isDown()+","+mc.options.keyJump.isDown()+","+mc.options.keyShift.isDown()+","+mc.options.keySprint.isDown()
    +" selected="+p.getInventory().getSelectedSlot()+" held="+p.getMainHandItem()+" inventory="+p.getInventory().countItem(FungalGarden.GILLS)+" screen="+(mc.gui.screen()==null?"none":mc.gui.screen().getClass().getSimpleName());
  }catch(Exception|AssertionError|LinkageError failure){client="observationError="+failure.getClass().getSimpleName();}
 }
 private AABB pickupBox(){return player.isPassenger()&&player.getVehicle()!=null&&!player.getVehicle().isRemoved()?player.getBoundingBox().minmax(player.getVehicle().getBoundingBox()).inflate(1,0,1):player.getBoundingBox().inflate(1,.5,1);}
 private void snapshot(String event,boolean last){
  if(!last&&rows>=MAX_ROWS){suppressed++;return;}
  if(!last)rows++;
  var stacks=new ArrayList<String>();for(int slot=0;slot<36;slot++)if(!player.getInventory().getItem(slot).isEmpty())stacks.add(slot+"="+player.getInventory().getItem(slot));
  var items=new ArrayList<String>();var pickup=pickupBox();
  for(var item:drops.values()){
   items.add(FungalNurseryProbe.observe(()->{
   var access=(dev.wildercord.mixin.ItemEntityAccessor)item;
   return "uuid="+item.getUUID()+" id="+item.getId()+" preexisting="+existing.get(item.getUUID())+" stack="+item.getItem()+" count="+item.getItem().getCount()+" age="+item.getAge()
    +" pickupDelay="+((ItemDelay)item).wildercord$gillPickupDelay()+" target="+access.wildercord$target()+" thrower="+(access.wildercord$thrower()==null?"none":access.wildercord$thrower().getUUID())
    +" position="+item.position()+" velocity="+item.getDeltaMovement()+" body="+item.getBoundingBox()+" ground="+item.onGround()+" water="+item.isInWater()+" removed="+item.isRemoved()+" reason="+item.getRemovalReason()
    +" pickupOverlap="+pickup.intersects(item.getBoundingBox())+" horizontalCollision="+item.horizontalCollision+" verticalCollision="+item.verticalCollision;
   }));
  }
  String row="event="+event+" now="+level.getGameTime()+" elapsed="+(level.getGameTime()-started)+" collectorElapsed="+(collectorStarted<0?"not-started":level.getGameTime()-collectorStarted)
   +" before="+before+" actualCount="+player.getInventory().countItem(FungalGarden.GILLS)+" baselineResident="+baselineResident+" blockDrops="+level.getGameRules().get(GameRules.BLOCK_DROPS)
   +" player="+player.getUUID()+" position="+player.position()+" velocity="+player.getDeltaMovement()+" body="+player.getBoundingBox()+" pickupBox="+pickup+" ground="+player.onGround()+" water="+player.isInWater()
   +" horizontalCollision="+player.horizontalCollision+" verticalCollision="+player.verticalCollision+" mode="+player.gameMode.getGameModeForPlayer()+" alive="+player.isAlive()+" spectator="+player.isSpectator()+" sameLevel="+(player.level()==level)
   +" selected="+player.getInventory().getSelectedSlot()+" freeSlot="+player.getInventory().getFreeSlot()+" inventory="+stacks+" drops="+items+" touchEntries="+touchEntries+" touchReturns="+touchReturns
   +" lastTouchEnter={"+lastTouchEnter+"} lastTouchReturn={"+lastTouchReturn+"} budget="+rows+"/"+MAX_ROWS+" suppressed="+suppressed+" omittedDrops="+omittedDrops+" errors="+errors+" client={"+client+"} geometry={"+FungalNurseryProbe.observe(this::geometry)+"}";
  output.accept(row);
 }
 private String geometry(){
  var area=player.getBoundingBox().minmax(new AABB(plant)).inflate(1,1.25,1);
  for(var item:drops.values())if(!item.isRemoved())area=area.minmax(item.getBoundingBox().inflate(1,1.25,1));
  if(area.getXsize()>12||area.getYsize()>8||area.getZsize()>12)return "unavailable-outside-bounds";
  if(!resident(level,area))return "unavailable-not-resident";
  var critical=new ArrayList<String>();var ordinary=new ArrayList<String>();int relevant=0;boolean playerOverlap=false;
  var body=player.getBoundingBox();var support=new AABB(body.minX,body.minY-.025,body.minZ,body.maxX,body.minY+.00001,body.maxZ);
  for(var at:BlockPos.betweenClosed(BlockPos.containing(area.minX,area.minY,area.minZ),BlockPos.containing(Math.nextDown(area.maxX),Math.nextDown(area.maxY),Math.nextDown(area.maxZ)))){
   var chunk=level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);if(chunk==null)return "unavailable-residency-changed";
   var state=chunk.getBlockState(at);var shape=state.getCollisionShape(level,at);var fluid=state.getFluidState();
   if(shape.isEmpty()&&fluid.isEmpty())continue;relevant++;
   var boxes=shape.toAabbs().stream().map(box->box.move(at.getX(),at.getY(),at.getZ())).toList();
   boolean overlap=boxes.stream().anyMatch(body::intersects),footing=boxes.stream().anyMatch(support::intersects);playerOverlap|=overlap;
   var itemContacts=new ArrayList<String>();
   for(var item:drops.values())if(!item.isRemoved()){
    var b=item.getBoundingBox();var base=new AABB(b.minX,b.minY-.025,b.minZ,b.maxX,b.minY+.00001,b.maxZ);
    boolean itemOverlap=boxes.stream().anyMatch(b::intersects),itemSupport=boxes.stream().anyMatch(base::intersects);
    if(itemOverlap||itemSupport||(!fluid.isEmpty()&&new AABB(at).intersects(b)))itemContacts.add(item.getUUID()+" overlap="+itemOverlap+" support="+itemSupport+" fluidCellOverlap="+(!fluid.isEmpty()&&new AABB(at).intersects(b)));
   }
   var destination=overlap||footing||!fluid.isEmpty()||!itemContacts.isEmpty()?critical:ordinary;
   if(destination.size()<20)destination.add(at.immutable()+"="+state+" fluid="+fluid+" boxes="+boxes+" playerOverlap="+overlap+" playerSupport="+footing+" items="+itemContacts);
  }
  return "area="+area+" playerBlockOverlap="+playerOverlap+" relevantCells="+relevant+" omitted="+(relevant-critical.size()-ordinary.size())+" contactCells="+critical+" surroundingCells="+ordinary;
 }
 private static boolean resident(ServerLevel level,AABB area){
  int minX=Math.floorDiv((int)Math.floor(area.minX)-1,16),maxX=Math.floorDiv((int)Math.ceil(area.maxX)+1,16),minZ=Math.floorDiv((int)Math.floor(area.minZ)-1,16),maxZ=Math.floorDiv((int)Math.ceil(area.maxZ)+1,16);
  if((long)(maxX-minX+1)*(maxZ-minZ+1)>16)return false;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
  return true;
 }
 private void observe(Runnable observation){
  try{observation.run();}
  catch(Exception|AssertionError|LinkageError failure){errors++;try{if(rows<MAX_ROWS){rows++;output.accept("event=observation-error type="+failure.getClass().getSimpleName()+" errors="+errors);}}catch(Exception|AssertionError|LinkageError ignored){}}
 }
}
