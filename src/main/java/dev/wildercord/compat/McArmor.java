package dev.wildercord.compat;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 1.21.1 armour: the 1.21.3+ `humanoidProperties` on the material class is gone, so the mod keeps a
 * minimal holder-based armour builder here. The honours the 1.21.1 record constructor and the
 * Lore wrapper the creative-tab items carry.
 */
public final class McArmor {

	private McArmor() {}

	/** A custom {@code ArmorLore} item carrying the Lore lore lines, mirroring AuraWorld.Lore. */
	public static class LoreArmor extends ArmorItem {
		private final String id;

		public LoreArmor(String id, Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
			super(material, type, properties);
			this.id = id;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
			Consumer<Component> out = lines::add;
			out.accept(Component.translatable("item.wildercord." + id + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8));
			out.accept(Component.translatable("item.wildercord." + id + ".use").withColor(0xB8D8A8));
		}
	}

	/** Creates an armour material holder from the mod's call-site shape. */
	public static Holder<ArmorMaterial> material(Map<ArmorItem.Type, Integer> defense, int enchantmentValue,
			float toughness, float knockbackResistance, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> repairTag, ResourceLocation model) {
		ArmorMaterial material = new ArmorMaterial(
			defense,
			enchantmentValue,
			Holder.direct(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value()),
			() -> Ingredient.of(repairTag),
			List.of(new ArmorMaterial.Layer(model)),
			toughness,
			knockbackResistance);
		return Holder.direct(material);
	}

	/** Wraps the material, attaches the slot type and the mod-specific lore/hover text. */
	public static LoreArmor loreArmor(String id, Holder<ArmorMaterial> material, ArmorItem.Type type, net.minecraft.world.item.Item.Properties properties) {
		return new LoreArmor(id, material, type, properties);
	}
}
