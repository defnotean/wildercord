package dev.wildercord.gametest;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Synthetic protocol controls only; no acceptance, source frame, screenshot or native timing claim. */
public final class CrimsonMoonClockPacingChecks {
    private static int checks;
    private CrimsonMoonClockPacingChecks() {}
    public static void main(String[] args){
        check(CrimsonMoonClockPacing.rendezvous(119,100)==119,"Ahead peer is reached by ordinary server advancement");
        check(CrimsonMoonClockPacing.rendezvous(90,100)==100,"Behind peer catches a held actual server clock");
        check(!CrimsonMoonClockPacing.reached(98,100)&&!CrimsonMoonClockPacing.reached(90,100)&&CrimsonMoonClockPacing.reached(100,100),"Unclamped ordinary rollback remains below the exact rendezvous");
        fails("overshot",()->CrimsonMoonClockPacing.reached(101,100));
        fails("initial clocks",()->CrimsonMoonClockPacing.rendezvous(-1,100));
        for(String malformed:new String[]{"-1","+1","01","1.0","9223372036854775808",null})fails(null,()->CrimsonMoonClockPacing.tick(malformed));

        var expected=new HashMap<>(Map.of("nonce","fresh","role","host","pid","111","actorUuid","owner","observerUuid","peer","actorEntity","1","observerEntity","2","clockRendezvousTick","100","clockReadySha256","a".repeat(64),"clockPhase","END_SERVER_TICK"));
        var fields=new HashMap<>(expected);fields.put("clockSequence","1");fields.put("clockServerTick","101");
        var first=CrimsonMoonClockPacing.read(bytes(fields),expected);var series=new CrimsonMoonClockPacing.Series();
        check(series.observe(first).equals(first)&&series.observe(first).equals(first),"Identical rereads of one atomic observation pass");
        for(String key:expected.keySet()){
            var changed=new HashMap<>(fields);changed.put(key,"other");fails("identity",()->CrimsonMoonClockPacing.read(bytes(changed),expected));
        }
        var prior=new HashMap<>(fields);prior.put("clockServerTick","99");fails("predates",()->CrimsonMoonClockPacing.read(bytes(prior),expected));
        fails("same sequence",()->series.observe(new CrimsonMoonClockPacing.Sample(1,102,"b".repeat(64))));
        fails("same sequence",()->series.observe(new CrimsonMoonClockPacing.Sample(1,101,"b".repeat(64))));
        check(series.observe(new CrimsonMoonClockPacing.Sample(2,101,"b".repeat(64))).sequence()==2,"Advancing sequence may observe the same actual server time");
        fails("sequence rolled back",()->series.observe(first));
        fails("server clock rolled back",()->series.observe(new CrimsonMoonClockPacing.Sample(3,100,"c".repeat(64))));
        series.clear();check(series.observe(first).equals(first),"A cleared observer does not leak prior sequence state");
        fails("Bounded",()->CrimsonMoonClockPacing.read(new byte[8193],expected));

        for(int budget:new int[]{35,45}){
            AtomicInteger ticks=new AtomicInteger();AtomicLong actual=new AtomicLong(199);
            fails("Timed out",()->CrimsonMoonClockPacing.waitFor(()->actual.get()==203,budget,()->{
                int step=ticks.incrementAndGet();actual.set(step%2==0?199:200);
            }));
            check(ticks.get()==budget,"Every native step counts despite repeated real clock rollback");
            ticks.set(0);check(CrimsonMoonClockPacing.waitFor(()->ticks.get()==budget,budget,ticks::incrementAndGet)==budget,"The final original-budget predicate is still inspected");
        }
        AtomicInteger ticks=new AtomicInteger();check(CrimsonMoonClockPacing.waitFor(()->true,35,ticks::incrementAndGet)==0&&ticks.get()==0,"Satisfied predicate never advances a native tick");
        RuntimeException disconnected=new RuntimeException("peer disconnected");
        try{CrimsonMoonClockPacing.waitFor(()->false,35,()->{throw disconnected;});throw new AssertionError("Missing native failure");}
        catch(RuntimeException failure){check(failure==disconnected,"Native failure propagates unchanged");}
        System.out.println("Crimson Moon clock pacing checks passed: "+checks);
    }
    private static byte[] bytes(Map<String,String> fields){
        try{var p=new Properties();p.putAll(fields);var out=new ByteArrayOutputStream();p.store(out,"synthetic protocol control");return out.toByteArray();}
        catch(java.io.IOException failure){throw new AssertionError(failure);}
    }
    private static void fails(String text,Runnable action){try{action.run();}catch(AssertionError failure){check(text==null||failure.getMessage().contains(text),"Expected rejection: "+text+"; got "+failure);return;}throw new AssertionError("Missing rejection: "+text);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
