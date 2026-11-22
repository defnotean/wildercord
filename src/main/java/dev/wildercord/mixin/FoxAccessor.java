package dev.wildercord.mixin;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Fox.class)
public interface FoxAccessor {
 @Invoker("addTrustedUUID") void wildercord$trust(java.util.UUID uuid);
 @Invoker("setSleeping") void wildercord$sleep(boolean value);
 @Invoker("setDefending") void wildercord$defend(boolean value);
}
