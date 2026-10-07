package net.minecraft.world.item;
public class ItemStack {
	public boolean is(Object item) { return item == Items.DIAMOND_SWORD; }
	public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) { return a == b; }
}
