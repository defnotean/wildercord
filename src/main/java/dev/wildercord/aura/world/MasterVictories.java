package dev.wildercord.aura.world;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Techniques;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Persistent first clears, separate from temporary encounters and existing technique-use stage restrictions. */
public final class MasterVictories {
	private MasterVictories() {}

	public static final Codec<MasterVictoryRules.Progress> CODEC = Codec.INT.xmap(MasterVictoryRules.Progress::new, MasterVictoryRules.Progress::schools);
	public static final AttachmentType<MasterVictoryRules.Progress> RECORD = AttachmentRegistry.create(Wildercord.id("master_victories"),
		builder -> builder.initializer(() -> MasterVictoryRules.Progress.NONE).persistent(CODEC).copyOnDeath());

	/** Load the attachment registration during initialization, before any entities enter the world. */
	public static void init() {}

	public static MasterVictoryRules.Progress progress(ServerPlayer player) {
		return player.getAttachedOrElse(RECORD, MasterVictoryRules.Progress.NONE);
	}

	/** Store the clear before calling learning hooks, so even a re-entrant hook cannot pay it twice. */
	static boolean award(ServerPlayer player, int school) {
		MasterVictoryRules.Progress before = progress(player);
		String part = MasterVictoryRules.reward(school);
		if (part.isEmpty() || before.cleared(school)) return false;
		player.setAttached(RECORD, before.withClear(school));
		boolean learned = Techniques.teach(player, part, "master_trial");
		player.sendSystemMessage(Component.translatable(learned ? "message.wildercord.master.first_clear" : "message.wildercord.master.first_clear_known",
			schoolName(school)));
		return true;
	}

	/** Owner attribution is bounded, tolerates missing owners and handles both arrows and owned summons. */
	public static ServerPlayer owner(Entity entity) {
		for (int depth = 0; entity != null && depth < 8; depth++) {
			if (entity instanceof ServerPlayer player) return player;
			Entity next = entity instanceof Projectile projectile ? projectile.getOwner()
				: entity instanceof OwnableEntity ownable ? ownable.getOwner() : null;
			if (next == entity) return null;
			entity = next;
		}
		return null;
	}

	public static Component schoolName(int school) {
		return Component.translatable("master.wildercord.school." + switch (school) {
			case MastersRules.GALE -> "gale";
			case MastersRules.STONE -> "stone";
			default -> "ember";
		});
	}

	/** A small, server-authored progress readout. It reveals only the requesting player's own record. */
	public static int describe(ServerPlayer player) {
		MasterVictoryRules.Progress progress = progress(player);
		for (int school = MastersRules.EMBER; school <= MastersRules.STONE; school++) {
			player.sendSystemMessage(Component.translatable(progress.cleared(school) ? "message.wildercord.master.record_clear" : "message.wildercord.master.record_open",
				schoolName(school)));
		}
		return Integer.bitCount(progress.schools());
	}
}
