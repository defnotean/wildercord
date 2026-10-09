package dev.wildercord.client.combat;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.gametest.CrimsonMoonRenderMath;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import dev.wildercord.gametest.OpeningCaptureWait;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.CameraType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Actual owner input/capture only. Owner world camera is explicitly not remote-observer evidence. */
public final class CrimsonMoonCaptureTest implements FabricClientGameTest {
    public record Appearance(String id,String mode,boolean armor,boolean shell){}
    public record Beat(String id,int tick,float partial,String phase){public float age(){return tick+partial;}}
    public record Trial(Appearance look,HumanoidArm hand,Beat beat){
        public String id(){return look.id()+"_"+hand.name().toLowerCase(Locale.ROOT)+"_"+beat.id();}
    }
    public static final List<Appearance> APPEARANCES=List.of(
        new Appearance("classic_bare_shell_down","classic",false,false),
        new Appearance("classic_netherite_funded","classic",true,true),
        new Appearance("articulated_bare_shell_down","articulated",false,false),
        new Appearance("articulated_netherite_funded","articulated",true,true),
        new Appearance("whole_fallback_netherite_funded","whole_fallback",true,true));
    public static final List<Beat> BEATS=List.of(new Beat("chamber",6,.5F,"WINDUP"),new Beat("release",10,0,"ACTIVE"),
        new Beat("follow",14,0,"RECOVERY"),new Beat("late_recovery",27,0,"RECOVERY"),
        new Beat("neutral",30,0,"NONE"),new Beat("cancelled_neutral",30,0,"NONE"));
    public static List<Trial> trials(String selector){
        var result=new ArrayList<Trial>();
        for(var a:APPEARANCES)for(var h:HumanoidArm.values())for(var b:BEATS){var t=new Trial(a,h,b);if(selector.equals("all")||selector.equals(a.id())||selector.equals(t.id()))result.add(t);}
        check(!result.isEmpty(),"Unknown Moon case selector: "+selector);return List.copyOf(result);
    }
    private static final String ART="crimson_moon";
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final long MAX_HOLD=Arrays.stream(AuraFxRules.Weight.values()).mapToInt(w->AuraFxRules.hitStop(w,1)).max().orElseThrow()*1_000_000L;
    private record Spend(long tick,double paid,double expected,int rest,double momentum,boolean committed,boolean backlash){}
    private record Completion(long tick,long accepted,List<Integer> marks){}
    private record Damage(long tick,int target,float amount){}
    private static Audit current;private static boolean damageHooked;
    private static final class Audit implements AutoCloseable {
        UUID owner;Mob target;final List<Spend> spends=new CopyOnWriteArrayList<>();
        final List<Completion> completions=new CopyOnWriteArrayList<>();final List<Damage> damage=new CopyOnWriteArrayList<>();
        final AuraApi.SpendHook spend=(p,paid,reason,backlash)->{
            if(p.getUUID().equals(owner)&&reason.equals("art:"+ART)){var art=AuraApi.artOf(p,ART).orElseThrow();
                spends.add(new Spend(p.level().getGameTime(),paid,SwordStrings.price(p,art),SwordStrings.rest(p,art),Momentum.value(p),MastersArts.committed(p),backlash));}
        };
        final AuraApi.StringHook done=(p,art,receipt)->{if(p.getUUID().equals(owner)&&art.id().equals(ART))completions.add(new Completion(p.level().getGameTime(),receipt.at(),List.copyOf(receipt.marks())));};
        Audit(){
            check(current==null,"Moon audits cannot overlap");current=this;
            if(!damageHooked){ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{
                var a=current;if(a!=null&&entity==a.target&&!a.spends.isEmpty()&&source.is(Aura.DAMAGE)&&source.getEntity() instanceof ServerPlayer p&&p.getUUID().equals(a.owner))
                    a.damage.add(new Damage(p.level().getGameTime(),entity.getId(),taken));});damageHooked=true;}
            AuraApi.onSpend(spend);AuraApi.onString(done);
        }
        void reset(ServerPlayer p){owner=p.getUUID();spends.clear();completions.clear();damage.clear();}
        @Override public void close(){current=null;AuraApi.spendHooks().remove(spend);AuraApi.stringHooks().remove(done);}
    }
    @Override public void runTest(ClientGameTestContext context){OpeningCaptureWait.withCleanup(()->run(context));}
    private void run(ClientGameTestContext c){
        String selector=System.getProperty("wildercord.moon.case","articulated_netherite_funded_right_release");
        String view=System.getProperty("wildercord.moon.ownerView","fp");
        String skin=System.getProperty("wildercord.moon.expectedSkin","any");
        check(view.equals("fp"),"This reviewed slice only accepts owner FP; world held-item matrices and remote observers remain unproved");
        check(List.of("any","wide","slim").contains(skin),"Expected original skin must be any, wide or slim");
        String[] properties={ArticulatedCombat.ENABLE_PROPERTY,ArticulatedCombat.STABLE_CAMERA_PROPERTY,ArticulatedArmorRenderer.ENABLE_PROPERTY,ArticulatedArmorRenderer.VIEW_PROPERTY,ArticulatedAuraShellRenderer.ENABLE_PROPERTY};
        String[] previous=Arrays.stream(properties).map(System::getProperty).toArray(String[]::new);
        var camera=c.computeOnClient(mc->mc.options.getCameraType());var hand=c.computeOnClient(mc->mc.options.mainHand().get());
        int width=c.computeOnClient(mc->mc.getWindow().getScreenWidth()),height=c.computeOnClient(mc->mc.getWindow().getScreenHeight()),gui=c.computeOnClient(mc->mc.options.guiScale().get());
        boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden()),fullscreen=c.computeOnClient(mc->mc.options.fullscreen().get()),toggle=c.computeOnClient(mc->mc.options.toggleCrouch().get());
        try(var world=c.worldBuilder().create();var audit=new Audit()){
            c.waitTicks(30);
            for(String command:List.of("gamerule spawn_mobs false","gamerule advance_time false","time set 3000","weather clear",
                "fill -10 99 -10 10 99 10 minecraft:stone_bricks","fill -10 100 -10 10 108 10 minecraft:air"))world.getServer().runCommand(command);
            c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(3);mc.resizeGui();mc.options.toggleCrouch().set(false);if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});
            try{for(var trial:trials(selector))capture(c,world,audit,trial,view,skin);}
            finally{world.getServer().runOnServer(s->{if(audit.target!=null)audit.target.discard();MastersArts.cancel(s.getPlayerList().getPlayers().getFirst());});}
            System.out.println("CRIMSON_MOON_OWNER_COMPLETE cases="+trials(selector).size()+" view="+view+" originalSkin="+skin+" remoteObserverCoverage=false momentumSetup=declared_peak_100");
        }finally{
            c.getInput().releaseKey(o->o.keyAttack);c.getInput().releaseKey(o->o.keyShift);CrimsonMoonRenderProbe.unwatch();HitStop.clear();
            for(int i=0;i<properties.length;i++){if(previous[i]==null)System.clearProperty(properties[i]);else System.setProperty(properties[i],previous[i]);}
            c.runOnClient(mc->{mc.options.setCameraType(camera);mc.options.mainHand().set(hand);mc.options.broadcastOptions();mc.options.toggleCrouch().set(toggle);
                mc.getWindow().setWindowed(width,height);mc.getWindow().setFullscreen(fullscreen);mc.options.guiScale().set(gui);mc.resizeGui();if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});
        }
    }
    private void capture(ClientGameTestContext c,TestSingleplayerContext world,Audit audit,Trial trial,String view,String expectedSkin){
        c.getInput().releaseKey(o->o.keyShift);
        c.waitFor(mc->mc.level.getGameTime()>=SwordStrings.readyAt(mc.player,ART),660); // Preserve every real 600-tick individual rest.
        System.setProperty(ArticulatedCombat.ENABLE_PROPERTY,Boolean.toString(!trial.look().mode().equals("classic")));
        System.setProperty(ArticulatedCombat.STABLE_CAMERA_PROPERTY,"true");System.setProperty(ArticulatedArmorRenderer.ENABLE_PROPERTY,"true");System.setProperty(ArticulatedArmorRenderer.VIEW_PROPERTY,"true");
        System.setProperty(ArticulatedAuraShellRenderer.ENABLE_PROPERTY,Boolean.toString(!trial.look().mode().equals("whole_fallback")));
        world.getServer().runOnServer(s->prepare(s.getPlayerList().getPlayers().getFirst(),audit,trial));
        c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.mainHand().set(trial.hand());mc.options.broadcastOptions();mc.player.setYRot(0);mc.player.setXRot(3);CrimsonMoonRenderProbe.watch(mc.player.getUUID());});
        c.waitTicks(15);int requests=c.computeOnClient(mc->SwordStringsClient.counts()[0]);
        for(int i=0;i<3;i++){
            c.waitFor(mc->mc.player.getAttackStrengthScale(0)>=.999F,40);c.getInput().pressKey(o->o.keyAttack);c.waitTicks(2);boolean last=i==2;
            world.getServer().runOnServer(s->{
                audit.target.snapTo(last?2.2:.5,100,last?2:3.1,180,0);audit.target.setDeltaMovement(Vec3.ZERO);
                if(last){var p=s.getPlayerList().getPlayers().getFirst();var art=AuraApi.artOf(p,ART).orElseThrow();
                    p.setAttached(AuraAttachments.AURA,Aura.data(p).withAura(trial.look().shell()?AuraRules.capacity(AuraRules.SOVEREIGN):(float)SwordStrings.price(p,art)+.5F));}
            });c.waitTicks(12);
        }
        check(c.computeOnClient(mc->SwordStringsClient.chain().size()>=3),"Three actual FULL attacks precede LOW");
        world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(Momentum.peak(p)&&AuraApi.artOf(p,ART).orElseThrow().condition().met(p),"Actual server Final gate is open before low input");});
        c.getInput().holdKey(o->o.keyShift);c.waitTicks(2);
        check(c.computeOnClient(mc->Momentum.peak(mc.player)&&AuraApi.artOf(mc.player,ART).orElseThrow().condition().met(mc.player)),"Synced declared peak remains valid at LOW input");
        c.getInput().pressKey(o->o.keyAttack);c.getInput().releaseKey(o->o.keyShift);
        c.waitFor(mc->MastersArtsClient.timeline(mc.player)!=null&&MastersArtsClient.timeline(mc.player).move()==19,35);
        var accepted=c.computeOnClient(mc->{
            var a=MastersArtsClient.timeline(mc.player);check(a.entity()==mc.player.getId()&&a.windup()==10&&a.recovery()==20,"Actual accepted ID19 timeline owns capture");
            check(SwordStringsClient.counts()[0]==requests+1&&SwordStringsClient.lastAsked().equals(ART),"Exactly one real reader request names Moon");return a;});
        c.runOnClient(mc->mc.options.setCameraType(view.equals("fp")?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT));
        boolean cancelled=trial.beat().id().equals("cancelled_neutral");
        if(cancelled){
            c.waitFor(mc->mc.level.getGameTime()>=accepted.startTick()+3,10);
            world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.level().getGameTime()<accepted.startTick()+10,"Cancellation damages actual windup");
                p.setInvulnerableTime(0);check(p.hurtServer(p.level(),p.level().damageSources().generic(),1),"Real damage cancels unreleased Final");check(MastersArts.committed(p),"Cancellation retains paid recovery");});
        }
        c.waitFor(mc->mc.level.getGameTime()>=accepted.startTick()+trial.beat().tick(),45);
        var snapshot=c.computeOnClient(mc->{boolean held=HitStop.holding();return new OpeningCaptureWait.Snapshot(new OpeningCaptureWait.Identity(mc.player.getUUID(),mc.player.getId(),accepted.entity(),19,accepted.startTick(),mc.level.getGameTime()),held,System.nanoTime()+(held?MAX_HOLD:0));});
        OpeningCaptureWait.await(snapshot);
        String actualSkin=c.computeOnClient(mc->mc.player.getSkin().model().getSerializedName());
        check(expectedSkin.equals("any")||expectedSkin.equals(actualSkin),"Actual original skin agrees with launch requirement");
        String name=CrimsonMoonRenderProbe.PREFIX+view+"_"+actualSkin+"_"+trial.id();
        c.runOnClient(mc->{
            float age=mc.level.getGameTime()-accepted.startTick()+trial.beat().partial();
            check(!HitStop.holding()&&CrimsonMoonRenderMath.age(trial.beat().age(),age),"Natural HitStop expiry stays in capture beat window");
            CrimsonMoonRenderProbe.arm(new CrimsonMoonRenderProbe.Expected(name,mc.player.getId(),mc.player.getUUID().toString(),accepted.startTick(),trial.look().mode(),view,
                trial.hand().name(),actualSkin,trial.look().armor(),trial.look().shell(),trial.beat().phase(),trial.beat().age()));
        });
        try{c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDeltaTicks(trial.beat().partial()));CrimsonMoonRenderProbe.requireVerified(name);}
        finally{CrimsonMoonRenderProbe.disarm(name);}
        c.waitFor(mc->mc.level.getGameTime()>=accepted.startTick()+34,45);
        world.getServer().runOnServer(s->{
            var p=s.getPlayerList().getPlayers().getFirst();check(audit.spends.size()==1&&audit.completions.size()==(cancelled?0:1),"Native Final paid once and only released trials completed");
            var spend=audit.spends.getFirst();check(spend.tick()==accepted.startTick()&&spend.paid()>0&&Math.abs(spend.paid()-spend.expected())<.0001&&spend.committed()&&!spend.backlash()&&spend.momentum()>=95,"Actual paid Final retains legitimate declared gate");
            check(spend.rest()==600&&SwordStrings.readyAt(p,ART)==accepted.startTick()+600&&!MastersArts.committed(p),"Original 600-tick rest and 30-tick physical recovery remain intact");
            if(cancelled)check(audit.damage.isEmpty(),"Unreleased cancelled Moon causes no art damage");
            else{var done=audit.completions.getFirst();check(done.accepted()==accepted.startTick()&&CrimsonMoonRenderMath.releaseAt(accepted.startTick(),done.tick()),"Actual server completion is exactly accepted+10; no clock tolerance substitutes for release timing");
                check(done.marks().size()==4&&done.marks().subList(0,3).stream().allMatch(SwordString.Token.FULL::fits)&&SwordString.Token.LOW.fits(done.marks().getLast()),"Authoritative completion retains FULL/FULL/FULL/LOW marks");
                check(!audit.damage.isEmpty()&&audit.damage.getFirst().amount()>0&&audit.damage.getFirst().tick()>=done.tick()+1&&audit.damage.getFirst().tick()<=done.tick()+5,"Actual direct strike follows physical release by its original distance delay");}
            System.out.println("CRIMSON_MOON_OWNER_SERVER name="+name+" accepted="+accepted.startTick()+" spend="+spend+" completion="+audit.completions+" damage="+audit.damage+" peakSetup=declared_100 authoredPoseActive=[10,11) requiredServerCompletionOffset=10 actualServerCompletionOffset="
                +(audit.completions.isEmpty()?"none_cancelled":Long.toString(audit.completions.getFirst().tick()-accepted.startTick()))
                +" requiredDirectDamageAges=[11,15] serverReleaseFrameCorrespondenceVerified=false");
            audit.target.discard();audit.target=null;
        });
    }
    private static void prepare(ServerPlayer p,Audit audit,Trial trial){
        audit.reset(p);p.setGameMode(GameType.SURVIVAL);p.teleportTo(p.level(),.5,100,.5,Set.<Relative>of(),0,3,false);p.setDeltaMovement(Vec3.ZERO);
        p.removeAllEffects();p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(var slot:ARMOR){var stack=!trial.look().armor()?ItemStack.EMPTY:new ItemStack(switch(slot){case HEAD->Items.NETHERITE_HELMET;case CHEST->Items.NETHERITE_CHESTPLATE;case LEGS->Items.NETHERITE_LEGGINGS;default->Items.NETHERITE_BOOTS;});
            if(!stack.isEmpty())stack.enchant(p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),4);p.setItemSlot(slot,stack);}
        p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("crimson",AuraRules.SOVEREIGN,AuraRules.threshold(AuraRules.SOVEREIGN),AuraRules.capacity(AuraRules.SOVEREIGN),0));
        // Declared fixture precondition, not evidence of earning momentum and not an overridden Final condition.
        p.setAttached(Momentum.MOMENTUM,new Momentum.State(100,p.level().getGameTime()+200,0,0,0));p.inventoryMenu.broadcastChanges();
        var art=AuraApi.artOf(p,ART).orElseThrow();var style=MastersStyleRules.of(ART);
        check(AuraApi.ArtSlot.of(art).orElseThrow()==AuraApi.ArtSlot.FINAL&&art.string().text().equals("full full full low")&&art.condition().met(p),"Real Crimson Sovereign slot V and declared peak satisfy ordinary gate");
        check(style.animation()==19&&style.windup()==10&&style.recovery()==20,"Native timeline uses Moon ID19");
        var target=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);check(target!=null,"Actual living visual target exists");
        target.addTag("wildercord.rolled");target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);target.snapTo(.5,100,3.1,180,0);p.level().addFreshEntity(target);audit.target=target;
    }
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}

