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
	/** Whether she's wearing her bow, kept with the owner because her body is replaced whenever it's needed. */
	public static final AttachmentType<Boolean> BOW = AttachmentRegistry.create(Wildercord.id("cinnamon_bow"),
		builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
}
