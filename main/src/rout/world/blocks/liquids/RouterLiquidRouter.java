package rout.world.blocks.liquids;

import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.storage.CoreBlock;
import rout.world.blocks.environment.RouterFloor;

public class RouterLiquidRouter extends LiquidRouter {
    public RouterLiquidRouter(String name) {
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
}
