package dev.wildercord.world.sites.oathhold;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive 6-row server-side Adventurer's Guild Request Board menu displaying the player's Rank Seal,
 * 8 ranked manga-style job requests, active job progress / turn-in button, and abandon button.
 */
public final class GuildBoardMenu extends ChestMenu {
	private static final int[] REQUEST_SLOTS = {19, 20, 21, 22, 23, 24, 25, 29};
	private static final int SLOT_LICENSE = 4;
	private static final int SLOT_TRACKER = 48;
	private static final int SLOT_ABANDON = 50;

	private final ServerPlayer viewer;
	private final BlockPos boardPos;
	private final List<OathholdQuestRules.JobSpec> displayed = new ArrayList<>(8);

	public GuildBoardMenu(int containerId, Inventory playerInventory, ServerPlayer viewer, BlockPos boardPos) {
		super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
		this.viewer = viewer;
		this.boardPos = boardPos;
		refresh();
	}

	public void refresh() {
		SimpleContainer grid = (SimpleContainer) getContainer();
		grid.clearContent();
		displayed.clear();

		ItemStack frameGray = pane(Items.GRAY_STAINED_GLASS_PANE);
		ItemStack frameGreen = pane(Items.GREEN_STAINED_GLASS_PANE);
		for (int s = 0; s < 54; s++) {
			int row = s / 9;
			int col = s % 9;
			if (row == 0 || row == 5 || col == 0 || col == 8) {
				grid.setItem(s, (s == 0 || s == 8 || s == 45 || s == 53) ? frameGreen.copy() : frameGray.copy());
			}
		}

		OathholdQuests.AdventurerProfile profile = OathholdQuests.profile(viewer);
		OathholdQuestRules.Rank rank = profile.rank();
		long day = viewer.level().getGameTime() / 24000L;

		// Slot 4: Adventurer License Card
		grid.setItem(SLOT_LICENSE, licenseItem(profile, rank));

		// Slots 19..25 & 29: Daily 8 Guild Requests
		List<OathholdQuestRules.JobSpec> specs = OathholdQuestRules.boardSelection(viewer.getUUID(), day, rank);
		for (int i = 0; i < Math.min(specs.size(), REQUEST_SLOTS.length); i++) {
			OathholdQuestRules.JobSpec spec = specs.get(i);
			displayed.add(spec);
			boolean activeThis = profile.activeJob().map(a -> a.jobId().equals(spec.id())).orElse(false);
			grid.setItem(REQUEST_SLOTS[i], requestItem(spec, activeThis));
		}

		// Slot 48: Active Job Tracker / Turn-In
		grid.setItem(SLOT_TRACKER, trackerItem(profile));

		// Slot 50: Abandon button
		ItemStack abandon = new ItemStack(Items.BARRIER);
		abandon.set(DataComponents.CUSTOM_NAME, Component.literal("Abandon Active Request").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
		abandon.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal(profile.activeJob().isPresent()
				? "Click to forfeit your current Guild job."
				: "No active request to abandon.").withStyle(ChatFormatting.GRAY)
		)));
		grid.setItem(SLOT_ABANDON, abandon);
		broadcastChanges();
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ClickType input, Player player) {
		if (slotIndex < 0 || slotIndex >= 54) {
			if (slotIndex >= 54) return;
			super.clicked(slotIndex, buttonNum, input, player);
			return;
		}
		if (!(player instanceof ServerPlayer server)) return;

		for (int i = 0; i < REQUEST_SLOTS.length; i++) {
			if (slotIndex == REQUEST_SLOTS[i] && i < displayed.size()) {
				OathholdQuests.acceptJob(server, boardPos, displayed.get(i));
				refresh();
				return;
			}
		}
		if (slotIndex == SLOT_TRACKER) {
			OathholdQuests.tryTurnInOrReport(server, boardPos);
			refresh();
			return;
		}
		if (slotIndex == SLOT_ABANDON) {
			OathholdQuests.abandonJob(server);
			refresh();
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.isAlive() && player.blockPosition().distSqr(boardPos) <= 100.0;
	}

	private ItemStack licenseItem(OathholdQuests.AdventurerProfile profile, OathholdQuestRules.Rank rank) {
		ItemStack stack = new ItemStack(Items.NAME_TAG);
		stack.set(DataComponents.CUSTOM_NAME,
			Component.literal("Adventurer License — Rank " + rank.name() + " (" + rank.sealMaterial + ")").withStyle(rankColor(rank), ChatFormatting.BOLD));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.literal("Bearer: " + viewer.getName().getString()).withStyle(ChatFormatting.WHITE));
		lore.add(Component.literal("Guild Points: " + profile.rankPoints() + " pts").withStyle(ChatFormatting.GOLD));
		OathholdQuestRules.Rank next = rank.next();
		lore.add(Component.literal(next == null
			? "Highest Orichalcum Guild Rank Achieved!"
			: "Next Rank (" + next.name() + "): " + Math.max(0, next.minPoints - profile.rankPoints()) + " pts needed")
			.withStyle(ChatFormatting.YELLOW));
		lore.add(Component.literal("Completed Requests: " + profile.completedCount()).withStyle(ChatFormatting.GREEN));
		int bounty = OathholdLaw.bountyOf(viewer);
		lore.add(Component.literal(OathholdLaw.isWanted(viewer)
			? "City Law Status: WANTED (" + bounty + " Emeralds)"
			: "City Law Status: Good Standing").withStyle(OathholdLaw.isWanted(viewer) ? ChatFormatting.RED : ChatFormatting.AQUA));
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}

	private static ItemStack requestItem(OathholdQuestRules.JobSpec spec, boolean active) {
		Item icon = switch (spec.category()) {
			case CLEANING -> Items.BRUSH;
			case HERB_GATHERING -> Items.PINK_PETALS;
			case WORK_SHIFT -> Items.ANVIL;
			case CARGO_DELIVERY -> Items.WRITABLE_BOOK;
			case MONSTER_HUNT -> Items.IRON_SWORD;
			case ARENA_BOUT -> Items.GOLDEN_SWORD;
		};
		ItemStack stack = new ItemStack(icon);
		stack.set(DataComponents.CUSTOM_NAME,
			Component.literal("[" + spec.minRank().name() + "] " + spec.title())
				.withStyle(rankColor(spec.minRank()), ChatFormatting.BOLD));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.literal(spec.category().label).withStyle(ChatFormatting.AQUA));
		lore.add(Component.literal("Client: " + spec.client()).withStyle(ChatFormatting.GRAY));
		lore.add(Component.literal(spec.description()).withStyle(ChatFormatting.WHITE));
		lore.add(Component.literal("Reward: " + spec.emeralds() + " Emeralds + " + spec.rankPoints() + " Guild Pts").withStyle(ChatFormatting.GREEN));
		lore.add(Component.literal(active ? "ACTIVE REQUEST" : "Click to accept request!").withStyle(active ? ChatFormatting.YELLOW : ChatFormatting.GOLD));
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}

	private ItemStack trackerItem(OathholdQuests.AdventurerProfile profile) {
		if (profile.activeJob().isEmpty()) {
			ItemStack empty = new ItemStack(Items.COMPASS);
			empty.set(DataComponents.CUSTOM_NAME, Component.literal("No Active Request").withStyle(ChatFormatting.GRAY));
			empty.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Select any request above to begin.").withStyle(ChatFormatting.DARK_GRAY))));
			return empty;
		}
		OathholdQuests.ActiveJob job = profile.activeJob().get();
		OathholdQuestRules.JobSpec spec = OathholdQuestRules.byId(job.jobId());
		boolean gatherReady = spec != null && spec.category() == OathholdQuestRules.JobCategory.HERB_GATHERING
			&& OathholdQuests.countRequiredGoods(viewer, job.targetKey()) >= job.needed();
		boolean ready = job.progress() >= job.needed() || gatherReady;

		ItemStack stack = new ItemStack(ready ? Items.EMERALD : Items.COMPASS);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(spec == null ? job.jobId() : spec.title())
			.withStyle(ready ? ChatFormatting.GREEN : ChatFormatting.YELLOW, ChatFormatting.BOLD));
		List<Component> lore = new ArrayList<>();
		if (spec != null && spec.category() == OathholdQuestRules.JobCategory.HERB_GATHERING) {
			int have = OathholdQuests.countRequiredGoods(viewer, job.targetKey());
			lore.add(Component.literal("Inventory: " + have + " / " + job.needed()).withStyle(ChatFormatting.WHITE));
			lore.add(Component.literal(ready ? "Click to turn in goods & claim reward!" : "Gather required items, then click here.").withStyle(ChatFormatting.GOLD));
		} else {
			lore.add(Component.literal("Progress: " + job.progress() + " / " + job.needed()).withStyle(ChatFormatting.WHITE));
			if (spec != null && (spec.category() == OathholdQuestRules.JobCategory.ARENA_BOUT
					|| spec.category() == OathholdQuestRules.JobCategory.CARGO_DELIVERY)) {
				lore.add(Component.literal("Destination: (" + job.destX() + ", " + job.destZ() + ")").withStyle(ChatFormatting.AQUA));
			}
		}
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}

	private static ItemStack pane(Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.empty());
		return stack;
	}

	public static ChatFormatting rankColor(OathholdQuestRules.Rank rank) {
		return switch (rank) {
			case F -> ChatFormatting.WHITE;
			case E -> ChatFormatting.GRAY;
			case D -> ChatFormatting.GOLD;
			case C -> ChatFormatting.AQUA;
			case B -> ChatFormatting.YELLOW;
			case A -> ChatFormatting.LIGHT_PURPLE;
			case S -> ChatFormatting.RED;
		};
	}

	public static void open(ServerPlayer player, BlockPos boardPos) {
		player.level().playSound(null, boardPos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.0F);
		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
			(id, inv, p) -> new GuildBoardMenu(id, inv, player, boardPos),
			Component.translatable("container.wildercord.guild_request_board")
		));
	}
}
