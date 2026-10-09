package dev.wildercord.gametest;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.function.BooleanSupplier;

/** Pure GameTest protocol checks. Observations permit native steps; they never assign a game clock. */
public final class CrimsonMoonClockPacing {
    private CrimsonMoonClockPacing() {}
    public record Sample(long sequence,long serverTick,String sha256) {}
    public static long tick(String value) {
        require(value!=null&&value.matches("0|[1-9][0-9]{0,18}"),"Canonical nonnegative native clock required");
        try{return Long.parseLong(value);}catch(NumberFormatException failure){throw new AssertionError("Native clock overflows",failure);}
    }
    public static long rendezvous(long client,long server){require(client>=0&&server>=0,"Actual initial clocks required");return Math.max(client,server);}
    public static boolean reached(long actual,long target){require(actual>=0&&target>=0&&actual<=target,"Native clock overshot rendezvous");return actual==target;}
    public static Sample read(byte[] bytes,Map<String,String> expected) {
        require(bytes.length>0&&bytes.length<=8192,"Bounded native clock witness required");
        try {
            var fields=new Properties();fields.load(new ByteArrayInputStream(bytes));
            for(var field:expected.entrySet())require(field.getValue().equals(fields.getProperty(field.getKey())),"Native clock identity: "+field.getKey());
            long sequence=tick(fields.getProperty("clockSequence")),server=tick(fields.getProperty("clockServerTick"));
            require(sequence>0,"Native clock sequence starts above zero");
            if(expected.containsKey("clockRendezvousTick"))require(server>=tick(expected.get("clockRendezvousTick")),"Native server clock predates rendezvous");
            return new Sample(sequence,server,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException failure){throw new AssertionError("Unreadable native clock witness",failure);}
    }
    public static final class Series {
        private Sample last;
        public Sample observe(Sample next){
            require(next!=null&&next.sequence()>0&&next.serverTick()>=0&&next.sha256()!=null&&next.sha256().matches("[a-f0-9]{64}"),"Complete native clock sample required");
            if(last!=null){
                require(next.sequence()>=last.sequence(),"Native clock sequence rolled back");
                if(next.sequence()==last.sequence())require(next.equals(last),"Native clock tuple changed at the same sequence");
                else require(next.serverTick()>=last.serverTick(),"Native server clock rolled back");
            }
            return last=next;
        }
        public void clear(){last=null;}
    }
    /** Mirrors Fabric's count: every actual tick counts, including ticks followed by ordinary time rollback. */
    public static int waitFor(BooleanSupplier ready,int budget,Runnable nativeTick) {
        require(budget>0,"Positive original native tick budget required");
        for(int steps=0;steps<budget;steps++){if(ready.getAsBoolean())return steps;nativeTick.run();}
        require(ready.getAsBoolean(),"Timed out waiting for predicate");return budget;
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
