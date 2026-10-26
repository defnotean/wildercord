package dev.wildercord.mixin;
import net.minecraft.world.entity.animal.equine.Llama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Llama.class)
public interface PackLlamaAccessor {
 @Invoker("setStrength") void wildercord$strength(int strength);
}
