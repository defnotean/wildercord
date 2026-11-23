package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** A bounded visitor event: an inhabited bell, dry flat ground and no edits to occupied blocks. */
public final class VillageTournaments {
	private VillageTournaments(){}
	private static final ResourceKey<Block> KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("tournament_board")),FLAG_KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("tournament_standard"));
	public static final TournamentBoard BOARD=Registry.register(BuiltInRegistries.BLOCK,KEY,new TournamentBoard(BlockBehaviour.Properties.of();if(!(ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT) || ground.is(Blocks.GRAVEL) || ground.is(Blocks.SAND)) || !ground.isFaceSturdy(s,p.below(),Direction.UP) || !s.getFluidState(p.below()).isEmpty())return null;
			for(int y=0;y<3;y++)if(!s.getBlockState(p.above(y)).isAir())return null;
		}
		BlockPos stand=center.offset(0,0,6);s.setBlockAndUpdate(stand,BOARD.defaultBlockState());
		if(!(s.getBlockEntity(stand) instanceof TournamentBoardEntity b))return null;
		b.host(bell);loaded(b);
		for(int x:new int[]{-8,8})for(int z:new int[]{-8,8})s.setBlockAndUpdate(center.offset(x,0,z),STANDARD.defaultBlockState());
		return b;
	}
	public static ItemStack history(){var stack=new ItemStack(Items.WRITTEN_BOOK);stackstack.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough("The Three Bows"),"The Village Stewards",0,List.of(Filterable.passThrough(Component.translatable("book.wildercord.tournament.1")),Filterable.passThrough(Component.translatable("book.wildercord.tournament.2")),Filterable.passThrough(Component.translatable("book.wildercord.tournament.3"))),true));return stack;}
	public static void init(){
		UseBlockCallback.EVENT.register((p,l,h,hit)->{if(!l.getBlockState(hit.getBlockPos()).is(BOARD))return InteractionResult.PASS;if(h==net.minecraft.world.InteractionHand.MAIN_HAND && p instanceof net.minecraft.server.level.ServerPlayer player && l.getBlockEntity(hit.getBlockPos()) instanceof TournamentBoardEntity b)b.use(player);return InteractionResult.SUCCESS;});
		ServerLifecycleEvents.SERVER_STOPPED.register(s->LOADED.clear());
		ServerTickEvents.END_SERVER_TICK.register(server->{
			if(server.getTickCount()%1200!=0 || !Config.get().auraWorld().tournaments())return;
			ServerLevel s=server.overworld();if(!(s.isDay() && s.dimensionType().hasSkyLight()) || !s.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).get() || !s.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).get())return;
			var players=s.players().stream().filter(p->!p.isSpectator()).toList();if(players.isEmpty())return;
			var random=s.getRandom();var p=players.get(random.nextInt(players.size()));
			var bell=s.getPoiManager().findClosest(t->t.is(PoiTypes.MEETING),p.blockPosition(),96,PoiManager.Occupancy.ANY).orElse(null);if(!inhabited(s,bell))return;
			var sites=LOADED.computeIfAbsent(s,k->new HashMap<>());sites.values().removeIf(BlockEntity::isRemoved);if(sites.size()>=128 || sites.values().stream().anyMatch(b->b.getBlockPos().distSqr(bell)<96*96))return;
			for(int i=0;i<24;i++){double angle=random.nextDouble()*Math.PI*2,radius=20+random.nextDouble()*24;BlockPos column=bell.offset((int)(Math.cos(angle)*radius),0,(int)(Math.sin(angle)*radius));BlockPos ground=DuelistSpawner.surface(s,column);if(ground!=null && Math.abs(ground.getY()-bell.getY())<=4 && install(s,ground,bell)!=null)return;}
		});
	}
}
