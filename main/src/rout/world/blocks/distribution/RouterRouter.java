package rout.world.blocks.distribution;

import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
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
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        super.canPlaceOn(tile, team, rotation);
        return false;
    }

    public class RouterRouterBuild extends RouterBuild {
        @Override
        public boolean acceptItem(Building source, Item item) {
            return items.get(item) < itemCapacity;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity;
        }
    }
}