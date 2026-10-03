package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class BasinfillTest implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c) {
        TestWorldSave save;
        try(var w=c.worldBuilder().create()) {
            c.waitTicks(30);
            w.getServer().runCommand("gamerule spawn_mobs false");
            w.getServer().runCommand("time set 6000");
            w.getServer().runOnServer(s -> {
                var p=s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
                var b=Spellbooks.get(p).withStarterGiven().learn(Runes.TOUCH.id()).learn(Runes.BASINFILL.id());
                Spellbooks.set(p,b);basin(p,0,2,2);
                p.teleportTo(s.overworld(),.5,102,-.5,Set.<Relative>of(),0,60,false);
                check(SpellCaster.edit(p,0,List.of(Runes.TOUCH.id(),Runes.BASINFILL.id()))==null,"Editable normal spell");
                Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);
            });c.waitTicks(8);
            shot(c,"basinfill_before");
            w.getServer().runOnServer(s -> {
                var p=s.getPlayerList().getPlayers().getFirst();
                p.teleportTo(s.overworld(),.5,102,-.5,Set.<Relative>of(),0,60,false);
                var aim=s.overworld().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(4.5)),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
                System.out.println("BASINFILL_AIM "+aim.getBlockPos()+" "+aim.getDirection()+" from="+p.getEyePosition());
                float before=Spellbooks.mana(p);
                SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Actual Survival cast pays mana");
            });c.waitTicks(15);
            w.getServer().runOnServer(s -> sources(s.getPlayerList().getPlayers().getFirst(),0,2,2));
            shot(c,"basinfill_after");
            w.getServer().runOnServer(s -> {
                var p=s.getPlayerList().getPlayers().getFirst();
                basin(p,10,4,4);check(fill(p,10,new Cast(p)),"Maximum 16 cells accepted");sources(p,10,4,4);
                basin(p,20,5,4);check(!fill(p,20,new Cast(p)),"Oversized cavity refused");air(p,20,5,4);
                basin(p,30,2,2);p.level().setBlockAndUpdate(new BlockPos(30,100,0),Blocks.AIR.defaultBlockState());
                check(!fill(p,30,new Cast(p)),"Deep hole refused");air(p,30,2,2);
                basin(p,40,2,2);p.level().setBlockAndUpdate(new BlockPos(39,101,0),Blocks.AIR.defaultBlockState());
                check(!fill(p,40,new Cast(p)),"Open edge refused");air(p,40,2,2);
                basin(p,50,2,2);p.setGameMode(GameType.ADVENTURE);
                check(!fill(p,50,new Cast(p)),"Adventure building permission refused");air(p,50,2,2);p.setGameMode(GameType.SURVIVAL);
                basin(p,60,2,2);var paid=new Cast(p);check(fill(p,60,paid),"First basin fills");
                basin(p,70,2,2);check(!fill(p,70,paid.pulse()),"Repeating shape cannot fill another basin per payment");air(p,70,2,2);
                basin(p,80,2,2);var depleted=new Cast(p);for(int i=0;i<Cast.MAX_BLOCKS-2;i++)check(depleted.takeBlock(),"Consume budget");
                check(!fill(p,80,depleted),"Insufficient budget refuses entire basin");air(p,80,2,2);
                basin(p,90,2,2);p.level().setBlockAndUpdate(new BlockPos(90,101,0),Blocks.DANDELION.defaultBlockState());
                check(!fill(p,90,new Cast(p)),"Plants and occupied blocks preserved");
                check(p.level().getBlockState(new BlockPos(90,101,0)).is(Blocks.DANDELION),"No plant destruction");
                var recipe=s.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,net.minecraft.resources.Identifier.parse("wildercord:rune_basinfill")));
                check(recipe.isPresent(),"Generated acquisition recipe loaded");
                var input=net.minecraft.world.item.crafting.CraftingInput.of(3,1,List.of(new ItemStack(WildercordItems.BLANK_RUNE),new ItemStack(net.minecraft.world.item.Items.CLAY_BALL),new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET)));
                var crafted=s.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,s.overworld());
                check(crafted.isPresent() && Runes.BASINFILL.id().equals(crafted.get().value().assemble(input).get(dev.wildercord.content.WildercordComponents.RUNE)),"Actual recipe crafts Basinfill");
                check(crafted.get().value().getRemainingItems(input).stream().anyMatch(stack -> stack.is(net.minecraft.world.item.Items.BUCKET)),"Craft returns empty bucket");
                basin(p,100,2,2);
                var veto=new java.util.concurrent.atomic.AtomicBoolean(true);
                net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity) -> !veto.get() || !pos.equals(new BlockPos(101,101,1)));
                try {check(!fill(p,100,new Cast(p)),"Claim veto refuses entire basin");air(p,100,2,2);}finally{veto.set(false);}
                var nether=s.getLevel(net.minecraft.world.level.Level.NETHER);
                p.teleportTo(nether,.5,102,-.5,Set.<Relative>of(),0,60,false);basin(p,0,2,2);
                check(!fill(p,0,new Cast(p)),"Evaporating Nether refuses water");air(p,0,2,2);
                p.teleportTo(s.overworld(),.5,102,-.5,Set.<Relative>of(),0,60,false);
            });c.waitTicks(30);
            w.getServer().runOnServer(s -> sources(s.getPlayerList().getPlayers().getFirst(),0,2,2));
            save=w.getWorldSave();
        }
        try(var w=save.open()) {c.waitTicks(30);w.getServer().runOnServer(s -> sources(s.getPlayerList().getPlayers().getFirst(),0,2,2));shot(c,"basinfill_saved_pool");}
    }
    private static void basin(ServerPlayer p,int x,int width,int length) {
        var l=p.level();for(int i=-1;i<=width;i++)for(int j=-1;j<=length;j++) {
            l.setBlock(new BlockPos(x+i,100,j),Blocks.STONE_BRICKS.defaultBlockState(),2);
            l.setBlock(new BlockPos(x+i,101,j),i<0||i==width||j<0||j==length?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            l.setBlock(new BlockPos(x+i,102,j),Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static boolean fill(ServerPlayer p,int x,Cast cast) {
        var floor=new BlockPos(x,100,0);return Basinfill.fill(cast,new Cast.Hit(List.of(),Vec3.atCenterOf(floor),new Vec3(0,-1,0),p.position(),floor,Direction.UP,false));
    }
    private static void sources(ServerPlayer p,int x,int width,int length) {
        for(int i=0;i<width;i++)for(int j=0;j<length;j++){var pos=new BlockPos(x+i,101,j);check(p.level().getBlockState(pos).is(Blocks.WATER)&&p.level().getFluidState(pos).isSource(),"Permanent source at "+pos);}
    }
    private static void air(ServerPlayer p,int x,int width,int length) {
        for(int i=0;i<width;i++)for(int j=0;j<length;j++)check(p.level().getBlockState(new BlockPos(x+i,101,j)).isAir(),"Refusal places no partial water");
    }
    private static void shot(ClientGameTestContext c,String name) {
        c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});
        c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }
    private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
}
