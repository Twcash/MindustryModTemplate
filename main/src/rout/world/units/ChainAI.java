package rout.world.units;

import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.entities.Units;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.blocks.environment.Floor;
import rout.world.draw.RoutFx;

/**
 * Merges nearby roustalkers into a single longer chain: one head drives
 * the normal pet behavior, the rest trail behind at a fixed spacing.
 * Chains only form when more than {@link #minChainUnits} roustalkers
 * gather. Segments keep individual health, and a chain that gets cut in
 * half splits into two separate chains. The unit's weapon only fires
 * while it is part of a chain.
 */
public class ChainAI extends RouterJumpAI{
    /** Units merge into a chain when this close to one another. */
    public float chainRange = 10f * 8f;
    /** A free unit must be within this of a chain member to actually link up. */
    public float mergeProximity = 3f * 8f;
    /** Chains only form when at least this many roustalkers are gathered. */
    public int minChainUnits = 1;
    /** Range used to count roustalkers for the formation gate. */
    public float chainFormRange = 12f * 8f;
    /** Distance between chained segments. Kept just above the physics
     *  radius sum so chain members never overlap and push each other. */
    public float segmentSpacing = 10f;
    /** A link breaks once a segment strays further than this from its parent. */
    public float maxLinkDistance = 40f;
    /** How sharply the chain is allowed to bend. */
    public float segmentRotationRange = 110f;
    /** Whether the whole chain shares a single health pool. */
    public boolean combinedHealth = true;
    /** Cap on the shared pool: fraction of the highest member's max health. */
    public float combinedHealthCap = 0.75f;
    /** How long a freshly split chain avoids re-merging. */
    public float splitGrace = 180f;
    /** Unit the chain merges into once it gets long enough. Null disables merging. */
    public UnitType chainMergeType;
    /** Chains merge into {@link #chainMergeType} once they exceed this many segments. */
    public int mergeChainSize = 3;
    /** Chain types this unit can link with. Empty means only the same type. */
    public Seq<UnitType> chainables = new Seq<>();

    /** Chain links. Transient: rebuilt every tick when missing/broken. */
    protected Unit head, parent, child;
    /** Index of this segment in the chain (0 = head). */
    protected int segment;
    /** The angle this segment of the chain is pointing at. */
    protected float chainAngle;
    /** Throttles the merge scan. */
    protected float mergeTimer;
    /** Counts down after a split; prevents instant re-merge. */
    protected float splitCooldown;
    /** Counts down after a routecerate merge; keeps the result stable. */
    protected float mergeCooldown;

    @Override
    public void updateMovement(){
        if(splitCooldown > 0f) splitCooldown -= Time.delta;
        if(mergeCooldown > 0f) mergeCooldown -= Time.delta;

        //reconcile chain structure
        refreshChain();

        //the swarm only drowns when the whole chain is submerged
        updateDrownProtection();

        if(parent == null){
            //head: act like a normal roustalker
            chainAngle = unit.rotation;
            super.updateMovement();

            if(combinedHealth) updateCombinedHealth();
            tryMergeUnit();
        }else{
            //segment: just trail the chain
            followChain();
        }
    }

    /** The weapon only fires while this unit is part of a chain. */
    @Override
    public boolean shouldFire(){
        return chainSize() >= 2;
    }

