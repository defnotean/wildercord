package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Plants, pollinators and a field instrument. No ticking blocks, resource cache or forced chunk loads. */
public final class WetlandGarden {
 private static final ResourceKey<Block> KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("moonreed"));
 public static final MoonreedBlock REED=Registry.register(BuiltInRegistries.BLOCK,KEY,new MoonreedBlock(BlockBehaviour.Properties.of().setId(KEY).noCollision().noOcclusion().instabreak().randomTicks().sound(SoundType.GRASS).lightLevel(s -> s.getValue(MoonreedBlock.AGE)==2?6:0)));
 private static ResourceKey<Item> key(String id) {return ResourceKey.create(Registries.ITEM,Wildercord.id(id));}
 public static final Item CUTTING=Registry.register(BuiltInRegistries.ITEM,key("moonreed"),new BlockItem(REED,new Item.Properties().setId(key("moonreed")).useBlockDescriptionPrefix()));
 public static final Item FLOSS=Registry.register(BuiltInRegistries.ITEM,key("moonreed_floss"),new AuraWorld.Lore("moonreed_floss",new Item.Properties().setId(key("moonreed_floss"))));
 public static final AttachmentType<Long> LENS_READY=AttachmentRegistry.create(Wildercord.id("dewglass_lens_ready"),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());
 private static long clock(ServerPlayer p) {return p.level().getServer().overworld().getGameTime();}
 private static boolean begin(ServerPlayer p,InteractionHand hand) {
  if(p.isSpectator())return false;
  var stack=p.getItemInHand(hand);long now=clock(p);if(now<p.getAttachedOrElse(LENS_READY,0L))return false;
  p.setAttached(LENS_READY,now+100);p.getCooldowns().addCooldown(stack,100);stack.hurtAndBreak(1,p,hand.asEquipmentSlot());Feels.sound(p.level(),p.position(),"wetland_lens_focus",.4F,1);return true;
 }
 public static final Item LENS=Registry.register(BuiltInRegistries.ITEM,key("dewglass_lens"),new AuraWorld.Lore("dewglass_lens",new Item.Properties().setId(key("dewglass_lens")).durability(96)) {
  @Override public InteractionResult useOn(UseOnContext c) {
   if(!c.getLevel().getBlockState(c.getClickedPos()).is(REED))return super.useOn(c);
   if(c.getPlayer() instanceof ServerPlayer p && begin(p,c.getHand()))inspect(p,c.getClickedPos());
   return InteractionResult.SUCCESS;
  }
  @Override public InteractionResult use(Level l,Player p,InteractionHand hand) {
   if(p instanceof ServerPlayer sp && begin(sp,hand)) {
    BlockPos best=null;double distance=Double.MAX_VALUE;var center=p.blockPosition();
    for(var at:BlockPos.betweenClosed(center.offset(-8,-2,-8),center.offset(8,2,8))) {
     if(!l.hasChunkAt(at) || !l.getBlockState(at).is(REED))continue;
     double d=at.distToCenterSqr(p.position());if(d<distance && l.clip(new ClipContext(p.getEyePosition(),Vec3.atCenterOf(at),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==net.minecraft.world.phys.HitResult.Type.MISS) {distance=d;best=at.immutable();}
    }
    if(best==null)p.sendOverlayMessage(Component.translatable("message.wildercord.dewglass.none"));
    else {var a=p.getEyePosition();var b=Vec3.atCenterOf(best).add(0,.5,0);for(int i=1;i<=8;i++) {var v=a.lerp(b,i/8.0);sp.level().sendParticles(ParticleTypes.GLOW,v.x,v.y,v.z,1,0,0,0,0);}inspect(sp,best);}
   }
   return InteractionResult.SUCCESS;
  }
 });
 private static void inspect(ServerPlayer p,BlockPos at) {
  var s=p.level().getBlockState(at);int age=s.getValue(MoonreedBlock.AGE);
  String condition=age==2?"ready":!MoonreedBlock.moist(p.level(),at)?"dry":!MoonreedBlock.openSky(p.level(),at)?"covered":!WetlandRules.night(p.level().getOverworldClockTime())?"day":age==0?"growing":age==1?"moth":"ready";
  p.sendOverlayMessage(Component.translatable("message.wildercord.dewglass."+condition));
 }
 public static void init() {
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
   var p=handler.player;long left=p.getAttachedOrElse(LENS_READY,0L)-clock(p);
   if(left>0)p.getCooldowns().addCooldown(new ItemStack(LENS),(int)Math.min(left,100));
  });
  Registry.register(BuiltInRegistries.FEATURE_TYPE,Wildercord.id("moonreed_patch"),MoonreedFeature.CODEC);
  BiomeModifications.addFeature(BiomeSelectors.includeByKey(Set.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP)),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("moonreed_patch")));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(CUTTING,FLOSS,LENS).forEach(o::accept));
  UseEntityCallback.EVENT.register((p,l,h,e,hit) -> {
   if(!(e instanceof LanternNewt n) || !p.getItemInHand(h).is(LENS) || p.isSpectator())return InteractionResult.PASS;
   if(p instanceof ServerPlayer sp && begin(sp,h)) {
    long left=Math.max(0,n.pearlReady()-l.getGameTime());
    p.sendOverlayMessage(Component.translatable(n.frightened()?"message.wildercord.dewglass.newt_hurt":!n.isInWaterOrRain()?"message.wildercord.dewglass.newt_dry":left>0?"message.wildercord.dewglass.newt_rest":"message.wildercord.dewglass.newt_ready",(left+19)/20));
   }
   return InteractionResult.SUCCESS;
  });
  WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {
   if(!(caster instanceof ServerPlayer p) || !Casters.mayBuild(p) || effects.stream().noneMatch(e -> e.element().equals("life")))return;
   var center=BlockPos.containing(point);
   for(var at:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {
    if(!p.level().hasChunkAt(at))continue;var s=p.level().getBlockState(at);
    if(s.is(REED) && s.getValue(MoonreedBlock.AGE)==0 && MoonreedBlock.moist(p.level(),at) && Casters.mayEdit(p,p.level(),at))p.level().setBlock(at,s.setValue(MoonreedBlock.AGE,1),Block.UPDATE_CLIENTS);
   }
  });
 }
}
