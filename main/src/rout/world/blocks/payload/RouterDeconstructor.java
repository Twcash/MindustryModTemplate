package rout.world.blocks.payload;

import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Puddles;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadDeconstructor;
import mindustry.world.blocks.storage.CoreBlock;
import rout.world.blocks.environment.RouterFloor;

import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;

/** A payload deconstructor that breaks blocks into configurable items per recipe. */
public class RouterDeconstructor extends PayloadDeconstructor {
    /** Per-block override: when this block is deconstructed, these items are produced
     *  instead of its normal requirements. Amounts are scaled by buildCostMultiplier. */
    public ObjectMap<Block, ItemStack[]> deconstructionResults = new ObjectMap<>();

    public RouterDeconstructor(String name) {
        super(name);
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        if (tile == null) return false;
        //Only change here is letting the core be placed on any router floor
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        super.canPlaceOn(tile, team, rotation);
        return false;
    }
    public class RouterDeconstructorBuild extends PayloadDeconstructorBuild {
        private ItemStack[] currentReqs;
        private Payload customReqsFor;

        private ItemStack[] resolveReqs(Payload payload){
            if(payload == customReqsFor && currentReqs != null) return currentReqs;
            if(payload instanceof BuildPayload bp && deconstructionResults.containsKey(bp.build.block)){
                ItemStack[] base = deconstructionResults.get(bp.build.block);
                ItemStack[] scaled = new ItemStack[base.length];
                for(int i = 0; i < base.length; i++){
                    scaled[i] = new ItemStack(base[i].item, Mathf.ceil(base[i].amount * Vars.state.rules.buildCostMultiplier));
                }
                customReqsFor = payload;
                currentReqs = scaled;
                return scaled;
            }
            customReqsFor = payload;
            currentReqs = null;
            return null;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            //Capacity check: if we have custom reqs, swap them in just for the check.
            ItemStack[] custom = resolveReqs(payload);
            if(custom == null) return super.acceptPayload(source, payload);

            // Temporarily wrap the payload so vanilla's acceptPayload sees the custom
            // reqs. We never let the wrapped payload escape - handlePayload restores
            // the original reference.
            Payload wrapped = new RoutedPayload(payload, custom);
            return super.acceptPayload(source, wrapped);
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            super.handlePayload(source, payload);
            //Cache custom reqs and reset accum to match the new length if needed.
            ItemStack[] custom = resolveReqs(deconstructing);
            if(custom != null && (accum == null || accum.length != custom.length)){
                accum = new float[custom.length];
            }
        }

        @Override
        public void updateTile(){
            //PayloadBlock.updateTile handles payload.update() + isDead check. Without
            //this call, the incoming payload at the edge never moves in.
            super.updateTile();

            //Standard dump + payload rotation (mirrors vanilla).
            if(items.total() > 0){
                for(int i = 0; i < dumpRate; i++){
                    dumpAccumulate();
                }
            }

            if(deconstructing == null){
                progress = 0f;
                currentReqs = null;
                customReqsFor = null;
            }

            payRotation = Angles.moveToward(payRotation, 90f, payloadRotateSpeed * edelta());

            if(deconstructing != null){
                //Prefer the cached custom reqs; fall back to the payload's vanilla reqs.
                ItemStack[] reqs = currentReqs != null ? currentReqs : deconstructing.requirements();

                if(accum == null || accum.length != reqs.length){
                    accum = new float[reqs.length];
                }

                boolean canProgress = true;
                for(ItemStack req : reqs){
                    if(items.get(req.item) >= itemCapacity){
                        canProgress = false;
                        break;
                    }
                }

                if(canProgress){
                    float shift = edelta() * deconstructSpeed / deconstructing.buildTime();
                    float realShift = Math.min(shift, 1f - progress);

                    if(progress == 0f && shift > 0f && deconstructing instanceof BuildPayload pay){
                        var build = pay.build;
                        if(build.liquids != null && build.liquids.currentAmount() > 0){
                            float perCell = build.liquids.currentAmount() / (block.size * block.size) * 2f;
                            tile.getLinkedTiles(other -> Puddles.deposit(other, build.liquids.current(), perCell));
                        }
                    }

                    progress += shift;
                    time += edelta();

                    for(int i = 0; i < reqs.length; i++){
                        accum[i] += reqs[i].amount * (deconstructing instanceof BuildPayload ? state.rules.buildCostMultiplier : state.rules.unitCost(team)) * realShift;
                    }
                }

                speedScl = Mathf.lerpDelta(speedScl, canProgress ? 1f : 0f, 0.1f);

                for(int i = 0; i < reqs.length; i++){
                    int taken = Math.min((int)accum[i], itemCapacity - items.get(reqs[i].item));
                    if(taken > 0){
                        items.add(reqs[i].item, taken);
                        accum[i] -= taken;
                    }
                }

                if(progress >= 1f){
                    canProgress = true;
                    for(int i = 0; i < reqs.length; i++){
                        if(Mathf.equal(accum[i], 1f, 0.0001f)){
                            if(items.total() < itemCapacity){
                                items.add(reqs[i].item, 1);
                                accum[i] = 0f;
                            }else{
                                canProgress = false;
                                break;
                            }
                        }
                    }

                    if(canProgress){
                        Fx.breakBlock.at(x, y, deconstructing.size() / tilesize);

                        deconstructing = null;
                        accum = null;
                        currentReqs = null;
                        customReqsFor = null;
                    }
                }
            }else if(moveInPayload(false) && payload != null){
                //Pull the incoming payload off the edge into the deconstruction slot.
                //resolveReqs against the (unwrapped) payload and prime accum with the
                //custom length if the block is in deconstructionResults.
                resolveReqs(payload);
                if(currentReqs != null){
                    accum = new float[currentReqs.length];
                }else{
                    accum = new float[payload.requirements().length];
                }
                deconstructing = payload;
                payload = null;
                progress = 0f;
            }
        }
    }

    /** A transient payload wrapper that swaps in custom requirements. Never written
     *  to a save - it's only used to hand vanilla's acceptPayload the right list. */
    private static class RoutedPayload implements Payload{
        final Payload inner;
        final ItemStack[] reqs;

        RoutedPayload(Payload inner, ItemStack[] reqs){
            this.inner = inner;
            this.reqs = reqs;
        }

        @Override public ItemStack[] requirements(){ return reqs; }

        @Override public void set(float x, float y, float r){ inner.set(x, y, r); }
        @Override public void draw(){ inner.draw(); }
        @Override public void drawShadow(float a){ inner.drawShadow(a); }
        @Override public float size(){ return inner.size(); }
        @Override public float x(){ return inner.x(); }
        @Override public float y(){ return inner.y(); }
        @Override public float buildTime(){ return inner.buildTime(); }
        @Override public boolean contentEquals(Payload other){ return inner.contentEquals(other); }
        @Override public void write(Writes w){ inner.write(w); }
        @Override public arc.graphics.g2d.TextureRegion icon(){ return inner.icon(); }
        @Override public mindustry.ctype.UnlockableContent content(){ return inner.content(); }
        @Override public float getX(){ return inner.getX(); }
        @Override public float getY(){ return inner.getY(); }
        @Override public void update(mindustry.gen.Unit u, Building b){ inner.update(u, b); }
        @Override public boolean isDead(){ return inner.isDead(); }
        @Override public boolean dump(){ return inner.dump(); }
        @Override public boolean fits(float s){ return inner.fits(s); }
        @Override public float rotation(){ return inner.rotation(); }
        @Override public void destroyed(){ inner.destroyed(); }
        @Override public void remove(){ inner.remove(); }
    }
}
