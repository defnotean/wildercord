package dev.wildercord.gametest.mixin;

import dev.wildercord.client.combat.ArticulatedAuraShellRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only shell ownership proof; never toggles an adapter to test compatibility. */
@Mixin(value=ArticulatedAuraShellRenderer.class,remap=false)
public interface CrimsonMoonShellAccess {
    @Accessor("slim") boolean moon$slim();
}
