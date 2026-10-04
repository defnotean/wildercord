package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** A wetland amphibian and nonlethal crafting loop; uses ordinary shared creature caps. */
public final class WetlandContent {
	private WetlandContent() {}
	private static final ResourceKey<EntityType<?>> ENTITY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("lantern_newt"));
	public static final EntityType<LanternNewt> NEWT=Registry.register(BuiltInRegistries.ENTITY_TYPE,ENTITY,EntityType.Builder.of(LanternNewt::new,MobCategory.CREATURE).sized(.6F,.4F).eyeHeight(.25F).clientTrackingRange(8).build(ENTITY));
	private static final ResourceKey<Block> BLOCK=ResourceKey.create(Registries.BLOCK,Wildercord.id("marshlight"));
	public static final MarshlightBlock MARSHLIGHT=Registry.register(BuiltInRegistries.BLOCK,BLOCK,new MarshlightBlock(BlockBehaviour.Properties.of().setId(BLOCK).strength(.6F).noOcclusion().lightLevel(s -> 12).sound(SoundType.BAMBOO)));
	private static ResourceKey<Item> itemKey(String name) {return ResourceKey.create(Registries.ITEM,Wildercord.id(name));}
	public static final Item DUSK_PEARL=Registry.register(BuiltInRegistries.ITEM,itemKey("dusk_pearl"),new AuraWorld.Lore("dusk_pearl",new Item.Properties().setId(itemKey("dusk_pearl")).rarity(Rarity.UNCOMMON)));
	public static final Item MARSHLIGHT_ITEM=Registry.register(BuiltInRegistries.ITEM,itemKey("marshlight"),new BlockItem(MARSHLIGHT,new Item.Properties().setId(itemKey("marshlight")).useBlockDescriptionPrefix()));
	public static final Item NEWT_EGG=Registry.register(BuiltInRegistries.ITEM,itemKey("lantern_newt_spawn_egg"),new SpawnEggItem(new Item.Properties().setId(itemKey("lantern_newt_spawn_egg")).spawnEgg(NEWT)));
	public static final Item FIELD_NOTES=Registry.register(BuiltInRegistries.ITEM,itemKey("tideward_notes"),new WrittenBookItem(new Item.Properties().setId(itemKey("tideward_notes")).stacksTo(1).component(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
		new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("Lights Along the Bank"),"Iona of the Tideward",0,
			java.util.stream.IntStream.rangeClosed(1,3).mapToObj(i -> net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.translatable("book.wildercord.tideward."+i))).toList(),true))));
	public static void init() {
		FabricDefaultAttributeRegistry.register(NEWT,LanternNewt.attributes());
		SpawnPlacements.register(NEWT,SpawnPlacementTypes.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,p,r) -> {
			if(reason==EntitySpawnReason.CHUNK_GENERATION)return false;if(reason!=EntitySpawnReason.NATURAL)return true;if(!(l instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return false;
			var settings=Config.get().wildlife();if(!settings.spawns("lantern_newt") || r.nextDouble()>=Math.min(1,.35*settings.spawnMultiplier()))return false;
			// Shallow water over solid footing, with air above, not an ocean-depth or cave spawn.
			return l.getFluidState(p).is(FluidTags.WATER) && l.getFluidState(p.above()).isEmpty() && l.getBlockState(p.below()).isSolidRender()
				&& l.canSeeSky(p.above()) && WetlandQueries.populationRoom(l.getLevel(),LanternNewt.class,new AABB(p).inflate(24),3);
		});
		BiomeModifications.create(Wildercord.id("wetland_newts")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP)),c -> {
			var settings=Config.get().wildlife();if(settings.spawns("lantern_newt"))c.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(NEWT,net.minecraft.util.valueproviders.UniformInt.of(1,2)),Math.max(1,(int)(5*Math.max(1,settings.spawnMultiplier()))));
		});
		FieldGuide.add(new FieldGuide.Entry("wildercord:lantern_newt",FieldGuide.Group.WILDLIFE,0xAAD5AE));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(DUSK_PEARL,MARSHLIGHT_ITEM,NEWT_EGG,FIELD_NOTES).forEach(o::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o -> o.accept(NEWT_EGG));
		WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {
			if(effects.stream().noneMatch(e -> e.id().equals(dev.wildercord.spell.Runes.TIDEBREATH.id()) || e.element().equals("life")))return;
			for(var target:targets)if(target instanceof LanternNewt newt)newt.answerMagic();
		});
	}
}
