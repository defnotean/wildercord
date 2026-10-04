package dev.wildercord.cast;

import dev.wildercord.mixin.ItemEntityAccessor;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.wildlife.MoonreedBlock;
import dev.wildercord.wildlife.WetlandGarden;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Six finite field signatures. No terrain scans outside loaded chunks and no inventory/fuel loss on refusal. */
public final class FieldFusions {
    private FieldFusions() {}
    private static final class Payment {
        final Map<String,Set<UUID>> targets=new HashMap<>();
        boolean target(String key,UUID id) {
            var ids=targets.computeIfAbsent(key,k -> new HashSet<>());
            return ids.size()<FieldFusionRules.TARGETS && ids.add(id);
        }
    }
    // Values never retain Cast/payment references, so expired spells do not become a permanent cache.
    private static final Map<Object,Payment> PAYMENTS=new WeakHashMap<>();
    private record Mark(Cast cast,LivingEntity target,Vec3 origin,long until,Object token) {}
    private static final Map<UUID,Mark> CLOCKS=new HashMap<>(), SKIES=new HashMap<>();
    private static final int MAX_MARKS=128;
    public static int clockMarks() {return CLOCKS.size();}
    public static int skyMarks() {return SKIES.size();}
    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {CLOCKS.clear();SKIES.clear();PAYMENTS.clear();});
        FieldFusionFeels.register();
    }
    private static Payment paid(Cast cast) {return PAYMENTS.computeIfAbsent(cast.payment(),key -> new Payment());}
    public static boolean apply(Cast cast,SpellPlan.EffectNode node,Cast.Hit hit,List<LivingEntity> helped,List<LivingEntity> harmed) {
        if(!Effects.builtIn(node.effect) || cast.passive)return false;
        switch(node.effect.path()) {
            case "springbed" -> springbed(cast,hit);
            case "cinder_sieve" -> sieve(cast,hit.point());
            case "ashen_mercy" -> helped.forEach(t -> mercy(cast,t));
            case "clockroot" -> harmed.forEach(t -> clockroot(cast,t));
            case "skylatch" -> helped.forEach(t -> skylatch(cast,t));
            case "thresherwind" -> thresh(cast,hit);
            default -> {return false;}
        }
        return true;
    }
    private static boolean loaded(Cast c,BlockPos p) {
        return c.level.isLoaded(p) && !c.level.isOutsideBuildHeight(p) && c.level.getWorldBorder().isWithinBounds(p);
    }
    private static boolean open(Cast c,Vec3 from,Vec3 to) {
        var delta=to.subtract(from);double length=delta.length();
        if(!Double.isFinite(length) || length>128)return false;
        int steps=Math.max(1,(int)Math.ceil(length*2));
        for(int i=0;i<=steps;i++)if(!loaded(c,BlockPos.containing(from.add(delta.scale(i/(double)steps)))))return false;
        return c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.caster)).getType()==HitResult.Type.MISS;
    }
    private static boolean editable(Cast c,BlockPos p) {
        return loaded(c,p) && !TemporaryBlocks.recorded(c.level,p) && Casters.mayEdit(c.caster,c.level,p);
    }
    private static void springbed(Cast c,Cast.Hit hit) {
        if(!c.once("springbed") || !Basinfill.fill(c,hit))return;
        BlockPos centre=hit.block().relative(hit.face());int grown=0;
        for(var raw:BlockPos.betweenClosed(centre.offset(-4,0,-4),centre.offset(4,1,4))) {
            if(grown>=FieldFusionRules.SPRING_PLANTS)break;
            var at=raw.immutable();if(!loaded(c,at))continue;
            var s=c.level.getBlockState(at);BlockState next=null;
            if(s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s))next=crop.getStateForAge(crop.getAge(s)+1);
            else if(s.is(WetlandGarden.REED) && s.getValue(MoonreedBlock.AGE)==0 && MoonreedBlock.moist(c.level,at))next=s.setValue(MoonreedBlock.AGE,1);
            if(next==null || !waterNear(c,at.below(),centre) || !editable(c,at) || !editable(c,at.below()) || !c.takeBlock())continue;
            c.level.setBlockAndUpdate(at,next);grown++;
            FieldFusionFx.rootlet(c.level,Vec3.atCenterOf(centre),Vec3.atCenterOf(at));
        }
        FieldFusionFx.spring(c.level,Vec3.atCenterOf(centre));
    }
    private static boolean waterNear(Cast c,BlockPos soil,BlockPos poured) {
        // The new basin's first source supplies this growth; unrelated existing pools cannot qualify a plant.
        return Math.abs(soil.getX()-poured.getX())<=4 && Math.abs(soil.getZ()-poured.getZ())<=4
            && poured.getY()>=soil.getY() && poured.getY()<=soil.getY()+1
            && loaded(c,poured) && c.level.getBlockState(poured).is(Blocks.WATER) && c.level.getFluidState(poured).isSource();
    }
    private record Cook(ItemEntity entity,int inputs,ItemStack result) {}
    private static void sieve(Cast c,Vec3 point) {
        if(!(c.caster instanceof ServerPlayer p) || !Casters.mayBuild(p) || !c.alive())return;
        var inventory=p.getInventory();int fuel=-1;
        var simulated=new ArrayList<ItemStack>();
        for(int i=0;i<36;i++) {var stack=inventory.getItem(i);simulated.add(stack.copy());if(fuel<0 && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)))fuel=i;}
        if(fuel<0)return;
        simulated.get(fuel).shrink(1);var jobs=new ArrayList<Cook>();int used=0;
        var drops=c.level.getEntitiesOfClass(ItemEntity.class,new AABB(point,point).inflate(FieldFusionRules.SIEVE_REACH),e -> e.isAlive() && !e.hasPickUpDelay())
            .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(point))).limit(24).toList();
        for(var item:drops) {
            if(used>=FieldFusionRules.SIEVE_ITEMS)break;
            var ownership=(ItemEntityAccessor)item;
            if(ownership.wildercord$thrower()!=null && !ownership.wildercord$thrower().getUUID().equals(p.getUUID()))continue;
            if(ownership.wildercord$target()!=null && !ownership.wildercord$target().equals(p.getUUID()))continue;
            if(item.distanceToSqr(point)>FieldFusionRules.SIEVE_REACH*FieldFusionRules.SIEVE_REACH
                || !editable(c,item.blockPosition().below()) || !open(c,p.getEyePosition(),item.position().add(0,.1,0)))continue;
            var input=new SingleRecipeInput(item.getItem());var recipe=c.level.recipeAccess().getRecipeFor(RecipeType.SMELTING,input,c.level);
            if(recipe.isEmpty())continue;var result=recipe.get().value().assemble(input);
            if(result.isEmpty())continue;
            int n=FieldFusionRules.sieveUnits(item.getItem().getCount(),result.getCount(),capacity(simulated,result),FieldFusionRules.SIEVE_ITEMS-used);
            if(n==0)continue;result.setCount(result.getCount()*n);insert(simulated,result.copy());jobs.add(new Cook(item,n,result));used+=n;
        }
        if(jobs.isEmpty() || !c.once("cinder_sieve"))return;
        // This transaction invokes no callbacks. The full simulation fits before either inputs or fuel change.
        for(int i=0;i<36;i++)inventory.setItem(i,simulated.get(i));inventory.setChanged();
        for(var job:jobs) {
            var rest=job.entity().getItem().copy();rest.shrink(job.inputs());
            if(rest.isEmpty())job.entity().discard();else job.entity().setItem(rest);
            FieldFusionFx.sieve(c.level,job.entity().position(),p.position().add(0,.8,0));
        }
    }
    static int capacity(List<ItemStack> slots,ItemStack output) {
        int space=0;for(var stack:slots)if(stack.isEmpty())space+=output.getMaxStackSize();
        else if(ItemStack.isSameItemSameComponents(stack,output))space+=Math.max(0,stack.getMaxStackSize()-stack.getCount());return space;
    }
    static void insert(List<ItemStack> slots,ItemStack output) {
        for(var stack:slots)if(!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,output)) {
            int n=Math.min(output.getCount(),Math.max(0,stack.getMaxStackSize()-stack.getCount()));stack.grow(n);output.shrink(n);
        }
        for(int i=0;i<slots.size() && !output.isEmpty();i++)if(slots.get(i).isEmpty()) {
            int n=Math.min(output.getCount(),output.getMaxStackSize());slots.set(i,output.copyWithCount(n));output.shrink(n);
        }
        if(!output.isEmpty())throw new IllegalStateException("Sieve preflight capacity disagrees with insertion");
    }
    private static void mercy(Cast c,LivingEntity t) {
        if(!c.alive() || !t.isAlive() || t.level()!=c.level || !Targets.canHelp(c.caster,t) || !paid(c).target("ashen_mercy",t.getUUID()))return;
        var observedMercy=LifeOwnerEvents.before(t);int removed=0;boolean fire=t.isOnFire();
        for(var effect:new ArrayList<>(t.getActiveEffects()))if(effect.getEffect().value().getCategory()==MobEffectCategory.HARMFUL && t.removeEffect(effect.getEffect()))removed++;
        t.clearFire();for(var mark:Reactions.Mark.values())Reactions.clear(t,mark);
        t.heal(FieldFusionRules.mercyHealing(removed,fire,t.getMaxHealth()-t.getHealth()));
        t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100,0,false,true));
        LifeOwnerEvents.changed(c,"ashen_mercy",t,observedMercy,null,LifeOwnerEvents.Moment.APPLY);
        FieldFusionFx.mercy(c.level,t);
    }
    private static boolean mobile(Cast c,LivingEntity t,boolean hostile) {
        return c.alive() && t.isAlive() && !t.isRemoved() && t.level()==c.level && loaded(c,t.blockPosition()) && !t.isPassenger() && !Spirits.isBoss(t)
            && (!hostile || !t.isPermanentlyInvulnerable() && !(t instanceof dev.wildercord.pet.CinnamonDog))
            && !VoidTime.anchored(t) && !(t instanceof Player p && (p.isCreative() || p.isSpectator()))
            && !DungeonWards.warded(c.level,t.blockPosition()) && (hostile?Targets.canHarm(c.caster,t):Targets.canHelp(c.caster,t));
    }
    private static boolean safeBody(Cast c,LivingEntity t,Vec3 at,boolean ground) {
        if(t.getBbWidth()>2 || t.getBbHeight()>3)return false;
        var box=t.getBoundingBox().move(at.subtract(t.position()));
        for(var q:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(!loaded(c,q))return false;
        if(!c.level.noCollision(t,box) || c.level.containsAnyLiquid(box))return false;
        var floor=BlockPos.containing(at).below();if(!loaded(c,floor))return false;
        if(ground) {
            var s=c.level.getBlockState(floor);
            if(!s.isFaceSturdy(c.level,floor,Direction.UP) || !editable(c,floor) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.CACTUS))return false;
        }
        return true;
    }
    private static void clockroot(Cast c,LivingEntity t) {
        if(CLOCKS.size()>=MAX_MARKS || !mobile(c,t,true) || !t.onGround() || !safeBody(c,t,t.position(),true)
            || Statuses.claimed(t,"clockroot",160) || !paid(c).target("clockroot",t.getUUID()))return;
        Statuses.claim(t,"clockroot",160);var mark=new Mark(c,t,t.position(),c.level.getGameTime()+FieldFusionRules.CLOCK_TICKS,new Object());
        CLOCKS.put(t.getUUID(),mark);FieldFusionFx.imprint(c.level,mark.origin());clockTick(mark);
    }
    private static void clockTick(Mark m) {
        var c=m.cast();var t=m.target();if(CLOCKS.get(t.getUUID())!=m)return;
        if(!mobile(c,t,true) || c.level.getGameTime()>=m.until()) {CLOCKS.remove(t.getUUID());return;}
        var delta=t.position().subtract(m.origin());
        if(FieldFusionRules.escaped(delta.x,delta.z)) {
            CLOCKS.remove(t.getUUID());
            if(delta.lengthSqr()>64 || !safeBody(c,t,m.origin(),true)
                || !open(c,t.getEyePosition(),m.origin().add(0,t.getEyeHeight(),0)))return;
            FieldFusionFx.clockReturn(c.level,t.position(),m.origin());
            t.teleportTo(c.level,m.origin().x,m.origin().y,m.origin().z,Set.<Relative>of(),t.getYRot(),t.getXRot(),false);
            t.resetFallDistance();return;
        }
        if(c.level.getGameTime()%10==0)FieldFusionFx.imprint(c.level,m.origin());
        Scheduler.later(2,Effects.carryContext(() -> clockTick(m)));
    }
    private static void skylatch(Cast c,LivingEntity t) {
        if(SKIES.size()>=MAX_MARKS || !mobile(c,t,false) || t.isShiftKeyDown() || t.hasEffect(MobEffects.LEVITATION) || Statuses.claimed(t,"skylatch",160)
            || t instanceof ServerPlayer p && p.getAbilities().mayfly)return;
        Vec3 up=t.position().add(0,1.1,0);
        if(!safeBody(c,t,up,false) || !open(c,t.getEyePosition(),up.add(0,t.getEyeHeight(),0)) || !paid(c).target("skylatch",t.getUUID()))return;
        Statuses.claim(t,"skylatch",160);var m=new Mark(c,t,up,c.level.getGameTime()+FieldFusionRules.SKY_TICKS,new Object());SKIES.put(t.getUUID(),m);
        FieldFusionFx.lift(c.level,t);skyTick(m);
    }
    private static void skyTick(Mark m) {
        var c=m.cast();var t=m.target();if(SKIES.get(t.getUUID())!=m)return;
        var levitation=t.getEffect(MobEffects.LEVITATION);
        boolean externalLift=levitation!=null && (levitation.getAmplifier()>0 || levitation.getDuration()>3);
        if(!mobile(c,t,false) || t.isShiftKeyDown() || c.level.getGameTime()>=m.until()
            || externalLift || !safeBody(c,t,t.position().add(0,.15,0),false)) {
            SKIES.remove(t.getUUID());
            if(t.isAlive() && t.level()==c.level) {t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,FieldFusionRules.SKY_DESCENT_TICKS,0,false,true));FieldFusionFx.unlatch(c.level,t);}
            return;
        }
        // A three-tick vanilla lift lease keeps server movement checks informed without granting flight abilities.
        // It expires naturally immediately after release; a stronger external effect is never removed.
        t.addEffect(new MobEffectInstance(MobEffects.LEVITATION,3,0,false,false));
        var velocity=t.getDeltaMovement();Effects.push(t,new Vec3(velocity.x,FieldFusionRules.hoverVelocity(t.getY(),m.origin().y),velocity.z));t.resetFallDistance();
        if(c.level.getGameTime()%10==0)FieldFusionFx.tether(c.level,t,m.origin().y);
        Scheduler.later(1,Effects.carryContext(() -> skyTick(m)));
    }
    private static void thresh(Cast c,Cast.Hit hit) {
        if(!(c.caster instanceof ServerPlayer p) || hit.block()==null || !Casters.mayBuild(p) || !c.once("thresherwind"))return;
        BlockPos centre=hit.block();if(!loaded(c,centre))return;if(!(c.level.getBlockState(centre).getBlock() instanceof CropBlock))centre=centre.above();
        Direction forward=Direction.fromYRot(p.getYRot()),right=forward.getClockWise();
        for(int row=0;row<3;row++) {int r=row;BlockPos line=centre.relative(forward,row);
            Scheduler.later(row*6,Effects.carryContext(() -> {
                if(!c.alive())return;FieldFusionFx.sickle(c.level,Vec3.atCenterOf(line),forward,r);
                for(int column=-1;column<=1;column++) {
                    var at=line.relative(right,column);if(!editable(c,at) || !editable(c,at.below()) || !open(c,p.getEyePosition(),Vec3.atCenterOf(at)))continue;
                    var state=c.level.getBlockState(at);
                    if(!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || !c.takeBlock())continue;
                    var drops=Block.getDrops(state,c.level,at,null,p,ItemStack.EMPTY);
                    var seed=state.getCloneItemStack(c.level,at,false);boolean replant=false;
                    for(var stack:drops)if(!replant && !seed.isEmpty() && ItemStack.isSameItem(stack,seed) && !stack.isEmpty()) {stack.shrink(1);replant=true;}
                    c.level.destroyBlock(at,false,p);if(replant)c.level.setBlockAndUpdate(at,crop.getStateForAge(0));
                    state.spawnAfterBreak(c.level,at,ItemStack.EMPTY,true);
                    for(var stack:drops)if(!stack.isEmpty()) {
                        var rest=stack.copy();p.getInventory().add(rest);
                        if(!rest.isEmpty()) {var loose=new ItemEntity(c.level,at.getX()+.5,at.getY()+.3,at.getZ()+.5,rest);loose.setTarget(p.getUUID());c.level.addFreshEntity(loose);}
                        FieldFusionFx.grain(c.level,Vec3.atCenterOf(at),p.position().add(0,.8,0));
                    }
                }
            }));
        }
    }
}
