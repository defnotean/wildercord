package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
import dev.wildercord.compat.McAttachments;
/** A three-room cave investigation with finite restoration and two lasting fieldcraft rewards. */
public final class DrainhouseContent {
 private DrainhouseContent() {}
 private static ResourceKey<Block> b(String n){return ResourceKey.create(Registries.BLOCK,Wildercord.id(n));}
 private static ResourceKey<Item> i(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 public static final DrainhouseMark MARK=Registry.register(BuiltInRegistries.BLOCK,b("drainhouse_mark"),new DrainhouseMark(BlockBehaviour.Properties.of());
  BiomeModifications.addFeature(BiomeSelectors.includeByKey(Set.of(Biomes.LUSH_CAVES,Biomes.DRIPSTONE_CAVES,dev.wildercord.world.biome.BelowkeeperClimate.HOLLOWS)),GenerationStep.Decoration.TOP_LAYER_MODIFICATION,ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("belowkeeper_drainhouse")));
  ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(ENTRANCE_NOTES,GARDEN_NOTES,ALCOVE_NOTES).forEach(o::accept));
 }
}
