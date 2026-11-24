package dev.wildercord.bedrock;

import dev.wildercord.content.CordTier;
import dev.wildercord.content.FusionAltarBlock;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Fusions;
import dev.wildercord.world.sites.oathhold.GuildRequestBoardBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercepts modded block interactions for Bedrock/Console players when the Java block opens
 * custom UIs (Fusion Altar screen, Guild Request Board, etc.) and presents Bedrock-safe chest
 * wrappers or Cumulus forms backed by the server's real logic.
 */
public final class BedrockInteractBridge {
	private static volatile boolean initialized = false;

	private BedrockInteractBridge() {}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (!BedrockPlayers.isBedrock(server)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hitResult.getBlockPos();
			BlockState state = level.getBlockState(pos);
			Block block = state.getBlock();

			if (block instanceof FusionAltarBlock) {
				openBedrockFusionAltar(server, pos);
				level.playSound(null, pos, WildercordSounds.ALTAR_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
				return InteractionResult.SUCCESS;
			}
			if (block instanceof GuildRequestBoardBlock) {
				BedrockForms.openGuildContracts(server);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static void openBedrockFusionAltar(ServerPlayer player, BlockPos pos) {
		player.openMenu(new SimpleMenuProvider(
			(syncId, inv, p) -> new BedrockFusionAltarChest(syncId, inv, ContainerLevelAccess.create(player.level(), pos)),
			FusionAltarBlock.TITLE
		));
	}

	/**
	 * 9-slot single-row chest UI wrapping a live {@link FusionAltarMenu}:
	 * <ul>
	 *   <li>Slots 0–2: Runes / Blank Rune / Reagent</li>
	 *   <li>Slot 3: Catalyst (Amethyst Shard or Block)</li>
	 *   <li>Slot 4: Result Output</li>
	 *   <li>Slots 5 &amp; 8: Frame Panes</li>
	 *   <li>Slot 6: [FUSE RUNES] (calls {@code FusionAltarMenu.BUTTON_FUSE})</li>
	 *   <li>Slot 7: [TIE KNOT] (calls {@code FusionAltarMenu.BUTTON_KNOT})</li>
	 * </ul>
	 */
	private static final class BedrockFusionAltarChest extends ChestMenu {
		private static final int SLOT_RESULT = 4;
		private static final int SLOT_SEP_LEFT = 5;
		private static final int SLOT_BTN_FUSE = 6;
		private static final int SLOT_BTN_KNOT = 7;
		private static final int SLOT_SEP_RIGHT = 8;

		private final FusionAltarMenu altar;
		private final SimpleContainer view;

		BedrockFusionAltarChest(int syncId, Inventory inv, ContainerLevelAccess access) {
			this(syncId, inv, access, new SimpleContainer(9));
		}

		private BedrockFusionAltarChest(int syncId, Inventory inv, ContainerLevelAccess access, SimpleContainer view) {
			super(MenuType.GENERIC_9x1, syncId, inv, view, 1);
			this.view = view;
			this.altar = new FusionAltarMenu(syncId, inv, access);
			syncViewFromAltar();
		}

		@Override
		public boolean stillValid(Player player) {
			return altar.stillValid(player);
		}

		@Override
		public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
			if (!(player instanceof ServerPlayer server)) {
				return;
			}
			if (slotIndex == SLOT_SEP_LEFT || slotIndex == SLOT_SEP_RIGHT) {
				broadcastChanges();
				return;
			}
			if (slotIndex == SLOT_BTN_FUSE) {
				syncAltarFromView(server);
				altar.clickMenuButton(server, FusionAltarMenu.BUTTON_FUSE);
				syncViewFromAltar();
				return;
			}
			if (slotIndex == SLOT_BTN_KNOT) {
				syncAltarFromView(server);
				int spell = Math.max(0, Math.min(CordTier.MAX_SPELLS - 1, Spellbooks.get(server).selected()));
				altar.clickMenuButton(server, FusionAltarMenu.BUTTON_KNOT + spell);
				syncViewFromAltar();
				return;
			}

			// Handle quick-move from player inventory into rune slots (0..2) or catalyst (3) only
			if (slotIndex >= 9 && input == ContainerInput.QUICK_MOVE) {
				Slot src = slotIndex < slots.size() ? slots.get(slotIndex) : null;
				if (src != null && src.hasItem()) {
					ItemStack stack = src.getItem();
					if (FusionAltarMenu.fusible(stack)) {
						moveItemStackTo(stack, 0, FusionAltarMenu.RUNE_SLOTS, false);
					} else if (FusionAltarMenu.catalyst(stack) != Fusions.Catalyst.OTHER && !stack.isEmpty()) {
						moveItemStackTo(stack, 3, 4, false);
					}
					syncAltarFromView(server);
					syncViewFromAltar();
				}
				return;
			}

			if (slotIndex == SLOT_RESULT) {
				ItemStack cur = altar.getSlot(SLOT_RESULT).getItem();
				if (cur.isEmpty()) {
					broadcastChanges();
					return;
				}
				// Let player take result out (either quick-move or pick up with empty cursor)
				if (input == ContainerInput.QUICK_MOVE) {
					ItemStack copy = cur.copy();
					if (moveItemStackTo(copy, 9, slots.size(), true)) {
						view.setItem(SLOT_RESULT, copy);
						altar.getSlot(SLOT_RESULT).set(copy.copy());
					}
					syncViewFromAltar();
					return;
				}
				if (getCarried().isEmpty()) {
					super.clicked(slotIndex, buttonNum, input, player);
					altar.getSlot(SLOT_RESULT).set(view.getItem(SLOT_RESULT).copy());
					syncViewFromAltar();
				}
				return;
			}

			super.clicked(slotIndex, buttonNum, input, player);
			if (slotIndex >= 0 && slotIndex < 4) {
				syncAltarFromView(server);
				syncViewFromAltar();
			}
		}

		private void syncAltarFromView(ServerPlayer player) {
			for (int i = 0; i < 4; i++) {
				ItemStack st = view.getItem(i);
				Slot target = altar.getSlot(i);
				if (!st.isEmpty() && !target.mayPlace(st)) {
					view.setItem(i, ItemStack.EMPTY);
					dev.wildercord.compat.McEntities.placeItemBackInInventory(player, st);
					target.set(ItemStack.EMPTY);
				} else {
					target.set(st.copy());
				}
			}
		}

		private void syncViewFromAltar() {
			for (int i = 0; i <= SLOT_RESULT; i++) {
				view.setItem(i, altar.getSlot(i).getItem().copy());
			}
			view.setItem(SLOT_SEP_LEFT, pane(Items.STAINED_GLASS_PANE.gray()));
			view.setItem(SLOT_SEP_RIGHT, pane(Items.STAINED_GLASS_PANE.gray()));

			Fusions.Plan plan = altar.plan();
			List<Component> fuseLore = new ArrayList<>();
			if (plan.kind() == Fusions.Kind.UPGRADE || plan.kind() == Fusions.Kind.COMBINE) {
				fuseLore.add(Component.literal("Ready: " + plan.result().name() + " (Tier " + plan.rank() + ")").withStyle(ChatFormatting.GREEN));
				fuseLore.add(Component.literal("XP Cost: " + plan.xp() + " levels").withStyle(ChatFormatting.AQUA));
				fuseLore.add(Component.literal("Click to Fuse!").withStyle(ChatFormatting.GOLD));
			} else if (plan.problem() != null) {
				fuseLore.add(Component.literal(plan.problem()).withStyle(ChatFormatting.RED));
			} else {
				fuseLore.add(Component.literal("Slots 1-3: Runes/Reagent | Slot 4: Amethyst").withStyle(ChatFormatting.GRAY));
			}
			ItemStack fuseBtn = new ItemStack(Items.AMETHYST_CLUSTER);
			fuseBtn.set(DataComponents.CUSTOM_NAME, Component.literal("[FUSE RUNES]").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
			fuseBtn.set(DataComponents.LORE, new ItemLore(fuseLore));
			view.setItem(SLOT_BTN_FUSE, fuseBtn);

			ItemStack knotBtn = new ItemStack(Items.NAME_TAG);
			knotBtn.set(DataComponents.CUSTOM_NAME, Component.literal("[TIE KNOT]").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			knotBtn.set(DataComponents.LORE, new ItemLore(List.of(
				Component.literal("Put Blank Rune + Catalyst, then click here").withStyle(ChatFormatting.GRAY),
				Component.literal("to tie your selected spell into a Knot!").withStyle(ChatFormatting.WHITE)
			)));
			view.setItem(SLOT_BTN_KNOT, knotBtn);

			broadcastChanges();
		}

		@Override
		public void removed(Player who) {
			if (who instanceof ServerPlayer server) {
				syncAltarFromView(server);
			}
			// Clear the view before delegates so items in slots 0..4 are returned once via altar.removed(who)
			// and decorative UI buttons never drop into the world.
			for (int i = 0; i < 9; i++) {
				view.setItem(i, ItemStack.EMPTY);
			}
			altar.removed(who);
			super.removed(who);
		}

		private static ItemStack pane(Item item) {
			ItemStack s = new ItemStack(item);
			s.set(DataComponents.CUSTOM_NAME, Component.empty());
			return s;
		}
	}
}
