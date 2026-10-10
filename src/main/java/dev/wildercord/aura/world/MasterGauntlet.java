package dev.wildercord.aura.world;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Master Gauntlet (0.13): every Sword Master again, back to back. See {@link GauntletRules} for the numbers. A run is lost
 * if its challenger dies, leaves, or a Master's trial ends without it falling; losing costs nothing but the run.
 */
public final class MasterGauntlet {
	private MasterGauntlet() {}

	/** The best full run in seconds (0 for none yet). */
	public static final AttachmentType<Integer> BEST = AttachmentRegistry.create(Wildercord.id("gauntlet_best"),
		builder -> builder.initializer(() -> 0).persistent(Codec.INT).copyOnDeath());
	private static final Set<String> BLADES = Set.of("crimson", "dawn", "dune", "echo", "ember", "gale", "hollow", "hourglass", "iron", "rime",
		"starlit", "stone", "thunder", "tide", "venom", "verdant");

	private static final class Run {
		final List<Integer> order;
		final long started;
		int index;
		SwordMaster master;
		boolean felled;
		long nextAt;

		Run(List<Integer> order, long started) {
			this.order = order;
			this.started = started;
		}
	}

	private static final Map<UUID, Run> RUNS = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 == 0 && !RUNS.isEmpty()) tick(server);
		});
	}

	public static boolean running(ServerPlayer player) {
		return RUNS.containsKey(player.getUUID());
	}

	/** {@code /master gauntlet}: opens a run if every Master has been beaten. */
	public static int start(ServerPlayer player) {
		if (running(player)) return say(player, "running", ChatFormatting.RED);
		int cleared = MasterVictories.progress(player).schools();
		if (!GauntletRules.eligible(cleared)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.gauntlet.locked", GauntletRules.missing(cleared)).withStyle(ChatFormatting.RED));
			return 0;
		}
		Run run = new Run(GauntletRules.order(player.getRandom().nextLong()), player.level().getGameTime());
		RUNS.put(player.getUUID(), run);
		player.sendSystemMessage(Component.translatable("message.wildercord.gauntlet.begin", run.order.size()).withStyle(ChatFormatting.GOLD));
		if (!next(player, run)) {
			RUNS.remove(player.getUUID());
			return 0;
		}
		return 1;
	}

	/** Called as a Master dies, with the players its clear credits. */
	static void felled(SwordMaster master, Set<UUID> credited) {
		for (Map.Entry<UUID, Run> entry : RUNS.entrySet()) {
			if (entry.getValue().master == master && credited.contains(entry.getKey())) entry.getValue().felled = true;
		}
	}

	private static boolean next(ServerPlayer player, Run run) {
		run.felled = false;
		run.master = SwordMaster.gauntletTrial(player, run.order.get(run.index));
		if (run.master == null) {
			say(player, "lost", ChatFormatting.RED);
			return false;
		}
		player.sendOverlayMessage(Component.translatable("message.wildercord.gauntlet.next", run.index + 1, run.order.size(),
			MasterVictories.schoolName(run.order.get(run.index))).withStyle(ChatFormatting.GOLD));
		return true;
	}

	private static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Run>> it = RUNS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Run> entry = it.next();
			Run run = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				if (player != null) say(player, "lost", ChatFormatting.RED);
				it.remove();
				continue;
			}
			long now = player.level().getGameTime();
			if (run.master != null) {
				if (run.felled) {
					run.master = null;
					run.index++;
					if (run.index >= run.order.size()) {
						it.remove();
						finish(server, player, (int) ((now - run.started) / 20));
						continue;
					}
					player.heal((float) (player.getMaxHealth() * GauntletRules.HEAL_SHARE));
					player.sendSystemMessage(Component.translatable("message.wildercord.gauntlet.breath", run.index, run.order.size()).withStyle(ChatFormatting.YELLOW));
					run.nextAt = now + GauntletRules.BREATH_TICKS;
				} else if (run.master.isRemoved()) {
					say(player, "lost", ChatFormatting.RED);
					it.remove();
				}
			} else if (now >= run.nextAt && !next(player, run)) {
				it.remove();
			}
		}
	}

	private static void finish(MinecraftServer server, ServerPlayer player, int seconds) {
		int best = player.getAttachedOrElse(BEST, 0);
		boolean first = best <= 0;
		if (GauntletRules.better(best, seconds)) player.setAttached(BEST, seconds);
		player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
		player.sendSystemMessage(Component.translatable("message.wildercord.gauntlet.won", GauntletRules.time(seconds),
			GauntletRules.time(first ? seconds : Math.min(best, seconds))).withStyle(ChatFormatting.GOLD));
		Component shout = Component.translatable("message.wildercord.gauntlet.champion", player.getDisplayName(), GauntletRules.time(seconds))
			.withStyle(ChatFormatting.GOLD);
		for (ServerPlayer other : server.getPlayerList().getPlayers()) if (other != player) other.sendSystemMessage(shout);
		if (!first) return;
		ItemStack blade = blade(player);
		if (!player.getInventory().add(blade)) player.drop(blade, false, net.minecraft.util.Prediction.SERVER_ONLY);
		dev.wildercord.cast.Grimoire.unlock(player, "aura:gauntlet");
	}

	/** The Blade of the Sixteen: a netherite blade in the look of its owner's own school, kept sharp. */
	public static ItemStack blade(ServerPlayer player) {
		ItemStack blade = new ItemStack(Items.NETHERITE_SWORD);
		String method = dev.wildercord.aura.Aura.data(player).method();
		blade.set(DataComponents.ITEM_MODEL, Wildercord.id("master_blade/" + (BLADES.contains(method) ? method : "ember")));
		blade.set(DataComponents.ITEM_NAME, Component.translatable("item.wildercord.gauntlet_blade"));
		blade.set(DataComponents.RARITY, Rarity.EPIC);
		blade.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.wildercord.gauntlet_blade.lore", player.getName())
			.withStyle(ChatFormatting.GRAY))));
		var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		blade.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 5);
		blade.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
		return blade;
	}

	private static int say(ServerPlayer player, String key, ChatFormatting colour) {
		player.sendSystemMessage(Component.translatable("message.wildercord.gauntlet." + key).withStyle(colour));
		return 0;
	}
}
