package rout.world.blocks.distribution;

import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.storage.CoreBlock;
import rout.world.blocks.environment.RouterFloor;

public class RouterRouter extends Router {
    public RouterRouter(String name) {
        super(name);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        if (tile == null) return false;
        //Only change here is letting the core be placed on any router floor
        tile.getLinkedTilesAs(this, tempTiles);
        if (!tempTiles.contains(o -> !o.floor().allowCorePlacement || o.block() instanceof CoreBlock || o.floor() instanceof RouterFloor)) {
            return true;
        }
        super.canPlaceOn(tile, team, rotation);
        return false;
    }
    public class RouterRouterBuild extends RouterBuild{

    }
}