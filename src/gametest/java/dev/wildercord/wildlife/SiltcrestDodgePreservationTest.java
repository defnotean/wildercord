package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Actual unforced fish evasion; finite independent habitat trials never move fish or force a strike. */
public final class SiltcrestDodgePreservationTest implements FabricClientGameTest {
 private SiltcrestBittern bird;private Observation observed;private Admission admission;private int calls;
 private final List<WitnessCod> fish=new ArrayList<>();
 private enum Gate { FISH_BASELINE,HISTORY,STALE_HISTORY,PROGRESS,WATER_ROUTE,REMAINING_ROUTE,HEADING,INACTIVE_SAMPLE,NON_MONOTONIC,BANK,NEAREST,REACH }
 private final Map<Gate,Integer> rejections=new EnumMap<>(Gate.class);
 private record SwimSample(long tick,Vec3 center,boolean navigating){}
 private record RouteReceipt(long tick,boolean navigationDone,boolean pathPresent,boolean pathDone,boolean canReach,int next,int nodes,BlockPos requested,Vec3 end,boolean remainingWater,double remainingSqr){
  boolean activeWater(){return !navigationDone&&pathPresent&&!pathDone&&next>=0&&next<nodes&&remainingWater;}
 }
 private record Rejection(Gate gate,long tick,Vec3 body,double progressSqr,RouteReceipt route){}
 private record Admission(Cod quarry,BlockPos bank,long tick,Vec3 from,Vec3 to,RouteReceipt route){}
 private record Sample(long tick,int phase,int left,double distanceSqr){}
 private static final class Observation {
  final Cod quarry;final Vec3 committed;final int epoch;Sample first,last,max,escape;
  Observation(Cod quarry,Vec3 committed,int epoch){this.quarry=quarry;this.committed=committed;this.epoch=epoch;}
  boolean active(SiltcrestBittern b){return b.pose()==SiltcrestBittern.COILING&&field(b,"quarry")==quarry&&field(b,"committed")==committed&&(Integer)field(b,"epoch")==epoch;}
  void sample(ServerLevel l,SiltcrestBittern b){
   last=new Sample(l.getGameTime(),b.phase(),(Integer)field(b,"left"),quarry.getBoundingBox().getCenter().distanceToSqr(committed));
   if(first==null)first=last;if(max==null||last.distanceSqr()>max.distanceSqr())max=last;if(escape==null&&last.distanceSqr()>.36)escape=last;
  }
 }
 private final class WitnessCod extends Cod {
  private final ArrayDeque<SwimSample> swimming=new ArrayDeque<>();
  private final Map<Gate,Integer> rejected=new EnumMap<>(Gate.class);
  private double maxSwimProgressSqr;private int navigatingTicks,rawNavigatingTicks,partialWaterTicks;
  private Rejection lastRejected,deepestRejected;private RouteReceipt previousRoute;
  WitnessCod(ServerLevel l){super(EntityTypes.COD,l);}
  @Override public void tick(){
   super.tick();
   if(!(level() instanceof ServerLevel l))return;
   var route=routeReceipt();var priorRoute=previousRoute;previousRoute=route;
   if(bird==null){
    if(!route.navigationDone())rawNavigatingTicks++;
    boolean navigating=route.activeWater();if(navigating){navigatingTicks++;if(!route.canReach())partialWaterTicks++;}
    if(swimming.size()==9)swimming.removeFirst();swimming.addLast(new SwimSample(l.getGameTime(),getBoundingBox().getCenter(),navigating));
    if(swimming.size()==9)maxSwimProgressSqr=Math.max(maxSwimProgressSqr,horizontal(swimming.getLast().center().subtract(swimming.getFirst().center())).lengthSqr());
    return;
   }
   // Native fish movement precedes the bird's synchronous final coil/strike/cancel step.
   // Read only a still-live commitment: later movement after cancellation is never evidence.
   if(bird.level()!=l||bird.pose()!=SiltcrestBittern.COILING||!isAlive()||field(bird,"quarry")!=this)return;
   var committed=(Vec3)field(bird,"committed");if(committed==null)return;
   if(observed==null){
    observed=new Observation(this,committed,(Integer)field(bird,"epoch"));
    // This fish ticks before the bird. The new commitment is first visible here on
    // the following fish tick; report that boundary honestly, not an invented exact tick.
    System.out.println("SILTCREST_DODGE_COMMIT firstObservedCoilTick="+l.getGameTime()+" admissionToFirstCoilSampleTicks="+(l.getGameTime()-admission.tick())+" quarry="+getUUID()+" committed="+committed+" phase="+bird.phase()+" left="+field(bird,"left")+" admissionRoute="+admission.route()+" precedingFishTickRoute="+priorRoute+" firstCoilSampleRoute="+route);
   }
   if(observed.active(bird))observed.sample(l,bird);
  }
  private RouteReceipt routeReceipt(){
   var l=(ServerLevel)level();var navigation=getNavigation();var path=navigation.getPath();
   if(path==null)return new RouteReceipt(l.getGameTime(),navigation.isDone(),false,true,false,-1,0,null,null,false,Double.NaN);
   int next=path.getNextNodeIndex(),nodes=path.getNodeCount();var end=nodes==0?null:path.getEntityPosAtNode(this,nodes-1);
   boolean water=next>=0&&next<nodes;
   for(int i=next;water&&i<nodes;i++){
    var at=path.getNodePos(i);if(!l.hasChunkAt(at)){water=false;break;}var fluid=l.getFluidState(at);
    water=fluid.is(FluidTags.WATER)&&fluid.isSource()&&l.getBlockState(at).getCollisionShape(l,at).isEmpty();
   }
   // Native navigation follows nonempty partial paths even when its requested
   // destination cannot be reached. Validate the actual remaining water nodes;
   // canReach describes the requested destination and is retained as a receipt.
   return new RouteReceipt(l.getGameTime(),navigation.isDone(),true,path.isDone(),path.canReach(),next,nodes,path.getTarget(),end,water,end==null?Double.NaN:horizontal(end.subtract(getBoundingBox().getCenter())).lengthSqr());
  }
  private double progressSqr(){return swimming.size()<2?0:horizontal(swimming.getLast().center().subtract(swimming.getFirst().center())).lengthSqr();}
  private boolean reject(Gate gate){
   rejections.merge(gate,1,Integer::sum);rejected.merge(gate,1,Integer::sum);
   lastRejected=new Rejection(gate,level().getGameTime(),position(),progressSqr(),routeReceipt());
   if(deepestRejected==null||gate.ordinal()>deepestRejected.gate().ordinal()||gate==deepestRejected.gate()&&lastRejected.progressSqr()>deepestRejected.progressSqr())deepestRejected=lastRejected;
   return false;
  }
  private boolean sustainedSwim(ServerLevel l){
   if(swimming.size()!=9)return reject(Gate.HISTORY);
   var first=swimming.getFirst();var last=swimming.getLast();var progress=horizontal(last.center().subtract(first.center()));var route=routeReceipt();
   if(last.tick()<l.getGameTime()-1||last.tick()-first.tick()!=8)return reject(Gate.STALE_HISTORY);
   if(progress.lengthSqr()<.16)return reject(Gate.PROGRESS);
   if(!route.activeWater())return reject(Gate.WATER_ROUTE);
   var remaining=horizontal(route.end().subtract(last.center()));if(remaining.lengthSqr()<4)return reject(Gate.REMAINING_ROUTE);
   if(progress.dot(remaining)<=0)return reject(Gate.HEADING);
   SwimSample previous=null;
   for(var sample:swimming){
    if(!sample.navigating())return reject(Gate.INACTIVE_SAMPLE);
    if(previous!=null&&(sample.tick()!=previous.tick()+1||horizontal(sample.center().subtract(previous.center())).dot(progress)<=0))return reject(Gate.NON_MONOTONIC);
    previous=sample;
   }
   return true;
  }
  private String swimDiagnostic(){return "id="+getUUID()+" body="+position()+" health="+getHealth()+" wild="+SiltcrestBittern.wildFish(this,(ServerLevel)level())+" noAI="+isNoAi()+" follower="+isFollower()+" hasFollowers="+hasFollowers()+" rawNavigatingTicks="+rawNavigatingTicks+" waterNavigatingTicks="+navigatingTicks+" partialWaterTicks="+partialWaterTicks+" maxEightTickProgressSqr="+maxSwimProgressSqr+" first="+swimming.peekFirst()+" last="+swimming.peekLast()+" route="+routeReceipt()+" rejected="+rejected+" deepestRejected="+deepestRejected+" lastRejected="+lastRejected;}
  @Override public boolean hurtServer(ServerLevel l,DamageSource d,float amount){if(d.getEntity() instanceof SiltcrestBittern)calls++;return super.hurtServer(l,d,amount);}
 }
 private static void swimmingShore(ServerLevel l){
  floor(l);
  // Test-local six-by-one source-water channel: each swimming cell has a dry
  // cardinal bank while six blocks still permit a genuine longitudinal escape.
  // Replace every extra row from floor's original pond with physical clay.
  for(int x=1;x<=6;x++)for(int z=0;z<=3;z++)l.setBlock(new BlockPos(x,100,z),(z==0?Blocks.WATER:Blocks.CLAY).defaultBlockState(),2);
  int sources=0,closedCells=0;var banks=new ArrayList<BlockPos>();
  for(int x=0;x<=7;x++)for(int z=-1;z<=3;z++){
   var at=new BlockPos(x,100,z);var state=l.getBlockState(at);var fluid=l.getFluidState(at);
   if(x>=1&&x<=6&&z==0){
    check(state.is(Blocks.WATER)&&fluid.is(FluidTags.WATER)&&fluid.isSource()&&state.getCollisionShape(l,at).isEmpty(),"Supplied swimming channel retains six collision-free source-water cells: "+at);
    check(l.getBlockState(at.below()).is(Blocks.CLAY)&&l.getBlockState(at.above()).isAir(),"Supplied swimming channel has actual clay below and clear space above: "+at);
    var bank=firstDryBank(l,at);check(at.offset(0,1,1).equals(bank),"Native bank order selects the adjacent dry shore for every supplied water cell: "+at+" bank="+bank);
    sources++;banks.add(bank);
   }else{
    check(state.is(Blocks.CLAY)&&fluid.isEmpty()&&state.isCollisionShapeFullBlock(l,at),"Physical clay closes the channel ends, both sides and every former extra water row: "+at);
    closedCells++;
   }
  }
  check(sources==6&&closedCells==34,"Supplied channel footprint has exactly six source cells and thirty-four checked clay cells");
  System.out.println("SILTCREST_DODGE_HABITAT sources="+sources+" checkedClay="+closedCells+" waterFrom="+new BlockPos(1,100,0)+" waterTo="+new BlockPos(6,100,0)+" firstDryBanks="+banks);
 }
 private static Vec3 horizontal(Vec3 v){return new Vec3(v.x,0,v.z);}
 private static BlockPos firstDryBank(ServerLevel l,BlockPos origin){
  // These are the first four directions, in order, in the ordinary hunt's bank scan.
  // Only admit a real cardinal shore; no alternate route or goal is supplied to the hunter.
  for(var offset:new int[][]{{0,1},{1,0},{0,-1},{-1,0}})for(int y=-1;y<=1;y++){
   var at=origin.offset(offset[0],y,offset[1]);if(BitternHabitat.bank(l,at)&&l.getFluidState(at).isEmpty())return at;
  }
  return null;
 }
 private boolean admitPredator(ServerLevel l){
  if(fish.size()!=3||fish.stream().anyMatch(f->!SiltcrestBittern.wildFish(f,l)||f.getHealth()!=3||f.isNoAi())){rejections.merge(Gate.FISH_BASELINE,1,Integer::sum);return false;}
  for(var candidate:fish){
   if(!candidate.sustainedSwim(l))continue;
   var bank=firstDryBank(l,candidate.blockPosition());if(bank==null||bank.getY()!=101){candidate.reject(Gate.BANK);continue;}
   var body=new Vec3(bank.getX()+.5,bank.getY(),bank.getZ()+.5);
   var nearest=fish.stream().min(Comparator.<WitnessCod>comparingDouble(f->f.position().distanceToSqr(body)).thenComparing(f->f.getUUID().toString())).orElseThrow();
   if(nearest!=candidate){candidate.reject(Gate.NEAREST);continue;}
   if(candidate.position().distanceToSqr(body)>3.24){candidate.reject(Gate.REACH);continue;}
   // Native motion owns admission. Initial placement is the only predator position write.
   admission=new Admission(candidate,bank,l.getGameTime(),candidate.swimming.getFirst().center(),candidate.swimming.getLast().center(),candidate.routeReceipt());
   bird=bird(l,body.x,body.z);
   check(bird.blockPosition().equals(bank)&&BitternHabitat.bank(l,bank)&&l.getFluidState(bank).isEmpty(),"Initial predator stands at the real dry bank selected by the normal hunt scan");
   var pool=bird.preyPool(l);check(pool.size()==3&&pool.getFirst()==candidate,"Actual production prey ordering admits the observed swimming quarry and all three healthy wild fish");
   System.out.println("SILTCREST_DODGE_ADMISSION tick="+admission.tick()+" quarry="+candidate.getUUID()+" bank="+bank+" from="+admission.from()+" to="+admission.to()+" eightTickProgressSqr="+horizontal(admission.to().subtract(admission.from())).lengthSqr()+" route="+admission.route()+" rejected="+rejections);
   return true;
  }
  return false;
 }
 public void runTest(ClientGameTestContext c){boolean proved=false;
  for(int trial=0;trial<8&&!proved;trial++)try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{bird=null;observed=null;admission=null;calls=0;fish.clear();rejections.clear();swimmingShore(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,.5));for(int i=0;i<3;i++){var prey=new WitnessCod(s.overworld());prey.snapTo(1.5+i,100.1,.5,0,0);check(s.overworld().addFreshEntity(prey),"Native swimming witness is added to the supplied shallow shore");fish.add(prey);}});
   try{
    // Candidate fixture: await measured native swimming, never a fixed delay or a supplied impulse.
    await(c,w,500,s->admitPredator(s.overworld()),"Supplied shallow shore must establish sustained ordinary swimming before predator admission");
    await(c,w,240,s->observed!=null||calls>0,"Ordinary native stalking reaches a genuine commitment or actual catch");
    w.getServer().runOnServer(s->check(observed!=null&&observed.quarry==admission.quarry(),"Actual native hunt commits to the same fish whose own swimming admitted this trial"));
    await(c,w,24,s->observed==null||!observed.active(bird),"Original finite coil resolves after actual fish movement");
    proved=w.getServer().computeOnServer(s->refusedEscape(s.overworld()));
   }finally{int number=trial+1;w.getServer().runOnServer(s->diagnose(s.overworld(),number));}
  }
  check(proved,"At least one actually observed unforced >.6 fish escape must retain pre-damage refusal and finite failed-attempt rest");
 }
 private boolean sameCancellation(){return observed!=null&&(Integer)field(bird,"epoch")==observed.epoch+1&&field(bird,"quarry")==null&&field(bird,"committed")==null;}
 private boolean refusedEscape(ServerLevel l){
  // Use the final active sample, not a mid-coil excursion that returned before resolution.
  return observed!=null&&admission!=null&&observed.quarry==admission.quarry()&&observed.last!=null&&observed.last.distanceSqr()>.36&&sameCancellation()&&calls==0&&observed.quarry.isAlive()&&observed.quarry.getHealth()==3&&bird.isAlive()&&bird.pose()==SiltcrestBittern.IDLE&&bird.huntReady()<=l.getGameTime()+200;
 }
 private void diagnose(ServerLevel l,int trial){
  if(bird==null){System.out.println("SILTCREST_DODGE trial="+trial+" outcome=admission_timeout now="+l.getGameTime()+" calls="+calls+" rejected="+rejections+" fish="+fish.stream().map(WitnessCod::swimDiagnostic).toList());return;}
  String outcome=refusedEscape(l)?"refused_escape":observed!=null&&observed.escape!=null&&observed.last.distanceSqr()<=.36?"escaped_returned":calls>0?"catch":observed==null||observed.escape==null?"no_escape":"escape_without_refusal";
  System.out.println("SILTCREST_DODGE trial="+trial+" outcome="+outcome+" quarry="+(observed==null?"none":observed.quarry.getUUID())+" committed="+(observed==null?"none":observed.committed)+" epoch="+(observed==null?"none":observed.epoch)+" actualEpoch="+field(bird,"epoch")+" sameCancellation="+sameCancellation()+" first="+(observed==null?"none":observed.first)+" firstEscape="+(observed==null?"none":observed.escape)+" max="+(observed==null?"none":observed.max)+" final="+(observed==null?"none":observed.last)+" now="+l.getGameTime()+" pose="+bird.pose()+" phase="+bird.phase()+" calls="+calls+" alive="+(observed!=null&&observed.quarry.isAlive())+" health="+(observed==null?"none":observed.quarry.getHealth())+" rest="+(bird.huntReady()-l.getGameTime()));
 }
 private static Object field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
