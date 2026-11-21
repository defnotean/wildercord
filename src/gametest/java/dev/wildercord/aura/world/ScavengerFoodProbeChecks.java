package dev.wildercord.aura.world;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/** Checks passive forwarding and scope isolation without creating a second scavenger encounter. */
public final class ScavengerFoodProbeChecks {
 private ScavengerFoodProbeChecks(){}
 public static void main(String[] args){verify();System.out.println("ScavengerFoodProbeChecks passed");}
 public static void verify(){scope();forwarding();reentry();path();}
 private static void scope(){
  var world=new Object();var actor=new Object();var food=new Object();var owner=Thread.currentThread();var rows=new ArrayList<String>();
  var outer=ScavengerFoodProbe.install(ScavengerFoodProbe.SUITE,world,actor,food,owner,rows::add);
  try{
   check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,world,actor,food,owner)==outer,"exact fixture admitted");
   check(ScavengerFoodProbe.selected("other",world,actor,food,owner)==null,"other suite refused");
   check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,new Object(),actor,food,owner)==null,"other world refused");
   check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,world,new Object(),food,owner)==null,"other actor refused");
   check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,world,actor,new Object(),owner)==null,"other food refused");
   check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,world,actor,food,new Thread())==null,"other thread refused");
   var marker=new IllegalStateException("scope failure");
   try{try(var inner=ScavengerFoodProbe.install(ScavengerFoodProbe.SUITE,new Object(),new Object(),new Object(),owner,line->{})){check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,world,actor,food,owner)==null,"nested session masks outer");throw marker;}}
   catch(IllegalStateException caught){check(caught==marker,"scope exception identity");}
   check(ScavengerFoodProbe.selected(ScavengerFoodProbe.SUITE,world,actor,food,owner)==outer,"finally restores outer session");
   for(int i=0;i<100;i++){var row=new JsonObject();row.addProperty("index",i);outer.retain(row);}
   check(outer.first.size()==16&&outer.last.size()==48&&outer.omitted==36,"64-row hard cap");
   check(outer.first.getFirst().get("index").getAsInt()==0&&outer.last.getLast().get("index").getAsInt()==99,"birth and latest rows retained");
  }finally{outer.close();}
  check(rows.size()==65,"bounded rows plus summary");check(ScavengerFoodProbe.scopesClear(),"all scope slots cleaned");outer.close();outer.retain(new JsonObject());check(rows.size()==65,"close is idempotent; no late rows");
  check(outer.world==null&&outer.actor==null&&outer.food==null&&outer.owner==null&&outer.sink==null&&outer.previous==null&&outer.first.isEmpty()&&outer.last.isEmpty(),"references released");
  var stale=ScavengerFoodProbe.install(ScavengerFoodProbe.SUITE,world,actor,food,owner,line->{});var nested=ScavengerFoodProbe.install(ScavengerFoodProbe.SUITE,world,actor,food,owner,line->{});stale.close();nested.close();check(ScavengerFoodProbe.scopesClear(),"out-of-order close cannot restore stale session");
  var sink=ScavengerFoodProbe.install(ScavengerFoodProbe.SUITE,world,actor,food,owner,line->{throw new AssertionError("sink");});sink.retain(new JsonObject());sink.close();check(sink.sinkFailures==2&&ScavengerFoodProbe.scopesClear(),"sink errors cannot prevent cleanup");
 }
 private static void forwarding(){
  int[] calls={0};var actor=new Object();var bounds=new AABB(1,2,3,4,5,6);Predicate<ItemEntity> eligible=item->{throw new AssertionError("observer evaluated eligibility");};var list=new ArrayList<ItemEntity>();
  var result=ScavengerFoodProbe.forage(actor,null,ItemEntity.class,bounds,eligible,args->{calls[0]++;check(args.length==4&&args[0]==null&&args[1]==ItemEntity.class&&args[2]==bounds&&args[3]==eligible,"query arguments preserve identity");return list;});check(result==list,"query return identity");
  result=ScavengerFoodProbe.complete(null,ItemEntity.class,bounds,null,eligible,args->{calls[0]++;check(args.length==5&&args[0]==null&&args[1]==ItemEntity.class&&args[2]==bounds&&args[3]==null&&args[4]==eligible,"complete arguments preserve identity");return list;});check(result==list,"complete return identity");
  ScavengerFoodProbe.raw(null,null,bounds,eligible,list,13,args->{calls[0]++;check(args.length==6&&args[0]==null&&args[1]==null&&args[2]==bounds&&args[3]==eligible&&args[4]==list&&(int)args[5]==13,"native query arguments unchanged");return null;});
  for(boolean returned:new boolean[]{false,true})check(ScavengerFoodProbe.move(actor,null,null,1,args->{calls[0]++;check(args.length==3&&args[0]==null&&args[1]==null&&(double)args[2]==1,"move arguments unchanged");return returned;})==returned,"move boolean unchanged");
  check(ScavengerFoodProbe.distance(actor,null,args->{calls[0]++;check(args.length==2&&args[0]==actor&&args[1]==null,"distance receiver unchanged");return 1.999;})==1.999,"distance unchanged");
  ScavengerFoodProbe.ai(actor,null,args->{calls[0]++;check(args.length==1&&args[0]==null,"AI args unchanged");return null;});
  ScavengerFoodProbe.act(actor,null,args->{calls[0]++;check(args.length==1&&args[0]==null,"act args unchanged");return null;});
  ScavengerFoodProbe.tick((ScavengerFoodProbe.Session)null,args->{calls[0]++;check(args.length==0,"tick args unchanged");return null;});
  check(calls[0]==9,"every original called once with observation inactive");
  var marker=new IllegalStateException("native failure");Operation<Object> throwing=args->{calls[0]++;throw marker;};
  throwsSame(marker,()->ScavengerFoodProbe.forage(actor,null,ItemEntity.class,bounds,eligible,args->{throwing.call(args);return list;}));
  throwsSame(marker,()->ScavengerFoodProbe.complete(null,ItemEntity.class,bounds,null,eligible,args->{throwing.call(args);return list;}));
  throwsSame(marker,()->ScavengerFoodProbe.raw(null,null,bounds,eligible,list,13,args->{throwing.call(args);return null;}));
  throwsSame(marker,()->ScavengerFoodProbe.move(actor,null,null,1,args->{throwing.call(args);return false;}));
  throwsSame(marker,()->ScavengerFoodProbe.distance(actor,null,args->{throwing.call(args);return 0D;}));
  throwsSame(marker,()->ScavengerFoodProbe.ai(actor,null,args->{throwing.call(args);return null;}));
  throwsSame(marker,()->ScavengerFoodProbe.act(actor,null,args->{throwing.call(args);return null;}));
  throwsSame(marker,()->ScavengerFoodProbe.tick((ScavengerFoodProbe.Session)null,args->{throwing.call(args);return null;}));
  check(calls[0]==17&&ScavengerFoodProbe.scopesClear(),"throwing originals called once; all transient scopes restored");
 }
 private static void reentry(){
  var actor=new Object();var food=new Object();var bounds=new AABB(1,2,3,4,5,6);Predicate<ItemEntity> predicate=item->{throw new AssertionError("no extra predicate call");};var returned=new ArrayList<ItemEntity>();int[] calls={0};
  try(var session=ScavengerFoodProbe.install(ScavengerFoodProbe.SUITE,null,actor,food,Thread.currentThread(),line->{})){
   ScavengerFoodProbe.tick(session,args->{
    ScavengerFoodProbe.act(actor,null,actArgs->{
     var actual=ScavengerFoodProbe.forage(actor,null,ItemEntity.class,bounds,predicate,queryArgs->ScavengerFoodProbe.complete(null,ItemEntity.class,bounds,null,predicate,completeArgs->{
      var raw=new ArrayList<ItemEntity>();
      ScavengerFoodProbe.raw(null,null,bounds,predicate,raw,13,rawArgs->{calls[0]++;raw.add(null);return null;});
      // Identical reentry must not overwrite the original raw count or return order.
      ScavengerFoodProbe.complete(null,ItemEntity.class,bounds,null,predicate,nestedArgs->{var wrong=new ArrayList<ItemEntity>();ScavengerFoodProbe.raw(null,null,bounds,predicate,wrong,13,rawArgs->{calls[0]++;for(int i=0;i<13;i++)wrong.add(null);return null;});return wrong;});
      return returned;
     }));check(actual==returned,"active query keeps exact returned list");
     for(boolean answer:new boolean[]{false,true})check(ScavengerFoodProbe.move(actor,null,null,1,moveArgs->answer)==answer,"active observation failure preserves native move result");
     ScavengerFoodProbe.tick(session,nestedArgs->{ScavengerFoodProbe.act(actor,null,nestedAct->{ScavengerFoodProbe.forage(actor,null,ItemEntity.class,bounds,predicate,nestedQuery->{calls[0]++;return returned;});return null;});return null;});
     ScavengerFoodProbe.ai(new Object(),null,otherArgs->{calls[0]++;return null;});
     return null;
    });return null;
   });
   check(calls[0]==4,"nested and unrelated originals still run once");
   var events=session.first.getFirst().getAsJsonArray("events");int queries=0;for(var event:events){var row=event.getAsJsonObject();if(row.get("event").getAsString().equals("forage_query")){queries++;check(row.get("rawCount").getAsInt()==1&&!row.get("rawRefused").getAsBoolean(),"nested raw pool cannot overwrite original");check(row.get("eligibleCount").getAsInt()==0,"real empty result preserved");}}
   check(queries==1&&session.count==1,"reentry and unrelated actors cannot create witness rows");
   check(session.errors>0,"diagnostic snapshot errors contained");
   var bounded=new ScavengerFoodProbe.Frame(session);for(int i=0;i<100;i++)bounded.event("bounded",JsonObject::new);check(bounded.events.size()==12&&session.eventsOmitted==88,"per-tick event storage is bounded");
   var marker=new IllegalStateException("active exception");throwsSame(marker,()->ScavengerFoodProbe.tick(session,args->{ScavengerFoodProbe.act(actor,null,actArgs->{ScavengerFoodProbe.forage(actor,null,ItemEntity.class,bounds,predicate,queryArgs->ScavengerFoodProbe.complete(null,ItemEntity.class,bounds,null,predicate,completeArgs->{throw marker;}));return null;});return null;}));
   check(ScavengerFoodProbe.transientScopesClear(),"exception clears tick, act, query and raw scopes");
   ScavengerFoodProbe.tick(session,args->{session.close();return null;});check(ScavengerFoodProbe.scopesClear(),"closing inside callback cannot revive transient scope");
  }
  check(ScavengerFoodProbe.scopesClear(),"no state survives final close");
 }
 private static void path(){
  var nodes=new ArrayList<>(List.of(new Node(1,101,3),new Node(2,102,3),new Node(3,101,3)));var target=new BlockPos(3,101,3);var path=new Path(nodes,target,true);path.setNextNodeIndex(1);var before=new ArrayList<>(nodes);var row=ScavengerFoodProbe.path(path);
  check(path.getNextNodeIndex()==1&&path.getNodeCount()==3&&path.getTarget()==target&&path.canReach(),"existing native path remains unchanged");for(int i=0;i<3;i++)check(path.getNode(i)==before.get(i),"node identity unchanged");check(row.getAsJsonArray("remainingNodes").size()==2&&row.get("next").getAsInt()==1,"existing path and height recorded");
 }
 private static void throwsSame(RuntimeException marker,Runnable call){try{call.run();throw new AssertionError("native exception swallowed");}catch(RuntimeException caught){check(caught==marker,"native exception identity retained");}}
 private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
