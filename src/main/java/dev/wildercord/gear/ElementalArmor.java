package dev.wildercord.gear;

import net.minecraft.world.item.TooltipFlag;
import java.util.List;
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
import dev.wildercord.compat.McArmor;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Three complete armour sidegrades and a timing mantle, all worn in normal armour slots. */
public final class ElementalArmor extends ArmorItem {
	public enum Kind { EMBERWEAVE, RIMEBOUND, STONEBOUND, MIRROR_THREAD }
	public final Kind kind;
	public static final List<ElementalArmor> ALL = new ArrayList<>();
	private ElementalArmor(Kind kind, net.minecraft.core.Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) { super(material,type,properties); this.kind=kind; }
	public static void init() {
		if(!ALL.isEmpty()) return;
		for(Kind kind:Kind.values()) for(ArmorItem.Type type:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS)) {
			if(kind==Kind.MIRROR_THREAD && type!=ArmorItem.Type.CHESTPLATE)continue;
			String material=kind.name().toLowerCase(Locale.ROOT);
			String path=kind==Kind.MIRROR_THREAD?"mirror_thread_mantle":material+"_"+type.getName();
			var key=ResourceKey.create(Registries.ITEM,Wildercord.id(path));
			var armor=McArmor.material(Map.of(ArmorItem.Type.HELMET,2,ArmorItem.Type.CHESTPLATE,4,ArmorItem.Type.LEGGINGS,3,ArmorItem.Type.BOOTS,1),12,0,kind==Kind.STONEBOUND?.10F:0,null,
				dev.wildercord.compat.McSpawnEgg.armorModel("wildercord:"+(kind==Kind.MIRROR_THREAD?"mirror_thread":material)));
			var properties=new Item.Properties().rarity(Rarity.UNCOMMON);
			if(kind==Kind.STONEBOUND) {
				var modifiers=new ArrayList<>(List.of(
					new ItemAttributeModifiers.Entry(Attributes.ARMOR, new AttributeModifier(Wildercord.id("stonebound_defense_"+type.getName()), armor.value().defense().get(type), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.bySlot(type.getSlot())),
					new ItemAttributeModifiers.Entry(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(Wildercord.id("stonebound_knockback_"+type.getName()), .10, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.bySlot(type.getSlot()))));
				modifiers.add(new ItemAttributeModifiers.Entry(Attributes.MOVEMENT_SPEED,
					new AttributeModifier(Wildercord.id("stonebound_"+type.getName()),-.03,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),EquipmentSlotGroup.bySlot(type.getSlot())));
				properties.attributes(new ItemAttributeModifiers(modifiers, false));
			}
			ALL.add(Registry.register(BuiltInRegistries.ITEM,key,new ElementalArmor(kind,armor,type,properties)));
		}
	}
	public static int count(net.minecraft.world.entity.LivingEntity holder,Kind kind) {
		int n=0;
		for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))
			if(holder.getItemBySlot(slot).getItem() instanceof ElementalArmor armor && armor.kind==kind)n++;
		return n;
	}
	@Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> wildercord$lines,TooltipFlag flag) {
		Consumer<Component> text = wildercord$lines::add;
		text.accept(Component.translatable("tooltip.wildercord.armor."+kind.name().toLowerCase(Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
