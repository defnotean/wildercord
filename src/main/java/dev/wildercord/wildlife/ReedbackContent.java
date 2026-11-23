package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import java.util.Set;
public final class ReedbackContent {
 private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("reedback_crab"));
 public static final EntityType<ReedbackCrab> CRAB=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,EntityType.Builder.of(ReedbackCrab::new,MobCategory.CREATURE).sized(1.1F,.7F).eyeHeight(.45F).clientTrackingRange(8).build(KEY));
 private static final ResourceKey<Item> EGG_KEY=ResourceKey.create(Registries.ITEM,Wildercord.id("reedback_crab_spawn_egg"));
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,EGG_KEY,dev.wildercord.compat.McSpawnEgg.create(CRAB, new Item.Properties();});
  FieldGuide.add(new FieldGuide.Entry("wildercord:reedback_crab",FieldGuide.Group.WILDLIFE,0x87906A));
  ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(o -> o.accept(EGG));
  ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> o.accept(EGG));
  WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {boolean water=effects.stream().anyMatch(e -> e.id().equals(dev.wildercord.spell.Runes.TIDEBREATH.id())),wind=effects.stream().anyMatch(e -> e.element().equals("wind"));if(!water && !wind)return;for(var target:targets)if(target instanceof ReedbackCrab crab)crab.answerMagic(water);});
 }
}
