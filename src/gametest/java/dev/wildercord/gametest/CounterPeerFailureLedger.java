package dev.wildercord.gametest;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Sticky test-only failure artifact. Presence rejects acceptance even after successful terminal files. */
public final class CounterPeerFailureLedger {
    private CounterPeerFailureLedger(){}
    public static void reject(Path directory,Map<String,String> identity,String capture,String reason){
        reject(directory,identity,capture,reason,failure->{System.err.println("COUNTER_RECEIPT_FAILURE_NOT_PERSISTED: "+failure);Runtime.getRuntime().halt(86);});
    }
    static synchronized void reject(Path directory,Map<String,String> identity,String capture,String reason,Consumer<Throwable> terminate){
        try{
            String role=identity.get("role");
            if(!List.of("host","peer").contains(role)||!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("Exact supervised failure destination required");
            Path destination=directory.resolve(role+"-counter-render-failure.properties");
            if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS))return; // Never replace, clear or bless an existing rejection.
            var fields=new Properties();fields.putAll(identity);fields.setProperty("capture",capture);fields.setProperty("reason",reason);fields.setProperty("rejected","true");
            Path temporary=Files.createTempFile(directory,role+"-counter-render-failure-",".tmp");
            try(var stream=Files.newOutputStream(temporary)){fields.store(stream,"Sticky native counter rejection; diagnostic identity never authorizes acceptance");}
            try(var file=FileChannel.open(temporary,StandardOpenOption.WRITE)){file.force(true);}
            Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE);
        }catch(Throwable problem){terminate.accept(problem);throw new AssertionError("Isolated counter test must terminate when rejection cannot persist",problem);}
    }
}
