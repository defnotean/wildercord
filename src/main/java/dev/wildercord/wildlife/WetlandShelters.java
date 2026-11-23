package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
 public static final ReedRefugeBlock REFUGE=Registry.register(BuiltInRegistries.BLOCK,BLOCK,new ReedRefugeBlock(BlockBehaviour.Properties.of()).toList(),true))));
 public static void init() {ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(REFUGE_ITEM,NOTES).forEach(o::accept));}
}
