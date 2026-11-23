package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.List;

/** The tomb's authored blocks and guardian; no natural monster spawn entry. */
public final class SwordTombs {
	private SwordTombs(){}
	private static final ResourceKey<Block> GATE_KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("intent_gate")),RELIQUARY_KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("tomb_reliquary"));
	public static final IntentGate GATE=Registry.register(BuiltInRegistries.BLOCK,GATE_KEY,new IntentGate(BlockBehaviour.Properties.of();
		UseBlockCallback.EVENT.register((player,level,hand,hit)->{
			var state=level.getBlockState(hit.getBlockPos());
			if(!state.is(GATE) && !state.is(RELIQUARY))return InteractionResult.PASS;
			if(player instanceof ServerPlayer p){
				if(state.is(GATE) && dev.wildercord.config.Config.get().auraWorld().swordTombs())IntentGate.use(p,hit.getBlockPos());
				else if(level.getBlockEntity(hit.getBlockPos()) instanceof TombReliquaryEntity t)t.use(p);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
