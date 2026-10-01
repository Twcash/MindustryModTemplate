package rout.world.blocks.production;

import arc.struct.ObjectSet;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Pump;
import rout.world.blocks.environment.RouterFloor;

/** A pump that may only sit on whitelisted router-bearing floors, and destroys
 *  itself if its floor becomes disallowed (e.g. after a FloorConverter reverts). */
public class RouterPump extends Pump {
    /** Floors the pump may sit on. Empty = any {@link RouterFloor}. */
    public ObjectSet<Floor> whitelist = new ObjectSet<>();
    /** Seconds on a disallowed floor before the pump destroys itself. */
    public float graceTime = 2f;

    public RouterPump(String name){
        super(name);
    }

    private boolean floorAllowed(Floor floor){
        return whitelist.isEmpty() ? floor instanceof RouterFloor : whitelist.contains(floor);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        tile.getLinkedTilesAs(this, tempTiles);
        for(Tile t : tempTiles){
            if(!floorAllowed(t.floor())) return false;
        }
        return super.canPlaceOn(tile, team, rotation);
    }

    public class RouterPumpBuild extends PumpBuild {
        private float invalidTimer;

        @Override
        public void updateTile(){
            super.updateTile();

            boolean allowed = true;
            tempTiles.clear();
            tile.getLinkedTilesAs(RouterPump.this, tempTiles);
            for(Tile t : tempTiles){
                if(!floorAllowed(t.floor())){
                    allowed = false;
                    break;
                }
            }

            if(allowed){
                invalidTimer = 0f;
            }else{
                invalidTimer += Time.delta / 60f;
                if(invalidTimer >= graceTime){
                    kill();
                }
            }
        }
    }
}