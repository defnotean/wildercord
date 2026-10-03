package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import java.util.*;
/** Functional fungal garden and investigation. Plants never spread; neither shelter nor magic prints resources. */
public final class FungalGarden {
 private FungalGarden() {}
 private static ResourceKey<Block> blockKey(String n) {return ResourceKey.create(Registries.BLOCK,Wildercord.id(n));}
 private static ResourceKey<Item> itemKey(String n) {return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 public static final GlowcapBlock GLOWCAP=Registry.register(BuiltInRegistries.BLOCK,blockKey("glowcap"),new GlowcapBlock(BlockBehaviour.Properties.of().setId(blockKey("glowcap")).noCollision().noOcclusion().instabreak().randomTicks().sound(SoundType.FUNGUS).lightLevel(s -> s.getValue(GlowcapBlock.AGE)==2?4:0)));
 public static final FungalNurseryBlock NURSERY=Registry.register(BuiltInRegistries.BLOCK,blockKey("fungal_nursery"),new FungalNurseryBlock(BlockBehaviour.Properties.of().setId(blockKey("fungal_nursery")).strength(.8F).noOcclusion().sound(SoundType.WOOD)));
 public static final BreathmarkBlock BREATHMARK=Registry.register(BuiltInRegistries.BLOCK,blockKey("breathmark"),new BreathmarkBlock(BlockBehaviour.Properties.of().setId(blockKey("breathmark")).strength(-1,3600000).noLootTable().noOcclusion().sound(SoundType.DEEPSLATE)));
 public static final BlockEntityType<BreathmarkEntity> BREATHMARK_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildercord.id("breathmark"),FabricBlockEntityTypeBuilder.create(BreathmarkEntity::new,BREATHMARK).build());
 public static final Item CUTTING=Registry.register(BuiltInRegistries.ITEM,itemKey("glowcap"),new BlockItem(GLOWCAP,new Item.Properties().setId(itemKey("glowcap")).useBlockDescriptionPrefix()));
 public static final Item GILLS=Registry.register(BuiltInRegistries.ITEM,itemKey("dried_glowcap_gills"),new AuraWorld.Lore("dried_glowcap_gills",new Item.Properties().setId(itemKey("dried_glowcap_gills"))));
 public static final Item NURSERY_ITEM=Registry.register(BuiltInRegistries.ITEM,itemKey("fungal_nursery"),new BlockItem(NURSERY,new Item.Properties().setId(itemKey("fungal_nursery")).useBlockDescriptionPrefix()) {
  @Override protected boolean placeBlock(BlockPlaceContext c,BlockState state) {boolean ok=super.placeBlock(c,state);if(ok && c.getPlayer() instanceof ServerPlayer p)FungalInvestigation.placed(p);return ok;}
 });
 public static final AttachmentType<Long> BREATHER_READY=AttachmentRegistry.create(Wildercord.id("cave_breather_ready"),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());
 private static long clock(ServerPlayer p) {return p.level().getServer().overworld().getGameTime();}
 public static final Item BREATHER=Registry.register(BuiltInRegistries.ITEM,itemKey("cave_breather"),new AuraWorld.Lore("cave_breather",new Item.Properties().setId(itemKey("cave_breather")).durability(32)) {
  @Override public void onCraftedBy(ItemStack stack,Player p) {super.onCraftedBy(stack,p);if(p instanceof ServerPlayer sp)FungalInvestigation.crafted(sp);}
  @Override public InteractionResult use(Level l,Player p,InteractionHand hand) {
   if(p.isSpectator() || !p.isAlive() || hand!=InteractionHand.OFF_HAND)return InteractionResult.PASS;
   if(p instanceof ServerPlayer sp) {
    if(!p.hasEffect(net.minecraft.world.effect.MobEffects.POISON) || clock(sp)<sp.getAttachedOrElse(BREATHER_READY,0L)) {p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.fungal.filter_rest"));return InteractionResult.SUCCESS;}
    sp.setAttached(BREATHER_READY,clock(sp)+200);var stack=p.getItemInHand(hand);p.getCooldowns().addCooldown(stack,200);stack.hurtAndBreak(1,sp,hand.asEquipmentSlot());p.removeEffect(net.minecraft.world.effect.MobEffects.POISON);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,60,0));Feels.sound(sp.level(),p.position(),"fungal_filter",.5F,1);
   }return InteractionResult.SUCCESS;
  }
 });
 private static Item journal(String n,String title,int text) {return Registry.register(BuiltInRegistries.ITEM,itemKey(n),new WrittenBookItem(new Item.Properties().setId(itemKey(n)).stacksTo(1).component(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough(title),"Mara, Belowkeeper",0,java.util.stream.IntStream.rangeClosed(1,3).mapToObj(i -> net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.translatable("book.wildercord.fungal."+text+"."+i))).toList(),true))));}
 public static final Item ROOT_NOTES=journal("breathmark_roots","Where the Roots Drink",0),AIR_NOTES=journal("breathmark_air","The Second Breath",1),CONCLUSION=journal("nursery_journal","Three Breathmarks",2);
 public static void init() {
  Registry.register(BuiltInRegistries.FEATURE_TYPE,Wildercord.id("glowcap_patch"),GlowcapFeature.CODEC);Registry.register(BuiltInRegistries.FEATURE_TYPE,Wildercord.id("breathmark_site"),BreathmarkFeature.CODEC);
  var biomes=BiomeSelectors.includeByKey(Set.of(Biomes.LUSH_CAVES,Biomes.DRIPSTONE_CAVES));
  BiomeModifications.addFeature(biomes,GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("glowcap_patch")));
  BiomeModifications.addFeature(biomes,GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("breathmark_site")));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(CUTTING,GILLS,NURSERY_ITEM,BREATHER,ROOT_NOTES,AIR_NOTES,CONCLUSION).forEach(o::accept));
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {var p=handler.player;long left=p.getAttachedOrElse(BREATHER_READY,0L)-clock(p);if(left>0)p.getCooldowns().addCooldown(new ItemStack(BREATHER),(int)Math.min(200,left));});
  WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {
   if(!(caster instanceof ServerPlayer p) || !Casters.mayBuild(p) || effects.stream().noneMatch(e -> e.element().equals("life")))return;
   var center=BlockPos.containing(point);for(var at:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {if(!p.level().hasChunkAt(at) || !Casters.mayEdit(p,p.level(),at))continue;var s=p.level().getBlockState(at);if(s.is(GLOWCAP) && s.getValue(GlowcapBlock.AGE)==0 && GlowcapBlock.conditions(p.level(),at))p.level().setBlock(at,s.setValue(GlowcapBlock.AGE,1),Block.UPDATE_CLIENTS);}
  });
 }
}
