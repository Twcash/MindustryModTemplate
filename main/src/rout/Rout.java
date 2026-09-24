package rout;

import arc.*;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.world.Tile;
import rout.content.*;
import mindustry.ctype.*;
import mindustry.mod.*;
import rout.annotations.Annotations.*;
import rout.gen.*;
import rout.world.draw.RoutFx;

import static mindustry.Vars.*;

/**
 * The mod's main mod class. Contains static references to other modules.
 * @author Avant Team
 */
@LoadRegs("error")// Need this temporarily, so the class gets generated.
@EnsureLoad
public class Rout extends Mod{
    public static boolean tools = false;

    /** Seconds an orphaned router/rich-router floor must be uncovered before reverting. */
    public static float orphanFloorTime = 90f;
    /** Seconds an orphaned RouterBlock must sit on non-router floor before dying. */
    public static float orphanBlockTime = 15f;

    public Rout(){
        this(false);
    }

    public Rout(boolean tools){

        Rout.tools = tools;

        if(!headless){
            Events.on(FileTreeInitEvent.class, e -> Core.app.post(routSounds::load));
        }


        Events.on(ContentInitEvent.class, e -> {

            if(!headless){
                Regions.load();
                content.each(content -> {
                    if(isTemplate(content) && content instanceof MappableContent mContent){
                        routContentRegionRegistry.load(mContent);
                    }
                });
            }
        });
    }

    @Override
    public void init(){
        //Per-tile tracking of how long they've been orphaned. Cleared when the tile
        //is no longer orphaned (or after the revert/die action fires).
        ObjectMap<Tile, Float> orphanFloorSince = new ObjectMap<>();
        ObjectMap<Tile, Float> orphanBlockSince = new ObjectMap<>();

        //Per-tick cache of which tiles are covered by some RouterCore's spreadRange.
        //Re-built each tick so newly-placed / destroyed cores are reflected.
        ObjectSet<Tile> covered = new ObjectSet<>();

        Events.run(Trigger.update, () -> {
            if(state == null || state.isMenu()) return;
            if(world == null) return;

            //1) Rebuild the covered-tile set from a direct world scan. This is more
            //   reliable than state.teams.cores(team), which can be empty mid-load.
            covered.clear();
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile ct = world.tile(x, y);
                    if(ct.build != null && ct.block() instanceof rout.world.blocks.core.RouterCore){
                        rout.world.blocks.core.RouterCore rc = (rout.world.blocks.core.RouterCore)ct.block();
                        int cx = ct.x + (rc.size - 1) / 2;
                        int cy = ct.y + (rc.size - 1) / 2;
                        int spread = rc.spreadRange;
                        int capSq = spread * spread;
                        for(int dx = -spread; dx <= spread; dx++){
                            for(int dy = -spread; dy <= spread; dy++){
                                if(dx * dx + dy * dy > capSq) continue;
                                Tile t = world.tile(cx + dx, cy + dy);
                                if(t != null) covered.add(t);
                            }
                        }
                    }
                }
            }

            float now = Time.time;

            for(Tile tile : world.tiles){
                if(!tile.isCenter()) continue;

                if(tile.floor() == RoutBlocks.richRouterFloor.asFloor() || tile.floor() == RoutBlocks.routerFloor.asFloor()){
                    if(!covered.contains(tile)){
                        Float start = orphanFloorSince.get(tile);
                        if(start == null){
                            orphanFloorSince.put(tile, now);
                        }else if(now - start >= orphanFloorTime){
                            boolean wasRich = tile.floor() == RoutBlocks.richRouterFloor.asFloor();
                            if(tile.build!= null) tile.build.kill();
                            tile.setFloor(mindustry.content.Blocks.stone.asFloor());
                            RoutFx.routerSpread.at(tile.worldx(), tile.worldy());
                            tile.setOverlay(wasRich ? mindustry.content.Blocks.oreCopper : mindustry.content.Blocks.air);
                            orphanFloorSince.remove(tile);
                        }
                    }else{
                        orphanFloorSince.remove(tile);
                    }
                    continue;
                }
                if(tile.build == null || tile.build.dead) continue;
                if(tile.build.team == Team.derelict) continue;
                if(!(tile.block() instanceof rout.world.blocks.bases.RouterBlock)) continue;

                mindustry.world.Block floor = tile.floor();
                if(floor != RoutBlocks.routerFloor && floor != RoutBlocks.richRouterFloor){
                    Float start = orphanBlockSince.get(tile);
                    if(start == null){
                        orphanBlockSince.put(tile, now);
                    }else if(now - start >= orphanBlockTime){
                        tile.build.kill();
                        orphanBlockSince.remove(tile);
                    }
                }else{
                    orphanBlockSince.remove(tile);
                }
            }
        });
    }

    @Override
    public void loadContent(){
        routSounds.load();
        RoutUnits.loadContent();
        RoutItems.loadContent();
        RoutBlocks.loadContent();
        RoutLoadouts.loadContent();
        RoutPlanets.loadContent();
        RoutTechTree.loadContent();
        routEntityMapping.init();
    }

    public static boolean isTemplate(Content content){
        return content.minfo.mod != null && content.minfo.mod.name.equals("rout");
    }
}