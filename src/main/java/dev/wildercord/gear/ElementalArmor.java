package dev.wildercord.gear;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.*;
import java.util.function.Consumer;

/** Three complete armour sidegrades and a timing mantle, all worn in normal armour slots. */
public final class ElementalArmor extends Item {
	public enum Kind { EMBERWEAVE, RIMEBOUND, STONEBOUND, MIRROR_THREAD }
	public final Kind kind;
	public static final List<ElementalArmor> ALL = new ArrayList<>();
	private ElementalArmor(Kind kind, Properties properties) { super(properties); this.kind=kind; }
	public static void init() {
		if(!ALL.isEmpty()) return;
		for(Kind kind:Kind.values()) for(ArmorType type:List.of(ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS,ArmorType.BOOTS)) {
			if(kind==Kind.MIRROR_THREAD && type!=ArmorType.CHESTPLATE)continue;
			String material=kind.name().toLowerCase(Locale.ROOT);
			String path=kind==Kind.MIRROR_THREAD?"mirror_thread_mantle":material+"_"+type.getName();
			var key=ResourceKey.create(Registries.ITEM,Wildercord.id(path));
			var armor=new ArmorMaterial(15,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,4,ArmorType.LEGGINGS,3,ArmorType.BOOTS,1),12,
				SoundEvents.ARMOR_EQUIP_LEATHER,0,kind==Kind.STONEBOUND?.10F:0,ItemTags.REPAIRS_LEATHER_ARMOR,
				ResourceKey.create(EquipmentAssets.ROOT_ID,Wildercord.id(material)));
			var properties=new Item.Properties().setId(key).humanoidArmor(armor,type).rarity(Rarity.UNCOMMON);
			if(kind==Kind.STONEBOUND) {
				var modifiers=new ArrayList<>(armor.createAttributes(type).modifiers());
				modifiers.add(new ItemAttributeModifiers.Entry(Attributes.MOVEMENT_SPEED,
					new AttributeModifier(Wildercord.id("stonebound_"+type.getName()),-.03,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),EquipmentSlotGroup.bySlot(type.getSlot())));
				properties.attributes(new ItemAttributeModifiers(modifiers));
			}
			ALL.add(Registry.register(BuiltInRegistries.ITEM,key,new ElementalArmor(kind,properties)));
		}
	}
	public static int count(net.minecraft.world.entity.LivingEntity holder,Kind kind) {
		int n=0;
		for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))
			if(holder.getItemBySlot(slot).getItem() instanceof ElementalArmor armor && armor.kind==kind)n++;
		return n;
	}
	@Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag) {
		text.accept(Component.translatable("tooltip.wildercord.armor."+kind.name().toLowerCase(Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
