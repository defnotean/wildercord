package dev.wildercord.bedrock;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.world.WeaversSilk;
import dev.wildercord.backpack.Backpacks;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.ExciseCasting;
import dev.wildercord.cast.LessonPackCasting;
import dev.wildercord.cast.Mastery;
import dev.wildercord.cast.MasteryChoices;
import dev.wildercord.cast.PracticeRoom;
import dev.wildercord.cast.RelayCircles;
import dev.wildercord.cast.ReweaveFields;
import dev.wildercord.cast.ReweaveState;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.compat.McEntities;
import dev.wildercord.net.NotebookPayload;
import dev.wildercord.net.ServerDisconnects;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.content.CordItem;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.ExciseLesson;
import dev.wildercord.content.PackLesson;
import dev.wildercord.content.RelayLesson;
import dev.wildercord.content.ReweaveLesson;
import dev.wildercord.content.SpellScrollItem;
import dev.wildercord.cosmetic.CordCosmetics;
import dev.wildercord.cosmetic.CordStyles;
import dev.wildercord.gear.CastingGearItem;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.loadout.Loadout;
import dev.wildercord.loadout.LoadoutData;
import dev.wildercord.loadout.LoadoutRules;
import dev.wildercord.loadout.Loadouts;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreJournalData;
import dev.wildercord.lore.LoreQuestRules;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.runesmith.ContractRules;
import dev.wildercord.runesmith.Contracts;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.LessonPackRules;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.Passives;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.world.sites.oathhold.GuildBoardMenu;
import dev.wildercord.world.sites.oathhold.OathholdQuestRules;
import dev.wildercord.world.sites.oathhold.OathholdQuests;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Controller- and touch-friendly Bedrock UI hub ({@code /wc} and right-clicking a Cord) powered by
 * Cumulus forms when Floodgate is present, plus vanilla chest bridges for equipping Cord/Gear slots
 * and full access to spells, loadouts, mastery, cosmetics, codex, Aura, and Master Lessons.
 */
public final class BedrockForms {
	private static final String BEDROCK_FOCUS_TAG = "wildercord:bedrock_focus";
	private static final Map<UUID, Long> LESSON_NONCE = new ConcurrentHashMap<>();
	private static final Queue<Runnable> NEXT_TICK = new ConcurrentLinkedQueue<>();
	private static volatile boolean initialized = false;

