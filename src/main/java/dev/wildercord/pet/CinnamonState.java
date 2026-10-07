package dev.wildercord.pet;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Legacy owner preferences, retained for migration and compatibility; CinnamonJournal owns the saved companion. */
public final class CinnamonState {
	private CinnamonState() {}
	public static void init() {}
	/** Independent identity witness: missing/conflicting journal data must not silently create a replacement. */
	public static final AttachmentType<java.util.UUID> IDENTITY = AttachmentRegistry.create(Wildercord.id("cinnamon_identity"),
		builder -> builder.persistent(net.minecraft.core.UUIDUtil.CODEC).copyOnDeath());
	public static final AttachmentType<Boolean> SITTING = AttachmentRegistry.create(Wildercord.id("cinnamon_sitting"),
		builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
	/** Legacy bow preference, migrated only when this owner first receives a journal identity. */
	public static final AttachmentType<Boolean> BOW = AttachmentRegistry.create(Wildercord.id("cinnamon_bow"),
		builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
}
