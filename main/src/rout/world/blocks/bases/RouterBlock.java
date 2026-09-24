package rout.world.blocks.bases;

import mindustry.game.Team;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import rout.world.blocks.environment.RouterFloor;

//Standard class in case I want to modify any block of the following.
public class RouterBlock extends Block {
    //Can this block be placed on a non-routerFloor block.
    public boolean canPlaceOutsideRouter = false;
    public RouterBlock(String name) {
        super(name);
        update = true;
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
