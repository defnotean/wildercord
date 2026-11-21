package dev.wildercord.wildlife;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/** Real forwarding, scope restoration, bounded storage and existing-path immutability checks. */
public final class EcologyReturnProbeChecks {
 public static void main(String[] args){verify();System.out.println("EcologyReturnProbeChecks passed");}
 public static void verify(){
  var world=new Object();var actor=new Object();var thread=Thread.currentThread();var rows=new ArrayList<String>();var suite=EcologyReturnProbe.REED;
  var outer=EcologyReturnProbe.install(suite,world,actor,thread,rows::add);
  try{
   check(EcologyReturnProbe.selected(suite,world,actor,thread)==outer,"exact scope");
   check(EcologyReturnProbe.selected("other.Test",world,actor,thread)==null,"other scene refused");
   check(EcologyReturnProbe.selected(suite,new Object(),actor,thread)==null,"other world refused");
   check(EcologyReturnProbe.selected(suite,world,new Object(),thread)==null,"other actor refused");
   check(EcologyReturnProbe.selected(suite,world,actor,new Thread())==null,"other thread refused");
   var marker=new IllegalStateException("native failure");
   try{try(var inner=EcologyReturnProbe.install(EcologyReturnProbe.BANK,new Object(),new Object(),thread,line->{})){check(EcologyReturnProbe.selected(suite,world,actor,thread)==null,"nested scope masks outer");throw marker;}}
   catch(IllegalStateException failure){check(failure==marker,"exact exception");}
   check(EcologyReturnProbe.selected(suite,world,actor,thread)==outer,"finally restores outer");
   for(int i=0;i<100;i++)outer.retain("row="+i);
   check(outer.first.size()==32&&outer.last.size()==32&&outer.count==100,"bounded first and last storage");
   check(outer.first.getFirst().equals("row=0")&&outer.last.getLast().equals("row=99"),"original and terminal evidence retained");
  }finally{outer.close();}
  check(rows.size()==65&&EcologyReturnProbe.selected(suite,world,actor,thread)==null,"bounded summary and cleanup");
  outer.close();outer.retain("late");outer.critical("late");check(rows.size()==65&&outer.first.isEmpty()&&outer.milestones.isEmpty(),"idempotent close refuses late rows");
  var critical=EcologyReturnProbe.install(suite,world,actor,thread,line->{});for(int i=0;i<100;i++)critical.critical("event="+i);check(critical.milestones.size()==32&&critical.criticalCount==100,"milestones independently bounded");critical.close();
  var stale=EcologyReturnProbe.install(suite,world,actor,thread,line->{});var nested=EcologyReturnProbe.install(suite,new Object(),new Object(),thread,line->{});stale.close();nested.close();check(EcologyReturnProbe.selected(suite,world,actor,thread)==null,"out of order close cannot restore a closed scope");
  var sink=EcologyReturnProbe.install(suite,world,actor,thread,line->{throw new IllegalStateException("sink");});sink.retain("row");sink.close();check(sink.sinkFailures==2&&EcologyReturnProbe.selected(suite,world,actor,thread)==null,"sink cannot prevent cleanup");
  int[] calls={0};Object goal=new Object();
  for(boolean result:new boolean[]{false,true}){
   boolean actual=EcologyReturnProbe.move(goal,null,null,.7,args->{calls[0]++;check(args.length==3&&args[0]==null&&args[1]==null&&(double)args[2]==.7,"receiver and original arguments retained");return result;});
   check(actual==result,"original boolean retained");
  }
  var marker=new IllegalStateException("native move failed");
  try{EcologyReturnProbe.move(goal,null,null,.7,args->{calls[0]++;throw marker;});throw new AssertionError("exception swallowed");}catch(IllegalStateException failure){check(failure==marker,"native exception identity retained");}
  check(calls[0]==3,"each original invoked exactly once");
  var nodes=new ArrayList<>(List.of(new Node(1,101,3),new Node(2,102,3),new Node(3,101,3)));var target=new BlockPos(3,101,3);var path=new Path(nodes,target,true);path.setNextNodeIndex(1);
  var before=new ArrayList<>(nodes);var text=EcologyReturnProbe.path(path);
  check(path.getNextNodeIndex()==1&&path.getNodeCount()==3&&path.getTarget()==target&&path.canReach(),"path read preserves native state");
  for(int i=0;i<3;i++)check(path.getNode(i)==before.get(i),"existing node identity preserved");
  check(text.contains("2,102,3")&&text.contains("index=1")&&text.contains("canReach=true"),"actual route height and progress recorded");
 }
 private static void check(boolean okay,String why){if(!okay)throw new AssertionError(why);}
}
