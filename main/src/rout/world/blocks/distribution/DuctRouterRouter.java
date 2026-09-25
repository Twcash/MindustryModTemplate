package rout.world.blocks.distribution;

import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Edges;
import mindustry.world.blocks.distribution.DuctRouter;

/** Ducted version of {@link RouterRouter}, same shape so it slots into transmutations. */
public class DuctRouterRouter extends DuctRouter{
    public DuctRouterRouter(String name){
        super(name);
    }

    public class DuctRouterRouterBuild extends DuctRouterBuild {
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
    }
}