    protected void refreshChain(){
        if(head == null) head = unit;

        //if the head was destroyed the chain breaks apart
        if(head != unit && (!head.isAdded() || head.dead())){
            //only segments need to split; units already re-linked as heads
            //(e.g. after a merge removed the old head) just fix the stale reference
            if(parent != null){
                split();
                return;
            }
            head = unit;
        }
        //a shot-out middle segment: the part behind it becomes its own chain
        if(parent != null && (!parent.isAdded() || parent.dead())){
            parent = null;
            head = unit;
            splitCooldown = splitGrace;
        }else if(parent != null && unit.dst(parent) > maxLinkDistance){
            //stretched too far from its parent: the link snaps and the
            //part behind this segment breaks off into its own chain
            if(parent.controller() instanceof ChainAI p && p.child == unit){
                p.child = null;
            }
            parent = null;
            head = unit;
            splitCooldown = splitGrace;
        }
        if(child != null && (!child.isAdded() || child.dead())){
            child = null;
        }
        //always keep a well-defined head: the root of the chain (no parent),
        //so the chain can always be directed as a unit
        if(parent == null){
            head = unit;
        }else if(parent.controller() instanceof ChainAI p && p.head != null && p.head.isAdded() && !p.head.dead()){
            head = p.head;
        }else{
            head = resolveHead();
        }

        //a free unit looks for a chain to merge into
        if(parent == null && head == unit){
            tryMerge();
        }
    }

    /** Walks up the parent links to the root of the chain. */
    protected Unit resolveHead(){
        ChainAI c = this;
        for(int i = 0; i < 512 && c.parent != null; i++){
            if(c.parent.controller() instanceof ChainAI p){
                c = p;
            }else{
                break;
            }
        }
        return c.unit;
    }

    protected void tryMerge(){
        mergeTimer -= Time.delta;
        if(mergeTimer > 0f || splitCooldown > 0f) return;
        mergeTimer = 15f;

        //count every chainable unit nearby, including this one
        int[] count = {0};
        Units.nearby(unit.team, unit.x, unit.y, chainFormRange, other -> {
            if(other == unit || canChainWith(other)) count[0]++;
        });
        if(count[0] <= minChainUnits) return;

        Unit[] best = {null};
        float[] bestDist = {chainRange};
        Units.nearby(unit.team, unit.x, unit.y, chainRange, other -> {
            if(canChainWith(other)){
                ChainAI ai = (ChainAI)other.controller();
                if(ai.head != null && ai.head != unit){
                    float d = unit.dst(other);
                    if(d < bestDist[0]){
                        bestDist[0] = d;
                        best[0] = ai.head;
                    }
                }
            }
        });
        if(best[0] != null && bestDist[0] <= mergeProximity){
            connectTo((ChainAI)best[0].controller());
        }
    }

    /** Whether this unit can chain with the given unit (same team, allowed type). */
    protected boolean canChainWith(Unit other){
        if(other == null || other == unit || !other.isAdded() || other.dead() || other.team != unit.team) return false;
        if(!(other.controller() instanceof ChainAI)) return false;
        return chainables.isEmpty() ? other.type == unit.type : chainables.contains(other.type);
    }

    protected void connectTo(ChainAI targetAi){
        if(targetAi == this) return;

        //resolve the true head by walking up the parent links: the found
        //unit may be a stale head from an in-flight merge
        ChainAI trueHead = targetAi;
        for(int i = 0; i < 512 && trueHead.parent != null; i++){
            if(trueHead.parent.controller() instanceof ChainAI p){
                trueHead = p;
            }else{
                break;
            }
        }

        //never attach to a chain that already contains us: prevents cycles
        if(chainContains(trueHead, this)) return;

        //join next to the physically closest chain unit, so a unit near the
        //head doesn't have to crawl the whole length to reach the tail
        ChainAI closest = trueHead;
        float bestDist = Float.MAX_VALUE;
        for(ChainAI c = trueHead; c != null; c = next(c)){
            float d = unit.dst(c.unit);
            if(d < bestDist){
                bestDist = d;
                closest = c;
            }
        }

        //insert this unit right after the closest unit
        Unit oldChild = closest.child;
        closest.child = unit;
        parent = closest.unit;
        child = oldChild;
        if(oldChild != null && oldChild.controller() instanceof ChainAI oc){
            oc.parent = unit;
        }
        head = trueHead.unit;
        segment = closest.segment + 1;

        //propagate the true head and segment indices down the whole chain
        int seg = 0;
        for(ChainAI c = trueHead; c != null && seg < 512; c = next(c)){
            c.head = trueHead.unit;
            c.segment = seg++;
        }

        RoutFx.routerMerge.at(unit.x, unit.y);
    }

