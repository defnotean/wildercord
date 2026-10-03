package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real entity-use packet, loaded acquisition, refusal boundaries, independent rests, and full save reopen. */
public final class ReedRattleTest implements FabricClientGameTest {
    private static ReedbackCrab crab, sweeping;
    private static UUID savedCrab;
    private static long ready, response, calm;
    @Override public void runTest(ClientGameTestContext c) {
        c.runOnClient(mc -> mc.options.keyShift.setDown(false));
        TestWorldSave save;
        try (var w = c.worldBuilder().create()) {
            c.waitTicks(30);
            w.getServer().runCommand("gamerule spawn_mobs false");
            w.getServer().runCommand("time set 6000");
            w.getServer().runCommand("difficulty normal");
            w.getServer().runOnServer(s -> {
                var l = s.overworld();
                for (int x=-8;x<=20;x++) for (int z=-8;z<=8;z++) {
                    l.setBlock(new BlockPos(x,100,z),Blocks.MUD.defaultBlockState(),2);
                    boolean pond=x>=-6 && x<=6 && z>=4;
                    boolean bank=pond && (x==-6 || x==6 || z==4 || z==8);
                    l.setBlock(new BlockPos(x,101,z),bank?Blocks.MUD.defaultBlockState():pond?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                    l.setBlock(new BlockPos(x,102,z),Blocks.AIR.defaultBlockState(),2);
                }
                p(s).setGameMode(GameType.SURVIVAL);
                p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ReedRattle.ITEM));
                check(p(s).getMainHandItem().getMaxDamage()==48,"Finite 48-use instrument");
                var input=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,new ItemStack(WetlandGarden.FLOSS),new ItemStack(Items.CLAY_BALL),
                    new ItemStack(Items.STRING),new ItemStack(Items.BAMBOO),new ItemStack(Items.CLAY_BALL),ItemStack.EMPTY,new ItemStack(Items.BAMBOO),ItemStack.EMPTY));
                var recipe=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);
                check(recipe.isPresent() && recipe.get().value().assemble(input).is(ReedRattle.ITEM),"Loaded shaped fieldcraft recipe produces rattle");
                place(s,.5,-2);crab=spawn(s,.5,.5);
            });
            await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Actual close approach raises warning claws");
            w.getServer().runOnServer(s -> {
                crab.setNoAi(true);
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab),"Standing refuses");untouched(s);
                p(s).setShiftKeyDown(true);place(s,.5,-4);
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab),"Outside three-block range refuses");untouched(s);
                place(s,.5,-2);
                s.overworld().setBlock(new BlockPos(0,101,-1),Blocks.STONE.defaultBlockState(),2);
                s.overworld().setBlock(new BlockPos(0,102,-1),Blocks.STONE.defaultBlockState(),2);
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab),"Obstructed view refuses");untouched(s);
                s.overworld().removeBlock(new BlockPos(0,101,-1),false);s.overworld().removeBlock(new BlockPos(0,102,-1),false);
                p(s).setGameMode(GameType.SPECTATOR);
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab),"Spectator refuses");untouched(s);
                p(s).setGameMode(GameType.SURVIVAL);p(s).setShiftKeyDown(false);
                place(s,10.5,-1.5);sweeping=spawn(s,10.5,.5);sweeping.setTarget(p(s));
            });
            await(c,w,()->sweeping.pose()==ReedbackCrab.SWEEP,"Real warning commits its sweep");
            w.getServer().runOnServer(s -> {
                sweeping.setNoAi(true);p(s).setShiftKeyDown(true);
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,sweeping),"Committed sweep cannot be stopped");
                check(sweeping.pose()==ReedbackCrab.SWEEP && sweeping.responseReady()==0,"Refused sweep retains commitment and response clock");untouched(s);
                sweeping.discard();place(s,.5,-2);p(s).setHealth(20);
            });
            c.runOnClient(mc -> mc.options.keyShift.setDown(true));c.waitTicks(5);
            c.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
            c.waitTicks(2);shot(c,"reed_rattle_inventory");c.runOnClient(mc -> mc.gui.setScreen(null));
            c.runOnClient(mc -> mc.options.keyShift.setDown(true));c.waitTicks(5);
            shot(c,"reed_rattle_warning");
            int id=w.getServer().computeOnServer(s -> crab.getId());
            use(c,id);c.waitTicks(5);
            w.getServer().runOnServer(s -> {
                check(p(s).getMainHandItem().getDamageValue()==1,"Actual client entity-use packet spends one Survival use");
                check(crab.pose()==ReedbackCrab.CALM && crab.getTarget()==null,"Actual packet lowers warning claws");
                ready=p(s).getAttachedOrElse(ReedRattle.READY,0L);response=crab.responseReady();calm=crab.calmUntil();
                check(ready>s.overworld().getGameTime() && calm>s.overworld().getGameTime() && response-calm==80,"Player rest and six-second calm/ten-second response are distinct");
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab),"Repeated use refuses");
                p(s).setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ReedRattle.ITEM));
                check(!ReedRattle.soothe(p(s),InteractionHand.OFF_HAND,crab),"New offhand rattle shares saved player rest");
                check(p(s).getOffhandItem().getDamageValue()==0 && p(s).getMainHandItem().getDamageValue()==1,"Refused swapped instrument has no wear");
                check(!crab.answerMagic(true) && !crab.answerRattle(),"Magic and instruments cannot renew shared crab response");
                check(crab.calmUntil()==calm && crab.responseReady()==response && p(s).getAttachedOrElse(ReedRattle.READY,0L)==ready,"Refusals extend no clock");
                crab.setNoAi(false);crab.setPersistenceRequired();savedCrab=crab.getUUID();
            });
            shot(c,"reed_rattle_settled");
            save=w.getWorldSave();
        }
        try(var w=save.open()) {
            c.waitTicks(25);
            w.getServer().runOnServer(s -> {
                crab=(ReedbackCrab)s.overworld().getEntity(savedCrab);
                check(crab!=null && crab.pose()==ReedbackCrab.CALM,"Reload resumes calm without a stale attack");
                check(crab.calmUntil()==calm && crab.responseReady()==response,"Exact crab clocks survive full restart");
                check(p(s).getMainHandItem().is(ReedRattle.ITEM) && p(s).getMainHandItem().getDamageValue()==1,"Model-bearing item and wear survive full restart");
                check(p(s).getAttachedOrElse(ReedRattle.READY,0L)==ready && p(s).getCooldowns().isOnCooldown(p(s).getMainHandItem()),"Persistent player rest restores native cooldown on join");
                p(s).setShiftKeyDown(true);
                check(!ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab) && p(s).getMainHandItem().getDamageValue()==1,"Restart cannot bypass rest");
                p(s).setGameMode(GameType.CREATIVE);
            });
            int untilCalmEnds=w.getServer().computeOnServer(s -> (int)Math.max(1,calm-s.overworld().getGameTime()+2));
            c.waitTicks(untilCalmEnds);
            check(w.getServer().computeOnServer(s -> crab.pose()!=ReedbackCrab.CALM && crab.calmUntil()==calm),"Calm expires naturally without extending saved deadline");
            w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.SURVIVAL);crab.setTarget(p(s));});
            await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"The warning returns after finite calm");
            w.getServer().runOnServer(s -> {
                check(s.overworld().getGameTime()<response,"Response rest outlasts the calm window");
                check(!crab.answerRattle() && crab.responseReady()==response && crab.calmUntil()==calm,"Another explorer's rattle cannot renew calm during the longer crab rest");
                p(s).setGameMode(GameType.CREATIVE);
            });
            long wait=w.getServer().computeOnServer(s -> Math.max(0,ready-s.overworld().getGameTime()+1));
            c.waitTicks((int)wait);
            w.getServer().runOnServer(s -> {
                crab.discard();p(s).setGameMode(GameType.SURVIVAL);place(s,.5,-2);
                crab=spawn(s,.5,.5);crab.setTarget(p(s));p(s).getMainHandItem().setDamageValue(47);
            });
            await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Rest expires and new warning offers another use");
            w.getServer().runOnServer(s -> {
                crab.setNoAi(true);p(s).setShiftKeyDown(true);
                check(ReedRattle.soothe(p(s),InteractionHand.MAIN_HAND,crab),"Rested player can answer again");
                check(p(s).getMainHandItem().isEmpty(),"Last durability point breaks the instrument");
                crab.setNoAi(false);crab.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p(s)),1);
                check(crab.calmUntil()==0 && crab.pose()!=ReedbackCrab.CALM,"An ordinary hit wakes a settled crab");
            });
        }
        c.runOnClient(mc -> mc.options.keyShift.setDown(false));
    }
    private static void use(ClientGameTestContext c,int id) {
        c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});
    }
    private static ReedbackCrab spawn(MinecraftServer s,double x,double z) {
        var e=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);e.snapTo(x,101,z,0,0);e.setPersistenceRequired();s.overworld().addFreshEntity(e);return e;
    }
    private static void untouched(MinecraftServer s) {
        check(p(s).getMainHandItem().getDamageValue()==0 && p(s).getAttachedOrElse(ReedRattle.READY,0L)==0 && crab.responseReady()==0,"Refused use spends no wear or clock");
    }
    private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
    private static void place(MinecraftServer s,double x,double z) {p(s).teleportTo(s.overworld(),x,101,z,Set.<Relative>of(),0,20,false);}
    private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.BooleanSupplier yes,String why) {
        for(int i=0;i<70;i++) {c.waitTicks(2);if(w.getServer().computeOnServer(s -> yes.getAsBoolean()))return;}throw new AssertionError(why);
    }
    private static void shot(ClientGameTestContext c,String name) {
        c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});
        c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }
    private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
}
