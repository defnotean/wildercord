package dev.wildercord.pet;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** The owner's choice survives Cinnamon's replaceable body, logout, death and dimension travel. */
public final class CinnamonState {
	private CinnamonState() {}
	public static void init() {}
	public static final AttachmentType<Boolean> SITTING = AttachmentRegistry.create(Wildercord.id("cinnamon_sitting"),
		builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
}
