package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
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
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import java.util.Set;
public final class ReedbackContent {
 private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("reedback_crab"));
 public static final EntityType<ReedbackCrab> CRAB=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,EntityType.Builder.of(ReedbackCrab::new,MobCategory.CREATURE).sized(1.1F,.7F).eyeHeight(.45F).clientTrackingRange(8).build(KEY));
 private static final ResourceKey<Item> EGG_KEY=ResourceKey.create(Registries.ITEM,Wildercord.id("reedback_crab_spawn_egg"));
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,EGG_KEY,new SpawnEggItem(new Item.Properties().setId(EGG_KEY).spawnEgg(CRAB)));
 public static void init() {
  FabricDefaultAttributeRegistry.register(CRAB,ReedbackCrab.attributes());
  SpawnPlacements.register(CRAB,SpawnPlacementTypes.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r) -> {
   if(reason!=EntitySpawnReason.NATURAL && reason!=EntitySpawnReason.CHUNK_GENERATION)return true;
   var config=Config.get().wildlife();if(!config.spawns("reedback_crab") || l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL || r.nextDouble()>=Math.min(1,.2*config.spawnMultiplier()))return false;
   if(!l.canSeeSky(p.above()) || !l.getBlockState(p.below()).isSolidRender() || !l.getBlockState(p).getCollisionShape(l,p).isEmpty() || !l.getBlockState(p.above()).isAir())return false;
   boolean wet=l.getFluidState(p).is(FluidTags.WATER);for(var d:Direction.Plane.HORIZONTAL)wet|=l.getFluidState(p.relative(d)).is(FluidTags.WATER);
   return wet && WetlandQueries.populationRoom(l.getLevel(),ReedbackCrab.class,new AABB(p).inflate(32),2);
  });
  BiomeModifications.create(Wildercord.id("reedback_banks")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP)),c -> {if(Config.get().wildlife().spawns("reedback_crab"))c.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(CRAB,net.minecraft.util.valueproviders.UniformInt.of(1,1)),Math.max(1,(int)(3*Math.max(1,Config.get().wildlife().spawnMultiplier()))));});
  FieldGuide.add(new FieldGuide.Entry("wildercord:reedback_crab",FieldGuide.Group.WILDLIFE,0x87906A));
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o -> o.accept(EGG));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> o.accept(EGG));
  WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {boolean water=effects.stream().anyMatch(e -> e.id().equals(dev.wildercord.spell.Runes.TIDEBREATH.id())),wind=effects.stream().anyMatch(e -> e.element().equals("wind"));if(!water && !wind)return;for(var target:targets)if(target instanceof ReedbackCrab crab)crab.answerMagic(water);});
 }
}
