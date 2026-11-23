package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import dev.wildercord.compat.McAttachments;
/** Functional fungal garden and investigation. Plants never spread; neither shelter nor magic prints resources. */
public final class FungalGarden {
 private FungalGarden() {}
 private static ResourceKey<Block> blockKey(String n) {return ResourceKey.create(Registries.BLOCK,Wildercord.id(n));}
 private static ResourceKey<Item> itemKey(String n) {return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 public static final GlowcapBlock GLOWCAP=Registry.register(BuiltInRegistries.BLOCK,blockKey("glowcap"),new GlowcapBlock(BlockBehaviour.Properties.of().durability(32)) {
  @Override public void onCraftedBy(ItemStack stack,Level l,Player p) {super.onCraftedBy(stack,l,p);if(p instanceof ServerPlayer sp)FungalInvestigation.crafted(sp);}
  @Override public InteractionResult use(Level l,Player p,InteractionHand hand) {
   if(p.isSpectator() || !p.isAlive() || hand!=InteractionHand.OFF_HAND)return InteractionResult.PASS;
   if(p instanceof ServerPlayer sp) {
    if(!p.hasEffect(net.minecraft.world.effect.MobEffects.POISON) || clock(sp)<sp.getAttachedOrElse(BREATHER_READY,0L)) {p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.fungal.filter_rest"), true);return InteractionResult.SUCCESS;}
    sp.setAttached(BREATHER_READY,clock(sp)+200);var stack=p.getItemInHand(hand);p.getCooldowns().addCooldown(stack,200);stack.hurtAndBreak(1,sp,dev.wildercord.compat.McHands.slot(hand));p.removeEffect(net.minecraft.world.effect.MobEffects.POISON);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,60,0));Feels.sound(((ServerLevel) sp.level()),p.position(),"fungal_filter",.5F,1);
   }return InteractionResult.SUCCESS;
  }
 });
 private static Item journal(String n,String title,int text) {return Registry.register(BuiltInRegistries.ITEM,itemKey(n),new WrittenBookItem(new Item.Properties()));
  ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(CUTTING,GILLS,NURSERY_ITEM,BREATHER,ROOT_NOTES,AIR_NOTES,CONCLUSION).forEach(o::accept));
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {var p=handler.player;long left=p.getAttachedOrElse(BREATHER_READY,0L)-clock(p);if(left>0)p.getCooldowns().addCooldown(new ItemStack(BREATHER),(int)Math.min(200,left));});
  WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {
   if(!(caster instanceof ServerPlayer p) || !Casters.mayBuild(p) || effects.stream().noneMatch(e -> e.element().equals("life")))return;
   var center=BlockPos.containing(point);for(var at:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {if(!((ServerLevel) p.level()).hasChunkAt(at) || !Casters.mayEdit(p,((ServerLevel) p.level()),at))continue;var s=((ServerLevel) p.level()).getBlockState(at);if(s.is(GLOWCAP) && s.getValue(GlowcapBlock.AGE)==0 && GlowcapBlock.conditions(((ServerLevel) p.level()),at))((ServerLevel) p.level()).setBlock(at,s.setValue(GlowcapBlock.AGE,1),Block.UPDATE_CLIENTS);}
  });
 }
}