    protected ChainAI next(ChainAI c){
        if(c.child == null || !c.child.isAdded()) return null;
        return c.child.controller() instanceof ChainAI cc ? cc : null;
    }

    /** Whether {@code target} appears anywhere in the chain headed by {@code head}. */
    protected boolean chainContains(ChainAI head, ChainAI target){
        ChainAI c = head;
        for(int i = 0; i < 512 && c != null; i++){
            if(c == target) return true;
            c = next(c);
        }
        return false;
    }

    /** Collapses {@link #mergeChainSize} adjacent segments of a type that has a
 *  {@link #chainMergeType} into that unit, wherever the run sits in the
 *  chain. The rest of the chain is left intact. */
protected void tryMergeUnit(){
        if(mergeCooldown > 0f) return;

        Seq<Unit> chain = chainUnits();

        //find the first contiguous same-type run whose type can merge onward
        Seq<Unit> toMerge = new Seq<>();
        UnitType runType = null;
        UnitType mergeInto = null;
        for(Unit u : chain){
            if(u.dead()){
                runType = null;
                mergeInto = null;
                toMerge.clear();
                continue;
            }
            if(u.type != runType){
                runType = u.type;
                toMerge.clear();
                mergeInto = (u.controller() instanceof ChainAI cu && cu.chainMergeType != null) ? cu.chainMergeType : null;
            }
            if(mergeInto != null) toMerge.add(u);
            if(toMerge.size >= mergeChainSize) break;
        }
        if(toMerge.size < mergeChainSize || mergeInto == null) return;

        float health = 0f, maxHealth = 0f;
        for(Unit u : toMerge){
            if(!u.dead()){
                health += u.health();
                maxHealth += u.maxHealth();
            }
        }

        Unit merged = mergeInto.spawn(unit.team, toMerge.first().x, toMerge.first().y);
        merged.rotation(toMerge.first().rotation);
        merged.health(Math.min(health, merged.maxHealth()));

        RoutFx.routerMerge.at(toMerge.first().x, toMerge.first().y);

        //remove only the nabbed units, re-linking the chain across the gaps so
        //it stays intact instead of fragmenting into split-off heads
        for(Unit u : toMerge){
            if(u != merged && !u.dead()){
                removeFromChain(u);
                u.remove();
            }
        }

        //promote the merged unit to the head of whatever chain remains
        if(merged.controller() instanceof ChainAI mc){
            mc.mergeCooldown = mergeChainSize * 10f;
            for(Unit u : chain){
                if(u != merged && u.isAdded() && !u.dead() && u.controller() instanceof ChainAI cu && cu.parent == null){
                    mc.child = u;
                    cu.parent = merged;
                    cu.head = merged;
                    cu.mergeCooldown = mergeChainSize * 10f;
                    int seg = 1;
                    for(ChainAI c = cu; c != null && seg < 512; c = next(c)){
                        c.head = merged;
                        c.segment = seg++;
                        c.mergeCooldown = mergeChainSize * 10f;
                    }
                    break;
                }
            }
        }
    }

    /** The swarm only drowns when every unit in the chain is submerged. */
    protected void updateDrownProtection(){
        //only units that are actively drowning need protecting
        if(head == null || !inDeepWater(unit)) return;
        if(!(head.controller() instanceof ChainAI hc)) return;

        for(ChainAI c = hc; c != null; c = next(c)){
            if(!inDeepWater(c.unit)){
                unit.drownTime(0f);
                return;
            }
        }
    }

    /** Whether this unit is sitting on a floor that would drown it. */
    protected boolean inDeepWater(Unit u){
        if(!u.isGrounded() || !u.type.canDrown) return false;
        Floor floor = u.floorOn();
        return floor != null && floor.isLiquid && floor.drownTime > 0f;
    }