	private BedrockForms() {}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
			literal("wc")
				.executes(ctx -> {
					openHub(ctx.getSource().getPlayerOrException());
					return 1;
				})
				.then(literal("cast")
					.executes(ctx -> {
						castOrTriggerActiveSpell(ctx.getSource().getPlayerOrException(), -1);
						return 1;
					})
					.then(argument("slot", IntegerArgumentType.integer(1, SpellSlots.ALL)).executes(ctx -> {
						int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
						castOrTriggerActiveSpell(ctx.getSource().getPlayerOrException(), slot);
						return 1;
					})))
				.then(literal("next").executes(ctx -> {
					cycleNextSpell(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("equip").executes(ctx -> {
					openEquipmentChest(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("backpack").executes(ctx -> {
					Backpacks.openWorn(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("board").executes(ctx -> {
					openGuildContracts(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("hud").executes(ctx -> {
					BedrockHud.toggle(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("silk").executes(ctx -> {
					WeaversSilk.toggle(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("loadout")
					.executes(ctx -> {
						openLoadouts(ctx.getSource().getPlayerOrException());
						return 1;
					})
					.then(literal("next").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						Loadouts.Result r = Loadouts.next(p);
						if (r != null) {
							p.sendSystemMessage(r.message());
						}
						return 1;
					})))
				.then(literal("aura").executes(ctx -> {
					BedrockAuraForms.openAuraHub(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("coat").executes(ctx -> {
					BedrockAuraForms.triggerSlash(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("slash").executes(ctx -> {
					BedrockAuraForms.triggerSlash(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("guard").executes(ctx -> {
					BedrockAuraForms.triggerGuard(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("step").executes(ctx -> {
					BedrockAuraForms.triggerStep(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("dominion").executes(ctx -> {
					BedrockAuraForms.triggerDominion(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("awaken").executes(ctx -> {
					BedrockAuraForms.triggerAwaken(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("dash").executes(ctx -> {
					BedrockAuraForms.triggerDash(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("form").executes(ctx -> {
					BedrockAuraForms.openDashAndForms(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("art").executes(ctx -> {
					BedrockAuraForms.openMastersArts(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("unity").executes(ctx -> {
					BedrockAuraForms.triggerUnity(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("clash").executes(ctx -> {
					BedrockAuraForms.triggerClash(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("counter").executes(ctx -> {
					BedrockAuraForms.triggerCounter(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("practice").executes(ctx -> {
					togglePracticeRoom(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("lessons").executes(ctx -> {
					openLessons(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(literal("focus").executes(ctx -> {
					giveBedrockFocus(ctx.getSource().getPlayerOrException(), false);
					return 1;
				}))
				.then(literal("wand").executes(ctx -> {
					giveBedrockFocus(ctx.getSource().getPlayerOrException(), false);
					return 1;
				}))
		));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int n = NEXT_TICK.size();
			for (int i = 0; i < n; i++) {
				Runnable task = NEXT_TICK.poll();
				if (task == null) {
					break;
				}
				task.run();
			}
		});
		ServerDisconnects.register((handler, server) -> LESSON_NONCE.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LESSON_NONCE.clear();
			NEXT_TICK.clear();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			ServerPlayer joined = handler.player;
			if (BedrockPlayers.isBedrock(joined)) {
				joined.sendSystemMessage(Component.literal(
					"[Wildercord Bedrock] Use /wc for the full Magic & Aura Hub. Right-click a Cord to auto-equip & receive your Cast Focus!"
				).withStyle(ChatFormatting.AQUA));
				joined.sendSystemMessage(Component.literal(
					"Tip: Sneak+Right-click Staff/Focus/Tome/Backpack to equip, or Sneak+Right-click Cast Focus/Weapon for /wc."
				).withStyle(ChatFormatting.GRAY));
			}
		}));

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClientSide() || !(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (!BedrockPlayers.isBedrock(server)) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			if (isBedrockFocus(held)) {
				if (server.isShiftKeyDown()) {
					openHub(server);
				} else if (server.hasAttached(WildercordAttachments.CHARGE)) {
					Charging.request(server, -1, false);
				} else if (Spellbooks.tier(server) == null) {
					server.sendSystemMessage(Component.literal(
						"No Cord equipped yet! Right-click a Cord in your hand to equip it, or open /wc equip."
					).withStyle(ChatFormatting.YELLOW));
					openHub(server);
				} else {
					castOrTriggerActiveSpell(server, -1);
				}
				return InteractionResult.SUCCESS;
			}
			if (held.getItem() instanceof CordItem) {
				if (server.isShiftKeyDown()) {
					openHub(server);
				} else if (Spellbooks.tier(server) == null) {
					Spellbooks.setCord(server, held.copyWithCount(1));
					if (!server.isCreative()) {
						held.shrink(1);
					}
					server.level().playSound(null, server, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
					giveBedrockFocus(server, true);
					server.sendSystemMessage(Component.literal(
						"Equipped Cord! Right-click your Wildercord Cast Focus to cast spells, or Sneak+Right-click it for /wc!"
					).withStyle(ChatFormatting.GREEN));
				} else if (server.hasAttached(WildercordAttachments.CHARGE)) {
					Charging.request(server, -1, false);
				} else {
					castOrTriggerActiveSpell(server, -1);
				}
				return InteractionResult.SUCCESS;
			}
			if (held.getItem() instanceof CastingGearItem) {
				if (server.isShiftKeyDown()) {
					var optSlot = GearSlots.slotFor(held);
					if (optSlot.isPresent()) {
						GearSlot slot = optSlot.get();
						ItemStack prev = GearSlots.get(server, slot).copy();
						ItemStack worn = held.copyWithCount(1);
						GearSlots.set(server, slot, worn);
						if (!server.isCreative()) {
							held.shrink(1);
						}
						if (!prev.isEmpty()) {
							McEntities.placeItemBackInInventory(server, prev);
						}
						server.level().playSound(null, server, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.1F);
						server.sendSystemMessage(Component.literal(
							"Equipped " + worn.getHoverName().getString() + " into " + slot.id() + " slot!"
						).withStyle(ChatFormatting.GREEN));
						return InteractionResult.SUCCESS;
					}
				} else if (Spellbooks.tier(server) != null) {
					if (server.hasAttached(WildercordAttachments.CHARGE)) {
						Charging.request(server, -1, false);
					} else {
						castOrTriggerActiveSpell(server, -1);
					}
					return InteractionResult.SUCCESS;
				}
			}
			if (Backpacks.isBackpack(held) && server.isShiftKeyDown() && Backpacks.worn(server).isEmpty()) {
				Backpacks.wear(server, held.copyWithCount(1));
				if (!server.isCreative()) {
					held.shrink(1);
				}
				server.level().playSound(null, server, SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);
				server.sendSystemMessage(Component.literal(
					"Equipped Backpack! Use /wc backpack to open it anytime."
				).withStyle(ChatFormatting.GREEN));
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	public static ItemStack createBedrockFocus() {
		ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(BEDROCK_FOCUS_TAG), List.of()));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Wildercord Cast Focus").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal("Right-click: Cast active spell").withStyle(ChatFormatting.WHITE),
			Component.literal("Sneak + Right-click: Open /wc Magic Hub").withStyle(ChatFormatting.GRAY)
		)));
		return stack;
	}

	public static boolean isBedrockFocus(ItemStack stack) {
		if (stack == null || !stack.is(Items.AMETHYST_SHARD)) {
			return false;
		}
		CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		return cmd != null && cmd.strings().contains(BEDROCK_FOCUS_TAG);
	}

	private static long nextLessonNonce(UUID playerId) {
		return LESSON_NONCE.merge(playerId, 1L, Long::sum);
	}

	public static void castOrTriggerActiveSpell(ServerPlayer player, int requestedSlot) {
		int slot = requestedSlot < 0 ? Spellbooks.get(player).selected() : requestedSlot;
		if (RelayCircles.contains(player, slot)) {
			long n1 = nextLessonNonce(player.getUUID());
			RelayCircles.input(player, RelayInputRules.DOWN, slot, n1);
			long n2 = nextLessonNonce(player.getUUID());
			RelayCircles.input(player, RelayInputRules.UP, slot, n2);
			return;
		}
		if (ReweaveFields.contains(player, slot)) {
			ReweaveState view = player.getAttached(ReweaveState.VIEW);
			long fieldId = view != null ? view.fieldId() : 0L;
			long n1 = nextLessonNonce(player.getUUID());
			ReweaveFields.input(player, RelayInputRules.DOWN, slot, n1, fieldId);
			long n2 = nextLessonNonce(player.getUUID());
			ReweaveFields.input(player, RelayInputRules.UP, slot, n2, fieldId);
			return;
		}
		if (ExciseCasting.contains(player, slot)) {
			if (ExciseCasting.pending(player)) {
				ExciseCasting.input(player, RelayInputRules.UP, slot, nextLessonNonce(player.getUUID()));
			} else {
				ExciseCasting.input(player, RelayInputRules.DOWN, slot, nextLessonNonce(player.getUUID()));
				NEXT_TICK.add(() -> {
					if (player.connection != null && !player.isRemoved()) {
						ExciseCasting.input(player, RelayInputRules.UP, slot, nextLessonNonce(player.getUUID()));
					}
				});
			}
			return;
		}
		if (LessonPackCasting.contains(player, slot)) {
			long n1 = nextLessonNonce(player.getUUID());
			LessonPackCasting.input(player, RelayInputRules.DOWN, slot, n1);
			long n2 = nextLessonNonce(player.getUUID());
			LessonPackCasting.input(player, RelayInputRules.UP, slot, n2);
			return;
		}
		SpellCaster.cast(player, requestedSlot);
	}

	public static void togglePracticeRoom(ServerPlayer player) {
		if (player.level().dimension().equals(PracticeRoom.DIMENSION)) {
			PracticeRoom.leave(player);
		} else {
			PracticeRoom.enter(player);
		}
	}

	public static void openRuneNotebook(ServerPlayer player, NotebookPayload notebook) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.openNotebookForm(player, notebook);
		}
	}

	public static void giveBedrockFocus(ServerPlayer player, boolean silentIfOwned) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (isBedrockFocus(player.getInventory().getItem(i))) {
				if (!silentIfOwned) {
					player.sendSystemMessage(Component.literal(
						"You already carry a Wildercord Cast Focus! Right-click to cast, Sneak+Right-click for /wc."
					).withStyle(ChatFormatting.AQUA));
				}
				return;
			}
		}
		McEntities.placeItemBackInInventory(player, createBedrockFocus());
		player.sendSystemMessage(Component.literal(
			"Given Wildercord Cast Focus! Right-click to cast, Sneak+Right-click for /wc."
		).withStyle(ChatFormatting.AQUA));
	}

	private static void cycleNextSpell(ServerPlayer player) {
		CordTier tier = Spellbooks.tier(player);
		int cordSpells = tier == null ? 1 : tier.spells;
		List<Integer> open = SpellSlots.open(cordSpells, Gear.tome(player));
		if (open.isEmpty()) {
			open = List.of(0);
		}
		int current = Spellbooks.get(player).selected();
		int idx = open.indexOf(current);
		int nextSlot = open.get((idx + 1) % open.size());
		SpellCaster.select(player, nextSlot);
		List<RuneDef> active = tier == null ? List.of() : SpellCaster.activeRunes(Spellbooks.get(player), nextSlot, tier);
		String name = active.isEmpty() ? "Empty" : SpellCaster.nameOf(player, Spellbooks.get(player), nextSlot, active);
		player.sendOverlayMessage(Component.literal("Spell " + (nextSlot + 1) + ": " + name).withStyle(ChatFormatting.AQUA));
	}

	public static void openHub(ServerPlayer player) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.openMainHub(player);
			return;
		}
		player.sendSystemMessage(Component.literal(
			"[Wildercord] Use /wc cast [1-5], /wc next, /wc equip, /wc backpack, /wc board, /wc loadout, /wc aura, /wc lessons, /wc hud, /wc silk."
		).withStyle(ChatFormatting.AQUA));
		openEquipmentChest(player);
	}

	public static void openGuildContracts(ServerPlayer player) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.openGuildContractsForm(player);
			return;
		}
		GuildBoardMenu.open(player, player.blockPosition());
	}

	public static void openLoadouts(ServerPlayer player) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.openLoadoutsForm(player);
			return;
		}
		Loadouts.Result res = Loadouts.next(player);
		if (res != null) {
			player.sendSystemMessage(res.message());
		}
	}

	public static void openLessons(ServerPlayer player) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.openLessonsMenu(player);
			return;
		}
		player.sendSystemMessage(Component.literal("Open your Grimoire / Lesson Lecterns or use /wc lessons on Bedrock.").withStyle(ChatFormatting.AQUA));
	}

	public static void openRelayLessonFromServer(ServerPlayer player, int page) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.startRelayStudy(player, Math.clamp(page, 0, 2));
		}
	}

	public static void openReweaveLessonFromServer(ServerPlayer player, int page) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.startReweaveStudy(player, Math.clamp(page, 0, 2));
		}
	}

	public static void openExciseLessonFromServer(ServerPlayer player, int page) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.startExciseStudy(player, Math.clamp(page, 0, 2));
		}
	}

	public static void openPackLessonFromServer(ServerPlayer player, int lessonOrd, int page) {
		if (!BedrockBridge.isFloodgateLoaded() || !BedrockPlayers.isBedrock(player)) {
			return;
		}
		LessonPackRules.Lesson[] all = LessonPackRules.Lesson.values();
		if (lessonOrd >= 0 && lessonOrd < all.length) {
			CumulusForms.startPackStudy(player, all[lessonOrd], Math.clamp(page, 0, 2));
		}
	}

	public static void openRunesmithContracts(ServerPlayer player, Contracts.ShowBoard board) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusForms.openRunesmithBoard(player, board);
		}
	}

	/**
	 * Opens a vanilla 9-slot chest menu synced directly with the player's Cord slot (0),
	 * Casting Gear slots (1..3), free Cast Focus button (7), and worn Backpack slot (8),
	 * with strict per-slot validation and zero item loss on close.
	 */
	public static void openEquipmentChest(ServerPlayer player) {
		player.openMenu(new SimpleMenuProvider(
			(syncId, inv, p) -> new BedrockEquipmentChest(syncId, inv),
			Component.literal("Wildercord: Cord [1], Gear [2-4], Pack [9]")
		));
	}

	private static final class BedrockEquipmentChest extends ChestMenu {
		private static final int SLOT_CORD = 0;
		private static final int SLOT_FOCUS_BTN = 7;
		private static final int SLOT_BACKPACK = 8;

		private final List<GearSlot> gearSlots = GearSlot.all();
		private final SimpleContainer view;

		BedrockEquipmentChest(int syncId, Inventory inv) {
			this(syncId, inv, new SimpleContainer(9));
		}

		private BedrockEquipmentChest(int syncId, Inventory inv, SimpleContainer view) {
			super(MenuType.GENERIC_9x1, syncId, inv, view, 1);
			this.view = view;
			if (inv.player instanceof ServerPlayer server) {
				syncViewFromPlayer(server);
			}
		}

		@Override
		public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
			return slot.index != SLOT_FOCUS_BTN && !(slot.index > gearSlots.size() && slot.index < SLOT_FOCUS_BTN);
		}

		@Override
		public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
			if (!(player instanceof ServerPlayer server)) {
				return;
			}
			int firstLocked = Math.min(SLOT_FOCUS_BTN, 1 + gearSlots.size());
			if (slotIndex >= firstLocked && slotIndex < SLOT_FOCUS_BTN) {
				syncViewFromPlayer(server);
				return;
			}
			if (slotIndex == SLOT_FOCUS_BTN) {
				giveBedrockFocus(server, false);
				syncViewFromPlayer(server);
				return;
			}
			if (slotIndex >= 9 && input == ContainerInput.QUICK_MOVE) {
				Slot src = slotIndex < slots.size() ? slots.get(slotIndex) : null;
				if (src != null && src.hasItem()) {
					ItemStack st = src.getItem();
					if (st.getItem() instanceof CordItem && view.getItem(SLOT_CORD).isEmpty()) {
						Spellbooks.setCord(server, st.copyWithCount(1));
						st.shrink(1);
						syncViewFromPlayer(server);
						return;
					}
					var gsOpt = GearSlots.slotFor(st);
					if (gsOpt.isPresent()) {
						int gi = gearSlots.indexOf(gsOpt.get());
						if (gi >= 0 && (1 + gi) < firstLocked && view.getItem(1 + gi).isEmpty()) {
							GearSlots.set(server, gsOpt.get(), st.copyWithCount(1));
							st.shrink(1);
							syncViewFromPlayer(server);
							return;
						}
					}
					if (Backpacks.isBackpack(st) && view.getItem(SLOT_BACKPACK).isEmpty()) {
						Backpacks.wear(server, st.copyWithCount(1));
						st.shrink(1);
						syncViewFromPlayer(server);
						return;
					}
				}
				syncViewFromPlayer(server);
				return;
			}

			super.clicked(slotIndex, buttonNum, input, player);
			validateAndSyncSlots(server);
		}

		private void validateAndSyncSlots(ServerPlayer player) {
			// Slot 0: Cord
			ItemStack cord = view.getItem(SLOT_CORD);
			if (!cord.isEmpty() && !(cord.getItem() instanceof CordItem)) {
				view.setItem(SLOT_CORD, ItemStack.EMPTY);
				McEntities.placeItemBackInInventory(player, cord);
				player.sendOverlayMessage(Component.literal("Slot 1 only holds a Cord!").withStyle(ChatFormatting.RED));
				cord = ItemStack.EMPTY;
			} else if (cord.getCount() > 1) {
				ItemStack extra = cord.copyWithCount(cord.getCount() - 1);
				cord.setCount(1);
				McEntities.placeItemBackInInventory(player, extra);
			}
			Spellbooks.setCord(player, cord.copy());

			// Slots 1..N: Casting Gear
			int maxGear = Math.min(gearSlots.size(), SLOT_FOCUS_BTN - 1);
			for (int i = 0; i < maxGear; i++) {
				int idx = 1 + i;
				GearSlot gs = gearSlots.get(i);
				ItemStack st = view.getItem(idx);
				if (!st.isEmpty() && !GearSlots.fits(gs, st)) {
					view.setItem(idx, ItemStack.EMPTY);
					McEntities.placeItemBackInInventory(player, st);
					player.sendOverlayMessage(Component.literal("Slot " + (idx + 1) + " only holds a " + gs.id() + "!").withStyle(ChatFormatting.RED));
					st = ItemStack.EMPTY;
				} else if (st.getCount() > 1) {
					ItemStack extra = st.copyWithCount(st.getCount() - 1);
					st.setCount(1);
					McEntities.placeItemBackInInventory(player, extra);
				}
				GearSlots.set(player, gs, st.copy());
			}

			// Slot 8: Backpack
			ItemStack pack = view.getItem(SLOT_BACKPACK);
			if (!pack.isEmpty() && !Backpacks.isBackpack(pack)) {
				view.setItem(SLOT_BACKPACK, ItemStack.EMPTY);
				McEntities.placeItemBackInInventory(player, pack);
				player.sendOverlayMessage(Component.literal("Slot 9 only holds a Backpack!").withStyle(ChatFormatting.RED));
				pack = ItemStack.EMPTY;
			} else if (pack.getCount() > 1) {
				ItemStack extra = pack.copyWithCount(pack.getCount() - 1);
				pack.setCount(1);
				McEntities.placeItemBackInInventory(player, extra);
			}
			Backpacks.wear(player, pack.copy());

			syncDecorations();
			broadcastChanges();
		}

		private void syncViewFromPlayer(ServerPlayer player) {
			view.setItem(SLOT_CORD, Spellbooks.cord(player).copy());
			int maxGear = Math.min(gearSlots.size(), SLOT_FOCUS_BTN - 1);
			for (int i = 0; i < maxGear; i++) {
				view.setItem(1 + i, GearSlots.get(player, gearSlots.get(i)).copy());
			}
			view.setItem(SLOT_BACKPACK, Backpacks.worn(player).copy());
			syncDecorations();
			broadcastChanges();
		}

		private void syncDecorations() {
			int firstLocked = Math.min(SLOT_FOCUS_BTN, 1 + gearSlots.size());
			for (int i = firstLocked; i < SLOT_FOCUS_BTN; i++) {
				ItemStack pane = new ItemStack(Items.STAINED_GLASS_PANE.gray());
				pane.set(DataComponents.CUSTOM_NAME, Component.literal("Slot 1=Cord | 2-4=Gear | 9=Pack").withStyle(ChatFormatting.GRAY));
				view.setItem(i, pane);
			}
			ItemStack focusBtn = new ItemStack(Items.AMETHYST_SHARD);
			focusBtn.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			focusBtn.set(DataComponents.CUSTOM_NAME, Component.literal("[Get Cast Focus Wand]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
			focusBtn.set(DataComponents.LORE, new ItemLore(List.of(
				Component.literal("Click to receive a free Cast Focus").withStyle(ChatFormatting.GRAY),
				Component.literal("Right-click=Cast | Sneak+Use=/wc").withStyle(ChatFormatting.WHITE)
			)));
			view.setItem(SLOT_FOCUS_BTN, focusBtn);
		}

		@Override
		public void removed(Player who) {
			if (who instanceof ServerPlayer server) {
				validateAndSyncSlots(server);
			}
			for (int i = 0; i < 9; i++) {
				view.setItem(i, ItemStack.EMPTY);
			}
			super.removed(who);
		}
	}

	private static final class CumulusForms {
		static void openMainHub(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			CordTier tier = Spellbooks.tier(player);
			Spellbook book = Spellbooks.get(player);
			String selectedName = spellLabel(book, book.selected());
			int stage = Aura.stage(player);
			String summary = String.format(
				"Cord: %s  |  Mana: %.0f / %.0f  |  Circles: %d / %d\nSpell [%d]: %s  |  Aura: %s (%.0f/%d)",
				tier != null ? tier.key.toUpperCase() : "None worn",
				Spellbooks.mana(player),
				Mana.max(player),
				Heart.active(player),
				Circles.MAX,
				book.selected() + 1,
				selectedName,
				stage > 0 ? "Stg " + stage : "None",
				Aura.aura(player),
				Aura.capacity(player)
			);

			SimpleForm form = SimpleForm.builder()
				.title("Wildercord Magic & Crossplay Hub")
				.content(summary)
				.button("Cast / Charge / Switch Spell")
				.button("Weave Runes Into Spell (Dynamic Sockets)")
				.button("Passive Spells & Spell Loadouts")
				.button("Spell Mastery Traits")
				.button("Cord Cosmetics, Silk & HUD")
				.button("Cord, Gear & Backpack")
				.button("Aura & Knights Hub")
				.button("Guild Request Board")
				.button("Codex, Runes, Lessons & Status")
				.validResultHandler(resp -> server.execute(() -> {
					if (!player.isAlive()) return;
					switch (resp.clickedButtonId()) {
						case 0 -> openCastingAndSwitchForm(player);
						case 1 -> openChooseSlotToWeaveForm(player);
						case 2 -> openPassivesAndLoadoutsForm(player);
						case 3 -> openMasteryMenuForm(player);
						case 4 -> openCosmeticsAndPrefsForm(player);
						case 5 -> openEquipmentMenuForm(player);
						case 6 -> BedrockAuraForms.openAuraHub(player);
						case 7 -> openGuildContractsForm(player);
						case 8 -> openCodexAndLessonsForm(player);
						default -> {}
					}
				}))
				.build();

			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		// ---- Casting / Charge / Switch / Rename / Scroll

		private static void openCastingAndSwitchForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			boolean charging = player.hasAttached(WildercordAttachments.CHARGE);

			SimpleForm form = SimpleForm.builder()
				.title("Cast, Charge & Switch Spell")
				.content("Active Slot: [" + (book.selected() + 1) + "] " + spellLabel(book, book.selected()))
				.button("Cast Active Spell")
				.button(charging ? "Release Charged Cast Now" : "Start Channeling / Charge Cast")
				.button("Switch Active Spell Slot")
				.button("Cycle to Next Open Spell")
				.button("Rename Spell Slot")
				.button("Inscribe Spell Scroll")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> castOrTriggerActiveSpell(player, -1);
						case 1 -> Charging.request(player, book.selected(), !charging);
						case 2 -> openSelectSpellForm(player);
						case 3 -> cycleNextSpell(player);
						case 4 -> openRenameSpellForm(player);
						case 5 -> SpellScrollItem.inscribe(player, book.selected());
						default -> openMainHub(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openSelectSpellForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			CordTier tier = Spellbooks.tier(player);
			int cordSpells = tier == null ? 1 : tier.spells;
			List<Integer> openSlots = SpellSlots.open(cordSpells, Gear.tome(player));

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Switch Active Spell")
				.content("Select which threaded spell casts when you trigger Cast:");
			for (int slotIdx : openSlots) {
				b.button(String.format("Slot %d: %s", slotIdx + 1, spellLabel(book, slotIdx)));
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < openSlots.size()) {
					int slot = openSlots.get(idx);
					SpellCaster.select(player, slot);
					player.sendOverlayMessage(Component.literal("Selected Spell " + (slot + 1)).withStyle(ChatFormatting.AQUA));
				} else {
					openCastingAndSwitchForm(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openRenameSpellForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			List<String> slotLabels = new ArrayList<>();
			for (int i = 0; i < book.spells().size(); i++) {
				slotLabels.add("Spell " + (i + 1) + " (" + spellLabel(book, i) + ")");
			}
			CustomForm form = CustomForm.builder()
				.title("Rename Spell")
				.dropdown("Spell Slot", slotLabels, book.selected())
				.input("New Name (blank clears custom name)", "Solar Lance")
				.validResultHandler(resp -> server.execute(() -> {
					int slot = resp.asDropdown(0);
					String name = resp.asInput(1);
					SpellCaster.rename(player, slot, name == null ? "" : name);
					player.sendSystemMessage(Component.literal("Updated name for Spell " + (slot + 1)).withStyle(ChatFormatting.AQUA));
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		// ---- Dynamic Socket Weave

		private static void openChooseSlotToWeaveForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			if (book.learned().isEmpty()) {
				player.sendSystemMessage(Component.literal("You haven't learned any runes yet.").withStyle(ChatFormatting.YELLOW));
				return;
			}
			CordTier tier = Spellbooks.tier(player);
			int sockets = tier != null ? tier.sockets : CordTier.TWINE.sockets;
			List<Integer> openSlots = SpellSlots.open(tier == null ? 1 : tier.spells, Gear.tome(player));

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Weave Spell Runes")
				.content("Cord Sockets per Spell: " + sockets + "\nChoose which Spell Slot to edit:");
			for (int slotIdx : openSlots) {
				b.button("Edit Slot " + (slotIdx + 1) + ": " + spellLabel(book, slotIdx));
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < openSlots.size()) {
					openWeaveSpellForm(player, openSlots.get(idx), sockets);
				} else {
					openMainHub(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openWeaveSpellForm(ServerPlayer player, int targetSlot, int socketCount) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			List<String> knownIds = new ArrayList<>(book.learned());

			List<String> runeChoices = new ArrayList<>();
			runeChoices.add("(Empty)");
			for (String id : knownIds) {
				RuneDef def = Runes.get(id).orElse(null);
				runeChoices.add(def != null ? def.name() + " [" + def.family().name().charAt(0) + ", T" + def.tier() + "]" : id);
			}

			List<String> existing = book.spells().get(targetSlot);
			int count = Math.max(CordTier.TWINE.sockets, Math.min(CordTier.MAX_SOCKETS, socketCount));
			CustomForm.Builder form = CustomForm.builder()
				.title("Weave Spell " + (targetSlot + 1) + " (" + count + " Sockets)");

			for (int s = 0; s < count; s++) {
				int defIdx = 0;
				if (s < existing.size()) {
					int found = knownIds.indexOf(existing.get(s));
					if (found >= 0) {
						defIdx = found + 1;
					}
				}
				String label = s == 0 ? "Socket 1 (Usually Shape)" : "Socket " + (s + 1);
				form.dropdown(label, runeChoices, defIdx);
			}

			form.validResultHandler(resp -> server.execute(() -> {
				List<String> next = new ArrayList<>();
				for (int s = 0; s < count; s++) {
					int choice = resp.asDropdown(s);
					if (choice > 0 && (choice - 1) < knownIds.size()) {
						next.add(knownIds.get(choice - 1));
					}
				}
				Component problem = SpellCaster.edit(player, targetSlot, next);
				if (problem != null) {
					player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED));
				} else {
					SpellCaster.select(player, targetSlot);
					player.sendSystemMessage(Component.literal("Threaded Spell " + (targetSlot + 1) + "!").withStyle(ChatFormatting.GREEN));
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), form.build());
		}

		// ---- Passives & Loadouts

		private static void openPassivesAndLoadoutsForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			SimpleForm form = SimpleForm.builder()
				.title("Passives & Spell Loadouts")
				.content("Toggle sustained passive spells or switch saved Cord loadouts.")
				.button("Manage Passive Spells")
				.button("Manage Spell Loadouts")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> openPassivesForm(player);
						case 1 -> openLoadoutsForm(player);
						default -> openMainHub(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openPassivesForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			int maxPassives = Passives.slots(Heart.circles(player));

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Passive Spells")
				.content("Unlocked Passive Slots: " + maxPassives + " / " + Passives.MAX
					+ " (opens at Circle 1 and Circle 5)");

			List<Runnable> actions = new ArrayList<>();
			for (int i = 0; i < Passives.MAX; i++) {
				final int slot = i;
				boolean off = (book.passivesOff() & (1 << i)) != 0;
				List<String> runes = book.passives().get(i);
				String label = runes.isEmpty() ? "(Empty)" : String.join(" + ", runes);
				b.button("Toggle Passive " + (i + 1) + " [" + (off ? "OFF" : "ON") + "]: " + label);
				actions.add(() -> {
					SpellCaster.togglePassive(player, slot);
					openPassivesForm(player);
				});
				b.button("Edit Passive " + (i + 1) + " Runes");
				actions.add(() -> openEditPassiveForm(player, slot));
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < actions.size()) {
					actions.get(idx).run();
				} else {
					openPassivesAndLoadoutsForm(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openEditPassiveForm(ServerPlayer player, int slot) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			List<String> allowedIds = new ArrayList<>();
			List<String> labels = new ArrayList<>();
			labels.add("(Empty)");
			for (String id : book.learned()) {
				RuneDef def = Runes.get(id).orElse(null);
				if (def != null && Passives.allowed(def)) {
					allowedIds.add(id);
					labels.add(def.name() + " [" + def.family().name().charAt(0) + "]");
				}
			}
			if (allowedIds.isEmpty()) {
				player.sendSystemMessage(Component.literal("Learn a passive-compatible Shape/Effect rune first (e.g. Self, Swift, Ember/Ward).").withStyle(ChatFormatting.YELLOW));
				return;
			}
			List<String> cur = book.passives().get(slot);
			int d0 = cur.size() > 0 && allowedIds.contains(cur.get(0)) ? allowedIds.indexOf(cur.get(0)) + 1 : 0;
			int d1 = cur.size() > 1 && allowedIds.contains(cur.get(1)) ? allowedIds.indexOf(cur.get(1)) + 1 : 0;

			CustomForm form = CustomForm.builder()
				.title("Edit Passive " + (slot + 1))
				.dropdown("Passive Socket 1", labels, d0)
				.dropdown("Passive Socket 2", labels, d1)
				.validResultHandler(resp -> server.execute(() -> {
					List<String> next = new ArrayList<>();
					for (int i = 0; i < 2; i++) {
						int c = resp.asDropdown(i);
						if (c > 0 && (c - 1) < allowedIds.size()) {
							next.add(allowedIds.get(c - 1));
						}
					}
					Component problem = SpellCaster.editPassive(player, slot, next);
					if (problem != null) {
						player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED));
					} else {
						player.sendSystemMessage(Component.literal("Updated Passive " + (slot + 1)).withStyle(ChatFormatting.GREEN));
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		static void openLoadoutsForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			LoadoutData data = Loadouts.data(player);
			String curName = data.current() >= 0 && data.current() < data.size()
				? data.get(data.current()).name() : "None";

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Spell Loadouts")
				.content(String.format("Saved Loadouts: %d / %d  |  Active: %s",
					data.size(), LoadoutRules.MAX, curName));

			List<Runnable> actions = new ArrayList<>();
			b.button("Cycle to Next Loadout (/wc loadout next)");
			actions.add(() -> {
				Loadouts.Result r = Loadouts.next(player);
				if (r != null) player.sendSystemMessage(r.message());
			});
			b.button("Save Current Cord as New Loadout");
			actions.add(() -> openSaveLoadoutForm(player));

			for (int i = 0; i < data.size(); i++) {
				final int idx = i;
				Loadout lo = data.get(i);
				b.button((i == data.current() ? "* " : "") + "Manage [" + lo.name() + "]");
				actions.add(() -> openSingleLoadoutForm(player, idx, lo.name()));
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int id = resp.clickedButtonId();
				if (id < actions.size()) {
					actions.get(id).run();
				} else {
					openPassivesAndLoadoutsForm(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openSaveLoadoutForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			CustomForm form = CustomForm.builder()
				.title("Save New Loadout")
				.input("Loadout Name", "Combat")
				.validResultHandler(resp -> server.execute(() -> {
					String name = resp.asInput(0);
					if (name != null) {
						Loadouts.Result r = Loadouts.request(player, Loadouts.SAVE_NEW, 0, name);
						if (r != null) player.sendSystemMessage(r.message());
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openSingleLoadoutForm(ServerPlayer player, int index, String name) {
			MinecraftServer server = player.level().getServer();
			SimpleForm form = SimpleForm.builder()
				.title("Loadout: " + name)
				.content("Choose an action for '" + name + "':")
				.button("Load This Setup Now")
				.button("Overwrite With Current Cord")
				.button("Delete Loadout")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					Loadouts.Result r = switch (resp.clickedButtonId()) {
						case 0 -> Loadouts.request(player, Loadouts.LOAD, index, "");
						case 1 -> Loadouts.request(player, Loadouts.SAVE_OVER, index, "");
						case 2 -> Loadouts.request(player, Loadouts.DELETE, index, "");
						default -> null;
					};
					if (r != null) {
						player.sendSystemMessage(r.message());
					} else {
						openLoadoutsForm(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		// ---- Mastery Choices

		private static void openMasteryMenuForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			CordTier tier = Spellbooks.tier(player);
			List<Integer> openSlots = SpellSlots.open(tier == null ? 1 : tier.spells, Gear.tome(player));

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Spell Mastery & Traits")
				.content("Select a Spell Slot to inspect rank or choose/reroll Mastery traits:");

			List<Integer> matchingSlots = new ArrayList<>();
			for (int slotIdx : openSlots) {
				List<RuneDef> runes = tier != null ? SpellCaster.activeRunes(book, slotIdx, tier) : List.of();
				MasteryBook.Entry entry = runes.isEmpty() ? null : Mastery.entry(player, runes);
				String info = entry == null ? "No record yet"
					: "Rank " + entry.rank() + (entry.offer().isEmpty() ? "" : " [CHOICE READY!]");
				b.button("Spell " + (slotIdx + 1) + ": " + spellLabel(book, slotIdx) + " (" + info + ")");
				matchingSlots.add(slotIdx);
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < matchingSlots.size()) {
					openSingleSpellMasteryForm(player, matchingSlots.get(idx));
				} else {
					openMainHub(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openSingleSpellMasteryForm(ServerPlayer player, int spellSlot) {
			MinecraftServer server = player.level().getServer();
			CordTier tier = Spellbooks.tier(player);
			List<RuneDef> runes = tier != null ? SpellCaster.activeRunes(Spellbooks.get(player), spellSlot, tier) : List.of();
			MasteryBook.Entry entry = runes.isEmpty() ? null : Mastery.entry(player, runes);
			if (entry == null) {
				player.sendSystemMessage(Component.literal("Cast this spell in real combat to build Mastery first!").withStyle(ChatFormatting.YELLOW));
				return;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("Rank ").append(entry.rank()).append("  |  XP: ").append(Math.round(entry.xp()))
				.append("  |  Casts: ").append(entry.casts()).append('\n');
			for (int s = 0; s < MasteryRules.SLOTS; s++) {
				String tid = entry.traits().get(s);
				String tLabel = tid.isEmpty() ? "(Locked / Empty)"
					: MasteryTraits.get(tid).map(MasteryTraits.Trait::name).orElse(tid);
				sb.append("Slot ").append(s + 1).append(" (Rank ").append(MasteryRules.rankOf(s)).append("): ").append(tLabel).append('\n');
			}

			List<Runnable> actions = new ArrayList<>();
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Mastery: Spell " + (spellSlot + 1))
				.content(sb.toString());

			int pending = entry.pendingSlot();
			if (pending >= 0 && !entry.offer().isEmpty()) {
				for (String traitId : entry.offer()) {
					String label = MasteryTraits.get(traitId)
						.map(t -> t.name() + " - " + t.desc())
						.orElse(traitId);
					b.button("CHOOSE (Slot " + (pending + 1) + "): " + label);
					actions.add(() -> sendMasteryMsg(player, MasteryChoices.request(player, MasteryChoices.CHOOSE, spellSlot, pending, traitId)));
				}
				if (!entry.changed(pending)) {
					b.button("Reroll Offer (Costs " + MasteryRules.CHANGE_LEVELS + " XP Levels)");
					actions.add(() -> sendMasteryMsg(player, MasteryChoices.request(player, MasteryChoices.REROLL, spellSlot, pending, "")));
				}
			}

			for (int s = 0; s < MasteryRules.SLOTS; s++) {
				if (!entry.traits().get(s).isEmpty() && !entry.changed(s)) {
					final int traitSlot = s;
					b.button("Unbind Slot " + (s + 1) + " (" + MasteryRules.CHANGE_LEVELS + " XP Levels)");
					actions.add(() -> sendMasteryMsg(player, MasteryChoices.request(player, MasteryChoices.UNBIND, spellSlot, traitSlot, "")));
				}
			}

			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < actions.size()) {
					actions.get(idx).run();
				} else {
					openMasteryMenuForm(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void sendMasteryMsg(ServerPlayer player, Component msg) {
			if (msg != null) {
				player.sendSystemMessage(msg);
			}
		}

		// ---- Cosmetics & Preferences

		private static void openCosmeticsAndPrefsForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			CordStyles.Style style = CordCosmetics.style(player);
			String silkStatus = !WeaversSilk.bound(player) ? "Not Bound"
				: WeaversSilk.stowed(player) ? "Stowed" : "Ready";
			SimpleForm form = SimpleForm.builder()
				.title("Cord Cosmetics & Preferences")
				.content(String.format("Material: %s | Glow: %s | Trail: %s\nBedrock HUD: %s | Weaver's Silk: %s",
					style.material(), style.glow(), style.trail(),
					BedrockHud.isEnabled(player) ? "Shown" : "Hidden", silkStatus))
				.button("Cord Materials")
				.button("Cord Glow Colors")
				.button("Cast Trails")
				.button("Toggle Bedrock Actionbar HUD (/wc hud)")
				.button("Toggle Weaver's Silk (/wc silk)")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> openStyleGroupForm(player, CordStyles.MATERIAL, CordStyles.MATERIALS);
						case 1 -> openStyleGroupForm(player, CordStyles.GLOW, CordStyles.GLOWS);
						case 2 -> openStyleGroupForm(player, CordStyles.TRAIL, CordStyles.TRAILS);
						case 3 -> {
							BedrockHud.toggle(player);
							openCosmeticsAndPrefsForm(player);
						}
						case 4 -> {
							WeaversSilk.toggle(player);
							openCosmeticsAndPrefsForm(player);
						}
						default -> openMainHub(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openStyleGroupForm(ServerPlayer player, String group, List<CordStyles.Option> options) {
			MinecraftServer server = player.level().getServer();
			CordStyles.Style current = CordCosmetics.style(player);
			CordStyles.Progress progress = CordCosmetics.progress(player);

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Cord " + group.toUpperCase())
				.content("Current " + group + ": " + current.get(group));

			for (CordStyles.Option opt : options) {
				boolean equipped = current.get(group).equals(opt.id());
				boolean unlocked = CordStyles.unlocked(opt, progress);
				String suffix = equipped ? " [Equipped]"
					: unlocked ? " [Equip]"
					: CordStyles.buyable(opt) ? String.format(" [Buy: %dx %s]", opt.unlock().amount(), opt.unlock().what())
					: " [Locked]";
				b.button(opt.id() + suffix);
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < options.size()) {
					CordStyles.Option opt = options.get(idx);
					if (CordStyles.unlocked(opt, CordCosmetics.progress(player))) {
						CordCosmetics.wear(player, CordCosmetics.style(player).with(opt));
					} else if (CordStyles.buyable(opt)) {
						CordCosmetics.buy(player, opt.key());
					} else {
						player.sendSystemMessage(Component.translatable("message.wildercord.cosmetic.locked").withStyle(ChatFormatting.RED));
					}
				} else {
					openCosmeticsAndPrefsForm(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		// ---- Equipment

		private static void openEquipmentMenuForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			SimpleForm form = SimpleForm.builder()
				.title("Cord, Casting Gear & Backpack")
				.content("Manage your equipped Wildercord gear using Bedrock chest windows:")
				.button("Edit Cord, Staff, Focus, Tome & Pack")
				.button("Open Worn Backpack")
				.button("Get Free Cast Focus Wand (/wc focus)")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> openEquipmentChest(player);
						case 1 -> Backpacks.openWorn(player);
						case 2 -> giveBedrockFocus(player, false);
						default -> openMainHub(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		static void openRunesmithBoard(ServerPlayer player, Contracts.ShowBoard board) {
			StringBuilder sb = new StringBuilder();
			if (board.contracts().isEmpty()) {
				sb.append("No daily Runesmith contracts available.\n");
			} else {
				for (int i = 0; i < board.contracts().size(); i++) {
					ContractRules.Contract c = board.contracts().get(i);
					ContractRules.Reward r = ContractRules.Reward.parse(c.reward());
					String status = c.claimed() ? "HANDED IN" : (c.done() ? "COMPLETE — Turn in at Scribing Desk" : c.progress() + " / " + c.target());
					sb.append(i + 1).append(". ").append(Contracts.describe(c).getString()).append('\n')
						.append("   Progress: ").append(status)
						.append(" | Reward: ").append(Contracts.rewardLabel(r).getString()).append("\n\n");
				}
			}
			if (!board.handedIn().isEmpty()) {
				sb.append("Handed In Just Now: ").append(String.join(", ", board.handedIn())).append('\n');
			}
			if (board.ticksToDawn() >= 0) {
				sb.append("Next Dawn Refresh: ~").append(Math.max(0, board.ticksToDawn() / 20)).append("s");
			}
			SimpleForm form = SimpleForm.builder()
				.title("Runesmith Daily Contracts")
				.content(sb.toString())
				.button("Close")
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		// ---- Guild Board

		static void openGuildContractsForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			OathholdQuests.AdventurerProfile prof = OathholdQuests.profile(player);
			long day = player.level().getGameTime() / 24000L;
			List<OathholdQuestRules.JobSpec> specs = OathholdQuestRules.boardSelection(player.getUUID(), day, prof.rank());

			String activeText = prof.activeJob()
				.map(j -> String.format("Active Job: %s (%d / %d)", j.jobId(), j.progress(), j.needed()))
				.orElse("No active request.");

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Adventurer's Guild (" + prof.rank().name() + " Rank)")
				.content(String.format("Rank Points: %d  |  Completed: %d\n%s",
					prof.rankPoints(), prof.completedCount(), activeText));
			b.button("Open Full Board Chest UI");
			for (OathholdQuestRules.JobSpec s : specs) {
				b.button("[" + s.minRank().name() + "] " + s.title() + " (" + s.emeralds() + "e)");
			}
			b.button("Back");

			b.validResultHandler(resp -> server.execute(() -> {
				int id = resp.clickedButtonId();
				if (id == 0) {
					GuildBoardMenu.open(player, player.blockPosition());
				} else if (id - 1 < specs.size()) {
					OathholdQuests.acceptJob(player, BlockPos.containing(player.position()), specs.get(id - 1));
				} else {
					openMainHub(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		// ---- Codex, Runes, Fusions, Lore Journal & Master Lessons

		private static void openCodexAndLessonsForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Spellbook book = Spellbooks.get(player);
			var aura = Aura.data(player);
			StringBuilder sb = new StringBuilder();
			sb.append("Known Runes: ").append(book.learned().size()).append(" / ").append(Runes.all().size()).append('\n');
			sb.append("Discovered Fusions / Feats: ").append(Heart.grimoire(player).size())
				.append(" (of ").append(Fusions.RECIPES.size() + Fusions.SIGNATURES.size()).append(" fusions)\n");
			sb.append("Heart Circles: ").append(Heart.active(player)).append(" active / ").append(Heart.circles(player)).append(" formed\n");
			sb.append("Condensed Mana: ").append(Heart.condensed(player)).append('\n');
			sb.append("Aura Stage: ").append(aura.learned() ? ("Stage " + aura.stage() + " (" + Math.round(aura.xp()) + " xp)") : "Unawakened");

			SimpleForm form = SimpleForm.builder()
				.title("Wildercord Codex & Lessons")
				.content(sb.toString())
				.button("Browse Learned Runes")
				.button("Fusions & Discovered Secret Spells")
				.button("Lore Journal & Quests")
				.button("Master Lessons (/wc lessons)")
				.button(player.level().dimension().equals(PracticeRoom.DIMENSION) ? "Leave Practice Arena (/wc practice)" : "Enter Practice Arena (/wc practice)")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> openLearnedRunesCodex(player);
						case 1 -> openFusionsAndSecretsCodex(player);
						case 2 -> openLoreJournalCodex(player);
						case 3 -> openLessonsMenu(player);
						case 4 -> togglePracticeRoom(player);
						default -> openMainHub(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		static void openNotebookForm(ServerPlayer player, NotebookPayload notebook) {
			StringBuilder sb = new StringBuilder();
			sb.append("Shapes Known: ").append(notebook.shapes())
				.append("  |  Fusions: ").append(notebook.fusions())
				.append("  |  Garden: ").append(notebook.garden() ? "Ready" : "No").append("\n\n");
			if (notebook.builds().isEmpty()) {
				sb.append("No saved notebook builds yet.");
			} else {
				sb.append("Recorded Builds:\n");
				for (NotebookPayload.Entry entry : notebook.builds()) {
					sb.append("- ").append(entry.name()).append(" (").append(entry.sockets()).append(" sockets)\n");
				}
			}
			SimpleForm form = SimpleForm.builder()
				.title("Runesmith Notebook")
				.content(sb.toString())
				.button("Close")
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openLearnedRunesCodex(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			List<String> learned = Spellbooks.get(player).learned();
			StringBuilder sb = new StringBuilder();
			for (String id : learned) {
				RuneDef r = Runes.get(id).orElse(null);
				if (r != null) {
					sb.append("- ").append(r.name()).append(" [").append(r.family().name()).append(", T").append(r.tier())
						.append(", ").append(String.format("%.0fMP", r.cost())).append("]: ")
						.append(r.description()).append("\n");
				}
			}
			SimpleForm form = SimpleForm.builder()
				.title("Learned Runes (" + learned.size() + ")")
				.content(sb.isEmpty() ? "No runes learned yet." : sb.toString())
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> openCodexAndLessonsForm(player)))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openFusionsAndSecretsCodex(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			List<String> grimoire = Heart.grimoire(player);
			StringBuilder sb = new StringBuilder("Unlocked Rune Fusions:\n");
			int shown = 0;
			for (Fusions.Recipe rec : Fusions.RECIPES) {
				if (grimoire.contains(rec.key())) {
					sb.append("- ").append(rec.first()).append(" + ").append(rec.second())
						.append(" -> ").append(rec.result().name()).append('\n');
					shown++;
				}
			}
			for (Fusions.Signature sig : Fusions.SIGNATURES) {
				if (grimoire.contains(sig.key())) {
					sb.append("- Signature: ").append(sig.result().name()).append('\n');
					shown++;
				}
			}
			if (shown == 0) {
				sb.append("(Fuse runes at a Fusion Altar to record recipes here.)\n");
			}
			sb.append("\nSecret Spells Found or Hinted:\n");
			for (Secrets.Secret s : Secrets.ALL) {
				if (Heart.discovered(player, s.key())) {
					sb.append("- [FOUND] ").append(s.name()).append(": ").append(s.description()).append('\n');
				} else if (Heart.discovered(player, "hint:" + s.id())) {
					sb.append("- [HINT] ").append(s.name()).append(": ").append(s.riddle()).append('\n');
				}
			}
			SimpleForm form = SimpleForm.builder()
				.title("Fusions & Secret Spells")
				.content(sb.toString())
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> openCodexAndLessonsForm(player)))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openLoreJournalCodex(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			LoreJournalData data = LoreJournal.data(player);
			StringBuilder sb = new StringBuilder("Lore Entries Recorded: " + data.entries().size() + "\n\nQuests:\n");
			for (LoreQuestRules.Quest q : LoreQuestRules.ALL) {
				int st = data.quests().getOrDefault(q.id(), LoreQuestRules.UNKNOWN);
				if (st == LoreQuestRules.DONE) {
					sb.append("[DONE] ").append(q.id()).append('\n');
				} else if (st == LoreQuestRules.ACTIVE) {
					sb.append("[ACTIVE] ").append(q.id()).append('\n');
				}
			}
			SimpleForm form = SimpleForm.builder()
				.title("Lore Journal & Quests")
				.content(sb.toString())
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> openCodexAndLessonsForm(player)))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		static void openLessonsMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			List<Runnable> actions = new ArrayList<>();
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Master Lessons")
				.content("Study unlocked Master Lessons (requires boss feat / lectern recovery + required Heart Circles):");

			b.button("Relay Lesson (Circle 8 / Archivist) - "
				+ (MasterStudies.knowsRelay(player) ? "LEARNED" : MasterStudies.mayStudyRelay(player) ? "READY TO STUDY" : "Locked"));
			actions.add(() -> startRelayStudy(player, 0));

			b.button("Reweave Lesson (Circle 12 / Low Tide) - "
				+ (MasterStudies.knowsReweave(player) ? "LEARNED" : MasterStudies.mayStudyReweave(player) ? "READY TO STUDY" : "Locked"));
			actions.add(() -> startReweaveStudy(player, 0));

			b.button("Excise Lesson (Circle 16 / Heartwood) - "
				+ (MasterStudies.knowsExcise(player) ? "LEARNED" : MasterStudies.mayStudyExcise(player) ? "READY TO STUDY" : "Locked"));
			actions.add(() -> startExciseStudy(player, 0));

			for (LessonPackRules.Lesson lesson : LessonPackRules.ALL) {
				String st = MasterStudies.knows(player, lesson) ? "LEARNED"
					: MasterStudies.mayStudy(player, lesson) ? "READY TO STUDY" : "Locked";
				b.button(lesson.name + " (Circle " + lesson.numeral + ") - " + st);
				actions.add(() -> startPackStudy(player, lesson, 0));
			}

			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < actions.size()) {
					actions.get(idx).run();
				} else {
					openCodexAndLessonsForm(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		static void startRelayStudy(ServerPlayer player, int page) {
			MinecraftServer server = player.level().getServer();
			if (MasterStudies.knowsRelay(player)) {
				showLessonReadOnly(player, RelayLesson.TITLE, RelayLesson.page(page).getString(), page, p -> startRelayStudy(player, p));
				return;
			}
			if (!MasterStudies.mayStudyRelay(player)) {
				player.sendSystemMessage(Component.translatable(MasterStudies.hasRelayLesson(player)
					? "message.wildercord.relay_lesson.locked" : "message.wildercord.relay_lesson.uncopied").withStyle(ChatFormatting.RED));
				return;
			}
			showStudyPage(player, RelayLesson.TITLE, RelayLesson.page(page).getString(), page, () -> {
				if (page + 1 < 3) {
					startRelayStudy(player, page + 1);
				} else if (MasterStudies.learnRelay(player)) {
					player.sendSystemMessage(Component.translatable("message.wildercord.relay_lesson.learned").withColor(0x7FDAD4));
				}
			});
		}

		static void startReweaveStudy(ServerPlayer player, int page) {
			if (MasterStudies.knowsReweave(player)) {
				showLessonReadOnly(player, ReweaveLesson.TITLE, ReweaveLesson.page(page).getString(), page, p -> startReweaveStudy(player, p));
				return;
			}
			if (!MasterStudies.mayStudyReweave(player)) {
				player.sendSystemMessage(Component.translatable(MasterStudies.hasReweaveLesson(player)
					? "message.wildercord.reweave_lesson.locked" : "message.wildercord.reweave_lesson.uncopied").withStyle(ChatFormatting.RED));
				return;
			}
			showStudyPage(player, ReweaveLesson.TITLE, ReweaveLesson.page(page).getString(), page, () -> {
				if (page + 1 < 3) {
					startReweaveStudy(player, page + 1);
				} else if (MasterStudies.learnReweave(player)) {
					player.sendSystemMessage(Component.translatable("message.wildercord.reweave_lesson.learned").withColor(0x7FDAD4));
				}
			});
		}

		static void startExciseStudy(ServerPlayer player, int page) {
			if (MasterStudies.knowsExcise(player)) {
				showLessonReadOnly(player, ExciseLesson.TITLE, ExciseLesson.page(page).getString(), page, p -> startExciseStudy(player, p));
				return;
			}
			if (!MasterStudies.mayStudyExcise(player)) {
				player.sendSystemMessage(Component.translatable(MasterStudies.hasExciseLesson(player)
					? "message.wildercord.excise_lesson.locked" : "message.wildercord.excise_lesson.uncopied").withStyle(ChatFormatting.RED));
				return;
			}
			showStudyPage(player, ExciseLesson.TITLE, ExciseLesson.page(page).getString(), page, () -> {
				if (page + 1 < 3) {
					startExciseStudy(player, page + 1);
				} else if (MasterStudies.learnExcise(player)) {
					player.sendSystemMessage(Component.translatable("message.wildercord.excise_lesson.learned").withColor(0x7FDAD4));
				}
			});
		}

		static void startPackStudy(ServerPlayer player, LessonPackRules.Lesson lesson, int page) {
			if (MasterStudies.knows(player, lesson)) {
				showLessonReadOnly(player, lesson.title, PackLesson.page(lesson, page).getString(), page, p -> startPackStudy(player, lesson, p));
				return;
			}
			if (!MasterStudies.mayStudy(player, lesson)) {
				player.sendSystemMessage(Component.translatable("message.wildercord." + lesson.path + "_lesson."
					+ (MasterStudies.hasLesson(player, lesson) ? "locked" : "uncopied")).withStyle(ChatFormatting.RED));
				return;
			}
			showStudyPage(player, lesson.title, PackLesson.page(lesson, page).getString(), page, () -> {
				if (page + 1 < 3) {
					startPackStudy(player, lesson, page + 1);
				} else if (MasterStudies.learn(player, lesson)) {
					player.sendSystemMessage(Component.translatable("message.wildercord." + lesson.path + "_lesson.learned").withColor(0x7FDAD4));
				}
			});
		}

		private static void showStudyPage(ServerPlayer player, String title, String text, int page, Runnable onAdvance) {
			MinecraftServer server = player.level().getServer();
			SimpleForm form = SimpleForm.builder()
				.title(title + " (Page " + (page + 1) + "/3)")
				.content(text)
				.button(page + 1 < 3 ? "Turn Page ->" : "Complete Lesson & Learn Rune")
				.button("Back to Lessons")
				.validResultHandler(resp -> server.execute(() -> {
					if (resp.clickedButtonId() == 0) {
						onAdvance.run();
					} else {
						openLessonsMenu(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void showLessonReadOnly(ServerPlayer player, String title, String text, int page, java.util.function.IntConsumer openPage) {
			MinecraftServer server = player.level().getServer();
			SimpleForm form = SimpleForm.builder()
				.title(title + " [Learned " + (page + 1) + "/3]")
				.content(text)
				.button("Next Page (" + (((page + 1) % 3) + 1) + "/3)")
				.button("Back to Lessons")
				.validResultHandler(resp -> server.execute(() -> {
					if (resp.clickedButtonId() == 0) {
						openPage.accept((page + 1) % 3);
					} else {
						openLessonsMenu(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static String spellLabel(Spellbook book, int idx) {
			String custom = book.name(idx);
			if (custom != null && !custom.isEmpty()) {
				return custom;
			}
			List<String> ids = book.spells().get(idx);
			if (ids.isEmpty()) {
				return "(Empty)";
			}
			return String.join(" + ", ids.stream().map(id -> id.contains(":") ? id.substring(id.indexOf(':') + 1) : id).toList());
		}
	}
}
