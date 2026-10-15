package dev.wildercord.world.sites.belowkeeper;

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
 * The Belowkeeper Listener at a Belowkeeper Listening Station. Normal use opens subterranean supply trades;
 * sneak-use shares the Belowkeepers' acoustic pressure stories and records the resident and spoken lines in the journal.
 */
public final class BelowkeeperResident {
	private BelowkeeperResident() {}

	public static final String TAG = "wildercord.belowkeeper_listener";
	public static final String ENTRY = "person:belowkeeper_listener";
	public static final int TALK_COOLDOWN = 100;
	public static final List<String> LINES = List.of("greet", "glowcap", "pressure", "hollow_bell");
	public static final List<String> PROGRESS_LINES = List.of("four_lights", "deep_records", "station_gauge", "gauge_return", "bell_return");
	private static final AttachmentType<Integer> LAST_TALK = AttachmentRegistry.create(Wildercord.id("belowkeeper_listener_last_talk"));
	private static final int SPEECH = 0xB8E2DC;

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !(entity instanceof Villager)
				|| !entity.entityTags().contains(TAG)) return InteractionResult.PASS;
			if (player instanceof ServerPlayer server) {
				if (player.isSecondaryUseActive() || !LoreJournal.data(server).has(ENTRY)) talk(server);
			}
			// Sneak-use is a conversation; plain use still opens this villager's normal trades.
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
		LoreJournal.record(player, "said:belowkeeper_listener." + chosen, false);
		player.sendSystemMessage(Component.translatable("dialogue.wildercord.says",
			Component.translatable("entity.wildercord.belowkeeper_listener"),
			Component.translatable("dialogue.wildercord.belowkeeper_listener." + chosen)).withColor(SPEECH));
	}

	public static String lineFor(LoreJournalData data) {
		if (data.stage("four_lights") == LoreQuestRules.ACTIVE && !data.has("said:belowkeeper_listener.four_lights")) return "four_lights";
		for (String line : LINES) if (!data.has("said:belowkeeper_listener." + line)) return line;
		if (data.has("learned:hollow_bell")) return "bell_return";
		if (data.stage("resonance_shutter") == LoreQuestRules.DONE) return "gauge_return";
		if (data.stage("resonance_shutter") == LoreQuestRules.ACTIVE) return "station_gauge";
		if (data.stage("pressure_records") == LoreQuestRules.ACTIVE) return "deep_records";
		return "glowcap";
	}

	/** The owning structure chunk calls this once, then persists its successful spawn flag. */
	public static boolean spawn(WorldGenLevel level, BlockPos at) {
		Villager resident = EntityTypes.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (resident == null) return false;
		resident.snapTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
		resident.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		resident.setVillagerData(resident.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN).withLevel(2));
		resident.setVillagerXp(10);
		resident.setCustomName(Component.translatable("entity.wildercord.belowkeeper_listener"));
		resident.setCustomNameVisible(true);
		resident.setPersistenceRequired();
		resident.setHomeTo(at, 6);
		resident.addTag(TAG);
		return level.addFreshEntity(resident);
	}
}
