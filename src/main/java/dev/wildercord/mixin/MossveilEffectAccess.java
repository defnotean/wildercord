package dev.wildercord.mixin;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Preserve the entire actual poison instance, including any hidden chain. */
@Mixin(MobEffectInstance.class)
public interface MossveilEffectAccess{@Accessor("duration")void mossveil$duration(int ticks);@Accessor("hiddenEffect")MobEffectInstance mossveil$hidden();}
