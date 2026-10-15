package dev.wildercord.world.sites.water;

import dev.wildercord.Wildercord;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreJournalData;
import dev.wildercord.lore.LoreQuestRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.WorldGenLevel;

/** A persistent fisher who keeps the first-crossing stories; ordinary trade and villager life remain available. */
public final class ReedkeeperResident {
	private ReedkeeperResident() {}
	public static final String TAG = "wildercord.reedkeeper";
	public static final String ENTRY = "person:reedkeeper";
	public static final int TALK_COOLDOWN = 100;
	private static final AttachmentType<Integer> LAST_TALK = AttachmentRegistry.create(Wildercord.id("reedkeeper_last_talk"));
	private static final String[] LINES = {"greet", "garden", "crossing", "ferryman"};

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !(entity instanceof Villager)
				|| !entity.entityTags().contains(TAG)) return InteractionResult.PASS;
			if (player instanceof ServerPlayer server) {
				if (player.isSecondaryUseActive() || !LoreJournal.data(server).has(ENTRY)) talk(server);
			}
			// Sneak-use is a conversation; plain use still opens this villager's normal fishing trades.
			return player.isSecondaryUseActive() ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});
	}

	private static void talk(ServerPlayer player) {
		Integer last = player.getAttached(LAST_TALK);
		if (last != null && player.tickCount >= last && player.tickCount - last < TALK_COOLDOWN) return;
		player.setAttached(LAST_TALK, player.tickCount);
		var data = LoreJournal.data(player);
		String chosen = nextLine(data);
		LoreJournal.record(player, ENTRY, true);
		LoreJournal.record(player, "said:reedkeeper." + chosen, false);
		player.sendSystemMessage(Component.translatable("dialogue.wildercord.says",
			Component.translatable("entity.wildercord.reedkeeper"),
			Component.translatable("dialogue.wildercord.reedkeeper." + chosen)).withColor(0xC6DFCD));
	}

	/** Finish the family account first, then answer what this visitor has actually learned. */
	private static String nextLine(LoreJournalData data) {
		for (String line : LINES) if (!data.has("said:reedkeeper." + line)) return line;
		if (data.has("learned:last_crossing")) return "crossing_kept";
		if (data.stage("last_boat_light") == LoreQuestRules.DONE) return "lamp_return";
		if (data.stage("last_boat_light") == LoreQuestRules.ACTIVE) return "lighthouse";
		if (data.stage("second_count") == LoreQuestRules.ACTIVE) return "ledger";
		return "garden";
	}

	/** The owning structure chunk calls this once, then persists its successful spawn flag. */
	static boolean spawn(WorldGenLevel level, BlockPos at) {
		Villager resident = EntityTypes.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (resident == null) return false;
		resident.snapTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
		resident.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		resident.setVillagerData(resident.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.FISHERMAN).withLevel(2));
		resident.setVillagerXp(10);
		resident.setCustomName(Component.translatable("entity.wildercord.reedkeeper"));
		resident.setCustomNameVisible(true);
		resident.setPersistenceRequired();
		resident.setHomeTo(at, 6);
		resident.addTag(WaterPiece.RESIDENT);
		resident.addTag(TAG);
		return level.addFreshEntity(resident);
	}
}
