package dev.wildercord.gametest;
/** A separate pure JVM must exit nonzero when a sticky rejection cannot be persisted. No Minecraft is initialized. */
public final class CheckCounterPeerFailureExit {
    public static void main(String[] args){
        CounterPeerFailureLedger.reject(java.nio.file.Path.of(args[0]),java.util.Map.of("role","host"),"late-duplicate","intentional missing-directory control");
        throw new AssertionError("Failure persistence returned without halting its isolated test JVM");
    }
}
