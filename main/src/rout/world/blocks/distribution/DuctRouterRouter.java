package rout.world.blocks.distribution;

import arc.util.Time;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.DuctRouter;
import rout.world.blocks.environment.RouterFloor;

/** Ducted version of {@link RouterRouter}, same shape so it slots into transmutations. */
public class DuctRouterRouter extends DuctRouter{
    /** Seconds on a non-router floor before the block destroys itself. */
    public float graceTime = 2f;

    public DuctRouterRouter(String name){
        super(name);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        tile.getLinkedTilesAs(this, tempTiles);
        for(Tile t : tempTiles){
            if(!(t.floor() instanceof RouterFloor)) return false;
        }
        return super.canPlaceOn(tile, team, rotation);
    }

    public class DuctRouterRouterBuild extends DuctRouterBuild {
        private float invalidTimer;

        @Override
        public boolean acceptItem(Building source, Item item) {
            if(items.get(item) >= itemCapacity) return false;
            if(source.block instanceof MetaphysicalRouter) return true;
            return (Edges.getFacingEdge(source.tile, tile).relativeTo(tile) == rotation);
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            //destroy itself if its floor reverts off router flooring
            if(tile.floor() instanceof RouterFloor){
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