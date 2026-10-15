package dev.wildercord.world.sites.highland;

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

import java.util.List;

/**
 * The Highland Cairn Keeper at a Windscar Cairn Lodge. Normal use opens mountain survey and rescue trades;
 * sneak-use shares the Marchkeepers' paired-cairn stories and records the resident and spoken lines in the journal.
 */
public final class CairnKeeperResident {
	private CairnKeeperResident() {}

	public static final String TAG = "wildercord.cairn_keeper";
	public static final String ENTRY = "person:cairn_keeper";
	public static final int TALK_COOLDOWN = 100;
	public static final List<String> LINES = List.of("greet", "windreed", "cairns", "cartographer");
	public static final List<String> PROGRESS_LINES = List.of("four_lights", "watch_orders", "cairn_light", "light_return", "cartographer_return");
	private static final AttachmentType<Integer> LAST_TALK = AttachmentRegistry.create(Wildercord.id("cairn_keeper_last_talk"));
	private static final int SPEECH = 0xEDE3C8;

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !(entity instanceof Villager)
				|| !entity.entityTags().contains(TAG)) return InteractionResult.PASS;
			if (player instanceof ServerPlayer server) {
				if (player.isSecondaryUseActive() || !LoreJournal.data(server).has(ENTRY)) talk(server);
			}
			// Sneak-use is a conversation; plain use still opens this villager's normal cartographer trades.
			return player.isSecondaryUseActive() ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});
	}

	private static void talk(ServerPlayer player) {
		Integer last = player.getAttached(LAST_TALK);
		if (last != null && player.tickCount >= last && player.tickCount - last < TALK_COOLDOWN) return;
		player.setAttached(LAST_TALK, player.tickCount);
		var data = LoreJournal.data(player);
		String chosen = lineFor(data);
		LoreJournal.record(player, ENTRY, true);
		LoreJournal.record(player, "said:cairn_keeper." + chosen, false);
		player.sendSystemMessage(Component.translatable("dialogue.wildercord.says",
			Component.translatable("entity.wildercord.cairn_keeper"),
			Component.translatable("dialogue.wildercord.cairn_keeper." + chosen)).withColor(SPEECH));
	}

	public static String lineFor(LoreJournalData data) {
		if (data.stage("four_lights") == LoreQuestRules.ACTIVE && !data.has("said:cairn_keeper.four_lights")) return "four_lights";
		for (String line : LINES) if (!data.has("said:cairn_keeper." + line)) return line;
		if (data.has("learned:glass_cartographer")) return "cartographer_return";
		if (data.stage("cairn_beacon") == LoreQuestRules.DONE) return "light_return";
		if (data.stage("cairn_beacon") == LoreQuestRules.ACTIVE) return "cairn_light";
		if (data.stage("ridge_orders") == LoreQuestRules.ACTIVE) return "watch_orders";
		return "windreed";
	}

	/** The owning structure chunk calls this once, then persists its successful spawn flag. */
	public static boolean spawn(WorldGenLevel level, BlockPos at) {
		Villager resident = EntityTypes.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (resident == null) return false;
		resident.snapTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
		resident.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		resident.setVillagerData(resident.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.CARTOGRAPHER).withLevel(2));
		resident.setVillagerXp(10);
		resident.setCustomName(Component.translatable("entity.wildercord.cairn_keeper"));
		resident.setCustomNameVisible(true);
		resident.setPersistenceRequired();
		resident.setHomeTo(at, 6);
		resident.addTag(TAG);
		return level.addFreshEntity(resident);
	}
}
