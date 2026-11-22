package dev.wildercord.compat;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * 1.21.2 fallback for the attachment builder. The 1.21.3+ `AttachmentRegistry.create(id, consumer)`
 * two-argument form does not exist here: the 1.21.2 attachment API has no sync predicate, so the
 * syncWith(codec, predicate) call on the builder did not exist, and attachments are neither
 * synchronized to the client nor validated against a predicate. The builder is otherwise identical
 * (initializer, persistent, copyOnDeath), so the consumer lambdas read exactly as on 1.21.3+, minus
 * the syncWith call, which this version does not have.
 */
public final class McAttachments {
	private McAttachments() {}

	/**
	 * Registers an attachment. The supplied consumer configures the builder; syncWith calls it made
	 * in source are simply gone (the API does not exist here).
	 */
	public static <A> AttachmentType<A> create(ResourceLocation id, Consumer<AttachmentRegistry.Builder<A>> consumer) {
		AttachmentRegistry.Builder<A> builder = AttachmentRegistry.builder();
		consumer.accept(builder);
		return builder.buildAndRegister(id);
	}
}
