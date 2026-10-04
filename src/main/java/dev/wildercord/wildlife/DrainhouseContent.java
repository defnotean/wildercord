package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import java.util.*;
/** A three-room cave investigation with finite restoration and two lasting fieldcraft rewards. */
public final class DrainhouseContent {
 private DrainhouseContent() {}
 private static ResourceKey<Block> b(String n){return ResourceKey.create(Registries.BLOCK,Wildercord.id(n));}
 private static ResourceKey<Item> i(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 public static final DrainhouseMark MARK=Registry.register(BuiltInRegistries.BLOCK,b("drainhouse_mark"),new DrainhouseMark(BlockBehaviour.Properties.of().setId(b("drainhouse_mark")).strength(-1,3600000).noLootTable().noOcclusion().sound(SoundType.COPPER).lightLevel(s -> s.getValue(DrainhouseMark.RESTORED)?3:0)));
 public static final BlockEntityType<DrainhouseMarkEntity> MARK_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildercord.id("drainhouse_mark"),FabricBlockEntityTypeBuilder.create(DrainhouseMarkEntity::new,MARK).build());
 public static final AttachmentType<Long> REPAIR_READY=AttachmentRegistry.create(Wildercord.id("drainhouse_repair_ready"),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());
 private static Item journal(String n,String title,int text){return Registry.register(BuiltInRegistries.ITEM,i(n),new WrittenBookItem(new Item.Properties().setId(i(n)).stacksTo(1).component(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough(title),"Mara, Belowkeeper",0,java.util.stream.IntStream.rangeClosed(1,3).mapToObj(page -> net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.translatable("book.wildercord.drainhouse."+text+"."+page))).toList(),true))));}
 public static final Item ENTRANCE_NOTES=journal("drainhouse_threshold","The Borrowed Third Breath",0),GARDEN_NOTES=journal("drainhouse_garden","Six Feet at the Table",1),ALCOVE_NOTES=journal("drainhouse_alcove","What the Empty Bell Keeps",2);
 public static void init(){
  BelowkeeperEquipment.init();Registry.register(BuiltInRegistries.FEATURE_TYPE,Wildercord.id("belowkeeper_drainhouse"),DrainhouseFeature.CODEC);
  BiomeModifications.addFeature(BiomeSelectors.includeByKey(Set.of(Biomes.LUSH_CAVES,Biomes.DRIPSTONE_CAVES)),GenerationStep.Decoration.TOP_LAYER_MODIFICATION,ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("belowkeeper_drainhouse")));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(ENTRANCE_NOTES,GARDEN_NOTES,ALCOVE_NOTES).forEach(o::accept));
 }
}