    /** Unlinks a unit from the chain and connects its neighbours to each other. */
    protected void removeFromChain(Unit u){
        if(!(u.controller() instanceof ChainAI c)) return;

        if(c.parent != null && c.parent.controller() instanceof ChainAI p){
            p.child = c.child;
        }
        if(c.child != null && c.child.controller() instanceof ChainAI ch){
            ch.parent = c.parent;
        }
        c.parent = null;
        c.child = null;
        c.head = c.unit;
    }

    protected void followChain(){
        //segments settle to the ground; never left hovering
        if(unit.elevation() > 0f){
            unit.elevation(Mathf.approachDelta(unit.elevation(), 0f, 0.3f));
        }
        if(parent == null || !parent.isAdded() || parent.dead()) return;
        if(!(parent.controller() instanceof ChainAI p)) return;

        UnitType st = unit.type;
        //angle toward the point we should stand at, clamped to the chain's bend range
        float a = Angles.moveToward(unit.rotation - 180f, Angles.clampRange(parent.angleTo(unit), p.chainAngle + 180f, segmentRotationRange), st.rotateSpeed * Time.delta);

        //move toward parent + trns(a, spacing), snapping into place quickly
        Tmp.v1.trns(a, segmentSpacing).add(parent.x, parent.y).sub(unit.x, unit.y);
        if(Tmp.v1.len() > 0.01f){
            Tmp.v1.setLength(Math.min(Tmp.v1.len(), prefSpeed() * 4f * Time.delta));
            unit.move(Tmp.v1.x, Tmp.v1.y);
        }

        unit.rotation(Angles.moveToward(unit.rotation, a + 180f, st.rotateSpeed * Time.delta));
        chainAngle = a + 180f;
        segment = p.segment + 1;
    }

    protected void updateCombinedHealth(){
        Seq<Unit> chain = chainUnits();
        float totalMax = 0f, totalCur = 0f, highestMax = 0f;
        for(Unit u : chain){
            if(!u.dead()){
                totalMax += u.maxHealth();
                totalCur += u.health();
                highestMax = Math.max(highestMax, u.maxHealth());
            }
        }
        if(totalMax <= 0f || highestMax <= 0f) return;

        //the shared pool is capped at 75% of the highest member's max health
        float poolMax = Math.min(totalMax, highestMax * combinedHealthCap);
        if(poolMax <= 0f) return;
        float poolCur = Math.min(totalCur, poolMax);

        //enough damage to cover a member's health removes it from the tail
        float damage = Math.max(0f, poolMax - poolCur);
        float covered = 0f;
        for(int i = chain.size - 1; i >= 1; i--){
            Unit u = chain.get(i);
            if(!u.dead()){
                covered += u.maxHealth();
                if(damage >= covered){
                    u.kill();
                    return;
                }
            }
        }

        //otherwise share the pool proportionally
        float ratio = Math.min(1f, poolCur / poolMax);
        for(Unit u : chain){
            if(!u.dead()) u.health(ratio * u.maxHealth());
        }
    }

    protected void split(){
        if(parent != null && parent.controller() instanceof ChainAI p && p.child == unit){
            p.child = null;
        }
        parent = null;
        head = unit;
        splitCooldown = splitGrace;

        //children break off and find their own chains
        if(child != null && child.controller() instanceof ChainAI c){
            c.parent = null;
            c.head = c.unit;
            c.splitCooldown = splitGrace;
            child = null;
        }
        segment = 0;
    }

    protected int chainSize(){
        int size = 0;
        ChainAI c = this;
        for(int i = 0; i < 512; i++){
            size++;
            if(c.child == null || !c.child.isAdded()) break;
            if(c.child.controller() instanceof ChainAI next){
                c = next;
            }else{
                break;
            }
        }
        return size;
    }

    protected Seq<Unit> chainUnits(){
        Seq<Unit> res = new Seq<>();
        ChainAI c = this;
        for(int i = 0; i < 512; i++){
            res.add(c.unit);
            if(c.child == null || !c.child.isAdded()) break;
            if(c.child.controller() instanceof ChainAI next){
                c = next;
            }else{
                break;
            }
        }
        return res;
    }
}