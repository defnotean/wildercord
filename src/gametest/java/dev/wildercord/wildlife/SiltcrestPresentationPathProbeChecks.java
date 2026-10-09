package dev.wildercord.wildlife;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded diagnostic storage/error controls; never constructs or ticks a game actor. */
public final class SiltcrestPresentationPathProbeChecks {
 private SiltcrestPresentationPathProbeChecks() {}
 public static void main(String[] args){verify();System.out.println("SiltcrestPresentationPathProbeChecks passed");}
 public static void verify() {
  var written=new ArrayList<String>();var log=new SiltcrestPresentationPathProbe.Log(written::add);var captures=new AtomicInteger();
  for(int tick=68;tick<=108;tick++){final int now=tick;log.observe(tick,"unchanged",()->{captures.incrementAndGet();var row=new JsonObject();row.addProperty("clock",now);return row;});}
  check(captures.get()==3&&log.selected==3&&log.polls==41,"Unchanged state captures only the bounded twenty-tick cadence");
  log.observe(109,"bank changed",JsonObject::new);check(log.selected==4,"A transition is retained between cadence points");
  log.close(new JsonObject());int sent=written.size();log.close(new JsonObject());log.observe(110,"later",JsonObject::new);
  check(sent==5&&written.size()==sent&&log.polls==42,"Closed diagnostic storage and output are idempotent");

  written.clear();log=new SiltcrestPresentationPathProbe.Log(written::add);
  for(int n=0;n<100;n++){final int index=n;log.observe(n,"transition_"+n,()->{var row=new JsonObject();row.addProperty("index",index);return row;});}
  check(log.first.size()==16&&log.last.size()==48&&log.omitted==36,"First/latest diagnostic history has exactly sixty-four rows and explicit omissions");
  check(log.first.getFirst().get("index").getAsInt()==0&&log.first.getLast().get("index").getAsInt()==15&&log.last.getFirst().get("index").getAsInt()==52&&log.last.getLast().get("index").getAsInt()==99,"The cap retains both initial refusal context and final timeout context");
  log.close(new JsonObject());var summary=JsonParser.parseString(written.getLast()).getAsJsonObject();
  check(written.size()==65&&summary.get("retained").getAsInt()==64&&summary.get("omitted").getAsInt()==36,"Bounded output reports retained and omitted counts");

  log=new SiltcrestPresentationPathProbe.Log(line->{throw new IllegalStateException("sink");});
  log.observe(68,"changed",()->{throw new AssertionError("diagnostic field unavailable");});
  check(log.errors==1&&log.selected==0,"A failed observation is counted without escaping into the native witness");
  log.observe(69,"changed again",JsonObject::new);log.close(new JsonObject());
  check(log.sinkFailures==2&&log.closed,"A broken sink cannot escape or retain an active scope");
  written.clear();var hostile=new SiltcrestPresentationPathProbe.Log(written::add);
  hostile.observe(68,"changed",()->{throw new AssertionError("hostile formatter"){@Override public String toString(){throw new IllegalStateException("formatter failed");}};});
  check(hostile.errors==1&&"unprintable diagnostic error".equals(hostile.firstError),"Even a throwing error formatter cannot escape observation containment");
  hostile.close(null);
  check(hostile.closed&&hostile.errors==2&&hostile.first.isEmpty()&&hostile.last.isEmpty(),"Summary construction failure remains contained and still releases every retained row");
  check(written.size()==1&&JsonParser.parseString(written.getFirst()).getAsJsonObject().get("event").getAsString().equals("path_summary_error"),"Failed summary construction emits a bounded explicit diagnostic failure when the sink works");
  var absentScope=new SiltcrestPresentationPathProbe(null);
  absentScope.sample(new SiltcrestPresentationWitness.Candidate(1,"fish",53,68,268,SiltcrestPresentationWitness.Outcome.ESCAPED));absentScope.close();absentScope.close();
  var frozen=new SiltcrestPresentationPathProbe.Log(line->{});
  for(int n=0;n<500;n++)frozen.observe(68,"unchanged",JsonObject::new);
  check(frozen.selected==25&&frozen.polls==500,"A stalled source clock remains observable at bounded poll cadence without waits or retries");
  var marker=new AssertionError("original gameplay failure");
  try {try{throw marker;}finally{frozen.close(new JsonObject());}}catch(AssertionError actual){check(actual==marker,"Diagnostic cleanup preserves the original failure object");}
 }
 private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
}
