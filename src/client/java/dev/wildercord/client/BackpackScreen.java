package dev.wildercord.client;

import dev.wildercord.menu.BackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * An open backpack, drawn like a chest of its size: its rows over the inventory. The backpack itself, if it's
 * in the hotbar, sits in a shaded slot that can't be touched until it's closed; the backpack key closes it,
 * as the inventory key does.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
	private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	/** The locked slot's shade: the slot well darkened, so the backpack in it reads as held in place. */
	private static final int LOCKED_SHADE = 0x90201A14;

	private final int rows;

	public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 114 + menu.rows() * 18);
		this.rows = menu.rows();
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.rows * 18 + 17, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, this.leftPos, this.topPos + this.rows * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
		if (BackpackMenu.isLocked(slot)) {
			graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, LOCKED_SHADE);
		}
		super.extractSlot(graphics, slot, mouseX, mouseY);
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> lines = super.getTooltipFromContainerItem(stack);
		if (this.hoveredSlot != null && BackpackMenu.isLocked(this.hoveredSlot)) {
			lines = new ArrayList<>(lines);
			lines.add(Component.translatable("screen.wildercord.backpack.open_here").withStyle(ChatFormatting.GOLD));
		}
		return lines;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (super.keyPressed(event)) {
			return true;
		}
		if (WildercordKeys.backpackMapping().matches(event)) {
			this.onClose();
			return true;
		}
		return false;
	}
}
