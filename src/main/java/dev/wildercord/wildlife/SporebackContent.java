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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Cave wildlife, two genuine fieldcraft items and a first-observation journal. */
public final class SporebackContent {
 private SporebackContent() {}
 private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("sporeback_snail"));
 public static final EntityType<SporebackSnail> SNAIL=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,EntityType.Builder.of(SporebackSnail::new,MobCategory.CREATURE).sized(.7F,.6F).eyeHeight(.32F).clientTrackingRange(8).build(KEY));
 private static ResourceKey<Item> key(String s) {return ResourceKey.create(Registries.ITEM,Wildercord.id(s));}
 public static final Item DEW=Registry.register(BuiltInRegistries.ITEM,key("mycelial_dew"),new AuraWorld.Lore("mycelial_dew",new Item.Properties().setId(key("mycelial_dew"))));
 public static final Item POULTICE=Registry.register(BuiltInRegistries.ITEM,key("fungal_poultice"),new AuraWorld.Lore("fungal_poultice",new Item.Properties().setId(key("fungal_poultice")).stacksTo(16)) {
  @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand h) {
   if(p.isSpectator() || !p.isAlive())return net.minecraft.world.InteractionResult.PASS;
   if(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) {if(!l.isClientSide())p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.sporeback.poultice_rest"));return net.minecraft.world.InteractionResult.SUCCESS;}
   if(!l.isClientSide()) {p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,600,0));p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,120,0));if(!p.getAbilities().instabuild)p.getItemInHand(h).shrink(1);dev.wildercord.cast.feel.Feels.sound((net.minecraft.server.level.ServerLevel)l,p.position(),"sporeback_gather",.35F,.8F);}return net.minecraft.world.InteractionResult.SUCCESS;
  }
 });
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,key("sporeback_snail_spawn_egg"),new SpawnEggItem(new Item.Properties().setId(key("sporeback_snail_spawn_egg")).spawnEgg(SNAIL)));
 public static final Item JOURNAL=Registry.register(BuiltInRegistries.ITEM,key("sporeback_journal"),new WrittenBookItem(new Item.Properties().setId(key("sporeback_journal")).stacksTo(1).component(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("The Patient Spiral"),"Mara, Belowkeeper",0,java.util.stream.IntStream.rangeClosed(1,3).mapToObj(i -> net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.translatable("book.wildercord.sporeback."+i))).toList(),true))));
 public static void init() {
  FabricDefaultAttributeRegistry.register(SNAIL,SporebackSnail.attributes());
  SpawnPlacements.register(SNAIL,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,p,r) -> {
   if(reason!=EntitySpawnReason.NATURAL && reason!=EntitySpawnReason.CHUNK_GENERATION)return true;
   var settings=Config.get().wildlife();if(!settings.spawns("sporeback_snail") || r.nextDouble()>=Math.min(1,.3*settings.spawnMultiplier()))return false;
   var ground=l.getBlockState(p.below());return p.getY()<48 && !l.canSeeSky(p) && l.getMaxLocalRawBrightness(p)<=8 && l.getFluidState(p).isEmpty() && (ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.CLAY)) && SporebackRules.room(l.getEntities(type,new AABB(p).inflate(24),e -> e.isAlive()).size());
  });
  BiomeModifications.create(Wildercord.id("sporeback_caves")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.LUSH_CAVES,Biomes.DRIPSTONE_CAVES)),c -> {var s=Config.get().wildlife();if(s.spawns("sporeback_snail"))c.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(SNAIL,net.minecraft.util.valueproviders.UniformInt.of(1,1)),Math.max(1,(int)(4*Math.max(1,s.spawnMultiplier()))));});
  FieldGuide.add(new FieldGuide.Entry("wildercord:sporeback_snail",FieldGuide.Group.WILDLIFE,0xAB96C5));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(DEW,POULTICE,EGG,JOURNAL).forEach(o::accept));CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o -> o.accept(EGG));
  WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {boolean fire=effects.stream().anyMatch(e -> e.element().equals("fire"));if(!fire && effects.stream().noneMatch(e -> e.element().equals("life")))return;for(var e:targets)if(e instanceof SporebackSnail snail)snail.answerMagic(fire);});
 }
}
