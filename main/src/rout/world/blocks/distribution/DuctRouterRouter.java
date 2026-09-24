package rout.world.blocks.distribution;

import mindustry.gen.Building;
import mindustry.type.Item;
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
            //meta routers can feed from any direction
            if(source.block instanceof MetaphysicalRouter) return true;
            //only accept from the back (input side)
            return relativeTo(source.tileX(), source.tileY()) == (rotation + 2) % 4;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity;
        }
    }
}
