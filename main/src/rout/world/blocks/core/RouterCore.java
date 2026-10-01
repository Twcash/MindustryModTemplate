package rout.world.blocks.core;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.Effect;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.BlockUnitUnit;
import mindustry.gen.BlockUnitc;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.StaticProp;
import mindustry.world.blocks.storage.CoreBlock;
import rout.content.RoutBlocks;
import rout.gen.routSounds;
import rout.world.blocks.environment.RouterFloor;
import rout.world.draw.RoutFx;

import static mindustry.Vars.*;

public class RouterCore extends CoreBlock {
    public int spreadRange = 8;
    public float spreadShake = 2f;
    public float spreadDuration = 15f;
    public float spreadInterval = 10f;
    public RouterFloor floor;
    /** Floors the spread will never convert and never pass through (acts like a
     *  wall for the wave). Add to this per-core in RoutBlocks to protect terrain. */
    public ObjectSet<Floor> blacklistedFloors = new ObjectSet<>();
    public RouterCore(String name) {
        super(name);
        replaceable = false;
    }

    /** True if the spread can convert this floor (not liquid, not blacklisted). */
    public boolean canSpreadOn(Floor f){
        return f != null && !f.isLiquid && !blacklistedFloors.contains(f);
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        if(Vars.state.isEditor() || Vars.state.rules.coreBuildAndConfig) return true;

        CoreBuild core = team.core();
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
        if(tile!=null &&tile.build!=null){
            //safeguard against accidental suicides.
            if(state.teams.cores(tile.build.team).size <2) return false;
        }
        return true;
    }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);

            //match Building.drawx()/drawy() exactly so the circle aligns with the block
            float cx = x * tilesize + offset;
            float cy = y * tilesize + offset;
            float radius = spreadRange * tilesize;

        arc.graphics.Color color = valid ? Pal.accent : Pal.remove;

        //Translucent fill so the player can eyeball which tiles will be hit.
        Draw.color(color, 0.05f);
        Fill.circle(cx, cy, radius);

        Draw.color(color);
        Lines.stroke(2f);
        Lines.circle(cx, cy, radius);
        Draw.reset();
    }

    public class RouterCoreBuild extends CoreBuild {

        private final Queue<Tile> spreadFrontier = new Queue<>();
        private final ObjectSet<Tile> spreadVisited = new ObjectSet<>();
        private int spreadTiles;
        private float spreadTimer;
        private boolean spreadStarted;

        public int spreadsPerInterval = 4;
        public float spreadChance = 0.85f;

        @Override
        public void placed(){
            super.placed();
            spreadStarted = true;
            spreadTimer = 0f;
            seedFrontier();
        }

        @Override
        public void onRemoved(){
            //MUST call super: CoreBuild.onRemoved() does team.data().removeCore(this).
            //Without it the core stays registered after destruction, so
            //state.teams.playerCores() never empties and Logic.checkGameState() never
            //fires game over — the sector can't be lost.
            super.onRemoved();
            spreadStarted = false;
            spreadFrontier.clear();
            spreadVisited.clear();
            spreadTiles = 0;
        }

        private void seedFrontier(){
            spreadFrontier.clear();
            spreadVisited.clear();
            int ax = tile.x + size / 2;
            int ay = tile.y + size / 2;
            //Distance is measured from the building's TRUE centre (x/y) to each tile's
            //centre (worldx/worldy), so the spread circle is centred on the block no
            //matter the size — even sizes sit between tiles.
            float capSq = (float)spreadRange * spreadRange * tilesize * tilesize;

            for(int dx = -size; dx <= size; dx++){
                for(int dy = -size; dy <= size; dy++){
                    Tile t = world.tile(ax + dx, ay + dy);
                    if(t == null || !canSpreadOn(t.floor())) continue;
                    float ndx = t.worldx() - x, ndy = t.worldy() - y;
                    if(ndx * ndx + ndy * ndy > capSq) continue;
                    if(spreadVisited.add(t)){
                        spreadFrontier.addLast(t);
                    }
                }
            }
        }

        /** Rebuild the frontier from the current world state after a load. The
         *  frontier isn't persisted, but every routerFloor/richRouterFloor tile IS,
         *  so we can recover the wave by finding every unspread tile in range that
         *  touches an already-spread tile. That way the wave resumes at the leading
         *  edge instead of restarting at the core and walking through every spread
         *  tile to get back where it was. */
        private void resumeFrontierFromWorld(){
            spreadFrontier.clear();
            spreadVisited.clear();
            int ax = tile.x + size / 2;
            int ay = tile.y + size / 2;
            float capSq = (float)spreadRange * spreadRange * tilesize * tilesize;

            //seed every non-liquid, non-blacklisted tile in range — already-spread tiles act
            //as pass-through so the wave can cross them to reach unconverted tiles.
            for(int dx = -spreadRange; dx <= spreadRange; dx++){
                for(int dy = -spreadRange; dy <= spreadRange; dy++){
                    Tile t = world.tile(ax + dx, ay + dy);
                    if(t == null || !canSpreadOn(t.floor())) continue;
                    float ndx = t.worldx() - x, ndy = t.worldy() - y;
                    if(ndx * ndx + ndy * ndy > capSq) continue;
                    if(spreadVisited.add(t)){
                        spreadFrontier.addLast(t);
                    }
                }
            }
        }

        /** True if at least one 8-neighbour of tile is in any RouterFloor form. */
        private boolean isAdjacentToSpreadTile(Tile tile){
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    if(dx == 0 && dy == 0) continue;
                    Tile n = world.tile(tile.x + dx, tile.y + dy);
                    if(n == null) continue;
                    if(n.floor() == floor) return true;
                    if(n.floor() == RoutBlocks.richRouterFloor.asFloor()) return true;
                }
            }
            return false;
        }

        @Override
        public void updateTile(){
            
            if(spreadStarted && floor != null){
                if(spreadFrontier.size == 0 && spreadTiles == 0){
                    seedFrontier();
                    if(spreadFrontier.size == 0) return;
                }
                spreadTimer += Time.delta;
                if(spreadTimer >= spreadInterval){
                    spreadTimer -= spreadInterval;
                    int ax = tile.x + size / 2;
                    int ay = tile.y + size / 2;
                    float capSq = (float)spreadRange * spreadRange * tilesize * tilesize;

                    for(int attempt = 0; attempt < spreadsPerInterval; attempt++){
                        if(spreadFrontier.size == 0) break;

                        Tile t = spreadFrontier.removeFirst();
                        if(t == null) continue;

                        float ddx = t.worldx() - x, ddy = t.worldy() - y;
                        boolean inRange = ddx * ddx + ddy * ddy <= capSq;

                        if(inRange && t.floor() != floor && t.floor() != RoutBlocks.richRouterFloor.asFloor() && !blacklistedFloors.contains(t.floor())){
                            //random skip pushes the tile to the back of the queue for a
                            //later pass, creating an organic irregular spread edge.
                            if(Mathf.chance(1f - spreadChance)){
                                spreadFrontier.addLast(t);
                                continue;
                            }
                            if(t.block().unitMoveBreakable) ConstructBlock.deconstructFinish(t, t.block(), null);
                            if(t.overlay() == Blocks.oreCopper){
                                t.setOverlay(Blocks.air);
                                t.setFloor(RoutBlocks.richRouterFloor.asFloor());
                            } else if(t.overlay() == Blocks.oreLead){
                                t.setOverlay(Blocks.air);
                                t.setFloor(RoutBlocks.dormantDistributiveSubstrate.asFloor());
                            }else{
                                t.setOverlay(Blocks.air);
                                t.setFloor(floor.asFloor());
                            }
                            Effect.shake(spreadShake, spreadDuration, t);
                            routSounds.RouterSpread.at(t.worldx(), t.worldy(), Mathf.random(0.8f, 1.1f), Mathf.random(0.8f, 1.1f));
                            RoutFx.routerSpread.at(t.worldx(), t.worldy());
                            spreadTiles++;
                        }

                        //expand to cardinal neighbours only — 8-connectivity creates
                        //square wavefronts; 4-connectivity with random chance gives
                        //organic branching patterns.
                        for(int d = 0; d < 4; d++){
                            Tile n = world.tile(t.x + Geometry.d4[d].x, t.y + Geometry.d4[d].y);
                            if(n == null || !canSpreadOn(n.floor())) continue;
                            float ndx = n.worldx() - x, ndy = n.worldy() - y;
                            if(ndx * ndx + ndy * ndy > capSq) continue;
                            if(spreadVisited.add(n)){
                                spreadFrontier.addLast(n);
                            }
                        }
                    }
                }
            }
        }
        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(spreadTimer);
            write.bool(spreadStarted);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            spreadTimer = read.f();
            if(revision >= 1){
                spreadStarted = read.bool();
            }else{
                //Old save (no spreadStarted bool) - derive it so cores resume after reload.
                spreadStarted = floor != null;
            }
            //The frontier isn't persisted. Rebuild it from the world state so the wave
            //resumes at the actual boundary (spread tile <-> unspread tile) instead of
            //restarting at the core and having to walk back through every already-spread
            //tile. This is what fixes "core stops spreading after reload".
            if(spreadFrontier.size == 0) resumeFrontierFromWorld();
            spreadVisited.clear();
        }

        @Override
        public void onDeconstructed(@Nullable Unit builder){
            super.onDeconstructed(builder);
            state.teams.unregisterCore(this);
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
        }
    }
}
