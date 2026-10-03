package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Highland agriculture and tools: native effects save/sync normally, with no player/world cache. */
public final class HighlandContent {
	private HighlandContent() {}
	private static final ResourceKey<Block> KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("windreed"));
	public static final WindreedBlock REED=Registry.register(BuiltInRegistries.BLOCK,KEY,new WindreedBlock(BlockBehaviour.Properties.of().setId(KEY).noCollision().noOcclusion().instabreak().randomTicks().sound(SoundType.GRASS)));
	public static final Item WINDREED=Registry.register(BuiltInRegistries.ITEM,ResourceKey.create(Registries.ITEM,Wildercord.id("windreed")),
		new BlockItem(REED,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Wildercord.id("windreed"))).useBlockDescriptionPrefix()));
	public static final Holder<MobEffect> DOWNWIND=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Wildercord.id("downwind"),
		new MobEffect(MobEffectCategory.BENEFICIAL,0x9FAE83) {}.addAttributeModifier(Attributes.MOVEMENT_SPEED,Wildercord.id("downwind_pace"),-.10,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final Item DRAFT_KITE=tool("draft_kite",true), WINDREED_BRAID=tool("windreed_braid",false);
	public static final AttachmentType<Long> KITE_READY=AttachmentRegistry.create(Wildercord.id("draft_kite_ready"),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());
	public static final AttachmentType<Long> BRAID_READY=AttachmentRegistry.create(Wildercord.id("windreed_braid_ready"),b -> b.initializer(() -> 0L).persistent(com.mojang.serialization.Codec.LONG).copyOnDeath());
	private static long clock(ServerPlayer p) {return p.level().getServer().overworld().getGameTime();}
	public static boolean quiet(LivingEntity who) {return who.hasEffect(DOWNWIND) && !who.isSprinting();}
	private static Item tool(String id,boolean kite) {
		var key=ResourceKey.create(Registries.ITEM,Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM,key,new AuraWorld.Lore(id,new Item.Properties().setId(key).stacksTo(kite?1:16)) {
			@Override public InteractionResult use(Level l,Player p,InteractionHand hand) {
				var stack=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(stack))return InteractionResult.PASS;
				if(l instanceof ServerLevel server) {
					long now=p instanceof ServerPlayer sp?clock(sp):server.getGameTime();
					var ready=kite?KITE_READY:BRAID_READY;
					if(now<p.getAttachedOrElse(ready,0L))return InteractionResult.PASS;
					if(kite) {
						ItemStack fuel=ItemStack.EMPTY;
						for(int i=0;i<p.getInventory().getContainerSize();i++) {var slot=p.getInventory().getItem(i);if(slot.is(WINDREED)) {fuel=slot;break;}}
						if(fuel.isEmpty() && !p.getAbilities().instabuild) {p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.draft_kite.fuel"));return InteractionResult.FAIL;}
						if(!p.getAbilities().instabuild)fuel.shrink(1);
						p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,160,0,false,true));p.getCooldowns().addCooldown(stack,400);
						p.setAttached(ready,now+400);
						Feels.sound(server,p.position(),"highland_kite_open",.75F,1);
					} else {
						p.addEffect(new MobEffectInstance(DOWNWIND,600,0,false,true));p.getCooldowns().addCooldown(stack,600);
						p.setAttached(ready,now+600);
						if(!p.getAbilities().instabuild)stack.shrink(1);Feels.sound(server,p.position(),"highland_braid_rustle",.55F,1);
					}
				}
				return InteractionResult.SUCCESS;
			}
		});
	}
	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
			var p=handler.player;long now=clock(p);
			for(var pair:List.of(Map.entry(DRAFT_KITE,KITE_READY),Map.entry(WINDREED_BRAID,BRAID_READY))) {
				long left=p.getAttachedOrElse(pair.getValue(),0L)-now;
				if(left>0)p.getCooldowns().addCooldown(new ItemStack(pair.getKey()),(int)Math.min(left,600));
			}
		});
		var feature=ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("windreed_patch"));
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(Set.of(net.minecraft.world.level.biome.Biomes.MEADOW,net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS,net.minecraft.world.level.biome.Biomes.WINDSWEPT_FOREST,net.minecraft.world.level.biome.Biomes.WINDSWEPT_GRAVELLY_HILLS)),GenerationStep.Decoration.VEGETAL_DECORATION,feature);
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> List.of(WINDREED,DRAFT_KITE,WINDREED_BRAID).forEach(o::accept));
		WildercordEvents.SPELL_HIT.register((caster,targets,point,effects) -> {
			if(!(caster instanceof ServerPlayer p) || !(caster.level() instanceof ServerLevel l) || !Casters.mayBuild(p))return;
			boolean life=effects.stream().anyMatch(e -> e.element().equals("life"));
			boolean wind=effects.stream().anyMatch(e -> e.element().equals("wind"));
			if(!life && !wind)return;
			// Only the 27 loaded cells around the impact. Wind reveals fronds; Life advances their existing root.
			var center=BlockPos.containing(point);
			for(var at:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {
				if(!l.hasChunkAt(at))continue;var s=l.getBlockState(at);if(!s.is(REED))continue;
				if(life && s.getValue(WindreedBlock.AGE)<2 && Casters.mayEdit(p,l,at))l.setBlock(at,s.setValue(WindreedBlock.AGE,s.getValue(WindreedBlock.AGE)+1),Block.UPDATE_CLIENTS);
				if(wind)l.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,at.getX()+.5,at.getY()+.8,at.getZ()+.5,3,.15,.1,.15,.015);
			}
		});
	}
}
