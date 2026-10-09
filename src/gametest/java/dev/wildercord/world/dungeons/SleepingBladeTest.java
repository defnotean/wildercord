package dev.wildercord.world.dungeons;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.aura.world.*;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** Actual draw packets, interruption, unique acquisition, bond ownership, defensive tradeoff and terrain generation. */
public final class SleepingBladeTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		SleepingBladeGenerationProbeChecks.verify();
		try (var world=c.worldBuilder().create()) {
			c.waitTicks(40); world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false"); world.getServer().runCommand("time set 6000");
			BlockPos at=world.getServer().computeOnServer(s -> {
				var level=s.overworld(); int index=0; BlockPos first=null;
				for (var facing:Direction.Plane.HORIZONTAL) for (int variant=0;variant<3;variant++) {
					SleepingBladePiece piece; int x=index++*48;
					do {piece=new SleepingBladePiece(x++,160,128,facing);} while(piece.variant()!=variant);
					var bounds=piece.getBoundingBox();
					for(int cx=bounds.minX()>>4;cx<=bounds.maxX()>>4;cx++) for(int cz=bounds.minZ()>>4;cz<=bounds.maxZ()>>4;cz++)level.getChunk(cx,cz);
					for(int bx=bounds.minX();bx<=bounds.maxX();bx++)for(int bz=bounds.minZ();bz<=bounds.maxZ();bz++)level.setBlock(new BlockPos(bx,160,bz),Blocks.GRASS_BLOCK.defaultBlockState(),2);
					piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(29),bounds,new ChunkPos(bounds.minX()>>4,bounds.minZ()>>4),BlockPos.ZERO);
					var p=piece.localPosition(9,1,9);var stone=(SleepingBladeEntity)level.getBlockEntity(p);
					check(stone!=null && stone.authentic() && stone.claimedBy()==null,"All variants/orientations generate authentic dormant stones");
					var loaded=BlockEntity.loadStatic(p,stone.getBlockState(),stone.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
					check(loaded instanceof SleepingBladeEntity b && b.authentic() && b.claimedBy()==null,"Dormant provenance survives load");
					if(first==null)first=p;
				}
				return first;
			});
			c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();mc.options.toggleCrouch().set(false);mc.options.setCameraType(CameraType.FIRST_PERSON);});
			stand(world,at,AuraRules.EDGE);c.waitTicks(25);aim(c,at);c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(10);
			use(c,at);c.waitTicks(130);unclaimed(world,at,"Edge cannot draw a Form blade");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",4,0,100,0));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));});
			use(c,at);c.waitTicks(15);check(!world.getServer().computeOnServer(s->SleepingBlades.drawingAt(s.overworld(),at)),"Occupied hand cannot start a draw");
			// Existing bonds are respected; no automatic replacement of a player's chosen weapon.
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(AuraApi.bondBlade(p,InteractionHand.MAIN_HAND,"test"),"Fixture bonds an ordinary blade");var old=p.getMainHandItem();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.getInventory().setItem(8,old);});
			use(c,at);c.waitTicks(15);check(!world.getServer().computeOnServer(s->SleepingBlades.drawingAt(s.overworld(),at)),"Standing bond refuses another blade");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(BondedBlades.release(p),"Deliberate release succeeds");p.getInventory().setItem(8,ItemStack.EMPTY);});c.waitTicks(10);
			use(c,at);c.waitTicks(40);world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),p.getX()+.7,p.getY(),p.getZ(),Set.<Relative>of(),p.getYRot(),p.getXRot(),false);});c.waitTicks(15);
			unclaimed(world,at,"Movement cancels without claiming");check(!world.getServer().computeOnServer(s->SleepingBlades.drawingAt(s.overworld(),at)),"Movement removes the active draw");
			c.runOnClient(mc->mc.options.keyShift.setDown(false));stand(world,at,4);c.waitTicks(20);aim(c,at);c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(10);
			use(c,at);c.waitTicks(35);world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.hurtServer(s.overworld(),s.overworld().damageSources().generic(),1);});c.waitTicks(20);
			unclaimed(world,at,"Actual damage cancels the draw");check(!world.getServer().computeOnServer(s->SleepingBlades.drawingAt(s.overworld(),at)),"Damage removes the active draw");
			// A saved scheduled pose from an interrupted server returns to rest without duplicating a blade.
			world.getServer().runOnServer(s->{var stone=(SleepingBladeEntity)s.overworld().getBlockEntity(at);stone.phase(3);s.overworld().scheduleTick(at,SleepingBlades.STONE,1);});c.waitTicks(10);
			check(world.getServer().computeOnServer(s->s.overworld().getBlockState(at).getValue(SleepingBladeStone.PHASE)==0),"Orphan lift pose resets on its scheduled tick");
			use(c,at);c.waitTicks(55);check(world.getServer().computeOnServer(s->s.overworld().getBlockState(at).getValue(SleepingBladeStone.PHASE)>0),"Steel visibly lifts during the actual draw");
			world.getServer().runOnServer(s->{var rival=new Rival(s.overworld());rival.snapTo(at.getX()+.5,at.getY(),at.getZ()-1.7,0,15);rival.setOnGround(true);rival.setShiftKeyDown(true);rival.setGameMode(GameType.SURVIVAL);rival.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",4,0,100,0));SleepingBlades.begin(rival,at);check(SleepingBlades.activeDraws()==1,"Another qualified swordsman cannot start a concurrent draw at the same site");});
			c.runOnClient(mc->mc.player.setXRot(0));shot(c,"sleeping_blade_lifting");
			c.waitTicks(85);
			world.getServer().runOnServer(s->{
				var p=s.getPlayerList().getPlayers().getFirst();var stone=(SleepingBladeEntity)s.overworld().getBlockEntity(at);var bond=BondedBlades.bond(p.getMainHandItem());
				check(stone.claimedBy().equals(p.getUUID()) && stone.getBlockState().getValue(SleepingBladeStone.PHASE)==4,"Stone permanently records its one owner and empty pose");
				check(p.getMainHandItem().is(SleepingBlades.BLADE) && bond!=null && bond.ownedBy(p.getUUID()),"Draw places the real custom bonded weapon in hand");
				check(bond.origin().how().equals("sleeping_blade") && bond.growth().resonance()>=119,"The bond records its origin and modest resonance head start");
				check(blades(p)==1 && books(p)==1 && Heart.discovered(p,"aura:sleeping_blade"),"Exactly one blade, one lore book and a saved discovery");
				var restored=(SleepingBladeEntity)BlockEntity.loadStatic(at,stone.getBlockState(),stone.saveWithFullMetadata(s.registryAccess()),s.registryAccess());
				check(restored.claimedBy().equals(p.getUUID()),"Claim owner survives serialization");s.overworld().setBlockEntity(restored);
			});
			use(c,at);c.waitTicks(130);world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(blades(p)==1 && books(p)==1,"Reloaded site cannot mint a second blade or book");});
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var rival=new Rival(s.overworld());rival.snapTo(p.getX(),p.getY(),p.getZ(),0,0);rival.setGameMode(GameType.SURVIVAL);check(!BondedBlades.mayTake(rival,p.getMainHandItem()),"Another survival player cannot take the acquired bonded blade");SleepingBlades.begin(rival,at);check(blades(rival)==0 && books(rival)==1,"Later visitor receives history, not a second blade");SleepingBlades.begin(rival,at);check(books(rival)==1,"History discovery remains finite for the other visitor");});
			c.runOnClient(mc->{mc.options.keyShift.setDown(false);mc.player.setXRot(0);});c.waitTicks(10);shot(c,"sleeping_blade_drawn_first_person");c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));shot(c,"sleeping_blade_drawn_third_person");c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.keyShift.setDown(true);});c.waitTicks(10);
			// The acquired weapon participates in real Aura Guard harm handling, not just a tooltip.
			world.getServer().runOnServer(s->{
				var p=s.getPlayerList().getPlayers().getFirst();var blade=p.getMainHandItem();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.getInventory().setItem(8,blade);
				p.setAttached(AuraAttachments.STATE,AuraAttachments.State.NONE);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",4,0,100,0));
				check(AuraGuard.raise(p),"Ordinary sword raises guard");double ordinary=100-Aura.aura(p);
				p.getInventory().setItem(8,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,blade);
				p.setAttached(AuraAttachments.STATE,AuraAttachments.State.NONE);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",4,0,100,0));
				check(AuraGuard.raise(p),"Oathkeeper raises guard");check(Math.abs((100-Aura.aura(p))-ordinary*.85)<.001,"Actual guard raise costs 15% less");
				Momentum.reset(p);p.setYRot(0);p.setXRot(0);var enemy=EntityTypes.ZOMBIE.create(s.overworld(),EntitySpawnReason.COMMAND);enemy.snapTo(p.getX(),p.getY(),p.getZ()+2,0,0);enemy.setNoAi(true);s.overworld().addFreshEntity(enemy);
				float health=p.getHealth();p.hurtServer(s.overworld(),s.overworld().damageSources().mobAttack(enemy),4);
				check(p.getHealth()==health,"Actual perfect guard turns the blow aside");check(Math.abs(Momentum.value(p)-MomentumRules.PERFECT_GUARD*MomentumRules.temper("stone").guard()*1.3)<.01,"Perfect guard actually builds 30% more momentum");
				var guardPaid=new double[]{-1};var playerId=p.getUUID();AuraApi.onSpend((who,paid,reason,backlash)->{if(who.getUUID().equals(playerId) && reason.equals("guard"))guardPaid[0]=paid;});
				p.setInvulnerableTime(0);p.hurtServer(s.overworld(),s.overworld().damageSources().mobAttack(enemy),4);
				double absorbed=4*dev.wildercord.config.Config.get().aura().guardShare();
				check(p.getHealth()<health,"Held guard still admits damage after the perfect moment");
				check(Math.abs(guardPaid[0]-absorbed*AuraRules.GUARD_COST_PER_POINT*.85)<.01,"Actual held guard pays the reduced cost for absorbed damage: "+guardPaid[0]);
				enemy.discard();Momentum.reset(p);check(Math.abs(Momentum.add(p,10,"hit",100)-8)<.01,"Ordinary-hit momentum has the stated tradeoff");
			});shot(c,"sleeping_blade_perfect_guard");
			// A newly placed socket cannot issue the legendary item, even with the right intent.
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var fresh=p.blockPosition().offset(2,0,0);s.overworld().setBlock(fresh,SleepingBlades.STONE.defaultBlockState(),3);check(!((SleepingBladeEntity)s.overworld().getBlockEntity(fresh)).authentic(),"New sockets have no provenance");SleepingBlades.begin(p,fresh);});
			c.runOnClient(mc->mc.options.keyShift.setDown(false));world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var blade=p.getMainHandItem();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.getInventory().setItem(8,blade);for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.has(DataComponents.WRITTEN_BOOK_CONTENT)){p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.getInventory().setItem(i,ItemStack.EMPTY);break;}}});
			c.waitTicks(10);c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(10);c.runOnClient(mc->check(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen,"Actual awarded lore opens for reading"));shot(c,"sleeping_blade_last_oath");
		}
		dev.wildercord.Wildercord.LOGGER.info("SLEEPING_BLADE_GENERATION PRIOR_CHECKS completed=12_rotated_variants,persisted_provenance,draw_interruptions,unique_claim,bond_ownership,guard_tradeoff,unprovenanced_socket_false,book_screen");
		try(var natural=c.worldBuilder().setUseConsistentSettings(false).create();
			var probe=natural.getServer().computeOnServer(s->SleepingBladeGenerationProbe.begin(s.overworld().getSeed()))) {
			c.waitTicks(40);natural.getServer().runCommand("gamerule spawn_mobs false");natural.getServer().runCommand("time set 6000");
			BlockPos habitat=natural.getServer().computeOnServer(s->{
				var cs=s.overworld().getChunkSource();var b=cs.getGenerator().getBiomeSource().findBiomeHorizontal(1024,128,1024,6400,32,v->v.is(net.minecraft.world.level.biome.Biomes.PLAINS),RandomSource.create(77),true,cs.randomState());
				check(b!=null,"Normal world contains a dry landmark habitat");
				var registered=s.overworld().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
					.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,net.minecraft.resources.Identifier.parse("wildercord:sleeping_blade")));
				probe.habitat(b.getFirst(),b.getSecond().unwrapKey().map(key->key.identifier().toString()).orElse("unregistered"),
					registered.map(value->value.value().getClass().getName()).orElse("unregistered"),registered.map(value->value.value().biomes().contains(b.getSecond())).orElse(false));
				return b.getFirst();
			});
			BlockPos found=null;
			for(int i=0;i<16 && found==null;i++) {
				int x=habitat.getX()+(i%4)*48,z=habitat.getZ()+(i/4)*48;
				natural.getServer().runOnServer(s->{for(int cx=(x>>4)-3;cx<=(x>>4)+3;cx++)for(int cz=(z>>4)-3;cz<=(z>>4)+3;cz++)s.overworld().getChunk(cx,cz);});
				int ordinal=i;
				var command=natural.getServer().computeOnServer(s->{
					try(var receipt=probe.command(s.overworld(),ordinal,x,z)) {
						// Fabric runCommand uses this same source and synchronous executor; only add the callback.
						s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withCallback(receipt::callback),"place structure wildercord:sleeping_blade "+x+" 70 "+z);
						receipt.returned();return receipt;
					}
				});
				found=natural.getServer().computeOnServer(s->{
					for(int cx=(x>>4)-3;cx<=(x>>4)+3;cx++)for(int cz=(z>>4)-3;cz<=(z>>4)+3;cz++)
						for(var be:s.overworld().getChunk(cx,cz).getBlockEntities().values())if(be instanceof SleepingBladeEntity b) {
							boolean authentic=b.authentic();command.marker(b.getBlockPos(),authentic);
							if(authentic) {command.scanned(b.getBlockPos());return b.getBlockPos();}
						}
					command.scanned(null);return null;
				});
			}
			check(found!=null,"Registered normal-terrain structure placement creates the authentic blade; "+probe.summary());BlockPos landmark=found;
			natural.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),landmark.getX()+4,landmark.getY()+3,landmark.getZ()+5,Set.<Relative>of(),0,0,false);p.getAbilities().flying=true;p.onUpdateAbilities();});
			c.waitTicks(30);aim(c,landmark);c.waitTicks(30);natural.getConnection().waitForChunksRender();shot(c,"sleeping_blade_natural_landmark");
		}
	}
	private static void stand(TestSingleplayerContext w,BlockPos at,int stage) {
		w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);Ways.set(p,"banner");p.teleportTo(s.overworld(),at.getX()+.5,at.getY(),at.getZ()-1.7,Set.<Relative>of(),0,15,false);p.setHealth(20);p.fallDistance=0;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",stage,0,100,0));});
	}
	private static void unclaimed(TestSingleplayerContext w,BlockPos at,String why) {check(w.getServer().computeOnServer(s->((SleepingBladeEntity)s.overworld().getBlockEntity(at)).claimedBy()==null),why);}
	private static int blades(net.minecraft.server.level.ServerPlayer p) {int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(SleepingBlades.BLADE))n+=p.getInventory().getItem(i).getCount();return n;}
	private static int books(net.minecraft.server.level.ServerPlayer p) {int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).has(DataComponents.WRITTEN_BOOK_CONTENT))n++;return n;}
	private static void use(ClientGameTestContext c,BlockPos at) {c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)));}
	private static void aim(ClientGameTestContext c,BlockPos at) {c.runOnClient(mc->{var d=Vec3.atCenterOf(at).subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));});}
	private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.waitTicks(1);c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
	private static final class Rival extends net.fabricmc.fabric.api.entity.FakePlayer {
		Rival(net.minecraft.server.level.ServerLevel level) { super(level,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("sleeping-blade-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"Rival")); }
	}
	private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
}
