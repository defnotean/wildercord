package dev.wildercord.gametest;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.wildercord.aura.*;
import dev.wildercord.client.*;
import dev.wildercord.client.combat.*;
import dev.wildercord.client.render.AuraShellLayer;
import dev.wildercord.gametest.CrimsonMoonRenderMath.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotCommonOptionsImpl;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotOptionsImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Moon-only passive renderer proof. Shared/opening receipt scopes and their acceptance gates are untouched. */
public final class CrimsonMoonRenderProbe {
    private CrimsonMoonRenderProbe(){}
    public static final String PREFIX="crimson_moon_owner_";
    private static final String RUN=UUID.randomUUID().toString();
    private static final AtomicLong SEQ=new AtomicLong();
    private static final Map<String,Expected> ARMED=new HashMap<>();
    private static final Map<String,Paired> PAIRED=new HashMap<>();
    private static final Map<String,Path> RECEIPTS=new HashMap<>();
    private static final Map<String,Report> DONE=new HashMap<>();
    private static final Map<Object,Token> TOKENS=new IdentityHashMap<>();
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private static final ThreadLocal<CopyCall> COPYING=new ThreadLocal<>();
    private static UUID watched;
    private record Origin(UUID owner,int entity,long activation,float age,long sequence){}
    public record Paired(String role,String observerUuid,int observerEntity,String angle,Path releaseReceipt,Map<String,String> releaseIdentity){
        public Paired { releaseIdentity=Map.copyOf(releaseIdentity); }
    }
    public record Artifact(Report report,Path receipt,String sha256){}
    private static final IdentityHashMap<MastersArtPose.Frame,Origin> ORIGINS=new IdentityHashMap<>();
    private static final ArrayDeque<MastersArtPose.Frame> HISTORY=new ArrayDeque<>();
    public record Expected(String name,int owner,String uuid,long activation,String mode,String view,String hand,String skin,
        boolean armor,boolean shell,String phase,float age){}
    public record Palette(long activation,int move,String phase,float age,Identity classic,Identity articulated){}
    public record Binding(long source,long state,long model,long item,long skinMaterial,int owner,String uuid,String skin,String texture,String hand,
        Palette palette,boolean matched){}
    public record Pass(String kind,Binding binding,Comparison geometry,boolean segmented,boolean rigid){}
    public record Copy(long extractSequence,long renderSequence,long copySequence,int width,int height,
        Map<String,String> observations,List<Pass> passes){}
    public record Report(int schemaVersion,String runId,String launchNonce,Expected expected,Copy copy,
        ArticulatedRenderReceipt.Pixels callbackPixels,ArticulatedRenderReceipt.Binding image,boolean scopeCleanupVerified,boolean releaseObservedBeforeSource,boolean verified,
        boolean pixelQualityReviewed,String phaseBasis,boolean serverReleaseFrameCorrespondenceVerified,List<String> failures){}
    private record Extracted(long source,long state,String uuid,String skin,String texture,Palette palette,
        MastersArtPose.Frame classic,ArticulatedCombat.Frame articulated,PlayerModel model,ItemStack main,ItemStack off,
        ItemStackRenderState item,List<ItemStack> armor,boolean shell,ArticulatedCombatPose.Pose expectedBody,
        ArticulatedCombatPose.ViewPose expectedView,PlayerSkin originalSkin){}
    private record View(AvatarRenderState avatar,ArticulatedViewModel model,ArticulatedViewModel.Frame frame,ArticulatedCombatPose.ViewPose expectedPose,Matrix4f root,RenderType material){}
    public record ViewCall(AvatarRenderState avatar,InteractionHand hand,Matrix4f before,ViewCall previous){}
    public record DeferredCall(Model<?> model,Object state,DeferredCall previous){}
    private record World(AvatarRenderState avatar,PlayerModel model,Matrix4f root,Matrix4f modelRoot,boolean modelRootVisible,RenderType material){}
    public record WorldItem(AvatarRenderState avatar,PlayerModel model,ItemStackRenderState item,Matrix4f expected,WorldItem previous){}
    public static final class FallbackView {
        final AvatarRenderState avatar;final FirstPersonHandsAndItemsRenderState hands;final FirstPersonHandsAndItemsRenderer renderer;
        final Matrix4f before,expectedItem;final float inverse,ownership;final ItemStackRenderState item;final FallbackView previous;
        boolean transform;
        FallbackView(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,FirstPersonHandsAndItemsRenderer r,Matrix4f b,
            Matrix4f expected,float inverse,float ownership,FallbackView previous){
            avatar=a;hands=h;renderer=r;before=b;expectedItem=expected;this.inverse=inverse;this.ownership=ownership;item=h.mainHandRenderState;this.previous=previous;
        }
    }
    public record ClassicCall(FallbackView owner,Matrix4f expected){}
    private record CopyCall(Token token,RenderTarget target,GpuTexture texture,Consumer<NativeImage> consumer){}
    public static final class Token {
        final Expected expected;final List<String> failures=new ArrayList<>();final Map<String,String> observations=new LinkedHashMap<>();
        final List<Pass> passes=new ArrayList<>();long extracted,rendered;Copy copy;ArticulatedRenderReceipt.Pixels pixels;ArticulatedRenderReceipt.Binding image;
        int callbacks;boolean ended,scopeCleanupVerified;final Paired paired;long releaseRead,sourceSequence;
        Token(Expected e,Paired paired){expected=e;this.paired=paired;}
        synchronized void reject(String reason){if(!failures.contains(reason))failures.add(reason);}
        synchronized void observe(String key,Object value){if(copy!=null||ended)reject("late_observation");else observations.put(key,String.valueOf(value));}
        synchronized void pass(Pass value){if(copy!=null||ended)reject("late_pass");else passes.add(value);}
    }
    public static final class Scope {
        final Token token;final Scope previous;boolean extracting,rendering;RenderTarget target;GpuTexture texture;
        PlayerSkin originalSkin;PlayerModel originalModel;
        final IdentityHashMap<Object,Long> ids=new IdentityHashMap<>();
        final IdentityHashMap<AvatarRenderState,Extracted> sources=new IdentityHashMap<>();
        final IdentityHashMap<PlayerModel,Map<String,Part>> baselines=new IdentityHashMap<>();
        final IdentityHashMap<ArticulatedViewModel.Frame,View> views=new IdentityHashMap<>();
        final IdentityHashMap<AvatarRenderState,World> worlds=new IdentityHashMap<>();
        final IdentityHashMap<AvatarRenderState,Map<String,Part>> layerBaselines=new IdentityHashMap<>();
        WorldItem worldItem;
        ViewCall view;DeferredCall deferred;FallbackView fallback;
        Scope(Token token,Scope previous){this.token=token;this.previous=previous;}
    }
    public static void watch(UUID owner){watched=owner;synchronized(ORIGINS){ORIGINS.clear();HISTORY.clear();}}
    public static void unwatch(){watch(null);}
    public static void classicExtracted(Avatar owner,AvatarRenderState state,float partial){
        if(!owner.getUUID().equals(watched))return;var frame=state.getData(MastersArtPose.FRAME);if(frame==null||frame.move()!=19)return;
        synchronized(ORIGINS){ORIGINS.put(frame,new Origin(owner.getUUID(),owner.getId(),frame.activation(),owner.level().getGameTime()-frame.activation()+partial,SEQ.incrementAndGet()));
            HISTORY.add(frame);while(HISTORY.size()>512)ORIGINS.remove(HISTORY.removeFirst());}
    }
    public static void arm(Expected expected){synchronized(ARMED){if(ARMED.putIfAbsent(expected.name(),expected)!=null)throw new AssertionError("Duplicate Moon capture");}}
    public static void armPaired(Expected expected,Paired paired){synchronized(ARMED){arm(expected);PAIRED.put(expected.name(),paired);}}
    public static void disarm(String name){synchronized(ARMED){ARMED.remove(name);PAIRED.remove(name);}}
    public static Report requireVerified(String name){Report report;synchronized(DONE){report=DONE.remove(name);}synchronized(RECEIPTS){RECEIPTS.remove(name);}if(report==null||!report.verified())throw new AssertionError("Missing verified Moon receipt: "+name);return report;}
    public static Artifact requireArtifact(String name){Path path;synchronized(RECEIPTS){path=RECEIPTS.get(name);}var report=requireVerified(name);
        try{return new Artifact(report,path,ArticulatedRenderReceipt.sha256(Files.readAllBytes(path)));}catch(Exception failure){throw new AssertionError("Missing persisted Moon report",failure);}}
    public static Token begin(TestScreenshotOptions options){
        if(!(options instanceof TestScreenshotOptionsImpl value)||!value.name.startsWith(PREFIX))return null;
        String nonce=System.getenv("WILDERCORD_MOON_RECEIPT_NONCE");
        if(nonce==null||!UUID.fromString(nonce).toString().equals(nonce))throw new AssertionError("Fresh canonical WILDERCORD_MOON_RECEIPT_NONCE is required");
        Expected expected;Paired paired;synchronized(ARMED){expected=ARMED.remove(value.name);paired=PAIRED.remove(value.name);}if(expected==null)throw new AssertionError("Unarmed Moon screenshot");
        if(!expected.view().equals("fp")&&!(expected.view().equals("remote")&&paired!=null&&paired.role().equals("peer")))
            throw new AssertionError("Owner world remains unproved; only the supervised actual peer can request remote capture");
        if(paired!=null&&(!(paired.role().equals("host")&&expected.view().equals("fp")||paired.role().equals("peer")&&expected.view().equals("remote"))
            ||!expected.mode().equals("articulated")||!expected.hand().equals("RIGHT")||!expected.phase().equals("ACTIVE")||expected.age()!=10
            ||!List.of("front_oblique","reverse_oblique").contains(paired.angle())))throw new AssertionError("Paired role and bounded release capture disagree");
        Token token=new Token(expected,paired);synchronized(TOKENS){if(TOKENS.putIfAbsent(options,token)!=null)throw new AssertionError("Reused screenshot options");}return token;
    }
    public static void unregister(TestScreenshotOptions options,Token token){synchronized(TOKENS){TOKENS.remove(options,token);}}
    public static Scope enter(TestScreenshotCommonOptionsImpl<?> options,Minecraft mc){
        Token token;synchronized(TOKENS){token=TOKENS.get(options);}var previous=CURRENT.get();
        if(token==null){if(previous==null)return null;var suspended=new Scope(null,previous);CURRENT.remove();return suspended;}
        var s=new Scope(token,previous);
        return CrimsonMoonScopeGuard.initialize(CURRENT,s,()->{
        var e=token.expected;
        if(previous!=null)token.reject("nested_capture");
        if(mc.player==null||mc.level==null)throw new AssertionError("Missing connected client");
        var actor=mc.level.getPlayerByUUID(UUID.fromString(e.uuid()));
        boolean remote=e.view().equals("remote");
        if(remote){
            var paired=token.paired;
            if(!CrimsonMoonMultiplayerProof.remoteIdentity(e.uuid(),e.owner(),paired.observerUuid(),paired.observerEntity(),
                mc.player.getUUID().toString(),mc.player.getId(),actor==null?null:actor.getUUID().toString(),actor==null?-1:actor.getId(),
                actor!=null&&actor.level()==mc.level,actor instanceof net.minecraft.client.player.RemotePlayer,mc.getCameraEntity()==mc.player))token.reject("wrong_remote_actor_or_observer");
        } else if(actor!=mc.player||mc.player.getId()!=e.owner())token.reject("wrong_owner");
        if(actor==null)throw new AssertionError("Missing actual connected actor");
        if(mc.getCameraEntity()!=mc.player)token.reject("substituted_camera");
        if(!mc.options.getCameraType().isFirstPerson())token.reject("wrong_owner_or_observer_camera");
        if(remote){
            boolean front=token.paired.angle().equals("front_oblique");double x=front?4.5:-3.5,z=front?5.5:-4.5;float yaw=front?141.3402F:-38.6598F;
            if(Math.abs(mc.player.getX()-x)>.01||Math.abs(mc.player.getY()-100)>.01||Math.abs(mc.player.getZ()-z)>.01
                ||Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()-yaw))>.01||Math.abs(mc.player.getXRot()-7)>.01)token.reject("wrong_actual_observer_angle");
            token.observe("observerPosition",mc.player.position());token.observe("observerYaw",mc.player.getYRot());token.observe("observerPitch",mc.player.getXRot());
        }
        if(mc.isPaused()||mc.level.tickRateManager().isEntityFrozen(actor)||mc.level.tickRateManager().isEntityFrozen(mc.player))token.reject("paused_or_frozen");
        var timeline=MastersArtsClient.timeline(actor);
        if(e.phase().equals("NONE")){if(timeline!=null)token.reject("neutral_timeline_present");}
        else if(timeline==null||timeline.entity()!=e.owner()||timeline.move()!=19||timeline.startTick()!=e.activation()||timeline.windup()!=10||timeline.recovery()!=20)token.reject("wrong_accepted_timeline");
        token.observe("timeline",timeline);token.observe("camera",mc.options.getCameraType());token.observe("observerCoverage",remote);
        token.observe("tick",mc.level.getGameTime());token.observe("partial",options.deltaTicks);token.observe("owner",actor.getUUID());
        token.observe("observerUuid",mc.player.getUUID());token.observe("observerEntity",mc.player.getId());
        if(!(actor instanceof net.minecraft.client.player.AbstractClientPlayer connectedActor))throw new AssertionError("Actor is not an actual connected client player");
        s.originalSkin=connectedActor.getSkin();
        s.originalModel=((net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(actor)).getModel();
        token.observe("connectedSkinTexture",s.originalSkin.body().texturePath());token.observe("connectedSkinModel",s.originalSkin.model());
        if(token.paired!=null){
            var proof=CrimsonMoonMultiplayerProof.readRelease(token.paired.releaseReceipt(),token.paired.releaseIdentity());
            if(proof.accepted()!=e.activation())token.reject("release_action_changed");
            token.releaseRead=SEQ.incrementAndGet();token.observe("releaseReceiptSha256",proof.sha256());
            token.observe("serverReleaseTick",proof.released());token.observe("releaseReadSequence",token.releaseRead);
            token.observe("observerAngle",token.paired.angle());token.observe("pairedRole",token.paired.role());
        }
        token.observe("viewport",mc.getWindow().getWidth()+"x"+mc.getWindow().getHeight());
        },CrimsonMoonRenderProbe::clearScope);
    }
    public static void leave(Scope s){if(s==null)return;
        try{clearScope(s);}finally{CrimsonMoonScopeGuard.restore(CURRENT,s.previous);}
        if(s.token!=null){s.token.scopeCleanupVerified=CURRENT.get()==s.previous&&s.sources.isEmpty()&&s.views.isEmpty()&&s.worlds.isEmpty()
            &&s.baselines.isEmpty()&&s.layerBaselines.isEmpty()&&s.ids.isEmpty()&&s.view==null&&s.worldItem==null&&s.deferred==null&&s.fallback==null
            &&s.target==null&&s.texture==null&&s.originalSkin==null&&s.originalModel==null;
            if(!s.token.scopeCleanupVerified)s.token.reject("failed_scope_cleanup");}
    }
    private static void clearScope(Scope s){s.sources.clear();s.views.clear();s.worlds.clear();s.layerBaselines.clear();s.baselines.clear();s.ids.clear();s.view=null;s.worldItem=null;s.deferred=null;s.fallback=null;s.target=null;s.texture=null;s.originalSkin=null;s.originalModel=null;}
    public static void extractBegin(DeltaTracker delta){var s=CURRENT.get();if(s!=null){s.extracting=true;s.token.observe("renderPartial",delta.getGameTimeDeltaPartialTick(false));}}
    public static void extractEnd(boolean completed){var s=CURRENT.get();if(s!=null){s.extracting=false;if(completed)s.token.extracted=SEQ.incrementAndGet();}}
    public static void extracted(Entity entity,float partial,EntityRenderState state){
        var s=CURRENT.get();if(s==null||!s.extracting||entity.getId()!=s.token.expected.owner()||!(state instanceof AvatarRenderState a))return;
        var e=s.token.expected;var c=a.getData(MastersArtPose.FRAME);var r=a.getData(ArticulatedCombat.FRAME);
        float age=entity.level().getGameTime()-e.activation()+partial;
        if(c!=null){
            Origin origin;synchronized(ORIGINS){origin=ORIGINS.get(c);}
            if(origin==null||!origin.owner().toString().equals(e.uuid())||origin.entity()!=e.owner()||origin.activation()!=e.activation())s.token.reject("missing_exact_classic_origin");else {age=origin.age();s.token.sourceSequence=origin.sequence();s.token.observe("sourceFrameSequence",origin.sequence());}
            if(c.activation()!=e.activation()||c.move()!=19||c.leftHanded()!=e.hand().equals("LEFT")||!c.pose().equals(MastersArtAnimation.sample(19,age,10,20)))s.token.reject("wrong_classic_source");
        }
        String phase=age<0||age>=30?"NONE":age<10?"WINDUP":age<11?"ACTIVE":"RECOVERY";
        if(c==null&&!phase.equals("NONE"))s.token.reject("missing_active_classic_source");
        var expectedBody=phase.equals("NONE")?ArticulatedCombatPose.NONE:ArticulatedCombatPose.samplePlayer(19,age,10,20,e.hand().equals("LEFT"));
        var expectedView=ArticulatedCombatPose.view(expectedBody,e.hand().equals("LEFT"));
        if(r!=null&&r.move()==19){
            var expected=expectedBody;
            if(r.activation()!=e.activation()||r.master()||r.leftHanded()!=e.hand().equals("LEFT")
                ||!r.pose().phase().name().equals(phase)||!hashPose(r.pose()::local,r.pose().weight()).equals(hashPose(expected::local,expected.weight())))s.token.reject("wrong_articulated_source");
        }
        if((c!=null&&(!Float.isFinite(c.yawDelta())||!Float.isFinite(c.pitchDelta())||!Float.isFinite(c.bladeTilt())))
            ||(r!=null&&(!Float.isFinite(r.yawDelta())||!Float.isFinite(r.pitchDelta()))))s.token.reject("nonfinite_aim");
        if(e.mode().equals("classic")){if(r!=null)s.token.reject("classic_has_stale_articulated_frame");}
        else if(r==null||r.master()||r.leftHanded()!=e.hand().equals("LEFT")
            ||r.move()!=(phase.equals("NONE")?-1:19)||r.activation()!=(phase.equals("NONE")?Long.MIN_VALUE:e.activation()))s.token.reject("wrong_complete_articulated_identity");
        if(r!=null&&!r.pose().phase().name().equals(phase))s.token.reject("actual_articulated_phase_changed");
        if(phase.equals("NONE")){
            if(c!=null)s.token.reject("neutral_classic_source_present");
            if(r!=null&&!CrimsonMoonRenderMath.neutralPose(key(r),e.hand().equals("LEFT"),r.pose()))
                s.token.reject("neutral_articulated_source_is_not_authoritative_none");
        }
        if(r!=null&&!CrimsonMoonRenderMath.viewSource(expectedBody,r.pose(),e.hand().equals("LEFT")).matched())
            s.token.reject("wrong_authoritative_source_view");
        var p=new Palette(c==null?Long.MIN_VALUE:c.activation(),c==null?-1:c.move(),phase,age,key(c),key(r));
        ItemStack off=offhand(a);boolean shell=a.getData(AuraShellLayer.SHELL_GLOW)!=null;
        var model=s.originalModel;
        if(!(entity instanceof net.minecraft.client.player.AbstractClientPlayer connected)||!connected.getSkin().equals(s.originalSkin)
            ||!a.skin.equals(s.originalSkin)||!CrimsonMoonRenderMath.originalSkin(s.originalSkin.body().texturePath().toString(),a.skin.body().texturePath().toString(),
                s.originalSkin.model().getSerializedName(),a.skin.model().getSerializedName(),true))s.token.reject("extracted_skin_is_not_original_connected_skin");
        var source=new Extracted(SEQ.incrementAndGet(),id(s,a),entity.getUUID().toString(),a.skin.model().toString(),a.skin.body().texturePath().toString(),
            p,c,r,model,a.getMainHandItemStack().copy(),off.copy(),a.getMainHandItemState(),armor(a).stream().map(ItemStack::copy).toList(),shell,expectedBody,expectedView,s.originalSkin);
        s.sources.put(a,source);
        if(!entity.getUUID().toString().equals(e.uuid())||!a.getMainHandItemStack().is(Items.DIAMOND_SWORD)||!off.isEmpty())s.token.reject("wrong_owner_weapon_offhand");
        if(!a.mainArm.name().equals(e.hand())||!a.skin.model().toString().equalsIgnoreCase(e.skin())||shell!=e.shell())s.token.reject("wrong_original_appearance");
        boolean armor=e.armor()?a.headEquipment.is(Items.NETHERITE_HELMET)&&a.chestEquipment.is(Items.NETHERITE_CHESTPLATE)
            &&a.legsEquipment.is(Items.NETHERITE_LEGGINGS)&&a.feetEquipment.is(Items.NETHERITE_BOOTS)&&armor(a).stream().allMatch(ItemStack::hasFoil):armor(a).stream().allMatch(ItemStack::isEmpty);
        if(!armor)s.token.reject("wrong_actual_armor");
        if(!backend(e,a))s.token.reject("wrong_backend_or_fallback_reason");
        if(e.mode().equals("articulated")?(ArticulatedCombat.viewFrame(a)==null||!phase.equals("NONE")&&ArticulatedCombat.frame(a)==null)
            :(ArticulatedCombat.viewFrame(a)!=null||ArticulatedCombat.frame(a)!=null))s.token.reject("wrong_backend_admission");
        s.token.observe("actualSourceAge",age);s.token.observe("actualSkin",a.skin.model());s.token.observe("actualHand",a.mainArm);
        s.token.observe("actualArmor",armor(a));s.token.observe("actualShell",shell);s.token.observe("articulation",ArticulatedCombat.enabled());
        s.token.observe("shellAdapter",ArticulatedAuraShellRenderer.enabled());s.token.observe("originalTexture",source.texture());
    }
    public static void renderBegin(){var s=CURRENT.get();if(s!=null){s.rendering=true;s.target=Minecraft.getInstance().gameRenderer.mainRenderTarget();s.texture=s.target.getColorTexture();}}
    public static void renderEnd(boolean completed){var s=CURRENT.get();if(s!=null){s.rendering=false;if(completed)s.token.rendered=SEQ.incrementAndGet();}}
    private static Scope rendering(){var s=CURRENT.get();return s!=null&&s.rendering?s:null;}
    public static DeferredCall deferredEnter(Model<?> model,Object state){var s=rendering();if(s==null)return null;var call=new DeferredCall(model,state,s.deferred);s.deferred=call;return call;}
    public static void deferredLeave(DeferredCall call){var s=rendering();if(s!=null&&call!=null)s.deferred=call.previous();}
    public static void bodyBaseline(PlayerModel model,AvatarRenderState a){
        var s=rendering();if(s==null||a.id!=s.token.expected.owner())return;
        if(s.deferred==null){if(s.token.expected.view().equals("remote")&&s.sources.containsKey(a)&&s.sources.get(a).model()==model)s.layerBaselines.put(a,rigid(model));return;}
        if(s.deferred.model()!=model||s.deferred.state()!=a)return;
        if(s.baselines.put(model,rigid(model))!=null)s.token.reject("duplicate_vanilla_baseline");
    }
    public static void bodyPalette(PlayerModel model,AvatarRenderState a){
        var s=rendering();if(s==null||a.id!=s.token.expected.owner()||s.deferred==null||s.deferred.model()!=model||s.deferred.state()!=a)return;
        var source=s.sources.get(a);var baseline=s.baselines.remove(model);Comparison proof=null;
        var access=model instanceof ArticulatedModelAccess x&&x.wildercord$bodyOwned()?x:null;
        boolean segmented=access!=null&&bodyVisible(access.wildercord$rig()),rigid=rigidVisible(model)&&(access==null||!access.wildercord$rig().root.visible);
        if(source==null||source.model()!=model||baseline==null)s.token.reject("missing_exact_owner_vanilla_baseline");
        else if(s.token.expected.mode().equals("articulated")&&!source.palette().phase().equals("NONE")){
            if(access==null||source.articulated()==null)s.token.reject("missing_actual_body_rig");
            else{var rig=access.wildercord$rig();proof=CrimsonMoonRenderMath.compare("deferred_body_locals",
                CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.body(source.expectedBody(),baseline,rig.slim())),CrimsonMoonRenderMath.values(parts(rig)));
                root(s,rig.root);if(rig.slim()!=a.skin.model().toString().equalsIgnoreCase("slim"))s.token.reject("wrong_body_width");}
        }else if(source!=null&&baseline!=null)proof=CrimsonMoonRenderMath.compare("deferred_classic_locals",
            CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.classic(source.classic()==null?null:source.classic().pose(),a.mainArm==HumanoidArm.LEFT,baseline)),
            CrimsonMoonRenderMath.values(rigid(model)));
        if(s.token.expected.view().equals("remote")){
            var world=s.worlds.get(a);
            if(world==null||world.model()!=model)s.token.reject("deferred_body_without_retained_outer_root");
            else if(proof!=null){
                var expected=new ArrayList<>(proof.expected());expected.addAll(CrimsonMoonRenderMath.outerRoot(world.modelRoot(),world.modelRootVisible()));
                var actual=new ArrayList<>(proof.actual());actual.addAll(CrimsonMoonRenderMath.outerRoot(part(model.root()).matrix(),model.root().visible));
                proof=CrimsonMoonRenderMath.compare("deferred_body_locals_and_retained_outer_root",expected,actual);
            }
        }
        pass(s,"body",a,model,null,proof,segmented,rigid);
    }
    /** Captures the actual body submission and its material, before any deferred model reuse. */
    public static void worldSubmitted(Model<?> model,Object state,Matrix4fc root,RenderType material){
        var s=rendering();if(s==null||!s.token.expected.view().equals("remote")||!(state instanceof AvatarRenderState a)||a.id!=s.token.expected.owner())return;
        var source=s.sources.get(a);
        if(source==null||source.model()!=model||!(model instanceof PlayerModel player)){s.token.reject("world_submit_without_exact_model");return;}
        var expectedMaterial=player.renderType(source.originalSkin().body().texturePath());
        if(!CrimsonMoonRenderMath.originalSkin(source.originalSkin().body().texturePath().toString(),a.skin.body().texturePath().toString(),
            source.originalSkin().model().getSerializedName(),a.skin.model().getSerializedName(),material==expectedMaterial))s.token.reject("wrong_world_skin_material");
        var submittedModelRoot=part(player.root()).matrix();
        var rootProof=CrimsonMoonRenderMath.compare("world_root_identity",CrimsonMoonRenderMath.outerRoot(new Matrix4f(),true),CrimsonMoonRenderMath.outerRoot(submittedModelRoot,player.root().visible));
        // Store a detached snapshot only after comparing the enclosing root against its authoritative bind.
        if(s.worlds.put(a,new World(a,player,new Matrix4f(root),new Matrix4f(submittedModelRoot),player.root().visible,material))!=null)s.token.reject("duplicate_world_body_submit");
        s.token.observe("originalSkinMaterialIdentity",id(s,expectedMaterial));s.token.observe("submittedSkinMaterialIdentity",id(s,material));
        // Both deferred preparation and the actual item-layer entry must retain this verified outer root.
        pass(s,"body_submit",a,model,null,rootProof,true,false);
    }
    /** The baseline comes from the real vanilla-to-articulated boundary for this same state/model. */
    public static WorldItem worldItemBegin(Object parent,ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose){
        var s=rendering();if(s==null||!s.token.expected.view().equals("remote")||!(state instanceof AvatarRenderState a)||a.id!=s.token.expected.owner()||arm!=a.mainArm)return null;
        var source=s.sources.get(a);var world=s.worlds.get(a);var baseline=s.layerBaselines.remove(a);
        if(source==null||world==null||parent!=source.model()||world.model()!=parent||baseline==null){s.token.reject("world_item_without_exact_vanilla_baseline");return null;}
        var model=source.model();boolean same=ItemStack.isSameItemSameComponents(stack,source.main())&&stack.getCount()==source.main().getCount();
        boolean eligible=CrimsonMoonRenderMath.item(id(s,source.item()),id(s,item),stack.is(Items.DIAMOND_SWORD),offhand(a).isEmpty(),same)
            &&!item.isEmpty()&&!a.isUsingItem&&!a.isBaby&&a.ticksUsingItem(arm)==0
            &&(a.currentSwing==null||a.currentSwing.animation().type()!=SwingAnimationType.STAB)&&ArticulatedCombat.frame(a)==source.articulated();
        s.token.observe("worldHandEligible",eligible);if(!eligible)s.token.reject("ineligible_ordinary_world_hand");
        if(!CrimsonMoonRenderMath.compare("world_item_root",CrimsonMoonRenderMath.values(world.root()),CrimsonMoonRenderMath.values(pose.last().pose())).matched())s.token.reject("changed_world_item_root");
        var rig=((ArticulatedModelAccess)model).wildercord$rig();
        var expectedBody=CrimsonMoonRenderMath.body(source.expectedBody(),baseline,rig.slim());
        if(!CrimsonMoonRenderMath.compare("world_item_retained_model_root",CrimsonMoonRenderMath.outerRoot(world.modelRoot(),world.modelRootVisible()),
            CrimsonMoonRenderMath.outerRoot(part(model.root()).matrix(),model.root().visible)).matched())s.token.reject("changed_world_item_model_root");
        // Derive the item from the retained verified root, never from a later mutable model value.
        var expected=CrimsonMoonRenderMath.sword(new Matrix4f(world.root()).mul(world.modelRoot()),expectedBody,arm==HumanoidArm.LEFT,rig.slim());
        var call=new WorldItem(a,model,item,expected,s.worldItem);s.worldItem=call;return call;
    }
    public static void worldItemSubmitted(WorldItem call,ItemStackRenderState item,Matrix4fc actual){
        var s=rendering();if(s==null||call==null)return;
        if(s.worldItem!=call||call.item()!=item)s.token.reject("substituted_world_item_submission");
        pass(s,"world_item",call.avatar(),call.model(),item,CrimsonMoonRenderMath.compare("actual_world_socket_item_matrix",
            CrimsonMoonRenderMath.values(call.expected()),CrimsonMoonRenderMath.values(actual)),true,false);
    }
    public static void worldItemEnd(WorldItem call,boolean completed){var s=rendering();if(s!=null&&call!=null){if(!completed)s.token.reject("incomplete_world_item_scope");s.worldItem=call.previous();}}
    public static ViewCall viewEnter(AvatarRenderState a,float partial,InteractionHand hand,PoseStack stack){
        var s=rendering();if(s==null||a.id!=s.token.expected.owner())return null;var call=new ViewCall(a,hand,new Matrix4f(stack.last().pose()),s.view);s.view=call;return call;
    }
    public static void viewLeave(ViewCall call,boolean submitted){var s=rendering();if(s!=null&&call!=null)s.view=call.previous();}
    public static void viewAttempt(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,float partial,InteractionHand hand){
        var s=rendering();if(s==null||a==null||a.id!=s.token.expected.owner()||hand!=InteractionHand.MAIN_HAND)return;
        var source=s.sources.get(a);boolean known=h instanceof MastersHandMotionState;
        boolean equipping=known&&((MastersHandMotionState)h).wildercord$mainHandEquipping();
        boolean same=source!=null&&ItemStack.isSameItemSameComponents(source.main(),h.mainHandItem);
        float height=net.minecraft.util.Mth.lerp(partial,h.oldMainHandHeight,h.mainHandHeight);
        boolean active=source!=null&&!source.palette().phase().equals("NONE")&&source.expectedBody().weight()>0;
        if(!CrimsonMoonRenderMath.handEligible(active,height,same,known,equipping))s.token.reject("ordinary_hand_admission_is_not_eligible");
        s.token.observe("handEquipKnown",known);s.token.observe("handEquipping",equipping);s.token.observe("handSameItem",same);
        s.token.observe("handInterpolatedHeight",height);s.token.observe("ordinaryHandAdmission",CrimsonMoonRenderMath.handEligible(active,height,same,known,equipping));
    }
    public static void viewSubmitted(Model<?> model,Object state,Matrix4fc actual,RenderType material){
        var s=rendering();if(s==null||s.view==null||s.view.hand()!=InteractionHand.MAIN_HAND||!(model instanceof ArticulatedViewModel view)||!(state instanceof ArticulatedViewModel.Frame frame))return;
        var a=s.view.avatar();var source=s.sources.get(a);if(source==null||source.articulated()==null){s.token.reject("missing_view_source");return;}
        if(!(source.model() instanceof ArticulatedModelAccess access)||access.wildercord$viewModel()!=model)s.token.reject("wrong_owner_view_model");
        var r=source.articulated();boolean neutral=source.palette().phase().equals("NONE");
        var expected=source.expectedView();
        if(expected.phase()!=frame.pose().phase()||!hashPose(expected::local,expected.weight()).equals(hashPose(frame.pose()::local,frame.pose().weight())))s.token.reject("wrong_submitted_view_palette");
        var matrix=CrimsonMoonRenderMath.viewRoot(s.view.before(),expected,source.expectedBody().weight(),neutral?0:r.yawDelta(),neutral?0:r.pitchDelta());
        RenderType expectedMaterial=RenderTypes.entityTranslucent(source.originalSkin().body().texturePath());
        if(!CrimsonMoonRenderMath.originalSkin(source.originalSkin().body().texturePath().toString(),a.skin.body().texturePath().toString(),
            source.originalSkin().model().getSerializedName(),a.skin.model().getSerializedName(),material==expectedMaterial))s.token.reject("submitted_skin_material_is_not_original");
        s.token.observe("originalSkinMaterialIdentity",id(s,expectedMaterial));s.token.observe("submittedSkinMaterialIdentity",id(s,material));
        s.views.put(frame,new View(a,view,frame,expected,matrix,material));
        pass(s,"view_submit",a,model,null,CrimsonMoonRenderMath.compare("view_root_matrix",CrimsonMoonRenderMath.values(matrix),CrimsonMoonRenderMath.values(actual)),true,false);
    }
    public static void viewPalette(ArticulatedViewModel model,ArticulatedViewModel.Frame frame){
        var s=rendering();if(s==null||s.deferred==null||s.deferred.model()!=model||s.deferred.state()!=frame)return;
        var view=s.views.get(frame);if(view==null||view.model()!=model){s.token.reject("missing_unique_view_submit");return;}
        root(s,model.rig().root);if(model.rig().slim()!=view.avatar().skin.model().toString().equalsIgnoreCase("slim"))s.token.reject("wrong_view_width");
        pass(s,"view_deferred",view.avatar(),model,null,CrimsonMoonRenderMath.compare("deferred_view_locals",
            CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.palette(view.expectedPose()::local)),CrimsonMoonRenderMath.values(parts(model.rig()))),viewVisible(model.rig()),false);
    }
    public static void viewItem(ItemStackRenderState item,Matrix4fc actual){
        var s=rendering();if(s==null||s.view==null||s.view.hand()!=InteractionHand.MAIN_HAND)return;
        var a=s.view.avatar();var source=s.sources.get(a);var candidates=s.views.values().stream().filter(v->v.avatar()==a).toList();
        if(source==null||candidates.size()!=1){s.token.reject("item_without_unique_view");return;}var view=candidates.getFirst();
        if(!CrimsonMoonRenderMath.item(id(s,source.item()),id(s,item),a.getMainHandItemStack().is(Items.DIAMOND_SWORD),offhand(a).isEmpty(),
            ItemStack.isSameItemSameComponents(source.main(),a.getMainHandItemStack()))||item.isEmpty())s.token.reject("wrong_articulated_item");
        var expected=CrimsonMoonRenderMath.sword(view.root(),CrimsonMoonRenderMath.palette(view.expectedPose()::local),a.mainArm==HumanoidArm.LEFT,view.model().rig().slim());
        pass(s,"view_item",a,view.model(),item,CrimsonMoonRenderMath.compare("actual_socket_item_matrix",CrimsonMoonRenderMath.values(expected),CrimsonMoonRenderMath.values(actual)),true,false);
    }
    public static FallbackView fallbackViewBegin(FirstPersonHandsAndItemsRenderer renderer,AvatarRenderState a,FirstPersonHandsAndItemsRenderState hands,
        InteractionHand hand,ItemStack stack,float attack,float inverse,PoseStack pose){
        var s=rendering();if(s==null||a==null||a.id!=s.token.expected.owner()||hand!=InteractionHand.MAIN_HAND)return null;
        var source=s.sources.get(a);if(source==null){s.token.reject("native_without_extraction");return null;}
        if(!stack.is(Items.DIAMOND_SWORD)||!ItemStack.isSameItemSameComponents(stack,hands.mainHandItem)||!ItemStack.isSameItemSameComponents(stack,source.main()))s.token.reject("wrong_native_stack");
        var c=source.classic();boolean equipping=!(hands instanceof MastersHandMotionState motion)||motion.wildercord$mainHandEquipping();
        float ownership=c==null||equipping?0:MastersViewMotion.ownership(c.pose().weight(),true);
        boolean whack=a.currentSwing!=null&&a.currentSwing.hand()==InteractionHand.MAIN_HAND&&a.currentSwing.animation().type()==SwingAnimationType.WHACK;
        if(a.isUsingItem||a.isAutoSpinAttack||hands.isScoping||a.currentSwing!=null&&a.currentSwing.animation().type()==SwingAnimationType.STAB)s.token.reject("unsupported_native_item_motion");
        var before=new Matrix4f(pose.last().pose());var expected=CrimsonMoonRenderMath.nativeSword(before,c==null?null:c.pose(),a.mainArm==HumanoidArm.LEFT,inverse,c==null?0:c.yawDelta(),c==null?0:c.pitchDelta(),ownership,attack,whack);
        var call=new FallbackView(a,hands,renderer,before,expected,inverse,ownership,s.fallback);s.fallback=call;return call;
    }
    public static ClassicCall classicBegin(PoseStack pose,InteractionHand hand,AvatarRenderState a,float inverse){
        var s=rendering();if(s==null||s.fallback==null||s.fallback.avatar!=a||hand!=InteractionHand.MAIN_HAND)return null;var call=s.fallback;var source=s.sources.get(a);var c=source==null?null:source.classic();
        var before=new Matrix4f(call.before).translate(0,MastersViewMotion.heightCompensation(call.inverse,call.ownership),0);
        if(inverse!=call.inverse||!CrimsonMoonRenderMath.compare("height_compensation",CrimsonMoonRenderMath.values(before),CrimsonMoonRenderMath.values(pose.last().pose())).matched())s.token.reject("wrong_classic_height_compensation");
        return new ClassicCall(call,before.mul(CrimsonMoonRenderMath.classicDelta(c==null?null:c.pose(),a.mainArm==HumanoidArm.LEFT,inverse,c==null?0:c.yawDelta(),c==null?0:c.pitchDelta())));
    }
    public static void classicEnd(ClassicCall call,PoseStack pose){
        var s=rendering();if(s==null||call==null)return;var owner=call.owner();if(owner.transform)s.token.reject("duplicate_classic_transform");owner.transform=true;
        pass(s,"classic_transform",owner.avatar,owner.renderer,null,CrimsonMoonRenderMath.compare("observed_classic_before_after",
            CrimsonMoonRenderMath.values(call.expected()),CrimsonMoonRenderMath.values(pose.last().pose())),false,true);
    }
    public static void fallbackViewItem(FallbackView call,ItemStackRenderState item,Matrix4fc actual){
        var s=rendering();if(s==null||call==null)return;var source=s.sources.get(call.avatar);
        if(source==null||!CrimsonMoonRenderMath.item(id(s,call.item),id(s,item),call.avatar.getMainHandItemStack().is(Items.DIAMOND_SWORD),
            offhand(call.avatar).isEmpty(),ItemStack.isSameItemSameComponents(source.main(),call.hands.mainHandItem))||item.isEmpty())s.token.reject("wrong_native_item");
        if(!call.transform)s.token.reject("skipped_classic_transform");
        pass(s,"native_item",call.avatar,call.renderer,item,CrimsonMoonRenderMath.compare("actual_native_item_matrix",CrimsonMoonRenderMath.values(call.expectedItem),CrimsonMoonRenderMath.values(actual)),false,true);
    }
    public static void fallbackViewEnd(FallbackView call,boolean completed){var s=rendering();if(s!=null&&call!=null){if(!completed)s.token.reject("incomplete_native_call");s.fallback=call.previous;}}
    private static void pass(Scope s,String kind,AvatarRenderState a,Object model,Object item,Comparison proof,boolean segmented,boolean rigid){
        var e=s.sources.get(a);boolean matched=e!=null&&matches(s,e,a);if(proof==null||!proof.matched())s.token.reject("geometry_mismatch:"+kind);
        s.token.pass(new Pass(kind,new Binding(e==null?0:e.source(),id(s,a),id(s,model),id(s,item!=null?item:s.token.expected.view().equals("remote")&&e!=null?e.item():null),skinMaterial(s,a,model),a.id,e==null?"missing":e.uuid(),
            a.skin.model().toString(),a.skin.body().texturePath().toString(),a.mainArm.name(),e==null?null:e.palette(),matched),proof,segmented,rigid));
    }
    private static long skinMaterial(Scope s,AvatarRenderState a,Object model){
        var world=s.worlds.get(a);if(world!=null&&world.model()==model)return id(s,world.material());
        var views=s.views.values().stream().filter(v->v.avatar()==a&&v.model()==model).toList();
        return views.size()==1?id(s,views.getFirst().material()):0;
    }
    private static boolean matches(Scope s,Extracted e,AvatarRenderState a){
        if(e.state()!=id(s,a)||e.classic()!=a.getData(MastersArtPose.FRAME)||e.articulated()!=a.getData(ArticulatedCombat.FRAME)
            ||!CrimsonMoonRenderMath.same(e.palette().classic(),key(a.getData(MastersArtPose.FRAME)))
            ||!CrimsonMoonRenderMath.same(e.palette().articulated(),key(a.getData(ArticulatedCombat.FRAME)))
            ||!e.originalSkin().equals(a.skin)||!e.skin().equals(a.skin.model().toString())||!e.texture().equals(a.skin.body().texturePath().toString())
            ||!a.mainArm.name().equals(s.token.expected.hand())||e.item()!=a.getMainHandItemState()||!ItemStack.isSameItemSameComponents(e.main(),a.getMainHandItemStack())
            ||e.main().getCount()!=a.getMainHandItemStack().getCount()||!e.off().isEmpty()||!offhand(a).isEmpty()||!backend(s.token.expected,a))return false;
        var armor=armor(a);for(int i=0;i<4;i++)if(!ItemStack.isSameItemSameComponents(e.armor().get(i),armor.get(i)))return false;
        return e.shell()==(a.getData(AuraShellLayer.SHELL_GLOW)!=null);
    }
    private static Identity key(MastersArtPose.Frame f){return f==null?null:new Identity(f.activation(),f.move(),f.leftHanded(),false,f.yawDelta(),f.pitchDelta(),f.bladeTilt(),false,"n/a",hash(f.pose().toString()));}
    private static Identity key(ArticulatedCombat.Frame f){return f==null?null:new Identity(f.activation(),f.move(),f.leftHanded(),f.master(),f.yawDelta(),f.pitchDelta(),0,f.scriptedFootwork(),Double.toHexString(f.horizontalVelocitySquared()),f.pose().phase().name()+":"+hashPose(f.pose()::local,f.pose().weight())+":"+hashPose(ArticulatedCombatPose.view(f.pose(),f.leftHanded())::local,f.pose().weight()));}
    private static boolean backend(Expected e,AvatarRenderState a){
        boolean enabled=ArticulatedCombat.enabled(),shell=ArticulatedAuraShellRenderer.enabled(),funded=a.getData(AuraShellLayer.SHELL_GLOW)!=null;
        var s=CURRENT.get();var source=s==null?null:s.sources.get(a);PlayerModel model=source==null?null:source.model();
        boolean owned=model instanceof ArticulatedModelAccess access&&access.wildercord$bodyOwned();
        var access=owned?(ArticulatedModelAccess)model:null;
        var shellAdapter=a.getData(ArticulatedAuraShellRenderer.READY);
        boolean shellOwned=!funded||e.mode().equals("classic")||shellAdapter!=null&&shellAdapter.owner()==model
            &&(Object)shellAdapter instanceof dev.wildercord.gametest.mixin.CrimsonMoonShellAccess shellAccess
            &&shellAccess.moon$slim()==source.originalSkin().model().getSerializedName().equals("slim");
        boolean modelOwned=CrimsonMoonRenderMath.modelOwned(model!=null&&model.getClass()==PlayerModel.class,owned,
            owned&&access.wildercord$rig().slim()==source.originalSkin().model().getSerializedName().equals("slim"),
            owned&&access.wildercord$viewModel()!=null&&access.wildercord$viewModel().rig().slim()==access.wildercord$rig().slim(),shellOwned);
        boolean compatible=modelOwned&&commonCompatible(a)&&ArticulatedArmorRenderer.supports(a);
        if(!e.mode().equals("classic"))compatible&=ArticulatedArmorRenderer.compatible(a)&&ArticulatedArmorRenderer.enabled()&&ArticulatedArmorRenderer.viewEnabled();
        return CrimsonMoonRenderMath.backend(e.mode(),enabled,shell,funded,compatible)
            &&(e.mode().equals("classic")? !ArticulatedArmorRenderer.enabled()&&!ArticulatedArmorRenderer.viewEnabled():true);
    }
    private static boolean commonCompatible(AvatarRenderState a){
        return a.deathTime<=0&&!a.isInvisible&&!a.isBaby&&!a.isUpsideDown&&!a.isAutoSpinAttack&&!a.isFallFlying&&!a.isCrouching&&!a.isPassenger&&!a.isUsingItem
            &&!a.hasPose(Pose.SWIMMING)&&!a.hasPose(Pose.SLEEPING)&&Boolean.TRUE.equals(a.getData(ArticulatedCombat.KNOWN_LAYERS))&&a.walkAnimationSpeed<=.2F&&!a.isSpectator
            &&a.arrowCount==0&&a.stingerCount==0&&!a.showExtraEars&&a.parrotOnLeftShoulder==null&&a.parrotOnRightShoulder==null&&(!a.showCape||a.skin.cape()==null)&&a.heldOnHead.isEmpty()
            &&a.getData(dev.wildercord.client.render.GearLook.PACK)==null&&a.getData(dev.wildercord.client.render.GearLook.PIECES)==null&&a.getData(AuraShellLayer.IMAGES)==null
            &&a.getMainHandItemStack().is(Items.DIAMOND_SWORD)&&offhand(a).isEmpty();
    }
    private static ItemStack offhand(AvatarRenderState a){return a.mainArm==HumanoidArm.RIGHT?a.leftHandItemStack:a.rightHandItemStack;}
    private static List<ItemStack> armor(AvatarRenderState a){return List.of(a.headEquipment,a.chestEquipment,a.legsEquipment,a.feetEquipment);}
    private static long id(Scope s,Object object){return object==null?0:s.ids.computeIfAbsent(object,key->SEQ.incrementAndGet());}
    private static Part part(ModelPart p){var initial=p.getInitialPose();return new Part(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale,initial.x(),initial.y(),initial.z());}
    private static Map<String,Part> rigid(PlayerModel m){var out=new LinkedHashMap<String,Part>();out.put("head",part(m.head));out.put("body",part(m.body));out.put("leftArm",part(m.leftArm));out.put("rightArm",part(m.rightArm));out.put("leftLeg",part(m.leftLeg));out.put("rightLeg",part(m.rightLeg));return out;}
    private static Map<ArticulatedCombatPose.Joint,Part> parts(ArticulatedRig rig){var out=new EnumMap<ArticulatedCombatPose.Joint,Part>(ArticulatedCombatPose.Joint.class);for(var j:ArticulatedCombatPose.Joint.values())out.put(j,part(rig.part(j)));return out;}
    private static boolean rigidVisible(PlayerModel m){return List.of(m.head,m.body,m.leftArm,m.rightArm,m.leftLeg,m.rightLeg).stream().allMatch(p->p.visible&&!p.skipDraw);}
    private static boolean bodyVisible(ArticulatedRig rig){if(!rig.root.visible)return false;for(var j:ArticulatedCombatPose.Joint.values())if(!rig.part(j).visible||rig.part(j).skipDraw)return false;return true;}
    private static boolean viewVisible(ArticulatedRig rig){
        if(!rig.root.visible)return false;
        for(var j:ArticulatedCombatPose.Joint.values()){
            boolean hidden=j==ArticulatedCombatPose.Joint.HEAD||j==ArticulatedCombatPose.Joint.LEFT_THIGH||j==ArticulatedCombatPose.Joint.RIGHT_THIGH;
            boolean skip=j==ArticulatedCombatPose.Joint.PELVIS||j==ArticulatedCombatPose.Joint.SPINE||j==ArticulatedCombatPose.Joint.CHEST;
            if(rig.part(j).visible==hidden||rig.part(j).skipDraw!=skip)return false;
        }return true;
    }
    private static void root(Scope s,ModelPart root){if(!CrimsonMoonRenderMath.compare("rig_root",CrimsonMoonRenderMath.values(new Matrix4f()),CrimsonMoonRenderMath.values(part(root).matrix())).matched())s.token.reject("changed_rig_root");}
    private static String hash(String value){return ArticulatedRenderReceipt.sha256(value.getBytes(StandardCharsets.UTF_8));}
    private static String hashPose(java.util.function.Function<ArticulatedCombatPose.Joint,ArticulatedCombatPose.Transform> source,float weight){var text=new StringBuilder(Float.toHexString(weight));for(var j:ArticulatedCombatPose.Joint.values())text.append(j).append(source.apply(j));return hash(text.toString());}
    public static void screenshot(RenderTarget target,Consumer<NativeImage> consumer,java.util.function.BiConsumer<RenderTarget,Consumer<NativeImage>> original){
        var s=CURRENT.get();if(s==null){original.accept(target,consumer);return;}var token=s.token;
        if(target!=s.target||target.getColorTexture()!=s.texture)token.reject("changed_render_target");
        Consumer<NativeImage> wrapped=image->{synchronized(token){if(++token.callbacks!=1||token.ended)token.reject("duplicate_or_late_readback");token.pixels=ArticulatedRenderReceipt.pixels(image.getWidth(),image.getHeight(),image::getPixel);}consumer.accept(image);};
        var previous=COPYING.get();COPYING.set(new CopyCall(token,target,target.getColorTexture(),wrapped));try{original.accept(target,wrapped);}finally{COPYING.set(previous);}
    }
    public static void copyEnqueued(GpuTexture texture,int mip,RenderTarget target,Consumer<NativeImage> consumer){
        var call=COPYING.get();if(call==null||call.target()!=target||call.consumer()!=consumer)return;var t=call.token();
        if(texture!=call.texture()||target.getColorTexture()!=texture||mip!=0)t.reject("changed_copy_texture");
        synchronized(t){if(t.copy!=null)t.reject("duplicate_copy");else t.copy=new Copy(t.extracted,t.rendered,SEQ.incrementAndGet(),target.width,target.height,Map.copyOf(t.observations),List.copyOf(t.passes));}
    }
    public static void success(Token t,Path path){try{t.image=ArticulatedRenderReceipt.bind(path,FabricLoader.getInstance().getGameDir());}catch(Exception failure){t.reject("png_binding:"+failure);}
        validate(t);t.ended=true;persist(t);if(!t.failures.isEmpty())throw new AssertionError("Moon native receipt rejected: "+t.failures);}
    public static void failed(Token t,Throwable failure){t.reject("original_screenshot_failed:"+failure);t.ended=true;persist(t);}
    private static void validate(Token t){
        var c=t.copy;if(c==null||c.extractSequence()<=0||c.renderSequence()<=c.extractSequence()||c.copySequence()<=c.renderSequence())t.reject("missing_native_stages");
        if(t.image==null||t.pixels==null||!t.pixels.equals(t.image.decodedPixels()))t.reject("png_pixels_not_bound");
        if(c==null)return;if(t.pixels!=null&&(c.width()!=t.pixels.width()||c.height()!=t.pixels.height()))t.reject("wrong_copy_dimensions");
        boolean fp=t.expected.view().equals("fp"),remote=t.expected.view().equals("remote")&&t.paired!=null;
        if(!fp&&!remote)t.reject("world_held_item_proof_incomplete");
        if(!t.scopeCleanupVerified)t.reject("missing_scope_cleanup");
        if(t.paired!=null&&!CrimsonMoonMultiplayerProof.sourceAfterRelease(t.releaseRead,t.sourceSequence,c.extractSequence()))t.reject("source_not_causally_after_server_release");
        boolean art=t.expected.mode().equals("articulated"),active=!t.expected.phase().equals("NONE");
        if(fp&&(!"true".equals(c.observations().get("ordinaryHandAdmission"))||!"true".equals(c.observations().get("handEquipKnown"))
            ||!"false".equals(c.observations().get("handEquipping"))||!"true".equals(c.observations().get("handSameItem"))))t.reject("missing_eligible_ordinary_hand_witness");
        if(remote&&!"true".equals(c.observations().get("worldHandEligible")))t.reject("missing_ordinary_world_hand_witness");
        List<String> allowed=!fp?List.of("body_submit","body","world_item"):art?List.of("view_submit","view_deferred","view_item"):List.of("classic_transform","native_item");
        for(String kind:allowed)if(c.passes().stream().filter(p->p.kind().equals(kind)).count()!=1)t.reject("missing_or_duplicate_"+kind);
        Binding first=c.passes().isEmpty()?null:c.passes().getFirst().binding();
        for(var p:c.passes()){
            var b=p.binding();var palette=b.palette();
            if(!allowed.contains(p.kind())||!b.matched()||b.source()<=0||b.state()<=0||b.model()<=0||b.owner()!=t.expected.owner()||!b.uuid().equals(t.expected.uuid()))t.reject("unbound_pass_identity");
            if(first!=null&&(b.source()!=first.source()||b.state()!=first.state()||b.model()!=first.model()||!Objects.equals(b.palette(),first.palette())||!b.skin().equals(first.skin())||!b.texture().equals(first.texture())||!b.hand().equals(first.hand())))t.reject("conflicting_pass_identity");
            if(remote&&(b.item()<=0||first!=null&&b.item()!=first.item()))t.reject("conflicting_world_item_identity");
            if(!b.hand().equals(t.expected.hand())||!b.skin().equalsIgnoreCase(t.expected.skin()))t.reject("wrong_pass_appearance");
            if(art&&b.skinMaterial()<=0)t.reject("unbound_original_skin_material");
            if(first!=null&&b.skinMaterial()!=first.skinMaterial())t.reject("changed_skin_material_identity");
            if(p.geometry()==null||!p.geometry().matched()||!CrimsonMoonRenderMath.compare(p.geometry().operation(),p.geometry().expected(),p.geometry().actual()).matched())t.reject("unverified_geometry");
            if(p.kind().contains("item")&&b.item()<=0)t.reject("unbound_item");
            if(palette==null||!palette.phase().equals(t.expected.phase())||!CrimsonMoonRenderMath.age(t.expected.age(),palette.age()))t.reject("wrong_rendered_beat");
            if(palette!=null&&(active?(palette.activation()!=t.expected.activation()||palette.move()!=19):(palette.activation()!=Long.MIN_VALUE||palette.move()!=-1)))t.reject("wrong_activation");
            boolean segmented=art&&(fp||active);if(p.segmented()!=segmented||p.rigid()==segmented)t.reject("incomplete_backend");
        }
    }
    private static void persist(Token t){
        String nonce=System.getenv("WILDERCORD_MOON_RECEIPT_NONCE");if(nonce==null)nonce="unbound";
        var report=new Report(t.paired==null?2:3,RUN,nonce,t.expected,t.copy,t.pixels,t.image,t.scopeCleanupVerified,t.copy!=null&&CrimsonMoonMultiplayerProof.sourceAfterRelease(t.releaseRead,t.sourceSequence,t.copy.extractSequence()),t.ended&&t.failures.isEmpty(),false,"authored_post_hitstop_source_palette",false,List.copyOf(t.failures));
        try{var dir=FabricLoader.getInstance().getGameDir().resolve("screenshots/crimson-moon-owner-receipts").resolve(RUN);Files.createDirectories(dir);
            var path=dir.resolve(String.format(Locale.ROOT,"%06d.json",SEQ.incrementAndGet()));Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n",StandardOpenOption.CREATE_NEW);
            if(report.verified()){synchronized(DONE){DONE.put(t.expected.name(),report);}synchronized(RECEIPTS){RECEIPTS.put(t.expected.name(),path);}}
            System.out.println("CRIMSON_MOON_OWNER_RECEIPT name="+t.expected.name()+" verified="+report.verified()+" actualSourceAge="+t.observations.get("actualSourceAge")+" receipt="+path+" phaseBasis=authored_post_hitstop_source_palette serverReleaseFrameCorrespondenceVerified=false pixelQualityReviewed=false");
        }catch(Exception failure){throw new AssertionError("Could not persist native Moon receipt",failure);}
    }
}
