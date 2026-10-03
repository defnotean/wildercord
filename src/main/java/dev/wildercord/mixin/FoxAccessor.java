package dev.wildercord.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Fox.class)
public interface FoxAccessor {
 @Invoker("addTrustedEntity") void wildercord$trust(LivingEntity player);
 @Invoker("setSleeping") void wildercord$sleep(boolean value);
 @Invoker("setDefending") void wildercord$defend(boolean value);
}
