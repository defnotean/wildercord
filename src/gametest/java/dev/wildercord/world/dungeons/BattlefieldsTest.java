package dev.wildercord.world.dungeons;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.world.*;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rotated real structures, persisted provenance, client use packets, cancelled memories and finite rewards. */
public final class BattlefieldsTest implements FabricClientGameTest {
	// Representative normal-terrain compatibility, not a promise that an arbitrary world's 16 sites admit generation.
	// Observed unchanged in https://github.com/defnotean/wildercord/actions/runs/37395703968/job/112051018891
	static final String REPRESENTATIVE_SEED="4424506075848880372";
	private static final BlockPos REPRESENTATIVE_HABITAT=new BlockPos(1408,70,1088);
	private static final BlockPos REPRESENTATIVE_MARKER=new BlockPos(1423,64,1103);
	@Override public void runTest(ClientGameTestContext context) {
		BattlefieldsGenerationProbeChecks.verify();
		try (var world=context.worldBuilder().create()) {
			context.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runCommand("time set 6000");world.getServer().runCommand("weather clear");
			List<BlockPos> markers=world.getServer().computeOnServer(server -> {
				server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE);
				var level=server.overworld();var result=new ArrayList<BlockPos>();int index=0;
				for (var facing:Direction.Plane.HORIZONTAL) for (int kind=0;kind<3;kind++) {
					int x=index++*80;OldBattlefieldPiece piece;
					do { piece=new OldBattlefieldPiece(x++,160,128,facing); } while (piece.variant()!=kind);
					var bounds=piece.getBoundingBox();
					for (int cx=bounds.minX()>>4;cx<=bounds.maxX()>>4;cx++) for (int cz=bounds.minZ()>>4;cz<=bounds.maxZ()>>4;cz++) level.getChunk(cx,cz);
					for(int bx=bounds.minX();bx<=bounds.maxX();bx++) for(int bz=bounds.minZ();bz<=bounds.maxZ();bz++)
						{
							level.setBlock(new BlockPos(bx,159,bz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
							level.setBlock(new BlockPos(bx,160,bz),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);
						}
					piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(932),bounds,
						new ChunkPos(bounds.minX()>>4,bounds.minZ()>>4),BlockPos.ZERO);
					var at=piece.localPosition(15,1,15);
					check(level.getBlockState(at).getValue(BattlefieldMemorial.KIND)==kind,"Marker kind survives rotation");
					var marker=(BattlefieldMemoryEntity)level.getBlockEntity(at);check(marker.oldGround(),"Generation sets authentic memory");
					var restored=BlockEntity.loadStatic(at,marker.getBlockState(),marker.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
					check(restored instanceof BattlefieldMemoryEntity m && m.oldGround(),"Provenance survives save/load");
					if (result.size()<3) result.add(at);
				}
				return result;
			});
			context.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();
				mc.options.toggleCrouch().set(false);mc.options.setCameraType(CameraType.FIRST_PERSON);});
			for (int kind=0;kind<3;kind++) {
				var at=markers.get(kind);int memoryKind=kind;
				context.runOnClient(mc -> mc.options.keyShift.setDown(false));context.waitTicks(15);
				world.getServer().runOnServer(server -> {
					var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
					p.teleportTo(p.level(),at.getX()+.5,at.getY(),at.getZ()-1.5,Set.<Relative>of(),0,12,false);
					p.fallDistance=0;
					p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",AuraRules.GLOW,0,20,0));
					p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
				});
				context.waitTicks(20);context.runOnClient(mc -> mc.options.keyShift.setDown(true));context.waitTicks(50);
				world.getServer().runOnServer(server -> {
					var p=server.getPlayerList().getPlayers().getFirst();check(Aura.state(p).breathing(),"Player actually breathes at the marker: "+p.position());
				});
				if (kind==0) {
					use(context,at);context.waitTicks(40);context.runOnClient(mc -> mc.options.keyShift.setDown(false));context.waitTicks(20);
					world.getServer().runOnServer(server -> check(!Heart.discovered(server.getPlayerList().getPlayers().getFirst(),BattlefieldRules.memory(0).key()),"Standing cancels without discovery"));
					context.runOnClient(mc -> mc.options.keyShift.setDown(true));context.waitTicks(50);
				}
				use(context,at);context.waitTicks(80);shot(context,"battlefield_"+kind+"_listening");context.waitTicks(100);
				world.getServer().runOnServer(server -> {
					var p=server.getPlayerList().getPlayers().getFirst();
					check(Heart.discovered(p,BattlefieldRules.memory(memoryKind).key()),"Memory discovery persists on the player");
					check(AuraApi.knowsPart(p,BattlefieldRules.memory(memoryKind).part()),"The memory teaches its actual technique intent");
					check(books(p)==memoryKind+1,"Each tradition gives one readable book");
				});
				use(context,at);context.waitTicks(180);
				world.getServer().runOnServer(server -> check(books(server.getPlayerList().getPlayers().getFirst())==memoryKind+1,"Repeating cannot mint books"));
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));shot(context,"battlefield_"+kind+"_ruin");
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			}
			// A new, unprovenanced marker refuses the same interaction even in the correct stance.
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();var at=p.blockPosition().offset(2,0,2);
				p.level().setBlockAndUpdate(at,Battlefields.MEMORIAL.defaultBlockState());
				check(!((BattlefieldMemoryEntity)p.level().getBlockEntity(at)).oldGround(),"Newly placed markers have no reward provenance");
				Battlefields.begin(p,at);
			});
			context.runOnClient(mc -> mc.options.keyShift.setDown(false));
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,Battlefields.book(1)));
			context.waitTicks(15);
			context.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));
			context.waitTicks(10);
			context.runOnClient(mc -> check(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen,"Recovered book opens the actual reading screen"));
			shot(context,"battlefield_lore_book");
		}
		dev.wildercord.Wildercord.LOGGER.info("BATTLEFIELD_GENERATION PRIOR_CHECKS completed=12_rotated_variants,persisted_provenance,standing_cancellation,three_discoveries,finite_repeat_rewards,unprovenanced_marker_false,book_screen");
		try (var natural=context.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed(REPRESENTATIVE_SEED)).create();
			 var probe=natural.getServer().computeOnServer(server -> BattlefieldsGenerationProbe.begin(server.overworld().getSeed()))) {
			check(probe.seed==Long.parseLong(REPRESENTATIVE_SEED),"Representative normal-world seed is applied exactly");
			context.waitTicks(50);natural.getServer().runCommand("gamerule spawn_mobs false");natural.getServer().runCommand("time set 6000");
			natural.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
			BlockPos found=null;
			BlockPos habitat=natural.getServer().computeOnServer(server -> {
				var nearest=server.overworld().findClosestBiome3d(b -> b.is(net.minecraft.world.level.biome.Biomes.PLAINS),new BlockPos(1024,70,1024),6400,32,64);
				check(nearest!=null,"Normal world contains the battlefield's plains habitat");
				var registered=server.overworld().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
					.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,net.minecraft.resources.Identifier.parse("wildercord:old_battlefield")));
				probe.habitat(nearest.getFirst(),nearest.getSecond().unwrapKey().map(key -> key.identifier().toString()).orElse("unregistered"),
					registered.value().getClass().getName(),registered.value().biomes().contains(nearest.getSecond()));
				return nearest.getFirst();
			});
			check(habitat.equals(REPRESENTATIVE_HABITAT),"Representative plains sample is unchanged: "+habitat);
			for (int attempt=0;attempt<16 && found==null;attempt++) {
				int x=habitat.getX()+(attempt%4)*48;
				int z=habitat.getZ()+(attempt/4)*48;
				int loadX=x;
				natural.getServer().runOnServer(server -> {
					for(int cx=(loadX>>4)-3;cx<=(loadX>>4)+3;cx++) for(int cz=(z>>4)-3;cz<=(z>>4)+3;cz++) server.overworld().getChunk(cx,cz);
				});
				int ordinal=attempt;
				var command=natural.getServer().computeOnServer(server -> {
					try (var receipt=probe.command(server.overworld(),ordinal,x,z)) {
						// Same source, command and synchronous executor as Fabric runCommand; add only its result callback.
						server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withCallback(receipt::callback),
							"place structure wildercord:old_battlefield "+x+" 70 "+z);
						receipt.returned();return receipt;
					}
				});
				int candidateX=x;
				found=natural.getServer().computeOnServer(server -> {
					var level=server.overworld();
					for(int cx=(candidateX-48)>>4;cx<=(candidateX+48)>>4;cx++) for(int cz=(z-48)>>4;cz<=(z+48)>>4;cz++) {
						if(!level.hasChunkAt(new BlockPos(cx*16,70,cz*16)))continue;
						for(var marker:level.getChunk(cx,cz).getBlockEntities().values()) if(marker instanceof BattlefieldMemoryEntity m) {
							boolean authentic=m.oldGround();command.marker(marker.getBlockPos(),authentic);
							if(authentic) {command.scanned(marker.getBlockPos());return marker.getBlockPos();}
						}
					}
					command.scanned(null);return null;
				});
				// The known first original candidate must still work. Never hide drift by selecting a later site.
				requireRepresentativeAdmission(command.admission());
				check(REPRESENTATIVE_MARKER.equals(found),"Representative command produces its exact authentic marker, not a nearby one: "+found);
			}
			check(found!=null,"The registered structure generates an authentic memorial on normal terrain; "+probe.summary());
			BlockPos at=found;
			natural.getServer().runOnServer(server -> {
				var level=server.overworld();
				check(level.getBlockState(at).is(Battlefields.MEMORIAL),"Representative generated marker has the registered memorial block type");
				check(level.getBlockEntity(at) instanceof BattlefieldMemoryEntity m && m.oldGround(),"Exact representative memorial retains generated provenance");
				var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),at.getX()+.5,at.getY()+9,at.getZ()-14.5,Set.<Relative>of(),0,27,false);
				p.getAbilities().flying=true;p.onUpdateAbilities();
			});
			context.waitTicks(50);natural.getConnection().waitForChunksRender();shot(context,"battlefield_natural_terrain");
		}
	}
	static void requireRepresentativeAdmission(BattlefieldsGenerationProbe.Admission observed) {
		check(Boolean.TRUE.equals(observed.enabled()),"Representative generation uses the actual enabled battlefield config: "+observed);
		check("north".equals(observed.direction()) && new BlockPos(1423,0,1103).equals(observed.centre())
			&& observed.heightCalls()==6 && Integer.valueOf(63).equals(observed.surface()) && Integer.valueOf(63).equals(observed.floor())
			&& observed.neighbours().equals(List.of(62,66,63,66)) && Integer.valueOf(63).equals(observed.sea()),
			"Representative terrain and original generation RNG remain compatible: "+observed);
		check(observed.locateCalls()==1 && Boolean.TRUE.equals(observed.footing()) && Boolean.TRUE.equals(observed.locateAdmitted()),
			"Representative original footing and locate admit the registered start: "+observed);
		check(observed.commandReturned() && observed.callbackCalls()==1 && Boolean.TRUE.equals(observed.commandSuccess())
			&& Integer.valueOf(1).equals(observed.commandResult()),"Representative registered placement command succeeds: "+observed);
	}
	private static int books(net.minecraft.server.level.ServerPlayer p) {
		int n=0;for (int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).has(DataComponents.WRITTEN_BOOK_CONTENT)) n++;
		return n;
	}
	private static void use(ClientGameTestContext c,BlockPos at) {c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)));}
	private static void shot(ClientGameTestContext c,String name) {c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
	private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
}
