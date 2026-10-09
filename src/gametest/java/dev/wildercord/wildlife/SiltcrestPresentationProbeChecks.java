package dev.wildercord.wildlife;

import com.google.gson.JsonObject;
import java.util.ArrayList;

/** Scope/storage checks run without constructing or ticking a Minecraft actor or world. */
public final class SiltcrestPresentationProbeChecks {
 private SiltcrestPresentationProbeChecks() {}
 public static void main(String[] args){verify();System.out.println("SiltcrestPresentationProbeChecks passed");}
 public static void verify() {
  var world=new Object();var bird=new Object();var thread=Thread.currentThread();var rows=new ArrayList<String>();
  String suite=SiltcrestPresentationProbe.SUITE;
  var outer=SiltcrestPresentationProbe.install(suite,world,bird,"full",thread,rows::add);
  try {
   check(SiltcrestPresentationProbe.selected(suite,world,bird,thread)==outer,"exact fixture/world/bird/thread admitted");
   check(SiltcrestPresentationProbe.selected("other.Test",world,bird,thread)==null,"another fixture refused");
   check(SiltcrestPresentationProbe.selected(suite,new Object(),bird,thread)==null,"another world refused");
   check(SiltcrestPresentationProbe.selected(suite,world,new Object(),thread)==null,"another bird refused");
   check(SiltcrestPresentationProbe.selected(suite,world,bird,new Thread())==null,"another thread refused");
   var marker=new IllegalStateException("original failure");
   try {
    try(var inner=SiltcrestPresentationProbe.install(suite,new Object(),new Object(),"minimal",thread,line -> {})) {
     check(SiltcrestPresentationProbe.selected(suite,world,bird,thread)==null,"nested different trial masks prior scope");
     throw marker;
    }
   }catch(IllegalStateException failure){check(failure==marker,"original exception identity retained");}
   check(SiltcrestPresentationProbe.selected(suite,world,bird,thread)==outer,"prior trial restored after exception");
   for(int i=0;i<SiltcrestPresentationProbe.MAX_RECORDS+3;i++)outer.retain(new JsonObject());
   check(outer.records.size()==SiltcrestPresentationProbe.MAX_RECORDS&&outer.omitted==3,"bounded receipts count every omission");
  }finally{outer.close();}
  check(SiltcrestPresentationProbe.selected(suite,world,bird,thread)==null,"closed scope cannot collect");
  check(rows.size()==SiltcrestPresentationProbe.MAX_RECORDS+1,"one bounded summary follows receipts");
  outer.close();check(rows.size()==SiltcrestPresentationProbe.MAX_RECORDS+1,"close is idempotent");
  var failedSink=SiltcrestPresentationProbe.install(suite,world,bird,"sink",thread,line -> {throw new IllegalStateException("sink");});
  failedSink.retain(new JsonObject());failedSink.close();
  check(SiltcrestPresentationProbe.selected(suite,world,bird,thread)==null&&failedSink.sinkFailures==2,"broken diagnostic sink still restores scope");
 }
 private static void check(boolean okay,String why){if(!okay)throw new AssertionError(why);}
}
