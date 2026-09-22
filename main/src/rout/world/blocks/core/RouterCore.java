package rout.world.blocks.core;

import arc.struct.Queue;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.BlockUnitUnit;
import mindustry.gen.BlockUnitc;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.blocks.storage.CoreBlock;
import rout.world.blocks.environment.RouterFloor;

import static mindustry.Vars.*;

public class RouterCore extends CoreBlock {
    public int spreadRange = 9;
    public float spreadInterval = 30f;
    public RouterFloor floor;

    public RouterCore(String name) {
        super(name);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        if(Vars.state.isEditor() || Vars.state.rules.coreBuildAndConfig) return true;

        CoreBuild core = team.core();

        //Allow the core to be placed on RouterFloor (with no pre-existing core required),
        //in addition to vanilla allowCorePlacement zones (sand, spore-moss, etc.).
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }

        if(core == null || (!Vars.state.rules.infiniteResources && !core.items.has(requirements, Vars.state.rules.buildCostMultiplier)))
            return false;

        return tile.block() instanceof CoreBlock && size > tile.block().size && (!requiresCoreZone || tempTiles.allMatch(o -> o.floor().allowCorePlacement));
    }

    @Override
    public boolean canBreak(Tile tile){
        //Vanilla CoreBlock.canBreak gates this to false outside the editor, which makes
        //the core unbuildable / un-deconstructable in normal play (Build.validBreak uses
        //Block.canBreak as its primary gate). Returning true here lets the player
        //right-click-deconstruct and break normally.
        return true;
    }

    public class RouterCoreBuild extends CoreBuild implements ControlBlock {
        //Lazily created so the BlockUnitUnit never leaks to the engine's unit group
        //(its `tile` is bound to this building via unit() before anything tries to add() it).
        private BlockUnitc unit;

        /** Next ring to convert; 1 = immediately adjacent to the core, growing outward. */
        private int currentRing = 1;
        /** Time accumulator since the last ring was applied (seconds). */
        private float spreadTimer;
        /** Set true when the building is fully placed, so the spread starts only then. */
        private boolean spreadStarted;

        @Override
        public Unit unit(){
            if(unit == null){
                BlockUnitUnit u = BlockUnitUnit.create();
                u.team = team;
                u.type = ((CoreBlock)block).unitType;
                unit = (BlockUnitc)u;
            }
            unit.tile(this);
            unit.team(team);
            return (Unit)unit;
        }

        @Override
        public boolean canControl(){
            return true;
        }

        @Override
        public void placed(){
            super.placed();
            spreadStarted = true;
            spreadTimer = 0f;
        }

        @Override
        public void onRemoved(){
            //Reset queue state in case the build gets recycled / deserialized.
            currentRing = 1;
            spreadTimer = 0f;
            spreadStarted = false;
        }

        @Override
        public void updateTile(){
            Unit u = unit();
            if(u.activelyBuilding()){
                u.lookAt(angleTo(u.buildPlan()));
            }
            if(u.buildPlan() == null){
                Queue<Teams.BlockPlan> blocks = team.data().plans;
                for(int i = 0; i < blocks.size; i++){
                    var block = blocks.get(i);
                    if(within(block.x * tilesize, block.y * tilesize, 250)){
                        var btype = block.block;

                        if(Build.validPlace(btype, u.team(), block.x, block.y, block.rotation)
                            && (state.rules.infiniteResources || team.rules().infiniteResources
                                || team.items().has(btype.requirements, state.rules.buildCostMultiplier))){
                            u.addBuild(new BuildPlan(block.x, block.y, block.rotation, block.block, block.config));
                            //shift build plan to tail so next unit builds something else
                            blocks.addLast(blocks.removeIndex(i));
                            break;
                        }
                    }
                }
            }

            //Gradual outward spread: each spreadInterval seconds we apply one more ring
            //of RouterFloor tiles, expanding from the core outward to spreadRange.
            if(spreadStarted && floor != null && currentRing <= spreadRange){
                spreadTimer += Time.delta;
                if(spreadTimer >= spreadInterval){
                    spreadTimer -= spreadInterval;
                    applyRing(currentRing++);
                }
            }
        }

        private void applyRing(int r){
            //Tile-coordinate center of the (possibly multi-tile) core. (size-1)/2 = 0
            //for size 1, 1 for size 3, etc.; size 2 ends up biased half a tile, which is
            //visually indistinguishable for radius >= a few.
            int cx = tile.x + (size - 1) / 2;
            int cy = tile.y + (size - 1) / 2;

            for(int dx = -r; dx <= r; dx++){
                for(int dy = -r; dy <= r; dy++){
                    //Only the perimeter tiles of this Chebyshev ring, and only inside
                    //the radius circle.
                    if(Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    if(dx * dx + dy * dy > spreadRange * spreadRange) continue;

                    Tile t = world.tile(cx + dx, cy + dy);
                    if(t == null) continue;
                    if(t.floor().isLiquid) continue;
                    if(t.floor() == floor) continue;

                    //Skip tiles inside the core's own footprint (covered by the building).
                    int localX = t.x - tile.x;
                    int localY = t.y - tile.y;
                    if(localX >= 0 && localX < size && localY >= 0 && localY < size) continue;

                    t.setOverlay(Blocks.air);
                    t.setFloor(floor);
                }
            }
        }
        @Override
        public void onDeconstructed(@Nullable Unit builder){
            super.onDeconstructed(builder);
            state.teams.unregisterCore(this);
        }
    }
}
