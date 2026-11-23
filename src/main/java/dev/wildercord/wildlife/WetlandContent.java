package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.aura.world.AuraWorld;
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
	public static final MarshlightBlock MARSHLIGHT=Registry.register(BuiltInRegistries.BLOCK,BLOCK,new MarshlightBlock(BlockBehaviour.Properties.of() -> {
			if(reason==MobSpawnType.CHUNK_GENERATION)return false;if(reason!=MobSpawnType.NATURAL)return true;if(!(l instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return false;
			var settings=Config.get().wildlife();if(!settings.spawns("lantern_newt") || r.nextDouble()>=Math.min(1,.35*settings.spawnMultiplier()))return false;
			// Shallow water over solid footing, with air above, not an ocean-depth or cave spawn.
			return l.getFluidState(p).is(FluidTags.WATER) && l.getFluidState(p.above()).isEmpty() && l.getBlockState(p.below()).isSolidRender(l, p.below())
				&& l.canSeeSky(p.above()) && WetlandQueries.populationRoom(l.getLevel(),LanternNewt.class,new AABB(p).inflate(24),3);
		});
		BiomeModifications.create(Wildercord.id("wetland_newts")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP,dev.wildercord.world.biome.ReedlandClimate.REEDLANDS)),c -> {
			var settings=Config.get().wildlife();if(settings.spawns("lantern_newt"))dev.wildercord.compat.McWorldgen.addSpawn(c.getSpawnSettings(), MobCategory.CREATURE, NEWT, 1, 2, Math.max(1,(int)(5*Math.max(1,settings.spawnMultiplier()))));
		});
		FieldGuide.add(new FieldGuide.Entry("wildercord:lantern_newt",FieldGuide.Group.WILDLIFE,0xAAD5AE));
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(DUSK_PEARL,MARSHLIGHT_ITEM,NEWT_EGG,FIELD_NOTES).forEach(o::accept));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(o -> o.accept(NEWT_EGG));
		WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {
			if(effects.stream().noneMatch(e -> e.id().equals(dev.wildercord.spell.Runes.TIDEBREATH.id()) || e.element().equals("life")))return;
			for(var target:targets)if(target instanceof LanternNewt newt)newt.answerMagic();
		});
	}
}
