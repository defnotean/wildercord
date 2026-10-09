package dev.wildercord.aura;

import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.ScrollSpell;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/** Native regressions for loaded-only ward reads, full movement leases and the bounded art-recovery contract. */
public final class WallTurnCommitmentTest implements FabricClientGameTest {
	private long sequence = 100;
	private static final class Reloaded extends net.fabricmc.fabric.api.entity.FakePlayer {
		Reloaded(net.minecraft.server.level.ServerLevel level) { super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "LandingReload")); }
	}
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	@Override public void runTest(ClientGameTestContext context) {
		WallRelayChecks.run(context);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.WallTurnCommitmentTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				var p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -16; x <= 48; x++) for (int z = -8; z <= 8; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 108; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				for (int y = 100; y <= 106; y++) for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE.defaultBlockState());
				prepare(p);
				var chunk = p.level().getChunkSource().getChunkNow(0, 0);
				check(chunk != null, "The body chunk is already FULL");
				var original = new HashMap<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet>();
				chunk.getAllReferences().forEach((key, value) -> original.put(key, new it.unimi.dsi.fastutil.longs.LongOpenHashSet(value)));
				var structure = p.level().registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("wildercord:sword_tomb"))).value();
				int far = 10000;
				check(p.level().getChunkSource().getChunk(far, far, ChunkStatus.STRUCTURE_STARTS, false) == null, "Referenced start is actually unloaded");
				try {
					chunk.addReferenceForStructure(structure, ChunkPos.pack(far, far));
					check(!DungeonWards.movementWard(p.level(), p.blockPosition()).known(), "An unloaded start is explicit UNKNOWN, never clear or warded");
					check(WallTurn.accept(p) == null, "Unknown admission refuses bracing before payment");
					check(p.level().getChunkSource().getChunk(far, far, ChunkStatus.STRUCTURE_STARTS, false) == null,
						"The ward query did not request or load even a STRUCTURE_STARTS chunk");
				} finally { chunk.setAllReferences(original); }
			});
			context.waitTicks(2);

			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				SpellCaster.cast(p, 99);
				check(MasterForms.ownsMotion(p), "An invalid spell-slot request cannot cancel a brace");
				SpellCaster.cast(p, 0);
				check(!MasterForms.ownsMotion(p) && MasterForms.committed(p), "An admitted paid spell replaces motion but retains Aura-art commitment");
				check(!MastersArts.activate(p, 0) && !AuraStep.step(p), "Neither a fixed Master art nor Aura Step bypasses the descent commitment");
			});
			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				Charging.request(p, 99, true);
				check(MasterForms.ownsMotion(p), "Invalid charging admission cannot cancel the brace");
				Charging.request(p, 0, true);
				check(p.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE) && !MasterForms.ownsMotion(p), "A valid ordinary charge replaces active motion");
				Charging.interrupt(p);
			});
			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var scroll = new ItemStack(WildercordItems.SPELL_SCROLL);
				scroll.set(WildercordComponents.SCROLL, new ScrollSpell(List.of(Runes.SELF.id(), Runes.HEAL.id()), "Ordinary offhand", "Fixture"));
				p.setItemInHand(InteractionHand.OFF_HAND, scroll);
				WildercordItems.SPELL_SCROLL.use(p.level(), p, InteractionHand.OFF_HAND);
				check(!MasterForms.ownsMotion(p) && MasterForms.committed(p), "A real offhand scroll can replace motion without removing landing recovery");
			});

			context.waitTicks(12);
			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
				check(!MasterForms.ownsMotion(p) && MasterForms.committed(p), "An actual admitted vanilla sword swing replaces physical motion without erasing recovery");
			});

			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				apply(p, p, Runes.ROOT, false);
				check(MasterForms.ownsMotion(p), "A root denied by actual target admission leaves the brace intact");
				var enemy = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.COMMAND);
				check(enemy != null, "The hostile control source exists"); enemy.setNoAi(true); enemy.snapTo(6, 100, 2, 0, 0); p.level().addFreshEntity(enemy);
				apply(enemy, p, Runes.ROOT, false); apply(p, p, Runes.CLEANSE, true);
				check(!p.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) && !MasterForms.ownsMotion(p),
					"Actual admitted root then cleanse in the same call cannot resurrect bracing");
				enemy.discard();
			});
			for (var mark : List.of(Reactions.Mark.FROZEN, Reactions.Mark.AIRBORNE)) {
				brace(context, world);
				world.getServer().runOnServer(server -> {
					var p = server.getPlayerList().getPlayers().getFirst(); Reactions.mark(p, mark, 20); Reactions.clear(p, mark);
					check(!Reactions.has(p, mark) && !MasterForms.ownsMotion(p), "An admitted " + mark + " then clear permanently retires the physical phase");
				});
			}

			for (RuneDef movement : List.of(Runes.DASH, Runes.GRAPPLE)) {
				world.getServer().runOnServer(server -> {
					var p = server.getPlayerList().getPlayers().getFirst(); prepare(p);
					MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence));
				});
				context.waitTicks(1);
				world.getServer().runOnServer(server -> {
					var p = server.getPlayerList().getPlayers().getFirst();
					var node = SpellCompiler.compile(List.of(Runes.SELF, movement)).root().groups.getFirst().effects.getFirst();
					Effects.apply(new Cast(p), node, new Cast.Hit(List.of(p), p.position().add(8, 1, 0), new Vec3(1, 0, 0), p.position(), null, null, true));
					check(MasterFormMovement.occupied(p), "Existing " + movement.path() + " reserves its entire scheduled movement lifetime before a callback");
					check(!MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, MasterForms.view(p).epoch(), ++sequence)) && Aura.aura(p) == 160,
						"An actual fresh form request cannot join a preexisting movement owner or spend Aura");
				});
				context.waitTicks(movement == Runes.GRAPPLE ? 29 : 4);
				world.getServer().runOnServer(server -> check(MasterFormMovement.occupied(server.getPlayerList().getPlayers().getFirst()),
					"The movement lease remains before its last native callback"));
				context.waitTicks(5);
			}

			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); MasterForms.cancel(p);
				p.setOnGround(true);
				check(!WallTurn.safeLanding(p), "A forged on-ground flag in midair is not support");
				p.setOnGround(false);
			});
			context.waitFor(mc -> MasterForms.view(mc.player).phase() == WallTurnRules.LAND, 40);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!MasterForms.data(p).airborneUsed(), "Native safe landing rearms the contact");
				check(MasterForms.data(p).recoveryUntil() > MasterForms.now(p), "The actual native landing began its ten-tick recovery");
				long until = MasterForms.data(p).recoveryUntil(); var home = p.level();
				p.teleportTo(server.getLevel(Level.NETHER), .5, 102, .5, Set.of(), 0, 0, false);
				p.teleportTo(home, .5, 100, .5, Set.of(), 0, 0, false);
				check(MasterForms.data(p).recoveryUntil() == until && !MastersArts.activate(p, 0), "Same-call dimension round trip cannot erase landing recovery");
				var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess());
				p.saveWithoutId(saved); var loaded = new Reloaded(home);
				loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess(), saved.buildResult()));
				check(MasterForms.data(loaded).recoveryUntil() == until && !MasterForms.ownsMotion(loaded), "Native attachment reload keeps the actual landing deadline without movement");
				loaded.discard();
			});
			context.waitTicks(12);
			world.getServer().runOnServer(server -> check(!MasterForms.committed(server.getPlayerList().getPlayers().getFirst()), "The persisted recovery expires naturally"));

			brace(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); MasterForms.cancel(p);
				p.teleportTo(.5, 1000, .5); p.setDeltaMovement(Vec3.ZERO);
			});
			context.waitTicks(WallTurnRules.REST_TICKS + WallTurnRules.RECOVERY_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!MasterForms.committed(p) && MasterForms.data(p).airborneUsed(), "Endless descent cannot permanently lock other arts or grant a second wall contact");
			});
		}
	}
	private void brace(ClientGameTestContext c, TestSingleplayerContext w) {
		w.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); prepare(p);
			MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence));
		});
		c.waitTicks(1);
		w.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst(); p.teleportTo(.5, 102, .5); p.setOnGround(false); p.setDeltaMovement(Vec3.ZERO);
			check(MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, MasterForms.view(p).epoch(), ++sequence)), "The isolated contract fixture enters the actual paid brace");
		});
	}
	private static void prepare(ServerPlayer p) {
		Charging.interrupt(p); p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.removeAllEffects(); p.setNoGravity(false); p.setShiftKeyDown(false);
		for (var mark : Reactions.Mark.values()) Reactions.clear(p, mark);
		p.teleportTo(.5, 102, .5); p.setOnGround(false); p.setDeltaMovement(Vec3.ZERO); p.fallDistance = 0;
		p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", 5, 4500, 160, 0));
		p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE));
		p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, 1, 0, false, false));
		Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
		Spellbooks.set(p, Spellbooks.get(p).withStarterGiven().learn(Runes.SELF.id()).learn(Runes.HEAL.id()));
		check(SpellCaster.edit(p, 0, List.of(Runes.SELF.id(), Runes.HEAL.id())) == null, "The ordinary spell fixture uses real editor admission");
		Spellbooks.setReadyAt(p, 0, 0); Spellbooks.setMana(p, 100);
	}
	private static void apply(LivingEntity caster, ServerPlayer target, RuneDef rune, boolean self) {
		var node = SpellCompiler.compile(List.of(Runes.SELF, rune)).root().groups.getFirst().effects.getFirst();
		Effects.apply(new Cast(caster), node, new Cast.Hit(List.of(target), target.position(), new Vec3(1, 0, 0), caster.position(), null, null, self));
	}
}
