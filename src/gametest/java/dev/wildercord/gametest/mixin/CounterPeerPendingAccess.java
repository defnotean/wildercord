package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.MastersArts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.UUID;

/** Read-only reference to the native cancellation token; never installs or changes an action. */
@Mixin(value=MastersArts.class,remap=false)
public interface CounterPeerPendingAccess {
    @Accessor("PENDING")
    static Map<UUID,?> counter$pending(){throw new AssertionError("Native pending accessor was not applied");}
}
