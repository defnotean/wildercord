package dev.wildercord.menu;

import dev.wildercord.cast.FusionVfx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Fusion Altar's menu: three rune slots set round a catalyst slot, and the result. The screen
 * shows what the runes on the altar would make ({@link #plan}); pressing its button asks the server,
 * which works the plan out again from the same slots, checks the XP and the result slot, and only then
 * uses anything up. Button 0 fuses; buttons 1 to 4 tie that spell into a Knot.
 */
public class FusionAltarMenu extends AbstractContainerMenu {
	public static final int RUNE_SLOTS = 3;
	public static final int CATALYST = 3;
	public static final int RESULT = 4;
	private static final int INVENTORY_START = 5;
	private static final int INVENTORY_END = INVENTORY_START + 36;

	public static final int BUTTON_FUSE = 0;
	/** Buttons 1 to 4 tie spell 1 to 4. */
	public static final int BUTTON_KNOT = 1;

	// The layout the screen draws around: slots are placed by their top-left corner.
	public static final int[][] RUNE_POS = {{56, 32}, {25, 86}, {87, 86}};
	public static final int[] CATALYST_POS = {56, 68};
	public static final int[] RESULT_POS = {130, 26};
	public static final int INVENTORY_X = 49;
	public static final int INVENTORY_Y = 146;

	private final ContainerLevelAccess access;
	private final Container inputs = new SimpleContainer(4) {
		@Override
		public void setChanged() {
			super.setChanged();
			FusionAltarMenu.this.slotsChanged(this);
		}
	};
	private final Container result = new SimpleContainer(1);

	/** The client's side of the menu: the server's holds where the altar is. */
	public FusionAltarMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public FusionAltarMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(WildercordMenus.FUSION_ALTAR, containerId);
		this.access = access;
		for (int i = 0; i < RUNE_SLOTS; i++) {
			addSlot(new Slot(inputs, i, RUNE_POS[i][0], RUNE_POS[i][1]) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return fusible(stack);
				}
			});
		}
		addSlot(new Slot(inputs, CATALYST, CATALYST_POS[0], CATALYST_POS[1]) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return catalyst(stack) != Fusions.Catalyst.OTHER && !stack.isEmpty();
			}
		});
		addSlot(new Slot(result, 0, RESULT_POS[0], RESULT_POS[1]) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
	}

	/** What can go on the altar: a rune (not a Knot) or a Blank Rune. Woven runes may be ranked up. */
	public static boolean fusible(ItemStack stack) {
		return stack.is(WildercordItems.BLANK_RUNE) || stack.is(WildercordItems.RUNE) || stack.is(WildercordItems.WOVEN_RUNE);
	}

	public static Fusions.Catalyst catalyst(ItemStack stack) {
		if (stack.isEmpty()) {
			return Fusions.Catalyst.NONE;
		}
		if (stack.is(Items.AMETHYST_SHARD)) {
			return Fusions.Catalyst.SHARD;
		}
		if (stack.is(Items.AMETHYST_BLOCK)) {
			return Fusions.Catalyst.BLOCK;
		}
		return stack.is(Items.STRING) ? Fusions.Catalyst.STRING : Fusions.Catalyst.OTHER;
	}

	public static Fusions.Slot slotOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return Fusions.Slot.EMPTY;
		}
		if (stack.is(WildercordItems.BLANK_RUNE)) {
			return Fusions.Slot.BLANK;
		}
		Optional<RuneDef> rune = RuneItem.runeOf(stack);
		// A Knot is read like any rune, so the rules turn it down for what it is (not an effect), never as a silent one.
		return rune.isEmpty() ? Fusions.Slot.SILENT : Fusions.Slot.of(rune.get(), RuneItem.rankOf(stack));
	}

	/** What the altar would do with what's on it now. The screen shows it; the server checks it again before fusing. */
	public Fusions.Plan plan() {
		List<Fusions.Slot> slots = new ArrayList<>();
		for (int i = 0; i < RUNE_SLOTS; i++) {
			slots.add(slotOf(inputs.getItem(i)));
		}
		return Fusions.plan(slots, catalyst(inputs.getItem(CATALYST)));
	}

	/**
	 * The runes of one of the player's spells as a Knot would hold them: the ones their Cord has room
	 * and tier for right now (what the Cord screen shows as live), or null if one of them isn't loaded.
	 */
	public static List<RuneDef> spellRunes(Player player, int spell) {
		Spellbook book = Spellbooks.get(player);
		if (spell < 0 || spell >= book.spells().size()) {
			return List.of();
		}
		for (String id : book.spells().get(spell)) {
			if (Runes.get(id).isEmpty()) {
				return null;
			}
		}
		dev.wildercord.content.CordTier tier = Spellbooks.tier(player);
		return tier == null ? List.of() : new ArrayList<>(dev.wildercord.cast.SpellCaster.activeRunes(book, spell, tier));
	}

	/** Whether the result slot has room: fusing never destroys what's already there. */
	public boolean resultFree() {
		return result.getItem(0).isEmpty();
	}

	public ItemStack input(int slot) {
		return inputs.getItem(slot);
	}

	@Override
	public boolean clickMenuButton(Player clicker, int button) {
		if (!(clicker instanceof ServerPlayer server)) {
			return false;
		}
		Fusions.Plan plan = plan();
		if (plan.kind() == Fusions.Kind.NONE || plan.problem() != null || !resultFree()) {
			refuse(server);
			return false;
		}
		if (button == BUTTON_FUSE && (plan.kind() == Fusions.Kind.UPGRADE || plan.kind() == Fusions.Kind.COMBINE)) {
			if (!pay(server, plan.xp())) {
				return false;
			}
			ItemStack made = RuneItem.stack(plan.result(), plan.rank());
			for (int i = 0; i < RUNE_SLOTS; i++) {
				inputs.removeItem(i, 1);
			}
			if (plan.kind() == Fusions.Kind.COMBINE) {
				inputs.removeItem(CATALYST, 1);
				if (plan.recipe() != null) {
					Grimoire.unlock(server, plan.recipe().key());
				}
				Grimoire.feat(server, Feats.COMBINE);
			} else {
				Grimoire.feat(server, Feats.UPGRADE);
			}
			result.setItem(0, made);
			// A signature fusion flares with its own star (see FusionVfx.altar).
			flourish(RuneColors.of(plan.result()), plan.kind() != Fusions.Kind.COMBINE ? 0 : plan.signature() ? 3 : 1);
			broadcastChanges();
			return true;
		}
		int spell = button - BUTTON_KNOT;
		if (plan.kind() == Fusions.Kind.KNOT && spell >= 0 && spell < dev.wildercord.content.CordTier.MAX_SPELLS) {
			List<RuneDef> runes = spellRunes(server, spell);
			String problem = runes == null ? "That spell holds a rune whose add-on is missing." : Knots.problem(runes);
			if (problem != null) {
				server.sendOverlayMessage(Component.literal(problem).withColor(0xF0C440));
				refuse(server);
				return false;
			}
			if (!pay(server, Knots.xpCost(runes))) {
				return false;
			}
			String id = Knots.id(runes, Spellbooks.get(server).name(spell));
			for (int i = 0; i < RUNE_SLOTS; i++) {
				inputs.removeItem(i, 1);
			}
			inputs.removeItem(CATALYST, 1);
			result.setItem(0, RuneItem.stack(id));
			Grimoire.feat(server, Feats.KNOT);
			flourish(RuneColors.KNOT, 2);
			broadcastChanges();
			return true;
		}
		refuse(server);
		return false;
	}

	/** Takes the XP levels, unless in creative. False (and a message) if there aren't enough. */
	private boolean pay(ServerPlayer server, int levels) {
		if (server.hasInfiniteMaterials()) {
			return true;
		}
		if (server.experienceLevel < levels) {
			server.sendOverlayMessage(Component.translatable("message.wildercord.altar_xp", levels).withColor(0xE05050));
			refuse(server);
			return false;
		}
		server.giveExperienceLevels(-levels);
		return true;
	}

	private void flourish(int color, int kind) {
		access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				FusionVfx.altar(serverLevel, Vec3.atBottomCenterOf(pos).add(0, 0.875, 0), color, kind);
			}
		});
	}

	private void refuse(ServerPlayer server) {
		access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				FusionVfx.altarRefuse(serverLevel, Vec3.atBottomCenterOf(pos).add(0, 0.875, 0));
			}
		});
	}

	@Override
	public void removed(Player who) {
		super.removed(who);
		access.execute((level, pos) -> {
			clearContainer(who, inputs);
			clearContainer(who, result);
		});
	}

	@Override
	public boolean stillValid(Player who) {
		return stillValid(access, who, WildercordBlocks.FUSION_ALTAR);
	}

	@Override
	public ItemStack quickMoveStack(Player who, int index) {
		Slot slot = slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < INVENTORY_START) {
			// Off the altar, into the inventory.
			if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (fusible(stack)) {
			// One rune into each empty rune slot, so three copies spread themselves out for an upgrade.
			boolean moved = false;
			for (int i = 0; i < RUNE_SLOTS && !stack.isEmpty(); i++) {
				if (!inputs.getItem(i).isEmpty()) {
					continue;
				}
				inputs.setItem(i, stack.split(1));
				moved = true;
			}
			if (!moved) {
				return ItemStack.EMPTY;
			}
		} else if (catalyst(stack) != Fusions.Catalyst.OTHER) {
			if (!moveItemStackTo(stack, CATALYST, CATALYST + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(who, stack);
		return copy;
	}
}
