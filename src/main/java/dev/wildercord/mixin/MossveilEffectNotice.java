package dev.wildercord.mixin;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Use ordinary effect synchronization after the bounded active-instance duration edit. */
@Mixin(LivingEntity.class)
public interface MossveilEffectNotice{@Invoker("onEffectUpdated")void mossveil$updated(MobEffectInstance effect,boolean refreshAttributes,Entity source);}
