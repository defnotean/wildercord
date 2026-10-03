package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.List;
/** Player-built habitat and Tideward field lore. */
public final class WetlandShelters {
 private static final ResourceKey<Block> BLOCK=ResourceKey.create(Registries.BLOCK,Wildercord.id("reed_refuge"));
 public static final ReedRefugeBlock REFUGE=Registry.register(BuiltInRegistries.BLOCK,BLOCK,new ReedRefugeBlock(BlockBehaviour.Properties.of().setId(BLOCK).strength(.8F).noOcclusion().sound(SoundType.BAMBOO)));
 private static ResourceKey<Item> key(String id) {return ResourceKey.create(Registries.ITEM,Wildercord.id(id));}
 public static final Item REFUGE_ITEM=Registry.register(BuiltInRegistries.ITEM,key("reed_refuge"),new BlockItem(REFUGE,new Item.Properties().setId(key("reed_refuge")).useBlockDescriptionPrefix()));
 public static final Item NOTES=Registry.register(BuiltInRegistries.ITEM,key("reed_roof_notes"),new WrittenBookItem(new Item.Properties().setId(key("reed_roof_notes")).stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("Under the Reed Roof"),"Tamsin of the Tideward",0,java.util.stream.IntStream.rangeClosed(1,3).mapToObj(i -> net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.translatable("book.wildercord.reed_roof."+i))).toList(),true))));
 public static void init() {CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(REFUGE_ITEM,NOTES).forEach(o::accept));}
}
