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
import dev.wildercord.gametest.CounterPeerRenderMath.*;
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

/** Counter-only passive renderer proof. Shared/opening receipt scopes and their acceptance gates are untouched. */
public final class CounterPeerRenderProbe {
    private CounterPeerRenderProbe(){}
    public static final String PREFIX="counter_peer_";
    private static final String RUN=UUID.randomUUID().toString();
    private static final AtomicLong SEQ=new AtomicLong();
    private static final Map<String,Expected> ARMED=new HashMap<>();
    private static final Map<String,Paired> PAIRED=new HashMap<>();
    private static final Map<String,Path> RECEIPTS=new HashMap<>();
    private static final Map<String,Report> DONE=new HashMap<>();
    private static final Map<Object,Token> TOKENS=new IdentityHashMap<>();
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private static final ThreadLocal<CopyCall> COPYING=new ThreadLocal<>();
    private static volatile UUID watched;
    private static volatile AssertionError failure;
    public static void assertHealthy(){if(failure!=null)throw failure;}
    private record Watched(net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.client.multiplayer.ClientPacketListener connection,
        net.minecraft.client.player.AbstractClientPlayer actor,net.minecraft.client.player.LocalPlayer viewer,CounterPeerSourceLatch source){}
    private static volatile Watched watchedBodies;
    private record Origin(UUID owner,int entity,long activation,float age,long sequence){}
    public record Paired(String role,String observerUuid,int observerEntity,Path acceptedReceipt,Map<String,String> acceptedIdentity){
        public Paired { acceptedIdentity=Map.copyOf(acceptedIdentity); }
    }
    public record Artifact(Report report,Path receipt,String sha256){}
    private static final IdentityHashMap<MastersArtPose.Frame,Origin> ORIGINS=new IdentityHashMap<>();
    private static final ArrayDeque<MastersArtPose.Frame> HISTORY=new ArrayDeque<>();
    public record Expected(String name,int owner,String uuid,long activation,String mode,String view,String hand,String skin,
        boolean armor,boolean shell,String phase,int move,int windup,int recovery){}
    public record Palette(long activation,int move,String phase,float age,Identity classic,Identity articulated){}
    public record Binding(long source,long state,long model,long item,long skinMaterial,int owner,String uuid,String skin,String texture,String hand,
        Palette palette,boolean matched){}
    public record Pass(String kind,Binding binding,Comparison geometry,boolean segmented,boolean rigid){}
    public record ItemDraw(Binding binding,Comparison geometry,String displayContext,int quads,String anchorKind,List<Float> anchorExpected,List<Float> anchorActual,
        List<Float> displayRotation,List<Float> displayTranslation,List<Float> displayScale,List<Float> displayLocal,
        List<Float> emittedQuadPositions,String emittedQuadSha256,long stackIdentity,long collectorIdentity,long entryStackIdentity,long entryCollectorIdentity){}
    public record Copy(long extractSequence,long renderSequence,long copySequence,int width,int height,
        Map<String,String> observations,List<Pass> passes,List<ItemDraw> draws){}
    public record Report(int schemaVersion,String runId,String launchNonce,Expected expected,Copy copy,
        ArticulatedRenderReceipt.Pixels callbackPixels,ArticulatedRenderReceipt.Binding image,boolean scopeCleanupVerified,boolean acceptanceObservedBeforeSource,boolean verified,
        boolean pixelQualityReviewed,String phaseBasis,boolean serverReleaseFrameCorrespondenceVerified,List<String> failures){}
    private record Extracted(long source,long state,String uuid,String skin,String texture,Palette palette,
        MastersArtPose.Frame classic,ArticulatedCombat.Frame articulated,PlayerModel model,ItemStack main,ItemStack off,
        ItemStackRenderState item,List<ItemStack> armor,boolean shell,ArticulatedCombatPose.Pose expectedBody,
        ArticulatedCombatPose.ViewPose expectedView,PlayerSkin originalSkin){}
    private record View(AvatarRenderState avatar,ArticulatedViewModel model,ArticulatedViewModel.Frame frame,ArticulatedCombatPose.ViewPose expectedPose,Matrix4f root,RenderType material){}
    public record ViewCall(AvatarRenderState avatar,InteractionHand hand,Matrix4f before,PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector,ViewCall previous){}
    public record DeferredCall(Object node,Model<?> model,Object state,NativeBodySubmission.Visit body,DeferredCall previous){}
    private record World(AvatarRenderState avatar,PlayerModel model,Matrix4f root,Matrix4f modelRoot,boolean modelRootVisible,RenderType material){}
    public record WorldItem(AvatarRenderState avatar,PlayerModel model,ItemStackRenderState item,Matrix4f expected,PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector,WorldItem previous){}
    public static final class FallbackView {
        final AvatarRenderState avatar;final FirstPersonHandsAndItemsRenderState hands;final FirstPersonHandsAndItemsRenderer renderer;
        final Matrix4f before,expectedItem;final PoseStack stack;final net.minecraft.client.renderer.SubmitNodeCollector collector;final float inverse,ownership;final ItemStackRenderState item;final FallbackView previous;
        boolean transform;
        FallbackView(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,FirstPersonHandsAndItemsRenderer r,Matrix4f b,
            Matrix4f expected,float inverse,float ownership,PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector,FallbackView previous){
            avatar=a;hands=h;renderer=r;before=b;expectedItem=expected;this.stack=stack;this.collector=collector;this.inverse=inverse;this.ownership=ownership;item=h.mainHandRenderState;this.previous=previous;
        }
    }
    public record ClassicCall(FallbackView owner,Matrix4f expected){}
    private record CopyCall(Token token,RenderTarget target,GpuTexture texture,Consumer<NativeImage> consumer){}
    public static final class Token {
        final Expected expected;final List<String> failures=new ArrayList<>();final Map<String,String> observations=new LinkedHashMap<>();
        final List<Pass> passes=new ArrayList<>();final List<ItemDraw> draws=new ArrayList<>();long extracted,rendered;Copy copy;ArticulatedRenderReceipt.Pixels pixels;ArticulatedRenderReceipt.Binding image;
        int callbacks;boolean ended,scopeCleanupVerified;final Paired paired;long releaseRead,sourceSequence;
        Token(Expected e,Paired paired){expected=e;this.paired=paired;}
        synchronized void reject(String reason){if(!failures.contains(reason))failures.add(reason);rejectPermanently(expected.name(),reason);}
        synchronized void observe(String key,Object value){if(copy!=null||ended)reject("late_observation");else observations.put(key,String.valueOf(value));}
        synchronized void pass(Pass value){if(copy!=null||ended)reject("late_pass");else passes.add(value);}
        synchronized void draw(ItemDraw value){if(copy!=null||ended)reject("late_item_draw");else draws.add(value);}
    }
    public static final class Scope {
        final Token token;final Scope previous;boolean extracting,rendering;RenderTarget target;GpuTexture texture;
        PlayerSkin originalSkin;PlayerModel originalModel;
        final NativeBodySubmission nativeBody=new NativeBodySubmission();
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
    /** One unchanged native update/extract/render/copy. File completion never inserts a game tick between phase renders. */
    public static java.util.concurrent.CompletableFuture<Artifact> capture(Minecraft mc,String name){
        var options=TestScreenshotOptions.of(name).disableCounterPrefix().withDeltaTicks(.5F);
        var token=begin(options);if(token==null)throw new AssertionError("Unarmed counter native capture");
        var output=new java.util.concurrent.CompletableFuture<Path>();
        Scope scope=null;
        DeltaTracker delta=new DeltaTracker(){public float getGameTimeDeltaTicks(){return .5F;}public float getGameTimeDeltaPartialTick(boolean paused){return .5F;}public float getRealtimeDeltaTicks(){return .5F;}};
        try{
            scope=enter((TestScreenshotCommonOptionsImpl<?>)options,mc);
            mc.gameRenderer.update(delta);extractBegin(delta);boolean extracted=false;
            try{mc.gameRenderer.extract(delta,true);extracted=true;}finally{extractEnd(extracted);}
            renderBegin();boolean rendered=false;try{mc.gameRenderer.render();rendered=true;}finally{renderEnd(rendered);}
            com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();
            screenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){
                var path=FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name+".png");
                Files.createDirectories(path.getParent());if(Files.exists(path))throw new AssertionError("Counter image cannot be replaced");
                image.writeToFile(path);output.complete(path);
            }catch(Throwable failure){output.completeExceptionally(failure);}},net.minecraft.client.Screenshot::takeScreenshot);
        }catch(Throwable failure){failed(token,failure);throw failure;}
        finally{leave(scope);unregister(options,token);}
        return output.thenApply(path->{success(token,path);return requireArtifact(name);});
    }
    public static void watch(UUID owner,int move,int windup,int recovery){
        watched=owner;watchedBodies=null;
        if(owner!=null){var mc=Minecraft.getInstance();var actor=mc.level.getPlayerByUUID(owner);
            if(!(actor instanceof net.minecraft.client.player.AbstractClientPlayer connected)||mc.player==null||mc.getConnection()==null)throw new AssertionError("Actual source/viewer connection required before arming");
            watchedBodies=new Watched(mc.level,mc.getConnection(),connected,mc.player,new CounterPeerSourceLatch(connected.getId(),move,windup,recovery));}
        synchronized(ORIGINS){ORIGINS.clear();HISTORY.clear();}
    }
    public static void unwatch(){watched=null;watchedBodies=null;synchronized(ORIGINS){ORIGINS.clear();HISTORY.clear();}}
    private static synchronized void rejectPermanently(String capture,String reason){
        if(failure==null)failure=new AssertionError("Counter native receipt failed: "+reason);
        try{
            var identity=new HashMap<String,String>();
            for(String key:List.of("nonce","suite","sourceHead","checkoutSha","prHeadSha","descriptorSha256","hostUuid","peerUuid","runIdentity","role")){
                String value=System.getProperty("wildercord.mp."+key);if(value==null)throw new IllegalStateException("Missing failure provenance "+key);identity.put(key,value);}
            identity.put("pid",Long.toString(ProcessHandle.current().pid()));
            CounterPeerFailureLedger.reject(Path.of(System.getProperty("wildercord.mp.directory")),identity,capture,reason);
        }catch(Throwable problem){System.err.println("COUNTER_RECEIPT_FAILURE_NOT_PERSISTED: "+problem);Runtime.getRuntime().halt(86);}
    }
    public static void received(Minecraft mc,MastersArts.Performed payload){
        var watch=watchedBodies;if(watch==null||payload.entity()!=watch.actor().getId())return;
        try{
            if(watch.level()!=mc.level||watch.connection()!=mc.getConnection()||watch.viewer()!=mc.player||mc.level.getPlayerByUUID(watch.actor().getUUID())!=watch.actor())throw new AssertionError("Original armed receive source replaced");
            watch.source().received(payload,payload.entity(),payload.move(),payload.startTick(),payload.windup(),payload.recovery(),SEQ.incrementAndGet());
        }catch(Throwable rejected){rejectPermanently("pre-input-first-source",rejected.toString()); /* Original receiver still executes unchanged. */}
    }
    public static long requireSource(MastersArts.Performed timeline,long accepted){
        try{if(watchedBodies==null)throw new AssertionError("Native receive source was not armed");return watchedBodies.source().require(timeline,accepted);}
        catch(AssertionError rejected){rejectPermanently("native-source-identity",rejected.getMessage());throw rejected;}
    }
    public static void classicExtracted(Avatar owner,AvatarRenderState state,float partial){
        var watch=watchedBodies;if(watch==null||!owner.getUUID().equals(watch.actor().getUUID()))return;
        try{
            if(watch.actor()!=owner||watch.level()!=owner.level())throw new AssertionError("Original counter source body replaced after arming");
            var frame=state.getData(MastersArtPose.FRAME);if(frame==null||(frame.move()!=24&&frame.move()!=25))return;
            synchronized(ORIGINS){ORIGINS.put(frame,new Origin(owner.getUUID(),owner.getId(),frame.activation(),owner.level().getGameTime()-frame.activation()+partial,SEQ.incrementAndGet()));
                HISTORY.add(frame);while(HISTORY.size()>512)ORIGINS.remove(HISTORY.removeFirst());}
        }catch(Throwable rejected){rejectPermanently("armed-classic-extraction",rejected.toString());}
    }
    public static void arm(Expected expected){synchronized(ARMED){if(ARMED.putIfAbsent(expected.name(),expected)!=null)throw new AssertionError("Duplicate counter capture");}}
    public static void armPaired(Expected expected,Paired paired){synchronized(ARMED){arm(expected);PAIRED.put(expected.name(),paired);}}
    public static void disarm(String name){synchronized(ARMED){ARMED.remove(name);PAIRED.remove(name);}}
    public static Report requireVerified(String name){Report report;synchronized(DONE){report=DONE.remove(name);}synchronized(RECEIPTS){RECEIPTS.remove(name);}if(report==null||!report.verified())throw new AssertionError("Missing verified counter receipt: "+name);return report;}
    public static Artifact requireArtifact(String name){Path path;synchronized(RECEIPTS){path=RECEIPTS.get(name);}var report=requireVerified(name);
        try{return new Artifact(report,path,ArticulatedRenderReceipt.sha256(Files.readAllBytes(path)));}catch(Exception failure){throw new AssertionError("Missing persisted counter report",failure);}}
    public static Token begin(TestScreenshotOptions options){
        if(!(options instanceof TestScreenshotOptionsImpl value)||!value.name.startsWith(PREFIX))return null;
        String nonce=System.getenv("WILDERCORD_COUNTER_RECEIPT_NONCE");
        if(nonce==null||!UUID.fromString(nonce).toString().equals(nonce))throw new AssertionError("Fresh canonical WILDERCORD_COUNTER_RECEIPT_NONCE is required");
        Expected expected;Paired paired;synchronized(ARMED){expected=ARMED.remove(value.name);paired=PAIRED.remove(value.name);}if(expected==null)throw new AssertionError("Unarmed counter screenshot");
        if(!expected.view().equals("fp")&&!(expected.view().equals("remote")&&paired!=null&&paired.role().equals("peer")))
            throw new AssertionError("Owner world remains unproved; only the supervised actual peer can request remote capture");
        if(expected.armor()||expected.shell())throw new AssertionError("Counter baseline fixture does not claim armor or shell rendering");
        if(paired==null||!CounterPeerPhaseContract.expected(expected.name(), expected.move(), expected.windup(), expected.recovery(), expected.mode(), expected.hand(), expected.phase(), paired.role(), expected.view()))
            throw new AssertionError("Counter identity, window, camera and role disagree");
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
        if(watchedBodies==null||watchedBodies.level()!=mc.level||watchedBodies.connection()!=mc.getConnection()
            ||watchedBodies.actor()!=actor||watchedBodies.viewer()!=mc.player)token.reject("original_body_world_or_connection_replaced");
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
            var eye=mc.gameRenderer.mainCamera().position();
            if(eye.distanceTo(actor.getEyePosition())<2)token.reject("observer_camera_overlaps_actor");
            var projection=mc.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f());
            var torso=actor.position().add(0,1,0).subtract(eye);var projected=projection.transform(new org.joml.Vector4f((float)torso.x,(float)torso.y,(float)torso.z,1));
            if(!projected.isFinite()||projected.w<=0||Math.abs(projected.x/projected.w)>=.85||Math.abs(projected.y/projected.w)>=.85)token.reject("original_actor_outside_viewport");
            for(double y:new double[]{.25,.9,1.55})for(double x:new double[]{-.15,0,.15}){
                var point=actor.position().add(x,y,0);
                if(mc.level.clip(new net.minecraft.world.level.ClipContext(eye,point,net.minecraft.world.level.ClipContext.Block.VISUAL,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)token.reject("world_geometry_occludes_actor");
                for(var entity:mc.level.entitiesForRendering())if(entity instanceof LivingEntity&&entity!=actor&&entity!=mc.player&&!entity.isInvisible()
                    &&entity.getBoundingBox().inflate(.1).clip(eye,point).isPresent())token.reject("other_body_occludes_actor");
            }
            token.observe("observerPosition",mc.player.position());token.observe("observerYaw",mc.player.getYRot());token.observe("observerPitch",mc.player.getXRot());
        }
        if(mc.isPaused()||mc.level.tickRateManager().isEntityFrozen(actor)||mc.level.tickRateManager().isEntityFrozen(mc.player))token.reject("paused_or_frozen");
        var timeline=MastersArtsClient.timeline(actor);
        if(e.phase().equals("NONE")){if(timeline!=null)token.reject("neutral_timeline_present");}
        else if(timeline==null||timeline.entity()!=e.owner()||timeline.move()!=e.move()||timeline.startTick()!=e.activation()||timeline.windup()!=e.windup()||timeline.recovery()!=e.recovery())token.reject("wrong_accepted_timeline");
        token.observe("acceptedSourceIdentity",requireSource(timeline,e.activation()));
        CounterPeerAdmissionProbe.sourceFields(timeline,"receivedSource").forEach(token::observe);
        token.observe("receivedOwnerUuid",actor.getUUID());token.observe("receivedOwnerEntity",actor.getId());token.observe("receivedLevel",mc.level.dimension().identifier());
        token.observe("receivedConnectionIdentity",Integer.toUnsignedString(System.identityHashCode(mc.getConnection())));
        token.observe("timeline",timeline);token.observe("camera",mc.options.getCameraType());token.observe("observerCoverage",remote);
        token.observe("tick",mc.level.getGameTime());token.observe("partial",options.deltaTicks);token.observe("owner",actor.getUUID());
        token.observe("observerUuid",mc.player.getUUID());token.observe("observerEntity",mc.player.getId());
        if(!(actor instanceof net.minecraft.client.player.AbstractClientPlayer connectedActor))throw new AssertionError("Actor is not an actual connected client player");
        s.originalSkin=connectedActor.getSkin();
        s.originalModel=((net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(actor)).getModel();
        token.observe("connectedSkinTexture",s.originalSkin.body().texturePath());token.observe("connectedSkinModel",s.originalSkin.model());
        if(token.paired!=null){
            var proof=CounterPeerPhaseContract.readAccepted(token.paired.acceptedReceipt(),token.paired.acceptedIdentity());
            if(proof.accepted()!=e.activation())token.reject("accepted_action_changed");
            token.releaseRead=SEQ.incrementAndGet();token.observe("acceptedReceiptSha256",proof.sha256());
            token.observe("acceptedReadSequence",token.releaseRead);token.observe("pairedRole",token.paired.role());
            token.observe("admissionReceiptSha256",token.paired.acceptedIdentity().get("admissionReceiptSha256"));token.observe("admissionGeneration",token.paired.acceptedIdentity().get("admissionGeneration"));
            token.observe("sourceHead",System.getProperty("wildercord.mp.sourceHead"));token.observe("runIdentity",System.getProperty("wildercord.mp.runIdentity"));
        }
        token.observe("viewport",mc.getWindow().getWidth()+"x"+mc.getWindow().getHeight());
        },CounterPeerRenderProbe::clearScope);
    }
    public static void leave(Scope s){if(s==null)return;
        try{clearScope(s);}finally{CrimsonMoonScopeGuard.restore(CURRENT,s.previous);}
        if(s.token!=null){s.token.scopeCleanupVerified=CURRENT.get()==s.previous&&s.sources.isEmpty()&&s.views.isEmpty()&&s.worlds.isEmpty()
            &&s.nativeBody.empty()&&s.baselines.isEmpty()&&s.layerBaselines.isEmpty()&&s.ids.isEmpty()&&s.view==null&&s.worldItem==null&&s.deferred==null&&s.fallback==null
            &&s.target==null&&s.texture==null&&s.originalSkin==null&&s.originalModel==null;
            if(!s.token.scopeCleanupVerified)s.token.reject("failed_scope_cleanup");}
    }
    private static void clearScope(Scope s){s.nativeBody.clear();s.sources.clear();s.views.clear();s.worlds.clear();s.layerBaselines.clear();s.baselines.clear();s.ids.clear();s.view=null;s.worldItem=null;s.deferred=null;s.fallback=null;s.target=null;s.texture=null;s.originalSkin=null;s.originalModel=null;}
    public static void extractBegin(DeltaTracker delta){var s=CURRENT.get();if(s!=null){s.extracting=true;s.token.observe("renderPartial",delta.getGameTimeDeltaPartialTick(false));}}
    public static void extractEnd(boolean completed){var s=CURRENT.get();if(s!=null){s.extracting=false;if(completed)s.token.extracted=SEQ.incrementAndGet();}}
    public static void extracted(Entity entity,float partial,EntityRenderState state){
        var s=CURRENT.get();if(s==null||!s.extracting||entity.getId()!=s.token.expected.owner()||!(state instanceof AvatarRenderState a))return;
        var e=s.token.expected;var c=a.getData(MastersArtPose.FRAME);var r=a.getData(ArticulatedCombat.FRAME);
        float age=entity.level().getGameTime()-e.activation()+partial;
        if(c!=null){
            Origin origin;synchronized(ORIGINS){origin=ORIGINS.get(c);}
            if(origin==null||!origin.owner().toString().equals(e.uuid())||origin.entity()!=e.owner()||origin.activation()!=e.activation())s.token.reject("missing_exact_classic_origin");else {age=origin.age();s.token.sourceSequence=origin.sequence();s.token.observe("sourceFrameSequence",origin.sequence());}
            if(c.activation()!=e.activation()||c.move()!=e.move()||c.leftHanded()!=e.hand().equals("LEFT")||!c.pose().equals(MastersArtAnimation.sample(e.move(),age,e.windup(),e.recovery()))||Float.floatToIntBits(c.bladeTilt())!=Float.floatToIntBits(MastersArtAnimation.bladeTilt(e.move(),age,e.windup(),c.pose().weight())))s.token.reject("wrong_classic_source");
        }
        String phase=CounterPeerPhaseContract.phase(e.move(),age);
        if(c==null&&!phase.equals("NONE"))s.token.reject("missing_active_classic_source");
        var expectedBody=phase.equals("NONE")?ArticulatedCombatPose.NONE:ArticulatedCombatPose.samplePlayer(e.move(),age,e.windup(),e.recovery(),e.hand().equals("LEFT"));
        var expectedView=ArticulatedCombatPose.view(expectedBody,e.hand().equals("LEFT"));
        if(r!=null&&r.move()==e.move()){
            var expected=expectedBody;
            if(r.activation()!=e.activation()||r.master()||r.leftHanded()!=e.hand().equals("LEFT")
                ||r.pose().phase()!=expectedBody.phase()||!hashPose(r.pose()::local,r.pose().weight()).equals(hashPose(expected::local,expected.weight())))s.token.reject("wrong_articulated_source");
        }
        if((c!=null&&(!Float.isFinite(c.yawDelta())||!Float.isFinite(c.pitchDelta())||!Float.isFinite(c.bladeTilt())))
            ||(r!=null&&(!Float.isFinite(r.yawDelta())||!Float.isFinite(r.pitchDelta()))))s.token.reject("nonfinite_aim");
        if(e.mode().equals("classic")){if(r!=null)s.token.reject("classic_has_stale_articulated_frame");}
        else if(r==null||r.master()||r.leftHanded()!=e.hand().equals("LEFT")
            ||r.move()!=(phase.equals("NONE")?-1:e.move())||r.activation()!=(phase.equals("NONE")?Long.MIN_VALUE:e.activation()))s.token.reject("wrong_complete_articulated_identity");
        if(r!=null&&r.pose().phase()!=expectedBody.phase())s.token.reject("actual_articulated_phase_changed");
        if(phase.equals("NONE")){
            if(c!=null)s.token.reject("neutral_classic_source_present");
            if(r!=null&&!CounterPeerRenderMath.neutralPose(key(r),e.hand().equals("LEFT"),r.pose()))
                s.token.reject("neutral_articulated_source_is_not_authoritative_none");
        }
        if(!phase.equals(e.phase()))s.token.reject("actual_phase_differs_before_draw");
        if(r!=null&&!CounterPeerRenderMath.viewSource(expectedBody,r.pose(),e.hand().equals("LEFT")).matched())
            s.token.reject("wrong_authoritative_source_view");
        var p=new Palette(c==null?Long.MIN_VALUE:c.activation(),c==null?-1:c.move(),phase,age,key(c),key(r));
        ItemStack off=offhand(a);boolean shell=a.getData(AuraShellLayer.SHELL_GLOW)!=null;
        var model=s.originalModel;
        if(!(entity instanceof net.minecraft.client.player.AbstractClientPlayer connected)||!connected.getSkin().equals(s.originalSkin)
            ||!a.skin.equals(s.originalSkin)||!CounterPeerRenderMath.originalSkin(s.originalSkin.body().texturePath().toString(),a.skin.body().texturePath().toString(),
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
    public static void renderEnd(boolean completed){var s=CURRENT.get();if(s!=null){s.rendering=false;if(completed){try{s.nativeBody.requireHealthy();if(s.token.expected.view().equals("remote")){s.nativeBody.requireComplete();s.token.observe("exactPrimaryBodySubmitVerified",true);}}catch(Throwable failure){s.token.reject("native_body_observer:"+failure);}s.token.rendered=SEQ.incrementAndGet();}}}
    public static <T> T passive(java.util.function.Supplier<T> observer,T fallback){var s=CURRENT.get();return s==null?fallback:s.nativeBody.observe(observer,fallback);}
    public static void passive(Runnable observer){var s=CURRENT.get();if(s!=null)s.nativeBody.observe(observer);}
    private static Scope rendering(){var s=CURRENT.get();return s!=null&&s.rendering?s:null;}
    private static NativeBodySubmission.Frames bodyFrames(AvatarRenderState a){return new NativeBodySubmission.Frames(a.getData(MastersArtPose.FRAME),a.getData(ArticulatedCombat.FRAME));}
    public static NativeBodySubmission.Origin worldSubmissionBegin(Model<?> model,Object state,Matrix4fc root,RenderType material){
        var s=rendering();if(s==null||!s.token.expected.view().equals("remote")||!(state instanceof AvatarRenderState a)||a.id!=s.token.expected.owner())return null;
        return s.nativeBody.begin(model,state,bodyFrames(a),material,root.get(new float[16]));
    }
    public static void worldSubmissionEnd(NativeBodySubmission.Origin call,boolean completed){var s=rendering();if(s!=null&&call!=null)s.nativeBody.submittedEnd(call,completed);}
    public static void nativeSubmitted(Object node,Model<?> model,Object state,Object material,float[] root,boolean outline){
        var s=rendering();if(s==null||!s.token.expected.view().equals("remote")||!(state instanceof AvatarRenderState a)||a.id!=s.token.expected.owner())return;
        s.nativeBody.submitted(node,model,state,bodyFrames(a),material,root,outline);
    }
    public static DeferredCall deferredEnter(Object node,Model<?> model,Object state,Object material,float[] root){
        var s=rendering();if(s==null)return null;NativeBodySubmission.Visit body=null;
        if(s.token.expected.view().equals("remote")&&(s.nativeBody.tracked(node)||state instanceof AvatarRenderState a&&a.id==s.token.expected.owner())){
            if(!(state instanceof AvatarRenderState a))throw new AssertionError("Tracked counter native Submit changed state type");
            body=s.nativeBody.enter(node,model,state,bodyFrames(a),material,root);
        }
        var call=new DeferredCall(node,model,state,body,s.deferred);s.deferred=call;return call;
    }
    public static void bodyDrawn(Object node,Model<?> model,Object state,float[] root){
        var s=rendering();if(s==null||s.deferred==null||s.deferred.body()==null)return;
        if(!(state instanceof AvatarRenderState a))throw new AssertionError("Counter native body draw changed state type");
        var source=s.sources.get(a);if(a.id!=s.token.expected.owner()||source==null||!matches(s,source,a))throw new AssertionError("Counter native body draw changed original owner/source");
        s.nativeBody.drawn(s.deferred.body(),node,model,state,bodyFrames(a),root);
    }
    public static void deferredLeave(DeferredCall call,boolean completed){
        var s=rendering();if(s==null||call==null)return;
        try{if(call.body()!=null)s.nativeBody.leave(call.body(),completed);}finally{s.deferred=call.previous();}
    }
    public static void bodyBaseline(PlayerModel model,AvatarRenderState a){
        var s=rendering();if(s==null||a.id!=s.token.expected.owner())return;
        if(s.deferred==null){if(s.token.expected.view().equals("remote")&&s.sources.containsKey(a)&&s.sources.get(a).model()==model)s.layerBaselines.put(a,rigid(model));return;}
        if(s.deferred.body()==null||!s.deferred.body().primary())return;
        s.nativeBody.baseline(s.deferred.body(),model,a,bodyFrames(a));
        if(s.baselines.put(model,rigid(model))!=null)s.token.reject("duplicate_vanilla_baseline");
    }
    public static void bodyPalette(PlayerModel model,AvatarRenderState a){
        var s=rendering();if(s==null||a.id!=s.token.expected.owner()||s.deferred==null||s.deferred.body()==null||!s.deferred.body().primary())return;
        var source=s.sources.get(a);var baseline=s.baselines.remove(model);Comparison proof=null;
        var access=model instanceof ArticulatedModelAccess x&&x.wildercord$bodyOwned()?x:null;
        boolean segmented=access!=null&&bodyVisible(access.wildercord$rig()),rigid=rigidVisible(model)&&(access==null||!access.wildercord$rig().root.visible);
        if(source==null||source.model()!=model||baseline==null)s.token.reject("missing_exact_owner_vanilla_baseline");
        else if(s.token.expected.mode().equals("articulated")&&!source.palette().phase().equals("NONE")){
            if(access==null||source.articulated()==null)s.token.reject("missing_actual_body_rig");
            else{var rig=access.wildercord$rig();proof=CounterPeerRenderMath.compare("deferred_body_locals",
                CounterPeerRenderMath.values(CounterPeerRenderMath.body(source.expectedBody(),baseline,rig.slim())),CounterPeerRenderMath.values(parts(rig)));
                root(s,rig.root);if(rig.slim()!=a.skin.model().toString().equalsIgnoreCase("slim"))s.token.reject("wrong_body_width");}
        }else if(source!=null&&baseline!=null)proof=CounterPeerRenderMath.compare("deferred_classic_locals",
            CounterPeerRenderMath.values(CounterPeerRenderMath.classic(source.classic()==null?null:source.classic().pose(),a.mainArm==HumanoidArm.LEFT,baseline)),
            CounterPeerRenderMath.values(rigid(model)));
        if(s.token.expected.view().equals("remote")){
            var world=s.worlds.get(a);
            if(world==null||world.model()!=model)s.token.reject("deferred_body_without_retained_outer_root");
            else if(proof!=null){
                var expected=new ArrayList<>(proof.expected());expected.addAll(CounterPeerRenderMath.outerRoot(world.modelRoot(),world.modelRootVisible()));
                var actual=new ArrayList<>(proof.actual());actual.addAll(CounterPeerRenderMath.outerRoot(part(model.root()).matrix(),model.root().visible));
                proof=CounterPeerRenderMath.compare("deferred_body_locals_and_retained_outer_root",expected,actual);
            }
        }
        pass(s,"body",a,model,null,proof,segmented,rigid);
        s.nativeBody.palette(s.deferred.body(),model,a,bodyFrames(a));
    }
    /** Captures the actual body submission and its material, before any deferred model reuse. */
    public static void worldSubmitted(Model<?> model,Object state,Matrix4fc root,RenderType material){
        var s=rendering();if(s==null||!s.token.expected.view().equals("remote")||!(state instanceof AvatarRenderState a)||a.id!=s.token.expected.owner())return;
        var source=s.sources.get(a);
        if(source==null||source.model()!=model||!(model instanceof PlayerModel player)){s.token.reject("world_submit_without_exact_model");return;}
        var expectedMaterial=player.renderType(source.originalSkin().body().texturePath());
        if(!CounterPeerRenderMath.originalSkin(source.originalSkin().body().texturePath().toString(),a.skin.body().texturePath().toString(),
            source.originalSkin().model().getSerializedName(),a.skin.model().getSerializedName(),material==expectedMaterial))s.token.reject("wrong_world_skin_material");
        var submittedModelRoot=part(player.root()).matrix();
        var rootProof=CounterPeerRenderMath.compare("world_root_identity",CounterPeerRenderMath.outerRoot(new Matrix4f(),true),CounterPeerRenderMath.outerRoot(submittedModelRoot,player.root().visible));
        // Store a detached snapshot only after comparing the enclosing root against its authoritative bind.
        if(s.worlds.put(a,new World(a,player,new Matrix4f(root),new Matrix4f(submittedModelRoot),player.root().visible,material))!=null)s.token.reject("duplicate_world_body_submit");
        s.token.observe("originalSkinMaterialIdentity",id(s,expectedMaterial));s.token.observe("submittedSkinMaterialIdentity",id(s,material));
        // Both deferred preparation and the actual item-layer entry must retain this verified outer root.
        pass(s,"body_submit",a,model,null,rootProof,s.token.expected.mode().equals("articulated"),s.token.expected.mode().equals("classic"));
    }
    /** The baseline comes from the real vanilla-to-articulated boundary for this same state/model. */
    public static WorldItem worldItemBegin(Object parent,ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector){
        var s=rendering();if(s==null||!s.token.expected.view().equals("remote")||!(state instanceof AvatarRenderState a)||a.id!=s.token.expected.owner()||arm!=a.mainArm)return null;
        var source=s.sources.get(a);var world=s.worlds.get(a);var baseline=s.layerBaselines.remove(a);
        if(source==null||world==null||parent!=source.model()||world.model()!=parent||baseline==null){s.token.reject("world_item_without_exact_vanilla_baseline");return null;}
        var model=source.model();boolean same=ItemStack.isSameItemSameComponents(stack,source.main())&&stack.getCount()==source.main().getCount();
        boolean eligible=CounterPeerRenderMath.item(id(s,source.item()),id(s,item),stack.is(Items.DIAMOND_SWORD),offhand(a).isEmpty(),same)
            &&!item.isEmpty()&&!a.isUsingItem&&!a.isBaby&&a.ticksUsingItem(arm)==0
            &&(a.currentSwing==null||a.currentSwing.animation().type()!=SwingAnimationType.STAB)&&ArticulatedCombat.frame(a)==source.articulated();
        s.token.observe("worldHandEligible",eligible);if(!eligible)s.token.reject("ineligible_ordinary_world_hand");
        if(!CounterPeerRenderMath.compare("world_item_root",CounterPeerRenderMath.values(world.root()),CounterPeerRenderMath.values(pose.last().pose())).matched())s.token.reject("changed_world_item_root");
        if(!CounterPeerRenderMath.compare("world_item_retained_model_root",CounterPeerRenderMath.outerRoot(world.modelRoot(),world.modelRootVisible()),
            CounterPeerRenderMath.outerRoot(part(model.root()).matrix(),model.root().visible)).matched())s.token.reject("changed_world_item_model_root");
        var rig=((ArticulatedModelAccess)model).wildercord$rig();
        Matrix4f expected;
        if(s.token.expected.mode().equals("articulated")){
            var expectedBody=CounterPeerRenderMath.body(source.expectedBody(),baseline,rig.slim());
            expected=CounterPeerRenderMath.sword(new Matrix4f(world.root()).mul(world.modelRoot()),expectedBody,arm==HumanoidArm.LEFT,rig.slim());
        }else{
            var expectedBody=CounterPeerRenderMath.classic(source.classic().pose(),arm==HumanoidArm.LEFT,baseline);
            expected=CounterPeerRenderMath.classicSword(new Matrix4f(world.root()).mul(world.modelRoot()),expectedBody,arm==HumanoidArm.LEFT,rig.slim(),source.classic().bladeTilt());
        }
        var call=new WorldItem(a,model,item,expected,pose,collector,s.worldItem);s.worldItem=call;return call;
    }
    public static void worldItemSubmitted(WorldItem call,ItemStackRenderState item,Matrix4fc actual){
        var s=rendering();if(s==null||call==null)return;
        if(s.worldItem!=call||call.item()!=item)s.token.reject("substituted_world_item_submission");
        pass(s,"world_item",call.avatar(),call.model(),item,CounterPeerRenderMath.compare("actual_world_socket_item_matrix",
            CounterPeerRenderMath.values(call.expected()),CounterPeerRenderMath.values(actual)),s.token.expected.mode().equals("articulated"),s.token.expected.mode().equals("classic"));
    }
    public static void worldItemEnd(WorldItem call,boolean completed){var s=rendering();if(s!=null&&call!=null){if(!completed)s.token.reject("incomplete_world_item_scope");s.worldItem=call.previous();}}
    public static ViewCall viewEnter(AvatarRenderState a,float partial,InteractionHand hand,PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector){
        var s=rendering();if(s==null||a.id!=s.token.expected.owner())return null;var call=new ViewCall(a,hand,new Matrix4f(stack.last().pose()),stack,collector,s.view);s.view=call;return call;
    }
    public static void viewLeave(ViewCall call,boolean submitted){var s=rendering();if(s!=null&&call!=null)s.view=call.previous();}
    public static void viewAttempt(AvatarRenderState a,FirstPersonHandsAndItemsRenderState h,float partial,InteractionHand hand){
        var s=rendering();if(s==null||a==null||a.id!=s.token.expected.owner()||hand!=InteractionHand.MAIN_HAND)return;
        var source=s.sources.get(a);boolean known=h instanceof MastersHandMotionState;
        boolean equipping=known&&((MastersHandMotionState)h).wildercord$mainHandEquipping();
        boolean same=source!=null&&ItemStack.isSameItemSameComponents(source.main(),h.mainHandItem);
        float height=net.minecraft.util.Mth.lerp(partial,h.oldMainHandHeight,h.mainHandHeight);
        boolean active=source!=null&&!source.palette().phase().equals("NONE")&&source.expectedBody().weight()>0;
        if(!CounterPeerRenderMath.handEligible(active,height,same,known,equipping))s.token.reject("ordinary_hand_admission_is_not_eligible");
        s.token.observe("handEquipKnown",known);s.token.observe("handEquipping",equipping);s.token.observe("handSameItem",same);
        s.token.observe("handInterpolatedHeight",height);s.token.observe("ordinaryHandAdmission",CounterPeerRenderMath.handEligible(active,height,same,known,equipping));
    }
    public static void viewSubmitted(Model<?> model,Object state,Matrix4fc actual,RenderType material){
        var s=rendering();if(s==null||s.view==null||s.view.hand()!=InteractionHand.MAIN_HAND||!(model instanceof ArticulatedViewModel view)||!(state instanceof ArticulatedViewModel.Frame frame))return;
        var a=s.view.avatar();var source=s.sources.get(a);if(source==null||source.articulated()==null){s.token.reject("missing_view_source");return;}
        if(!(source.model() instanceof ArticulatedModelAccess access)||access.wildercord$viewModel()!=model)s.token.reject("wrong_owner_view_model");
        var r=source.articulated();boolean neutral=source.palette().phase().equals("NONE");
        var expected=source.expectedView();
        if(expected.phase()!=frame.pose().phase()||!hashPose(expected::local,expected.weight()).equals(hashPose(frame.pose()::local,frame.pose().weight())))s.token.reject("wrong_submitted_view_palette");
        var matrix=CounterPeerRenderMath.viewRoot(s.view.before(),expected,source.expectedBody().weight(),neutral?0:r.yawDelta(),neutral?0:r.pitchDelta());
        RenderType expectedMaterial=RenderTypes.entityTranslucent(source.originalSkin().body().texturePath());
        if(!CounterPeerRenderMath.originalSkin(source.originalSkin().body().texturePath().toString(),a.skin.body().texturePath().toString(),
            source.originalSkin().model().getSerializedName(),a.skin.model().getSerializedName(),material==expectedMaterial))s.token.reject("submitted_skin_material_is_not_original");
        s.token.observe("originalSkinMaterialIdentity",id(s,expectedMaterial));s.token.observe("submittedSkinMaterialIdentity",id(s,material));
        s.views.put(frame,new View(a,view,frame,expected,matrix,material));
        pass(s,"view_submit",a,model,null,CounterPeerRenderMath.compare("view_root_matrix",CounterPeerRenderMath.values(matrix),CounterPeerRenderMath.values(actual)),true,false);
    }
    public static void viewPalette(ArticulatedViewModel model,ArticulatedViewModel.Frame frame){
        var s=rendering();if(s==null||s.deferred==null||s.deferred.model()!=model||s.deferred.state()!=frame)return;
        var view=s.views.get(frame);if(view==null||view.model()!=model){s.token.reject("missing_unique_view_submit");return;}
        root(s,model.rig().root);if(model.rig().slim()!=view.avatar().skin.model().toString().equalsIgnoreCase("slim"))s.token.reject("wrong_view_width");
        pass(s,"view_deferred",view.avatar(),model,null,CounterPeerRenderMath.compare("deferred_view_locals",
            CounterPeerRenderMath.values(CounterPeerRenderMath.palette(view.expectedPose()::local)),CounterPeerRenderMath.values(parts(model.rig()))),viewVisible(model.rig()),false);
    }
    public static void viewItem(ItemStackRenderState item,Matrix4fc actual){
        var s=rendering();if(s==null||s.view==null||s.view.hand()!=InteractionHand.MAIN_HAND)return;
        var a=s.view.avatar();var source=s.sources.get(a);var candidates=s.views.values().stream().filter(v->v.avatar()==a).toList();
        if(source==null||candidates.size()!=1){s.token.reject("item_without_unique_view");return;}var view=candidates.getFirst();
        if(!CounterPeerRenderMath.item(id(s,source.item()),id(s,item),a.getMainHandItemStack().is(Items.DIAMOND_SWORD),offhand(a).isEmpty(),
            ItemStack.isSameItemSameComponents(source.main(),a.getMainHandItemStack()))||item.isEmpty())s.token.reject("wrong_articulated_item");
        var expected=CounterPeerRenderMath.sword(view.root(),CounterPeerRenderMath.palette(view.expectedPose()::local),a.mainArm==HumanoidArm.LEFT,view.model().rig().slim());
        pass(s,"view_item",a,view.model(),item,CounterPeerRenderMath.compare("actual_socket_item_matrix",CounterPeerRenderMath.values(expected),CounterPeerRenderMath.values(actual)),true,false);
    }
    public static FallbackView fallbackViewBegin(FirstPersonHandsAndItemsRenderer renderer,AvatarRenderState a,FirstPersonHandsAndItemsRenderState hands,
        InteractionHand hand,ItemStack stack,float attack,float inverse,PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector){
        var s=rendering();if(s==null||a==null||a.id!=s.token.expected.owner()||hand!=InteractionHand.MAIN_HAND)return null;
        var source=s.sources.get(a);if(source==null){s.token.reject("native_without_extraction");return null;}
        if(!stack.is(Items.DIAMOND_SWORD)||!ItemStack.isSameItemSameComponents(stack,hands.mainHandItem)||!ItemStack.isSameItemSameComponents(stack,source.main()))s.token.reject("wrong_native_stack");
        var c=source.classic();boolean equipping=!(hands instanceof MastersHandMotionState motion)||motion.wildercord$mainHandEquipping();
        float ownership=c==null||equipping?0:MastersViewMotion.ownership(c.pose().weight(),true);
        boolean whack=a.currentSwing!=null&&a.currentSwing.hand()==InteractionHand.MAIN_HAND&&a.currentSwing.animation().type()==SwingAnimationType.WHACK;
        if(a.isUsingItem||a.isAutoSpinAttack||hands.isScoping||a.currentSwing!=null&&a.currentSwing.animation().type()==SwingAnimationType.STAB)s.token.reject("unsupported_native_item_motion");
        var before=new Matrix4f(pose.last().pose());var expected=CounterPeerRenderMath.nativeSword(before,c==null?null:c.pose(),a.mainArm==HumanoidArm.LEFT,inverse,c==null?0:c.yawDelta(),c==null?0:c.pitchDelta(),ownership,attack,whack);
        var call=new FallbackView(a,hands,renderer,before,expected,inverse,ownership,pose,collector,s.fallback);s.fallback=call;return call;
    }
    public static ClassicCall classicBegin(PoseStack pose,InteractionHand hand,AvatarRenderState a,float inverse){
        var s=rendering();if(s==null||s.fallback==null||s.fallback.avatar!=a||hand!=InteractionHand.MAIN_HAND)return null;var call=s.fallback;var source=s.sources.get(a);var c=source==null?null:source.classic();
        var before=new Matrix4f(call.before).translate(0,MastersViewMotion.heightCompensation(call.inverse,call.ownership),0);
        if(inverse!=call.inverse||!CounterPeerRenderMath.compare("height_compensation",CounterPeerRenderMath.values(before),CounterPeerRenderMath.values(pose.last().pose())).matched())s.token.reject("wrong_classic_height_compensation");
        return new ClassicCall(call,before.mul(CounterPeerRenderMath.classicDelta(c==null?null:c.pose(),a.mainArm==HumanoidArm.LEFT,inverse,c==null?0:c.yawDelta(),c==null?0:c.pitchDelta())));
    }
    public static void classicEnd(ClassicCall call,PoseStack pose){
        var s=rendering();if(s==null||call==null)return;var owner=call.owner();if(owner.transform)s.token.reject("duplicate_classic_transform");owner.transform=true;
        pass(s,"classic_transform",owner.avatar,owner.renderer,null,CounterPeerRenderMath.compare("observed_classic_before_after",
            CounterPeerRenderMath.values(call.expected()),CounterPeerRenderMath.values(pose.last().pose())),false,true);
    }
    public static void fallbackViewItem(FallbackView call,ItemStackRenderState item,Matrix4fc actual){
        var s=rendering();if(s==null||call==null)return;var source=s.sources.get(call.avatar);
        if(source==null||!CounterPeerRenderMath.item(id(s,call.item),id(s,item),call.avatar.getMainHandItemStack().is(Items.DIAMOND_SWORD),
            offhand(call.avatar).isEmpty(),ItemStack.isSameItemSameComponents(source.main(),call.hands.mainHandItem))||item.isEmpty())s.token.reject("wrong_native_item");
        if(!call.transform)s.token.reject("skipped_classic_transform");
        pass(s,"native_item",call.avatar,call.renderer,item,CounterPeerRenderMath.compare("actual_native_item_matrix",CounterPeerRenderMath.values(call.expectedItem),CounterPeerRenderMath.values(actual)),false,true);
    }
    public static void fallbackViewEnd(FallbackView call,boolean completed){var s=rendering();if(s!=null&&call!=null){if(!completed)s.token.reject("incomplete_native_call");s.fallback=call.previous;}}
    private static void pass(Scope s,String kind,AvatarRenderState a,Object model,Object item,Comparison proof,boolean segmented,boolean rigid){
        var e=s.sources.get(a);boolean matched=e!=null&&matches(s,e,a);if(proof==null||!proof.matched())s.token.reject("geometry_mismatch:"+kind);
        s.token.pass(new Pass(kind,binding(s,a,model,item),proof,segmented,rigid));
    }
    private static Binding binding(Scope s,AvatarRenderState a,Object model,Object item){
        var e=s.sources.get(a);boolean matched=e!=null&&matches(s,e,a);
        return new Binding(e==null?0:e.source(),id(s,a),id(s,model),id(s,item!=null?item:s.token.expected.view().equals("remote")&&e!=null?e.item():null),skinMaterial(s,a,model),a.id,e==null?"missing":e.uuid(),
            a.skin.model().toString(),a.skin.body().texturePath().toString(),a.mainArm.name(),e==null?null:e.palette(),matched);
    }
    /** Deep native draw after baked ItemTransform and localTransform. Missing/foreign nodes cannot satisfy entry receipts. */
    public static void itemDraw(ItemStackRenderState item,net.minecraft.client.resources.model.cuboid.ItemTransform transform,Matrix4fc local,
        ItemDisplayContext context,net.minecraft.client.resources.model.geometry.ItemQuads quads,Matrix4fc actual,PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector){
        var s=rendering();if(s==null)return;AvatarRenderState a;Object model;Matrix4f entry;PoseStack entryStack;net.minecraft.client.renderer.SubmitNodeCollector entryCollector;
        boolean remote=s.token.expected.view().equals("remote");
        if(remote){var call=s.worldItem;if(call==null||call.item()!=item)return;a=call.avatar();model=call.model();entry=call.expected();entryStack=call.stack();entryCollector=call.collector();}
        else if(s.view!=null&&s.view.hand()==InteractionHand.MAIN_HAND){
            a=s.view.avatar();var source=s.sources.get(a);if(source==null||source.item()!=item)return;
            var views=s.views.values().stream().filter(v->v.avatar()==a).toList();if(views.size()!=1){s.token.reject("deep_draw_without_unique_view");return;}
            var view=views.getFirst();model=view.model();entryStack=s.view.stack();entryCollector=s.view.collector();entry=CounterPeerRenderMath.sword(view.root(),CounterPeerRenderMath.palette(view.expectedPose()::local),a.mainArm==HumanoidArm.LEFT,view.model().rig().slim());
        }else{var call=s.fallback;if(call==null||call.item!=item)return;a=call.avatar;model=call.renderer;entry=call.expectedItem;entryStack=call.stack;entryCollector=call.collector;}
        if(entryStack!=stack||entryCollector!=collector)s.token.reject("substituted_deep_draw_stack_or_collector");
        var expectedContext=remote?(a.mainArm==HumanoidArm.LEFT?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
            :(a.mainArm==HumanoidArm.LEFT?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        if(context!=expectedContext||quads==null||quads.isEmpty())s.token.reject("wrong_or_empty_deep_item_draw");
        var rotation=List.of(transform.rotation().x(),transform.rotation().y(),transform.rotation().z());
        var translation=List.of(transform.translation().x(),transform.translation().y(),transform.translation().z());
        var scale=List.of(transform.scale().x(),transform.scale().y(),transform.scale().z());
        var stockRotation=List.of(0F,context.leftHand()?90F:-90F,(context.leftHand()?-1:1)*(remote?55F:25F));
        var stockTranslation=remote?List.of(0F,4F/16,.5F/16):List.of(1.13F/16,3.2F/16,1.13F/16);
        float size=remote?.85F:.68F;
        if(!CounterPeerRenderMath.compare("stock_display_rotation",stockRotation,rotation).matched()
            ||!CounterPeerRenderMath.compare("stock_display_translation",stockTranslation,translation).matched()
            ||!CounterPeerRenderMath.compare("stock_display_scale",List.of(size,size,size),scale).matched()
            ||!CounterPeerRenderMath.compare("stock_display_local",CounterPeerRenderMath.values(new Matrix4f()),CounterPeerRenderMath.values(local)).matched())s.token.reject("changed_original_diamond_display");
        var display=CounterPeerRenderMath.display(new Matrix4f(),context.leftHand(),transform.rotation(),transform.translation(),transform.scale(),
            transform==net.minecraft.client.resources.model.cuboid.ItemTransform.NO_TRANSFORM,local);
        var expected=new Matrix4f(entry).mul(display);
        var modelPoint=new org.joml.Vector3f(3.5F/16,3.5F/16,8F/16);
        var wanted=CounterPeerRenderMath.point(expected,modelPoint);var consumed=CounterPeerRenderMath.point(actual,modelPoint);
        var geometry=CounterPeerRenderMath.compare("actual_displayed_item_matrix",CounterPeerRenderMath.values(expected),CounterPeerRenderMath.values(actual));
        if(!geometry.matched()||!CounterPeerRenderMath.compare("displayed_anchor",wanted,consumed).matched())s.token.reject("wrong_displayed_item_or_hilt");
        var emitted=new ArrayList<Float>();
        if(quads!=null)for(var quad:quads.all())for(int v=0;v<4;v++){
            var point=quad.position(v);for(float component:new float[]{point.x(),point.y(),point.z()}){
                if(!Float.isFinite(component))s.token.reject("nonfinite_emitted_item_quad");emitted.add(component);
            }
        }
        var bytes=java.nio.ByteBuffer.allocate(emitted.size()*4);for(float value:emitted)bytes.putInt(Float.floatToRawIntBits(value));
        String quadHash=ArticulatedRenderReceipt.sha256(bytes.array());
        s.token.draw(new ItemDraw(binding(s,a,model,item),geometry,context.name(),quads==null?0:quads.all().size(),"stock_diamond_hilt",wanted,consumed,rotation,translation,scale,CounterPeerRenderMath.values(local),List.copyOf(emitted),quadHash,id(s,stack),id(s,collector),id(s,entryStack),id(s,entryCollector)));
    }
    private static long skinMaterial(Scope s,AvatarRenderState a,Object model){
        var world=s.worlds.get(a);if(world!=null&&world.model()==model)return id(s,world.material());
        var views=s.views.values().stream().filter(v->v.avatar()==a&&v.model()==model).toList();
        return views.size()==1?id(s,views.getFirst().material()):0;
    }
    private static boolean matches(Scope s,Extracted e,AvatarRenderState a){
        if(e.state()!=id(s,a)||e.classic()!=a.getData(MastersArtPose.FRAME)||e.articulated()!=a.getData(ArticulatedCombat.FRAME)
            ||!CounterPeerRenderMath.same(e.palette().classic(),key(a.getData(MastersArtPose.FRAME)))
            ||!CounterPeerRenderMath.same(e.palette().articulated(),key(a.getData(ArticulatedCombat.FRAME)))
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
            &&(Object)shellAdapter instanceof dev.wildercord.gametest.mixin.CounterPeerShellAccess shellAccess
            &&shellAccess.counter$slim()==source.originalSkin().model().getSerializedName().equals("slim");
        boolean modelOwned=CounterPeerRenderMath.modelOwned(model!=null&&model.getClass()==PlayerModel.class,owned,
            owned&&access.wildercord$rig().slim()==source.originalSkin().model().getSerializedName().equals("slim"),
            owned&&access.wildercord$viewModel()!=null&&access.wildercord$viewModel().rig().slim()==access.wildercord$rig().slim(),shellOwned);
        boolean compatible=modelOwned&&commonCompatible(a)&&ArticulatedArmorRenderer.supports(a);
        if(!e.mode().equals("classic"))compatible&=ArticulatedArmorRenderer.compatible(a)&&ArticulatedArmorRenderer.enabled()&&ArticulatedArmorRenderer.viewEnabled();
        return CounterPeerRenderMath.backend(e.mode(),enabled,shell,funded,compatible)
            &&(e.mode().equals("classic")? !ArticulatedArmorRenderer.enabled()&&!ArticulatedArmorRenderer.viewEnabled():true);
    }
    private static boolean commonCompatible(AvatarRenderState a){
        return a.deathTime<=0&&!a.isInvisible&&!a.isBaby&&!a.isUpsideDown&&!a.isAutoSpinAttack&&!a.isFallFlying&&!a.isPassenger&&!a.isUsingItem
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
    private static void root(Scope s,ModelPart root){if(!CounterPeerRenderMath.compare("rig_root",CounterPeerRenderMath.values(new Matrix4f()),CounterPeerRenderMath.values(part(root).matrix())).matched())s.token.reject("changed_rig_root");}
    private static String hash(String value){return ArticulatedRenderReceipt.sha256(value.getBytes(StandardCharsets.UTF_8));}
    private static String hashPose(java.util.function.Function<ArticulatedCombatPose.Joint,ArticulatedCombatPose.Transform> source,float weight){var text=new StringBuilder(Float.toHexString(weight));for(var j:ArticulatedCombatPose.Joint.values())text.append(j).append(source.apply(j));return hash(text.toString());}
    public static void screenshot(RenderTarget target,Consumer<NativeImage> consumer,java.util.function.BiConsumer<RenderTarget,Consumer<NativeImage>> original){
        var s=CURRENT.get();if(s==null){original.accept(target,consumer);return;}var token=s.token;
        if(target!=s.target||target.getColorTexture()!=s.texture)token.reject("changed_render_target");
        validateStages(token, false);
        if(!token.failures.isEmpty())throw new AssertionError("Counter pre-readback render receipt rejected: "+token.failures);
        Consumer<NativeImage> wrapped=image->{synchronized(token){
            if(++token.callbacks!=1||token.ended)token.reject("duplicate_or_late_readback");
            if(token.copy==null||token.copy.copySequence()<=token.copy.renderSequence())token.reject("missing_exact_native_copy");
            if(!token.failures.isEmpty()){image.close();throw new AssertionError("Counter callback cannot save rejected frame: "+token.failures);}
            token.pixels=ArticulatedRenderReceipt.pixels(image.getWidth(),image.getHeight(),image::getPixel);
        }consumer.accept(image);};
        var previous=COPYING.get();COPYING.set(new CopyCall(token,target,target.getColorTexture(),wrapped));try{original.accept(target,wrapped);}finally{COPYING.set(previous);}
    }
    public static void copyEnqueued(GpuTexture texture,int mip,RenderTarget target,Consumer<NativeImage> consumer){
        var call=COPYING.get();if(call==null||call.target()!=target||call.consumer()!=consumer)return;var t=call.token();
        if(texture!=call.texture()||target.getColorTexture()!=texture||mip!=0)t.reject("changed_copy_texture");
        synchronized(t){if(t.copy!=null)t.reject("duplicate_copy");else t.copy=new Copy(t.extracted,t.rendered,SEQ.incrementAndGet(),target.width,target.height,Map.copyOf(t.observations),List.copyOf(t.passes),List.copyOf(t.draws));}
    }
    public static void success(Token t,Path path){try{t.image=ArticulatedRenderReceipt.bind(path,FabricLoader.getInstance().getGameDir());}catch(Exception failure){t.reject("png_binding:"+failure);}
        validate(t);t.ended=true;persist(t);if(!t.failures.isEmpty())throw new AssertionError("Counter native receipt rejected: "+t.failures);}
    public static void failed(Token t,Throwable failure){t.reject("original_screenshot_failed:"+failure);t.ended=true;persist(t);}
    private static void validate(Token t){
        validateStages(t,true);
    }
    private static void validateStages(Token t, boolean saved){
        var c=t.copy;
        if(!saved)c=new Copy(t.extracted,t.rendered,SEQ.incrementAndGet(),CURRENT.get().target.width,CURRENT.get().target.height,Map.copyOf(t.observations),List.copyOf(t.passes),List.copyOf(t.draws));if(c==null||c.extractSequence()<=0||c.renderSequence()<=c.extractSequence()||c.copySequence()<=c.renderSequence())t.reject("missing_native_stages");
        if(saved&&(t.image==null||t.pixels==null||!t.pixels.equals(t.image.decodedPixels())))t.reject("png_pixels_not_bound");
        if(c==null)return;if(t.pixels!=null&&(c.width()!=t.pixels.width()||c.height()!=t.pixels.height()))t.reject("wrong_copy_dimensions");
        boolean fp=t.expected.view().equals("fp"),remote=t.expected.view().equals("remote")&&t.paired!=null;
        if(!fp&&!remote)t.reject("world_held_item_proof_incomplete");
        if(saved&&!t.scopeCleanupVerified)t.reject("missing_scope_cleanup");
        if(t.paired!=null&&!CounterPeerPhaseContract.sourceAfterAcceptance(t.releaseRead,t.sourceSequence,c.extractSequence()))t.reject("source_not_causally_after_server_acceptance");
        boolean art=t.expected.mode().equals("articulated"),active=!t.expected.phase().equals("NONE");
        if(fp&&(!"true".equals(c.observations().get("ordinaryHandAdmission"))||!"true".equals(c.observations().get("handEquipKnown"))
            ||!"false".equals(c.observations().get("handEquipping"))||!"true".equals(c.observations().get("handSameItem"))))t.reject("missing_eligible_ordinary_hand_witness");
        if(remote&&!"true".equals(c.observations().get("worldHandEligible")))t.reject("missing_ordinary_world_hand_witness");
        if(remote&&!"true".equals(c.observations().get("exactPrimaryBodySubmitVerified")))t.reject("missing_exact_primary_body_submit");
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
            if(p.geometry()==null||!p.geometry().matched()||!CounterPeerRenderMath.compare(p.geometry().operation(),p.geometry().expected(),p.geometry().actual()).matched())t.reject("unverified_geometry");
            if(p.kind().contains("item")&&b.item()<=0)t.reject("unbound_item");
            if(palette==null||!palette.phase().equals(t.expected.phase())||!CounterPeerPhaseContract.phase(t.expected.move(),palette.age()).equals(t.expected.phase()))t.reject("wrong_rendered_beat");
            if(palette!=null&&(active?(palette.activation()!=t.expected.activation()||palette.move()!=t.expected.move()):(palette.activation()!=Long.MIN_VALUE||palette.move()!=-1)))t.reject("wrong_activation");
            boolean segmented=art&&(fp||active);if(p.segmented()!=segmented||p.rigid()==segmented)t.reject("incomplete_backend");
        }
        if(c.draws().size()!=1)t.reject("missing_or_duplicate_deep_item_draw");
        else{
            var draw=c.draws().getFirst();var itemPass=c.passes().stream().filter(p->p.kind().endsWith("item")).toList();
            if(itemPass.size()!=1||!draw.binding().equals(itemPass.getFirst().binding())||draw.quads()<=0)t.reject("unbound_deep_item_draw");
            if(!draw.geometry().matched()||!CounterPeerRenderMath.compare("deep_draw",draw.geometry().expected(),draw.geometry().actual()).matched()
                ||!CounterPeerRenderMath.compare("displayed_anchor",draw.anchorExpected(),draw.anchorActual()).matched())t.reject("unverified_deep_item_draw");
        }
    }
    private static void persist(Token t){
        String nonce=System.getenv("WILDERCORD_COUNTER_RECEIPT_NONCE");if(nonce==null)nonce="unbound";
        var report=new Report(1,RUN,nonce,t.expected,t.copy,t.pixels,t.image,t.scopeCleanupVerified,t.copy!=null&&CounterPeerPhaseContract.sourceAfterAcceptance(t.releaseRead,t.sourceSequence,t.copy.extractSequence()),t.ended&&t.failures.isEmpty(),false,"actual_post_hitstop_source_palette",false,List.copyOf(t.failures));
        try{var dir=FabricLoader.getInstance().getGameDir().resolve("screenshots/counter-peer-receipts").resolve(RUN);Files.createDirectories(dir);
            var path=dir.resolve(String.format(Locale.ROOT,"%06d.json",SEQ.incrementAndGet()));Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n",StandardOpenOption.CREATE_NEW);
            if(report.verified()){synchronized(DONE){DONE.put(t.expected.name(),report);}synchronized(RECEIPTS){RECEIPTS.put(t.expected.name(),path);}}
            System.out.println("COUNTER_PEER_RENDER_RECEIPT name="+t.expected.name()+" verified="+report.verified()+" actualSourceAge="+t.observations.get("actualSourceAge")+" receipt="+path+" phaseBasis=actual_post_hitstop_source_palette serverReleaseFrameCorrespondenceVerified=false pixelQualityReviewed=false");
        }catch(Exception failure){throw new AssertionError("Could not persist native counter receipt",failure);}
    }
}
