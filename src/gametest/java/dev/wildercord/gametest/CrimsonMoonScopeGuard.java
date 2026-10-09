package dev.wildercord.gametest;

import java.util.function.Consumer;

/** Transactional thread-local scope setup; testable without loading a client or renderer. */
public final class CrimsonMoonScopeGuard {
    private CrimsonMoonScopeGuard() {}

    public static <S> S initialize(ThreadLocal<S> current,S scope,Runnable initialize,Consumer<S> clear) {
        S previous=current.get();
        current.set(scope);
        try {
            initialize.run();
            return scope;
        } catch (Throwable failure) {
            try {
                clear.accept(scope);
            } catch (Throwable cleanupFailure) {
                if(cleanupFailure!=failure)failure.addSuppressed(cleanupFailure);
            } finally {
                restore(current,previous);
            }
            throw failure;
        }
    }

    public static <S> void restore(ThreadLocal<S> current,S previous) {
        if(previous==null)current.remove();else current.set(previous);
    }
}

