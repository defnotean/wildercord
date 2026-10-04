package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import dev.wildercord.config.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Actual registered natural placement/config/cap predicate; no guaranteed natural population claim. */
public final class MossveilHabitatTest implements FabricClientGameTest{
 private static final BlockPos SITE=new BlockPos(0,30,0),CAP=SITE.east();
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");var original=w.getServer().computeOnServer(s->Config.get());
  try{
   w.getServer().runOnServer(s->{habitat(s.overworld());set(original,Map.of("enabled",true,"mossveilDormouse",true,"spawnMultiplier",100D));});c.waitTicks(15);
   w.getServer().runOnServer(s->{var l=s.overworld();System.out.println("MOSSVEIL_HABITAT_INITIAL natural="+MossveilHome.natural(l,SITE)+" placement="+rule(l,EntitySpawnReason.NATURAL)+" generation="+rule(l,EntitySpawnReason.CHUNK_GENERATION)+" thread="+s.isSameThread()+" config="+Config.get().wildlife()+" siteLight="+l.getMaxLocalRawBrightness(SITE)+" capLight="+l.getMaxLocalRawBrightness(CAP)+" sky="+l.canSeeSky(SITE)+" floor="+l.getBlockState(SITE.below())+" feet="+l.getBlockState(SITE)+" head="+l.getBlockState(SITE.above())+" roof="+l.getBlockState(SITE.above(2))+" cap="+l.getBlockState(CAP)+" capFoot="+GlowcapBlock.footing(l,CAP)+" capMoist="+GlowcapBlock.moist(l,CAP)+" capCover="+GlowcapBlock.structuralCover(l,CAP)+" capConditions="+GlowcapBlock.conditions(l,CAP)+" room="+MossveilContent.room(l,SITE));check(MossveilHome.natural(l,SITE)&&rule(l,EntitySpawnReason.NATURAL)&&!rule(l,EntitySpawnReason.CHUNK_GENERATION),"Actual main-thread ServerLevel natural predicate admits habitat; chunk generation refuses before live population queries");
    l.setBlock(SITE.below(),Blocks.STONE.defaultBlockState(),2);refuse(l,"Wrong actual floor");l.setBlock(SITE.below(),Blocks.MOSS_BLOCK.defaultBlockState(),2);
    l.setBlock(CAP,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,1),2);refuse(l,"Actual immature cap refuses");l.setBlock(CAP,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);
    l.setBlock(SITE,Blocks.WATER.defaultBlockState(),2);refuse(l,"Actual wet feet refuse");l.setBlock(SITE,Blocks.AIR.defaultBlockState(),3);
    var peers=new ArrayList<MossveilDormouse>();for(int i=0;i<3;i++){var e=MossveilContent.DORMOUSE.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Actual cap fixture");e.snapTo(-3+i,30,2.5,0,0);check(l.addFreshEntity(e),"Real full-AI local peer admitted");peers.add(e);if(i==0)check(MossveilContent.room(l,SITE),"One real peer leaves room");else check(!MossveilContent.room(l,SITE),"Two or saturated three raw actual peers refuse");}peers.forEach(Entity::discard);
    var nether=s.getLevel(Level.NETHER);habitat(nether);refuse(nether,"Otherwise genuine supported non-Overworld fungal cave refuses");check(rule(nether,EntitySpawnReason.COMMAND)&&rule(nether,EntitySpawnReason.SPAWN_ITEM_USE),"Explicit command/egg remain usable independently of natural habitat");
    for(var changes:List.<Map<String,Object>>of(Map.of("enabled",false),Map.of("mossveilDormouse",false),Map.of("spawnMultiplier",0D))){set(original,changes);refuse(l,"Actual natural config switch/multiplier refusal");}
    set(original,Map.of("enabled",true,"mossveilDormouse",true,"spawnMultiplier",100D));check(rule(l,EntitySpawnReason.NATURAL),"Configuration restoration preserves ordinary admitted branch");
   });w.getServer().runCommand("difficulty peaceful");c.waitTicks(3);w.getServer().runOnServer(s->check(rule(s.overworld(),EntitySpawnReason.NATURAL),"Neutral creature habitat remains eligible in actual Peaceful difficulty"));
   var live=w.getServer().computeOnServer(s->s.overworld());check(!live.getServer().isSameThread(),"Actual native test thread is distinct from owning server");
   for(int n=0;n<16;n++)check(!rule(live,EntitySpawnReason.NATURAL)&&!rule(live,EntitySpawnReason.CHUNK_GENERATION),"Eligible actual level refuses off-thread natural/generation before world queries");
   check(rule(live,EntitySpawnReason.COMMAND)&&rule(live,EntitySpawnReason.SPAWN_ITEM_USE),"Manual command/egg bypass remains independent of natural thread admission");
  }finally{w.getServer().runOnServer(s->current(original));}
 }}
 // Wider real roof keeps the adjacent mature cap within the same genuine darkness requirement.
 private static void habitat(ServerLevel l){for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),y==32?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}l.setBlock(CAP.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(CAP,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);}
 private static boolean rule(ServerLevel l,EntitySpawnReason reason){return SpawnPlacements.checkSpawnRules(MossveilContent.DORMOUSE,l,reason,SITE,RandomSource.create(817));}
 private static void refuse(ServerLevel l,String why){check(!rule(l,EntitySpawnReason.NATURAL)&&!rule(l,EntitySpawnReason.CHUNK_GENERATION),why);}
 private static void set(WildercordConfig original,Map<String,Object> changes){current(copy(original,Map.of("wildlife",copy(original.wildlife(),changes))));}
 @SuppressWarnings("unchecked")private static<T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();Class<?>[] types=new Class<?>[parts.length];Object[] values=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(record));}return(T)record.getClass().getDeclaredConstructor(types).newInstance(values);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig cfg){try{var field=Config.class.getDeclaredField("current");field.setAccessible(true);field.set(null,cfg);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
