package dev.wildercord.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.21.2: text-display setters are private here; the mod drives them through this accessor. */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Invoker("setText")
	void wildercord$setText(Component text);
	@Invoker("setBackgroundColor")
	void wildercord$setBackgroundColor(int color);
	@Invoker("setFlags")
	void wildercord$setFlags(byte flags);
}